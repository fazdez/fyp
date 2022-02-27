package fazirul.fyp.elements;

import fazirul.fyp.dragon.utils.Constants;
import org.cloudbus.cloudsim.allocationpolicies.VmAllocationPolicyRandom;
import org.cloudbus.cloudsim.brokers.DatacenterBroker;
import org.cloudbus.cloudsim.brokers.DatacenterBrokerSimple;
import org.cloudbus.cloudsim.cloudlets.Cloudlet;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.datacenters.DatacenterSimple;
import org.cloudbus.cloudsim.hosts.Host;
import org.cloudbus.cloudsim.hosts.HostSimple;
import org.cloudbus.cloudsim.resources.Pe;
import org.cloudbus.cloudsim.resources.PeSimple;
import org.cloudbus.cloudsim.vms.Vm;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class EdgeServer extends DatacenterSimple{
    private static final String DEFAULT_NAME = "EdgeServer_";
    private static int globalID = 0;
    private final int id;
    private final static int DEFAULT_STORAGE_CAPACITY = 100000000;
    private final ResourceBundle totalResources;

    public EdgeServer(CloudSim simulation, ResourceBundle resources) {
        super(simulation, Collections.singletonList(createHostFromResourceBundle(resources)));
        id = globalID;
        globalID++;
        setName(DEFAULT_NAME + id);
        totalResources = resources;
    }

    private static Host createHostFromResourceBundle(ResourceBundle resources) {
        List<Pe> peList = new ArrayList<>();
        for (int i = 0; i < resources.getCPU(); i ++){
            peList.add(new PeSimple(Constants.HOST_DEFAULT_MIPS));
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
