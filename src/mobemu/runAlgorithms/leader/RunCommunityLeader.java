package mobemu.runAlgorithms.leader;

import mobemu.node.Node;
import mobemu.node.leader.communityBasedLeaderElection.CommunityLeaderNode;
import mobemu.utils.Constants;

import static mobemu.utils.Constants.*;
import static mobemu.utils.Constants.cacheMemorySize;

/**
 * Created by radu on 5/12/2017.
 */
public class RunCommunityLeader extends RunLeader{

    public RunCommunityLeader(String[] args) {
        super(args);
    }

    @Override
    protected void initialize(String[] args){
        centralityWeight = Double.parseDouble(args[0]);
        trustWeight = Double.parseDouble(args[1]);
        probabilityWeight = Double.parseDouble(args[2]);

        Constants.responseTimesFileName = "responseTimes_direct_HCMM_" + centralityWeight + "_" +
                trustWeight + "_" + probabilityWeight + ".txt";
    }

    @Override
    public Node initializeNode(int index, long seed, Node[] nodes) {
        return new CommunityLeaderNode(index, parser.getContextData().get(index), parser.getSocialNetwork()[index],
                dataMemorySize, exchangeHistorySize, seed, parser.getTraceData().getStartTime(),
                parser.getTraceData().getEndTime(), false, nodes, cacheMemorySize, null);
    }
}
