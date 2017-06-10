package mobemu.node.consensus;

/**
 * Created by radu on 5/27/2017.
 */
public class DecisionResponse {
    /**
     * The id of the source of the request
     */
    private int sourceId;

    private String value;

    private long timestamp;

    private long spentTime;

    public DecisionResponse(int sourceId, String value, long timestamp, long spentTime) {
        this.sourceId = sourceId;
        this.value = value;
        this.timestamp = timestamp;
        this.spentTime = spentTime;
    }

    public int getSourceId() {
        return sourceId;
    }

    public String getValue() {
        return value;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public long getSpentTime() {
        return spentTime;
    }
}
