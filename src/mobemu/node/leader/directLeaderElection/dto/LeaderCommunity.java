package mobemu.node.leader.directLeaderElection.dto;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by radu on 1/14/2017.
 */
public class LeaderCommunity {
    /**
     * Dictionary of nodes and the timestamp of membership request
     */
    private Map<Integer, Long> community;

    public LeaderCommunity(){
        community = new HashMap<>();
    }

    /**
     * Checks whether a given node is a member of the community
     * @param nodeId the id of the node
     * @return {@code true} if the node is already a member of the community, {@code false} otherwise
     */
    public boolean containsNode(int nodeId){
        return community.containsKey(nodeId);
    }

    /**
     * Add a node to the current community
     * @param nodeId the id of the node
     * @param timestamp the timestamp of the request
     */
    public void addNode(int nodeId, long timestamp){
        //if the node is already in the community, check the timestamp and update if necessary
        if(community.containsKey(nodeId)){
            long oldTimestamp = community.get(nodeId);

            //older request than the current one => ignore
            if(oldTimestamp >= timestamp)
                return;

            //remove the old request
            community.remove(nodeId);
        }

        //add the new request
        community.put(nodeId, timestamp);
    }

    /**
     * Remove a node from the current community
     * @param nodeId the id of the node
     * @param timestamp the timestamp of the request
     */
    public void removeNode(int nodeId, long timestamp){
        if(!community.containsKey(nodeId))
            return;

        long oldTimestamp = community.get(nodeId);

        //the received request of leader change is older than the contained request of membership
        if(oldTimestamp > timestamp)
            return;
    }



}
