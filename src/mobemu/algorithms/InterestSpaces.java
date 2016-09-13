/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package mobemu.algorithms;

import java.util.*;
import mobemu.node.*;

/**
 * Class for an Interest Spaces node.
 *
 * Radu-Ioan Ciobanu, Radu-Corneliu Marin, Ciprian Dobre, Florin Pop. Interest
 * Spaces: A unified interest-based dissemination framework for opportunistic
 * networks. Journal of Systems Architecture, 2016.
 *
 * @author Radu
 */
public class InterestSpaces extends Node {

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
    private double w1, w2, w3, w4;
    /**
     * Social network threshold.
     */
    private final double socialNetworkThreshold;
    /**
     * Interest threshold.
     */
    private final double interestThreshold;
    /**
     * Contacts threshold.
     */
    private final int contactsThreshold;
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
     * List of nodes encountered by the current node during a time window.
     */
    private Map<Integer, ContactInfo> encounteredNodesInterestSpace;
    /**
     * Default value for the time window.
     */
    private long timeWindow;
    /**
     * Current value of the time window for storing contact information.
     */
    private long currentTimeWindow;
    /**
     * Last tick when a contact was registered.
     */
    private long lastTick;

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
     * @param socialNetworkThreshold social network threshold
     * @param interestThreshold interests threshold
     * @param contactsThreshold contacts threshold
     * @param algorithm dissemination algorithm to be used with Interest Spaces
     */
    public InterestSpaces(int id, Context context, boolean[] socialNetwork, int dataMemorySize, int exchangeHistorySize,
            long seed, long traceStart, long traceEnd, boolean altruism, Node[] nodes, double socialNetworkThreshold,
            double interestThreshold, int contactsThreshold, InterestSpacesAlgorithm algorithm) {
        super(id, nodes.length, context, socialNetwork, dataMemorySize, exchangeHistorySize, seed, traceStart, traceEnd);

        this.altruismAnalysis = altruism;
        this.maxFriendship = Double.MIN_VALUE;
        this.maxSimilarity = Double.MIN_VALUE;
        this.maxContacts = Double.MIN_VALUE;
        this.socialNetworkThreshold = socialNetworkThreshold;
        this.interestThreshold = interestThreshold;
        this.contactsThreshold = contactsThreshold;
        this.interestSpaceContext = new Context(context);
        this.encounteredNodesInterestSpace = new HashMap<>();
        this.lastTick = traceStart;

        this.w1 = 0.25;
        this.w2 = 0.25;
        this.w3 = 0.25;
        this.w4 = 0.25;

        this.timeWindow = Long.MAX_VALUE;
        this.currentTimeWindow = timeWindow;

        if (InterestSpaces.nodes == null) {
            InterestSpaces.nodes = nodes;
        }

        switch (algorithm) {
            case ONSIDE: {
                this.algorithm = new ONSIDEAlgorithm();
                break;
            }
            case CacheDecision: {
                this.algorithm = new CacheDecisionAlgorithm(seed);
                break;
            }
            default: {
                this.algorithm = null;
            }
        }
    }

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
     * @param socialNetworkThreshold social network threshold
     * @param interestThreshold interests threshold
     * @param contactsThreshold contacts threshold
     * @param algorithm dissemination algorithm to be used with Interest Spaces
     * @param aggregationW1 aggregation weight for the node similarity component
     * @param aggregationW2 aggregation weight for the node friendship component
     * @param aggregationW3 aggregation weight for the node connectivity
     * component
     * @param aggregationW4 aggregation weight for the node contacts component
     * @param cacheW1 cache weight for the interested nodes ratio component
     * @param cacheW2 cache weight for the interests encountered ratio component
     * @param cacheW3 cache weight for the interested friends ratio component
     * @param timeWindow duration of the time window
     */
    public InterestSpaces(int id, Context context, boolean[] socialNetwork, int dataMemorySize, int exchangeHistorySize,
            long seed, long traceStart, long traceEnd, boolean altruism, Node[] nodes, double socialNetworkThreshold,
            double interestThreshold, int contactsThreshold, InterestSpacesAlgorithm algorithm,
            double aggregationW1, double aggregationW2, double aggregationW3, double aggregationW4,
            double cacheW1, double cacheW2, double cacheW3, long timeWindow) {
        this(id, context, socialNetwork, dataMemorySize, exchangeHistorySize, seed, traceStart, traceEnd, altruism,
                nodes, socialNetworkThreshold, interestThreshold, contactsThreshold, algorithm);

        this.w1 = aggregationW1;
        this.w2 = aggregationW2;
        this.w3 = aggregationW3;
        this.w4 = aggregationW4;

        this.timeWindow = timeWindow;
        this.currentTimeWindow = timeWindow;

        if (this.algorithm instanceof CacheDecisionAlgorithm) {
            ((CacheDecisionAlgorithm) this.algorithm).setCacheWeights(cacheW1, cacheW2, cacheW3);
        }
    }

    @Override
    public String getName() {
        return "Interest Spaces";
    }

    @Override
    protected void preDataExchange(Node encounteredNode, long currentTime) {
        if (!(encounteredNode instanceof InterestSpaces)) {
            return;
        }

        InterestSpaces interestSpaceEncounteredNode = (InterestSpaces) encounteredNode;

        if (algorithm != null) {
            algorithm.preExchangeData(interestSpaceEncounteredNode, currentTime);
            interestSpaceEncounteredNode.algorithm.preExchangeData(this, currentTime);
        }

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

        // aggregate Interest Spaces contacts
        nodeNewMap = aggregateInterestSpaceContacts(interestSpaceEncounteredNode, aggregationWeightNode);
        encounteredNewMap = interestSpaceEncounteredNode.aggregateInterestSpaceContacts(this, aggregationWeightEncountered);
        this.encounteredNodesInterestSpace = nodeNewMap;
        interestSpaceEncounteredNode.encounteredNodesInterestSpace = encounteredNewMap;

        // aggregate social network
        boolean[] nodeNewSocialNetwork = aggregateSocialNetwork(interestSpaceEncounteredNode, aggregationWeightNode);
        boolean[] encounteredNewSocialNetwork = interestSpaceEncounteredNode.aggregateSocialNetwork(this, aggregationWeightEncountered);
        socialNetwork = nodeNewSocialNetwork;
        interestSpaceEncounteredNode.socialNetwork = encounteredNewSocialNetwork;

        // aggregate interests
        Context nodeNewContext = aggregateInterests(interestSpaceEncounteredNode, aggregationWeightNode);
        Context encounteredNewContext = interestSpaceEncounteredNode.aggregateInterests(this, aggregationWeightEncountered);
        interestSpaceContext = nodeNewContext;
        interestSpaceEncounteredNode.interestSpaceContext = encounteredNewContext;
    }

    @Override
    protected void onDataExchange(Node encounteredNode, long contactDuration, long currentTime) {
        if (!(encounteredNode instanceof InterestSpaces)) {
            return;
        }

        if (algorithm != null) {
            algorithm.exchangeData((InterestSpaces) encounteredNode, contactDuration, currentTime);
        }
    }

    @Override
    public void updateContactDuration(int id, long sampleTime, long currentTime) {
        super.updateContactDuration(id, sampleTime, currentTime);

        if (updateTimeWindow(currentTime)) {
            encounteredNodesInterestSpace.clear();
        }

        ContactInfo info = encounteredNodesInterestSpace.get(id);
        if (info != null) {
            info.increaseDuration(sampleTime);
        } else {
            encounteredNodesInterestSpace.put(id, new ContactInfo(currentTime));
        }
    }

    @Override
    public void updateContactsNumber(int id, long currentTime) {
        super.updateContactsNumber(id, currentTime);

        if (updateTimeWindow(currentTime)) {
            encounteredNodesInterestSpace.clear();
        }

        ContactInfo info = encounteredNodesInterestSpace.get(id);
        if (info != null) {
            info.increaseContacts();
            info.setLastEncounterTime(currentTime);
        } else {
            encounteredNodesInterestSpace.put(id, new ContactInfo(currentTime));
        }
    }

    /**
     * Updates the time window based on the current time.
     *
     * @param currentTime current trace time
     * @return {@code true} if the time window was reset, {@code false}
     * otherwise
     */
    private boolean updateTimeWindow(long currentTime) {
        boolean result = false;

        currentTimeWindow -= currentTime - lastTick;
        if (currentTimeWindow < 0) {
            currentTimeWindow = timeWindow;
            result = true;
        }

        lastTick = currentTime;

        return result;
    }

    /**
     * Computes the aggregation weight between two nodes.
     *
     * @param encounteredNode node for which the aggregation is computed
     * @param contacts the number of contacts so far between the two nodes
     * @param currentTime current trace time
     * @return the aggregation weight between the two nodes
     */
    private double computeAggregationWeight(InterestSpaces encounteredNode, int contacts, long currentTime) {
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
    private HashMap<Integer, ContactInfo> aggregateContacts(InterestSpaces encounteredNode, double weight) {
        HashMap<Integer, ContactInfo> result = new HashMap<>();

        Iterator it = encounteredNodes.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<Integer, ContactInfo> pairs = (Map.Entry) it.next();

            ContactInfo info = pairs.getValue();
            ContactInfo encounteredInfo = encounteredNode.encounteredNodes.get(pairs.getKey());

            if (encounteredInfo != null) {
                long newDuration = (long) Math.max(info.getDuration(), weight * encounteredInfo.getDuration());
                int newContacts = (int) Math.max(info.getContacts(), weight * encounteredInfo.getContacts());
                long newLastEncounterTime = (long) Math.max(info.getLastEncounterTime(),
                        weight * encounteredInfo.getLastEncounterTime());

                ContactInfo newInfo = new ContactInfo(newDuration, newContacts, newLastEncounterTime);
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
                ContactInfo newInfo = new ContactInfo((long) (weight * encounteredInfo.getDuration()),
                        (int) (weight * encounteredInfo.getContacts()), (long) (weight * encounteredInfo.getLastEncounterTime()));
                result.put(pairs.getKey(), newInfo);
            }
        }

        return result;
    }

    /**
     * Aggregates the number of Interest Spaces contacts at a given node, when
     * it comes into contact with another node.
     *
     * @param encounteredNode encountered node
     * @param weight aggregation weight
     *
     * @return hash map of new contact info for node
     */
    private HashMap<Integer, ContactInfo> aggregateInterestSpaceContacts(InterestSpaces encounteredNode, double weight) {
        HashMap<Integer, ContactInfo> result = new HashMap<>();

        Iterator it = encounteredNodesInterestSpace.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<Integer, ContactInfo> pairs = (Map.Entry) it.next();

            ContactInfo info = pairs.getValue();
            ContactInfo encounteredInfo = encounteredNode.encounteredNodesInterestSpace.get(pairs.getKey());

            if (encounteredInfo != null) {
                long newDuration = (long) Math.max(info.getDuration(), weight * encounteredInfo.getDuration());
                int newContacts = (int) Math.max(info.getContacts(), weight * encounteredInfo.getContacts());
                long newLastEncounterTime = (long) Math.max(info.getLastEncounterTime(),
                        weight * encounteredInfo.getLastEncounterTime());

                ContactInfo newInfo = new ContactInfo(newDuration, newContacts, newLastEncounterTime);
                result.put(pairs.getKey(), newInfo);
            } else {
                result.put(pairs.getKey(), info);
            }
        }

        it = encounteredNode.encounteredNodesInterestSpace.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<Integer, ContactInfo> pairs = (Map.Entry) it.next();

            ContactInfo encounteredInfo = pairs.getValue();
            ContactInfo info = encounteredNodesInterestSpace.get(pairs.getKey());

            if (info == null) {
                ContactInfo newInfo = new ContactInfo((long) (weight * encounteredInfo.getDuration()),
                        (int) (weight * encounteredInfo.getContacts()), (long) (weight * encounteredInfo.getLastEncounterTime()));
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
    private boolean[] aggregateSocialNetwork(InterestSpaces encounteredNode, double weight) {
        boolean[] result = new boolean[socialNetwork.length];

        for (int i = 0; i < result.length; i++) {
            result[i] = socialNetwork[i] || weight * (encounteredNode.socialNetwork[i] ? 1.0 : 0.0) > socialNetworkThreshold;
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
    private Context aggregateInterests(InterestSpaces encounteredNode, double weight) {
        Context result = new Context(id);
        result.addTopicSet(interestSpaceContext.getTopics());

        if (weight > interestThreshold) {
            result.addTopicSet(encounteredNode.interestSpaceContext.getTopics());
        }

        return result;
    }

    /**
     * Type of dissemination algorithm to be used with Interest Spaces.
     */
    public enum InterestSpacesAlgorithm {

        ONSIDE, CacheDecision
    }

    /**
     * Interface for a dissemination algorithm to be used with Interest Spaces.
     */
    private abstract class Algorithm {

        abstract void exchangeData(InterestSpaces encounteredNode, long contactDuration, long currentTime);

        abstract void preExchangeData(InterestSpaces encounteredNode, long currentTime);

        /**
         * Checks the altruism of this node towards a message, from the
         * standpoint of an encountered node.
         *
         * @param encounteredNode the encountered node
         * @param message message to be analyzed
         * @return {@code true} if the message is to be transferred, {@code false}
         * otherwise
         */
        protected boolean checkAltruism(InterestSpaces encounteredNode, Message message) {
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

            return total == 0.0 || perceivedAltruism >= Altruism.getTrustThreshold();
        }
    }

    /**
     * Class for the Interest Spaces cache selection algorithm.
     */
    private class CacheDecisionAlgorithm extends Algorithm {

        /**
         * Probabilities that this node is a cache for a tag.
         */
        private Map<Integer, Double> cacheProbabilities;
        /**
         * Random number generator for cache decisions.
         */
        private Random cacheDecisionRandom;
        /**
         * Automatically download messages with tags that have a caching
         * probability higher than this threshold.
         */
        private static final double downloadThreshold = 0.8;
        /**
         * Cache function weights.
         */
        private double cacheW1, cacheW2, cacheW3;

        /**
         * Instantiates a {@code CacheDecisionAlgorithm} object.
         *
         * @param seed random number generator seed
         */
        public CacheDecisionAlgorithm(long seed) {
            this(seed, 0.33, 0.33, 0.33);
        }

        /**
         * Instantiates a {@code CacheDecisionAlgorithm} object.
         *
         * @param seed random number generator seed
         * @param w1 weight for the interested nodes ratio component
         * @param w2 weight for the interests encountered ratio component
         * @param w3 weight for the interested friends ratio component
         */
        public CacheDecisionAlgorithm(long seed, double w1, double w2, double w3) {
            cacheProbabilities = new HashMap<>();
            cacheDecisionRandom = new Random(seed);
            cacheW1 = w1;
            cacheW2 = w2;
            cacheW3 = w3;
        }

        /**
         * Sets the cache weights.
         *
         * @param w1 weight for the interested nodes ratio component
         * @param w2 weight for the interests encountered ratio component
         * @param w3 weight for the interested friends ratio component
         */
        public void setCacheWeights(double w1, double w2, double w3) {
            this.cacheW1 = w1;
            this.cacheW2 = w2;
            this.cacheW3 = w3;
        }

        /**
         * Verifies whether a message should be downloaded by the current node,
         * based on the caching probabilities.
         *
         * @param message message to be verified
         * @return {@code true} if the message should be downloaded, {@code false}
         * otherwise
         */
        private boolean shouldDownload(InterestSpaces encounteredNode, Message message) {
            if (!(encounteredNode.algorithm instanceof CacheDecisionAlgorithm)) {
                return false;
            }

            double value = cacheDecisionRandom.nextDouble();
            double maxCacheProbability = Double.MIN_VALUE;
            double maxEncounterProbability = Double.MIN_VALUE;
            Map<Integer, Double> encounteredCacheProbabilities =
                    ((CacheDecisionAlgorithm) encounteredNode.algorithm).cacheProbabilities;

            for (Topic topic : message.getTags().getTopics()) {
                Double probability = cacheProbabilities.get(topic.getTopic());
                if (probability == null) {
                    continue;
                }

                if (probability > maxCacheProbability) {
                    maxCacheProbability = probability;
                }

                probability = encounteredCacheProbabilities.get(topic.getTopic());
                if (probability == null) {
                    continue;
                }

                if (probability > maxEncounterProbability) {
                    maxEncounterProbability = probability;
                }
            }

            return value <= maxCacheProbability || maxCacheProbability >= downloadThreshold
                    || (maxEncounterProbability != Long.MIN_VALUE && (maxCacheProbability - maxEncounterProbability > 0.5));
        }

        @Override
        public void exchangeData(InterestSpaces encounteredNode, long contactDuration, long currentTime) {
            int remainingMessages = deliverDirectMessages(encounteredNode, altruismAnalysis, contactDuration, currentTime, true);
            int totalMessages = 0;

            for (Message message : encounteredNode.dataMemory) {
                if (totalMessages >= remainingMessages) {
                    return;
                }

                if (!dataMemory.contains(message) && !ownMessages.contains(message) && shouldDownload(encounteredNode, message)) {

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
                            altruism.setSelfishness(false);
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

                if (!dataMemory.contains(message) && !ownMessages.contains(message) && shouldDownload(encounteredNode, message)) {

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
                            altruism.setSelfishness(false);
                        }
                    }

                    insertMessage(message, encounteredNode, currentTime, altruismAnalysis, true);
                    totalMessages++;
                }
            }
        }

        @Override
        public void preExchangeData(InterestSpaces encounteredNode, long currentTime) {
            Map<Integer, Integer> interestsEncountered = new HashMap<>();
            int totalInterestsEncountered = 0;

            Map<Integer, Integer> nodesInterested = new HashMap<>(); // topic ID, count, divide by encountered nodes size
            Map<Integer, Integer> contactsWithNodesInterested = new HashMap<>();
            int totalContacts = 0;

            Map<Integer, Integer> friendsInterested = new HashMap<>();
            Set<Integer> totalFriendsInterested = new HashSet<>(); // which of my friends have I encountered

            Iterator it = encounteredNodesInterestSpace.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry<Integer, ContactInfo> pairs = (Map.Entry) it.next();

                if (!(nodes[pairs.getKey()] instanceof InterestSpaces)) {
                    continue;
                }

                InterestSpaces seenNode = (InterestSpaces) nodes[pairs.getKey()];
                ContactInfo nodeInfo = pairs.getValue();

                totalContacts += nodeInfo.getContacts();

                for (Topic topic : seenNode.interestSpaceContext.getTopics()) {
                    int topicId = topic.getTopic();

                    Integer currentNodesInterested = nodesInterested.get(topicId);
                    if (currentNodesInterested == null) {
                        nodesInterested.put(topicId, 1);
                    } else {
                        nodesInterested.put(topicId, currentNodesInterested + 1);
                    }

                    Integer currentContactsWithNodesInterested = contactsWithNodesInterested.get(topicId);
                    if (currentContactsWithNodesInterested == null) {
                        contactsWithNodesInterested.put(topicId, nodeInfo.getContacts());
                    } else {
                        contactsWithNodesInterested.put(topicId, currentContactsWithNodesInterested + nodeInfo.getContacts());
                    }

                    Integer currentInterestsEncountered = interestsEncountered.get(topicId);
                    if (currentInterestsEncountered == null) {
                        interestsEncountered.put(topicId, 1);
                    } else {
                        interestsEncountered.put(topicId, currentInterestsEncountered + 1);
                    }
                    totalInterestsEncountered++;
                }

                if (socialNetwork[seenNode.id]) {
                    totalFriendsInterested.add(seenNode.id);

                    for (Topic topic : seenNode.interestSpaceContext.getTopics()) {
                        int topicId = topic.getTopic();
                        Integer currentFriendsInterested = friendsInterested.get(topicId);
                        if (currentFriendsInterested == null) {
                            friendsInterested.put(topicId, 1);
                        } else {
                            friendsInterested.put(topicId, currentFriendsInterested + 1);
                        }
                    }
                }
            }

            it = nodesInterested.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry<Integer, Integer> pairs = (Map.Entry) it.next();
                int key = pairs.getKey();

                double interestedNodesRatio, interestsEncounteredRatio, interestedFriendsRatio;

                if (encounteredNodesInterestSpace.isEmpty() || totalContacts == 0) {
                    interestedNodesRatio = 0;
                } else {
                    interestedNodesRatio = (double) (nodesInterested.get(key) * contactsWithNodesInterested.get(key))
                            / (encounteredNodesInterestSpace.size() * totalContacts);
                }

                if (totalInterestsEncountered == 0) {
                    interestsEncounteredRatio = 0;
                } else {
                    interestsEncounteredRatio = (double) interestsEncountered.get(key) / totalInterestsEncountered;
                    if (interestsEncounteredRatio >= (double) 1 / Context.getMaxTopicsPerNode()) {
                        interestsEncounteredRatio = Math.pow(Math.E, interestsEncounteredRatio - 1);
                    }
                }

                if (totalFriendsInterested.isEmpty() || friendsInterested.get(key) == null) {
                    interestedFriendsRatio = 0;
                } else {
                    interestedFriendsRatio = (double) friendsInterested.get(key) / totalFriendsInterested.size();
                }

                Double previousCacheValue = cacheProbabilities.get(key);
                double newValue = cacheW1 * interestedNodesRatio + cacheW2 * interestsEncounteredRatio + cacheW3 * interestedFriendsRatio;
                if (interestSpaceContext.isTopicCommon(key, currentTime)) {
                    newValue += 1.0 / interestSpaceContext.getNumberOfTopics(currentTime);
                    if (newValue > 1.0) {
                        newValue = 1.0;
                    }
                }
                if (previousCacheValue == null) {
                    cacheProbabilities.put(key, newValue);
                } else {
                    cacheProbabilities.put(key, 0.75 * newValue + 0.25 * previousCacheValue);
                }
            }

            // sort messages in my memory by cache probability (so that, when I
            // have to drop some of them, I drop the ones with lower probabilities)
            Collections.sort(dataMemory, new CacheProbabilityComparator(cacheProbabilities));

            // look at all tags (that nodes are interested in) that I have seen,
            // and perform a ratio of the amount of encounters vs. all tags
            // - take into account if the tags I've seen are wanted by friends of mine
            // - take into account if the tags I've seen are also tags I'm interested in
            // - take into account if the tags I've seen belong to nodes that I've encountered often
            // - take into account if the tags I've seen are similar to what encountered nodes
            // result should be a value between 0 and 1, and should be composed with the previous one
        }

        /**
         * Class that sorts messages by caching probability.
         */
        class CacheProbabilityComparator implements Comparator<Message> {

            private Map<Integer, Double> cacheProbabilities;

            /**
             * Instantiates a {@code CacheProbabilityComparator} object.
             *
             * @param cacheProbabilities map of caching probabilities per tag
             */
            public CacheProbabilityComparator(Map<Integer, Double> cacheProbabilities) {
                this.cacheProbabilities = cacheProbabilities;
            }

            @Override
            public int compare(Message m1, Message m2) {
                double probabilityM1 = 0, probabilityM2 = 0;

                for (Topic tag : m1.getTags().getTopics()) {
                    probabilityM1 += cacheProbabilities.get(tag.getTopic());
                }

                for (Topic tag : m2.getTags().getTopics()) {
                    probabilityM2 += cacheProbabilities.get(tag.getTopic());
                }

                if (probabilityM1 < probabilityM2) {
                    return -1;
                } else if (probabilityM1 > probabilityM2) {
                    return 1;
                } else {
                    return 0;
                }
            }
        }
    }

    /**
     * Class for ONSIDE algorithm in Interest Spaces.
     */
    private class ONSIDEAlgorithm extends Algorithm {

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
        public void exchangeData(InterestSpaces encounteredNode, long contactDuration, long currentTime) {
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
                            altruism.setSelfishness(false);
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
                            altruism.setSelfishness(false);
                        }
                    }

                    insertMessage(message, encounteredNode, currentTime, altruismAnalysis, true);
                    totalMessages++;
                }
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

            return commonInterestsPercentage > encounteredInterestsThreshold;
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

        @Override
        public void preExchangeData(InterestSpaces encounteredNode, long currentTime) {
        }
    }
}
