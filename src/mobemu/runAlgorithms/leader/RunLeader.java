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

    private Parser HCMMParser(){
        int nodesNo = 40;
        float days = 1f;
        float size = 1000f;
        int tiles = 100;
        int groups = 4;
        int travelers = 10;
        return new HCMM(nodesNo, (int)(days * 2 * 3600), 30 * 24 * 3600, 1.25f, 1.50f, 0.1f, size, size, tiles, tiles, 10.0, 0.7, groups, travelers, 1.50f, 0.8f, 0);
    }

}
