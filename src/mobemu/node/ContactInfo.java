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
    private long lastEncounterTime; // time of last encounter with the node

    /**
     * Instantiates a {@code ContactInfo} object.
     *
     * @param lastEncounterTime time of the last encounter
     */
    public ContactInfo(long lastEncounterTime) {
        this.duration = 0;
        this.contacts = 0;
        this.lastEncounterTime = lastEncounterTime;
    }

    /**
     * Instantiates a {@code ContactInfo} object.
     *
     * @param duration duration of the contact
     * @param contacts number of contacts
     * @param lastEncounterTime time of the last encounter
     */
    public ContactInfo(long duration, int contacts, long lastEncounterTime) {
        this.duration = duration;
        this.contacts = contacts;
        this.lastEncounterTime = lastEncounterTime;
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
     * Gets the last encounter time.
     *
     * @return the last encounter time
     */
    public long getLastEncounterTime() {
        return lastEncounterTime;
    }

    /**
     * Increases the contact duration.
     *
     * @param amount value that the contact duration is increased with
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

    /**
     * Sets the last encounter time.
     *
     * @param lastEncounterTime new value for the last encounter time
     */
    public void setLastEncounterTime(long lastEncounterTime) {
        this.lastEncounterTime = lastEncounterTime;
    }
}
