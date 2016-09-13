/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package mobemu.node;

/**
 * Class for representing an opportunistic node's network capabilities.
 *
 * @author Radu
 */
public class Network {

    private double transferSpeed; // node transfer speed (in messages per tick)
    public static final double UNLIMITED_TRANSFER_SPEED = Double.MAX_VALUE;

    /**
     * Instantiates a {@code Network} object.
     *
     * @param transferSpeed transfer speed of the current node
     */
    public Network(double transferSpeed) {
        this.transferSpeed = transferSpeed;
    }

    /**
     * Instantiates a {@code Network} object with unlimited transfer speed.
     */
    public Network() {
        this.transferSpeed = UNLIMITED_TRANSFER_SPEED;
    }

    /**
     * Computes the number of messages that this node can receive during a
     * contact.
     *
     * @param contactDuration contact duration in ticks
     * @return the number of messages that this node can receive during a
     * contact
     */
    public int computeMaxMessages(long contactDuration) {
        return (transferSpeed == UNLIMITED_TRANSFER_SPEED) ? Integer.MAX_VALUE : (int) (transferSpeed * (float) contactDuration + 1);
    }
}
