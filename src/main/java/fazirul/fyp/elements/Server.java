package fazirul.fyp.elements;

import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.datacenters.DatacenterSimple;
import org.cloudbus.cloudsim.hosts.Host;
import org.cloudbus.cloudsim.hosts.HostSimple;
import org.cloudbus.cloudsim.resources.Pe;
import org.cloudbus.cloudsim.resources.PeSimple;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Server extends DatacenterSimple {
    private static final String DEFAULT_NAME = "Server_";
    private static int globalID = 0;
    private final static int DEFAULT_STORAGE_CAPACITY = Integer.MAX_VALUE;
    private final static int HOST_DEFAULT_MIPS = 1000;
    private final ResourceBundle totalResources;

    public Server(CloudSim simulation, ResourceBundle resources) {
        super(simulation, Collections.singletonList(createHostFromResourceBundle(resources)));
        setName(DEFAULT_NAME + globalID);
        globalID++;
        totalResources = resources;
    }

    private static Host createHostFromResourceBundle(ResourceBundle resources) {
        List<Pe> peList = new ArrayList<>();
        for (int i = 0; i < resources.getCPU(); i++){
            peList.add(new PeSimple(HOST_DEFAULT_MIPS));
        }
        return new HostSimple(resources.getMemory(), resources.getBandwidth(), DEFAULT_STORAGE_CAPACITY, peList);
    }

    public ResourceBundle getAvailableResources() {
        int bw = (int) this.getHost(0).getBw().getAvailableResource();
        int cpu = this.getHost(0).getFreePesNumber();
        int ram = (int) this.getHost(0).getRam().getAvailableResource();
        return new ResourceBundle(cpu, bw, ram);
    }

    public ResourceBundle getTotalResources() {
        return totalResources.clone();
    }
}
