package mobemu.node.leader.communityBasedLeaderElection.dto;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by radu on 1/16/2017.
 */
public class LeaderProposals {

    /**
     * Dictionary of leader proposals
     * Key = the id of the node the proposal was issued for
     * Value = dictionary of proposals for the given nodeId
     * Key = the id of the node that issued the proposal
     * Value = the leader score of the target as seen by the issuer of the proposal
     */
    private Map<Integer, Map<Integer, Double>> proposals;

    public LeaderProposals(){
        proposals = new HashMap<>();
    }

    /**
     * Adds a proposal to the dictionary
     * @param targetId the id of the node which the proposal was issued for
     * @param sourceId the id of the node that issued the proposal
     * @param score the score of the target as seen by the source of the proposal
     */
    public void addProposal(int targetId, int sourceId, double score){
        Map<Integer, Double> proposalsForNode = new HashMap<>();
        if(proposals.containsKey(targetId)){
            proposalsForNode = proposals.get(targetId);
        }

        proposalsForNode.put(sourceId, score);
        proposals.put(targetId, proposalsForNode);
    }

    /**
     * Gets the number of proposals
     * @return the number of proposals
     */
    public int size(){
        int size = 0;
        for(int targetId: proposals.keySet()){
            size += proposals.get(targetId).size();
        }

        return size;
    }

    /**
     * Iterates through the dictionary of proposals
     * Computes the average score for each node
     * Returns the node with highest average score
     * @return the id of the node with highest score
     */
    public int getLeader(){
        Map<Integer, Double> averageScorePerNode = new HashMap<>();

        //compute the average scores for all nodes
        for(int targetId : proposals.keySet()){
            Map<Integer, Double> proposalsForNode = proposals.get(targetId);

            double averageScore = 0.0;
            int numberOfProposalsForTarget = 0;
            for(int sourceId : proposalsForNode.keySet()){
                double score = proposalsForNode.get(sourceId);
                averageScore += score;
                numberOfProposalsForTarget++;
            }
            averageScore /= numberOfProposalsForTarget;

            averageScorePerNode.put(targetId, averageScore);
        }

        //get the node with the highest score
        double maxScore = 0.0;
        int leaderId = -1;
        for(int targetId : averageScorePerNode.keySet()){
            double score = averageScorePerNode.get(targetId);

            if(score > maxScore){
                maxScore = score;
                leaderId = targetId;
            }
        }

        return leaderId;
    }
}
