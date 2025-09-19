<%@ include file="Prelude.jsp" %>
<%
    String pageName = "CheckKifFile";
    String pageString = "Check KIF File";
    if (welcomeString == null) welcomeString = "";
%>
<%@ include file="CommonHeader.jsp" %>

<!DOCTYPE html>
<html>
<head>
  <meta charset="UTF-8">
  <title>Check KIF File</title>

  <!-- CodeMirror -->
  <script src="https://cdnjs.cloudflare.com/ajax/libs/codemirror/5.65.2/codemirror.min.js"></script>
  <link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/codemirror/5.65.2/codemirror.min.css">
  <link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/codemirror/5.65.2/theme/default.min.css">
  <script src="https://cdnjs.cloudflare.com/ajax/libs/codemirror/5.65.2/addon/display/placeholder.min.js"></script>

  <!-- ✅ All CSS merged here -->
  <style>
    body { font-family: system-ui, sans-serif; margin: 24px; }
    .card { border: 1px solid #000; padding: 16px; margin: 12px 0; border-radius: 4px; }
    .layout { display: grid; grid-template-columns: 1fr 1fr; gap: 16px; margin-top: 12px; }
    .scroller { max-height: 60vh; overflow: auto; border: 1px solid #ddd; border-radius: 6px; background: #fff; }
    .msg { padding: 12px; white-space: pre-line; }
    .errors-box { color: #b00020; }
    .success { color: #077d3f; }
    h3 { margin: 8px 0; }

    /* Buttons */
    .check-button {
      background: #007bff;
      color: white;
      border: none;
      padding: 8px 16px;
      border-radius: 4px;
      cursor: pointer;
      font-size: 14px;
    }
    .check-button:hover { background: #0056b3; }

    .download-button {
      background: #28a745;
      color: white;
      border: none;
      padding: 8px 16px;
      border-radius: 4px;
      cursor: pointer;
      font-size: 14px;
    }
    .download-button:hover { background: #1e7e34; }

    /* CodeMirror customization */
    .CodeMirror {
      border: 1px solid #ddd;
      border-radius: 6px;
      height: 400px;
      font-family: ui-monospace, monospace;
      font-size: 14px;
      line-height: 1.4;
    }

    .CodeMirror-gutters {
      background: #f0f0f0;
      border-right: 1px solid #ddd;
    }

    .CodeMirror-linenumber {
      color: #666;
      padding: 0 8px;
      min-width: 2.5em;
    }

    /* Error line highlighting */
    .CodeMirror-line-error {
      background-color: #ffebee;
      border-bottom: 2px wavy #d32f2f;
      position: relative;
    }

    .CodeMirror .error-line {
      background-color: #ffebee !important;
    }

    .CodeMirror .error-line-gutter {
      background-color: #ffcdd2 !important;
      color: #d32f2f !important;
      font-weight: bold;
    }

    /* KIF syntax highlighting for CodeMirror */
    .cm-kif-comment { color: red; }
    .cm-kif-operator { color: #0000CD; font-weight: bold; }
    .cm-kif-quantifier { color: #0000CD; font-style: italic; }
    .cm-kif-variable { color: #3399ff; }
    .cm-kif-instance { color: #228B22; font-weight: bold; }
    .cm-kif-string { color: #B22222; }
    .cm-kif-number { color: #FF4500; }

    .editor-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      margin-bottom: 8px;
    }
    .button-group { display: flex; gap: 8px; }
  </style>
</head>
<body>

<%!
  private static String esc(String s) {
    return (s == null) ? "" : s.replace("&","&amp;").replace("<","&lt;").replace(">","&gt;");
  }
%>
<h3>Check KIF File For Errors</h3>
<div class="card">
    <form method="post" action="CheckKifFile" onsubmit="return checkFileSize();"
      enctype="multipart/form-data" style="display:flex; align-items:center; gap:12px; flex-wrap:wrap;">
  <label>
    <input label="Select a .kif file" type="file" name="kifFile" id="kifFile" accept=".kif,.txt" required/>
  </label>
</form>
</div>
<script>
document.getElementById("kifFile").addEventListener("change", function() {
  if (this.files.length > 0) {
    if (checkFileSize()) {
      this.form.submit();
    }
  }
});

// Define KIF mode for CodeMirror
CodeMirror.defineMode("kif", function() {
  return {
    token: function(stream, state) {
      // Handle comments
      if (stream.match(/;.*/)) {
        return "kif-comment";
      }

      // Handle strings
      if (stream.match(/"(?:[^"\\]|\\.)*"/)) {
        return "kif-string";
      }

      // Handle numbers
      if (stream.match(/\b\d+(?:\.\d+)?\b/)) {
        return "kif-number";
      }

      // Handle variables
      if (stream.match(/\?[A-Za-z0-9_-]+/)) {
        return "kif-variable";
      }

      // Handle quantifiers
      if (stream.match(/\b(?:exists|forall)\b/)) {
        return "kif-quantifier";
      }

      // Handle operators
      if (stream.match(/(?:\band\b|\bor\b|\bnot\b|=>|<=>)/)) {
        return "kif-operator";
      }

      // Handle instance keywords
      if (stream.match(/\b(?:instance|subclass|domain|range)\b/)) {
        return "kif-instance";
      }

      // Handle parentheses
      if (stream.match(/[()]/)) {
        return "bracket";
      }

      // Skip whitespace
      if (stream.match(/\s+/)) {
        return null;
      }

      // Default: advance one character
      stream.next();
      return null;
    }
  };
});

let codeEditor;

function initializeCodeMirror() {
  const textarea = document.getElementById("codeEditor");
  codeEditor = CodeMirror.fromTextArea(textarea, {
    mode: "kif",
    lineNumbers: true,
    theme: "default",
    placeholder: "Enter your KIF code here or upload a file above...",
    indentUnit: 2,
    tabSize: 2,
    lineWrapping: true,
    autoCloseBrackets: true,
    matchBrackets: true
  });

  // Apply error highlighting if errorMask is available
  highlightErrorLines();
}

function highlightErrorLines() {
  // Get error mask from JSP
  const errorMask = [<%
    boolean[] errorMask = (boolean[]) request.getAttribute("errorMask");
    if (errorMask != null) {
      for (int i = 0; i < errorMask.length; i++) {
        if (i > 0) out.print(", ");
        out.print(errorMask[i] ? "true" : "false");
      }
    }
  %>];

  if (errorMask && errorMask.length > 0 && codeEditor) {
    // Clear any existing error highlighting
    codeEditor.eachLine(function(lineHandle) {
      codeEditor.removeLineClass(lineHandle, "background", "error-line");
      codeEditor.removeLineClass(lineHandle, "gutter", "error-line-gutter");
    });

    // Apply error highlighting to lines with errors
    for (let i = 0; i < errorMask.length; i++) {
      if (errorMask[i]) {
        codeEditor.addLineClass(i, "background", "error-line");
        codeEditor.addLineClass(i, "gutter", "error-line-gutter");
      }
    }
  }
}

function checkFileSize() {
  const fileInput = document.getElementById("kifFile");
  if (fileInput.files.length > 0) {
    const file = fileInput.files[0];
    const maxSize = 200 * 1024; // 200 KB
    if (file.size > maxSize) {
      alert("File is too large. Maximum allowed size is 200 KB.");
      return false; // prevent form submission
    }
  }
  return true;
}

function submitCodeForCheck() {
  const codeContent = codeEditor.getValue();

  // Create a form dynamically to submit the code content
  const form = document.createElement('form');
  form.method = 'post';
  form.action = 'CheckKifFile';

  const codeInput = document.createElement('input');
  codeInput.type = 'hidden';
  codeInput.name = 'codeContent';
  codeInput.value = codeContent;

  form.appendChild(codeInput);
  document.body.appendChild(form);
  form.submit();
}

function downloadKifFile() {
  const codeContent = codeEditor.getValue();
  if (!codeContent.trim()) {
    alert("No content to download. Please enter some KIF code first.");
    return;
  }

  // Create a blob with the code content
  const blob = new Blob([codeContent], { type: 'text/plain' });
  const url = window.URL.createObjectURL(blob);

  // Create a temporary download link
  const a = document.createElement('a');
  a.href = url;
  a.download = 'kif_file.kif';
  document.body.appendChild(a);
  a.click();

  // Clean up
  document.body.removeChild(a);
  window.URL.revokeObjectURL(url);
}
</script>
<%
  String errorMessage = (String) request.getAttribute("errorMessage");
  String fileName = (String) request.getAttribute("fileName");
  List<String> errors = (List<String>) request.getAttribute("errors");
  List<String> fileContent = (List<String>) request.getAttribute("fileContent");
  String codeContent = (String) request.getAttribute("codeContent");

  // Always show the layout now
%>
<div class="layout">
  <!-- Left column: Code -->
  <div>
    <div class="editor-header">
      <h3>KIF Code Editor</h3>
      <div class="button-group">
        <button type="button" class="download-button" onclick="downloadKifFile()">Download</button>
        <button type="button" class="check-button" onclick="submitCodeForCheck()">Check</button>
      </div>
    </div>
    <textarea id="codeEditor" style="display: none;"><%=
      codeContent != null ? esc(codeContent) :
      (fileContent != null ? String.join("\n", fileContent) : "")
    %></textarea>
  </div>

  <!-- Right column: Errors -->
  <div>
    <h3>Validation Results</h3>
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
      Ready to check KIF code. Enter code in the editor or upload a file.
<%
      }
    } else {
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
<%@ include file="Postlude.jsp" %>
<script>
// Initialize CodeMirror on page load
document.addEventListener('DOMContentLoaded', function() {
  initializeCodeMirror();
});

// Re-highlight errors when content changes (for file uploads)
function refreshErrorHighlighting() {
  if (codeEditor) {
    // Small delay to ensure content is updated
    setTimeout(highlightErrorLines, 100);
  }
}
</script>
</body>
</html>