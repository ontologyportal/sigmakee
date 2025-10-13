<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ page import="com.articulate.sigma.Formula" %>
<%@ page import="com.articulate.sigma.trans.TPTPutil" %>
<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <title>TPTP Formula Formatter</title>
    <style>
        body {
            font-family: system-ui, sans-serif;
            margin: 24px;
            max-width: 900px;
        }
        h1 {
            margin-bottom: 0.5em;
        }
        form {
            margin-bottom: 2em;
        }
        textarea {
            width: 100%;
            height: 120px;
            font-family: monospace;
            font-size: 14px;
            padding: 8px;
        }
        .button-row {
            margin-top: 10px;
        }
        .formatted-output {
            border: 1px solid #ccc;
            background: #f9f9f9;
            padding: 16px;
            white-space: normal;
            overflow-x: auto;
            font-size: 15px;
        }
    </style>
</head>
<body>

<h1>TPTP Formula HTML Formatter</h1>

<form method="post">
    <label for="tptpInput">Enter a TPTP Formula:</label><br>
    <textarea id="tptpInput" name="tptpInput"><%= request.getParameter("tptpInput") != null ? request.getParameter("tptpInput") : "" %></textarea>
    <div class="button-row">
        <input type="submit" value="Format Formula" />
    </div>
</form>

<%
    String tptpInput = request.getParameter("tptpInput");
    if (tptpInput != null && !tptpInput.trim().isEmpty()) {
        Formula f = new Formula();
        f.theTptpFormulas.add(tptpInput);

        String html = TPTPutil.htmlTPTPFormat(
            f,
            "http://sigma.ontologyportal.org:4040/sigma?kb=SUMO&term=",
            false // set true for traditional logic symbols like ∀, ∃
        );
%>
    <h2>Formatted Output:</h2>
    <div class="formatted-output">
        <%= html %>
    </div>
<%
    }
%>

</body>
</html>
