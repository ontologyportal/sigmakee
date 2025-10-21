<%@ page import="com.articulate.sigma.nlg.LanguageFormatter" %>
<%@ page import="com.articulate.sigma.utils.StringUtil" %>
<%@ page import="com.articulate.sigma.InferenceTestSuite" %>
<%@ page import="java.io.File, java.util.Arrays, java.util.Comparator, java.util.Set" %>
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
            // If you implemented the streaming JSP:
            const url = 'ViewTest.jsp?name=' + encodeURIComponent(sel.value);

            // If instead you copy tests under /sigma/tests, use:
            // const url = 'tests/' + encodeURIComponent(sel.value);
            window.open(url, '_blank');
        }
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

    com.articulate.sigma.tp.EProver eProver = null;
    com.articulate.sigma.tp.Vampire vampire = null;

    String lineHtml =
            "<table ALIGN='LEFT' WIDTH='40%'><tr><TD BGCOLOR='#AAAAAA'><IMG SRC='pixmaps/1pixel.gif' width=1 height=1 border=0></TD></tr></table><BR>\n";
%>

<body style="face=Arial,Helvetica" bgcolor="#FFFFFF">
<form name="AskTell" id="AskTell" action="AskTell.jsp" method="POST">
    <%
        String pageName = "AskTell";
        String pageString = "Inference Interface";
    %>
    <%@include file="CommonHeader.jsp" %>

    <!-- ===== INPUT ===== -->
    <fieldset>
        <legend>Input</legend>
        <div class="row">
            <label><input type="radio" name="runSource" value="custom"
                <%= "test".equals(session.getAttribute("runSource")) ? "" : "checked" %> > Custom query</label>
            &nbsp;&nbsp;
            <label><input type="radio" name="runSource" value="test"
                <%= "test".equals(session.getAttribute("runSource")) ? "checked" : "" %> > Saved test (.tq)</label>
        </div>

        <div class="row" id="lblCustom">
            <textarea rows="5" cols="70" name="stmt" id="stmtArea"><%=stmt%></textarea>
        </div>

        <%
            String testDir = KBmanager.getMgr().getPref("inferenceTestDir");
            File[] tqFiles = (testDir == null) ? new File[0]
                    : new File(testDir).listFiles((d,n) -> n.endsWith(".tq"));
            if (tqFiles == null) tqFiles = new File[0];
            Arrays.sort(tqFiles, Comparator.comparing(File::getName));
            if (selectedTest == null && tqFiles.length > 0) selectedTest = tqFiles[0].getName();
        %>

            <!-- ===== Open test in a new page ===== -->
        <div class="row" id="lblTest">
            <b>Test:</b>
            <select name="testName" id="testName">
                <% for (File f : tqFiles) {
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

            <span class="muted">(Uses the configuration below)</span>
        </div>
    </fieldset>

    <!-- ===== CONFIG ===== -->
    <fieldset>
        <legend>Configuration (applies to both)</legend>

        Maximum answers: <input type="text" name="maxAnswers" value="<%=maxAnswers%>">
        &nbsp; Query time limit: <input type="text" name="timeout" value="<%=timeout%>"><br>

        [ <input type="radio" id="TPTPlang" name="TPTPlang" value="tptp"
        <% if (SUMOformulaToTPTPformula.lang.equals("fof")) { out.print(" CHECKED"); } %> >
        <label>tptp mode</label>
        <input type="radio" id="TPTPlang" name="TPTPlang" value="tff"
            <% if (SUMOformulaToTPTPformula.lang.equals("tff")){ out.print(" CHECKED"); } %> >
        <label>tff mode</label> ]
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
        try {
            if ("test".equals(runSource)) {
                // ---- RUN SAVED TEST ----
                try { InferenceTestSuite.resetAllForInference(kb); } catch (IOException ignore) {}
                InferenceTestSuite its = new InferenceTestSuite();
                String testPath = KBmanager.getMgr().getPref("inferenceTestDir")
                        + File.separator + session.getAttribute("selectedTest");
                InferenceTestSuite.InfTestData itd = its.readTestFile(new File(testPath));
                if (itd == null) {
                    out.println("<font color='red'>Could not read test file.</font>");
                } else {
                    for (String s : itd.statements) if (!StringUtil.emptyString(s)) kb.tell(s);

                    // Use the maxAns and timeout values from the tq files
//                    int maxAns = Math.max(1, (itd.expectedAnswers==null)?1:itd.expectedAnswers.size());
//                    int tmo = InferenceTestSuite.overrideTimeout ? InferenceTestSuite._DEFAULT_TIMEOUT
//                            : Math.max(1, itd.timeout);

                    // Always use page values, ignore .tq and suite override
                    int maxAns = Math.max(1, maxAnswers);
                    int tmo = Math.max(1, timeout);

                    System.out.println("Max Answers = "+maxAns);
                    System.out.println("Time-out = "+tmo);

                    FormulaPreprocessor fp = new FormulaPreprocessor();
                    Set<Formula> qs = fp.preProcess(new Formula(itd.query), true, kb);

                    com.articulate.sigma.trans.TPTP3ProofProcessor tpp =
                            new com.articulate.sigma.trans.TPTP3ProofProcessor();

                    for (Formula q : qs) {
                        String qstr = q.getFormula();
                        if ("EProver".equals(inferenceEngine)) {
                            com.articulate.sigma.tp.EProver eRun = kb.askEProver(qstr, tmo, maxAns);
                            tpp.parseProofOutput(eRun.output, qstr, kb, eRun.qlist);
                        } else if ("Vampire".equals(inferenceEngine)) {
                            if ("CASC".equals(vampireMode)) com.articulate.sigma.tp.Vampire.mode = com.articulate.sigma.tp.Vampire.ModeType.CASC;
                            if ("Avatar".equals(vampireMode)) com.articulate.sigma.tp.Vampire.mode = com.articulate.sigma.tp.Vampire.ModeType.AVATAR;
                            if ("Custom".equals(vampireMode)) com.articulate.sigma.tp.Vampire.mode = com.articulate.sigma.tp.Vampire.ModeType.CUSTOM;
                            com.articulate.sigma.tp.Vampire vRun = Boolean.TRUE.equals(modensPonens)
                                    ? kb.askVampireModensPonens(qstr, tmo, maxAns)
                                    : kb.askVampire(qstr, tmo, maxAns);
                            tpp.parseProofOutput(vRun.output, qstr, kb, vRun.qlist);
                        } else if ("LEO".equals(inferenceEngine)) {
                            com.articulate.sigma.tp.LEO leoRun = kb.askLeo(qstr, tmo, maxAns);
                            tpp.parseProofOutput(leoRun.output, qstr, kb, leoRun.qlist);
                        }
                    }

//                    String link = tpp.createProofDotGraph();
//                    if (tpp.proof.size() > 0) out.println("<a href=\"" + link + "\">graphical proof</a><p>");

                    String imgPath = null;
                    try {
                        imgPath = tpp.createProofDotGraph();   // absolute path on disk, e.g. .../webapps/sigma/graph/proof.dot.png
                    } catch (Exception _ignore) { imgPath = null; }

                    if (imgPath != null) {
                        // Where the webapp serves /graph from:
                        String webGraphDir = application.getRealPath("/graph");
                        if (webGraphDir == null) webGraphDir = new File(".").getAbsolutePath(); // fallback (shouldn't happen)

                        File onDisk = new File(imgPath);
                        File webDir  = new File(webGraphDir);
                        if (!webDir.exists()) webDir.mkdirs();

                        // If the image is not already under the webapp’s /graph, copy it there
                        File webImg = new File(webDir, onDisk.getName());
                        try {
                            if (!onDisk.getCanonicalPath().equals(webImg.getCanonicalPath())) {
                                java.nio.file.Files.copy(onDisk.toPath(), webImg.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                            }
                            String url = request.getContextPath() + "/graph/" + webImg.getName();  // e.g. /sigma/graph/proof.dot.png
                            out.println("<a href=\"" + url + "\" target=\"_blank\">graphical proof</a><p>");
                        }
                        catch (Exception copyEx) {
                            // Fall back: show a file:// link for local debugging (not ideal for remote clients)
                            out.println("<span style='color:#b00'>Could not publish proof image to /graph. "
                                    + "Path: " + imgPath + "</span><br>");
                        }
                    }

                    tpp.processAnswersFromProof(null, itd.query);
                    out.println(HTMLformatter.formatTPTP3ProofResult(tpp, itd.query, lineHtml, kbName, language));
                }
            } else {
                // ---- RUN CUSTOM QUERY (Ask) ----
                if (stmt.indexOf('@') != -1) throw(new IOException("Row variables not allowed in query: " + stmt));
                if ("EProver".equals(inferenceEngine)) {
                    eProver = kb.askEProver(stmt, timeout, maxAnswers);
                    com.articulate.sigma.trans.TPTP3ProofProcessor tpp = new com.articulate.sigma.trans.TPTP3ProofProcessor();
                    tpp.parseProofOutput(eProver.output, stmt, kb, eProver.qlist);

//                    String link = tpp.createProofDotGraph();
//                    out.println("<a href=\"" + link + "\">graphical proof</a><p>");

                    String imgPath = null;
                    try {
                        imgPath = tpp.createProofDotGraph();   // absolute path on disk, e.g. .../webapps/sigma/graph/proof.dot.png
                    } catch (Exception _ignore) { imgPath = null; }

                    if (imgPath != null) {
                        // Where the webapp serves /graph from:
                        String webGraphDir = application.getRealPath("/graph");
                        if (webGraphDir == null) webGraphDir = new File(".").getAbsolutePath(); // fallback (shouldn't happen)

                        File onDisk = new File(imgPath);
                        File webDir  = new File(webGraphDir);
                        if (!webDir.exists()) webDir.mkdirs();

                        // If the image is not already under the webapp’s /graph, copy it there
                        File webImg = new File(webDir, onDisk.getName());
                        try {
                            if (!onDisk.getCanonicalPath().equals(webImg.getCanonicalPath())) {
                                java.nio.file.Files.copy(onDisk.toPath(), webImg.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                            }
                            String url = request.getContextPath() + "/graph/" + webImg.getName();  // e.g. /sigma/graph/proof.dot.png
                            out.println("<a href=\"" + url + "\" target=\"_blank\">graphical proof</a><p>");
                        }
                        catch (Exception copyEx) {
                            // Fall back: show a file:// link for local debugging (not ideal for remote clients)
                            out.println("<span style='color:#b00'>Could not publish proof image to /graph. "
                                    + "Path: " + imgPath + "</span><br>");
                        }
                    }


                    out.println(HTMLformatter.formatTPTP3ProofResult(tpp,stmt,lineHtml,kbName,language));
                    if (!StringUtil.emptyString(tpp.status)) out.println("Status: " + tpp.status);
                } else if ("Vampire".equals(inferenceEngine)) {
                    if ("CASC".equals(vampireMode)) com.articulate.sigma.tp.Vampire.mode = com.articulate.sigma.tp.Vampire.ModeType.CASC;
                    if ("Avatar".equals(vampireMode)) com.articulate.sigma.tp.Vampire.mode = com.articulate.sigma.tp.Vampire.ModeType.AVATAR;
                    if ("Custom".equals(vampireMode)) com.articulate.sigma.tp.Vampire.mode = com.articulate.sigma.tp.Vampire.ModeType.CUSTOM;
                    vampire = Boolean.TRUE.equals(modensPonens)
                            ? kb.askVampireModensPonens(stmt, timeout, maxAnswers)
                            : kb.askVampire(stmt, timeout, maxAnswers);
                    if (vampire == null || vampire.output == null) out.println("<font color='red'>Error. No response from Vampire.</font>");
                    else {
                        com.articulate.sigma.trans.TPTP3ProofProcessor tpp = new com.articulate.sigma.trans.TPTP3ProofProcessor();
                        tpp.parseProofOutput(vampire.output, stmt, kb, vampire.qlist);

//                        String link = tpp.createProofDotGraph();
//                        if (tpp.proof.size() > 0) out.println("<a href=\"" + link + "\">graphical proof</a><p>");

                        /** Ensure web-visible graph path + build a proper URL */
                        String imgPath = null;
                        try {
                            imgPath = tpp.createProofDotGraph();   // absolute path on disk, e.g. .../webapps/sigma/graph/proof.dot.png
                        } catch (Exception _ignore) { imgPath = null; }

                        if (imgPath != null) {
                            // Where the webapp serves /graph from:
                            String webGraphDir = application.getRealPath("/graph");
                            if (webGraphDir == null) webGraphDir = new File(".").getAbsolutePath(); // fallback (shouldn't happen)

                            File onDisk = new File(imgPath);
                            File webDir  = new File(webGraphDir);
                            if (!webDir.exists()) webDir.mkdirs();

                            // If the image is not already under the webapp’s /graph, copy it there
                            File webImg = new File(webDir, onDisk.getName());
                            try {
                                if (!onDisk.getCanonicalPath().equals(webImg.getCanonicalPath())) {
                                    java.nio.file.Files.copy(onDisk.toPath(), webImg.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                                }
                                String url = request.getContextPath() + "/graph/" + webImg.getName();  // e.g. /sigma/graph/proof.dot.png
                                out.println("<a href=\"" + url + "\" target=\"_blank\">graphical proof</a><p>");
                            }
                            catch (Exception copyEx) {
                                // Fall back: show a file:// link for local debugging (not ideal for remote clients)
                                out.println("<span style='color:#b00'>Could not publish proof image to /graph. "
                                        + "Path: " + imgPath + "</span><br>");
                            }
                        }

                        tpp.processAnswersFromProof(vampire.qlist,stmt);
                        out.println(HTMLformatter.formatTPTP3ProofResult(tpp,stmt,lineHtml,kbName,language));
                    }
                } else if ("LEO".equals(inferenceEngine)) {
                    kb.leo = kb.askLeo(stmt,timeout,maxAnswers);
                    if (kb.leo == null || kb.leo.output == null) out.println("<font color='red'>Error. No response from LEO-III.</font>");
                    else {
                        com.articulate.sigma.trans.TPTP3ProofProcessor tpp = new com.articulate.sigma.trans.TPTP3ProofProcessor();
                        tpp.parseProofOutput(kb.leo.output, stmt, kb, kb.leo.qlist);

//                        String link = tpp.createProofDotGraph();
//                        if (tpp.proof.size() > 0) out.println("<a href=\"" + link + "\">graphical proof</a><p>");

                        String imgPath = null;
                        try {
                            imgPath = tpp.createProofDotGraph();   // absolute path on disk, e.g. .../webapps/sigma/graph/proof.dot.png
                        } catch (Exception _ignore) { imgPath = null; }

                        if (imgPath != null) {
                            // Where the webapp serves /graph from:
                            String webGraphDir = application.getRealPath("/graph");
                            if (webGraphDir == null) webGraphDir = new File(".").getAbsolutePath(); // fallback (shouldn't happen)

                            File onDisk = new File(imgPath);
                            File webDir  = new File(webGraphDir);
                            if (!webDir.exists()) webDir.mkdirs();

                            // If the image is not already under the webapp’s /graph, copy it there
                            File webImg = new File(webDir, onDisk.getName());
                            try {
                                if (!onDisk.getCanonicalPath().equals(webImg.getCanonicalPath())) {
                                    java.nio.file.Files.copy(onDisk.toPath(), webImg.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                                }
                                String url = request.getContextPath() + "/graph/" + webImg.getName();  // e.g. /sigma/graph/proof.dot.png
                                out.println("<a href=\"" + url + "\" target=\"_blank\">graphical proof</a><p>");
                            }
                            catch (Exception copyEx) {
                                // Fall back: show a file:// link for local debugging (not ideal for remote clients)
                                out.println("<span style='color:#b00'>Could not publish proof image to /graph. "
                                        + "Path: " + imgPath + "</span><br>");
                            }
                        }

                        tpp.processAnswersFromProof(kb.leo.qlist,stmt);
                        out.println(HTMLformatter.formatTPTP3ProofResult(tpp,stmt,lineHtml,kbName,language));
                    }
                }
            }
        } catch (IOException ioe) {
            out.println("<font color='red'>" + ioe.getMessage() + "</font>");
        }
    }

    // ---- Global flags for paraphrasing ----
    System.out.println("AskTell.jsp / showProofInEnglish = "+showEnglish);
    HTMLformatter.proofParaphraseInEnglish = showEnglish;
    System.out.println("AskTell.jsp / showProofFromLLM = "+llmProof);
    LanguageFormatter.paraphraseLLM = llmProof;

    if (status != null && status.toString().length() > 0) { out.println("Status: "); out.println(status.toString()); }
%>

<p>
    <%@ include file="Postlude.jsp" %>
</body>
</html>