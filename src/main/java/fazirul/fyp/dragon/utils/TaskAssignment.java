package fazirul.fyp.dragon.utils;

import fazirul.fyp.dragon.dragonDevice.EdgeDeviceDragon;
import fazirul.fyp.elements.EdgeServer;

/**
 * Represents a task and indicates which server and what virtual machine is this task is offloaded to.
 *
 * @see fazirul.fyp.dragon.dragonDevice.AssignmentVector
 */
public class TaskAssignment {
    private int taskID;
    private EdgeServer server;
    private int virtualMachineID;
    private int privateUtility = 0;

    /**
     * Identifies the assignment to a server in a virtual machine for a task. By default, private utility is 0.
     * @param taskID Each task is uniquely identified in an edgeDevice by their index in the {@link EdgeDeviceDragon#getTasks() edgeDevice's task list}.
     * @param server the server assigned
     * @param virtualMachineID the index that identifies the virtual machine in {@link fazirul.fyp.dragon.utils.VirtualMachineHandler}
     */
    public TaskAssignment(int taskID, EdgeServer server, int virtualMachineID) {
        this.taskID = taskID;
        this.server = server;
        this.virtualMachineID = virtualMachineID;
    }

    public EdgeServer getServer() {
        return server;
    }

    public int getVirtualMachineID() {
        return virtualMachineID;
    }

    public int getTaskID() {
        return taskID;
    }

    public void setServer(EdgeServer server) {
        this.server = server;
    }

    public void setTaskID(int taskID) {
        this.taskID = taskID;
    }

    public void setVirtualMachineID(int virtualMachineID) {
        this.virtualMachineID = virtualMachineID;
    }

    public void setPrivateUtility(int utility) { privateUtility = utility; }

    public int getPrivateUtility() { return privateUtility; }
}