/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package mobemu.trace;

/**
 * Class for a contact between two mobile node in an opportunistic network.
 *
 * @author Radu
 */
public class Contact implements Comparable<Contact> {

    private int observer;
    private int observed;
    private long start;
    private long end;
    private boolean type; // true - WiFi, false - Bluetooth

    /**
     * Constructor for the {@link Contact} class.
     *
     * @param observer ID of the node that observed the contact
     * @param observed ID of the node that was observed in the contact
     * @param start start time of the contact
     * @param end end time of the contact
     */
    public Contact(int observer, int observed, long start, long end) {
        this.observer = observer;
        this.observed = observed;
        this.start = start;
        this.end = end;
        this.type = false;
    }

    /**
     * Constructor for the {@link Contact} class.
     *
     * @param observer ID of the node that observed the contact
     * @param observed ID of the node that was observed in the contact
     * @param start start time of the contact
     * @param end end time of the contact
     * @param type type of contact ({@code true} for WiFi, {@code false} for
     * Bluetooth)
     */
    public Contact(int observer, int observed, long start, long end, boolean type) {
        this.observer = observer;
        this.observed = observed;
        this.start = start;
        this.end = end;
        this.type = type;
    }

    /**
     * Checks if there is a contact between the current node and another node at
     * a given time.
     *
     * @param time contact time
     * @return {@code true} if there is a contact, {@code false} otherwise
     */
    public boolean hasContactAt(int time) {
        return start <= time && time <= end;
    }

    /**
     * Returns the observer node for this contact.
     *
     * @return the observer node ID
     */
    public int getObserver() {
        return observer;
    }

    /**
     * Returns the observed node for this contact.
     *
     * @return the observed node ID
     */
    public int getObserved() {
        return observed;
    }

    /**
     * Returns the start time of this contact.
     *
     * @return the start time
     */
    public long getStart() {
        return start;
    }

    /**
     * Returns the end time of this contact.
     *
     * @return the end time
     */
    public long getEnd() {
        return end;
    }

    /**
     * Returns the type of this contact.
     *
     * @return the type
     */
    public boolean getType() {
        return type;
    }

    /**
     * Sets the observed node of this contact.
     *
     * @param observed the observed ID to be set
     */
    public void setObserved(int observed) {
        this.observed = observed;
    }

    /**
     * Sets the observer node of this contact.
     *
     * @param observer the observer ID to be set
     */
    public void setObserver(int observer) {
        this.observer = observer;
    }

    /**
     * Sets the start time of this contact.
     *
     * @param start start time to be set
     */
    public void setStart(long start) {
        this.start = start;
    }

    /**
     * Sets the end time of this contact.
     *
     * @param end end time to be set
     */
    public void setEnd(long end) {
        this.end = end;
    }

    /**
     * Sets the type of this contact.
     *
     * @param type type to be set
     */
    public void setType(boolean type) {
        this.type = type;
    }

    /**
     * Compares two {@link Contact} objects by start time.
     *
     * @param contact {@link Contact} object to compare
     * @return {@code -1} if current {@link Contact} starts first, {@code 0} if
     * the two contacts start at once, {@code 1} if the second
     * {@link Contact} starts first
     */
    @Override
    public int compareTo(Contact contact) {
        long diff = this.start - contact.start;

        if (diff > 0) {
            return 1;
        } else if (diff < 0) {
            return -1;
        } else {
            return 0;
        }
    }
}
