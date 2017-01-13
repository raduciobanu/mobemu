package mobemu.node.leaderElection.dto;

/**
 * Created by radu on 1/13/2017.
 */
public class LeaderHeartBeat {
    protected static int heartBeatCount = 0; // total number of messages generated

    /**
     * The id of the heartbeat
     */
    private int id;

    /**
     * The id of the source of the heartbeat
     */
    private int sourceId;

    /**
     * The id of the destination of the heartbeat
     */
    private int destinationId;

    /**
     * Whether the heart beat is a request or a response
     */
    private boolean request;

    /**
     * The generation time of the heartbeat
     */
    private long timestamp;

    public LeaderHeartBeat(int sourceId, int destinationId, boolean request, long timestamp) {
        this.id = heartBeatCount++;
        this.sourceId = sourceId;
        this.destinationId = destinationId;
        this.request = request;
        this.timestamp = timestamp;
    }

    public int getId() {
        return id;
    }

    public int getSourceId() {
        return sourceId;
    }

    public int getDestinationId() {
        return destinationId;
    }

    public boolean isRequest() {
        return request;
    }

    public long getTimestamp() {
        return timestamp;
    }
}
