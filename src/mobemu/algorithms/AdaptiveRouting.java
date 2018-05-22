/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package mobemu.algorithms;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import mobemu.node.Centrality;
import mobemu.node.Context;
import mobemu.node.Message;
import mobemu.node.Node;

/**
 * Class for an Adaptive Routing node.
 *
 * Radu-Ioan Ciobanu, Ciprian Dobre, Daniel Gutierrez Raina and Sergio L. Toral.
 * A dynamic data routing solution for opportunistic networks. In Proceedings of
 * the 14th International Conference on Telecommunications, ConTEL 2017, pages
 * 83-90, Zagreb, Croatia, 2017. IEEE.
 *
 * @author Radu
 */
public class AdaptiveRouting extends Node {

    /**
     * Number of contacts with each node.
     */
    private int contactCount[];
    /**
     * Contact duration with each node.
     */
    private long contactTime[];
    /**
     * Number of contacts with any node.
     */
    private int anyContactCount = 0;
    /**
     * Contact duration with any node.
     */
    private long anyContactTime = 0;
    /**
     * Inter-contact time with each node.
     */
    private long interContactTime[];
    /**
     * Last contact time with each node.
     */
    private long lastContactTime[];
    /**
     * Inter-contact time with any node.
     */
    private long interAnyContactTime = 0;
    /**
     * Last contact time with any node.
     */
    private long lastAnyContactTime = 0;
    /**
     * Total number of nodes.
     */
    private int totalNodes = 0;
    /**
     * List of nodes.
     */
    private Node[] nodeList;
    /**
     * Random number generator.
     */
    private static Random RAND = null;
    /**
     * Maximum values of utility function parameters seen so far (used for
     * normalization).
     */
    private UtilityFunctionParams maxParams;
    /**
     * Initial weights for (in this order) similarity, centrality, friendship,
     * strength, trust.
     */
    //double[] initialWeights = {0.05, 0.3, 0.5, 0.1, 0.05}; // UPB
    //double[] initialWeights = {0.05, 0.3, 0.3, 0.1, 0.25}; // Sigcomm
    double[] initialWeights = {0.2, 0.4, 0.1, 0.1, 0.2}; // HCMM
    /**
     * Algorithm version (1 - equal split, 2 - weighted split, 3 - equal
     * values).
     */
    private int version;
    /**
     * Default message exchange probability.
     */
    private double prob;
    /**
     * Difference between two nodes' parameters, in order for them to be
     * considered close enough.
     */
    private double closeness;
    /**
     * Set to {@code true} if a comparison should be performed between the
     * utility parameters of the two encountering nodes.
     */
    private boolean compare;
    /**
     * Set to {@code true} if the algorithm should use only default parameters.
     */
    private boolean defaultRun;
    /**
     * Map that specifies the importance of each message from this node's
     * standpoint.
     */
    private Map<Integer, Double> messageImportances;
    /**
     * Sorter for the message in the data memory.
     */
    private Comparator<Message> messageSorter;

    /**
     * Constructor for the {@link AdaptiveRouting} class.
     *
     * @param id ID of the node
     * @param nodes total number of existing nodes
     * @param context the context of this node
     * @param socialNetwork the social network as seen by this node
     * @param dataMemorySize the maximum allowed size of the data memory
     * @param exchangeHistorySize the maximum allowed size of the exchange
     * history
     * @param seed the seed for the random number generators
     * @param traceStart timestamp of the start of the trace
     * @param traceEnd timestamp of the end of the trace
     * @param nodeList list of trace nodes
     * @param version algorithm version (1 - equal split, 2 - weighted split, 3
     * - equal values)
     * @param prob default message exchange probability
     * @param closeness difference between two nodes' parameters, in order for
     * them to be considered close enough
     * @param compare set to {@code true} if a comparison should be performed
     * between the utility parameters of the two encountering nodes
     * @param defaultRun set to {@code true} if the algorithm should use only
     * default parameters
     */
    public AdaptiveRouting(int id, int nodes, Context context, boolean[] socialNetwork, int dataMemorySize, int exchangeHistorySize,
            long seed, long traceStart, long traceEnd, Node[] nodeList, int version, double prob, double closeness, boolean compare, boolean defaultRun) {
        super(id, nodes, context, socialNetwork, dataMemorySize, exchangeHistorySize, seed, traceStart, traceEnd);

        this.nodeList = nodeList;
        if (AdaptiveRouting.RAND == null) {
            AdaptiveRouting.RAND = new Random(seed);
        }
        this.totalNodes = nodes;
        this.contactCount = new int[nodes];
        this.contactTime = new long[nodes];
        this.interContactTime = new long[nodes];
        this.lastContactTime = new long[nodes];
        this.maxParams = new UtilityFunctionParams(-1);
        this.messageImportances = new HashMap<>();
        this.messageSorter = Comparator.comparing(Message::getUtility).thenComparing(Message::getTimestamp);

        this.version = version;
        this.prob = prob;
        this.closeness = closeness;
        this.compare = compare;
        this.defaultRun = defaultRun;
    }

    @Override
    public String getName() {
        return "Adaptive Routing";
    }

    @Override
    protected void onDataExchange(Node encounteredNode, long contactDuration, long currentTime) {
        if (!(encounteredNode instanceof AdaptiveRouting)) {
            return;
        }

        AdaptiveRouting encNode = (AdaptiveRouting) encounteredNode;

        // update contact information with data about newly-encountered node
        updateContactData(this, encNode, contactDuration, currentTime);
        updateContactData(encNode, this, contactDuration, currentTime);

        int remainingMessages = deliverDirectMessages(encNode, false, contactDuration, currentTime, false);
        int totalMessages = 0;

        // download from the encountered node's memory all messages
        // required (based on download probability function)
        for (Message message : encNode.dataMemory) {
            if (totalMessages >= remainingMessages) {
                return;
            }

            if (shouldDownloadProbability(encNode, message, currentTime)) {
                insertMessage(message, encNode, currentTime, false, false);
                totalMessages++;
            }
        }

        // download from the encountered node's own messages all messages
        // required (based on download probability function)
        for (Message message : encNode.ownMessages) {
            if (totalMessages >= remainingMessages) {
                return;
            }

            if (shouldDownloadProbability(encNode, message, currentTime)) {
                insertMessage(message, encNode, currentTime, false, false);
                totalMessages++;
            }
        }

        for (Message m : dataMemory) {
            m.setUtility(messageImportances.get(m.getId()));
        }

        if (dataMemory.size() > 1) {
            dataMemory.sort(messageSorter);
        }
    }

    /**
     * Sets the initial weights for the current node.
     *
     * @param similarityWeight weight for similarity
     * @param centralityWeight weight for centrality
     * @param friendshipWeight weight for friendship
     * @param strengthWeight weight for strength
     * @param trustWeight weight for trust
     */
    public void setInitialWeights(double similarityWeight, double centralityWeight,
            double friendshipWeight, double strengthWeight, double trustWeight) {
        initialWeights[0] = similarityWeight;
        initialWeights[1] = centralityWeight;
        initialWeights[2] = friendshipWeight;
        initialWeights[3] = strengthWeight;
        initialWeights[4] = trustWeight;
    }

    /**
     * Shows contact metrics.
     *
     * @return a string containing average contact, inter-contact, any-contact
     * and inter-any-contact times
     */
    public String showMetrics() {
        double averageContactTime = 0;
        double averageInterContactTime = 0;
        int countCT = 0;
        int countICT = 0;

        for (int i = 0; i < totalNodes; i++) {
            if (contactCount[i] > 0) {
                countCT++;
                averageContactTime += (double) contactTime[i] / contactCount[i];
            }

            if (contactCount[i] > 1) {
                countICT++;
                averageInterContactTime += (double) interContactTime[i] / (contactCount[i] - 1);
            }
        }

        averageContactTime /= countCT;
        averageInterContactTime /= countICT;

        return "" + averageContactTime + "," + averageInterContactTime + ","
                + (double) anyContactTime / anyContactCount + "," + (double) interAnyContactTime / (anyContactCount - 1);
    }

    /**
     * Gets a node's familiars (i.e., nodes it has encountered often and has
     * stayed in contact with for long durations).
     *
     * @return list of familiar nodes
     */
    public List<Integer> getFamiliars() {
        List<Integer> familiars = new ArrayList<>();

        int contactsLimit = anyContactCount / totalNodes;
        long contactTimeLimit = anyContactTime / totalNodes;

        for (int i = 0; i < totalNodes; i++) {
            if (contactCount[i] > contactsLimit && contactTime[i] > contactTimeLimit) {
                familiars.add(i);
            }
        }

        return familiars;
    }

    /**
     * Updates node context data upon a contact.
     *
     * @param node node that sees the contact
     * @param encNode node encountered
     * @param contactDuration duration of the contact
     * @param currentTime current trace time
     */
    private static void updateContactData(AdaptiveRouting node, AdaptiveRouting encNode, long contactDuration, long currentTime) {
        // compute information for contact time
        node.contactCount[encNode.id]++;
        node.contactTime[encNode.id] += contactDuration;

        // compute information for any-contact time
        node.anyContactCount++;
        node.anyContactTime += contactDuration;

        // compute information for inter-contact time
        if (node.lastContactTime[encNode.id] != 0) {
            node.interContactTime[encNode.id] += currentTime - node.lastContactTime[encNode.id];
        }

        node.lastContactTime[encNode.id] = currentTime;

        // compute information for inter-any-contact time
        if (node.lastAnyContactTime != 0) {
            node.interAnyContactTime += currentTime - node.lastAnyContactTime;
        }

        node.lastAnyContactTime = currentTime;
    }

    /**
     * Decides if a message from an encountered node should be downloaded.
     *
     * @param encounteredNode encountered node
     * @param message message to be analyzed
     * @param currentTime current trace time
     * @return {@code true} if the message should be downloaded, {@code false}
     * otherwise
     */
    private boolean shouldDownloadProbability(AdaptiveRouting encounteredNode, Message message, long currentTime) {
        double nodeProbability = 0;

        // compute the utility function parameters from the POV of each of the two encountering nodes
        UtilityFunctionParams params = computeUtilityFunctionParams(this, message.getDestination(), currentTime);
        UtilityFunctionParams paramsEncounteredNode = computeUtilityFunctionParams((AdaptiveRouting) encounteredNode, message.getDestination(), currentTime);

        // if the nodes have one parameter that is similar, set it to 0, and use that remaining value for the others
        double[] weights = Arrays.copyOf(initialWeights, initialWeights.length);
        List<Integer> zeroIndices = new ArrayList<>(5);
        int inCommon = 0;

        // count the number of parameters "in common" (i.e., similar in value)
        if (Math.abs(params.similarity * initialWeights[0] - paramsEncounteredNode.similarity * encounteredNode.initialWeights[0]) <= closeness) {
            inCommon++;
        }

        if (Math.abs(params.centrality * initialWeights[1] - paramsEncounteredNode.centrality * encounteredNode.initialWeights[1]) <= closeness) {
            inCommon++;
        }

        if (Math.abs(params.friendship * initialWeights[2] - paramsEncounteredNode.friendship * encounteredNode.initialWeights[2]) <= closeness) {
            inCommon++;
        }

        if (Math.abs(params.strength * initialWeights[3] - paramsEncounteredNode.strength * encounteredNode.initialWeights[3]) <= closeness) {
            inCommon++;
        }

        if (Math.abs(params.trust * initialWeights[4] - paramsEncounteredNode.trust * encounteredNode.initialWeights[4]) <= closeness) {
            inCommon++;
        }

        // if all parameters are similar, use initial formula
        if (defaultRun) {
            nodeProbability = initialWeights[0] * params.similarity + initialWeights[1] * params.centrality
                    + initialWeights[2] * params.friendship + initialWeights[3] * params.strength + initialWeights[4] * params.trust;
        } else if (inCommon == 5) {
            // if all five metrics are similar and the default probability is greater than 1, set to default value
            if (prob > 1) {
                prob = initialWeights[0] * params.similarity + initialWeights[1] * params.centrality
                        + initialWeights[2] * params.friendship + initialWeights[3] * params.strength + initialWeights[4] * params.trust;
            }

            // if node comparison is enabled, compute utilities for both nodes and compare them
            if (compare) {
                double pNode = initialWeights[0] * params.similarity + initialWeights[1] * params.centrality
                        + initialWeights[2] * params.friendship + initialWeights[3] * params.strength + initialWeights[4] * params.trust;
                double pEncountered = initialWeights[0] * paramsEncounteredNode.similarity + initialWeights[1] * paramsEncounteredNode.centrality
                        + initialWeights[2] * paramsEncounteredNode.friendship + initialWeights[3] * paramsEncounteredNode.strength
                        + initialWeights[4] * paramsEncounteredNode.trust;
                nodeProbability = pEncountered > pNode ? prob : 1;
            } else {
                nodeProbability = prob;
            }
        } else {
            List<Integer> similarMetrics = new ArrayList<>(5);
            double weightRemainder = 0;

            // if a parameter is similar for both nodes, set its weight
            // to zero and update the other weights accordingly
            if (Math.abs(params.similarity * initialWeights[0] - paramsEncounteredNode.similarity * encounteredNode.initialWeights[0]) <= closeness) {
                updateWeights(zeroIndices, weights, 0);
                weightRemainder += updateInitialWeights(similarMetrics, 0);
            }

            if (Math.abs(params.centrality * initialWeights[1] - paramsEncounteredNode.centrality * encounteredNode.initialWeights[1]) <= closeness) {
                updateWeights(zeroIndices, weights, 1);
                weightRemainder += updateInitialWeights(similarMetrics, 1);
            }

            if (Math.abs(params.friendship * initialWeights[2] - paramsEncounteredNode.friendship * encounteredNode.initialWeights[2]) <= closeness) {
                updateWeights(zeroIndices, weights, 2);
                weightRemainder += updateInitialWeights(similarMetrics, 2);
            }

            if (Math.abs(params.strength * initialWeights[3] - paramsEncounteredNode.strength * encounteredNode.initialWeights[3]) <= closeness) {
                updateWeights(zeroIndices, weights, 3);
                weightRemainder += updateInitialWeights(similarMetrics, 3);
            }

            if (Math.abs(params.trust * initialWeights[4] - paramsEncounteredNode.trust * encounteredNode.initialWeights[4]) <= closeness) {
                updateWeights(zeroIndices, weights, 4);
                weightRemainder += updateInitialWeights(similarMetrics, 4);
            }

            nodeProbability = weights[0] * params.similarity + weights[1] * params.centrality
                    + weights[2] * params.friendship + weights[0] * params.strength + weights[3] * params.trust;

            for (int i = 0; i < 5; i++) {
                if (!similarMetrics.contains(i)) {
                    initialWeights[i] += weightRemainder / (5 - similarMetrics.size());
                }
            }
        }

        // set the importance of this message as its probability
        messageImportances.put(message.getId(), nodeProbability);

        return RAND.nextDouble() <= nodeProbability;
    }

    private double updateInitialWeights(List<Integer> similarMetrics, int index) {
        double decrease = 0.01;

        double ret = 0;
        similarMetrics.add(index);

        if (initialWeights[index] > decrease) {
            initialWeights[index] -= decrease;
            ret = decrease;
        } else {
            ret = initialWeights[index];
            initialWeights[index] = 0;
        }

        return ret;
    }

    /**
     * Updates the weights when a metric is similar for two encountering nodes.
     *
     * @param zeroIndices list of indices with the value 0
     * @param weights array of weight values
     * @param index index of the current metric weight in the weight array
     */
    private void updateWeights(List<Integer> zeroIndices, double[] weights, int index) {
        zeroIndices.add(index);

        double sum = 0.0;
        for (int i = 0; i < 5; i++) {
            if (!zeroIndices.contains(i)) {
                sum += weights[i];
            }
        }

        for (int i = 0; i < 5; i++) {
            if (!zeroIndices.contains(i)) {
                switch (version) {
                    case 1: //equal split
                        weights[i] += weights[index] / (5 - zeroIndices.size());
                        break;
                    case 2: // weighted split
                        weights[i] += weights[index] * weights[i] / sum;
                        break;
                    case 3: // equal values
                    default:
                        weights[i] = 1.0 / (5 - zeroIndices.size());
                        break;
                }
            }
        }

        weights[index] = 0;
    }

    /**
     * Computes the utility function parameters for a given node towards a given
     * destination.
     *
     * @param node node to compute the parameters for
     * @param destination the ID of the destination to be used for similarity
     * and friendship computation
     * @param currentTime current trace time
     * @return the utility function parameters
     */
    private static UtilityFunctionParams computeUtilityFunctionParams(AdaptiveRouting node, int destination, long currentTime) {
        UtilityFunctionParams params = new UtilityFunctionParams();

        // 1) similarity with the destination (number of common neighbors between individuals on social networks)
        params.similarity = node.getCommonNeighbors((AdaptiveRouting) node.nodeList[destination]);
        node.maxParams.similarity = Math.max(node.maxParams.similarity, params.similarity);
        params.similarity = node.maxParams.similarity == 0 ? 0 : params.similarity / node.maxParams.similarity;

        // 2) centrality (using k-clique, per-node, no destination)
        params.centrality = node.centrality.getValue(Centrality.CentralityValue.CURRENT);
        node.maxParams.centrality = Math.max(node.maxParams.centrality, params.centrality);
        params.centrality = node.maxParams.centrality == 0 ? 0 : params.centrality / node.maxParams.centrality;

        // 3) friendship (common interests with the destination)
        params.friendship = node.context.getCommonTopics(node.nodeList[destination].getContext(), currentTime);
        node.maxParams.friendship = Math.max(node.maxParams.friendship, params.friendship);
        params.friendship = node.maxParams.friendship == 0 ? 0 : params.friendship / node.maxParams.friendship;

        // 4) social strength (number of social network friends)
        params.strength = node.getSocialStrength();
        node.maxParams.strength = Math.max(node.maxParams.strength, params.strength);
        params.strength = node.maxParams.strength == 0 ? 0 : params.strength / node.maxParams.strength;

        // 5) trust (number of delivered messages)
        params.trust = node.messagesDelivered;
        node.maxParams.trust = Math.max(node.maxParams.trust, params.trust);
        params.trust = node.maxParams.trust == 0 ? 0 : params.trust / node.maxParams.trust;

        return params;
    }

    /**
     * Computes the number of common neighbors between the current node and
     * another node.
     *
     * @param otherNode the other node
     * @return number of common neighbors between the current node and the other
     * node
     */
    private int getCommonNeighbors(AdaptiveRouting otherNode) {
        int count = 0;

        for (int i = 0; i < socialNetwork.length; i++) {
            if (socialNetwork[i] && otherNode.socialNetwork[i]) {
                count++;
            }

            if (inLocalCommunity(i) && otherNode.inLocalCommunity(i)) {
                count++;
            }
        }

        return count;
    }

    /**
     * Computes the social strength of the current node as the number of social
     * connections.
     *
     * @return the social strength of the current node
     */
    private int getSocialStrength() {
        int count = 0;

        for (int i = 0; i < totalNodes; i++) {
            if (socialNetwork[i]) {
                count++;
            }

            if (inLocalCommunity(i)) {
                count++;
            }
        }

        return count;
    }

    /**
     * Class for representing the parameters of the utility function for
     * adaptive routing.
     */
    private static class UtilityFunctionParams {

        private double similarity;
        private double centrality;
        private double friendship;
        private double strength;
        private double trust;

        /**
         * Constructs a {@code UtilityFunctionParams} object.
         *
         * @param defaultValue default value for the five metrics
         */
        public UtilityFunctionParams(double defaultValue) {
            similarity = defaultValue;
            centrality = defaultValue;
            friendship = defaultValue;
            strength = defaultValue;
            trust = defaultValue;
        }

        /**
         * Constructs a {@code UtilityFunctionParams} object.
         */
        public UtilityFunctionParams() {
            this(0);
        }
    }
}
