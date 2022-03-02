package fazirul.fyp.dragon_implementation.utils;

import fazirul.fyp.dragon_implementation.config.Config;
import fazirul.fyp.dragon.utils.Constants;
import fazirul.fyp.elements.ResourceBundle;
import org.cloudbus.cloudsim.vms.Vm;
import org.cloudbus.cloudsim.vms.VmSimple;

import java.util.*;

/**
 * A singleton class that manages the information on the virtual machines (VMs). In DRAGON context, virtual machines are logically equivalent to functions.
 *
 * <p>The number of VMs are fixed and initialized (from a config file) once at the start of the program.
 * Here we store templates of such VMs in a {@link #virtualMachines list}. Each VM template is represented by {@link ResourceBundle}.
 * Each VM template is uniquely identified via its index in the list.</p>
 */
public class VirtualMachineHandler {
    /**
     * Singleton Class logic
     */
    private static VirtualMachineHandler singleInstance = null;

    /**
     * List of VM templates maintained by the handler. This list is fixed and final.
     */
    private final List<ResourceBundle> virtualMachines;

    public VirtualMachineHandler() {
        virtualMachines = Config.getInstance().getFunctions();
    }

    /**
     * Singleton Class logic
     * @return the single instance of the class
     */
    public static VirtualMachineHandler getInstance() {
        if (singleInstance == null) {
            singleInstance = new VirtualMachineHandler();
        }
        return singleInstance;
    }

    /**
     * @param virtualMachineID the ID of the VM
     * @return the resource usage of specified VM.
     */
    public ResourceBundle getVmResourceUsage(int virtualMachineID) {
        return virtualMachines.get(virtualMachineID);
    }

    /**
     * @param virtualMachineID
     * @return A {@link VmSimple} entity based on the VM template specified by the ID.
     */
    public Vm createVm(int virtualMachineID) {
        ResourceBundle vmResources = virtualMachines.get(virtualMachineID);
        Vm toReturn = new VmSimple(Constants.VM_DEFAULT_MIPS, vmResources.getCPU());
        toReturn.setRam(vmResources.getMemory());
        toReturn.setBw(vmResources.getBandwidth());
        return toReturn;
    }


    /**
     * @param task resource demanded by task
     * @return A list of VM IDs that is able to satisfy the task.
     */
    public List<Integer> getFeasibleVirtualMachinesForTask(ResourceBundle task) {
        List<Integer> result = new ArrayList<>();
        for (int vm = 0; vm < virtualMachines.size(); vm++) {
            if (virtualMachines.get(vm).isBounded(task)) {
                result.add(vm);
            }
        }

        return result;
    }
}
