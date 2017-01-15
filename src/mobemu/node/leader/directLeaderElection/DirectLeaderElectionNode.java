package mobemu.node.leader.directLeaderElection;

import mobemu.node.Context;
import mobemu.node.Node;
import mobemu.node.leader.LeaderNode;
import mobemu.node.leader.directLeaderElection.dto.LeaderCandidacy;
import mobemu.node.leader.directLeaderElection.dto.LeaderCommunity;
import mobemu.node.leader.directLeaderElection.dto.DirectLeaderMessage;

import java.util.ArrayList;
import java.util.List;

import static mobemu.utils.Constants.communityMaxHop;

/**
 * Created by radu on 1/13/2017.
 */
public class DirectLeaderElectionNode extends LeaderNode {

    /**
     * List of leader candidacies from other nodes
     */
    protected List<LeaderCandidacy> candidacies;

    protected List<DirectLeaderMessage> heartBeats;

    protected List<DirectLeaderMessage> ownHeartBeats;

    protected LeaderCommunity leaderCommunity;

    protected List<Long> responseTimes;

    /**
     * Constructor for the {@link Node} class.
     *
     * @param id                  ID of the node
     * @param nodes               total number of existing nodes
     * @param context             the context of this node
     * @param socialNetwork       the social network as seen by this node
     * @param dataMemorySize      the maximum allowed size of the data memory
     * @param exchangeHistorySize the maximum allowed size of the exchange
     *                            history
     * @param seed                the seed for the random number generators
     * @param traceStart          timestamp of the start of the trace
     * @param traceEnd            timestamp of the end of the trace
     */
    public DirectLeaderElectionNode(int id, int nodes, Context context, boolean[] socialNetwork, int dataMemorySize, int exchangeHistorySize, long seed, long traceStart, long traceEnd) {
        super(id, nodes, context, socialNetwork, dataMemorySize, exchangeHistorySize, seed, traceStart, traceEnd);

        candidacies = new ArrayList<>();
        heartBeats = new ArrayList<>();
        ownHeartBeats = new ArrayList<>();
        leaderCommunity = new LeaderCommunity();
        responseTimes = new ArrayList<>();
    }

    public List<Long> getResponseTimes(){
        return responseTimes;
    }

    @Override
    protected void onDataExchange(Node encounteredNode, long contactDuration, long currentTime) {
        if (!(encounteredNode instanceof DirectLeaderElectionNode)) {
            return;
        }

        generateCandidacy(currentTime);

        DirectLeaderElectionNode encounteredLeaderNode = (DirectLeaderElectionNode) encounteredNode;
        exchangeCandidacies(encounteredLeaderNode.candidacies, currentTime);
        exchangeHeartBeats(encounteredLeaderNode.heartBeats, encounteredLeaderNode.ownHeartBeats, currentTime);

//        super.onDataExchange(encounteredNode, contactDuration, currentTime);

    }

    public LeaderCandidacy generateCandidacy(long currentTime){

        LeaderCandidacy leaderCandidacy = new LeaderCandidacy(id, getCentrality(false), communityMaxHop,
                currentTime);

        removeOldCandidacy();

        candidacies.add(leaderCandidacy);

        return leaderCandidacy;
    }

    public LeaderCandidacy removeOldCandidacy(){
        LeaderCandidacy formerCandidacy = null;
        for (LeaderCandidacy candidacy: candidacies){
            if(candidacy.getNodeId() == id){
                formerCandidacy = candidacy;
                break;
            }
        }

        if(formerCandidacy != null){
            candidacies.remove(formerCandidacy);
        }

        return formerCandidacy;
    }

    public void exchangeCandidacies(List<LeaderCandidacy> encounteredNodeCandidacies, long currentTime){
        for(LeaderCandidacy encounteredCandidacy: encounteredNodeCandidacies){
            LeaderCandidacy encounteredCandidacyClone = new LeaderCandidacy(encounteredCandidacy);

            if(encounteredCandidacyClone.exceededHopCount())
                continue;

            LeaderCandidacy candidacyWithId = LeaderCandidacy.containsId(candidacies,
                    encounteredCandidacyClone.getNodeId());

            if(candidacyWithId != null){
                //current node contains a candidacy for the given node
                if(encounteredCandidacyClone.getTimestamp() > candidacyWithId.getTimestamp()){
                    //encountered candidacy is more updated than the local one
                    candidacies.remove(candidacyWithId);
                    addCandidacy(encounteredCandidacyClone, currentTime);
                }
            }else{
                //current node does not contain any candidacy for the given node
                addCandidacy(encounteredCandidacyClone, currentTime);
            }
        }
    }

    public void addCandidacy(LeaderCandidacy candidacy, long currentTime) {
        candidacies.add(candidacy);

        double trust = altruism.getPerceived(candidacy.getNodeId());
        double centrality = candidacy.getCentrality();

        if (leaderScore == 0) {
            //we accept leaders with 0 centrality if they have a positive trust value
            if (centrality == 0 && trust > 0) {
                leaderNodeId = candidacy.getNodeId();
            }
        }

        double candidateScore = candidacy.getCentrality() * altruism.getPerceived(candidacy.getNodeId());
        if (candidateScore > leaderScore) {
            leaderScore = candidateScore;

            changeLeader(candidacy.getNodeId(), currentTime);
        }
    }

    public void generateHeartBeat(long currentTime){
        ownHeartBeats.add(DirectLeaderMessage.CreateRequest(id, leaderNodeId, currentTime));
    }

    private void changeLeader(int newLeaderId, long currentTime){
        //generate ChangedLeader message for the old leader
        ownHeartBeats.add(DirectLeaderMessage.CreateChangedLeader(id, leaderNodeId, currentTime));

        leaderNodeId = newLeaderId;

        //generate Request of membership for the new leader
        generateHeartBeat(currentTime);
        //            System.out.println("Node " + id + " changed leader to " + leaderNodeId + " with score " + leaderScore
//            + ". Centrality = " + centrality + ", Trust = " + trust);
    }


    private void checkHeartBeat(DirectLeaderMessage heartBeat, long currentTime){
        if(heartBeat.getDestinationId() == id){
            deliverHeartBeat(heartBeat, currentTime);
            return;
        }

        if(heartBeats.contains(heartBeat) || ownHeartBeats.contains(heartBeat))
            return;

        heartBeats.add(heartBeat);
    }

    protected void exchangeHeartBeats(List<DirectLeaderMessage> encounteredHeartBeats,
                                      List<DirectLeaderMessage> encounteredOwnHeartBeats, long currentTime){

        for(DirectLeaderMessage heartBeat : encounteredHeartBeats){
            checkHeartBeat(heartBeat, currentTime);
        }

        for(DirectLeaderMessage heartBeat : encounteredOwnHeartBeats){
            checkHeartBeat(heartBeat, currentTime);
        }
    }

    protected void deliverHeartBeat(DirectLeaderMessage heartBeat, long currentTime){

        /**
         * if the heartbeat is a request, then the current node is considered a leader by the source of the heartbeat
         */
        int sourceId = heartBeat.getSourceId();
        if(heartBeat.isRequest()){
            if(!leaderCommunity.containsNode(sourceId)){
                leaderCommunity.addNode(sourceId, currentTime);

                ownHeartBeats.add(DirectLeaderMessage.CreateResponse(id, sourceId, currentTime));
            }
        }
        else if(heartBeat.isResponse())
        {
            long responseTime = currentTime - heartBeat.getTimestamp();
            responseTimes.add(responseTime);
//            System.out.println("Response Received after " + responseTime + "s!");
        }
        else{
            leaderCommunity.removeNode(sourceId, currentTime);
        }
    }
}
