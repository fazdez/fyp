package fazirul.fyp.elements;

import org.cloudbus.cloudsim.core.CloudSimEntity;
import org.cloudbus.cloudsim.core.Simulation;
import org.cloudbus.cloudsim.core.events.SimEvent;

import java.util.ArrayList;
import java.util.List;

public class DistributedSimulation extends CloudSimEntity {
    private final List<DistributedApplication> applications = new ArrayList<>();
    public DistributedSimulation(Simulation simulation) {
        super(simulation);
    }

    @Override
    protected void startInternal() {
        for (DistributedApplication app: applications) {
            app.start();
        }
    }

    @Override
    public void processEvent(SimEvent evt) {
        //we won't receive any events
        shutdown();
    }

    public void addApplication(DistributedApplication app) {
        applications.add(app);
    }
}
