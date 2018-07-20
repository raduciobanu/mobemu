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
 * NUS trace parser (http://crawdad.org/nus/contact/20060801/).
 *
 * @author Radu
 */
public class NUS implements Parser {

    private Trace trace;
    private Map<Integer, Context> context;
    private boolean[][] socialNetwork;

    /**
     * Constructs an {@link NUS} object.
     */
    public NUS() {
        parseNus("traces" + File.separator + "nus-contact" + File.separator + "mobicom06-trace.txt", 22341);
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
     * Parses a National University of Singapore trace and creates a list of
     * contacts.
     *
     * @param file contacts file
     * @param devices number of devices
     * @return a list of {@link Contact} objects generated from the trace
     */
    private void parseNus(String file, int devices) {

        trace = new Trace("NUS");
        socialNetwork = new boolean[devices][devices];
        context = new HashMap<>();

        long end = Long.MIN_VALUE;
        long start = Long.MAX_VALUE;

        // parse contacts file.
        try {
            String line;
            FileInputStream fstream = new FileInputStream(file);
            try (DataInputStream in = new DataInputStream(fstream)) {
                BufferedReader br = new BufferedReader(new InputStreamReader(in));

                line = br.readLine();
                String[] tokens;
                String delimiter = " ";
                tokens = line.split(delimiter);
                int sessions = Integer.parseInt(tokens[0]);
                PseudoMap[] count = new PseudoMap[devices];

                for (int k = 0; k < devices; k++) {
                    count[k] = new PseudoMap(k, 0);
                }

                for (int i = 0; i < sessions; i++) {
                    line = br.readLine();
                    tokens = line.split(delimiter);

                    long contactStart = Long.parseLong(tokens[0]);
                    long contactEnd = contactStart + Long.parseLong(tokens[3]);

                    line = br.readLine();
                    tokens = line.split(delimiter);

                    for (int j = 0; j < tokens.length; j++) {
                        for (int k = 0; k < tokens.length; k++) {
                            if (!tokens[j].equals(tokens[k]) && Integer.parseInt(tokens[j]) < devices && Integer.parseInt(tokens[k]) < devices) {
                                trace.addContact(new Contact(Integer.parseInt(tokens[j]), Integer.parseInt(tokens[k]), contactStart, contactEnd));
                                count[Integer.parseInt(tokens[j])].value = count[Integer.parseInt(tokens[j])].value + 1;
                            }
                        }
                    }

                    // compute trace duration.
                    if (contactEnd > end) {
                        end = contactEnd;
                    }

                    // compute trace zero time.
                    if (contactStart < start) {
                        start = contactStart;
                    }
                }
            }
        } catch (IOException | NumberFormatException e) {
            System.err.println("NUS Parser exception: " + e.getMessage());
        }

        trace.setStartTime(start);
        trace.setEndTime(end);
    }

    /**
     * Class that implements a pseudo-map that can be compared by value.
     */
    private static class PseudoMap implements Comparable<PseudoMap> {

        int key;
        int value;

        /**
         * Instantiates a {@code PseudoMap} object.
         *
         * @param key key of the entry
         * @param value value of the entry
         */
        public PseudoMap(int key, int value) {
            this.key = key;
            this.value = value;
        }

        @Override
        public int compareTo(PseudoMap o) {
            return o.value - this.value;
        }
    }
}
