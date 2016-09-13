/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package mobemu.utils;

import java.util.*;
import mobemu.node.Context;
import mobemu.node.Node;
import mobemu.trace.Contact;
import mobemu.trace.Parser;

/**
 * Helper class for interest analysis.
 *
 * @author Radu
 */
public class InterestsAnalysis {

    private Parser parser = null;
    private Node[] nodes;
    /**
     * key = contacts number, value = interests / max interests
     */
    private Map<Integer, CountHelper> interestsPerContactsCount = new TreeMap<>();
    /**
     * key = contact duration, value = interests / max interests
     */
    private Map<Long, CountHelper> interestsPerContactDuration = new TreeMap<>();
    /**
     * key = inter-contact, value = interests / max interests
     */
    private Map<Long, CountHelper> interestsPerInterContactTime = new TreeMap<>();
    private Map<Integer, CountHelper> interestsPerCommonFriends = new TreeMap<>();
    /**
     * key = number of common friends, value = interests / max interests
     */
    private Map<Integer, CountHelper> interestsPerCommonKclique = new TreeMap<>();
    /**
     * key = number of common k-clique friends, value = interests / max
     * interests
     */
    private Map<IntPair, CountHelper> interestsPerContactsFriends = new HashMap<>();
    /**
     * key = pairs of contacts and common friends, value = interests / max
     * interests
     */
    private Map<IntPair, CountHelper> interestsPerKcliqueFriends = new HashMap<>();
    /**
     * key = pairs of k-clique contacts and common friends, value = interests /
     * max interests
     */
    private CountHelper connectedInterests = new CountHelper(0, 0);
    private CountHelper unconnectedInterests = new CountHelper(0, 0);
    private int maxContacts = Integer.MIN_VALUE;

    /**
     * Instantiates an {@code InterestAnalysis} object.
     *
     * @param parser trace parser object
     * @param nodes array of nodes
     */
    public InterestsAnalysis(Parser parser, Node[] nodes) {
        this.parser = parser;
        this.nodes = nodes;

        precompute();
    }

    /**
     * Prints the distribution of common interests as a function of contacts
     * between node pairs.
     */
    public void analyzeContactsCount() {
        for (Map.Entry<Integer, CountHelper> entry : interestsPerContactsCount.entrySet()) {
            int key = entry.getKey();
            double value = (double) entry.getValue().value / entry.getValue().count;
            System.out.println(key + "," + value);
        }
    }

    /**
     * Prints the distribution of common interests as a function of contact
     * duration between node pairs.
     */
    public void analyzeContactDuration() {
        for (Map.Entry<Long, CountHelper> entry : interestsPerContactDuration.entrySet()) {
            long key = entry.getKey();
            double value = (double) entry.getValue().value / entry.getValue().count;
            System.out.println(key + "," + value);
        }
    }

    /**
     * Prints the distribution of common interests as a function of
     * inter-contact times between node pairs.
     */
    public void analyzeInterContactTime() {
        for (Map.Entry<Long, CountHelper> entry : interestsPerInterContactTime.entrySet()) {
            long key = entry.getKey();
            double value = (double) entry.getValue().value / entry.getValue().count;
            System.out.println(key + "," + value);
        }
    }

    /**
     * Prints the distribution of common interests as a function of social
     * network connections between node pairs.
     */
    public void analyzeSocialNetworkConnections() {
        System.out.println("0," + (double) unconnectedInterests.value / unconnectedInterests.count);
        System.out.println("1," + (double) connectedInterests.value / connectedInterests.count);
    }

    /**
     * Prints the distribution of common interests as a function of common
     * social network friends between node pairs.
     */
    public void analyzeSocialNetworkFriends() {
        for (Map.Entry<Integer, CountHelper> entry : interestsPerCommonFriends.entrySet()) {
            int key = entry.getKey();
            double value = (double) entry.getValue().value / entry.getValue().count;
            System.out.println(key + "," + value);
        }
    }

    /**
     * Prints the distribution of common interests as a function of common
     * k-clique friends between node pairs.
     */
    public void analyzeKclique() {
        for (Map.Entry<Integer, CountHelper> entry : interestsPerCommonKclique.entrySet()) {
            int key = entry.getKey();
            double value = (double) entry.getValue().value / entry.getValue().count;
            System.out.println(key + "," + value);
        }
    }

    /**
     * Prints the distribution of common interests as a function of contacts and
     * social network friends between node pairs.
     */
    public void analyzeContactsFriends() {
        for (Map.Entry<IntPair, CountHelper> entry : interestsPerContactsFriends.entrySet()) {
            IntPair key = entry.getKey();
            double value = (double) entry.getValue().value / entry.getValue().count;
            System.out.println(key.first + "," + key.second + "," + value);
        }
    }

    /**
     * Prints the distribution of common interests as a function of k-clique and
     * social network friends between node pairs.
     */
    public void analyzeKcliqueFriends() {
        for (Map.Entry<IntPair, CountHelper> entry : interestsPerKcliqueFriends.entrySet()) {
            IntPair key = entry.getKey();
            double value = (double) entry.getValue().value / entry.getValue().count;
            System.out.println(key.first + "," + key.second + "," + value);
        }
    }

    /**
     * Pre-computes the analysis data.
     */
    private void precompute() {
        Map<Integer, Context> contextData = parser.getContextData();

        // list of all contacts for each node pair
        Map<NodePair, List<Contact>> nodePairMap = new HashMap<>();

        // populate the map of node pairs
        for (int i = 0; i < parser.getTraceData().getContactsCount(); i++) {
            Contact contact = parser.getTraceData().getContactAt(i);
            NodePair pair = new NodePair(contact.getObserver(), contact.getObserved());

            List<Contact> contactsPerPair = nodePairMap.get(pair);
            if (contactsPerPair == null) {
                contactsPerPair = new ArrayList<>();
            }

            contactsPerPair.add(contact);
            nodePairMap.put(pair, contactsPerPair);
        }

        for (Map.Entry<NodePair, List<Contact>> entry : nodePairMap.entrySet()) {
            NodePair pair = entry.getKey();
            List<Contact> contacts = entry.getValue();

            int contactsCount = contacts.size();
            if (contactsCount > maxContacts) {
                maxContacts = contactsCount;
            }

            long contactDuration = 0;
            long averageInterContactTime = 0;
            int interContactTimeCount = 0;

            // compute contact durations and inter-contact times
            for (int i = 0; i < contacts.size(); i++) {
                Contact contact = contacts.get(i);
                contactDuration += contact.getEnd() - contact.getStart();

                if (i > 0 && contact.getStart() > contacts.get(i - 1).getEnd()) {
                    averageInterContactTime += contact.getStart() - contacts.get(i - 1).getEnd();
                    interContactTimeCount++;
                }
            }

            // compute common topics
            int commonTopics = contextData.get(pair.a).getCommonTopics(contextData.get(pair.b), Long.MAX_VALUE);

            // update the map of interests per contacts
            CountHelper previousValue = interestsPerContactsCount.get(contactsCount);
            if (previousValue == null) {
                interestsPerContactsCount.put(contactsCount, new CountHelper(commonTopics, 1));
            } else {
                previousValue.value += commonTopics;
                previousValue.count++;
                interestsPerContactsCount.put(contactsCount, previousValue);
            }

            // update the map of interests per contact durations
            previousValue = interestsPerContactDuration.get(contactDuration);
            if (previousValue == null) {
                interestsPerContactDuration.put(contactDuration, new CountHelper(commonTopics, 1));
            } else {
                previousValue.value += commonTopics;
                previousValue.count++;
                interestsPerContactDuration.put(contactDuration, previousValue);
            }

            // update the map of interests per inter-contact times
            if (interContactTimeCount > 0) {
                averageInterContactTime /= contacts.size() - 1;

                previousValue = interestsPerInterContactTime.get(averageInterContactTime);
                if (previousValue == null) {
                    interestsPerInterContactTime.put(averageInterContactTime, new CountHelper(commonTopics, 1));
                } else {
                    previousValue.value += commonTopics;
                    previousValue.count++;
                    interestsPerInterContactTime.put(averageInterContactTime, previousValue);
                }
            }

            // compute the common friends
            boolean[][] socialNetwork = parser.getSocialNetwork();
            int commonFriends = 0;
            for (int k = 0; k < socialNetwork[pair.a].length; k++) {
                if (socialNetwork[pair.a][k] && socialNetwork[pair.b][k]) {
                    commonFriends++;
                }
            }

            // update the map of interests per common social network friends
            previousValue = interestsPerCommonFriends.get(commonFriends);
            if (previousValue == null) {
                interestsPerCommonFriends.put(commonFriends, new CountHelper(commonTopics, 1));
            } else {
                previousValue.value += commonTopics;
                previousValue.count++;
                interestsPerCommonFriends.put(commonFriends, previousValue);
            }

            // update the map of interests per social connection
            if (socialNetwork[pair.a][pair.b]) {
                connectedInterests.value += commonTopics;
                connectedInterests.count++;
            } else {
                unconnectedInterests.value += commonTopics;
                unconnectedInterests.count++;
            }

            // update the map of interests per contacts and social network friends
            IntPair contactsFriendsPair = new IntPair(contactsCount, commonFriends);
            previousValue = interestsPerContactsFriends.get(contactsFriendsPair);
            if (previousValue == null) {
                interestsPerContactsFriends.put(contactsFriendsPair, new CountHelper(commonTopics, 1));
            } else {
                previousValue.value += commonTopics;
                previousValue.count++;
                interestsPerContactsFriends.put(contactsFriendsPair, previousValue);
            }

            // compute the common k-clique friends
            int commonNodes = 0;
            for (int i : nodes[pair.a].getLocalCommunity()) {
                for (int j : nodes[pair.b].getLocalCommunity()) {
                    if (i != pair.a && j != pair.b && i == j) {
                        commonNodes++;
                    }
                }
            }

            // update the map of interests per k-clique friends
            previousValue = interestsPerCommonKclique.get(commonNodes);
            if (previousValue == null) {
                interestsPerCommonKclique.put(commonNodes, new CountHelper(commonTopics, 1));
            } else {
                previousValue.value += commonTopics;
                previousValue.count++;
                interestsPerCommonKclique.put(commonNodes, previousValue);
            }

            // update the map of interests per k-clique and social network friends
            IntPair kcliqueFriendsPair = new IntPair(commonNodes, commonFriends);
            previousValue = interestsPerKcliqueFriends.get(kcliqueFriendsPair);
            if (previousValue == null) {
                interestsPerKcliqueFriends.put(kcliqueFriendsPair, new CountHelper(commonTopics, 1));
            } else {
                previousValue.value += commonTopics;
                previousValue.count++;
                interestsPerKcliqueFriends.put(kcliqueFriendsPair, previousValue);
            }
        }
    }

    /**
     * Helper class for storing a total value and the count of elements that
     * form it.
     */
    private class CountHelper {

        /**
         * Total value.
         */
        int value;
        /**
         * Number of elements composing the value.
         */
        int count;

        /**
         * Instantiates a {@code CountHelper} object.
         *
         * @param value total value
         * @param count number of elements composing the value
         */
        public CountHelper(int value, int count) {
            this.value = value;
            this.count = count;
        }
    }

    /**
     * Helper class for a pair of nodes.
     */
    private class NodePair {

        /**
         * First node's ID.
         */
        int a;
        /**
         * The second node's ID.
         */
        int b;

        /**
         * Instantiates a {@code NodePair} object.
         *
         * @param a first node's ID
         * @param b the second node's ID
         */
        public NodePair(int a, int b) {
            this.a = a;
            this.b = b;
        }

        @Override
        public boolean equals(Object obj) {
            if (!(obj instanceof NodePair)) {
                return false;
            }

            NodePair other = (NodePair) obj;

            return (this.a == other.a && this.b == other.b)
                    || (this.a == other.b && this.b == other.a);
        }

        @Override
        public int hashCode() {
            return this.a + this.b;
        }
    }

    /**
     * Helper class for a pair of integers.
     */
    private class IntPair {

        /**
         * First integer.
         */
        int first;
        /**
         * Second integer.
         */
        int second;

        /**
         * Instantiates an {@code IntPair} object.
         *
         * @param first first integer
         * @param second second integer
         */
        public IntPair(int first, int second) {
            this.first = first;
            this.second = second;
        }

        @Override
        public boolean equals(Object obj) {
            if (!(obj instanceof IntPair)) {
                return false;
            }

            IntPair other = (IntPair) obj;

            return (this.first == other.first && this.second == other.second);
        }

        @Override
        public int hashCode() {
            int hash = 7;
            hash = 97 * hash + this.first;
            hash = 97 * hash + this.second;
            return hash;
        }
    }
}
