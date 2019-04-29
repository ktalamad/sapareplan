package edu.asu.sapa.monitor;

import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.HashMap;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.lang.NullPointerException;

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
import edu.asu.sapa.lifted.LiftedSet;
import edu.asu.sapa.lifted.MathForm;
import edu.asu.sapa.lifted.Predicate; //import edu.asu.sapa.lifted.Problem;
import edu.asu.sapa.lifted.Action;
import edu.asu.sapa.lifted.Problem;
import edu.asu.sapa.lifted.Symbol;
import edu.asu.sapa.lifted.Term;
import edu.asu.sapa.lifted.Type;
import edu.asu.sapa.lifted.Variable;
import edu.asu.sapa.parsing.PDDL21Parser;
import edu.asu.sapa.parsing.ParseException;
import edu.asu.sapa.parsing.TokenMgrError;
import edu.asu.sapa.rmtpg.RMTPG;

/**
 * Class to assist in the creation of Action objects.
 * Complements the API.
 * @author Kartik Talamadupula
 *
 */
public class ActionMaker {
	
	Action actionRef;
	Problem probRef;
	MathForm cost;
	MathForm duration;
	
	ArrayList<Constant> constants;
	ArrayList<Function> functions;
	ArrayList<Variable> variables;
	ArrayList<Predicate> predicates;
	
	ArrayList<Predicate> startConds;
	ArrayList<Predicate> overAllConds;
	ArrayList<Predicate> endConds;
	ArrayList<Predicate> condPredList;
	ArrayList<LiftedTest> liftedTestList;
	
	ArrayList<Predicate> startAdds;
	ArrayList<Predicate> endAdds;
	ArrayList<Predicate> startDeletes;
	ArrayList<Predicate> endDeletes;
	ArrayList<Predicate> setPredList;
	ArrayList<LiftedSet> liftedSetList;
	
	// constructor(s)
	
	public ActionMaker (Action actR, Problem probR) {
		
		this.actionRef = actR;
		this.probRef = probR;
		
		constants = new ArrayList<Constant>();
		functions = new ArrayList<Function>();
		variables = new ArrayList<Variable>();
		predicates = new ArrayList<Predicate>();
		
		startConds = new ArrayList<Predicate>();
		overAllConds = new ArrayList<Predicate>();
		endConds = new ArrayList<Predicate>();
		condPredList = new ArrayList<Predicate>();
		liftedTestList = new ArrayList<LiftedTest>();
		
		startAdds = new ArrayList<Predicate>();
		endAdds = new ArrayList<Predicate>();
		startDeletes = new ArrayList<Predicate>();
		endDeletes = new ArrayList<Predicate>();
		setPredList = new ArrayList<Predicate>();
		liftedSetList = new ArrayList<LiftedSet>();
	}
	
	public boolean amalgamate () {
		
		actionRef.setCost(this.cost);
		actionRef.setDuration(this.duration);
		
		if (constants.size() > 0)
			for (Constant c : constants)
				actionRef.putConstant(c);
		
		if (functions.size() > 0)
			for (Function f : functions)
				actionRef.putFunction(f);
		
		if (predicates.size() > 0)
			for (Predicate p : predicates)
				actionRef.putPredicate(p);
		
		if (variables.size() > 0)
			for (Variable v : variables)
				actionRef.putVariable(v);
		
		// Putting conditions
		
		for (Predicate p : startConds)
			actionRef.putCondition(p, MathForm.zero);
		
		for (Predicate p : overAllConds)
			actionRef.putProtectCondition(p);
		
		for (Predicate p : endConds)
			actionRef.putCondition(p, actionRef.getDuration());		
		
		LiftedTest lt;
		// NOTE: condPredList and liftedTestList should have a 1-1 mapping (hence same size too).
		for (Predicate p : condPredList) {
			
			lt = liftedTestList.get(condPredList.indexOf(p));
			
			switch (lt.getEvalTime()) {
				
			case LiftedTest.START:
				actionRef.putTest(lt);
				actionRef.putCondition(p);
				break;
			
			case LiftedTest.OVERALL:
				actionRef.putTest(lt, MathForm.zero);
				actionRef.putProtectCondition(p);
				break;
			
			case LiftedTest.END:		
				actionRef.putTest(lt, duration);
				actionRef.putCondition(p, duration);
				break;
			}
		}
		
		// Putting Add and Delete effects
		
		for (Predicate p : startAdds)
			actionRef.putAdd(p, MathForm.zero);
		
		for (Predicate p : endAdds)
			actionRef.putAdd(p, actionRef.getDuration());
		
		for (Predicate p : startDeletes)
			actionRef.putDelete(p, MathForm.zero);
		
		for (Predicate p : endDeletes)
			actionRef.putDelete(p, actionRef.getDuration());		
		
		// Action Set Effects
		
		LiftedSet ls;		
		// NOTE: setPredList and liftedSetList should have a 1-1 mapping (hence same size too).
		for (Predicate p : setPredList) {
			
			ls = liftedSetList.get(setPredList.indexOf(p));
			
			switch (ls.getSetTime()) {
			
			case LiftedSet.START:
				actionRef.putSet(ls, MathForm.zero);
				break;
			
			case LiftedSet.END:
				actionRef.putSet(ls, actionRef.getDuration());
				break;
			}			
		}
		
		return true;
	}

        public void initGrounding() {
            actionRef.initGrounding();
        }

	public MathForm getCost() {
		return cost;
	}
	
	public void setCost(MathForm cost) {
		this.cost = cost;
	}
	
	public MathForm getDuration() {
		return duration;
	}
	
	public void setDuration(MathForm duration) {
		this.duration = duration;
	}
	
	public ArrayList<Constant> getConstants() {
		return constants;
	}
	
	public void setConstants(ArrayList<Constant> constants) {
		this.constants = constants;
	}
	
	public ArrayList<Function> getFunctions() {
		return functions;
	}
	
	public void setFunctions(ArrayList<Function> functions) {
		this.functions = functions;
	}
	
	public ArrayList<Variable> getVariables() {
		return variables;
	}
	
	public void setVariables(ArrayList<Variable> variables) {
		this.variables = variables;
	}
	
	public ArrayList<Predicate> getPredicates() {
		return predicates;
	}
	
	public void setPredicates(ArrayList<Predicate> predicates) {
		this.predicates = predicates;
	}
	
	public ArrayList<Predicate> getStartConds() {
		return startConds;
	}
	
	public void setStartConds(ArrayList<Predicate> startConds) {
		this.startConds = startConds;
	}
	
	public ArrayList<Predicate> getOverAllConds() {
		return overAllConds;
	}
	
	public void setOverAllConds(ArrayList<Predicate> overAllConds) {
		this.overAllConds = overAllConds;
	}
	
	public ArrayList<Predicate> getEndConds() {
		return endConds;
	}
	
	public void setEndConds(ArrayList<Predicate> endConds) {
		this.endConds = endConds;
	}
	
	public ArrayList<Predicate> getCondPredList() {
		return condPredList;
	}
	
	public void setCondPredList(ArrayList<Predicate> condPredList) {
		this.condPredList = condPredList;
	}
	
	public ArrayList<LiftedTest> getLiftedTestList() {
		return liftedTestList;
	}
	
	public void setLiftedTestList(ArrayList<LiftedTest> liftedTestList) {
		this.liftedTestList = liftedTestList;
	}
	
	public ArrayList<Predicate> getStartAdds() {
		return startAdds;
	}
	
	public void setStartAdds(ArrayList<Predicate> startAdds) {
		this.startAdds = startAdds;
	}
	
	public ArrayList<Predicate> getEndAdds() {
		return endAdds;
	}
	
	public void setEndAdds(ArrayList<Predicate> endAdds) {
		this.endAdds = endAdds;
	}
	
	public ArrayList<Predicate> getStartDeletes() {
		return startDeletes;
	}
	
	public void setStartDeletes(ArrayList<Predicate> startDeletes) {
		this.startDeletes = startDeletes;
	}
	
	public ArrayList<Predicate> getEndDeletes() {
		return endDeletes;
	}
	
	public void setEndDeletes(ArrayList<Predicate> endDeletes) {
		this.endDeletes = endDeletes;
	}
	
	public ArrayList<Predicate> getSetPredList() {
		return setPredList;
	}
	
	public void setSetPredList(ArrayList<Predicate> setPredList) {
		this.setPredList = setPredList;
	}
	
	public ArrayList<LiftedSet> getLiftedSetList() {
		return liftedSetList;
	}
	
	public void setLiftedSetList(ArrayList<LiftedSet> liftedSetList) {
		this.liftedSetList = liftedSetList;
	}
	
	public Action getActionRef() {
		return actionRef;
	}
	
	public Problem getProbRef() {
		return probRef;
	}
	
	public boolean addConstant (Constant c) throws NullPointerException {
		try {
			this.constants.add(c);
			return true;
		}
		catch (NullPointerException np) {
			System.err.println("Constant List not defined: " + np);
			return false;
		}
	}
	
	public boolean addFunction (Function f) throws NullPointerException {
		try {
			this.functions.add(f);
			return true;
		}
		catch (NullPointerException np) {
			System.err.println("Function List not defined: " + np);
			return false;
		}
	}
	
	public boolean addVariable (Variable v) throws NullPointerException {
		try {
			this.variables.add(v);
			return true;
		}
		catch (NullPointerException np) {
			System.err.println("Variable List not defined: " + np);
			return false;
		}
	}
	
	public boolean addPredicate (Predicate p) throws NullPointerException {
		try {
			this.predicates.add(p);
			return true;
		}
		catch (NullPointerException np) {
			System.err.println("Predicate List not defined: " + np);
			return false;
		}
	}
	
	public boolean addStartCond (Predicate p) throws NullPointerException {
		try {
			this.startConds.add(p);
			return true;
		}
		catch (NullPointerException np) {
			System.err.println("List not defined: " + np);
			return false;
		}
	}
	
	public boolean addOverAllCond (Predicate p) throws NullPointerException {
		try {
			this.overAllConds.add(p);
			return true;
		}
		catch (NullPointerException np) {
			System.err.println("List not defined: " + np);
			return false;
		}
	}
	
	public boolean addEndCond (Predicate p) throws NullPointerException {
		try {
			this.endConds.add(p);
			return true;
		}
		catch (NullPointerException np) {
			System.err.println("List not defined: " + np);
			return false;
		}
	}
	
	public boolean addCondPred (Predicate p) throws NullPointerException {
		try {
			this.condPredList.add(p);
			return true;
		}
		catch (NullPointerException np) {
			System.err.println("List not defined: " + np);
			return false;
		}
	}
	
	public boolean addLiftedTest (LiftedTest lt) throws NullPointerException {
		try {
			this.liftedTestList.add(lt);
			return true;
		}
		catch (NullPointerException np) {
			System.err.println("List not defined: " + np);
			return false;
		}
	}
	
	public boolean addStartAdd (Predicate p) throws NullPointerException {
		try {
			this.startAdds.add(p);
			return true;
		}
		catch (NullPointerException np) {
			System.err.println("List not defined: " + np);
			return false;
		}
	}
	
	public boolean addEndAdd (Predicate p) throws NullPointerException {
		try {
			this.endAdds.add(p);
			return true;
		}
		catch (NullPointerException np) {
			System.err.println("List not defined: " + np);
			return false;
		}
	}
	
	public boolean addStartDelete (Predicate p) throws NullPointerException {
		try {
			this.startDeletes.add(p);
			return true;
		}
		catch (NullPointerException np) {
			System.err.println("List not defined: " + np);
			return false;
		}
	}
	
	public boolean addEndDelete (Predicate p) throws NullPointerException {
		try {
			this.endDeletes.add(p);
			return true;
		}
		catch (NullPointerException np) {
			System.err.println("List not defined: " + np);
			return false;
		}
	}
	
	public boolean addSetPred (Predicate p) throws NullPointerException {
		try {
			this.setPredList.add(p);
			return true;
		}
		catch (NullPointerException np) {
			System.err.println("List not defined: " + np);
			return false;
		}
	}
	
	public boolean addLiftedSet (LiftedSet ls) throws NullPointerException {
		try {
			this.liftedSetList.add(ls);
			return true;
		}
		catch (NullPointerException np) {
			System.err.println("List not defined: " + np);
			return false;
		}
	}		

}
