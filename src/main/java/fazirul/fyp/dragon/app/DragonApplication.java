package fazirul.fyp.dragon.app;

import fazirul.fyp.dragon.election.Election;
import fazirul.fyp.dragon.election.GlobalData;
import fazirul.fyp.dragon.election.PerNodeElection;
import fazirul.fyp.dragon.utils.Message;
import fazirul.fyp.elements.DistributedApplication;
import fazirul.fyp.elements.MessageInterface;
import fazirul.fyp.elements.Node;
import fazirul.fyp.elements.ResourceBundle;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.cloudbus.cloudsim.cloudlets.Cloudlet;

import java.text.MessageFormat;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class DragonApplication extends DistributedApplication {
    private final Logger logger = LogManager.getLogger(DragonApplication.class);
    private GlobalData data;
    private Assignment assignment;
    private boolean initialized = false;
    private boolean lost = false;
    private HashMap<Node, Double> maxBidRatios = new HashMap<>();
    private final HashSet<Node> nodes;

    private final int id;

    public DragonApplication(int id, HashSet<Node> nodes, int numOfApplications, HashSet<Cloudlet> services) {
        this.data = new GlobalData(nodes, numOfApplications);
        this.assignment = new Assignment(services, nodes);
        this.nodes = nodes;
        this.id = id;
        for (Node n: nodes) {
            maxBidRatios.put(n, Double.MAX_VALUE);
        }
    }

    public Set<Cloudlet> getServices() {
        return assignment.getServices();
    }

    public void printResults() {
        String message = MessageFormat.format("""
                application ID: {0, number, integer} || lost = {1}
                assignment = {2}\n""", id, lost, assignment.toString());

        System.out.println(message);
    }

    @Override
    protected void orchestrate() {
        if (!initialized) {
            if(!embedding(getNodeResources())) {
                logger.error("could not find suitable embedding before initialization. ending...");
                ended = true;
                lost = true;
                return;
            }
            voting();
            Election result = new Election(data, getNodeResources());
            broadcast(new Message(data, id, LocalTime.now(), result.getWinnersPerNode()));
            return;
        }

        List<MessageInterface> messages = incomingMessages.flush();
        if (messages.isEmpty() && !initialized) {
            //make sure to indicate that orchestration has ended
            ended = true;
            return;
        }

        Election result = new Election(data, getNodeResources());
        if (agreement(result.getElectionResults(), messages)) {
            try{
                sleep(1500); //double confirm by waiting a short amount of time in case new messages come in
            } catch (Exception e) {
                e.printStackTrace();
            }

            messages = incomingMessages.flush();
            if (agreement(result.getElectionResults(), messages)) {
                ended = true;
                return;
            }
        }

        //agreement failed, thus need to run election again with updated result
        result = new Election(data, getNodeResources());
        while(outvoted(result)) {
            updateMaxRatio(result.getElectionResults());
            this.assignment.reset();
            if (!embedding(result.getResidualResources())) {
                //shutdown, no way to win
                ended = true;
                lost = true;
                break;
            }

            voting();
            result = new Election(data, getNodeResources());
        }
        broadcast(new Message(data, id, LocalTime.now(), result.getWinnersPerNode()));
    }

    @Override
    protected void initialize() {
        orchestrate();
        try {
            sleep(1000); //wait in initialize in case other threads have not started
        } catch (Exception e) {
            e.printStackTrace();
        }
        initialized = true;
        logger.debug(MessageFormat.format("application {0} initialized with services: \n {1}\n", id, assignment.getCloudletsInformation()));
    }

    protected void postProcessing() {
        if (!lost) {
            assignment.offload();
        }
    }

    @Override
    public HashMap<Node, ResourceBundle> getFinalResourcesConsumption() {
        HashMap<Node, ResourceBundle> result = new HashMap<>();
        for (Node n: nodes) {
            result.put(n, assignment.getResourceUsage(n));
        }

        return result;
    }

    private boolean agreement(HashMap<Node, PerNodeElection> result, List<MessageInterface> messages) {
        boolean agreed = true;
        for (MessageInterface m: messages) {
            for (Node n: result.keySet()) {
                if (!((Message)m).getWinnersForNode(n).equals(result.get(n).getWinners())) {
                    agreed = false;
                    break;
                }
            }

            if (!agreed) { break; }
        }

        if (!agreed) {
            for (MessageInterface m: messages) {
                this.data.updateData(((Message)m).getData(), this.id);
            }
        }

        return agreed;
    }

    private boolean embedding(HashMap<Node, ResourceBundle> residualResources) {
        if (this.assignment.completed()) { return true; } //base case
        List<ServiceAssignment> nextAssignments = this.assignment.getNextBestAssignments();
        int oldNumIncompleteServices = this.assignment.numIncompleteServices();
        for (ServiceAssignment serviceAssignment: nextAssignments) {
            ResourceBundle resourcesUsed = serviceAssignment.getResourcesUsed();

            // only those within bounds
            if (residualResources.get(serviceAssignment.getNode()).isBounded(resourcesUsed)) {
                this.assignment.addAssignment(serviceAssignment);
                residualResources.get(serviceAssignment.getNode()).deductResources(resourcesUsed);
                if (this.embedding(residualResources)) {
                    return true; //recursive
                } else {
                    //undo and try next assignment
                    this.assignment.removeAssignment(serviceAssignment.getService());
                    residualResources.get(serviceAssignment.getNode()).addResources(resourcesUsed); //i.e. undoing the deductResources
                }
            }
        }

        // false means it has exhausted through the for loop and found no assignments that could satisfy the remaining resources
        return oldNumIncompleteServices != this.assignment.numIncompleteServices(); // if false, go back to previous recursive iteration and try next one
    }

    private void voting() {
        for (Node n: nodes) {
            LocalTime timestamp = LocalTime.now();
            ResourceBundle resourceDemanded = this.assignment.getResourceUsage(n);
            int vote = getVote(n);

            this.data.updatePerNodeData(n, this.id, vote, resourceDemanded, timestamp);
        }
    }

    private int getVote(Node n) {
        int vote = this.assignment.getTotalPrivateUtility(n);
        double normalisedDemandedResource = this.assignment.getResourceUsage(n).normalise(n.getAvailableResources());
        double ratio = (vote/normalisedDemandedResource);
        if (ratio > maxBidRatios.get(n)) {
            return (int) (normalisedDemandedResource*maxBidRatios.get(n));
        }
        //if it doesn't exceed the ratio-limit, then just return the original vote (its private utility)
        return vote;
    }

    private boolean outvoted(Election election) {
        return election.getLosers().contains(id);
    }

    private HashMap<Node, ResourceBundle> getNodeResources() {
        HashMap<Node, ResourceBundle> result = new HashMap<>();
        for (Node n: nodes) {
            result.put(n, n.getAvailableResources());
        }
        return result;
    }

    // to ensure bounds. See: "score function" in the dragon paper.
    private void updateMaxRatio(HashMap<Node, PerNodeElection> electionResults) {
        for (Node n: electionResults.keySet()) {
            updateMaxRatioForNode(n, electionResults.get(n).getWinners());
        }
    }

    private void updateMaxRatioForNode(Node node, HashSet<Integer> nodeWinners) {
        double smallestRatio = Integer.MAX_VALUE;
        for (Integer application: nodeWinners) {
            double applicationRatio = this.data.getVoteResourceRatioForApplication(node, application);
            if (applicationRatio < smallestRatio) {
                smallestRatio = applicationRatio;
            }
        }

        if (!nodeWinners.contains(this.id) || smallestRatio < this.data.getVoteResourceRatioForApplication(node, this.id)){
            smallestRatio -= Double.MIN_NORMAL;
        }

        this.maxBidRatios.put(node, Math.min(this.maxBidRatios.get(node), smallestRatio));
    }
}
