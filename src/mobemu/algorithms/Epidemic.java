/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package mobemu.algorithms;

import mobemu.node.Context;
import mobemu.node.Message;
import mobemu.node.Node;

/**
 * Class for an Epidemic node.
 *
 * @author Radu
 */
public class Epidemic extends Node {

    private boolean dissemination;
    private boolean altruismAnalysis;

    /**
     * Instantiates an {@code Epidemic} object.
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
     * @param dissemination {@code true} if dissemination is used, {@code false}
     * if routing is used
     * @param altruism {@code true} if altruism computations are performed, {@code false}
     * otherwise
     */
    public Epidemic(int id, int nodes, Context context, boolean[] socialNetwork, int dataMemorySize, int exchangeHistorySize,
            long seed, long traceStart, long traceEnd, boolean dissemination, boolean altruism) {
        super(id, nodes, context, socialNetwork, dataMemorySize, exchangeHistorySize, seed, traceStart, traceEnd);

        this.dissemination = dissemination;
        this.altruismAnalysis = altruism;
    }

    @Override
    protected void onDataExchange(Node encounteredNode, long contactDuration, long currentTime) {
        if (!(encounteredNode instanceof Epidemic)) {
            return;
        }

        Epidemic epidemicEncounteredNode = (Epidemic) encounteredNode;
        int remainingMessages = deliverDirectMessages(epidemicEncounteredNode, altruismAnalysis, contactDuration, currentTime, dissemination);
        int totalMessages = 0;

        for (Message message : epidemicEncounteredNode.dataMemory) {
            if (totalMessages >= remainingMessages) {
                return;
            }

            insertMessage(message, epidemicEncounteredNode, currentTime, altruismAnalysis, dissemination);
            totalMessages++;
        }

        for (Message message : epidemicEncounteredNode.ownMessages) {
            if (totalMessages >= remainingMessages) {
                return;
            }

            insertMessage(message, epidemicEncounteredNode, currentTime, altruismAnalysis, dissemination);
            totalMessages++;
        }
    }
}
