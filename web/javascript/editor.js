const fileInput = document.getElementById('kifFile');
const fileNameSpan = document.getElementById('file-name');
const uploadForm = document.getElementById('uploadForm');

// codeEditors is a 2-D array, each array within contains the filename and contents.
// [0] = filename (string)
// [1] = file contents (string)
let codeEditors = [];
let activeTab = 0;
let errors = window.initialErrors || [];
let errorMarks = [];
let errorMask = window.initialErrorMask || [];
let codeEditor;

const $ = (s) => document.querySelector(s);
const getContent = () => codeEditor.getValue().trim();
const setEditorContent = (t = "") => codeEditor.setValue(t);
const setMode = () => codeEditor.setOption("mode", getActiveMode());
const saveActiveTab = () => codeEditors[activeTab] && (codeEditors[activeTab][1] = codeEditor.getValue());
const showMessage = (m, err = false) => {
  const box = $(".scroller.msg");
  if (!box) return;
  box.classList.toggle("errors-box", err);
  box.classList.toggle("success", !err);
  box.textContent = m;
};

async function postToServlet(mode, data = {}) {
  const body = new URLSearchParams({ mode, ...data }).toString();
  const res = await fetch("EditorServlet", {
    method: "POST",
    headers: {
      "Content-Type": "application/x-www-form-urlencoded; charset=UTF-8",
      "Accept": "application/json"
    },
    body
  });
  const text = await res.text();
  try {
    return (res.headers.get("content-type") || "").includes("json")
      ? JSON.parse(text)
      : { message: text };
  } catch { return { message: text }; }
}

function createTabElement(fileName, i) {
  const tab = document.createElement("div");
  tab.className = "tab";
  tab.dataset.index = i;
  const label = document.createElement("span");
  label.textContent = fileName;
  label.onclick = () => switchTab(i);
  const close = document.createElement("span");
  close.textContent = "×";
  close.className = "close-btn";
  close.onclick = (e) => { e.stopPropagation(); closeTab(i); };
  tab.append(label, close);
  return tab;
}


// ------------------------------------------------------------------
// Event Listeners & Initialization

async function defineModeFromXML(modeName, xmlPath) {
  const res = await fetch(xmlPath);
  const xmlText = await res.text();
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
        const prev = stream.start > 0 ? stream.string.charAt(stream.start - 1) : '';
        const validBefore = !prev || /[\s(]/.test(prev);
        const next = stream.pos < stream.string.length ? stream.string.charAt(stream.pos) : '';
        const validAfter = !next || /[\s)]/.test(next);
        const noLetterNearby = !(/[A-Za-z]/.test(prev) || /[A-Za-z]/.test(next));
        if (validBefore && validAfter && noLetterNearby)
          return "number";
        stream.backUp(stream.current().length);
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
  await Promise.all([
    defineModeFromXML("kif", "LanguagesXML/kif.xml"),
    defineModeFromXML("tptp", "LanguagesXML/tptp.xml")
  ]);
  initializeCodeMirror();
  if (window.initialErrors || window.initialErrorMask)
    highlightErrors(window.initialErrors || [], window.initialErrorMask || []);
  const examples = {
    "example.kif": `; Example Kif File\n(=>\n  (instance ?X Man)\n  (attribute ?X Mortal))`,
    "example.tptp": `% Example TPTP file\n tff(mortal_rule, axiom, (![X]: (man(X) => mortal(X)))).`,
    "example.thf": `% Example THF (Typed Higher-Order Form)\nthf(example_thf, conjecture, (![P: $i>$o, X: $i]: (P @ X))).`,
    "example.tff": `% Example TFF (Typed First-Order Form)\ntff(example_tff, axiom, (![X:$i]: (man(X) => mortal(X)))).`,
    "example.fof": `% Example FOF\nfof(example_fof, axiom, (![X]: (man(X)=>mortal(X)))).`,
    "example.cnf": `% Example CNF\ncnf(example_cnf, axiom, (~man(X)|mortal(X))).`
  };
  Object.entries(examples).forEach(([name, content]) => openFileInNewTab(name, content));
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
  const editor = codeEditor;
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
    editor.addLineClass(line, "gutter", cls === "warning-text" ? "warning-line-gutter" : "error-line-gutter");
  }
}

// ------------------------------------------------------------------
// Menu options

function toggleDropdown() {
  const d = $("#dropdownContent");
  d.style.display = d.style.display === "block" ? "none" : "block";
}

function triggerFileUpload() {
  $("#kifFile").click();
}

async function check() {
  const text = getContent();
  if (!text) return alert("Nothing to check.");
  const fileName = getActiveFileName();
  const { errors = [], errorMask = [], message } = await postToServlet("check", { fileName, code: text });
  renderErrorBox(errors, message);
  highlightErrors(errors, errorMask);
}

async function formatBuffer() {
  const text = getContent();
  if (!text) return alert("No content to format.");
  const { message } = await postToServlet("format", { fileName: getActiveFileName(), code: text });
  if (message?.trim()) setEditorContent(message);
}

// ------------------------------------------------------------------
// Tabs Management

function addTab(fileName = "untitled.kif") {
  const tabBar = $("#tabBar");
  const i = codeEditors.length;
  const tab = createTabElement(fileName, i);
  tabBar.appendChild(tab);
  codeEditors.push([fileName, ""]);
}

function switchTab(i, isNew = false) {
  if (i < 0 || i >= codeEditors.length) return;
  saveActiveTab();
  document.querySelectorAll(".tab").forEach((t, j) => t.classList.toggle("active", j === i));
  activeTab = i;
  setEditorContent(isNew ? "" : codeEditors[i]?.[1] || "");
  setMode();
}

function closeTab(i) {
  const tabs = document.querySelectorAll(".tab");
  if (i < 0 || i >= codeEditors.length) return;
  if (!confirm(`Close "${codeEditors[i][0]}"?`)) return;
  tabs[i].remove();
  codeEditors.splice(i, 1);
  switchTab(Math.max(0, i - 1));
  document.querySelectorAll(".tab").forEach((t, j) => (t.dataset.index = j));
}

function openFileInNewTab(name, contents = "") {
  saveActiveTab();
  addTab(name);
  switchTab(codeEditors.length - 1, true);
  setEditorContent(contents);
  codeEditors[activeTab][1] = contents;
  setMode();
}

function newFile(ext = "kif") {
  const base = prompt(`Name for new ${ext.toUpperCase()} file:`, `untitled.${ext}`);
  if (!base) return;
  const fileName = base.replace(/\.[^/.]+$/, "") + "." + ext;
  openFileInNewTab(fileName, "");
  toggleDropdown();
}

function downloadFile() {
  const text = getContent();
  if (!text) return alert("Nothing to download.");
  const blob = new Blob([text], { type: "text/plain" });
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