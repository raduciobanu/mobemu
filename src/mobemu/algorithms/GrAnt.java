/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package mobemu.algorithms;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import mobemu.utils.Tuple;
import mobemu.node.BetweennessUtility;
import mobemu.node.Centrality.CentralityValue;
import mobemu.node.ContactInfo;
import mobemu.node.Context;
import mobemu.node.ExchangeHistory;
import mobemu.node.Message;
import mobemu.node.Node;
import mobemu.node.SocialProximity;


/**
 * Class for an GrAnt node.
 *
 */
public class GrAnt extends Node {

	public static final long BATTL = 3600; // default is one hour	//maybe change it according to the corresponding FA's path latency
	protected double[][] phConcentration; // Keeps track of the current node's neighboring links pheromone level concentration for a given destination.
	protected int[] bestForwarders; // Keeps track of the current node's best forwarder for a given destination.
	protected double[] bestForwardersUtilities; // Keeps track of the current node's best forwarder utility value for a given destination.
	protected int[] betweennessUtility; // Keeps track of the current node's betweenness utility value for a given destination.
	protected SocialProximity socialProximiy; // Keeps track of the current node's social proximity value for a given destination.

	class ForwardAnt extends Message {			

		private List<Tuple<Integer, Double>> currentPath; // A list of nodes encountered during the ant's lifetime identified by the node's id and the node's quality during the encounter. 

		public ForwardAnt(Message m) {
			super(m);
			currentPath = new ArrayList<>();
		}

		public List<Tuple<Integer, Double>> getCurrentPath() {
			return currentPath;
		}

		public void setCurrentPath(List<Tuple<Integer, Double>> path) {
			currentPath = path;
		}
	}

	class BackwardAnt extends Message {

		private double foundPathQuality; // A measure of the found path quality
		private List<Tuple<Integer, Double>> foundPath; // A list of tuples representing the nodes encountered along the path found and their utilities, represented by the node centrality.

		public BackwardAnt(Message m, List<Tuple<Integer, Double>> path, Double quality) {
			super(m);
			this.foundPath = path;
			this.foundPathQuality = quality;
		}

		public List<Tuple<Integer, Double>> getFoundPath() {
			return foundPath;
		}

		public Double getFoundPathQuality() {
			return foundPathQuality;
		}
	}

	/**
	 * Instantiates an {@code Epidemic} object.
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
	 * @param traceEnd timestamp of the end of the trace
	 * @param dissemination {@code true} if dissemination is used, {@code false}
	 * if routing is used
	 * @param altruism {@code true} if altruism computations are performed,
	 * {@code false} otherwise
	 */
	public GrAnt(int id, int nodes, Context context, boolean[] socialNetwork, int dataMemorySize, int exchangeHistorySize,
			long seed, long traceStart, long traceEnd, boolean dissemination, boolean altruism) {
		super(id, nodes, context, socialNetwork, dataMemorySize, exchangeHistorySize, seed, traceStart, traceEnd);
		phConcentration = new double[nodes][nodes];
		bestForwarders = new int[nodes];
		bestForwardersUtilities = new double[nodes];
		for (int i = 0; i < nodes; i++) 
			for (int j = 0; j < nodes; j++)
				phConcentration[i][j] = 0;
		for (int i = 0; i < nodes; i++) { 
			bestForwarders[i] = -1;
			bestForwardersUtilities[i] = 0;
		}
	}

	@Override
	public String getName() {
		return "GrAnt";
	}

	@Override
	protected int deliverDirectMessages(Node encounteredNode, boolean altruism, long contactDuration, long currentTime, boolean dissemination) {
		GrAnt grantEncounteredNode = (GrAnt) encounteredNode;
		List<Message> messagesForMe = new ArrayList<>();
		int maxMessages = network.computeMaxMessages(contactDuration);
		int totalMessages = 0;

		for (Message message : grantEncounteredNode.dataMemory) {
			if (totalMessages >= maxMessages) {
				break;
			}

			boolean condition = message instanceof BackwardAnt || (message instanceof ForwardAnt && message.hasDestination(id));

			if (condition) {
				messagesForMe.add(message);
				totalMessages++;
			}
		} 

		for (Message message : messagesForMe) {
			if (message instanceof ForwardAnt) {
				List<Tuple<Integer, Double>> currentPath = ((ForwardAnt) message).getCurrentPath();
				currentPath.add(new Tuple<Integer, Double>(this.id, this.centrality.getValue(CentralityValue.CURRENT)));
				double pathQuality = computePathQuality(currentPath);
				Collections.reverse(currentPath);
				currentPath.remove(0);
				// create BA using FA's stored info
				BackwardAnt ba = new BackwardAnt(message, new ArrayList<Tuple<Integer, Double>>(currentPath), pathQuality);	// create BA
				dataMemory.add(ba);
				grantEncounteredNode.removeMessage(message, true);
			}
			if (message instanceof BackwardAnt) {

				if (currentTime - message.getTimestamp() > BATTL) {
					grantEncounteredNode.dataMemory.remove(message);
					message.deleteCopies(grantEncounteredNode.getId());
					continue;
				}

				List<Tuple<Integer, Double>> foundPath = ((BackwardAnt) message).getFoundPath();
				Double foundPathQuality = ((BackwardAnt) message).getFoundPathQuality();
				// if this is the next expected node on the reversed path
				if (id == foundPath.get(0).x) {
					// update pheromone concentration 
					phConcentration[grantEncounteredNode.getId()][message.getDestination()] *= 0.5;
					phConcentration[grantEncounteredNode.getId()][message.getDestination()] += foundPathQuality;
					// update node betweenness
					betweennessUtility[message.getDestination()] += 1;
					// remove reversed path's head
					foundPath.remove(0);
					// if Backward Ant reached source node
					if (foundPath.isEmpty() && id == message.getSource()) {
						// delete source message
						removeMessage(message, false);
					} else {
						// send Backward Ant to encountered node
						insertMessage(message, grantEncounteredNode, currentTime, false, false);
					}
				}
				// if this isn't the next expected node on the reversed path
				else {
					// update pheromone concentration
					phConcentration[grantEncounteredNode.getId()][message.getDestination()] *= 0.5;
				}
			}  	
		}

		messagesForMe.clear();

		// return if the total number of messages has been reached
		if (totalMessages >= maxMessages) {
			return 0;
		}

		if (maxMessages == Integer.MAX_VALUE) {
			return maxMessages;
		}

		return Math.max(maxMessages - totalMessages, 0);
	}

	private double computePathQuality(List<Tuple<Integer, Double>> path) {	
		double sum = 0;
		for (Tuple<Integer, Double> t : path) {
			sum += t.y;
		}
		return (sum / path.size() + 1 / path.size());
	}

	@Override
	protected void onDataExchange(Node encounteredNode, long contactDuration, long currentTime) {
		if (!(encounteredNode instanceof GrAnt)) {
			return;
		}

		GrAnt grantEncounteredNode = (GrAnt) grantEncounteredNode;
		int remainingMessages = deliverDirectMessages(grantEncounteredNode, false, contactDuration, currentTime, false);
		int totalMessages = 0;

		// update node's social proximity with encounteredNode based on this encounter
		// TODO

		for (Message message : grantEncounteredNode.dataMemory) {
			if (totalMessages >= remainingMessages) {
				return;
			}

			if (message instanceof ForwardAnt) {

				if (grantEncounteredNode.phConcentration[id][message.getDestination()] > 0 &&
						(grantEncounteredNode.getSocialProximity(message.getDestination()) + grantEncounteredNode.getBetweennessUtility(message.getDestination())) < 
						(getSocialProximity(message.getDestination()) + getBetweennessUtility(message.getDestination()))) {


					List<Tuple<Integer, Double>> currentPath = ((ForwardAnt) message).getCurrentPath();
					currentPath.add(new Tuple<Integer, Double>(this.id, this.centrality.getValue(CentralityValue.CURRENT)));
					insertMessage(message, grantEncounteredNode, currentTime, false, false);

				} 		
				else if (grantEncounteredNode.bestForwarders[message.getDestination()] != -1 &&
						grantEncounteredNode.bestForwardersUtilities[message.getDestination()] < 
						grantEncounteredNode.phConcentration[id][message.getDestination()] * 
						(getSocialProximity(message.getDestination()) + getBetweennessUtility(message.getDestination()))) {

					grantEncounteredNode.bestForwarders[message.getDestination()] = id;

					grantEncounteredNode.bestForwardersUtilities[message.getDestination()] = 
							grantEncounteredNode.phConcentration[id][message.getDestination()] * 
							(getSocialProximity(message.getDestination()) + getBetweennessUtility(message.getDestination()));

					List<Tuple<Integer, Double>> currentPath = ((ForwardAnt) message).getCurrentPath();
					currentPath.add(new Tuple<Integer, Double>(this.id, this.centrality.getValue(CentralityValue.CURRENT)));
					insertMessage(message, grantEncounteredNode, currentTime, false, false);    	        	
				}    		
				else if (grantEncounteredNode.phConcentration[message.getDestination()][message.getDestination()] * 
						(grantEncounteredNode.getSocialProximity(message.getDestination()) + grantEncounteredNode.getBetweennessUtility(message.getDestination())) <
						grantEncounteredNode.phConcentration[id][message.getDestination()] * 
						(getSocialProximity(message.getDestination()) + getBetweennessUtility(message.getDestination()))) {

					grantEncounteredNode.bestForwarders[message.getDestination()] = id;

					grantEncounteredNode.bestForwardersUtilities[message.getDestination()] = 
							grantEncounteredNode.phConcentration[id][message.getDestination()] * 
							(getSocialProximity(message.getDestination()) + getBetweennessUtility(message.getDestination()));

					List<Tuple<Integer, Double>> currentPath = ((ForwardAnt) message).getCurrentPath();
					currentPath.add(new Tuple<Integer, Double>(this.id, this.centrality.getValue(CentralityValue.CURRENT)));
					insertMessage(message, grantEncounteredNode, currentTime, false, false);
				}
			}
		}


		// download each message generated by the encountered node that is not in the current node's memory
		for (Message message : grantEncounteredNode.ownMessages) {
			if (totalMessages >= remainingMessages) {
				return;
			}

			if (grantEncounteredNode.phConcentration[id][message.getDestination()] > 0 &&
					grantEncounteredNode.getSocialProximity(message.getDestination()) < getSocialProximity(message.getDestination())) {

				// create new FA and forward it to this node
				ForwardAnt fa = new ForwardAnt(message);
				List<Tuple<Integer, Double>> currentPath = fa.getCurrentPath();
				currentPath.add(new Tuple<Integer, Double>(grantEncounteredNode.getId(), grantEncounteredNode.getCentrality(false)));
				currentPath.add(new Tuple<Integer, Double>(this.id, this.centrality.getValue(CentralityValue.CURRENT)));
				insertMessage(fa, grantEncounteredNode, currentTime, false, false);    
			}

			else if (grantEncounteredNode.bestForwarders[message.getDestination()] != -1 &&
					grantEncounteredNode.bestForwardersUtilities[message.getDestination()] < 
					grantEncounteredNode.phConcentration[id][message.getDestination()] * 
					(getSocialProximity(message.getDestination()) + getBetweennessUtility(message.getDestination()))) {

				grantEncounteredNode.bestForwarders[message.getDestination()] = id; 
				grantEncounteredNode.bestForwardersUtilities[message.getDestination()] = 
						grantEncounteredNode.phConcentration[id][message.getDestination()] * 
						(getSocialProximity(message.getDestination()) + getBetweennessUtility(message.getDestination()));

				// create new FA and forward it to this node
				ForwardAnt fa = new ForwardAnt(message);
				List<Tuple<Integer, Double>> currentPath = fa.getCurrentPath();
				currentPath.add(new Tuple<Integer, Double>(grantEncounteredNode.getId(), grantEncounteredNode.getCentrality(false)));
				insertMessage(fa, grantEncounteredNode, currentTime, false, false);    
			}

			else if (grantEncounteredNode.getSocialProximity(message.getDestination()) < getSocialProximity(message.getDestination())) {

				grantEncounteredNode.bestForwarders[message.getDestination()] = id;
				grantEncounteredNode.bestForwardersUtilities[message.getDestination()] = 
						grantEncounteredNode.phConcentration[id][message.getDestination()] * 
						(getSocialProximity(message.getDestination()) + getBetweennessUtility(message.getDestination()));

				// create new FA and forward it to this node
				ForwardAnt fa = new ForwardAnt(message);
				List<Tuple<Integer, Double>> currentPath = fa.getCurrentPath();
				currentPath.add(new Tuple<Integer, Double>(grantEncounteredNode.getId(), grantEncounteredNode.getCentrality(false)));
				insertMessage(fa, grantEncounteredNode, currentTime, false, false);  
			}
		}
	}
}
