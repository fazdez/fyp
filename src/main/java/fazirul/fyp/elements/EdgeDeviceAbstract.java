package fazirul.fyp.elements;

import org.cloudbus.cloudsim.brokers.DatacenterBrokerSimple;
import org.cloudbus.cloudsim.cloudlets.Cloudlet;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.core.CloudSimEntity;
import org.cloudbus.cloudsim.core.events.SimEvent;
import org.cloudbus.cloudsim.vms.Vm;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public abstract class EdgeDeviceAbstract extends CloudSimEntity {
    private static final String DEFAULT_NAME = "EdgeDevice_";
    private final DatacenterBrokerSimple broker;
    private final String username;
    protected List<EdgeDeviceAbstract> neighbours = new ArrayList<>();
    protected MessageQueue incomingMessages = new MessageQueue();
    protected boolean ended = false;
    private int totalMessagesSent = 0;

    public EdgeDeviceAbstract(CloudSim simulation, String username) {
        super(simulation);
        setName(DEFAULT_NAME + username);
        broker = new DatacenterBrokerSimple(simulation, DEFAULT_NAME + username);
        this.username = username;
    }

    public String getUsername() { return this.username; }

    public void addNeighbour(EdgeDeviceAbstract neighbour) {
        this.neighbours.add(neighbour);
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
    protected boolean offload(EdgeServer server, Vm virtualMachine, Cloudlet task) {
        if (!checkIfOffloadPossible(server, virtualMachine)) { return false; }
        broker.setDatacenterMapper((dc, u) -> server);

        broker.submitVm(virtualMachine);
        ArrayList<Cloudlet> cloudletList = new ArrayList<>();
        cloudletList.add(task);
        broker.submitCloudletList(cloudletList, virtualMachine);

        return true;
    }

    /**
     * Used only for use by {@link DistributedSimulation}. Not to be modified.
     */
    protected void startDistributedAlgorithm() {
        initialize();
        while (!ended) {
            orchestrate();
        }
        postProcessing();
    }

    /**
     * The orchestration algorithm to decide where to offload its tasks.
     * Will run in a loop until {@link #ended} is set to true.
     *
     * Example can be found in {@link fazirul.fyp.dragon.app.DragonApplication}.
     *
     * @see fazirul.fyp.dragon.app.DragonApplication
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
     * For use by {@link DistributedSimulation}.
     * Prints any useful information about the application after the DistributedSimulation has run.
     */
    public abstract void printResults();

    /**
     * For each {@link EdgeServer edge server}, get the resource consumption by this edge device.
     * @return the resource consumption
     */
    public abstract HashMap<Node, ResourceBundle> getFinalResourcesConsumption();
}
