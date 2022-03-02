package fazirul.fyp.dragon.config;

class ConfigPOJO {
    private ResourceBundlePOJO[] edgeDevices;
    public ResourceBundlePOJO[] getEdgeDevices() {
        return edgeDevices;
    }
    public void setEdgeDevices(ResourceBundlePOJO[] edgeDevices) {
        this.edgeDevices = edgeDevices;
    }

    private ResourceBundlePOJO[][] applications;
    public ResourceBundlePOJO[][] getApplications() {
        return applications;
    }
    public void setApplications(ResourceBundlePOJO[][] applications) {
        this.applications = applications;
    }

    private ResourceBundlePOJO[] edgeServers;
    public ResourceBundlePOJO[] getEdgeServers() {
        return edgeServers;
    }
    public void setEdgeServers(ResourceBundlePOJO[] edgeServers) {
        this.edgeServers = edgeServers;
    }

    private int[] getArrivalTimes;
    public int[] getGetArrivalTimes() {
        return getArrivalTimes;
    }
    public void setGetArrivalTimes(int[] getArrivalTimes) {
        this.getArrivalTimes = getArrivalTimes;
    }
}