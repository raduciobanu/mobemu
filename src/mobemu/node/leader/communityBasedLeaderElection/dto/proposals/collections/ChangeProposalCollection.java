package mobemu.node.leader.communityBasedLeaderElection.dto.proposals.collections;

import mobemu.node.leader.communityBasedLeaderElection.dto.proposals.ChangeProposal;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by radu on 2/6/2017.
 */
public abstract class ChangeProposalCollection <T extends ChangeProposal>{

    protected List<T> proposals;

    public ChangeProposalCollection(){
        proposals = new ArrayList<>();
    }

    public void add(T proposalToAdd){
        T sameProposal = getSameProposal(proposalToAdd);
        if(sameProposal != null){
            if(sameProposal.getTimestamp() < proposalToAdd.getTimestamp()){
                proposals.remove(sameProposal);
                proposals.add(proposalToAdd);
            }
            return;
        }
        proposals.add(proposalToAdd);
    }

    private T getSameProposal(T elementToAdd){
        for(T proposal: proposals){
            if(elementToAdd.getSourceId() == proposal.getSourceId()
                    && elementToAdd.getTargetId() == proposal.getTargetId())
                return proposal;
        }

        return null;
    }

//    public List<Integer> getTargetIds(){
//        List<Integer> targetIds = new ArrayList<>();
//        for(T proposal: proposals){
//            int targetId = proposal.getTargetId();
//            if(!targetIds.contains(targetId)){
//                targetIds.add(targetId);
//            }
//        }
//
//        return targetIds;
//    }

    public boolean contains(T proposal){
        T sameProposal = getSameProposal(proposal);

        return sameProposal != null && sameProposal.getTimestamp() == proposal.getTimestamp();
    }

    public int size(){
        return proposals.size();
    }
}
