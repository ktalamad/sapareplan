/**********************************************************************
 Author: Minh B. Do - Arizona State University
 ***********************************************************************/

package edu.asu.sapa.rmtpg;

import edu.asu.sapa.ground.Proposition;

/**
 * FactLink: Class to represent a fact, and links with actions which link to
 * that fact (has a fact as its precond or effects)
 */
public class FactLink extends CostFunction<Proposition> {

	public FactLink() {
		super();
	}

	public FactLink(Proposition p) {
		super(p);
	}

}
