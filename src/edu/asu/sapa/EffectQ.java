package edu.asu.sapa;

import java.lang.reflect.Array;

import edu.asu.sapa.ground.update.Effect;

public class EffectQ extends PriorityQueue<Effect> {

	public EffectQ() {
		Priority<Effect> f = new Priority<Effect>();
		typePriorityE = (Class<Priority<Effect>>) f.getClass();
		events = (Priority<Effect>[])Array.newInstance(typePriorityE, DEFAULT);
		events[0] = f;
	}

	public EffectQ(int capacity) {
		if (capacity < DEFAULT)
			capacity = DEFAULT;
		Priority<Effect> f = new Priority<Effect>();
		typePriorityE = (Class<Priority<Effect>>) f.getClass();
		events = (Priority<Effect>[])Array.newInstance(typePriorityE, capacity);
		events[0] = f;
	}

	public EffectQ(PriorityQueue<Effect> queue) {
		super(queue);
	}

	public boolean add(float time, int id, boolean value) {
		Priority<Effect> t = newElement();
		t.set(new Effect(id, value, null), time);
		int i = size++;
		int parent;
		for (; (parent = (i - 1) >> 1) >= 0 && t.compareTo(events[parent]) < 0; i = parent) {
			events[i] = events[parent];
		}
		events[i] = t;
		return true;
	}
}
