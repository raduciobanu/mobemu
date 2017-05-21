package mobemu.node.consensus;

import mobemu.node.Message;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by radu on 5/5/2017.
 */
public class ConsensusMessageDictionary {
    /**
     * Dictionary<MessageId, ConsensusMessageReplicas>>
     */
    private Map<Integer, ConsensusMessageReplicas> requestDictionary;

    private int decisionThreshold;

    public ConsensusMessageDictionary(int decisionThreshold){
        requestDictionary = new HashMap<>();
        this.decisionThreshold = decisionThreshold;
    }

    public ConsensusMessageDictionary(){
        requestDictionary = new HashMap<>();
    }

    public void add(Message message){
        int messageId = message.getId();
        ConsensusMessageReplicas messagesFromSource;

        if(requestDictionary.containsKey(messageId)){
            messagesFromSource = requestDictionary.get(messageId);
        }else{
            messagesFromSource = new ConsensusMessageReplicas();
        }

        messagesFromSource.add(message);
        requestDictionary.put(messageId, messagesFromSource);
    }

    public int getNumberOfReplicasForMessageId(int messageId){
        return requestDictionary.get(messageId).size();
    }

    public ConsensusDecision getDecision(int messageId, int sourceId, long currentTime){
//        if(getNumberOfReplicasForMessageId(messageId) < decisionThreshold)
//            return null;

        if(!requestDictionary.containsKey(messageId)){
            return null;
        }

        ConsensusMostFrequentValue mostFrequentValue = requestDictionary.get(messageId).getDecision();
        return new ConsensusDecision(messageId, sourceId, currentTime, mostFrequentValue);

    }
}
