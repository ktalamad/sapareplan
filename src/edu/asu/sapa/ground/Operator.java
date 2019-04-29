/************************************************************************
   Author: Minh B. Do - Arizona State University
 *************************************************************************/
package edu.asu.sapa.ground;

import java.util.ArrayList;
import java.util.Arrays;

import edu.asu.sapa.Planner;
import edu.asu.sapa.ground.update.Condition;
import edu.asu.sapa.ground.update.Effect;
import edu.asu.sapa.ground.update.ProtectCondition;
import edu.asu.sapa.ground.update.ProtectTest;
import edu.asu.sapa.ground.update.Set;
import edu.asu.sapa.ground.update.Test;
import edu.asu.sapa.lifted.Action;
import edu.asu.sapa.lifted.Symbol;

public class Operator extends Symbol<ArrayList<Integer>> {
	/**
	 * 
	 */
	private static final long serialVersionUID = -1357254502931122107L;

	// should add nCondition stuff, or create a generic light weight array
	// to track current active conditions/effects etc.
	// could also move postgroundaction to here, in order to allow
	// dynamically adding/removing activation of this operator.
	// for that matter, if we wait till after grounding and analysis to do
	// linking then we can rely on this.isPossible,
	// and perform one-time trimming of the arrays
	// (as long as monitor just regrounds everything, which is reasonable
	// at the current speed of grounding)
	private String stringName;

	public GMathForm duration;
	public GMathForm cost;

	public Condition[] conditionList;
	public ProtectCondition[] protectConditionList;
	public Test[] testList;
	public ProtectTest[] protectTestList;
	public Effect[] effectList;
	public Set[] setList;

	public int deleteIndex;

	public boolean isPossible;

	public static final Operator nullOperator = new Operator();
	
	private Operator() {
		super(new ArrayList<Integer>(1));
		name.add(0);
		stringName = "";
		duration = GMathForm.NaN;
		cost = GMathForm.NaN;
		conditionList = new Condition[1];
		conditionList[0] = Condition.bottom;
		protectConditionList = new ProtectCondition[0];
		testList = new Test[0];
		protectTestList = new ProtectTest[0];
		effectList = new Effect[0];
		setList = new Set[0];
		
		isPossible = false;
		deleteIndex = 0;
	}
	
	public Operator(ArrayList<Integer> name, Action template) {
		super(name);

		int s = template.conditionList.size();
		conditionList = new Condition[s];

		s = template.protectConditionList.size();
		protectConditionList = new ProtectCondition[s];

		s = template.testList.size();
		testList = new Test[s];

		s = template.protectTestList.size();
		protectTestList = new ProtectTest[s];

		s = template.deleteList.size() + template.addList.size();
		effectList = new Effect[s];

		s = template.setList.size();
		setList = new Set[s];
	}
	
	public void update(Action template) {
		int s = template.conditionList.size();
		if (s != conditionList.length)
			conditionList = new Condition[s];

		s = template.protectConditionList.size();
		if (s != protectConditionList.length)
			protectConditionList = new ProtectCondition[s];

		s = template.testList.size();
		if (s != testList.length)
			testList = new Test[s];

		s = template.protectTestList.size();
		if (s != protectTestList.length)
			protectTestList = new ProtectTest[s];

		s = template.deleteList.size() + template.addList.size();
		if (s != effectList.length)
			effectList = new Effect[s];

		s = template.setList.size();
		if (s != setList.length)
			setList = new Set[s];
	}

	public Object clone() {
		Operator o = (Operator)super.clone();

		// shallow copies are fine -- if we want to overwrite effects we assign a new reference, not alter the actual effect
		o.conditionList = Arrays.copyOf(conditionList, conditionList.length);
		o.protectConditionList = Arrays.copyOf(protectConditionList, protectConditionList.length);
		o.testList = Arrays.copyOf(testList, testList.length);
		o.protectTestList = Arrays.copyOf(protectTestList, protectTestList.length);
		o.effectList = Arrays.copyOf(effectList, effectList.length);
		o.setList = Arrays.copyOf(setList, setList.length);

		// very shallow copies sufficient
		o.duration = new GMathForm(duration);
		o.cost = new GMathForm(cost);

		return o;
	}

	public boolean analyzeStatic(ArrayList<Proposition> propositions) {
		float dur = Float.NaN;
		isPossible = true;

		if (duration.analyzeStatic(dur)) {
			dur = duration.value;
//			if (dur < Planner.EPSILON) {
////				duration.value = dur = Planner.EPSILON;
//				duration = GMathForm.epsilon;
//			}
			if (dur != dur)
				isPossible = false;
		}
		else {
			// for planning graph analysis; which doesn't guess-timate the duration of dynamic duration actions
			if(duration.value < 1 || duration.value != duration.value)
				duration.value = 1;
		}

		if (cost.analyzeStatic(dur)) {
			if (cost.value != cost.value)
				isPossible = false;
		} else {
			// only for planning graph
			if (cost.value != cost.value) {
				cost.value = 1;
			}
		}

		for (Effect c : effectList) {
			c.analyzeStatic(dur);
		}
		for (Set c : setList) {
			c.analyzeStatic(dur);
		}
		
		for (Condition c : conditionList) {
			isPossible &= c.analyzeStatic(dur);
		}
		for (ProtectCondition c : protectConditionList) {
			isPossible &= c.analyzeStatic(dur);
		}
		for (Test c : testList) {
			isPossible &= c.analyzeStatic(dur);
		}
		for (ProtectTest c : protectTestList) {
			isPossible &= c.analyzeStatic(dur);
		}

		Arrays.sort(conditionList);
		Arrays.sort(protectConditionList);
		Arrays.sort(testList);
		Arrays.sort(protectTestList);

		return isPossible;
	}

	void updateSupporters(ArrayList<Proposition> propositions) {
		for (Condition c : conditionList) {
			propositions.get(c.id).addDependent(id);
		}
		for (Condition c : protectConditionList) {
			propositions.get(c.id).addDependent(id);
		}
	}

	public float getCost(FluentDB mresDB, float dur) {
		return cost.value(mresDB, dur);
	}

	public float getCost(FluentDB mresDB) {
		return cost.value(mresDB);
	}

	public float getCost(State state, float dur) {
		return cost.value(state.fluentDB, dur);
	}

	public float getDuration(FluentDB mresDB) {
		return duration.value(mresDB, Float.POSITIVE_INFINITY);
	}

	public float getDuration(State state) {
		return duration.value(state.fluentDB, Float.POSITIVE_INFINITY);
	}

	public String getName() {
		if (stringName == null) {
			if (name != null && name.size() > 0 && name.get(0) != 0) {
				StringBuilder str = new StringBuilder(50);
				int id = name.get(0);
				str.append('(').append(Planner.problem.actions.get(id).name);
				for (int i = 1, size = name.size(); i < size; ++i) {
					id = name.get(i);
					str.append(' ').append(
							Planner.problem.constants.get(id).name);
				}
				str.setCharAt(0, '(');
				str.append(')');
				stringName = str.toString();
			} else {
				stringName = Planner.problem.actions.get(0).name;
			}
		}
		return stringName;
	}

	public int indexAdd(int id) {
		for (int i = 0; i < effectList.length; ++i) {
			Effect e = effectList[i];
			if (e.id == id && e.value == true)
				return i;
		}
		return -1;
	}

	public int indexCondition(int id, boolean value) {
		for (int i = 0; i < conditionList.length; ++i) {
			Condition e = conditionList[i];
			if (e.id == id && e.value == value)
				return i;
		}
		return -1;
	}

	public int indexDelete(int id) {
		for (int i = 0; i < effectList.length; ++i) {
			Effect e = effectList[i];
			if (e.id == id && e.value == false)
				return i;
		}
		return -1;
	}

	public int indexProtectCondition(int id, boolean value) {
		for (int i = 0; i < protectConditionList.length; ++i) {
			Condition e = protectConditionList[i];
			if (e.id == id && e.value == value)
				return i;
		}
		return -1;
	}

	public int indexSet(int id) {
		for (int i = 0; i < setList.length; ++i) {
			Set e = setList[i];
			if (e.id == id)
				return i;
		}
		return -1;
	}

	public void setCost(float f) {
		cost = new GMathForm(f);
	}

	public void setCost(GMathForm m) {
		cost = m;
	}

	public void setDuration(float f) {
		duration = new GMathForm(f);
	}

	public void setDuration(GMathForm m) {
		duration = m;
	}

	public String toString() {
		String s = new String();
		s += "(:action " + getName() + "\n";
		s += ":duration (= ?duration ";
		// if( d_isConstant)
		// s += d_constant;
		// else
		s += duration;
		s += ")\n";
		s += ":costs ";
		// if( c_isConstant)
		// s += c_constant;
		// else
		s += cost;
		s += "\n";

		s += ":condition (and";
		for (Condition c : conditionList) {
			s += "\t" + c + "\n";
		}
		for (ProtectCondition c : protectConditionList) {
			s += "\t" + c + "\n";
		}
		for (Test c : testList) {
			s += "\t" + c + "\n";
		}
		for (ProtectTest c : protectTestList) {
			s += "\t" + c + "\n";
		}
		s += "\t)\n";

		s += ":effect (and\n";
		for (Effect c : effectList) {
			s += "\t" + c + "\n";
		}
		for (Set c : setList) {
			s += "\t" + c + "\n";
		}
		s += "\t)\n";

		s += ")\n";

		return s;
	}

	public void remove() {
		for (Condition c : conditionList) {
			Planner.grounding.propositions.symbols.get(c.id).remove(this.id);
		}
		for (Condition c : protectConditionList) {
			Planner.grounding.propositions.symbols.get(c.id).remove(this.id);
		}
		// should go forward and kill supporters too
	}
}
