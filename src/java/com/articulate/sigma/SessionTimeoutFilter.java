package com.articulate.sigma;

import javax.servlet.*;
import javax.servlet.http.*;
import java.io.IOException;

public class SessionTimeoutFilter implements Filter {

    @Override
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest request = (HttpServletRequest) req;
        HttpServletResponse response = (HttpServletResponse) res;

        HttpSession session = request.getSession(false);
        String uri = request.getRequestURI();

        boolean loggedIn =
            session != null &&
            session.getAttribute("user") != null;

        boolean loginRequest =
            uri.endsWith("login.jsp") ||
            uri.endsWith("login.html") ||
            uri.endsWith("Register.jsp");

        chain.doFilter(req, res);
    }
}
