/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package mobemu.node;

/**
 * Class containing statistics about a data exchange.
 *
 * @author Radu
 */
public class ExchangeStats {

    private long lastExchangeTime;
    private long lastExchangeDuration;

    /**
     * Instantiates an {@link ExchangeStats} object.
     *
     * @param lastExchangeTime the time when the last exchange occurred
     * @param lastExchangeDuration the duration of the last exchange
     */
    public ExchangeStats(long lastExchangeTime, long lastExchangeDuration) {
        this.lastExchangeTime = lastExchangeTime;
        this.lastExchangeDuration = lastExchangeDuration;
    }

    /**
     * Instantiates an {@link ExchangeStats} object.
     */
    public ExchangeStats() {
        this(0, 0);
    }

    /**
     * Returns a node's last exchange time.
     *
     * @return the last exchange time
     */
    public long getLastExchangeTime() {
        return lastExchangeTime;
    }

    /**
     * Sets a node's last exchange time.
     *
     * @param lastExchangeTime the last exchange time to set
     */
    public void setLastExchangeTime(long lastExchangeTime) {
        this.lastExchangeTime = lastExchangeTime;
    }

    /**
     * Returns a node's last exchange duration.
     *
     * @return the last exchange duration
     */
    public long getLastExchangeDuration() {
        return lastExchangeDuration;
    }

    /**
     * Sets a node's last exchange duration.
     *
     * @param lastExchangeDuration the last exchange duration to set
     */
    public void setLastExchangeDuration(long lastExchangeDuration) {
        this.lastExchangeDuration = lastExchangeDuration;
    }
}
