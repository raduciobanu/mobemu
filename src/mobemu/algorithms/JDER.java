/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package mobemu.algorithms;

import java.util.*;
import mobemu.node.ContactInfo;
import mobemu.node.Context;
import mobemu.node.Message;
import mobemu.node.Node;
import mobemu.trace.Contact;
import mobemu.trace.Trace;

/**
 * Class for a JDER node.
 *
 * Radu Ioan Ciobanu, Daniel Gutierrez Reina, Ciprian Dobre, Sergio L. Toral,
 * and Princy Johnson. JDER: A history-based forwarding scheme for delay
 * tolerant networks using Jaccard distance and encountered ration. Journal of
 * Network and Computer Applications, 40:279-291, April 2014.
 *
 * @author Radu
 */
public class JDER extends Node {

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
     * Algorithm uses the historical form of JDER.
     */
    private boolean historicalJDER;
    /**
     * Algorithm uses the historical form of JDER with limited cache
     */
    private boolean historicalJDERLimitedCache;
    /**
     * Algorithm uses encountered ratio.
     */
    private boolean usesEncounteredRatio;
    /**
     * List of all the contacts from the trace.
     */
    private static Trace contacts = null;
    /**
     * Random number generator.
     */
    private Random random;

    /**
     * Instantiates a {@code JDER} object.
     *
     * @param id ID of the node
     * @param nodes total number of existing nodes
     * @param context the context of this node
     * @param socialNetwork the social network as seen by this node
     * @param dataMemorySize the maximum allowed size of the data memory
     * @param exchangeHistorySize the maximum allowed size of the exchange
     * history
     * @param seed the seed for the random number generators if routing is used
     * @param traceStart timestamp of the start of the trace
     * @param traceEnd timestamp of the end of the trace
     * @param cacheMemorySize size of the cache holding the most recent
     * encounters exchange
     * @param historicalJDER use the historical form of JDER
     * @param historicalJDERLimitedCache use the historical form of JDER with
     * limited cache
     * @param usesEncounteredRatio uses the encountered ratio
     * @param contacts list containing all the contacts from the trace
     */
    public JDER(int id, int nodes, Context context, boolean[] socialNetwork, int dataMemorySize, int exchangeHistorySize,
            long seed, long traceStart, long traceEnd, int cacheMemorySize, boolean historicalJDER,
            boolean historicalJDERLimitedCache, boolean usesEncounteredRatio, Trace contacts) {
        super(id, nodes, context, socialNetwork, dataMemorySize, exchangeHistorySize, seed, traceStart, traceEnd);

        if (JDER.cacheMemorySize == null) {
            JDER.cacheMemorySize = cacheMemorySize;
        }

        this.cacheMemory = new ArrayList<>(Collections.nCopies(cacheMemorySize, new EncounterInfo(-1)));
        this.cacheMemoryPosition = 0;
        this.historicalJDER = historicalJDER;
        this.historicalJDERLimitedCache = historicalJDERLimitedCache;
        this.usesEncounteredRatio = usesEncounteredRatio;

        if (JDER.contacts == null) {
            JDER.contacts = contacts;
        }

        this.random = new Random(seed);
    }

    @Override
    public String getName() {
        return "JDER";
    }

    @Override
    protected void onDataExchange(Node encounteredNode, long contactDuration, long currentTime) {
        if (!(encounteredNode instanceof JDER)) {
            return;
        }

        JDER jderEncounteredNode = (JDER) encounteredNode;
        int remainingMessages = deliverDirectMessages(jderEncounteredNode, false, contactDuration, currentTime, false);
        int totalMessages = 0;

        cacheMemory.set(cacheMemoryPosition, new EncounterInfo(jderEncounteredNode.id));
        cacheMemoryPosition = (cacheMemoryPosition + 1) % cacheMemorySize;

        double jaccardDistance;

        if (!historicalJDER) {
            // get all contacts that are happening at the current moment, in order to know the current neighbors
            List<Contact> currentContacts = new ArrayList<>();

            for (int i = 0; i < contacts.getContactsCount(); i++) {
                if (contacts.getContactAt(i).getStart() <= currentTime && contacts.getContactAt(i).getEnd() >= currentTime) {
                    currentContacts.add(contacts.getContactAt(i));
                }
            }

            // get the current node's neighbors at this moment
            List<Integer> nodeNeighbors = new ArrayList<>();
            for (Contact contact : currentContacts) {
                if (contact.getObserver() == id) {
                    if (!nodeNeighbors.contains(contact.getObserved())) {
                        nodeNeighbors.add(contact.getObserved());
                    }
                }
            }

            // get the encountered node's neighbors at this moment
            List<Integer> encounteredNodeNeighbors = new ArrayList<>();
            for (Contact contact : currentContacts) {
                if (contact.getObserver() == jderEncounteredNode.id) {
                    if (!encounteredNodeNeighbors.contains(contact.getObserved())) {
                        encounteredNodeNeighbors.add(contact.getObserved());
                    }
                }
            }

            // compute the Jaccard distance between the current and the encountered node
            int commonNodes = 0;
            for (Integer nodeID : nodeNeighbors) {
                for (Integer encounteredNodeID : encounteredNodeNeighbors) {
                    if (nodeID.intValue() == encounteredNodeID.intValue()) {
                        commonNodes++;
                    }
                }
            }

            jaccardDistance = 1.0 - (double) commonNodes / (double) (nodeNeighbors.size() + encounteredNodeNeighbors.size() - commonNodes);
            currentContacts.clear();
        } else if (historicalJDER && !historicalJDERLimitedCache) {
            // compute the Jaccard distance between the current and the encountered node
            int commonNodes = 0;
            Iterator it = encounteredNodes.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry<Integer, ContactInfo> pairs = (Map.Entry) it.next();
                if (jderEncounteredNode.encounteredNodes.get(pairs.getKey()) != null) {
                    commonNodes++;
                }
            }

            jaccardDistance = 1.0 - (double) commonNodes / (double) (encounteredNodes.size() + jderEncounteredNode.encounteredNodes.size() - commonNodes);
        } else {
            // compute the Jaccard distance between the current and the encountered node
            int commonNodes = 0;
            for (EncounterInfo encNode : cacheMemory) {
                for (EncounterInfo encEncNode : jderEncounteredNode.cacheMemory) {
                    if (encNode.node != -1 && encNode.node == encEncNode.node) {
                        commonNodes++;
                    }
                }
            }

            int nodeSize = 0;
            int encounteredNodeSize = 0;
            for (EncounterInfo encNode : cacheMemory) {
                if (encNode.node != -1) {
                    nodeSize++;
                }
            }
            for (EncounterInfo encNode : jderEncounteredNode.cacheMemory) {
                if (encNode.node != -1) {
                    encounteredNodeSize++;
                }
            }

            jaccardDistance = 1.0 - (double) commonNodes / (double) (nodeSize + encounteredNodeSize - commonNodes);
        }

        if (usesEncounteredRatio) {
            // compute encounter ratio
            double encounteredRatio = 0.0;
            double factor = 0.0;
            for (int i = 0; i < cacheMemory.size(); i++) {
                if (cacheMemory.get(i).node == jderEncounteredNode.id) {
                    encounteredRatio++;
                }
                if (cacheMemory.get(i).node != -1) {
                    factor++;
                }
            }
            encounteredRatio /= factor;

            // use encounter ratio and Jaccard distance to decide which messages
            // from the encountered node's data memory should be downloaded
            for (Message message : jderEncounteredNode.dataMemory) {
                if (totalMessages >= remainingMessages) {
                    break;
                }

                double value = random.nextDouble();
                if (value <= jaccardDistance) {
                    if (random.nextDouble() > encounteredRatio) {
                        if (encounteredNodes.get(message.getDestination()) != null) {
                            insertMessage(message, jderEncounteredNode, currentTime, false, false);
                            totalMessages++;

                            break;
                        }

                        if (random.nextDouble() <= 0.33) {
                            insertMessage(message, jderEncounteredNode, currentTime, false, false);
                            totalMessages++;
                        }
                    }
                }
            }

            // use encounter ratio and Jaccard distance to decide which messages
            // generated by the encountered node should be downloaded
            for (Message message : jderEncounteredNode.ownMessages) {
                if (totalMessages >= remainingMessages) {
                    break;
                }

                double value = random.nextDouble();
                if (value <= jaccardDistance) {
                    if (random.nextDouble() > encounteredRatio) {
                        if (encounteredNodes.get(message.getDestination()) != null) {
                            insertMessage(message, jderEncounteredNode, currentTime, false, false);
                            totalMessages++;

                            break;
                        }

                        if (random.nextDouble() <= 0.33) {
                            insertMessage(message, jderEncounteredNode, currentTime, false, false);
                            totalMessages++;
                        }
                    }
                }
            }
        } else {
            // use Jaccard distance to decide which messages from the
            // encountered node's data memory should be downloaded
            for (Message message : jderEncounteredNode.dataMemory) {
                if (totalMessages >= remainingMessages) {
                    break;
                }

                double value = random.nextDouble();
                if (value <= jaccardDistance) {
                    insertMessage(message, jderEncounteredNode, currentTime, false, false);
                    totalMessages++;
                }
            }

            // use Jaccard distance to decide which messages generated
            // by the encountered node should be downloaded
            for (Message message : jderEncounteredNode.ownMessages) {
                if (totalMessages >= remainingMessages) {
                    break;
                }

                double value = random.nextDouble();
                if (value <= jaccardDistance) {
                    insertMessage(message, jderEncounteredNode, currentTime, false, false);
                    totalMessages++;
                }
            }
        }
    }

    /**
     * Class used for storing information about node encounters in a JDER-based
     * opportunistic network.
     */
    private static class EncounterInfo {

        private int node;

        /**
         * Creates an {@code EncounterInfo} object.
         *
         * @param node encountered node
         */
        public EncounterInfo(int node) {
            this.node = node;
        }

        /**
         * Sets the encountered node.
         *
         * @param node encountered node
         */
        public void setNode(int node) {
            this.node = node;
        }
    }
}
