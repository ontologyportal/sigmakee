// ======================================================
// 1. GLOBAL VARIABLES & UTILITIES
// ======================================================
const debug = true;

const fileInput = document.getElementById('kifFile');

let codeEditors = [];
let activeTab = 0;
let errors = window.initialErrors || [];
let errorMarks = [];
let errorMask = window.initialErrorMask || [];
let codeEditor;

let checkTimer = null;
const CHECK_DEBOUNCE_MS = 2000;   // 2 seconds after typing stops
let lastCheckedText = "";
let checkSequence = 0; 

const $ = (s) => document.querySelector(s);
const getContent = () => codeEditor.getValue().trim();
const setEditorContent = (t = "") => codeEditor.setValue(t);
const setMode = () => codeEditor.setOption("mode", getActiveMode());
const saveActiveTab = () => codeEditors[activeTab] && (codeEditors[activeTab][1] = codeEditor.getValue());

// ======================================================
// 2. DROPDOWN MENU LOGIC
// ======================================================
let dropdownRoot, dropdown;

function ensureDropdownEls() {
  if (!dropdownRoot || !dropdown) {
    dropdownRoot = document.getElementById('fileDropdown');
    dropdown     = document.getElementById('dropdownContent');
    if (dropdownRoot) {
      dropdownRoot.addEventListener('click', e => e.stopPropagation());
    }
  }
}

function isDropdownOpen(){ 
  ensureDropdownEls(); 
  return dropdown && dropdown.style.display === 'block'; 
}

function openDropdown(){
  ensureDropdownEls();
  if (!dropdown) return;
  dropdown.style.display = 'block';
  setTimeout(() => {
    document.addEventListener('click', onDocClick, { passive:true });
    document.addEventListener('keydown', onEsc, { passive:true });
  }, 0);
}

function closeDropdown(){
  ensureDropdownEls();
  if (!dropdown) return;
  dropdown.style.display = 'none';
  document.removeEventListener('click', onDocClick);
  document.removeEventListener('keydown', onEsc);
}

function toggleFileMenu(evt){
  if (evt) evt.stopPropagation();
  isDropdownOpen() ? closeDropdown() : openDropdown();
}

function onDocClick(e) {
  ensureDropdownEls();
  if (!dropdownRoot || !dropdown) return;
  const clickedInside = dropdownRoot.contains(e.target);
  const isDropdownVisible = dropdown.style.display === 'block';
  if (isDropdownVisible && !clickedInside)
    closeDropdown();
}
function onEsc(e){ if (e.key === 'Escape') closeDropdown(); }
document.addEventListener('DOMContentLoaded', ensureDropdownEls);

// ======================================================
// 3. BACKEND COMMUNICATION (SERVLET API)
// ======================================================

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

// ======================================================
// 4. CODEMIRROR MODE DEFINITIONS
// ======================================================

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
          const next = stream.pos < stream.string.length ? stream.string.charAt(stream.pos) : '';
          const validBefore = !prev || !/[A-Za-z0-9_]/.test(prev);
          const validAfter  = !next || !/[A-Za-z0-9_]/.test(next);
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

// ======================================================
// 5. FILE HANDLING
// ======================================================

fileInput.addEventListener('change', () => {
  const files = fileInput.files;
  if (!files || files.length === 0) {
    console.warn("No file selected.");
    return;
  }
  const file = files[0];
  const reader = new FileReader();
  reader.onload = (e) => {
    try {
      const contents = e.target.result || "";
      if (typeof contents !== "string") {
        console.error("Invalid file contents");
        alert("Unable to read file contents.");
        return;
      }
      openFileInNewTab(file.name, contents);
    } catch (err) {
      console.error("Error processing file:", err);
      alert("Error loading file.");
    } finally {
      closeDropdown();
    }
  };
  reader.onerror = (e) => {
    console.error("FileReader error:", e);
    alert("Error reading file.");
    closeDropdown();
  };
  reader.readAsText(file);
});

function triggerFileUpload() {
  $("#kifFile").click();
}

function newFile(ext = "kif") {
  const allowed = ["kif", "tptp", "thf", "tff", "fof", "cnf"];
  ext = ext.trim().toLowerCase();
  if (!allowed.includes(ext)) {
    alert(`❌ Unsupported base file type: .${ext}`);
    return;
  }
  const base = prompt(`Enter name for new ${ext.toUpperCase()} file:`, `untitled.${ext}`);
  if (!base) return;
  const userExtMatch = base.match(/\.([^.]+)$/);
  const userExt = userExtMatch ? userExtMatch[1].toLowerCase() : ext;
  if (!allowed.includes(userExt)) {
    alert(`❌ Unsupported file extension: ".${userExt}"\n\nAllowed types:\n${allowed.map(e => "." + e).join(", ")}`);
    closeDropdown();
    return;
  }
  const fileName = base.replace(/\.[^/.]+$/, "") + "." + userExt;
  openFileInNewTab(fileName, "");
  closeDropdown();
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
  closeDropdown();
}

// ======================================================
// 6. INITIALIZATION
// ======================================================

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
  };
  Object.entries(examples).forEach(([name, content]) => openFileInNewTab(name, content));
  switchTab(0);
  reorderTabs();
});

// ======================================================
// 7. EDITOR CORE FUNCTIONS
// ======================================================

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
  codeEditor.on("change", onEditorChange)
}

function onEditorChange() {
  const text = getContent();
  if (checkTimer) clearTimeout(checkTimer);
  checkTimer = setTimeout(() => {
    if (!text.trim()) {
      lastCheckedText = "";
      renderErrorBox([], null);
      highlightErrors([], []);
      return;
    }
    if (text === lastCheckedText) return;
    runCheck(true);
  }, CHECK_DEBOUNCE_MS);
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
      const lineHuman = (Number(e.line) ?? 0);
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
    const line = Math.max(0, Math.min(Number(e.line - 1 ?? 0), lastLine));
    const from = { line, ch: e.start };
    const to   = { line, ch: e.end };
    const cls  = (Number(e.type) === 1) ? "warning-text" : "error-text";
    const mark = editor.markText(from, to, { className: cls, title: e.msg || "" });
    errorMarks.push(mark);
    editor.addLineClass(line, "gutter", cls === "warning-text" ? "warning-line-gutter" : "error-line-gutter");
  }
}

// ======================================================
// 8. TAB MANAGEMENT
// ======================================================

function createTabElement(fileName, i) {
  const tab = document.createElement("div");
  tab.className = "tab";
  tab.dataset.index = i;
  tab.draggable = true;
  const label = document.createElement("span");
  label.draggable = false;
  label.textContent = fileName;
  label.onclick = () => switchTab(i);
  const close = document.createElement("span");
  close.textContent = "×";
  close.className = "close-btn";
  close.draggable = false;
  close.onclick = (e) => { e.stopPropagation(); closeTab(i); };
  tab.append(label, close);
  return tab;
}

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

function reorderTabs() {
  const tabBar = document.getElementById("tabBar");
  if (!tabBar) return;
  let draggingEl = null;
  let draggedStart = null;
  let draggedWasActive = false;
  let isDragging = false;
  let dragEndAt = 0;
  tabBar.addEventListener("click", (e) => {
    const tab = e.target.closest(".tab");
    if (!tab || e.target.closest(".close-btn")) return;
    if (isDragging || (performance.now() - dragEndAt) < 60) return;
    const idx = Array.prototype.indexOf.call(tabBar.children, tab);
    switchTab(idx);
  });

  const reconcileDataToDom = () => {
    const tabsInDom = Array.from(tabBar.querySelectorAll(".tab"));
    const oldOrder  = codeEditors.slice();
    const domOldIdx = tabsInDom.map(t => parseInt(t.dataset.index, 10));
    codeEditors     = domOldIdx.map(oi => oldOrder[oi]);
    const mapOldToNew = (oldIdx) => domOldIdx.indexOf(oldIdx);
    if (draggedWasActive && draggedStart != null) activeTab = mapOldToNew(draggedStart);
    else activeTab = mapOldToNew(activeTab);
    tabBar.querySelectorAll(".tab").forEach((t, idx) => {
      t.dataset.index = idx;
      const label = t.querySelector("span:first-child");
      if (label) label.onclick = () => switchTab(idx);
      const close = t.querySelector(".close-btn");
      if (close) close.onclick = (ev) => { ev.stopPropagation(); closeTab(idx); };
    });
    const current = codeEditors[activeTab];
    if (current) { setEditorContent(current[1]); setMode(); }
  };

  tabBar.addEventListener("dragstart", (e) => {
    const tab = e.target.closest(".tab");
    if (!tab || !tabBar.contains(tab)) return;
    saveActiveTab();
    draggingEl = tab;
    draggedStart = parseInt(tab.dataset.index, 10);
    draggedWasActive = (activeTab === draggedStart);
    isDragging = true;
    tab.classList.add("dragging");
    try { e.dataTransfer.setData("text/plain", ""); } catch {}
    e.dataTransfer.effectAllowed = "move";
  });
  
  tabBar.addEventListener("dragover", (e) => {
    if (!draggingEl) return;
    e.preventDefault();
    e.dataTransfer.dropEffect = "move";
    const over = e.target.closest(".tab");
    if (!over || over === draggingEl) return;
    const rect = over.getBoundingClientRect();
    const before = e.clientX < rect.left + rect.width / 2;
    if (before) tabBar.insertBefore(draggingEl, over);
    else        tabBar.insertBefore(draggingEl, over.nextSibling);
  });

  const finalize = () => {
    if (!draggingEl) return;
    draggingEl.classList.remove("dragging");
    reconcileDataToDom();
    draggingEl = null;
    dragEndAt = performance.now();
    isDragging = false;
    draggedStart = null;
    draggedWasActive = false;
  };
  tabBar.addEventListener("drop", (e) => { e.preventDefault(); finalize(); });
  tabBar.addEventListener("dragend", () => { finalize(); });
}

// ======================================================
// 9. EDITOR ACTIONS
// ======================================================

async function runCheck(isAuto = false) {
  const text = getContent();

  // Handle empty buffer
  if (!text.trim()) {
    if (!isAuto) alert("Nothing to check.");
    lastCheckedText = "";
    renderErrorBox([], null);
    highlightErrors([], []);
    return;
  }

  const fileName = getActiveFileName();
  const thisSeq = ++checkSequence;  // track this request

  const { errors = [], errorMask = [], message } =
    await postToServlet("check", { fileName, code: text });
  if (thisSeq !== checkSequence) return;

  lastCheckedText = text;

  if (debug) console.log("Check results:", errors);

  renderErrorBox(errors, message);
  highlightErrors(errors, errorMask);
}

async function check() {
  return runCheck(false);
}

async function formatBuffer() {
  const text = getContent();
  if (!text) return alert("No content to format.");
  const { message } = await postToServlet("format", { fileName: getActiveFileName(), code: text });
  if (message?.trim()) setEditorContent(message);
  check();
}