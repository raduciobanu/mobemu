package mobemu.node.leader.directLeaderElection.dto;

import mobemu.node.leader.directLeaderElection.dto.enums.LeaderMessageType;

/**
 * Created by radu on 1/13/2017.
 */
public class LeaderMessage {
    protected static int heartBeatCount = 0; // total number of messages generated

    /**
     * The id of the heartbeat
     */
    private int id;

    /**
     * The id of the source of the heartbeat
     */
    private int sourceId;

    /**
     * The id of the destination of the heartbeat
     */
    private int destinationId;

    /**
     * Whether the heart beat is a messageType or a response
     */
    private LeaderMessageType messageType;

    /**
     * The generation time of the heartbeat
     */
    private long timestamp;

    private LeaderMessage(int sourceId, int destinationId, long timestamp) {
        this.id = heartBeatCount++;
        this.sourceId = sourceId;
        this.destinationId = destinationId;

        this.timestamp = timestamp;
    }

    public static LeaderMessage CreateRequest(int sourceId, int destinationId, long timestamp){
        LeaderMessage leaderMessage = new LeaderMessage(sourceId, destinationId, timestamp);
        leaderMessage.messageType = LeaderMessageType.Request;

        return leaderMessage;
    }

    public static LeaderMessage CreateResponse(int sourceId, int destinationId, long timestamp){
        LeaderMessage leaderMessage = new LeaderMessage(sourceId, destinationId, timestamp);
        leaderMessage.messageType = LeaderMessageType.Response;

        return leaderMessage;
    }

    public static LeaderMessage CreateChangedLeader(int sourceId, int destinationId, long timestamp){
        LeaderMessage leaderMessage = new LeaderMessage(sourceId, destinationId, timestamp);
        leaderMessage.messageType = LeaderMessageType.ChangedLeader;

        return leaderMessage;
    }

    public int getId() {
        return id;
    }

    public int getSourceId() {
        return sourceId;
    }

    public int getDestinationId() {
        return destinationId;
    }

    public boolean isRequest() {
        return messageType == LeaderMessageType.Request;
    }

    public boolean isResponse(){
        return messageType == LeaderMessageType.Response;
    }

    public boolean isChangedLeader(){
        return messageType == LeaderMessageType.ChangedLeader;
    }

    public long getTimestamp() {
        return timestamp;
    }
}
