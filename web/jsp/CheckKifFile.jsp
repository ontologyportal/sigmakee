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
   <script src="/sigma/javascript/codemirror/codemirror.min.js"></script>
   <script src="/sigma/javascript/codemirror/placeholder.min.js"></script>
   <link rel="stylesheet" href="/sigma/javascript/codemirror/codemirror.min.css" />
  <style>
    body {font-family: system-ui, sans-serif; margin: 24px;}
    .card {border: 1px solid #000; padding: 16px; margin: 12px 0; border-radius: 4px;}
    .layout {display: grid; grid-template-columns: 1fr 1fr; gap: 16px; margin-top: 12px; align-items: stretch;}
    .layout > div {display: flex; flex-direction: column;}
    .scroller { height: 59vh; overflow: auto; border: 1px solid #ddd; border-radius: 6px; background: #fff;}
    .msg { padding: 12px; white-space: pre-line;}
    .errors-box { color: #b00020;}
    .success { color: #077d3f;}
    h3 {margin: 8px 0;}
    .check-button {background: #007bff; color: white; border: none; padding: 8px 16px; border-radius: 4px; cursor: pointer; font-size: 14px;}
    .check-button:hover {background: #0056b3;}
    .download-button {background: #28a745; color: white; border: none; padding: 8px 16px; border-radius: 4px; cursor: pointer; font-size: 14px;}
    .download-button:hover { background: #1e7e34; }
    .upload-button {background: #20c997; color: white; border: none; padding: 8px 16px; border-radius: 4px; cursor: pointer; font-size: 14px;}
    .upload-button:hover { background: #1aa179; }
    .CodeMirror {border: 1px solid #ddd; border-radius: 6px; height: 60vh; font-family: ui-monospace, monospace; font-size: 14px; line-height: 1.4;}
    .CodeMirror-gutters {background: #f0f0f0; border-right: 1px solid #ddd;}
    .CodeMirror-linenumber {color: #666; padding: 0 8px; min-width: 2.5em;}
    .CodeMirror-line-error {background-color: #ffebee; border-bottom: 2px wavy #d32f2f; position: relative;}
    .CodeMirror .error-line {background-color: #ffebee !important;}
    .CodeMirror .error-text {background-color: #ffebee; border-bottom: 2px wavy #d32f2f;}
    .cm-kif-comment { color: red; }
    .cm-kif-operator { color: #0000CD; font-weight: bold; }
    .cm-kif-quantifier { color: #0000CD; font-style: italic; }
    .cm-kif-variable { color: #3399ff; }
    .cm-kif-instance { color: #228B22; font-weight: bold; }
    .cm-kif-string { color: #B22222; }
    .cm-kif-number { color: #FF4500; }
    .editor-header {display: flex; justify-content: space-between; align-items: center; margin-bottom: 8px;}
    .button-group { display: flex; gap: 8px; }
    .file-name-display {font-family: monospace; font-size: 12px; color: #666; margin-top: 4px;}
  </style>
</head>
<body>
<%!
  private static String esc(String s) {
    return (s == null) ? "" : s.replace("&","&amp;").replace("<","&lt;").replace(">","&gt;");
  }
%>
<div class="card">
<div class="language-select">
  <label>
    <input type="radio" name="language" value="kif" checked 
           onchange="switchLanguage(this.value)">
    SUO-KIF
  </label>
  <label style="margin-left:16px;">
    <input type="radio" name="language" value="tptp" 
           onchange="switchLanguage(this.value)">
    TPTP
  </label>
</div>
<!-- Hidden form for file upload -->
<form method="post" action="CheckKifFile" onsubmit="return checkFileSize();"
      enctype="multipart/form-data" style="display:none;" id="uploadForm">
  <input type="file" name="kifFile" id="kifFile" accept=".kif,.txt" required />
</form>
<script>
  const fileInput = document.getElementById('kifFile');
  const fileNameSpan = document.getElementById('file-name');
  const uploadForm = document.getElementById('uploadForm');
  fileInput.addEventListener('change', () => {
    if (fileInput.files.length > 0) {
      if (fileNameSpan) {
        fileNameSpan.textContent = 'Uploaded: ' + fileInput.files[0].name;
      }
      uploadForm.submit();
    } else {
      if (fileNameSpan) {
        fileNameSpan.textContent = '';
      }
    }
  });

CodeMirror.defineMode("kif", function() {
  return {
    token: function(stream, state) {
      if (stream.match(/;.*/))
        return "kif-comment";
      if (stream.match(/"(?:[^"\\]|\\.)*"/))
        return "kif-string";
      if (stream.match(/\b\d+(?:\.\d+)?\b/))
        return "kif-number";
      if (stream.match(/\?[A-Za-z0-9_-]+/))
        return "kif-variable";
      if (stream.match(/\b(?:exists|forall)\b/))
        return "kif-quantifier";
      if (stream.match(/(?:\band\b|\bor\b|\bnot\b|=>|<=>)/))
        return "kif-operator";
      if (stream.match(/\b(?:instance|subclass|domain|range)\b/))
        return "kif-instance";
      if (stream.match(/[()]/))
        return "bracket";
      if (stream.match(/\s+/))
        return null;
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
    placeholder: "Enter your KIF code here or upload a file using the Upload button...",
    indentUnit: 2,
    tabSize: 2,
    lineWrapping: true,
    autoCloseBrackets: true,
    matchBrackets: true
  });
  highlightErrorLines();
}

  const errors = [
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

let errorMarks = [];

function highlightErrorLines() {
  for (const mark of errorMarks)
    mark.clear();
  errorMarks = [];
  if (errors && errors.length > 0 && codeEditor) {
    for (const err of errors) {
      const from = { line: err.line, ch: err.start };
      const to = { line: err.line, ch: err.end };
      const mark = codeEditor.markText(from, to, { className: "error-text" });
      errorMarks.push(mark);
    }
  }
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
    codeEditor.eachLine(function(lineHandle) {
      codeEditor.removeLineClass(lineHandle, "gutter", "error-line-gutter");
    });

    for (let i = 0; i < errorMask.length; i++)
      if (errorMask[i])
        codeEditor.addLineClass(i, "gutter", "error-line-gutter");
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

function triggerFileUpload() {
  document.getElementById('kifFile').click();
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
    alert("No content to download. Please enter some code first.");
    return;
  }

  // Ask the user for a filename
  const fileName = prompt("Enter a filename:");
  if (!fileName) return; // user cancelled

  // Create a blob with the code content
  const blob = new Blob([codeContent], { type: 'text/plain' });
  const url = window.URL.createObjectURL(blob);

  // Create a temporary download link
  const a = document.createElement('a');
  a.href = url;
  a.download = fileName;  // Suggests filename to browser
  document.body.appendChild(a);

  // Trigger click → browser should show Save As dialog
  a.click();

  // Clean up
  document.body.removeChild(a);
  window.URL.revokeObjectURL(url);
}

</script>
<%
  String errorMessage = (String) request.getAttribute("errorMessage");
  String fileName = (String) request.getAttribute("fileName");
  List<ErrRec> errors = (List<ErrRec>) request.getAttribute("errors");
  List<String> fileContent = (List<String>) request.getAttribute("fileContent");
  String codeContent = (String) request.getAttribute("codeContent");
%>
<div class="layout">
  <!-- Left column: Code -->
  <div>
    <div class="editor-header">
      <h3>SUO-KIF Code Editor</h3>
        <% if (fileName != null) { %>
        <div class="file-name-display" id="file-name">Uploaded: <%= esc(fileName) %></div>
        <% } else { %>
        <div class="file-name-display" id="file-name"></div>
        <% } %>
      <div class="button-group">
        <button type="button" class="upload-button" onclick="triggerFileUpload()">Upload</button>
        <button type="button" class="download-button" onclick="downloadKifFile()">Download</button>
        <button type="button" class="check-button" onclick="formatBuffer()">Format</button>
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

      for (ErrRec e : errors) {
          if (e.line >= 0) {
              errorLines.add(e.line + 1); // ErrRec.line is 0-based, convert to 1-based if needed
          }
      }

      boolean first = true;
      for (ErrRec e : errors) {
          if (!first) out.print("<br/><br/>");
          out.print(esc(e.toString()));   // or esc(e.msg) if you only want the message
          first = false;
      }
    }
%>

    </div>
  </div>
</div>
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

<%@ include file="Postlude.jsp" %>
</body>
</html>