package mobemu.node.leader;

import mobemu.algorithms.SPRINT;
import mobemu.node.Context;
import mobemu.node.Node;
import mobemu.node.leader.directLeaderElection.dto.DirectLeaderMessage;

import java.util.ArrayList;
import java.util.List;

import static mobemu.utils.Constants.heartBeatGenerationTime;

/**
 * Created by radu on 1/15/2017.
 */
public abstract class LeaderNode extends SPRINT {

    /**
     * The id of the Leader (for current node's point of view)
     */
    protected int leaderNodeId;

    /**
     * The score computed for the current leader
     */
    protected double leaderScore;

    protected List<DirectLeaderMessage> heartBeats;

    protected List<DirectLeaderMessage> ownHeartBeats;

    protected List<Long> responseTimes;

    protected List<DirectLeaderMessage> heartBeatsForCurrentNode;

    /**
     * Instantiates an {@code ONSIDE} object.
     *
     * @param id                  ID of the node
     * @param context             the context of this node
     * @param socialNetwork       the social network as seen by this node
     * @param dataMemorySize      the maximum allowed size of the data memory
     * @param exchangeHistorySize the maximum allowed size of the exchange
     *                            history
     * @param seed                the seed for the random number generators if routing is used
     * @param traceStart          timestamp of the start of the trace
     * @param traceEnd            timestamp of the end of the trace
     * @param altruism            {@code true} if altruism computations are performed, {@code false}
     *                            otherwise
     * @param nodes               array of all the nodes in the network
     * @param cacheMemorySize     size of the cache holding the most recent
     */
    public LeaderNode(int id, Context context, boolean[] socialNetwork, int dataMemorySize, int exchangeHistorySize, long seed, long traceStart, long traceEnd, boolean altruism, Node[] nodes, int cacheMemorySize) {
        super(id, context, socialNetwork, dataMemorySize, exchangeHistorySize, seed, traceStart, traceEnd, altruism, nodes, cacheMemorySize);

        leaderNodeId = id;
        leaderScore = 0.0;
        heartBeats = new ArrayList<>();
        ownHeartBeats = new ArrayList<>();
        responseTimes = new ArrayList<>();
        heartBeatsForCurrentNode = new ArrayList<>();
    }


    public int getLeaderNodeId() {
        return leaderNodeId;
    }

    public double getLeaderScore() {
        return leaderScore;
    }

    public List<Long> getResponseTimes(){
        return responseTimes;
    }

    private boolean checkOwnHeartBeats(long currentTime){
        for (DirectLeaderMessage heartbeat: ownHeartBeats){
            if(currentTime - heartbeat.getTimestamp() < heartBeatGenerationTime){
                return false;
            }
        }

        return true;
    }

    public void generateHeartBeat(long currentTime){
        if(leaderNodeId == -1)
            return;

        if(checkOwnHeartBeats(currentTime)){
            ownHeartBeats.add(DirectLeaderMessage.CreateRequest(id, leaderNodeId, currentTime));
        }

    }

    protected void deliverHeartBeat(DirectLeaderMessage heartBeat, long currentTime){
        int sourceId = heartBeat.getSourceId();
        long heartBeatTimestamp = heartBeat.getTimestamp();
        if(heartBeat.isRequest()){
            if(!heartBeatsForCurrentNode.contains(heartBeat)){
                heartBeatsForCurrentNode.add(heartBeat);
                ownHeartBeats.add(DirectLeaderMessage.CreateResponse(id, sourceId, heartBeatTimestamp));
            }
        }
        else if(heartBeat.isResponse())
        {
            long responseTime = currentTime - heartBeatTimestamp;
            responseTimes.add(responseTime);
//            System.out.println("Response Received after " + responseTime + "s!");
        }
    }

    private void checkHeartBeat(DirectLeaderMessage heartBeat, long currentTime,
                                List<DirectLeaderMessage> heartBeatsToAdd){
        if(heartBeat.getDestinationId() == id){
            deliverHeartBeat(heartBeat, currentTime);
            return;
        }

        if(heartBeats.contains(heartBeat) || ownHeartBeats.contains(heartBeat))
            return;

        heartBeatsToAdd.add(heartBeat);
    }

    protected void exchangeHeartBeats(List<DirectLeaderMessage> encounteredHeartBeats,
                                      List<DirectLeaderMessage> encounteredOwnHeartBeats, long currentTime){

        List<DirectLeaderMessage> heartBeatsToAdd = new ArrayList<>();
        for(DirectLeaderMessage heartBeat : encounteredHeartBeats){
            checkHeartBeat(heartBeat, currentTime, heartBeatsToAdd);
        }

        for(DirectLeaderMessage heartBeat : encounteredOwnHeartBeats){
            checkHeartBeat(heartBeat, currentTime, heartBeatsToAdd);
        }

        for(DirectLeaderMessage heartBeat: heartBeatsToAdd){
            heartBeats.add(heartBeat);
        }
    }

    @Override
    protected void onDataExchange(Node encounteredNode, long contactDuration, long currentTime) {
        if(this.equals(encounteredNode))
            return;

        LeaderNode encounteredLeaderNode = (LeaderNode) encounteredNode;
        exchangeHeartBeats(encounteredLeaderNode.heartBeats, encounteredLeaderNode.ownHeartBeats, currentTime);

        super.onDataExchange(encounteredNode, contactDuration, currentTime);
    }
}
