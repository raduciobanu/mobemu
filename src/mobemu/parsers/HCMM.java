/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package mobemu.parsers;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
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
	private boolean staticNodes;

	/**
	 * Constructor for an {@code HCMM} object.
	 *
	 * @param devices
	 *            number of devices
	 * @param simulationTime
	 *            duration of simulation (in seconds)
	 * @param reconfigurationInterval
	 *            reconfiguration interval
	 * @param minHostSpeed
	 *            minimum node speed
	 * @param maxHostSpeed
	 *            maximum node speed
	 * @param connectionThreshold
	 *            connection width
	 * @param gridWidth
	 *            the width of the grid (in meters)
	 * @param gridHeight
	 *            the height of the grid (in meters)
	 * @param rows
	 *            rows the grid is split into
	 * @param columns
	 *            columns the grid is split into
	 * @param transmissionRadius
	 *            transmission radius of nodes
	 * @param rewiringProb
	 *            rewiring probability
	 * @param groupsNumber
	 *            number of communities
	 * @param travelersNumber
	 *            number of traveler nodes
	 * @param travelersSpeed
	 *            speed of traveler nodes
	 * @param remainingProbability
	 *            probability of a node remaining in its home cell
	 * @param showRun
	 *            set to true if the simulation should be shown graphically
	 * @param sleepTime
	 *            duration of sleep between simulation steps
	 * @param seed
	 *            random number generator seed
	 * @param staticNodes
	 *            specifies if there will be any static nodes in the simulation
	 */
	public HCMM(int devices, long simulationTime, long reconfigurationInterval, float minHostSpeed, float maxHostSpeed,
			float connectionThreshold, float gridWidth, float gridHeight, int rows, int columns,
			double transmissionRadius, double rewiringProb, int groupsNumber, int travelersNumber,
			double travelersSpeed, double remainingProbability, int seed, boolean showRun, long sleepTime,
			boolean staticNodes) {
		super(seed);

		trace = new Trace("HCMM");
		socialNetwork = new boolean[devices][devices];
		context = new HashMap<>();
		contactsInProgress = new ArrayList<>();
		this.staticNodes = staticNodes;

		int nodesCount = staticNodes ? devices - 2 : devices;

		runSimulator(nodesCount, simulationTime, reconfigurationInterval, minHostSpeed, maxHostSpeed,
				connectionThreshold, gridWidth, gridHeight, rows, columns, transmissionRadius, rewiringProb,
				groupsNumber, travelersNumber, travelersSpeed, remainingProbability, showRun, sleepTime);
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
	public int getStaticNodesNumber() {
		return staticNodes ? 2 : 0;
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
	 * Adds the contacts in progress to the list of contacts.
	 *
	 * @param simulationTime
	 *            duration of HCMM simulation
	 */
	private void addContactsInProgress(long simulationTime) {
		for (Contact contact : contactsInProgress) {
			long endTime = simulationTime * MILLIS_PER_SECOND;

			if (endTime > end) {
				end = endTime;
			}

			contact.setEnd(endTime);
			trace.addContact(contact);
		}
	}

	/**
	 * Configures and runs the simulator.
	 *
	 * @param devices
	 *            number of devices
	 * @param simulationTime
	 *            duration of simulation (in seconds)
	 * @param reconfigurationInterval
	 *            reconfiguration interval
	 * @param minHostSpeed
	 *            minimum node speed
	 * @param maxHostSpeed
	 *            maximum node speed
	 * @param connectionThreshold
	 *            connection width
	 * @param gridWidth
	 *            the width of the grid (in meters)
	 * @param gridHeight
	 *            the height of the grid (in meters)
	 * @param rows
	 *            rows the grid is split into
	 * @param columns
	 *            columns the grid is split into
	 * @param transmissionRadius
	 *            transmission radius of nodes
	 * @param rewiringProb
	 *            rewiring probability
	 * @param groupsNumber
	 *            number of communities
	 * @param travelersNumber
	 *            number of traveler nodes
	 * @param travelersSpeed
	 *            speed of traveler nodes
	 * @param remainingProbability
	 *            probability of a node remaining in its home cell
	 * @param showRun
	 *            set to true if the simulation should be shown graphically
	 * @param sleepTime
	 *            duration of sleep between simulation steps
	 */
	private void runSimulator(int devices, long simulationTime, long reconfigurationInterval, float minHostSpeed,
			float maxHostSpeed, float connectionThreshold, float gridWidth, float gridHeight, int rows, int columns,
			double transmissionRadius, double rewiringProb, int groupsNumber, int travelersNumber,
			double travelersSpeed, double remainingProbability, boolean showRun, long sleepTime) {
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

		setShowRun(showRun);
		setSleepTime(sleepTime);

		move();

		addContactsInProgress(simulationTime);

		if (staticNodes && devices <= 42) {
			mapStaticNodeContacts(true, devices);
			mapStaticNodeContacts(false, devices);
		}

		trace.setStartTime(start == Long.MAX_VALUE ? 0 : start);
		trace.setEndTime(end == Long.MIN_VALUE ? simulationTime * MILLIS_PER_SECOND : end);
		trace.setSampleTime(MILLIS_PER_SECOND);
	}

	/**
	 * Maps contacts with static nodes on this trace.
	 * 
	 * @param first
	 *            specifies if this is the first static node or the second
	 * @param devices
	 *            number of devices
	 */
	private void mapStaticNodeContacts(boolean first, int devices) {
		try {
			// map with nodes from trace mapped to the (at most) 40 trace nodes
			Map<Integer, Integer> myMap = new HashMap<>();

			if (first) {
				myMap.put(5730, 0);
				myMap.put(60705, 1);
				myMap.put(60708, 2);
				myMap.put(60743, 3);
				myMap.put(60773, 4);
				myMap.put(60856, 5);
				myMap.put(61068, 6);
				myMap.put(61083, 7);
				myMap.put(61804, 8);
				myMap.put(62040, 9);
				myMap.put(62072, 10);
				myMap.put(62292, 11);
				myMap.put(64605, 12);
				myMap.put(8785, 13);
				myMap.put(8974, 14);
				myMap.put(11630, 15);
				myMap.put(2495, 16);
				myMap.put(2920, 17);
				myMap.put(4213, 18);
				myMap.put(42246, 19);
				myMap.put(5539, 20);
				myMap.put(56790, 21);
				myMap.put(60913, 22);
				myMap.put(61361, 23);
				myMap.put(61362, 24);
				myMap.put(2032, 25);
				myMap.put(33489, 26);
				myMap.put(40, 27);
				myMap.put(60859, 28);
				myMap.put(61110, 29);
				myMap.put(63809, 30);
				myMap.put(65291, 31);
				myMap.put(65318, 32);
				myMap.put(654, 33);
				myMap.put(41883, 34);
				myMap.put(5945, 35);
				myMap.put(58590, 36);
				myMap.put(74, 37);
				myMap.put(15400, 38);
				myMap.put(32327, 39);
			} else {
				myMap.put(19101, 0);
				myMap.put(22827, 1);
				myMap.put(24668, 2);
				myMap.put(26048, 3);
				myMap.put(26252, 4);
				myMap.put(28257, 5);
				myMap.put(30792, 6);
				myMap.put(31001, 7);
				myMap.put(31479, 8);
				myMap.put(31717, 9);
				myMap.put(32049, 10);
				myMap.put(32072, 11);
				myMap.put(41580, 12);
				myMap.put(41986, 13);
				myMap.put(42174, 14);
				myMap.put(42717, 15);
				myMap.put(84428, 16);
				myMap.put(89415, 17);
				myMap.put(98034, 18);
				myMap.put(99163, 19);
				myMap.put(100559, 20);
				myMap.put(1192, 21);
				myMap.put(129400, 22);
				myMap.put(136485, 23);
				myMap.put(287, 24);
				myMap.put(28932, 25);
				myMap.put(45804, 26);
				myMap.put(100367, 27);
				myMap.put(103458, 28);
				myMap.put(27103, 29);
				myMap.put(99932, 30);
				myMap.put(1150, 31);
				myMap.put(42167, 32);
				myMap.put(45288, 33);
				myMap.put(685, 34);
				myMap.put(130677, 35);
				myMap.put(45259, 36);
				myMap.put(45252, 37);
				myMap.put(5509, 38);
				myMap.put(45274, 39);
			}

			String line;
			String filename = "traces" + File.separator + "static-nodes" + File.separator + "hcmm" + File.separator;
			filename += first ? "node1.txt" : "node2.txt";

			long minContactStart = first ? 1526345914000L : 1526378252000L;
			long maxContactEnd = first ? 1526402502000L : 1526407470000L;
			int newObservedId = first ? devices : devices + 1;

			FileInputStream fstream = new FileInputStream(filename);
			try (DataInputStream in = new DataInputStream(fstream)) {
				BufferedReader br = new BufferedReader(new InputStreamReader(in));

				while ((line = br.readLine()) != null) {
					String[] tokens;
					String delimiter = ",";

					tokens = line.split(delimiter);

					if (tokens.length < 4) {
						continue;
					}

					long contactStart = Long.parseLong(tokens[1]);
					long contactEnd = Long.parseLong(tokens[2]);

					if (contactEnd == contactStart) {
						contactEnd += 1000 * 60 * 10; // 10 minutes
					}

					if (contactStart < minContactStart || contactEnd > maxContactEnd) {
						continue;
					}

					contactStart = start + contactStart - minContactStart;
					contactEnd = start + contactEnd - minContactStart;

					int observerID = Integer.parseInt(tokens[0]);
					int observedID = newObservedId;

					if (!myMap.containsKey(observerID)) {
						continue;
					}

					observerID = myMap.get(observerID);

					trace.addContact(new Contact(observerID, observedID, contactStart, contactEnd));

					// compute trace zero time.
					if (contactStart < start) {
						start = contactStart;
					}

					// compute trace duration.
					if (contactEnd > end) {
						end = contactEnd;
					}
				}
			}
		} catch (IOException | NumberFormatException e) {
			System.err.println("HCMM Parser exception: " + e.getMessage());
		}
	}

	/**
	 * Computes the social network based on the simulation.
	 */
	private void computeSocialNetwork() {
		for (int i = 0; i < socialNetwork.length; i++) {
			for (int j = 0; j < socialNetwork[i].length; j++) {
				socialNetwork[i][j] = areInTheSameGroup(i, j, groups, numberOfGroups, numberOfMembers);
			}
		}
	}

	/**
	 * Computes the context.
	 *
	 * @param devices
	 *            number of devices
	 */
	private void computeContext(int devices) {
		for (int i = 0; i < devices; i++) {
			context.put(i, new Context(i));
		}
	}
}
