package com.articulate.sigma;

import TPTPWorld.InterfaceTPTP;
import TPTPWorld.SystemOnTPTP;
import com.articulate.sigma.tp.Vampire;
import com.articulate.sigma.trans.SUMOKBtoTPTPKB;
import com.articulate.sigma.trans.SUMOformulaToTPTPformula;
import com.articulate.sigma.trans.TPTP2SUMO;
import com.articulate.sigma.trans.TPTP3ProofProcessor;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;

/** This code is copyright Articulate Software (c) 2003.  Some portions
copyright Teknowledge (c) 2003 and reused under the terms of the GNU license.
This software is released under the GNU Public License <http://www.gnu.org/copyleft/gpl.html>.
Users of this code also consent, by use of this code, to credit Articulate Software
and Teknowledge in any writings, briefings, publications, presentations, or 
other representations of any software which incorporates, builds on, or uses this 
code.  Please cite the following article in any publication with references:

Pease, A., (2003). The Sigma Ontology Development Environment, 
in Working Notes of the IJCAI-2003 Workshop on Ontology and Distributed Systems,
August 9, Acapulco, Mexico.  See also http://sigmakee.sourceforge.net
*/

/** A framework for doing a series of assertions and queries, and for comparing
 *  the actual result of queries against an expected result.  Also will 
 *  keep track of the time needed for each query.  Tests are expected in files
 *  with a .tq extension contained in a directory specified by the 
 *  "inferenceTestDir" parameter, and results are provided in the same directory 
 *  with a .res extension.
 * 
 *  The test files contain legal KIF expressions including several kinds of
 *  meta-information.  Meta-predicates include (note <String>), (query <Formula>),
 *  and (answer <term1>..<termn>).  There may be only one note and query statements,
 *  but there may be several answer statements, if multiple binding sets are
 *  expected.
 * 
 *  Comments are allowed in test files, and are signified by a ';', after which
 *  all content on the line is ignored.
 * 
 *  Note that since answers are provided in an ordered list, without reference
 *  to their respective variable names, that the inference engine is assumed to
 *  return bindings in the same order.
 */
public class InferenceTestSuite {

    /** Total time */
    public static long totalTime = 0;

    /** Default timeout for queries with unspecified timeouts */
    public static int _DEFAULT_TIMEOUT = 600;

    public KB kb = null;

    /** ***************************************************************
     * Compare the expected answers to the returned answers.  Return
     * true if any pair of answers is different.  Return false otherwise.
     */
    private static boolean compareAnswers(ProofProcessor pp, ArrayList answerList) {

    
        System.out.println("INFO in InferenceTestSuite.compareAnswers(): num answers: "
			   + String.valueOf(pp.numAnswers()));
        for (int j = 0; j < pp.numAnswers(); j++) {
            System.out.println("INFO in InferenceTestSuite.compareAnswers(): result: "
			       + "" + " expected: " + (String) answerList.get(j));
            if (!pp.equalsAnswer(j,(String) answerList.get(j)))
                return true;
        }
        return false;
    }

    /** ***************************************************************
     * Compare the expected answers to the returned answers.  Return
     * true if no answers are found in E or if any pair of answers
     * is different.  Return false otherwise.
     *
     * TODO: If both answersList and tpp.bindings are a lit of entities,
     *       we enforce that all entity pair should be exactly the same;
     */
    private static boolean compareAnswers(TPTP3ProofProcessor tpp, ArrayList answerList) {

        if (tpp == null || tpp.proof.size() == 0)
            return true;         // return true if no answers are found in the inference engine
        if (answerList != null && !answerList.isEmpty()) {
            if (answerList.get(0).equals("yes"))
                return tpp.proof.size() > 0;   // return false if "yes" is expected, and we do find a contradiction (answer)
            else {
                for (int i = 0; i < tpp.bindings.size(); i++) {
                    String actualRes = tpp.bindings.get(i);
                    if (!answerList.get(i).equals(actualRes))
                        return true;    // return true if any pair of answers is different
                }
            }

        }
        return false;
    }

    /** ***************************************************************
     */
    private static File setOutputDir() throws IOException {

        String outputDirPath = KBmanager.getMgr().getPref("testOutputDir");
        if ((outputDirPath != null) && !outputDirPath.equals(""))            
            return new File(outputDirPath);  // testOutputDir is set.     
        else
            return null;
    }

    /** ***************************************************************
     *  Note that files is modified as a side effect.
     *  @return error messages, or null if none
     */
    private static String getTestFiles(ArrayList<File> files, File outputDir) throws IOException  {

        String inferenceTestDirPath = KBmanager.getMgr().getPref("inferenceTestDir");
        if ((inferenceTestDirPath == null) || inferenceTestDirPath.equals("")) 
            return("Error in InferenceTestSuite.getTestFiles(): The Sigma preference \"inferenceTestDir\" has not been set");
        
        File inferenceTestDir = new File(inferenceTestDirPath);
        if (!inferenceTestDir.isDirectory() && !inferenceTestDir.mkdir()) 
            return("Error in InferenceTestSuite.getTestFiles(): Could not find or create " + inferenceTestDir.getCanonicalPath());
        
        if (!outputDir.isDirectory() && !outputDir.mkdirs()) {
            File baseDir = new File(KBmanager.getMgr().getPref("baseDir"));
            if (baseDir.isDirectory()) {
                File webappsDir = new File(baseDir,"webapps");
                if (webappsDir.isDirectory()) {
                    File waSigmaDir = new File(webappsDir,"sigma");
                    if (waSigmaDir.isDirectory()) 
                        outputDir = new File(waSigmaDir,"tests");                    
                }
            }
        }
        if ((outputDir == null) || !outputDir.isDirectory()) 
            return("Error in InferenceTestSuite.getTestFiles(): Could not find or create " + outputDir);        

        // At this point, inferenceTestDir and outputDir shouild be
        // set to viable values.
        File[] newfiles = inferenceTestDir.listFiles();
        System.out.println("INFO in InferenceTestSuite.getTestFiles(): number of files: " + newfiles.length); 
        if (newfiles == null || newfiles.length == 0) {
            System.out.println("INFO in InferenceTestSuite.getTestFiles(): No test files found in " + inferenceTestDir.getCanonicalPath());
            return("No test files found in " + inferenceTestDir.getCanonicalPath());
        }
        Arrays.sort(newfiles);
        for (int i = 0; i < newfiles.length; i++) {
            if (newfiles[i].getName().endsWith(".tq")) 
                files.add(newfiles[i]);            
        }
        if (files.size() < 1) {
            System.out.println("INFO in InferenceTestSuite.getTestFiles(): No test files found in " + inferenceTestDir.getCanonicalPath());
            return("No test files found in " + inferenceTestDir.getCanonicalPath());
        }
        return null;  // Normal exit, with "files" modified as a side effect
    }

    /** ***************************************************************

    private static String askSoTPTP(String processedStmt, int timeout, int maxAnswers,
                                    String systemChosen, KB kb, String TPTPlocation) throws Exception {

        System.out.println("INFO in InferenceTestSuite.askSoTPTP()"); 
        String tptpresult = "";
        Formula conjectureFormula;
        conjectureFormula = new Formula();
        conjectureFormula.read(processedStmt);
        conjectureFormula.read(conjectureFormula.makeQuantifiersExplicit(true));
        SUMOformulaToTPTPformula stptp = new SUMOformulaToTPTPformula();
    	stptp._f = conjectureFormula;
    	stptp.tptpParse(conjectureFormula,true,kb);
    	SUMOKBtoTPTPKB stptpkb = new SUMOKBtoTPTPKB();
    	stptpkb.kb = kb;
        String kbFileName = stptpkb.writeFile(null,conjectureFormula,true);
        InterfaceTPTP.init();
        String res = InterfaceTPTP.callTPTP(TPTPlocation, systemChosen, kbFileName,
                                            timeout, "-q3", "-S");
        StringTokenizer st = new StringTokenizer(res,"\n");
        String temp = "";
        while (st.hasMoreTokens()) {
            String next = st.nextToken();
            if (!next.equals("") && !next.substring(0,1).equals("%")) 
                temp += next + "\n";                                
        }
        tptpresult = res;
        res = temp;
        if (SystemOnTPTP.isTheorem(tptpresult)) 
            return TPTP2SUMO.convert(res,false);
        else 
            return "<queryResponse>\n<answer result=\"no\" number=\"0\">\n</answer>\n" +
                   "<summary proofs=\"0\"/>\n</queryResponse>\n";                            
    }
*/
    /** ***************************************************************
     * Convenience method that sets default parameters
    */
    public String test(KB kb) throws IOException  {
        return test(kb, _DEFAULT_TIMEOUT, "");
    }

    /** ***************************************************************
     * Convenience method that sets some default parameters
     * @param timeout is a default timeout that is likely to be
     *                overwritten by a specification in the test
     *                data
    */
    public String test(KB kb, String systemChosen, int timeout) throws IOException  {
        return test(kb, timeout, "");
    }

    /** ***************************************************************
     */
    public class InfTestData {
        public String filename = "";
        public String note = "";
        public String query = "";
        public HashSet<String> kbFiles = new HashSet<>();
        public ArrayList<String> expectedAnswers = new ArrayList<>();
        public ArrayList<String> actualAnswers = new ArrayList<>();
        public int timeout = 30;
        public ArrayList<String> files = new ArrayList<>();
        public ArrayList<String> statements = new ArrayList<>();
        public boolean inconsistent = false;
    }

    /** ***************************************************************
     * Check if the required file set is loaded and return false and
     * print an error if not.  Also issue a warning if extra non-required
     * files are present.
     */
    public boolean compareFiles(InfTestData itd) {

        boolean result = true;
        for (String f : itd.files) {
            if (!kb.containsFile(f)) {
                result = false;
                System.out.println("Required file " + f + " not in KB");
            }
        }
        for (String f : kb.constituents) {
            if (!itd.files.contains(f)) {
                System.out.println("Extra, non-required file " + f + " found in KB");
            }
        }
        return result;
    }

    /** ***************************************************************
     * Read in a .tq inference test files and return an InfTestData
     * object or null if error
     */
    public InfTestData readTestFile(File f) {

        InfTestData ifd = null;
        try {
            if (f.getCanonicalPath().endsWith(".tq")) {
                ifd = new InfTestData();
                ifd.filename = f.getName();
                KIF test = new KIF();
                test.readFile(f.getCanonicalPath());
                System.out.println("INFO in InferenceTestSuite.readTestFile(): num formulas: " +
                        String.valueOf(test.formulaMap.keySet().size()));
                for (String formula : test.formulaMap.keySet()) {
                    if (formula.indexOf(";") != -1)
                        formula = formula.substring(0, formula.indexOf(";"));
                    System.out.println("INFO in InferenceTestSuite.readTestFile(): Formula: " + formula);
                    if (formula.startsWith("(note"))
                        ifd.note = formula.substring(6, formula.length() - 1);
                    else if (formula.startsWith("(query"))
                        ifd.query = formula.substring(7, formula.length() - 1);
                    else if (formula.startsWith("(answer")) {
                        String answerstring = formula.substring(8, formula.length() - 1);
                        if (answerstring.equals("yes") || answerstring.equals("no")) {
                            ifd.expectedAnswers.add(answerstring);
                        }
                        else {
                            if (answerstring.startsWith("(and"))
                                answerstring = answerstring.substring(4, answerstring.length() - 1).trim();
                            String[] answers = answerstring.split("\\)");
                            for (String a : answers) {
                                ifd.expectedAnswers.add(a.substring(a.indexOf("(") + 1, a.length()));
                            }
                        }
                    }
                    else if (formula.startsWith("(time"))
                        ifd.timeout = Integer.parseInt(formula.substring(6, formula.length() - 1));
                    else if (formula.startsWith("(file"))
                        ifd.kbFiles.add(formula.substring(6, formula.length() - 1));
                    else
                        ifd.statements.add(formula);
                }
            }
            else {
                System.out.println("Error in readTestFile(): not a tq file: " + f.getName());
            }
        }
        catch (Exception e) {
            System.out.println(e.getMessage());
            e.printStackTrace();
        }
        return ifd;
    }

    /** ***************************************************************
     * Read in all the .tq inference test files from the given list
     */
    public ArrayList<InfTestData> readTestFiles(ArrayList<File> files) {

        ArrayList<InfTestData> result = new ArrayList<>();
        for (File f : files) {
            InfTestData ifd = readTestFile(f);
            if (ifd != null)
                result.add(ifd);
        }
        return result;
    }

    /** ***************************************************************
     * The main method that controls running a set of tests and returning
     * the result as an HTML page showing test results and links to proofs.
     * Note that this procedure deletes any prior user assertions.
     * If a test file has a set of KIF files different from what is already
     * loaded, create a new KB for it.
     */
    public String test(KB inputKB, int defaultTimeout, String TPTPlocation)
        throws IOException {

        kb = inputKB;
        if (TPTPlocation == "" || TPTPlocation == null) {
            TPTPlocation = KBmanager.getMgr().getPref("systemsDir");
        }
        System.out.println("INFO in InferenceTestSuite.test(): Note that any prior user assertions will be deleted.");
        System.out.println("INFO in InferenceTestSuite.test(): Prover: " + KBmanager.getMgr().prover);
        StringBuffer result = new StringBuffer();
        FileWriter fw = null;
        int fail = 0;
        int pass = 0;
        PrintWriter pw = null;
        String proof = null;
        String processedStmt = null;

        String language = "EnglishLanguage";
        int maxAnswers = 1;
        totalTime = 0;
        long duration = 0;
        result = result.append("<h2>Inference tests</h2>\n");
        result = result.append("<table><tr><td>name</td><td>test file</td><td>result</td><td>Time (ms)</td></tr>");

        File outputDir = setOutputDir();
        ArrayList<File> files = new ArrayList();
        String error = getTestFiles(files,outputDir);
        if (error != null) 
            return error;

        File allResultsFile = new File(outputDir, "TestSuiteResults");
        FileWriter afw = new FileWriter(allResultsFile);
        PrintWriter apw = new PrintWriter(afw);
        ArrayList<InfTestData> tests = readTestFiles(files);
        System.out.println("INFO in InferenceTestSuite.test(): number of files: " + files.size());
        int counter = 0;
        for (InfTestData itd : tests) {
            kb.deleteUserAssertionsAndReload();
            for (String s : itd.statements)
                kb.tell(s);
            compareFiles(itd);
            maxAnswers = itd.expectedAnswers.size();
            try {
                System.out.println("INFO in InferenceTestSuite.test(): Query: " + itd.query);
                Formula theQuery = new Formula(itd.query);
                FormulaPreprocessor fp = new FormulaPreprocessor();
                Set<Formula> theQueries = fp.preProcess(theQuery,true,kb);
                Iterator q = theQueries.iterator();
                for (Formula processed : theQueries) {
                    long start = System.currentTimeMillis();
                    System.out.println("INFO in InferenceTestSuite.test(): Query is posed to " + KBmanager.getMgr().prover);
                    proof = kb.ask(processed.getFormula(), itd.timeout, maxAnswers) + " ";

                    duration = System.currentTimeMillis() - start;
                    System.out.print("INFO in InferenceTestSuite.test(): Duration: ");
                    System.out.println(duration);
                    totalTime = totalTime + duration;
                }
            }
            catch (Exception ex) {
                    result = result.append("<br>Error in InferenceTestSuite.test() while executing query " +
					   itd.filename + ": " + ex.getMessage() + "<br>");
            }
            String lineHtml = "<table ALIGN='LEFT' WIDTH=40%%><tr><TD BGCOLOR='#AAAAAA'>" +
                "<IMG SRC='pixmaps/1pixel.gif' width=1 height=1 border=0></TD></tr></table><BR>\n";
            String rfn = itd.filename;
            String resultsFilename = rfn.substring(0,rfn.length()-3) + "-res.html";
            File resultsFile = new File(outputDir, resultsFilename);
            TPTP3ProofProcessor tpp = new TPTP3ProofProcessor();
            try {
                fw = new FileWriter(resultsFile);
                pw = new PrintWriter(fw);
                tpp.parseProofOutput(proof, kb);
                pw.println(HTMLformatter.formatTPTP3ProofResult(tpp, itd.query, lineHtml, kb.name, language));
                itd.actualAnswers = tpp.bindings;
            }
            catch (java.io.IOException e) {
                throw new IOException("Error writing file " + resultsFile.getCanonicalPath());
            }
            finally {
                try {
                    if (pw != null) { pw.close(); }
                    if (fw != null) { fw.close(); }
                }
                catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
            if (tpp.inconsistency) {
                result.append("<h1>InferenceTestSuite.inferenceUnitTest(): Danger! possible inconsistency!</h1>");
            }
            boolean different = true;
            if (proof != null)
                different = compareAnswers(tpp,itd.expectedAnswers);
            String resultString = "";
            if (different || tpp.inconsistency) {
                resultString = "fail";
                fail++;
            }
            else {
                resultString = "succeed";
                pass++;
            }
            result.append("<tr><td>" + itd.note + "</td><td><a href=\"" + outputDir.getName() +
                                   "/" + itd.note + "\">" + itd.filename + "</a></td>");
            result.append("<td><a href=\"" + outputDir.getName() + "/" + resultsFile.getName() +
                                   "\">" + resultString + "</a></td>");
            result.append("<td>" + String.valueOf(duration) + "</td></tr>\n");
            System.out.println("FINISHED TEST #" + counter + " of " + files.size() + " : " + itd.filename);
            counter++;
        }

        System.out.println();
        System.out.println("ALL TEST QUERIES FINISHED");
        System.out.println();

        result.append("</table><P>\n");
        result.append("Total time: ");
        result.append(String.valueOf(totalTime/1000));
        result.append(" seconds<P>\n");

        result.append("Total correct: ");
        result.append(String.valueOf(pass));
        result.append("<P>\n");
        
        result.append("Total failed: ");
        result.append(String.valueOf(fail));
        result.append("<P>\n");
        kb.deleteUserAssertionsAndReload();
        return result.toString();
    }

    /** ***************************************************************
     * The method will be called in InferenceTest in unit test;
     * It takes a TQG file path, reading the kif statements and queries and expected answers;
     * It parses the theorem prover's inference output for actual answers;
     * Note that this procedure DOES NOT delete any prior user assertions.
     */
    public InfTestData inferenceUnitTest(String testpath, KB kb) {

        System.out.println("INFO in InferenceTestSuite.inferenceUnitTest(): testpath: " + testpath);
        // read the test file
        InfTestData itd = readTestFile(new File(testpath));
        compareFiles(itd);
        for (String formula : itd.statements)
             kb.tell(formula);
        KBmanager.getMgr().loadKBforInference(kb);
        System.out.println("INFO in InferenceTestSuite.inferenceUnitTest(): expected answers: " + itd.expectedAnswers);
        int maxAnswers = itd.expectedAnswers.size();
        Formula theQuery = new Formula();
        theQuery.read(itd.query);
        FormulaPreprocessor fp = new FormulaPreprocessor();
        Set<Formula> theQueries = fp.preProcess(theQuery,true,kb);
        TPTP3ProofProcessor tpp = new TPTP3ProofProcessor();
        for (Formula f : theQueries) {
            String processedStmt = f.getFormula();
            System.out.println("\n============================");
            System.out.println("InferenceTestSuite.inferenceUnitTest(): ask: " + processedStmt);
            System.out.println("INFO in InferenceTestSuite.test(): Query is posed to " + KBmanager.getMgr().prover);
            if (KBmanager.getMgr().prover == KBmanager.Prover.VAMPIRE) {
                Vampire vampire = kb.askVampire(processedStmt, itd.timeout, maxAnswers);
                System.out.println("InferenceTestSuite.inferenceUnitTest(): proof: " + vampire.toString());
                tpp.parseProofOutput(vampire.output, processedStmt, kb);
            }
            else if (KBmanager.getMgr().prover == KBmanager.Prover.EPROVER) {
                com.articulate.sigma.tp.EProver eprover = kb.askEProver(processedStmt, itd.timeout, maxAnswers);
                System.out.println("InferenceTestSuite.inferenceUnitTest(): proof: " + eprover.toString());
                tpp.parseProofOutput(eprover.output, processedStmt, kb);
            }
            else {
                System.out.println("Error in InferenceTestSuite.inferenceUnitTest(): no prover or unknown prover: " + KBmanager.getMgr().prover);
                continue;
            }
            if (tpp.inconsistency) {
                System.out.println("*****************************************");
                System.out.println("InferenceTestSuite.inferenceUnitTest(): Danger! possible inconsistency!");
                System.out.println("proof with no negated conjecture in " + itd.filename);
                System.out.println("*****************************************");
            }
            System.out.println("InferenceTestSuite.inferenceUnitTest(): bindings: " + tpp.bindings);
            System.out.println("InferenceTestSuite.inferenceUnitTest(): bindingMap: " + tpp.bindingMap);
            System.out.println("InferenceTestSuite.inferenceUnitTest(): proof: " + tpp.proof);
            System.out.println("InferenceTestSuite.inferenceUnitTest(): inconsistency: " + tpp.inconsistency);

            itd.actualAnswers.addAll(tpp.bindings);
            System.out.println("InferenceTestSuite.inferenceUnitTest(): answers: " + itd.actualAnswers);
            if (itd.actualAnswers != null)
                itd.actualAnswers.addAll(itd.actualAnswers);
            System.out.println("InferenceTestSuite.inferenceUnitTest(): tpp status: " + tpp.status);
            System.out.println("InferenceTestSuite.inferenceUnitTest(): actual answers: " + itd.actualAnswers);
            if (tpp.status != null && tpp.status.startsWith("Theorem") && itd.actualAnswers.size() == 0)
                itd.actualAnswers.add("yes");
            if (tpp.inconsistency) {
                itd.inconsistent = true;
                itd.actualAnswers = new ArrayList<>();
            }
            System.out.println("InferenceTestSuite.inferenceUnitTest(): actual answers(2): " + itd.actualAnswers);
            System.out.println("InferenceTestSuite.inferenceUnitTest(): expected answers(2): " + itd.expectedAnswers);
        }

        System.out.println("\n============================");
        return itd;
    }

    /** ***************************************************************
     */
    public static void showHelp() {

        System.out.println("InferenceTestSuite class");
        System.out.println("Test files are s-expressions that take valid SUMO formulas");
        System.out.println("or meta-predicates that are one of (note \"message\"), ");
        System.out.println("(query <formula>), (answer <term>), (time <integer>)");
        System.out.println("or (file \"path\")");
        System.out.println("  options:");
        System.out.println("  -h - show this help screen");
        System.out.println("  -t <name> - run named test file in config.xml inferenceTestDir");
        System.out.println("  -i <mode> - run test files known to pass in the given mode in config.xml inferenceTestDir");
        System.out.println("  -a <mode> - run all test files in the given mode in config.xml inferenceTestDir");
        System.out.println("     e - run with eprover (add letter to options above)");
        System.out.println("     v - run with vampire (add letter to options above)");
    }

    /** ***************************************************************
     * Test method
     */
    public void runPassing() {

        kb = KBmanager.getMgr().getKB(KBmanager.getMgr().getPref("sumokbname"));
        List<String> passingSet = Arrays.asList("TQG2","TQG3","TQG4","TQG13","TQG18","TQG30","TQG31","TQG32");
        for (String s : passingSet) {
            cmdLineTest(s + ".kif.tq");
        }
    }

    /** ***************************************************************
     * Test method
     */
    public boolean cmdLineTest(String filename) {

        ArrayList<String> expected = new ArrayList<String>();
        ArrayList<String> actual = new ArrayList<String>();
        try {
            kb = KBmanager.getMgr().getKB(KBmanager.getMgr().getPref("sumokbname"));
            String path = KBmanager.getMgr().getPref("inferenceTestDir");
            InfTestData itd = inferenceUnitTest(path + File.separator + filename,kb);
            if (itd.inconsistent) {
                System.out.println("*****************************************");
                System.out.println("InferenceTestSuite.cmdLineTest(): Danger! possible inconsistency!");
                System.out.println("proof with no negated conjecture in " + itd.filename);
                System.out.println("*****************************************");
            }
            if (itd.actualAnswers.equals(itd.expectedAnswers) && !itd.inconsistent) {
                System.out.println("InferenceTestSuite.cmdLineTest() : Success on " + filename);
                return true;
            }
            else {
                System.out.println("InferenceTestSuite.cmdLineTest() : Failure on " + filename);
                return false;
            }
        }
        catch (Exception e) {
            System.out.println("Error in InferenceTestSuite.cmdLineTest()");
            System.out.println(e.getMessage());
            e.printStackTrace();
        }
        return false;
    }

    /** ***************************************************************
     * Test method
     */
    public static void main(String[] args) {

        if (args != null && args.length > 1) {
            if (args[0].equals("-h"))
                showHelp();
            else {
                KBmanager.getMgr().initializeOnce();
                InferenceTestSuite its = new InferenceTestSuite();
                KB kb = KBmanager.getMgr().getKB(KBmanager.getMgr().getPref("sumokbname"));
                if (args[0].indexOf('e') != -1) {
                    KBmanager.getMgr().prover = KBmanager.Prover.EPROVER;
                    kb.loadEProver();
                }
                if (args[0].indexOf('v') != -1)
                    KBmanager.getMgr().prover = KBmanager.Prover.VAMPIRE;
                System.out.println("in InferenceTestSuite.main(): using prover: " + KBmanager.getMgr().prover);

                if (args != null && args.length > 1 && args[0].contains("t")) {
                    its.cmdLineTest(args[1]);
                }
                else if (args != null && args.length > 1 && args[0].contains("i")) {
                    its.runPassing();
                }
            }
        }
        else
            showHelp();
    }
}
 
