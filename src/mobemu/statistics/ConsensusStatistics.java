package mobemu.statistics;

import mobemu.node.Message;
import mobemu.node.Node;
import mobemu.node.consensus.ConsensusLeaderNode;
import mobemu.utils.message.ConsensusMessageGenerator;

import java.util.*;

/**
 * Created by radu on 5/7/2017.
 */
public class ConsensusStatistics extends BaseStatisticsWriteToFile<Message, ConsensusMessageGenerator> {

    /**
     * The percent of correct values when generating random messages
     */
    private float correctPercentage;

    public ConsensusStatistics(float correctPercentage) {
        super(new ConsensusMessageGenerator(correctPercentage));
        this.correctPercentage = correctPercentage;
    }

    @Override
    public void runEveryTick(Node[] nodes, long tick, long startTime) {

    }

    @Override
    public void runBeforeTraceStart() {
        super.runBeforeTraceStart();
    }

    @Override
    public void runAfterTraceEnd(Node[] nodes) {
        computeCorrectnessPercentage(nodes);

        super.runAfterTraceEnd(nodes);
    }

    public void computeCorrectnessPercentage(Node[] nodes) {
        int correctDecisions = 0;
        int decisionNumber = 0;
        for (int msgId = 1; msgId <= messageGenerator.getNumberOfMessages(); msgId++) {
            for (Node node : nodes) {
                if (!(node instanceof ConsensusLeaderNode)) {
                    continue;
                }

                ConsensusLeaderNode consensusNode = (ConsensusLeaderNode) node;

                String decisionValue = consensusNode.getDecisionValueForMessageId(msgId);
                if (decisionValue == "") {
                    continue;
                }

                decisionNumber++;
                if (decisionValue.equals(getMostFrequentValueForMessage(msgId))) {
                    correctDecisions++;
                }
            }
        }

        int messageNumber = nodes.length * messageGenerator.getNumberOfMessages();
        double correctnessPercentage =  (double)correctDecisions / (double)messageNumber;
        double decisionPercentage = (double)decisionNumber / (double)messageNumber;

        System.out.println("Correctness percentage: " + correctnessPercentage);
        System.out.println("Decision percentage: " + decisionPercentage);
        System.out.println("Generation percentage: " + correctPercentage);
    }

    public String getMostFrequentValueForMessage(int msgId) {
        Map<String, Integer> valuesForMessage = messageGenerator.get(msgId);
        int maxAppearances = 0;
        String mostFrequentValue = "";

        for (Map.Entry<String, Integer> entry : valuesForMessage.entrySet()) {
            if (entry.getValue() > maxAppearances) {
                maxAppearances = entry.getValue();
                mostFrequentValue = entry.getKey();
            }
        }

        return mostFrequentValue;
    }
}
