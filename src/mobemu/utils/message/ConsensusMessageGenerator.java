package mobemu.utils.message;

import mobemu.node.Message;
import mobemu.node.Node;
import mobemu.node.consensus.ConsensusLeaderNode;
import sun.misc.resources.Messages_pt_BR;

import java.util.*;

/**
 * Created by radu on 5/13/2017.
 */
public class ConsensusMessageGenerator extends MessageGenerator {

    /**
     * The id of the message (All messages generated in the same time will have the same messageId)
     */
    private static int messageId;

    /**
     * The percentage of malicious nodes in the network
     */
    private float maliciousPercentage;

    /**
     * The percentage of correct values when generating random messages
     */
    private float correctPercentage;

    /**
     * Dictionary of messageId, messageValue, numberOfAppearancesForValue
     */
    private Map<Integer, Map<String, Integer>> messageValues;

    public ConsensusMessageGenerator(float maliciousPercentage, float correctPercentage) {
        this.messageValues = new HashMap<>();
        this.maliciousPercentage = maliciousPercentage;
        this.correctPercentage = correctPercentage;
    }

    @Override
    public List<Message> generateMessages(Node[] nodes, int messageCount, int messageCopies, long tick, boolean dissemination, Random random) {
        messageId++;
        messageValues.put(messageId, new HashMap<>());
        List<Message> messages = new ArrayList<>();

        for (Node node : nodes) {
            int nodeId = node.getId();

            if (!(node instanceof ConsensusLeaderNode))
                continue;

            ConsensusLeaderNode consensusLeaderNode = (ConsensusLeaderNode) node;
            int leaderId = consensusLeaderNode.getLeaderNode().getLeaderNodeId();
            if (leaderId == -1) {
//                continue;
                leaderId = nodeId;
            }

            boolean isMalicious = isMalicious(random, maliciousPercentage);
            String messageValue = generateMessageValue(random, correctPercentage, isMalicious);

            Message message = new Message(messageId, nodeId, leaderId, messageValue, tick, messageCopies);
            node.generateMessage(message);

            addToMessageValues(messageId, messageValue);
            messages.add(message);

//            System.out.println("Node " + node.getId() + ", leader: " + leaderId);
        }

        return messages;
    }

    public static boolean isMalicious(Random random, double maliciousPercentage){
        return random.nextDouble() < maliciousPercentage;
    }

    public static String generateMessageValue(Random random, double correctPercentage, boolean isMalicious) {
        if(isMalicious){
            if (random.nextDouble() < correctPercentage) {
                return "1";
            }

            return "0";
        }

        return "1";
    }

    public void addToMessageValues(int messageId, String messageValue) {
        Map<String, Integer> valuesForMessageId = messageValues.get(messageId);

        if (!valuesForMessageId.containsKey(messageValue)) {
            valuesForMessageId.put(messageValue, 1);
            return;
        }

        int numberOfAppearances = valuesForMessageId.get(messageValue);
        valuesForMessageId.put(messageValue, ++numberOfAppearances);
    }

    public Map<String, Integer> get(int messageId) {
        return messageValues.get(messageId);
    }

    public int getNumberOfMessages() {
        return messageId;
    }
}
