package edu.asu.sapa.ground.update;

import edu.asu.sapa.ground.GMathForm;
import edu.asu.sapa.ground.State;

public class ProtectTest extends Test {

	public ProtectTest(int c, GMathForm left, GMathForm right) {
		super(c, left, right, null);
	}

	public String toString() {
		String s = "(over all (";

		s += op + " " + leftSide + " " + rightSide + "))";

		return s;
	}

	public boolean update(State s, float dur) {
		if (isConstant)
			return value;
		// float t = time.value(s, dur);
		// if (t <= 0)
		// return this.bind(dur).update(s);
		s.proFluents.add(this.bind(dur), s.time + dur);
		return true;
	}

}
