package fazirul.fyp.elements;

import org.cloudbus.cloudsim.cloudlets.Cloudlet;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.core.SimEntity;
import org.cloudbus.cloudsim.util.ResourceLoader;
import org.cloudsimplus.traces.google.GoogleTaskEventsTraceReader;
import org.cloudsimplus.traces.google.TaskEvent;
import org.cloudsimplus.traces.google.TaskEventType;

import javax.swing.text.html.Option;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Optional;
import java.util.function.Function;

/**
 * The built-in {@link GoogleTaskEventsTraceReader} implementation of CloudSim Plus does not fit the current implementation of distributed simulation.
 *
 * However, it contains useful parsing of the Google Trace Data, thus this class overrides only certain functionalities of the built-in reader.
 */
public class GoogleTraceReader extends GoogleTaskEventsTraceReader  {
    private final HashMap<String, EdgeDeviceAbstract> edgeDevices = new HashMap<>();
    private final HashSet<String> addedTasks = new HashSet<>();

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
        if (event.getType() != TaskEventType.SUBMIT) {
            return true; // only process submit events which are essentially the arrival event
        }

        String edgeDeviceUserName = createEdgeDeviceUsername(event);
        String taskIdentifier = edgeDeviceUserName + "_" + event.getJobId();

        if (addedTasks.contains(taskIdentifier)) {
            return true; //don't process duplicate tasks
        }


        EdgeDeviceAbstract edgeDevice = edgeDevices.get(edgeDeviceUserName);
        if (edgeDevice == null) {
            //create new
            edgeDevice = edgeDeviceCreateFunction.apply(event);
            edgeDevices.put(edgeDeviceUserName, edgeDevice);

        }

        ResourceBundle task = createTaskFromEvent(event);
        edgeDevice.addTask(task);
        addedTasks.add(taskIdentifier);
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
