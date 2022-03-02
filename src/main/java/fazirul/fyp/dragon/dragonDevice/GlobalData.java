package fazirul.fyp.dragon.dragonDevice;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import fazirul.fyp.dragon.utils.EdgeDeviceInformation;
import fazirul.fyp.dragon.utils.Election;
import fazirul.fyp.dragon.utils.Message;
import fazirul.fyp.elements.EdgeServer;
import fazirul.fyp.elements.ResourceBundle;

import java.time.LocalTime;
import java.util.*;

/**
 * The data that an edge device maintains on other edge devices. There is one instance such data for each edge device.
 */
public class GlobalData {
    /**
     * For each edge server, we maintain a list of {@link EdgeDeviceInformation information} on other edge devices (i.e. vote, resource, voting time).
     * Since each edge device has an assigned index, we can grab the information of a particular edge device using this index.
     *
     * @see EdgeDeviceDragon#getIndex()
     */
    private final HashMap<EdgeServer, List<EdgeDeviceInformation>> data = new HashMap<>();

    private final EdgeDeviceDragon edgeDevice;

    /**
     * @param edgeDevice the edge device that is maintaining such information
     * @param sizeOfNetwork the total number of edge devices participating in the distributed algorithm.
     */
    public GlobalData(EdgeDeviceDragon edgeDevice, int sizeOfNetwork) {
        this.edgeDevice = edgeDevice;
        HashSet<EdgeServer> edgeServers = edgeDevice.getEdgeServers();
        for (EdgeServer e: edgeServers) {
            List<EdgeDeviceInformation> deviceInformations = new ArrayList<>();
            for (int i = 0; i < sizeOfNetwork; i++) {
                deviceInformations.add(new EdgeDeviceInformation(i));
            }
            data.put(e, deviceInformations);
        }
    }

    /**
     * Performs the multi-node election routine as specified in the DRAGON paper.
     * @return Election results in each edge server
     */
    protected HashMap<EdgeServer, Election> election() {
        return election(new HashSet<>());
    }

    /**
     * The main logic behind the election routine. See DRAGON paper for more information.
     *
     * <p>It is a recursive procedure, but can also be implemented iteratively.</p>
     * @param blacklistedDevices the devices that we don't take into consideration to resolve election-conflicts.
     * @return Election results in each edge server
     */
    private HashMap<EdgeServer, Election> election(HashSet<Integer> blacklistedDevices) {
        HashMap<EdgeServer, Election> electionResults = new HashMap<>();

        //perform election for all servers
        for (EdgeServer e: edgeDevice.getEdgeServers()) {
            electionResults.put(e, singleServerElection(e, blacklistedDevices));
        }

        int nextFalseWinner = computeNextFalseWinner(electionResults);
        //base case: when there are no more false winners
        if (nextFalseWinner == -1) {
            //release all the votes held by the losers
            for (EdgeServer edgeServer: electionResults.keySet()) {
                HashSet<Integer> losers = electionResults.get(edgeServer).getLosers();
                for (int loser: losers) {
                    EdgeDeviceInformation loserInfo = data.get(edgeServer).get(loser);
                    loserInfo.setVote(0);
                    loserInfo.setResource(new ResourceBundle(0,0,0));
                    loserInfo.setTimestamp(LocalTime.now());
                }
            }
            return electionResults;
        }

        blacklistedDevices.add(nextFalseWinner);
        return election(blacklistedDevices);
    }

    /**
     * Performs single-node election on an edge server.
     * @param e the edge server to perform election on
     * @param blacklistedDevices A set of edge devices that will not be considered during election.
     * @return The results of the {@link Election}
     */
    private Election singleServerElection(EdgeServer e, HashSet<Integer> blacklistedDevices) {
        Election electionResult = new Election();
        electionResult.addToResidualResources(e.getAvailableResources()); //eventually, the residual resources will decrease as we add winners

        List<EdgeDeviceInformation> sortedEdgeDeviceInformation = new ArrayList<>(data.get(e)); //first create a copy as we do not want the original list to be modified
        sortedEdgeDeviceInformation.sort(Comparator.comparingDouble(device ->
                -device.getVoteResourceRatio(e.getAvailableResources()))); //then sort based on vote:resource ratio

        for (EdgeDeviceInformation edi: sortedEdgeDeviceInformation) {
            if (blacklistedDevices.contains(edi.getEdgeDeviceID()) ||
                    !electionResult.getResidualResources().isBounded(edi.getResource())) { continue; }

            //device is not blacklisted & there is enough resources remaining --> it's a winner for this node
            electionResult.removeFromResidualResources(edi.getResource());
            electionResult.addWinner(edi.getEdgeDeviceID());
        }

        //losers are those that voted but did not win
        for (EdgeDeviceInformation edi: sortedEdgeDeviceInformation) {
            if (!electionResult.getWinners().contains(edi.getEdgeDeviceID()) && edi.getVote() > 0) {
                electionResult.addLoser(edi.getEdgeDeviceID());
            }
        }

        return electionResult;
    }

    /**
     * Based on the election results of all edge servers, decide on a false winner.
     * A false winner is an edge device that has won elections in some edge servers, but at the same time lost elections in other edge servers.
     *
     * <p>The decision on false winner is as follows:
     * <ul>
     *     <li>Among all the false winners, select the false winner that has the highest number of elections lost.</li>
     *     <li>If there is a tie, select the false winner that has the lowest vote:resource ratio across all edge servers voted on.</li>
     *     <li>If there is a further tie, select any of the candidates to be the next false winner.</li>
     * </ul>
     * </p>
     * @param electionResults the election results
     * @return the index of the false winner
     */
    private int computeNextFalseWinner(HashMap<EdgeServer, Election> electionResults) {
        HashSet<Integer> overallWinners = new HashSet<>();
        HashSet<Integer> overallLosers = new HashSet<>();

        for (Election serverElection: electionResults.values()) {
            overallWinners.addAll(serverElection.getWinners());
            overallLosers.addAll(serverElection.getLosers());
        }

        //possible false winners are those who won some and lost some
        HashSet<Integer> possibleFalseWinners = new HashSet<>(overallWinners);
        possibleFalseWinners.retainAll(overallLosers);

        if (possibleFalseWinners.isEmpty()) { return -1; }


        //now the next false winner we choose to blacklist is the one with
        // 1) highest number of elections lost and if tied:
        // 2) lowest vote:resource ratio across all edge server
        class FalseWinner {
            final int id;
            int numberOfElectionsLost = 0;
            double minVoteResourceRatio = Double.MAX_VALUE;

            FalseWinner(int id) { this.id = id; }
        } //temporary data structure just within this function call

        List<FalseWinner> falseWinnerList = new ArrayList<>();
        for (int falseWinnerID: possibleFalseWinners) {
            falseWinnerList.add(new FalseWinner(falseWinnerID));
        }

        for (FalseWinner falseWinner: falseWinnerList) {
            for (EdgeServer edgeServer: electionResults.keySet()) {
                if (electionResults.get(edgeServer).getLosers().contains(falseWinner.id)) { falseWinner.numberOfElectionsLost++; }

                if (data.get(edgeServer).get(falseWinner.id).getVote() == 0) { continue; } //if it didn't vote on this server, don't have to update minVoteResourceRatio
                falseWinner.minVoteResourceRatio = Math.min(data.get(edgeServer).get(falseWinner.id).getVoteResourceRatio(edgeServer.getAvailableResources()),
                        falseWinner.minVoteResourceRatio);
            }
        }

        Comparator<FalseWinner> comparator = Comparator.comparingInt(f -> -f.numberOfElectionsLost);
        comparator.thenComparingDouble(f -> f.minVoteResourceRatio);

        falseWinnerList.sort(comparator);
        return falseWinnerList.get(0).id;
    }

    /**
     * Returns the {@link EdgeDeviceInformation} for the server specified.
     * @param edgeDeviceIndex the index of the edge device
     * @param edgeServer the specified edge server
     * @return the edge device information
     */
    protected EdgeDeviceInformation getEdgeDeviceInformationForServer(int edgeDeviceIndex, EdgeServer edgeServer) {
        return data.get(edgeServer).get(edgeDeviceIndex);
    }

    /**
     * Gets the winners of the most-recently ran election. Implementation:
     * <p>
     * Because we know that loser votes are reset to 0 after each {@link #election()},
     * the winners must be those whose votes are more than 0.
     * </p>
     * @return each server mapped to the set of edge devices that won election on the server
     */
    private HashMap<EdgeServer, HashSet<Integer>> getWinners() {
        HashMap<EdgeServer, HashSet<Integer>> winnerSet = new HashMap<>();
        for (EdgeServer edgeServer: data.keySet()) {
            winnerSet.put(edgeServer, new HashSet<>());
            List<EdgeDeviceInformation> serverInfo = data.get(edgeServer);
            for (EdgeDeviceInformation edgeDeviceInformation: serverInfo) {
                if (edgeDeviceInformation.getVote() > 0) { winnerSet.get(edgeServer).add(edgeDevice.getIndex()); }
            }
        }

        return winnerSet;
    }

    /**
     * Based on the messages received from neighbours, run the agreement algorithm as specified in the DRAGON paper.
     * @return true if consensus is reached
     */
    protected boolean agreement(Message incomingMessage) {
        GlobalData otherData = incomingMessage.getData();
        HashMap<EdgeServer, HashSet<Integer>> otherDataWinners = otherData.getWinners();
        HashMap<EdgeServer, HashSet<Integer>> currentDataWinners = getWinners();

        //for each edge server, we compare the winners between the two global data
        for (EdgeServer edgeServer: otherDataWinners.keySet()) {
            HashSet<Integer> other = otherDataWinners.get(edgeServer);
            HashSet<Integer> curr = currentDataWinners.get(edgeServer);

            //if the winners are not the same, consensus is not reached. Update the latest information received
            if (!other.equals(curr)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Updates information received from other devices if the timestamp is newer.
     * @param other the other {@link GlobalData} object
     */
    private void update(GlobalData other) {
        for (EdgeServer edgeServer: data.keySet()) {
            for (int i = 0; i < data.get(edgeServer).size(); i++) {
                EdgeDeviceInformation currInfo = getEdgeDeviceInformationForServer(i, edgeServer);
                EdgeDeviceInformation otherInfo = other.getEdgeDeviceInformationForServer(i, edgeServer);

                if (currInfo.getTimestamp().isBefore(otherInfo.getTimestamp())) {
                    currInfo = otherInfo;
                }
            }
        }
    }

    /**
     * Updates the current application's own vote for the server specified.
     * @param vote the new vote
     * @param e the edge server
     */
    protected void updateVoteForServer(int vote, EdgeServer e) {
        EdgeDeviceInformation ownInfo = data.get(e).get(edgeDevice.getIndex());
        ownInfo.setVote(vote);
        ownInfo.setTimestamp(LocalTime.now());
    }

    /**
     * Updates the current application's own resource demanded for the server specified.
     * @param resource the resource demanded
     * @param e the edge server
     */
    protected void updateResourceForServer(ResourceBundle resource, EdgeServer e) {
        EdgeDeviceInformation ownInfo = data.get(e).get(edgeDevice.getIndex());
        ownInfo.setResource(resource);
    }

    public GlobalData clone() {
        Gson gson = new GsonBuilder().create();
        String jsonString = gson.toJson(this);
        return gson.fromJson(jsonString, this.getClass());
    }
}
