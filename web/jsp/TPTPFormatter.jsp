<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ page import="com.articulate.sigma.TPTPFormatter" %>
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
            white-space: pre-wrap;
            overflow-x: auto;
            font-size: 15px;
        }
        .formatted-output pre {
            margin: 0;                /* ✨ remove extra white space above and below */
            padding: 0;
            white-space: pre-wrap;    /* keep line breaks and wrapping */
        }
        .error {
            color: #b00020;
            white-space: pre-wrap;
        }
    </style>
</head>
<body>

<h1>TPTP Formula Formatter</h1>

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
        TPTPFormatter formatter = new TPTPFormatter();
        String formatted = formatter.formatTptpText(tptpInput, "(web-form)");

        if (formatted != null) {
            // ✅ Strip leading spaces on the first line before displaying
            String[] lines = formatted.split("\\R", -1);
            if (lines.length > 0) {
                lines[0] = lines[0].replaceFirst("^\\s+", "");
            }
            formatted = String.join(System.lineSeparator(), lines);
            System.out.println(formatted);
%>
    <h2>Formatted Output:</h2>
    <div class="formatted-output">
        <pre><%= formatted %></pre>
    </div>
<%
        } else {
%>
    <div class="formatted-output">
        <pre><%= formatted %></pre>
    </div>
<%
        }
    }
%>


</body>
</html>
