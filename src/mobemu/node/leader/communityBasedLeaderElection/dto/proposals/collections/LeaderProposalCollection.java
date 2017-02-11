package mobemu.node.leader.communityBasedLeaderElection.dto.proposals.collections;

import mobemu.node.leader.communityBasedLeaderElection.dto.proposals.LeaderProposal;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by radu on 1/16/2017.
 */
public class LeaderProposalCollection extends ChangeProposalCollection<LeaderProposal> {

    /**
     * Dictionary of leader proposals
     * Key = the id of the node the proposal was issued for
     * Value = dictionary of proposals for the given nodeId
     * Key = the id of the node that issued the proposal
     * Value = the leader score of the target as seen by the issuer of the proposal
     */
//    private Map<Integer, Map<Integer, Double>> proposals;
//
//    public LeaderProposalCollection(){
//        proposals = new HashMap<>();
//    }
//
//    /**
//     * Adds a proposal to the dictionary
//     * @param targetId the id of the node which the proposal was issued for
//     * @param sourceId the id of the node that issued the proposal
//     * @param score the score of the target as seen by the source of the proposal
//     */
//    public void addProposal(int targetId, int sourceId, double score){
//        Map<Integer, Double> proposalsForNode = new HashMap<>();
//        if(proposals.containsKey(targetId)){
//            proposalsForNode = proposals.get(targetId);
//        }
//
//        proposalsForNode.put(sourceId, score);
//        proposals.put(targetId, proposalsForNode);
//    }
//
//    /**
//     * Gets the number of proposals
//     * @return the number of proposals
//     */
//    public int size(){
//        int size = 0;
//        for(int targetId: proposals.keySet()){
//            size += proposals.get(targetId).size();
//        }
//
//        return size;
//    }
//
//    /**
//     * Checks if there is any proposal with the given arguments
//     * @param targetId the id of the target
//     * @param sourceId the id of the source
//     * @param score the score
//     * @return
//     */
//    public boolean contains(int targetId, int sourceId, double score){
//        if(!proposals.containsKey(targetId)){
//            return false;
//        }
//
//        Map<Integer, Double> proposalsForNode = proposals.get(targetId);
//        if(!proposalsForNode.containsKey(sourceId)){
//            return false;
//        }
//
//        double existingScore = proposalsForNode.get(sourceId);
//
//        return existingScore == score;
//    }

    public Map<Integer, Double> getAverageScorePerNode(){
        Map<Integer, Double> averageScorePerNode = new HashMap<>();
        Map<Integer, Integer> numberOfScoresPerNode = new HashMap<>();
        for(LeaderProposal proposal : proposals){
            int targetId = proposal.getTargetId();
            if(!averageScorePerNode.containsKey(targetId)){
                averageScorePerNode.put(targetId, 0.0);
                numberOfScoresPerNode.put(targetId, 0);
            }

            averageScorePerNode.put(targetId, averageScorePerNode.get(targetId) + proposal.getScore());
            numberOfScoresPerNode.put(targetId, numberOfScoresPerNode.get(targetId) + 1);
        }

        for (Map.Entry<Integer, Double> entry: averageScorePerNode.entrySet()){
            int targetId = entry.getKey();
            double sum = entry.getValue();
            averageScorePerNode.put(targetId, sum/numberOfScoresPerNode.get(targetId));
        }

        return averageScorePerNode;
    }

    /**
     * Iterates through the dictionary of proposals
     * Computes the average score for each node
     * Returns the node with highest average score
     * @return the id of the node with highest score
     */
    public int getLeader(){
        Map<Integer, Double> averageScorePerNode = getAverageScorePerNode();
        int leaderId = -1;
        double maxScore = -1.0;
        for (Map.Entry<Integer, Double> entry : averageScorePerNode.entrySet()){
            int targetId = entry.getKey();
            double averageScore = entry.getValue();
            if(averageScore > maxScore){
                maxScore = averageScore;
                leaderId = targetId;
            }
        }

        return leaderId;


//        List<Integer> targetIds = getTargetIds();
//        for(Integer targetId : targetIds){
//
//        }
//
//        //compute the average scores for all nodes
//        for(int targetId : proposals.keySet()){
//            Map<Integer, Double> proposalsForNode = proposals.get(targetId);
//
//            double averageScore = 0.0;
//            int numberOfProposalsForTarget = 0;
//            for(int sourceId : proposalsForNode.keySet()){
//                double score = proposalsForNode.get(sourceId);
//                averageScore += score;
//                numberOfProposalsForTarget++;
//            }
//            averageScore /= numberOfProposalsForTarget;
//
//            averageScorePerNode.put(targetId, averageScore);
//        }
//
//        //get the node with the highest score
//        double maxScore = 0.0;
//        int leaderId = -1;
//        for(int targetId : averageScorePerNode.keySet()){
//            double score = averageScorePerNode.get(targetId);
//
//            if(score > maxScore){
//                maxScore = score;
//                leaderId = targetId;
//            }
//        }
//
//        return leaderId;
    }
}
