package com.articulate.sigma;

import javax.servlet.*;
import javax.servlet.http.*;
import javax.servlet.annotation.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * Unified Editor Servlet for both .kif and .tptp code/files.
 * Handles format, check, and user file operations (save/load/list).
 */
@WebServlet("/EditorServlet")
@MultipartConfig(
    fileSizeThreshold = 1024,
    maxFileSize = 200 * 1024,
    maxRequestSize = 220 * 1024
)
public class EditorServlet extends HttpServlet {

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
        StringBuilder b = new StringBuilder((int)(s.length() * 1.1));
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '\\': b.append("\\\\"); break;
                case '"':  b.append("\\\""); break;
                case '\b': b.append("\\b");  break;
                case '\f': b.append("\\f");  break;
                case '\n': b.append("\\n");  break;
                case '\r': b.append("\\r");  break;
                case '\t': b.append("\\t");  break;
                default:
                    if (c < 0x20) b.append(String.format("\\u%04x", (int)c));
                    else b.append(c);
            }
        }
        return b.toString();
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws IOException, ServletException {

        String mode = req.getParameter("mode");
        String code = req.getParameter("code");
        String codeContent = req.getParameter("codeContent");
        String action = req.getParameter("action");
        String fileName = req.getParameter("fileName");
        String text = null;

        // ------------------------------------------------------------
        // Enforce logged-in user with role=user or role=admin
        // ------------------------------------------------------------
        HttpSession session = req.getSession(false);
        String username = session != null ? (String) session.getAttribute("user") : null;
        String role = session != null ? (String) session.getAttribute("role") : null;

        if (username == null || role == null) {
            resp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            resp.getWriter().write("{\"success\":false,\"message\":\"Not logged in.\"}");
            return;
        }

        if (!role.equalsIgnoreCase("user") && !role.equalsIgnoreCase("admin")) {
            resp.setStatus(HttpServletResponse.SC_FORBIDDEN);
            resp.getWriter().write("{\"success\":false,\"message\":\"Access denied. Must be user or admin.\"}");
            return;
        }

        // Root dir: ~/.sigmakee/KBs/UserKBs/<username>/
        File userDir = new File(System.getProperty("user.home") + "/.sigmakee/KBs/UserKBs/" + username);
        if (!userDir.exists()) userDir.mkdirs();

        // ============================================================
        // SAVE USER FILE
        // ============================================================
        if ("saveUserFile".equalsIgnoreCase(mode)) {
            String filename = req.getParameter("fileName");
            String contents = req.getParameter("code");

            if (filename == null || filename.isBlank()) {
                resp.setContentType("application/json; charset=UTF-8");
                resp.getWriter().write("{\"success\":false,\"message\":\"Filename missing.\"}");
                return;
            }
            if (contents == null) contents = "";

            File outFile = new File(userDir, filename);
            try (FileWriter fw = new FileWriter(outFile)) {
                fw.write(contents);
            } catch (IOException e) {
                resp.setContentType("application/json; charset=UTF-8");
                resp.getWriter().write("{\"success\":false,\"message\":\"" + e.getMessage().replace("\"", "'") + "\"}");
                return;
            }

            resp.setContentType("application/json; charset=UTF-8");
            resp.getWriter().write("{\"success\":true,\"message\":\"Saved successfully.\"}");
            return;
        }

        // ============================================================
        // LIST USER FILES
        // ============================================================
        if ("listUserFiles".equalsIgnoreCase(mode)) {
            List<String> fileNames = new ArrayList<>();
            if (userDir.exists() && userDir.isDirectory()) {
                File[] files = userDir.listFiles();
                if (files != null) {
                    for (File f : files)
                        if (f.isFile()) fileNames.add(f.getName());
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
            return;
        }

        // ============================================================
        // LOAD USER FILE
        // ============================================================
        if ("loadUserFile".equalsIgnoreCase(mode)) {
            String filename = req.getParameter("fileName");
            if (filename == null || filename.isBlank()) {
                resp.setContentType("application/json; charset=UTF-8");
                resp.getWriter().write("{\"success\":false,\"message\":\"Filename missing.\"}");
                return;
            }

            File target = new File(userDir, filename);
            if (!target.exists()) {
                resp.setContentType("application/json; charset=UTF-8");
                resp.getWriter().write("{\"success\":false,\"message\":\"File not found.\"}");
                return;
            }

            String contents = new String(java.nio.file.Files.readAllBytes(target.toPath()), StandardCharsets.UTF_8);
            resp.setContentType("application/json; charset=UTF-8");
            resp.getWriter().write("{\"success\":true,\"fileName\":\"" + jsonEscape(filename) +
                    "\",\"contents\":\"" + jsonEscape(contents) + "\"}");
            return;
        }

        // ============================================================
        // FORMAT / CHECK (original logic unchanged)
        // ============================================================
        if (code != null && !code.isBlank()) text = code;
        else if (codeContent != null && !codeContent.isBlank()) text = codeContent;

        if (text == null && req.getContentType() != null &&
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
            try {
                String formatted = isTptp
                        ? new TPTPFileChecker().formatTptpText(text, "(web-editor)")
                        : KifFileChecker.formatKif(text);
                resp.getWriter().write(formatted);
            } catch (Exception e) {
                resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                resp.getWriter().write("Formatting error: " + e.getMessage());
            }
            return;
        }

        // --- Run check and return JSON ---
        List<ErrRec> errors;
        List<String> lines = Arrays.asList(text.split("\\R", -1));
        boolean[] errorMask = new boolean[lines.size()];
        String errorMessage = null;

        try {
            errors = isTptp ? TPTPFileChecker.check(text, "(web-editor)") : KifFileChecker.check(text);
        } catch (Exception e) {
            errors = Collections.emptyList();
            errorMessage = "Error while checking: " + e.getMessage();
        }

        if (errors != null) {
            for (ErrRec er : errors) {
                int ln = er.line;
                if (ln >= 1 && ln <= errorMask.length) errorMask[ln - 1] = true;
            }
        }

        resp.setStatus(HttpServletResponse.SC_OK);
        resp.setContentType("application/json; charset=UTF-8");
        StringBuilder json = new StringBuilder("{\"ok\":true");
        json.append(",\"fileName\":\"").append(jsonEscape(fileName)).append("\"");
        if (errorMessage != null)
            json.append(",\"message\":\"").append(jsonEscape(errorMessage)).append("\"");
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
}
