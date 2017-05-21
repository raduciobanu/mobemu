package mobemu.runAlgorithms.consensus;

import mobemu.node.Node;
import mobemu.node.consensus.ConsensusLeaderNode;
import mobemu.node.leader.LeaderNode;
import mobemu.node.leader.communityBasedLeaderElection.CommunityLeaderNode;
import mobemu.node.leader.directLeaderElection.DirectLeaderElectionNode;
import mobemu.runAlgorithms.RunNode;
import mobemu.statistics.ConsensusStatistics;

import static mobemu.utils.Constants.cacheMemorySize;
import static mobemu.utils.Constants.dataMemorySize;
import static mobemu.utils.Constants.exchangeHistorySize;

/**
 * Created by radu on 5/13/2017.
 */
public class RunConsensus extends RunNode{

    public RunConsensus(String[] args) {
        super(args);

        statistics = new ConsensusStatistics(Float.parseFloat(args[0]));
    }

    @Override
    public Node initializeNode(int index, long seed, Node[] nodes) {
        LeaderNode leaderNode = getCommunityLeaderNode(index, seed, nodes);

        return new ConsensusLeaderNode(index, parser.getContextData().get(index), parser.getSocialNetwork()[index],
                dataMemorySize, exchangeHistorySize, seed, parser.getTraceData().getStartTime(),
                parser.getTraceData().getEndTime(), false, nodes, cacheMemorySize, leaderNode);
    }

    public DirectLeaderElectionNode getDirectLeaderElectionNode(int index, long seed, Node[] nodes){
        return new DirectLeaderElectionNode(index, parser.getContextData().get(index), parser.getSocialNetwork()[index],
                dataMemorySize, exchangeHistorySize, seed, parser.getTraceData().getStartTime(),
                parser.getTraceData().getEndTime(), false, nodes, cacheMemorySize);
    }

    public CommunityLeaderNode getCommunityLeaderNode(int index, long seed, Node[] nodes){
        return new CommunityLeaderNode(index, parser.getContextData().get(index), parser.getSocialNetwork()[index],
                dataMemorySize, exchangeHistorySize, seed, parser.getTraceData().getStartTime(),
                parser.getTraceData().getEndTime(), false, nodes, cacheMemorySize);
    }
}
