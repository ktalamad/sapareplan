package edu.asu.sapa.ground.update;

import edu.asu.sapa.Planner;
import edu.asu.sapa.ground.Proposition;
import edu.asu.sapa.ground.State;

public class ProtectCondition extends Condition {

	public ProtectCondition(int id, boolean value) {
		super(id, value, null);
	}

	public String toString() {
		return "(over all " + "(== " + id + " " + value + "))";
	}

	/**
	 * warning: dur is actually the end time, and this.time means nothing
	 * 
	 * (should extend to protection intervals over sub-intervals)
	 * 
	 * @see edu.asu.sapa.ground.update.Condition#update(edu.asu.sapa.ground.State,
	 *      float)
	 */
	public boolean update(State s, float dur) {
		if (isConstant)
			return value;
		// float t = time.value(s, dur);
		// if (t <= 0)
		// return s.propDB.contains(id) == value;
		s.proProps.add(this, dur);
		return true;
	}

	public boolean analyzeStatic(float dur) {
		isConstant = false;
		Proposition p = Planner.grounding.propositions.get(id);
		if (p.isConstant) {
			isConstant = true;
			value = (value == p.value);
			return value;
		}
		return true;
	}

}
