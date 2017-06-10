package mobemu.node.consensus;

import mobemu.algorithms.SPRINT;
import mobemu.node.Altruism;
import mobemu.node.Context;
import mobemu.node.Message;
import mobemu.node.Node;
import mobemu.node.consensus.Malevolence.Malevolence;
import mobemu.node.leader.LeaderNode;
import mobemu.node.leader.communityBasedLeaderElection.CommunityLeaderNode;
import mobemu.utils.message.MessageList;

import java.util.*;

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

    protected Malevolence malevolence;

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
        this.messageDictionary = new ConsensusMessageDictionary(this);
        this.receivedDecisions = new DecisionMessageList();
        this.ownDecisions = new DecisionMessageList();

//        requests = new HashMap<>();
        decisionRequestsAndResponses = new DecisionRequestsAndResponses();
        leaderNode.setConsensusLeaderNode(this);
        malevolence = new Malevolence(this.id);
    }

    public Malevolence getMalevolence(){
        return this.malevolence;
    }

    public Altruism getAltruism(){
        return this.altruism;
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

        exchangeOpinions(encounteredNode);

        leaderNode.onDataExchange(encounteredLeaderNode, contactDuration, currentTime);

        exchangeDecisions(encounteredNode, currentTime);

//        if(getId() == 17){
//            System.out.println("Node 17 meet node " + encounteredLeaderNode.getId() + ", with received decisions " +
//                    receivedDecisions.size() + " and own decisions " + ownDecisions.size());
//        }
    }

    @Override
    protected void downloadMessage(Message message, Node encounteredNode, boolean dissemination, long currentTime) {
        boolean messageDelivered = message.isDelivered(id);
        super.downloadMessage(message, encounteredNode, dissemination, currentTime);

        if (!messageDelivered) {
            computeDecisionForMessage(message, currentTime);
        }
    }

    protected void computeDecisionForMessage(Message message, long currentTime) {
        messageDictionary.add(message);
        ConsensusDecision decision = messageDictionary.getDecision(message.getId(), this.getId(), currentTime);
//        printCommunity();
        if(decision != null){
            addDecision(decision, currentTime);
        }
    }

    private void printCommunity(){
        if(!(leaderNode instanceof CommunityLeaderNode))
            return;

        CommunityLeaderNode communityLeaderNode = (CommunityLeaderNode)leaderNode;
        System.out.println(leaderNode.getId() + "->" + communityLeaderNode.getLeaderCommunityNodes());
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

    protected void exchangeOpinions(Node encounteredNode){
        if (!(encounteredNode instanceof ConsensusLeaderNode))
            return;

        ConsensusLeaderNode encounteredConsensusNode = (ConsensusLeaderNode) encounteredNode;
        this.malevolence.exchange(encounteredConsensusNode.getMalevolence());
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
        DecisionResponse decisionForMessage = decisionRequestsAndResponses.getResponseForMessageId(messageId);
        if(decisionForMessage != null){
            return decisionForMessage;
        }

//        return null;
        List<ConsensusDecision> allDecisionsByMessageId = receivedDecisions.getByMessageId(messageId);
        allDecisionsByMessageId.addAll(ownDecisions.getByMessageId(messageId));

        if(allDecisionsByMessageId.size() == 0){
//            System.out.println("Node " + getId() + ", leaderId: " + leaderNode.getId() + ", no decision received for:" +
//                    " " + messageId + "decisions size: " + receivedDecisions.size());
        }

        return getAverageDecision(allDecisionsByMessageId);
    }

    public DecisionResponse getAverageDecision(List<ConsensusDecision> decisionsByMessageId){
        //Dictionary of values and their number of appearances
        Map<String, Double> values = new HashMap<>();
        Map<String, Integer> appearances = new HashMap<>();
        for(ConsensusDecision decision : decisionsByMessageId){
            String value = decision.getValue();
            if(!values.containsKey(value)){
                values.put(value, decision.getConfidenceLevel());
                appearances.put(value, 1);
                continue;
            }

            int currentNumberOfAppearances = appearances.get(value);
            appearances.put(value, currentNumberOfAppearances + 1);
            double currentConfidenceLevel = values.get(value);
            values.put(value, currentConfidenceLevel + decision.getConfidenceLevel());

        }

        //compute average confidence level
        for(Map.Entry<String, Double> value: values.entrySet()){
            value.setValue(value.getValue() / appearances.get(value.getKey()));
        }

        String mostFrequentValue = "";
        double maxConfidenceLevel = 0;
        boolean multipleMostFrequentValues = false;
        for(Map.Entry<String, Double> item: values.entrySet()){
            if(item.getValue() == maxConfidenceLevel){
                multipleMostFrequentValues=true;
                continue;
            }

            if(item.getValue() > maxConfidenceLevel){
                maxConfidenceLevel = item.getValue();
                mostFrequentValue = item.getKey();
                multipleMostFrequentValues = false;
            }
        }

        if(multipleMostFrequentValues){
//            System.out.println("MultipleValues all: " + values.size());

            return null;
        }

        if(mostFrequentValue.equals("")){
            return null;
//            System.out.println("Empty Value, decisionsForMessage size: " + decisionsByMessageId.size());
        }

        return new DecisionResponse(-1, mostFrequentValue, 0, 0);
    }

    public void changeLeader(int leaderId, long currentTime){
        List<Message> unrepliedRequests = decisionRequestsAndResponses.updateUnrepliedRequest(leaderId, currentTime);

        for (Message message: unrepliedRequests){
            super.generateMessage(message);
        }
    }
}
