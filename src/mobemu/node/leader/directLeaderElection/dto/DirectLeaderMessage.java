package mobemu.node.leader.directLeaderElection.dto;

import mobemu.node.leader.LeaderMessage;
import mobemu.node.leader.directLeaderElection.dto.enums.LeaderMessageType;

/**
 * Created by radu on 1/13/2017.
 */
public class DirectLeaderMessage extends LeaderMessage{
    /**
     * The type of the message
     */
    private LeaderMessageType messageType;

    private DirectLeaderMessage(int sourceId, int destinationId, long timestamp) {
        super(sourceId, destinationId, timestamp);
    }

    public static DirectLeaderMessage CreateRequest(int sourceId, int destinationId, long timestamp){
        DirectLeaderMessage leaderMessage = new DirectLeaderMessage(sourceId, destinationId, timestamp);
        leaderMessage.messageType = LeaderMessageType.Request;

        return leaderMessage;
    }

    public static DirectLeaderMessage CreateResponse(int sourceId, int destinationId, long timestamp){
        DirectLeaderMessage leaderMessage = new DirectLeaderMessage(sourceId, destinationId, timestamp);
        leaderMessage.messageType = LeaderMessageType.Response;

        return leaderMessage;
    }

    public static DirectLeaderMessage CreateChangedLeader(int sourceId, int destinationId, long timestamp){
        DirectLeaderMessage leaderMessage = new DirectLeaderMessage(sourceId, destinationId, timestamp);
        leaderMessage.messageType = LeaderMessageType.ChangedLeader;

        return leaderMessage;
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


}
