/********************************************************************
   Author: Minh B. Do (Arizona State University - binhminh@asu.edu)
 *********************************************************************/
package edu.asu.sapa.ground;

import java.util.HashMap;
import java.util.Map.Entry;

import edu.asu.sapa.ground.update.Set;
import edu.asu.sapa.ground.update.Test;

/**
 * FluentDB: Metric Resource Database. This class manages the set of values for
 * all continuous functions. All things related to the metric map are from June
 * 12, 2004 and done by J. Benton.
 * 
 * @author Binh Minh Do
 * @author J. Benton
 */
public class FluentDB implements Cloneable {
	public HashMap<Integer, Float> valueMap = new HashMap<Integer, Float>();

	public FluentDB() {
	}

	public FluentDB(FluentDB mr) {
		valueMap = new HashMap<Integer, Float>(mr.valueMap);
	}

	public Object clone() {
		FluentDB o = null;
		try {
			o = (FluentDB) super.clone();
		} catch (CloneNotSupportedException e) {
			// assert false;
		}
		o.valueMap = (HashMap<Integer, Float>) o.valueMap.clone();
		return o;
	}

	public int size() {
		return valueMap.size();
	}

	/** Get the value of a particular function */
	public float get(int id) {
		Float f = valueMap.get(id);
		if (f == null)
			return Float.NaN;
		return f;
	}

	public Float put(int id, float value) {
		if (value != value)
			return remove(id);
		Float f = valueMap.put(id, value);
		if (f == null)
			return Float.NaN;
		return f;
	}

	public float remove(int id) {
		Float f = valueMap.remove(id);
		if (f == null)
			return Float.NaN;
		return f;
	}

	/**
	 * Update a function value according to the op and the indicated
	 * assignment/increment/decreasement value oper = 0 (=); 1: (-=); 2: (+=);
	 * 3: (*=): 4: (/=)
	 */
	public boolean update(int op, int id, float value) {
		if (op == 0) {
			valueMap.put(id, value);
			return true;
		}

		Float v = valueMap.get(id);
		if (v == null)
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
			valueMap.remove(id);
			return false;
		}

		valueMap.put(id, v);
		return true;
	}

	/**
	 * Function to only update the resource values if the change is increase
	 * used in the heuristics function when we relax the reduction effects
	 * (similar to the delete effects of the predicate)
	 */
	public void relaxUpdate(Operator a, float dur) {
		Set ms;
		int fID;
		float value;
		int oper;

		for (int i = 0; i < a.setList.length; i++) {
			ms = (Set) a.setList[i];

			oper = ms.getOp();

			fID = ms.getLeftSide();
			value = ms.getRightSide().value(this, dur);

			float v = valueMap.get(fID);

			if (oper == 0 && v < value) {
				valueMap.put(fID, value);
			} else if (oper == 1 && value > 0) {
				valueMap.put(fID, v + value);
			} else if (oper == 2 && value < 0) {
				valueMap.put(fID, v - value);
			} else if (oper == 3 && value > 1) {
				valueMap.put(fID, v * value);
			} else if (oper == 4 && value < 1) {
				valueMap.put(fID, v / value);
			}
		}

	}

	public String toString() {
		String s = "FluentDB:\n";
		for (Entry<Integer, Float> fv : valueMap.entrySet()) {
			s += "\t(:= " + fv.getKey() + " " + fv.getValue() + ")\n";
		}
		return s;
	}

	public boolean applicable(Operator a) {
		float dur = a.getDuration(this);
		Test test;
		float t;
		for (int i = 0; i < a.testList.length; ++i) {
			test = a.testList[i];
			t = test.time.value(this, dur);
			if (t <= 0)
				if (!test.holds(this))
					return false;
		}
		return true;
	}

	public float value(int id) {
		Float val = valueMap.get(id);
		if (val == null) return Float.NaN;
		return val;
	}
}
