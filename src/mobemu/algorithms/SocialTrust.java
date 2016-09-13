/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package mobemu.algorithms;

import java.util.*;
import mobemu.node.Context;
import mobemu.node.Message;
import mobemu.node.Node;
import mobemu.utils.Trust;
import mobemu.utils.Trust.TrustMessage;

/**
 * Class for a Social Trust node.
 *
 * Abderrahmen Mtibaa and Khaled A. Harras. Social-based trust in mobile
 * opportunistic networks. In Proceedings of 20th International Conference on
 * Computer Communications and Networks (ICCCN), pages 1-6, July 2011.
 *
 * @author Radu
 */
public class SocialTrust extends Node {

    /**
     * Type of the filter to be applied.
     */
    private FilterType filterType;
    /**
     * Limit for the filter to be applied.
     */
    private int filterLimit;
    /**
     * {@code true} if the algorithm analyzes encountered nodes, {@code false}
     * if it analyzes the message's source
     */
    boolean relayToRelay;
    /**
     * Random value for trust computations
     */
    private static Random trustRandom = null;
    /**
     * Information about all the other nodes in the trace.
     */
    private static Node[] nodes = null;
    /**
     * {@code true} if the raw algorithm should be run (i.e. without trust
     * handling), {@code false} otherwise.
     */
    private static Boolean raw = null;

    /**
     * Constructor for the {@link SocialTrust} class.
     *
     * @param id ID of the node
     * @param context the context of this node
     * @param socialNetwork the social network as seen by this node
     * @param dataMemorySize the maximum allowed size of the data memory
     * @param exchangeHistorySize the maximum allowed size of the exchange
     * history
     * @param seed the seed for the random number generators
     * @param traceStart timestamp of the start of the trace
     * @param traceEnd timestamp of the end of the trace
     * @param filterType type of the filter to be applied
     * @param filterLimit limit for the filter (maximum social graph distance
     * for the d-distance filter, minimum common interests for the common
     * interests filter, minimum common friends for the common friends and
     * combination filters
     * @param relayToRelay {@code true} if the algorithm analyzes encountered
     * nodes, {@code false} if it analyzes the message's source
     * @param altruism altruism value of the current node (1 for 100%
     * altruistic, 0 for 100% selfish)
     * @param raw {@code true} if the raw algorithm should be run (i.e. without
     * trust handling), {@code false} otherwise
     * @param nodes array of all the nodes in the network
     */
    public SocialTrust(int id, Context context, boolean[] socialNetwork,
            int dataMemorySize, int exchangeHistorySize, long seed, long traceStart,
            long traceEnd, FilterType filterType, int filterLimit, boolean relayToRelay,
            double altruism, boolean raw, Node[] nodes) {
        super(id, nodes.length, context, socialNetwork, dataMemorySize, exchangeHistorySize, seed, traceStart, traceEnd);

        this.filterType = filterType;
        this.filterLimit = filterLimit;
        this.relayToRelay = relayToRelay;

        // set the node's altruism
        this.altruism.setLocal(altruism > 1.0 ? 1.0 : altruism < 0.0 ? 0.0 : altruism);

        if (SocialTrust.trustRandom == null) {
            SocialTrust.trustRandom = new Random(seed);
        }

        if (SocialTrust.nodes == null) {
            SocialTrust.nodes = nodes;
        }

        if (SocialTrust.raw == null) {
            SocialTrust.raw = raw;
        }
    }

    /**
     * Filter types.
     */
    public static enum FilterType {

        D_DISTANCE,
        COMMON_INTERESTS,
        COMMON_FRIENDS,
        COMBINATION
    }

    @Override
    public String getName() {
        return SocialTrust.raw ? "Social Trust raw" : "Social Trust";
    }

    @Override
    protected void onDataExchange(Node encounteredNode, long contactDuration, long currentTime) {
        epidemicDataExchange(encounteredNode, contactDuration, currentTime);
    }

    @Override
    public Message generateMessage(Message message) {
        // use specialized TrustMessage objects instead of regular Message objects
        Trust.TrustMessage newMessage = new Trust.TrustMessage(message);
        ownMessages.add(newMessage);
        return newMessage;
    }

    @Override
    public boolean insertMessage(Message message, Node from, long currentTime, boolean altruism, boolean dissemination) {
        Message firstMessage = dataMemory.size() > 0 ? dataMemory.get(0) : null;
        boolean inserted = super.insertMessage(message, from, currentTime, altruism, dissemination);

        if (inserted && firstMessage != null && dataMemory.get(0) != firstMessage
                && firstMessage instanceof TrustMessage) {
            ((TrustMessage) firstMessage).removePath(this.id);
        }

        TrustMessage trustMessage = (TrustMessage) message;
        boolean modify = false;

        // modify message based on altruism value
        if (trustRandom.nextDouble() >= this.altruism.getLocal()) {
            modify = true;
        }

        if (inserted) {
            trustMessage.addToPath(from.getId(), this.id, modify ? 1 + trustRandom.nextInt(Integer.MAX_VALUE - 1) : -1);
        }

        return inserted;
    }

    @Override
    protected int deliverDirectMessages(Node encounteredNode, boolean altruism, long contactDuration, long currentTime, boolean dissemination) {
        if (!(encounteredNode instanceof SocialTrust)) {
            return -1;
        }

        SocialTrust encounteredNodeSocialTrust = (SocialTrust) encounteredNode;
        List<Message> messagesForMe = new ArrayList<>();
        int maxMessages = network.computeMaxMessages(contactDuration);
        int totalMessages = 0;

        // for each of the messages carried by the encountered node, download
        // the ones that are intended for the current node
        for (Message message : encounteredNodeSocialTrust.dataMemory) {
            if (totalMessages >= maxMessages) {
                break;
            }

            if (context.getCommonTopics(message.getTags(), currentTime) > 0 && message.getSource() != id) {
                messagesForMe.add(message);
                totalMessages++;
            }
        }

        // deliver messages intended for the current node
        for (Message message : messagesForMe) {
            if (!message.isDelivered(id)) {
                if (!(message instanceof TrustMessage)) {
                    continue;
                }

                TrustMessage trustMessage = (TrustMessage) message;
                Trust.TrustMessageInfo info = trustMessage.getInfo(encounteredNodeSocialTrust.id);

                if (info == null) {
                    continue;
                }

                encounteredNodeSocialTrust.messagesDelivered++;
                encounteredNodeSocialTrust.messagesExchanged++;
                message.markAsDelivered(id, currentTime);
                trustMessage.deliverModified(id, info.getModified());
            }
        }

        messagesForMe.clear();

        // return if the total number of messages has been reached
        if (totalMessages >= maxMessages) {
            return 0;
        }

        // for each of the messages generated by the encountered node, download
        // the ones that are intended for the current node
        for (Message message : encounteredNodeSocialTrust.ownMessages) {
            if (totalMessages >= maxMessages) {
                break;
            }

            if (context.getCommonTopics(message.getTags(), currentTime) > 0 && message.getSource() != id) {
                messagesForMe.add(message);
                totalMessages++;
            }
        }

        // deliver messages intended for the current mode.
        for (Message message : messagesForMe) {
            if (!message.isDelivered(id)) {
                if (!(message instanceof TrustMessage)) {
                    continue;
                }

                TrustMessage trustMessage = (TrustMessage) message;
                Trust.TrustMessageInfo info = trustMessage.getInfo(encounteredNodeSocialTrust.id);

                if (info == null) {
                    continue;
                }

                encounteredNodeSocialTrust.messagesDelivered++;
                encounteredNodeSocialTrust.messagesExchanged++;
                message.markAsDelivered(id, currentTime);
                trustMessage.deliverModified(id, info.getModified());
            }
        }

        if (maxMessages == Integer.MAX_VALUE) {
            return maxMessages;
        }

        return Math.max(maxMessages - totalMessages, 0);
    }

    /**
     * Epidemic algorithm modified for SocialTrust.
     *
     * @param encounteredNode encountered node
     * @param contactDuration duration of the contact
     * @param currentTime current timestamp
     */
    private void epidemicDataExchange(Node encounteredNode, long contactDuration, long currentTime) {
        if (!(encounteredNode instanceof SocialTrust)) {
            return;
        }

        SocialTrust encounteredNodeSocialTrust = (SocialTrust) encounteredNode;

        // start of actual dissemination algorithm (this part should be replaced with any desired dissemination algorithm)
        int remainingMessages = deliverDirectMessages(encounteredNodeSocialTrust, false, contactDuration, currentTime, true);
        int totalMessages = 0;

        for (Message message : encounteredNodeSocialTrust.dataMemory) {
            if (totalMessages >= remainingMessages) {
                return;
            }

            // this is the extra trust condition (if the encountered node trusts the
            // current node or the source of the message, it delivers the data)
            if (SocialTrust.raw || encounteredNodeSocialTrust.isTrusted(this, message, currentTime)) {
                insertMessage(message, encounteredNodeSocialTrust, currentTime, false, true);
                totalMessages++;
            }
        }

        for (Message message : encounteredNodeSocialTrust.ownMessages) {
            if (totalMessages >= remainingMessages) {
                return;
            }

            insertMessage(message, encounteredNodeSocialTrust, currentTime, false, true);
            totalMessages++;
        }
    }

    /**
     * Checks whether an encountered node or message is trusted by the current
     * node (based on whether the algorithm is ran as relay-to-relay, or
     * source-to-relay).
     *
     * @param encounteredNode the encountered node
     * @param message the encountered message
     * @param currentTime current timestamp of the trace
     * @return {@code true} if the node or message is trusted, {@code false}
     * otherwise
     */
    private boolean isTrusted(SocialTrust encounteredNode, Message message, Long currentTime) {

        switch (filterType) {
            // for the d-distance filter, check if the minimum social distance
            // between the carrier node and the encountered node or the source
            // of the message is lower than the specified limit
            case D_DISTANCE:
                return getMinimumSocialDistance(relayToRelay ? encounteredNode : nodes[message.getSource()]) <= filterLimit;

            // for the common interests filter, check if the number of common
            // interests between the carrier node and the encountered node or
            // the source of the message is higher than the specified limit
            case COMMON_INTERESTS:
                return context.getCommonTopics(relayToRelay ? encounteredNode.context : nodes[message.getSource()].getContext(), currentTime) >= filterLimit;

            // for the common friends filter, check if the number of common social
            // network friends between the carrier node and the encountered node
            // of the source of the message is higher than the specified limit
            case COMMON_FRIENDS:
                return getCommonNeighbors(relayToRelay ? encounteredNode : nodes[message.getSource()]) >= filterLimit;

            // for the combination filter, check if the social distance between
            // the carrier node and the encountered node or the source of the
            // message is 1, and that the number of common social network
            // friends is higher than the specified limit
            case COMBINATION:
                if (relayToRelay) {
                    return socialNetwork[encounteredNode.getId()] && getCommonNeighbors(encounteredNode) >= filterLimit;
                } else {
                    return socialNetwork[message.getSource()] && getCommonNeighbors(nodes[message.getSource()]) >= filterLimit;
                }

            default:
                return false;
        }
    }

    /**
     * Computes the minimum social distance between the current node and a given
     * node, within the limit specified by {@code filterLimit}.
     *
     * @param node node to compute social distance for
     * @return the minimum social distance between the current node and the
     * given one, or MAX_INT if the value is higher than {@code filterLimit}
     */
    private int getMinimumSocialDistance(Node node) {

        Set<Integer> currentNodes = new HashSet<>();
        Set<Integer> nextNodes = new HashSet<>();
        Set<Integer> visited = new HashSet<>();
        int level = 1;

        // initial current nodes set contains the direct friends of the current node
        for (int i = 0; i < socialNetwork.length; i++) {
            if (socialNetwork[i]) {
                if (i == node.getId()) {
                    return level;
                }

                currentNodes.add(i);
            }
        }

        visited.addAll(currentNodes);

        while (!currentNodes.isEmpty() && level < filterLimit) {
            level++;

            for (Integer socialNode : currentNodes) {
                for (int i = 0; i < socialNetwork.length; i++) {
                    if (nodes[socialNode].inSocialNetwork(i) && !visited.contains(i)) {
                        if (i == node.getId()) {
                            return level;
                        }

                        nextNodes.add(i);
                        visited.add(i);
                    }
                }
            }

            currentNodes.clear();
            currentNodes.addAll(nextNodes);
            nextNodes.clear();
        }

        return Integer.MAX_VALUE;
    }
}
