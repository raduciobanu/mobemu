/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package mobemu.node;

/**
 * Class for message exchange information.
 *
 * @author Radu
 */
public class ExchangeHistory implements Comparable<ExchangeHistory> {

    private long exchangeTime;
    private Message message;
    private int nodeSeen;
    private int node;
    private double batteryNodeSeen;

    /**
     * Creates an ExchangeHistory object.
     *
     * @param exchangeTime time of the exchange
     * @param message the message that was exchanged
     * @param nodeSeen the encountered node
     * @param node the current node
     * @param batteryNodeSeen the battery level of the encountered node
     */
    public ExchangeHistory(long exchangeTime, Message message, int nodeSeen, int node, double batteryNodeSeen) {
        this.exchangeTime = exchangeTime;
        this.message = message;
        this.nodeSeen = nodeSeen;
        this.node = node;
        this.batteryNodeSeen = batteryNodeSeen;
    }

    /**
     * Gets the node seen.
     *
     * @return the ID of the seen node
     */
    public int getNodeSeen() {
        return nodeSeen;
    }
    
    /**
     * Gets the current node.
     *
     * @return the ID of the current node
     */
    public int getNode() {
        return node;
    }

    /**
     * Gets the message.
     *
     * @return the ID of the message
     */
    public Message getMessage() {
        return message;
    }

    /**
     * Gets the exchange time.
     *
     * @return the exchange time
     */
    public long getExchangeTime() {
        return exchangeTime;
    }

    /**
     * Gets the battery level of the encountered node.
     *
     * @return battery level
     */
    public double getBattery() {
        return batteryNodeSeen;
    }

    @Override
    public int compareTo(ExchangeHistory o) {
        long diff = this.exchangeTime - o.exchangeTime;

        if (diff < 0) {
            return 1;
        } else if (diff > 0) {
            return -1;
        } else {
            return 0;
        }
    }
}
