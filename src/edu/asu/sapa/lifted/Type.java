package edu.asu.sapa.lifted;

import java.util.Collection;
import java.util.HashSet;


public class Type extends Symbol<String> {
	public HashSet<Type> subTypes = new HashSet<Type>();
	public HashSet<Constant> constants = new HashSet<Constant>();
	public Type(String n) {
		super(n);
	}
	public void initGrounding() {
		for (Type sub : subTypes){
			sub.initGrounding();
			constants.addAll(sub.constants);
		}
	}
	
	public void addAll(Collection<? extends Symbol<String>> stuff) {
		for (Symbol<String> s : stuff) {
			if (s instanceof Type) 
				subTypes.add((Type)s);
			else if (s instanceof Constant)
				constants.add((Constant)s);
			else if (s instanceof Variable)
				((Variable)s).setType(this);
			else 
				System.err.println("Error thrown in Type.addAll()");
		}
	}
	
	public  <T extends Symbol<?>> T add(T s) {
		
		if (s instanceof Type) 
			subTypes.add((Type)s);
		else if (s instanceof Constant)
			constants.add((Constant)s);
		else if (s instanceof Variable)
			((Variable)s).setType(this);
		else 
			System.err.println("Error thrown in Type.add()");
		
		return s;
	}
}
