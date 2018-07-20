/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package mobemu.parsers;

import java.io.*;
import java.util.HashMap;
import java.util.Map;
import mobemu.node.Context;
import mobemu.node.Topic;
import mobemu.trace.Contact;
import mobemu.trace.Parser;
import static mobemu.trace.Parser.MILLIS_PER_SECOND;
import mobemu.trace.Trace;

/**
 * SocialBlueConn trace parser
 * (http://crawdad.org/unical/socialblueconn/20150208/).
 *
 * @author Radu
 */
public class SocialBlueConn implements Parser {

    private Trace trace;
    private Map<Integer, Context> context;
    private boolean[][] socialNetwork;

    /**
     * Constructs a {@link StAndrews} object.
     */
    public SocialBlueConn() {
        String prefix = "traces" + File.separator + "socialblueconn" + File.separator;
        parseSocialBlueConn(prefix + "contacts" + File.separator + "Bluetooth_contacts.txt",
                prefix + "friendships" + File.separator + "Facebook_friendships.txt",
                prefix + "interests", 15);
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
     * Parses the SocialBlueConn trace.
     *
     * @param contacts contacts file
     * @param friendship social connections file
     * @param interestsFolder folder with interests file
     * @param devices number of devices in the trace
     */
    private void parseSocialBlueConn(String contacts, String friendship, String interestsFolder, int devices) {
        trace = new Trace("SocialBlueConn");
        socialNetwork = new boolean[devices][devices];
        context = new HashMap<>();

        long end = Long.MIN_VALUE;
        long start = Long.MAX_VALUE;

        // parse contacts file.
        try {
            String line;
            FileInputStream fstream = new FileInputStream(contacts);
            try (DataInputStream in = new DataInputStream(fstream)) {
                BufferedReader br = new BufferedReader(new InputStreamReader(in));

                while ((line = br.readLine()) != null) {
                    String[] tokens;
                    String delimiter = "\\s+";

                    tokens = line.split(delimiter);

                    int observerID = Integer.parseInt(tokens[0]) - 1;
                    int observedID = Integer.parseInt(tokens[1]) - 1;

                    if (observedID >= devices) {
                        continue;
                    }

                    long contactStart = Long.parseLong(tokens[2]);
                    contactStart -= contactStart % MILLIS_PER_SECOND;

                    // since the ending of the contact isn't specified, we'll assume it is a second
                    long contactEnd = contactStart + MILLIS_PER_SECOND;

                    if (trace.getContactsCount() > 0) {
                        Contact previousContact = trace.getContactAt(trace.getContactsCount() - 1);

                        if (previousContact.getObserver() == observerID && previousContact.getObserved() == observedID
                                && Math.abs(previousContact.getStart() - contactStart) < 10 * MILLIS_PER_MINUTE) {
                            previousContact.setEnd(Math.max(previousContact.getEnd(), contactEnd));
                            continue;
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
            System.err.println("SocialBlueConn Parser exception: " + e.getMessage());
        }

        trace.setStartTime(start);
        trace.setEndTime(end);
        trace.setSampleTime(MILLIS_PER_SECOND);

        // parse friendship file.
        try {
            String line;
            FileInputStream fstream = new FileInputStream(friendship);
            try (DataInputStream in = new DataInputStream(fstream)) {
                BufferedReader br = new BufferedReader(new InputStreamReader(in));

                br.readLine();

                while ((line = br.readLine()) != null) {
                    if (line.isEmpty()) {
                        continue;
                    }

                    String[] tokens;
                    String delimiter = "\\s+";

                    tokens = line.split(delimiter);

                    int observerID = Integer.parseInt(tokens[0]) - 1;

                    for (int i = 1; i < tokens.length; i++) {
                        socialNetwork[observerID][i - 1] = (Integer.parseInt(tokens[i]) == 1);
                    }
                }
            }
        } catch (IOException | NumberFormatException e) {
            System.err.println("SocialBlueConn Parser exception: " + e.getMessage());
        }

        // parse interest files.
        File dir = new File(interestsFolder);
        File[] files = dir.listFiles(new FileFilter() {

            @Override
            public boolean accept(File file) {
                return !file.getName().contains("legend");
            }
        });

        Map<String, Integer> interestsSeen = new HashMap<>();
        int currentId = 0;

        for (File file : files) {
            try {
                String line;
                FileInputStream fstream = new FileInputStream(file);
                try (DataInputStream in = new DataInputStream(fstream)) {
                    BufferedReader br = new BufferedReader(new InputStreamReader(in));

                    br.readLine();
                    line = br.readLine();

                    String[] interestIds;
                    String delimiter = "\\s+";
                    interestIds = line.split(delimiter);

                    for (int i = 1; i < interestIds.length; i++) {
                        if (!interestsSeen.containsKey(interestIds[i])) {
                            interestsSeen.put(interestIds[i], currentId++);
                        }
                    }

                    while ((line = br.readLine()) != null) {
                        String[] tokens;
                        delimiter = "\\s+";

                        tokens = line.split(delimiter);

                        int observerID = Integer.parseInt(tokens[0]) - 1;
                        Context contextItem = context.get(observerID);
                        if (contextItem == null) {
                            contextItem = new Context(observerID);
                        }

                        for (int i = 1; i < tokens.length; i++) {
                            if (Integer.parseInt(tokens[i]) == 1) {
                                contextItem.addTopic(new Topic(interestsSeen.get(interestIds[i]), 0));
                            }
                        }

                        context.put(contextItem.getId(), contextItem);
                    }
                }
            } catch (IOException | NumberFormatException e) {
                System.err.println("SocialBlueConn Parser exception: " + e.getMessage());
            }
        }
    }
}
