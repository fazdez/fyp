package fazirul.fyp.elements;

import org.apache.commons.math3.geometry.spherical.twod.Edge;
import org.cloudbus.cloudsim.brokers.DatacenterBrokerSimple;
import org.cloudbus.cloudsim.cloudlets.Cloudlet;
import org.cloudbus.cloudsim.cloudlets.CloudletSimple;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.core.CloudSimEntity;
import org.cloudbus.cloudsim.core.CloudSimTag;
import org.cloudbus.cloudsim.core.SimEntity;
import org.cloudbus.cloudsim.core.events.SimEvent;
import org.cloudbus.cloudsim.utilizationmodels.UtilizationModelFull;
import org.cloudbus.cloudsim.vms.Vm;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.*;

/**
 * Represents an edge device in a distributed context.
 * Basic properties of edge device:
 * <ul>
 *     <li>Inter-device {@link #broadcast(MessageInterface) communication}</li>
 *     <li>List of {@link #tasks}</li>
 *     <li>An {@link #orchestrate() orchestration algorithm}</li>
 * </ul>
 */
public abstract class EdgeDeviceAbstract extends CloudSimEntity {
    protected static final Logger LOGGER = LoggerFactory.getLogger(CloudSimEntity.class.getSimpleName());
    private static final String DEFAULT_NAME = "EdgeDevice_";
    private static final int DEFAULT_CLOUDLET_LENGTH = 100;
    public static final double WARM_UP_TIME = 0.5;
    private static int VM_ID =0;

    /**
     * <p>For communication to the edge server</p>
     * {@inheritDoc}
     */
    private final DatacenterBrokerSimple broker;

    /**
     * To differentiate between edge devices. Must be unique.
     */
    private final String username;

    /**
     * List of edge devices connected to the current device.
     */

    protected final List<EdgeDeviceAbstract> neighbours = new ArrayList<>();

    /**
     * Incoming {@link MessageQueue message queue}.
     * @see #addToQueue(MessageInterface)
     */
    protected MessageQueue incomingMessages = new MessageQueue();

    /**
     * Each task is represented by the resource demanded.
     * <p>Upon offload, a {@link CloudletSimple} will be created to integrate with CloudSimPlus.</p>
     */
    protected final List<ResourceBundle> tasks;

    /**
     * Indicates the end of orchestration.
     */
    protected boolean ended = false;

    /**
     * Keeps track of messages sent. Useful for statistics purposes.
     */
    private int totalMessagesSent = 0;

    /**
     * @see #startInternal()
     */
    private final double arrivalTime;

    /**
     * Index will be set by {@link DistSimManager}.
     * <p>This is to identify between different edge devices during the distributed algorithm process.
     * {@link DistSimManager} will assign based on its index in the current list of edge devices.
     * </p>
     */
    private int index = -1;

    /**
     * This represents the time it took for the device to complete the distributed process (in seconds).
     */
    private double runtime = -1;

    /**
     * If edge device fails to win any resources after the algorithm process, set failed = true.
     */
    protected boolean failed = false;

    /**
     *
     */
    private final HashSet<EdgeServer> edgeServers = new HashSet<>();

    public EdgeDeviceAbstract(CloudSim simulation, String username, double arrivalTime, List<ResourceBundle> tasks) {
        super(simulation);
        setName(DEFAULT_NAME + username);
        broker = new DatacenterBrokerSimple(simulation, DEFAULT_NAME + username);
        broker.setVmDestructionDelay(0.2);
        //add all edge servers found. IMPORTANT, Edge servers MUST be created BEFORE edge device created!
        getSimulation().getEntityList().stream().filter(simEntity -> simEntity instanceof EdgeServer)
                .forEach(simEntity -> edgeServers.add((EdgeServer) simEntity));
        this.username = username;
        this.arrivalTime = arrivalTime;
        this.tasks = tasks;
    }

    public void setIndex(int id) {
        this.index = id;
    }

    public int getIndex() {
        return this.index;
    }

    public String getUsername() { return this.username; }

    public double getRuntime() {
        return runtime;
    }

    public void addNeighbour(EdgeDeviceAbstract neighbour) {
        this.neighbours.add(neighbour);
    }

    public List<ResourceBundle> getTasks() {
        return tasks;
    }

    public HashSet<EdgeServer> getEdgeServers() { return edgeServers; }

    @Override
    protected void startInternal() {
        //send ArrivalEvent to itself at the arrival time
        if(!schedule(arrivalTime, DistributedSimTags.ARRIVAL_EVENT)) {
            LOGGER.warn("{}: {}: Could not schedule ArrivalEvent to itself.",
                    getSimulation().clockStr(), getName());
        };
    }

    @Override
    public void processEvent(SimEvent simEvent) {
        if (simEvent.getTag() == CloudSimTag.SIMULATION_END) {
//            broker.shutdown();
            return;
        }

        DistSimManager manager = getDistSimManager();
        if (manager == null) {
            return;
        }

        if (simEvent.getTag() == DistributedSimTags.ARRIVAL_EVENT) {
            LOGGER.info("{}: {}: Arrived into system.",
                    getSimulation().clockStr(), getName());
            //next event will be StartAlgoEvent at the following time
            double startAlgoTime = getSimulation().clock() + WARM_UP_TIME;

            //check if the next event will be at the same time as any future StartAlgoEvent
            long numStartAlgoEventsAtSameTime = getSimulation().getNumberOfFutureEvents(evt -> evt.getTag() == DistributedSimTags.START_ALGORITHM_EVENT
                    && evt.getSource() instanceof EdgeDeviceAbstract && evt.getTime() == startAlgoTime);

            getDistSimManager().addEdgeDevice(this); //register the edge device to the DistSimManager

            if (numStartAlgoEventsAtSameTime > 1) {
                //this should NOT happen
                LOGGER.warn("{}: {}: There are two or more StartAlgoEvent at the same time.",
                            getSimulation().clockStr(), getName());
            } else if (numStartAlgoEventsAtSameTime == 0) {
                //if no StartAlgoEvent at the same time, proceed to send this event to the DistSimManager
                send(manager, WARM_UP_TIME, DistributedSimTags.START_ALGORITHM_EVENT);
            }
        } else if (simEvent.getTag() == DistributedSimTags.TASK_OFFLOAD_EVENT) {
            manager.removeEdgeDevice(this);
            handleTaskOffloadEvent(simEvent);
            manager.addToCompletedList(this);
        }
    }

    /**
     * Used by other EdgeDeviceAbstract instances to send messages to the current instance.
     * Usage only in broadcast function.
     *
     * @param message Message to be sent.
     * @see #broadcast(MessageInterface)
     */
    private void addToQueue(MessageInterface message) {
        new Thread(() -> incomingMessages.addMessage(message)).start();
    }


    /**
     * Based on the simulation's entity list, find the only DistSimManager.
     *
     * @return the manager for distributed simulation
     * @see DistSimManager
     */
    protected DistSimManager getDistSimManager() {
        List<SimEntity> entities = getSimulation().getEntityList();
        Optional<SimEntity> result = entities.stream().
                filter(s -> s instanceof DistSimManager).findAny();

        if (result.isEmpty()) {
            LOGGER.warn(
                    "{}: {}: Cannot start EdgeDevice without a DistSimManager entity registered.",
                    getSimulation().clockStr(), getName());
            shutdown();
            return null;
        }

        return (DistSimManager) result.get();
    }

    /**
     * Broadcast message to all of its neighbours.
     *
     * @param message Message to be sent.
     */
    protected void broadcast(MessageInterface message) {
        totalMessagesSent++;
        for (EdgeDeviceAbstract n: this.neighbours) {
            n.addToQueue(message.clone());
        }
    }


    /**
     * @param server the edge server to offload to
     * @param virtualMachine the virtual machine being created
     * @return true if there is enough resources in the server to spin up the virtual machine
     */
    protected boolean checkIfOffloadPossible(EdgeServer server, Vm virtualMachine) {
        ResourceBundle availableResources = server.getAvailableResources();
        ResourceBundle resourceDemanded = new ResourceBundle(virtualMachine);

        return availableResources.isBounded(resourceDemanded);
    }

    /**
     * Attempts to create a {@link Vm virtual machine} on the {@link EdgeServer server}.
     * If successful, task will be offloaded to the virtual machine.
     *
     * @param server the server to offload to
     * @param virtualMachine the virtual machine being created
     * @param task the task to offload
     * @return true if the task was offloaded
     */
    protected boolean offload(EdgeServer server, Vm virtualMachine, ResourceBundle task) {
        if (!checkIfOffloadPossible(server, virtualMachine)) { return false; }
        broker.setDatacenterMapper((dc, u) -> server);
        virtualMachine.setId(VM_ID++);
        broker.submitVm(virtualMachine);

        ArrayList<Cloudlet> cloudletList = new ArrayList<>();
        Cloudlet cloudlet = new CloudletSimple(DEFAULT_CLOUDLET_LENGTH, task.getCPU());
        cloudlet.setUtilizationModel(new UtilizationModelFull());
        cloudletList.add(cloudlet);
        broker.submitCloudletList(cloudletList, virtualMachine);

        return true;
    }

    /**
     * Called when this edge device receives a TASK_OFFLOAD_EVENT.
     *
     * @see #processEvent(SimEvent)
     */
    protected abstract void handleTaskOffloadEvent(SimEvent evt);

    /**
     * Used only for use by {@link DistSimManager}. Not to be modified.
     */
    public void startDistributedAlgorithm() {
        LocalTime startTime = LocalTime.now();
        initialize();
        while (!ended) {
            orchestrate();
        }
        postProcessing();
        if (!failed) { runtime = startTime.until(LocalTime.now(), ChronoUnit.MILLIS)/1000.0; }
        else { runtime = -1; }
    }

    @Override
    public void shutdown() {
//        super.shutdown();
        broker.shutdown();
    }

    /**
     * The orchestration algorithm to decide where to offload its tasks.
     * Will run in a loop until {@link #ended} is set to true.
     *
     * Example can be found in {@link fazirul.fyp.dragon.dragonDevice.EdgeDeviceDragon}.
     *
     * @see fazirul.fyp.dragon.dragonDevice.EdgeDeviceDragon
     */
    protected abstract void orchestrate();


    /**
     * Pre-orchestration initialization.
     */
    protected abstract void initialize();

    /**
     * Post-orchestration processing.
     */
    protected abstract void postProcessing();


    /**
     * For use by {@link DistSimManager}.
     * Prints any useful information about the application after a single run of distributed simulation.
     */
    public abstract void printResults();

    /**
     * For each {@link EdgeServer edge server}, get the resource consumption by this edge device.
     * @return the resource consumption
     */
    public abstract HashMap<EdgeServer, ResourceBundle> getFinalResourcesConsumption();

    /**
     * Resets the relevant variables for a distributed simulation to start anew.
     */
    public abstract void reset();
}
