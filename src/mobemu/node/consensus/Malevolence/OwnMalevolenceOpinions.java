//package mobemu.node.consensus.Malevolence;
//
//import java.util.HashMap;
//import java.util.Map;
//
///**
// * Created by radu.dragan on 6/7/2017.
// */
//public class OwnMalevolenceOpinions {
//    /**
//     * Dictionary of the id of a node and a list of opinions toward that node
//     */
//    Map<Integer, MalevolenceOpinionListTowardsNode> opinions;
//
//    public OwnMalevolenceOpinions(){
//        opinions = new HashMap<>();
//    }
//
//    public void add(int nodeId, int messageId, double value, long currentTime){
//        if(!opinions.containsKey(nodeId)){
//            opinions.put(nodeId, new MalevolenceOpinionListTowardsNode());
//        }
//
//        MalevolenceOpinionListTowardsNode opinionsTowardsNode = opinions.get(nodeId);
//        opinionsTowardsNode.add(new MalevolenceOpinionTowardsNode(messageId, value, currentTime));
//    }
//}
