/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package mobemu.algorithms;

import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.Map;
import mobemu.node.*;

/**
 * Class for an ONSIDE node.
 *
 * Radu-Ioan Ciobanu, Radu-Corneliu Marin, Ciprian Dobre, and Valentin Cristea.
 * Interest-awareness in data dissemination for opportunistic networks. Ad Hoc
 * Networks, 25:330-345, 2015.
 *
 * @author Radu
 */
public class ONSIDE extends Node {

    /**
     * Threshold for encountered interests.
     */
    private double encounteredInterestsThreshold;
    /**
     * Threshold for interested friends.
     */
    private int interestedFriendsThreshold;
    /**
     * Altruism analysis.
     */
    private boolean altruismAnalysis;
    /**
     * Minimum number of common interests required between two nodes, in order
     * for them to exchange data.
     */
    private int commonInterests;
    /**
     * Information about all the other nodes in the trace.
     */
    private static Node[] nodes = null;
    /**
     * Type of sort to be performed on the messages at each data exchange.
     */
    private ONSIDESort sort = ONSIDESort.None;

    /**
     * Instantiates an {@code ONSIDE} object.
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
     * @param interestedFriendsThreshold threshold for interested friends
     * @param encounteredInterestsThreshold threshold for encountered interests
     * @param commonInterests common interests required between two nodes, in
     * order for them to exchange data
     * @param sort type of sort to be performed on the messages at each data
     * exchange
     */
    public ONSIDE(int id, Context context, boolean[] socialNetwork, int dataMemorySize, int exchangeHistorySize,
            long seed, long traceStart, long traceEnd, boolean altruism, Node[] nodes,
            int interestedFriendsThreshold, double encounteredInterestsThreshold, int commonInterests, ONSIDESort sort) {
        super(id, nodes.length, context, socialNetwork, dataMemorySize, exchangeHistorySize, seed, traceStart, traceEnd);

        this.altruismAnalysis = altruism;
        this.interestedFriendsThreshold = interestedFriendsThreshold;
        this.encounteredInterestsThreshold = encounteredInterestsThreshold;
        this.commonInterests = commonInterests;
        this.sort = sort;

        if (ONSIDE.nodes == null) {
            ONSIDE.nodes = nodes;
        }
    }

    @Override
    public String getName() {
        return "ONSIDE";
    }

    /**
     * Type of sort to be performed on the messages at each data exchange.
     */
    public enum ONSIDESort {

        None, IncreasingTimestamp, DecreasingTimestamp, CommonTopics, FriendsInterested, EncounteredInterests
    }

    @Override
    protected void onDataExchange(Node encounteredNode, long contactDuration, long currentTime) {
        if (!(encounteredNode instanceof ONSIDE)) {
            return;
        }

        ONSIDE onsideEncounteredNode = (ONSIDE) encounteredNode;
        int remainingMessages = deliverDirectMessages(onsideEncounteredNode, altruismAnalysis, contactDuration, currentTime, true);
        int totalMessages = 0;

        Comparator comparator = null;
        switch (sort) {
            case IncreasingTimestamp:
                comparator = new IncreasingTimestampComparator();
                break;
            case DecreasingTimestamp:
                comparator = new DecreasingTimestampComparator();
                break;
            case CommonTopics:
                comparator = new CommonTopicsComparator(this, currentTime);
                break;
            case FriendsInterested:
                comparator = new FriendsInterestedComparator(this, currentTime);
                break;
            case EncounteredInterests:
                comparator = new EncounteredInterestsComparator(this, currentTime);
                break;
            default:
                break;
        }

        if (comparator != null) {
            Collections.sort(dataMemory, comparator);
        }

        for (Message message : onsideEncounteredNode.dataMemory) {
            if (totalMessages >= remainingMessages) {
                return;
            }

            if (!dataMemory.contains(message) && !ownMessages.contains(message)
                    && context.getCommonTopics(onsideEncounteredNode.context, currentTime) >= commonInterests
                    && (context.getCommonTopics(message.getTags(), currentTime) > 0
                    || haveFriendsInterestedInContext(message.getTags(), interestedFriendsThreshold, currentTime)
                    || willEncounterContext(message.getTags(), currentTime))) {

                if (altruismAnalysis) {
                    if (!onsideEncounteredNode.altruism.isSelfish() && !checkAltruism(onsideEncounteredNode, message)) {
                        altruism.setSelfishness(true);

                        if (context.getCommonTopics(message.getTags(), currentTime) > 0) {
                            altruism.increaseLocal();
                        } else {
                            altruism.increaseGlobal();
                        }

                        continue;
                    } else if (!onsideEncounteredNode.altruism.isSelfish()) {
                        altruism.setSelfishness(false);
                    }
                }

                insertMessage(message, onsideEncounteredNode, currentTime, altruismAnalysis, true);
                totalMessages++;
            }
        }

        if (comparator != null) {
            Collections.sort(dataMemory, comparator);
        }

        for (Message message : onsideEncounteredNode.ownMessages) {
            if (totalMessages >= remainingMessages) {
                return;
            }

            if (!dataMemory.contains(message) && !ownMessages.contains(message)
                    && context.getCommonTopics(onsideEncounteredNode.context, currentTime) >= commonInterests
                    && (context.getCommonTopics(message.getTags(), currentTime) > 0
                    || haveFriendsInterestedInContext(message.getTags(), interestedFriendsThreshold, currentTime)
                    || willEncounterContext(message.getTags(), currentTime))) {

                if (altruismAnalysis) {
                    if (!onsideEncounteredNode.altruism.isSelfish() && !checkAltruism(onsideEncounteredNode, message)) {
                        altruism.setSelfishness(true);

                        if (context.getCommonTopics(message.getTags(), currentTime) > 0) {
                            altruism.increaseLocal();
                        } else {
                            altruism.increaseGlobal();
                        }

                        continue;
                    } else if (!onsideEncounteredNode.altruism.isSelfish()) {
                        altruism.setSelfishness(false);
                    }
                }

                insertMessage(message, onsideEncounteredNode, currentTime, altruismAnalysis, true);
                totalMessages++;
            }
        }
    }

    /**
     * Checks the altruism of this node towards a message, from the standpoint
     * of an encountered node.
     *
     * @param encounteredNode the encountered node
     * @param message message to be analyzed
     * @return {@code true} if the message is to be transferred, {@code false}
     * otherwise
     */
    private boolean checkAltruism(ONSIDE encounteredNode, Message message) {
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

    /**
     * Checks whether this node is likely to encounter a node with given tags,
     * based on the history of encountered interests
     *
     * @param tags tags to check against
     * @return {@code true} if the given node is likely to encounter the set of
     * tags, {@code false} otherwise
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
     * Checks whether a node has social network friends interested in a set of
     * tags.
     *
     * @param tags set of tags the node's friends should be interested in
     * @param noFriends number of friends that are interested in this set of
     * tags
     * @return {@code true} if the node given as parameter has the specified
     * number of friends interested in the given tags, {@code false} otherwise
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

    /**
     * Class that sorts messages by increasing timestamp.
     */
    static class IncreasingTimestampComparator implements Comparator<Message> {

        @Override
        public int compare(Message m1, Message m2) {
            if (m1.getTimestamp() < m2.getTimestamp()) {
                return -1;
            } else if (m1.getTimestamp() > m2.getTimestamp()) {
                return 1;
            } else {
                return 0;
            }
        }
    }

    /**
     * Class that sorts messages by decreasing timestamp.
     */
    static class DecreasingTimestampComparator implements Comparator<Message> {

        @Override
        public int compare(Message m1, Message m2) {
            if (m1.getTimestamp() < m2.getTimestamp()) {
                return 1;
            } else if (m1.getTimestamp() > m2.getTimestamp()) {
                return -1;
            } else {
                return 0;
            }
        }
    }

    /**
     * Class that sorts messages by the number of common topics between a
     * message and the current node.
     */
    static class CommonTopicsComparator implements Comparator<Message> {

        private ONSIDE node;
        private long time;

        /**
         * {@code CommonTopicsComparator} constructor.
         *
         * @param node node this comparator is applied to
         * @param time current trace time
         */
        public CommonTopicsComparator(ONSIDE node, long time) {
            this.node = node;
            this.time = time;
        }

        @Override
        public int compare(Message m1, Message m2) {

            int m1Common = node.context.getCommonTopics(m1.getTags(), time);
            int m2Common = node.context.getCommonTopics(m2.getTags(), time);

            if (m1Common < m2Common) {
                return -1;
            } else if (m1Common > m2Common) {
                return 1;
            } else {
                return 0;
            }
        }
    }

    /**
     * Class that sorts messages by the number of the current node's friends
     * interested in a message's tags.
     */
    static class FriendsInterestedComparator implements Comparator<Message> {

        private ONSIDE node;
        private long time;

        /**
         * {@code FriendsInterestedComparator} constructor.
         *
         * @param node node this comparator is applied to
         * @param time current trace time
         */
        public FriendsInterestedComparator(ONSIDE node, long time) {
            this.node = node;
            this.time = time;
        }

        @Override
        public int compare(Message m1, Message m2) {
            int m1Count = 0;
            int m2Count = 0;

            for (int i = 0; i < node.socialNetwork.length; i++) {
                if (node.socialNetwork[i]) {
                    if (nodes[i].getContext().getCommonTopics(m1.getTags(), time) > 0) {
                        m1Count++;
                    }

                    if (nodes[i].getContext().getCommonTopics(m2.getTags(), time) > 0) {
                        m2Count++;
                    }
                }
            }

            if (m1Count < m2Count) {
                return -1;
            } else if (m1Count > m2Count) {
                return 1;
            } else {
                return 0;
            }
        }
    }

    /**
     * Class that sorts message by the percentage of encountered interests.
     */
    static class EncounteredInterestsComparator implements Comparator<Message> {

        private ONSIDE node;
        private long time;

        /**
         * {@code EncounteredInterestsComparator} constructor.
         *
         * @param node node this comparator is applied to
         * @param time current trace time
         */
        public EncounteredInterestsComparator(ONSIDE node, long time) {
            this.node = node;
            this.time = time;
        }

        @Override
        public int compare(Message m1, Message m2) {
            double m1Percentage = 0.0;
            double m2Percentage = 0.0;
            int total = 0;

            Iterator it = node.encounteredNodes.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry<Integer, ContactInfo> pairs = (Map.Entry) it.next();
                ContactInfo info = pairs.getValue();
                int nodeId = pairs.getKey();

                m1Percentage += info.getContacts() * (nodes[nodeId].getContext().getCommonTopics(m1.getTags(), time) > 0 ? 1 : 0);
                m2Percentage += info.getContacts() * (nodes[nodeId].getContext().getCommonTopics(m2.getTags(), time) > 0 ? 1 : 0);
                total += info.getContacts();
            }

            m1Percentage /= total;
            m2Percentage /= total;

            if (m1Percentage < m2Percentage) {
                return -1;
            } else if (m1Percentage > m2Percentage) {
                return 1;
            } else {
                return 0;
            }
        }
    }
}
