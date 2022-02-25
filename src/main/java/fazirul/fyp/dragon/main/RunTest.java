package fazirul.fyp.dragon.main;

import fazirul.fyp.elements.Node;
import fazirul.fyp.elements.ResourceBundle;
import org.cloudbus.cloudsim.cloudlets.Cloudlet;
import org.cloudbus.cloudsim.cloudlets.CloudletSimple;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.utilizationmodels.UtilizationModelFull;
import org.cloudbus.cloudsim.vms.VmSimple;

public class RunTest {
    public static void main(String[] args) {
        CloudSim sim = new CloudSim();
        Node n = new Node(sim, new ResourceBundle(10, 10000, 10000));
        Node n2 = new Node(sim, new ResourceBundle(10, 10000, 10000));
        Cloudlet c = new CloudletSimple(100, 10);
        c.setUtilizationModel(new UtilizationModelFull());


        n.allocate(new VmSimple(100, 10),c);
        n2.allocate(new VmSimple(100, 10), new CloudletSimple(1000, 10));
        sim.start();
        System.out.println(n.getAvailableResources());
    }
}
