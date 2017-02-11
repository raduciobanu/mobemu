package mobemu.node.leader.communityBasedLeaderElection.dto.communityMessage.handlers;

import mobemu.node.leader.communityBasedLeaderElection.CommunityLeaderNode;
import mobemu.node.leader.communityBasedLeaderElection.dto.CommunityMessageType;
import mobemu.node.leader.communityBasedLeaderElection.dto.communityMessage.CommunityMessage;

/**
 * Created by radu on 2/5/2017.
 */
public class AddedNodeHandler extends CommunityMessageHandler{
    public AddedNodeHandler(CommunityLeaderNode leaderNode) {
        super(leaderNode);
        this.messageType = CommunityMessageType.AddedNode;
    }

    @Override
    protected void parse(CommunityMessage communityMessage, long currentTime) {
        double targetCentrality = communityMessage.getTargetCentrality();
        int targetId = communityMessage.getTargetId();
        leaderNode.addToLeaderCommunity(targetId, targetCentrality, currentTime);
    }
}
