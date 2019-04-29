package edu.asu.sapa.ground.update;

import edu.asu.sapa.ground.State;

public interface Update<T> extends Cloneable, Comparable<T> {
	boolean update(State s);

	boolean update(State s, float dur);
	
}
