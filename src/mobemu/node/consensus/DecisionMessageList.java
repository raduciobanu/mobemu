package mobemu.node.consensus;

import mobemu.utils.message.MessageList;

/**
 * Created by radu on 5/6/2017.
 */
public class DecisionMessageList extends MessageList<ConsensusDecision> {

    @Override
    public boolean matches(ConsensusDecision existingMessage, ConsensusDecision newMessage) {
        return super.matches(existingMessage, newMessage)
                && existingMessage.getSourceId() == newMessage.getSourceId();
    }
}
