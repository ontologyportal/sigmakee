/** This code is copyrighted by Articulate Software (c) 2007.  It is
released under the GNU Public License &lt;http://www.gnu.org/copyleft/gpl.html&gt;."\""
Users of this code also consent, by use of this code, to credit
Articulate Software in any writings, briefings, publications,
presentations, or other representations of any software which
incorporates, builds on, or uses this code.  Please cite the following
article in any publication with references:
Pease, A., (2003). The Sigma Ontology Development Environment, in Working
Notes of the IJCAI-2003 Workshop on Ontology and Distributed Systems,
August 9, Acapulco, Mexico.  See also http://sigmakee.sourceforge.net.
 */
/*************************************************************************************************/
package com.articulate.sigma;

import com.articulate.sigma.dataProc.Hotel;
import com.articulate.sigma.utils.AVPair;
import com.articulate.sigma.utils.StringUtil;
import com.articulate.sigma.wordNet.WSD;
import com.articulate.sigma.wordNet.WordNet;
import com.articulate.sigma.wordNet.WordNetUtilities;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** A class to interface with databases and database-like formats,
such as spreadsheets. */
public class DB {
      // a map of word keys, broken down by POS, listing whether it's a positive or negative word
      // keys are pre-defined as type, POS, stemmed, polarity
    public static Map<String,Map<String,String>> sentiment = new HashMap<>();
    public static Set<String> amenityTerms = new HashSet<>();
    public static Set<String> stopConcepts = new HashSet<>();

    /** ***************************************************************
     *  Print statistics in a summary form for TPTP test run
     *  data
     */
    public Map printTPTPDataInCSV(Map byProver) throws IOException {

        // get header from any entry
        Map header = (HashMap) byProver.values().iterator().next();
        Set problemNames = new TreeSet();
        problemNames.addAll(header.keySet());
        Iterator it2 = problemNames.iterator();
        System.out.print(",");
        String problemName;
        while (it2.hasNext()) {
            problemName = (String) it2.next();
            System.out.print(problemName.substring(0,8) + ",");
        }
        System.out.println();
        Iterator it1 = byProver.keySet().iterator();
        String proverName, value;
        Map problems;
        while (it1.hasNext()) {
            proverName = (String) it1.next();
            System.out.print(proverName + ", ");
            problems = (HashMap) byProver.get(proverName);
            it2 = problemNames.iterator();
            while (it2.hasNext()) {
                problemName = (String) it2.next();
                value = (String) problems.get(problemName);
                System.out.print(value + ", ");
            }
            System.out.println();
        }
        return byProver;
    }

    /** ***************************************************************
     *  Reorganize statistics in a summary form for TPTP test run
     *  data
     */
    public Map resortTPTPData(Map stats) throws IOException {

        Map byProver = new HashMap();
        // A HashMap where keys are prover names and values are HashMaps
        // The interior HashMap keys are problem name and values are times
        // with 0 if the problem failed.
        String problemName, proverName, value;
        Map problem, proverPerformance;
        for (Iterator it1 = stats.keySet().iterator(); it1.hasNext();) {
            problemName = (String) it1.next();
            problem = (HashMap) stats.get(problemName);
            for (Iterator it2 = problem.keySet().iterator(); it2.hasNext();) {
                proverName = (String) it2.next();
                value = (String) problem.get(proverName);
                if (value.equals("F"))
                    value = "600";
                if (byProver.containsKey(proverName))
                    proverPerformance = (HashMap) byProver.get(proverName);
                else {
                    proverPerformance = new HashMap();
                    byProver.put(proverName,proverPerformance);
                }
                proverPerformance.put(problemName,value);
            }
        }
        return byProver;
    }

    /** ***************************************************************
     *  Read statistics for TPTP test run data
     */
    public Map processTPTPData() throws IOException {

        Map problemSet = new HashMap();
        // a HashMap of HashMaps where the key is the problem name
        // The interior HashMap has a key of prover name and a String
        // representation of the time to proof or "F" if no proof.
        String problemName;
        List al = readSpreadsheet("TPTPresults.csv", null,true);
        //System.out.println("INFO in DB.processTPTPData: " + al.size() + " lines");
        Map problem = null;
        List row;
        String cell, proverName, success, time;
        for (int i = 0; i < al.size(); i++) {
            row = (ArrayList) al.get(i);
            //System.out.println("INFO in DB.processTPTPData line: " + row);
            if (!row.isEmpty()) {
                cell = (String) row.get(0);
                //System.out.println(cell);
                if (cell.startsWith("CSR")) {
                    problem = new HashMap();
                    problemName = cell;
                    problemSet.put(problemName,problem);
                }
                else {
                    proverName = cell;
                    success = (String) row.get(1);
                    time = (String) row.get(3);
                    if (!success.equals("T"))
                        time = "F";
                    problem.put(proverName,time);
                }
            }
        }
        //System.out.println("INFO in DB.processTPTPData: " + problemSet.keySet().size() + " problems");
        return problemSet;
    }

    /** ***************************************************************
     * This procedure is called by @see generateDB().  It generates
     * SQL statements of some of the following forms:
     *
     * create table [table name] (personid int(50),firstname
     *  varchar(35));
     * alter table [table name]
     * add column [new column name] varchar (20);
     * drop database [database name];
     * INSERT INTO [table name]
     * (Host,Db,User,Select_priv,Insert_priv,Update_priv,Delete_priv,Create_priv,Drop_priv)
     * VALUES
     * ('%','databasename','username','Y','Y','Y','Y','Y','N');
     */
    private void generateDBElement(KB kb, String element) {

        List docs = kb.askWithRestriction(0,"localDocumentation",3,element);
        System.out.println("alter table " + element + " add column documentation varchar(255);");
        if (!docs.isEmpty()) {
            Formula f = (Formula) docs.get(0);
            String doc = f.getStringArgument(4);
            System.out.println("insert into " + element + "(documentation) values ('" + doc + "');");
        }
        List subs = kb.askWithRestriction(0,"HasDatabaseColumn",1,element);
        Formula f;
        String t;
        for (int i = 0; i < subs.size(); i++) {
            f = (Formula) subs.get(i);
            t = f.getStringArgument(2);
            System.out.println("alter table " + element + " add column " + t + " varchar(255);");
        }
    }

    /** ***************************************************************
     *  Generate an SQL database from the knowledge base
     *  Tables must be defined as instances of &%DatabaseTable and
     *  must have &%localDocumentation and &%HasDatabaseColumn
     *  relations.
     */
    public void generateDB(KB kb) {

        System.out.println("create database " + kb.name + ";");
        List composites = kb.askWithRestriction(0,"instance",2,"DatabaseTable");
        Formula f;
        String element;
        for (int i = 0; i < composites.size(); i++) {
            f = (Formula) composites.get(i);
            element = f.getStringArgument(1);
            System.out.println("create table " + element + ";");
            generateDBElement(kb, element);
        }
    }

    /** ***************************************************************
     * Parse the input from a Reader for a CSV file into an ArrayList
     * of ArrayLists.  If lineStartTokens is a non-empty list, all
     * lines not starting with one of the String tokens it contains
     * will be concatenated.  ';' denotes a comment line and will be skipped
     *
     * @param inReader A reader for the file to be processed
     * @param lineStartTokens If a List containing String tokens, all
     *        lines not starting with one of the tokens will be concatenated
     * @param quote signifies whether to retain quotes in elements
     * @return An ArrayList of ArrayLists
     */
    public static List<List<String>> readSpreadsheet(Reader inReader, List<String> lineStartTokens, boolean quote, char delimiter) {

        long t1 = System.currentTimeMillis();
        System.out.println("ENTER DB.readSpreadsheet(" + inReader + ", " + lineStartTokens + ")");
        List<List<String>> rows = new ArrayList<>();
        String line = null, cellVal;
        StringBuilder cell = null;
        try (LineNumberReader lr = new LineNumberReader(inReader)) {
            List<String> textrows = new ArrayList<>();
            int trLen = 0;
            boolean areTokensListed = ((lineStartTokens != null) && !lineStartTokens.isEmpty());
            boolean skippingHeader = true, concat;
            String unquoted, token, previousLine;
            while ((line = lr.readLine()) != null) {
                if (skippingHeader) {
                    skippingHeader = line.startsWith(";") || line.trim().isEmpty();
                }
                if (!skippingHeader) {
                    try {
                        if (StringUtil.containsNonAsciiChars(line))
                            System.out.println("\nINFO in DB.readSpreadsheet(): NonASCII char near line " + lr.getLineNumber() + ": " + line + "\n");
                        line += " ";
                        // concatenate lines not starting with one of the
                        // tokens in lineStartTokens.
                        concat = false;
                        if (areTokensListed) {
                            unquoted = StringUtil.unquote(line);
                            token = null;
                            for (Iterator<String> it = lineStartTokens.iterator(); it.hasNext();) {
                                token = (String) it.next();
                                if (unquoted.startsWith(token)) {
                                    concat = true;
                                    break;
                                }
                            }
                        }
                        if (concat && !textrows.isEmpty()) {
                            trLen = textrows.size();
                            previousLine = (String) textrows.get(trLen - 1);
                            line = previousLine + line;
                            textrows.remove(trLen - 1);
                            textrows.add(line);
                        }
                        else
                            textrows.add(line);
                    }
                    catch (Exception ex1) {
                        System.err.println("ERROR in ENTER DB.readSpreadsheet(" + inReader + ", " + lineStartTokens + ")");
                        System.err.println("  approx. line # == " + lr.getLineNumber());
                        System.err.println("  line == " + line);
                        ex1.printStackTrace();
                    }
                }
            }
            try {
                if (lr != null) { lr.close(); } // Close the input stream.
            }
            catch (IOException lre) {
                lre.printStackTrace();
            }
            cell = new StringBuilder();
            int linelen;
            List<String> row;
            boolean inString;
            for (Iterator<String> itr = textrows.iterator(); itr.hasNext();) {
                // parse comma delimited cells into an ArrayList
                line = (String) itr.next();
                linelen = line.length();
                cell.setLength(0);
                row = new ArrayList<>();
                inString = false;
                for (int j = 0; j < linelen; j++) {
                    if ((line.charAt(j) == delimiter) && !inString) {
                        cellVal = cell.toString();
                        // cellVal = cellVal.trim()
                        if (cellVal.matches(".*\\w+.*"))
                            cellVal = cellVal.trim();
                        if (!quote)
                            cellVal = StringUtil.removeEnclosingQuotes(cellVal);
                        row.add(cellVal);
                        cell.setLength(0);
                        // cell = new StringBuilder();
                    }
                    else {
                        if ((line.charAt(j) == '"') && ((j == 0) || (line.charAt(j-1) != '\\')))
                            inString = !inString;
                        cell.append(line.charAt(j));
                    }
                }
                cellVal = cell.toString();
                // cellVal = cellVal.trim();
                if (cellVal.matches(".*\\w+.*"))
                    cellVal = cellVal.trim();
                if (!quote)
                    cellVal = StringUtil.removeEnclosingQuotes(cellVal);
                row.add(cellVal);
                rows.add(row);
            }
        }
        catch (Exception e) {
            System.err.println("ERROR in DB.readSpreadsheet(" + inReader + ", " + lineStartTokens + ")");
            System.err.println("  line == " + line);
            System.err.println("  cell == " + cell.toString());
            System.err.println(e.getMessage());
            e.printStackTrace();
        }
        System.out.println("EXIT DB.readSpreadsheet(" + inReader + ", " + lineStartTokens + ")");
        System.out.println("  rows == [list of " + rows.size() + " rows]");
        System.out.println("  " + ((System.currentTimeMillis() - t1) / 1000.0) + " seconds elapsed time");
        return rows;
    }

    /** ***************************************************************
     * Parse a CSV file into an ArrayList of ArrayLists.  If
     * lineStartTokens is a non-empty list, all lines not starting
     * with one of the String tokens it contains will be concatenated.
     *
     * @param fname The pathname of the CSV file to be processed
     *
     * @param lineStartTokens If a List containing String tokens, all
     * lines not starting with one of the tokens will be concatenated
     * @param quote signifies whether to retain quotes in elements
     * @return An ArrayList of ArrayLists
     */
    public static List<List<String>> readSpreadsheet(String fname, List lineStartTokens,
            boolean quote, char delimiter) {

        System.out.println("ENTER DB.readSpreadsheet(" + fname + ", " + lineStartTokens + ")");
        List<List<String>> rows = new ArrayList<>();
        try (FileReader fr = new FileReader(fname)) {
            rows = readSpreadsheet(fr, lineStartTokens,quote,delimiter);
        }
        catch (Exception e) {
            System.err.println("Error in DB.readSpreadsheet()");
            System.err.println(e.getMessage());
            e.printStackTrace();
        }
        System.out.println("EXIT DB.readSpreadsheet(" + fname + ", " + lineStartTokens + ")");
        return rows;
    }

    /** ***************************************************************
     */
    public static List<List<String>> readSpreadsheet(String fname, List lineStartTokens,
            boolean quote) {

    	return readSpreadsheet(fname,lineStartTokens,quote,',');
    }

    /** ***************************************************************
     */
    private static boolean isInteger(String input) {

       try {
          Integer.valueOf(input);
          return true;
       }
       catch (NumberFormatException e) {
          return false;
       }
    }

    /** ***************************************************************
     * @param quote signifies whether to quote entries from the spreadsheet
     */
    public static String writeSpreadsheetLine(List<String> al, boolean quote) {

        StringBuilder result = new StringBuilder();
        String s;
        for (int j = 0; j < al.size(); j++) {
            s = al.get(j);
            if (quote && !isInteger(s))
                result.append("\"").append(s).append("\"");
            else
                result.append(s);
            if (j < al.size())
                result.append(",");
        }
        result.append("\n");
        return result.toString();
    }

    /** ***************************************************************
     * @param quote signifies whether to quote entries from the spreadsheet
     */
    public static String writeSpreadsheet(List<List<String>> values, boolean quote) {

        StringBuilder result = new StringBuilder();
        List<String> al;
        for (int i = 0; i < values.size(); i++) {
            al = values.get(i);
            result.append(writeSpreadsheetLine(al,  quote));
        }
        return result.toString();
    }

    /** ***************************************************************
     * Parse an input stream Reader from a Data Interchange Format
     * (.dif) file into an ArrayList of ArrayLists.
     *
     * @param inReader A reader created from the .dif file to be processed
     *
     * @return An ArrayList of ArrayLists
     */
    public static List<List> readDataInterchangeFormatFile(Reader inReader) {

        long t1 = System.currentTimeMillis();
        System.out.println("ENTER DB.readDataInterchangeFormatFile(" + inReader + ")");
        List<List> rows = new ArrayList<>();
        try (LineNumberReader lr = new LineNumberReader(inReader)) {
            List row = new ArrayList();
            String token = "";
            String val = "";
            String prevVal;
            boolean beginningOfTuple = false;
            boolean inHeader = false;
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = lr.readLine()) != null) {
                try {
                    if (StringUtil.containsNonAsciiChars(line))
                        System.out.println("NonASCII char near line " + lr.getLineNumber() + ": "
                                + line);
                    prevVal = val;
                    // Remove only one layer of quotes.
                    // line = line.trim();
                    val = StringUtil.removeEnclosingChars(line, 1, '"');
                    // val = val.trim();
                    if (val.matches(".*\\w+.*"))
                        val = val.trim();
                    if (val.equals("TABLE")) {
                        token = val;
                        inHeader = true;
                        continue;
                    }
                    if (val.equals("BOT")) {
                        token = val;
                        inHeader = false;
                        beginningOfTuple = true;
                        sb.setLength(0);
                        row = new ArrayList();
                        continue;
                    }
                    if (val.equals("-1,0")) {
                        token = val;
                        if (!inHeader && !beginningOfTuple) {
                            row.add(sb.toString());
                            sb.setLength(0);
                            rows.add(row);
                        }
                        beginningOfTuple = false;
                        continue;
                    }
                    if (val.equals("1,0")) {
                        token = val;
                        if (!inHeader && !beginningOfTuple) {
                            row.add(sb.toString());
                            sb.setLength(0);
                        }
                        beginningOfTuple = false;
                        continue;
                    }
                    if (val.startsWith("0,")) {
                        token = "0,";
                        if (!inHeader && !beginningOfTuple) {
                            row.add(sb.toString());
                            sb.setLength(0);
                        }
                        val = val.substring(2);
                        beginningOfTuple = false;
                        continue;
                    }
                    if (val.equals("V")) {
                        token = val;
                        if (sb.length() > 0) sb.append(" ");
                        sb.append(prevVal);
                        continue;
                    }
                    if (val.equals("EOD")) {
                        token = val;
                        row.add(sb.toString());
                        rows.add(row);
                        break;
                    }
                    if (token.equals("1,0")) {
                        if (sb.length() > 0) sb.append(" ");
                        sb.append(val);
                    }
                }
                catch (Exception ex1) {
                    System.err.print("ERROR in DB.readDataInterchangeFormatFile(");
                    System.err.println(inReader + ")");
                    System.err.println("  approx. line # == " + lr.getLineNumber());
                    ex1.printStackTrace();
                }
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        System.out.println("EXIT DB.readDataInterchangeFormatFile(" + inReader + ")");
        System.out.println("  rows == [list of " + rows.size() + " rows]");
        System.out.println("  "  + ((System.currentTimeMillis() - t1) / 1000.0) + " seconds elapsed time");
        return rows;
    }

    /** ***************************************************************
     * Parse and load a Data Interchange Format (.dif) file into an
     * ArrayList of ArrayLists.
     *
     * @param fname The pathname of the file to be processed
     *
     * @return An ArrayList of ArrayLists
     */
    public static List<List> readDataInterchangeFormatFile(String fname) {

        System.out.println("ENTER DB.readDataInterchangeFormatFile(" + fname + ")");
        List<List> rows = new ArrayList<>();
        try (Reader fr = new FileReader(fname)) {
            rows = readDataInterchangeFormatFile(fr);
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        System.out.println("EXIT DB.readDataInterchangeFormatFile(" + fname + ")");
        return rows;
    }

    /** ***************************************************************
     */
    private void processForRDFExport(List rows) {

        //System.out.println("<!-- Begin Export -->");
        //System.out.println(rows.size());
        System.out.println("<rdf:RDF");
        System.out.println("  xmlns:rdf=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\"");
        String kbName = KBmanager.getMgr().getPref("sumokbname");
        System.out.println("  xmlns:op=\"http://ontologyportal.org/" + kbName + ".owl.txt\">");
        String domain;
//        String subject;
        String relator;
        String range;
        List row;
        for (int rowNum = 0; rowNum < rows.size(); rowNum++) {
            row = (ArrayList) rows.get(rowNum);
            if (row.size() > 7) {
                domain = (String) row.get(4);
                relator = (String) row.get(5);
                range = (String) row.get(7);
                if (!StringUtil.emptyString(domain)
                        && !StringUtil.emptyString(relator)
                        && !StringUtil.emptyString(range)) {
                    System.out.println("  <rdf:Description rdf:about=\"" + domain + "\">");
                    System.out.println("    <op:" + relator + ">" + range + "</op:" + relator + ">");
                    System.out.println("  </rdf:Description>");
                }
            }
        }
        System.out.println("</rdf:RDF>");
    }

    /** ***************************************************************
     */
    public static int writeSuoKifStatements(Set statements, PrintWriter pw) {

        long t1 = System.currentTimeMillis();
        System.out.println("ENTER DB.writeSuoKifStatements([" + statements.size() + " statements], " + pw.toString() + ")");
        int n = 0;
        try {
            if (!statements.isEmpty()) {
                System.out.println("  writing ");
                String stmt;
                Formula printF = new Formula();
                for (Iterator it = statements.iterator(); it.hasNext(); n++) {
                    stmt = StringUtil.normalizeSpaceChars((String) it.next());
                    if (stmt.startsWith("(contentRegexPattern"))
                        stmt = StringUtil.escapeEscapeChars(stmt);
                    printF.read(stmt);
                    pw.println(printF.toString());
                    pw.println("");
                    if ((n % 100) == 1) System.out.print(".");
                }
                System.out.println("x: " + n + " statements written");
            }
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
        System.out.println("EXIT DB.writeSuoKifStatements([" + statements.size() + " statements], " + pw.toString() + ")");
        System.out.println("  n == " + n);
        System.out.println("  " + ((System.currentTimeMillis() - t1) / 1000.0) + " seconds elapsed time");
        return n;
    }

    /** ***************************************************************
     * Writes to sourceFilePath all Formulae in kb that have
     * sourceFilePath as source file.
     *
     * @param kb The KB from which Formulae will be written
     *
     * @param sourceFilePath The canonical pathname of the file to
     * which Formulae will be written
     *
     * @return An int denoting the number of expressions saved to the
     * file named by sourceFilePath
     */
    public static int writeSuoKifStatements(KB kb, String sourceFilePath) {

        long t1 = System.currentTimeMillis();
        System.out.println("ENTER DB.writeSuoKifStatements(" + kb.name + ", " + sourceFilePath + ")");
        boolean foundFirstOne = false;
        PrintWriter pw = null;
        int count = 0;
        try {
            File sourceFile = new File(sourceFilePath);
            String canonicalPath = sourceFile.getCanonicalPath();
            System.out.print("  writing " + canonicalPath + " ");
            Formula printF = new Formula();
            Formula f;
            String stmt, pathname;
            for (Iterator it = kb.formulaMap.values().iterator(); it.hasNext();) {
                f = (Formula) it.next();
                pathname = f.getSourceFile();
                if (pathname.equals(canonicalPath)) {
                    if (!foundFirstOne) {
                        pw = new PrintWriter(new FileWriter(canonicalPath));
                        foundFirstOne = true;
                    }
                    printF.read(StringUtil.normalizeSpaceChars(f.getFormula()));
                    if (printF.getStringArgument(0).equalsIgnoreCase("contentRegexPattern"))
                        printF.read(StringUtil.escapeEscapeChars(printF.getFormula()));
                    // stmt = StringUtil.removeEscapedEscapes(stmt);
                    pw.println(printF.toString());
                    pw.println("");
                    if ((count++ % 100) == 1) System.out.print(".");
                }
            }
            System.out.println("x: " + count + " statements written");
        }
        catch (IOException ex) {
            System.err.println("Error writing file " + sourceFilePath);
            ex.printStackTrace();
        }
        finally {
            try {
                if (pw != null) {
                    pw.close();
                }
            }
            catch (Exception ioe) {
                ioe.printStackTrace();
            }
        }
        System.out.println("EXIT DB.writeSuoKifStatements(" + kb.name + ", " + sourceFilePath + ")");
        System.out.println("  count == " + count);
        System.out.println("  "  + ((System.currentTimeMillis() - t1) / 1000.0) + " seconds elapsed time");
        return count;
    }

    /** ***************************************************************
     *  Collect relations in the knowledge base
     *
     *  @return The set of relations in the knowledge base.
     */
    private List getRelations(KB kb) {

        List relations = new ArrayList();
        synchronized (kb.getTerms()) {
            for (String term : kb.getTerms()) {
                if (kb.isInstanceOf(term, "Predicate"))
                    relations.add(term.intern());
            }
        }
        return relations;
    }

    /** ***************************************************************
     * Print a comma-delimited matrix.  The values of the rows
     * are TreeMaps, whose values in turn are Strings.  The ArrayList of
     * relations forms the column headers, which are Strings.
     *
     * @param rows - the matrix
     *
     * @param relations - the relations that form the column header
     */
    public void printSpreadsheet(Map rows, List relations) {

        StringBuilder line = new StringBuilder();
        line.append("Domain/Range,");
        String relation, term;
        for (int i = 0; i < relations.size(); i++) {
            relation = (String) relations.get(i);
            line.append(relation);
            if (i < relations.size()-1)
                line.append(",");
        }
        System.out.println(line);
        Iterator it = rows.keySet().iterator();
        Map row;
        while (it.hasNext()) {
            term = (String) it.next();
            row = (TreeMap) rows.get(term);
            System.out.print(term + ",");
            for (int i = 0; i < relations.size(); i++) {
                relation = (String) relations.get(i);
                if (row.get(relation) == null)
                    System.out.print(",");
                else {
                    System.out.print((String) row.get(relation));
                    if (i < relations.size()-1)
                        System.out.print(",");
                }
                if (i == relations.size()-1)
                    System.out.println();
            }
        }
    }

    /** ***************************************************************
     * Export a comma-delimited table of all the ground binary
     * statements in the knowledge base.  Only the relations that are
     * actually used are included in the header.
     *
     *  @param kb The knowledge base.
     */
    public void exportTable(KB kb) {

        List relations = getRelations(kb);
        List usedRelations = new ArrayList();
        Map rows = new TreeMap();
        Map row;
        String term, arg1, arg2, element;
        List statements;
        Formula f;
        for (Iterator itr = relations.iterator(); itr.hasNext();) {
            term = (String) itr.next();
            statements = kb.ask("arg",0,term);
            if (statements != null) {
//                row = new TreeMap();
                for (Iterator its = statements.iterator(); its.hasNext();) {
                    f = (Formula) its.next();
                    arg1 = f.getStringArgument(1);
                    if (Character.isUpperCase(arg1.charAt(0)) && !arg1.endsWith(Formula.FN_SUFF)) {
                        if (!usedRelations.contains(term))
                            usedRelations.add(term);
                        arg2 = f.getStringArgument(2);
                        if (rows.get(f.getArgument(1)) == null) {
                            row = new TreeMap();
                            rows.put(arg1,row);
                        }
                        else
                            row = (TreeMap) rows.get(arg1);
                        if (row.get(term) == null)
                            row.put(term,f.getArgument(2));
                        else {
                            element = (String) row.get(term);
                            element = element + "/" + f.getArgument(2);
                            row.put(term,element);
                        }
                    }
                }
            }
        }
        printSpreadsheet(rows,usedRelations);
    }

    /** ***************************************************************
     */
    private String replaceStringWithID(int counter, String arg, HashMap stringMap) {

        String id = ("String" + counter);
        stringMap.put(id,arg);
        return id;
    }

    /** *******************************************************************
     */
    public static String wordWrap(String input, int length) {
        return StringUtil.wordWrap(input, length);
    }

    /** *******************************************************************
     */
    public static boolean emptyString(String input) {
        return StringUtil.emptyString(input);
    }

    /** *******************************************************************
     */ // TODO: not used
    public static void RearDBtoKIF() {

        LineNumberReader lnr = null;
        PrintWriter pw = null;
        // key is table name, value is a list of relation names for the table
        Map<String,List> tables = new TreeMap<>();
        // key is relation name, value is table names that share the relation name
        Map<String,List<String>> ids = new TreeMap<>();
        // Each of the fields in the table, indexed by tableName+fieldName
        Map<String,String> fields = new TreeMap<>();
        try {
            File fin  = new File("RearDB.csv");
            Reader fr = new FileReader(fin);
            File fout  = new File("Rear.kif");
            pw = new PrintWriter(fout);
            if (fr != null) {
                lnr = new LineNumberReader(fr);
                String line;
                String oldCell, newCell;
                int count = 0;
                while ((line = lnr.readLine()) != null) {
                    line = line.trim();
                    Pattern p = Pattern.compile("([^,]*),([^,]*),([^,]*),([^,]*),([^,]*),([^,]*)");
                    Matcher m = p.matcher(line);
                    if (m.matches()) {
                        count++;
                        String table = StringUtil.asSUMOID(StringUtil.removeEnclosingQuotes(m.group(1)));
                        String unprocessedRelation = StringUtil.removeEnclosingQuotes(m.group(2));
                        String relation = StringUtil.asSUMORelationID(StringUtil.asSUMOID(unprocessedRelation));
                        String dataType = StringUtil.removeEnclosingQuotes(m.group(3));
                        String dataLength = StringUtil.removeEnclosingQuotes(m.group(4));
                        String SUMO = StringUtil.removeEnclosingQuotes(m.group(5));
                        String comment = m.group(6);
                        fields.put(table + "|" + relation,dataType);
                        fields.put(table + "|" + relation,dataLength);
                        fields.put(table + "|" + relation,SUMO);
                        fields.put(table + "|" + relation,comment);
                        if (!StringUtil.emptyString(table)) {
                            if (tables.keySet().contains(table)) {
                                List al = tables.get(table);
                                al.add(relation);
                            }
                            else {
                                List al = new ArrayList();
                                al.add(relation);
                                tables.put(table,al);
                            }
                            if (!StringUtil.emptyString(relation)) {
                                if (relation.endsWith("_ID")) {
                                    pw.println(";; ID relation: " + relation);
                                    if (ids.keySet().contains(relation)) {
                                        List al = ids.get(relation);
                                        al.add(table);
                                    }
                                    else {
                                        List al = new ArrayList();
                                        al.add(table);
                                        ids.put(relation,al);
                                    }
                                }
                            }
                        }
                    }
                    //else
                    //    System.out.println("error: bad line: " + line);
                }
                pw.println(";; " + count + " lines read from .csv file");
                pw.println(";; class defs");
                for (String key : tables.keySet()) {
                    List value = (ArrayList) tables.get(key);
                    pw.println("(subclass " + key + " Entity)");
                    for (int i = 0; i < value.size(); i++) {
                        String field = (String) value.get(i);
                    }
                }
                pw.println(";; related");
                for (String key : ids.keySet()) {
                    List value = (ArrayList) ids.get(key);
                    pw.print("(" + key);
                    for (int i = 0; i < value.size(); i++) {
                        String t = (String) value.get(i);
                        pw.print(" " + t);
                    }
                    pw.println(")");
                }
            }
            else
                System.out.println("Error file not found");
            pw.flush();
        }
        catch (IOException ioe) {
            System.err.println("File error: " + ioe.getMessage());
        }
        finally {
            try {
                if (lnr != null) lnr.close();
            }
            catch (IOException e) {
                System.err.println("Exception in RearDBtoKIF()");
            }
            try {
                if (pw != null) pw.close();
            }
            catch (Exception e) {
                System.err.println("Exception in RearDBtoKIF()");
            }
        }
    }

    /** *******************************************************************
     */
    public static String parseCuisines(String cuisine, String RST_RESTAURANTNAME, String RST_RESTAURANTID) {

        StringBuilder result = new StringBuilder();
        cuisine = StringUtil.removeEnclosingQuotes(cuisine);
        String[] al = cuisine.split(",");
        for (String al1 : al) {
            result.append("(=>\n  (and\n    (instance ?X Eating)\n    (located ?X Rest-").append(RST_RESTAURANTNAME).append("-").append(RST_RESTAURANTID).append(")\n    (instance ?Y (FoodForFn Human))\n    (patient ?X ?Y))\n  (attribute ?Y ").append(StringUtil.stringToKIF(al1.trim(), true)).append("Cuisine))\n");
        }
        return result.toString();
    }

    /** *************************************************************
     * Excludes cases of where the mapping is to multiple SUMO terms
     *
     * @return a list of attribute value pairs where the count is in
     * the attribute and the SUMO term is the value
     */
    public static List<AVPair> topSUMOInReviews(List<Hotel> reviews) {

        System.out.println("INFO in topSUMOInReviews()");
        List<AVPair> result = new ArrayList<>();
        Map<String,Integer> countMap = wordSensesInReviews(reviews);
        Map<String,Integer> newCountMap = new HashMap<>();
        String SUMO;
        int val1, val2;
        for (String s : countMap.keySet()) {
            SUMO = WordNet.wn.getSUMOMapping(s);
            SUMO = WordNetUtilities.getBareSUMOTerm(SUMO);
            if (!StringUtil.emptyString(SUMO) && !SUMO.contains("&%") && !stopConcepts.contains(SUMO)) {
                if (newCountMap.keySet().contains(SUMO)) {
                    val1 = newCountMap.get(SUMO);
                    val2 = countMap.get(s);
                    newCountMap.put(SUMO, val1 + val2);
                }
                else {
                    val2 = countMap.get(s);
                    newCountMap.put(SUMO, val2);
                }
            }
            if (newCountMap.keySet().size() % 100 == 0)
                System.out.print('.');
        }
        System.out.println();
        Set<String> resultSet = new HashSet<>(); // set of synsets that have been examined
        int max, count;
        String maxSUMO;
        AVPair avp;
        while (resultSet.size() < newCountMap.keySet().size()) {
            max = 0;
            maxSUMO = "";
            for (String SMO : newCountMap.keySet()) {
                if (!resultSet.contains(SMO)) {
                    count = newCountMap.get(SMO);
                    if (count > max) {
                        max = count;
                        maxSUMO = SMO;
                    }
                }
            }
            resultSet.add(maxSUMO);
            //System.out.println("INFO in topSUMOInReviews(): " + maxSUMO);
            if (!StringUtil.emptyString(maxSUMO)) {
                avp = new AVPair();
                avp.attribute = String.valueOf(max);
                avp.value = maxSUMO;
                result.add(avp);
            }
        }
        return result;
    }

    /** *************************************************************
     * @return a map of all the word senses used in the reviews and
     * a count of their appearances
     */
    public static Map<String,Integer> wordSensesInReviews(List<Hotel> reviews) {

        System.out.println("INFO in wordSensesInReviews()");
        disambigReviews(reviews);
        Map<String,Integer> countMap = new HashMap<>();  // synset, count of usages
        int i = 0;
        for (Hotel h : reviews) {
            if (h != null && h.senses != null) {
                for (String synset : h.senses.keySet()) {
                    if (countMap.keySet().contains(synset)) {
                        Integer in = countMap.get(synset);
                        countMap.put(synset, in + h.senses.get(synset));
                    }
                    else
                        countMap.put(synset, 1);
                    if (i % 100 == 0)
                        System.out.println('.');
                    i++;
                }
            }
        }
        System.out.println();
        return countMap;
    }

    /** *************************************************************
     * process a side effect on reviews of setting the SUMO list
     */
    public static void SUMOReviews(List<Hotel> reviews) {

        Hotel h;
        String SUMO;
        for (int i = 0; i < reviews.size(); i++) {
            h = reviews.get(i);
            for (String s : h.senses.keySet()) {
                SUMO = WordNet.wn.getSUMOMapping(s);
                SUMO = WordNetUtilities.getBareSUMOTerm(SUMO);
                if (!StringUtil.emptyString(SUMO)) {
                    if (!h.SUMO.keySet().contains(SUMO))
                        h.SUMO.put(SUMO,h.senses.get(s));
                    else
                        h.SUMO.put(SUMO, h.senses.get(s) + h.SUMO.get(SUMO));
                }
            }
        }
    }

    /** *************************************************************
     * @param hotels an ArrayList of Hotel with reviews as text
     * process synset values as a side effect
     */
    public static void disambigReviews(List<Hotel> hotels) {

        /*
        System.out.println("INFO in disambigReviews()");
        try {
            KBmanager.getMgr().initializeOnce();
        } catch (Exception ex) {
            System.out.println(ex.getMessage());
        }
        WordNet.wn.initOnce();
*/
        Map<String,Integer> senses;
        Hotel h;
        String r;
        for (int i = 0; i < hotels.size(); i++) {
            h = hotels.get(i);
            if (h != null && h.reviews != null) {
                for (int j = 0; j < h.reviews.size(); j++) {
                    r = h.reviews.get(j);
                    if (!StringUtil.emptyString(r)) {
                        senses = WordNet.wn.collectCountedWordSenses(r);
                        h.addAllSenses(senses);
                    }
                }
            }
            if (i % 10 == 0)
                System.out.print('.');
        }
        System.out.println();
    }

    /** *******************************************************************
     */
    public static String processTimeDate(String timeDate) {

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss"); // "2009-03-19 17:30:00"
        Date d = null;
        try {
            d = sdf.parse(timeDate);
        }
        catch (ParseException pe) {
            System.out.println("Error in DB.processTimeDate(): error parsing date/time string: " + timeDate);
        }
        Calendar calendar = new GregorianCalendar();
        if (d == null)
            return null;
        calendar.setTime(d);
        return "(SecondFn " + calendar.get(Calendar.SECOND) +
        " (MinuteFn " + calendar.get(Calendar.MINUTE) +
        " (HourFn " + calendar.get(Calendar.HOUR_OF_DAY) +
        " (DayFn " + calendar.get(Calendar.DAY_OF_MONTH) +
        " (MonthFn " + calendar.get(Calendar.MONTH) +
        " (YearFn " + calendar.get(Calendar.YEAR) + "))))))";
    }

    /** *******************************************************************
     */
    public static Map<String,String> readStateAbbrevs() {

        Map<String,String> result = new HashMap<>();
        List<List<String>> st = DB.readSpreadsheet("states.csv",null,false);
        List<String> al;
        String stateName, abbrev;
        for (int i = 1; i < st.size(); i++) {
            al = st.get(i);
            stateName = ((String) al.get(0)).trim();
            abbrev = ((String) al.get(1)).trim();
            result.put(stateName,abbrev);
        }
        return result;
    }

    /** *******************************************************************
     */
    public static List<String> fill(String value, int count) {

        List<String> line = new ArrayList<>();
        for (int i = 0; i < count; i++)
            line.add(value);
        return line;
    }

    /** *******************************************************************
     */ // TODO: not used
    public static void DiningDBImport() {

        List<List<String>> f = DB.readSpreadsheet("dining.csv",null,false);
        List<String> al;
        String RST_PROCESS_ID, RST_CUSTOMER_ID, RST_COMPANY_NAME, RST_ADDRESSCITY, RST_ADDRESSPOSTALCODE, RST_ADDRESSSTATE, RST_CUISINETYPE, RST_PARTYSIZE, RST_RESTAURANTID, RST_RESTAURANTNAME, RST_RESERVATION_TIME;
        for (int i = 0; i < f.size(); i++) {
            al = f.get(i);
            RST_PROCESS_ID          = (String) al.get(5);
            RST_CUSTOMER_ID         = (String) al.get(10);
            System.out.println("(instance Human-" + RST_CUSTOMER_ID + " Human)");
            RST_COMPANY_NAME        = (String) al.get(28);
            //RST_COMPANY_NAME = RST_COMPANY_NAME;
            RST_COMPANY_NAME = StringUtil.stringToKIF(RST_COMPANY_NAME,true);
            System.out.println("(instance " + RST_COMPANY_NAME + " Organization)");
            System.out.println("(employs " + RST_COMPANY_NAME + " Human-" + RST_CUSTOMER_ID + ")");
            RST_ADDRESSCITY         = (String) al.get(41);
            //RST_ADDRESSCITY = RST_ADDRESSCITY;
            RST_ADDRESSCITY = StringUtil.stringToKIF(RST_ADDRESSCITY,true);
            RST_ADDRESSPOSTALCODE   = (String) al.get(42);
            RST_ADDRESSSTATE        = (String) al.get(43);
            RST_CUISINETYPE         = (String) al.get(45);
            RST_PARTYSIZE           = (String) al.get(47);
            System.out.println("(and\n  (instance Eat-" + RST_PROCESS_ID + " Eating)\n  (instance Group-" + RST_PROCESS_ID + " Group)\n" +
                    "  (experiencer Eat-" + RST_PROCESS_ID + " Group-" + RST_PROCESS_ID + ")\n"+
                    "  (memberCount Group-" + RST_PROCESS_ID + " " + RST_PARTYSIZE + "))");
            //String RST_PRICERANGE          = (String) al.get(48);
            //System.out.println("(instance Rest-" + RST_COMPANY_NAME + " Restaurant)");
            RST_RESTAURANTID          = (String) al.get(49);
            RST_RESTAURANTNAME      = (String) al.get(50);
            //RST_RESTAURANTNAME = RST_RESTAURANTNAME;
            RST_RESTAURANTNAME = StringUtil.stringToKIF(RST_RESTAURANTNAME,true);
            System.out.println("(postCity " + RST_ADDRESSCITY + " Rest-" + RST_RESTAURANTNAME + "-" + RST_RESTAURANTID + ")");
            System.out.println("(postPostcode " + RST_ADDRESSPOSTALCODE + " Rest-" + RST_RESTAURANTNAME + "-" + RST_RESTAURANTID + ")");
            System.out.println("(postState " + RST_ADDRESSSTATE + " Rest-" + RST_RESTAURANTNAME + "-" + RST_RESTAURANTID + ")");
            System.out.println("(instance Rest-" + RST_RESTAURANTNAME + "-" + RST_RESTAURANTID + " Restaurant)");
            System.out.println(parseCuisines(RST_CUISINETYPE,RST_RESTAURANTNAME,RST_RESTAURANTID));
            System.out.println("(located Eat-" + RST_PROCESS_ID + " Rest-" + RST_RESTAURANTNAME + "-" + RST_RESTAURANTID + ")");
            RST_RESERVATION_TIME    = (String) al.get(52);
            //RST_RESERVATION_TIME = RST_RESERVATION_TIME;
            String timeDateFormula = processTimeDate(RST_RESERVATION_TIME);
            System.out.println("(equals (BeginFn Eat-" + RST_PROCESS_ID + ") " + timeDateFormula + ")");
            System.out.println();
            System.out.println();
        }
    }

    /** *******************************************************************
     * @return a list of SUMO terms that are the best guess at classes for
     * each word
     */
    public static List<String> getWordSenses(List<String> al) {

        //System.out.println("INFO in DB.getWordSenses()");
        List<String> result = new ArrayList<>();
        String term;
        for (int i = 0; i < al.size(); i++) {
            term = WordNet.wn.getSUMOMapping(WSD.findWordSenseInContext(al.get(i),al));
            if (!DB.emptyString(term))
                result.add(term);
            else {
                term = WSD.getBestDefaultSense(al.get(i));
                if (!DB.emptyString(term))
                    result.add(term);
            }
            //System.out.println("INFO in DB.getWordSenses(): word: " + al.get(i) + " term: " + term);
        }
        return result;
    }

    /** *******************************************************************
     * @return a list of SUMO terms that are the best guess at classes for
     * each word
     */
    public static List<String> getFoodWordSenses(List<String> al) {

        KB kb = KBmanager.getMgr().getKB(KBmanager.getMgr().getPref("sumokbname"));
        String foodSynset1 = "107555863"; // food, solid_food
        String foodSynset2 = "100004475"; // being, organism
        //System.out.println("INFO in DB.getFoodWordSenses()");
        List<String> result = new ArrayList<>();
        Map<String,List<String>> al2;
        List<String> al3;
        boolean equivalence;
        String[] terms;
        String term, aterm;
        for (int i = 0; i < al.size(); i++) {
            //System.out.println("\nINFO in DB.getFoodWordSenses(): word: " + al.get(i));
            // TreeMap of word keys and values that are ArrayLists of synset Strings
            al2 = WordNet.wn.getSenseKeysFromWord(al.get(i));
            equivalence = false;
            for (String word : al2.keySet()) {
                al3 = al2.get(word);
                for (int j = 0; j < al3.size(); j++) {
                    term = WordNet.wn.getSUMOMapping(al3.get(j));
                    if (!DB.emptyString(term)) {
                        terms = term.split(" ");                     // have to split on space - may be multiple mappings
                        for (String term1 : terms) {
                            if (term.charAt(term.length() - 1) == '=')
                                equivalence = true;
                            aterm = WordNetUtilities.getBareSUMOTerm(term1);
                            if (WordNet.wn.isHyponym(al3.get(j),foodSynset1) ||
                                    WordNet.wn.isHyponym(al3.get(j),foodSynset2))
                                if (!kb.isChildOf(aterm,"Human") && !kb.isChildOf(aterm,"GroupOfPeople") &&
                                        !kb.isChildOf(aterm,"Food") && !kb.isChildOf(aterm,"Attribute"))
                                    result.add(aterm);
                            //System.out.println("INFO in DB.getFoodWordSenses(): word: " + WordNet.wn.getWordsFromSynset(al3.get(j)) + " term: " + term);
                        }
                    }
                }
            }
            if (!equivalence)
                System.out.println(";; need equivalence for word: " + al.get(i));
        }
        return result;
    }

    /** *******************************************************************
     */
    public static Set<String> parseRest(String menu, String placename, String price,
            String address, String latitude, String longitude, String phone) {

        Set<String> axioms = new HashSet();
        placename = StringUtil.stringToKIF(placename,true);
        if (!StringUtil.emptyString(phone))
            axioms.add("(telephoneNumber \"" + phone + "\" " + placename + ")");
        if (!StringUtil.emptyString(phone))
            axioms.add("(postAddressText \"" + address + "\" " + placename + ")");
        switch (price) {
            case "$":
                price = "CheapMenu";
                break;
            case "$$":
                price = "InexpensiveMenu";
                break;
            case "$$$":
                price = "ModerateMenu";
                break;
            case "$$$$":
                price = "ExpensiveMenu";
                break;
            case "$$$$$":
                price = "VeryExpensiveMenu";
                break;
            default:
                break;
        }
        if (!StringUtil.emptyString(phone))
            axioms.add("(ratingAttribute " + price + " " + placename + " MenuPagesCom)");
        axioms.add("(serves " + placename + " " + placename + "-menu)");
        axioms.add("(instance " + placename + " Restaurant)");
        axioms.add("(instance " + placename + "-menu Menu)");
        menu = menu.replaceAll("&[^;]+;","");
        Pattern p = Pattern.compile("(<[^>]+>)([^<]*)");
        Matcher m = p.matcher(menu);
        int fieldCount = 0;
        String menuItem = null;
        String code, content, term;
        Pattern p2;
        Matcher m2;
        String description;
        String[] words;
        List<String> al = new ArrayList<>(), terms;
        while (m.find()) {
            code = m.group(1);
            content = m.group(2);
            p2 = Pattern.compile("[^\\d]*(\\d*\\.\\d\\d)");
            m2 = p2.matcher(content);
            if (m2.matches())
                axioms.add("(itemPrice " + menuItem + " (MeasureFn " + m2.group(1) + " UnitedStatesDollar))");
            else if (Pattern.matches("</tr>", code)) {
                System.out.println();
                fieldCount = 0;
            }
            else if (content.trim().length() > 0) {
                if (fieldCount == 0) {
                    fieldCount++;
                    menuItem = StringUtil.stringToKIF(content.trim(),true);
                    axioms.add("(subclass " + menuItem + " (FoodForFn Human))");
                    axioms.add("(menuItem " + menuItem + " " + placename + "-menu)");
                }
                else
                    axioms.add("(documentation " + menuItem + " EnglishLanguage \"" + content.trim() + "\")");
                description = WordNet.wn.removeStopWords(content.trim());
                description = StringUtil.removePunctuation(description);
                words = description.split(" ");
                //System.out.println("INFO in DB.parseRestaurantMenu(): content: " + content);
                //System.out.println("INFO in DB.parseRestaurantMenu(): words: " + words);
                //System.out.println("INFO in DB.parseRestaurantMenu(): num words: " + words.length);
                al.clear();
                al.addAll(Arrays.asList(words));
                //System.out.println("INFO in DB.parseRestaurantMenu(): al: " + al);
                terms = getFoodWordSenses(al);
                //System.out.println("INFO in DB.parseRestaurantMenu(): terms: " + terms);
                for (int i = 0; i < terms.size(); i++) {
                    term = terms.get(i);
                    axioms.add("(ingredient-PreparedFood " + term + " " + menuItem + ")");
                }
            }
        }
        return axioms;
    }

    /** *************************************************************
     */
    public static Set<String> getAllRest() {

        Set<String> result = new HashSet<>();
        File dir = new File(".");
        String[] children = dir.list();
        File f;
        if (children != null) {
            for (String children1 : children) {
                if (!children1.equals(".")) {
                    f = new File(children1 + File.separator + "menu");
                    if (f.exists()) {
                        result.add(children1 + File.separator + "menu");
                    }
                }
            }
        }
        return result;
    }

    /** *************************************************************
     * Call Google's geocode API to convert an address string into a
     * lat/lon, which is returned as an ArrayList of two String elements
     * containing a real-number format latitude and longitude.
     */
    public static List<String> geocode(String address) {

        List<String> result = new ArrayList<>();
        String urlStart = "http://maps.googleapis.com/maps/api/geocode/xml?address=";
        String urlEnd = "&sensor=false";
        String encoded;
        try {
            encoded = URLEncoder.encode(address,"US-ASCII");
        }
        catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            return result;
        }
        URL url;
        try {
            url = new URL(urlStart + encoded + urlEnd);
            try (InputStream is = url.openStream()) {  // throws an IOException
                SimpleDOMParser sdp = new SimpleDOMParser();
                SimpleElement se = sdp.parse(new LineNumberReader(new InputStreamReader(is)));
                SimpleElement res = se.getChildByFirstTag("result");
                if (res != null) {
                    SimpleElement geo = res.getChildByFirstTag("geometry");
                    if (geo != null) {
                        SimpleElement loc = geo.getChildByFirstTag("location");
                        if (loc != null) {
                            SimpleElement lat = loc.getChildByFirstTag("lat");
                            result.add(lat.getText());
                            SimpleElement lng = loc.getChildByFirstTag("lng");
                            result.add(lng.getText());
                        }
                    }
                }
            }
        }
        catch (MalformedURLException mue) {
             mue.printStackTrace();
        }
        catch (IOException ioe) {
             ioe.printStackTrace();
        }
        //System.out.println("INFO in DB.geocode(): " + address);
        if (result != null && result.size() == 2 &&
            !StringUtil.emptyString(result.get(0)) && !StringUtil.emptyString(result.get(1)))
            System.out.println(result.get(0) + ":" + result.get(1));
        else {
            System.out.println("geocode fail");
            return null;
        }
        return result;
    }

    /** *************************************************************
     */
    public static String printTopSUMOInReviews(List<AVPair> topSUMO) {

        StringBuilder result = new StringBuilder();
        AVPair avp;
        for (int i = 0; i < topSUMO.size(); i++) {
            avp = topSUMO.get(i);
            result.append(avp.attribute).append(" ").append(avp.value).append("\n");
        }
        return result.toString();
    }

    /** *************************************************************
     * A test method.
     * @param fname has no file extension or directory
     */
    public static Set<String> parseOneRestFile(String fname) {

        String menu = "";
        Pattern pMenu = Pattern.compile("restaurant.menu");
        String placename = "";
        Pattern pPlacename = Pattern.compile(".h2 class..fn org..([^<]+)");
        String price = "";
        Pattern pPrice = Pattern.compile(".acronym class..price.key pricerange..([^<]+)");
        String address = "";
        Pattern pAddress = Pattern.compile("span class..addr street.address..([^<]+)");
        String latitude = "";
        Pattern pLatitude = Pattern.compile("latitude..([^<]+)");
        String longitude = "";
        Pattern pLongitude = Pattern.compile("longitude..([^<]+)");
        String phone = "";
        Pattern pPhone = Pattern.compile("li class..phone..Phone: .string.([^<]+)");

        File fin  = new File(fname);
        try (Reader fr = new FileReader(fin)) {
            if (fr != null) {
                try (LineNumberReader lnr = new LineNumberReader(fr)) {
                    String line;
                    boolean done = false;
                    Matcher mMenu, mPlacename, mPrice, mAddress, mLatitude, mLongitude, mPhone;
                    while ((line = lnr.readLine()) != null && !done) {
                        line = line.trim();
                        mMenu = pMenu.matcher(line);
                        mPlacename = pPlacename.matcher(line);
                        mPrice = pPrice.matcher(line);
                        mAddress = pAddress.matcher(line);
                        mLatitude = pLatitude.matcher(line);
                        mLongitude = pLongitude.matcher(line);
                        mPhone = pPhone.matcher(line);
                        System.out.println(line);
                        if (mMenu.find()) {
                            menu = lnr.readLine();
                        }
                        if (mPlacename.find()) {
                            placename = mPlacename.group(1);
                        }
                        if (mPrice.find()) {
                            price = mPrice.group(1);
                        }
                        if (mAddress.find()) {
                            address = mAddress.group(1);
                        }
                        if (mLatitude.find()) {
                            latitude = mLatitude.group(1);
                        }
                        if (mLongitude.find()) {
                            longitude = mLongitude.group(1);
                        }
                        if (mPhone.find()) {
                            phone = mPhone.group(1);
                        }
                    }
                }
            }
        }
        catch (IOException ioe) {
            System.err.println("File error: " + ioe.getMessage());
            return null;
        }
        return parseRest(menu,placename,price,address,latitude,longitude,phone);
    }

    /** *************************************************************
     *  Fill out from a CSV file a set of concepts that should be ignored
     *  during content extraction
     *  process side effect on static variable "stopConcept"
     */
    public static void readStopConceptArray() {

        if (!stopConcepts.isEmpty()) {
            System.err.println("Error in readStopConceptArray(): file previously read.");
            return;
        }
        List<List<String>> f = DB.readSpreadsheet(KBmanager.getMgr().getPref("kbDir") +
        		File.separator + "WordNetMappings" + File.separator + "stopConcept.csv",null,false);

        List<String> al;
        for (int i = 0; i < f.size(); i++) {
            al = f.get(i);
            stopConcepts.add(al.get(0));
        }
    }

    /** *************************************************************
     *  Fill out from a CSV file a map of word keys, and values broken down by POS,
        listing whether it's a positive or negative word interior hash map keys are
        type, POS, stemmed, polarity
     */
    public static void readSentimentArray() {

        if (!sentiment.isEmpty()) {
            System.out.println("Error in DB.readSentimentArray(): file previously read.");
            return;
        }
        List<List<String>> f = DB.readSpreadsheet(KBmanager.getMgr().getPref("kbDir") +
        		File.separator + "WordNetMappings" + File.separator + "sentiment.csv",null,false);

        List<String> al;
        Map<String,String> entry;
        for (int i = 0; i < f.size(); i++) {
            al = f.get(i);
            entry = new HashMap<>();
            entry.put("type",al.get(0));   // weak, strong
            entry.put("POS",al.get(2));    // noun,verb,adj,adverb,anypos
            entry.put("stemmed",al.get(3));   // y,n
            entry.put("polarity",al.get(4));  // positive, negative
            sentiment.put(al.get(1),entry);
        }
    }

    /** *************************************************************
     * Calculate an integer sentiment value for a string of words.
     */
    public static int computeSentiment(String input) {

        String description = WordNet.wn.removeStopWords(input.trim());
        description = StringUtil.removePunctuation(description);
        String[] words = description.split(" ");
        int total = 0;
        for (String word : words) {
            total = total + computeSentimentForWord(word);
        }
        return total;
    }

    /** *************************************************************
     * Find the sentiment value for a given word, after finding the root
     * form of the word.
     */
    public static int computeSentimentForWord(String word) {

        //System.out.println("INFO in DB.computeSentimentForWord() word: " + word);
        if (sentiment.keySet().size() < 1) {
            System.err.println("Error in DB.computeSentimentForWord() sentiment list not loaded.");
            return 0;
        }
        String nounroot = WordNet.wn.nounRootForm(word,word.toLowerCase());
        String verbroot = WordNet.wn.verbRootForm(word,word.toLowerCase());
        Map<String,String> hm = null;
        if (sentiment.keySet().contains(word))
            hm = sentiment.get(word);
        else if (!word.equals(verbroot) && sentiment.keySet().contains(verbroot))
            hm = sentiment.get(verbroot);
        else if (!word.equals(nounroot) && sentiment.keySet().contains(nounroot))
            hm = sentiment.get(nounroot);
        if (hm != null) {
            int multiplier = 0;
            if (hm.get("type").equals("weak"))
                multiplier = 1;
            if (hm.get("type").equals("strong"))
                multiplier = 5;
            if (hm.get("polarity").equals("neutral"))
                multiplier = 0;
            if (hm.get("polarity").equals("positive"))
                return multiplier;
            else
                return - multiplier;
        }
        return 0;
    }

    /** *************************************************************
     * Add new scores to existing scores.  Note the side effect on scores.
     * @return a map of concept keys and integer sentiment score values
     */
    public static Map<String,Integer> addConceptSentimentScores(Map<String,Integer> scores,
                                                            String SUMOs, int total) {

        String[] terms = SUMOs.split(" ");
        String term;
        int newTotal;
        for (String term1 : terms) {
            term = term1.trim();
            if (!StringUtil.emptyString(term)) {
                newTotal = total;
                if (scores.keySet().contains(term))
                    newTotal = total + scores.get(term);
                scores.put(term, newTotal);
            }
        }
        return scores;
    }
    /** *************************************************************
     * Associate individual concepts with a sentiment score
     * @return a map of concept keys and integer sentiment score values
     */
    public static Map<String,Integer> computeConceptSentimentFromFile(String filename) {

        Map<String,Integer> result = new HashMap<>();
        try (InputStream fis = new FileInputStream(filename)) {
            if (fis != null) {
                StringBuilder buffer = new StringBuilder();
                try (Reader isr = new InputStreamReader(fis, "US-ASCII"); Reader in = new BufferedReader(isr)) {
                    int ch;
                    while ((ch = in.read()) > -1) {
                        buffer.append((char) ch);
                        if (ch == '!' || ch == '.' || ch == '?') {
                            result = addSentiment(result, computeConceptSentiment(buffer.toString()));
                            buffer = new StringBuilder();
                        }
                    }
                }
            }
        }
        catch (IOException ioe) {
            System.err.println("Error in DB.computeConceptSentimentFromFile() reading : " + filename);
            ioe.printStackTrace();
        }
        return result;
    }

    /** *************************************************************
     * Associate individual concepts with a sentiment score
     * @return a map of concept keys and integer sentiment score values
     */
    public static Map<String,Integer> computeConceptSentiment(String input) {

        System.out.println("INFO in DB.computeConceptSentiment(): " + input);
        Map<String,Integer> result = new HashMap<>();
        String paragraph = WordNet.wn.removeStopWords(input.trim());
        paragraph = StringUtil.removeHTML(paragraph);
        String[] sentences = paragraph.split("[\\.\\/\\!]");
        String sentence, SUMOs;
        String[] words;
        int total;
        List<String> SUMOal;
        for (String sentence1 : sentences) {
            // look at each sentence
            sentence = StringUtil.removePunctuation(sentence1);
            words = sentence.split(" ");
            total = 0;
            for (String word : words) {
                // look at each word
                total = total + computeSentimentForWord(word);
            }
            SUMOal = WSD.collectSUMOFromWords(sentence);
            SUMOs = StringUtil.arrayListToSpacedString(SUMOal);
            System.out.println("INFO in DB.computeConceptSentiment(): done collecting SUMO terms: " + SUMOs + " from input: " + sentence);
            result = addConceptSentimentScores(result,SUMOs,total);
        }
        return result;
    }

    /** *************************************************************
     */
    public static void readAmenities() {

        List<List<String>> f = DB.readSpreadsheet(KBmanager.getMgr().getPref("kbDir") +
                File.separator + "Feeds-SUMO_Mapping.csv",null,false);
        List<String> al;
        for (int i = 1; i < f.size(); i++) {
            al = f.get(i);
            amenityTerms.add(al.get(3));
        }
    }

    /** *************************************************************
     * Add the Integer values of two HashMaps that have corresponding String keys
     */
    private static Map<String,Integer> addSentiment(Map<String,Integer>totalSent,
    		                                            Map<String,Integer>sent) {

    	Map<String,Integer> result = new HashMap<>();
    	result.putAll(totalSent);
        for (String key : sent.keySet()) {
            if (!totalSent.keySet().contains(key))
                result.put(key, sent.get(key));
            else
                result.put(key, sent.get(key) + result.get(key));
        }
    	return result;
    }

    /** *************************************************************
     */
    public static void textSentimentByPeriod() {

    	// ArrayList by time period of an array of SUMO terms and sentiment values for that period
    	int periodLength = 7; // in days
        SimpleDateFormat sdf = new SimpleDateFormat("EEE MMM dd HH:mm:ss ZZZZZ yyyy"); // "Tue Jun 28 06:53:37 +0000 2011"

    	Map<String,Integer> totalSent = new HashMap<>();
        List<List<String>> f = DB.readSpreadsheet("t_filtered.csv",null,false,'\t');
        System.out.println("INFO in DB.textSentiment() : read spreadsheet with " + f.size() + " rows");
        List<String> al;
        Calendar c, calendar;
        Date d;
        String date, text;
        Map<String,Integer> sent;
        for (int i = 1; i < f.size(); i++) {
            al = f.get(i);
            if (al.size() > 1) {
            	c = Calendar.getInstance();  // now
            	c.add(Calendar.DATE, periodLength);
            	d = new Date();
            	d.setTime(c.getTime().getTime());
                date = al.get(0);
                try {
                    d = sdf.parse(date);
                }
                catch (ParseException pe) {
                    System.err.println("Error in DB.processTimeDate(): error parsing date/time string: " + date);
                }
                calendar = new GregorianCalendar();
                if (d != null) {
                    calendar.setTime(d);
                    System.out.print(calendar.get(Calendar.SECOND));
                }
                //String id = al.get(1);
                text = al.get(2);
                sent = computeConceptSentiment(text);
                totalSent = addSentiment(totalSent,sent);
            }
        }
        System.out.println(totalSent);
    }

    /** *************************************************************
     */
    public static void textSentiment() {

    	Map<String,Integer> totalSent = new HashMap<>();
        List<List<String>> f = DB.readSpreadsheet("t_filtered.csv",null,false,'\t');
        System.out.println("INFO in DB.textSentiment() : read spreadsheet with " + f.size() + " rows");
        List<String> al;
        String date, text;
        Map<String,Integer> sent;
        for (int i = 1; i < f.size(); i++) {
            al = f.get(i);
            if (al.size() > 1) {
                date = al.get(0);
                //String id = al.get(1);
                text = al.get(2);
                sent = computeConceptSentiment(text);
                totalSent = addSentiment(totalSent,sent);
            }
        }
        System.out.println(totalSent);
    }

    /** *************************************************************
     * Compute sentiment for each line of a text file and output as CSV.
     */
    public static void textFileSentiment(String fname, boolean neg) {

       File fin  = new File(fname);
       try (Reader fr = new FileReader(fin);
            LineNumberReader lnr = new LineNumberReader(fr)) {
            String line;
            while ((line = lnr.readLine()) != null) {
                line = StringUtil.removePunctuation(line);
                int sent = computeSentiment(line);
                if (neg) {
                    if (sent < 0)
                        System.out.println(line + ", " + sent + ", 1");
                    else
                        System.out.println(line + ", " + sent + ", 0");
                }
                else {
                    if (sent > 0)
                        System.out.println(line + ", " + sent + ", 1");
                    else
                        System.out.println(line + ", " + sent + ", 0");
                }
            }
        }
        catch (IOException ioe) {
            System.err.println("File error: " + ioe.getMessage());
        }
    }

    /** *************************************************************
     */
    public static void testSentiment() {
        readSentimentArray();
        String[] s = new String[24];
        s[0] = "The bathroom is gorgeous but the shower doesn't work properly.";
        s[1] = "The warm welcome atmosphere disappeared right after I checked into the room.";
        s[2] = "The room decorations are poor but we felt at home.";
        s[3] = "As we recall, the room service food could have been better prepared.";
        s[4] = "The room stinks.";
        s[5] = "The price sucks.";
        s[6] = "I found that the mattress is no younger than my age although we're told the rooms were completely renovated last year.";
        s[7] = "We all like the indoor swimming pool.";
        s[8] = "In summary, the rooms are great but the services are not.";
        s[9] = "The hotel provides free airport transportation which you cannot count on because the driver is unavailable all the time.";
        s[10] = "The hotel is called 'Comfort Inn' which it is really not.";
        s[11] = "I will never stay at that hotel again!";
        s[12] = "Think again if you want to book that hotel!";
        s[13] = "I really enjoy my stay there despite it is so short and exhausting.";
        s[14] = "Who would dislike the free home-style breakfast which includes almost everything you can imagine?";
        s[15] = "Are you willing to walk 5 miles to get to the nearest restaurant?";
        s[16] = "The front desk staff could be warmer.";
        s[17] = "It hurts just to think our experience there!";
        s[18] = "We definitely will re-visit the place and stay at the same hotel again.";
        s[19] = "I cannot stand the odor from the old carpet.";
        s[20] = "My kids hate the noise outside of the room in the early morning.";
        s[21] = "My kids no longer love that ski resort any more.";
        s[22] = "I guess you could find that kind of hotel nowhere in the world.";
        s[23] = "After a report about the food poisoning case in its cafe, I gave up my plan to visit the bar and restaurant in that hotel.";
        for (int i = 0; i < 24; i++) {
            System.out.println(s[i] + ":" + computeSentiment(s[i]));
        }
    }

    /** *************************************************************
     */
    public static void testSentimentCorpus() {

        try {
            KBmanager.getMgr().initializeOnce();
        } catch (Exception ex) {
            System.out.println(ex.getMessage());
        }
        WordNet.initOnce();
        readSentimentArray();
        //System.out.println(DB.computeConceptSentiment("This hotel is really bad."));
        textFileSentiment("rt-polarity.neg",true);
    }

    /** *************************************************************
     */
    public static void guessGender(String fname) {

        List<List<String>> fn = DB.readSpreadsheet("FirstNames.csv",null,false,',');
        Map<String,String> names = new HashMap<>();
        List<String> row;
        for (int i = 1; i < fn.size(); i++) {  // skip header
            row = fn.get(i);
            names.put(row.get(0).toUpperCase(), row.get(1));
        }
        List<List<String>> dat = DB.readSpreadsheet(fname,null,false,'\t');
        String firstName, gender;
        for (int i = 1; i < dat.size(); i++) {
            row = dat.get(i);
            if (row != null && row.size() > 10 && StringUtil.emptyString(row.get(1))) {   // gender column
                //System.out.println(row.get(1));
                firstName = names.get(row.get(10).toUpperCase());  // first name column
                if (firstName != null) {
                    //System.out.println(firstName);
                    gender = "male";
                    if (names.get(row.get(10).toUpperCase()).equals("F"))
                        gender = "female";
                    row.set(1,gender);
                }
            }
        }
        System.out.println(DB.writeSpreadsheet(dat, true));
    }

    /** *************************************************************
     * A test method
     */
    public static void main (String args[]) {
        /*
    Set<String> result = new TreeSet<>();
    */

        if (args[0].equals("-help") || StringUtil.emptyString(args[0]) ) {
            System.out.println("usage:");
            System.out.println(">java -classpath . com.articulate.sigma.DB -gender /home/me/data.csv");
            System.out.println(">java -classpath . com.articulate.sigma.DB -sent");
        }
        if (args[0].equals("-gender")) {
            if (StringUtil.emptyString(args[1]))
                System.out.println("Error in DB.main: no filename");
            else
                guessGender(args[1]);
        }
        if (args[0].equals("-sent")) {
            testSentimentCorpus();
        }
        if (args[0].equals("-conSent")) {
            KBmanager.getMgr().initializeOnce();
            WordNet.initOnce();
            System.out.println("INFO in DB.main: completed initialization");
            if (StringUtil.emptyString(args[1]))
                System.out.println("Error in DB.main: no filename");
            else
                System.out.println(computeConceptSentimentFromFile(args[1]));
        }


        //textSentiment();
        /*
    PrintWriter pw = null;
    try {
            HashSet<String> fileList = getAllRest();
    Iterator it = fileList.iterator();
    while (it.hasNext()) {
    String f = (String) it.next();
            result.addAll(DB.parseOneRestFile(f));
    }
   File fout  = new File(args[0]);
pw = new PrintWriter(fout);
    it = result.iterator();
    while (it.hasNext()) {
    String axiom = (String) it.next();
    pw.println(axiom);
    }
    }
        catch (Exception ex) {
            System.out.println("Error in DB.main(): " + ex.getMessage());
            ex.printStackTrace();
        }
        finally {
            try {
                if (pw != null) {
                    pw.close();
                }
            }
            catch (Exception ioe) {
                ioe.printStackTrace();
            }
        }
         */
        //System.out.println(geocode("300 Bayshore, Mountain View, CA"));
        //System.out.println(geocode("300 Bayshore, Mntn View, CA"));
        //DB.hotelAmenitySentiment(Hotel.parseAllHotelReviewFiles("hotelReviews-US-fileList.txt"));
        //DB.testSentiment();
        //soaper();
        //DiningDBImport();
        /*
    String timeDate = "2007-11-15 19:30:00";
    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss"); // "2007-11-15 19:30:00"
    Date d = null;
    try {
    d = sdf.parse(timeDate);
    }
    catch (ParseException pe) {
    System.out.println("Error in DB.processTimeDate(): error parsing date/time string: " + timeDate);
    }
    System.out.println(d);
         */
        //readSentimentArray();
        //System.out.println(DB.computeConceptSentiment("I have stayed at this hotel a number of times and have enjoyed my room on each occasion. Always clean and comfortable and the location of the hotel is very nice.I would say that if you want the room to be more quite take one on an upper floor, the rooms at the bottom are noisy because of the dining room and side street. My main problem with this hotel is that there is no parking. You have to park on the street and you must be very diligent to pay attention to the signs or your car will be towed. I am speaking from experience and it cost me $600 to have it returned. When I went to the front desk to get the number of the city towing lot, they had it readily available and said 'Sorry, it happens a lot, they are really strict about parking in this area. If this issue was so common they should be proactive about solving it. My suggestion would be that upon arrival the hotel could provide a small map of the immediate surrounding streets to show where parking can be found and the schedule of street cleaning, resident parking hours, etc. This would be an easy problem to avoid in the future and one that no doubt is costing the hotel repeat customers."));
    }
}
