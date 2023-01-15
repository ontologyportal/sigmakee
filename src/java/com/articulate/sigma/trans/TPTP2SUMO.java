/** This code is copyright Articulate Software (c) 2003.
This software is released under the GNU Public License
<http://www.gnu.org/copyleft/gpl.html>.  Users of this code also consent,
by use of this code, to credit Articulate Software and Teknowledge in any
writings, briefings, publications, presentations, or other representations
of any software which incorporates, builds on, or uses this code.  Please
cite the following article in any publication with references:

Pease, A., (2003). The Sigma Ontology Development Environment, in Working
Notes of the IJCAI-2003 Workshop on Ontology and Distributed Systems,
August 9, Acapulco, Mexico. see also 
http://sigmakee.sourceforge.net 
*/

/*************************************************************************************************/
package com.articulate.sigma.trans;

import com.articulate.sigma.Formula;
import com.articulate.sigma.utils.StringUtil;
import tptp_parser.*;
import java.io.FileReader;
import java.util.*;

public class TPTP2SUMO {

    public static boolean debug = false;

  /** ***************************************************************
   * Remove binary cascading or's and and's and consolidate as single
   * connectives with more arguments.  For example
   * (and (and A B) C) becomes (and A B C)
   */
  public static Formula collapseConnectives(Formula form) {

      if (form.getFormula().indexOf("(and ") ==  -1 && form.getFormula().indexOf("(or ") ==  -1)
          return form;
      if (!form.isBalancedList())
          return form;
      if (debug) System.out.println("collapseConnectives(): input: " + form);
      ArrayList<Formula> args = form.complexArgumentsToArrayList(1);
      if (args == null)
          return form;
      StringBuffer sb = new StringBuffer();
      String pred = form.car();
      sb.append("(" + pred + " ");
      ArrayList<Formula> newargs = new ArrayList<>();
      if (debug) System.out.println("collapseConnectives(): args: " + args);
      for (Formula f : args)
          newargs.add(collapseConnectives(f));
      if (debug) System.out.println("collapseConnectives(): newargs: " + newargs);
      if (pred.equals("or") || pred.equals("and")) {
          for (Formula f : newargs) {
              if (f.car() != null && f.car().equals(pred)) {
                  if (debug) System.out.println("collapseConnectives(): matching connectives in " + f);
                  ArrayList<Formula> subargs = f.complexArgumentsToArrayList(1);
                  if (debug) System.out.println("collapseConnectives(): subargs " + subargs);
                  for (Formula f2 : subargs)
                      sb.append(f2.toString() + " ");
                  if (debug) System.out.println("collapseConnectives(): after adding to " + f + " result is " + sb);
              }
              else {
                  if (debug) System.out.println("collapseConnectives(): not matching connective in " + f);
                  if (debug) System.out.println("collapseConnectives(): adding to " + sb);
                  sb.append(f.toString() + " ");
              }
          }
      }
      else {
          for (Formula f : newargs)
            sb.append(f.toString() + " ");
      }
      sb.deleteCharAt(sb.length()-1);
      sb.append(")");
      Formula newForm = new Formula(sb.toString());
      if (debug) System.out.println("collapseConnectives(): result: " + newForm);
      return newForm;
  }

  /** ***************************************************************
   */
  private static String addIndent (int indent, int indented) {

      String res = "";
      for (int i = indented+1; i <= indent; i++) 
          res += " ";      
      return res;    
  }

  /** ***************************************************************
   * remove dollar sign, for special tptp terms such as $false and $true
   */
  private static String removeDollarSign (String argument) {

      if (argument.length() > 0) {
          if (argument.charAt(0) == '$') 
              return argument.substring(1,argument.length());
          else
              return argument;          
      }
      return "";
  }
  
  /** ***************************************************************
   * remove termVariablePrefix
   */
  private static String transformVariable (String variable) {

      return variable.replace(Formula.termVariablePrefix, "");
  } 

  /** ***************************************************************
   * remove termSymbolPrefix and termMentionSuffix
   */
  public static String transformTerm (String term) {

      term = term.replaceFirst(Formula.termSymbolPrefix, "");
      term = term.replace(Formula.termMentionSuffix, "");
      if (term.matches(".*__\\d"))
          term = term.substring(0,term.length()-3);
      return term;
  }

    /** ***************************************************************
     * Convert a TPTP formula (without metadata) to SUMO, by wrapping
     * it with dummy metadata
     */
    public static String formToSUMO (String clause) {

        clause = "fof(test,axiom," + clause + ").";
        if (debug) System.out.println("formToSUMO() " + clause);
        TPTPVisitor sv = new TPTPVisitor();
        sv.parseString(clause);
        HashMap<String, TPTPFormula> hm = sv.result;
        if (debug) {
            for (String s : hm.keySet()) {
                System.out.println(hm.get(s));
                System.out.println("\t" + hm.get(s).sumo + "\n");
            }
        }
        String result = null;
        for (String s : hm.keySet())
            result = hm.get(s).sumo;
        return result;
    }

    /** ***************************************************************
     * Convert a TPTP formula (with metadata) to SUMO
     */
    public static String toSUMO (String clause) {

        if (debug) System.out.println("toSUMO() " + clause);
        TPTPVisitor sv = new TPTPVisitor();
        sv.parseString(clause);
        HashMap<String, TPTPFormula> hm = sv.result;
        if (debug) {
            for (String s : hm.keySet()) {
                System.out.println(hm.get(s));
                System.out.println("\t" + hm.get(s).sumo + "\n");
            }
        }
        String result = null;
        for (String s : hm.keySet())
            result = hm.get(s).sumo;
        return result;
    }

    /** ***************************************************************
     */
    public static void showHelp() {

        System.out.println("KB class");
        System.out.println("  options:");
        System.out.println("  -h - show this help screen");
        System.out.println("  -c - parse test clause");
        System.out.println("  -t \"<form>\" - convert a formula");
        System.out.println("  -f <fname> - convert file to SUO-KIF");
    }

  /** ***************************************************************
   */
  public static void main (String args[]) {

      System.out.println("INFO in TPTP2SUMO.main() with args: " + Arrays.toString(args));
      if (args != null && args.length > 0 && args[0].equals("-h"))
          showHelp();
      else {
          //String formula = "fof(1,axiom,(    s_holds_2__(s_p,s_a) ),    file('/tmp/SystemOnTPTP11002/Simple2965.tptp',kb_Simple_1)).fof(2,conjecture,(    s_holds_2__(s_p,s_a) ),    file('/tmp/SystemOnTPTP11002/Simple2965.tptp',prove_from_Simple)).fof(3,negated_conjecture,(    ~ s_holds_2__(s_p,s_a) ),    inference(assume_negation,[status(cth)],[2])).fof(4,negated_conjecture,(    ~ s_holds_2__(s_p,s_a) ),    inference(fof_simplification,[status(thm)],[3,theory(equality)])).cnf(5,plain,    ( s_holds_2__(s_p,s_a) ),    inference(split_conjunct,[status(thm)],[1])).cnf(6,negated_conjecture,    ( ~ s_holds_2__(s_p,s_a) ),    inference(split_conjunct,[status(thm)],[4])).cnf(7,negated_conjecture,    ( $false ),    inference(rw,[status(thm)],[6,5,theory(equality)])).cnf(8,negated_conjecture,    ( $false ),    inference(cn,[status(thm)],[7,theory(equality)])).cnf(9,negated_conjecture,    ( $false ),    8,    [proof]).";
          //String formula = "fof(pel55,conjecture,(killed(X,Z) )). cnf(1,plain,( agatha = butler| hates(agatha,agatha) ),inference(subst,[[X,$fot(X0)]],[pel55])). cnf(6,plain,( a ) , inference(subst,[[X0,$fot(skolemFOFtoCNF_X)],[Z,$fot(a)]],[1])).";
          //String formula = "fof(pel55_1,axiom,(? [X] : (lives(X) & killed(X,agatha) ) )).fof(pel55,conjecture,(? [X] : killed(X,agatha) )).cnf(0,plain,(killed(skolemFOFtoCNF_X,agatha)), inference(fof_to_cnf,[],[pel55_1])).cnf(1,plain,(~killed(X,agatha)),inference(fof_to_cnf,[],[pel55])).cnf(2,plain,(~killed(skolemFOFtoCNF_X,agatha)),inference(subst,[[X,$fot(skolemFOFtoCNF_X)]],[1])).cnf(3,theorem,($false),inference(resolve,[$cnf(killed(skolemFOFtoCNF_X,agatha))],[0,2])).";
          //String formula = "fof(ax1,axiom,(! [X0] : (~s__irreflexiveOn(s__relatedInternalConcept__m,X0) | ! [X1] : (~s__instance(X0,s__Class) | ~s__instance(X1,X0) | ~s__relatedInternalConcept(X1,X1))))).";
          //String clause  = "cnf(ax1,axiom,(~s__irreflexiveOn(s__relatedInternalConcept__m,X0) | ~s__instance(X0,s__Class) | ~s__instance(X1,X0) | ~s__relatedInternalConcept(X1,X1))).";
          String clause = "cnf(c_0_10,negated_conjecture,($answer(esk1_1(X1))|~s__subclass(X1,s__Object)), c_0_8).";
          try {
              if (args.length > 0 && args[0].equals("-c")) {
                  debug = true;
                  System.out.println("main parse " + clause);
                  toSUMO(clause);
              }
              if (args.length > 1 && args[0].equals("-t")) {
                  debug = true;
                  clause = StringUtil.removeEnclosingQuotes(args[1]);
                  System.out.println("main parse " + clause);
                  formToSUMO(clause);
              }
              if (args.length > 1 && args[0].equals("-f")) {
                  tptp_parser.TPTPVisitor sv = new TPTPVisitor();
                  System.out.println("main parse file " + args[1]);
                  sv.parseFile(args[1]);
                  HashMap<String, TPTPFormula> hm = sv.result;
                  for (String s : hm.keySet()) {
                      System.out.println(hm.get(s));
                      System.out.println("\t" + hm.get(s).sumo + "\n");
                  }
              }
          }
          catch (Exception e) {
              System.out.println("e: " + e);
          }
      }
  }
}
 
