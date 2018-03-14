package mobemu.node;


/**
 * Class for representing an opportunistic node's social proximity information.
 *
 */
public class SocialProximity {
	
    private double[] currentContactDurations;
    private double[] previousContactDurations;
    private double[] cumulatedContactDurations;
    private int[] contactFrequencies;
    private static double lastThreshold;
    private static int timeWindow;

    /**
     * Instantiates a {@code SocialProximity} object.
     */
    public SocialProximity(int nodes) {
        this.currentContactDurations = new double[nodes];
        this.previousContactDurations = new double[nodes];
        this.cumulatedContactDurations = new double[nodes];
        this.contactFrequencies = new int[nodes];
        lastThreshold = 0;
        timeWindow = 21600 * 1000; // default is six hours
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
    
    public void setFrequency(int node, int value) {
    	contactFrequencies[node] = value;
    }
    
    public void increaseFrequency(int node) {
    	contactFrequencies[node] += 1;
    }
    
    public int getFrequency(int node) {
    	return contactFrequencies[node];
    }

    /**
     * Sets a new value for the given contact type (cumulated, current,
     * previous) with respect to a given node.
     *
     * @param type type of contact to be changed
     * @param node the id of the node who's contact duration is set against
     * @param value new value to be set
     */
    public void setContactDurationValue(ContactType type, int node, double value) {
        switch (type) {
            case CUMULATED:
            	cumulatedContactDurations[node] = value;
                break;
            case CURRENT:
            	currentContactDurations[node] = value;
                break;
            case PREVIOUS:
            	previousContactDurations[node] = value;
                break;
            default:
                break;
        }
    }

    /**
     * Increases the contact duration for the contact type (cumulated, current,
     * previous) for a given node.
     *
     * @param type type of contact to be changed
     * @param node the id of the node who's contact duration is increased
     * @param value value with which the duration is increased
     */
    public void increaseContactDurationValue(ContactType type, int node, double value) {
        switch (type) {
            case CUMULATED:
            	cumulatedContactDurations[node] += value;
                break;
            case CURRENT:
            	currentContactDurations[node] += value;
                break;
            case PREVIOUS:
            	previousContactDurations[node] += value;
                break;
            default:
                break;
        }
    }

    /**
     * Gets the total duration for the given contact type (cumulated, current,
     * previous) for a given node.
     *
     * @param type type of contact duration to be returned
     * @return current value of the desired contact duration
     */
    public double getContactDurationValue(ContactType type, int node) {
        switch (type) {
            case CUMULATED:
                return cumulatedContactDurations[node];
            case CURRENT:
                return currentContactDurations[node];
            case PREVIOUS:
                return previousContactDurations[node];
            default:
                return Double.MAX_VALUE;
        }
    }

    /**
     * Helper class for a centrality type.
     */
    public static enum ContactType {

        CURRENT, PREVIOUS, CUMULATED
    };
}
