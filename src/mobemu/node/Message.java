/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package mobemu.node;

import java.util.*;

/**
 * Class for a message exchanged between two nodes in an opportunistic network.
 *
 * @author Radu
 */
public class Message implements Comparable<Message> {

    protected int id; // message ID
    protected int source; // ID of source node
    protected int destination; // ID of destination node or -1 if transmission is publish/subscribe (i.e. dissemination)
    protected String message; // message content
    protected long timestamp; // time when the message was generated
    protected Context tags; // tags of this message
    protected double utility = 1.0; // utility of this message
    protected MessageStats stats; // message statistics
    protected static int messageCount = 0; // total number of messages generated
    public static final int DISSEMINATION_ID = -1;

    /**
     * Constructor for a routing {@link Message}.
     */
    public Message() {
    }

    /**
     * Constructor for a routing {@link Message}.
     *
     * @param source the sender of the message
     * @param destination the intended recipient of the message
     * @param message content of the message
     * @param timestamp time when the message was generated
     * @param copies number of copies this message starts with
     */
    public Message(int source, int destination, String message, long timestamp, int copies) {
        this.id = messageCount++;
        this.source = source;
        this.destination = destination;
        this.message = message;
        this.timestamp = timestamp;
        this.tags = new Context();
        this.stats = new MessageStats(copies, source);
    }

    /**
     * Constructor for a dissemination {@link Message}.
     *
     * @param source the sender of the message
     * @param tags tags for this message
     * @param message content of the message
     * @param timestamp time when the message was generated
     * @param copies number of copies this message starts with
     */
    public Message(int source, Context tags, String message, long timestamp, int copies) {
        this.id = messageCount++;
        this.source = source;
        this.destination = DISSEMINATION_ID;
        this.message = message;
        this.timestamp = timestamp;
        this.tags = tags;
        this.stats = new MessageStats(copies, source);
    }

    /**
     * Gets the message's ID.
     *
     * @return the message's ID
     */
    public int getId() {
        return id;
    }

    /**
     * Gets the message's source.
     *
     * @return the message's source
     */
    public int getSource() {
        return source;
    }

    /**
     * Gets the message's destination.
     *
     * @return the message's destination
     */
    public int getDestination() {
        return destination;
    }

    /**
     * Gets the message's tags.
     *
     * @return {@link Context} object with the message's tags
     */
    public Context getTags() {
        return tags;
    }

    /**
     * Gets the message's content.
     *
     * @return the context of this message
     */
    public String getMessage() {
        return message;
    }

    /**
     * Gets the message's timestamp (i.e. time of creation).
     *
     * @return the timestamp of this message
     */
    public long getTimestamp() {
        return timestamp;
    }

    /**
     * Gets the message's utility.
     *
     * @return the utility of this message
     */
    public double getUtility() {
        return utility;
    }

    /**
     * Gets the message's stats.
     *
     * @return the stats of this message
     */
    public MessageStats getStats() {
        return stats;
    }

    /**
     * Gets the delivery latency of this message at a given destination.
     *
     * @param id ID of the destination node
     * @return delivery latency
     */
    public long getLatency(int id) {
        return stats.getLatency(id);
    }

    /**
     * Gets the hop count of this message at a given destination.
     *
     * @param id ID of the destination node
     * @return hop count
     */
    public long getHopCount(int id) {
        return stats.getHopCount(id);
    }

    /**
     * Gets the number of copies of this message at a given node.
     *
     * @param id ID of the node
     * @return number of copies
     */
    public int getCopies(int id) {
        return stats.getCopies(id);
    }

    /**
     * Sets a new value for the number of copies of a message at a node.
     *
     * @param id ID of the node
     * @param value new value for copies
     */
    public void setCopies(int id, int value) {
        stats.setCopies(id, value);
    }

    /**
     * Checks whether the message has a given destination.
     *
     * @param destination the destination to be checked
     * @return {@code true} if the given destination is the message's
     * destination, {@code false} otherwise
     */
    public boolean hasDestination(int destination) {
        return this.destination == destination;
    }

    /**
     * Mark a message as delivered for a given node at a certain time.
     *
     * @param nodeId the ID of the node the message was delivered to
     * @param tick the current trace tick
     */
    public void markAsDelivered(int nodeId, long tick) {
        stats.markAsDelivered(nodeId, tick - timestamp);
    }

    /**
     * Increases the hop count for this message towards a given node.
     *
     * @param id ID of the node
     * @return new hop count value
     */
    public int increaseHopCount(int id) {
        return stats.increaseHopCount(id);
    }

    /**
     * Checks whether this message has been delivered to a given node.
     *
     * @param nodeId the ID of the node
     * @return {@code true} if the message was delivered to the node, {@code false}
     * otherwise
     */
    public boolean isDelivered(int nodeId) {
        return stats.isDelivered(nodeId);
    }

    /**
     * Deletes all copies of this message at the given node.
     *
     * @param nodeId ID of the node from which the message is deleted
     */
    public void deleteCopies(int nodeId) {
        stats.deleteCopies(nodeId);
    }

    /**
     * Duplicates the copies of a message from a given node to another one.
     *
     * @param from source node ID
     * @param to destination node ID
     */
    public void copy(int from, int to) {
        stats.copy(from, to);
    }

    /**
     * Sets the utility of a message.
     *
     * @param utility utility value to be set
     */
    public void setUtility(double utility) {
        this.utility = utility;
    }

    @Override
    public int compareTo(Message message) {
        if (this.utility > message.utility) {
            return -1;
        }

        if (this.utility < message.utility) {
            return 1;
        }

        return 0;
    }

    /**
     * Generate the time a message should be sent based on a given random value,
     * with the highest generation probability in the interval where most
     * contacts are likely to happen.
     *
     * @param value value between 0 and 1 to be used as interval selection
     * probability
     * @return a date when a new message should be generated
     */
    public static Calendar generateMessageTime(double value) {
        Calendar time = Calendar.getInstance();
        int hour;

        // note: these values are taken from the UPB 2012 trace
        if (value < 0.0169) {
            hour = 8;
        } else if (value < 0.1106) {
            hour = 10;
        } else if (value < 0.2524) {
            hour = 12;
        } else if (value < 0.5708) {
            hour = 14;
        } else if (value < 0.9467) {
            hour = 16;
        } else {
            hour = 18;
        }

        time.set(Calendar.HOUR_OF_DAY, hour);
        time.set(Calendar.MINUTE, 0);

        return time;
    }

    /**
     * Generates messages to be sent in the network. If dissemination is
     * selected, messages are tagged with a random topic from the areas of
     * interest of the generating node (i.e. a node can only mark messages with
     * tags it is interested in). If routing is selected, the message's
     * destination is selected using a Zipf distribution, with the highest
     * probability being reserved for nodes in the social and discovered network
     * of the generating node, and the lowest probability for unkown nodes.
     *
     * @param nodes array of nodes
     * @param messageCount number of messages to be generated
     * @param messageCopies number of copies of each generated message
     * @param tick current tick of the trace
     * @param dissemination {@code true} if the generated messages are for
     * dissemination, {@code false} if they are for routing
     * @param random random number generator
     * @return list of messages generated
     */
    public static List<Message> generateMessages(Node[] nodes, int messageCount, int messageCopies, long tick, boolean dissemination, Random random) {
        int nodeCount = nodes.length;
        List<Message> result = new ArrayList<>();

        // if data is being disseminated, each node generates data that belongs to one of its interest topics
        if (dissemination) {
            for (int i = 0; i < nodeCount; i++) {
                for (int j = 0; j < messageCount; j++) {
                    Set<Topic> topics = nodes[i].getContext().getTopics();

                    if (Topic.hasTopics(topics, tick)) {
                        int index = random.nextInt(Topic.getTopicsSize(topics, tick));
                        Topic topic = Topic.getTopicAt(topics, tick, index);

                        if (topic != null) {
                            Context messageContext = new Context();
                            messageContext.addTopic(topic);
                            result.add(nodes[i].generateMessage(new Message(i, messageContext, "", tick, messageCopies)));
                        }
                    }
                }
            }

            return result;
        }

        // for routing, use a Zipf distribution
        for (int i = 0; i < nodeCount; i++) {
            List<Integer> socialNetworkNodes = new ArrayList<>();
            List<Integer> kcliqueNodes = new ArrayList<>();
            List<Integer> commonNodes = new ArrayList<>();

            for (int nodeId = 0; nodeId < nodeCount; nodeId++) {
                if (nodeId == i) {
                    continue;
                }

                boolean social = false;
                boolean kclique = false;

                if (nodes[i].inSocialNetwork(nodeId)) {
                    socialNetworkNodes.add(nodeId);
                    social = true;
                }

                if (nodes[i].inLocalCommunity(nodeId)) {
                    kcliqueNodes.add(nodeId);
                    kclique = true;
                }

                if (social && kclique) {
                    commonNodes.add(nodeId);
                }
            }

            // generate destination according to a Zipf distribution
            for (int j = 0; j < messageCount; j++) {
                int zipf = zipfDistribution(random.nextDouble());
                int destination;

                if (zipf == 0 && !commonNodes.isEmpty()) { // destination both in the social network and in the discovered network
                    destination = random.nextInt(commonNodes.size());
                } else if (zipf == 1 && !socialNetworkNodes.isEmpty()) { // destination in the social network
                    destination = random.nextInt(socialNetworkNodes.size());
                } else if (zipf == 2 && !kcliqueNodes.isEmpty()) { // destination in the discovered network
                    destination = random.nextInt(kcliqueNodes.size());
                } else { // random destination
                    destination = random.nextInt(nodeCount);
                    while (destination == i) {
                        destination = random.nextInt(nodeCount);
                    }
                }

                result.add(nodes[i].generateMessage(new Message(i, destination, "", tick, messageCopies)));
            }
        }

        return result;
    }

    /**
     * Generates a Zipf distribution.
     *
     * @double value random value for the distribution
     *
     * @return Zipf value for the given index
     */
    private static int zipfDistribution(double value) {
        int zipfExponent = 1;
        int zipfSize = 4;
        double sum = 0;

        for (int i = 1; i <= zipfSize; i++) {
            double up = 1.0 / Math.pow(i, zipfExponent);
            double down = 0;

            for (int j = 1; j <= zipfSize; j++) {
                down += 1 / Math.pow(j, zipfExponent);
            }

            sum += up / down;

            if (value < sum) {
                return i - 1;
            }
        }

        return 0;
    }
}
