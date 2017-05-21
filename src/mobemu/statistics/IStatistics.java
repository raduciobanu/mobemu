package mobemu.statistics;

import mobemu.node.Node;
import mobemu.utils.message.IMessage;
import mobemu.utils.message.IMessageGenerator;

/**
 * Created by radu on 5/12/2017.
 */
public interface IStatistics<T extends IMessage, U extends IMessageGenerator<T>> extends IMessageGenerator<T> {

    void runBeforeTraceStart();
    void runEveryTick(Node[] nodes, long tick, long startTime);
    void runAfterTraceEnd(Node[] nodes);
}
