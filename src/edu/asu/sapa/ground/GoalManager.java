/*
 * Created on Jun 22, 2004
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package edu.asu.sapa.ground;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;

import edu.asu.sapa.Planner;
import edu.asu.sapa.lifted.Problem;

public class GoalManager {
	public int numGoal;

	public int[] id;

	// a quick and dirty solution--need to refactor
	// maps index to goal ID
//	HashMap<Integer, Integer> indexToGoalMap = new HashMap<Integer, Integer>();
	// maps goal ID to index
	HashMap<Integer, Integer> goalToIndexMap;
	public float[] deadline;
	public boolean[] isHard;
	public float[] reward;
	public float[] penalty;

	//int numChosenGoals;

	ArrayList<Integer> sortedSoftGoal = new ArrayList<Integer>();

	public int maxGDSize;
	public int numGd;
	public int[][] gds;
	public int[] gdSizes;
	public float[] gdUtils;

	HashSet<Integer> achievedGoals = new HashSet<Integer>();

	HashMap<Integer, ArrayList<Integer>> goalToDependencyMap = new HashMap<Integer, ArrayList<Integer>>();

	public boolean[] inactiveGoal;

	private Object sp;

	private GoalManager(int ng, int nGd, int maxGd) {
		init(ng,nGd,maxGd);
	}

	final private void init(int ng, int nGd, int maxGd) {
		numGoal = 0;
		goalToIndexMap = new HashMap<Integer, Integer>(ng);
		id = new int[ng];
		deadline = new float[ng];
		isHard = new boolean[ng];
		reward = new float[ng];
		penalty = new float[ng];
		inactiveGoal = new boolean[ng];

		numGd = 0;
		gdSizes = new int[nGd];
		gdUtils = new float[nGd];
		gds = new int[nGd][maxGd];

		// fill gds with -1 so "0" doesn't get mistaken as predicate with ID 0
		for (int i = 0; i < nGd; i++) {
			for (int j = 0; j < maxGd; j++) {
				gds[i][j] = -1;
			}
		}
		maxGDSize = maxGd;
	}

	final private void reinit(int ng, int nGd, int maxGd) {
		numGoal = 0;
		goalToIndexMap.clear();
		if (id.length != ng) {
			id = new int[ng];
			deadline = new float[ng];
			isHard = new boolean[ng];
			reward = new float[ng];
			penalty = new float[ng];
			inactiveGoal = new boolean[ng];
		}


		numGd = 0;
		if (nGd != gdSizes.length) {
			gdSizes = new int[nGd];
			gdUtils = new float[nGd];
			if (maxGd != maxGDSize) {
				maxGDSize = maxGd;
				gds = new int[nGd][maxGd];
				// fill gds with -1 so "0" doesn't get mistaken as predicate with ID 0
				for (int i = 0; i < nGd; i++) {
					for (int j = 0; j < maxGd; j++) {
						gds[i][j] = -1;
					}
				}
			}
		}
	}

	public GoalManager(Problem prob) {
		init(prob.numGoal(), prob.goalDeps.size(), prob.maxGDSize());
	}

	/**
	 * Deep copy constructor.
	 * 
	 * @param gm
	 */
	public GoalManager(GoalManager gm) {
		this.numGoal = gm.numGoal;

		this.id = Arrays.copyOf(gm.id, numGoal);
		goalToIndexMap = new HashMap<Integer, Integer>(gm.goalToIndexMap);
		this.deadline = Arrays.copyOf(gm.deadline, numGoal);
		this.isHard = Arrays.copyOf(gm.isHard, numGoal);
		this.reward = Arrays.copyOf(gm.reward, numGoal);
		this.penalty = Arrays.copyOf(gm.penalty, numGoal);
		// penalty??

		inactiveGoal = new boolean[numGoal];
		this.numGd = gm.numGd;

		this.gdSizes = Arrays.copyOf(gm.gdSizes, gm.numGd);
		this.gdUtils = Arrays.copyOf(gm.gdUtils, gm.numGd);
		int maxGd = 0;
		for (int gdSize : gdSizes) {
			maxGd = Math.max(maxGd, gdSize);
		}
		this.gds = new int[gm.numGd][maxGd];

		for (int i = 0; i < gm.numGd; i++) {
			System.arraycopy(gm.gds[i], 0, gds[i], 0, gdSizes[i]);
		}

		this.maxGDSize = maxGd;
	}

	public void setHardGoals(Collection<Integer> goals) {
		Arrays.fill(inactiveGoal, true);
		for (int gID : goals) {
			int idx = goalToIndexMap.get(gID);
			isHard[idx] = true;
			inactiveGoal[idx] = false;
		}
	}

	public void addGoal(int index, int gid, float gdl, boolean gt, float su, float sp) {
		goalToIndexMap.put(gid, index);
		id[index] = gid;
		deadline[index] = gdl;
		isHard[index] = gt;
		reward[index] = su;
		penalty[index] = sp;
	}

	public void addGoal(int gid, float gdl, boolean gt, float su, float sp) {
		Integer index = goalToIndexMap.get(gid);
		if (index == null) {
			id[numGoal] = gid;
			goalToIndexMap.put(gid, numGoal);
			deadline[numGoal] = gdl;
			isHard[numGoal] = gt;
			reward[numGoal] = su;
			penalty[numGoal] = sp;
			++numGoal;
		} else {
			deadline[index] = gdl;
			isHard[index] = gt;
			reward[index] = su;
			penalty[index] = sp;
		}
	}


	public void addGoalDep(int[] gids, int size, float gdUtil) {
		gdSizes[numGd] = size;
		gdUtils[numGd] = gdUtil;

		for (int i = 0; i < size; i++) {
			gds[numGd][i] = gids[i];
			// assuming that the goal will be there
			// (since we don't handle when its not)
			int idx = goalToIndexMap.get(gids[i]);
//			idx = Arrays.indexOf(id, gids[i]);
			ArrayList<Integer> depList = goalToDependencyMap.get(idx);
			if (depList == null) {
				depList = new ArrayList<Integer>();
			}
			depList.add(numGd);
			goalToDependencyMap.put(idx, depList);
		}

		numGd++;
	}

	/**
	 * Check if the predicate set P at a given time t is consistent with all the
	 * goal's deadline (if some goal G has deadline ti < t, and G is not
	 * achieved in P)
	 */
	public boolean checkConsistentGoalDeadline(PropDB gp, float time) {
		int index;
		Float oFloat;

		for (index = 0; index < numGoal; index++) {
			if (deadline[index] >= time)
				continue;
			oFloat = gp.getTime(id[index]);
			if (oFloat == null || oFloat > deadline[index])
				return false;
		}
		return true;
	}

	public float getDeadline(int predID) {
		for (int i = 0; i < numGoal; i++)
			if (id[i] == predID)
				return deadline[i];

		return Float.POSITIVE_INFINITY;
	}

	public HashMap<Integer, Float> getGoalDeadline() {
		HashMap<Integer, Float> goalTime = new HashMap<Integer, Float>();

		for (int i = 0; i < numGoal; i++)
			goalTime.put(id[i], deadline[i]);

		return goalTime;
	}

	/**
	 * Function to return Goals and deadlines in ArrayLists. Mostly for
	 * compatability with the old way of using Goals in rmtpg.java. May change
	 * and remove those functions later.
	 */
	public ArrayList<Integer> getGoalID() {
		ArrayList<Integer> goals = new ArrayList<Integer>();
		for (int i = 0; i < numGoal; i++)
			goals.add(new Integer(id[i]));

		return goals;
	}

	public HashMap<Integer, Float> getGoalUtil() {
		HashMap<Integer, Float> goalUtil = new HashMap<Integer, Float>();

		for (int i = 0; i < numGoal; i++)
			goalUtil.put(new Integer(id[i]), new Float(reward[i]));

		return goalUtil;
	}

	// NOTE: NEED TO MODIFY THIS ONE LATER
	public HashMap<Integer, Float> getHardGoalDeadline() {
		HashMap<Integer, Float> goalTime = new HashMap<Integer, Float>();

		for (int i = 0; i < numGoal; i++) {
			if (isHard[i])
				goalTime.put(id[i], deadline[i]);
		}

		return goalTime;
	}

	public float getReward(int predID) {
		float predReward = reward[goalToIndexMap.get(predID)];

		return predReward;
	}
	
	public float getPenalty(int predID) {
		float predPenalty = penalty[goalToIndexMap.get(predID)];
		
		return predPenalty;
	}

	public float getCurrentReward() {
		float reward = 0.0f;
		achievedGoals.clear();

		for (int index = 0; index < this.numGoal; index++) {
			Proposition p = Planner.grounding.propositions.get(id[index]);
			if (p.isConstant && p.value == false) {
				continue;
			}
			if (!inactiveGoal[index]) {
				reward += this.reward[index];
			} else {
				reward -= this.penalty[index];
			}
		}

		int gdSize;
		// utility dependencies
		for (int i = 0; i < numGd; i++) {
			gdSize = gdSizes[i];
			for (int j = 0; j < gdSizes[i]; j++) {
				if (inactiveGoal[goalToIndexMap.get(gds[i][j])]) {
					break;
				}
				gdSize--;
			}
			if (gdSize == 0)
				reward += gdUtils[i];
		}
		return reward;
	}

	public void reset() {
		Arrays.fill(inactiveGoal, false);
	}

	public float getDependencyReward(int predID) {
		float depReward = 0.0f;
		int idx = goalToIndexMap.get(predID);
		ArrayList<Integer> goalDeps = goalToDependencyMap.get(idx);
		if (goalDeps != null) {
			boolean ok;
			for (int depID : goalDeps) {
				ok = true;
				int size = gdSizes[depID];
				for (int i = 0; i < size && ok; i++) {
					// no difference because GD is already broken
					// indexOf inefficient--better to use a map
					ok = (ok && !inactiveGoal[goalToIndexMap.get(gds[depID][i])]);
				}
				if (ok)
					depReward += gdUtils[depID];
			}
		}
		return depReward;
	}

	public void setInactive(int predID) {
		inactiveGoal[goalToIndexMap.get(predID)] = true;
	}

	public boolean getType(int predID) {
		for (int i = 0; i < numGoal; i++)
			if (id[i] == predID)
				return isHard[i];
		return false;
	}

	/**
	 * Check if the given predicate is in a goal or goal dependency.
	 * 
	 * @param pred
	 * @return
	 */
	public boolean isGoal(int pred) {
		for (int goal : id) {
			if (goal == pred)
				return true;
		}

		for (int[] gdArray : gds) {
			for (int goal : gdArray) {
				if (goal == pred)
					// if (pred == 0) System.out.println("yes, in a
					// dependency");
					return true;
			}
		}

		return false;
	}

	public boolean isSolution(State sp) {
		int index;
		HashMap<Integer, Float> timeMap = sp.getPropDB().getTimes();
		Float time;
		for (index = 0; index < this.numGoal; index++) {
			if (!isHard[index])
				continue;
			time = timeMap.get(id[index]);
			if (time == null || deadline[index] < time)
				return false;
		}
		return true;
	}

	@Deprecated
	public float maxReward(Collection<Goal> goals) {
		float reward = 0.0f;

		achievedGoals.clear();
		for (Goal g : goals) {
			achievedGoals.add(g.proposition);
		}

		for (int index = 0; index < this.numGoal; index++) {
			if (achievedGoals.contains(id[index]))
				reward += this.reward[index];
		}

		int gdSize;
		// utility dependencies
		for (int i = 0; i < numGd; i++) {
			gdSize = gdSizes[i];
			for (int j = 0; j < gdSizes[i]; j++) {
				if (achievedGoals.contains(gds[i][j])) {
					gdSize--;
				}
			}
			if (gdSize == 0)
				reward += gdUtils[i];
		}

		return reward;
	}

	public float maxReward(HashSet<Integer> goals) {
		float reward = 0.0f;

		achievedGoals.clear();
		for (int g : goals) {
			Proposition p = Planner.grounding.propositions.get(g);
			if (p.isConstant && p.value == false) {
				continue;
			}
			achievedGoals.add(g);
		}

		for (int index = 0; index < this.numGoal; index++) {
			if (achievedGoals.contains(id[index])) {
				reward += this.reward[index];
			}
		}

		int gdSize;
		// utility dependencies
		for (int i = 0; i < numGd; i++) {
			gdSize = gdSizes[i];
			for (int j = 0; j < gdSizes[i]; j++) {
				if (achievedGoals.contains(gds[i][j])) {
					gdSize--;
				}
			}
			if (gdSize == 0)
				reward += gdUtils[i];
		}

		return reward;
	}

	public void clear() {
		// clear everything that should be cleared
	}

	public int setGoalDeadlines(float[] hardGoals, float[] softGoals) {
		int num = 0;
		Arrays.fill(hardGoals, Float.NaN);
		Arrays.fill(softGoals, Float.NaN);
		for (int i = 0; i < numGoal; i++) {
			if (isHard[i]) {
				hardGoals[id[i]] = deadline[i];
				++num;
			} else {
				if (inactiveGoal[i]) continue;
				softGoals[id[i]] = deadline[i];
			}
		}
		return num;
	}

	public boolean update(State si) {
		PropDB preds = si.getPropDB();
		HashMap<Integer, Float> timeMap = preds.getTimes();

		float now = si.time;

		achievedGoals.clear();
		float reward = 0.0f;

		Float time;
		boolean solved = true;
		for (int index = 0; index < this.numGoal; index++) {
			time = timeMap.get(id[index]);
			if (time == null || time > deadline[index]) {
				if (isHard[index]) {
					if (deadline[index] < now)
						return false;
					else {
						solved = false;
						reward -= penalty[index];
					}
				} else {
					reward -= penalty[index];
				}
			} else {
				achievedGoals.add(id[index]);
				reward += this.reward[index];
			}
		}

		if (solved)
			si.setSolved(true);

		int gdSize;
		// utility dependencies
		for (int i = 0; i < numGd; i++) {
			gdSize = gdSizes[i];
			for (int j = 0; j < gdSizes[i]; j++) {
				if (achievedGoals.contains(gds[i][j])) {
					gdSize--;
				} else
					break;
			}
			if (gdSize == 0)
				reward += gdUtils[i];
		}

		si.setTotalUtility(reward);
		return true;
	}

	public void update(Problem problem) {
		reinit(problem.numGoal(), problem.goalDeps.size(), problem.maxGDSize());		
	}
	
	@Deprecated
	public void deleteGoalAtIndex (int i) {
		int j = numGoal - 1;
		deadline[i] = deadline[j];
		id[i] = id[j];
		inactiveGoal[i] = inactiveGoal[j];
		isHard[i] = isHard[j];
		penalty[i] = penalty[j];
		reward[i] = reward[j];
		numGoal--;
		goalToIndexMap.put(id[i], i);
	}

	public void addGoal(Goal g) {
		Integer index = goalToIndexMap.get(g.proposition);
		if (index == null) {
			id[numGoal] = g.proposition;
			goalToIndexMap.put(g.proposition, numGoal);
			deadline[numGoal] = g.deadline;
			isHard[numGoal] = g.isHard;
			reward[numGoal] = g.rewardConstant;
			penalty[numGoal] = g.penaltyConstant;
			++numGoal;
		} else {
			deadline[index] = g.deadline;
			isHard[index] = g.isHard;
			reward[index] = g.rewardConstant;
			penalty[index] = g.penaltyConstant;
		}
		
	}
	
}
