package com.articulate.sigma;

import java.util.*;
import java.io.*;

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
    public static int TIMEOUT = 60;

    /** ***************************************************************
     * Compare the expected answers to the returned answers.  Return
     * true if any pair of answers is different.  Return false otherwise.
     */
    public static boolean compareAnswers(ProofProcessor pp, ArrayList answerList) {

        System.out.println("INFO in InferenceTestSuite.compareAnswers(): num answers: " + String.valueOf(pp.numAnswers()));
        for (int j = 0; j < pp.numAnswers(); j++) {
            System.out.println("INFO in InferenceTestSuite.compareAnswers(): result: " + pp.returnAnswer(j) + " expected: " + (String) answerList.get(j));
            if (!pp.equalsAnswer(j,(String) answerList.get(j)))
                return true;
        }
        return false;
    }

    /** ***************************************************************
     * The main method that controls running a set of tests and returning
     * the result as an HTML page showing test results and links to proofs.
     * Note that this procedure deletes any prior user assertions.
     */
    public static String test(KB kb) throws IOException  {

        System.out.println("INFO in InferenceTestSuite.test(): Note that any prior user assertions will be deleted.");
        StringBuffer result = new StringBuffer();
        FileWriter fw = null;
        int fail = 0;
        int pass = 0;
        PrintWriter pw = null;
        String proof = null;
        String processedStmt = null;
        String inferenceTestDirPath = KBmanager.getMgr().getPref("inferenceTestDir");
        if ((inferenceTestDirPath == null) || inferenceTestDirPath.equals("")) {
            return("The Sigma preference \"inferenceTestDir\" has not been set");
        }
        File inferenceTestDir = new File(inferenceTestDirPath);
        if (!inferenceTestDir.isDirectory() && !inferenceTestDir.mkdir()) {
            return("Could not find or create " + inferenceTestDir.getCanonicalPath());
        }
        File outputDir = null;
        String outputDirPath = KBmanager.getMgr().getPref("testOutputDir");
        if ((outputDirPath != null) && !outputDirPath.equals("")) {
            // testOutputDir is set.
            outputDir = new File(outputDirPath);
        }
        if (!outputDir.isDirectory() && !outputDir.mkdirs()) {
            File baseDir = new File(KBmanager.getMgr().getPref("baseDir"));
            if (baseDir.isDirectory()) {
                File webappsDir = new File(baseDir, "webapps");
                if (webappsDir.isDirectory()) {
                    File waSigmaDir = new File(webappsDir, "sigma");
                    if (waSigmaDir.isDirectory()) {
                        outputDir = new File(waSigmaDir, "tests");
                    }
                }
            }
        }
        if ((outputDir == null) || !outputDir.isDirectory()) {
            return("Could not find or create " + outputDir);
        }

        // At this point, inferenceTestDir and outputDir shouild be
        // set to viable values.
        String language = "en";
        int maxAnswers = 1;
        totalTime = 0;
        long duration = 0;
        result = result.append("<h2>Inference tests</h2>\n");
        result = result.append("<table><tr><td>name</td><td>test file</td><td>result</td><td>Time (ms)</td></tr>");

        File[] files = inferenceTestDir.listFiles();
        Arrays.sort(files);
        boolean testFilesPresent = false;
        if (files == null || files.length == 0) {
            System.out.println("INFO in InferenceTestSuite.test(): No test files found in " + inferenceTestDir.getCanonicalPath());
            return("No test files found in " + inferenceTestDir.getCanonicalPath());
        }
        int i = 0;
        for (i = 0; i < files.length; i++) {
            if (files[i].getName().endsWith(".tq")) {
                testFilesPresent = true;
                break;
            }
        }
        if (!testFilesPresent) {
            System.out.println("INFO in InferenceTestSuite.test(): No test files found in " + inferenceTestDir.getCanonicalPath());
            return("No test files found in " + inferenceTestDir.getCanonicalPath());
        }
        for (i = 0; i < files.length; i++) {

            int timeout = TIMEOUT;

            // This probably isn't necessary.  Make this a preferences
            // parameter.
            kb.deleteUserAssertions();

            if (files[i].getCanonicalPath().endsWith(".tq")) {

                System.out.println();
                System.out.println("STARTING TEST " + files[i].getName());
                System.out.println();

                KIF test = new KIF();
                try {
                    test.readFile(files[i].getCanonicalPath());
                }
                catch (Exception e) {
                    return("Error in InferenceTestSuite.test(): exception reading file: " + files[i].getCanonicalPath() + ". " + e.getMessage());
                }
                File outfile = new File(outputDir, files[i].getName());
                try {
                    test.writeFile(outfile.getCanonicalPath());
                }
                catch (IOException ioe) {
                    return("Error in InferenceTestSuite.test(): exception writing file: " + outfile.getCanonicalPath() + ". " + ioe.getMessage());
                }
                System.out.println("INFO in InferenceTestSuite.test(): num formulas: " + String.valueOf(test.formulaSet.size()));
                Iterator it = test.formulaSet.iterator();
                String note = files[i].getName();
                String query = null;
                ArrayList answerList = new ArrayList();
                while (it.hasNext()) {
                    String formula = (String) it.next();
                    if (formula.indexOf(";") != -1)
                        formula = formula.substring(0,formula.indexOf(";"));
                    System.out.println("INFO in InferenceTestSuite.test(): Formula: " + formula);
                    if (formula.startsWith("(note")) 
                        note = formula.substring(6,formula.length()-1);
                    else if (formula.startsWith("(query")) 
                        query = formula.substring(7,formula.length()-1);
                    else if (formula.startsWith("(answer")) 
                        answerList.add(formula.substring(8,formula.length()-1));
                    else if (formula.startsWith("(time")) 
                        timeout = Integer.parseInt(formula.substring(6,formula.length()-1));
                    else
                        kb.tell(formula);
                }
                maxAnswers = answerList.size();
                try {
                    System.out.println("INFO in InferenceTestSuite.test(): Query: " + query);

                    Formula theQuery = new Formula();
                    ArrayList theQueries = null;
                    theQuery.theFormula = query;

                    theQueries = theQuery.preProcess(true,kb);
                    Iterator q = theQueries.iterator();
                    while (q.hasNext()) {
                        processedStmt = ((Formula)q.next()).theFormula;
                        long start = System.currentTimeMillis();
                        proof = kb.ask(processedStmt,timeout,maxAnswers);
                        duration = System.currentTimeMillis() - start;
                        
                        System.out.print("INFO in InferenceTestSuite.test(): Duration: ");
                        System.out.println(duration);
                        totalTime = totalTime + duration;
                    }
                }
                catch ( Exception ex ) {
                    return("Error in InferenceTestSuite.test() while executing query: " + ex.getMessage());
                }
                String lineHtml = "<table ALIGN='LEFT' WIDTH=40%%><tr><TD BGCOLOR='#AAAAAA'><IMG SRC='pixmaps/1pixel.gif' width=1 height=1 border=0></TD></tr></table><BR>\n";
                String rfn = files[i].getName();
                String resultsFilename = rfn.substring(0,rfn.length()-3) + "-res.html";
                File resultsFile = new File(outputDir, resultsFilename);
                try {
                    fw = new FileWriter(resultsFile);
                    pw = new PrintWriter(fw);
                    pw.println(HTMLformatter.formatProofResult(proof,query,processedStmt,lineHtml,kb.name,language));
                }
                catch (java.io.IOException e) {
                    throw new IOException("Error writing file " + resultsFile.getCanonicalPath());
                }
                finally {
                    try {
                        if (pw != null) {
                            pw.close();
                        }
                        if (fw != null) {
                            fw.close();
                        }
                    }
                    catch (Exception ex) {
                    }
                }
                BasicXMLparser res = new BasicXMLparser(proof);
                ProofProcessor pp = new ProofProcessor(res.elements);
                String resultString = "";
                boolean different = compareAnswers(pp,answerList);
                if (different) {
                    resultString = "fail";
                    fail++;
                }
                else {
                    resultString = "succeed";
                    pass++;
                }
                result = result.append("<tr><td>" + note + "</td><td><a href=\"" + outputDir.getName() + "/" + files[i].getName() + "\">" + files[i].getName() + "</a></td>");
                result = result.append("<td><a href=\"" + outputDir.getName() + "/" + resultsFile.getName() + "\">" + resultString + "</a></td>");
                result = result.append("<td>" + String.valueOf(duration) + "</td></tr>\n");
            }
        }

        System.out.println();
        System.out.println("ALL TEST QUERIES FINISHED");
        System.out.println();

        result = result.append("</table><P>\n");
        result = result.append("Total time: ");
        result = result.append(String.valueOf(totalTime/1000));
        result = result.append(" seconds<P>\n");

        result = result.append("Total correct: ");
        result = result.append(String.valueOf(pass));
        result = result.append("<P>\n");
        
        result = result.append("Total failed: ");
        result = result.append(String.valueOf(fail));
        result = result.append("<P>\n");
        kb.deleteUserAssertions();
        return result.toString();
    }

    /** ***************************************************************
     * Test method
     */
    public static void main(String[] args) {
    }
}
