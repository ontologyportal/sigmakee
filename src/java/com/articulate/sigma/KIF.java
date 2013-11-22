/** This code is copyright Articulate Software (c) 2003.  Some portions
copyright Teknowledge (c) 2003 and reused under the terms of the GNU license.
This software is released under the GNU Public License <http://www.gnu.org/copyleft/gpl.html>.
Users of this code also consent, by use of this code, to credit Articulate Software
and Teknowledge in any writings, briefings, publications, presentations, or 
other representations of any software which incorporates, builds on, or uses this 
code.  Please cite the following article in any publication with references:

Pease, A., (2003). The Sigma Ontology Development Environment, 
in Working Notes of the IJCAI-2003 Workshop on Ontology and Distributed Systems,
August 9, Acapulco, Mexico.  See also http://sigmakee.sourceforget.net
*/

package com.articulate.sigma;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.StreamTokenizer;
import java.io.StringReader;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.TreeSet;
import java.util.logging.Logger;

/** *****************************************************************
 * A class designed to read a file in SUO-KIF format into memory.
 * See <http://suo.ieee.org/suo-kif.html> for a language specification.
 * readFile() and writeFile() are the primary methods.
 * @author Adam Pease
 */
public class KIF {

    /** ***************************************************************
     * A numeric constant denoting normal parse mode, in which syntax
     * constraints are enforced.
     */
    public static final int NORMAL_PARSE_MODE = 1;
    public static int count = 0;
    /** ***************************************************************
     * A numeric constant denoting relaxed parse mode, in which fewer
     * syntax constraints are enforced than in NORMAL_PARSE_MODE.
     */
    public static final int RELAXED_PARSE_MODE = 2;

    private int parseMode = NORMAL_PARSE_MODE;

    /** The set of all terms in the knowledge base.  This is a set of Strings. */
    public TreeSet<String> terms = new TreeSet<String>();

    /** A HashMap of ArrayLists of Formulas.  @see KIF.createKey for key format. */
    public HashMap<String, ArrayList<Formula>> formulas = new HashMap<String, ArrayList<Formula>>();    

    /** A "raw" HashSet of unique Strings which are the formulas from the file without 
     *  any further processing, in the order which they appear in the file. */
    public LinkedHashSet<String> formulaSet = new LinkedHashSet<String>();

    private String filename;
    private File file;
    private int totalLinesForComments = 0;

    /** warnings generated during parsing */
    public TreeSet warningSet = new TreeSet();
    private static Logger logger = null;
    
    public KIF() {
    	if (logger == null)
    		logger = Logger.getLogger(this.getClass().getName());    	
    }
    
    /** ***************************************************************
     */
    public String getFilename() {
        return this.filename;
    }

    /** ***************************************************************
     */
    public void setFilename(String canonicalPath) {
        this.filename = canonicalPath;
        return;
    }

    /** ***************************************************************
     * @return int Returns an integer value denoting the current parse
     * mode.
     */
    public int getParseMode() {
        return this.parseMode;
    }

    /** ***************************************************************
     * Sets the current parse mode to the input value mode.     
     * @param mode An integer value denoting a parsing mode.     
     * @return void
     */
    public void setParseMode(int mode) {
        this.parseMode = mode;
    }

    /** ***************************************************************
     * This routine sets up the StreamTokenizer_s so that it parses SUO-KIF.
     * = < > are treated as word characters, as are normal alphanumerics.
     * ; is the line comment character and " is the quote character.
     */
    public static void setupStreamTokenizer(StreamTokenizer_s st) {

        st.whitespaceChars(0,32);
        st.ordinaryChars(33,44);   // !"#$%&'()*+,
        st.wordChars(45,46);       // -.
        st.ordinaryChar(47);       // /
        st.wordChars(48,58);       // 0-9:
        st.ordinaryChar(59);       // ;
        st.wordChars(60,64);       // <=>?@
        st.wordChars(65,90);       // A-Z
        st.ordinaryChars(91,94);   // [\]^
        st.wordChars(95,95);       // _
        st.ordinaryChar(96);       // `
        st.wordChars(97,122);      // a-z
        st.ordinaryChars(123,255); // {|}~
        // st.parseNumbers();
        st.quoteChar('"');
        st.commentChar(';');
        st.eolIsSignificant(true);
    }

    /** ***************************************************************
     */
    private void display(StreamTokenizer_s st, boolean inRule, boolean inAntecedent,
                         boolean inConsequent, int argumentNum, int parenLevel, String key) {

		StringBuilder result = new StringBuilder();
		result.append(inRule);
		result.append("\t");
		result.append(inAntecedent);
		result.append("\t");
		result.append(inConsequent);
		result.append("\t");
		result.append(st.ttype);
		result.append("\t");
		result.append(argumentNum);
		result.append("\t");
		result.append(parenLevel);
		result.append("\t");
		result.append(st.sval);
		result.append("\t");
		result.append(st.nval);
		result.append("\t");
		result.append(st.toString());
		result.append("\t");
		result.append(key);

		logger.fine(result.toString());
    }

    /** ***************************************************************
     *  This method has the side effect of setting the contents of
     *  formulaSet and formulas as it parses the file.  It throws a
     *  ParseException with file line numbers if fatal errors are
     *  encountered during parsing.
     *  @return a Set of warnings that may indicate syntax errors,
     *          but not fatal parse errors.
     */
    protected Set parse(Reader r) {

        //System.out.println("INFO in KIF.parse()");
    	logger.entering("KIF", "parse");    	
        int mode = this.getParseMode();                       
        logger.info("Parsing " + this.getFilename() + " with parseMode = " + ((mode == RELAXED_PARSE_MODE) ? "RELAXED_PARSE_MODE" : "NORMAL_PARSE_MODE"));  
        //System.out.println("Parsing " + this.getFilename() + " with parseMode = " + ((mode == RELAXED_PARSE_MODE) ? "RELAXED_PARSE_MODE" : "NORMAL_PARSE_MODE"));  

        String key = null;
        StringBuilder expression = new StringBuilder();
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
        String fstr = null;
        ArrayList list;
        String errStart = ("Parsing error in " + filename);
        String errStr = null;
        int duplicateCount = 0;
        
        if (r == null) {
            errStr = "No Input Reader Specified";
            logger.warning("No input string reader specified.");
            warningSet.add(errStr);
            System.err.println(errStr);
            System.out.println("EXIT KIF.parse(" + r + ")");
            logger.warning("Exiting KIF.parse without doing anything.");
            return warningSet;
        }
        try {
        	count++;
            st = new StreamTokenizer_s(r);
            KIF.setupStreamTokenizer(st);
            parenLevel = 0;
            inRule = false;
            argumentNum = -1;
            inAntecedent = false;
            inConsequent = false;
            Set<String> keySet = new HashSet<String>();
            lineStart = 0;
            isEOL = false;
            do {
                lastVal = st.ttype;
                st.nextToken();
                // check the situation when multiple KIF statements read as one
                // This relies on extra blank line to seperate KIF statements
                if (st.ttype == StreamTokenizer.TT_EOL) {
                    if (isEOL) { 
                        // two line seperators in a row, shows a new KIF
                        // statement is to start.  check if a new statement
                        // has already been generated, otherwise report
                        // error
                        if (!keySet.isEmpty() || (expression.length() > 0)) {
                            //System.out.print("INFO in KIF.parse(): Parsing Error:"); System.out.println(new Integer(lineStart + totalLinesForComments).toString());
                            errStr = (errStart + ": possible missed closing parenthesis near line " + f.startLine);
                            logger.warning(errStr);
                            logger.fine("st.sval=" + st.sval);
                            int elen = expression.length();
                            if (elen > 300)
                                logger.fine("expression == ... " + expression.substring(elen - 300));
                            else 
                                logger.fine("expression == " + expression.toString());
                            throw new ParseException(errStr, f.startLine);
                        }
                        continue;
                    }
                    else {    // Found a first end of line character.                                                                
                        isEOL = true;   // Turn on flag, to watch for a second consecutive one.                                                      
                        continue;
                    }
                }
                else if (isEOL) 
                    isEOL = false;                                                                               
                if (st.ttype==40) {    // Turn off isEOL if a non-space token encountered                                  
                    // open paren
                    if (parenLevel == 0) {
                        lineStart = st.lineno();
                        f = new Formula();
                        f.startLine = st.lineno() + totalLinesForComments;
                        f.sourceFile = filename;
                    }
                    parenLevel++;
                    if (inRule && !inAntecedent && !inConsequent)
                        inAntecedent = true;                    
                    else {
                        if (inRule && inAntecedent && (parenLevel == 2)) {
                            inAntecedent = false;
                            inConsequent = true;
                        }
                    }
                    if ((parenLevel != 0) && (lastVal != 40) && (expression.length() > 0))                          
                        expression.append(" ");   // add back whitespace that ST removes                 
                    expression.append("(");
                }
                else if (st.ttype==41) {
                    // )  - close paren
                    parenLevel--;
                    expression.append(")");
                    if (parenLevel == 0) {                                    
                        // The end of the statement...
                        fstr = StringUtil.normalizeSpaceChars(expression.toString());
                        f.theFormula = (StringUtil.replaceDateTime(fstr)).intern();
                        if (formulaSet.contains(f.theFormula)) {
                            String warning = ("Duplicate formula at line " + f.startLine + " of " + f.sourceFile + ": " + expression);
                            //lineStart + totalLinesForComments + expression;
							warningSet.add(warning);
							System.out.println(warning);
                            duplicateCount++;
                        }
                        // Check argument validity ONLY if we are in NORMAL_PARSE_MODE.
                        if (mode == NORMAL_PARSE_MODE) {
                            String validArgs = f.validArgs((file != null ? file.getName() : null), 
                                                           (file != null 
                                                            ? new Integer(f.startLine) 
                                                            : null));
                            if (StringUtil.emptyString(validArgs))
                                validArgs = f.badQuantification();                      
                            if (StringUtil.isNonEmptyString(validArgs)) {
                                errStr = (errStart + ": Invalid number of arguments near line " + f.startLine);
                                logger.warning(errStr);
                                logger.fine("st.sval = " + st.sval);
                                int elen = expression.length();
                                if (elen > 300)
                                    logger.fine("expression == ... " + expression.substring(elen - 300));
                                else 
                                    logger.fine("expression == " + expression.toString());
                                throw new ParseException(errStr, f.startLine);  
                            }
                        }
                        keySet.add(f.theFormula);           // Make the formula itself a key
                        keySet.add(f.createID());  
                        f.endLine = st.lineno() + totalLinesForComments;
                        for (String fkey : keySet) {   // Add the expression but ...                            
                            if (formulas.containsKey(fkey)) {
                                if (!formulaSet.contains(f.theFormula)) {  
                                    // don't add keys if formula is already present
                                    list = (ArrayList) formulas.get(fkey);
                                    if (!list.contains(f)) 
                                        list.add(f);
                                }
                            }
                            else {
                                list = new ArrayList();
                                list.add(f);
                                formulas.put(fkey,list);
                            }
                        }
                        formulaSet.add(f.theFormula);
                        inConsequent = false;
                        inRule = false;
                        argumentNum = -1;
                        lineStart = (st.lineno() + 1);    // start next statement from next line                     
                        expression = new StringBuilder();
                        keySet.clear();
                    }
                    else if (parenLevel < 0) {
                        errStr = (errStart + ": Extra closing parenthesis found near line " + f.startLine);
                        logger.warning(errStr);
                        logger.fine("st.sval = " + st.sval);
                        int elen = expression.length();
                        if (elen > 300)
                        	logger.fine("expression == ... " + expression.substring(elen - 300));
                        else 
                        	logger.fine("expression == " + expression.toString());
                        throw new ParseException(errStr, f.startLine);
                    }
                }
                else if (st.ttype==34) {                                      // " - it's a string                 
                    st.sval = StringUtil.escapeQuoteChars(st.sval);
                    if (lastVal != 40)           // add back whitespace that ST removes
                        expression.append(" ");
                    expression.append("\"");
                    com = st.sval;
                    totalLinesForComments += countChar(com,(char)0X0A);
                    expression.append(com);
                    expression.append("\"");
                    if (parenLevel < 2)   // Don't care if parenLevel > 1
                        argumentNum = argumentNum + 1;
                }
                else if ((st.ttype == StreamTokenizer.TT_NUMBER) ||           // number
                         (st.sval != null && (Character.isDigit(st.sval.charAt(0))))) {              
                    if (lastVal != 40)  // add back whitespace that ST removes
                        expression.append(" ");
                    if (st.nval == 0) 
                        expression.append(st.sval);
                    else
                        expression.append(Double.toString(st.nval));
                    if (parenLevel<2)                                 // Don't care if parenLevel > 1
                        argumentNum = argumentNum + 1;                // RAP - added on 11/27/04 
                }
                else if (st.ttype == StreamTokenizer.TT_WORD) {                  // a token
                    if ((st.sval.equals("=>") || st.sval.equals("<=>")) && parenLevel == 1)   
                        // RAP - added parenLevel clause on 11/27/04 to 
                        // prevent implications embedded in statements from being rules
                        inRule = true;
                    if (parenLevel<2)                                 // Don't care if parenLevel > 1
                        argumentNum = argumentNum + 1;
                    if (lastVal != 40)                                // add back whitespace that ST removes
                        expression.append(" ");
                    expression.append(String.valueOf(st.sval));
                    if (expression.length() > 64000) {
                        errStr = (errStart + ": Sentence over 64000 characters new line " + f.startLine);
                        logger.warning(errStr);
                        logger.fine("st.sval = " + st.sval);
                        int elen = expression.length();
                        if (elen > 300)
                            logger.fine("expression == ... " + expression.substring(elen - 300));
                        else 
                            logger.fine("expression == " + expression.toString());
                        throw new ParseException(errStr, f.startLine);                      
                    }
                    // Build the terms list and create special keys ONLY if we are in NORMAL_PARSE_MODE.
                    if ((mode == NORMAL_PARSE_MODE) 
                        && (st.sval.charAt(0) != '?') 
                        && (st.sval.charAt(0) != '@')) {   // Variables are not terms
                        terms.add(st.sval);                  // collect all terms
                        key = createKey(st.sval,inAntecedent,inConsequent,argumentNum,parenLevel);
                        keySet.add(key); // Collect all the keys until the end of the statement is reached.
                    }                                        
                } 
                else if ((mode == RELAXED_PARSE_MODE) && (st.ttype == 96)) { 
                    // AB: 5/2007 - allow '`' in relaxed parse mode.
                    expression.append(" `");
                }
                else if (st.ttype != StreamTokenizer.TT_EOF) {
                    key = null;
                    errStr = (errStart + ": Illegal character near line " + f.startLine);
                    logger.warning(errStr);
                    logger.fine("st.sval = " + st.sval);
                    int elen = expression.length();
                    if (elen > 300)
                        logger.fine("expression == ... " + expression.substring(elen - 300));
                    else 
                        logger.fine("expression == " + expression.toString());
                    throw new ParseException(errStr, f.startLine);                      
                }
            } while (st.ttype != StreamTokenizer.TT_EOF);
            if (!keySet.isEmpty() || expression.length() > 0) {
                errStr = (errStart + ": Missed closing parenthesis near line " + f.startLine);
                logger.warning(errStr);
                logger.fine("st.sval == " + st.sval);
                int elen = expression.length();
                if (elen > 300)
                    logger.fine("expression == ... " + expression.substring(elen - 300));
                else 
                    logger.fine("expression == " + expression.toString());
                throw new ParseException(errStr, f.startLine);            
            }
        }
        catch (Exception ex) {
            warningSet.add("Error in KIF.parse(): " + ex.getMessage());
            logger.severe("Error in KIF.parse(): " + ex.getMessage());
            logger.severe("Error: " + ex.getStackTrace());
            ex.printStackTrace();
        }
        if (duplicateCount > 0) {
        	String warning = "WARNING in KIF.parse(Reader): " + duplicateCount + " duplicate statement"
            + ((duplicateCount > 1) ? "s " : " ") + "detected in " + (StringUtil.emptyString(filename)
                    ? " the input file" : filename);
            logger.warning(warning);
        }
        if (!warningSet.isEmpty()) {
            Iterator it = warningSet.iterator();
            while (it.hasNext()) {
                String w = (String) it.next();
               logger.finer(w.matches("^(?i)Error.+") 
                                   ? w 
                                   : ("WARNING in KIF.parse(): " + w));
            }
        }
        logger.exiting("KIF", "parse");
        return warningSet;
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
    private String createKey (String sval, boolean inAntecedent, boolean inConsequent,
                              int argumentNum, int parenLevel) {

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
    public void readFile(String fname) throws Exception {

        //System.out.println("INFO in KIF.readFile()");
    	logger.entering("KIF", "readFile", fname);

        FileReader fr = null;
        Exception exThr = null;
        try {
            this.file = new File(fname);
            this.filename = file.getCanonicalPath();
            fr = new FileReader(file);
            parse(fr);
        }
        catch (Exception ex) {
            exThr = ex;
            String er = ex.getMessage() + ((ex instanceof ParseException)
                                           ? " at line " + ((ParseException)ex).getErrorOffset()
                                           : "");
            logger.severe("ERROR in KIF.readFile(\"" + fname + "\"):" + "  " + er);
            KBmanager.getMgr().setError(KBmanager.getMgr().getError() 
                                        + "\n<br/>" + er + " in file " + fname + "\n<br/>");
        }
        finally {
            if (fr != null) {
                try {
                    fr.close();
                }
                catch (Exception ex2) {
                }
            }
        }
        logger.exiting("KIF", "readFile");
        if (exThr != null) 
            throw exThr;        
        return;
    }
  
    /** ***************************************************************
     * Write a KIF file.
     * @param fname - the name of the file to write, including full path.
     */
    public void writeFile(String fname) {

    	logger.entering("KIF", "writeFile", fname);
        logger.finer("Number of formulas = " + formulaSet.size());

        FileWriter fr = null;
        PrintWriter pr = null;
        try {
            fr = new FileWriter(fname);
            pr = new PrintWriter(fr);

            for (Iterator it = formulaSet.iterator(); it.hasNext();)
                pr.println((String) it.next());          
        }
        catch (Exception ex) {
        	logger.severe(ex.getMessage());
            ex.printStackTrace();
        }
        finally {
            try {
                if (pr != null) {
                    pr.close();
                }
                if (fr != null) {
                    fr.close();
                }
            }
            catch (Exception ex2) {
            }
        }
        logger.exiting("KIF", "writeFile");
        return;
    }

    /** ***************************************************************
     * Parse a single formula.
     */
    public String parseStatement(String formula) {

        StringReader r = new StringReader(formula);
        boolean isError = false;
        try {
            isError = !parse(r).isEmpty();
            if (isError) {
                String msg = "Error parsing " + formula;
                logger.warning(msg);
                return msg;
            }
        }
        catch (Exception e) {
            logger.warning("Error parsing " + formula);
            logger.severe(e.getStackTrace().toString());
            return e.getMessage();
        }
        return null;
    }


    /** ***************************************************************
     * Writes the TPTP output to a file.
     */
    public static void tptpOutputTest(String filename) throws IOException {

        Iterator it;
        KIF kifp = new KIF();
        Formula f;
        String form;
        ArrayList list;
        int axiomCount = 0;
        File toFile;
        FileWriter fw;
        PrintWriter pw;

        logger.entering("KIF", "tptpOutputTest", "filename=" + filename);        
        try {
            kifp.readFile(filename);
        }
        catch (Exception e1) {
            String msg = e1.getMessage();
            if (e1 instanceof ParseException) 
                msg += (" in statement starting at line " + ((ParseException)e1).getErrorOffset());            
            logger.warning(msg);
        }
        fw = null;
        pw = null;
        File outfile = new File(filename + ".tptp");

        try {
            fw = new FileWriter(outfile);
            pw = new PrintWriter(fw);
            it = kifp.formulaSet.iterator();
            while (it.hasNext()) {
                axiomCount++;
                form = (String) it.next();
                form = Formula.tptpParseSUOKIFString( form );
                form = "fof(axiom" + axiomCount + ",axiom,(" + form + ")).";
                if (form.indexOf('"') < 0 && form.indexOf('\'') < 0) 
                    pw.println(form + '\n');
            }
        }
        catch (Exception ex) {
            logger.warning("Error writing " + outfile.getCanonicalPath() + ": "  + ex.getMessage());
            ex.printStackTrace();
        }
        finally {
            try {
                if (pw != null) pw.close();                
                if (fw != null) fw.close();                
            }
            catch (Exception e3) { }
        }
    }

    /** ***************************************************************
     * Test method for this class.  
     */
    public static void main(String[] args) throws IOException {
    	
        // tptpOutputTest(args[0]);
        String exp = "(documentation foo \"(written by Claude François).\")";
        System.out.println(exp);
        KIF kif = new KIF();
        Reader r = new StringReader(exp);
        kif.parse(r);
        System.out.println(kif.formulaSet);
        ArrayList al = (ArrayList) kif.formulas.get("arg-0-documentation");
        Formula f = (Formula) al.get(0);
        System.out.println(f);
        f.read(f.cdr());
        f.read(f.cdr());
        System.out.println(f);
        System.out.println(f.car());
    }
}


