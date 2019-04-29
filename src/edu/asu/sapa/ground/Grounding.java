package edu.asu.sapa.ground;

import java.util.ArrayList;
import java.util.HashSet;

import edu.asu.sapa.Priority;
import edu.asu.sapa.ground.update.Condition;
import edu.asu.sapa.ground.update.Effect;
import edu.asu.sapa.ground.update.ProtectCondition;
import edu.asu.sapa.ground.update.Set;
import edu.asu.sapa.ground.update.Test;
import edu.asu.sapa.lifted.Action;
import edu.asu.sapa.lifted.Constant;
import edu.asu.sapa.lifted.Function;
import edu.asu.sapa.lifted.LiftedGoal;
import edu.asu.sapa.lifted.LiftedSet;
import edu.asu.sapa.lifted.LiftedTest;
import edu.asu.sapa.lifted.MathForm;
import edu.asu.sapa.lifted.Predicate;
import edu.asu.sapa.lifted.Problem;
import edu.asu.sapa.lifted.Symbol;
import edu.asu.sapa.lifted.SymbolTable;
import edu.asu.sapa.lifted.Variable;
import edu.asu.sapa.utils.Int;

class PEvent {
	public int id;
	public Proposition p;

	public void set(int id, Proposition p) {
		this.id = id;
		this.p = p;
	}
}

class FEvent {
	public int id;
	public Fluent f;

	public void set(int id, Fluent f) {
		this.id = id;
		this.f = f;
	}
}

/**
 * Grounding: Class to help to ground Predicate, Function, Actions, StateManager
 * etc. according to the templates in the domain file and the object instances
 * in the problem file.
 */
public class Grounding {
	public Problem problem;
	boolean analysisFlag = false;

	public GoalManager gm;
	// should make GoalManager use these...
	public SymbolTable<Integer, Goal> goals = new SymbolTable<Integer, Goal>();
	public State init;

	public Operator initOperator;

	public SymbolTable<ArrayList<Integer>, Operator> operators = new SymbolTable<ArrayList<Integer>, Operator>();
	public SymbolTable<ArrayList<Integer>, Proposition> propositions = new SymbolTable<ArrayList<Integer>, Proposition>();
	public SymbolTable<ArrayList<Integer>, Fluent> fluents = new SymbolTable<ArrayList<Integer>, Fluent>();

	transient Constant[] map;

	// for updates
	transient int oldOperatorCount=0;
	transient public boolean addedReachableOperator=false;
	transient public boolean removedReachableOperator=false;
	transient public int numReachableOperators; 
	transient public int numReachableProps; 
	transient public int numReachableFluents;

	
	public Grounding(boolean analysisFlag) {
		this.analysisFlag = analysisFlag;
	}

	public Grounding(Grounding g) {
		analysisFlag = g.analysisFlag;

		gm = new GoalManager(g.gm);
		goals = new SymbolTable<Integer, Goal>(g.goals);
		init = new State(g.init);
		initOperator = (Operator) g.initOperator.clone();

		operators = new SymbolTable<ArrayList<Integer>, Operator>(g.operators);
		propositions = new SymbolTable<ArrayList<Integer>, Proposition>(g.propositions);
		fluents = new SymbolTable<ArrayList<Integer>, Fluent>(g.fluents);

		//prob = null;
	}

	private void analyzeStatic() {
		int numProp = propositions.count;
		ArrayList<Proposition> props = propositions.symbols;

		numReachableOperators = 0;
		numReachableProps = 0;
		numReachableFluents = 0;
		
		boolean wasPossible;

		int i=1;
		for (; i < oldOperatorCount; i++) {
			Operator instance = operators.get(i);
			if (instance == Operator.nullOperator) { 
				removedReachableOperator = true;
				continue;
			}
			wasPossible = instance.isPossible;
			instance.analyzeStatic(props);
			if (instance.isPossible) {
				instance.updateSupporters(props);
				++numReachableOperators;
				if (!wasPossible)
					addedReachableOperator = true;
			} else {
				if (wasPossible)
					removedReachableOperator = true;
			}
		}
		
		for (; i < operators.count; i++) {
			Operator instance = operators.get(i);
			instance.analyzeStatic(props);
			if (instance.isPossible) {
				++numReachableOperators;
				instance.updateSupporters(props);
				addedReachableOperator = true;
			}
		}
		
		System.out.println(";;GROUNDING GOALS");
		
		for (i = 0; i < goals.count; i++) {
			Goal g = goals.get(i);
			//System.out.println(";; processing: " + g);
			if (!g.isHard && (g.penaltyConstant == -g.rewardConstant)) {				
				//System.out.println(";;Skipped because useless");
				init.gCost -= g.rewardConstant;
				continue;
			}			
			//System.out.println(";;Skipped because constant");
			Proposition tempProp = props.get(g.proposition); 
			if (tempProp.isConstant) {
				if (tempProp.value) {
					init.gCost -= g.rewardConstant;
				}
				else {
					init.gCost += g.penaltyConstant;
				}
				continue;
			}
						
			System.out.println(";; Goal: " + g);
			gm.addGoal(g);
		}

		for (i = 1; i < numProp; ++i) {
			Proposition p = props.get(i);
			p.trim();
			if (p.isConstant) {
				init.propDB.remove(p.id);
			} else {
				++numReachableProps;
			}
		}

		for (i = 1; i < fluents.count; ++i) {
			Fluent f = fluents.symbols.get(i);
			if (f.isConstant) {
				init.fluentDB.remove(f.id);
			} else {
				++numReachableFluents;
			}
		}

	}

	public Fluent getFluent(int index) {
		return fluents.get(index);
	}

	public Operator getOperator(int index) {
		return operators.get(index);
	}

	public ArrayList<Operator> getOperators() {
		return operators.symbols;
	}

	public Proposition getProposition(int index) {
		return propositions.get(index);
	}

	public ArrayList<Proposition> getPropositions() {
		return propositions.symbols;
	}

	private Operator groundAction(Action template, Operator instance) {
		int k;
		FluentDB fluentDB = init.fluentDB;

		++numReachableOperators;
		
		GMathForm dur = groundMathForm(template.d_dynamic, fluentDB, null);
		instance.setDuration(dur);

		GMathForm m = groundMathForm(template.c_dynamic, fluentDB, dur);
		instance.setCost(m);

		LiftedTest testTemplate;
		for (k = 0; k < template.numTest(); k++) {
			testTemplate = template.getTest(k);

			GMathForm l = groundMathForm(testTemplate.getLeftSide(),
					init.fluentDB, dur);
			GMathForm r = groundMathForm(testTemplate.getRightSide(),
					init.fluentDB, dur);
			GMathForm t = groundMathForm(template.getTestTime(k), fluentDB, dur);

			instance.testList[k] = new Test(testTemplate.op, l, r, t);
		}

		LiftedSet setTemplate;
		for (k = 0; k < template.numSet(); k++) {
			setTemplate = template.getSet(k);

			GMathForm r = groundMathForm(setTemplate.getRightSide(), fluentDB,
					dur);
			GMathForm t = groundMathForm(template.getSetTime(k), fluentDB, dur);
			Fluent f;
			float v;
			f = groundFunction(setTemplate.getLeftSide());
			if (setTemplate.getOp() == 0) {
				v = r.getValue(dur);
				if (f.value != f.value && v == v) {
					f.isConstant = false;
					this.updateDependents(f.name.get(0),f);
				} else if (f.value != v) {
					f.isConstant = false;
				}
			} else
				f.isConstant = false;

			instance.setList[k] = new Set(setTemplate.getOp(), f.id, r, t);
		}

		Proposition prop;
		GMathForm time;
		int size;

		for (k = 0, size = template.numCondition(); k < size; k++) {
			prop = groundPredicate(template.getCondition(k));
			time = groundMathForm(template.getConditionTime(k), fluentDB, dur);

			instance.conditionList[k] = new Condition(prop.id, true, time);
		}

		for (k = 0, size = template.numProtectCondition(); k < size; k++) {
			prop = groundPredicate(template.getProtectCondition(k));

			instance.protectConditionList[k] = new ProtectCondition(prop.id, true);
		}

		int index = 0;
		for (k = 0, size = template.numAdd(); k < size; k++) {
			prop = groundPredicate(template.getAdd(k));
			//System.out.println(";; prop from grounding: " + prop.getName() + " init value: " + prop.value);
			time = groundMathForm(template.getAddTime(k), fluentDB, dur);

			if (prop.value == false) {
				prop.isConstant = false;
				this.updateDependents(prop.name.get(0),prop);
			}

			instance.effectList[index++] = new Effect(prop.id, true, time);
		}

		instance.deleteIndex = index;

		for (k = 0, size = template.numDelete(); k < size; k++) {
			prop = groundPredicate(template.getDelete(k));
			time = groundMathForm(template.getDeleteTime(k), fluentDB, dur);

			if (prop.value == true) {
				prop.isConstant = false;
			}

			instance.effectList[index++] = new Effect(prop.id, false, time);
		}

		return instance;
	}

	private int getFunction(Function template) {
		Fluent f = fluents.get(template.bind(map));
		if (f == null)
			return -1;
		return f.id;
	}

	private Fluent groundFunction(Function template) {
		Fluent p = new Fluent(template.bind(map));
		Fluent q = fluents.put(p);

		return q;
	}

	@Deprecated
	private int groundFunction(Function template, float value) {
		Fluent f = new Fluent(template.bind(map));
		Fluent q = fluents.put(f);
		if (q == f) {
			// addFEvent(template.id, f);
			updateDependents(template.id, q);
			q.value = value;
			if (value != value) {
				q.isConstant = true;				
			} else
				q.isConstant = false;
		} else if (q.value != value && (value == value || q.value == q.value) && q.isConstant) {
			q.value = value;
			q.isConstant = false;
		}
		return q.id;
	}

	private GMathForm groundMathForm(MathForm template, FluentDB mr,
			GMathForm dur) {
		GMathForm m;
		switch (template.type) {
		case 1:
			if (template.value == 0)
				return GMathForm.zero;
			if (template.value == 1)
				return GMathForm.one;
			if (template.value == GMathForm.epsilon.value)
				return GMathForm.epsilon;
			if (template.value != template.value)
				return GMathForm.NaN;

			return new GMathForm(template.value);
		case 5:
			if (dur == null)
				return null;
			if (dur.type == 1)
				return dur; // don't need copies of constants...
			return GMathForm.dur;
		case 0:
			int f = getFunction(template.function);
			m = new GMathForm(f);
			m.value = mr.get(f);
			return m;
		case 3:
			GMathForm left = groundMathForm(template.left, mr, dur);
			GMathForm right = groundMathForm(template.right, mr, dur);
			if ((left.type != 1) || (right.type != 1)) {
				m = new GMathForm(template.op, left, right);
				if (dur != null)
					m.value = m.value(mr, dur.value);
				else
					m.value  = m.value(mr);
				return m;
			}
			switch (template.op) {
			case '+':
				return new GMathForm(left.value + right.value);
			case '-':
				return new GMathForm(left.value - right.value);
			case '*':
				return new GMathForm(left.value * right.value);
			case '/':
				return new GMathForm(left.value / right.value);
			}
			break;
		case 2:
//			return GMathForm.NaN;
			break;
		default:
		}
		throw new NullPointerException(template.toString());
	}

	private int getPredicate(Predicate template) {
		Proposition p = propositions.get(template.bind(map));
		if (p == null)
			return -1;
		return p.id;
	}

	private Proposition groundPredicate(Predicate template) {
		Proposition p = propositions.put(new Proposition(template.bind(map)));
		return p;
	}

	@Deprecated
	private int groundPredicate(Predicate template, boolean v) {
		Proposition p = new Proposition(template.bind(map));
		Proposition q = propositions.put(p);
		if (q == p) {
			// addPEvent(template.id, p);
			updateDependents(template.id, q);
			q.value = v;
			if (!v)
				q.isConstant = true;
			else
				q.isConstant = false;
		} else if (q.value != v && q.isConstant) {
			q.isConstant = false;
			q.value = v;
		}
		// propositions.put(p);
		//
		// while (reachable.size() <= p.id) {
		// reachable.add(false);
		// }
		// if (add && !reachable.get(p.id)) {
		// addPEvent(template.id, p);
		// reachable.set(p.id, true);
		// }
		return q.id;
	}

	private void updateDependents(int id, Proposition p) {
		Predicate template = problem.predicates.get(id);
		for (int i = 0, size = p.name.size() - 1; i < size; ++i) {
			HashSet<Constant> objs = template.reachable.get(i);
			Constant c = problem.constants.get(p.name.get(i + 1));
			if (!objs.contains(c)) {
				objs.add(c);
				for (Predicate d : template.dependents) {
					Symbol<String> s = d.get(i);
					if (s instanceof Variable) {
						Variable v = (Variable) s;
						Int count = v.unreachable.get(c);
						// c may not be of v's type
						if (count == null)
							continue;
						if (--count.value == 0) {
							v.reachable.add(c);
						}
					}
				}
			}
		}

	}

	private void updateDependents(int id, Fluent f) {
		Function template = problem.functions.get(id);
		for (int i = 0, size = f.name.size() - 1; i < size; ++i) {
			HashSet<Constant> objs = template.reachable.get(i);
			Constant c = problem.constants.get(f.name.get(i + 1));
			if (!objs.contains(c)) {
				objs.add(c);
				for (Function d : template.dependents) {
					Symbol<String> s = d.get(i);
					if (s instanceof Variable) {
						Variable v = (Variable) s;
						Int count = v.unreachable.get(c);
						if (count == null)
							continue;
						if (--count.value == 0) {
							v.reachable.add(c);
						}
					}
				}
			}
		}
	}

	private void updateTemplate(Action template, int index) {
		if (index == map.length) {
			ArrayList<Integer> name = template.bind(map);
			Operator instance = operators.get(name);
			if (instance != null) {
				int i = instance.id;
				if (operators.symbols.get(i) != Operator.nullOperator)
					return;
				if (preGroundAction(template,instance)) {
					operators.symbols.set(i,instance);
					groundAction(template,instance);
				}
				return;
			}

			instance = new Operator(name, template);
			if (preGroundAction(template, instance)) {
				operators.put(instance);
				groundAction(template, instance);
			}
		} else {
			Variable v = template.order[index++];
			//System.err.println("Variable from UpdateTemplate: " + v.name + " Reachable Size: " + v.reachable.size()
				//	+ " Reference Count: " + v.referenceCount);
//			for (Constant o : v.type.constants) {
//			map[v.id] = o;
			for (int i = v.reachable.size() - 1; i >= 0; --i) {
				map[v.id] = v.reachable.get(i);
				// for (Constant object : v.reachable) {
				// map[v.id] = object;
				updateTemplate(template, index);
			}
		}
	}

	@Deprecated
	private void updateAction(Action template, Operator instance) {
		int k;
		FluentDB fluentDB = init.fluentDB;
		ArrayList<Fluent> fluents = this.fluents.symbols;
		ArrayList<Proposition> propositions = this.propositions.symbols;
		
		++numReachableOperators;
		GMathForm dur = instance.duration;
		for (Set s : instance.setList) {

			GMathForm r = s.rightSide;
			Fluent f = fluents.get(s.id);
			float v;
			if (s.op == 0) {
				v = r.getValue(dur);
				if (f.value != f.value && v == v) {
					f.isConstant = false;
					this.updateDependents(f.name.get(0),f);
				} else if (f.value != v) {
					f.isConstant = false;
				}
			} else
				f.isConstant = false;
		}

		for (Effect e : instance.effectList) {
			Proposition prop = propositions.get(e.id);
			if (e.value == true) {
				if (prop.value == false) {
					prop.isConstant = false;
					this.updateDependents(prop.name.get(0),prop);
				}				
			} else {
				if (prop.value == true) {
					prop.isConstant = false;
				}				
			}
		}
		
	}

	// Broken
	private void updateTemplateUnroll(Action template, int index) {
		ArrayList<Integer> name;
		Operator instance;
		int f = map.length - index;
		if (f <= 0) {
			name = template.bind(map);
			instance = operators.get(name);
			if (instance != null) {
				return;
			}
			instance = new Operator(name, template);
			if (preGroundAction(template, instance)) {
				operators.put(instance);
				groundAction(template, instance);
			}
			return;
		}

		Variable t, u, v;
		switch (f) {
		case 1:
			v = template.order[index];
			for (int i = v.reachable.size() - 1; i >= 0; --i) {
				map[v.id] = v.reachable.get(i);
				name = template.bind(map);
				instance = operators.get(name);
				if (instance != null) {
					return;
				}
				instance = new Operator(name, template);
				if (preGroundAction(template, instance)) {
					operators.put(instance);
					groundAction(template, instance);
				}
			}
			return;
		case 2:
			u = template.order[index++];
			v = template.order[index];
			for (int j = u.reachable.size() - 1; j >= 0; --j) {
				map[u.id] = u.reachable.get(j);
				for (int i = v.reachable.size() - 1; i >= 0; --i) {
					map[v.id] = v.reachable.get(i);
					name = template.bind(map);
					instance = operators.get(name);
					if (instance != null) {
						return;
					}
					instance = new Operator(name, template);
					if (preGroundAction(template, instance)) {
						operators.put(instance);
						groundAction(template, instance);
					}
				}
			}
			return;
		case 3:
			t = template.order[index++];
			u = template.order[index++];
			v = template.order[index];
			for (int k = t.reachable.size() - 1; k >= 0; --k) {
				map[t.id] = t.reachable.get(k);
				for (int j = u.reachable.size() - 1; j >= 0; --j) {
					map[u.id] = u.reachable.get(j);
					for (int i = v.reachable.size() - 1; i >= 0; --i) {
						map[v.id] = v.reachable.get(i);
						name = template.bind(map);
						instance = operators.get(name);
						if (instance != null) {
							return;
						}
						instance = new Operator(name, template);
						if (preGroundAction(template, instance)) {
							operators.put(instance);
							groundAction(template, instance);
						}
					}
				}
			}
			return;
		default:
			t = template.order[index++];
		u = template.order[index++];
		v = template.order[index++];
		for (int k = t.reachable.size() - 1; k >= 0; --k) {
			map[t.id] = t.reachable.get(k);
			for (int j = u.reachable.size() - 1; j >= 0; --j) {
				map[u.id] = u.reachable.get(j);
				for (int i = v.reachable.size() - 1; i >= 0; --i) {
					map[v.id] = v.reachable.get(i);
					updateTemplate(template, index);
				}
			}
		}
		}
	}

	private ArrayList<Operator> groundTemplates() {
		int count;
		int size = problem.actions.count;
		ArrayList<Action> templates = problem.actions.symbols;
		numReachableOperators=0;
		// ugly hack to track which actions have become re-reachable
		for(int i = operators.count-1; i>=1; --i)
			operators.symbols.set(i,Operator.nullOperator);
		do {
			count = numReachableOperators;
			for (int i = 1; i < size; ++i) {
				Action template = templates.get(i);
				//System.err.println("Template Name: " + template.name);
				// for (int j = 0; j < pEventsSize; ++j) {
				// updateDependents(pEvents[j].id, pEvents[j].p);
				// }
				// pEventsSize = 0;
				// for (int j = 0; j < fEventsSize; ++j) {
				// updateDependents(fEvents[j].id, fEvents[j].f);
				// }
				// fEventsSize = 0;
				map = template.map;
				updateTemplate(template, 0);
			}
		} while (count != numReachableOperators);

		return operators.symbols;
	}

	public boolean initialize(Problem prob) {

		this.problem = prob;
		gm = new GoalManager(prob);

		// Initial State

		ArrayList<Integer> name = new ArrayList<Integer>();
		propositions.put(new Proposition(name)); // 'begin-problem-proposition'
		fluents.put(new Fluent(name)); // 'begin-problem-proposition'
		initOperator = operators.put(new Operator(name, prob.initAction));

		groundInit(prob.initAction, initOperator);

		groundTemplates();

		boolean solvable = groundGoals();
		
		if (analysisFlag)
			analyzeStatic();
		else {
			numReachableOperators = -1;
			numReachableProps = -1;
			numReachableFluents = -1;			
		}


		return solvable;
	}

	private boolean groundGoals() {
		// if the goal propositions/fluents aren't ground out by the above procedure
		// then the problem must be unsolvable
		boolean solvable = true;

		// Goals
		LiftedGoal g;
		Goal goal;
		for (int i = 0; i < problem.numGoal(); i++) {
			g = problem.getGoal(i);

			if (!preGroundPredicate(g.proposition))
				solvable = false;
			else
				;
			Proposition p = groundPredicate(g.proposition);
			int id = p.id;
			//propositions.symbols.get(id).isConstant=false; // hack to make sure GoalManager works.
			// should just update GoalManager for constant satisfied goals
			// or make a goal action, and use that explicitly in PG......hmm....

			GMathForm mf = null;
			if (g.guIsConstant == false) {
				if (!preGroundMathForm(g.guDynamic, null))
					solvable = false;
				mf = groundMathForm(g.guDynamic, init.fluentDB, null);
			}
			if ((goal = goals.get((Integer)id)) == null) {
				goal = new Goal(id, g.gIsHard, g.deadline, g.guIsConstant,
						g.guConstant, mf, g.gpConstant);
				goals.put(goal);
			} else {
				goal.set(g.deadline, g.gIsHard, g.guIsConstant, g.guConstant, g.gpConstant, mf);
			}

			//gm.addGoal(id, g.deadline, g.gIsHard, g.guConstant, g.gpConstant);
		}

		//		gm.numGoal = goals.count;

		GoalDep gd;
		int[] goalList = new int[problem.maxGDSize()];
		ArrayList<GoalDep> goalDeps = problem.goalDeps;
		for (int i = 0, isz = goalDeps.size(); i < isz; ++i) {
			gd = goalDeps.get(i);
			int jsz = gd.size();
			for (int j = 0; j < jsz; j++) {
				Predicate p = gd.goals.get(j);
				if (!preGroundPredicate(p))
					solvable = false;
				Proposition prop = groundPredicate(p);
				int id = prop.id;
//				propositions.symbols.get(id).isConstant=false;
				goalList[j] = id;
			}
			gm.addGoalDep(goalList, jsz, gd.sUtil);
		}

		return solvable;
	}

	public void makeGoal() {
		
	}
	
	public boolean update() {
		oldOperatorCount = operators.count;
		addedReachableOperator = false;
		removedReachableOperator = false;

		// need to make room for more goal dependencies, but also goal arrays probably have to be sized down if there are fewer goals
		gm.update(problem);

		initOperator.update(problem.initAction);

		groundInit(problem.initAction,initOperator);

		groundTemplates();

		boolean solvable = groundGoals();
		
		if (analysisFlag)
			analyzeStatic();
		else {
			numReachableOperators = -1;
			numReachableProps = -1;
			numReachableFluents = -1;			
		}

		return solvable;
	}

	private Operator groundInit(Action template, Operator instance) {
		int k;
		FluentDB fluentDB = null;
		init = null;

		GMathForm dur = groundMathForm(template.d_dynamic, fluentDB, null);
		instance.setDuration(dur);

		GMathForm m = groundMathForm(template.c_dynamic, fluentDB, dur);
		instance.setCost(m);

		LiftedTest testTemplate;
		for (k = 0; k < template.numTest(); k++) {
			testTemplate = template.getTest(k);

			GMathForm l = groundMathForm(testTemplate.getLeftSide(),
					init.fluentDB, dur);
			GMathForm r = groundMathForm(testTemplate.getRightSide(),
					init.fluentDB, dur);
			GMathForm t = groundMathForm(template.getTestTime(k), fluentDB, dur);

			instance.testList[k] = new Test(testTemplate.op, l, r, t);
		}

		LiftedSet setTemplate;
		for (k = 0; k < template.numSet(); k++) {
			setTemplate = template.getSet(k);

			GMathForm r = groundMathForm(setTemplate.getRightSide(), fluentDB,
					dur);
			GMathForm t = groundMathForm(template.getSetTime(k), fluentDB, dur);

			Fluent f;
			f = groundFunction(setTemplate.getLeftSide());
			f.isConstant = false; // to make sure rolling init state forward succeeds on deadline goals

			instance.setList[k] = new Set(setTemplate.getOp(), f.id, r, t);
		}

		Proposition prop;
		GMathForm time;
		int size;

		for (k = 0, size = template.numCondition(); k < size; k++) {
			prop = groundPredicate(template.getCondition(k));
			time = groundMathForm(template.getConditionTime(k), fluentDB, dur);

			instance.conditionList[k] = new Condition(prop.id, true, time);
		}

		for (k = 0, size = template.numProtectCondition(); k < size; k++) {
			prop = groundPredicate(template.getProtectCondition(k));

			instance.protectConditionList[k] = new ProtectCondition(prop.id, true);
		}

		int index = 0;
		for (k = 0, size = template.numAdd(); k < size; k++) {
			prop = groundPredicate(template.getAdd(k));
			time = groundMathForm(template.getAddTime(k), fluentDB, dur);

			//prop.isConstant = false;

			instance.effectList[index++] = new Effect(prop.id, true, time);
		}

		instance.deleteIndex = index;

		for (k = 0, size = template.numDelete(); k < size; k++) {
			prop = groundPredicate(template.getDelete(k));
			time = groundMathForm(template.getDeleteTime(k), fluentDB, dur);

			//prop.isConstant = false;

			instance.effectList[index++] = new Effect(prop.id, false, time);
		}


		init = new State(instance);

		for (Proposition p : propositions.symbols) {
			p.isConstant = true;
			boolean v = init.propDB.holds(p.id);
			p.value = v; 
			if (v)
				updateDependents(p.name.get(0),p);
			p.size = 0;
		}

		for (Priority<Effect> pr : init.events) {
			Effect e = pr.event;
			Proposition p = propositions.symbols.get(e.id);
			boolean v = e.value;
			if (p.value != v) {
				p.isConstant = false;
			}
			if (v)
				updateDependents(p.name.get(0),p);
		}

		for (Fluent f : fluents.symbols) {
			f.isConstant = true;
			float v = init.fluentDB.value(f.id);
			f.value = v;
			if (v == v)
				updateDependents(f.name.get(0),f);
		}

		for (Priority<Set> pr : init.sets) {
			Set s = pr.event;
			Fluent f = fluents.symbols.get(s.id);
			if (s.op != 0)
				f.isConstant = false;
			else {
				float v = s.rightSide.value(init.fluentDB,init.time);
				if (v != v) {
					if (f.value == f.value)
						f.isConstant = false;
				} else {
					if (f.value != v) {
						f.isConstant = false;
						updateDependents(f.name.get(0),f);
					}
				}
			}
		}


		return instance;
	}

	private void name(Operator instance) {
	}

	public int numAct() {
		return operators.count;
	}

	public int numFunc() {
		return fluents.count;
	}

	public int numProp() {
		return propositions.count;
	}

	private boolean preGroundAction(Action template, Operator instance) {

		if (!preGroundMathForm(template.d_dynamic, null))
			return false;

		int k;
		int size;

		for (k = 0, size = template.numCondition(); k < size; k++) {
			if (!preGroundPredicate(template.getCondition(k)))
				return false;
			if (!preGroundMathForm(template.getConditionTime(k),
					template.d_dynamic))
				return false;
		}

		LiftedTest liftedTestTemplate;
		for (k = 0, size = template.numTest(); k < size; k++) {
			liftedTestTemplate = template.getTest(k);

			if (!preGroundMathForm(liftedTestTemplate.getLeftSide(),
					template.d_dynamic))
				return false;
			if (!preGroundMathForm(liftedTestTemplate.getRightSide(),
					template.d_dynamic))
				return false;
			if (!preGroundMathForm(template.getTestTime(k), template.d_dynamic))
				return false;
		}

		for (k = 0, size = template.numProtectCondition(); k < size; k++) {
			if (!preGroundPredicate(template.getProtectCondition(k)))
				return false;
		}

		for (k = 0, size = template.numProtectTest(); k < size; k++) {
			liftedTestTemplate = template.getProtectTest(k);
			if (!preGroundMathForm(liftedTestTemplate.getLeftSide(),
					template.d_dynamic))
				return false;
			if (!preGroundMathForm(liftedTestTemplate.getRightSide(),
					template.d_dynamic))
				return false;
			return false;
		}

		LiftedSet setTemplate;
		for (k = 0; k < template.numSet(); k++) {
			setTemplate = template.getSet(k);

			if (!preGroundMathForm(setTemplate.getRightSide(),
					template.d_dynamic))
				return false;
			if (!preGroundMathForm(template.getSetTime(k), template.d_dynamic))
				return false;
		}

		for (k = 0, size = template.numAdd(); k < size; k++) {
			if (!preGroundMathForm(template.getAddTime(k), template.d_dynamic))
				return false;
		}

		for (k = 0, size = template.numDelete(); k < size; k++) {
			if (!preGroundMathForm(template.getDeleteTime(k),
					template.d_dynamic))
				return false;
		}

		if (!preGroundMathForm(template.c_dynamic, template.d_dynamic))
			return false;

		return true;
	}

	private boolean preGroundFunction(Function template) {
		Fluent tempF = fluents.get(template.bind(map));
		if (tempF == null || (tempF.isConstant && tempF.value != tempF.value))
			return false;
		return true;
	}

	private boolean preGroundMathForm(MathForm template, MathForm dur) {
		switch (template.type) {
		case 2:
			// ?
			return false;
		case 1:
			return true;
		case 5:
			if (dur == null)
				return false;
			return true;
		case 0:
			return preGroundFunction(template.function);
		case 3:
			if (!preGroundMathForm(template.left, dur)
					|| !preGroundMathForm(template.right, dur))
				return false;
			return true;
		default:
			return false;
		}
	}

	private boolean preGroundPredicate(Predicate template) {
		Proposition p = propositions.get(template.bind(map));
		if (p == null)
			return false;
		// only works for purely positive preconditions. Otherwise we need
		// to pass in the value of the condition.
		if (p.isConstant)
			return p.value;
		return true;
	}

	public boolean premerge(Problem pu) {
		return false;
	}

	public boolean pregoals(Problem pu) {
		return true;
	}

	public void updateGoals(Problem pu) {
		// TODO Auto-generated method stub
	}

}
