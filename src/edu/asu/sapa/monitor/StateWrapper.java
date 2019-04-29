package edu.asu.sapa.monitor;

import edu.asu.sapa.ground.State;

public class StateWrapper {
	// likely safe, but volatile just in case...
	private volatile State state;
	
	synchronized public State getState() {
		return state;
	}
	
	synchronized public void setState(State st) {
		state = st;
	}
}
