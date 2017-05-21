package mobemu.node.consensus;

import mobemu.node.Message;

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

    public ConsensusMessageReplicas(){
        this.messageReplicas = new HashMap<>();
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
        String mostFrequentValue = null;
        int maxNoOfAppearances = 0;

        Map<String, Integer> appearances = new HashMap<>();

        Iterator it = messageReplicas.entrySet().iterator();

        while(it.hasNext()){
            Map.Entry<Integer, ConsensusMessageValue> pair = (Map.Entry) it.next();
            String value = pair.getValue().getValue();
            if(!appearances.containsKey(value)){
                appearances.put(value, 0);
            }

            //increase the number of appearances for current value;
            int noOfAppearances = appearances.get(value);
            noOfAppearances++;
            appearances.put(value, noOfAppearances);

            if(noOfAppearances > maxNoOfAppearances){
                mostFrequentValue = value;
                maxNoOfAppearances = noOfAppearances;
            }
        }

        double confidenceLevel = (double)(maxNoOfAppearances / messageReplicas.size());
        return new ConsensusMostFrequentValue(mostFrequentValue, confidenceLevel);
    }

}
