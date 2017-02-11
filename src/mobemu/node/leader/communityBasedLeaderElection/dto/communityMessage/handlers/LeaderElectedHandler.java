package mobemu.node.leader.communityBasedLeaderElection.dto.communityMessage.handlers;

import mobemu.node.leader.communityBasedLeaderElection.CommunityLeaderNode;
import mobemu.node.leader.communityBasedLeaderElection.dto.CommunityMessageType;
import mobemu.node.leader.communityBasedLeaderElection.dto.communityMessage.CommunityMessage;

/**
 * Created by radu on 2/5/2017.
 */
public class LeaderElectedHandler extends CommunityMessageHandler {

    public LeaderElectedHandler(CommunityLeaderNode leaderNode) {
        super(leaderNode);
        this.messageType = CommunityMessageType.LeaderElected;
    }

    @Override
    protected void parse(CommunityMessage communityMessage, long currentTime) {
        long messageTimestamp = communityMessage.getTimestamp();
        int targetId = communityMessage.getTargetId();

        if(messageTimestamp > leaderNode.getLeaderElectionTimestamp()){
            leaderNode.electLeader(targetId, messageTimestamp);
        }
    }
}
