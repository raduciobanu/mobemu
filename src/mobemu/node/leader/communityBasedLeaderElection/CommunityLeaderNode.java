package mobemu.node.leader.communityBasedLeaderElection;

import mobemu.node.Context;
import mobemu.node.Node;
import mobemu.node.leader.LeaderNode;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by radu on 1/15/2017.
 */
public class CommunityLeaderNode extends LeaderNode {

    /**
     *  the community of the current node
     */
    List<Integer> leaderCommunity;

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
    public CommunityLeaderNode(int id, int nodes, Context context, boolean[] socialNetwork, int dataMemorySize,
                               int exchangeHistorySize, long seed, long traceStart, long traceEnd) {
        super(id, nodes, context, socialNetwork, dataMemorySize, exchangeHistorySize, seed, traceStart, traceEnd);

        leaderCommunity = new ArrayList<>();
    }

    @Override
    protected void addToLocalCommunity(Node encounteredNode){
        if(!(encounteredNode instanceof CommunityLeaderNode)){
            return;
        }

        super.addToLocalCommunity(encounteredNode);

        //if current node is in the local community of encountered node => both nodes are in the localCommunity of each other
        //add the nodes in the LeaderCommunity of each other
        if(encounteredNode.inLocalCommunity(id)){
            addToLeaderCommunity(encounteredNode);
            ((CommunityLeaderNode) encounteredNode).addToLeaderCommunity(this);
        }

    }

    protected void addToLeaderCommunity(Node encounteredNode){
        if(!leaderCommunity.contains(encounteredNode.getId()))
            leaderCommunity.add(encounteredNode.getId());
    }

    @Override
    protected void onDataExchange(Node encounteredNode, long contactDuration, long currentTime) {

    }

}
