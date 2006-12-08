 
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
  *  @author Adam Pease - apease [at] articulatesoftware [dot] com, with thanks
  *  to Michal Sevcenko - sevcenko@vc.cvut.cz for development of the
  *  formatting language.
  */

public class LanguageFormatter {

  private static HashMap keywordMap;

  private LanguageFormatter () { }  // This class should never have any instances.

  /** ***************************************************************
   */
  private static String getKeyword(String englishWord, String language) {

      HashMap hm = (HashMap) keywordMap.get(englishWord);
      return (String) hm.get(language);
  }

  /** ***************************************************************
   * Format a list of variables which are not enclosed by parens
   */
  private static String formatList(String list, String language) {

      StringBuffer result = new StringBuffer();
      String[] ar = list.split(" ");
      for (int i = 0; i < ar.length; i++) {
          if (i == 0) 
              result.append(transliterate(ar[i],language));
          if (i > 0 && i < ar.length - 1) 
              result.append(", " + transliterate(ar[i],language));
          if (i == ar.length -1) 
              result.append(" " + getKeyword("and",language) + " " + transliterate(ar[i],language));
      }
      return result.toString();
  }

  /** ***************************************************************
   */
  private static boolean logicalOperator(String word) {

      if (word.equals("if") || word.equals("then") || word.equals("=>") ||
          word.equals("and") || word.equals("or") ||
          word.equals("<=>") || word.equals("not") ||
          word.equals("forall") || word.equals("exists") ||
          word.equals("holds")) 
          return true;
      else
          return false;
  }

  /** ***************************************************************
   */
  private static String transliterate(String word, String language) {

      if (word.charAt(0) != '?') 
          return word;
      else
          if (language.equals("ar")) {
              StringBuffer result = new StringBuffer();
              result.append('?');
              for (int i = 1; i < word.length(); i++) {
                  switch (word.charAt(i)) {
                    case 'A': result.append("\u0627\u0654"); break;
                    case 'B': result.append("\uFE8F"); break;
                    case 'C': result.append("\uFED9"); break;
                    case 'D': result.append("\uFEA9"); break;
                    case 'E': result.append("\u0650"); break;
                    case 'F': result.append("\uFED1"); break;
                    case 'G': result.append("\uFE9D"); break;
                    case 'H': result.append("\uFEE9"); break;
                    case 'I': result.append("\u0650"); break;
                    case 'J': result.append("\uFE9D"); break;
                    case 'K': result.append("\uFED9"); break;
                    case 'L': result.append("\uFEDD"); break;
                    case 'M': result.append("\uFEE1"); break;
                    case 'N': result.append("\uFEE5"); break;
                    case 'O': result.append("\u064F"); break;
                    case 'P': result.append("\uFE8F"); break;
                    case 'Q': result.append("\uFED5"); break;
                    case 'R': result.append("\uFEAD"); break;
                    case 'S': result.append("\uFEB1"); break;
                    case 'T': result.append("\uFE95"); break;
                    case 'U': result.append("\u064F"); break;
                    case 'V': result.append("\uFE8F"); break;
                    case 'W': result.append("\uFEED"); break;
                    case 'X': result.append("\uFEAF"); break;
                    case 'Y': result.append("\uFEF1"); break;
                    case 'Z': result.append("\uFEAF"); 
                    result.append(word.charAt(i));
                  }
              }
              return result.toString();
          }
          else
              return word;
  }

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
      BufferedReader br;
      try {
          br = new BufferedReader(new InputStreamReader(new FileInputStream(fname),"UTF-8"));
      }
      catch (IOException ioe) {
          System.out.println("Error in LanguageFormatter.readKeywordMap(): Error opening file " + fname);
          System.out.println(System.getProperty("user.dir"));
          return keywordMap;
      }

      try {
          if (keywordMap == null) {
               keywordMap = new HashMap();
               br = new BufferedReader(new InputStreamReader(new FileInputStream(fname),"UTF-8"));
               // lnr = new LineNumberReader(new FileReader(fname));
               do {
                   line = br.readLine();
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
                       else if (line.length() == 0) {  // ignore blank lines
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
                           // System.out.println("INFO in LanguageFormatter.keywordMap(): key: " + key + " value: " + newLine);
                           keywordMap.put(key.intern(),newLine);
                       }
                       else {
                           System.out.println("INFO in LanguageFormatter.keywordMap(): Unrecognized line in language.txt: " + line);
                       }
                   }
               } while (line != null);
           }
      }
      catch (IOException ioe) {
          try {
              br.close();
          }
          catch (IOException e) {
              System.out.println("Error in LanguageFormatter.keywordMap(): Error closing file " + fname);
          };
          return (keywordMap);
      }
      try {
          br.close();
      }
      catch (IOException e) {
          System.out.println("Error  in LanguageFormatter.readKeywordMap(): Error closing file " + fname);
      };
      return (keywordMap);
  }
  
  /** ***************************************************************
   * */
  private static String processAtom(String atom, Map termMap, String language) {

      if (atom.charAt(0) == '?') 
          return transliterate(atom,language);
      if (termMap.containsKey(atom)) 
          return (String) termMap.get(atom);
      return atom;
  }

  /** ***************************************************************
  *  Create a natural language paraphrase of a logical statement.  This is the
  *  entry point for this function, but kifExprPara does most of the work.
  *
  *  @param stmt The statement to be paraphrased.
  *  @param isNegMode Whether the statement is negated.
  *  @param phraseMap An association list of relations and their natural language format statements.
  *  @param termMap An association list of terms and their natural language format statements.
  *  @return A String, which is the paraphrased statement.
  */
  public static String nlStmtPara(String stmt, boolean isNegMode, Map phraseMap, Map termMap, String language) {

      System.out.println("INFO in LanguageFormatter.nlStmtPara(): Statement: " + stmt);
      if (stmt == null || stmt.length() < 1) {
          System.out.println("Error in LanguageFormatter.nlStmtPara(): Statement is empty: " + stmt);
          return "";
      }
      StringBuffer result = new StringBuffer();
      Formula f = new Formula();
      f.read(stmt);
      if (f.atom())
          return processAtom(stmt,termMap,language);
      else
          if (!f.listP()) {
              System.out.println("Error in LanguageFormatter.nlStmtPara(): Statement is not an atom or a list: " + stmt);
              return "";
          }
      if (phraseMap == null) {
          System.out.println("Error in LanguageFormatter.nlStmtPara(): phrase map is null.");
          phraseMap = new HashMap();
      }
      String pred = f.car();
      if (!Formula.atom(pred)) {
          System.out.println("Error in LanguageFormatter.nlStmtPara(): statement: " + stmt + " has a formula in the predicate position."); 
          return stmt;
      }
      if (logicalOperator(pred)) 
          return paraphraseLogicalOperator(stmt,isNegMode,phraseMap,termMap,language);       
      if (phraseMap.containsKey(pred)) 
          return paraphraseWithFormat(stmt,isNegMode,phraseMap,termMap,language);      
      else {                                                    // predicate has no paraphrase
          if (pred.charAt(0) == '?') 
              result.append(transliterate(pred,language));
          else {
              if (termMap.containsKey(pred)) 
                  result.append((String) termMap.get(pred));
              else
                  result.append(pred);
          }
          f.read(f.cdr());
          while (!f.empty()) {
              String arg = f.car();
              f.read(f.cdr());
              if (!Formula.atom(arg)) 
                  result.append(" " + nlStmtPara(arg,isNegMode,phraseMap,termMap,language));
              else
                  result.append(" " + translateWord(termMap,arg,language));
          }
      }
      return result.toString();
  }

  /** ***************************************************************
   * Return the NL format of an individual word.
   */
  private static String translateWord(Map termMap, String word, String language) {

      if (termMap !=null && termMap.containsKey(word))
          return((String) termMap.get(word));
      else {
          if (word.charAt(0) == '?') 
              return transliterate(word,language);
          else
              return (word);
      }
  }
  
  /** ***************************************************************
   * Create a natural language paraphrase for statements involving the logical operators.
   *
   * @param pred the logical predicate in the expression
   * @param isNegMode is the expression negated?
   * @param words  the expression as an ArrayList of tokens
   * @return the natural language paraphrase as a String, or null if the predicate was not a logical operator.
   */
  private static String paraphraseLogicalOperator(String stmt, boolean isNegMode, Map phraseMap, Map termMap, String language) {

      System.out.println("INFO in LanguageFormatter.paraphraseLogicalOperator(): statement: " + stmt);
      if (keywordMap == null) {
          System.out.println("Error in LanguageFormatter.paraphraseLogicalOperator(): keywordMap is null.");
          return null;
      }
      ArrayList args = new ArrayList();
      Formula f = new Formula();
      f.read(stmt);
      String pred = f.getArgument(0);
      f.read(f.cdr());

      if (pred.equals("not")) 
          return nlStmtPara(f.car(),true,phraseMap,termMap,language);

      while (!f.empty()) {
          String arg = f.car();
          String result = nlStmtPara(arg,isNegMode,phraseMap,termMap,language);

          if (result != null && result != "" && result.length() > 0) 
              args.add(result);
          else {
              System.out.println("INFO in LanguageFormatter.paraphraseLogicalOperators(): bad result for: " + arg);
              arg = " ";
          }

          System.out.println("INFO in LanguageFormatter.paraphraseLogicalOperators(): adding argument: " + ((String) args.get(args.size()-1)));
          f.read(f.cdr());
      }
      String IF = getKeyword("if",language);
      String THEN = getKeyword("then",language);
      String AND = getKeyword("and",language);
      String OR = getKeyword("or",language);
      String IFANDONLYIF = getKeyword("if and only if",language);
      String NOT = getKeyword("not",language);
      String FORALL = getKeyword("for all",language);
      String EXISTS = getKeyword("there exists",language);
      String EXIST = getKeyword("there exist",language); 
      String NOTEXIST = getKeyword("there don't exist",language);
      String NOTEXISTS = getKeyword("there doesn't exist",language);
      String HOLDS = getKeyword("holds",language);
      String SOTHAT = getKeyword("so that",language);

      StringBuffer sb = new StringBuffer();

      if (pred.equalsIgnoreCase("=>")) {
          if (!isNegMode)
            sb.append("<ul><li>"+IF+" ").append(args.get(0)).append(",<li>"+THEN+" ").append(args.get(1)).append("</ul>");
          else
            sb.append(args.get(1)).append(" "+AND+" ").append("~{").append(args.get(0)).append("}");
          return sb.toString();
      }
      if (pred.equalsIgnoreCase("and")) {
          if (!isNegMode)
              for (int i = 0; i < args.size(); i++) {
                  if (i != 0) 
                      sb.append(" "+AND+" ");
                  sb.append(translateWord(termMap,(String) args.get(i),language));
              }
          else
              for (int i = 0; i < args.size(); i++) {
                  if (i != 0) 
                      sb.append(" "+OR+" ");
                  sb.append("~{ ");
                  sb.append(translateWord(termMap,(String) args.get(i),language));
                  sb.append(" }");
              }
          return sb.toString();
      }
      if (pred.equalsIgnoreCase("holds")) {
          if (!isNegMode)
              for (int i = 0; i < args.size(); i++) {
                  if (i != 0) 
                      sb.append(" "+HOLDS+" ");
                  sb.append(translateWord(termMap,(String) args.get(i),language));
              }
          else
              for (int i = 0; i < args.size(); i++) {
                  if (i != 0) 
                      sb.append(" "+NOT+" "+HOLDS+" ");
                  sb.append(translateWord(termMap,(String) args.get(i),language));
              }
          return sb.toString();
      }
      if (pred.equalsIgnoreCase("or")) {
          if (!isNegMode)
              for (int i = 0; i < args.size(); i++) {
                  if (i != 0) 
                      sb.append(" "+OR+" ");
                  sb.append(translateWord(termMap,(String) args.get(i),language));
              }
          else
              for (int i = 0; i < args.size(); i++) {
                  if (i != 0) 
                      sb.append(" "+AND+" ");
                  sb.append(translateWord(termMap,(String) args.get(i),language));
              }
          return sb.toString();
      }
      if (pred.equalsIgnoreCase("<=>")) {
          if (!isNegMode) {
              sb.append(translateWord(termMap,(String) args.get(0),language));
              sb.append(" "+IFANDONLYIF+" ");
              sb.append(translateWord(termMap,(String) args.get(1),language));
          }
          else {
              sb.append(translateWord(termMap,(String) args.get(1),language));
              sb.append(" "+OR+" ");
              sb.append("~{ ");
              sb.append(translateWord(termMap,(String) args.get(0),language));
              sb.append(" }");
              sb.append(" "+OR+" ");
              sb.append(translateWord(termMap,(String) args.get(0),language));
              sb.append(" "+OR+" ");
              sb.append("~{ ");
              sb.append(translateWord(termMap,(String) args.get(1),language));
              sb.append(" }");
          }
          return sb.toString();
      }
      if (pred.equalsIgnoreCase("forall")) {
          if (!isNegMode) {
              sb.append(FORALL+" ");
              sb.append(translateWord(termMap,formatList((String) args.get(0),language),language));
              sb.append(" "+HOLDS+": ");
              sb.append(translateWord(termMap,(String) args.get(1),language));
          }
          else {
              sb.append(" "+NOT+" "+FORALL+" ");
              sb.append(translateWord(termMap,formatList((String) args.get(0),language),language));
              sb.append(" "+HOLDS+": ");
              sb.append(translateWord(termMap,(String) args.get(1),language));
          }
          return sb.toString();
      }
      if (pred.equalsIgnoreCase("exists")) {
          if (((String) args.get(0)).indexOf(" ") == -1) {   // just one variable
              if (args.size() != 3) {
                  for (int i = args.size()-1; i >= 0; i--) {
                      sb.append(translateWord(termMap,(String) args.get(i),language));
                      sb.append(" ");
                  }
                  return sb.toString(); // not the right english format
              }
              if (!isNegMode) {
                  sb.append(EXISTS+" ");
                  sb.append(translateWord(termMap,(String) args.get(0),language));
                  sb.append(" "+SOTHAT+" ");
                  sb.append(translateWord(termMap,(String) args.get(1),language));
              }
              else {
                  sb.append(NOTEXIST+" ");
                  sb.append(translateWord(termMap,(String) args.get(0),language));
                  sb.append(" "+SOTHAT+" ");
                  sb.append(translateWord(termMap,(String) args.get(1),language));
              }
              return sb.toString();
          }
          else {                                // more than one variable
              if (!isNegMode) {
                  sb.append(EXIST+" ");
                  sb.append(translateWord(termMap,formatList((String) args.get(0),language),language));
                  sb.append(" "+SOTHAT+" ");
                  sb.append(translateWord(termMap,(String) args.get(1),language));
              }
              else {
                  sb.append(NOTEXISTS+" ");
                  sb.append(translateWord(termMap,formatList((String) args.get(0),language),language));
                  sb.append(" "+SOTHAT+" ");
                  sb.append(translateWord(termMap,(String) args.get(1),language));
              }
              return sb.toString();
          }
      }       
      return "";
  }
   
  /** ***************************************************************
   * Create a natural language paraphrase of a logical statement, where the
   * predicate is not a logical operator.  Use a printf-like format string to generate
   * the paraphrase.
   *
   * @param stmt the statement to format
   * @param isNegMode whether the statement is negated, and therefore requiring special formatting.
   * @return the paraphrased statement.
   */
  private static String paraphraseWithFormat(String stmt, boolean isNegMode, Map phraseMap, 
                                             Map termMap, String language) {

      System.out.println("INFO in LanguageFormatter.paraphraseWithFormat(): Statement: " + stmt);
      System.out.println("neg mode: " + isNegMode);
      Formula f = new Formula();
      f.read(stmt);
      String pred = f.car();
      String strFormat = (String) phraseMap.get(pred);
      System.out.println("str format: " + strFormat);
      int index;

      if (strFormat.indexOf("&%") > -1)                     // setup the term hyperlink
          strFormat.replace("&%","&%"+pred+"$");
      
      if (isNegMode) {                                      // handle negation
          if (strFormat.indexOf("%n") == -1) 
              strFormat = getKeyword("not",language) + " " + strFormat;
          else {
              if (strFormat.indexOf("%n{") == -1) 
                  strFormat = strFormat.replace("%n",getKeyword("not",language));
              else {
                  int start = strFormat.indexOf("%n{") + 3;
                  int end = strFormat.indexOf("}",start);
                  strFormat = strFormat.substring(0,start-3) + strFormat.substring(start,end) + 
                      strFormat.substring(end+1,strFormat.length());
              }
          }
          isNegMode = false;
          strFormat = strFormat.replace("%p ","");          // delete all the unused positive commands
          strFormat = strFormat.replaceAll("%p\\{[\\w\\']+\\} ","");
      }
      else {                                                // statement is not negated
          strFormat = strFormat.replace("%n ","");          // delete all the unused negative commands
          strFormat = strFormat.replaceAll("%n\\{[\\w\\']+\\} ","");
          if (strFormat.indexOf("%p{") != -1) {           
              int start = strFormat.indexOf("%p{") + 3;
              int end = strFormat.indexOf("}",start);
              strFormat = strFormat.substring(0,start-3) + strFormat.substring(start,end) + 
                  strFormat.substring(end+1,strFormat.length());              
          }
      }

      int num = 1;                                          // handle arguments
      String argPointer = "%" + (new Integer(num)).toString();
      while (strFormat.indexOf(argPointer) > -1) {
          System.out.println("INFO in LanguageFormatter.paraphraseWithFormat(): Statement: " + f.theFormula);
          System.out.println("arg: " + f.getArgument(num));
          System.out.println("num: " + num);
          System.out.println("str: " + strFormat);
          strFormat = strFormat.replace(argPointer,nlStmtPara(f.getArgument((int) num),isNegMode,phraseMap,termMap,language));
          num++;
          argPointer = "%" + (new Integer(num)).toString();
      }
      
      System.out.println("str: " + strFormat);
      return strFormat.toString();
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
      String nlFormat = nlStmtPara(stmt,false,phraseMap,termMap,language);
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

      readKeywordMap("C:\\Program Files\\Apache Software Foundation\\Tomcat 5.5\\KBs");
      HashMap phraseMap = new HashMap();
      phraseMap.put("foo","%1 is %n{nicht} a &%foo of %2");
      HashMap termMap = new HashMap();
      System.out.println(htmlParaphrase("","(=> (exists (?FOO ?BAR) (foo ?FOO ?BAR)) (bar ?BIZ ?BONG))",phraseMap,termMap,"en"));

  }
}

