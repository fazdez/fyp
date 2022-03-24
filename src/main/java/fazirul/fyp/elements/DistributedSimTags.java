package fazirul.fyp.elements;

import org.cloudbus.cloudsim.core.CloudSimTag;

/**
 * As CloudSimTag enums are not extensible, we have to map our own enums to the available enums.
 * <p>Only for use between {@link DistSimManager} and {@link DistributedApplication}.</p>
 */
public class DistributedSimTags {
    public static final CloudSimTag ARRIVAL_EVENT = CloudSimTag.CLOUDLET_SUBMIT;
    public static final CloudSimTag START_ALGORITHM_EVENT = CloudSimTag.CLOUDLET_READY;
    public static final CloudSimTag TASK_OFFLOAD_EVENT = CloudSimTag.CLOUDLET_FINISH;
}
