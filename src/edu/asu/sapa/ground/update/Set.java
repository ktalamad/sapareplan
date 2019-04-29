/*
   Author: Minh B. Do (Arizona State University)
 */
package edu.asu.sapa.ground.update;

import edu.asu.sapa.Planner;
import edu.asu.sapa.ground.Fluent;
import edu.asu.sapa.ground.FluentDB;
import edu.asu.sapa.ground.GMathForm;
import edu.asu.sapa.ground.State;

/**
 * GMySet: Class to store the grounded "Set" structure, which is an action
 * effect related to changing the value of a continuous function. The "Set"
 * contains of 3 parts, the function in the left hand side, the assigment
 * indication (increase/decrease etc.) and the GMathForm structure as the right
 * hand side
 * 
 * @author Minh B. Do
 * @author J. Daniel Benton
 */
public class Set implements Update<Set> {
	public int op; // "=" : 0; "-=" : 1; "+=" : 2; "*=" : 3; "/=" : 4;
	public int id; // Ground func ID of left side
	public GMathForm rightSide;
	public GMathForm time;

	private boolean isConstant;
	private boolean value;

	// public Set() {
	// id = 0;
	// op = 0;
	// rightSide = null;
	// }

	public Set(int op, int id, GMathForm right, GMathForm time) {
		this.id = id;
		this.op = op;
		rightSide = right;
		this.time = time;
	}

	public Set bind(float dur) {
		if (dur != dur)
			return this;
		GMathForm r = rightSide.bind(dur);
		if (r != rightSide)
			return new Set(op, id, r, null);
		return this;
	}

	public boolean update(State s) {
		FluentDB fluentDB = s.fluentDB;
		float value = rightSide.value(fluentDB);
		if (op == 0) {
			if (value != value)
				fluentDB.remove(id);
			else
				fluentDB.put(id, value);
			return true;
		}

		float v = fluentDB.get(id);
		if (v != v)
			return false;
		switch (op) {
		case 1:
			v += value;
			break;
		case 2:
			v -= value;
			break;
		case 3:
			v *= value;
			break;
		case 4:
			v /= value;
			break;
		default:
			v = Float.NaN;
		}

		fluentDB.put(id, v);
		return true;
	}

	public boolean update(State s, float dur) {
		if (isConstant)
			return value;
		float t = time.value(s, dur);
		if (t <= 0)
			return this.bind(dur).update(s);
		s.sets.add(this.bind(dur), s.time + t);
		return true;
	}

	public void analyzeStatic(float dur) {
		isConstant = false;
		rightSide.analyzeStatic(dur);
		if (time != null) // time being null ought to be impossible
			if (time.analyzeStatic(dur)) {
				if (time.type == 5)
					time = new GMathForm(dur);
			}
		Fluent f = Planner.grounding.fluents.get(id);
		if (f.isConstant) {
			isConstant = true;
			value = (rightSide.getValue() == f.value);
			assert value == true && op == 0;
		}
	}

	public void setLeftSide(int id) {
		this.id = id;
	}

	public int getLeftSide() {
		return id;
	}

	public int getOp() {
		return op;
	}

	public void setRightSide(GMathForm m) {
		rightSide = m;
	}

	public GMathForm getRightSide() {
		return rightSide;
	}

	public String toString() {
		String s = "(at " + time + " (" + op + " ";

		s += id + " " + rightSide + "))";

		return s;
	}

	public int compareTo(Set o) {
		// assert this != null && o != null;
		if (id < o.id)
			return -1;
		if (id > o.id)
			return 1;
		// groups sets
		// (assignments | linear | geometric)
		if (op < o.op)
			return -1;
		if (op > o.op)
			return 1;
		return 0;
	}

}
