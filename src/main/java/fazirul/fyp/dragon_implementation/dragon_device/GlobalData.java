package fazirul.fyp.dragon_implementation.dragon_device;

import fazirul.fyp.dragon_implementation.utils.EdgeDeviceInformation;
import fazirul.fyp.dragon_implementation.utils.Election;
import fazirul.fyp.elements.EdgeServer;
import fazirul.fyp.elements.ResourceBundle;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

/**
 * The data that an edge device maintains on other edge devices.
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
                deviceInformations.add(new EdgeDeviceInformation());
            }
            data.put(e, deviceInformations);
        }
    }

    protected Election election() {
        return null;
    }

    protected void updateVoteForServer(int vote, EdgeServer e) {
        EdgeDeviceInformation ownInfo = data.get(e).get(edgeDevice.getIndex());
        ownInfo.setVote(vote);
        ownInfo.setTimestamp(LocalTime.now());
    }

    protected void updateResourceForServer(ResourceBundle resource, EdgeServer e) {
        EdgeDeviceInformation ownInfo = data.get(e).get(edgeDevice.getIndex());
        ownInfo.setResource(resource);
    }
}
