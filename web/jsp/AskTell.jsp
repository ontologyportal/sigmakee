<%@ page import="com.articulate.sigma.nlg.LanguageFormatter" %>
<%@ page import="com.articulate.sigma.utils.StringUtil" %>
<%@include file="Prelude.jsp" %>
<%
/** This code is copyright Teknowledge (c) 2003, Articulate Software (c) 2003-present,
    Infosys (c) 2017-2020.

    This software is released under the GNU Public License
    <http://www.gnu.org/copyleft/gpl.html>.

    Please cite the following article in any publication with references:

    Pease A., and Benzmüller C. (2013). Sigma: An Integrated Development Environment
    for Logical Theories. AI Communications 26, pp79-97.  See also
    http://github.com/ontologyportal
*/

if (!role.equalsIgnoreCase("admin")) {
    response.sendRedirect("login.html");
    return;
}

  String systemsDir = KBmanager.getMgr().getPref("systemsDir");
%>
<HTML>
<HEAD>
  <TITLE>Sigma Knowledge Engineering Environment - Ask/Tell</TITLE>

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
            if (drop) {
                drop.disabled = !mpOn;
                if (!mpOn) drop.checked = false; // optional: uncheck when disabled
            }
        }

        window.onload = function () {
            toggleVampireOptions();
            document.querySelectorAll('input[name="inferenceEngine"], #ModensPonens')
                .forEach(el => el.addEventListener('change', toggleVampireOptions));
        };
    </script>

</HEAD>
<%
    System.out.println("INFO in AskTell.jsp");
    StringBuilder status = new StringBuilder();
    ArrayList processedStmts = null;

    String req = request.getParameter("request");
    String stmt = request.getParameter("stmt");
    String cwa = request.getParameter("CWA");

    // --- ModensPonens: read/update session + use boolean for rendering ---
    Boolean modensPonens = (Boolean) session.getAttribute("ModensPonens");
    if (req != null) {
        modensPonens = request.getParameter("ModensPonens") != null
                || "yes".equalsIgnoreCase(request.getParameter("ModensPonens"))
                || "on".equalsIgnoreCase(request.getParameter("ModensPonens"))
                || "true".equalsIgnoreCase(request.getParameter("ModensPonens"));
        session.setAttribute("ModensPonens", modensPonens);
    }
    if (modensPonens == null) modensPonens = false;

    // --- dropOnePremise: read/update session + global ---
    Boolean dropOnePremise = (Boolean) session.getAttribute("dropOnePremise");
    if (req != null) { // form submitted
        dropOnePremise = request.getParameter("dropOnePremise") != null;
        session.setAttribute("dropOnePremise", dropOnePremise);
    }
    if (dropOnePremise == null) dropOnePremise = false;
    KB.dropOnePremiseFormulas = dropOnePremise;

    String selectedTest = (String) session.getAttribute("selectedTest");
    if (req != null && request.getParameter("testName") != null) {
        selectedTest = request.getParameter("testName");
        session.setAttribute("selectedTest", selectedTest);
    }

    if (StringUtil.emptyString(cwa))
        cwa = "no";
    if (cwa.equals("yes"))
        SUMOKBtoTPTPKB.CWA = true;
    else
        SUMOKBtoTPTPKB.CWA = false;
    String inferenceEngine = request.getParameter("inferenceEngine");
    String vampireMode = request.getParameter("vampireMode");
    if (StringUtil.emptyString(vampireMode))
        vampireMode = "CASC";
    String TPTPlang = request.getParameter("TPTPlang");
    if (StringUtil.emptyString(TPTPlang) || TPTPlang.equals("fof")) {
        TPTPlang = "fof";
        SUMOformulaToTPTPformula.lang = "fof";
        SUMOKBtoTPTPKB.lang = "fof";
    }
    if (TPTPlang.equals("tff")) {
        TPTPlang = "tff";
        SUMOformulaToTPTPformula.lang = "tff";
        SUMOKBtoTPTPKB.lang = "tff";
    }
    System.out.println("INFO in AskTell.jsp: inferenceEngine: " + inferenceEngine);
    System.out.println("INFO in AskTell.jsp: vampireMode: " + vampireMode);
    System.out.println("INFO in AskTell.jsp: TPTPlang: " + TPTPlang);
    System.out.println("INFO in AskTell.jsp: cwa: " + cwa);
    boolean syntaxError = false;
    boolean english = false;
    String englishStatement = null;
    int maxAnswers = 1;
    int timeout = 30;

    Boolean showEnglish = (Boolean) session.getAttribute("showProofInEnglish");
    if (req != null) { // form submitted
        showEnglish = "yes".equalsIgnoreCase(request.getParameter("showProofInEnglish"));
        session.setAttribute("showProofInEnglish", showEnglish);
    }
    if (showEnglish == null) showEnglish = true; // first load default = ON

    Boolean llmProof = (Boolean) session.getAttribute("showProofFromLLM");
    if (req != null) { // form submitted
        llmProof = "yes".equalsIgnoreCase(request.getParameter("showProofFromLLM"));
        session.setAttribute("showProofFromLLM", llmProof);
    }
    if (llmProof == null) llmProof = false; // first load default = OFF

    // ---- Ollama availability check ----
    boolean ollamaUp = false;
    try {
        ollamaUp = LanguageFormatter.checkOllamaHealth();
    } catch (Exception ignore) {
        ollamaUp = false;
        System.out.println("ERROR - There was an error while attemping to Test Ollama Availability!");
    }

// If Ollama is down, force the toggle off so server logic never tries to call it
    if (!ollamaUp) {
        llmProof = false;
        session.setAttribute("showProofFromLLM", false);
    }

    String eproverExec = KBmanager.getMgr().getPref("eprover");
    String tptpFile = KBmanager.getMgr().getPref("kbDir") + File.separator + "SUMO.tptp";
    File ep = new File(eproverExec);
    if (kb.eprover == null && ep.exists())
        kb.eprover = new com.articulate.sigma.tp.EProver(eproverExec,tptpFile);
    if (inferenceEngine == null) {
        if (kb.eprover != null)
            inferenceEngine = "EProver";
        else
            inferenceEngine = "Vampire";
    }
    System.out.println("INFO in AskTell.jsp: Engine: " + inferenceEngine);
    if (request.getParameter("maxAnswers") != null)
        maxAnswers = Integer.parseInt(request.getParameter("maxAnswers"));
    if (request.getParameter("timeout") != null)
        timeout= Integer.parseInt(request.getParameter("timeout"));

    if ((kbName == null) || kbName.equals("")) {
        System.out.println("Error: No knowledge base specified");
        return;
    }

    if (stmt != null)
        System.out.println("  text box input: " + stmt.trim());

    if (stmt == null || stmt.equalsIgnoreCase("null"))   // check if there is an attribute for stmt
        stmt = "(instance ?X Relation)";
    else {
        if (stmt.trim().charAt(0) != '(')
            english = true;
        else {
            String msg = (new KIF()).parseStatement(stmt);
            if (msg != null) {
                status.append("<font color='red'>Error: Syntax Error in statement: " + stmt);
                status.append("Message: " + msg + "</font><br>\n");
                syntaxError = true;
            }
        }
    }

    if (english) {
        englishStatement = stmt;
        if (!KBmanager.getMgr().getPref("loadCELT").equalsIgnoreCase("yes") || kb.celt == null) {
            stmt = null;
            status.append("<font color='red'>CELT not loaded.  Only KIF syntax is allowed.</font><br>\n");
        }
        else {
            System.out.println("INFO in AskTell.jsp: stmt: " + stmt);
            System.out.println("INFO in AskTell.jsp: kb: " + kb);
            System.out.println("INFO in AskTell.jsp: kb.celt " + kb.celt);
            stmt = kb.celt.submit(stmt);
        }
        System.out.println("INFO in AskTell.jsp: Completed translation.");
    }

    if (stmt == null || stmt.length() < 2 || stmt.trim().charAt(0) != '(') {
        syntaxError = true;
        status.append("<font color='red'>Error: Syntax Error or parsing failure in statement: " + englishStatement + "</font><br>\n");
        stmt = englishStatement;
    }

    com.articulate.sigma.tp.EProver eProver = null;
    com.articulate.sigma.tp.Vampire vampire = null;

    String lineHtml =
      "<table ALIGN='LEFT' WIDTH='40%'><tr><TD BGCOLOR='#AAAAAA'><IMG SRC='pixmaps/1pixel.gif' width=1 height=1 border=0></TD></tr></table><BR>\n";

    if (req != null && !syntaxError) {
        try {
            if (stmt.indexOf('@') != -1)
                throw(new IOException("Row variables not allowed in query: " + stmt));
            if (req.equalsIgnoreCase("tell")) {
                Formula statement = new Formula(stmt);
                System.out.println("INFO in AskTell.jsp: statement: " + stmt);
                String kbHref = "http://" + hostname + ":" + port + "/sigma/Browse.jsp?kb=" + kbName;
                status.append(kb.tell(stmt) + "<P>\n" + statement.htmlFormat(kbHref));
            }
            if (req.equalsIgnoreCase("ask") && inferenceEngine.equals("EProver")) {
		        eProver = kb.askEProver(stmt, timeout, maxAnswers);
		        System.out.println("INFO in AskTell.jsp------------------------------------");
		        System.out.println("EProver output: " + eProver.output);
            }
            if (req.equalsIgnoreCase("ask") && inferenceEngine.equals("Vampire")) {
                if (vampireMode.equals("CASC"))
                    com.articulate.sigma.tp.Vampire.mode = com.articulate.sigma.tp.Vampire.ModeType.CASC;
                if (vampireMode.equals("Avatar"))
                    com.articulate.sigma.tp.Vampire.mode = com.articulate.sigma.tp.Vampire.ModeType.AVATAR;
                if (vampireMode.equals("Custom"))
                    com.articulate.sigma.tp.Vampire.mode = com.articulate.sigma.tp.Vampire.ModeType.CUSTOM;
//                vampire = kb.askVampire(stmt, timeout, maxAnswers);
                vampire = modensPonens
                        ? kb.askVampireModensPonens(stmt, timeout, maxAnswers)
                        : kb.askVampire(stmt, timeout, maxAnswers);
                System.out.println("INFO in AskTell.jsp------------------------------------");
                System.out.println("Vampire output: " + vampire.toString());
            }
            if (req.equalsIgnoreCase("ask") && inferenceEngine.equals("LEO")) {
                kb.leo = kb.askLeo(stmt,timeout,maxAnswers);
            }
        }
        catch (IOException ioe) {
            status.append(ioe.getMessage());
        }
    }
%>

<BODY style="face=Arial,Helvetica" BGCOLOR=#FFFFFF>
<FORM name="AskTell" ID="AskTell" action="AskTell.jsp" METHOD="POST">
    <%
        String pageName = "AskTell";
        String pageString = "Inference Interface";
    %>
    <%@include file="CommonHeader.jsp" %>

    <IMG SRC='pixmaps/1pixel.gif' width=1 height=1 border=0><BR>
    <textarea rows="5" cols="70" name="stmt"><%=stmt%></textarea>
    <br>
    Maximum answers: <input TYPE="TEXT" NAME="maxAnswers" VALUE="<%=maxAnswers%>">
    Query time limit:<input TYPE="TEXT" NAME="timeout" VALUE="<%=timeout%>"><BR>
    [
          <input type="radio" id="TPTPlang" name="TPTPlang" value="tptp"
              <% if (SUMOformulaToTPTPformula.lang.equals("fof")) { out.print(" CHECKED"); } %> >
              <label>tptp mode</label>
          <input type="radio" id="TPTPlang" name="TPTPlang" value="tff"
              <% if (SUMOformulaToTPTPformula.lang.equals("tff")){ out.print(" CHECKED"); } %> >
              <label>tff mode</label> ]
    &nbsp;&nbsp;<INPUT TYPE=CHECKBOX NAME="CWA" id="CWA" VALUE="yes" <% if (cwa.equals("yes")) {%>CHECKED<%}%>
    <label>Closed World Assumption</label><br>
    Choose an inference engine:<BR>

    <INPUT TYPE=RADIO NAME="inferenceEngine" VALUE="LEO" <% if (inferenceEngine.equals("LEO")) {%>CHECKED<%}%>
           onclick="document.getElementById('SoTPTPControl').style.display='none'" >
    LEO-III <BR>

    <INPUT TYPE=RADIO NAME="inferenceEngine" VALUE="EProver" <% if (inferenceEngine.equals("EProver")) {%>CHECKED<%}%>
           onclick="document.getElementById('SoTPTPControl').style.display='none'"
        <% if (kb.eprover == null) { %> DISABLED <% } %> >
    EProver <BR>

    <INPUT TYPE=RADIO NAME="inferenceEngine" VALUE="Vampire" <% if (inferenceEngine.equals("Vampire")) {%>CHECKED<%}%>
           onclick="document.getElementById('SoTPTPControl').style.display='none'"
        <% if (KBmanager.getMgr().getPref("vampire") == null) { %> DISABLED <% } %> >
    Vampire : [
      <input type="radio" id="CASC" name="vampireMode" value="CASC"
          <% if (vampireMode.equals("CASC")) { out.print(" CHECKED"); } %> >
          <label>CASC mode</label>
      <input type="radio" id="Avatar" name="vampireMode" value="Avatar"
          <% if (vampireMode.equals("Avatar")) { out.print(" CHECKED"); } %> >
          <label>Avatar mode</label>
      <input type="radio" id="Custom" name="vampireMode" value="Custom"
          <% if (vampireMode.equals("Custom")) { out.print(" CHECKED"); } %> >
          <label>Custom mode</label>]

      <input type="checkbox" id="ModensPonens" name="ModensPonens" value="yes" <% if (modensPonens) { out.print(" CHECKED"); } %> >
        <label for="ModensPonens">Modens Ponens</label>
        <span title="Runs Vampire with modens-ponens-only routine in authored-only axioms Proof">&#9432;</span> [

      <input type="checkbox" name="dropOnePremise" id="dropOnePremise" value="true"
            <% if (Boolean.TRUE.equals(dropOnePremise)) { out.print(" CHECKED"); } %> >
        <label for="dropOnePremise">Drop One-Premise Formulas</label>]
    <br>

    <input type="checkbox" name="showProofInEnglish" value="yes"
           <% if (Boolean.TRUE.equals(showEnglish)) { %>checked<% } %> >
    <label>Show English Paraphrases</label><BR>

    <input type="checkbox" name="showProofFromLLM" value="yes"
        <%= (Boolean.TRUE.equals(llmProof) && ollamaUp) ? "checked" : "" %>
        <%= ollamaUp ? "" : "disabled" %> >
    <label>Use LLM for Paraphrasing</label>
    <% if (!ollamaUp) { %>
    <span title="Ollama is not running. Start Ollama to enable LLM paraphrasing.">&#9432;</span>
    <% } %>
    <br>
    <br>

    <INPUT type="submit" name="request" value="Ask">

<% if (role != null && role.equalsIgnoreCase("admin")) { %>
    <INPUT type="submit" name="request" value="Tell"><BR>
<% } %>

    <hr>

    <%
        String testDir = KBmanager.getMgr().getPref("inferenceTestDir");
        File[] tqFiles = (testDir == null) ? new File[0]
                : new File(testDir).listFiles((d,n) -> n.endsWith(".tq"));
        if (tqFiles == null) tqFiles = new File[0];
        Arrays.sort(tqFiles, Comparator.comparing(File::getName));

        // --- pick default (first file) if nothing selected yet ---
        if (selectedTest == null && tqFiles.length > 0)
            selectedTest = tqFiles[0].getName();
    %>
    <hr>
    <b>Run a saved test (.tq):</b>
    <select name="testName">
        <% for (File f : tqFiles) {
            String fname = f.getName();
            boolean isSelected = fname.equals(selectedTest);
        %>
        <option value="<%= fname %>" <%= isSelected ? "selected" : "" %>>
            <%= fname %>
        </option>
        <% } %>
    </select>
    <input type="submit" name="request" value="RunTest">
    <hr>
    <br>

</FORM>
<table ALIGN='LEFT' WIDTH='80%'><tr><TD BGCOLOR='#AAAAAA'>
<IMG SRC='pixmaps/1pixel.gif' width=1 height=1 border=0></TD></tr></table><BR>

<%

    if ("RunTest".equalsIgnoreCase(req)) {
        // Reset user assertions so tests don’t contaminate each other
        try { InferenceTestSuite.resetAllForInference(kb); } catch (IOException ignore) {}

        InferenceTestSuite its = new InferenceTestSuite();
        String testPath = KBmanager.getMgr().getPref("inferenceTestDir")
                + File.separator + request.getParameter("testName");

        // --- Read the .tq ---
        InferenceTestSuite.InfTestData itd = its.readTestFile(new File(testPath));
        if (itd == null) {
            status.append("<font color='red'>Could not read test file.</font><br>");
        } else {
            // Load any extra statements the test needs
            for (String s : itd.statements) if (!StringUtil.emptyString(s)) kb.tell(s);

            // Default max answers / timeout
            int maxAns = Math.max(1, (itd.expectedAnswers == null) ? 1 : itd.expectedAnswers.size());
            int tmo = InferenceTestSuite.overrideTimeout
                    ? InferenceTestSuite._DEFAULT_TIMEOUT
                    : Math.max(1, itd.timeout);

            // Preprocess query → possibly multiple first-order variants
            FormulaPreprocessor fp = new FormulaPreprocessor();
            Set<Formula> qs = fp.preProcess(new Formula(itd.query), true, kb);

            com.articulate.sigma.trans.TPTP3ProofProcessor tpp =
                    new com.articulate.sigma.trans.TPTP3ProofProcessor();

            // Ask the chosen prover (reuse your MP / mode toggles)
            for (Formula q : qs) {
                String qstr = q.getFormula();
                if ("EProver".equals(inferenceEngine)) {
                    com.articulate.sigma.tp.EProver ep_run = kb.askEProver(qstr, tmo, maxAns);
                    tpp.parseProofOutput(ep_run.output, qstr, kb, ep_run.qlist);
                } else if ("Vampire".equals(inferenceEngine)) {
                    if ("CASC".equals(vampireMode))
                        com.articulate.sigma.tp.Vampire.mode = com.articulate.sigma.tp.Vampire.ModeType.CASC;
                    if ("Avatar".equals(vampireMode))
                        com.articulate.sigma.tp.Vampire.mode = com.articulate.sigma.tp.Vampire.ModeType.AVATAR;
                    if ("Custom".equals(vampireMode))
                        com.articulate.sigma.tp.Vampire.mode = com.articulate.sigma.tp.Vampire.ModeType.CUSTOM;

                    com.articulate.sigma.tp.Vampire v = (Boolean.TRUE.equals(modensPonens))
                            ? kb.askVampireModensPonens(qstr, tmo, maxAns)
                            : kb.askVampire(qstr, tmo, maxAns);
                    tpp.parseProofOutput(v.output, qstr, kb, v.qlist);
                } else if ("LEO".equals(inferenceEngine)) {
                    com.articulate.sigma.tp.LEO leo = kb.askLeo(qstr, tmo, maxAns);
                    tpp.parseProofOutput(leo.output, qstr, kb, leo.qlist);
                }
            }

            // Render like the “Ask” branch (graph link + HTML table)
            String link = tpp.createProofDotGraph();
            if (tpp.proof.size() > 0)
                out.println("<a href=\"" + link + "\">graphical proof</a><P>");
            tpp.processAnswersFromProof(null, itd.query);   // keep if you want bindings extraction
            out.println(HTMLformatter.formatTPTP3ProofResult(
                    tpp, itd.query, lineHtml, kbName, language));
        }
    }

    System.out.println("AskTell.jsp / showProofInEnglish = "+showEnglish);
    HTMLformatter.proofParaphraseInEnglish = showEnglish;

    System.out.println("AskTell.jsp / showProofFromLLM = "+llmProof);
    LanguageFormatter.paraphraseLLM = llmProof;

    if (status != null && status.toString().length() > 0) {
        out.println("Status: ");
        out.println(status.toString());
    }
    if (cwa.equals("yes"))
        SUMOKBtoTPTPKB.CWA = true;
    else
        SUMOKBtoTPTPKB.CWA = false;
    System.out.println("INFO in AskTell.jsp (2): cwa: " + cwa);
    System.out.println("INFO in AskTell.jsp (2): CWA: " + SUMOKBtoTPTPKB.CWA);
    if (inferenceEngine.equals("EProver")) {
        KBmanager.getMgr().prover = KBmanager.Prover.EPROVER;
        if ((eProver != null) && (eProver.output.contains("Syntax error detected")))
            out.println("<font color='red'>A syntax error was detected in your input.</font>");
        else if (eProver != null) {
        	System.out.println("in AskTell.jsp: parsing EProver results--------------");
        	System.out.println("output size: " + eProver.output.size());
            com.articulate.sigma.trans.TPTP3ProofProcessor tpp = new com.articulate.sigma.trans.TPTP3ProofProcessor();
        	tpp.parseProofOutput(eProver.output, stmt, kb, eProver.qlist);
        	String link = tpp.createProofDotGraph();
        	out.println("<a href=\"" + link + "\">graphical proof</a><P>");
        	System.out.println("in AskTell.jsp: HTML format results --------------");
            out.println(HTMLformatter.formatTPTP3ProofResult(tpp,stmt,lineHtml,kbName,language));
            System.out.println("in AskTell.jsp: EProver status: " + tpp.status);
            if (!StringUtil.emptyString(tpp.status))
                out.println("Status: " + tpp.status);
            //out.println("Output: " + eProver.output);
        }
    }
    if (inferenceEngine.equals("Vampire")) {
        KBmanager.getMgr().prover = KBmanager.Prover.VAMPIRE;
        if (vampireMode.equals("CASC"))
            com.articulate.sigma.tp.Vampire.mode = com.articulate.sigma.tp.Vampire.ModeType.CASC;
        if (vampireMode.equals("Avatar"))
            com.articulate.sigma.tp.Vampire.mode = com.articulate.sigma.tp.Vampire.ModeType.AVATAR;
        if (vampireMode.equals("Custom"))
            com.articulate.sigma.tp.Vampire.mode = com.articulate.sigma.tp.Vampire.ModeType.CUSTOM;
        if (req != null && req.equalsIgnoreCase("ask") && (vampire == null || vampire.output == null))
            out.println("<font color='red'>Error.  No response from Vampire.</font>");
        else if (vampire != null && (vampire.output != null) && (vampire.output.indexOf("Syntax error detected") != -1))
            out.println("<font color='red'>A syntax error was detected in your input.</font>");
        else if (vampire != null && vampire.output != null) {
            System.out.println("in AskTell.jsp: trying Vampire--------------");
            com.articulate.sigma.trans.TPTP3ProofProcessor tpp = new com.articulate.sigma.trans.TPTP3ProofProcessor();
            tpp.parseProofOutput(vampire.output, stmt, kb, vampire.qlist);
            String link = tpp.createProofDotGraph();
            if (tpp.proof.size() > 0)
                out.println("<a href=\"" + link + "\">graphical proof</a><P>");
            tpp.processAnswersFromProof(vampire.qlist,stmt);
            System.out.println("in AskTell.jsp: sending the HTML formatter--------------");
            out.println(HTMLformatter.formatTPTP3ProofResult(tpp,stmt,lineHtml,kbName,language));
        }
    }
    if (inferenceEngine.equals("LEO")) {
        KBmanager.getMgr().prover = KBmanager.Prover.LEO;
        if (kb.leo == null || kb.leo.output == null)
            out.println("<font color='red'>Error.  No response from LEO-III.</font>");
        if ((kb.leo != null) && (kb.leo.output.toString().indexOf("Syntax error detected") != -1))
            out.println("<font color='red'>A syntax error was detected in your input.</font>");
        else {
            System.out.println("in AskTell.jsp: trying LEO-III--------------");
            com.articulate.sigma.trans.TPTP3ProofProcessor tpp = new com.articulate.sigma.trans.TPTP3ProofProcessor();
            tpp.parseProofOutput(kb.leo.output, stmt, kb, kb.leo.qlist);
            String link = tpp.createProofDotGraph();
            if (tpp.proof.size() > 0)
                out.println("<a href=\"" + link + "\">graphical proof</a><P>");
            tpp.processAnswersFromProof(kb.leo.qlist,stmt);
            System.out.println("in AskTell.jsp: sending the HTML formatter--------------");
            out.println(HTMLformatter.formatTPTP3ProofResult(tpp,stmt,lineHtml,kbName,language));
        }

    }
%>
    <p>

<%@ include file="Postlude.jsp" %>

</BODY>
</HTML>