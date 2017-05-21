package mobemu.runAlgorithms.leader;

import mobemu.node.Node;
import mobemu.node.leader.directLeaderElection.DirectLeaderElectionNode;
import mobemu.utils.Constants;

import java.util.Random;

import static mobemu.utils.Constants.*;

/**
 * Created by radu on 5/12/2017.
 */
public class RunDirectLeader extends RunLeader {

    public RunDirectLeader(String[] args) {
        super(args);
    }

    @Override
    protected void initialize(String[] args){
        centralityWeight = Double.parseDouble(args[0]);
        trustWeight = Double.parseDouble(args[1]);
        probabilityWeight = Double.parseDouble(args[2]);
        latencyWeight = Double.parseDouble(args[3]);

        Constants.responseTimesFileName = "responseTimes_direct_HCMM_" + centralityWeight + "_" +
                trustWeight + "_" + probabilityWeight
                + "_" + latencyWeight
                + ".txt";
    }

    @Override
    public Node initializeNode(int index, long seed, Node[] nodes) {
        return new DirectLeaderElectionNode(index, parser.getContextData().get(index), parser.getSocialNetwork()[index],
                dataMemorySize, exchangeHistorySize, seed, parser.getTraceData().getStartTime(),
                parser.getTraceData().getEndTime(), false, nodes, cacheMemorySize);
    }
}
