<%@ page language="java" contentType="text/html; charset=US-ASCII" pageEncoding="US-ASCII" %>
<%
    if (session != null) {
        session.invalidate();
    }
    response.sendRedirect("KBs.jsp");
%>
