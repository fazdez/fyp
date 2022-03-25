package fazirul.fyp.dragon.main;

import fazirul.fyp.dragon.config.Config;
import fazirul.fyp.dragon.utils.VirtualMachineHandler;
import fazirul.fyp.elements.DistSimManager;
import org.cloudbus.cloudsim.core.CloudSim;

public class Main {
    public static void main(String[] args) {
        CloudSim simulation = new CloudSim(1);
        DistSimManager distSimManager = new DistSimManager(simulation);
        distSimManager.setSparseTopology();

        //initialize SINGLETON classes
        Config cfg = Config.getInstance();
        cfg.setConfigPath(Config.filename);
        VirtualMachineHandler.getInstance();

        cfg.createEdgeServers(simulation);
        cfg.createEdgeDevices(simulation);

        simulation.start();
        distSimManager.printStatistics();
    }
}
