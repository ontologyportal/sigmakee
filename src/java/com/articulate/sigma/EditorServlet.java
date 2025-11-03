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
    /** KIF auto-formatter that preserves ; comments relative to formulas (no extra classes). */
public static String formatKif(String contents) {
    if (contents == null || contents.trim().isEmpty()) return contents;

    // --- 1) Scan original text: collect top-level formula spans and comments (offset,text)
    final List<int[]> spans = new ArrayList<>();          // each int[]{start, endExclusive}
    final List<Integer> commentOffsets = new ArrayList<>(); // char offsets of ';'
    final List<String> commentTexts = new ArrayList<>();    // text after ';' (no newline)

    scanTopLevelSpansAndComments(contents, spans, commentOffsets, commentTexts);

    // --- 2) Parse and render formulas with your existing parser
    final List<String> formattedForms = new ArrayList<>();
    KIF kif = new KIF();
    try (StringReader sr = new StringReader(contents)) {
        kif.parse(sr);
    } catch (Exception e) {
        // If parse fails, fall back to original text (do not lose comments)
        System.err.println("EditorServlet.formatKif(): parse failed - returning original: " + e.getMessage());
        return contents;
    }
    for (Formula f : kif.formulasOrdered.values()) {
        formattedForms.add(f.toString().trim());
    }

    // --- 3) If counts mismatch, just return concatenated formatted forms + all comments at the end (safe fallback)
    if (spans.isEmpty() || spans.size() != formattedForms.size()) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < formattedForms.size(); i++) {
            sb.append(formattedForms.get(i)).append("\n");
        }
        // append original comments as standalone lines (to avoid losing them)
        for (int i = 0; i < commentTexts.size(); i++) {
            sb.append("; ").append(commentTexts.get(i)).append("\n");
        }
        return sb.toString();
    }

    // --- 4) Assign each comment to a bucket around the nearest formula span
    // We'll create two comment lists per formula index:
    //   leading[i]  = comments between previous span end and this span start
    //   inside[i]   = comments whose offset falls inside this span
    @SuppressWarnings("unchecked")
    List<String>[] leading = new List[spans.size()];
    @SuppressWarnings("unchecked")
    List<String>[] inside  = new List[spans.size()];
    for (int i = 0; i < spans.size(); i++) { leading[i] = new ArrayList<>(); inside[i]  = new ArrayList<>(); }

    // Build a flat array of boundaries to allow O(log n) mapping if desired.
    // Here we do a simple linear sweep since all lists are in source order.
    int commentIdx = 0;
    int spanIdx = 0;

    // Sort is unnecessary because our scan preserved source order, but just in case:
    // (no-op since we didn’t permute them)

    // Walk through comments and place them relative to span boundaries.
    while (commentIdx < commentOffsets.size()) {
        int cOff = commentOffsets.get(commentIdx);
        String cTxt = commentTexts.get(commentIdx);

        // Advance to the span that either contains cOff or starts after it.
        while (spanIdx < spans.size() && spans.get(spanIdx)[1] <= cOff) {
            spanIdx++;
        }
        if (spanIdx < spans.size()) {
            int[] span = spans.get(spanIdx);
            if (cOff < span[0]) {
                // The comment occurs before this span starts -> it's leading for this span
                leading[spanIdx].add(cTxt);
            } else if (cOff >= span[0] && cOff < span[1]) {
                // The comment lies inside this span
                inside[spanIdx].add(cTxt);
            } else {
                // (shouldn’t happen because we advanced while end <= cOff)
                // Put as leading of the next span if any, else attach to last span's inside
                if (spanIdx + 1 < spans.size()) leading[spanIdx + 1].add(cTxt);
                else inside[spans.size() - 1].add(cTxt);
            }
        } else {
            // Past the last span: attach to the last formula as "inside" (trailing overall)
            inside[spans.size() - 1].add(cTxt);
        }
        commentIdx++;
    }

    // --- 5) Emit: leading comments (as lines), the formatted formula, then inside comments (as lines)
    StringBuilder out = new StringBuilder();
    for (int i = 0; i < formattedForms.size(); i++) {
        for (String c : leading[i]) out.append("; ").append(c).append("\n");
        out.append(formattedForms.get(i)).append("\n");
        for (String c : inside[i]) out.append("; ").append(c).append("\n");
    }
    return out.toString();
}

/**
 * Scans the text to:
 *  - collect top-level S-expression spans into 'spans' as [start,endExclusive]
 *  - collect every ';' line comment as (offset,text) into commentOffsets/commentTexts.
 *
 * Rules:
 *  - Comments start at ';' and terminate at newline. Parens in comments are ignored.
 *  - Only top-level (depth transitions 0->1 ... ->0) parentheses define formula spans.
 */
private static void scanTopLevelSpansAndComments(
        String s,
        List<int[]> spans,
        List<Integer> commentOffsets,
        List<String> commentTexts
) {
    int n = s.length();
    int depth = 0;
    int i = 0;
    int currentSpanStart = -1;

    while (i < n) {
        char ch = s.charAt(i);

        // Handle line comment: read to end-of-line; ignore parens within comment
        if (ch == ';') {
            int commentStart = i;
            int j = i + 1;
            while (j < n) {
                char cj = s.charAt(j);
                if (cj == '\n' || cj == '\r') break;
                j++;
            }
            // record comment
            commentOffsets.add(commentStart);
            // strip leading ';' and optional whitespace
            String raw = s.substring(commentStart + 1, j);
            commentTexts.add(raw.stripLeading());
            // continue after newline (if present)
            i = j;
            continue;
        }

        // Newline normalization: just advance
        if (ch == '\r') { i++; continue; }

        // Depth tracking for top-level spans
        if (ch == '(') {
            if (depth == 0) {
                currentSpanStart = i;
            }
            depth++;
        } else if (ch == ')') {
            depth = Math.max(0, depth - 1);
            if (depth == 0 && currentSpanStart >= 0) {
                // endExclusive is i+1
                spans.add(new int[]{ currentSpanStart, i + 1 });
                currentSpanStart = -1;
            }
        }
        i++;
    }
}

}