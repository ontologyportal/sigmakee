package com.articulate.sigma.tp;

import com.articulate.sigma.KB;
import com.articulate.sigma.KBmanager;
import org.json.simple.JSONObject;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;

/** Serves sigma-rs consistency-audit requests and status results. */
@WebServlet("/SigmaRsAuditServlet")
public final class SigmaRsAuditServlet extends HttpServlet {

    private SigmaRsAuditManager manager;

    /***************************************************************
     * Initializes the application-scoped audit manager.
     */
    @Override
    public void init() {

        synchronized (getServletContext()) {
            manager = (SigmaRsAuditManager) getServletContext().getAttribute("sigmaRsAuditManager");
            if (manager == null) {
                Path executable = Path.of("/home/shaun/workspace/sigma-rs/target/release/sumo");
                manager = new SigmaRsAuditManager(executable);
                getServletContext().setAttribute("sigmaRsAuditManager", manager);
            }
        }
    }

    /***************************************************************
     * Writes a UTF-8 JSON response.
     * @param response servlet response
     * @param status HTTP status code
     * @param json JSON response body
     * @throws IOException if the response cannot be written
     */
    private static void writeJson(HttpServletResponse response, int status, String json) throws IOException {

        response.setStatus(status);
        response.setContentType("application/json");
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getWriter().write(json);
    }

    /***************************************************************
     * Starts an audit for the requested knowledge base.
     * @param request servlet request containing the audit options
     * @param response servlet response receiving the audit state
     * @throws IOException if the response cannot be written
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {

        // Apply the same authorization check used by Diagnostics.jsp.
        String kbName = request.getParameter("kb");
        KB kb = KBmanager.getMgr().getKB(kbName);
        if (kb == null) {
            writeJson(response, 404, "{\"success\":false,\"message\":\"Unknown KB\"}");
            return;
        }
        int timeout = parseInt(request, "timeout", 60, 1, 3600);
        int limit = parseInt(request, "limit", 64, 1, 1000);
        double thoroughness = parseDouble(request, "thoroughness", 1.0, 0.0001, 1.0);
        double scope = parseDouble(request, "scope", 2.0, 1.0, 100.0);
        boolean started = manager.start(kb, new SigmaRsAuditRunner.Options(timeout, limit, thoroughness, scope));
        if (started) writeJson(response, 202, "{\"success\":true,\"state\":\"RUNNING\"}");
        else writeJson(response, 409, "{\"success\":false,\"message\":\"Audit already running\"}");
    }

    /***************************************************************
     * Returns the audit status for the requested knowledge base.
     * @param request servlet request containing the knowledge-base name
     * @param response servlet response receiving the audit status
     * @throws IOException if the response cannot be written
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {

        String kbName = request.getParameter("kb");
        JSONObject json = new JSONObject();
        if (kbName == null || kbName.isBlank()) {
            json.put("success", false);
            json.put("state", "FAILED");
            json.put("message", "Missing KB name");
            writeJson(response, 400, json.toJSONString());
            return;
        }
        SigmaRsAuditManager.Status status = manager.status(kbName);
        json.put("success", true);
        json.put("state", status.state().name());
        if (status.startedAt() != null) json.put("startedAt", status.startedAt().toString());
        if (status.completedAt() != null) json.put("completedAt", status.completedAt().toString());
        if (status.error() != null) json.put("error", status.error());
        SigmaRsAuditRunner.Result result = status.result();
        if (result != null) {
            json.put("exitCode", result.exitCode());
            json.put("timedOut", result.timedOut());
            json.put("report", result.report());
            json.put("diagnostics", result.diagnostics());
            json.put("command", result.command());
        }
        writeJson(response, 200, json.toJSONString());
    }

    /***************************************************************
     * Parses a bounded integer request parameter.
     * @param request servlet request containing the parameter
     * @param name parameter name
     * @param defaultValue value used when the parameter is absent
     * @param minimum minimum accepted value
     * @param maximum maximum accepted value
     * @return the parsed or default value
     */
    private static int parseInt(HttpServletRequest request, String name, int defaultValue, int minimum, int maximum) {

        String value = request.getParameter(name);
        if (value == null || value.isBlank()) return defaultValue;
        try {
            int parsed = Integer.parseInt(value);
            if (parsed < minimum || parsed > maximum) throw new IllegalArgumentException(name + " must be between " + minimum + " and " + maximum);
            return parsed;
        }
        catch (NumberFormatException exception) {
            throw new IllegalArgumentException(name + " must be an integer", exception);
        }
    }

    /***************************************************************
     * Parses a bounded decimal request parameter.
     * @param request servlet request containing the parameter
     * @param name parameter name
     * @param defaultValue value used when the parameter is absent
     * @param minimum minimum accepted value
     * @param maximum maximum accepted value
     * @return the parsed or default value
     */
    private static double parseDouble(HttpServletRequest request, String name, double defaultValue, double minimum, double maximum) {

        String value = request.getParameter(name);
        if (value == null || value.isBlank()) return defaultValue;
        try {
            double parsed = Double.parseDouble(value);
            if (!Double.isFinite(parsed) || parsed < minimum || parsed > maximum) throw new IllegalArgumentException(name + " must be between " + minimum + " and " + maximum);
            return parsed;
        }
        catch (NumberFormatException exception) {
            throw new IllegalArgumentException(name + " must be numeric", exception);
        }
    }
}
