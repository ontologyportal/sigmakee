package com.articulate.sigma.semRewrite;

import java.util.*;

import com.articulate.sigma.DependencyConverter;
import com.articulate.sigma.StringUtil;

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
  public void interpret(String input) {
      
      Lexer lex = new Lexer(input);
      CNF cnf = CNF.parseSimple(lex);
      interpret(cnf);
  }
  
  /** *************************************************************
   */
  public void interpret(CNF input) {
      
      // TODO: when we have multiple inputs, or start generating
      // intermediate inputs, loop through them
      for (int i = 0; i < rs.rules.size(); i++) {
          Rule r = rs.rules.get(i);
          HashMap<String,String> bindings = r.cnf.unify(input);
          if (bindings != null) {
              System.out.println(r.rhs.applyBindings(bindings));
          }
      }
  }
  
  /** ***************************************************************
   */
  public static void main(String[] args) {  
      
      for (int i = 0; i < args.length; i++) {
          System.out.println(args[i]);
      }

      if (args[0] != null && args[0].equals("-i")) {
          String input = args[1];
          String filename = "/home/apease/IPsoft/SemRewrite.txt";
          try {
              input = StringUtil.removeEnclosingQuotes(input);
              RuleSet rs = RuleSet.readFile(filename);
              Interpreter interp = new Interpreter(rs);
              ArrayList<String> results = DependencyConverter.getDependencies(input);
              for (int i = 0; i < results.size(); i++) {
                  String in = results.get(i);
                  interp.interpret(in);
              }
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
