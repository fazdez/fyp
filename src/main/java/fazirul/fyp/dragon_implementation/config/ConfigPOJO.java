package fazirul.fyp.dragon_implementation.config;

import java.util.HashMap;

class ConfigPOJO {
    private ResourceBundlePOJO[] functions;
    public ResourceBundlePOJO[] getFunctions() {
        return functions;
    }
    public void setFunctions(ResourceBundlePOJO[] functions) {
        this.functions = functions;
    }

    private ResourceBundlePOJO[][] applications;
    public ResourceBundlePOJO[][] getApplications() {
        return applications;
    }
    public void setApplications(ResourceBundlePOJO[][] applications) {
        this.applications = applications;
    }

    private ResourceBundlePOJO[] nodes;
    public ResourceBundlePOJO[] getNodes() {
        return nodes;
    }
    public void setNodes(ResourceBundlePOJO[] nodes) {
        this.nodes = nodes;
    }

    private HashMap<Integer, int[]> topology;
    public HashMap<Integer, int[]> getTopology() { return topology; }
    public void setTopology(HashMap<Integer, int[]> topology) {
        this.topology = topology;
    }
}