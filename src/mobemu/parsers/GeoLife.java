/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package mobemu.parsers;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import mobemu.node.Context;
import mobemu.trace.Contact;
import mobemu.trace.Parser;
import mobemu.trace.Trace;

/**
 * GeoLife trace parser
 * (https://www.microsoft.com/en-us/download/details.aspx?id=52367).
 *
 * @author Radu
 */
public class GeoLife implements Parser {

    private Trace trace;
    private Map<Integer, Context> context;
    private boolean[][] socialNetwork;
    private static final int GEOLIFE_RANGE = 30; // range for the GeoLife devices (in meters)

    /**
     * Constructs a {@link GeoLife} object.
     */
    public GeoLife() {
        parseGeolife("traces" + File.separator + "geolife-trajectories1.3", null, true, 182, 50);
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
     * Parses the GeoLife trace.
     *
     * @param folder location of the trace
     * @param startsWith first characters of the currently parsed subsection of
     * the trace
     * @param contacts {@code true} if the function parses only contacts
     * (pre-parsed data), {@code false} if it parses the original file
     * @param devices the number of participating devices
     * @param contactsLimit the limit of contacts for which a node is kept
     * @return a list of {@link Contact} objects generated from the trace
     */
    private void parseGeolife(String folder, String startsWith, boolean contacts, int devices, int contactsLimit) {

        trace = new Trace("GeoLife");
        socialNetwork = new boolean[devices][devices];
        context = new HashMap<>();

        long end = Long.MIN_VALUE;
        long start = Long.MAX_VALUE;

        if (!contacts) {
            parseGeoLifeFull(folder, startsWith);
            return;
        }

        File file = new File(folder + File.separator + "Contacts");

        for (String fileName : file.list()) {
            if (fileName.startsWith(".")) {
                continue;
            }

            try {
                String line;
                FileInputStream fstream = new FileInputStream(folder + File.separator + "Contacts" + File.separator + fileName);
                try (DataInputStream in = new DataInputStream(fstream)) {
                    BufferedReader br = new BufferedReader(new InputStreamReader(in));

                    while ((line = br.readLine()) != null) {

                        String[] tokens;
                        String delimiter = "\t";

                        tokens = line.split(delimiter);

                        int observerID = Integer.parseInt(tokens[0]);
                        int observedID = Integer.parseInt(tokens[1]);

                        long contactStart = Long.parseLong(tokens[2]) * MILLIS_PER_SECOND;
                        long contactEnd = Long.parseLong(tokens[3]) * MILLIS_PER_SECOND;
                        contactStart -= contactStart % MILLIS_PER_SECOND;
                        contactEnd -= contactEnd % MILLIS_PER_SECOND;

                        // compute trace duration.
                        if (contactEnd > end) {
                            end = contactEnd;
                        }

                        // compute trace zero time.
                        if (contactStart < start) {
                            start = contactStart;
                        }

                        if (contactStart > contactEnd) {
                            contactStart = contactStart - contactEnd;
                            contactEnd += contactStart;
                            contactStart = contactEnd - contactStart;
                        }

                        trace.addContact(new Contact(observerID, observedID, contactStart, contactEnd));
                    }
                }
            } catch (IOException | NumberFormatException e) {
                System.err.println("GeoLife Parser exception: " + e.getMessage());
            }
        }

        // create a list with nodes that have less than contactsLimit contacts
        ArrayList<Integer> lowContactNodes = new ArrayList<>();
        int[] contactsPerNode = new int[devices];
        for (int i = 0; i < trace.getContactsCount(); i++) {
            Contact contact = trace.getContactAt(i);
            contactsPerNode[contact.getObserved()]++;
            contactsPerNode[contact.getObserver()]++;
        }
        for (int i = 0; i < devices; i++) {
            if (contactsPerNode[i] < contactsLimit) {
                lowContactNodes.add(i);
            }
        }
        // remove all contacts with those nodes
        ArrayList<Contact> toRemove = new ArrayList<>();
        for (int i = 0; i < trace.getContactsCount(); i++) {
            Contact contact = trace.getContactAt(i);
            if (lowContactNodes.contains(contact.getObserved()) || lowContactNodes.contains(contact.getObserver())) {
                toRemove.add(contact);
            }
        }
        for (Contact removedContact : toRemove) {
            trace.removeContact(removedContact);
        }

        // rearrange the IDs of the nodes
        int id = 0;
        for (int i = 0; i < devices; i++) {
            if (!lowContactNodes.contains(i)) {
                for (int j = 0; j < trace.getContactsCount(); j++) {
                    Contact contact = trace.getContactAt(j);
                    if (contact.getObserved() == i) {
                        contact.setObserved(id);
                    }

                    if (contact.getObserver() == i) {
                        contact.setObserver(id);
                    }
                }
                id++;
            }
        }

        trace.setEndTime(end);
        trace.setStartTime(start);
        trace.setSampleTime(MILLIS_PER_SECOND);

        // clear data
        lowContactNodes.clear();
        toRemove.clear();
    }

    /**
     * Fully parses the GeoLife trace (note: the actual files are no long in the
     * MobEmu folder, since they were to large to be kept, but they can be found
     * at the following link:
     * https://www.microsoft.com/en-us/download/details.aspx?id=52367).
     *
     * @param folder location of the trace
     * @param startsWith first characters of the currently parsed subsection of
     * the trace
     */
    private static void parseGeoLifeFull(String folder, String startsWith) {
        List<GeoLifeUser> geoLifeUsers = new ArrayList<>();
        List<Contact> contactsList = new ArrayList<>();
        long end = Long.MIN_VALUE;
        long start = Long.MAX_VALUE;

        // auxiliary variables
        String[] tokens;
        String delimiter = ",";
        double d1, d2, d3, d4;
        int id;

        try {
            String location = folder + File.separator + "Data";
            File mainFolder = new File(location);
            File[] users = mainFolder.listFiles();

            for (File user : users) {
                if (!user.getName().startsWith(".")) {
                    id = Integer.parseInt(user.getName());
                    GeoLifeUser currentUser = new GeoLifeUser(id);

                    File currentFolder = new File(user.getAbsolutePath() + File.separator + "Trajectory");

                    File[] trajectories = currentFolder.listFiles();

                    for (File trajectory : trajectories) {
                        if (!trajectory.getName().startsWith(".")) {
                            Trajectory currentTrajectory = new Trajectory();

                            String line;
                            try (FileInputStream fstream = new FileInputStream(trajectory); DataInputStream in = new DataInputStream(fstream);
                                    BufferedReader br = new BufferedReader(new InputStreamReader(in))) {
                                int lineIndex = 0;

                                while ((line = br.readLine()) != null) {
                                    if (lineIndex <= 5) {
                                        lineIndex++;
                                        continue;
                                    }

                                    tokens = line.split(delimiter);

                                    if (!tokens[5].startsWith(startsWith)) {
                                        break;
                                    }

                                    d1 = Double.valueOf(tokens[0]);
                                    d2 = Double.valueOf(tokens[1]);
                                    d3 = Double.valueOf(tokens[3]);
                                    d4 = Double.valueOf(tokens[4]);

                                    currentTrajectory.addEntry(d1, d2, d3, d4);

                                    lineIndex++;
                                }

                                currentUser.addTrajectory(currentTrajectory);
                            }
                        }
                    }

                    geoLifeUsers.add(currentUser);
                }
            }
        } catch (NumberFormatException | IOException e) {
            System.err.println("GeoLife Parser exception: " + e.getMessage());
        }

        for (int i = 0; i < geoLifeUsers.size(); i++) {
            for (int j = i + 1; j < geoLifeUsers.size(); j++) {

                System.out.println(i + " " + j);

                GeoLifeUser userA = geoLifeUsers.get(i);
                GeoLifeUser userB = geoLifeUsers.get(j);

                outer_loop:
                for (Trajectory trajectoryUserA : userA.trajectories) {
                    for (Trajectory trajectoryUserB : userB.trajectories) {

                        for (int k = 0; k < trajectoryUserA.trajectory.size(); k++) {
                            TrajectoryEntry trajectoryEntryUserA = trajectoryUserA.trajectory.get(k);
                            boolean contact = false;
                            double contactStart = -1;

                            for (TrajectoryEntry trajectoryEntryUserB : trajectoryUserB.trajectory) {

                                // if the B month is higher that the A month, exit the loop
                                if ((int) (trajectoryEntryUserB.timeElapsed * 12) > (int) (trajectoryEntryUserA.timeElapsed * 12)) {
                                    continue outer_loop;
                                }

                                if (Math.abs(trajectoryEntryUserA.timeElapsed - trajectoryEntryUserB.timeElapsed) <= (1.0 / (24.0 * 60.0)) // error of maximum 1 minute
                                        && computeDistance(trajectoryEntryUserA.latitude, trajectoryEntryUserA.longitude, // range of max 30 meters
                                        trajectoryEntryUserB.latitude, trajectoryEntryUserB.longitude) < GEOLIFE_RANGE) {
                                    // if a contact hadn't begun, we set the start of the contact (if a contact had begun, do nothing)
                                    if (!contact) {
                                        contact = true;
                                        contactStart = trajectoryEntryUserA.timeElapsed;
                                    }
                                } else {
                                    // if a contact finished, advance the trajectory from A to the last value of the contact
                                    if (contact) {
                                        contact = false;

                                        long cStart = (long) (contactStart * 24 * 3600);
                                        long cEnd = (long) (trajectoryEntryUserB.timeElapsed * 24 * 3600);
                                        cStart -= cStart % MILLIS_PER_SECOND;
                                        cEnd -= cEnd % MILLIS_PER_SECOND;

                                        contactsList.add(new Contact(userA.id, userB.id, cStart, cEnd));
                                        while (trajectoryUserA.trajectory.get(k).timeElapsed < trajectoryEntryUserB.timeElapsed) {
                                            k++;
                                            if (k >= trajectoryUserA.trajectory.size()) {
                                                break;
                                            }
                                        }

                                        // compute trace duration.
                                        if ((long) (trajectoryEntryUserB.timeElapsed * 24 * 3600) > end) {
                                            end = (long) (trajectoryEntryUserB.timeElapsed * 24 * 3600);
                                        }

                                        // compute trace zero time.
                                        if ((long) (contactStart * 24 * 3600) < start) {
                                            start = (long) (contactStart * 24 * 3600);
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        try {
            FileWriter fstream = new FileWriter(folder + File.separator + "contacts" + startsWith + ".dat");
            try (PrintWriter out = new PrintWriter(fstream)) {
                for (Contact contact : contactsList) {
                    out.println(contact.getObserver() + "\t" + contact.getObserved() + "\t" + contact.getStart() + "\t" + contact.getEnd());
                }
            }
        } catch (IOException e) {
            System.err.println("GeoLife Parser exception: " + e.getMessage());
        }
    }

    /**
     * Computes the distance between two sets of coordinates
     *
     * @param firstLatitude latitude of the first point
     * @param firstLongitude longitude of the first point
     * @param secondLatitude latitude of the second point
     * @param secondLongitude longitude of the second point
     * @return
     */
    private static double computeDistance(double firstLatitude, double firstLongitude, double secondLatitude, double secondLongitude) {
        double earthRadius = 3958.75;
        double dLat = Math.toRadians(secondLatitude - firstLatitude);
        double dLng = Math.toRadians(secondLongitude - firstLongitude);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(firstLatitude)) * Math.cos(Math.toRadians(secondLatitude))
                * Math.sin(dLng / 2) * Math.sin(dLng / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        double dist = earthRadius * c;

        int meterConversion = 1609;

        return dist * meterConversion;
    }

    /**
     * Class for the trajectory of a Geolife node.
     *
     * @author Radu
     */
    private static class Trajectory {

        List<TrajectoryEntry> trajectory;

        /**
         * Creates a Trajectory object.
         */
        public Trajectory() {
            trajectory = new ArrayList<>();
        }

        /**
         * Adds an entry to a trajectory
         *
         * @param latitude the latitude of the new entry
         * @param longitude the longitude of the new entry
         * @param altitude the altitude of the new entry
         * @param timeElapsed time elapsed since 12/30/1899
         */
        public void addEntry(double latitude, double longitude, double altitude, double timeElapsed) {
            trajectory.add(new TrajectoryEntry(latitude, longitude, altitude, timeElapsed));
        }

        @Override
        public String toString() {
            String out = "";

            for (TrajectoryEntry entry : trajectory) {
                out += entry.latitude + " " + entry.longitude + " " + entry.altitude + " " + entry.timeElapsed + "\n";
            }

            return out;
        }
    }

    /**
     * Class for an entry in a trajectory.
     */
    private static class TrajectoryEntry {

        double latitude;
        double longitude;
        double altitude;
        double timeElapsed;

        /**
         * Creates a TrajectoryEntry object.
         *
         * @param latitude latitude of the user
         * @param longitude longitude of the user
         * @param altitude altitude of the user
         * @param timeElapsed time elapsed since 12/30/1899
         */
        TrajectoryEntry(double latitude, double longitude, double altitude, double timeElapsed) {
            this.latitude = latitude;
            this.longitude = longitude;
            this.altitude = (altitude == -777) ? 0 : altitude;
            this.timeElapsed = timeElapsed;
        }
    }

    /**
     * Class for a GeoLife trace user.
     *
     * @author Radu
     */
    private static class GeoLifeUser {

        ArrayList<Trajectory> trajectories;
        int id;

        /**
         * Creates a GeoLifeUser object.
         *
         * @param id ID of the current user
         */
        public GeoLifeUser(int id) {
            this.id = id;
            trajectories = new ArrayList<>();
        }

        /**
         * Adds a trajectory for the current user.
         *
         * @param trajectory trajectory to be added
         */
        public void addTrajectory(Trajectory trajectory) {
            trajectories.add(trajectory);
        }
    }
}
