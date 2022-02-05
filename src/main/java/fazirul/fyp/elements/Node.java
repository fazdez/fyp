package fazirul.fyp.elements;


import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


import fazirul.fyp.dragon.utils.Constants;
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

public class Node {
    private final DatacenterBroker broker;
    private final DatacenterSimple datacenter;

    public Node(CloudSim simulation, ResourceBundle resources) {
        datacenter = new DatacenterSimple(simulation, Collections.singletonList(createHostFromResourceBundle(resources)));
        broker = new DatacenterBrokerSimple(simulation);
        broker.setDatacenterMapper((dc, u)-> datacenter);
    }

    public void allocate(Vm function, Cloudlet task) {
        ArrayList<Cloudlet> cloudletList = new ArrayList<>();
        cloudletList.add(task);

        ArrayList<Vm> vmList = new ArrayList<>();
        vmList.add(function);
        this.broker.submitVmList(vmList);
        this.broker.submitCloudletList(cloudletList,function);
    }

    private Host createHostFromResourceBundle(ResourceBundle resources) {
        List<Pe> peList = new ArrayList<>();
        for (int i = 0; i < resources.getCPU(); i ++){
            peList.add(new PeSimple(Constants.HOST_DEFAULT_MIPS));
        }

        return new HostSimple(resources.getMemory(), resources.getBandwidth(), 0, peList);
    }

    public ResourceBundle getResources() {
        int bw = (int) datacenter.getHost(0).getBw().getAvailableResource();
        int cpu = datacenter.getHost(0).getFreePesNumber();
        int ram = (int) datacenter.getHost(0).getRam().getAvailableResource();
        return new ResourceBundle(cpu, bw, ram);
    }
}