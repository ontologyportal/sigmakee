/** This code is copyright Articulate Software (c) 2003.
 Some portions copyright Teknowledge (c) 2003 and reused under
 the terms of the GNU license. This software is released under
 the GNU Public License <http://www.gnu.org/copyleft/gpl.html>.
 Users of this code also consent, by use of this code, to credit
 Articulate Software and Teknowledge in any writings, briefings,
 publications, presentations, or other representations of any
 software which incorporates, builds on, or uses this code.
 Please cite the following article in any publication with references:

 Pease, A., (2003). The Sigma Ontology Development Environment,
 in Working Notes of the IJCAI-2003 Workshop on Ontology and
 Distributed Systems, August 9, Acapulco, Mexico.
 */

package com.articulate.sigma;

import javax.servlet.annotation.MultipartConfig;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;
import javax.servlet.ServletException;
import javax.servlet.http.Part;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.*;
import java.util.regex.*;


@WebServlet("/CheckKifFile")
@MultipartConfig(
        fileSizeThreshold = 1024,         // buffer to disk after 1 KB
        maxFileSize = 190 * 1024,          // 70 KB per file
        maxRequestSize = 200 * 1024        // 80 KB total request size
)

/**
 * Servlet that validates uploaded SUO-KIF files and forwards the results
 * to a JSP for display.
 */
public class KifFileCheckServlet extends HttpServlet {

    /** *************************************************************
     * Reads an entire InputStream into a UTF-8 String.
     */
    private static String readUtf8(InputStream in) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buf = new byte[8192];
        int n;
        while ((n = in.read(buf)) != -1) out.write(buf, 0, n);
        return new String(out.toByteArray(), StandardCharsets.UTF_8);
    }

    /** *************************************************************
     * Handles POST requests that upload a SUO-KIF file for validation
     * or submit code content directly from the editor.
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws IOException, ServletException {

        List<String> errors = Collections.emptyList();
        List<String> lines = null;
        String fileName = null;
        String text = null;
        boolean isCodeSubmission = false;

        // Check if this is a direct code submission from the editor
        String codeContent = request.getParameter("codeContent");
        if (codeContent != null && !codeContent.trim().isEmpty()) {
            // Handle direct code submission
            text = codeContent;
            fileName = "Editor Content";
            isCodeSubmission = true;

            // Split into lines for JSP display
            lines = Arrays.asList(text.split("\\R", -1));

            // Validate the code content
            try {
                Future<List<String>> future = KifCheckWorker.submit(text);
                errors = future.get(); // block until job is processed
            } catch (Exception e) {
                request.setAttribute("errorMessage",
                        "Error while checking code content: " + e.getMessage());
            }
        }
        // Handle file upload
        else if (request.getContentType() != null &&
                request.getContentType().toLowerCase(Locale.ROOT).startsWith("multipart/")) {

            Part filePart = request.getPart("kifFile");
            if (filePart == null || filePart.getSize() == 0) {
                request.setAttribute("errorMessage", "No file uploaded.");
                request.getRequestDispatcher("/CheckKifFile.jsp").forward(request, response);
                return;
            }

            fileName = filePart.getSubmittedFileName();
            if (fileName == null || (!fileName.toLowerCase(Locale.ROOT).endsWith(".kif") && !fileName.toLowerCase(Locale.ROOT).endsWith(".txt"))) {
                request.setAttribute("errorMessage", "Only .kif & .txt files are allowed.");
                request.getRequestDispatcher("/CheckKifFile.jsp").forward(request, response);
                return;
            }

            try (InputStream in = filePart.getInputStream()) {
                text = readUtf8(in);
            }

            // Split into lines for JSP display
            lines = Arrays.asList(text.split("\\R", -1));

            // Enqueue request for sequential processing ---
            try {
                Future<List<String>> future = KifCheckWorker.submit(text);
                errors = future.get(); // block until job is processed
            } catch (Exception e) {
                request.setAttribute("errorMessage",
                        "Error while checking uploaded file: " + e.getMessage());
            }
        } else {
            // Neither file upload nor code content submission
            request.setAttribute("errorMessage", "Please upload a .kif or .txt file or enter code in the editor.");
            request.getRequestDispatcher("/CheckKifFile.jsp").forward(request, response);
            return;
        }

        // Highlight lines with errors
        boolean[] errorMask = new boolean[lines != null ? lines.size() : 0];
        if (errors != null) {
            for (String e : errors) {
                Pattern p = Pattern.compile("#(\\d+):");
                Matcher m = p.matcher(e);
                if (m.find()) {
                    try {
                        int ln = Integer.parseInt(m.group(1));
                        if (ln >= 1 && ln <= errorMask.length) {
                            errorMask[ln - 1] = true;
                        }
                    } catch (NumberFormatException ignore) { }
                }
            }
        }

        // Set attributes for JSP
        request.setAttribute("errorMask", errorMask);
        request.setAttribute("fileName", fileName);
        request.setAttribute("fileContent", lines);
        request.setAttribute("errors", errors);

        // If this was a code submission, also set the codeContent attribute
        // so the JSP can preserve the editor content
        if (isCodeSubmission) {
            request.setAttribute("codeContent", text);
        }

        request.getRequestDispatcher("/CheckKifFile.jsp").forward(request, response);
    }
}