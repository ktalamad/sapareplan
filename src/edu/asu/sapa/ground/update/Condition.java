package edu.asu.sapa.ground.update;

import edu.asu.sapa.Planner;
import edu.asu.sapa.ground.GMathForm;
import edu.asu.sapa.ground.Proposition;
import edu.asu.sapa.ground.State;

public class Condition implements Update<Condition> {

	public int id;
	public boolean value;
	public GMathForm time;
	public boolean isConstant;
	
	public final static Condition bottom = new Condition();
	
	private Condition() {
		id = 0;
		value = false;
		time = GMathForm.NaN;
		isConstant = true;
	}

	public Condition(int id, boolean value, GMathForm time) {
		this.id = id;
		this.value = value;
		this.time = time;
	}

	public boolean update(State s) {
		if (s.propDB.contains(id) == value || (isConstant && value))
			return true;
		return false;
	}

	// yes, it is a repeat, but update() implies that things might change
	// i.e., i might implement a holds() interface to complement the update()
	// (or maybe check()?)
	public boolean holds(State s) {
		if (s.propDB.contains(id) == value || (isConstant && value))
			return true;
		return false;
	}

	public boolean update(State s, float dur) {
		if (isConstant)
			return value;
		float t = time.value(s, dur);
		if (t <= 0)
			return s.propDB.contains(id) == value;
		s.conditions.add(this, s.time + t);
		return true;
	}

	public int compareTo(Condition o) {
		// assert this != null && o != null;
		if (isConstant || o.isConstant) {
			if (isConstant && o.isConstant) {
				if (!value && o.value)
					return -1;
				if (value && !o.value)
					return 1;
				return 0;
			}
			if (isConstant) {
				if (!value)
					return -1;
				return 1;
			}
			if (!o.value)
				return 1;
			return -1;
		}
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

	public String toString() {
		return "(at " + time + " (== " + id + " " + value + "))";
	}

	// conditions return whether or not they are conceivably true, 
	// which is a) both dynamic conditions, and b) some static conditions
	public boolean analyzeStatic(float dur) {
		isConstant = false;
		if (time.analyzeStatic(dur)) {
			if (time.type == 5)
				time = new GMathForm(dur);
		}
		Proposition p = Planner.grounding.propositions.get(id);
		if (p.isConstant) {
			isConstant = true;
			value = (value == p.value);
			return value;
		}
		return true;
	}
}
