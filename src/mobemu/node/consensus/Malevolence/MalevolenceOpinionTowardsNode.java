package mobemu.node.consensus.Malevolence;

import mobemu.utils.message.IMessage;

/**
 * Created by radu.dragan on 6/7/2017.
 */
public class MalevolenceOpinionTowardsNode implements IMessage{

    /**
     * The id of the message that generated the current opinion
     */
    public int id;

    /**
     * The value of the opinion
     */
    public double value;

    /**
     * The timestamp of the opinion
     */
    public long timestamp;

    public MalevolenceOpinionTowardsNode(int id, double value, long timestamp) {
        this.id = id;
        this.value = value;
        this.timestamp = timestamp;
    }

    @Override
    public int getId() {
        return id;
    }

    @Override
    public long getTimestamp() {
        return timestamp;
    }

    public double getValue() {
        return value;
    }
}
