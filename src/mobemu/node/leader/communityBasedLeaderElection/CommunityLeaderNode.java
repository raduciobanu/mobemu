package mobemu.node.leader.communityBasedLeaderElection;

import mobemu.node.ContactInfo;
import mobemu.node.Context;
import mobemu.node.Node;
import mobemu.node.leader.LeaderNode;
import mobemu.node.leader.communityBasedLeaderElection.dto.communityMessage.CommunityMessage;
import mobemu.node.leader.communityBasedLeaderElection.dto.LeaderCommunity;
import mobemu.node.leader.communityBasedLeaderElection.dto.proposals.LeaderProposal;
import mobemu.node.leader.communityBasedLeaderElection.dto.proposals.collections.AddProposalCollection;
import mobemu.node.leader.communityBasedLeaderElection.dto.proposals.collections.LeaderProposalCollection;
import mobemu.node.leader.communityBasedLeaderElection.dto.communityMessage.handlers.*;
import mobemu.node.leader.communityBasedLeaderElection.dto.proposals.collections.RemoveProposalCollection;

import java.util.*;

/**
 * Created by radu on 1/15/2017.
 */
public class CommunityLeaderNode extends LeaderNode {

    /**
     * threshold related to last encounter with a given node
     * if the time passed since last encounter with a node exceeded this value
     * a request should be issued to exclude the node form community
     */
    private static long TIME_SINCE_LAST_ENCOUNTER_THRESHOLD = 1000 * 60 * 60 * 24;
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
//    protected Map<Integer, Double> requestsSent;
    /**
     * Dictionary of responses received for the requests made
     * key = the id of the node whom the request was issued for
     * value = the list of nodes that responded to the requests
     */
//    protected Map<Integer, List<Integer>> receivedResponses;

    /**
     * Proposals for electing a leader in the community
     */
    protected LeaderProposalCollection leaderProposalCollection;

    /**
     * The timestamp of the latest leader change
     */
    protected long leaderElectionTimestamp;

    /**
     * Handler for community messages
     */
    private CommunityMessageHandler addedNodeHandler;

    /**
     * Nodes from leader community seen in the last period
     */
    private List<Integer> nodesSeen;

    /**
     * Nodes from leader community not seen in the last period
     */
    private List<Integer> nodesNotSeen;

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
    public CommunityLeaderNode(int id, Context context, boolean[] socialNetwork, int dataMemorySize, int exchangeHistorySize, long seed, long traceStart, long traceEnd, boolean altruism, Node[] nodes, int cacheMemorySize) {
        super(id, context, socialNetwork, dataMemorySize, exchangeHistorySize, seed, traceStart, traceEnd, altruism, nodes, cacheMemorySize);

        this.leaderNodeId = -1;
        this.leaderCommunity = new LeaderCommunity();
        this.leaderCommunity.addNode(this, traceStart);

        this.communityMessages = new ArrayList<>();
        this.ownCommunityMessages = new ArrayList<>();
        this.nodesSeen = new ArrayList<>();
        this.nodesNotSeen = new ArrayList<>();
//        this.requestsSent = new HashMap<>();
//        this.receivedResponses = new HashMap<>();
        this.leaderProposalCollection = new LeaderProposalCollection();
        AddCommunityMessageHandlers();
    }

    public void AddCommunityMessageHandlers(){
        addedNodeHandler = new AddedNodeHandler(this);
        CommunityMessageHandler addRequestHandler = new AddRequestHandler(this);
        CommunityMessageHandler removedRequestHandler = new RemovedRequestHandler(this);
        CommunityMessageHandler removeRequestHandler = new RemoveRequestHandler(this);
        CommunityMessageHandler leaderElectedHandler = new LeaderElectedHandler(this);
        CommunityMessageHandler leaderProposalHandler = new LeaderProposalHandler(this);

        addedNodeHandler.setNextHandler(addRequestHandler);
        addRequestHandler.setNextHandler(removedRequestHandler);
        removedRequestHandler.setNextHandler(removeRequestHandler);
        removeRequestHandler.setNextHandler(leaderElectedHandler);
        leaderElectedHandler.setNextHandler(leaderProposalHandler);
    }

    public int getLeaderNodeId(){
        return leaderNodeId;
    }

    public long getLeaderElectionTimestamp(){
        return leaderElectionTimestamp;
    }

    public List<Integer> getNodesSeen(){
        return nodesSeen;
    }

    public List<Integer> getNodesNotSeen(){
        return nodesNotSeen;
    }

    public Set<Integer> getLeaderCommunityNodes() {
        return leaderCommunity.getNodes();
    }

    public void addToLeaderCommunity(int nodeId, double nodeCentrality, long currentTime){
        leaderCommunity.add(nodeId, nodeCentrality, currentTime);
    }

    protected void addToLeaderCommunity(Node encounteredNode, long currentTime){
        if(!leaderCommunity.containsNode(encounteredNode.getId())){
            leaderCommunity.addNode(encounteredNode, currentTime);
            proposeLeader(currentTime);
        }
    }

    public void removeFromLeaderCommunity(int nodeId){
        if(nodeId == id)
            return;

        leaderCommunity.remove(nodeId);
    }

    public LeaderProposalCollection getLeaderProposalCollection(){
        return leaderProposalCollection;
    }

    public void addToLeaderProposals(int sourceId, int targetId, long currentTime, double score){
        LeaderProposal proposal = new LeaderProposal(sourceId, targetId, currentTime, score);
        leaderProposalCollection.add(proposal);
    }

//    public Map<Integer, Double> getRequestsSent(){
//        return requestsSent;
//    }

//    public Map<Integer, List<Integer>> getReceivedResponses(){
//        return receivedResponses;
//    }

    public void addToOwnCommunityMessages(CommunityMessage communityMessage){
        ownCommunityMessages.add(communityMessage);
    }

    @Override
    protected void addToLocalCommunity(Node encounteredNode, long currentTime){
        if(!(encounteredNode instanceof CommunityLeaderNode)){
            return;
        }
        CommunityLeaderNode encounteredLeaderNode = (CommunityLeaderNode)encounteredNode;

        super.addToLocalCommunity(encounteredLeaderNode, currentTime);

        //if current node is in the local community of encountered node => both nodes are in the localCommunity of each other
        //add the nodes in the CommunityByLeader of each other
        if(encounteredLeaderNode.inLocalCommunity(id)){
            requestMembershipPermissionForNode(encounteredLeaderNode, currentTime);
        }
    }

    public boolean shouldBeToLeaderCommunity(int targetId){
        return inLocalCommunity(targetId) || inSocialNetwork(targetId);
    }

    private void requestMembershipPermissionForNode(CommunityLeaderNode encounteredLeaderNode, long currentTime){
        int encounteredNodeId = encounteredLeaderNode.getId();
//        if(receivedResponses.containsKey(encounteredNodeId))
//            return;

        if(leaderCommunity.size() == 1){
            addToLeaderCommunity(encounteredLeaderNode, currentTime);
            encounteredLeaderNode.addToLeaderCommunity(this, currentTime);
//            proposeLeader(currentTime);
            return;
        }

        for(int nodeId : leaderCommunity.getNodes()){
            CommunityMessage request = CommunityMessage.CreateAddRequest(this.id, nodeId,
                    encounteredNodeId, currentTime);
            addToOwnCommunityMessages(request);
        }

//        requestsSent.put(encounteredNodeId, encounteredLeaderNode.getNormalizedCentrality(true));

//        receivedResponses.put(encounteredNodeId, new ArrayList<>());
    }

//    protected void generateCommunityResponse(int destinationId, int targetId, long currentTime, int hopCount){
//        double probabilityOfMeeting = getProbabilityOfMeetingNode(this, targetId, currentTime);
//        if(probabilityOfMeeting > 0.0){
//            System.out.println(this.id + " will meet " + targetId + " with a probability of "
//                    + probabilityOfMeeting);
//        }
//
//        if(inLocalCommunity(targetId) || inSocialNetwork(targetId)) {
//            ownCommunityMessages.add(CommunityMessage.CreateResponse(id, destinationId, targetId, currentTime, hopCount));
//        }
//    }

    public void proposeLeader(long currentTime){
        double maxScore = 0.0;
        int leaderId = -1;
        for(int nodeId: leaderCommunity.getNodes()){
            double centrality = leaderCommunity.get(nodeId);
            double trust = altruism.getNormalizedPerceived(nodeId);
            double probabilityOfMeeting = getProbabilityOfMeetingNode(this, nodeId, currentTime);

//            DecimalFormat df = new DecimalFormat("#.###");
//            String formattedCentrality = df.format(centrality);
//            System.out.println("Id: " + nodeId + ", Trust: " + trust + ", Centrality: " + formattedCentrality
//                    + ", Probability: " + probabilityOfMeeting);

            double score = computeLeaderScore(centrality, trust, probabilityOfMeeting);

//            if(leaderProposalCollection.contains(nodeId, id, score))
//                continue;

            if(score > maxScore){
                maxScore = score;
                leaderId = nodeId;
            }
        }

        //if a leader has been elected locally, send proposal to all community nodes
        if(leaderId != -1){
            for(int nodeId: leaderCommunity.getNodes()) {
                double centrality = leaderCommunity.get(nodeId);

                addToLeaderProposals(id, leaderId, currentTime, maxScore);
                addToOwnCommunityMessages(CommunityMessage.CreateLeaderProposal(id, nodeId, leaderId, centrality,
                        currentTime));

            }
        }
    }

//    protected void checkLeaderCommunityThreshold(int targetId, long currentTime){
//        List<Integer> nodesThatConfirmed = receivedResponses.get(targetId);
//        if(nodesThatConfirmed.size() > leaderCommunityThreshold * leaderCommunity.size() - 1){
//            double targetCentrality = requestsSent.get(targetId);
//
//            leaderCommunity.add(targetId, targetCentrality, currentTime);
//
//            for(int nodeId : leaderCommunity.getNodes()){
//                ownCommunityMessages.add(CommunityMessage.CreateAddedNode(id, nodeId, targetId, targetCentrality, currentTime));
//                proposeLeader(currentTime);
//            }
//        }
//    }

    public void electLeader(int leaderNodeId, long currentTime){
        this.leaderNodeId = leaderNodeId;
        this.leaderElectionTimestamp = currentTime;
    }

    protected void deliverCommunityMessage(CommunityMessage message, long currentTime, int encounteredNodeId){
//        int targetId = message.getTargetId();
//        int sourceId = message.getSourceId();

        message.transfer(encounteredNodeId, this.id);
        try {
            addedNodeHandler.receiveMessage(message, currentTime);
        } catch (Exception e) {
            e.printStackTrace();
        }
//        if(message.isRequest()){
//            generateCommunityResponse(sourceId, targetId, currentTime, message.getHopCount(this.id));
//        }
//        else if(message.isResponse()){
//            List<Integer> nodesThatConfirmed = receivedResponses.get(targetId);
//            if(!nodesThatConfirmed.contains(targetId)){
//                nodesThatConfirmed.add(targetId);
//
//                checkLeaderCommunityThreshold(targetId, currentTime);
//            }
//        }
//        else if(message.isAddedNode()){
//            double targetCentrality = message.getTargetCentrality();
//            leaderCommunity.add(targetId, targetCentrality, currentTime);
//        }
//        else if(message.isLeaderProposal()){
//            double score = message.getScore();
//            leaderProposalCollection.addProposal(targetId, sourceId, score);
//
//            if(Double.compare(leaderProposalCollection.size(), leaderProposalsThreshold * leaderCommunity.size() - 1) > 0){
//                int newLeaderId = leaderProposalCollection.getLeader();
//
//                if(newLeaderId != leaderNodeId && newLeaderId != -1){
//                    //send LeaderElected message to all nodes from leaderCommunity
//                    for (int nodeId : leaderCommunity.getNodes()){
//                        ownCommunityMessages.add(CommunityMessage.CreateLeaderElected(this.id, nodeId, targetId,
//                                currentTime));
//                    }
//
//                    electLeader(newLeaderId, currentTime);
//                }
//            }
//        }
//        else if(message.isLeaderElected()){
//            long messageTimestamp = message.getTimestamp();
//
//            if(messageTimestamp > leaderElectionTimestamp){
//                electLeader(targetId, messageTimestamp);
//            }
//        }

    }

    protected void checkCommunityMessage(CommunityMessage message, long currentTime, int encounteredNodeId){
        if(message.getDestinationId() == this.id){
            deliverCommunityMessage(message, currentTime, encounteredNodeId);
            return;
        }

        if(communityMessages.contains(message) || ownCommunityMessages.contains(message))
            return;

        communityMessages.add(message);
    }

    protected void exchangeCommunityMessages(CommunityLeaderNode leaderNode, long currentTime, int encounteredNodeId){
        for(CommunityMessage message: leaderNode.communityMessages){
            checkCommunityMessage(message, currentTime, encounteredNodeId);
        }

        for(CommunityMessage message: leaderNode.ownCommunityMessages){
            checkCommunityMessage(message, currentTime, encounteredNodeId);
        }
    }

    private void updatesCentralities(CommunityLeaderNode encounteredNode, long currentTime){
        boolean centralityChanged = leaderCommunity.update(encounteredNode.leaderCommunity);
        if(centralityChanged){
            proposeLeader(currentTime);
        }
    }

    public void checkCommunities(long currentTime){
        Set<Integer> leaderCommunityNodes = leaderCommunity.getNodes();
//        leaderCommunityNodes.remove(id);
        nodesSeen = new ArrayList<>(leaderCommunityNodes);
        nodesNotSeen = new ArrayList<>();
        for (int nodeId : leaderCommunityNodes){
            ContactInfo info = encounteredNodes.get(nodeId);
            if(info == null)
                continue;

            long lastEncounterTime = info.getLastEncounterTime();
            if(currentTime - lastEncounterTime > TIME_SINCE_LAST_ENCOUNTER_THRESHOLD){
                nodesSeen.remove(new Integer(nodeId));
                nodesNotSeen.add(nodeId);
            }
        }

        //send messages to nodes still in the community to remove the nodes not seen
        for(int nodeSeen : nodesSeen){
            for(int nodeNotSeen: nodesNotSeen){
                CommunityMessage removeRequest = CommunityMessage.CreateRemoveRequest(id, nodeSeen,
                        nodeNotSeen, currentTime);
                addToOwnCommunityMessages(removeRequest);
            }
        }
    }

    @Override
    public void onDataExchange(Node encounteredNode, long contactDuration, long currentTime) {
        CommunityLeaderNode leaderNode = (CommunityLeaderNode) encounteredNode;
        updatesCentralities(leaderNode, currentTime);
        exchangeCommunityMessages(leaderNode, currentTime, encounteredNode.getId());

//        double probabilityOfMeeting = getProbabilityOfMeetingNode(this, encounteredNode.getId(), currentTime);
//        if(probabilityOfMeeting > 0.0){
//            System.out.println(this.id + " will meet " + encounteredNode.getId() + " with a probability of "
//                    + probabilityOfMeeting);
//        }

        super.onDataExchange(encounteredNode, contactDuration, currentTime);
    }

    @Override
    public String getName() {
        return this.getClass().getName();
    }

}
