package edu.asu.sapa;

import java.lang.reflect.Array;
import java.util.Arrays;

import edu.asu.sapa.ground.State;

//can recycle memory a little bit, if we implement a State.set(State) method
// (copying).  But it can reuse this.* allocated elements, since all States
// are kept memory independent.  (so, like, this.actions.set(index,foo)...
// this.actions.setSize(fooSize)...)
public class StateQ extends PriorityQueue<State> {
	public StateQ() {
		Priority<State> f = new Priority<State>();
		typePriorityE = (Class<Priority<State>>) f.getClass();
		events = (Priority<State>[])Array.newInstance(typePriorityE, 1024);
		events[0] = f;
	}

	public StateQ(int capacity) {
		if (capacity < 1024)
			capacity = 1024;
		Priority<State> f = new Priority<State>();
		typePriorityE = (Class<Priority<State>>) f.getClass();
		events = (Priority<State>[])Array.newInstance(typePriorityE, capacity);
		events[0] = f;
	}

	public StateQ(PriorityQueue<State> queue) {
		super(queue);
	}

	public boolean add(State s,float f) {
		Priority<State> t = newElement();
		t.set(s, f);
		int i = size++;
		int parent;
		for (; (parent = (i - 1) >> 1) >= 0 && t.compareTo(events[parent]) < 0; i = parent) {
			events[i] = events[parent];
		}
		events[i] = t;
		return true;
	}

	protected void growArray() {
		events = Arrays.copyOf(events, size << 1);
		System.out.println(";;State Queue doubled");
	}

}
