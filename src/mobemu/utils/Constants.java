package mobemu.utils;

/**
 * Created by radu on 1/12/2017.
 */
public class Constants {

    public static int dataMemorySize = 1000;
    public static int exchangeHistorySize = 100;
    public static int cacheMemorySize = 40;

    public static int communityMaxHop = 2;
    public static double heartBeatGenerationTime = 1000* 60*60*0.5;
    public static double leaderCommunityThreshold = 0.5;
    public static double leaderProposalsThreshold = 0.5;
    public static String responseTimesFileName = "responseTimes.txt";
    public static String deliveryLatenciesFileName = "deliveryLatency.txt";
    public static int numberOfGeneratedMessages = 4;


    public static int MIN_REPLICAS = 3;
    public static double MIN_CONFIDENCE_LEVEL = 0.6;
    public static double Epsilon = 0.001;
    public static double MalavolenceConstant = 0.2;
    public static double centralityWeight = 0.2;
    public static double trustWeight = 0.2;
    public static double probabilityWeight = 0.6;
    public static double latencyWeight = 0.2;

}
