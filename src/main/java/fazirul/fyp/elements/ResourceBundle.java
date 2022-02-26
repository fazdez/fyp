package fazirul.fyp.elements;


import org.cloudbus.cloudsim.cloudlets.Cloudlet;
import org.cloudbus.cloudsim.utilizationmodels.UtilizationModel;
import org.cloudbus.cloudsim.vms.Vm;

import java.text.MessageFormat;

public class ResourceBundle {
    private static final int CPU = 1;
    private static final int MEMORY = 2;
    private static final int BANDWIDTH = 3;

    private int cpu;
    private int bandwidth;
    private int memory;
    public boolean empty;

    public ResourceBundle(int cpu, int bandwidth, int memory) {
        this.cpu = cpu;
        this.bandwidth = bandwidth;
        this.memory = memory;
    }

    public ResourceBundle(Cloudlet service) {
        this.cpu = (int) service.getNumberOfPes();
        this.bandwidth = (int) service.getUtilizationOfBw();
        this.memory = (int) service.getUtilizationOfRam();

        if (service.getUtilizationModelBw().getUnit() == UtilizationModel.Unit.PERCENTAGE || service.getUtilizationModelRam().getUnit() == UtilizationModel.Unit.PERCENTAGE) {
            System.out.println("creating ResourceBundle: invalid definition of cloudlet");
        }
    }

    public ResourceBundle(Vm virtualMachine) {
        this.cpu = (int) virtualMachine.getNumberOfPes();
        this.bandwidth = (int) virtualMachine.getBw().getCapacity();
        this.memory = (int) virtualMachine.getRam().getCapacity();
    }

    public int getCPU() {
        return this.cpu;
    }

    public int getBandwidth() {
        return bandwidth;
    }

    public int getMemory() {
        return memory;
    }

    public boolean isEmpty() {
        return this.cpu == 0 || this.bandwidth == 0 || this.memory == 0;
    }

    //A.isBounded(B) == true if A has more resources than B
    public boolean isBounded(ResourceBundle resourceDemanded) {
        return this.cpu >= resourceDemanded.cpu && this.memory >= resourceDemanded.memory && this.bandwidth >= resourceDemanded.bandwidth;
    }

    public void addResources(ResourceBundle toAdd) {
        this.cpu += toAdd.cpu;
        this.bandwidth += toAdd.bandwidth;
        this.memory += toAdd.memory;
    }

    public void deductResources(ResourceBundle toDeduct) {
        this.cpu -= toDeduct.cpu;
        this.bandwidth -= toDeduct.bandwidth;
        this.memory -= toDeduct.memory;
    }

    public double normalise(ResourceBundle residualResources) {
        double quadraticSum = 0;
        quadraticSum += this.getQuadraticValue(residualResources, CPU);
        quadraticSum += this.getQuadraticValue(residualResources, MEMORY);
        quadraticSum += this.getQuadraticValue(residualResources, BANDWIDTH);

        return Math.sqrt(quadraticSum);
    }

    private double getQuadraticValue(ResourceBundle residualResources, int type) {
        double average = (residualResources.bandwidth + residualResources.cpu + residualResources.memory)/3.0;
        switch(type) {
            case CPU: //cpu
                return Math.pow(this.cpu*(average/residualResources.cpu), 2);
            case MEMORY: //memory
                return Math.pow(this.memory*(average/residualResources.memory), 2);
            case BANDWIDTH: //bandwidth
                return Math.pow(this.bandwidth*(average/residualResources.bandwidth), 2);
        }

        return 0;
    }

    public ResourceBundle clone() {
        return new ResourceBundle(this.cpu, this.bandwidth, this.memory);
    }

    public String toString() {
        return MessageFormat.format("(cpu = {0}, bw = {1}, memory = {2})", getCPU(), getBandwidth(), getMemory());
    }
}
