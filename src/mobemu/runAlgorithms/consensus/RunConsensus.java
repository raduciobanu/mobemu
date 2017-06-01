package mobemu.runAlgorithms.consensus;

import mobemu.node.Node;
import mobemu.node.consensus.ConsensusLeaderNode;
import mobemu.node.leader.LeaderNode;
import mobemu.node.leader.communityBasedLeaderElection.CommunityLeaderNode;
import mobemu.node.leader.directLeaderElection.DirectLeaderElectionNode;
import mobemu.runAlgorithms.RunNode;
import mobemu.statistics.ConsensusStatistics;
import mobemu.utils.Constants;

import static mobemu.utils.Constants.*;

/**
 * Created by radu on 5/13/2017.
 */
public class RunConsensus extends RunNode{
    public String leaderElectionAlgorithm;

    public RunConsensus(String[] args) {
        super(args);

        statistics = new ConsensusStatistics(Float.parseFloat(args[0]));
        leaderElectionAlgorithm = "direct";
        Constants.responseTimesFileName = "results_consensus_" + leaderElectionAlgorithm + "_" + parser.getTraceData().getName()
                + "_" + args[0]+ ".txt";
        Constants.deliveryLatenciesFileName = "latencies_consensus_"+ leaderElectionAlgorithm + "_" + parser.getTraceData().getName()
                + "_" + args[0]+ ".txt";
    }

    @Override
    public Node initializeNode(int index, long seed, Node[] nodes) {
        LeaderNode leaderNode = getDirectLeaderElectionNode(index, seed, nodes);

        return new ConsensusLeaderNode(index, parser.getContextData().get(index), parser.getSocialNetwork()[index],
                dataMemorySize, exchangeHistorySize, seed, parser.getTraceData().getStartTime(),
                parser.getTraceData().getEndTime(), false, nodes, cacheMemorySize, leaderNode);
    }

    public DirectLeaderElectionNode getDirectLeaderElectionNode(int index, long seed, Node[] nodes){
        this.leaderElectionAlgorithm = "direct";

        return new DirectLeaderElectionNode(index, parser.getContextData().get(index), parser.getSocialNetwork()[index],
                dataMemorySize, exchangeHistorySize, seed, parser.getTraceData().getStartTime(),
                parser.getTraceData().getEndTime(), false, nodes, cacheMemorySize, null);
    }

    public CommunityLeaderNode getCommunityLeaderNode(int index, long seed, Node[] nodes){
        this.leaderElectionAlgorithm = "community";

        return new CommunityLeaderNode(index, parser.getContextData().get(index), parser.getSocialNetwork()[index],
                dataMemorySize, exchangeHistorySize, seed, parser.getTraceData().getStartTime(),
                parser.getTraceData().getEndTime(), false, nodes, cacheMemorySize, null);
    }
}
