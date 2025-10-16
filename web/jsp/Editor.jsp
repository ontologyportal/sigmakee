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
<form method="post" action="Editor" onsubmit="return checkFileSize();"
      enctype="multipart/form-data" style="display:none;" id="uploadForm">
  <input type="file" name="kifFile" id="kifFile" accept=".kif,.txt" required />
</form>

<script>
  const fileInput = document.getElementById('kifFile');
  const fileNameSpan = document.getElementById('file-name');
  const uploadForm = document.getElementById('uploadForm');
  let errorMarks = [];
  let codeEditors = [];
  let activeTab = 0;

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
    const maxSize = 200 * 1024;
    if (file.size > maxSize) {
      alert("File is too large. Maximum allowed size is 200 KB.");
      return false;
    }
  }
  return true;
}

function triggerFileUpload() {
  document.getElementById('kifFile').click();
}

function downloadTPTPFile() {
  const codeContent = codeEditor.getValue();
  if (!codeContent.trim()) {
    alert("No content to download. Please enter some code first.");
    return;
  }
  const fileName = prompt("Enter a filename:");
  if (!fileName) return; 
  const blob = new Blob([codeContent], { type: 'text/plain' });
  const url = window.URL.createObjectURL(blob);
  const a = document.createElement('a');
  a.href = url;
  a.download = fileName;
  document.body.appendChild(a);
  a.click();
  document.body.removeChild(a);
  window.URL.revokeObjectURL(url);
}

function addTab(name = null) {
  if (!name) {
    const inputName = prompt("Enter a name for the new file:", "Untitled.kif");
    if (!inputName) return;
    name = inputName.trim();
  }
  const tabBar = document.getElementById("tabBar");
  const newIndex = codeEditors.length;
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

async function checkTPTP() {
  const codeContent = codeEditor.getValue();
  if (!codeContent.trim()) return alert("Nothing to check.");
  const activeTabElem = document.querySelector(".tab.active span:not(.close-btn)");
  const fileName = activeTabElem ? activeTabElem.textContent.trim() : "Untitled.kif";
  try {
    const response = await fetch("EditorServlet", {
      method: "POST",
      headers: {
        "Content-Type": "application/x-www-form-urlencoded; charset=UTF-8"
      },
      body:
        "mode=check" +
        "&fileName=" + encodeURIComponent(fileName) +
        "&code=" + encodeURIComponent(codeContent)
    });
    const html = await response.text();
    const parser = new DOMParser();
    const doc = parser.parseFromString(html, "text/html");
    const newErrorBox = doc.querySelector(".scroller.msg");
    const currentErrorBox = document.querySelector(".scroller.msg");
    if (newErrorBox && currentErrorBox)
      currentErrorBox.replaceWith(newErrorBox);
    refreshErrorHighlighting();
  } catch (err) {
    alert("Error checking file: " + err);
  }
}

function toggleDropdown() {
  const content = document.getElementById("dropdownContent");
  const isVisible = content.style.display === "block";
  content.style.display = isVisible ? "none" : "block";
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
  const tabName = tabs[index].querySelector("span").textContent;
  const confirmClose = confirm(`Are you sure you want to close "${tabName}"?`);
  if (!confirmClose) return;
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

function refreshErrorHighlighting() {
  if (codeEditor)
    setTimeout(highlightErrorLines, 100);
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
  <div>
    <div class="editor-header">
        <div class="file-menu-bar">
          <div class="dropdown" id="fileDropdown">
            <span class="dropdown-file-label" onclick="toggleDropdown()">File</span>
            <span class="dropdown-file-label" onclick="formatBuffer()">Format</span>
            <span class="dropdown-file-label" onclick="checkTPTP()">Check</span>
            <div class="dropdown-content" id="dropdownContent">
              <a href="#" onclick="addTab()">New</a>
              <a href="#" onclick="triggerFileUpload()">Upload</a>
              <a href="#" onclick="downloadTPTPFile()">Download</a>
              <a href="#" onclick="formatBuffer()">Format</a>
              <a href="#" onclick="checkTPTP()">Check</a>
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
      (fileContent != null ? String.join("\n", fileContent) : "")
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
      Ready to check TPTP code. Enter code in the editor or upload a file.
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