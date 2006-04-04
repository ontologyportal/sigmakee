/** This code is copyright Articulate Software (c) 2003.  Some portions
copyright Teknowledge (c) 2003 and reused under the terms of the GNU license.
This software is released under the GNU Public License <http://www.gnu.org/copyleft/gpl.html>.
Users of this code also consent, by use of this code, to credit Articulate Software
and Teknowledge in any writings, briefings, publications, presentations, or 
other representations of any software which incorporates, builds on, or uses this 
code.  Please cite the following article in any publication with references:

Pease, A., (2003). The Sigma Ontology Development Environment, 
in Working Notes of the IJCAI-2003 Workshop on Ontology and Distributed Systems,
August 9, Acapulco, Mexico.
*/

package com.articulate.sigma;

import java.io.*;
import java.util.*;
import java.text.ParseException;

/** *****************************************************************
 * A class designed to read a file in SUO-KIF format into memory.
 * See <http://suo.ieee.org/suo-kif.html> for a language specification.
 * readFile() and writeFile() and the only public methods.
 * @author Adam Pease
 */

public class KIF {

   /** The set of all terms in the knowledge base.  This is a set of Strings. */
  public TreeSet terms = new TreeSet();
   /** A HashMap of ArrayLists of Formulas.  @see KIF.createKey for key format. */
  public HashMap formulas = new HashMap();    
   /** A "raw" HashSet of unique Strings which are the formulas from the file without 
    *  any further processing, in the order which they appear in the file. */
  public LinkedHashSet formulaSet = new LinkedHashSet();
   /** A "raw" ArrayList of Strings which are the formulas from the file without 
    *  any further processing, in the order which they appear in the file. */
  // public ArrayList formulaList = new ArrayList();

  private String filename;
  private int totalLinesForComments = 0;

  /** ***************************************************************
   * This routine sets up the StreamTokenizer_s so that it parses SUO-KIF.
   * = < > are treated as word characters, as are normal alphanumerics.
   * ; is the line comment character and " is the quote character.
   */
  private void setupStreamTokenizer(StreamTokenizer_s st) {

      st.whitespaceChars(0,32);
      st.ordinaryChars(33,44);   // !"#$%&'()*+,
      st.wordChars(45,46);       // -.
      st.ordinaryChar(47);       // /
      st.wordChars(48,57);       // 0-9
      st.ordinaryChars(58,59);   // :;
      st.wordChars(60,64);       // <=>?@
      st.wordChars(65,90);       // A-Z
      st.ordinaryChars(91,94);   // [\]^
      st.wordChars(95,95);       // _
      st.ordinaryChar(96);       // `
      st.wordChars(97,122);      // a-z
      st.ordinaryChars(123,127); // {|}~
      // st.parseNumbers();
      st.quoteChar('"');
      st.commentChar(';');
      st.eolIsSignificant(true);
  }

  /** ***************************************************************
   */
  private void display(StreamTokenizer_s st,
                             boolean inRule,
                             boolean inAntecedent,
                             boolean inConsequent,
                             int argumentNum,
                             int parenLevel,
                             String key) {

      System.out.print (inRule);
      System.out.print ("\t");
      System.out.print (inAntecedent);
      System.out.print ("\t");
      System.out.print (inConsequent);
      System.out.print ("\t");
      System.out.print (st.ttype);
      System.out.print ("\t");
      System.out.print (argumentNum);
      System.out.print ("\t");
      System.out.print (parenLevel);
      System.out.print ("\t");
      System.out.print (st.sval);
      System.out.print ("\t");
      System.out.print (st.nval);
      System.out.print ("\t");
      System.out.print (st.toString());
      System.out.print ("\t");
      System.out.println (key);
  }

  /** ***************************************************************
   * The routine keeps track of the parenthesis level.  Outside parens
   * is level 0.  The program also keeps track of the number of the
   * argument in an expression, but only for ground statements.
   */
  private void parse(Reader r) throws ParseException, IOException {

      String key = null;
      ArrayList keySet;
      String expression;
      StreamTokenizer_s st;
      int parenLevel;
      boolean inRule;
      int argumentNum;
      boolean inAntecedent;
      boolean inConsequent;
      int lastVal;
      int lineStart;
      boolean isEOL;
      String com;
      Formula f = new Formula();
      ArrayList list;
      
      if (r == null) {
          System.err.println("No Input Reader Specified");
          return;
      }
      try {
          st = new StreamTokenizer_s(r);
          setupStreamTokenizer(st);
          parenLevel = 0;
          inRule = false;
          argumentNum = -1;
          inAntecedent = false;
          inConsequent = false;
          expression ="";
          keySet = new ArrayList();
          lastVal = -99;
          lineStart = 0;
          isEOL = false;
          do {
              lastVal = st.ttype;
              st.nextToken();

              // check the situation when multiple KIF statements read as one
              // This relies on extra blank line to seperate KIF statements
              if (st.ttype == StreamTokenizer.TT_EOL ) {
                  if (isEOL) { // two line seperators in a row, shows a new KIF statement is to start.
                      // check if a new statement has already been generated, otherwise report error
                      if (keySet.size() != 0 || expression.length() > 0) {
                          System.out.print("Parsing Error:");
                          System.out.println(new Integer(lineStart + totalLinesForComments).toString());
                          throw new ParseException("Parsing error in " + filename + ": possible missing close parenthesis.",f.startLine);
                      }
                      continue;
                  }
                  else {                                            // Found a first end of line character.
                      isEOL = true;                                 // Turn on flag, to watch for a second consecutive one.
                      continue;
                  }
              }
              else if (isEOL) 
                  isEOL = false;                                    // Turn off isEOL if a non-space token encountered                
              
              if (st.ttype==40) {                                   // open paren
                  if (parenLevel == 0) {
                      lineStart = st.lineno();
                      f = new Formula();
                      f.startLine = st.lineno() + totalLinesForComments;
                      f.sourceFile = filename;
                  }
                  parenLevel=parenLevel+1;
                  if (inRule && !inAntecedent && !inConsequent) {
                      inAntecedent = true;
                  }
                  else {
                      if (inRule && inAntecedent && (parenLevel == 2)) {
                          inAntecedent = false;
                          inConsequent = true;
                      }
                  }
                  if ((parenLevel != 0) && (lastVal != 40) && (expression != "")) { // add back whitespace that ST removes
                      expression = expression.concat(" ");
                  }
                  expression = expression.concat("(");
              }
              else if (st.ttype==41) {                                      // )  - close paren
                  parenLevel=parenLevel-1;
                  expression = expression.concat(")");
                  if (parenLevel == 0) {                                    // The end of the statement...
                      f.theFormula = expression;
                      if (formulaSet.contains(expression.intern())) {
                          System.out.print("Warning in KIF.parse(): Duplicate formula at line");
                          System.out.println(lineStart + totalLinesForComments);
                          System.out.println(expression);
                          System.out.println();
                      }
                      String validArgs = f.validArgs();
                      if (validArgs != "") 
                          throw new ParseException("Parsing error in " + filename + ".\n " + validArgs,f.startLine);  
                      // formulaList.add(expression.intern());
                      if (formulaSet.size() % 100 == 0) 
                          System.out.print('.');
                      keySet.add(expression.intern());                      // Make the formula itself a key
                      f.endLine = st.lineno() + totalLinesForComments;
                      for (int i = 0; i < keySet.size(); i++) {             // Add the expression but ...
                          if (formulas.containsKey(keySet.get(i))) {
                              if (!formulaSet.contains(expression.intern())) {  // don't add keys if formula is already present
                                  list = (ArrayList) formulas.get(keySet.get(i));
                                  if (!list.contains(f)) 
                                      list.add(f);
                              }
                          }
                          else {
                              list = new ArrayList();
                              list.add(f);
                              formulas.put((String) keySet.get(i),list);
                          }
                      }
                      formulaSet.add(expression.intern());
                      inConsequent = false;
                      inRule = false;
                      argumentNum = -1;
                      lineStart = st.lineno()+1;                            // start next statement from next line
                      expression = "";
                      keySet.clear();
                  }
                  else if (parenLevel < 0) {
                      System.out.print("Error is KIF.parse(): Extra Closing Paranthesis Found at line: ");
                      System.out.println(new Integer(lineStart + totalLinesForComments).toString());
                      throw new ParseException("Parsing error in " + filename + ": Extra closing paranthesis found.",f.startLine);
                  }
              }
              else if (st.ttype==34) {                              // " a string
                  if (lastVal != 40)                                // add back whitespace that ST removes
                      expression = expression.concat(" ");
                  expression = expression.concat("\"");
                  com = st.sval;
                  totalLinesForComments += countChar(com,(char)0X0A);
                  expression = expression.concat(com);
                  expression = expression.concat("\"");
              }
              else if ((st.ttype == StreamTokenizer.TT_NUMBER) || 
                       (st.sval != null && (Character.isDigit(st.sval.charAt(0))))) {                  // number
                  if (lastVal != 40)  // add back whitespace that ST removes
                      expression = expression.concat(" ");
                  if (st.nval == 0) 
                      expression = expression.concat(st.sval);
                  else
                      expression = expression.concat(Double.toString(st.nval));
                  if (parenLevel<2)                                 // Don't care if parenLevel > 1
                      argumentNum = argumentNum + 1;                // RAP - added on 11/27/04 
              }
              else if (st.ttype == StreamTokenizer.TT_WORD) {                  // a token
                  if ((st.sval.compareTo("=>") == 0 || st.sval.compareTo("<=>") == 0) && parenLevel == 1)   
                                                                    // RAP - added parenLevel clause on 11/27/04 to 
                                                                    // prevent implications embedded in statements from being rules
                      inRule = true;
                  if (parenLevel<2)                                 // Don't care if parenLevel > 1
                      argumentNum = argumentNum + 1;
                  if (lastVal != 40)                                // add back whitespace that ST removes
                      expression = expression.concat(" ");
                  expression = expression.concat(String.valueOf(st.sval));
                  if (expression.length() > 64000) {
                      System.out.print("Error in KIF.parse(): Parsing error: Sentence Over 64000 characters.");
                      System.out.println(new Integer(lineStart + totalLinesForComments).toString());
                      throw new ParseException("Parsing error in " + filename + ": Sentence Over 64000 characters.",f.startLine);                      
                  }
                  if (st.sval.charAt(0) != '?' && st.sval.charAt(0) != '@') {   // Variables are not terms
                      terms.add(st.sval);                                       // collect all terms
                      key = createKey(st.sval,inAntecedent,inConsequent,argumentNum,parenLevel);
                      keySet.add(key);                                          // Collect all the keys until the end of
                  }                                                             // the statement is reached.
              }                                    
              else if (st.ttype != StreamTokenizer.TT_EOF) {
                  key = null;
                  System.out.print("Error in KIF.parse(): Parsing Error: Illegal character at line: ");
                  System.out.println(new Integer(lineStart + totalLinesForComments).toString());
                  throw new ParseException("Parsing error in " + filename + ": Illegal character.",f.startLine);                      
              }
              // if (key != null)
              //    display(st,inRule,inAntecedent,inConsequent,argumentNum,parenLevel,key);
          } while (st.ttype != StreamTokenizer.TT_EOF);
          if (keySet.size() != 0 || expression.length() > 0) {
              System.out.println("Error in KIF.parse(): Parsing error: ");
              System.out.println("Kif ends before parsing finishes.  Missing closing parenthesis.");
              throw new ParseException("Parsing error in " + filename + ": Missing closing paranthesis.",f.startLine);                      
          }
      }
      catch (java.io.FileNotFoundException e) {
          throw new FileNotFoundException("kif file " + filename + " not found");
      }
      catch (java.io.IOException e) {
          throw new IOException("IO exception parsing file " + filename);
      }
      System.out.println();
  }

  /** ***************************************************************
   * This routine creates a key that relates a token in a
   * logical statement to the entire statement.  It prepends
   * to the token a string indicating its position in the
   * statement.  The key is of the form type-[num]-term, where [num]
   * is only present when the type is "arg", meaning a statement in which
   * the term is nested only within one pair of parentheses.  The other
   * possible types are "ant" for rule antecedent, "cons" for rule consequent,
   * and "stmt" for cases where the term is nested inside multiple levels of
   * parentheses.  An example key would be arg-0-instance for a appearance of
   * the term "instance" in a statement in the predicate position.
   *
   * @param sval - the token such as "instance", "Human" etc.
   * @param inAntecedent - whether the term appears in the antecedent of a rule.
   * @param inConsequent - whether the term appears in the consequent of a rule.
   * @param argumentNum - the argument position in which the term appears.  The
   *             predicate position is argument 0.  The first argument is 1 etc.
   * @param parenLevel - if the paren level is > 1 then the term appears nested
   *             in a statement and the argument number is ignored.
   */
  private String createKey (String sval,
                                  boolean inAntecedent,
                                  boolean inConsequent,
                                  int argumentNum,
                                  int parenLevel) {

    if (sval == null) { sval="null";}
    String key = new String("");
    if (inAntecedent) {
        key = key.concat("ant-");
        key = key.concat(sval);
    }

    if (inConsequent) {
        key = key.concat("cons-");
        key = key.concat(sval);
    }

    if (!inAntecedent && !inConsequent && (parenLevel==1)) {
        key = key.concat("arg-");
        key = key.concat(String.valueOf(argumentNum));
        key = key.concat("-");
        key = key.concat(sval);
    }
    if (!inAntecedent && !inConsequent && (parenLevel>1)) {
        key = key.concat("stmt-");
        key = key.concat(sval);
    }
    return (key);
  }

  /** ***************************************************************
   * Count the number of appearences of a certain character in a string.
   * @param str - the string to be tested.
   * @param c - the character to be counted.
   */

  private int countChar(String str, char c) {

      int len = 0;
      char[] cArray = str.toCharArray();
      for (int i = 0; i < cArray.length; i++) {
          if (cArray[i] == c)
              len ++;      
      }
      return len;
  }
  
  /** ***************************************************************
   * Read a KIF file.
   * @param fname - the full pathname of the file.
   */
  public void readFile(String fname) throws IOException, ParseException {

      FileReader fr = new FileReader(fname);
      filename = fname;
      try {
          parse(fr);
      }
      catch (ParseException pe) {
          System.out.print("Error in KIF.readFile(): " + pe.getMessage() + " at line ");
          System.out.println(pe.getErrorOffset());
      }
      catch (java.io.IOException e) {
          throw new IOException("Error in KIF.readFile(): IO exception parsing file " + filename);
      }
  }
  
  /** ***************************************************************
   * Write a KIF file.
   * @param fname - the name of the file to write, including full path.
   */
  public void writeFile(String fname) throws IOException {

      FileWriter fr = null;
      PrintWriter pr = null;
      Iterator it;
      ArrayList formulaArray;

      System.out.println("INFO in KIF.writeFile(): Filename: " + fname + " num formulas: " + String.valueOf(formulaSet.size()));
      try {
          fr = new FileWriter(fname);
          pr = new PrintWriter(fr);

          it = formulaSet.iterator();
          while (it.hasNext())
              pr.println((String) it.next());          
      }
      catch (java.io.IOException e) {
          throw new IOException("Error writing file " + filename);
      }
      finally {
          if (pr != null) {
              pr.close();
          }
          if (fr != null) {
              fr.close();
          }
      }
  }

  /** ***************************************************************
   * Parse a single formula.
   */
  public String parseStatement(String formula, String f) {

      StringReader r = new StringReader(formula);
      filename = f;
      try {
          parse(r);
      }
      catch (Exception e) {
          return e.getMessage();
      }
      finally {
          return null;
      }
  }

  /** ***************************************************************
   * Test method for this class.
   */
  public static void main(String args[]) {

      Iterator it;
      KIF kifp = new KIF();
      Formula f;
      String form;
      ArrayList list;
      
      try {
          kifp.readFile("C:\\Program Files\\Apache Tomcat 4.0\\tests\\little-celtTest.txt");
      }
      catch (IOException ioe) {
          System.out.println(ioe.getMessage());
      }
      catch (ParseException pe) {
          System.out.println(pe.getMessage());
          System.out.print("In statement starting at line: ");
          System.out.println(pe.getErrorOffset());
      }
      it = kifp.formulaSet.iterator();
      while (it.hasNext()) {
          form = (String) it.next();
          System.out.println (form);          
      }      
  }
}
