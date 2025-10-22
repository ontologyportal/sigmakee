<%@ include file="Prelude.jsp" %>
<%@ page import="com.articulate.sigma.*, java.util.List" %>
<%
    String pageName = "Editor";
    String pageString = "Editor";
    if (welcomeString == null) welcomeString = "";
%>
<%@ include file="CommonHeader.jsp" %>
<!DOCTYPE html>
<html>
<head>
  <meta charset="UTF-8">
  <title>Editor</title>
   <script src="/sigma/javascript/codemirror/codemirror.min.js"></script>
   <script src="/sigma/javascript/codemirror/placeholder.min.js"></script>
   <link rel="stylesheet" href="/sigma/javascript/codemirror/codemirror.min.css" />
   <link rel="stylesheet" href="Editor.css">
</head>
<body>
<%!
  private static String esc(String s) {
    return (s == null) ? "" : s.replace("&","&amp;").replace("<","&lt;").replace(">","&gt;");
  }
%>
<div class="card">
<form onsubmit="return false;" enctype="multipart/form-data" style="display:none;" id="uploadForm">
  <input type="file" name="kifFile" id="kifFile" accept=".kif,.tptp,.tff,.p,.fof,.cnf,.thf,.txt" required />
</form>

<script src="/sigma/javascript/editor.js"></script>
<script>
window.initialErrors = [
  <%
    List<ErrRec> errs = (List<ErrRec>) request.getAttribute("errors");
    if (errs != null) {
        boolean first = true;
        for (ErrRec e : errs) {
            if (!first) out.print(", ");
            out.print("{ line: " + e.line + ", start: " + e.start + ", end: " + e.end + " }");
            first = false;
        }
    }
  %>
];

window.initialErrorMask = [
  <%
    boolean[] mask = (boolean[]) request.getAttribute("errorMask");
    if (mask != null) {
      for (int i = 0; i < mask.length; i++) {
        if (i > 0) out.print(", ");
        out.print(mask[i] ? "true" : "false");
      }
    } %>
  ];

</script>
<%
  String errorMessage = (String) request.getAttribute("errorMessage");
  String fileName = (String) request.getAttribute("fileName");
  List<ErrRec> errors = (List<ErrRec>) request.getAttribute("errors");
  List<String> fileContent = (List<String>) request.getAttribute("fileContent");
  String codeContent = (String) request.getAttribute("codeContent");
%>
<div class="layout">
  <div>
    <div class="editor-header">
        <div class="file-menu-bar">
          <div class="dropdown" id="fileDropdown">
            <span class="dropdown-file-label" onclick="toggleDropdown()">File</span>
            <span class="dropdown-file-label" onclick="formatBuffer()">Format</span>
            <span class="dropdown-file-label" onclick="check()">Check</span>
            <div class="dropdown-content" id="dropdownContent">
              <a href="#" onclick="newFile()">New</a>
              <a href="#" onclick="downloadFile()">Download</a>
              <a href="#" onclick="triggerFileUpload()">Upload</a>
            </div>
          </div>
        </div>
        <% if (fileName != null) { %>
        <div class="file-name-display" id="file-name">Uploaded: <%= esc(fileName) %></div>
        <% } else { %>
        <div class="file-name-display" id="file-name"></div>
        <% } %>
    </div>
    <hr class="divider">
    <div class="tab-bar" id="tabBar">
        <div class="tab active" data-index="0">
            <span onclick="switchTab(0)">example.kif</span>
            <span class="close-btn" onclick="event.stopPropagation(); closeTab(0)">&times;</span>
        </div>
    </div>
    <textarea id="codeEditor" style="display: none;"><%=
      codeContent != null ? esc(codeContent) :
      (fileContent != null ? String.join("\n", fileContent) :
      esc("(=>\n  (instance ?X Man)\n  (attribute ?X Mortal))\n"))
    %></textarea>
  </div>
  <div>
    <div class="scroller msg <%= (errors == null || errors.isEmpty()) ? "success" : "errors-box" %>">
<%
    if (errorMessage != null) {
%>
      <%= esc(errorMessage) %>
<%
    } else if (errors == null || errors.isEmpty()) {
      if (fileName != null || codeContent != null) {
%>
      &#9989; No errors found.
<%
      } else {
%>
      Ready to check code.
<%
      }
    } else {
      java.util.Set<Integer> errorLines = new java.util.HashSet<>();
      for (ErrRec e : errors) {
          if (e.line >= 0)
              errorLines.add(e.line + 1);
      }
      boolean first = true;
      for (ErrRec e : errors) {
          if (!first) out.print("<br/><br/>");
          out.print(esc(e.toString()));
          first = false;
      }
    }
%>
    </div>
  </div>
</div>
<%@ include file="Postlude.jsp" %>
</body>
</html>