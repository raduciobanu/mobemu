/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package mobemu.parsers;

import java.io.*;
import java.util.HashMap;
import java.util.Map;
import mobemu.node.Context;
import mobemu.trace.Contact;
import mobemu.trace.Parser;
import mobemu.trace.Trace;

/**
 * St. Andrews trace parser (http://crawdad.org/st_andrews/sassy/20110603/).
 *
 * @author Radu
 */
public class StAndrews implements Parser {

    private Trace trace;
    private Map<Integer, Context> context;
    private boolean[][] socialNetwork;

    /**
     * Constructs a {@link StAndrews} object.
     */
    public StAndrews() {
        String prefix = "traces" + File.separator + "standrews-sassy" + File.separator;
        parseStAndrews(prefix + "srsn.csv", prefix + "dsn.csv", 27);
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
     * Parses a St. Andrews trace and creates a list of contacts.
     *
     * @param SRSN self-reported social network file
     * @param DSN detected social network file
     * @param devices number of devices
     */
    private void parseStAndrews(String SRSN, String DSN, int devices) {

        trace = new Trace("St. Andrews");
        socialNetwork = new boolean[devices][devices];
        context = new HashMap<>();

        long end = Long.MIN_VALUE;
        long start = Long.MAX_VALUE;

        // parse SRSN file.
        try {
            String line;
            FileInputStream fstream = new FileInputStream(SRSN);
            try (DataInputStream in = new DataInputStream(fstream)) {
                BufferedReader br = new BufferedReader(new InputStreamReader(in));

                while ((line = br.readLine()) != null) {

                    String[] tokens;
                    String delimiter = ", ";

                    tokens = line.split(delimiter);

                    if (tokens[0].equals("device_having_encounter")) {
                        continue;
                    }

                    int observerID = Integer.parseInt(tokens[0]);
                    int observedID = Integer.parseInt(tokens[1]);

                    socialNetwork[observerID - 1][observedID - 1] = true;
                }
            }
        } catch (IOException | NumberFormatException e) {
            System.err.println("St. Andrews Parser exception: " + e.getMessage());
        }

        // parse DSN file.
        try {
            String line;
            FileInputStream fstream = new FileInputStream(DSN);
            try (DataInputStream in = new DataInputStream(fstream)) {
                BufferedReader br = new BufferedReader(new InputStreamReader(in));

                while ((line = br.readLine()) != null) {

                    String[] tokens;
                    String delimiter = ", ";

                    tokens = line.split(delimiter);

                    if (tokens[0].equals("device_having_encounter")) {
                        continue;
                    }

                    for (int i = 0; i < tokens.length; i++) {
                        tokens[i] = tokens[i].trim();
                    }

                    int observerID = Integer.parseInt(tokens[0]);
                    int observedID = Integer.parseInt(tokens[1]);

                    long contactStart = Integer.parseInt(tokens[2].substring(0, tokens[2].length() - 2)) * MILLIS_PER_SECOND;
                    long contactEnd = Integer.parseInt(tokens[3].substring(0, tokens[3].length() - 2)) * MILLIS_PER_SECOND;
                    contactStart -= contactStart % (MILLIS_PER_SECOND * 100);
                    contactEnd -= contactEnd % (MILLIS_PER_SECOND * 100);

                    int timeUploaded = Integer.parseInt(tokens[4].substring(0, tokens[4].length() - 2));
                    int RSSI = Integer.parseInt(tokens[5].substring(0, tokens[5].length() - 2));
                    String errorVal = tokens[6];

                    // compute trace duration.
                    if (contactEnd > end) {
                        end = contactEnd;
                    }

                    // compute trace zero time.
                    if (contactStart < start) {
                        start = contactStart;
                    }

                    // use observerID, observedID, contactStart, contactEnd to form objects.
                    if (observerID <= devices && observedID <= devices) {
                        trace.addContact(new Contact(observerID - 1, observedID - 1, contactStart, contactEnd));
                    }
                }
            }
        } catch (IOException | NumberFormatException e) {
            System.err.println("St. Andrews Parser exception: " + e.getMessage());
        }

        trace.setStartTime(start);
        trace.setEndTime(end);
        trace.setSampleTime(MILLIS_PER_SECOND * 100);
    }
}
