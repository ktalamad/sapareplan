package pspud_gen;

/**
 *  Extended from the original Random PSP Generator without Utility Dependencies
 *  (original files in CVS under project PSP_Generator/ps
 * @author Minh
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */

import edu.asu.sapa.parsing.*;
import edu.asu.sapa.basic_ds.*;

import java.io.*;
import java.util.*;

import java.text.*;

import edu.asu.sapa.parsing.PDDL21Parser;

public class Satellite_Gen {
	Domain dom;
	Problem prob;
	
	/**
	 * Check if an instrument ins is onboard of a satellite sat
	 * @param sat
	 * @param ins
	 * @return
	 */
	private boolean checkOnboard(String sat, String ins) {
		Predicate pred;
		int i;
		for(i = 0; i < prob.numInitPred(); i++) {
			pred = prob.getInitPred(i);
			if(!pred.getName().equals("on_board"))
				continue;
			
			if(pred.getObj(0).equals(ins) && pred.getObj(1).equals(sat))
				return true;
		}
		
		return false;
	}
	
	public void generate(Domain domain, Problem problem, long seed, String probFileName) {
		int i,j;
		dom = domain;
		prob = problem;
		Random random = new Random(seed);
		NumberFormat formatter = new DecimalFormat("0.00");
		
		String PSPFileName = new String(probFileName +"_psp");
		
		System.out.println("\n******* Generator for PSP version of ZenoTravel Domain ********");
		System.out.println("domain: " + domain.getName() +
							" | problem: " + problem.getName() +
							" | random-seed = " + seed);
		System.out.println("Input Problem File: " + probFileName +
							" | Output PSP File: " + PSPFileName);
		
		try {
			FileWriter out = new FileWriter(PSPFileName, false);
			out.write(";; Automatically generated by Satellite_Gen\n" +
					";; Original problem file: " + probFileName + " | Random Seed:");
			out.write(seed + "\n");
			
			// Output to file portions in the original file. Shared for all domains
			out.write("(define (problem " + problem.getName() + ")\n");
			out.write("(:domain " + domain.getName() + ")\n");
			out.write("(:objects\n");
			
			String type;
			ArrayList objects;		
			for(i = 0; i < problem.numObjectType(); i++) {
				type = problem.getObjectType(i);
				objects = problem.getObjectMap(type);
				
				out.write("\t");
				for(j = 0; j < objects.size(); j++)
					out.write((String) objects.get(j) + " ");
				out.write("- " + type + "\n");
			}
			
			// Original initial predicates and functions
			out.write(")\n(:init\n");
			Predicate pred;
			for(i = 0; i < problem.numInitPred(); i++) {
				pred = problem.getInitPred(i);
				
				out.write("\t(" + pred.getName());
				for(j = 0; j < pred.predSize(); j++)
					out.write(" " + pred.getObj(j));
				out.write(")\n");
			}
			out.write("\n");
			
			Function func;
			for(i = 0; i < problem.numInitFunct(); i++) {
				func = problem.getInitFunct(i);
				if(func.getName().toLowerCase().equals("boarding-time") ||
						func.getName().toLowerCase().equals("debarking-time"))
					continue;
				
				out.write("\t(= (" + func.getName());
				for(j = 0; j < func.funcSize(); j++)
					out.write(" " + func.getObj(j));
				out.write(") " + func.getValue() + ")\n");
			}
			
			/**
			 * Speciallized for the Satellite domain. Generate new functions 
			 */
			ArrayList satList = problem.getObjectMap("satellite");
			ArrayList insList = problem.getObjectMap("instrument");
			
			System.out.println("\n (slew_energy_rate ?satellite) : 0.5 - 2 (random)");
			out.write("\n\t;; (slew_energy_rate ?satellite) : 0.5 - 2 (random)\n");
			float randFloat;
			for(i = 0; i < satList.size(); i++) {
				out.write("\t(= (slew_energy_rate " + (String) satList.get(i) + ") ");
				randFloat = random.nextFloat();
				out.write(formatter.format(0.5 + (2.0-0.5)*randFloat) + ")\n");
			}
			
			System.out.println(" (switch_on_energy ?satellite ?instrument) : 10 - 20");
			out.write("\n\t;; (switch_on_energy ?satellite ?instrument) : 10 - 20\n");			
			for(i = 0; i < satList.size(); i++) {
				for(j = 0; j < insList.size(); j++) {
					if(!checkOnboard((String) satList.get(i), (String) insList.get(j)))
						continue;
					
					out.write("\t(= (switch_on_energy " + (String) satList.get(i) + 
							" " + (String) insList.get(j) + ") ");
					randFloat = random.nextFloat();
					out.write(formatter.format(10 + (20-10)*randFloat) + ")\n");
				}
			}
			
			System.out.println(" (switch_off_energy ?satellite ?instrument) : 5 - 15");
			out.write("\n\t;; (switch_off_energy ?satellite ?instrument) : 5 - 15\n");			
			for(i = 0; i < satList.size(); i++) {
				for(j = 0; j < insList.size(); j++) {
					if(!checkOnboard((String) satList.get(i), (String) insList.get(j)))
						continue;
					
					out.write("\t(= (switch_off_energy " + (String) satList.get(i) + 
							" " + (String) insList.get(j) + ") ");
					randFloat = random.nextFloat();
					out.write(formatter.format(5 + (15-5)*randFloat) + ")\n");
				}
			}
			
			System.out.println(" (calibration_energy_rate ?satellite ?instrument) : 20 - 40");
			out.write("\n\t;; (calibration_energy_rate ?satellite ?instrument) : 20 - 40\n");			
			for(i = 0; i < satList.size(); i++) {
				for(j = 0; j < insList.size(); j++) {
					if(!checkOnboard((String) satList.get(i), (String) insList.get(j)))
						continue;
					
					out.write("\t(= (calibration_energy_rate " + (String) satList.get(i) + 
							" " + (String) insList.get(j) + ") ");
					randFloat = random.nextFloat();
					out.write(formatter.format(20 + (40-20)*randFloat) + ")\n");
				}
			}
			
			System.out.println(" (data_process_energy_rate ?satellite ?instrument) : 1 - 10");
			out.write("\n\t;; (data_process_energy_rate ?satellite ?instrument) : 1 - 10\n");			
			for(i = 0; i < satList.size(); i++) {
				for(j = 0; j < insList.size(); j++) {
					if(!checkOnboard((String) satList.get(i), (String) insList.get(j)))
						continue;
					
					out.write("\t(= (data_process_energy_rate " + (String) satList.get(i) + 
							" " + (String) insList.get(j) + ") ");
					randFloat = random.nextFloat();
					out.write(formatter.format(1 + (10-1)*randFloat) + ")\n");
				}
			}
			
			
			
			out.write(")\n(:goal (and\n");
			
			// Output the new goal format
			System.out.println(" Goals: (pointing ?sat ?dir) - soft | (have_image ?dir ?mode) 80% soft");
			System.out.println(" Goal utility: random with bounds [2000 - 6000]. Doubled for hard goals.");
			out.write("\t;; Goals: (pointing ?sat ?dir) - soft | (have_image ?dir ?mode) 80% soft\n");
			out.write("\t;; Goal utility: random with bounds [2000 - 6000]. Doubled for hard goals.\n");
			ArrayList dirList = problem.getObjectMap("direction");
			ArrayList modeList = problem.getObjectMap("mode");
			ArrayList remainDir = new ArrayList(dirList);
			for(i = 0; i < problem.numGoal(); i++) {
				pred = problem.getGoal(i).gID;
				
				out.write("\t((" + pred.getName());
				out.write(" " + pred.getObj(0) + " " + pred.getObj(1) + ")");
				if(pred.getName().equals("pointing")) {
					out.write(" soft ");
					satList.remove(satList.indexOf(pred.getObj(0)));
					
					out.write(formatter.format(10000 + (20000-10000)*random.nextFloat()) + ")\n");	
				} else { // Goal: "have_image"
					if(random.nextFloat() > 0.2) {
						out.write(" soft ");
						out.write(formatter.format(10000 + (20000-10000)*random.nextFloat()) + ")\n");
					} else { 
						out.write(" hard ");
						out.write(formatter.format(2*(10000 + (20000-10000)*random.nextFloat())) + ")\n");	
					}
					remainDir.remove(remainDir.indexOf(pred.getObj(0)));
				}
				
			}
			out.write("\n");
			
			/*
			 * Add goals (have_image ?dir ?mode) to ?dir that did not already appeared
			 * in the original goal set. Also add (pointint ?sat ?dir) to satellite that did
			 * not appeared in the original goal list.
			 * All additional goals are soft
			 */
			int index;
			ArrayList newGoals = new ArrayList ();
			if(satList.size() > 0 || remainDir.size() > 0) {
//				out.write("\t;; Additional goals. Utility: 1000-5000 (random)\n");
//				for(i = 0; i < remainDir.size() / 3; i++) {
//					out.write("\t((have_image " + (String) remainDir.get(i));
//					// Randomly select the camera mode
//					index = random.nextInt(modeList.size());
//					out.write(" " + (String) modeList.get(index) + ") soft "
//							+ formatter.format(1000 + (5000-1000)*random.nextFloat())
//							+ ")\n");
//					
//					pred = new Predicate();
//					pred.setName("have_image");
//					pred.addObj((String) remainDir.get(i));
//					pred.addObj((String) modeList.get(index));
//					
//					newGoals.add(pred);
//				}
				
				for(i = 0; i < satList.size() / 3; i++) {
					out.write("\t((pointing " + (String) satList.get(i));
					// Randomly select the direction
					index = random.nextInt(dirList.size());
					out.write(" " + (String) dirList.get(index) + ") soft "
							+ formatter.format(1000 + (5000-1000)*random.nextFloat())
							+ ")\n");
					
					pred = new Predicate();
					pred.setName("pointing");
					pred.addObj((String) satList.get(i));
					pred.addObj((String) dirList.get(index));
					
					newGoals.add(pred);
				}
			}
					
			/*
			 * Goal utility dependencies: ({g1 g2 g3... gn} util-value)
			 */
			for(i = 0; i < problem.numGoal(); i++) {
				newGoals.add(problem.getGoal(i).gID);
			}
			
			int udSize, gIndex, numG = newGoals.size();
			ArrayList uds = new ArrayList(); // store generated uds for duplicate detection
			if(numG >= 2) {
//				int numUD = random.nextInt(2*maxNumGUD(numG));
				int numUD = Math.min(3*numG, (int) Math.pow(2,
						numG)
						- (numG + 1));
				ArrayList selected = new ArrayList();
				System.out.println(" Goal Dependencies (random):\n\t n = [0,2*min(3*|G|,2^|G|-(|G|-1)], size = [2,numG], u=[-2000,2000]");
				out.write("\n;; Goal Dependencies (random):\n\t;; n = [0,2*min(3*|G|,2^|G|-(|G|+1)], size = [2,numG], u=[-3000,3000]\n");
				for(i = 0; i < numUD; i++) {
					udSize = 2 + random.nextInt(numG-2);
					if (numG > 5 && i < (numUD / 2) && udSize > 5) {
						udSize = udSize / 3;
					}
					selected.clear();
					for(j = 0; j < udSize; j++) {
						while(true){
							gIndex = random.nextInt(numG);
							if (!selected.contains(new Integer(gIndex))) {
								selected.add(new Integer(gIndex));
								break;
							}
						}
					}
					
					if(notGenerated(uds,selected)) {
						uds.add(new ArrayList(selected));
						out.write("\t({");
						for(int k = 0; k < udSize; k++) {
							gIndex = ((Integer)selected.get(k)).intValue();
							pred = (Predicate) newGoals.get(gIndex);
							out.write("(" + pred.getName() + " "+ pred.getObj(0) + " " + pred.getObj(1) + ") ");
						}
						float value;
						int negChance = random.nextInt(2);
						// if (negChance == 1) { // negative
						if (i < numUD / 2) {
							value = -(10000 + (20000 - 10000)
									* random.nextFloat());
						} else {
							value = 4000 + (10000 - 4000) * random.nextFloat();
						}
						out.write("} " + formatter.format(value) + ")\n");
						
					}
				}
			}
			
			out.write("\t)\n");
			out.write("))\n");
		

			out.flush();
			out.close();
		} catch( IOException e) {
			System.err.println(e);
		}
	}

	/**
	 * Return the upper-bound on the number of goal utility dependencies.
	 * max(3*numG, n=2^|G|-(|G|+1)) with n is the number of possible goal set
	 * of size >= 2. 
	 */
	private int maxNumGUD (int numG) {
		return (int) Math.min(3*numG, Math.pow(2,numG)-(numG+1));
	}
	
	/**
	 * check fi ud is already generated and stored in uds
	 * @return
	 */
	private boolean notGenerated(ArrayList uds, ArrayList ud) {
		ArrayList tempUD;
		int i,j;
		for(i = 0; i < uds.size(); i++) {
			tempUD = (ArrayList) uds.get(i);
			if(tempUD.size() == ud.size()) {
				for(j = 0; j < ud.size(); j++)
					if(!tempUD.contains(ud.get(j)))
						break;
				if(j == ud.size())
					return false;
			}
		}
		return true;
	}
	/**
	 * Main program that call the parsers to read in the domain and problem files
	 * in PDDL2.1 then call the generator program to generate the new PSP Problem file.
	 * @param args
	 */
	
	public static void main(String args[]) {
		PDDL21Parser parser21 = new PDDL21Parser(System.in);
		Satellite_Gen satGen = new Satellite_Gen();
		System.out.println(";; " + System.getProperty("user.dir"));
		
			Domain domain;// = new Domain();
			Problem prob;// = new Problem();
			
			FileInputStream pddl_file;
			
			/*
			 * Instantiate a parser
			 */

			if( args.length != 3) {
				System.out.println("Usage: java ZenoTravel_Gen <domain> <problem> <random-seed>");
				return;
			}
			
			/*** Parse the Domain specification file ****/
			try {
				pddl_file = new java.io.FileInputStream(args[0]);
				PDDL21Parser.ReInit(pddl_file);
			} catch(java.io.FileNotFoundException e) {
				System.out.println("Domain file " + args[0] + " not found !!!");
				return;
			}
			
			try {
				domain = PDDL21Parser.parse_domain_pddl();
				System.out.println(";;Domain file succesfully read !!" + " num actions = " + domain.numAction());
			} catch(Exception e) {
				System.out.println("Exception while parsing domain file!!");
				e.printStackTrace();
				return;
			}
			
			/**** Parse the problem file ****/
			try {
				pddl_file = new java.io.FileInputStream(args[1]);
				PDDL21Parser.ReInit(pddl_file);
			} catch(java.io.FileNotFoundException e) {
				System.out.println("Problem file " + args[1] + " not found !!!");
				return;
			}
			
			try {
				prob = PDDL21Parser.parse_problem_pddl();
				System.out.println(";;Problem file succesfully read !!");
			} catch(Exception e) {
				System.out.println("Exception while parsing problem file!!");
				e.printStackTrace();
				return;
			}
			
			/*** Read the random seed (integer) ****/
			long seed = (new Long(args[2])).longValue();
			
			satGen.generate(domain, prob, seed, args[1]);
	}
}