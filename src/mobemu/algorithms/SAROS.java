/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package mobemu.algorithms;

import java.util.*;
import mobemu.node.Context;
import mobemu.node.Message;
import mobemu.node.Node;
import mobemu.utils.Trust.TransactionData;
import mobemu.utils.Trust.TrustData;
import mobemu.utils.Trust.TrustMessage;
import mobemu.utils.Trust.TrustMessageInfo;

/**
 * Class for a SAROS node.
 *
 * Radu-Ioan Ciobanu, Radu-Corneliu Marin, Ciprian Dobre, Valentin Cristea.
 * Trust and Reputation Management for Opportunistic Dissemination. Pervasive
 * and Mobile Computing, 2016.
 *
 * @author Radu
 */
public class SAROS extends Node {

    /**
     * Map of messages not yet accepted because of insufficient quorum evidence.
     */
    private final Map<Integer, List<TrustMessageInfo>> pendingMessages;
    /**
     * Random value for trust computations
     */
    private static Random trustRandom = null;
    /**
     * Number of message copies required for the message to be analyzed by the
     * quorum algorithm.
     */
    private final int quorumCopies;
    /**
     * Default value for the number of message copies required for the message
     * to be analyzed by the quorum algorithm.
     */
    private static final int DEFAULT_QUORUM_COPIES = 3;
    /**
     * Percentage of message copies with the same value for the quorum
     * algorithm.
     */
    private final double quorumPercentage;
    /**
     * Default value for the percentage of message copies with the same value
     * for the quorum algorithm.
     */
    private static final double DEFAULT_QUORUM_PERCENTAGE = 0.5;
    /**
     * Number of common maximum friends between nodes.
     */
    private static final int MAX_COMMON_FRIENDS = 1;
    /**
     * SAROS values received from other nodes (receivedTrustValues[i][j]
     * represents node's i opinion of node j, as known by the current node).
     */
    private final TrustData[][] receivedTrustValues;
    /**
     * Transaction data for each node.
     */
    private final TransactionData[] transactionData;
    /**
     * Information about all the other nodes in the trace.
     */
    private static Node[] nodes = null;
    /**
     * {@code true} if the raw algorithm should be run (i.e. without trust
     * handling), {@code false} otherwise.
     */
    private static Boolean raw = null;

    /**
     * Constructor for the {@link SAROS} class.
     *
     * @param id ID of the node
     * @param context the context of this node
     * @param socialNetwork the social network as seen by this node
     * @param dataMemorySize the maximum allowed size of the data memory
     * @param exchangeHistorySize the maximum allowed size of the exchange
     * history
     * @param seed the seed for the random number generators
     * @param traceStart timestamp of the start of the trace
     * @param traceEnd timestamp of the end of the trace
     * @param altruism altruism value of the current node (1 for 100%
     * altruistic, 0 for 100% selfish)
     * @param raw {@code true} if the raw algorithm should be run (i.e. without
     * trust handling), {@code false} otherwise
     * @param quorumCopies number of message copies required for the message to
     * be analyzed by the quorum algorithm
     * @param quorumPercentage percentage of message copies with the same value
     * for the quorum algorithm
     * @param nodes array of all the nodes in the network
     */
    public SAROS(int id, Context context, boolean[] socialNetwork,
            int dataMemorySize, int exchangeHistorySize, long seed,
            long traceStart, long traceEnd, double altruism, boolean raw,
            int quorumCopies, double quorumPercentage, Node[] nodes) {
        super(id, nodes.length, context, socialNetwork, dataMemorySize,
                exchangeHistorySize, seed, traceStart, traceEnd);

        this.transactionData = new TransactionData[nodes.length];

        for (int i = 0; i < nodes.length; i++) {
            this.transactionData[i] = new TransactionData();
        }

        // set the node's altruism
        this.altruism.setLocal(altruism > 1.0 ? 1.0 : altruism < 0.0 ? 0.0 : altruism);

        this.pendingMessages = new HashMap<>();

        this.quorumCopies = quorumCopies > 0 ? quorumCopies : DEFAULT_QUORUM_COPIES;
        this.quorumPercentage = quorumPercentage > 0 ? quorumPercentage : DEFAULT_QUORUM_PERCENTAGE;

        if (SAROS.trustRandom == null) {
            SAROS.trustRandom = new Random(seed);
        }

        this.receivedTrustValues = new TrustData[nodes.length][nodes.length];

        if (SAROS.nodes == null) {
            SAROS.nodes = nodes;
        }

        if (SAROS.raw == null) {
            SAROS.raw = raw;
        }
    }

    @Override
    public String getName() {
        return SAROS.raw ? "SAROS raw" : "SAROS";
    }

    @Override
    protected void onDataExchange(Node encounteredNode, long contactDuration, long currentTime) {
        if (!SAROS.raw) {
            exchangeTrustData(encounteredNode, currentTime);
        }

        epidemicDataExchange(encounteredNode, contactDuration, currentTime);
    }

    @Override
    public Message generateMessage(Message message) {
        // use specialized TrustMessage objects instead of regular Message objects
        TrustMessage newMessage = new TrustMessage(message);
        ownMessages.add(newMessage);
        return newMessage;
    }

    @Override
    public boolean insertMessage(Message message, Node from, long currentTime, boolean altruism, boolean dissemination) {
        Message firstMessage = dataMemory.size() > 0 ? dataMemory.get(0) : null;
        boolean inserted = super.insertMessage(message, from, currentTime, altruism, dissemination);

        if (inserted && firstMessage != null && dataMemory.get(0) != firstMessage
                && firstMessage instanceof TrustMessage) {
            ((TrustMessage) firstMessage).removePath(this.id);
        }

        TrustMessage trustMessage = (TrustMessage) message;
        boolean modify = false;

        // modify message based on altruism value
        if (trustRandom.nextDouble() >= this.altruism.getLocal()) {
            modify = true;
        }

        if (inserted) {
            trustMessage.addToPath(from.getId(), this.id, modify ? +trustRandom.nextInt(Integer.MAX_VALUE - 1) : -1);
        }

        return inserted;
    }

    @Override
    protected int deliverDirectMessages(Node encounteredNode, boolean altruism, long contactDuration, long currentTime, boolean dissemination) {
        if (!(encounteredNode instanceof SAROS)) {
            return -1;
        }

        SAROS encounteredNodeSaros = (SAROS) encounteredNode;
        List<Message> messagesForMe = new ArrayList<>();
        int maxMessages = network.computeMaxMessages(contactDuration);
        int totalMessages = 0;

        // for each of the messages carried by the encountered node, download
        // the ones that are intended for the current node
        for (Message message : encounteredNodeSaros.dataMemory) {
            if (totalMessages >= maxMessages) {
                break;
            }

            if (context.getCommonTopics(message.getTags(), currentTime) > 0 && message.getSource() != id) {
                messagesForMe.add(message);
                totalMessages++;
            }
        }

        // deliver messages intended for the current node
        for (Message message : messagesForMe) {
            if (!message.isDelivered(id)) {
                if (!(message instanceof TrustMessage)) {
                    continue;
                }

                TrustMessage trustMessage = (TrustMessage) message;
                TrustMessageInfo info = trustMessage.getInfo(encounteredNodeSaros.id);

                if (info == null) {
                    continue;
                }

                // add the message info to the pending messages
                List<TrustMessageInfo> infos = pendingMessages.get(trustMessage.getId());
                if (infos == null) {
                    infos = new ArrayList<>();
                }
                infos.add(info);
                pendingMessages.put(trustMessage.getId(), infos);

                encounteredNodeSaros.messagesDelivered++;
                encounteredNodeSaros.messagesExchanged++;

                // check if the message is accepted; if not yet, store it
                // until another copy is received, and try again
                int modified = messageAccepted(trustMessage.getId(), false);
                if (modified >= 0) {
                    pendingMessages.remove(trustMessage.getId());
                    message.markAsDelivered(id, currentTime);
                    trustMessage.deliverModified(id, modified);
                }
            }
        }

        messagesForMe.clear();

        // return if the total number of messages has been reached
        if (totalMessages >= maxMessages) {
            return 0;
        }

        // for each of the messages generated by the encountered node, download
        // the ones that are intended for the current node
        for (Message message : encounteredNodeSaros.ownMessages) {
            if (totalMessages >= maxMessages) {
                break;
            }

            if (context.getCommonTopics(message.getTags(), currentTime) > 0 && message.getSource() != id) {
                messagesForMe.add(message);
                totalMessages++;
            }
        }

        // deliver messages intended for the current node
        for (Message message : messagesForMe) {
            if (!message.isDelivered(id)) {
                if (!(message instanceof TrustMessage)) {
                    continue;
                }

                TrustMessage trustMessage = (TrustMessage) message;
                TrustMessageInfo info = trustMessage.getInfo(encounteredNodeSaros.id);

                if (info == null) {
                    continue;
                }

                // add the message info to the pending messages
                List<TrustMessageInfo> infos = pendingMessages.get(trustMessage.getId());
                if (infos == null) {
                    infos = new ArrayList<>();
                }
                infos.add(info);
                pendingMessages.put(trustMessage.getId(), infos);

                encounteredNodeSaros.messagesDelivered++;
                encounteredNodeSaros.messagesExchanged++;

                // check if the message is accepted; if not yet, store it
                // until another copy is received, and try again
                int modified = messageAccepted(trustMessage.getId(), true);
                if (modified >= 0) {
                    pendingMessages.remove(trustMessage.getId());
                    message.markAsDelivered(id, currentTime);
                    trustMessage.deliverModified(id, modified);
                }
            }
        }

        if (maxMessages == Integer.MAX_VALUE) {
            return maxMessages;
        }

        return Math.max(maxMessages - totalMessages, 0);
    }

    /**
     * Verifies if a message is accepted by the quorum algorithm.
     *
     * @param id ID of the message to be checked
     * @param source {@code true} if the analyzed message is being received from
     * its actual source, {@code false} otherwise
     * @return -1 if the message wasn't accepted, the modified message otherwise
     */
    private int messageAccepted(int id, boolean source) {
        List<TrustMessageInfo> messageInfos = pendingMessages.get(id);

        if (messageInfos == null) {
            return -1;
        }

        if (SAROS.raw) {
            return messageInfos.get(0).getModified();
        }

        // if a message was pending and its source is encountered, then it is certain
        // that the version it carries is the correct one, so the trust of all the
        // nodes that have relayed the correct version is increased, and the trust
        // of those that have not relayed it correctly is decreased
        if (source) {
            for (TrustMessageInfo info : messageInfos) {
                if (info.getModified() == 0) {
                    for (Integer nodeInPath : info.getPath()) {
                        transactionData[nodeInPath].positive();
                    }
                } else {
                    for (Integer nodeInPath : info.getPath()) {
                        transactionData[nodeInPath].negative();
                    }
                }
            }

            return 0;
        }

        // check if a minimum number of copies has been received
        if (messageInfos.size() < quorumCopies) {
            return -1;
        }

        // count maximum number of identical message content versions
        Map<Integer, Integer> messageVersions = new HashMap<>();
        int maxCountKey = 0, maxCountValue = Integer.MIN_VALUE;
        for (TrustMessageInfo info : messageInfos) {
            Integer count = messageVersions.get(info.getModified());
            if (count == null) {
                count = 0;
            }

            messageVersions.put(info.getModified(), ++count);

            if (count > maxCountValue) {
                maxCountValue = count;
                maxCountKey = info.getModified();
            }
        }

        // check if the number is higher than or equal to the selected percentage of the number of received copies
        if (maxCountValue <= messageInfos.size() * quorumPercentage) {
            return -1;
        }

        // if the message is accepted, increase local trust for all the nodes on the
        // correct path, and decrease for all the nodes on the wrong path
        for (TrustMessageInfo info : messageInfos) {
            if (info.getModified() == maxCountKey) {
                for (Integer nodeInPath : info.getPath()) {
                    transactionData[nodeInPath].positive();

                }
            } else {
                for (Integer nodeInPath : info.getPath()) {
                    transactionData[nodeInPath].negative();
                }
            }
        }

        return maxCountKey;
    }

    /**
     * Get trust data from an encountered node and store it if it is fresher
     * than what the current node already has.
     *
     * @param encounteredNode node to download the trust data from
     */
    private void exchangeTrustData(Node encounteredNode, long timestamp) {
        if (!(encounteredNode instanceof SAROS)) {
            return;
        }

        SAROS encounteredNodeSaros = (SAROS) encounteredNode;

        // get data from the encountered nodes about all the other nodes in the network
        for (int i = 0; i < receivedTrustValues.length; i++) {
            for (int j = 0; j < receivedTrustValues[i].length; j++) {
                TrustData encounteredTrust = encounteredNodeSaros.receivedTrustValues[i][j];

                if (encounteredTrust == null) {
                    continue;
                }

                if (receivedTrustValues[i][j] == null
                        || receivedTrustValues[i][j].timestamp < encounteredTrust.timestamp) {
                    receivedTrustValues[i][j] = encounteredTrust;
                }
            }
        }

        // get data from the encountered node about itself
        for (int i = 0; i < receivedTrustValues[encounteredNodeSaros.id].length; i++) {
            if (receivedTrustValues[encounteredNodeSaros.id][i] == null) {
                receivedTrustValues[encounteredNodeSaros.id][i] = new TrustData(encounteredNodeSaros.computeLocalTrust(i), timestamp);
            } else {
                receivedTrustValues[encounteredNodeSaros.id][i].trust = encounteredNodeSaros.computeLocalTrust(i);
                receivedTrustValues[encounteredNodeSaros.id][i].timestamp = timestamp;
            }
        }
    }

    /**
     * Epidemic algorithm modified for SocialTrust.
     *
     * @param encounteredNode encountered node
     * @param contactDuration duration of the contact
     * @param currentTime current timestamp
     */
    private void epidemicDataExchange(Node encounteredNode, long contactDuration, long currentTime) {
        if (!(encounteredNode instanceof SAROS)) {
            return;
        }

        SAROS encounteredNodeSaros = (SAROS) encounteredNode;
        double trust = computeEncounteredTrust(encounteredNodeSaros.id);
        double encounteredTrust = encounteredNodeSaros.computeEncounteredTrust(id);

        // start of actual dissemination algorithm (this part should be replaced with any desired dissemination algorithm)
        int remainingMessages = deliverDirectMessages(encounteredNodeSaros, false, contactDuration, currentTime, true);
        int totalMessages = 0;

        double randValue = trustRandom.nextDouble();

        // this is the extra trust condition, only for carried messages (i.e. if
        // I don't trust the encountered node, I don't download data from it; if
        // it doesn't trust me, it doesn't give me its messages)
        if (SAROS.raw || (trust > randValue && encounteredTrust > randValue)) {
            for (Message message : encounteredNodeSaros.dataMemory) {
                if (totalMessages >= remainingMessages) {
                    return;
                }

                insertMessage(message, encounteredNodeSaros, currentTime, false, true);
                totalMessages++;
            }
        }

        // if the encountered node doesn't trust me, it doesn't give me its messages
        if (SAROS.raw || encounteredTrust > randValue) {
            for (Message message : encounteredNodeSaros.ownMessages) {
                if (totalMessages >= remainingMessages) {
                    return;
                }

                insertMessage(message, encounteredNodeSaros, currentTime, false, true);
                totalMessages++;
            }
        }
    }

    /**
     * Computes the normalized local trust in a given node.
     *
     * @param id ID of the node to compute the local trust for
     * @return normalized local trust in the given node
     */
    private double computeLocalTrust(int node) {
        double result = transactionData[node].getTrust();

        if (result == -1) {
            if (socialNetwork[node]) {
                return 1.0;
            }

            int commonFriends = 0;
            for (int i = 0; i < socialNetwork.length; i++) {
                if (socialNetwork[i] && nodes[node].inSocialNetwork(i)) {
                    commonFriends++;
                }
            }

            return Math.min((double) commonFriends / MAX_COMMON_FRIENDS, 1.0);
        }

        return result;
    }

    /**
     * Compute the trust in an encountered node.
     *
     * @param encounteredNodeId ID of the encountered node
     * @return the trust value of the current node in the encountered node
     */
    private double computeEncounteredTrust(int encounteredNodeId) {
        double normalizer = 1.0; // for the current node's trust in itself
        double[] localTrust = new double[receivedTrustValues.length];

        double trust = computeLocalTrust(encounteredNodeId);
        for (int i = 0; i < localTrust.length; i++) {
            localTrust[i] = computeLocalTrust(i);

            if (receivedTrustValues[i][encounteredNodeId] != null) {
                normalizer += localTrust[i];
                trust += localTrust[i] * receivedTrustValues[i][encounteredNodeId].trust;
            }
        }

        return trust / normalizer;
    }
}
