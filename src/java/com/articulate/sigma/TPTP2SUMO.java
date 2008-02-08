package com.articulate.sigma;

import java.io.*;
import java.util.*;
import tptp_parser.*;

public class TPTP2SUMO {
  public static class TPTPFormula {
    int id;
    SimpleTptpParserOutput.TopLevelItem item;
    SimpleTptpParserOutput.Annotations annotations = null;
    SimpleTptpParserOutput.Source source = null;
    String type = "";
    ArrayList<TPTPFormula> parent;
    ArrayList<TPTPFormula> child;
    TPTPFormula (SimpleTptpParserOutput.TopLevelItem item, int id) {
      this.item = item;
      this.id = id;
      type = getType(item);
      parent = new ArrayList();
      child = new ArrayList();
      
      if (item.getKind() == SimpleTptpParserOutput.TopLevelItem.Kind.Formula) {
        SimpleTptpParserOutput.AnnotatedFormula AF = ((SimpleTptpParserOutput.AnnotatedFormula)item);
        annotations = AF.getAnnotations();
      } else if (item.getKind() == SimpleTptpParserOutput.TopLevelItem.Kind.Clause) {
        SimpleTptpParserOutput.AnnotatedClause AC = ((SimpleTptpParserOutput.AnnotatedClause)item);
        annotations = AC.getAnnotations();
      } 
      if (annotations != null) {
        source = annotations.getSource();
      }
    }
    void addParent (TPTPFormula that) {
      this.parent.add(that);
      that.child.add(this);
    }
  }

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
      kif = TPTP2SUMO.convert(file);
    } catch (Exception e) { 
      System.out.println("e: " + e);
    }

    System.out.println("START---");
    System.out.println(kif);
    System.out.println("END-----");
  }

  public static String convert (String tptp) throws Exception {
    //    System.out.println("convert String");
    return convert(new BufferedReader(new StringReader(tptp)));
  }

  public static String convert (Reader in) throws Exception {
    return convert(new BufferedReader(in));
  }

  public static String convert (BufferedReader reader) throws Exception {
    //    System.out.println("convert BufferedReader");    
    StringBuffer result = new StringBuffer();
    TptpLexer lexer = new TptpLexer(reader);
    TptpParser parser = new TptpParser(lexer);
    SimpleTptpParserOutput outputManager = new SimpleTptpParserOutput();
    Hashtable<String,TPTPFormula> ftable = new Hashtable();
    Vector<SimpleTptpParserOutput.TopLevelItem> Items = new Vector();
    
    //----Start SUMO output
    result.append("<queryResponse>\n");
    result.append("  <answer result='yes' number='1'>\n");

    int i = 0;

    for (SimpleTptpParserOutput.TopLevelItem item = 
           (SimpleTptpParserOutput.TopLevelItem)parser.topLevelItem(outputManager);
         item != null;
         item = (SimpleTptpParserOutput.TopLevelItem)parser.topLevelItem(outputManager)) {
      //      System.out.println("new item");
      String name = getName(item);
      //      System.out.println("new form: " + name);
      System.out.println(item.toString());      
      TPTPFormula formula = new TPTPFormula(item, i);
      i++;
      ftable.put(name, formula);
      Items.add(item);
    }
    //----Start proof output
    StringBuffer proof = new StringBuffer();
    proof.append("    <proof>\n");
    for (SimpleTptpParserOutput.TopLevelItem item : Items) {
      String name = getName(item);
      TPTPFormula fthis = ftable.get(name);
      proof.append(convertTPTPFormula(item, fthis, ftable));
    }
    //----End proof output
    proof.append("    </proof>\n");

    // print out answer (if exists)
    // (CURRENTLY: only metis proof prints variable bindings)
    StringBuffer binding = AnswerExtractor.extractAnswers(ftable);

    //----Append proof after bindings (xml order matters)
    result.append(binding);
    result.append(proof);

    //----End SUMO output
    result.append("  </answer>\n");
    result.append("  <summary proofs='1'/>\n");
    result.append("</queryResponse>\n");

    return result.toString();
  }

  private static String getType (SimpleTptpParserOutput.TopLevelItem item) {
    if (item.getKind() == SimpleTptpParserOutput.TopLevelItem.Kind.Formula) {
      SimpleTptpParserOutput.AnnotatedFormula AF = ((SimpleTptpParserOutput.AnnotatedFormula)item);
      return AF.getRole().toString();
    } else if (item.getKind() == SimpleTptpParserOutput.TopLevelItem.Kind.Clause) {
      SimpleTptpParserOutput.AnnotatedClause AC = ((SimpleTptpParserOutput.AnnotatedClause)item);
      return AC.getRole().toString();
    } else {
      return "";
    }
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
      if (psource.getKind() == SimpleTptpParserOutput.Source.Kind.Inference) {
        gatherParents(psource, parents);
      } else if (!(p.toString()).contains("(") && !(p.toString()).contains(")")){
        parents.add(p.toString());
      }
    }
  }

  private static StringBuffer convertFormula (String formula, Hashtable<String,TPTPFormula> ftable, int indent, int indented) {
    StringBuffer result = new StringBuffer();
    result.append("          <premise>\n");
    int id = (ftable.get(formula)).id;
    SimpleTptpParserOutput.TopLevelItem item = (ftable.get(formula)).item;
    if (item != null) {
      if (item.getKind() == SimpleTptpParserOutput.TopLevelItem.Kind.Formula) {
        SimpleTptpParserOutput.AnnotatedFormula AF = ((SimpleTptpParserOutput.AnnotatedFormula)item);
        String type = "formula";
        result.append(addIndent(indent-2,indented));
        result.append("<" + type + " number='" + id + "'>\n");
        result.append(convertFormula(AF.getFormula(),indent,indented));
        result.append("\n");
        result.append(addIndent(indent-2,indented));
        result.append("</" + type + ">\n");
      } else if (item.getKind() == SimpleTptpParserOutput.TopLevelItem.Kind.Clause) {
        SimpleTptpParserOutput.AnnotatedClause AC = ((SimpleTptpParserOutput.AnnotatedClause)item);
        String type = "clause";
        result.append(addIndent(indent-2,indented));
        result.append("<" + type + " number='" + id + "'>\n");
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

  private static StringBuffer convertTPTPFormula (SimpleTptpParserOutput.TopLevelItem item, TPTPFormula fthis, Hashtable<String,TPTPFormula> ftable) {
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
      if (source.getKind() == SimpleTptpParserOutput.Source.Kind.Inference) {
        gatherParents(source, parents);
        for (String parent : parents) {
          fthis.addParent((TPTPFormula)ftable.get(parent));
          result.append(convertFormula(parent, ftable, indent+2, indented));
        }
      } else {
        if (!sourceInfo.contains("(") && !sourceInfo.contains(")")) {
          fthis.addParent((TPTPFormula)ftable.get(sourceInfo));
          result.append(convertFormula(sourceInfo, ftable, indent+2, indented));
        }       
      }
    }
    result.append("        </premises>\n");
    result.append("        <conclusion>\n");
    String type = "";
    int id = fthis.id;
    if (item.getKind() == SimpleTptpParserOutput.TopLevelItem.Kind.Formula) {
      SimpleTptpParserOutput.AnnotatedFormula AF = ((SimpleTptpParserOutput.AnnotatedFormula)item);
      type = "formula";
      result.append("          <" + type + " number='" + id + "'>\n");
      result.append(convertFormula(AF.getFormula(),indent,indented));
      result.append("\n");
      result.append("          </" + type + ">\n");
    } else if (item.getKind() == SimpleTptpParserOutput.TopLevelItem.Kind.Clause) {
      SimpleTptpParserOutput.AnnotatedClause AC = ((SimpleTptpParserOutput.AnnotatedClause)item);
      type = "clause";
      result.append("          <" + type + " number='" + id + "'>\n");
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

  public static class AnswerExtractor {
    
    static class Binding {
      String variable;
      String binding;
      Binding next;
      Binding (String variable) {
        this.variable = variable;
        this.binding = null;
        this.next = null;
      }
      Binding (String variable, String binding) {
        this.variable = variable;
        this.binding = binding;
        this.next = null;        
      }
      Binding addBinding (String bind) {
        this.binding = bind;
        char c = bind.charAt(0);
        boolean upper = Character.isUpperCase(c);
        if (upper) {
          this.next = new Binding(bind);
          return this.next;
        } 
        return null;
      }
      String getFinalBinding () {
        if (next == null) {
          return binding;
        } 
        return next.getFinalBinding();        
      }
    }

    // find the unique conjecture/negated_conjecture leaf
    private static TPTPFormula extractVine (Hashtable<String,TPTPFormula> ftable) {
      //      System.out.println("extract vine");
      Set<String> set = ftable.keySet();
      Iterator<String> itr = set.iterator();
      while (itr.hasNext()) {
        String str = itr.next();
        TPTPFormula formula = ftable.get(str);
        String type = formula.type; 
        // if not conjecture of negated conjecture, skip
        String name = getName(formula.item);
        //        System.out.println("name: " + name);
        //        System.out.println("type: " + type);
        //        System.out.println(" # children: " + formula.child.size());
        //        System.out.println(" # parent: " + formula.parent.size());
        if (!type.equals("conjecture") && !type.equals("negated_conjecture")) {          
          continue;
        }
        if (formula.parent.isEmpty()) {
          //          System.out.println("found conjecture leaf");
          return formula;
        }
      }
      return null;
    }


    public static ArrayList<Binding> identifyFormulaVariables (SimpleTptpParserOutput.Formula formula,
                                                               ArrayList<Binding> bind) {
      switch(formula.getKind()) {
      case Atomic:
        bind = identifyTermVariables((SimpleTptpParserOutput.Formula.Atomic)formula, bind);
        break;
      case Negation:
        bind = identifyFormulaVariables(((SimpleTptpParserOutput.Formula.Negation)formula).getArgument(), bind);
        break;
      case Binary:
        bind = identifyFormulaVariables(((SimpleTptpParserOutput.Formula.Binary)formula).getLhs(), bind);
        bind = identifyFormulaVariables(((SimpleTptpParserOutput.Formula.Binary)formula).getRhs(), bind);
        break;
      case Quantified:
        bind = identifyFormulaVariables(((SimpleTptpParserOutput.Formula.Quantified)formula).getMatrix(), bind);
        break;
      default:
        break;
      }
      return bind;
    }

    public static ArrayList<Binding> identifyTermVariables (SimpleTptpParserOutput.Formula.Atomic atom,
                                                              ArrayList<Binding> bind) {
      // if arguments.size() > 1, then definetly is not a variable
      for (int j = 0; j < atom.getNumberOfArguments(); j++) {
        SimpleTptpParserOutput.Term term = (SimpleTptpParserOutput.Term) ((LinkedList)atom.getArguments()).get(j);
        //        System.out.println("atom: " + atom.toString());
        //        System.out.println("term: " + term.toString());
        if (term.getTopSymbol().isVariable()) {          
          String variable = transformVariable(term.getTopSymbol().toString());
          boolean unique = true;
          for (Binding oldbind : bind) {
            if (oldbind.variable.equals(variable)) {
              unique = false;
            }
          }
          if (unique) {
            bind.add(new Binding(variable));
          }
        }
      }
      return bind;
    }

    public static ArrayList<Binding> identifyClauseVariables (SimpleTptpParserOutput.Clause clause,
                                                              ArrayList<Binding> bind) {
      LinkedList<SimpleTptpParserOutput.Literal> literals = (LinkedList)clause.getLiterals();
      if (literals == null) {
        return bind;
      }
      for (int i = 0; i < literals.size(); i++) {        
        SimpleTptpParserOutput.Literal literal = literals.get(i);
        bind = identifyTermVariables((SimpleTptpParserOutput.Formula.Atomic)literal.getAtom(), bind);
      }
      return bind;
    }

    // identify variables in the formula, store as Bindings
    public static ArrayList<Binding> identifyVariables (TPTPFormula formula) {
      ArrayList<Binding> bind = new ArrayList();
      SimpleTptpParserOutput.TopLevelItem item = formula.item;
      if (item.getKind() == SimpleTptpParserOutput.TopLevelItem.Kind.Formula) {
        SimpleTptpParserOutput.AnnotatedFormula AF = ((SimpleTptpParserOutput.AnnotatedFormula)item);
        bind = identifyFormulaVariables(AF.getFormula(), bind);
      } else if (item.getKind() == SimpleTptpParserOutput.TopLevelItem.Kind.Clause) {
        SimpleTptpParserOutput.AnnotatedClause AC = ((SimpleTptpParserOutput.AnnotatedClause)item);
        bind = identifyClauseVariables(AC.getClause(), bind);
      } 
      return bind;     
    }

    // given a single formula, extract all variable bindings from formula source
    public static ArrayList<Binding> extractBinding (TPTPFormula formula) {
      //      System.out.println("extract binding from a formula");
      // look at formula, see if there are variable bindings
      ArrayList<Binding> bind = new ArrayList();
      SimpleTptpParserOutput.Source source = formula.source;      
      if (source.getKind() != SimpleTptpParserOutput.Source.Kind.Inference) {
        return bind;
      }            
      String type = ((SimpleTptpParserOutput.Source.Inference)source).getInferenceRule();
      if (type == null) {
        return bind;
      }
      if (!type.equals("subst")) {
        return bind;
      }
      Iterable<SimpleTptpParserOutput.InfoItem> infoList = ((SimpleTptpParserOutput.Source.Inference)source).getUsefulInfo();
      if (infoList == null) {
        return bind;
      }
      for (SimpleTptpParserOutput.InfoItem info : infoList) {
        if (info.getKind() != SimpleTptpParserOutput.InfoItem.Kind.GeneralFunction) {
          continue;
        }
        String infoString = info.toString(4);
        SimpleTptpParserOutput.GeneralTerm term = ((SimpleTptpParserOutput.InfoItem.GeneralFunction)info).getGeneralFunction();
        // List : [ Variable, Term ]
        Iterable<SimpleTptpParserOutput.GeneralTerm> list = term.getListElements();
        Iterator<SimpleTptpParserOutput.GeneralTerm> itr = list.iterator();
        String variable = transformVariable((itr.next()).toString());
        String binding  = transformTerm((itr.next()).getTerm().toString());
        //        System.out.println("variable: " + variable);
        //        System.out.println("binding: " + binding);
        bind.add(new Binding(variable, binding));
      }
      //      System.out.println("done binding from a formula, return");
      return bind;
    }

    // recursive method: extract variables from current,
    // compare to unsolved list, remove from list each time a binding is solved
    public static ArrayList<Binding> extractBinding (TPTPFormula formula, 
                                                     ArrayList<Binding> unsolvedBindings) {
      //      System.out.println("extract binding: recursive method ");
      // for each variable substitute in the formula, check against bindings
      // each new binding has to exist in the array list
      if (unsolvedBindings.isEmpty()) {
        return unsolvedBindings;
      }
      ArrayList<Binding> newBindings = extractBinding(formula);
      ArrayList<Binding> removeBindings = new ArrayList();
      ArrayList<Binding> addBindings = new ArrayList();
      for (Binding bind : newBindings) {
        for (Binding unsolved : unsolvedBindings) {
          // fresh variable
          if (unsolved.variable.equals(bind.variable)) {
            Binding newBind = unsolved.addBinding(bind.binding);
            // remove old
            removeBindings.add(unsolved);
            // add new if applicable
            if (newBind != null) {
              addBindings.add(newBind);
            }
          }
        }
      }
      // remove solved bindings
      for (Binding bind : removeBindings) {
        unsolvedBindings.remove(unsolvedBindings.indexOf(bind));
      }
      // add new unsolved bindings
      for (Binding bind : addBindings) {
        unsolvedBindings.add(bind);
      }
      // if no children, then no more recursion, return
      if (formula.child.isEmpty()) {
        return unsolvedBindings;
      }      
      // recurse on each child, updating the unsolved list each iteration
      for (TPTPFormula child : formula.child) {       
        unsolvedBindings = extractBinding(child, unsolvedBindings);
      }
      // returned any unsolved bindings
      return unsolvedBindings;
    }

    public static StringBuffer extractAnswers (Hashtable<String,TPTPFormula> ftable) {
      StringBuffer binding = new StringBuffer();
      //      System.out.println("extracting answers");
    /*
      Procedure:
      1) Vine Extract (since only conjecture variable bindings are important)
      2) In conjecture identify variables
      3) From conjecture to false clause: identify variable bindings 
      4) Stop when all variables are binded
    */

      
      // vine extraction
      TPTPFormula conjecture = extractVine(ftable);
      if (conjecture == null) {
        System.out.println("conjecture is null!: ");
        return binding;
      }
      //      System.out.println("conjecture: " + conjecture.item.toString());
      
      // identify variables
      // if any literal starts with a capital letter, it is therefore a variable
      ArrayList<Binding> unsolvedBindings = identifyVariables(conjecture);
      assert unsolvedBindings != null;

      if (unsolvedBindings.isEmpty()) {
        System.out.println(" no variables.");
        return binding;
      } else {
        System.out.println("conjecture is null!: ");
        //        binding.append("conjecture has variables!: size: " + unsolvedBindings.size() + ", 1st: " + unsolvedBindings.get(0).variable);
        //        return binding;
      }

      for (Binding bind : unsolvedBindings) {
        String variable = bind.variable;
        System.out.println("new variable: " + variable);
      }
      
      ArrayList<Binding> finalBindings = new ArrayList();
      finalBindings.addAll(unsolvedBindings);
      
      assert finalBindings != null;

      // from conjecture to false clause: identify bindings
      // stop when all variables are binded
      assert conjecture != null;
      for (TPTPFormula child : conjecture.child) {        
        //        System.out.println("working on child: " + getName(child.item));
        unsolvedBindings = extractBinding(child, unsolvedBindings);
      }

      if (!unsolvedBindings.isEmpty()) {
        System.out.println(" failed to bind: ");
      }

      ArrayList<Binding> finalB = new ArrayList();
      
      for (Binding bind : finalBindings) {
        assert bind != null;
        String variable = bind.variable;
        String answer = bind.getFinalBinding();
        if (variable != null && answer != null) {
          finalB.add(bind);
        }
      }
      
      if (!finalB.isEmpty()) {
        binding.append("  <bindingSet type='definite'>\n");
        binding.append("    <binding>\n");
        for (Binding bind : finalB) {
          assert bind.binding != null;
          //          if (bind.binding != null) {
            binding.append("      <var name='?" + bind.variable + "' value='" + bind.binding + "'/>\n");
            //          }
        }
        binding.append("    </binding>\n");
        binding.append("  </bindingSet>\n");
      }
      
      return binding;      

    }
  }
}
