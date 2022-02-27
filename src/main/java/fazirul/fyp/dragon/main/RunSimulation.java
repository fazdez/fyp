package fazirul.fyp.dragon.main;

import fazirul.fyp.dragon.app.DragonApplication;
import fazirul.fyp.dragon.utils.Config;
import fazirul.fyp.dragon.utils.VirtualMachineHandler;
import fazirul.fyp.elements.DistributedSimulation;
import fazirul.fyp.elements.Node;
import org.cloudbus.cloudsim.core.CloudSim;

import java.util.HashSet;
import java.util.List;

public class RunSimulation {
    public static void main(String[] args) {
        // initialization
        Config configManager = Config.getInstance();
        CloudSim simulation = new CloudSim();
        VirtualMachineHandler virtualMachineHandler = VirtualMachineHandler.getInstance();

        HashSet<Node> nodes = configManager.getNodes(simulation);
        List<DragonApplication> applicationList = configManager.getApplications(nodes);
        virtualMachineHandler.registerApplications(applicationList);

        DistributedSimulation distributedSimulation = new DistributedSimulation();
        distributedSimulation.addApplications(applicationList);

        distributedSimulation.start();
        simulation.start();

        distributedSimulation.printResults();
        distributedSimulation.printSimulationTime();
        distributedSimulation.printTotalMessagesExchanged();
        distributedSimulation.printTotalResourceConsumption();
    }

    // ISSUES :
    // 1) CHECK CONSUMPTION OF NODE, MAYBE SOMETHING WRONG WITH THE NODERESIDUALRESOURCES? --> could be because election fails but it still offloads. (ok it's not)
    // 2) CHECK MESSAGES EXCHANGEDDDDD (shouldn't just be four??) ok this is done, because of the broadcast() should be OUTSIDE of result.outvoted.
    // 3) CHECK ELECTION (RUNS FOREVER) --> probably the blacklisted shit (done i guess)
}
