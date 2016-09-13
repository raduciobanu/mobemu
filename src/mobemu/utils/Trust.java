/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package mobemu.utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import mobemu.node.Message;

/**
 * Helper class for trust-based algorithms.
 *
 * @author Radu
 */
public class Trust {

    /**
     * Class for trust message stats.
     */
    public static class TrustMessageStats {

        /**
         * Computes the overall correctness rate for a given list of messages.
         *
         * @param messages list of messages to compute the hit rate for
         * @return the correctness rate for the given list of messages
         */
        public static double computeCorrectnessRate(List<Message> messages) {
            int total = 0;
            double correct = 0.0;

            for (Message message : messages) {
                if (!(message instanceof TrustMessage)) {
                    continue;
                }

                TrustMessage trustMessage = (TrustMessage) message;
                for (Map.Entry<Integer, Integer> entry : trustMessage.deliveredModified.entrySet()) {
                    total++;
                    if (entry.getValue() == 0) {
                        correct++;
                    }
                }
            }

            return correct / total;
        }
    }

    /**
     * Class for storing information about transactions.
     */
    public static class TransactionData {

        /**
         * Number of positive transactions.
         */
        private int positive;
        /**
         * Number of negative transactions.
         */
        private int negative;

        public TransactionData() {
            this.positive = 0;
            this.negative = 0;
        }

        /**
         * Gets the trust for the current transaction data (percentage of
         * positive transactions per total transactions). If there were no
         * transactions, the result is -1.
         *
         * @return current transaction data
         */
        public double getTrust() {
            if (positive == 0 && negative == 0) {
                return -1;
            }

            return (double) (positive) / (positive + negative);
        }

        /**
         * Store a positive transaction.
         */
        public void positive() {
            this.positive++;
        }

        /**
         * Store a negative transaction.
         */
        public void negative() {
            this.negative++;
        }
    }

    /**
     * Class used for storing trust data from other nodes.
     */
    public static class TrustData {

        /**
         * Trust value for the given node.
         */
        public double trust;
        /**
         * Time when the trust value was recorded.
         */
        public long timestamp;

        /**
         * Constructor for a {@link TrustData} object.
         *
         * @param trust trust value
         * @param timestamp time when the trust value was recorded
         */
        public TrustData(double trust, long timestamp) {
            this.trust = trust;
            this.timestamp = timestamp;
        }
    }

    /**
     * Class for a trust-modified message.
     */
    public static class TrustMessage extends Message {

        /**
         * List containing information about every path this message takes.
         */
        private List<TrustMessageInfo> info;
        /**
         * Map containing the value of this message when delivered to each node.
         */
        private Map<Integer, Integer> deliveredModified;

        /**
         * Instantiates a {@code TrustMessage} object.
         *
         * @param message {@code Message} object to create the new object from
         */
        public TrustMessage(Message message) {
            this.id = message.getId();
            this.source = message.getSource();
            this.destination = message.getDestination();
            this.message = message.getMessage();
            this.timestamp = message.getTimestamp();
            this.tags = message.getTags();
            this.stats = message.getStats();
            this.utility = message.getUtility();

            info = new ArrayList<>();
            info.add(new TrustMessageInfo(message.getId(), source));
            deliveredModified = new HashMap<>();
        }

        /**
         * Deliver this message to the given node and store the modified value.
         *
         * @param nodeId ID of the node to deliver the message to
         * @param modified modified value of the node
         */
        public void deliverModified(int nodeId, int modified) {
            deliveredModified.put(nodeId, modified);
        }

        /**
         * Modifies the message's path based on the current transfer.
         *
         * @param previousCarrier previous node that had the message
         * @param currentCarrier new node that has the message
         * @param modify specifies whether the current node modifies the message
         * (-1 if it doesn't, a positive value if it does)
         * @return {@code true} if a correct message info was found,
         * {@code false} otherwise
         */
        public boolean addToPath(int previousCarrier, int currentCarrier, int modify) {
            TrustMessageInfo messageInfo = getInfo(previousCarrier);
            if (messageInfo != null) {
                TrustMessageInfo newMessageInfo = new TrustMessageInfo(messageInfo.getID(), source);
                for (int i = 1; i < messageInfo.path.size(); i++) {
                    newMessageInfo.addToPath(messageInfo.path.get(i));
                }
                newMessageInfo.modified = messageInfo.modified;
                info.add(newMessageInfo);
                messageInfo.addToPath(currentCarrier);

                if (modify >= 0) {
                    messageInfo.modified = modify;
                }

                return true;
            }

            return false;
        }

        /**
         * Removes an existing message path (called when a node has to delete a
         * message).
         *
         * @param previousCarrier previous node that had the message but has
         * deleted it
         * @return {@code true} if the message existed and was deleted
         * successfully, {@code false} otherwise
         */
        public boolean removePath(int previousCarrier) {
            for (int i = 0; i < info.size(); i++) {
                TrustMessageInfo messageInfo = info.get(i);

                if (messageInfo.getOwner() == previousCarrier) {
                    info.remove(i);
                    return true;
                }
            }

            return false;
        }

        /**
         * Gets the trust message information for the given carrier.
         *
         * @param carrier carrier node ID
         * @return trust message information object for the given carrier
         */
        public TrustMessageInfo getInfo(int carrier) {
            for (TrustMessageInfo messageInfo : info) {
                if (messageInfo.getOwner() == carrier && !messageInfo.hasReachedDestination()) {
                    return messageInfo;
                }
            }

            return null;
        }
    }

    /**
     * Class for storing information about trust messages.
     */
    public static class TrustMessageInfo {

        /**
         * Message ID.
         */
        private int id;
        /**
         * Specifies whether the message has been modified or not (0 is not
         * modified, any other value is modified).
         */
        private int modified;
        /**
         * List containing the nodes that this message has passed through
         * (including the source and the current carrier).
         */
        private List<Integer> path;
        /**
         * Specifies whether the message has reached its destination (where it
         * is to be analyzed when further copies arrive).
         */
        private boolean reachedDestination;

        /**
         * Instantiates a {@code TrustMessageInfo} object.
         *
         * @param id message ID
         * @param source message source
         */
        public TrustMessageInfo(int id, int source) {
            this.id = id;
            this.modified = 0;
            this.reachedDestination = false;
            this.path = new ArrayList<>();
            this.path.add(source);
        }

        /**
         * Gets the message's ID.
         *
         * @return the ID of the message
         */
        public int getID() {
            return id;
        }

        /**
         * Gets the current owner of this message.
         *
         * @return the ID of this message's owner
         */
        public int getOwner() {
            return path.get(path.size() - 1);
        }

        /**
         * Checks whether this message has been modified.
         *
         * @return 0 if the message has not been modified, any other value
         * otherwise
         */
        public int getModified() {
            return modified;
        }

        /**
         * Gets the path of this message.
         *
         * @return list containing the IDs of the nodes this message has passed
         * through
         */
        public List<Integer> getPath() {
            return path;
        }

        /**
         * Checks whether this message has reached its destination.
         *
         * @return {@code true} if the message has reached its destination,
         * {@code false} otherwise
         */
        public boolean hasReachedDestination() {
            return reachedDestination;
        }

        /**
         * Add another node to this message's path.
         *
         * @param node ID of the node to be added
         */
        public void addToPath(int node) {
            path.add(node);
        }

        /**
         * Mark this message as having reached its destination.
         */
        public void reachDestination() {
            reachedDestination = true;
        }
    }
}
