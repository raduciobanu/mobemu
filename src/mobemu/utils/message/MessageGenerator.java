package mobemu.utils.message;

import mobemu.node.Message;
import mobemu.node.Node;
import mobemu.utils.Constants;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Random;

/**
 * Created by radu on 5/12/2017.
 */
public class MessageGenerator implements IMessageGenerator<Message> {
    @Override
    public List<Message> generateMessages(Node[] nodes, int messageCount, int messageCopies, long tick,
                                          boolean dissemination, Random random) {
        return Message.generateMessages(nodes, messageCount, messageCopies, tick, dissemination, random);
    }

    @Override
    public List<Long> getMessageGenerationTimes(Random random, long startTrace, long endTrace, long sampleTime) {
        long generationWindow = (endTrace - startTrace) / Constants.numberOfGeneratedMessages;

        List<Long> generationTimes = new ArrayList<>();
        for (int index = 0; index < Constants.numberOfGeneratedMessages; index++){
            long generationTimeInWindow = (long)(random.nextDouble() * (generationWindow / 4));

            long generationTime = startTrace + generationWindow * index + generationTimeInWindow;

            //round to sample time
            long remainder = generationTime % sampleTime;
            generationTime -=remainder;

            generationTimes.add(generationTime);
        }

        return generationTimes;
    }
}
