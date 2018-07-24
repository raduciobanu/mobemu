/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package mobemu.algorithms;

import java.util.*;
import mobemu.node.Battery;
import mobemu.node.Context;
import mobemu.node.Node;

/**
 * Class for a Drop Computing node.
 *
 * Radu-Ioan Ciobanu, Catalin Negru, Florin Pop, Ciprian Dobre, Constandinos X.
 * Mavromoustakis, and George Mastorakis. "Drop computing: Ad-hoc dynamic
 * collaborative computing." Future Generation Computer Systems (2017).
 * 
 * and
 * 
 * TODO: Add NBiS paper when published (TTOff).
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
	 * Mobile broadband transfer speed (in MB/s).
	 */
	private static final int MBB_SPEED = 3;
	/**
	 * Edge (Wi-Fi) transfer speed (in MB/s).
	 */
	private static final int EDGE_SPEED = 10; // caused by Flash write speed
	/**
	 * Specifies if this node is a static access point (edge device) or a mobile
	 * device.
	 */
	boolean isWiFiAccessPoint;

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
	 *            specifies whether a node employs other nodes for task computation
	 *            or not
	 * @param useCloud
	 *            specifies whether a node employs other nodes for task computation
	 *            or not
	 */
	public DropComputing(int id, int nodes, Context context, boolean[] socialNetwork, int dataMemorySize,
			int exchangeHistorySize, long seed, long traceStart, long traceEnd, DeviceType deviceType, long sampleTime,
			boolean opportunistic, boolean useCloud, boolean isWiFiAccessPoint) {
		super(id, nodes, context, socialNetwork, dataMemorySize, exchangeHistorySize, seed, traceStart, traceEnd);

		this.opportunistic = opportunistic;
		this.useCloud = useCloud;
		this.sampleTime = sampleTime;
		this.isWiFiAccessPoint = isWiFiAccessPoint;

		deviceInfo = DEVICE_FACTORY.getDevice(deviceType);
		ownTasks = new TaskGroup(id);
		cloudTasks = new TaskGroup(id);
		otherTasks = new TreeSet<>(new TaskComparator(id));
		completedTasks = new ArrayList<>();

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

	@Override
	protected void onDataExchange(Node encounteredNode, long contactDuration, long currentTime) {
		if (!(encounteredNode instanceof DropComputing) || !opportunistic) {
			return;
		}

		DropComputing dcNode = (DropComputing) encounteredNode;
		boolean exchangedData = false;
		int[] exchangedTasksCount = { 0, 0, 0 };

		// condition for node closeness
		boolean condition = (encounteredNodes.get(dcNode.id) != null
				&& encounteredNodes.get(dcNode.id).getContacts() >= 2);

		if ((battery.canParticipate() || isWiFiAccessPoint)
				&& (dcNode.battery.canParticipate() || dcNode.isWiFiAccessPoint)) {
			// get all data regarding solved tasks from other node
			List<Task> toRemove = new ArrayList<>();

			for (Task task : dcNode.completedTasks) {
				// the current node gets a message that one of its tasks has finished
				if (task.ownerID == id && ownTasks.finishTask(task)) {

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
					setBatteryOpportunisticExchange(this, dcNode, task.type, sampleTime, currentTime);
				} else if ((!completedTasks.contains(task) && condition) || isWiFiAccessPoint) {
					// get completed tasks info to disseminate it further
					if (completedTasks.size() == 5000000) {
						completedTasks.remove(0);
					}
					completedTasks.add(task);
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
					setBatteryOpportunisticExchange(this, dcNode, task.type, sampleTime, currentTime);
				}
			}

			// compute transfer duration based on task types
			long transferDuration = TaskType.getTransferDuration(TaskType.SMALL, dcNode.isWiFiAccessPoint ? 2 : 1,
					dcNode, currentTime) * exchangedTasksCount[0]
					+ TaskType.getTransferDuration(TaskType.MEDIUM, dcNode.isWiFiAccessPoint ? 2 : 1, dcNode,
							currentTime) * exchangedTasksCount[1]
					+ TaskType.getTransferDuration(TaskType.LARGE, dcNode.isWiFiAccessPoint ? 2 : 1, dcNode,
							currentTime) * exchangedTasksCount[2];

			// clear the current task group if all tasks have finished
			if (exchangedData && ownTasks.hasFinished()) {
				ownTasks.finish(currentTime + transferDuration);

				// compute statistics (increase the number of completed task groups and the
				// total computation duration for all task groups that have completed)
				DropComputingStats.taskGroupsCompleted++;
				DropComputingStats.totalComputationDuration += ownTasks.getCompletionDuration();

				ownTasks.clear();
			}

			// remove all completion messages from encountered node, so they won't be
			// disseminated more
			dcNode.completedTasks.removeAll(toRemove);

			// if the two nodes are not balanced, balance their tasks
			if (!isWiFiAccessPoint && !dcNode.isWiFiAccessPoint && !isBalanced(this, dcNode)) {
				List<Task> currentTasks = new ArrayList<>();
				List<Task> otherNodeTasks = new ArrayList<>();

				currentTasks.addAll(otherTasks);
				currentTasks.addAll(dcNode.otherTasks);

				balance(this, dcNode, currentTasks, otherNodeTasks);

				// if at least one task has been exchanged between the two
				// devices, increase battery decrease rate
				for (Task task : otherTasks) {
					if (!currentTasks.contains(task)) {
						setBatteryOpportunisticExchange(this, dcNode, task.type, sampleTime, currentTime);
					}
				}

				for (Task task : dcNode.otherTasks) {
					if (!otherNodeTasks.contains(task)) {
						setBatteryOpportunisticExchange(dcNode, this, task.type, sampleTime, currentTime);
					}
				}

				// remove transferred tasks if the nodes are sufficiently familiar
				if (condition) {
					otherTasks.clear();
				}
				otherTasks.addAll(currentTasks);

				if (condition) {
					dcNode.otherTasks.clear();
				}
				dcNode.otherTasks.addAll(otherNodeTasks);
			} else if (dcNode.isWiFiAccessPoint) {
				// based on contact duration, decide how many tasks I
				// can have the edge node compute for me

				long totalTime = contactDuration;

				while (totalTime > 0) {
					// if the current node is already computing a task, continue to run it;
					// if the task finished, remove it from the list of tasks
					if (!otherTasks.isEmpty()) {
						Task task = otherTasks.first();

						if (task.execute(deviceInfo, sampleTime, id)) {
							otherTasks.remove(task);

							setBatteryOpportunisticExchange(dcNode, this, task.type, sampleTime, currentTime);

							if (task.ownerID == id) {
								// if this was a local task, check if the local task group has finished
								if (ownTasks.hasFinished()) {
									ownTasks.finish(currentTime);

									// compute statistics (increase the number of completed task groups and the
									// total computation duration for all task groups that have completed)
									DropComputingStats.taskGroupsCompleted++;
									DropComputingStats.totalComputationDuration += ownTasks.getCompletionDuration();

									ownTasks.clear();
								}
							} else {
								// if it was a non-local task, add it to the dissemination memory
								completedTasks.add(task);
								dcNode.completedTasks.add(task);
							}
						}
					} else {
						break;
					}

					totalTime -= sampleTime;
				}
			}
		}
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
	 * @param timestamp
	 *            current trace time
	 */
	private static void setBatteryOpportunisticExchange(DropComputing first, DropComputing second, TaskType type,
			long sampleTime, long timestamp) {
		if (!first.isWiFiAccessPoint) {
			first.battery
					.setDecreaseRate(first.battery.getDecreaseRate() + first.deviceInfo.getTransferBatteryMultiplier(
							second.isWiFiAccessPoint ? 2 : 1, type, sampleTime, timestamp, first));
		}
		if (!second.isWiFiAccessPoint) {
			second.battery
					.setDecreaseRate(second.battery.getDecreaseRate() + second.deviceInfo.getTransferBatteryMultiplier(
							first.isWiFiAccessPoint ? 2 : 1, type, sampleTime, timestamp, second));
		}
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

		// compute status (increase the maximum number of messages stored per node)
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
		if (!ownTasks.hasTasks() && !cloudTasks.hasTasks() && RAND.nextDouble() >= 0.999 && !isWiFiAccessPoint) {
			SortedSet<Task> newTasks = generateTasks(currentTime);
			ownTasks.addTasks(newTasks);
			otherTasks.addAll(newTasks);
		}

		double batteryDecreaseRate = 0;

		// if the current node is already computing a task, continue to run it;
		// if the task finished, remove it from the list of tasks
		if (battery.canParticipate() && !otherTasks.isEmpty()) {
			Task task = otherTasks.first();

			// compute battery decrease rate based on the type of task
			batteryDecreaseRate = deviceInfo.taskBatteryMultiplier;

			if (task.execute(deviceInfo, sampleTime, id)) {
				otherTasks.remove(task);

				if (task.ownerID == id) {
					// if this was a local task, check if the local task group has finished
					if (ownTasks.hasFinished()) {
						ownTasks.finish(currentTime);

						// compute statistics (increase the number of completed task groups and the
						// total computation duration for all task groups that have completed)
						DropComputingStats.taskGroupsCompleted++;
						DropComputingStats.totalComputationDuration += ownTasks.getCompletionDuration();

						ownTasks.clear();
					}
				} else {
					// if it was a non-local task, add it to the dissemination memory
					completedTasks.add(task);
				}
			}
		}

		// case for Cloud usage
		if (useCloud && DropComputingCloud.hasFinished(cloudTasks)) {
			// increase battery decrease rate for cloud download
			batteryDecreaseRate += cloudTasks.computeCloudTransferBatteryMultiplier(deviceInfo, sampleTime);

			DropComputingCloud.downloadResult(cloudTasks);
			cloudTasks.finish(currentTime + cloudTasks.getTransferDuration());

			// compute statistics (increase the number of completed task groups and the
			// total computation duration for all task groups that have completed)
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

			// compute statistics (increase the number of task group expirations)
			DropComputingStats.taskGroupExpirations++;
		}

		// set decrease rate if it is not the default value
		if (batteryDecreaseRate > 0) {
			battery.setDecreaseRate(battery.getDecreaseRate() + batteryDecreaseRate);
		}
	}

	/**
	 * Generates a set of tasks at the current node (at least one small task and a
	 * maximum of 5, a maximum of 4 medium tasks, and a maximum of 4 large tasks).
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

		// compute statistics (increase the number of tasks and task groups created)
		DropComputingStats.taskGroups++;
		DropComputingStats.tasks[0] += smallTasksCount;
		DropComputingStats.tasks[1] += mediumTasksCount;
		DropComputingStats.tasks[2] += largeTasksCount;

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
		 * @return {@code true} if the task group was fully executed, {@code false}
		 *         otherwise
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
		 * @return {@code true} if the task had finished, {@code false} otherwise
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
		 * Battery multiplier when transferring data through MBB.
		 */
		protected double mbbBatteryMultiplier = 5.74;
		/**
		 * Battery multiplier when transferring data to the edge device (Wi-Fi).
		 */
		protected double edgeBatteryMultiplier = 2.87;
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
		 * Computes the battery multiplier using this device for a task transfer.
		 *
		 * @param type
		 *            0 if the transfer is done using WiFi, 1 if Bluetooth is employed,
		 *            2 if edge is used
		 * @param taskType
		 *            type of task
		 * @param sampleTime
		 *            sample time of the trace
		 * @return the battery multiplier of this device when performing a transfer
		 */
		public double getTransferBatteryMultiplier(int type, TaskType taskType, long sampleTime) {
			return TaskType.getTransferDuration(taskType, type) * (type == 0 ? mbbBatteryMultiplier
					: type == 1 ? bluetoothBatteryMultiplier : edgeBatteryMultiplier) / sampleTime;
		}

		/**
		 * Computes the battery multiplier using this device for a task transfer.
		 *
		 * @param type
		 *            0 if the transfer is done using WiFi, 1 if Bluetooth is employed,
		 *            2 if edge is used
		 * @param taskType
		 *            type of task
		 * @param sampleTime
		 *            sample time of the trace
		 * @param timestamp
		 *            current trace time
		 * @param node
		 *            current node
		 * @return the battery multiplier of this device when performing a transfer
		 */
		public double getTransferBatteryMultiplier(int type, TaskType taskType, long sampleTime, long timestamp,
				DropComputing node) {
			return TaskType.getTransferDuration(taskType, type, node, timestamp) * (type == 0 ? mbbBatteryMultiplier
					: type == 1 ? bluetoothBatteryMultiplier : edgeBatteryMultiplier) / sampleTime;
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
		 * @return {@code true} if the task group has tasks, {@code false} otherwise
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
		 * @return the duration it took for this task group to finish, or -1 if the task
		 *         hasn't completed
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
		 * @return {@code true} if the task group has finished, {@code false} otherwise
		 */
		public boolean execute(DeviceInfo device, long sampleTime, int executorID) {
			int finishedTasks = 0;

			for (Task task : tasks) {
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

		/**
		 * Decreases this task group's upload duration.
		 *
		 * @param sampleTime
		 *            sample time of the trace
		 * @return {@code true} if the task has finished uploading, {@code false}
		 *         otherwise
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
		 * Computes the battery multiplier for transferring this task group to the
		 * cloud.
		 *
		 * @param device
		 *            device that uploads the task group
		 * @param sampleTime
		 *            sample time of the trace
		 * @return the battery multiplier for transferring this task group to the cloud
		 */
		private double computeCloudTransferBatteryMultiplier(DeviceInfo device, long sampleTime) {
			double multiplier = 0;

			for (Task task : tasks) {
				multiplier += device.getTransferBatteryMultiplier(0, task.type, sampleTime);
			}

			return multiplier;
		}

		/**
		 * Checks whether all tasks from this task group have finished.
		 *
		 * @return {@code true} if all tasks have finished, {@code false} otherwise
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
		 * Checks whether all tasks from this task group have finished at a different
		 * node than the current one.
		 *
		 * @return {@code true} if all tasks have finished, {@code false} otherwise
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
		 * Gets the duration (in ms) that it takes for a type of task to be transferred
		 * through Bluetooth, Wi-Fi, or MBB. We assume tasks of 5 MB.
		 *
		 * @param type
		 *            task type
		 * @param exchangeType
		 *            0 if the task is uploaded using mobile broadband, 1 if Bluetooth
		 *            is employed, 2 if edge is used
		 * @return duration (in ms) that it takes to upload transfer this type of task
		 */
		public static long getTransferDuration(TaskType type, int exchangeType, DropComputing node, long timestamp) {
			double transferSizeMB = 5;
			transferSizeMB /= getScalingFactor(type);

			double duration = transferSizeMB;
			duration /= exchangeType == 0 ? MBB_SPEED
					: exchangeType == 1 ? BT_SPEED
							: Math.max(EDGE_SPEED, getMaxSpeedBySignalLevel(node.wifiMetrics.getRssi(timestamp)));
			// transform to ms
			duration *= 1000;

			return (long) Math.ceil(duration);
		}

		/**
		 * Gets the duration (in ms) that it takes for a type of task to be transferred
		 * towards or from the Cloud. We assume tasks of 5 MB.
		 *
		 * @param type
		 *            task type
		 * @param exchangeType
		 *            0 if the task is uploaded using mobile broadband, 1 if Bluetooth
		 *            is employed, 2 if edge is used
		 * @return duration (in ms) that it takes to upload transfer this type of task
		 */
		public static long getTransferDuration(TaskType type, int exchangeType) {
			double transferSizeMB = 5;
			transferSizeMB /= getScalingFactor(type);

			double duration = transferSizeMB;
			duration /= exchangeType == 0 ? MBB_SPEED : exchangeType == 1 ? BT_SPEED : EDGE_SPEED;
			// transform to ms
			duration *= 1000;

			return (long) Math.ceil(duration);
		}

		/**
		 * Computes the maximum Wi-Fi data transfer speed based on signal level (taken
		 * from https://bit.ly/2uNDikQ).
		 *
		 * @param signalLevel
		 *            signal level
		 * @return data rate
		 */
		private static int getMaxSpeedBySignalLevel(int signalLevel) {

			if (signalLevel < -81) {
				return 6;
			} else if (signalLevel < -80) {
				return 9;
			} else if (signalLevel < -78) {
				return 12;
			} else if (signalLevel < -76) {
				return 18;
			} else if (signalLevel < -73) {
				return 24;
			} else if (signalLevel < -69) {
				return 36;
			} else if (signalLevel < -65) {
				return 48;
			} else {
				return 54;
			}
		}

		/**
		 * Gets the scaling factor for a certain type of task.
		 *
		 * @param type
		 *            task type
		 * @return scaling factor for the given task type
		 */
		private static int getScalingFactor(TaskType type) {
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
	}

	/**
	 * Number of generated tasks.
	 */
	static int numberOfTasks = 0;

	/**
	 * Class used for representing a task.
	 */
	private class Task {

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
		}

		/**
		 * Executes a task on a tick.
		 *
		 * @param device
		 *            type of device executing this task
		 * @param sampleTime
		 *            sample time of the trace
		 * @param executorID
		 *            the ID of the node executing this task
		 * @return {@code true} if the task has finished, {@code false} otherwise
		 */
		public boolean execute(DeviceInfo device, long sampleTime, int executorID) {
			this.executorID = executorID;

			// if this is the start of running this task, set initial remaining time
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
		 * @return {@code true} if the task has finished running, {@code false} if it
		 *         hasn't started, or if it has started but hasn't finished
		 */
		public boolean hasFinished() {
			return remainingTime <= 0 && remainingTime != Double.MIN_VALUE && executorID == ownerID;
		}

		/**
		 * Checks whether the task has finished running at a different node.
		 *
		 * @return {@code true} if the task has finished running, {@code false} if it
		 *         hasn't started, or if it has started but hasn't finished
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
			return TaskType.getTransferDuration(type, 0);
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
		 * Total uptime (i.e., the uptime for each mobiel device in the simulation).
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
