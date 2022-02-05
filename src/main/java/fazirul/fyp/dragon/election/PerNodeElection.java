package fazirul.fyp.dragon.election;


import fazirul.fyp.elements.ResourceBundle;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

public class PerNodeElection {
    private final HashSet<Integer> losers;
    private final HashSet<Integer> winners;
    private final ResourceBundle residualResources;

    public PerNodeElection(PerNodeGlobalData data, ResourceBundle resources, HashSet<Integer> blacklistedApplications) {
        ResourceBundle nodeResidualResources = resources.clone(); // use temp instead of modifying the original
        HashSet<Integer> winners = new HashSet<>();
        List<Integer> sortedApplications = data.getApplicationsSortedByRatio(nodeResidualResources);

        for (int application: sortedApplications) {
            if (nodeResidualResources.isEmpty()) { break; }
            if (blacklistedApplications.contains(application)) { continue; } //skip those blacklisted (false winners)

            ResourceBundle resourceDemanded = data.getResourceDemandedByApplication(application);
            if (nodeResidualResources.isBounded(resourceDemanded)) {
                nodeResidualResources.deductResources(resourceDemanded);
                winners.add(application);
            } else {
                break;
            }
        }

        this.residualResources = nodeResidualResources;
        this.winners = winners;
        this.losers = data.getLosers(winners);
    }

    public ResourceBundle getResidualResources() {
        return this.residualResources;
    }

    public HashSet<Integer> getWinners() {
        return this.winners;
    }

    public HashSet<Integer> getLosers() { return this.losers; }
}
