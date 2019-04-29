/**********************************************************************
 Author: Minh B. Do - Arizona State University
 ***********************************************************************/

package edu.asu.sapa.rmtpg;

import edu.asu.sapa.ground.Operator;

/**
 * ActLink: Class to represent an Action, and links with facts which are its
 * preconds or effects. This class is similar to FactLink class and is much
 * simpler than GAction class. It is used in the relaxed bi-level planning graph
 */
public class ActLink extends CostFunction<Operator> {
	public ActLink(Operator o) {
		super(o);
	}

}
