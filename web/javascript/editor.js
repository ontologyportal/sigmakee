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
let suppressChangeEvent = false;
let checkTimer = null;
const CHECK_DEBOUNCE_MS = 2000;
let lastCheckedText = "";
let checkSequence = 0; 
const $ = (s) => document.querySelector(s);
const getContent = () => codeEditor.getValue();

const setEditorContent = (t = "") => {
  suppressChangeEvent = true;
  codeEditor.setValue(t);  // "change" fires here, but is ignored
  suppressChangeEvent = false;
};

const setMode = () => {
  suppressChangeEvent = true;
  codeEditor.setOption("mode", getActiveMode());
  suppressChangeEvent = false;
  updateAtpAvailability();
};

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

let inFlightCheck = null;

async function postToServlet(mode, data = {}, opts = {}) {
  const body = new URLSearchParams({ mode, ...data }).toString();
  const res = await fetch("EditorServlet", {
    method: "POST",
    headers: {
      "Content-Type": "application/x-www-form-urlencoded; charset=UTF-8",
      "Accept": "application/json"
    },
    body,
    signal: opts.signal
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
  const allowed = ["kif", "tptp", "thf", "tff", "fof", "cnf", "tq"];
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
  if (window.initialOpenFile) {
    openFileInNewTab(
      window.initialOpenFile.name,
      window.initialOpenFile.contents || "",
      window.initialOpenFile.source || "server",
      window.initialOpenFile.path || ""
    );

    const line = Number(window.initialOpenFile.line || 1);
    jumpToLine(line, 0);
  }
  else {
    const examples = {
      "example.kif": `; Example Kif File\n(=>\n  (instance ?X Man)\n  (attribute ?X Mortal))`,
      "example.tptp": `% Example TPTP file\n fof(example_1, axiom, ( ( ! [V__X] : ((s__instance(V__X,s__Man) => s__attribute(V__X,s__Mortal)) ) ) )).`,
    };
    Object.entries(examples).forEach(([name, content]) => openFileInNewTab(name, content));
    switchTab(0);
  }
  reorderTabs();
});

document.addEventListener("click", function (e) {
  const entry = e.target.closest(".error-entry");
  if (!entry) return;
  const line  = Number(entry.dataset.line || 1);
  const start = Number(entry.dataset.start || 0);
  jumpToError(line, start);
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
    matchBrackets: true,
    extraKeys: {
      "Ctrl-F": "findPersistent",
      "Cmd-F": "findPersistent",
      "Ctrl-G": "findNext",
      "Cmd-G": "findNext",
      "Shift-Ctrl-G": "findPrev",
      "Shift-Cmd-G": "findPrev",
      "Ctrl-Enter": queryHighlightedExpression,
      "Cmd-Enter": queryHighlightedExpression
    }
  });
  codeEditor.on("change", onEditorChange);
  codeEditor.on("cursorActivity", () => updateParenContext(codeEditor, { highlightRange: true }));
  codeEditor.on("focus", () => updateParenContext(codeEditor, { highlightRange: true }));
  codeEditor.on("change", () => updateParenContext(codeEditor, { highlightRange: true }));
}

async function queryHighlightedExpression() {
  const stmt = codeEditor.getSelection().trim();

  if (!stmt) {
    alert("Highlight a SUO-KIF expression first.");
    codeEditor.focus();
    return;
  }

  if (getActiveMode() !== "kif") {
    alert("Query selection currently supports SUO-KIF files only.");
    return;
  }

  renderQueryOutput(
    "Running Vampire (30 second timeout)...",
    true
  );

  try {
    const urlParameters = new URLSearchParams(window.location.search);

    const requestBody = new URLSearchParams({
      mode: "query",
      stmt: stmt,
      kb: urlParameters.get("kb") || "SUMO"
    });

    const response = await fetch("EditorServlet", {
      method: "POST",
      headers: {
        "Content-Type":
          "application/x-www-form-urlencoded;charset=UTF-8"
      },
      body: requestBody.toString()
    });

    const result = await response.json();

    const message = result.message ||
      (result.success
        ? "Vampire completed without output."
        : "Unable to run query.");

    renderQueryOutput(message, Boolean(result.success));
  }
  catch (error) {
    renderQueryOutput(
      "Unable to run query: " + error.message,
      false
    );
  }
}

function onEditorChange() {
  if (suppressChangeEvent) return;
  const entry = codeEditors[activeTab];
  if (!entry) return;
  const text = getContent();
  entry[1] = text;
  const isDirty = (entry.lastSaved !== text);
  entry.dirty = isDirty;
  if (isDirty) markTabUnsaved(activeTab);
  else markTabSaved(activeTab);
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

function renderQueryOutput(message, success) {
  const box = document.querySelector(".scroller.msg");

  if (!box) {
    return;
  }

  box.innerHTML = "";

  const title = document.createElement("div");
  title.className = "problems-title";
  title.textContent = success ? "Query Result:" : "Query Error:";

  const divider = document.createElement("hr");
  divider.className = "problems-divider";

  const body = document.createElement("div");
  body.className = "problems-body";
  body.textContent = message;
  body.style.whiteSpace = "pre-wrap";

  box.classList.toggle("success", success);
  box.classList.toggle("errors-box", !success);

  box.appendChild(title);
  box.appendChild(divider);
  box.appendChild(body);
}

function renderErrorBox(errors = [], message = null) {
  const box = document.querySelector(".scroller.msg");
  if (!box) return;
  box.innerHTML = "";
  const body = addProblemsHeader(box);
  if (message) {
    box.classList.remove("success");
    box.classList.add("errors-box");
    body.textContent = message;
    return;
  }
  if (!errors.length) {
    box.classList.remove("errors-box");
    box.classList.add("success");
    body.textContent = "No errors found.";
    return;
  }
  box.classList.remove("success");
  box.classList.add("errors-box");
  errors.forEach((e, idx) => {
    if (idx > 0) body.appendChild(document.createElement("br"));
    const div = document.createElement("div");
    div.className = "error-entry";
    div.dataset.line  = String(e.line ?? 1);
    div.dataset.start = String(e.start ?? 0);
    div.dataset.end   = String(e.end ?? 0);
    const sev = Number(e.type) === 1 ? "WARNING" : "ERROR";
    const lineHuman = Number(e.line ?? 0);
    const colHuman  = Number(e.start ?? 0) + 1;
    div.textContent =
      `${sev} ${e.file ? e.file : "(buffer)"}:${lineHuman}:${colHuman}\n${e.msg || ""}`;
    body.appendChild(div);
  });
}

function addProblemsHeader(box) {
  const title = document.createElement("div");
  title.className = "problems-title";
  title.textContent = "Problems:";
  const hr = document.createElement("hr");
  hr.className = "problems-divider";
  const body = document.createElement("div");
  body.className = "problems-body";
  box.appendChild(title);
  box.appendChild(hr);
  box.appendChild(body);
  return body;
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
  label.onclick = () => {
    const idx = parseInt(tab.dataset.index, 10);
    switchTab(idx);
  };

  const close = document.createElement("span");
  close.textContent = "×";
  close.className = "close-btn";
  close.draggable = false;
  close.onclick = (e) => {
    e.stopPropagation();
    const idx = parseInt(tab.dataset.index, 10);
    closeTab(idx);
  };

  tab.append(label, close);
  return tab;
}

function addTab(fileName = "untitled.kif", source = "user", path = "") {
  const tabBar = $("#tabBar");
  const i = codeEditors.length;
  const tab = createTabElement(fileName, i);
  tabBar.appendChild(tab);
  const entry = makeEditorEntry(fileName, "", source, path);
  codeEditors.push(entry);
}

function renameTab(index) {
  const entry = codeEditors[index];
  if (!entry) return;
  const oldName = entry[0];
  const newName = prompt("Rename file:", oldName);
  if (!newName) return;
  const trimmed = newName.trim();
  if (!trimmed || trimmed === oldName) return;
  const validationError = validateFilename(trimmed);
  if (validationError) {
    alert("❌ " + validationError);
    return;
  }
  entry[0] = trimmed;
  updateTabLabels();
}

function switchTab(i, { skipSave = false } = {}) {
  if (i < 0 || i >= codeEditors.length) return;
  if (!skipSave) {
    saveActiveTab();
  }
  document.querySelectorAll(".tab").forEach((t, j) => {
    t.classList.toggle("active", j === i);
  });
  activeTab = i;
  const entry = codeEditors[i];
  const text  = entry ? (entry[1] || "") : "";
  setEditorContent(text);
  setMode();
  updateTranslateMenu();
  // if (entry && entry.dirty)
  //   markTabUnsaved(i);
  // else
  //   markTabSaved(i);
}

function closeTab(i) {
  const tabs = document.querySelectorAll(".tab");
  if (i < 0 || i >= codeEditors.length) return;
  if (!confirm(`Close "${codeEditors[i][0]}"?`)) return;

  const wasActive = (i === activeTab);

  // Remove DOM node & entry
  tabs[i].remove();
  codeEditors.splice(i, 1);

  // Reindex dataset indices
  document.querySelectorAll(".tab").forEach((t, j) => (t.dataset.index = j));

  // No tabs left → clear editor
  if (!codeEditors.length) {
    activeTab = 0;
    setEditorContent("");
    return;
  }

  if (wasActive) {
    // If we closed the active tab, move focus to the previous tab (or 0)
    const newIndex = Math.max(0, i - 1);
    // Important: skipSave so we don't overwrite that tab's content
    switchTab(newIndex, { skipSave: true });
  } else {
    // If we closed a tab before the current active, shift activeTab left
    if (i < activeTab) {
      activeTab -= 1;
    }
    // The active tab’s DOM element already has the "active" class, so no need
    // to call switchTab here.
  }
}

function openFileInNewTab(name, contents = "", source = "user", path = "") {
  saveActiveTab();
  addTab(name, source, path);
  const newIndex = codeEditors.length - 1;
  switchTab(newIndex);
  const entry = codeEditors[newIndex];
  entry[1] = contents;
  entry.lastSaved = contents;
  entry.dirty = false;
  entry.source = source;
  entry.path = path;
  setEditorContent(contents);
  markTabSaved(newIndex);
}

function reorderTabs() {
  const tabBar = document.getElementById("tabBar");
  if (!tabBar) return;
  let draggingEl = null;
  let draggedStart = null;
  let draggedWasActive = false;
  let isDragging = false;
  let dragEndAt = 0;
  tabBar.addEventListener("dblclick", (e) => {
    const tab = e.target.closest(".tab");
    if (!tab || e.target.closest(".close-btn")) return;
    const idx = parseInt(tab.dataset.index, 10);
    if (Number.isNaN(idx)) return;
    renameTab(idx);
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

// ======================================================
// 10. USER FILE SAVING \ OPENING ACTIONS
// ======================================================

async function openFileModal() {
  const modal = document.getElementById("openFileModal");
  const listElem = modal.querySelector(".file-list");

  // Clear old contents
  listElem.innerHTML = "<li>Loading...</li>";

  modal.style.display = "flex";

  // Load from backend
  const files = await loadUserFileList();

  if (!files.length) {
    listElem.innerHTML = "<li>(No saved files found)</li>";
    return;
  }

  // Generate clickable list
  listElem.innerHTML = "";
  for (const f of files) {
    const li = document.createElement("li");
    li.textContent = f;
    li.onclick = () => openUserFile(f);
    listElem.appendChild(li);
  }
}

function closeOpenFileModal() {
  document.getElementById("openFileModal").style.display = "none";
}

async function openUserFile(filename) {
  const res = await postToServlet("loadUserFile", { fileName: filename });

  if (!res || !res.success) {
    alert("Error loading file: " + (res?.message || ""));
    return;
  }

  openFileInNewTab(filename, res.contents || "");
  closeOpenFileModal();
}

async function openSaveFileModal() {
  const entry = codeEditors[activeTab];
  if (entry?.source === "server") {
    await saveServerFile(entry);
    return;
  }

  const activeName = getActiveFileName().trim();
  const files = await loadUserFileList();

  if (files.includes(activeName)) {
    const overwrite = confirm(`The file "${activeName}" already exists.\n\nDo you want to overwrite it?`);
    if (!overwrite) return;

    const content = getContent();
    const res = await postToServlet("saveUserFile", { fileName: activeName, code: content });

    if (res?.success) {
      const entry = codeEditors[activeTab];
      entry.lastSaved = content;
      entry[1] = content;
      entry.dirty = false;
      markTabSaved(activeTab);
      alert(`File "${activeName}" overwritten successfully.`);
    } else {
      alert("Error overwriting: " + (res?.message || ""));
    }
    return;
  }

  document.getElementById("saveFileNameInput").value = activeName;
  populateFileList("saveFileList");
  document.getElementById("saveFileModal").style.display = "flex";
}

function closeSaveFileModal() {
  document.getElementById("saveFileModal").style.display = "none";
}

async function saveFile() {
  const filename = document.getElementById("saveFileNameInput").value.trim();
  if (!filename) {
    alert("Filename cannot be empty.");
    return;
  }
  const validationError = validateFilename(filename);
  if (validationError) {
    alert("❌ " + validationError);
    return;
  }

  const oldName = getActiveFileName();
  const content = getContent();
  const res = await postToServlet("saveUserFile", {
    fileName: filename,
    code: content
  });

  if (res?.success) {
    alert("File saved successfully.");
    closeSaveFileModal();

    const entry = codeEditors[activeTab];
    if (filename !== oldName) {
      entry[0] = filename;       // update name
      updateTabLabels();
    }

    // This is now the saved snapshot
    entry.lastSaved = content;
    entry[1]        = content;   // keep buffer in sync
    entry.dirty     = false;
    markTabSaved(activeTab);
  } else {
    alert("Error saving file:\n" + (res?.message || "Unknown error"));
  }
}

async function openSaveAsModal() {
  const entry = codeEditors[activeTab];
  if (entry?.source === "server") {
    await saveServerFileAs(entry);
    return;
  }
  document.getElementById("saveAsFileNameInput").value = getActiveFileName();
  populateFileList("saveAsFileList");
  document.getElementById("saveAsModal").style.display = "flex";
}

function closeSaveAsModal() {
  document.getElementById("saveAsModal").style.display = "none";
}

async function saveFileAs() {
  const filename = document.getElementById("saveAsFileNameInput").value.trim();
  if (!filename) {
    alert("Filename cannot be empty.");
    return;
  }

  const validationError = validateFilename(filename);
  if (validationError) {
    alert("❌ " + validationError);
    return;
  }

  const files = await loadUserFileList();
  if (files.includes(filename)) {
    const overwrite = confirm(
      `The file "${filename}" already exists.\n\nDo you want to overwrite it?`
    );
    if (!overwrite) return;
  }

  const content = getContent();
  const res = await postToServlet("saveUserFile", {
    fileName: filename,
    code: content
  });
  if (res?.success) {
    const entry = codeEditors[activeTab];
    entry[0]       = filename;
    entry.lastSaved = content;
    entry[1]        = content;
    entry.dirty     = false;
    updateTabLabels();
    markTabSaved(activeTab);
    alert(`Saved as ${filename}`);
    closeSaveAsModal();
  } else {
    alert("Error saving:\n" + (res?.message || "Unknown error"));
  }
}

async function loadUserFileList() {
  const res = await postToServlet("listUserFiles");
  return res?.files || [];
}

function markTabUnsaved(index) {
  const tab = document.querySelector(`.tab[data-index="${index}"] span:first-child`);
  if (tab && !tab.textContent.endsWith(" ●")) {
    tab.innerHTML = tab.innerHTML.replace(/\s*●$/, "") + ' <span class="unsaved-dot">●</span>';
  }
}

function markTabSaved(index) {
  const tab = document.querySelector(`.tab[data-index="${index}"] span:first-child`);
  if (tab) {
    tab.innerHTML = tab.innerHTML.replace(/\s*<span class="unsaved-dot">●<\/span>/, "");
  }
}

function updateTabLabels() {
  document.querySelectorAll(".tab").forEach((tab, idx) => {
    const label = tab.querySelector("span:first-child");
    const name = codeEditors[idx][0];
    const dirty = codeEditors[idx].dirty;
    label.innerHTML = dirty
      ? `${name} <span class="unsaved-dot">●</span>`
      : name;
  });
}

async function populateFileList(listElemId) {
  const listElem = document.getElementById(listElemId);
  if (!listElem) return;
  listElem.innerHTML = "<li>Loading...</li>";
  const files = await loadUserFileList();
  if (!files.length) {
    listElem.innerHTML = "<li>(No files yet)</li>";
    return;
  }
  listElem.innerHTML = "";
  files.forEach(f => {
    const li = document.createElement("li");
    li.textContent = f;
    li.onclick = () => {
      if (listElemId === "saveFileList") {
        document.getElementById("saveFileNameInput").value = f;
      } else if (listElemId === "saveAsFileList") {
        document.getElementById("saveAsFileNameInput").value = f;
      }
    };
    
    listElem.appendChild(li);
  });
}

function validateFilename(name) {
  if (!name || typeof name !== "string") return "Filename cannot be empty.";
  const trimmed = name.trim();
  if (trimmed.length === 0) return "Filename cannot be blank or whitespace.";
  const illegal = /[<>:"\/\\|?*\x00-\x1F]/g;
  if (illegal.test(trimmed)) {
    return 'Filename contains illegal characters:  < > : " / \\ | ? *';
  }
  if (/[\x00-\x1F]/.test(trimmed)) {
    return "Filename contains control characters (ASCII 0–31).";
  }
  if (/^\.+$/.test(trimmed)) {
    return "Filename cannot be only dots.";
  }
  const reserved = [
    "CON", "PRN", "AUX", "NUL",
    "COM1", "COM2", "COM3", "COM4", "COM5", "COM6", "COM7", "COM8", "COM9",
    "LPT1", "LPT2", "LPT3", "LPT4", "LPT5", "LPT6", "LPT7", "LPT8", "LPT9"
  ];
  if (reserved.includes(trimmed.toUpperCase()))
    return `The filename "${trimmed}" is reserved and cannot be used.`;
  return null;
}

function makeEditorEntry(fileName, content = "", source = "user", path = "") {
  const entry = [fileName, content];
  entry.lastSaved = content;
  entry.dirty = false;
  entry.source = source;
  entry.path = path;
  return entry;
}

function openHelpModal() {
  const modal = document.getElementById("helpModal");
  if (modal) modal.style.display = "flex";
}

function closeHelpModal() {
  const modal = document.getElementById("helpModal");
  if (modal) modal.style.display = "none";
}

async function translateKifToTptp() {
  const fileName = getActiveFileName();
  const ext = fileName.split(".").pop().toLowerCase();
  if (ext !== "kif") {
    const proceed = confirm(`The current file "${fileName}" is not a .kif file.\nTranslate anyway?`);
    if (!proceed) return;
  }
  const code = getContent();
  if (!code.trim()) {
    alert("Nothing to translate.");
    return;
  }
  try {
    const res = await postToServlet("translateToTPTP", {
      fileName,
      code,
    });
    if (!res || !res.success) {
      alert("TPTP translation failed:\n" + (res && res.message ? res.message : "Unknown error"));
      return;
    }
    const tptpText = res.tptp || "";
    if (!tptpText.trim()) {
      alert("Translation produced no output.");
      return;
    }
    const newName = fileName.replace(/\.kif$/i, "") + ".tptp";
    openFileInNewTab(newName, tptpText);
  } catch (e) {
    console.error("Error while translating to TPTP:", e);
    alert("Unexpected error during translation.\nSee console for details.");
  }
}


async function translateKifToTff() {
  const fileName = getActiveFileName();
  const code = getContent();
  if (!code.trim()) {
    alert("Nothing to translate.");
    return;
  }
  try {
    const res = await postToServlet("translateToTFF", {
      fileName,
      code,
      kb: "SUMO",
    });
    if (!res || !res.success) {
      alert("TFF translation failed:\n" + (res?.message || "Unknown error"));
      return;
    }
    if (!(res.tff || "").trim()) {
      alert("Translation produced no output.");
      return;
    }
    const newName = fileName.replace(/\.kif$/i, "") + ".tff";
    openFileInNewTab(newName, res.tff);
  }
  catch (e) {
    console.error("Error while translating to TFF:", e);
    alert("Unexpected error during translation.");
  }
}

async function translateKifToThf() {
  const fileName = getActiveFileName();
  const code = getContent();
  if (!code.trim()) {
    alert("Nothing to translate.");
    return;
  }
  try {
    const res = await postToServlet("translateToTHF", {
      fileName,
      code,
      kb: "SUMO",
    });
    if (!res || !res.success) {
      alert("THF translation failed:\n" + (res?.message || "Unknown error"));
      return;
    }
    if (!(res.thf || "").trim()) {
      alert("Translation produced no output.");
      return;
    }
    const newName = fileName.replace(/\.kif$/i, "") + ".thf";
    openFileInNewTab(newName, res.thf);
  }
  catch (e) {
    console.error("Error while translating to THF:", e);
    alert("Unexpected error during translation.");
  }
}
// ======================================================
// Translate DROPDOWN MENU LOGIC
// ======================================================
let translateRoot, translateMenu;

function ensureTranslateEls() {
  if (!translateRoot || !translateMenu) {
    translateRoot = document.getElementById("translateDropdown");
    translateMenu = document.getElementById("translateDropdownContent");
    if (translateRoot) {
      translateRoot.addEventListener("click", (e) => e.stopPropagation());
    }
  }
}

function isTranslateOpen() {
  ensureTranslateEls();
  return translateMenu && translateMenu.style.display === "block";
}

function openTranslateMenu() {
  ensureTranslateEls();
  if (!translateMenu) return;
  translateMenu.style.display = "block";
  setTimeout(() => {
    document.addEventListener("click", onTranslateDocClick, { passive: true });
    document.addEventListener("keydown", onTranslateEsc, { passive: true });
  }, 0);
}

function closeTranslateMenu() {
  ensureTranslateEls();
  if (!translateMenu) return;
  translateMenu.style.display = "none";
  document.removeEventListener("click", onTranslateDocClick);
  document.removeEventListener("keydown", onTranslateEsc);
}

function toggleTranslateMenu(evt) {
  if (evt) evt.stopPropagation();
  isTranslateOpen() ? closeTranslateMenu() : openTranslateMenu();
}

function onTranslateDocClick(e) {
  ensureTranslateEls();
  if (!translateRoot || !translateMenu) return;
  const clickedInside = translateRoot.contains(e.target);
  const isVisible = translateMenu.style.display === "block";
  if (isVisible && !clickedInside) closeTranslateMenu();
}

function onTranslateEsc(e) {
  if (e.key === "Escape") closeTranslateMenu();
}

document.addEventListener("DOMContentLoaded", ensureTranslateEls);

// Enable/disable translate options based on active tab extension
function updateTranslateMenu() {
  const fileName = getActiveFileName();
  const ext = (fileName.split(".").pop() || "").toLowerCase();

  const label = document.getElementById("translateLabel");
  const kifToTptp = document.getElementById("translate-kif-tptp");
  const kifToTff = document.getElementById("translate-kif-tff");
  const kifToThf = document.getElementById("translate-kif-thf");
  const allOptions = document.querySelectorAll(".translate-option");

  if (!label || !kifToTptp || !kifToTff || !kifToThf) return;

  // Default: disable all options
  allOptions.forEach((opt) => {
    opt.classList.add("disabled");
    opt.setAttribute("aria-disabled", "true");
  });

  // Only enable KIF → TPTP when active file is .kif
  if (ext === "kif") {
    [kifToTptp, kifToTff, kifToThf].forEach((option) => {
      option.classList.remove("disabled");
      option.removeAttribute("aria-disabled");
    });
  }

  // You can change the label text if you want:
  // label.textContent = ext === "kif" ? "Translate ▾" : "Translate ▾";
}

// Handle clicking on translate menu items
function handleTranslateClick(event, kind) {
  event.preventDefault();
  const target = event.currentTarget;
  if (target.classList.contains("disabled")) {
    return; // do nothing for disabled items
  }

  switch (kind) {
    case "kif-tptp":
      translateKifToTptp();
      break;
    case "kif-tff":
      translateKifToTff();
      break;
    case "kif-thf":
      translateKifToThf();
      break;
    default:
      console.warn("Translate action not implemented:", kind);
  }

  closeTranslateMenu();
}

// ======================================================
// ATP / TQ QUERY
// ======================================================

function isActiveTqFile() {
  return getActiveFileName().toLowerCase().endsWith(".tq");
}

function activeFileExtension() {
  return (getActiveFileName().split(".").pop() || "").toLowerCase();
}

function activeProblemLanguage() {
  const extension = activeFileExtension();
  const code = getContent();
  if (extension === "thf" || /^\s*thf\s*\(/im.test(code))
    return "THF";
  if (extension === "tff" || /^\s*tff\s*\(/im.test(code))
    return "TFF";
  return "FOF";
}

function isActiveDirectProblemFile() {
  return ["tptp", "p", "fof", "tff", "thf", "cnf"]
    .includes(activeFileExtension());
}

function isActiveAtpFile() {
  return isActiveTqFile() || isActiveDirectProblemFile();
}

function updateAtpAvailability() {
  const header = document.getElementById("atpHeader");
  if (!header) return;
  const available = isActiveAtpFile();
  header.classList.toggle("atp-unavailable", !available);
  header.title = available
    ? "Run the active file with an automated theorem prover"
    : "Open a TQ or TPTP-family file to use automated theorem proving";
}

function openAtpModal() {
  if (!isActiveAtpFile()) {
    alert("ATP requires an active .tq, .tptp, .p, .fof, .tff, .thf, or .cnf file.");
    return;
  }
  const modal = document.getElementById("atpModal");
  const note = document.getElementById("atpNote");
  const runButton = document.getElementById("runAtpButton");
  if (!modal) return;
  if (note) note.textContent = isActiveTqFile()
    ? "The active TQ file's meta-predicates determine the translation language."
    : "The active problem file will be passed directly to the selected prover.";
  if (runButton) runButton.textContent = isActiveTqFile()
    ? "Run TQ Query" : "Run Problem File";
  modal.style.display = "flex";
  updateDirectProverCompatibility();
  toggleVampireOptions();
}

function closeAtpModal() {
  const modal = document.getElementById("atpModal");
  if (modal) modal.style.display = "none";
}
function updateDirectProverCompatibility() {
  const engines = Array.from(document.querySelectorAll(
    '#atpOptions input[name="inferenceEngine"]'
  ));
  const direct = isActiveDirectProblemFile();
  const thf = activeProblemLanguage() === "THF";

  engines.forEach((engine) => {
    if (engine.dataset.configuredDisabled === undefined)
      engine.dataset.configuredDisabled = engine.disabled ? "true" : "false";
    const unavailable = engine.dataset.configuredDisabled === "true";
    const incompatible = direct && (
      (engine.value === "EPROVER" && thf) ||
      (engine.value === "LEO" && !thf)
    );
    engine.disabled = unavailable || incompatible;
    const card = engine.closest(".card");
    if (card) card.classList.toggle("engineDisabled", engine.disabled);
  });

  const selected = engines.find((engine) => engine.checked);
  if (!selected || selected.disabled) {
    const fallback = engines.find((engine) =>
      engine.value === "VAMPIRE" && !engine.disabled
    ) || engines.find((engine) => !engine.disabled);
    if (fallback) fallback.checked = true;
  }
}


function toggleVampireOptions() {
  const vampire = document.getElementById("engineVampire");
  const vampireInputs = document.querySelectorAll(
    '#atpOptions input[name="vampireMode"], #atpOptions #ModusPonens, ' +
    '#atpOptions #dropOnePremise, #atpOptions #HolUseModals'
  );
  vampireInputs.forEach((input) => {
    input.disabled = vampire && !vampire.checked;
  });
}

async function runActiveAtp(event) {
  event.preventDefault();
  if (!isActiveAtpFile()) {
    closeAtpModal();
    alert("The active tab is no longer an ATP-supported file.");
    return;
  }

  const form = document.getElementById("atpOptions");
  const runButton = document.getElementById("runAtpButton");
  const status = document.getElementById("atpRunStatus");
  const results = document.getElementById("atpResults");
  const options = Object.fromEntries(new FormData(form).entries());
  const urlParameters = new URLSearchParams(window.location.search);

  runButton.disabled = true;
  status.className = "atp-run-status running";
  status.textContent = "Running " +
    (options.inferenceEngine || "ATP") + "...";
  results.style.display = "none";
  results.textContent = "";

  try {
    const response = await postToServlet("runAtp", {
      ...options,
      fileName: getActiveFileName(),
      code: getContent(),
      kb: urlParameters.get("kb") || "SUMO"
    });

    if (!response.success) {
      status.className = "atp-run-status failed";
      status.textContent = response.message || "Unable to run the ATP problem.";
      return;
    }

    if (response.error) {
      status.className = "atp-run-status failed";
      status.textContent = "ERROR: the prover could not process the problem.";
    }
    else if (response.direct) {
      status.className = "atp-run-status " +
        (response.passed ? "passed" : "failed");
      status.textContent = response.passed
        ? "PROVED: the conjecture in the problem file was established."
        : "NOT PROVED: SZS status " + (response.szs || "Unknown") + ".";
    }
    else if (response.expectationProvided) {
      status.className = "atp-run-status " +
        (response.passed ? "passed" : "failed");
      status.textContent = response.passed
        ? "PASS: returned answers match the TQ expectations."
        : "FAIL: returned answers do not match the TQ expectations.";
    }
    else {
      status.className = "atp-run-status " +
        (response.passed ? "passed" : "failed");
      status.textContent = response.passed
        ? "PROVED: the conjecture was established. Add (answer yes) to check an explicit expectation."
        : "NOT PROVED: the prover did not establish the conjecture.";
    }

    const summary = [
      "SZS status: " + (response.szs || "Unknown"),
      (response.direct ? "Input language: " : "Translation: ") +
        String(response.translation || "").toUpperCase(),
      "Time: " + response.time + " ms"
    ];
    if (!response.direct) {
      summary.push(
        "Expected: " + (response.expectationProvided
          ? JSON.stringify(response.expected || []) : "(none supplied)"),
        "Actual: " + JSON.stringify(response.answers || [])
      );
    }
    if (response.proof) {
      summary.push("", "Proof / prover output:", response.proof);
    }
    results.textContent = summary.join("\n");
    results.style.display = "block";
  }
  catch (error) {
    status.className = "atp-run-status failed";
    status.textContent = "Unable to run the ATP problem: " + error.message;
  }
  finally {
    runButton.disabled = false;
  }
}


function jumpToLine(line, start = 0) {
  if (!codeEditor) return;

  const lineIndex = Math.max(
    0,
    Math.min(Number(line || 1) - 1, codeEditor.lineCount() - 1)
  );

  const ch = Math.max(0, Number(start || 0));
  const pos = { line: lineIndex, ch };

  requestAnimationFrame(() => {
    requestAnimationFrame(() => {
      codeEditor.refresh();

      codeEditor.operation(() => {
        codeEditor.focus();
        codeEditor.setCursor(pos);

        // First try CodeMirror's built-in scroll.
        codeEditor.scrollIntoView({ from: pos, to: pos }, 120);

        // Then force an explicit scroll position. This is more reliable
        // for large files loaded immediately after page initialization.
        const scroller = codeEditor.getScrollerElement();
        const coords = codeEditor.charCoords(pos, "local");
        const y = Math.max(0, coords.top - scroller.clientHeight / 3);
        codeEditor.scrollTo(null, y);
      });

      const handle = codeEditor.addLineClass(lineIndex, "background", "error-flash");
      setTimeout(() => {
        if (handle)
          codeEditor.removeLineClass(lineIndex, "background", "error-flash");
      }, 800);
    });
  });
}

function jumpToError(line, start = 0) {
  jumpToLine(line, start);
}

// ======================================================
// PAREN CONTEXT HIGHLIGHTING (NO ADDONS REQUIRED)
// ======================================================
let parenContextMarks = [];
let parensRAF = 0;

function clearParenContext() {
  for (const m of parenContextMarks) m.clear();
  parenContextMarks = [];
}

function samePos(a, b) {
  return a && b && a.line === b.line && a.ch === b.ch;
}

function beforeOrEqual(a, b) { return CodeMirror.cmpPos(a, b) <= 0; }
function afterOrEqual(a, b)  { return CodeMirror.cmpPos(a, b) >= 0; }

function getCharAt(cm, pos) {
  const line = cm.getLine(pos.line);
  if (!line) return "";
  return line.charAt(pos.ch) || "";
}

function nextPos(cm, pos) {
  const lineText = cm.getLine(pos.line) || "";
  if (pos.ch + 1 <= lineText.length - 1) return { line: pos.line, ch: pos.ch + 1 };
  // move to next line
  if (pos.line + 1 >= cm.lineCount()) return null;
  return { line: pos.line + 1, ch: 0 };
}

function prevPos(cm, pos) {
  if (pos.ch - 1 >= 0) return { line: pos.line, ch: pos.ch - 1 };
  if (pos.line - 1 < 0) return null;
  const prevLine = cm.getLine(pos.line - 1) || "";
  return { line: pos.line - 1, ch: Math.max(0, prevLine.length - 1) };
}

// Scan backwards from cursor to find the nearest enclosing '('
function findEnclosingOpenParen(cm, cursor, maxScanChars = 50000) {
  let pos = { line: cursor.line, ch: cursor.ch - 1 };
  let depth = 0;
  let scanned = 0;

  while (pos && scanned < maxScanChars) {
    const c = getCharAt(cm, pos);
    if (c === ')') depth++;
    else if (c === '(') {
      if (depth === 0) return pos;
      depth--;
    }
    pos = prevPos(cm, pos);
    scanned++;
  }
  return null;
}

// From an open '(', scan forward to its matching ')'
function findMatchingCloseParen(cm, openPos, maxScanChars = 50000) {
  let pos = { line: openPos.line, ch: openPos.ch + 1 };
  let depth = 1;
  let scanned = 0;

  while (pos && scanned < maxScanChars) {
    const c = getCharAt(cm, pos);
    if (c === '(') depth++;
    else if (c === ')') {
      depth--;
      if (depth === 0) return pos;
    }
    pos = nextPos(cm, pos);
    scanned++;
  }
  return null;
}

function findEnclosingParenPair(cm, cursor) {
  // Optional: avoid doing this in strings/comments
  const tok = cm.getTokenAt(cursor);
  if (tok?.type && (tok.type.includes("string") || tok.type.includes("comment"))) return null;

  const open = findEnclosingOpenParen(cm, cursor);
  if (!open) return null;

  const close = findMatchingCloseParen(cm, open);
  if (!close) return null;

  // ensure cursor is within [open, close]
  if (!beforeOrEqual(open, cursor) || !afterOrEqual(close, cursor)) return null;

  return { open, close };
}

function updateParenContext(cm, { highlightRange = true } = {}) {
  if (!cm) return;

  const cursor = cm.getCursor("head");

  cancelAnimationFrame(parensRAF);
  parensRAF = requestAnimationFrame(() => {
    const pair = findEnclosingParenPair(cm, cursor);

    if (!pair) {
      clearParenContext();
      cm.state._parenContextPrev = null;
      return;
    }

    const prev = cm.state._parenContextPrev;
    if (prev && samePos(prev.open, pair.open) && samePos(prev.close, pair.close)) return;

    cm.state._parenContextPrev = pair;
    clearParenContext();

    const openFrom = pair.open;
    const openTo   = { line: pair.open.line, ch: pair.open.ch + 1 };
    const closeFrom = pair.close;
    const closeTo   = { line: pair.close.line, ch: pair.close.ch + 1 };

    parenContextMarks.push(cm.markText(openFrom, openTo, { className: "cm-paren-context-edge" }));
    parenContextMarks.push(cm.markText(closeFrom, closeTo, { className: "cm-paren-context-edge" }));

    if (highlightRange) {
      parenContextMarks.push(
        cm.markText(openFrom, closeTo, { className: "cm-paren-context-range" })
      );
    }
  });
}

async function postToServerFile(action, data = {}) {
  const body = new URLSearchParams({ action, ...data }).toString();
  const res = await fetch("ServerFile.jsp", {
    method: "POST",
    headers: {
      "Content-Type": "application/x-www-form-urlencoded; charset=UTF-8",
      "Accept": "application/json"
    },
    body
  });
  const text = await res.text();
  try {
    return JSON.parse(text);
  } catch {
    return { success: false, message: text };
  }
}

async function openServerPathPrompt() {
  const path = prompt("Enter full server file path:", "/home/shaun/.sigmakee/KBs/tests/");
  if (!path) return;

  const res = await postToServerFile("load", { path });
  if (!res?.success) {
    alert("Error opening file:\n" + (res?.message || "Unknown error"));
    return;
  }

  openFileInNewTab(res.name, res.contents || "", "server", res.path);
  closeDropdown();
}

async function saveServerFile(entry, savePath = null) {
  if (!entry) return alert("No active file.");

  const path = savePath || entry.path;
  if (!path) return alert("No server path for this file. Use Save As.");

  const res = await postToServerFile("save", {
    path,
    code: getContent()
  });

  if (!res?.success) {
    alert("Error saving file:\n" + (res?.message || "Unknown error"));
    return;
  }

  entry.path = res.path || path;
  entry[0] = entry.path.split(/[\\/]/).pop();
  entry[1] = getContent();
  entry.lastSaved = entry[1];
  entry.dirty = false;
  entry.source = "server";
  updateTabLabels();
  markTabSaved(activeTab);
  alert("Saved:\n" + entry.path);
}

async function saveServerFileAs(entry) {
  const current = entry?.path || getActiveFileName();
  const path = prompt("Save as full server path:", current);
  if (!path) return;
  await saveServerFile(entry, path);
}
