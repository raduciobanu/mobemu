/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package mobemu;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.*;

import mobemu.algorithms.DropComputing;
import mobemu.algorithms.DropComputing.DeviceType;
import mobemu.algorithms.DropComputing.DropComputingStats;
import mobemu.algorithms.Epidemic;
import mobemu.node.Context;
import mobemu.node.Message;
import mobemu.node.Node;
import mobemu.node.Stats;
import mobemu.parsers.HCMM;
import mobemu.parsers.UPB;
import mobemu.trace.Parser;

/**
 * Main class for MobEmu.
 *
 * @author Radu
 */
public class MobEmu {

    public static void main(String[] args) {
       //Parser parser = new UPB(UPB.UpbTrace.UPB2011);
    	try {
			System.setOut(new PrintStream(new FileOutputStream("output.txt")));
			System.setErr(new PrintStream(new FileOutputStream("errors.txt")));
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    	
    	Parser parser = new HCMM(30, 6 * 3600, 30 * 24 * 3600, 1.25f, 1.50f, 0.1f, 1000f, 1000f, 100, 100, 10.0, 0.7, 5, 5, 1.25f, 0.8f, 0,false,0);
    	//se parseaza fisierul social.dat si upb2011.dat din traces/upb2011 si se adauga un contact in lista de contacte care e retinuta in clasa Trace
        //creandu se un trace la inceputul parsarii in metoda de parsare din clasa UPB, la 2011 nr. de device-uri e hardcodat la 22
        // print some trace statistics
        double duration = (double) (parser.getTraceData().getEndTime() - parser.getTraceData().getStartTime()) / (Parser.MILLIS_PER_MINUTE * 60);
       

        // initialize Epidemic nodes
        long seed = 0;
        boolean dissemination = false;
        Node[] nodes = new Node[parser.getNodesNumber()];
        
        if(args.length != 7) {
        	System.out.println("Please insert 7 arguments");
        	return ;
        }
        int useWaitingTasks = Integer.valueOf(args[0]);
        int nrWaitingTasks = Integer.valueOf(args[1]);
        int useRating = Integer.valueOf(args[2]);
        int percentOfExecutors = Integer.valueOf(args[3]);
        int useHamming = Integer.valueOf(args[4]);
        int corruptCompletedTasksFromEncounteredNodes = Integer.valueOf(args[5]);
        int corruptTasksAtExecutors = Integer.valueOf(args[6]);
        
        for (int i = 0; i < nodes.length; i++) {
        	/*public DropComputing(int id, int nodes, Context context, boolean[] socialNetwork, int dataMemorySize,
                    int exchangeHistorySize, long seed, long traceStart, long traceEnd, DeviceType deviceType,
                    long sampleTime, boolean opportunistic, boolean useCloud) */
        	
        	nodes[i] = new DropComputing(corruptTasksAtExecutors == 1 ? true:false, corruptCompletedTasksFromEncounteredNodes == 1 ? true:false, useWaitingTasks == 1 ? true:false, nrWaitingTasks, useRating == 1 ? true:false, percentOfExecutors, useHamming == 1 ? true:false, i, nodes.length, 
        			parser.getContextData().get(i),  parser.getSocialNetwork()[i], 10000, 100, seed, parser.getTraceData().getStartTime(), parser.getTraceData().getEndTime(),
        			DropComputing.DeviceType.IPHONE_6S, parser.getTraceData().getSampleTime(), true, false);
        }

        // run the trace
        //false  = consumul bateriei nu se ia in considerare, true otherwise
        List<Message> messages = Node.runTrace(nodes, parser.getTraceData(), false, dissemination, seed);

        //  System.out.println(messages);
        // print opportunistic algorithm statistics
        
        try {
			System.setOut(new PrintStream(new FileOutputStream("statistics.txt")));
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
        if(useRating == 1)
			System.out.println("Se tine cont de ratingul nodului");
		if(percentOfExecutors != 0 && useWaitingTasks == 1) {
			double percent = (1.0*percentOfExecutors/100)*nrWaitingTasks;
			System.out.println("Acelasi task poate fi executat de maxim " + Math.floor(nrWaitingTasks/(1 + 1.0*percentOfExecutors/100)) + "ori de acelasi nod din totalul de " + nrWaitingTasks + " versiuni");
			System.out.println("Se asteapta un numar de " + nrWaitingTasks + " versiuni ale aceluiasi task inainte de a accepta taskul");
		}
		if(corruptTasksAtExecutors == 1)
			System.out.println("Taskurile se corup de catre cei care le execute => toate nodurile care vor avea taskuri executate de acel executant le vor detine corupte");
		if(corruptCompletedTasksFromEncounteredNodes == 1)
			System.out.println("Taskurile se corup dupa ce au fost primite din lista de taskuri completate ale nodurilor intalnite");
	
		if(useHamming  == 1)
			System.out.println("Se folosesc coduri de corectie hamming");
        
        
        
        
        
        System.out.println("Trace duration in hours: " + duration);
        System.out.println("Trace contacts: " + parser.getTraceData().getContactsCount());
        System.out.println("Trace contacts per hour: " + (parser.getTraceData().getContactsCount() / duration));
        System.out.println("Nodes: " + parser.getNodesNumber());
        System.out.println("" + DropComputingStats.getAverageTaskDuration());
        System.out.println("Completion rate " + DropComputingStats.getCompletionRate()*100 + "%");
      /*  System.out.println("number of own task executed " + DropComputingStats.executedOwnTaskNumber);
        System.out.println("number of tasks created " + DropComputingStats.nrOfTasksCreated);
        System.out.println("number of tasks executed " + DropComputingStats.nrOfTasksExecuted);
        
        System.out.println();
        System.out.println("Number of task groups created " + DropComputingStats.taskGroups);
        System.out.println("Number of task groups completed " + DropComputingStats.taskGroupsCompleted);
        
        //daca a timpul de asteptare s-a incheiat si daca este setat sa se foloseasca cloud-ul, se trimite la cloud si se incrementeaza expirarea taskurilor, momentan e setat pe false si va fi 0 acest nr
        System.out.println("Number of task groups expirations " + DropComputingStats.taskGroupExpirations);
        
        System.out.println();
        System.out.println("Number of large tasks " + DropComputingStats.tasks[2] );
        System.out.println("Number of medium tasks " + DropComputingStats.tasks[1]);
        System.out.println("Number of small tasks " + DropComputingStats.tasks[0]);
        */
        System.out.println();
        System.out.println(" Number of depleting batteries " + DropComputingStats.batteryDepletions);
        System.out.println("Total computation time " + DropComputingStats.totalComputationDuration);
        
        //timpul de intretinere se mareste atunci cand o baterie a unui nod poate participa
      //  System.out.println("Total uptime " + DropComputingStats.totalUptime);
        
        System.out.println();
        System.out.println("Number of corrupted tasks " + DropComputingStats.corruptedTasks);
        System.out.println("Number of corrupted groups tasks " + DropComputingStats.corruptedTaskGroups);
        System.out.println("Number of onTick function calls " + DropComputingStats.onTickCallNumber);
        
        
        
        if(useRating == 1) {
    	DropComputing node = (DropComputing)nodes[0];
    	Map<Integer, Double> ratings = node.getNodesRating();
        for(double rating : ratings.values()){
        	if( rating >= 75.0 )
        		DropComputingStats.nrOfNodesHighRating++;
        	else 
        		DropComputingStats.nrOfNodesLowRating++;
        }
        }
        System.out.println();
        System.out.println("Percent of tasks which was accepting correct at destination " + DropComputingStats.getAverageUncorruptedReceivedTasks());
        System.out.println("Proccessing latency for every tasks between generation and execution " + DropComputingStats.getAverageProcessingLatency());
        System.out.println("Number of tasks which was accepted at destination executed by other nodes: " + DropComputingStats.nrOfTasksAcceptExecutedByOthers );
        if(useHamming == 1) {
        	System.out.println("Hamming correction: " + DropComputingStats.nrOfTasksHammingCorrection++ + " tasks");
        }
       System.out.println("\n\nDiferenta cu cel de mai sus ar trebui sa fie foarte mica"); 
        System.out.println("Number of nodes which was executed the tasks of other nodes: " + DropComputingStats.nrOfTasksAcceptExecutedByOthers);

        
        System.out.println("NUmber of nodes with high rating " + DropComputingStats.nrOfNodesHighRating);
        System.out.println("Number of nodes with low rating " + DropComputingStats.nrOfNodesLowRating);
        System.out.println("Number of tasks which had corrupted versions at destination but in minority " + DropComputingStats.nrOfCorruptedTasksButArrivedUncorrupted);
        System.out.println("Number of tasks which had more corrupted versions than uncorrupted " + DropComputingStats.moreCorruptedVersionThanOriginalVersion );
        
        System.out.println("Number of tasks which which was accepting corrupted version at destination even if in the network was more good than corrupted versions " + DropComputingStats.acceptCorruptedVersionInMinority);
        System.out.println("Number of tasks which which was accepting corrupted version at destination but the network was having more corrupted  than good versions " + DropComputingStats.acceptCorruptedVersionInMajority);
        
        System.out.println("Number of tasks which which was accepting uncorrupted at destination even if in the network was more corrupted than uncorrupted versions " + DropComputingStats.acceptGoodVersionInMinority);
        System.out.println("Number of tasks which which was accepting uncorrupted version at destination but the network was having more uncorrupted  than corrupted versions " + DropComputingStats.acceptGoodVersionInMajority);
        System.out.println("In total " + DropComputingStats.counter);
        /*
        System.out.println("" + Stats.computeHitRate(messages, nodes, dissemination));
        System.out.println("" + Stats.computeDeliveryCost(messages, nodes, dissemination));
        System.out.println("" + Stats.computeDeliveryLatency(messages, nodes, dissemination));
        System.out.println("" + Stats.computeHopCount(messages, nodes, dissemination));
    	*/
    }
}
