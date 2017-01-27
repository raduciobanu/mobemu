package mobemu.node.leader.directLeaderElection.dto;

import java.util.List;

/**
 * Created by radu on 1/13/2017.
 */
public class LeaderCandidacy {
    /**
     * The id of the node who announced the candidacy
     */
    private int nodeId;

    private double centrality;

    private int hopCount;

    private long timestamp;

    private long MAX_LATENCY;

    public LeaderCandidacy(int nodeId, double centrality, int hopCount, long timestamp) {
        this.nodeId = nodeId;
        this.centrality = centrality;
        this.hopCount = hopCount;
        this.timestamp = timestamp;
    }

    public LeaderCandidacy (LeaderCandidacy candidacy){
        this.nodeId = candidacy.nodeId;
        this.centrality = candidacy.centrality;
        this.hopCount = candidacy.hopCount - 1;
        this.timestamp = candidacy.timestamp;
    }

    public int getNodeId() {
        return nodeId;
    }

    public double getCentrality() {
        return centrality;
    }

    public int getHopCount(){
        return hopCount;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public boolean exceededHopCount(){
        return hopCount <= 0;
    }

    public double getNormalizedLatencyValue(long currentTime){
        long latency = currentTime - timestamp;
        if(latency > MAX_LATENCY)
            MAX_LATENCY = latency;

        if(MAX_LATENCY == 0)
            return 1d;

        return 1d - (double)(latency / MAX_LATENCY);
    }

    /**
     * Searches for a candidacy with a given id in the given array
     * @param candidacies the array of candidacies
     * @param nodeId the id of the node searched for
     * @return {@code true} if the array contains the given id
     */
    public static LeaderCandidacy containsId(List<LeaderCandidacy> candidacies, int nodeId){
        for(LeaderCandidacy candidacy: candidacies){
            if(candidacy.getNodeId()  == nodeId){
                return candidacy;
            }
        }
        return null;
    }
}
