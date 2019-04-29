/****************************************************************
 Author: Minh B. Do - Arizona State University
 ****************************************************************/
package edu.asu.sapa;

import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;

import edu.asu.sapa.ground.GoalManager;
import edu.asu.sapa.ground.Grounding;
import edu.asu.sapa.ground.Operator;
import edu.asu.sapa.ground.PropDB;
import edu.asu.sapa.ground.State;
import edu.asu.sapa.lifted.Domain;
import edu.asu.sapa.lifted.Problem;
import edu.asu.sapa.parsing.PDDL21Parser;
import edu.asu.sapa.rmtpg.RMTPG;
import edu.asu.sapa.utils.Utility;

/**
 * Planner.java: The main class to actually do the search will use the main DS
 * and utility functions defined in other classes.o
 */
public class Planner {

	public static final float EPSILON = (float) Math.pow(2.0, -7.0);


	public static Problem problem;
	public static Grounding grounding;
	public static ArrayList<Operator> operators;

	public static long timeX;

	/** ******* MAIN FUNCTION ********** */
	public static void main(String args[]) {
		@SuppressWarnings("unused")
		PDDL21Parser parser21 = new PDDL21Parser(System.in);
		Planner sapa = new Planner();

		if (args.length < 2) {
			sapa.printUsage();
		} else {
			sapa.readOptions(args);
		}

		Problem problem = PDDL21Parser.prob;

		FileInputStream pddl_file;
		if (args.length < 1) {
			sapa.readOptions(args);
		}

		/*
		 * Instantiate a parser
		 */
		Date d = new Date();
		timeX = d.getTime(); // Get the starting time of the program

		/** * Parse the Domain specification file *** */
		try {
			pddl_file = new java.io.FileInputStream(args[0]);
			PDDL21Parser.ReInit(pddl_file);
		} catch (java.io.FileNotFoundException e) {
			System.out.println("Domain file " + args[0] + " not found !!!");
			return;
		}

		try {
			PDDL21Parser.parse_domain_pddl();
			if (sapa.debug)
				System.out.println(";;Domain " + ((Domain) problem).name
						+ " successfully parsed!" + " num actions = "
						+ problem.actions.count);
		} catch (edu.asu.sapa.parsing.ParseException e) {
			System.out.println("Exception while parsing domain "
					+ ((Domain) problem).name + "!");
			e.printStackTrace();
			return;
		}

		/** ** Parse the problem file *** */
		try {
			pddl_file = new java.io.FileInputStream(args[1]);
			PDDL21Parser.ReInit(pddl_file);
		} catch (java.io.FileNotFoundException e) {
			System.out.println("Problem file " + args[1] + " not found !!!");
			return;
		}

		try {
			PDDL21Parser.parse_problem_pddl();
			if (sapa.debug)
				System.out.println(";;Problem " + problem.name
						+ " successfully parsed!" + " num objects = "
						+ problem.constants.count);
		} catch (edu.asu.sapa.parsing.ParseException e) {
			System.out.println("Exception while parsing problem "
					+ problem.name + "!");
			e.printStackTrace();
			return;
		}

		sapa.initialize(problem);

//		goalSelection(sapa);

		// loop over reading stdin for updates and writing solutions
		// need a update procedure for stateMan
		// initSearch only if an update happens
		// monitor code goes here; goal selection happens here
		sapa.initSearch();
		float initCost = sapa.init.hCost;
		State goalState = null;
		State best = null;
		float old = 0;
		do {
			best = goalState;
			goalState = sapa.getNextSolution();
			if (goalState != null) {
				System.out.println("\nRETURNED FROM getNextSolution() and not null\n");
				sapa.printSolution(goalState);
				sapa.bestH = goalState.h;
			}
//			sapa.bestH = Float.POSITIVE_INFINITY;
			// if ((sapa.bestBenefit) / (sapa.maxReward - initCost) > 0.8) {
			// sapa.useRelaxedPlan = false;
			// System.out.println("\nVery close");
			// old = sapa.bestBenefit;
			// sapa.queue.clear();
			// sapa.queue.add(goalState, 0);
			// System.out.println("prune");
			// }
		} while (goalState != null);
		if (sapa.interrupted) {
			System.out.println("\nThis is the non-threaded version.");
		}
		if (best != null) {
			if (sapa.debug)
				System.out.println("\nExhausted Queue. Best plan found:");
			sapa.printSolution(best);
		} else {
			if (sapa.debug)
				System.out.println("\nExhausted Queue. No solutions found.");
		}
	}

	public GoalManager gm;
	public State init;

	public StateQ queue = new StateQ(65536);

	private Utility util = new Utility();

	// private boolean builtSoftRMTPG = false;
//	private RMTPG softPG = new RMTPG();
	private GoalManager hardGM;

	private RMTPG hardPG = new RMTPG();
	public int costPropOption = 0;

	public boolean relaxedPlanOption = true;
	public int goalCostOption = 1;
	public int lookaheadOption = -1;
	public boolean haFlag = false;
	public boolean haneFlag = false;

	// public boolean refineGoalFlag = true;
	public boolean res_adj = true;
	// public boolean PSP = false;
	public boolean usePlanGraph;
	public float bestH = Float.POSITIVE_INFINITY;
	private float maxReward = Float.POSITIVE_INFINITY;
	private float bestBenefit = Float.NEGATIVE_INFINITY;
//	private float bestBenefit = -29;
	private float bestMakespan = Float.POSITIVE_INFINITY;
	public boolean autoFlag = false;
	public boolean qualityFlag = true;
	private boolean checkBestHeu;
	private float bestHeuValue;
	private float heuUpperLimit = (float) 10000000.00;
	private int exploredStateLimit;

	private String outfileName = new String("");

	public boolean useRelaxedPlan = false;

	private float G_WEIGHT = 1;
	private float H_WEIGHT = 2f;
	private static final float PRUNE_H_WEIGHT = 0.5f;

	
	private State sp;

	private int generatedState = 0, exploredState = 0;

	private long time1, time2;

	private Date aDate;

	// for RP-execution
	private boolean[] executed = new boolean[500];

	private int prunecounter = 0;

	private float bestDistance = Float.POSITIVE_INFINITY;

	private int autoStep;

	public boolean goalSelect = false;

	// due to unfortunate Java things, we must poll this variable to see if it
	// is time to be interrupted
	public volatile boolean interrupted = false;

	private boolean evaluate(State tempSP) {

		if (!gm.update(tempSP))
			return false;

		if (!hardPG.costPropagation(tempSP))
			return false;

		float heuristicValue = hardPG.getHeuristicValue();

		// if (res_adj && relaxedPlanOption) {
		// float resadjValue = util.resourceAdjustment(rmtpg.getRelaxedPlan(),
		// rmtpg.getRPSize(), tempSP.fluentDB, false);
		// heuristicValue += resadjValue;
		// }
		tempSP.h = heuristicValue;
		tempSP.hCost = hardPG.getHCost();

		fValue = G_WEIGHT * (tempSP.g) + H_WEIGHT * (heuristicValue) + tempSP.a;
//		fValue = G_WEIGHT * (tempSP.g) + (float)Math.pow(2.0,heuristicValue + tempSP.a);
		//fValue += (tempSP.hCost + tempSP.gCost) / (init.hCost + init.gCost);

		if (prune(tempSP))
			return false;

		tempSP.getTime();
		
		int[] children = hardPG.applicableActions();
		tempSP.setPotentialActions(children);

		return true;
	}

	private State ehc() {
		sp = queue.peek().event;
		State[] children = new State[100];
		int nChildren = 0;
		int[] potentialActions;
		State tempSP;
		State[] currentLayer = new State[10000];
		int nCurrentLayer = 0;
		State[] nextLayer = new State[10000];
		int nNextLayer = 0;
		State[] tmp;
		float currentLevel = sp.h;
		float level = currentLevel;
		State best = sp;
		Operator a;
		float nextTime;
		int count;
		top: while (sp != null && !sp.isSolved()) {
			System.out.print(currentLevel + "(" + sp.getCost() + "):");
			potentialActions = sp.potentialOperators;
			for (int id : potentialActions) {
				a = operators.get(id);
				if (!sp.applicable(a))
					continue;

				tempSP = new State(sp);
				if (!tempSP.update(a))
					continue;
				if (!evaluate(tempSP))
					continue;
				if (tempSP.h < level) {
					level = tempSP.h;
					best = tempSP;
				}
				children[nChildren++] = tempSP;
				if (this.useRelaxedPlan) {
					tempSP = useRP(tempSP);
					if (tempSP != null) {
						if (tempSP.h < level) {
							level = tempSP.h;
							best = tempSP;
						}
						children[nChildren++] = tempSP;
					}
				}
			}

			nextTime = sp.getNextTime();
			if (nextTime > sp.time
					&& gm.checkConsistentGoalDeadline(sp.getPropDB(), nextTime)) {
				tempSP = new State(sp);
				generate(tempSP);
				if (tempSP.moveForward() && evaluate(tempSP)) {
					children[nChildren++] = tempSP;
					if (tempSP.h < level) {
						level = tempSP.h;
						best = tempSP;
					}
				}
			}

			if (level < currentLevel) {
				sp = best;
				currentLevel = level;
				nChildren = 0;
			} else if (nChildren == 1) {
				sp = children[0];
				nChildren = 0;
				currentLevel = level;
			} else if (nChildren == 0)
				return null;
			else {
				System.arraycopy(children, 0, nextLayer, 0, nChildren);
				nNextLayer = nChildren;
				nChildren = 0;
				level = Float.POSITIVE_INFINITY;
				count = 1;
				bfs: while (level >= currentLevel && nNextLayer > 0) {
					System.out.print("[" + count + "],");
					++count;
					nCurrentLayer = nNextLayer;
					nNextLayer = 0;
					tmp = currentLayer;
					currentLayer = nextLayer;
					nextLayer = tmp;
					for (int i = 0; i < nCurrentLayer; ++i) {
						sp = currentLayer[i];

						potentialActions = sp.potentialOperators;
						for (int id : potentialActions) {
							a = operators.get(id);
							if (!sp.applicable(a))
								continue;

							tempSP = new State(sp);
							if (!tempSP.update(a))
								continue;
							if (!evaluate(tempSP))
								continue;
							if (tempSP.h < level) {
								level = tempSP.h;
								best = tempSP;
							}
							if (nNextLayer >= nextLayer.length) {
								sp = best;
								currentLevel = level;
								break bfs;
							}
							nextLayer[nNextLayer++] = tempSP;
							if (this.useRelaxedPlan) {
								tempSP = useRP(tempSP);
								if (tempSP != null) {
									if (tempSP.h < level) {
										level = tempSP.h;
										best = tempSP;
									}
									if (nNextLayer >= nextLayer.length) {
										sp = best;
										currentLevel = level;
										break bfs;
									}
									nextLayer[nNextLayer++] = tempSP;
								}
							}
						} // applicable actions

						nextTime = sp.getNextTime();
						if (nextTime > sp.time
								&& gm.checkConsistentGoalDeadline(sp
										.getPropDB(), nextTime)) {
							tempSP = new State(sp);
							generate(tempSP);
							if (tempSP.moveForward() && evaluate(tempSP)) {
								if (tempSP.h < level) {
									level = tempSP.h;
									best = tempSP;
								}
								if (nNextLayer >= nextLayer.length) {
									sp = best;
									currentLevel = level;
									break bfs;
								}
								nextLayer[nNextLayer++] = tempSP;
							}
						}

					}
				}
				sp = best;
				currentLevel = level;
			}
			System.out.println();
		}
		if (sp != null)
			processSolution(sp);
		return sp;
	}

	float fValue;

	/** * Function to generate the search tree until the solution node is found ** */
	synchronized public State getNextSolution() {
		int j;
		State tempSP;
//                System.out.println("Yes, we started.");

		int[] potentialActions;

		Priority<State> wrapper;
		if (sp == null) {
			wrapper = queue.poll();
			//System.out.println("\nFIRST STATE from getNextSolution():\n" 
									//+ wrapper.event.printDetailedState());
		} else {
			wrapper = queue.newElement().set(sp, 0);
		}
		
/*		System.out.println("\nPRINTING detailed state from Planner.getNextSolution():");
		System.out.println(wrapper.event.printDetailedState());
*/		
		for (; wrapper != null; wrapper = queue.poll()) {

			exploredState++;
			sp = wrapper.event;

			tempSP = new State(sp);
			generate(tempSP);
			
			if (tempSP.moveForward()) {
//				fValue = G_WEIGHT * sp.g + H_WEIGHT * sp.h;
				if (evaluate(tempSP))
					queue.add(tempSP, fValue);
			} else {
				if (sp.isSolved()) {
					sp.setSolved(false);
					if (processSolution(sp))
						return sp;
				}
			}
			
			potentialActions = sp.getPotentialActions();

			// branch-and-bound
			if (prune(sp)) {
			continue;
			}

//			if (sp.hCost == 0)
//			continue;

			if (potentialActions != null) {
				for (int aID : potentialActions) {
					Operator a = operators.get(aID);

					if (!sp.applicable(a)) {
						continue;
					}

					tempSP = new State(sp);
					generate(tempSP);

					if (!tempSP.update(a)) {
						// System.out.print('.');
						continue;
					}
					if (!evaluate(tempSP))
						continue;
					queue.add(tempSP, fValue);

					if (useRelaxedPlan) {
						tempSP = useRP(tempSP);
						if (tempSP != null)
							queue.add(tempSP, fValue);
					}
				} // end for loop for finding all applicable actions
			}

			// ask if we are interrupted...if so, stop searching
			if (interrupted)
				return null;
		} // exhaust search queue (yeah right!)

		return null;
	}

	synchronized public void setGrounding(Grounding g) {
		grounding = g;
		initSearch();
	}

	private State useRP(State tempSP) {
		Operator a;
		State newSP;
		int j;
		int[] relaxedPlan;
		int rpSize;
		boolean action = true;
		relaxedPlan = hardPG.getRelaxedPlan();
		rpSize = hardPG.getRPSize();

		Arrays.fill(executed, false);
		// for (j = 0; j < rpSize; j++) {
		// executed[j] = false;
		// }

		tempSP = new State(tempSP);
		generate(tempSP);
		while (action) {
			action = false;
			for (j = rpSize - 1; j >= 0; j--) {
				if (executed[j]) {
					continue;
				}
				a = operators.get(relaxedPlan[j]);
				if (!tempSP.applicable(a))
					continue;

				newSP = new State(tempSP);
				generate(newSP);

				if (!newSP.update(a)) {
					// System.out.print(',');
					continue;
				} else {
					executed[j] = true;
					action = true;
					tempSP = newSP;
					// ++count;
				}
			}
			newSP = new State(tempSP);
			generate(newSP);

			if (newSP.moveForward()) {
				tempSP = newSP;
				action = true;
			}
		}
		// System.out.println();
		if (!evaluate(tempSP))
			return null;
		return tempSP;
	}

	public float getRMTPGHeuristicValue(State si) {
		hardPG.costPropagation(si);
		return hardPG.getHeuristicValue();
	}

	// current list of selected hard goals
	HashSet<Integer> hardGoalSelection = new HashSet<Integer>();

	public boolean debug = true;

	public float getMaxReward() {
		return maxReward;
	}

	synchronized public void setGoals(GoalManager gm, HashSet<Integer> goalSet) {
		this.gm = gm;
		hardPG.setSelectedGoals(goalSet);
		maxReward = gm.getCurrentReward();
		hardGoalSelection = goalSet;
	}

	public HashSet<Integer> getCurrentHardGoals() {
		return hardGoalSelection;
	}

	public boolean haveSamePreds(State s1, State s2) {
		PropDB p1 = s1.getPropDB();
		PropDB p2 = s2.getPropDB();

		if (p1.equals(p2))
			return true;

		return false;
	}

	public void update() {

		timeX = (new Date()).getTime();
		problem.updateGrounding();
		grounding.update();

		init = grounding.init;
		gm = grounding.gm;

		operators = grounding.getOperators();

		if (debug) {
			System.out.println(";;Reachable: Fluents: " + grounding.numFunc()
					+ " Propositions: " + grounding.numProp() + " Actions: "
					+ grounding.numAct());
			System.out.println(";;Dynamic: Fluents: " + grounding.numReachableFluents
					+ " Propositions: " + grounding.numReachableProps + " Actions: " + grounding.numReachableOperators);
		}
		
		/*
		 * Finish initialization. Do some pre-processing.
		 */
		aDate = new Date();
		time1 = aDate.getTime();

		System.out.println(";;Parsing & grounding: " + (time1 - timeX)
				+ " milliseconds.");

		// Pass the Goals & Ground Actions information to the Utility class

		// need to initialize here for the selection of goals 
		// (before initializing the search)
		hardGM = new GoalManager(gm);
		hardPG.buildBiLevelGraph(hardGM, operators, grounding.propositions.symbols);		
	}


	/**
	 * Main routine to solve the problem
	 */
	public void initialize(Problem prob) {
		problem = prob;

		prob.initGrounding();

		grounding = new Grounding(true);
		grounding.initialize(prob);
		
		init = grounding.init;
		gm = grounding.gm;

		operators = grounding.getOperators();

		if (debug) {
			System.out.println(";;Reachable: Fluents: " + grounding.numFunc()
					+ " Propositions: " + grounding.numProp() + " Actions: "
					+ grounding.numAct());
			System.out.println(";;Dynamic: Fluents: " + grounding.numReachableFluents
					+ " Propositions: " + grounding.numReachableProps + " Actions: " + grounding.numReachableOperators);
		}
		
		/*
		 * Finish initialization. Do some pre-processing.
		 */
		aDate = new Date();
		time1 = aDate.getTime();

		if (debug)
			System.out.println(";;Parsing & grounding: " + (time1 - timeX)
					+ " milliseconds.");

		// Pass the Goals & Ground Actions information to the Utility class

		// need to initialize here for the selection of goals 
		// (before initializing the search)
		hardPG.optionSetting(false, costPropOption, relaxedPlanOption,
				goalCostOption, haFlag, haneFlag, lookaheadOption, false,
				false, false);
		hardGM = new GoalManager(gm);
		hardPG.buildBiLevelGraph(hardGM, operators, grounding.propositions.symbols);
	}

	public void initSearch() {
		// state-dependent
		// Preprocessing the maximum value for achieving each resource
		util.initialize(operators, init.fluentDB, grounding.fluents.count);

		bestBenefit = Float.NEGATIVE_INFINITY;
		bestMakespan = 0;
		// Planner.grounding.goals.remove(4); // for zenotravel 4 (soft STRIPS)
		maxReward = gm.getCurrentReward();

		if (debug)
			System.out.println("\n;;<<< Start Searching for Solution >>>");

		generatedState = 1;
		exploredState = 0;

//		changeGoals(init);

		if (!evaluate(init)) {
			if (debug) System.out.println("Problem unsolvable? (maybe?)");
		}
		
		//System.err.println("\nPlanner Initial State: " + init.toString());
		sp = null;
		queue.add(init, fValue);
		// TODO: Super hack. Pointless. Remove.
		/*sp = init;
		printRelaxedPlan(hardPG.getRelaxedPlan(), hardPG.getRPSize(), hardPG.getRelaxedPlanTimes());
		sp = null;*/
		// -----------
		if (useRelaxedPlan) {
			State tempSP = new State(init);
			tempSP = useRP(tempSP);
			if (tempSP != null)
				queue.add(tempSP, fValue);
		}
	}

	@Deprecated
	private static RMTPG softPG = new RMTPG();
	private static HashSet<Integer> currentGoalSet = new HashSet<Integer>();

	@Deprecated
	public static boolean selectObjectives(State s) {
		softPG.costPropagation(s);
		softPG.getHeuristicValue();
		HashSet<Integer> heuristicGoalSet = softPG.getSelectedGoals();
		System.out.println(heuristicGoalSet);
		System.out.println(currentGoalSet);

		if (grounding.gm.maxReward(heuristicGoalSet) > grounding.gm
				.maxReward(currentGoalSet)) {
			currentGoalSet = heuristicGoalSet;
			return true;
		}
		return false;
	}

	@Deprecated
	public static void goalSelection(Planner sapa) {
		softPG.buildBiLevelGraph(grounding.gm, grounding.operators.symbols,
				grounding.propositions.symbols);
		selectObjectives(grounding.init);
		sapa.setGoals(sapa.gm, currentGoalSet);
	}

	private String printAction(float time, String name, float dur, int i) {
		String s = new String();

		s += String.format("%.2f", time + i * EPSILON) + ": ";
		s += name;
		s += "[" + dur + "]\n";

		return s;
	}

	/**
	 * Print the usage to run the planner
	 */
	private void printUsage() {
		System.out
		.println("Usage: java [Sapa-dir].Planner domain.pddl problem.pddl [option]\n"
				+ "Flags: -cp [NUMBER] -norp -gc -la [NUMBER] -noauto -quality -postProcess"
				+ " -ha -hane -noresadj -timelimit [NUMBER] -freq [NUMBER] -outfile [STRING]\n\n"

				// +"\t-gui Use the GUI\n"
				// + "\t-debug Print detailed problems, domains, actions
				// information\n"
				+ "\t-cp                Cost Propagation Option: 0-max; 1-sum (default); 2-Combo\n"
				+ "\t-norp              Turn off Relaxed Plan heuristic (-rp to turn on)\n"
				+ "\t-gc                GoalCost Aggregation Option: 0-max; 1-sum(default); 2-Combo\n"
				+ "\t-la                Lookahead option. Default: lookahead = -1\n"
				+ "\t-noauto            Turn off *auto* running option (-auto to turn on)"
				+ "\t-quality           Try to improve the quality after found first solution (with different options)\n"
				+ "\t-ha                Helpful actions (auto = false)\n"
				+ "\t-hane              Using negative effects of helpful actions (auto=false)\n"
				+ "\t-noresadj          Do not use the resource adjustment technique\n"
				// + "\t-dupa Check duplicate parameters in action
				// description\n"
				+ "\t-timelimit         Time cutoff in seconds (to stop the program)\n"
				+ "\t-freq              Frequency to check the time cutoff limit\n"
				+ "\t                       (e.g number of generated search nodes)\n"
				+ "\t-hw                Weight given to the h value (heuristic = g + hw*h)\n"
				+ "\t-outfile           Output file for the solution.\n"
				+ "\t-psp				Solving PSP problem\n"
				+ "\t-sapaps            Ignore goal dependencies in heuristic (act like SapaPS)\n"
				+ "\t-goalremove        Use SapaPS goal removal technique in heuristic\n"
				// + "\t-hmax Use hMax, admissible PSP heuristic\n"
				+ "\t-userp             Use (as many as possible) actions in the relaxed plan to move forward\n"
				+ "\t-norefinegoal      In PSP, don't use refine steps to remove Goals and actions when estimate heus\n"
				+ "\t-goalselect        Select Goals from the initial state's relaxed plan\n"
				// + "\t-sas Specify the SAS file associated with the
				// given domain\n"
				// + "\t-group Specify the SAS group file associated
				// with the given domain\n"
				+ "\t-outputcostfile    Output the costs file to test other planners like OptiPlan and AltAltPS\n"
				+ "\t-sgPSP             Static Greedy PSP search\n"
				// + "\t-usePlanGraph Use the planning graph to execute
				// RP (forces -userp, SAS and optimalpsp)\n"
				// + "\t-printNodeTime Periodically print the time spent
				// per node\n"
				// + "\t-bound Do not search past root node\n"
				+ "\t-pddl3             PDDL3 simple preferences mode (but still read domain as if PSP)\n"
				+ "\t-forceForward      For optimal search, try to improve the solution using RP\n"
				+ "\t                      (0 = always, 1 = only if improvement (less greedy))\n"
				// + "\t-cplex Use an ILOG CPlex solver (will cause an
				// error if none exists)\n"
				// + "\t-holdbad Bad nodes held with probability equal
				// to distance from current best (PSP)\n"
				+ "(Some) Default options: -cp 1 -la -1 -rp -userp -noauto -quality\n\n");
	}

	/**
	 * Function to print the solution
	 */

	private boolean processSolution(State sp) {
		float benefit = sp.benefit();
		if (benefit >= bestBenefit) {
			if (benefit > bestBenefit) {
				bestBenefit = benefit;
				bestMakespan = sp.time;
			} else if (sp.time < bestMakespan) {
				bestMakespan = sp.time;
			} else
				return false;
		} else
			return false;

		//printSolution(sp);

		return true;
	}

	public void printSolution(State sp) {
		float benefit = sp.benefit();
		ArrayList<Integer> actions = sp.getActions();
		ArrayList<Float> times = new ArrayList<Float>(sp.getActionTimes());
		ArrayList<Float> durs = new ArrayList<Float>(sp.getActionDurations());
		ArrayList<String> actSigList = new ArrayList<String>();
		ArrayList<Operator> gActs = new ArrayList<Operator>();

		aDate = new Date();
		time2 = aDate.getTime();
		if (debug) {
			System.out
			.println("\n;; Search time " + (time2 - time1) + " millisecs");
			time1 = time2;
			System.out.println(";; State generated: " + generatedState
					+ "     State explored: " + exploredState);
	
			// for (int i = 0; i < actions.size(); ++i) {
			// if (times.get(i) + durs.get(i) + 0 * EPSILON > makespan) {
			// makespan = times.get(i) + durs.get(i) + 0 * EPSILON;
			// }
			// }
	
			System.out.println(";; Utility: " + benefit + "  Actions: "
					+ actions.size() + "  Makespan: " + sp.time);
		}
		
		String s;
		for (int i = 0; i < actions.size(); ++i) {
			int actID = actions.get(i);
			Operator act = operators.get(actID);
			s = printAction(times.get(i), act.getName(), durs.get(i), 0);
			System.out.print(s);
		}
		System.out.println(";; EOP");
	}
	
	public void printRelaxedPlan(int[] rp, int size, float [] times) {
		ArrayList<String> actSigList = new ArrayList<String>();
		ArrayList<Operator> gActs = new ArrayList<Operator>();

		aDate = new Date();
		time2 = aDate.getTime();
		if (debug) {
			System.out
			.println("\n;; Search time " + (time2 - time1) + " millisecs");
			time1 = time2;
			System.out.println(";; State generated: " + generatedState
					+ "     State explored: " + exploredState);
	
			// for (int i = 0; i < actions.size(); ++i) {
			// if (times.get(i) + durs.get(i) + 0 * EPSILON > makespan) {
			// makespan = times.get(i) + durs.get(i) + 0 * EPSILON;
			// }
			// }
	
			System.out.println(";; Utility: " + (-sp.gCost) + "  Actions: "
					+ size);
		}
		
		String s;
		for (int i = 0; i < size; ++i) {
			int actID = rp[i];
			Operator act = operators.get(actID);
			s = printAction(times[i], act.getName(), act.getDuration(init), 0);
			System.out.print(s);
		}
		System.out.println(";; EOP");
	}

	private void generate(State sp) {
		++generatedState;
//		if (bestBenefit == Float.NEGATIVE_INFINITY) {
		if (generatedState % (10000) == 0) {
			int size = queue.size();
//			System.out.println("generate: " + this.generatedState + " "
//			+ this.exploredState + " " + prunecounter + " " + size
//			+ " " + maxReward + " " + bestBenefit + " "
//			+ queue.events[0].priority + " "
//			+ queue.events[0].event.getCost() + " "
//			+ queue.events[0].event.hCost + " "
//			+ queue.events[0].event.g + " "
//			+ queue.events[0].event.h);
		}
//		}
	}

	private boolean prune(State sp) {
		float h = sp.h;
		double hh = h;

		/*
		 * hh = h -3; hh = Math.max(0, hh); hh = h * 0.9f; hh = h * 0.99f; hh =
		 * h;
		 */
		// if (h < 5)
		// hh = h;
		// else hh = h - 1;
		// hh=h;
		/*
		 * if (h < 10) hh = h*0.95; if (h < 30) hh = h *0.925; else hh = h -3;
		 */
		// if (h < 1000)
		// hh = h * 0.90;
//		 if (exploredState % 100 == 0) System.out.println(exploredState + " " + sp.getCost() + " " + h);
		if (bestBenefit == Float.NEGATIVE_INFINITY) {
			if (h <= bestH) {
				if (h < bestH || sp.g < bestDistance) {
					bestH = h;
					bestDistance = sp.g;
					// prunecounter += queue.size();
					// queue.clear();
					/*System.out.println("\nOUTPUTTING state (initial): ");
					System.out.println(sp.printDetailedState());*/
					if (debug)
						System.out.println("h: " + h + " g: " + sp.g + " hCost: "
						+ sp.hCost + " gCost: " + sp.totalExecCost()
						+ " generated: " + generatedState + " explored: "
						+ exploredState + " bound: " + maxReward);
					return false;
				}
			}
		} else {
			float estimate = maxReward - (sp.gCost + sp.hCost*PRUNE_H_WEIGHT);
//			System.out.println("estimate: " + estimate);
			if (estimate < bestBenefit ||
					(estimate == bestBenefit && sp.time >= this.bestMakespan)) {
				++prunecounter;
//				 if (prunecounter % 70 == 0) {
//				 System.out.print(".");
//				 }
				if (prunecounter % (5000) == 0) {
//				System.out.println("pruned: " + this.generatedState + " "
//				+ this.exploredState + " " + prunecounter + " "
//				+ queue.size() + " " + maxReward + " "
//				+ bestBenefit + " " + queue.events[0].priority
//				+ " " + sp.g + " " + sp.h + " " + sp.getCost()
//				+ " " + sp.hCost);
				}
				return true;
//				return false;
			}
			if (sp.hCost <= bestH) {
				if (sp.hCost < bestH || sp.gCost < bestDistance) {
					bestH = sp.hCost;
					float hhh = sp.hCost;
					bestDistance = sp.gCost;
					// prunecounter += queue.size();
					// queue.clear();
					/*System.out.println("\nOUTPUTTING state (found better cost)");
					System.out.println(sp.printDetailedState());*/
					System.out.println("h: " + h + " g: " + sp.g + " hCost: "
							+ sp.hCost + " gCost: " + sp.totalExecCost()
							+ " generated: " + generatedState + " explored: "
							+ exploredState + " bound: " + maxReward);
				}
			}
		}
		// if (maxReward - (sp.getCost()+hh) <= -124) {
		// System.out.println(sp.getCost());
		// System.out.println(hh);
		// return true;
		// }

		return false;
	}


	public void clearQueue() {
		queue = null;
		System.gc();
		queue = new StateQ(10);
//		queue.clear();
	}

	/**
	 * Function to parse the running options
	 */
	public void readOptions(String args[]) {
		int i;

		for (i = 2; i < args.length; i++) {

			if (args[i].equalsIgnoreCase("-cp")) {
				if (i + 1 >= args.length) {
					System.out
					.println("No costs propagation value specified. Ignore -cp flag.");
					continue;
				}

				try {
					costPropOption = (new Integer(args[++i])).intValue();
				} catch (NumberFormatException e) {
					costPropOption = 1;
					System.out
					.println("Cost prop. option in INCORRECT format. Ignore -cp flag.");
				}

				if ((costPropOption < 0) || (costPropOption > 2)) {
					System.out
					.println("Valid costPropOption = 0,1, or 2. Use default value");
					costPropOption = 1;
				}
				continue;
			}

			if (args[i].equalsIgnoreCase("-norp")) {
				relaxedPlanOption = false;
				continue;
			}

			if (args[i].equalsIgnoreCase("-rp")) {
				relaxedPlanOption = true;
				continue;
			}

			if (args[i].equalsIgnoreCase("-gc")) {
				if (i + 1 >= args.length) {
					System.out
					.println("Need to specify goal costs option. Ignore -gc flag.");
					continue;
				}

				try {
					goalCostOption = (new Integer(args[++i])).intValue();
				} catch (NumberFormatException e) {
					goalCostOption = 1;
					System.out
					.println("Goal costs option in INCORRECT format. Ignore -gc flag.");
				}

				if ((goalCostOption < 0) || (goalCostOption > 2)) {
					System.out
					.println("Valid goalCostOption = 0,1 or 2. Use default value");
					goalCostOption = 1;
				}
				continue;
			}

			if (args[i].equalsIgnoreCase("-holdbad")) {
				continue;
			}

			if (args[i].equalsIgnoreCase("-goalselect")) {
				goalSelect = true;
				continue;
			}
			if (args[i].equalsIgnoreCase("-nogoalselect")) {
				goalSelect = false;
				continue;
			}

			if (args[i].equalsIgnoreCase("-la")) {
				if (i + 1 >= args.length) {
					System.out
					.println("Need to specify lookahead value. Ignore -la flag.");
					continue;
				}

				try {
					lookaheadOption = (new Integer(args[++i])).intValue();
				} catch (NumberFormatException e) {
					lookaheadOption = 1;
					System.out
					.println("Goal costs option in INCORRECT format. Ignore -gc flag.");
				}

				if ((lookaheadOption < -1) || (lookaheadOption > 2)) {
					System.out
					.println("Valid lookaheadOption = -1,0,1 or 2. Use default value");
					lookaheadOption = -1;
				}
				continue;
			}
			if (args[i].equalsIgnoreCase("-noauto")) {
				autoFlag = false;
				continue;
			}
			if (args[i].equalsIgnoreCase("-auto")) {
				autoFlag = true;
				continue;
			}
			if (args[i].equalsIgnoreCase("-quality")) {
				qualityFlag = true;
				continue;
			}
			if (args[i].equalsIgnoreCase("-noquality")) {
				qualityFlag = false;
				continue;
			}
			if (args[i].equalsIgnoreCase("-ha")) {
				haFlag = true;
				autoFlag = false;
				continue;
			}
			if (args[i].equalsIgnoreCase("-hane")) {
				haneFlag = true;
				haFlag = true;
				autoFlag = false;
				continue;
			}
			if (args[i].equalsIgnoreCase("-resadj")) {
				res_adj = true;
				continue;
			}
			if (args[i].equalsIgnoreCase("-noresadj")) {
				res_adj = false;
				continue;
			}

			if (args[i].equalsIgnoreCase("-postProcess")) {
				continue;
			}
			if (args[i].equalsIgnoreCase("-nopostProcess")) {
				continue;
			}
			if (args[i].equalsIgnoreCase("-hw")) {
				if (i + 1 >= args.length) {
					System.out
					.println("No H_WEIGHT value specified. Ignore -hw flag.");
					continue;
				}

				try {
					H_WEIGHT = (new Integer(args[++i])).intValue();
				} catch (NumberFormatException e) {
					H_WEIGHT = 5;
					System.out
					.println("H_WEIGHT value in INCORRECT format. Ignore -hw flag.");
				}
				continue;
			}
			if (args[i].equalsIgnoreCase("-outfile")) {
				if (i + 1 >= args.length) {
					System.out
					.println("No outfile name specified. Ignore -outfile flag.");
					continue;
				}

				outfileName += args[++i];
				continue;
			}

			if (args[i].equalsIgnoreCase("-debug")) {
				debug = true;
				continue;
			}
			
			if (args[i].equalsIgnoreCase("-userp")) {
				/** use (as many as possible) actions in the RP to move forward */
				useRelaxedPlan = true;
				continue;
			}
			if (args[i].equalsIgnoreCase("-nouserp")) {
				useRelaxedPlan = false;
				continue;
			}

			if (args[i].equalsIgnoreCase("-pddl3")) {
				System.out.println(";; PDDL3 mode");
				continue;
			}

			System.out.println("Ignore incorrect flag: " + args[i]);
		}
	};

	public State solve() {
		// return ehc();
		getNextSolution();

		if (sp == null) {
			if (autoFlag) {
				switchAutoOption();
				hardPG.resetHAOption(haFlag, haneFlag);
				return solve();
			} else
				return null;
		} else {
			System.out.println("\nRETURNED FROM getNextSolution() and not null\n");
			return sp;
		}
	}

	private void switchAutoOption() {
		if (autoStep == 1) {
			haFlag = true;
			haneFlag = true;
			System.out.println(";; Switch -ha => -hane: ");
		}

		if (autoStep == 2) {
			haFlag = false;
			haneFlag = false;
			checkBestHeu = false;
			// autoFlag = false;
			// relaxedPlanOption = false;
			System.out.println(";; Switch -hane => no-ha: ");
		}

		autoStep++;
		if (autoStep > 3) {
			autoFlag = false;
		}
	}

}
