/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package mobemu.parsers;

import java.awt.Point;
import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import mobemu.node.Context;
import mobemu.trace.Contact;
import mobemu.trace.Parser;
import mobemu.trace.Trace;

/**
 * NCCU trace parser (http://www.cs.nccu.edu.tw/~d10003/).
 *
 * @author Radu
 */
public class NCCU implements Parser {

    // maximum range (in metres) of desired communication protocol (default Bluetooth)
    public static double maxRange = 10;
    // trace length as specified in the paper "NCCU Trace: Social-Network-Aware
    // Mobility Trace" by Tzu-Chieh Tsai and Ho-Hsiang Chan
    private static int traceLength = 3764;
    // maximum time allowed between two samples to be considered as part of the same contact
    private static final long SAMPLE_RANGE = 600 * MILLIS_PER_SECOND;
    private Trace trace;
    private Map<Integer, Context> context;
    private boolean[][] socialNetwork;
    private static int size = 115;

    /**
     * Constructs an {@link NCCU} object.
     */
    public NCCU() {
        parseNCCU("traces" + File.separator + "nccu" + File.separator + "nccu.dat");
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
     * Parses an NCCU trace in the MobEmu format.
     *
     * @param path path to the trace file
     */
    private void parseNCCU(String path) {
        trace = new Trace("NCCU");
        socialNetwork = new boolean[size][size];
        context = new HashMap<>();

        long end = Long.MIN_VALUE;
        long start = Long.MAX_VALUE;

        try {
            FileInputStream fstream = new FileInputStream(path);
            try (DataInputStream in = new DataInputStream(fstream)) {
                BufferedReader br = new BufferedReader(new InputStreamReader(in));

                String line;
                while ((line = br.readLine()) != null) {
                    String[] tokens = line.split("\\s+");

                    if (tokens.length >= 4) {
                        Contact contact = new Contact(Integer.parseInt(tokens[0]), Integer.parseInt(tokens[1]),
                                Long.parseLong(tokens[2]), Long.parseLong(tokens[3]));

                        // compute trace zero time.
                        if (contact.getStart() < start) {
                            start = contact.getStart();
                        }

                        // compute trace duration
                        if (contact.getEnd() > end) {
                            end = contact.getEnd();
                        }

                        trace.addContact(contact);

                        // also add reverse contact
                        trace.addContact(new Contact(contact.getObserved(), contact.getObserver(),
                                contact.getStart(), contact.getEnd()));
                    }
                }
            } catch (IOException ex) {
                Logger.getLogger(NCCU.class.getName()).log(Level.SEVERE, null, ex);
            }
        } catch (FileNotFoundException ex) {
            Logger.getLogger(NCCU.class.getName()).log(Level.SEVERE, null, ex);
        }

        trace.setStartTime(start);
        trace.setEndTime(end);
        trace.setSampleTime(MILLIS_PER_SECOND);
    }

    /**
     * Parses an NCCU trace in the original formal.
     *
     * @param path location of the trace files
     */
    private void parseNCCUOriginal(String path) {
        trace = new Trace("NCCU");
        socialNetwork = new boolean[size][size];
        context = new HashMap<>();

        long end = Long.MIN_VALUE;
        long start = Long.MAX_VALUE;

        File dir = new File(path);
        File[] files = dir.listFiles(new FileFilter() {

            @Override
            public boolean accept(File file) {
                return file.getName().endsWith(".csv");
            }
        });

        for (File file : files) {
            // current locations for each node
            Point[] locations = new Point[size];
            for (int i = 0; i < locations.length; i++) {
                locations[i] = new Point();
            }

            // map containing the timestamps of each successful connection between two nodes
            Map<NodePair, ArrayList<Long>> contacts = new HashMap<>();
            long timestamp = -1;

            FileInputStream fstream = null;
            try {
                fstream = new FileInputStream(file);
                try (DataInputStream in = new DataInputStream(fstream)) {
                    BufferedReader br = new BufferedReader(new InputStreamReader(in));

                    int minPosition = 0, maxPosition = 0;
                    String line = br.readLine();
                    String[] tokens = line.split("\\s+");

                    if (tokens.length >= 6) {
                        minPosition = Integer.parseInt(tokens[2]);
                        maxPosition = Integer.parseInt(tokens[3]);

                        if (Integer.parseInt(tokens[4]) != minPosition || Integer.parseInt(tokens[5]) != maxPosition) {
                            throw new RuntimeException("NCCU Parser exception: Invalid trace data");
                        }
                    }

                    while ((line = br.readLine()) != null) {
                        tokens = line.split("\\s+");

                        if (tokens.length >= 4) {
                            long newTimestamp = Long.parseLong(tokens[0]);
                            int nodeID = Integer.parseInt(tokens[1]) - 2;

                            // if the timestamp has changed, a new set of contacts has to be generated
                            if (timestamp != -1 && timestamp != newTimestamp) {
                                generateContacts(locations, minPosition, maxPosition, timestamp * MILLIS_PER_SECOND, contacts);
                            }

                            timestamp = newTimestamp;

                            locations[nodeID].x = Integer.parseInt(tokens[2]);
                            locations[nodeID].y = Integer.parseInt(tokens[3]);
                        }
                    }

                    // iterate over the contact pairs
                    for (Map.Entry<NodePair, ArrayList<Long>> entry : contacts.entrySet()) {
                        ArrayList<Long> timestamps = entry.getValue();
                        NodePair nodes = entry.getKey();

                        long startTime = timestamps.get(0);
                        long previousTime = startTime;

                        // compute trace zero time.
                        if (startTime < start) {
                            start = startTime;
                        }

                        // iterate over all the contacts
                        for (Long time : timestamps) {
                            if (time - previousTime <= SAMPLE_RANGE) {
                                // if the duration between contacts is lower than the
                                // limit, consider them as a single contact
                                previousTime = time;
                            } else {
                                // otherwise, add the current contact and start a new one
                                trace.addContact(new Contact(nodes.node1, nodes.node2, startTime, previousTime));
                                startTime = time;
                                previousTime = time;
                            }
                        }

                        trace.addContact(new Contact(nodes.node1, nodes.node2, startTime, previousTime));

                        // compute trace duration.
                        if (previousTime > end) {
                            end = previousTime;
                        }
                    }
                }
            } catch (IOException e) {
                System.err.println("NCCU Parser exception: " + e.getMessage());
            } finally {
                try {
                    if (fstream != null) {
                        fstream.close();
                    }
                } catch (IOException e) {
                    System.err.println("NCCU Parser exception: " + e.getMessage());
                }
            }
        }

        trace.setStartTime(start);
        trace.setEndTime(end);
        trace.setSampleTime(MILLIS_PER_SECOND);
    }

    /**
     * Generates contacts between nodes (as a map of node pairs and list of
     * timestamps) based on the location of each node at a given moment.
     *
     * @param locations array of locations of each node
     * @param minPosition minimum position on the map
     * @param maxPosition maximum position on the map
     * @param timestamp current timestamp
     * @param contacts maps of contacts per node pair where the result is
     * returned
     */
    private void generateContacts(Point[] locations, int minPosition, int maxPosition,
            long timestamp, Map<NodePair, ArrayList<Long>> contacts) {
        for (int i = 0; i < locations.length - 1; i++) {
            for (int j = i + 1; j < locations.length; j++) {
                if (inRange(locations[i], locations[j], minPosition, maxPosition)) {
                    NodePair key = new NodePair(i, j);

                    ArrayList<Long> pairContacts = contacts.get(key);
                    if (pairContacts == null) {
                        pairContacts = new ArrayList<>();
                        pairContacts.add(timestamp);
                        contacts.put(key, pairContacts);
                    } else {
                        pairContacts.add(timestamp);
                    }
                }
            }
        }
    }

    /**
     * Checks whether two nodes are in wireless communication range.
     *
     * @param first position of the first node
     * @param second position of the second node
     * @param minPosition minimum position on the map
     * @param maxPosition maximum position on the map
     * @return {@code true} if the nodes are in range, {@code false} otherwise
     */
    private boolean inRange(Point first, Point second, int minPosition, int maxPosition) {
        return Point.distance(first.x, first.y, second.x, second.y)
                <= (maxRange * traceLength / (maxPosition - minPosition));
    }

    /**
     * Simple class for a node pair.
     */
    private static class NodePair {

        public int node1 = 0;
        public int node2 = 0;

        /**
         * {@link NodePair} constructor.
         */
        public NodePair() {
        }

        /**
         * {@link NodePair} constructor.
         *
         * @param first first node
         * @param second second node
         */
        public NodePair(int first, int second) {
            node1 = first;
            node2 = second;
        }

        /**
         * Sets the two nodes in the pair
         *
         * @param first first node
         * @param second second node
         */
        public void set(int first, int second) {
            node1 = first;
            node2 = second;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj instanceof NodePair) {
                NodePair castObj = (NodePair) obj;
                return castObj.node1 == node1 && castObj.node2 == node2;
            }

            return false;
        }

        @Override
        public int hashCode() {
            return node1 * node2;
        }
    }
}
