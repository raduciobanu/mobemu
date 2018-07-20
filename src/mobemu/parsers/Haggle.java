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
 * Haggle trace parser (http://crawdad.org/cambridge/haggle/20090529/).
 *
 * @author Radu
 */
public class Haggle implements Parser {

    private Trace trace;
    private Map<Integer, Context> context;
    private boolean[][] socialNetwork;
    private Calendar calendar;

    /**
     * Constructs a {@link Haggle} object.
     *
     * @param subtrace type of Haggle trace to be parsed
     */
    public Haggle(HaggleTrace subtrace) {
        this.trace = null;
        this.context = new HashMap<>();
        this.socialNetwork = null;

        switch (subtrace) {
            case INTEL: {
                calendar = Calendar.getInstance();
                calendar.set(2005, 0, 6, 21, 0, 0); // start of the trace (25/01/2005 21:00)

                String prefix = "traces" + File.separator + "cambridge-haggle-imote-intel" + File.separator;
                this.trace = new Trace("Haggle Intel");
                this.socialNetwork = new boolean[9][9];
                parseHaggle(prefix + "contacts.Exp1.dat", prefix + "table.Exp1.dat", prefix + "MAC3BTable.Exp1.dat", 9);
                break;
            }
            case CAMBRIDGE: {
                calendar = Calendar.getInstance();
                calendar.set(2005, 0, 25, 8, 0, 0); // start of the trace (06/01/2005 8:00)

                String prefix = "traces" + File.separator + "cambridge-haggle-imote-cambridge" + File.separator;
                this.trace = new Trace("Haggle Cambridge");
                this.socialNetwork = new boolean[12][12];
                parseHaggle(prefix + "contacts.Exp2.dat", prefix + "table.Exp2.dat", prefix + "MAC3BTable.Exp2.dat", 12);
                break;
            }
            case INFOCOM: {
                calendar = Calendar.getInstance();
                calendar.set(2005, 2, 7, 8, 0, 0); // start of the trace (07/02/2005 8:00)

                String prefix = "traces" + File.separator + "cambridge-haggle-imote-infocom" + File.separator;
                this.trace = new Trace("Haggle Infocom");
                this.socialNetwork = new boolean[41][41];
                parseHaggle(prefix + "contacts.Exp3.dat", prefix + "table.Exp3.dat", prefix + "MAC3BTable.Exp3.dat", 41);
                break;
            }
            case CONTENT: {
                String prefix = "traces" + File.separator + "cambridge-haggle-imote-content";
                this.trace = new Trace("Haggle Content");
                this.socialNetwork = new boolean[54][54];
                parseHaggle(prefix + File.separator + "MAC3table", prefix, 54);
                break;
            }
            case INFOCOM2006: {
                calendar = Calendar.getInstance();
                calendar.set(2006, 3, 23, 12, 0, 0); // start of the trace (23/04/2006 12:00)

                String prefix = "traces" + File.separator + "cambridge-haggle-imote-infocom2006" + File.separator;
                this.trace = new Trace("Haggle Infocom 2006");
                this.socialNetwork = new boolean[99][99];
                parseHaggle(prefix + "contacts.Exp6.dat", prefix + "table.Exp6.dat", prefix + "MAC2id.dat", prefix + "forms", 99);
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
        for (int i = 0; i < socialNetwork.length; i++) {
            Context contextItem = context.get(i);
            if (contextItem == null) {
                context.put(i, new Context(i));
            }
        }

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
     * Subtrace types for the Haggle trace.
     */
    public static enum HaggleTrace {

        INTEL, CAMBRIDGE, INFOCOM, CONTENT, INFOCOM2006
    }

    /**
     * Parses a Haggle trace and creates a list of contacts.
     *
     * @param contacts contacts file
     * @param table contact details file
     * @param MAC MAC addresses file
     * @param devices number of devices
     */
    private void parseHaggle(String contacts, String table, String MAC, int devices) {

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

        if (MAC == null) {
            return;
        }

        // parse MAC addresses file.
        try {
            String line;
            FileInputStream fstream = new FileInputStream(MAC);
            try (DataInputStream in = new DataInputStream(fstream)) {
                BufferedReader br = new BufferedReader(new InputStreamReader(in));

                while ((line = br.readLine()) != null) {

                    String[] tokens;
                    String delimiter = "\t";

                    tokens = line.split(delimiter);

                    int ID = Integer.parseInt(tokens[0]);
                    String startMAC = tokens[1];

                    // use ID and startMAC to add data if necessary.
                }
            }
        } catch (IOException | NumberFormatException e) {
            System.err.println("Haggle Parser exception: " + e.getMessage());
        }

        if (table == null) {
            return;
        }

        // parse contact details file.
        try {
            int linesRead = 0;
            String line;
            FileInputStream fstream = new FileInputStream(table);
            try (DataInputStream in = new DataInputStream(fstream)) {
                BufferedReader br = new BufferedReader(new InputStreamReader(in));

                while ((line = br.readLine()) != null) {

                    linesRead++;

                    if (linesRead > 2) {
                        // read the first line.
                        String[] tokens;
                        String delimiter = "\t";

                        tokens = line.split(delimiter);

                        int ID = Integer.parseInt(tokens[0]);
                        int device = Integer.parseInt(tokens[1]);
                        int incidence = Integer.parseInt(tokens[2]);

                        int totalContacts = Integer.parseInt(tokens[6]);
                        int[] individualContacts = new int[devices];
                        for (int i = 0; i < devices; i++) {
                            individualContacts[i] = Integer.parseInt(tokens[7 + i]);
                        }

                        // read the second line.
                        line = br.readLine();
                        linesRead++;
                        tokens = line.split(delimiter);

                        int totalTime = Integer.parseInt(tokens[6]);
                        int[] individualTime = new int[devices];
                        for (int i = 0; i < devices; i++) {
                            individualTime[i] = Integer.parseInt(tokens[7 + i]);
                        }

                        // read empty line.
                        br.readLine();
                        linesRead++;

                        // use gathered fields to store data.
                    }
                }
            }
        } catch (IOException | NumberFormatException e) {
            System.err.println("Haggle Parser exception: " + e.getMessage());
        }
    }

    /**
     * Parses a Haggle trace and creates a list of contacts.
     *
     * @param MAC MAC addresses file
     * @param folder folder containing contact details file
     * @param devices number of devices
     */
    private void parseHaggle(String MAC, String folder, int devices) {

        long end = Long.MIN_VALUE;
        long start = Long.MAX_VALUE;

        // parse MAC addresses file.
        try {
            String line;
            FileInputStream fstream = new FileInputStream(MAC);
            try (DataInputStream in = new DataInputStream(fstream)) {
                BufferedReader br = new BufferedReader(new InputStreamReader(in));

                while ((line = br.readLine()) != null) {

                    String[] tokens;
                    String delimiter = " ";

                    tokens = line.split(delimiter);

                    String startMAC = tokens[0];
                    int ID = Integer.parseInt(tokens[1]);

                    // use ID and startMAC to add data.
                }
            }
        } catch (IOException | NumberFormatException e) {
            System.err.println("Haggle Parser exception: " + e.getMessage());
        }

        FileFilter fileFilter = new FileFilter() {

            @Override
            public boolean accept(File file) {
                return file.isDirectory();
            }
        };

        // parse contact files.
        File dir = new File(folder);
        File[] files = dir.listFiles(fileFilter);

        for (int i = 0; i < files.length; i++) {
            String[] children = files[i].list();
            if (children == null) {
                break;
            } else {
                for (int j = 0; j < children.length; j++) {
                    String filename = files[i] + File.separator + children[j];

                    if (filename.startsWith(".")) {
                        continue;
                    }

                    try {
                        String line;
                        FileInputStream fstream = new FileInputStream(filename);

                        DataInputStream in = new DataInputStream(fstream);
                        BufferedReader br = new BufferedReader(new InputStreamReader(in));

                        while ((line = br.readLine()) != null) {

                            String[] tokens;
                            String delimiter = " ";

                            tokens = line.split(delimiter);

                            int observerID = Integer.parseInt(children[j].substring(0, children[j].length() - 4));
                            int observedID = Integer.parseInt(tokens[0]);
                            long contactStart = Integer.parseInt(tokens[1]) * MILLIS_PER_SECOND;
                            long contactEnd = Integer.parseInt(tokens[2]) * MILLIS_PER_SECOND;
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

                            // use observerID, observedID, contactStart, contactEnd to form objects.
                            if (observerID <= devices && observedID <= devices) {
                                trace.addContact(new Contact(observerID - 1, observedID - 1, contactStart, contactEnd));
                            }
                        }

                        in.close();
                    } catch (IOException | NumberFormatException e) {
                        System.err.println("Haggle Parser exception: " + e.getMessage());
                    }
                }
            }
        }

        trace.setStartTime(start);
        trace.setEndTime(end);
        trace.setSampleTime(MILLIS_PER_SECOND);
    }

    /**
     * Parses a Haggle trace and creates a list of contacts.
     *
     * @param contacts contacts file
     * @param table contact details file
     * @param MAC MAC addresses file
     * @param forms additional info file
     * @param devices number of devices
     */
    private void parseHaggle(String contacts, String table, String MAC, String forms, int devices) {

        // parse contacts, table and MAC files.
        parseHaggle(contacts, table, MAC, devices);

        // parse forms.
        File file = new File(forms);

        String[] children = file.list();
        if (children != null) {
            for (int j = 0; j < children.length; j++) {
                String filename = file + File.separator + children[j];

                if (filename.substring(filename.length() - 4).equals(".doc")
                        || filename.startsWith(".")) {
                    continue;
                }

                try {
                    int currentLine = 0;
                    String line;
                    FileInputStream fstream = new FileInputStream(filename);
                    try (DataInputStream in = new DataInputStream(fstream)) {
                        BufferedReader br = new BufferedReader(new InputStreamReader(in));

                        Context contextItem = null;

                        while ((line = br.readLine()) != null) {

                            currentLine++;
                            String[] tokens;
                            String delimiter = "=";

                            tokens = line.split(delimiter);

                            String[] tks;
                            String del = ",";

                            if (tokens.length >= 2) {
                                switch (currentLine) {
                                    case 1:
                                        int id = Integer.parseInt(tokens[1]) - 1;
                                        contextItem = context.get(id);
                                        if (contextItem == null) {
                                            contextItem = new Context(id);
                                        }
                                        break;

                                    case 19:
                                        tks = tokens[1].split(del);
                                        for (int i = 0; i < tks.length; i++) {
                                            if (contextItem != null) {
                                                contextItem.addTopic(new Topic(Integer.parseInt(tks[i]) - 1, 0));
                                            }
                                        }
                                        break;

                                    default:
                                        break;
                                }
                            }
                        }

                        if (contextItem != null) {
                            context.put(contextItem.getId(), contextItem);
                        }
                    }
                } catch (IOException | NumberFormatException e) {
                    System.err.println("Haggle Parser exception: " + e.getMessage());
                }
            }
        }
    }
}
