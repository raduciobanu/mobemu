package mobemu.node.consensus;

/**
 * Created by radu on 5/6/2017.
 */
public class ConsensusMostFrequentValue {
    /**
     * The most frequent value found
     */
    private String value;

    /**
     * The percent of value appearances out of all values
     */
    private double confidenceLevel;


    public ConsensusMostFrequentValue(String value, double confidenceLevel) {
        this.value = value;
        this.confidenceLevel = confidenceLevel;
    }

    public String getValue() {
        return value;
    }

    public double getConfidenceLevel() {
        return confidenceLevel;
    }
}
