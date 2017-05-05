package mobemu.node.leader.directLeaderElection.dto;

/**
 * Created by radu on 1/18/2017.
 */
public class HeartbeatResponse {

    private int heartbeatId;

    private long responseTime;

    private int hopCount;

    public HeartbeatResponse(int heartbeatId, long responseTime, int hopCount) {
        this.heartbeatId = heartbeatId;
        this.responseTime = responseTime;
        this.hopCount = hopCount;
    }

    public int getHeartbeatId(){
        return heartbeatId;
    }

    public long getResponseTime() {
        return responseTime;
    }

    public int getHopCount() {
        return hopCount;
    }

    public String toString(){
        return responseTime + "";
    }
}
