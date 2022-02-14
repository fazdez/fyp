package fazirul.fyp.dragon.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import fazirul.fyp.dragon.app.DragonApplication;
import fazirul.fyp.elements.Node;
import fazirul.fyp.elements.ResourceBundle;
import fazirul.fyp.elements.UtilizationModelAbsolute;
import org.cloudbus.cloudsim.cloudlets.Cloudlet;
import org.cloudbus.cloudsim.cloudlets.CloudletSimple;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.core.Simulation;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

public class Config {
    private static final String filename = "/config.json";
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
        int totalNumApplications = configurations.getApplications().length;
        List<DragonApplication> result = new ArrayList<>();
        int id = 0;
        for (ResourceBundlePOJO[] app: configurations.getApplications()) {
            HashSet<Cloudlet> services = new HashSet<>();
            for (ResourceBundlePOJO serviceInfo: app) {
                Cloudlet service = new CloudletSimple(Constants.CLOUDLET_DEFAULT_MI, serviceInfo.getCpu());
                service.setUtilizationModelBw(new UtilizationModelAbsolute(serviceInfo.getBandwidth()));
                service.setUtilizationModelRam(new UtilizationModelAbsolute(serviceInfo.getMemory()));

                services.add(service);
            }

            result.add(new DragonApplication(id, nodes, totalNumApplications, services));
            id++;
        }

        return result;
    }
}

//define POJO classes here
class ConfigPOJO {
    private ResourceBundlePOJO[] functions;
    public ResourceBundlePOJO[] getFunctions() {
        return functions;
    }
    public void setFunctions(ResourceBundlePOJO[] functions) {
        this.functions = functions;
    }

    private ResourceBundlePOJO[][] applications;
    public ResourceBundlePOJO[][] getApplications() {
        return applications;
    }
    public void setApplications(ResourceBundlePOJO[][] applications) {
        this.applications = applications;
    }

    private ResourceBundlePOJO[] nodes;
    public ResourceBundlePOJO[] getNodes() {
        return nodes;
    }
    public void setNodes(ResourceBundlePOJO[] nodes) {
        this.nodes = nodes;
    }

    private HashMap<Integer, Integer[]> topology;
    public HashMap<Integer, Integer[]> getTopology() { return topology; }
    public void setTopology(HashMap<Integer, Integer[]> topology) {
        this.topology = topology;
    }
}

class ResourceBundlePOJO {
    private int cpu;
    public int getCpu() {
        return cpu;
    }
    public void setCpu(int cpu) {
        this.cpu = cpu;
    }

    private int memory;
    public int getMemory() {
        return memory;
    }
    public void setMemory(int memory) {
        this.memory = memory;
    }

    private int bandwidth;
    public int getBandwidth() {
        return bandwidth;
    }
    public void setBandwidth(int bandwidth) {
        this.bandwidth = bandwidth;
    }
}
