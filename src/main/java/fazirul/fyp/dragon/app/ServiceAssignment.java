package fazirul.fyp.dragon.app;

import fazirul.fyp.dragon.utils.VirtualMachineHandler;
import fazirul.fyp.elements.Node;
import fazirul.fyp.elements.ResourceBundle;
import org.cloudbus.cloudsim.cloudlets.Cloudlet;

public class ServiceAssignment {
    private final Cloudlet service;
    private final int function;
    private final Node node;
    private int utility;

    public ServiceAssignment(Cloudlet service, int function, Node node, int utility) {
        this.service = service;
        this.function = function;
        this.node = node;
        this.utility = utility;
    }

    public ResourceBundle getResourcesUsed() {
        return VirtualMachineHandler.getInstance().getFunctionResourceUsage(this.function);
    }

    public Cloudlet getService() {
        return service;
    }

    public int getFunction() {
        return function;
    }

    public Node getNode() { return node; }

    public int getUtility() {
        return this.utility;
    }

    public void setUtility(int utility) {
        this.utility = utility;
    }

    public String toString() {
        return service.getUid() + function + node.toString();
    }
}


