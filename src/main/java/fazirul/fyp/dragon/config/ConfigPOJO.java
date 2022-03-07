package fazirul.fyp.dragon.config;

/**
 * Plain-Old Java Object of the config for use by {@link com.fasterxml.jackson.databind.ObjectMapper}.
 */
public class ConfigPOJO {
    private ResourceBundlePOJO[] virtualMachines;
    public ResourceBundlePOJO[] getVirtualMachines() {
        return virtualMachines;
    }
    public void setVirtualMachines(ResourceBundlePOJO[] virtualMachines) {
        this.virtualMachines = virtualMachines;
    }

    private ResourceBundlePOJO[][] edgeDevices;
    public ResourceBundlePOJO[][] getEdgeDevices() {
        return edgeDevices;
    }
    public void setEdgeDevices(ResourceBundlePOJO[][] edgeDevices) {
        this.edgeDevices = edgeDevices;
    }

    private ResourceBundlePOJO[] edgeServers;
    public ResourceBundlePOJO[] getEdgeServers() {
        return edgeServers;
    }
    public void setEdgeServers(ResourceBundlePOJO[] edgeServers) {
        this.edgeServers = edgeServers;
    }

    private int[] arrivalTimes;
    public int[] getArrivalTimes() {
        return arrivalTimes;
    }
    public void setArrivalTimes(int[] arrivalTimes) {
        this.arrivalTimes = arrivalTimes;
    }
}