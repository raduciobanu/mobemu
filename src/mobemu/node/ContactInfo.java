/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package mobemu.node;

/**
 * Class for mobile node contact information in an opportunistic network.
 *
 * @author Radu
 */
public class ContactInfo {

    private long duration; // duration of this contact
    private int contacts = 0; // number of contacts with the current node

    /**
     * Instantiates a {@code ContactInfo} object.
     */
    public ContactInfo() {
        this.duration = 0;
        this.contacts = 0;
    }

    /**
     * Instantiates a {@code ContactInfo} object.
     *
     * @param duration duration of the contact
     * @param contacts number of contacts
     */
    public ContactInfo(long duration, int contacts) {
        this.duration = duration;
        this.contacts = contacts;
    }

    /**
     * Gets the number of contacts.
     *
     * @return the number of contacts
     */
    public int getContacts() {
        return contacts;
    }

    /**
     * Gets the duration of the contacts.
     *
     * @return the duration of the contacts
     */
    public long getDuration() {
        return duration;
    }

    /**
     * Increases the contact duration.
     *
     * @return the new value for the contact duration
     */
    public long increaseDuration(long amount) {
        this.duration += amount;
        return this.duration;
    }

    /**
     * Increases the number of contacts.
     *
     * @return the new value for the number of contacts
     */
    public int increaseContacts() {
        return ++this.contacts;
    }
}
