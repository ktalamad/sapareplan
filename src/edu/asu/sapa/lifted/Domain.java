/***********************************************************
   Author: Minh B. Do - Arizona State University
 ***********************************************************/
package edu.asu.sapa.lifted;

import java.util.ArrayList;

/**
 * Domain: Storing Domain structure returned by parser from reading the domain
 * file in PDDL2.1.
 */
public class Domain implements Scope {

	public String name;
	public ArrayList<String> requirements = new ArrayList<String>();

	public Type obj = new Type("object");

	public SymbolTable<String, Type> types = new SymbolTable<String, Type>(obj);
	public SymbolTable<String, Constant> constants = new SymbolTable<String, Constant>();
	public SymbolTable<String, Predicate> predicates = new SymbolTable<String, Predicate>();
	public SymbolTable<String, Function> functions = new SymbolTable<String, Function>();
	public SymbolTable<String, Variable> variables = new SymbolTable<String, Variable>();
	public SymbolTable<String, Action> actions = new SymbolTable<String, Action>();

	public void initGrounding() {
		obj.initGrounding();
		// everything is a subtype of object...
//		for (Predicate p : predicates.symbols) {
//			p.initGrounding();
//		}
//		for (Function f : functions.symbols) {
//			f.initGrounding();
//		}
		for (Action a : actions.symbols) {
			a.initGrounding();
		}
	}
	
	public void updateGrounding() {
		obj.initGrounding();
		// everything is a subtype of object...
		for (Predicate p : predicates.symbols) {
			p.updateGrounding();
		}
		for (Function f : functions.symbols) {
			f.updateGrounding();
		}
		for (Action a : actions.symbols) {
			a.updateGrounding();
		}
	}

	public Action putAction(Action a) {
		a.setScope(this);
		//System.err.println("Action Name: " + a.name);
		return actions.put(a);
	}

	public Constant putConstant(Constant c) {
		return constants.put(c);
	}

	public Function putFunction(Function f) {
		Function func = functions.put(f);
		if (func == f)
			func.dependents = new ArrayList<Function>();
		return func;
	}

	public Predicate putPredicate(Predicate p) {
		Predicate pred = predicates.put(p);
		if (pred == p)
			pred.dependents = new ArrayList<Predicate>();
		return pred;
	}

	public Type putType(Type t) {
		return types.put(t);
	}

	public Variable putVariable(Variable v) {
		return variables.put(v);
	}

	public String getActionName(int a) {
		return actions.getName(a);
	}

	public String getConstantName(int c) {
		return constants.getName(c);
	}

	public String getFunctionName(int f) {
		return functions.getName(f);
	}

	public String getPredicateName(int p) {
		return predicates.getName(p);
	}

	public String getTypeName(int t) {
		return types.getName(t);
	}

	public String getVariableName(int v) {
		return variables.getName(v);
	}

	public Action getAction(int a) {
		return actions.get(a);
	}

	public Constant getConstant(int c) {
		return constants.get(c);
	}

	public Function getFunction(int f) {
		return functions.get(f);
	}

	public Predicate getPredicate(int p) {
		return predicates.get(p);
	}

	public Type getType(int t) {
		return types.get(t);
	}

	public Variable getVariable(int v) {
		return variables.get(v);
	}

	public Action getAction(String a) {
		return actions.get(a);
	}

	public Constant getConstant(String c) {
		return constants.get(c);
	}

	public Function getFunction(String f) {
		return functions.get(f);
	}

	public Predicate getPredicate(String p) {
		return predicates.get(p);
	}

	public Type getType(String t) {
		return types.get(t);
	}

	public Variable getVariable(String v) {
		return variables.get(v);
	}

}
