package fazirul.fyp.dragon_implementation.utils;

import fazirul.fyp.elements.ResourceBundle;

import java.util.HashSet;

public class Election {
    private final HashSet<Integer> losers;
    private final HashSet<Integer> winners;
    private final ResourceBundle residualResources;

    public Election() {
        losers = new HashSet<>();
        winners = new HashSet<>();
        residualResources = new ResourceBundle(0, 0, 0);
    }

    public Election(HashSet<Integer> losers, HashSet<Integer> winners, ResourceBundle residualResources) {
        this.losers = losers;
        this.winners = winners;
        this.residualResources = residualResources;
    }

    public HashSet<Integer> getLosers() {
        return losers;
    }

    public HashSet<Integer> getWinners() {
        return winners;
    }

    public ResourceBundle getResidualResources() {
        return residualResources;
    }

    public void addLoser(int loserID) {
        losers.add(loserID);
    }

    public void addWinner(int winnerID) {
        winners.add(winnerID);
    }

    public void addToResidualResources(ResourceBundle toAdd) {
        residualResources.addResources(toAdd);
    }

    public void removeLoser(int loserID) {
        losers.remove(loserID);
    }

    public void removeWinner(int winnerID) {
        winners.remove(winnerID);
    }

    public void removeFromResidualResources(ResourceBundle toRemove) {
        residualResources.deductResources(toRemove);
    }
}
