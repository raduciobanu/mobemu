/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package mobemu.node;

/**
 * Class for a device's battery.
 *
 * @author Radu
 */
public class Battery {

    private double currentLevel; // current battery level
    private long leftToRecharge; // duration until the device is fully recharged (in ticks)
    private boolean justDepleted; // specifies if the current device's battery has just depleted
    private static double maxLevel; // maximum battery level (i.e. when it is fully charged)
    private final double rechargeDuration; // duration of recharge (in ticks)
    private static double minBatteryThreshold; // threshold under which a node doesn't participate in the network any more
    private double decreaseRate; // the battery level decreases with this amount at every contact
    private static final int NOT_CHARGING = -1;

    /**
     * Creates a {@link Battery} object.
     *
     * @param currentLevel starting level of this device's battery; if set to a
     * negative value, the level is chosen randomly between 0 and
     * {@code maxLevel}; a battery's level is the duration it takes for the
     * battery to completely deplete, and is measured in trace ticks
     * @param maxLevel the maximum allowed battery level
     * @param rechargeDuration the duration it takes for this battery to
     * recharge (in ticks)
     * @param minBatteryThreshold minimum battery threshold
     */
    public Battery(double currentLevel, double maxLevel, double rechargeDuration, double minBatteryThreshold) {
        this.currentLevel = currentLevel;
        this.leftToRecharge = NOT_CHARGING;
        this.decreaseRate = 1.0;
        this.justDepleted = false;
        this.rechargeDuration = rechargeDuration;

        Battery.maxLevel = maxLevel;
        Battery.minBatteryThreshold = minBatteryThreshold;
    }

    /**
     * Gets the current battery level.
     *
     * @return the current battery level
     */
    public double getCurrentLevel() {
        return currentLevel;
    }

    /**
     * Sets the current battery level.
     *
     * @param currentLevel new battery level to be set
     */
    public void setCurrentLevel(double currentLevel) {
        this.currentLevel = currentLevel;
    }

    /**
     * Gets the current battery decrease rate.
     *
     * @return the current battery decrease rate
     */
    public double getDecreaseRate() {
        return decreaseRate;
    }

    /**
     * Sets the default battery decrease rate.
     */
    public void resetDecreaseRate() {
        decreaseRate = 1.0;
    }

    /**
     * Verifies if the device is currently charging.
     *
     * @return {@code true} if the device is charging, {@code false} otherwise
     */
    public boolean isCharging() {
        return leftToRecharge != NOT_CHARGING;
    }

    /**
     * Specifies if the current device's battery has just depleted.
     *
     * @return {@code true} if the battery has just depleted, {@code false}
     * otherwise
     */
    public boolean hasJustDepleted() {
        return justDepleted;
    }

    /**
     * Gets the maximum battery level.
     *
     * @return the maximum battery level
     */
    public static double getMaxLevel() {
        return maxLevel;
    }

    /**
     * Sets the current battery decrease rate (i.e. amount with which the
     * battery decreases at every trace tick).
     *
     * @param decreaseRate
     */
    public void setDecreaseRate(double decreaseRate) {
        this.decreaseRate = decreaseRate;
    }

    /**
     * Update the device's current battery level.
     */
    public void updateBatteryLevel() {
        if (leftToRecharge == NOT_CHARGING) {
            currentLevel -= decreaseRate;

            if (currentLevel <= 0) {
                currentLevel = 0;
                leftToRecharge = 0;
                justDepleted = true;
            }
        } else {
            if (justDepleted) {
                justDepleted = false;
            }

            leftToRecharge++;
            if (leftToRecharge >= rechargeDuration) {
                currentLevel = maxLevel;
                leftToRecharge = NOT_CHARGING;
            }
        }
    }

    /**
     * Checks whether the current node can participate in the network (i.e. if
     * the node's battery level is higher than the minimum battery threshold).
     *
     * @return {@code true} if the node can participate, {@code false} otherwise
     */
    public boolean canParticipate() {
        return currentLevel >= minBatteryThreshold;
    }
}
