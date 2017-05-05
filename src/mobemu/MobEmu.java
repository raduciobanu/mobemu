/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package mobemu;

import java.util.*;
import mobemu.algorithms.Epidemic;
import mobemu.algorithms.SPRINT;
import mobemu.node.Message;
import mobemu.node.Node;
import mobemu.node.Stats;
import mobemu.node.leader.communityBasedLeaderElection.CommunityLeaderNode;
import mobemu.node.leader.directLeaderElection.DirectLeaderElectionNode;
import mobemu.parsers.HCMM;
import mobemu.parsers.Sigcomm;
import mobemu.trace.Parser;
import mobemu.utils.Constants;

import static mobemu.utils.Constants.*;

/**
 * Main class for MobEmu.
 *
 * @author Radu
 */
public class MobEmu {

    public static void main(String[] args) {

        int nodesNo = 40;
        float days = 1f;
        float size = 1000f;
        int tiles = 100;
        int groups = 4;
        int travelers = 10;
        Parser parser = new HCMM(nodesNo, (int)(days * 2 * 3600), 30 * 24 * 3600, 1.25f, 1.50f, 0.1f, size, size, tiles, tiles, 10.0, 0.7, groups, travelers, 1.50f, 0.8f, 0);

//        Parser parser = new Sigcomm();
//                Parser parser = new UPB(UPB.UpbTrace.UPB2012);

        //set some system variables
//        leaderCommunityThreshold = Double.parseDouble(args[0]);
//        leaderProposalsThreshold = Double.parseDouble(args[1]);

        centralityWeight = Double.parseDouble(args[0]);
        trustWeight = Double.parseDouble(args[1]);
        probabilityWeight = Double.parseDouble(args[2]);
        latencyWeight = Double.parseDouble(args[3]);

        Constants.responseTimesFileName = "responseTimes_direct_HCMM_" + centralityWeight + "_" +
                trustWeight + "_" + probabilityWeight
                + "_" + latencyWeight
                + ".txt";


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
//            nodes[i] = new Epidemic(i, nodes.length, parser.getContextData().get(i), parser.getSocialNetwork()[i],
//                    10000, 100, seed, parser.getTraceData().getStartTime(), parser.getTraceData().getEndTime(), dissemination, false);
//                        nodes[i] = new SPRINT(i, parser.getContextData().get(i), parser.getSocialNetwork()[i],
//                                dataMemorySize, exchangeHistorySize, seed, parser.getTraceData().getStartTime(),
//                                parser.getTraceData().getEndTime(), false, nodes, cacheMemorySize);
            nodes[i] = new DirectLeaderElectionNode(i, parser.getContextData().get(i), parser.getSocialNetwork()[i],
                    dataMemorySize, exchangeHistorySize, seed, parser.getTraceData().getStartTime(),
                    parser.getTraceData().getEndTime(), false, nodes, cacheMemorySize);

//            nodes[i] = new CommunityLeaderNode(i, parser.getContextData().get(i), parser.getSocialNetwork()[i],
//                    dataMemorySize, exchangeHistorySize, seed, parser.getTraceData().getStartTime(),
//                    parser.getTraceData().getEndTime(), false, nodes, cacheMemorySize);

        }

        // run the trace
        List<Message> messages = Node.runTrace(nodes, parser.getTraceData(), false, dissemination, seed);
        System.out.println("Messages: " + messages.size());

        // print opportunistic algorithm statistics
        System.out.println(nodes[0].getName());
//        System.out.println("" + Stats.computeHitRate(messages, nodes, dissemination));
//        System.out.println("" + Stats.computeDeliveryCost(messages, nodes, dissemination));
//        System.out.println("" + Stats.computeDeliveryLatency(messages, nodes, dissemination));
//        System.out.println("" + Stats.computeHopCount(messages, nodes, dissemination));
    }
}
