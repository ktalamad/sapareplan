
package edu.asu.sapa.lifted;

import java.util.ArrayList;

import edu.asu.sapa.ground.GoalDep;

/**
 * This class represent the problem structure that we parsed from the problem
 * file in PDDL2.1
 */
public class Problem extends Domain {
	public String name; // Name of the problem (super.name for the domain name)
	private String domainLink; // the intended domain

	public Action initAction = new Action("");
	public Predicate initPredicate;
	public Function initFunction;

	protected ArrayList<LiftedGoal> liftedGoals;

	public ArrayList<GoalDep> goalDeps;
	
	public Problem() {
		ArrayList<Symbol<String>> empty = new ArrayList<Symbol<String>>();
		liftedGoals = new ArrayList<LiftedGoal>();
		goalDeps = new ArrayList<GoalDep>();

		initPredicate = new Predicate("",empty);
		initFunction = new Function("",empty);
		
		putAction(initAction);
		putPredicate(initPredicate);
		putFunction(initFunction);

		initAction.setDuration(MathForm.zero);
		initAction.setCost(MathForm.zero);
	}

	/** Set the name of this problem */
	public void setName(String n) {
		name = n;
	}

	public void setDomainName(String n) {
		super.name = n;
	}

	public void setDomainLink(String n) {
		domainLink = n;
		if (!domainLink.equalsIgnoreCase(super.name))
			System.err.println("Problem " + name + " not meant for domain "
					+ super.name + '.');
	}
	
	public void setCurrentTime (float now) {
		initAction.d_dynamic = new MathForm(now);
	}

	public String getName() {
		return name;
	}

	public void addInitialAdd(Predicate pred) {
		//System.out.println("Called addInitialAdd(pred): " + pred);
		//initAction.putAdd(pred,MathForm.dur);
		initAction.putAdd(pred,initAction.d_dynamic);
	}

	public void addInitialDelete(Predicate p) {
		//System.out.println("Called addInitialDelete(pred): " + p);
		//initAction.putDelete(p, MathForm.dur);
		initAction.putDelete(p,initAction.d_dynamic);
	}

	public void addInitialSet(Function func, MathForm value) {
		//initAction.putSet(new LiftedSet("=", func, value),MathForm.dur);
		initAction.putSet(new LiftedSet("=", func, value),initAction.d_dynamic);
	}
	
	public void addInitialSet(LiftedSet s, MathForm time) {
		initAction.putSet(s, time);
	}

	public void addInitialSet(Function func, MathForm value, MathForm time) {
		initAction.putSet(new LiftedSet("=", func, value), time);
	}

	public void addInitialAdd(Predicate p, MathForm time) {
		//System.out.println("Called addInitialAdd(pred,time): " + p + " at" + time);
		initAction.putAdd(p, time);
	}

	public void addInitialDelete(Predicate p, MathForm time) {
		//System.out.println("Called addInitialDelete(pred,time): " + p + " at" + time);
		initAction.putDelete(p, time);
	}

	public void addGoal(LiftedGoal g) {
		liftedGoals.add(g);
	}

	public void addGoalDep(GoalDep gd) {
		goalDeps.add(gd);
	}

	public int numGoal() {
		return liftedGoals.size();
	}

	public LiftedGoal getGoal(int index) {
		return liftedGoals.get(index);
	}

	public ArrayList<LiftedGoal> getAllliftedGoals() {
		return liftedGoals;
	}

	public int maxGDSize() {
		int max = 0, temp;
		for (int i = 0; i < goalDeps.size(); i++) {
			temp = (goalDeps.get(i)).size();
			if (max < temp)
				max = temp;
		}
		return max;
	}
	
	public void clearInit() {
		initAction.clearLists();
	}

	@Override
	public String toString() {
		return name;
	}

}
