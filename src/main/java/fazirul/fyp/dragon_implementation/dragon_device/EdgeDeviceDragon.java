package fazirul.fyp.dragon_implementation.dragon_device;

import fazirul.fyp.dragon.utils.VirtualMachineHandler;
import fazirul.fyp.elements.EdgeDeviceAbstract;
import fazirul.fyp.elements.EdgeServer;
import fazirul.fyp.elements.ResourceBundle;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.core.events.SimEvent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class EdgeDeviceDragon extends EdgeDeviceAbstract {
    public EdgeDeviceDragon(CloudSim simulation, String username, double arrivalTime, List<ResourceBundle> tasks) {
        super(simulation, username, arrivalTime, tasks);
    }

    //TODO
    @Override
    protected void handleTaskOffloadEvent(SimEvent evt) {

    }

    @Override
    protected void orchestrate() {

    }

    @Override
    protected void initialize() {

    }

    @Override
    protected void postProcessing() {

    }

    @Override
    public void printResults() {

    }

    //TODO
    @Override
    public HashMap<EdgeServer, ResourceBundle> getFinalResourcesConsumption() {
        return null;
    }

    @Override
    public void reset() {
        setIndex(-1);
        neighbours.clear();
        setRuntime(-1);
        incomingMessages.flush();
        failed = false;
    }
}
