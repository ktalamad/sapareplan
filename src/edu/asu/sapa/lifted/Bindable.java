package edu.asu.sapa.lifted;

import java.util.ArrayList;
import java.util.List;

public interface Bindable<T extends Symbol<?>> extends List<T> {

	public ArrayList<Integer> bind(ConstantSymbol<?>[] map);

	public ArrayList<Integer> bind();

}

//class Binder {
//	static public <T extends Symbol<?> & Bindable<?>> ArrayList<Integer> bind(
//			T o, ConstantSymbol<?>[] map) {
//		if (map == null)
//			return bind(o);
//		if (o.id == 0)
//			return new ArrayList<Integer>();
//		int size = o.size();
//		ArrayList<Integer> intName = new ArrayList<Integer>(size + 1);
//		intName.add(o.id);
//		for (int i = 0; i < size; ++i) {
//			Symbol<?> s = o.get(i);
//			if (s instanceof ConstantSymbol) {
//				intName.add(s.id);
//			} else
//				// if (s instanceof Variable)
//				intName.add(map[s.id].id);
//		}
//		return intName;
//	}
//
//	static public <T extends Symbol<?> & Bindable<?>> ArrayList<Integer> bind(
//			T o) {
//		if (o.id == 0)
//			return new ArrayList<Integer>();
//		int size = o.size();
//		ArrayList<Integer> intName = new ArrayList<Integer>(size + 1);
//		intName.add(o.id);
//		for (int i = 0; i < size; ++i) {
//			Symbol<?> s = o.get(i);
//			if (s instanceof ConstantSymbol)
//				intName.add(s.id);
//			else
//				intName.add(-s.id - 1);
//		}
//		return intName;
//	}
//
//	static public <T extends Symbol<? extends String> & Bindable<? extends Symbol<? extends String>>> String toString(
//			T o) {
//		int size = o.size();
//		StringBuilder buf = new StringBuilder(10 * (size + 1));
//		buf.append(o.name);
//		for (int i = 0; i < size; ++i) {
//			buf.append(' ').append(o.get(i));
//		}
//		return buf.toString();
//	}
//
//	static public <T extends Bindable<? extends Symbol<? extends String>>> String argsToString(
//			T o) {
//		int size = o.size();
//		StringBuilder buf = new StringBuilder(10 * (size));
//		if (size > 0)
//			buf.append(o.get(0));
//		for (int i = 1; i < size; ++i) {
//			buf.append(' ').append(o.get(i));
//		}
//		return buf.toString();
//	}
//
//}