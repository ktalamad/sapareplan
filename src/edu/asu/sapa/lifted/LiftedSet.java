/********************************************************************
   Author: Minh B. Do (Arizona State University - binhminh@asu.edu)
*********************************************************************/
package edu.asu.sapa.lifted;

/**
 * MySet: Represent the "Set" template from the domain file. Used 
 * to ground and store action's effects related to continuous function
 * in GMySet. Refer to GMySet for detail descriptions on components of
 * this class.
 */
public class LiftedSet {
    Function  leftSide;
    int op;
    MathForm rightSide;
    
    int setTime;    
    public final static int START = -1;
    public final static int END = 1;

    public LiftedSet() {
	leftSide = null;
	op = -1;
	rightSide = null;
	setTime = START;
    }

    public LiftedSet(String o, Function left, MathForm right) {
		leftSide = left;
		rightSide = right;
		setOp(o);
		setTime = START;
	}
    
    public LiftedSet(String o, Function left, MathForm right, int time) {
    	leftSide = left;
    	rightSide = right;
    	setOp(o);
    	setTime = time;
    }

    public void setOp(String o ) {
		switch (o.charAt(0)) {
		case '=':
			op = 0;
			break;
		case '+':
			op = 1;
			break;
		case '-':
			op = 2;
			break;
		case '*':
			op = 3;
			break;
		case '/':
			op = 4;
			break;
		default:
			op = -1;
			break;
		}    	
    }
    
    public int getOp() {
    	return op;
    }
    
    public void setLeftSide(Function l) {
	leftSide = l;
    }

    public Function getLeftSide( ) {
	return leftSide;
    }


    public void setRightSide(MathForm m) {
	rightSide = m;
    }

    public MathForm getRightSide() {
	return rightSide;
    }

    public String toString() {
	String s = "(" + op + " ";

	s += "(" + leftSide.toString() + ") " +
	    rightSide.toString() + ")";

	return s;
    }

	public int getSetTime() {
		return setTime;
	}

	public void setSetTime(int setTime) {
		this.setTime = setTime;
	}
}
