package edu.asu.sapa.lifted;

import java.io.Serializable;
import java.util.*;

public class Term extends Symbol<String> implements Bindable<Symbol<String>>, Serializable {
//	private static final long serialVersionUID = 1L;

	public ArrayList<Symbol<String>> args = new ArrayList<Symbol<String>>();

	public Term(String n) {
		super(n);
	}

	public Term(String n, ArrayList<Symbol<String>> v) {
		super(n);
		args = v;
	}

	// shallow copy!! OMG watch out @! !!!
	public Term(Term t) {
		super(t.name);
		args = t.args;
	}

	public ArrayList<Integer> bind(ConstantSymbol<?>[] map) {
		return Binder.bind(this, map);
	}

	public ArrayList<Integer> bind() {
		return Binder.bind(this);
	}

	public void add(int index, Symbol<String> element) {
		args.add(index, element);
	}

	public boolean add(Symbol<String> e) {
		return args.add(e);
	}

	public boolean addAll(Collection<? extends Symbol<String>> c) {
		return args.addAll(c);
	}

	public boolean addAll(int index, Collection<? extends Symbol<String>> c) {
		return args.addAll(index, c);
	}

	public void clear() {
		args.clear();
	}

	public void ensureCapacity(int minCapacity) {
		args.ensureCapacity(minCapacity);
	}

	public Symbol<String> get(int index) {
		return args.get(index);
	}

	public int indexOf(Object o) {
		return args.indexOf(o);
	}

	public boolean isEmpty() {
		return args.isEmpty();
	}

	public Iterator<Symbol<String>> iterator() {
		return args.iterator();
	}

	public Symbol<String> remove(int index) {
		return args.remove(index);
	}

	public boolean remove(Object o) {
		return args.remove(o);
	}

	public boolean removeAll(Collection<?> c) {
		return args.removeAll(c);
	}

	public boolean retainAll(Collection<?> c) {
		return args.retainAll(c);
	}

	public Symbol<String> set(int index, Symbol<String> element) {
		return args.set(index, element);
	}

	public int size() {
		return args.size();
	}

	public List<Symbol<String>> subList(int fromIndex, int toIndex) {
		return args.subList(fromIndex, toIndex);
	}

	public boolean contains(Object o) {
		return args.contains(o);
	}

	public boolean containsAll(Collection<?> c) {
		return args.containsAll(c);
	}

	public int lastIndexOf(Object o) {
		return args.lastIndexOf(o);
	}

	public Object[] toArray() {
		return args.toArray();
	}

	public <T> T[] toArray(T[] a) {
		return args.toArray(a);
	}

	public ListIterator<Symbol<String>> listIterator() {
		return args.listIterator();
	}

	public ListIterator<Symbol<String>> listIterator(int index) {
		return args.listIterator(index);
	}

	public void trimToSize() {
		args.trimToSize();
	}

	public String toString() {
		return Binder.toString(this);
	}

}
