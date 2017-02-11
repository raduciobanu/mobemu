package mobemu.node.leader.communityBasedLeaderElection.dto.proposals;

import mobemu.node.leader.communityBasedLeaderElection.dto.proposals.ChangeProposal;

/**
 * Created by radu on 2/6/2017.
 */
public class LeaderProposal extends ChangeProposal {

    private double score;

    public LeaderProposal(int sourceId, int targetId, long timestamp, double score){
        super(sourceId, targetId, timestamp);
        this.score = score;
    }

    public double getScore(){
        return score;
    }
}
