package fazirul.fyp.dragon_implementation.utils;

import fazirul.fyp.dragon.app.DragonApplication;
import fazirul.fyp.dragon.utils.Config;
import fazirul.fyp.dragon.utils.Constants;
import fazirul.fyp.elements.EdgeServer;
import fazirul.fyp.elements.ResourceBundle;
import org.cloudbus.cloudsim.cloudlets.Cloudlet;
import org.cloudbus.cloudsim.vms.Vm;
import org.cloudbus.cloudsim.vms.VmSimple;

import java.util.*;

// Singleton class
public class VirtualMachineHandler {
    private static VirtualMachineHandler singleInstance = null;
    private final List<ResourceBundle> virtualMachines;
    private final HashMap<Cloudlet, List<Integer>> serviceMappings = new HashMap<>();
    private static int globalID;

    public VirtualMachineHandler() {
        virtualMachines = Config.getInstance().getFunctions();
    }

    public void addServiceMapping(Cloudlet service, List<Integer> possibleFunctions) {
        serviceMappings.put(service, possibleFunctions);
    }

    //randomly maps a service to feasible virtualMachines
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
        return virtualMachines.get(functionID);
    }

    public void registerApplications(List<DragonApplication> applicationList) {
        for (DragonApplication app: applicationList) {
            for (Cloudlet service: app.getServices()) {
                addServiceMapping(service);
            }
        }
    }

    public Vm createFunction(int functionID) {
        ResourceBundle functionResources = virtualMachines.get(functionID);
        Vm toReturn = new VmSimple(Constants.VM_DEFAULT_MIPS, functionResources.getCPU());
        toReturn.setRam(functionResources.getMemory());
        toReturn.setBw(functionResources.getBandwidth());
        return toReturn;
    }

    public static VirtualMachineHandler getInstance() {
        if (singleInstance == null) {
            singleInstance = new VirtualMachineHandler();
        }
        return singleInstance;
    }

    private List<Integer> getFeasibleFunctions(Cloudlet service) {
        ResourceBundle resourceDemanded = new ResourceBundle(service);
        final List<Integer> result = new ArrayList<>();

        for (int function = 0; function < virtualMachines.size(); function++) {
            if (virtualMachines.get(function).isBounded(resourceDemanded)) {
                result.add(function);
            }
        }

        return result;
    }
}
