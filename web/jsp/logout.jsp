<%@ page import="com.articulate.sigma.UserKBManagers" %>
<%
    String username = null;
    if (session != null) {
        Object u = session.getAttribute("user");
        if (u != null) {
            username = u.toString();
        }
    }

    if (username != null && !username.isEmpty()) {
        // Remove this user's manager from the active registry
        UserKBManagers.remove(username);
        System.out.println("logout.jsp: removed per-user KB manager for " + username);
    }

    if (session != null) {
        session.invalidate();
    }

    response.sendRedirect("KBs.jsp");
%>
