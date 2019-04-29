package edu.asu.sapa.utils;

// mutable integers
// but using these as keys in a hashtable requires not changing the value
// i.e., be careful!
public class Int implements Comparable<Int>, Cloneable {

	public int value;

	public Int() {
		value = 0;
	}

	public Int(int v) {
		value = v;
	}

	public int hashCode() {
		return value;
	}

	public boolean equals(Object obj) {
		if (obj instanceof Int && value == ((Int) obj).value)
			return true;
		return false;
	}

	public Object clone() {
		try {
			return super.clone();
		} catch (CloneNotSupportedException e) {
			// assert false;
		}
		return null;
	}

	public int compareTo(Int o) {
		// assert this != null && o != null;
		return value - o.value;
	}

	public String toString() {
		return Integer.toString(value);
	}

}
