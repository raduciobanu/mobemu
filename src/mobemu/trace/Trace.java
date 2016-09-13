/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package mobemu.trace;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Class for representing all the contacts of a mobility trace.
 *
 * @author Radu
 */
public class Trace {

    private List<Contact> contacts; // all contacts
    private String name; // the designated name of the trace
    private long start; // the starting time of the trace
    private long end; // the finish time of the trace
    private long sampleTime; // the sample time of this trace (in ticks)

    /**
     * Instantiates a {@code Trace} object.
     *
     * @param name designated name for this trace
     */
    public Trace(String name) {
        this(name, 0, 0);
    }

    /**
     * Instantiates a {@code Trace} object.
     *
     * @param name designated name for this trace
     * @param start start time of the trace
     * @param end finish time of the trace
     */
    public Trace(String name, long start, long end) {
        this.name = name;
        this.contacts = new ArrayList<>();
        this.start = start;
        this.end = end;
        this.sampleTime = 1;
    }

    /**
     * Adds a contact to the trace.
     *
     * @param contact contact to be added
     * @return {@code true} if the contact didn't exist before in the trace, {@code false}
     * otherwise
     */
    public boolean addContact(Contact contact) {
        if (contacts.contains(contact)) {
            return false;
        }

        return contacts.add(contact);
    }

    /**
     * Sorts the contacts by start time.
     */
    public void sort() {
        Collections.sort(contacts);
    }

    /**
     * Sets the start time of the trace.
     *
     * @param start start time to be set
     */
    public void setStartTime(long start) {
        this.start = start;
    }

    /**
     * Sets the finish time of the trace.
     *
     * @param end finish time to be set
     */
    public void setEndTime(long end) {
        this.end = end;
    }

    /**
     * Sets the sample time of this trace.
     *
     * @param sampleTime sample time to be set
     */
    public void setSampleTime(long sampleTime) {
        this.sampleTime = sampleTime;
    }

    /**
     * Gets the start time of the trace.
     *
     * @return start time of the trace
     */
    public long getStartTime() {
        return start;
    }

    /**
     * Gets the finish time of the trace.
     *
     * @return finish time of the trace
     */
    public long getEndTime() {
        return end;
    }

    /**
     * Gets the sample time of this trace.
     *
     * @return trace sample time in ticks
     */
    public long getSampleTime() {
        return sampleTime;
    }

    /**
     * Gets the contact of this trace at the given index.
     *
     * @param id index in the contacts list
     * @return the contact at the given index, or {@code null} if the index is
     * out of bounds
     */
    public Contact getContactAt(int id) {
        return contacts.get(id);
    }

    /**
     * Gets the number of contacts in this trace.
     *
     * @return the number of contacts
     */
    public int getContactsCount() {
        return contacts.size();
    }

    /**
     * Gets the trace's name.
     *
     * @return the name of the trace
     */
    public String getName() {
        return name;
    }

    /**
     * Removes a given contact from the trace.
     *
     * @param contact the contact to be removed
     */
    public void removeContact(Contact contact) {
        contacts.remove(contact);
    }

    /**
     * Removes a contact from a given index.
     *
     * @param index index of the contact to be removed
     * @return the contact that was removed, or {@code null} if the index was
     * out of bounds
     */
    public Contact removeContactAt(int index) {
        return contacts.remove(index);
    }
}
