package mobemu.node.leader.directLeaderElection;

import mobemu.node.Context;
import mobemu.node.Node;
import mobemu.node.leader.LeaderNode;
import mobemu.node.leader.directLeaderElection.dto.CommunityByLeader;
import mobemu.node.leader.directLeaderElection.dto.LeaderCandidacy;
import mobemu.node.leader.directLeaderElection.dto.DirectLeaderMessage;

import java.text.DecimalFormat;
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

    protected CommunityByLeader communityByLeader;

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
    public DirectLeaderElectionNode(int id, Context context, boolean[] socialNetwork, int dataMemorySize,
                                    int exchangeHistorySize, long seed, long traceStart, long traceEnd, boolean altruism, Node[] nodes, int cacheMemorySize) {
        super(id, context, socialNetwork, dataMemorySize, exchangeHistorySize, seed, traceStart, traceEnd, altruism, nodes, cacheMemorySize);

        candidacies = new ArrayList<>();
        communityByLeader = new CommunityByLeader();
    }

    @Override
    protected void onDataExchange(Node encounteredNode, long contactDuration, long currentTime) {
        if (!(encounteredNode instanceof DirectLeaderElectionNode)) {
            return;
        }

        generateCandidacy(currentTime);

        DirectLeaderElectionNode encounteredLeaderNode = (DirectLeaderElectionNode) encounteredNode;
        exchangeCandidacies(encounteredLeaderNode.candidacies, currentTime);

        super.onDataExchange(encounteredNode, contactDuration, currentTime);
    }

    public LeaderCandidacy generateCandidacy(long currentTime){

        LeaderCandidacy leaderCandidacy = new LeaderCandidacy(id, getNormalizedCentrality(false),
                communityMaxHop, currentTime);

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

        double trust = altruism.getNormalizedPerceived(candidacy.getNodeId());
        double centrality = candidacy.getCentrality();
//        double candidacyLatency = currentTime - candidacy.getTimestamp();
        double latencyValue = candidacy.getNormalizedLatencyValue(currentTime);
        double probabilityOfMeeting = getProbabilityOfMeetingNode(this, candidacy.getNodeId(), currentTime);
//        System.out.println(candidacyLatency);

//        DecimalFormat df = new DecimalFormat("#.###");
//        String formattedCentrality = df.format(centrality);
//        System.out.println("Id: " + candidacy.getNodeId() + ", Trust: " + trust + ", Centrality: " + formattedCentrality
//                + ", Latency: " + latencyValue + ", Probability: " + probabilityOfMeeting);

//        if (leaderScore == 0) {
//            //we accept leaders with 0 centrality if they have a positive trust value
//            if (centrality == 0 && trust > 0) {
//                leaderNodeId = candidacy.getNodeId();
//            }
//        }


//        double candidateScore = candidacy.getCentrality() * altruism.getPerceived(candidacy.getNodeId());

        double candidateScore = computeLeaderScore(centrality, trust, latencyValue, probabilityOfMeeting);

        if (Double.compare(candidateScore,leaderScore) > 0) {
            leaderScore = candidateScore;

            changeLeader(candidacy.getNodeId(), currentTime);
        }
    }

    @Override
    protected void deliverHeartBeat(DirectLeaderMessage heartBeat, long currentTime, int encounteredNodeId){

        /**
         * if the heartbeat is a request, then the current node is considered a leader by the source of the heartbeat
         */
        int sourceId = heartBeat.getSourceId();
        if(heartBeat.isRequest()){
            if(!communityByLeader.containsNode(sourceId)){
                communityByLeader.addNode(sourceId, currentTime);
            }
        }
        else if(heartBeat.isChangedLeader()){
            communityByLeader.removeNode(sourceId, currentTime);
        }

        super.deliverHeartBeat(heartBeat, currentTime, encounteredNodeId);
    }

    private void changeLeader(int newLeaderId, long currentTime){
        if(leaderNodeId != id){
            //generate ChangedLeader message for the old leader
            ownHeartBeats.add(DirectLeaderMessage.CreateChangedLeader(id, leaderNodeId, currentTime));
        }

        leaderNodeId = newLeaderId;

        //generate Request of membership for the new leader
        generateHeartBeat(currentTime);
        //            System.out.println("Node " + id + " changed leader to " + leaderNodeId + " with score " + leaderScore
//            + ". Centrality = " + centrality + ", Trust = " + trust);
    }

    @Override
    public String getName() {
        return this.getClass().getName();
    }

}
