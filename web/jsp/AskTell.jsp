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
        .spin-overlay { position:fixed; inset:0; background:rgba(255,255,255,.95); display:none; z-index:9999; }
        body.busy .spin-overlay { display:block; }   /* spinner visible on the new page until load */
        .bounce-icon { position:absolute; top:50%; left:50%; width:120px; height:120px;
            transform:translate(-50%,-50%); animation:bounce 1.2s ease-in-out infinite; }
        @keyframes bounce { 0%,100%{transform:translate(-50%,-50%) scale(1)} 50%{transform:translate(-50%,-60%) scale(1.1)} }
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
            const vamp = document.querySelector('input[name="inferenceEngine"][value="Vampire"]');
            const casc = document.getElementById('CASC');
            const avatar = document.getElementById('Avatar');
            const custom = document.getElementById('Custom');
            const mp = document.getElementById('ModensPonens');
            const drop = document.getElementById('dropOnePremise');
            const vampireOn = vamp && vamp.checked && !vamp.disabled;
            [casc, avatar, custom, mp].forEach(el => { if (el) el.disabled = !vampireOn; });
            const mpOn = vampireOn && mp && mp.checked;
            if (drop) { drop.disabled = !mpOn; if (!mpOn) drop.checked = false; }
        }
        function toggleRunSource() {
            const src = document.querySelector('input[name="runSource"]:checked')?.value || 'custom';
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
        window.onload = function(){ toggleVampireOptions(); toggleRunSource();
            document.querySelectorAll('input[name="inferenceEngine"], #ModensPonens')
                .forEach(el => el.addEventListener('change', toggleVampireOptions));
            document.querySelectorAll('input[name="runSource"]')
                .forEach(el => el.addEventListener('change', toggleRunSource));
        };
    </script>

    <script>
        function viewSelectedTest() {
            const sel = document.getElementById('testName');
            if (!sel || !sel.value) return;
            const name = sel.value.toLowerCase();
            // .tq via ViewTest.jsp, others directly from /tests/
            const url = name.endsWith('.tq') || name.endsWith('.tptp') || name.endsWith('.tff')
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

    // ---- Remember selected test in session ----
    String selectedTest = (String) session.getAttribute("selectedTest");
    if (req != null && request.getParameter("testName") != null) {
        selectedTest = request.getParameter("testName");
        session.setAttribute("selectedTest", selectedTest);
    }

    // ---- CWA ----
    if (StringUtil.emptyString(cwa)) cwa = "no";
    SUMOKBtoTPTPKB.CWA = "yes".equals(cwa);

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
    String tptpFile = KBmanager.getMgr().getPref("kbDir") + File.separator + "SUMO.tptp";
    File epFile = new File(eproverExec);
    if (kb.eprover == null && epFile.exists())
        kb.eprover = new com.articulate.sigma.tp.EProver(eproverExec,tptpFile);
    if (inferenceEngine == null) inferenceEngine = (kb.eprover != null) ? "EProver" : "Vampire";

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

<div id="loading" class="spin-overlay">
    <img src="pixmaps/sumo.gif" class="bounce-icon" alt="Loading...">
</div>


<form name="AskTell" id="AskTell" action="AskTell.jsp" method="POST">
    <%
        String pageName = "AskTell";
        String pageString = "Inference Interface";
    %>
    <%@include file="CommonHeader.jsp" %>
    <table ALIGN="LEFT" WIDTH=80%><tr><TD BGCOLOR='#AAAAAA'>
        <IMG SRC='pixmaps/1pixel.gif' width=1 height=1 border=0></TD></tr></table><BR>

    <!-- ===== INPUT ===== -->
    <fieldset>
        <legend>Input</legend>
        <div class="row">
            <label><input type="radio" name="runSource" value="custom"
                <%= "test".equals(session.getAttribute("runSource")) ? "" : "checked" %> > Custom query</label>
            &nbsp;&nbsp;
            <!-- Radio label -->
            <label><input type="radio" name="runSource" value="test"
                <%= "test".equals(session.getAttribute("runSource")) ? "checked" : "" %> >
                Saved test (.tq / .tptp / .tff)
            </label>
        </div>

        <div class="row" id="lblCustom">
            <textarea rows="5" cols="70" name="stmt" id="stmtArea"><%=stmt%></textarea>
        </div>

        <%
            String testDir = KBmanager.getMgr().getPref("inferenceTestDir");
            File[] allFiles = (testDir == null) ? new File[0]
                    : new File(testDir).listFiles((d,n) -> n.endsWith(".tq") || n.endsWith(".tptp") || n.endsWith(".tff"));
            if (allFiles == null) allFiles = new File[0];

            File[] testFiles = allFiles;
            Arrays.sort(testFiles, Comparator.comparing(File::getName));
            if (selectedTest == null && testFiles.length > 0) selectedTest = testFiles[0].getName();
        %>

            <!-- ===== Open test in a new page ===== -->
        <div class="row" id="lblTest">
            <b>Test:</b>
            <select name="testName" id="testName">
                <% for (File f : testFiles) {
                    String fname = f.getName();
                    boolean sel = fname.equals(selectedTest);
                %>
                <option value="<%= fname %>" <%= sel ? "selected" : "" %>><%= fname %></option>
                <% } %>
            </select>

            <!-- JS-driven, always fresh -->
            <a href="javascript:void(0)" onclick="viewSelectedTest()"
               style="margin-left:10px; text-decoration:underline; color:#0073e6;">
                View Test
            </a>

            <div class="row" id="filterRow">
                Filter:
                <label><input type="radio" name="testFilter" value="all"  <%= "all".equalsIgnoreCase(testFilter)  ? "checked":"" %>> All</label>
                <label><input type="radio" name="testFilter" value="tq"   <%= "tq".equalsIgnoreCase(testFilter)   ? "checked":"" %>> tq</label>
                <label><input type="radio" name="testFilter" value="tptp" <%= "tptp".equalsIgnoreCase(testFilter) ? "checked":"" %>> tptp</label>
                <label><input type="radio" name="testFilter" value="tff"  <%= "tff".equalsIgnoreCase(testFilter)  ? "checked":"" %>> tff</label>
            </div>
            <input type="hidden" name="testFilter" id="testFilterHidden" value="<%= testFilter %>">

            <span class="muted">(Uses the configuration below)</span>
        </div>
    </fieldset>

    <!-- ===== CONFIG ===== -->
    <fieldset>
        <legend>Configuration (applies to both)</legend>

        Maximum answers: <input type="text" name="maxAnswers" value="<%=maxAnswers%>">
        &nbsp; Query time limit: <input type="text" name="timeout" value="<%=timeout%>"><br>

        [ <input type="radio" id="langFof" name="TPTPlang" value="fof" <%= "fof".equals(TPTPlang)?"checked":"" %> >
        <label for="langFof">tptp mode</label>

        <input type="radio" id="langTff" name="TPTPlang" value="tff" <%= "tff".equals(TPTPlang)?"checked":"" %> >
        <label for="langTff">tff mode</label>]

        &nbsp;&nbsp;<input type="checkbox" name="CWA" id="CWA" value="yes" <% if ("yes".equals(cwa)) {%>checked<%}%> >
        <label>Closed World Assumption</label><br>

        Choose an inference engine:<br>

        <input type="radio" name="inferenceEngine" value="LEO" <% if ("LEO".equals(inferenceEngine)) {%>checked<%}%>
               onclick="toggleVampireOptions()"> LEO-III <br>

        <input type="radio" name="inferenceEngine" value="EProver" <% if ("EProver".equals(inferenceEngine)) {%>checked<%}%>
               onclick="toggleVampireOptions()" <% if (kb.eprover == null) { %> disabled <% } %> > EProver <br>

        <input type="radio" name="inferenceEngine" value="Vampire" <% if ("Vampire".equals(inferenceEngine)) {%>checked<%}%>
               onclick="toggleVampireOptions()" <% if (KBmanager.getMgr().getPref("vampire") == null) { %> disabled <% } %> >
        Vampire :
        [ <input type="radio" id="CASC" name="vampireMode" value="CASC"
        <% if ("CASC".equals(vampireMode)) { out.print(" CHECKED"); } %> > <label>CASC mode</label>
        <input type="radio" id="Avatar" name="vampireMode" value="Avatar"
            <% if ("Avatar".equals(vampireMode)) { out.print(" CHECKED"); } %> > <label>Avatar mode</label>
        <input type="radio" id="Custom" name="vampireMode" value="Custom"
            <% if ("Custom".equals(vampireMode)) { out.print(" CHECKED"); } %> > <label>Custom mode</label> ]

        <input type="checkbox" id="ModensPonens" name="ModensPonens" value="yes" <% if (modensPonens) { out.print(" CHECKED"); } %> >
        <label for="ModensPonens">Modens Ponens</label>
        <span title="Runs Vampire with modus-ponens-only routine over authored axioms">&#9432;</span>
        [ <input type="checkbox" name="dropOnePremise" id="dropOnePremise" value="true"
        <% if (Boolean.TRUE.equals(dropOnePremise)) { out.print(" CHECKED"); } %> >
        <label for="dropOnePremise">Drop One-Premise Formulas</label> ]
        <br>

        <input type="checkbox" name="showProofInEnglish" value="yes"
               <% if (Boolean.TRUE.equals(showEnglish)) { %>checked<% } %> >
        <label>Show English Paraphrases</label><br>

        <input type="checkbox" name="showProofFromLLM" value="yes"
            <%= (Boolean.TRUE.equals(llmProof) && ollamaUp) ? "checked" : "" %>
            <%= ollamaUp ? "" : "disabled" %> >
        <label>Use LLM for Paraphrasing</label>
        <% if (!ollamaUp) { %><span title="Ollama is not running.">&#9432;</span><% } %>

        <input type="checkbox" name="showProofSummary" value="yes"
            <%= Boolean.TRUE.equals(showProofSummary) ? "checked" : "" %> >
        <label>Show LLM Proof Summary</label><br>
    </fieldset>
        


    <div class="row">
        <input type="submit" name="request" value="Run">
        <% if (role != null && role.equalsIgnoreCase("admin")) { %>
        <input type="submit" name="request" value="Tell">
        <% } %>
    </div>

</form>

<table align='left' width='80%'><tr><td bgcolor='#AAAAAA'>
    <img src='pixmaps/1pixel.gif' width=1 height=1 border=0></td></tr></table><br>

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
                    setVampMode(vampireMode);
                    vampire = Boolean.TRUE.equals(modensPonens)
                            ? kb.askVampireModensPonens(stmt, timeout, maxAnswers)
                            : kb.askVampire(stmt, timeout, maxAnswers);
                    if (vampire == null || vampire.output == null) out.println("<font color='red'>Error. No response from Vampire.</font>");
                    else {
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
                    kb.leo = kb.askLeo(stmt,timeout,maxAnswers);
                    if (kb.leo == null || kb.leo.output == null) out.println("<font color='red'>Error. No response from LEO-III.</font>");
                    else {
                        com.articulate.sigma.trans.TPTP3ProofProcessor tpp = new com.articulate.sigma.trans.TPTP3ProofProcessor();
                        tpp.parseProofOutput(kb.leo.output, stmt, kb, kb.leo.qlist);
                        publishGraph(tpp, inferenceEngine, vampireMode, request, application, out);
                        tpp.processAnswersFromProof(kb.leo.qlist,stmt);

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
                }
            }
        } catch (IOException ioe) {
            out.println("<font color='red'>" + ioe.getMessage() + "</font>");
        }
    }

    if (status != null && status.toString().length() > 0) { out.println("Status: "); out.println(status.toString()); }
%>

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

        <script>
            (function(){
                const form = document.getElementById('AskTell');
                let clicked = null;
                form.querySelectorAll('input[type=submit]').forEach(b =>
                    b.addEventListener('click', e => clicked = e.target.value)
                );
                form.addEventListener('submit', function(){
                    if (clicked === 'Run') {
                        // show spinner on the OLD page immediately
                        document.getElementById('loading').style.display = 'block';
                    }
                });
                // hide spinner when the NEW page is fully loaded
                window.addEventListener('load', () => document.body.classList.remove('busy'));
                // BFCache: ensure spinner is hidden when navigating back
                window.addEventListener('pageshow', e => { if (e.persisted) document.body.classList.remove('busy'); });
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