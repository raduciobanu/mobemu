package mobemu.node.leader;

/**
 * Created by radu on 1/15/2017.
 */
public abstract class LeaderMessage{
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
     * The generation time of the heartbeat
     */
    private long timestamp;

    public LeaderMessage(int sourceId, int destinationId, long timestamp){
        this.id = heartBeatCount++;
        this.sourceId = sourceId;
        this.destinationId = destinationId;
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

    public long getTimestamp() {
        return timestamp;
    }

}
