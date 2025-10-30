const fileInput = document.getElementById('kifFile');
const fileNameSpan = document.getElementById('file-name');
const uploadForm = document.getElementById('uploadForm');
let errorMarks = [];
let codeEditors = [];
let activeTab = 0;
let errors = window.initialErrors || [];
let errorMask = window.initialErrorMask || [];
let codeEditor;

// ------------------------------------------------------------------
// Event Listeners

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

  codeEditors.push("");

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
  toggleDropdown();
  switchTab(0); // Show the first tab by default
});


// ------------------------------------------------------------------
// Syntax highlighting / coloring

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

// ------------------------------------------------------------------
// Editor Functions

function initializeCodeMirror() {
  const textarea = document.getElementById("codeEditor");
  const activeTabElem = document.querySelector(".tab.active span:not(.close-btn)");
  let fileName = activeTabElem ? activeTabElem.textContent.trim() : "Untitled.tptp";
  let ext = fileName.split('.').pop().toLowerCase();
  codeEditor = CodeMirror.fromTextArea(textarea, {
    mode: ext === "kif" ? "kif" : "tptp",
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
  errors.forEach((err, i) => {
    console.log(`editor.highlightErrors(): #${i}:`, JSON.stringify(err, null, 2));
  });
  console.log("editor.highlightErrors(): ActiveTab: " + activeTab);
  console.log("editor.highlightErrors(): Mask: " + mask);
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

function newFile(){
  addTab();
}

function downloadFile() {
  const editorObj = codeEditors[activeTab];
  const editor = editorObj?.cm || codeEditor;
  if (!editor || typeof editor.getValue !== "function") {
    alert("No active editor instance found.");
    return;
  }
  const codeContent = editor.getValue();
  if (!codeContent.trim()) {
    alert("No content to download. Please enter some code first.");
    return;
  }
  let fileName = "untitled.kif";
  const tab = document.querySelector(`.tab[data-index="${activeTab}"] span:not(.close-btn)`);
  if (tab) fileName = tab.textContent.trim();
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
  const activeTabElem = document.querySelector(".tab.active span:not(.close-btn)");
  const fileName = activeTabElem ? activeTabElem.textContent.trim() : "Untitled.kif";
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
  codeEditors.push({ cm: null, value: "" });
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
  const activeTabElem = tabs[index]?.querySelector("span:not(.close-btn)");
  const fileName = activeTabElem ? activeTabElem.textContent.trim() : "Untitled.kif";
  const ext = fileName.split(".").pop().toLowerCase();
  const mode = ext === "kif" ? "kif" : "tptp";
  codeEditor.setOption("mode", mode);
}


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

function openFileInNewTab(fileName, fileContents) {
  if (!fileName) return;
  if (codeEditors.length > activeTab && codeEditor)
    codeEditors[activeTab].value = codeEditor.getValue();
  addTab(fileName);
  const ext = fileName.split('.').pop().toLowerCase();
  const mode = ext === 'kif' ? 'kif' : 'tptp';
  const old = document.querySelector(".CodeMirror");
  if (old) old.remove();
  const textarea = document.createElement("textarea");
  textarea.value = fileContents || "";
  textarea.id = "codeEditor";
  document.querySelector(".tab-bar").after(textarea);
  codeEditor = CodeMirror.fromTextArea(textarea, {
    mode,
    lineNumbers: true,
    theme: "default",
    indentUnit: 2,
    tabSize: 2,
    lineWrapping: true,
    autoCloseBrackets: true,
    matchBrackets: true
  });
  codeEditors[activeTab] = { cm: codeEditor, value: fileContents || "" };
  if (fileNameSpan) fileNameSpan.textContent = 'Opened: ' + fileName;
  console.log(`✅ Opened "${fileName}" as ${mode} with ${fileContents?.length || 0} chars`);
}

// ------------------------------------------------------------------
// Extraneous for now
function getActiveFileName() {
  const activeTabElem = document.querySelector(".tab.active span:not(.close-btn)");
  return activeTabElem ? activeTabElem.textContent.trim() : "Untitled.tptp";
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

const exampleP = `% Example P (Propositional)
p(proposition, conjecture,
    ( (a & b) => c )
).`;
