package mobemu.node.leader;

import mobemu.algorithms.SPRINT;
import mobemu.node.Context;
import mobemu.node.Node;
import mobemu.node.leader.directLeaderElection.dto.DirectLeaderMessage;
import mobemu.node.leader.directLeaderElection.dto.HeartbeatResponse;
import sun.plugin2.message.HeartbeatMessage;

import java.util.*;

import static mobemu.utils.Constants.*;

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

    protected List<HeartbeatResponse> responseTimes;

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

    public List<HeartbeatResponse> getResponseTimes(){
        return responseTimes;
    }

    public List<DirectLeaderMessage> getHeartBeats(){
        return heartBeats;
    }

    public List<DirectLeaderMessage> getOwnHeartBeats(){
        return ownHeartBeats;
    }

    public boolean isNormalized(double value){
        return value >= 0 && value <= 1;
    }

    public double computeLeaderScore(double centrality, double trust, double latencyValue, double probabilityOfMeeting){
        if(!isNormalized(centrality) || !isNormalized(trust) || !isNormalized(latencyValue)
                || !isNormalized(probabilityOfMeeting))
            System.out.println("VALUES NOT NORMALIZED!!!!!");

        return centralityWeight * centrality + trustWeight * trust + latencyWeight * latencyValue
                + probabilityWeight * probabilityOfMeeting;
    }

    public double computeLeaderScore(double centrality, double trust, double probabilityOfMeeting){
        if(!isNormalized(centrality) || !isNormalized(trust) || !isNormalized(probabilityOfMeeting))
            System.out.println("VALUES NOT NORMALIZED!!!!!");


        return centralityWeight * centrality + trustWeight * trust + probabilityWeight * probabilityOfMeeting;
    }

    public double getProbabilityOfMeetingNode(LeaderNode node, int encounteredNodeId, long currentTime){
        Calendar today = Calendar.getInstance();
        today.setTimeInMillis(currentTime);
        int hourNow = today.get(Calendar.HOUR_OF_DAY);
        today.set(Calendar.HOUR_OF_DAY, 0);
        today.set(Calendar.MINUTE, 0);
        today.set(Calendar.SECOND, 0);
        int dayNow = today.get(Calendar.DAY_OF_MONTH);

        ArrayList<ArrayList<Probability>> futureEncounters = computeFutureEncounters(node, currentTime, hourNow, dayNow);

        if(futureEncounters == null)
            return -1.0;

        double sumProbabilities = 0.0;
        for (int hour = 0; hour < futureEncounters.size(); hour++){
            ArrayList<Probability> encountersInAnHour = futureEncounters.get(hour);

            for (Probability probability: encountersInAnHour){
                if(probability.getId() == encounteredNodeId){
                    sumProbabilities += probability.getProbability();
                }
            }
        }

        double averageProbability = 10 * (sumProbabilities/HOURS_IN_DAY);

        return averageProbability * 10 > 1? 1: averageProbability;
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
        if(leaderNodeId == -1 || leaderNodeId == this.id)
            return;

        if(checkOwnHeartBeats(currentTime)){
            ownHeartBeats.add(DirectLeaderMessage.CreateRequest(id, leaderNodeId, currentTime));
        }

    }

    public boolean containsResponseForHeartBeat(DirectLeaderMessage heartBeat){
        for(HeartbeatResponse heartbeatResponse : responseTimes){
            if(heartbeatResponse.getHeartbeatId() == heartBeat.getId()){
                return true;
            }
        }
        return false;
    }

    protected void deliverHeartBeat(DirectLeaderMessage heartBeat, long currentTime, int encounteredNodeId){
        int sourceId = heartBeat.getSourceId();
        long heartBeatTimestamp = heartBeat.getTimestamp();

        heartBeat.transfer(encounteredNodeId, this.id);

        if(heartBeat.isRequest()){
            if(!heartBeatsForCurrentNode.contains(heartBeat)){
                heartBeatsForCurrentNode.add(heartBeat);
                ownHeartBeats.add(DirectLeaderMessage.CreateResponse(this.id, sourceId, heartBeatTimestamp,
                        heartBeat.getHopCount(this.id)));
            }
        }
        else if(heartBeat.isResponse())
        {
            int responseSourceId = heartBeat.getSourceId();
            if(responseSourceId != leaderNodeId){
                return;
            }

            long responseTime = currentTime - heartBeatTimestamp;
            if(!containsResponseForHeartBeat(heartBeat)){
                responseTimes.add(new HeartbeatResponse(heartBeat.getId(), responseTime, heartBeat.getHopCount(this.id)));
//            System.out.println("AddResponse Received after " + responseTime + "s!");
            }
        }
    }

    private void checkHeartBeat(DirectLeaderMessage heartBeat, long currentTime, int encounteredNodeId,
                                List<DirectLeaderMessage> heartBeatsToAdd){
        if(heartBeat.getDestinationId() == id){
            deliverHeartBeat(heartBeat, currentTime, encounteredNodeId);
            return;
        }

        if(heartBeats.contains(heartBeat) || ownHeartBeats.contains(heartBeat))
            return;

//        heartBeatsToAdd.add(heartBeat.copy());
        heartBeat.transfer(encounteredNodeId, this.id);
        heartBeatsToAdd.add(heartBeat);
    }

    protected void exchangeHeartBeats(int encounteredNodeId, List<DirectLeaderMessage> encounteredHeartBeats,
                                      List<DirectLeaderMessage> encounteredOwnHeartBeats, long currentTime){

        List<DirectLeaderMessage> heartBeatsToAdd = new ArrayList<>();
        for(DirectLeaderMessage heartBeat : encounteredHeartBeats){
            checkHeartBeat(heartBeat, currentTime, encounteredNodeId, heartBeatsToAdd);
        }

        for(DirectLeaderMessage heartBeat : encounteredOwnHeartBeats){
            checkHeartBeat(heartBeat, currentTime, encounteredNodeId, heartBeatsToAdd);
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
        exchangeHeartBeats(encounteredNode.getId(), encounteredLeaderNode.heartBeats,
                encounteredLeaderNode.ownHeartBeats, currentTime);

        super.onDataExchange(encounteredNode, contactDuration, currentTime);
    }
}
