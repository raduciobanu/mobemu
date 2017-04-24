/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package mobemu.trace;

import java.util.ArrayList;
import java.util.List;

/**
 * Class containing various statistics about a mobility trace.
 *
 * @author Radu
 */
public class TraceStats {

    /**
     * Average contact time.
     */
    public double averageContactTime;
    /**
     * Average inter-contact time.
     */
    public double averageInterContactTime;
    /**
     * Average any-contact time.
     */
    public double averageAnyContactTime;
    /**
     * Average inter-any-contact time.
     */
    public double averageInterAnyContactTime;
    /**
     * Trace the stats are computed for.
     */
    private final Trace trace;
    /**
     * Number of nodes in the trace.
     */
    private final int nodesNumber;

    /**
     * Constructs a {@code TraceStats} object.
     *
     * @param parser parser that these stats are computed for
     */
    public TraceStats(Parser parser) {
        trace = parser.getTraceData();
        nodesNumber = parser.getNodesNumber();

        computeStats();
    }

    /**
     * Computes various tasks for a given trace.
     */
    private void computeStats() {
        computeAverageContactTime();
        computeAverageInterContactTime();
        computeAverageAnyContactTime();
        computeAverageInterAnyContactTime();
    }

    /**
     * Computes and prints the average contact time.
     */
    private void computeAverageContactTime() {
        int[][] contacts = new int[nodesNumber][nodesNumber];
        double[][] contactTimes = new double[nodesNumber][nodesNumber];

        int traceSize = trace.getContactsCount();
        for (int i = 0; i < traceSize; i++) {
            Contact contact = trace.getContactAt(i);

            contacts[contact.getObserver()][contact.getObserved()]++;
            contactTimes[contact.getObserver()][contact.getObserved()] += (contact.getEnd() - contact.getStart());
        }

        int count = 0;
        averageContactTime = 0;
        for (int i = 0; i < nodesNumber; i++) {
            for (int j = 0; j < nodesNumber; j++) {
                if (contacts[i][j] > 0) {
                    count++;
                    averageContactTime += contactTimes[i][j] / contacts[i][j];
                }
            }
        }
        averageContactTime /= count;

        System.out.println("Average contact time: " + averageContactTime);
    }

    /**
     * Computes and prints the average inter-contact time.
     */
    private void computeAverageInterContactTime() {
        // finish time of the last contact between two nodes
        long[][] finishTimes = new long[nodesNumber][nodesNumber];
        int[][] contacts = new int[nodesNumber][nodesNumber];
        double[][] interContactTimes = new double[nodesNumber][nodesNumber];

        int traceSize = trace.getContactsCount();
        for (int i = 0; i < traceSize; i++) {
            Contact contact = trace.getContactAt(i);
            int observer = contact.getObserver();
            int observed = contact.getObserved();

            if (finishTimes[observer][observed] != 0) {
                contacts[observer][observed]++;
                interContactTimes[observer][observed] += contact.getStart() - finishTimes[observer][observed];
            }

            finishTimes[observer][observed] = contact.getEnd();
        }

        int count = 0;
        averageInterContactTime = 0;
        for (int i = 0; i < nodesNumber; i++) {
            for (int j = 0; j < nodesNumber; j++) {
                if (contacts[i][j] > 0) {
                    count++;
                    averageInterContactTime += interContactTimes[i][j] / contacts[i][j];
                }
            }
        }
        averageInterContactTime /= count;

        System.out.println("Average inter-contact time: " + averageInterContactTime);
    }

    /**
     * Computes and prints the average any-contact time.
     */
    private void computeAverageAnyContactTime() {
        averageAnyContactTime = 0.0;
        int count = 0;

        for (int i = 0; i < nodesNumber; i++) {
            List<Contact> contacts = new ArrayList<>();

            int traceSize = trace.getContactsCount();
            for (int j = 0; j < traceSize; j++) {
                Contact contact = trace.getContactAt(j);
                if (contact.getObserver() == i) {
                    contacts.add(contact);
                }
            }

            if (contacts.isEmpty()) {
                continue;
            }

            long start = contacts.get(0).getStart();
            long end = contacts.get(0).getEnd();
            int localCount = 0;
            double localAnyContactTime = 0;

            for (Contact contact : contacts) {
                if (contact.getStart() >= start && contact.getStart() <= end) {
                    end = Math.max(end, contact.getEnd());
                } else {
                    // contact just finished, log it and reset
                    localCount++;
                    localAnyContactTime += end - start;

                    start = contact.getStart();
                    end = contact.getEnd();
                }
            }

            localCount++;
            localAnyContactTime += end - start;
            localAnyContactTime /= localCount;

            averageAnyContactTime += localAnyContactTime;
            count++;
        }
        averageAnyContactTime /= count;

        System.out.println("Average any contact time: " + averageAnyContactTime);
    }

    /**
     * Computes and prints the average inter-any-contact time.
     */
    private void computeAverageInterAnyContactTime() {
        averageInterAnyContactTime = 0.0;
        int count = 0;

        for (int i = 0; i < nodesNumber; i++) {
            List<Contact> contacts = new ArrayList<>();

            int traceSize = trace.getContactsCount();
            for (int j = 0; j < traceSize; j++) {
                Contact contact = trace.getContactAt(j);
                if (contact.getObserver() == i) {
                    contacts.add(contact);
                }
            }

            if (contacts.size() <= 1) {
                continue;
            }

            long end = contacts.get(0).getEnd();
            int localCount = 0;
            double localInterAnyContactTime = 0;
            contacts.remove(0);

            for (Contact contact : contacts) {
                localCount++;
                localInterAnyContactTime += contact.getStart() - end;
                end = contact.getEnd();
            }

            localInterAnyContactTime /= localCount;

            averageInterAnyContactTime += localInterAnyContactTime;
            count++;
        }
        averageInterAnyContactTime /= count;

        System.out.println("Average inter-any contact time: " + averageInterAnyContactTime);
    }
}
