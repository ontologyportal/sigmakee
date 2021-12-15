
package TPTPWorld;

import java.util.*;
import java.io.*;
import tptp_parser.*;
import java.util.regex.*;

public class LooksDifferent {

  // Given a tptp file, return unique set of symbols
  public static TreeSet<TPTPParser.Symbol> getUniqueSymbols (TreeSet symbolsSoFar, TreeSet newSymbols) {
    TreeSet<TPTPParser.Symbol> set = new TreeSet(new TPTPParser.SymbolComparator());
    Iterator<TPTPParser.Symbol> it = newSymbols.iterator();
    while (it.hasNext()) {
      TPTPParser.Symbol symbol = it.next();
      if (!symbolsSoFar.contains(symbol)) {
        set.add(symbol);
      }
    }
    return set;
  }

  public static final String predicate_ld = "dummy_ld";

  public static ArrayList<String> getUniqueAxioms () {
    ArrayList<String> axioms = new ArrayList();
    // only unique ld_axiom at the moment is symmetry
    axioms.add("fof(ld_symm, axiom, ![X,Y]: (" + predicate_ld + "(X,Y) => " + predicate_ld + "(Y,X))).");
    return axioms;
  }

  private static String generateVarList (String var, int arity) {
    String list = "";   
    if (arity <= 0) {
      return list;
    }
    list += var + "1";
    for (int i = 1; i < arity; i++) {
      int id = (i + 1);
      list += "," + var + id;
    }
    return list;
  }

  public static ArrayList<String> addAxioms (TreeSet uniqueSymbols, TreeSet symbolList) {
    ArrayList<String> axioms = new ArrayList();
    Iterator<TPTPParser.Symbol> ita = uniqueSymbols.iterator();
    // symmetry
    String formula = "";
    while (ita.hasNext()) {
			Iterator<TPTPParser.Symbol> itb = symbolList.iterator();
      TPTPParser.Symbol unique = ita.next();
      // ![X1,...,Xn,Y1,...,Yn]: (predicate_ld(X1,Y1) | ... | predicate_ld(Xn,Yn)) => predicate_ld(functor_u(X1,...,Xn), functor_u(Y1,...Yn))
      if (unique.arity > 0) {
        String functor_u = unique.text;
        String V1 = generateVarList("X",unique.arity);
        String V2 = generateVarList("Y",unique.arity);
        String V12 = V1 + "," + V2;
        formula = "";
        formula += "fof(ld_f_" + unique.text;
        formula += ", axiom, ";
        formula += "![" + V12 + "]:(";
        formula += predicate_ld + "(" + V12 + ") =>";
        formula += predicate_ld + "(" + functor_u + "(" + V1 + ")," + functor_u + "(" + V2 + "))";
        formula += ")).";
        axioms.add(formula);
      }
      while (itb.hasNext()) {
        TPTPParser.Symbol symbol = itb.next();
        // do not add ld(a, a), where a is the symbol        
        if (unique.text.equals(symbol.text)) {
          continue;
        }
        if (unique.arity == 0 && symbol.arity == 0) {
          String constant_u = unique.text;
          String constant_s = symbol.text;
          formula = "";
          formula += "fof(ld_c_" + constant_u + "_c_" + constant_s;
          formula += ", axiom, ";
          formula += predicate_ld + "(" + constant_u + ", " + constant_s + ")";
          formula += ").";
          axioms.add(formula);
        } else if (unique.arity == 0 && symbol.arity > 0) {
          // ![X1,...Xn]: " + predicate_ld + "(constant_u, functor_s(X1,...,Xn))
          String constant_u = unique.text;
          String functor_s = symbol.text;
          String V = generateVarList("X",symbol.arity);
          formula = "";
          formula += "fof(ld_c_" + constant_u + "_f_" + functor_s;
          formula += ", axiom, ";
          formula += "![" + V + "]:(";
          formula += predicate_ld + "(" + constant_u + ", ";
          formula += functor_s + "(" + V + ")";
          formula += "))).";
          axioms.add(formula);
        } else if (unique.arity > 0 && symbol.arity == 0) {
          // ![X1,...Xn]: " + predicate_ld + "(functor_u(X1,...,Xn), constant_s)
          String functor_u = unique.text;
          String constant_s = symbol.text;
          String V = generateVarList("X",unique.arity);
          formula = "";
          formula += "fof(ld_f_" + functor_u + "_c_" + constant_s;
          formula += ", axiom, ";
          formula += "![" + V + "]:(";
          formula += predicate_ld + "(" + functor_u + "(" + V + "), ";
          formula += symbol.text + ")";
          formula += ")).";
          axioms.add(formula);
        } else if (unique.arity > 0 && symbol.arity > 0) {
          // ![X1,...Xn,Y1,...,Ym]: predicate_ld(functor_u(X1,...Xn), functor_s(Y1,...,Ym))
          String functor_u = unique.text;
          String functor_s = symbol.text;
          String V1 = generateVarList("X",unique.arity);
          String V2 = generateVarList("Y",symbol.arity);
          String V12 = V1 + "," + V2;
          formula = "";
          formula += "fof(ld_f_" + functor_u + "_f_" + functor_s;
          formula += ", axiom, ";
          formula += "![" + V12 + "]:(";
          formula += predicate_ld + "(" + functor_u + "(" + V1 + "),";
          formula += functor_s + "(" + V2 + "))";
          formula += ")).";
          axioms.add(formula);
        }
      }
    }
    return axioms;
  }

  public static String addToConjecture(String conjecture, ArrayList<Binding> binds) {
    int question_mark = conjecture.indexOf("?");
    int parenthesis = conjecture.indexOf("(");
    while (parenthesis < question_mark) {
      // remove '(' from front, and ')' from end
      conjecture = conjecture.substring(parenthesis+1, conjecture.length());
      parenthesis = conjecture.lastIndexOf(")");
      conjecture = conjecture.substring(0, parenthesis);
      // get new position of first question mark and first parenthesis
      question_mark = conjecture.indexOf("?");
      parenthesis = conjecture.indexOf("(");
    }
    // add open '(' after the first colon in the conjecture
    int colon = conjecture.indexOf(':');
    conjecture = conjecture.substring(0,colon+1) + "       (   " + conjecture.substring(colon+1,conjecture.length());
    conjecture += " & (";
    conjecture += predicate_ld + "(" + binds.get(0).variable + "," + binds.get(0).binding + ")";    
    for (int i = 1; i < binds.size(); i++) {      
      conjecture += "| " + predicate_ld + "(" + binds.get(i).variable + "," + binds.get(i).binding + ")";
    }
    conjecture += "))   ";
    return conjecture;
  }

	// bind the variables of the conjecture
	// assumption quantified variables on outside only
	public static String bindConjecture (String conjecture, ArrayList<Binding> original, ArrayList<Binding> bind) {
		// remove outer most existential (find first ':')
		int colon = conjecture.indexOf(":");
		String formula = conjecture.substring(colon+1, conjecture.length());
		String result = "";
		// for each old Xi in original, replace with new Yi in bind
		// careful not to remove Xi from some part of a string
		// check Xi,  and ,Xi
		if (original.size() != bind.size()) {
			return "different size of variable lists in original and binds, orig: " + original.size() + ", while bind size: " + bind.size();
		}
		String delimiters = "(),. :?![]~";
		StringTokenizer st = new StringTokenizer(formula, delimiters, true);
		while (st.hasMoreTokens()) {
			String token = st.nextToken();
			boolean flag = false;
			String replacement = "";
  		for (int i = 0; i < original.size(); i++) {
	  		String orig = (original.get(i)).variable;
		  	String b = (bind.get(i)).binding;
				if (token.equals(orig)) {
					flag = true;
					replacement = b;
					break;
				} 
			}
			if (flag) {
				result += replacement;
			} else {
				result += token;
			}
		}
		// add front back in
		formula = conjecture.substring(0, colon+1) + result;
		return formula;
	}
	
	public static String filterLooksDifferent (String tptp) throws Exception {

		// keep only TPTPFormula which are "axioms" and has no ld predicates
		String axioms = "";
		TPTPParser parser = TPTPParser.parse(tptp);
		Hashtable<String,TPTPFormula> ftable = parser.ftable;
		Vector<SimpleTptpParserOutput.TopLevelItem> Items = parser.Items;
		for (SimpleTptpParserOutput.TopLevelItem item : Items) {
			String name = TPTPParser.getName(item);
			TPTPFormula formula = ftable.get(name);
			if (!formula.parent.isEmpty()) {
				continue;
			}
			if (((formula.item).toString()).contains(predicate_ld)) {
				continue;
			}
                        if ((formula.getRole()).contains("conjecture")) {
				continue;
			}
			axioms += formula.fofify();
		} 	
		return axioms;
	}
	
  public static void main (String args[]) throws Exception {

    TPTPParser.checkArguments(args);
    // assumption: filename is args[0] or "--" for stdin
    TPTPParser parser = TPTPParser.parse(args[0]);
    // get symbols from proof
    TreeSet<TPTPParser.Symbol> symbolList = TPTPParser.getSymbolList(args[0]);
    Iterator it = symbolList.iterator();
    int count = 0;
    while (it.hasNext()) {
      System.out.println("[" + count + "]: " + it.next());
      count++;
    }

    // get answer bindings from proof
    ArrayList<Binding> lastAnswer = AnswerExtractor.extractAnswers(parser.ftable);
    TreeSet<TPTPParser.Symbol> newSymbols = TPTPParser.getSymbolList(lastAnswer);
    it = newSymbols.iterator();
    count = 0;
    while (it.hasNext()) {
      System.out.println("[" + count + "]: " + it.next());
      count++;
    }


    ArrayList<String> ldAxioms = LooksDifferent.addAxioms(newSymbols, symbolList);
    for (int i = 0; i < ldAxioms.size(); i++) {
      System.out.println("axiom: " + ldAxioms.get(i));
    }
  }

}
