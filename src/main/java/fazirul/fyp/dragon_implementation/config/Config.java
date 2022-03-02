package fazirul.fyp.dragon_implementation.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import fazirul.fyp.dragon.app.DragonApplication;
import fazirul.fyp.dragon.utils.Constants;
import fazirul.fyp.elements.Node;
import fazirul.fyp.elements.ResourceBundle;
import fazirul.fyp.elements.UtilizationModelAbsolute;
import org.cloudbus.cloudsim.cloudlets.Cloudlet;
import org.cloudbus.cloudsim.cloudlets.CloudletSimple;
import org.cloudbus.cloudsim.core.CloudSim;

import java.util.ArrayList;
import java.util.HashMap;
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

    public List<ResourceBundle> getFunctions() {
        ResourceBundlePOJO[] fns = configurations.getFunctions();
        List<ResourceBundle> result = new ArrayList<>();
        for (ResourceBundlePOJO fn: fns) {
            result.add(new ResourceBundle(fn.getCpu(), fn.getBandwidth(), fn.getMemory()));
        }

        return result;
    }

    public HashSet<Node> getNodes(CloudSim sim) {
        HashSet<Node> result = new HashSet<>();
        for (ResourceBundlePOJO n: configurations.getNodes()) {
            result.add(new Node(sim, new ResourceBundle(n.getCpu(), n.getBandwidth(), n.getMemory())));
        }

        return result;
    }

    public List<DragonApplication> getApplications(HashSet<Node> nodes) {
        int serviceID = 0;
        int totalNumApplications = configurations.getApplications().length;
        List<DragonApplication> result = new ArrayList<>();
        int id = 0;
        for (ResourceBundlePOJO[] app: configurations.getApplications()) {
            HashSet<Cloudlet> services = new HashSet<>();
            for (ResourceBundlePOJO serviceInfo: app) {
                Cloudlet service = new CloudletSimple(Constants.CLOUDLET_DEFAULT_MI, serviceInfo.getCpu());
                service.setUtilizationModelBw(new UtilizationModelAbsolute(serviceInfo.getBandwidth()));
                service.setUtilizationModelRam(new UtilizationModelAbsolute(serviceInfo.getMemory()));
                service.setId(serviceID);
                serviceID++;


                services.add(service);
            }
            result.add(new DragonApplication(id, nodes, totalNumApplications, services));
            id++;
        }

        HashMap<Integer, int[]> topology = configurations.getTopology();

        //set topology
        for (int app: topology.keySet()) {
            int[] neighbours = topology.get(app);
            for (int n: neighbours) {
                result.get(app).addNeighbour(result.get(n));
            }
        }

        return result;
    }
}

