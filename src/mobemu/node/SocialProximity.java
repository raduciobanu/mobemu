package mobemu.node;


/**
 * Class for representing an opportunistic node's social proximity information.
 *
 */
public class SocialProximity {

    private double[] current;
    private double[] previous;
    private double[] cumulated;
    private double[] encounters;
    private static double lastThreshold;
    private static int timeWindow;

    /**
     * Instantiates a {@code SocialProximity} object.
     */
    public SocialProximity(int nodes) {
        this.current = new double[nodes];
        this.previous = new double[nodes];
        this.cumulated = new double[nodes];
        this.encounters = new double[nodes];
        lastThreshold = 0;
        timeWindow = 21600; // default is six hours
    }

    /**
     * Sets a new time window value for social proximity computation.
     *
     * @param newTimeWindow new time window value
     */
    public static void setTimeWindow(int newTimeWindow) {
        timeWindow = newTimeWindow;
    }

    /**
     * Returns the time window.
     *
     * @return time window for computing the social proximity
     */
    public static int getTimeWindow() {
        return timeWindow;
    }

    /**
     * Returns the last threshold.
     *
     * @return the last threshold for the social proximity computation
     */
    public static double getLastThreshold() {
        return lastThreshold;
    }

    /**
     * Increases the last threshold for social proximity computation.
     *
     * @return the new value of the last threshold
     */
    public static double increaseLastThreshold() {
        return ++lastThreshold;
    }

    /**
     * Sets a new value for the given social proximity type (cumulated, current,
     * previous) with respect to a given node.
     *
     * @param type type of social proximity to be changed
     * @param node the id of the node who's social proximity is set against
     * @param value new value to be set
     */
    public void setValue(CentralityValue type, int node, double value) {
        switch (type) {
            case CUMULATED:
                cumulated[node] = value;
                break;
            case CURRENT:
                current[node] = value;
                break;
            case PREVIOUS:
                previous[node] = value;
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
                break;
            case CURRENT:
                current++;
                break;
            case PREVIOUS:
                previous++;
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
     * Helper class for a centrality type.
     */
    public static enum CentralityValue {

        CURRENT, PREVIOUS, CUMULATED
    };
}
