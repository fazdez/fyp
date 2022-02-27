package fazirul.fyp.dragon_implementation.dragon_device;

import fazirul.fyp.dragon_implementation.utils.TaskAssignment;
import fazirul.fyp.dragon_implementation.utils.VirtualMachineHandler;
import fazirul.fyp.elements.EdgeDeviceAbstract;
import fazirul.fyp.elements.EdgeServer;
import fazirul.fyp.elements.ResourceBundle;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.core.events.SimEvent;
import org.cloudbus.cloudsim.vms.Vm;

import java.util.HashMap;
import java.util.List;

public class EdgeDeviceDragon extends EdgeDeviceAbstract {
    private final AssignmentVector assignments;
    protected final VirtualMachineHandler vmHandler = VirtualMachineHandler.getInstance();

    public EdgeDeviceDragon(CloudSim simulation, String username, double arrivalTime, List<ResourceBundle> tasks) {
        super(simulation, username, arrivalTime, tasks);
        assignments = new AssignmentVector(this);
    }

    //TODO
    @Override
    protected void handleTaskOffloadEvent(SimEvent evt) {
        if (!assignments.isOffloadPossible()) {
            LOGGER.warn("{}: {}: Not enough resources available for the edge device to offload its tasks.", getSimulation().clockStr(), this);
            //TODO: Some post-debugging logic to identify why this is the case.
            return;
        }

        for (TaskAssignment t: assignments.assignmentList) {
            Vm virtualMachine = vmHandler.createVm(t.getVirtualMachineID());
            ResourceBundle task = tasks.get(t.getTaskID());
            if (!offload(t.getServer(), virtualMachine, task)) {
                LOGGER.error("{}: {}: Attempting to offload when resource available is not enough.", getSimulation().clockStr(), this);
            }
        }
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
        assignments.clear();
    }

    /**
     * For each edge server found in the simulation, get the resource available.
     * @return each edge server mapped to their available resource
     *
     * @see EdgeServer#getAvailableResources()
     */
    protected HashMap<EdgeServer, ResourceBundle> getResourceAvailableInServers() {
        HashMap<EdgeServer, ResourceBundle> result = new HashMap<>();
        for (EdgeServer e: getEdgeServers()) {
            result.put(e, e.getAvailableResources());
        }
        return result;
    }
}
