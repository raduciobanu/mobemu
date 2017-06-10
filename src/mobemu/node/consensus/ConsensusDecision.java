package mobemu.node.consensus;

import mobemu.utils.message.IMessage;

/**
 * Created by radu on 5/5/2017.
 */
public class ConsensusDecision implements IMessage{

    /**
     * The id of the message
     */
    private int messageId;

    /**
     * The id of the leader that generated the decision
     */
    private int sourceId;

    /**
     * The timestamp when the decision was made
     */
    private long currentTime;

    /**
     * The most frequent value found so far for the given messageId
     */
    private ConsensusMostFrequentValue value;

    /**
     *
     * @param messageId the id of the message considered for the decision
     * @param sourceId the id of the leader that generated the decision
     * @param currentTime timestamp of the decision
     * @param value the most frequent value
     */
    public ConsensusDecision(int messageId, int sourceId, long currentTime, ConsensusMostFrequentValue value){
        this.messageId = messageId;
        this.sourceId = sourceId;
        this.currentTime = currentTime;
        this.value = value;
    }

    public int getSourceId() {
        return sourceId;
    }

    public double getConfidenceLevel(){
        return value.getConfidenceLevel();
    }


    @Override
    public int getId() {
        return messageId;
    }

    public String getValue() {
        if(value == null)
            return "";

        return value.getValue();
    }

    @Override
    public long getTimestamp() {
        return currentTime;
    }

    public boolean fullMatch(int messageId, int leaderId){
        return this.messageId == messageId && this.sourceId == leaderId;
    }

    public boolean semiMatch(int messageId){
        return this.messageId == messageId;
    }
}
