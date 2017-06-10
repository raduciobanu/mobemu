package mobemu.node.consensus.Malevolence;

import mobemu.utils.message.MessageList;

import java.util.List;

/**
 * Created by radu.dragan on 6/5/2017.
 */
public class MalevolenceOpinionList extends MessageList<MalevolenceOpinion> {
    @Override
    public List<MalevolenceOpinion> getByMessageId(int messageId) {
        return null;
    }

    public double computeAverage(){
        double sum = 0d;
        for (MalevolenceOpinion opinion: messageList){
            sum += opinion.getValue();
        }

        return sum / messageList.size();
    }


}
