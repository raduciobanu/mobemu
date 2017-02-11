package mobemu.node.leader.communityBasedLeaderElection;

import mobemu.node.Node;

/**
 * Created by radu on 2/3/2017.
 */
public class CommunityLeaderStats {

    /**
     * Check if the communities did not become sparse
     * It can happend that some nodes go far from their leader community, so they should be excluded from the leader community
     * @param nodes
     * @param tick
     */
    public static void checkCommunities(Node[] nodes, long tick){

        for(Node node : nodes){
            if(node instanceof CommunityLeaderNode)
                return;

            CommunityLeaderNode communityLeaderNode = (CommunityLeaderNode) node;

        }
    }
}
