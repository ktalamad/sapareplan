/************************************************************
 Author: Minh B. Do -- Arizona State University
 *************************************************************/
package edu.asu.sapa.rmtpg;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import edu.asu.sapa.Planner;
import edu.asu.sapa.Priority;
import edu.asu.sapa.PriorityQueue;
import edu.asu.sapa.ground.GoalManager;
import edu.asu.sapa.ground.Operator;
import edu.asu.sapa.ground.Proposition;
import edu.asu.sapa.ground.State;
import edu.asu.sapa.ground.update.Condition;
import edu.asu.sapa.ground.update.Effect;
import edu.asu.sapa.ground.update.ProtectCondition;
import edu.asu.sapa.utils.Utility;

class CostEventQ extends PriorityQueue<CostEvent> {

	@SuppressWarnings("unchecked")
	public CostEventQ() {
		Priority<CostEvent> f = new Priority<CostEvent>();
		typePriorityE = (Class<Priority<CostEvent>>) f.getClass();
		events = (Priority<CostEvent>[])Array.newInstance(typePriorityE, DEFAULT);
		events[0] = f;
	}

	@SuppressWarnings("unchecked")
	public CostEventQ(int capacity) {
		if (capacity < DEFAULT)
			capacity = DEFAULT;
		Priority<CostEvent> f = new Priority<CostEvent>();
		typePriorityE = (Class<Priority<CostEvent>>) f.getClass();
		events = (Priority<CostEvent>[])Array.newInstance(typePriorityE, capacity);
		events[0] = f;
	}

	public CostEventQ(PriorityQueue<CostEvent> queue) {
		super(queue);
	}

	public boolean add(float time, int fID, float cost, int aID, float aTime) {
		Priority<CostEvent> t = newElement();
		if (t.event != null) {
			// the following is only safe if the queue is never copied
			t.event.set(fID, cost, aID, aTime);
			t.priority = time;
		} else {
			t.set(new CostEvent(fID, cost, aID, aTime), time);
		}
		int i = size++;
		int parent;
		for (; (parent = (i - 1) >> 1) >= 0 && t.compareTo(events[parent]) < 0; i = parent) {
			events[i] = events[parent];
		}
		events[i] = t;
		return true;
	}
}

public class RMTPG {
	/* Constants */
	final static int RPMAXSIZE = 500;
	final static boolean statistics = false;

	int costPropOption = 1;
	public boolean relaxedPlanOption = true;
	int goalCostOption = 1;
	boolean haFlag = false;
	boolean haneFlag = false;
	int lookaheadOption = 1;
	public boolean pspFlag = false;

	// ArrayList<Operator> operators;
	// ArrayList<Proposition> propositions;
	State currentState;
	// FluentDB mresDB;

	int capacityActions;
	int capacityFacts;

	ActLink[] actLevel;
	int numActions;
	int[] initialNumPredNotSat;
	int[] numPredNotSat;
	int[] noCondActs = new int[100];
	int numNCA;
	int[] actionCostID;
	float[] maxActionCost;
	float[] sumActionCost;
	float[] zeros;
	int[] readyActions;
	int nReadyActions;
	boolean[] readyFlag;
	int[] potentialActions;
	int nPotentialActions;
	int[] helpfulActions;
	int nHelpfulActions;

	FactLink[] factLevel;
	int numFacts;
	// int[] statics = new int[100];
	// int nStatics = 0;
	float[] hGoalTimes, sGoalTimes;
	int nHGoalTimes;
	float[] hardGoals, softGoals;
	int nHardGoals;
	// float[] sortedGoalTime; // reuse hGoalTime instead

	// need to iterate through achieved goals
	HashMap<Integer, Float> aGoalTime = new HashMap<Integer, Float>();
	// HashMap<Integer, Float> hGoalTime = new HashMap<Integer, Float>();
	// HashMap<Integer, Float> sGoalTime = new HashMap<Integer, Float>();
	// HashMap<Integer, Float> hardGoals = new HashMap<Integer, Float>();
	// HashMap<Integer, Float> softGoals = new HashMap<Integer, Float>();
	// HashMap<Integer, Float> sortedGoalTime = new HashMap<Integer, Float>();

	float ctime;
	CostEventQ eventQueue = new CostEventQ(100);

	GoalSupportTree gsTree = new GoalSupportTree();
	GoalManager gm;

	int[] sortedGoals = new int[100];
	int nSortedGoals;
	float[] goalCosts = new float[100];

	// auto grow rp? rps past 500 seem...painful
	int[] relaxedPlan = new int[100];
	float [] relaxedPlanTimes = new float[100];
	int[] tempRP = new int[100];
	int nRelaxedPlan;
	private float hCost;

	// int[] haPEffects = new int[RPMAXSIZE / 5];
	// int hapeIndex = 0;

	public RMTPG() {
	}

	private void activateCondition(int aID) {
		if (--numPredNotSat[aID] == 0) {
			// actLevel[aID].ready == 'false'
			// value not reset, but this then-block fires
			// at most once per costPropagation
			readyActions[nReadyActions++] = aID;
			readyFlag[aID] = true;
		}
	}

	private boolean activateInitialCondition(int pID, float ctime) {
		CostFunction<Proposition> f = factLevel[pID];
		f.addCost(ctime, 0, 0, ctime);

		this.ctime = ctime;
		if (!updateGoals(pID))
			return false; // shouldn't happen (Will)

		for (int i = 0; i < f.o.size; ++i) {
			int aID = f.o.dependents[i];
			if (--numPredNotSat[aID] == 0) {
				// actLevel[aID].ready == 'false' (Will)
				// (value not reset, but this then-block fires
				// at most once per costPropagation
				readyActions[nReadyActions++] = aID;
				readyFlag[aID] = true;
				potentialActions[nPotentialActions++] = aID;
			}
		}

		return true;
	}

	public int[] helpfulActions() {
		Operator act;
		int id;
		for (int i = 0; i < nPotentialActions; ++i) {
			id = potentialActions[i];
			if (readyFlag[id])
				continue;
			act = actLevel[id].o;

			for (Effect e : act.effectList) {
				if (!e.value || e.isConstant) {
					break;
				}
				// eTime = actLevel[i].times[0] + e.time.value;
				// if (factLevel[e.id].getSupport(eTime) < 0) {
				// helpfulActions[nHelpfulActions++] = id;
				// break;
				// }
				if (factLevel[e.id].getCost() <= 0) {
					helpfulActions[nHelpfulActions++] = id;
					readyFlag[id] = true;
					break;
				}
			}
		}
		// for (int i = 0; i < nPotentialActions; ++i) {
		// id = potentialActions[i];
		// if (readyFlag[id]) {
		// helpfulActions[nHelpfulActions++] = id;
		// }}
		int[] a = new int[nHelpfulActions];
		System.arraycopy(helpfulActions, 0, a, 0, nHelpfulActions);
		return a;
	}

	public int[] rpActions() {
		int[] a = new int[nHelpfulActions];
		System.arraycopy(helpfulActions, 0, a, 0, nHelpfulActions);
		return a;
	}

	public int[] applicableActions() {
		int[] a = new int[nPotentialActions];
		System.arraycopy(potentialActions, 0, a, 0, nPotentialActions);
		return a;
	}

	/***************************************************************************
	 * Set of functions to build the bi-level graph structure to help calculate
	 * the relaxed plan fast.
	 **************************************************************************/

	public void buildBiLevelGraph(GoalManager gm,
			ArrayList<Operator> operators, ArrayList<Proposition> propositions) {
		numNCA = 0;
		// nStatics=0;
		numActions = operators.size();
		numFacts = propositions.size();
		if (numActions > capacityActions) {
			capacityActions = numActions;
			actLevel = new ActLink[numActions];
			initialNumPredNotSat = new int[numActions];
			numPredNotSat = new int[numActions];
			actionCostID = new int[numActions];
			maxActionCost = new float[numActions];
			sumActionCost = new float[numActions];
			zeros = new float[numActions];
			readyActions = new int[numActions];
			readyFlag = new boolean[numActions];
			potentialActions = new int[numActions];
			helpfulActions = new int[numActions];
			actLevel[0] = new ActLink(operators.get(0));
			int p;
			for (int i = 1; i < numActions; ++i) {
				Operator act = operators.get(i);
				actLevel[i] = new ActLink(act);
				if (!act.isPossible)
					continue;
				p = act.conditionList.length + act.protectConditionList.length;
				for (int j = act.conditionList.length - 1; j >= 0; j--) {
					if (!act.conditionList[j].isConstant)
						break;
					--p;
				}
				for (int j = act.protectConditionList.length - 1; j >= 0; j--) {
					if (!act.protectConditionList[j].isConstant)
						break;
					--p;
				}
				if (p > 0) {
					initialNumPredNotSat[act.id] = p;
				} else {
					// initialNumPredNotSat[act.id] = 0; //already done by java
					if (numNCA >= noCondActs.length) {
						noCondActs = Utility.grow(noCondActs);
					}
					noCondActs[numNCA++] = act.id;
				}
			}
		} else {
			actLevel[0].o = operators.get(0);
			int p;
			for (int i = 1; i < numActions; ++i) {
				Operator act = operators.get(i);
				if (!act.isPossible)
					continue;
				actLevel[act.id].size = 0;
				actLevel[act.id].o = act;
				p = act.conditionList.length + act.protectConditionList.length;
				for (int j = act.conditionList.length - 1; j >= 0; j--) {
					if (!act.conditionList[j].isConstant)
						break;
					--p;
				}
				for (int j = act.protectConditionList.length - 1; j >= 0; j--) {
					if (!act.protectConditionList[j].isConstant)
						break;
					--p;
				}
				if (p > 0) {
					initialNumPredNotSat[act.id] = p;
				} else {
					initialNumPredNotSat[act.id] = 0;
					if (numNCA >= noCondActs.length) {
						noCondActs = Utility.grow(noCondActs);
					}
					noCondActs[numNCA++] = act.id;
				}
			}

		}
		if (numFacts > capacityFacts) {
			capacityFacts = numFacts;
			factLevel = new FactLink[numFacts];
			hGoalTimes = new float[numFacts];
			sGoalTimes = new float[numFacts];
			hardGoals = new float[numFacts];
			softGoals = new float[numFacts];
			factLevel[0] = new FactLink(propositions.get(0));
			Proposition p;
			for (int i = 1; i < numFacts; i++) {
				p = propositions.get(i);
				factLevel[i] = new FactLink(p);
				// if (p.isConstant && p.value) {
				// factLevel[i].addCost(0, 0, 0);
				// if (nStatics >= statics.length) {
				// statics = Utility.grow(statics);
				// }
				// statics[nStatics++] = p.id;
				// }
			}
		} else {
			// (h/s)GoalTime reset by resetBiLevelGraph
			// (hard/soft)Goals reset by gm
			factLevel[0].o = propositions.get(0);
			Proposition p;
			for (int i = 1; i < numFacts; i++) {
				p = propositions.get(i);
				factLevel[i].o = p;
				factLevel[i].size = 0;
				// if (p.isConstant && p.value) {
				// factLevel[i].addCost(0, 0, 0);
				// if (nStatics >= statics.length) {
				// statics = Utility.grow(statics);
				// }
				// statics[nStatics++] = p.id;
				// }
			}
		}

		nReadyActions = 0;
		nPotentialActions = 0;
		nHelpfulActions = 0;

		this.gm = gm;

		nHGoalTimes = nHardGoals = gm.setGoalDeadlines(hardGoals, softGoals);
		gsTree.initialize(RPMAXSIZE, gm, operators);
	}

	public void setGoalManager(GoalManager gm) {
		this.gm = gm;

		nHGoalTimes = nHardGoals = gm.setGoalDeadlines(hardGoals, softGoals);
		gsTree.reinitialize(gm);
	}

	public boolean costPropagation(State currentState) {
		int i;

		this.currentState = currentState;

		ctime = currentState.time;

		resetBiLevelGraph();

		for (Priority<Condition> g : currentState.getConditions()) {
			if (g.event.value) {
				float time = hGoalTimes[g.event.id];
				if (time != time) {
					hGoalTimes[g.event.id] = g.priority;
					++nHGoalTimes;
				} else {
					if (g.priority < time)
						hGoalTimes[g.event.id] = g.priority;
				}
			}
		}
		for (Priority<ProtectCondition> g : currentState.getProProps()) {
			if (g.event.value) {
				float time = hGoalTimes[g.event.id];
				if (time != time) {
					hGoalTimes[g.event.id] = ctime;
					++nHGoalTimes;
				} else {
					hGoalTimes[g.event.id] = ctime;
				}
			}
		}

		for (Map.Entry<Integer, Float> e : currentState.propDB.timeMap.entrySet()) {
			/*if (!activateInitialCondition(e.getKey(), e.getValue())) {
				System.out.println("\n****POINT 1****\n");
				return false;
			}*/
			if (!activateInitialCondition(e.getKey(), ctime)) {
				System.out.println("\n****POINT 1****\n");
				return false;
			}
		}

		for (Priority<Effect> e : currentState.getEvents()) {
			if (e.event.value) {
				eventQueue.add(e.priority, e.event.id, 0, 0, ctime);
			}
		}

		propagateActions();
		// time has 'advanced', check that all overall conditions were achieved.
		// would be even smoother to prevent all deletes of the protections
		// until they expire as well as checking that they hold
		// after the first update
		for (Priority<ProtectCondition> g : currentState.getProProps()) {
			if (g.event.value) {
				// hGoalTimes[id] gets assigned to NaN when goal is achieved
				float time = hGoalTimes[g.event.id];
				if (time == time) {
					System.out.println("\n****POINT 2****\n");
					return false;
				}
			}
		}

		while (nHGoalTimes > 0) {
			if (eventQueue.isEmpty()) {
//				System.out.println("\n****POINT 3****\n");
				return false;
			}
			if (!propagateFacts()) {
//				System.out.println("\n****POINT 4****\n");
				return false;
			}
			propagateActions();
		}

		if (lookaheadOption < 0) {
			while (!eventQueue.isEmpty()) {
				propagateFacts();
				propagateActions();
			}
		} else {
			for (i = lookaheadOption; i > 0 && !eventQueue.isEmpty(); --i) {
				propagateFacts();
				propagateActions();
			}
		}
		return true;
	}

	private boolean effectHandler(float ctime, int fID, float factCost, int aID, float aTime) {
		float old;
		FactLink f = factLevel[fID];
		if (f.size == 0) {
			if (!updateGoals(fID))
				return false;
			f.addCost(ctime, factCost, aID, aTime);
			for (int i = 0; i < f.o.size; ++i) {
				int spActID = f.o.dependents[i];
				updateCost(spActID, factCost, fID);
				activateCondition(spActID);
			}
		} else if (factCost < (old = f.costs[f.size - 1])) {
			f.addCost(ctime, factCost, aID, aTime);
			for (int i = 0; i < f.o.size; ++i) {
				int spActID = f.o.dependents[i];
				reduceCost(spActID, factCost, fID, old);
			}
		}
		return true;
	}

	private boolean effectHandler(Priority<CostEvent> te) {
		return effectHandler(te.priority, te.event.id, te.event.cost,
				te.event.supportID, te.event.supportTime);
	}

	public int[] getAchievedGoal() {
		return gsTree.achievedGoal();
	}

	private float getActionCost(int aID) {
		if (costPropOption == 0)
			return maxActionCost[aID];
		else if (costPropOption == 1)
			return sumActionCost[aID];
		else if (costPropOption == 2)
			return 0.5f * (sumActionCost[aID] + maxActionCost[aID]);
		return Float.POSITIVE_INFINITY;
	}

	private float getDirectHeuristic() {
		float goalCost, sumCost = 0, maxCost = Float.NEGATIVE_INFINITY;
		for (Map.Entry<Integer, Float> g : aGoalTime.entrySet()) {
			goalCost = factLevel[g.getKey()].getCost(g.getValue());
			sumCost += goalCost;
			maxCost = Math.max(maxCost, goalCost);
		}
		if (goalCostOption == 0)
			return maxCost;
		else if (goalCostOption == 1)
			return sumCost;
		else
			return (float) 0.5 * (maxCost + sumCost);
	}

	/**
	 * Main function to get the heuristic values according to the setup options
	 */
	public float getHeuristicValue() {
		if (relaxedPlanOption)
			return relaxedPlanHeuristic();
		else
			return getDirectHeuristic();
	}

	public HashSet<Integer> getSelectedGoals() {
		// if (relaxedPlanOption) {
		// relaxedPlanHeuristic();
		// }
		// (if no RP extraction, then should be all hard goals
		// but this is untested)
		return gsTree.getSelectedGoals();
	}

	public void removeSoftGoals() {
		Arrays.fill(softGoals, Float.NaN);
	}

	public void setSelectedGoals(Collection<Integer> goals) {
		gm.setHardGoals(goals);
		nHGoalTimes = nHardGoals = gm.setGoalDeadlines(hardGoals, softGoals);
	}

	public int[] getRelaxedPlan() {
		return relaxedPlan;
	}
	
	public float[] getRelaxedPlanTimes() {
		return relaxedPlanTimes;
	}

	public int getRPSize() {
		return nRelaxedPlan;
	}

	public float hUtil() {
		return gsTree.hUtil();
	}

	public int naGoal() {
		return gsTree.naGoal();
	}

	/**
	 * Function to set up the running options
	 */
	public void optionSetting(boolean dlf, int cpo, boolean rpo, int gco,
			boolean haf, boolean hanef, int lao, boolean PSP, boolean sgPSP,
			boolean grFlag) {
		// deadlineFlag = dlf;
		costPropOption = cpo;
		relaxedPlanOption = rpo;
		goalCostOption = gco;
		haFlag = haf;
		haneFlag = hanef;
		lookaheadOption = lao;
		pspFlag = PSP;
	}

	private void propagateActions() {
		int actID;
		float actionCost;
		float time;
		for (int i = 0; i < nReadyActions; ++i) {
			actID = readyActions[i];
			actionCost = getActionCost(actID);
			actLevel[actID].addCost(ctime, actionCost, actionCostID[actID],ctime);
			readyFlag[actID] = false;

			Operator act = actLevel[actID].o;
			actionCost += act.cost.value;

			for (Effect e : act.effectList) {
				if (!e.value) {
					break;
				}
				time = e.time.value;
				if (time > 0) {
					eventQueue.add(ctime + time, e.id, actionCost, actID, ctime);
				} else {
					effectHandler(ctime, e.id, actionCost, actID, ctime);
				}
			}
		}
		nReadyActions = 0;
	}

	private boolean propagateFacts() {
		Priority<CostEvent> te = null;
		FactLink f;

		do {
			te = eventQueue.delete();
			f = factLevel[te.event.id];
			if (f.size <= 0 || te.event.cost < f.costs[f.size - 1]) {
				if (!effectHandler(te))
					return false;
			}
			if (te.priority > ctime) {
				break;
			}
		} while (!eventQueue.isEmpty());

		ctime = te.priority; // advance time

		te = eventQueue.peek();
		while (te != null) {
			if (te.priority > ctime) {
				break;
			}
			f = factLevel[te.event.id];
			if (f.size == 0 || te.event.cost < f.costs[f.size - 1]) {
				if (!effectHandler(te))
					return false;
			}
			eventQueue.delete();
			te = eventQueue.peek();
		}
		return true;
	}

	private void reduceCost(int aID, float cost, int fID, float old) {
		Operator act = actLevel[aID].o;
		float m = Float.NEGATIVE_INFINITY;
		int id, m_id;
		float c;
		FactLink f;
		sumActionCost[aID] += cost - old;
		if (costPropOption == 0 || costPropOption == 2) {
			if ((m_id = actionCostID[aID]) != fID) {
				if (costPropOption != 2)
					return;
			} else {
				m = cost;
				for (int i = act.conditionList.length - 1; i >= 0; --i) {
					id = act.conditionList[i].id;
					f = factLevel[id];
					if (f.size > 0) {
						c = f.costs[f.size - 1];
						if (c > m) {
							m = c;
							m_id = id;
						}
					}
				}
				if (costPropOption == 0) {
					if (m < maxActionCost[aID]) {
						maxActionCost[aID] = m;
						actionCostID[aID] = m_id;
					} else
						return;
				} else {
					if (m < maxActionCost[aID]) {
						maxActionCost[aID] = m;
						actionCostID[aID] = m_id;
					}
				}
			}
		} else {
			actionCostID[aID] = fID;
		}

		if (numPredNotSat[aID] == 0 && !readyFlag[aID]) {
			readyActions[nReadyActions++] = aID;
			readyFlag[aID] = true;
		}
	}

	private float refineRelaxedPlan() {
		gsTree.identifyBeneficialGoals(aGoalTime, relaxedPlan, nRelaxedPlan);
		if (gsTree.improvement > 0) {
			boolean[] removedAct = gsTree.raFlag();
			int trpSize = 0;
			for (int i = 0; i < nRelaxedPlan; i++) {
				if (!removedAct[i]) {
					tempRP[trpSize++] = relaxedPlan[i];
				}
			}

			nRelaxedPlan = trpSize;
			System.arraycopy(tempRP, 0, relaxedPlan, 0, nRelaxedPlan);

			return gsTree.improvement;
		}
		return 0;
	}

	public float getHCost() {
		return hCost;
	}

	private float relaxedPlanHeuristic() {
		int aID;
		float aTime;
		float heuValue = 0.0f;
		int gID, gIndex;
		float eTime;
		float gTime;
		hCost = 0.0f;
		Operator act;

		gsTree.reset();
		nRelaxedPlan = 0;
		nHelpfulActions = 0;
		sortTheGoals();

		for (gIndex = 0; gIndex < nSortedGoals; ++gIndex) {
			gID = sortedGoals[gIndex];

			// sortedGoals can become stale
			gTime = hGoalTimes[gID];
			if (gTime != gTime) {
				continue;
			}

			
			int supportIndex = factLevel[gID].getSupportIndex(gTime);
			aID = factLevel[gID].supportIDs[supportIndex];
			aTime = factLevel[gID].supportTimes[supportIndex];
			gTime = factLevel[gID].times[supportIndex];
			//gTime = factLevel[gID].getMinTime(gTime);
			hGoalTimes[gID] = Float.NaN;

			if (aID <= 0) {
				if (aID == 0)
					gsTree.addSubGoal(gID);
				else
					gsTree.addSubGoal(gID, aID, relaxedPlan, nRelaxedPlan);
				continue;
			}

			if (nRelaxedPlan >= relaxedPlan.length) {
				relaxedPlan = Utility.grow(relaxedPlan);
				relaxedPlanTimes = Utility.grow(relaxedPlanTimes);
				this.tempRP = new int[relaxedPlan.length];
			}

			relaxedPlanTimes[nRelaxedPlan] = aTime;
			relaxedPlan[nRelaxedPlan++] = aID;
			gsTree.addSubGoal(gID, aID, relaxedPlan, nRelaxedPlan);

			act = actLevel[aID].o;
			heuValue += 1;
			hCost += act.cost.value;

/*			eIndex = act.indexAdd(gID);
			eTime = act.effectList[eIndex].time.value;
			// without this, cycles easily form.
			aTime = gTime - eTime;
			aTime += Math.ulp(gTime);
			if (aTime + eTime < gTime)
				System.err.println("\nERROR 1: gTime: " + gTime + " aTime+eTime: " + (aTime + eTime));
			if (aTime < actLevel[aID].times[0]) {
				System.err.println("\nERROR ActLevel: gTime: " + gTime 
						+ " aTime+eTime: " + (aTime + eTime) + " eTime: " + eTime);
			}
			aTime = actLevel[aID].getMinTime(aTime);
*/
			// aTime = gTime - eTime;

			for (Effect e : act.effectList) {
				if (!e.value || e.isConstant) {
					break;
				}
				eTime = e.time.value;
				factLevel[e.id].addCost(aTime+eTime, 0, -aID, aTime);
			}

			for (Condition c : act.conditionList) {
				if (!c.value || c.isConstant)
					break;
				if (nSortedGoals >= sortedGoals.length) {
					sortedGoals = Utility.grow(sortedGoals);
				}
				sortedGoals[nSortedGoals++] = c.id;
				gTime = hGoalTimes[c.id];
				if (gTime != gTime || gTime > aTime) {
					hGoalTimes[c.id] = aTime;
				}
			}
			for (Condition c : act.protectConditionList) {
				if (!c.value || c.isConstant)
					break;
				if (nSortedGoals >= sortedGoals.length) {
					sortedGoals = Utility.grow(sortedGoals);
				}
				sortedGoals[nSortedGoals++] = c.id;
				gTime = hGoalTimes[c.id];
				if (gTime != gTime || gTime > aTime) {
					hGoalTimes[c.id] = aTime;
				}
			}

			// if (Utility.indexOf(potentialActions,aID) >= 0) {
			if (actLevel[aID].getCost() <= 0) {// && !readyFlag[aID]) {
				// helpfulActions[nHelpfulActions++] = aID;
				// readyFlag[aID] = true;
				helpfulActions[nHelpfulActions++] = aID;
			}

		}

		if (pspFlag) {
			return heuValue + refineRelaxedPlan();
		}

		return heuValue;
	}

	private void resetBiLevelGraph() {
		int i;
		System.arraycopy(initialNumPredNotSat, 0, numPredNotSat, 0, numActions);
		// Utility.fill(actionCostID, 0, numActions, 0);
		// Utility.fill(maxActionCost, 0, numActions, 0.0f);
		// Utility.fill(sumActionCost, 0, numActions, 0.0f);
		// can skip id's because of the way updateCost works, despite
		// the way reduceCost works (I think)
		System.arraycopy(zeros, 0, maxActionCost, 0, numActions);
		System.arraycopy(zeros, 0, sumActionCost, 0, numActions);
		Arrays.fill(readyFlag, false);
		for (i = 0; i < numActions; i++) {
			actLevel[i].size = 0;
		}
		for (i = 0; i < numFacts; i++) {
			factLevel[i].size = 0;
		}
		eventQueue.clear();
		aGoalTime.clear();
		System.arraycopy(hardGoals, 0, hGoalTimes, 0, numFacts);
		nHGoalTimes = nHardGoals;
		System.arraycopy(softGoals, 0, sGoalTimes, 0, numFacts);
		// readyActions.clear();
		// nReadyActions=0;
		// applicableActions.clear();
		// nPotentialActions=0;
		System.arraycopy(noCondActs, 0, readyActions, 0, numNCA);
		nReadyActions = numNCA;
		System.arraycopy(noCondActs, 0, potentialActions, 0, numNCA);
		nPotentialActions = numNCA;
	}

	public void resetHAOption(boolean haf, boolean hanef) {
		haFlag = haf;
		haneFlag = hanef;
	}

	public void resetLAOption(int lao) {
		lookaheadOption = lao;
	}

	private void sortTheGoals() {
		int i, s = 0;
		int gID;
		float gCost;

		for (Map.Entry<Integer, Float> g : aGoalTime.entrySet()) {
			gID = g.getKey();
			hGoalTimes[gID] = g.getValue();
			gCost = factLevel[gID].getCost(hGoalTimes[gID]);
			for (i = 0; i < s; ++i) {
				if (goalCosts[i] < gCost) {
					break;
				}
			}
			if (s >= sortedGoals.length) {
				int l = sortedGoals.length + (sortedGoals.length >> 1) + 1;
				int[] tmpS = new int[l];
				float[] tmpG = new float[l];
				System.arraycopy(sortedGoals, 0, tmpS, 0, i);
				System.arraycopy(goalCosts, 0, tmpG, 0, i);
				l = sortedGoals.length - i++;
				System.arraycopy(sortedGoals, 0, tmpS, i, l);
				System.arraycopy(goalCosts, 0, tmpG, i, l);
				sortedGoals = tmpS;
				goalCosts = tmpG;
				sortedGoals[--i] = gID;
				goalCosts[i] = gCost;
			} else {
				Utility.insert(goalCosts, i, gCost, s);
				Utility.insert(sortedGoals, i, gID, s);
			}
			++s;
		}
		nSortedGoals = s;
	}

	private void updateCost(int aID, float factCost, int fID) {
		if ((costPropOption == 0) || (costPropOption == 2)) {
			if (maxActionCost[aID] < factCost) {
				maxActionCost[aID] = factCost;
				actionCostID[aID] = fID;
			}
		}
		if ((costPropOption == 1) || (costPropOption == 2)) {
			sumActionCost[aID] += factCost;
		}
	}

	// ignore pending conditions in terms of rewards/penalties
	private boolean updateGoals(int pID) {
		float time;
		time = hGoalTimes[pID];
		// false if NaN
		if (time == time) {
			if (ctime > time) {
//				System.out.println("ctime: " + ctime + " time: " + time);
				return false;
			}
			hGoalTimes[pID] = Float.NaN;
			--nHGoalTimes;
			aGoalTime.put(pID, time);
			return true;
		}
		time = sGoalTimes[pID];
		if (time == time) {
			if (ctime <= time) {
				sGoalTimes[pID] = Float.NaN;
				aGoalTime.put(pID, time);
			}
		}
		return true;
	}

	public void optionSetting(Planner p) {
		// deadlineFlag = dlf;
		costPropOption = p.costPropOption;
		relaxedPlanOption = p.relaxedPlanOption;
		goalCostOption = p.goalCostOption;
		haFlag = p.haFlag;
		haneFlag = p.haneFlag;
		lookaheadOption = p.lookaheadOption;
		pspFlag = true;
	}
}
