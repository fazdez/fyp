package fazirul.fyp.elements;

import fazirul.fyp.dragon.dragonDevice.EdgeDeviceDragon;
import org.cloudbus.cloudsim.cloudlets.Cloudlet;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.core.SimEntity;
import org.cloudbus.cloudsim.util.ResourceLoader;
import org.cloudsimplus.traces.google.GoogleTaskEventsTraceReader;
import org.cloudsimplus.traces.google.TaskEvent;
import org.cloudsimplus.traces.google.TaskEventType;

import java.io.IOException;
import java.util.HashMap;
import java.util.Optional;
import java.util.function.Function;

/**
 * The built-in {@link GoogleTaskEventsTraceReader} implementation of CloudSim Plus does not fit the current implementation of distributed simulation.
 *
 * However, it contains useful parsing of the Google Trace Data, thus this class overrides only certain functionalities of the built-in reader.
 */
public class GoogleTraceReader extends GoogleTaskEventsTraceReader  {
    private final HashMap<String, EdgeDeviceAbstract> edgeDevices = new HashMap<>();
    private final HashMap<String, Integer> jobIDToTaskIndexMapping = new HashMap<>();
    private final HashMap<String, EdgeDeviceAbstract> jobIDToEdgeDeviceMapping = new HashMap<>();
    private final HashMap<Double, Integer> arrivalEventCount = new HashMap<>();

    private static final int MAX_EVENTS = 50;
    private final Function<TaskEvent, EdgeDeviceAbstract> edgeDeviceCreateFunction;
    private final ResourceBundle resourceAvailableOnSingleServer;

    public GoogleTraceReader(CloudSim simulation, String filePath, Function<TaskEvent, Cloudlet> cloudletCreationFunction, Function<TaskEvent, EdgeDeviceAbstract> edgeDeviceCreateFunction) throws IOException {
        super(simulation, filePath, ResourceLoader.newInputStream(filePath, GoogleTraceReader.class), cloudletCreationFunction);
        this.edgeDeviceCreateFunction = edgeDeviceCreateFunction;
        resourceAvailableOnSingleServer = new ResourceBundle(0,0,0);
        Optional<SimEntity> optionalSimEntity = getSimulation().getEntityList().stream().filter(simEntity -> simEntity instanceof EdgeServer).findAny();
        if (optionalSimEntity.isEmpty()) {
            System.out.println("No edge servers registered with simulation!");
        } else {
            resourceAvailableOnSingleServer.addResources(((EdgeServer)optionalSimEntity.get()).getTotalResources());
        }
    }



    @Override
    protected boolean processParsedLineInternal() {
        /*
        for Tasks that happen at the same TIME, DIFFERENT ids, and SAME username, then we aggregate all these tasks to a DistributedApplication.
        if different time --> different DistributedApplication.
        if same ID --> ignore and skip
        if different username --> different Distributed Application

        e.g.

        time || taskID || username
        0    || 1      || abc
        0    || 2      || abc
        0    || 2      || xyz
        1    || 3      || abc

        then "abc0" with 2 tasks is a distribtued application, and "xyz0" with 2 tasks is another distributed application
        "abc1" with 1 task is another distributed application even though it is technically from the same broker!!
         */
        final var event = TaskEvent.of(this);
        if (event.getType() != TaskEventType.SUBMIT && event.getType() != TaskEventType.FINISH) {
            return true;
        }

        if (event.getTaskIndex() > 0) { return true; } //only process tasks with index == 0;

        String edgeDeviceUserName = createEdgeDeviceUsername(event);
        String taskIdentifier = event.getUserName() + "_" + event.getJobId();
        EdgeDeviceAbstract edgeDevice;
        if (event.getType() == TaskEventType.FINISH) {
            if (jobIDToTaskIndexMapping.get(taskIdentifier) == null || jobIDToEdgeDeviceMapping.get(taskIdentifier) == null) {
                return true;
            }
            edgeDevice = jobIDToEdgeDeviceMapping.get(taskIdentifier);
            if (!(edgeDevice instanceof EdgeDeviceDragon)) {
                return true; //TODO: do for all EdgeDeviceAbstract types?
            }
            int taskIndex = jobIDToTaskIndexMapping.get(taskIdentifier);
            double duration = event.getTimestamp() - edgeDevice.getArrivalTime();
            ((EdgeDeviceDragon) edgeDevice).addTaskLength(taskIndex, duration);
            return true;
        }

        if (arrivalEventCount.get(event.getTimestamp()) != null && arrivalEventCount.get(event.getTimestamp()) == MAX_EVENTS) {
            return true; //don't process anymore...
        }

        edgeDevice = edgeDevices.get(edgeDeviceUserName);
        if (edgeDevice == null) {
            //create new
            edgeDevice = edgeDeviceCreateFunction.apply(event);
            edgeDevices.put(edgeDeviceUserName, edgeDevice);
        }
        if (edgeDevice.tasks.size() > 10 ) { return true; }

        ResourceBundle task = createTaskFromEvent(event);
        jobIDToTaskIndexMapping.put(taskIdentifier, edgeDevice.tasks.size());
        jobIDToEdgeDeviceMapping.put(taskIdentifier, edgeDevice);
        edgeDevice.addTask(task);
        int currentEventCount = arrivalEventCount.get(event.getTimestamp()) == null ? 0 : arrivalEventCount.get(event.getTimestamp());
        arrivalEventCount.put(event.getTimestamp(), currentEventCount+1);
        return true;
    }

    private ResourceBundle createTaskFromEvent(TaskEvent event) {
        int cpu = (int) (resourceAvailableOnSingleServer.getCPU() * event.getResourceRequestForCpuCores());
        if (cpu == 0) { cpu = 1; } //minimum cpu
        return new ResourceBundle(cpu, 0,
                (int) (resourceAvailableOnSingleServer.getMemory() * event.getResourceRequestForRam())); //because the Google Trace Data does not specify bandwidth used...
    }

    public static String createEdgeDeviceUsername(TaskEvent event) {
        return String.format("%s_%.2f", event.getUserName(), event.getTimestamp());
    }
}
