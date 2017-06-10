package mobemu.node.consensus.Malevolence;

import mobemu.utils.message.MessageList;

import java.util.List;

/**
 * Created by radu.dragan on 6/7/2017.
 */
public class MalevolenceOpinionListTowardsNode extends MessageList<MalevolenceOpinionTowardsNode> {
    @Override
    public List<MalevolenceOpinionTowardsNode> getByMessageId(int messageId) {
        return null;
    }

    public double getAverageOpinion(){
        double opinionsSum = 0d;
        for (MalevolenceOpinionTowardsNode opinion: messageList){
            opinionsSum += opinion.getValue();
        }

        return opinionsSum/messageList.size();
    }


}
