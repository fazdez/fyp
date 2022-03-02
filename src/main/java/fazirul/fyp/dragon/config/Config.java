package fazirul.fyp.dragon.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import fazirul.fyp.elements.EdgeServer;
import fazirul.fyp.elements.ResourceBundle;
import org.cloudbus.cloudsim.core.CloudSim;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

public class Config {
    private static final String filename = "/config.json";
    private static final String testFilename = "/config-test-single-application.json";
    private static final String filename_nofeasiblesolution = "/config-no-feasible-solution.json";
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

    public List<ResourceBundle> getVirtualMachines() {
        ResourceBundlePOJO[] vms = configurations.getEdgeDevices();
        List<ResourceBundle> result = new ArrayList<>();
        for (ResourceBundlePOJO vm: vms) {
            result.add(new ResourceBundle(vm.getCpu(), vm.getBandwidth(), vm.getMemory()));
        }

        return result;
    }

    public HashSet<EdgeServer> getEdgeServers(CloudSim sim) {
        HashSet<EdgeServer> result = new HashSet<>();
        for (ResourceBundlePOJO n: configurations.getEdgeServers()) {
            result.add(new EdgeServer(sim, new ResourceBundle(n.getCpu(), n.getBandwidth(), n.getMemory())));
        }

        return result;
    }

//    public List<DragonApplication> getEdgeDevices(HashSet<EdgeServer> nodes) {
//        int serviceID = 0;
//        int totalNumApplications = configurations.getApplications().length;
//        List<DragonApplication> result = new ArrayList<>();
//        int id = 0;
//        for (ResourceBundlePOJO[] app: configurations.getApplications()) {
//            HashSet<Cloudlet> services = new HashSet<>();
//            for (ResourceBundlePOJO serviceInfo: app) {
//                Cloudlet service = new CloudletSimple(Constants.CLOUDLET_DEFAULT_MI, serviceInfo.getCpu());
//                service.setUtilizationModelBw(new UtilizationModelAbsolute(serviceInfo.getBandwidth()));
//                service.setUtilizationModelRam(new UtilizationModelAbsolute(serviceInfo.getMemory()));
//                service.setId(serviceID);
//                serviceID++;
//
//
//                services.add(service);
//            }
//            result.add(new DragonApplication(id, nodes, totalNumApplications, services));
//            id++;
//        }
//
//        HashMap<Integer, int[]> topology = configurations.getTopology();
//
//        //set topology
//        for (int app: topology.keySet()) {
//            int[] neighbours = topology.get(app);
//            for (int n: neighbours) {
//                result.get(app).addNeighbour(result.get(n));
//            }
//        }
//
//        return result;
//    }
}

