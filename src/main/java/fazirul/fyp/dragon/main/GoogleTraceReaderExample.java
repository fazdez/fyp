package fazirul.fyp.dragon.main;

import fazirul.fyp.dragon.config.Config;
import fazirul.fyp.dragon.dragonDevice.EdgeDeviceDragon;
import fazirul.fyp.dragon.utils.VirtualMachineHandler;
import fazirul.fyp.elements.DistSimManager;
import fazirul.fyp.elements.DistributedApplication;
import fazirul.fyp.elements.GoogleTraceReader;
import org.cloudbus.cloudsim.cloudlets.Cloudlet;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudsimplus.traces.google.TaskEvent;

import java.util.ArrayList;

public class GoogleTraceReaderExample {
    private static CloudSim SIMULATION;

    public static void main(String[] args) {
        new GoogleTraceReaderExample();
    }

    private GoogleTraceReaderExample() {
        SIMULATION = new CloudSim(1 );
        DistSimManager distSimManager = new DistSimManager(SIMULATION);
        distSimManager.setSparseTopology();
        //initialize SINGLETON classes
        Config cfg = Config.getInstance();
        cfg.setConfigPath(Config.filenameGoogleTraceDataSet);
        VirtualMachineHandler.getInstance();

        cfg.createEdgeServers(SIMULATION);
        try {
            GoogleTraceReader reader = new GoogleTraceReader(SIMULATION, "task-events-sample-2.csv", this::createCloudlet, this::createEdgeDevice);
            reader.process();
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }

        SIMULATION.start();
        distSimManager.printStatistics();
    }

    public DistributedApplication createEdgeDevice(TaskEvent event) {
        DistributedApplication distributedApplication = new EdgeDeviceDragon(SIMULATION, GoogleTraceReader.createEdgeDeviceUsername(event), event.getTimestamp(), new ArrayList<>());
        distributedApplication.setNetworkLatency(0);
        return distributedApplication;
    }

    // we dont use this
    public Cloudlet createCloudlet(TaskEvent event) {
        return null;
    }

}
