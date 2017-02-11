package mobemu.node.leader.communityBasedLeaderElection.dto.proposals;

/**
 * Created by radu on 2/5/2017.
 */
public abstract class ChangeProposal {

    int sourceId;

    int targetId;

    long timestamp;

    public ChangeProposal(int sourceId, int targetId, long timestamp){
        this.sourceId = sourceId;
        this.targetId = targetId;
        this.timestamp = timestamp;
    }

    public int getSourceId() {
        return sourceId;
    }

    public int getTargetId() {
        return targetId;
    }

    public long getTimestamp() {
        return timestamp;
    }
}
