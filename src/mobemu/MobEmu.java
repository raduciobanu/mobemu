/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package mobemu;

import java.util.*;
import mobemu.algorithms.InterestSpace;
import mobemu.node.Message;
import mobemu.node.Node;
import mobemu.node.Stats;
import mobemu.parsers.NCCU;
import mobemu.trace.Contact;
import mobemu.trace.Parser;
import mobemu.trace.Trace;

/**
 *
 * @author Radu
 */
public class MobEmu {

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        long seed = 0;
        boolean dissemination = true;
        boolean batteryComputation = false;

        //Parser parser = new Sigcomm();
        //Parser parser = new UPB(UPB.UpbTrace.UPB2012);
        //Parser parser = new Haggle(Haggle.HaggleTrace.INFOCOM2006);
        Parser parser = new NCCU();
        //Parser parser = new HCMM(33, 3 * 24 * 3600, 30 * 24 * 3600, 1.25f, 1.50f, 0.1f, 400f, 400f, 40, 40, 10.0, 0.7, 7, 0, 0.0, 0.8f, 0);

        double duration = (double) (parser.getTraceData().getEndTime() - parser.getTraceData().getStartTime()) / (Parser.MILLIS_PER_MINUTE * 60);
        System.out.println("Trace duration in hours: " + duration);
        System.out.println("Trace contacts: " + parser.getTraceData().getContactsCount());
        System.out.println("Trace contacts per hour: " + (parser.getTraceData().getContactsCount() / duration));
        System.out.println("Nodes: " + parser.getNodesNumber());
        
        if (true) {
            System.exit(0);
        }

        Node[] nodes = initNodes(parser, dissemination, seed);
        List<Message> messages = run(nodes, parser.getTraceData(), batteryComputation, dissemination, seed);

        System.out.println(nodes[0].getName());
        System.out.println("" + Stats.computeHitRate(messages, nodes, dissemination));
        System.out.println("" + Stats.computeDeliveryCost(messages, nodes, dissemination));
        System.out.println("" + Stats.computeDeliveryLatency(messages, nodes, dissemination));
        System.out.println("" + Stats.computeHopCount(messages, nodes, dissemination));
    }

    /**
     * Runs an opportunistic algorithm.
     *
     * @param nodes array of nodes
     * @param trace mobility trace
     * @param batteryComputation {@code true} if battery is taken into account
     * when routing/disseminating, {@code false} otherwise
     * @param dissemination {@code true} for dissemination, {@code false} for
     * routing
     * @param seed random number generator seed
     * @return list of messages generated during the trace
     */
    private static List<Message> run(Node[] nodes, Trace trace, boolean batteryComputation, boolean dissemination, long seed) {
        int messageCopies = nodes.length;
        int messageCount = nodes.length;

        int contactCount = trace.getContactsCount();
        long startTime = trace.getStartTime();
        long endTime = trace.getEndTime();
        long sampleTime = trace.getSampleTime();

        Calendar currentDay = Calendar.getInstance();
        Calendar generationTime = null;
        int previousDay = -1;
        boolean generate = false;
        Random messageRandom = new Random(seed);

        List<Message> messages = new ArrayList<>();

        for (long tick = startTime; tick < endTime; tick += sampleTime) {
            int count = 0;

            // update battery level
            if (batteryComputation) {
                for (Node node : nodes) {
                    node.updateBatteryLevel();
                }
            }

            currentDay.setTimeInMillis(tick);

            if (currentDay.get(Calendar.DATE) != previousDay) {
                generate = true;
                previousDay = currentDay.get(Calendar.DATE);
                generationTime = Message.generateMessageTime(messageRandom.nextDouble());
            }

            // generate messages
            if (generate && generationTime.get(Calendar.HOUR) == currentDay.get(Calendar.HOUR)) {
                messages.addAll(Message.generateMessages(nodes, messageCount, messageCopies, tick, dissemination, messageRandom));
                generate = false;
                System.out.println(messages.size());
            }

            for (int i = 0; i < contactCount; i++) {
                Contact contact = trace.getContactAt(i);

                if (contact.getStart() <= tick && contact.getEnd() >= tick) {

                    // there is a contact.
                    count++;

                    int observer = contact.getObserver();
                    int observed = contact.getObserved();

                    if (!nodes[observer].inFamiliarSet(observed)) {
                        // update total contact duration of encountered node.
                        nodes[observer].updateContactDuration(observed, sampleTime, tick);

                        // when the threshold has been exceeded, insert vi in F0 and C0
                        nodes[observer].checkThreshold(observed);
                    }

                    nodes[observer].updateCentrality(tick - startTime);
                    nodes[observer].updateTimes(observed, tick);

                    // do these steps only if the contact just began.
                    if (contact.getStart() == tick) {
                        // update the number of contacts
                        nodes[observer].updateContactsNumber(observed, tick);

                        // update the global familiar set of the current node.
                        nodes[observer].updateFamiliarSet(nodes[observed], true);

                        // update the number of encounters per hour
                        nodes[observer].updateEncounters(observed, tick);

                        // step 4 of the K-clique algorithm.
                        if (!nodes[observer].inFamiliarSet(observed)) {
                            nodes[observer].updateFamiliarSet(nodes[observed], false);
                        }

                        // step 5 of the K-clique algorithm.
                        if (!nodes[observer].inLocalCommunity(observed)) {
                            nodes[observer].updateLocalCommunity(nodes[observed]);
                        }

                        // step 6 of the K-clique algorithm.
                        if (nodes[observer].inLocalCommunity(observed)) {
                            nodes[observer].updateLocalCommunityAggressive(nodes[observed]);
                        }

                        // data dissemination.
                        nodes[observer].exchangeData(nodes[observed], contact.getEnd() - contact.getStart() + sampleTime, tick);
                    }
                }
            }

            // remove unused contacts.
            for (int i = count - 1; i >= 0; i--) {
                if (trace.getContactAt(i).getEnd() == tick) {
                    trace.removeContactAt(i);
                }
            }

            contactCount = trace.getContactsCount();
        }

        return messages;
    }

    /**
     * Initializes the nodes in a trace.
     *
     * @param parser parser information
     * @param dissemination {@code true} for dissemination, {@code false} for
     * routing
     * @param seed random number generator seed
     * @return
     */
    private static Node[] initNodes(Parser parser, boolean dissemination, long seed) {
        int dataMemorySize = 10000;
        int exchangeHistorySize = 100;
        int cacheMemorySize = 40;
        long traceStart = parser.getTraceData().getStartTime();
        long traceEnd = parser.getTraceData().getEndTime();
        Node[] nodes = new Node[parser.getNodesNumber()];

        PerTraceParams p = getPerTraceParams(parser.getTraceData().getName());

        for (int i = 0; i < nodes.length; i++) {
            //nodes[i] = new MoghadamSchulzrinne(i, nodes.length, parser.getContextData().get(i),
            //       parser.getSocialNetwork()[i], dataMemorySize, exchangeHistorySize, seed,
            //        traceStart, traceEnd, dissemination);
            nodes[i] = new InterestSpace(i, parser.getContextData().get(i), parser.getSocialNetwork()[i],
                    dataMemorySize, exchangeHistorySize, seed, traceStart, traceEnd, false, nodes,
                    p.socialNetworkThreshold, p.interestThreshold, p.contactsThreshold,
                    InterestSpace.InterestSpaceAlgorithm.CacheDecision, p.aggregationW1,
                    p.aggregationW2, p.aggregationW3, p.aggregationW4, p.cacheW1, p.cacheW2,
                    p.cacheW3, p.timeWindow);
            //nodes[i] = new ONSIDE(i, parser.getContextData().get(i), parser.getSocialNetwork()[i],
            //        dataMemorySize, exchangeHistorySize, seed, traceStart, traceEnd, false, nodes,
            //        p.interestedFriendsThreshold, p.encounteredInterestsThreshold, p.commonInterests,
            //        ONSIDE.ONSIDESort.None);
            //nodes[i] = new Epidemic(i, nodes.length, parser.getContextData().get(i), parser.getSocialNetwork()[i],
            //        dataMemorySize, exchangeHistorySize, seed, traceStart, traceEnd, dissemination, false);
            //nodes[i] = new MLSOR(i, parser.getContextData().get(i), parser.getSocialNetwork()[i],
            //        dataMemorySize, exchangeHistorySize, seed, traceStart, traceEnd, false, nodes);
            //nodes[i] = new SPRINT(i, parser.getContextData().get(i), parser.getSocialNetwork()[i],
            //      dataMemorySize, exchangeHistorySize, seed, traceStart, traceEnd, false, nodes, cacheMemorySize);
            //nodes[i] = new IRONMAN(i, nodes.length, parser.getContextData().get(i), parser.getSocialNetwork()[i],
            //        dataMemorySize, exchangeHistorySize, seed, traceStart, traceEnd, true);
            //nodes[i] = new BubbleRap(i, nodes.length, parser.getContextData().get(i), parser.getSocialNetwork()[i],
            //      dataMemorySize, exchangeHistorySize, seed, traceStart, traceEnd);
            //nodes[i] = new Jaccard(i, nodes.length, parser.getContextData().get(i), parser.getSocialNetwork()[i],
            //      dataMemorySize, exchangeHistorySize, seed, traceStart, traceEnd, cacheMemorySize,
            //    false, false, false, parser.getTraceData());
            //nodes[i] = new SENSE(i, parser.getContextData().get(i), parser.getSocialNetwork()[i],
            //        dataMemorySize, exchangeHistorySize, seed, traceStart, traceEnd, true, nodes);

        }

        return nodes;
    }

    /**
     * Gets the ONSIDE parameters for a given trace.
     *
     * @param trace name of the trace
     * @return ONSIDE parameters for the specified trace
     */
    private static PerTraceParams getPerTraceParams(String trace) {
        Map<String, PerTraceParams> perTraceParams = new HashMap<>();
        perTraceParams.put("Sigcomm", new PerTraceParams(1, 1, 1.0, 0.95, 0.98, 30, 0.25, 0.25, 0.25, 0.25, 0.2, 0.4, 0.4, 3600 * 1000));
        perTraceParams.put("UPB 2012", new PerTraceParams(1, 5, 0.2, 0.5, 0.1, 50, 0.25, 0.25, 0.25, 0.25, 0.2, 0.4, 0.4, 12 * 3600 * 1000));
        perTraceParams.put("Haggle Infocom 2006", new PerTraceParams(1, 0, 0.3, 1, 0.5, 0, 0.25, 0.25, 0.25, 0.25, 0.34, 0.66, 0.0, 360 * 1000));
        perTraceParams.put("SocialBlueConn", new PerTraceParams(1, 3, 0.32, 0.7, 0.2, 30, 0.25, 0.25, 0.25, 0.25, 0.2, 0.4, 0.4, 1800 * 1000));

        return perTraceParams.get(trace);
    }

    /**
     * Specifies the variable per-trace parameters.
     */
    private static class PerTraceParams {

        int commonInterests; // ONSIDE
        int interestedFriendsThreshold; // ONSIDE
        double encounteredInterestsThreshold; // ONSIDE
        double socialNetworkThreshold; // Interest Space
        double interestThreshold; // Interest Space
        int contactsThreshold; // Interest Spaces
        double aggregationW1; // Interest Spaces
        double aggregationW2; // Interest Spaces
        double aggregationW3; // Interest Spaces
        double aggregationW4; // Interest Spaces
        double cacheW1; // Interest Spaces
        double cacheW2; // Interest Spaces
        double cacheW3; // Interest Spaces
        long timeWindow; // Interest Spaces

        /**
         * Instantiates a {@code PerTraceParams} object.
         *
         * @param commonInterests number of common interests
         * @param interestedFriendsThreshold threshold for the number of
         * interested friends
         * @param encounteredInterestsThreshold threshold for percentage of
         * encountered interests
         * @param socialNetworkThreshold social network threshold
         * @param interestThreshold interest threshold
         * @param contactsThreshold contact threshold
         * @param aggregationW1 aggregation weight for the node similarity
         * component
         * @param aggregationW2 aggregation weight for the node friendship
         * component
         * @param aggregationW3 aggregation weight for the node connectivity
         * component
         * @param aggregationW4 aggregation weight for the node contacts
         * component
         * @param cacheW1 cache weight for the interested nodes ratio component
         * @param cacheW2 cache weight for the interests encountered ratio
         * component
         * @param cacheW3 cache weight for the interested friends ratio
         * component
         * @param timeWindow time window for Interest Spaces
         */
        public PerTraceParams(int commonInterests, int interestedFriendsThreshold,
                double encounteredInterestsThreshold, double socialNetworkThreshold,
                double interestThreshold, int contactsThreshold, double aggregationW1,
                double aggregationW2, double aggregationW3, double aggregationW4,
                double cacheW1, double cacheW2, double cacheW3, long timeWindow) {
            this.commonInterests = commonInterests;
            this.interestedFriendsThreshold = interestedFriendsThreshold;
            this.encounteredInterestsThreshold = encounteredInterestsThreshold;
            this.socialNetworkThreshold = socialNetworkThreshold;
            this.interestThreshold = interestThreshold;
            this.contactsThreshold = contactsThreshold;
            this.aggregationW1 = aggregationW1;
            this.aggregationW2 = aggregationW2;
            this.aggregationW3 = aggregationW3;
            this.aggregationW4 = aggregationW4;
            this.cacheW1 = cacheW1;
            this.cacheW2 = cacheW2;
            this.cacheW3 = cacheW3;
            this.timeWindow = timeWindow;
        }
    }
}
