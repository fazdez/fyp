package fazirul.fyp.basic;

import fazirul.fyp.elements.DistSimManager;
import fazirul.fyp.elements.ResourceBundle;
import fazirul.fyp.elements.Server;
import org.cloudbus.cloudsim.core.CloudSim;

import java.util.Collections;

public class Main {
    public static void main(String[] args) {
        CloudSim simulation = new CloudSim(1);
        DistSimManager distSimManager = new DistSimManager(simulation);
        distSimManager.setSparseTopology();

        new Server(simulation, new ResourceBundle(12, 0, 0));

        new PlaceholderApplication(simulation, "da1", 0.3, Collections.singletonList(new ResourceBundle(1, 0,0)));
        new PlaceholderApplication(simulation, "da2", 0.6, Collections.singletonList(new ResourceBundle(1, 0,0)));
        new PlaceholderApplication(simulation, "da3", 0.7, Collections.singletonList(new ResourceBundle(1, 0,0)));
        new PlaceholderApplication(simulation, "da4", 3, Collections.singletonList(new ResourceBundle(1, 0,0)));

        simulation.start();
    }
}
