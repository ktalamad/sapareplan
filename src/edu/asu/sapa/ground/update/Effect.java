package edu.asu.sapa.ground.update;

import edu.asu.sapa.Planner;
import edu.asu.sapa.ground.GMathForm;
import edu.asu.sapa.ground.Proposition;
import edu.asu.sapa.ground.State;

public class Effect implements Update<Effect> {
	public int id;
	public boolean value;
	public GMathForm time;

	public boolean isConstant = false;

	public Effect(int id, boolean value, GMathForm time) {
		this.id = id;
		this.value = value;
		this.time = time;
	}

	public boolean update(State s) {
		if (value)
			s.propDB.add(id, s.time);
		else
			s.propDB.delete(id, s.time);
		return true;
	}

	public int compareTo(Effect o) {
		//assert this != null && o != null;
		if (id < o.id)
			return -1;
		if (id > o.id)
			return 1;
		if (value != o.value)
			if (!value)
				return -1;
			else
				return 1;
		return 0;
	}

	public boolean update(State s, float dur) {
		if (isConstant)
			return value;
		float t = time.value(s, dur);
		if (t <= 0)
			return update(s);
		s.events.add(this, s.time + t);
		return true;
	}

	public String toString() {
		return "(at " + time + "(:= " + id + " " + value + "))";
	}

	public void analyzeStatic(float dur) {
		isConstant = false;
		if (time.analyzeStatic(dur)) {
			if (time.type == 5)
				time = new GMathForm(dur);
		}

		Proposition p = Planner.grounding.propositions.get(id);
		if (p.isConstant) {
			isConstant = true;
			value = (value == p.value);
			assert value == true;
		}

	}

}
