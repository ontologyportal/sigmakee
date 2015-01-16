package com.articulate.sigma.semRewrite;

import java.util.*;

import com.articulate.sigma.*;

public class Interpreter {

    // Canonicalize rules into CNF then unify.

  public RuleSet rs = null;
  public CNF input = null;
  
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
      
      System.out.println("INFO in Interpreter.addWSD(): " + clauses);
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
      System.out.println("INFO in Interpreter.addWSD(): " + results);
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
  public void interpret(ArrayList<CNF> inputs) {
      
      // TODO: when we have multiple inputs, or start generating
      // intermediate inputs, loop through them
      ArrayList<CNF> newinputs = new ArrayList<CNF>();
      boolean bindingFound = true;
      int counter = 0;
      while (bindingFound && counter < 10000 && inputs != null && inputs.size() > 0) {
          counter++;
          bindingFound = false;
          for (int j = 0; j < inputs.size(); j++) {
              CNF input = inputs.get(j);
              System.out.println("INFO in Interpreter.interpret(): checking assertion: " + input);
              for (int i = 0; i < rs.rules.size(); i++) {
                  Rule r = rs.rules.get(i);
                  System.out.println("INFO in Interpreter.interpret(): trying rule: " + r);
                  HashMap<String,String> bindings = r.cnf.unify(input);
                  System.out.println("INFO in Interpreter.interpret(): bindings: " + bindings);
                  if (bindings != null) {
                      bindingFound = true;
                      RHS rhs = r.rhs.applyBindings(bindings);
                      System.out.println("INFO in Interpreter.interpret(): rhs: " + rhs);  
                      if (rhs.cnf != null)
                          newinputs.add(rhs.cnf);
                      if (r.operator == Rule.RuleOp.IMP) {
                          input.removeBound();  
                      }
                      else
                          input.clearBound();
                      if (!input.empty())
                          newinputs.add(input);
                  }
              }
          }
          inputs = newinputs;
      }
      System.out.println("INFO in Interpreter.interpret(): result: " + inputs);
  }
  
  /** ***************************************************************
   */
  public static void main(String[] args) {  

      if (args[0] != null && args[0].equals("-i")) {
          KBmanager.getMgr().initializeOnce();
          String input = args[1];
          String filename = "/home/apease/IPsoft/SemRewrite.txt";
          try {
              input = StringUtil.removeEnclosingQuotes(input);
              RuleSet rs = RuleSet.readFile(filename);
              Interpreter interp = new Interpreter(rs);
              ArrayList<String> results = DependencyConverter.getDependencies(input);
              //System.out.println("INFO in Interpreter.main(): deps " + results);
              ArrayList<String> wsd = findWSD(results);
              //System.out.println("INFO in Interpreter.main(): wsd " + wsd);
              results.addAll(wsd);
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
      else {
          String input = "sense(212345678,hired3), det(bank-2, The-1), nsubj(hired-3, bank-2), root(ROOT-0, hired-3), dobj(hired-3, John-4).";
          Lexer lex = new Lexer(input);
          CNF cnfInput = CNF.parseSimple(lex);
          
          System.out.println("INFO in Interpreter.main(): " + cnfInput);
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
          System.out.println("INFO in Interpreter.main(): CNF: " + cnf);
          HashMap<String,String> bindings = cnf.unify(cnfInput);
          System.out.println(bindings);  
          System.out.println(r.rhs.applyBindings(bindings));
      }
  }
}
