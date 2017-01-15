package mobemu.node.leader.communityBasedLeaderElection;

import mobemu.node.Context;
import mobemu.node.Node;
import mobemu.node.leader.LeaderNode;
import mobemu.node.leader.communityBasedLeaderElection.dto.CommunityMessage;
import mobemu.node.leader.communityBasedLeaderElection.dto.LeaderCommunity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static mobemu.utils.Constants.leaderCommunityThreshold;

/**
 * Created by radu on 1/15/2017.
 */
public class CommunityLeaderNode extends LeaderNode {

    /**
     *  The community of the current node
     *  Key = nodeId, Value = node centrality
     */
    protected LeaderCommunity leaderCommunity;

    /**
     * The list of the community messages carried in the benefit of others
     */
    protected List<CommunityMessage> communityMessages;

    /**
     * The list of the community messages issued by the current node
     */
    protected List<CommunityMessage> ownCommunityMessages;

    /**
     * Dictionary of issued requests
     * Key = the id of the node, Value = the centrality of the node
     */
    protected Map<Integer, Double> requestsSent;
    /**
     * Dictionary of responses received for the requests made
     * key = the id of the node whom the request was issued for
     * value = the list of nodes that responded to the requests
     */
    protected Map<Integer, List<Integer>> receivedResponses;

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
    public CommunityLeaderNode(int id, int nodes, Context context, boolean[] socialNetwork, int dataMemorySize,
                               int exchangeHistorySize, long seed, long traceStart, long traceEnd) {
        super(id, nodes, context, socialNetwork, dataMemorySize, exchangeHistorySize, seed, traceStart, traceEnd);

        this.leaderCommunity = new LeaderCommunity();
        this.leaderCommunity.addNode(this);

        this.communityMessages = new ArrayList<>();
        this.ownCommunityMessages = new ArrayList<>();
        this.requestsSent = new HashMap<>();
        this.receivedResponses = new HashMap<>();
    }

    @Override
    protected void addToLocalCommunity(Node encounteredNode){
        if(!(encounteredNode instanceof CommunityLeaderNode)){
            return;
        }
        CommunityLeaderNode encounteredLeaderNode = (CommunityLeaderNode)encounteredNode;

        super.addToLocalCommunity(encounteredLeaderNode);

        //if current node is in the local community of encountered node => both nodes are in the localCommunity of each other
        //add the nodes in the CommunityByLeader of each other
        if(encounteredLeaderNode.inLocalCommunity(id)){
            requestMembershipPermissionForNode(encounteredLeaderNode, currentTime);
        }
    }

    private void requestMembershipPermissionForNode(CommunityLeaderNode encounteredLeaderNode, long currentTime){
        int encounteredNodeId = encounteredLeaderNode.getId();
        if(receivedResponses.containsKey(encounteredNodeId))
            return;

        if(leaderCommunity.size() == 1){
            addToLeaderCommunity(encounteredLeaderNode);
            encounteredLeaderNode.addToLeaderCommunity(this);
            return;
        }

        for(int nodeId : leaderCommunity.getNodes()){
            CommunityMessage request = CommunityMessage.CreateRequest(this.id, nodeId, encounteredNodeId, currentTime);
            ownCommunityMessages.add(request);
        }

        requestsSent.put(encounteredNodeId, encounteredLeaderNode.getCentrality(true));

        receivedResponses.put(encounteredNodeId, new ArrayList<>());
    }

    protected void addToLeaderCommunity(Node encounteredNode){
        if(!leaderCommunity.containsNode(encounteredNode.getId()))
            leaderCommunity.addNode(encounteredNode);
    }

    protected void generateCommunityResponse(int destinationId, int targetId, long currentTime){
        ownCommunityMessages.add(CommunityMessage.CreateResponse(id, destinationId, targetId, currentTime));
    }

    protected void electLeader(){
        double maxScore = 0.0;
        int leaderId = -1;
        for(int nodeId: leaderCommunity.getNodes()){
            double score = leaderCommunity.get(nodeId) * altruism.getPerceived(nodeId);

            if(score > maxScore){
                maxScore = score;
                leaderId = nodeId;
            }
        }
    }

    protected void checkLeaderCommunityThreshold(int targetId, long currentTime){
        List<Integer> nodesThatConfirmed = receivedResponses.get(targetId);
        if(nodesThatConfirmed.size() > leaderCommunityThreshold * leaderCommunity.size()){
            double targetCentrality = requestsSent.get(targetId);

            leaderCommunity.add(targetId, targetCentrality);

            for(int nodeId : leaderCommunity.getNodes()){
                ownCommunityMessages.add(CommunityMessage.CreateAddedNode(id, nodeId, targetId, currentTime));

                electLeader();
            }
        }
    }

    protected void deliverCommunityMessage(CommunityMessage message, long currentTime){
        int targetId = message.getTargetId();
        int sourceId = message.getSourceId();
        if(message.isRequest()){
            if(inLocalCommunity(targetId)){
                generateCommunityResponse(sourceId, targetId, currentTime);
            }
        }
        else if(message.isResponse()){
            List<Integer> nodesThatConfirmed = receivedResponses.get(targetId);
            if(!nodesThatConfirmed.contains(targetId)){
                nodesThatConfirmed.add(targetId);

                checkLeaderCommunityThreshold(targetId, currentTime);
            }
        }
        else if(message.isAddedNode()){
            double targetCentrality = requestsSent.get(targetId);
            leaderCommunity.add(targetId, targetCentrality);
        }
    }

    protected void checkCommunityMessage(CommunityMessage message, long currentTime){
        if(message.getDestinationId() == this.id){
            deliverCommunityMessage(message, currentTime);
            return;
        }

        if(communityMessages.contains(message) || ownCommunityMessages.contains(message))
            return;

        communityMessages.add(message);
    }

    protected void exchangeCommunityMessages(CommunityLeaderNode leaderNode, long currentTime){
        for(CommunityMessage message: leaderNode.communityMessages){
            checkCommunityMessage(message, currentTime);
        }

        for(CommunityMessage message: leaderNode.ownCommunityMessages){
            checkCommunityMessage(message, currentTime);
        }
    }

    @Override
    protected void onDataExchange(Node encounteredNode, long contactDuration, long currentTime) {
        CommunityLeaderNode leaderNode = (CommunityLeaderNode) encounteredNode;
        exchangeCommunityMessages(leaderNode, currentTime);
    }

}
