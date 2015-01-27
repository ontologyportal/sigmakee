package com.articulate.sigma.semRewrite;

import java.util.*;
import java.io.*;
import com.articulate.sigma.*;

public class Interpreter {

    // Canonicalize rules into CNF then unify.

  public RuleSet rs = null;
  //public CNF input = null;
  
  /** *************************************************************
   */
  public Interpreter (RuleSet rsin) {
      
      canon(rsin);
      rs = rsin;
  }

  /** *************************************************************
   */
  public static RuleSet canon(RuleSet rsin) {
      
      return Clausifier.clausify(rsin);
  }
  
  /** *************************************************************
   */
  public static ArrayList<String> findWSD(ArrayList<String> clauses) {
      
      //System.out.println("INFO in Interpreter.addWSD(): " + clauses);
      DependencyConverter.readFirstNames();
      ArrayList<String> results = new ArrayList<String>();
      HashSet<String> words = new HashSet<String>();
      HashMap<String,String> purewords = new HashMap<String,String>();
      for (int i = 0; i < clauses.size(); i++) {
          String clause = clauses.get(i);
          int paren = clause.indexOf('(');
          int comma = clause.indexOf(',');
          
          String arg1 = clause.substring(paren+1,comma).trim();
          int wordend1 = arg1.indexOf('-');
          String purearg1 = arg1.substring(0,wordend1);
          words.add(arg1);
          if (!purearg1.equals("ROOT"))
              purewords.put(purearg1,arg1);
          
          String arg2 = clause.substring(comma + 1, clause.length()-1).trim();
          int wordend2 = arg2.indexOf('-');
          String purearg2 = arg2.substring(0,wordend2);
          words.add(arg2);
          if (!purearg2.equals("ROOT"))
              purewords.put(purearg2,arg2);
      }
      ArrayList<String> pure = new ArrayList<String>();
      pure.addAll(purewords.keySet());
      for (int i = 0; i < pure.size(); i++) {
          String pureword = pure.get(i);
          if (WordNet.wn.stopwords.contains(pureword))
              continue;
          String id = WSD.findWordSenseInContext(pureword, pure);
          if (!StringUtil.emptyString(id)) {
              String sumo = WordNetUtilities.getBareSUMOTerm(WordNet.wn.getSUMOMapping(id));
              if (!StringUtil.emptyString(sumo)) {
                  if (sumo.indexOf(" ") > -1) {  // TODO: if multiple mappings...
                      sumo = sumo.substring(0,sumo.indexOf(" ")-1);
                  }
                  results.add("sumo(" + sumo + "," + purewords.get(pureword) + ")");
              }
          }
          else if (DependencyConverter.maleNames.contains(pureword)) {
              results.add("sumo(Human," + purewords.get(pureword) + ")");
          }
          else if (DependencyConverter.femaleNames.contains(pureword)) {
              results.add("sumo(Human," + purewords.get(pureword) + ")");                 
          }                            
          else
              results.add("sumo(Entity," + purewords.get(pureword) + ")");    
      }
      //System.out.println("INFO in Interpreter.addWSD(): " + results);
      //results.addAll(clauses);
      return results;
  }

  /** *************************************************************
   */
  public void interpret(String input) {
      
      Lexer lex = new Lexer(input);
      CNF cnf = CNF.parseSimple(lex);
      ArrayList<CNF> inputs = new ArrayList<CNF>();
      inputs.add(cnf);
      interpret(inputs);
  }
  
  /** *************************************************************
   */
  public String printKB(ArrayList<CNF> inputs) {
      
      StringBuilder sb = new StringBuilder();
      sb.append("\n------------------------------\n");
      for (int i = 0; i < inputs.size(); i++)
          sb.append(inputs.get(i).toString() + ".\n");
      sb.append("------------------------------\n");
      return sb.toString();
  }
  
  /** *************************************************************
   */
  public void interpret(ArrayList<CNF> inputs) {
      
      ArrayList<String> kifoutput = new ArrayList<String>();
      System.out.println("INFO in Interpreter.interpret(): inputs: " + inputs); 
      boolean bindingFound = true;
      int counter = 0;
      while (bindingFound && counter < 10 && inputs != null && inputs.size() > 0) {
          counter++;
          bindingFound = false;
          ArrayList<CNF> newinputs = new ArrayList<CNF>();
          CNF newInput = null;
          for (int j = 0; j < inputs.size(); j++) {          
              newInput = inputs.get(j).deepCopy();
              for (int i = 0; i < rs.rules.size(); i++) {
                  Rule r = rs.rules.get(i).deepCopy();
                  //System.out.println("INFO in Interpreter.interpret(): r: " + r);
                  HashMap<String,String> bindings = r.cnf.unify(newInput);
                  if (bindings != null) {
                      bindingFound = true;
                      //System.out.println("INFO in Interpreter.interpret(): new input: " + newInput);
                      //System.out.println("INFO in Interpreter.interpret(): bindings: " + bindings);
                      RHS rhs = r.rhs.applyBindings(bindings);   
                      if (r.operator == Rule.RuleOp.IMP) {
                          CNF bindingsRemoved = newInput.removeBound(); // delete the bound clauses
                          //System.out.println("INFO in Interpreter.interpret(): bindings removed: " + bindingsRemoved);
                          if (!bindingsRemoved.empty()) {  // assert the input after removing bindings
                              if (rhs.cnf != null)
                                  bindingsRemoved.merge(rhs.cnf);
                              newInput = bindingsRemoved;
                          }
                          else
                              if (rhs.cnf != null)
                                  newInput = rhs.cnf;
                          if (rhs.form != null && !kifoutput.contains(rhs.form.toString())) { // assert a KIF RHS
                              kifoutput.add(rhs.form.toString());
                          }
                          //System.out.println("INFO in Interpreter.interpret(): new input: " + newInput);
                      }
                      else if (r.operator == Rule.RuleOp.OPT) {
                          CNF bindingsRemoved = newInput.removeBound(); // delete the bound clauses
                          if (!bindingsRemoved.empty() && !newinputs.contains(bindingsRemoved)) {  // assert the input after removing bindings
                              if (rhs.cnf != null)
                                  bindingsRemoved.merge(rhs.cnf);
                              newinputs.add(bindingsRemoved);
                          }
                          if (rhs.form != null && !kifoutput.contains(rhs.form.toString())) { // assert a KIF RHS
                              kifoutput.add(rhs.form.toString());
                          }
                      }
                      else                                                                         // empty RHS
                          newInput.clearBound();                      
                  }
              }
          }
          if (bindingFound)
              newinputs.add(newInput);
          inputs = new ArrayList<CNF>();
          inputs.addAll(newinputs);
          System.out.println("INFO in Interpreter.interpret(): KB: " + printKB(inputs));
          //System.out.println("INFO in Interpreter.interpret(): bindingFound: " + bindingFound);
          //System.out.println("INFO in Interpreter.interpret(): counter: " + counter);
          //System.out.println("INFO in Interpreter.interpret(): newinputs: " + newinputs);
          //System.out.println("INFO in Interpreter.interpret(): inputs: " + inputs);
      }
      System.out.println("INFO in Interpreter.interpret(): KIF: " + kifoutput);
  }
  
  /** ***************************************************************
   */
  public static void testUnify() {
      
      String input = "sense(212345678,hired-3), det(bank-2, The-1), nsubj(hired-3, bank-2), root(ROOT-0, hired-3), dobj(hired-3, John-4).";
      Lexer lex = new Lexer(input);
      CNF cnfInput = CNF.parseSimple(lex);
      
      String rule = "sense(212345678,?E) , nsubj(?E,?X) , dobj(?E,?Y) ==> " +
              "{(and " +
                  "(instance ?X Organization) " +
                  "(instance ?Y Human)" +
                  "(instance ?E Hiring)" +
                  "(agent ?E ?X) " +
                  "(patient ?E ?Y))}.";
      Rule r = new Rule();
      r = Rule.parseString(rule);
      CNF cnf = Clausifier.clausify(r.lhs);
      System.out.println("INFO in Interpreter.testUnify(): Input: " + cnfInput);
      System.out.println("INFO in Interpreter.testUnify(): CNF rule antecedent: " + cnf);
      HashMap<String,String> bindings = cnf.unify(cnfInput);
      System.out.println("bindings: " + bindings);  
      System.out.println("result: " + r.rhs.applyBindings(bindings));
  }
  
  /** ***************************************************************
   */
  public static void testInterpret() {
      
      //KBmanager.getMgr().initializeOnce();
      String filename = "/home/apease/IPsoft/SemRewrite2.txt";
      try {
          RuleSet rs = RuleSet.readFile(filename);
          Interpreter interp = new Interpreter(rs);
          ArrayList<String> results = new ArrayList<String>();
          results.add("nsubj(arrives-2,Mary-1), root(ROOT-0,arrives-2), prep_at(arrives-2,school-4), sumo(Human,Mary-1).");
          //results.add("amod(John-1,Bob-2)");
          //System.out.println("INFO in Interpreter.main(): deps " + results);
          //ArrayList<String> wsd = findWSD(results);
          //System.out.println("INFO in Interpreter.main(): wsd " + wsd);
          //results.addAll(wsd);
          //System.out.println("INFO in Interpreter.main(): combined " + results);             
          String in = StringUtil.removeEnclosingCharPair(results.toString(),Integer.MAX_VALUE,'[',']');
          //System.out.println("INFO in Interpreter.main(): " + in);
          interp.interpret(in);              
      }
      catch (Exception e) {
          e.printStackTrace();
          System.out.println(e.getMessage());
      }
  }
  
  /** ***************************************************************
   */
  public void interpSingle(String input) {
      
      ArrayList<String> results = null;
      try {
          input = StringUtil.removeEnclosingQuotes(input);
          try {
              results = DependencyConverter.getDependencies(input);
          }
          catch (Exception e) {
              e.printStackTrace();
              System.out.println(e.getMessage());
          }
          ArrayList<String> wsd = findWSD(results);
          results.addAll(wsd);            
          String in = StringUtil.removeEnclosingCharPair(results.toString(),Integer.MAX_VALUE,'[',']');
          interpret(in);              
      }
      catch (Exception e) {
          e.printStackTrace();
          System.out.println(e.getMessage());
      }
  }
  
  /** ***************************************************************
   */
  public void interpInter() {
      
      String input = "";
      ArrayList<String> results = null;
      do {
          Console c = System.console();
          if (c == null) {
              System.err.println("No console.");
              System.exit(1);
          }
          input = c.readLine("Enter sentence: ");
          if (!StringUtil.emptyString(input)) {
              try {
                  results = DependencyConverter.getDependencies(input);
              }
              catch (Exception e) {
                  e.printStackTrace();
                  System.out.println(e.getMessage());
              }
              ArrayList<String> wsd = findWSD(results);
              results.addAll(wsd);           
              String in = StringUtil.removeEnclosingCharPair(results.toString(),Integer.MAX_VALUE,'[',']');
              interpret(in); 
          }
      } while (!StringUtil.emptyString(input));
  }
  
  /** ***************************************************************
   */
  public static void main(String[] args) {  

      Interpreter interp = null;
      if (args != null && args.length > 0 && (args[0].equals("-s") || args[0].equals("-i"))) {
          KBmanager.getMgr().initializeOnce();
          String filename = "/home/apease/SourceForge/KBs/WordNetMappings/SemRewrite.txt";
          try {
              RuleSet rs = RuleSet.readFile(filename);
              interp = new Interpreter(rs);
          }
          catch (Exception e) {
              e.printStackTrace();
              System.out.println(e.getMessage());
          }
      }
      if (args[0].equals("-s")) {
          interp.interpSingle(args[1]);
      }
      else if (args[0].equals("-i")) {
          interp.interpInter();
      }
      else {
          //testUnify();
          testInterpret();
      }
  }
}
