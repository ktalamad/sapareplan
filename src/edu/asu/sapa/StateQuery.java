package edu.asu.sapa;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;

import edu.asu.sapa.PriorityQueue.PriorityQueueIterator;
import edu.asu.sapa.ground.*;
import edu.asu.sapa.lifted.Problem;
import edu.asu.sapa.lifted.SymbolTable;

/**
 * State Query class to enable queries (by timepoint) on a currently existing plan. 
 * This will house all the operations that need to be performed in order to extract 
 * return the current state of the world as required by an external architecture.
 * @author KRT
 */

public class StateQuery {
	
	private static Planner plannerInstance;
	private double queryTime;

	private StateQ queue;
	private SymbolTable<ArrayList<Integer>, Proposition> propositions;
	
	private String propName;
	
	private HashMap<Double, String> timeMap;
	
	public StateQuery (Planner planner) {
		
		StateQuery.plannerInstance = planner;
		this.queue = planner.queue;
		this.propositions = planner.grounding.propositions;
	}
	
	public StateQuery (Planner planner, double qTime) {
		
		StateQuery.plannerInstance = planner;
		this.queryTime = qTime;
		this.queue = planner.queue;
		this.propositions = planner.grounding.propositions;
	}
	
	/** Prints the state based on the query time given */
	public void printProps1 () {
		
		PriorityQueue<State>.PriorityQueueIterator stateQueueIterator = queue.iterator();
		State tempState;
		PropDB tempPropDB;
		
		ArrayList<Proposition> props = propositions.symbols;
						
		/**
		 * So get the timeMap from the propDB object. Then, for each prop, use that timeMap to cross reference
		 * the ID and then print the name of that prop, along with the time at that time.
		 */
		
		while (stateQueueIterator.hasNext()) {
			
			tempState = stateQueueIterator.next().event;
			tempPropDB = tempState.propDB;
			
			for (Proposition tempProp : props) {
				
				if (tempPropDB.contains(tempProp.id)) {
					System.out.println("PROP: at " + tempPropDB.getTime(tempProp.id) + 
							" " + tempProp.getName());					
				}
			}
		}
		
		System.out.println("\n");
		
		
	}
	
	public void printState (State st) {
		
		State tempState = st;
		ArrayList<Proposition> props = propositions.symbols;
		
		/**
		 * So get the timeMap from the propDB object. Then, for each prop, use that timeMap to cross reference
		 * the ID and then print the name of that prop, along with the time at that time.
		 */
		
		PropDB tempPropDB = tempState.propDB;
		
		for (Proposition tempProp : props) {
			
			if (tempPropDB.contains(tempProp.id)) {
				System.out.println("PROP: " + tempProp.id + " at " + tempPropDB.getTime(tempProp.id) + 
						" " + tempProp.getName());					
			}
		}

	}
	
	public void printProps2 () {
		
		System.out.println("\nFULL: " + getStatePredicates() + "\n");
		System.out.println("BEFORE: " + getPredsBefore(51.0) + "\n");
		System.out.println("AFTER: " + getPredsAfter(51.0) + "\n");
		System.out.println("BETWEEN: " + getPredsBetween(25.0, 75.0) + "\n");
		
	}

	public double getQueryTime() {
		return queryTime;
	}
	
	public void setQueryTime(double qTime) {
		this.queryTime = qTime;
	}
	
	public ArrayList getPredsBefore (Double time) {
		
		ArrayList<String> retList = new ArrayList<String>();
		
		// Not checking for null to avoid re-instantiation because this may 
		// be called at various points during execution monitoring, and it is 
		// best to get a fresh copy of the predicates for each call.
		// TODO: Implement an update function for timeMap to improve efficiency
		
		timeMap = getStatePredicates();
		
		Iterator<Double> timeMapIterator = timeMap.keySet().iterator();
		
		while (timeMapIterator.hasNext()) {
			Double next;
			if ((next = timeMapIterator.next()) <= time)
				retList.add(timeMap.get(next));
		}
		
		return retList;		
	}
	
	public ArrayList getPredsAfter (Double time) {
		
		ArrayList<String> retList = new ArrayList<String>();
		
		// Not checking for null to avoid re-instantiation because this may 
		// be called at various points during execution monitoring, and it is 
		// best to get a fresh copy of the predicates for each call.
		// TODO: Implement an update function for timeMap to improve efficiency
		
		timeMap = getStatePredicates();
		
		Iterator<Double> timeMapIterator = timeMap.keySet().iterator();
		
		while (timeMapIterator.hasNext()) {
			Double next;
			if ((next = timeMapIterator.next()) >= time)
				retList.add(timeMap.get(next));
		}
		
		return retList;
	}
	
	public ArrayList getPredsBetween (Double startTime, Double endTime) {
		
		ArrayList<String> retList = new ArrayList<String>();
		
		// Not checking for null to avoid re-instantiation because this may 
		// be called at various points during execution monitoring, and it is 
		// best to get a fresh copy of the predicates for each call.
		// TODO: Implement an update function for timeMap to improve efficiency
		
		timeMap = getStatePredicates();
		
		Iterator<Double> timeMapIterator = timeMap.keySet().iterator();
		
		while (timeMapIterator.hasNext()) {
			Double next;
			if (((next = timeMapIterator.next()) >= startTime) && (next <= endTime))
				retList.add(timeMap.get(next));
		}
		
		return retList;
	}
	
	private HashMap<Double, String> getStatePredicates () {
		
		timeMap = new HashMap<Double, String>();
		PriorityQueue<State>.PriorityQueueIterator stateQueueIterator = queue.iterator();
		State tempState;
		PropDB tempPropDB;
		
		ArrayList<Proposition> props = propositions.symbols;
		
		while (stateQueueIterator.hasNext()) {
			
			tempState = stateQueueIterator.next().event;
			tempPropDB = tempState.propDB;
			
			for (Proposition tempProp : props) {
				
				if (tempPropDB.contains(tempProp.id)) {
					
					Double propTime = tempPropDB.getTime(tempProp.id).doubleValue();
					String propName = tempProp.getName();
					
					if (!(timeMap.containsValue(propName))) {
						
						timeMap.put(propTime, propName);
					}						
				}
			}
		}
				
		return timeMap;		
	}
	
/*
 * Need methods for accessing by both key as well as by value. Querying 
 * by predicate name is probably not as important, but we definitely need 
 * to be able to query by time (double).
 */
	
/*
 * After discussion with Gordon: Double is passed in as query, return all 
 * predictes that are true as of (?) that time.
 */
	
	
	

}
