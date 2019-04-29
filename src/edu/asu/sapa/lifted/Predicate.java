package edu.asu.sapa.lifted;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashSet;

public class Predicate extends Term implements Serializable {

	public ArrayList<Predicate> dependents;
	public ArrayList<HashSet<Constant>> reachable;
        public String text;

	private Predicate(String n) {
		super(n);
                text = new String(name);
	}

	public Predicate(String n, ArrayList<Symbol<String>> v) {
		super(n, v);
		reachable = new ArrayList<HashSet<Constant>>();
		for (int i = this.size() - 1; i >= 0; --i) {
			reachable.add(new HashSet<Constant>());
		}
                text = new String(name);
	}

	public Predicate(Term t) {
		super(t);
		reachable = new ArrayList<HashSet<Constant>>();
		for (int i = this.size() - 1; i >= 0; --i) {
			reachable.add(new HashSet<Constant>());
		}
                text = new String(name);
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
