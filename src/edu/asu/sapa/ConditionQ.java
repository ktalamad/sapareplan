package edu.asu.sapa;

import java.lang.reflect.Array;

import edu.asu.sapa.ground.update.Condition;

public class ConditionQ extends PriorityQueue<Condition> {

	public ConditionQ() {
		Priority<Condition> f = new Priority<Condition>();
		typePriorityE = (Class<Priority<Condition>>) f.getClass();
		events = (Priority<Condition>[])Array.newInstance(typePriorityE, DEFAULT);
		events[0] = f;
	}

	public ConditionQ(int capacity) {
		if (capacity < DEFAULT)
			capacity = DEFAULT;
		Priority<Condition> f = new Priority<Condition>();
		typePriorityE = (Class<Priority<Condition>>) f.getClass();
		events = (Priority<Condition>[])Array.newInstance(typePriorityE, capacity);
		events[0] = f;
	}

	public ConditionQ(PriorityQueue<Condition> queue) {
		super(queue);
	}

	public boolean add(float time, int id, boolean value) {
		Priority<Condition> t = newElement();
		t.set(new Condition(id, value, null), time);
		int i = size++;
		int parent;
		for (; (parent = (i - 1) >> 1) >= 0 && t.compareTo(events[parent]) < 0; i = parent) {
			events[i] = events[parent];
		}
		events[i] = t;
		return true;
	}
}
