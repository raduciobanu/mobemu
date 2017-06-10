package mobemu.node.consensus;

import mobemu.utils.message.MessageList;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by radu on 5/6/2017.
 */
public class DecisionMessageList extends MessageList<ConsensusDecision> {

    @Override
    public boolean matches(ConsensusDecision existingMessage, ConsensusDecision newMessage) {
        return super.matches(existingMessage, newMessage)
                && existingMessage.getSourceId() == newMessage.getSourceId();
    }

    @Override
    public List<ConsensusDecision> getByMessageId(int messageId) {
        List<ConsensusDecision> decisionsForMessage = new ArrayList<>();
        for (ConsensusDecision decision : messageList) {
            if (decision.semiMatch(messageId)) {
                decisionsForMessage.add(decision);
            }
        }
        return decisionsForMessage;
    }
}
