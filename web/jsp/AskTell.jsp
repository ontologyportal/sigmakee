<%@ page import="com.articulate.sigma.nlg.LanguageFormatter" %>
<%@ page import="com.articulate.sigma.utils.StringUtil" %>
<%@ page import="com.articulate.sigma.InferenceTestSuite" %>
<%@ page import="com.articulate.sigma.trans.TPTPGenerationManager" %>
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
    <link rel="stylesheet" href="AskTell.css">
    <script src="AskTell.js" defer></script>
</head>
<%
    StringBuilder status = new StringBuilder();
    String req = request.getParameter("request");
    String stmt = request.getParameter("stmt");
    String cwa = request.getParameter("CWA");
    String isModal = request.getParameter("isModal");
    String runSource = request.getParameter("runSource");

    // ---- Run source + persist in session ----
    if (runSource == null) runSource = (String) session.getAttribute("runSource");
    if (runSource == null) runSource = "custom";
    session.setAttribute("runSource", runSource);

    Boolean modensPonens = (Boolean) session.getAttribute("ModensPonens");
    Boolean dropOnePremise = (Boolean) session.getAttribute("dropOnePremise");
    // ---- Statement + basics ----

    // ---- Modens Ponens (persist) ----
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
    // ---- Engine / modes / language ----
    String inferenceEngine = request.getParameter("inferenceEngine");
    String vampireMode = request.getParameter("vampireMode");
    if (StringUtil.emptyString(vampireMode)) vampireMode = "CASC";
    String TPTPlang = request.getParameter("TPTPlang");
    if (StringUtil.emptyString(TPTPlang) || TPTPlang.equals("fof")) {
        TPTPlang = "fof";
        SUMOformulaToTPTPformula.setLang("fof");
        SUMOKBtoTPTPKB.setLang("fof");
    }
    if ("tff".equals(TPTPlang)) {
        SUMOformulaToTPTPformula.setLang("tff");
        SUMOKBtoTPTPKB.setLang("tff");
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
    String graphFormulaFormat = request.getParameter("graphFormulaFormat"); // "SUO_KIF" or "TPTP"
    if (StringUtil.emptyString(graphFormulaFormat) || graphFormulaFormat.equals("SUO_KIF")) {
        graphFormulaFormat = "SUO_KIF";
    }else{
        graphFormulaFormat = "TPTP";
    }
    session.setAttribute("graphFormulaFormat", graphFormulaFormat);
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
    String lineHtml = "<table ALIGN='LEFT' WIDTH='40%'><tr><TD BGCOLOR='#AAAAAA'><IMG SRC='pixmaps/1pixel.gif' width=1 height=1 border=0></TD></tr></table><BR>\n";
    // ---- Global flags for paraphrasing ----
    HTMLformatter.proofParaphraseInEnglish = showEnglish;
    LanguageFormatter.paraphraseLLM = llmProof;
    boolean busy = "Run".equalsIgnoreCase(req);
%>
<body class="<%= busy ? "busy" : "" %>" aria-busy="<%= busy %>">
<div id="loading" class="spin-overlay" aria-live="polite" aria-atomic="true">
    <div class="spin-card">
        <div class="spin-row">
            <img src="pixmaps/sumo.gif" class="bounce-icon" alt="Loading...">
            <div>
                <div id="spinTitle" class="spin-title">Processing...</div>
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
    <table ALIGN="LEFT" WIDTH=80%>
        <tr>
            <TD BGCOLOR='#AAAAAA'>
                <IMG SRC='pixmaps/1pixel.gif' width=1 height=1 border=0>
            </TD>
        </tr>
    </table><BR>
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
    <fieldset class="step" id="step2Fieldset">
        <legend>Step 2 - Translation mode</legend>
        <div class="row inline" >
            <label><input type="radio" id="modeFOL" name="translationMode" value="FOL"
                <%= "HOL".equalsIgnoreCase((String)session.getAttribute("translationMode")) ? "" : "checked" %> >
                FOL (TPTP / TFF)
            </label>
            <label><input type="radio" id="modeHOL" name="translationMode" value="HOL"
                <%= "HOL".equalsIgnoreCase((String)session.getAttribute("translationMode")) ? "checked" : "" %> >
                HOL (THF)
            </label>
        </div>
        <div id="folOptions" class="row">
            <div class="inline">
                <span class="muted">Language:</span>
                <label><input type="radio" id="langFof" name="TPTPlang" value="fof" <%= "fof".equals(TPTPlang)?"checked":"" %> >
                    tptp (fof)
                </label>
                <label><input type="radio" id="langTff" name="TPTPlang" value="tff" <%= "tff".equals(TPTPlang)?"checked":"" %> >
                    tff
                </label>
            </div>
            <details class="advanced">
                <summary>Advanced Options</summary>
                <div class="row">
                    <label class="inline" style="margin-left:14px;">
                        <input type="checkbox" name="CWA" id="CWA" value="yes" <% if ("yes".equals(cwa)) {%>checked<%}%> >
                        Closed World Assumption
                    </label>
                    <span title="Runs only in TFF mode">&#9432;</span>
                </div>
            </details>
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
        <!-- Graph label format toggle -->
        <div class="row graph-format-toggle">
            <span class="opt-label">Graph formulas:</span>
            <div class="segmented" role="group" aria-label="Graph formula format">
                <input type="radio" id="gfmt-kif" name="graphFormulaFormat" value="SUO_KIF"
                    <%= (graphFormulaFormat == null || "SUO_KIF".equals(graphFormulaFormat)) ? "checked" : "" %> >
                <label for="gfmt-kif" title="Render graph node labels using SUO-KIF">SUO-KIF</label>
                <input type="radio" id="gfmt-tptp" name="graphFormulaFormat" value="TPTP"
                    <%= "TPTP".equals(graphFormulaFormat) ? "checked" : "" %> >
                <label for="gfmt-tptp" title="Render graph node labels using TPTP">TPTP</label>
            </div>
            <span class="hint" title="Controls only the graph labels; it does not change the prover output.">&#9432;</span>
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
        // Check if required TPTP format is ready (background generation may still be in progress)
        boolean isHOL = "HOL".equalsIgnoreCase(translationMode);
        boolean needsFOF = !isHOL && "fof".equals(TPTPlang);
        boolean needsTFF = !isHOL && "tff".equals(TPTPlang);
        boolean needsTHF = isHOL;
        boolean generationInProgress = false;
        String waitingFor = "";
        if (needsFOF && !TPTPGenerationManager.isFOFReady()) {
            generationInProgress = true;
            waitingFor = "FOF (SUMO.tptp)";
        } else if (needsTFF && !TPTPGenerationManager.isTFFReady()) {
            generationInProgress = true;
            waitingFor = "TFF (SUMO.tff)";
        } else if (needsTHF) {
            boolean useModals = Boolean.TRUE.equals(holUseModals);
            if (useModals && !TPTPGenerationManager.isTHFModalReady()) {
                generationInProgress = true;
                waitingFor = "THF Modal (SUMO_modals.thf)";
            } else if (!useModals && !TPTPGenerationManager.isTHFPlainReady()) {
                generationInProgress = true;
                waitingFor = "THF Plain (SUMO_plain.thf)";
            }
        }
        if (generationInProgress) {
%>
<div style="border:1px solid #ff9900; background:#fff8f0; padding:16px; margin:10px 0; border-radius:6px;">
    <strong style="color:#b35900;">KB Translation In Progress</strong><br><br>
    The <code><%= waitingFor %></code> translation file is still being generated in the background.<br>
    Please wait a moment and try your query again.<br><br>
    <em style="color:#666;">This happens once after startup while the knowledge base is being prepared for inference.</em>
</div>
<%
        } else {
        // Always retrieve the proof answers
        Vampire.askQuestion = true;
        try {
            if ("test".equals(runSource)) {
                // ---- RUN SAVED TEST ----
                // Clear All
                try { InferenceTestSuite.resetAllForInference(kb, session.getId()); }
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
                        try {
                            final String sid = session.getId();
                            SessionTPTPManager.beginBatchTells(sid);
                            try {
                                for (String s : itd.statements) {
                                    if (!StringUtil.emptyString(s)) kb.tell(s, sid);
                                }
                            }
                            finally {
                                SessionTPTPManager.endBatchTells(sid);
                            }
                            FormulaPreprocessor fp = new FormulaPreprocessor();
                            Set<Formula> qs =
                                    SessionTPTPManager.withSessionCache(
                                            sid, kb, () -> fp.preProcess(new Formula(itd.query), true, kb));
                            for (Formula q : qs) {
                                String qstr = q.getFormula();
                                if ("EProver".equals(inferenceEngine)) {
                                    com.articulate.sigma.tp.EProver eRun = kb.askEProver(qstr, tmo, maxAns);
                                    if (eRun != null && eRun.getResult() != null) {
                                        renderATPResultPanel(eRun.getResult(), out);
                                    }
                                    tpp.parseProofOutput(eRun.output, qstr, kb, eRun.qlist);
                                } else if ("Vampire".equals(inferenceEngine)) {
                                    setVampMode(vampireMode);
                                    com.articulate.sigma.tp.Vampire vRun = kb.askVampireForTQ(qstr, tmo, maxAns, modensPonens, session.getId());
//                                    com.articulate.sigma.tp.Vampire vRun = Boolean.TRUE.equals(modensPonens)
//                                            ? kb.askVampireModensPonens(qstr, tmo, maxAns)
//                                            : kb.askVampire(qstr, tmo, maxAns);
                                    if (vRun != null && vRun.getResult() != null) {
                                        renderATPResultPanel(vRun.getResult(), out);
                                    }
                                    tpp.parseProofOutput(vRun.output, qstr, kb, vRun.qlist);
                                }
                                else if ("LEO".equals(inferenceEngine)) {
                                   LEO leoRun = kb.askLeo(qstr, tmo, maxAns, session.getId());
                                    if (leoRun != null && leoRun.getResult() != null) {
                                        renderATPResultPanel(leoRun.getResult(), out);
                                    }
                                    tpp.parseProofOutput(leoRun.output, qstr, kb, leoRun.qlist);
                                }
                            }
                            setGraphFormat(graphFormulaFormat,tpp);
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
                        } catch (com.articulate.sigma.tp.ExecutableNotFoundException enfe) {
                            renderExceptionPanel(enfe, out);
                        } catch (ProverTimeoutException | ProverCrashedException pte) {
                            renderExceptionPanel(pte, out);
                            if (pte.getResult() != null) {
                                renderATPResultPanel(pte.getResult(), out);
                            }
                        } catch (com.articulate.sigma.tp.ATPException ae) {
                            renderExceptionPanel(ae, out);
                        }
                    }
                } else if (ext.endsWith(".tptp") || ext.endsWith(".tff")) {
                    // ===== NEW .tptp / .tff FLOW via askVampireTPTP =====
                    if (!"Vampire".equals(inferenceEngine)) {
                        out.println("<span style='color:#b00'>Only Vampire is supported for .tptp/.tff tests.</span><br>");
                    } else {
                        try {
                            setVampMode(vampireMode);
                            com.articulate.sigma.tp.Vampire vRun = kb.askVampireTPTP(testPath, tmo, maxAns);
                            // Show ATPResult panel with SZS status and diagnostics
                            if (vRun != null && vRun.getResult() != null) {
                                renderATPResultPanel(vRun.getResult(), out);
                            }
                            // Provide a friendly "query label" (TPTP problems don't have a KIF query string)
                            String pseudoQuery = "TPTP file: " + new File(testPath).getName();
                            // Parse + render just like the other flows
                            tpp.parseProofOutput(vRun.output, pseudoQuery, kb, vRun.qlist);
                            setGraphFormat(graphFormulaFormat,tpp);
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
                        } catch (com.articulate.sigma.tp.ExecutableNotFoundException enfe) {
                            renderExceptionPanel(enfe, out);
                        } catch (ProverTimeoutException | ProverCrashedException pte) {
                            renderExceptionPanel(pte, out);
                            if (pte.getResult() != null) {
                                renderATPResultPanel(pte.getResult(), out);
                            }
                        } catch (com.articulate.sigma.tp.ATPException ae) {
                            renderExceptionPanel(ae, out);
                        }
                    }
                } else if (ext.endsWith(".thf")) {
                    if (!"Vampire".equals(inferenceEngine)) {
                        out.println("<span style='color:#b00'>Only Vampire is supported for .thf tests.</span><br>");
                    } else {
                        try {
                            setVampMode(vampireMode);
                            com.articulate.sigma.tp.Vampire vRun = kb.askVampireTHF(testPath, tmo, maxAns);
                            // Show ATPResult panel with SZS status and diagnostics
                            if (vRun != null && vRun.getResult() != null) {
                                renderATPResultPanel(vRun.getResult(), out);
                            }
                            // Provide a friendly "query label" (TPTP problems don't have a KIF query string)
                            String pseudoQuery = "TPTP file: " + new File(testPath).getName();
                            List<String> cleaned = TPTPutil.clearProofFile(vRun.output);
                            // Vampire version 4.8→5.0 reordering…
                            List<String> normalized = TPTP3ProofProcessor.reorderVampire4_8(cleaned);
                            normalized = THFutil.preprocessTHFProof(normalized);
                            tpp.parseProofOutput(normalized, pseudoQuery, kb, vRun.qlist);
                            setGraphFormat(graphFormulaFormat,tpp);
                            publishGraph(tpp, inferenceEngine, vampireMode, request, application, out);
//                            tpp.processAnswersFromProof(vRun.qlist, pseudoQuery);
                            printAnswersBlock(tpp, kbName, language, out);
                            /* Prevent duplicate answers inside HTMLformatter */
                            if (tpp.bindingMap != null) tpp.bindingMap.clear();
                            if (tpp.bindings   != null) tpp.bindings.clear();
                            out.println(HTMLformatter.formatTPTP3ProofResult(tpp, pseudoQuery, lineHtml, kbName, language));
                        } catch (com.articulate.sigma.tp.ExecutableNotFoundException enfe) {
                            renderExceptionPanel(enfe, out);
                        } catch (ProverTimeoutException | ProverCrashedException pte) {
                            renderExceptionPanel(pte, out);
                            if (pte.getResult() != null) {
                                renderATPResultPanel(pte.getResult(), out);
                            }
                        } catch (com.articulate.sigma.tp.ATPException ae) {
                            renderExceptionPanel(ae, out);
                        }
                    }
                } else {
                    out.println("<font color='red'>Unsupported test file type: " + ext + "</font>");
                }
            } else {
                // ---- RUN CUSTOM QUERY (Ask) ----
                // Reset spinner message to default (clear any stale "Regenerating KB..." from previous Tell)
                out.println("<script>");
                out.println("if(parent.document.getElementById('spinTitle'))");
                out.println("  parent.document.getElementById('spinTitle').textContent='Processing query...';");
                out.println("if(parent.document.getElementById('spinSub'))");
                out.println("  parent.document.getElementById('spinSub').textContent='';");
                out.println("</script>");
                out.flush();
                if (stmt.indexOf('@') != -1) throw(new IOException("Row variables not allowed in query: " + stmt));
                if ("EProver".equals(inferenceEngine)) {
                    try {
                        eProver = kb.askEProver(stmt, timeout, maxAnswers);
                        // Show ATPResult panel with SZS status and diagnostics
                        if (eProver != null && eProver.getResult() != null) {
                            renderATPResultPanel(eProver.getResult(), out);
                        }
                        com.articulate.sigma.trans.TPTP3ProofProcessor tpp = new com.articulate.sigma.trans.TPTP3ProofProcessor();
                        tpp.parseProofOutput(eProver.output, stmt, kb, eProver.qlist);
                        setGraphFormat(graphFormulaFormat,tpp);
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
                    } catch (com.articulate.sigma.tp.ExecutableNotFoundException enfe) {
                        renderExceptionPanel(enfe, out);
                    } catch (ProverTimeoutException | ProverCrashedException pte) {
                        renderExceptionPanel(pte, out);
                        if (eProver != null && eProver.getResult() != null) {
                            renderATPResultPanel(eProver.getResult(), out);
                        }
                    } catch (com.articulate.sigma.tp.ATPException ae) {
                        renderExceptionPanel(ae, out);
                    }
                } else if ("Vampire".equals(inferenceEngine)) {
                    Formula f = new Formula();
                    f.read(stmt);
                    setVampMode(vampireMode);
//                    boolean isHOL = f.isHigherOrder(kb);
                    // Use explicit UI toggle (Translation Mode) rather than auto-detection.
                    // This makes behavior predictable for users and avoids accidental HOL attempts.
//                    isHOL = "HOL".equalsIgnoreCase(translationMode);
                    try {
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

                        } else { // First-Order Formula
                            System.out.println(" -- First Order Formula Detected - Attempring to run normal Vampire");
                            String sessId = session.getId();
                            vampire = Boolean.TRUE.equals(modensPonens)
                                    ? kb.askVampireModensPonens(stmt, timeout, maxAnswers, sessId)
                                    : kb.askVampire(stmt, timeout, maxAnswers, sessId);
                        }
                        // Show ATPResult panel with SZS status and diagnostics
                        if (vampire != null && vampire.getResult() != null) {
                            renderATPResultPanel(vampire.getResult(), out);
                        }
                        if (vampire == null || vampire.output == null){
                            out.println("<font color='red'>Error. No response from Vampire.</font>");
                        } else {
                            com.articulate.sigma.trans.TPTP3ProofProcessor tpp = new com.articulate.sigma.trans.TPTP3ProofProcessor();
                            tpp.parseProofOutput(vampire.output, stmt, kb, vampire.qlist);
                            setGraphFormat(graphFormulaFormat,tpp);
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
                    } catch (com.articulate.sigma.tp.ExecutableNotFoundException enfe) {
                        renderExceptionPanel(enfe, out);
                    } catch (ProverTimeoutException | ProverCrashedException pte) {
                        renderExceptionPanel(pte, out);
                        // Still show any partial output
                        if (pte.getResult() != null) {
                            renderATPResultPanel(pte.getResult(), out);
                        }
                    } catch (com.articulate.sigma.tp.ATPException ae) {
                        renderExceptionPanel(ae, out);
                    }
                } else if ("LEO".equals(inferenceEngine)) {
                    LEO leo = null;
                    try {
                        leo = kb.askLeo(stmt,timeout,maxAnswers,session.getId());
                        // Show ATPResult panel with SZS status and diagnostics
                        if (leo != null && leo.getResult() != null) {
                            renderATPResultPanel(leo.getResult(), out);
                        }
                        if (leo == null || leo.output == null) {
                            out.println("<font color='red'>Error. No response from LEO-III.</font>");
                        } else {
                            com.articulate.sigma.trans.TPTP3ProofProcessor tpp = new com.articulate.sigma.trans.TPTP3ProofProcessor();
                            tpp.parseProofOutput(leo.output, stmt, kb, leo.qlist);
                            setGraphFormat(graphFormulaFormat,tpp);
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
                    } catch (com.articulate.sigma.tp.ExecutableNotFoundException enfe) {
                        renderExceptionPanel(enfe, out);
                    } catch (ProverTimeoutException | ProverCrashedException pte) {
                        renderExceptionPanel(pte, out);
                        if (leo != null && leo.getResult() != null) {
                            renderATPResultPanel(leo.getResult(), out);
                        }
                    } catch (com.articulate.sigma.tp.ATPException ae) {
                        renderExceptionPanel(ae, out);
                    }
                }
            }
        } catch (IOException ioe) {
            out.println("<font color='red'>" + ioe.getMessage() + "</font>");
        }
        } // end else (generation not in progress)
    }

    // ===== Server-side execution for "Tell" button =====
    if ("Tell".equalsIgnoreCase(req) && !syntaxError) {
        // Check if required TPTP format is ready (background generation may still be in progress)
        boolean tellNeedsFOF = "fof".equals(TPTPlang);
        boolean tellNeedsTFF = "tff".equals(TPTPlang);
        boolean tellGenerationInProgress = false;
        String tellWaitingFor = "";
        if (tellNeedsFOF && !TPTPGenerationManager.isFOFReady()) {
            tellGenerationInProgress = true;
            tellWaitingFor = "FOF (SUMO.tptp)";
        } else if (tellNeedsTFF && !TPTPGenerationManager.isTFFReady()) {
            tellGenerationInProgress = true;
            tellWaitingFor = "TFF (SUMO.tff)";
        }
        if (tellGenerationInProgress) {
%>
<div style="border:1px solid #ff9900; background:#fff8f0; padding:16px; margin:10px 0; border-radius:6px;">
    <strong style="color:#b35900;">KB Translation In Progress</strong><br><br>
    The <code><%= tellWaitingFor %></code> translation file is still being generated in the background.<br>
    Please wait a moment and try your assertion again.<br><br>
    <em style="color:#666;">This happens once after startup while the knowledge base is being prepared for inference.</em>
</div>
<%
        } else {
        try {
            final java.util.concurrent.atomic.AtomicBoolean mustRegenBaseRef =
                    new java.util.concurrent.atomic.AtomicBoolean(false);
            final java.util.concurrent.atomic.AtomicReference<String> tellResultRef =
                    new java.util.concurrent.atomic.AtomicReference<>("");
            final java.util.concurrent.atomic.AtomicReference<String> regenLangRef =
                    new java.util.concurrent.atomic.AtomicReference<>("tptp");
            final String tellStmt = stmt;
            final JspWriter jspOut = out;  // Capture for use in lambda
            final String sessionId = session.getId();  // Capture session ID for session-specific UA
            // ONE atomic critical section
            kb.withUserAssertionLock(() -> {
                boolean mustRegen = kb.tellRequiresBaseRegeneration(tellStmt);
                mustRegenBaseRef.set(mustRegen);
                String tellResult = kb.tell(tellStmt, sessionId);
                tellResultRef.set(tellResult);
                if (mustRegen) {
                    final String requestedLang = SUMOKBtoTPTPKB.getLang(); // "fof" or "tff"
                    final String lang = "fof".equals(requestedLang) ? "tptp" : "tff";
                    regenLangRef.set(lang);
                    // Update spinner message before slow regen (early flush to iframe)
                    jspOut.println("<script>");
                    jspOut.println("if(parent.document.getElementById('spinTitle'))");
                    jspOut.println("  parent.document.getElementById('spinTitle').textContent='Regenerating KB...';");
                    jspOut.println("if(parent.document.getElementById('spinSub'))");
                    jspOut.println("  parent.document.getElementById('spinSub').textContent='Full TPTP regen required - please wait';");
                    jspOut.println("</script>");
                    jspOut.flush();
                    // Use session-specific TPTP generation for session-isolated tells
                    if (sessionId != null && !sessionId.isEmpty()) {
                        System.out.println("INFO AskTell.jsp(Tell): Session-specific regen required for session " + sessionId +
                                " -> regenerating session " + kb.name + "." + lang + " (Tell changed schema/transitive facts)");
                        com.articulate.sigma.trans.SessionTPTPManager.generateSessionTPTP(sessionId, kb, lang);
                    } else {
                        System.out.println("INFO AskTell.jsp(Tell): FULL base regen required -> regenerating "
                                + kb.name + "." + lang + " (Tell changed schema/transitive facts)");
                        TPTPGenerationManager.generateProperFile(kb, lang);
                    }
                } else {
                    // Reset spinner message when no regeneration needed (prevent stale message)
                    jspOut.println("<script>");
                    jspOut.println("if(parent.document.getElementById('spinTitle'))");
                    jspOut.println("  parent.document.getElementById('spinTitle').textContent='Processing...';");
                    jspOut.println("if(parent.document.getElementById('spinSub'))");
                    jspOut.println("  parent.document.getElementById('spinSub').textContent='';");
                    jspOut.println("</script>");
                    jspOut.flush();
                }
                return null;
            });
            final boolean mustRegenBase = mustRegenBaseRef.get();
            final String tellResult = tellResultRef.get();
            final String regenLang = regenLangRef.get();
            final boolean ok =
                    tellResult != null
                            && !tellResult.toLowerCase().startsWith("error")
                            && !tellResult.toLowerCase().contains("could not be added");
%>
    <div class="atp-result-panel">
        <div class="result-header">
            <span class="szs-badge <%= ok ? "szs-success" : "szs-error" %>"><%= ok ? "Success" : "Error" %></span>
            <span class="engine-tag">Tell Assertion</span>
        </div>
        <div class="result-meta">
        <span>Formula:
            <code><%= htmlEncode(stmt.length() > 120 ? stmt.substring(0, 120) + "..." : stmt) %></code>
        </span>
            <% if (mustRegenBase) { %>
            <span style="color:#b35900;">Triggered base regen: <code><%= kb.name %>.<%= regenLang %></code></span>
            <% } %>
        </div>
        <% if (mustRegenBase) { %>
        <div style="border:1px solid #ff9900; background:#fff8f0; padding:10px; border-radius:6px; margin-top:10px;">
            <b>Warning:</b> This Tell changed schema/transitive facts, so a full base regeneration was required.
        </div>
        <% } %>
        <div style="margin-top: 10px; padding: 10px; background: #f6f8fa; border-radius: 4px;">
            <%= htmlEncode(tellResult) %>
        </div>
    </div>
    <%
            } catch (com.articulate.sigma.tp.FormulaTranslationException fte) {
                renderExceptionPanel(fte, out);
            } catch (com.articulate.sigma.tp.ArityException ae) {
                renderExceptionPanel(ae, out);
            } catch (com.articulate.sigma.tp.ATPException atpe) {
                renderExceptionPanel(atpe, out);
            }
        } // end else (generation ready)
        }
    %>
</div>
<%!
    /** 1) One place to set Vampire mode */
    void setVampMode(String mode){
        if ("CASC".equals(mode)) com.articulate.sigma.tp.Vampire.mode = com.articulate.sigma.tp.Vampire.ModeType.CASC;
        else if ("Avatar".equals(mode)) com.articulate.sigma.tp.Vampire.mode = com.articulate.sigma.tp.Vampire.ModeType.AVATAR;
        else if ("Custom".equals(mode)) com.articulate.sigma.tp.Vampire.mode = com.articulate.sigma.tp.Vampire.ModeType.CUSTOM;
    }

    void setGraphFormat(String format, com.articulate.sigma.trans.TPTP3ProofProcessor tpp){
        if ("TPTP".equalsIgnoreCase(format))
            tpp.setGraphFormulaFormat(TPTP3ProofProcessor.GraphFormulaFormat.TPTP);
        else
            tpp.setGraphFormulaFormat(TPTP3ProofProcessor.GraphFormulaFormat.SUO_KIF);
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
<%!
    /** Render the ATPResult panel showing SZS status, timing, and diagnostics */
    void renderATPResultPanel(com.articulate.sigma.tp.ATPResult r,
                              javax.servlet.jsp.JspWriter out) throws java.io.IOException {
        if (r == null) return;
        out.println("<div class='atp-result-panel'>");
        // Header with status badge
        out.println("<div class='result-header'>");
        String statusName = r.getSzsStatus() != null ? r.getSzsStatus().getTptpName() : "Unknown";
        String cssClass = r.getCssClass();
        String szsUrl = "https://tptp.org/UserDocs/SZSOntology/";
//        out.println("<span class='szs-badge " + cssClass + "'>" + htmlEncode(statusName) + "</span>");
        out.println("<a class='szs-link' href='" + szsUrl + "' target='_blank' rel='noopener noreferrer'>"
                + "<span class='szs-badge " + cssClass + "'>" + htmlEncode(statusName) + "</span>"
                + "</a>");
        String engineInfo = r.getEngineName() != null ? r.getEngineName() : "Prover";
        if (r.getEngineMode() != null && !r.getEngineMode().isEmpty()) {
            engineInfo += " (" + r.getEngineMode() + ")";
        }
        out.println("<span class='engine-tag'>" + htmlEncode(engineInfo) + "</span>");
        out.println("</div>");
        // Metadata row
        out.println("<div class='result-meta'>");
        if (r.getInputLanguage() != null) {
            out.println("<span>Input: " + htmlEncode(r.getInputLanguage()) + "</span>");
        }
        out.println("<span>Time: " + r.getElapsedTimeMs() + "ms");
        if (r.getTimeoutMs() > 0) {
            out.println(" / " + r.getTimeoutMs() + "ms limit");
        }
        out.println("</span>");
        if (r.getExitCode() != 0 && r.getExitCode() != -1) {
            out.println("<span>Exit: " + r.getExitCode() + "</span>");
        }
        if (r.isTimedOut()) {
            out.println("<span style='color:#856404;'>Timed out</span>");
        }
        out.println("</div>");
        // Error/diagnostics section (only if there are errors)
        if (r.hasErrors() || r.hasStderr()) {
            out.println("<details class='result-errors' open>");
            out.println("<summary>Diagnostics</summary>");
            out.println("<pre>");
            // Show primary error first
            if (r.getPrimaryError() != null && !r.getPrimaryError().isEmpty()) {
                out.println(htmlEncode(r.getPrimaryError()));
            }
            // Show SZS diagnostics if available
            if (r.getSzsDiagnostics() != null && !r.getSzsDiagnostics().isEmpty()) {
                out.println("SZS: " + htmlEncode(r.getSzsDiagnostics()));
            }
            // Show error lines
            java.util.List<String> errorLines = r.getErrorLines();
            if (errorLines != null && !errorLines.isEmpty()) {
                for (int i = 0; i < Math.min(20, errorLines.size()); i++) {
                    out.println(htmlEncode(errorLines.get(i)));
                }
                if (errorLines.size() > 20) {
                    out.println("... (" + (errorLines.size() - 20) + " more lines)");
                }
            }
            // Show stderr if different from error lines
            java.util.List<String> stderr = r.getStderr();
            if (stderr != null && !stderr.isEmpty() && (errorLines == null || errorLines.isEmpty())) {
                for (int i = 0; i < Math.min(15, stderr.size()); i++) {
                    out.println(htmlEncode(stderr.get(i)));
                }
                if (stderr.size() > 15) {
                    out.println("... (" + (stderr.size() - 15) + " more lines)");
                }
            }
            out.println("</pre>");
            out.println("</details>");
        }
        // Raw output (collapsible, only show if there's content)
        java.util.List<String> stdout = r.getStdout();
        if (stdout != null && !stdout.isEmpty()) {
            out.println("<details class='result-raw'>");
            out.println("<summary>Raw Prover Output (" + stdout.size() + " lines)</summary>");
            out.println("<pre>");
            int total = stdout.size();
            int start = Math.max(0, total - 200);   // last 200
            for (int i = start; i < total; i++) {
                out.println(htmlEncode(stdout.get(i)));
            }
            if (total > 200) {
                out.println("... (" + (total - 200) + " earlier lines omitted)");
            }
            out.println("</pre>");
            out.println("</details>");
        }
        out.println("</div>");
    }

    /** Render an exception panel for ATP exceptions */
    void renderExceptionPanel(com.articulate.sigma.tp.ATPException e,
                              javax.servlet.jsp.JspWriter out) throws java.io.IOException {
        if (e == null) return;
        out.println("<div class='exception-panel'>");
        out.println("<h4>" + htmlEncode(e.getEngineName() != null ? e.getEngineName() : "Prover") + " Error</h4>");
        out.println("<p>" + htmlEncode(e.getMessage()) + "</p>");
        // Show command line if available
        if (e.getCommandLine() != null && !e.getCommandLine().isEmpty()) {
            out.println("<div class='meta'>Command: <code>" + htmlEncode(e.getCommandLineString()) + "</code></div>");
        }
        // Show stderr if available
        System.out.println(e.getStderr());
        if (e.hasStderr()) {
            out.println("<details open>");
            out.println("<summary>Error Output</summary>");
            out.println("<pre>");
            java.util.List<String> stderr = e.getStderr();
            for (int i = 0; i < Math.min(15, stderr.size()); i++) {
                out.println(htmlEncode(stderr.get(i)));
            }
            if (stderr.size() > 15) {
                out.println("... (" + (stderr.size() - 15) + " more lines)");
            }
            out.println("</pre>");
            out.println("</details>");
        }
        // Show suggestion
        String suggestion = e.getSuggestion();
        if (suggestion != null && !suggestion.isEmpty()) {
            out.println("<div class='suggestion'>");
            out.println("<strong>Suggestion:</strong><br>");
            out.println(htmlEncode(suggestion).replace("\n", "<br>"));
            out.println("</div>");
        }
        out.println("</div>");
    }
    /** HTML-encode a string to prevent XSS */
    String htmlEncode(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }
%>
<p>
    <%@ include file="Postlude.jsp" %>
    <iframe name="runFrame" id="runFrame" style="display:none;"></iframe>
</body>
</html>