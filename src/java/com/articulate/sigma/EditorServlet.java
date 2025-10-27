package com.articulate.sigma;

import javax.servlet.*;
import javax.servlet.http.*;
import javax.servlet.annotation.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.*;

/**
 * Unified Editor Servlet for both .kif and .tptp code/files.
 * - "format": returns text/plain with formatted content.
 * - "check":  returns application/json with the full list of ErrRec objects and an errorMask.
 */
@WebServlet("/EditorServlet")
@MultipartConfig(
        fileSizeThreshold = 1024,   // buffer to disk after 1 KB
        maxFileSize = 200 * 1024,   // 200 KB max file
        maxRequestSize = 220 * 1024 // 220 KB total
)
public class EditorServlet extends HttpServlet {

    private static String readUtf8(InputStream in) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buf = new byte[8192];
        int n;
        while ((n = in.read(buf)) != -1) out.write(buf, 0, n);
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
                    if (c < 0x20) {
                        b.append(String.format("\\u%04x", (int)c));
                    } else {
                        b.append(c);
                    }
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
            if ("format".equalsIgnoreCase(mode) || "format".equalsIgnoreCase(action)) {
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                resp.setContentType("text/plain; charset=UTF-8");
                resp.getWriter().write("No content provided for formatting.");
            } else {
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                resp.setContentType("application/json; charset=UTF-8");
                resp.getWriter().write("{\"ok\":false,\"message\":\"No content provided for checking.\"}");
            }
            return;
        }
        if (fileName == null) fileName = "Untitled.kif";
        boolean isTptp = fileName.toLowerCase().endsWith(".tptp")
                || fileName.toLowerCase().endsWith(".tff")
                || fileName.toLowerCase().endsWith(".p")
                || fileName.toLowerCase().endsWith(".fof")
                || fileName.toLowerCase().endsWith(".cnf")
                || fileName.toLowerCase().endsWith(".thf");
        if ("format".equalsIgnoreCase(mode) || "format".equalsIgnoreCase(action)) {
            resp.setContentType("text/plain; charset=UTF-8");
            try {
                String formatted;
                if (isTptp) {
                    TPTPChecker formatter = new TPTPChecker();
                    formatted = formatter.formatTptpText(text, "(web-editor)");
                } else {
                    formatted = formatKif(text);
                }
                if (formatted == null) {
                    resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                    resp.getWriter().write("Failed to format input.");
                } else {
                    resp.getWriter().write(formatted);
                }
            } catch (Exception e) {
                resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                resp.getWriter().write("Formatting error: " + e.getMessage());
            }
            return;
        }
        List<ErrRec> errors = Collections.emptyList();
        List<String> lines = Arrays.asList(text.split("\\R", -1));
        boolean[] errorMask = new boolean[lines.size()];
        String errorMessage = null;
        try {
            if (isTptp) {
                errors = TPTPChecker.check(text, "(web-editor)");
            } else {
                Future<List<ErrRec>> future = KifCheckWorker.submit(text);
                errors = future.get();
            }
        } catch (Exception e) {
            errorMessage = "Error while checking: " + e.getMessage();
            errors = Collections.emptyList();
        }
        if (errors != null) {
            for (ErrRec er : errors) {
                int ln = er.line + 1; // ErrRec is 0-based; mask is 1-based for readability
                if (ln >= 1 && ln <= errorMask.length) errorMask[ln - 1] = true;
            }
        }
        resp.setStatus(HttpServletResponse.SC_OK);
        resp.setContentType("application/json; charset=UTF-8");
        StringBuilder json = new StringBuilder(4096);
        json.append("{\"ok\":true");
        json.append(",\"fileName\":\"").append(jsonEscape(fileName)).append("\"");
        if (errorMessage != null) {
            json.append(",\"message\":\"").append(jsonEscape(errorMessage)).append("\"");
        }
        json.append(",\"errors\":[");
        if (errors != null) {
            for (int i = 0; i < errors.size(); i++) {
                ErrRec e = errors.get(i);
                json.append("{")
                    .append("\"type\":").append(e.type).append(",")
                    .append("\"file\":\"").append(jsonEscape(e.file == null ? "" : e.file)).append("\",")
                    .append("\"line\":").append(e.line).append(",")
                    .append("\"start\":").append(e.start).append(",")
                    .append("\"end\":").append(e.end).append(",")
                    .append("\"msg\":\"").append(jsonEscape(e.msg)).append("\"")
                    .append("}");
                if (i < errors.size() - 1) json.append(",");
            }
        }
        json.append("]");
        json.append(",\"errorMask\":[");
        for (int i = 0; i < errorMask.length; i++) {
            if (i > 0) json.append(",");
            json.append(errorMask[i] ? "true" : "false");
        }
        json.append("]");
        json.append(",\"isTptp\":").append(isTptp ? "true" : "false");
        json.append(",\"lineCount\":").append(lines.size());
        json.append("}");
        resp.getWriter().write(json.toString());
    }

    /** KIF auto-formatter (migrated from KifFileCheckServlet) */
    public static String formatKif(String contents) {
        if (contents == null || contents.trim().isEmpty()) return contents;

        KIF kif = new KIF();
        try (StringReader sr = new StringReader(contents)) {
            kif.parse(sr);
        } catch (Exception e) {
            System.err.println("EditorServlet.formatKif(): parse failed - returning original: " + e.getMessage());
            return contents;
        }

        StringBuilder sb = new StringBuilder();
        for (Formula f : kif.formulasOrdered.values()) {
            sb.append(f.toString()).append("\n");
        }
        return sb.toString();
    }
}
