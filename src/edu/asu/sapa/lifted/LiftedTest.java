/****************************************************
   Author: Minh B. Do (Arizona State University)
 *****************************************************/
package edu.asu.sapa.lifted;

/**
 *  Test: The template for "Test", a (pre)condition involving
 *  a comparison related to a continuous function. Consult description
 *  for class GTest for explanation on components.
 */
public class LiftedTest {
	public int  op;
	MathForm leftSide;
	MathForm rightSide;
	
	// -1 for start, 0 for over all, and 1 for end
	int evalTime;
	
	public final static int START = -1;
	public final static int OVERALL = 0;
	public final static int END = 1;

	public LiftedTest() {
		op = -1;
		leftSide = null;
		rightSide = null;
		evalTime = LiftedTest.START;
	}

	public LiftedTest(String c, MathForm left, MathForm right) {
		leftSide = left;
		rightSide = right;
		setOp(c);
		evalTime = LiftedTest.START;
	}
	
	public LiftedTest (String c, MathForm left, MathForm right, int eval) {
		leftSide = left;
		rightSide = right;
		setOp(c);
		evalTime = eval;
	}

	public void setOp(String c) {
		op=-1;
		if (c.length()==2) {
			switch(c.charAt(0)) {
			case '=':
				op = 0; break;
			case '<':
				op = 2; break;
			case '>':
				op = 4; break;
			}
		} else {
			switch(c.charAt(0)) {
			case '<':
				op = 1; break;
			case '>':
				op = 3; break;
			}
		}    	
	}

	public void setLeftSide(MathForm l) {
		leftSide = l;
	}

	public MathForm getLeftSide( ) {
		return leftSide;
	}


	public void setRightSide(MathForm m) {
		rightSide = m;
	}

	public MathForm getRightSide() {
		return rightSide;
	}
	
	public void setEvalTime (int eval) {
		evalTime = eval;
	}
	
	public int getEvalTime () {
		return evalTime;
	}

	public String toString() {
		String s = "(";

		String opS="??";
		switch(op) {
		case 0:
			opS = "=="; break;
		case 1:
			opS = "<"; break;
		case 2:
			opS = "<="; break;
		case 3:
			opS = ">"; break;
		case 4:
			opS = ">="; break;
		}

		s += opS + " " + leftSide.toString() + " " +
		rightSide.toString() + ")";

		return s;
	}
}
