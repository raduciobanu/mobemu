/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package mobemu.algorithms;

import mobemu.node.*;

/**
 * Class for an IRONMAN node.
 *
 * Greg Bigwood and Tristan Henderson. IRONMAN: Using social networks to add
 * incentives and reputation to opportunistic networks. In SocialCom/PASSAT,
 * pages 65-72. IEEE, 2011.
 *
 * @author Radu
 */
public class IRONMAN extends Node {

    /**
     * Altruism analysis.
     */
    private boolean altruismAnalysis;

    /**
     * Instantiates an {@code IRONMAN} object.
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
    public IRONMAN(int id, int nodes, Context context, boolean[] socialNetwork, int dataMemorySize, int exchangeHistorySize,
            long seed, long traceStart, long traceEnd, boolean altruism) {
        super(id, nodes, context, socialNetwork, dataMemorySize, exchangeHistorySize, seed, traceStart, traceEnd);

        this.altruismAnalysis = altruism;
    }

    @Override
    public String getName() {
        return "IRONMAN";
    }

    @Override
    protected void onDataExchange(Node encounteredNode, long contactDuration, long currentTime) {
        if (!(encounteredNode instanceof IRONMAN)) {
            return;
        }

        IRONMAN ironmanEncounteredNode = (IRONMAN) encounteredNode;

        // update perceived altruism values
        for (int i = 0; i < socialNetwork.length; i++) {
            altruism.setPerceived(i, altruism.getPerceived(i) + ironmanEncounteredNode.altruism.getPerceived(i));
        }

        int remainingMessages = deliverDirectMessages(ironmanEncounteredNode, altruismAnalysis, contactDuration, currentTime, false);
        int totalMessages = 0;

        if (altruismAnalysis) {
            updatePerceivedData(ironmanEncounteredNode);
        }

        // look for messages that aren't in the node's data memory or in the transferred list,
        // that have more than one copy available, and that are not addressed to the current node
        for (Message message : ironmanEncounteredNode.dataMemory) {
            if (totalMessages >= remainingMessages) {
                return;
            }

            if (!dataMemory.contains(message) && !ownMessages.contains(message)
                    && message.getCopies(ironmanEncounteredNode.id) > 1 && message.getDestination() != id) {

                message.setCopies(ironmanEncounteredNode.id, message.getCopies(ironmanEncounteredNode.id) / 2);
                message.setCopies(id, message.getCopies(ironmanEncounteredNode.id));

                if (altruismAnalysis) {
                    if (message.getSource() == ironmanEncounteredNode.id && altruism.getPerceived(ironmanEncounteredNode.id) < Altruism.getTrustThreshold()) {
                        message.setCopies(id, message.getCopies(id) - message.getCopies(ironmanEncounteredNode.id));

                        ironmanEncounteredNode.altruism.increaseLocal();
                        ironmanEncounteredNode.altruism.increaseGlobal();
                        continue;
                    }
                }

                if (message.getSource() != ironmanEncounteredNode.id) {
                    altruism.increasePerceived(ironmanEncounteredNode.id);
                }

                insertMessage(message, ironmanEncounteredNode, currentTime, altruismAnalysis, false);
                totalMessages++;
            }
        }

        // look for messages that aren't in the node's data memory or in the transferred list, that have more than one copy available, and
        // that are not addressed to the current node (because this situation has been treated in deliverDirectMessages)
        for (Message message : ironmanEncounteredNode.ownMessages) {
            if (totalMessages >= remainingMessages) {
                return;
            }

            if (!dataMemory.contains(message) && !ownMessages.contains(message)
                    && message.getCopies(ironmanEncounteredNode.id) > 1 && message.getDestination() != id) {

                message.setCopies(ironmanEncounteredNode.id, message.getCopies(ironmanEncounteredNode.id) / 2);
                message.setCopies(id, message.getCopies(ironmanEncounteredNode.id));

                if (altruismAnalysis) {
                    if (message.getSource() == ironmanEncounteredNode.id && altruism.getPerceived(ironmanEncounteredNode.id) < Altruism.getTrustThreshold()) {
                        message.setCopies(id, message.getCopies(id) - message.getCopies(ironmanEncounteredNode.id));

                        ironmanEncounteredNode.altruism.increaseLocal();
                        ironmanEncounteredNode.altruism.increaseGlobal();
                        continue;
                    }
                }

                if (message.getSource() != ironmanEncounteredNode.id) {
                    altruism.increasePerceived(ironmanEncounteredNode.id);
                }

                insertMessage(message, ironmanEncounteredNode, currentTime, altruismAnalysis, false);
                totalMessages++;
            }
        }
    }

    /**
     * Updates the perceived altruism information about the other nodes, upon a
     * contact.
     *
     * @param ironmanEncounteredNode the encountered node
     */
    public void updatePerceivedData(IRONMAN ironmanEncounteredNode) {

        // get moment of last encounter with encounteredNode
        long lastEncounter = Long.MAX_VALUE;
        for (int i = exchangeHistorySent.size() - 1; i >= 0; i--) {
            if (exchangeHistorySent.get(i).getNodeSeen() == ironmanEncounteredNode.id) {
                lastEncounter = exchangeHistorySent.get(i).getExchangeTime();
                break;
            }
        }
        for (int i = exchangeHistoryReceived.size() - 1; i >= 0; i--) {
            if (exchangeHistoryReceived.get(i).getNodeSeen() == ironmanEncounteredNode.id) {
                if (exchangeHistoryReceived.get(i).getExchangeTime() > lastEncounter) {
                    lastEncounter = exchangeHistoryReceived.get(i).getExchangeTime();
                }
                break;
            }
        }

        for (ExchangeHistory exInfo : ironmanEncounteredNode.exchangeHistorySent) {
            if (exInfo.getExchangeTime() > lastEncounter) {
                if (exInfo.getMessage().getDestination() == id) {

                    // get moment of last encounter with exInfo.nodeSeen
                    long lastEncounterNodeSeen = Long.MAX_VALUE;
                    for (int i = exchangeHistorySent.size() - 1; i >= 0; i--) {
                        if (exchangeHistorySent.get(i).getNodeSeen() == exInfo.getNodeSeen()) {
                            lastEncounterNodeSeen = exchangeHistorySent.get(i).getExchangeTime();
                            break;
                        }
                    }
                    for (int i = exchangeHistoryReceived.size() - 1; i >= 0; i--) {
                        if (exchangeHistoryReceived.get(i).getNodeSeen() == exInfo.getNodeSeen()) {
                            if (exchangeHistoryReceived.get(i).getExchangeTime() > lastEncounterNodeSeen) {
                                lastEncounterNodeSeen = exchangeHistoryReceived.get(i).getExchangeTime();
                            }
                            break;
                        }
                    }

                    if (lastEncounterNodeSeen > lastEncounter) {
                        boolean received = false;

                        for (ExchangeHistory exInfoSeen : exchangeHistoryReceived) {
                            if (exInfoSeen.getMessage().getId() == exInfo.getMessage().getId()
                                    && exInfoSeen.getNodeSeen() == exInfo.getNodeSeen()) {
                                received = true;
                                break;
                            }
                        }

                        if (!received) {
                            altruism.increasePerceived(exInfo.getNodeSeen());
                        }
                    }
                }
            }
        }
    }
}
