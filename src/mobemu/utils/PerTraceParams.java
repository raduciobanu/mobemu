/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package mobemu.utils;

import java.util.HashMap;
import java.util.Map;

/**
 * Helper class for some trace-specific parameters.
 *
 * @author Radu
 */
public class PerTraceParams {

    int commonInterests; // ONSIDE
    int interestedFriendsThreshold; // ONSIDE
    double encounteredInterestsThreshold; // ONSIDE
    double socialNetworkThreshold; // Interest Space
    double interestThreshold; // Interest Space
    int contactsThreshold; // Interest Spaces
    double aggregationW1; // Interest Spaces
    double aggregationW2; // Interest Spaces
    double aggregationW3; // Interest Spaces
    double aggregationW4; // Interest Spaces
    double cacheW1; // Interest Spaces
    double cacheW2; // Interest Spaces
    double cacheW3; // Interest Spaces
    long timeWindow; // Interest Spaces

    /**
     * Instantiates a {@code PerTraceParams} object.
     *
     * @param commonInterests number of common interests
     * @param interestedFriendsThreshold threshold for the number of interested
     * friends
     * @param encounteredInterestsThreshold threshold for percentage of
     * encountered interests
     * @param socialNetworkThreshold social network threshold
     * @param interestThreshold interest threshold
     * @param contactsThreshold contact threshold
     * @param aggregationW1 aggregation weight for the node similarity component
     * @param aggregationW2 aggregation weight for the node friendship component
     * @param aggregationW3 aggregation weight for the node connectivity
     * component
     * @param aggregationW4 aggregation weight for the node contacts component
     * @param cacheW1 cache weight for the interested nodes ratio component
     * @param cacheW2 cache weight for the interests encountered ratio component
     * @param cacheW3 cache weight for the interested friends ratio component
     * @param timeWindow time window for Interest Spaces
     */
    public PerTraceParams(int commonInterests, int interestedFriendsThreshold,
            double encounteredInterestsThreshold, double socialNetworkThreshold,
            double interestThreshold, int contactsThreshold, double aggregationW1,
            double aggregationW2, double aggregationW3, double aggregationW4,
            double cacheW1, double cacheW2, double cacheW3, long timeWindow) {
        this.commonInterests = commonInterests;
        this.interestedFriendsThreshold = interestedFriendsThreshold;
        this.encounteredInterestsThreshold = encounteredInterestsThreshold;
        this.socialNetworkThreshold = socialNetworkThreshold;
        this.interestThreshold = interestThreshold;
        this.contactsThreshold = contactsThreshold;
        this.aggregationW1 = aggregationW1;
        this.aggregationW2 = aggregationW2;
        this.aggregationW3 = aggregationW3;
        this.aggregationW4 = aggregationW4;
        this.cacheW1 = cacheW1;
        this.cacheW2 = cacheW2;
        this.cacheW3 = cacheW3;
        this.timeWindow = timeWindow;
    }

    /**
     * Gets the per-trace parameters for a given trace.
     *
     * @param trace name of the trace
     * @return parameters for the specified trace
     */
    public static PerTraceParams getPerTraceParams(String trace) {
        Map<String, PerTraceParams> perTraceParams = new HashMap<>();
        perTraceParams.put("Sigcomm", new PerTraceParams(1, 1, 1.0, 0.95, 0.98, 30, 0.25, 0.25, 0.25, 0.25, 0.2, 0.4, 0.4, 3600 * 1000));
        perTraceParams.put("UPB 2012", new PerTraceParams(1, 5, 0.2, 0.5, 0.1, 50, 0.25, 0.25, 0.25, 0.25, 0.2, 0.4, 0.4, 12 * 3600 * 1000));
        perTraceParams.put("Haggle Infocom 2006", new PerTraceParams(1, 0, 0.3, 1, 0.5, 0, 0.25, 0.25, 0.25, 0.25, 0.34, 0.66, 0.0, 360 * 1000));
        perTraceParams.put("SocialBlueConn", new PerTraceParams(1, 3, 0.32, 0.7, 0.2, 30, 0.25, 0.25, 0.25, 0.25, 0.2, 0.4, 0.4, 1800 * 1000));

        return perTraceParams.get(trace);
    }
}
