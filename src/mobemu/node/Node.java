/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package mobemu.node;

/**
 * Class for a mobile node in an opportunistic network.
 *
 * @author Radu
 */
import java.util.*;

/**
 * Class for a mobile node in an opportunistic network.
 *
 * @author Radu
 */
public abstract class Node {

    protected static int contactThreshold = 20 * 60 * 1000; // contact threshold for the K-clique algorithm
    protected static int communityThreshold = 6; // community threshold for the K-clique algorithm.
    protected static Random deliveryRandom = null; // random number generator for node delivery
    protected static Random batteryRandom = null; // random number generator for battery levels
    protected static Random altruismRandom = null; // random number generator for altruism values
    protected int id; // ID of the current node
    protected int dataMemorySize; // size of a node's data memory
    protected List<Message> dataMemory; // the data memory of the current node
    protected List<Message> ownMessages; // list of messages generated by the current node
    protected Map<Integer, ContactInfo> encounteredNodes; // list of nodes encountered by the current node
    protected int[][][] encountersPerHour; // how many times a node has encountered the other nodes per hour
    protected long[][][] timesPerHour; // how much time a node has been in contact with the other nodes per hour
    protected Map<Integer, ExchangeStats> exchangeStats; // aggregation statistics
    protected boolean[] familiarSet; // the familiar set of the current node
    protected int familiarSetSize; // size of the current node's familiar set
    protected List<Integer> localCommunity; // the local community of the current node
    protected boolean[][] globalFamiliarSet; // the global familiar set of the current node
    protected Battery battery; // the amount of battery left for the device
    protected int messagesDelivered; // total number of messages ever delivered by the current node
    protected int messagesExchanged; // total number of message exchanged by the current node
    protected Centrality centrality; // the node's centrality
    protected Centrality localCentrality; // the node's local centrality
    protected List<Integer> uniqueNodes; // list of unique nodes encountered per time unit
    protected List<Integer> uniqueLocalNodes; // list of unique local nodes encountered per time unit
    protected int overflowCount; // count of overflow events
    protected int messagesReceived; // total number of messages received by the current node
    protected Altruism altruism; // the node's altruism information
    protected List<ExchangeHistory> exchangeHistorySent; // list of sent exchange information
    protected List<ExchangeHistory> exchangeHistoryReceived; // list of received exchange information
    protected int exchangeHistorySize; // maximum exchange history size
    protected int[] encounters; // how many times a node has encountered the other nodes
    protected Network network; // the current node's network information
    protected Context context; // the node's context
    protected boolean[] socialNetwork; // social network of the node
    protected static Long traceStart = null; // timestamp of the start of the trace
    protected static Long traceEnd = null; // timestamp of the end of the trace
    protected static Long traceStartReset = null; // reset timestamp of the start of the trace
    protected static Long traceEndReset = null; // reset timestamp of the end of the trace
    protected static final int HOURS_IN_DAY = 24; // total number of hours in a day
    protected static final int DAYS_IN_WEEK = 7; // total number of days in a week
    protected static final int MILLIS_IN_DAY = 1000 * 60 * 60 * 24; // total number of milliseconds in a day

    /**
     * Constructor for the {@link Node} class.
     *
     * @param id ID of the node
     * @param nodes total number of existing nodes
     * @param context the context of this node
     * @param socialNetwork the social network as seen by this node
     * @param dataMemorySize the maximum allowed size of the data memory
     * @param exchangeHistorySize the maximum allowed size of the exchange
     * history
     * @param seed the seed for the random number generators
     * @param traceStart timestamp of the start of the trace
     * @param traceEndtimestamp of the end of the trace
     */
    public Node(int id, int nodes, Context context, boolean[] socialNetwork, int dataMemorySize, int exchangeHistorySize, long seed, long traceStart, long traceEnd) {
        if (deliveryRandom == null) {
            deliveryRandom = new Random(seed);
        }

        if (batteryRandom == null) {
            batteryRandom = new Random(seed);
        }

        if (altruismRandom == null) {
            altruismRandom = new Random(seed);
        }

        this.id = id;
        this.socialNetwork = socialNetwork;
        this.dataMemorySize = dataMemorySize;

        if (dataMemorySize == Integer.MAX_VALUE) {
            this.dataMemory = new ArrayList<>();
            this.ownMessages = new ArrayList<>();
        } else {
            this.dataMemory = new ArrayList<>(dataMemorySize);
            this.ownMessages = new ArrayList<>(dataMemorySize);
        }

        if (Node.traceStart == null) {
            Node.traceStart = traceStart;
            Node.traceEnd = traceEnd;

            Calendar calendar = Calendar.getInstance();
            calendar.setTimeInMillis(traceStart);
            calendar.set(Calendar.HOUR_OF_DAY, 0);
            calendar.set(Calendar.MINUTE, 0);
            calendar.set(Calendar.SECOND, 0);
            calendar.set(Calendar.MILLISECOND, 0);
            Node.traceStartReset = calendar.getTimeInMillis();

            calendar.setTimeInMillis(traceEnd);
            calendar.set(Calendar.HOUR_OF_DAY, 23);
            calendar.set(Calendar.MINUTE, 59);
            calendar.set(Calendar.SECOND, 59);
            calendar.set(Calendar.MILLISECOND, 999);
            Node.traceEndReset = calendar.getTimeInMillis();
        }

        this.encounteredNodes = new HashMap<>();
        this.encountersPerHour = new int[HOURS_IN_DAY][(int) ((double) (traceEndReset - traceStartReset) / MILLIS_IN_DAY) + 1][nodes];
        this.timesPerHour = new long[HOURS_IN_DAY][(int) ((double) (traceEndReset - traceStartReset) / MILLIS_IN_DAY) + 1][nodes];
        this.exchangeStats = new HashMap<>();
        this.familiarSet = new boolean[nodes];
        this.familiarSetSize = 0;
        this.localCommunity = new ArrayList<>(nodes);
        this.localCommunity.add(id);
        this.globalFamiliarSet = new boolean[nodes][nodes];
        this.battery = new Battery(batteryRandom.nextDouble() * 24.0 * 3600.0, 24.0 * 3600.0, 3600, 0.2);
        this.messagesDelivered = 0;
        this.messagesExchanged = 0;
        this.centrality = new Centrality();
        this.localCentrality = new Centrality();
        this.uniqueNodes = new ArrayList<>();
        this.uniqueLocalNodes = new ArrayList<>();
        this.overflowCount = 0;
        this.messagesReceived = 0;
        this.altruism = new Altruism(altruismRandom.nextDouble(), altruismRandom.nextDouble(), this.socialNetwork);
        this.exchangeHistorySize = exchangeHistorySize;
        this.exchangeHistorySent = new ArrayList<>(exchangeHistorySize);
        this.exchangeHistoryReceived = new ArrayList<>(exchangeHistorySize);
        this.encounters = new int[nodes];
        this.network = new Network();
        this.context = context;
    }

    /**
     * Returns the name of the dissemination or routing algorithm this node is
     * running.
     *
     * @return name of the algorithm
     */
    public abstract String getName();

    /**
     * Gets the ID of the node.
     *
     * @return ID of the node
     */
    public int getId() {
        return id;
    }

    /**
     * Gets the context of the node.
     *
     * @return the context of the node
     */
    public Context getContext() {
        return context;
    }

    /**
     * Gets the current size of the data memory.
     *
     * @return the current data memory size
     */
    public int getDataMemorySize() {
        return dataMemory.size();
    }

    /**
     * Gets the maximum size of the data memory.
     *
     * @return the maximum data memory size
     */
    public int getMaximumDataMemorySize() {
        return dataMemorySize;
    }

    /**
     * Gets the total number of messages exchanged.
     *
     * @return number of messages exchanged
     */
    public int getMessagesExchanged() {
        return messagesExchanged;
    }

    /**
     * Return this node's battery information.
     *
     * @return the battery information of this node
     */
    public Battery getBattery() {
        return battery;
    }

    /**
     * Checks if the encountered node is in the familiar set of the current
     * node.
     *
     * @param id ID of the encountered node
     * @return {@code true} if the encountered node is in the familiar set, {@code false}
     * otherwise
     */
    public boolean inFamiliarSet(int id) {
        return familiarSet[id];
    }

    /**
     * Checks if the encountered node is in the local community of the current
     * node.
     *
     * @param id ID of the encountered node
     * @return {@code true} if the encountered node is in the local community, {@code false}
     * otherwise
     */
    public boolean inLocalCommunity(int id) {
        return localCommunity.contains(id);
    }

    /**
     * Checks if the encountered node is in the social network of the current
     * node.
     *
     * @param id ID of the encountered node
     * @return {@code true} if the encountered node is in the social network, {@code false}
     * otherwise
     */
    public boolean inSocialNetwork(int id) {
        return socialNetwork[id];
    }

    /**
     * Updates the global familiar set for the encountered node.
     *
     * @param node encountered node
     * @param global {@code true} for updating the global familiar set, {@code false}
     * for updating the local familiar set
     */
    public void updateFamiliarSet(Node node, boolean global) {

        if (global) {
            for (int i = 0; i < globalFamiliarSet.length; i++) {
                for (int j = 0; j < globalFamiliarSet[i].length; j++) {
                    globalFamiliarSet[i][j] |= node.globalFamiliarSet[i][j];
                }
            }
        } else {
            for (int j = 0; j < globalFamiliarSet[node.id].length; j++) {
                globalFamiliarSet[node.id][j] |= node.familiarSet[j];
            }
        }
    }

    /**
     * Updates the number of encounters with other nodes.
     *
     * @param id ID of the encountered node
     * @param currentTime current trace time
     */
    public void updateEncounters(int id, long currentTime) {
        Calendar day = Calendar.getInstance();
        day.setTimeInMillis(currentTime);
        int hour = day.get(Calendar.HOUR_OF_DAY);

        encountersPerHour[hour][(int) ((double) (currentTime - traceStart) / MILLIS_IN_DAY)][id]++;
    }

    /**
     * Updates the contact duration for the encountered node. If the node hasn't
     * been encountered before, add a new entry to the list of contacts.
     *
     * @param id ID of the encountered node
     * @param sampleTime number of milliseconds the trace is sampled in
     * @param currentTime current trace time
     */
    public void updateContactDuration(int id, long sampleTime, long currentTime) {
        ContactInfo info = encounteredNodes.get(id);
        if (info != null) {
            info.increaseDuration(sampleTime);
        } else {
            encounteredNodes.put(id, new ContactInfo(currentTime));
        }
    }

    /**
     * Updates the number of contacts with the encountered node. If the node
     * hasn't been encountered before, add a new entry to the list of contacts.
     *
     * @param id ID of the encountered node
     * @param currentTime current trace time
     */
    public void updateContactsNumber(int id, long currentTime) {
        encounters[id]++;

        ContactInfo info = encounteredNodes.get(id);
        if (info != null) {
            info.increaseContacts();
            info.setLastEncounterTime(currentTime);
        } else {
            encounteredNodes.put(id, new ContactInfo(currentTime));
        }
    }

    /**
     * Updated the time spent in contact with other nodes.
     *
     * @param id ID of the encountered node
     * @param currentTime current trace time
     */
    public void updateTimes(int id, long currentTime) {
        Calendar day = Calendar.getInstance();
        day.setTimeInMillis(currentTime);
        int hour = day.get(Calendar.HOUR_OF_DAY);

        timesPerHour[hour][(int) ((double) (currentTime - traceStart) / MILLIS_IN_DAY)][id]++;
    }

    /**
     * Computes the number of contacts with a node.
     *
     * @param id ID of the requested node
     * @return number of contacts with the requested node
     */
    public int getContactsNumber(int id) {
        ContactInfo info = encounteredNodes.get(id);
        if (info != null) {
            return info.getContacts();
        }

        return 0;
    }

    /**
     * Updates the battery level of the current node.
     */
    public void updateBatteryLevel() {
        battery.updateBatteryLevel();
    }

    /**
     * Checks if the contact duration threshold has been exceeded and adds the
     * encountered node to the local community if it has.
     *
     * @param id ID of the encountered node
     */
    public void checkThreshold(int id) {
        ContactInfo node = encounteredNodes.get(id);

        if (node != null) {
            if (node.getDuration() > contactThreshold) {
                if (!inFamiliarSet(id)) {
                    familiarSet[id] = true;
                    familiarSetSize++;
                }

                if (!inLocalCommunity(id)) {
                    localCommunity.add(id);
                }
            }
        }
    }

    /**
     * Updates the local community.
     *
     * @param encounteredNode encountered node
     */
    public void updateLocalCommunity(Node encounteredNode) {

        int count = 0;

        for (Integer localNode : localCommunity) {
            if (encounteredNode.familiarSet[localNode]) {
                count++;
            }
        }

        if (count >= communityThreshold - 1) {
            localCommunity.add(encounteredNode.id);
        }
    }

    /**
     * Aggressively updates the local community.
     *
     * @param encounteredNode encountered node
     */
    public void updateLocalCommunityAggressive(Node encounteredNode) {

        for (Integer newID : encounteredNode.localCommunity) {
            int count = 0;

            for (int i = 0; i < globalFamiliarSet[newID].length; i++) {
                if (globalFamiliarSet[newID][i] && inLocalCommunity(i)) {
                    count++;
                }
            }

            if (count >= communityThreshold - 1) {
                if (!inLocalCommunity(newID)) {
                    localCommunity.add(newID);
                }
            }
        }
    }

    /**
     * Generates a message in the node's own memory.
     *
     * @param message the message to be generated
     */
    public Message generateMessage(Message message) {
        ownMessages.add(message);
        return message;
    }

    /**
     * Inserts a message in the node's data memory.
     *
     * @param message the message to be inserted
     * @param from the node that delivers this message
     * @param currentTime the time this exchange is performed at
     * @param altruism {@code true} if altruism computations are performed, {@code false}
     * otherwise
     * @param dissemination {@code true} if dissemination is used, {@code false}
     * if routing is used
     * @return {@code true} if the message wasn't already in the data memory, {@code false}
     * if it was
     */
    public boolean insertMessage(Message message, Node from, long currentTime, boolean altruism, boolean dissemination) {
        if (dataMemory.contains(message) || ownMessages.contains(message)) {
            return false;
        }

        // increase total number of messages delivered
        messagesExchanged++;

        if (altruism && isSelfish(this, message, currentTime, dissemination)) {
            return false;
        }

        // update the history of exchanged data
        exchangeHistoryReceived.add(new ExchangeHistory(currentTime, message, from.id, id, from.battery.getCurrentLevel()));
        from.exchangeHistorySent.add(new ExchangeHistory(currentTime, message, id, from.id, battery.getCurrentLevel()));

        if (dataMemorySize != Integer.MAX_VALUE && dataMemory.size() == dataMemorySize) {
            overflowCount++;
            Message removed = dataMemory.remove(0);
            removed.deleteCopies(id);
        }

        message.copy(from.id, id);
        dataMemory.add(message);
        messagesReceived++;

        // if message had not already been delivered, increase its hop count
        int destination = message.getDestination();
        if (destination == Message.DISSEMINATION_ID) {
            for (int i = 0; i < encounters.length; i++) {
                message.increaseHopCount(i);
            }
        } else {
            message.increaseHopCount(destination);
        }

        return true;
    }

    /**
     * Performs the data exchange between two encountering nodes.
     *
     * @param encounteredNode the encountered node
     * @param contactDuration the duration of the contact
     * @param currentTime the current time of the trace
     */
    public void exchangeData(Node encounteredNode, long contactDuration, long currentTime) {
        ExchangeStats currentStats = this.exchangeStats.get(encounteredNode.id);
        ExchangeStats encounteredStats = encounteredNode.exchangeStats.get(this.id);

        if (currentStats == null) {
            this.exchangeStats.put(encounteredNode.id, new ExchangeStats());
            currentStats = this.exchangeStats.get(encounteredNode.id);
        }

        if (encounteredStats == null) {
            encounteredNode.exchangeStats.put(this.id, new ExchangeStats());
            encounteredStats = encounteredNode.exchangeStats.get(this.id);
        }

        if (currentStats.getLastExchangeTime() + currentStats.getLastExchangeDuration() <= currentTime) {
            currentStats.setLastExchangeDuration(contactDuration);
            currentStats.setLastExchangeTime(currentTime);

            encounteredStats.setLastExchangeDuration(contactDuration);
            encounteredStats.setLastExchangeTime(currentTime);

            if (this.battery.canParticipate() && encounteredNode.battery.canParticipate()) {
                exchangeHistory(encounteredNode);
                encounteredNode.exchangeHistory(this);

                computeCentrality(encounteredNode, currentTime - Node.traceStart);
                encounteredNode.computeCentrality(this, currentTime - Node.traceStart);

                preDataExchange(encounteredNode, currentTime);

                onDataExchange(encounteredNode, contactDuration, currentTime);
                encounteredNode.onDataExchange(this, contactDuration, currentTime);
            }
        }
    }

    protected void preDataExchange(Node encounteredNode, long currentTime) {
    }

    /**
     * Callback for data exchange between two encountering nodes. Implement this
     * method for your routing/dissemination algorithm.
     *
     * @param encounteredNode the encountered node
     * @param contactDuration the duration of the contact
     * @param currentTime the current time of the trace
     */
    abstract protected void onDataExchange(Node encounteredNode, long contactDuration, long currentTime);

    /**
     * Downloads the history of received and sent messages with an encountered
     * node.
     *
     * @param from node that sends history
     */
    protected void exchangeHistory(Node from) {
        for (ExchangeHistory exInfo : from.exchangeHistoryReceived) {
            if (!exchangeHistoryReceived.contains(exInfo)) {
                exchangeHistoryReceived.add(exInfo);
            }
        }

        Collections.sort(exchangeHistoryReceived);

        while (exchangeHistoryReceived.size() > exchangeHistorySize) {
            exchangeHistoryReceived.remove(0);
        }

        for (ExchangeHistory exInfo : from.exchangeHistorySent) {
            if (!exchangeHistorySent.contains(exInfo)) {
                exchangeHistorySent.add(exInfo);
            }
        }

        Collections.sort(exchangeHistorySent);

        while (exchangeHistorySent.size() > exchangeHistorySize) {
            exchangeHistorySent.remove(0);
        }
    }

    /**
     * Delivers messages directed to the current node.
     *
     * @param encounteredNode node containing the messages
     * @param altruism set to {@code true} if an altruism algorithm is used
     * @param contactDuration duration of the contact
     * @param currentTime the current time of the trace
     * @param dissemination {@code true} if dissemination is used, {@code false}
     * if routing is used
     * @return total number of messages still available for transfer
     */
    protected int deliverDirectMessages(Node encounteredNode, boolean altruism, long contactDuration, long currentTime, boolean dissemination) {
        List<Message> messagesForMe = new ArrayList<>();
        int maxMessages = network.computeMaxMessages(contactDuration);
        int totalMessages = 0;

        /*
         * for each of the messages carried by the encountered node, download
         * the ones that are intended for the current node
         */
        if (!altruism) {
            for (Message message : encounteredNode.dataMemory) {
                if (totalMessages >= maxMessages) {
                    break;
                }

                boolean condition = dissemination
                        ? (context.getCommonTopics(message.getTags(), currentTime) > 0 && message.getSource() != id)
                        : (message.hasDestination(id));

                if (condition) {
                    messagesForMe.add(message);
                    totalMessages++;
                }
            }
        } else {
            for (Message message : encounteredNode.dataMemory) {
                if (totalMessages >= maxMessages) {
                    break;
                }

                boolean condition = dissemination
                        ? (context.getCommonTopics(message.getTags(), currentTime) > 0 && message.getSource() != id)
                        : (message.hasDestination(id));

                if (condition) {
                    if (isSelfish(encounteredNode, message, currentTime, dissemination)) {
                        continue;
                    }

                    messagesForMe.add(message);

                    exchangeHistoryReceived.add(new ExchangeHistory(currentTime, message, encounteredNode.id, id, encounteredNode.battery.getCurrentLevel()));
                    encounteredNode.exchangeHistorySent.add(new ExchangeHistory(currentTime, message, id, encounteredNode.id, battery.getCurrentLevel()));

                    totalMessages++;
                }
            }
        }

        /*
         * deliver messages intended for the current node.
         */
        for (Message message : messagesForMe) {
            if (!message.isDelivered(id)) {
                if (!dissemination) {
                    encounteredNode.dataMemory.remove(message);
                    message.deleteCopies(encounteredNode.id);
                }

                encounteredNode.messagesDelivered++;
                encounteredNode.messagesExchanged++;
                message.markAsDelivered(id, currentTime);
            } else if (!dissemination) {
                encounteredNode.dataMemory.remove(message);
                message.deleteCopies(encounteredNode.id);
            }
        }

        messagesForMe.clear();

        // return if the total number of messages has been reached
        if (totalMessages >= maxMessages) {
            return 0;
        }

        /*
         * for each of the messages generated by the encountered node, download
         * the ones that are intended for the current node (here, the node won't
         * be selfish with the nodes it generated itself, so no need to account
         * for selfishness).
         */
        for (Message message : encounteredNode.ownMessages) {
            if (totalMessages >= maxMessages) {
                break;
            }

            boolean condition = dissemination
                    ? (context.getCommonTopics(message.getTags(), currentTime) > 0 && message.getSource() != id)
                    : (message.hasDestination(id));

            if (condition) {
                messagesForMe.add(message);
                totalMessages++;

                if (altruism) {
                    exchangeHistoryReceived.add(new ExchangeHistory(currentTime, message, encounteredNode.id, id, encounteredNode.battery.getCurrentLevel()));
                    encounteredNode.exchangeHistorySent.add(new ExchangeHistory(currentTime, message, id, encounteredNode.id, battery.getCurrentLevel()));
                }
            }
        }

        /*
         * deliver messages intended for the current mode.
         */
        for (Message message : messagesForMe) {
            if (!message.isDelivered(id)) {
                if (!dissemination) {
                    encounteredNode.dataMemory.remove(message);
                    message.deleteCopies(encounteredNode.id);
                }

                encounteredNode.messagesDelivered++;
                encounteredNode.messagesExchanged++;
                message.markAsDelivered(id, currentTime);
            } else if (!dissemination) {
                encounteredNode.dataMemory.remove(message);
                message.deleteCopies(encounteredNode.id);
            }
        }

        if (maxMessages == Integer.MAX_VALUE) {
            return maxMessages;
        }

        return Math.max(maxMessages - totalMessages, 0);
    }

    /**
     * Checks if a node is selfish towards a given message.
     *
     * @param node node to be checked for selfishness
     * @param message message to be checked for selfishness towards
     * @param currentTime the current time of the trace
     * @param dissemination {@code true} if dissemination is used, {@code false}
     * if routing is used
     * @return
     */
    protected static boolean isSelfish(Node node, Message message, long currentTime, boolean dissemination) {
        double probability = deliveryRandom.nextDouble();
        double altruismValue;

        boolean condition = dissemination
                ? node.context.getCommonTopics(message.getTags(), currentTime) > 0
                : node.socialNetwork[message.getSource()];

        // compute the altruism value
        if (condition) {
            altruismValue = node.altruism.getLocal();
        } else {
            altruismValue = node.altruism.getGlobal();
        }

        if (probability > altruismValue) {
            return true;
        }

        return false;
    }

    /**
     * Updates the centrality values.
     *
     * @param timeDelta duration between the beginning of the trace and the
     * current moment
     */
    public void updateCentrality(long timeDelta) {
        // if a new window has started, recompute the centrality.
        if (timeDelta / Centrality.getTimeWindow() > Centrality.getLastThreshold()) {
            uniqueNodes.clear();
            uniqueLocalNodes.clear();

            if (timeDelta >= 2 * Centrality.getTimeWindow()) {
                double value = 0.5 * centrality.getValue(Centrality.CentralityValue.CURRENT)
                        + 0.5 * centrality.getValue(Centrality.CentralityValue.PREVIOUS);
                centrality.setValue(Centrality.CentralityValue.PREVIOUS, value);

                value = 0.5 * localCentrality.getValue(Centrality.CentralityValue.CURRENT)
                        + 0.5 * localCentrality.getValue(Centrality.CentralityValue.PREVIOUS);
                localCentrality.setValue(Centrality.CentralityValue.PREVIOUS, value);
            } else {
                centrality.setValue(Centrality.CentralityValue.PREVIOUS, centrality.getValue(Centrality.CentralityValue.CURRENT));
                localCentrality.setValue(Centrality.CentralityValue.PREVIOUS, localCentrality.getValue(Centrality.CentralityValue.CURRENT));
            }

            Centrality.increaseLastThreshold();

            centrality.setValue(Centrality.CentralityValue.CURRENT, 0);
            localCentrality.setValue(Centrality.CentralityValue.CURRENT, 0);
        }

        if (timeDelta < Centrality.getTimeWindow()) {
            centrality.setValue(Centrality.CentralityValue.CUMULATED, centrality.getValue(Centrality.CentralityValue.CURRENT));
            localCentrality.setValue(Centrality.CentralityValue.CUMULATED, localCentrality.getValue(Centrality.CentralityValue.CURRENT));
        } else {
            centrality.setValue(Centrality.CentralityValue.CUMULATED, centrality.getValue(Centrality.CentralityValue.PREVIOUS));
            localCentrality.setValue(Centrality.CentralityValue.CUMULATED, localCentrality.getValue(Centrality.CentralityValue.PREVIOUS));
        }
    }

    /**
     * Gets the S-window centrality for the current node.
     *
     * @param local the type of computation (inside or outside the local
     * community)
     * @return the centrality value for the current node
     */
    public double getCentrality(boolean local) {
        return local
                ? localCentrality.getValue(Centrality.CentralityValue.CUMULATED)
                : centrality.getValue(Centrality.CentralityValue.CUMULATED);
    }

    /**
     * Computes the centrality for the current node upon a contact.
     *
     * @param encounteredNode node the current node is in contact with
     * @param timeDelta duration between the beginning of the trace and the
     * current moment
     */
    protected void computeCentrality(Node encounteredNode, long timeDelta) {
        if (!uniqueNodes.contains(encounteredNode.id)) {
            uniqueNodes.add(encounteredNode.id);

            centrality.increaseValue(Centrality.CentralityValue.CURRENT);
            if (timeDelta < Centrality.getTimeWindow()) {
                centrality.setValue(Centrality.CentralityValue.CUMULATED, centrality.getValue(Centrality.CentralityValue.CURRENT));
            }
        }

        if (inLocalCommunity(encounteredNode.id) && !uniqueLocalNodes.contains(encounteredNode.id)) {
            uniqueLocalNodes.add(encounteredNode.id);

            localCentrality.increaseValue(Centrality.CentralityValue.CURRENT);
            if (timeDelta < Centrality.getTimeWindow()) {
                localCentrality.setValue(Centrality.CentralityValue.CUMULATED, localCentrality.getValue(Centrality.CentralityValue.CURRENT));
            }
        }
    }

    /**
     * Computes common neighbors between this node and another node.
     *
     * @param node node to compare common friends with
     * @return number of common social network friends
     */
    public int getCommonNeighbors(Node node) {
        int count = 0;

        for (int i = 0; i < socialNetwork.length; i++) {
            if (socialNetwork[i] && node.socialNetwork[i]) {
                count++;
            }
        }

        return count;
    }
}
