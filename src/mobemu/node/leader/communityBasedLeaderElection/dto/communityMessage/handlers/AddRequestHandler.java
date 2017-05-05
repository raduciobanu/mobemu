package mobemu.node.leader.communityBasedLeaderElection.dto.communityMessage.handlers;

import mobemu.node.leader.communityBasedLeaderElection.CommunityLeaderNode;
import mobemu.node.leader.communityBasedLeaderElection.dto.CommunityMessageType;
import mobemu.node.leader.communityBasedLeaderElection.dto.LeaderCommunity;
import mobemu.node.leader.communityBasedLeaderElection.dto.communityMessage.CommunityMessage;
import mobemu.node.leader.communityBasedLeaderElection.dto.proposals.AddProposal;
import mobemu.node.leader.communityBasedLeaderElection.dto.proposals.collections.AddProposalCollection;

import java.util.List;
import java.util.Set;

/**
 * Created by radu on 2/5/2017.
 */
public class AddRequestHandler extends CommunityMessageHandler{

    private AddProposalCollection addProposalCollection;

    public AddRequestHandler(CommunityLeaderNode leaderNode){
        super(leaderNode);
        this.messageType = CommunityMessageType.AddRequest;
        this.addProposalCollection = new AddProposalCollection();
    }

    @Override
    protected void parse(CommunityMessage communityMessage, long currentTime) {
        int sourceId = communityMessage.getSourceId();
        int targetId = communityMessage.getTargetId();
        int leaderNodeId = leaderNode.getId();
        double targetCentrality = communityMessage.getTargetCentrality();
//        int messageHopCount = communityMessage.getHopCount(leaderNodeId);

        double probabilityOfMeeting = leaderNode.getProbabilityOfMeetingNode(leaderNode, targetId, currentTime);

        if(probabilityOfMeeting > 0.0){
//            System.out.println(leaderNodeId + " will meet " + targetId + " with a probability of "
//                    + probabilityOfMeeting);
        }

        AddProposal addProposal = new AddProposal(sourceId, targetId, currentTime, targetCentrality);
        addProposalCollection.add(addProposal);
        if(leaderNode.shouldBeToLeaderCommunity(targetId)) {
//            leaderNode.addToOwnCommunityMessages(
//                    CommunityMessage.CreateResponse(leaderNodeId, sourceId, targetId, currentTime, messageHopCount));
            checkLeaderCommunityThreshold(targetId, currentTime, targetCentrality);
        }
    }

    /**
     * Check the threshold only for leader community
     * @param targetId the id of the node for the check
     * @param currentTime current timestamp
     */
    private void checkLeaderCommunityThreshold(int targetId, long currentTime, double targetCentrality){
        if(checkCommunityThreshold(addProposalCollection.size())){
            leaderNode.addToLeaderCommunity(targetId, targetCentrality, currentTime);

            Set<Integer> leaderCommunityNodes = leaderNode.getLeaderCommunityNodes();
            for(int nodeId : leaderCommunityNodes){
                leaderNode.addToOwnCommunityMessages(CommunityMessage.CreateAddedNode(leaderNode.getId(),
                        nodeId, targetId, targetCentrality, currentTime));
                leaderNode.proposeLeader(currentTime);
            }
        }
    }


}
