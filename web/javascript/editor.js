const fileInput = document.getElementById('kifFile');
const fileNameSpan = document.getElementById('file-name');
const uploadForm = document.getElementById('uploadForm');
let errorMarks = [];
let codeEditors = [];
let activeTab = 0;

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
    console.log(`📄 Loaded file: ${file.name} (${contents.length} chars)`);
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
  console.log("📘 Using mode:", codeEditor.getOption("mode"));
  codeEditors.push("");
});

document.addEventListener("click", (e) => {
  const dropdown = document.getElementById("fileDropdown");
  const content = document.getElementById("dropdownContent");
  if (!dropdown.contains(e.target)) {
    content.style.display = "none";
  }
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

  // Extract keyword sets by tag name
  const tagTypes = ['KEYWORD1', 'KEYWORD2', 'KEYWORD3', 'KEYWORD4', 'LITERAL1', 'LITERAL2', 'COMMENT1', 'FUNCTION'];
  const keywords = {};
  for (const tag of tagTypes)
    keywords[tag] = [...xml.getElementsByTagName(tag)].map(n => n.textContent);

  const commentStart = xml.querySelector('PROPERTY[NAME="lineComment"]')?.getAttribute('VALUE') || ';';

  // Simple color classes mapping to your CSS
  const classMap = {
    'KEYWORD1': 'keyword1',
    'KEYWORD2': 'keyword2',
    'KEYWORD3': 'keyword3',
    'KEYWORD4': 'keyword4',
    'COMMENT1': 'comment',
    'LITERAL1': 'string',
    'LITERAL2': 'literal',
    'FUNCTION': 'function',
  };

  // Register mode
  CodeMirror.defineMode(modeName, function() {
    return {
      token: function(stream, state) {
        // Comments
        if (commentStart && stream.match(new RegExp(`${commentStart}.*`))) return classMap['COMMENT1'];

        // Literals
        if (stream.match(/"(?:[^"\\]|\\.)*"/)) return classMap['LITERAL1'];
        if (stream.match(/'(?:[^'\\]|\\.)*'/)) return classMap['LITERAL1'];

        // Numbers
        if (stream.match(/\b\d+(?:\.\d+)?\b/)) return "number";

        // Variables
        if (stream.match(/\?[A-Za-z0-9_-]+/)) return classMap['KEYWORD4'];

        // Keywords
        for (const type in keywords) {
          for (const word of keywords[type]) {
            console.log("Word: " + word + ", Type:" + type);
            if (stream.match(new RegExp(`\\b${word}\\b`))) return classMap[type];
          }
        }

        // Brackets
        if (stream.match(/[(){}\[\]]/)) return "bracket";

        // Default
        stream.next();
        return null;
      }
    };
  });

  console.log(`✅ Mode "${modeName}" loaded from ${xmlPath}`);
}


// ------------------------------------------------------------------
// Editor Functions

let codeEditor;
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
  highlightErrorLines();
}

let errors = window.initialErrors || [];
let errorMask = window.initialErrorMask || [];

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

  if (errorMask && errorMask.length > 0 && codeEditor) {
    codeEditor.eachLine(lineHandle => codeEditor.removeLineClass(lineHandle, "gutter", "error-line-gutter"));
    for (let i = 0; i < errorMask.length; i++)
      if (errorMask[i])
        codeEditor.addLineClass(i, "gutter", "error-line-gutter");
  }
}

function refreshErrorHighlighting() {
  if (codeEditor)
    setTimeout(highlightErrorLines, 100);
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

function triggerFileUpload() {
document.getElementById('kifFile').click();
}

async function check() {
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

async function formatBuffer() {
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
  if (!fileName) {
    console.error("openFileInNewTab: missing file name.");
    return;
  }

  // Save current buffer before switching
  if (codeEditors.length > activeTab && codeEditor) {
    codeEditors[activeTab] = codeEditor.getValue();
  }

  // Create the new tab
  addTab(fileName);

  // Set tab content and mode
  const ext = fileName.split('.').pop().toLowerCase();
  const mode = ext === 'kif' ? 'kif' : 'tptp';
  codeEditor.setOption('mode', mode);
  codeEditor.setValue(fileContents || "");

  // Store it in the editors array
  codeEditors[activeTab] = fileContents || "";

  // Update the filename display
  if (fileNameSpan) fileNameSpan.textContent = 'Opened: ' + fileName;

  console.log(`✅ Created new tab "${fileName}" with ${fileContents?.length || 0} chars`);
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
