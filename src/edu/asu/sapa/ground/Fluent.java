package edu.asu.sapa.ground;

import java.util.ArrayList;

import edu.asu.sapa.ground.update.Test;
import edu.asu.sapa.lifted.Symbol;

public class Fluent extends Symbol<ArrayList<Integer>> {

	// Test lock;
	GMathForm idForm;

	public boolean isConstant = true;
	public float value = Float.NaN;

	public Fluent(ArrayList<Integer> name) {
		super(name);
	}

	// should change this to ProtectTest
	// add a .remove() (or .kill, or whatever) to ProtectTest
	// with the definition rightSide.value == GMathForm.NaN
	// and leftSide.value == GMathForm.NaN
	// and then have lock return failure if rightSide.value != GMathForm.NaN
	// && rightSide.value != state.fluentDB.get(id);
	// in the latter case, need to search the priority Q in the state ( :( ) for
	// the
	// current lock, compare times, and remove and re-add to the Q if this time
	// is
	// larger. (must have the time information here to make that useful...)
	// perhaps instead -- make lock a Priority<ProtectTest> and set the time
	// field
	// of the priority test to the maximum of the interval
	// and have .remove() auto re-add to the queue if state.time <
	// this.event.time
	public Test lock(State state) {
		// if (lock == null)
		// lock = new Test(0, new GMathForm(id), GMathForm.NaN, null);
		// lock.rightSide.value = state.fluentDB.get(id);
		// return lock;
		if (idForm == null)
			idForm = new GMathForm(id);
		return new Test(0, idForm, new GMathForm(state.fluentDB.get(id)), null);
	}

	public String toString() {
		return name.toString();
	}
}
