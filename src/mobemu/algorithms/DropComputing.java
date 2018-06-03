/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package mobemu.algorithms;

import java.util.*;

import javax.sound.midi.SysexMessage;

import jdk.net.NetworkPermission;
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
	private Map<Integer, List<Data>> waitingTasks;
	private Map<Integer, List<int[]>> waitingTasksHammingCode;
	
	private Map<Integer, List<Map<Integer, Data>>> taskHistory;
	
	
	private Map<Integer, List<Integer>> mapTaskIdExecutorId;
	private Map<Integer, DropComputing> mapNodeIdExecutorNode;
	private Map<Integer, Double> nodesRating;
	private Map<Integer, Double> nodesTimerRating;
	
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

	public double getRating() {
		return rating;
	}

	public void setRating(int rating) {
		this.rating = rating;
	}

	private double rating;
	private int timerRating;
	private final int timerRatingValue = 3;
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
	private boolean useHammingCode = true;
	
	private MutableBoolean correctOneBit;
	
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
			this.timerRating = this.timerRatingValue;
			nodesRating = new HashMap<Integer,Double>();
			nodesRating.put(id, 100.0);
			int counterFriends = 0;
			for( int i = 0; i < socialNetwork.length; i++) {
				if(socialNetwork[i]) {
					nodesRating.put(i, 100.0);
					counterFriends++;
				}
			}
			System.err.println("Nodul " + id + " are " + counterFriends + " prieteni in retea!!");
			nodesTimerRating = new HashMap<Integer,Double>();
		}
		if (useWaitingTasks) {
			waitingTasks = new HashMap<Integer, List<Data>>();
			numberOfWaitingTasks = 3; //il putem seta altfel la hamming
		}
		//trebuie indiferent de waitingTask sau hammingCode pentru ca e parametru in verifyHash si dupa ce se iese din functie se seteaza la false
		correctOneBit = new MutableBoolean(false); 
			
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
				System.out.println("Acelasi task poate fi executat de maxim " + Math.floor(numberOfWaitingTasks/(1 + 1.0*maxPercentageOfTaskExecutors/100)) + "ori de acelasi nod din totalul de " + numberOfWaitingTasks + " versiuni");
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
		taskHistory = new HashMap<Integer,List<Map<Integer,Data>>>();
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

	int testare = 0;
	public double calculatingMajority(List<Data> list) {
		
		List<Integer> frequencies = new ArrayList<Integer>();
		//if(!useHammingCode)
			for(Data elem : list) {
				frequencies.add(Collections.frequency(list, elem));
			}
		
		int maxFrequency = Collections.max(frequencies);
		
		double majority = (maxFrequency*100)/list.size();
		
		
		/*majority = 100;
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
			DropComputingStats.nrOfCorruptedTasksButArrivedUncorrupted++;*/
		
		return majority;
	}
	public boolean verifyHashCollisions(Data taskData, Data data, Task task, MutableBoolean correctOneBit){
		
		//System.err.println("cele doua date::" + taskData + " si " + data);
		String oldData = null, newData = null;
		if( useHammingCode && taskData.hammingData != null ) {
			oldData = Arrays.toString(taskData.hammingData);
			//se incearca corectarea unui singur bit si se verifica dupa daca coincid
			int[] correctHamming = task.receive(data.hammingData, task.parityBits, correctOneBit);
			newData = Arrays.toString(correctHamming);
		}
		
		if( !useHammingCode && taskData.simpleData != -1) {
			oldData = String.valueOf(taskData.simpleData);
			newData = String.valueOf(data.simpleData);
		}
		messageDigest.update(oldData.getBytes());
		byte[] enString1 = messageDigest.digest();
		
		messageDigest.reset();
		
		messageDigest.update(newData.getBytes());
		byte[] enString2 = messageDigest.digest();
	
		boolean result = Arrays.equals(enString1, enString2);
		/*if(result && correctOneBit.isValue()) {
			System.err.println("taskul " + task.id + " a fost corupt dar s-a corectat cu hamming");
			correctOneBit.setValue(false);
		}
		if(!result && correctOneBit.isValue()) {
			System.err.println("taskul " + task.id + " a avut mai mult de o eroare, s-a corectat doar una");
			correctOneBit.setValue(false);
		}*/
		/*if(!result && !correctOneBit.isValue())
			System.err.println("nu s a putut corecta!!");*/
		return result; 
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
		
	/*	if( (nodesRating.get(dcNode.id) != null && nodesRating.get(dcNode.id) != 100 ) ) {
			System.err.println("rating1");
		}
		if (dcNode.nodesRating.get(this.id) != null && dcNode.nodesRating.get(this.id) != 100 )  {
			System.err.println("rating2");
		}*/
		
		// condition for node closeness
		boolean condition = (encounteredNodes.get(dcNode.id) != null
				&& encounteredNodes.get(dcNode.id).getContacts() >= 2);
		List<Integer> currentPath = null;
		
		if (battery.canParticipate() && dcNode.battery.canParticipate()) {
			// get all data regarding solved tasks from other node
			List<Task> toRemove = new ArrayList<>();

			for (Task task : dcNode.completedTasks) {
				// the current node gets a message that one of its tasks has finished				
				
				if(task.id == 533 && task.ownerID == id){
					int x = 3;
					System.out.println(x);
				}

				if (task.ownerID == id && ownTasks.getTasks().contains(task) && task.hasFinishedAtOtherNode() && !task.listOfSolvers.contains(task.ownerID) ) {	//adaugat aici pentru a verifica versiunea datelor din nodul parinte a taskului cu versiunile pe care le-am primit
					
					if( verifyNumberOfTaskExecutors && !verifyTaskExecutors(mapTaskIdExecutorId.get(task.id), task.mapNodeIdExecutorId.get(dcNode.id)) ){
						//System.out.println("verify task executors failed");
						continue;
					}
					
					//verific ratingul nodului care a executat taskul de la dcNode(AICII)
					/*if( useRating && !verifyRating(task.mapNodeIdExecutorNode.get(dcNode.id))){
						continue;
					}*/
					
					if (useRating && !verifyRating(dcNode.id)) {
					//	System.err.println("Nodul " + dcNode.id + " a fost sarit pt ca are ratingul " + nodesRating.get(dcNode.id));
						continue;
					}

					if (useWaitingTasks && task.nrReceivedVersions > task.nrWaitingVersions)
						continue;

					if (task.id == 188) {
						System.err.println("AICIII");
						printPathsFromSourceToDest(task.paths, dcNode.id);

					}

					if (useRating) {
						aggregateRatings(this.nodesRating, this.nodesTimerRating, dcNode.nodesRating, dcNode.id,
								dcNode.nodesTimerRating);
					}
					addNodeInPath(task.paths, dcNode.id, this.id, task.id, task.newMap, task.mapNodeIdExecutorId.get(dcNode.id), task.ownerID, true);
					//afisez calea pe care a venit taskul si o salvez
					currentPath = printAndSaveTaskPath(task.paths, this.id, task.id, currentTime, task.mapNodeIdExecutorId.get(dcNode.id),dcNode.id);
					/*if(task.id == 533) {
						for(int el : currentPath)
							System.err.print(task.mapNodeIdExecutorId.get(el) + " ");
						System.err.println(task.paths);
					}*/
					//task.mapNodeIdExecutorId.get(dcNode.id) ar trebui sa fie actualul executant al taskului chiar daca a mai fost suprascris, o calea a unui task poate fi rezolvata doar de un singur nod
					
					task.listOfNodesMeetByOwner.add(dcNode.id);
					
					
					
					
					//	task.newMap.put(new SenderAndReceiver(dcNode.id, this.id), new ValueAndTime(task.map.get(dcNode.id), currentTime));
					task.map.put(this.id, task.map.get(dcNode.id));
					addValues(task.newMap, new SenderAndReceiver(dcNode.id, this.id), new ValueAndTime(task.map.get(dcNode.id), currentTime, task.mapNodeIdExecutorId.get(dcNode.id), dcNode.id));
										
					task.mapNodeIdExecutorId.put(this.id, task.mapNodeIdExecutorId.get(dcNode.id));
					task.mapNodeIdExecutorNode.put(this.id, task.mapNodeIdExecutorNode.get(dcNode.id)); //nodul care l a executat si pe dcNode
					
					task.listOfVisitedNodes.add(this.id);
						
					verifyTaskPath(task, currentPath, this.id);
					saveTasksMap(taskHistory, task.id, task.map);
					
				//	taskHistory.put(task.id, task.map);
					
					
					// verificare numarul de noduri la care se gaseste task-ul
					if (useWaitingTasks && waitingTasks.containsKey(task.id)) {
						
						List<Integer> list2 = mapTaskIdExecutorId.get(task.id);
						list2.add(task.mapNodeIdExecutorId.get(dcNode.id));
						//optionalmapTaskIdExecutorId.put(task.id, list2);
						task.nrReceivedVersions++;

						List<Data> list = waitingTasks.get(task.id);
						list.add(task.map.get(dcNode.id));

							if (task.id == 13386) {
								System.out.println("Nodul: " + this.id + " task-ul 133386 este la nodul " + dcNode.id
										+ " " + task.map.get(dcNode.id) + " " + task.map);
							}

						waitingTasks.put(task.id, list);
							
						if (waitingTasks.get(task.id).size() == task.nrWaitingVersions)
								verifyWaitingTasks(task, taskHistory.get(task.id));

							System.out.println("\n\n");
							/*if(useHammingCode && waitingTasksHammingCode.containsKey(task.id)) {
							List<int[]> list = waitingTasksHammingCode.get(task.id);
							list.add(task.mapHamming.get(dcNode.id));
							
							if(list.size() == task.nrWaitingVersions)
								verifyWaitingTasksHammingCode(task, taskHistory.get(task.id));
							System.out.println("\n\n");

						}
						*/
					}else if (useWaitingTasks && !waitingTasks.containsKey(task.id)) {

							task.nrReceivedVersions++;
							List<Data> list = new ArrayList<Data>();
							list.add(task.map.get(dcNode.id));
							waitingTasks.put(task.id, list);
							
							List<Integer> list2 = new ArrayList<Integer>();
							list2.add(task.mapNodeIdExecutorId.get(dcNode.id));
							mapTaskIdExecutorId.put(task.id, list2);
							task.nrReceivedVersions++;
							
							if (task.id == 13386) {
								System.out.println("Nodul: " + this.id + " prima data task-ul 13386 este la nodul " + dcNode.id + " " + task.map.get(dcNode.id) + " " + task.map);
							}
					}
					
					//stergem valoarea ultimei muchii, pentru a nu mai fi nevoie de timestamp daca se va mai primi o data de la acel nod
					//????dar se sterg toate versiunile, si se mai intampla sa mai ajunga la owner de la acelasi nod pana sa fie executat, deci se va verifica incpeand cu executantul
					//trebuie verificat incepand de la ultima aparitie a executantului ca putea sa mai ajunga la nodul executant si mai inainte de a fi executat
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
					addNodeInPath(task.paths, dcNode.id, this.id, task.id, task.newMap, task.mapNodeIdExecutorId.get(dcNode.id), task.ownerID, true);
					if( useRating )
					aggregateRatings(this.nodesRating, this.nodesTimerRating, dcNode.nodesRating, dcNode.id, dcNode.nodesTimerRating);
					
					completedTasks.add(task);
					
					
					task.map.put(this.id, task.map.get(dcNode.id));
					addValues(task.newMap, new SenderAndReceiver(dcNode.id, this.id), new ValueAndTime(task.map.get(dcNode.id), currentTime, task.mapNodeIdExecutorId.get(dcNode.id), dcNode.id));
					//aici il corupt, si ii corup rezolvare
					if( RAND.nextDouble() > 0.95 && corruptCompletedTasksFromEncounteredNodes && !useHammingCode ) {
						task.map.put(this.id, new Data(0));
					}
					if( RAND.nextDouble() > 0.95 && corruptCompletedTasksFromEncounteredNodes && useHammingCode ) {
						int nrCorruptedPosition = 1 + RAND.nextInt(3);			// se poate corupte 1, 2 sau 3 pozitii
						int[] corruptedData = new int[task.extendedData.length];
						System.arraycopy(task.extendedData, 0, corruptedData, 0, task.extendedData.length);
						int position;
						while(nrCorruptedPosition > 0) {
							position = 1 + RAND.nextInt(task.extendedData.length); //trebuie la mai multe pozitii
							System.err.println("s-a corupt pozitia " + position + " din vectorul ede lungime " + task.extendedData.length + " la taskul " + task.id);
							corruptedData[position-1] ^= 1;
							nrCorruptedPosition--;
						}
						
						//System.out.println("s-a corupt pozitia " + position + " la taskul " + task.id);
						
						task.map.put(this.id, new Data(corruptedData));
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
				
				if (task.map.get(this.id) != null)	//in caz ca a salvat un task, se verifica daca a fost corupt sau nu
					if (!verifyHashCollisions(task.taskData, task.map.get(this.id), task, correctOneBit)) {
						if(correctOneBit.value == true)
							//System.err.println("S-a corectat un singur bit, dar au fost si altii corupti");
						correctOneBit.setValue(false);
						DropComputingStats.corruptedTasks++;
					}else if(correctOneBit.value == true) {
						System.err.println("S-a corectat un bit si data a devenit corecta");
						DropComputingStats.nrOfTasksHammingCorrection++;
						correctOneBit.setValue(false);
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
			for(Task task : toRemove)
				deletePath(task.paths, dcNode.id);
			/*for (Task task : toRemove) {
				// RC: se face remove la toate entry-urile in map pentru taskurile completate
				task.map.remove(dcNode.id);
				task.mapNodeIdExecutorId.remove(dcNode.id);
				task.mapNodeIdExecutorNode.remove(dcNode.id); //DELETE, dar se face dupa ce se apeleaza functia de verificare noduri intermediare din verifyTask
			}*/
			
			
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
					if (!currentTasks.contains(task) && !dcNode.completedTasks.contains(task)) {
						setBatteryOpportunisticExchange(this, dcNode, task.type, sampleTime);
						// means that the task was transferred at encounteredNode
						task.map.put(dcNode.id, task.map.get(this.id));	
						addValues(task.newMap, new SenderAndReceiver(this.id, dcNode.id), new ValueAndTime(task.map.get(this.id), currentTime));
						
						
						if(task.mapNodeIdExecutorId.get(this.id) != null && task.mapNodeIdExecutorId.get(this.id) != -1){
							System.err.println("Ar trebui sa nu aiba executant aici");
							/* pentru ca e setat la -1 in constructor, dar poate avea daca e setatat ca un nod sa accepte o versiune neexecutata chiar daca o are el deja executata*/
						}
						
						addNodeInPath(task.paths, this.id, dcNode.id, task.id, task.newMap,task.executorID, task.ownerID, false);
						
						task.listOfVisitedNodes.add(dcNode.id);

						toRemoveCurrentNode.add(task);
					}
				}

				for (Task task : dcNode.otherTasks) {
					if (!otherNodeTasks.contains(task) && !this.completedTasks.contains(task)) {
						setBatteryOpportunisticExchange(dcNode, this, task.type, sampleTime);
						// means that the task was transferred from
						// encounteredNode to currentNode
						task.map.put(this.id, task.map.get(dcNode.id));
						addValues(task.newMap, new SenderAndReceiver(dcNode.id, this.id), new ValueAndTime(task.map.get(dcNode.id), currentTime));
						
						if(task.mapNodeIdExecutorId.get(dcNode.id) != null && task.mapNodeIdExecutorId.get(dcNode.id) != -1){
							System.err.println("Ar trebui sa nu aiba executant nici aici");
							//task.mapNodeIdExecutorId.put(this.id, task.mapNodeIdExecutorId.get(dcNode.id));
						
						}
						addNodeInPath(task.paths, dcNode.id, this.id, task.id, task.newMap, task.executorID, task.ownerID, false);
						task.listOfVisitedNodes.add(this.id);
						toRemoveOtherNode.add(task);
					}
				}
				
			//	aggregateRatings(this.nodesRating, this.id, dcNode.nodesRating, dcNode.id);
				if(useRating)
					aggregateRatings(this.nodesRating, this.nodesTimerRating, dcNode.nodesRating, dcNode.id, dcNode.nodesTimerRating);
				if (id == 12 && (toRemoveCurrentNode.size() > 0 || toRemoveOtherNode.size()> 0))
					System.out.println("12 id");
				
				// RC: recompute condition
				/*condition = (encounteredNodes.get(dcNode.id) != null
						&& encounteredNodes.get(dcNode.id).getContacts() >= 3);*/

				
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

	private void aggregateRatings(Map<Integer, Double> nodesRating1, Map<Integer, Double> nodesTimerRating1,
			Map<Integer, Double> nodesRating2, int dcNodeId, Map<Integer, Double> nodesTimerRating2) {
		// TODO Auto-generated method stub
		double timer1, timer2, min;
		boolean set = false;
		for (int key : nodesRating2.keySet()) {
			if (nodesRating1.containsKey(key) && socialNetwork[dcNodeId]) {
				adjustRating(nodesRating1, nodesRating2, key, 60, 40);
				set = true;
			}

			if (nodesRating1.containsKey(key) && !socialNetwork[dcNodeId]) {
				adjustRating(nodesRating1, nodesRating2, key, 80, 20);
				set = true;
			}

			if (!nodesRating1.containsKey(key) && socialNetwork[dcNodeId]) {
				nodesRating1.put(key, nodesRating2.get(key));
				set = true;
			}

			if (set) {
				/*timer1 = nodesTimerRating1.get(key) == null ? 0 : nodesTimerRating1.get(key);
				timer2 = nodesTimerRating2.get(key) == null ? 0 : nodesTimerRating2.get(key);
				min = Math.min(timer2, timer1);
				if(min < 0)
					System.err.println("Sepoate?");*/
				nodesTimerRating1.put(key, timerRatingValue*1.0);
				nodesTimerRating2.put(key, timerRatingValue*1.0);
			}
		}
		for (int key : nodesRating1.keySet()) {
			if (!nodesRating2.containsKey(key) && socialNetwork[dcNodeId])
				nodesRating2.put(key, nodesRating1.get(key));
		}
		/*if(socialNetwork[dcNodeId]) {
			nodesRating1.put(dcNodeId, 100.0);
		}
*/
	}

	private void adjustRating(Map<Integer, Double> nodesRating1, Map<Integer, Double> nodesRating2, int key, int i,
			int j) {

		double rating1 = 0, rating2 = 0;
		if( nodesRating1 != null)
			rating1 = nodesRating1.get(key);
		if( nodesRating2 != null)
			rating2 = nodesRating2.get(key);
		
		double value1 = (i*rating1 + j*rating2)/(i+j);
		double value2 = (i*rating2 + j*rating1)/(i+j);
		if( nodesRating1 != null)
			nodesRating1.put(key, value1);
		if( nodesRating2 != null)
			nodesRating2.put(key, value2);
	}

	private void saveTasksMap(Map<Integer, List<Map<Integer, Data>>> taskHistory2, int id,
			Map<Integer, Data> map) {
		// TODO Auto-generated method stub
		List<Map<Integer, Data>> list;
		if(taskHistory2.containsKey(id)) {
			list = taskHistory2.get(id);
			list.add(map);
		}else {
			list = new LinkedList<Map<Integer, Data>>();
			list.add(map);
			taskHistory2.put(id, list);
		}
		
	}

	private void deletePath(List<List<Integer>> paths, int id) {
		// TODO Auto-generated method stub
		ListIterator<List<Integer>> it = paths.listIterator(paths.size());

		
		boolean succes = false;
		boolean alone = false;
		int counter = 0;
		LinkedList<Integer> list;
		while(it.hasPrevious()){
			list = (LinkedList<Integer>)it.previous();
			/*if(list.peekLast() == id) {
				alone = true;
			}*/
			if( list.peekLast() == id ){	//1.06
				succes = true;
				it.remove();
				
				counter++;
				break;
			}
		}
		if(counter > 1)
			System.err.println("S-au sters mai multe cai");
		if(!succes && alone)
			it.remove();
		else if(!succes)
			System.err.println("Nu s-a sters nicio cale");
	}

	private void verifyTaskPath(Task task, List<Integer> list, int id) {
		// TODO Auto-generated method stub
		Map<SenderAndReceiver, List<ValueAndTime>> newMap = task.newMap;
		int executorId = -1,executorIndex = -1;
		try{
			List<ValueAndTime> lastValues = newMap.get(new SenderAndReceiver(list.get(list.size()-2), list.get(list.size()-1)));
			
			if(lastValues != null){
				List<Integer> executanti = new ArrayList<Integer>();
				for( ValueAndTime value : lastValues){
					
					if(value.executorNodeId != -1 ) {
						executorId = value.executorNodeId;
						executanti.add(executorId);
						//System.err.println("executant: " + value.executorNode.id + " equal " + task.mapNodeIdExecutorId.get(list.get(list.size()-2)));
					}else System.err.println("S-ar putea: pentru ca se pastreaza si cele care mai ajung din nou la proprietar inainte de a fi executate");
				}
				
				if(executorId == -1)
					System.err.println("Nu s-ar putea: pentru ca ultima valoare, cea cu timestampul cel mai mare TREBUIE sa aibe un executant");
				
				//15.05setare moment de timpcand s-a terminat de executat un task 1.06 ADAUGARE LAST INDEX
				executorIndex = list.lastIndexOf(executorId);
				if(executorIndex == -1) {
					System.err.println("Lista " + list + " trebuia sa contina executantul taskului!!! " + executorId);
					System.err.println("Drumurile pana la " + list.get(list.size()-2) + " au fost: ");
					printPathsFromSourceToDest(task.paths, list.get(list.size()-2));
					System.err.println("Toti executantii " + executanti);
				}
			}
		}catch(NullPointerException ex){
			System.err.println("Nu exista sender and receiver in?? " + list);
			ex.printStackTrace();
		}
		
		int i, nrNodesWithCorruptedData = 0;
		boolean corruptedVersion;
		DropComputing corruptedNode = null;
		
		//executantul poate aparea de mai multe ori, se alege indexul care verifica faptul ca taskul de la el la urmatorul are un executant si e mai mic ca urmatorul
		
		
		int index = executorIndex == -1 ? 0:executorIndex;
		int corruptedNodeId = -1;
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
							if(vat.executorNodeId != -1)
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
					if(valueAndTime.executorNodeId == -1) {
						System.err.println("Ar trebui sa nu se mai intample acum!!");
						continue;
					}
				}else{
					valueAndTime = values.get(0);
					if(valueAndTime.executorNodeId == -1)			//a venit o singura data de la executant la urmatorul in calea gasita dar atunci nu era rezolvat
						continue;
				}
				boolean verifyCorruption = verifyHashCollisions(task.taskData, valueAndTime.data, task, correctOneBit);
				if( !verifyCorruption || (verifyCorruption && correctOneBit.value == true) ) {
					/*chiar daca s-a corectat cu hamming nodul tot a corupt taskul si merita scaderea ratingului*/
					correctOneBit.setValue(false);
					if(valueAndTime.executorNodeId == -1){
						System.err.println("Nu ar trebui - coruperea datelor se incepe dupa executarea taskului!!");
					}
					System.err.println("S-a transmis de la nodul " + list.get(i)  + " la nodul "+ list.get(i+1) + " data corupta fiind exeutata de: " + valueAndTime.executorNodeId);
					
					//doar primul nod la care se intampla coruperea se retine
					if(nrNodesWithCorruptedData++ == 0){
						if(useRating && corruptTasksAtExecutors && !corruptCompletedTasksFromEncounteredNodes){
							corruptedNode = valueAndTime.executorNode; 
							corruptedNodeId = valueAndTime.executorNodeId;
						}
						if(useRating && corruptCompletedTasksFromEncounteredNodes){
							corruptedNode = valueAndTime.currentNode;
							corruptedNodeId = valueAndTime.currentNodeId;
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
		
		/*NU stiu ce a vrut sa insemne asta??corruptedVersion = 2*nrNodesWithCorruptedData > nrNodesWithCorruptedData ? true : false;
		saveTaskVersions(task.map, task.id, corruptedVersion);
		*/
		//scaderea ratingului unui singur nod - cine a corupt primul( raman cei care incearca coruperea, dar taskul e deja corupt)
		if(corruptedNodeId != -1){
			//corruptedNode.rating -= 25;   //(de testat aici ca n am rulat fara sa scot asta)
			if(nodesRating.containsKey(corruptedNodeId)) {
				nodesRating.put(corruptedNodeId, nodesRating.get(corruptedNodeId)-25);
			}else {
				nodesRating.put(corruptedNodeId, 75.0);
			}
			
			nodesTimerRating.put(corruptedNodeId, timerRatingValue*1.0);
			//corruptedNode.timerRating = timerRatingValue;
		}
		
	}

	private void printPathsFromSourceToDest(List<List<Integer>> paths, Integer integer) {
		// TODO Auto-generated method stub
		Iterator<List<Integer>> it = paths.iterator();
		LinkedList<Integer> list = null;
		LinkedList<Integer> path = null;
		List<List<Integer>> foundedPaths = new LinkedList<List<Integer>>();
		
		
		int ok = 0;
		while(it.hasNext()){
			list = (LinkedList<Integer>)it.next();
			if(list.peekLast() == integer){			
				path = (LinkedList<Integer>)list.clone();
				foundedPaths.add(path);
				ok++;
			}
		}
		System.err.println(foundedPaths);
	}

	private ValueAndTime findCorrectVersion(List<ValueAndTime> values, List<Integer> list,
			Map<SenderAndReceiver, List<ValueAndTime>> newMap, int index) {
		// TODO Auto-generated method stub
		try{
			int counter = 0;
			ValueAndTime vat = null;
			for(ValueAndTime value : values) {
				if(value.executorNodeId != -1) {
					counter++;
					vat = value;
				}	
			}
			if(counter == 1)
				return vat;
			else {
				//System.err.println("de gandit!");
			}
			List<ValueAndTime> nextValues;
			while(true) {///AIICICICICIICIC
				nextValues = newMap.get(new SenderAndReceiver(list.get(index), list.get(index++ + 1)));
				if(nextValues.size() != 1)
					System.err.println("problema interesanta?!!");	//cred ca sunt sanse sa apara, dar nu apare la rularea asta, iar daca apare ar trebui verificat altfel in continuare
				
				for(ValueAndTime valueAndTime : values){
					if( valueAndTime.timestamp <= nextValues.get(0).timestamp && nextValues.get(0).executorNodeId != -1 ){					//nu cu asta trebuie verificat si cu primul care trimite o singura data Taskul!!
					//	System.err.println("S a gasit un timestamp mai mic");
						return valueAndTime;
					}else{
						//System.err.println("AiciDAA");
					}
				}
			}
			
		}catch(IndexOutOfBoundsException ex){
			System.err.println("desi se poate atinge, ar trebui sa fie o singura valoarea care ajunge la destinatar,pentru ca atunci se apeleaza functia"); //trebuie adaugata si ultima "muchie" dcNode - > ownerNode in onDataExchange
			System.err.println("Caz special cand ajunge de la acelasi nod la owner si inainte de a fi executat");
			ex.printStackTrace();
		}
		
		return null;
	}

	private LinkedList<Integer> printAndSaveTaskPath(List<List<Integer>> paths, int id, int taskID, long currentTime, int executorId, int dcNodeID) {
		System.err.println("\n");
		// TODO Auto-generated method stub
		Iterator<List<Integer>> it = paths.iterator();
		LinkedList<Integer> list = null;
		LinkedList<Integer> path = null;
		List<List<Integer>> foundedPaths = new LinkedList<List<Integer>>();
		
		boolean succes = false;
		int ok = 0;
		while(it.hasNext()){
			list = (LinkedList<Integer>)it.next();
			if(list.peekLast() == id){
				succes = true;
				path = (LinkedList<Integer>)list.clone();
				foundedPaths.add(path);
				ok++;
			}
		}
		if(succes){
			System.err.println("taskul " + taskID + " a venit pe calea " + path + " la momentul de timp " + currentTime);
			System.err.println("executantul " + executorId + " trebuie sa apartine caii:" + path.contains(executorId));
		}else{
			System.err.println("nu s a gasit calea");
		}
		
		if(ok > 1) {
			System.err.println("s au gasit mai multe cai");
			System.err.println(foundedPaths);
			//posibil in momentul in care se mai pastreaza taskul neexecutat in nodul proprietar, se va salva ultimul drum din lista(care este si cel potrivit)
		}
		
		return path;
	}

	
	private void addNodeInPath(List<List<Integer>> paths, int idSender, int idReceiver, int taskId, Map<SenderAndReceiver, List<ValueAndTime>> newMap, int idExecutor, int idOwner, boolean completed) {
		// TODO Auto-generated method stub
		
		/*if(idOwner == idExecutor) {	//aceasta metoda se aplica si cand se fac schimb de task-uri necompletate, iar taskul poate ajunge sa fie executat de owner intre timp
			deletePath(paths, idOwner);
			return ;
		}*/
		
		boolean succes = false;
		LinkedList<Integer> list2 = null, list = null;
		int counter = 0;
		
		List<List<Integer>> foundedPaths = new LinkedList<>(); 
		Iterator<List<Integer>> it = paths.iterator();
		while ( it.hasNext() ) {
			list = (LinkedList<Integer>)it.next();
			if( list.peekLast() == idSender ) {
				succes = true;
				list2 = new LinkedList<Integer>(list);				///HEREE era clone cu cast - warning
				foundedPaths.add(list2);
				counter++;
				//break;
			}
		}
		
		//alegere lista cu cel mai mare timestamp 	
		if(counter > 1) {
			System.err.println("Mai multe cai la taskul " + taskId);
			list2 = findMaxTimestamp(foundedPaths, newMap, idExecutor, completed);
			System.err.println("dintre: " + foundedPaths + " s-a ales " + list2);
			if(taskId == 112) {
				System.err.println("a venit de la " + idSender + " la " + idReceiver + " find a lui " + idOwner + " fiind executat " + completed);
			}
		}
		//pentru a ramane si calea pana la nodul precedent in cazul in care acel nod inca pastreaza taskul
		//LinkedList<Integer> list2 = (LinkedList<Integer>)list.clone(); 
		if(succes) {
			list2.add(idReceiver);
			paths.add(list2);
		}else{
			System.err.println("Nu s-a gasit drumul pana la " + idSender);
			System.err.println("drumurile existente sunt " + paths);
		}
	}

	private LinkedList<Integer> findMaxTimestamp(List<List<Integer>> foundedPaths,
			Map<SenderAndReceiver, List<ValueAndTime>> newMap, int executorId, boolean completed) {
		// TODO Auto-generated method stub
		long maxTime = 0, time;
		LinkedList<Integer> list2 = null;
		List<ValueAndTime> auxList;
		
		List<List<Integer>> testList = new LinkedList<>();
		if(!completed) {		/*taskul inca nu este executat inseamna ca voi alege versiunea care a venit cel mai tarziu(pana in acel moment oricum nu se corup date)*/
			for(List<Integer> list : foundedPaths ) {
				try {
					if(list.size() == 1)	//in cazul in care ownerul inca a pastrat taskul si in memoria lui vom mai gasi calea cu un singur nod [owner]
						continue;
					auxList = newMap.get(new SenderAndReceiver(list.get(list.size()-2), list.get(list.size()-1)));
					if(auxList.size() != 0) {
						System.err.println("se va alege calea cu timestampul cel mai mare");
					}
					time = auxList.get(auxList.size()-1).getTimestamp(); //ultima e cea cu timestamp-ul cel mai mare
					if(time > maxTime) {
						System.err.println("de la " + list.get(list.size()-2) + " la " + list.get(list.size()-1) + " a venit la momentul " + time);
						maxTime = time;
						list2 = (LinkedList<Integer>)list;
					}
				}catch(IndexOutOfBoundsException ex) {
					ex.printStackTrace();
				}
			}
		}else {					/*inseamna ca task-ul este executat*/
			for(List<Integer> list : foundedPaths) {
				if(list.contains(executorId))
					testList.add(list);
			}
			if(testList.size() == 1) {
				list2 = (LinkedList<Integer>)testList.get(0);
				System.err.println("doar lista " + list2 + " contine executantul " + executorId);
			}
			
			if(testList.size() > 1) {		/*pot contine mai multe executantul pentru ca taskul a mai putut trece si pe la nodul executant fara ca el sa fie completat
			aici cred ca trebuie sa verific ultima care are un executant*/
				System.err.println("au fost maimulte rute ce contin executantu " + executorId + " PS: pe care o aleg? pe cea cu timestamp-ul mai mare(ultima din lista) " + testList);
				int length = testList.size(), indexExecuter;
				ValueAndTime vat = null;
				for(int i = 0; i < length; i++) {	
					list2 = (LinkedList<Integer>)testList.get(length-i-1);
					indexExecuter = list2.lastIndexOf(executorId);
					if ( indexExecuter != list2.size()-1 ) {
						List<ValueAndTime> vatList = newMap.get(new SenderAndReceiver(list2.get(indexExecuter), list2.get(indexExecuter+1)));
						if(vatList == null) {
							System.err.println("Nu ar trebui niciodata!!");
							continue;
						}
						if(vatList.size() != 1)
							System.err.println("chiar si asa!");
						vat = vatList.get(vatList.size()-1);
						if(vat.executorNodeId == executorId) {
							System.err.println("asa trebuie");
							break;
						}
						if(vat.executorNodeId != -1) {
							System.err.println("nu ar trebui");
							break;					/*in ordine descrescatoare de timp prima cale pe care o intalnim cu executant, de gandit!!*/
							
						}
					}else {
						System.err.println("Avem o cale care se termina cu executantul " + executorId + list2);
						break;
					}
						
				}
			}
			if(testList.size() == 0) {
				System.err.println("NUUUU: nicio lista " + foundedPaths + " nu contine executantu " + executorId + "- imposibil");
				list2  = (LinkedList<Integer>)foundedPaths.get(foundedPaths.size()-1);
			}
		}
		
		if(list2 == null)
			System.err.println("NEVEREVER");
		return list2;
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
		boolean verifyCorruption = verifyHashCollisions(task.taskData, task.map.get(encounteredId), task, correctOneBit);
		if( !verifyCorruption || (verifyCorruption && correctOneBit.value == true) ) {
			correctOneBit.setValue(false);
			numberOfCorruptedTasks++;
			System.out.println("corrupted data from node " + encounteredId + " executed by " + task.mapNodeIdExecutorId.get(encounteredId));
		}
		displayTaskHistory(task,numberOfCorruptedTasks==1,taskHistory.get(task.id));
		
		task.listOfNodesMeetByOwner.clear();
		ownTasks.finishTask(task);
		otherTasks.remove(task);	
	}

	private void verifyWaitingTasks(Task task, List<Map<Integer, Data>> list) {
		// TODO Auto-generated method stub
		//System.out.println("inainte!!" + dropComputing.waitingTasks.get(task.id));
		this.majority = calculatingMajority(waitingTasks.get(task.id));
		System.err.println("S-a gasit majoritatea " + majority + " pt taskul " + task.id);
		//se mai asteapta o versiune doar pentru taskul respectiv pentru a avea o majoritate
		if( this.majority == 50.0 ){
			//setare numarul de waitingTask doar pentru acest task 
			task.nrWaitingVersions++;
			System.err.println("Caz special");
			return ;
		}
		
		if(this.majority < 50) {
			System.err.println("se poate sunt corupte altfel");
		}
			
		System.out.println("task " + task.id + " has enough versions at node " + this.id + " -- " + waitingTasks.get(task.id) );
		
		/*Iterator<Task> it = ownTasks.getTasks().iterator();
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
		}*/
		//dar nu se modifica task.data, coruperea se face in task.map, deci nu trebuie facuta asta, in loc de currentData pot sa folosesc task.data nu?
		int currentData = task.data;
		 
		
		int counter = -1, numberOfCorruptedTask = 0;
		for(Data data : waitingTasks.get(task.id) ) {
			counter++;
			if( !verifyHashCollisions(task.taskData, data, task, correctOneBit) ){
				/*aici se salveaza doar cele care au ajuns corupte, chiar si dupa incercarea de corectie cu hamming*/
				numberOfCorruptedTask++;
				/*int executorNodeId = mapTaskIdExecutorId.get(task.id).get(counter);
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
				}*/
			}
		}
		
		if(numberOfCorruptedTask > 0)
			System.err.println("Taskul " + task.id + " a fost primit cu " + numberOfCorruptedTask + " versiuni corupte");
		else {
			System.err.println("Taskul " + task.id + " nu a fost primit corupt");
		}
		boolean acceptCorruptedVersion = false;
		if(majority > 50.0 && 2*numberOfCorruptedTask > numberOfWaitingTasks)
			acceptCorruptedVersion = true;
			
		displayTaskHistory(task,acceptCorruptedVersion, list);
		
		task.listOfNodesMeetByOwner.clear();
		ownTasks.finishTask(task);
		//otherTasks.remove(task);	//adaugat aici
		
		waitingTasks.remove(task.id);
		
	}

	private boolean verifyRating(DropComputing dropComputing) {
		// TODO Auto-generated method stub
		
		if(dropComputing == null)
			return true;
	//	System.out.println(dropComputing.rating);
		
		dropComputing.timerRating--;
		
		if(dropComputing.rating >= 50 && dropComputing.timerRating <= 0 && dropComputing.rating < 100)
			dropComputing.rating += 20;
		
		
		
		return dropComputing.rating >= 75.0;
	}
	
	private boolean verifyRating(int dcNodeId) {
		// TODO Auto-generated method stub
		
		if(nodesRating.containsKey(dcNodeId)) {
			//System.err.println("Intra aici!!");
			nodesTimerRating.put(dcNodeId, nodesTimerRating.get(dcNodeId) - 1);
			if(nodesRating.get(dcNodeId) >= 50 && nodesTimerRating.get(dcNodeId) <= 0 && nodesRating.get(dcNodeId) < 100) {
				nodesRating.put(dcNodeId, Math.min(100.0, nodesRating.get(dcNodeId)+20));
				nodesTimerRating.put(dcNodeId, timerRatingValue*1.0); //timer value
				//System.err.println("nu prea il creste");
			}else {
				//System.err.println("nu il creste pt ca ratingul e " + nodesRating.get(dcNodeId) + " si timerul " +  nodesTimerRating.get(dcNodeId));
			}
			return nodesRating.get(dcNodeId) >= 75.0;
			
		}
		
		//return socialNetwork[dcNodeId];
		return true;
	}

	private void displayTaskHistory(Task task, boolean corruptedVersion, List<Map<Integer, Data>> list2) {
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
		//saveTaskVersions(task.map, task.id, corruptedVersion);
		
		if(list2.size() != numberOfWaitingTasks)
			System.err.println("Imposible");
		for( Map<Integer,Data> map : list2) {
			saveTaskVersions(map, task.id, corruptedVersion);
		}
		
		
		
		System.out.println(" ");
	}

	private void saveTaskVersions(Map<Integer, Data> map, int taskId, boolean corruptedVersion) {
		// TODO Auto-generated method stub
		
		DropComputingStats.counter++;
		
		int corruptedVersions = 0, uncorruptedVersions = 0;
		for( Data version : map.values() ){
			if(version.simpleData == 0)
				corruptedVersions++;
			else if(version.simpleData == 1)
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
				
			if( !verifyHashCollisions(task.taskData, task.map.get(idNode), task, correctOneBit) ){
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
			}
		}
		
	}

	private boolean verifyTaskExecutors(List<Integer> list, int executorID) {
		// TODO Auto-generated method stub
		
		if(list == null)
			return true;
		
		//return !list.contains(executorID);
		int numberOfOccurrences = Collections.frequency(list, executorID);

		// strict < pentru ca se apeleaza inainte de a se  adauga in lista un nou task executat de executorID 
		return numberOfOccurrences < Math.floor(numberOfWaitingTasks/(1 + 1.0*maxPercentageOfTaskExecutors/100));
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
				if(!first.completedTasks.contains(task)) {	//sa nu mai faca schimb de taskuri completate
					temp1.add(task);
					node = false;
				}
			} else {
				task = tasksSecond.get(tasksSecond.size() - 1);
				if(!second.completedTasks.contains(task)) {
					temp2.add(task);
					node = true;
				}
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
				
				//se sterg din otherTasks nodurile executate
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
						task.map.put(this.id, new Data(0));
					}
				}
				if(!verifyHashCollisions(task.taskData, task.map.get(this.id), task, correctOneBit)){
					DropComputingStats.corruptedTasks++;
				}
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
			if(!verifyHashCollisions(task.taskData, task.map.get(idNode), task, correctOneBit)){
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
		private int executorNodeId;
		private int currentNodeId;
		private DropComputing executorNode;
		private DropComputing currentNode;
		private int[] hammingCode;
		private Data data;
		
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
			this.executorNodeId = -1;
			this.currentNodeId = -1;
		}
		public ValueAndTime(Data data, long currentTime) {
			super();
			this.data = data;
			this.timestamp = currentTime;
			this.executorNode = null;
			this.currentNode = null;
			this.executorNodeId = -1;
			this.currentNodeId = -1;
		}
		public ValueAndTime(int value, long timestamp, int executorNodeId, int currentNodeId) {
			super();
			this.value = value;
			this.timestamp = timestamp;
			this.executorNodeId = executorNodeId;
			this.currentNodeId = currentNodeId;
		}
		public ValueAndTime(int[] hammingCode, long timestamp, int executorNodeId, int currentNodeId) {
			super();
			this.hammingCode = hammingCode;
			this.timestamp = timestamp;
			this.executorNodeId = executorNodeId;
			this.currentNodeId = currentNodeId;
		}
		public ValueAndTime(int value, long timestamp, DropComputing executorNode, DropComputing currentNode) {
			super();
			this.value = value;
			this.timestamp = timestamp;
			this.executorNode = executorNode;
			this.currentNode = currentNode;
		}
		public ValueAndTime(int[] hammingCode, long timestamp, DropComputing executorNode, DropComputing currentNode) {
			super();
			this.hammingCode = hammingCode;
			this.timestamp = timestamp;
			this.executorNode = executorNode;
			this.currentNode = currentNode;
		}
		public ValueAndTime(Data data, long timestamp, DropComputing executorNode, DropComputing currentNode) {
			super();
			this.data = data;
			this.timestamp = timestamp;
			this.executorNode = executorNode;
			this.currentNode = currentNode;
		}
		public ValueAndTime(Data data, long timestamp, int executorNodeId, int currentNodeId) {
			super();
			this.data = data;
			this.timestamp = timestamp;
			this.executorNodeId = executorNodeId;
			this.currentNodeId = currentNodeId;
		}
		@Override
		public String toString() {
			// TODO Auto-generated method stub
			return "Valoare: " + value + " timestamp: " + timestamp;
		}
	}
	private class MutableBoolean{
		private boolean value;

		public boolean isValue() {
			return value;
		}

		public void setValue(boolean value) {
			this.value = value;
		}

		public MutableBoolean(boolean value) {
			super();
			this.value = value;
		}
		
		
	}
	
	private class Data{
		public int simpleData = -1;
		public int[] hammingData = null;
		public Data(int simpleData) {
			super();
			this.simpleData = simpleData;
		}
		public Data(int[] hammingData) {
			super();
			this.hammingData = hammingData;
		}
		@Override
		public String toString() {
			if(simpleData == -1)
				return Arrays.toString(hammingData);
			return String.valueOf(simpleData);
		}
		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + Arrays.hashCode(hammingData);
			result = prime * result + simpleData;
			return result;
		}
		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			Data other = (Data) obj;
			if(other.simpleData == simpleData && hammingData == null)
				return true;
			if(Arrays.equals(hammingData, other.hammingData) && simpleData == -1)
				return true;
			return false;
		}
		
		
	}
	
	
	
	private class Task {

		public int nrWaitingVersions;
		public int nrReceivedVersions;
		/*
		 * data of the current task, 0 means it was corrupted (1 means
		 * unmodified)
		 */
		private int data = 1;
		private Data taskData;
		
		private int[] data2;
		private int[] extendedData;
		private int dataSize;
		private int parityBits;
		public int getParityBits() {
			return parityBits;
		}

		public void setParityBits(int parityBits) {
			this.parityBits = parityBits;
		}

		private Map<Integer, int[]> mapHamming;
		
		private boolean solvedByOwner;
		private int solverBy;
		private List<Integer> listOfSolvers;
		private List<Integer> listOfPartialSolvers;
		//private List<Integer> listOfEncounteredSolvers;
		private Set<Integer> listOfVisitedNodes;
		private List<Integer> listOfNodesMeetByOwner;
		
		private Map<Integer,Integer> mapNodeIdExecutorId;
		private Map<Integer, Data> map;
		
		private Map<SenderAndReceiver, List<ValueAndTime>> newMap;
		//private Map<SenderAndReceiver, List<>>
		//toate drumurile pana unde este transmis taskul
		private List<List<Integer>> paths;
		
		//toate drumurile complete pana ajunge taskul executat din nou la owner
		private List<List<Integer>> listOfCompletedPaths;

		
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
			
			this.map = new HashMap<Integer, Data>();
			if (useHammingCode) {
				if (type == TaskType.SMALL)
					this.dataSize = 8;
				if (type == TaskType.MEDIUM)
					this.dataSize = 8;
				if (type == TaskType.LARGE)
					this.dataSize = 16;

				this.data2 = new int[this.dataSize];
				for (int i = 0; i < this.dataSize; i++) {
					data2[i] = (byte) RAND.nextInt(2);
				}
				//this.taskData = new Data(this.data2);
				
				//de lucrat cu extended data
				generateCode(this.data2);
				
				this.taskData = new Data(this.extendedData);
				
				this.map.put(this.ownerID, this.taskData);
				if (this.id == 883 || this.id == 1234) {
					System.err.println("ALOOO" + ownerID);
					printArray(data2);
				}
			}else {
				this.taskData = new Data(1);
				this.map.put(this.ownerID, this.taskData); // map ul este intre id-ul nodurilor
				// si valoarea
			}
				
			this.newMap = new HashMap<SenderAndReceiver, List<ValueAndTime>>();
			this.nrWaitingVersions = numberOfWaitingTasks;
			this.nrReceivedVersions = 0;
			
			this.paths = new LinkedList<>();
			List<Integer> list = new LinkedList<Integer>();
			list.add(ownerID);
			this.paths.add(list);
			
			this.listOfCompletedPaths = new LinkedList<>();
			
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

		private void printArray(int[] data22) {
			// TODO Auto-generated method stub
			System.err.print("[");
			for(int i = 0; i < data22.length; i++) {
				System.err.print(data22[i] + ",");
			}
			System.err.println("]");
		}

		public void generateCode(int a[]) {
			
			int i = 0, j = 0, k = 0;
			int checkValue = 4;
			for (int a_i = 3; a_i < a.length; a_i++) {
				this.parityBits = a_i;
				checkValue *= 2;
				if ( checkValue > a.length + this.parityBits + 1)
					break;
			}
			this.extendedData = new int[a.length + this.parityBits];
			for (i = 0; i < this.extendedData.length; i++) {
				if (isPowerOfTwo(i + 1)) {
					continue;
				}
				this.extendedData[i] = a[k++];
			}

			/*System.out.println("afisare b din memorie");
			for (int b_i = 0; b_i < b.length; b_i++)
				System.out.print(b[b_i]);
			System.out.println();*/

			for (int power = 0; power < parityBits; power++)
				for (j = 0; j < this.extendedData.length; j++) {
					if (isPowerOfTwo(j + 1))
						continue;
					char[] bits = convertAndPadding(j + 1, parityBits);
					if (bits[bits.length - power - 1] == '1')
						this.extendedData[(int) Math.pow(2, power) - 1] ^= this.extendedData[j];
				}

			/*System.out.println("afisare b din memorie");
			for (int b_i = 0; b_i < b.length; b_i++)
				System.out.print(b[b_i]);
			System.out.println();
*/
			//return b;
		}

		private char[] convertAndPadding(int i, int parity_count) {
			// TODO Auto-generated method stub
			char[] bits = Integer.toBinaryString(i).toCharArray();

			if (bits.length < parity_count)
				bits = padLeft(Integer.valueOf(Integer.toBinaryString(i)), parity_count).toCharArray();

			return bits;
		}

		private boolean isPowerOfTwo(int i) {
			// TODO Auto-generated method stub
			return i > 0 && ((i & (i - 1)) == 0);
		}

		private String padLeft(int s, int n) {
			return String.format("%1$0" + n + "d", s);
		}
		
		public int[] receive(int a[], int parityBits, MutableBoolean correctOneBit) {
			int parity[] = new int[parityBits];

			StringBuilder errorLocation = new StringBuilder();

			for (int power = 0; power < parityBits; power++) {
				for (int i = 0; i < a.length; i++) {

					char[] bits = convertAndPadding(i + 1, parityBits);

					if (bits[bits.length - power - 1] == '1')
						parity[power] ^= a[i];
				}
				errorLocation.append(parity[power]);

			}

			int error_location = Integer.parseInt(errorLocation.reverse().toString(), 2);
			
			if (error_location != 0 && error_location <= a.length) {
				//System.err.println("Error is at location " + error_location + " pentru taskul " + id);
				a[error_location - 1] ^= 1;
				correctOneBit.setValue(true);
				
			} else {
				//System.err.println("There is no error in the received data.");
			}

			// Finally, we shall extract the original data from the received (and
			// corrected) code:
			int[] original = new int[a.length - parityBits];
			int original_i = 0;
			//System.out.println("Original data sent was:");
			for (int i = a.length - 1; i > 0; i--) {
				if (isPowerOfTwo(i+1))
					continue;
				//System.out.print(a[i]);
				original[original_i++] = a[i];
			}
			
			//AICI
			int[] dest = new int[a.length];
			System.arraycopy(a, 0, dest, 0, dest.length);
			return dest;
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
		public static int nrOfTasksHammingCorrection = 0;
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
