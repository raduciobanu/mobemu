/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package mobemu;

import java.util.*;

import mobemu.algorithms.DropComputing;
import mobemu.algorithms.DropComputing.DeviceType;
import mobemu.algorithms.DropComputing.DropComputingStats;
import mobemu.algorithms.Epidemic;
import mobemu.node.Context;
import mobemu.node.Message;
import mobemu.node.Node;
import mobemu.node.Stats;
import mobemu.parsers.UPB;
import mobemu.trace.Parser;

/**
 * Main class for MobEmu.
 *
 * @author Radu
 */
public class MobEmu {

    public static void main(String[] args) {
        Parser parser = new UPB(UPB.UpbTrace.UPB2011);
        //se parseaza fisierul social.dat si upb2011.dat din traces/upb2011 si se adauga un contact in lista de contacte care e retinuta in clasa Trace
        //creandu se un trace la inceputul parsarii in metoda de parsare din clasa UPB, la 2011 nr. de device-uri e hardcodat la 22
        // print some trace statistics
        double duration = (double) (parser.getTraceData().getEndTime() - parser.getTraceData().getStartTime()) / (Parser.MILLIS_PER_MINUTE * 60);
        System.out.println("Trace duration in hours: " + duration);
        System.out.println("Trace contacts: " + parser.getTraceData().getContactsCount());
        System.out.println("Trace contacts per hour: " + (parser.getTraceData().getContactsCount() / duration));
        System.out.println("Nodes: " + parser.getNodesNumber());

        // initialize Epidemic nodes
        long seed = 0;
        boolean dissemination = false;
        Node[] nodes = new Node[parser.getNodesNumber()];
        for (int i = 0; i < nodes.length; i++) {
           // nodes[i] = new Epidemic(i, nodes.length, parser.getContextData().get(i), parser.getSocialNetwork()[i],
             //       10000, 100, seed, parser.getTraceData().getStartTime(), parser.getTraceData().getEndTime(), dissemination, false);
        
        	
        	
        	/*public DropComputing(int id, int nodes, Context context, boolean[] socialNetwork, int dataMemorySize,
                    int exchangeHistorySize, long seed, long traceStart, long traceEnd, DeviceType deviceType,
                    long sampleTime, boolean opportunistic, boolean useCloud) */
        	
        	nodes[i] = new DropComputing(i, nodes.length, parser.getContextData().get(i),  parser.getSocialNetwork()[i], 10000, 100, seed, parser.getTraceData().getStartTime(), parser.getTraceData().getEndTime(),
        			DropComputing.DeviceType.IPHONE_6S, parser.getTraceData().getSampleTime(), true, false);
        }
        System.out.println("first node" + nodes[0]);


        // run the trace
        //false  = consumul bateriei nu se ia in considerare, true otherwise
        List<Message> messages = Node.runTrace(nodes, parser.getTraceData(), false, dissemination, seed);
        System.out.println("Messages: " + messages.size());
      //  System.out.println(messages);
        // print opportunistic algorithm statistics
        System.out.println(nodes[0].getName());
        
        System.out.println("" + DropComputingStats.getAverageTaskDuration());
        System.out.println("Completion rate " + DropComputingStats.getCompletionRate()*100 + "%");
        System.out.println("number of own task executed " + DropComputingStats.executedOwnTaskNumber);
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
        
        System.out.println();
        System.out.println(" Number of depleting batteries " + DropComputingStats.batteryDepletions);
        System.out.println("Total computation time " + DropComputingStats.totalComputationDuration);
        
        //timpul de intretinere se mareste atunci cand o baterie a unui nod poate participa
        System.out.println("Total uptime " + DropComputingStats.totalUptime);
        
        System.out.println();
        System.out.println("Number of corrupted tasks " + DropComputingStats.corruptedTasks);
        System.out.println("Number of corrupted groups tasks " + DropComputingStats.corruptedTaskGroups);
        System.out.println("Number of onTick function calls " + DropComputingStats.onTickCallNumber);
        /*
        System.out.println("" + Stats.computeHitRate(messages, nodes, dissemination));
        System.out.println("" + Stats.computeDeliveryCost(messages, nodes, dissemination));
        System.out.println("" + Stats.computeDeliveryLatency(messages, nodes, dissemination));
        System.out.println("" + Stats.computeHopCount(messages, nodes, dissemination));
    	*/
    }
}
