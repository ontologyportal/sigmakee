/** This code is copyright Articulate Software (c) 2003.  Some
portions copyright Teknowledge (c) 2003 and reused under the terms of the GNU license.
This software is released under the GNU Public License <http://www.gnu.org/copyleft/gpl.html>.
Users of this code also consent, by use of this code, to credit Articulate Software
and Teknowledge in any writings, briefings, publications, presentations, or
other representations of any software which incorporates, builds on, or uses this
code.  Please cite the following article in any publication with references:

Pease, A., (2003). The Sigma Ontology Development Environment,
in Working Notes of the IJCAI-2003 Workshop on Ontology and Distributed Systems,
August 9, Acapulco, Mexico.  See also sigmakee.sourceforge.net
*/

package com.articulate.sigma.tp;

import com.articulate.sigma.*;
import com.articulate.sigma.trans.Modals;
import com.articulate.sigma.trans.SUMOKBtoTPTPKB;
import com.articulate.sigma.trans.SUMOformulaToTPTPformula;
import com.articulate.sigma.trans.SUMOtoTFAform;
import com.articulate.sigma.trans.SessionTPTPManager;
import com.articulate.sigma.trans.THFnew;
import com.articulate.sigma.trans.TPTP3ProofProcessor;
import com.articulate.sigma.trans.TPTPGenerationManager;
import com.articulate.sigma.trans.TPTPutil;
import com.articulate.sigma.utils.FileUtil;
import com.articulate.sigma.utils.StringUtil;
import com.articulate.sigma.parsing.Expr;
import com.articulate.sigma.parsing.ExprToTHF;
import com.articulate.sigma.parsing.ExprToTPTP;
import com.articulate.sigma.parsing.FormulaAST;
import com.articulate.sigma.parsing.SuokifVisitor;
import com.articulate.sigma.parsing.CLIMapParser;

import tptp_parser.TPTPFormula;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Supplier;

/**
 * Class for invoking the latest research version of Vampire from Java
 * A previous version invoked the KIF version of Vampire from Java
 * but that's 15 years old now.  The current Vampire does TPTP3 output
 * instead of XML.

 * @author Andrei Voronkov
 * @since 14/08/2003, Acapulco
 * @author apease
 */
public class Vampire {

    /** Turn debugging logs on or off */
    public int debug = 1;
    /** ModeTypes: AVATAR is faster but doesn't provide answers, CASC (CADE Automated System Competition) is the mode used in competition, CUSTOM takes values from the env var */
    public enum ModeType {AVATAR, CASC, CUSTOM, VAMPIRE};
    /** Logic modes: FOL and HOL */
    public enum Logic { FOL, HOL }
    /** Knowledge base to be used for inference */
    private KB kb;
    /** The path where the vampire executable is found */
    private String executablePath;
    /** The path where the vampire executable is found */
    private String holExecutablePath;
    /** Directory of the knowledge base vampire will query against */
    private String inferenceFilePath;
    /**  */
    private String inferenceFileExtension;
    /** Session id to match the user to their assertions */
    private String sessionId;
    /** Temporary problem file generated for inference */
    private String tempProblemFilePath;
    /** Quantifier list in order for answer extraction */
    public StringBuilder qlist = null;
    /** The current logic mode (default to FOL) */
    public Logic logic = Logic.FOL;
    /** The language that vampire will process [fof|tff] */
    private String requestedTptpLanguage;
    /** Time in seconds that vampire will timeout */
    private int timeout;
    /** Max number of output answers vampire will give */
    public int maxAnswers;
    /** Commands to be run by the process */
    private List<String> commands;
    /** Mode to be run by vampire [AVATART|CASC|CUSTOM] */
    public ModeType mode = null;
    /**  */
    public boolean askQuestion = true;
    /**  */
    public boolean modensPonens = false;
    /** Storage variable for the output of Vampire */
    public List<String> output = new ArrayList<>();
    /** Full result structure for error handling */
    private ATPResult result;

    /*****************************************************************
     * This constructor is for cases like GenPropFormulas where there is
     * no SUMO KB and no inference file generation is needed.
     */
    public Vampire() {

        this.kb = null;
        this.sessionId = null;
        this.executablePath = KBmanager.getMgr().getPref("vampire");

        this.requestedTptpLanguage = "fof";
        this.inferenceFileExtension = "tptp";

        this.mode = ModeType.CASC;
        this.timeout = 30;
        this.maxAnswers = 1;
        this.askQuestion = true;
        this.logic = Logic.FOL;
        this.output = new ArrayList<>();
    }

    /*****************************************************************
     */
    public Vampire(KB kb) {

        this(kb, "tptp", "CASC", false, 30, 1);
    }

    /***************************************************************
     * Initialize a new Vampire Object with an Inference File and TPTP Language
     * @param kb
     * @param requestedTptpLang
     * @param mode Mode for Vampire [CASC|AVATAR|CUSTOM]. Can also be Profile, but this is set automatically in askVampireHOL.
     * @param modensPonens if true then Vampire will use implication
     * @param timeout
     * @param maxAnswers max number of answers to be returned
     */
    public Vampire(KB kb, String requestedTptpLang, String mode, boolean modensPonens, int timeout, int maxAnswers) {

        this(kb, requestedTptpLang, mode, modensPonens, timeout, maxAnswers, null);
    }

    /***************************************************************
     * Initialize a new Vampire Object with an Inference File, TPTP Language and Session ID
     * @param kb
     * @param requestedTptpLang
     * @param mode Mode for Vampire [CASC|AVATAR|CUSTOM]. Can also be Profile, but this is set automatically in askVampireHOL.
     * @param modensPonens if true then Vampire will use implication
     * @param timeout
     * @param maxAnswers max number of answers to be returned
     * @param sessionId Session ID for isolation
     */
    public Vampire(KB kb, String requestedTptpLang, String mode, boolean modensPonens, int timeout, int maxAnswers, String sessionId) {

        if (debug > 0) System.out.printf("\nVampire(%s, %s, %s, %b, %d, %d, %s)", kb.name, requestedTptpLang, mode, modensPonens, timeout, maxAnswers, sessionId);
        this.kb = kb;
        this.sessionId = sessionId;
        this.executablePath = KBmanager.getMgr().getPref("vampire");
        if ("fof".equalsIgnoreCase(requestedTptpLang) || "tptp".equalsIgnoreCase(requestedTptpLang)) {
            this.requestedTptpLanguage = "fof";
            this.inferenceFileExtension = "tptp";
        }
        else if ("tff".equalsIgnoreCase(requestedTptpLang)){
            this.requestedTptpLanguage = "tff";
            this.inferenceFileExtension = "tff";
        } else {
            this.requestedTptpLanguage = "thf";
            this.inferenceFileExtension = "thf";
            this.executablePath = this.executablePath.replace(
            File.separator + "build" + File.separator,
            File.separator + "build_hol" + File.separator
        );
        }
        System.out.println("VAMPIRE LANGUAGE: " + this.requestedTptpLanguage);
        this.modensPonens = modensPonens;
        if (mode.equalsIgnoreCase(ModeType.AVATAR.name())) this.mode = ModeType.AVATAR;
        if (mode.equalsIgnoreCase(ModeType.CASC.name())) this.mode = ModeType.CASC;
        if (mode.equalsIgnoreCase(ModeType.CUSTOM.name())) this.mode = ModeType.CUSTOM;
        if (mode.equalsIgnoreCase(ModeType.VAMPIRE.name())) this.mode = ModeType.VAMPIRE;
        this.timeout = timeout;
        this.maxAnswers = maxAnswers;
        this.inferenceFilePath = KBmanager.getMgr().getPref("kbDir") + File.separator + KBmanager.getMgr().getPref("sumokbname") + "." + this.inferenceFileExtension;
        if (!(new File(this.inferenceFilePath).exists()) || KBmanager.getMgr().infBaseFileOldIgnoringUserAssertions(this.inferenceFileExtension)) {
            System.out.println("INFO in KB.loadVampire(): this.inferenceFilePath=" + !(new File(this.inferenceFilePath).exists()));
            System.out.println("INFO in KB.loadVampire(): managerInfFileOld " + KBmanager.getMgr().infFileOld());
            synchronized (kb.baseGenLock) {
                TPTPGenerationManager.generateProperFile(kb, this.requestedTptpLanguage);
            }
        }
    }

    /***************************************************************
     * Set the sessionId
     */
    public void setSessionId(String sid) { this.sessionId = sid; }

    public static boolean isAvailable() {return Files.isRegularFile(Paths.get(KBmanager.getMgr().getPref("vampire")));}

    /***************************************************************
     * @param requestedLang The TPTP language format requested by the user ("fof" or "tff")
     */
    public void askVampire(String suoKifFormula) {

        if (debug > 0) System.out.printf("\nVampire.askVampire(%s)", suoKifFormula);
        FormulaPreprocessor fp = new FormulaPreprocessor();

        // FormulaAST fast path: KIFAST → preProcessExpr → Set<Expr>
        Set<Expr> processedExprs = null;
        try {
            KIFAST kifAst = new KIFAST();
            String parseErr = kifAst.parseStatement(suoKifFormula);
            if (parseErr == null && !kifAst.formulaMap.isEmpty()) {
                FormulaAST queryFA = kifAst.formulaMap.values().iterator().next();
                processedExprs = SessionTPTPManager.withSessionCache(
                        this.sessionId, this.kb, () -> fp.preProcessExpr(queryFA, true, this.kb));
            }
        } catch (Exception e) {
            if (debug > 0) System.err.println("Vampire.askVampire(): FormulaAST path failed, using string fallback: " + e.getMessage());
            processedExprs = null;
        }

        // String fallback path (TFF mode or AST path unavailable/empty)
        Set<Formula> processedStmts = null;
        if (processedExprs == null || processedExprs.isEmpty()) {
            if (debug > 0) System.err.println("Vampire.askVampire(): FormulaAST path failed or empty, using string fallback, for formula: " + suoKifFormula);
            Formula query = new Formula();
            query.read(suoKifFormula);
            processedStmts = SessionTPTPManager.withSessionCache(
                    this.sessionId, this.kb, () -> fp.preProcess(query, true, this.kb));
        }

        boolean hasProcessed = (processedExprs != null && !processedExprs.isEmpty())
                || (processedStmts != null && !processedStmts.isEmpty());

        if (hasProcessed) {
            int axiomIndex = 0;
            File inferenceFile = new File(this.inferenceFilePath);
            Set<String> tptpQuery = new HashSet<>();

            if (processedExprs != null && !processedExprs.isEmpty()) {
                for (Expr e : processedExprs) {
                    String kifStr = e.toKifString();
                    String tptpBody = ExprToTPTP.translateKifString(kifStr, true, this.requestedTptpLanguage);
                    if (tptpBody == null)
                        tptpBody = SUMOformulaToTPTPformula.tptpParseSUOKIFString(kifStr, true, this.requestedTptpLanguage);
                    String theTPTPstatement = this.requestedTptpLanguage + "(query_" + axiomIndex++ + ",conjecture,(" + tptpBody + ")).";
                    tptpQuery.add(theTPTPstatement);
                }
            }
            else {
                if (processedStmts.size() > 1) {
                    StringBuilder combined = new StringBuilder();
                    combined.append("(or ");
                    for (Formula p : processedStmts) combined.append(p.getFormula()).append(Formula.SPACE);
                    combined.append(Formula.RP);
                    String theTPTPstatement = this.requestedTptpLanguage + "(query" + "_" + axiomIndex++ +
                        ",conjecture,(" +
                        SUMOformulaToTPTPformula.tptpParseSUOKIFString(combined.toString(), true, this.requestedTptpLanguage)
                        + ")).";
                    tptpQuery.add(theTPTPstatement);
                }
                else {
                    String theTPTPstatement = this.requestedTptpLanguage + "(query" + "_" + axiomIndex++ +
                        ",conjecture,(" +
                        SUMOformulaToTPTPformula.tptpParseSUOKIFString(processedStmts.iterator().next().getFormula(), true, this.requestedTptpLanguage)
                        + ")).";
                    tptpQuery.add(theTPTPstatement);
                }
            }
            try {
                this.run(inferenceFile, tptpQuery);
            } catch (ATPException e) {
                throw e;
            } catch (Exception e) {
                System.err.printf("\nEXCEPTION IN Vampire.run(%s, %s)\n", inferenceFile.getName(), tptpQuery);
                throw new ATPException("\nVampire execution failed", e.getMessage());
            }
        }
        else System.err.println("Vampire.askVampire(): no TPTP formula translation for query: " + suoKifFormula);
        if (this.modensPonens) this.modensPonensPostProcess();
    }

    /*********************************************************************************
     * Vampire Modus Ponens with session-specific isolation.
     * STEPS:
     * 1 - AskVampire to get the first output
     * 2 - Process the output to keep only the authored axioms
     * 3 - Send new command to vampire with Modens Ponens options
     * 4 - If wanted drop the one premise formulas.
     * 5 - Replace the new proof's infRules with the original ones.
     * 6 - Return Vampire object for further processing from AskTell.jsp
     * @param suoKifFormula The query in SUO-KIF format
     * @return Vampire result object
     */
    public void askVampireModensPonens(String suoKifFormula) {

        if (debug > 0) System.out.printf("\nVampire.askVampireModensPonens()");
        // STEP 1 - use session-aware askVampire
        this.askVampire(suoKifFormula);
        // STEPS 2-6
        this.modensPonensPostProcess();
    }

    /*********************************************************************************
     * Post-process an initial Vampire result with Modus Ponens reasoning.
     * Extracts authored axioms, re-runs Vampire with MP options, optionally drops
     * one-premise formulas, and replaces inference rules.
     */
    private void modensPonensPostProcess() {

        if (debug > 0) System.out.printf("\nVampire.modensPonensPostProcess()");
        // STEP 2
        List<TPTPFormula> proof = TPTPutil.processProofLines(this.output);
        List<TPTPFormula> authored_lines = TPTPutil.writeMinTPTP(proof);
        // STEP 3
        File kbFile = new File("min-problem.tptp");
        //vampire --mode vampire --forced_options av=off:nm=0:bce=off:updr=off:fde=none:rp=off --proof tptp -m 16384 -t %d %s
        this.commands = new ArrayList<>(Arrays.asList(
                "--input_syntax","tptp",
                "--proof","tptp",
                "-av","off","-nm","0","-fsr","off","-fd","off","-bd","off",
                "-fde","none","-updr","off","-rp","off","-bce","off"
        ));
        if (this.askQuestion){
            this.commands.add("-qa plain");
        }
        try{
            this.runCustom(kbFile);
            this.output = TPTPutil.clearProofFile(this.output);
        }
        catch (ATPException e){
            throw e;
        }
        catch (Exception e){
            throw new ATPException("Vampire ModensPonens execution failed: " + e.getMessage(), "Vampire");
        }
        // STEP 4
        if (this.kb.dropOnePremiseFormulas) {
            this.output = TPTPutil.dropOnePremiseFormulasFOF(this.output);
        }
        // STEP 5
        this.output = TPTPutil.replaceFOFinfRule(this.output, authored_lines);
    }

    /******************************************************************
     * Ask Vampire HOL using the existing <kbName>.thf axioms.
     * Input  : SUO-KIF query string (stmt).
     * Output : Vampire object with HOL proof output.
     */
    public void askVampireHOL(String stmt, boolean useModals) {

        if (debug > 0) System.out.printf("\nVampire.askVampireHOL(%s, %b)", stmt, useModals);
        KBmanager mgr = KBmanager.getMgr();
        if (useModals)
            System.out.println("==== Using Modals/HOL mode ====");
        else
            System.out.println("==== Using plain HOL mode ====");
        try {
            String dir;
            if (this.sessionId != null && !this.sessionId.isEmpty()) {
                dir = SessionTPTPManager.getSessionDir(this.sessionId).toString() + File.separator;
            } else {
                dir = mgr.getPref("kbDir") + File.separator;
            }

            // -------- 1. Ensure base <kb>.thf exists (modal vs plain) --------
            String kbThfFile = "";
            if (useModals) {
                kbThfFile = this.kb.name + "_modals.thf";
            }
            else {
                kbThfFile = this.kb.name + "_plain.thf";
            }
            // Base THF axiom file is always in the shared kbDir (no session-specific THF versions exist)
            String kbThfPath = mgr.getPref("kbDir") + File.separator + kbThfFile;
            File thfAxioms = new File(kbThfPath);
            if (!thfAxioms.exists()) {
                System.out.println("Vampire.askVampireHOL(): no such file: " + kbThfPath + ". Waiting for background generation or creating it.");
                // Wait for background THF generation if in progress, otherwise generate synchronously
                if (useModals) {
                    if (!TPTPGenerationManager.waitForTHFModal(600)) {
                        System.out.println("Vampire.askVampireHOL(): Background generation not ready, generating THF Modal synchronously");
                        THFnew.transModalTHF(this.kb);
                    }
                } else {
                    if (!TPTPGenerationManager.waitForTHFPlain(600)) {
                        System.out.println("Vampire.askVampireHOL(): Background generation not ready, generating THF Plain synchronously");
                        THFnew.transPlainTHF(this.kb);
                    }
                }
            }
            // -------- 2. Prepare temp-stmt.thf and temp-comb.thf (mirrors FOF/TFF run() pattern) --------
            String stmtFile = dir + "temp-stmt." + this.inferenceFileExtension;
            String outfile = dir + "temp-comb." + this.inferenceFileExtension;
            File fout = new File(outfile);
            if (fout.exists()) fout.delete();
            File fstmt = new File(stmtFile);
            if (fstmt.exists()) fstmt.delete();

            // -------- 3. Parse SUO-KIF query using FormulaAST --------
            SuokifVisitor sv = SuokifVisitor.parseSentence(stmt);
            if (sv.result.isEmpty()) {
                System.err.println("Vampire.askVampireHOL(): failed to parse query: " + stmt);
                return;
            }
            FormulaAST fa = sv.result.get(0);
            if (fa.expr == null) {
                System.err.println("Vampire.askVampireHOL(): null expr for query: " + stmt);
                return;
            }
            if (debug>1) System.out.println("Vampire.askVampireHOL(): parsed expr: " + fa.expr.toKifString());
            // -------- 4. Preprocess and build typeMap --------
            FormulaPreprocessor fp = new FormulaPreprocessor();
            Map<String, Set<String>> typeMap = new HashMap<>();
            Set<Expr> processed;
            if (useModals) {
                Map.Entry<Expr, Map<String, Set<String>>> modalResult = SessionTPTPManager.withSessionCache(
                        this.sessionId, this.kb, () -> Modals.processModalsExpr(fa.expr, this.kb));
                Expr resExpr = modalResult.getKey();
                if (resExpr == null) {
                    System.err.println("Vampire.askVampireHOL(): processModalsExpr returned null for: " + stmt);
                    return;
                }
                if (debug>1) System.out.println("Vampire.askVampireHOL(): modalized expr: " + resExpr.toKifString());
                processed = SessionTPTPManager.withSessionCache(
                        this.sessionId, this.kb, () -> fp.preProcessExpr(fa, true, this.kb));
                if (processed == null || processed.isEmpty()) {
                    System.err.println("Vampire.askVampireHOL(): preProcessExpr returned empty for: " + stmt);
                    return;
                }
                typeMap.putAll(SessionTPTPManager.withSessionCache(
                        this.sessionId, this.kb, () -> fp.findTypeRestrictionsExpr(resExpr, this.kb)));
                typeMap.putAll(modalResult.getValue());
                String primaryWorldVar = Modals.makeWorldVarExpr(fa.expr);
                typeMap.put(primaryWorldVar, new HashSet<>(Collections.singleton("World")));
                Modals.markModalAttributeFormulaVarsExpr(fa.expr, typeMap);
                if (debug>1) System.out.println("Vampire.askVampireHOL(): typeMap: " + typeMap);
            } else {
                processed = SessionTPTPManager.withSessionCache(
                        this.sessionId, this.kb, () -> fp.preProcessExpr(fa, true, this.kb));
                if (processed == null || processed.isEmpty()) {
                    System.err.println("Vampire.askVampireHOL(): preProcessExpr returned empty for: " + stmt);
                    return;
                }
                typeMap.putAll(SessionTPTPManager.withSessionCache(
                        this.sessionId, this.kb, () -> fp.findTypeRestrictionsExpr(fa.expr, this.kb)));
            }
            if (debug>1) System.out.println("Vampire.askVampireHOL(): preprocessed exprs: " + processed.size());
            // -------- 5. Collect conjectures into temp-stmt.thf, then concat with base --------
            Set<String> conjectureStmts = new LinkedHashSet<>();
            int conjIndex = 0;
            for (Expr e : processed) {
                if (SUMOKBtoTPTPKB.hasUnresolvedPredVar(e)) continue;
                String thfQuery;
                if (useModals) {
                    Map.Entry<Expr, Map<String, Set<String>>> fmodalResult = SessionTPTPManager.withSessionCache(
                            this.sessionId, this.kb, () -> Modals.processModalsExpr(e, this.kb));
                    Expr fmodal = fmodalResult.getKey();
                    if (fmodal == null) continue;
                    thfQuery = ExprToTHF.translate(fmodal, true, typeMap);
                } else {
                    thfQuery = ExprToTHF.translateNonModal(e, true, typeMap);
                }
                if (thfQuery == null || thfQuery.isEmpty()) continue;
                String conjName = "user_conj_" + (conjIndex++);
                String final_query = "thf(" + conjName + ",conjecture," + thfQuery + ").";
                conjectureStmts.add(final_query);
                if (debug>1) System.out.println("Vampire.askVampireHOL(): final query: " + final_query);
            }
            List<String> userAsserts = getUserAssertions(this.kb, this.sessionId);
            Set<String> allStmts = new LinkedHashSet<>(userAsserts);
            allStmts.addAll(conjectureStmts);
            writeStatements(allStmts);
            writeIncludeProblem(kbThfPath, stmtFile, outfile);
            // -------- 6. Actually call Vampire on temp-comb.thf --------
            if (debug>1) System.out.println("------ Vampire.askVampireHOL(): Asking Vampire");
            this.askVampireTHF(outfile);
        } catch (ATPException e) {
            throw e; // Preserve type + payload for proper error handling in UI
        } catch (Exception e) {
            System.out.println("Vampire.askVampireHOL(): Exception: " + e.getMessage());
            e.printStackTrace();
            throw new ATPException("Vampire HOL execution failed: " + e.getMessage(), "Vampire");
        }
    }

    /******************************************************************
     * Executes the Vampire automated theorem prover with higher-order logic (HOL) mode
     * on a given THF problem file. This method processes the includes
     * and runs the Vampire prover with the specified parameters.
     * @param test_path The file path of the TPTP problem to be processed.
     * @return A Vampire instance populated with the results of the proof attempt.
     */
    public void askVampireTHF(String test_path) {

        if (debug > 0) System.out.printf("\nVampire.askVampireTHF(%s)", test_path);
        String testDir = KBmanager.getMgr().getPref("inferenceTestDir");
        String includesPath = testDir + File.separator + "includes";
        File test = new File(test_path);
        List<String> includes = TPTPutil.extractIncludesFromTPTP(test);
        if (!includes.isEmpty()) {
            String error = TPTPutil.validateIncludesInTPTPFiles(includes, includesPath);
            if (error != null) System.err.println(error);
        }
        this.commands = new ArrayList<>(Arrays.asList(
            "--proof", "tptp",
            "--output_axiom_names","on"
        ));
        // This HOL Vampire version (4.8) does not support "-qa plain"
        if (!includes.isEmpty()){
            this.commands.add("--include");
            this.commands.add(includesPath);
        }

        if (this.askQuestion){
            this.commands.add("-qa plain");
        }

        this.logic = Vampire.Logic.HOL;
        try{
            this.runCustom(test);
        } catch (ATPException e){
            throw e;
        } catch (Exception e){
            throw new ATPException("Vampire THF execution failed: " + e.getMessage(), "Vampire");
        }
    }

    /***************************************************************
     * Return a SUMO-formatted proof string.
     * @param suoKifFormula format this formula.
     * @return the formatted formula.
     */
    public String askVampireFormat(String suoKifFormula) {

        if (debug > 0) System.out.printf("\nVampire.askVampireFormat(%s)", suoKifFormula);
        StringBuilder sb = new StringBuilder();
        if (!StringUtil.emptyString(System.getenv("VAMPIRE_OPTS")))
            this.mode = Vampire.ModeType.CUSTOM;
        else
            this.mode = Vampire.ModeType.CASC;
        this.askVampire(suoKifFormula);
        TPTP3ProofProcessor tpp = new TPTP3ProofProcessor();
        tpp.parseProofOutput(this.output, suoKifFormula, this.kb, this.qlist);
        sb.append(tpp.proof.toString().trim()).append("\n").append("answers: ").append(tpp.bindings.toString()).append("\n");
        return sb.toString();
    }

    /***************************************************************
     * Get the full ATPResult for this run.
     * This provides detailed execution metadata, SZS status, and error info.
     * @return The ATPResult, or null if run() hasn't been called
     */
    public ATPResult getResult() {return result;}

    /***************************************************************
     * Get the SZS status from the last run.
     *
     * @return The SZSStatus, or SZSStatus.NOT_RUN if not run
     */
    public SZSStatus getSzsStatus() {return result != null ? result.getSzsStatus() : SZSStatus.NOT_RUN;}

    /***************************************************************
     * Check if there was an error during execution.
     * @return true if the result indicates an error
     */
    public boolean hasError() {return result != null && result.hasErrors();}

    /***************************************************************
     *
     */
    private void createCommandList(File kbFile) {

        if (debug > 0) System.out.printf("\nVampire.createCommandList(%s)", kbFile.getName());
        String space = Formula.SPACE;
        StringBuilder options = new StringBuilder("--output_axiom_names").append(space).append("on").append(space).append("--proof").append(space).append("tptp").append(space);;
        if (this.mode == ModeType.AVATAR) {
            options.append("-av").append(space).append("on").append(space).append("-p").append(space).append("tptp").append(space);
            if (askQuestion) options.append("-qa").append(space).append("plain").append(space);
            options.append("-t").append(space);
        }
        if (this.mode == ModeType.CASC) {
            options.append("--mode").append(space).append("casc").append(space); // NOTE: [--mode casc] is a shortcut for [--mode portfolio --schedule casc --proof tptp]
            if (askQuestion) options.append("-qa").append(space).append("plain").append(space);
            options.append("-t").append(space);
        }
        if (this.mode == ModeType.VAMPIRE) {
            options.append("--mode").append(space).append("vampire").append(space);
            if (askQuestion) options.append("-qa").append(space).append("plain").append(space);
            options.append("-t").append(space);
        }
        if (this.mode == ModeType.CUSTOM) {
            if (askQuestion) options.append("-qa").append(space).append("plain").append(space);
            options.append(System.getenv("VAMPIRE_OPTS")).append(space);
        }
        this.commands = new ArrayList<>();
        this.commands.add(this.executablePath.toString());
        Collections.addAll(this.commands, options.toString().split(Formula.SPACE));
        this.commands.add(Integer.toString(this.timeout));
        this.commands.add(kbFile.toString());
    }

    /***************************************************************
     * don't include a timeout if timeout is 0
     * @param executable
     * @param timeout
     * @param kbFile
     * @param commands
     */
    private void createCustomCommandList(File executable, int timeout, File kbFile, Collection<String> commands) {

        if (debug > 0) System.out.printf("\nVampire.createCustomCommandList(%s, %d, %s, %s)", executable.getName(), timeout, kbFile.getName(), commands);
        String space = Formula.SPACE;
        StringBuilder opts = new StringBuilder();
        boolean callerSuppliesMode = commands.contains("--mode");
        if (!callerSuppliesMode) {
            if (mode == ModeType.AVATAR) {
                opts.append("-av").append(space).append("on").append(space)
                    .append("-p").append(space).append("tptp").append(space);
            }
            else if (mode == ModeType.CASC) {
                opts.append("--mode").append(space).append("casc").append(space);
            }
            else if (mode == ModeType.CUSTOM) {
                opts.append(System.getenv("VAMPIRE_OPTS")).append(space);
            }
            else if (mode == ModeType.VAMPIRE) {
                opts.append("--mode").append(space).append("vampire").append(space);
            }
            else {
                System.err.println("Error in Vampire.createCustomCommandList(): no mode selected");
            }
        }
        for (String s : commands) opts.append(s).append(space);
        System.out.println("h-1");
        if (timeout != 0) {
            opts.append("-t").append(space);
            opts.append(timeout).append(space);
        }
        opts.append(kbFile.toString());
        String[] optar = opts.toString().split(Formula.SPACE);
        System.out.println("h0");
        this.commands = new ArrayList<>();
        System.out.println("h01");
        this.commands.add(executable.toString());
        System.out.println("h1");
        Collections.addAll(this.commands, optar);
        System.out.println("h2");
    }

    /***************************************************************
     * Creates a running instance of Vampire and collects its output
     * @param kbFile Initial knowledge base to be loaded by the Vampire executable.
     * @param timeout the time given for Vampire to finish execution
     * @throws ExecutableNotFoundException if the Vampire executable is not found
     * @throws ProverCrashedException if Vampire crashes with a non-zero exit code
     * @throws ProverTimeoutException if Vampire times out
     * @throws Exception for other errors
     */
    public void run(File kbFile) throws Exception {

        if (debug > 0) System.out.printf("\nVampire.run(%s)", kbFile.getName());
        this.output.clear();
        long startTime = System.currentTimeMillis();
        long timeoutMs = this.timeout * 1000L;
        this.result = new ATPResult.Builder()
                .engineName("Vampire")
                .engineMode(mode != null ? mode.name() : "CASC")
                .inputLanguage(SUMOKBtoTPTPKB.getLang().toUpperCase())
                .inputSource(kbFile != null ? kbFile.getName() : "unknown")
                .timeoutMs(timeoutMs)
                .build();
        File executable = new File(this.executablePath);
        if (!executable.exists()) {
            String msg = "Error in Vampire.run(): no executable " + this.executablePath;
            System.err.println(msg);
            this.result.setSzsStatus(SZSStatus.OS_ERROR);
            this.result.setPrimaryError(msg);
            throw new ExecutableNotFoundException("Vampire", this.executablePath, "vampire");
        }
        createCommandList(kbFile);
        this.result.setCommandLine(this.commands);
        System.out.println("Vampire.run(): command: " + String.join(" ", this.commands));
        ProcessBuilder _builder = new ProcessBuilder(this.commands);
        _builder.redirectErrorStream(false);  // Keep stderr separate for better error capture
        Process _vampire = _builder.start();
        List<String> stdoutLines = new ArrayList<>();
        List<String> stderrLines = new ArrayList<>();
        Thread stderrReader = new Thread(() -> {
            try (BufferedReader r = new BufferedReader(new InputStreamReader(_vampire.getErrorStream()))) {
                String line;
                while ((line = r.readLine()) != null) stderrLines.add(line);
            } catch (IOException e) {
                // Ignore - stream may close on process termination
            }
        });
        stderrReader.start();
        try (BufferedReader _reader = new BufferedReader(new InputStreamReader(_vampire.getInputStream()))) {
            String line;
            while ((line = _reader.readLine()) != null) {
                stdoutLines.add(line);
                this.output.add(line);
            }
        }
        stderrReader.join(5000);
        int exitValue = _vampire.waitFor();
        long elapsed = System.currentTimeMillis() - startTime;
        this.result.setStdout(stdoutLines);
        this.result.setStderr(stderrLines);
        this.result.setQList(this.qlist);
        this.result.finalize(exitValue, elapsed, elapsed >= timeoutMs);
        if (exitValue != 0) {
            System.err.println("Error in Vampire.run(): Abnormal process termination (exit code " + exitValue + ")");
            if (!stderrLines.isEmpty()) {
                System.err.println("Stderr: " + stderrLines);
            }
            if (this.result.isTimedOut() || this.result.getSzsStatus() == SZSStatus.TIMEOUT) {
                throw new ProverTimeoutException("Vampire", timeoutMs, elapsed, false, stdoutLines, stderrLines, this.result);
            } else if (exitValue > 128 && exitValue < 160) {
                throw new ProverCrashedException("Vampire", exitValue, stdoutLines, stderrLines, this.result);
            } else if (this.result != null
                    && this.result.getStderr() != null
                    && !this.result.getStderr().isEmpty()
                    && this.result.getStderr().get(0) != null
                    && (this.result.getStderr().get(0).contains("% Exception at proof search level") || (this.result.getStderr().get(0).contains("Parser exception")))
                    && this.result.getStderr().size() > 1)
            {
                int lineNo = -1;
                if (this.result.getStderr().size() > 1 && this.result.getStderr().get(1) != null) {
                    Matcher m = Pattern.compile("Parsing Error on line\\s+(\\d+)").matcher(this.result.getStderr().get(1));
                    if (m.find()) {
                        lineNo = Integer.parseInt(m.group(1));
                    }
                }
                String msg = "Vampire: exception at proof search level" + (lineNo > 0 ? " (Parsing Error on line " + lineNo + ")" : "");
                throw new FormulaTranslationException(msg, this.result.getInputLanguage(), lineNo, stdoutLines, stderrLines);
            }
        }
    }

    /***************************************************************
     * Creates a running instance of Vampire adding a set of statements
     * in TFF or TPTP language to a file and then calling Vampire.
     * Note that any query must be given as a "conjecture"
     * @param kb the current knowledge base
     * @param kbFile the current knowledge base TPTP file
     * @param timeout the timeout given to Vampire to find a proof
     * @param stmts a Set of user assertions
     * @param sessionId explicit HTTP session ID for temp file isolation;
     *                  if null, falls back to extracting sessionId from kbFile path
     * @throws Exception of something goes south
     */
    public void run(File kbFile, Set<String> stmts) throws Exception {

        if (debug > 0) System.out.printf("\nVampire.run(%s, %s)", kbFile.getName(), stmts);
        this.output.clear();
        String lang = "tff";
        if (SUMOKBtoTPTPKB.getLang().equalsIgnoreCase("fof"))
            lang = "tptp";
        if (this.sessionId == null || this.sessionId.isEmpty()) {
            String kbFilePath = kbFile.getAbsolutePath();
            if (kbFilePath.contains(File.separator + "sessions" + File.separator)) {
                String[] parts = kbFilePath.split(File.separator + "sessions" + File.separator);
                if (parts.length > 1) {
                    String remainder = parts[1];
                    int nextSep = remainder.indexOf(File.separator);
                    if (nextSep > 0) {
                        this.sessionId = remainder.substring(0, nextSep);
                    }
                }
            }
        }
        if (this.sessionId != null && !this.sessionId.isEmpty()) {
            System.out.println("INFO Vampire.run(): using session dir for temp files, sessionId=" + sessionId);
        }
        String dir;
        if (this.sessionId != null && !this.sessionId.isEmpty()) {
            dir = SessionTPTPManager.getSessionDir(this.sessionId).toString() + File.separator;
        }
        else {
            dir = KBmanager.getMgr().getPref("kbDir") + File.separator;
        }
        String outfile = dir + "temp-comb." + this.inferenceFileExtension;
        String stmtFile = dir + "temp-stmt." + this.inferenceFileExtension;

        // Resolve base file: prefer session-specific TPTP if it exists, otherwise use the provided kbFile (usually shared)
        File baseFile = kbFile;
        if (this.sessionId != null && !this.sessionId.isEmpty()) {
            Path sessionPath = SessionTPTPManager.getSessionTPTPPath(this.sessionId, this.kb.name, this.inferenceFileExtension);
            if (Files.exists(sessionPath)) {
                if (debug > 0) System.out.println("Vampire.run(): Using session-specific base file: " + sessionPath);
                baseFile = sessionPath.toFile();
            }
        }

        File fout = new File(outfile);
        if (fout.exists())
            fout.delete();
        File fstmt = new File(stmtFile);
        if (fstmt.exists())
            fstmt.delete();
        List<String> userAsserts = getUserAssertions(this.kb, this.sessionId);
        if (userAsserts != null && stmts != null)
            stmts.addAll(userAsserts);
        else {
            System.err.println("Error in Vampire.run(): null query or user assertions set");
            return;
        }
        writeStatements(stmts);
        concatFiles(baseFile.toString(), stmtFile, outfile);
        File comb = new File(outfile);
        this.run(comb);
    }

    /***************************************************************
     * Creates a running instance of Vampire with custom command line options.
     * @param kbFile Initial knowledge base loaded by the Vampire executable.
     * @throws ExecutableNotFoundException if the Vampire executable is not found
     * @throws ProverCrashedException if Vampire crashes with a non-zero exit code
     * @throws ProverTimeoutException if Vampire times out
     * @throws Exception for other errors
     */
    public void runCustom(File kbFile) throws Exception {

        if (debug > 0) System.out.printf("\nVampire.runCustom(%s)", kbFile.getName());
        long startTime = System.currentTimeMillis();
        long timeoutMs = timeout * 1000L;
        output = new ArrayList<>();
        // Determine which executable to use
        String configKey = "vampire";
        this.executablePath = KBmanager.getMgr().getPref("vampire");
        result = new ATPResult.Builder()
                .engineName("Vampire")
                .engineMode(this.logic == Logic.HOL ? "HOL" : (this.mode != null ? this.mode.name() : "CUSTOM"))
                .inputLanguage(this.logic == Logic.HOL ? "THF" : SUMOKBtoTPTPKB.getLang().toUpperCase())
                .inputSource(kbFile != null ? kbFile.getName() : "unknown")
                .timeoutMs(timeoutMs)
                .build();
        if (StringUtil.emptyString(this.executablePath)) {
            String msg = "Error in Vampire.runCustom(): no executable string in preferences";
            System.err.println(msg);
            result.setSzsStatus(SZSStatus.OS_ERROR);
            result.setPrimaryError(msg);
            throw new ExecutableNotFoundException("Vampire", this.executablePath, configKey);
        }
        File executable = new File(this.executablePath);
        if (!executable.exists()) {
            String msg = "Error in Vampire.runCustom(): no executable " + this.executablePath;
            System.err.println(msg);
            result.setSzsStatus(SZSStatus.OS_ERROR);
            result.setPrimaryError(msg);
            throw new ExecutableNotFoundException("Vampire", this.executablePath, configKey);
        }
        createCustomCommandList(new File(this.executablePath), this.timeout, kbFile.getAbsoluteFile(), this.commands);
        System.out.println("Vampire.runCustom(): command: " + String.join(" ", this.commands));
        result.setCommandLine(this.commands);
        ProcessBuilder _builder = new ProcessBuilder(this.commands);
        _builder.redirectErrorStream(false);  // Keep stderr separate
        if (kbFile != null && kbFile.getParentFile() != null) {
            _builder.directory(kbFile.getParentFile());
            System.out.println("Vampire CWD: " + _builder.directory().getAbsolutePath());
        }
        if (debug > 0) System.out.println("Vampire.runCustom(): running with commands " + String.join(" ", this.commands));
        Process _vampire = _builder.start();
        List<String> stdoutLines = new ArrayList<>();
        List<String> stderrLines = new ArrayList<>();
        Thread stderrReader = new Thread(() -> {
            try (BufferedReader r = new BufferedReader(new InputStreamReader(_vampire.getErrorStream()))) {
                String line;
                while ((line = r.readLine()) != null) {
                    stderrLines.add(line);
                }
            } catch (IOException e) {
                // Ignore
            }
        });
        stderrReader.start();
        try (BufferedReader _reader = new BufferedReader(new InputStreamReader(_vampire.getInputStream()))) {
            String line;
            while ((line = _reader.readLine()) != null) {
                stdoutLines.add(line);
                output.add(line);
            }
        }
        stderrReader.join(5000);
        int exitValue = _vampire.waitFor();
        long elapsed = System.currentTimeMillis() - startTime;
        result.setStdout(stdoutLines);
        result.setStderr(stderrLines);
        result.finalize(exitValue, elapsed, elapsed >= timeoutMs);
        if (exitValue != 0) {
            System.err.println("Error in Vampire.runCustom(): Abnormal process termination (exit code " + exitValue + ")");
            if (!stderrLines.isEmpty()) {
                System.err.println("Stderr: " + stderrLines);
            }
            if (result.isTimedOut() || result.getSzsStatus() == SZSStatus.TIMEOUT) {
                throw new ProverTimeoutException("Vampire", timeoutMs, elapsed, false, stdoutLines, stderrLines, result);
            } else if (exitValue > 128 && exitValue < 160) {
                throw new ProverCrashedException("Vampire", exitValue, stdoutLines, stderrLines, result);
            } else if (result != null
                && result.getStderr() != null
                && !result.getStderr().isEmpty()
                && result.getStderr().get(0) != null
                && (result.getStderr().get(0).contains("% Exception at proof search level") || (result.getStderr().get(0).contains("Parser exception")))
                && result.getStderr().size() > 1)
            {
                int lineNo = -1;
                if (result.getStderr().size() > 1 && result.getStderr().get(1) != null) {
                    Matcher m = Pattern.compile("Parsing Error on line\\s+(\\d+)").matcher(result.getStderr().get(1));
                    if (m.find()) lineNo = Integer.parseInt(m.group(1));
                }
                String msg = "Vampire: exception at proof search level" + (lineNo > 0 ? " (Parsing Error on line " + lineNo + ")" : "");
                throw new FormulaTranslationException(msg, result.getInputLanguage(), lineNo, stdoutLines, stderrLines);
            }
        }
        System.out.println("Vampire.runCustom() done executing");
    }

    /*****************************************************************
     * Write all the strings in stmts to temp-stmt.[tptp|tff|thf]
     * When sessionId is provided, writes to the session-specific directory,
     * creating it first if it does not yet exist.
     * @param stmts write all of these to temp-stmt
     */
    public void writeStatements(Set<String> stmts) {

        if (debug > 0) System.out.printf("\nVampire.writeStatements(%s)", stmts);
        String dir;
        if (sessionId != null && !sessionId.isEmpty()) {
            java.nio.file.Path sessionDir = SessionTPTPManager.getSessionDir(this.sessionId);
            if (!java.nio.file.Files.exists(sessionDir)) {
                try {
                    java.nio.file.Files.createDirectories(sessionDir);
                    if (debug>1) System.out.println("Vampire.writeStatements(): created session dir " + sessionDir);
                }
                catch (IOException ex) {
                    System.err.println("Error in writeStatements(): could not create session dir " + sessionDir);
                    ex.printStackTrace();
                }
            }
            dir = sessionDir.toString();
        }
        else {
            dir = KBmanager.getMgr().getPref("kbDir");
        }
        String fname = "temp-stmt." + this.inferenceFileExtension;
        try (FileWriter fw = new FileWriter(dir + File.separator + fname);
            PrintWriter pw = new PrintWriter(fw)) {
            for (String s : stmts) pw.println(s);
        }
        catch (IOException e) {
            System.err.println("Error in writeStatements(): " + e.getMessage());
            System.err.println("Error writing file " + dir + File.separator + fname + "\n" + e.getMessage());
            e.printStackTrace();
        }
    }

    /*****************************************************************
     * Read in two files and write their contents to a new file
     * @param f1 file to concat
     * @param f2 file to concat
     * @param fout output of concat of f1 + f2
     */
    public void concatFiles(String f1, String f2, String fout) throws IOException {

        if (debug > 0) System.out.printf("\nVampire.concatFiles(%s, %s, %s)", f1, f2, fout);
        File f1file = new File(f1);
        File f2file = new File(f2);
        if (!f1file.exists())
            System.err.println("ERROR in concatFiles(): " + f1 + " does not exist");
        if (!f2file.exists())
            System.err.println("ERROR in concatFiles(): " + f2 + " does not exist");
        try (PrintWriter pw = new PrintWriter(fout);
            BufferedReader br = new BufferedReader(new FileReader(f1))) {
            String line = br.readLine();
            while (line != null) {
                pw.println(line);
                line = br.readLine();
            }
            try (BufferedReader bufr = new BufferedReader(new FileReader(f2))) {
                line = bufr.readLine();
                while (line != null) {
                    pw.println(line);
                    line = bufr.readLine();
                }
            }
        }
    }

    /*****************************************************************
     * Write a TPTP/THF problem file that includes the base KB file
     * rather than physically concatenating the base KB into temp-comb.thf.
     *
     * @param baseFile base THF axiom file, e.g. SUMO_plain.thf or SUMO_modals.thf
     * @param stmtFile temp-stmt.thf containing user assertions and conjectures
     * @param outfile temp-comb.thf wrapper file given to Vampire
     */
    public void writeIncludeProblem(String baseFile, String stmtFile, String outfile) throws IOException {

        if (debug > 0)
            System.out.printf("\nVampire.writeIncludeProblem(%s, %s, %s)", baseFile, stmtFile, outfile);

        Path basePath = Paths.get(baseFile).toAbsolutePath().normalize();
        Path stmtPath = Paths.get(stmtFile).toAbsolutePath().normalize();
        Path outPath = Paths.get(outfile).toAbsolutePath().normalize();

        if (!Files.exists(basePath))
            System.err.println("ERROR in writeIncludeProblem(): " + basePath + " does not exist");

        if (!Files.exists(stmtPath))
            System.err.println("ERROR in writeIncludeProblem(): " + stmtPath + " does not exist");

        try (PrintWriter pw = new PrintWriter(Files.newBufferedWriter(outPath));
            BufferedReader br = Files.newBufferedReader(stmtPath)) {

            pw.println("% Base SUMO axioms");
            pw.println("include('" + escapeTPTPPath(basePath.toString()) + "').");
            pw.println();

            pw.println("% User assertions and conjectures");
            String line;
            while ((line = br.readLine()) != null) {
                pw.println(line);
            }
        }
    }

    /*****************************************************************
     * Escape file paths for single-quoted TPTP include atoms.
     */
    private static String escapeTPTPPath(String path) {

        return path.replace("\\", "/").replace("'", "\\'");
    }

    /***************************************************************
     * Get user assertions with optional session isolation.
     * @param kb The knowledge base
     * @param sessionId Optional HTTP session ID for session-specific UA files.
     *                  If null or empty, uses shared UA files.
     * @return List of user assertion TPTP formulas
     */
    public List<String> getUserAssertions(KB kb, String sessionId) {

        if (debug > 0) System.out.printf("\nVampire.getUserAssertions(%s, %s)", kb.name, sessionId);
        return kb.withUserAssertionLock(() -> {
            String userAssertionTPTP = kb.name + KB._userAssertionsTPTP;
            if ("tff".equals(this.inferenceFileExtension))
                userAssertionTPTP = kb.name + KB._userAssertionsTFF;
            else if ("thf".equals(this.inferenceFileExtension))
                userAssertionTPTP = kb.name + KB._userAssertionsTHF;
            File dir;
            if (sessionId != null && !sessionId.isEmpty()) {
                java.nio.file.Path sessionDir = com.articulate.sigma.trans.SessionTPTPManager.getSessionDir(sessionId);
                dir = sessionDir.toFile();
            } else {
                dir = new File(KBmanager.getMgr().getPref("kbDir"));
            }
            String fname = dir + File.separator + userAssertionTPTP;
            File ufile = new File(fname);
            if (ufile.exists())
                return FileUtil.readLines(fname, false);
            else
                return new ArrayList<>();
        });
    }

    /***************************************************************
     * Add an assertion for inference.
     * @param userAssertionTPTP asserted formula in the TPTP/TFF syntax
     * @param kb Knowledge base
     * @param parsedFormulas a lit of parsed formulas in KIF syntax
     * @param tptp convert formula to TPTP if tptp = true
     * @return true if all assertions are added for inference
     *
     * TODO: This function might not be necessary if we find a way to
     * directly add assertion into opened inference engine (e_ltb_runner)
     */
    public boolean assertFormula(String userAssertionTPTP, KB kb, List<Formula> parsedFormulas, boolean tptp) {

        System.out.printf("\nVampire.assertFormula(%s, %s, %s, %b)", userAssertionTPTP, kb.name, parsedFormulas, tptp);
        boolean allAdded = false;
        Set<Formula> processedFormulas = new HashSet();
        FormulaPreprocessor fp = new FormulaPreprocessor();
        Set<String> tptpFormulas = new HashSet<>();
        String tptpStr;
        int axiomIndex = 0;
        try (PrintWriter pw = new PrintWriter(new BufferedWriter(new FileWriter(userAssertionTPTP, true)))) {
            for (Formula parsedF : parsedFormulas) {
                processedFormulas.clear();
                processedFormulas.addAll(fp.preProcess(parsedF,false, kb));
                if (processedFormulas.isEmpty())
                    allAdded = false;
                else {   // 2. Translate to TPTP/TFF.
                    tptpFormulas.clear();
                    if (tptp) {
                        for (Formula p : processedFormulas) {
                            if (!p.isHigherOrder(kb)) {
                                tptpStr = SUMOformulaToTPTPformula.tptpParseSUOKIFString(p.getFormula(), false);
                                tptpFormulas.add(tptpStr);
                            }
                        }
                    }
                    // 3. Write to new tptp file
                    for (String theTPTPFormula : tptpFormulas) {
                        pw.print(SUMOformulaToTPTPformula.getLang() + "(kb_" + kb.name + "_UserAssertion" + "_" + axiomIndex++);
                        pw.println(",axiom,(" + theTPTPFormula + ")).");
                        tptpStr = SUMOformulaToTPTPformula.getLang() + "(kb_" + kb.name + "_UserAssertion" +
                                "_" + axiomIndex + ",axiom,(" + theTPTPFormula + ")).";
                    }
                    pw.flush();
                }
            }
        }
        catch (IOException e) {
            e.printStackTrace();
            if (this.sessionId != null && !this.sessionId.isEmpty()) {
                if (debug>1) System.out.println("INFO Vampire.run(): using session dir for temp files, sessionId=" + sessionId);
            }
        }
        return allAdded;
    }

    /*****************************************************************
     */
    @Override
    public String toString() {

        StringBuilder sb = new StringBuilder();
        for (String s : output)
            sb.append(s).append("\n");
        return sb.toString();
    }

    /*****************************************************************
     */
    public static void printHelp() {

        System.out.println();
        System.out.println("Vampire class");
        System.out.println("  options:");
        System.out.println("  -h - show this help screen");
        System.out.println("  -p - run a test and process the proof");
        System.out.println("  -t - execute a test");
        System.out.println("  --ask <SuoKif-Query> <fof or tff> <timeoutInSec> <maxAnswers> - ask query");
        System.out.println();
    }

    /***************************************************************
     */
    public static void main (String[] args) throws Exception {

        System.out.println("INFO in Vampire.main()");
        Map<String, List<String>> argMap = CLIMapParser.parse(args);
        if (argMap.isEmpty() || argMap.containsKey("h")) {
            printHelp();
            return;
        }
        KBmanager.getMgr().initializeOnce();
        String kbName = KBmanager.getMgr().getPref("sumokbname");
        KB kb = KBmanager.getMgr().getKB(kbName);
        String dir = KBmanager.getMgr().getPref("kbDir") + File.separator;
        String lang = "tff";
        if (SUMOKBtoTPTPKB.getLang().equalsIgnoreCase("fof"))
            lang = "tptp";
        Vampire vampire = new Vampire(kb, lang, "CASC", false, 30, 1);
        File kbFile = new File(vampire.inferenceFilePath);
        if (!kbFile.exists()) {
            System.err.println("Error in Vampire.main(): no KB file: " + kbFile);
            return;
        }
        vampire.mode = Vampire.ModeType.CASC; // default
        TPTP3ProofProcessor tpp = new TPTP3ProofProcessor();
        if (argMap.containsKey("t")) {
            String outfile = dir + "temp-comb." + lang;
            String stmtFile = dir + "temp-stmt." + lang;
            File f1 = new File(outfile);
            f1.delete();
            File f2 = new File(stmtFile);
            f2.delete();
            File f3 = new File(dir + kbName + KB._userAssertionsString);
            f3.delete();
            File f4 = new File(dir + kbName + KB._userAssertionsTPTP);
            f4.delete();
            System.out.println("Vampire.main(): first test");
            Set<String> query = new HashSet<>();
            query.add("tff(conj1,conjecture,?[V__X, V__Y] : (s__subclass(V__X,V__Y))).");
            System.out.println("Vampire.main(): calling Vampire with: " + kbFile + ", 30, " + query);
            vampire.run(kbFile, query);
            System.out.println("----------------\nVampire output\n");
            for (String l : vampire.output)
                System.out.println(l);
            String queryStr = "(subclass ?X ?Y)";
            tpp.parseProofOutput(vampire.output,queryStr,kb,vampire.qlist);
            System.out.println("Vampire.main(): bindings: " + tpp.bindings);
            System.out.println("Vampire.main(): proof: " + tpp.proof);
            System.out.println("-----------------\n");
            System.out.println();
            System.out.println("Vampire.main(): second test");
            vampire.askVampire("(subclass ?X Entity)");
            System.out.println(vampire.toString());
        }
        if (argMap.containsKey("p")) {
            vampire.run(kbFile);
            String query = "(maximumPayloadCapacity ?X (MeasureFn ?Y ?Z))";
            StringBuilder answerVars = new StringBuilder("?X ?Y ?Z");
            System.out.println("input: " + vampire.output + "\n");
            tpp.parseProofOutput(vampire.output, query, kb, answerVars);
            tpp.createProofDotGraph();
            System.out.println("Vampire.main(): " + tpp.proof.size() + " steps ");
            System.out.println("Vampire.main() bindings: " + tpp.bindingMap);
            System.out.println("Vampire.main() skolems: " + tpp.skolemTypes);
            System.out.println("Vampire.main() proof[3]: {");
            tpp.printProof(3);
            System.out.println("}");
        }
    }
}