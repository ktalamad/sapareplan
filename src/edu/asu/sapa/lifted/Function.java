package edu.asu.sapa.lifted;

import java.util.ArrayList;
import java.util.HashSet;

public class Function extends Term {

	public ArrayList<Function> dependents;
	public ArrayList<HashSet<Constant>> reachable;

	public Function(String n, ArrayList<Symbol<String>> v) {
		super(n, v);
		reachable = new ArrayList<HashSet<Constant>>();
		for (int i = this.size() - 1; i >= 0; --i) {
			reachable.add(new HashSet<Constant>());
		}
	}

	private Function(String n) {
		super(n);
	}

	public Function(Term t) {
		super(t);
		reachable = new ArrayList<HashSet<Constant>>();
		for (int i = this.size() - 1; i >= 0; --i) {
			reachable.add(new HashSet<Constant>());
		}
	}

	public void updateGrounding() {
		for (int i = this.size() - 1; i >= 0; --i) {
			reachable.get(i).clear();
		}
	}
	
	private void initGrounding() {
		reachable = new ArrayList<HashSet<Constant>>();
		for (int i = this.size() - 1; i >= 0; --i) {
			reachable.add(new HashSet<Constant>());
		}
	}

}
