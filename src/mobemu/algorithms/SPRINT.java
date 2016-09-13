/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package mobemu.algorithms;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import mobemu.node.*;

/**
 * Class for a SPRINT node.
 *
 * Radu Ioan Ciobanu, Ciprian Dobre, and Valentin Cristea. SPRINT: Social
 * prediction-based opportunistic routing. In IEEE 14th International Symposium
 * and Workshops on a World of Wireless, Mobile and Multimedia Networks
 * (WoWMoM), pages 1-7, June 2013.
 *
 * @author Radu
 */
public class SPRINT extends Node {

    /**
     * Specifies whether the altruism is analyzed or not.
     */
    private boolean altruismAnalysis;
    /**
     * Size of a node's cache memory.
     */
    private static Integer cacheMemorySize = null;
    /**
     * The cache memory of the current node.
     */
    private List<EncounterInfo> cacheMemory;
    /**
     * Current position in the cache memory.
     */
    private int cacheMemoryPosition;
    /**
     * Probabilities for encountering the other nodes.
     */
    private List<Probability> encounterProbabilities;
    /**
     * Information about all the other nodes in the trace.
     */
    private static Node[] nodes = null;

    /**
     * Instantiates an {@code ONSIDE} object.
     *
     * @param id ID of the node
     * @param context the context of this node
     * @param socialNetwork the social network as seen by this node
     * @param dataMemorySize the maximum allowed size of the data memory
     * @param exchangeHistorySize the maximum allowed size of the exchange
     * history
     * @param seed the seed for the random number generators if routing is used
     * @param traceStart timestamp of the start of the trace
     * @param traceEnd timestamp of the end of the trace
     * @param altruism {@code true} if altruism computations are performed, {@code false}
     * otherwise
     * @param nodes array of all the nodes in the network
     * @param cacheMemorySize size of the cache holding the most recent
     * encounters exchange
     */
    public SPRINT(int id, Context context, boolean[] socialNetwork, int dataMemorySize, int exchangeHistorySize,
            long seed, long traceStart, long traceEnd, boolean altruism, Node[] nodes, int cacheMemorySize) {
        super(id, nodes.length, context, socialNetwork, dataMemorySize, exchangeHistorySize, seed, traceStart, traceEnd);

        this.altruismAnalysis = altruism;

        if (SPRINT.nodes == null) {
            SPRINT.nodes = nodes;
        }

        if (SPRINT.cacheMemorySize == null) {
            SPRINT.cacheMemorySize = cacheMemorySize;
        }

        this.cacheMemory = new ArrayList<>(Collections.nCopies(cacheMemorySize, new EncounterInfo(-1, -1, -1)));
        this.cacheMemoryPosition = 0;
        this.encounterProbabilities = new ArrayList<>(Collections.nCopies(nodes.length, new Probability(0, 0.0)));
    }

    @Override
    public String getName() {
        return "SPRINT";
    }

    /**
     * Data exchange function for SPRINT routing between two nodes.
     *
     * @param encounteredNode encountered node
     * @param contactDuration duration of the contact
     * @param currentTime current trace time
     */
    @Override
    protected void onDataExchange(Node encounteredNode, long contactDuration, long currentTime) {
        if (!(encounteredNode instanceof SPRINT)) {
            return;
        }

        SPRINT sprintEncounteredNode = (SPRINT) encounteredNode;

        int remainingMessages = deliverDirectMessages(sprintEncounteredNode, altruismAnalysis, contactDuration, currentTime, false);
        int totalMessages = 0;

        Calendar today = Calendar.getInstance();
        today.setTimeInMillis(currentTime);
        int hourNow = today.get(Calendar.HOUR_OF_DAY);
        today.set(Calendar.HOUR_OF_DAY, 0);
        today.set(Calendar.MINUTE, 0);
        today.set(Calendar.SECOND, 0);
        int dayNow = today.get(Calendar.DAY_OF_MONTH);

        // compute future encounters array for both nodes
        ArrayList<ArrayList<Probability>> thisFutureEncounters = computeFutureEncounters(this, currentTime, hourNow, dayNow);
        List<Message> tempDataMemory = new ArrayList<>(dataMemorySize);

        // update cache memory
        cacheMemory.set(cacheMemoryPosition, new EncounterInfo(sprintEncounteredNode.id, dayNow, hourNow));
        cacheMemoryPosition = (cacheMemoryPosition + 1) % cacheMemorySize;

        // if we're at the end of the trace, stop
        if (thisFutureEncounters == null) {
            return;
        }

        // compose the extended data memory with messages from my cache, encountered
        // node's cache and encountered node's own messages
        List<Message> extendedDataMemory = new ArrayList<>(3 * dataMemorySize);
        for (Message message : sprintEncounteredNode.dataMemory) {
            if (!extendedDataMemory.contains(message)) {
                extendedDataMemory.add(message);
            }
        }

        for (Message message : dataMemory) {
            if (!extendedDataMemory.contains(message)) {
                extendedDataMemory.add(message);
            }
        }

        for (Message message : sprintEncounteredNode.ownMessages) {
            if (!extendedDataMemory.contains(message)) {
                extendedDataMemory.add(message);
            }
        }

        // compute first-tier utility for the extended data memory
        for (Message message : extendedDataMemory) {
            computeUtility(message, thisFutureEncounters, currentTime);
        }

        // select what messages to download from the encountered node based on
        // what nodes I will encounter and the encountered node will encounter
        // in the next 24 hours
        Collections.sort(extendedDataMemory);

        // copy new messages with positive utility in the data memory
        for (int i = 0; i < Math.min(dataMemorySize, extendedDataMemory.size()); i++) {
            if (totalMessages >= remainingMessages) {
                break;
            }

            Message message = extendedDataMemory.get(i);

            if (message.getUtility() == 0.0f) {
                break;
            }

            if (tempDataMemory.contains(message)) {
                continue;
            }

            if ((sprintEncounteredNode.dataMemory.contains(message)
                    || sprintEncounteredNode.ownMessages.contains(message))
                    && !dataMemory.contains(message)) {

                // compute perceived altruism and decide if the encountered node accepts
                // sending the message to the current node
                if (altruismAnalysis) {
                    if (!checkAltruism(sprintEncounteredNode, message)) {
                        altruism.setSelfishness(true);

                        if (nodes[sprintEncounteredNode.id].inSocialNetwork(message.getSource())
                                || nodes[sprintEncounteredNode.id].inLocalCommunity(message.getSource())) {
                            altruism.increaseLocal();
                        } else {
                            altruism.increaseGlobal();
                        }

                        continue;
                    } else if (!sprintEncounteredNode.altruism.isSelfish()) {
                        altruism.setSelfishness(false);
                    }
                }

                totalMessages++;
            }

            tempDataMemory.add(message);
        }

        ArrayList<Message> toRemove = new ArrayList<>();

        // remain only with messages with 0 utility
        for (Message message : extendedDataMemory) {
            if (message.getUtility() > 0.0f) {
                toRemove.add(message);
            }
        }

        // eliminate messages that don't have the destination in my social
        // community (or in the communities of nodes I'll presumably encounter
        // or k-CLIQUE community)
        for (Message message : extendedDataMemory) {
            if (!(inSocialNetwork(message.getDestination()) || inLocalCommunity(message.getDestination()))
                    && !willEncounterCommunity(thisFutureEncounters, message.getDestination())) {
                toRemove.add(message);
            }
        }

        for (Message message : toRemove) {
            extendedDataMemory.remove(message);
        }

        // sort extended data memory with messages to my community first
        ArrayList<Message> tempExtendedDataMemory = new ArrayList<>(extendedDataMemory.size());
        for (Message message : extendedDataMemory) {
            if (!(inSocialNetwork(message.getDestination()) || inLocalCommunity(message.getDestination()))) {
                tempExtendedDataMemory.add(message);
            }
        }
        for (Message message : extendedDataMemory) {
            if (inSocialNetwork(message.getDestination()) || inLocalCommunity(message.getDestination())) {
                tempExtendedDataMemory.add(message);
            }
        }
        extendedDataMemory = tempExtendedDataMemory;

        for (Message message : extendedDataMemory) {
            computeSecondTierUtility(message, sprintEncounteredNode, currentTime, hourNow);
        }
        Collections.sort(extendedDataMemory);

        // add the remaining messages (or how many fit in) into the temp data memory
        for (int i = 0; i < Math.min(dataMemorySize - tempDataMemory.size(), extendedDataMemory.size()); i++) {
            if (totalMessages >= remainingMessages) {
                break;
            }

            Message message = extendedDataMemory.get(i);

            if (tempDataMemory.contains(message)) {
                continue;
            }

            if ((sprintEncounteredNode.dataMemory.contains(message)
                    || sprintEncounteredNode.ownMessages.contains(message))
                    && !dataMemory.contains(message)) {

                // compute perceived altruism and decide if the encountered node accepts
                // sending the message to the current node
                if (altruismAnalysis) {
                    if (!checkAltruism(sprintEncounteredNode, message)) {
                        altruism.setSelfishness(true);

                        if (nodes[sprintEncounteredNode.id].inSocialNetwork(message.getSource())
                                || nodes[sprintEncounteredNode.id].inLocalCommunity(message.getSource())) {
                            altruism.increaseLocal();
                        } else {
                            altruism.increaseGlobal();
                        }

                        continue;
                    } else if (!sprintEncounteredNode.altruism.isSelfish()) {
                        altruism.setSelfishness(false);
                    }
                }

                totalMessages++;
            }

            tempDataMemory.add(message);
        }

        // if there's still room in the data memory, add some of the previously removed nodes
        while (tempDataMemory.size() < dataMemorySize && toRemove.size() > 0) {
            if (totalMessages >= remainingMessages) {
                break;
            }

            Message message = toRemove.remove(toRemove.size() - 1);

            if (tempDataMemory.contains(message)) {
                continue;
            }

            if ((sprintEncounteredNode.dataMemory.contains(message)
                    || sprintEncounteredNode.ownMessages.contains(message))
                    && !dataMemory.contains(message)) {

                // compute perceived altruism and decide if the encountered node accepts
                // sending the message to the current node
                if (altruismAnalysis) {
                    if (!checkAltruism(sprintEncounteredNode, message)) {
                        altruism.setSelfishness(true);

                        if (nodes[sprintEncounteredNode.id].inSocialNetwork(message.getSource())
                                || nodes[sprintEncounteredNode.id].inLocalCommunity(message.getSource())) {
                            altruism.increaseLocal();
                        } else {
                            altruism.increaseGlobal();
                        }

                        continue;
                    } else if (!sprintEncounteredNode.altruism.isSelfish()) {
                        altruism.setSelfishness(false);
                    }
                }

                totalMessages++;
            }

            tempDataMemory.add(message);
        }

        toRemove.clear(); // toRemove -> all messages from the data memory not in the temp data memory
        for (Message message : dataMemory) {
            if (!tempDataMemory.contains(message)) {
                toRemove.add(message);
            }
        }
        dataMemory.removeAll(toRemove);
        tempDataMemory.removeAll(dataMemory);

        for (Message message : tempDataMemory) {
            insertMessage(message, encounteredNode, currentTime, altruismAnalysis, false);
        }

        for (int i = toRemove.size() - 1; i >= 0; i--) {
            if (dataMemory.size() < dataMemorySize) {
                dataMemory.add(toRemove.get(i));
            }
        }
    }

    /**
     * Checks the altruism of a given message with regard to a carrier and a
     * potential receiver.
     *
     * @param encounteredNode carrier node
     * @param message message to be analyzed
     * @return {@code true} if the message is to be transferred, {@code false}
     * otherwise
     */
    private boolean checkAltruism(SPRINT encounteredNode, Message message) {
        double perceivedAltruism = 0.0;
        double down = 0.0;
        int messageSource = message.getSource();

        for (ExchangeHistory sent : encounteredNode.exchangeHistorySent) {
            for (ExchangeHistory received : encounteredNode.exchangeHistoryReceived) {
                if (sent.getMessage().getId() == received.getMessage().getId()) {

                    int source = sent.getMessage().getSource();
                    double currentValue = (nodes[messageSource].inSocialNetwork(source)
                            || nodes[messageSource].inLocalCommunity(source)) ? 1 : 0;
                    if (currentValue == 0) {
                        continue;
                    }

                    currentValue *= (id == sent.getNodeSeen()) ? 1 : 0;

                    if (currentValue == 0) {
                        continue;
                    } else {
                        down++;
                    }

                    currentValue *= (id == received.getNodeSeen()) ? 1.0 : 0.0;
                    currentValue *= received.getExchangeTime() > sent.getExchangeTime() ? 1.0 : 0.0;

                    if (sent.getBattery() <= Altruism.getMaxBatteryThreshold() * Battery.getMaxLevel()) {
                        currentValue = 1.0;
                    }

                    perceivedAltruism += currentValue;
                }
            }
        }

        if (down == 0) {
            perceivedAltruism = (inSocialNetwork(messageSource)
                    || inLocalCommunity(messageSource)) ? Altruism.getTrustThreshold() + 1 : 0;
        } else {
            perceivedAltruism /= down;
        }

        return !(perceivedAltruism < Altruism.getTrustThreshold()
                && !(inSocialNetwork(encounteredNode.id) || inLocalCommunity(encounteredNode.id)));
    }

    /**
     * Checks if a message's destination is in the social community with a node
     * that will be met.
     *
     * @param futureEncounters array of nodes that are to be encountered in the
     * next 24 hours
     * @param destination destination node of the message
     * @return {@code true} if the destination is in the social community, {@code false}
     * otherwise
     */
    private boolean willEncounterCommunity(ArrayList<ArrayList<Probability>> futureEncounters, int destination) {
        for (ArrayList<Probability> encounter : futureEncounters) {
            for (Probability probability : encounter) {
                if (nodes[probability.id].inSocialNetwork(destination)
                        || nodes[probability.id].inLocalCommunity(destination)) {
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * Computes the future encounters for the next 24 hours.
     *
     * @param node node to compute encounters for
     * @param currentTime current trace time
     * @param hourNow current hour of the day
     * @param dayNow current day of the week
     * @return array of future encounters per hour for 24 hours
     */
    private static ArrayList<ArrayList<Probability>> computeFutureEncounters(SPRINT node, long currentTime, int hourNow, int dayNow) {
        int devices = node.encounterProbabilities.size();

        // reset probabilities list
        node.encounterProbabilities.clear();
        for (int i = 0; i < devices; i++) {
            node.encounterProbabilities.add(new Probability(i, 0.0));
        }

        // compute initial probabilities values
        double totalEncountersInCache = 0;
        for (EncounterInfo encounterInfo : node.cacheMemory) {
            if (encounterInfo.node != -1) {
                totalEncountersInCache++;
            }
        }

        // cycle through all devices
        for (int i = 0; i < devices; i++) {
            // check the cache
            for (int j = 0; j < cacheMemorySize; j++) {
                if (i == node.cacheMemory.get(j).node) {
                    node.encounterProbabilities.get(i).add(1);

                    // increase probability for those device that have been met in the same day/2-hour interval
                    if (node.cacheMemory.get(j).day % 7 == dayNow % 7) {
                        totalEncountersInCache++;
                        node.encounterProbabilities.get(i).add(1);
                    }

                    if (node.cacheMemory.get(j).hour >= hourNow - 2 && node.cacheMemory.get(j).hour <= hourNow + 2) {
                        totalEncountersInCache++;
                        node.encounterProbabilities.get(i).add(1);
                    }
                }
            }
        }

        // cycle through all devices
        for (int i = 0; i < node.encounterProbabilities.size(); i++) {
            node.encounterProbabilities.get(i).divideBy(totalEncountersInCache);
        }

        // double the chance of meeting a node if it's in my community
        for (int i = 0; i < node.encounterProbabilities.size(); i++) {
            if (node.inSocialNetwork(i) || node.inLocalCommunity(i)) {
                node.encounterProbabilities.get(i).multiplyBy(2);
            }
        }

        // renormalize values
        double probabilitiesSum = 0;
        for (int i = 0; i < node.encounterProbabilities.size(); i++) {
            probabilitiesSum += node.encounterProbabilities.get(i).probability;
        }
        for (int i = 0; i < node.encounterProbabilities.size(); i++) {
            node.encounterProbabilities.get(i).divideBy(probabilitiesSum);
        }

        // compute how many nodes I'm expected to encounter this hour (as well as the next 24)
        ArrayList<ArrayList<Probability>> totalFutureEncounters = new ArrayList<>(HOURS_IN_DAY);
        for (int hour = 0; hour < HOURS_IN_DAY; hour++) {
            Calendar day = Calendar.getInstance();
            day.setTimeInMillis(currentTime);
            day.add(Calendar.HOUR_OF_DAY, hour);

            // get hour
            int currentHour = day.get(Calendar.HOUR_OF_DAY);

            // get day and weekday
            day.set(Calendar.HOUR_OF_DAY, 0);
            day.set(Calendar.MINUTE, 0);
            day.set(Calendar.SECOND, 0);

            int currentDay = (int) (currentTime - traceStartReset) / MILLIS_IN_DAY;
            int currentWeekday = currentDay % 7;

            // knowing the hour and the day, compute the max likelihood value for previous weekdays (if any)
            double maxLikelihood = 0;
            for (int i = 0; i < currentDay / DAYS_IN_WEEK + 1; i++) {
                for (int dev = 0; dev < devices; dev++) {
                    maxLikelihood += node.encountersPerHour[currentHour][currentWeekday + i * DAYS_IN_WEEK][dev];
                }
            }
            maxLikelihood /= (double) ((int) (currentDay / DAYS_IN_WEEK + 1));

            // compute Poisson values for meeting 0, 1, ... devices and see how many I will meet in the current hour
            double poisson;
            double maxPoissonValue = -1;
            int devicesToMeet = -1;
            for (int i = 0; i <= devices; i++) {
                poisson = Math.pow(Math.E, -maxLikelihood) * Math.pow(maxLikelihood, i) / factorial(i);

                if (poisson > maxPoissonValue) {
                    maxPoissonValue = poisson;
                    devicesToMeet = i;
                }
            }

            // now that I know I will meet N devices, select the best N (in terms of probability)
            ArrayList<Probability> futureEncounters = new ArrayList<>(devicesToMeet);
            Collections.sort(node.encounterProbabilities);
            for (int i = 0; i < devicesToMeet; i++) {
                futureEncounters.add(node.encounterProbabilities.get(i));
            }

            // normalize the future encounters array too
            probabilitiesSum = 0;
            for (int i = 0; i < futureEncounters.size(); i++) {
                probabilitiesSum += futureEncounters.get(i).probability;
            }
            for (int i = 0; i < futureEncounters.size(); i++) {
                futureEncounters.get(i).divideBy(probabilitiesSum);
            }

            // add to total array
            totalFutureEncounters.add(futureEncounters);
        }

        return totalFutureEncounters;
    }

    /**
     * Compute the utility of a message when running the SPRINT algorithm.
     *
     * @param message message that the utility will be computed for
     * @param futureEncounters the list of future encounters for the current
     * node
     * @param currentTime current trace time
     */
    private void computeUtility(Message message, ArrayList<ArrayList<Probability>> futureEncounters, long currentTime) {
        double utility = 0.0;
        message.setUtility(0.0f);

        // i -> what hour will I meet the devices at
        for (int i = 0; i < futureEncounters.size(); i++) {
            ArrayList<Probability> probabilityList = futureEncounters.get(i);

            for (Probability prob : probabilityList) {
                if (prob.id == message.getDestination()) {
                    utility += prob.probability;
                    break;
                }
            }

            if (utility > 0.0) {
                break;
            }
        }

        message.setUtility((float) utility);

        if (currentTime - message.getTimestamp() < MILLIS_IN_DAY) {
            message.setUtility(message.getUtility() + 0.5f);
        }
    }

    /**
     * Compute the utility of a message when running the SPRINT algorithm for
     * the nodes in the second tier.
     *
     * @param message message that the utility will be computed for
     * @param encounteredNode encountered node
     * @param currentTime current trace time
     * @param currentHour current hour of the day
     */
    private void computeSecondTierUtility(Message message, SPRINT encounteredNode, long currentTime, int currentHour) {
        double utility = 0.0;

        message.setUtility(0.0f);

        if (!(nodes[message.getSource()].inSocialNetwork(message.getDestination())
                || nodes[message.getSource()].inLocalCommunity(message.getDestination()))) {
            utility++;
        }


        if (message.getHopCount(message.getDestination()) > 2) {
            utility++;
        }

        utility += (double) messagesDelivered / (messagesReceived + ownMessages.size());

        int thisCount = 0;
        int encounteredCount = 0;

        for (int i = 0; i < socialNetwork.length; i++) {
            if (inSocialNetwork(i) || inLocalCommunity(i)) {
                thisCount++;
            }

            if (nodes[encounteredNode.id].inSocialNetwork(i)
                    || nodes[encounteredNode.id].inLocalCommunity(i)) {
                encounteredCount++;
            }
        }

        if (encounteredCount < thisCount) {
            utility++;
        }

        thisCount = 0;
        for (int i = 0; i < encounters.length; i++) {
            if (dataMemory.contains(message)) {
                thisCount += encounters[i];
            } else {
                thisCount += encounteredNode.encounters[i];
            }
        }

        if (currentTime > 3 * MILLIS_IN_DAY + traceStartReset && thisCount < 3) {
            utility = 0.0f;
        }

        utility /= 5.0f;


        int currentDay = (int) (currentTime - traceStartReset) / MILLIS_IN_DAY;

        float maxTime = 0;
        float totalTime = 0;
        for (int i = 0; i < currentDay / DAYS_IN_WEEK + 1; i++) {
            for (int dev = 0; dev < nodes.length; dev++) {
                if (timesPerHour[currentHour][i][dev] > maxTime) {
                    maxTime = timesPerHour[currentHour][i][dev];
                }
            }
            totalTime += timesPerHour[currentHour][i][message.getDestination()];
        }
        totalTime /= (float) ((int) (currentDay / 7 + 1));
        utility += totalTime / maxTime;

        message.setUtility((float) utility);
    }

    /**
     * Computes the factorial of a number.
     *
     * @param n number whose factorial is to be computed
     * @return factorial of n
     */
    public static double factorial(double n) {
        double factorial = 1.0;

        if (n <= 1.0) {
            return 1.0;
        }

        for (double i = 1.0; i <= n; i++) {
            factorial *= i;
        }

        return factorial;
    }

    /**
     * Class used for storing information about node encounters in a
     * SPRINT-based opportunistic network.
     */
    private static class EncounterInfo {

        private int node;
        private int day;
        private int hour;

        /**
         * Creates an {@code EncounterInfo} object.
         *
         * @param node encountered node
         * @param day day the encountered took place
         * @param hour hour of the day the encounter took place
         */
        public EncounterInfo(int node, int day, int hour) {
            this.node = node;
            this.day = day;
            this.hour = hour;
        }

        /**
         * Sets the encountered node.
         *
         * @param node encountered node
         */
        public void setNode(int node) {
            this.node = node;
        }

        /**
         * Sets the day of the encounter.
         *
         * @param day day of the encounter
         */
        public void setDay(int day) {
            this.day = day;
        }

        /**
         * Sets the hour of the encounter.
         *
         * @param hour hour of the encounter
         */
        public void setHour(int hour) {
            this.hour = hour;
        }
    }

    /**
     * Class for storing the probability of encountering a certain node in a
     * SPRINT-based network.
     */
    private static class Probability implements Comparable<Probability> {

        private int id;
        private double probability;

        /**
         * Creates a {@link Probability} object.
         *
         * @param id ID of the node
         * @param probability probability of encountering the node with the
         * given ID
         */
        public Probability(int id, double probability) {
            this.id = id;
            this.probability = probability;
        }

        /**
         * Adds a value to the current probability.
         *
         * @param nr value to be added
         */
        public void add(double nr) {
            this.probability += nr;
        }

        /**
         * Divides the current probability by a given value.
         *
         * @param nr value to divide by
         */
        public void divideBy(double nr) {
            this.probability /= nr;
        }

        /**
         * Multiplies the current probability with a given value.
         *
         * @param nr value to multiply with
         */
        public void multiplyBy(double nr) {
            this.probability *= nr;
        }

        @Override
        public int compareTo(Probability probabilityObject) {
            double diff = this.probability - probabilityObject.probability;

            if (diff > 0) {
                return -1;
            } else if (diff < 0) {
                return 1;
            } else {
                return 0;
            }
        }
    }
}
