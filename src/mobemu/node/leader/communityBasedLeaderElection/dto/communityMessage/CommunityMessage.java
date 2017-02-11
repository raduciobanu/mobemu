package mobemu.node.leader.communityBasedLeaderElection.dto.communityMessage;

import mobemu.node.leader.LeaderMessage;
import mobemu.node.leader.communityBasedLeaderElection.dto.CommunityMessageType;

/**
 * Created by radu on 1/15/2017.
 */
public class CommunityMessage extends LeaderMessage{

    /**
     * The type of the message
     */
    protected CommunityMessageType messageType;


    /**
     * The id of the target node of the message (the node for whom the message was issued)
     */
    protected int targetId;

    /**
     * The score of the target for a leaderProposal
     */
    protected double score;

    /**
     * The centrality of the targetNode
     */
    protected double targetCentrality;

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

    public static CommunityMessage CreateAddRequest(int sourceId, int destinationId, int targetId, long timestamp){
        CommunityMessage leaderMessage = new CommunityMessage(sourceId, destinationId, targetId, timestamp);
        leaderMessage.messageType = CommunityMessageType.AddRequest;
        leaderMessage.setHopCount(sourceId, 0);

        return leaderMessage;
    }

    public static CommunityMessage CreateResponse(int sourceId, int destinationId, int targetId, long timestamp,
                                                  int hopCount){
        CommunityMessage leaderMessage = new CommunityMessage(sourceId, destinationId, targetId, timestamp);
        leaderMessage.messageType = CommunityMessageType.AddResponse;
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

    public static CommunityMessage CreateRemoveRequest(int sourceId, int destinationId, int targetId, long timestamp){
        CommunityMessage leaderMessage = new CommunityMessage(sourceId, destinationId, targetId, timestamp);
        leaderMessage.messageType = CommunityMessageType.RemoveRequest;

        return leaderMessage;
    }

    public static CommunityMessage CreateRemovedNode(int sourceId, int destinationId, int targetId, long timestamp){
        CommunityMessage leaderMessage = new CommunityMessage(sourceId, destinationId, targetId, timestamp);
        leaderMessage.messageType = CommunityMessageType.RemovedNode;

        return leaderMessage;
    }

    public CommunityMessageType getMessageType(){
        return messageType;
    }

    public boolean isRequest(){
        return messageType == CommunityMessageType.AddRequest;
    }

    public boolean isResponse(){
        return messageType == CommunityMessageType.AddResponse;
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
