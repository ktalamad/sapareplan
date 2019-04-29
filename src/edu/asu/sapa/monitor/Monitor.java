package edu.asu.sapa.monitor;

import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.HashMap;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

import edu.asu.sapa.muri.*;
import edu.asu.sapa.Planner;
import edu.asu.sapa.StateQuery;
import edu.asu.sapa.ground.Grounding;
import edu.asu.sapa.ground.Operator;
import edu.asu.sapa.ground.State;
import edu.asu.sapa.lifted.Constant;
import edu.asu.sapa.lifted.Domain;
import edu.asu.sapa.lifted.Function;
import edu.asu.sapa.lifted.LiftedGoal;
import edu.asu.sapa.lifted.LiftedTest;
import edu.asu.sapa.lifted.LiftedSet;
import edu.asu.sapa.lifted.MathForm;
import edu.asu.sapa.lifted.Predicate; //import edu.asu.sapa.lifted.Problem;
import edu.asu.sapa.lifted.Action;
import edu.asu.sapa.lifted.Symbol;
import edu.asu.sapa.lifted.Term;
import edu.asu.sapa.lifted.Type;
import edu.asu.sapa.lifted.Variable;
import edu.asu.sapa.parsing.PDDL21Parser;
import edu.asu.sapa.parsing.ParseException;
import edu.asu.sapa.parsing.TokenMgrError;
import edu.asu.sapa.rmtpg.RMTPG;

public class Monitor {
	private static RMTPG softPG = new RMTPG();
	private static Grounding grounding;
	private static HashSet<Integer> currentGoalSet;
	private static float bestBenefit = Float.NEGATIVE_INFINITY;
	private static State best = null;
	private static State extBest = null;
        private static final Object bestSync = new Object();
	public static Planner sapa = new Planner();
	private static StateWrapper sw = new StateWrapper();
	private static BlockingQueue<WaitingMessage> eventQueue = new ArrayBlockingQueue<WaitingMessage>(
			1);
	private static PlannerExec plannerExec = new PlannerExec(eventQueue, sapa,
			sw);

	public static ProblemFacade problem;
	private static float bestMakespan = 0;
	
	private static StateQuery sQuery;

	public static void interruptPlanner() throws InterruptedException {
		synchronized (plannerExec) {
			sapa.interrupted = true;
			plannerExec.notify();
			plannerExec.wait();
            bestBenefit = Float.NEGATIVE_INFINITY;
            bestMakespan = problem.initAction.d_dynamic.value;
            problem.groundFirstOrders();
            sapa.update();
            problem.grounding = grounding; // can really do this just once
            if (problem.generateGoals(sapa.init)) sapa.update();            
            softPG.buildBiLevelGraph(grounding.gm,
                              grounding.operators.symbols,
                              grounding.propositions.symbols);
            selectObjectives(sapa.init);
            // KRT: added this code
            /*StateQuery sq1 = new StateQuery(sapa);
            System.out.println("\nSTATE FROM interruptPlanner(): ");
            System.out.println("Current timestamp: " + sapa.init.getTime() + "\n");
            sq1.printState(sapa.init);*/
            //System.out.println("\nEvent Queue: " + sapa.init.events + "\n");
            // ---------
            sapa.queue.clear();
            sapa.initSearch();
		}
	}

	public static void restartPlanner() {
		synchronized (plannerExec) {
			sapa.interrupted = false;
			plannerExec.notify();
		}
	}

    public static void updatePlanner() {
            sapa.update();
    }

	public static State getBest() {
		synchronized (bestSync) {
                        State newBest = extBest;
                        extBest = null;
			return newBest;
		}
	}

	public static boolean selectObjectives(State s) {
		softPG.costPropagation(s);
		softPG.getHeuristicValue();
		HashSet<Integer> heuristicGoalSet = softPG.getSelectedGoals();
//		if (grounding.gm.maxReward(heuristicGoalSet) > grounding.gm
//				.maxReward(currentGoalSet)) {
			currentGoalSet = new HashSet<Integer>(heuristicGoalSet);
			sapa.setGoals(sapa.gm, currentGoalSet);
			System.out.println(";;SELECTED GOALS: " + heuristicGoalSet);
			return true;
//		}
//		return false;
	}
	
/* *******************
 * CREATION OF UPDATES
 * ******************* 
 */

	public static void newFirstOrderConstraint(ArrayList<String> forAlls,
			ArrayList<String> types, String exists, String existsType,
			ArrayList<Predicate> facts, Predicate sensingFact, Predicate goal,
			boolean hard, float deadline, float utility) {

		if (forAlls.size() != types.size()) {
			throw new RuntimeException("sizes on forall and types not equal");
		}

		int size = forAlls.size();
		// Symbol<String> param;
		for (int i = 0; i < size; i++) {
			String fa = forAlls.get(i);
			if (fa.startsWith("?")) {
				// param =
				newVariable(fa, types.get(i));
			}
			// else {
			// // never should happen.
			// param = newObject(fa,types.get(i));
			// }
		}

		Variable extVar = newVariable(exists, existsType);

		LiftedGoal lg = new LiftedGoal(goal, deadline, hard, true, utility,
				null, 0.0f, null);

		problem.addSensingRule(extVar, sensingFact, facts, lg);
	}
	public static Constant newObject(String objName, String type) {
		Constant c;
		c = problem.putConstant(new Constant(objName));
		problem.putType(new Type(type)).constants.add(c); // stamp
		//System.err.println("\nConstant Name: " + objName + " Constant Type: " + type);

		return c;
	}

	public static Variable newVariable(String varName, String type) {
		Variable v;
		v = problem.putVariable(new Variable(varName));
		//problem.putVariable(v); // stamp
                v.setType(problem.putType(new Type(type))); // PWS: more like PDDL21Parser.java

		return v;
	}

        public static void submitNewDeadlineGoal(String factName,
                        ArrayList<String> parameters, boolean hard, float utility,
                        float deadline) {
            LiftedGoal lg = newDeadlineGoal(factName, parameters, hard, utility, deadline);
            problem.addGoal(lg);
            try {
                interruptPlanner();
            } catch (InterruptedException ie) {
            }
            //sapa.update(); // PWS: gets called in interruptPlanner, right?
            restartPlanner();
        }

	public static LiftedGoal newDeadlineGoal(String factName,
			ArrayList<String> parameters, boolean hard, float utility,
			float deadline) {
		Term t = new Term(factName);
		addParameters(t, parameters);
		Predicate p = new Predicate(t);
		problem.putPredicate(p); // stamp it

		LiftedGoal lg = new LiftedGoal(p, deadline, hard, true, utility, null,
				0, null);

		return lg;
	}

	public static LiftedGoal newDeadlineGoal(String factName,
			ArrayList<String> parameters, boolean hard, float deadline) {
		return newDeadlineGoal(factName, parameters, hard, 0, deadline);
	}

        public static void submitNewGoal(String factName,
                        ArrayList<String> parameters, boolean hard, float utility) {
            LiftedGoal lg = newGoal(factName, parameters, hard, utility);
            problem.addGoal(lg);
            try {
                interruptPlanner();
            } catch (InterruptedException ie) {
            }
            //sapa.update(); // PWS: gets called in iP, right?
            restartPlanner();
        }

	public static LiftedGoal newGoal(String factName,
			ArrayList<String> parameters, boolean hard, float utility) {
		// PWS: looks like a bug to me -- utility disappears
		//return newDeadlineGoal(factName, parameters, hard, 0);
		return newDeadlineGoal(factName, parameters, hard, utility, Float.POSITIVE_INFINITY);
	}

	public static Predicate newTimedEvent(String factName,
			ArrayList<String> parameters, boolean add, float time) {
		Term t = new Term(factName);
		addParameters(t, parameters);
		Predicate p = new Predicate(t);

		problem.putPredicate(p); // stamp!
                if (add) 
                    problem.addInitialAdd(p,new MathForm(time));
                else
                    problem.addInitialDelete(p,new MathForm(time));


                //System.out.println(p.id + " " + p.name + " " + p.args + " " + add + " " + time);
		return p;
	}

	public static Predicate newUntimedEvent(String factName,
			ArrayList<String> parameters, boolean add) {
		Term t = new Term(factName);
		addParameters(t, parameters);
		Predicate p = new Predicate(t);

		problem.putPredicate(p); // stamp!
                if (add) 
                    problem.addInitialAdd(p);
                else
                    problem.addInitialDelete(p);

                //System.out.println(p.id + " " + p.name + " " + p.args);
		return p;
	}

	public static Function newFunctionEvent(String funcName,
			ArrayList<String> parameters, float value) {
		Term t = new Term(funcName);
		addParameters(t, parameters);

		Function f = new Function(t); // stamp...

                problem.putFunction(f);
                
                problem.addInitialSet(f, new MathForm(value));
                
                System.out.println(f.id + " " + f.name + " " + f.args + " " + value);
		return f;
	}

	public static Predicate newFact(String factName,
			ArrayList<String> parameters) {
		Term t = new Term(factName);
		addParameters(t, parameters);

		Predicate p = new Predicate(t);
		// no stamp!

		return p;
	}

	public static void setCurrentTime(float now) {
		//System.out.println("\nWE WERE IN setCurrentTime()");
		//problem.initAction.setDuration(new MathForm(now));
		problem.setCurrentTime(now);
	}

	public static void setCost(float cost) {
                problem.initAction.setCost(new MathForm(cost));
        }

	private static void addParameters(Term t, ArrayList<String> parameters) {
		int pSize = parameters.size();
                Constant c;
		for (int i = 0; i < pSize; i++) {
			String param = parameters.get(i);
			if (param.startsWith("?")) {
				t.args.add(problem.putVariable(new Variable(param)));
			} else {
                                c = new Constant(param);
				t.args.add(problem.putConstant(c));
                                //System.out.println("Constant: " + param + " " + c + " " + c.id);
			}
		}
	}

	public static void monitor(Planner planner) {
		// PlannerExec plannerExec = new PlannerExec(eventQueue, planner, sw);
		SimulatorServer simServer = new SimulatorServer(eventQueue);
		Thread plannerThread = new Thread(plannerExec);
		Thread simThread = new Thread(simServer);
		long simThreadID = simThread.getId();
		long plannerThreadID = plannerThread.getId();
		simThread.setName("World State Reader");
		plannerThread.setName("Planner");

		simThread.start();

		plannerThread.start();

		while (true) {
			try {
				WaitingMessage wm = eventQueue.take();
				System.out.flush();
				// can only switch on int
				//System.err.println("BEFORE IF");
				if (wm.threadID == simThreadID) {
					if (problem.name == null) {
						break;
					}
//					synchronized (plannerExec) {
//						planner.interrupted = true;
//						plannerExec.notify();
//						plannerExec.wait();
//					}
//					bestBenefit = Float.NEGATIVE_INFINITY;
//					best = null;
//					bestMakespan = problem.initAction.d_dynamic.value;
//
//					problem.groundFirstOrders();
//					planner.update();
//					softPG.buildBiLevelGraph(grounding.gm,
//							grounding.operators.symbols,
//							grounding.propositions.symbols);
//					selectObjectives(planner.init);
//					planner.queue.clear();
//					// eventQueue.clear();
//					planner.initSearch();
					
					interruptPlanner();
					synchronized (simServer) {
						simServer.updating = false;
						simServer.notify();
					}

					synchronized (plannerExec) {
						planner.interrupted = false;
						plannerExec.notify();
					}
					// if calling setGrounding in planner or setGoals do this
				} else if (wm.threadID == plannerThreadID) {
					State state = sw.getState();
					// planner.printSolution(state);
					if (state.benefit() > bestBenefit
							|| state.time < bestMakespan
							&& state.benefit() == bestBenefit) {
						bestBenefit = state.benefit();
						bestMakespan = state.time;
						best = state;
						synchronized (bestSync) {						        
						        extBest = best;
						}
						// if (bestBenefit >= 0)
						//System.err.println("INSIDE THE IF AND BEST = " + best);
						//System.out.println(best);
						//System.out.println(sapa.queue);
/*						sQuery = new StateQuery(sapa);
						sQuery.printProps2();*/
						printSolution(best);
						
						
					}
//					 Thread.currentThread().sleep(3000);
					 synchronized (plannerExec) {
						 planner.interrupted = false;
						 plannerExec.notify();
					 }
				} else {
					// (hopefully) impossible
					throw new MainThreadException("Unknown thread ID");
				}
			} catch (InterruptedException e) {
				// also (hopefully) impossible
				throw new MainThreadException("Main thread interrupted", e);
			}
		}
		planner.interrupted = true;
		plannerThread.interrupt();
		simThread.interrupt();
	}

	private static String printAction(float time, String name, float dur, int i) {
		String s = new String();

		s += String.format("%.2f", time + i * Planner.EPSILON) + ": ";
		s += name;
		s += "[" + dur + "]\n";

		return s;
	}

    public static String getSolution(State sp) {
        // float benefit = sp.benefit();
        ArrayList<Integer> actions = sp.getActions();
        ArrayList<Float> times = new ArrayList<Float>(sp.getActionTimes());
        ArrayList<Float> durs = new ArrayList<Float>(sp.getActionDurations());
        // ArrayList<String> actSigList = new ArrayList<String>();
        // ArrayList<Operator> gActs = new ArrayList<Operator>();

        String s = "";
        for (int i = 0; i < actions.size(); ++i) {
            int actID = actions.get(i);
            Operator act = Planner.operators.get(actID);
            if (problem.isSensingAction(Planner.problem.actions.get(act.name.get(0)).name)) {
                s += printAction(times.get(i), act.getName(), durs.get(i), 0);
                break;
            }
            s += printAction(times.get(i), act.getName(), durs.get(i), 0);
        }
        return s;
    }

	public static void printSolution(State sp) {
		// float benefit = sp.benefit();
		ArrayList<Integer> actions = sp.getActions();
		ArrayList<Float> times = new ArrayList<Float>(sp.getActionTimes());
		ArrayList<Float> durs = new ArrayList<Float>(sp.getActionDurations());
		// ArrayList<String> actSigList = new ArrayList<String>();
		// ArrayList<Operator> gActs = new ArrayList<Operator>();

		String s;
		for (int i = 0; i < actions.size(); ++i) {
			int actID = actions.get(i);
			Operator act = Planner.operators.get(actID);
			if (problem.isSensingAction(Planner.problem.actions.get(act.name.get(0)).name)) {
				s = printAction(times.get(i), act.getName(), durs.get(i), 0);
				System.out.print(s);
				break;
			}
			s = printAction(times.get(i), act.getName(), durs.get(i), 0);
			System.out.print(s);
		}
		System.out.println(";; EOP");
	}
	
//	public static ArrayList<Operator> supports(Operator op) {
//		
//	}

	// the main monitor thread (creates and "controls" the others)
	public static void main(String args[]) {
		// set up parser
		@SuppressWarnings("unused")
		boolean usingAPI = false;
		PDDL21Parser parser = new PDDL21Parser(System.in);
		problem = PDDL21Parser.prob;
		// and parse initial domain/problem
		InputStream domainIn;
		InputStream problemIn;
		if (args.length < 2) {
			System.out.println("Need at least two arguments (a domain and problem file name, respectively), use 'java -jar <jarfile> - - <otherargs>' or something similar to read from standard in.");
		}
		try {
			if (args[0].equals("-"))
				domainIn = System.in;
			else if (args[0].equals("apiInput")) {
				usingAPI = true;
				domainIn = null;
				apiDomain3();
			}
			else
				domainIn = new java.io.FileInputStream(args[0]);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			System.err.println("Domain file not found, using standard in.");
			domainIn = System.in;
		}
		try {
			if (args[1].equals("-"))
				problemIn = System.in;
			else
				problemIn = new java.io.FileInputStream(args[1]);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			System.err.println("Problem file not found, using standard in.");
			problemIn = System.in;
		}
		boolean success;
		do {
			try {
				if (!usingAPI) {
					PDDL21Parser.ReInit(domainIn);
					PDDL21Parser.parse_domain_pddl();
				}
				System.out.println(";; parsed domain: "
						+ ((Domain) problem).name);
				//System.err.println("Action table size: " + problem.actions.count);
				PDDL21Parser.ReInit(problemIn);
				PDDL21Parser.parse_problem_pddl();
				System.out.println(";; parsed problem: " + problem.name);
				success = true;
			} catch (ParseException e) {
				e.printStackTrace();
				System.err
						.println("Try again, from the domain, on standard in.");
				if (!usingAPI)
					domainIn = System.in;
				problemIn = System.in;
				success = false;
				PDDL21Parser.prob = new ProblemFacade();
				problem = PDDL21Parser.prob;
			}
		} while (!success);

		Planner.timeX = (new Date()).getTime();
		// set up planner
//		sapa.useRelaxedPlan = false;
		sapa.initialize(problem);
		// grounding = new Grounding(Planner.grounding);
		grounding = Planner.grounding;
                sapa.debug = true;

		// MURI
		// setupDomain();
        problem.groundFirstOrders();
		System.out.println(";; grounded first orders");

		softPG.optionSetting(sapa);
		softPG.buildBiLevelGraph(grounding.gm, grounding.operators.symbols,
				grounding.propositions.symbols);

		currentGoalSet = new HashSet<Integer>();
		selectObjectives(grounding.init); // selectObjectives needs a valid
        // Send updates
		// Only use below invocation if artificial doorway updates needed
		// test();
		
		// currentGoalSet, in particular,
		// one that contains all hard goals
		monitor(sapa);
	}
	
	// need to initialize the problem before setting up the domain and problem; anything else?
	public static void initSapaADE() {
		// set up parser
		@SuppressWarnings("unused")
		// PWS: do I need this, or can I just create a new ProblemFacade?
		PDDL21Parser parser = new PDDL21Parser(System.in);
		problem = PDDL21Parser.prob;
	}

	// domain and problem finished, start the planner
	public static void runSapaADE() {
		Planner.timeX = (new Date()).getTime();
		// set up planner
		sapa.initialize(problem);
		grounding = Planner.grounding;
		sapa.debug = true;

		// MURI
		problem.groundFirstOrders();
		System.out.println(";; grounded first orders");

		softPG.optionSetting(sapa);
		softPG.buildBiLevelGraph(grounding.gm, grounding.operators.symbols,
				grounding.propositions.symbols);

		// currentGoalSet, in particular,
		// one that contains all hard goals
		currentGoalSet = new HashSet<Integer>();
		selectObjectives(grounding.init); // selectObjectives needs a valid
		
		monitor(sapa);
	}
	
	// 
	/*private static void apiDomain () {

		// DOMAIN NAME
		
		PDDLHelper.setDomainName(problem, "SimpleRobot");		
		
		// REQUIREMENTS
		
		PDDLHelper.putDomainRequirement(problem, ":typing");
		PDDLHelper.putDomainRequirement(problem, ":durative-actions");
		
		// TYPES
		
		// creating and putting types		
		ArrayList<Type> typeList = new ArrayList<Type> ();
		
		//typeList.add(locType = problem.putType(new Type("location"));
		//typeList.add(hallType = problem.putType(new Type("hallway"));
		
		// setting the su0b-types; 1 is ST of 0, and 2 is ST of 1.
		
		ArrayList<Type> objSubList = new ArrayList<Type>();
		objSubList.add(locType);
		objType = PDDLHelper.addSubTypes(objType, objSubList);
		typeList.set(0, objType);
		
		ArrayList<Type> locSubList = new ArrayList<Type>();
		locSubList.add(hallType);
		locType = PDDLHelper.addSubTypes(locType, locSubList);
		typeList.set(1, locType);
		
		PDDLHelper.putDomainType(problem, problem.obj);
		
		// PREDICATES
		
		// creating and putting variables to be used in the definition of predicates, below
		Variable locVar1 = PDDLHelper.createVariable("?loc", problem.obj);
		Variable locVar2 = PDDLHelper.createVariable("?loc1", problem.obj);
		Variable locVar3 = PDDLHelper.createVariable("?loc2", problem.obj);
		PDDLHelper.putVariable(problem, locVar1);
		PDDLHelper.putVariable(problem, locVar2);
		PDDLHelper.putVariable(problem, locVar3);

//		Put the below code here to see if problem is solvable then		
//		objSubList = new ArrayList<Type>();
//		objSubList.add(locType);
//		objType = PDDLHelper.addSubTypes(objType, objSubList);
//		typeList.set(0, objType);
//		
//		PDDLHelper.putDomainTypes(problem, typeList);
		
		// creating terms for predicate creation, below
		
		ArrayList<Symbol<String>> tempVarList1 = new ArrayList<Symbol<String>>();
		tempVarList1.add(locVar1);
		Term predTerm1 = PDDLHelper.createTerm("at", tempVarList1);
		
		ArrayList<Symbol<String>> tempVarList2 = new ArrayList<Symbol<String>>();
		tempVarList2.add(locVar2);
		tempVarList2.add(locVar3);

		Term predTerm2 = PDDLHelper.createTerm("connected", tempVarList2);
		
		// creating and putting domain predicates
		
		// the predicate (at ?loc - location)
		Predicate pred1 = PDDLHelper.createPredicate(predTerm1);

		// the predicate (connected ?loc1 ?loc2 - location)
		Predicate pred2 = PDDLHelper.createPredicate(predTerm2);
		
		ArrayList<Predicate> predList = new ArrayList<Predicate> ();
		predList.add(pred1);
		predList.add(pred2);
		
		PDDLHelper.putPredicate(problem, pred1);
		PDDLHelper.putPredicate(problem, pred2);
		
		// FUNCTIONS		
		 No functions in this domain; costs and durations can be set dynamically
		 If functions were to exist, procedure would mostly mimic above (for predicates) 
		
		// ACTIONS / OPERATORS		
		// Only 1 action in this domain; move
		
		// MOVE ACTION SCOPE STARTS
		
		Action moveAction = PDDLHelper.createAction("move");
		
		// Creating predicate conditions start, overall and end
		
		MathForm move_cost = PDDLHelper.createMathForm(23.3f);
		MathForm move_duration = PDDLHelper.createMathForm(100.3f);
		
		// NEED TO CREATE NEW variables, terms, predicates for this action's scope
		
		ArrayList<Variable> moveActionVarList = new ArrayList<Variable>();		
		Variable fromLoc = PDDLHelper.createVariable("?from", problem.obj);
		moveActionVarList.add(fromLoc);
		Variable toLoc = PDDLHelper.createVariable("?to", problem.obj);
		moveActionVarList.add(toLoc);
		
		ArrayList<Symbol<String>> moveVarList1 = new ArrayList<Symbol<String>>();
		ArrayList<Symbol<String>> moveVarList2 = new ArrayList<Symbol<String>>();
		ArrayList<Symbol<String>> moveVarList3 = new ArrayList<Symbol<String>>();

		moveVarList1.add(fromLoc);		
		Term atFromLocPredTerm = PDDLHelper.createTerm("at", moveVarList1);
		
		moveVarList2.add(toLoc);
		Term atToLocPredTerm = PDDLHelper.createTerm("at", moveVarList2);
		
		moveVarList3.add(fromLoc);
		moveVarList3.add(toLoc);
		Term connectedFromToPredTerm = PDDLHelper.createTerm("connected", moveVarList3);
		
		Predicate atFromLocPred = PDDLHelper.createPredicate(atFromLocPredTerm);
		Predicate atToLocPred = PDDLHelper.createPredicate(atToLocPredTerm);
		Predicate connectedFromToPred = PDDLHelper.createPredicate(connectedFromToPredTerm);
		
		ArrayList<Predicate> moveActionPredList = new ArrayList<Predicate> ();
		moveActionPredList.add(atFromLocPred);
		moveActionPredList.add(atToLocPred);
		moveActionPredList.add(connectedFromToPred);
		
		ArrayList<Predicate> startConds = new ArrayList<Predicate>();
		startConds.add(atFromLocPred);
		
		ArrayList<Predicate> overAllConds = new ArrayList<Predicate>();
		overAllConds.add(connectedFromToPred);
		
		ArrayList<Predicate> endConds = new ArrayList<Predicate>();
		
		// creating LiftedTests
		// no LiftedTests in this domain
		
		ArrayList<Predicate> condPredList = new ArrayList<Predicate>();
		ArrayList<LiftedTest> liftedTestList = new ArrayList<LiftedTest>();
		
		// creating adds and deletes (start and end)
		
		ArrayList<Predicate> startAdds = new ArrayList<Predicate>();
		
		ArrayList<Predicate> endAdds = new ArrayList<Predicate>();
		endAdds.add(atToLocPred);
		
		ArrayList<Predicate> startDeletes = new ArrayList<Predicate>();
		startDeletes.add(atFromLocPred);
		
		ArrayList<Predicate> endDeletes = new ArrayList<Predicate>();		
		
		// creating LiftedSets		
		// no LiftedSets in this domain
		
		ArrayList<Predicate> setPredList = new ArrayList<Predicate>();
		ArrayList<LiftedSet> liftedSetList = new ArrayList<LiftedSet>();
		
		moveAction = PDDLHelper.createAction(moveAction, problem, move_cost, move_duration, 
						new ArrayList<Constant> (), new ArrayList<Function> (), moveActionPredList, moveActionVarList,
							startConds,	overAllConds, endConds, condPredList, liftedTestList, 
								startAdds, endAdds,	startDeletes, endDeletes, 
									setPredList, liftedSetList);
		
		PDDLHelper.putAction(problem, moveAction);
		
	}
	*/
	/**
	 * API Domain 3 method to test API version of 'meet' action inspired domain.
	 * @author KRT
	 */
	public static void apiDomain3 () {
		
		// INITIAL DOMAIN SETUP INFORMATION
		// --------------------------------
		
		PDDLHelper.setDomainName(problem, "robot");
		PDDLHelper.putDomainRequirement(problem, ":typing");
		PDDLHelper.putDomainRequirement(problem, ":durative-actions");
		
		// TYPES
		// -----
		
		// Type names first
		
		Type pseudObjectType = problem.putType(new Type("pseudobject"));
		Type locationType = problem.putType(new Type("location"));
		Type agentType = problem.putType(new Type("agent"));
		Type roomType = problem.putType(new Type("room"));
		Type commanderType = problem.putType(new Type("commander"));
		Type actorType = problem.putType(new Type("actor"));
		
		// Type hierarchy next
		
		ArrayList<Type> subList = new ArrayList<Type>();
		
		// pseudobject - object
		
		subList.add(pseudObjectType);
		PDDLHelper.addSubTypes(problem.obj, subList);
		
		// location agent - pseudobject
		
		subList = new ArrayList<Type>();
		subList.add(locationType);
		subList.add(agentType);
		PDDLHelper.addSubTypes(pseudObjectType, subList);
		
		// room - location
		
		subList = new ArrayList<Type>();
		subList.add(roomType);
		PDDLHelper.addSubTypes(locationType, subList);
		
		// commander actor - agent
		
		subList = new ArrayList<Type>();
		subList.add(commanderType);
		subList.add(actorType);
		PDDLHelper.addSubTypes(agentType, subList);
		
		
		// PREDICATES
		// ----------
		
		Term predTerm1 = new Term("at");
		predTerm1.add(problem.putVariable(new Variable("?act", agentType)));
		predTerm1.add(problem.putVariable(new Variable("?loc", locationType)));
		
		Term predTerm2 = new Term("met");
		predTerm2.add(problem.putVariable(new Variable("?act", actorType)));
		predTerm2.add(problem.putVariable(new Variable("?other", agentType)));
		
		problem.putPredicate(new Predicate(predTerm1));
		problem.putPredicate(new Predicate(predTerm2));
		
		// ACTIONS 
		// -------		
		
		// "meet" ACTION
		
		Action act = new Action ("meet");
		problem.putAction(act);
		
		// This condition needs to be set to true right after an action is defined, when
		// the cost, duration, parameters and conditions are being defined. It needs to be 
		// reset to false when the effects are being defined.		
		act.inTheMidstOfDeclaringThingsThatThisActionConditionsUpon=true;
			
		// COST & DURATION

		MathForm move_cost = PDDLHelper.createMathForm(5.0f); // this probably needs to avoid tracking references.  
		MathForm move_duration = PDDLHelper.createMathForm(5.0f); // the parser does sneaky sneaky things.
	
		act.setCost(move_cost);
		act.setDuration(move_duration);
		
		// VARIABLES (for use in this action)
		
		Variable actor = act.createParameter("?act", actorType);
		Variable agent = act.createParameter("?other", agentType);
		Variable location = act.createParameter("?loc", locationType);
		
		// CONDITIONS (adding conditions in)
				
		Term atActorLocPredTerm = new Term("at");
		atActorLocPredTerm.add(act.getVariableRefForConditions(actor));
		atActorLocPredTerm.add(act.getVariableRefForConditions(location));
		act.createCondition(atActorLocPredTerm);
			
		Term atAgentLocPredTerm = new Term("at");
		atAgentLocPredTerm.add(act.getVariableRefForConditions(agent));
		atAgentLocPredTerm.add(act.getVariableRefForConditions(location));
		act.createCondition(atAgentLocPredTerm);	
		
		// None
		
		// LiftedTests
		
		// None
		
		// EFFECTS (ADDS & DELETES)
		
		
		// This variable needs to be reset to false again before adding effects in.
		act.inTheMidstOfDeclaringThingsThatThisActionConditionsUpon=false;
		
		Term metActorAgentPredTerm = new Term ("met");
		metActorAgentPredTerm.add(act.getVariableRefForEffects(actor));
		metActorAgentPredTerm.add(act.getVariableRefForEffects(agent));	
		act.createAdd(metActorAgentPredTerm);		

		// -------------------------
		
	}

	public static void apiDomain2 () {

		// DOMAIN NAME
		
		PDDLHelper.setDomainName(problem, "SimpleRobot");		
		
		// REQUIREMENTS
		
		PDDLHelper.putDomainRequirement(problem, ":typing");
		PDDLHelper.putDomainRequirement(problem, ":durative-actions");
		
		// TYPES
		
		// creating and putting types		
		ArrayList<Type> typeList = new ArrayList<Type> ();
		typeList.add(problem.obj);		
		Type locType = problem.putType(new Type("location"));
		typeList.add(locType);
		Type hallType = problem.putType(new Type("hallway"));
		typeList.add(hallType);
		
		/* NOTE about problem.obj: The basic type of any PDDL object is "object" by
		 * default. Hence all objects in the domain are trivially of type object. This 
		 * main type can be thought of as the root of the type hierarchy tree. In order to 
		 * account for its special status, there is a "problem.obj" type that stands for it; 
		 * one must always use that (and not define a type with name "object" instead). 
		 */
		
		// setting the sub-types; locType is ST of problem.obj, and hallType is ST of locType.
		
		ArrayList<Type> objSubList = new ArrayList<Type>();
		objSubList.add(locType);
		problem.obj = PDDLHelper.addSubTypes(problem.obj, objSubList);
		typeList.set(0, problem.obj);
		
		ArrayList<Type> locSubList = new ArrayList<Type>();
		locSubList.add(hallType);
		locType = PDDLHelper.addSubTypes(locType, locSubList);
		typeList.set(1, locType);
		
		PDDLHelper.putDomainType(problem, problem.obj);
		PDDLHelper.putDomainType(problem, locType);
		PDDLHelper.putDomainType(problem, hallType);
		
		// PREDICATES
		
		// creating and putting variables to be used in the definition of predicates, below
		
		Variable locVar1 = PDDLHelper.createVariable("?loc", locType);
		Variable locVar2 = PDDLHelper.createVariable("?loc1", locType);
		Variable locVar3 = PDDLHelper.createVariable("?loc2", locType);
		PDDLHelper.putVariable(problem, locVar1);
		PDDLHelper.putVariable(problem, locVar2);
		PDDLHelper.putVariable(problem, locVar3);
		
		// For the first Term, which is for the "at" predicate
		
		ArrayList<Symbol<String>> tempVarList1 = new ArrayList<Symbol<String>>();
		tempVarList1.add(locVar1);
		Term predTerm1 = PDDLHelper.createTerm("at", tempVarList1);
		
		// For the second Term, which is for the "connected" predicate
		
		ArrayList<Symbol<String>> tempVarList2 = new ArrayList<Symbol<String>>();
		tempVarList2.add(locVar2);
		tempVarList2.add(locVar3);
		Term predTerm2 = PDDLHelper.createTerm("connected", tempVarList2);
		
		// creating and putting domain predicates
		
		// the predicate (at ?loc - location)
		Predicate pred1 = PDDLHelper.createPredicate(predTerm1);

		// the predicate (connected ?loc1 ?loc2 - location)
		Predicate pred2 = PDDLHelper.createPredicate(predTerm2);
		
		ArrayList<Predicate> predList = new ArrayList<Predicate> ();
		predList.add(pred1);
		predList.add(pred2);
		
		PDDLHelper.putPredicate(problem, pred1);
		PDDLHelper.putPredicate(problem, pred2);
		
		// FUNCTIONS		
		
		/* No functions in this domain; costs and durations can be set dynamically
		 * If functions were to exist, procedure would mostly mimic above (for predicates) */
		
		// ACTIONS -- Only 1 action in this domain; move
		
		// MOVE ACTION SCOPE STARTS
		
		/* When a new scope starts (for example for a new action), one MUST declare new
		 * variables, terms, predicates etc. that go with that action. One may NOT re-use
		 * the objects declared for making previous constructs. This is why we will be 
		 * (re)creating terms and variables in the code below. */
		
		Action moveAction = PDDLHelper.createAction("move");
		moveAction.setScope(problem);
		
		// Creating an ActionMaker object which will help in the overall creation of the action
		
		ActionMaker moveMaker = new ActionMaker (moveAction, problem);
			
		// COST & DURATION

		MathForm move_cost = PDDLHelper.createMathForm(23.3f);
		MathForm move_duration = PDDLHelper.createMathForm(100.3f);
	
		moveMaker.setCost(move_cost);
		moveMaker.setDuration(move_duration);
		
		// Need to (re)create new variables, terms, predicates for this action's scope
		
		// VARIABLES
		
		Variable fromLoc = PDDLHelper.createVariable("?from", locType);
		moveMaker.addVariable(fromLoc);
		Variable toLoc = PDDLHelper.createVariable("?to", locType);
		moveMaker.addVariable(toLoc);
		
		// TERMS & PREDICATES
		
		ArrayList<Symbol<String>> moveVarList1 = new ArrayList<Symbol<String>>();
		ArrayList<Symbol<String>> moveVarList2 = new ArrayList<Symbol<String>>();
		ArrayList<Symbol<String>> moveVarList3 = new ArrayList<Symbol<String>>();

		moveVarList1.add(fromLoc);		
		Term atFromLocPredTerm = PDDLHelper.createTerm("at", moveVarList1);
		
		moveVarList2.add(toLoc);
		Term atToLocPredTerm = PDDLHelper.createTerm("at", moveVarList2);
		
		moveVarList3.add(fromLoc);
		moveVarList3.add(toLoc);
		Term connectedFromToPredTerm = PDDLHelper.createTerm("connected", moveVarList3);
		
		Predicate atFromLocPred = PDDLHelper.createPredicate(atFromLocPredTerm);
		Predicate atToLocPred = PDDLHelper.createPredicate(atToLocPredTerm);
		Predicate connectedFromToPred = PDDLHelper.createPredicate(connectedFromToPredTerm);
		
		moveMaker.addPredicate(atFromLocPred);
		moveMaker.addPredicate(atToLocPred);
		moveMaker.addPredicate(connectedFromToPred);
		
		// CONDITIONS
		
		moveMaker.addStartCond(atFromLocPred);
		
		moveMaker.addOverAllCond(connectedFromToPred);
		
		// no end condition for this action
		
		// LiftedTests
		
		// no LiftedTests in this action
		
		// ADDS & DELETES
		
		// no start Adds
		
		moveMaker.addEndAdd(atToLocPred);
		
		moveMaker.addStartDelete(atFromLocPred);
		
		// no end Deletes		
		
		// LIFTEDSETS
		
		// no LiftedSets in this action
		
		// IMPORTANT
		/* The below method MUST be called in order to combine everything
		* into the action reference */
		
		moveMaker.amalgamate();
		
		PDDLHelper.putAction(problem, moveMaker.getActionRef());		
	}
	
	private static void setupDomain() {
		// set up external sensing variable
		Variable extVar = new Variable("?bx");
		extVar.setType(problem.putType(new Type("box")));
		extVar = problem.putVariable(extVar);

		// setup "for all" variable
		Variable forAllVar = new Variable("?z");
		forAllVar.setType(problem.putType(new Type("zone")));
		problem.putVariable(forAllVar);

		// setup sensing predicate
		Term t = new Term("looked_for");
		t.args.add(problem.putVariable(new Variable("?bx")));
		t.args.add(problem.putVariable(new Variable("?z")));
		// Predicate fop = problem.putPredicate(new Predicate(t));
		Predicate fop = new Predicate(t);

		// setup conjunctive formula
		ArrayList<Predicate> preds = new ArrayList<Predicate>();
		t = new Term("has_property");
		t.args.add(problem.putVariable(new Variable("?bx")));
		Constant c;
		t.args.add(c = problem.putConstant(new Constant("green")));
		problem.putType(new Type("color")).constants.add(c);
		// preds.add(problem.putPredicate(new Predicate(t)));
		preds.add(new Predicate(t));
		t = new Term("in");
		t.args.add(problem.putVariable(new Variable("?bx")));
		t.args.add(problem.putVariable(new Variable("?z")));
		// preds.add(problem.putPredicate(new Predicate(t)));
		preds.add(new Predicate(t));

		// set up lifted goal
		Predicate goalPred;
		float deadline = Float.POSITIVE_INFINITY;
		boolean isHard = false;
		boolean constantUtil = true;
		float reward = 900;
		float penalty = 0;
		t = new Term("reported");
		t.args.add(problem.putVariable(new Variable("?bx")));
		t.args.add(problem.putConstant(new Constant("green")));
		t.args.add(problem.putVariable(new Variable("?z")));
		goalPred = new Predicate(t);
		// goalPred = problem.putPredicate(new Predicate(t));
		LiftedGoal lg = new LiftedGoal(goalPred, deadline, isHard,
				constantUtil, reward, null, penalty, null);

		problem.addSensingRule(extVar, fop, preds, lg);
	}

}

class SimulatorServer implements Runnable {

	private BlockingQueue<WaitingMessage> eventQueue;

	public boolean updating;

	public SimulatorServer(BlockingQueue<WaitingMessage> eventQueue) {
		this.eventQueue = eventQueue;
	}

	public void run() {
		WaitingMessage wm = new WaitingMessage(Thread.currentThread().getId());
		PDDL21Parser.ReInit(System.in);
		while (true) {
			try {
				synchronized (this) {
					while (updating) {
						this.notify();
						this.wait();
					}
				}
				// get the stinkin' problem updates
				// Problem pu =
				PDDL21Parser.parse_update();
				updating = true;
				// System.out.println(";; DONE PARSING");
				eventQueue.put(wm);
				// PDDL21Parser.ReInit(System.in);
			} catch (ParseException e) {
				PDDL21Parser.ReInit(System.in);
				System.err.println("Parse error:\n" + e.getMessage());
			} catch (TokenMgrError e) {
				PDDL21Parser.ReInit(System.in);
				System.err.println("Parse error:\n" + e.getMessage());
			} catch (InterruptedException e) {
				break;
				// interrupt effectively stops the sim reader
				// without killing the world
			}
		}
	}
}

class PlannerExec implements Runnable {
	BlockingQueue<WaitingMessage> eventQueue;
	Planner planner;
	StateWrapper sw;
	WaitingMessage wm;

	public PlannerExec(BlockingQueue<WaitingMessage> eventQueue,
			Planner planner, StateWrapper sw) {
		this.planner = planner;
		this.eventQueue = eventQueue;
		this.sw = sw;
	}

	public void run() {
		WaitingMessage wm = new WaitingMessage(Thread.currentThread().getId());
		planner.initSearch();
		while (true) {
			try {
//				System.err.println("STATUS OF PLANNER INTERRUPTED: " + planner.interrupted);
				synchronized (this) {
					while (planner.interrupted) {
						this.notify();
						this.wait();						
						// planner.queue.clear();						
						
					}
				}
				State goalState = null;
				goalState = planner.getNextSolution();
				planner.bestH = Float.POSITIVE_INFINITY;
				if (goalState != null) {
					//System.out.println("\nRETURNED FROM getNextSolution() and not null\n");
					sw.setState(goalState);
					synchronized (this) {
						planner.interrupted = true;
					}
					eventQueue.put(wm);
				} else if (!planner.interrupted) {
					if (planner.debug)
						System.out.println("\n; Exhausted Queue.");
					synchronized (this) {
						planner.interrupted = true; // sortof true
					}
				}
			} catch (InterruptedException e) {
				// thread interrupted because world ceased
				return;
			} catch (OutOfMemoryError m) {
				planner.clearQueue();
				synchronized (this) {
					planner.interrupted = true;
				}
				System.err.println("The Legendary Memory Beast of Aaaaarrrrrrggghhh");
				// restart this thread??
			} catch (Exception e) {
			}
		}
	}
}

class MainThreadException extends RuntimeException {

	private static final long serialVersionUID = -748327821940529013L;

	public MainThreadException(String message) {
		super(message);
	}

	public MainThreadException(String message, Exception e) {
		super(message, e);
	}
}
