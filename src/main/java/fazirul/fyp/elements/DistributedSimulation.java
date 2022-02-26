package fazirul.fyp.elements;

import fazirul.fyp.dragon.app.DragonApplication;

import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;


public class DistributedSimulation {
    private final List<DistributedApplication> applications = new ArrayList<>();
    private long timeTaken = 0;

    public void start() {
        LocalTime startTime = LocalTime.now();
        for (DistributedApplication app: applications) {
            app.start();
        }

        for (DistributedApplication app: applications) {
            try {
                app.join();
            } catch(Exception e) {
                e.printStackTrace();
            }
        }

        LocalTime endTime = LocalTime.now();
        timeTaken = startTime.until(endTime, ChronoUnit.MILLIS);
    }

    public void addApplication(DistributedApplication app) {
        applications.add(app);
    }

    public void addApplications(List<DragonApplication> apps) {
        applications.addAll(apps);
    }

    public void printSimulationTime() {
        System.out.println("""
                Time Taken (ms):
                """ + timeTaken);
    }

    public void printTotalMessagesExchanged() {
        int result = 0;
        for (DistributedApplication app: applications) {
            result += app.getTotalMessagesSent();
        }

        System.out.println("Total Messages Exchanged: " + result);
    }

    public void printResults() {
        for (DistributedApplication app: applications) {
            app.printResults();
        }
    }

    public void printTotalResourceConsumption() {
        if (applications.size() == 0) {
            System.out.println("No applications found.");
            return;
        }

        HashMap<Node, ResourceBundle> resourceConsumption = applications.get(0).getFinalResourcesConsumption();

        for (int i = 1; i < applications.size(); i++) {
            HashMap<Node, ResourceBundle> toAdd = applications.get(i).getFinalResourcesConsumption();
            for (Node n: resourceConsumption.keySet()) {
                if (toAdd.containsKey(n)) {
                    resourceConsumption.get(n).addResources(toAdd.get(n));
                }
            }
        }

        for (Node n: resourceConsumption.keySet()) {
            System.out.print("Node " + n.getID() + " consumption: " + resourceConsumption.get(n).toString());
            System.out.println(" exceeded = " + (!n.getTotalResources().isBounded(resourceConsumption.get(n))));
        }
    }
}
