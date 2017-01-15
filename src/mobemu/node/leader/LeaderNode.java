package mobemu.node.leader;

import mobemu.node.Context;
import mobemu.node.Node;

/**
 * Created by radu on 1/15/2017.
 */
public abstract class LeaderNode extends Node {

    /**
     * The id of the Leader (for current node's point of view)
     */
    protected int leaderNodeId;

    /**
     * The score computed for the current leader
     */
    protected double leaderScore;

    /**
     * Constructor for the {@link Node} class.
     *
     * @param id                  ID of the node
     * @param nodes               total number of existing nodes
     * @param context             the context of this node
     * @param socialNetwork       the social network as seen by this node
     * @param dataMemorySize      the maximum allowed size of the data memory
     * @param exchangeHistorySize the maximum allowed size of the exchange
     *                            history
     * @param seed                the seed for the random number generators
     * @param traceStart          timestamp of the start of the trace
     * @param traceEnd            timestamp of the end of the trace
     */
    public LeaderNode(int id, int nodes, Context context, boolean[] socialNetwork, int dataMemorySize, int exchangeHistorySize, long seed, long traceStart, long traceEnd) {
        super(id, nodes, context, socialNetwork, dataMemorySize, exchangeHistorySize, seed, traceStart, traceEnd);

        leaderNodeId = id;
    }

    public int getLeaderNodeId() {
        return leaderNodeId;
    }

    public double getLeaderScore() {
        return leaderScore;
    }

    @Override
    public String getName() {
        return null;
    }
}
