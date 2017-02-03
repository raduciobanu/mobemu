package mobemu.utils;

/**
 * Created by radu on 1/12/2017.
 */
public class Constants {

    public static int dataMemorySize = 1000;
    public static int exchangeHistorySize = 100;
    public static int cacheMemorySize = 40;

    public static int communityMaxHop = 2;
    public static int heartBeatGenerationTime = 1000* 60*60*3;
    public static double leaderCommunityThreshold = 0.5;
    public static double leaderProposalsThreshold = 0.5;
    public static String responseTimesFileName = "responseTimes.txt";

    public static double centralityWeight = 0.2;
    public static double trustWeight = 0.2;
    public static double probabilityWeight = 0.6;
    public static double latencyWeight = 0.2;
}