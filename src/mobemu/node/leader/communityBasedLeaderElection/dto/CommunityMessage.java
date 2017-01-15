package mobemu.node.leader.communityBasedLeaderElection.dto;

import mobemu.node.leader.LeaderMessage;

/**
 * Created by radu on 1/15/2017.
 */
public class CommunityMessage extends LeaderMessage{

    /**
     * The type of the message
     */
    private CommunityMessageType messageType;

    public CommunityMessage(int sourceId, int destinationId, long timestamp) {
        super(sourceId, destinationId, timestamp);
    }

    public static CommunityMessage CreateRequest(int sourceId, int destinationId, long timestamp){
        CommunityMessage leaderMessage = new CommunityMessage(sourceId, destinationId, timestamp);
        leaderMessage.messageType = CommunityMessageType.Request;

        return leaderMessage;
    }

    public static CommunityMessage CreateResponse(int sourceId, int destinationId, long timestamp){
        CommunityMessage leaderMessage = new CommunityMessage(sourceId, destinationId, timestamp);
        leaderMessage.messageType = CommunityMessageType.Response;

        return leaderMessage;
    }

    public CommunityMessageType getMessageType() {
        return messageType;
    }
}
