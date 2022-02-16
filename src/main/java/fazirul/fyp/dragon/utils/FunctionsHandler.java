package fazirul.fyp.dragon.utils;

import fazirul.fyp.dragon.app.DragonApplication;
import fazirul.fyp.elements.ResourceBundle;
import org.cloudbus.cloudsim.cloudlets.Cloudlet;
import org.cloudbus.cloudsim.vms.Vm;
import org.cloudbus.cloudsim.vms.VmSimple;

import java.util.*;

// Singleton class
public class FunctionsHandler {
    private static FunctionsHandler singleInstance = null;
    private final List<ResourceBundle> functions; // function_id = index in the list
    private final HashMap<Cloudlet, List<Integer>> serviceMappings = new HashMap<>();
    private static int globalID;

    public FunctionsHandler() {
        functions = Config.getInstance().getFunctions();
    }

    public void addServiceMapping(Cloudlet service, List<Integer> possibleFunctions) {
        serviceMappings.put(service, possibleFunctions);
    }

    //randomly maps a service to feasible functions
    public void addServiceMapping(Cloudlet service) {
        List<Integer> feasibleFunctions = getFeasibleFunctions(service);
        Collections.shuffle(feasibleFunctions);

        if (feasibleFunctions.size() == 0) { serviceMappings.put(service, new ArrayList<>()); return; }
        int randomNumber = new Random().nextInt(feasibleFunctions.size() + 1);
        if (randomNumber == 0) { randomNumber = 1; }
        serviceMappings.put(service, feasibleFunctions.subList(0, randomNumber));
    }

    public List<Integer> getServiceMapping(Cloudlet service) {
        return serviceMappings.get(service);
    }

    public ResourceBundle getFunctionResourceUsage(int functionID) {
        return functions.get(functionID);
    }

    public void registerApplications(List<DragonApplication> applicationList) {
        for (DragonApplication app: applicationList) {
            for (Cloudlet service: app.getServices()) {
                addServiceMapping(service);
            }
        }
    }

    public Vm createFunction(int functionID) {
        ResourceBundle functionResources = functions.get(functionID);
        Vm toReturn = new VmSimple(Constants.VM_DEFAULT_MIPS, functionResources.getCPU());
        toReturn.setRam(functionResources.getMemory());
        toReturn.setBw(functionResources.getBandwidth());
        return toReturn;
    }

    public static FunctionsHandler getInstance() {
        if (singleInstance == null) {
            singleInstance = new FunctionsHandler();
        }
        return singleInstance;
    }

    private List<Integer> getFeasibleFunctions(Cloudlet service) {
        ResourceBundle resourceDemanded = new ResourceBundle(service);
        final List<Integer> result = new ArrayList<>();

        for (int function = 0; function < functions.size(); function++) {
            if (functions.get(function).isBounded(resourceDemanded)) {
                result.add(function);
            }
        }

        return result;
    }
}
