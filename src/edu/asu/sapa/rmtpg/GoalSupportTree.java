/************************************************************
 Author: Minh B. Do -- Arizona State University
 *************************************************************/
package edu.asu.sapa.rmtpg;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;

import edu.asu.sapa.ground.GoalManager;
import edu.asu.sapa.ground.Operator;
import edu.asu.sapa.ground.update.Condition;
import edu.asu.sapa.ground.update.ProtectCondition;
import edu.asu.sapa.utils.Utility;

/**
 * GoalSupportTree: Class to build the tree of supporting actions for the top
 * level Goals. Only used in the partial satisfaction problem to help remove
 * Goals that are not beneficial from the remaining goal set.
 */

public class GoalSupportTree {
	GoalManager gm;

	int maxGoal, naGoal; // total Goals, achieved Goals,

	int[] achievedGoal, nogoodGoal;

	int nra; // nra: Number of Removed Action (from RP)
	int[] relaxedPlan;
	int nRelaxedPlan;

	// number of reward-Goals supported by each action
	int[] gbaCount;
	// Goals supported
	// by each action in the relaxed plan. For each action store indexes of
	// Goals
	int[][] goalByAction;

	int numSG; // number of subgoals
	int[] subGoals; // List of subgoals
	// The action (ID) in the relaxed plan supporting each subgoal
	int[] subgoalAct;
	// Utility array. Store the _index_ in relaxedPlan[] for
	// action supporting this goal.
	int[] subgoalActIndex;

	ArrayList<Operator> operators;

	boolean[] removedGoal, removedAct;

	float hUtil;

	int[] tempV;

	float improvement;

	public void initialize(int MAXRPSIZE, GoalManager gm,
			ArrayList<Operator> groundActions) {
		this.operators = groundActions;

		maxGoal = gm.numGoal;
		naGoal = 0;
		achievedGoal = new int[maxGoal];
		nogoodGoal = new int[maxGoal];

		gbaCount = new int[MAXRPSIZE];
		Arrays.fill(gbaCount, 0);
		goalByAction = new int[MAXRPSIZE][maxGoal];

		numSG = 0;
		subGoals = new int[MAXRPSIZE * 2];
		subgoalAct = new int[2 * MAXRPSIZE];
		subgoalActIndex = new int[2 * MAXRPSIZE];

		removedGoal = new boolean[maxGoal];
		removedAct = new boolean[MAXRPSIZE];

		this.gm = gm;

		tempV = new int[gm.maxGDSize];

	}

	public void reinitialize(GoalManager gm2) {
		// reset();
		this.gm = gm2;
	}

	/**
	 * Reset the class for each iteration
	 */
	public void reset() {

		Arrays.fill(gbaCount, 0);
		Arrays.fill(removedGoal, false);
		Arrays.fill(removedAct, false);

		numSG = 0;
		naGoal = 0;

	}

	/**
	 * Add new subgoal. It's always added with the supporting action. Action can
	 * be new or already existed in the relaxed plan.
	 */
	public void addSubGoal(int sgID) {
		int i, actIndex;

		// Subgoal is already added (precond of another processed action)
		// for (i = 0; i < numSG; i++) {
		// if (sgID == subGoals[i])
		// return;
		// }

		// System.out.println("Add subgoals: sgID = " + sgID + " | aID = " +
		// aID);
		if (numSG >= subGoals.length) {
			subGoals = Utility.grow(subGoals);
			subgoalAct = Utility.grow(subgoalAct);
		}
		subGoals[numSG] = sgID;
		subgoalAct[numSG++] = 0;
	}

	public void addSubGoal(int sgID, int aID, int[] relaxedPlan, int size) {
		int i, actIndex;

		// Subgoal is already added (precond of another processed action)
		// for (i = 0; i < numSG; i++) {
		// if (sgID == subGoals[i])
		// return;
		// }

		// System.out.println("Add subgoals: sgID = " + sgID + " | aID = " +
		// aID);
		if (numSG >= subGoals.length) {
			subGoals = Utility.grow(subGoals);
			subgoalAct = Utility.grow(subgoalAct);
		}
		subGoals[numSG] = sgID;
		if (aID == 0) {
			subgoalAct[numSG++] = aID;
			return;
		}
		if (aID < 0) {
			subgoalAct[numSG++] = -aID;
			actIndex = Utility.indexOf(relaxedPlan, -aID, size);
		} else {
			subgoalAct[numSG++] = aID;
			actIndex = size - 1;
		}

		if (actIndex < 0) {
			assert false;
			return;
		}

		subgoalActIndex[numSG - 1] = actIndex;
	}

	public void identifyBeneficialGoals(HashMap<Integer, Float> achievedGoals,
			int[] relaxedPlan, int nRelaxedPlan) {
		improvement = 0.0f;
		this.relaxedPlan = relaxedPlan;
		this.nRelaxedPlan = nRelaxedPlan;
		setupGoal(achievedGoals.keySet());
		buildGoalSupportTree();
		findNogoodActions();
//		setHeuristic();
	}

//	/**
//	 * First round: Build the supporting tree stored in goalByAction[][] and the
//	 * associated Utility
//	 */
//	public void buildGoalSupportTree_SapaPS() {
//		int i, j, spAct;
//
//		// Now build the Utility goalByAction[][]
//		int[] tempSubGoal = new int[numSG];
//		int numTempSG = 0, sgIndex, spaIndex;
//		boolean[] processed = new boolean[numSG];
//
//		for (int topGoalIndex = 0; topGoalIndex < naGoal; topGoalIndex++) {
//			tempSubGoal[numTempSG++] = topGoalIndex;
//
//			for (j = 0; j < numSG; j++)
//				processed[j] = false;
//
//			while (numTempSG > 0) {
//				sgIndex = tempSubGoal[--numTempSG];
//				processed[sgIndex] = true;
//
//				spAct = subgoalAct[sgIndex];
//				if (spAct < 0) // A_init
//					continue;
//
//				spaIndex = subgoalActIndex[sgIndex];
//				// Index in
//				// relaxedPlan[] of
//				// action supporting
//				// this subgoal
//				if ((gbaCount[spaIndex] == 0)
//						|| ((gbaCount[spaIndex] > 0) && (goalByAction[spaIndex][gbaCount[spaIndex] - 1] != topGoalIndex)))
//					goalByAction[spaIndex][gbaCount[spaIndex]++] = topGoalIndex;
//
//				// Now add to the subgoal list the set of preconditions of each
//				// selected action
//				Operator act = operators.get(spAct);
//				for (Condition c : act.conditionList) {
//					int pID = c.id;
//					sgIndex = getSubGoalIndex(pID);
//
//					if (sgIndex < 0) {
//						assert false;
//						continue;
//					}
//
//					if (!processed[sgIndex])
//						tempSubGoal[numTempSG++] = sgIndex;
//				}
//				for (ProtectCondition c : act.protectConditionList) {
//					int pID = c.id;
//					sgIndex = getSubGoalIndex(pID);
//
//					if (sgIndex < 0) {
//						assert false;
//						continue;
//					}
//
//					if (!processed[sgIndex])
//						tempSubGoal[numTempSG++] = sgIndex;
//				}
//			}
//		}
//	}

	/**
	 * Second round: Use the goal supportIDs tree to identify unbeneficial Goals
	 * NOTE: For possible improvement, we can rerun the following function
	 * multiple times.
	 */
	private void findNogoodActions() {
		int i, j, k, goalIndex1, goalIndex2, actIndex;
		// Setup the list of supporting actions for each (set of) goal
		// Note: Each entry is the action INDEX in relaxedPlan[] array
		int[][][] supportAct = new int[naGoal][naGoal][nRelaxedPlan];
		int[][] nSP = new int[naGoal][naGoal];

		// Array to check if a given goal is "hard" or "soft". We only remove
		// "soft" Goals but have to fulfill all "hard" Goals
		boolean[] hardGoalFlag = new boolean[naGoal];
		for (i = 0; i < naGoal; i++)
			hardGoalFlag[i] = gm.getType(achievedGoal[i]);

		for (i = 0; i < naGoal; i++)
			for (j = 0; j < naGoal; j++)
				nSP[i][j] = 0;

		// Only process actions supporting one or two Goals now. Can increase
		// to bigger subsets of Goals later
		for (i = 0; i < nRelaxedPlan; i++) {
			switch (gbaCount[i]) {
			case 1:
				goalIndex1 = goalByAction[i][0];
				supportAct[goalIndex1][goalIndex1][nSP[goalIndex1][goalIndex1]++] = i;
				break;
			case 2:
				goalIndex1 = goalByAction[i][0];
				goalIndex2 = goalByAction[i][1]; // Always: goalIndex2 >
				// goalIndex1 (by the way we
				// set them up)
				supportAct[goalIndex1][goalIndex2][nSP[goalIndex1][goalIndex2]++] = i;
				break;
			default:
			}
		}

		// First: try to remove individual Goals
		float totalCost, totalReward;

		for (i = 0; i < nRelaxedPlan; i++)
			removedAct[i] = false;

		Arrays.fill(removedGoal, false);

		float worstUtility, worstReward = 0.0f, worstCost = 0.0f;
		int worstIdx = -1, worstJdx = -1;

		while (true) {
			worstUtility = Float.POSITIVE_INFINITY;
			for (i = 0; i < naGoal; i++) {
				// Skip "hard" Goals
				if (hardGoalFlag[i] || removedGoal[i])
					continue;
				int predID = achievedGoal[i];
				totalReward = gm.getReward(predID);
				totalReward += gm.getDependencyReward(predID);
				// System.out.println(totalUtil);
				totalCost = 0;

				for (j = 0; j < nSP[i][i]; j++) {
					actIndex = supportAct[i][i][j]; // Index in the relaxed plan
					totalCost += operators.get(relaxedPlan[actIndex]).cost.value;
				}

				// System.out.println("Round1: TotalCost: " + totalCost + " |
				// TotalUtil: " + totalUtil);

				if (totalReward - totalCost <= worstUtility) {
					worstUtility = totalReward - totalCost;
					worstReward = totalReward;
					worstCost = totalCost;
					worstIdx = i;
				}
			}
			if (worstUtility == Float.POSITIVE_INFINITY) return;
			if (worstUtility < -gm.getPenalty(achievedGoal[worstIdx])) {
				improvement -= worstUtility;

				removedGoal[worstIdx] = true;
				gm.setInactive(achievedGoal[worstIdx]);

				// System.out.println("............Removed goal " + i);

				// Remove all actions only associated with this goal
				for (j = 0; j < nSP[worstIdx][worstIdx]; j++) {
					removedAct[supportAct[worstIdx][worstIdx][j]] = true;
					// System.out.println("............Removed act " +
					// supportAct[i][i][j]);
					nra++;
				}

				for (i = 0; i < naGoal; i++)
					for (j = 0; j < naGoal; j++)
						nSP[i][j] = 0;

				// Only process actions supporting one or two Goals now. Can increase
				// to bigger subsets of Goals later
				for (i = 0; i < nRelaxedPlan; i++) {
					if (removedAct[i]) continue;
//					 System.out.println("gbaCount[i] = " + gbaCount[i]);
					int count = 0;
					for (j = 0;j<gbaCount[i];j++) {
						int gIdx = goalByAction[i][j];
						if (!removedGoal[gIdx]) {
							goalByAction[i][count] = gIdx;
							count++;
						}
					}
					gbaCount[i] = count;
					switch (count) {
					case 1:
						goalIndex1 = goalByAction[i][0];
						supportAct[goalIndex1][goalIndex1][nSP[goalIndex1][goalIndex1]++] = i;
						break;
					case 2:
						goalIndex1 = goalByAction[i][0];
						goalIndex2 = goalByAction[i][1]; // Always: goalIndex2 >
						// goalIndex1 (by the way we
						// set them up)
						supportAct[goalIndex1][goalIndex2][nSP[goalIndex1][goalIndex2]++] = i;
						break;
					default:
					}
				}
			} else {
				worstUtility = Float.POSITIVE_INFINITY;
				// Second: try to remove pair of Goals (one may already be
				// removed by
				// the first run)
				for (i = 0; i < naGoal - 1; i++) {
					if (hardGoalFlag[i])
						continue;

					if (removedGoal[i]) continue;
					
					for (j = i + 1; j < naGoal; j++) {
						if (removedGoal[j]) continue;
						if (hardGoalFlag[j])
							continue;

						if (removedGoal[i] && removedGoal[j]) {
							for (k = 0; k < nSP[i][j]; k++)
								removedAct[supportAct[i][j][k]] = true;
							nra++;
							continue;
						}

						totalReward = totalCost = 0;
						totalReward += gm.getReward(achievedGoal[i])
								+ gm.getDependencyReward(achievedGoal[i]);
						totalReward += gm.getReward(achievedGoal[j])
								+ gm.getDependencyReward(achievedGoal[j]);

						for (k = 0; k < nSP[i][j]; k++) {
							actIndex = supportAct[i][j][k]; // Index in the
							// relaxed plan
							totalCost += operators.get(relaxedPlan[actIndex]).cost.value;
						}

						for (k = 0; k < nSP[i][i]; k++) {
							actIndex = supportAct[i][i][k]; // Index in the
							// relaxed
							// plan
							totalCost += operators
									.get(relaxedPlan[actIndex]).cost.value;
						}

						for (k = 0; k < nSP[j][j]; k++) {
							actIndex = supportAct[j][j][k]; // Index in the
							// relaxed
							// plan
							totalCost += operators
									.get(relaxedPlan[actIndex]).cost.value;
						}

						if (totalReward - totalCost <= worstUtility) {
							worstUtility = totalReward - totalCost;
							worstReward = totalReward;
							worstCost = totalCost;
							worstIdx = i;
							worstJdx = j;
						}
						// System.out.println("Round2: TotalCost: " + totalCost
						// + " |
						// TotalUtil: " + totalUtil);
					}
				}
				if (worstUtility == Float.POSITIVE_INFINITY) return;
				if (worstUtility < (-gm.getPenalty(achievedGoal[worstIdx]) - gm.getPenalty(achievedGoal[worstJdx]))) {
					improvement -= worstUtility;
					for (k = 0; k < nSP[worstIdx][worstJdx]; k++) {
						removedAct[supportAct[worstIdx][worstJdx][k]] = true;
						nra++;
						// System.out.println("Round2: Removed act " +
						// supportAct[worstIdx][worstJdx][k] + "(" +
						// nSP[i][worstJdx]);
					}
					removedGoal[worstIdx] = true;
					gm.setInactive(achievedGoal[worstIdx]);
					// System.out.println("Round2: Removed goal " + worstIdx + "
					// [worstIdx,worstJdx]=" +worstIdx + "," + worstJdx + "
					// nSP[worstIdx][worstJdx] = " + nSP[worstIdx][worstJdx]);
					for (k = 0; k < nSP[worstIdx][worstIdx]; k++) {
						removedAct[supportAct[worstIdx][worstIdx][k]] = true;
						nra++; 
					}

					removedGoal[worstJdx] = true;
					gm.setInactive(achievedGoal[worstJdx]);
					// System.out.println("Round2: Removed goal " + worstJdx+
					// "[worstIdx,worstJdx]=" + worstIdx + "," + worstJdx + "
					// nSP[worstIdx][worstJdx] = " +
					// nSP[worstIdx][worstJdx]);
					for (k = 0; k < nSP[worstJdx][worstJdx]; k++) {
						removedAct[supportAct[worstJdx][worstJdx][k]] = true;
						nra++;
					}
					
					for (i = 0; i < naGoal; i++)
						for (j = 0; j < naGoal; j++)
							nSP[i][j] = 0;

					// Only process actions supporting one or two Goals now. Can increase
					// to bigger subsets of Goals later
					for (i = 0; i < nRelaxedPlan; i++) {
						if (removedAct[i]) continue;
//						 System.out.println("gbaCount[i] = " + gbaCount[i]);
						int count = 0;
						for (j = 0;j<gbaCount[i];j++) {
							int gIdx = goalByAction[i][j];
							if (!removedGoal[gIdx]) {
								goalByAction[i][count] = gIdx;
								count++;
							}
						}
						gbaCount[i] = count;
						switch (count) {
						case 1:
							goalIndex1 = goalByAction[i][0];
							supportAct[goalIndex1][goalIndex1][nSP[goalIndex1][goalIndex1]++] = i;
							break;
						case 2:
							goalIndex1 = goalByAction[i][0];
							goalIndex2 = goalByAction[i][1]; // Always: goalIndex2 >
							// goalIndex1 (by the way we
							// set them up)
							supportAct[goalIndex1][goalIndex2][nSP[goalIndex1][goalIndex2]++] = i;
							break;
						default:
						}
					}
				} else
					break;

			}
		}
		// Refine the final relaxed plan by removing nogood Goals and their
		// associated actions
		hUtil = gm.getCurrentReward();
	}

	public void setupGoal(Collection<Integer> achievedGoals) {
		for (Integer g : achievedGoals) {
			achievedGoal[naGoal++] = g;
		}
	}
	
	@Deprecated
	private void setHeuristic() {
		int i, j, k;

		hUtil = 0;
		// add the utility of the chosen Goals
		for (i = 0; i < naGoal; i++)
			if (!removedGoal[i])
				improvement -= gm.getReward(achievedGoal[i]);

		// now reward of goalDep (GAI local functions)
		int[] gdSat = new int[numGd];
		int g;
		for (i = 0; i < numGd; i++)
			gdSat[i] = gm.gdSizes[i];

		// rather inefficient...(Will)
		for (i = 0; i < naGoal; i++) {
			g = achievedGoal[i];
			if (removedGoal[i]) {
				for (j = 0; j < numGd; j++)
					if (gdSat[j] > 0)
						for (k = 0; k < gm.gdSizes[j]; k++)
							if (g == gm.gds[j][k]) {
								gdSat[j] = -1;
								break;
							}
			} else {
				for (j = 0; j < numGd; j++)
					if (gdSat[j] > 0)
						for (k = 0; k < gm.gdSizes[j]; k++)
							if (g == gm.gds[j][k]) {
								gdSat[j]--;
								if (gdSat[j] == 0)
									improvement -= gm.gdUtils[j];
								break;
							}
			}
		}

	}

	public float getImprovement() {
		return improvement;
	}

	public void buildGoalSupportTree() {
		int j, spAct;

		// Now build the Utility goalByAction[][]
		int[] tempSubGoal = new int[numSG];
		int numTempSG = 0, sgIndex, spaIndex, precond;
		boolean[] processed = new boolean[numSG];

		for (int topGoalIndex = 0; topGoalIndex < naGoal; topGoalIndex++) {
			tempSubGoal[numTempSG++] = topGoalIndex;

			for (j = 0; j < numSG; j++)
				processed[j] = false;

			while (numTempSG > 0) {
				sgIndex = tempSubGoal[--numTempSG];
				processed[sgIndex] = true;

				spAct = subgoalAct[sgIndex];
				if (spAct < 0) // A_init
					continue;

				spaIndex = subgoalActIndex[sgIndex];
				// Index in
				// relaxedPlan[] of
				// action supporting
				// this subgoal
				if ((gbaCount[spaIndex] == 0)
						|| ((gbaCount[spaIndex] > 0) && (goalByAction[spaIndex][gbaCount[spaIndex] - 1] != topGoalIndex)))
					goalByAction[spaIndex][gbaCount[spaIndex]++] = topGoalIndex;

				// Now add to the subgoal list the set of preconditions of each
				// selected action
				Operator act = operators.get(spAct);
				for (Condition c : act.conditionList) {
					int pID = c.id;
					sgIndex = getSubGoalIndex(pID);

					if (sgIndex > 0 && !processed[sgIndex])
						tempSubGoal[numTempSG++] = sgIndex;
				}
				for (ProtectCondition c : act.protectConditionList) {
					int pID = c.id;
					sgIndex = getSubGoalIndex(pID);

					if (sgIndex > 0 && !processed[sgIndex]) {
						if (Utility.indexOf(tempSubGoal, sgIndex) < 0) {
							tempSubGoal[numTempSG++] = sgIndex;
						}
					}
				}
			}
		}
	}

	public float hUtil() {
		return hUtil;
	}

	public boolean[] raFlag() {
		return removedAct;
	}

	public int[] achievedGoal() {
		return achievedGoal;
	}

	public int naGoal() {
		return naGoal;
	}

	/**
	 * Utility function. Return the index of a given subgoal in the subGoal[]
	 * array
	 */
	private int getSubGoalIndex(int subgoal) {
		return Utility.indexOf(subGoals, subgoal);
	}

	/**
	 * BM: Jan, 2006. PSP with utility dependencies
	 */
	final static short TRUE = 1;

	final static short FALSE = 0;

	final static short LE = 0;

	final static short EQ = 1;

	final static short GE = 2;

	final static short OF = 3;

	double v[];

	int totalVars, totalConst, maxVars, gIndex, actIndex, gdIndex, numGd;

	HashSet<Integer> selectedGoals = new HashSet<Integer>();

	public HashSet<Integer> getSelectedGoals() {
		selectedGoals.clear();

		for (int i = 0; i < naGoal; i++) {
			int propID = achievedGoal[i];
			if (!removedGoal[i])
				selectedGoals.add(propID);
		}

		return (HashSet<Integer>) selectedGoals;
	}
}
