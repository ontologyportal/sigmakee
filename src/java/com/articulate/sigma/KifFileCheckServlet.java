package com.articulate.sigma;

import javax.servlet.annotation.MultipartConfig;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;
import javax.servlet.ServletException;
import javax.servlet.http.Part;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Validates uploaded SUO-KIF files and forwards results to the JSP.
 */
@WebServlet("/CheckKifFile")
@MultipartConfig
public class KifFileCheckServlet extends HttpServlet {
    boolean debug = true;
    // ---------- utils ----------

    private static String readUtf8(InputStream in) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buf = new byte[8192];
        int n;
        while ((n = in.read(buf)) != -1) out.write(buf, 0, n);
        return new String(out.toByteArray(), StandardCharsets.UTF_8);
    }

    // A form plus where it starts in the original file (1-based)
    private static final class FormSpan {
        final String text;
        final int startLine;
        FormSpan(String t, int s) { text = t; startLine = s; }
    }

    /**
     * Split KIF text into balanced s-expressions, tracking the starting line
     * of each form. Handles ; comments and quoted strings.
     */
    private static List<FormSpan> splitKifFormsWithLines(String text) {
        List<FormSpan> forms = new ArrayList<>();
        String[] lines = text.replace("\r\n","\n").replace("\r","\n").split("\n", -1);

        StringBuilder cur = new StringBuilder();
        int depth = 0;
        boolean inStr = false;
        int formStart = 1;

        for (int ln = 0; ln < lines.length; ln++) {
            String rawLine = lines[ln];
            String line = rawLine;

            // strip ; comments (outside strings)
            StringBuilder sb = new StringBuilder();
            boolean cut = false;
            for (int i = 0; i < line.length(); i++) {
                char c = line.charAt(i);
                if (c == '"' && (i == 0 || line.charAt(i-1) != '\\')) {
                    inStr = !inStr;
                }
                if (!inStr && c == ';') { cut = true; break; }
                sb.append(c);
            }
            if (cut) line = sb.toString();

            // track depth (outside strings)
            for (int i = 0; i < line.length(); i++) {
                char c = line.charAt(i);
                if (c == '"' && (i == 0 || line.charAt(i-1) != '\\')) {
                    inStr = !inStr;
                } else if (!inStr) {
                    if (c == '(') depth++;
                    else if (c == ')') depth--;
                }
            }

            if (cur.length() == 0) formStart = ln + 1;
            if (cur.length() > 0) cur.append('\n');
            cur.append(rawLine);

            if (depth == 0 && !inStr && cur.toString().trim().length() > 0) {
                forms.add(new FormSpan(cur.toString(), formStart));
                cur.setLength(0);
            }
        }
        if (cur.toString().trim().length() > 0) {
            forms.add(new FormSpan(cur.toString(), formStart));
        }
        return forms;
    }

    private static String adjustErrorLineNumbers(String msg, int offset) {
        if (offset <= 1) return msg;
        Pattern p = Pattern.compile("^Line\\s+(\\d+):", Pattern.MULTILINE);
        Matcher m = p.matcher(msg);
        StringBuffer sb = new StringBuffer();
        while (m.find()) {
            int local = Integer.parseInt(m.group(1));
            int global = local + (offset - 1);
            m.appendReplacement(sb, "Line " + global + ":");
        }
        m.appendTail(sb);
        return sb.toString();
    }

    // ---------- servlet ----------

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws IOException, ServletException {

        // read UI toggle
        final boolean includeBelow = request.getParameter("includeBelow") != null;
        request.setAttribute("includeBelow", includeBelow);

        List<String> lines = null;
        List<String> errorsOut = new ArrayList<>();
        String fileName = null;

        try {
            // Validate multipart + file
            String ct = request.getContentType();
            if (ct == null || !ct.toLowerCase(Locale.ROOT).startsWith("multipart/")) {
                request.setAttribute("errorMessage", "Please upload a .kif file.");
                forward(request, response);
                return;
            }

            Part filePart = request.getPart("kifFile");
            if (filePart == null || filePart.getSize() == 0) {
                request.setAttribute("errorMessage", "No file uploaded.");
                forward(request, response);
                return;
            }

            fileName = filePart.getSubmittedFileName();
            if (fileName == null || !fileName.toLowerCase(Locale.ROOT).endsWith(".kif")) {
                request.setAttribute("errorMessage", "Only .kif files are allowed.");
                forward(request, response);
                return;
            }

            // Read text (UTF-8) and normalize newlines
            String text;
            try (InputStream in = filePart.getInputStream()) {
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                byte[] buf = new byte[8192];
                int n;
                while ((n = in.read(buf)) != -1) out.write(buf, 0, n);
                text = new String(out.toByteArray(), java.nio.charset.StandardCharsets.UTF_8);
            }
            // Strip UTF-8 BOM if present
            if (!text.isEmpty() && text.charAt(0) == '\uFEFF') {
                text = text.substring(1);
            }
            text = text.replace("\r\n", "\n").replace("\r", "\n");
            lines = Arrays.asList(text.split("\n", -1));

            // Get KB
            KBmanager mgr = KBmanager.getMgr();
            String kbName = mgr.getPref("sumokbname");
            if (kbName == null) kbName = "SUMO";
            KB kb = mgr.getKB(kbName);
            if (kb == null) {
                request.setAttribute("errorMessage", "KB not available: " + kbName);
                forward(request, response);
                return;
            }
            
            if (debug) System.out.println("\n\n*****************************************************************" + 
                                            "\nKifFileCheckerServlet.doPost() Running KifFileChecker.check() on file: " + fileName);
            errorsOut = KifFileChecker.check(kb, lines, includeBelow);
            if (debug) System.out.println("\n\n*****************************************************************" + 
                                            "\nKifFileCheckServlet.java doPost() errors: " + errorsOut);

            // Build error mask for UI (highlight lines)
            boolean[] errorMask = new boolean[lines.size()];
            Pattern linePat = Pattern.compile("^Line\\s+(\\d+):");
            for (String e : errorsOut) {
                Matcher m = linePat.matcher(e);
                if (m.find()) {
                    try {
                        int ln = Integer.parseInt(m.group(1));
                        if (ln >= 1 && ln <= errorMask.length) errorMask[ln - 1] = true;
                    } catch (NumberFormatException ignore) { /* no-op */ }
                }
            }
            request.setAttribute("errorMask", errorMask);

        } catch (Exception ex) {
            request.setAttribute("errorMessage", "Error while checking uploaded file: " + ex.getMessage());
        }

        // Always populate these so the JSP can render whatever we have
        request.setAttribute("fileName", fileName);
        request.setAttribute("fileContent", lines);
        request.setAttribute("errors", errorsOut);

        forward(request, response);
    }


    private void forward(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.getRequestDispatcher("/CheckKifFile.jsp").forward(request, response);
    }
}
