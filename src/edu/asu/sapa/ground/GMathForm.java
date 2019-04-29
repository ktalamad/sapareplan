/*******************************************************************
    Author: Minh B. Do (Arizona State Univ. - binhminh@asu.edu)
 ********************************************************************/
package edu.asu.sapa.ground;

import edu.asu.sapa.Planner;

public class GMathForm {
	public static final GMathForm zero = new GMathForm(0f);
	public static final GMathForm one = new GMathForm(1f);
	public static final GMathForm dur = new GMathForm();
	public static final GMathForm NaN = new GMathForm(Float.NaN);
	public static final GMathForm epsilon = new GMathForm(Planner.EPSILON);
	public static final GMathForm negativeInfinity = new GMathForm(Float.NEGATIVE_INFINITY);

	public int type; // 0: function, 1: value, 2: time (#t), 3: Non
						// Primititive,
	// 5:duration
	
	// if its static, it has type 1 and a correct value setting.
	// otherwise the planning graph needs a value, and 1 is okay-ish for both
	// durations and costs.
	public float value=1.0f;

	public int fluent;

	public char op; // +, -, *, /

	public GMathForm left;
	public GMathForm right;

	private GMathForm() {
		type = 5;
	}

	public GMathForm(float f) {
		type = 1;
		value = f;
	}

	public GMathForm(int id) {
		type = 0;
		fluent = id;
	}

	public GMathForm(char o, GMathForm l, GMathForm r) {
		type = 3;
		op = o;
		left = l;
		right = r;
	}

	public GMathForm(GMathForm g) {
		type = g.getType();
		value = g.getValue();
		fluent = g.getElement();
		op = g.getOperator();

		if (type == 3) {
			// left = new GMathForm(g.getLeft());
			// right = new GMathForm(g.getRight());
			left = g.getLeft();
			right = g.getRight();
		}
	}

	/**
	 * Set the "Type" of this math formula. 0: function, 1: float value, 2: #t
	 * or intermediate time point (not used for level 3, PDDL21), 3: Non
	 * primitive or complex math formula
	 */
	public GMathForm setType(int b) {
		type = b;
		return this;
	}

	public int getType() {
		return type;
	}

	public void setValue(float f) {
		type = 1;
		value = f;
	}

	public float getValue() {
		if (type == 5)
			return Float.POSITIVE_INFINITY;
		if (type != 1)
			return Float.NaN;
		return value;
	}

	public float getValue(GMathForm dur) {
		if (type == 5) {
			return dur.getValue();
		}
		if (type != 1)
			return Float.NaN;
		return value;
	}

	public float getValue(float v) {
		if (type == 5) {
			return v;
		}
		if (type != 1)
			return Float.NaN;
		return value;
	}

	/**
	 * Set the "fluent" of this GMathForm to the Function's ID if the type of
	 * this GMathForm is 0
	 */
	public void setElement(int e) {
		type = 0;
		fluent = e;
	}

	public int getElement() {
		return fluent;
	}

	public void setOperator(char c, GMathForm l, GMathForm r) {
		type = 3;
		op = c;
		left = l;
		right = r;
	}

	public char getOperator() {
		return op;
	}

	public void setLeft(GMathForm m) {
		// left = new GMathForm(m);
		left = m;
	}

	public GMathForm getLeft() {
		return left;
	}

	public void setRight(GMathForm m) {
		right = m;
	}

	public GMathForm getRight() {
		return right;
	}

	public float value(State s, float dur) {
		return value(s.fluentDB, dur);
	}

	public float value(FluentDB fluentDB, float dur) {
		switch (type) {
		case 0:
			return fluentDB.get(fluent);
		case 1:
			return value;
		case 3:
			switch (op) {
			case '+':
				return left.value(fluentDB, dur) + right.value(fluentDB, dur);
			case '-':
				return left.value(fluentDB, dur) - right.value(fluentDB, dur);
			case '*':
				return left.value(fluentDB, dur) * right.value(fluentDB, dur);
			case '/':
				return left.value(fluentDB, dur) / right.value(fluentDB, dur);
			default:
				return Float.NaN;
			}
		case 5:
			return dur;
		case 2:
		default:
			return Float.NaN;
		}
	}

	public float value(FluentDB fluentDB) {
		switch (type) {
		case 0:
			return fluentDB.get(fluent);
		case 1:
			return value;
		case 3:
			switch (op) {
			case '+':
				return left.value(fluentDB) + right.value(fluentDB);
			case '-':
				return left.value(fluentDB) - right.value(fluentDB);
			case '*':
				return left.value(fluentDB) * right.value(fluentDB);
			case '/':
				return left.value(fluentDB) / right.value(fluentDB);
			default:
				return Float.NaN;
			}
		case 5:
		case 2:
		default:
			return Float.NaN;
		}
	}

	public GMathForm bind(float dur) {
		switch (type) {
		case 3:
			GMathForm l = left.bind(dur);
			GMathForm r = right.bind(dur);
			if (l.type == 1 && r.type == 1) {
				float v = 0;
				switch (op) {
				case '+':
					v = left.value + right.value;
					break;
				case '-':
					v = left.value - right.value;
					break;
				case '*':
					v = left.value * right.value;
					break;
				case '/':
					v = left.value / right.value;
					break;
				default:
					v = Float.NaN;
				}
				return new GMathForm(v);
			}
			if (l != left || r != right)
				return new GMathForm(op, l, r);
			return this;
		case 5:
			return new GMathForm(dur);
		case 0:
		case 1:
		case 2:
		default:
			return this;
		}
	}

	/**
	 * Function to evaluate and return the value of this math formula given the
	 * current values... used to evaluate static/isConstant functions during the
	 * grounding process. This is intended to be called only while grounding.
	 * When this method is called, we are assuming that no states but the intial
	 * state exist.
	 * 
	 * @param constants
	 *            An array of booleans, each fluent specified by function ID.
	 *            False if isConstant, true if not.
	 * @param dur
	 *            TODO
	 * @author J. Benton (adapted from above value() method by Minh)
	 */
	public boolean analyzeStatic(float dur) {
		switch (type) {
		case 0:
			Fluent f = Planner.grounding.fluents.get(fluent);
			if (f.isConstant) {
				value = f.value;
				type = 1;
				return true;
			}
			return false;
		case 1:
			return true;
		case 2:
			return false;
		case 3:
			boolean res = true;
			res &= left.analyzeStatic(dur);
			res &= right.analyzeStatic(dur);
			if (!res)
				return false;
			type = 1;
			switch (op) {
			case '+':
				value = left.getValue(dur) + right.getValue(dur);
				break;
			case '-':
				value = left.getValue(dur) - right.getValue(dur);
				break;
			case '*':
				value = left.getValue(dur) * right.getValue(dur);
				break;
			case '/':
				value = left.getValue(dur) / right.getValue(dur);
				break;
			default:
				value = Float.NaN;
			}
			return true;
		case 5:
			if (dur == dur) {
				return true;
			}
			return false;
		default:
			return false;
		}
	}

	/** Function to print this GMathForm. Mostly for debugging */
	public String toString() {
		if (type == 0)
			return "(" + fluent + ')';

		if (type == 1)
			return Float.toString(value);

		if (type == 2)
			return "#t";

		if (type == 3)
			return "(" + op + ' ' + left + ' ' + right + ')';

		if (type == 5)
			return "?duration";

		return "NaN";
	}
}
