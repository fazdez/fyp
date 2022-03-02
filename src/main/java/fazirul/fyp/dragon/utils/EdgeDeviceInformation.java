package fazirul.fyp.dragon.utils;

import fazirul.fyp.elements.ResourceBundle;

import java.time.LocalTime;

/**
 * The vote, resource demanded and voting time information (on a single edge server) of a single edge device.
 * See DRAGON paper for more details.
 */
public class EdgeDeviceInformation {
    private final int edgeDeviceID;
    private int vote;
    private ResourceBundle resource;
    private LocalTime timestamp;

    public EdgeDeviceInformation(int edgeDeviceID) {
        this.edgeDeviceID = edgeDeviceID;
        this.vote = 0;
        this.resource = new ResourceBundle(0,0,0);
        this.timestamp = LocalTime.now();
    }

    public EdgeDeviceInformation(int edgeDeviceID, int vote, ResourceBundle resource, LocalTime timestamp) {
        this.edgeDeviceID = edgeDeviceID;
        this.vote = vote;
        this.resource = resource;
        this.timestamp = timestamp;
    }

    public int getEdgeDeviceID() {
        return edgeDeviceID;
    }

    public int getVote() {
        return vote;
    }

    public void setVote(int vote) {
        this.vote = vote;
    }

    public ResourceBundle getResource() {
        return resource;
    }

    public void setResource(ResourceBundle resource) {
        this.resource = resource;
    }

    public LocalTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalTime timestamp) {
        this.timestamp = timestamp;
    }

    /**
     * returns the ratio between vote and resourceDemanded, where the resourceDemanded is normalised against the resource available on server to get a scalar value.
     * @param resourceAvailableOnServer resource available on the server
     * @return the ratio
     *
     * @see ResourceBundle#normalise(ResourceBundle)
     */
    public double getVoteResourceRatio(ResourceBundle resourceAvailableOnServer) {
        return vote/resource.normalise(resourceAvailableOnServer);
    }
}
