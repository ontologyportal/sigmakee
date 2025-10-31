const fileInput = document.getElementById('kifFile');
const fileNameSpan = document.getElementById('file-name');
const uploadForm = document.getElementById('uploadForm');

// codeEditors is a 2-D array, each array within contains the filename and contents.
// [0] = filename (string)
// [1] = file contents (string)
let codeEditors = [];
let activeTab = 0;
let errors = window.initialErrors || [];
let errorMarks = window.initialErrorMask || [];
let codeEditor;

// ------------------------------------------------------------------
// Event Listeners & Initialization

async function loadKifColors() {
  const response = await fetch('/sigma/modes/kif.xml');
  const xmlText = await response.text();
  const parser = new DOMParser();
  const xml = parser.parseFromString(xmlText, 'text/xml');
  const colorMap = {
    'KEYWORD1': 'keyword1',
    'KEYWORD2': 'keyword2',
    'KEYWORD3': 'keyword3',
    'KEYWORD4': 'keyword4',
    'COMMENT1': 'comment',
    'LITERAL1': 'string',
    'LITERAL2': 'literal',
    'FUNCTION': 'function',
  };
  const keywords = {};
  for (const type in colorMap)
    keywords[type] = [...xml.getElementsByTagName(type)].map(n => n.textContent);
  return { colorMap, keywords };
}

async function defineModeFromXML(modeName, xmlPath) {
  const response = await fetch(xmlPath);
  const xmlText = await response.text();
  const parser = new DOMParser();
  const xml = parser.parseFromString(xmlText, 'text/xml');
  const tagTypes = [
    'KEYWORD1', 'KEYWORD2', 'KEYWORD3', 'KEYWORD4',
    'LITERAL1', 'LITERAL2', 'COMMENT1', 'FUNCTION',
    'OPERATOR', 'MARKUP', 'NULL'
  ];
  const keywords = {};
  for (const tag of tagTypes)
    keywords[tag] = [...xml.getElementsByTagName(tag)].map(n => n.textContent);
  const commentStart = xml.querySelector('PROPERTY[NAME="lineComment"]')?.getAttribute('VALUE') || ';';
  const classMap = {
    'KEYWORD1': 'keyword1',
    'KEYWORD2': 'keyword2',
    'KEYWORD3': 'keyword3',
    'KEYWORD4': 'keyword4',
    'COMMENT1': 'comment',
    'LITERAL1': 'string',
    'LITERAL2': 'string',
    'FUNCTION': 'function',
    'OPERATOR': 'operator',
    'MARKUP': 'markup',
    'NULL': 'null'
  };

  CodeMirror.defineMode(modeName, function() {
  return {
    token: function(stream, state) {
      if (commentStart && stream.match(new RegExp(`${commentStart}.*`))) return classMap['COMMENT1'];
      if (stream.match(/"(?:[^"\\]|\\.)*"/)) return classMap['LITERAL1'];
      if (stream.match(/'(?:[^'\\]|\\.)*'/)) return classMap['LITERAL1'];
      if (stream.match(/\[[^\]]*\]/)) return classMap['LITERAL1'];
      if (stream.match(/\d+(?:\.\d+)?/)) {
        const cur = stream.current();
        const start = stream.start;
        const end = stream.pos;
        const prev = start > 0 ? stream.string.charAt(start - 1) : '';
        const next = end < stream.string.length ? stream.string.charAt(end) : '';
        const validBefore = !prev || /[\s(]/.test(prev);
        const validAfter = !next || /[\s)]/.test(next);
        const noLetterNearby = !(/[A-Za-z]/.test(prev) || /[A-Za-z]/.test(next));
        if (validBefore && validAfter && noLetterNearby)
          return "number";
        stream.backUp(cur.length);
      }
      if (stream.match(/\?[A-Za-z0-9_-]+/)) return classMap['KEYWORD4'];
      for (const type in keywords)
        for (const word of keywords[type])
          if (stream.match(new RegExp(`\\b${word}\\b`))) return classMap[type];
      if (stream.match(/[()]/)) return classMap['MARKUP'];
      if (stream.match(/[\[\]{}]/)) return "bracket";
      stream.next();
      return null;
    }
  };
});
}

fileInput.addEventListener('change', () => {
  if (fileInput.files.length === 0) {
    console.warn("No file selected.");
    return;
  }
  const file = fileInput.files[0];
  const reader = new FileReader();
  reader.onload = (e) => {
    const contents = e.target.result;
    openFileInNewTab(file.name, contents);
  };
  reader.onerror = (e) => {
    console.error("Error reading file:", e);
    alert("Error reading file.");
  };
  reader.readAsText(file);
  toggleDropdown();
});

document.addEventListener("DOMContentLoaded", async function() {
  await defineModeFromXML("kif", "LanguagesXML/kif.xml");
  await defineModeFromXML("tptp", "LanguagesXML/tptp.xml");
  initializeCodeMirror();
  if (window.initialErrors || window.initialErrorMask)
    highlightErrors(window.initialErrors || [], window.initialErrorMask || []);
  const exampleTPTP = `% Example TPTP file
tff(mortal_rule, axiom,
    (![X]: (man(X) => mortal(X)))
).

tff(socrates_fact, axiom,
    man(socrates)
).

tff(query, conjecture,
    mortal(socrates)
).`;
  const exampleTHF = `% Example THF (Typed Higher-Order Form)
thf(example_thf, conjecture,
    ( ![P: $i > $o, X: $i]: ( P @ X ) )
).`;
  const exampleTFF = `% Example TFF (Typed First-Order Form)
tff(example_tff, axiom,
    ![X: $i]: ( man(X) => mortal(X) )
).`;
  const exampleFOF = `% Example FOF (First-Order Form)
fof(example_fof, axiom,
    ![X]: ( man(X) => mortal(X) )
).`;
  const exampleCNF = `% Example CNF (Clausal Normal Form)
cnf(example_cnf, axiom,
    ( ~man(X) | mortal(X) )
).`;
  openFileInNewTab("example.tptp", exampleTPTP);
  openFileInNewTab("example.thf", exampleTHF);
  openFileInNewTab("example.tff", exampleTFF);
  openFileInNewTab("example.fof", exampleFOF);
  openFileInNewTab("example.cnf", exampleCNF);
  switchTab(0);
});


// ------------------------------------------------------------------
// Editor Functions

function getActiveMode() {
  if (["tptp", "thf", "tff", "fof", "cnf"].includes(getActiveFileName().split('.').pop().toLowerCase()))
    return 'tptp';
  return 'kif';
}

function getActiveFileName() {
  return codeEditors[activeTab]?.[0] || "Untitled.kif";
}

function initializeCodeMirror() {
  const textarea = document.getElementById("codeEditor");
  codeEditor = CodeMirror.fromTextArea(textarea, {
    mode: getActiveMode(),
    lineNumbers: true,
    theme: "default",
    indentUnit: 2,
    tabSize: 2,
    lineWrapping: true,
    autoCloseBrackets: true,
    matchBrackets: true
  });
}

function renderErrorBox(errors = [], message = null) {
  const box = document.querySelector(".scroller.msg");
  if (!box) return;
  if (message) {
    box.classList.remove("success");
    box.classList.add("errors-box");
    box.textContent = message;
    return;
  }
  if (!errors.length) {
    box.classList.remove("errors-box");
    box.classList.add("success");
    box.textContent = "✅ No errors found.";
  } else {
    box.classList.remove("success");
    box.classList.add("errors-box");
    box.textContent = errors.map(e => {
      const sev = Number(e.type) === 1 ? "WARNING" : "ERROR";
      const lineHuman = (Number(e.line) ?? 0) + 1;
      const colHuman  = (Number(e.start) ?? 0) + 1;
      return `${sev} ${e.file ? e.file : "(buffer)"}:${lineHuman}:${colHuman}\n${e.msg || ""}`;
    }).join("\n\n");
  }
}

function highlightErrors(errors = [], mask = []) {
  for (const m of errorMarks) m.clear();
  errorMarks = [];
  const editor = codeEditors[activeTab]?.cm || codeEditor;
  if (!editor) return;
  editor.eachLine(h => {
    editor.removeLineClass(h, "gutter", "error-line-gutter");
    editor.removeLineClass(h, "gutter", "warning-line-gutter");
  });
  if (Array.isArray(mask)) {
    mask.forEach((isErr, i) => {
      if (isErr) editor.addLineClass(i, "gutter", "error-line-gutter");
    });
  }
  if (!Array.isArray(errors) || errors.length === 0) return;
  const lastLine = Math.max(0, editor.lineCount() - 1);
  for (const e of errors) {
    const line = Math.max(0, Math.min(Number(e.line ?? 0), lastLine));
    const from = { line, ch: e.start };
    const to   = { line, ch: e.end };
    const cls  = (Number(e.type) === 1) ? "warning-text" : "error-text";
    const mark = editor.markText(from, to, { className: cls, title: e.msg || "" });
    errorMarks.push(mark);
    editor.addLineClass(line, "gutter", cls === "warning-text"
      ? "warning-line-gutter"
      : "error-line-gutter");
  }
}

// ------------------------------------------------------------------
// Menu options

function toggleDropdown() {
  const content = document.getElementById("dropdownContent");
  const isVisible = content.style.display === "block";
  content.style.display = isVisible ? "none" : "block";
}

function downloadFile() {
  if (!codeEditor || typeof codeEditor.getValue !== "function") {
    alert("No active editor instance found.");
    return;
  }
  const codeContent = codeEditor.getValue();
  if (!codeContent.trim()) {
    alert("No content to download. Please enter some code first.");
    return;
  }
  const fileName = getActiveFileName();
  const blob = new Blob([codeContent], { type: "text/plain" });
  const url = window.URL.createObjectURL(blob);
  const a = document.createElement("a");
  a.href = url;
  a.download = fileName;
  document.body.appendChild(a);
  a.click();
  document.body.removeChild(a);
  window.URL.revokeObjectURL(url);
  toggleDropdown();
}

function triggerFileUpload() {
  document.getElementById('kifFile').click();
  toggleDropdown();
}

async function check() {
  const codeContent = codeEditor.getValue();
  if (!codeContent.trim()) return alert("Nothing to check.");
  const fileName = getActiveFileName();
  try {
    const res = await fetch("EditorServlet", {
      method: "POST",
      headers: {
        "Content-Type": "application/x-www-form-urlencoded; charset=UTF-8",
        "Accept": "application/json"
      },
      body: new URLSearchParams({
        mode: "check",
        fileName,
        code: codeContent
      }).toString()
    });
    const isJson = (res.headers.get("content-type") || "").includes("application/json");
    const payload = isJson ? await res.json() : JSON.parse(await res.text());
    const serverErrors = Array.isArray(payload.errors) ? payload.errors : [];
    const mask = Array.isArray(payload.errorMask) ? payload.errorMask : [];
    renderErrorBox(serverErrors, payload.message);
    highlightErrors(serverErrors, mask);
  } catch (err) {
    console.error("Check failed:", err);
    alert("Error checking file: " + err);
  }
}


async function formatBuffer() {
  check();
  const codeContent = codeEditor.getValue();
  if (!codeContent.trim()) {
    alert("No content to format.");
    return;
  }
  try {
    const response = await fetch("EditorServlet", {
      method: "POST",
      headers: {
        "Content-Type": "application/x-www-form-urlencoded; charset=UTF-8",
      },
      body:
        "mode=format" +
        "&fileName=" + encodeURIComponent(getActiveFileName()) +
        "&code=" + encodeURIComponent(codeContent),
    });
    const formatted = await response.text();
    const oldContent = codeEditor.getValue();
    if (response.ok && formatted && formatted.trim().length > 0) {
      codeEditor.setValue(formatted);
    } else {
      alert("Formatting failed or produced no output. Keeping original content.");
      codeEditor.setValue(oldContent);
    }
  } catch (e) {
    console.error("Error formatting file:", e);
    alert("An error occurred while formatting.");
  }
}

// ------------------------------------------------------------------
// Tabs Management

function addTab(fileName = "untitled.kif") {
  const tabBar = document.getElementById("tabBar");
  const index = codeEditors.length;

  const tab = document.createElement("div");
  tab.className = "tab";
  tab.dataset.index = index;

  const label = document.createElement("span");
  label.textContent = fileName;
  label.onclick = () => switchTab(index);

  const closeBtn = document.createElement("span");
  closeBtn.textContent = "×";
  closeBtn.className = "close-btn";
  closeBtn.onclick = (e) => {
    e.stopPropagation();
    closeTab(index);
  };

  tab.append(label, closeBtn);
  tabBar.appendChild(tab);
  codeEditors.push([fileName, ""]);
}

function switchTab(index, isNew = false) {
  const tabs = document.querySelectorAll(".tab");
  tabs.forEach((t, i) => t.classList.toggle("active", i === index));

  // save current buffer before switching
  if (codeEditors[activeTab]) codeEditors[activeTab][1] = codeEditor.getValue();

  activeTab = index;
  const newContent = codeEditors[index]?.[1] || "";
  codeEditor.setValue(isNew ? "" : newContent);
  codeEditor.setOption("mode", getActiveMode());
}

function closeTab(index) {
  const tabs = document.querySelectorAll(".tab");
  if (index < 0 || index >= codeEditors.length) return;
  if (!confirm(`Close "${codeEditors[index][0]}"?`)) return;

  tabs[index].remove();
  codeEditors.splice(index, 1);
  if (codeEditors.length === 0) {
    codeEditor.setValue("");
    activeTab = 0;
  } else {
    const newActive = Math.max(0, index - 1);
    switchTab(newActive);
  }

  document.querySelectorAll(".tab").forEach((t, i) => (t.dataset.index = i));
}

function openFileInNewTab(fileName, contents = "") {
  // Save old tab
  if (codeEditors[activeTab]) codeEditors[activeTab][1] = codeEditor.getValue();

  addTab(fileName);
  switchTab(codeEditors.length - 1, true);
  codeEditor.setValue(contents);
  codeEditors[activeTab][1] = contents;
  codeEditor.setOption("mode", getActiveMode());
}

function newFile(ext = "kif") {
  const base = prompt(`Enter a name for the new ${ext.toUpperCase()} file:`, `untitled.${ext}`);
  if (base === null) return;
  const cleanName = base.replace(/\.[^/.]+$/, "") + "." + ext;
  openFileInNewTab(cleanName, "");
  toggleDropdown();
}

function downloadFile() {
  const content = codeEditor.getValue();
  if (!content.trim()) return alert("Nothing to download.");
  const blob = new Blob([content], { type: "text/plain" });
  const a = document.createElement("a");
  a.href = URL.createObjectURL(blob);
  a.download = getActiveFileName();
  a.click();
  URL.revokeObjectURL(a.href);
  toggleDropdown();
}


// ------------------------------------------------------------------
// Extraneous for now

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