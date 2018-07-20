/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package mobemu.parsers;

import java.io.*;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;
import mobemu.node.Context;
import mobemu.node.Topic;
import mobemu.trace.Contact;
import mobemu.trace.Parser;
import mobemu.trace.Trace;

/**
 * Sigcomm trace parser (http://crawdad.org/thlab/sigcomm2009/20120715/).
 *
 * @author Radu
 */
public class Sigcomm implements Parser {

    private Trace trace;
    private Map<Integer, Context> context;
    private boolean[][] socialNetwork;
    private Calendar calendar;

    /**
     * Constructs a {@link Sigcomm} object.
     */
    public Sigcomm() {
        calendar = Calendar.getInstance();
        calendar.set(2009, 7, 17, 8, 0, 0); // start of the trace (17/08/2009 08:00)

        String prefix = "traces" + File.separator + "sigcomm2009" + File.separator;
        parseSigcomm(prefix + "interests1.csv", prefix + "friends1.csv", prefix + "interests2.csv", prefix + "proximity.csv", 76);
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
		return 0;
	}

    /**
     * Parses a Sigcomm trace and creates a list of contacts.
     *
     * @param interests interests file
     * @param friends friends file
     * @param interests2 second interest file
     * @param proximity proximity file
     * @param devices number of devices
     */
    private void parseSigcomm(String interests, String friends, String interests2, String proximity, int devices) {
        trace = new Trace("Sigcomm");
        socialNetwork = new boolean[devices][devices];
        context = new HashMap<>();

        long end = Long.MIN_VALUE;
        long start = Long.MAX_VALUE;

        // parse initial interests file.
        try {
            String line;
            FileInputStream fstream = new FileInputStream(interests);
            try (DataInputStream in = new DataInputStream(fstream)) {
                BufferedReader br = new BufferedReader(new InputStreamReader(in));

                while ((line = br.readLine()) != null) {

                    if (line.startsWith("#")) {
                        continue;
                    }

                    String[] tokens;
                    String delimiter = ";";

                    tokens = line.split(delimiter);

                    int id = Integer.parseInt(tokens[0]) - 1;
                    Context contextItem = context.get(id);
                    if (contextItem == null) {
                        contextItem = new Context(id);
                    }

                    if (Integer.parseInt(tokens[1]) > 3) {
                        contextItem.addTopic(new Topic(Integer.parseInt(tokens[1]), 0));
                    }

                    context.put(contextItem.getId(), contextItem);
                }
            }
        } catch (IOException | NumberFormatException e) {
            System.err.println("Sigcomm Parser exception: " + e.getMessage());
        }

        // parse initial friends file.
        try {
            String line;
            FileInputStream fstream = new FileInputStream(friends);
            try (DataInputStream in = new DataInputStream(fstream)) {
                BufferedReader br = new BufferedReader(new InputStreamReader(in));

                while ((line = br.readLine()) != null) {

                    if (line.startsWith("#")) {
                        continue;
                    }

                    String[] tokens;
                    String delimiter = ";";

                    tokens = line.split(delimiter);
                    socialNetwork[Integer.parseInt(tokens[0]) - 1][Integer.parseInt(tokens[1]) - 1] = true;
                }
            }
        } catch (IOException | NumberFormatException e) {
            System.err.println("Sigcomm Parser exception: " + e.getMessage());
        }

        // parse second friends file (assume all connections are available from the start)
        try {
            String line;
            FileInputStream fstream = new FileInputStream(friends);
            try (DataInputStream in = new DataInputStream(fstream)) {
                BufferedReader br = new BufferedReader(new InputStreamReader(in));

                while ((line = br.readLine()) != null) {

                    if (line.startsWith("#")) {
                        continue;
                    }

                    String[] tokens;
                    String delimiter = ";";

                    tokens = line.split(delimiter);
                    socialNetwork[Integer.parseInt(tokens[0]) - 1][Integer.parseInt(tokens[1]) - 1] = true;
                }
            }
        } catch (IOException | NumberFormatException e) {
            System.err.println("Sigcomm Parser exception: " + e.getMessage());
        }

        // parse second interests file.
        try {
            String line;
            FileInputStream fstream = new FileInputStream(interests2);
            try (DataInputStream in = new DataInputStream(fstream)) {
                BufferedReader br = new BufferedReader(new InputStreamReader(in));

                while ((line = br.readLine()) != null) {

                    if (line.startsWith("#")) {
                        continue;
                    }

                    String[] tokens;
                    String delimiter = ";";

                    tokens = line.split(delimiter);

                    int id = Integer.parseInt(tokens[0]) - 1;
                    Context contextItem = context.get(id);
                    if (contextItem == null) {
                        contextItem = new Context(id);
                    }

                    if (Integer.parseInt(tokens[1]) > 3) {
                        long timestamp = Long.parseLong(tokens[2]) * MILLIS_PER_SECOND + calendar.getTimeInMillis();
                        timestamp -= timestamp % MILLIS_PER_SECOND;

                        contextItem.addTopic(new Topic(Integer.parseInt(tokens[1]), timestamp));
                    }

                    context.put(contextItem.getId(), contextItem);
                }
            }
        } catch (IOException | NumberFormatException e) {
            System.err.println("Sigcomm Parser exception: " + e.getMessage());
        }

        // parse proximity file.
        try {
            String line;
            FileInputStream fstream = new FileInputStream(proximity);
            try (DataInputStream in = new DataInputStream(fstream)) {
                BufferedReader br = new BufferedReader(new InputStreamReader(in));

                while ((line = br.readLine()) != null) {

                    if (line.startsWith("#")) {
                        continue;
                    }

                    String[] tokens;
                    String delimiter = ";";

                    tokens = line.split(delimiter);

                    long timestamp = Long.parseLong(tokens[0]) * MILLIS_PER_SECOND + calendar.getTimeInMillis();
                    timestamp -= timestamp % MILLIS_PER_SECOND;

                    int observerID = Integer.parseInt(tokens[1]) - 1;
                    int observedID = Integer.parseInt(tokens[2]) - 1;

                    if (observedID >= devices) {
                        continue;
                    }

                    boolean merged = false;
                    for (int i = 0; i < trace.getContactsCount(); i++) {
                        Contact contact = trace.getContactAt(i);
                        if (contact.getObserver() == observerID && contact.getObserved() == observedID
                                && contact.getEnd() + 10 * MILLIS_PER_MINUTE >= timestamp) {

                            contact.setEnd(Math.max(contact.getEnd(), timestamp));
                            merged = true;

                            // compute trace duration.
                            if (contact.getEnd() > end) {
                                end = contact.getEnd();
                            }

                            break;
                        }
                    }

                    if (!merged) {

                        trace.addContact(new Contact(observerID, observedID, timestamp, timestamp));

                        // compute trace zero time.
                        if (timestamp < start) {
                            start = timestamp;
                        }
                    }
                }
            }
        } catch (IOException | NumberFormatException e) {
            System.err.println("Sigcomm Parser exception: " + e.getMessage());
        }

        trace.setStartTime(start);
        trace.setEndTime(end);
        trace.setSampleTime(MILLIS_PER_SECOND);
    }
}
