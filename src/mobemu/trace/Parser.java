/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package mobemu.trace;

import java.util.Map;
import mobemu.node.Context;

/**
 * Interface to be implemented by a trace parser.
 *
 * @author Radu
 */
public interface Parser {

	/**
	 * Number of milliseconds in a second.
	 */
	static final long MILLIS_PER_SECOND = 1000L;
	/**
	 * Number of milliseconds in a minute.
	 */
	static final long MILLIS_PER_MINUTE = 60 * MILLIS_PER_SECOND;

	/**
	 * Gets the contact data contained in a mobility trace.
	 *
	 * @return a {@link Trace} object containing the parsed trace
	 */
	Trace getTraceData();

	/**
	 * Gets the context data contained in a mobility trace.
	 *
	 * @return a map of {@link Context} objects for each node
	 */
	Map<Integer, Context> getContextData();

	/**
	 * Gets the social network contained in a mobility trace.
	 *
	 * @return a boolean matrix representing the social network
	 */
	boolean[][] getSocialNetwork();

	/**
	 * Gets the number of nodes participating in this trace.
	 *
	 * @return the number of nodes in this trace
	 */
	int getNodesNumber();

	/**
	 * Gets the number of static nodes participating in this trace (if such nodes
	 * exist, they will be the ones with the highest IDs).
	 * 
	 * @return the number of static nodes in this trace
	 */
	int getStaticNodesNumber();
}
