package com.articulate.sigma;

import com.articulate.sigma.tp.Vampire;
import com.articulate.sigma.trans.*;
import com.articulate.sigma.utils.FileUtil;
import com.articulate.sigma.utils.StringUtil;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
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
August 9, Acapulco, Mexico.  See also https://github.com/ontologyportal
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
 *
 *  All test file statements must be valid SUO-KIF.  Allowable meta-predicates
 *  are: note, time, query, answer.  All other predicates are assumed to be
 *  SUO-KIF expressions.  'answer' may take multiple SUO-KIF statements where there
 *  could be more than one valid answer.
 */
public class InferenceTestSuite {

    /** Total time */
    public static long totalTime = 0;

    /** Default timeout for queries with unspecified timeouts or override when selected */
    public static int _DEFAULT_TIMEOUT = 30;

    public static boolean overrideTimeout = false;

    public KB kb = null;

    public static boolean debug = false;

    // save TPTP translations of each problem as <probName>.p
    public static boolean saveTPTP =  true;

    public static HashSet<String> metaPred = new HashSet(
            Arrays.asList("note", "time", "query", "answer"));

    /** ***************************************************************
     * Compare the expected answers to the returned answers.  Return
     * true if no answers are found or if any pair of answers
     * is different.  Return false otherwise.
     *
     * TODO: If both answersList and tpp.bindings are a lit of entities,
     *       we enforce that all entity pair should be exactly the same;
     */
    private static boolean sameAnswers(ArrayList<String> actualAnswerList, ArrayList<String> expectedAnswerList) {

        if (debug) System.out.println("InferenceTestSuite.sameAnswers(1): expected answers: " + expectedAnswerList);
        if (debug) System.out.println("InferenceTestSuite.sameAnswers(1): bindings: " + actualAnswerList);
        for (int i = 0; i < actualAnswerList.size(); i++) {
            String actualRes = actualAnswerList.get(i);
            if (TPTP3ProofProcessor.isSkolemRelation(actualRes)) {
                TPTP2SUMO tptp2sumo =  new TPTP2SUMO();
                actualRes = normalizeSkolem(tptp2sumo.formToSUMO(actualRes));
                if (!normalizeSkolem(expectedAnswerList.get(i)).equals(actualRes)) {
                    if (debug) System.out.println("InferenceTestSuite.sameAnswers(1): different skolem answers: " +
                            actualRes + " and " + expectedAnswerList.get(i));
                    return false;    // return false if any pair of answers is different
                }
            }
            else
                if (!expectedAnswerList.get(i).equals(actualRes)) {
                    if (debug) System.out.println("InferenceTestSuite.sameAnswers(1): different answers: " +
                            actualRes + " and " + expectedAnswerList.get(i));
                    return false;    // return false if any pair of answers is different
                }
        }
        if (debug) System.out.println("InferenceTestSuite.sameAnswers(1): returning true");
        return true;
    }

    /** ***************************************************************
     * Compare the expected answers to the returned answers.  Return
     * true if no answers are found or if any pair of answers
     * is different.  Return false otherwise.
     *
     * TODO: If both answersList and tpp.bindings are a lit of entities,
     *       we enforce that all entity pair should be exactly the same;
     */
    private static boolean sameAnswers(TPTP3ProofProcessor tpp, ArrayList<String> answerList) {

        if (debug) System.out.println("InferenceTestSuite.sameAnswers(2): expected answers: " + answerList);
        if (debug) System.out.println("InferenceTestSuite.sameAnswers(2): tpp proof size: " + tpp.proof.size());
        if (debug) System.out.println("InferenceTestSuite.sameAnswers(2): bindings: " + tpp.bindings);
        if ((tpp == null || tpp.proof.size() == 0) && (answerList == null || answerList.contains("no")))
            return true;         // return true if no answers are found in the inference engine
        if (answerList != null && !answerList.isEmpty()) {
            if (answerList.get(0).equals("yes"))
                return tpp.proof.size() > 0 && tpp.containsFalse;   // return true if "yes" is expected, and we do find a contradiction (answer)
            else
                return sameAnswers(tpp.bindings, answerList);
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
     */
    private static void clearOutputDir(File outputDir) throws IOException {

        if (outputDir == null) {
            System.out.println("Error in InferenceTestSuite.clearOutputDir(): null file input");
            return;
        }
        final File[] files = outputDir.listFiles();
        if (files != null)
            for (File f : files)
                f.delete();
    }

    /** ***************************************************************
     *  Copy test files to the output directory so that they are visible
     *  to Sigma as a Tomcat application.
     */
    private static void copyTestFiles(ArrayList<File> files, File outputDir) throws IOException  {

        if ((outputDir == null) || !outputDir.isDirectory()) {
            System.out.println("Error in InferenceTestSuite.copyTestFiles(): Could not find  " + outputDir);
            return;
        }
        for (File f : files) {
            String target = outputDir.getAbsolutePath() + File.separator + f.getName();
            File fout = new File(target);
            if (!fout.exists()) {
                Files.copy(f.toPath(), fout.toPath());
                System.out.println("InferenceTestSuite.copyTestFiles(): copied file to: " + fout.getAbsolutePath());
            }
            else
                System.out.println("InferenceTestSuite.copyTestFiles(): already exists: " + fout.getAbsolutePath());
        }
    }

    /** ***************************************************************
     *  Note that 'files' variable is modified as a side effect.
     *  @return error messages, or null if none
     */
    private static String getTestFiles(ArrayList<File> files, File outputDir) throws IOException  {

        String inferenceTestDirPath = KBmanager.getMgr().getPref("inferenceTestDir");
        if ((inferenceTestDirPath == null) || inferenceTestDirPath.equals("")) 
            return("Error in InferenceTestSuite.getTestFiles(): The Sigma preference \"inferenceTestDir\" has not been set");
        
        File inferenceTestDir = new File(inferenceTestDirPath);
        if (!inferenceTestDir.isDirectory() && !inferenceTestDir.mkdir()) 
            return("Error in InferenceTestSuite.getTestFiles(): Could not find or create " +
                    inferenceTestDir.getCanonicalPath());
        
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

        // At this point, inferenceTestDir and outputDir should be
        // set to viable values.
        File[] newfiles = inferenceTestDir.listFiles();
        System.out.println("INFO in InferenceTestSuite.getTestFiles(): number of files: " + newfiles.length); 
        if (newfiles == null || newfiles.length == 0) {
            System.out.println("Error in InferenceTestSuite.getTestFiles(): No test files found in " +
                    inferenceTestDir.getCanonicalPath());
            return("No test files found in " + inferenceTestDir.getCanonicalPath());
        }
        Arrays.sort(newfiles);
        for (File f : newfiles) {
            if (f.getName().endsWith(".tq"))
                files.add(f);
        }
        if (files.size() < 1) {
            System.out.println("INFO in InferenceTestSuite.getTestFiles(): No test files found in " +
                    inferenceTestDir.getCanonicalPath());
            return("No test files found in " + inferenceTestDir.getCanonicalPath());
        }
        return null;  // Normal exit, with "files" modified as a side effect
    }

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

        if (systemChosen.equals("EProver"))
            KBmanager.getMgr().prover = KBmanager.Prover.EPROVER;
        if (systemChosen.equals("Vampire"))
            KBmanager.getMgr().prover = KBmanager.Prover.VAMPIRE;
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
        public boolean success = false;
        public float execTime = 0;
        public String SZSstatus = "";
    }

    /** ***************************************************************
     * Check if the required SUO-KIF file set is loaded and return false and
     * print an error if not.  Also issue a warning if extra non-required
     * files are present.
     */
    public boolean compareFiles(InfTestData itd) {

        boolean result = true;
        for (String f : itd.files) {
            if (!kb.containsFile(f)) {
                result = false;
                System.out.println("Error in InferenceTestSuite.compareFiles(): Required file " + f + " not in KB");
            }
        }
        for (String f : kb.constituents) {
            if (!itd.files.contains(f)) {
                System.out.println("InferenceTestSuite.compareFiles(): Extra, non-required file " + f + " found in KB");
            }
        }
        return result;
    }

    /** ***************************************************************
     * skolem terms in a string are converted to 'sK1'
     */
    public static String normalizeSkolem(String s) {

        //System.out.println("INFO in InferenceTestSuite.normalizeSkolem(): input: " + s);
        s = s.replaceAll("sK[0-9 ]+","sK1").replaceAll("esk\\d+","esk1");
        //System.out.println("INFO in InferenceTestSuite.normalizeSkolem(): result: " + s);
        return s;
    }

    /** ***************************************************************
     * parse answers
     */
    public static void parseAnswers(String s, InfTestData itd) {

        String answerstring = s;
        if (debug) System.out.println("INFO in InferenceTestSuite.readTestFile(): answerString: " + answerstring);
        if (answerstring.equals("yes") || answerstring.equals("no")) {
            itd.expectedAnswers.add(answerstring);
        }
        else {
            Formula ansForm = new Formula(answerstring);
            if (debug) System.out.println("INFO in InferenceTestSuite.readTestFile(): answer form: " + ansForm);
            ArrayList<String> answers = ansForm.complexArgumentsToArrayListString(1);
            if (debug) System.out.println("INFO in InferenceTestSuite.readTestFile(): answers: " + answers);
            //answerstring = normalizeSkolem(answerstring);
            //answerstring = StringUtil.removeEnclosingCharPair(answerstring,1,'(',')');
            for (String a : answers) {
                if (TPTP3ProofProcessor.isSkolemRelation(a))
                    a = normalizeSkolem(a);
                if (debug) System.out.println("INFO in InferenceTestSuite.readTestFile(): answers normalized: " + a);
                itd.expectedAnswers.add(a);
            }
        }
    }

    /** ***************************************************************
     * Read in a .tq inference test files and return an InfTestData
     * object or null if error
     * Note that if the expected answer is a skolem term, the function is converted to 'sK1'
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
                        parseAnswers(formula,ifd);
                    }
                    else if (formula.startsWith("(time"))
                        ifd.timeout = Integer.parseInt(formula.substring(6, formula.length() - 1));
                    else if (formula.startsWith("(file")) {
                        String filename = formula.substring(6, formula.length() - 1);
                        if (!ifd.kbFiles.contains(filename))
                            ifd.kbFiles.add(filename);
                    }
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
     */
    public void saveTPTP(InfTestData itd) {

        String name = FileUtil.noExt(FileUtil.noPath(itd.filename));
        String kbName = KBmanager.getMgr().getPref("sumokbname");
        String kbDir = KBmanager.getMgr().getPref("kbDir");
        String sep = File.separator;
        try {
            String langExt = SUMOformulaToTPTPformula.lang;
            if (langExt.equals("fof"))
                langExt = "tptp";
            Files.copy(Paths.get(kbDir + sep + kbName + "." + langExt),
                    Paths.get(kbDir + sep + "KB.ax"), StandardCopyOption.REPLACE_EXISTING);
            Files.copy(Paths.get(kbDir + sep + "temp-stmt." + langExt),
                    Paths.get(kbDir + sep + name + ".p"), StandardCopyOption.REPLACE_EXISTING);
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    /** ***************************************************************
     */
    public void printResults(Collection<InfTestData> tests) {

        System.out.println();
        System.out.println("ALL TEST QUERIES FINISHED");
        System.out.println();
        for (InfTestData itd : tests) {
            System.out.print(itd.filename + "\t" + itd.SZSstatus + "\t" + itd.execTime);
            if (itd.inconsistent)
                System.out.println(" inconsistent!");
            else
                System.out.println();
        }
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

        String language = "EnglishLanguage";
        int maxAnswers = 1;
        totalTime = 0;
        long duration = 0;
        result = result.append("<h2>Inference tests</h2>\n");
        result = result.append("<table><tr><td>name</td><td>test file</td><td>result</td><td>Time (ms)</td></tr>");

        File outputDir = setOutputDir();
        clearOutputDir(outputDir);
        ArrayList<File> files = new ArrayList();
        String error = getTestFiles(files,outputDir);
        copyTestFiles(files,outputDir);
        if (error != null) 
            return error;

        ArrayList<InfTestData> tests = readTestFiles(files);
        System.out.println("INFO in InferenceTestSuite.test(): number of files: " + files.size());
        int counter = 0;
        for (InfTestData itd : tests) {
            kb.deleteUserAssertionsAndReload();
            for (String s : itd.statements)
                if (!StringUtil.emptyString(s))
                    kb.tell(s);
            compareFiles(itd);
            maxAnswers = itd.expectedAnswers.size();
            try {
                System.out.println("====================================");
                System.out.println("INFO in InferenceTestSuite.test(): Note: " + itd.note);
                System.out.println("INFO in InferenceTestSuite.test(): Query: " + itd.query);
                Formula theQuery = new Formula(itd.query);
                FormulaPreprocessor fp = new FormulaPreprocessor();
                SUMOKBtoTFAKB stfa = new SUMOKBtoTFAKB();
                stfa.initOnce();
                SUMOtoTFAform.initOnce();
                Set<Formula> theQueries = fp.preProcess(theQuery,true,kb);
                for (Formula processed : theQueries) {
                    if (processed.isHigherOrder(kb)) {
                        System.out.println("Error in InferenceTestSuite.test(): skipping higher order query: " +
                                processed + " in test " + itd.note);
                        continue;
                    }
                    long start = System.currentTimeMillis();
                    System.out.println("INFO in InferenceTestSuite.test(): Query " + processed + " is posed to " + KBmanager.getMgr().prover);
                    int actualTimeout = itd.timeout;
                    if (overrideTimeout)
                        actualTimeout = defaultTimeout;
                    if (KBmanager.getMgr().prover == KBmanager.Prover.EPROVER)
                        proof = kb.askEProver(processed.getFormula(), actualTimeout, maxAnswers) + " ";
                    if (KBmanager.getMgr().prover == KBmanager.Prover.VAMPIRE)
                        proof = kb.askVampire(processed.getFormula(), actualTimeout, maxAnswers) + " ";
                    if (KBmanager.getMgr().prover == KBmanager.Prover.LEO)
                        proof = kb.askLeo(processed.getFormula(), actualTimeout, maxAnswers) + " ";
                    duration = System.currentTimeMillis() - start;
                    System.out.print("INFO in InferenceTestSuite.test(): Duration: ");
                    System.out.println(duration);
                    itd.execTime = duration;
                    totalTime = totalTime + duration;
                    if (saveTPTP)
                        saveTPTP(itd);
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
                System.out.println("InferenceTestSuite.test() proof status: " + tpp.status + " for " + itd.note);
                itd.SZSstatus = tpp.status;
                if (tpp != null && tpp.status != null && (tpp.status.contains("Refutation") ||  tpp.status.contains("Theorem"))) {
                    pw.println(HTMLformatter.formatTPTP3ProofResult(tpp, itd.query, lineHtml, kb.name, language));
                    System.out.println("InferenceTestSuite.test() wrote results of: " + itd.note + " to " + resultsFile);
                }
                else
                    if (tpp != null)
                        pw.println(tpp.status);
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
                itd.inconsistent = true;
            }
            boolean different = true;
            if (proof != null && tpp != null && tpp.status != null && !tpp.status.startsWith("Timeout"))
                different = !sameAnswers(tpp,itd.expectedAnswers);
            String resultString = "";
            if (different || tpp.noConjecture) {
                resultString = "fail";
                fail++;
            }
            else {
                resultString = "succeed";
                pass++;
            }
            result.append("<tr><td>" + itd.note + "</td><td><a href=\"tests/" + itd.filename +
                                    "\">" + itd.filename + "</a></td>");
            result.append("<td><a href=\"" + outputDir.getName() + "/" + resultsFile.getName() +
                                   "\">" + resultString + "</a></td>");
            result.append("<td>" + String.valueOf(duration) + "</td></tr>\n");
            System.out.println("Finished test #" + (counter+1) + " of " + files.size() + " : " + itd.filename);
            System.out.println(resultString);
            System.out.println();
            counter++;
        }

        printResults(tests);
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
        if (overrideTimeout)
            itd.timeout = _DEFAULT_TIMEOUT;
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
            if (f.isHigherOrder(kb) && !SUMOformulaToTPTPformula.lang.equals("thf")) {
                System.out.println("Error in InferenceTestSuite.inferenceUnitTest(): skipping higher order query: " +
                        processedStmt + " in test " + itd.note);
                continue;
            }
            System.out.println("\n============================");
            System.out.println("InferenceTestSuite.inferenceUnitTest(): ask: " + processedStmt);
            System.out.println("INFO in InferenceTestSuite.test(): Query is posed to " + KBmanager.getMgr().prover);
            if (KBmanager.getMgr().prover == KBmanager.Prover.VAMPIRE) {
                Vampire vampire = kb.askVampire(processedStmt, itd.timeout, maxAnswers);
                System.out.println("InferenceTestSuite.inferenceUnitTest(): proof: " + vampire.toString());
                tpp.parseProofOutput(vampire.output, processedStmt, kb,vampire.qlist);
            }
            else if (KBmanager.getMgr().prover == KBmanager.Prover.EPROVER) {
                com.articulate.sigma.tp.EProver eprover = kb.askEProver(processedStmt, itd.timeout, maxAnswers);
                System.out.println("InferenceTestSuite.inferenceUnitTest(): proof: " + eprover.toString());
                tpp.parseProofOutput(eprover.output, processedStmt, kb,eprover.qlist);
            }
            else if (KBmanager.getMgr().prover == KBmanager.Prover.LEO) {
                com.articulate.sigma.tp.LEO leo = kb.askLeo(processedStmt, itd.timeout, maxAnswers);
                System.out.println("InferenceTestSuite.inferenceUnitTest(): proof: " + leo.toString());
                tpp.parseProofOutput(leo.output, processedStmt, kb,leo.qlist);
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
            if (!tpp.noConjecture || tpp.inconsistency)
                System.out.println("InferenceTestSuite.inferenceUnitTest(): proof: " + tpp.proof);
            System.out.println("InferenceTestSuite.inferenceUnitTest(): inconsistency: " + tpp.noConjecture);

            itd.actualAnswers.addAll(tpp.bindings);
            System.out.println("InferenceTestSuite.inferenceUnitTest(): answers: " + itd.actualAnswers);
            //if (itd.actualAnswers != null)
            //    itd.actualAnswers.addAll(itd.actualAnswers);
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
            System.out.println("InferenceTestSuite.inferenceUnitTest(): status: " + tpp.status);
            boolean different = true;
            if (tpp != null && tpp.proof != null  && !tpp.status.startsWith("Timeout"))
                different = !sameAnswers(tpp,itd.expectedAnswers);
            if (different || tpp.noConjecture)
                itd.success = false;
            else
                itd.success = true;
        }

        System.out.println("\n============================");
        return itd;
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

        System.out.println("InferenceTestSuite.cmdLineTest(): trying: " + filename);
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
            if (itd.success) {
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

    /****************************************************************
     * Undo all parts of the state that have anything to do with user assertions made during inference.
     * @throws IOException
     */
    public static void resetAllForInference(KB kb) throws IOException {

        System.out.println("in InferenceTestSuite.resetAllForInference(): delete user assertions: ");
        kb.deleteUserAssertions();
        // Remove the assertions in the files.
        File userAssertionsFile = new File(KBmanager.getMgr().getPref("kbDir") +
                KBmanager.getMgr().getPref("sumokbname") + kb._userAssertionsString);
        if (userAssertionsFile.exists())
            userAssertionsFile.delete();
        String tptpFileName = userAssertionsFile.getAbsolutePath().replace(".kif", ".tptp");
        userAssertionsFile = new File(tptpFileName);
        if (userAssertionsFile.exists())
            userAssertionsFile.delete();
        tptpFileName = userAssertionsFile.getAbsolutePath().replace(".tptp", ".tff");
        userAssertionsFile = new File(tptpFileName);
        if (userAssertionsFile.exists())
            userAssertionsFile.delete();
    }

    /** ***************************************************************
     */
    public static void showHelp() {

        System.out.println("============== InferenceTestSuite class =============");
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
        System.out.println("     l - run with LEO-III (add letter to options above)");
        System.out.println("     f - run with TF0 language");
        System.out.println("     0 - run with TH0 language");
        System.out.println("     o - override test timeout with global timeout of " + _DEFAULT_TIMEOUT + " sec");
    }

     /** ***************************************************************
     * Test method
     */
    public static void main(String[] args) {

        System.out.println("args: " + Arrays.toString(args));
        if (args != null && args.length > 0) {
            if (args[0].equals("-h"))
                showHelp();
            else {
                KBmanager.getMgr().initializeOnce();
                InferenceTestSuite its = new InferenceTestSuite();
                if (args[0].indexOf('l') != -1)
                    SUMOKBtoTPTPKB.lang = "thf";
                if (args[0].indexOf('f') != -1)
                    SUMOKBtoTPTPKB.lang = "tff";
                KB kb = KBmanager.getMgr().getKB(KBmanager.getMgr().getPref("sumokbname"));
                try {
                    resetAllForInference(kb);
                }
                catch (Exception e) {
                    e.printStackTrace();
                }
                if (args[0].indexOf('e') != -1) {
                    KBmanager.getMgr().prover = KBmanager.Prover.EPROVER;
                    kb.loadEProver();
                }
                if (args[0].indexOf('l') != -1) {
                    SUMOformulaToTPTPformula.lang = "thf";
                    KBmanager.getMgr().prover = KBmanager.Prover.LEO;
                    kb.loadLeo();
                }
                if (args[0].indexOf('f') != -1) {
                    SUMOformulaToTPTPformula.lang = "tff";
                    SUMOKBtoTFAKB skbtfakb = new SUMOKBtoTFAKB();
                    skbtfakb.initOnce();
                    SUMOtoTFAform.initOnce();
                }
                if (args[0].indexOf('0') != -1) {
                    SUMOformulaToTPTPformula.lang = "thf";
                }
                if (args[0].indexOf('v') != -1)
                    KBmanager.getMgr().prover = KBmanager.Prover.VAMPIRE;
                if (args[0].indexOf('0') != -1)
                    overrideTimeout = true;
                System.out.println("in InferenceTestSuite.main(): using prover: " + KBmanager.getMgr().prover);

                if (args != null && args.length > 1 && args[0].indexOf("t") != -1) {
                    its.cmdLineTest(args[1]);
                }
                else if (args != null && args.length > 1 && args[0].indexOf("i") != -1) {
                    its.runPassing();
                }
                else if (args != null && args.length > 0 && args[0].indexOf("a") != -1) {
                    try {
                        its.test(kb);
                    }
                    catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }
        else
            showHelp();
    }
}
 
