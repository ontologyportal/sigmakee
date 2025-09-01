<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ page import="java.util.*,java.util.regex.*" %>

<%!
  // Simple HTML escaper for rendering safely
  private static String esc(String s){
    if (s == null) return "";
    return s.replace("&","&amp;").replace("<","&lt;").replace(">","&gt;");
  }
%>

<!DOCTYPE html>
<html>
<head>
  <meta charset="UTF-8">
  <title>Check KIF File</title>
  <style>
    body { font-family: system-ui, sans-serif; margin: 24px; }
    .card { border: 1px solid #000; padding: 16px; margin: 12px 0; border-radius: 4px; }
    .layout { display: grid; grid-template-columns: 1fr 1fr; gap: 16px; margin-top: 12px; }
    .scroller { max-height: 60vh; overflow: auto; border: 1px solid #ddd; border-radius: 6px; background: #fff; }
    .msg { padding: 12px; white-space: pre-wrap; }
    .errors-box { color:#b00020; }
    .success { color:#077d3f; }
    pre {
      margin: 0; background: #f8f8f8; padding: 8px;
      border: 0; font-family: ui-monospace, SFMono-Regular, Menlo, Consolas, "Liberation Mono", monospace;
      color: #000;
    }
    .row { display: inline-block; padding: 0 4px; }
    .row.bad { background: #ffd6d6; }
    .ln  { display:inline-block; width: 4ch; text-align: right; color:#555; }
    h3 { margin: 8px 0; }
  </style>
</head>
<body>
<flex>
<div class="card">
  <form method="post" action="CheckKifFile" enctype="multipart/form-data">
    <label>Select a .kif file to check:</label><br/>
    <input type="file" name="kifFile" accept=".kif" required/>
    <div style="margin-top:8px">
      <label>
        <input type="checkbox" name="includeBelow"
               value="1"
               <%= (request.getAttribute("includeBelow") == null
                    || Boolean.TRUE.equals(request.getAttribute("includeBelow")))
                    ? "checked" : "" %> />
        Terms Below Entity Errors
      </label>
    </div>
    <button type="submit">Upload & Check</button>
  </form>
</div>
</flex>
<%
  String errorMessage = (String) request.getAttribute("errorMessage");
  String fileName = (String) request.getAttribute("fileName");
  List<String> errors = (List<String>) request.getAttribute("errors");
  List<String> fileContent = (List<String>) request.getAttribute("fileContent");

  if (errorMessage != null) {
%>
  <div class="scroller msg errors-box"><%= esc(errorMessage) %></div>
<%
  } else if (fileName != null) {
    // Collect error line numbers from messages like "Line N: ..."
    Set<Integer> errorLines = new HashSet<>();
    if (errors != null) {
      Pattern p = Pattern.compile("^Line\\s+(\\d+):");
      for (String e : errors) {
        Matcher m = p.matcher(e);
        if (m.find()) {
          try { errorLines.add(Integer.parseInt(m.group(1))); } catch (NumberFormatException ignore) {}
        }
      }
    }
    String panelClass = (errors == null || errors.isEmpty())
            ? "scroller msg success"
            : "scroller msg errors-box";
%>
  <div>
    <div>
      <h3>Uploaded File: <%= fileName %></h3>
      <div class="scroller">
        <pre>
<%
          int lineNo = 1;
          for (String line : fileContent) {
            boolean bad = errorLines.contains(lineNo);
            out.print("<span class='row" + (bad ? " bad" : "") + "'>");
            out.print("<span class='ln'>");
            out.print(String.format("%4d", lineNo));
            out.print("</span> | ");
            out.print(esc(line));
            out.println("</span>");
            lineNo++;
          }
%>
        </pre>
      </div>
    </div>
    <div>
      <h3>Errors in <%= fileName %>:</h3>
      <div class="<%= panelClass %>">
<%
        if (errors == null || errors.isEmpty()) {
          out.print("✅ No errors.");
        } else {
          for (String e : errors) out.println(esc(e) + "\n");
        }
%>
      </div>
    </div>
  </div>
<%
  }
%>

</body>
</html>
