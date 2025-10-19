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
 * Supports "format" and "check" actions.
 * Determines which checker/formatter to use based on the file name or content.
 */
@WebServlet("/EditorServlet")
@MultipartConfig(
        fileSizeThreshold = 1024,   // buffer to disk after 1 KB
        maxFileSize = 200 * 1024,   // 200 KB max file
        maxRequestSize = 220 * 1024 // 220 KB total
)
public class EditorServlet extends HttpServlet {

    /** Utility: read stream as UTF-8 string */
    private static String readUtf8(InputStream in) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buf = new byte[8192];
        int n;
        while ((n = in.read(buf)) != -1) out.write(buf, 0, n);
        return new String(out.toByteArray(), StandardCharsets.UTF_8);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws IOException, ServletException {

        String mode = req.getParameter("mode");           // "format" or "check"
        String code = req.getParameter("code");           // for TPTP editor
        String codeContent = req.getParameter("codeContent"); // for KIF editor
        String action = req.getParameter("action");
        String fileName = req.getParameter("fileName");
        String text = null;

        if (code != null && !code.isBlank())
            text = code;
        else if (codeContent != null && !codeContent.isBlank())
            text = codeContent;

        // Fallback if uploaded file
        if (text == null && req.getContentType() != null &&
                req.getContentType().toLowerCase(Locale.ROOT).startsWith("multipart/")) {

            Part filePart = req.getPart("kifFile");
            if (filePart != null && filePart.getSize() > 0) {
                fileName = filePart.getSubmittedFileName();
                text = readUtf8(filePart.getInputStream());
            }
        }

        if (text == null || text.trim().isEmpty()) {
            req.setAttribute("errorMessage", "No content provided for checking or formatting.");
            req.getRequestDispatcher("/Editor.jsp").forward(req, resp);
            return;
        }

        if (fileName == null)
            fileName = "Untitled.kif"; // default if not specified

        boolean isTptp = fileName.toLowerCase().endsWith(".tptp");
        List<ErrRec> errors = Collections.emptyList();
        List<String> lines = Arrays.asList(text.split("\\R", -1));

        // -------- Handle format request ----------
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

        // -------- Handle check request ----------
        try {
            if (isTptp) {
                errors = TPTPChecker.check(text, "(web-editor)");
            } else {
                Future<List<ErrRec>> future = KifCheckWorker.submit(text);
                errors = future.get();
            }
        } catch (Exception e) {
            req.setAttribute("errorMessage", "Error while checking: " + e.getMessage());
        }

        // Highlight mask
        boolean[] errorMask = new boolean[lines.size()];
        if (errors != null) {
            for (ErrRec e : errors) {
                int ln = e.line + 1;
                if (ln >= 1 && ln <= errorMask.length) {
                    errorMask[ln - 1] = true;
                }
            }
        }

        req.setAttribute("errorMask", errorMask);
        req.setAttribute("errors", errors);
        req.setAttribute("fileName", fileName);
        req.setAttribute("fileContent", lines);
        req.setAttribute("codeContent", text);

        req.getRequestDispatcher("/Editor.jsp").forward(req, resp);
    }

    /** KIF autoformatter (migrated from KifFileCheckServlet) */
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
