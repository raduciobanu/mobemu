package mobemu.utils.message;

import mobemu.node.Node;

import java.util.Calendar;
import java.util.List;
import java.util.Random;

/**
 * Created by radu on 5/12/2017.
 */
public interface IMessageGenerator<T> {
    List<T> generateMessages(Node[] nodes, int messageCount, int messageCopies, long tick, boolean dissemination,
                             Random random);

    List<Long> getMessageGenerationTimes(Random random, long startTrace, long endTrace, long sampleTime);
}
