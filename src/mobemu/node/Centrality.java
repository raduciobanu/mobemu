/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package mobemu.node;

/**
 * Class for representing an opportunistic node's centrality information.
 *
 * @author Radu
 */
public class Centrality {

    private double current;
    private double previous;
    private double cumulated;
    private static double lastThreshold;
    private static int timeWindow;

    private static double MAX_CURRENT;
    private static double MAX_PREVIOUS;
    private static double MAX_CUMULATED;

    /**
     * Instantiates a {@code Centrality} object.
     */
    public Centrality() {
        this.current = 0;
        this.previous = 0;
        this.cumulated = 0;
        lastThreshold = 0;
        timeWindow = 21600; // default is six hours
    }

    /**
     * Sets a new time window value for centrality computation.
     *
     * @param newTimeWindow new time window value
     */
    public static void setTimeWindow(int newTimeWindow) {
        timeWindow = newTimeWindow;
    }

    /**
     * Returns the time window.
     *
     * @return time window for computing the centrality
     */
    public static int getTimeWindow() {
        return timeWindow;
    }

    /**
     * Returns the last threshold.
     *
     * @return the last threshold for the centrality computation
     */
    public static double getLastThreshold() {
        return lastThreshold;
    }

    /**
     * Increases the last threshold for centrality computation.
     *
     * @return the new value of the last threshold
     */
    public static double increaseLastThreshold() {
        return ++lastThreshold;
    }

    /**
     * Sets a new value for the given centrality type (cumulated, current,
     * previous).
     *
     * @param type type of centrality to be changed
     * @param value new value to be set
     */
    public void setValue(CentralityValue type, double value) {
        switch (type) {
            case CUMULATED:
                cumulated = value;
                if(cumulated > MAX_CUMULATED){
                    MAX_CUMULATED = cumulated;
                }
                break;
            case CURRENT:
                current = value;
                if(current > MAX_CURRENT){
                    MAX_CURRENT = current;
                }
                break;
            case PREVIOUS:
                previous = value;
                if(previous > MAX_PREVIOUS){
                    MAX_PREVIOUS = previous;
                }
                break;
            default:
                break;
        }
    }

    /**
     * Increases the value for the given centrality type (cumulated, current,
     * previous).
     *
     * @param type type of centrality to be increased
     */
    public void increaseValue(CentralityValue type) {
        switch (type) {
            case CUMULATED:
                cumulated++;
                if(cumulated > MAX_CUMULATED){
                    MAX_CUMULATED = cumulated;
                }
                break;
            case CURRENT:
                current++;
                if(current > MAX_CURRENT){
                    MAX_CURRENT = current;
                }
                break;
            case PREVIOUS:
                previous++;
                if(previous > MAX_PREVIOUS){
                    MAX_PREVIOUS = previous;
                }
                break;
            default:
                break;
        }
    }

    /**
     * Gets the value for the given centrality type (cumulated, current,
     * previous).
     *
     * @param type type of centrality to be returned
     * @return current value of the desired centrality
     */
    public double getValue(CentralityValue type) {
        switch (type) {
            case CUMULATED:
                return cumulated;
            case CURRENT:
                return current;
            case PREVIOUS:
                return previous;
            default:
                return Double.MAX_VALUE;
        }
    }

    /**
     * Get the normalized value for the given centrality type (cumulated, current,
     * previous).
     * @param type type of centrality to be returned
     * @return normalized current value of the desired centrality (between 0 and 1)
     */
    public double getNormalizedValue(CentralityValue type){
        switch (type) {
            case CUMULATED:
                if(MAX_CUMULATED == 0d)
                    return cumulated;

                return cumulated / MAX_CUMULATED;
            case CURRENT:
                if(MAX_CURRENT == 0d)
                    return current ;

                return current / MAX_CURRENT;
            case PREVIOUS:
                if(MAX_PREVIOUS == 0d)
                    return previous;

                return previous / MAX_PREVIOUS;
            default:
                return Double.MAX_VALUE;
        }
    }

    /**
     * Helper class for a centrality type.
     */
    public static enum CentralityValue {

        CURRENT, PREVIOUS, CUMULATED
    };
}
