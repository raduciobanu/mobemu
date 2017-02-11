package mobemu.node.leader.communityBasedLeaderElection.dto.communityMessage.handlers;

import mobemu.node.leader.communityBasedLeaderElection.CommunityLeaderNode;
import mobemu.node.leader.communityBasedLeaderElection.dto.CommunityMessageType;
import mobemu.node.leader.communityBasedLeaderElection.dto.communityMessage.CommunityMessage;

/**
 * Created by radu on 2/6/2017.
 */
public class RemovedRequestHandler extends CommunityMessageHandler {
    public RemovedRequestHandler(CommunityLeaderNode leaderNode) {
        super(leaderNode);
        this.messageType = CommunityMessageType.RemovedNode;
    }

    @Override
    protected void parse(CommunityMessage communityMessage, long currentTime) {
        int targetId = communityMessage.getTargetId();
        leaderNode.removeFromLeaderCommunity(targetId);
    }
}
