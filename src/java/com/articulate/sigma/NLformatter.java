  
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

import java.util.*;
import java.io.*;

  /** ***************************************************************
  *  A class that handles the generation of natural language from logic.
  *
  *  @author Adam Pease - adampease@earthlink.net, and Justin Chen, with thanks
  *  to Michal Sevcenko - sevcenko@vc.cvut.cz for development of the
  *  formatting language.
  */

public class NLformatter {

  private static HashMap keywordMap;

  private NLformatter () { }  // This class should never have any instances.
  
  /** ***************************************************************
  *  Read a set of standard words and phrases in several languages.
  *  Each phrase must appear on a new line with alternatives separated by '|'.
  *  The first entry should be a set of two letter language identifiers.
  *
  *  @return a HashMap of HashMaps where the first HashMap has a key of the 
  *  English phrase, and the interior HashMap has a key of the two letter
  *  language identifier.
  */
  public static HashMap readKeywordMap(String dir) {

      String fname = dir + File.separator + "language.txt";
      String line;
      HashMap newLine;
      ArrayList languageKeyArray = new ArrayList();
      String key;
      int i;
      int count;
      LineNumberReader lnr;
      try {
          lnr = new LineNumberReader(new FileReader(fname));
      }
      catch (IOException ioe) {
          System.out.println("Error in NLformatter.readKeywordMap(): Error opening file " + fname);
          System.out.println(System.getProperty("user.dir"));

          return keywordMap;
      }

      try {
          if (keywordMap == null) {
               keywordMap = new HashMap();
               lnr = new LineNumberReader(new FileReader(fname));
               do {
                   line = lnr.readLine();
                   if (line != null) {
                       if (line.startsWith("en|")) { // The language key line.
                           i = 0;
                           while (line.indexOf('|',i) > 0) {
                               languageKeyArray.add(line.substring(i,line.indexOf('|',i)));
                               i = line.indexOf('|',i) + 1;
                           }
                           languageKeyArray.add(line.substring(i,i+2));
                       }
                       else if (line.startsWith(";")) {  // ignore comment lines
                       }
                       else if (line.indexOf('|') > -1) { // Line with phrase alternates in different languages.
                           newLine = new HashMap();
                           key = line.substring(0,line.indexOf('|'));
                           i = 0;
                           count = 0;
                           while (line.indexOf('|',i) > 0) {
                               newLine.put(languageKeyArray.get(count),line.substring(i,line.indexOf('|',i)));
                               i = line.indexOf('|',i) + 1;
                               count++;
                           }
                           newLine.put(languageKeyArray.get(count),line.substring(i,line.length()));
                           // System.out.println("INFO in NLformatter.keywordMap(): key: " + key + " value: " + newLine);
                           keywordMap.put(key.intern(),newLine);
                       }
                       else {
                           System.out.println("INFO in NLformatter.keywordMap(): Unrecognized line in language.txt: " + line);
                       }
                   }
               } while (line != null);
           }
      }
      catch (IOException ioe) {
          try {
              lnr.close();
          }
          catch (IOException e) {
              System.out.println("Error in NLformatter.keywordMap(): Error closing file " + fname);
          };
          return (keywordMap);
      }
      try {
          lnr.close();
      }
      catch (IOException e) {
          System.out.println("Error  in NLformatter.readKeywordMap(): Error closing file " + fname);
      };
      return (keywordMap);
  }

  /** ***************************************************************
  *  Create a natural language paraphrase of a logical statement.  This is the
  *  entry point for this function, but kifExprPara does most of the work.
  *
  *  @param stmt The statement to be paraphrased.
  *  @param phraseMap An association list of relations and their natural language format statements.
  *  @param termMap An association list of terms and their natural language format statements.
  *  @return A String, which is the paraphrased statement.
  */
  public static String nlStmtPara(String stmt, Map phraseMap, Map termMap, String language) {
 
      String theStmt;
      Stack phraser = new Stack();   // the words stack for each sentence
      int pos = stmt.indexOf("(");
      if (pos != -1) {
          theStmt = stmt.substring(pos);
      }
      else {
          theStmt = stmt;
          System.out.println("Error in NLformatter.nlStmtPara(): statement: " + stmt + " has no opening parenthesis"); 
          return theStmt;
      }
      String delimit = " ()\"";
      StringTokenizer st = new StringTokenizer(theStmt, delimit, true);
      
      //System.out.println("INFO in NLformatter.nlStmtPara(): Statement: " + stmt);
      if (phraseMap == null) {
          System.out.println("Error in NLformatter.nlStmtPara(): phrase map is null.");     
          return "";
      }

      while (st.hasMoreTokens()) {
          String token = st.nextToken();
          if (token.equalsIgnoreCase(" ")) continue;   // Ignore spaces
          if (token.equalsIgnoreCase("\"")) {          // Read a string as one token
              StringBuffer sb = new StringBuffer();
              sb.append(token);
              while (st.hasMoreTokens() && !(token = st.nextToken()).equalsIgnoreCase("\""))
                  sb.append(token);
              sb.append(token);
              token = sb.toString();
          }
              // Expression found.  Paraphrase the expression and push it onto the stack.
          if (token.equalsIgnoreCase(")")) {
              // System.out.println("INFO in NLformatter.nlStmtPara(): " + phraser);
              String paraExp = kifExprPara(phraser,phraseMap,termMap,language);    // Paraphrase the expression.

              if (paraExp != null)
                  phraser.push(paraExp);  // push result onto the stack
              else {
                  System.out.println("Error in NLformatter.nlStmtPara(): English formatting error: " + theStmt + " with stack:");
                  while (phraser.size() > 0)
                      System.out.println(" stack: " + phraser.pop().toString());
              }
              continue;
          }
          phraser.push(token);
      }

      // There should be just one english phrase left in the stack
      if (phraser.size() > 1)
          System.out.println("Error in NLformatter.nlStmtPara(): English paraphrasing doesn't complete.");
      else if (phraser.size() < 1) {
          System.out.println("Error in NLformatter.nlStmtPara(): English paraphrasing fails.");
          return null;
      }
      return phraser.pop().toString();
  }

  /** ***************************************************************
  *  This method reads all tokens from the top of a stack until it reaches "(".
  *  It then converts them into a natural language sentence and pushes the result back onto the stack.
  *
  *  @param phraser The stack of words
  *  @param phraseMap The mapping from the predicate, the key, to its format, the value
  *  @param termMap The mapping from the term, the key, to its format, the value
  *  @return String the paraphrased expression.
  */

  private static String kifExprPara(Stack phraser, Map phraseMap, Map termMap, String language) {

      //System.out.println("INFO in NLformatter.kifExprPara(): phraser: " + phraser.toString());
      ArrayList words = new ArrayList();
      boolean isPlural = false;
      String pred;
      StringBuffer sb; 
      String term = null;
      boolean isNegMode = false;            // Whether the statement is negated.

      if (phraser.size() != 0)              // Empty phrase?
          term = phraser.pop().toString();
      else {
          System.out.println("Error in NLformatter.kifExprPara(): missing (");
          return "null";
      }
      while (!term.equalsIgnoreCase("(")) { // Look for the start of the expression.
          words.add(term);
          if (phraser.size() != 0)
              term = phraser.pop().toString();
          else {
              System.out.println("Error in NLformatter.kifExprPara(): missing (");
              break;
          }
      }

      if (words.size() == 0) return null;   // no tokens
      // System.out.println("INFO in NLformatter.kifExprPara(): " + words.toString());
      if (phraser.size() > 0 && phraser.peek().toString().equalsIgnoreCase("not")) {
          phraser.pop();   // Pop the "not" off the stack.
          if (phraser.peek().toString().equalsIgnoreCase("(")) // The predicate is a "not"
              isNegMode = true;
          else
              phraser.push("not"); // The "not" is not a predicate, so put it back on the stack.
      }
      else if (phraser.size() > 0 && words.size() >= 2) { // should change to plural if it is at subject and has two operands
          pred = phraser.pop().toString();
          if (phraser.size() > 0 && phraser.peek().toString().equalsIgnoreCase("(")) 
              phraser.push("$p$" + pred); // found plural
          else
              phraser.push(pred); // put it back.
      }

      sb = new StringBuffer();
      pred = words.get(words.size()-1).toString();
      // System.out.println("INFO in NLformatter.kifExprPara(): pred: " + pred);
      if (pred == null || pred == "" || (pred != null && pred != "" && !Character.isJavaIdentifierStart(pred.charAt(0)))) { 
          if (pred.charAt(0) == '?') {                       // If the predicate is a variable, assume it's a quantifier list
              for (int i = words.size()-1; i >= 0; i--) {
                  sb.append(words.get(i).toString());
                  if (i != 0) sb.append(" and ");
              }
              return sb.toString();
          }
          else
              return pred;
      }
      sb.append(paraphraseLogicalOperators(pred,isNegMode,words,termMap,language));
      if (sb.toString().length() != 0) 
          return sb.toString();

      if (pred.startsWith("$p$")) {
          isPlural = true;
          pred = pred.substring(pred.indexOf("$p$")+3);
          words.set(words.size()-1,pred);
      }

      String format = null;
      if (phraseMap != null)
          format = (String) phraseMap.get(pred);    // Get the format string for the current predicate.
      else
          System.out.println("Error in NLformatter.kifExprPara(): Phrase map is null."); 
      if (format == null)                                                                                                                                                   
          System.out.println("Error in NLformatter.kifExprPara(): Format is null for predicate " + pred + " with phrase map of size " + (new Integer(phraseMap.keySet().size())).toString()); 
          
      if (format == null) {                // If no format statement, return default format.
          if (isNegMode) 
              sb.append(((String) (((HashMap) keywordMap.get("not")).get(language))));
          sb.append (" ");
          for (int i = words.size()-1; i >= 0; i--) {
              if (termMap !=null && termMap.containsKey((String) words.get(i)))
                  sb.append((String) termMap.get((String) words.get(i)));
              else
                  sb.append(words.get(i).toString());
              if (i != 0) sb.append(" ");
          }
          return sb.toString();
      }
      else {               // Paraphrase the expression using a natural language format string
          sb.append(paraphraseWithFormat(format,words,termMap,isNegMode,isPlural,language));
          return sb.toString();
      }
  }

  /** ***************************************************************
   * Return the NL format of an individual word.
   */
  private static String translateWord(Map termMap, String word) {

      if (termMap !=null && termMap.containsKey(word))
          return((String) termMap.get(word));
      else
          return (word);
  }
  
  /** ***************************************************************
   * Create a natural language paraphrase for statements involving the logical operators.
   *
   * @param pred the logical predicate in the expression
   * @param isNegMode is the expression negated?
   * @param words  the expression as an ArrayList of tokens
   * @return the natural language paraphrase as a String, or null if the predicate was not a logical operator.
   */

  private static String paraphraseLogicalOperators (String pred, boolean isNegMode, ArrayList words, Map termMap, String language) {

      //System.out.println("INFO in NLformatter.paraphraseLogicalOperators(): predicate: " + pred);
      //System.out.println("words: " + words.toString());
      if (keywordMap == null) {
          System.out.println("Error in NLformatter.paraphraseLogicalOperators(): keywordMap is null.");
          return null;
      }
      if (termMap == null) {
          System.out.println("Error in NLformatter.paraphraseLogicalOperators(): termMap is null.");
      }
      String IF = ((String) (((HashMap) keywordMap.get("if")).get(language)));
      String THEN = ((String) (((HashMap) keywordMap.get("then")).get(language)));
      String AND = ((String) (((HashMap) keywordMap.get("and")).get(language)));
      String OR = ((String) (((HashMap) keywordMap.get("or")).get(language)));
      String IFANDONLYIF = (((String) ((HashMap) keywordMap.get("if and only if")).get(language)));
      String NOT = ((String) (((HashMap) keywordMap.get("not")).get(language)));
      String FORALL = ((String) (((HashMap) keywordMap.get("for all")).get(language)));
      String EXISTS = ((String) (((HashMap) keywordMap.get("there exists")).get(language)));
      String EXIST = ((String) (((HashMap) keywordMap.get("there exist")).get(language))); 
      String NOTEXIST = ((String) (((HashMap) keywordMap.get("there don't exist")).get(language)));
      String NOTEXISTS = ((String) (((HashMap) keywordMap.get("there doesn't exist")).get(language)));
      String HOLDS = ((String) (((HashMap) keywordMap.get("holds")).get(language)));
      String SOTHAT = ((String) (((HashMap) keywordMap.get("so that")).get(language)));

      StringBuffer sb = new StringBuffer();

      if (pred.equalsIgnoreCase("=>") || pred.equalsIgnoreCase("$p$=>")) {
          if (!isNegMode)
            sb.append("<ul><li>"+IF+" ").append(words.get(1)).append(",<li>"+THEN+" ").append(words.get(0)).append("</ul>");
          else
            sb.append(words.get(1)).append(" "+AND+" ").append("~{").append(words.get(0)).append("}");
          return sb.toString();
      }
      else if (pred.equalsIgnoreCase("and") || pred.equalsIgnoreCase("$p$and")) {
          if (!isNegMode)
              for (int i = (words.size()-2); i >= 0; i--) {
                  if (i != (words.size()-2)) 
                      sb.append(" "+AND+" ");
                  sb.append(translateWord(termMap,(String) words.get(i)));
              }
          else
              for (int i = (words.size()-2); i >= 0; i--) {
                  if (i != (words.size()-2)) 
                      sb.append(" "+OR+" ");
                  sb.append("~{ ");
                  sb.append(translateWord(termMap,(String) words.get(i)));
                  sb.append(" }");
              }
          return sb.toString();
      }
      else if (pred.equalsIgnoreCase("or")|| pred.equalsIgnoreCase("$p$or")) {
          if (!isNegMode)
              for (int i = (words.size()-2); i >= 0; i--) {
                  if (i != (words.size()-2)) 
                      sb.append(" "+OR+" ");
                  sb.append(translateWord(termMap,(String) words.get(i)));
              }
          else
              for (int i = (words.size()-2); i >= 0; i--) {
                  if (i != (words.size()-2)) 
                      sb.append(" "+AND+" ");
                  sb.append(translateWord(termMap,(String) words.get(i)));
              }
          return sb.toString();
      }
      else if (pred.equalsIgnoreCase("<=>") || pred.equalsIgnoreCase("$p$<=>")) {
          if (!isNegMode) {
              sb.append(translateWord(termMap,(String) words.get(1)));
              sb.append(" "+IFANDONLYIF+" ");
              sb.append(translateWord(termMap,(String) words.get(0)));
          }
          else {
              sb.append(translateWord(termMap,(String) words.get(1)));
              sb.append(" "+OR+" ");
              sb.append("~{ ");
              sb.append(translateWord(termMap,(String) words.get(0)));
              sb.append(" }");
              sb.append(" "+OR+" ");
              sb.append(translateWord(termMap,(String) words.get(0)));
              sb.append(" "+OR+" ");
              sb.append("~{ ");
              sb.append(translateWord(termMap,(String) words.get(1)));
              sb.append(" }");
          }
          return sb.toString();
      }
      else if (pred.equalsIgnoreCase("forall") || pred.equalsIgnoreCase("$p$forall")) {
          if (!isNegMode) {
              sb.append(FORALL+" ");
              sb.append(translateWord(termMap,(String) words.get(1)));
              sb.append(" "+HOLDS+": ");
              sb.append(translateWord(termMap,(String) words.get(0)));
          }
          else {
              sb.append(" "+NOT+" "+FORALL+" ");
              sb.append(translateWord(termMap,(String) words.get(1)));
              sb.append(" "+HOLDS+": ");
              sb.append(translateWord(termMap,(String) words.get(0)));
          }
          return sb.toString();
      }
      else if (pred.equalsIgnoreCase("exists")) {
          if (words.size() != 3) {
              for (int i = words.size()-1; i >= 0; i--) {
                  sb.append(translateWord(termMap,(String) words.get(i)));
                  sb.append(" ");
              }
              return sb.toString(); // not the right english format
          }
          if (!isNegMode) {
              sb.append(EXISTS+" ");
              sb.append(translateWord(termMap,(String) words.get(1)));
              sb.append(" "+SOTHAT+" ");
              sb.append(translateWord(termMap,(String) words.get(0)));
          }
          else {
              sb.append(NOTEXIST+" ");
              sb.append(translateWord(termMap,(String) words.get(1)));
              sb.append(" "+SOTHAT+" ");
              sb.append(translateWord(termMap,(String) words.get(0)));
          }
          return sb.toString();
      }
      else if (pred.equalsIgnoreCase("$p$exists")) { // the plural
          if (!isNegMode) {
              sb.append(EXIST+" ");
              sb.append(translateWord(termMap,(String) words.get(1)));
              sb.append(" "+SOTHAT+" ");
              sb.append(translateWord(termMap,(String) words.get(0)));
          }
          else {
              sb.append(NOTEXISTS+" ");
              sb.append(translateWord(termMap,(String) words.get(1)));
              sb.append(" "+SOTHAT+" ");
              sb.append(translateWord(termMap,(String) words.get(0)));
          }
          return sb.toString();
      }
      else 
          return "";
  }
   
  /** ***************************************************************
   * Create a natural language paraphrase of a logical statement, where the
   * predicate is not a logical operator.  Use a printf-like format string to generate
   * the paraphrase.
   *
   * @param strFormat the prinf-style formatting string
   * @param words the expression as an ArrayList of tokens
   * @param isNegMode whether the statement is negated, and therefore requiring special formatting.
   * @param isPlural whether the statement is plural, and therefore requiring special formatting.
   * @return the paraphrased statement.
   */
  private static String paraphraseWithFormat(String strFormat, ArrayList words, Map termMap, 
                                             boolean isNegMode, boolean isPlural, String language) {

      //System.out.println("INFO in NLformatter.paraphraseWithFormat(): words: " + words.toString());
      //System.out.println("INFO in NLformatter.paraphraseWithFormat(): strFormat: " + strFormat);
      //System.out.print("INFO in NLformatter.paraphraseWithFormat(): isNegMode: ");
      //System.out.println(isNegMode);
      StringBuffer sb = new StringBuffer();
      int total = words.size();
      int index;

      if (strFormat.indexOf("%n") == -1 && isNegMode) 
          sb.append(((String) (((HashMap) keywordMap.get("not")).get(language))) + " ");
      
      FORMAT:
      for (int i = 0; i < strFormat.length(); i++) {
          char c = strFormat.charAt(i);
          switch (c) {
              case '%': {                            // Is it a formatting code?
                  if (i >= 1 && strFormat.charAt(i-1) == '&' || i == (strFormat.length() - 1) ) {
                      sb.append(c);   // The percent sign didn't indicate a formatting character.
                      continue;       // Don't touch a &% symbol or a final %
                  }
                  else {                             // The percent sign indicated a format character.
                      switch (strFormat.charAt(i+1)) { // look at next char
                          case '0': case '1': case '2': case '3': case '4': case '5': case '6': case '7': case '8':
                          case '9': {
                              int pos = strFormat.charAt(i+1) - '0';
                              if (pos >= total)
                                  index = total - 1;
                              else
                                  index = total - 1 - pos;
                              if (termMap !=null && termMap.containsKey((String) words.get(index)))
                                  sb.append((String) termMap.get((String) words.get(index)));
                              else
                                  sb.append(words.get(index));
                              i++; // Skip the next character.
                              continue;
                          }
                          case 'p': { 
                              i = paraphrasePositiveRendering(i,sb,strFormat,isPlural);
                              continue FORMAT;
                          }
                          case 'n': {
                              i = paraphraseNegativeRendering(i,sb,strFormat,isNegMode,language);
                              continue FORMAT;
                          }
                          case '*': { // skip %*{2-}[,] 
                              i = paraphraseStar(i,sb,strFormat,words,termMap);
                              continue FORMAT;
                          }
                          default: {       // Ignore undefined format characters.
                              sb.append(c);
                          }
                      } // switch
                  } // if else
              }
              default: {
                  sb.append(c);  // not a "%"  formatting character
              }
          }  // switch
      }// for
      //System.out.println("INFO in NLformatter.paraphraseWithFormat(): sb: " + sb.toString());
      return sb.toString();
  }

  /** ***************************************************************
   * Create a natural language paraphrase with regards to a positive element
   * in the format string.
   *
   * @param i the index of the current character under examination in the format string
   * @param sb the output String, containing the paraphrased expression.
   * @param strFormat the prinf-style formatting string
   * @param isPlural whether the statement is plural, and therefore requiring special formatting.
   * @return the index in the formatting string of the next character to examine.
   */
  private static int paraphrasePositiveRendering(int i, StringBuffer sb, String strFormat, boolean isPlural) {
  
      //System.out.println("INFO in NLformatter.paraphrasePositiveRendering(): sb: " + sb.toString());
      char ch;
      i++;  // Point to the 'p' format character that indicates a positive rendering.
      if (strFormat.charAt(i+1) == '{') {
          i++ ; // point to '{'
          ch = strFormat.charAt(i+1);
          while (ch != '}') {
              if (!isPlural)
                  sb.append(ch);
              i++;
              ch = strFormat.charAt(i+1);
          }
          i++;  // skip the closing format bracket '}'
      }
      return i;
  }
  
  /** ***************************************************************
   * Create a natural language paraphrase with regards to a negative element
   * in the format string.
   *
   * @param i the index of the current character under examination in the format string
   * @param sb the output String, containing the paraphrased expression.
   * @param strFormat the prinf-style formatting string
   * @param isNegMode whether the statement is negated, and therefore requiring special formatting.  
   * @return the index in the formatting string of the next character to examine.
   */
  private static int paraphraseNegativeRendering(int i, StringBuffer sb, String strFormat, boolean isNegMode, String language) {
      
      //System.out.println("INFO in NLformatter.paraphraseNegativeRendering(): sb: " + sb.toString());
      //System.out.println("INFO in NLformatter.paraphraseNegativeRendering(): strFormat: " + strFormat);
      char ch;
      i++;  // Point to the 'n' format character that indicates a negative rendering.
      if (strFormat.charAt(i+1) == '{') {
          i++; // point to '{'
          ch = strFormat.charAt(i+1);
          while (ch != '}') {
              if (isNegMode)
                sb.append(ch);
              i++;
              ch = strFormat.charAt(i+1);
          }
          i++;  // skip the closing format bracket '}'
      }
      else    // no appending negative text
        if (isNegMode)
          sb.append(((String) (((HashMap) keywordMap.get("not")).get(language))));
      return i;
  }

  /** ***************************************************************
   * Handle the '*' format character in a natural language formatting string.
   *
   * @param i the index of the current character under examination in the format string
   * @param sb the output String, containing the paraphrased expression.
   * @param strFormat the prinf-style formatting string
   * @param words  the expression as an ArrayList of tokens
   * @return the index in the formatting string of the next character to examine.
   */
  private static int paraphraseStar (int i, StringBuffer sb, String strFormat, ArrayList words, Map termMap) {

      //System.out.println("INFO in NLformatter.paraphraseStar(): sb: " + sb.toString());
      char seperator = ',';
      int start = 0;
      int end = -1;

      i++; // move to either "*"
      if (strFormat.charAt(i+1)== '{') {
          i++; // point to '{'
          start = strFormat.charAt(i+1) - '0';
          i++; // start
          i++; // "-"
          if ( strFormat.charAt(i+1) != '}') {
              end = strFormat.charAt(i+1);
              i++; // end
              i++; // '}'
          }
          else
              i++; // '}'
      }
      if ( strFormat.charAt(i+1) == '[') {
          i++; // point to "["
          seperator = strFormat.charAt(i+1);
          i++; // point to seperator
          i++; // point to "]"
      }

      for (int j = (words.size()-start-2); j >= ( end != -1? (words.size()-end):0); j--) {
          if (j != words.size()-start-2) 
              sb.append(' ').append(seperator).append(' ');
          if (termMap !=null && termMap.containsKey((String) words.get(1)))
              sb.append((String) termMap.get((String) words.get(j)));
          else
              sb.append(words.get(j));
      }
      return i;
  }

  /** **************************************************************
   * Hyperlink terms in a natural language format string.  This assumes that
   * terms to be hyperlinked are in the form &%termName$termString , where
   * termName is the name of the term to be browsed in the knowledge base and
   * termString is the text that should be displayed hyperlinked.
   *
   * @param href the anchor string up to the term= parameter, which this method
   *               will fill in.
   * @param stmt the KIF statement that will be passed to nlStmtPara for formatting.
   * @param phraseMap the set of NL formatting statements that will be passed to nlStmtPara.
   * @param termMap the set of NL statements for terms that will be passed to nlStmtPara.
   * @param language the natural language in which the paraphrase should be generated.
   */

  public static String htmlParaphrase(String href,String stmt, Map phraseMap, Map termMap, String language) {

      int end;
      int start = -1;
      String nlFormat = nlStmtPara(stmt,phraseMap,termMap,language);
      while (nlFormat.indexOf("&%") > -1) {

          start = nlFormat.indexOf("&%",start+1);
          int word = nlFormat.indexOf("$",start);
          if (word == -1) 
              end = start + 2;
          else
              end = word + 1;
          while (end < nlFormat.length() && Character.isJavaIdentifierPart(nlFormat.charAt(end))) 
              end++;
          if (word == -1) 
              nlFormat = nlFormat.substring(0,start) + "<a href=\"" + href + "&term=" + nlFormat.substring(start+2,end) + "\">" +
                  nlFormat.substring(start+1,end) + "</a>" + nlFormat.substring(end, nlFormat.length());
          else
              nlFormat = nlFormat.substring(0,start) + "<a href=\"" + href + "&term=" + nlFormat.substring(start+2,word) + "\">" +
                  nlFormat.substring(word+1,end) + "</a>" + nlFormat.substring(end, nlFormat.length());
      }

      return nlFormat;
  }

  /** **************************************************************
   */
  public static void main(String[] args) {

      readKeywordMap("C:\\Program Files\\Apache Tomcat 4.0\\KBs");
      HashMap phraseMap = new HashMap();
      phraseMap.put("foo","%1 is a foo of %2");
      HashMap termMap = new HashMap();
      System.out.println(nlStmtPara("(not (foo ?FOO ?BAR))",phraseMap,termMap,"en"));

  }
}

