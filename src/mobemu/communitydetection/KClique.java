/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package mobemu.communitydetection;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import mobemu.node.CommunityDetection;
import mobemu.node.ContactInfo;
import mobemu.node.Node;

/**
 * Class for the K-clique community detection algorithm.
 *
 * Pan Hui, Eiko Yoneki, Shu Yan Chan, and Jon Crowcroft. Distributed community
 * detection in delay tolerant networks. Proceedings of 2nd ACM/IEEE
 * International Workshop on Mobility in the Evolving Internet Architecture.
 * ACM, 2007.
 *
 * @author Radu
 */
public class KClique implements CommunityDetection {

    private final List<Integer> localCommunity; // the local community of the current node
    private final boolean[] familiarSet; // the familiar set of the current node
    private final boolean[][] globalFamiliarSet; // the global familiar set of the current node
    private int contactThreshold; // contact threshold for the K-clique algorithm
    private int communityThreshold; // community threshold for the K-clique algorithm

    private static final int DEFAULT_CONTACT_THRESHOLD = 20 * 60 * 1000;
    private static final int DEFAULT_COMMUNITY_THRESHOLD = 5;

    /**
     * Instantiates a {@code KClique} object.
     *
     * @param id id of the node this object belongs to
     * @param nodes number of total nodes in the simulation
     */
    public KClique(int id, int nodes) {
        this(id, nodes, DEFAULT_CONTACT_THRESHOLD, DEFAULT_COMMUNITY_THRESHOLD);
    }

    /**
     * Instantiates a {@code KClique} object.
     *
     * @param id id of the node this object belongs to
     * @param nodes number of total nodes in the simulation
     * @param contactThreshold contact threshold for the K-clique algorithm
     * @param communityThreshold community threshold for the K-clique algorithm
     */
    public KClique(int id, int nodes, int contactThreshold, int communityThreshold) {
        this.familiarSet = new boolean[nodes];
        this.localCommunity = new ArrayList<>(nodes);
        this.localCommunity.add(id);
        this.globalFamiliarSet = new boolean[nodes][nodes];
        this.contactThreshold = contactThreshold;
        this.communityThreshold = communityThreshold;
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
        if (!inFamiliarSet(encounteredNode.getId())) {
            // when the threshold has been exceeded, insert vi in F0 and C0
            checkThreshold(encounteredNode.getId(), encounteredNodes);
        }
    }

    @Override
    public void onContact(Node encounteredNode, long tick, long sampleTime) {
        // update the global familiar set of the current node
        updateFamiliarSet(encounteredNode, true);

        // step 4 of the K-clique algorithm
        if (!inFamiliarSet(encounteredNode.getId())) {
            updateFamiliarSet(encounteredNode, false);
        }

        // step 5 of the K-clique algorithm
        if (!inLocalCommunity(encounteredNode.getId())) {
            updateLocalCommunity(encounteredNode);
        }

        // step 6 of the K-clique algorithm
        if (inLocalCommunity(encounteredNode.getId())) {
            updateLocalCommunityAggressive(encounteredNode);
        }
    }

    /**
     * Checks if the encountered node is in the familiar set of the current
     * node.
     *
     * @param id ID of the encountered node
     * @return {@code true} if the encountered node is in the familiar set,
     * {@code false} otherwise
     */
    private boolean inFamiliarSet(int id) {
        return familiarSet[id];
    }

    /**
     * Updates the global familiar set for the encountered node.
     *
     * @param node encountered node
     * @param global {@code true} for updating the global familiar set,
     * {@code false} for updating the local familiar set
     */
    private void updateFamiliarSet(Node node, boolean global) {
        if (!(node.getCommunityInfo() instanceof KClique)) {
            return;
        }

        KClique nodeCommunity = (KClique) node.getCommunityInfo();

        if (global) {
            for (int i = 0; i < globalFamiliarSet.length; i++) {
                for (int j = 0; j < globalFamiliarSet[i].length; j++) {
                    globalFamiliarSet[i][j] |= nodeCommunity.globalFamiliarSet[i][j];
                }
            }
        } else {
            for (int j = 0; j < globalFamiliarSet[node.getId()].length; j++) {
                globalFamiliarSet[node.getId()][j] |= nodeCommunity.familiarSet[j];
            }
        }
    }

    /**
     * Checks if the K-clique contact duration threshold has been exceeded and
     * adds the encountered node to the local community if it has.
     *
     * @param id ID of the encountered node
     * @param encounteredNodes contact information about encountered nodes
     */
    private void checkThreshold(int id, Map<Integer, ContactInfo> encounteredNodes) {
        ContactInfo node = encounteredNodes.get(id);

        if (node != null) {
            if (node.getDuration() > contactThreshold) {
                if (!inFamiliarSet(id)) {
                    familiarSet[id] = true;
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
    private void updateLocalCommunity(Node encounteredNode) {
        if (!(encounteredNode.getCommunityInfo() instanceof KClique)) {
            return;
        }

        KClique nodeCommunity = (KClique) encounteredNode.getCommunityInfo();

        int count = 0;

        for (Integer localNode : localCommunity) {
            if (nodeCommunity.familiarSet[localNode]) {
                count++;
            }
        }

        if (count >= communityThreshold - 1) {
            localCommunity.add(encounteredNode.getId());
        }
    }

    /**
     * Aggressively updates the local community.
     *
     * @param encounteredNode encountered node
     */
    private void updateLocalCommunityAggressive(Node encounteredNode) {
        if (!(encounteredNode.getCommunityInfo() instanceof KClique)) {
            return;
        }

        KClique nodeCommunity = (KClique) encounteredNode.getCommunityInfo();

        for (Integer newID : nodeCommunity.localCommunity) {
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
}
