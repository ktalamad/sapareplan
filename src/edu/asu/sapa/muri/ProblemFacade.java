package edu.asu.sapa.muri;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import edu.asu.sapa.ground.GMathForm;
import edu.asu.sapa.ground.Grounding;
import edu.asu.sapa.ground.Operator;
import edu.asu.sapa.ground.Proposition;
import edu.asu.sapa.ground.State;
import edu.asu.sapa.ground.update.Condition;
import edu.asu.sapa.lifted.*;

public class ProblemFacade extends Problem {
	private static int objID; 
	
	// the following ArrayLists are linked by index
	private ArrayList<Variable> extVariableList = new ArrayList<Variable>(2);
	private ArrayList<Predicate> sensingPredicateList = new ArrayList<Predicate>(2);
	private ArrayList<ArrayList<Predicate>> formulaList = new ArrayList<ArrayList<Predicate>>(2);
	private ArrayList<LiftedGoal> lgList = new ArrayList<LiftedGoal>(2);
	private ArrayList<HashSet<Variable>> variableSetList = new ArrayList<HashSet<Variable>>(2);

	// set of all sensing actions
	private HashSet<String> sensingActionSet = new HashSet<String>();

	private HashMap<String, ArrayList<Predicate>> createdBindingMap = new HashMap<String, ArrayList<Predicate>>();

	// HACK note: two entries for each goal (one with closure condition string and one with full goal binding string)
	private HashMap<String, LiftedGoal> createdBindingGoalMap = new HashMap<String, LiftedGoal>();
	private HashMap<String, String> sensedObjectToBindingMap = new HashMap<String,String>();

	/** map of facts in formula to associated object index in implication list. index by formula ID */  
//	private ArrayList<HashMap<String, Integer>> factMapList = new ArrayList<HashMap<String, Integer>>();
	/** list where each element is associated with a particular sensed object
	*  where the element is equal to the number of never-existing facts 
	*  note that the code currently assumes facts are never deleted--if a fact is 
	*  ever "said" it will decrement and if the fact is later deleted, nothing
	*  will happen
	*  <br/><br/>
	*   indexed by formula ID */
//	private ArrayList<ArrayList<Integer>> implicationListList = new ArrayList<ArrayList<Integer>>();

	private HashSet<Constant> unestablishedConsts = new HashSet<Constant>();

	public Grounding grounding;
	
	public ProblemFacade() {
		super();
		//System.err.println("Size of actions from ProblemFacade: " + this.actions.count);
	}
	
	/**
	 * Hits the symbol table for all arguments of the predicate and the predicate itself.
	 * @param pr
	 * @return
	 */
	public Predicate getTokenPredicate(Predicate pr) {
		for (int i = 0;i<pr.args.size();i++) {
			Symbol<String> s = pr.args.get(i);
			if (s instanceof Variable) {
				pr.args.set(i, putVariable((Variable)s));
			} else if (s instanceof Constant) {
				pr.args.set(i, putConstant((Constant)s));
			} else
				System.err.println(";; WARNING: something might be wrong with: " + s);
		}
//		return putPredicate(pr);
		return pr;
	}
	
	
	/**
	 * 
	 * @param extVar existential variable (the sensed object in question)
	 * @param fop First order predicate which indicates sensing for object has happened
	 * @param preds predicates in conjunctive first order formula
	 * @param lg Lifted goal associated with object (can be null if none exists)
	 */
	public void addSensingRule(Variable extVar, Predicate fop, ArrayList<Predicate> preds, LiftedGoal lg) {
		extVariableList.add(putVariable(extVar));
		sensingPredicateList.add(getTokenPredicate(fop));
		
		/* ArrayList for symbol table representation of predicates in first order formula
		 * Associated with a specific first order formula */
		ArrayList<Predicate> symbolPreds = new ArrayList<Predicate>(preds.size());
		for (int i=0;i<preds.size();i++) {
			symbolPreds.add(getTokenPredicate(preds.get(i)));
		}
		
		formulaList.add(symbolPreds);
		lgList.add(lg);

		// generate variable list for "grounding"
		HashSet<Variable> variableSet = new HashSet<Variable>();
		
		// collect the ones from the sensing predicate
		for (Symbol<String> var : fop.args) {
			if (!var.name.equals(extVar.name) && !(var instanceof Constant)) {
				variableSet.add(putVariable((Variable)var));
			}
		}

		for (Predicate pred : preds) {
			for (Symbol<String> var : pred.args) {
				// relies on variables being unique
				if (!var.name.equals(extVar.name) && !(var instanceof Constant)) {
					variableSet.add(putVariable((Variable)var));
				}
			}
		}
		
		variableSetList.add(variableSet);
		
		// find sensing actions by running through the add effects of actions
		for (Action a : actions.symbols) {
			for (Predicate p : a.addList) {
				if (p.name.equals(fop.name)) {
					sensingActionSet.add(a.name);
				}
			}
		}
	}

	public boolean isSensingAction(String a) {
		if (sensingActionSet.contains(a)) {
			return true;
		}
		return false;
	}
	
	public void groundFirstOrders() {
		/* 	TODO J.:  refactor first order goal grounding
		 *  New high level procedure: ground everything except sensing predicate, then if 
		 *  it's a goal predicate, ground that out too.
		 */
		
		// 	sensingPredicateList.size == number of rules
		// 	Make it go through with a while fixed point check on # of facts
		
		/*	If there is one rule, it is not a problem. With two or more, the stopping condition 
		 * 	changes to a level-off kind of definition --> No more facts added
		 * 	OR have a depth limit on the number of times you traverse rule cycles
		 */
		
		// ensure all existentials have entry
		
		for (int i = 0;i<sensingPredicateList.size();i++) {
			groundFirstOrder(i, false);
			groundFirstOrder(i, true);
		}
		
		/**
		 * FOR Later: Could generate a graph like structure and check for cycles and warn user
		 * if cuts have not been specified.
		 */
	}
	
	public void groundFirstOrder(int formulaID, boolean goal) {
		ArrayList<Variable> varList = new ArrayList<Variable>(variableSetList.get(formulaID));
		if (goal) {
			varList.add(extVariableList.get(formulaID));
		}
		groundFirstOrder(formulaID, extVariableList.get(formulaID), varList,
				new HashMap<Variable,Constant>(), goal, 0);
	}

	/**
	 * 
	 * @param formulaID ID of this formula
	 * @param extVar existential variable
	 * @param varSet set of variables in the formula
	 * @param binding binding for this first order formula
	 * @param i iteration (i.e., which variable we are iterating on)
	 */
	public void groundFirstOrder(int formulaID, Variable extVar, ArrayList<Variable> varList,
			HashMap<Variable, Constant> binding, boolean goal, int i) {
		if (i==varList.size() && !goal) {
			groundFirstOrder(formulaID, binding, varList);
		} else if (i==varList.size() && goal) {
//			groundFirstOrderGoal(formulaID, binding, varList);
		} else if (varList.get(i)==extVar && !goal) {
		} else {
			Variable var = varList.get(i);
			HashSet<Constant> constants = new HashSet<Constant>(var.type.constants);

			for (Constant c : constants) {
				binding.put(var, c);
				groundFirstOrder(formulaID, extVar, varList, binding, goal, i+1);
			}
		}
	}
	
	/**
	 * Has object already been bound?
	 * @param binding Bindings
	 * @return
	 */
//	private boolean isDuplicateSensed(int formulaID, HashMap<Variable, Constant> binding) {
//		// extract phantom object
//		Constant sensed = null;
//		for (Constant c : binding.values()) {
//			if (c.toString().equals(object)) break; 
//		}
//		if (phantom == null || 
//				sensedObjectToBindingMap.get(phantom.toString()) == null) return false;
//		
//		return true;
//	}
//
//	private void groundFirstOrderGoal(int formulaID, HashMap<Variable, Constant> binding,
//			ArrayList<Variable> varList) {
////		System.out.print("checking for binding: " + binding);
//		LiftedGoal newlg, lg = lgList.get(formulaID);
//		String goalString = makePredicateString(lg.proposition, binding);
//		
//		if (isDuplicateSensed(binding)) {
//			System.out.println(" .... abandoning binding on string " + goalString);
//			return;
//		} else {
//			System.out.println(" .... accepting binding on string " + goalString);
//		}
//		
////		if (createdBindingGoalMap.keySet().contains(goalString = makePredicateString(lg.proposition, binding))) {
////			System.out.println(" .... abandoning binding on string " + goalString);
////			return;
////		} else {
////			System.out.println(" .... accepting binding on string " + goalString);
////		}
//		
//		newlg = new LiftedGoal(lg, bindForAll(lg.proposition, binding, varList));
//
//		String sensedString = makeSensedString(formulaID, binding);
//		super.addGoal(newlg);
//		System.out.println("placing goal string: " + sensedString);
//		
//		
//		
//		// ugly hack---keep two entries for each goal (sensedString for 'removing' goals)
//		// and goalString for checking for duplicate binds
////		createdBindingGoalMap.put(sensedString, newlg);
////		createdBindingGoalMap.put(goalString, newlg);
//	}

	private Predicate predDeepCopy(Predicate p) {
		Predicate pred = new Predicate(p);
		pred.args = new ArrayList<Symbol<String>>(pred.args.size());
		
		for (Symbol<String> s : p.args) {
			pred.args.add(s);
		}
		
		return pred;
	}

	private boolean isSensingPredicate(Predicate pred) {
		/*System.err.println("\nINSIDE SENSING PREDICATE (to remove fake goal): " 
				+ pred + " Sensing Index: " + sensingIndex(pred));*/
		return sensingIndex(pred) >= 0;
	}
	
	private int sensingIndex(Predicate pred) {
		int sensingPred = -1;
		for (int i=0;i<sensingPredicateList.size();i++) {
			if (pred.name.equals(sensingPredicateList.get(i).name)) {
				sensingPred=i;
				break;
			}
		}
		return sensingPred;
	}
	
	/**
	 * Makes a string (sans "phantom" object).
	 * @param pred
	 * @return
	 */
	public String makeSensedString(Predicate pred) {
		// search for sensing predicate formula ID
		int formulaID = sensingIndex(pred);
		Predicate sensingPred = sensingPredicateList.get(formulaID);
		if (formulaID < 0) return null;
		StringBuilder st = new StringBuilder(pred.name);
		// generate binding
		Variable extVar = extVariableList.get(formulaID);
		for (int i=0;i<pred.args.size();i++) {
			if (!extVar.name.equals(sensingPred.args.get(i).name))
				st.append(' ').append(pred.args.get(i).name);
		}
		return st.toString();
	}
	
	public String getSensedObjectName(Predicate pred) {
		// search for sensing predicate formula ID
		int formulaID = sensingIndex(pred);
		Predicate sensingPred = sensingPredicateList.get(formulaID);
		if (formulaID < 0) return null;
		StringBuilder st = new StringBuilder();
		// generate binding
		Variable extVar = extVariableList.get(formulaID);
		for (int i=0;i<pred.args.size();i++) {
			if (extVar.name.equals(sensingPred.args.get(i).name)) {
				st.append(pred.args.get(i).name); 
				break; 
			} 
		}
		return st.toString();
	}
	
	public boolean replaceSensedObject(Predicate closurePred, ArrayList<Symbol<String>> predargs, Constant newObj) {
		// search for sensing predicate formula ID
		int formulaID = sensingIndex(closurePred);
//		System.out.println(" pred for replacement: " + predargs);
		boolean replaced = false;
		Predicate goalPred = lgList.get(formulaID).proposition;
		if (formulaID < 0) return replaced;
//		System.out.println("OLD PREDICATE ARGS: " + predargs);
		// generate binding
		Variable extVar = extVariableList.get(formulaID);
		for (int i=0;i<predargs.size();i++) {
			if (extVar.name.equals(goalPred.args.get(i).name)) {
				predargs.set(i, newObj);
				replaced = true;
			} 
		}
		
//		System.out.println("NEW PREDICATE ARGS: " + predargs);
		
		return replaced;
	}
	
	
	/**
	 * Makes a string (sans "phantom" object). This version is intended to be called by grounding method.
	 * @param formulaID
	 * @param binding
	 * @return
	 */
	public String makeSensedString(int formulaID, HashMap<Variable, Constant> binding) {
		Predicate pred = sensingPredicateList.get(formulaID);
		Variable extVar = extVariableList.get(formulaID);
		StringBuilder st = new StringBuilder(pred.name);
		for (int i=0;i<pred.args.size();i++) {
			Symbol<String> s = pred.args.get(i);
			if (!extVar.name.equals(s.name)) { //&& s instanceof Variable) {
				if (s instanceof Variable) {
					st.append(' ').append(binding.get((Variable)s));
				}
				else {
					st.append(' ').append(s.name);
				}
			}
		}
		return st.toString();
	}
	
	public String makePredicateString(Predicate pred, HashMap<Variable,Constant> binding) {
		StringBuilder st = new StringBuilder(pred.name);
		for (int i=0;i<pred.args.size();i++) {
			Symbol<String> s = pred.args.get(i);
			if (s instanceof Variable) {
				st.append(' ').append(binding.get(s));
			} else {
				st.append(' ').append(s);
			}
		}
		return st.toString();
	}
	
	public String makePredicateString(Predicate pred) {
		StringBuilder st = new StringBuilder(pred.name);
		for (int i=0;i<pred.args.size();i++) {
			Symbol<String> s = pred.args.get(i);
			st.append(' ').append(s);
		}
		return st.toString();
	}

	public boolean isGenerated(int formulaID, HashMap<Variable, Constant> binding) {
		return createdBindingMap.keySet().contains(makeSensedString(formulaID, binding)); 
	}
	
	private Predicate bindForAll(Predicate p, HashMap<Variable, Constant> binding, ArrayList<Variable> varList) {
		Predicate pred = predDeepCopy(p);
		for (Variable v : varList) {
			for (int i=0;i<pred.args.size();i++) {
				Symbol<String> pv = pred.args.get(i);
				if (pv.name.equals(v.name) && binding.containsKey(pv)) {
					pred.args.set(i, binding.get(pv));
				}
			}
		}
		putPredicate(pred); // assign appropriate ID
		return pred;
	}
	
	private Predicate bindExistential(Predicate p, Variable extVar, Constant pConst) {
		for (int i=0;i<p.args.size();i++) {
			if (extVar.name.equals(p.args.get(i).name)) {
				p.args.set(i, putConstant(pConst));
			}
		}
		return p;
	}

	/**
	 * Returns whether a particular fact is established or unestablished, 
	 * where established means that the fact is true either in the initial 
	 * state of the problem, or is _known_ true. Counterfactuals, for example, 
	 * are unestablished.
	 */
	private boolean isEstablished (Variable extVar, Constant pConst) {
		
		// You only need support if you are using a cut variable (!) to
		// create a counterfactual.
		
		// return false if cannot ascertain that fact is established
		// CONSTANTS ARE THE ESTABLISHED THINGS, *NOT* VARIABLES
		// If a variable has a ! in front, then it cannot bind to an unestablished constant!
		
		if (unestablishedConsts.contains(pConst)) {
			
			// should be the forall var associated with the extVar
			// throws an arrayindexoutofbounds ...
			// should not use varList

			HashSet<Variable> tempVarList = (HashSet<Variable>)variableSetList.get
															(extVariableList.indexOf(extVar));
			
			// currently, if *any* of the associated forall variables is a cut
			// variable, establishment will be cut.
			
			for (Variable tempVar : tempVarList)
				if (tempVar.isCutVar) return false;
		}
		
		return true;
	}
	
	/**
	 * Generates "phantom" existential object and adds corresponding facts and goal.
	 * TODO This is a very inefficient implementation and will scale poorly in very large domains. Need to re-implement in the
	 * parser/grounder.
	 * @param formulaID
	 * @param binding
	 * @param varList
	 */
	private void groundFirstOrder(int formulaID, HashMap<Variable, Constant> binding, 
			ArrayList<Variable> varList) {
		
		// TODO KRT: Should this check be here, or not? -> App. it should, o/w code doesn't run
		// Need to do some kind of clever "generated" check
		if (isGenerated(formulaID, binding)) return;
		//System.err.println("\nFROM ProblemFacade.groundFirstOrder, varList: " + varList);
		String sensedString = makeSensedString(formulaID, binding);
		
		ArrayList<Predicate> generatedPredicates = new ArrayList<Predicate>();
		Variable extVar = extVariableList.get(formulaID);
		
		ArrayList<Predicate> formula = new ArrayList<Predicate>(formulaList.get(formulaID));
		
		// bind "for all"
		for (int j=0;j<formula.size();j++) {
			// establishment must be checked inside the method called below
			Predicate p = bindForAll(formula.get(j), binding, varList);
			formula.set(j, p);
		}
		
		// do the same for goal
		LiftedGoal newlg, lg = lgList.get(formulaID);
		Predicate lgPred;
		newlg = new LiftedGoal(lg, lgPred = bindForAll(lg.proposition, binding, varList));

		
		// bind existential
		ArrayList<Constant> phantomConstant = new ArrayList<Constant>();
		String constantStr = extVar.type.name + "!" + objID++;
		Constant pConst;
		phantomConstant.add(pConst = putConstant(new Constant(constantStr)));
		unestablishedConsts.add(pConst);
//		System.out.println(";; generating counterfactual object " + pConst.name);
		
		// generate existential
		extVar.type.addAll(phantomConstant);
		for (Predicate p : formula) {
			if (isEstablished(extVar, pConst)) {	
				p = bindExistential(p, extVar, pConst);
				generatedPredicates.add(p);
			}
		}
		// do same for goal
		if (isEstablished(extVar, pConst))
			bindExistential(lgPred, extVar, pConst);
		
		// add predicates to problem ("phantom" predicates)
		for (Predicate p : generatedPredicates) {
			super.addInitialAdd(p);
		}
		
		
		// add "phantom goal"
		createdBindingMap.put(sensedString, generatedPredicates);
		//System.err.println("\nFROM ProblemFacade.groundFirstOrder(): " + sensedString + " " + generatedPredicates);
		super.addGoal(newlg);
		sensedObjectToBindingMap.put(constantStr, sensedString);
		
		String goalString = makePredicateString(newlg.proposition, binding);
		if (createdBindingGoalMap.get(sensedString) == null) {
			// add goal to problem
//		System.out.println("adding binding: " + binding + " for goal " + newlg + " goal string: " + goalString + " sensed string: " + sensedString);
			createdBindingGoalMap.put(sensedString, newlg);
		}
			
		// generate binding map 
		// to avoid re-binding later and remove facts after sensing
	}
	
	public void updateGrounding() {
		// process quantified goal
		super.updateGrounding();
	}
	
	public ArrayList<Predicate> getCounterfactuals(Predicate pred) {
		String st;
		st = makeSensedString(pred);
		//System.err.println("counterfactuals get: " + st);
		return createdBindingMap.get(st);
	}
	
	public LiftedGoal getCounterfactualGoal(Predicate pred) {
		String st;
		st = makeSensedString(pred);
		//System.err.println("counterfactual goal get: " + st);
		return createdBindingGoalMap.get(st);
	}

	HashMap<LiftedGoal, Operator> goalConditionsMap = new HashMap<LiftedGoal, Operator>();

	
	public boolean generateGoals(State currState) {
		boolean goalAdded = false;
		for (Map.Entry<LiftedGoal, Operator> e : goalConditionsMap.entrySet()) {
			LiftedGoal lg = e.getKey();
			
			if (currState.applicable(e.getValue())) {
				goalAdded = true;
				super.addGoal(lg);
			}
		}
		return goalAdded;
	}
	
	private void handleSensed(Predicate pred, MathForm time) {
		// delete "false" predicates
		ArrayList<Predicate> toDelete = getCounterfactuals(pred);
		ArrayList<Predicate> checkedPredicates = new ArrayList<Predicate>();

		// will be not null if toDelete is not null 
		LiftedGoal lg = getCounterfactualGoal(pred);

		if (toDelete != null) {
			// make 'false' goal soft and worth neg. infinity

			String sensedObject = getSensedObjectName(pred);
			Constant newConstant = putConstant(new Constant(sensedObject));

			ArrayList<Predicate> counterfactuals = getCounterfactuals(pred);
			
			Operator goalConditionOp = (Operator)Operator.nullOperator.clone();
			
			goalConditionOp.conditionList = new Condition[counterfactuals.size()];
			
			for (int i = 0;i<counterfactuals.size();i++) {
				Predicate p = counterfactuals.get(i);
				ArrayList<Symbol<String>> newargs = new ArrayList<Symbol<String>>(p.args);
				replaceSensedObject(pred,newargs,newConstant);
				Predicate newPred;
				putPredicate(newPred = new Predicate(p.name, newargs));

				Proposition prop = grounding.propositions.put(new Proposition(newPred.bind()));
				
				goalConditionOp.conditionList[i] = new Condition(prop.id, true, GMathForm.zero);
				
				checkedPredicates.add(newPred);
				
				//System.err.println(";; deleting due to sensing: " + p.toString());
				// addInitialDelete(p, MathForm.epsilon); // unclear if this will work
				// addInitialDelete(p, time); // changed 04/02/2012
				addInitialDelete(p);
			}

			ArrayList<Symbol<String>> newargs = new ArrayList<Symbol<String>>(lg.proposition.args);
			replaceSensedObject(pred, newargs, newConstant);
			Predicate newGoalPred;
			putPredicate(newGoalPred = new Predicate(lg.proposition.name, newargs));
			LiftedGoal newlg = new LiftedGoal(lg, newGoalPred);
			lg.gIsHard = false;
			//lg.guConstant = Float.NEGATIVE_INFINITY;
			lg.guConstant = 0.0f;
			
			//System.err.println("\nLifted Goal from handleSensed: " + lg.toString());

			goalConditionsMap.put(newlg, goalConditionOp);
		}
	}
	
	public void addInitialAdd(Predicate pred) {
		super.addInitialAdd(pred);
		if (isSensingPredicate(pred)) {
			//System.err.println("\nProblemFacade.addInitialAdd: " + pred);
			handleSensed(pred,initAction.d_dynamic);
		}
	}
	
	public void addInitialAdd(Predicate pred, MathForm time) {
		super.addInitialAdd(pred, time);
		if (isSensingPredicate(pred)) {
			//System.err.println("\nProblemFacade.addInitialAdd: " + pred);
			handleSensed(pred,time);
		}
	}
}
