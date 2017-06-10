package mobemu.node.consensus;

import mobemu.node.Message;
import mobemu.utils.Constants;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Created by radu on 5/5/2017.
 */
public class ConsensusMessageReplicas {

    /**
     * Map<SourceId, Value of the message + timestamp>
     */
    private Map<Integer, ConsensusMessageValue> messageReplicas;

    /**
     * Back reference to the node
     */
    private ConsensusLeaderNode node;

    public ConsensusMessageReplicas(ConsensusLeaderNode node){
        this.messageReplicas = new HashMap<>();
        this.node = node;
    }

    public int size(){
        return messageReplicas.size();
    }

    public void add(Message message){
        int sourceId = message.getSource();

        if(messageReplicas.containsKey(sourceId)){
            ConsensusMessageValue consensusMessageValue = messageReplicas.get(sourceId);

            if(message.getTimestamp() > consensusMessageValue.getTimestamp()){
                return;
            }
        }

        messageReplicas.put(sourceId, new ConsensusMessageValue(message));
    }

    public ConsensusMostFrequentValue getDecision(){
        if(messageReplicas.size() < Constants.MIN_REPLICAS)
            return null;

        String mostFrequentValue = null;
        double maxTrust = 0;
        int maxNoOfAppearances = 0;

        Map<String, Integer> appearances = new HashMap<>();
        Map<String, Double> trust = new HashMap<>();
        Iterator it = messageReplicas.entrySet().iterator();
        boolean multipleValues = false;
        while(it.hasNext()){
            Map.Entry<Integer, ConsensusMessageValue> pair = (Map.Entry) it.next();
            String value = pair.getValue().getValue();
            double perceivedTrust = node.getMalevolence().getValue(pair.getKey());
            if(!appearances.containsKey(value)){
                appearances.put(value, 0);
                trust.put(value, 0d);
            }

            //increase the number of appearances for current value;
            appearances.put(value, appearances.get(value) + 1);
            double newTrustValue = trust.get(value) + perceivedTrust;
            trust.put(value, newTrustValue);

            //there are multiple most frequent values so far => Cannot decide
            double x = Math.abs(newTrustValue - maxTrust);
            if(Math.abs(newTrustValue - maxTrust) < Constants.Epsilon){
                multipleValues = true;
                continue;
            }

            if(newTrustValue > maxTrust){
                mostFrequentValue = value;
                maxTrust = newTrustValue;
                multipleValues = false;
                maxNoOfAppearances = appearances.get(value);
            }
        }

        //multiple most frequent values
        if(multipleValues){
//            System.out.println("MultipleValues: " + messageReplicas.size());

            return null;
        }

        if(!mostFrequentValue.equals("1")){
//            System.out.println("Id: " + node.getId() + ", MostFrequentValue: " + mostFrequentValue + ", trust: " +
//                    trust +
//                    ", " +
//                    "replicas: " +
//                    messageReplicas);
        }

        double confidenceLevel = (double)maxNoOfAppearances / messageReplicas.size();
        if(confidenceLevel < Constants.MIN_CONFIDENCE_LEVEL)
            return null;

        return new ConsensusMostFrequentValue(mostFrequentValue, confidenceLevel);
    }

    public void updateMalevolence(ConsensusMostFrequentValue mostFrequentValue, long currentTime){
        for(Map.Entry<Integer, ConsensusMessageValue> pair: messageReplicas.entrySet()){
            int nodeId = pair.getKey();
            ConsensusMessageValue value = pair.getValue();
            double confidenceLevel = mostFrequentValue.getConfidenceLevel();
            if(!value.getValue().equals(mostFrequentValue.getValue())){
                node.getMalevolence().setOwnOpinion(nodeId, node.getId(), 1d-confidenceLevel, currentTime);
                continue;
            }
            node.getMalevolence().setOwnOpinion(nodeId, node.getId(), confidenceLevel, currentTime);
        }
    }

    @Override
    public String toString() {
        return messageReplicas.toString();
    }
}
