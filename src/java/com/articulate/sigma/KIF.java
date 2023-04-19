/**
 * This code is copyright Articulate Software (c) 2003.  Some portions
 * copyright Teknowledge (c) 2003 and reused under the terms of the GNU license.
 * This software is released under the GNU Public License <http://www.gnu.org/copyleft/gpl.html>.
 * Users of this code also consent, by use of this code, to credit Articulate Software
 * and Teknowledge in any writings, briefings, publications, presentations, or
 * other representations of any software which incorporates, builds on, or uses this
 * code.  Please cite the following article in any publication with references:
 * <p>
 * Pease, A., (2003). The Sigma Ontology Development Environment,
 * in Working Notes of the IJCAI-2003 Workshop on Ontology and Distributed Systems,
 * August 9, Acapulco, Mexico.  See also http://sigmakee.sourceforget.net
 * 
 * Authors:
 * Adam Pease
 * Infosys LTD.
 */
package com.articulate.sigma;

import com.articulate.sigma.tp.EProver;
import com.articulate.sigma.tp.Vampire;
import com.articulate.sigma.trans.SUMOKBtoTPTPKB;
import com.articulate.sigma.trans.SUMOformulaToTPTPformula;
import com.articulate.sigma.trans.TPTP3ProofProcessor;
import com.articulate.sigma.utils.StringUtil;

import java.io.*;
import java.text.ParseException;
import java.util.*;

/******************************************************************
 * A class designed to read a file in SUO-KIF format into memory. See
 * <http://sigmakee.cvs.sourceforge.net/viewvc/sigmakee/sigma/suo-kif.pdf> for a
 * language specification. readFile() and writeFile() are the primary entry
 * points and parse() does all the real work.
 *
 * @author Adam Pease
 */
public class KIF {

    /*****************************************************************
     * A numeric constant denoting normal parse mode, in which syntax constraints are
     * enforced.
     */
    public static final int NORMAL_PARSE_MODE = 1;
    public static int count = 0;

    /****************************************************************
     * A numeric constant denoting relaxed parse mode, in which fewer syntax constraints
     * are enforced than in NORMAL_PARSE_MODE.
     */
    public static final int RELAXED_PARSE_MODE = 2;

    private int parseMode = NORMAL_PARSE_MODE;

    /** The set of all terms in the knowledge base. This is a set of Strings. */
    public TreeSet<String> terms = new TreeSet<String>();

    /** A hashMap to store term frequencies for each term in knowledge base */
    public Map<String, Integer> termFrequency = new HashMap<String, Integer>();

    /**
     * A HashMap of ArrayLists of Formulas. Each String key points to a list of
     * String formulas that correspond to that key. For example, "arg-1-Foo"
     * would be one of several keys for "(instance Foo Bar)".
     *
     * @see #createKey(String, boolean, boolean, int, int) for key format.
     */
    public HashMap<String, ArrayList<String>> formulas = new HashMap<String, ArrayList<String>>();

    /**
     * A HashMap of String keys representing the formula, and Formula values.
     * For example, "(instance Foo Bar)" is a String key that might point to a
     * Formula that is that string, along with information about at what line
     * number and in what file it appears.
     */
    public HashMap<String, Formula> formulaMap = new HashMap<String, Formula>();

    public String filename;
    private File file;
    private int totalLinesForComments = 0;

    /** warnings generated during parsing */
    public TreeSet<String> warningSet = new TreeSet<String>();
    /** errors generated during parsing */
    public TreeSet<String> errorSet = new TreeSet<String>();

    /*****************************************************************
     */
    public KIF() {
    }

    /*****************************************************************
     * Pre-allocate space for hashes, based on file size
     */
    public KIF(String fname) {

        long size = getKIFFileSize(fname);
        if (size != 0) {
            termFrequency = new HashMap<String, Integer>((int) size/25, (float) 0.75);
            formulas = new HashMap<String, ArrayList<String>>((int) size/3, (float) 0.75);
            formulaMap = new HashMap<String, Formula>((int) size/3, (float) 0.75);
        }
        filename = fname;
    }

    /*****************************************************************
     * @return long file size in bytes handling any errors
     */
    public long getKIFFileSize(String filename) {

        try {
            File f = new File(filename);
            if (!f.exists()) {
                System.out.println("KIF.getKIFFileSize(): error file " + filename + "does not exist");
                return 0;
            }
            return f.length();
        }
        catch (Exception ex) {
            System.out.println("KIF.getKIFFileSize(): error file " + ex.getMessage());
            ex.printStackTrace();
        }
        return 0;
    }

    /*****************************************************************
     * @return int Returns an integer value denoting the current parse mode.
     */
    public int getParseMode() {

        return this.parseMode;
    }

    /***************************************************************** 
     * Sets the current parse mode to the input value mode.
     *
     * @param mode
     *            An integer value denoting a parsing mode.
     * @return void
     */
    public void setParseMode(int mode) {

        this.parseMode = mode;
    }

    /****************************************************************
     * This routine sets up the StreamTokenizer_s so that it parses SUO-KIF. = < >
     * are treated as word characters, as are normal alphanumerics. ; is the
     * line comment character and " is the quote character.
     */
    public static void setupStreamTokenizer(StreamTokenizer_s st) {

        st.whitespaceChars(0, 32);
        st.ordinaryChars(33, 44); // !"#$%&'()*+,
        st.wordChars(45, 46); // -.
        st.ordinaryChar(47); // /
        st.wordChars(48, 58); // 0-9:
        st.ordinaryChar(59); // ;
        st.wordChars(60, 64); // <=>?@
        st.wordChars(65, 90); // A-Z
        st.ordinaryChars(91, 94); // [\]^
        st.wordChars(95, 95); // _
        st.ordinaryChar(96); // `
        st.wordChars(97, 122); // a-z
        st.ordinaryChars(123, 255); // {|}~
        // st.parseNumbers();
        st.quoteChar('"');
        st.commentChar(';');
        st.eolIsSignificant(true);
    }

    /*****************************************************************
     */
    private void display(StreamTokenizer_s st, boolean inRule, boolean inAntecedent, boolean inConsequent,
                         int argumentNum, int parenLevel, String key) {

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
    }

    /*****************************************************************
     * This method has the side effect of setting the contents of formulaMap and
     * formulas as it parses the file. It throws a ParseException with file line
     * numbers if fatal errors are encountered during parsing. Keys in variable
     * "formulas" include the string representation of the formula.
     *
     * @return a Set of warnings that may indicate syntax errors, but not fatal
     *         parse errors.
     */
    public TreeSet<String> parse(Reader r) {

        int mode = this.getParseMode();
        StringBuilder expression = new StringBuilder();
        int lastVal;
        Formula f = new Formula();
        String errStart = "Parsing error in " + filename;
        String errStr = null;
        int duplicateCount = 0;

        if (r == null) {
            errStr = "No Input Reader Specified";
            warningSet.add(errStr);
            System.err.println("Error in KIF.parse(): " + errStr);
            return warningSet;
        }
        try {
            count++;
            StreamTokenizer_s st = new StreamTokenizer_s(r);
            KIF.setupStreamTokenizer(st);
            int parenLevel = 0;
            boolean inRule = false;
            int argumentNum = -1;
            boolean inAntecedent = false;
            boolean inConsequent = false;
            HashSet<String> keySet = new HashSet<String>();
            // int lineStart = 0;
            boolean isEOL = false;
            do {
                lastVal = st.ttype;
                st.nextToken();
                //System.out.println("KIF.parse(): sval: " + st.sval);
                //System.out.println("KIF.parse(): parenLevel: " + parenLevel);
                //System.out.println("KIF.parse(): argumentNum: " + argumentNum);
                // check the situation when multiple KIF statements read as one
                // This relies on extra blank line to separate KIF statements
                if (st.ttype == StreamTokenizer.TT_EOL) {
                    if (isEOL) {
                        // Two line separators in a row shows a new KIF
                        // statement is to start. Check if a new statement
                        // has already been generated, otherwise report error
                        if (f.startLine != 0 && (!keySet.isEmpty() || (expression.length() > 0))) {
                            errStr = (errStart + " possible missed closing parenthesis near start line: " + f.startLine
                                    + " end line " + f.endLine + " for formula " + expression.toString() + "\n and key "
                                    + keySet.toString() + " keyset size " + keySet.size() + " exp length "
                                    + expression.length() + " comment lines " + totalLinesForComments);
                            errorSet.add(errStr);
                            throw new ParseException(errStr, f.startLine);
                        }
                        continue;
                    }
                    else { // Found a first end of line character.
                        isEOL = true; // Turn on flag, to watch for a second consecutive one.
                        continue;
                    }
                }
                else if (isEOL)
                    isEOL = false; // Turn off isEOL if a non-space token encountered
                if (st.ttype == 40) { // Open paren
                    if (parenLevel == 0) {
                        // lineStart = st.lineno();
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
                        expression.append(" "); // add back whitespace that ST removes
                    expression.append("(");
                }
                else if (st.ttype == 41) { // ) - close paren
                    parenLevel--;
                    expression.append(")");
                    if (parenLevel == 0) { // The end of the statement...
                        String fstr = StringUtil.normalizeSpaceChars(expression.toString());
                        f.read(fstr.intern());
                        if (formulaMap.keySet().contains(f.getFormula()) && !KBmanager.getMgr().getPref("reportDup").equals("no")) {
                            String warning = ("Duplicate axiom at line: " + f.startLine + " of " + f.sourceFile + ": "
                                    + expression);
                            warningSet.add(warning);
                            System.out.println(warning);
                            duplicateCount++;
                        }
                        if (mode == NORMAL_PARSE_MODE) { // Check arg validity ONLY in NORMAL_PARSE_MODE
                            String validArgs = f.validArgs((file != null ? file.getName() : null),
                                    (file != null ? Integer.valueOf(f.startLine) : null));
                            if (StringUtil.emptyString(validArgs))
                                validArgs = f.badQuantification();
                            if (StringUtil.isNonEmptyString(validArgs)) {
                                errStr = (errStart + ": Invalid number of arguments near line: " + f.startLine + " : "
                                        + validArgs);
                                errorSet.add(errStr);
                                throw new ParseException(errStr, f.startLine);
                            }
                        }
                        keySet.add(f.getFormula()); // Make the formula itself a key
                        keySet.add(f.createID());
                        f.endLine = st.lineno() + totalLinesForComments;
                        Iterator<String> it = keySet.iterator();
                        while (it.hasNext()) { // Add the expression but ...
                            String fkey = it.next();
                            if (formulas.containsKey(fkey)) {
                                if (!formulaMap.keySet().contains(f.getFormula())) { // don't add keys if formula is already present
                                    ArrayList<String> list = formulas.get(fkey);
                                    if (StringUtil.emptyString(f.getFormula())) {
                                        System.out.println("Error in KIF.parse(): Storing empty formula from line: "
                                                + f.startLine);
                                        errorSet.add(errStr);
                                    }
                                    else if (!list.contains(f.getFormula()))
                                        list.add(f.getFormula());
                                }
                            }
                            else {
                                ArrayList<String> list = new ArrayList<String>();
                                if (StringUtil.emptyString(f.getFormula())) {
                                    System.out.println(
                                            "Error in KIF.parse(): Storing empty formula from line: " + f.startLine);
                                    errorSet.add(errStr);
                                }
                                else if (!list.contains(f.getFormula()))
                                    list.add(f.getFormula());
                                formulas.put(fkey, list);
                            }
                        }
                        formulaMap.put(f.getFormula(), f);
                        inConsequent = false;
                        inRule = false;
                        argumentNum = -1;
                        expression = new StringBuilder();
                        keySet.clear();
                    }
                    else if (parenLevel < 0) {
                        errStr = (errStart + ": Extra closing parenthesis found near line: " + f.startLine);
                        errorSet.add(errStr);
                        throw new ParseException(errStr, f.startLine);
                    }
                }
                else if (st.ttype == 34) { // " - it's a string
                    st.sval = StringUtil.escapeQuoteChars(st.sval);
                    if (lastVal != 40) // add back whitespace that ST removes
                        expression.append(" ");
                    expression.append("\"");
                    String com = st.sval;
                    totalLinesForComments += StringUtil.countChar(com, (char) 0X0A);
                    expression.append(com);
                    expression.append("\"");
                    if (parenLevel < 2) // Don't care if parenLevel > 1
                        argumentNum = argumentNum + 1;
                }
                else if ((st.ttype == StreamTokenizer.TT_NUMBER) || // number
                        (st.sval != null && (Character.isDigit(st.sval.charAt(0))))) {
                    if (lastVal != 40) // add back whitespace that ST removes
                        expression.append(" ");
                    if (st.nval == 0)
                        expression.append(st.sval);
                    else
                        expression.append(Double.toString(st.nval));
                    if (parenLevel < 2) // Don't care if parenLevel > 1
                        argumentNum = argumentNum + 1;
                }
                else if (st.ttype == StreamTokenizer.TT_WORD) { // a token
                    if ((st.sval.equals("=>") || st.sval.equals("<=>")) && parenLevel == 1)
                        inRule = true; // implications in statements aren't rules
                    if (parenLevel < 2) // Don't care if parenLevel > 1
                        argumentNum = argumentNum + 1;
                    if (lastVal != 40) // add back whitespace that ST removes
                        expression.append(" ");
                    expression.append(String.valueOf(st.sval));
                    if (expression.length() > 64000) {
                        errStr = (errStart + ": Sentence over 64000 characters new line: " + f.startLine);
                        errorSet.add(errStr);
                        throw new ParseException(errStr, f.startLine);
                    }
                    // Build the terms list and special keys ONLY if in NORMAL_PARSE_MODE
                    if ((mode == NORMAL_PARSE_MODE) && (st.sval.charAt(0) != '?') && (st.sval.charAt(0) != '@')) { // Variables are not terms
                        terms.add(st.sval); // collect all terms
                        f.termCache.add(st.sval);

                        if (!termFrequency.containsKey(st.sval)) {
                            termFrequency.put(st.sval, 0);
                        }
                        termFrequency.put(st.sval, termFrequency.get(st.sval) + 1);
                        
                        String key = createKey(st.sval, inAntecedent, inConsequent, argumentNum, parenLevel);
                        keySet.add(key); // Collect all the keys until the end of the statement is reached.
                    }
                }
                else if ((mode == RELAXED_PARSE_MODE) && (st.ttype == 96)) // allow '`' in relaxed parse mode
                    expression.append(" `");
                else if (st.ttype != StreamTokenizer.TT_EOF) {
                    errStr = (errStart + ": Illegal character '" + st.sval + "' near line: " + f.startLine);
                    errorSet.add(errStr);
                    throw new ParseException(errStr, f.startLine);
                }
            } while (st.ttype != StreamTokenizer.TT_EOF);

            if (!keySet.isEmpty() || expression.length() > 0) {
                errStr = (errStart + ": Missed closing parenthesis near line: " + f.startLine +
                        " for token " + st.sval + " and form " + f.getFormula() +
                        " and expression " + expression + " and keySet " + keySet);
                errorSet.add(errStr);
                throw new ParseException(errStr, f.startLine);
            }
        }
        catch (Exception ex) {
            String message = ex.getMessage().replaceAll(":", "&58;"); // HTMLformatter.formatErrors depends on :
            warningSet.add("Warning in KIF.parse() " + message);
            ex.printStackTrace();
        }
        if (duplicateCount > 0) {
            String warning = "WARNING in KIF.parse(Reader), " + duplicateCount + " duplicate statement"
                    + ((duplicateCount > 1) ? "s " : " ") + "detected in "
                    + (StringUtil.emptyString(filename) ? " the input file" : filename);
            warningSet.add(warning);
        }
        return warningSet;
    }

    /*****************************************************************
     * This routine creates a key that relates a token in a logical statement to the
     * entire statement. It prepends to the token a string indicating its
     * position in the statement. The key is of the form type-[num]-term, where
     * [num] is only present when the type is "arg", meaning a statement in
     * which the term is nested only within one pair of parentheses. The other
     * possible types are "ant" for rule antecedent, "cons" for rule consequent,
     * and "stmt" for cases where the term is nested inside multiple levels of
     * parentheses. An example key would be arg-0-instance for a appearance of
     * the term "instance" in a statement in the predicate position.
     *
     * @param sval            - the token such as "instance", "Human" etc.
     * @param inAntecedent    - whether the term appears in the antecedent of a rule.
     * @param inConsequent    - whether the term appears in the consequent of a rule.
     * @param argumentNum     - the argument position in which the term appears. The
     *            predicate position is argument 0. The first argument is 1 etc.
     * @param parenLevel      - if the paren level is > 1 then the term appears nested in a
     *            statement and the argument number is ignored.
     */
    public static String createKey(String sval, boolean inAntecedent, boolean inConsequent,
                                   int argumentNum, int parenLevel) {

        //System.out.println("KIF.createKey(): sval: " + sval);
        //System.out.println("KIF.createKey(): argumentNum: " + argumentNum);
        //System.out.println("KIF.createKey(): parenLevel: " + parenLevel);
        if (sval == null) {
            sval = "null";
        }
        String key = new String("");
        if (inAntecedent) {
            key = key.concat("ant-");
            key = key.concat(sval);
        }

        if (inConsequent) {
            key = key.concat("cons-");
            key = key.concat(sval);
        }

        if (!inAntecedent && !inConsequent && (parenLevel == 1)) {
            key = key.concat("arg-");
            key = key.concat(String.valueOf(argumentNum));
            key = key.concat("-");
            key = key.concat(sval);
        }
        if (!inAntecedent && !inConsequent && (parenLevel > 1)) {
            key = key.concat("stmt-");
            key = key.concat(sval);
        }
        //System.out.println("KIF.createKey(): key: " + key);
        return (key);
    }

    /****************************************************************
     * Read a KIF file.
     *
     * @param fname - the full pathname of the file.
     */
    public void readFile(String fname) throws Exception {

        FileReader fr = null;
        Exception exThr = null;
        try {
            this.file = new File(fname);
            if (!this.file.exists()) {
                String errString =  " error file " + fname + "does not exist";
                KBmanager.getMgr()
                        .setError(KBmanager.getMgr().getError() + "\n<br/>" + errString + "\n<br/>");
                System.out.println("Error in KIF.readFile(): " + errString);
                return;
            }
            this.filename = file.getCanonicalPath();
            fr = new FileReader(file);
            parse(new BufferedReader(fr));
        }
        catch (Exception ex) {
            exThr = ex;
            String er = ex.getMessage()
                    + ((ex instanceof ParseException) ? " at line " + ((ParseException) ex).getErrorOffset() : "");
            KBmanager.getMgr()
                    .setError(KBmanager.getMgr().getError() + "\n<br/>" + er + " in file " + fname + "\n<br/>");
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
        if (exThr != null)
            throw exThr;
        return;
    }

    /****************************************************************
     * Write a KIF file.
     *
     * @param fname - the name of the file to write, including full path.
     */
    public void writeFile(String fname) {

        FileWriter fr = null;
        PrintWriter pr = null;
        try {
            fr = new FileWriter(fname);
            pr = new PrintWriter(fr);
            Iterator<Formula> it = formulaMap.values().iterator();
            while (it.hasNext())
                pr.println(it.next().getFormula());
        }
        catch (Exception ex) {
            System.out.println(ex.getMessage());
            ex.printStackTrace();
        }
        finally {
            try {
                if (pr != null)
                    pr.close();
                if (fr != null)
                    fr.close();
            }
            catch (Exception ex2) {
            }
        }
        return;
    }

    /*****************************************************************
     * Return an ArrayList of Formula in the same lexical order as their
     * source file
     */
    public ArrayList<Formula> lexicalOrder() {

        ArrayList<Formula> ordered = new ArrayList<>();
        ordered.addAll(formulaMap.values());
        Collections.sort(ordered,new Formula.SortByLine());
        return ordered;
    }

    /*****************************************************************
     * Parse a single formula.
     */
    public String parseStatement(String formula) {

        StringReader r = new StringReader(formula);
        boolean isError = false;
        try {
            isError = !parse(r).isEmpty();
            if (isError) {
                String msg = "Error parsing " + formula;
                return msg;
            }
        }
        catch (Exception e) {
            System.out.println("Error parsing " + formula);
            e.printStackTrace();
            return e.getMessage();
        }
        return null;
    }

    /*****************************************************************
     * Writes the TPTP output to a file.
     */
    public static void tptpOutputTest(String filename) throws IOException {

        KIF kifp = new KIF();
        int axiomCount = 0;
        try {
            kifp.readFile(filename);
        }
        catch (Exception e1) {
            String msg = e1.getMessage();
            if (e1 instanceof ParseException)
                msg = msg + (" in statement starting at line " + ((ParseException) e1).getErrorOffset());
        }
        FileWriter fw = null;
        PrintWriter pw = null;
        File outfile = new File(filename + ".tptp");
        try {
            fw = new FileWriter(outfile);
            pw = new PrintWriter(fw);
            Iterator<String> it = kifp.formulaMap.keySet().iterator();
            while (it.hasNext()) {
                axiomCount++;
                String form = it.next();
                form = SUMOformulaToTPTPformula.tptpParseSUOKIFString(form, false); // not
                // a
                // query
                form = "fof(axiom" + axiomCount + ",axiom,(" + form + ")).";
                if (form.indexOf('"') < 0 && form.indexOf('\'') < 0)
                    pw.println(form + '\n');
            }
        }
        catch (Exception ex) {
            System.out.println("Error writing " + outfile.getCanonicalPath() + ": " + ex.getMessage());
            ex.printStackTrace();
        }
        finally {
            try {
                if (pw != null)
                    pw.close();
                if (fw != null)
                    fw.close();
            }
            catch (IOException ioe) {
                ioe.printStackTrace();
                System.out.println(ioe.getMessage());
            }
        }
    }

    /*****************************************************************
     * Test method for this class.
     */
    public static void test() {

        // tptpOutputTest(args[0]);
        String exp = "(documentation foo \"(written by John Smith).\")";
        System.out.println(exp);
        KIF kif = new KIF();
        Reader r = new StringReader(exp);
        kif.parse(r);
        System.out.println(kif.formulaMap);
        ArrayList<String> al = kif.formulas.get("arg-0-documentation");
        String fstr = al.get(0);
        Formula f = kif.formulaMap.get(fstr);
        System.out.println(f);
        f.read(f.cdr());
        f.read(f.cdr());
        System.out.println(f);
        System.out.println(f.car());
    }

    /** ***************************************************************
     */
    public static void showHelp() {

        System.out.println("KIF class");
        System.out.println("  options (with a leading '-'):");
        System.out.println("  h - show this help screen");
        System.out.println("  p \"<statement>\" - parse and show keys");
        System.out.println("  f <filename> - parse and show keys");
        System.out.println("  t - run a test");
    }

    /** ***************************************************************
     */
    public static void main(String[] args) throws IOException {

        System.out.println("INFO in KIF.main()");
        if (args != null && args.length > 0 && args[0].equals("-h"))
            showHelp();
        else {
            if (args != null && args.length > 1 && args[0].contains("p")) {
                KIF kif = new KIF();
                Reader r = new StringReader(args[1]);
                kif.parse(r);
                System.out.println("formulaMap: " + kif.formulaMap);
                System.out.println("formulas: " + kif.formulas);
            }
            else if (args != null && args.length > 1 && args[0].contains("f")) {
                try {
                    KIF kif = new KIF();
                    kif.readFile(args[1]);
                    System.out.println("formulaMap: " + kif.formulaMap);
                    System.out.println("formulas: " + kif.formulas);
                }
                catch (Exception e) {
                    e.printStackTrace();
                }
            }
            else if (args != null && args.length > 0 && args[0].contains("t"))
                test();
        }
    }
}
