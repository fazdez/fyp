package fazirul.fyp.elements;

import org.cloudbus.cloudsim.utilizationmodels.UtilizationModelAbstract;

public class UtilizationModelAbsolute extends UtilizationModelAbstract {
    private final double value;

    public UtilizationModelAbsolute(double value) {
        this.value = value;
        setUnit(Unit.ABSOLUTE);
    }

    @Override
    protected double getUtilizationInternal(double time) {
        return value;
    }
}
