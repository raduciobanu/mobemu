package mobemu.node.consensus;

import mobemu.node.Message;

/**
 * Created by radu on 5/5/2017.
 */
public class ConsensusMessageValue {

    private String value;
    private long timestamp;

    public ConsensusMessageValue(Message message) {
        this.value = message.getMessage();
        this.timestamp = message.getTimestamp();
    }

    public String getValue() {
        return value;
    }

    public long getTimestamp() {
        return timestamp;
    }
}
