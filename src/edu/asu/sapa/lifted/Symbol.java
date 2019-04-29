package edu.asu.sapa.lifted;

import java.io.Serializable;

public class Symbol<K> implements Cloneable, Comparable<Symbol<K>>, Serializable {
	public int id = -1;
	public K name = null;

	public Symbol(K name) {
		this.name = name;
	}

	// symbols are logical constants
	@Override
	public Object clone() {
		try {
			return super.clone();
		} catch (CloneNotSupportedException e) {
			// assert false;
		}
		return null;
	}

	public int compareTo(Symbol<K> o) {
		assert this != null && o != null;
		return id - o.id;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof Symbol && id == ((Symbol) obj).id)
			return true;
		return false;
	}

	@Override
	public int hashCode() {
		return id;
	}

	@Override
	public String toString() {
		return name.toString();
	}

}
