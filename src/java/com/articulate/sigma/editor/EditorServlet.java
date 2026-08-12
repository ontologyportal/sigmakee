package com.articulate.sigma.editor;

import com.articulate.sigma.trans.SUMOformulaToTPTPformula;
import com.articulate.sigma.*;
import com.articulate.sigma.tp.ATPQuery;
import com.articulate.sigma.tp.ATPResult;
import com.articulate.sigma.tp.ProverCrashedException;
import com.articulate.sigma.tp.ProverTimeoutException;
import com.articulate.sigma.tp.TheoremProverController;
import com.articulate.sigma.parsing.ExprToTFF;

import javax.servlet.*;
import com.articulate.sigma.tp.InferenceTest;
import javax.servlet.http.*;
import javax.servlet.annotation.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ExecutionException;

/**
 * Unified Editor Servlet for both .kif and .tptp code/files.
 * Handles format, check, and user file operations (save/load/list/translate).
 */
@WebServlet("/EditorServlet")
@MultipartConfig(
        fileSizeThreshold = 1024,
        maxFileSize = 200 * 1024,
        maxRequestSize = 220 * 1024
)
public class EditorServlet extends HttpServlet {
    boolean debug = true;
    private static final Object KIF_CHECK_LOCK = new Object();
    private static final Object TRANSLATE_LOCK = new Object();
    private static final Set<String> TQ_META_PREDICATES = new HashSet<>(Arrays.asList(
        "note",
        "category",
        "file",
        "minLang",
        "regen",
        "time",
        "query",
        "answer",
        "closedWorldAssumption",
        "modusPonens",
        "dropOnePremise",
        "HOLUseModals",
        "holUseModals",
        "HolUseModals"
    ));

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException, ServletException {

        if (KBmanager.configuration.isAws()) {
            resp.setStatus(HttpServletResponse.SC_FORBIDDEN);
            resp.setContentType("application/json; charset=UTF-8");
            resp.getWriter().write("{\"success\":false,\"message\":\"Editor is disabled on this node.\"}");
            return;
        }
        String username = requireUser(req, resp);
        if (username == null) return;
        String mode = Optional.ofNullable(req.getParameter("mode")).orElse("").toLowerCase(Locale.ROOT);
        File userDir = getUserDir(username);
        switch (mode) {
            case "saveuserfile":
                handleSaveUserFile(req, resp, userDir);
                break;
            case "listuserfiles":
                handleListUserFiles(resp, userDir);
                break;
            case "loaduserfile":
                handleLoadUserFile(req, resp, userDir);
                break;
            case "runatp":
                handleRunAtp(req, resp);
                break;
            case "runtq":
                handleRunTq(req, resp);
                break;
            case "translatetotptp":
                handleTranslateToTPTP(req, resp);
                break;
            case "translatetotff":
                handleTranslateToTFF(req, resp);
                break;
            case "translatetothf":
                handleTranslateToTHF(req, resp);
                break;
            case "query":
                handleQuery(req, resp);
                break;
            case "format":
            case "check":
            default:
                handleFormatOrCheck(req, resp);
                break;
        }
    }

    private static String readUtf8(InputStream in) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buf = new byte[8192];
        int n;
        while ((n = in.read(buf)) != -1)
            out.write(buf, 0, n);
        return new String(out.toByteArray(), StandardCharsets.UTF_8);
    }

    private static String jsonEscape(String s) {
        if (s == null) return "";
        StringBuilder b = new StringBuilder((int) (s.length() * 1.1));
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '\\':
                    b.append("\\\\");
                    break;
                case '"':
                    b.append("\\\"");
                    break;
                case '\b':
                    b.append("\\b");
                    break;
                case '\f':
                    b.append("\\f");
                    break;
                case '\n':
                    b.append("\\n");
                    break;
                case '\r':
                    b.append("\\r");
                    break;
                case '\t':
                    b.append("\\t");
                    break;
                default:
                    if (c < 0x20) b.append(String.format("\\u%04x", (int) c));
                    else b.append(c);
            }
        }
        return b.toString();
    }

    private void writeJson(HttpServletResponse resp,
                           boolean success,
                           String message,
                           String tptp) throws IOException {
        
                            StringBuilder sb = new StringBuilder();
        sb.append("{\"success\":").append(success);
        if (message != null) sb.append(",\"message\":\"").append(escapeJson(message)).append("\"");
        if (tptp != null) sb.append(",\"tptp\":\"").append(escapeJson(tptp)).append("\"");
        sb.append("}");
        resp.getWriter().write(sb.toString());
    }

    private String escapeJson(String s) {
        if (s == null) return "";
        return s
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\r", "\\r")
                .replace("\n", "\\n");
    }

    private void writeTffJson(HttpServletResponse resp,
                          boolean success,
                          String message,
                          String tff) throws IOException {

        StringBuilder json = new StringBuilder();
        json.append("{\"success\":").append(success);

        if (message != null)
            json.append(",\"message\":\"").append(escapeJson(message)).append("\"");

        if (tff != null)
            json.append(",\"tff\":\"").append(escapeJson(tff)).append("\"");

        json.append("}");
        resp.getWriter().write(json.toString());
    }

    private void writeThfJson(HttpServletResponse resp,
                              boolean success,
                              String message,
                              String thf) throws IOException {

        StringBuilder json = new StringBuilder();
        json.append("{\"success\":").append(success);

        if (message != null)
            json.append(",\"message\":\"").append(escapeJson(message)).append("\"");

        if (thf != null)
            json.append(",\"thf\":\"").append(escapeJson(thf)).append("\"");

        json.append("}");
        resp.getWriter().write(json.toString());
    }

    /**
     * Check session + role. Returns username or null if already responded with error.
     */
    private String requireUser(HttpServletRequest req, HttpServletResponse resp) throws IOException {

        HttpSession session = req.getSession(false);
        String username = null;
        String role = null;
        if (session != null) {
            username = (String) session.getAttribute("username");
            if (username == null) username = (String) session.getAttribute("user");
            role = (String) session.getAttribute("role");
        }
        if (username == null || username.trim().isEmpty() || role == null || role.trim().isEmpty()) {
            resp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            resp.setContentType("application/json; charset=UTF-8");
            resp.getWriter().write("{\"success\":false,\"message\":\"Not logged in.\"}");
            return null;
        }
        if (!role.equalsIgnoreCase("user") && !role.equalsIgnoreCase("admin")) {
            resp.setStatus(HttpServletResponse.SC_FORBIDDEN);
            resp.setContentType("application/json; charset=UTF-8");
            resp.getWriter().write("{\"success\":false,\"message\":\"Access denied. Must be user or admin.\"}");
            return null;
        }
        return username;
    }

    private File getUserDir(String username) {
        File userDir = new File(System.getProperty("user.home") + "/.sigmakee/KBs/UserKBs/" + username);
        if (!userDir.exists()) userDir.mkdirs();
        return userDir;
    }

    // ============================================================
    // Handlers for each mode
    // ============================================================

    private void handleSaveUserFile(HttpServletRequest req, HttpServletResponse resp, File userDir)
            throws IOException {

        String filename = req.getParameter("fileName");
        String contents = req.getParameter("code");
        resp.setContentType("application/json; charset=UTF-8");
        if (filename == null || filename.isBlank()) {
            resp.getWriter().write("{\"success\":false,\"message\":\"Filename missing.\"}");
            return;
        }
        if (contents == null) contents = "";
        File outFile = new File(userDir, filename);
        try (FileWriter fw = new FileWriter(outFile)) {
            fw.write(contents);
        } catch (IOException e) {
            resp.getWriter().write("{\"success\":false,\"message\":\"" +
                    e.getMessage().replace("\"", "'") + "\"}");
            return;
        }
        resp.getWriter().write("{\"success\":true,\"message\":\"Saved successfully.\"}");
    }

    private void handleListUserFiles(HttpServletResponse resp, File userDir) throws IOException {
        List<String> fileNames = new ArrayList<>();
        if (userDir.exists() && userDir.isDirectory()) {
            File[] files = userDir.listFiles();
            if (files != null) {
                for (File f : files) {
                    if (f.isFile()) fileNames.add(f.getName());
                }
            }
        }
        resp.setContentType("application/json; charset=UTF-8");
        StringBuilder json = new StringBuilder("{\"success\":true,\"files\":[");
        for (int i = 0; i < fileNames.size(); i++) {
            json.append("\"").append(jsonEscape(fileNames.get(i))).append("\"");
            if (i < fileNames.size() - 1) json.append(",");
        }
        json.append("]}");
        resp.getWriter().write(json.toString());
    }

    private void handleLoadUserFile(HttpServletRequest req, HttpServletResponse resp, File userDir) throws IOException {

        String filename = req.getParameter("fileName");
        resp.setContentType("application/json; charset=UTF-8");
        if (filename == null || filename.isBlank()) {
            resp.getWriter().write("{\"success\":false,\"message\":\"Filename missing.\"}");
            return;
        }
        File target = new File(userDir, filename);
        if (!target.exists()) {
            resp.getWriter().write("{\"success\":false,\"message\":\"File not found.\"}");
            return;
        }
        String contents = new String(java.nio.file.Files.readAllBytes(target.toPath()), StandardCharsets.UTF_8);
        resp.getWriter().write("{\"success\":true,\"fileName\":\"" + jsonEscape(filename) +
                "\",\"contents\":\"" + jsonEscape(contents) + "\"}");
    }

    private void handleTranslateToTPTP(HttpServletRequest req, HttpServletResponse resp)
            throws IOException {
        
        resp.setContentType("application/json; charset=UTF-8");
        String fileName = Optional.ofNullable(req.getParameter("fileName")).orElse("buffer.kif");
        String code = req.getParameter("code");
        if (debug) System.out.println("editorServlet.handleTranslateToTPTP(): Entering function with following parameters... \n    File = " + fileName + "\n    Code = " + code);
        if (code == null || code.trim().isEmpty()) {
            writeJson(resp, false, "No KIF content received.", null);
            return;
        }
        try {
            String tptp = EditorWorkerQueue.submit(() -> {
                synchronized (TRANSLATE_LOCK) {
                    KBmanager.getMgr().initializeOnce();
                    SUMOformulaToTPTPformula.setLang("fof");
                    final String codeFinal = code;
                    final String fileNameFinal = fileName;
                    List<String> kifForms = splitKifFormulas(codeFinal);
                    if (kifForms.isEmpty())
                        return null;
                    String base = fileNameFinal.replaceAll("\\.[^.]+$", "");
                    StringBuilder out = new StringBuilder();
                    int idx = 1;
                    for (String kif : kifForms) {
                        String tptpBody = SUMOformulaToTPTPformula.tptpParseSUOKIFString(kif, false);
                        if (tptpBody == null || tptpBody.isBlank())
                            continue;
                        out.append("fof(")
                                .append((base.isEmpty() ? "buf" : base)).append("_").append(idx++)
                                .append(", axiom, ")
                                .append(tptpBody.trim())
                                .append(").\n");
                    }
                    return out.length() == 0 ? null : out.toString();
                }
            }, 20000); // 20s timeout for translation (tune)
            if (tptp == null) {
                writeJson(resp, false, "Translation produced no output.", null);
                return;
            }
            writeJson(resp, true, null, tptp);
        } catch (RejectedExecutionException rex) {
            resp.setStatus(429);
            writeJson(resp, false, "Server busy. Please retry.", null);
        } catch (TimeoutException tex) {
            resp.setStatus(503);
            writeJson(resp, false, "Translation timed out. Please retry.", null);
        } catch (Exception e) {
            e.printStackTrace();
            writeJson(resp, false, "Exception during translation: " + e.getMessage(), null);
        }
    }

    private void handleTranslateToTFF(HttpServletRequest req,
                                  HttpServletResponse resp) throws IOException {

        resp.setContentType("application/json; charset=UTF-8");

        String fileName = Optional.ofNullable(req.getParameter("fileName"))
                .orElse("buffer.kif");
        String code = Optional.ofNullable(req.getParameter("code")).orElse("");
        String kbName = Optional.ofNullable(req.getParameter("kb")).orElse("SUMO");

        if (code.trim().isEmpty()) {
            writeTffJson(resp, false, "No KIF content received.", null);
            return;
        }

        try {
            String result = EditorWorkerQueue.submit(() -> {
                synchronized (TRANSLATE_LOCK) {
                    KBmanager.getMgr().initializeOnce();

                    KB kb = KBmanager.getMgr().getKB(kbName);
                    if (kb == null)
                        throw new IllegalArgumentException(
                                "Knowledge base not found: " + kbName);

                    List<String> formulas = splitKifFormulas(code);
                    String base = fileName.replaceAll("\\.[^.]+$", "");
                    if (base.isEmpty()) base = "buf";

                    StringBuilder output = new StringBuilder();
                    int index = 1;

                    for (String kif : formulas) {
                        String body =
                                ExprToTFF.translateKifString(kif, false, kb);

                        if (body == null || body.isBlank())
                            throw new IllegalArgumentException(
                                    "Unable to translate formula: " + kif);

                        output.append("tff(")
                                .append(base).append("_").append(index++)
                                .append(", axiom, ")
                                .append(body.trim())
                                .append(").\n");
                    }

                    return output.toString();
                }
            }, 20000);

            writeTffJson(resp, true, null, result);
        }
        catch (RejectedExecutionException e) {
            resp.setStatus(429);
            writeTffJson(resp, false, "Server busy. Please retry.", null);
        }
        catch (TimeoutException e) {
            resp.setStatus(503);
            writeTffJson(resp, false, "Translation timed out.", null);
        }
        catch (Exception e) {
            resp.setStatus(400);
            writeTffJson(resp, false,
                    "Exception during translation: " + e.getMessage(), null);
        }
    }

    private void handleTranslateToTHF(HttpServletRequest req,
                                      HttpServletResponse resp) throws IOException {

        resp.setContentType("application/json; charset=UTF-8");

        String fileName = Optional.ofNullable(req.getParameter("fileName"))
                .orElse("buffer.kif");
        String code = Optional.ofNullable(req.getParameter("code")).orElse("");
        String kbName = Optional.ofNullable(req.getParameter("kb")).orElse("SUMO");

        if (code.trim().isEmpty()) {
            writeThfJson(resp, false, "No KIF content received.", null);
            return;
        }

        try {
            String result = EditorWorkerQueue.submit(() -> {
                synchronized (TRANSLATE_LOCK) {
                    KBmanager.getMgr().initializeOnce();

                    KB kb = KBmanager.getMgr().getKB(kbName);
                    if (kb == null)
                        throw new IllegalArgumentException(
                                "Knowledge base not found: " + kbName);

                    List<String> formulas = splitKifFormulas(code);
                    String base = fileName.replaceAll("\\.[^.]+$", "");
                    if (base.isEmpty()) base = "buf";

                    StringBuilder output = new StringBuilder();
                    int index = 1;

                    for (String kif : formulas) {
                        String body = SUMOformulaToTPTPformula
                                .tptpParseSUOKIFString(kif, false, "thf");

                        if (body == null || body.isBlank())
                            throw new IllegalArgumentException(
                                    "Unable to translate formula: " + kif);

                        output.append("thf(")
                                .append(base).append("_").append(index++)
                                .append(", axiom, ")
                                .append(body.trim())
                                .append(").\n");
                    }

                    return output.toString();
                }
            }, 20000);

            if (result == null || result.isBlank()) {
                writeThfJson(resp, false, "Translation produced no output.", null);
                return;
            }
            writeThfJson(resp, true, null, result);
        }
        catch (RejectedExecutionException e) {
            resp.setStatus(429);
            writeThfJson(resp, false, "Server busy. Please retry.", null);
        }
        catch (TimeoutException e) {
            resp.setStatus(503);
            writeThfJson(resp, false, "Translation timed out.", null);
        }
        catch (Exception e) {
            resp.setStatus(400);
            writeThfJson(resp, false,
                    "Exception during translation: " + e.getMessage(), null);
        }
    }

    /**
     * Run a highlighted SUO-KIF expression as a query without leaving the editor.
     * The first version intentionally mirrors AskTell's basic Vampire defaults.
     */
    private void handleQuery(HttpServletRequest req, HttpServletResponse resp) throws IOException {

        resp.setContentType("application/json; charset=UTF-8");
        String statement = Optional.ofNullable(req.getParameter("stmt")).orElse("").trim();
        String kbName = Optional.ofNullable(req.getParameter("kb")).orElse("SUMO").trim();
        if (kbName.isEmpty()) kbName = "SUMO";

        if (statement.isEmpty()) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            writeJson(resp, false, "Highlight a SUO-KIF expression first.", null);
            return;
        }
        if (statement.indexOf('@') >= 0) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            writeJson(resp, false, "Row variables (@) are not allowed in queries.", null);
            return;
        }

        HttpSession session = req.getSession(false);
        if (session == null) {
            resp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            writeJson(resp, false, "Your session has expired. Please log in again.", null);
            return;
        }

        try {
            KBmanager.getMgr().initializeOnce();
            KB kb = KBmanager.getMgr().getKB(kbName);
            if (kb == null) {
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                writeJson(resp, false, "Knowledge base not found: " + kbName, null);
                return;
            }

            ATPQuery atpQuery = new ATPQuery(
                    kb,
                    session.getId(),
                    statement,
                    null,
                    "custom",
                    "Vampire",
                    "fof",
                    "CASC",
                    false,
                    false,
                    false,
                    false,
                    30,
                    1
            );
            ATPResult result = new TheoremProverController().ask(atpQuery);

            if (result == null) {
                writeJson(resp, false, "No result returned by Vampire.", null);
                return;
            }

            List<String> stdout = result.getStdout();
            String proof = stdout == null || stdout.isEmpty()
                    ? "Vampire completed, but returned no proof output."
                    : String.join(System.lineSeparator(), stdout);
            writeJson(resp, true, proof, null);
        }
        catch (ProverTimeoutException e) {
            resp.setStatus(HttpServletResponse.SC_GATEWAY_TIMEOUT);
            writeJson(resp, false, "Vampire timed out after 30 seconds.", null);
        }
        catch (ProverCrashedException e) {
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            String detail = e.getResult() == null || e.getResult().getStdout() == null
                    ? ""
                    : System.lineSeparator() + String.join(System.lineSeparator(), e.getResult().getStdout());
            writeJson(resp, false, "Vampire crashed." + detail, null);
        }
        catch (Exception e) {
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            writeJson(resp, false, "Unable to run query: " +
                    (e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage()), null);
        }
    }
    /**
     * Dispatch ATP execution based on the active editor file type.
     */
    private void handleRunAtp(HttpServletRequest req, HttpServletResponse resp) throws IOException {

        String fileName = Optional.ofNullable(req.getParameter("fileName")).orElse("");
        if (isTqFile(fileName)) {
            handleRunTq(req, resp);
            return;
        }
        handleRunProblemFile(req, resp);
    }

    /**
     * Run a complete TPTP-family editor buffer directly through the selected
     * prover. This path intentionally does not use InferenceTest.
     */
    private void handleRunProblemFile(HttpServletRequest req,
                                      HttpServletResponse resp) throws IOException {

        resp.setContentType("application/json; charset=UTF-8");
        String fileName = Optional.ofNullable(req.getParameter("fileName")).orElse("");
        String code = Optional.ofNullable(req.getParameter("code")).orElse("");
        String extension = fileExtension(fileName);
        Set<String> supported = new HashSet<>(Arrays.asList(
                "tptp", "p", "fof", "tff", "thf", "cnf"));

        if (!supported.contains(extension)) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            writeJson(resp, false,
                    "Direct ATP runs require a .tptp, .p, .fof, .tff, .thf, or .cnf file.",
                    null);
            return;
        }
        if (code.trim().isEmpty()) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            writeJson(resp, false, "The problem buffer is empty.", null);
            return;
        }

        String prover = Optional.ofNullable(req.getParameter("inferenceEngine"))
                .orElse("VAMPIRE").trim().toUpperCase(Locale.ROOT);
        String vampireMode = Optional.ofNullable(req.getParameter("vampireMode"))
                .orElse("CASC").trim().toUpperCase(Locale.ROOT);
        int timeout = boundedInteger(req.getParameter("timeout"), 30, 1, 300);
        int maxAnswers = boundedInteger(req.getParameter("maxAnswers"), 1, 1, 100);
        String language = detectProblemLanguage(extension, code);
        String kbName = Optional.ofNullable(req.getParameter("kb")).orElse("SUMO").trim();
        java.nio.file.Path problemFile = null;

        try {
            KBmanager.getMgr().initializeOnce();
            KB kb = KBmanager.getMgr().getKB(kbName.isEmpty() ? "SUMO" : kbName);
            if (kb == null) {
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                writeJson(resp, false, "Knowledge base not found: " + kbName, null);
                return;
            }

            problemFile = java.nio.file.Files.createTempFile(
                    "sigma-editor-problem-", "." + extension);
            java.nio.file.Files.writeString(problemFile, code, StandardCharsets.UTF_8);
            HttpSession session = req.getSession(false);
            String sessionId = session == null ? "editor-" + UUID.randomUUID() :
                    session.getId();

            ATPResult result = new TheoremProverController().runProblemFile(
                    kb, problemFile, prover, language, vampireMode,
                    timeout, maxAnswers, sessionId);
            writeDirectAtpResult(resp, result, language);
        }
        catch (ProverTimeoutException e) {
            resp.setStatus(HttpServletResponse.SC_GATEWAY_TIMEOUT);
            writeDirectAtpResult(resp, e.getResult(), language);
        }
        catch (ProverCrashedException e) {
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            writeDirectAtpResult(resp, e.getResult(), language);
        }
        catch (Exception e) {
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            writeJson(resp, false, "Unable to run problem file: " +
                    (e.getMessage() == null ? e.getClass().getSimpleName() :
                            e.getMessage()), null);
        }
        finally {
            if (problemFile != null)
                java.nio.file.Files.deleteIfExists(problemFile);
        }
    }

    private void writeDirectAtpResult(HttpServletResponse resp, ATPResult result,
                                      String language) throws IOException {

        if (result == null) {
            writeJson(resp, false, "The theorem prover returned no result.", null);
            return;
        }

        String szs = result.getSzsStatus() == null ? "" :
                result.getSzsStatus().getTptpName();
        boolean proved = "Theorem".equalsIgnoreCase(szs);
        boolean error = result.getSzsStatus() == null ? result.hasErrors() :
                result.getSzsStatus().isError();
        List<String> output = new ArrayList<>();
        if (result.getStdout() != null) output.addAll(result.getStdout());
        if (result.getStderr() != null) output.addAll(result.getStderr());
        if (result.getPrimaryError() != null) output.add(result.getPrimaryError());

        StringBuilder json = new StringBuilder();
        json.append("{\"success\":true")
                .append(",\"direct\":true")
                .append(",\"passed\":").append(proved)
                .append(",\"error\":").append(error)
                .append(",\"expectationProvided\":false")
                .append(",\"status\":\"").append(jsonEscape(szs)).append("\"")
                .append(",\"szs\":\"").append(jsonEscape(szs)).append("\"")
                .append(",\"translation\":\"").append(jsonEscape(language)).append("\"")
                .append(",\"time\":").append(result.getElapsedTimeMs())
                .append(",\"answers\":[]")
                .append(",\"expected\":[]")
                .append(",\"proof\":\"")
                .append(jsonEscape(String.join(System.lineSeparator(), output)))
                .append("\"}");
        resp.getWriter().write(json.toString());
    }

    private static String fileExtension(String fileName) {

        int dot = fileName == null ? -1 : fileName.lastIndexOf('.');
        return dot < 0 ? "" : fileName.substring(dot + 1).toLowerCase(Locale.ROOT);
    }

    private static String detectProblemLanguage(String extension, String code) {

        if ("thf".equals(extension) ||
                java.util.regex.Pattern.compile("(?im)^\\s*thf\\s*\\(").matcher(code).find())
            return "THF";
        if ("tff".equals(extension) ||
                java.util.regex.Pattern.compile("(?im)^\\s*tff\\s*\\(").matcher(code).find())
            return "TFF";
        return "FOF";

    }

    /**
     * Run the active .tq editor buffer with the selected ATP.  The TQ
     * meta-predicates continue to determine the translation language.
     */
    private void handleRunTq(HttpServletRequest req, HttpServletResponse resp) throws IOException {

        resp.setContentType("application/json; charset=UTF-8");
        String fileName = Optional.ofNullable(req.getParameter("fileName")).orElse("");
        String code = Optional.ofNullable(req.getParameter("code")).orElse("");
        String kbName = Optional.ofNullable(req.getParameter("kb")).orElse("SUMO").trim();
        String prover = Optional.ofNullable(req.getParameter("inferenceEngine"))
                .orElse("VAMPIRE").trim().toUpperCase(Locale.ROOT);
        String vampireMode = Optional.ofNullable(req.getParameter("vampireMode"))
                .orElse("CASC").trim().toUpperCase(Locale.ROOT);

        if (!isTqFile(fileName)) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            writeJson(resp, false, "ATP runs are available only for .tq files.", null);
            return;
        }
        if (code.trim().isEmpty()) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            writeJson(resp, false, "The TQ buffer is empty.", null);
            return;
        }
        if (!Arrays.asList("VAMPIRE", "EPROVER", "LEO").contains(prover)) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            writeJson(resp, false, "Unsupported theorem prover: " + prover, null);
            return;
        }
        if (!Arrays.asList("CASC", "AVATAR", "VAMPIRE").contains(vampireMode))
            vampireMode = "CASC";

        List<String> available = TheoremProverController.availableProvers();
        if (!available.contains(prover.toLowerCase(Locale.ROOT))) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            writeJson(resp, false, prover + " is not configured on this server.", null);
            return;
        }

        int timeout = boundedInteger(req.getParameter("timeout"), 30, 1, 300);
        int maxAnswers = boundedInteger(req.getParameter("maxAnswers"), 1, 1, 100);
        boolean modusPonens = "yes".equalsIgnoreCase(req.getParameter("ModusPonens"));
        boolean dropOnePremise = "true".equalsIgnoreCase(req.getParameter("dropOnePremise"));
        boolean holUseModals = "yes".equalsIgnoreCase(req.getParameter("HolUseModals"));
        java.nio.file.Path tempTq = null;

        try {
            KBmanager.getMgr().initializeOnce();
            KB kb = KBmanager.getMgr().getKB(kbName.isEmpty() ? "SUMO" : kbName);
            if (kb == null) {
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                writeJson(resp, false, "Knowledge base not found: " + kbName, null);
                return;
            }

            tempTq = java.nio.file.Files.createTempFile("sigma-editor-", ".tq");
            java.nio.file.Files.writeString(tempTq, code, StandardCharsets.UTF_8);
            InferenceTest test = new InferenceTest(tempTq.toString());

            if (!test.errors.isEmpty()) {
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                writeJson(resp, false, String.join("; ", test.errors), null);
                return;
            }

            boolean expectationProvided = !test.expectedAnswers.isEmpty();
            List<String> displayedExpectations = new ArrayList<>(test.expectedAnswers);
            // A boolean editor query without an (answer ...) form is still a
            // proof request. Supplying an implicit "yes" lets InferenceTest
            // classify and clean up a successful theorem run normally.
            if (!expectationProvided && !test.query.contains("?"))
                test.expectedAnswers.add("yes");
            test.runTest(kb, prover, test.minLang, vampireMode,
                    test.closedWorldAssumption, modusPonens, dropOnePremise,
                    holUseModals, timeout, maxAnswers);

            InferenceTest.InferenceTestResult result = test.result;
            if (result == null) {
                writeJson(resp, false, "The theorem prover returned no result.", null);
                return;
            }

            String proof = result.proof == null ? "" :
                    String.join(System.lineSeparator(), result.proof);
            boolean proved = result.szsStatus != null &&
                    result.szsStatus.startsWith("Theorem");
            boolean passed = expectationProvided ? result.success : proved;
            String status = expectationProvided
                    ? (result.success ? "PASS" : "FAIL")
                    : (proved ? "PROVED" : "NOT PROVED");

            StringBuilder json = new StringBuilder();
            json.append("{\"success\":true")
                    .append(",\"passed\":").append(passed)
                    .append(",\"expectationProvided\":").append(expectationProvided)
                    .append(",\"status\":\"").append(status).append("\"")
                    .append(",\"szs\":\"").append(jsonEscape(result.szsStatus)).append("\"")
                    .append(",\"translation\":\"").append(jsonEscape(test.minLang)).append("\"")
                    .append(",\"time\":").append(result.execTime)
                    .append(",\"answers\":").append(jsonStringArray(result.answers))
                    .append(",\"expected\":").append(jsonStringArray(displayedExpectations))
                    .append(",\"proof\":\"").append(jsonEscape(proof)).append("\"")
                    .append("}");
            resp.getWriter().write(json.toString());
        }
        catch (Exception e) {
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            writeJson(resp, false, "Unable to run TQ query: " +
                    (e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage()), null);
        }
        finally {
            if (tempTq != null)
                java.nio.file.Files.deleteIfExists(tempTq);
        }
    }

    private static int boundedInteger(String raw, int fallback, int min, int max) {

        try {
            int value = Integer.parseInt(raw);
            return Math.max(min, Math.min(max, value));
        }
        catch (Exception ignored) {
            return fallback;
        }
    }

    private static String jsonStringArray(Collection<String> values) {

        if (values == null || values.isEmpty()) return "[]";
        StringBuilder json = new StringBuilder("[");
        boolean first = true;
        for (String value : values) {
            if (!first) json.append(',');
            json.append('"').append(jsonEscape(value)).append('"');
            first = false;
        }
        return json.append(']').toString();
    }

    private void handleFormatOrCheck(HttpServletRequest req, HttpServletResponse resp)
            throws IOException, ServletException {

        String mode = Optional.ofNullable(req.getParameter("mode")).orElse("");
        String action = Optional.ofNullable(req.getParameter("action")).orElse("");
        String code = req.getParameter("code");
        String codeContent = req.getParameter("codeContent");
        String fileName = req.getParameter("fileName");
        String text = null;
        if (code != null && !code.isBlank()) {
            text = code;
        } else if (codeContent != null && !codeContent.isBlank()) {
            text = codeContent;
        }
        // Multipart upload case
        if (text == null &&
                req.getContentType() != null &&
                req.getContentType().toLowerCase(Locale.ROOT).startsWith("multipart/")) {
            Part filePart = req.getPart("kifFile");
            if (filePart != null && filePart.getSize() > 0) {
                fileName = filePart.getSubmittedFileName();
                text = readUtf8(filePart.getInputStream());
            }
        }
        if (text == null || text.trim().isEmpty()) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            if ("format".equalsIgnoreCase(mode) || "format".equalsIgnoreCase(action)) {
                resp.setContentType("text/plain; charset=UTF-8");
                resp.getWriter().write("No content provided for formatting.");
            } else {
                resp.setContentType("application/json; charset=UTF-8");
                resp.getWriter().write("{\"ok\":false,\"message\":\"No content provided for checking.\"}");
            }
            return;
        }
        if (fileName == null) fileName = "Untitled.kif";
        boolean isTptp = fileName.toLowerCase().matches(".*\\.(tptp|tff|p|fof|cnf|thf)$");
        if ("format".equalsIgnoreCase(mode) || "format".equalsIgnoreCase(action)) {
            resp.setContentType("text/plain; charset=UTF-8");
            final String textFinal = text;
            final boolean isTptpFinal = isTptp;
            try {
                String formatted = EditorWorkerQueue.submit(() -> {
                    return isTptpFinal
                            ? new TPTPFileChecker().formatTptpText(textFinal, "(web-editor)")
                            : KifFileChecker.formatKif(textFinal);
                }, 4000); // 4s timeout (tune)
                resp.getWriter().write(formatted);
            }
            catch (RejectedExecutionException rex) {
                resp.setStatus(429);
                resp.getWriter().write("Server busy. Please retry.");
            }
            catch (TimeoutException tex) {
                resp.setStatus(503);
                resp.getWriter().write("Formatting timed out. Please retry.");
            }
            catch (Exception e) {
                resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                resp.getWriter().write("Formatting error: " + e.getMessage());
            }
            return;
        }
        List<ErrRec> errors = Collections.emptyList();
        List<String> lines = Arrays.asList(text.split("\\R", -1));
        boolean[] errorMask = new boolean[lines.size()];
        String errorMessage = null;

        final boolean isTptpFinal = isTptp;
        final String fileNameFinal = fileName;
        final boolean isTqFinal = isTqFile(fileNameFinal);
        final String textFinal = isTqFinal ? stripTqMetaPredicatesForCheck(text) : text;

        if (isTqFinal && textFinal.trim().isEmpty()) {
            errors = Collections.emptyList();
        }
        else {
            try {
                errors = EditorWorkerQueue.submit(() -> {
                    if (isTptpFinal) return TPTPFileChecker.check(textFinal, "(web-editor)");
                    synchronized (KIF_CHECK_LOCK) {
                        return KifFileChecker.check(textFinal, fileNameFinal);
                    }
                }, 15000);
            }
            catch (RejectedExecutionException rex) {
                writeBusy(resp, "Server busy. Please retry.");
                return;
            }
            catch (TimeoutException tex) {
                writeTimeout(resp, "Check timed out. Please retry.");
                return;
            }
            catch (Exception e) {
                errors = Collections.emptyList();
                errorMessage = "Error while checking: " + e.getMessage();
            }
        }
        if (errors != null) {
            for (ErrRec er : errors) {
                int ln = er.line;
                if (ln >= 1 && ln <= errorMask.length) {
                    errorMask[ln - 1] = true;
                }
            }
        }
        resp.setStatus(HttpServletResponse.SC_OK);
        resp.setContentType("application/json; charset=UTF-8");
        StringBuilder json = new StringBuilder("{\"ok\":true");
        json.append(",\"fileName\":\"").append(jsonEscape(fileName)).append("\"");
        if (errorMessage != null) {
            json.append(",\"message\":\"").append(jsonEscape(errorMessage)).append("\"");
        }
        json.append(",\"errors\":[");
        if (errors != null) {
            for (int i = 0; i < errors.size(); i++) {
                ErrRec e = errors.get(i);
                json.append("{\"type\":").append(e.type)
                        .append(",\"file\":\"").append(jsonEscape(e.file == null ? "" : e.file)).append("\"")
                        .append(",\"line\":").append(e.line)
                        .append(",\"start\":").append(e.start)
                        .append(",\"end\":").append(e.end)
                        .append(",\"msg\":\"").append(jsonEscape(e.msg)).append("\"}");
                if (i < errors.size() - 1) json.append(",");
            }
        }
        json.append("],\"errorMask\":[");
        for (int i = 0; i < errorMask.length; i++) {
            if (i > 0) json.append(",");
            json.append(errorMask[i] ? "true" : "false");
        }
        json.append("],\"isTptp\":").append(isTptp ? "true" : "false");
        json.append(",\"lineCount\":").append(lines.size()).append("}");
        resp.getWriter().write(json.toString());
    }

    // ============================================================
    // KIF splitter used by translate handle
    // ============================================================

    /**
     * Very simple SUO-KIF splitter:
     * - strips ';' comments
     * - tracks parentheses depth
     * - whenever depth returns to 0 and we've seen some content,
     *   we treat that as one complete formula.
     */
    private List<String> splitKifFormulas(String text) {
        List<String> result = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        int depth = 0;
        boolean inString = false;
        boolean seenNonWhitespace = false;
        try (BufferedReader br = new BufferedReader(new StringReader(text))) {
            String line;
            while ((line = br.readLine()) != null) {
                int semi = line.indexOf(';');
                if (semi >= 0)
                    line = line.substring(0, semi);
                if (line.isEmpty() && depth == 0)
                    continue;
                for (int i = 0; i < line.length(); i++) {
                    char c = line.charAt(i);
                    current.append(c);
                    if (c == '"') {
                        inString = !inString;
                    }
                    if (!inString) {
                        if (c == '(') {
                            depth++;
                            seenNonWhitespace = true;
                        } else if (c == ')') {
                            depth--;
                        } else if (!Character.isWhitespace(c)) {
                            seenNonWhitespace = true;
                        }
                    }
                    if (!inString && depth == 0 && seenNonWhitespace) {
                        String f = current.toString().trim();
                        if (!f.isEmpty()) {
                            result.add(f);
                        }
                        current.setLength(0);
                        seenNonWhitespace = false;
                    }
                }
                if (depth > 0) current.append('\n');
            }
        } catch (IOException ignore) {
        }
        String leftover = current.toString().trim();
        if (!leftover.isEmpty() && depth == 0) {
            result.add(leftover);
        }
        return result;
    }

    private void writeBusy(HttpServletResponse resp, String msg) throws IOException {
        resp.setStatus(429); // Too Many Requests
        resp.setContentType("application/json; charset=UTF-8");
        String json = "{\"ok\":false,\"retry\":true,\"message\":\"" + jsonEscape(msg) + "\"" +
                ",\"queueDepth\":" + EditorWorkerQueue.queueDepth() +
                ",\"active\":" + EditorWorkerQueue.activeCount() +
                ",\"workers\":" + EditorWorkerQueue.workers() +
                "}";
        resp.getWriter().write(json);
    }

    private void writeTimeout(HttpServletResponse resp, String msg) throws IOException {
        resp.setStatus(503); // Service Unavailable
        resp.setContentType("application/json; charset=UTF-8");
        String json = "{\"ok\":false,\"retry\":true,\"message\":\"" + jsonEscape(msg) + "\"" +
                ",\"queueDepth\":" + EditorWorkerQueue.queueDepth() +
                ",\"active\":" + EditorWorkerQueue.activeCount() +
                ",\"workers\":" + EditorWorkerQueue.workers() +
                "}";
        resp.getWriter().write(json);
    }

    private static boolean isTqFile(String fileName) {
        return fileName != null && fileName.toLowerCase(Locale.ROOT).endsWith(".tq");
    }

    private static String topLevelPredicate(String form) {

        if (form == null) return "";
        String s = form.trim();
        if (!s.startsWith("(")) return "";
        int i = 1;
        while (i < s.length() && Character.isWhitespace(s.charAt(i))) i++;
        int start = i;
        while (i < s.length()) {
            char c = s.charAt(i);
            if (Character.isWhitespace(c) || c == ')' || c == '(') break;
            i++;
        }
        return start < i ? s.substring(start, i) : "";
    }

    private static int parenDeltaIgnoringCommentsAndStrings(String line) {

        int delta = 0;
        boolean inString = false;
        boolean escaped = false;

        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);

            if (!inString && c == ';') break;

            if (inString) {
                if (escaped) {
                    escaped = false;
                }
                else if (c == '\\') {
                    escaped = true;
                }
                else if (c == '"') {
                    inString = false;
                }
                continue;
            }

            if (c == '"') {
                inString = true;
            }
            else if (c == '(') {
                delta++;
            }
            else if (c == ')') {
                delta--;
            }
        }

        return delta;
    }

    private static void appendBlankLines(StringBuilder sb, int count) {

        for (int i = 0; i < count; i++)
            sb.append('\n');
    }

    /**
     * Removes .tq meta-predicate forms before normal KIF checking.
     * Preserves line count so returned ErrRec line numbers still match the editor buffer.
     */
    private static String stripTqMetaPredicatesForCheck(String text) {

        String[] lines = text.split("\\R", -1);
        StringBuilder out = new StringBuilder(text.length());
        List<String> formLines = new ArrayList<>();
        int depth = 0;

        for (String line : lines) {
            String trimmed = line.trim();

            if (depth == 0 && (trimmed.isEmpty() || trimmed.startsWith(";"))) {
                out.append(line).append('\n');
                continue;
            }

            formLines.add(line);
            depth += parenDeltaIgnoringCommentsAndStrings(line);

            if (depth == 0) {
                String form = String.join("\n", formLines);
                String pred = topLevelPredicate(form);

                if (TQ_META_PREDICATES.contains(pred))
                    appendBlankLines(out, formLines.size());
                else
                    for (String kept : formLines)
                        out.append(kept).append('\n');

                formLines.clear();
            }
        }

        if (!formLines.isEmpty()) {
            for (String leftover : formLines)
                out.append(leftover).append('\n');
        }

        return out.toString();
    }
}