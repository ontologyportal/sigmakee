package com.articulate.sigma.jobscheduler;

import com.articulate.sigma.Formula;
import com.articulate.sigma.KB;
import com.articulate.sigma.KBmanager;
import com.articulate.sigma.tp.SInEContradictionAuditor;
import com.articulate.sigma.tp.SInEContradictionAuditor.Attempt;
import com.articulate.sigma.tp.SInEContradictionAuditor.Options;
import com.articulate.sigma.tp.SInEContradictionAuditor.Result;
import com.articulate.sigma.tp.SZSStatus;
import com.articulate.sigma.user.EmailService;
import java.time.Duration;
import java.time.Instant;

/** Scheduled randomized SInE contradiction audit. */
public class SInEContradictionJob extends Job {

    private final String kbName;
    private final int attempts;
    private final int scope;
    private final int vampireTimeout;
    private final int maxAxioms;

    public SInEContradictionJob(String id, String kbName, Schedule schedule) {

        this(id, kbName, schedule, 1000, 2, 10, 1000);
    }

    public SInEContradictionJob(String id, String kbName, Schedule schedule, int attempts, int scope, int vampireTimeout, int maxAxioms) {

        super(id, "SInE Contradiction Audit", "Search " + kbName + " for contradictions using randomized SInE neighborhoods and Vampire", schedule);
        this.kbName = kbName;
        this.attempts = attempts;
        this.scope = scope;
        this.vampireTimeout = vampireTimeout;
        this.maxAxioms = maxAxioms;
    }

    /** Runs the audit and emails its result. */
    @Override
    public void run() throws Exception {

        Instant started = Instant.now();
        KBmanager.getMgr().initializeOnce();
        KB kb = KBmanager.getMgr().getKB(kbName);
        if (kb == null) throw new IllegalStateException("Knowledge base not found: " + kbName);
        Result result = new SInEContradictionAuditor().audit(kb, new Options(attempts, 0.001, scope, vampireTimeout, 2, maxAxioms, System.nanoTime()));
        boolean emailed = new EmailService().sendAdminNotification(subject(result), report(result, started));
        if (!emailed) System.err.println("SInEContradictionJob: unable to email report");
    }

    private String subject(Result result) {

        return "[SigmaKEE " + kbName + "] " + (result.foundContradiction() ? "SInE CONTRADICTION FOUND" : "SInE audit completed");
    }

    private String report(Result result, Instant started) {

        long timeouts = result.attempts().stream().filter(a -> a.result() != null && a.result().getSzsStatus() == SZSStatus.TIMEOUT).count();
        StringBuilder report = new StringBuilder("<html><body><h2>SInE contradiction audit</h2><ul>");
        report.append("<li><b>Knowledge base:</b> ").append(escape(kbName)).append("</li>");
        report.append("<li><b>Outcome:</b> ").append(result.foundContradiction() ? "Contradiction found" : "No contradiction found within configured limits").append("</li>");
        report.append("<li><b>Seed:</b> ").append(result.seed()).append("</li>");
        report.append("<li><b>Vampire attempts:</b> ").append(result.attempts().size()).append("</li>");
        report.append("<li><b>Timeouts:</b> ").append(timeouts).append("</li>");
        report.append("<li><b>Artifacts:</b> ").append(escape(result.runDirectory().toString())).append("</li>");
        report.append("<li><b>Elapsed:</b> ").append(Duration.between(started, Instant.now())).append("</li></ul>");
        if (result.foundContradiction()) {
            Attempt attempt = result.contradiction();
            report.append("<h3>Contradictory source axioms</h3><p>Sample ").append(attempt.number()).append(" contained ").append(attempt.axiomCount()).append(" translated axioms.</p><ul>");
            for (Formula formula : result.sourceAxioms()) report.append("<li>").append(escape(formula.sourceFile)).append(":").append(formula.startLine).append(" ").append(escape(formula.getFormula())).append("</li>");
            report.append("</ul>");
        }
        return report.append("<p>This is a randomized heuristic search. A completed run does not establish consistency.</p></body></html>").toString();
    }

    private static String escape(String value) {

        if (value == null) return "";
        return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;").replace("'", "&#39;");
    }

    @Override
    public String getType() {

        return "sine-contradiction";
    }

    public String getKbName() {

        return kbName;
    }

    public int getAttempts() {

        return attempts;
    }

    public int getScope() {

        return scope;
    }

    public int getVampireTimeout() {

        return vampireTimeout;
    }

    public int getMaxAxioms() {

        return maxAxioms;
    }
}
