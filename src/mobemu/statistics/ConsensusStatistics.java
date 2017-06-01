package mobemu.statistics;

import mobemu.node.Message;
import mobemu.node.Node;
import mobemu.node.consensus.ConsensusLeaderNode;
import mobemu.node.consensus.DecisionResponse;
import mobemu.utils.Constants;
import mobemu.utils.message.ConsensusMessageGenerator;

import java.io.PrintWriter;
import java.text.DecimalFormat;
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
        int decisionsRequestedNumber = 0;
        long decisionLatency = 0;
//        PrintWriter latencies = super.openFile(Constants.deliveryLatenciesFileName);
        DecimalFormat numberFormat = new DecimalFormat("#.00");
        for (int msgId = 1; msgId <= messageGenerator.getNumberOfMessages(); msgId++) {
            for (Node node : nodes) {
                if (!(node instanceof ConsensusLeaderNode)) {
                    continue;
                }

                ConsensusLeaderNode consensusNode = (ConsensusLeaderNode) node;

                if(!consensusNode.hasRequestForMessageId(msgId))
                    continue;

                decisionsRequestedNumber++;

                DecisionResponse response = consensusNode.getDecisionValueForMessageId(msgId);
                if (response == null) {
                    continue;
                }

                String decisionValue = response.getValue();
                decisionNumber++;
                if (decisionValue.equals(getMostFrequentValueForMessage(msgId))) {
                    correctDecisions++;
                }
                decisionLatency += response.getSpentTime();

                double responseLatency = response.getSpentTime();
                if(responseLatency != 0){
                    double deliveryLatencyInHours =  responseLatency / (double) (1000 * 60 * 60);
//                    latencies.println(numberFormat.format(deliveryLatencyInHours));
                }

            }
        }
//        latencies.close();

//        int messageNumber = nodes.length * messageGenerator.getNumberOfMessages();

        double correctnessPercentage =  (double)correctDecisions / (double)decisionNumber;
        double decisionPercentage = (double)decisionNumber / (double)decisionsRequestedNumber;
        double meanDecisionLatency = (double)decisionLatency / (decisionNumber * 1000 * 60 * 60);


        String formattedCorrectness = numberFormat.format(correctnessPercentage);
        String formattedDecision = numberFormat.format(decisionPercentage);
        String formattedMeanDecisionLatency = numberFormat.format(meanDecisionLatency);

        writer.println(formattedCorrectness);

        System.out.println("Correctness percentage: " + formattedCorrectness);
        System.out.println("Decision percentage: " + formattedDecision);
        System.out.println("Mean Decision latency: " + formattedMeanDecisionLatency);
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
