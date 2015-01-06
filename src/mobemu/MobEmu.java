/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package mobemu;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Random;
import mobemu.algorithms.Epidemic;
import mobemu.node.Message;
import mobemu.node.Node;
import mobemu.node.Stats;
import mobemu.parsers.Sigcomm;
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
        boolean dissemination = false;
        boolean batteryComputation = false;

        Parser sigcomm = new Sigcomm();
        Node[] nodes = initNodes(sigcomm, dissemination, seed);
        List<Message> messages = run(nodes, sigcomm.getTraceData(), batteryComputation, dissemination, seed);

        System.out.println("Total number of messages generated: " + messages.size());
        System.out.println("Hit rate: " + Stats.computeHitRate(messages, nodes, dissemination));
        System.out.println("Delivery cost: " + Stats.computeDeliveryCost(messages, nodes, dissemination));
        System.out.println("Delivery latency: " + Stats.computeDeliveryLatency(messages, nodes, dissemination));
        System.out.println("Hop count: " + Stats.computeHopCount(messages, nodes, dissemination));
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
        int messageCopies = 30;
        int messageCount = 30;

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

        int twoDays = 0;

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
            if (generate && generationTime.get(Calendar.HOUR) == currentDay.get(Calendar.HOUR) && twoDays++ < 2) {
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
                        nodes[observer].updateContactDuration(observed, sampleTime);

                        // when the threshold has been exceeded, insert vi in F0 and C0
                        nodes[observer].checkThreshold(observed);
                    }

                    nodes[observer].updateCentrality(tick - startTime);
                    nodes[observer].updateTimes(observed, tick);

                    // do these steps only if the contact just began.
                    if (contact.getStart() == tick) {
                        // update the number of contacts
                        nodes[observer].updateContactsNumber(observed);

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
        int dataMemorySize = 4500;
        int exchangeHistorySize = 100;
        int cacheMemorySize = 40;
        int commonInterests = 1;
        int interestedFriendsThreshold = 1;
        double encounteredInterestsThreshold = 1.0;
        long traceStart = parser.getTraceData().getStartTime();
        long traceEnd = parser.getTraceData().getEndTime();

        Node[] nodes = new Node[parser.getNodesNumber()];

        for (int i = 0; i < nodes.length; i++) {
            //nodes[i] = new MoghadamSchulzrinne(i, nodes.length, parser.getContextData().get(i),
            //        parser.getSocialNetwork()[i], dataMemorySize, exchangeHistorySize, seed,
            //        traceStart, traceEnd, dissemination);

            //nodes[i] = new ONSIDE(i, parser.getContextData().get(i), parser.getSocialNetwork()[i],
            //      dataMemorySize, exchangeHistorySize, seed, traceStart, traceEnd, false, nodes,
            //    interestedFriendsThreshold, encounteredInterestsThreshold, commonInterests, ONSIDE.ONSIDESort.None);

            nodes[i] = new Epidemic(i, nodes.length, parser.getContextData().get(i), parser.getSocialNetwork()[i],
                    dataMemorySize, exchangeHistorySize, seed, traceStart, traceEnd, dissemination, false);

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
}
