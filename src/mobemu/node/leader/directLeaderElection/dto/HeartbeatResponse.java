package mobemu.node.leader.directLeaderElection.dto;

/**
 * Created by radu on 1/18/2017.
 */
public class HeartbeatResponse {

    private long responseTime;

    private int hopCount;

    public HeartbeatResponse(long responseTime, int hopCount) {
        this.responseTime = responseTime;
        this.hopCount = hopCount;
    }

    public long getResponseTime() {
        return responseTime;
    }

    public int getHopCount() {
        return hopCount;
    }
}
