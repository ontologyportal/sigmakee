package com.articulate.sigma;

import java.io.*;
import java.util.*;
import tptp_parser.*;
import TPTPWorld.*;

public class TPTP2SUMO {

  public static void main (String args[]) {
    //String formula = "fof(1,axiom,(    s_holds_2__(s_p,s_a) ),    file('/tmp/SystemOnTPTP11002/Simple2965.tptp',kb_Simple_1)).fof(2,conjecture,(    s_holds_2__(s_p,s_a) ),    file('/tmp/SystemOnTPTP11002/Simple2965.tptp',prove_from_Simple)).fof(3,negated_conjecture,(    ~ s_holds_2__(s_p,s_a) ),    inference(assume_negation,[status(cth)],[2])).fof(4,negated_conjecture,(    ~ s_holds_2__(s_p,s_a) ),    inference(fof_simplification,[status(thm)],[3,theory(equality)])).cnf(5,plain,    ( s_holds_2__(s_p,s_a) ),    inference(split_conjunct,[status(thm)],[1])).cnf(6,negated_conjecture,    ( ~ s_holds_2__(s_p,s_a) ),    inference(split_conjunct,[status(thm)],[4])).cnf(7,negated_conjecture,    ( $false ),    inference(rw,[status(thm)],[6,5,theory(equality)])).cnf(8,negated_conjecture,    ( $false ),    inference(cn,[status(thm)],[7,theory(equality)])).cnf(9,negated_conjecture,    ( $false ),    8,    [proof]).";
    //String formula = "fof(pel55,conjecture,(killed(X,Z) )). cnf(1,plain,( agatha = butler| hates(agatha,agatha) ),inference(subst,[[X,$fot(X0)]],[pel55])). cnf(6,plain,( a ) , inference(subst,[[X0,$fot(skolemFOFtoCNF_X)],[Z,$fot(a)]],[1]))."; 
    String formula = "fof(pel55_1,axiom,(   ? [X] :       ( lives(X)      & killed(X,agatha) ) )).fof(pel55,conjecture,(    ? [X] : killed(X,agatha) )).cnf(0,plain,    ( killed(skolemFOFtoCNF_X,agatha) ),    inference(fof_to_cnf,[],[pel55_1])).cnf(1,plain,    ( ~ killed(X,agatha) ),    inference(fof_to_cnf,[],[pel55])).cnf(2,plain,    ( ~ killed(skolemFOFtoCNF_X,agatha) ),    inference(subst,[[X,$fot(skolemFOFtoCNF_X)]],[1])).cnf(3,theorem,    ( $false ),    inference(resolve,[$cnf(killed(skolemFOFtoCNF_X,agatha))],[0,2])).";
    String inFile;
    if (args.length == 1) {
      inFile = args[0];
    } else {
      System.out.println("give file name");
      return;
    }
    FileReader file;
    String kif = "";
    try {
      file = new FileReader(inFile);
      kif = TPTP2SUMO.convert(file, false);
    } catch (Exception e) { 
      System.out.println("e: " + e);
    }

    System.out.println("START---");
    System.out.println(kif);
    System.out.println("END-----");
  }

  public static String convert (String tptp, boolean instantiated) throws Exception {    
    return convert(new BufferedReader(new StringReader(tptp)), null, instantiated);
  }

  public static String convert (Reader in, boolean instantiated) throws Exception {
    return convert(new BufferedReader(in), null, instantiated);
  }

  public static String convert (String tptp, ArrayList<Binding> answer, boolean instantiated) throws Exception {
    return convert(new BufferedReader(new StringReader(tptp)), answer, instantiated);
  }

  public static String convert (Reader in, ArrayList<Binding> answer, boolean instantiated) throws Exception {
    return convert(new BufferedReader(in), answer, instantiated);
  }

  public static String convert (BufferedReader reader, ArrayList<Binding> answer, boolean instantiated) throws Exception {
    StringBuffer result = new StringBuffer();
    TPTPParser parser = TPTPParser.parse(reader);
    Hashtable<String,TPTPFormula> ftable = parser.ftable;
    Vector<SimpleTptpParserOutput.TopLevelItem> Items = parser.Items;
    System.out.println("# of formulas: " + ftable.size());
    System.out.println("# of Items: " + Items.size());

    //----Start SUMO output
    result.append("<queryResponse>\n");
    result.append("  <answer result='yes' number='1'>\n");

    //----Start proof output
    StringBuffer proof = new StringBuffer();
    proof.append("    <proof>\n");
    for (SimpleTptpParserOutput.TopLevelItem item : Items) {
      String name = TPTPParser.getName(item);
      TPTPFormula formula = ftable.get(name);
      proof.append(convertTPTPFormula(formula, ftable, instantiated));
    }
    //----End proof output
    proof.append("    </proof>\n");

    // print out answer (if exists)
    // (CURRENTLY: only metis proof prints variable bindings)
    StringBuffer binding = new StringBuffer();
    ArrayList<Binding> binds;
    if (answer != null && !answer.isEmpty()) {
      binds = answer;
    } else {
      binds = AnswerExtractor.extractAnswers(ftable);
    }
    if (!binds.isEmpty()) {
      binding.append("  <bindingSet type='definite'>\n");
      binding.append("    <binding>\n");
      for (Binding bind : binds) {
        assert bind.binding != null;
        binding.append("      <var name='?" + transformVariable(bind.variable) + "' value='" + transformTerm(bind.binding) + "'/>\n");
      }
      binding.append("    </binding>\n");
      binding.append("  </bindingSet>\n");
    }

    //----Append proof after bindings (xml order matters)
    result.append(binding);
    result.append(proof);

    //----End SUMO output
    result.append("  </answer>\n");
    result.append("  <summary proofs='1'/>\n");
    result.append("</queryResponse>\n");

    return result.toString();
  }

  private static StringBuffer convertTPTPFormula (TPTPFormula formula, Hashtable<String,TPTPFormula> ftable, boolean instantiated) {
    StringBuffer result = new StringBuffer();
    int indent = 12;
    int indented = 0;
    Vector<String> parents = new Vector();
    
    result.append("      <proofStep>\n");
    result.append("        <premises>\n");
    SimpleTptpParserOutput.Annotations annotations = null;    
    SimpleTptpParserOutput.Source source = null;
    String sourceInfo = "";

    //----Add parents info as "premises"
    for (TPTPFormula parent : formula.parent) {
      result.append("          <premise>\n");
      result.append(convertType(parent, indent+2, indented));
      result.append("          </premise>\n");
    }
    result.append("        </premises>\n");
    result.append("        <conclusion>\n");
    result.append(convertType(formula, indent, indented));
		if (formula.parent.isEmpty()) {
			if (formula.type.equals("conjecture")) {
				if (!instantiated) {
					result.append("          <query type='" + ProofStep.QUERY + "'/>\n");
				} else {
					result.append("          <query type='" + ProofStep.INSTANTIATED_QUERY + "'/>\n");
				}
			} else if (formula.type.equals("negated_conjecture")) {
				result.append("          <query type='" + ProofStep.NEGATED_QUERY + "'/>\n");
			}
		}
    result.append("        </conclusion>\n");
    result.append("      </proofStep>\n");
    return result;
  }

  private static StringBuffer convertType (TPTPFormula formula, int indent, int indented) {
    StringBuffer result = new StringBuffer();
    String type = "";
    int id = formula.id;
    SimpleTptpParserOutput.TopLevelItem item = formula.item;
    if (item.getKind() == SimpleTptpParserOutput.TopLevelItem.Kind.Formula) {
      SimpleTptpParserOutput.AnnotatedFormula AF = ((SimpleTptpParserOutput.AnnotatedFormula)item);
      type = "formula";
      result.append(addIndent(indent-2,indented));
      result.append("<" + type + " number='" + id + "'>\n");
      result.append(convertFormula(AF.getFormula(),indent,indented));
      result.append("\n");
      result.append(addIndent(indent-2,indented));
      result.append("</" + type + ">\n");
    } else if (item.getKind() == SimpleTptpParserOutput.TopLevelItem.Kind.Clause) {
      SimpleTptpParserOutput.AnnotatedClause AC = ((SimpleTptpParserOutput.AnnotatedClause)item);
      type = "clause";
      result.append(addIndent(indent-2,indented));
      result.append("<" + type + " number='" + id + "'>\n");
      result.append(convertClause(AC.getClause(),indent,indented));
      result.append("\n");
      result.append(addIndent(indent-2,indented));
      result.append("</" + type + ">\n");
    } else {
      result.append("Error: TPTP Formula syntax unknown for converting");
    }
    return result;
  }

  private static StringBuffer convertConnective (SimpleTptpParserOutput.BinaryConnective connective) {
    StringBuffer result = new StringBuffer();
    switch (connective) {
    case And:
      result.append("and");
      break;
    case Or:
      result.append("or");
      break;
    case Equivalence: 
      result.append("<=>");
      break;
    case Implication:
      result.append("=>");
      break;
    case ReverseImplication:
      result.append("<=");
      break;
    case Disequivalence:
      result.append("not <=>");
      break;
    case NotOr:
      result.append("not or");
      break;
    case NotAnd:
      result.append("not and");
      break;
    default:
      result.append("Not a connective");
      break;
    }
    return result;
  }

  private static String convertQuantifier (SimpleTptpParserOutput.Quantifier quantifier) {
    switch (quantifier) {
    case ForAll:
      return "forall";
    case Exists:
      return "exists";
    default:
      return "Not a quantifier";
    }
  }

  private static boolean kifAssociative (SimpleTptpParserOutput.BinaryConnective connective) {
    switch (connective) {
    case And:
    case Or:
      return true;
    default:
      return false;
    }
  }

  private static String addIndent (int indent, int indented) {
    String res = "";
    for (int i = indented+1; i <= indent; i++) {
      res += " ";
    }
    return res;    
  }

  // remove dollar sign, for special tptp terms such as $false and $true
  private static String removeDollarSign (String argument) {
    if (argument.length() > 0) {
      if (argument.charAt(0) == '$') {
        return argument.substring(1,argument.length());
      } else {
        return argument;
      }
    }
    return "";
  }
  
  // remove termVariablePrefix
  private static String transformVariable (String variable) {
    return variable.replace(Formula.termVariablePrefix, "");
  } 

  // remove termSymbolPrefix and termMentionSuffix
  private static String transformTerm (String term) {
    term = term.replace(Formula.termSymbolPrefix, "");
    term = term.replace(Formula.termMentionSuffix, "");
    return term;
  }

  private static String convertTerm (SimpleTptpParserOutput.Formula.Atomic atom) {
    String res = "";
    LinkedList<SimpleTptpParserOutput.Term> arguments = (LinkedList)atom.getArguments();
    if (arguments != null) {
      res += "(";
    }
    res += transformTerm(removeDollarSign(atom.getPredicate()));
    if (arguments != null) {
      for (int n = 0; n < arguments.size();  n++) {
        if (((SimpleTptpParserOutput.Term)arguments.get(n)).getTopSymbol().isVariable()) {
          res += " " + "?" + transformVariable(arguments.get(n).toString());
        } else {
          res += " " + transformTerm(removeDollarSign(arguments.get(n).toString()));
        }
      }
    }
    if (arguments != null) {
      res += ")";
    }
    return res;
  }

  private static StringBuffer convertFormula (SimpleTptpParserOutput.Formula formula, int indent, int indented) {
    StringBuffer result = new StringBuffer();
    switch(formula.getKind()) {
    case Atomic:
      result.append(addIndent(indent,indented));
      result.append(convertTerm((SimpleTptpParserOutput.Formula.Atomic)formula));
      break;
    case Negation:
      result.append(addIndent(indent,indented));
      result.append("(" + "not" + " ");
      result.append(convertFormula(((SimpleTptpParserOutput.Formula.Negation)formula).getArgument(),indent+4,indent+4));
      result.append(")");
      break;
    case Binary:
      result.append(addIndent(indent,indented));
      result.append("(");
      result.append(convertConnective(((SimpleTptpParserOutput.Formula.Binary)formula).getConnective()));
      result.append(" ");
      result.append(convertFormula(((SimpleTptpParserOutput.Formula.Binary)formula).getLhs(),indent+4,indent+4));
      result.append("\n");
      while (kifAssociative(((SimpleTptpParserOutput.Formula.Binary)formula).getConnective()) &&
             ((SimpleTptpParserOutput.Formula.Binary)formula).getRhs().getKind() == SimpleTptpParserOutput.Formula.Kind.Binary &&
             ((SimpleTptpParserOutput.Formula.Binary)formula).getConnective() == 
               ((SimpleTptpParserOutput.Formula.Binary)((SimpleTptpParserOutput.Formula.Binary)formula).getRhs()).getConnective()) {
        formula = ((SimpleTptpParserOutput.Formula.Binary)formula).getRhs();
        result.append(convertFormula(((SimpleTptpParserOutput.Formula.Binary)formula).getLhs(),indent+4,0));
        result.append("\n");
      }
      result.append(convertFormula(((SimpleTptpParserOutput.Formula.Binary)formula).getRhs(),indent+4,0));
      result.append(")");
      break;
    case Quantified:
      result.append(addIndent(indent,indented));
      result.append("(");
      result.append(convertQuantifier(((SimpleTptpParserOutput.Formula.Quantified)formula).getQuantifier()));
      result.append(" (");
      result.append("?" + transformVariable(((SimpleTptpParserOutput.Formula.Quantified)formula).getVariable()));
      while (((SimpleTptpParserOutput.Formula.Quantified)formula).getKind() == SimpleTptpParserOutput.Formula.Kind.Quantified &&
             ((SimpleTptpParserOutput.Formula.Quantified)formula).getMatrix().getKind() == SimpleTptpParserOutput.Formula.Kind.Quantified &&
             ((SimpleTptpParserOutput.Formula.Quantified)formula).getQuantifier() == 
               ((SimpleTptpParserOutput.Formula.Quantified)((SimpleTptpParserOutput.Formula.Quantified)formula).getMatrix()).getQuantifier()) {
        formula = ((SimpleTptpParserOutput.Formula.Quantified)formula).getMatrix();
        result.append(" ?" + transformVariable(((SimpleTptpParserOutput.Formula.Quantified)formula).getVariable()));
      }
      result.append(") ");
      if (((SimpleTptpParserOutput.Formula.Quantified)formula).getMatrix().getKind() == SimpleTptpParserOutput.Formula.Kind.Negation ||
          ((SimpleTptpParserOutput.Formula.Quantified)formula).getMatrix().getKind() == SimpleTptpParserOutput.Formula.Kind.Atomic) {
        result.append(convertFormula(((SimpleTptpParserOutput.Formula.Quantified)formula).getMatrix(),indent,indent));
      } else {
        result.append("\n");
        result.append(convertFormula(((SimpleTptpParserOutput.Formula.Quantified)formula).getMatrix(),indent + 4,0));
      }
      result.append(")");
      break;
    default:
      result.append("Error: TPTP Formula syntax unkown for converting");
      break;
    }
    return result;
  }

  private static StringBuffer convertClause (SimpleTptpParserOutput.Clause clause, int indent, int indented) {    
    StringBuffer result = new StringBuffer();
    LinkedList<SimpleTptpParserOutput.Literal> literals = (LinkedList)clause.getLiterals();
    result.append(addIndent(indent,indented));
    if (literals == null) {
      result.append("false\n");
      return result;
    }   
    assert !literals.isEmpty();
    if (literals.size() == 1) {
      result.append(convertLiteral(literals.get(0),indent,indent));
    } else {
      result.append("(");
      result.append(convertConnective(SimpleTptpParserOutput.BinaryConnective.Or));
      result.append(" ");
      result.append(convertLiteral(literals.get(0),indent,indent));
      for (int i = 1; i < literals.size(); i++) {
        result.append("\n");
        result.append(convertLiteral(literals.get(i),indent+4,0));
      }
      result.append(")");
    }
    return result;
  }

  private static StringBuffer convertLiteral (SimpleTptpParserOutput.Literal literal, int indent, int indented) {
    StringBuffer result = new StringBuffer();
    result.append(addIndent(indent,indented));
    if (literal.isPositive()) {
      result.append(convertTerm((SimpleTptpParserOutput.Formula.Atomic)literal.getAtom()));
    } else {
      result.append("(not ");
      result.append(convertTerm((SimpleTptpParserOutput.Formula.Atomic)literal.getAtom()));
      result.append(")");
    }
    return result;
  }
}
