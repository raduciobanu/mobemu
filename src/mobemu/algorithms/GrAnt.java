/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package mobemu.algorithms;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import mobemu.utils.Tuple;
import mobemu.node.Centrality.CentralityValue;
import mobemu.node.Context;
import mobemu.node.Message;
import mobemu.node.Node;


/**
 * Class for an GrAnt node.
 *
 */
public class GrAnt extends Node {

	protected HashMap<Integer, List<Double>> phConcentration; // Keeps track of the current node's neighboring links pheromone level concentration for a given destination.
	protected HashMap<Integer, Tuple<Integer, Double>> bestForwarders; // Keeps track of the current node's best forwarder for a given destination.

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
		public BackwardAnt(Message m) {
			super(m);
			foundPathQuality = 0;
			foundPath = new ArrayList<>();
		}
		public List<Tuple<Integer, Double>> getFoundPath() {
			return foundPath;
		}	
		public void setFoundPath(List<Tuple<Integer, Double>> path) {
			foundPath = path;
		}
		public Double getFoundPathQuality() {
			return foundPathQuality;
		}	
		public void setFoundPathQuality(double pathQuality) {
			foundPathQuality = pathQuality;
		}
	}

	/**
	 * Instantiates an {@code GrAnt} object.
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

		phConcentration = new HashMap<>();
		bestForwarders = new HashMap<>();

		for (int i = 0; i < nodes; i++) {
			List<Double> links = new ArrayList<>(nodes); 
			for (int j = 0; j < nodes; j++) {
				links.add(0.0);
			}
			phConcentration.put(i, links);
			bestForwarders.put(i, null);
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
				ForwardAnt fa = (ForwardAnt) message;
				
				// if ForwardAnt has expired, delete it from encounteredNode's data memory
				if (currentTime - message.getTimestamp() > fa.getTtl()) {
					grantEncounteredNode.dataMemory.remove(fa);
					fa.deleteCopies(grantEncounteredNode.getId());
					continue;
				}	
				// mark ForwardAnt delivered
				fa.markAsDelivered(id, currentTime);
				
				grantEncounteredNode.messagesDelivered++;
				grantEncounteredNode.messagesExchanged++;
				
				// remove ForwardAnt from encounteredNode
				grantEncounteredNode.removeMessage(fa, true);
				
				// compute ForwardAnt's path quality
				List<Tuple<Integer, Double>> currentPath = fa.getCurrentPath();
				currentPath.add(new Tuple<Integer, Double>(id, centrality.getValue(CentralityValue.CURRENT)));
				double pathQuality = computePathQuality(currentPath);	
				Collections.reverse(currentPath);
				currentPath.remove(0);
				// create BackwardAnt for ForwardAnt
				Message	message_copy = (Message) fa.clone();
				message_copy.setTimestamp(currentTime);
				BackwardAnt ba = new BackwardAnt(message_copy);
				ba.setTtl(currentTime - fa.getTimestamp());
				ba.setFoundPath(new ArrayList<>(currentPath));
				ba.setFoundPathQuality(pathQuality);
				// save BackwardAnt in data memory
				dataMemory.add(ba);


			}
			if (message instanceof BackwardAnt) {				
				BackwardAnt ba = (BackwardAnt) message;
				// if BackwardAnt has expired, delete it from encounteredNode's data memory
				if (currentTime - message.getTimestamp() > ba.getTTL()) {
					grantEncounteredNode.dataMemory.remove(ba);
					ba.deleteCopies(grantEncounteredNode.getId());
					continue;
				}		
				// delete any existing ForwardAnts associated with this BackwardAnt, if any
				for (Message m : dataMemory) {
					if (m instanceof ForwardAnt && m.equals(ba)) {
						removeMessage(m, true);
					}
				}
				List<Tuple<Integer, Double>> foundPath = ba.getFoundPath();
				Double foundPathQuality = ba.getFoundPathQuality();		
				if (foundPath.get(0).getKey() == id) {
					// remove reversed path's head
					foundPath.remove(0);
					// download BackwardAnt
					insertMessage(ba, grantEncounteredNode, currentTime, false, false);
					// delete BackwardAnt from sender's data memory
					grantEncounteredNode.removeMessage(ba, true);
					// update pheromone concentration 
					double ph = phConcentration.get(ba.getDestination()).get(grantEncounteredNode.getId()); 
					double ph_new = 0.5 * ph + foundPathQuality;
					phConcentration.get(ba.getDestination()).set(grantEncounteredNode.getId(), ph_new); 
					// update node betweenness
	                computeBetweennessUtility(encounteredNode, currentTime - Node.traceStart);
					// if BackwardAnt reached source node, delete source message
					if (foundPath.isEmpty()) {
						for (int i = 0; i < ownMessages.size(); i++) {
							if (ownMessages.get(i).equals(ba)) {
								removeMessage(ownMessages.get(i), false);
							}
						}
					}
					// delete BackwardAnt from memory
					removeMessage(ba, true);
				}
				else {
					// update pheromone concentration
					double ph = phConcentration.get(ba.getDestination()).get(grantEncounteredNode.getId()); 
					double ph_new = 0.5 * ph;
					phConcentration.get(ba.getDestination()).set(grantEncounteredNode.getId(), ph_new); 
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
		for (int i = 0; i < path.size(); i++) {
			sum += path.get(i).getValue();
		}
		return (sum / path.size() + 1 / path.size());
	}

	@Override
	protected void onDataExchange(Node encounteredNode, long contactDuration, long currentTime) {
		/*
		System.out.println("###########new encounter#############");
		System.out.println("Social Proximity");
		for (int i = 0; i < nodes; i++) {
			System.out.println(i + " : " + encounteredNode.getSocialProximity(i));
		}
		System.out.println("Best Forwarders");
		for (int i = 0; i < nodes; i++) {
			System.out.println(i + " : " + ((GrAnt)encounteredNode).bestForwarders.get(i));
		}
		System.out.println("#####################################");
		*/
		if (!(encounteredNode instanceof GrAnt)) {
			return;
		}

		GrAnt grantEncounteredNode = (GrAnt) encounteredNode;
		int remainingMessages = deliverDirectMessages(grantEncounteredNode, false, contactDuration, currentTime, false);
		int totalMessages = 0;

        ArrayList<Message> toRemove = new ArrayList<>();
		
		for (Message message : grantEncounteredNode.dataMemory) {
			if (totalMessages >= remainingMessages) {
				return;
			}

			if (message instanceof ForwardAnt) {
				
				// if ForwardAnt has expired, delete it from encounteredNode's data memory
				if (currentTime - message.getTimestamp() > message.getTtl()) {
					toRemove.add(message);
					continue;
				}	
				
				if (grantEncounteredNode.phConcentration.get(message.getDestination()).get(id) > 0 &&
						(grantEncounteredNode.getSocialProximity(message.getDestination()) + grantEncounteredNode.getBetweennessUtility(message.getDestination())) < 
						(getSocialProximity(message.getDestination()) + getBetweennessUtility(message.getDestination()))) {
					// exploit existing path
					List<Tuple<Integer, Double>> currentPath = ((ForwardAnt) message).getCurrentPath();
					currentPath.add(new Tuple<Integer, Double>(this.id, this.centrality.getValue(CentralityValue.CURRENT)));
					insertMessage(message, grantEncounteredNode, currentTime, false, false);
					toRemove.add(message);
				} 		
				else if (grantEncounteredNode.bestForwarders.get(message.getDestination()) != null) {
					
					int bestForwarder = grantEncounteredNode.bestForwarders.get(message.getDestination()).getKey();
					double bestForwarderUtility = grantEncounteredNode.bestForwarders.get(message.getDestination()).getValue();
					double currentUtility = grantEncounteredNode.phConcentration.get(message.getDestination()).get(id) * 
							(getSocialProximity(message.getDestination()) + getBetweennessUtility(message.getDestination()));
					
					if (id == bestForwarder) {
						// forward ForwardAnt via current best forwarder
						List<Tuple<Integer, Double>> currentPath = ((ForwardAnt) message).getCurrentPath();
						currentPath.add(new Tuple<Integer, Double>(this.id, this.centrality.getValue(CentralityValue.CURRENT)));
						insertMessage(message, grantEncounteredNode, currentTime, false, false);
						toRemove.add(message);
					} 
					else if ( currentUtility >= bestForwarderUtility) {
						// update best forwarder & send ForwardAnt
						grantEncounteredNode.bestForwarders.put(message.getDestination(), new Tuple<Integer, Double>(id, currentUtility));
						List<Tuple<Integer, Double>> currentPath = ((ForwardAnt) message).getCurrentPath();
						currentPath.add(new Tuple<Integer, Double>(this.id, this.centrality.getValue(CentralityValue.CURRENT)));
						insertMessage(message, grantEncounteredNode, currentTime, false, false);
						toRemove.add(message);
					}
				}
				else {
					double currentUtility = grantEncounteredNode.phConcentration.get(message.getDestination()).get(id) * 
							(getSocialProximity(message.getDestination()) + getBetweennessUtility(message.getDestination()));
					double encounteredNodeUtility = grantEncounteredNode.phConcentration.get(message.getDestination()).get(message.getDestination()) *
							(grantEncounteredNode.getSocialProximity(message.getDestination()) + grantEncounteredNode.getBetweennessUtility(message.getDestination()));
					if (encounteredNodeUtility < currentUtility) {
						// initialize best forwarder & send ForwardAnt
						grantEncounteredNode.bestForwarders.put(message.getDestination(), new Tuple<Integer, Double>(id, currentUtility));
						List<Tuple<Integer, Double>> currentPath = ((ForwardAnt) message).getCurrentPath();
						currentPath.add(new Tuple<Integer, Double>(this.id, this.centrality.getValue(CentralityValue.CURRENT)));
						insertMessage(message, grantEncounteredNode, currentTime, false, false);
						toRemove.add(message);
					}
				}
			}
		}
		
		for (Message message : toRemove) {
			grantEncounteredNode.removeMessage(message, true);
		}
		
		toRemove = new ArrayList<>();


		// download each message generated by the encountered node that is not in the current node's memory
		for (Message message: grantEncounteredNode.ownMessages) {
			if (totalMessages >= remainingMessages) {
				return;
			}
			
			// if message has expired, delete it and proceed with next message
			if(currentTime - message.getTimestamp() > message.getTTL()) {
				toRemove.add(message);
				continue;
			}

			if (grantEncounteredNode.phConcentration.get(message.getDestination()).get(id) > 0 &&
					grantEncounteredNode.getSocialProximity(message.getDestination()) < getSocialProximity(message.getDestination())) {

				// exploit existing path
				ForwardAnt fa = new ForwardAnt(message);
				List<Tuple<Integer, Double>> currentPath = fa.getCurrentPath();
				currentPath.add(new Tuple<Integer, Double>(grantEncounteredNode.getId(), grantEncounteredNode.getCentrality(false)));
				currentPath.add(new Tuple<Integer, Double>(this.id, this.centrality.getValue(CentralityValue.CURRENT)));
				insertMessage(fa, grantEncounteredNode, currentTime, false, false);    
				//toRemove.add(message);
			}
			else if (grantEncounteredNode.bestForwarders.get(message.getDestination()) != null) {
				int bestForwarder = grantEncounteredNode.bestForwarders.get(message.getDestination()).getKey();
				double bestForwarderUtility = grantEncounteredNode.bestForwarders.get(message.getDestination()).getValue();
				double currentUtility = grantEncounteredNode.phConcentration.get(message.getDestination()).get(id) * 
						(getSocialProximity(message.getDestination()) + getBetweennessUtility(message.getDestination()));
				
				if (id == bestForwarder) {
					// forward ForwardAnt via current best forwarder
					ForwardAnt fa = new ForwardAnt(message);
					List<Tuple<Integer, Double>> currentPath = fa.getCurrentPath();
					currentPath.add(new Tuple<Integer, Double>(grantEncounteredNode.getId(), grantEncounteredNode.getCentrality(false)));
					insertMessage(fa, grantEncounteredNode, currentTime, false, false);    
					//toRemove.add(message);
				}
				else if (currentUtility >= bestForwarderUtility) {
					// update best forwarder & send ForwardAnt
					grantEncounteredNode.bestForwarders.put(message.getDestination(), new Tuple<Integer, Double>(id, currentUtility));
					// create new ForwardAnt and forward it to this node
					ForwardAnt fa = new ForwardAnt(message);
					List<Tuple<Integer, Double>> currentPath = fa.getCurrentPath();
					currentPath.add(new Tuple<Integer, Double>(grantEncounteredNode.getId(), grantEncounteredNode.getCentrality(false)));
					insertMessage(fa, grantEncounteredNode, currentTime, false, false);    
					//toRemove.add(message);
				}
			}
			else {
				if (grantEncounteredNode.getSocialProximity(message.getDestination()) < getSocialProximity(message.getDestination())) {
					double currentUtility = grantEncounteredNode.phConcentration.get(message.getDestination()).get(id) * 
							(getSocialProximity(message.getDestination()) + getBetweennessUtility(message.getDestination()));
					// initialize best forwarder
					grantEncounteredNode.bestForwarders.put(message.getDestination(), new Tuple<Integer, Double>(id, currentUtility));
					// create new ForwardAnt and forward it to this node
					ForwardAnt fa = new ForwardAnt(message);
					List<Tuple<Integer, Double>> currentPath = fa.getCurrentPath();
					currentPath.add(new Tuple<Integer, Double>(grantEncounteredNode.getId(), grantEncounteredNode.getCentrality(false)));
					insertMessage(fa, grantEncounteredNode, currentTime, false, false);  
					//toRemove.add(message);
				}
			}
		}
		
		for (Message message : toRemove) {
			grantEncounteredNode.removeMessage(message, false);
		}
	}
}
