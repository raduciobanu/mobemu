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


    /**
     * The id of the target node of the message (the node for whom the message was issued)
     */
    private int targetId;

    /**
     * Creates a new community message
     * @param sourceId the id of the source
     * @param destinationId the id of the destination
     * @param timestamp the destination of the message
     */
    private CommunityMessage(int sourceId, int destinationId, int targetId, long timestamp) {
        super(sourceId, destinationId, timestamp);

        this.targetId = targetId;
    }

    public static CommunityMessage CreateRequest(int sourceId, int destinationId, int targetId, long timestamp){
        CommunityMessage leaderMessage = new CommunityMessage(sourceId, destinationId, targetId, timestamp);
        leaderMessage.messageType = CommunityMessageType.Request;

        return leaderMessage;
    }

    public static CommunityMessage CreateResponse(int sourceId, int destinationId, int targetId, long timestamp){
        CommunityMessage leaderMessage = new CommunityMessage(sourceId, destinationId, targetId, timestamp);
        leaderMessage.messageType = CommunityMessageType.Response;

        return leaderMessage;
    }

    public static CommunityMessage CreateAddedNode(int sourceId, int destinationId, int targetId, long timestamp){
        CommunityMessage leaderMessage = new CommunityMessage(sourceId, destinationId, targetId, timestamp);
        leaderMessage.messageType = CommunityMessageType.AddedNode;

        return leaderMessage;
    }

    public static CommunityMessage CreateLeaderProposal(int sourceId, int destinationId, int targetId, long timestamp){
        CommunityMessage leaderMessage = new CommunityMessage(sourceId, destinationId, targetId, timestamp);
        leaderMessage.messageType = CommunityMessageType.LeaderProposal;

        return leaderMessage;
    }

    public boolean isRequest(){
        return messageType == CommunityMessageType.Request;
    }

    public boolean isResponse(){
        return messageType == CommunityMessageType.Response;
    }

    public boolean isAddedNode(){
        return messageType == CommunityMessageType.AddedNode;
    }

    public int getTargetId() {
        return targetId;
    }
}
