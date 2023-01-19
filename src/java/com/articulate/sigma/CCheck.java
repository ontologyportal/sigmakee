package com.articulate.sigma;

/** This code is copyright Articulate Software (c) 2014.
 This software is released under the GNU Public License <http://www.gnu.org/copyleft/gpl.html>.
 Users of this code also consent, by use of this code, to credit Articulate Software
 and Teknowledge in any writings, briefings, publications, presentations, or
 other representations of any software which incorporates, builds on, or uses this
 code.  Please cite the following article in any publication with references:

 Pease, A., (2003). The Sigma Ontology Development Environment,
 in Working Notes of the IJCAI-2003 Workshop on Ontology and Distributed Systems,
 August 9, Acapulco, Mexico.  See also sigmakee.sourceforge.net
 */

import com.articulate.sigma.tp.EProver;
import com.articulate.sigma.tp.LEO;
import com.articulate.sigma.trans.TPTP3ProofProcessor;

import java.io.*;
import java.util.*;

public class CCheck implements Runnable {
    private KB kb;
    private File ccheckFile;
    private FileWriter fw;
    private PrintWriter pw;
    private String ccheck_kb;
    private String inferenceEngine;
    private HashMap<String, String> ieSettings;
    private int timeOut = 10;
    private String lineHtml = "<table ALIGN='LEFT' WIDTH='40%'><tr><TD BGCOLOR='#AAAAAA'>" + 
            "<IMG SRC='pixmaps/1pixel.gif' width=1 height=1 border=0></TD></tr></table><BR>\n";
    
    /** *************************************************************
     */
    public CCheck(KB kb, String filename) {
        
        this.kb = kb;
        try {
            ccheckFile = new File(filename);                        
            fw = new FileWriter(ccheckFile);
            pw = new PrintWriter(fw);            
        }
        catch (Exception e) {
            System.out.println(e.getMessage());
            e.printStackTrace();
        }
    }
    
    /** *************************************************************
     */
    public CCheck(KB kb, String fileName, String chosenEngine, int timeout) throws Exception {
        
        this(kb, fileName);
        timeOut = timeout;
        if (setInferenceEngine(chosenEngine) == false) {
            System.out.println("Unable to create CCheck for kb: " + kb.name + 
                    "; Error setting up inference engine = " + inferenceEngine);
            throw new Exception("Could not set inference engine with the following params for KB " + 
                    kb.name + ". Inference Engine chosen = " + chosenEngine);
        }
    }

    /** *************************************************************
     */
    public CCheck(KB kb, String fileName, String chosenEngine, String systemChosen, String quietFlag,
                String location, String language, int timeout) throws Exception {
        
        this (kb, fileName);
        timeOut = timeout;
        if (!setInferenceEngine(chosenEngine, systemChosen, location.toLowerCase(), quietFlag, language))
            throw new Exception("Could not set inference engine with the following params: {chosenEngine=" + 
        chosenEngine + ", systemChosen=" + systemChosen + ", location=" + location + "}");
        else System.out.println("Set up inference engine for Consistency Check of KB: " + kb.name + 
                ". Engine Chosen: " + chosenEngine);
    }

    /** *************************************************************
     * This sets the inference engine to be used for the consistency check.  
     * This particular method sets it if chosenEngine == 'SoTPTP'
     * 
     * @param chosenEngine - string describing the inference engine to be used.  
     *         For this particular method, it should be 'SoTPTP'
     * @param systemChosen - the theorem prover to be used
     * @param location - if it's local or remote
     * @param quietFlag - command option as to the verbosity of the result 
     * @param language - language for formatting
     * @return true if there are no errors in setting the engine, false if errors are encountered.
     */
    private boolean setInferenceEngine(String chosenEngine, String systemChosen, String location,
            String quietFlag, String language) {

        try {
            if (chosenEngine.equals("SoTPTP")) {
                //String result = InterfaceTPTP.queryTPTP("(instance instance BinaryPredicate)", 10, 1, lineHtml,
                //        systemChosen, location, quietFlag, kb.name, language);
                inferenceEngine = "SoTPTP";
                ieSettings = new HashMap<String, String>();
                ieSettings.put("systemChosen", systemChosen);
                if (location == "" || location == null) 
                    return false;
                else ieSettings.put("location", location);                
                if (quietFlag == "" || quietFlag == null)
                    ieSettings.put("quietFlag", "hyperlinkedKIF");
                else
                    ieSettings.put("quietFlag", quietFlag);                
                if (language == "" || language == null)
                    language = "EnglishLanguage";
                ieSettings.put("language", language);                
                return true;                
            }
            else 
                setInferenceEngine(chosenEngine);
        }
        catch (Exception e) {
            System.out.println("Error in setting up SystemOnTPTP: " + e.getMessage());
            return false;
        }
        return false;
    }
    
    /** *************************************************************
     * This sets the inference engine to be used for the consistency check.  
     * It sends a test query to the inference engine to
     * ensure that the engine works.
     * @param chosenEngine - string describing the inference engine to be used.
     * @return true if there are no errors in setting the engine, false if 
     *         errors are encountered
     */    
    private boolean setInferenceEngine(String chosenEngine) {
        
        String result = "";
        try {
            if (chosenEngine.equals("EProver")) {
                result = kb.askEProver("(instance instance BinaryPredicate)", 10, 1) + " ";
                inferenceEngine = "EProver";
                return true;
            }
            else if (chosenEngine.equals("SInE")) {
                result = kb.askSInE("(instance instance BinaryPredicate)", 10, 1);
                inferenceEngine = "SInE";
                return true;
            }
            else if (chosenEngine.equals("LeoLocal")) {
                LEO leo = kb.askLeo("(instance instance BinaryPredicate)", 10, 1);
                inferenceEngine = "LeoLocal";
                return true;
            }
            else 
                return false;
        }
        catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
    
    /** *************************************************************
     */
    public String getKBName() {
        return kb.name;
    }

    /** *************************************************************
     */
    private KB makeEmptyKB() {
        
        ccheck_kb = "CCheck_" + kb.name;
        String kbDir = (String) KBmanager.getMgr().getPref("kbDir");
        if (KBmanager.getMgr().existsKB(ccheck_kb)) 
            KBmanager.getMgr().removeKB(ccheck_kb);        
        File dir = new File( kbDir );
        File emptyCFile = new File( dir, "emptyConstituent.txt" );
        String emptyCFilename = emptyCFile.getAbsolutePath();        
        FileWriter fwriter = null; 
        PrintWriter pwriter = null;
        KBmanager.getMgr().addKB(ccheck_kb, false);
        KB empty = KBmanager.getMgr().getKB(ccheck_kb);

        try { // Fails elsewhere if no constituents, or empty constituent, thus...
            empty.eprover = new EProver(KBmanager.getMgr().getPref("eprover"));
            fwriter = new FileWriter( emptyCFile );
            pwriter = new PrintWriter(fwriter);   
            pwriter.println("(instance instance BinaryPredicate)\n");
            if (pwriter != null) pwriter.close();
            if (fwriter != null) fwriter.close();
            empty.addConstituent(emptyCFilename);
        }
        catch (java.io.IOException e) {
            System.out.println("Error writing file " + emptyCFilename);
        }
        catch (Exception e) {
            System.out.println(e.getMessage());
            e.printStackTrace();
        }
        return empty;
    }
    
    /** *************************************************************     
     */
    private void printReport(Formula query, String processedQ,
            String sourceFile, boolean syntaxError, String proof,
            String testType) {
        
           pw.println("    <entry>");
        pw.println("      <query>");            
        pw.println("        " + query.getFormula());
        pw.println("      </query>");
        pw.println("      <processedStatement>");
        pw.println("        " + processedQ);
        pw.println("      </processedStatement>");
        pw.println("      <sourceFile>");
        if (sourceFile != null)
            pw.println("        " + sourceFile);
        pw.println("      </sourceFile>");
        pw.println("      <type>");
        if (syntaxError)
            pw.println("        Syntax error in formula");
        else
            pw.println("        " + testType);
        pw.println("      </type>");
        pw.println("      <proof src=\"" + inferenceEngine + "\">");
        String[] split = proof.split("\n");
        for (int i = 0; i < split.length; i++)
            pw.println("      " + split[i]);
        pw.println("      </proof>");
        pw.println("    </entry>");
    }
    
    /** *************************************************************
     * This method saves the answer and proof for detected redundancies 
     * or inconsistencies into the file.
     * 
     * @param proof - the proof presented that establishes the 
     *         redundancy or inconsistency
     * @param query - the statement that caused the error
     * @param testType - whether it is a redundancy or inconsistency
     */
    private void reportAnswer(String proof, Formula query, String testType, 
            String processedQ, String sourceFile) {

        if (proof.indexOf("Syntax error detected") != -1) 
            printReport(query,processedQ,sourceFile,true,proof,testType);        
        else if (inferenceEngine.equals("EProver")) {
    		StringReader sr = new StringReader(proof);
    		LineNumberReader lnr = new LineNumberReader(sr);
            TPTP3ProofProcessor tpp = new TPTP3ProofProcessor();
            tpp.parseProofOutput(lnr, kb);
            if (tpp.proof != null && tpp.proof.size() > 0) 
                printReport(query,processedQ,sourceFile,false,proof,testType);            
        }
        else if (inferenceEngine.equals("SoTPTP")) {
            proof = proof.replaceAll("<", "%3C");
            proof = proof.replaceAll(">", "%3E");
            proof = proof.replaceAll("/n", "");
            if (proof.contains("[yes]") || proof.contains("[Theorem]")
                    || proof.contains("[definite]")) 
                printReport(query,processedQ,sourceFile,false,proof,testType);            
        }
        try {
            pw.flush();
            fw.flush();
        }
        catch (Exception ex) {
            System.out.println(ex.getMessage());
            ex.printStackTrace();
        }
    }
    
    /** *************************************************************
     * This would save the error message for a formula in the CCheck results
     * file to inform the user that an error occurred while performing a
     * consistency check on one of the statements.
     * 
     * @param message
     *            - error message
     * @param query
     *            - the formula being tested
     * @param processedQ
     *            - the processed query
     * @param sourceFile
     *            - the source file where the formula being tested came from
     */
    private void reportError(String message, Formula query, String processedQ, String sourceFile) {
        
        pw.println("    <entry>");
        pw.println("      <query>");            
        pw.println("        " + query.getFormula());
        pw.println("      </query>");
        pw.println("      <processedStatement>");
        pw.println("        " + processedQ);
        pw.println("      </processedStatement>");
        pw.println("      <sourceFile>");
        if (sourceFile != null)
            pw.println("        " + sourceFile);
        pw.println("      </sourceFile>");
        pw.println("      <type>");
        pw.println("        Error from Inference Engine");
        pw.println("      </type>");
        pw.println("      <proof src=\"" + inferenceEngine + "\">");
        pw.println("        " + message);
        pw.println("      </proof>");
        pw.println("    </entry>");
        
        try {
            pw.flush();
            fw.flush();
        }
        catch (Exception ex) {
            System.out.println(ex.getMessage());
            ex.printStackTrace();
        }
    }

    /** *************************************************************
     * This initiates the consistency check
     */
    private void runConsistencyCheck() {
        
        String proof;        
        KB empty = this.makeEmptyKB();
        try {
            pw.println("<ConsistencyCheck>");
            pw.println("  <kb>");
            pw.println("    " + kb.name);
            pw.println("  </kb>");            
            Collection<Formula> allFormulas = kb.formulaMap.values();
            Iterator<Formula> it = allFormulas.iterator();
            pw.println("  <entries>");            
            while (it.hasNext()) {
                Formula query = (Formula) it.next();
                System.out.println("CCheck.runConsistencyCheck: eprover: " + empty.eprover);
                FormulaPreprocessor fp = new FormulaPreprocessor();
                Set<Formula> processedQueries = fp.preProcess(query,false, kb);
                
                String processedQuery = null;
                String sourceFile = null;
                Iterator<Formula> q = processedQueries.iterator();                
                while(q.hasNext()) {
                    Formula f = q.next();                    
                    processedQuery = f.makeQuantifiersExplicit(false);                    
                    sourceFile = f.sourceFile;
                    sourceFile = sourceFile.replace("/", "&#47;");
                    try {
                        proof = askInferenceEngine(empty, processedQuery);
                        reportAnswer(proof, query, "Redundancy", processedQuery, sourceFile);
                    }
                    catch(Exception e) {
                        reportError(e.getMessage(), query, processedQuery, sourceFile);                        
                        System.out.println("Error from inference engine: " + e.getMessage());
                    }                        
                    StringBuffer negatedQuery = new StringBuffer();
                    negatedQuery.append("(not " + processedQuery + ")");
                    try {
                        proof = askInferenceEngine(empty, negatedQuery.toString());
                        reportAnswer(proof, query ,"Inconsistency", processedQuery, sourceFile);                           
                    }
                    catch(Exception e) {
                        reportError(e.getMessage(), query, processedQuery, sourceFile);                        
                        System.out.println("Error from inference engine: " + e.getMessage());
                    }
                }                                
                empty.tell(query.getFormula());
            }
            pw.println("  </entries>");
            pw.print("</ConsistencyCheck>");
        }
        catch (Exception e) {
            pw.println("  </entries>");
            pw.print("  <error>");
            pw.print("Error encountered while running consistency check.");
            pw.println("</error>");            
            pw.print("</ConsistencyCheck>");
            System.out.println(e.getMessage());    
            e.printStackTrace();
        }
        finally {
            KBmanager.getMgr().removeKB(ccheck_kb);
        }
    }

    /** *************************************************************
     * This initiates the consistency check
     */
    private void runConsistencyCheckNew() {

        String proof;
        KB empty = this.makeEmptyKB();
        try {
            pw.println("<ConsistencyCheck>");
            pw.println("  <kb>");
            pw.println("    " + kb.name);
            pw.println("  </kb>");
            Collection<Formula> allFormulas = kb.formulaMap.values();
            Collection<String> allTPTP= new ArrayList<String>();
            for (Formula f : allFormulas) {
                allTPTP.addAll(f.theTptpFormulas);
            }
            Iterator<String> it = allTPTP.iterator();
            pw.println("  <entries>");
            while (it.hasNext()) {
                String query = (String) it.next();
                System.out.println("CCheck.runConsistencyCheck: eprover: " + empty.eprover);

            }
            pw.println("  </entries>");
            pw.print("</ConsistencyCheck>");
        }
        catch (Exception e) {
            pw.println("  </entries>");
            pw.print("  <error>");
            pw.print("Error encountered while running consistency check.");
            pw.println("</error>");
            pw.print("</ConsistencyCheck>");
            System.out.println(e.getMessage());
            e.printStackTrace();
        }
        finally {
            KBmanager.getMgr().removeKB(ccheck_kb);
        }
    }

    /** *************************************************************
     * Picks the inference engine to use for the consistency check based on the
     * set-up inference engine.
     * 
     * @param empty
     *            - the kb to be used for the check
     * @param query
     *            - the statement to be checked
     * @return - the result of the query
     */
    private String askInferenceEngine(KB empty, String query) {

        String result = "";
        
        try {
            if (inferenceEngine.equals("EProver")) {
                result = empty.askEProver(query, timeOut, 1) + " ";
            }
            else if (inferenceEngine.equals("SInE")) {
                result = empty.askSInE(query, timeOut, 1);
            }
            else if (inferenceEngine.equals("LeoLocal")) {
                LEO leo = empty.askLeo(query, timeOut, 1);
            }
            else if (inferenceEngine.equals("SoTPTP")) {
                //result = InterfaceTPTP.queryTPTP(query, timeOut, 1, lineHtml,
                //        ieSettings.get("systemChosen"),
                //        ieSettings.get("location"),
                //        ieSettings.get("quietFlag"), empty.name,
                //        ieSettings.get("language"));
            }                        
            else throw new Exception("No inference engine.");
        }
        catch (Exception e) {
            System.out.println(e.getMessage());
            result = "ERROR [for query: " + query + "]: " + e.getMessage(); 
        }
        return result;
    }

    /** *************************************************************
     */
    @Override
    public void run() {
        runConsistencyCheck();
    }

}
