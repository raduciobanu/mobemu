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

    private long spentTime;

    public DecisionResponse(int sourceId, String value, long spentTime) {
        this.sourceId = sourceId;
        this.value = value;
        this.spentTime = spentTime;
    }

    public int getSourceId() {
        return sourceId;
    }

    public String getValue() {
        return value;
    }

    public long getSpentTime() {
        return spentTime;
    }
}
