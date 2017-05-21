package mobemu.utils.message;

import mobemu.node.Message;
import mobemu.node.Node;

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
    public Calendar generateMessageTime(double value) {
        return Message.generateMessageTime(value);
    }
}
