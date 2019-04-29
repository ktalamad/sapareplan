/***********************************************************
     Author: Minh B. Do - Arizona State University
 ***********************************************************/
package edu.asu.sapa;

public class Priority<T extends Comparable<? super T>> implements
		Comparable<Priority<T>>, Cloneable {
	public T event;
	public float priority;

	public Priority() {
		event = null;
		priority = 0;
	}

	public Priority(T event) {
		this.event = event;
		priority = 0;
	}

	public Priority(T event, float p) {
		this.event = event;
		priority = p;
	}

	@Override
	public Priority<T> clone() {
		Priority<T> o = null;
		try {
			o = (Priority<T>) super.clone();
		} catch (CloneNotSupportedException e) {
			// assert false;
		}
		// Priority is a wrapper that does not own its event.
		// event = (T) event.clone();
		return o;
	}

	public int compareTo(Priority<T> o) {
		assert this != null && o != null;
		float d = priority - o.priority;
		if (d < 0) {
			return -1;
		}
		if (d > 0) {
			return 1;
		}
		// return event.compareTo(o.event);
		return 0;
	}

	public Priority<T> set(T event, float p) {
		this.event = event;
		priority = p;
		return this;
	}

	@Override
	public String toString() {
		return priority + ": " + event;
	}

}