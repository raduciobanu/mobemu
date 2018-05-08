/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package mobemu;

import java.util.*;

import mobemu.algorithms.BubbleRap;
import mobemu.algorithms.DropComputing;
import mobemu.algorithms.Epidemic;
import mobemu.algorithms.GrAnt;
import mobemu.algorithms.IRONMAN;
import mobemu.algorithms.JDER;
import mobemu.algorithms.SAROS;
import mobemu.algorithms.SENSE;
import mobemu.algorithms.SPRINT;
import mobemu.node.Message;
import mobemu.node.Node;
import mobemu.node.Stats;
import mobemu.parsers.GeoLife;
import mobemu.parsers.HCMM;
import mobemu.parsers.Haggle;
import mobemu.parsers.NCCU;
import mobemu.parsers.Sigcomm;
import mobemu.parsers.StAndrews;
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
        //Parser parser = new UPB(UPB.UpbTrace.UPB2012);
    	//Parser parser = new Haggle(Haggle.HaggleTrace.CONTENT);
    	//Parser parser = new HCMM(30, 48 * 3600, 30 * 24 * 3600, 1.25f, 1.50f, 0.1f, 1000f, 1000f, 100, 100, 10.0, 0.7, 5, 5, 1.25f, 0.8f, 0);

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
            nodes[i] = new GrAnt(i, nodes.length, parser.getContextData().get(i), parser.getSocialNetwork()[i],
                    10000, 100, seed, parser.getTraceData().getStartTime(), parser.getTraceData().getEndTime(), dissemination, false);
           // nodes[i] = new Epidemic(i, nodes.length, parser.getContextData().get(i), parser.getSocialNetwork()[i],
           //         10000, 100, seed, parser.getTraceData().getStartTime(), parser.getTraceData().getEndTime(), dissemination, false);
           // nodes[i] = new BubbleRap(i, nodes.length, parser.getContextData().get(i), parser.getSocialNetwork()[i],
           //         10000, 100, seed, parser.getTraceData().getStartTime(), parser.getTraceData().getEndTime());
           // nodes[i] = new IRONMAN(i, nodes.length, parser.getContextData().get(i), parser.getSocialNetwork()[i],
           //         10000, 100, seed, parser.getTraceData().getStartTime(), parser.getTraceData().getEndTime(), false);         
           // nodes[i] = new JDER(i, nodes.length, parser.getContextData().get(i), parser.getSocialNetwork()[i],
           //         10000, 100, seed, parser.getTraceData().getStartTime(), parser.getTraceData().getEndTime(), 10000, true, true, true, parser.getTraceData());         	
           // nodes[i] = new SENSE(i, parser.getContextData().get(i), parser.getSocialNetwork()[i],
           //         10000, 100, seed, parser.getTraceData().getStartTime(), parser.getTraceData().getEndTime(), false, nodes);               
           //nodes[i] = new SPRINT(i, parser.getContextData().get(i), parser.getSocialNetwork()[i],
           //            10000, 100, seed, parser.getTraceData().getStartTime(), parser.getTraceData().getEndTime(), false, nodes, 1000);           
        }

        // run the trace
        List<Message> messages = Node.runTrace(nodes, parser.getTraceData(), false, dissemination, seed);
        System.out.println("Messages Count: " + messages.size());

        // print opportunistic algorithm statistics
        System.out.println(nodes[0].getName());
        System.out.println(GrAnt.totalDelivers);
        System.out.println("" + Stats.computeHitRate(messages, nodes, dissemination));
        System.out.println("" + Stats.computeDeliveryCost(messages, nodes, dissemination));
        System.out.println("" + Stats.computeDeliveryLatency(messages, nodes, dissemination));
        System.out.println("" + Stats.computeHopCount(messages, nodes, dissemination));
        System.out.println("" + DropComputing.DropComputingStats.getAverageTaskDuration());
    }
}
