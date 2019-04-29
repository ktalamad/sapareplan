/*
 * Created on Jun 22, 2004
 */
package edu.asu.sapa.lifted;


/**
 * @author Minh
 *
 */
public class LiftedGoal {
	public Predicate proposition;
	public float deadline=Float.POSITIVE_INFINITY;
	public boolean gIsHard=true;
	public boolean guIsConstant=true;
	public float guConstant=0;
	public MathForm guDynamic=null;
	public float gpConstant=0;
	// gpDynamic unsupported in non-lifted form
	public MathForm gpDynamic=null;
	
	public LiftedGoal(Predicate id, float gdl, boolean gt, boolean gu, float su, MathForm du, float sp, MathForm dp) {
		proposition = id;
		deadline = gdl;
		gIsHard = gt;
		guIsConstant = gu;
		guConstant = su;
		guDynamic = du;
		gpConstant = sp;
		gpDynamic = dp;
	}
	
	public LiftedGoal(LiftedGoal lg, Predicate p) {
		proposition = p;
		deadline = lg.deadline;
		gIsHard = lg.gIsHard;
		guIsConstant = lg.guIsConstant;
		guConstant = lg.guConstant;
		guDynamic = lg.guDynamic;
		gpConstant = lg.gpConstant;
		gpDynamic = lg.gpDynamic;
	}
	
	public LiftedGoal(Predicate id, float gdl) {
		proposition = id;
		deadline = gdl;
	}
	
	public LiftedGoal(LiftedGoal lg) {
		proposition = lg.proposition;
		deadline = lg.deadline;
		gIsHard = lg.gIsHard;
		guIsConstant = lg.guIsConstant;
		guConstant = lg.guConstant;
		guDynamic = lg.guDynamic;
		gpConstant = lg.gpConstant;
		gpDynamic = lg.gpDynamic;
	}
	
	public int hashCode() {
		return toString().hashCode();
	}
	
	public boolean equals(Object o) {
		return ((LiftedGoal)o).toString().equals(toString());
	}
	
	public String toString() {
		String s = new String();
		s += proposition;
		s += " " + deadline;
		if(gIsHard)
			s += " hard";
		else
			s += " soft";
		if(guIsConstant)
			s += " " +guConstant;
		else
			s += " " + guDynamic;
				
		return s;
	}
}
