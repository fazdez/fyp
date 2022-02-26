package fazirul.fyp.elements;

import org.cloudbus.cloudsim.brokers.DatacenterBrokerSimple;
import org.cloudbus.cloudsim.cloudlets.Cloudlet;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.vms.Vm;

import java.util.ArrayList;
import java.util.HashMap;

public abstract class EdgeDeviceAbstract extends DatacenterBrokerSimple {
    private static final String DEFAULT_NAME = "EdgeDevice_";
    private final int retries = 3;
    private int id = -1;
    private final String username;
    private boolean ended = false;

    public EdgeDeviceAbstract(CloudSim simulation, String username) {
        super(simulation, DEFAULT_NAME + username);
        this.username = username;
    }

    public void setID(int id) {
        this.id = id;
    }

    public int getID() {
        return this.id;
    }

    public String getUsername() { return this.username; }

    public boolean checkIfOffloadPossible(EdgeServer server, Vm virtualMachine) {
        ResourceBundle availableResources = server.getAvailableResources();
        ResourceBundle resourceDemanded = new ResourceBundle(virtualMachine);

        return availableResources.isBounded(resourceDemanded);
    }

    public boolean offload(EdgeServer server, Vm virtualMachine, Cloudlet task) {
        if (!checkIfOffloadPossible(server, virtualMachine)) { return false; }
        this.setDatacenterMapper((dc, u) -> server);

        this.submitVm(virtualMachine);
        ArrayList<Cloudlet> cloudletList = new ArrayList<>();
        cloudletList.add(task);
        this.submitCloudletList(cloudletList, virtualMachine);

        return true;
    }

    public void startDistributedAlgorithm() {
        initialize();
        while (!ended) {
            orchestrate();
        }
        postProcessing();
    }

    protected abstract void orchestrate();

    protected abstract void initialize();

    protected abstract void postProcessing();

    public abstract void printResults();

    public abstract HashMap<Node, ResourceBundle> getFinalResourcesConsumption();
}
