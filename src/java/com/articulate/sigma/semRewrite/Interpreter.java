package com.articulate.sigma.semRewrite;


/*
Copyright 2014-2015 IPsoft

Author: Adam Pease adam.pease@ipsoft.com

This program is free software; you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation; either version 2 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program ; if not, write to the Free Software
Foundation, Inc., 59 Temple Place, Suite 330, Boston,
MA  02111-1307 USA 
*/

import java.util.*;
import java.util.regex.*;
import java.io.*;

import com.articulate.sigma.*;
import com.articulate.sigma.semRewrite.datesandnumber.*;
import com.google.common.collect.Lists;

public class Interpreter {

    // Canonicalize rules into CNF then unify.

  public RuleSet rs = null;
  //public CNF input = null;
  public String fname = "";
  
  // execution options
  public static boolean inference = false;
  public static boolean question = false;
  public static boolean addUnprocessed = false;
  
  // debug options
  public static boolean showrhs = false;
  public static boolean showr = true;
  
  public static List<String> qwords = Lists.newArrayList("who","what","where","when","why","which","how");
  public static List<String> months = Lists.newArrayList("January","February","March","April","May","June",
		  "July","August","September","October","November","December");
  public static List<String> days = Lists.newArrayList("Sunday","Monday","Tuesday","Wednesday","Thursday","Friday","Saturday");
  
  /** *************************************************************
   */
  public Interpreter () {
      
  }

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
   * @return a map of the word key and the value as a string 
   * consisting of the word plus a dash and its number in
   * the sentence such as walks -> walks-5
   */
  private static HashMap<String,String> extractWords(ArrayList<String> clauses) {
      
      HashMap<String,String> purewords = new HashMap<String,String>();
      for (int i = 0; i < clauses.size(); i++) {
          String clause = clauses.get(i);
          int paren = clause.indexOf('(');
          int comma = clause.indexOf(',');
          
          String arg1 = clause.substring(paren+1,comma).trim();
          int wordend1 = arg1.indexOf('-');
          String purearg1 = arg1.substring(0,wordend1);
          if (!purearg1.equals("ROOT"))
              purewords.put(purearg1,arg1);
          
          String arg2 = clause.substring(comma + 1, clause.length()-1).trim();
          int wordend2 = arg2.indexOf('-');
          String purearg2 = arg2.substring(0,wordend2);
          if (!purearg2.equals("ROOT"))
              purewords.put(purearg2,arg2);
      }
      return purewords;
  }
  
  /** *************************************************************
   */
  public static boolean excluded(String word) {
	  
	  return (months.contains(word) || days.contains(word));
  }
  
  /** *************************************************************
   */
  public static ArrayList<String> findWSD(ArrayList<String> clauses, HashMap<String,String> purewords) {
      
      System.out.println("INFO in Interpreter.addWSD(): " + clauses);
	  KB kb = KBmanager.getMgr().getKB("SUMO");
      DependencyConverter.readFirstNames();
      ArrayList<String> results = new ArrayList<String>();
      ArrayList<String> pure = new ArrayList<String>();
      pure.addAll(purewords.keySet());
      for (int i = 0; i < pure.size(); i++) {
          String pureword = pure.get(i);
          if (WordNet.wn.stopwords.contains(pureword) || qwords.contains(pureword.toLowerCase()) || excluded(pureword))
              continue;
          String id = WSD.findWordSenseInContext(pureword, pure);
          if (!StringUtil.emptyString(id)) {
              String sumo = WordNetUtilities.getBareSUMOTerm(WordNet.wn.getSUMOMapping(id));
              if (!StringUtil.emptyString(sumo)) {
                  if (sumo.indexOf(" ") > -1) {  // TODO: if multiple mappings...
                      sumo = sumo.substring(0,sumo.indexOf(" ")-1);
                  }
                  if (kb.isInstance(sumo))
                      results.add("equals(" + sumo + "," + purewords.get(pureword) + ")");
                  else
                	  results.add("sumo(" + sumo + "," + purewords.get(pureword) + ")");
              }
          }
          else if (DependencyConverter.maleNames.contains(pureword)) {
              results.add("sumo(Human," + purewords.get(pureword) + ")");
              results.add("attribute(" + purewords.get(pureword) + ",Male)");
          }
          else if (DependencyConverter.femaleNames.contains(pureword)) {
              results.add("sumo(Human," + purewords.get(pureword) + ")"); 
              results.add("attribute(" + purewords.get(pureword) + ",Female)");
          }      
          else {
              String synset = WSD.getBestDefaultSense(pureword);
              if (!StringUtil.emptyString(synset)) {
                  String sumo = WordNetUtilities.getBareSUMOTerm(WordNet.wn.getSUMOMapping(synset));
                  if (!StringUtil.emptyString(sumo)) {
                      if (sumo.indexOf(" ") > -1) {  // TODO: if multiple mappings...
                          sumo = sumo.substring(0,sumo.indexOf(" ")-1);
                      }
                      results.add("sumo(" + sumo + "," + purewords.get(pureword) + ")");
                  }
                  else
                      results.add("sumo(Entity," + purewords.get(pureword) + ")");
              }
          }
      }
      //System.out.println("INFO in Interpreter.addWSD(): " + results);
      //results.addAll(clauses);
      return results;
  }

  /** *************************************************************
   */
  private static ArrayList<String> findQuantification(String form) {
	  
	  ArrayList<String> quantified = new ArrayList<String>();
	  String pattern = "\\?[A-Za-z0-9_]+\\-[0-9]+";
	  Pattern p = Pattern.compile(pattern);
	  Formula f = new Formula(form);
	  Set<String> vars = f.collectAllVariables();
	  System.out.println("INFO in Interpreter.testAddQuantification(): vars: " + vars);
	  for (String v : vars) {
		  Matcher matcher = p.matcher(v);
		  if (matcher.matches()) 
			  quantified.add(v);
	  }
	  return quantified;
  }
  
  /** *************************************************************
   */
  private static String prependQuantifier(ArrayList<String> vars, String form) {
	  
	  System.out.println("INFO in Interpreter.prependQuantifier(): " + vars);
	  StringBuffer sb = new StringBuffer();
	  if (vars == null || vars.size() < 1)
		  return form;
	  sb.append("(exists (");
	  boolean first = true;
	  for (String v : vars) {
		  if (!first) {
			  sb.append(" ");
		  }
		  sb.append(v);
		  first = false;
	  }
	  sb.append(") \n");
	  sb.append(form);
	  sb.append(") \n");
	  return sb.toString();
  }
  
  /** *************************************************************
   */
  private static String addQuantification(String form) {
	  
	  ArrayList<String> vars = findQuantification(form);
	  return prependQuantifier(vars, form);
  }
  
  /** *************************************************************
   */
  public String toFOL(ArrayList<String> clauses) {
      
      StringBuilder sb = new StringBuilder();
      if (clauses.size() > 1)
          sb.append("(and \n");
      for (int i = 0; i < clauses.size(); i++) {
          sb.append("  " + clauses.get(i));
          if (i < clauses.size()-1)
              sb.append("\n");
      }
      if (clauses.size() > 1)
          sb.append(")\n");
      return sb.toString();
  }
  
  /** *************************************************************
   * Take in a sentence and output a SUO-KIF string
   */
  public String interpretSingle(String input) {
      
	  System.out.println("INFO in Interpreter.interpretSingle(): " + input); 
	  ArrayList<String> results = null;
      try {
          results = DependencyConverter.getDependencies(input);
      }
      catch (Exception e) {
          e.printStackTrace();
          System.out.println(e.getMessage());
      }
      HashMap<String,String> purewords = extractWords(results);
      ArrayList<String> wsd = findWSD(results,purewords);
      results.addAll(wsd);           
      String in = StringUtil.removeEnclosingCharPair(results.toString(),Integer.MAX_VALUE,'[',']'); 
      
      ArrayList<CNF> inputs = new ArrayList<CNF>();
      Lexer lex = new Lexer(in);
      CNF cnf = CNF.parseSimple(lex);
      inputs.add(cnf);
      ArrayList<String> kifClauses = interpretCNF(inputs);
      kifClauses.addAll(InterpretNumerics.getSumoTerms(input));
      return fromKIFClauses(kifClauses);
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
  public static String postProcess(String s) {
	  
	  String pattern = "([^\\?A-Za-z])([A-Za-z0-9_]+\\-[0-9]+)";
	  Pattern p = Pattern.compile(pattern);
	  Matcher matcher = p.matcher(s);
	  while (matcher.find()) {
		  s = s.replace(matcher.group(1) + matcher.group(2), matcher.group(1) + "?" + matcher.group(2));
	  }
	  Formula f = new Formula(s);
	  
	  return s;
  }
  
  /** *************************************************************
   */
  public static void preProcessQuestionWords(CNF inputs) {
      
      //List<String> qphrase = Lists.newArrayList("how much","how many","how often","how far","how come");
      inputs.preProcessQuestionWords(qwords);
  }
  
  /** *************************************************************
   */
  public static void addUnprocessed(ArrayList<String> kifoutput, CNF cnf) {
      
      StringBuilder sb = new StringBuilder();
      for (Disjunct d : cnf.clauses) {
          if (d.disjuncts.size() > 1)
              sb.append("(or \n");
          for (Clause c : d.disjuncts) {
              kifoutput.add("(" + c.pred + " " + c.arg1  + " " + c.arg2 + ") ");
          }
          if (d.disjuncts.size() > 1)
              sb.append(")\n");
      }
      kifoutput.add(sb.toString());      
  }
  
  /** *************************************************************
   */
  public ArrayList<String> interpretCNF(ArrayList<CNF> inputs) {
      
	  if (inputs.size() > 1) {
	      System.out.println("Error in Interpreter.interpretCNF(): multiple clauses"); 
		  return null;
	  }
      ArrayList<String> kifoutput = new ArrayList<String>();
      System.out.println("INFO in Interpreter.interpretCNF(): inputs: " + inputs); 
      boolean bindingFound = true;
      int counter = 0;
      while (bindingFound && counter < 10 && inputs != null && inputs.size() > 0) {
          counter++;
          bindingFound = false;
          ArrayList<CNF> newinputs = new ArrayList<CNF>();
          CNF newInput = null;
          for (int j = 0; j < inputs.size(); j++) {          
              newInput = inputs.get(j).deepCopy();
              //System.out.println("INFO in Interpreter.interpret(): new input 0: " + newInput);
              for (int i = 0; i < rs.rules.size(); i++) {
                  Rule r = rs.rules.get(i).deepCopy();      
                  //System.out.println("INFO in Interpreter.interpret(): new input 0.5: " + newInput);
                  //System.out.println("INFO in Interpreter.interpret(): r: " + r);
                  HashMap<String,String> bindings = r.cnf.unify(newInput);
                  if (bindings == null) {
                      newInput.clearBound();
                  }
                  else {
                      bindingFound = true;
                      //System.out.println("INFO in Interpreter.interpret(): new input 1: " + newInput);
                      //System.out.println("INFO in Interpreter.interpret(): bindings: " + bindings);
                      if (showr)
                    	  System.out.println("INFO in Interpreter.interpret(): r: " + r);
                      RHS rhs = r.rhs.applyBindings(bindings);   
                      if (r.operator == Rule.RuleOp.IMP) {
                          CNF bindingsRemoved = newInput.removeBound(); // delete the bound clauses
                          //System.out.println("INFO in Interpreter.interpret(): input with bindings removed: " + bindingsRemoved);
                          if (!bindingsRemoved.empty()) {  // assert the input after removing bindings
                              if (rhs.cnf != null) {
                            	  if (showrhs)
                            		  System.out.println("INFO in Interpreter.interpret(): add rhs " + rhs.cnf);
                                  bindingsRemoved.merge(rhs.cnf);
                              }
                              newInput = bindingsRemoved;
                          }
                          else
                              if (rhs.cnf != null) {
                            	  if (showrhs)
                            		  System.out.println("INFO in Interpreter.interpret(): add rhs " + rhs.cnf);
                                  newInput = rhs.cnf;
                              }
                          if (rhs.form != null && !kifoutput.contains(rhs.form.toString())) { // assert a KIF RHS
                              kifoutput.add(rhs.form.toString());
                          }
                          //System.out.println("INFO in Interpreter.interpret(): new input 2: " + newInput + "\n");
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
                  newInput.clearBound();                    
                  newInput.clearPreserve();
              }
          }
          if (bindingFound)
              newinputs.add(newInput);
          else
        	  if (addUnprocessed)
        		  addUnprocessed(kifoutput,newInput); // a hack to add unprocessed SDP clauses as if they were KIF
          inputs = new ArrayList<CNF>();
          inputs.addAll(newinputs);
          System.out.println("INFO in Interpreter.interpret(): KB: " + printKB(inputs));
          //System.out.println("INFO in Interpreter.interpret(): bindingFound: " + bindingFound);
          //System.out.println("INFO in Interpreter.interpret(): counter: " + counter);
          //System.out.println("INFO in Interpreter.interpret(): newinputs: " + newinputs);
          //System.out.println("INFO in Interpreter.interpret(): inputs: " + inputs);
      }
      return kifoutput;
  }
  
  /** ***************************************************************
   */
  public String fromKIFClauses(ArrayList<String> kifcs) {
	  
      String s1 = toFOL(kifcs);
      String s2 = postProcess(s1);
      String s3 = addQuantification(s2);
      System.out.println("INFO in Interpreter.interpret(): KIF: " + s3);
      if (inference) {
    	  KB kb = KBmanager.getMgr().getKB("SUMO");
    	  if (question)
    		  System.out.println(kb.askNoProof(s3,30,1));
    	  else
    		  System.out.println(kb.tell(s3));
      }
      return s3;
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
              if (input.equals("reload")) {
            	  System.out.println("reloading semantic rewriting rules");
                  loadRules();
              }
              else if (input.equals("inference")) {
                  inference = true;
            	  System.out.println("turned inference on");
              }
              else if (input.equals("noinference")) {
                  inference = false;
            	  System.out.println("turned inference off");
              }
              else if (input.equals("addUnprocessed")) {
                  addUnprocessed = true;
            	  System.out.println("adding unprocessed clauses");
              }
              else if (input.equals("noUnprocessed")) {
                  addUnprocessed = false;
            	  System.out.println("not adding unprocessed clauses");
              }
              else if (input.equals("noshowr")) {
                  showr = false;
            	  System.out.println("not showing rule that are applied");
              }
              else if (input.equals("showr")) {
                  showr = true;
            	  System.out.println("showing rules that are applied");
              }
              else if (input.equals("noshowrhs")) {
                  showrhs = false;
            	  System.out.println("not showing right hand sides that are asserted");
              }
              else if (input.equals("showrhs")) {
                  showrhs = true;
            	  System.out.println("showing right hand sides that are asserted");
              }
              else if (input.startsWith("load "))
                  loadRules(input.substring(input.indexOf(' ')+1));
              else {
            	  if (input.trim().endsWith("?"))
            		  question = true;
            	  else
            		  question = false;
            	  System.out.println("INFO in Interpreter.interpretIter(): " + input); 
            	  interpretSingle(input);
              }
          }
      } while (!StringUtil.emptyString(input));
  }
  
  /** ***************************************************************
   */
  public void loadRules(String f) {

      if (f.indexOf(File.separator.toString(),2) < 0)
          f = "/home/apease/SourceForge/KBs/WordNetMappings" + f;
      try {
          fname = f;
          RuleSet rsin = RuleSet.readFile(f);
          rs = canon(rsin);
      }
      catch (Exception e) {
          e.printStackTrace();
          System.out.println(e.getMessage());
          return;
      }
      System.out.println("INFO in Interpreter.loadRules(): " +
          rs.rules.size() + " rules loaded from " + f);
  }
  
  /** ***************************************************************
   */
  public void loadRules() {

      String filename = KBmanager.getMgr().getPref("kbDir") + File.separator + "SemRewrite.txt";
      String pref = KBmanager.getMgr().getPref("SemRewrite");
      if (!StringUtil.emptyString(pref))
          filename = pref;
      loadRules(filename);
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

      try {
    	  KBmanager.getMgr().initializeOnce();
          Interpreter interp = new Interpreter();
          interp.loadRules();
          String sent = "John walks to the store.";
          System.out.println("INFO in Interpreter.testInterpret(): " + sent);
          String input = "nsubj(runs-2,John-1), root(ROOT-0,runs-2), det(store-5,the-4), prep_to(runs-2,store-5), sumo(Human,John-1), attribute(John-1,Male), sumo(RetailStore,store-5), sumo(Running,runs-2).";
          Lexer lex = new Lexer(input);
          CNF cnfInput = CNF.parseSimple(lex);
          ArrayList<CNF> inputs = new ArrayList<CNF>();
          inputs.add(cnfInput);
          
          System.out.println(interp.interpretCNF(inputs));
          //System.out.println("INFO in Interpreter.testInterpret():" + interp.interpretSingle(sent));
          
          sent = "John takes a walk.";
          System.out.println("INFO in Interpreter.testInterpret(): " + sent);
          input = "nsubj(takes-2,John-1), root(ROOT-0,takes-2), det(walk-4,a-3), dobj(takes-2,walk-4), sumo(Human,John-1), attribute(John-1,Male), sumo(agent,takes-2), sumo(Walking,walk-4).";
          lex = new Lexer(input);
          cnfInput = CNF.parseSimple(lex);
          inputs = new ArrayList<CNF>();
          inputs.add(cnfInput);
          System.out.println(interp.interpretCNF(inputs));
          //System.out.println("INFO in Interpreter.testInterpret():" + interp.interpretSingle(sent));
      }
      catch (Exception e) {
          e.printStackTrace();
          System.out.println(e.getMessage());
      }
  }
  
  /** *************************************************************
   * A test method
   */
  public static void testPreserve() {
      
      System.out.println("INFO in Interpreter.testPreserve()--------------------");
      Interpreter interp = new Interpreter();
      String rule = "+sumo(?O,?X), nsubj(?E,?X), dobj(?E,?Y) ==> " +
              "{(foo ?E ?X)}.";
      Rule r = new Rule();
      r = Rule.parseString(rule);
      RuleSet rsin = new RuleSet();
      rsin.rules.add(r);
      interp.rs = canon(rsin);
      Clausifier.clausify(r.lhs);
      String input = "sumo(Object,bank-2), nsubj(hired-3, bank-2),  dobj(hired-3, John-4).";
      Lexer lex = new Lexer(input);
      CNF cnfInput = CNF.parseSimple(lex);
      ArrayList<CNF> inputs = new ArrayList<CNF>();
      inputs.add(cnfInput);
      interp.interpretCNF(inputs);
      System.out.println("INFO in Interpreter.testPreserve(): result should be KIF for foo and sumo");
      
      interp = new Interpreter();
      String rule2 = "sumo(?O,?X), nsubj(?E,?X), dobj(?E,?Y) ==> " +  // no preserve tag
              "{(foo ?E ?X)}.";
      r = new Rule();
      r = Rule.parseString(rule2);
      rsin = new RuleSet();
      rsin.rules.add(r);
      interp.rs = canon(rsin);
      Clausifier.clausify(r.lhs);
      input = "sumo(Object,bank-2), nsubj(hired-3, bank-2),  dobj(hired-3, John-4).";
      lex = new Lexer(input);
      cnfInput = CNF.parseSimple(lex);
      inputs = new ArrayList<CNF>();
      inputs.add(cnfInput);
      interp.interpretCNF(inputs);
      System.out.println("INFO in Interpreter.testPreserve(): result should be KIF for foo");
      
      interp = new Interpreter();
      String rule3 = "sumo(?O,?X) ==> (instance(?X,?O)).";
      r = new Rule();
      r = Rule.parseString(rule3);
      rsin = new RuleSet();
      rsin.rules.add(r);
      interp.rs = canon(rsin);
      Clausifier.clausify(r.lhs);
      input = "det(river-5,the-4), sumo(Walking,walks-2), sumo(Human,John-1), sumo(River,river-5).";
      lex = new Lexer(input);
      cnfInput = CNF.parseSimple(lex);
      inputs = new ArrayList<CNF>();
      inputs.add(cnfInput);
      interp.interpretCNF(inputs);
      System.out.println("INFO in Interpreter.testPreserve(): result should be KIF:");
      System.out.println(" (and (det river-5 the-4) (instance walks-2 Walking) (instance John-1 Human) (instance river-5 River))");
  }
  
  /** ***************************************************************
   */
  public static void testQuestionPreprocess() {
      
      String input = "advmod(is-2, Where-1), root(ROOT-0, is-2), nsubj(is-2, John-3).";
      Lexer lex = new Lexer(input);
      CNF cnfInput = CNF.parseSimple(lex);
      Rule r = new Rule();
      preProcessQuestionWords(cnfInput);
      System.out.println("INFO in Interpreter.testUnify(): Input: " + cnfInput);
  }
  
  /** ***************************************************************
   */
  public static void testPostProcess() {
      
      String input = "(and (agent kicks-2 John-1) (instance kicks-2 Kicking) (patient kicks-2 cart-4)" +
    		  			"(instance John-1 Human) (instance cart-4 Wagon))";
      System.out.println("INFO in Interpreter.testUnify(): Input: " + postProcess(input));
  }
  
  
  /** ***************************************************************
   */
  public static void testWSD() {

	  KBmanager.getMgr().initializeOnce();
	  String input = "Amelia is a pilot.";
	  ArrayList<String> results = null;
	  try {
		  results = DependencyConverter.getDependencies(input);
	  }
	  catch (Exception e) {
		  e.printStackTrace();
		  System.out.println(e.getMessage());
	  }
	  HashMap<String,String> purewords = extractWords(results);
	  ArrayList<String> wsd = findWSD(results,purewords);
	  System.out.println("INFO in Interpreter.testUnify(): Input: " + wsd);
  }

  /** ***************************************************************
   */
  public static void testTimeDateExtraction() {

	  System.out.println("INFO in Interpreter.testTimeDateExtraction()");
	  Interpreter interp = new Interpreter();
	  KBmanager.getMgr().initializeOnce();
	  interp.loadRules();

	  System.out.println("----------------------");
	  String input = "John killed Mary on 31 March and also in July 1995 by travelling back in time.";
	  System.out.println(input);
	  String sumoTerms = interp.interpretSingle(input);
	  System.out.println(sumoTerms);

	  System.out.println("----------------------");
	  input = "Amelia Mary Earhart (July 24, 1897 – July 2, 1937) was an American aviator.";
	  System.out.println(input);
	  sumoTerms = interp.interpretSingle(input);
	  System.out.println(sumoTerms);

	  System.out.println("----------------------");
	  input = "Earhart vanished over the South Pacific Ocean in July 1937 while trying to fly around the world.";
	  System.out.println(input);
	  sumoTerms = interp.interpretSingle(input);
	  System.out.println(sumoTerms);

	  System.out.println("----------------------");
	  input = "She was declared dead on January 5, 1939.";
	  System.out.println(input);
	  sumoTerms = interp.interpretSingle(input);
	  System.out.println(sumoTerms);

	  System.out.println("----------------------");
	  input = "Bob went to work only 5 times in 2003.";
	  System.out.println(input);
	  sumoTerms = interp.interpretSingle(input);
	  System.out.println(sumoTerms);
  }

  /** ***************************************************************
   */
  public static void testAddQuantification() {

      String input = "(and (agent kicks-2 John-1) (instance kicks-2 Kicking) (patient kicks-2 cart-4)" +
	  			"(instance John-1 Human) (instance cart-4 Wagon))";
      String s1 = postProcess(input);
      System.out.println("INFO in Interpreter.testAddQuantification(): Input: " + input);
      System.out.println("INFO in Interpreter.testAddQuantification(): Output: " + addQuantification(s1));
      
      input = "(and (agent kicks-2 ?WH) (instance kicks-2 Kicking) (patient kicks-2 cart-4)" +
	  			"(instance ?WH Human) (instance cart-4 Wagon))";
      s1 = postProcess(input);
      System.out.println("INFO in Interpreter.testAddQuantification(): Input: " + input);
      System.out.println("INFO in Interpreter.testAddQuantification(): Output: " + addQuantification(s1));
  }

  /** ***************************************************************
   */
  public static void main(String[] args) {  

      System.out.println("INFO in Interpreter.main()");
      Interpreter interp = new Interpreter();
      if (args != null && args.length > 0 && (args[0].equals("-s") || args[0].equals("-i"))) {
          KBmanager.getMgr().initializeOnce();
          interp.loadRules();
      }
      if (args != null && args.length > 0 && args[0].equals("-s")) {
          interp.interpretSingle(args[1]);
      }
      else if (args != null && args.length > 0 && args[0].equals("-i")) {
          interp.interpInter();
      }
      else if (args != null && args.length > 0 && args[0].equals("-h")) {
          System.out.println("Semantic Rewriting with SUMO, Sigma and E");
          System.out.println("  options:");
          System.out.println("  -s - runs one conversion of one sentence");
          System.out.println("  -i - runs a loop of conversions of one sentence at a time,");
          System.out.println("       prompting the user for more.  Empty line to exit.");
          System.out.println("       'load filename' will load a specified rewriting rule set.");
          System.out.println("       'reload' (no quotes) will reload the rewriting rule set.");
          System.out.println("       'inference/noinference' will turn on/off inference.");
          System.out.println("       'addUnprocessed/noUnprocessed' will add/not add unprocessed clauses.");
          System.out.println("       'showr/noshowr' will show/not show what rules get matched.");
          System.out.println("       'showrhs/noshowrhs' will show/not show what right hand sides get asserted.");
          System.out.println("       Ending a sentence with a question mark will trigger a query,");
          System.out.println("       otherwise results will be asserted to the KB.");
      }
      else {
          //testUnify();
          //testInterpret();
          //testPreserve();
          //testQuestionPreprocess();
    	  //testPostProcess();
    	  //testTimeDateExtraction();
    	  testAddQuantification();
      }
  }
}
