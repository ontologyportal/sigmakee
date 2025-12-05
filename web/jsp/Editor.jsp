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
  <link rel="stylesheet" href="Editor.css?v=1">
</head>
<body>
<%!
  private static String esc(String s) {
    return (s == null) ? "" : s.replace("&","&amp;").replace("<","&lt;").replace(">","&gt;");
  }
%>
<div class="card">

  <form onsubmit="return false;" enctype="multipart/form-data" style="display:none;" id="uploadForm">
    <input type="file" name="kifFile" id="kifFile" accept=".kif,.tptp,.tff,.p,.fof,.cnf,.thf,.txt" required />
  </form>
  <script src="/sigma/javascript/editor.js"></script>
  <script>
    window.initialErrors = [
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
    window.initialErrorMask = [
      <%
        boolean[] mask = (boolean[]) request.getAttribute("errorMask");
        if (mask != null) {
          for (int i = 0; i < mask.length; i++) {
            if (i > 0) out.print(", ");
            out.print(mask[i] ? "true" : "false");
          }
        } %>
      ];
  </script>

    <%
      String errorMessage = (String) request.getAttribute("errorMessage");
      String fileName = (String) request.getAttribute("fileName");
      List<ErrRec> errors = (List<ErrRec>) request.getAttribute("errors");
      List<String> fileContent = (List<String>) request.getAttribute("fileContent");
      String codeContent = (String) request.getAttribute("codeContent");
    %>

    <div class="layout">
      <!-- Editor Menu -->
      <div>
        <div class="editor-header">
          <div class="dropdown" id="fileDropdown">
            <span class="dropdown-file-label" onclick="toggleFileMenu(event)">File</span>
            <span class="dropdown-file-label" onclick="formatBuffer()">Format</span>
            <span class="dropdown-file-label" onclick="openHelpModal()">Help</span>
            <a href="#" onclick="translateKifToTptp()">Translate KIF -> TPTP</a>
            <div class="dropdown-content" id="dropdownContent">
              <div class="submenu">
                <a href="#" class="submenu-label">New ></a>
                <div class="submenu-content">
                  <a href="#" onclick="newFile('kif')">KIF (.kif)</a>
                  <a href="#" onclick="newFile('tptp')">TPTP (.tptp)</a>
                  <a href="#" onclick="newFile('thf')">THF (.thf)</a>
                  <a href="#" onclick="newFile('tff')">TFF (.tff)</a>
                  <a href="#" onclick="newFile('fof')">FOF (.fof)</a>
                  <a href="#" onclick="newFile('cnf')">CNF (.cnf)</a>
                </div>
              </div>
              <a href="#" onclick="openFileModal()">Open File</a>
              <a href="#" onclick="openSaveFileModal()">Save</a>
              <a href="#" onclick="openSaveAsModal()">Save As...</a>
              <a href="#" onclick="downloadFile()">Download</a>
              <a href="#" onclick="triggerFileUpload()">Upload</a>
            </div>
          </div>
          <% if (fileName != null) { %>
          <div class="file-name-display" id="file-name">Uploaded: <%= esc(fileName) %></div>
          <% } else { %>
          <div class="file-name-display" id="file-name"></div>
          <% } %>
        </div>
        <hr class="divider">
        <div class="tab-bar" id="tabBar"></div>
        <!-- Editor -->
        <textarea id="codeEditor" style="display: none;"></textarea>
      </div>

      <!-- Error box -->
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
          Ready to check code.
    <%
          }
        } else {
          java.util.Set<Integer> errorLines = new java.util.HashSet<>();
          for (ErrRec e : errors) {
              if (e.line >= 0)
                  errorLines.add(e.line);
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

<!-- Open File Modal -->
<div id="openFileModal" class="modal-overlay" style="display:none;">
  <div class="modal-window">
    <h3>Select a File</h3>
    <hr class="divider">
    <ul class="file-list">
      <li onclick="openMockFile('example1.kif')">example1.kif</li>
      <li onclick="openMockFile('demo_rules.tptp')">demo_rules.tptp</li>
      <li onclick="openMockFile('test_case.cnf')">test_case.cnf</li>
      <li onclick="openMockFile('experiment.thf')">experiment.thf</li>
    </ul>
    <button class="modal-close" onclick="closeOpenFileModal()">Close</button>
  </div>
</div>

<!-- Save File Modal -->
<div id="saveFileModal" class="modal-overlay" style="display:none;">
  <div class="modal-window">
    <h3>Save File</h3>
    <hr class="divider">

    <label>File name:</label>
    <input id="saveFileNameInput" type="text" style="width:100%; margin-top:6px;">

    <h4 style="margin-top:12px;">Existing Files:</h4>
    <ul id="saveFileList" class="file-list" style="max-height:180px; overflow-y:auto; border:1px solid #ccc; padding:6px;">
      <li>Loading...</li>
    </ul>

    <div style="flex:1; margin-top:10px;">
      <button class="modal-close" onclick="saveFile()">Save</button>
      <button class="modal-close" onclick="closeSaveFileModal()">Cancel</button>
    </div>

  </div>
</div>


<div id="saveAsModal" class="modal-overlay" style="display:none;">
  <div class="modal-window">
    <h3>Save File As</h3>
    <hr class="divider">
    <label>New file name:</label>
    <input id="saveAsFileNameInput" type="text" style="width:100%; margin-top:6px;">
    <h4 style="margin-top:12px;">Existing Files:</h4>
    <ul id="saveAsFileList" class="file-list" style="max-height:180px; overflow-y:auto; border:1px solid #ccc; padding:6px;">
      <li>Loading...</li>
    </ul>
    <button onclick="saveFileAs()">Save</button>
    <button onclick="closeSaveAsModal()">Cancel</button>
  </div>
</div>

<div id="helpModal" class="modal-overlay" style="display:none;">
  <div class="modal-window">
    <h3>Editor Help</h3>
    <hr class="divider">

    <div class="help-content" style="max-height:260px; overflow-y:auto; font-size:0.9rem; line-height:1.4;">
      <p><strong>Tabs & Files</strong></p>
      <ul>
        <li>Click a tab to switch between open files.</li>
        <li><strong>Double-click</strong> a tab to rename the file.</li>
        <li>Drag a tab left/right to reorder tabs.</li>
        <li>Click the <strong>x</strong> on a tab to close it.</li>
        <li>A small dot after the name means the tab has unsaved changes.</li>
      </ul>

      <p style="margin-top:8px;"><strong>File menu</strong></p>
      <ul>
        <li><strong>File -> New</strong>: create a new empty file of the chosen type (KIF, TPTP, THF, TFF, FOF, CNF).</li>
        <li><strong>File -> Open File</strong>: open a file previously saved in your user area.</li>
        <li><strong>File -> Save</strong>: save the current tab to your user directory (overwrites if the name already exists).</li>
        <li><strong>File -> Save As</strong>: save the current content under a new name.</li>
        <li><strong>File -> Download</strong>: download the current buffer as a plain text file.</li>
        <li><strong>File -> Upload</strong>: load a file from your local machine into a new tab.</li>
      </ul>

      <p style="margin-top:8px;"><strong>Editing & Formatting</strong></p>
      <ul>
        <li>The editor automatically chooses syntax highlighting based on the file extension (.kif vs .tptp/.thf/.tff/.fof/.cnf).</li>
        <li>Use the <strong>Format</strong> button in the header to pretty-print the current buffer (KIF or TPTP).</li>
        <li>After you stop typing for about 2 seconds, the editor automatically runs checks.</li>
        <li>Errors and warnings appear in the right-hand panel and are highlighted in the gutter and text.</li>
      </ul>

      <p style="margin-top:8px;"><strong>Checks & Errors</strong></p>
      <ul>
        <li>Red lines / text indicate errors; yellow indicates warnings.</li>
        <li>Each error shows file, line, and column, plus a message.</li>
        <li>If the buffer is empty, the checker does nothing.</li>
      </ul>
    </div>

    <div style="margin-top:12px; text-align:right;">
      <button class="modal-close" onclick="closeHelpModal()">Close</button>
    </div>
  </div>
</div>

<%@ include file="Postlude.jsp" %>
</body>
</html>