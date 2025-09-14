<%@ include file="Prelude.jsp" %>
<%
    String pageName = "CheckKifFile";
    String pageString = "Check KIF File";
    if (welcomeString == null) welcomeString = "";
%>
<%@ include file="CommonHeader.jsp" %>

<table align="left" width="80%">
  <tr><td bgcolor="#AAAAAA"><img src="pixmaps/1pixel.gif" width="1" height="1" border="0"></td></tr>
</table><br>
<!DOCTYPE html>
<html>
<head>
  <meta charset="UTF-8">
  <title>Check KIF File</title>
  <style>
    body { font-family: system-ui, sans-serif; margin: 24px; }
    .card { border: 1px solid #000; padding: 16px; margin: 12px 0; border-radius: 4px; }
    .layout { display: grid; grid-template-columns: 50% 50%; gap: 16px; margin-top: 12px; }
    .scroller { max-height: 60vh; overflow: auto; border: 1px solid #ddd; border-radius: 6px; background: #fff; }
    .msg { padding: 12px; white-space: pre-line; }
    .errors-box { color: #b00020; }
    .success { color: #077d3f; }
    pre { margin: 0; background: #f8f8f8; border: 0; font-family: ui-monospace, monospace; color: #000; }
    .row { display: inline-block; padding: 0 4px; }
    .row.bad { text-decoration: red wavy underline; }
    .ln { display: inline-block; width: 4ch; text-align: right; color: #555; }
    h3 { margin: 8px 0; }

    /* Syntax highlighting */
    .comment { color: red; }
    .operator { color: darkblue; font-weight: bold; }
    .quantifier { color: darkblue; font-style: italic; }
    .variable { color: #3399ff; }
    .instance { color: green; font-weight: bold; }
  </style>
</head>
<body>
<%!
  private static String esc(String s) {
    return (s == null) ? "" : s.replace("&","&amp;").replace("<","&lt;").replace(">","&gt;");
  }

  private static String highlightKif(String s) {
    if (s == null) return "";
    String out = esc(s);

    out = out.replaceAll("(^|\\s)(;.*)", "$1<span class='comment'>$2</span>");

    out = out.replaceAll("\\b(and|or|not|=>|<=>)\\b", "<span class='operator'>$1</span>");
    out = out.replaceAll("\\b(exists|forall)\\b", "<span class='quantifier'>$1</span>");

    out = out.replaceAll("(\\?[A-Za-z0-9_-]+)", "<span class='variable'>$1</span>");

    out = out.replaceAll("\\b(instance)\\b", "<span class='instance'>$1</span>");

    return out;
  }
%>
<h3>Check KIF File For Errors</h3>
<div class="card">
  <form method="post" action="CheckKifFile" onsubmit="return checkFileSize();"
        enctype="multipart/form-data" style="display:flex; align-items:center; gap:12px; flex-wrap:wrap;">
    <label>
      <input label="Select a .kif file" type="file" name="kifFile" id="kifFile" accept=".kif" required/>
    </label>
    <label style="display:flex; align-items:center; gap:8px; margin:0;">
      <input type="checkbox" name="includeBelow" value="1"
        <%= (request.getAttribute("includeBelow") == null
            || Boolean.TRUE.equals(request.getAttribute("includeBelow"))) ? "checked" : "" %> />
      Terms Below Entity Errors
    </label>
    <button type="submit">Upload & Check</button>
  </form>
</div>
<script>
function checkFileSize() {
  const fileInput = document.getElementById("kifFile");
  if (fileInput.files.length > 0) {
    const file = fileInput.files[0];
    const maxSize = 200 * 1024; // 70 KB
    if (file.size > maxSize) {
      alert("File is too large. Maximum allowed size is 70 KB.");
      return false; // prevent form submission
    }
  }
  return true;
}
</script>
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
    java.util.Set<Integer> errorLines = new java.util.HashSet<>();
    java.util.regex.Pattern p = java.util.regex.Pattern.compile("Line\\s*#(\\d+)");
    for (String e : errors) {
        String l = e.trim();
        java.util.regex.Matcher m = p.matcher(l);
        if (m.find()) {
            try {
                errorLines.add(Integer.parseInt(m.group(1)));
            } catch (NumberFormatException ignore) {}
        }
}

%>
  <div class="layout">
    <!-- Left column: Code -->
    <div>
      <h3>Uploaded File: <%= fileName %></h3>
      <div class="scroller"><pre>
<%
      int lineNo = 1;
      for (String lineText : fileContent) {
          boolean bad = errorLines.contains(lineNo);
          out.print("<span class='row" + (bad ? " bad" : "") + "'>");
          out.print("<span class='ln'>" + String.format("%4d", lineNo) + "</span> | ");
          out.print(highlightKif(lineText));
          out.println("</span>");
          lineNo++;
      }
%>
      </pre></div>
    </div>
    <!-- Right column: Errors -->
    <div>
      <h3>Errors in <%= fileName %>:</h3>
      <div class="scroller msg <%= (errors == null || errors.isEmpty()) ? "success" : "errors-box" %>">
<%
          if (errors == null || errors.isEmpty()) {
              out.print("&#9989; No errors.");
          } else {
              boolean first = true;
              for (String e : errors) {
                  if (!first) out.print("<br/><br/>");
                  out.print(esc(e));
                  first = false;
              }
          }
%>
      </div>
    </div>
  </div>
<%
  }
%>
<%@ include file="Postlude.jsp" %>
</body>
</html>
