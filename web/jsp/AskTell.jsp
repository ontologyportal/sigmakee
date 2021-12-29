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

</HEAD>
<%
    System.out.println("INFO in AskTell.jsp");
    StringBuffer status = new StringBuffer();
    ArrayList processedStmts = null;

    String req = request.getParameter("request");
    String stmt = request.getParameter("stmt");
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
    boolean syntaxError = false;
    boolean english = false;
    String englishStatement = null;
    int maxAnswers = 1;
    int timeout = 30;

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
    String resultLeo = null;    

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
                vampire = kb.askVampire(stmt, timeout, maxAnswers);
                System.out.println("INFO in AskTell.jsp------------------------------------");
                System.out.println("Vampire output: " + vampire.toString());
            }
            if (req.equalsIgnoreCase("ask") && inferenceEngine.equals("LeoSine")) {
                resultLeo = kb.askLEO(stmt,timeout,maxAnswers,"LeoSine");
            }	
            if (req.equalsIgnoreCase("ask") && inferenceEngine.equals("LeoLocal")) {
                resultLeo = kb.askLEO(stmt,timeout,maxAnswers,"LeoLocal");
            }
            if (req.equalsIgnoreCase("ask") && inferenceEngine.equals("LeoGlobal")) {
                resultLeo = kb.askLEO(stmt,timeout,maxAnswers,"LeoGlobal");
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
              <label>tff mode</label> ]<BR>
    Choose an inference engine:<BR>

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
          <label>Avatar mode</label> ]<BR>

    <INPUT type="submit" name="request" value="Ask">

<% if (role != null && role.equalsIgnoreCase("admin")) { %>
    <INPUT type="submit" name="request" value="Tell"><BR>
<% } %>
</FORM>
<table ALIGN='LEFT' WIDTH='80%'><tr><TD BGCOLOR='#AAAAAA'>
<IMG SRC='pixmaps/1pixel.gif' width=1 height=1 border=0></TD></tr></table><BR>

<%
    if (status != null && status.toString().length() > 0) {
        out.println("Status: ");
        out.println(status.toString());
    }
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
        if (vampire == null || vampire.output == null)
            out.println("<font color='red'>Error.  No response from Vampire.</font>");
        else if ((vampire.output != null) && (vampire.output.indexOf("Syntax error detected") != -1))
            out.println("<font color='red'>A syntax error was detected in your input.</font>");
        else if (vampire.output != null) {
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
    if (inferenceEngine.equals("LeoSine") || inferenceEngine.equals("LeoLocal") || inferenceEngine.equals("LeoGlobal")) {
        if ((resultLeo != null) && (resultLeo.indexOf("Syntax error detected") != -1)) 
            out.println("<font color='red'>A syntax error was detected in your input.</font>");
        else 
	    out.println("<font color='blue'>" + resultLeo + "</font>");
    }
%>
    <p>

<%@ include file="Postlude.jsp" %>

</BODY>
</HTML>
 
