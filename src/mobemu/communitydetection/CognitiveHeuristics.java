/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package mobemu.communitydetection;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import mobemu.node.CommunityDetection;
import mobemu.node.ContactInfo;
import mobemu.node.Node;

/**
 * Class for the Cognitive Heuristics community detection algorithm.
 *
 * Matteo Mordacchini, Andrea Passarella, and Marco Conti. Community detection
 * in opportunistic networks using memory-based cognitive heuristics. Pervasive
 * Computing and Communications Workshops (PERCOM Workshops), 2014 IEEE
 * International Conference on, pp. 243-248. IEEE, 2014.
 *
 * @author Radu
 */
public class CognitiveHeuristics implements CommunityDetection {

    private final List<Integer> localCommunity; // the local community of the current node
    private double localCommunityActivation = 0; // the activation of the local community
    private final Map<Integer, List<Long>> contactHistory;
    private double decay;
    private double thetaForget;
    private double thetaSplit;
    private double alpha;

    private static final double DEFAULT_DECAY = 0.5;
    private static final double DEFAULT_THETA_FORGET = 0;
    private static final double DEFAULT_THETA_SPLIT = 1.0;
    private static final double DEFAULT_ALPHA = 0.9;

    /**
     * Instantiates a {@code CognitiveHeuristics} object.
     *
     * @param id id of the node this object belongs to
     * @param nodes number of total nodes in the simulation
     */
    public CognitiveHeuristics(int id, int nodes) {
        this(id, nodes, DEFAULT_DECAY, DEFAULT_THETA_FORGET, DEFAULT_THETA_SPLIT, DEFAULT_ALPHA);
    }

    /**
     * Instantiates a {@code CognitiveHeuristics} object.
     *
     * @param id id of the node this object belongs to
     * @param nodes number of total nodes in the simulation
     * @param decay decay parameter as per the proposed algorithm
     * @param thetaForget theta forget parameter as per the proposed algorithm
     * @param thetaSplit theta split parameter as per the proposed algorithm
     * @param alpha alpha parameter as per the proposed algorithm
     */
    public CognitiveHeuristics(int id, int nodes, double decay, double thetaForget, double thetaSplit, double alpha) {
        this.localCommunity = new ArrayList<>(nodes);
        this.contactHistory = new HashMap<>();
        this.decay = decay;
        this.thetaForget = thetaForget;
        this.thetaSplit = thetaSplit;
        this.alpha = alpha;
    }

    @Override
    public List<Integer> getLocalCommunity() {
        return localCommunity;
    }

    @Override
    public boolean inLocalCommunity(int id) {
        return localCommunity.contains(id);
    }

    @Override
    public void onUpdate(Node encounteredNode, Map<Integer, ContactInfo> encounteredNodes) {
    }

    @Override
    public void onContact(Node encounteredNode, long tick, long sampleTime) {
        int id = encounteredNode.getId();

        // remove old contacts
        updateContacts(id, tick);

        // update local community using the community detection algorithm
        updateLocalCommunity(id, tick, sampleTime);

        // add new contact to list
        recordContact(id, tick);
    }

    /**
     * Updates local community using the community detection algorithm.
     *
     * @param id ID of the encountered node
     * @param tick current trace timestamp
     * @param sampleTime trace sample time
     */
    private void updateLocalCommunity(int id, long tick, long sampleTime) {
        // compute activation
        double activation = computeActivation(id, tick, sampleTime);
        if (activation == 0) {
            return;
        }

        // if the community is empty, add the node directly (and its current activation)
        if (localCommunity.isEmpty()) {
            localCommunity.add(id);
            localCommunityActivation = activation;
            return;
        }

        // if the list has multiple nodes, compare the current activation to the community activation
        if (Math.abs(activation - localCommunityActivation) > thetaSplit) {
            // if the distance between the new activation and the activation stored is
            // higher than theta split and the new activation is greater than the community
            // activation, reset the local community and only add the current node
            if (activation > localCommunityActivation) {
                localCommunityActivation = activation;
                localCommunity.clear();
                localCommunity.add(id);
            }
        } else {
            // if not, apply a smoothing average, also adding the node
            localCommunityActivation = alpha * localCommunityActivation + (1 - alpha) * activation;
            if (!localCommunity.contains(id)) {
                localCommunity.add(id);
            }
        }
    }

    /**
     * Removes contacts that need to be forgotten.
     *
     * @param id ID of the encountered node
     * @param tick current trace timestamp
     */
    private void updateContacts(int id, long tick) {
        List<Long> history = contactHistory.get(id);
        if (history == null) {
            return;
        }

        for (Iterator<Long> iterator = history.iterator(); iterator.hasNext();) {
            Long currentTime = iterator.next();
            double sum = 0.0;
            for (Long time : history) {
                sum += Math.pow(tick - time, -decay);
            }

            if (Math.pow(tick - currentTime, -decay) / sum < thetaForget) {
                iterator.remove();
            } else {
                return;
            }
        }
    }

    /**
     * Computes the activation with a node.
     *
     * @param id ID of the encountered node
     * @param tick current trace timestamp
     * @param sampleTime trace sample time
     * @return the activation value for the encountered node
     */
    private double computeActivation(int id, long tick, long sampleTime) {
        List<Long> history = contactHistory.get(id);
        if (history == null) {
            return 0;
        }

        double sum = 0;
        for (Long time : history) {
            sum += Math.pow((tick - time) / sampleTime, -decay);
        }

        return Math.log(sum);
    }

    /**
     * Records a contact with a node.
     *
     * @param id ID of the encountered node
     * @param tick current trace timestamp
     */
    private void recordContact(int id, long tick) {
        List<Long> history = contactHistory.get(id);
        if (history == null) {
            history = new ArrayList<>();
            history.add(tick);
            contactHistory.put(id, history);
        } else {
            history.add(tick);
        }
    }
}
