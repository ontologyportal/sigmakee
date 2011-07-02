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
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;
import java.util.logging.FileHandler;
import java.util.logging.Level;

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
    private Logger logger;
    
    public KIF() {
    	logger = Logger.getLogger("KIF_LOGGER");
    	logger.setLevel(Level.SEVERE);
    	
    	try {
    		FileHandler file = new FileHandler("/home/knomorosa/Desktop/KIF_LOG.log", 500000, 10);
    		file.setFormatter(new SimpleFormatter());
            logger.addHandler(file);
    	}
    	catch (Exception e) {
    		logger = Logger.getLogger("SIGMA_LOGGER");
    	}
    	
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
     *  This method has the side effect of setting the contents of
     *  formulaSet and formulas as it parses the file.  It throws a
     *  ParseException with file line numbers if fatal errors are
     *  encountered during parsing.
     *  @return a Set of warnings that may indicate syntax errors,
     *          but not fatal parse errors.
     */
    protected Set parse(Reader r) {

        // System.out.println("ENTER KIF.parse(" + r + ")");
        int mode = this.getParseMode();                
        //  System.out.println("INFO in KIF.parse()");
        //  System.out.println("  filename == " + this.getFilename());
        //  System.out.println("  parseMode == " + ((mode == RELAXED_PARSE_MODE) ? "RELAXED_PARSE_MODE" : "NORMAL_PARSE_MODE"));        
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
            warningSet.add(errStr);
            System.err.println(errStr);
            System.out.println("EXIT KIF.parse(" + r + ")");
            return warningSet;
        }
        try {
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
                            System.out.println("\n" + errStr + "\n");
                            System.out.println("st.sval == " + st.sval);
                            int elen = expression.length();
                            if (elen > 300)
                                System.out.println("expression == ... " + expression.substring(elen - 300));
                            else 
                                System.out.println("expression == " + expression.toString());
                            throw new ParseException(errStr, f.startLine);
                        }
                        continue;
                    }
                    else {                                            
                        // Found a first end of line character.
                        isEOL = true;                                 
                        // Turn on flag, to watch for a second consecutive one.
                        continue;
                    }
                }
                else if (isEOL) 
                    isEOL = false;                                    
                // Turn off isEOL if a non-space token encountered                              
                if (st.ttype==40) {                                   
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
                        // add back whitespace that ST removes
                        expression.append(" ");                    
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
                        // if (f.theFormula.startsWith("(contentRegex")) {
                        //      System.out.println("  formula == " + f.theFormula);
                        //  }
                        //f.tptpParse(false,null);   // not a query
                        if (formulaSet.contains(f.theFormula)) {
                            //String warning = ("Duplicate formula at line " + f.startLine + " of " + f.sourceFile + ": " + expression);
                            // lineStart + totalLinesForComments + expression;
                            // warningSet.add(warning);                            
                            duplicateCount++;
                        }
                        // Check argument validity ONLY if we are in
                        // NORMAL_PARSE_MODE.
                        if (mode == NORMAL_PARSE_MODE) {
                            String validArgs = f.validArgs((file != null ? file.getName() : null), 
                                                           (file != null 
                                                            ? new Integer(f.startLine) 
                                                            : null));
                            if (StringUtil.emptyString(validArgs))
                                validArgs = f.badQuantification();                      
                            if (StringUtil.isNonEmptyString(validArgs)) {
                                errStr = (errStart + ": Invalid number of arguments near line " + f.startLine);
                                System.out.println("\n" + errStr + "\n");
                                System.out.println("st.sval == " + st.sval);
                                int elen = expression.length();
                                if (elen > 300)
                                    System.out.println("expression == ... " + expression.substring(elen - 300));
                                else 
                                    System.out.println("expression == " + expression.toString());
                                throw new ParseException(errStr, f.startLine);  
                            }
                        }
                        // formulaList.add(expression.intern());
                        // if (formulaSet.size() % 100 == 0) System.out.print('.');
                        keySet.add(f.theFormula);           // Make the formula itself a key
                        keySet.add(f.createID());  
                        f.endLine = st.lineno() + totalLinesForComments;
                        for (String fkey : keySet) {
                            // Add the expression but ...
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
                        logger.finest("formulaSet.add(" + f.theFormula + ")");

                        inConsequent = false;
                        inRule = false;
                        argumentNum = -1;
                        lineStart = (st.lineno() + 1);                            

                        // start next statement from next line
                        expression = new StringBuilder();
                        // expression.delete(0,expression.length());
                        logger.finest("keySet: " + keySet);
                        keySet.clear();
                    }
                    else if (parenLevel < 0) {
                        errStr = (errStart + ": Extra closing parenthesis found near line " + f.startLine);
                        System.out.println("\n" + errStr + "\n");
                        System.out.println("st.sval == " + st.sval);
                        int elen = expression.length();
                        if (elen > 300)
                            System.out.println("expression == ... " + expression.substring(elen - 300));
                        else 
                            System.out.println("expression == " + expression.toString());
                        throw new ParseException(errStr, f.startLine);
                    }
                }
                else if (st.ttype==34) {                                      // " - it's a string
                    //System.out.println("INFO in KIF.parse()");
                    //System.out.println(st.sval);                    
                    st.sval = StringUtil.escapeQuoteChars(st.sval);
                    // st.sval = st.sval.replace("\"","\\\"");
                    // if (st.sval.contains("W[")) {
                    //   System.out.println("  st.sval == " + st.sval);
                    // }
                    //System.out.println(st.sval);
                    if (lastVal != 40)           // add back whitespace that ST removes
                        expression.append(" ");
                    expression.append("\"");
                    com = st.sval;
                    // if (com.contains("[")) {
                    //     System.out.println("  com == \"" + com + "\"");
                    // }
                    totalLinesForComments += countChar(com,(char)0X0A);
                    expression.append(com);
                    expression.append("\"");
                    if (parenLevel < 2)   // Don't care if parenLevel > 1
                        argumentNum = argumentNum + 1;
                }
                else if ((st.ttype == StreamTokenizer.TT_NUMBER) || 
                         (st.sval != null && (Character.isDigit(st.sval.charAt(0))))) { 
                    // number
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
                        //System.out.print("Error in KIF.parse(): Parsing error: Sentence Over 64000 characters.");
                        //System.out.println(new Integer(lineStart + totalLinesForComments).toString());
                        errStr = (errStart + ": Sentence over 64000 characters new line " + f.startLine);
                        System.out.println("\n" + errStr + "\n");
                        System.out.println("st.sval == " + st.sval);
                        int elen = expression.length();
                        if (elen > 300)
                            System.out.println("expression == ... " + expression.substring(elen - 300));
                        else 
                            System.out.println("expression == " + expression.toString());
                        throw new ParseException(errStr, f.startLine);                      
                    }
                    // Build the terms list and create special keys
                    // ONLY if we are in NORMAL_PARSE_MODE.
                    if ((mode == NORMAL_PARSE_MODE) 
                        && (st.sval.charAt(0) != '?') 
                        && (st.sval.charAt(0) != '@')) {   // Variables are not terms
                        terms.add(st.sval);                  // collect all terms
                        key = createKey(st.sval,inAntecedent,inConsequent,argumentNum,parenLevel);
                        keySet.add(key);                     // Collect all the keys until the end of
                    }                                        // the statement is reached.
                } 
                else if ((mode == RELAXED_PARSE_MODE) && (st.ttype == 96)) { 
                    // AB: 5/2007 - allow '`' in relaxed parse mode.
                    expression.append(" `");
                    // expression.append("`");
                }
                else if (st.ttype != StreamTokenizer.TT_EOF) {
                    key = null;
                    // System.out.println( "st.ttype == " + st.ttype );
                    //System.out.print("Error in KIF.parse(): Parsing Error: Illegal character at line: ");
                    //System.out.println(new Integer(lineStart + totalLinesForComments).toString());
                    errStr = (errStart + ": Illegal character near line " + f.startLine);
                    System.out.println("\n" + errStr + "\n");
                    System.out.println("st.sval == " + st.sval);
                    int elen = expression.length();
                    if (elen > 300)
                        System.out.println("expression == ... " + expression.substring(elen - 300));
                    else 
                        System.out.println("expression == " + expression.toString());
                    throw new ParseException(errStr, f.startLine);                      
                }
            } while (st.ttype != StreamTokenizer.TT_EOF);
            if (!keySet.isEmpty() || expression.length() > 0) {
                errStr = (errStart + ": Missed closing parenthesis near line " + f.startLine);
                System.out.println("\n" + errStr + "\n");
                System.out.println("st.sval == " + st.sval);
                int elen = expression.length();
                if (elen > 300)
                    System.out.println("expression == ... " + expression.substring(elen - 300));
                else 
                    System.out.println("expression == " + expression.toString());
                throw new ParseException(errStr, f.startLine);            
            }
        }
        catch (Exception ex) {
            warningSet.add("Error in KIF.parse(): " + ex.getMessage());
            System.out.println("Error in KIF.parse(): " + ex.getMessage());
            ex.printStackTrace();
        }
        // System.out.println( "x" );
        if (duplicateCount > 0) {
            warningSet.add("WARNING in KIF.parse(Reader): " + duplicateCount + " duplicate statement"
                           + ((duplicateCount > 1) ? "s " : " ") + "detected in " + (StringUtil.emptyString(filename)
                              ? " the input file" : filename));
        }
        if (!warningSet.isEmpty()) {
            Iterator it = warningSet.iterator();
            while (it.hasNext()) {
                String w = (String) it.next();
                System.out.println(w.matches("^(?i)Error.+") 
                                   ? w 
                                   : ("WARNING in KIF.parse(): " + w));
            }
        }
        // System.out.println("EXIT KIF.parse(" + r + ")");
        logger.finest("formulaSet: " + this.formulaSet);
        logger.finest("formulas: " + this.formulas);
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

        System.out.println("ENTER KIF.readFile(\"" + fname + "\")");

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
            System.out.println("ERROR in KIF.readFile(\"" + fname + "\")");
            System.out.println("  " + er);
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
        System.out.println("EXIT KIF.readFile(\"" + fname + "\")");
        if (exThr != null) 
            throw exThr;        
        return;
    }
  
    /** ***************************************************************
     * Write a KIF file.
     * @param fname - the name of the file to write, including full path.
     */
    public void writeFile(String fname) {

        System.out.println("ENTER KIF.writeFile(\"" + fname + "\")");
        System.out.println("  number of formulas == " + formulaSet.size());

        FileWriter fr = null;
        PrintWriter pr = null;
        try {
            fr = new FileWriter(fname);
            pr = new PrintWriter(fr);

            for (Iterator it = formulaSet.iterator(); it.hasNext();)
                pr.println((String) it.next());          
        }
        catch (Exception ex) {
            System.out.println("ERROR in KIF.writeFile(\"" + fname + "\")");
            System.out.println("  " + ex.getMessage());
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
        System.out.println("EXIT KIF.writeFile(\"" + fname + "\")");
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
                System.out.println(msg);
                return msg;
            }
        }
        catch (Exception e) {
            System.out.println("Error parsing " + formula);
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

        try {
            System.out.println("Loading from " + filename);
            kifp.readFile(filename);
        }
        catch (Exception e1) {
            String msg = e1.getMessage();
            if (e1 instanceof ParseException) {
                msg += (" in statement starting at line " 
                        + ((ParseException)e1).getErrorOffset());
            }
            System.out.println(msg);
        }
        /*
          it = kifp.formulaSet.iterator();
          while (it.hasNext()) {
          form = (String) it.next();
          System.out.println (form);
          }
        */
        System.out.println();

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
            System.out.println("Error writing " 
                               + outfile.getCanonicalPath() + ": " 
                               + ex.getMessage());
            ex.printStackTrace();
        }
        finally {
            try {
                if (pw != null) 
                    pw.close();                
                if (fw != null) 
                    fw.close();                
            }
            catch (Exception e3) {
            }
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


