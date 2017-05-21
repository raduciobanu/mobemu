package mobemu.statistics;

import mobemu.node.Message;
import mobemu.node.Node;
import mobemu.node.leader.LeaderNode;
import mobemu.node.leader.communityBasedLeaderElection.CommunityLeaderNode;
import mobemu.node.leader.directLeaderElection.dto.HeartbeatResponse;
import mobemu.utils.message.MessageGenerator;

import static mobemu.utils.Constants.heartBeatGenerationTime;

/**
 * Created by radu on 5/12/2017.
 */
public class LeaderStatistics extends BaseStatisticsWriteToFile<Message, MessageGenerator> {

    public LeaderStatistics() {
        super(new MessageGenerator());
    }

    @Override
    public void runBeforeTraceStart() {
        super.runBeforeTraceStart();
    }

    @Override
    public void runEveryTick(Node[] nodes, long tick, long startTime) {
        generateHeartBeats(nodes, tick, startTime);
        checkCommunities(nodes, tick, startTime);
    }

    @Override
    public void runAfterTraceEnd(Node[] nodes) {
        computesContactsRatio(nodes);
        computeAverageHeartBeatResponseTime(nodes);

        super.closeFile();
    }

    private void generateHeartBeats(Node[] nodes, long tick, long startTime){
        if((tick - startTime) % heartBeatGenerationTime == 0){
            for(Node node: nodes){
                if(!(node instanceof LeaderNode))
                    continue;

                LeaderNode leaderNode = (LeaderNode) node;
                leaderNode.generateHeartBeat(tick);
            }
        }
    }

    public void checkCommunities(Node[] nodes, long tick, long startTime){
        if((tick - startTime) % (heartBeatGenerationTime * 3) == 0){
            for(Node node: nodes){
                CommunityLeaderNode leaderNode = getCommunityLeaderNode(node);
                if(leaderNode == null)
                    return;

                leaderNode.checkCommunities(tick);
            }
        }
    }

    public static CommunityLeaderNode getCommunityLeaderNode(Node node){
        if(!(node instanceof CommunityLeaderNode))
            return null;

        return (CommunityLeaderNode) node;
    }

    public void computesContactsRatio(Node[] nodes){

        double sumNumberOfContacts = 0;
        double totalNumberOfNodes = 0;
        double sumNumberOfContactsLeader = 0;
        double noOfLeaders = 0;
        for(Node node: nodes){
            CommunityLeaderNode leaderNode = getCommunityLeaderNode(node);
            if(leaderNode == null)
                return;

            int leaderNodeId = leaderNode.getLeaderNodeId();
            if(leaderNodeId != -1){
                int numberOfContactsWithLeader = leaderNode.getContactsNumber(leaderNodeId);
                sumNumberOfContactsLeader+= numberOfContactsWithLeader;
                noOfLeaders++;
            }

            for (int encounteredNodeId : leaderNode.getLeaderCommunityNodes()){
                int contactsNumber = leaderNode.getContactsNumber(encounteredNodeId);
                sumNumberOfContacts += contactsNumber;
            }
            totalNumberOfNodes += leaderNode.getLeaderCommunityNodes().size();
        }

        double avg = sumNumberOfContacts / totalNumberOfNodes;
        double avgLeaders = sumNumberOfContactsLeader / noOfLeaders;
        System.out.println("Contacts ratio:" + avg/avgLeaders);
    }

    public void computeAverageHeartBeatResponseTime(Node[] nodes){
        long sumResponseTime = 0;
        long sumHopCount = 0;
        long number = 0;
        long millisInAnHour= 1000 * 3600;
        double sumOwnHeartBeats = 0.0;
        double sumResponses = 0.0;


        for (Node node : nodes){
            if(!(node instanceof LeaderNode))
                continue;

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



                writer.println((double)responseTime / (double)millisInAnHour);
            }

            int numberOfOwnHeartBeats = leaderNode.getOwnHeartBeats().size();
            int numberOfResponses = leaderNode.getResponseTimes().size();

            sumOwnHeartBeats += numberOfOwnHeartBeats;
            sumResponses += numberOfResponses;

//            System.out.println(leaderNode.getId() + " -- " + numberOfOwnHeartBeats + " -- " + numberOfResponses);

//            System.out.println(leaderNode.getId() + " -- " + leaderNode.getLeaderNodeId() + " -- " +
//                    leaderNode.getResponseTimes());
        }

        System.out.println("Response percentage: " + sumResponses / sumOwnHeartBeats);
        System.out.println("Average responseTime: " + (double)sumResponseTime/(double)(number * millisInAnHour));
        System.out.println("Average hopCount:" + sumHopCount / number);
    }

    /**
     * Print the local communities for all nodes in order to learn about their differences
     */
//    public void printLocalCommunities(Node[] nodes, long tick, long startTime, PrintWriter writer){
//        if((tick - startTime) % 100000 != 0)
//            return;
//
//        if(writer == null)
//            return;
//
//        boolean newLine = false;
//        for (Node node : nodes){
//            if(node.getLocalCommunity().size() <= 1)
//                continue;
//
//            String line = "{" + node.getId() + "}" + node.getLocalCommunity() + " -- ";
//            writer.print(line);
////            System.out.print(line);
//            newLine = true;
//        }
//
//        if(newLine){
//            writer.println();
////            System.out.println();
//        }
//
//    }

//    public static void printLeaderCommunities(Node[] nodes){
//        System.out.println("LeaderCommunities:");
//        for(Node node:nodes){
//            CommunityLeaderNode leaderNode = getCommunityLeaderNode(node);
//            if(leaderNode == null)
//                return;
//
//            System.out.println(leaderNode.getId() + " -- " + leaderNode.getLeaderCommunityNodes());
//        }
//    }


}
