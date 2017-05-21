package mobemu.node.consensus;

import mobemu.algorithms.SPRINT;
import mobemu.node.Context;
import mobemu.node.Message;
import mobemu.node.Node;
import mobemu.node.leader.LeaderNode;
import mobemu.utils.message.MessageList;

import java.util.HashMap;
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

    /**
     * Dictionary of decision requests made (MessageId, LeaderId)
     * Used because the leader might change several times before the end of the trace
     * And we are interested if the request has been answered by the leader which the request was issued for
     */
    protected Map<Integer, Integer> decisionRequests;

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

        decisionRequests = new HashMap<>();
    }

    public LeaderNode getLeaderNode() {
        return leaderNode;
    }

    public int getLeaderNodeId() {
        return leaderNode.getLeaderNodeId();
    }

    @Override
    public Message generateMessage(Message message) {
        decisionRequests.put(message.getId(), message.getDestination());

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

        exchangeDecisions(encounteredNode);
    }

    @Override
    protected void downloadMessage(Message message, Node encounteredNode, boolean dissemination, long currentTime) {
        super.downloadMessage(message, encounteredNode, dissemination, currentTime);

        computeDecisionForMessage(message, currentTime);
    }

    protected void computeDecisionForMessage(Message message, long currentTime) {
        messageDictionary.add(message);

        ConsensusDecision decision = messageDictionary.getDecision(message.getId(), this.getId(), currentTime);

        addDecision(decision);
    }

    /**
     * Add a decision to the list of own decisions
     *
     * @param decision
     */
    protected void addDecision(ConsensusDecision decision) {
        ownDecisions.add(decision);
    }

    protected void exchangeDecisions(Node encounteredNode) {
        if (!(encounteredNode instanceof ConsensusLeaderNode))
            return;

        ConsensusLeaderNode encounteredConsensusNode = (ConsensusLeaderNode) encounteredNode;

        getDecisionsFromList(encounteredConsensusNode.ownDecisions);
        getDecisionsFromList(encounteredConsensusNode.receivedDecisions);
    }

    /**
     * Add only decisions generated by the leader of the current node
     *
     * @param decisions
     */
    private void getDecisionsFromList(MessageList<ConsensusDecision> decisions) {
        for (ConsensusDecision decision : decisions) {
            receivedDecisions.add(decision);
        }
    }

    public String getDecisionValueForMessageId(int messageId) {
        for (ConsensusDecision decision : receivedDecisions) {
            if (decision.match(messageId, decisionRequests.get(messageId))) {
                return decision.getValue();
            }
        }

        for (ConsensusDecision decision : ownDecisions) {
            if (decision.match(messageId, decisionRequests.get(messageId))) {
                return decision.getValue();
            }
        }

        return "";
    }
}
