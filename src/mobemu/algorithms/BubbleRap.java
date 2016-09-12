/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package mobemu.algorithms;

import mobemu.node.Context;
import mobemu.node.Message;
import mobemu.node.Node;

/**
 * Class for a BUBBLE Rap node.
 *
 * Pan Hui, Jon Crowcroft, and Eiko Yoneki. BUBBLE Rap: social-based forwarding
 * in delay tolerant networks. In Proceedings of the 9th ACM International
 * Symposium on Mobile Ad Hoc Networking and Computing, MobiHoc '08, pages
 * 241-250, New York, USA, 2008. ACM.
 *
 * @author Radu
 */
public class BubbleRap extends Node {

    /**
     * Instantiates a {@code BubbleRap} object.
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
     */
    public BubbleRap(int id, int nodes, Context context, boolean[] socialNetwork, int dataMemorySize, int exchangeHistorySize, long seed, long traceStart, long traceEnd) {
        super(id, nodes, context, socialNetwork, dataMemorySize, exchangeHistorySize, seed, traceStart, traceEnd);
    }

    @Override
    public String getName() {
        return "Bubble Rap";
    }

    @Override
    protected void onDataExchange(Node encounteredNode, long contactDuration, long currentTime) {
        if (!(encounteredNode instanceof BubbleRap)) {
            return;
        }

        BubbleRap bubbleRapEncounteredNode = (BubbleRap) encounteredNode;
        int remainingMessages = deliverDirectMessages(bubbleRapEncounteredNode, false, contactDuration, currentTime, false);
        int totalMessages = 0;

        for (Message message : bubbleRapEncounteredNode.dataMemory) {
            if (totalMessages >= remainingMessages) {
                return;
            }

            if (bubbleRapEncounteredNode.inLocalCommunity(message.getDestination())) {
                // if the encountered node is in the same community as the message's destination, use the local centrality
                if (inLocalCommunity(message.getDestination()) && (getCentrality(true) > bubbleRapEncounteredNode.getCentrality(true))) {
                    insertMessage(message, bubbleRapEncounteredNode, currentTime, false, false);
                    totalMessages++;
                }
            } else {
                // otherwise, use the global centrality
                if (inLocalCommunity(message.getDestination()) || (getCentrality(false) > bubbleRapEncounteredNode.getCentrality(false))) {
                    insertMessage(message, bubbleRapEncounteredNode, currentTime, false, false);
                    totalMessages++;
                }
            }
        }

        for (Message message : bubbleRapEncounteredNode.ownMessages) {
            if (totalMessages >= remainingMessages) {
                return;
            }

            if (bubbleRapEncounteredNode.inLocalCommunity(message.getDestination())) {
                // if the encountered node is in the same community as the message's destination, use the local centrality
                if (inLocalCommunity(message.getDestination()) && (getCentrality(true) > bubbleRapEncounteredNode.getCentrality(true))) {
                    insertMessage(message, bubbleRapEncounteredNode, currentTime, false, false);
                    totalMessages++;
                }
            } else {
                // otherwise, use the global centrality
                if (inLocalCommunity(message.getDestination()) || (getCentrality(false) > bubbleRapEncounteredNode.getCentrality(false))) {
                    insertMessage(message, bubbleRapEncounteredNode, currentTime, false, false);
                    totalMessages++;
                }
            }
        }
    }
}
