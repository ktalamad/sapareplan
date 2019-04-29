package edu.asu.sapa.lifted;

import java.util.ArrayList;
import java.util.HashMap;

import edu.asu.sapa.utils.Int;

public class Variable extends Symbol<String> {
	public Type type;
	public HashMap<Constant, Int> unreachable;
	public int referenceCount = -1;
	public ArrayList<Constant> reachable;
	// states whether this variable is a "cut" variable
	public boolean isCutVar = false;

	public Variable(String n) {
		super(n);
	}

	public Variable(String n, Type t) {
		super(n);
		type = t;
	}
	
	public Variable(String n, boolean cutVarStatus) {
		super(n);
		isCutVar = cutVarStatus;
	}
	
	public Variable(String n, Type t, boolean cutVarStatus) {
		super(n);
		type = t;
		isCutVar = cutVarStatus;
	}

	public void setType(Type t) {
		if (type == null)
			type = t;
	}

	public Type getType() {
		return type;
	}

	public void updateGrounding() {
		reachable.clear();
		for (Constant c : type.constants) {
			if (unreachable.get(c) != null) 
				unreachable.get(c).value = referenceCount;
			else
				unreachable.put(c,new Int(referenceCount));
		}
		if (referenceCount == 0) {
			for (Constant c : type.constants) {
				reachable.add(c);
			}
		}
	}
	
	public void initGrounding() {
		unreachable = new HashMap<Constant, Int>();
		reachable = new ArrayList<Constant>();
		for (Constant c : type.constants) {
			unreachable.put(c, new Int(referenceCount));
		}
		if (referenceCount == 0) {
			for (Constant c : type.constants) {
				reachable.add(c);
			}
		}
	}

	public int compareTo(Symbol<String> o) {
		if (o instanceof Variable) {
			return ((Variable) o).referenceCount - this.referenceCount;
		} else {
			return super.compareTo(o);
		}
	}

}
