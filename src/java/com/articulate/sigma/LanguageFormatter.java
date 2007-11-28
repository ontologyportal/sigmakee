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

	String ans = "";
	HashMap hm = (HashMap) keywordMap.get(englishWord);
	if (hm != null) {
	    ans = (String) hm.get(language);
	    if (ans == null)
		ans = "";	   
	}
	return ans;
    }

    /** ***************************************************************
     * Format a list of variables which are not enclosed by parens
     */
    private static String formatList(String list, String language) {

	StringBuffer result = new StringBuffer();
        String comma = getKeyword(",", language);
	String[] ar = list.split(" ");
	for (int i = 0; i < ar.length; i++) {
	    if (i == 0) 
		result.append(transliterate(ar[i],language));
	    if (i > 0 && i < ar.length - 1) 
		result.append(comma + " " + transliterate(ar[i],language));
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

        return word;
// 	if (word.charAt(0) != '?') 
// 	    return word;
// 	else
// 	    if (language.equals("ar")) {
// 		StringBuffer result = new StringBuffer();
// 		result.append(getKeyword("?","ar"));
// 		for (int i = 1; i < word.length(); i++) {
// 		    switch (word.charAt(i)) {
//                     case 'A': result.append("&#x0627;&#x0654;"); break;
//                     case 'B': result.append("&#xFE8F;"); break;
//                     case 'C': result.append("&#xFED9;"); break;
//                     case 'D': result.append("&#xFEA9;"); break;
//                     case 'E': result.append("&#x0650;"); break;
//                     case 'F': result.append("&#xFED1;"); break;
//                     case 'G': result.append("&#xFE9D;"); break;
//                     case 'H': result.append("&#xFEE9;"); break;
//                     case 'I': result.append("&#x0650;"); break;
//                     case 'J': result.append("&#xFE9D;"); break;
//                     case 'K': result.append("&#xFED9;"); break;
//                     case 'L': result.append("&#xFEDD;"); break;
//                     case 'M': result.append("&#xFEE1;"); break;
//                     case 'N': result.append("&#xFEE5;"); break;
//                     case 'O': result.append("&#x064F;"); break;
//                     case 'P': result.append("&#xFE8F;"); break;
//                     case 'Q': result.append("&#xFED5;"); break;
//                     case 'R': result.append("&#xFEAD;"); break;
//                     case 'S': result.append("&#xFEB1;"); break;
//                     case 'T': result.append("&#xFE95;"); break;
//                     case 'U': result.append("&#x064F;"); break;
//                     case 'V': result.append("&#xFE8F;"); break;
//                     case 'W': result.append("&#xFEED;"); break;
//                     case 'X': result.append("&#xFEAF;"); break;
//                     case 'Y': result.append("&#xFEF1;"); break;
//                     case 'Z': result.append("&#xFEAF;"); 
// 			result.append(word.charAt(i));
// 		    }
// 		}
// 		return result.toString();
// 	    }
// 	    else
// 		return word;
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

        // System.out.println("INFO in LanguageFormatter.readKeywordMap()");
        // System.out.println("  dir == " + dir);
	if (keywordMap == null) 
            keywordMap = new HashMap();
	if (keywordMap.isEmpty()) {

	    System.out.println( "INFO in LanguageFormatter.readKeywordMap(): filling keywordMap" );

	    String fname = null;
	    String line;
	    HashMap newLine;
	    ArrayList languageKeyArray = new ArrayList();
	    String key;
	    int i;
	    int count;
	    BufferedReader br;
	    try {
		File dirFile = new File( dir );
		File file = new File( dirFile, "language.txt" );
		fname = file.getCanonicalPath();
		br = new BufferedReader(new InputStreamReader(new FileInputStream(fname),"UTF-8"));
	    }
	    catch (IOException ioe) {
		System.out.println("Error in LanguageFormatter.readKeywordMap(): Error opening file " + fname);
		// System.out.println(System.getProperty("user.dir"));
		return keywordMap;
	    }

	    try {
		br = new BufferedReader(new InputStreamReader(new FileInputStream(fname),"UTF-8"));
		// lnr = new LineNumberReader(new FileReader(fname));
		do {
		    line = br.readLine();
		    if (line != null) {
			if (line.startsWith("EnglishLanguage|")) { // The language key line.
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
	    }
	}
	return (keywordMap);
    }
  
    /** ***************************************************************
     * 
     */
    private static String processAtom(String atom, Map termMap, String language) {

	if (atom.charAt(0) == '?') 
	    return transliterate(atom,language);
	if (termMap.containsKey(atom)) 
	    return (String) termMap.get(atom);
	return atom;
    }

    /** ***************************************************************
     * For debugging ...
     */
    private static void printSpaces( int depth ) {
	for (int i = 0 ; i <= depth ; i++) 
	    System.out.print( "  " );	
	System.out.print( depth + ":" );
	return;
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
    public static String nlStmtPara(String stmt, boolean isNegMode, Map phraseMap, 
    				    Map termMap, String language, int depth) {

	/*
	System.out.println( "INFO in LanguageFormatter.nlStmtPara( " + depth + " ):" );
	printSpaces( depth );
	System.out.println( "stmt == " + stmt );
	*/

	if (stmt == null || stmt.length() < 1) {
	    System.out.println("Error in LanguageFormatter.nlStmtPara(): stmt is empty");
	    /*
	    System.out.println( "INFO in LanguageFormatter.nlStmtPara( " + depth + " ):" );
	    printSpaces( depth );
	    System.out.println( "a:return == \"\"" );
	    */
	    return "";
	}
	if ( (phraseMap == null) || phraseMap.isEmpty() ) {
	    /*
	    System.out.println( "INFO in LanguageFormatter.nlStmtPara( " + depth + " ):" );
	    printSpaces( depth );
	    System.out.println( "b:return == \"\"" );
	    */
	    return "";
	}
	if ( (termMap == null) || termMap.isEmpty() ) {
	    /*
	    System.out.println( "INFO in LanguageFormatter.nlStmtPara( " + depth + " ):" );
	    printSpaces( depth );
	    System.out.println( "c:return == \"\"" );
	    */
	    return "";
	}
	StringBuffer result = new StringBuffer();
	String ans = null;
	Formula f = new Formula();
	f.read(stmt);
	if (f.atom()) {
	    ans = processAtom(stmt,termMap,language);
	    /*
	    System.out.println( "INFO in LanguageFormatter.nlStmtPara( " + depth + " ):" );
	    printSpaces( depth );
	    System.out.println( "d:return == " + ans );
	    */
	    return ans;
	}
	else {
	    if (!f.listP()) {
		System.out.println("Error in LanguageFormatter.nlStmtPara(): Statement is not an atom or a list: " + stmt);
		/*
		System.out.println( "INFO in LanguageFormatter.nlStmtPara( " + depth + " ):" );
		printSpaces( depth );
		System.out.println( "e:return == \"\"" );
		*/
		return "";
	    }
	}
	/*
	  if (phraseMap == null) {
          System.out.println("Error in LanguageFormatter.nlStmtPara(): phrase map is null.");
          phraseMap = new HashMap();
	  }
	*/
	String pred = f.car();
	if (!Formula.atom(pred)) {
	    System.out.println("Error in LanguageFormatter.nlStmtPara(): statement: " + stmt + " has a formula in the predicate position."); 
	    /*
	    System.out.println( "INFO in LanguageFormatter.nlStmtPara( " + depth + " ):" );
	    printSpaces( depth );
	    System.out.println( "f:return == " + stmt );
	    */
	    return stmt;
	}
	if (logicalOperator(pred)) {
	    ans = paraphraseLogicalOperator(stmt,isNegMode,phraseMap,termMap,language,depth+1);
	    /*
	    System.out.println( "INFO in LanguageFormatter.nlStmtPara( " + depth + " ):" );
	    printSpaces( depth );
	    System.out.println( "g:return == " + ans );
	    */
	    return ans;
	}
	if (phraseMap.containsKey(pred)) {
	    ans = paraphraseWithFormat(stmt,isNegMode,phraseMap,termMap,language);
	    /*
	    System.out.println( "INFO in LanguageFormatter.nlStmtPara( " + depth + " ):" );
	    printSpaces( depth );
	    System.out.println( "h:return == " + ans );
	    */
	    return ans;
	}
	else {  
	    // predicate has no paraphrase
	    if (pred.charAt(0) == '?') {
		result.append(transliterate(pred,language));
	    }
	    else {
		if (termMap.containsKey(pred)) {
		    result.append((String) termMap.get(pred));
		}
		else {
		    result.append(pred);
		}
	    }
	    f.read(f.cdr());
	    while (!f.empty()) {
		String arg = f.car();
		f.read(f.cdr());
		if (!Formula.atom(arg)) {
		    result.append(" " + nlStmtPara(arg,isNegMode,phraseMap,termMap,language,depth+1));
		}
		else {
		    result.append(" " + translateWord(termMap,arg,language));
		}
	    }
	}
	ans = result.toString();
	/*
	System.out.println( "INFO in LanguageFormatter.nlStmtPara( " + depth + " ):" );
	printSpaces( depth );
	System.out.println( "i:return == " + ans );
	*/
	return ans;
    }

    /** ***************************************************************
     * Return the NL format of an individual word.
     */
    private static String translateWord(Map termMap, String word, String language) {

        String ans = word;
        try {
            if (termMap !=null && termMap.containsKey(word))
                ans = ((String) termMap.get(word));
            else if (word.charAt(0) == '?') 
		ans = transliterate(word,language);
//             if (language.equalsIgnoreCase("ar") && !ans.startsWith("?") &&
//                 ans.indexOf("&#x6") == -1) {
//                     // left-to-right embedding
//                     ans = ("&#x202a;" + ans + "&#x202c;");
//             }
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
        return ans;
    }
  
    /** ***************************************************************
     * Create a natural language paraphrase for statements involving the logical operators.
     *
     * @param pred the logical predicate in the expression
     * @param isNegMode is the expression negated?
     * @param words  the expression as an ArrayList of tokens
     * @return the natural language paraphrase as a String, or null if the predicate was not a logical operator.
     */
    private static String paraphraseLogicalOperator( String stmt, 
						     boolean isNegMode, 
						     Map phraseMap, 
						     Map termMap, 
						     String language,
						     int depth ) {

	//System.out.println( "INFO in LanguageFormatter.paraphraseLogicalOperator(): stmt == " + stmt);
	/*
	System.out.println( "INFO in LanguageFormatter.paraphraseLogicalOperator( " + depth + " ):" );
	printSpaces( depth );
	System.out.println( "stmt == " + stmt);
	*/

	if (keywordMap == null) {
	    System.out.println("Error in LanguageFormatter.paraphraseLogicalOperator(): keywordMap is null");
	    /*
	    System.out.println( "INFO in LanguageFormatter.paraphraseLogicalOperator( " + depth + " ):" );
	    printSpaces( depth );
	    System.out.println( "a:return == null" );
	    */
	    return null;
	}
	ArrayList args = new ArrayList();
	Formula f = new Formula();
	f.read(stmt);
	String pred = f.getArgument(0);
	f.read(f.cdr());

	String ans = null;
	if (pred.equals("not")) {
	    ans = nlStmtPara(f.car(),true,phraseMap,termMap,language,depth+1);
	    /*
	    System.out.println( "INFO in LanguageFormatter.paraphraseLogicalOperator( " + depth + " ):" );
	    printSpaces( depth );
	    System.out.println( "b:return == " + ans );
	    */
	    return ans;
	}

	while (!f.empty()) {
	    String arg = f.car();
	    String result = nlStmtPara(arg,false,phraseMap,termMap,language,depth+1);

	    if (result != null && result != "" && result.length() > 0) {
		args.add(result);
	    }
	    else {
		System.out.println("INFO in LanguageFormatter.paraphraseLogicalOperators(): bad result for: " + arg);
		arg = " ";
	    }

	    // System.out.println("INFO in LanguageFormatter.paraphraseLogicalOperators(): adding argument: " + ((String) args.get(args.size()-1)));
	    f.read(f.cdr());
	}
        String COMMA = getKeyword(",",language);
        String QUESTION = getKeyword("?",language);
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
	String SUCHTHAT = getKeyword("such that",language);
	if ( ! Formula.isNonEmptyString(SUCHTHAT) ) { SUCHTHAT = SOTHAT; }

	StringBuffer sb = new StringBuffer();

	if (pred.equalsIgnoreCase("=>")) {
	    if ( isNegMode ) {
		sb.append(args.get(1)).append(" "+AND+" ").append("~{").append(args.get(0)).append("}");
	    }
	    else {
		sb.append("<ul><li>" 
                          + (language.equalsIgnoreCase("ar") ? "&#x202b;" : "")
                          + IF
                          + " ").append(args.get(0)).append(COMMA
                                                            + (language.equalsIgnoreCase("ar") ? "&#x202c;" : "")
                                                            + "</li><li>"
                                                            + (language.equalsIgnoreCase("ar") ? "&#x202b;" : "")
                                                            + THEN
                                                            + " ").append(args.get(1)).append((language.equalsIgnoreCase("ar") ? "&#x202c;" : "")
                                                                                              + "</li></ul>");
	    }
	    ans = sb.toString();
	    /*
	    System.out.println( "INFO in LanguageFormatter.paraphraseLogicalOperator( " + depth + " ):" );
	    printSpaces( depth );
	    System.out.println( "c:return == " + ans );
	    */
	    return ans;
	}
	if (pred.equalsIgnoreCase("and")) {
	    if ( isNegMode ) {
		for (int i = 0; i < args.size(); i++) {
		    if (i != 0) { sb.append(" "+OR+" "); }
		    sb.append("~{ ");
		    sb.append(translateWord(termMap,(String) args.get(i),language));
		    sb.append(" }");
		}
	    }
	    else {
		for (int i = 0; i < args.size(); i++) {
		    if (i != 0) { sb.append(" "+AND+" "); }
		    sb.append(translateWord(termMap,(String) args.get(i),language));
		}
	    }
	    ans = sb.toString();
	    /*
	    System.out.println( "INFO in LanguageFormatter.paraphraseLogicalOperator( " + depth + " ):" );
	    printSpaces( depth );
	    System.out.println( "d:return == " + ans );
	    */
	    return ans;
	}
	if (pred.equalsIgnoreCase("holds")) {

	    for (int i = 0; i < args.size(); i++) {
		if (i != 0) {
		    if ( isNegMode ) { sb.append(" "+NOT); }
		    sb.append(" "+HOLDS+" ");
		}
		sb.append(translateWord(termMap,(String) args.get(i),language));
	    }

	    ans = sb.toString();
	    /*
	    System.out.println( "INFO in LanguageFormatter.paraphraseLogicalOperator( " + depth + " ):" );
	    printSpaces( depth );
	    System.out.println( "e:return == " + ans );
	    */
	    return ans;
	}
	if (pred.equalsIgnoreCase("or")) {
	    for (int i = 0; i < args.size(); i++) {
		if (i != 0) { 
		    if ( isNegMode ) { sb.append(" "+AND+" "); }
		    else { sb.append(" "+OR+" "); }
		}
		sb.append(translateWord(termMap,(String) args.get(i),language));
	    }
	    ans = sb.toString();
	    /*
	    System.out.println( "INFO in LanguageFormatter.paraphraseLogicalOperator( " + depth + " ):" );
	    printSpaces( depth );
	    System.out.println( "f:return == " + ans );
	    */
	    return ans;
	}
	if (pred.equalsIgnoreCase("<=>")) {
	    if ( isNegMode ) {
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
	    else {
		sb.append(translateWord(termMap,(String) args.get(0),language));
		sb.append(" "+IFANDONLYIF+" ");
		sb.append(translateWord(termMap,(String) args.get(1),language));
	    }
	    ans = sb.toString();
	    /*
	    System.out.println( "INFO in LanguageFormatter.paraphraseLogicalOperator( " + depth + " ):" );
	    printSpaces( depth );
	    System.out.println( "g:return == " + ans );
	    */
	    return ans;
	}
	if (pred.equalsIgnoreCase("forall")) {
	    if ( isNegMode ) { sb.append(" "+NOT+" "); }
	    sb.append(FORALL+" ");
	    if (((String) args.get(0)).indexOf(" ") == -1) {
		// If just one variable ...
		sb.append(translateWord(termMap,(String) args.get(0),language));
	    }
	    else {
		// If more than one variable ...
		sb.append(translateWord(termMap,formatList((String) args.get(0),language),language));
	    }
	    sb.append( " " );
	    // sb.append(" "+HOLDS+": ");
	    sb.append(translateWord(termMap,(String) args.get(1),language));
	    ans = sb.toString();
	    /*
	    System.out.println( "INFO in LanguageFormatter.paraphraseLogicalOperator( " + depth + " ):" );
	    printSpaces( depth );
	    System.out.println( "h:return == " + ans );
	    */
	    return ans;
	}
	if (pred.equalsIgnoreCase("exists")) {
	    if (((String) args.get(0)).indexOf(" ") == -1) {

		// If just one variable ...

		// NS: The section immediately below seems to be just
		// wrong, so I've commented it out.
		/*
		  if (args.size() != 3) {
                  for (int i = args.size()-1; i >= 0; i--) {
		  sb.append(translateWord(termMap,(String) args.get(i),language));
		  sb.append(" ");
                  }
		  ans = sb.toString();
		  System.out.println( "INFO in LanguageFormatter.paraphraseLogicalOperator( " + depth + " ):" );
		  printSpaces( depth );
		  System.out.println( "i:return == " + ans );
		  return ans;  // not the right english format
		  }
		*/
		if ( isNegMode ) { sb.append(NOTEXISTS+" "); }
		else { sb.append(EXISTS+" "); }                 
		sb.append(translateWord(termMap,(String) args.get(0),language));
	    }
	    else {

		// If more than one variable ...

		if ( isNegMode ) { sb.append(NOTEXIST+" "); }
		else { sb.append(EXIST+" "); }
		sb.append(translateWord(termMap,formatList((String) args.get(0),language),language));
	    }
	    sb.append(" "+SUCHTHAT+" ");
	    sb.append(translateWord(termMap,(String) args.get(1),language));
	    ans = sb.toString();
	    /*
	      System.out.println( "INFO in LanguageFormatter.paraphraseLogicalOperator( " + depth + " ):" );
	      printSpaces( depth );
	      System.out.println( "j:return == " + ans );
	    */
	    return ans;
	}       
	/*
	System.out.println( "INFO in LanguageFormatter.paraphraseLogicalOperator( " + depth + " ):" );
	printSpaces( depth );
	System.out.println( "l:return == \"\"" );
	*/
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

	// System.out.println("INFO in LanguageFormatter.paraphraseWithFormat(): Statement: " + stmt);
	// System.out.println("neg mode: " + isNegMode);
	Formula f = new Formula();
	f.read(stmt);
	String pred = f.car();
	String strFormat = (String) phraseMap.get(pred);
	// System.out.println("str format: " + strFormat);
	int index;

	if (strFormat.indexOf("&%") > -1) {                   // setup the term hyperlink
	    strFormat.replace("&%","&%"+pred+"$");
	}
	if ( isNegMode ) {                                    // handle negation
	    if (strFormat.indexOf("%n") == -1) {
		strFormat = getKeyword("not",language) + " " + strFormat;
	    }
	    else {
		if (strFormat.indexOf("%n{") == -1) {
		    strFormat = strFormat.replace("%n",getKeyword("not",language));
		}
		else {
		    int start = strFormat.indexOf("%n{") + 3;
		    int end = strFormat.indexOf("}",start);
		    strFormat = ( strFormat.substring(0,start-3) 
				  + strFormat.substring(start,end) 
				  + strFormat.substring(end+1,strFormat.length()) );
		}
	    }
	    // delete all the unused positive commands
	    isNegMode = false;
	    // strFormat = strFormat.replace("%p ","");
	    // strFormat = strFormat.replaceAll(" %p\\{[\\w\\']+\\} "," ");
	    // strFormat = strFormat.replaceAll("%p\\{[\\w\\']+\\} "," ");
	    strFormat = strFormat.replaceAll(" %p\\{.+?\\} "," ");
	    strFormat = strFormat.replaceAll("%p\\{.+?\\} "," ");
	}
	else {                                      
	    // delete all the unused negative commands          
	    strFormat = strFormat.replace(" %n "," ");
	    strFormat = strFormat.replace("%n "," ");
	    // strFormat = strFormat.replaceAll(" %n\\{[\\w\\']+\\} "," ");
	    // strFormat = strFormat.replaceAll("%n\\{[\\w\\']+\\} "," ");
	    strFormat = strFormat.replaceAll(" %n\\{.+?\\} "," ");
	    strFormat = strFormat.replaceAll("%n\\{.+?\\} "," ");
	    if (strFormat.indexOf("%p{") != -1) {           
		int start = strFormat.indexOf("%p{") + 3;
		int end = strFormat.indexOf("}",start);
		strFormat = ( strFormat.substring(0,start-3) 
			      + strFormat.substring(start,end) 
			      + strFormat.substring(end+1,strFormat.length()) );
	    }
	}

	strFormat = expandStar( f, strFormat, language );

	int num = 1;                                          // handle arguments
	String argPointer = "%" + (new Integer(num)).toString();
	while (strFormat.indexOf(argPointer) > -1) {
	    // System.out.println("INFO in LanguageFormatter.paraphraseWithFormat(): Statement: " + f.theFormula);
	    // System.out.println("arg: " + f.getArgument(num));
	    // System.out.println("num: " + num);
	    // System.out.println("str: " + strFormat);
            
	    strFormat = strFormat.replace(argPointer,nlStmtPara(f.getArgument((int) num),isNegMode,phraseMap,termMap,language,1));
	    num++;
	    argPointer = "%" + (new Integer(num)).toString();
	}
      
	// System.out.println("str: " + strFormat);
	return strFormat.toString();
    }

    /** ***************************************************************
     * This method expands all "star" (asterisk) directives in the input
     * format string, and returns a new format string with individually
     * numbered argument pointers.
     *
     * @param f The Formula being paraphrased.
     *
     * @param strFormat The format string that contains the patterns and
     * directives for paraphrasing f.
     *
     * @param lang A two-character string indicating the language into
     * which f should be paraphrased.
     *
     * @return A format string with all relevant argument pointers
     * expanded.
     */
    private static String expandStar(Formula f, String strFormat, String lang) {

	String result = strFormat;
	ArrayList problems = new ArrayList();
	try {
	    int flen = f.listLength();
	    if ( Formula.isNonEmptyString(strFormat) && (flen > 1) ) {
		int p1 = 0;
		int p2 = strFormat.indexOf("%*");
		if ( p2 != -1 ) {
		    int slen = strFormat.length();
		    String lb = null;
		    String rb = null;
		    int lbi = -1;
		    int rbi = -1;
		    String ss = null;
		    String range = null;
		    String[] rangeArr = null;
		    String[] rangeArr2 = null;
		    String lowStr = null;
		    String highStr = null;
		    int low = -1;
		    int high = -1;
		    String delim = " ";
		    boolean isRange = false;
		    boolean[] argsToPrint = new boolean[ flen ];
		    int nArgsSet = -1;
		    StringBuffer sb = new StringBuffer();
		    while ( (p1 < slen) && (p2 >= 0) && (p2 < slen) ) {
			sb.append( strFormat.substring( p1, p2 ) );
			p1 = ( p2 + 2 );
			for ( int k = 0 ; k < argsToPrint.length ; k++ ) {
			    argsToPrint[k] = false;
			}
			lowStr = null;
			highStr = null;
			low = -1;
			high = -1;
			delim = " ";
			nArgsSet = 0;
			lb = null;
			lbi = p1;
			if ( lbi < slen ) { lb = strFormat.substring( lbi, (lbi + 1) ); }
			while ( (lb != null) && (lb.equals("{") || lb.equals("[")) ) {
			    rb = "]";
			    if ( lb.equals("{") ) { rb = "}"; }
			    rbi = strFormat.indexOf( rb, lbi );
			    if ( rbi == -1 ) { 
				problems.add( "Error in format \"" + strFormat + "\": missing \"" + rb + "\"" );
				break; 
			    }
			    p1 = ( rbi + 1 );
			    ss = strFormat.substring( (lbi + 1), rbi );
			    if ( lb.equals("{") ) { 
				range = ss.trim();
				rangeArr = range.split( "," );
				// System.out.println( "INFO in LanguageFormatter.expandStar(): rangeArr == " + rangeArr );
				for ( int i = 0 ; i < rangeArr.length ; i++ ) {
				    if ( Formula.isNonEmptyString(rangeArr[i]) ) {
					isRange = ( rangeArr[i].indexOf( "-" ) != -1 );
					rangeArr2 = rangeArr[i].split( "-" );
					lowStr = rangeArr2[0].trim();
					try { 
					    low = Integer.parseInt( lowStr );
					}
					catch ( Exception e1 ) {
					    problems.add( "Error in format \"" + strFormat + "\": bad value in \"" + ss + "\"" );
					    low = 1;
					}
					// System.out.println( "INFO in LanguageFormatter.expandStar(): low == " + low );
					high = low;
					if ( isRange ) {
					    if ( rangeArr2.length == 2 ) {
						highStr = rangeArr2[1].trim();
						try {
						    high = Integer.parseInt( highStr );
						}
						catch ( Exception e2 ) {
						    problems.add( "Error in format \"" + strFormat + "\": bad value in \"" + ss + "\"" );
						    high = ( flen - 1 );
						}
					    }
					    else {
						high = ( flen - 1 );
					    }
					}
					// System.out.println( "INFO in LanguageFormatter.expandStar(): high == " + high );
					for ( int j = low ; (j <= high) && (j < argsToPrint.length) ; j++ ) {
					    argsToPrint[j] = true;
					    nArgsSet++;
					}
				    }
				}
			    }
			    else { 
				delim = ss; 
			    }
			    lb = null;
			    lbi = p1;
			    if ( lbi < slen ) { lb = strFormat.substring( lbi, (lbi + 1) ); }
			}
			String AND = getKeyword("and",lang);
			if ( ! Formula.isNonEmptyString(AND) ) {
			    AND = "+";
			}
			int nAdded = 0;
			boolean addAll = ( nArgsSet == 0 );
			int nToAdd = ( addAll ? (argsToPrint.length - 1) : nArgsSet );
			for ( int i = 1 ; i < argsToPrint.length ; i++ ) {
			    if ( addAll || (argsToPrint[i] == true) ) {
				if ( nAdded >= 1 ) {
				    if ( nToAdd == 2 ) { sb.append( " " + AND + " " ); }
				    else { sb.append( delim ); }
				    if ( (nToAdd > 2) && ((nAdded + 1) == nToAdd) ) {
					sb.append( AND + " " );
				    }
				}
				sb.append( "%" + i );
				nAdded++;
			    }
			}
			if ( p1 < slen ) {
			    p2 = strFormat.indexOf( "%*", p1 );
			    if ( p2 == -1 ) {
				sb.append( strFormat.substring( p1, slen ) );
				break;
			    }
			}
		    }
		    if ( sb.length() > 0 ) {
			result = sb.toString();
		    }
		}
	    }				
	}
	catch ( Exception ex ) {
	    ex.printStackTrace();
	}
	if ( ! problems.isEmpty() ) {
	    String errStr = KBmanager.getMgr().getError();
	    String str = null;
	    if ( errStr == null ) { errStr = ""; }
	    Iterator it = problems.iterator();
	    while ( it.hasNext() ) {
		str = (String) it.next();
		System.out.println( "Error in LanguageFormatter.expandStar(): " );
		System.out.println( "  " + str );
		errStr += ( "\n<br/>" + str + "\n<br/>" );
	    }
	    KBmanager.getMgr().setError( errStr );
	}
	return result;
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
    public static String htmlParaphrase(String href,String stmt, Map phraseMap, Map termMap, KB kb, String language) {

	int end;
	int start = -1;
        Formula f = new Formula();
        f.read(stmt);
        // System.out.println("Formula: " + f.theFormula);
        HashMap varMap = f.computeVariableTypes(kb);
	String nlFormat = nlStmtPara(stmt,false,phraseMap,termMap,language,1);
        String charsAllowed = "&#;";
	if (nlFormat != null) {
	    while (nlFormat.indexOf("&%") > -1) {
		start = nlFormat.indexOf("&%",start+1);
		int word = nlFormat.indexOf("$",start);
		if (word == -1)
		    end = start + 2;
		else
		    end = word + 1;
		while (end < nlFormat.length() && (Character.isJavaIdentifierPart(nlFormat.charAt(end))
                                                   || charsAllowed.indexOf(nlFormat.charAt(end)) != -1))
		    end++;		
		if (word == -1)
		    nlFormat = (nlFormat.substring(0,start) + "<a href=\"" + href + "&term=" 
				 + nlFormat.substring(start+2,end) + "\">" + nlFormat.substring(start+1,end) 
				 + "</a>" + nlFormat.substring(end, nlFormat.length()) );		
		else 
		    nlFormat = (nlFormat.substring(0,start) + "<a href=\"" + href + "&term=" 
				 + nlFormat.substring(start+2,word) + "\">" + nlFormat.substring(word+1,end) 
				 + "</a>" + nlFormat.substring(end, nlFormat.length()) );		
	    }
            if (varMap != null && varMap.keySet().size() > 0) 
                nlFormat = variableReplace(nlFormat,varMap,kb,language);
	}
	else  
	    nlFormat = "";
//         if (Formula.isNonEmptyString(nlFormat)) {
//             if (language.equalsIgnoreCase("ar")) {
//                 nlFormat = ("&#x202b;" + nlFormat + "&#x202c;");
//             }
//         }
	return nlFormat;
    }

    /** **************************************************************
     * Generate a linguistic article appropriate to how many times in a
     * paraphrase a particular type has already occurred.
     */
    public static String getArticle(String s, int count, int occurrence, String language) {

        String ordinal = "";
        switch (occurrence) {
          case 3: ordinal = getKeyword("third",language); break;
          case 4: ordinal = getKeyword("fourth",language); break;
          case 5: ordinal = getKeyword("fifth",language); break;
          case 6: ordinal = getKeyword("sixth",language); break;
          case 7: ordinal = getKeyword("seventh",language); break;
          case 8: ordinal = getKeyword("eighth",language); break;
          case 9: ordinal = getKeyword("ninth",language); break;
          case 10: ordinal = getKeyword("tenth",language); break;
          case 11: ordinal = getKeyword("eleventh",language); break;
          case 12: ordinal = getKeyword("twelfth",language); break;
        }
        if (count == 1 && occurrence == 2)
            return getKeyword("another",language);        
        if (count > 1) {
            if (occurrence == 1 || occurrence > 2) {
                if (language.equalsIgnoreCase("ar")) {
                    return ordinal;
                }
                else {
                    return (getKeyword("the",language) + " " + ordinal);
                }
            }
            else {
                if (language.equalsIgnoreCase("ar")) {
                    return (getKeyword("the",language) + getKeyword("other",language));
                }
                else {
                    return (getKeyword("the",language) + " " + getKeyword("other",language));
                }
            }
        }
        // count = 1 (first occurrence of a type)
        if (language.equalsIgnoreCase("EnglishLanguage")) {
            if ((s.charAt(0) == 'A' || s.charAt(0) == 'a' || 
                 s.charAt(0) == 'E' || s.charAt(0) == 'e' ||
                 s.charAt(0) == 'I' || s.charAt(0) == 'i' ||
                 s.charAt(0) == 'O' || s.charAt(0) == 'o' ||
                 s.charAt(0) == 'U' || s.charAt(0) == 'u') &&
                occurrence == 1)
                return "an ";
            else
                return "a " + ordinal;
        }
        else if (language.equalsIgnoreCase("ar")) {
            String defArt = getKeyword("the",language);
            if (ordinal.startsWith(defArt)) {
                // remove the definite article
                ordinal = ordinal.substring(defArt.length());
                // remove shadda
                ordinal = ordinal.replace("&#x651;","");
            }
            return ordinal;
        }
        else {
            return ordinal;
        }
    }

    /** **************************************************************
     * Replace variables with types, and articles appropriate to how many times
     * they have occurred.
     */
    private static String incrementalVarReplace(String form, String varString, String varType,
                                                String varPretty, String language, 
                                                boolean isClass, HashMap typeMap) {

        String result = new String(form);
        if (!Formula.isNonEmptyString(varPretty)) {
            varPretty = varType;
        }
        boolean found = true;
        int occurrenceCounter = 1;
        if (typeMap.keySet().contains(varType)) {
            occurrenceCounter = (Integer) typeMap.get(varType);
            occurrenceCounter++;
            typeMap.put(varType,new Integer(occurrenceCounter));
        }
        else
            typeMap.put(varType,new Integer(1));
        int count = 1;
        while (found) {
            if (result.indexOf(varString) > -1 && count < 20) {
                String article = "";
                String replacement = "";
                if (isClass) {
                    article = getArticle("kind",count,occurrenceCounter,language);
                    replacement = (article
                                   + " "
                                   + getKeyword("kind of",language)
                                   + " "
                                   + varPretty);
                    if (language.equalsIgnoreCase("ar")) {
                        replacement = (getKeyword("kind of",language) + " " + varPretty);
                    }
                    result = result.replaceFirst("\\?" + varString.substring(1), replacement);
                }
                else {
                    article = getArticle(varPretty,count,occurrenceCounter,language);
                    replacement = (article + " " + varPretty);
                    if (language.equalsIgnoreCase("ar")) {
                        String defArt = getKeyword("the",language);
                        if (article.startsWith(defArt) && !varPretty.startsWith(defArt)) {
                            // This has to be refined to insert shadda for sun letters.
                            varPretty = (defArt + varPretty);
                        }
                        replacement = (varPretty + " " + article);
                    }
                    result = result.replaceFirst("\\?" + varString.substring(1), replacement);
                }
            }
            else
                found = false;
            count++;
        }
        return result;
    }

    /** **************************************************************
     * Collect all the variables occurring in a formula in order.  Return
     * an ArrayList of Strings.
     */
    private static ArrayList collectOrderedVariables(String form) {

        boolean inString = false;
        boolean inVar = false;
        String var = "";
        ArrayList result = new ArrayList();
        for (int i = 0; i < form.length(); i++) {
            char ch = form.charAt(i);
            switch (ch) {
                case '"': inString = !inString; break;
                case '?': if (!inString) inVar = true; break;
            }
            if (inVar && !Character.isLetterOrDigit(ch) && ch != '?') {
                if (!result.contains(var)) 
                    result.add(var);                
                inVar = false;
                var = "";
            }
            if (inVar) 
                var = var + ch;
        }
        return result;
    }

    /** **************************************************************
     * Replace variables in a formula with paraphrases expressing their
     * type.
     */
    public static String variableReplace(String form, HashMap varMap, KB kb, String language) {

        String result = form;
        HashMap typeMap = new HashMap();
        ArrayList varList = collectOrderedVariables(form);
        Iterator it = varList.iterator();
        while (it.hasNext()) {
            String varString = (String) it.next();
            if (!DB.emptyString(varString)) {
                ArrayList outerArray = (ArrayList) varMap.get(varString);
                if (outerArray != null && outerArray.size() > 0) {
                    ArrayList instanceArray = (ArrayList) outerArray.get(0);
                    ArrayList subclassArray = (ArrayList) outerArray.get(1);
                    if (subclassArray.size() > 0) {
                        String varType = (String) subclassArray.get(0);
                        String varPretty = (String) kb.getTermFormatMap(language).get(varType);
                        if (Formula.isNonEmptyString(varPretty))
                            result = incrementalVarReplace(result,varString,varType,varPretty,language,true,typeMap);
                        else
                            result = incrementalVarReplace(result,varString,varType,varType,language,true,typeMap);
                    }
                    else {
                        if (instanceArray.size() > 0) {
                            String varType = (String) instanceArray.get(0);
                            String varPretty = (String) kb.getTermFormatMap(language).get(varType);
                            if (Formula.isNonEmptyString(varPretty))
                                result = incrementalVarReplace(result,varString,varType,varPretty,language,false,typeMap);
                            else
                                result = incrementalVarReplace(result,varString,varType,varType,language,false,typeMap);
                        }
                        else
                            result = incrementalVarReplace(result,varString,"Entity","entity",language,false,typeMap);
                    }
                }
                else
                    System.out.println("Error in LanguageFormatter.variableReplace(): varString : " + varString + " formula: " + form);
            }
        }
        return result;
    }

    /** **************************************************************
     */
    public static void main(String[] args) {

        try {
            KBmanager.getMgr().initializeOnce();
        } catch (IOException ioe ) {
            System.out.println(ioe.getMessage());
        }
        KB kb = KBmanager.getMgr().getKB("SUMO");

        // String stmt = "(<=> (instance ?PHYS Physical) (exists (?LOC ?TIME) (and (located ?PHYS ?LOC) (time ?PHYS ?TIME))))";
        String stmt = "(=> (and (instance ?OBJ1 Object) (partlyLocated ?OBJ1 ?OBJ2)) (exists (?SUB) (and (part ?SUB ?OBJ1) (located ?SUB ?OBJ2))))";
        //collectOrderedVariables(stmt);
        Formula f = new Formula();
        f.read(stmt);
        System.out.println("Formula: " + f.theFormula);
        System.out.println("result: " + htmlParaphrase("",stmt, kb.getFormatMap("EnglishLanguage"), 
                            kb.getTermFormatMap("EnglishLanguage"), kb,"EnglishLanguage"));
    }
}

