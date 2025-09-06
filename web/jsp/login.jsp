<%@ page language="java" contentType="text/html; charset=US-ASCII" import="com.articulate.sigma.*" pageEncoding="US-ASCII"%>
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
  <head>
    <meta http-equiv="Content-Type" content="text/html; charset=US-ASCII">
    <title>login</title>
  </head>
  <body>
<%
/** This code is copyright Teknowledge (c) 2003,
    Articulate Software (c) 2003-2017, Infosys (c) 2017-present.
    GNU GPL: http://www.gnu.org/copyleft/gpl.html
    Cite: Pease & Benzmüller (2013), AI Communications 26, pp79-97.
*/

String userName = request.getParameter("userName");
String password = request.getParameter("password");

// Basic null/empty guard (optional; keeps logs clean)
if (userName == null) userName = "";
if (password == null) password = "";

PasswordService ps = PasswordService.getInstance();

boolean ok = false;
String effectiveUser = null;
String role = null;

if (ps.userExists(userName)) {
    User u = User.fromDB(ps.conn, userName);
    if (u != null && ps.encrypt(password).equals(u.password)) {
        ok = true;
        effectiveUser = u.username;
        role = u.role;
    } else {
        System.err.println("Bad login attempt in login.jsp - no matching password for " + userName);
    }
} else {
    // Fallback to legacy validation path
    role = Login.validateUser(userName, password);
    if (role != null && role.length() > 0) {
        ok = true;
        effectiveUser = userName;
    } else {
        System.err.println("Bad login attempt in login.jsp - legacy path failed for " + userName);
    }
}

if (ok) {
    // Session identity
    session.setAttribute("user", effectiveUser);
    session.setAttribute("role", role);

    // Also in sibling context (as in your original code)
    ServletContext siblingContext = request.getSession().getServletContext().getContext("/sigma");
    if (siblingContext != null) {
        siblingContext.setAttribute("user", effectiveUser);
        siblingContext.setAttribute("role", role);
        System.out.println("login.jsp: Set sibling context");
    } else {
        System.out.println("login.jsp: sibling context '/sigma' not found (continuing)");
    }

    // === NEW: per-user KB manager ===
    try {
        // Reuse if already present for this user; otherwise initialize new one.
        UserKBmanager userMgr = UserKBManagers.getOrInit(effectiveUser);

        // Make the per-user manager available to JSPs/servlets:
        // many pages expect a "kbManager" in session scope.
        session.setAttribute("kbManager", userMgr);

        // Optional: log a couple of useful prefs for debugging
        System.out.println("login.jsp: Per-user KB initialized for " + effectiveUser +
                           " kbDir=" + userMgr.getPref("kbDir"));
    } catch (Exception e) {
        // If user manager init fails, abort login gracefully
        System.err.println("login.jsp: Failed to initialize per-user KB manager for " + effectiveUser);
        e.printStackTrace();
        response.sendRedirect("login.html");
        return;
    }

    System.out.println("login.jsp: Successful login for " + effectiveUser + " with role " + role);
    response.sendRedirect("KBs.jsp");
} else {
    response.sendRedirect("login.html");
}
%>
  </body>
</html>
