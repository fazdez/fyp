package fazirul.fyp.dragon.dragonDevice;

import fazirul.fyp.dragon.utils.TaskAssignment;
import fazirul.fyp.elements.Server;
import fazirul.fyp.elements.ResourceBundle;

import java.util.*;

/**
 * Represents the assignment for each task found in an edge device.
 */
public class AssignmentVector {
    /**
     * The edge device that this vector belongs to.
     */
    private final EdgeDeviceDragon edgeDevice;

    /**
     * Each element in the list represents a task and its actual {@link TaskAssignment assignment}.
     * Each index here corresponds to the index in the task list maintained by each edge device.
     */
    protected final List<TaskAssignment> assignmentList = new ArrayList<>();

    /**
     * If there are x tasks, y servers and z functions, there is a total of x*y*z possible assignments.
     * <p>These assignments are sorted in increasing order based on the private utility. Private utility is randomly generated
     * in {@link #generateRandomUtility()} during initialization of object.
     * </p>
     */
    private List<TaskAssignment> possibleAssignments = new ArrayList<>();

    public AssignmentVector(EdgeDeviceDragon edgeDevice) {
        this.edgeDevice = edgeDevice;
        List<ResourceBundle> tasks = edgeDevice.getTasks();
        tasks.forEach(t -> assignmentList.add(null));
        generateRandomUtility();
    }

    public void addTask(ResourceBundle task) {
        assignmentList.add(null);
        generateRandomUtility();
    }

    /**
     * For each possible assignment, generate a random private utility and add this assignment to {@link #possibleAssignments}.
     * Sort the list based on private utility generated.
     */
    private void generateRandomUtility() {
        int taskID = 0;
        for (ResourceBundle task: edgeDevice.getTasks()) {
            List<Integer> feasibleVms = edgeDevice.vmHandler.getFeasibleVirtualMachinesForTask(task);

            //pick a random selection from the list of feasible Vms.
            Collections.shuffle(feasibleVms);
            int randomNumber = new Random().nextInt(feasibleVms.size() + 1);
            if (randomNumber == 0) { randomNumber = 1; }
            if (feasibleVms.isEmpty()) {
                continue;
            }
            if (randomNumber > 3) {
                randomNumber = 3;
            }
            feasibleVms = feasibleVms.subList(0, randomNumber);


            for (int virtualMachineID: feasibleVms) {
                for (Server e: edgeDevice.getEdgeServers()) {
                    TaskAssignment toAdd = new TaskAssignment(taskID, e, virtualMachineID);
                    toAdd.setPrivateUtility((int) (Math.random() * 100)); //generates a random utility from 0 to 100.
                    possibleAssignments.add(toAdd);
                }
            }
            taskID++;
        }

        possibleAssignments.sort(Comparator.comparingInt(TaskAssignment::getPrivateUtility).reversed());
        if (possibleAssignments.size() >= 100) {
            possibleAssignments = possibleAssignments.subList(0, 101);
        }
    }

    /**
     * For each task in the {@link #assignmentList}, clear the assignment by setting it to null.
     */
    protected void clear() {
        assignmentList.replaceAll(t -> null);
    }

    /**
     *  Gets the resource available in each edge server, and compares it with the resource demanded by the VM of each task.
     *  @return true if there are enough resources available.
     */
    protected boolean isOffloadPossible() {
        HashMap<Server, ResourceBundle> resourceAvailable = edgeDevice.getResourceAvailableInServers();
        HashMap<Server, ResourceBundle> resourceDemanded = new HashMap<>();
        for (TaskAssignment t: assignmentList) {
            if (t == null) { continue; }
            ResourceBundle currResourceDemanded = resourceDemanded.get(t.getServer());
            if (currResourceDemanded == null) {
                currResourceDemanded = new ResourceBundle(0, 0, 0);
            }
            currResourceDemanded.addResources(edgeDevice.vmHandler.getVmResourceUsage(t.getVirtualMachineID()));
            resourceDemanded.put(t.getServer(), currResourceDemanded);
        }

        for (Server e: resourceAvailable.keySet()) {
            ResourceBundle avail = resourceAvailable.get(e);
            ResourceBundle want = resourceDemanded.get(e);

            if (want == null) { continue; }
            if (!avail.isBounded(want)) { return false; }
        }
        return true;
    }

    /**
     * Find the combination of task assignments with the highest total utility where the total resource usage is bounded by the maximum resources given.
     * This method directly modifies the assignment vector.
     *
     * @param maximumResources the bound restriction such that the assignment does not exceed this amount
     * @return true if such combination is found
     *
     * @see #embedding(HashMap, HashSet, int)
     */
    protected boolean embedding(HashMap<Server, ResourceBundle> maximumResources) {
        clear();
        return embedding(maximumResources, new HashSet<>(), 0);
    }

    /**
     * Find the combination of task assignments with the highest total utility where the total resource usage is bounded by the maximum resources given.
     * Directly modifies the {@link #assignmentList}.
     *
     * @param maximumResources the bound restriction such that the assignment does not exceed this amount
     * @param ignoredTasks a set of tasks to ignore. Tasks are ignored when it has a non-null assignment in {@link #assignmentList}.
     * @param startIndex start from this index when iterating through {@link #possibleAssignments}.
     * @return true if such combination is found
     * 
     * @see #embedding(HashMap) 
     */
    private boolean embedding(HashMap<Server, ResourceBundle> maximumResources, HashSet<Integer> ignoredTasks, int startIndex) {
        if (ignoredTasks == null) {
            ignoredTasks = new HashSet<>();
        }

        if (isCompleted()) { return true; } // base case

        for (int idx = startIndex; idx < possibleAssignments.size(); idx++) {
            TaskAssignment t = possibleAssignments.get(idx);
            ResourceBundle resourceDemanded = edgeDevice.vmHandler.getVmResourceUsage(t.getVirtualMachineID());
            Server e = t.getServer();

            // if task is ignored or exceeds maximum resources, we skip
            if (ignoredTasks.contains(t.getTaskID()) || !maximumResources.get(e).isBounded(resourceDemanded)) { continue; }

            assignmentList.set(t.getTaskID(), t); // set the assignment for task in the assignment list
            ignoredTasks.add(t.getTaskID()); //task is now ignored
            maximumResources.get(e).deductResources(resourceDemanded); //reduce maximumResources for next recursive call


            if (embedding(maximumResources, ignoredTasks, idx)) { return true; } //recursion call

            //else, this embedding does not work. Undo the previous assignment and try next one.
            assignmentList.set(t.getTaskID(), null);
            ignoredTasks.remove(t.getTaskID());
            maximumResources.get(e).addResources(resourceDemanded);
        }

        // we have exhausted through every possible combination
        return false;
    }

    /**
     * An AssignmentVector is considered completed if none of the task assignments are null.
     * @return true if {@link #assignmentList} has no null values.
     */
    protected boolean isCompleted() {
        for(TaskAssignment t: assignmentList) {
            if (t == null) { return false; }
        }

        return true;
    }
}
