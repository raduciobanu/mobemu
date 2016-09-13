/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package mobemu.node;

import java.util.List;

/**
 * Class with static methods for performing computations on message statistics.
 *
 * @author Radu
 */
public class Stats {

    /**
     * Computes the overall hit rate for a given list of messages.
     *
     * @param messages list of messages to compute the hit rate for
     * @param nodes array of trace nodes
     * @param dissemination {@code true} if the messages were used for
     * disseminating data, {@code false} if they were employed for forwarding
     * @return the hit rate for the given list of messages
     */
    public static double computeHitRate(List<Message> messages, Node[] nodes, boolean dissemination) {
        double hitRate = 0;

        if (dissemination) {
            int totalMessagesGenerated = 0;

            // for dissemination messages, compute the total number of generated
            // messages as the sum of all nodes interested in each message
            for (Message message : messages) {
                for (Node node : nodes) {
                    if (node.getContext().getCommonTopics(message.getTags(), Long.MAX_VALUE) > 0 && node.getId() != message.getSource()) {
                        totalMessagesGenerated++;

                        if (message.isDelivered(node.getId())) {
                            hitRate++;
                        }
                    }
                }
            }

            hitRate /= totalMessagesGenerated;
        } else {
            for (Message message : messages) {
                if (message.isDelivered(message.getDestination())) {
                    hitRate++;
                }
            }

            hitRate /= messages.size();
        }

        return hitRate;
    }

    /**
     * Computes the overall delivery cost for a given list of messages.
     *
     * @param messages list of messages to compute the delivery cost for
     * @param nodes array of trace nodes
     * @param dissemination {@code true} if the messages were used for
     * disseminating data, {@code false} if they were employed for forwarding
     * @return the delivery cost for the given list of messages
     */
    public static double computeDeliveryCost(List<Message> messages, Node[] nodes, boolean dissemination) {
        double deliveryCost = 0.0;

        if (dissemination) {
            int totalMessagesGenerated = 0;

            for (Node node : nodes) {
                deliveryCost += node.getMessagesExchanged();
            }

            // for dissemination messages, compute the total number of generated
            // messages as the sum of all nodes interested in each message
            for (Message message : messages) {
                for (Node node : nodes) {
                    if (node.getContext().getCommonTopics(message.getTags(), Long.MAX_VALUE) > 0 && node.getId() != message.getSource()) {
                        totalMessagesGenerated++;
                    }
                }
            }

            deliveryCost /= totalMessagesGenerated;
        } else {
            for (Node node : nodes) {
                deliveryCost += node.getMessagesExchanged();
            }

            deliveryCost /= messages.size();
        }

        return deliveryCost;
    }

    /**
     * Computes the overall delivery latency for a given list of messages.
     *
     * @param messages list of messages to compute the delivery latency for
     * @param nodes array of trace nodes
     * @param dissemination {@code true} if the messages were used for
     * disseminating data, {@code false} if they were employed for forwarding
     * @return the delivery latency (in seconds) for the given list of messages
     */
    public static double computeDeliveryLatency(List<Message> messages, Node[] nodes, boolean dissemination) {
        double deliveryLatency = 0.0;
        double deliveredRate = 0.0;

        if (dissemination) {
            // for dissemination messages, compute the total number of generated
            // messages as the sum of all nodes interested in each message
            for (Message message : messages) {
                for (Node node : nodes) {
                    if (node.getContext().getCommonTopics(message.getTags(), Long.MAX_VALUE) > 0
                            && node.getId() != message.getSource() && message.isDelivered(node.getId())) {
                        deliveryLatency += message.getLatency(node.getId());
                        deliveredRate++;
                    }
                }
            }

            deliveryLatency /= deliveredRate;
        } else {
            for (Message message : messages) {
                if (message.isDelivered(message.getDestination())) {
                    deliveryLatency += message.getLatency(message.getDestination());
                    deliveredRate++;
                }
            }

            deliveryLatency /= deliveredRate;
        }

        return deliveryLatency / 1000.0;
    }

    /**
     * Computes the overall hop count for a given list of messages.
     *
     * @param messages list of messages to compute the hop count for
     * @param nodes array of trace nodes
     * @param dissemination {@code true} if the messages were used for
     * disseminating data, {@code false} if they were employed for forwarding
     * @return the hop count for the given list of messages
     */
    public static double computeHopCount(List<Message> messages, Node[] nodes, boolean dissemination) {
        double hopCount = 0.0;
        double deliveredRate = 0.0;

        if (dissemination) {
            // for dissemination messages, compute the total number of generated
            // messages as the sum of all nodes interested in each message
            for (Message message : messages) {
                for (Node node : nodes) {
                    if (node.getContext().getCommonTopics(message.getTags(), Long.MAX_VALUE) > 0
                            && node.getId() != message.getSource() && message.isDelivered(node.getId())) {
                        hopCount += message.getHopCount(node.getId());
                        deliveredRate++;
                    }
                }
            }

            hopCount /= deliveredRate;
        } else {
            for (Message message : messages) {
                if (message.isDelivered(message.getDestination())) {
                    hopCount += message.getHopCount(message.getDestination());
                    deliveredRate++;
                }
            }

            hopCount /= deliveredRate;
        }

        return hopCount;
    }
}
