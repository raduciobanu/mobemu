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
import mobemu.node.Context;
import mobemu.node.Message;
import mobemu.node.Node;


/**
 * Class for an GrAnt node.
 *
 */
public class GrAnt extends Node {
	
	public static final double phEvaporationRate = 0.7;
	
	public static HashMap<Integer, Integer> totalDelivers = new HashMap<>();

	public HashMap<Integer, List<Double>> phConcentration; 			// Keeps track of the current node's neighboring links pheromone level concentration for a given destination.
	public HashMap<Integer, Tuple<Integer, Double>> bestForwarders; // Keeps track of the current node's best forwarder for a given destination.

	class ForwardAnt extends Message {			
		private List<Tuple<Integer, Double>> currentPath; 			// A list of nodes encountered during the ant's lifetime identified by the node's id and the node's quality during the encounter. 
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
		private double foundPathQuality; 							// A measure of the found path quality
		private List<Tuple<Integer, Double>> foundPath; 			// A list of tuples representing the nodes encountered along the path found and their utilities, represented by the node centrality.
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
					grantEncounteredNode.removeMessage(fa, true);
					continue;
				}
				
				// mark ForwardAnt delivered
				if (!fa.isDelivered(id)) {
					fa.markAsDelivered(id, currentTime);
					grantEncounteredNode.messagesDelivered++;
					grantEncounteredNode.messagesExchanged++;
					
					Integer value = totalDelivers.get(id);
					if (value == null) {
						totalDelivers.put(id, 1);
					}
					else {
						totalDelivers.put(id, value+1);
					}
				}
				
				// remove ForwardAnt from encounteredNode
				grantEncounteredNode.removeMessage(fa, true);


				// compute ForwardAnt's path quality
				List<Tuple<Integer, Double>> currentPath = fa.getCurrentPath();
				currentPath.add(new Tuple<Integer, Double>(id, getCentrality(false)));
				double pathQuality = computePathQuality(currentPath);	
				Collections.reverse(currentPath);
				currentPath.remove(0);
				currentPath.remove(0);
				// create BackwardAnt for ForwardAnt
				Message	message_copy = (Message) fa.clone();
				message_copy.setTimestamp(currentTime);
				message_copy.setTtl(7 * 24 * 3600 * 1000);
				BackwardAnt ba = new BackwardAnt(message_copy);
				ba.setFoundPath(new ArrayList<>(currentPath));
				ba.setFoundPathQuality(pathQuality);
				// save BackwardAnt in data memory
				//dataMemory.add(ba);
				grantEncounteredNode.insertMessage(ba, this, currentTime, false, false);
				
				// update pheromone concentration
				double ph = grantEncounteredNode.phConcentration.get(this.id).get(this.getId()); 
				double ph_new = phEvaporationRate * ph + pathQuality;
				if(ph_new > 0.000001)
					grantEncounteredNode.phConcentration.get(this.id).set(this.id, ph_new); 
				else
					grantEncounteredNode.phConcentration.get(this.id).set(this.id, 0.0); 
				
				// update betweenness utility
				grantEncounteredNode.updateBetweennessUtility(this, currentTime - Node.traceStart); //khkkkkkkkkkkkkkkkkkkkkkkk
			}
			
			if (message instanceof BackwardAnt) {				
				BackwardAnt ba = (BackwardAnt) message;
				
				// if BackwardAnt has expired, delete it from encounteredNode's data memory
				if (currentTime - message.getTimestamp() > ba.getTTL()) {
					grantEncounteredNode.removeMessage(ba, true);
					continue;
				}
				
		        ArrayList<Message> toRemove = new ArrayList<>();
					
				// delete any existing ForwardAnts associated with this BackwardAnt, if any
				for (Message m : dataMemory) {
					if (m instanceof ForwardAnt && m.getSource() == ba.getSource() && m.getDestination() == ba.getDestination() && m.getMessage() == ba.getMessage()) {	//suprascrie equals din ba si punte ba in loc de m
						toRemove.add(m);
					}
				}
				
				for (Message m : toRemove) {
					removeMessage(m, true);
				}
				
				toRemove.clear();
				
				List<Tuple<Integer, Double>> foundPath = ba.getFoundPath();
				Double foundPathQuality = ba.getFoundPathQuality();		
				if (foundPath.get(0).getKey() == id) {
					// remove reversed path's head
					foundPath.remove(0);
					// download BackwardAnt
					insertMessage(ba, grantEncounteredNode, currentTime, false, false);
					// update pheromone concentration 
					double ph = phConcentration.get(ba.getDestination()).get(grantEncounteredNode.getId()); 
					double ph_new = phEvaporationRate * ph + foundPathQuality;
					phConcentration.get(ba.getDestination()).set(grantEncounteredNode.getId(), ph_new); 
					// delete BackwardAnt from sender's data memory
					grantEncounteredNode.removeMessage(ba, true);
					// update node betweenness utility
	                increaseBetweennessUtility(grantEncounteredNode, currentTime - Node.traceStart);
					// if BackwardAnt reached source node, delete source message
					if (foundPath.size() == 0) {
						for (Message m : ownMessages) {
							if (m.getSource() == ba.getSource() && m.getDestination() == ba.getDestination() && m.getMessage() == ba.getMessage()) {
								removeMessage(m, false);
								break;
							}
						}
						// delete BackwardAnt from memory
						removeMessage(ba, true);
						removeMessage(ba, false);
					}
				}
				else {
					// update pheromone concentration
					double ph = phConcentration.get(ba.getDestination()).get(grantEncounteredNode.getId()); 
					double ph_new = phEvaporationRate * ph;
					if (ph_new > 0.000001)
						phConcentration.get(ba.getDestination()).set(grantEncounteredNode.getId(), ph_new); 
					else
						phConcentration.get(this.id).set(this.id, 0.0); 
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
		double sum = 0.0;
		for (int i = 0; i < path.size(); i++) {
			sum += path.get(i).getValue();
		}
		return sum / path.size() + 1.0 / path.size();
	}
	
	@Override
    protected void preDataExchange(Node encounteredNode, long currentTime) {

    }

	@Override
	protected void onDataExchange(Node encounteredNode, long contactDuration, long currentTime) {
		/*
		System.out.println("###########new encounter between " + encounteredNode.getId() + " and " + this.id + "#############");
		System.out.println("Ph Concentrations of " + encounteredNode.getId());
		System.out.println(((GrAnt)encounteredNode).phConcentration);
		System.out.println("Social Proximity of " + encounteredNode.getId() + ":");
		for (int i = 0; i < nodes; i++) {
			System.out.println(i + " : " + encounteredNode.getSocialProximity(i));
		}
		System.out.println("Betweenness Utility of " + encounteredNode.getId());
		for (int i = 0; i < nodes; i++) {
			System.out.println(i + " : " + ((GrAnt)encounteredNode).getBetweennessUtility(i));
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
				ForwardAnt fa = (ForwardAnt) message;
				
				// if ForwardAnt has expired, delete it from encounteredNode's data memory
				if (currentTime - fa.getTimestamp() > fa.getTtl()) {
					toRemove.add(fa);
					continue;
				}	
				
				if (grantEncounteredNode.phConcentration.get(fa.getDestination()).get(id) > 0 &&
						(grantEncounteredNode.getSocialProximity(fa.getDestination()) + grantEncounteredNode.getBetweennessUtility(fa.getDestination())) < 
						(getSocialProximity(fa.getDestination()) + getBetweennessUtility(fa.getDestination()))) {
					// exploit existing path
					List<Tuple<Integer, Double>> currentPath = ((ForwardAnt) fa).getCurrentPath();
					currentPath.add(new Tuple<Integer, Double>(this.id, getCentrality(false)));
					insertMessage(fa, grantEncounteredNode, currentTime, false, false);
					toRemove.add(fa);
				} 		
				else if (grantEncounteredNode.bestForwarders.get(fa.getDestination()) != null) {
					
					int bestForwarder = grantEncounteredNode.bestForwarders.get(fa.getDestination()).getKey();
					double bestForwarderUtility = grantEncounteredNode.bestForwarders.get(fa.getDestination()).getValue();
					double nodeUtility = grantEncounteredNode.phConcentration.get(fa.getDestination()).get(id) * 
							(getSocialProximity(fa.getDestination()) + getBetweennessUtility(fa.getDestination()));
					
					
					if (id == bestForwarder) {
						// forward ForwardAnt via current best forwarder
						List<Tuple<Integer, Double>> currentPath = ((ForwardAnt) fa).getCurrentPath();
						currentPath.add(new Tuple<Integer, Double>(this.id, getCentrality(false)));
						insertMessage(fa, grantEncounteredNode, currentTime, false, false);
						toRemove.add(fa);
					}
					
					else if ( nodeUtility >= bestForwarderUtility) {
						// update best forwarder & send ForwardAnt
						grantEncounteredNode.bestForwarders.put(fa.getDestination(), new Tuple<Integer, Double>(id, nodeUtility));
						List<Tuple<Integer, Double>> currentPath = ((ForwardAnt) fa).getCurrentPath();
						currentPath.add(new Tuple<Integer, Double>(this.id, getCentrality(false)));
						insertMessage(fa, grantEncounteredNode, currentTime, false, false);
						toRemove.add(fa);
					}
				}
				else {
					double nodeUtility = grantEncounteredNode.phConcentration.get(fa.getDestination()).get(id) * 
							(getSocialProximity(fa.getDestination()) + getBetweennessUtility(fa.getDestination()));
					double encounteredNodeUtility = grantEncounteredNode.phConcentration.get(fa.getDestination()).get(fa.getDestination()) *
							(grantEncounteredNode.getSocialProximity(fa.getDestination()) + grantEncounteredNode.getBetweennessUtility(fa.getDestination()));
					if (encounteredNodeUtility <= nodeUtility) {
						// initialize best forwarder & send ForwardAnt
						grantEncounteredNode.bestForwarders.put(fa.getDestination(), new Tuple<Integer, Double>(id, nodeUtility));
						List<Tuple<Integer, Double>> currentPath = ((ForwardAnt) fa).getCurrentPath();
						currentPath.add(new Tuple<Integer, Double>(this.id, getCentrality(false)));
						insertMessage(fa, grantEncounteredNode, currentTime, false, false);
						toRemove.add(fa);
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
				currentPath.add(new Tuple<Integer, Double>(this.id, getCentrality(false)));
				insertMessage(fa, grantEncounteredNode, currentTime, false, false);    
			}
			else if (grantEncounteredNode.bestForwarders.get(message.getDestination()) != null) {
				int bestForwarder = grantEncounteredNode.bestForwarders.get(message.getDestination()).getKey();
				double bestForwarderUtility = grantEncounteredNode.bestForwarders.get(message.getDestination()).getValue();
				double nodeUtility = grantEncounteredNode.phConcentration.get(message.getDestination()).get(id) * 
						(getSocialProximity(message.getDestination()) + getBetweennessUtility(message.getDestination()));
				
				if (id == bestForwarder) {
					// forward ForwardAnt via current best forwarder
					ForwardAnt fa = new ForwardAnt(message);
					List<Tuple<Integer, Double>> currentPath = fa.getCurrentPath();
					currentPath.add(new Tuple<Integer, Double>(grantEncounteredNode.getId(), grantEncounteredNode.getCentrality(false)));
					currentPath.add(new Tuple<Integer, Double>(this.id, getCentrality(false)));
					insertMessage(fa, grantEncounteredNode, currentTime, false, false);    
				}
				
				else if (nodeUtility >= bestForwarderUtility) {
					// update best forwarder & send ForwardAnt
					grantEncounteredNode.bestForwarders.put(message.getDestination(), new Tuple<Integer, Double>(id, nodeUtility));
					// create new ForwardAnt and forward it to this node
					ForwardAnt fa = new ForwardAnt(message);
					List<Tuple<Integer, Double>> currentPath = fa.getCurrentPath();
					currentPath.add(new Tuple<Integer, Double>(grantEncounteredNode.getId(), grantEncounteredNode.getCentrality(false)));
					currentPath.add(new Tuple<Integer, Double>(this.id, getCentrality(false)));
					insertMessage(fa, grantEncounteredNode, currentTime, false, false);    
				}
			}
			else {
				if (grantEncounteredNode.getSocialProximity(message.getDestination()) < getSocialProximity(message.getDestination())) {
					double nodeUtility = grantEncounteredNode.phConcentration.get(message.getDestination()).get(id) * 
							(getSocialProximity(message.getDestination()) + getBetweennessUtility(message.getDestination()));
					// initialize best forwarder
					grantEncounteredNode.bestForwarders.put(message.getDestination(), new Tuple<Integer, Double>(id, nodeUtility));
					// create new ForwardAnt and forward it to this node
					ForwardAnt fa = new ForwardAnt(message);
					List<Tuple<Integer, Double>> currentPath = fa.getCurrentPath();
					currentPath.add(new Tuple<Integer, Double>(grantEncounteredNode.getId(), grantEncounteredNode.getCentrality(false)));
					currentPath.add(new Tuple<Integer, Double>(this.id, getCentrality(false)));
					insertMessage(fa, grantEncounteredNode, currentTime, false, false);  
				}
			}
		}
		
		for (Message message : toRemove) {
			grantEncounteredNode.removeMessage(message, false);
		}
	}
}
