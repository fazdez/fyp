package fazirul.fyp.basic;

import fazirul.fyp.elements.DistributedApplication;
import fazirul.fyp.elements.ResourceBundle;
import fazirul.fyp.elements.Server;
import org.cloudbus.cloudsim.cloudlets.CloudletSimple;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.core.events.SimEvent;
import org.cloudbus.cloudsim.utilizationmodels.UtilizationModelFull;
import org.cloudbus.cloudsim.vms.Vm;
import org.cloudbus.cloudsim.vms.VmSimple;

import java.util.HashMap;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class PlaceholderApplication extends DistributedApplication {

    public PlaceholderApplication(CloudSim simulation, String username, double arrivalTime, List<ResourceBundle> tasks) {
        super(simulation, username, arrivalTime, tasks);
    }

    @Override
    protected void handleTaskOffloadEvent(SimEvent evt) {
        for (ResourceBundle task: getTasks()) {
            CloudletSimple cloudlet = new CloudletSimple(1, task.getCPU());
            cloudlet.setUtilizationModel(new UtilizationModelFull());

            Vm virtualMachine = new VmSimple(1, task.getCPU());
            virtualMachine.setRam(0);
            virtualMachine.setBw(0);
            if (!offload(getEdgeServers().stream().findAny().get(), virtualMachine, cloudlet)) {
                LOGGER.error("{}: {}: Attempting to offload when resource available is not enough.", getSimulation().clockStr(), getName());
            }

        }
    }

    @Override
    protected void orchestrate() {
        try {
            TimeUnit.SECONDS.sleep(1);
        } catch (Exception e) {
            e.printStackTrace();
        }

        ended = true;
    }

    @Override
    protected void initialize() {
        //empty
    }

    @Override
    protected void postProcessing() {
        //empty
    }

    @Override
    public void printResults() {

    }

    @Override
    public HashMap<Server, ResourceBundle> getFinalResourcesConsumption() {
        return null;
    }
}
