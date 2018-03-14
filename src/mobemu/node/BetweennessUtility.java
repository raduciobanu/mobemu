package mobemu.node;

public class BetweennessUtility {
	
	int[] betweennessUtilities;
	private static double lastThreshold;
	private static int timeWindow;
	
	public BetweennessUtility(int nodes) {
		betweennessUtilities = new int[nodes];
		for (int i = 0; i < nodes; i++)
			betweennessUtilities[i] = 0;
		lastThreshold = 0;
		timeWindow = 21600; // default is six hours
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
    public static double getLastThreshold() {
        return lastThreshold;
    }

    /**
     * Increases the last threshold for betweenness computation.
     *
     * @return the new value of the last threshold
     */
    public static double increaseLastThreshold() {
        return ++lastThreshold;
    }
    
    /**
     * Sets a new value for the given destination.
     * 
     * @param dest destination
     * @param value new value to be set
     */
    public void setValue(int dest, int value) {
    	betweennessUtilities[dest] = value;
    }

    /**
     * Increases the value for the given destination.
     *
     * @param dest destination
     */
    public void increaseValue(int dest) {
    	betweennessUtilities[dest]++;
    }

    /**
     * Gets the value for the given destination.
     * 
     * @param dest destination
     * @return current value of the desired betweenness utility
     */
    public int getValue(int dest) {
    	return betweennessUtilities[dest];
    }
}
