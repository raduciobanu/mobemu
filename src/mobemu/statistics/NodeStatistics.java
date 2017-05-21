package mobemu.statistics;

import mobemu.node.Message;
import mobemu.node.Node;
import mobemu.utils.message.MessageGenerator;

/**
 * Created by radu on 5/12/2017.
 */
public class NodeStatistics extends BaseStatistics<Message, MessageGenerator> {

    public NodeStatistics() {
        super(new MessageGenerator());
    }

    @Override
    public void runBeforeTraceStart() {

    }

    @Override
    public void runEveryTick(Node[] nodes, long tick, long startTime) {

    }

    @Override
    public void runAfterTraceEnd(Node[] nodes) {

    }
}
