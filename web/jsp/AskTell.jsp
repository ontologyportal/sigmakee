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

//----Check if SystemOnTPTP exists in a local copy of TPTPWorld
  String BuiltInDir = KBmanager.getMgr().getPref("systemsDir");
  String TPTPWorld = KBmanager.getMgr().getPref("tptpHomeDir");
  InterfaceTPTP.init();
  ArrayList<String> systemListBuiltIn = InterfaceTPTP.systemListBuiltIn;
  ArrayList<String> systemListLocal = InterfaceTPTP.systemListLocal;
  ArrayList<String> systemListRemote = InterfaceTPTP.systemListRemote;
  String defaultSystemBuiltIn = InterfaceTPTP.defaultSystemBuiltIn;
  String defaultSystemLocal = InterfaceTPTP.defaultSystemLocal;
  String defaultSystemRemote = InterfaceTPTP.defaultSystemRemote;
  boolean tptpWorldExists = InterfaceTPTP.tptpWorldExists;
  boolean builtInExists = InterfaceTPTP.builtInExists;

//----Determine Location
  String location = request.getParameter("systemOnTPTP");  
  if (location == null) {
      if (tptpWorldExists)
          location = "local";
      else if (builtInExists)
          location = "local";
      else 
          location = "remote";      
  }
%>

<HTML>
<HEAD>
  <TITLE>Sigma Knowledge Engineering Environment - Ask/Tell</TITLE>

<%
// SystemOnTPTP: script for SystemList toggling
%>
  <script type="text/javascript">//<![CDATA[
    var tstp_dump;
    function openSoTSTP (dump) {
      var tstp_url = 'http://www.cs.miami.edu/~tptp/cgi-bin/SystemOnTSTP';
      var tstp_browser = window.open(tstp_url, '_blank');
      tstp_dump = dump;
    }
    function getTSTPDump () {
      return tstp_dump;
    }
<% if (tptpWorldExists && location.equals("local")) { %>
       var current_location = "Local";
<% } else if (builtInExists && location.equals("local")) { %>
       var current_location = "BuiltIn";
<% } else { %>
       var current_location = "Remote";
<% } %>
//----Toggle to either the local/builtin/remote list by showing new and hiding current
    function toggleList (location) {
        
        if (current_location == location) 
            return;      
        var obj;
        obj = window.document.getElementById("systemList" + current_location);
        if (obj) 
            obj.setAttribute("style","display:none");      
        current_location = location;
        obj = window.document.getElementById("systemList" + location);
        if (obj) 
            obj.setAttribute("style","display:inline");      
    }
  //]]></script>

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
    System.out.println("INFO in AskTell.jsp: inferenceEngine: " + inferenceEngine);
    System.out.println("INFO in AskTell.jsp: vampireMode: " + vampireMode);
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

//----SystemOnTPTP parameters
    String systemChosenLocal = request.getParameter("systemChosenLocal");
    String systemChosenRemote = request.getParameter("systemChosenRemote");
    String systemChosenBuiltIn = request.getParameter("systemChosenBuiltIn");
    if (systemChosenLocal == null) 
        systemChosenLocal = defaultSystemLocal;
    if (systemChosenRemote == null) 
        systemChosenRemote = defaultSystemRemote;
    if (systemChosenBuiltIn == null) 
        systemChosenBuiltIn = defaultSystemBuiltIn;

    String quietFlag = request.getParameter("quietFlag");
    String tstpFormat = request.getParameter("tstpFormat");
    String systemChosen;

    if (quietFlag == null) 
        quietFlag = "hyperlinkedKIF";
    if (systemChosenLocal == null) 
        systemChosenLocal = defaultSystemLocal;
    if (systemChosenRemote == null) 
        systemChosenRemote = defaultSystemRemote;
    if (systemChosenBuiltIn == null)
        systemChosenBuiltIn = defaultSystemBuiltIn;

    if (location.equals("local")) {
        if (tptpWorldExists) 
            systemChosen = systemChosenLocal;
        else 
            systemChosen = systemChosenBuiltIn;
    } 
    else
        systemChosen = systemChosenRemote;

    if (tstpFormat == null) 
        tstpFormat = "";

    com.articulate.sigma.tp.EProver eProver = null;
    com.articulate.sigma.tp.Vampire vampire = null;
    String resultSoTPTP = null;           
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
            if (req.equalsIgnoreCase("ask") && inferenceEngine.equals("SoTPTP")) {
                systemChosen = systemChosen.replace("%2E", ".");
                resultSoTPTP = InterfaceTPTP.queryTPTP(stmt, timeout, maxAnswers, lineHtml,
                                                        systemChosen, location, quietFlag, 
                                                        kbName, language);
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

<!--
    <INPUT TYPE=RADIO NAME="inferenceEngine" VALUE="LeoSine" <% if (inferenceEngine.equals("LeoSine")) {%>CHECKED<%}%>
    onclick="document.getElementById('SoTPTPControl').style.display='none'">
    LEO-II with SInE (experimental)<BR>
    <INPUT TYPE=RADIO NAME="inferenceEngine" VALUE="LeoLocal" <% if (inferenceEngine.equals("LeoLocal")) {%>CHECKED<%}%>
    onclick="document.getElementById('SoTPTPControl').style.display='none'">
    LEO-II local (experimental)<BR>
    <INPUT TYPE=RADIO NAME="inferenceEngine" VALUE="LeoGlobal" <% if (inferenceEngine.equals("LeoGlobal")) {%>CHECKED<%}%>
    onclick="document.getElementById('SoTPTPControl').style.display='none'">
    LEO-II global (experimental)<BR>	
    <INPUT TYPE=RADIO NAME="inferenceEngine" VALUE="SoTPTP" <% if (inferenceEngine.equals("SoTPTP")) {%>CHECKED<%}%>
    onclick="document.getElementById('SoTPTPControl').style.display='inline'">
    System on TPTP<BR>
    -->

<%
//----System selection
%>
<!--
  <DIV ID="SoTPTPControl" <% if (!inferenceEngine.equals("SoTPTP")) {%>style="display:none;"<%}%>>
    <IMG SRC='pixmaps/1pixel.gif' width=30 height=1 border=0>
    <INPUT TYPE=RADIO NAME="systemOnTPTP" VALUE="local"
<% if (!tptpWorldExists && !builtInExists) { out.print(" DISABLED"); } %>
<% if (location.equals("local")) { out.print(" CHECKED"); } %>
<% if (tptpWorldExists) 
       out.println("onClick=\"javascript:toggleList('Local');\"");
   else 
       out.println("onClick=\"javascript:toggleList('BuiltIn');\"");     
   
%>
  >Local 
    <INPUT TYPE=RADIO NAME="systemOnTPTP" VALUE="remote"
<% if (location.equals("remote")) { out.print(" CHECKED"); } %>
  onClick="javascript:toggleList('Remote');">Remote&nbsp;System:
<%
  String params;
  //----Create atp drop down list for local
  if (tptpWorldExists) {
      if (location.equals("local"))
          params = "ID=systemListLocal style='display:inline'";
      else 
          params = "ID=systemListLocal style='display:none'";     
      out.println(HTMLformatter.createMenu("systemChosenLocal",systemChosenLocal,
                                         systemListLocal, params)); 
  }
  //----Create atp drop down list for builtin
  if (builtInExists && !tptpWorldExists) {
      if (location.equals("local"))
          params = "ID=systemListBuiltIn style='display:inline'";
      else
          params = "ID=systemListBuiltIn style='display:none'";     
      out.println(HTMLformatter.createMenu("systemChosenBuiltIn", systemChosenBuiltIn,
                                         systemListBuiltIn, params));
  }
  //----Create atp drop down list for remote
  if ((!tptpWorldExists && !builtInExists) || location.equals("remote")) 
      params = "ID=systemListRemote style='display:inline'";
  else
      params = "ID=systemListRemote style='display:none'";
  out.println(HTMLformatter.createMenu("systemChosenRemote",systemChosenRemote,
                                       systemListRemote, params));
%>
    <br>
    <IMG SRC='pixmaps/1pixel.gif' width=30 height=1 border=0>
    <INPUT TYPE="hidden" NAME="tstpFormat" VALUE="-S">
    <INPUT TYPE=RADIO NAME="quietFlag" VALUE="-q4"
<% if (quietFlag.equals("-q4")) { out.print(" CHECKED"); } %>
    >TPTP Proof
    <INPUT TYPE=RADIO NAME="quietFlag" VALUE="IDV"
<% if (quietFlag.equals("IDV")) { out.print(" CHECKED"); } %>
    >IDV-Proof tree
    <INPUT TYPE=RADIO NAME="quietFlag" ID="hyperlinkedKIF" VALUE="hyperlinkedKIF"
<% if (quietFlag.equals("hyperlinkedKIF")) { out.print(" CHECKED"); } %>
    >Hyperlinked KIF
    <br>
  </DIV>
-->

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
            tpp.processAnswersFromProof(vampire.qlist,stmt);
            System.out.println("in AskTell.jsp: sending the HTML formatter--------------");
            out.println(HTMLformatter.formatTPTP3ProofResult(tpp,stmt,lineHtml,kbName,language));
        }
    }
    if ((inferenceEngine.equals("SoTPTP")) && (resultSoTPTP != null))
        out.print(resultSoTPTP);
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
 
