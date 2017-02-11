package mobemu.node.leader.communityBasedLeaderElection.dto.communityMessage.handlers;

import mobemu.node.leader.communityBasedLeaderElection.CommunityLeaderNode;
import mobemu.node.leader.communityBasedLeaderElection.dto.CommunityMessageType;
import mobemu.node.leader.communityBasedLeaderElection.dto.LeaderCommunity;
import mobemu.node.leader.communityBasedLeaderElection.dto.communityMessage.CommunityMessage;

import java.util.Set;

import static mobemu.utils.Constants.leaderCommunityThreshold;
import static mobemu.utils.Constants.leaderProposalsThreshold;

/**
 * Created by radu on 2/5/2017.
 *
 * Base class of handlers for community messages
 * Implementing chain of responsibility pattern
 */
public abstract class CommunityMessageHandler {

    /**
     * Next handler in the chain of responsibility
     */
    protected CommunityMessageHandler nextHandler;

    /**
     * The node current handler is intended for
     */
    protected CommunityLeaderNode leaderNode;

    /**
     * The type of message current handler can handle
     */
    protected CommunityMessageType messageType;

    public CommunityMessageHandler(CommunityLeaderNode leaderNode){
        this.leaderNode = leaderNode;
    }

    public void setNextHandler(CommunityMessageHandler nextHandler){
        this.nextHandler = nextHandler;
    }

    public void receiveMessage(CommunityMessage communityMessage, long currentTime) throws Exception {
        if(communityMessage.getMessageType() == messageType){
            parse(communityMessage, currentTime);
            return;
        }

        if(nextHandler == null)
            throw new Exception("No handler found for message with type " + communityMessage.getMessageType());

        nextHandler.receiveMessage(communityMessage, currentTime);
    }

    protected abstract void parse(CommunityMessage communityMessage, long currentTime);

    protected boolean checkCommunityThreshold(int proposalCollectionSize){
        Set<Integer> leaderCommunityNodes = leaderNode.getLeaderCommunityNodes();
        return Double.compare(proposalCollectionSize,
                leaderProposalsThreshold * leaderCommunityNodes.size() - 1) > 0;
    }

    protected boolean checkCommunityThreshold(int proposalCollectionSize, int leaderCommunitySize){
        return Double.compare(proposalCollectionSize,
                leaderProposalsThreshold * leaderCommunitySize - 1) > 0;
    }
}
