package edu.asu.sapa;

import java.lang.reflect.Array;

import edu.asu.sapa.ground.update.Test;

public class TestQ extends PriorityQueue<Test> {

	public TestQ() {
		Priority<Test> f = new Priority<Test>();
		typePriorityE = (Class<Priority<Test>>) f.getClass();
		events = (Priority<Test>[])Array.newInstance(typePriorityE, DEFAULT);
		events[0] = f;
	}

	public TestQ(int capacity) {
		if (capacity < DEFAULT)
			capacity = DEFAULT;
		Priority<Test> f = new Priority<Test>();
		typePriorityE = (Class<Priority<Test>>) f.getClass();
		events = (Priority<Test>[])Array.newInstance(typePriorityE, capacity);
		events[0] = f;
	}

	public TestQ(PriorityQueue<Test> queue) {
		super(queue);
	}

}
