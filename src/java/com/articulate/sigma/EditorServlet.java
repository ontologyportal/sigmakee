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

                // --- Always run a check when formatting (keep it simple)
                // Use the ORIGINAL text for stable line numbers (safer for UI highlights)
                List<ErrRec> fmtErrors = Collections.emptyList();
                String checkMsg = null;
                try {
                    if (isTptp) {
                        fmtErrors = TPTPChecker.check(text, "(web-editor)");
                    } else {
                        Future<List<ErrRec>> fut = KifCheckWorker.submit(text);
                        fmtErrors = fut.get();
                    }
                } catch (Exception ce) {
                    checkMsg = "Check failed: " + ce.getMessage();
                }

                // Expose simple info in headers (UI can read and show errors)
                int errCount = (fmtErrors == null) ? 0 : fmtErrors.size();
                resp.setHeader("X-Has-Errors", (errCount > 0) ? "true" : "false");
                resp.setHeader("X-Error-Count", String.valueOf(errCount));
                if (checkMsg != null) resp.setHeader("X-Check-Message", checkMsg);

                // Optional: include first error message for convenience (kept short)
                if (errCount > 0) {
                    ErrRec first = fmtErrors.get(0);
                    String preview = first.msg;
                    if (preview != null && preview.length() > 200) preview = preview.substring(0, 200);
                    resp.setHeader("X-First-Error", preview == null ? "" : preview);
                }

                if (formatted == null || formatted.trim().isEmpty()) {
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

    public static String formatKif(String contents) {
        if (contents == null || contents.trim().isEmpty()) return contents;
        final List<int[]> spans = new ArrayList<>();
        final List<Integer> commentOffsets = new ArrayList<>();
        final List<String> commentLines = new ArrayList<>();
        scanTopLevelSpansAndComments(contents, spans, commentOffsets, commentLines);
        final List<String> formattedForms = new ArrayList<>();
        KIF kif = new KIF();
        try (StringReader sr = new StringReader(contents)) {
            kif.parse(sr);
        } catch (Exception e) {
            System.err.println("EditorServlet.formatKif(): parse failed - returning original: " + e.getMessage());
            return contents;
        }
        if (kif.formulasOrdered == null || kif.formulasOrdered.isEmpty()) {
            return contents;
        }
        for (Formula f : kif.formulasOrdered.values()) {
            String s = (f == null) ? "" : f.toString();
            formattedForms.add(s == null ? "" : s.trim());
        }
        boolean allEmpty = formattedForms.stream().allMatch(s -> s.isEmpty());
        if (allEmpty) return contents;
        if (spans.isEmpty() || spans.size() != formattedForms.size()) {
            StringBuilder sb = new StringBuilder();
            for (String s : formattedForms) if (!s.isEmpty()) sb.append(s).append("\n");
            for (String c : commentLines) sb.append(c).append("\n");  // emit raw comment lines
            String result = sb.toString();
            return result.trim().isEmpty() ? contents : result;
        }
        for (int i = 0; i < formattedForms.size(); i++) {
            if (formattedForms.get(i).isEmpty()) {
                int[] span = spans.get(i);
                String raw = contents.substring(span[0], Math.min(span[1], contents.length())).trim();
                if (!raw.isEmpty()) formattedForms.set(i, raw);
            }
        }
        int[] extraBlankAfterPrev = new int[spans.size()];
        for (int i = 1; i < spans.size(); i++) {
            int prevEnd = spans.get(i - 1)[1];
            int curStart = spans.get(i)[0];
            int extra = 0;
            if (prevEnd < curStart) {
                String between = contents.substring(prevEnd, curStart);
                String[] parts = between.split("\\R", -1);
                for (int idx = 1; idx < parts.length - 1; idx++) {
                    if (parts[idx].trim().isEmpty()) extra++;
                    else break; // stop at first non-empty content
                }
            }
            extraBlankAfterPrev[i] = extra;
        }
        @SuppressWarnings("unchecked")
        List<String>[] leading = new List[spans.size()];
        @SuppressWarnings("unchecked")
        List<String>[] inside  = new List[spans.size()];
        for (int i = 0; i < spans.size(); i++) { leading[i] = new ArrayList<>(); inside[i]  = new ArrayList<>(); }
        int commentIdx = 0, spanIdx = 0;
        while (commentIdx < commentOffsets.size()) {
            int cOff = commentOffsets.get(commentIdx);
            String cLine = commentLines.get(commentIdx);
            while (spanIdx < spans.size() && spans.get(spanIdx)[1] <= cOff) spanIdx++;
            if (spanIdx < spans.size()) {
                int[] span = spans.get(spanIdx);
                if (cOff < span[0])            leading[spanIdx].add(cLine);
                else if (cOff < span[1])       inside[spanIdx].add(cLine);
                else if (spanIdx + 1 < spans.size()) leading[spanIdx + 1].add(cLine);
                else                           inside[spans.size() - 1].add(cLine);
            } else {
                inside[spans.size() - 1].add(cLine);
            }
            commentIdx++;
        }
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < formattedForms.size(); i++) {
            if (i > 0 && extraBlankAfterPrev[i] > 0) {
                for (int k = 0; k < extraBlankAfterPrev[i]; k++) out.append("\n");
            }
            for (String c : leading[i]) out.append(c).append("\n");
            String form = formattedForms.get(i).trim();
            if (!form.isEmpty()) out.append(form).append("\n");
            for (String c : inside[i]) out.append(c).append("\n");
        }
        String result = out.toString();
        return result.trim().isEmpty() ? contents : result;
    }

    private static void scanTopLevelSpansAndComments(
            String s,
            List<int[]> spans,
            List<Integer> commentOffsets,
            List<String> commentLines
    ) {
        int n = s.length();
        int depth = 0;
        int i = 0;
        int currentSpanStart = -1;
        boolean inString = false;
        boolean escaping = false;
        while (i < n) {
            char ch = s.charAt(i);
            if (ch == '"' && !escaping) {
                inString = !inString;
                i++;
                continue;
            }
            escaping = (ch == '\\' && !escaping);
            if (!inString) {
                if (ch == ';') {
                    int commentStart = i;
                    int j = i + 1;
                    while (j < n) {
                        char cj = s.charAt(j);
                        if (cj == '\n' || cj == '\r') break;
                        j++;
                    }
                    commentOffsets.add(commentStart);
                    String rawLine = s.substring(commentStart, j);
                    commentLines.add(rawLine);
                    i = j;
                    continue;
                }
                if (ch == '\r') { i++; continue; }
                if (ch == '(') {
                    if (depth == 0) currentSpanStart = i;
                    depth++;
                } else if (ch == ')') {
                    depth = Math.max(0, depth - 1);
                    if (depth == 0 && currentSpanStart >= 0) {
                        spans.add(new int[]{ currentSpanStart, i + 1 });
                        currentSpanStart = -1;
                    }
                }
            }
            i++;
        }
    }
}