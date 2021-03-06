/* Generated By:JavaCC: Do not edit this line. PDDL21ParserConstants.java */
package edu.asu.sapa.parsing;


/**
 * Token literal values and constants.
 * Generated by org.javacc.parser.OtherFilesGen#start()
 */
public interface PDDL21ParserConstants {

  /** End of File. */
  int EOF = 0;
  /** RegularExpression Id. */
  int SINGLE_LINE_COMMENT = 6;
  /** RegularExpression Id. */
  int DEFINE = 7;
  /** RegularExpression Id. */
  int DOMAIN = 8;
  /** RegularExpression Id. */
  int FUNCTION = 9;
  /** RegularExpression Id. */
  int REQUIREMENTS = 10;
  /** RegularExpression Id. */
  int TYPES = 11;
  /** RegularExpression Id. */
  int CONSTANTS = 12;
  /** RegularExpression Id. */
  int PREDICATES = 13;
  /** RegularExpression Id. */
  int ACTION = 14;
  /** RegularExpression Id. */
  int DURATIVE_ACTION = 15;
  /** RegularExpression Id. */
  int VARS = 16;
  /** RegularExpression Id. */
  int PARAMETERS = 17;
  /** RegularExpression Id. */
  int COST = 18;
  /** RegularExpression Id. */
  int CONDITION = 19;
  /** RegularExpression Id. */
  int PRECONDITION = 20;
  /** RegularExpression Id. */
  int EFFECT = 21;
  /** RegularExpression Id. */
  int ASSIGN = 22;
  /** RegularExpression Id. */
  int SCALEUP = 23;
  /** RegularExpression Id. */
  int SCALEDOWN = 24;
  /** RegularExpression Id. */
  int INCREASE = 25;
  /** RegularExpression Id. */
  int DECREASE = 26;
  /** RegularExpression Id. */
  int DURATION = 27;
  /** RegularExpression Id. */
  int DURATION_VAR = 28;
  /** RegularExpression Id. */
  int AT = 29;
  /** RegularExpression Id. */
  int START = 30;
  /** RegularExpression Id. */
  int END = 31;
  /** RegularExpression Id. */
  int OVER = 32;
  /** RegularExpression Id. */
  int ALL = 33;
  /** RegularExpression Id. */
  int LOCALTIME = 34;
  /** RegularExpression Id. */
  int AND = 35;
  /** RegularExpression Id. */
  int OR = 36;
  /** RegularExpression Id. */
  int NOT = 37;
  /** RegularExpression Id. */
  int FORALL = 38;
  /** RegularExpression Id. */
  int WHEN = 39;
  /** RegularExpression Id. */
  int EXISTS = 40;
  /** RegularExpression Id. */
  int EITHER = 41;
  /** RegularExpression Id. */
  int PROBLEM = 42;
  /** RegularExpression Id. */
  int DOMAIN_TAG = 43;
  /** RegularExpression Id. */
  int OBJECT = 44;
  /** RegularExpression Id. */
  int GOAL = 45;
  /** RegularExpression Id. */
  int OPEN = 46;
  /** RegularExpression Id. */
  int INIT = 47;
  /** RegularExpression Id. */
  int METRIC = 48;
  /** RegularExpression Id. */
  int MAXIMIZE = 49;
  /** RegularExpression Id. */
  int MINIMIZE = 50;
  /** RegularExpression Id. */
  int LENGTH = 51;
  /** RegularExpression Id. */
  int SERIAL = 52;
  /** RegularExpression Id. */
  int PARALLEL = 53;
  /** RegularExpression Id. */
  int HARD = 54;
  /** RegularExpression Id. */
  int SOFT = 55;
  /** RegularExpression Id. */
  int UPDATE = 56;
  /** RegularExpression Id. */
  int EVENT = 57;
  /** RegularExpression Id. */
  int NOW = 58;
  /** RegularExpression Id. */
  int SENSE = 59;
  /** RegularExpression Id. */
  int A_NUMBER = 60;
  /** RegularExpression Id. */
  int EQUAL = 61;
  /** RegularExpression Id. */
  int PLUS = 62;
  /** RegularExpression Id. */
  int MINUS = 63;
  /** RegularExpression Id. */
  int MUL = 64;
  /** RegularExpression Id. */
  int DIVIDE = 65;
  /** RegularExpression Id. */
  int COMPARISON = 66;
  /** RegularExpression Id. */
  int VAR = 67;
  /** RegularExpression Id. */
  int CUTVAR = 68;
  /** RegularExpression Id. */
  int NAME = 69;
  /** RegularExpression Id. */
  int COMMA = 70;
  /** RegularExpression Id. */
  int OPENCURLYBRACKET = 71;
  /** RegularExpression Id. */
  int CLOSECURLYBRACKET = 72;
  /** RegularExpression Id. */
  int OPENBRACKET = 73;
  /** RegularExpression Id. */
  int CLOSEBRACKET = 74;
  /** RegularExpression Id. */
  int OPENBRACE = 75;
  /** RegularExpression Id. */
  int CLOSEBRACE = 76;
  /** RegularExpression Id. */
  int REQUIREMENT = 77;

  /** Lexical state. */
  int DEFAULT = 0;
  /** Lexical state. */
  int Requirement = 1;

  /** Literal token values. */
  String[] tokenImage = {
    "<EOF>",
    "\" \"",
    "\"\\t\"",
    "\"\\n\"",
    "\"\\r\"",
    "\"\\f\"",
    "<SINGLE_LINE_COMMENT>",
    "\"define\"",
    "\"domain\"",
    "\":functions\"",
    "\":requirements\"",
    "\":types\"",
    "\":constants\"",
    "\":predicates\"",
    "\":action\"",
    "\":durative-action\"",
    "\":vars\"",
    "\":parameters\"",
    "\":cost\"",
    "\":condition\"",
    "\":precondition\"",
    "\":effect\"",
    "<ASSIGN>",
    "<SCALEUP>",
    "<SCALEDOWN>",
    "<INCREASE>",
    "<DECREASE>",
    "\":duration\"",
    "\"?duration\"",
    "\"at\"",
    "\"start\"",
    "\"end\"",
    "\"over\"",
    "\"all\"",
    "\"#t\"",
    "\"and\"",
    "\"or\"",
    "\"not\"",
    "\"forall\"",
    "\"when\"",
    "\"exists\"",
    "\"either\"",
    "\"problem\"",
    "\":domain\"",
    "\":objects\"",
    "\":goal\"",
    "\":open\"",
    "\":init\"",
    "\":metric\"",
    "\"maximize\"",
    "\"minimize\"",
    "\":length\"",
    "\":serial\"",
    "\":parallel\"",
    "\"hard\"",
    "\"soft\"",
    "\":update\"",
    "\":events\"",
    "\":now\"",
    "\"sense\"",
    "<A_NUMBER>",
    "\"=\"",
    "\"+\"",
    "\"-\"",
    "\"*\"",
    "\"/\"",
    "<COMPARISON>",
    "<VAR>",
    "<CUTVAR>",
    "<NAME>",
    "\",\"",
    "\"{\"",
    "\"}\"",
    "\"[\"",
    "\"]\"",
    "\"(\"",
    "\")\"",
    "<REQUIREMENT>",
  };

}
