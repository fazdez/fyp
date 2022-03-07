package fazirul.fyp.elements;

import org.cloudbus.cloudsim.core.CloudSimEntity;
import org.cloudbus.cloudsim.core.CloudSimTag;
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
    private final List<EdgeDeviceAbstract> edgeDeviceList = new ArrayList<>();

    /**
     * Each device that has ended will be moved to this list.
     * A device is considered to have ended if it received a {@link DistributedSimTags#TASK_OFFLOAD_EVENT}.
     *
     * <p>This is useful to print some statistics after the whole simulation run.</p>
     */
    private final List<EdgeDeviceAbstract> completedList = new ArrayList<>();

    /**
     * Each device that has failed will be moved to this list.
     * A device is considered to have failed if its runtime is still -1 even after {@link #runSimulation()}.
     *
     * <p>This is useful to print some statistics after the whole simulation run.</p>
     */
    private final List<EdgeDeviceAbstract> failedList = new ArrayList<>();

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
            if (!(simEvent.getSource() instanceof EdgeDeviceAbstract)) {
                LOGGER.warn("{}: {}: received event from unexpected entity.", getSimulation().clockStr(), this);
                return;
            }
            resetEdgeDevices();
            runSimulation();
            offloadEligibleEdgeDevices();
        } else {
            shutdown();
        }
    }

    /**
     * If edge device is successful in the distributed simulation (i.e. runtime != -1),
     * create a new offload event to be handled by the edge device. If device is unsuccessful, move
     * device to failedDevices list.
     */
    private void offloadEligibleEdgeDevices() {
        int idx = 0;
        while (idx < edgeDeviceList.size()) {
            EdgeDeviceAbstract ed = edgeDeviceList.get(idx);
            if (ed.getRuntime() != -1) {
                double offloadTime = getSimulation().clock() + ed.getRuntime();

                /*
                Check future events earlier than offloadTime for the following
                1) StartAlgorithmEvent
                2) ArrivalEvent, a, if a.getTime() + WARM_UP_TIME < offloadTime

                If no such events exist in the future event list, schedule OffloadEvent at offloadTime to the edge device.
                 */

                long numInvalidationEvents = getSimulation().getNumberOfFutureEvents(evt -> {
                   if (evt.getTime() <= offloadTime) {
                       if (evt.getTag() == DistributedSimTags.START_ALGORITHM_EVENT && evt.getSource() instanceof EdgeDeviceAbstract) {
                           return true;
                       }

                       return evt.getTag() == DistributedSimTags.ARRIVAL_EVENT && evt.getSource() instanceof EdgeDeviceAbstract &&
                               evt.getTime() + EdgeDeviceAbstract.WARM_UP_TIME <= offloadTime;
                   }
                   return false;
                });

                if (numInvalidationEvents == 0) {
//                    LOGGER.info("{}: {}: {} successful in distributed simulation. Runtime = {}",
//                            getSimulation().clockStr(), getName(), ed.getName(), ed.getRuntime());
                    send(ed, ed.getRuntime(), DistributedSimTags.TASK_OFFLOAD_EVENT);
                }
                idx++;
            } else {
                removeEdgeDevice(ed);
                ed.shutdown();
                failedList.add(ed);
            }
        }
    }

    /**
     * Based on the {@link #edgeDeviceList participating edge devices}, run the distributed algorithm for each device.
     * <p>Functions are run using multi-threading.</p>
     *
     * @see EdgeDeviceAbstract#startDistributedAlgorithm()
     */
    private void runSimulation() {
        LOGGER.info("{}: {} starting Distributed Simulation with {} participating devices...",
                getSimulation().clockStr(), getName(), getNumParticipatingEntities());
        List<Thread> threads = new ArrayList<>();
        for (EdgeDeviceAbstract ed : edgeDeviceList) {
            threads.add(new Thread(ed::startDistributedAlgorithm));
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

        for (EdgeDeviceAbstract ed: edgeDeviceList) {
            ed.printResults();
        }
    }

    /**
     * For each edge device, reset its internal variables related to distributed algorithm.
     * Then create the topology based on the current device list.
     * @see EdgeDeviceAbstract#reset()
     */
    private void resetEdgeDevices() {
        for (int idx = 0; idx < edgeDeviceList.size(); idx++) {
            EdgeDeviceAbstract edgeDevice = edgeDeviceList.get(idx);
            edgeDevice.reset();
            edgeDevice.setIndex(idx);
        }

        createTopology();
    }

    /**
     * @param edgeDevice device to be added
     * @see #edgeDeviceList
     */
    public void addEdgeDevice(EdgeDeviceAbstract edgeDevice) {
        edgeDeviceList.add(edgeDevice);
    }

    /**
     * @param edgeDevice device to be removed
     * @see #edgeDeviceList
     */
    public void removeEdgeDevice(EdgeDeviceAbstract edgeDevice) {
        edgeDeviceList.remove(edgeDevice);
    }

    /**
     * @param edgeDevice device that has completed and not participating in future events
     * @see #completedList
     */
    public void addToCompletedList(EdgeDeviceAbstract edgeDevice) {
        completedList.add(edgeDevice);
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
     * Usage can be found in {@link #resetEdgeDevices()} only.
     *
     * <p>For sparse topology, each device will have at most 2 neighbours (left and/or right in the list).</p>
     * <p>For dense topology, each device will be connected to every other device.</p>
     */
    private void createTopology() {
        for (int idx = 0; idx < edgeDeviceList.size(); idx++) {
            EdgeDeviceAbstract edgeDevice = edgeDeviceList.get(idx);
            if (topology == 0) { // SPARSE TOPOLOGY
                if (idx > 0) {
                    edgeDevice.addNeighbour(edgeDeviceList.get(idx - 1));
                }
                if (idx < edgeDeviceList.size() - 1) {
                    edgeDevice.addNeighbour(edgeDeviceList.get(idx + 1));
                }
            } else { // DENSE TOPOLOGY
                int copy = idx; //not allowed to have reassigned variable in lambda function, thus make a copy
                edgeDeviceList.forEach(e -> {
                    // if it's not the current index (itself), add as neighbour
                    if (e.getIndex() != copy) {
                        edgeDevice.addNeighbour(e);
                    }
                });
            }
        }
    }

    /**
     * @return the total number of edge devices participating in distributed simulation.
     */
    public int getNumParticipatingEntities() {
        return edgeDeviceList.size();
    }
}
