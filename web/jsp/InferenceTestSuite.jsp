<%@ page import="java.io.*" buffer="64kb" trimDirectiveWhitespaces="true" %>
<%@ page import="java.util.*" %>
<%@ page import="com.articulate.sigma.KB" %>
<%@ page import="com.articulate.sigma.KBmanager" %>
<%@ page import="com.articulate.sigma.tp.TheoremProverController" %>
<%@ page import="com.articulate.sigma.tp.InferenceTestSuite" %>
<%@ page import="com.articulate.sigma.tp.InferenceTest" %>
<%@ page import="com.articulate.sigma.utils.StringUtil" %>
<%@ page import="com.articulate.sigma.utils.ValidationUtils" %>
<%@ include file="fragments/universal/Prelude.jspf" %>
<%!
    private static int langOrder(String lang) {

        if ("fof".equalsIgnoreCase(lang)) return 0;
        if ("tff".equalsIgnoreCase(lang)) return 1;
        if ("thf".equalsIgnoreCase(lang)) return 2;
        return 99;
    }

    private static int statusOrder(String status) {

        if ("NOT RUN".equals(status)) return 0;
        if ("PASS".equals(status)) return 1;
        if ("FAIL".equals(status)) return 2;
        if ("ERROR".equals(status)) return 3;
        return 99;
    }
%>
<%
    String action = request.getParameter("action");
    String runMessage = null;
    if ("reload".equalsIgnoreCase(action)) {
        session.removeAttribute("newITS");
        runMessage = "Reloaded inference tests.";
    }
    if ("reloadKB".equalsIgnoreCase(action)) {
        long t0 = System.currentTimeMillis();
        try {
            kb.deleteUserAssertionsAndReload();
            session.removeAttribute("newITS");
            long millis = System.currentTimeMillis() - t0;
            runMessage = "KB reloaded in " + millis + " ms.";
        }
        catch (Exception e) {
            runMessage = "Error reloading KB: " + e.getMessage();
            e.printStackTrace();
        }
    }
    InferenceTestSuite inferenceTestSuite = (InferenceTestSuite) session.getAttribute("newITS");
    if (inferenceTestSuite == null) {
        inferenceTestSuite = new InferenceTestSuite(kb);
        session.setAttribute("newITS", inferenceTestSuite);
    }
    if ("clearResults".equalsIgnoreCase(action)) {
        inferenceTestSuite.clearAllTestResults();
        runMessage = "Cleared test results.";
    }
    String translationMode = Optional.ofNullable(request.getParameter("translationMode")).orElse(Optional.ofNullable((String) session.getAttribute("translationMode")).orElse("FOL"));
    String TPTPlang = Optional.ofNullable(request.getParameter("TPTPlang")).orElse(Optional.ofNullable((String) session.getAttribute("TPTPlang")).orElse("fof"));
    String cwa = Optional.ofNullable(request.getParameter("CWA")).orElse(Optional.ofNullable((String) session.getAttribute("CWA")).orElse("no"));
    int timeout = ValidationUtils.sanitizeInteger(request.getParameter("timeout"), 30);
    int maxAnswers = ValidationUtils.sanitizeInteger(request.getParameter("maxAnswers"), 1);
    boolean overrideLanguage = "yes".equalsIgnoreCase(request.getParameter("overrideLanguage"));
    boolean overrideTimeout = "yes".equalsIgnoreCase(request.getParameter("overrideTimeout"));
    List<String> availableProvers = TheoremProverController.availableProvers();
    String inferenceEngine = Optional.ofNullable(request.getParameter("inferenceEngine")).orElse("VAMPIRE");
    String vampireMode = Optional.ofNullable(request.getParameter("vampireMode")).orElse("CASC");
    boolean modusPonens = "yes".equalsIgnoreCase(request.getParameter("ModusPonens"));
    boolean dropOnePremise = "true".equalsIgnoreCase(request.getParameter("dropOnePremise"));
    boolean holUseModals = "yes".equalsIgnoreCase(request.getParameter("HolUseModals"));
    session.setAttribute("translationMode", translationMode);
    session.setAttribute("TPTPlang", TPTPlang);
    session.setAttribute("CWA", cwa);
    if ("runOneAjax".equalsIgnoreCase(action)) {
        try {
            out.clearBuffer();
        }
        catch (Exception ignored) {
        }
        response.resetBuffer();
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.setHeader("Cache-Control", "no-store");
        String testPath = request.getParameter("testPath");
        InferenceTest test = testPath == null ? null : inferenceTestSuite.getInferenceTests().get(testPath);
        if (test == null) {
            out.print("{"
                + "\"ok\":false,"
                + "\"status\":\"ERROR\","
                + "\"actual\":\"\","
                + "\"szs\":\"\","
                + "\"time\":\"\","
                + "\"message\":\"Unknown test path\""
                + "}");
            out.flush();
            return;
        }
        String proverType = inferenceEngine;
        String selectedLanguage = "HOL".equalsIgnoreCase(translationMode) ? "thf" : TPTPlang;
        String effectiveLanguage = overrideLanguage ? selectedLanguage : test.minLang;
        int effectiveTimeout = overrideTimeout ? timeout : test.timeout;
        boolean usingVampire = "VAMPIRE".equalsIgnoreCase(inferenceEngine);
        boolean usingTHF = "thf".equalsIgnoreCase(effectiveLanguage);
        boolean usingTFF = "tff".equalsIgnoreCase(effectiveLanguage);
        boolean effectiveModusPonens = usingVampire && !usingTHF && modusPonens;
        boolean effectiveDropOnePremise = effectiveModusPonens && dropOnePremise;
        boolean effectiveHolUseModals = usingVampire && usingTHF && holUseModals;
        boolean effectiveCWA = usingTFF && "yes".equalsIgnoreCase(cwa);
        String message = "";
        try {
            inferenceTestSuite.runTest(testPath, inferenceEngine, effectiveLanguage, vampireMode, effectiveCWA, effectiveModusPonens, effectiveDropOnePremise, effectiveHolUseModals, effectiveTimeout, maxAnswers);
        }
        catch (Throwable t) {
            message = t.getClass().getSimpleName() + ": " + t.getMessage();
            if (test.result == null) test.result = new InferenceTest.InferenceTestResult();
            test.result.success = false;
            test.result.szsStatus = "Exception";
            test.result.proof.add(message);
        }
        String status = (test.errors != null && !test.errors.isEmpty()) ? "ERROR" : (test.result == null ? "NOT RUN" : (test.result.success ? "PASS" : "FAIL"));
        String actual = test.result == null ? "" : String.valueOf(test.result.answers);
        String szs = "";
        if (test.result != null) {
            szs = test.result.szsStatus;
            if (StringUtil.emptyString(szs) || "null".equalsIgnoreCase(szs)) szs = "See Proof for Details";
        }
        String execTime = test.result == null ? "" : String.valueOf(test.result.execTime);
        boolean hasProof = test.result != null && test.result.proof != null && !test.result.proof.isEmpty();
        out.print("{"
            + "\"ok\":true,"
            + "\"testPath\":\"" + ValidationUtils.jsonEsc(testPath) + "\","
            + "\"status\":\"" + ValidationUtils.jsonEsc(status) + "\","
            + "\"actual\":\"" + ValidationUtils.jsonEsc(actual) + "\","
            + "\"szs\":\"" + ValidationUtils.jsonEsc(szs) + "\","
            + "\"time\":\"" + ValidationUtils.jsonEsc(execTime) + "\","
            + "\"hasProof\":" + hasProof + ","
            + "\"message\":\"" + ValidationUtils.jsonEsc(message) + "\""
            + "}");
        out.flush();
        return;
    }
%>
<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <title>Sigma - Inference Test Suite</title>
    <style>
        body{font-family:Arial,Helvetica,sans-serif;background:#fff;color:#222;}
        .selectorRow{display:flex;gap:14px;align-items:stretch;margin:14px 0;}
        .overrideRow{display:flex;gap:14px;align-items:center;flex-wrap:wrap;margin:10px 0;}
        .translationBox{flex:0 0 28%;}
        .proverBox{flex:1;}
        .selectorRow .step{height:100%;margin:0;box-sizing:border-box;}
        @media(max-width:1000px){.selectorRow{flex-direction:column;}.translationBox,.proverBox{flex:1 1 auto;}}
        .pageWrap{width:92%;margin:0 auto;}
        .step{border:1px solid #bbb;border-radius:6px;padding:12px 14px;margin:14px 0;background:#fafafa;}
        .row{margin:8px 0;}
        .inline{display:flex;gap:14px;align-items:center;flex-wrap:wrap;}
        .grid2{display:grid;grid-template-columns:repeat(3,minmax(220px,1fr));gap:12px;}
        .card{border:1px solid #ddd;background:#fff;padding:10px;border-radius:6px;}
        .engineDisabled{opacity:0.5;}
        .sub,.muted,.tiny{color:#666;font-size:12px;}
        .advanced{margin-top:8px;}
        .actions{margin:16px 0;display:flex;gap:10px;align-items:center;}
        .actionBtn{color:#fff; border:none; border-radius:5px; padding:8px 14px; cursor:pointer; font-weight:bold;}
        .runBtn{background:#1d75b8;}
        .clearBtn{background:#6f42c1;}
        .reloadBtn{background:#2e7d32;}
        .reloadKbBtn{background:#b33;}
        .exportBtn{background:#e07a00;}
        .actionBtn:hover{filter:brightness(0.92);}
        .actionBtn:disabled{opacity:0.6;cursor:not-allowed;}
        .message{border:1px solid #ccd;background:#f6f8ff;padding:8px 12px;border-radius:5px;margin:12px 0;}
        .testTable{width:100%;border-collapse:collapse;margin-top:16px;background:#fff;font-size:13px;}
        .testTable th,.testTable td{border:1px solid #ddd;padding:7px 8px;vertical-align:top;text-align:left;}
        .testTable th{background:#f2f2f2;position:sticky;top:0;z-index:1;}
        .testTable tr:nth-child(even){background:#fbfbfb;}
        .testTable tr:hover{background:#f7faff;}
        .fileName{font-weight:bold;}
        .filePath{color:#777;font-size:11px;word-break:break-all;}
        .statusPASS{color:#0a7a21;font-weight:bold;}
        .statusFAIL,.statusERROR{color:#b00020;font-weight:bold;}
        .statusNOTRUN{color:#777;font-weight:bold;}
        .runningStatus{display:flex;align-items:center;gap:6px;color:#1d75b8;font-weight:bold;}
        .statusSpinner{width:14px;height:14px;border:2px solid #ccc;border-top-color:#1d75b8;border-radius:50%;animation:spin 0.8s linear infinite;}
        .sumoBounceSmall{width:24px;height:24px;animation:sumoBounce 0.7s infinite alternate ease-in-out;}
        @keyframes spin{to{transform:rotate(360deg);}}
        @keyframes sumoBounce{from{transform:translateY(0);}to{transform:translateY(-6px);}}
        .errors{margin:4px 0 0 18px;padding:0;color:#b00020;font-weight:normal;}
        .sortable{cursor:pointer;user-select:none;}
        .sortable:hover{text-decoration:underline;}
        code{white-space:pre-wrap;word-break:break-word;}
    </style>
    <script>
        let currentSortKey = 'file';
        let currentSortAsc = true;

        function statusOrderJS(status) {
            if (status === 'NOT RUN') return 0;
            if (status === 'PASS') return 1;
            if (status === 'FAIL') return 2;
            if (status === 'ERROR') return 3;
            return 99;
        }

        function sortTable(key) {
            const tbody = document.querySelector('.testTable tbody');
            if (!tbody) return;
            if (currentSortKey === key) currentSortAsc = !currentSortAsc;
            else {
                currentSortKey = key;
                currentSortAsc = true;
            }
            const rows = Array.from(tbody.querySelectorAll('tr'));
            rows.sort(function(a, b) {
                let av;
                let bv;
                if (key === 'file') {
                    av = a.dataset.file || '';
                    bv = b.dataset.file || '';
                    return currentSortAsc ? av.localeCompare(bv) : bv.localeCompare(av);
                }
                if (key === 'category') {
                    av = a.dataset.category || '';
                    bv = b.dataset.category || '';
                    return currentSortAsc ? av.localeCompare(bv) : bv.localeCompare(av);
                }
                if (key === 'meta') {
                    av = parseInt(a.dataset.langOrder || '99', 10);
                    bv = parseInt(b.dataset.langOrder || '99', 10);
                    return currentSortAsc ? av - bv : bv - av;
                }
                if (key === 'status') {
                    av = parseInt(a.dataset.statusOrder || '99', 10);
                    bv = parseInt(b.dataset.statusOrder || '99', 10);
                    return currentSortAsc ? av - bv : bv - av;
                }
                return 0;
            });
            rows.forEach( function(row) {tbody.appendChild(row); });
            updateSortIndicators();
        }

        function updateSortIndicators() {
            ['file', 'category', 'meta', 'status'].forEach(function(key) {
                const el = document.getElementById('sort_' + key);
                if (!el) return;
                el.innerHTML = key === currentSortKey
                    ? (currentSortAsc ? '&#9650;' : '&#9660;')
                    : '';
            });
        }

        function exportReport() {
            const table = document.querySelector('.testTable');
            if (!table) {
                alert('No test table found.');
                return;
            }
            const html =
                '<!doctype html><html><head><meta charset="utf-8">' +
                '<title>Inference Test Report</title>' +
                '<style>' +
                'body{font-family:Arial,Helvetica,sans-serif;margin:24px;color:#222;}' +
                'table{border-collapse:collapse;width:100%;font-size:13px;}' +
                'th,td{border:1px solid #ddd;padding:8px;vertical-align:top;text-align:left;}' +
                'th{background:#f2f2f2;}' +
                '.statusPASS{color:#0a7a21;font-weight:bold;}' +
                '.statusFAIL,.statusERROR{color:#b00020;font-weight:bold;}' +
                '.statusNOTRUN{color:#777;font-weight:bold;}' +
                '.tiny{font-size:12px;color:#666;}' +
                '.fileName{font-weight:bold;}' +
                '.filePath{color:#777;font-size:11px;word-break:break-all;}' +
                '</style></head><body>' +
                '<h2>Inference Test Report</h2>' +
                '<div class="tiny">Generated: ' + new Date().toString() + '</div><br>' +
                table.outerHTML +
                '</body></html>';
            const blob = new Blob([html], {type: 'text/html'});
            const url = URL.createObjectURL(blob);
            const a = document.createElement('a');
            a.href = url;
            a.download = 'inference-test-report.html';
            document.body.appendChild(a);
            a.click();
            document.body.removeChild(a);
            URL.revokeObjectURL(url);
        }

        function viewTestFile(fileName) {
            const url = 'ViewTest.jsp?name=' + encodeURIComponent(fileName);
            window.open(url, '_blank');
        }

        function toggleAllTests(source) {
            const boxes = document.querySelectorAll('input[name="selectedTests"]');
            boxes.forEach(function(box) {
                box.checked = source.checked;
            });
        }

        function toggleTranslationOptions() {
            const hol = document.getElementById('modeHOL');
            const folOptions = document.getElementById('folOptions');
            const holOptions = document.getElementById('holOptions');
            if (!hol || !folOptions || !holOptions) return;
            if (hol.checked) {
                folOptions.style.display = 'none';
                holOptions.style.display = 'block';
            }
            else {
                folOptions.style.display = 'block';
                holOptions.style.display = 'none';
            }
        }

        function toggleVampireOptions() {
            const vampire = document.getElementById('engineVampire');
            const vampireInputs = document.querySelectorAll(
                'input[name="vampireMode"], #ModusPonens, #dropOnePremise, #HolUseModals'
            );
            vampireInputs.forEach(function(input) {
                input.disabled = vampire && !vampire.checked;
            });
        }

        function statusClassJS(status) {
            return 'status' + status.replace(/\s+/g, '');
        }

        function setRowStatus(rowId, status, message, szs) {
            const statusCell = document.getElementById('status_' + rowId);
            if (!statusCell) return;
            const row = statusCell.closest('tr');
            if (row) row.dataset.statusOrder = statusOrderJS(status);
            statusCell.classList.remove('statusPASS', 'statusFAIL', 'statusERROR', 'statusNOTRUN');
            if (status === 'RUNNING') {
                statusCell.classList.add('statusNOTRUN');
                statusCell.innerHTML =
                    '<div class="runningStatus">' +
                        '<img src="pixmaps/sumo.gif" class="sumoBounceSmall" alt="Running">' +
                        '<span>RUNNING</span>' +
                    '</div>' +
                    '<div class="tiny" id="szs_' + rowId + '"></div>';
                return;
            }
            statusCell.classList.add(statusClassJS(status));
            statusCell.innerHTML = status +
                '<div class="tiny" id="szs_' + rowId + '">' +
                    (szs ? 'SZS: ' + szs : '') +
                '</div>';
            if (message) {
                const div = document.createElement('div');
                div.className = 'tiny';
                div.textContent = message;
                statusCell.appendChild(div);
            }
        }

        async function runSelectedTests() {
            const form = document.getElementById('itsRunnerForm');
            const boxes = Array.from(document.querySelectorAll('input[name="selectedTests"]:checked'));
            if (boxes.length === 0) {
                alert('Select at least one test.');
                return;
            }
            const runButton = document.querySelector('.runBtn');
            if (runButton) {
                runButton.disabled = true;
                runButton.textContent = 'Running...';
            }
            for (const box of boxes) {
                const testPath = box.value;
                const rowId = box.dataset.rowId;
                setRowStatus(rowId, 'RUNNING', '');
                const params = new URLSearchParams(new FormData(form));
                params.set('action', 'runOneAjax');
                params.delete('selectedTests');
                params.set('testPath', testPath);
                try {
                    const response = await fetch('<%= request.getContextPath() %>/InferenceTestSuite.jsp', {
                        method: 'POST',
                        headers: {
                            'Content-Type': 'application/x-www-form-urlencoded',
                            'X-Requested-With': 'XMLHttpRequest'
                        },
                        body: params.toString()
                    });
                    const text = await response.text();
                    let data;
                    try {
                        data = JSON.parse(text);
                    }
                    catch (jsonErr) {
                        console.error('Expected JSON but got:', text);
                        throw new Error('Server returned HTML instead of JSON. Check the Network tab response.');
                    }
                    const actualCell = document.getElementById('actual_' + rowId);
                    const timeCell = document.getElementById('time_' + rowId);
                    const szsCell = document.getElementById('szs_' + rowId);
                    const proofCell = document.getElementById('proof_' + rowId);
                    if (actualCell) actualCell.textContent = data.actual || '';
                    if (timeCell) timeCell.textContent = data.time ? data.time + ' ms' : '';
                    if (proofCell) proofCell.style.display = data.hasProof ? '' : 'none';
                    setRowStatus(rowId, data.status || 'ERROR', data.message || '', data.szs || '');
                    if (szsCell && data.szs) szsCell.textContent = 'SZS: ' + data.szs;
                }
                catch (err) {
                    setRowStatus(rowId, 'ERROR', err.message);
                }
            }
            if (runButton) {
                runButton.disabled = false;
                runButton.textContent = 'Run Selected';
            }
        }

        function reloadKB() {
            if (!confirm('This will fully reload the KB and clear user assertions. Continue?')) return false;
            document.getElementById('itsAction').value = 'reloadKB';
            document.getElementById('itsRunnerForm').submit();
            return false;
        }
        
        window.addEventListener('load', function() {
            toggleTranslationOptions();
            toggleVampireOptions();
            updateSortIndicators();
            const modeFOL = document.getElementById('modeFOL');
            const modeHOL = document.getElementById('modeHOL');
            if (modeFOL) modeFOL.addEventListener('change', toggleTranslationOptions);
            if (modeHOL) modeHOL.addEventListener('change', toggleTranslationOptions);
        });
    </script>
</head>
<body>
<%
    String pageName = "InferenceTestSuite";
    String pageString = "Inference Test Suite";
%>
<%@ include file="fragments/universal/CommonHeader.jspf" %>
<div class="pageWrap">
    <h2>Inference Test Suite</h2>
    <% if (runMessage != null) { %>
        <div class="message"><%= ValidationUtils.escapeHtml(runMessage) %></div>
    <% } %>
    <form id="itsRunnerForm" method="POST" action="InferenceTestSuite.jsp">
        <input type="hidden" id="itsAction" name="action" value="runSelected">
        <div class="selectorRow">
            <div class="translationBox"><%@ include file="fragments/tp/TranslationSelector.jspf" %></div>
            <div class="proverBox"><%@ include file="fragments/tp/ProverSelector.jspf" %></div>
        </div>
        <div class="overrideRow">
            <label>
                <input type="checkbox" name="overrideLanguage" value="yes" <%= overrideLanguage ? "checked" : "" %>>
                Override meta predicate minLang
            </label>
            <label>
                <input type="checkbox" name="overrideTimeout" value="yes" <%= overrideTimeout ? "checked" : "" %>>
                Override meta predicate timeout
            </label>
            <span class="tiny">
                (Unchecked means use each test file's meta predicates/defaults.)
            </span>
        </div>
        <div class="actions">
            <button type="button" class="actionBtn runBtn" onclick="runSelectedTests()">Run Selected</button>
            <button type="submit" class="actionBtn clearBtn" onclick="return confirm('Clear all displayed test results?') && (document.getElementById('itsAction').value='clearResults');">Clear Results</button>
            <button type="submit" class="actionBtn reloadBtn" onclick="document.getElementById('itsAction').value='reload';">Reload Tests</button>
            <button type="button" class="actionBtn reloadKbBtn" onclick="return reloadKB();">Reload KB</button>
            <button type="button" class="actionBtn exportBtn" onclick="exportReport();">Export Report</button>
            <span class="tiny">Showing <%= inferenceTestSuite.getInferenceTests().size() %> inference tests.</span>
        </div>
        <table class="testTable">
            <thead>
                <tr>
                    <th style="width:36px;"> <input type="checkbox" onclick="toggleAllTests(this)"> </th>
                    <th class="sortable" onclick="sortTable('file')">File <span id="sort_file"></span></th>
                    <th class="sortable" onclick="sortTable('category')">Category <span id="sort_category"></span></th>
                    <th class="sortable" onclick="sortTable('meta')">Meta <span id="sort_meta"></span></th>
                    <th class="sortable" onclick="sortTable('status')">Status <span id="sort_status"></span></th>
                    <th>Result</th>
                </tr>
            </thead>
            <tbody>
                <%
                    int rowNum = 0;
                    for (InferenceTest test : inferenceTestSuite.getInferenceTests().values()) {
                        String status = (test.errors != null && !test.errors.isEmpty()) ? "ERROR" : (test.result == null ? "NOT RUN" : (test.result.success ? "PASS" : "FAIL"));
                        String cssClass = "status" + status.replace(" ", "");
                        String rowId = "test_" + rowNum++;
                        String testFileName = StringUtil.removeFilePath(test.filePath);
                        boolean hasProof = test.result != null && test.result.proof != null && !test.result.proof.isEmpty();
                %>
                <tr data-file="<%= ValidationUtils.escapeHtml(testFileName.toLowerCase()) %>"
                    data-category="<%= ValidationUtils.escapeHtml(test.category == null ? "" : test.category.toLowerCase()) %>"
                    data-lang-order="<%= langOrder(test.minLang) %>"
                    data-status-order="<%= statusOrder(status) %>">
                    <td>
                        <input type="checkbox" name="selectedTests" data-row-id="<%= rowId %>" value="<%= ValidationUtils.escapeHtml(test.filePath) %>">
                    </td>
                    <td>
                        <div class="fileName">
                            <a href="javascript:void(0);"
                            onclick="viewTestFile('<%= ValidationUtils.escapeHtml(testFileName) %>')"
                            style="color:#0073e6;text-decoration:underline;">
                                <%= ValidationUtils.escapeHtml(testFileName) %>
                            </a>
                        </div>
                        <div class="filePath"><%= ValidationUtils.escapeHtml(test.filePath) %></div>
                        <% if (test.note != null) { %>
                            <div class="tiny">Note: <%= ValidationUtils.escapeHtml(test.note) %></div>
                        <% } %>
                    </td>
                    <td>
                        <%= ValidationUtils.escapeHtml(test.category) %>
                    </td>
                    <td>
                        <div>file minLang: <b><%= ValidationUtils.escapeHtml(test.minLang) %></b></div>
                        <div>file timeout: <b><%= test.timeout %></b>s</div>
                        <div>regen: <b><%= test.tptpRegenRequired %></b></div>
                    </td>
                    <td id="status_<%= rowId %>" class="<%= cssClass %>">
                        <%= status %>
                        <% if (test.result != null && test.result.szsStatus != null) { %>
                            <div class="tiny" id="szs_<%= rowId %>">SZS: <%= ValidationUtils.escapeHtml(test.result.szsStatus) %></div>
                        <% } else { %>
                            <div class="tiny" id="szs_<%= rowId %>"></div>
                        <% } %>
                        <% if (test.errors != null && !test.errors.isEmpty()) { %>
                            <ul class="errors">
                            <% for (String err : test.errors) { %>
                                <li><%= ValidationUtils.escapeHtml(err) %></li>
                            <% } %>
                            </ul>
                        <% } %>
                        <% if (test.result != null && test.result.contradictionFound) { %>
                            <div class="tiny">Contradiction found</div>
                        <% } %>
                    </td>
                    <td id="result_<%= rowId %>">
                        <div>
                            <b>Expected:</b>
                            <span><%= ValidationUtils.escapeHtml(test.expectedAnswers) %></span>
                        </div>
                        <div>
                            <b>Actual:</b>
                            <span id="actual_<%= rowId %>">
                                <% if (test.result != null) { %>
                                    <%= ValidationUtils.escapeHtml(test.result.answers) %>
                                <% } %>
                            </span>
                        </div>
                        <div>
                            <b>Time:</b>
                            <span id="time_<%= rowId %>">
                                <% if (test.result != null && test.result.execTime > 0) { %>
                                    <%= test.result.execTime %> ms
                                <% } %>
                            </span>
                        </div>
                        <div class="tiny" id="proof_<%= rowId %>" style="<%= hasProof ? "" : "display:none;" %>">
                            <a href="ViewProof.jsp?path=<%= StringUtil.encode(test.filePath) %>" target="_blank" rel="noopener noreferrer"> View proof </a>
                        </div>
                    </td>
                </tr>
            <%
                }
            %>
            </tbody>
        </table>
    </form>
</div>
</body>
</html>