/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package mobemu.algorithms;

import mobemu.node.*;

/**
 * Class for a dissemination-based ML-SOR node.
 *
 * Annalisa Socievole, Eiko Yoneki, Floriano De Rango, and Jon Crowcroft.
 * Opportunistic message routing using multi-layer social networks. In
 * Proceedings of the 2nd ACM Workshop on High Performance Mobile Opportunistic
 * Systems, HP-MOSys '13, pages 39-46, New York, NY, USA, 2013. ACM.
 *
 * @author Radu
 */
public class MLSOR extends Node {

    /**
     * Altruism analysis.
     */
    private boolean altruismAnalysis;
    /**
     * Information about all the other nodes in the trace.
     */
    private static Node[] nodes = null;

    /**
     * Instantiates an {@code MLSOR} object.
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
    public MLSOR(int id, Context context, boolean[] socialNetwork, int dataMemorySize, int exchangeHistorySize,
            long seed, long traceStart, long traceEnd, boolean altruism, Node[] nodes) {
        super(id, nodes.length, context, socialNetwork, dataMemorySize, exchangeHistorySize, seed, traceStart, traceEnd);

        this.altruismAnalysis = altruism;

        if (MLSOR.nodes == null) {
            MLSOR.nodes = nodes;
        }
    }

    @Override
    protected void onDataExchange(Node encounteredNode, long contactDuration, long currentTime) {
        if (!(encounteredNode instanceof MLSOR)) {
            return;
        }

        MLSOR mlsorEncounteredNode = (MLSOR) encounteredNode;
        int remainingMessages = deliverDirectMessages(mlsorEncounteredNode, altruismAnalysis, contactDuration, currentTime, true);
        int totalMessages = 0;

        for (Message message : mlsorEncounteredNode.dataMemory) {
            if (totalMessages >= remainingMessages) {
                return;
            }

            if (!dataMemory.contains(message) && !ownMessages.contains(message)
                    && shouldDownload(message, mlsorEncounteredNode, currentTime)) {
                if (altruismAnalysis) {
                    if (!mlsorEncounteredNode.altruism.isSelfish() && !checkAltruism(mlsorEncounteredNode, message)) {
                        altruism.setSelfishness(true);

                        if (context.getCommonTopics(message.getTags(), currentTime) > 0) {
                            altruism.increaseLocal();
                        } else {
                            altruism.increaseGlobal();
                        }

                        continue;
                    } else if (!mlsorEncounteredNode.altruism.isSelfish()) {
                        altruism.setSelfishness(false);
                    }
                }

                insertMessage(message, mlsorEncounteredNode, currentTime, altruismAnalysis, true);
                totalMessages++;
            }
        }

        for (Message message : mlsorEncounteredNode.ownMessages) {
            if (totalMessages >= remainingMessages) {
                return;
            }

            if (!dataMemory.contains(message) && !ownMessages.contains(message)
                    && shouldDownload(message, mlsorEncounteredNode, currentTime)) {

                if (altruismAnalysis) {
                    if (!mlsorEncounteredNode.altruism.isSelfish() && !checkAltruism(mlsorEncounteredNode, message)) {
                        altruism.setSelfishness(true);

                        if (context.getCommonTopics(message.getTags(), currentTime) > 0) {
                            altruism.increaseLocal();
                        } else {
                            altruism.increaseGlobal();
                        }

                        continue;
                    } else if (!mlsorEncounteredNode.altruism.isSelfish()) {
                        altruism.setSelfishness(false);
                    }
                }

                insertMessage(message, mlsorEncounteredNode, currentTime, altruismAnalysis, true);
                totalMessages++;
            }
        }
    }

    @Override
    public String getName() {
        return "ML-SOR";
    }

    /**
     * Checks whether a message should be downloaded by the current node, based
     * on the ML-SOR algorithm.
     *
     * @param message message to be checked
     * @param encounteredNode encountered node
     * @param currentTime current trace time
     * @return {@code true} if the message should be downloaded, {@code false}
     * otherwise
     */
    private boolean shouldDownload(Message message, MLSOR encounteredNode, long currentTime) {
        /*
         * compute CS(node, encountered) as: C(node) / (C(node) +
         * C(encountered))
         */
        double thisCS = getCentrality(false) + encounteredNode.getCentrality(false);
        thisCS = (thisCS == 0) ? 0 : (getCentrality(false) / thisCS);
        double encounteredCS = getCentrality(false) + encounteredNode.getCentrality(false);
        encounteredCS = (encounteredCS == 0) ? 0 : (encounteredNode.getCentrality(false) / encounteredCS);

        /*
         * compute TSS(node, encountered, source) as: TS(node, source) /
         * (TS(node, source) + TS(encountered, source))
         */
        int TS1 = socialNetwork[message.getSource()] ? 1 : 0;
        int TS2 = encounteredNode.socialNetwork[message.getSource()] ? 1 : 0;
        double thisTSS = TS1 + TS2;
        thisTSS = (thisTSS == 0) ? 0 : (TS1 / thisTSS);
        double encounteredTSS = TS1 + TS2;
        encounteredTSS = (encounteredTSS == 0) ? 0 : (TS2 / encounteredTSS);

        /*
         * compute LPS(node, encountered, source) as: LP(node, source) /
         * (LP(node, source) + LP(encountered, source))
         */
        double LP1 = (double) context.getCommonTopics(nodes[message.getSource()].getContext(), currentTime);
        if (context.getNumberOfTopics(currentTime) == 0 && nodes[message.getSource()].getContext().getNumberOfTopics(currentTime) == 0) {
            LP1 = 0.0;
        } else {
            LP1 = LP1 / (context.getNumberOfTopics(currentTime) + nodes[message.getSource()].getContext().getNumberOfTopics(currentTime) - LP1);
        }

        double LP2 = (double) encounteredNode.context.getCommonTopics(nodes[message.getSource()].getContext(), currentTime);
        if (encounteredNode.context.getNumberOfTopics(currentTime) == 0 && nodes[message.getSource()].getContext().getNumberOfTopics(currentTime) == 0) {
            LP2 = 0.0;
        } else {
            LP2 = LP2 / (encounteredNode.context.getNumberOfTopics(currentTime) + nodes[message.getSource()].getContext().getNumberOfTopics(currentTime) - LP2);
        }

        double thisLPS = LP1 + LP2;
        thisLPS = (thisLPS == 0) ? 0 : (LP1 / thisLPS);
        double encounteredLPS = LP1 + LP2;
        encounteredLPS = (encounteredLPS == 0) ? 0 : (LP2 / encounteredLPS);

        /*
         * compute MLS(node, encountered, source) as: CS(node, encountered) * (1
         * + TSS(node, encountered, source) + LPS(node, encountered, source))
         */
        double thisMLS = thisCS * (1 + thisTSS + thisLPS);
        double encounteredMLS = encounteredCS * (1 + encounteredTSS + encounteredLPS);

        return thisMLS >= encounteredMLS;
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
    private boolean checkAltruism(MLSOR encounteredNode, Message message) {
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
