package mobemu.statistics;

import mobemu.node.Node;
import mobemu.utils.message.IMessage;
import mobemu.utils.message.IMessageGenerator;

import java.util.Calendar;
import java.util.List;
import java.util.Random;

/**
 * Created by radu on 5/12/2017.
 */
public abstract class BaseStatistics<T extends IMessage, U extends IMessageGenerator<T>> implements IStatistics<T, U> {

    /**
     * Used to generate messages of type T (which extends IMessage)
     */
    protected U messageGenerator;

    public BaseStatistics(U messageGenerator){
        this.messageGenerator = messageGenerator;
    }

    @Override
    public List<T> generateMessages(Node[] nodes, int messageCount, int messageCopies, long tick, boolean dissemination, Random random) {
        return messageGenerator.generateMessages(nodes, messageCount, messageCopies, tick, dissemination, random);
    }

    @Override
    public Calendar generateMessageTime(double value) {
        return messageGenerator.generateMessageTime(value);
    }
}
