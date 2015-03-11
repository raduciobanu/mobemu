/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package mobemu.parsers;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import mobemu.node.Context;
import mobemu.trace.Contact;
import mobemu.trace.Parser;
import mobemu.trace.Trace;

/**
 * Runs an HCMM simulation and returns the results as a trace.
 *
 * @author Radu
 */
public class HCMM extends mobemu.utils.HCMM implements Parser {

    private Trace trace;
    private Map<Integer, Context> context;
    private boolean[][] socialNetwork;
    private long end = Long.MIN_VALUE;
    private long start = Long.MAX_VALUE;
    private List<Contact> contactsInProgress;

    /**
     * Constructor for an {@code HCMM} object.
     *
     * @param devices number of devices
     * @param simulationTime duration of simulation (in seconds)
     * @param reconfigurationInterval reconfiguration interval
     * @param minHostSpeed minimum node speed
     * @param maxHostSpeed maximum node speed
     * @param connectionThreshold connection width
     * @param gridWidth the width of the grid (in meters)
     * @param gridHeight the height of the grid (in meters)
     * @param rows rows the grid is split into
     * @param columns columns the grid is split into
     * @param transmissionRadius transmission radius of nodes
     * @param rewiringProb rewiring probability
     * @param groupsNumber number of communities
     * @param travelersNumber number of traveler nodes
     * @param travelersSpeed speed of traveler nodes
     * @param remainingProbability probability of a node remaining in its home
     * cell
     * @param seed random number generator seed
     */
    public HCMM(int devices, long simulationTime, long reconfigurationInterval,
            float minHostSpeed, float maxHostSpeed, float connectionThreshold, float gridWidth, float gridHeight,
            int rows, int columns, double transmissionRadius, double rewiringProb, int groupsNumber,
            int travelersNumber, double travelersSpeed, double remainingProbability, int seed) {
        super(seed);

        trace = new Trace("HCMM");
        socialNetwork = new boolean[devices][devices];
        context = new HashMap<>();
        contactsInProgress = new ArrayList<>();

        runSimulator(devices, simulationTime, reconfigurationInterval, minHostSpeed,
                maxHostSpeed, connectionThreshold, gridWidth, gridHeight, rows, columns,
                transmissionRadius, rewiringProb, groupsNumber, travelersNumber,
                travelersSpeed, remainingProbability);
        computeSocialNetwork();
        computeContext(devices);
    }

    @Override
    public Trace getTraceData() {
        trace.sort();
        return trace;
    }

    @Override
    public Map<Integer, Context> getContextData() {
        return context;
    }

    @Override
    public boolean[][] getSocialNetwork() {
        return socialNetwork;
    }

    @Override
    public int getNodesNumber() {
        return socialNetwork.length;
    }

    @Override
    protected void initializeData() {
    }

    @Override
    protected void publishData() {
    }

    @Override
    protected void startContact(int nodeA, int nodeB, double tick) {
        tick *= MILLIS_PER_SECOND;

        contactsInProgress.add(new Contact(nodeA, nodeB, (long) tick, (long) tick));

        if ((long) tick < start) {
            start = (long) tick;
        }
    }

    @Override
    protected void endContact(int nodeA, int nodeB, double tick) {
        tick *= MILLIS_PER_SECOND;
        Contact contact = null;

        for (Contact currentContact : contactsInProgress) {
            if (currentContact.getObserver() == nodeA && currentContact.getObserved() == nodeB) {
                contact = currentContact;
                break;
            }
        }

        if (contact == null) {
            return;
        }

        contactsInProgress.remove(contact);
        contact.setEnd((long) tick);

        if ((long) tick > end) {
            end = (long) tick;
        }

        trace.addContact(contact);
    }

    /**
     * Configures and runs the simulator.
     *
     * @param devices number of devices
     * @param simulationTime duration of simulation (in seconds)
     * @param reconfigurationInterval reconfiguration interval
     * @param minHostSpeed minimum node speed
     * @param maxHostSpeed maximum node speed
     * @param connectionThreshold connection width
     * @param gridWidth the width of the grid (in meters)
     * @param gridHeight the height of the grid (in meters)
     * @param rows rows the grid is split into
     * @param columns columns the grid is split into
     * @param transmissionRadius transmission radius of nodes
     * @param rewiringProb rewiring probability
     * @param groupsNumber number of communities
     * @param travelersNumber number of traveler nodes
     * @param travelersSpeed speed of traveler nodes
     * @param remainingProbability probability of a node remaining in its home
     * cell
     */
    private void runSimulator(int devices, long simulationTime, long reconfigurationInterval,
            float minHostSpeed, float maxHostSpeed, float connectionThreshold, float gridWidth,
            float gridHeight, int rows, int columns, double transmissionRadius, double rewiringProb,
            int groupsNumber, int travelersNumber, double travelersSpeed, double remainingProbability) {
        setNumHosts(devices);
        setTotalSimulationTime(simulationTime);
        setReconfigurationInterval(reconfigurationInterval);

        setMinHostSpeed(minHostSpeed);
        setMaxHostSpeed(maxHostSpeed);
        setConnectionThreshold(connectionThreshold);
        setSideLengthWholeAreaX(gridWidth);
        setSideLengthWholeAreaY(gridHeight);

        setNumberOfRows(rows);
        setNumberOfColumns(columns);
        setRadius(transmissionRadius);
        setRewiringProb(rewiringProb);
        setNumberOfGroups(groupsNumber);

        setNumberOfTravelers(travelersNumber);
        setTravelerSpeed(travelersSpeed);
        setRemainingProb(remainingProbability);

        move();

        trace.setStartTime(start);
        trace.setEndTime(end);
        trace.setSampleTime(MILLIS_PER_SECOND);
    }

    /**
     * Computes the social network based on the simulation.
     */
    private void computeSocialNetwork() {
        for (int i = 0; i < socialNetwork.length; i++) {
            for (int j = 0; j < socialNetwork[i].length; j++) {
                if (areInTheSameGroup(i, j, groups, numberOfGroups, numberOfMembers)) {
                    socialNetwork[i][j] = true;
                } else {
                    socialNetwork[i][j] = false;
                }
            }
        }
    }

    /**
     * Computes the context.
     *
     * @param devices number of devices
     */
    private void computeContext(int devices) {
        for (int i = 0; i < devices; i++) {
            context.put(i, new Context(i));
        }
    }
}
