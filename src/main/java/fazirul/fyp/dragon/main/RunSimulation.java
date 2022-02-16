package fazirul.fyp.dragon.main;

import fazirul.fyp.dragon.app.DragonApplication;
import fazirul.fyp.dragon.utils.Config;
import fazirul.fyp.dragon.utils.FunctionsHandler;
import fazirul.fyp.elements.Node;
import org.cloudbus.cloudsim.core.CloudSim;

import java.util.HashSet;
import java.util.List;

public class RunSimulation {
    public static void main(String[] args) {
        // initialization
        Config configManager = Config.getInstance();
        CloudSim simulation = new CloudSim();
        FunctionsHandler functionsHandler = FunctionsHandler.getInstance();

        HashSet<Node> nodes = configManager.getNodes(simulation);
        List<DragonApplication> applicationList = configManager.getApplications(nodes);
        functionsHandler.registerApplications(applicationList);

        for (DragonApplication app: applicationList) {
            app.start();
        }

        for (DragonApplication app: applicationList) {
            try {
                app.join();
            } catch(Exception e) {
                e.printStackTrace();
            }
        }

        for (DragonApplication app: applicationList) {
            app.printResults();
            System.out.println();
        }

        simulation.start();
    }
}
