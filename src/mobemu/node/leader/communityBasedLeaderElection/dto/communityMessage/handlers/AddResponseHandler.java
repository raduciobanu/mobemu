//package mobemu.node.leader.communityBasedLeaderElection.dto.communityMessage.handlers;
//
//import mobemu.node.leader.communityBasedLeaderElection.CommunityLeaderNode;
//import mobemu.node.leader.communityBasedLeaderElection.dto.CommunityMessageType;
//import mobemu.node.leader.communityBasedLeaderElection.dto.LeaderCommunity;
//import mobemu.node.leader.communityBasedLeaderElection.dto.communityMessage.CommunityMessage;
//
//import java.util.List;
//
//import static mobemu.utils.Constants.leaderCommunityThreshold;
//
///**
// * Created by radu on 2/5/2017.
// */
//public class AddResponseHandler extends CommunityMessageHandler{
//
//    public AddResponseHandler(CommunityLeaderNode leaderNode){
//        super(leaderNode);
//        this.messageType = CommunityMessageType.AddResponse;
//    }
//
//    @Override
//    protected void parse(CommunityMessage communityMessage, long currentTime) {
//        int targetId = communityMessage.getTargetId();
//        List<Integer> nodesThatConfirmed = leaderNode.getReceivedResponses().get(targetId);
//        if(!nodesThatConfirmed.contains(targetId)){
//            nodesThatConfirmed.add(targetId);
//
//            checkLeaderCommunityThreshold(targetId, currentTime);
//        }
//    }
//
//    protected void checkLeaderCommunityThreshold(int targetId, long currentTime){
//        List<Integer> nodesThatConfirmed = leaderNode.getReceivedResponses().get(targetId);
//        LeaderCommunity leaderCommunity = leaderNode.getLeaderCommunity();
//        if(nodesThatConfirmed.size() > leaderCommunityThreshold * leaderCommunity.size() - 1){
//            double targetCentrality = leaderNode.getRequestsSent().get(targetId);
//
//            leaderCommunity.add(targetId, targetCentrality, currentTime);
//
//            for(int nodeId : leaderCommunity.getNodes()){
//                leaderNode.addToOwnCommunityMessages(
//                        CommunityMessage.CreateAddedNode(
//                                leaderNode.getId(), nodeId, targetId, targetCentrality, currentTime)
//                );
//                leaderNode.proposeLeader(currentTime);
//            }
//        }
//    }
//}
