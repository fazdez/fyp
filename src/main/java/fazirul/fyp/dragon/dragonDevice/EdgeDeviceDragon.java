package fazirul.fyp.dragon.dragonDevice;

import fazirul.fyp.dragon.utils.Election;
import fazirul.fyp.dragon.utils.Message;
import fazirul.fyp.dragon.utils.TaskAssignment;
import fazirul.fyp.dragon.utils.VirtualMachineHandler;
import fazirul.fyp.elements.*;
import org.cloudbus.cloudsim.cloudlets.CloudletSimple;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.core.events.SimEvent;
import org.cloudbus.cloudsim.utilizationmodels.UtilizationModelFull;
import org.cloudbus.cloudsim.vms.Vm;

import java.time.LocalTime;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class EdgeDeviceDragon extends DistributedApplication {
    private static final long CLOUDLET_LENGTH = 1;
    private final long TIME_TO_WAIT = 100;
    protected final AssignmentVector assignments;
    protected GlobalData globalData;
    protected final HashMap<Server, Double> maxBidRatio = new HashMap<>();
    private final HashMap<Integer, Double> taskLength = new HashMap<>();


    protected final VirtualMachineHandler vmHandler = VirtualMachineHandler.getInstance();

    public EdgeDeviceDragon(CloudSim simulation, String username, double arrivalTime, List<ResourceBundle> tasks) {
        super(simulation, username, arrivalTime, tasks);
        assignments = new AssignmentVector(this);
        globalData = new GlobalData(this, getDistSimManager().getNumParticipatingApplications());
        getEdgeServers().forEach(e -> maxBidRatio.put(e, Double.MAX_VALUE));
    }


    @Override
    public void addTask(ResourceBundle task) {
        super.addTask(task);
        assignments.addTask(task);
    }

    public void addTaskLength(int taskIndex, double duration) {
        taskLength.put(taskIndex, duration);
    }

    //TODO
    @Override
    protected void handleTaskOffloadEvent(SimEvent evt) {
        if (!assignments.isOffloadPossible()) {
            LOGGER.warn("{}: {}: Not enough resources available for the edge device to offload its tasks.", getSimulation().clockStr(), getName());
            //TODO: Some post-debugging logic to identify why this is the case.
            return;
        }

        for (TaskAssignment t: assignments.assignmentList) {
            Vm virtualMachine = vmHandler.createVm(t.getVirtualMachineID());
            ResourceBundle task = tasks.get(t.getTaskID());
            CloudletSimple cloudlet = new CloudletSimple(CLOUDLET_LENGTH, task.getCPU());
            cloudlet.setUtilizationModel(new UtilizationModelFull());
            if (taskLength.get(t.getTaskID()) != null) {
                cloudlet.setLength(taskLength.get(t.getTaskID()).longValue());
            }

            if (!offload(t.getServer(), virtualMachine, cloudlet)) {
                LOGGER.error("{}: {}: Attempting to offload when resource available is not enough.", getSimulation().clockStr(), getName());
            }
        }
    }

    @Override
    protected void orchestrate() {
        orchestrate(false);
    }

    /**
     * See Algorithm 1 in DRAGON paper.
     *
     * <p>NOTE: upon each successful agreement, we wait a certain amount of time, re-run the agreement again
     * as a double-confirmation mechanism i.e. a device is considered completed & successful if agreement is successful
     * two times in a row within a specified time period.
     * </p>
     * @param repeat if true, wait for {@link #TIME_TO_WAIT} seconds
     */
    private void orchestrate(boolean repeat) {
        if (repeat) {
            try {
                TimeUnit.MILLISECONDS.sleep(TIME_TO_WAIT);
            } catch(Exception e) {
                e.printStackTrace();
            }
        }

        List<MessageInterface> messages = incomingMessages.flush();
        if (messages.isEmpty()) {
            if (repeat) {
                ended = true;
                return;
            }
            orchestrate(true);
            return;
        }

        //get the latest messages from each neighbour (in case there are more than 1 messages from the neighbour)
        HashMap<Integer, Message> latestMessages = new HashMap<>();
        for (MessageInterface m: messages) {
            Message message = (Message) m;
            if (latestMessages.get(message.getSenderID()) == null ||
                    latestMessages.get(message.getSenderID()).getTimestamp().isBefore(message.getTimestamp())) {
                latestMessages.put(message.getSenderID(), message);
            }
        }

        boolean agreementSuccess = true;
        //must agree with all incoming messages for the agreement to succeed
        for (Message message: latestMessages.values()) {
            if (!globalData.agreement(message)) {
                agreementSuccess = false;
                break;
            }
        }


        if (agreementSuccess) {
            if (repeat) { ended = true; }
            else { orchestrate(true); }
            return;
        }

        //agreement failed, update the new info from messages received
        for (Message message: latestMessages.values()) {
            globalData.update(message.getData());
        }

        HashMap<Server, Election> electionResults = globalData.election();
        while(outvoted(electionResults)) {
            updateMaxBidRatio(electionResults);
            if (!assignments.embedding(getResidualResourcesFromElection(electionResults))) {
                ended = true;
                failed = true;
                return;
            }

            voting();
            electionResults = globalData.election();
        }

        broadcast(new Message(globalData, getIndex(), LocalTime.now()));
    }

    @Override
    protected void initialize() {
        if (!assignments.embedding(getResourceAvailableInServers())) {
            LOGGER.info("{}: {}: Could not find suitable embedding at initialization.", getSimulation().clockStr(), getName());
            failed = true;
            ended = true;
        } else {
            voting();
            globalData.election();
            broadcast(new Message(globalData, getIndex(), LocalTime.now()));
            try{
                //wait a short amount of time for the respective
                TimeUnit.MILLISECONDS.sleep(TIME_TO_WAIT);
            } catch(Exception e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    protected void postProcessing() {
        //no need for any postprocessing here
    }

    @Override
    public void printResults() {
        LOGGER.info("Device (index = {}): total run time = {}, is_winner = {}", getIndex(), getRuntime(), !failed);
        HashMap<Server, ResourceBundle> resourceConsumption = getFinalResourcesConsumption();
        for (Server e: resourceConsumption.keySet()) {
            ResourceBundle consumption = resourceConsumption.get(e);
            LOGGER.info("{}: {} on {}", getName(), consumption, e.getName());
        }
    }

    @Override
    public HashMap<Server, ResourceBundle> getFinalResourcesConsumption() {
        HashMap<Server, ResourceBundle> result = new HashMap<>();
        for (Server e: getEdgeServers()) {
            result.put(e, new ResourceBundle(0,0,0));
        }

        for (TaskAssignment assignment: assignments.assignmentList) {
            if (assignment == null) {
                continue;
            }
            result.get(assignment.getServer()).addResources(vmHandler.getVmResourceUsage(assignment.getVirtualMachineID()));
        }

        return result;
    }

    @Override
    public void reset() {
        super.reset();
        setIndex(-1);
        neighbours.clear();
        incomingMessages.flush(); //could be from previous distributed simulation run, thus clear all
//        failed = false;
//        ended = false;
        assignments.clear();
        globalData = new GlobalData(this, getDistSimManager().getNumParticipatingApplications());
        getEdgeServers().forEach(e -> maxBidRatio.put(e, Double.MAX_VALUE));
    }

    /**
     * For each edge server found in the simulation, get the resource available.
     * @return each edge server mapped to their available resource
     *
     * @see Server#getAvailableResources()
     */
    protected HashMap<Server, ResourceBundle> getResourceAvailableInServers() {
        HashMap<Server, ResourceBundle> result = new HashMap<>();
        for (Server e: getEdgeServers()) {
            result.put(e, e.getAvailableResources());
        }
        return result;
    }

    /**
     * Based on the current assignment, generate a value for the vote for each server.
     * Then update this vote, resource demanded and current time into globalData.
     *
     * <p>See "score" function in the DRAGON paper to see how vote is calculated.</p>
     */
    private void voting() {
        for (Server e: getEdgeServers()) {
            int totalPrivateUtility = 0;
            ResourceBundle totalResourceDemanded = new ResourceBundle(0, 0, 0);

            for (TaskAssignment t: assignments.assignmentList) {
                if (t.getServer() != e) { continue; }
                totalPrivateUtility += t.getPrivateUtility();
                totalResourceDemanded.addResources(vmHandler.getVmResourceUsage(t.getVirtualMachineID()));
            }

            if (totalPrivateUtility == 0) { continue; } //the device does not have any assignment on this server.

            //total resource demanded is a 3-dimensional vector, we have to normalise it to get a number
            double normalisedResource = totalResourceDemanded.normalise(e.getAvailableResources());

            double ratio = totalPrivateUtility/normalisedResource;
            double maxBidRatioForServer = maxBidRatio.get(e);

            //see "score" function for more information
            int vote = ratio < maxBidRatioForServer ? totalPrivateUtility : (int) (normalisedResource * maxBidRatioForServer);

            //update globalData for its own information
            globalData.updateVoteForServer(vote, e);
            globalData.updateResourceForServer(totalResourceDemanded, e);
        }
    }

    /**
     * @param electionResults each edge server mapped to their election result
     * @return true if this edge device did not win in any election
     */
    private boolean outvoted(HashMap<Server, Election> electionResults) {
        for (Election result: electionResults.values()) {
            if (result.getWinners().contains(getIndex())) { return false; }
        }
        return true;
    }

    /**
     * Each election result has a {@link Election#getResidualResources() residual resources}, which is
     * the available resources on the server minus the resource consumption of the winners of that election.
     *
     * <p>
     *     The residual resources is then used in the {@link AssignmentVector#embedding(HashMap) embedding} algorithm
     *     to find an assignment that does not exceed the available resource on the server, taking into consideration the resources used
     *     by the current winners.
     * </p>
     * @param electionResults each server mapped to their election result
     * @return each server mapped to their election result's residual resources
     */
    private HashMap<Server, ResourceBundle> getResidualResourcesFromElection(HashMap<Server, Election> electionResults) {
        HashMap<Server, ResourceBundle> result = new HashMap<>();
        for (Server e: electionResults.keySet()) {
            result.put(e, electionResults.get(e).getResidualResources());
        }

        return result;
    }

    /**
     * {@link #maxBidRatio} is used to bound the vote in {@link #voting()}.
     *
     * We have to update it everytime we run an election so that we do not give a higher vote than before.
     * <p>See "score" function in the DRAGON paper.</p>*/
    private void updateMaxBidRatio(HashMap<Server, Election> electionResults) {
        for (Server server : getEdgeServers()) {
            double smallestRatio = Double.MAX_VALUE;
            Election electionResult = electionResults.get(server);
            for (int winner: electionResult.getWinners()) {
                double candidateSmallestRatio = globalData.getEdgeDeviceInformationForServer(winner, server).getVoteResourceRatio(server.getAvailableResources());
                smallestRatio = Math.min(smallestRatio, candidateSmallestRatio);
            }

            if (smallestRatio < Double.MAX_VALUE) {
                maxBidRatio.put(server, smallestRatio);
            }
        }
    }
}
