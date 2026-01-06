package com.articulate.sigma;

import com.articulate.sigma.trans.SUMOformulaToTPTPformula;

import javax.servlet.*;
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
    private static final Object TRANSLATE_LOCK = new Object();

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException, ServletException {

        boolean awsMode = "yes".equalsIgnoreCase(KBmanager.getMgr().getPref("aws"));
        if (awsMode) {
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
            case "translatetotptp":
                handleTranslateToTPTP(req, resp);
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

    /**
     * Check session + role. Returns username or null if already responded with error.
     */
    private String requireUser(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        HttpSession session = req.getSession(false);
        String username = session != null ? (String) session.getAttribute("user") : null;
        String role = session != null ? (String) session.getAttribute("role") : null;
        if (username == null || role == null) {
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
                    SUMOformulaToTPTPformula.lang = "fof";
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
        List<ErrRec> errors;
        List<String> lines = Arrays.asList(text.split("\\R", -1));
        boolean[] errorMask = new boolean[lines.size()];
        String errorMessage = null;
        try {
            final String textFinal = text;
            final boolean isTptpFinal = isTptp;
            try {
                errors = EditorWorkerQueue.submit(() -> {
                    return isTptpFinal
                            ? TPTPFileChecker.check(textFinal, "(web-editor)")
                            : KifFileChecker.check(textFinal);
                }, 4000); // 4s timeout for auto-checks

            } catch (RejectedExecutionException rex) {
                writeBusy(resp, "Server busy. Please retry.");
                return;

            } catch (TimeoutException tex) {
                writeTimeout(resp, "Check timed out. Please retry.");
                return;

            } catch (Exception e) {
                errors = Collections.emptyList();
                errorMessage = "Error while checking: " + e.getMessage();
            }
        } catch (Exception e) {
            errors = Collections.emptyList();
            errorMessage = "Error while checking: " + e.getMessage();
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
    // KIF splitter used by translate handler
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
}
