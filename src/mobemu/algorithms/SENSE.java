/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package mobemu.algorithms;

import mobemu.node.*;

/**
 * Class for a SENSE node.
 *
 * Radu-Ioan Ciobanu, Ciprian Dobre, Mihai Dascalu, Stefan Trausan-Matu, and
 * Valentin Cristea. SENSE: A collaborative selfish node detection and incentive
 * mechanism for opportunistic networks. Journal of Network and Computer
 * Applications, 41:240-249, May 2014.
 *
 * @author Radu
 */
public class SENSE extends Node {

    private boolean altruismAnalysis;
    private static Node[] nodes = null;

    /**
     * Instantiates a {@code SENSE} object.
     *
     * @param id ID of the node
     * @param context the context of this node
     * @param socialNetwork the social network as seen by this node
     * @param dataMemorySize the maximum allowed size of the data memory
     * @param exchangeHistorySize the maximum allowed size of the exchange
     * history
     * @param seed the seed for the random number generators if routing is used
     * @param traceStart timestamp of the start of the trace
     * @param traceEnd timestamp of the end of the trace
     * @param altruism {@code true} if altruism computations are performed, {@code false}
     * otherwise
     * @param nodes array of all the nodes in the network
     */
    public SENSE(int id, Context context, boolean[] socialNetwork, int dataMemorySize, int exchangeHistorySize,
            long seed, long traceStart, long traceEnd, boolean altruism, Node[] nodes) {
        super(id, nodes.length, context, socialNetwork, dataMemorySize, exchangeHistorySize, seed, traceStart, traceEnd);

        this.altruismAnalysis = altruism;

        if (SENSE.nodes == null) {
            SENSE.nodes = nodes;
        }
    }

    @Override
    public String getName() {
        return "SENSE";
    }

    @Override
    protected void onDataExchange(Node encounteredNode, long contactDuration, long currentTime) {
        if (!(encounteredNode instanceof SENSE)) {
            return;
        }

        SENSE senseEncounteredNode = (SENSE) encounteredNode;
        int remainingMessages = deliverDirectMessages(senseEncounteredNode, false, contactDuration, currentTime, false);
        int totalMessages = 0;

        // analyze every message from the encountered node's data memory
        for (Message message : senseEncounteredNode.dataMemory) {
            if (totalMessages >= remainingMessages) {
                return;
            }

            // verify that we are in the Spray phase of Spray-and-Wait
            if (message.getCopies(senseEncounteredNode.id) > 1 && message.getDestination() != id) {

                message.setCopies(senseEncounteredNode.id, message.getCopies(senseEncounteredNode.id) / 2);
                message.setCopies(id, message.getCopies(id) + message.getCopies(senseEncounteredNode.id));

                if (altruismAnalysis) {
                    // if the node is not altruistic towards the given message, it will not download it
                    if (computePerceivedAltruism(message, senseEncounteredNode) < Altruism.getTrustThreshold()
                            && !socialNetwork[senseEncounteredNode.id]) {
                        message.setCopies(id, message.getCopies(id) - message.getCopies(senseEncounteredNode.id));

                        // increase perceived altruism value
                        if (socialNetwork[message.getSource()]) {
                            senseEncounteredNode.altruism.increaseLocal();
                        } else {
                            senseEncounteredNode.altruism.increaseGlobal();
                        }

                        continue;
                    }
                }

                insertMessage(message, senseEncounteredNode, currentTime, altruismAnalysis, false);
                totalMessages++;
            }
        }

        // analyze every message from the encountered node's own memory
        for (Message message : senseEncounteredNode.ownMessages) {
            if (totalMessages >= remainingMessages) {
                return;
            }

            // verify that we are in the Spray phase of Spray-and-Wait
            if (message.getCopies(senseEncounteredNode.id) > 1 && message.getDestination() != id) {

                message.setCopies(senseEncounteredNode.id, message.getCopies(senseEncounteredNode.id) / 2);
                message.setCopies(id, message.getCopies(id) + message.getCopies(senseEncounteredNode.id));

                if (altruismAnalysis) {
                    // if the node is not altruistic towards the given message, it will not download it
                    if (computePerceivedAltruism(message, senseEncounteredNode) < Altruism.getTrustThreshold()
                            && !socialNetwork[senseEncounteredNode.id]) {
                        message.setCopies(id, message.getCopies(id) - message.getCopies(senseEncounteredNode.id));

                        // increase perceived altruism value
                        if (socialNetwork[message.getSource()]) {
                            senseEncounteredNode.altruism.increaseLocal();
                        } else {
                            senseEncounteredNode.altruism.increaseGlobal();
                        }

                        continue;
                    }
                }

                insertMessage(message, senseEncounteredNode, currentTime, altruismAnalysis, false);
                totalMessages++;
            }
        }
    }

    /**
     * Computes the current node's perceived altruism for a given message
     * carried by a given node.
     *
     * @param message the message to compute the altruism for
     * @param encounteredNode the node carrying the message
     * @return the perceived altruism value for the message
     */
    private double computePerceivedAltruism(Message message, SENSE encounteredNode) {
        double perceivedAltruism = 0.0;
        double down = 0.0;

        for (ExchangeHistory sent : encounteredNode.exchangeHistorySent) {
            for (ExchangeHistory received : encounteredNode.exchangeHistoryReceived) {
                if (sent.getMessage().getId() == received.getMessage().getId()) {

                    double current = nodes[message.getSource()].inSocialNetwork(sent.getMessage().getSource()) ? 1 : 0;

                    if (current == 0) {
                        continue;
                    }

                    current *= (id == sent.getNodeSeen() ? 1 : 0);

                    if (current == 0) {
                        continue;
                    } else {
                        down++;
                    }

                    current *= (id == received.getNodeSeen() ? 1 : 0);
                    current *= (received.getExchangeTime() > sent.getExchangeTime() ? 1 : 0);

                    if (sent.getBattery() <= Altruism.getMaxBatteryThreshold() * Battery.getMaxLevel()) {
                        current = 1.0;
                    }

                    perceivedAltruism += current;
                }
            }
        }

        if (down == 0) {
            perceivedAltruism = 1;
        }

        return perceivedAltruism;
    }
}
