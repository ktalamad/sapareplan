package edu.asu.sapa.monitor;

import java.io.InputStream;
import java.io.*;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.HashMap;
import java.util.StringTokenizer;

import java.lang.*;
import java.lang.Exception;

import edu.asu.sapa.muri.*;
import edu.asu.sapa.Planner;
import edu.asu.sapa.ground.Grounding;
import edu.asu.sapa.ground.Operator;
import edu.asu.sapa.ground.State;
import edu.asu.sapa.lifted.Constant;
import edu.asu.sapa.lifted.Domain;
import edu.asu.sapa.lifted.Function;
import edu.asu.sapa.lifted.LiftedGoal;
import edu.asu.sapa.lifted.LiftedTest;
import edu.asu.sapa.lifted.MathForm;
import edu.asu.sapa.lifted.Predicate; //import edu.asu.sapa.lifted.Problem;
import edu.asu.sapa.lifted.Action;
import edu.asu.sapa.lifted.Symbol;
import edu.asu.sapa.lifted.Term;
import edu.asu.sapa.lifted.Type;
import edu.asu.sapa.lifted.Variable;
import edu.asu.sapa.lifted.LiftedSet;
import edu.asu.sapa.parsing.PDDL21Parser;
import edu.asu.sapa.parsing.PDDL21ParserTokenManager;
import edu.asu.sapa.parsing.ParseException;
import edu.asu.sapa.parsing.TokenMgrError;
import edu.asu.sapa.rmtpg.RMTPG;
import edu.asu.sapa.lifted.Problem;

/**
 * Class to provide API functionality for creating domains.
 * Implements the PDDL21Parser in an API fashion.
 * @author Kartik Talamadupula
 *
 */
public class PDDLHelper {
	
	// Starting from the top of domain parsing
	
	public static void setDomainName (Problem problem, String name) {
		problem.setDomainName(name);
	}
	
	public static boolean putDomainRequirement (Problem problem, String requirement) {
		return problem.requirements.add(requirement);
	}
	
	public static Type putDomainType (Problem problem, Type t) {
		return problem.putType(t);
	}
	
	public static Predicate putPredicate (Problem problem, Predicate p) {
		return problem.putPredicate(p);
	}
	
	public static Function putFunction (Problem problem, Function f) {
		return problem.putFunction(f);
	}

	public static Constant putConstant (Problem problem, Constant c) {
		return problem.putConstant(c);
	}
	
	public static Variable putVariable (Problem problem, Variable v) {
		return problem.putVariable(v);
	}	
	
	public static Action putAction (Problem problem, Action act) {
		return problem.putAction(act);
	}
	
	// ------
	
	// Creator Methods

	
	public static Predicate createPredicate (Term t) {
		return new Predicate (t);		
	}
	
	public static Function createFunction (Term t) {
		return new Function (t);
	}
	
	/**
	 * The createTerm method creates a Term object, which is used in the creation
	 * of a number of objects like predicates, functions etc.
	 * @param act The action that the term is being created as a part of.
	 * @param name The name of the term object.
	 * @param args This is an ArrayList of Symbol(String) objects; these essentially 
	 *  stand for the arguments of the term, and are string representations of all the 
	 *  variables or constants that are in this term (as indicated in parsing).
	 * @return
	 */
	
	public static Term createTerm (String name, ArrayList<Symbol<String>> args) {
		
		Term tm = new Term(name);
		tm.addAll(args);		
		return tm;
	}
	
	public static Action createAction (String name) {
		Action retAct = new Action (name);
		return retAct;
	}

	
	// if adding tests, then, when building them out of their functions,
	// set track to true and use putfunction at that time in order to set up the grounding dependency/unreachable/reachable
	// pointers between all of the various java objects.
	public static Action createAction (Action act, Problem prob, MathForm cost, MathForm duration,  
				ArrayList<Variable> variables,			
					ArrayList<Predicate> startConds, ArrayList<Predicate> overAllConds, ArrayList<Predicate> endConds,
						ArrayList<Predicate> condPredList, ArrayList<LiftedTest> liftedTestList,
							ArrayList<Predicate> startAdds, ArrayList<Predicate> endAdds,
								ArrayList<Predicate> startDeletes, ArrayList<Predicate> endDeletes,
									ArrayList<Predicate> setPredList, ArrayList<LiftedSet> liftedSetList) {
				
		act.setScope(prob);
		
		act.setDuration(duration);
		act.setCost(cost);
		
		// Putting all the objects associated with this action's scope
		
		
		if (variables.size() > 0)
			for (Variable v : variables)
				act.putVariable(v);
		
		act.inTheMidstOfDeclaringThingsThatThisActionConditionsUpon=true;
		// Putting conditions
		for (Predicate p : startConds){
			act.putPredicate(p);
			act.putCondition(p, MathForm.zero);
		}
		for (Predicate p : overAllConds) {
			act.putPredicate(p);
			act.putProtectCondition(p);
		}
		for (Predicate p : endConds){
			act.putPredicate(p);
			act.putCondition(p, act.getDuration());		
		}
		LiftedTest lt;
		// NOTE: condPredList and liftedTestList should have a 1-1 mapping (hence same size too).
		for (Predicate p : condPredList) {
			
			lt = liftedTestList.get(condPredList.indexOf(p));
			
			switch (lt.getEvalTime()) {
				
			case LiftedTest.START:
				act.putTest(lt);
				act.putPredicate(p);
				act.putCondition(p);
				break;
			
			case LiftedTest.OVERALL:
				act.putTest(lt, MathForm.zero);
				act.putPredicate(p);
				act.putProtectCondition(p);
				break;
			
			case LiftedTest.END:		
				act.putTest(lt, duration);
				act.putPredicate(p);
				act.putCondition(p, duration);
				break;
			}
		}
		
		act.inTheMidstOfDeclaringThingsThatThisActionConditionsUpon=false;
		
		// Putting Add and Delete effects
		for (Predicate p : startAdds){
			act.putPredicate(p);
			act.putAdd(p, MathForm.zero);
		}
		for (Predicate p : endAdds){
			act.putPredicate(p);
			act.putAdd(p, act.getDuration());
		}
		for (Predicate p : startDeletes){
			act.putPredicate(p);
			act.putDelete(p, MathForm.zero);
		}
		for (Predicate p : endDeletes){
			act.putPredicate(p);
			act.putDelete(p, act.getDuration());		
		}
		// Action Set Effects
		
		LiftedSet ls;		
		// NOTE: setPredList and liftedSetList should have a 1-1 mapping (hence same size too).
		for (Predicate p : setPredList) {
			
			ls = liftedSetList.get(setPredList.indexOf(p));
			
			switch (ls.getSetTime()) {
			
			case LiftedSet.START:
				act.putSet(ls, MathForm.zero);
				break;
			
			case LiftedSet.END:
				act.putSet(ls, act.getDuration());
				break;
			}			
		}
		
		return act;
	}
	
	// Helper Methods
	
	public static Type createType (String typeName) {
		return new Type(typeName);
	}
	
	public static Type addSubTypes (Type t, ArrayList<Type> subTypeList) {
		t.addAll(subTypeList);
		return t;
	}
	
	public static LiftedTest createLiftedTest (String op, MathForm left, MathForm right, int evalTime) {
		return new LiftedTest(op, left, right, evalTime);
	}
	
	public static LiftedSet createLiftedSet (String op, Function f, MathForm mf, int setTime) {
		return new LiftedSet (op, f, mf, setTime);
	}
	
	public static MathForm createMathForm (char op, MathForm l, MathForm r) {
		return new MathForm(op, l, r);
	}
	
	public static MathForm createMathForm (float v) {
		return new MathForm(v);		
	}
	
	public static MathForm createMathForm (Function f) {
		return new MathForm(f);
	}
	
	public static MathForm createMathForm() {
		return new MathForm();
	}
	
	/** Not recommended for use, since it doesn't assign a type to a variable */
	public static Variable createVariable (String name, boolean cutVarStatus) {
		return new Variable (name, cutVarStatus);
	}
	
	/** For variables that are NOT cut variables */
	public static Variable createVariable (String name, Type t) {
		return new Variable (name, t);
	}
	
	/** Use if a variable is a cut variable; set 3rd argument to true */
	public static Variable createVariable (String name, Type t, boolean cutVarStatus) {
		return new Variable (name, t, cutVarStatus);
	}
	
	public static Constant createConstant (String n) {
		return new Constant(n);
	}
	
	/**
	 * Utility method to add an argument to a pre-existing term object.
	 * @param t The term object to be updated.
	 * @param arg The argument to be added.
	 * @return
	 */
	public static Term addArgToTerm (Term t, Symbol<String> arg) {
		t.add(arg);
		return t;
	}
	
	public static Term insertConstantIntoTerm (Term t, String name) {
		t.add(new Constant(name));
		return t;
	}
	
	public static Term insertVariableIntoTerm (Term t, String name) {
		t.add(new Variable(name));
		return t;
	}
	
	public static void setActionCost (Action act, Float cost) {
		act.setCost(cost);
	}
	
	public static void setActionDuration (Action act, Float duration) {
		act.setDuration(duration);
	}
	
/*	private Predicate createPredicate (Action act, String predicate) throws IOException {
		
		StringTokenizer tokenizer = new StringTokenizer (predicate, " ");		
		Term predTerm = new Term (tokenizer.nextToken());

		ArrayList<Symbol<String>> tempTermArgs = new ArrayList<Symbol<String>> ();
		ArrayList<String> varStrList = new ArrayList<String> ();
		
		while (tokenizer.hasMoreTokens()) {
						
			String varStr = tokenizer.nextToken();
			tokenizer.nextToken();
			String typeStr = tokenizer.nextToken();
			
			Type tempType = new Type (typeStr);			
			Variable tempVar = new Variable (varStr, tempType);
			
			act.putType(tempType);
			act.putVariable(tempVar);
			
			tempTermArgs.add(new Symbol<String> (varStr));
			tempTermArgs.add(new Symbol<String> (typeStr));
			
			predTerm.addAll(tempTermArgs);
			
			tempTermArgs.clear();
			varStrList.clear();
		}
		
		return new Predicate (predTerm);		
	} */

}
