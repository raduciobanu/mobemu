/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package mobemu.parsers;

import java.io.*;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import mobemu.node.Context;
import mobemu.node.Topic;
import mobemu.trace.Contact;
import mobemu.trace.Parser;
import static mobemu.trace.Parser.MILLIS_PER_SECOND;
import mobemu.trace.Trace;

/**
 * UPB trace parser (http://crawdad.org/upb/mobility2011/20120618/).
 *
 * @author Radu
 */
public class UPB implements Parser {

	private Trace trace;
	private Map<Integer, Context> context;
	private boolean[][] socialNetwork;
	private Calendar calendar;
	private boolean staticNodes;

	/**
	 * Constructs a {@link UPB} object.
	 *
	 * @param subtrace
	 *            type of UPB trace to be parsed
	 */
	public UPB(UpbTrace subtrace) {
		this(subtrace, false);
	}

	/**
	 * Constructs a {@link UPB} object.
	 *
	 * @param subtrace
	 *            type of UPB trace to be parsed
	 * @param staticNodes
	 *            specifies if there will be any static nodes in the simulation
	 *            (only for UPB 2015 trace)
	 */
	public UPB(UpbTrace subtrace, boolean staticNodes) {
		this.trace = null;
		this.context = new HashMap<>();
		this.socialNetwork = null;

		switch (subtrace) {
		case UPB2011: {
			String prefix = "traces" + File.separator + "upb2011" + File.separator;
			this.trace = new Trace("UPB 2011");
			calendar = Calendar.getInstance();
			calendar.set(2011, 10, 18, 8, 0, 0); // start of the trace (18/11/2011 08:00)
			parseUpb2011(prefix + "upb2011.dat", prefix + "social.dat", 22);
			break;
		}
		case UPB2012: {
			String prefix = "traces" + File.separator + "upb-hyccups2012" + File.separator;
			this.trace = new Trace("UPB 2012");
			parseUpb2012(prefix + "full_output.txt", prefix + "social_network.txt", prefix + "users_and_interests.txt");
			break;
		}
		case UPB2015: {
			this.staticNodes = staticNodes;
			String prefix = "traces" + File.separator + "upb-hyccups2015" + File.separator;
			this.trace = new Trace("UPB 2015");
			parseUpb2015(prefix + "contacts.sql");
			break;
		}
		default: {
			this.trace = new Trace("");
			this.socialNetwork = new boolean[0][0];
			break;
		}
		}
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

	/**
	 * Subtrace types for the Haggle trace.
	 */
	public static enum UpbTrace {

		UPB2011, UPB2012, UPB2015
	}

	/**
	 * Parses a UPB 2011 trace and creates a list of contacts.
	 *
	 * @param contacts
	 *            contacts file
	 * @param social
	 *            social network file
	 * @param devices
	 *            number of devices
	 */
	private void parseUpb2011(String contacts, String social, int devices) {

		long end = Long.MIN_VALUE;
		long start = Long.MAX_VALUE;

		socialNetwork = new boolean[devices][devices];

		// parse contacts file.
		try {
			String line;
			FileInputStream fstream = new FileInputStream(contacts);
			try (DataInputStream in = new DataInputStream(fstream)) {
				BufferedReader br = new BufferedReader(new InputStreamReader(in));

				while ((line = br.readLine()) != null) {

					String[] tokens;
					String delimiter = "\t";

					tokens = line.split(delimiter);

					int observerID = Integer.parseInt(tokens[0]);
					int observedID = Integer.parseInt(tokens[1]);

					long contactStart = Long.parseLong(tokens[2]) * MILLIS_PER_SECOND + calendar.getTimeInMillis();
					long contactEnd = Long.parseLong(tokens[3]) * MILLIS_PER_SECOND + calendar.getTimeInMillis();
					contactStart -= contactStart % MILLIS_PER_SECOND;
					contactEnd -= contactEnd % MILLIS_PER_SECOND;

					// compute trace finish time.
					if (contactEnd > end) {
						end = contactEnd;
					}

					// compute trace start time.
					if (contactStart < start) {
						start = contactStart;
					}

					if (observerID <= devices && observedID <= devices) {
						trace.addContact(new Contact(observerID - 1, observedID - 1, contactStart, contactEnd));
					}
				}
			}
		} catch (IOException | NumberFormatException e) {
			System.err.println("Haggle Parser exception: " + e.getMessage());
		}

		trace.setStartTime(start);
		trace.setEndTime(end);
		trace.setSampleTime(MILLIS_PER_SECOND);

		// parse the social network file
		try {
			BufferedReader rdr = new BufferedReader(new FileReader(social));

			for (int i = 0; i < devices; i++) {
				String line = rdr.readLine();

				String[] parts = line.split(",|\\s+");

				for (int j = 0; j < parts.length; j++) {
					socialNetwork[i][j] = Integer.parseInt(parts[j]) >= 1;
				}
			}
		} catch (IOException | NumberFormatException e) {
			System.err.println("UPB Parser exception: " + e.getMessage());
		}
	}

	/**
	 * Parses a UPB 2012 trace and creates a list of contacts.
	 *
	 * @param contacts
	 *            contacts file
	 * @param social
	 *            social network file
	 * @param interests
	 *            interests file
	 */
	private void parseUpb2012(String contacts, String social, String interests) {

		long end = Long.MIN_VALUE;
		long start = Long.MAX_VALUE;

		Map<Integer, Integer> users = Upb2012GetValidUsers();
		for (int i = 0; i < users.size(); i++) {
			Context contextItem = context.get(i);
			if (contextItem == null) {
				context.put(i, new Context(i));
			}
		}

		// parse proximity file.
		try {
			String line;
			FileInputStream fstream = new FileInputStream(contacts);
			try (DataInputStream in = new DataInputStream(fstream)) {
				BufferedReader br = new BufferedReader(new InputStreamReader(in));

				while ((line = br.readLine()) != null) {

					String[] tokens;
					String delimiter = ",";

					tokens = line.split(delimiter);

					Integer observerID = users.get(Integer.parseInt(tokens[0]) - 1);
					Integer observedID = users.get(Integer.parseInt(tokens[1]) - 1);

					if (observerID == null || observedID == null) {
						continue;
					}

					long contactStart = Long.parseLong(tokens[2]);
					long contactEnd = Long.parseLong(tokens[3]) + contactStart;
					contactStart -= contactStart % MILLIS_PER_SECOND;
					contactEnd -= contactEnd % MILLIS_PER_SECOND;

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
			System.err.println("UPB Parser exception: " + e.getMessage());
		}

		trace.setStartTime(start);
		trace.setEndTime(end);
		trace.setSampleTime(MILLIS_PER_SECOND);

		// parse initial interests file.
		try {
			String line;
			FileInputStream fstream = new FileInputStream(interests);
			try (DataInputStream in = new DataInputStream(fstream)) {
				BufferedReader br = new BufferedReader(new InputStreamReader(in));

				while ((line = br.readLine()) != null) {

					String[] tokens;
					String delimiter = " ";

					tokens = line.split(delimiter);

					Integer id = users.get(Integer.parseInt(tokens[0]) - 1);
					if (id == null) {
						continue;
					}

					Context contextItem = context.get(id);
					if (contextItem == null) {
						contextItem = new Context(id);
					}

					if (tokens.length >= 2 && !tokens[1].equals("0")) {
						delimiter = ",";
						tokens = tokens[1].split(delimiter);

						for (String interest : tokens) {
							contextItem.addTopic(new Topic(Integer.parseInt(interest) - 1, 0));
						}

						context.put(contextItem.getId(), contextItem);
					}
				}
			}
		} catch (IOException | NumberFormatException e) {
			System.err.println("UPB Parser exception: " + e.getMessage());
		}

		// parse social network file.
		socialNetwork = new boolean[users.size()][users.size()];
		try {
			String line;
			FileInputStream fstream = new FileInputStream(social);
			try (DataInputStream in = new DataInputStream(fstream)) {
				BufferedReader br = new BufferedReader(new InputStreamReader(in));

				while ((line = br.readLine()) != null) {

					String[] tokens;
					String delimiter = ",";

					tokens = line.split(delimiter);

					Integer id = users.get(Integer.parseInt(tokens[0]) - 1);
					if (id == null) {
						continue;
					}

					for (int i = 1; i < tokens.length; i++) {
						Integer newId = users.get(Integer.parseInt(tokens[i]) - 1);
						if (newId != null) {
							socialNetwork[id][newId] = true;
						}
					}
				}
			}
		} catch (IOException | NumberFormatException e) {
			System.err.println("UPB Parser exception: " + e.getMessage());
		}
	}

	/**
	 * Parses a UPB 2015 trace and creates a list of contacts and social
	 * connections.
	 *
	 * @param contacts
	 *            contacts file
	 */
	private void parseUpb2015(String contacts) {

		long end = Long.MIN_VALUE;
		long start = Long.MAX_VALUE;
		int userCount = 0;

		Map<String, Integer> users = new HashMap<>(); // key = string ID, value = user ID
		Map<Integer, Set> connections = new HashMap<>();
		int contactsPerHour = 0;
		long startTime = 0;

		// parse proximity file.
		try {
			String line;
			FileInputStream fstream = new FileInputStream(contacts);
			try (DataInputStream in = new DataInputStream(fstream)) {
				BufferedReader br = new BufferedReader(new InputStreamReader(in));

				while ((line = br.readLine()) != null) {

					if (line.startsWith("#")) {
						continue;
					}

					String[] tokens;
					String delimiter = "[,() \']+";

					tokens = line.split(delimiter);

					if (tokens.length < 8) {
						continue;
					}

					long contactStart = Long.parseLong(tokens[2]) * MILLIS_PER_SECOND;
					long contactEnd = contactStart;

					// 5-hour interval with the most contacts
					if (contactStart < 1430996325000L || contactStart > 1431020970000L) {
						continue;
					}
					String observer = tokens[3];
					String observed = tokens[5];

					// Integer observerID = Integer.parseInt(tokens[2]);
					// Integer observedID = Integer.parseInt(tokens[3]);
					if (!users.containsKey(observer)) {
						users.put(observer, userCount++);
					}

					if (!users.containsKey(observed)) {
						users.put(observed, userCount++);
					}

					int observerID = users.get(observer);
					int observedID = users.get(observed);

					if (Integer.parseInt(tokens[7]) == 1) {
						Set<Integer> con = connections.get(observerID);
						if (con == null) {
							con = new TreeSet<>();
							con.add(observedID);
							connections.put(observerID, con);
						} else {
							con.add(observedID);
						}

						con = connections.get(observedID);
						if (con == null) {
							con = new TreeSet<>();
							con.add(observerID);
							connections.put(observedID, con);
						} else {
							con.add(observerID);
						}
					}

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
			System.err.println("UPB Parser exception: " + e.getMessage());
		}

		if (staticNodes && userCount <= 6) {
			StartEnd contactInfo = new StartEnd(start, end);

			mapStaticNodeContacts(true, userCount++, contactInfo);
			mapStaticNodeContacts(false, userCount++, contactInfo);

			start = contactInfo.start;
			end = contactInfo.end;
		}

		trace.setStartTime(start);
		trace.setEndTime(end);
		trace.setSampleTime(MILLIS_PER_SECOND);

		socialNetwork = new boolean[userCount][userCount];
		for (int i = 0; i < userCount; i++) {
			for (int j = 0; j < userCount; j++) {
				Set<Integer> con = connections.get(i);
				if (con != null && con.contains(j)) {
					socialNetwork[i][j] = true;
					socialNetwork[j][i] = true;
				} else {
					socialNetwork[i][j] = false;
					socialNetwork[j][i] = false;
				}
			}
		}

		for (int i = 0; i < userCount; i++) {
			Context contextItem = context.get(i);
			if (contextItem == null) {
				context.put(i, new Context(i));
			}
		}

		/*
		 * startTime = 0; contactsPerHour = 0; trace.sort(); for (int i = 0; i <
		 * trace.getContactsCount(); i++) { Contact c = trace.getContactAt(i);
		 * 
		 * if (c.getStart() - startTime > 5 * MILLIS_PER_MINUTE * 60) {
		 * System.out.println(c.getStart() + "," + contactsPerHour); startTime =
		 * c.getStart(); contactsPerHour = 0; } contactsPerHour++; }
		 */
	}

	/**
	 * Maps contacts with static nodes on this trace.
	 * 
	 * @param first
	 *            specifies if this is the first static node or the second
	 * @param devices
	 *            number of devices
	 * @param contactInfo
	 *            lowest starting point and highest ending point of contacts
	 *            registered so far
	 */
	private void mapStaticNodeContacts(boolean first, int devices, StartEnd contactInfo) {
		try {
			// map with nodes from trace mapped to the 6 trace nodes
			Map<Integer, Integer> myMap = new HashMap<>();

			if (first) {
				myMap.put(60859, 0);
				myMap.put(60913, 1);
				myMap.put(61110, 2);
				myMap.put(61362, 3);
				myMap.put(62040, 4);
				myMap.put(41883, 5);
			} else {
				myMap.put(1150, 0);
				myMap.put(27103, 1);
				myMap.put(45274, 2);
				myMap.put(130677, 3);
				myMap.put(45288, 4);
				myMap.put(5509, 5);
			}

			String line;
			String filename = "traces" + File.separator + "static-nodes" + File.separator + "upb-hyccups2015"
					+ File.separator;
			filename += first ? "node1.txt" : "node2.txt";

			long minContactStart = first ? 1526370796000L : 1526378252000L;
			long maxContactEnd = first ? 1526388533000L : 1526396306000L;

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

					contactStart = contactInfo.start + contactStart - minContactStart;
					contactEnd = contactInfo.start + contactEnd - minContactStart;

					int observerID = Integer.parseInt(tokens[0]);
					int observedID = devices;

					if (!myMap.containsKey(observerID)) {
						continue;
					}

					observerID = myMap.get(observerID);

					trace.addContact(new Contact(observerID, observedID, contactStart, contactEnd));

					// compute trace zero time.
					if (contactStart < contactInfo.start) {
						contactInfo.start = contactStart;
					}

					// compute trace duration.
					if (contactEnd > contactInfo.end) {
						contactInfo.end = contactEnd;
					}
				}
			}
		} catch (IOException | NumberFormatException e) {
			System.err.println("UPB Parser exception: " + e.getMessage());
		}
	}

	/**
	 * Gets only the valid UPB 2012 trace users (i.e. those who actively
	 * participated in the trace and had at least one interest). This function can
	 * be modified depending on what is required.
	 *
	 * @return a map that correlates between the valid user's real ID from the trace
	 *         and the newly-assigned one (since the IDs should be consecutive)
	 */
	private static Map<Integer, Integer> Upb2012GetValidUsers() {
		Map<Integer, Integer> users = new HashMap<>(); // real id / new id

		users.put(0, 0);
		users.put(12, 1);
		users.put(22, 2);
		users.put(14, 3);
		users.put(5, 4);
		users.put(32, 5);
		users.put(7, 6);
		users.put(30, 7);
		users.put(34, 8);
		users.put(31, 9);
		users.put(55, 10);
		users.put(52, 11);
		users.put(33, 12);
		users.put(43, 13);
		users.put(4, 14);
		users.put(15, 15);
		users.put(26, 16);
		users.put(20, 17);
		users.put(29, 18);
		users.put(40, 19);
		users.put(6, 20);
		users.put(45, 21);
		users.put(48, 22);
		users.put(61, 23);

		return users;
	}

	/**
	 * Class for storing the start and the stop of a contact.
	 * 
	 * @author raduioanciobanu
	 */
	private class StartEnd {
		public long start = 0;
		public long end = 0;

		/**
		 * Creates a {@code StartEnd} object.
		 * 
		 * @param start
		 *            start of the contact
		 * @param end
		 *            end of the contact
		 */
		public StartEnd(long start, long end) {
			this.start = start;
			this.end = end;
		}
	}
}
