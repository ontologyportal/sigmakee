<%@ page import="java.util.*" %>
<%@ page import="com.articulate.sigma.tp.InferenceTestSuite" %>
<%@ page import="com.articulate.sigma.tp.InferenceTest" %>
<%@ include file="fragments/universal/Prelude.jspf" %>
<%!
    private static String esc(Object o) {
        if (o == null) return "";
        return String.valueOf(o)
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }
%>
<%
    if (!role.equalsIgnoreCase("admin")) {
        response.sendRedirect("login.jsp");
        return;
    }
    String testPath = request.getParameter("path");
    InferenceTestSuite its = (InferenceTestSuite) session.getAttribute("newITS");
    InferenceTest test = null;
    if (its != null && testPath != null) test = its.getInferenceTests().get(testPath);
%>
<html>
<head>
    <title>Inference Proof</title>
    <style>
        body{font-family:Arial,Helvetica,sans-serif;margin:24px;}
        pre{background:#f7f7f7;border:1px solid #ccc;padding:14px;border-radius:6px;white-space:pre-wrap;}
        .path{color:#777;font-size:12px;margin-bottom:12px;word-break:break-all;}
    </style>
</head>
<body>
    <h2>Inference Proof</h2>
    <% if (test == null) { %>
        <p>No test found for:</p>
        <pre><%= esc(testPath) %></pre>
    <% } else { %>
        <h3><%= esc(test.filePath) %></h3>
        <div class="path">SZS: <%= test.result == null ? "" : esc(test.result.szsStatus) %></div>
        <% if (test.result == null || test.result.proof == null || test.result.proof.isEmpty()) { %>
            <p>No proof output available yet. Run the test first.</p>
        <% } else { %>
            <pre><%
                for (String l : test.result.proof) {
                    out.println(esc(l));
                }
            %></pre>
        <% } %>
    <% } %>
</body>
</html>