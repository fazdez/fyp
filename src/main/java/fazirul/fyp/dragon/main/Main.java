package fazirul.fyp.dragon.main;

import fazirul.fyp.dragon.config.Config;
import fazirul.fyp.dragon.utils.VirtualMachineHandler;
import fazirul.fyp.elements.DistSimManager;
import org.cloudbus.cloudsim.core.CloudSim;

public class Main {
    public static void main(String[] args) {
        CloudSim simulation = new CloudSim();
        DistSimManager distSimManager = new DistSimManager(simulation);

        //initialize SINGLETON classes
        Config cfg = Config.getInstance();
        VirtualMachineHandler.getInstance();

        cfg.createEdgeServers(simulation);
        cfg.createEdgeDevices(simulation);

        simulation.start();
    }
}
