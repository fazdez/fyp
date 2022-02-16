package fazirul.fyp.dragon.app;

import fazirul.fyp.dragon.utils.FunctionsHandler;
import fazirul.fyp.elements.Node;
import fazirul.fyp.elements.ResourceBundle;
import org.cloudbus.cloudsim.cloudlets.Cloudlet;

import java.text.MessageFormat;
import java.util.*;

// made up of ServiceAssignments
public class Assignment {
    private final HashMap<Cloudlet, ServiceAssignment> data = new HashMap<>();
    private final HashMap<String, Integer> knownPrivateUtilities = new HashMap<>();
    private final HashSet<Node> nodes;

    public Assignment(HashSet<Cloudlet> services, HashSet<Node> nodes) {
        this.nodes = nodes;
        services.forEach((s) -> this.data.put(s, null)); //initialize
    }

    public List<ServiceAssignment> getAssignments() {
        return new ArrayList<>(this.data.values());
    }

    public Set<Cloudlet> getServices() {
        return data.keySet();
    }

    public void offload() {
        for (Cloudlet c: data.keySet()) {
            Node n = data.get(c).getNode();
            int function = data.get(c).getFunction();
            n.allocate(FunctionsHandler.getInstance().createFunction(function), c);
        }
    }

    public void addAssignment(Cloudlet service, int function, Node node, int utility) {
        this.data.put(service, new ServiceAssignment(service, function, node, utility));
    }

    public void addAssignment(ServiceAssignment assignment) {
        this.data.put(assignment.getService(), assignment);
    }

    public void removeAssignment(Cloudlet service) {
        this.data.put(service, null);
    }

    public ServiceAssignment getAssignmentForService(Cloudlet service) {
        return this.data.get(service);
    }

    public int size() {
        return this.data.size();
    }

    public ResourceBundle getResourceUsage(Node node) {
        ResourceBundle resourceUsage = new ResourceBundle(0,0,0);
        for (ServiceAssignment assignment: this.data.values()) {
            if (assignment.getNode() == node) {
                resourceUsage.addResources(FunctionsHandler.getInstance().getFunctionResourceUsage(assignment.getFunction()));
            }
        }
        return resourceUsage;
    }

    public void reset() {
        this.data.replaceAll((s, v) -> null);
    }

    public List<ServiceAssignment> getNextBestAssignments() {
        List<ServiceAssignment> result = new ArrayList<>();

        for (Cloudlet s: this.data.keySet()) {
            if (this.data.get(s) == null) {
                List<Integer> possibleFunctions = FunctionsHandler.getInstance().getServiceMapping(s);
                for (Node n: nodes) {
                    for (int f: possibleFunctions) {
                        ServiceAssignment toAdd = new ServiceAssignment(s, f, n, 0);
                        toAdd.setUtility(this.getPrivateUtility(toAdd));
                        result.add(toAdd);
                    }
                }
            }
        }
        result.sort(Comparator.comparingInt(ServiceAssignment::getUtility).reversed()); // sort in descending order based on marginal utility.
        return result;
    }

    //calculates a randomly-generated private utility
    private int getPrivateUtility(ServiceAssignment assignment) {
        if (this.knownPrivateUtilities.containsKey(assignment.toString())) {
            return this.knownPrivateUtilities.get(assignment.toString());
        }
        // returns a random value from 1 - 100
        double v = Math.random() * 100;
        if (v == 0) { v = 1; }
        int result = (int) v;

        this.knownPrivateUtilities.put(assignment.toString(), result);
        return result;
    }

    public int getTotalPrivateUtility(Node n) {
        int result = 0;
        for (ServiceAssignment s: this.data.values()) {
            if (s.getNode() == n) {
                result += s.getUtility();
            }
        }

        return result;
    }

    //all its services have an assignment
    public boolean completed() {
        boolean completed = true;
        for (ServiceAssignment s: this.data.values()) {
            if (s == null) {
                completed = false;
                break;
            }
        }

        return completed;
    }

    public int numIncompleteServices() {
        int result = 0;
        for (ServiceAssignment s: this.data.values()) {
            if (s == null) { result += 1; }
        }
        return result;
    }

    public String toString() {
        StringBuilder result = new StringBuilder();

        for (ServiceAssignment sa: data.values()) {
            if (sa == null) { continue; }
            result.append(getCloudletInformation(sa.getService())).append(" deployed on node ").append(sa.getNode().getID()).append(" using function ").append(sa.getFunction()).append(" with utility ").append(sa.getUtility()).append("\n");
        }
        return result.toString();
    }

    private String getCloudletInformation(Cloudlet cloudlet) {
        return MessageFormat.format("service (cpu: {0},mem: {1},bw: {2})\n", cloudlet.getNumberOfPes(), cloudlet.getUtilizationOfRam(), cloudlet.getUtilizationOfBw());
    }

    public String getCloudletsInformation() {
        StringBuilder result = new StringBuilder();
        for (Cloudlet service: data.keySet()) {
            result.append(getCloudletInformation(service));
        }
        return result.toString();
    }

}

