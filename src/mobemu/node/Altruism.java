/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package mobemu.node;

/**
 * Class for representing an opportunistic node's altruism.
 *
 * @author Radu
 */
public class Altruism {

    private boolean selfish; // other nodes' perception about this node
    private double local; // altruism value for local nodes
    private double global; // altruism value for non-local nodes
    private double[] perceived; // perceived altruism values for the other nodes
    private static double maxBatteryThreshold; // threshold under which a node is considered too low on battery
    private static double perceivedBehaviorConstant; // amount that is added or subtracted when a node seems to become more or less altruistic
    private static double behaviorConstant; // amount that is added or subtracted when a node becomes more or less altruistic
    private static double trustThreshold; // nodes with trust values over this threshold are considered altruistic
    private static final int ALTRUISM_UNKNOWN = 50; // default initial altruism value for non-community nodes
    private static final int ALTRUISM_COMMUNITY = 100; // default initial altruism value for community nodes

    /**
     * Instantiates an {@link Altruism} object.
     *
     * @param local value of altruism for local nodes
     * @param global value of altruism for non-local nodes
     * @param isConnected array of booleans for socially-connected nodes
     * @param altruismUnknown default altruism values for non-community nodes
     * @param altruismCommunity default altruism values for community nodes
     */
    public Altruism(double local, double global, boolean[] isConnected, int altruismUnknown, int altruismCommunity) {
        this.selfish = false;
        this.local = local;
        this.global = global;
        this.perceived = new double[isConnected.length];
        for (int i = 0; i < perceived.length; i++) {
            this.perceived[i] = isConnected[i] ? altruismCommunity : altruismUnknown;
        }
        maxBatteryThreshold = 0.25;
        behaviorConstant = 0.1;
        perceivedBehaviorConstant = 50;
        trustThreshold = 0.8;
    }

    /**
     * Instantiates an {@link Altruism} object.
     *
     * @param local value of altruism for local nodes
     * @param global value of altruism for non-local nodes
     * @param isConnected array of booleans for socially-connected nodes
     */
    public Altruism(double local, double global, boolean[] isConnected) {
        this(local, global, isConnected, ALTRUISM_UNKNOWN, ALTRUISM_COMMUNITY);
    }

    /**
     * Gets the value of the local altruism.
     *
     * @return the local altruism value
     */
    public double getLocal() {
        return local;
    }

    /**
     * Gets the value of the global altruism.
     *
     * @return the global altruism value
     */
    public double getGlobal() {
        return global;
    }

    /**
     * Gets the perceived altruism for a given node.
     *
     * @param id ID of the node whose perceived altruism is requested
     * @return perceived altruism value for the given node
     */
    public double getPerceived(int id) {
        return perceived[id];
    }

    /**
     * Gets the behavior constant (i.e. amount that is added or subtracted when
     * a node becomes more or less altruistic).
     *
     * @return the behavior constant
     */
    public static double getBehaviorConstant() {
        return behaviorConstant;
    }

    /**
     * Gets the perceived behavior constant (i.e. amount that is added or
     * subtracted to another node's perception of a node when it seems to become
     * more or less altruistic).
     *
     * @return the behavior constant
     */
    public static double getPerceivedBehaviorConstant() {
        return perceivedBehaviorConstant;
    }

    /**
     * Increases the local altruism value.
     *
     * @return new local altruism value
     */
    public double increaseLocal() {
        local += behaviorConstant;
        return local;
    }

    /**
     * Sets a new value for the local altruism.
     *
     * @param value new value for the local altruism
     */
    public void setLocal(double value) {
        local = value;
    }

    /**
     * Increases the global altruism value with the behavior constant.
     *
     * @return new global altruism value
     */
    public double increaseGlobal() {
        global += behaviorConstant;
        return global;
    }

    /**
     * Checks if a node is selfish.
     *
     * @return {@code true} if the node is selfish, {@code false} otherwise
     */
    public boolean isSelfish() {
        return selfish;
    }

    /**
     * Sets a node's selfishness.
     *
     * @param selfish {@code true} if the node is selfish, {@code false}
     * otherwise
     */
    public void setSelfishness(boolean selfish) {
        this.selfish = selfish;
    }

    /**
     * Sets the perceived altruism for a given node.
     *
     * @param id ID of the node the perceived altruism is being set for
     * @param value value of the perceived altruism
     */
    public void setPerceived(int id, double value) {
        this.perceived[id] = value;
    }

    /**
     * Increases the perceived altruism for a given node with the perceived
     * behavior constant.
     *
     * @param id ID of the node the perceived altruism is being increased for
     */
    public void increasePerceived(int id) {
        this.perceived[id] += perceivedBehaviorConstant;
    }

    /**
     * Decreases the perceived altruism for a given node with the perceived
     * behavior constant.
     *
     * @param id ID of the node the perceived altruism is being decreased for
     */
    public void decreasePerceived(int id) {
        this.perceived[id] -= perceivedBehaviorConstant;
    }

    /**
     * Gets the threshold under which a node is considered too low on battery.
     *
     * @return the maximum battery threshold
     */
    public static double getMaxBatteryThreshold() {
        return maxBatteryThreshold;
    }

    /**
     * Gets the trust threshold (nodes with trust values over this threshold are
     * considered altruistic)
     *
     * @return trust threshold
     */
    public static double getTrustThreshold() {
        return trustThreshold;
    }

    /**
     * Sets the behavior constant.
     *
     * @param behaviorConstant the new value of the behavior constant
     */
    public static void setBehaviorConstant(double behaviorConstant) {
        Altruism.behaviorConstant = behaviorConstant;
    }

    /**
     * Sets the perceived behavior constant.
     *
     * @param perceivedBehaviorConstant the new value of the perceived behavior
     * constant
     */
    public static void setPerceivedBehaviorConstant(double perceivedBehaviorConstant) {
        Altruism.perceivedBehaviorConstant = perceivedBehaviorConstant;
    }
}
