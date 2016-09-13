/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package mobemu.algorithms;

import mobemu.node.*;

/**
 * Class for a Moghadam-Schulzrinne node.
 *
 * Arezu Moghadam and Henning Schulzrinne. Interest-aware content distribution
 * protocol for mobile disruption-tolerant networks. In IEEE International
 * Symposium on a World of Wireless, Mobile and Multimedia Networks Workshops,
 * WoWMoM 2009, pages 1-7, June 2009.
 *
 * @author Radu
 */
public class MoghadamSchulzrinne extends Node {

    private boolean altruismAnalysis;

    /**
     * Instantiates a {@code MoghadamSchulzrinne} object.
     *
     * @param id ID of the node
     * @param nodes total number of existing nodes
     * @param context the context of this node
     * @param socialNetwork the social network as seen by this node
     * @param dataMemorySize the maximum allowed size of the data memory
     * @param exchangeHistorySize the maximum allowed size of the exchange
     * history
     * @param seed the seed for the random number generators
     * @param traceStart timestamp of the start of the trace
     * @param traceEnd timestamp of the end of the trace
     * @param altruism {@code true} if altruism computations are performed, {@code false}
     * otherwise
     */
    public MoghadamSchulzrinne(int id, int nodes, Context context, boolean[] socialNetwork, int dataMemorySize, int exchangeHistorySize,
            long seed, long traceStart, long traceEnd, boolean altruism) {
        super(id, nodes, context, socialNetwork, dataMemorySize, exchangeHistorySize, seed, traceStart, traceEnd);

        this.altruismAnalysis = altruism;
    }

    @Override
    public String getName() {
        return "Moghadam Schulzrinne";
    }

    @Override
    protected void onDataExchange(Node encounteredNode, long contactDuration, long currentTime) {
        if (!(encounteredNode instanceof MoghadamSchulzrinne)) {
            return;
        }

        MoghadamSchulzrinne mogSchuNode = (MoghadamSchulzrinne) encounteredNode;
        int remainingMessages = deliverDirectMessages(mogSchuNode, altruismAnalysis, contactDuration, currentTime, true);
        int totalMessages = 0;

        for (Message message : mogSchuNode.dataMemory) {
            if (totalMessages >= remainingMessages) {
                return;
            }

            if (!dataMemory.contains(message) && !ownMessages.contains(message)
                    && context.getCommonTopics(message.getTags(), currentTime) > 0) {

                if (altruismAnalysis) {
                    if (!mogSchuNode.altruism.isSelfish() && !checkAltruism(mogSchuNode, message)) {
                        altruism.setSelfishness(true);

                        if (context.getCommonTopics(message.getTags(), currentTime) > 0) {
                            altruism.increaseLocal();
                        } else {
                            altruism.increaseGlobal();
                        }

                        continue;
                    } else if (!mogSchuNode.altruism.isSelfish()) {
                        altruism.setSelfishness(false);
                    }
                }

                insertMessage(message, mogSchuNode, currentTime, altruismAnalysis, true);
                totalMessages++;
            }
        }

        for (Message message : mogSchuNode.ownMessages) {
            if (totalMessages >= remainingMessages) {
                return;
            }

            if (!dataMemory.contains(message) && !ownMessages.contains(message)
                    && context.getCommonTopics(message.getTags(), currentTime) > 0) {

                if (altruismAnalysis) {
                    if (!mogSchuNode.altruism.isSelfish() && !checkAltruism(mogSchuNode, message)) {
                        altruism.setSelfishness(true);

                        if (context.getCommonTopics(message.getTags(), currentTime) > 0) {
                            altruism.increaseLocal();
                        } else {
                            altruism.increaseGlobal();
                        }

                        continue;
                    } else if (!mogSchuNode.altruism.isSelfish()) {
                        altruism.setSelfishness(false);
                    }
                }

                insertMessage(message, mogSchuNode, currentTime, altruismAnalysis, true);
                totalMessages++;
            }
        }
    }

    /**
     * Checks the altruism of this node towards a message, from the standpoint
     * of an encountered node.
     *
     * @param encounteredNode the encountered node
     * @param message message to be analyzed
     * @return {@code true} if the message is to be transferred, {@code false}
     * otherwise
     */
    private boolean checkAltruism(MoghadamSchulzrinne encounteredNode, Message message) {
        double perceivedAltruism = 0.0;
        double total = 0.0;

        for (ExchangeHistory sent : encounteredNode.exchangeHistorySent) {
            for (ExchangeHistory received : encounteredNode.exchangeHistoryReceived) {
                if (sent.getNodeSeen() == id && message.getTags().equals(sent.getMessage().getTags())
                        && ((sent.getMessage() == received.getMessage() && received.getExchangeTime() > sent.getExchangeTime())
                        || sent.getBattery() <= Altruism.getMaxBatteryThreshold() * Battery.getMaxLevel())) {
                    perceivedAltruism++;
                    break;
                }
            }

            if (sent.getNodeSeen() == id && message.getTags().equals(sent.getMessage().getTags())) {
                total++;
            }
        }

        perceivedAltruism /= total;

        return total == 0.0 || perceivedAltruism >= Altruism.getTrustThreshold();
    }
}
