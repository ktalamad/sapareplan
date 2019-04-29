/****************************************************************
    Author: Minh B. Do - Arizona State University
 *****************************************************************/
package edu.asu.sapa.ground.update;

import edu.asu.sapa.ground.FluentDB;
import edu.asu.sapa.ground.GMathForm;
import edu.asu.sapa.ground.State;

public class Test implements Update<Test> {
	public int op;
	public GMathForm leftSide; // Ground Function ID of the left side
	public GMathForm rightSide;
	public boolean isConstant;
	public boolean value;
	public GMathForm time;

	// public Test() {
	// op = 0;
	// leftSide = null;
	// rightSide = null;
	// }

	public Test(int c, GMathForm left, GMathForm right, GMathForm time) {
		op = c;
		leftSide = left;
		rightSide = right;
		this.time = time;
	}

	public boolean analyzeStatic(float dur) {
		isConstant = false;
		if (time != null)
			if (time.analyzeStatic(dur)) {
				if (time.type == 5)
					time = new GMathForm(dur);
			}
		boolean isConstant = true;
		isConstant &= leftSide.analyzeStatic(dur);
		isConstant &= rightSide.analyzeStatic(dur);
		if (isConstant) {
			value = holds(null);
			return value;
		}
		return true;
	}

	/** Set the Comparator: "==" : 0; "<" : 1; "<=" : 2; ">" : 3; ">=" : 4; */
	public void setComparator(int i) {
		op = i;
	}

	public int getComparator() {
		return op;
	}

	public Test bind(float dur) {
		if (dur != dur)
			return this;
		GMathForm l = leftSide.bind(dur);
		GMathForm r = rightSide.bind(dur);
		if (l != leftSide || r != rightSide)
			return new Test(op, l, r, null);
		return this;
	}

	public boolean update(State s) {
		FluentDB fluentDB = s.fluentDB;
		switch (op) {
		case 0:
			return leftSide.value(fluentDB) == rightSide.value(fluentDB);
		case 1:
			return leftSide.value(fluentDB) < rightSide.value(fluentDB);
		case 2:
			return leftSide.value(fluentDB) <= rightSide.value(fluentDB);
		case 3:
			return leftSide.value(fluentDB) > rightSide.value(fluentDB);
		case 4:
			return leftSide.value(fluentDB) >= rightSide.value(fluentDB);
		default:
			return false;
		}
	}

	public boolean update(State s, float dur) {
		if (isConstant)
			return value;
		float t = time.value(s, dur);
		if (t <= 0)
			return this.bind(dur).update(s);
		s.tests.add(this.bind(dur), s.time + t);
		return true;
	}

	public boolean holds(FluentDB fluentDB) {
		switch (op) {
		case 0:
			return leftSide.value(fluentDB) == rightSide.value(fluentDB);
		case 1:
			return leftSide.value(fluentDB) < rightSide.value(fluentDB);
		case 2:
			return leftSide.value(fluentDB) <= rightSide.value(fluentDB);
		case 3:
			return leftSide.value(fluentDB) > rightSide.value(fluentDB);
		case 4:
			return leftSide.value(fluentDB) >= rightSide.value(fluentDB);
		default:
			return false;
		}
	}

	/** Function to do short printing */
	public String toString() {
		String s = "(at " + time + " (";

		s += op + " " + leftSide + " " + rightSide + "))";

		return s;
	}

	public int compareTo(Test o) {
		// assert this != null && o != null;
		// constants should go first if they can be false;
		// otherwise they should go last.
		if (isConstant) {
			if (o.isConstant) {
				if (value) {
					if (o.value)
						return 0;
					return 1;
				}
				if (o.value)
					return -1;
				return 0;
			}
			if (value)
				return 1;
			return -1;
		}
		if (o.isConstant) {
			if (o.value)
				return -1;
			return 1;
		}
		return 0;
	}
}