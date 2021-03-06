package fazirul.fyp.dragon.dragonDevice;

import fazirul.fyp.dragon.utils.EdgeDeviceInformation;
import fazirul.fyp.dragon.utils.Election;
import fazirul.fyp.dragon.utils.Message;
import fazirul.fyp.elements.Server;
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
    private final HashMap<Server, List<EdgeDeviceInformation>> data = new HashMap<>();

    /**
     * The edge device that this GlobalData belongs to.
     */
    private final EdgeDeviceDragon edgeDevice;


    /**
     * The winners of the most recently computed election.
     */
    private final HashMap<Server, HashSet<Integer>> electionWinners = new HashMap<>();

    /**
     * @param edgeDevice the edge device that is maintaining such information
     * @param sizeOfNetwork the total number of edge devices participating in the distributed algorithm.
     */
    public GlobalData(EdgeDeviceDragon edgeDevice, int sizeOfNetwork) {
        this.edgeDevice = edgeDevice;
        HashSet<Server> servers = edgeDevice.getEdgeServers();
        for (Server e: servers) {
            List<EdgeDeviceInformation> deviceInformations = new ArrayList<>();
            for (int i = 0; i < sizeOfNetwork; i++) {
                deviceInformations.add(new EdgeDeviceInformation(i));
            }
            data.put(e, deviceInformations);

            electionWinners.put(e, new HashSet<>()); // initialize election winners to be null.
        }
    }

    /**
     * Performs the multi-node election routine as specified in the DRAGON paper.
     * @return Election results in each edge server
     */
    protected HashMap<Server, Election> election() {
        HashMap<Server, Election> results = election(new HashSet<>());
        for (Server e: results.keySet()) {
            electionWinners.put(e, new HashSet<>(results.get(e).getWinners())); //update the winners
        }

        return results;
    }

    /**
     * The main logic behind the election routine. See DRAGON paper for more information.
     *
     * <p>It is a recursive procedure, but can also be implemented iteratively.</p>
     * @param blacklistedDevices the devices that we don't take into consideration to resolve election-conflicts.
     * @return Election results in each edge server
     */
    private HashMap<Server, Election> election(HashSet<Integer> blacklistedDevices) {
        HashMap<Server, Election> electionResults = new HashMap<>();

        //perform election for all servers
        for (Server e: edgeDevice.getEdgeServers()) {
            electionResults.put(e, singleServerElection(e, blacklistedDevices));
        }

        int nextFalseWinner = computeNextFalseWinner(electionResults);
        //base case: when there are no more false winners
        if (nextFalseWinner == -1) {
            //release all the votes held by the losers
            for (Server server : electionResults.keySet()) {
                HashSet<Integer> losers = electionResults.get(server).getLosers();
                for (int loser: losers) {
                    EdgeDeviceInformation loserInfo = data.get(server).get(loser);
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
    private Election singleServerElection(Server e, HashSet<Integer> blacklistedDevices) {
        Election electionResult = new Election();
        electionResult.addToResidualResources(e.getAvailableResources()); //eventually, the residual resources will decrease as we add winners

        List<EdgeDeviceInformation> sortedEdgeDeviceInformation = new ArrayList<>(data.get(e)); //first create a copy as we do not want the original list to be modified
        sortedEdgeDeviceInformation.sort(Comparator.comparingDouble(device ->
                -device.getVoteResourceRatio(e.getAvailableResources()))); //then sort based on vote:resource ratio

        for (EdgeDeviceInformation edi: sortedEdgeDeviceInformation) {
            if (edi.getVote() == 0) { break; }
            if (blacklistedDevices.contains(edi.getEdgeDeviceID()) ||
                    !electionResult.getResidualResources().isBounded(edi.getResource())) { continue; }

            //device is not blacklisted & there is enough resources remaining --> it's a winner for this node
            electionResult.removeFromResidualResources(edi.getResource());
            electionResult.addWinner(edi.getEdgeDeviceID());
        }

        //losers are those that voted but did not win
        for (EdgeDeviceInformation edi: sortedEdgeDeviceInformation) {
            if (edi.getVote() == 0) { break; }
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
    private int computeNextFalseWinner(HashMap<Server, Election> electionResults) {
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
            for (Server server : electionResults.keySet()) {
                if (electionResults.get(server).getLosers().contains(falseWinner.id)) { falseWinner.numberOfElectionsLost++; }

                if (data.get(server).get(falseWinner.id).getVote() == 0) { continue; } //if it didn't vote on this server, don't have to update minVoteResourceRatio
                falseWinner.minVoteResourceRatio = Math.min(data.get(server).get(falseWinner.id).getVoteResourceRatio(server.getAvailableResources()),
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
     * @param server the specified edge server
     * @return the edge device information
     */
    protected EdgeDeviceInformation getEdgeDeviceInformationForServer(int edgeDeviceIndex, Server server) {
        return data.get(server).get(edgeDeviceIndex);
    }

    /**
     * Gets the winners of the most-recently ran election. Implementation:
     * <p>
     * Because we know that loser votes are reset to 0 after each {@link #election()},
     * the winners must be those whose votes are more than 0.
     * </p>
     * @return each server mapped to the set of edge devices that won election on the server
     */
    private HashMap<Server, HashSet<Integer>> getWinners() {
        return electionWinners;
    }

    /**
     * Based on the messages received from neighbours, run the agreement algorithm as specified in the DRAGON paper.
     * @return true if consensus is reached
     */
    protected boolean agreement(Message incomingMessage) {
        GlobalData otherData = incomingMessage.getData();
        HashMap<Server, HashSet<Integer>> otherDataWinners = otherData.getWinners();
        HashMap<Server, HashSet<Integer>> currentDataWinners = getWinners();

        //for each edge server, we compare the winners between the two global data
        for (Server server : otherDataWinners.keySet()) {
            HashSet<Integer> other = otherDataWinners.get(server);
            HashSet<Integer> curr = currentDataWinners.get(server);

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
    protected void update(GlobalData other) {
        for (Server server : data.keySet()) {
            for (int i = 0; i < data.get(server).size(); i++) {
                EdgeDeviceInformation currInfo = getEdgeDeviceInformationForServer(i, server);
                EdgeDeviceInformation otherInfo = other.getEdgeDeviceInformationForServer(i, server);

                //curr info is outdated
                if (currInfo.getTimestamp().isBefore(otherInfo.getTimestamp())) {
                    data.get(server).set(i, otherInfo);
                }
            }
        }
    }

    /**
     * Updates the current application's own vote for the server specified.
     * @param vote the new vote
     * @param e the edge server
     */
    protected void updateVoteForServer(int vote, Server e) {
        EdgeDeviceInformation ownInfo = data.get(e).get(edgeDevice.getIndex());
        ownInfo.setVote(vote);
        ownInfo.setTimestamp(LocalTime.now());
    }

    /**
     * Updates the current application's own resource demanded for the server specified.
     * @param resource the resource demanded
     * @param e the edge server
     */
    protected void updateResourceForServer(ResourceBundle resource, Server e) {
        EdgeDeviceInformation ownInfo = data.get(e).get(edgeDevice.getIndex());
        ownInfo.setResource(resource);
    }

    public GlobalData clone() {
        GlobalData copy = null;
        for (List<EdgeDeviceInformation> list: data.values()) {
            copy = new GlobalData(edgeDevice, list.size());
            break;
        }

        if (copy == null) {
            System.out.println("ERROR: Could not create a copy of GlobalData.");
            return null;
        }

        copy.data.forEach((k, v) -> {
            for (int i = 0; i < v.size(); i++) {
                EdgeDeviceInformation original = data.get(k).get(i);
                EdgeDeviceInformation duplicate = v.get(i);

                duplicate.setResource(original.getResource().clone());
                duplicate.setVote(original.getVote());
                duplicate.setTimestamp(original.getTimestamp());
            }
        });

        copy.electionWinners.forEach((k, v) -> v.addAll(electionWinners.get(k)) );
        return copy;
    }
}
