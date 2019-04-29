/*************************************************************
   Author: Minh B. Do - Arizona State University
 **************************************************************/
package edu.asu.sapa.ground;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map.Entry;

import edu.asu.sapa.Planner;

/**
 * PropDB: Class to manage the set of predicates that are true at a given time
 * point.
 */

public class PropDB implements Cloneable {
	public HashMap<Integer, Float> timeMap;

	public PropDB() {
		timeMap = new HashMap<Integer, Float>();
	}

	public PropDB(PropDB p) {
		timeMap = new HashMap<Integer, Float>(p.timeMap);
	}

	public PropDB(ArrayList<Integer> seed, float time) {
		timeMap = new HashMap<Integer, Float>();

		for (Integer index : seed) {
			timeMap.put(index, time);
		}
	}

	public Float getTime(Integer id) {
		return timeMap.get(id);
	}
	
	public boolean holds(Integer id) {
		return timeMap.containsKey(id);
	}

	public Object clone() {
		PropDB o = null;
		try {
			o = (PropDB) super.clone();
		} catch (CloneNotSupportedException e) {
			// assert false;
		}
		o.timeMap = (HashMap<Integer, Float>) o.timeMap.clone();
		return o;
	}

	public boolean equals(PropDB p) {
		if (p != null && (this == p || timeMap.equals(p.timeMap))) {
			return true;
		}
		return false;
	}

	public int numPred() {
		return timeMap.size();
	}

	public Collection<Integer> getProps() {
		return timeMap.keySet();
	}

	public HashMap<Integer, Float> getTimes() {
		return timeMap;
	}

	public void add(int id, float time) {
		if (!timeMap.containsKey(id))
			timeMap.put(id, time);
	}

	public void delete(int id, float time) {
		timeMap.remove(id);
	}

	public boolean contains(Integer p) {
		return timeMap.containsKey(p);
	}

	public String toString() {
		String s = "PropDB:\n";
		for (Entry<Integer, Float> fv : timeMap.entrySet()) {
			Proposition prop = Planner.grounding.propositions.symbols.get(fv.getKey());
			s += "\t(at " + fv.getValue() + ' ' + prop.getName() + ")\n";
		}
		return s;
	}

	public void remove(int id) {
		timeMap.remove(id);
	}

	public int size() {
		return timeMap.size();
	}


}
