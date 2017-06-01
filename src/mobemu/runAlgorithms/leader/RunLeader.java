package mobemu.runAlgorithms.leader;

import mobemu.parsers.HCMM;
import mobemu.runAlgorithms.RunNode;
import mobemu.statistics.LeaderStatistics;
import mobemu.trace.Parser;

/**
 * Created by radu on 5/12/2017.
 */
public abstract class RunLeader extends RunNode{

    public RunLeader(String[] args) {
        super(args);

        statistics = new LeaderStatistics();
    }

}
