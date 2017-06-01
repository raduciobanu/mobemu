package mobemu.node.consensus;

import mobemu.algorithms.SPRINT;
import mobemu.node.Context;
import mobemu.node.Message;
import mobemu.node.Node;
import mobemu.node.leader.LeaderNode;
import mobemu.utils.message.MessageList;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Created by radu on 5/5/2017.
 */
public class ConsensusLeaderNode extends SPRINT {

    /**
     * Dictionary containing the latest versions of the messages
     */
    protected ConsensusMessageDictionary messageDictionary;

    protected LeaderNode leaderNode;

    protected MessageList<ConsensusDecision> receivedDecisions;

    protected MessageList<ConsensusDecision> ownDecisions;

    protected DecisionRequestsAndResponses decisionRequestsAndResponses;

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
    public ConsensusLeaderNode(int id, Context context, boolean[] socialNetwork, int dataMemorySize, int exchangeHistorySize, long seed, long traceStart, long traceEnd, boolean altruism, Node[] nodes, int cacheMemorySize, LeaderNode leaderNode) {
        super(id, context, socialNetwork, dataMemorySize, exchangeHistorySize, seed, traceStart, traceEnd, altruism, nodes, cacheMemorySize);

        this.leaderNode = leaderNode;
        this.messageDictionary = new ConsensusMessageDictionary();

        this.receivedDecisions = new DecisionMessageList();
        this.ownDecisions = new DecisionMessageList();

//        requests = new HashMap<>();
        decisionRequestsAndResponses = new DecisionRequestsAndResponses();
        leaderNode.setConsensusLeaderNode(this);
    }

    public LeaderNode getLeaderNode() {
        return leaderNode;
    }

    public int getLeaderNodeId() {
        return leaderNode.getLeaderNodeId();
    }

    @Override
    public Message generateMessage(Message message) {
        decisionRequestsAndResponses.addRequest(message);

        //if the current node is its own leader
        //add the decision
        if (message.getDestination() == this.id) {
            computeDecisionForMessage(message, message.getTimestamp());
        }

        return super.generateMessage(message);
    }

    @Override
    public String getName() {
        return "ConsensusNode using " + leaderNode.getName();
    }

//    @Override
//    public void onTick(long currentTime, long sampleTime) {
//        leaderNode.onTick(currentTime, sampleTime);
//        super.onTick(currentTime, sampleTime);
//    }

    @Override
    public void run(Node encounteredNode, long tick, long contactDuration, boolean newContact, long timeDelta, long sampleTime) {
        super.run(encounteredNode, tick, contactDuration, newContact, timeDelta, sampleTime);

        ConsensusLeaderNode consensusEncounteredNode = (ConsensusLeaderNode)encounteredNode;
        LeaderNode encounteredLeaderNode = consensusEncounteredNode.getLeaderNode();

        leaderNode.run(encounteredLeaderNode, tick, contactDuration, newContact, timeDelta, sampleTime);
    }

    @Override
    protected void onDataExchange(Node encounteredNode, long contactDuration, long currentTime) {
        if (!(encounteredNode instanceof ConsensusLeaderNode))
            return;

        if (encounteredNode.getId() == this.getId())
            return;

        super.onDataExchange(encounteredNode, contactDuration, currentTime);

        ConsensusLeaderNode consensusLeaderNode = (ConsensusLeaderNode) encounteredNode;
        LeaderNode encounteredLeaderNode = consensusLeaderNode.leaderNode;

        leaderNode.onDataExchange(encounteredLeaderNode, contactDuration, currentTime);

        exchangeDecisions(encounteredNode, currentTime);
    }

    @Override
    protected void downloadMessage(Message message, Node encounteredNode, boolean dissemination, long currentTime) {
        super.downloadMessage(message, encounteredNode, dissemination, currentTime);

        computeDecisionForMessage(message, currentTime);
    }

    protected void computeDecisionForMessage(Message message, long currentTime) {
        messageDictionary.add(message);

        ConsensusDecision decision = messageDictionary.getDecision(message.getId(), this.getId(), currentTime);

        addDecision(decision, currentTime);
    }

    /**
     * Add a decision to the list of own decisions
     *
     * @param decision
     */
    protected void addDecision(ConsensusDecision decision, long currentTime) {

        ownDecisions.add(decision);
        decisionRequestsAndResponses.addResponse(decision, currentTime);
    }

    protected void exchangeDecisions(Node encounteredNode, long currentTime) {
        if (!(encounteredNode instanceof ConsensusLeaderNode))
            return;

        ConsensusLeaderNode encounteredConsensusNode = (ConsensusLeaderNode) encounteredNode;

        getDecisionsFromList(encounteredConsensusNode.ownDecisions, currentTime);
        getDecisionsFromList(encounteredConsensusNode.receivedDecisions, currentTime);
    }

    /**
     * Add only decisions generated by the leader of the current node
     *
     * @param decisions
     */
    private void getDecisionsFromList(MessageList<ConsensusDecision> decisions, long currentTime) {
        for (ConsensusDecision decision : decisions) {
            receivedDecisions.add(decision);
            decisionRequestsAndResponses.addResponse(decision, currentTime);
        }
    }

    public boolean hasRequestForMessageId(int messageId){
        return decisionRequestsAndResponses.requestsContainKey(messageId);
    }

    public DecisionResponse getDecisionValueForMessageId(int messageId) {
        return decisionRequestsAndResponses.getResponseForMessageId(messageId);

//        for (ConsensusDecision decision : receivedDecisions) {
//            if (decision.match(messageId, decisionRequestsAndResponses.getRequestDestination(messageId))) {
//                return decision.getValue();
//            }
//        }
//
//        for (ConsensusDecision decision : ownDecisions) {
//            if (decision.match(messageId, decisionRequestsAndResponses.getRequestDestination(messageId))) {
//                return decision.getValue();
//            }
//        }

//        return "";
    }

    public void changeLeader(int leaderId, long currentTime){
        List<Message> unrepliedRequests = decisionRequestsAndResponses.updateUnrepliedRequest(leaderId, currentTime);

        for (Message message: unrepliedRequests){
            super.generateMessage(message);
        }
    }
}
