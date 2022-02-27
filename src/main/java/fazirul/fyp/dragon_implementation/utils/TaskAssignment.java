package fazirul.fyp.dragon_implementation.utils;

import fazirul.fyp.elements.EdgeServer;
import fazirul.fyp.elements.ResourceBundle;

public class TaskAssignment {
    private ResourceBundle task;
    private EdgeServer server;
    private int virtualMachineIndex;

    /**
     * Identifies the assignment to a server in a virtual machine for a task.
     * @param task represented in ResourceBundle
     * @param server the server assigned
     * @param virtualMachineIndex the index that identifies the virtual machine in {@link fazirul.fyp.dragon.utils.VirtualMachineHandler}
     */
    public TaskAssignment(ResourceBundle task, EdgeServer server, int virtualMachineIndex) {
        this.task = task;
        this.server = server;
        this.virtualMachineIndex = virtualMachineIndex;
    }

    public EdgeServer getServer() {
        return server;
    }

    public int getVirtualMachineIndex() {
        return virtualMachineIndex;
    }

    public ResourceBundle getTask() {
        return task;
    }

    public void setServer(EdgeServer server) {
        this.server = server;
    }

    public void setTask(ResourceBundle task) {
        this.task = task;
    }

    public void setVirtualMachineIndex(int virtualMachineIndex) {
        this.virtualMachineIndex = virtualMachineIndex;
    }

}