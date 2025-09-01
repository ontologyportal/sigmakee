/** This code is copyright Articulate Software (c) 2003.  Some portions
copyright Teknowledge (c) 2003 and reused under the terms of the GNU license.
This software is released under the GNU Public License <http://www.gnu.org/copyleft/gpl.html>.
Users of this code also consent, by use of this code, to credit Articulate Software
and Teknowledge in any writings, briefings, publications, presentations, or
other representations of any software which incorporates, builds on, or uses this
code.  Please cite the following article in any publication with references:

Pease, A., (2003). The Sigma Ontology Development Environment,
in Working Notes of the IJCAI-2003 Workshop on Ontology and Distributed Systems,
August 9, Acapulco, Mexico.
*/

import com.articulate.sigma.KifFileChecker;
import javax.servlet.annotation.MultipartConfig;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;
import javax.servlet.ServletException;
import javax.servlet.http.Part;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** *************************************************************
 * Servlet that validates uploaded SUO-KIF files and forwards the results
 * to a JSP for display.
 */
@WebServlet("/CheckKifFile")
@MultipartConfig
public class KifFileCheckServlet extends HttpServlet {
    
    /** *************************************************************
     * Reads an entire InputStream into a UTF-8 String.
     *
     * @param in input stream to read (the caller is responsible for closing it)
     * @return the UTF-8 decoded contents of the stream
     * @throws IOException if an I/O error occurs while reading
     */
    private static String readUtf8(InputStream in) throws IOException {

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buf = new byte[8192];
        int n;
        while ((n = in.read(buf)) != -1) out.write(buf, 0, n);
        return new String(out.toByteArray(), StandardCharsets.UTF_8);
    }

    /** *************************************************************
     * Handles POST requests that upload a SUO-KIF file for validation.
     * 
     * @param request HTTP request carrying the uploaded file and parameters
     * @param response HTTP response
     * @throws IOException if reading input or forwarding fails
     * @throws ServletException if request processing or forwarding fails
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        boolean includeBelow = request.getParameter("includeBelow") != null;
        request.setAttribute("includeBelow", includeBelow);
        List<String> errors = java.util.Collections.emptyList();
        List<String> lines  = null;
        String fileName     = null;
        if (request.getContentType() != null
                && request.getContentType().toLowerCase(java.util.Locale.ROOT).startsWith("multipart/")) {
            Part filePart = request.getPart("kifFile");
            if (filePart == null || filePart.getSize() == 0) {
                request.setAttribute("errorMessage", "No file uploaded.");
                request.getRequestDispatcher("/CheckKifFile.jsp").forward(request, response);
                return;
            }
            fileName = filePart.getSubmittedFileName();
            if (fileName == null || !fileName.toLowerCase(java.util.Locale.ROOT).endsWith(".kif")) {
                request.setAttribute("errorMessage", "Only .kif files are allowed.");
                request.getRequestDispatcher("/CheckKifFile.jsp").forward(request, response);
                return;
            }
            String text;
            try (InputStream in = filePart.getInputStream()) {
                text = readUtf8(in);
            }
            lines = java.util.Arrays.asList(text.split("\\R", -1));
            try (Reader r = new StringReader(text)) {
                errors = KifFileChecker.check(r, fileName, includeBelow);
            } catch (Exception e) {
                request.setAttribute("errorMessage", "Error while checking uploaded file: " + e.getMessage());
            }
        } else {
            request.setAttribute("errorMessage", "Please upload a .kif file.");
        }
        boolean[] errorMask = new boolean[lines != null ? lines.size() : 0];
        if (errors != null) {
            java.util.regex.Pattern p = java.util.regex.Pattern.compile("^Line\\s+(\\d+):");
            for (String e : errors) {
                java.util.regex.Matcher m = p.matcher(e);
                if (m.find()) {
                    try {
                        int ln = Integer.parseInt(m.group(1));
                        if (ln >= 1 && ln <= errorMask.length) errorMask[ln - 1] = true;
                    } catch (NumberFormatException ignore) {
                    }
                }
            }
        }
        request.setAttribute("errorMask", errorMask);
        request.setAttribute("fileName", fileName);
        request.setAttribute("fileContent", lines);
        request.setAttribute("errors", errors);
        request.getRequestDispatcher("/CheckKifFile.jsp").forward(request, response);
    }
}
