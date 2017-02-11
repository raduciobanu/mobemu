package mobemu.node.leader.communityBasedLeaderElection.dto.communityMessage.handlers;

import mobemu.node.leader.communityBasedLeaderElection.CommunityLeaderNode;
import mobemu.node.leader.communityBasedLeaderElection.dto.CommunityMessageType;
import mobemu.node.leader.communityBasedLeaderElection.dto.LeaderCommunity;
import mobemu.node.leader.communityBasedLeaderElection.dto.proposals.collections.LeaderProposalCollection;
import mobemu.node.leader.communityBasedLeaderElection.dto.communityMessage.CommunityMessage;

import java.util.Set;

import static mobemu.utils.Constants.leaderProposalsThreshold;

/**
 * Created by radu on 2/5/2017.
 */
public class LeaderProposalHandler extends CommunityMessageHandler {

    public LeaderProposalHandler(CommunityLeaderNode leaderNode) {
        super(leaderNode);
        messageType = CommunityMessageType.LeaderProposal;
    }

    @Override
    protected void parse(CommunityMessage communityMessage, long currentTime) {
        int targetId = communityMessage.getTargetId();
        int sourceId = communityMessage.getSourceId();
        double score = communityMessage.getScore();
        LeaderProposalCollection leaderProposals = leaderNode.getLeaderProposalCollection();
        Set<Integer> leaderCommunityNodes = leaderNode.getLeaderCommunityNodes();

        leaderNode.addToLeaderProposals(sourceId, targetId, currentTime, score);
        if(checkCommunityThreshold(leaderProposals.size())){
            int newLeaderId = leaderProposals.getLeader();

            if(newLeaderId != leaderNode.getLeaderNodeId() && newLeaderId != -1){
                //send LeaderElected message to all nodes from leaderCommunity
                for (int nodeId : leaderCommunityNodes){
                    leaderNode.addToOwnCommunityMessages(CommunityMessage.CreateLeaderElected(
                            leaderNode.getId(), nodeId, targetId, currentTime));
                }

                leaderNode.electLeader(newLeaderId, currentTime);
            }
        }
    }
}
