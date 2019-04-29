package edu.asu.sapa.lifted;

import java.util.ArrayList;

public class Binder {
	static public <T extends Symbol<?> & Bindable<? extends Symbol<?>>> ArrayList<Integer> bind(
			T o, ConstantSymbol<?>[] map) {
		if (map == null /*|| map.length < 1*/)
			return bind(o);
		if (o.id == 0)
			return new ArrayList<Integer>();
		int size = o.size();
		ArrayList<Integer> intName = new ArrayList<Integer>(size + 1);
		intName.add(o.id);
		for (int i = 0; i < size; ++i) {
			Symbol<?> s = o.get(i);
			if (s instanceof ConstantSymbol) {
				intName.add(s.id);
			} else
				// if (s instanceof Variable)
				intName.add(map[s.id].id);
		}
		return intName;
	}

	static public <T extends Symbol<?> & Bindable<? extends Symbol<?>>> ArrayList<Integer> bind(
			T o) {
		if (o.id == 0)
			return new ArrayList<Integer>();
		int size = o.size();
		ArrayList<Integer> intName = new ArrayList<Integer>(size + 1);
		intName.add(o.id);
		for (int i = 0; i < size; ++i) {
			Symbol<?> s = o.get(i);
			if (s instanceof ConstantSymbol)
				intName.add(s.id);
			else
				intName.add(-s.id - 1);
		}
		return intName;
	}

	static public <T extends Symbol<String> & Bindable<? extends Symbol<String>>> String toString(
			T o) {
		int size = o.size();
		StringBuilder buf = new StringBuilder(10 * (size + 1));
		buf.append(o.name);
		for (int i = 0; i < size; ++i) {
			buf.append(' ').append(o.get(i));
		}
		return buf.toString();
	}

	static public <T extends Bindable<? extends Symbol<String>>> String argsToString(
			T o) {
		int size = o.size();
		StringBuilder buf = new StringBuilder(10 * (size));
		if (size > 0)
			buf.append(o.get(0));
		for (int i = 1; i < size; ++i) {
			buf.append(' ').append(o.get(i));
		}
		return buf.toString();
	}

}

