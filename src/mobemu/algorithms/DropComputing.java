/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package mobemu.algorithms;

import java.util.*;

import javax.sound.midi.SysexMessage;

import mobemu.node.Battery;
import mobemu.node.Context;
import mobemu.node.Node;


import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Class for a Drop Computing node.
 *
 * TODO: add paper when published
 *
 * @author Radu
 */
public class DropComputing extends Node {

	/**
	 * Specifies whether a node employs other nodes for task computation or not.
	 */
	boolean opportunistic;
	/**
	 * Specifies whether a node employs the Cloud for task computation or not.
	 */
	boolean useCloud;
	/**
	 * Device information for battery data and task computation.
	 */
	private final DeviceInfo deviceInfo;
	/**
	 * List of tasks this node generates.
	 */
	private final TaskGroup ownTasks;
	/**
	 * List of tasks that have been completed for other nodes.
	 */
	private final List<Task> completedTasks;
	/**
	 * List of tasks that are currently being served by the cloud.
	 */
	private final TaskGroup cloudTasks;
	/**
	 * List of tasks to handle (or move around) for other nodes.
	 */
	private final SortedSet<Task> otherTasks;
	/*
	 * A map between task's id and the list with data version(corrupted or
	 * unmodified)
	 */
	private Map<Integer, List<Integer>> waitingTasks;
	
	private Map<Integer, List<Integer>> mapTaskIdExecutorId;
	private Map<Integer, DropComputing> mapNodeIdExecutorNode;
	
	private MessageDigest messageDigest;	
	
	private static int nodes = -1;
	private static PrintWriter writer;
	
	private boolean useRating = true;
	public boolean isUseRating() {
		return useRating;
	}

	public void setUseRating(boolean useRating) {
		this.useRating = useRating;
	}

	public int getRating() {
		return rating;
	}

	public void setRating(int rating) {
		this.rating = rating;
	}

	private int rating;
	private int timerRating;
	
	/**
	 * Sample time of the trace.
	 */
	private final long sampleTime;
	/**
	 * Random number generator.
	 */
	private static Random RAND = null;
	/**
	 * Device factory.
	 */
	private final static DeviceFactory DEVICE_FACTORY;
	/**
	 * Expiration time of a small task.
	 */
	private static final long TASK_EXPIRATION_TIME = 1;
	/**
	 * Bluetooth transfer speed (in MB/s).
	 */
	private static final int BT_SPEED = 3;
	/**
	 * WiFi transfer speed (in MB/s).
	 */
	private static final int WIFI_SPEED = 5;
	
	private static boolean verifyNumberOfTaskExecutors = true;
	private static int maxPercentageOfTaskExecutors = 0;
	private static boolean corruptCompletedTasksFromEncounteredNodes = true;
	private static boolean corruptTasksAtExecutors = false;
	private boolean useWaitingTasks = true;
	private int numberOfWaitingTasks;

	private double majority = 0.0;
	private int allowCorruptedTasksInGroup = 0;

	static {
		DEVICE_FACTORY = new DeviceFactory();
	}

	/**
	 * Constructor for the {@link DropComputing} class.
	 *
	 * @param id
	 *            ID of the node
	 * @param nodes
	 *            total number of existing nodes
	 * @param context
	 *            the context of this node
	 * @param socialNetwork
	 *            the social network as seen by this node
	 * @param dataMemorySize
	 *            the maximum allowed size of the data memory
	 * @param exchangeHistorySize
	 *            the maximum allowed size of the exchange history
	 * @param seed
	 *            the seed for the random number generators
	 * @param traceStart
	 *            timestamp of the start of the trace
	 * @param traceEnd
	 *            timestamp of the end of the trace
	 * @param deviceType
	 *            type of device for this node
	 * @param sampleTime
	 *            sample time of the trace
	 * @param opportunistic
	 *            specifies whether a node employs other nodes for task
	 *            computation or not
	 * @param useCloud
	 *            specifies whether a node employs other nodes for task
	 *            computation or not
	 */
	public DropComputing(int id, int nodes, Context context, boolean[] socialNetwork, int dataMemorySize,
			int exchangeHistorySize, long seed, long traceStart, long traceEnd, DeviceType deviceType, long sampleTime,
			boolean opportunistic, boolean useCloud) {
		super(id, nodes, context, socialNetwork, dataMemorySize, exchangeHistorySize, seed, traceStart, traceEnd);

		this.opportunistic = opportunistic;
		this.useCloud = useCloud;
		this.sampleTime = sampleTime;
		
		if (useRating) {
			this.rating = 100;
			this.timerRating = 5;
		}
		if(useWaitingTasks){
			waitingTasks = new HashMap<Integer, List<Integer>>();
			numberOfWaitingTasks = 5;
		}
			
		//this condition is true only once
		if( DropComputing.nodes == -1 ) {
			DropComputing.nodes = nodes;
			try{
				DropComputing.writer = new PrintWriter("tasks.txt", "UTF-8");
			}catch(FileNotFoundException ex){
				ex.printStackTrace();
			} catch (UnsupportedEncodingException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}catch(NullPointerException e){
				e.printStackTrace();
			}
			if(useRating)
				System.out.println("Se tine cont de ratingul nodului");
			if(verifyNumberOfTaskExecutors && useWaitingTasks) {
				double percent = (1.0*maxPercentageOfTaskExecutors/100)*numberOfWaitingTasks;
				System.out.println("Acceptam maxim " + percent + " versiuni ale aceluiasi task executate de acelasi nod, adica "  + maxPercentageOfTaskExecutors + "% din " + numberOfWaitingTasks);
				System.out.println("Se asteapta un numar de " + numberOfWaitingTasks + " versiuni ale aceluiasi task inainte de a accepta taskul");
			}
			if(corruptTasksAtExecutors)
				System.out.println("Taskurile se corup de catre cei care le execute => toate nodurile care vor avea taskuri executate de acel executant le vor detine corupte");
			if(corruptCompletedTasksFromEncounteredNodes)
				System.out.println("Taskurile se corup dupa ce au fost primite din lista de taskuri completate ale nodurilor intalnite");
		
		}
		
		deviceInfo = DEVICE_FACTORY.getDevice(deviceType);
		ownTasks = new TaskGroup(id);
		cloudTasks = new TaskGroup(id);
		otherTasks = new TreeSet<>(new TaskComparator(id));
		completedTasks = new ArrayList<>();
		
		mapTaskIdExecutorId = new HashMap<Integer, List<Integer>>();
		mapNodeIdExecutorNode = new HashMap<Integer, DropComputing>();
		
		try{
			messageDigest = MessageDigest.getInstance("SHA-256");
		}catch(NoSuchAlgorithmException ex){
			ex.printStackTrace();
		}
		if (RAND == null) {
			RAND = new Random(seed);
		}

		this.battery = new Battery(RAND.nextDouble() * deviceInfo.fullChargeDuration / sampleTime,
				deviceInfo.fullChargeDuration / sampleTime, deviceInfo.rechargeDuration / sampleTime, 0.2);
	}

	@Override
	public String getName() {
		return "Drop Computing";
	}

	public int whatPercentOfAisB(int A, int B) {

		return 0;
	}

	public double calculatingMajority(List<Integer> list) {
		double majority = 100;
		int nrOfOnes = 0, nrOfZeros = 0;
		for (Integer dataVersion : list) {
			if (dataVersion == 0)
				nrOfZeros++;
			else if (dataVersion == 1)
				nrOfOnes++;
		}
		if (nrOfOnes > nrOfZeros && nrOfZeros != 0)
			majority = (nrOfOnes*100) / list.size();
		else if (nrOfOnes != 0 && nrOfZeros != 0)
			majority = (nrOfZeros*100) / list.size();
		
		if(nrOfZeros > nrOfOnes)
			DropComputingStats.nrOfTasksAcceptCorrupted++;
		else if(nrOfZeros != 0)
			DropComputingStats.nrOfCorruptedTasksButArrivedUncorrupted++;
		
		return majority;
	}
	public boolean verifyHashCollisions(String data1, String data2){
		
		messageDigest.update(data1.getBytes());
		byte[] enString1 = messageDigest.digest();
		
		messageDigest.reset();
		
		messageDigest.update(data2.getBytes());
		byte[] enString2 = messageDigest.digest();
	
		//System.out.println(Arrays.equals(enString1, enString2));
		return Arrays.equals(enString1, enString2);
	}
	
	public void printPath(LinkedList<Integer> path){
		
		System.out.print("[");
		for(int elem : path){
			System.out.print(elem + ",");
		}
		System.out.println("]");
	}
	public boolean isVisited(int elem, LinkedList<Integer> path){
		return path.contains(elem);
	}
	public boolean historyOfTask(int source, int destination, LinkedList<LinkedList<Integer>> adjcent, List<Integer> intermidiateNodes){
	
		//LinkedList<Integer> queue = (LinkedList<Integer>) adjcent.get(source).clone();
	
		LinkedList<LinkedList<Integer>> paths = new LinkedList<>();	
		LinkedList<Integer> firstPath = new LinkedList<Integer>();
		firstPath.add(source);
		paths.add(firstPath);
		
		LinkedList<List<Integer>> pathFinder = new LinkedList<>();
		
		LinkedList<Integer> currentPath;
		int currentVertex, elem;
		Iterator<Integer> it;
		boolean findPath = false, findDestination = false;
		while(!paths.isEmpty()){
			
			findDestination = false;
			currentPath = paths.poll();
			currentVertex = currentPath.peekLast();
			if(currentVertex == destination && !pathFinder.contains(currentPath)){
				System.out.println("S-a gasit calea intre " + source + " si " + destination);
				printPath(currentPath);
				pathFinder.add(currentPath);
				//intermidiateNodes.addAll(currentPath.subList(1, currentPath.size()));
				findPath = true;
				findDestination = true;
				//return true;
			}
			
			
			if(!findDestination){
				it = adjcent.get(currentVertex).iterator();
				
				while(it.hasNext()){
					elem = it.next();
					if( !isVisited(elem, currentPath )){
						LinkedList<Integer> newPath = (LinkedList<Integer>) currentPath.clone();
						newPath.add(elem);
						paths.add(newPath);
					}
				}
			}
		}
		
		if(pathFinder.size() != 1 && findPath){
			System.out.println("s au gasit mai multe cai se alege prima//");
		}
		
		
		if(!findPath)	
			System.out.println("nu s-a gasit cale intre " + source + " si " + destination);
		else
			intermidiateNodes.addAll(pathFinder.get(0).subList(1, pathFinder.get(0).size()));
		
		return findPath;
	}

	@Override
	protected void onDataExchange(Node encounteredNode, long contactDuration, long currentTime) {
		if (!(encounteredNode instanceof DropComputing) || !opportunistic) {
			return;
		}

		DropComputing dcNode = (DropComputing) encounteredNode;
		boolean exchangedData = false;
		int[] exchangedTasksCount = { 0, 0, 0 };

		/*if( dcNode.rating != 100 || this.rating != 100){
			System.out.println("dcnode " + dcNode.id + " " + dcNode.rating);
			System.out.println("this " + this.id + " " + this.rating);
		}*/
		// condition for node closeness
		boolean condition = (encounteredNodes.get(dcNode.id) != null
				&& encounteredNodes.get(dcNode.id).getContacts() >= 2);

		if (battery.canParticipate() && dcNode.battery.canParticipate()) {
			// get all data regarding solved tasks from other node
			List<Task> toRemove = new ArrayList<>();

			for (Task task : dcNode.completedTasks) {
				// the current node gets a message that one of its tasks has finished				
				
				if(task.id == 533 && task.ownerID == id){
					int x = 3;
					System.out.println(x);
				}
				//linia 3967
				if (task.ownerID == id && ownTasks.getTasks().contains(task) && task.hasFinishedAtOtherNode() && !task.listOfSolvers.contains(task.ownerID) ) {	//adaugat aici pentru a verifica versiunea datelor din nodul parinte a taskului cu versiunile pe care le-am primit
					
					if( verifyNumberOfTaskExecutors && !verifyTaskExecutors(mapTaskIdExecutorId.get(task.id), task.mapNodeIdExecutorId.get(dcNode.id)) ){
						//System.out.println("verify task executors failed");
						continue;
					}
					
					//verific ratingul nodului care a executat taskul de la dcNode
					if( useRating && !verifyRating(task.mapNodeIdExecutorNode.get(task.mapNodeIdExecutorId.get(dcNode.id)))){
						continue;
					}
					
					
					addNodeInPath(task.paths, dcNode.id, this.id, task.id);
					
					
					
					//afisez calea pe care a venit taskul
					listTaskPath(task.paths, this.id, task.id, currentTime, task.mapNodeIdExecutorId.get(dcNode.id));
					//task.mapNodeIdExecutorId.get(dcNode.id) ar trebui sa fie actualul executant al taskului chiar daca a mai fost suprascris, o calea a unui task poate fi rezolvata doar de un singur nod
					
					task.listOfNodesMeetByOwner.add(dcNode.id);
					task.map.put(this.id, task.map.get(dcNode.id));
					
					//	task.newMap.put(new SenderAndReceiver(dcNode.id, this.id), new ValueAndTime(task.map.get(dcNode.id), currentTime));
					addValues(task.newMap, new SenderAndReceiver(dcNode.id, this.id), new ValueAndTime(task.map.get(dcNode.id), currentTime, task.mapNodeIdExecutorNode.get(dcNode.id), dcNode));

					task.mapNodeIdExecutorId.put(this.id, task.mapNodeIdExecutorId.get(dcNode.id));
					task.mapNodeIdExecutorNode.put(this.id, task.mapNodeIdExecutorNode.get(dcNode.id)); //nodul care l a executat si pe dcNode
					
					task.listOfVisitedNodes.add(this.id);
						
					verifyTaskPath(task, this.id);
					
					// verificare numarul de noduri la care se gaseste task-ul
					if (useWaitingTasks && waitingTasks.containsKey(task.id)) {
						List<Integer> list = waitingTasks.get(task.id);	
						list.add(task.map.get(dcNode.id));
						
						List<Integer> list2 = mapTaskIdExecutorId.get(task.id);
						list2.add(task.mapNodeIdExecutorId.get(dcNode.id));
						
						if (task.id == 13386) {
							System.out.println("Nodul: " + this.id + " task-ul 133386 este la nodul " + dcNode.id + " " + task.map.get(dcNode.id) + " " + task.map);
						}
						
						waitingTasks.put(task.id, list);
						mapTaskIdExecutorId.put(task.id, list2);
						
						if (waitingTasks.get(task.id).size() == task.nrWaitingVersions)
							verifyWaitingTasks(task);	
						System.out.println("\n\n");
						
					} else if (useWaitingTasks && !waitingTasks.containsKey(task.id)) {

					
							List<Integer> list = new ArrayList<Integer>();
							list.add(task.map.get(dcNode.id));

							waitingTasks.put(task.id, list);
							
							
							List<Integer> list2 = new ArrayList<Integer>();
							list2.add(task.mapNodeIdExecutorId.get(dcNode.id));
							mapTaskIdExecutorId.put(task.id, list2);
							
							if (task.id == 13386) {
								System.out.println("Nodul: " + this.id + " prima data task-ul 13386 este la nodul " + dcNode.id + " " + task.map.get(dcNode.id) + " " + task.map);
							}
					}
					
					//stergem valoarea ultimei muchii, pentru a nu mai fi nevoie de timestamp daca se va mai primi o data de la acel nod
					//????dar se sterg toate versiunile, si se mai intampla sa mai ajunga la owner de la acelasi nod pana sa fie executat
					task.newMap.remove(new SenderAndReceiver(dcNode.id, this.id));
					//se sterge path-ul pe care a venit, ramanand caile pana in nodurile in care inca mai exista
					deletePath(task.paths, this.id);
					
	
					if(!useWaitingTasks){
						verifyTask(task, dcNode.id);
						if(task.id == 533){
							int x = 0;
						}
					
					}
					
					
					toRemove.add(task);
					exchangedData = true;

					// count the number of transferred tasks
					switch (task.type) {
					case SMALL:
						exchangedTasksCount[0]++;
						break;
					case MEDIUM:
						exchangedTasksCount[1]++;
						break;
					default:
						exchangedTasksCount[2]++;
						break;
					}

					// update battery level
					setBatteryOpportunisticExchange(this, dcNode, task.type, sampleTime);
				} else if (!completedTasks.contains(task) && condition) {
					if( ((dcNode.id == 4 && this.id == 28) || (dcNode.id == 28 && this.id == 4) ) && task.id == 533 ){
						int x = 9;
						System.out.println(task.map);
					}
					
					task.addEdge(dcNode.id, this.id);
					addNodeInPath(task.paths, dcNode.id, this.id, task.id);
					
					completedTasks.add(task);
					if (task.map.get(this.id) != null) {
						//System.err.println("nu ar trebuiSa il detina a suprascris " + task.map.get(this.id));
					}
					
					task.map.put(this.id, task.map.get(dcNode.id));
					addValues(task.newMap, new SenderAndReceiver(dcNode.id, this.id), new ValueAndTime(task.map.get(dcNode.id), currentTime, task.mapNodeIdExecutorNode.get(dcNode.id), dcNode));
					//aici il corupt, si ii corup rezolvare
					if( RAND.nextDouble() > 0.95 && corruptCompletedTasksFromEncounteredNodes) {
						task.map.put(this.id, 0);
					}
					task.mapNodeIdExecutorId.put(this.id, task.mapNodeIdExecutorId.get(dcNode.id));
					task.mapNodeIdExecutorNode.put(this.id, task.mapNodeIdExecutorNode.get(dcNode.id)); //adica dcNode

					
					task.listOfVisitedNodes.add(this.id);
					
					exchangedData = true;
					
					// count the number of transferred tasks
					switch (task.type) {
					case SMALL:
						exchangedTasksCount[0]++;
						break;
					case MEDIUM:
						exchangedTasksCount[1]++;
						break;
					default:
						exchangedTasksCount[2]++;
						break;
					}

					// update battery level
					setBatteryOpportunisticExchange(this, dcNode, task.type, sampleTime);
				}
				
				if(!verifyHashCollisions(String.valueOf(task.data), String.valueOf(task.map.get(this.id)))){
					DropComputingStats.corruptedTasks++;
				}
			}

			// compute transfer duration based on task types
			long transferDuration = TaskType.getTransferDuration(TaskType.SMALL, false) * exchangedTasksCount[0]
					+ TaskType.getTransferDuration(TaskType.MEDIUM, false) * exchangedTasksCount[1]
					+ TaskType.getTransferDuration(TaskType.LARGE, false) * exchangedTasksCount[2];

			// clear the current task group if all tasks have finished
			if (exchangedData && ownTasks.hasFinished()) {
				ownTasks.finish(currentTime + transferDuration);

				// compute statistics (increase the number of completed task
				// groups and the total
				// computation duration for all task groups that have completed)
				DropComputingStats.taskGroupsCompleted++;
				DropComputingStats.totalComputationDuration += ownTasks.getCompletionDuration();

				ownTasks.clear();
			}

			// remove all completion messages from encountered node, so they
			// won't be disseminated more
			dcNode.completedTasks.removeAll(toRemove);
			
			
			for (Task task : toRemove) {
				// RC: se face remove la toate entry-urile in map pentru taskurile completate
				/*task.map.remove(dcNode.id);
				task.mapNodeIdExecutorId.remove(dcNode.id);
				task.mapNodeIdExecutorNode.remove(dcNode.id); DELETE, dar se face dupa ce se apeleaza functia de verificare noduri intermediare din verifyTask*/
			}
			
			
			// if the two nodes are not balanced, balance their tasks
			if (!isBalanced(this, dcNode)) {
				List<Task> currentTasks = new ArrayList<>();
				List<Task> otherNodeTasks = new ArrayList<>();

				currentTasks.addAll(otherTasks);
				currentTasks.addAll(dcNode.otherTasks);

				balance(this, dcNode, currentTasks, otherNodeTasks);

				List<Task> toRemoveCurrentNode = new ArrayList<Task>();
				List<Task> toRemoveOtherNode = new ArrayList<Task>();

				// if at least one task has been exchanged between the two
				// devices, increase battery decrease rate
				for (Task task : otherTasks) {
					if (!currentTasks.contains(task)) {
						setBatteryOpportunisticExchange(this, dcNode, task.type, sampleTime);
						// means that the task was transferred at encounteredNode
						task.map.put(dcNode.id, task.map.get(this.id));	
						addValues(task.newMap, new SenderAndReceiver(this.id, dcNode.id), new ValueAndTime(task.map.get(this.id), currentTime));
						
						if(task.mapNodeIdExecutorId.get(this.id) != null){
							//System.err.println("Ar trebuie sa nu aibe executant aici");
						}
						
						addNodeInPath(task.paths, this.id, dcNode.id, task.id);
						
						task.listOfVisitedNodes.add(dcNode.id);

						toRemoveCurrentNode.add(task);
					}
				}

				for (Task task : dcNode.otherTasks) {
					if (!otherNodeTasks.contains(task)) {
						setBatteryOpportunisticExchange(dcNode, this, task.type, sampleTime);
						// means that the task was transferred from
						// encounteredNode to currentNode
						task.map.put(this.id, task.map.get(dcNode.id));
						addValues(task.newMap, new SenderAndReceiver(dcNode.id, this.id), new ValueAndTime(task.map.get(dcNode.id), currentTime));
						
						addNodeInPath(task.paths, dcNode.id, this.id, task.id);
						task.listOfVisitedNodes.add(this.id);
						toRemoveOtherNode.add(task);
					}
				}

				if (id == 12 && (toRemoveCurrentNode.size() > 0 || toRemoveOtherNode.size()> 0))
					System.out.println("12 id");
				
				// RC: recompute condition
				condition = (encounteredNodes.get(dcNode.id) != null
						&& encounteredNodes.get(dcNode.id).getContacts() >= 3);

				
				// remove transferred tasks if the nodes are sufficiently
				// familiar
				if (condition) {
					for( Task task : toRemoveCurrentNode) {
						
						/* de sters calea care s-a terminat in this.id, pentru ca this.id nu mai detine taskul si nu ar mai putea disemina aceasta varianta
						 * 
						 */
						deletePath(task.paths, this.id);
					}
					otherTasks.clear();
				}
				otherTasks.addAll(currentTasks);

				if (condition) {
					
					for(Task task : toRemoveOtherNode)
						deletePath(task.paths, dcNode.id);
					dcNode.otherTasks.clear();
				}
				dcNode.otherTasks.addAll(otherNodeTasks);

			}
		}
	}

	private void deletePath(List<List<Integer>> paths, int id) {
		// TODO Auto-generated method stub
		Iterator<List<Integer>> it = paths.iterator();
		LinkedList<Integer> list;
		boolean succes = false;
		while(it.hasNext()){
			list = (LinkedList<Integer>)it.next();
			if(list.peekLast() == id){
				succes = true;
				it.remove();
				break;
			}
		}
		if(!succes)
			System.err.println("Failed delete path ::");
		
	}

	private void verifyTaskPath(Task task, int id) {
		// TODO Auto-generated method stub
		List<List<Integer>> paths = task.paths;
		Map<SenderAndReceiver, List<ValueAndTime>> newMap = task.newMap;
		Iterator<List<Integer>> it = paths.iterator();
		List<Integer> list = null;
		boolean succes = false;
		
		while(it.hasNext()){
			list = it.next();
			if(list.get(list.size()-1) == id){
				succes = true;
				break;
			}
		}

		if(succes){
			//System.err.println("S-a gasit o cale..de verificat unicitatea \n" + list);
		}

		int executorId = -1,executorIndex = -1;
		try{
			List<ValueAndTime> lastValues = newMap.get(new SenderAndReceiver(list.get(list.size()-2), list.get(list.size()-1)));
			
			if(lastValues != null){
				for( ValueAndTime value : lastValues){
					
					if(value.executorNode != null ) {
						executorId = value.executorNode.id;
						//System.err.println("executant: " + value.executorNode.id + " equal " + task.mapNodeIdExecutorId.get(list.get(list.size()-2)));
					}else System.err.println("S-ar putea: pentru ca se pastreaza si cele care mai ajung din nou la proprietar inainte de a fi executate");
				}
				
				if(executorId == -1)
					System.err.println("Nu s-ar putea: pentru ca ultima valoare, cea cu timestampul cel mai mare TREBUIE sa aibe un executant");
				
				executorIndex = list.indexOf(executorId);
				if(executorIndex == -1)
					System.err.println("Lista " + list + " trebuia sa contina executantul taskului!!! " + executorId);
			}
		}catch(NullPointerException ex){
			System.err.println("Nu exista sender and receiver in?? " + list);
			ex.printStackTrace();
		}
		
		int i, nrNodesWithCorruptedData = 0;
		DropComputing corruptedNode = null;
		
		int index = executorIndex == -1 ? 0:executorIndex;
		for(i = index; i < list.size() - 1; i++) {
			try{
				ValueAndTime valueAndTime = null;
				
				List<ValueAndTime> values = newMap.get(new SenderAndReceiver(list.get(i), list.get(i+1)));
				if(values.size() != 1) {
					System.err.println("Trebuie verificat si timestampul pentru ca a venit de mai multe ori de la " + list.get(i) + " la " + list.get(i+1));
					
					//in cazul in care sunt mai multe variante de la penultimul la owner se va alege acea versiune care are un executant
					//pentru ca se mai poate ajunge la acest schimb si inainte ca taskul sa fie executat
					if( i+1 == list.size() - 1 ) {
						for( ValueAndTime vat : values ){
							if(vat.executorNode != null)
								valueAndTime = vat;
						}
					}else
						valueAndTime = findCorrectVersion(values, list, newMap, i+1);
					
					//trebuie verificat urmatoarele toate care au mai multe trimiteri pana cand se ajunge la nodul care a primit doar o data data,
					//si trebuie verificat cu el timestampul si apoi se merge inapoi catre punctul curent de interes
					
					if(valueAndTime == null){
						System.err.println("nu s a gasit un timestamp mai mic intre " + list.get(i) + " si " + list.get(i+1));
						return ;
					}	
				}else{
					valueAndTime = values.get(0);
				}
				if( !verifyHashCollisions(String.valueOf(valueAndTime.value), String.valueOf(task.data)) ) {
					if(valueAndTime.executorNode == null){
						System.err.println("Nu ar trebui - coruperea datelor se incepe dupa executarea taskului!!");
					}
					System.err.println("S-a transmis de la nodul " + list.get(i)  + " la nodul "+ list.get(i+1) + " data corupta fiind exeutata de: " + valueAndTime.executorNode.id);
					
					//doar primul nod la care se intampla coruperea se retine
					if(nrNodesWithCorruptedData++ == 0){
						if(useRating && corruptTasksAtExecutors && !corruptCompletedTasksFromEncounteredNodes){
							corruptedNode = valueAndTime.executorNode; 
						}
						if(useRating && corruptCompletedTasksFromEncounteredNodes){
							corruptedNode = valueAndTime.currentNode;
						}
					}		
				}
			}catch(NullPointerException ex){
				System.err.println("Lista de path-uri e goala?");
				System.err.println("nu s a gasit cheie de la " + list.get(i) + " la " + list.get(i+1));
				System.err.println(newMap);
				ex.printStackTrace();
			}
		}
		
		//scaderea ratingului unui singur nod - cine a corupt primul( raman cei care incearca coruperea, dar taskul e deja corupt)
		if(corruptedNode != null){
			corruptedNode.rating -= 25;
			corruptedNode.timerRating = 5;
		}
		
	}

	private ValueAndTime findCorrectVersion(List<ValueAndTime> values, List<Integer> list,
			Map<SenderAndReceiver, List<ValueAndTime>> newMap, int index) {
		// TODO Auto-generated method stub
		try{
			List<ValueAndTime> nextValues = newMap.get(new SenderAndReceiver(list.get(index), list.get(index + 1)));
			if(nextValues.size() != 1)
				System.err.println("problema interesanta?!!");	//cred ca sunt sanse sa apara, dar nu apare la rularea asta, iar daca apare ar trebui verificat altfel in continuare
			
			for(ValueAndTime valueAndTime : values){
				if( valueAndTime.timestamp <= nextValues.get(0).timestamp ){					//nu cu asta trebuie verificat si cu primul care trimite o singura data Taskul!!
				//	System.err.println("S a gasit un timestamp mai mic");
					return valueAndTime;
				}else{
					//System.err.println("AiciDAA");
				}
			}
			
		}catch(IndexOutOfBoundsException ex){
			System.err.println("desi se poate atinge, ar trebui sa fie o singura valoarea care ajunge la destinatar,pentru ca atunci se apeleaza functia"); //trebuie adaugata si ultima "muchie" dcNode - > ownerNode in onDataExchange
			System.err.println("Caz special cand ajunge de la acelasi nod la owner si inainte de a fi executat");
			ex.printStackTrace();
		}
		
		return null;
	}

	private void listTaskPath(List<List<Integer>> paths, int id, int taskID, long currentTime, int executorId) {
		System.err.println("\n");
		// TODO Auto-generated method stub
		Iterator<List<Integer>> it = paths.iterator();
		List<Integer> list = null;
		boolean succes = false;
		while(it.hasNext()){
			list = it.next();
			if(list.get(list.size()-1) == id){
				succes = true;
				break;
			}
		}
		if(succes){
			System.err.println("taskul " + taskID + " a venit pe calea " + list + " la momentul de timp " + currentTime);
			System.err.println("executantul " + executorId + " trebuie sa apartine caii:" + list.contains(executorId));
		}else{
			System.err.println("nu s a gasit calea");
		}
		
	}

	
	private void addNodeInPath(List<List<Integer>> paths, int idSender, int idReceiver, int taskId) {
		// TODO Auto-generated method stub
		
		//ar trebui sa existe o unica cale care are pe ultima pozitie acelasi nod - pentru ca nodurile nu accepta un task daca il au deja
		LinkedList<Integer> list = null;
		boolean succes = false;
		
		Iterator<List<Integer>> it = paths.iterator();
		while(it.hasNext()){
			list = (LinkedList<Integer>)it.next();
			if(list.peekLast() == idSender){
				succes = true;
				break;
			}
		}
		
		//pentru a ramane si calea pana la nodul precedent in cazul in care acel nod inca pastreaza taskul
		LinkedList<Integer> list2 = (LinkedList<Integer>)list.clone(); 
		if(succes) {
			list2.add(idReceiver);
			//System.err.println("drumurile existente pentru taskul " + taskId + " au fost " + paths);
			paths.add(list2);
			//System.err.println("S-a gasit drumul pana la " + idSender);
			//System.err.println("acum drumurile existente sunt " + paths);
		}else{
			System.err.println("Nu s-a gasit drumul pana la " + idSender);
			System.err.println("drumurile existente sunt " + paths);
		}
	}

	private void addValues(Map<SenderAndReceiver, List<ValueAndTime>> newMap, SenderAndReceiver senderAndReceiver,
			ValueAndTime valueAndTime) {
		// TODO Auto-generated method stub
		if(newMap.containsKey(senderAndReceiver)){
			List<ValueAndTime> list = newMap.get(senderAndReceiver);
			list.add(valueAndTime);	//nu mai trebuie readaugarea in map
		}else{
			List<ValueAndTime> list = new ArrayList<ValueAndTime>();
			list.add(valueAndTime);
			newMap.put(senderAndReceiver, list);
		}
		
	}

	private void verifyTask(Task task, int encounteredId) {
		// TODO Auto-generated method stub
		System.out.println("task " + task.id + " has arrived at node " + this.id + " from " + encounteredId + " with [" + task.map.get(encounteredId) + "]");
		
		if( String.valueOf(task.map.get(encounteredId)) == null ){
			System.out.println("nu era pastrat in map valoarea taskului " + task.id + " de la nodul " + encounteredId);
		}
		
		int numberOfCorruptedTasks = 0;
		if(!verifyHashCollisions(String.valueOf(task.data), String.valueOf(task.map.get(encounteredId)))){
			numberOfCorruptedTasks++;
			System.out.println("corrupted data from node " + encounteredId + " executed by " + task.mapNodeIdExecutorId.get(encounteredId));
			/*DropComputing dc = task.mapNodeIdExecutorNode.get(encounteredId);
			if(dc == null)
				System.out.println("nu s a recuperat nodul care a corupt");
			else{
				dc.rating -= 25;
				dc.timerRating = 5;
			}*/
		}
		displayTaskHistory(task,numberOfCorruptedTasks==1);
		
		task.listOfNodesMeetByOwner.clear();
		ownTasks.finishTask(task);
		otherTasks.remove(task);	
	}

	private void verifyWaitingTasks(Task task) {
		// TODO Auto-generated method stub
		//System.out.println("inainte!!" + dropComputing.waitingTasks.get(task.id));
		this.majority = calculatingMajority(waitingTasks.get(task.id));
		
		
		
		//se mai asteapta o versiune doar pentru taskul respectiv pentru a avea o majoritate
		if( this.majority == 50.0 ){
			
			//setare numarul de waitingTask doar pentru acest task 
			task.nrWaitingVersions++;
			return ;
		}
			
		
		//nu contine taskul peste care e proprietar?? nu ar trebui sa fie generat de el
		//este false pentru ca poate fi acelasi task prezent de mai multe ori aici, dar dupa ce e prezent o data se elimina din ownTask
		//System.out.println("aiciTest " + ownTasks.getTasks().contains(task));
		System.out.println("task " + task.id + " has enough versions at node " + this.id + " -- " + waitingTasks.get(task.id) );
		
		Iterator<Task> it = ownTasks.getTasks().iterator();
		int currentData = -1;
		Task currentTask;
		while(it.hasNext()){
			currentTask = it.next();
			if(task == currentTask){
				currentData = currentTask.data;
			}
		}
		if(currentData == -1){
			System.err.println("nu ar trebui, nu mai este referinta catre taskul pentru care se asteapta variante?");
		}
		//dar nu se modifica task.data, coruperea se face in task.map, deci nu trebuie facuta asta, in loc de currentData pot sa folosesc task.data nu?
		currentData = task.data;
		 
		
		int counter = -1, numberOfCorruptedTask = 0;
		for(Integer data : waitingTasks.get(task.id) ) {
			counter++;
			if( !verifyHashCollisions(String.valueOf(data),String.valueOf(currentData)) ){
				numberOfCorruptedTask++;
				int executorNodeId = mapTaskIdExecutorId.get(task.id).get(counter);
				System.out.println("corrupted data from node " + task.listOfNodesMeetByOwner.get(counter) + " executed by " + executorNodeId );
				System.out.println("data lui initiala " + currentData + "\nData primita: " + data);
				int nodeIdWithCorruptedData = task.listOfNodesMeetByOwner.get(counter);
				
				if(corruptTasksAtExecutors && !corruptCompletedTasksFromEncounteredNodes){
					try{
						DropComputing copyNode = task.mapNodeIdExecutorNode.get(nodeIdWithCorruptedData);
						copyNode.rating -= 25;
						copyNode.timerRating = 5; //se reseteaza timerul la cel initial
					}catch(NullPointerException ex){
						System.err.println("nodul care l-a executat pe " + nodeIdWithCorruptedData + " nu e salvat");
						System.err.println("cele salvate sunt " + task.mapNodeIdExecutorId);
						ex.printStackTrace();
					}
				}
			}
		}
		
		boolean acceptCorruptedVersion = false;
		if(majority > 50.0 && 2*numberOfCorruptedTask > numberOfWaitingTasks)
			acceptCorruptedVersion = true;
			
		displayTaskHistory(task,acceptCorruptedVersion);
		
		task.listOfNodesMeetByOwner.clear();
		ownTasks.finishTask(task);
		otherTasks.remove(task);	//adaugat aici
		
		
		waitingTasks.remove(task.id);
		
		/*for(int nodeIdMeetByOwner : task.listOfNodesMeetByOwner){
			task.map.remove(nodeIdMeetByOwner);
			task.mapNodeIdExecutorId.remove(nodeIdMeetByOwner);
			task.mapNodeIdExecutorNode.remove(nodeIdMeetByOwner);
		}*/
		/*task.map.remove(this.id);
		task.mapNodeIdExecutorId.remove(this.id);
		task.mapNodeIdExecutorNode.remove(this.id);*/
		
	}

	private boolean verifyRating(DropComputing dropComputing) {
		// TODO Auto-generated method stub
		
		if(dropComputing == null)
			return true;
	//	System.out.println(dropComputing.rating);
		
		dropComputing.timerRating--;
		
		if(dropComputing.rating >= 50 && dropComputing.timerRating == 0 && dropComputing.rating < 100)
			dropComputing.rating += 20;
		
		
		
		return dropComputing.rating >= 75.0;
	}

	private void displayTaskHistory(Task task, boolean corruptedVersion) {
		// TODO Auto-generated method stub
		System.out.println("Partial rezolvat de " + task.listOfPartialSolvers);
		System.out.println("Task-ul " + task.id + " al nodului " + this.id + " a fost rezolvat de nodurile " + task.listOfSolvers);
		
		if(useWaitingTasks)
			System.out.println("Si cele "  + numberOfWaitingTasks + " versiuni au fost primite de la nodurile " + task.listOfNodesMeetByOwner);
		else
			System.out.println("Versiunea a fost primita de la " + task.listOfNodesMeetByOwner);
		
		System.out.println("versiunile au venit pe calea: ");
		
		int crt = -1;
		int executorId;
		List<Integer> intermidiateNodes = new ArrayList<Integer>();
		for(int nodeId : task.listOfNodesMeetByOwner){
			crt++;
			if(useWaitingTasks){
				executorId = mapTaskIdExecutorId.get(task.id).get(crt);
				
			}
			else executorId = task.mapNodeIdExecutorId.get(id);
			
			System.out.println("Asa a ajuns de la sursa la executant ");
			historyOfTask(this.id, executorId, task.adjacent, intermidiateNodes);
			
			intermidiateNodes.clear();
			intermidiateNodes.add(executorId);
			System.out.println("Asa a ajuns de la executant din nou la sursa");
			historyOfTask(executorId, nodeId, task.adjacent, intermidiateNodes);
			
			intermidiateNodes.add(this.id); //la this.id s-a reintors
			System.out.println("nodurile intermediare sunt " + intermidiateNodes);
			verifyIntermediateNodesData(intermidiateNodes,task);
			intermidiateNodes.clear();
			
			/*System.out.println("Direct nu trece mereu printr-un executant");
			historyOfTask(this.id, nodeId, task.adjacent, intermidiateNodes);

			intermidiateNodes.clear();*/
			
		}
		
		if(useWaitingTasks)
			System.out.println("Si cele " + numberOfWaitingTasks + " versiuni au fost executate de " + mapTaskIdExecutorId.get(task.id));
		else
			System.out.println("Versiunea a fost executata de " + task.mapNodeIdExecutorId.get(id));
		//System.out.println("also 3 versiuni " + task.listOfEncounteredSolvers );
		
		System.out.print("Taskul a mai fost prezent in nodurile " + task.listOfVisitedNodes + " cu valorile [");
		for( int task_id : task.listOfVisitedNodes)
			System.out.print( task.map.get(task_id)  + ", ");
		System.out.println("]");
		
		System.out.println("Taskul a ramas prezent in nodurile " + task.map.keySet());
		saveTaskVersions(task.map, task.id, corruptedVersion);
		
		System.out.println(" ");
	}

	private void saveTaskVersions(Map<Integer, Integer> map, int taskId, boolean corruptedVersion) {
		// TODO Auto-generated method stub
		
		DropComputingStats.counter++;
		
		int corruptedVersions = 0, uncorruptedVersions = 0;
		for( int version : map.values() ){
			if(version == 0)
				corruptedVersions++;
			else if(version == 1)
				uncorruptedVersions++;
		}
		if(corruptedVersions > uncorruptedVersions)
			DropComputingStats.moreCorruptedVersionThanOriginalVersion++;
		
		if(corruptedVersion && corruptedVersions >= uncorruptedVersions)
			DropComputingStats.acceptCorruptedVersionInMajority++;
		
		if(corruptedVersion && corruptedVersions < uncorruptedVersions)
			DropComputingStats.acceptCorruptedVersionInMinority++;
		
		if(!corruptedVersion && corruptedVersions >= uncorruptedVersions)
			DropComputingStats.acceptGoodVersionInMinority++;
		
		if(!corruptedVersion && corruptedVersions < uncorruptedVersions)
			DropComputingStats.acceptGoodVersionInMajority++;
		
		
		String message = "Task " + taskId + " had " + corruptedVersions + " corrupted versions and " + uncorruptedVersions + " uncorrupted versions";
		DropComputing.writer.println(message);
	}

	private void verifyIntermediateNodesData(List<Integer> intermidiateNodes, Task task) {
		// TODO Auto-generated method stub
		
		for(int idNode : intermidiateNodes){
			
			if( task.map.get(idNode) == null ){
				System.err.println("s-a pierdut valoarea din nodul " + idNode + " pentru taskul " + task.id);
				System.err.println("valorile retinute sunt " + task.map);
			}
				
			if( !verifyHashCollisions(String.valueOf(task.data), String.valueOf(task.map.get(idNode))) ){
				System.out.println("nodul " + idNode + " are data corupta(pe care trebuie sa o gasim propagata corupta)");
				/*try{
					DropComputing dc = task.mapNodeIdExecutorNode.get(idNode);
					dc.rating -= 25;
					dc.timerRating = 5;
				}catch(NullPointerException ex){
					System.out.println("nodul care l-a executat pe " + idNode + " nu e salvat");
					ex.printStackTrace();
				}*/
				return;
				//8.05 intermidiateNodes nu ar trebui sa aibe primul nod proprietarul(voi retine doar de la executant pana cand intalneste proprietarul iar)
				//ma opresc cu scaderea ratingul la primul la care il gasesc corupt, el l-a corupt, ceilalti doar l-au transportat
				//( dar se poate si ca ei sa incearca coruperea taskului deja corupt si ar merita scaderea ratingului)
			}
		}
		
	}

	private boolean verifyTaskExecutors(List<Integer> list, int executorID) {
		// TODO Auto-generated method stub
		
		if(list == null)
			return true;
		
		//return !list.contains(executorID);
		int numberOfOccurrences = Collections.frequency(list, executorID);

		// adaugat +1 pentru ca se apeleaza inainte de a se  adauga in lista un nou task executat de executorID 
		return numberOfWaitingTasks > numberOfOccurrences*(1 + 1.0*maxPercentageOfTaskExecutors/100);
		//sa asteptam mai multi executanti de taskuri in functie de cate variante ale taskului dorim sa asteptam sa primim rezolvate
			
	}

	/**
	 * Sets the battery decrease rate for an opportunistic exchange between two
	 * nodes.
	 *
	 * @param first
	 *            first node
	 * @param second
	 *            second node
	 * @param type
	 *            type of task
	 * @param sampleTime
	 *            sample time of the trace
	 */
	private static void setBatteryOpportunisticExchange(DropComputing first, DropComputing second, TaskType type,
			long sampleTime) {
		first.battery.setDecreaseRate(first.battery.getDecreaseRate()
				+ first.deviceInfo.getTransferBatteryMultiplier(false, type, sampleTime));
		second.battery.setDecreaseRate(second.battery.getDecreaseRate()
				+ second.deviceInfo.getTransferBatteryMultiplier(false, type, sampleTime));
	}

	/**
	 * Checks whether two DropComputing nodes have their tasks balanced.
	 *
	 * @param first
	 *            first node
	 * @param second
	 *            second node
	 * @return {@code true} if the nodes' tasks are balanced, {@code false}
	 *         otherwise
	 */
	private static boolean isBalanced(DropComputing first, DropComputing second) {

		double firstValue = 0, secondValue = 0;

		for (Task task : first.otherTasks) {
			firstValue += task.computeExecutionTime(first.deviceInfo);
		}

		for (Task task : second.otherTasks) {
			secondValue += task.computeExecutionTime(second.deviceInfo);
		}

		firstValue /= firstValue + secondValue;
		secondValue = 1 - firstValue;

		return Math.abs(firstValue - secondValue) <= 0.2;
	}

	/**
	 * Balances tasks between two encountering nodes.
	 *
	 * @param first
	 *            first node
	 * @param second
	 *            second node
	 * @param tasksFirst
	 *            tasks of the first node
	 * @param tasksSecond
	 *            tasks of the second node
	 */
	private static void balance(final DropComputing first, final DropComputing second, List<Task> tasksFirst,
			List<Task> tasksSecond) {
		
		
		if(first.rating < 75 || second.rating < 75)
			return;
		
		final int BOOST = 1000;

		List<Task> temp1 = new ArrayList<>();
		List<Task> temp2 = new ArrayList<>();
		tasksSecond.clear();
		// here??
		tasksSecond.addAll(tasksFirst);

		// sort tasks in the first list based on importance for the first node
		Collections.sort(tasksFirst, new Comparator<Task>() {
			@Override
			public int compare(Task o1, Task o2) {
				int firstBoost = o1.ownerID == first.id ? BOOST : 0;
				int secondBoost = o2.ownerID == first.id ? BOOST : 0;

				return (int) (firstBoost + o1.computeExecutionTime(first.deviceInfo) - secondBoost
						- o2.computeExecutionTime((first.deviceInfo)));
			}
		});

		// sort tasks in the second list based on importance for the second node
		Collections.sort(tasksSecond, new Comparator<Task>() {
			@Override
			public int compare(Task o1, Task o2) {
				int firstBoost = o1.ownerID == second.id ? BOOST : 0;
				int secondBoost = o2.ownerID == second.id ? BOOST : 0;

				return (int) (firstBoost + o1.computeExecutionTime(second.deviceInfo) - secondBoost
						- o2.computeExecutionTime((second.deviceInfo)));
			}
		});

		boolean node = true;
		// alternatively allow each node to take the most
		// important remaining task (from its standpoint)
		while (!tasksFirst.isEmpty()) {
			Task task;

			if (node) {
				task = tasksFirst.get(tasksFirst.size() - 1);
				temp1.add(task);
				node = false;
			} else {
				task = tasksSecond.get(tasksSecond.size() - 1);
				temp2.add(task);
				node = true;
			}

			tasksFirst.remove(task);
			tasksSecond.remove(task);
		}

		tasksFirst.addAll(temp1);
		tasksSecond.addAll(temp2);
	}

	@Override
	protected void onTick(long currentTime, long sampleTime) {
		super.onTick(currentTime, sampleTime);
		
		// se apeleaza in runTrace pentru fiecare nod la fiecare moment de timp,
		// e implementata in clasa parinte Node

		DropComputingStats.onTickCallNumber++;
		// compute status (inscrease the maximum number of messages stored per
		// node
		if (completedTasks.size() > DropComputingStats.maxMessagesStoredPerNode) {
			DropComputingStats.maxMessagesStoredPerNode = completedTasks.size();
		}

		// compute stats (increase the number of battery depletions)
		if (battery.hasJustDepleted()) {
			DropComputingStats.batteryDepletions++;
		}

		// compute stats (increase the total uptime)
		if (battery.canParticipate()) {
			DropComputingStats.totalUptime += sampleTime;
		}

		// set default decrease rate
		battery.resetDecreaseRate();

		// node with ID 0 deals with managing the cloud's execution
		if (useCloud && id == 0) {
			DropComputingCloud.executeTasks(currentTime, sampleTime);
		}

		// decide whether the current node needs to perform a series of tasks
		if (!ownTasks.hasTasks() && !cloudTasks.hasTasks() && RAND.nextDouble() >= 0.999) {
			SortedSet<Task> newTasks = generateTasks(currentTime);
			ownTasks.addTasks(newTasks);
			otherTasks.addAll(newTasks);
		}

		double batteryDecreaseRate = 0;

		// coruperea datelor random a celor pe care le detin si nu sunt proprietar
		//daca le corup aici voi propaga taskul doar corupt, trebuie implementata coruperea random in lista de taskuri completate fara a distribui acea varianta
		/*for (Task task : otherTasks) {
			//if (currentTime % 450 == 0) {
			if (task.ownerID != id && RAND.nextDouble() > 0.95) {
				task.map.put(this.id, 0);
				// System.out.println("too much");
			} //else
				//task.map.put(this.id, 1); // RC: De ce pui pe 1? Daca un nod a primit informatia si era deja corupta, nu poate sa o de-corupa
		}*/
		// if the current node is already computing a task, continue to run it;
		// if the task finished, remove it from the list of tasks
		if (battery.canParticipate() && !otherTasks.isEmpty()) {
			Task task = otherTasks.first();

			// could the same task be more than once here? that's how i supposed
			// Yes
			/*aici corupeam doar acelasi task de mai multe ori si era doar primul si lista
			 * if (currentTime % 450 == 0) {
				task.map.put(this.id, 0);
			}*/ 

			// compute battery decrease rate based on the type of task
			batteryDecreaseRate = deviceInfo.taskBatteryMultiplier;

			if (task.execute(deviceInfo, sampleTime, id)) {
				
				//executerID este id - setat in functia execute
				task.mapNodeIdExecutorId.put(this.id, task.executorID);
				task.mapNodeIdExecutorNode.put(this.id, this);	//ocupa mult?
				
				//executorId e setat in functia execute
				task.solverBy = id;
				task.listOfSolvers.add(id);
				
				otherTasks.remove(task);
				DropComputingStats.nrOfTasksExecuted++;

				if (task.ownerID == id) {
					// if this was a local task, check if the local task group
					// has finished	
					if (task.id == 13386) {
						System.out.println("Finished here!");
					}
					
					DropComputingStats.executedOwnTaskNumber++;
					
					/* here? why have to finish all the tasks which are own
					 * 
					 * R: because we are supposed that a task contains many other subtasks which are not independently
					 */
					if (ownTasks.hasFinished()) {
						ownTasks.finish(currentTime);

						// compute statistics (increase the number of completed
						// task groups and the total
						// computation duration for all task groups that have
						// completed)
						if(!verifyTasks(ownTasks.tasks,id))
							DropComputingStats.corruptedTaskGroups++;
						
						/*if (!task.modifiedOrUnmodifiedData())
							DropComputingStats.corruptedTaskGroups++;*/
						DropComputingStats.taskGroupsCompleted++;
						DropComputingStats.totalComputationDuration += ownTasks.getCompletionDuration();

						ownTasks.clear();
					}
				} else {
					// if it was a non-local task, add it to the dissemination
					// memory
					// aici adaugat coruperea dupa ce l-a rezolvat, dar trebuie corupt si la nodurile care il transportA
					
					completedTasks.add(task);
					if(RAND.nextDouble() > 0.95 && corruptTasksAtExecutors) {
						task.map.put(this.id, 0);
					}
				}
			}
			if(!verifyHashCollisions(String.valueOf(task.data), String.valueOf(task.map.get(this.id)))){
				DropComputingStats.corruptedTasks++;
			}
		}

		// case for Cloud usage
		if (useCloud && DropComputingCloud.hasFinished(cloudTasks)) {
			// increase battery decrease rate for cloud download
			batteryDecreaseRate += cloudTasks.computeCloudTransferBatteryMultiplier(deviceInfo, sampleTime);

			DropComputingCloud.downloadResult(cloudTasks);
			cloudTasks.finish(currentTime + cloudTasks.getTransferDuration());

			// compute statistics (increase the number of completed task groups
			// and the total
			// computation duration for all task groups that have completed)
			DropComputingStats.taskGroupsCompleted++;
			DropComputingStats.totalComputationDuration += cloudTasks.getCompletionDuration();

			cloudTasks.clear();
		}

		// if the current node is waiting on the result of a task, decide if
		// the time has passed and the task can be done through the cloud
		if (useCloud && ownTasks.hasTasks() && currentTime >= ownTasks.getExpiration()) {
			sendToCloud();

			// increase battery decrease rate for cloud upload
			batteryDecreaseRate += cloudTasks.computeCloudTransferBatteryMultiplier(deviceInfo, sampleTime);

			// compute statistics (increase the number of task group
			// expirations)
			DropComputingStats.taskGroupExpirations++;
		}

		// set decrease rate if it is not the default value
		if (batteryDecreaseRate > 0) {
			battery.setDecreaseRate(battery.getDecreaseRate() + batteryDecreaseRate);
		}
		if (currentTime+sampleTime >= traceEnd){
			DropComputing.writer.close();
		}
	}

	private boolean verifyTasks(SortedSet<Task> tasks, int idNode) {
		// TODO Auto-generated method stub
		int counter = 0;
		for(Task task : tasks){
			if(!verifyHashCollisions(String.valueOf(task.data), String.valueOf(task.map.get(idNode)))){
				counter++;
			}
		}
		
		return counter <= allowCorruptedTasksInGroup;
	}

	/**
	 * Generates a set of tasks at the current node (at least one small task and
	 * a maximum of 5, a maximum of 4 medium tasks, and a maximum of 4 large
	 * tasks).
	 *
	 * @param currentTime
	 *            current trace time
	 * @return sorted set of newly-generated tasks (at least one)
	 */
	private SortedSet<Task> generateTasks(long currentTime) {
		SortedSet<Task> tasks = new TreeSet<>(new TaskComparator(id));

		int smallTasksCount = RAND.nextInt(10) + 1;
		int mediumTasksCount = RAND.nextInt(10);
		int largeTasksCount = RAND.nextInt(10);
		long expiration = currentTime;

		if (opportunistic) {
			expiration += (TASK_EXPIRATION_TIME * smallTasksCount * TaskType.getScalingFactor(TaskType.SMALL)
					+ TASK_EXPIRATION_TIME * mediumTasksCount * TaskType.getScalingFactor(TaskType.MEDIUM)
					+ TASK_EXPIRATION_TIME * largeTasksCount * TaskType.getScalingFactor(TaskType.LARGE)) / 4;
		}

		for (int i = 0; i < smallTasksCount; i++) {
			tasks.add(new Task(id, currentTime, expiration, TaskType.SMALL));
		}

		for (int i = 0; i < mediumTasksCount; i++) {
			tasks.add(new Task(id, currentTime, expiration, TaskType.MEDIUM));
		}

		for (int i = 0; i < largeTasksCount; i++) {
			tasks.add(new Task(id, currentTime, expiration, TaskType.LARGE));
		}

		// compute statistics (increase the number of tasks and task groups
		// created)

		/*
		 * for (Task task : tasks) if( !task.modifiedOrUnmodifiedData() ){
		 * DropComputingStats.corruptedTaskGroups++; break; }
		 */

		/*
		 * if( currentTime % 213 == 0) for (Task task : tasks) task.data = 0;
		 */

		DropComputingStats.taskGroups++;
		DropComputingStats.tasks[0] += smallTasksCount;
		DropComputingStats.tasks[1] += mediumTasksCount;
		DropComputingStats.tasks[2] += largeTasksCount;
		DropComputingStats.nrOfTasksCreated += tasks.size();
		return tasks;
	}

	/**
	 * Starts the execution of all the cloud tasks in the cloud.
	 */
	private void sendToCloud() {
		otherTasks.removeAll(ownTasks.getTasks());
		cloudTasks.moveTasks(ownTasks);
		DropComputingCloud.uploadTasks(cloudTasks);
	}

	/**
	 * Class for a Drop Computing Cloud.
	 */
	private static class DropComputingCloud {

		/**
		 * List of running tasks.
		 */
		private static final SortedSet<TaskGroup> RUNNING_TASKS;
		/**
		 * List of finished tasks.
		 */
		private static final SortedSet<TaskGroup> FINISHED_TASKS;
		/**
		 * List of tasks currently being uploaded.
		 */
		private static final SortedSet<TaskGroup> UPLOADING_TASKS;
		/**
		 * Cloud information.
		 */
		private static final DeviceInfo CLOUD_DEVICE;
		/**
		 * Number of maximum VM instances allowed.
		 */
		private static final int VIRTUAL_MACHINES;

		static {
			FINISHED_TASKS = new TreeSet<>();
			RUNNING_TASKS = new TreeSet<>();
			UPLOADING_TASKS = new TreeSet<>();
			CLOUD_DEVICE = new Cloud();
			VIRTUAL_MACHINES = 5;
		}

		/**
		 * Construct a {@code DropComputingCloud} object.
		 */
		private DropComputingCloud() {
		}

		/**
		 * Executes tasks in the cloud, if any are available.
		 *
		 * @param currentTime
		 *            current trace time
		 * @param sampleTime
		 *            duration between two execution samples
		 */
		static void executeTasks(long currentTime, long sampleTime) {
			// check whether new tasks have finished uploading
			for (Iterator<TaskGroup> iterator = UPLOADING_TASKS.iterator(); iterator.hasNext();) {
				TaskGroup tasks = iterator.next();
				if (tasks.upload(sampleTime)) {
					RUNNING_TASKS.add(tasks);
					iterator.remove();
				}
			}

			int availableVMs = VIRTUAL_MACHINES;

			if (!RUNNING_TASKS.isEmpty()) {
				for (TaskGroup toExecute : RUNNING_TASKS) {

					for (Task task : toExecute.tasks) {
						if (!task.hasFinishedAtOtherNode() && availableVMs > 0) {
							// cloud has MAX_VALUE ID as task executor
							task.execute(CLOUD_DEVICE, sampleTime, Integer.MAX_VALUE);
							availableVMs--;

							// compute stats (increase cloud utilization)
							DropComputingStats.cloudUsageTime += sampleTime;
						}
					}

					if (toExecute.hasFinishedAtOtherNode()) {
						FINISHED_TASKS.add(toExecute);
					}
				}

				RUNNING_TASKS.removeAll(FINISHED_TASKS);
			}

			// compute status (update the number of active VMs in the cloud)
			DropComputingStats.activeVMsInCloud.add(VIRTUAL_MACHINES - availableVMs);
			DropComputingStats.activeVMsInCloudTime.add(currentTime);
		}

		/**
		 * Checks whether the cloud has finished executing the given task group
		 *
		 * @param group
		 *            task group to check
		 * @return {@code true} if the task group was fully executed,
		 *         {@code false} otherwise
		 */
		static boolean hasFinished(TaskGroup group) {
			return FINISHED_TASKS.contains(group);
		}

		/**
		 * Uploads a task group to the cloud.
		 *
		 * @param group
		 */
		static void uploadTasks(TaskGroup group) {
			UPLOADING_TASKS.add(group);
		}

		/**
		 * Downloads the result of a task computation in the cloud.
		 *
		 * @param group
		 *            task group whose result is wanted
		 * @return {@code true} if the task had finished, {@code false}
		 *         otherwise
		 */
		static boolean downloadResult(TaskGroup group) {
			if (FINISHED_TASKS.contains(group)) {
				return FINISHED_TASKS.remove(group);
			}

			return false;
		}
	}

	/**
	 * Type of mobile device.
	 */
	public enum DeviceType {

		CLOUD, LG_G5, HTC_ONE_M9, IPHONE_5S, IPHONE_6, IPHONE_6S, SAMSUNG_GALAXY_S4, SAMSUNG_GALAXY_S5, SAMSUNG_GALAXY_S6, SAMSUNG_GALAXY_S7
	}

	/**
	 * Factory class for device types.
	 */
	private static class DeviceFactory {

		/**
		 * Constructs a {@code DeviceFactory} object.
		 */
		private DeviceFactory() {
		}

		/**
		 * Creates a new {@link DeviceInfo} object.
		 *
		 * @param type
		 *            type of the object to be instantiated.
		 * @return a new {@link DeviceInfo} object
		 */
		DeviceInfo getDevice(DeviceType type) {
			switch (type) {
			default:
			case CLOUD:
				return new Cloud();
			case LG_G5:
				return new LGG5();
			case HTC_ONE_M9:
				return new HTCOneM9();
			case IPHONE_5S:
				return new IPhone5s();
			case IPHONE_6:
				return new IPhone6();
			case IPHONE_6S:
				return new IPhone6s();
			case SAMSUNG_GALAXY_S4:
				return new SamsungGalaxyS4();
			case SAMSUNG_GALAXY_S5:
				return new SamsungGalaxyS5();
			case SAMSUNG_GALAXY_S6:
				return new SamsungGalaxyS6();
			case SAMSUNG_GALAXY_S7:
				return new SamsungGalaxyS7();
			}
		}
	}

	/**
	 * Class for information regarding a device.
	 */
	private static abstract class DeviceInfo {

		protected static final int HOURS_IN_MILLIS = 60 * 60 * 1000;
		/**
		 * Duration of a full charge (ms).
		 */
		protected double fullChargeDuration;
		/**
		 * Duration of a recharge (ms).
		 */
		protected double rechargeDuration = 2 * HOURS_IN_MILLIS;
		/**
		 * Battery multiplier when transferring data through WiFi.
		 */
		protected double wifiBatteryMultiplier = 2.87;
		/**
		 * Battery multiplier when transferring data through Bluetooth.
		 */
		protected double bluetoothBatteryMultiplier = 1.65;
		/**
		 * Battery multiplier when computing tasks.
		 */
		protected double taskBatteryMultiplier = 2.70;
		/**
		 * Duration of computing a megacycle, i.e. a small task (ms).
		 */
		protected double megaCycleDuration;

		/**
		 * Computes the battery multiplier using this device for a task
		 * transfer.
		 *
		 * @param wifi
		 *            {@code true} if the transfer is done using WiFi,
		 *            {@code false} if Bluetooth is employed
		 * @param taskType
		 *            type of task
		 * @param sampleTime
		 *            sample time of the trace
		 * @return the battery multiplier of this device when performing a
		 *         transfer
		 */
		public double getTransferBatteryMultiplier(boolean wifi, TaskType taskType, long sampleTime) {
			return TaskType.getTransferDuration(taskType, wifi)
					* (wifi ? wifiBatteryMultiplier : bluetoothBatteryMultiplier) / sampleTime;
		}
	}

	/**
	 * Device info for Cloud.
	 */
	private static class Cloud extends DeviceInfo {

		/**
		 * Constructs a {@code Cloud} object.
		 */
		public Cloud() {
			fullChargeDuration = 0;

			// duration of a Megacycle on an Amazon t2.small node
			// (Intel Xeon with Turbo up to 3.3GHz)
			megaCycleDuration = 0.303;
		}
	}

	/**
	 * Device info for LG G5.
	 */
	private static class LGG5 extends DeviceInfo {

		/**
		 * Constructs an {@code LGG5} object.
		 */
		public LGG5() {
			fullChargeDuration = 45 * HOURS_IN_MILLIS;
			megaCycleDuration = 0.535;
		}
	}

	/**
	 * Device info for HTC One M9.
	 */
	private static class HTCOneM9 extends DeviceInfo {

		/**
		 * Constructs an {@code HTCOneM9} object.
		 */
		public HTCOneM9() {
			fullChargeDuration = 46 * HOURS_IN_MILLIS;
			megaCycleDuration = 0.500;
		}
	}

	/**
	 * Device info for iPhone 5s.
	 */
	private static class IPhone5s extends DeviceInfo {

		/**
		 * Constructs an {@code IPhone5s} object.
		 */
		public IPhone5s() {
			fullChargeDuration = 44 * HOURS_IN_MILLIS;
			megaCycleDuration = 0.769;
		}
	}

	/**
	 * Device info for iPhone 6.
	 */
	private static class IPhone6 extends DeviceInfo {

		/**
		 * Constructs an {@code IPhone6} object.
		 */
		public IPhone6() {
			fullChargeDuration = 49 * HOURS_IN_MILLIS;
			megaCycleDuration = 0.714;
		}
	}

	/**
	 * Device info for iPhone 6s.
	 */
	private static class IPhone6s extends DeviceInfo {

		/**
		 * Constructs an {@code IPhone6s} object.
		 */
		public IPhone6s() {
			fullChargeDuration = 51 * HOURS_IN_MILLIS;
			megaCycleDuration = 0.543;
		}
	}

	/**
	 * Device info for Samsung Galaxy S4.
	 */
	private static class SamsungGalaxyS4 extends DeviceInfo {

		/**
		 * Constructs a {@code SamsungGalaxyS4} object.
		 */
		public SamsungGalaxyS4() {
			fullChargeDuration = 50 * HOURS_IN_MILLIS;
			megaCycleDuration = 0.625;
		}
	}

	/**
	 * Device info for Samsung Galaxy S5.
	 */
	private static class SamsungGalaxyS5 extends DeviceInfo {

		/**
		 * Constructs a {@code SamsungGalaxyS5} object.
		 */
		public SamsungGalaxyS5() {
			fullChargeDuration = 61 * HOURS_IN_MILLIS;
			megaCycleDuration = 0.400;
		}
	}

	/**
	 * Device info for Samsung Galaxy S6.
	 */
	private static class SamsungGalaxyS6 extends DeviceInfo {

		/**
		 * Constructs a {@code SamsungGalaxyS6} object.
		 */
		public SamsungGalaxyS6() {
			fullChargeDuration = 57 * HOURS_IN_MILLIS;
			megaCycleDuration = 0.476;
		}
	}

	/**
	 * Device info for Samsung Galaxy S7.
	 */
	private static class SamsungGalaxyS7 extends DeviceInfo {

		/**
		 * Constructs a {@code SamsungGalaxyS7} object.
		 */
		public SamsungGalaxyS7() {
			fullChargeDuration = 59 * HOURS_IN_MILLIS;
			megaCycleDuration = 0.535;
		}
	}

	/**
	 * Class for a group of tasks.
	 */
	private class TaskGroup implements Comparable<TaskGroup> {

		private int corruptedTasks;
		/**
		 * The tasks for the group.
		 */
		private SortedSet<Task> tasks;
		/**
		 * Timestamp when the last task of the group has been completed.
		 */
		private long finishTimestamp;
		/**
		 * Duration to upload this task group.
		 */
		private long uploadDuration;

		/**
		 * Constructs a {@code TaskGroup} object.
		 *
		 * @param id
		 *            ID of the task group
		 */
		public TaskGroup(int id) {
			tasks = new TreeSet<>(new TaskComparator(id));
			finishTimestamp = -1;
			uploadDuration = 0;
		}

		/**
		 * Checks whether the task group has tasks.
		 *
		 * @return {@code true} if the task group has tasks, {@code false}
		 *         otherwise
		 */
		boolean hasTasks() {
			return !tasks.isEmpty();
		}

		/**
		 * Adds a new task to the task group.
		 *
		 * @param task
		 *            task to be added
		 */
		public void addTask(Task task) {
			tasks.add(task);
			uploadDuration += task.getCloudUploadDuration();
		}

		/**
		 * Adds a set of tasks to the task group.
		 *
		 * @param tasks
		 *            set of tasks to be added
		 */
		public void addTasks(SortedSet<Task> tasks) {
			for (Task task : tasks) {
				if (!task.hasFinished()) {
					this.tasks.add(task);
					uploadDuration += task.getCloudUploadDuration();
				}
			}
		}

		/**
		 * Gets the task group's tasks.
		 *
		 * @return the task group's tasks
		 */
		public SortedSet<Task> getTasks() {
			return tasks;
		}

		/**
		 * Clears the tasks in this task group.
		 */
		public void clear() {
			tasks.clear();
			uploadDuration = 0;
		}

		/**
		 * Moves the tasks from the task group to the current task group.
		 *
		 * @param other
		 *            task group where the new tasks are taken from
		 */
		public void moveTasks(TaskGroup other) {
			addTasks(other.tasks);
			other.clear();
			restartTasks();
		}

		/**
		 * Finishes a task from this task group.
		 *
		 * @param task
		 *            task to be finished
		 * @return {@code true} if the task was in this group and finished,
		 *         {@code false} otherwise
		 */
		public boolean finishTask(Task task) {
			if (tasks.contains(task) && !task.hasFinished()) {
				task.finish();
				return true;
			}

			return false;
		}

		/**
		 * Marks this task group as finished.
		 *
		 * @param timestamp
		 *            timestamp when this task group was finished
		 */
		public void finish(long timestamp) {
			finishTimestamp = timestamp;

			for (Task task : tasks) {
				task.finish();
			}
		}

		/**
		 * Restarts the tasks in the task group (when being moved to the cloud).
		 */
		public void restartTasks() {
			for (Task task : tasks) {
				task.restartTask();
			}
		}

		/**
		 * Computes the duration it took to finish this task group.
		 *
		 * @return the duration it took for this task group to finish, or -1 if
		 *         the task hasn't completed
		 */
		public long getCompletionDuration() {
			return hasFinished() ? getFinishTime() - getTimestamp() : -1;
		}

		/**
		 * Gets the expiration time of the task group.
		 *
		 * @return expiration time of the task group
		 */
		public long getExpiration() {
			return hasTasks() ? tasks.first().expiration : -1;
		}

		/**
		 * Gets the finish time of the task group.
		 *
		 * @return finish time of the task group
		 */
		public long getFinishTime() {
			return finishTimestamp;
		}

		/**
		 * Gets the timestamp of the task group.
		 *
		 * @return timestamp of the task group
		 */
		public long getTimestamp() {
			return hasTasks() ? tasks.first().timestamp : -1;
		}

		/**
		 * Gets the task group's ID (i.e. ID of the first task in the group).
		 *
		 * @return the task group's ID
		 */
		public int getId() {
			return hasTasks() ? tasks.first().id : -1;
		}

		/**
		 * Executes the next task in this task group.
		 *
		 * @param device
		 *            device that executes the task group
		 * @param sampleTime
		 *            time between two execution samples
		 * @param executorID
		 *            ID of the executing node
		 * @return {@code true} if the task group has finished, {@code false}
		 *         otherwise
		 */
		public boolean execute(DeviceInfo device, long sampleTime, int executorID) {
			System.out.println("se apeleaza??");
			int finishedTasks = 0;

			for (Task task : tasks) {

				if (task.modifiedOrUnmodifiedData()) {		//aici
					System.out.println("aici2");
					corruptedTasks++;
					
				}

				if (task.hasFinished()) {
					finishedTasks++;
					continue;
				}

				if (task.execute(device, sampleTime, executorID)) {
					finishedTasks++;
				}
			}

			return finishedTasks == tasks.size();
		}

		public int getCorruptedTasks() {
			return corruptedTasks;
		}

		public void setCorruptedTasks(int corruptedTasks) {
			this.corruptedTasks = corruptedTasks;
		}

		/**
		 * Decreases this task group's upload duration.
		 *
		 * @param sampleTime
		 *            sample time of the trace
		 * @return {@code true} if the task has finished uploading,
		 *         {@code false} otherwise
		 */
		public boolean upload(long sampleTime) {
			uploadDuration -= sampleTime;
			return uploadDuration <= 0;
		}

		/**
		 * Computes the total transfer duration of this task group.
		 *
		 * @return the transfer duration
		 */
		public long getTransferDuration() {
			long ret = 0;

			for (Task task : tasks) {
				ret += task.getCloudUploadDuration();
			}

			return ret;
		}

		/**
		 * Computes the battery multiplier for transferring this task group to
		 * the cloud.
		 *
		 * @param device
		 *            device that uploads the task group
		 * @param sampleTime
		 *            sample time of the trace
		 * @return the battery multiplier for transferring this task group to
		 *         the cloud
		 */
		private double computeCloudTransferBatteryMultiplier(DeviceInfo device, long sampleTime) {
			double multiplier = 0;

			for (Task task : tasks) {
				multiplier += device.getTransferBatteryMultiplier(true, task.type, sampleTime);
			}

			return multiplier;
		}

		/**
		 * Checks whether all tasks from this task group have finished.
		 *
		 * @return {@code true} if all tasks have finished, {@code false}
		 *         otherwise
		 */
		public boolean hasFinished() {
			int finishedTasks = 0;

			for (Task task : tasks) {
				if (task.hasFinished()) {
					finishedTasks++;
				}
			}

			return finishedTasks > 0 && finishedTasks == tasks.size();
		}

		/**
		 * Checks whether all tasks from this task group have finished at a
		 * different node than the current one.
		 *
		 * @return {@code true} if all tasks have finished, {@code false}
		 *         otherwise
		 */
		public boolean hasFinishedAtOtherNode() {
			int finishedTasks = 0;

			for (Task task : tasks) {
				if (task.hasFinishedAtOtherNode()) {
					finishedTasks++;
				}
			}

			return finishedTasks > 0 && finishedTasks == tasks.size();
		}

		@Override
		public int compareTo(TaskGroup o) {
			long diff = this.getTimestamp() - o.getTimestamp();

			if (diff > 0) {
				return 1;
			} else if (diff < 0) {
				return -1;
			} else {
				return this.getId() - o.getId();
			}
		}
	}

	/**
	 * Type of computational task.
	 */
	private enum TaskType {

		SMALL, MEDIUM, LARGE;

		/**
		 * Gets the scaling factor for a certain type of task.
		 *
		 * @param type
		 *            task type
		 * @return scaling factor for the given task type
		 */
		public static int getScalingFactor(TaskType type) {
			switch (type) {
			default:
			case SMALL:
				return 1;
			case MEDIUM:
				return 1000;
			case LARGE:
				return 10000;
			}
		}

		/**
		 * Gets the duration (in ms) that it takes for a type of task to be
		 * transferred through Bluetooth or WiFi. We assume tasks of 5 MB, a
		 * speed of 5 MB/s for Cloud download and upload, and 3 MB/s for
		 * Bluetooth transfer (which is the maximum speed of Bluetooth 3.0).
		 *
		 * @param type
		 *            task type
		 * @param wifi
		 *            {@code true} if the task is uploaded using WiFi,
		 *            {@code false} if Bluetooth is employed
		 * @return duration (in ms) that it takes to upload transfer this type
		 *         of task
		 */
		public static long getTransferDuration(TaskType type, boolean wifi) {
			double transferSizeMB = 5;
			transferSizeMB /= getScalingFactor(type);

			double duration = transferSizeMB;
			duration /= wifi ? WIFI_SPEED : BT_SPEED;
			// transform to ms
			duration *= 1000;

			return (long) Math.ceil(duration);
		}
	}

	/**
	 * Number of generated tasks.
	 */
	static int numberOfTasks = 0;

	/**
	 * Class used for representing a task.
	 */
	private class SenderAndReceiver{
		private int senderId;
		private int receiverId;
		public SenderAndReceiver(int senderId, int receiverId) {
			super();
			this.senderId = senderId;
			this.receiverId = receiverId;
		}
		public int getSenderId() {
			return senderId;
		}
		public void setSenderId(int senderId) {
			this.senderId = senderId;
		}
		public int getReceiverId() {
			return receiverId;
		}
		public void setReceiverId(int receiverId) {
			this.receiverId = receiverId;
		}
		@Override
		public boolean equals(Object obj) {
			// TODO Auto-generated method stub
			return ((SenderAndReceiver)obj).senderId == this.senderId && ((SenderAndReceiver)obj).receiverId == this.receiverId;
		}
		
		@Override
		public int hashCode() {
			// TODO Auto-generated method stub
			return 11;
		}
		@Override
		public String toString() {
			return "SenderAndReceiver [senderId=" + senderId + ", receiverId=" + receiverId + "]";
		}
		
		
	}
	private class ValueAndTime{
		private int value;
		private long timestamp;
		private DropComputing executorNode;
		private DropComputing currentNode;
		
		public int getValue() {
			return value;
		}
		public void setValue(int value) {
			this.value = value;
		}
		public long getTimestamp() {
			return timestamp;
		}
		public void setTimestamp(long timestamp) {
			this.timestamp = timestamp;
		}
		public ValueAndTime(int value, long currentTime) {
			super();
			this.value = value;
			this.timestamp = currentTime;
			this.executorNode = null;
			this.currentNode = null;
		}
		
		public ValueAndTime(int value, long timestamp, DropComputing executorNode, DropComputing currentNode) {
			super();
			this.value = value;
			this.timestamp = timestamp;
			this.executorNode = executorNode;
			this.currentNode = currentNode;
		}
		@Override
		public String toString() {
			// TODO Auto-generated method stub
			return "Valoare: " + value + " timestamp: " + timestamp;
		}
	}
	private class ExecutorAndTime{
		private int executorId;
		private long timestamp;
		public ExecutorAndTime(int executorId, long timestamp) {
			super();
			this.executorId = executorId;
			this.timestamp = timestamp;
		}
		@Override
		public String toString() {
			return "ExecutorAndTime [executorId=" + executorId + ", timestamp=" + timestamp + "]";
		}
		
	}
	private class Task {

		public int nrWaitingVersions;
		/*
		 * data of the current task, 0 means it was corrupted (1 means
		 * unmodified)
		 */
		private int data = 1;
		private boolean solvedByOwner;
		private int solverBy;
		private List<Integer> listOfSolvers;
		private List<Integer> listOfPartialSolvers;
		//private List<Integer> listOfEncounteredSolvers;
		private Set<Integer> listOfVisitedNodes;
		private List<Integer> listOfNodesMeetByOwner;
		
		
		private Map<Integer,Integer> mapNodeIdExecutorId;
		private Map<Integer, Integer> map;
		
		private Map<SenderAndReceiver, List<ValueAndTime>> newMap;
		//private Map<SenderAndReceiver, List<>>
		private List<List<Integer>> paths;
		
		private LinkedList<LinkedList<Integer>> adjacent;
	
		private Map<Integer, DropComputing> mapNodeIdExecutorNode;
		/*
		 * List of variants of executed tasks(corrupted or unmodified)
		 */
		private List<Integer> list;

		/**
		 * ID of the current task.
		 */
		private int id;
		/**
		 * Type of the current task.
		 */
		private TaskType type;
		/**
		 * Timestamp when the task was created.
		 */
		private long timestamp;
		/**
		 * Time when the task expires (for the opportunistic case).
		 */
		private long expiration;
		/**
		 * Duration of uploading this task to the cloud.
		 */
		private final long cloudUploadDuration;
		/**
		 * ID of the owner of this task.
		 */
		private int ownerID;
		/**
		 * ID of the current executor of the task.
		 */
		private int executorID;
		/**
		 * Remaining execution time for this task.
		 */
		private double remainingTime;
		/**
		 * Initial task execution time.
		 */
		private double initialTime;

		/**
		 * Constructs a {@code Task} object.
		 *
		 * @param ownerID
		 *            ID of the owner of the task
		 * @param timestamp
		 *            timestamp when the task was created
		 * @param expiration
		 *            time when the task expires
		 * @param type
		 *            type of the task
		 */
		public Task(int ownerID, long timestamp, long expiration, TaskType type) {
			this.id = numberOfTasks++;
			this.ownerID = ownerID;
			this.executorID = -1;
			this.timestamp = timestamp;
			this.expiration = expiration;
			this.type = type;
			this.remainingTime = Double.MIN_VALUE;
			this.initialTime = Double.MIN_VALUE;
			this.cloudUploadDuration = computeCloudUploadDuration();

			this.map = new HashMap<Integer, Integer>();
			this.map.put(this.ownerID, 1); // map ul este intre id-ul nodurilor
											// si valoarea
			this.newMap = new HashMap<SenderAndReceiver, List<ValueAndTime>>();
			this.nrWaitingVersions = numberOfWaitingTasks;
			
			this.paths = new LinkedList<>();
			List<Integer> list = new LinkedList<Integer>();
			list.add(ownerID);
			this.paths.add(list);
			
			this.solvedByOwner = false;
			this.mapNodeIdExecutorId = new HashMap<Integer, Integer>();
			this.mapNodeIdExecutorId.put(this.ownerID, executorID);		//map intre id-ul nodurilor prezent si id-ul nodului care l-a executat
			this.mapNodeIdExecutorNode = new HashMap<Integer, DropComputing>();
			
			this.data = 1;
			this.list = new ArrayList<Integer>();
			
			this.listOfSolvers = new ArrayList<Integer>();
			this.listOfPartialSolvers = new ArrayList<Integer>();
			
			this.listOfVisitedNodes = new HashSet<Integer>();
			this.listOfVisitedNodes.add(this.ownerID);
			
			
			this.listOfNodesMeetByOwner = new ArrayList<Integer>();
			this.adjacent = new LinkedList<>();
			for(int i = 0; i < DropComputing.nodes; ++i){
				this.adjacent.add(new LinkedList<Integer>());
			}
		}

		public void addEdge(int node, int encounteredNode){
			
			//sa nu l mai contina
			if(!this.adjacent.get(node).add(encounteredNode))
				System.out.println("error add edge");
		}
		
		/*
		 * checking if the data was corrupted or not
		 */
		public boolean modifiedOrUnmodifiedData() {
			return this.data == 1;
		}

		// here?? initial nu sunt taskuri si se generezeaza aleator un numar de
		// 10 taskuri pentru fiecare nod
		/**
		 * Executes a task on a tick.
		 *
		 * @param device
		 *            type of device executing this task
		 * @param sampleTime
		 *            sample time of the trace
		 * @param executorID
		 *            the ID of the node executing this task
		 * @return {@code true} if the task has finished, {@code false}
		 *         otherwise
		 */
		public boolean execute(DeviceInfo device, long sampleTime, int executorID) {
			
			/*if (solvedByOwner) {
				//System.out.println("A fost rezolvat de owner si nu mai trebuie executat");
				return false;
			}*/
			
			this.executorID = executorID;
			this.listOfPartialSolvers.add(executorID);
			
			
			// if this is the start of running this task, set initial remaining
			// time
			if (initialTime == Double.MIN_VALUE) {
				initialTime = computeExecutionTime(device);
				remainingTime = initialTime;
			}

			remainingTime -= sampleTime;

			return remainingTime <= 0;
		}

		/**
		 * Compute task execution time for the specified device.
		 *
		 * @param device
		 *            device executing the task
		 * @return task execution time for the specified device
		 */
		public double computeExecutionTime(DeviceInfo device) {
			return device.megaCycleDuration * TaskType.getScalingFactor(type);
		}

		/**
		 * Checks whether the task has finished running.
		 *
		 * @return {@code true} if the task has finished running, {@code false}
		 *         if it hasn't started, or if it has started but hasn't
		 *         finished
		 */
		public boolean hasFinished() {
			return remainingTime <= 0 && remainingTime != Double.MIN_VALUE && executorID == ownerID;
		}

		/**
		 * Checks whether the task has finished running at a different node.
		 *
		 * @return {@code true} if the task has finished running, {@code false}
		 *         if it hasn't started, or if it has started but hasn't
		 *         finished
		 */
		public boolean hasFinishedAtOtherNode() {
			return remainingTime <= 0 && remainingTime != Double.MIN_VALUE;
		}

		/**
		 * Restarts a task (i.e., resets the initial time).
		 */
		public void restartTask() {
			initialTime = Double.MIN_VALUE;
		}

		/**
		 * Finishes the current task (i.e., sets the remaining time to 0).
		 */
		public void finish() {
			remainingTime = 0;
			executorID = ownerID;
		}

		/**
		 * Gets this task's upload duration.
		 *
		 * @return this task's upload duration
		 */
		public long getCloudUploadDuration() {
			return cloudUploadDuration;
		}

		/**
		 * Computes this task's upload duration.
		 *
		 * @return this task's upload duration
		 */
		private long computeCloudUploadDuration() {
			return TaskType.getTransferDuration(type, true);
		}
	}

	/**
	 * Comparator class for {@link Task} objects.
	 */
	class TaskComparator implements Comparator<Task> {

		/**
		 * Task ID.
		 */
		private final int id;

		/**
		 * Constructs a {@code TaskComparator} object.
		 *
		 * @param id
		 *            ID of the task
		 */
		public TaskComparator(int id) {
			this.id = id;
		}

		@Override
		public int compare(Task t1, Task t2) {
			if (t1.ownerID == t2.ownerID) {
				return getDiff(t1, t2);
			} else if (t1.ownerID == id) {
				return -1;
			} else if (t2.ownerID == id) {
				return 1;
			} else {
				return getDiff(t1, t2);
			}
		}

		/**
		 * Computes the expiration difference between two tasks.
		 *
		 * @param t1
		 *            first task
		 * @param t2
		 *            second task
		 * @return expiration difference between the tasks
		 */
		private int getDiff(Task t1, Task t2) {
			long diff = t1.expiration - t2.expiration;

			if (diff > 0) {
				return 1;
			} else if (diff < 0) {
				return -1;
			} else {
				return t1.id - t2.id;
			}
		}
	}

	/**
	 * Statistics for Drop Computing computation.
	 */
	public static class DropComputingStats {

		public static int counter = 0;
		public static int acceptGoodVersionInMajority = 0;
		public static int acceptGoodVersionInMinority = 0;
		public static int acceptCorruptedVersionInMinority;
		public static int acceptCorruptedVersionInMajority = 0;
		public static int moreCorruptedVersionThanOriginalVersion = 0;
		public static int corruptedTaskGroups = 0;
		public static int corruptedTasks = 0;

		public static int executedOwnTaskNumber = 0;
		public static int onTickCallNumber = 0;
		public static int nrOfTasksCreated = 0;
		public static int nrOfTasksExecuted = 0;
		public static int nrOfNodesLowRating = 0;
		public static int nrOfNodesHighRating = 0;
		
		public static int nrOfTasksAcceptCorrupted = 0;
		public static int nrOfCorruptedTasksButArrivedUncorrupted = 0;
		/**
		 * Total number of task groups.
		 */
		public static int taskGroups = 0;
		/**
		 * Number of tasks of each type.
		 */
		public static int[] tasks;
		/**
		 * Total number of completed task groups.
		 */
		public static int taskGroupsCompleted = 0;
		/**
		 * Total number of task group expirations.
		 */
		public static int taskGroupExpirations = 0;
		/**
		 * Total number of battery depletions.
		 */
		public static int batteryDepletions = 0;
		/**
		 * Total computation duration (for all tasks).
		 */
		public static long totalComputationDuration = 0;
		/**
		 * Total uptime (i.e., the uptime for each mobiel device in the
		 * simulation).
		 */
		public static long totalUptime = 0;
		/**
		 * Total cloud usage time.
		 */
		public static long cloudUsageTime = 0;
		/**
		 * List of active VMs in the cloud when computing tasks.
		 */
		public static List<Integer> activeVMsInCloud;
		/**
		 * List of timestamps for the active VMs.
		 */
		public static List<Long> activeVMsInCloudTime;
		/**
		 * Maximum messages stored per node.
		 */
		public static int maxMessagesStoredPerNode = 0;

		static {
			activeVMsInCloud = new ArrayList<>();
			activeVMsInCloudTime = new ArrayList<>();
			tasks = new int[3];
			tasks[0] = 0;
			tasks[1] = 0;
			tasks[2] = 0;
		}

		/**
		 * Computes the task completion rate.
		 *
		 * @return the completion rate of all the complete task groups
		 */
		public static double getCompletionRate() {
			return (double) taskGroupsCompleted / taskGroups;
		}

		/**
		 * Computes the number of task group expirations.
		 *
		 * @return the number of task group expirations
		 */
		public static double getTaskGroupExpirations() {
			return (double) taskGroupExpirations / taskGroups;
		}

		/**
		 * Computes the average task computation duration.
		 *
		 * @return averate task computation duration
		 */
		public static double getAverageTaskDuration() {
			double sum = tasks[0] + (double) TaskType.getScalingFactor(TaskType.MEDIUM) * tasks[1]
					+ (double) TaskType.getScalingFactor(TaskType.LARGE) * tasks[2];

			return totalComputationDuration / sum;
		}
	}
}
