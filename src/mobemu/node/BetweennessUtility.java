package mobemu.node;

public class BetweennessUtility {
	
	private int[] betweennessUtilities;
	private double[] lastThresholds;
	private static int timeWindow;
	
	public BetweennessUtility(int nodes) {
		betweennessUtilities = new int[nodes];
		lastThresholds = new double[nodes];
		for (int i = 0; i < nodes; i++) {
			betweennessUtilities[i] = 0;
			lastThresholds[i] = 0.0;
		}
		timeWindow = 7 * 24 * 3600 * 1000; // default is one week
	}
	
    /**
     * Sets a new time window value for betweenness computation.
     *
     * @param newTimeWindow new time window value
     */
    public static void setTimeWindow(int newTimeWindow) {
        timeWindow = newTimeWindow;
    }

    /**
     * Returns the time window.
     *
     * @return time window for computing the betweenness
     */
    public static int getTimeWindow() {
        return timeWindow;
    }

    /**
     * Returns the last threshold.
     *
     * @return the last threshold for the betweenness computation
     */
    public double getLastThreshold(int id) {
        return lastThresholds[id];
    }

    /**
     * Increases the last threshold for betweenness computation.
     *
     * @return the new value of the last threshold
     */
    public double increaseLastThreshold(int id) {
        return ++lastThresholds[id];
    }
    
    public double setLastThreshold(int id, double value) {
        lastThresholds[id] = value;
        return lastThresholds[id];
    }
    
    /**
     * Gets the value for the given destination.
     * 
     * @param dest destination
     * @return current value of the desired betweenness utility
     */
    public int getValue(int id) {
    	return betweennessUtilities[id];
    }
    
    /**
     * Sets a new value for the given destination.
     * 
     * @param dest destination
     * @param value new value to be set
     */
    public void setValue(int id, int value) {
    	betweennessUtilities[id] = value;
    }

    /**
     * Increases the value for the given destination.
     *
     * @param dest destination
     */
    public void increaseValue(int dest) {
    	betweennessUtilities[dest]++;
    }
}
