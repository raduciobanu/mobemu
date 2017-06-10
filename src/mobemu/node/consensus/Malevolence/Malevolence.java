package mobemu.node.consensus.Malevolence;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Created by radu.dragan on 6/5/2017.
 */
public class Malevolence {
    /**
     * Opinions from the point of view of the current node
     * Dictionary of Node Id and opinions towards that node
     */
    private Map<Integer, MalevolenceOpinionListTowardsNode> ownOpinions;

    /**
     * All opinions gathered by the current node
     * Dictionary of Node Id and list of opinions towards that node
     */
    public Map<Integer, MalevolenceOpinionList> allOpinions;

    public int currentNodeId;

    public Malevolence(int currentNodeId) {
        ownOpinions = new HashMap<>();
        allOpinions = new HashMap<>();

        this.currentNodeId = currentNodeId;
    }

    /**
     * Get the malevolence value for the specified node
     *
     * @param nodeId the id of the node
     * @return the malevolence value
     */
    public double getValue(int nodeId) {
        if (!allOpinions.containsKey(nodeId))
            return 1d;

        return allOpinions.get(nodeId).computeAverage();
    }

    public void setOwnOpinion(int nodeId, int messageId, double value, long currentTime) {
        if(!ownOpinions.containsKey(nodeId)){
            ownOpinions.put(nodeId, new MalevolenceOpinionListTowardsNode());
        }

        MalevolenceOpinionListTowardsNode opinionsTowardsNode = ownOpinions.get(nodeId);
        opinionsTowardsNode.add(new MalevolenceOpinionTowardsNode(messageId, value, currentTime));

        double averageOpinion = getAverageOpinionForNode(nodeId);
        updateAllOpinions(nodeId, averageOpinion, currentTime);
    }

    private double getAverageOpinionForNode(int nodeId){
        return ownOpinions.get(nodeId).getAverageOpinion();
    }

    public void updateAllOpinions(int nodeId, double value, long currentTime){
        if(!allOpinions.containsKey(nodeId)){
            allOpinions.put(nodeId, new MalevolenceOpinionList());
        }

        allOpinions.get(nodeId).add(new MalevolenceOpinion(currentNodeId, value, currentTime));
    }

    private void addOpinion(int nodeId, MalevolenceOpinion opinion){
        if(!allOpinions.containsKey(nodeId)){
            allOpinions.put(nodeId, new MalevolenceOpinionList());
        }

        allOpinions.get(nodeId).add(opinion);
    }

    public void exchange(Malevolence malevolence){
        for (Map.Entry<Integer, MalevolenceOpinionList> pair : malevolence.allOpinions.entrySet()){
            int nodeId = pair.getKey();
            MalevolenceOpinionList opinionsForNode = pair.getValue();

            Iterator<MalevolenceOpinion> iterator = opinionsForNode.iterator();
            while (iterator.hasNext()){
                addOpinion(nodeId, iterator.next());
            }
        }
    }
}
