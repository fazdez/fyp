package fazirul.fyp.dragon.election;

import fazirul.fyp.elements.Node;
import fazirul.fyp.elements.ResourceBundle;

import java.time.LocalTime;
import java.util.*;

// data to exchange
public class GlobalData {
    private final HashMap<Node, PerNodeGlobalData> data = new HashMap<>();

    public GlobalData(HashSet<Node> nodes, int numOfApplications) {
        for (Node n: nodes) {
            data.put(n, new PerNodeGlobalData(numOfApplications));
        }
    }

    public HashMap<Node, PerNodeElection> election(HashMap<Node, ResourceBundle> nodeResourceBundleHashMap, HashSet<Integer> blacklistedApplications) {
        HashMap<Node, PerNodeElection> electionResults = new HashMap<>();

        for (Node n: data.keySet()) {
            PerNodeGlobalData data_per_node = data.get(n);
            ResourceBundle node_residual_resources = nodeResourceBundleHashMap.get(n);

            electionResults.put(n, new PerNodeElection(data_per_node, node_residual_resources, blacklistedApplications));
        }

        return electionResults;
    }

    public GlobalData clone() {
        GlobalData clone = new GlobalData(new HashSet<>(this.data.keySet()), 0);
        for (Node n: data.keySet()) {
            clone.data.put(n, data.get(n).clone());
        }

        return clone;
    }

    protected List<Integer> getApplicationsSortedByMaxBids() {
        HashMap<Integer, Integer> applicationsMaxBids = getMaxBids();

        List<Integer> result = new ArrayList<>(applicationsMaxBids.keySet());
        result.sort(Comparator.comparingInt(applicationsMaxBids::get).reversed());
        return result;
    }

    protected HashMap<Integer, Integer> getMaxBids() {
        HashMap<Integer, Integer> applicationsMaxBids = new HashMap<>();
        for (Node n: data.keySet()) {
            for (int application = 0; application < data.get(n).votes.size(); application++) {
                if (!applicationsMaxBids.containsValue(application) || applicationsMaxBids.get(application) < data.get(n).getApplicationVote(application)) {
                    applicationsMaxBids.put(application, data.get(n).getApplicationVote(application));
                }
            }
        }

        return applicationsMaxBids;
    }

    protected void resetVotes(HashMap<Node, PerNodeElection> electionResults) {
        for (Node n: electionResults.keySet()) {
            data.get(n).resetLoserVotes(electionResults.get(n).getWinners());
        }
    }

    public void updatePerNodeData(Node n, int app, int vote, ResourceBundle resource, LocalTime timestamp) {
        data.get(n).updateData(app, vote, resource, timestamp);
    }

    public double getVoteResourceRatioForApplication(Node n, int application) {
        return data.get(n).getVoteResourceRatioForApplication(application, n.getAvailableResources());
    }

    public PerNodeGlobalData getPerNodeGlobalData(Node n) {
        return data.get(n);
    }

    public void updateData(GlobalData otherData, int application) {
        for (Node n: data.keySet()) {
            data.get(n).updateData(otherData.getPerNodeGlobalData(n), application);
        }
    }
}

