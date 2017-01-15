package mobemu.node.directLeaderElection;

import mobemu.node.Node;

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
                DirectLiderElectionNode leaderNode = (DirectLiderElectionNode) node;

                leaderNode.generateHeartBeat(tick);
            }
        }
    }

    public static void computeAverageHeartBeatResponseTime(Node[] nodes, PrintWriter writer){
        long sum = 0;
        int number = 0;

        for (Node node : nodes){
            DirectLiderElectionNode leaderNode = (DirectLiderElectionNode) node;
            for(long responseTime : leaderNode.getResponseTimes()){
                //skip scenarios when the node is its own leader
                if(responseTime == 0)
                    continue;

                sum += responseTime;
                number++;

                writer.println(responseTime / 1000);
            }
        }

        System.out.println(sum/(number * 1000));
    }
}
