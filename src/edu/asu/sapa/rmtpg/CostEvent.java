/********************************************************
   Author: Minh B. Do (Arizona State University)
 ********************************************************/
package edu.asu.sapa.rmtpg;

/**
 * CostEvent: Class represents the costs update event. It's used when propagate
 * the costs in the RMTPG and specify the costs to achieve a given fact is
 * update at a specific time.
 * 
 * Generalize this by changing id to CostFuction<T> (CostEvent<T>) and having
 * a single queue. Enforce Priorioty<CostEvent<T>>.time >= currentState.time,
 * and at-end preconditions can be supported. (or conditions after 0, in
 * general)
 */
public class CostEvent implements Comparable<CostEvent> {
	public int id;
	public float cost;
	public int supportID;
	public float supportTime;

	public CostEvent(int fId, float c, int aId, float aTime) {
		id = fId;
		cost = c;
		supportID = aId;
		supportTime = aTime;
	}

	public void set(int pID, float cost, int aID, float aTime) {
		id = pID;
		this.cost = cost;
		this.supportID = aID;
		this.supportTime = aTime;
	}

	public int compareTo(CostEvent o) {
		return 0; // sorting by time is sufficient
		// assert this != null && o != null;
		// if (factID < o.factID)
		// return -1;
		// if (factID > o.factID)
		// return 1;
		// if (costs < o.cost)
		// return -1;
		// if (costs > o.cost)
		// return 1;
		// // don't care how supporting actions get sorted
		// return 0;
	}

	public String toString() {
		return "f: " + id + " c: " + cost + " a: " + supportID + " aTime: " + supportTime;
	}
}
