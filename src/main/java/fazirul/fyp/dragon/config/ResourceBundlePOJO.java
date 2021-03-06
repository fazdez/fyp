package fazirul.fyp.dragon.config;

/**
 * Plain-Old Java Object of the config for use by {@link com.fasterxml.jackson.databind.ObjectMapper}.
 */
public class ResourceBundlePOJO {
    private int cpu;
    public int getCpu() {
        return cpu;
    }
    public void setCpu(int cpu) {
        this.cpu = cpu;
    }

    private int memory;
    public int getMemory() {
        return memory;
    }
    public void setMemory(int memory) {
        this.memory = memory;
    }

    private int bandwidth;
    public int getBandwidth() {
        return bandwidth;
    }
    public void setBandwidth(int bandwidth) {
        this.bandwidth = bandwidth;
    }
}
