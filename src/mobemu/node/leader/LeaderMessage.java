package mobemu.node.leader;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by radu on 1/15/2017.
 */
public abstract class LeaderMessage{
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
     * The generation time of the heartbeat
     */
    private long timestamp;

    /**
     * Dictionary containing the number of hops traveled by the message to reach each node
     * Key = node's Id, Value = number of hops traveled until current node
     */
    private Map<Integer, Integer> hopCountPerNode;

    public LeaderMessage(int sourceId, int destinationId, long timestamp){
        this.id = heartBeatCount++;
        this.sourceId = sourceId;
        this.destinationId = destinationId;
        this.timestamp = timestamp;
        this.hopCountPerNode = new HashMap<>();
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

    public long getTimestamp() {
        return timestamp;
    }

    public int getHopCount(int nodeId) {
        if(!hopCountPerNode.containsKey(nodeId)){
            return -1;
        }

        return hopCountPerNode.get(nodeId);
    }

    public void setHopCount(int nodeId, int hopCount){
        this.hopCountPerNode.put(nodeId, hopCount);
    }

    /**
     * Increases the hop count for the receiver according to the hop count of the sender
     * @param fromNodeId the id of the node the sender
     * @param toNodeId the id of the receiver
     */
    public void transfer(int fromNodeId, int toNodeId){
        if(hopCountPerNode.containsKey(toNodeId))
            return;

        if(!hopCountPerNode.containsKey(fromNodeId)){
            hopCountPerNode.put(fromNodeId, 0);
        }

        int hopCountUntilFromNode = hopCountPerNode.get(fromNodeId);
        hopCountPerNode.put(toNodeId, hopCountUntilFromNode + 1);
    }
}
