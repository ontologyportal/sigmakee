package com.articulate.sigma;

import java.io.BufferedReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import com.articulate.sigma.parsing.Expr;
import com.articulate.sigma.tp.ATPResult;
import com.articulate.sigma.tp.TheoremProverController;
import com.articulate.sigma.trans.SessionTPTPManager;

public class ConsistencyCheck implements Runnable {

    /****************************************************************
     * Result from running a prover process.
     */
    private static class ProverProcessResult {

        private final List<String> command;
        private final List<String> output;
        private final int exitCode;

        /****************************************************************
         * Creates a prover process result.
         * @param command command that was run
         * @param output combined stdout/stderr output
         * @param exitCode process exit code
         */
        private ProverProcessResult(List<String> command, List<String> output, int exitCode) {
            this.command = command;
            this.output = output;
            this.exitCode = exitCode;
        }
    }

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
    private final ConsistencyCheckManager.ConsistencyCheckMode checkMode;
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
     * @param checkMode consistency-check mode
     */
    public ConsistencyCheck(KB kb, String fileName, String userSessionId,
                            String proverType, String tptpLanguage, String vampireMode,
                            boolean closedWorldAssumption, boolean modusPonens,
                            boolean dropOnePremise, boolean holUseModals,
                            int timeout, int maxAnswers, ConsistencyCheckManager.ConsistencyCheckMode checkMode) {

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
        this.checkMode = checkMode == null
            ? ConsistencyCheckManager.ConsistencyCheckMode.GLOBAL
            : checkMode;
    }

    /****************************************************************
     * Runs the consistency check.
     */
    @Override
    public void run() {

        if (checkMode == ConsistencyCheckManager.ConsistencyCheckMode.INCREMENTAL) runIncrementalConsistencyCheck();
        else runGlobalConsistencyCheck();
    }

    /****************************************************************
     * Runs a global consistency check by sending the whole generated KB
     * to Vampire as axioms only, with no conjecture.
     */
    private void runGlobalConsistencyCheck() {

        try (FileWriter fw = new FileWriter(fileName);
            PrintWriter pw = new PrintWriter(fw)) {
            String fileLang = getFileLang();
            Path tptpFile = SessionTPTPManager.generateSessionTPTP(ccheckSessionId, kb, fileLang);
            if (tptpFile == null)
                throw new RuntimeException("Could not generate session inference file for " + kb.name);
            Path savedTptpFile = Path.of(
                    KBmanager.configuration.getBaseDir(),
                    "CCHECK_GLOBAL_" + kb.name + "." + fileLang);
            Files.copy(tptpFile, savedTptpFile, StandardCopyOption.REPLACE_EXISTING);
            writeXmlHeader(pw, "GLOBAL");
            if (!"VAMPIRE".equalsIgnoreCase(proverType)) {
                writeGlobalEntry(
                        pw,
                        "Error from Inference Engine",
                        "Global consistency check currently supports Vampire only.",
                        Collections.emptyList(),
                        -1,
                        savedTptpFile);
            }
            else {
                ProverProcessResult result = runVampireOnAxiomsOnly(tptpFile);
                String type = classifyGlobalResult(result);
                writeGlobalEntry(
                        pw,
                        type,
                        "Whole-KB axioms-only consistency check.",
                        result.output,
                        result.exitCode,
                        savedTptpFile);
            }
            writeXmlFooter(pw);
        }
        catch (Exception e) {
            System.err.println("Error in ConsistencyCheck.runGlobalConsistencyCheck(): " + e.getMessage());
            e.printStackTrace();
            try (FileWriter fw = new FileWriter(fileName, true);
                PrintWriter pw = new PrintWriter(fw)) {
                pw.println("  <error>");
                pw.println("    " + xml(e.getMessage()));
                pw.println("  </error>");
                pw.println("</ConsistencyCheck>");
            }
            catch (IOException ioe) {
                ioe.printStackTrace();
            }
        }
        finally {
            SessionTPTPManager.cleanupSession(ccheckSessionId);
        }
    }

    private String classifyGlobalResult(ProverProcessResult result) {

        String output = String.join("\n", result.output);
        if (output.contains("SZS status Unsatisfiable"))
            return "Inconsistency";
        if (output.contains("SZS status Satisfiable") ||
                output.contains("SZS status CounterSatisfiable"))
            return "No inconsistency found";
        if (output.contains("SZS status Timeout") ||
                output.contains("Proof not found in time") ||
                output.contains("Termination reason: Time limit"))
            return "Timeout";
        if (output.contains("SZS status GaveUp") ||
                output.contains("SZS status Unknown") ||
                output.contains("Refutation not found, incomplete strategy"))
            return "Unknown";
        if (result.exitCode != 0)
            return "Error from Inference Engine";
        return "Unknown";
    }

    /****************************************************************
     * Checks each formula against a temporary KB containing only the formulas
     * that have already been accepted.
     */
    private void runIncrementalConsistencyCheck() {

        KB workingKb = null;
        String workingKbName = "CCheck_" + kb.name;
        try (FileWriter fw = new FileWriter(fileName);
            PrintWriter pw = new PrintWriter(fw)) {
            workingKb = Diagnostics.makeEmptyKB(workingKbName);
            workingKb.kbCache = new KBcache(kb.kbCache, workingKb);
            String fileLang = "tptp";
            if ("TFF".equals(tptpLanguage)) fileLang = "tff";
            else if ("THF".equals(tptpLanguage)) fileLang = "thf";
            Path initialSessionFile = SessionTPTPManager.generateSessionTPTP(ccheckSessionId, workingKb, fileLang);
            if (initialSessionFile == null) throw new RuntimeException("Could not generate session inference file for " + workingKb.name);
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
                        if (sourceFile == null) sourceFile = "";
                        sourceFile = sourceFile.replace("/", "&#47;");
                        try {
                            ATPResult redundancy = theoremProverController.runQuery(workingKb, ccheckSessionId, processedQuery, null, "CUSTOM", proverType, tptpLanguage, vampireMode, closedWorldAssumption, modusPonens,dropOnePremise, holUseModals, timeout, maxAnswers);
                            if (redundancy != null) redundancy.parseProof(workingKb, processedQuery);
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
                                    for (String line : redundancy.getStdout()) pw.println("        " + line);
                                }
                                if (redundancy.getStderr() != null && !redundancy.getStderr().isEmpty()) {
                                    pw.println("        STDERR:");
                                    for (String line : redundancy.getStderr()) pw.println("        " + line);
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
                                if (redundancy.getPrimaryError() != null) pw.println("        " + redundancy.getPrimaryError());
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
                            ATPResult inconsistency = theoremProverController.runQuery(workingKb, ccheckSessionId, negatedQuery, null, "CUSTOM", proverType, tptpLanguage, vampireMode, closedWorldAssumption, modusPonens, dropOnePremise, holUseModals, timeout, maxAnswers);
                            if (inconsistency != null) inconsistency.parseProof(workingKb, negatedQuery);
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
                                    for (String line : inconsistency.getStdout()) pw.println("        " + line);
                                }
                                if (inconsistency.getStderr() != null && !inconsistency.getStderr().isEmpty()) {
                                    pw.println("        STDERR:");
                                    for (String line : inconsistency.getStderr()) pw.println("        " + line);
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
                                if (inconsistency.getPrimaryError() != null) pw.println("        " + inconsistency.getPrimaryError());
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

    /****************************************************************
     * Returns the generated TPTP file language suffix.
     * @return tptp, tff, or thf
     */
    private String getFileLang() {

        if ("TFF".equalsIgnoreCase(tptpLanguage))
            return "tff";

        if ("THF".equalsIgnoreCase(tptpLanguage))
            return "thf";

        return "tptp";
    }

    /****************************************************************
     * Runs Vampire directly on an axioms-only TPTP file.
     * @param tptpFile generated TPTP file
     * @return process result
     * @throws IOException if the process cannot be started
     * @throws InterruptedException if interrupted while waiting
     */
    private ProverProcessResult runVampireOnAxiomsOnly(Path tptpFile)
            throws IOException, InterruptedException {

        String vampireExec = KBmanager.configuration.getVampireExec();
        if (vampireExec == null || vampireExec.isBlank())
            throw new IOException("No Vampire executable configured.");

        List<String> command = new ArrayList<>();
        command.add(vampireExec);
        command.add("--output_axiom_names");
        command.add("on");
        command.add("--proof");
        command.add("tptp");
        command.add("--mode");
        command.add(getVampireModeArg());
        command.add("-t");
        command.add(Integer.toString(timeout));
        command.add(tptpFile.toString());

        ProcessBuilder pb = new ProcessBuilder(command);
        pb.redirectErrorStream(true);

        Process process = pb.start();
        List<String> output = Collections.synchronizedList(new ArrayList<>());

        Thread reader = new Thread(() -> {
            try (BufferedReader br = new BufferedReader(
                    new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {

                String line;
                while ((line = br.readLine()) != null)
                    output.add(line);
            }
            catch (IOException ioe) {
                output.add("Error reading Vampire output: " + ioe.getMessage());
            }
        });

        reader.start();

        boolean finished = process.waitFor(timeout + 20L, TimeUnit.SECONDS);

        int exitCode;
        if (finished) {
            exitCode = process.exitValue();
        }
        else {
            process.destroyForcibly();
            exitCode = 124;
            output.add("Vampire process killed after exceeding wall-clock guard timeout.");
        }

        reader.join(5000L);

        return new ProverProcessResult(command, output, exitCode);
    }

    /****************************************************************
     * Returns the Vampire mode argument.
     * @return Vampire mode argument
     */
    private String getVampireModeArg() {

        if ("AVATAR".equalsIgnoreCase(vampireMode)) return "avatar";
        return "casc";
    }

    /****************************************************************
     * Writes the consistency-check XML header.
     * @param pw writer
     * @param mode check mode
     */
    private void writeXmlHeader(PrintWriter pw, String mode) {

        pw.println("<ConsistencyCheck>");
        pw.println("  <kb>");
        pw.println("    " + xml(kb.name));
        pw.println("  </kb>");
        pw.println("  <mode>");
        pw.println("    " + xml(mode));
        pw.println("  </mode>");
        pw.println("  <entries>");
    }

    /****************************************************************
     * Writes the consistency-check XML footer.
     * @param pw writer
     */
    private void writeXmlFooter(PrintWriter pw) {

        pw.println("  </entries>");
        pw.println("</ConsistencyCheck>");
    }

    /****************************************************************
     * Writes one global consistency-check entry.
     * @param pw writer
     * @param type result type
     * @param message result message
     * @param output prover output
     * @param exitCode process exit code
     * @param tptpFile generated TPTP file
     */
    private void writeGlobalEntry(PrintWriter pw,
                                String type,
                                String message,
                                List<String> output,
                                int exitCode,
                                Path tptpFile) {

        pw.println("    <entry>");
        pw.println("      <query>");
        pw.println("        Global consistency check: axioms only, no conjecture");
        pw.println("      </query>");
        pw.println("      <processedStatement>");
        pw.println("        " + xml(tptpFile.toString()));
        pw.println("      </processedStatement>");
        pw.println("      <sourceFile>");
        pw.println("        " + xml(tptpFile.toString()));
        pw.println("      </sourceFile>");
        pw.println("      <type>");
        pw.println("        " + xml(type));
        pw.println("      </type>");
        pw.println("      <proof src=\"" + xml(proverType) + "\">");
        pw.println("        " + xml(message));
        pw.println("        Exit code: " + exitCode);
        for (String line : output)
            pw.println("        " + xml(line));
        pw.println("      </proof>");
        pw.println("    </entry>");
    }

    /****************************************************************
     * Escapes text for XML output.
     * @param value raw text
     * @return escaped text
     */
    private String xml(String value) {

        if (value == null) return "";
        return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&apos;");
    }
}