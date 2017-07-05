/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package mobemu.node;

import java.util.List;
import java.util.Map;

/**
 * Interface for a community detection algorithm.
 *
 * @author Radu
 */
public interface CommunityDetection {

    /**
     * Gets the node's local community.
     *
     * @return the node's local community
     */
    public List<Integer> getLocalCommunity();

    /**
     * Checks if the encountered node is in the local community of the current
     * node.
     *
     * @param id ID of the encountered node
     * @return {@code true} if the encountered node is in the local community,
     * {@code false} otherwise
     */
    public boolean inLocalCommunity(int id);

    /**
     * Function that performs community detection work, called at every trace
     * update.
     *
     * @param encounteredNode encountered node
     * @param encounteredNodes contact information about encountered nodes
     */
    public void onUpdate(Node encounteredNode, Map<Integer, ContactInfo> encounteredNodes);

    /**
     * Function that performs community detection work, called at every new
     * contact.
     *
     * @param encounteredNode encountered node
     * @param tick trace time
     * @param sampleTime trace sample time
     */
    public void onContact(Node encounteredNode, long tick, long sampleTime);
}
