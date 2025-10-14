package com.articulate.sigma;

import javax.servlet.*;
import javax.servlet.http.*;
import javax.servlet.annotation.WebServlet;
import java.io.IOException;
import java.util.List;

@WebServlet("/FormatTPTP")
public class TPTPEditorServlet extends HttpServlet {

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        String mode = req.getParameter("mode"); // "format" or "check"
        String input = req.getParameter("code");

        if (input == null || input.trim().isEmpty()) {
            req.setAttribute("errorMessage", "No input provided");
            RequestDispatcher dispatcher = req.getRequestDispatcher("/TPTPEditor.jsp");
            dispatcher.forward(req, resp);
            return;
        }

        // Handle format mode (default)
        if (mode == null || mode.equals("format")) {
            resp.setContentType("text/plain; charset=UTF-8");

            TPTPChecker formatter = new TPTPChecker();
            String formatted = formatter.formatTptpText(input, "(web-editor)");

            if (formatted == null) {
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                resp.getWriter().write("Failed to format input.");
            } else {
                // Strip leading spaces on first line
                String[] lines = formatted.split("\\R", -1);
                if (lines.length > 0) {
                    lines[0] = lines[0].replaceFirst("^\\s+", "");
                }
                formatted = String.join(System.lineSeparator(), lines);
                resp.getWriter().write(formatted);
            }
        }

        // Handle check mode
        else if (mode.equals("check")) {
            // ✅ Run TPTPChecker and attach results to request
            List<ErrRec> errors = TPTPChecker.check(input, "(web-editor)");
            req.setAttribute("errors", errors);
            req.setAttribute("codeContent", input);

            // Forward back to the JSP to display error messages / highlights
            RequestDispatcher dispatcher = req.getRequestDispatcher("/TPTPEditor.jsp");
            dispatcher.forward(req, resp);
        }
    }
}
