/****************************************************************************
 Author: Minh B. Do (Arizona State Univ. - binhminh@asu.edu)
 *****************************************************************************/
package edu.asu.sapa.ground;

import java.util.ArrayList;
import java.util.Collection;

import edu.asu.sapa.ConditionQ;
import edu.asu.sapa.EffectQ;
import edu.asu.sapa.Planner;
import edu.asu.sapa.Priority;
import edu.asu.sapa.PriorityQueue;
import edu.asu.sapa.SetQ;
import edu.asu.sapa.TestQ;
import edu.asu.sapa.ground.update.Condition;
import edu.asu.sapa.ground.update.Effect;
import edu.asu.sapa.ground.update.ProtectCondition;
import edu.asu.sapa.ground.update.ProtectTest;
import edu.asu.sapa.ground.update.Set;
import edu.asu.sapa.ground.update.Test;
import edu.asu.sapa.lifted.SymbolTable;

/**
 * State.java: Store the information about the current state such as : (1) Set
 * of predicates that is true & resource information (stored in a GState
 * structure) (2) Set of future events and the occuring time points. (3) Set of
 * actions leading to this state and their execution time. (4) Set of
 * predicates/functions that need to be protected and their end time points. (5)
 * Time at which we measure this state (6) Distance to the goal (heuristically
 * measured) (7) Other information such as total execution costs, or total
 * duration etc.
 */
public class State implements Cloneable, Comparable<State> {
	public PropDB propDB;
	public FluentDB fluentDB;
	public float time;
	// public float distance;
	public float a;
	public float h;
	public float g;

	public ArrayList<Integer> actions;
	public ArrayList<Float> actionTimes;
	public ArrayList<Float> actionDurations;
	public ArrayList<Float> actionCosts;

	public EffectQ events;
	public SetQ sets;

	public ConditionQ conditions;
	public TestQ tests;

	public PriorityQueue<ProtectCondition> proProps;
	public TestQ proFluents;

	public float gCost;
	public float totalReward;
	public float hCost;

	public int[] potentialOperators;

	public boolean solved;

	public State(Operator a) {
		actions = new ArrayList<Integer>();
		actionTimes = new ArrayList<Float>();
		actionDurations = new ArrayList<Float>();
		actionCosts = new ArrayList<Float>();

		fluentDB = new FluentDB();
		propDB = new PropDB();

		events = new EffectQ();
		sets = new SetQ();
		conditions = new ConditionQ();
		tests = new TestQ();

		Priority<ProtectCondition> foo = new Priority<ProtectCondition>();
		proProps = new PriorityQueue<ProtectCondition>(foo);
		proFluents = new TestQ();

		potentialOperators = new int[0];

		float dynamicDur = a.getDuration(fluentDB);
		gCost = a.getCost(fluentDB);
		g=0;
		
		// goal and heuristic stuff set by Planner.evaluate()
		//solved = false;
		//totalReward = 0;
		// distance = 0;
		
		time = 0;

		for (Condition c : a.conditionList) {
			c.update(this, dynamicDur);
		}
		for (Test t : a.testList) {
			t.update(this, dynamicDur);
		}

		for (Set s : a.setList) {
			s.update(this, dynamicDur);
		}

		for (Effect e : a.effectList)
			e.update(this, dynamicDur);

		// no proProps or proFuncs to check

		// protection intervals out to infinity are not incredibly useful;
		// but if we've decided to achieve and maintain a goal, then
		// we could use this facility.
		float t = Float.POSITIVE_INFINITY;
		for (ProtectCondition p : a.protectConditionList) {
			p.update(this, t);
		}
		for (ProtectTest p : a.protectTestList)
			p.update(this, t);

		moveForward(dynamicDur);
		
		this.g = 0;
		this.a = 0;
	}

	public State(State sp) {
		g = sp.g;
		h = sp.h;
		a = sp.a;
		hCost = sp.hCost;
		gCost = sp.gCost;
		totalReward = sp.totalReward;

		// arraylists have silly copy constructors
//		actions = new ArrayList<Integer>(sp.actions);
//		actionTimes = new ArrayList<Float>(sp.actionTimes);
//		actionDurations = new ArrayList<Float>(sp.actionDurations);
//		actionCosts = new ArrayList<Float>(sp.actionCosts);
		actions = (ArrayList<Integer>) (sp.actions.clone());
		actionTimes = (ArrayList<Float>)(sp.actionTimes.clone());
		actionDurations = (ArrayList<Float>)(sp.actionDurations.clone());
		actionCosts = (ArrayList<Float>)(sp.actionCosts.clone());

		propDB = new PropDB(sp.propDB);
		fluentDB = new FluentDB(sp.fluentDB);
		time = sp.time;

		events = new EffectQ(sp.events);
		sets = new SetQ(sp.sets);
		conditions = new ConditionQ(sp.conditions);
		tests = new TestQ(sp.tests);

		proProps = new PriorityQueue<ProtectCondition>(sp.proProps);
		proFluents = new TestQ(sp.proFluents);

		// distance = sp.distance;
		// totalReward = sp.totalReward;

		potentialOperators = sp.potentialOperators;
		solved = sp.solved;
	}

	public void addEvent(Priority<Effect> e) {
		events.add(e);
	}

	private void addProFluents(GMathForm mf, float dur) {
		switch (mf.getType()) {
		case 0:
			Fluent f = Planner.grounding.fluents.get(mf.fluent);
			proFluents.add(new Priority<Test>(f.lock(this), time + dur));
			break;
		case 3:
			addProFluents(mf.getLeft(), dur);
			addProFluents(mf.getRight(), dur);
			break;
		case 1:
		case 2:
		case 5:
		default:
			break;
		}
	}

	public float benefit() {
		return totalReward - gCost;
	}

	private boolean checkProFluents() {
		for (Priority<Test> e : proFluents) {
			if (!e.event.update(this))
				return false;
		}
		return true;
	}

	// Check this after applying action's immediate effects
	// (instant violation doesn't count...)
	// but before adding action's over all conditions
	// (other actions can achieve them) (really!)
	private boolean checkProProps() {
		for (Priority<ProtectCondition> e : proProps) {
			if (!e.event.update(this))
				return false;
		}
		return true;
	}

	@Override
	public Object clone() {
		State s = null;
		try {
			s = (State) super.clone();
		} catch (CloneNotSupportedException e) {
			// assert false;
		}

		s.actions = (ArrayList<Integer>) s.actions.clone();
		s.actionTimes = (ArrayList<Float>) s.actionTimes.clone();
		s.actionDurations = (ArrayList<Float>) s.actionDurations.clone();
		s.actionCosts = (ArrayList<Float>) s.actionCosts.clone();

		s.propDB = (PropDB) s.propDB.clone();
		s.fluentDB = (FluentDB) s.fluentDB.clone();

		s.events = (EffectQ) s.events.clone();
		s.sets = (SetQ) s.sets.clone();
		s.conditions = (ConditionQ) s.conditions.clone();
		s.tests = (TestQ) s.tests.clone();

		s.proProps = (PriorityQueue<ProtectCondition>) s.proProps.clone();
		s.proFluents = (TestQ) s.proFluents.clone();

		// immutable
		// s.potentialOperators = s.potentialOperators.clone();
		return s;
	}

	public int compareTo(State s) {
		// for speed, leave these as assertions instead of ifs
		// assert this != null;
		// assert s != null;
		// if (distance < s.distance)
		// return -1;
		// if (distance > s.distance)
		// return 1;
		return 0;
	}

	public ArrayList<Float> getActionCosts() {
		return actionCosts;
	}

	public ArrayList<Float> getActionDurations() {
		return actionDurations;
	}

	public ArrayList<Integer> getActions() {
		return actions;
	}

	public ArrayList<Float> getActionTimes() {
		return actionTimes;
	}

	public Collection<Priority<Condition>> getConditions() {
		return conditions;
	}

	public float getCost() {
		return gCost;
	}

	public float getDistance() {
		// return distance;
		return 0;
	}

	public Collection<Priority<Effect>> getEvents() {
		return events;
	}

	public FluentDB getFluentDB() {
		return fluentDB;
	}

	public float getNextTime() {
		float nextTime = Float.POSITIVE_INFINITY, tempTime;

		if (events.size() > 0) {
			nextTime = events.peek().priority;
		}

		if (sets.size() > 0) {
			tempTime = sets.peek().priority;
			if (nextTime > tempTime) {
				nextTime = tempTime;
			}
		}

		if (conditions.size() > 0) {
			tempTime = conditions.peek().priority;
			if (nextTime > tempTime) {
				nextTime = tempTime;
			}
		}

		if (tests.size() > 0) {
			tempTime = tests.peek().priority;
			if (nextTime > tempTime) {
				nextTime = tempTime;
			}
		}

		if (proProps.size() > 0) {
			tempTime = proProps.peek().priority;
			if (nextTime > tempTime) {
				nextTime = tempTime;
			}
		}

		if (proFluents.size() > 0) {
			tempTime = proFluents.peek().priority;
			if (nextTime > tempTime) {
				nextTime = tempTime;
			}
		}

		return nextTime;
	}

	public int[] getPotentialActions() {
		return potentialOperators;
	}

	public int[] getPotentialOperators() {
		return potentialOperators;
	}

	public TestQ getProFluents() {
		return proFluents;
	}

	public PropDB getPropDB() {
		return propDB;
	}

	public PriorityQueue<ProtectCondition> getProProps() {
		return proProps;
	}

	/*
	 * private void addProFluents(GTest t, float dur) {
	 * addProFluents(t.leftSide, dur); addProFluents(t.rightSide, dur); }
	 * 
	 * private void addProFluents(GMySet s, float dur) {
	 * //addProFluents(s.getLeftSide(), time); addProFluents(s.rightSide, dur); }
	 */

	public SetQ getSets() {
		return sets;
	}

	public TestQ getTests() {
		return tests;
	}

	public float getTime() {
		return time;
	}

	/***************************************************************************
	 * Function to get the gValue, which equals the total execution costs of
	 * actions in the partial plan. Used to guide the A* search Modified for PSP
	 * (include totalUtility)
	 **************************************************************************/
	public float gValue() {
		return (gCost - totalReward);
	}

	public boolean isSolved() {
		return solved;
	}

	/**
	 * Move forward to the first event's time, and update the current-state
	 * according to earliest events
	 */
	public boolean moveForward() {
		float nextTime = getNextTime();

		if (nextTime == Float.POSITIVE_INFINITY)
			return false;
		time = nextTime;

		if (!processProProps())
			return false;
		if (!processProFluents())
			return false;
		if (!processConditions())
			return false;
		if (!processTests())
			return false;
		if (!processEvents())
			return false;
		if (!processSets())
			return false;
//		++a;
		this.a = 0;
		return true;
	}

	public boolean tryForward() {
		float nextTime = getNextTime();

		if (nextTime >= Float.POSITIVE_INFINITY)
			return true;

		return moveForward(nextTime);
	}

	public boolean tryForward(float next) {
		return moveForward(next);
	}

	public boolean moveForward(float next) {
		float nextTime = getNextTime();

		while (nextTime <= next) {
			time = nextTime;

			if (!processProProps())
				return false;
			if (!processProFluents())
				return false;
			if (!processConditions())
				return false;
			if (!processTests())
				return false;
			if (!processEvents())
				return false;
			if (!processSets())
				return false;
//			++a;
			a = 0;
			
			nextTime = getNextTime();
		}
		time = next;
		
		return true;
	}

	public int numAction() {
		return actions.size();
	}

	private boolean processConditions() {
		Priority<Condition> e = conditions.peek();
		while (e != null && e.priority <= time) {
			if (!conditions.remove().event.update(this))
				return false;
			e = conditions.peek();
		}
		return true;
	}

	private boolean processEvents() {
		Priority<Effect> e = events.peek();
		while (e != null && e.priority <= time) {
			if (!events.remove().event.update(this))
				return false;
			e = events.peek();
		}
		return true;
	}

	private boolean processProFluents() {
		Priority<Test> e = proFluents.peek();
		while (e != null && e.priority <= time) {
			if (!proFluents.remove().event.update(this))
				return false;
			e = proFluents.peek();
		}
		if (!checkProFluents())
			return false;
		return true;
	}

	private boolean processProProps() {
		Priority<ProtectCondition> e = proProps.peek();
		while (e != null && e.priority <= time) {
			if (!proProps.remove().event.update(this))
				return false;
			e = proProps.peek();
		}
		if (!checkProProps())
			return false;
		return true;
	}

	private boolean processSets() {
		Priority<Set> e = sets.peek();
		while (e != null && e.priority <= time) {
			if (!sets.remove().event.update(this))
				return false;
			e = sets.peek();
		}
		return true;
	}

	private boolean processTests() {
		Priority<Test> e = tests.peek();
		while (e != null && e.priority <= time) {
			if (!tests.remove().event.update(this))
				return false;
			e = tests.peek();
		}
		return true;
	}

	public boolean resApplicable(Operator a) {
		if (!fluentDB.applicable(a))
			return false;
		return true;
	}

	public void setDistance(float d) {
		// distance = d;
	}

	public void setFluentDB(FluentDB fluentDB) {
		this.fluentDB = fluentDB;
	}

	public void setPotentialActions(int[] potentialActions) {
		this.potentialOperators = potentialActions;
	}

	public void setPropDB(PropDB propDB) {
		this.propDB = propDB;
	}

	public void setSolved(boolean s) {
		solved = s;
	}

	public void setTime(float time) {
		this.time = time;
	}

	public void setTotalUtility(float util) {
		totalReward = util;
	}

	@Override
	public String toString() {
		return propDB.toString() + fluentDB.toString();
	}
	
	public String printDetailedState () {
		
		String retString = "\nCurrent timestamp: " + this.time + "\n";
		SymbolTable<ArrayList<Integer>, Proposition> propositions = Planner.grounding.propositions;
		ArrayList<Proposition> props = propositions.symbols;
		
		/**
		 * So get the timeMap from the propDB object. Then, for each prop, use that timeMap to cross reference
		 * the ID and then print the name of that prop, along with the time at that time.
		 */
		
		PropDB tempPropDB = this.propDB;
		
		for (Proposition tempProp : props) {
			
			if (tempPropDB.contains(tempProp.id)) {
				retString += "PROP: " + tempProp.id + " at " + tempPropDB.getTime(tempProp.id) + 
						" " + tempProp.getName() + "\n";					
			}
		}
		
		//retString += "\nEvent Queue: " + this.events + "\n";
		
		return retString;
	}

	public float totalExecCost() {
		return gCost;
	}

	public float totalUtility() {
		return totalReward;
	}

	public boolean update(Operator a) {
		float dur;
		float cost;

		// if (!resApplicable(a) return false;

		dur = a.getDuration(fluentDB);
		// if (a.duration.type == 1)
		// return updateConstantDuration(a, dur);

		// don't really need to check at start conditions and tests again
		// planning graph monitors conditions and search code calls
		// resApplicable before copying stuff.
		for (Condition c : a.conditionList) {
			if (!c.update(this, dur)) {
				return false;
			}
		}

		for (Test t : a.testList) {
			if (!t.update(this, dur)) {
				return false;
			}
		}

		boolean possible = true;
		for (int i = a.effectList.length - 1; i >= 0; --i) {
			possible &= a.effectList[i].update(this, dur);
		}
		for (Set s : a.setList) {
			possible &= s.update(this, dur);
		}
		if (!possible)
			return false;

		if (!checkProProps())
			return false;
		if (!checkProFluents())
			return false;

		float t = time + dur;
		for (ProtectCondition p : a.protectConditionList) {
			possible &= p.update(this, t);
		}
		for (ProtectTest p : a.protectTestList) {
			possible &= p.update(this, dur);
		}
		if (!possible)
			return false;

		addProFluents(a.duration, dur);

		cost = a.getCost(fluentDB, dur);
		addProFluents(a.cost, dur);

		actions.add(a.id);
		actionTimes.add(time);
		actionDurations.add(dur);
		actionCosts.add(cost);

		gCost += cost;
		g += 1;
		this.a = 1;

		return true;
	}

	protected boolean updateConstantDuration(Operator a, float dur) {
		float dynamicDur = Float.NaN;

		// don't really need to check at start conditions and tests again
		// planning graph monitors conditions and search code calls
		// resApplicable before copying stuff.
		for (Condition c : a.conditionList) {
			if (!c.update(this, dynamicDur)) {
				return false;
			}
		}
		for (Test t : a.testList) {
			if (!t.update(this, dynamicDur)) {
				return false;
			}
		}
		for (Effect e : a.effectList) {
			if (!e.update(this, dynamicDur)) {
				return false;
			}
		}
		for (Set s : a.setList) {
			if (!s.update(this, dynamicDur)) {
				return false;
			}
		}

		if (!checkProProps())
			return false;
		if (!checkProFluents())
			return false;

		float t = time + dur;
		for (ProtectCondition p : a.protectConditionList) {
			if (!p.update(this, t)) {
				return false;
			}
		}
		for (ProtectTest p : a.protectTestList) {
			if (!p.update(this, dur)) {
				return false;
			}
		}

		float cost = a.getCost(fluentDB);
		addProFluents(a.cost, dur);

		actions.add(a.id);
		actionTimes.add(time);
		actionDurations.add(dur);
		actionCosts.add(cost);

		gCost += cost;

		return true;
	}

	public boolean applicable(Operator a) {
		float dur = a.getDuration(this);

		// the PG should do condition checking for us...
		for (Condition c : a.conditionList) {
			if (c.time.value(fluentDB, dur) <= 0)
				if (!c.holds(this))
					return false;
		}

		// this is technically too strong
		for (Condition c : a.protectConditionList) {
			if (!c.holds(this))
				return false;
		}

		Test test;
		float t;
		for (int i = 0; i < a.testList.length; ++i) {
			test = a.testList[i];
			t = test.time.value(fluentDB, dur);
			if (t <= 0)
				if (!test.holds(fluentDB))
					return false;
		}

		Effect e;
		int id;
		next: for (Priority<ProtectCondition> c : proProps) {
			if (c.event.holds(this))
				continue;
			id = c.event.id;
			for (int j = 0; j < a.deleteIndex; ++j) {
				e = a.effectList[j];
				if (e.id == id) {
					t = a.effectList[j].time.value(fluentDB, dur);
					if (t <= 0)
						continue next;
				}
			}
			return false;
		}

		// should do a similar check for proFluents...

		return true;
	}
}
