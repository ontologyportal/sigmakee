<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ page import="java.util.*" %>

<!DOCTYPE html>
<html>
<head>
  <meta charset="UTF-8">
  <title>Check KIF File</title>
  <style>
    body { font-family: system-ui, sans-serif; margin: 24px; }
    .card { border: 1px solid #000; padding: 16px; margin: 12px 0; border-radius: 4px; }
    .success { color:#077d3f; }
    .danger { color:#b00020; white-space: pre; }
    pre { background: #f8f8f8; padding: 8px; border: 1px solid #ddd; }
  </style>
</head>
<body>
<h1>Check KIF File</h1>

<div class="card">
  <form method="post" action="CheckKifFile" enctype="multipart/form-data">
    <label>Select a .kif file to check:</label><br/>
    <input type="file" name="kifFile" accept=".kif" required/>
    <button type="submit">Upload & Check</button>
  </form>
</div>

<%
    String errorMessage = (String) request.getAttribute("errorMessage");
    String fileName = (String) request.getAttribute("fileName");
    List<String> errors = (List<String>) request.getAttribute("errors");
    List<String> fileContent = (List<String>) request.getAttribute("fileContent");

    if (errorMessage != null) {
%>
    <div class="danger"><%= errorMessage %></div>
<%
    } else if (fileName != null) {
        if (errors != null && errors.isEmpty()) {
%>
    <div class="success">✅ File <%= fileName %> passed syntax check.</div>
<%
        } else if (errors != null) {
%>
    <div class="danger"><b>Errors in <%= fileName %>:</b><br/>
    <%
        for (String e : errors) {
            out.println(e + "<br/>");
        }
    %>
    </div>
<%
        }
        if (fileContent != null) {
%>
    <h3>Uploaded File: <%= fileName %></h3>
    <pre>
<%
        int lineNo = 1;
        for (String line : fileContent) {
            out.println(String.format("%4d | %s", lineNo++, line));
        }
%>
    </pre>

<%
        }
    }
%>

</body>
</html>