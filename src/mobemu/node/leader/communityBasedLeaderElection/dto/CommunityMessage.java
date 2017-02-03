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
     * The score of the target for a leaderProposal
     */
    private double score;

    /**
     * The centrality of the targetNode
     */
    private double targetCentrality;

    /**
     * Creates a new community message
     * @param sourceId the id of the source
     * @param destinationId the id of the destination
     * @param timestamp the destination of the message
     */
    private CommunityMessage(int sourceId, int destinationId, int targetId, long timestamp) {
        super(sourceId, destinationId, timestamp);

        this.targetId = targetId;
        this.targetCentrality = 0.0;
    }

    public static CommunityMessage CreateRequest(int sourceId, int destinationId, int targetId, long timestamp){
        CommunityMessage leaderMessage = new CommunityMessage(sourceId, destinationId, targetId, timestamp);
        leaderMessage.messageType = CommunityMessageType.Request;
        leaderMessage.setHopCount(sourceId, 0);

        return leaderMessage;
    }

    public static CommunityMessage CreateResponse(int sourceId, int destinationId, int targetId, long timestamp,
                                                  int hopCount){
        CommunityMessage leaderMessage = new CommunityMessage(sourceId, destinationId, targetId, timestamp);
        leaderMessage.messageType = CommunityMessageType.Response;
        leaderMessage.setHopCount(sourceId, hopCount);

        return leaderMessage;
    }

    public static CommunityMessage CreateAddedNode(int sourceId, int destinationId, int targetId, double targetCentrality,
                                                   long timestamp){
        CommunityMessage leaderMessage = new CommunityMessage(sourceId, destinationId, targetId, timestamp);
        leaderMessage.messageType = CommunityMessageType.AddedNode;
        leaderMessage.targetCentrality = targetCentrality;

        return leaderMessage;
    }

    public static CommunityMessage CreateLeaderProposal(int sourceId, int destinationId, int targetId, double score,
                                                        long timestamp){
        CommunityMessage leaderMessage = new CommunityMessage(sourceId, destinationId, targetId, timestamp);
        leaderMessage.messageType = CommunityMessageType.LeaderProposal;
        leaderMessage.score = score;

        return leaderMessage;
    }

    public static CommunityMessage CreateLeaderElected(int sourceId, int destinationId, int targetId, long timestamp){
        CommunityMessage leaderMessage = new CommunityMessage(sourceId, destinationId, targetId, timestamp);
        leaderMessage.messageType = CommunityMessageType.LeaderElected;

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

    public boolean isLeaderProposal(){
        return messageType == CommunityMessageType.LeaderProposal;
    }

    public boolean isLeaderElected(){
        return messageType == CommunityMessageType.LeaderElected;
    }

    public int getTargetId() {
        return this.targetId;
    }

    public double getScore(){
        return this.score;
    }

    public double getTargetCentrality(){
        return this.targetCentrality;
    }
}
