package mobemu.node.leader.communityBasedLeaderElection.dto;

import mobemu.node.Node;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Created by radu on 1/15/2017.
 */
public class LeaderCommunity {

    /**
     *  The community of the current node
     *  Key = nodeId, Value = node centrality
     */
    protected Map<Integer, Double> leaderCommunity;

    public LeaderCommunity(){
        leaderCommunity = new HashMap<>();
    }

    public void add(int nodeId, double nodeCentrality){
        leaderCommunity.put(nodeId, nodeCentrality);
    }

    public void addNode(Node node){
        leaderCommunity.put(node.getId(), node.getCentrality(true));
    }

    public int size(){
        return leaderCommunity.size();
    }

    public Set<Integer> getNodes(){
        return leaderCommunity.keySet();
    }

    public double get(int nodeId){
        return leaderCommunity.get(nodeId);
    }

    public boolean containsNode(int nodeId){
        return leaderCommunity.containsKey(nodeId);
    }
}
