/********************************************************************
     Author: Minh B. Do - Arizona State University
 *********************************************************************/
package edu.asu.sapa.utils;

import java.util.ArrayList;

import edu.asu.sapa.ground.Operator;

/**
 * GreedyPost.java: Greedy Post Processing the solution to greedily build in
 * linear time the correspondent p.o plan. The topological sort of that p.o plan
 * would give the possible better makespan plan.
 */
public class GreedyPost {
	// Dan
	public String output = new String("");
	// Dan
	public Ordering[] orderingSet;
	public ArrayList<Operator> aPlan;
	public ArrayList<String> actSigList;
	public float[] startTimes, actDurs;
	public int planSize;

	public int[] cActs; // Set of actions that supportIDs a specific proposition
	public float[] caDurs; // Candidate set for a given precondition that need
	// to be supported
	int caIndex = 0;

	// float totalDur;
	float delta = (float) 0.01;

	int numCL, numLM, numRM; // Number of causal links, logical mutex, and
	// resource mutex
	int numGoal;
	ArrayList<Integer>[] goalSA; // Supporting action set for each goal

	float utility;
	String outputPlan = new String();

	OCPlan anOCPlan; // Uniform DS to store the o.c. plan

	/**
	 * Functions to take the set of grounded actions that represent the
	 * fixed-time plan returned by Sapa, and their fixed execution time and
	 * durations and greedily build the causal structure from it.
	 * 
	 * Note: First action = initial state; last action = goal state.
	 */
	public float buildOrdering() {
		int i, j, k, addIndex = 0;
		float preTime, delTime, maxAddTime, addTime, addDur = 0;

		Operator anAct;
		int prop;

		// Go through all actions and their preconditions
		// start from the *latest* actions to set up the causal links
		for (i = planSize - 1; i > 0; i--) {
			anAct = aPlan.get(i);

			// Setting up the orderings based on the *logical* causal structure
			for (j = 0; j < anAct.conditionList.length; j++) {
				prop = anAct.conditionList[j].id;
				preTime = startTimes[i];
				preTime += Math.min(actDurs[i], anAct.conditionList[j].time
						.getValue());

				getSPAct(prop, preTime, i);
				delTime = getLatestDelTime(prop, preTime);

				// Find the earliest supporter which is *after* delTime
				maxAddTime = -1;
				for (k = 0; k < caIndex; k++) {
					addTime = startTimes[cActs[k]] + caDurs[k];
					if (addTime <= delTime) {
						continue;
					}

					if ((addTime < maxAddTime) || (maxAddTime < 0)) {
						addIndex = cActs[k];
						addDur = caDurs[k];
						maxAddTime = addTime;
					}
				}

				// Make k the supporting action for precondition *prop* of
				// action i
				orderingSet[i].addTemporalOrdering(addIndex, addDur);

				// Add the logical ordering (causal link)
				if (addDur > 0) {
					numCL++;
					orderingSet[i].addLogicalOrdering(addIndex, 1, 0, 1, prop);
				} else {
					numCL++;
					orderingSet[i].addLogicalOrdering(addIndex, 0, 0, 1, prop);
				}
			}
		}

		// Set up the mutex index for all actions
		for (i = 1; i < planSize; i++) {
			orderingSet[i].setMutexRelIndex();
		}

		// Setup the ordering between all pairs of logically mutex actions
		// Exclude the first (init) and last (goal) actions
		for (i = 1; i < planSize; i++) {
			for (j = i + 1; j < planSize; j++) {
				setLogicalMutexOrdering(i, j);
			}
		}

		// Setting up the orderings based on the *resource* related issue.
		// Thus, two actions using the same resource can't overlap
		for (i = 1; i < planSize; i++) {
			for (j = i + 1; j < planSize; j++) {
				setResourceMutexOrdering(i, j);
			}
		}

		return topoSort();
	}

	/**
	 * Function to find a set of actions needed to supportIDs each single goal
	 */
	public void findSAforGoals() {
		Operator goalAct = aPlan.get(planSize - 1);
		int i, j, k, aIndex, saIndex;

		numGoal = goalAct.conditionList.length;
		goalSA = new ArrayList[numGoal];
		for (i = 0; i < numGoal; i++) {
			goalSA[i] = new ArrayList<Integer>();
		}

		// Go backward to find all actions supportIDs something using the causal
		// link
		// structure.
		for (i = 0; i < numGoal; i++) {
			goalSA[i].add(new Integer(orderingSet[planSize - 1].getSAct(i)));

			for (j = 0; j < goalSA[i].size(); j++) {
				aIndex = (goalSA[i].get(j)).intValue();

				for (k = 0; k < orderingSet[aIndex].getMutexIndex(); k++) {
					saIndex = orderingSet[aIndex].getSAct(k);

					if (!goalSA[i].contains(new Integer(saIndex))) {
						goalSA[i].add(new Integer(saIndex));
					}
				}
			}
		}

		// Later find actions supporting resources (like "refuel" is one such
		// action).
		// The idea is to go throught the set of actions selected above first,
		// then
		// reason about if the resource to execute each action is enough. If
		// not, then
		// we will "add" action that increase resource level one by one until
		// resource
		// consumption is consistent.
	}

	/**
	 * Function to find the time point at which the "proposition" is deleted at
	 * the latest time point _before_ time point preTime.
	 */
	private float getLatestDelTime(int prop, float preTime) {
		int i, index;
		Operator act;
		float dur, maxDelTime = -1, delTime;

		for (i = 0; i < planSize; i++) {
			if (startTimes[i] > preTime) {
				break;
			}

			act = aPlan.get(i);
			if ((index = act.indexDelete(prop)) < 0) {
				continue;
			}

			// 0: st; 1: et;
			dur = Math.min(actDurs[i], act.effectList[index].time.getValue());
			delTime = dur + startTimes[i];

			// Check if the effect time is smaller than the precondition time
			if (delTime >= preTime) {
				continue;
			}

			if (delTime > maxDelTime) {
				maxDelTime = delTime;
			}
		}

		return maxDelTime;
	}

	public OCPlan getOCPlan() {
		return anOCPlan;
	}

	/**
	 * Function to return the plan in String
	 */
	public String getPlan() {
		return outputPlan;
	}

	/**
	 * Function to find a list of actions that supportIDs a given *prop* at time
	 * point earlier than preTime (candidate set)
	 */
	private void getSPAct(int prop, float preTime, int actIndex) {
		Operator act;
		float dur, addTime;
		int i, index;

		// Finding the set of actions that supportIDs *prop*
		caIndex = 0;
		// for(i = 0; i < planSize; i++) {
		for (i = 0; i < actIndex; i++) {
			if (startTimes[i] > preTime) {
				break;
			}

			act = aPlan.get(i);
			if ((index = act.indexAdd(prop)) < 0) {
				continue;
			}

			// 0: st; 1: et;
			dur = Math.min(actDurs[i], act.effectList[index].time.getValue());
			addTime = dur + startTimes[i];

			// Check if the effect time is smaller than the precondition time
			if (addTime > preTime) {
				continue;
			}

			cActs[caIndex] = i;
			caDurs[caIndex++] = dur;
		}

		// If we can't find the right supporter for *prop*, report error
		if (caIndex == 0) {
			System.out
					.println("GreedyPost.getSPAct(): No supporter for precond.");
			System.exit(1);
		}
	}

	/**
	 * Function to initialize the datastructures in the GreedyPost class to
	 * build the o.c. plan greedily later.
	 */
	public void initialize(ArrayList<String> actSigList,
			ArrayList<Operator> actions, ArrayList<Float> times,
			ArrayList<Float> durs, float U) {
		int i;
		aPlan = new ArrayList<Operator>(actions);
		this.actSigList = actSigList;
		planSize = aPlan.size();

		startTimes = new float[planSize];
		actDurs = new float[planSize];

		for (i = 0; i < planSize; i++) {
			startTimes[i] = times.get(i);
			actDurs[i] = durs.get(i);
		}

		orderingSet = new Ordering[planSize];
		for (i = 0; i < planSize; i++) {
			orderingSet[i] = new Ordering(i);
		}

		cActs = new int[planSize];
		caDurs = new float[planSize];

		utility = U;
		numCL = numLM = numRM = 0;
	}

	/**
	 * Function to print an action in the format required in the competition
	 * using the action signature.
	 */
	private String printAction(int index) {
		String s = new String();

		s += String.format("%.2f", startTimes[index] + delta) + ": ";
		s += "(" + actSigList.get(index) + ")";
		s += "[" + actDurs[index] + "]\n";

		return s;
	}

	/**
	 * Function to remove redundant mutex relations. A mutex ordering is
	 * redundant if it's implied by transitive closure. This function should be
	 * called only AFTER calling function buildOrdering().
	 */
	public void removeRedundantMutex() {
		int i, j, k;

		// Check if there is ordering i->k->j so that any order i->j is
		// redundant
		for (i = 0; i < planSize - 3; i++) {
			for (j = i + 2; j < planSize - 1; j++) {
				for (k = i + 1; k < j; k++) {
					if (orderingSet[k].existOrdering(i)
							&& orderingSet[j].existOrdering(k)) {
						orderingSet[j].removeMutexOrdering(i);
					}
				}
			}
		}
	}

	/**
	 * Function to set up the orderings based on the mutex relation between two
	 * actions. Note that we already have: actIndex1 < actIndex2.
	 * 
	 * This function assume the SHORTCUT that all precondition start from the
	 * starting time point, and the delete effect assumedly start taking effect
	 * from the starting point also.
	 * 
	 * Note: Can be changed for better separating duration calculation according
	 * to the different between PDDL2.1 mutex rules and the original mutex rules
	 * in Sapa.
	 */
	private void setLogicalMutexOrdering(int actIndex1, int actIndex2) {
		Operator act1, act2;
		int i, indexAdd, indexPre;
		int prop = 0;
		float dur1, maxDur1 = -1;

		act1 = aPlan.get(actIndex1);
		act2 = aPlan.get(actIndex2);

		// First check if those two are mutex
		for (i = 0; i < act1.effectList.length; i++) {
			if (act1.effectList[i].value) {
				continue;
			}

			prop = act1.effectList[i].id;

			if ((act2.indexCondition(prop, true) > -1)
					|| (act2.indexAdd(prop) > -1)) {

				dur1 = Math.min(actDurs[actIndex1], act1.effectList[i].time
						.getValue());

				if (dur1 > maxDur1) {
					maxDur1 = dur1;
				}

				// Add the logical mutex ordering relation
				if (dur1 > 0) {
					orderingSet[actIndex2].addLogicalOrdering(actIndex1, 1, 0,
							2, prop);
				} else {
					orderingSet[actIndex2].addLogicalOrdering(actIndex1, 0, 0,
							2, prop);
				}

				numLM++;
			}
		}

		for (i = 0; i < act2.effectList.length; i++) {
			if (act2.effectList[i].value) {
				continue;
			}
			prop = act2.effectList[i].id;

			indexPre = act1.indexCondition(prop, true);
			indexAdd = act1.indexAdd(prop);

			if ((indexPre > -1) || (indexAdd > -1)) {

				if (indexPre > -1) {
					dur1 = Math.min(actDurs[actIndex1],
							act1.conditionList[indexPre].time.getValue());
				} else {
					dur1 = Math.min(actDurs[actIndex1],
							act1.effectList[indexAdd].time.getValue());
				}

				if (dur1 > maxDur1) {
					maxDur1 = dur1;
				}

				// Add the logical mutex ordering relation
				if (dur1 > 0) {
					orderingSet[actIndex2].addLogicalOrdering(actIndex1, 1, 0,
							2, prop);
				} else {
					orderingSet[actIndex2].addLogicalOrdering(actIndex1, 0, 0,
							2, prop);
				}

				numLM++;
			}
		}

		// Now if those two are mutex, set up the ordering according to the p.c.
		// plan
		if (maxDur1 > -1) {
			orderingSet[actIndex2].addTemporalOrdering(actIndex1, maxDur1);
		}
	}

	/**
	 * Function to set the mutex ordering between two actions because they use
	 * the same resource. Note that actIndex2 start "after" actIndex1.
	 */
	private void setResourceMutexOrdering(int actIndex1, int actIndex2) {
		Operator act1, act2;
		act1 = aPlan.get(actIndex1);
		act2 = aPlan.get(actIndex2);

		// FIXME
		// for(i = 0; i < act1.setList.length; i++) {
		// resID = act1.setList[i].getLeftSide();
		//
		// // Check if match the *test* (precond) list of the other action
		// for(j = 0; j < act2.testList.length; j++)
		// if( resID == act2.testList[j].getLeftSide() ) {
		// orderingSet[actIndex2].addTemporalOrdering(actIndex1,actDurs[actIndex1]);
		//
		// // Add the resource mutex ordering relation
		// numRM++;
		// orderingSet[actIndex2].addLogicalOrdering(actIndex1,1,0,3,resID);
		// }
		// }

		// for(i = 0; i < act1.testList.length; i++) {
		// resID = act1.testList[i].getLeftSide();
		//
		// // Check if match the *set* (effect) list
		// for(j = 0; j < act2.setList.length; j++)
		// if( resID == act2.setList[j].getLeftSide() ) {
		// orderingSet[actIndex2].addTemporalOrdering(actIndex1,actDurs[actIndex1]);
		// // Add the resource mutex ordering relation
		// numRM++;
		// orderingSet[actIndex2].addLogicalOrdering(actIndex1,1,0,3,resID);
		// return;
		// }
		// }
	}

	/**
	 * Function to topologically sort the p.o plan to return the final
	 * consistent fixed time plan.
	 */
	private float topoSort() {
		// ArrayList unExecAct = new ArrayList(aPlan);
		int i, j;
		float maxDur, makespan = 0, tempDur;
		for (i = 0; i < planSize; i++) {
			startTimes[i] = 0;
		}

		OrderRelation oRel;

		anOCPlan = new OCPlan(planSize, numCL, numLM, numRM);
		// Add the set of logical orderings
		for (i = 0; i < planSize; i++) {
			for (j = 0; j < orderingSet[i].numLogicalOrdering(); j++) {
				oRel = orderingSet[i].getLogicalOrdering(j);
				anOCPlan.addLogOrder(oRel.relation, oRel.actIndex, i,
						oRel.saTime == 0, oRel.eaTime == 0, oRel.relID, 0);
			}
		}

		// First, removing all actions that only preceeded by action #0,
		// which is the initial state.
		for (i = 1; i < planSize; i++) {
			if ((tempDur = orderingSet[i].removeTemporalOrdering(0)) >= 0) {
				anOCPlan.addTempOrder(0, i, tempDur);
			}
		}
		// outputPlan = "";
		float time = Float.NEGATIVE_INFINITY;
		for (i = 1; i < planSize; i++) {
			float minTime = Float.POSITIVE_INFINITY;
			int index = 0;
			for (int k = 1; k < planSize; k++) {
				if (orderingSet[k].numBefore() == 0 && startTimes[k] > time) {
					minTime = Math.min(minTime, startTimes[k]);
				}
			}
			time = minTime;
			for (index = 1; index < planSize; index++) {
				if (orderingSet[index].numBefore() == 0
						&& startTimes[index] == minTime) {
					anOCPlan.setEST(index, startTimes[index] + delta);
					outputPlan += printAction(index);
					if (startTimes[index] + actDurs[index] > makespan) {
						makespan = startTimes[index] + actDurs[index];
					}

					for (j = index + 1; j < planSize; j++) {
						maxDur = orderingSet[j].removeTemporalOrdering(index);

						// Adjust the starting time of secondActIndex
						if (maxDur >= 0) {
							anOCPlan.addTempOrder(index, j, maxDur);
							if (startTimes[j] < startTimes[index] + maxDur
									+ delta) {
								startTimes[j] = startTimes[index] + maxDur
										+ delta;
							}
						}
					}
				}
			}
		}
		// anOCPlan.setEST(planSize - 1, makespan + delta);

		// Print the final statistics
		System.out.println(";; Actions: " + (planSize - 1) + " Makespan: "
				+ makespan + " Utility: " + utility);
		output += "<p>Makespan:         " + makespan + "<p>Utility:         "
				+ utility;

		System.out
				.println(";;--------start greedy post-processed plan-----------");
		System.out.print(outputPlan);
		System.out
				.println(";;--------end greedy post-processed plan-------------");

		return utility;
	}
}
