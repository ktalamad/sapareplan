/*********************************************************************
   Author: Minh B. Do (Arizona State University - binhminh@asu.edu)
 **********************************************************************/
package edu.asu.sapa.lifted;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

/**
 * Action: Abstract class to store the action structure specified in the domain
 * file. Will be used as the action template to ground actions using object
 * instances from the problem file
 */
public class Action extends Symbol<String> implements Bindable<Variable>, Scope {
	public Scope outerScope;

	public SymbolTable<String, Variable> variables = new SymbolTable<String, Variable>();

	public MathForm d_dynamic;
	public MathForm c_dynamic;

	public ArrayList<Predicate> conditionList;
	public ArrayList<Predicate> protectConditionList;
	public ArrayList<LiftedTest> testList;
	public ArrayList<LiftedTest> protectTestList;

	public ArrayList<Predicate> deleteList;
	public ArrayList<Predicate> addList;
	public ArrayList<LiftedSet> setList; // effects for metric resource

	public ArrayList<MathForm> addTime, deleteTime, setTime, conditionTime,
			testTime;

	public Constant[] map;
	public Variable[] order;
	// whether we are parsing reads (true) or writes (false)
	public boolean inTheMidstOfDeclaringThingsThatThisActionConditionsUpon;

	// should avoid excess work, but action name collisions don't happen
	public Action(String n) {
		super(n);

		d_dynamic = new MathForm(MathForm.epsilon.getValue());
		c_dynamic = MathForm.one;

		conditionList = new ArrayList<Predicate>();
		protectConditionList = new ArrayList<Predicate>();
		testList = new ArrayList<LiftedTest>();
		protectTestList = new ArrayList<LiftedTest>();

		addList = new ArrayList<Predicate>();
		deleteList = new ArrayList<Predicate>();
		setList = new ArrayList<LiftedSet>();

		conditionTime = new ArrayList<MathForm>();
		testTime = new ArrayList<MathForm>();
		addTime = new ArrayList<MathForm>();
		deleteTime = new ArrayList<MathForm>();
		setTime = new ArrayList<MathForm>();
	}

	public void updateGrounding() {
		for (Variable v : variables.symbols) {
			v.updateGrounding();
		}
	}
	
	public void initGrounding() {
		map = new Constant[variables.count];
		order = new Variable[variables.count];
		for (Variable v : variables.symbols) {
			v.initGrounding();
			order[v.id] = v;
		}
		Arrays.sort(order);
	}

	public void clearLists() {
		conditionList.clear();
		protectConditionList.clear();
		testList.clear();
		protectTestList.clear();

		addList.clear();
		deleteList.clear();
		setList.clear();

		conditionTime.clear();
		testTime.clear();
		addTime.clear();
		deleteTime.clear();
		setTime.clear();
	}
	
	public Action putAction(Action a) {
		return outerScope.putAction(a);
	}

	public Constant putConstant(Constant c) {
		return outerScope.putConstant(c);
	}

	// watch out for mathform processing and tracking of conditions in the parser.
	// the grounding is tracking the proposition "is this defined?"
	public Function putFunction(Function f) {
		Function func = outerScope.putFunction(f);
		if (inTheMidstOfDeclaringThingsThatThisActionConditionsUpon)
			func.dependents.add(f);
		return func;
	}

	public Predicate putPredicate(Predicate p) {
		Predicate pred = outerScope.putPredicate(p);
		if (inTheMidstOfDeclaringThingsThatThisActionConditionsUpon)
			pred.dependents.add(p);
		return pred;
	}
	
	public Predicate getPredicateRefForCondition(Predicate p) {
		assert this.inTheMidstOfDeclaringThingsThatThisActionConditionsUpon;
		Predicate pred = outerScope.putPredicate(p);
		pred.dependents.add(p);
		return p;
	}
	
	public Predicate getPredicateRefForEffect(Predicate p) {
		assert !this.inTheMidstOfDeclaringThingsThatThisActionConditionsUpon;
		outerScope.putPredicate(p);
		return p;
	}

	public Type putType(Type t) {
		return outerScope.putType(t);
	}

	public Variable putVariable(Variable v) {
		Variable o = variables.put(v);
		if (inTheMidstOfDeclaringThingsThatThisActionConditionsUpon)
			++o.referenceCount;
		return o;
	}
	
	// PUBLIC API FOR CREATING ACTIONS
	public Variable putParameter(Variable v) {
		assert this.inTheMidstOfDeclaringThingsThatThisActionConditionsUpon;
		Variable o = variables.put(v);
		++o.referenceCount;
		return o;
	}
	
	public Variable createParameter(String name, Type t) {
		assert this.inTheMidstOfDeclaringThingsThatThisActionConditionsUpon;
		Variable o = variables.put(new Variable(name, t));
		++o.referenceCount;
		return o;
	}
	
	public Variable getVariableRefForConditions(Variable v) {
		assert this.inTheMidstOfDeclaringThingsThatThisActionConditionsUpon;
		Variable o = variables.get(v.id);		
		++o.referenceCount;
		return o;
	}
	
	// PUBLIC API FOR CREATING ACTIONS
	public Variable getVariableRefForEffects(Variable v) {
		assert !(this.inTheMidstOfDeclaringThingsThatThisActionConditionsUpon);
		Variable o = variables.get(v.id);		
		return o;
	}


	public Predicate getAdd(int index) {
		return addList.get(index);
	}

	public MathForm getAddTime(int index) {
		return addTime.get(index);
	}

	public Predicate getCondition(int index) {
		return conditionList.get(index);
	}

	/** Duration that this (pre)condition should hold (at start/end/over-all) */
	public MathForm getConditionTime(int index) {
		return conditionTime.get(index);
	}

	public Predicate getDelete(int index) {
		return deleteList.get(index);
	}

	public MathForm getDeleteTime(int index) {
		return deleteTime.get(index);
	}

	public LiftedSet getSet(int index) {
		return setList.get(index);
	}

	public MathForm getSetTime(int index) {
		return setTime.get(index);
	}

	public LiftedTest getTest(int index) {
		return testList.get(index);
	}

	public MathForm getTestTime(int index) {
		return testTime.get(index);
	}

	public int numAdd() {
		return addList.size();
	}

	public int numCondition() {
		return conditionList.size();
	}

	public int numDelete() {
		return deleteList.size();
	}

	public int numSet() {
		return setList.size();
	}

	public int numTest() {
		return testList.size();
	}

	public void putAdd(Predicate p, MathForm time) {
		addList.add(p);
		addTime.add(time);
	}
	public void putDelete(Predicate p, MathForm time) {
		deleteList.add(p);
		deleteTime.add(time);
	}

	public void putSet(LiftedSet f, MathForm time) {
		setList.add(f);
		setTime.add(time);
	}


	public void createAdd(Term t) {
		assert !this.inTheMidstOfDeclaringThingsThatThisActionConditionsUpon;
		addList.add(this.getPredicateRefForEffect(new Predicate(t)));
		addTime.add(MathForm.dur);
	}

	public void createDelete(Term t) {
		assert !this.inTheMidstOfDeclaringThingsThatThisActionConditionsUpon;
		deleteList.add(this.getPredicateRefForEffect(new Predicate(t)));
		deleteTime.add(MathForm.zero);
	}
	
	
	public void createAdd(Term t, MathForm time) {
		assert !this.inTheMidstOfDeclaringThingsThatThisActionConditionsUpon;
		addList.add(this.getPredicateRefForEffect(new Predicate(t)));
		addTime.add(time);
	}
	public void createDelete(Term t, MathForm time) {
		assert !this.inTheMidstOfDeclaringThingsThatThisActionConditionsUpon;
		deleteList.add(this.getPredicateRefForEffect(new Predicate(t)));
		deleteTime.add(time);
	}

/*	public void putSet(LiftedSet f, MathForm time) {
		setList.add(f);
		setTime.add(time);
	}*/


	public void putAdd(Predicate p) {
		addList.add(p);
		addTime.add(MathForm.dur);
	}

	public void putDelete(Predicate p) {
		deleteList.add(p);
		deleteTime.add(MathForm.zero);
	}
	
	

	public void putSet(LiftedSet f) {
		setList.add(f);
		setTime.add(MathForm.dur);
	}

	public void putProtectCondition(Predicate p) {
		assert this.inTheMidstOfDeclaringThingsThatThisActionConditionsUpon;
		protectConditionList.add(p);
	}


	public void putCondition(Predicate p, MathForm time) {
		assert this.inTheMidstOfDeclaringThingsThatThisActionConditionsUpon;
		conditionList.add(p);
		conditionTime.add(time);
	}

	public void putTest(LiftedTest f, MathForm time) {
		assert this.inTheMidstOfDeclaringThingsThatThisActionConditionsUpon;
		testList.add(f);
		testTime.add(time);
	}
	public void putCondition(Predicate p) {
		assert this.inTheMidstOfDeclaringThingsThatThisActionConditionsUpon;
		conditionList.add(p);
		conditionTime.add(MathForm.zero);
	}

	public void putTest(LiftedTest f) {
		assert this.inTheMidstOfDeclaringThingsThatThisActionConditionsUpon;
		protectTestList.add(f);
		//testTime.add(MathForm.zero);
	}
	
	/** For overall condition */
	public void createProtectCondition(Term t) {
		assert this.inTheMidstOfDeclaringThingsThatThisActionConditionsUpon;
		protectConditionList.add(this.getPredicateRefForCondition(new Predicate(t)));
	}


	public void createCondition(Term t, MathForm time) {
		assert this.inTheMidstOfDeclaringThingsThatThisActionConditionsUpon;
		conditionList.add(this.getPredicateRefForCondition(new Predicate(t)));
		conditionTime.add(time);
	}

	/*public void createTest(LiftedTest f, MathForm time) {
		assert this.inTheMidstOfDeclaringThingsThatThisActionConditionsUpon;
		testList.add(f);
		testTime.add(time);
	}*/
	
	public void createCondition(Term t) {
		assert this.inTheMidstOfDeclaringThingsThatThisActionConditionsUpon;
		conditionList.add(this.getPredicateRefForCondition(new Predicate(t)));
		conditionTime.add(MathForm.zero);
	}

/*	public void putTest(LiftedTest f) {
		assert this.inTheMidstOfDeclaringThingsThatThisActionConditionsUpon;
		protectTestList.add(f);
		//testTime.add(MathForm.zero);
	}
*/	
	public void setCost(float f) {
		// c_isConstant = true;
		// c_constant = f;
		c_dynamic = new MathForm(f);
	}

	public void setCost(MathForm mf) {
		// c_isConstant = false;
		c_dynamic = mf;
	}

	public void setDuration(float f) {
		// d_isConstant = true;
		// d_constant = f;
		d_dynamic.setValue(f);
	}

	public void setDuration(MathForm mf) {
		// d_isConstant = false;
		d_dynamic.set(mf);
	}

	@Override
	public String toString() {
		String s = new String();

		s += "(" + name + "\n";
		s += ":parameters (";
		int sz = size();
		if (sz > 0) {
			Variable p = (Variable) get(0);
			s += p;
			s += " - " + p.type.name;
		}
		for (int i = 1; i < sz; ++i) {
			s += ' ';
			Variable p = (Variable) get(i);
			s += p;
			s += " - " + p.type.name;
		}
		s += ")\n";

		s += ":duration ";
		// if( d_isConstant )
		// s += d_constant;
		// else
		s += d_dynamic;
		s += "\n";

		s += ":costs ";
		// if( c_isConstant )
		// s += c_constant;
		// else
		s += c_dynamic;
		s += "\n";

		s += ":precondition\n";
		for (int i = 0; i < numCondition(); i++) {
			s += "\t(at " + getConditionTime(i) + " (";
			s += getCondition(i) + "))\n";
		}
		for (int i = 0; i < numProtectCondition(); i++) {
			s += "\t(over all (" + getProtectCondition(i) + "))\n";
		}
		for (int i = 0; i < numTest(); i++) {
			s += "\t(at " + getTestTime(i) + " ";
			s += getTest(i) + ")\n";
		}
		for (int i = 0; i < numProtectTest(); i++) {
			s += "\t(over all " + getProtectTest(i) + ")\n";
		}
		s += "\n";

		s += ":effect\n";
		for (int i = 0; i < numDelete(); i++) {
			s += "\t(at " + getDeleteTime(i) + " ";
			s += "(not (" + getDelete(i) + "))\n";
		}
		for (int i = 0; i < numAdd(); i++) {
			s += "\t(at " + getAddTime(i) + " (";
			s += getAdd(i) + "))\n";
		}
		for (int i = 0; i < numSet(); i++) {
			s += "\t(at " + getSetTime(i) + " ";
			s += getSet(i) + ")\n";
		}
		s += ")\n";

		return s;
	}

	public LiftedTest getProtectTest(int i) {
		return this.protectTestList.get(i);
	}

	public int numProtectTest() {
		return this.protectTestList.size();
	}

	public Variable get(int index) {
		return variables.symbols.get(index);
	}

	public int size() {
		return variables.symbols.size();
	}

	public void add(int index, Variable element) {
		variables.symbols.add(index, element);
	}

	public boolean add(Variable e) {
		return variables.symbols.add(e);
	}

	public boolean addAll(Collection<? extends Variable> c) {
		return variables.symbols.addAll(c);
	}

	public boolean addAll(int index, Collection<? extends Variable> c) {
		return variables.symbols.addAll(index, c);
	}

	public void clear() {
		variables.symbols.clear();
	}

	public boolean contains(Object o) {
		return variables.symbols.contains(o);
	}

	public boolean containsAll(Collection<?> c) {
		return variables.symbols.containsAll(c);
	}

	public void ensureCapacity(int minCapacity) {
		variables.symbols.ensureCapacity(minCapacity);
	}

	public int indexOf(Object o) {
		return variables.symbols.indexOf(o);
	}

	public boolean isEmpty() {
		return variables.symbols.isEmpty();
	}

	public Iterator<Variable> iterator() {
		return variables.symbols.iterator();
	}

	public int lastIndexOf(Object o) {
		return variables.symbols.lastIndexOf(o);
	}

	public ListIterator<Variable> listIterator() {
		return variables.symbols.listIterator();
	}

	public ListIterator<Variable> listIterator(int index) {
		return variables.symbols.listIterator(index);
	}

	public Variable remove(int index) {
		return variables.symbols.remove(index);
	}

	public boolean remove(Object o) {
		return variables.symbols.remove(o);
	}

	public boolean removeAll(Collection<?> c) {
		return variables.symbols.removeAll(c);
	}

	public boolean retainAll(Collection<?> c) {
		return variables.symbols.retainAll(c);
	}

	public Variable set(int index, Variable element) {
		return variables.symbols.set(index, element);
	}

	public List<Variable> subList(int fromIndex, int toIndex) {
		return variables.symbols.subList(fromIndex, toIndex);
	}

	public Object[] toArray() {
		return variables.symbols.toArray();
	}

	public <T> T[] toArray(T[] a) {
		return variables.symbols.toArray(a);
	}

	public void trimToSize() {
		variables.symbols.trimToSize();
	}

	public int numProtectCondition() {
		return this.protectConditionList.size();
	}

	public Predicate getProtectCondition(int k) {
		return this.protectConditionList.get(k);
	}

	public ArrayList<Integer> bind(ConstantSymbol<?>[] map) {
		return Binder.bind(this, map);
	}

	public ArrayList<Integer> bind() {
		return Binder.bind(this);
	}

	public void setScope(Scope o) {
		outerScope = o;
	}

	public Action getAction(int a) {
		return outerScope.getAction(a);
	}

	public Action getAction(String a) {
		return outerScope.getAction(a);
	}

	public String getActionName(int a) {
		return outerScope.getActionName(a);
	}

	public Constant getConstant(int c) {
		return outerScope.getConstant(c);
	}

	public Constant getConstant(String c) {
		return outerScope.getConstant(c);
	}

	public String getConstantName(int c) {
		return outerScope.getConstantName(c);
	}
	
	public MathForm getDuration () {
		return this.d_dynamic;
	}

	public Function getFunction(int f) {
		return outerScope.getFunction(f);
	}

	public Function getFunction(String f) {
		return outerScope.getFunction(f);
	}

	public String getFunctionName(int f) {
		return outerScope.getFunctionName(f);
	}

	public Predicate getPredicate(int p) {
		return outerScope.getPredicate(p);
	}

	public Predicate getPredicate(String p) {
		return outerScope.getPredicate(p);
	}

	public String getPredicateName(int p) {
		return outerScope.getPredicateName(p);
	}

	public Type getType(int t) {
		return outerScope.getType(t);
	}

	public Type getType(String t) {
		return outerScope.getType(t);
	}

	public String getTypeName(int t) {
		return outerScope.getTypeName(t);
	}

	public Variable getVariable(int v) {
		return outerScope.getVariable(v);
	}

	public Variable getVariable(String v) {
		return outerScope.getVariable(v);
	}

	public String getVariableName(int v) {
		return outerScope.getVariableName(v);
	}
}
