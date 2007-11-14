package com.articulate.sigma;

import java.io.*;
import java.util.*;

public class TPTP2SUMO {

  public static void main (String args[]) {
    String formula = "fof(1,axiom,(    s_holds_2__(s_p,s_a) ),    file('/tmp/SystemOnTPTP11002/Simple2965.tptp',kb_Simple_1)).fof(2,conjecture,(    s_holds_2__(s_p,s_a) ),    file('/tmp/SystemOnTPTP11002/Simple2965.tptp',prove_from_Simple)).fof(3,negated_conjecture,(    ~ s_holds_2__(s_p,s_a) ),    inference(assume_negation,[status(cth)],[2])).fof(4,negated_conjecture,(    ~ s_holds_2__(s_p,s_a) ),    inference(fof_simplification,[status(thm)],[3,theory(equality)])).cnf(5,plain,    ( s_holds_2__(s_p,s_a) ),    inference(split_conjunct,[status(thm)],[1])).cnf(6,negated_conjecture,    ( ~ s_holds_2__(s_p,s_a) ),    inference(split_conjunct,[status(thm)],[4])).cnf(7,negated_conjecture,    ( $false ),    inference(rw,[status(thm)],[6,5,theory(equality)])).cnf(8,negated_conjecture,    ( $false ),    inference(cn,[status(thm)],[7,theory(equality)])).cnf(9,negated_conjecture,    ( $false ),    8,    [proof]).";
    String kif = "";
    try {
      kif = TPTP2SUMO.convert(formula);
    } catch (Exception e) {}
    
    System.out.println("START---");
    System.out.println(kif);
    System.out.println("END-----");
  }

  public static String HelloWorld () {
    //    parser.TptpLexer lexer = null;
    TptpLexer lex;
    return "Hello World";
  }

  public static String convert (String tptp) throws Exception {
    return convert(new BufferedReader(new StringReader(tptp)));
  }

  public static String convert (BufferedReader reader) throws Exception {
    StringBuffer result = new StringBuffer();
    TptpLexer lexer = new TptpLexer(reader);
    TptpParser parser = new TptpParser(lexer);
    SimpleTptpParserOutput outputManager = new SimpleTptpParserOutput();
    Hashtable ftable = new Hashtable();
    Vector<SimpleTptpParserOutput.TopLevelItem> Items = new Vector();

    //----Start SUMO output
    result.append("<queryResponse>\n");
    result.append("  <answer result='yes' number='1'>\n");
    result.append("    <proof>\n");

    for (SimpleTptpParserOutput.TopLevelItem item = 
           (SimpleTptpParserOutput.TopLevelItem)parser.topLevelItem(outputManager);
         item != null;
         item = (SimpleTptpParserOutput.TopLevelItem)parser.topLevelItem(outputManager)) {
      String name = getName(item);
      ftable.put(name, item);
      Items.add(item);
    }

    for (SimpleTptpParserOutput.TopLevelItem item : Items) {
      result.append(convertTPTPFormula(item, ftable));
    }

    //----End SUMO output
    result.append("    </proof>\n");
    result.append("  </answer>\n");
    result.append("  <summary proofs='1'/>\n");
    result.append("</queryResponse>\n");

    return result.toString();
  }

  private static String getName (SimpleTptpParserOutput.TopLevelItem item) {
    if (item.getKind() == SimpleTptpParserOutput.TopLevelItem.Kind.Formula) {
      SimpleTptpParserOutput.AnnotatedFormula AF = ((SimpleTptpParserOutput.AnnotatedFormula)item);
      return AF.getName();
    } else if (item.getKind() == SimpleTptpParserOutput.TopLevelItem.Kind.Clause) {
      SimpleTptpParserOutput.AnnotatedClause AC = ((SimpleTptpParserOutput.AnnotatedClause)item);
      return AC.getName();
    } else {
      return null;
    }
  }

  private static void gatherParents (SimpleTptpParserOutput.Source source, Vector<String> parents) {
    for (SimpleTptpParserOutput.ParentInfo p : ((SimpleTptpParserOutput.Source.Inference)source).getParentInfoList()) {
      SimpleTptpParserOutput.Source psource = p.getSource();
      if (psource._kind == SimpleTptpParserOutput.Source.Kind.Inference) {
        gatherParents(psource, parents);
      } else if (!(p.toString()).contains("(") && !(p.toString()).contains(")")){
        parents.add(p.toString());
      }
    }
  }

  private static StringBuffer convertFormula (String formula, Hashtable ftable, int indent, int indented) {
    StringBuffer result = new StringBuffer();
    result.append("          <premise>\n");
    SimpleTptpParserOutput.TopLevelItem item = (SimpleTptpParserOutput.TopLevelItem) ftable.get(formula);
    if (item != null) {
      if (item.getKind() == SimpleTptpParserOutput.TopLevelItem.Kind.Formula) {
        SimpleTptpParserOutput.AnnotatedFormula AF = ((SimpleTptpParserOutput.AnnotatedFormula)item);
        String type = "formula";
        result.append(addIndent(indent-2,indented));
        result.append("<" + type + " number='" + AF.getName() + "'>\n");
        result.append(convertFormula(AF.getFormula(),indent,indented));
        result.append("\n");
        result.append(addIndent(indent-2,indented));
        result.append("</" + type + ">\n");
      } else if (item.getKind() == SimpleTptpParserOutput.TopLevelItem.Kind.Clause) {
        SimpleTptpParserOutput.AnnotatedClause AC = ((SimpleTptpParserOutput.AnnotatedClause)item);
        String type = "clause";
        result.append(addIndent(indent-2,indented));
        result.append("<" + type + " number='" + AC.getName() + "'>\n");
        result.append(convertClause(AC.getClause(),indent,indented));
        result.append("\n");
        result.append(addIndent(indent-2,indented));
        result.append("</" + type + ">\n");
      } else {
        result.append("Error: TPTP Formula syntax unknown for converting");
      }          
    }
    result.append("          </premise>\n");
    return result;
  }

  private static StringBuffer convertTPTPFormula (SimpleTptpParserOutput.TopLevelItem item, Hashtable ftable) {
    StringBuffer result = new StringBuffer();
    int indent = 12;
    int indented = 0;
    Vector<String> parents = new Vector();
    
    result.append("      <proofStep>\n");
    result.append("        <premises>\n");
    SimpleTptpParserOutput.Annotations annotations = null;    
    SimpleTptpParserOutput.Source source = null;
    String sourceInfo = "";
    //----Add parents info
    if (item.getKind() == SimpleTptpParserOutput.TopLevelItem.Kind.Formula) {
      SimpleTptpParserOutput.AnnotatedFormula AF = ((SimpleTptpParserOutput.AnnotatedFormula)item);
      annotations = AF.getAnnotations();
    } else if (item.getKind() == SimpleTptpParserOutput.TopLevelItem.Kind.Clause) {
      SimpleTptpParserOutput.AnnotatedClause AC = ((SimpleTptpParserOutput.AnnotatedClause)item);
      annotations = AC.getAnnotations();
    } else {
      result.append("Error: TPTP Formula syntax unknown for converting");
     return null;
    }
    if (annotations != null) {
      source = annotations.getSource();
      sourceInfo = source.toString();
      if (source._kind == SimpleTptpParserOutput.Source.Kind.Inference) {
        gatherParents(source,parents);
        for (String parent : parents) {
          result.append(convertFormula(parent, ftable, indent+2, indented));
        }
      } else {
        if (!sourceInfo.contains("(") && !sourceInfo.contains(")")) {
          result.append(convertFormula(sourceInfo, ftable, indent+2, indented));
        }       
      }
    }
    result.append("        </premises>\n");
    result.append("        <conclusion>\n");
    String type = "";
    if (item.getKind() == SimpleTptpParserOutput.TopLevelItem.Kind.Formula) {
      SimpleTptpParserOutput.AnnotatedFormula AF = ((SimpleTptpParserOutput.AnnotatedFormula)item);
      type = "formula";
      result.append("          <" + type + " number='" + AF.getName() + "'>\n");
      result.append(convertFormula(AF.getFormula(),indent,indented));
      result.append("\n");
      result.append("          </" + type + ">\n");
    } else if (item.getKind() == SimpleTptpParserOutput.TopLevelItem.Kind.Clause) {
      SimpleTptpParserOutput.AnnotatedClause AC = ((SimpleTptpParserOutput.AnnotatedClause)item);
      type = "clause";
      result.append("          <" + type + " number='" + AC.getName() + "'>\n");
      result.append(convertClause(AC.getClause(),indent,indented));
      result.append("\n");
      result.append("          </" + type + ">\n");
    } else {
      result.append("Error: TPTP Formula syntax unknown for converting");
    }

    result.append("        </conclusion>\n");
    result.append("      </proofStep>\n");
    return result;
  }

  private static StringBuffer convertConnective(SimpleTptpParserOutput.BinaryConnective connective) {
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

  private static String convertTerm (SimpleTptpParserOutput.Formula.Atomic atom) {
    String res = "";
    LinkedList<SimpleTptpParserOutput.Term> arguments = (LinkedList)atom.getArguments();
    if (arguments != null) {
      res += "(";
    }
    res += removeDollarSign(atom.getPredicate());
    if (arguments != null) {
      for (int n = 0; n < arguments.size();  n++) {
        res += " " + removeDollarSign(arguments.get(n).toString());
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
      result.append("?" + ((SimpleTptpParserOutput.Formula.Quantified)formula).getVariable());
      while (((SimpleTptpParserOutput.Formula.Quantified)formula).getKind() == SimpleTptpParserOutput.Formula.Kind.Quantified &&
             ((SimpleTptpParserOutput.Formula.Quantified)formula).getMatrix().getKind() == SimpleTptpParserOutput.Formula.Kind.Quantified &&
             ((SimpleTptpParserOutput.Formula.Quantified)formula).getQuantifier() == 
               ((SimpleTptpParserOutput.Formula.Quantified)((SimpleTptpParserOutput.Formula.Quantified)formula).getMatrix()).getQuantifier()) {
        formula = ((SimpleTptpParserOutput.Formula.Quantified)formula).getMatrix();
        result.append(" ?" + ((SimpleTptpParserOutput.Formula.Quantified)formula).getVariable());
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
