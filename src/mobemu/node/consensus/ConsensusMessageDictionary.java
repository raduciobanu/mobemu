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

    /**
     * Back reference to the node for getting trust values for other nodes
     */
    private ConsensusLeaderNode node;

    private int decisionThreshold;

    public ConsensusMessageDictionary(int decisionThreshold, ConsensusLeaderNode node){
        this(node);
        this.decisionThreshold = decisionThreshold;
    }

    public ConsensusMessageDictionary(ConsensusLeaderNode node){
        requestDictionary = new HashMap<>();
        this.node = node;
    }

    public void add(Message message){
        int messageId = message.getId();
        ConsensusMessageReplicas messagesFromSource;

        if(requestDictionary.containsKey(messageId)){
            messagesFromSource = requestDictionary.get(messageId);
        }else{
            messagesFromSource = new ConsensusMessageReplicas(node);
        }

        messagesFromSource.add(message);
        requestDictionary.put(messageId, messagesFromSource);
    }

    public int getNumberOfReplicasForMessageId(int messageId){
        return requestDictionary.get(messageId).size();
    }

    public ConsensusDecision getDecision(int messageId, int sourceId, long currentTime){
        if(!requestDictionary.containsKey(messageId)){
            return null;
        }

        ConsensusMostFrequentValue mostFrequentValue = requestDictionary.get(messageId).getDecision();
        if(mostFrequentValue == null)
            return null;

        requestDictionary.get(messageId).updateMalevolence(mostFrequentValue, currentTime);
        return new ConsensusDecision(messageId, sourceId, currentTime, mostFrequentValue);
    }
}
