/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package mobemu;

import java.util.*;
import mobemu.algorithms.Epidemic;
import mobemu.algorithms.SPRINT;
import mobemu.node.Message;
import mobemu.node.Node;
import mobemu.node.Stats;
import mobemu.node.leader.communityBasedLeaderElection.CommunityLeaderNode;
import mobemu.node.leader.directLeaderElection.DirectLeaderElectionNode;
import mobemu.parsers.HCMM;
import mobemu.parsers.Sigcomm;
import mobemu.runAlgorithms.IRun;
import mobemu.runAlgorithms.consensus.RunConsensus;
import mobemu.runAlgorithms.leader.RunCommunityLeader;
import mobemu.runAlgorithms.leader.RunDirectLeader;
import mobemu.trace.Parser;
import mobemu.utils.Constants;

import static mobemu.utils.Constants.*;

/**
 * Main class for MobEmu.
 *
 * @author Radu
 */
public class MobEmu {

    public static void main(String[] args) {
        IRun algorithm = new RunConsensus(args);

        algorithm.run();
    }
}
