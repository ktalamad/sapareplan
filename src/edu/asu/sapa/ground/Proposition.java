package edu.asu.sapa.ground;

import java.util.ArrayList;
import java.util.Arrays;

import edu.asu.sapa.Planner;
import edu.asu.sapa.lifted.Symbol;
import edu.asu.sapa.utils.Utility;

@SuppressWarnings("serial")
public class Proposition extends Symbol<ArrayList<Integer>> {

	// should keep track of supporters too, and realize when they drop to 0
	// (if we want to do eager analysis upon no longer applicable actions)
	// the other possibility is to just clear out all propositions and do
	// a repropagation from the (new) initial action
	public int[] dependents = new int[8];
	public int size = 0;

	public boolean isConstant = true;
	public boolean value=false;
	
	private String stringName;

	public Proposition(ArrayList<Integer> name) {
		super(name);
	}

	public String toString() {
		return name.toString();
	}
	
	public String getName() {
		if (stringName == null) {
			if (name != null && name.size() > 0 && name.get(0) != 0) {
				StringBuilder str = new StringBuilder(50);
				int id = name.get(0);
				str.append('(').append(Planner.problem.predicates.get(id).name);
				for (int i = 1, size = name.size(); i < size; ++i) {
					id = name.get(i);
					str.append(' ').append(
							Planner.problem.constants.get(id).name);
				}
				str.setCharAt(0, '(');
				str.append(')');
				stringName = str.toString();
			} else {
				stringName = Planner.problem.predicates.get(0).name;
			}
		}
		return stringName;
	}

	public int addDependent(int id) {
		if (size >= dependents.length) {
			dependents = Arrays.copyOf(dependents, size + 8 + (size >> 2));
		}
		dependents[size++] = id;
		return size;
	}

	public void trim() {
		if (size != dependents.length)
			dependents = Arrays.copyOf(dependents, size);
	}

	public int remove(int id) {
		// could be present multiple times (at-start,over-all,at-end condition)
		for (int i = 0; i < size; ++i) {
			if (dependents[i] == id) {
				Utility.remove(dependents, i);
				--size;
				--i;
			}
		}
		return size;
	}

}
