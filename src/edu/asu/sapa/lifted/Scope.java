package edu.asu.sapa.lifted;


public interface Scope {
	public Type putType(Type t);

	public Constant putConstant(Constant c);

	public Predicate putPredicate(Predicate p);

	public Function putFunction(Function f);

	public Variable putVariable(Variable v);

	public Action putAction(Action a);

	public String getActionName(int a);

	public String getConstantName(int c);

	public String getFunctionName(int f);

	public String getPredicateName(int p);

	public String getTypeName(int t);

	public String getVariableName(int v);

	public Action getAction(int a);

	public Constant getConstant(int c);

	public Function getFunction(int f);

	public Predicate getPredicate(int p);

	public Type getType(int t);

	public Variable getVariable(int v);

	public Action getAction(String a);

	public Constant getConstant(String c);

	public Function getFunction(String f);

	public Predicate getPredicate(String p);

	public Type getType(String t);

	public Variable getVariable(String v);
}
