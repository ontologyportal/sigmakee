// package com.articulate.sigma;
// import java.io.File;
// import java.io.FileWriter;
// import java.io.IOException;
// import java.io.LineNumberReader;
// import java.io.PrintWriter;
// import java.io.StringReader;
// import java.util.ArrayList;
// import java.util.Collection;
// import java.util.HashMap;
// import java.util.Iterator;
// import java.util.Map;
// import java.util.Set;

// import com.articulate.sigma.parsing.Expr;
// import com.articulate.sigma.tp.EProver;
// import com.articulate.sigma.tp.LEO;
// import com.articulate.sigma.tp.TheoremProverController;
// import com.articulate.sigma.trans.TPTP3ProofProcessor;

// public class CCheck implements Runnable {
    
//     private TheoremProverController theoremProverController;
//     private KB kb;
//     private File ccheckFile;
//     private FileWriter fw;
//     private PrintWriter pw;
//     private String ccheck_kb;
//     private String inferenceEngine;
//     private Map<String, String> ieSettings;
//     private int timeOut = 10;
//     private String ccheckSessionId;
//     private String proverType;
//     private String tptpLanguage;
//     private String vampireMode;
//     private boolean closedWorldAssumption;
//     private boolean modusPonens;
//     private boolean dropOnePremise;
//     private boolean holUseModals;
//     private int maxAnswers = 1;

//     /****************************************************************
//      */
//     public CCheck(KB kb, String filename) {

//         this.kb = kb;
//         try {
//             ccheckFile = new File(filename);
//             fw = new FileWriter(ccheckFile);
//             pw = new PrintWriter(fw);
//         }
//         catch (IOException e) {
//             System.err.println(e.getMessage());
//             e.printStackTrace();
//         }
//     }

//     /****************************************************************
//      * Creates a consistency check using TheoremProverController.runQuery().
//      * @param kb KB to check
//      * @param fileName result file
//      * @param userSessionId source user session id
//      * @param proverType prover selected by the UI
//      * @param language TPTP-family language: FOF, TFF, or THF
//      * @param vampireMode Vampire execution mode
//      * @param closedWorldAssumption whether to use CWA
//      * @param modusPonens whether to use modus ponens
//      * @param dropOnePremise whether to drop one-premise formulas
//      * @param holUseModals whether to use modal HOL translation
//      * @param timeout timeout in seconds
//      * @param maxAnswers maximum answers
//      */
//     public CCheck(KB kb, String fileName, String userSessionId, String proverType, String language, String vampireMode, boolean closedWorldAssumption, boolean modusPonens, boolean dropOnePremise, boolean holUseModals, int timeout, int maxAnswers) throws Exception {

//         this(kb, fileName);
//         this.theoremProverController = new TheoremProverController();
//         this.ccheckSessionId = makeCCheckSessionId(kb, userSessionId);
//         this.proverType = normalizeProverType(proverType);
//         this.tptpLanguage = normalizeLanguage(language, this.proverType);
//         this.vampireMode = normalizeVampireMode(vampireMode);
//         this.closedWorldAssumption = closedWorldAssumption;
//         this.modusPonens = modusPonens;
//         this.dropOnePremise = dropOnePremise && modusPonens;
//         this.holUseModals = holUseModals && "THF".equals(this.tptpLanguage);
//         this.timeOut = timeout > 0 ? timeout : 30;
//         this.maxAnswers = maxAnswers > 0 ? maxAnswers : 1;
//         this.inferenceEngine = this.proverType;
//     }

//     /****************************************************************
//      * @param kb KB being checked
//      * @param userSessionId source user session id
//      * @return isolated session id for this consistency-check run
//      */
//     private static String makeCCheckSessionId(KB kb, String userSessionId) {

//         String base = userSessionId;
//         if (base == null || base.isEmpty())
//             base = "nosession";
//         return "ccheck_" + kb.name.replaceAll("\\W", "_") + "_" +
//                 base.replaceAll("\\W", "_") + "_" +
//                 Long.toUnsignedString(System.nanoTime(), 36);
//     }

//     /****************************************************************
//      */
//     public CCheck(KB kb, String fileName, String chosenEngine, int timeout) throws Exception {

//         this(kb, fileName);
//         timeOut = timeout;
//         if (!setInferenceEngine(chosenEngine)) {
//             System.err.println("Unable to create CCheck for kb: " + kb.name +
//                     "; Error setting up inference engine = " + inferenceEngine);
//             throw new Exception("Could not set inference engine with the following params for KB " +
//                     kb.name + ". Inference Engine chosen = " + chosenEngine);
//         }
//     }

//     /****************************************************************
//      */
//     public CCheck(KB kb, String fileName, String chosenEngine, String systemChosen, String quietFlag,
//                 String location, String language, int timeout) throws Exception {

//         this (kb, fileName);
//         timeOut = timeout;
//         if (!setInferenceEngine(chosenEngine, systemChosen, location.toLowerCase(), quietFlag, language))
//             throw new Exception("Could not set inference engine with the following params: {chosenEngine=" +
//         chosenEngine + ", systemChosen=" + systemChosen + ", location=" + location + "}");
//         else System.out.println("Set up inference engine for Consistency Check of KB: " + kb.name +
//                 ". Engine Chosen: " + chosenEngine);
//     }

//     /****************************************************************
//      * This sets the inference engine to be used for the consistency check.
//      * This particular method sets it if chosenEngine == 'SoTPTP.
//      * @param chosenEngine - string describing the inference engine to be used.
//      * @param systemChosen - the theorem prover to be used
//      * @param location - if it's local or remote
//      * @param quietFlag - command option as to the verbosity of the result
//      * @param language - language for formatting
//      * @return true if there are no errors in setting the engine, false if errors are encountered.
//      */
//     private boolean setInferenceEngine(String chosenEngine, String systemChosen, String location, String quietFlag, String language) {

//         try {
//             if (chosenEngine.equals("SoTPTP")) {
//                 //String result = InterfaceTPTP.queryTPTP("(instance instance BinaryPredicate)", 10, 1, lineHtml,
//                 //        systemChosen, location, quietFlag, kb.name, language);
//                 inferenceEngine = "SoTPTP";
//                 ieSettings = new HashMap<>();
//                 ieSettings.put("systemChosen", systemChosen);
//                 if ("".equals(location) || location == null)
//                     return false;
//                 else ieSettings.put("location", location);
//                 if ("".equals(quietFlag) || quietFlag == null)
//                     ieSettings.put("quietFlag", "hyperlinkedKIF");
//                 else
//                     ieSettings.put("quietFlag", quietFlag);
//                 if ("".equals(language) || language == null)
//                     language = "EnglishLanguage";
//                 ieSettings.put("language", language);
//                 return true;
//             }
//             else
//                 setInferenceEngine(chosenEngine);
//         }
//         catch (Exception e) {
//             System.err.println("Error in setting up SystemOnTPTP: " + e.getMessage());
//             return false;
//         }
//         return false;
//     }

//     /****************************************************************
//      */
//     private KB makeEmptyKB() {

//         ccheck_kb = "CCheck_" + kb.name;
//         return Diagnostics.makeEmptyKB(ccheck_kb);
//     }

//     /****************************************************************
//      */
//     private void printReport(Formula query, String processedQ,
//                              String sourceFile, boolean syntaxError, String proof,
//                              String testType) {

//         pw.println("    <entry>");
//         pw.println("      <query>");
//         pw.println("        " + query.getFormula());
//         pw.println("      </query>");
//         pw.println("      <processedStatement>");
//         pw.println("        " + processedQ);
//         pw.println("      </processedStatement>");
//         pw.println("      <sourceFile>");
//         if (sourceFile != null)
//             pw.println("        " + sourceFile);
//         pw.println("      </sourceFile>");
//         pw.println("      <type>");
//         if (syntaxError)
//             pw.println("        Syntax error in formula");
//         else
//             pw.println("        " + testType);
//         pw.println("      </type>");
//         pw.println("      <proof src=\"" + inferenceEngine + "\">");
//         String[] split = proof.split("\n");
//         for (String split1 : split) {
//             pw.println("      " + split1);
//         }
//         pw.println("      </proof>");
//         pw.println("    </entry>");
//     }

//     /****************************************************************
//      * This method saves the answer and proof for detected redundancies
//      * or inconsistencies into the file.
//      * @param proof - the proof presented that establishes the redundancy or inconsistency
//      * @param query - the statement that caused the error
//      * @param testType - whether it is a redundancy or inconsistency
//      */
//     private void reportAnswer(String proof, Formula query, String testType,
//                               String processedQ, String sourceFile) {

//         try {
//             if (proof.contains("Syntax error detected")) {
//                 printReport(query, processedQ, sourceFile, true, proof, testType);
//             } else if (inferenceEngine.equals("EProver")) {
//                 try (StringReader sr = new StringReader(proof); LineNumberReader lnr = new LineNumberReader(sr)) {
//                     TPTP3ProofProcessor tpp = new TPTP3ProofProcessor();
//                     tpp.parseProofOutput(lnr, kb);
//                     if (tpp.proof != null && !tpp.proof.isEmpty()) {
//                         printReport(query, processedQ, sourceFile, false, proof, testType);
//                     }
//                 }
//             } else if (inferenceEngine.equals("SoTPTP")) {
//                 proof = proof.replaceAll("<", "%3C");
//                 proof = proof.replaceAll(">", "%3E");
//                 proof = proof.replaceAll("/n", "");
//                 if (proof.contains("[yes]") || proof.contains("[Theorem]")
//                         || proof.contains("[definite]")) {
//                     printReport(query, processedQ, sourceFile, false, proof, testType);
//                 }
//             }
//         } catch (IOException ex) {
//             System.err.println(ex.getMessage());
//             ex.printStackTrace();
//         }
//     }

//     /****************************************************************
//      * This would save the error message for a formula in the CCheck results
//      * file to inform the user that an error occurred while performing a
//      * consistency check on one of the statements.
//      * @param message - error message
//      * @param query - the formula being tested
//      * @param processedQ - the processed query
//      * @param sourceFile - the source file where the formula being tested came from
//      */
//     private void reportError(String message, Formula query, String processedQ, String sourceFile) {

//         pw.println("    <entry>");
//         pw.println("      <query>");
//         pw.println("        " + query.getFormula());
//         pw.println("      </query>");
//         pw.println("      <processedStatement>");
//         pw.println("        " + processedQ);
//         pw.println("      </processedStatement>");
//         pw.println("      <sourceFile>");
//         if (sourceFile != null)
//             pw.println("        " + sourceFile);
//         pw.println("      </sourceFile>");
//         pw.println("      <type>");
//         pw.println("        Error from Inference Engine");
//         pw.println("      </type>");
//         pw.println("      <proof src=\"" + inferenceEngine + "\">");
//         pw.println("        " + message);
//         pw.println("      </proof>");
//         pw.println("    </entry>");

//         try {
//             pw.flush();
//             fw.flush();
//         }
//         catch (IOException ex) {
//             System.err.println(ex.getMessage());
//             ex.printStackTrace();
//         }
//     }

//     /****************************************************************
//      * This initiates the consistency check
//      */
// private void runConsistencyCheck() {

//     KB empty = makeEmptyKB();

//     try {
//         SessionTPTPManager.generateSessionTPTP(ccheckSessionId, empty, fileLang);

//         writeXmlHeader();

//         for (Formula query : kb.formulaMap.values()) {

//             FormulaPreprocessor fp = new FormulaPreprocessor();
//             Set<Expr> processedQueries = fp.preProcessExpr(query, false, kb);

//             for (Expr ex : processedQueries) {

//                 Formula processedFormula = new Formula(ex.toKifString());
//                 String processedQuery = processedFormula.makeQuantifiersExplicit(false);

//                 ATPResult redundancyResult =
//                         theoremProverController.runQuery(
//                                 empty,
//                                 ccheckSessionId,
//                                 processedQuery,
//                                 null,
//                                 "CUSTOM",
//                                 proverType,
//                                 tptpLanguage,
//                                 vampireMode,
//                                 closedWorldAssumption,
//                                 modusPonens,
//                                 dropOnePremise,
//                                 holUseModals,
//                                 timeOut,
//                                 maxAnswers);

//                 if (redundancyResult proves processedQuery)
//                     printReport(query, processedQuery, sourceFile, false,
//                                 redundancyResult text, "Redundancy");

//                 String negatedQuery = "(not " + processedQuery + ")";

//                 ATPResult inconsistencyResult =
//                         theoremProverController.runQuery(
//                                 empty,
//                                 ccheckSessionId,
//                                 negatedQuery,
//                                 null,
//                                 "CUSTOM",
//                                 proverType,
//                                 tptpLanguage,
//                                 vampireMode,
//                                 closedWorldAssumption,
//                                 modusPonens,
//                                 dropOnePremise,
//                                 holUseModals,
//                                 timeOut,
//                                 maxAnswers);

//                 if (inconsistencyResult proves negatedQuery)
//                     printReport(query, processedQuery, sourceFile, false,
//                                 inconsistencyResult text, "Inconsistency");
//             }

//             empty.tell(query.getFormula());

//             SessionTPTPManager.applyIncrementalUpdate(
//                     empty,
//                     ccheckSessionId,
//                     query,
//                     fileLang);
//         }

//         writeXmlFooter();
//     }
//     finally {
//         closeWriters();
//         SessionTPTPManager.cleanupSession(ccheckSessionId);
//         KBmanager.getMgr().removeKB(ccheck_kb);
//     }
// }

//     /****************************************************************
//      * This initiates the consistency check
//      */
//     private void runConsistencyCheckNew() {

//         KB empty = this.makeEmptyKB();
//         try {
//             pw.println("<ConsistencyCheck>");
//             pw.println("  <kb>");
//             pw.println("    " + kb.name);
//             pw.println("  </kb>");
//             Collection<Formula> allFormulas = kb.formulaMap.values();
//             Collection<String> allTPTP = new ArrayList<>();
//             for (Formula f : allFormulas) {
//                 allTPTP.addAll(f.getTheTptpFormulas());
//             }
//             Iterator<String> it = allTPTP.iterator();
//             pw.println("  <entries>");
//             String query;
//             while (it.hasNext()) {
//                 query = it.next();
//             }
//             pw.println("  </entries>");
//             pw.print("</ConsistencyCheck>");
//         }
//         catch (Exception e) {
//             pw.println("  </entries>");
//             pw.print("  <error>");
//             pw.print("Error encountered while running consistency check.");
//             pw.println("</error>");
//             pw.print("</ConsistencyCheck>");
//             System.err.println(e.getMessage());
//             e.printStackTrace();
//         }
//         finally {
//             KBmanager.getMgr().removeKB(ccheck_kb);
//         }
//     }

//     /****************************************************************
//      */
//     @Override
//     public void run() {
//         runConsistencyCheck();
//     }
// }
