package mobemu.node.leader.communityBasedLeaderElection.dto.communityMessage.handlers;

import mobemu.node.leader.communityBasedLeaderElection.CommunityLeaderNode;
import mobemu.node.leader.communityBasedLeaderElection.dto.CommunityMessageType;
import mobemu.node.leader.communityBasedLeaderElection.dto.LeaderCommunity;
import mobemu.node.leader.communityBasedLeaderElection.dto.communityMessage.CommunityMessage;
import mobemu.node.leader.communityBasedLeaderElection.dto.proposals.RemoveProposal;
import mobemu.node.leader.communityBasedLeaderElection.dto.proposals.collections.RemoveProposalCollection;

import java.util.Set;

/**
 * Created by radu on 2/5/2017.
 */
public class RemoveRequestHandler extends CommunityMessageHandler {

    private RemoveProposalCollection removeProposalCollection;

    public RemoveRequestHandler(CommunityLeaderNode leaderNode) {
        super(leaderNode);
        messageType = CommunityMessageType.RemoveRequest;
        removeProposalCollection = new RemoveProposalCollection();
    }

    @Override
    protected void parse(CommunityMessage communityMessage, long currentTime) {
        int targetId = communityMessage.getTargetId();
        int sourceId = communityMessage.getSourceId();

        RemoveProposal removeProposal = new RemoveProposal(sourceId, targetId, currentTime);
        removeProposalCollection.add(removeProposal);

        if (!leaderNode.shouldBeToLeaderCommunity(targetId)) {
            checkLeaderCommunityThreshold(targetId, currentTime);
        }
    }

    /**
     * Check the threshold only for the seen nodes (the others might be unreachable)
     * @param targetId the id of the node for the check
     * @param currentTime current timestamp
     */
    private void checkLeaderCommunityThreshold(int targetId, long currentTime){
        if(checkCommunityThreshold(removeProposalCollection.size(), leaderNode.getNodesSeen().size())){
            leaderNode.removeFromLeaderCommunity(targetId);

            Set<Integer> leaderCommunityNodes = leaderNode.getLeaderCommunityNodes();
            for(int nodeId : leaderCommunityNodes){
                leaderNode.addToOwnCommunityMessages(CommunityMessage.CreateRemovedNode(leaderNode.getId(),
                        nodeId, targetId, currentTime));
            }
        }
    }
}
