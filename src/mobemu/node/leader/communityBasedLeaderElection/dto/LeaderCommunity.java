package mobemu.node.leader.communityBasedLeaderElection.dto;

import mobemu.node.Node;

import java.util.*;

/**
 * Created by radu on 1/15/2017.
 */
public class LeaderCommunity {

    /**
     *  The community of the current node
     *  Key = nodeId, Value = (node centrality, timestamp)
     */
    private Map<Integer, AbstractMap.SimpleEntry<Double, Long>> leaderCommunity;

    public LeaderCommunity(){
        leaderCommunity = new HashMap<>();
    }

    public void add(int nodeId, double nodeCentrality, long currentTime){
        leaderCommunity.put(nodeId, new AbstractMap.SimpleEntry<>(nodeCentrality, currentTime));
    }

    public void addNode(Node node, long currentTime){
        leaderCommunity.put(node.getId(), new AbstractMap.SimpleEntry<>(node.getCentrality(true), currentTime));
    }

    public int size(){
        return leaderCommunity.size();
    }

    public Set<Integer> getNodes(){
        return leaderCommunity.keySet();
    }

    public double get(int nodeId){
        return leaderCommunity.get(nodeId).getKey();
    }

    public boolean containsNode(int nodeId){
        return leaderCommunity.containsKey(nodeId);
    }

    public boolean update(LeaderCommunity encounteredLeaderCommunity){
        boolean centralityChanged = false;
        for(int nodeId : encounteredLeaderCommunity.getNodes()){
            double encounteredCentrality = encounteredLeaderCommunity.leaderCommunity.get(nodeId).getKey();
            long encounteredTimestamp = encounteredLeaderCommunity.leaderCommunity.get(nodeId).getValue();

            centralityChanged |= update(nodeId, encounteredCentrality, encounteredTimestamp);
        }

        return centralityChanged;
    }

    public boolean update(int nodeId, double centrality, long timestamp){
        if(!leaderCommunity.containsKey(nodeId)){
//            leaderCommunity.put(nodeId, new AbstractMap.SimpleEntry<>(centrality, timestamp));
            return false;
        }

        long currentTimestamp = leaderCommunity.get(nodeId).getValue();
        if(currentTimestamp < timestamp){
            leaderCommunity.put(nodeId, new AbstractMap.SimpleEntry<>(centrality, timestamp));

            return true;
        }

        return false;
    }
}
