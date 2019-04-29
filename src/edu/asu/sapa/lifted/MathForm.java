/*************************************************
    Author: Minh B. Do (ASU - binhminh@asu.edu)
**************************************************/
package edu.asu.sapa.lifted;

import edu.asu.sapa.Planner;

/**
 * MathForm: Represent the abstract template class parsed from the domain
 * file. Used to make ground math form (stored in GMathForm). Structurally
 * similar to GMathForm. Refer to that class for member functions.
 */
public class MathForm {
	public static final MathForm zero = new MathForm(0f);
	public static final MathForm one = new MathForm(1f);
	public static final MathForm dur = new MathForm();
	public static final MathForm epsilon = new MathForm(Planner.EPSILON);
	
	public int type; // 0: function, 1: float value, 2: time (#t), 3: Non Primititive, 
    // 5: = ?duration 
	public float value;  
    public Function function; 

    public char op; // +, -, *, /

    public MathForm left;
    public MathForm right;

    public MathForm() {
    	type = 5;
    }

    public MathForm(char op, MathForm l, MathForm r) {
    	type=3;
    	this.op = op;
    	left = l;
    	right = r;
    }

    public MathForm(float v) {
    	type=1;
    	value = v;
    }

    public MathForm(Function f) {
    	type=0;
    	function = f;
    }

    public void set (MathForm mf) {
    	type = mf.type;
    	value = mf.value;
    	function = mf.function;
    	op = mf.op;
    	left = mf.left;
    	right = mf.right;
    }
    
    public void setType(int b) {
	type = b;
    }

    public int getType() {
	return type;
    }

    public void setValue(float f) {
	value = f;
    }

    public float getValue() {
	return value;
    }

    public void setElement(Function e) {
	function = e;
    }

    public Function getElement() {
	return function;
    }

    public void setOperator(char c) {
	op = c;
    }

    public char getOperator() {
	return op;
    }

    public void setLeft(MathForm m) {
	left = m;
    }

    public MathForm getLeft() {
	return left;
    }

    public void setRight(MathForm m) {
	right = m;
    }

    public MathForm getRight() {
	return right;
    }

    public String toString() {
	if( type == 0 )
	    return "(" + function.toString() + ")";

	if( type == 1)
	    return (new Float(value)).toString();

	if( type == 2 )
	    return "#t";

	if (type == 5)
		return "?duration";
	
	if (type == 3)
		return "(" +  op + " " + left.toString() 
	    	+ " " + right.toString() + ")";
	
	return "unknown type: " + type;
    }
}
