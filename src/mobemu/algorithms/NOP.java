/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package mobemu.algorithms;

import mobemu.node.Context;
import mobemu.node.Node;

/**
 * Class for an ON exchange algorithm that does nothing.
 *
 * @author Radu
 */
public class NOP extends Node {

    /**
     * Constructor for the {@link NOP} class.
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
    public NOP(int id, int nodes, Context context, boolean[] socialNetwork, int dataMemorySize,
            int exchangeHistorySize, long seed, long traceStart, long traceEnd) {
        super(id, nodes, context, socialNetwork, dataMemorySize, exchangeHistorySize, seed, traceStart, traceEnd);
    }

    @Override
    public String getName() {
        return "NOP";
    }

    @Override
    protected void onDataExchange(Node encounteredNode, long contactDuration, long currentTime) {
    }
}
