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
    .layout {display: flex; flex-direction: column; grid-template-columns: 1fr 1fr; gap: 16px; margin-top: 12px; align-items: stretch;}
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
    .file-menu-bar {
    display: flex;
    justify-content: flex-start;
    margin-bottom: 8px;
    }

    .dropdown {
    position: relative;
    display: inline-block;
    }

    .dropdown-content a {
    display: block;
    padding: 8px 12px;
    color: #333;
    text-decoration: none;
    font-size: 14px;
    }

    .dropdown-content a:hover {
    background-color: #f0f0f0;
    }

    .dropdown-content {
        display: none;
        position: absolute;
        background-color: #fff;
        min-width: 140px;
        box-shadow: 0 4px 8px rgba(0,0,0,0.1);
        border-radius: 4px;
        z-index: 100;
        margin-top: 4px;
    }
    .tab-bar {
    display: flex;
    align-items: center;
    background: #f8f8f8;
    border-bottom: 1px solid #ccc;
    border-radius: 6px 6px 0 0;
    padding: 4px 8px;
    margin-bottom: 4px;
    }

    .tab {
    padding: 6px 12px;
    border: 1px solid #ccc;
    border-bottom: none;
    border-radius: 6px 6px 0 0;
    background: #e9e9e9;
    margin-right: 4px;
    cursor: pointer;
    user-select: none;
    font-size: 13px;
    }

    .tab .close-btn {
        margin-left: 6px;
        color: #666;
        font-weight: bold;
        cursor: pointer;
        transition: color 0.2s ease;
        padding: 0 4px; 
    }
    
    .tab .close-btn:hover {
    color: #c00;
    }

    .tab.active {
    background: white;
    border-bottom: 1px solid white;
    font-weight: bold;
    }

    .tab:hover {
    background: #f0f0f0;
    }

    .add-tab {
    background: #007bff;
    color: white;
    border: none;
    border-radius: 4px;
    padding: 4px 10px;
    cursor: pointer;
    font-size: 14px;
    margin-left: auto;
    }

    .add-tab:hover {
    background: #0056b3;
    }

  </style>
</head>
<body>
<%!
  private static String esc(String s) {
    return (s == null) ? "" : s.replace("&","&amp;").replace("<","&lt;").replace(">","&gt;");
  }
%>

<div class="card">
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
    placeholder: "Enter your TPTP code here or upload a file using the Upload button...",
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

function downloadTPTPFile() {
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
<script>
let codeEditors = []; // store each tab’s content
let activeTab = 0;

function addTab(name = null) {
  if (!name) {
    const inputName = prompt("Enter a name for the new file:", "Untitled.kif");
    if (!inputName) return;
    name = inputName.trim();
  }

  const tabBar = document.getElementById("tabBar");
  const newIndex = codeEditors.length;

  // Create tab element
  const tab = document.createElement("div");
  tab.className = "tab";
  tab.setAttribute("data-index", newIndex);

  const label = document.createElement("span");
  label.textContent = name;
  label.onclick = (e) => {
    if (e.target.classList.contains("close-btn")) return;
    switchTab(newIndex);
  };

  const closeBtn = document.createElement("span");
  closeBtn.textContent = 'x';
  closeBtn.className = "close-btn";
  closeBtn.onclick = (e) => {
    e.stopPropagation();
    closeTab(newIndex);
  };

  tab.appendChild(label);
  tab.appendChild(closeBtn);
  tabBar.appendChild(tab);
  if (codeEditors.length > activeTab)
    codeEditors[activeTab] = codeEditor.getValue();
  codeEditors.push("");
  switchTab(newIndex, true);
  toggleDropdown();
}

function switchTab(index, isNew = false) {
  const tabs = document.querySelectorAll(".tab");
  tabs.forEach((tab, i) => tab.classList.toggle("active", i === index));
  if (codeEditors.length > activeTab)
    codeEditors[activeTab] = codeEditor.getValue();
  activeTab = index;
  if (isNew) codeEditor.setValue("");
  else if (codeEditors[index] !== undefined)
    codeEditor.setValue(codeEditors[index]);
}

document.addEventListener("DOMContentLoaded", function() {
  initializeCodeMirror();
  codeEditors.push("");
});

</script>
<script>
async function formatBuffer() {
  const codeContent = codeEditor.getValue();
  if (!codeContent.trim()) {
    alert("No content to format.");
    return;
  }
  try {
    const response = await fetch('Editor', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/x-www-form-urlencoded; charset=UTF-8'
      },
      body: 'code=' + encodeURIComponent(codeContent)
    });
    const formatted = await response.text();
    if (response.ok) {
      codeEditor.setValue(formatted);
    } else {
      alert("Formatting failed:\n" + formatted);
    }
  } catch (e) {
    console.error("Error formatting TPTP:", e);
    alert("An error occurred while formatting.");
  }
}
</script>
<script>
function checkTPTP() {
  const codeContent = codeEditor.getValue();
  if (!codeContent.trim()) {
    alert("Nothing to check.");
    return;
  }

  const form = document.createElement('form');
  form.method = 'post';
  form.action = 'FormatTPTP';
  form.style.display = 'none';

  const codeInput = document.createElement('input');
  codeInput.type = 'hidden';
  codeInput.name = 'code';
  codeInput.value = codeContent;
  form.appendChild(codeInput);

  const modeInput = document.createElement('input');
  modeInput.type = 'hidden';
  modeInput.name = 'mode';
  modeInput.value = 'check';
  form.appendChild(modeInput);

  document.body.appendChild(form);
  form.submit();
}
</script>
<script>
function toggleDropdown() {
  const content = document.getElementById("dropdownContent");
  const arrow = document.getElementById("fileArrow");
  const isVisible = content.style.display === "block";
  content.style.display = isVisible ? "none" : "block";
  arrow.textContent = isVisible ? "›" : "▾";
}

document.addEventListener("click", (e) => {
  const dropdown = document.getElementById("fileDropdown");
  const content = document.getElementById("dropdownContent");
  const arrow = document.getElementById("fileArrow");
  if (!dropdown.contains(e.target)) {
    content.style.display = "none";
    arrow.textContent = "›";
  }
});

function closeTab(index) {
  const tabs = document.querySelectorAll(".tab");
  if (index < 0 || index >= tabs.length) return;
  tabs[index].remove();
  codeEditors.splice(index, 1);
  if (activeTab === index) {
    if (codeEditors.length > 0) {
      const newActive = Math.max(0, index - 1);
      switchTab(newActive);
    } else {
      codeEditor.setValue("");
      activeTab = 0;
    }
  }
  const updatedTabs = document.querySelectorAll(".tab");
  updatedTabs.forEach((tab, i) => tab.setAttribute("data-index", i));
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
        <div class="file-menu-bar">
            <div class="dropdown" id="fileDropdown">
                <span class="dropdown-label" onclick="toggleDropdown()">File</span>
                <div class="dropdown-content" id="dropdownContent">
                <a href="#" onclick="addTab()">New</a>
                <a href="#" onclick="triggerFileUpload()">Upload</a>
                <a href="#" onclick="downloadTPTPFile()">Download</a>
                <a href="#" onclick="formatBuffer()">Format</a>
                <a href="#" onclick="checkTPTP()">Check</a>
                </div>
            </div>
            </div>
      <h3>Editor</h3>
        <% if (fileName != null) { %>
        <div class="file-name-display" id="file-name">Uploaded: <%= esc(fileName) %></div>
        <% } else { %>
        <div class="file-name-display" id="file-name"></div>
        <% } %>
    </div>
    <!-- Tab bar -->
    <div class="tab-bar" id="tabBar">
        <div class="tab active" data-index="0">
            <span onclick="switchTab(0)">example.kif</span>
            <span class="close-btn" onclick="event.stopPropagation(); closeTab(0)">&times;</span>
        </div>
    </div>
    <textarea id="codeEditor" style="display: none;"><%=
      codeContent != null ? esc(codeContent) :
      (fileContent != null ? String.join("\n", fileContent) : "")
    %></textarea>
  </div>

  <!-- Right column: Errors -->
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
      Ready to check TPTP code. Enter code in the editor or upload a file.
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

function refreshErrorHighlighting() {
  if (codeEditor) {
    setTimeout(highlightErrorLines, 100);
  }
}
</script>

<%@ include file="Postlude.jsp" %>
</body>
</html>