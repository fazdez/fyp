package fazirul.fyp.elements;

import org.cloudbus.cloudsim.core.CloudSimEntity;
import org.cloudbus.cloudsim.core.Simulation;
import org.cloudbus.cloudsim.core.events.SimEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class DistSimManager extends CloudSimEntity {
    private static final Logger LOGGER = LoggerFactory.getLogger(CloudSimEntity.class.getSimpleName());
    private int topology = 0;

    /**
     * To run the distributed algorithm, the DistSimManager has to keep track of the current devices
     * participating in the algorithm. It is maintained in this list.
     */
    private final List<DistributedApplication> participatingApplications = new ArrayList<>();

    /**
     * Each device that has ended will be moved to this list.
     * A device is considered to have ended if it received a {@link DistributedSimTags#TASK_OFFLOAD_EVENT}.
     *
     * <p>This is useful to print some statistics after the whole simulation run.</p>
     */
    private final List<DistributedApplication> completedList = new ArrayList<>();

    /**
     * Each device that has failed will be moved to this list.
     * A device is considered to have failed if its runtime is still -1 even after {@link #runSimulation()}.
     *
     * <p>This is useful to print some statistics after the whole simulation run.</p>
     */
    private final List<DistributedApplication> failedList = new ArrayList<>();

    public DistSimManager(Simulation simulation) {
        super(simulation);
    }

    @Override
    protected void startInternal() {
        LOGGER.info("{}: {} starting...", getSimulation().clockStr(), this);
    }

    @Override
    public void processEvent(SimEvent simEvent) {
        if (simEvent.getTag() == DistributedSimTags.START_ALGORITHM_EVENT) {
            if (!(simEvent.getSource() instanceof DistributedApplication)) {
                LOGGER.warn("{}: {}: received event from unexpected entity.", getSimulation().clockStr(), this);
                return;
            }
            resetApplications();
            runSimulation();
            offloadEligibleApplications();
        } else {
            shutdown();
        }
    }

    /**
     * If application is successful in the distributed simulation (i.e. runtime != -1),
     * create a new offload event to be handled by the application. If device is unsuccessful, move
     * device to failedDevices list.
     */
    private void offloadEligibleApplications() {
        int idx = 0;
        while (idx < participatingApplications.size()) {
            DistributedApplication app = participatingApplications.get(idx);
            if (app.getRuntime() != -1) {
                double offloadTime = getSimulation().clock() + app.getRuntime();

                /*
                Check future events earlier than offloadTime for the following
                1) StartAlgorithmEvent
                2) ArrivalEvent, a, if a.getTime() + WARM_UP_TIME < offloadTime

                If no such events exist in the future event list, schedule OffloadEvent at offloadTime to the application.
                 */

                long numInvalidationEvents = getSimulation().getNumberOfFutureEvents(evt -> {
                   if (evt.getTime() <= offloadTime) {
                       if (evt.getTag() == DistributedSimTags.START_ALGORITHM_EVENT && evt.getSource() instanceof DistributedApplication) {
                           return true;
                       }

                       return evt.getTag() == DistributedSimTags.ARRIVAL_EVENT && evt.getSource() instanceof DistributedApplication &&
                               evt.getTime() + DistributedApplication.WARM_UP_TIME <= offloadTime;
                   }
                   return false;
                });

                if (numInvalidationEvents == 0) {
//                    LOGGER.info("{}: {}: {} successful in distributed simulation. Runtime = {}",
//                            getSimulation().clockStr(), getName(), app.getName(), app.getRuntime());
                    send(app, app.getRuntime(), DistributedSimTags.TASK_OFFLOAD_EVENT);
                }
                idx++;
            } else {
                removeApplication(app);
                app.shutdown();
                failedList.add(app);
            }
        }
    }

    /**
     * Based on the {@link #participatingApplications}, run the distributed algorithm for each device.
     * <p>Functions are run using multi-threading.</p>
     *
     * @see DistributedApplication#startDistributedAlgorithm()
     */
    private void runSimulation() {
        LOGGER.info("{}: {} starting Distributed Simulation with {} participating devices...",
                getSimulation().clockStr(), getName(), getNumParticipatingApplications());
        List<Thread> threads = new ArrayList<>();
        for (DistributedApplication app : participatingApplications) {
            threads.add(new Thread(app::startDistributedAlgorithm));
        }

        for (Thread t: threads) {
            t.start();
        }

        for (Thread t: threads) {
            try {
                t.join();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        for (DistributedApplication app : participatingApplications) {
            app.printResults();
        }
    }

    /**
     * For each application, reset its internal variables related to distributed algorithm.
     * Then create the topology based on the current device list.
     * @see DistributedApplication#reset()
     */
    private void resetApplications() {
        for (int idx = 0; idx < participatingApplications.size(); idx++) {
            DistributedApplication app = participatingApplications.get(idx);
            app.reset();
            app.setIndex(idx);
        }

        createTopology();
    }

    /**
     * @param Application device to be added
     * @see #participatingApplications
     */
    public void addApplication(DistributedApplication Application) {
        participatingApplications.add(Application);
    }

    /**
     * @param Application device to be removed
     * @see #participatingApplications
     */
    public void removeApplication(DistributedApplication Application) {
        participatingApplications.remove(Application);
    }

    /**
     * @param Application device that has completed and not participating in future events
     * @see #completedList
     */
    public void addToCompletedList(DistributedApplication Application) {
        completedList.add(Application);
    }

    /**
     * Sets the network topology to be sparse (i.e. maximum network diameter)
     */
    public void setSparseTopology() {
        topology = 0;
    }

    /**
     * Sets the network topology to be dense (i.e. network diameter = 1)
     */
    public void setDenseTopology() {
        topology = 1;
    }

    /**
     * Based on the topology (sparse vs dense), add the appropriate neighbours for each device.
     * Usage can be found in {@link #resetApplications()} only.
     *
     * <p>For sparse topology, each device will have at most 2 neighbours (left and/or right in the list).</p>
     * <p>For dense topology, each device will be connected to every other device.</p>
     */
    private void createTopology() {
        for (int idx = 0; idx < participatingApplications.size(); idx++) {
            DistributedApplication app = participatingApplications.get(idx);
            if (topology == 0) { // SPARSE TOPOLOGY
                if (idx > 0) {
                    app.addNeighbour(participatingApplications.get(idx - 1));
                }
                if (idx < participatingApplications.size() - 1) {
                    app.addNeighbour(participatingApplications.get(idx + 1));
                }
            } else { // DENSE TOPOLOGY
                int copy = idx; //not allowed to have reassigned variable in lambda function, thus make a copy
                participatingApplications.forEach(e -> {
                    // if it's not the current index (itself), add as neighbour
                    if (e.getIndex() != copy) {
                        app.addNeighbour(e);
                    }
                });
            }
        }
    }

    /**
     * @return the total number of applications participating in distributed simulation.
     */
    public int getNumParticipatingApplications() {
        return participatingApplications.size();
    }
}
