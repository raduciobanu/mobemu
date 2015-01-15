/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package mobemu.algorithms;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import mobemu.node.*;

/**
 *
 * @author Radu
 */
public class InterestSpace extends Node {

    /**
     * Normalization value for friendship.
     */
    double maxFriendship;
    /**
     * Normalization value for similarity.
     */
    double maxSimilarity;
    /**
     * Normalization value for contacts.
     */
    double maxContacts;
    /**
     * Aggregation weights.
     */
    private static final double w1 = 0.25, w2 = 0.25, w3 = 0.25, w4 = 0.25;
    /**
     * Social network threshold.
     */
    private final double socialNetworkThreshold; // sigcomm: 0.95 // upb: 0.5
    /**
     * Interest threshold.
     */
    private final double interestThreshold; // sigcomm: 0.98 // upb: 0.1
    /**
     * Contacts threshold.
     */
    private final int contactsThreshold; // sigcomm: 30 // upb 50/0
    /**
     * Interest Spaces context.
     */
    Context interestSpaceContext;
    /**
     * Altruism analysis.
     */
    private boolean altruismAnalysis;
    /**
     * Information about all the other nodes in the trace.
     */
    private static Node[] nodes = null;
    /**
     * Algorithm to be used for dissemination.
     */
    Algorithm algorithm;

    /**
     * Instantiates an {@code InterestSpace} object.
     *
     * @param id ID of the node
     * @param context the context of this node
     * @param socialNetwork the social network as seen by this node
     * @param dataMemorySize the maximum allowed size of the data memory
     * @param exchangeHistorySize the maximum allowed size of the exchange
     * history
     * @param seed the seed for the random number generators if routing is used
     * @param traceStart timestamp of the start of the trace
     * @param traceEnd timestamp of the end of the trace
     * @param altruism {@code true} if altruism computations are performed, {@code false}
     * otherwise
     * @param nodes array of all the nodes in the network
     */
    public InterestSpace(int id, Context context, boolean[] socialNetwork, int dataMemorySize, int exchangeHistorySize,
            long seed, long traceStart, long traceEnd, boolean altruism, Node[] nodes, double socialNetworkThreshold,
            double interestThreshold, int contactsThreshold, InterestSpaceAlgorithm algorithm) {
        super(id, nodes.length, context, socialNetwork, dataMemorySize, exchangeHistorySize, seed, traceStart, traceEnd);

        this.altruismAnalysis = altruism;
        this.maxFriendship = Double.MIN_VALUE;
        this.maxSimilarity = Double.MIN_VALUE;
        this.maxContacts = Double.MIN_VALUE;
        this.socialNetworkThreshold = socialNetworkThreshold;
        this.interestThreshold = interestThreshold;
        this.contactsThreshold = contactsThreshold;
        this.interestSpaceContext = new Context(context);

        if (InterestSpace.nodes == null) {
            InterestSpace.nodes = nodes;
        }

        switch (algorithm) {
            case ONSIDE: {
                this.algorithm = new ONSIDEAlgorithm();
                break;
            }
            default: {
                this.algorithm = null;
            }
        }
    }

    @Override
    protected void preDataExchange(Node encounteredNode, long currentTime) {
        if (!(encounteredNode instanceof InterestSpace)) {
            return;
        }

        InterestSpace interestSpaceEncounteredNode = (InterestSpace) encounteredNode;

        int contacts = 0, encounteredContacts = 0;
        for (int i = 0; i < encounters.length; i++) {
            contacts += encounters[i];
        }
        for (int i = 0; i < interestSpaceEncounteredNode.encounters.length; i++) {
            encounteredContacts += interestSpaceEncounteredNode.encounters[i];
        }

        // compute aggregation weights
        double aggregationWeightNode = computeAggregationWeight(interestSpaceEncounteredNode, contacts, currentTime);
        double aggregationWeightEncountered = interestSpaceEncounteredNode.computeAggregationWeight(this, encounteredContacts, currentTime);

        // aggregate contacts
        HashMap<Integer, ContactInfo> nodeNewMap = aggregateContacts(interestSpaceEncounteredNode, aggregationWeightNode);
        HashMap<Integer, ContactInfo> encounteredNewMap = interestSpaceEncounteredNode.aggregateContacts(this, aggregationWeightEncountered);
        this.encounteredNodes = nodeNewMap;
        interestSpaceEncounteredNode.encounteredNodes = encounteredNewMap;

        // aggregate social network
        boolean[] nodeNewSocialNetwork = aggregateSocialNetwork(interestSpaceEncounteredNode, aggregationWeightNode);
        boolean[] encounteredNewSocialNetwork = interestSpaceEncounteredNode.aggregateSocialNetwork(this, aggregationWeightEncountered);
        socialNetwork = nodeNewSocialNetwork;
        interestSpaceEncounteredNode.socialNetwork = encounteredNewSocialNetwork;

        // aggregate interests
        Context nodeNewContext = aggregateInterests(interestSpaceEncounteredNode, aggregationWeightNode);
        Context encounteredNewContext = interestSpaceEncounteredNode.aggregateInterests(this, aggregationWeightEncountered);
        context = nodeNewContext;
        interestSpaceEncounteredNode.context = encounteredNewContext;
    }

    @Override
    protected void onDataExchange(Node encounteredNode, long contactDuration, long currentTime) {
        if (!(encounteredNode instanceof InterestSpace)) {
            return;
        }
        
        if (algorithm != null) {
            algorithm.exchangeData((InterestSpace) encounteredNode, contactDuration, currentTime);
        }
    }

    /**
     * Computes the aggregation weight between two nodes.
     *
     * @param encounteredNode node for which the aggregation is computed
     * @param contacts the number of contacts so far between the two nodes
     * @param currentTime current trace time
     * @return the aggregation weight between the two nodes
     */
    private double computeAggregationWeight(InterestSpace encounteredNode, int contacts, long currentTime) {
        // 1) similarity (number of common neighbors between individuals on social networks)
        double nodeSimilarity = getCommonNeighbors(encounteredNode);
        if (maxSimilarity < nodeSimilarity) {
            maxSimilarity = nodeSimilarity;
        }
        nodeSimilarity = maxSimilarity == 0 ? 0 : nodeSimilarity / maxSimilarity;

        // 2) friendship (common interests)
        double nodeFriendship = interestSpaceContext.getCommonTopics(encounteredNode.interestSpaceContext, currentTime);
        if (maxFriendship < nodeFriendship) {
            maxFriendship = nodeFriendship;
        }
        nodeFriendship = maxFriendship == 0 ? 0 : nodeFriendship / maxFriendship;

        // 3) connectivity (whether nodes are social network friends);
        // don't use k-clique because its results are affected by aggregation
        double nodeConnectivity = socialNetwork[encounteredNode.id] ? 1.0 : 0.0;

        // 4) number of contacts
        double nodeContacts = getContactsNumber(encounteredNode.id);
        if (maxContacts < nodeContacts) {
            maxContacts = nodeContacts;
        }
        nodeContacts = maxContacts == 0 ? 0 : nodeContacts / maxContacts;

        return contacts > contactsThreshold ? w1 * nodeSimilarity + w2 * nodeFriendship + w3 * nodeConnectivity + w4 * nodeContacts : 0;
    }

    /**
     * Aggregates the number of contacts at a given node, when it comes into
     * contact with another node.
     *
     * @param encounteredNode encountered node
     * @param weight aggregation weight
     *
     * @return hash map of new contact info for node
     */
    private HashMap<Integer, ContactInfo> aggregateContacts(InterestSpace encounteredNode, double weight) {
        HashMap<Integer, ContactInfo> result = new HashMap<>();

        Iterator it = encounteredNodes.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<Integer, ContactInfo> pairs = (Map.Entry) it.next();

            ContactInfo info = pairs.getValue();
            ContactInfo encounteredInfo = encounteredNode.encounteredNodes.get(pairs.getKey());

            if (encounteredInfo != null) {
                int newDuration = (int) Math.max(info.getDuration(), weight * encounteredInfo.getDuration());
                int newContacts = (int) Math.max(info.getContacts(), weight * encounteredInfo.getContacts());

                ContactInfo newInfo = new ContactInfo(newDuration, newContacts);
                result.put(pairs.getKey(), newInfo);
            } else {
                result.put(pairs.getKey(), info);
            }
        }

        it = encounteredNode.encounteredNodes.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<Integer, ContactInfo> pairs = (Map.Entry) it.next();

            ContactInfo encounteredInfo = pairs.getValue();
            ContactInfo info = encounteredNodes.get(pairs.getKey());

            if (info == null) {
                ContactInfo newInfo = new ContactInfo((int) (weight * encounteredInfo.getDuration()), (int) (weight * encounteredInfo.getContacts()));
                result.put(pairs.getKey(), newInfo);
            }
        }

        return result;
    }

    /**
     * Aggregates the social network of a given node, when it comes into contact
     * with another node.
     *
     * @param encounteredNode encountered node
     * @param weight aggregation weight
     *
     * @return new social network array
     */
    private boolean[] aggregateSocialNetwork(InterestSpace encounteredNode, double weight) {
        boolean[] result = new boolean[socialNetwork.length];

        for (int i = 0; i < result.length; i++) {
            if (socialNetwork[i] || weight * (encounteredNode.socialNetwork[i] ? 1.0 : 0.0) > socialNetworkThreshold) {
                result[i] = true;
            } else {
                result[i] = false;
            }
        }

        return result;
    }

    /**
     * Aggregates the interests of a given node, when it comes into contact with
     * another node.
     *
     * @param encounteredNode encountered node
     * @param weight aggregation weight
     *
     * @return aggregated interests
     */
    private Context aggregateInterests(InterestSpace encounteredNode, double weight) {
        Context result = new Context(id);
        result.addTopicSet(interestSpaceContext.getTopics());

        if (weight > interestThreshold) {
            result.addTopicSet(encounteredNode.interestSpaceContext.getTopics());
        }

        return result;
    }

    /**
     * Type of dissemination algorithm to be used with Interest Space.
     */
    public enum InterestSpaceAlgorithm {

        ONSIDE
    }

    /**
     * Interface for a dissemination algorithm to be used with Interest Space.
     */
    private interface Algorithm {

        void exchangeData(InterestSpace encounteredNode, long contactDuration, long currentTime);
    }

    /**
     * Class for ONSIDE algorithm in Interest Space.
     */
    private class ONSIDEAlgorithm implements Algorithm {

        /**
         * Threshold for encountered interests.
         */
        private double encounteredInterestsThreshold;
        /**
         * Threshold for interested friends.
         */
        private int interestedFriendsThreshold;
        /**
         * Common interests required between two nodes, in order for them to
         * exchange data.
         */
        private int commonInterests;

        /**
         * Instantiates an {@code ONSIDEAlgorithm} object.
         *
         * @param interestedFriendsThreshold threshold for interested friends
         * @param encounteredInterestsThreshold threshold for encountered
         * interests
         * @param commonInterests common interests required between two nodes,
         * in order for them to exchange data
         */
        public ONSIDEAlgorithm(int interestedFriendsThreshold,
                double encounteredInterestsThreshold, int commonInterests) {
            this.interestedFriendsThreshold = interestedFriendsThreshold;
            this.encounteredInterestsThreshold = encounteredInterestsThreshold;
            this.commonInterests = commonInterests;
        }

        /**
         * Instantiates an {@code ONSIDEAlgorithm} object with default parameter
         * values.
         */
        public ONSIDEAlgorithm() {
            this(1, 1.0, 1);
        }

        @Override
        public void exchangeData(InterestSpace encounteredNode, long contactDuration, long currentTime) {
            int remainingMessages = deliverDirectMessages(encounteredNode, altruismAnalysis, contactDuration, currentTime, true);
            int totalMessages = 0;
            
            for (Message message : encounteredNode.dataMemory) {
                if (totalMessages >= remainingMessages) {
                    return;
                }

                if (!dataMemory.contains(message) && !ownMessages.contains(message)
                        && interestSpaceContext.getCommonTopics(encounteredNode.interestSpaceContext, currentTime) >= commonInterests
                        && (interestSpaceContext.getCommonTopics(message.getTags(), currentTime) > 0
                        || haveFriendsInterestedInContext(message.getTags(), interestedFriendsThreshold, currentTime)
                        || willEncounterContext(message.getTags(), currentTime))) {

                    if (altruismAnalysis) {
                        if (!encounteredNode.altruism.isSelfish() && !checkAltruism(encounteredNode, message)) {
                            altruism.setSelfishness(true);

                            if (interestSpaceContext.getCommonTopics(message.getTags(), currentTime) > 0) {
                                altruism.increaseLocal();
                            } else {
                                altruism.increaseGlobal();
                            }

                            continue;
                        } else if (!encounteredNode.altruism.isSelfish()) {
                            altruism.setSelfishness(true);
                        }
                    }

                    insertMessage(message, encounteredNode, currentTime, altruismAnalysis, true);
                    totalMessages++;
                }
            }

            for (Message message : encounteredNode.ownMessages) {
                if (totalMessages >= remainingMessages) {
                    return;
                }

                if (!dataMemory.contains(message) && !ownMessages.contains(message)
                        && interestSpaceContext.getCommonTopics(encounteredNode.interestSpaceContext, currentTime) >= commonInterests
                        && (interestSpaceContext.getCommonTopics(message.getTags(), currentTime) > 0
                        || haveFriendsInterestedInContext(message.getTags(), interestedFriendsThreshold, currentTime)
                        || willEncounterContext(message.getTags(), currentTime))) {

                    if (altruismAnalysis) {
                        if (!encounteredNode.altruism.isSelfish() && !checkAltruism(encounteredNode, message)) {
                            altruism.setSelfishness(true);

                            if (interestSpaceContext.getCommonTopics(message.getTags(), currentTime) > 0) {
                                altruism.increaseLocal();
                            } else {
                                altruism.increaseGlobal();
                            }

                            continue;
                        } else if (!encounteredNode.altruism.isSelfish()) {
                            altruism.setSelfishness(true);
                        }
                    }

                    insertMessage(message, encounteredNode, currentTime, altruismAnalysis, true);
                    totalMessages++;
                }
            }
        }

        /**
         * Checks the altruism of this node towards a message, from the
         * standpoint of an encountered node.
         *
         * @param encounteredNode the encountered node
         * @param message message to be analyzed
         * @return {@code true} if the message is to be transferred, {@code false}
         * otherwise
         */
        private boolean checkAltruism(InterestSpace encounteredNode, Message message) {
            double perceivedAltruism = 0.0;
            double total = 0.0;

            for (ExchangeHistory sent : encounteredNode.exchangeHistorySent) {
                for (ExchangeHistory received : encounteredNode.exchangeHistoryReceived) {
                    if (sent.getNodeSeen() == id && message.getTags().equals(sent.getMessage().getTags())
                            && ((sent.getMessage() == received.getMessage() && received.getExchangeTime() > sent.getExchangeTime())
                            || sent.getBattery() <= Altruism.getMaxBatteryThreshold() * Battery.getMaxLevel())) {
                        perceivedAltruism++;
                        break;
                    }
                }

                if (sent.getNodeSeen() == id && message.getTags().equals(sent.getMessage().getTags())) {
                    total++;
                }
            }

            perceivedAltruism /= total;

            if (total == 0.0 || perceivedAltruism >= Altruism.getTrustThreshold()) {
                return true;
            } else {
                return false;
            }
        }

        /**
         * Checks whether this node is likely to encounter a node with given
         * tags, based on the history of encountered interests
         *
         * @param tags tags to check against
         * @return {@code true} if the given node is likely to encounter the set
         * of tags, {@code false} otherwise
         */
        private boolean willEncounterContext(Context tags, long tick) {
            double commonInterestsPercentage = 0.0;
            double total = 0.0;

            Iterator it = encounteredNodes.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry<Integer, ContactInfo> pairs = (Map.Entry) it.next();
                ContactInfo info = pairs.getValue();
                int nodeId = pairs.getKey();

                commonInterestsPercentage += info.getContacts() * (nodes[nodeId].getContext().getCommonTopics(tags, tick) > 0 ? 1 : 0);
                total += info.getContacts();
            }
            commonInterestsPercentage /= total;

            if (commonInterestsPercentage > encounteredInterestsThreshold) {
                return true;
            }

            return false;
        }

        /**
         * Checks whether a node has social network friends interested in a set
         * of tags.
         *
         * @param tags set of tags the node's friends should be interested in
         * @param noFriends number of friends that are interested in this set of
         * tags
         * @return {@code true} if the node given as parameter has the specified
         * number of friends interested in the given tags, {@code false}
         * otherwise
         */
        private boolean haveFriendsInterestedInContext(Context tags, int noFriends, long tick) {
            int count = 0;

            for (int i = 0; i < socialNetwork.length; i++) {
                if (socialNetwork[i] && nodes[i].getContext().getCommonTopics(tags, tick) > 0) {
                    count++;

                    if (count >= noFriends) {
                        return true;
                    }
                }
            }

            return false;
        }
    }
}
