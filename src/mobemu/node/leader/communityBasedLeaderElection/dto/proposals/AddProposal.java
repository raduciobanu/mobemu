package mobemu.node.leader.communityBasedLeaderElection.dto.proposals;

import mobemu.node.leader.communityBasedLeaderElection.dto.proposals.ChangeProposal;

/**
 * Created by radu on 2/6/2017.
 */
public class AddProposal extends ChangeProposal {

    /**
     * The centrality of the node to be added
     */
    private double centrality;

    public AddProposal(int sourceId, int targetId, long timestamp, double centrality) {
        super(sourceId, targetId, timestamp);
        this.centrality = centrality;
    }
}
