package com.articulate.sigma;

import javax.servlet.*;
import javax.servlet.http.*;
import javax.servlet.annotation.WebServlet;
import java.io.IOException;

@WebServlet("/FormatTPTP")
public class TPTPEditorServlet extends HttpServlet {

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        resp.setContentType("text/plain; charset=UTF-8");
        String input = req.getParameter("code");
        if (input == null || input.trim().isEmpty()) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            resp.getWriter().write("No input provided");
            return;
        }

        TPTPFormatter formatter = new TPTPFormatter();
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
}
