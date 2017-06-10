package mobemu.node.consensus.Malevolence;

import mobemu.utils.Constants;
import mobemu.utils.message.IMessage;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by radu.dragan on 6/5/2017.
 */
public class MalevolenceOpinion implements IMessage {

    /**
     * The source of the opinion
     */
    public int id;

    /**
     * The malevolence value
     */
    public double value;

    /**
     * The timestamp of the opinion
     */
    public long timestamp;

    public List<Integer> messageIds;

    public MalevolenceOpinion(int sourceId, double value, long timeStamp) {
        this.id = sourceId;
        this.value = value;
        this.timestamp = timeStamp;
        this.messageIds = new ArrayList<>();
    }

    @Override
    public int getId() {
        return id;
    }

    public double getValue() {
        return value;
    }

    @Override
    public long getTimestamp() {
        return timestamp;
    }

    private void setValue(int messageId, double value, long currentTime){
        if(messageIds.contains(messageId))
            return;

        messageIds.add(messageId);
        this.value = value;
        this.timestamp = currentTime;
    }

    public void increaseValue(int messageId, long currentTime){
        setValue(messageId, value + Constants.MalavolenceConstant, currentTime);
    }

    public void decreaseValue(int messageId, long currentTime){
        setValue(messageId, value - Constants.MalavolenceConstant, currentTime);
    }
}
