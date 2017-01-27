package mobemu.node.leader.directLeaderElection;

import mobemu.node.Node;
import mobemu.node.leader.LeaderNode;
import mobemu.node.leader.communityBasedLeaderElection.CommunityLeaderNode;
import mobemu.node.leader.directLeaderElection.dto.HeartbeatResponse;

import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;

import static mobemu.utils.Constants.heartBeatGenerationTime;

/**
 * Created by radu on 1/15/2017.
 */
public class LeaderStats {


    public static PrintWriter openFile(String filename){
        PrintWriter writer = null;

        try {
            writer = new PrintWriter(filename, "UTF-8");

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

        System.out.println(filename + " opened!");
        return writer;
    }

    public static void closeFile(PrintWriter writer){
        writer.close();
    }

    /**
     * Print the local communities for all nodes in order to learn about their differences
     */
    public static void printLocalCommunities(Node[] nodes, long tick, long startTime, PrintWriter writer){
        if((tick - startTime) % 100000 != 0)
            return;

        if(writer == null)
            return;

        boolean newLine = false;
        for (Node node : nodes){
            if(node.getLocalCommunity().size() <= 1)
                continue;

            String line = "{" + node.getId() + "}" + node.getLocalCommunity() + " -- ";
            writer.print(line);
//            System.out.print(line);
            newLine = true;
        }

        if(newLine){
            writer.println();
//            System.out.println();
        }

    }

    public static void generateHeartBeats(Node[] nodes, long tick, long startTime){
        if((tick - startTime) % heartBeatGenerationTime == 0){
            for(Node node: nodes){
                LeaderNode leaderNode = (LeaderNode) node;

                leaderNode.generateHeartBeat(tick);
            }
        }
    }

    public static void computeAverageHeartBeatResponseTime(Node[] nodes, PrintWriter writer){
        long sumResponseTime = 0;
        long sumHopCount = 0;
        long number = 0;
        long millisInAnHour= 1000 * 3600;

        for (Node node : nodes){
            LeaderNode leaderNode = (LeaderNode) node;
            for(HeartbeatResponse heartbeatResponse : leaderNode.getResponseTimes()){
                //skip scenarios when the node is its own leader
//                if(responseTime == 0)
//                    continue;

                long responseTime = heartbeatResponse.getResponseTime();
                int hopCount = heartbeatResponse.getHopCount();
                sumResponseTime += responseTime;
                sumHopCount += hopCount;
                number++;

                writer.println(responseTime / millisInAnHour);
            }
        }

        System.out.println("Average responseTime: " + sumResponseTime/(number * millisInAnHour));
        System.out.println("Average hopCount:" + sumHopCount / number);
    }

    public static void printLeaderCommunities(Node[] nodes){
        System.out.println("LeaderCommunities:");
        for(Node node:nodes){
            CommunityLeaderNode leaderNode = (CommunityLeaderNode)node;

            System.out.println(leaderNode.getId() + " -- " + leaderNode.getLeaderCommunity().getNodes());
        }
    }

//    public static void printDirectLeaderCommunities(Node[] nodes){
//
//    }
}
