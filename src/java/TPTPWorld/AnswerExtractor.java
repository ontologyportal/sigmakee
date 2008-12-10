
package TPTPWorld;

import java.util.*;
import java.io.*;
import tptp_parser.*;

public class AnswerExtractor {    
  
  // find the unique conjecture/negated_conjecture leaf
  public static TPTPFormula extractVine (Hashtable<String,TPTPFormula> ftable) {
    //      System.out.println("extract vine");
    Set<String> set = ftable.keySet();
    Iterator<String> itr = set.iterator();
    TPTPFormula formula;
    while (itr.hasNext()) {
      //      System.out.println("-------------");
      String str = itr.next();
      formula = ftable.get(str);
      String type = formula.type; 
      // if not conjecture, skip (only dealing with fof conjectures)
      String name = TPTPParser.getName(formula.item);
      //      System.out.println("type: " + type);
      if (type.equals("conjecture") && formula.parent.isEmpty()) {     
        //        System.out.println("found it, returning conjecture");
        return formula;
      }
    }
    return null;
  }
    
  // given a single formula, extract all variable bindings from formula source
  private static ArrayList<Binding> extractBinding (TPTPFormula formula) {



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


    Iterable<SimpleTptpParserOutput.ParentInfo> parents = ((SimpleTptpParserOutput.Source.Inference)source).getParentInfoList();
    // System.out.println("#######ParentInfo#######");
    // System.out.println(source.toString());
    SimpleTptpParserOutput.GeneralTerm details;
    if (!(parents == null)) for (SimpleTptpParserOutput.ParentInfo parent : parents) {
	try {
	    // System.out.println(parent.getParentDetails().toString());
            details = parent.getParentDetails();
            if (details.isList()) {
		for (SimpleTptpParserOutput.GeneralTerm detail : details.getListElements()) {
		    // System.out.println(detail.toString());
                    //if (detail.isFunction()) System.out.println(detail.getFunction());
		    if (detail.isFunction() && (detail.getFunction().equals("bind"))) {
			//System.out.println("Bind!");
			Iterator<SimpleTptpParserOutput.GeneralTerm> itr = detail.getArguments().iterator();
			String variable = (itr.next()).toString();
			String binding  = (itr.next()).getTerm().toString();
      //      System.out.println("variable: " + variable);
      //      System.out.println("binding: " + binding);
			bind.add(new Binding(variable, binding));
		    } // else System.out.println("No bind!");
		}
	    }
	} catch (Exception e) {}
    }
    // System.out.println("#######End ParentInfo#######");


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
      String variable = (itr.next()).toString();
      String binding  = (itr.next()).getTerm().toString();
      //      System.out.println("variable: " + variable);
      //      System.out.println("binding: " + binding);
      bind.add(new Binding(variable, binding));
    }
    return bind;
  }
  
  // recursive method: extract variables from current,
  // compare to unsolved list, remove from list each time a binding is solved
  private static ArrayList<Binding> extractBinding (TPTPFormula formula, 
                                                   ArrayList<Binding> unsolvedBindings) {
    // for each variable substitute in the formula, check against bindings
    // each new binding has to exist in the array list
    if (unsolvedBindings.isEmpty()) {
      return unsolvedBindings;
    }

    // System.out.println("##### Extracting: "+formula.toString());

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
  
  public static ArrayList<Binding> extractAnswers (Hashtable<String,TPTPFormula> ftable) {
    ArrayList<Binding> binds = new ArrayList();
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

    // try { System.out.println("##### Conjecture: "+conjecture.toString());} catch (Exception e) {}

    if (conjecture == null) {      
      System.out.println("% ERROR: No fof conjecture in proof -> extract answers failed");
      return binds;
    }

    // identify variables
    // if any literal starts with a capital letter, it is therefore a variable
    ArrayList<Binding> unsolvedBindings = new ArrayList();
    ArrayList<String> variables = TPTPParser.identifyVariables(conjecture);
    for (String variable : variables) {
      unsolvedBindings.add(new Binding(variable));
    }

    assert unsolvedBindings != null;
    
    if (unsolvedBindings.isEmpty()) {
      return binds;
    }

    for (Binding bind : unsolvedBindings) {
      String variable = bind.variable;
    }
    
    ArrayList<Binding> finalBindings = new ArrayList();
    finalBindings.addAll(unsolvedBindings);
    
    assert finalBindings != null;
    
    // from conjecture to false clause: identify bindings
    // stop when all variables are binded
    assert conjecture != null;
    for (TPTPFormula child : conjecture.child) {
      
	// try {System.out.println("##### Checking child: " + child.toString());} catch (Exception e) {}
       
      unsolvedBindings = extractBinding(child, unsolvedBindings);
    }
    
    if (!unsolvedBindings.isEmpty()) {
      System.out.print("% ERROR: Failed to bind: ");
      for (Binding bind : unsolvedBindings) {
        System.out.print(" " + bind.variable);
      }
      System.out.println("");
    }
        
    for (Binding bind : finalBindings) {
      assert bind != null;
      String variable = bind.variable;
      String answer = bind.getFinalBinding();
      if (variable != null && answer != null) {
        binds.add(new Binding(variable, answer));
      }
    }

    return binds;
  }

  public static boolean extractAnswers (TPTPParser parser) {
    ArrayList<Binding> binds = AnswerExtractor.extractAnswers(parser.ftable);
    TPTPFormula conjecture = AnswerExtractor.extractVine(parser.ftable);    
    return AnswerExtractor.printSZS(conjecture, binds);
  }

  public static boolean printSZS (TPTPFormula conjecture, ArrayList<Binding> binds) {
    // no conjecture = no answers
    if (conjecture == null) {    
      return false;
    }    

    // only dealing with fof conjectures
    assert conjecture.item.getKind() == SimpleTptpParserOutput.TopLevelItem.Kind.Formula;
    SimpleTptpParserOutput.AnnotatedFormula AF = ((SimpleTptpParserOutput.AnnotatedFormula)conjecture.item);
    ArrayList<String> variables = new ArrayList();    
    variables = TPTPParser.identifyQuantifiedVariables(AF.getFormula(), variables);

    assert variables != null;
    assert binds != null;
    if (!binds.isEmpty() && !variables.isEmpty()) {
      String res = "";
      // O(n^2) for-loop, variables list is ordered, output in ordered fashion
      res += "% SZS answers short ";      
      res += "[[" + Binding.getBinding(variables.get(0), binds);
      for (int i = 1; i < variables.size(); i++) {
        res += "," + Binding.getBinding(variables.get(i), binds);
      }
      res += "]]";
      System.out.println(res);      
      return true;
    } else {
      return false;
    }
  }

  // given a proof, extract answers (if any exist)
  public static void main (String[] args) throws Exception {
    TPTPParser.checkArguments(args);
    // assumption: filename is args[0] or "--" for stdin
    BufferedReader reader = TPTPParser.createReader(args[0]);

    // call AnswerExtractor
    TPTPParser parser = TPTPParser.parse(reader);
    if (!AnswerExtractor.extractAnswers(parser)) {
      System.out.println("% No answers found in AnswerExtractor");
    }

    ArrayList<Binding> bindings = AnswerExtractor.extractAnswers(parser.ftable);
    TreeSet<TPTPParser.Symbol> symbols = TPTPParser.getSymbolList(bindings);
    Iterator it = symbols.iterator();
    int count = 0;
    while (it.hasNext()) {
      System.out.println("[" + count + "]: " + it.next());
      count++;
    }

  }
}
