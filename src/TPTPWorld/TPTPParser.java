package TPTPWorld;

import java.util.*;
import java.io.*;
import tptp_parser.*;

public class TPTPParser {

  public Hashtable<String,TPTPFormula> ftable;
  public Vector<SimpleTptpParserOutput.TopLevelItem> Items;

  public TPTPParser (Hashtable<String,TPTPFormula> ftable, 
                     Vector<SimpleTptpParserOutput.TopLevelItem> Items) {
    this.ftable = ftable;
    this.Items = Items;
  }

  public static void checkArguments (String args[]) {
    // has to have at least one argument (for filename or stdin)
    if (args.length < 1) {
      System.out.println("%ERROR: Please supply filename or -- for stdin");
      System.exit(0);
    }
  }

  public static BufferedReader createReader (String arg) throws Exception {
    BufferedReader reader;
    if (arg.equals("--")) {
      // read from stdin
      InputStreamReader sr = new InputStreamReader(System.in);
      reader = new BufferedReader(sr);
    } else {
      // read from file
      FileReader fr = new FileReader(arg);
      reader = new BufferedReader(fr);
    }
    return reader;
  }

  public static TPTPParser parse (BufferedReader reader) throws Exception {
    StringBuffer result = new StringBuffer();
    TptpLexer lexer = new TptpLexer(reader);
    TptpParser parser = new TptpParser(lexer);
    SimpleTptpParserOutput outputManager = new SimpleTptpParserOutput();
    Hashtable<String,TPTPFormula> ftable = new Hashtable();
    Vector<SimpleTptpParserOutput.TopLevelItem> Items = new Vector();

    int i = 0;
    for (SimpleTptpParserOutput.TopLevelItem item = 
           (SimpleTptpParserOutput.TopLevelItem)parser.topLevelItem(outputManager);
         item != null;
         item = (SimpleTptpParserOutput.TopLevelItem)parser.topLevelItem(outputManager)) {
      TPTPFormula formula = new TPTPFormula(item, i);
      String name = getName(formula.item);
      ftable.put(name, formula);
      Items.add(item);
      i++;
    }

    // add parents to tptp formula info
    Set<String> set = ftable.keySet();
    Iterator<String> itr = set.iterator();
    Vector<String> parents = new Vector();
    while (itr.hasNext()) {
      String str = itr.next();
      TPTPFormula formula = ftable.get(str);
      SimpleTptpParserOutput.Source source = formula.source;
      if (source != null) {
        if (source.getKind() == SimpleTptpParserOutput.Source.Kind.Inference) {
          gatherParents(source, parents);
          for (String parent : parents) {
            formula.addParent((TPTPFormula)ftable.get(parent));
          }
        } else {
          String sourceInfo = source.toString();
          if (!sourceInfo.contains("(") && !sourceInfo.contains(")")) {
            formula.addParent((TPTPFormula)ftable.get(sourceInfo));
          }       
        }
      }
      parents.clear();
    }
    return new TPTPParser(ftable, Items);
  }

  public static void gatherParents (SimpleTptpParserOutput.Source source, Vector<String> parents) {
    for (SimpleTptpParserOutput.ParentInfo p : ((SimpleTptpParserOutput.Source.Inference)source).getParentInfoList()) {
      SimpleTptpParserOutput.Source psource = p.getSource();
      if (psource.getKind() == SimpleTptpParserOutput.Source.Kind.Inference) {
        gatherParents(psource, parents);
      } else if (!(p.toString()).contains("(") && !(p.toString()).contains(")")){
        parents.add(p.toString());
      }
    }
  }

  public static String getType (SimpleTptpParserOutput.TopLevelItem item) {
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

  public static String getName (SimpleTptpParserOutput.TopLevelItem item) {
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
  
  public static ArrayList<String> identifyTermVariables (SimpleTptpParserOutput.Formula.Atomic atom,
                                                          ArrayList<String> variables) {
    // if arguments.size() > 1, then definetly is not a variable
    for (int j = 0; j < atom.getNumberOfArguments(); j++) {
      SimpleTptpParserOutput.Term term = (SimpleTptpParserOutput.Term) ((LinkedList)atom.getArguments()).get(j);
      if (term.getTopSymbol().isVariable()) {          
        String variable = term.getTopSymbol().toString();
        boolean unique = true;
        for (String oldvariable : variables) {
          if (oldvariable.equals(variable)) {
            unique = false;
          }
        }
        if (unique) {
          variables.add(variable);
        }
      }
    }
    return variables;
  }

  // given an fof, identify all quantified variables in order from start of
  public static ArrayList<String> identifyQuantifiedVariables (SimpleTptpParserOutput.Formula formula,
                                                             ArrayList<String> variables) {
    switch(formula.getKind()) {
    case Atomic:
      // no more quantified variables
      break;
    case Negation:
      variables = identifyQuantifiedVariables(((SimpleTptpParserOutput.Formula.Negation)formula).getArgument(), variables);
      break;
    case Binary:
      variables = identifyQuantifiedVariables(((SimpleTptpParserOutput.Formula.Binary)formula).getLhs(), variables);
      variables = identifyQuantifiedVariables(((SimpleTptpParserOutput.Formula.Binary)formula).getRhs(), variables);
      break;
    case Quantified:
      variables.add(((SimpleTptpParserOutput.Formula.Quantified)formula).getVariable());
      variables = identifyQuantifiedVariables(((SimpleTptpParserOutput.Formula.Quantified)formula).getMatrix(), variables);
      break;
    default:
      break;
    }
    return variables;    
  }

  public static ArrayList<String> identifyFormulaVariables (SimpleTptpParserOutput.Formula formula,
                                                             ArrayList<String> variables) {
    switch(formula.getKind()) {
    case Atomic:
      variables = identifyTermVariables((SimpleTptpParserOutput.Formula.Atomic)formula, variables);
      break;
    case Negation:
      variables = identifyFormulaVariables(((SimpleTptpParserOutput.Formula.Negation)formula).getArgument(), variables);
      break;
    case Binary:
      variables = identifyFormulaVariables(((SimpleTptpParserOutput.Formula.Binary)formula).getLhs(), variables);
      variables = identifyFormulaVariables(((SimpleTptpParserOutput.Formula.Binary)formula).getRhs(), variables);
      break;
    case Quantified:
      variables = identifyFormulaVariables(((SimpleTptpParserOutput.Formula.Quantified)formula).getMatrix(), variables);
      break;
    default:
      break;
    }
    return variables;
  }
  
  public static ArrayList<String> identifyClauseVariables (SimpleTptpParserOutput.Clause clause,
                                                            ArrayList<String> variables) {
    LinkedList<SimpleTptpParserOutput.Literal> literals = (LinkedList)clause.getLiterals();
    if (literals == null) {
      return variables;
    }
    for (int i = 0; i < literals.size(); i++) {        
      SimpleTptpParserOutput.Literal literal = literals.get(i);
      variables = identifyTermVariables((SimpleTptpParserOutput.Formula.Atomic)literal.getAtom(), variables);
    }
    return variables;
  }
  
  // identify variables in the formula, store as ArrayList of Strings
  public static ArrayList<String> identifyVariables (TPTPFormula formula) {
    ArrayList<String> variables = new ArrayList();
    SimpleTptpParserOutput.TopLevelItem item = formula.item;
    if (item.getKind() == SimpleTptpParserOutput.TopLevelItem.Kind.Formula) {
      SimpleTptpParserOutput.AnnotatedFormula AF = ((SimpleTptpParserOutput.AnnotatedFormula)item);
      variables = identifyFormulaVariables(AF.getFormula(), variables);
    } else if (item.getKind() == SimpleTptpParserOutput.TopLevelItem.Kind.Clause) {
      SimpleTptpParserOutput.AnnotatedClause AC = ((SimpleTptpParserOutput.AnnotatedClause)item);
      variables = identifyClauseVariables(AC.getClause(), variables);
    } 
    return variables;     
  }

}
