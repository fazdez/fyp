package fazirul.fyp.dragon_implementation.utils;

import fazirul.fyp.elements.ResourceBundle;

import java.time.LocalTime;

/**
 * The vote, resource demanded and voting time information (on a single edge server) of a single edge device.
 * See DRAGON paper for more details.
 */
public class EdgeDeviceInformation {
    private int vote;
    private ResourceBundle resource;
    private LocalTime timestamp;

    public EdgeDeviceInformation() {
        this.vote = 0;
        this.resource = new ResourceBundle(0,0,0);
        this.timestamp = LocalTime.now();
    }

    public EdgeDeviceInformation(int vote, ResourceBundle resource, LocalTime timestamp) {
        this.vote = vote;
        this.resource = resource;
        this.timestamp = timestamp;
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
}
