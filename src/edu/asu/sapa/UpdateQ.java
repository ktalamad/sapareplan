package edu.asu.sapa;

import java.lang.reflect.Array;

import edu.asu.sapa.ground.update.Update;

public class UpdateQ extends PriorityQueue<Update<Update<?>>> {

	public UpdateQ() {
		Priority<Update<Update<?>>> f = new Priority<Update<Update<?>>>();
		typePriorityE = (Class<Priority<Update<Update<?>>>>) f.getClass();
		events = (Priority<Update<Update<?>>>[])Array.newInstance(typePriorityE, DEFAULT);
		events[0] = f;
	}

	public UpdateQ(int capacity) {
		if (capacity < DEFAULT)
			capacity = DEFAULT;
		Priority<Update<Update<?>>> f = new Priority<Update<Update<?>>>();
		typePriorityE = (Class<Priority<Update<Update<?>>>>) f.getClass();
		events = (Priority<Update<Update<?>>>[])Array.newInstance(typePriorityE, capacity);
		events[0] = f;
	}

	public UpdateQ(PriorityQueue<Update<Update<?>>> queue) {
		super(queue);
	}

}
