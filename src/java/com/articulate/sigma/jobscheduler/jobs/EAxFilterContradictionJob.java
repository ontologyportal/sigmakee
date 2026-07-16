package com.articulate.sigma.jobscheduler;

import com.articulate.sigma.KB;
import com.articulate.sigma.KBmanager;
import com.articulate.sigma.tp.SZSStatus;
import com.articulate.sigma.tp.TheoremProverController;
import com.articulate.sigma.tp.TheoremProverController.EAxFilterVampireResult;
import com.articulate.sigma.tp.TheoremProverController.FilteredVampireAttempt;
import com.articulate.sigma.tp.e.EAxFilter;
import com.articulate.sigma.user.EmailService;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;

public class EAxFilterContradictionJob extends Job {

    /**  */
    private final String kbName;
    /**  */
    private final int cnfTimeout;
    /**  */
    private final int filterTimeout;
    /**  */
    private final int vampireTimeout;

    /********************************************************************
     * 
     */
    public EAxFilterContradictionJob(String id, String kbName, Schedule schedule) {
        this(id, kbName, schedule, 300, 300, 30);
    }

    /********************************************************************
     * 
     */ 
    public EAxFilterContradictionJob(String id, String kbName, Schedule schedule, int cnfTimeout, int filterTimeout, int vampireTimeout) {
        super(id, "EAxFilter Contradiction Search", "Search " + kbName + " for contradictions using E axiom filtering and Vampire", schedule);
        this.kbName = kbName;
        this.cnfTimeout = cnfTimeout;
        this.filterTimeout = filterTimeout;
        this.vampireTimeout = vampireTimeout;
    }

    /********************************************************************
     * 
     */
    @Override
    public void run() throws Exception {
        Instant startedAt = Instant.now();
        Path inputProblem = Path.of(KBmanager.configuration.getKbDir(), kbName + ".tptp");
        try {
            KBmanager.getMgr().initializeOnce();
            KB kb = KBmanager.getMgr().getKB(kbName);
            if (kb == null) throw new IllegalStateException("Knowledge base not found: " + kbName);
            EAxFilterVampireResult result = new TheoremProverController().runEAxFilterWithVampire(kb, inputProblem, EAxFilter.EAxFilterOptions.forContradictions(), cnfTimeout, filterTimeout, vampireTimeout, "CASC");
            boolean emailed = new EmailService().sendAdminNotification(buildSubject(result), buildReport(result, inputProblem, startedAt));
            if (!emailed) System.err.println("EAxFilterContradictionJob: unable to email report");
        }
        catch (Exception exception) {
            boolean emailed = new EmailService().sendAdminNotification("[SigmaKEE " + kbName + "] EAxFilter search failed", buildFailureReport(inputProblem, startedAt, exception));
            if (!emailed) System.err.println("EAxFilterContradictionJob: unable to email failure report");
            if (exception instanceof InterruptedException) Thread.currentThread().interrupt();
            throw exception;
        }
    }

    /********************************************************************
     * 
     */
    private String buildSubject(EAxFilterVampireResult result) {
        return "[SigmaKEE " + kbName + "] " + (result.foundContradiction() ? "EAxFilter CONTRADICTION FOUND" : "EAxFilter search completed");
    }

    private String buildReport(EAxFilterVampireResult result, Path inputProblem, Instant startedAt) {
        long satisfiable = countStatus(result, SZSStatus.SATISFIABLE);
        long timeouts = countStatus(result, SZSStatus.TIMEOUT);
        String contradictorySubset = result.successfulAttempt() == null ? "None" : escapeHtml(result.successfulAttempt().problemFile().toString());
        return "<html><body><h2>EAxFilter contradiction search</h2><ul>" +
            "<li><b>Knowledge base:</b> " + escapeHtml(kbName) + "</li>" +
            "<li><b>Outcome:</b> " + (result.foundContradiction() ? "Contradiction found" : "No contradiction found within configured limits") + "</li>" +
            "<li><b>Input:</b> " + escapeHtml(inputProblem.toString()) + "</li>" +
            "<li><b>CNF clauses:</b> " + result.cnfResult().clauseCount() + "</li>" +
            "<li><b>Filtered problems generated:</b> " + result.filterResult().generatedProblems().size() + "</li>" +
            "<li><b>Vampire attempts:</b> " + result.attempts().size() + "</li>" +
            "<li><b>Satisfiable subsets:</b> " + satisfiable + "</li>" +
            "<li><b>Timeouts:</b> " + timeouts + "</li>" +
            "<li><b>Contradictory subset:</b> " + contradictorySubset + "</li>" +
            "<li><b>Elapsed:</b> " + formatDuration(Duration.between(startedAt, Instant.now())) + "</li>" +
            "</ul><p>This is a heuristic filtered search. No contradiction found does not establish global consistency.</p></body></html>";
    }

    /********************************************************************
     * 
     */
    private String buildFailureReport(Path inputProblem, Instant startedAt, Exception exception) {
        return "<html><body><h2>EAxFilter contradiction search failed</h2><ul>" +
            "<li><b>Knowledge base:</b> " + escapeHtml(kbName) + "</li>" +
            "<li><b>Input:</b> " + escapeHtml(inputProblem.toString()) + "</li>" +
            "<li><b>Elapsed:</b> " + formatDuration(Duration.between(startedAt, Instant.now())) + "</li>" +
            "<li><b>Exception:</b> " + escapeHtml(exception.getClass().getName()) + "</li>" +
            "<li><b>Message:</b> " + escapeHtml(String.valueOf(exception.getMessage())) + "</li>" +
            "</ul><p>Consult the SigmaKEE log for the complete stack trace.</p></body></html>";
    }

    /********************************************************************
     * 
     */
    private long countStatus(EAxFilterVampireResult result, SZSStatus status) {
        return result.attempts().stream().map(FilteredVampireAttempt::result).filter(r -> r != null && r.getSzsStatus() == status).count();
    }

    /********************************************************************
     * 
     */
    private static String formatDuration(Duration duration) {
        long seconds = duration.getSeconds();
        return String.format("%dh %dm %ds", seconds / 3600, seconds % 3600 / 60, seconds % 60);
    }

    /********************************************************************
     * 
     */
    private static String escapeHtml(String value) {
        if (value == null) return "";
        return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;").replace("'", "&#39;");
    }

    /********************************************************************
     * 
     */
    @Override
    public String getType() {
        return "eaxfilter-contradiction";
    }

    /******************************************************************** 
     */
    public String getKbName() { return kbName; }
    
    /******************************************************************** 
     */
    public int getCnfTimeout() { return cnfTimeout; }
    
    /******************************************************************** 
     */
    public int getFilterTimeout() { return filterTimeout; }

    /******************************************************************** 
     */
    public int getVampireTimeout() { return vampireTimeout; }
}