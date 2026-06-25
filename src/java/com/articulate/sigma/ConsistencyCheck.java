package com.articulate.sigma;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Set;

import com.articulate.sigma.parsing.Expr;
import com.articulate.sigma.tp.ATPResult;
import com.articulate.sigma.tp.TheoremProverController;
import com.articulate.sigma.trans.SessionTPTPManager;

public class ConsistencyCheck implements Runnable {

    public final String kbName;

    private final KB kb;
    private final String fileName;
    private final String ccheckSessionId;
    private final String proverType;
    private final String tptpLanguage;
    private final String vampireMode;
    private final boolean closedWorldAssumption;
    private final boolean modusPonens;
    private final boolean dropOnePremise;
    private final boolean holUseModals;
    private final int timeout;
    private final int maxAnswers;
    private final TheoremProverController theoremProverController = new TheoremProverController();

    /****************************************************************
     * Creates a consistency check using TheoremProverController.runQuery().
     * @param kb KB to check
     * @param fileName output XML file
     * @param userSessionId source user session id
     * @param proverType prover selected by the UI
     * @param tptpLanguage TPTP-family language: FOF, TFF, or THF
     * @param vampireMode Vampire execution mode
     * @param closedWorldAssumption whether to use CWA
     * @param modusPonens whether to use modus ponens
     * @param dropOnePremise whether to drop one-premise formulas
     * @param holUseModals whether to use modal HOL translation
     * @param timeout timeout in seconds
     * @param maxAnswers maximum answers
     */
    public ConsistencyCheck(KB kb, String fileName, String userSessionId,
                            String proverType, String tptpLanguage, String vampireMode,
                            boolean closedWorldAssumption, boolean modusPonens,
                            boolean dropOnePremise, boolean holUseModals,
                            int timeout, int maxAnswers) {

        this.kb = kb;
        this.kbName = kb.name;
        this.fileName = fileName;
        String baseSessionId = userSessionId;
        if (baseSessionId == null || baseSessionId.isEmpty())
            baseSessionId = "nosession";
        this.ccheckSessionId =
                "ccheck_" +
                kb.name.replaceAll("\\W", "_") +
                "_" +
                baseSessionId.replaceAll("\\W", "_") +
                "_" +
                Long.toUnsignedString(System.nanoTime(), 36);
        this.proverType = proverType;
        this.tptpLanguage = tptpLanguage;
        this.vampireMode = vampireMode;
        this.closedWorldAssumption = closedWorldAssumption;
        this.modusPonens = modusPonens;
        this.dropOnePremise = dropOnePremise;
        this.holUseModals = holUseModals;
        this.timeout = timeout > 0 ? timeout : 30;
        this.maxAnswers = maxAnswers > 0 ? maxAnswers : 1;
    }

    /****************************************************************
     * Runs the consistency check.
     */
    @Override
    public void run() {

        runConsistencyCheck();
    }

    /****************************************************************
     * Checks each formula against a temporary KB containing only the formulas
     * that have already been accepted.
     */
    private void runConsistencyCheck() {

        KB workingKb = null;
        String workingKbName = "CCheck_" + kb.name;

        try (FileWriter fw = new FileWriter(fileName);
             PrintWriter pw = new PrintWriter(fw)) {

            workingKb = Diagnostics.makeEmptyKB(workingKbName);
            workingKb.kbCache = new KBcache(kb.kbCache, workingKb);

            String fileLang = "tptp";
            if ("TFF".equals(tptpLanguage))
                fileLang = "tff";
            else if ("THF".equals(tptpLanguage))
                fileLang = "thf";

            Path initialSessionFile =
                    SessionTPTPManager.generateSessionTPTP(ccheckSessionId, workingKb, fileLang);

            if (initialSessionFile == null)
                throw new RuntimeException("Could not generate session inference file for " + workingKb.name);

            pw.println("<ConsistencyCheck>");
            pw.println("  <kb>");
            pw.println("    " + kb.name);
            pw.println("  </kb>");
            pw.println("  <entries>");
            pw.flush();
            fw.flush();

            Collection<Formula> formulas = kb.formulaMap.values();

            for (Formula query : formulas) {

                FormulaPreprocessor fp = new FormulaPreprocessor();
                Set<Expr> processedQueries = fp.preProcessExpr(query, false, kb);

                if (processedQueries != null) {
                    for (Expr ex : processedQueries) {

                        Formula processedFormula = new Formula(ex.toKifString());
                        String processedQuery = processedFormula.makeQuantifiersExplicit(false);

                        String sourceFile = query.sourceFile;
                        if (sourceFile == null)
                            sourceFile = "";
                        sourceFile = sourceFile.replace("/", "&#47;");

                        try {
                            ATPResult redundancy = theoremProverController.runQuery(
                                    workingKb,
                                    ccheckSessionId,
                                    processedQuery,
                                    null,
                                    "CUSTOM",
                                    proverType,
                                    tptpLanguage,
                                    vampireMode,
                                    closedWorldAssumption,
                                    modusPonens,
                                    dropOnePremise,
                                    holUseModals,
                                    timeout,
                                    maxAnswers);

                            if (redundancy != null)
                                redundancy.parseProof(workingKb, processedQuery);

                            if (redundancy != null && redundancy.hasProof()) {
                                pw.println("    <entry>");
                                pw.println("      <query>");
                                pw.println("        " + query.getFormula());
                                pw.println("      </query>");
                                pw.println("      <processedStatement>");
                                pw.println("        " + processedQuery);
                                pw.println("      </processedStatement>");
                                pw.println("      <sourceFile>");
                                pw.println("        " + sourceFile);
                                pw.println("      </sourceFile>");
                                pw.println("      <type>");
                                pw.println("        Redundancy");
                                pw.println("      </type>");
                                pw.println("      <proof src=\"" + proverType + "\">");
                                pw.println("        " + redundancy.getSummary());
                                if (redundancy.getStdout() != null) {
                                    for (String line : redundancy.getStdout())
                                        pw.println("        " + line);
                                }
                                if (redundancy.getStderr() != null && !redundancy.getStderr().isEmpty()) {
                                    pw.println("        STDERR:");
                                    for (String line : redundancy.getStderr())
                                        pw.println("        " + line);
                                }
                                pw.println("      </proof>");
                                pw.println("    </entry>");
                            }
                            else if (redundancy != null && redundancy.hasErrors()) {
                                pw.println("    <entry>");
                                pw.println("      <query>");
                                pw.println("        " + query.getFormula());
                                pw.println("      </query>");
                                pw.println("      <processedStatement>");
                                pw.println("        " + processedQuery);
                                pw.println("      </processedStatement>");
                                pw.println("      <sourceFile>");
                                pw.println("        " + sourceFile);
                                pw.println("      </sourceFile>");
                                pw.println("      <type>");
                                pw.println("        Error from Inference Engine");
                                pw.println("      </type>");
                                pw.println("      <proof src=\"" + proverType + "\">");
                                pw.println("        " + redundancy.getSummary());
                                if (redundancy.getPrimaryError() != null)
                                    pw.println("        " + redundancy.getPrimaryError());
                                pw.println("      </proof>");
                                pw.println("    </entry>");
                            }
                        }
                        catch (Exception e) {
                            pw.println("    <entry>");
                            pw.println("      <query>");
                            pw.println("        " + query.getFormula());
                            pw.println("      </query>");
                            pw.println("      <processedStatement>");
                            pw.println("        " + processedQuery);
                            pw.println("      </processedStatement>");
                            pw.println("      <sourceFile>");
                            pw.println("        " + sourceFile);
                            pw.println("      </sourceFile>");
                            pw.println("      <type>");
                            pw.println("        Error from Inference Engine");
                            pw.println("      </type>");
                            pw.println("      <proof src=\"" + proverType + "\">");
                            pw.println("        " + e.getMessage());
                            pw.println("      </proof>");
                            pw.println("    </entry>");
                        }

                        String negatedQuery = "(not " + processedQuery + ")";

                        try {
                            ATPResult inconsistency = theoremProverController.runQuery(
                                    workingKb,
                                    ccheckSessionId,
                                    negatedQuery,
                                    null,
                                    "CUSTOM",
                                    proverType,
                                    tptpLanguage,
                                    vampireMode,
                                    closedWorldAssumption,
                                    modusPonens,
                                    dropOnePremise,
                                    holUseModals,
                                    timeout,
                                    maxAnswers);

                            if (inconsistency != null)
                                inconsistency.parseProof(workingKb, negatedQuery);

                            if (inconsistency != null && inconsistency.hasProof()) {
                                pw.println("    <entry>");
                                pw.println("      <query>");
                                pw.println("        " + query.getFormula());
                                pw.println("      </query>");
                                pw.println("      <processedStatement>");
                                pw.println("        " + processedQuery);
                                pw.println("      </processedStatement>");
                                pw.println("      <sourceFile>");
                                pw.println("        " + sourceFile);
                                pw.println("      </sourceFile>");
                                pw.println("      <type>");
                                pw.println("        Inconsistency");
                                pw.println("      </type>");
                                pw.println("      <proof src=\"" + proverType + "\">");
                                pw.println("        " + inconsistency.getSummary());
                                if (inconsistency.getStdout() != null) {
                                    for (String line : inconsistency.getStdout())
                                        pw.println("        " + line);
                                }
                                if (inconsistency.getStderr() != null && !inconsistency.getStderr().isEmpty()) {
                                    pw.println("        STDERR:");
                                    for (String line : inconsistency.getStderr())
                                        pw.println("        " + line);
                                }
                                pw.println("      </proof>");
                                pw.println("    </entry>");
                            }
                            else if (inconsistency != null && inconsistency.hasErrors()) {
                                pw.println("    <entry>");
                                pw.println("      <query>");
                                pw.println("        " + query.getFormula());
                                pw.println("      </query>");
                                pw.println("      <processedStatement>");
                                pw.println("        " + processedQuery);
                                pw.println("      </processedStatement>");
                                pw.println("      <sourceFile>");
                                pw.println("        " + sourceFile);
                                pw.println("      </sourceFile>");
                                pw.println("      <type>");
                                pw.println("        Error from Inference Engine");
                                pw.println("      </type>");
                                pw.println("      <proof src=\"" + proverType + "\">");
                                pw.println("        " + inconsistency.getSummary());
                                if (inconsistency.getPrimaryError() != null)
                                    pw.println("        " + inconsistency.getPrimaryError());
                                pw.println("      </proof>");
                                pw.println("    </entry>");
                            }
                        }
                        catch (Exception e) {
                            pw.println("    <entry>");
                            pw.println("      <query>");
                            pw.println("        " + query.getFormula());
                            pw.println("      </query>");
                            pw.println("      <processedStatement>");
                            pw.println("        " + processedQuery);
                            pw.println("      </processedStatement>");
                            pw.println("      <sourceFile>");
                            pw.println("        " + sourceFile);
                            pw.println("      </sourceFile>");
                            pw.println("      <type>");
                            pw.println("        Error from Inference Engine");
                            pw.println("      </type>");
                            pw.println("      <proof src=\"" + proverType + "\">");
                            pw.println("        " + e.getMessage());
                            pw.println("      </proof>");
                            pw.println("    </entry>");
                        }
                    }
                }

                workingKb.tell(query.getFormula());

                Path updatedSessionFile = SessionTPTPManager.applyIncrementalUpdate(
                        workingKb,
                        ccheckSessionId,
                        query,
                        fileLang);

                if (updatedSessionFile == null) {
                    Files.deleteIfExists(SessionTPTPManager.getSessionTPTPPath(
                            ccheckSessionId,
                            workingKb.name,
                            fileLang));

                    SessionTPTPManager.generateSessionTPTP(ccheckSessionId, workingKb, fileLang);
                }

                pw.flush();
                fw.flush();
            }

            pw.println("  </entries>");
            pw.println("</ConsistencyCheck>");
        }
        catch (Exception e) {
            System.err.println("Error in ConsistencyCheck.runConsistencyCheck(): " + e.getMessage());
            e.printStackTrace();

            try (FileWriter fw = new FileWriter(fileName, true);
                 PrintWriter pw = new PrintWriter(fw)) {
                pw.println("  <error>");
                pw.println("    " + e.getMessage());
                pw.println("  </error>");
                pw.println("</ConsistencyCheck>");
            }
            catch (IOException ioe) {
                ioe.printStackTrace();
            }
        }
        finally {
            SessionTPTPManager.cleanupSession(ccheckSessionId);

            if (workingKb != null)
                KBmanager.getMgr().removeKB(workingKb.name);
            else
                KBmanager.getMgr().removeKB(workingKbName);
        }
    }
}