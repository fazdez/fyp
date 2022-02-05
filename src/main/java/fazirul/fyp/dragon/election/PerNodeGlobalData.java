package fazirul.fyp.dragon.election;

import fazirul.fyp.elements.ResourceBundle;
import org.apache.commons.math3.util.Pair;

import java.time.LocalTime;
import java.util.*;


public class PerNodeGlobalData {
    protected List<Integer> votes;
    protected List<ResourceBundle> resources;
    protected List<LocalTime> timestamps;

    public PerNodeGlobalData(int totalNumOfApplications) {
        this.votes = new ArrayList<>(Collections.nCopies(totalNumOfApplications, 0));
        this.resources = new ArrayList<>(Collections.nCopies(totalNumOfApplications,null));
        this.timestamps = new ArrayList<>(Collections.nCopies(totalNumOfApplications, LocalTime.now()));
        for (int i = 0; i < totalNumOfApplications; i++) {
            this.resources.set(i, new ResourceBundle(0,0,0));
        }
    }

    // returns a list of (ratio, application) pair, sorted in decreasing order by ratio
    private List<Pair<Double, Integer>> getVoteResourceRatio(ResourceBundle availableResources) {
        List<Pair<Double, Integer>> result = new ArrayList<>();

        for (int application = 0; application < this.votes.size(); application++) {
            double ratio = this.getVoteResourceRatioForApplication(application, availableResources);
            if (ratio > 0) {
                result.add(new Pair<>(ratio, application));
            }
        }

        result.sort(Comparator.comparingDouble(Pair::getFirst));
        return result;
    }

    public List<Integer> getApplicationsSortedByRatio(ResourceBundle availableResources) {
        List<Pair<Double, Integer>> voteResourceRatioList = this.getVoteResourceRatio(availableResources);
        List<Integer> result = new ArrayList<>();

        for(Pair<Double, Integer> element: voteResourceRatioList) {
            result.add(element.getSecond());
        }

        return result;
    }

    public List<Integer> getApplicationsSortedByVote() {
        List<Integer> result = new ArrayList<>();

        for (int application = 0; application < this.votes.size(); application++) {
            result.add(application);
        }

        result.sort(Comparator.comparingInt(this::getApplicationVote));
        return result;
    }

    public ResourceBundle getResourceDemandedByApplication(int application) {
        return this.resources.get(application);
    }

    public int getApplicationVote(int application) { return this.votes.get(application); }

    //those who are not winners (i.e. losers) are reset
    public void resetLoserVotes(HashSet<Integer> winners) {
        for (int i = 0; i < votes.size(); i++) {
            if (!winners.contains(i)) {
                votes.set(i, 0);
                resources.set(i, new ResourceBundle(0,0,0));
            }
        }
    }

    public void updateData(int application, int vote, ResourceBundle resource, LocalTime timestamp) {
        this.votes.set(application, vote);
        this.resources.set(application, resource);
        this.timestamps.set(application, timestamp);
    }

    //resource scalar is calculated using the available node resources
    public double getVoteResourceRatioForApplication(int application, ResourceBundle nodeResources) {
        int vote = this.getApplicationVote(application);
        ResourceBundle resources = this.getResourceDemandedByApplication(application);

        return (vote/resources.normalise(nodeResources));
    }

    public void updateData(PerNodeGlobalData incoming, int application) {
        for (int idx = 0; idx < this.votes.size(); idx++) {
            if (idx == application) { continue; }
            if (incoming.timestamps.get(idx).isAfter(this.timestamps.get(idx)) || this.votes.get(idx) == 0) {
                this.votes.set(idx, incoming.votes.get(idx));
                this.resources.set(idx, incoming.resources.get(idx));
                this.timestamps.set(idx, incoming.timestamps.get(idx));
            }
        }
    }

    // return those applications who voted but not in winner set
    public HashSet<Integer> getLosers(HashSet<Integer> winners) {
        HashSet<Integer> losers = new HashSet<>();
        for (int application = 0; application < votes.size(); application++) {
            if (!winners.contains(application) && votes.get(application) > 0) {
                losers.add(application);
            }
        }
        return losers;
    }

    public PerNodeGlobalData clone() {
        PerNodeGlobalData clone = new PerNodeGlobalData(0);
        clone.votes = new ArrayList<>(this.votes);
        clone.resources = new ArrayList<>(this.resources);
        clone.timestamps = new ArrayList<>(this.timestamps);

        return clone;
    }
}
