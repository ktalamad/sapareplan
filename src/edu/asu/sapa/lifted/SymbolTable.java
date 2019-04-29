package edu.asu.sapa.lifted;

import java.util.ArrayList;
import java.util.HashMap;

public class SymbolTable<K, T extends Symbol<K>> {
	public int count = 0;
	public ArrayList<T> symbols = new ArrayList<T>();
	public HashMap<K, T> map = new HashMap<K, T>();

	public SymbolTable() {
	}

	public SymbolTable(T s) {
		put(s);
	}

	public SymbolTable(SymbolTable<K, T> o) {
		count = o.count;
		symbols = new ArrayList<T>(o.symbols);
		map = new HashMap<K,T>(o.map);
	}

	public T put(T s) {
		T o = map.get(s.name);
		if (o != null) {
			s.id = o.id;
			return o;
		}
		map.put(s.name, s);
		symbols.add(s);
		s.id = count++;
		return s;
	}

	public T get(K k) {
		return map.get(k);
	}

	public T get(int i) {
		return symbols.get(i);
	}

	public K getName(int i) {
		return symbols.get(i).name;
	}

	public String toString() {
		return symbols.toString();
	}
}
