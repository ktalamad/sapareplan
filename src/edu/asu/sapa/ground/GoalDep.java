package edu.asu.sapa.ground;

import java.util.ArrayList;

import edu.asu.sapa.lifted.Predicate;

public class GoalDep {
	public ArrayList<Predicate> goals;
	public float sUtil; // Static utility
	
	public GoalDep() {
		goals = new ArrayList<Predicate>();
		sUtil = 0;
	}
	
	public GoalDep(ArrayList<Predicate> gs, float su) {
		goals = new ArrayList<Predicate>(gs);
		sUtil = su;
	}
	
	public int size() {
		return goals.size();
	}
	
	public String toString() {
		return "(" + goals.toString() + ") " + sUtil;
	}
}
