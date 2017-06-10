package mobemu.node.consensus;

import mobemu.node.Message;

import java.util.*;

/**
 * Created by radu on 5/27/2017.
 */
public class DecisionRequestsAndResponses {

    /**
     * Dictionary of decision requests made (MessageId, DecisionRequest)
     * Used because the leader might change several times before the end of the trace
     * And we are interested if the request has been answered by the leader which the request was issued for
     */
    protected Map<Integer, Message> requests;
    /**
     * Dictionary of decision responses (MessageId, DecisionResponse)
     */
    protected Map<Integer, DecisionResponse> responses;

    public DecisionRequestsAndResponses(){
        requests = new HashMap<>();
        responses = new HashMap<>();
    }

    public void addRequest(Message message){
        requests.put(message.getId(), message);
    }

    public boolean requestsContainKey(int messageId){
        return requests.containsKey(messageId);
    }

    public void addResponse(ConsensusDecision decision, long currentTime){
        //if there is already a response for the decision
        DecisionResponse responseForMessageId = getResponse(decision.getId());
        if(responseForMessageId != null && responseForMessageId.getTimestamp() > decision.getTimestamp())
            return;

        //if there is no request for the decision

        Message request = getRequestForDecision(decision);
        if(request == null){
            return;
        }

        long spentTime = currentTime - request.getTimestamp();
        responses.put(decision.getId(), new DecisionResponse(decision.getSourceId(), decision.getValue(), currentTime, spentTime));
    }

    public DecisionResponse getResponseForMessageId(int messageId){
        return responses.get(messageId);
    }

    public DecisionResponse getResponse(int messageId){
        Iterator it = responses.entrySet().iterator();
        while(it.hasNext()){
            Map.Entry<Integer, DecisionResponse> pair = (Map.Entry)it.next();
            if(pair.getKey() == messageId){
                return pair.getValue();
            }
        }

        return null;
    }

    public Message getRequestForDecision(ConsensusDecision decision){
        Iterator it = requests.entrySet().iterator();
        while(it.hasNext()){
            Map.Entry<Integer, Message> pair = (Map.Entry)it.next();
            int messageId = pair.getKey();
            Message request = pair.getValue();
            if(decision.fullMatch(messageId, request.getDestination())){
                return request;
            }
        }

        return null;
    }

    public List<Message> updateUnrepliedRequest(int newLeaderId, long currentTime){
        List<Message> unrepliedRequests = new ArrayList<>();

        Iterator it = requests.entrySet().iterator();
        while (it.hasNext()){
            Map.Entry<Integer, Message> pair = (Map.Entry)it.next();
            int messageId = pair.getKey();
            Message request = pair.getValue();

            if(!responses.containsKey(messageId)){
                Message message = new Message(messageId, request.getSource(), newLeaderId, request.getMessage(), currentTime,
                        request.getCopies());
                unrepliedRequests.add(message);
                requests.put(messageId, message);
            }
        }

        return unrepliedRequests;
    }
}
