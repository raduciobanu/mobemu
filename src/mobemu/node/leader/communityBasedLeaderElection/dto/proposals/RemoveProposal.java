package mobemu.node.leader.communityBasedLeaderElection.dto.proposals;

import mobemu.node.leader.communityBasedLeaderElection.dto.proposals.ChangeProposal;

/**
 * Created by radu on 2/6/2017.
 */
public class RemoveProposal extends ChangeProposal {
    public RemoveProposal(int sourceId, int targetId, long timestamp) {
        super(sourceId, targetId, timestamp);
    }
}
