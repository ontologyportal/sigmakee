<%@ page import="com.articulate.sigma.nlg.LanguageFormatter" %>
<%@ page import="com.articulate.sigma.utils.StringUtil" %>
<%@ page import="com.articulate.sigma.InferenceTestSuite" %>
<%@ page import="java.io.File, java.util.Arrays, java.util.ArrayList, java.util.Comparator, java.util.List, java.util.Set" %>
<%@include file="Prelude.jsp" %>
<%
    /** Copyright header omitted for brevity; keep your original text **/

    if (!role.equalsIgnoreCase("admin")) {
        response.sendRedirect("login.html");
        return;
    }

    String systemsDir = KBmanager.getMgr().getPref("systemsDir");
%>
<html>
<head>
    <title>Sigma Knowledge Engineering Environment - Ask/Tell</title>

    <style>
        fieldset { border:1px solid #bbb; padding:10px; margin:12px 0; }
        legend { font-weight:600; }
        .row { margin:6px 0; }
        .muted { color:#666; font-size:0.9em; }
        /* Layout helpers */
        .step { margin: 14px 0; }
        .helpText { color:#555; font-size:0.92em; margin-top:4px; }
        .grid2 { display: grid; grid-template-columns: 1fr 1fr; gap: 12px; }
        .card { border:1px solid #d8dee4; border-radius:8px; padding:12px 14px; background:#fff; }
        .card h4 { margin:0 0 8px; font-size:1.0rem; }
        .card .sub { color:#666; font-size:0.9em; margin:4px 0 0; }
        .pill { display:inline-block; padding:2px 8px; border:1px solid #cfd7de; border-radius:999px; font-size:0.85em; color:#444; }
        details.advanced { margin-top: 10px; }
        details.advanced summary { cursor: pointer; font-weight: 600; }
        .inline { display:inline-flex; gap:12px; align-items:center; flex-wrap:wrap; }
        .engineDisabled { opacity: .55; }
    </style>

    <style>
        .proof-thumb-wrap { margin: 8px 0 14px; display: inline-block; position: relative; }
        .proof-thumb { max-width: 400px; height: auto; border:1px solid #ccc; border-radius:4px;
            box-shadow: 0 1px 3px rgba(0,0,0,.15); cursor: zoom-in; }
        .proof-badge {
            position: absolute; left: 6px; top: 6px; background: rgba(0,0,0,.65);
            color:#fff; font-size:12px; padding:2px 6px; border-radius:3px;
        }
        .proof-caption { color:#666; font-size:.9em; margin-top:4px; }
    </style>

    <style>
        .spin-overlay {
            position: fixed;
            inset: 0;
            background: rgba(255,255,255,.95);
            display: none;
            z-index: 9999;
        }
        /*body.busy .spin-overlay { display: block; }*/

        .spin-card {
            position: absolute;
            left: 50%;
            top: 42%;
            transform: translate(-50%,-50%);
            background: #fff;
            border: 1px solid #d8dee4;
            border-radius: 10px;
            padding: 16px 18px;
            min-width: 420px;
            box-shadow: 0 6px 22px rgba(0,0,0,.10);
        }

        .spin-row { display: flex; align-items: center; gap: 12px; margin-bottom: 10px; }
        .bounce-icon { width: 56px; height: 56px; animation: bounce 1.2s ease-in-out infinite; }
        @keyframes bounce {
            0%,100% { transform: scale(1); }
            50% { transform: translateY(-6px) scale(1.06); }
        }

        .spin-title { font-weight: 700; margin: 0; }
        .spin-sub { color:#666; font-size: 0.92em; margin-top: 2px; }

        .bar-wrap {
            height: 10px;
            background: #eef2f6;
            border-radius: 999px;
            overflow: hidden;
            border: 1px solid #dde5ee;
        }
        .bar-fill {
            height: 100%;
            width: 0%;
            background: #4a90e2;
            transition: width 80ms linear;
        }

        .spin-meta {
            display:flex;
            justify-content: space-between;
            margin-top: 8px;
            color:#666;
            font-size: 0.9em;
        }
    </style>

    <style>
        .answers-card {
            border:1px solid #d8dee4; border-radius:6px; padding:12px 14px; margin:10px 0 18px;
            background:#f8fafc;
        }
        .answers-card h3 { margin:0 0 8px; font-size:1.05rem; font-weight:600; }
        .answers-list { margin:0; padding-left:20px; }
        .answers-list li { margin:4px 0; }
        .answers-empty { color:#666; font-size:.95em; }
        .answers-meta { color:#666; font-size:.9em; margin-top:6px; }
    </style>

    <script>
        function toggleVampireOptions() {
            const vamp   = document.querySelector('input[name="inferenceEngine"][value="Vampire"]');
            const casc   = document.getElementById('CASC');
            const avatar = document.getElementById('Avatar');
            const custom = document.getElementById('Custom');
            const mp     = document.getElementById('ModensPonens');
            const drop   = document.getElementById('dropOnePremise');
            const holModal = document.getElementById('HolUseModals');

            // NEW: check if .thf filter is selected
            const thfRadio = document.querySelector('input[name="testFilter"][value="thf"]');
            const isThf = thfRadio && thfRadio.checked;

            // NEW: Translation Mode toggle (FOL vs HOL)
            const holRadio = document.getElementById('modeHOL');
            const isHolMode = holRadio && holRadio.checked;

            // If THF file OR HOL Translation Mode → always disable Modus Ponens + Drop One Premise
            if (isThf || isHolMode) {
                if (mp)   { mp.checked = false;   mp.disabled = true; }
                if (drop) { drop.checked = false; drop.disabled = true; }

                // Still keep Vampire mode radios tied to Vampire on/off
                const vampireOn = vamp && vamp.checked && !vamp.disabled;
                [casc, avatar, custom, holModal].forEach(el => { if (el) el.disabled = !vampireOn; });

                custom.checked = false; custom.disabled = true;
                return; // THF overrides the rest of the logic
            }

            // Original behavior when NOT in THF mode
            const vampireOn = vamp && vamp.checked && !vamp.disabled;
            [casc, avatar, custom, mp].forEach(el => { if (el) el && (el.disabled = !vampireOn); });

            const mpOn = vampireOn && mp && mp.checked;
            if (drop) {
                drop.disabled = !mpOn;
                if (!mpOn) drop.checked = false;
            }
            // Modal is Disabled on Non-HOL option
            holModal.checked = false; holModal.disabled = true;
            // Disable Custom until it gets fixed and tested!
            custom.checked = false; custom.disabled = true;
        }

        function toggleRunSource() {
            const src  = document.querySelector('input[name="runSource"]:checked')?.value || 'custom';
            const ta   = document.getElementById('stmtArea');
            const test = document.getElementById('testName');
            const lblC = document.getElementById('lblCustom');
            const lblT = document.getElementById('lblTest');
            const isTest = (src === 'test');
            ta.disabled   = isTest;
            test.disabled = !isTest;
            lblC.style.opacity = isTest ? .5 : 1;
            lblT.style.opacity = isTest ? 1 : .5;
        }


        function toggleTranslationMode() {
            const fol = document.getElementById('modeFOL');
            const hol = document.getElementById('modeHOL');
            const folBlock = document.getElementById('folOptions');
            const holBlock = document.getElementById('holOptions');

            const leo = document.getElementById('engineLEO');
            const epr = document.getElementById('engineEProver');
            const vamp = document.getElementById('engineVampire');

            const isHol = hol && hol.checked;

            if (folBlock) folBlock.style.display = isHol ? 'none' : 'block';
            if (holBlock) holBlock.style.display = isHol ? 'block' : 'none';

            // In HOL mode, only Vampire is currently supported.
            if (isHol) {
                if (vamp) vamp.checked = true;
                [leo, epr].forEach(el => { if (el) { el.dataset.prevDisabled = String(el.disabled); el.disabled = true; } });
            } else {
                // Restore engine availability (server may still disable missing binaries)
                [leo, epr].forEach(el => { if (el) { const prev = el.dataset.prevDisabled; if (prev !== undefined) el.disabled = (prev === "true"); } });
            }

            // Re-apply dependent enabling/disabling
            toggleVampireOptions();
        }

        function filterTestsByExtension(ext) {
            const select = document.getElementById("testFile");
            let hasVisibleSelected = false;

            for (let opt of select.options) {
                if (ext === "all") {
                    opt.style.display = "";
                    hasVisibleSelected = hasVisibleSelected || opt.selected;
                } else {
                    const show = opt.value.endsWith("." + ext);
                    opt.style.display = show ? "" : "none";
                    if (show && opt.selected) hasVisibleSelected = true;
                }
            }

            // reset selection if current one is hidden
            if (!hasVisibleSelected) {
                select.selectedIndex = 0;
            }
        }

        document.querySelectorAll("input[name='testFilter']").forEach(radio => {
            radio.addEventListener("change", () => {
                filterTestsByExtension(radio.value);
            });
        });

        window.onload = function () {
            toggleVampireOptions();
            toggleRunSource();
            toggleTranslationMode();

            document.querySelectorAll('input[name="inferenceEngine"], #ModensPonens')
                .forEach(el => el.addEventListener('change', toggleVampireOptions));

            // NEW: when .thf / other filters change, re-apply logic
            document.querySelectorAll('input[name="testFilter"]')
                .forEach(el => el.addEventListener('change', toggleVampireOptions));

            document.querySelectorAll('input[name="runSource"]')
                .forEach(el => el.addEventListener('change', toggleRunSource));

            document.querySelectorAll('input[name="translationMode"]')
                .forEach(el => el.addEventListener('change', toggleTranslationMode));
        };
    </script>

    <script>
        function viewSelectedTest() {
            const sel = document.getElementById('testName');
            if (!sel || !sel.value) return;
            const name = sel.value.toLowerCase();
            // .tq via ViewTest.jsp, others directly from /tests/
            const url = name.endsWith('.tq') || name.endsWith('.tptp') || name.endsWith('.tff') || name.endsWith('.thf')
                ? ('ViewTest.jsp?name=' + encodeURIComponent(sel.value))
                : ('tests/' + encodeURIComponent(sel.value));
            window.open(url, '_blank');
        }
    </script>

    <script>
        window.addEventListener('load', function(){
            document.body.classList.remove('busy');
        });
    </script>

</head>
<%
    System.out.println("INFO in AskTell.jsp");

    StringBuilder status = new StringBuilder();
    String req = request.getParameter("request");

    // ---- Run source + persist in session ----
    String runSource = request.getParameter("runSource");
    if (runSource == null) runSource = (String) session.getAttribute("runSource");
    if (runSource == null) runSource = "custom";
    session.setAttribute("runSource", runSource);

    // ---- Statement + basics ----
    String stmt = request.getParameter("stmt");
    String cwa = request.getParameter("CWA");
    String isModal = request.getParameter("isModal");

    // ---- Modens Ponens (persist) ----
    Boolean modensPonens = (Boolean) session.getAttribute("ModensPonens");
    if (req != null) {
        modensPonens = request.getParameter("ModensPonens") != null
                || "yes".equalsIgnoreCase(request.getParameter("ModensPonens"))
                || "on".equalsIgnoreCase(request.getParameter("ModensPonens"))
                || "true".equalsIgnoreCase(request.getParameter("ModensPonens"));
        session.setAttribute("ModensPonens", modensPonens);
    }
    if (modensPonens == null) modensPonens = false;
    KB.modensPonens = modensPonens;

    // ---- Drop-one-premise (persist + global) ----
    Boolean dropOnePremise = (Boolean) session.getAttribute("dropOnePremise");
    if (req != null) {
        dropOnePremise = request.getParameter("dropOnePremise") != null;
        session.setAttribute("dropOnePremise", dropOnePremise);
    }
    if (dropOnePremise == null) dropOnePremise = false;
    KB.dropOnePremiseFormulas = dropOnePremise;

    Boolean holUseModals = (Boolean) session.getAttribute("HolUseModals");
    if (req != null) {
        holUseModals = request.getParameter("HolUseModals") != null
                || "yes".equalsIgnoreCase(request.getParameter("HolUseModals"))
                || "on".equalsIgnoreCase(request.getParameter("HolUseModals"))
                || "true".equalsIgnoreCase(request.getParameter("HolUseModals"));
        session.setAttribute("HolUseModals", holUseModals);
    }
    if (holUseModals == null) holUseModals = false;

    // ---- Remember selected test in session ----
    String selectedTest = (String) session.getAttribute("selectedTest");
    if (req != null && request.getParameter("testName") != null) {
        selectedTest = request.getParameter("testName");
        session.setAttribute("selectedTest", selectedTest);
    }

    // ---- CWA ----
    if (StringUtil.emptyString(cwa)) cwa = "no";
    SUMOKBtoTPTPKB.CWA = "yes".equals(cwa);
    
    // ---- Modal ---- 
    if (StringUtil.emptyString(isModal)) isModal = "no";
    THFnew.isModal = "yes".equals(isModal);
    
    // ---- Engine / modes / language ----
    String inferenceEngine = request.getParameter("inferenceEngine");
    String vampireMode = request.getParameter("vampireMode");
    if (StringUtil.emptyString(vampireMode)) vampireMode = "CASC";
    String TPTPlang = request.getParameter("TPTPlang");
    if (StringUtil.emptyString(TPTPlang) || TPTPlang.equals("fof")) {
        TPTPlang = "fof";
        SUMOformulaToTPTPformula.lang = "fof";
        SUMOKBtoTPTPKB.lang = "fof";
    }
    if ("tff".equals(TPTPlang)) {
        SUMOformulaToTPTPformula.lang = "tff";
        SUMOKBtoTPTPKB.lang = "tff";
    }

    boolean syntaxError = false, english = false;
    String englishStatement = null;
    int maxAnswers = 1, timeout = 30;

    Boolean showEnglish = (Boolean) session.getAttribute("showProofInEnglish");
    if (req != null) {
        showEnglish = "yes".equalsIgnoreCase(request.getParameter("showProofInEnglish"));
        session.setAttribute("showProofInEnglish", showEnglish);
    }
    if (showEnglish == null) showEnglish = true;

    Boolean llmProof = (Boolean) session.getAttribute("showProofFromLLM");
    if (req != null) {
        llmProof = "yes".equalsIgnoreCase(request.getParameter("showProofFromLLM"));
        session.setAttribute("showProofFromLLM", llmProof);
    }
    if (llmProof == null) llmProof = false;

    boolean ollamaUp = false;
    try { ollamaUp = LanguageFormatter.checkOllamaHealth(); }
    catch (Exception ignore) { ollamaUp = false; }
    if (!ollamaUp) { llmProof = false; session.setAttribute("showProofFromLLM", false); }

    Boolean showProofSummary = (Boolean) session.getAttribute("showProofSummary");
    if (req != null) {
        showProofSummary = "yes".equalsIgnoreCase(request.getParameter("showProofSummary"));
        session.setAttribute("showProofSummary", showProofSummary);
    }
    if (showProofSummary == null) showProofSummary = false;


    String eproverExec = KBmanager.getMgr().getPref("eprover");
    File epFile = new File(eproverExec);
    if (kb.eprover == null && epFile.exists())
        kb.eprover = new com.articulate.sigma.tp.EProver(eproverExec);

    String leoExec = KBmanager.getMgr().getPref("leoExecutable");
    if (!StringUtil.emptyString(leoExec)) {
        File leoFile = new File(leoExec);
        if (kb.leo == null && leoFile.exists()) {
            kb.leo = new com.articulate.sigma.tp.LEO();
        }
    }

    if (inferenceEngine == null) inferenceEngine = "Vampire";

    if (request.getParameter("maxAnswers") != null)
        maxAnswers = Integer.parseInt(request.getParameter("maxAnswers"));
    if (request.getParameter("timeout") != null)
        timeout= Integer.parseInt(request.getParameter("timeout"));

    if ((kbName == null) || kbName.equals("")) { System.out.println("Error: No KB specified"); return; }

    if (stmt == null || stmt.equalsIgnoreCase("null")) stmt = "(instance ?X Relation)";
    else {
        if (stmt.trim().charAt(0) != '(') english = true;
        else {
            String msg = (new KIF()).parseStatement(stmt);
            if (msg != null) { status.append("<font color='red'>Error: ").append(msg).append("</font><br>"); syntaxError = true; }
        }
    }
    if (english) {
        englishStatement = stmt;
        if (!KBmanager.getMgr().getPref("loadCELT").equalsIgnoreCase("yes") || kb.celt == null) {
            stmt = null;
            status.append("<font color='red'>CELT not loaded.  Only KIF syntax is allowed.</font><br>");
        } else stmt = kb.celt.submit(stmt);
    }
    if (stmt == null || stmt.length() < 2 || stmt.trim().charAt(0) != '(') {
        syntaxError = true;
        status.append("<font color='red'>Syntax Error or parsing failure in: ").append(englishStatement).append("</font><br>");
        stmt = englishStatement;
    }

    String testFilter = request.getParameter("testFilter");
    if (testFilter == null) testFilter = (String) session.getAttribute("testFilter");
    if (testFilter == null) testFilter = "all";
    session.setAttribute("testFilter", testFilter);


    // ---- Translation mode (persist) ----
    // FOL: standard TPTP/TFF pipeline.  HOL: THF pipeline via Vampire HOL.
    String translationMode = request.getParameter("translationMode");
    if (translationMode == null) translationMode = (String) session.getAttribute("translationMode");
    if (translationMode == null) translationMode = "FOL";
    session.setAttribute("translationMode", translationMode);

    com.articulate.sigma.tp.EProver eProver = null;
    com.articulate.sigma.tp.Vampire vampire = null;

    String lineHtml =
            "<table ALIGN='LEFT' WIDTH='40%'><tr><TD BGCOLOR='#AAAAAA'><IMG SRC='pixmaps/1pixel.gif' width=1 height=1 border=0></TD></tr></table><BR>\n";

    // ---- Global flags for paraphrasing ----
    System.out.println("AskTell.jsp / showProofInEnglish = "+showEnglish);
    HTMLformatter.proofParaphraseInEnglish = showEnglish;
    System.out.println("AskTell.jsp / showProofFromLLM = "+llmProof);
    LanguageFormatter.paraphraseLLM = llmProof;

    boolean busy = "Run".equalsIgnoreCase(req);
%>


<body class="<%= busy ? "busy" : "" %>" aria-busy="<%= busy %>">


<div id="loading" class="spin-overlay" aria-live="polite" aria-atomic="true">
    <div class="spin-card">
        <div class="spin-row">
            <img src="pixmaps/sumo.gif" class="bounce-icon" alt="Loading...">
            <div>
                <div class="spin-title">Running inference...</div>
                <div id="spinSub" class="spin-sub">Time limit: <span id="spinLimit">30</span>s</div>
            </div>
        </div>

        <div class="bar-wrap">
            <div id="spinBar" class="bar-fill"></div>
        </div>

        <div class="spin-meta">
            <span id="spinPct">0%</span>
            <span id="spinEta">~30s remaining</span>
        </div>
    </div>
</div>

<form name="AskTell" id="AskTell" action="AskTell.jsp" method="POST">
    <%
        String pageName = "AskTell";
        String pageString = "Inference Interface";
    %>
    <%@include file="CommonHeader.jsp" %>
    <table ALIGN="LEFT" WIDTH=80%><tr><TD BGCOLOR='#AAAAAA'>
        <IMG SRC='pixmaps/1pixel.gif' width=1 height=1 border=0></TD></tr></table><BR>


    <!-- ===== STEP 1: INPUT ===== -->
    <fieldset class="step">
        <legend>Step 1 - Input</legend>

        <div class="row inline">
            <label><input type="radio" name="runSource" value="custom"
                <%= "test".equals(session.getAttribute("runSource")) ? "" : "checked" %> >
                Custom query</label>

            <label><input type="radio" name="runSource" value="test"
                <%= "test".equals(session.getAttribute("runSource")) ? "checked" : "" %> >
                Saved test file</label>

            <span class="pill">.tq / .tptp / .tff / .thf</span>
        </div>

        <div class="row" id="lblCustom">
            <textarea rows="5" cols="80" name="stmt" id="stmtArea"><%=stmt%></textarea>
            <div class="helpText">Enter a KIF query..</div>
        </div>

        <%
            String testDir = KBmanager.getMgr().getPref("inferenceTestDir");
            File[] allFiles = (testDir == null) ? new File[0]
                    : new File(testDir).listFiles((d,n) -> n.endsWith(".tq") || n.endsWith(".tptp") || n.endsWith(".tff") || n.endsWith(".thf"));
            if (allFiles == null) allFiles = new File[0];

            File[] testFiles = allFiles;
            Arrays.sort(testFiles, Comparator.comparing(File::getName));
            if (selectedTest == null && testFiles.length > 0) selectedTest = testFiles[0].getName();
        %>

        <div class="row" id="lblTest">
            <div class="inline">
                <label><b>Test:</b>
                    <select name="testName" id="testName">
                        <% for (File f : testFiles) {
                            String fname = f.getName();
                            boolean sel = fname.equals(selectedTest);
                        %>
                        <option value="<%= fname %>" <%= sel ? "selected" : "" %>><%= fname %></option>
                        <% } %>
                    </select>
                </label>

                <a href="javascript:void(0)" onclick="viewSelectedTest()"
                   style="text-decoration:underline; color:#0073e6;">
                    View
                </a>

                <span class="muted">Applies the configuration below.</span>
            </div>

            <div class="row">
                <span class="muted">Filter:</span>
                <label><input type="radio" name="testFilter" value="all"  <%= "all".equalsIgnoreCase(testFilter)  ? "checked":"" %>> All</label>
                <label><input type="radio" name="testFilter" value="tq"   <%= "tq".equalsIgnoreCase(testFilter)   ? "checked":"" %>> tq</label>
                <label><input type="radio" name="testFilter" value="tptp" <%= "tptp".equalsIgnoreCase(testFilter) ? "checked":"" %>> tptp</label>
                <label><input type="radio" name="testFilter" value="tff"  <%= "tff".equalsIgnoreCase(testFilter)  ? "checked":"" %>> tff</label>
                <label><input type="radio" name="testFilter" value="thf"  <%= "thf".equalsIgnoreCase(testFilter)  ? "checked":"" %>> thf</label>
            </div>
            <input type="hidden" name="testFilter" id="testFilterHidden" value="<%= testFilter %>">
        </div>
    </fieldset>

    <!-- ===== STEP 2: TRANSLATION MODE ===== -->
    <fieldset class="step">
        <legend>Step 2 - Translation mode</legend>

        <div class="row inline">
            <label><input type="radio" id="modeFOL" name="translationMode" value="FOL"
                <%= "HOL".equalsIgnoreCase((String)session.getAttribute("translationMode")) ? "" : "checked" %> >
                FOL (TPTP / TFF)</label>

            <label><input type="radio" id="modeHOL" name="translationMode" value="HOL"
                <%= "HOL".equalsIgnoreCase((String)session.getAttribute("translationMode")) ? "checked" : "" %> >
                HOL (THF)</label>
        </div>

        <div id="folOptions" class="row">
            <div class="inline">
                <span class="muted">Language:</span>
                <label><input type="radio" id="langFof" name="TPTPlang" value="fof" <%= "fof".equals(TPTPlang)?"checked":"" %> >
                    tptp (fof)</label>

                <label><input type="radio" id="langTff" name="TPTPlang" value="tff" <%= "tff".equals(TPTPlang)?"checked":"" %> >
                    tff</label>

                <label class="inline" style="margin-left:14px;">
                    <input type="checkbox" name="CWA" id="CWA" value="yes" <% if ("yes".equals(cwa)) {%>checked<%}%> >
                    Closed World Assumption
                </label>
                
                <label class="inline" style="margin-left:14px;">
                    <input type="checkbox" name="Modal" id="Modal" value="yes" <% if ("yes".equals(isModal)) {%>checked<%}%> >
                    Modal
                </label>
            </div>
        </div>

        <div id="holOptions" class="row" style="display:none;">
            <div class="helpText">
                HOL uses THF translation and currently requires Vampire (HOL-enabled build). "Modus Ponens" and
                "Drop One-Premise" are disabled in HOL mode.
            </div>
        </div>
    </fieldset>

    <!-- ===== STEP 3: REASONER ===== -->
    <fieldset class="step">
        <legend>Step 3 - Reasoner</legend>

        <div class="row inline">
            <label>Maximum answers:
                <input type="text" name="maxAnswers" value="<%=maxAnswers%>" size="4">
            </label>

            <label>Query time limit (sec):
                <input type="text" name="timeout" value="<%=timeout%>" size="4">
            </label>
        </div>

        <div class="row grid2">
            <div class="card <%= (kb.leo == null ? "engineDisabled" : "") %>">
                <h4>
                    <label>
                        <input type="radio" id="engineLEO" name="inferenceEngine" value="LEO" <% if ("LEO".equals(inferenceEngine)) {%>checked<%}%>
                            <% if (kb.leo == null) { %> disabled <% } %>
                               onclick="toggleVampireOptions()">
                        LEO-III
                    </label>
                </h4>
                <div class="sub">Higher-order prover (available if configured).</div>
            </div>

            <div class="card <%= (kb.eprover == null ? "engineDisabled" : "") %>">
                <h4>
                    <label>
                        <input type="radio" id="engineEProver" name="inferenceEngine" value="EProver" <% if ("EProver".equals(inferenceEngine)) {%>checked<%}%>
                            <% if (kb.eprover == null) { %> disabled <% } %>
                               onclick="toggleVampireOptions()">
                        EProver
                    </label>
                </h4>
                <div class="sub">First-order prover (fof/tff).</div>
            </div>

            <div class="card <%= (KBmanager.getMgr().getPref("vampire") == null ? "engineDisabled" : "") %>">
                <h4>
                    <label>
                        <input type="radio" id="engineVampire" name="inferenceEngine" value="Vampire" <% if ("Vampire".equals(inferenceEngine)) {%>checked<%}%>
                            <% if (KBmanager.getMgr().getPref("vampire") == null) { %> disabled <% } %>
                               onclick="toggleVampireOptions()">
                        Vampire
                    </label>
                </h4>

                <div class="inline" style="margin-top:6px;">
                    <span class="muted">Mode:</span>
                    <label><input type="radio" id="CASC" name="vampireMode" value="CASC" <% if ("CASC".equals(vampireMode)) { out.print(" CHECKED"); } %> > CASC</label>
                    <label><input type="radio" id="Avatar" name="vampireMode" value="Avatar" <% if ("Avatar".equals(vampireMode)) { out.print(" CHECKED"); } %> > Avatar</label>
                    <label title="Disabled until fully tested.">
                        <input type="radio" id="Custom" name="vampireMode" value="Custom" <% if ("Custom".equals(vampireMode)) { out.print(" CHECKED"); } %> > Custom
                    </label>
                </div>

                <details class="advanced">
                    <summary>Advanced (Vampire)</summary>
                    <div class="row">
                        <label>
                            <input type="checkbox" id="ModensPonens" name="ModensPonens" value="yes" <% if (modensPonens) { out.print(" CHECKED"); } %> >
                            Modus Ponens
                        </label>
                        <span title="Runs Vampire with modus-ponens-only routine over authored axioms">&#9432;</span>

                        <label style="margin-left:14px;">
                            <input type="checkbox" name="dropOnePremise" id="dropOnePremise" value="true" <% if (Boolean.TRUE.equals(dropOnePremise)) { out.print(" CHECKED"); } %> >
                            Drop one-premise formulas
                        </label>
                    </div>

                    <div class="row">
                        <label>
                            <input type="checkbox" id="HolUseModals" name="HolUseModals" value="yes" <% if (holUseModals) { out.print(" CHECKED"); } %> >
                            HOL - use modals
                        </label>
                        <span class="muted">Only relevant in HOL mode.</span>
                    </div>
                </details>
            </div>
        </div>
    </fieldset>

    <!-- ===== STEP 4: OUTPUT OPTIONS ===== -->
    <fieldset class="step">
        <legend>Step 4 - Output</legend>

        <div class="row">
            <label>
                <input type="checkbox" name="showProofInEnglish" value="yes" <% if (Boolean.TRUE.equals(showEnglish)) { %>checked<% } %> >
                Show English paraphrases
            </label>
        </div>

        <div class="row inline">
            <label>
                <input type="checkbox" name="showProofFromLLM" value="yes"
                    <%= (Boolean.TRUE.equals(llmProof) && ollamaUp) ? "checked" : "" %>
                    <%= ollamaUp ? "" : "disabled" %> >
                Use LLM for paraphrasing
            </label>
            <% if (!ollamaUp) { %><span title="Ollama is not running.">&#9432;</span><% } %>

            <label style="margin-left:14px;">
                <input type="checkbox" name="showProofSummary" value="yes" <%= Boolean.TRUE.equals(showProofSummary) ? "checked" : "" %> >
                Show LLM proof summary
            </label>
        </div>
    </fieldset>




    <div class="row">
        <input type="submit" name="request" value="Run">
        <% if (role != null && role.equalsIgnoreCase("admin")) { %>
        <input type="submit" name="request" value="Tell">
        <% } %>
    </div>

</form>

<div id="resultsHost"></div>

<table align='left' width='80%'><tr><td bgcolor='#AAAAAA'>
    <img src='pixmaps/1pixel.gif' width=1 height=1 border=0></td></tr></table><br>

<div id="serverResults">
<%
    // ===== Server-side execution for single "Run" button =====
    if ("Run".equalsIgnoreCase(req) && !syntaxError) {
        // Always retrieve the proof answers
        Vampire.askQuestion = true;
        try {
            if ("test".equals(runSource)) {
                // ---- RUN SAVED TEST ----

                // Clear All
                try { InferenceTestSuite.resetAllForInference(kb); }
                catch (IOException ignore) { System.out.println("ERROR resetAllForInference: " + ignore.getMessage()); }

                String testName = (String) session.getAttribute("selectedTest");
                String testPath = KBmanager.getMgr().getPref("inferenceTestDir") + File.separator + testName;
                String ext = testName == null ? "" : testName.toLowerCase();

                int maxAns = Math.max(1, maxAnswers);
                int tmo    = Math.max(1, timeout);
                System.out.println("Max Answers = " + maxAns);
                System.out.println("Time-out    = " + tmo);

                // Common proof processor
                com.articulate.sigma.trans.TPTP3ProofProcessor tpp =
                        new com.articulate.sigma.trans.TPTP3ProofProcessor();

                if (ext.endsWith(".tq")) {
                    // ===== tq tests =====
                    InferenceTestSuite its = new InferenceTestSuite();
                    InferenceTestSuite.InfTestData itd = its.readTestFile(new File(testPath));
                    if (itd == null) {
                        out.println("<font color='red'>Could not read test file.</font>");
                    } else {
                        for (String s : itd.statements) if (!StringUtil.emptyString(s)) kb.tell(s);
                        FormulaPreprocessor fp = new FormulaPreprocessor();
                        Set<Formula> qs = fp.preProcess(new Formula(itd.query), true, kb);
                        for (Formula q : qs) {
                            String qstr = q.getFormula();
                            if ("EProver".equals(inferenceEngine)) {
                                com.articulate.sigma.tp.EProver eRun = kb.askEProver(qstr, tmo, maxAns);
                                tpp.parseProofOutput(eRun.output, qstr, kb, eRun.qlist);
                            } else if ("Vampire".equals(inferenceEngine)) {
                                setVampMode(vampireMode);
                                com.articulate.sigma.tp.Vampire vRun = Boolean.TRUE.equals(modensPonens)
                                        ? kb.askVampireModensPonens(qstr, tmo, maxAns)
                                        : kb.askVampire(qstr, tmo, maxAns);
                                tpp.parseProofOutput(vRun.output, qstr, kb, vRun.qlist);
                            } else if ("LEO".equals(inferenceEngine)) {
                                com.articulate.sigma.tp.LEO leoRun = kb.askLeo(qstr, tmo, maxAns);
                                tpp.parseProofOutput(leoRun.output, qstr, kb, leoRun.qlist);
                            }
                        }
                        publishGraph(tpp, inferenceEngine, vampireMode, request, application, out);
                        tpp.processAnswersFromProof(new StringBuilder(), itd.query);

                        printAnswersBlock(tpp, kbName, language, out);
                        /* Prevent duplicate answers inside HTMLformatter */
                        if (tpp.bindingMap != null) tpp.bindingMap.clear();
                        if (tpp.bindings   != null) tpp.bindings.clear();

                        out.println(HTMLformatter.formatTPTP3ProofResult(tpp, itd.query, lineHtml, kbName, language));
                        // Generate proof summary if requested
                        if (showProofSummary && tpp != null && tpp.proof != null && !tpp.proof.isEmpty()) {
                            // Extract proof steps as strings
                            List<String> proofSteps = new ArrayList<>();
                            for (Object formula : tpp.proof) {
                                String stepText = "";
                                if (formula != null) {
                                    // Get the string representation of the formula
                                    stepText = formula.toString();
                                    // Try to convert to more readable format if it's in TPTP format
                                    if (stepText.startsWith("fof(") || stepText.startsWith("cnf(")) {
                                        // Extract just the formula part, skipping the TPTP wrapper
                                        int start = stepText.indexOf(',', stepText.indexOf(',') + 1) + 1;
                                        int end = stepText.lastIndexOf(')');
                                        if (start > 0 && end > start) {
                                            stepText = stepText.substring(start, end).trim();
                                        }
                                    }
                                    // Clean up the text
                                    stepText = stepText.replaceAll("\\s+", " ").trim();
                                }
                                if (!stepText.isEmpty()) {
                                    proofSteps.add(stepText);
                                }
                            }

                            // Generate and display the summary
                            String proofSummary = LanguageFormatter.generateProofSummary(proofSteps);
                            if (!proofSummary.isEmpty()) {
                                out.println(proofSummary);
                            }
                        }
                    }
                } else if (ext.endsWith(".tptp") || ext.endsWith(".tff")) {
                    // ===== NEW .tptp / .tff FLOW via askVampireTPTP =====
                    if (!"Vampire".equals(inferenceEngine)) {
                        out.println("<span style='color:#b00'>Only Vampire is supported for .tptp/.tff tests.</span><br>");
                    } else {
                        setVampMode(vampireMode);
                        com.articulate.sigma.tp.Vampire vRun = kb.askVampireTPTP(testPath, tmo, maxAns);
                        // Provide a friendly “query label” (TPTP problems don’t have a KIF query string)
                        String pseudoQuery = "TPTP file: " + new File(testPath).getName();
                        // Parse + render just like the other flows
                        tpp.parseProofOutput(vRun.output, pseudoQuery, kb, vRun.qlist);
                        publishGraph(tpp, inferenceEngine, vampireMode, request, application, out);
                        tpp.processAnswersFromProof(vRun.qlist, pseudoQuery);

                        printAnswersBlock(tpp, kbName, language, out);
                        /* Prevent duplicate answers inside HTMLformatter */
                        if (tpp.bindingMap != null) tpp.bindingMap.clear();
                        if (tpp.bindings   != null) tpp.bindings.clear();

                        out.println(HTMLformatter.formatTPTP3ProofResult(tpp, pseudoQuery, lineHtml, kbName, language));
                        // Generate proof summary if requested
                        if (showProofSummary && tpp != null && tpp.proof != null && !tpp.proof.isEmpty()) {
                            // Extract proof steps as strings
                            List<String> proofSteps = new ArrayList<>();
                            for (Object formula : tpp.proof) {
                                String stepText = "";
                                if (formula != null) {
                                    // Get the string representation of the formula
                                    stepText = formula.toString();
                                    // Try to convert to more readable format if it's in TPTP format
                                    if (stepText.startsWith("fof(") || stepText.startsWith("cnf(")) {
                                        // Extract just the formula part, skipping the TPTP wrapper
                                        int start = stepText.indexOf(',', stepText.indexOf(',') + 1) + 1;
                                        int end = stepText.lastIndexOf(')');
                                        if (start > 0 && end > start) {
                                            stepText = stepText.substring(start, end).trim();
                                        }
                                    }
                                    // Clean up the text
                                    stepText = stepText.replaceAll("\\s+", " ").trim();
                                }
                                if (!stepText.isEmpty()) {
                                    proofSteps.add(stepText);
                                }
                            }

                            // Generate and display the summary
                            String proofSummary = LanguageFormatter.generateProofSummary(proofSteps);
                            if (!proofSummary.isEmpty()) {
                                out.println(proofSummary);
                            }
                        }
                    }
                } else if (ext.endsWith(".thf")) {
                    if (!"Vampire".equals(inferenceEngine)) {
                        out.println("<span style='color:#b00'>Only Vampire is supported for .thf tests.</span><br>");
                    } else {
                        setVampMode(vampireMode);

                        com.articulate.sigma.tp.Vampire vRun = kb.askVampireTHF(testPath, tmo, maxAns);

                        // Provide a friendly “query label” (TPTP problems don’t have a KIF query string)
                        String pseudoQuery = "TPTP file: " + new File(testPath).getName();

                        List<String> cleaned = TPTPutil.clearProofFile(vRun.output);

                        // Vampire version 4.8→5.0 reordering…
                        List<String> normalized = TPTP3ProofProcessor.reorderVampire4_8(cleaned);

                        normalized = THFutil.preprocessTHFProof(normalized);

                        tpp.parseProofOutput(normalized, pseudoQuery, kb, vRun.qlist);

                        publishGraph(tpp, inferenceEngine, vampireMode, request, application, out);
//                        tpp.processAnswersFromProof(vRun.qlist, pseudoQuery);

                        printAnswersBlock(tpp, kbName, language, out);
                        /* Prevent duplicate answers inside HTMLformatter */
                        if (tpp.bindingMap != null) tpp.bindingMap.clear();
                        if (tpp.bindings   != null) tpp.bindings.clear();

                        out.println(HTMLformatter.formatTPTP3ProofResult(tpp, pseudoQuery, lineHtml, kbName, language));
                    }
                } else {
                    out.println("<font color='red'>Unsupported test file type: " + ext + "</font>");
                }
            } else {
                // ---- RUN CUSTOM QUERY (Ask) ----
                if (stmt.indexOf('@') != -1) throw(new IOException("Row variables not allowed in query: " + stmt));
                if ("EProver".equals(inferenceEngine)) {
                    eProver = kb.askEProver(stmt, timeout, maxAnswers);
                    com.articulate.sigma.trans.TPTP3ProofProcessor tpp = new com.articulate.sigma.trans.TPTP3ProofProcessor();
                    tpp.parseProofOutput(eProver.output, stmt, kb, eProver.qlist);
                    publishGraph(tpp, inferenceEngine, vampireMode, request, application, out);

                    printAnswersBlock(tpp, kbName, language, out);
                    /* Prevent duplicate answers inside HTMLformatter */
                    if (tpp.bindingMap != null) tpp.bindingMap.clear();
                    if (tpp.bindings   != null) tpp.bindings.clear();

                    out.println(HTMLformatter.formatTPTP3ProofResult(tpp,stmt,lineHtml,kbName,language));
                    // Generate proof summary if requested
                    if (showProofSummary && tpp != null && tpp.proof != null && !tpp.proof.isEmpty()) {
                        // Extract proof steps as strings
                        List<String> proofSteps = new ArrayList<>();
                        for (Object formula : tpp.proof) {
                            String stepText = "";
                            if (formula != null) {
                                // Get the string representation of the formula
                                stepText = formula.toString();
                                // Try to convert to more readable format if it's in TPTP format
                                if (stepText.startsWith("fof(") || stepText.startsWith("cnf(")) {
                                    // Extract just the formula part, skipping the TPTP wrapper
                                    int start = stepText.indexOf(',', stepText.indexOf(',') + 1) + 1;
                                    int end = stepText.lastIndexOf(')');
                                    if (start > 0 && end > start) {
                                        stepText = stepText.substring(start, end).trim();
                                    }
                                }
                                // Clean up the text
                                stepText = stepText.replaceAll("\\s+", " ").trim();
                            }
                            if (!stepText.isEmpty()) {
                                proofSteps.add(stepText);
                            }
                        }

                        // Generate and display the summary
                        String proofSummary = LanguageFormatter.generateProofSummary(proofSteps);
                        if (!proofSummary.isEmpty()) {
                            out.println(proofSummary);
                        }
                    }
                    if (!StringUtil.emptyString(tpp.status)) out.println("Status: " + tpp.status);
                } else if ("Vampire".equals(inferenceEngine)) {
                    Formula f = new Formula();
                    f.read(stmt);
                    setVampMode(vampireMode);
//                    boolean isHOL = f.isHigherOrder(kb);
                    // Use explicit UI toggle (Translation Mode) rather than auto-detection.
                    // This makes behavior predictable for users and avoids accidental HOL attempts.
                    boolean isHOL = "HOL".equalsIgnoreCase(translationMode);
                    if (isHOL){ // Higher-Order Formula
                        System.out.println(" -- Higher Order Formula Detected - Attempring to run Vampire HOL ");
                        vampire = kb.askVampireHOL(stmt, timeout, maxAnswers, holUseModals);
                        System.out.println("============ Vampire_HOL Output Returned =============");
                        List<String> cleaned = TPTPutil.clearProofFile(vampire.output);
                        System.out.println("============ Vampire-HOL Output Cleaned =============");
                        for (String s:cleaned){
                            System.out.println(s);
                        }
                        // Vampire version 4.8→5.0 reordering…
                        List<String> normalized = TPTP3ProofProcessor.reorderVampire4_8(cleaned);
                        System.out.println("============ Vampire_HOL Output Reordered =============");
                        vampire.output = THFutil.preprocessTHFProof(normalized);
                        System.out.println("============ Vampire_HOL Output Preprocessed =============");

                    }else { // First-Order Formula
                        System.out.println(" -- First Order Formula Detected - Attempring to run normal Vampire");
                        vampire = Boolean.TRUE.equals(modensPonens)
                                ? kb.askVampireModensPonens(stmt, timeout, maxAnswers)
                                : kb.askVampire(stmt, timeout, maxAnswers);
                    }

                    if (vampire == null || vampire.output == null){
                        out.println("<font color='red'>Error. No response from Vampire.</font>");
                    } else {
                        com.articulate.sigma.trans.TPTP3ProofProcessor tpp = new com.articulate.sigma.trans.TPTP3ProofProcessor();
                        tpp.parseProofOutput(vampire.output, stmt, kb, vampire.qlist);
                        publishGraph(tpp, inferenceEngine, vampireMode, request, application, out);
                        tpp.processAnswersFromProof(vampire.qlist,stmt);

                        printAnswersBlock(tpp, kbName, language, out);
                        /* Prevent duplicate answers inside HTMLformatter */
                        if (tpp.bindingMap != null) tpp.bindingMap.clear();
                        if (tpp.bindings   != null) tpp.bindings.clear();

                        out.println(HTMLformatter.formatTPTP3ProofResult(tpp,stmt,lineHtml,kbName,language));
                        // Generate proof summary if requested
                        if (showProofSummary && tpp != null && tpp.proof != null && !tpp.proof.isEmpty()) {
                            // Extract proof steps as strings
                            List<String> proofSteps = new ArrayList<>();
                            for (Object formula : tpp.proof) {
                                String stepText = "";
                                if (formula != null) {
                                    // Get the string representation of the formula
                                    stepText = formula.toString();
                                    // Try to convert to more readable format if it's in TPTP format
                                    if (stepText.startsWith("fof(") || stepText.startsWith("cnf(")) {
                                        // Extract just the formula part, skipping the TPTP wrapper
                                        int start = stepText.indexOf(',', stepText.indexOf(',') + 1) + 1;
                                        int end = stepText.lastIndexOf(')');
                                        if (start > 0 && end > start) {
                                            stepText = stepText.substring(start, end).trim();
                                        }
                                    }
                                    // Clean up the text
                                    stepText = stepText.replaceAll("\\s+", " ").trim();
                                }
                                if (!stepText.isEmpty()) {
                                    proofSteps.add(stepText);
                                }
                            }

                            // Generate and display the summary
                            String proofSummary = LanguageFormatter.generateProofSummary(proofSteps);
                            if (!proofSummary.isEmpty()) {
                                out.println(proofSummary);
                            }
                        }
                    }


                } else if ("LEO".equals(inferenceEngine)) {
                    com.articulate.sigma.tp.LEO leo = kb.askLeo(stmt,timeout,maxAnswers);
                    if (leo == null || leo.output == null) out.println("<font color='red'>Error. No response from LEO-III.</font>");
                    else {
                        com.articulate.sigma.trans.TPTP3ProofProcessor tpp = new com.articulate.sigma.trans.TPTP3ProofProcessor();
                        tpp.parseProofOutput(leo.output, stmt, kb, leo.qlist);
                        publishGraph(tpp, inferenceEngine, vampireMode, request, application, out);
                        tpp.processAnswersFromProof(leo.qlist,stmt);

                        printAnswersBlock(tpp, kbName, language, out);
                        /* Prevent duplicate answers inside HTMLformatter */
                        if (tpp.bindingMap != null) tpp.bindingMap.clear();
                        if (tpp.bindings   != null) tpp.bindings.clear();

                        System.out.println("========== PROOF LEO ===========");
                        for(String s : leo.output){
                            System.out.println(s);
                        }


                        out.println(HTMLformatter.formatTPTP3ProofResult(tpp,stmt,lineHtml,kbName,language));
                        // Generate proof summary if requested
                        if (showProofSummary && tpp != null && tpp.proof != null && !tpp.proof.isEmpty()) {
                            // Extract proof steps as strings
                            List<String> proofSteps = new ArrayList<>();
                            for (Object formula : tpp.proof) {
                                String stepText = "";
                                if (formula != null) {
                                    // Get the string representation of the formula
                                    stepText = formula.toString();
                                    // Try to convert to more readable format if it's in TPTP format
                                    if (stepText.startsWith("fof(") || stepText.startsWith("cnf(")) {
                                        // Extract just the formula part, skipping the TPTP wrapper
                                        int start = stepText.indexOf(',', stepText.indexOf(',') + 1) + 1;
                                        int end = stepText.lastIndexOf(')');
                                        if (start > 0 && end > start) {
                                            stepText = stepText.substring(start, end).trim();
                                        }
                                    }
                                    // Clean up the text
                                    stepText = stepText.replaceAll("\\s+", " ").trim();
                                }
                                if (!stepText.isEmpty()) {
                                    proofSteps.add(stepText);
                                }
                            }

                            // Generate and display the summary
                            String proofSummary = LanguageFormatter.generateProofSummary(proofSteps);
                            if (!proofSummary.isEmpty()) {
                                out.println(proofSummary);
                            }
                        }
                    }
                }
            }
        } catch (IOException ioe) {
            out.println("<font color='red'>" + ioe.getMessage() + "</font>");
        }
    }

    if (status != null && status.toString().length() > 0) { out.println("Status: "); out.println(status.toString()); }
%>
</div>

<%!
    /** 1) One place to set Vampire mode */
    void setVampMode(String mode){
        if ("CASC".equals(mode)) com.articulate.sigma.tp.Vampire.mode = com.articulate.sigma.tp.Vampire.ModeType.CASC;
        else if ("Avatar".equals(mode)) com.articulate.sigma.tp.Vampire.mode = com.articulate.sigma.tp.Vampire.ModeType.AVATAR;
        else if ("Custom".equals(mode)) com.articulate.sigma.tp.Vampire.mode = com.articulate.sigma.tp.Vampire.ModeType.CUSTOM;
    }

    /** 2) Publish the proof graph (the big repeated block) */
    void publishGraph(com.articulate.sigma.trans.TPTP3ProofProcessor tpp,
                      String inferenceEngine, String vampireMode,
                      javax.servlet.http.HttpServletRequest request,
                      javax.servlet.ServletContext application,
                      javax.servlet.jsp.JspWriter out) throws java.io.IOException {
        String imgPath=null; try { imgPath = tpp.createProofDotGraph(); } catch (Exception ignore) {}
        if (imgPath==null || tpp.proof.size()==0) return;
        String webGraphDir = application.getRealPath("/graph"); if (webGraphDir==null) return;
        java.io.File onDisk = new java.io.File(imgPath), webDir = new java.io.File(webGraphDir);
        if (!webDir.exists()) webDir.mkdirs();
        String base = onDisk.getName(), stamped = (System.currentTimeMillis()+"-"+base).replaceAll("[^A-Za-z0-9._-]","_");
        java.io.File webImg = new java.io.File(webDir, stamped);
        try {
            java.nio.file.Files.copy(onDisk.toPath(), webImg.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            String url = request.getContextPath()+"/graph/"+webImg.getName();
            String imgUrl = url + "?v=" + System.currentTimeMillis();
            String badge = "Vampire".equals(inferenceEngine)? ("Vampire ("+vampireMode+")") : inferenceEngine;
            out.println("<div class='proof-thumb-wrap'>"
                    + "<span class='proof-badge'>" + badge + "</span>"
                    + "<a href='" + url + "' target='_blank' title='Open full-size proof graph'>"
                    + "<img class='proof-thumb' src='" + imgUrl + "' alt='Proof graph thumbnail'></a>"
                    + "<div class='proof-caption'>Click to open full-size</div></div>");
        } catch (Exception ex) {
            out.println("<span style='color:#b00'>Could not publish proof image to /graph. Path: "+imgPath+"</span><br>");
        }
    }
%>

<%!
    void printAnswersBlock(com.articulate.sigma.trans.TPTP3ProofProcessor tpp,
                           String kbName, String language,
                           javax.servlet.jsp.JspWriter out) throws java.io.IOException {

        boolean hasMap = (tpp.bindingMap != null && !tpp.bindingMap.isEmpty());
        boolean hasList = (tpp.bindings != null && !tpp.bindings.isEmpty());

        out.println("<div class='answers-card'>");
        out.println("<h3>Answers</h3>");

        if (!hasMap && !hasList) {
            out.println("<div class='answers-empty'>No explicit answer bindings were produced.</div>");
            out.println("</div>");
            return;
        }

        out.println("<ol class='answers-list'>");

        if (hasMap) {
            // Variable bindings: ?X = term
            for (java.util.Map.Entry<String,String> e : tpp.bindingMap.entrySet()) {
                String var = e.getKey();
                String raw = e.getValue();
                String term = com.articulate.sigma.trans.TPTP2SUMO.transformTerm(raw);
                String kbHref = com.articulate.sigma.HTMLformatter.createKBHref(kbName, language);
                out.println("<li><code>" + var + "</code> = "
                        + "<a href='" + kbHref + "&term=" + term + "'>" + term + "</a></li>");
            }
        } else if (hasList) {
            // Positional answers: 1. term
            for (int i = 0; i < tpp.bindings.size(); i++) {
                String raw = String.valueOf(tpp.bindings.get(i));
                String term = com.articulate.sigma.trans.TPTP2SUMO.transformTerm(raw);
                String kbHref = com.articulate.sigma.HTMLformatter.createKBHref(kbName, language);
                out.println("<li>" + (i+1) + ". "
                        + "<a href='" + kbHref + "&term=" + term + "'>" + term + "</a></li>");
            }
        }

        out.println("</ol>");

        // Handy meta line
        int count = hasMap ? tpp.bindingMap.size() : tpp.bindings.size();
        out.println("<div class='answers-meta'>" + count + " answer"
                + (count==1 ? "" : "s") + " shown.</div>");

        out.println("</div>");
    }
%>



<p>
    <%@ include file="Postlude.jsp" %>
        <iframe name="runFrame" id="runFrame" style="display:none;"></iframe>
        <script>
            (function(){
                const form = document.getElementById('AskTell');
                const overlay = document.getElementById('loading');
                const frame = document.getElementById('runFrame');

                const bar   = document.getElementById('spinBar');
                const pct   = document.getElementById('spinPct');
                const eta   = document.getElementById('spinEta');
                const limit = document.getElementById('spinLimit');

                let clicked = null;
                let rafId = null;
                let runInFlight = false;

                function clampInt(n, def) {
                    const x = parseInt(n, 10);
                    return Number.isFinite(x) && x > 0 ? x : def;
                }

                function stopProgress() {
                    if (rafId) cancelAnimationFrame(rafId);
                    rafId = null;
                }

                function startProgress(durationSec) {
                    stopProgress();
                    if (limit) limit.textContent = String(durationSec);
                    if (bar) bar.style.width = '0%';
                    if (pct) pct.textContent = '0%';
                    if (eta) eta.textContent = `~${durationSec}s remaining`;

                    const durationMs = durationSec * 1000;
                    const start = performance.now();

                    function tick(now) {
                        const elapsed = now - start;
                        const p = Math.min(1, elapsed / durationMs);

                        if (bar) bar.style.width = (p * 100).toFixed(1) + '%';
                        if (pct) pct.textContent = Math.floor(p * 100) + '%';

                        const remaining = Math.max(0, Math.ceil((durationMs - elapsed) / 1000));
                        if (eta) eta.textContent = remaining > 0 ? `~${remaining}s remaining` : 'Timeout reached';

                        if (p < 1 && runInFlight) rafId = requestAnimationFrame(tick);
                    }

                    rafId = requestAnimationFrame(tick);
                }

                // Track which submit button was pressed
                form.querySelectorAll('input[type=submit]').forEach(b =>
                    b.addEventListener('click', e => clicked = e.target.value)
                );

                // Run submits into iframe so the page stays alive
                form.addEventListener('submit', function(){
                    if (clicked !== 'Run') {
                        form.removeAttribute('target'); // normal for Tell
                        return;
                    }

                    form.setAttribute('target', 'runFrame');

                    const timeoutField = form.querySelector('input[name="timeout"]');
                    const tSec = clampInt(timeoutField ? timeoutField.value : null, 30);

                    runInFlight = true;
                    overlay.style.display = 'block';
                    startProgress(tSec);
                });

                // IMPORTANT: hide overlay when iframe finishes loading server response
                frame.addEventListener('load', function(){
                    if (!runInFlight) return;
                    runInFlight = false;

                    stopProgress();
                    overlay.style.display = 'none';

                    // Pull results from iframe and inject into main page
                    try {
                        const doc = frame.contentDocument || frame.contentWindow.document;
                        const results = doc.getElementById('serverResults');
                        const host = document.getElementById('resultsHost');

                        if (!host) return;

                        if (results) {
                            host.innerHTML = results.innerHTML;
                        } else {
                            // Fallback: show something instead of "nothing"
                            host.innerHTML = "<div style='color:#b00'>No #serverResults found in response.</div>";
                        }
                    } catch (e) {
                        document.getElementById('resultsHost').innerHTML =
                            "<div style='color:#b00'>Could not read iframe response (same-origin issue).</div>";
                    }
                });

                // Safety: if user navigates back/forward
                window.addEventListener('pageshow', e => {
                    if (e.persisted) {
                        runInFlight = false;
                        stopProgress();
                        overlay.style.display = 'none';
                    }
                });
            })();
        </script>

        <script>
        document.addEventListener('DOMContentLoaded', function(){
            const sel = document.getElementById('testName');
            const radios = document.querySelectorAll('input[name="testFilter"]');
            const hidden = document.getElementById('testFilterHidden');
            const form = document.getElementById('AskTell');
            if (!sel || !radios.length) return;

            const original = Array.from(sel.options).map(o => ({text:o.text, value:o.value}));

            function applyFilter(kind){
                const prev = sel.value;
                const match = f => kind === 'all' || f.toLowerCase().endsWith('.' + kind);
                sel.innerHTML = '';
                original.filter(o => match(o.value)).forEach(o => {
                    const opt = document.createElement('option'); opt.value=o.value; opt.text=o.text; sel.add(opt);
                });
                sel.value = Array.from(sel.options).some(o => o.value===prev) ? prev : (sel.options[0]?.value || '');
            }
            function currentFilter(){ return document.querySelector('input[name="testFilter"]:checked')?.value || 'all'; }

            // initialize from session-backed radio
            applyFilter(currentFilter());

            // keep session in sync on change + submit
            radios.forEach(r => r.addEventListener('change', e => { hidden.value = e.target.value; applyFilter(e.target.value); }));
            form.addEventListener('submit', () => { hidden.value = currentFilter(); });
        });
    </script>
</body>
</html>