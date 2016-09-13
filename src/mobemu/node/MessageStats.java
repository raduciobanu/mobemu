/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package mobemu.node;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Class for opportunistic message statistics.
 *
 * @author Radu
 */
public class MessageStats {

    private Map<Integer, Integer> hops; // number of hops until each destination
    private Set<Integer> delivered; // list of nodes the message has been delivered to
    private Map<Integer, Long> latency; // delivery latency for each destination
    private Map<Integer, Integer> copies; // number of copies of this message for each node

    /**
     * Initializes a {@code MessageStats} object.
     *
     * @param copies number of initial copies
     * @param id ID of the node that generates the message
     */
    public MessageStats(int copies, int id) {
        this.copies = new HashMap<>();
        this.copies.put(id, copies);
        this.hops = new HashMap<>();
        this.delivered = new HashSet<>();
        this.latency = new HashMap<>();
    }

    /**
     * Gets the number of copies of this message at a given node.
     *
     * @param id ID of the node
     * @return number of copies
     */
    public int getCopies(int id) {
        Integer result = copies.get(id);
        return result == null ? 0 : result;
    }

    /**
     * Sets a new value for the number of copies of a message at a node.
     *
     * @param id ID of the node
     * @param value new value for copies
     */
    public void setCopies(int id, int value) {
        copies.put(id, value);
    }

    /**
     * Deletes all copies of this message at the given node.
     *
     * @param id ID of the node from which the message is deleted
     */
    public void deleteCopies(int id) {
        if (copies.containsKey(id)) {
            copies.put(id, 0);
        }
    }

    /**
     * Duplicates the copies of a message from a given node to another one.
     *
     * @param from source node ID
     * @param to destination node ID
     */
    public void copy(int from, int to) {
        copies.put(to, copies.get(from));
    }

    /**
     * Marks a message as delivered to a given node.
     *
     * @param id the ID of the node this message was delivered to
     * @param deliveryLatency the delivery latency of this message for the given
     * node
     */
    public void markAsDelivered(int id, long deliveryLatency) {
        delivered.add(id);
        increaseHopCount(id);
        latency.put(id, deliveryLatency);
    }

    /**
     * Increases the hop count for this message towards a given node.
     *
     * @param id ID of the node
     * @return new hop count value
     */
    public int increaseHopCount(int id) {
        if (delivered.contains(id)) {
            Integer hopsCount = hops.get(id);
            if (hopsCount == null) {
                hops.put(id, 0);
                return 0;
            }

            return hopsCount;
        }

        Integer hopsCount = hops.get(id);
        if (hopsCount == null) {
            hopsCount = 0;
        }
        hopsCount++;
        hops.put(id, hopsCount);

        return hopsCount;
    }

    /**
     * Gets the latency of a message delivered at the given destination.
     *
     * @param id ID of the destination node
     * @return the latency of the message
     */
    public long getLatency(int id) {
        Long result = latency.get(id);
        return result == null ? -1 : result;
    }

    /**
     * Gets the hop count of a message delivered at the given destination.
     *
     * @param id ID of the destination node
     * @return the hop count of the message
     */
    public int getHopCount(int id) {
        Integer result = hops.get(id);
        return result == null ? -1 : result;
    }

    /**
     * Checks whether a message has been delivered to a given node.
     *
     * @param id the ID of the node
     * @return {@code true} if the message was delivered to the node, {@code false}
     * otherwise
     */
    public boolean isDelivered(int id) {
        return delivered.contains(id);
    }
}
