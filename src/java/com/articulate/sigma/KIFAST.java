package com.articulate.sigma;

import com.articulate.sigma.utils.StringUtil;

import java.io.*;
import java.text.ParseException;
import java.util.*;

public class KIFAST {

    /*****************************************************************
     * A numeric constant denoting normal parse mode, in which syntax constraints are
     * enforced.
     */
    public static int count = 0;

    /** The set of all terms in the knowledge base. This is a set of Strings. */
    public Set<String> terms = new TreeSet<>();

    /** A hashMap to store term frequencies for each term in knowledge base */
    public Map<String, Integer> termFrequency = new HashMap<>();

    /**
     * A HashMap of ArrayLists of Formulas. Each String key points to a list of
     * String formulas that correspond to that key. For example, "arg-1-Foo"
     * would be one of several keys for "(instance Foo Bar)".
     *
     * see #createKey(String, boolean, boolean, int, int) for key format.
     */
    public Map<String, List<FormulaAST>> formulas = new HashMap<>();

    /**
     * A HashMap of String keys representing the formula, and Formula values.
     * For example, "(instance Foo Bar)" is a String key that might point to a
     * Formula that is that string, along with information about at what line
     * number and in what file it appears.
     */
    public Map<String, FormulaAST> formulaMap = new HashMap<>();

    public String filename;
    private File file;
    private int totalLinesForComments = 0;

    /** warnings generated during parsing */
    public Set<String> warningSet = new TreeSet<>();
    /** errors generated during parsing */
    public Set<String> errorSet = new TreeSet<>();

    /****************************************************************
     * Read a KIF file.
     *
     * @param fname - the full pathname of the file.
     */
    public Set<String> readFile(String fname) throws Exception {

        Set<String> warnings = new TreeSet<>();
        Exception exThr = null;
        this.file = new File(fname);
        if (!this.file.exists()) {
            String errString =  " error file " + fname + "does not exist";
            KBmanager.getMgr()
                    .setError(KBmanager.getMgr().getError() + "\n<br/>" + errString + "\n<br/>");
            System.err.println("Error in KIF.readFile(): " + errString);
            return null;
        }
        this.filename = file.getCanonicalPath();
        try (FileReader fr = new FileReader(file);
            BufferedReader br = new BufferedReader(fr)) {
            if (br == null) {
                String errStr = "No Input Reader Specified";
                warningSet.add(errStr);
                System.err.println("Error in KIF.readFile(): " + errStr);
                return warningSet;
            }
            count++;
            StreamTokenizer_s st = new StreamTokenizer_s(br);
            KIF.setupStreamTokenizer(st);
            parseNew(st);
        }
        catch (Exception ex) {
            exThr = ex;
            String er = ex.getMessage()
                    + ((ex instanceof ParseException) ? " at line " + ((ParseException) ex).getErrorOffset() : "");
            KBmanager.getMgr()
                    .setError(KBmanager.getMgr().getError() + "\n<br/>" + er + " in file " + fname + "\n<br/>");
        }
        if (exThr != null)
            throw exThr;
        return warnings;
    }

    /*****************************************************************
     * @return a Set of warnings that may indicate syntax errors, but not fatal
     *         parse errors.
     */
    public Set<String> parseBody(String s, int startLine, int endLine) {

        int lastVal;
        String errStr;
        int parenLevel = 0;
        String errStart = "Parsing error in " + filename;
        StringBuilder sb = new StringBuilder();
//        FormulaAST fast = new FormulaAST();
        System.out.println("parseBody: " + s);
        Reader r = new StringReader(s);
        StreamTokenizer_s st = new StreamTokenizer_s(r);
        KIF.setupStreamTokenizer(st);
        try {
            do {
                lastVal = st.ttype;
                st.nextToken();
                switch (st.ttype) {
                    case 40:
                        // Open paren
                        parenLevel++;
                        if ((parenLevel != 0) && (lastVal != 40) && (sb.length() > 0))
                            sb.append(" "); // add back whitespace that ST removes
                        sb.append("(");
                        break;
                    case 41:
                        // ) - close paren
                        parenLevel--;
                        sb.append(")");
                        if (parenLevel == 0) { // The end of the statement...
                            if (formulaMap.keySet().contains(sb.toString()) && !KBmanager.getMgr().getPref("reportDup").equals("no")) {
                                String warning = ("Duplicate axiom at line: " + startLine + " of " + filename + ": " + sb);
                                warningSet.add(warning);
                                System.out.println(warning);
                            }
                            warningSet.addAll(parseBody(sb.toString(),startLine,endLine));  // <--- call parseBody()
                            sb = new StringBuilder();
                        }
                        else if (parenLevel < 0) {
                            errStr = (errStart + ": Extra closing parenthesis found near line: " + startLine);
                            errorSet.add(errStr);
                            throw new ParseException(errStr, startLine);
                        }
                        break;
                    case 34:
                        // " - it's a string
                        st.sval = StringUtil.escapeQuoteChars(st.sval);
                        if (lastVal != 40) // add back whitespace that ST removes
                            sb.append(" ");
                        sb.append("\"");
                        String com = st.sval;
                        sb.append(com);
                        sb.append("\"");
                        break;
                    default:
                        if (lastVal != 40) // add back whitespace that ST removes
                            sb.append(" ");
                        sb.append(st.sval);
                        break;
                }
            } while (st.ttype != StreamTokenizer.TT_EOF);
        }
        catch (IOException | ParseException ex) {
            String message = ex.getMessage().replaceAll(":", "&#58;"); // HTMLformatter.formatErrors depends on :
            warningSet.add("Warning in KIFAST.parseBody() " + message);
            ex.printStackTrace();
        }
        return new TreeSet<>();
    }

    /*****************************************************************
     * Get a formula at a time and send to parseBody()
     *
     * @return a Set of warnings that may indicate syntax errors, but not fatal
     *         parse errors.
     */
    public Set<String> parseNew(StreamTokenizer_s st) {

        String errStr;
        Set<String> warnings = new TreeSet<>();
//        char ch;
        boolean isEOL = false;
        int lastVal;
//        int lineNum = 0;
        int startLine = 0;
        int endLine;
        int parenLevel = 0;
        int duplicateCount = 0;
        String errStart = "Parsing error in " + filename;
        StringBuilder sb = new StringBuilder();

        try {
            do {
                lastVal = st.ttype;
                st.nextToken();
                if (st.ttype == StreamTokenizer.TT_EOL) {
                    if (isEOL) {
                        // Two line separators in a row shows a new KIF statement is to start.
                        if (startLine != 0 && sb.length() > 0) {
                            errStr = (errStart + " possible missed closing parenthesis near start line: " + startLine +
                                    " for formula " + sb.toString());
                            errorSet.add(errStr);
                            throw new ParseException(errStr, startLine);
                        }
                        continue;
                    }
                    else { // Found a first end of line character.
                        isEOL = true; // Turn on flag, to watch for a second consecutive one.
                        continue;
                    }
                }
                else if (isEOL)
                    isEOL = false; // Turn off isEOL if a non-EOL token encountered
                switch (st.ttype) {
                    case 40:
                        // Open paren
                        if (parenLevel == 0)
                            startLine = st.lineno();
                        parenLevel++;
                        if ((parenLevel != 0) && (lastVal != 40) && (sb.length() > 0))
                            sb.append(" "); // add back whitespace that ST removes
                        sb.append("(");
                        break;
                    case 41:
                        // ) - close paren
                        parenLevel--;
                        sb.append(")");
                        if (parenLevel == 0) { // The end of the statement...
                            if (formulaMap.keySet().contains(sb.toString()) && !KBmanager.getMgr().getPref("reportDup").equals("no")) {
                                String warning = ("Duplicate axiom at line: " + startLine + " of " + filename + ": " + sb);
                                warningSet.add(warning);
                                System.out.println(warning);
                                duplicateCount++;
                            }
                            endLine = st.lineno() + totalLinesForComments;
                            warningSet.addAll(parseBody(sb.toString(),startLine,endLine));  // <--- call parseBody()
                            sb = new StringBuilder();
                        }
                        else if (parenLevel < 0) {
                            errStr = (errStart + ": Extra closing parenthesis found near line: " + startLine);
                            errorSet.add(errStr);
                            throw new ParseException(errStr, startLine);
                        }
                        break;
                    case 34:
                        // " - it's a string
                        st.sval = StringUtil.escapeQuoteChars(st.sval);
                        if (lastVal != 40) // add back whitespace that ST removes
                            sb.append(" ");
                        sb.append("\"");
                        String com = st.sval;
                        totalLinesForComments += StringUtil.countChar(com, (char) 0X0A);
                        sb.append(com);
                        sb.append("\"");
                        break;
                    default:
                        if (lastVal != 40) // add back whitespace that ST removes
                            sb.append(" ");
                        sb.append(st.sval);
                        break;
                }
            } while (st.ttype != StreamTokenizer.TT_EOF);
        }
        catch (IOException | ParseException ex) {
            String message = ex.getMessage().replaceAll(":", "&#58;"); // HTMLformatter.formatErrors depends on :
            warningSet.add("Warning in KIFAST.parseNew(StreamTokenizer_s) " + message);
            ex.printStackTrace();
        }
        if (duplicateCount > 0) {
            String warning = "WARNING in KIFAST.parse(StreamTokenizer_s), " + duplicateCount + " duplicate statement"
                    + ((duplicateCount > 1) ? "s " : " ") + "detected in "
                    + (StringUtil.emptyString(filename) ? " the input file" : filename);
            warningSet.add(warning);
        }
        return warnings;
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
    public Set<String> parse(Reader r) {

//        Stack<FormulaAST.Term> stack = new Stack<>();
        StringBuilder expression = new StringBuilder();
        int lastVal;
        FormulaAST f = new FormulaAST();
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
            Set<String> keySet = new HashSet<>();
            // int lineStart = 0;
            boolean isEOL = false;
            String fstr, warning, validArgs;
            List<FormulaAST> list;
            do {
                lastVal = st.ttype;
                st.nextToken();
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
                        f = new FormulaAST();
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
                        fstr = StringUtil.normalizeSpaceChars(expression.toString());
                        f.read(fstr.intern());
                        if (formulaMap.keySet().contains(f.toString()) && !KBmanager.getMgr().getPref("reportDup").equals("no")) {
                            warning = ("Duplicate axiom at line: " + f.startLine + " of " + f.sourceFile + ": "
                                    + expression);
                            warningSet.add(warning);
                            System.err.println(warning);
                            duplicateCount++;
                        }
                        validArgs = f.validArgs((file != null ? file.getName() : null),
                                (file != null ? f.startLine : null));
                        if (StringUtil.emptyString(validArgs))
                            validArgs = f.badQuantification();
                        if (StringUtil.isNonEmptyString(validArgs)) {
                            errStr = (errStart + ": Invalid number of arguments near line: " + f.startLine + " : "
                                    + validArgs);
                            errorSet.add(errStr);
                            throw new ParseException(errStr, f.startLine);
                        }
                        keySet.add(f.toString()); // Make the formula itself a key
                        keySet.add(f.createID());
                        f.endLine = st.lineno() + totalLinesForComments;
                        for (String fkey : keySet) {
                            // Add the expression but ...
                            if (formulas.containsKey(fkey)) {
                                if (!formulaMap.keySet().contains(f.toString())) { // don't add keys if formula is already present
                                    list = formulas.get(fkey);
                                    if (StringUtil.emptyString(f.toString())) {
                                        System.err.println("Error in KIF.parse(): Storing empty formula from line: "
                                                + f.startLine);
                                        errorSet.add(errStr);
                                    }
                                    else if (!list.contains(f))
                                        list.add(f);
                                }
                            } else {
                                list = new ArrayList<>();
                                if (StringUtil.emptyString(f.toString())) {
                                    System.out.println(
                                            "Error in KIF.parse(): Storing empty formula from line: " + f.startLine);
                                    errorSet.add(errStr);
                                }
                                else if (!list.contains(f))
                                    list.add(f);
                                formulas.put(fkey, list);
                            }
                        }
                        formulaMap.put(f.toString(), f);
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
                    if ((st.sval.charAt(0) != '?') && (st.sval.charAt(0) != '@')) { // Variables are not terms
                        terms.add(st.sval); // collect all terms
                        f.termCache.add(st.sval);

                        if (!termFrequency.containsKey(st.sval)) {
                            termFrequency.put(st.sval, 0);
                        }
                        termFrequency.put(st.sval, termFrequency.get(st.sval) + 1);

                        String key = KIF.createKey(st.sval, inAntecedent, inConsequent, argumentNum, parenLevel);
                        keySet.add(key); // Collect all the keys until the end of the statement is reached.
                    }
                }
                else if (st.ttype == 96) // allow '`' in relaxed parse mode
                    expression.append(" `");
                else if (st.ttype != StreamTokenizer.TT_EOF) {
                    errStr = (errStart + ": Illegal character near line: " + f.startLine);
                    errorSet.add(errStr);
                    throw new ParseException(errStr, f.startLine);
                }
            } while (st.ttype != StreamTokenizer.TT_EOF);

            if (!keySet.isEmpty() || expression.length() > 0) {
                errStr = (errStart + ": Missed closing parenthesis near line: " + f.startLine);
                errorSet.add(errStr);
                throw new ParseException(errStr, f.startLine);
            }
        }
        catch (IOException | ParseException ex) {
            String message = ex.getMessage().replaceAll(":", "&#58;"); // HTMLformatter.formatErrors depends on :
            warningSet.add("Warning in KIFAST.parse(Reader) " + message);
            ex.printStackTrace();
        }
        if (duplicateCount > 0) {
            String warning = "WARNING in KIFAST.parse(Reader), " + duplicateCount + " duplicate statement"
                    + ((duplicateCount > 1) ? "s " : " ") + "detected in "
                    + (StringUtil.emptyString(filename) ? " the input file" : filename);
            warningSet.add(warning);
        }
        return warningSet;
    }

    /*****************************************************************
     * Test method for this class. TODO: Currently throws a stack overflow. 2/4/25 tdn
     */
    public static void main(String[] args) throws IOException {

        String exp = "(documentation foo \"(written by John Smith).\")" +
                "(instance Foo Bar)\n" +
                "(=>\n" +
                "  (instance ?X Man)\n" +
                "  (attribute ?X Mortal))";
        System.out.println(exp);
        KIFAST kif = new KIFAST();
        try (Reader r = new StringReader(exp)) {
            StreamTokenizer_s st = new StreamTokenizer_s(r);
            KIF.setupStreamTokenizer(st);
            kif.parseNew(st);
            System.out.println(kif.formulaMap);
            List<FormulaAST> al = new ArrayList<>();
            al.addAll(kif.formulaMap.values());
            FormulaAST f = al.get(0);
            System.out.println(f);
            f.read(f.cdr());
            f.read(f.cdr());
            System.out.println(f);
            System.out.println(f.car());
        }
    }
}
