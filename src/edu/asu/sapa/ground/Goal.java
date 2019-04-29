package edu.asu.sapa.ground;

import edu.asu.sapa.Planner;
import edu.asu.sapa.lifted.Symbol;

public class Goal extends Symbol<Integer> {
	public int proposition;
	public float deadline=Float.POSITIVE_INFINITY;
	public boolean isHard=true;
	public boolean rewardIsConstant=true;
	public float rewardConstant=0;
	public float penaltyConstant=0;
	public GMathForm rewardDynamic=null;
	
	public Goal(int gid, boolean gt, float gdl, boolean gu, float su, GMathForm du, float penaltyConstant) {
		super(gid);
		proposition = gid;
		deadline = gdl;
		isHard = gt;
		rewardIsConstant = gu;
		rewardConstant = su;
		rewardDynamic = du;
		this.penaltyConstant = penaltyConstant; 
	}
	
	public Goal(int gid, float gdl) {
		super(gid);
		proposition = gid;
		deadline = gdl;
	}
	
	public Goal(Goal goal) {
		super(goal.id);
		proposition = goal.proposition;
		deadline = goal.deadline;
		isHard = goal.isHard;
		rewardIsConstant = goal.rewardIsConstant;
		rewardConstant = goal.rewardConstant;
		rewardDynamic = goal.rewardDynamic;
		penaltyConstant = goal.penaltyConstant;
	}
	
	public void set(float deadline, boolean isHard,
			boolean rewardIsConstant, float rewardConstant, float penaltyConstant,
			GMathForm rewardDynamic) {
		this.deadline = deadline;
		this.isHard = isHard;
		this.rewardIsConstant = rewardIsConstant;
		this.rewardConstant = rewardConstant;
		this.penaltyConstant = penaltyConstant;
		this.rewardDynamic = rewardDynamic;
	}

	public String toString() {
		String s = new String();
		s += Planner.grounding.propositions.get(proposition).getName() + " ";
		s += proposition;
		s += " " + "Deadline: " + deadline;
		if(isHard)
			s += " hard";
		else
			s += " soft";
		if(rewardIsConstant)
			s += " " + "RewardC: " + rewardConstant;
		else
			s += " " + "Reward: " + rewardDynamic;
		
		s += " " + "Penalty: " + penaltyConstant;
				
		return s;
	}
}
