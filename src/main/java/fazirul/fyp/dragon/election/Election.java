package fazirul.fyp.dragon.election;

import fazirul.fyp.elements.Node;
import fazirul.fyp.elements.ResourceBundle;

import java.util.*;

public class Election {
    private final HashSet<Integer> blacklistedApplications = new HashSet<>(); //essentially the losers
    private final GlobalData data;
    private final HashMap<Node, ResourceBundle> nodeResourceBundleHashMap;
    private HashMap<Node, PerNodeElection> electionResults;
    private HashMap<Node, ResourceBundle> residualResources;
    private HashMap<Node, HashSet<Integer>> winnersPerNode;

    public Election(GlobalData data, HashMap<Node, ResourceBundle> nodeResourceBundleHashMap) {
        this.nodeResourceBundleHashMap = nodeResourceBundleHashMap;
        this.data = data;
        electionResults = this.election();
        residualResources = getResidualResources(electionResults);
        winnersPerNode = getWinnersPerNode(electionResults);
        this.data.resetVotes(electionResults);
    }

    public HashSet<Integer> getLosers() {
        return blacklistedApplications;
    }

    public HashMap<Node, PerNodeElection> getElectionResults() {
        return electionResults;
    }

    public HashMap<Node, HashSet<Integer>> getWinnersPerNode() {
        return winnersPerNode;
    }

    public HashMap<Node, ResourceBundle> getResidualResources() {
        return residualResources;
    }

    private HashMap<Node, PerNodeElection> election() {
        HashMap<Node, PerNodeElection> electionResults = data.election(nodeResourceBundleHashMap, blacklistedApplications);
        HashSet<Integer> falseWinners = computeFalseWinners(electionResults);

        // recursively perform election with new blacklisted applications until there are no false winners (i.e. no election conflict)
        if (falseWinners.size() > 0) {
            blacklistedApplications.addAll(falseWinners);
            return election();
        }

        return electionResults;
    }

    private HashSet<Integer> computeFalseWinners(HashMap<Node, PerNodeElection> electionResults) {
        HashMap<Integer, List<Node>> losers = getLosers(electionResults);
        HashSet<Integer> winners = getWinners(electionResults);
        HashSet<Integer> falseWinners = new HashSet<>();
        List<Integer> sortedApplications = data.getApplicationsSortedByMaxBids();

        for (int app: sortedApplications) {
            if (!winners.contains(app)) { continue; } // if the app was not even a winner in any node i.e. a loser in all nodes, skip.

            if (losers.containsKey(app)) {
                HashSet<Integer> possibleFakes = new HashSet<>();
                for (Node n: losers.get(app)) {
                    PossibleFalseWinners result = findPossibleFalseWinnersForNode(data.getMaxBids(), electionResults, app, n, losers, falseWinners, new HashSet<>());
                    possibleFakes.addAll(result.possibles);

                    // no possible fake found for this node, means there's no way for this app to win any in this node.
                    if (!result.found) {
                        falseWinners.add(app);
                        break;
                    }
                }

                // check all the possibleFakes to see whether they are really fake, and add to falseWinners
                for (int fake: possibleFakes) {
                    for (Node n: losers.get(fake)) {
                        // for each node that the possible fake has lost, if you minus off all the known fakes, and it still lost, means it's a fake
                        HashSet<Integer> winnersForNode = new HashSet<>(electionResults.get(n).getWinners());
                        winnersForNode.removeAll(falseWinners);
                        if (!winnersForNode.isEmpty()) {
                            falseWinners.add(fake);
                            break;
                        }
                    }
                }
            }
        }
        return falseWinners;
    }

    //recursively finds, for each winner of this node, whether this winner has lost in other nodes. if yes, then it is a possible false winner. (otherwise, it is a true winner)
    private PossibleFalseWinners findPossibleFalseWinnersForNode(HashMap<Integer, Integer> maxBids, HashMap<Node, PerNodeElection> electionResults, int application, Node node,
                                                                 HashMap<Integer, List<Node>> losers, HashSet<Integer> falseWinners, HashSet<Integer> ignored) {

        PossibleFalseWinners result = new PossibleFalseWinners();
        List<Integer> winnersForNode = new ArrayList<>(electionResults.get(node).getWinners());
        winnersForNode.sort(Comparator.comparingInt(maxBids::get)); // we start with the winner with the smallest bid.

        // check each winner for that node whether they are true or fake winners
        for (int winner: winnersForNode) {
            if (falseWinners.contains(winner)) { // it has already been confirmed this is a fake
                result.found = true;
                return result;
            }

            //if not ignored & is a loser --> means it is a possible fakeWinner
            if (!ignored.contains(winner) && losers.containsKey(winner)) {
                for (Node n: losers.get(winner)) {
                    // for each other node that this "winner" lost, assume it won and see whether any other false winners can be found.
                    PossibleFalseWinners otherFalseWinners = findPossibleFalseWinnersForNode(maxBids, electionResults, winner, n, losers,
                            union(falseWinners, result.possibles), union(ignored, application));

                    if (!otherFalseWinners.found) { //if no other false winners found, itself is the false winner.
                        result.found = true;
                        return result; // we can return
                    }

                    result.possibles.addAll(otherFalseWinners.possibles);
                }
            }
        }

        return result;
    }

    //only for use in findPossibleFalseWinnersForNode
    private class PossibleFalseWinners {
        HashSet<Integer> possibles = new HashSet<>();
        boolean found = false;
    }

    private HashMap<Integer, List<Node>> getLosers(HashMap<Node, PerNodeElection> electionResults) {
        HashMap<Integer, List<Node>> losers = new HashMap<>();
        for (Node n: electionResults.keySet()) {
            for (int loser: electionResults.get(n).getLosers()) {
                if (!losers.containsKey(loser)) {
                    losers.put(loser, new ArrayList<>());
                }
                losers.get(loser).add(n);
            }
        }
        return losers;
    }

    private HashSet<Integer> getWinners(HashMap<Node, PerNodeElection> electionResults) {
        HashSet<Integer> results = new HashSet<>();

        for (Node n: electionResults.keySet()) {
            results.addAll(electionResults.get(n).getWinners());
        }

        return results;
    }

    private HashMap<Node, HashSet<Integer>> getWinnersPerNode(HashMap<Node, PerNodeElection> electionResults) {
        HashMap<Node, HashSet<Integer>> winnersPerNode = new HashMap<>();

        for (Node n: electionResults.keySet()) {
            winnersPerNode.put(n, electionResults.get(n).getWinners());
        }

        return winnersPerNode;
    }

    private HashMap<Node, ResourceBundle> getResidualResources(HashMap<Node, PerNodeElection> electionResults) {
        HashMap<Node, ResourceBundle> residualResources = new HashMap<>();
        for (Node n: electionResults.keySet()) {
            residualResources.put(n, electionResults.get(n).getResidualResources());
        }

        return residualResources;
    }

    //returns new hashset
    private HashSet<Integer> union(HashSet<Integer> h1, HashSet<Integer> h2) {
        HashSet<Integer> result = new HashSet<>(h1);
        result.addAll(h2);
        return result;
    }

    private HashSet<Integer> union(HashSet<Integer> h1, int h2) {
        HashSet<Integer> result = new HashSet<>(h1);
        result.add(h2);
        return result;
    }
}
