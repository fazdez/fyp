package fazirul.fyp.dragon.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import fazirul.fyp.dragon.dragonDevice.EdgeDeviceDragon;
import fazirul.fyp.elements.EdgeServer;
import fazirul.fyp.elements.ResourceBundle;
import org.cloudbus.cloudsim.core.CloudSim;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

public class Config {
    public static final String filename = "/config.json";
    public static final String testFilename = "/config-test-single-application.json";
    public static final String filename_nofeasiblesolution = "/config-no-feasible-solution.json";
    public static final String filenameGoogleTraceDataSet = "/config-google.json";
    private static Config singleInstance = null;
    private ConfigPOJO configurations;

    public Config() {
        ObjectMapper om = new ObjectMapper();
        try {
            configurations = om.readValue(getClass().getResourceAsStream(filename), ConfigPOJO.class);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static Config getInstance() {
        if (singleInstance == null) {
            singleInstance = new Config();
        }

        return singleInstance;
    }

    public void setConfigPath(String configPath) {
        ObjectMapper om = new ObjectMapper();
        try {
            configurations = om.readValue(getClass().getResourceAsStream(configPath), ConfigPOJO.class);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public List<ResourceBundle> getVirtualMachines() {
        ResourceBundlePOJO[] vms = configurations.getVirtualMachines();
        List<ResourceBundle> result = new ArrayList<>();
        for (ResourceBundlePOJO vm: vms) {
            result.add(new ResourceBundle(vm.getCpu(), vm.getBandwidth(), vm.getMemory()));
        }

        return result;
    }

    public HashSet<EdgeServer> createEdgeServers(CloudSim sim) {
        HashSet<EdgeServer> result = new HashSet<>();
        for (ResourceBundlePOJO n: configurations.getEdgeServers()) {
            result.add(new EdgeServer(sim, new ResourceBundle(n.getCpu(), n.getBandwidth(), n.getMemory())));
        }

        return result;
    }

    public List<EdgeDeviceDragon> createEdgeDevices(CloudSim sim) {
        List<EdgeDeviceDragon> result = new ArrayList<>();
        int[] arrivalTimes = configurations.getArrivalTimes();
        for(int idx = 0; idx < configurations.getEdgeDevices().length; idx++) {
            int arrivalTime = 0; //default arrival time
            if (idx < arrivalTimes.length) {
                //ensure bounds
                arrivalTime = arrivalTimes[idx];
            }
            ResourceBundlePOJO[] tasksPOJO = configurations.getEdgeDevices()[idx];
            List<ResourceBundle> tasks = new ArrayList<>();
            for (ResourceBundlePOJO rb : tasksPOJO) {
                tasks.add(new ResourceBundle(rb.getCpu(), rb.getBandwidth(), rb.getMemory()));
            }
            result.add(new EdgeDeviceDragon(sim, Integer.toString(idx), arrivalTime, tasks));
        }

        return result;
    }
}

