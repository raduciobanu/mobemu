package mobemu.runAlgorithms;

import mobemu.node.Message;
import mobemu.node.Node;
import mobemu.parsers.Sigcomm;
import mobemu.parsers.UPB;
import mobemu.statistics.IStatistics;
import mobemu.statistics.NodeStatistics;
import mobemu.trace.Contact;
import mobemu.trace.Parser;
import mobemu.trace.Trace;
import mobemu.utils.message.IMessageGenerator;
import mobemu.utils.message.MessageGenerator;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Random;

/**
 * Created by radu on 5/12/2017.
 */
public abstract class RunNode implements IRun {

    protected IStatistics statistics;

    protected long seed;
    protected boolean dissemination;

    protected Parser parser;

    public RunNode(String[] args){
        parser = getSigcommParser();
        statistics = new NodeStatistics();
        initialize(args);
    }

    protected void initialize(String[] args){
        seed = 0;
        dissemination = false;
    }

    protected void printInitialStatistics(){
        // print some trace Statistics
        double duration = (double) (parser.getTraceData().getEndTime() - parser.getTraceData().getStartTime()) / (Parser.MILLIS_PER_MINUTE * 60);
        System.out.println("Trace duration in hours: " + duration);
        System.out.println("Trace contacts: " + parser.getTraceData().getContactsCount());
        System.out.println("Trace contacts per hour: " + (parser.getTraceData().getContactsCount() / duration));
        System.out.println("Nodes: " + parser.getNodesNumber());
    }

    protected void printFinalStatistics(Node[] nodes, List<Message> messages, boolean dissemination){
        System.out.println(nodes[0].getName());
//        System.out.println("" + Stats.computeHitRate(messages, nodes, dissemination));
//        System.out.println("" + Stats.computeDeliveryCost(messages, nodes, dissemination));
//        System.out.println("" + Stats.computeDeliveryLatency(messages, nodes, dissemination));
//        System.out.println("" + Stats.computeHopCount(messages, nodes, dissemination));
    }

    @Override
    public void run() {
        printInitialStatistics();

        Node[] nodes = new Node[parser.getNodesNumber()];
        for (int i = 0; i < nodes.length; i++) {
            nodes[i] = initializeNode(i, seed, nodes);
        }

        List<Message> messages = runTrace(nodes, parser.getTraceData(), false, dissemination, seed);
        System.out.println("Messages: " + messages.size());

        printFinalStatistics(nodes, messages, dissemination);
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
    public List<Message> runTrace(Node[] nodes, Trace trace, boolean batteryComputation, boolean dissemination, long seed) {
        int messageCopies = nodes.length;
        int messageCount = nodes.length;

        int contactCount = trace.getContactsCount();
        long startTime = trace.getStartTime();
        long endTime = trace.getEndTime();
        long sampleTime = trace.getSampleTime();

        Calendar currentDay = Calendar.getInstance();
        Calendar generationTime = Calendar.getInstance();
        int previousDay = -1;
        boolean generate = false;
        Random messageRandom = new Random(seed);

        List<Message> messages = new ArrayList<>();

        statistics.runBeforeTraceStart();
        statistics.runBeforeTraceStart();
        for (long tick = startTime; tick < endTime; tick += 10 * sampleTime) {
            double x = (double)(tick - startTime) / (endTime - startTime);
            int count = 0;

            // update battery level
            if (batteryComputation) {
                for (Node node : nodes) {
                    node.updateBatteryLevel();
                }
            }

            for (Node node : nodes) {
                node.onTick(tick, sampleTime);
            }

            currentDay.setTimeInMillis(tick);

            if (currentDay.get(Calendar.DATE) != previousDay) {
                generate = true;
                previousDay = currentDay.get(Calendar.DATE);
                generationTime = statistics.generateMessageTime(messageRandom.nextDouble());
            }

            // generate messages
            if (generate && generationTime.get(Calendar.HOUR) == currentDay.get(Calendar.HOUR)) {
                messages.addAll(statistics.generateMessages(nodes, messageCount, messageCopies, tick, dissemination, messageRandom));
                generate = false;
            }

            for (int i = 0; i < contactCount; i++) {
                Contact contact = trace.getContactAt(i);

                if (contact.getStart() <= tick && contact.getEnd() >= tick) {

                    // there is a contact.
                    count++;

                    Node observer = nodes[contact.getObserver()];
                    Node observed = nodes[contact.getObserved()];

                    long contactDuration = 0;
                    boolean newContact = (contact.getStart() == tick);
                    if (newContact) {
                        contactDuration = contact.getEnd() - contact.getStart() + sampleTime;
                    }

                    // run
                    observer.run(observed, tick, contactDuration, newContact, tick - startTime, sampleTime);
                }
            }

            // remove unused contacts.
            for (int i = count - 1; i >= 0; i--) {
                if (trace.getContactAt(i).getEnd() == tick) {
                    trace.removeContactAt(i);
                }
            }

            contactCount = trace.getContactsCount();
            statistics.runEveryTick(nodes, tick, startTime);
        }

        statistics.runAfterTraceEnd(nodes);

        return messages;
    }

    /**
     * To be overwritten by the subclasses using the desired algorithm
     * @param index
     * @return the initialized node
     */
    public abstract Node initializeNode(int index, long seed, Node[] nodes);

    protected Parser getSigcommParser(){
        return new Sigcomm();
    }

    protected Parser getUPB2012Parser(){
        return new UPB(UPB.UpbTrace.UPB2012);
    }
}
