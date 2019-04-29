package edu.asu.sapa;

import java.lang.reflect.Array;

import edu.asu.sapa.ground.update.Set;

public class SetQ extends PriorityQueue<Set> {

	public SetQ() {
		Priority<Set> f = new Priority<Set>();
		typePriorityE = (Class<Priority<Set>>) f.getClass();
		events = (Priority<Set>[])Array.newInstance(typePriorityE, DEFAULT);
		events[0] = f;
	}

	public SetQ(int capacity) {
		if (capacity < DEFAULT)
			capacity = DEFAULT;
		Priority<Set> f = new Priority<Set>();
		typePriorityE = (Class<Priority<Set>>) f.getClass();
		events = (Priority<Set>[])Array.newInstance(typePriorityE, capacity);
		events[0] = f;
	}

	public SetQ(PriorityQueue<Set> queue) {
		super(queue);
	}
}
