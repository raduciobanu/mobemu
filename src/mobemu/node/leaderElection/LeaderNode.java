package mobemu.node.leaderElection;

import mobemu.algorithms.SPRINT;
import mobemu.node.Context;
import mobemu.node.Node;
import mobemu.node.leaderElection.dto.LeaderCandidacy;
import mobemu.node.leaderElection.dto.LeaderHeartBeat;

import java.util.ArrayList;
import java.util.List;

import static mobemu.utils.Constants.communityMaxHop;

/**
 * Created by radu on 1/13/2017.
 */
public class LeaderNode extends SPRINT {

    /**
     * The id of the Leader (for current node's point of view)
     */
    protected int leaderNodeId;

    /**
     * The score computed for the current leader
     */
    protected double leaderScore;

    /**
     * List of leader candidacies from other nodes
     */
    protected List<LeaderCandidacy> candidacies;

    protected List<LeaderHeartBeat> heartBeats;

    protected List<Integer> leaderCommunity;

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
        candidacies = new ArrayList<>();
        heartBeats = new ArrayList<>();
        leaderCommunity = new ArrayList<>();
    }

    public int getLeaderNodeId() {
        return leaderNodeId;
    }

    public double getLeaderScore() {
        return leaderScore;
    }

    @Override
    protected void onDataExchange(Node encounteredNode, long contactDuration, long currentTime) {
        if (!(encounteredNode instanceof LeaderNode)) {
            return;
        }

        generateCandidacy(currentTime);

        LeaderNode encounteredLeaderNode = (LeaderNode) encounteredNode;
        exchangeCandidacies(encounteredLeaderNode.candidacies, currentTime);
        exchangeHeartBeats(encounteredLeaderNode.heartBeats, currentTime);

        super.onDataExchange(encounteredNode, contactDuration, currentTime);

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
            leaderNodeId = candidacy.getNodeId();

            heartBeats.add(new LeaderHeartBeat(id, leaderNodeId, true, currentTime));
//            System.out.println("Node " + id + " changed leader to " + leaderNodeId + " with score " + leaderScore
//            + ". Centrality = " + centrality + ", Trust = " + trust);
        }
    }


    protected void exchangeHeartBeats(List<LeaderHeartBeat> encounteredHeartBeats, long currentTime){

        for(LeaderHeartBeat heartBeat : encounteredHeartBeats){
            if(heartBeat.getDestinationId() == id){
                deliverHeartBeat(heartBeat, currentTime);
                continue;
            }

            if(heartBeats.contains(heartBeat))
                continue;

            heartBeats.add(heartBeat);
        }
    }

    protected void deliverHeartBeat(LeaderHeartBeat heartBeat, long currentTime){

        /**
         * if the heartbeat is a request, then the current node is considered a leader by the source of the heartbeat
         */
        if(heartBeat.isRequest()){
            int heartBeatSource = heartBeat.getSourceId();
            if(!leaderCommunity.contains(heartBeatSource)){
                leaderCommunity.add(heartBeatSource);

                heartBeats.add(new LeaderHeartBeat(id, heartBeatSource, false, currentTime));
            }
        }
    }
}
