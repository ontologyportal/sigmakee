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

  <!-- CodeMirror 6 -->
  <script type="module">
    import { EditorView, basicSetup } from "https://cdn.jsdelivr.net/npm/@codemirror/basic-setup@0.20.0/dist/index.js";
    import { EditorState } from "https://cdn.jsdelivr.net/npm/@codemirror/state@0.20.0/dist/index.js";
    import { lineNumbers } from "https://cdn.jsdelivr.net/npm/@codemirror/view@0.20.0/dist/index.js";

    // Very simple KIF syntax highlighting
    import { HighlightStyle, tags } from "https://cdn.jsdelivr.net/npm/@codemirror/highlight@0.20.0/dist/index.js";

    const kifHighlight = HighlightStyle.define([
      { tag: tags.comment, color: "#708090" },
      { tag: tags.string, color: "#B22222" },
      { tag: tags.number, color: "#FF4500" },
      { tag: tags.keyword, color: "#0000CD", fontWeight: "bold" },
      { tag: tags.variableName, color: "#3399ff" }
    ]);

    window.startEditor = function(initialContent) {
      const parent = document.getElementById("editor");
      const state = EditorState.create({
        doc: initialContent,
        extensions: [
          basicSetup,
          lineNumbers(),
          EditorView.updateListener.of(update => {
            if (update.docChanged) {
              document.getElementById("codeContent").value = update.state.doc.toString();
            }
          }),
          kifHighlight
        ]
      });
      new EditorView({ state, parent });
    };
  </script>

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

    #editor {
      border: 1px solid #ddd;
      border-radius: 6px;
      height: 400px;
    }
  </style>
</head>
<body>

<h3>Check KIF File For Errors</h3>
<div class="card">
  <form method="post" action="CheckKifFile" onsubmit="return checkFileSize();"
        enctype="multipart/form-data" style="display:flex; align-items:center; gap:12px; flex-wrap:wrap;">
    <label>
      <input label="Select a .kif file" type="file" name="kifFile" id="kifFile" accept=".kif,.txt" required/>
    </label>
    <label style="display:flex; align-items:center; gap:8px; margin:0;">
      <input type="checkbox" name="includeBelow" value="1"
        <%= (request.getAttribute("includeBelow") == null
            || Boolean.TRUE.equals(request.getAttribute("includeBelow"))) ? "checked" : "" %> />
      Terms Below Entity Errors
    </label>
    <button type="submit">Upload</button>
  </form>
</div>

<%
  String errorMessage = (String) request.getAttribute("errorMessage");
  String fileName = (String) request.getAttribute("fileName");
  List<String> errors = (List<String>) request.getAttribute("errors");
  List<String> fileContent = (List<String>) request.getAttribute("fileContent");
  String codeContent = (String) request.getAttribute("codeContent");
  String editorContent = codeContent != null ? esc(codeContent) :
                         (fileContent != null ? String.join("\n", fileContent) : "");
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

    <div id="editor"></div>
    <input type="hidden" id="codeContent" name="codeContent" value="<%=editorContent%>">
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
function checkFileSize() {
  const fileInput = document.getElementById("kifFile");
  if (fileInput.files.length > 0) {
    const file = fileInput.files[0];
    const maxSize = 200 * 1024; // 200 KB
    if (file.size > maxSize) {
      alert("File is too large. Maximum allowed size is 200 KB.");
      return false;
    }
  }
  return true;
}

function submitCodeForCheck() {
  document.getElementById("codeContent").form.submit();
}

function downloadKifFile() {
  const codeContent = document.getElementById("codeContent").value;
  if (!codeContent.trim()) {
    alert("No content to download. Please enter some KIF code first.");
    return;
  }
  const blob = new Blob([codeContent], { type: 'text/plain' });
  const url = window.URL.createObjectURL(blob);
  const a = document.createElement('a');
  a.href = url;
  a.download = 'kiffile.kif';
  document.body.appendChild(a);
  a.click();
  document.body.removeChild(a);
  window.URL.revokeObjectURL(url);
}

// Start CodeMirror editor on page load
document.addEventListener('DOMContentLoaded', () => {
  startEditor(document.getElementById("codeContent").value);
});
</script>

</body>
</html>
