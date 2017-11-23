<%@include file="Prelude.jsp" %>

<%
/** This code is copyright Articulate Software (c) 2003-2011.  Some portions
copyright Teknowledge (c) 2003 and reused under the terms of the GNU license.
This software is released under the GNU Public License <http://www.gnu.org/copyleft/gpl.html>.
Users of this code also consent, by use of this code, to credit Articulate Software
and Teknowledge in any writings, briefings, publications, presentations, or 
other representations of any software which incorporates, builds on, or uses this 
code.  Please cite the following article in any publication with references:

Pease, A., (2003). The Sigma Ontology Development Environment, 
in Working Notes of the IJCAI-2003 Workshop on Ontology and Distributed Systems,
August 9, Acapulco, Mexico.  See also http://sigmakee.sourceforge.net
*/

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
    String chosenEngine = request.getParameter("inferenceEngine");
    boolean syntaxError = false;
    boolean english = false;
    String englishStatement = null;
    int maxAnswers = 1;
    int timeout = 30;

    if (chosenEngine == null) {
        if (kb.eprover == null) 
            chosenEngine = "SoTPTP";
        else
            chosenEngine = "EProver";
    }
    System.out.println("INFO in AskTell.jsp: Engine: " + chosenEngine);
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

    String resultEProver = null;
    String resultSoTPTP = null;           
    String resultLeo = null;    

    String lineHtml =
      "<table ALIGN='LEFT' WIDTH='40%'><tr><TD BGCOLOR='#AAAAAA'><IMG SRC='pixmaps/1pixel.gif' width=1 height=1 border=0></TD></tr></table><BR>\n";

    if (req != null && !syntaxError) {
        try {
            if (req.equalsIgnoreCase("tell")) {
                Formula statement = new Formula();
                statement.theFormula = stmt;
                System.out.println("INFO in AskTell.jsp: statement: " + stmt);
                String kbHref = "http://" + hostname + ":" + port + "/sigma/Browse.jsp?kb=" + kbName;
                status.append(kb.tell(stmt) + "<P>\n" + statement.htmlFormat(kbHref));
            }
            if (req.equalsIgnoreCase("ask") && chosenEngine.equals("EProver")) {
                if (stmt.indexOf('@') != -1)
                    throw(new IOException("Row variables not allowed in query: " + stmt));
		        resultEProver = kb.askEProver(stmt, timeout, maxAnswers);
		        System.out.println("INFO in AskTell.jsp------------------------------------");
		        System.out.println(resultEProver);
            }
            if (req.equalsIgnoreCase("ask") && chosenEngine.equals("LeoSine")) {
                if (stmt.indexOf('@') != -1)
                    throw(new IOException("Row variables not allowed in query: " + stmt));
                resultLeo = kb.askLEO(stmt,timeout,maxAnswers,"LeoSine");
            }	
            if (req.equalsIgnoreCase("ask") && chosenEngine.equals("LeoLocal")) {
                if (stmt.indexOf('@') != -1)
                    throw(new IOException("Row variables not allowed in query: " + stmt));
                resultLeo = kb.askLEO(stmt,timeout,maxAnswers,"LeoLocal");
            }
            if (req.equalsIgnoreCase("ask") && chosenEngine.equals("LeoGlobal")) {
                if (stmt.indexOf('@') != -1)
                    throw(new IOException("Row variables not allowed in query: " + stmt));
                resultLeo = kb.askLEO(stmt,timeout,maxAnswers,"LeoGlobal");
            }	
            if (req.equalsIgnoreCase("ask") && chosenEngine.equals("SoTPTP")) {
                if (stmt.indexOf('@') != -1)
                    throw(new IOException("Row variables not allowed in query: " + stmt));
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
    <TABLE width="95%" cellspacing="0" cellpadding="0">
      <TR>
          <TD align="left" valign="top"><img src="pixmaps/sigmaSymbol-gray.gif"></TD>
          <TD align="left" valign="top"><img src="pixmaps/logoText-gray.gif"><br><B>Inference Interface</B></TD>
          <TD valign="bottom"></TD>
          <TD>
            <font FACE="Arial, Helvetica" SIZE=-1><b>[ <A href="KBs.jsp">Home</A></b>&nbsp;|&nbsp;
            <A href="Graph.jsp?kb=<%=kbName %>&lang=<%=language %>"><B>Graph</B></A>&nbsp;|&nbsp;                                                      
            <b><A href="Properties.jsp">Prefs</A></b>&nbsp;]&nbsp;
            <b>KB:&nbsp;
<%
            ArrayList kbnames = new ArrayList();
            kbnames.addAll(KBmanager.getMgr().getKBnames());
            out.println(HTMLformatter.createMenu("kb",kbName,kbnames)); 
%>              
            </b>
            <b>Language:&nbsp;<%= HTMLformatter.createMenu("lang",language,kb.availableLanguages()) %></b>
          <BR>
          </TD>
      </TR>
    </TABLE><br>
    
    <IMG SRC='pixmaps/1pixel.gif' width=1 height=1 border=0><BR>
    <textarea rows="5" cols="70" name="stmt"><%=stmt%></textarea>
    <br>
    Maximum answers: <input TYPE="TEXT" NAME="maxAnswers" VALUE="<%=maxAnswers%>">
    Query time limit:<input TYPE="TEXT" NAME="timeout" VALUE="<%=timeout%>"><BR>
    Choose an inference engine:<BR>
    <INPUT TYPE=RADIO NAME="inferenceEngine" VALUE="EProver" <% if (chosenEngine.equals("EProver")) {%>CHECKED<%}%>
    onclick="document.getElementById('SoTPTPControl').style.display='none'"
    <% if (kb.eprover == null) { %> DISABLED <% } %> >
    EProver <BR>
    <INPUT TYPE=RADIO NAME="inferenceEngine" VALUE="LeoSine" <% if (chosenEngine.equals("LeoSine")) {%>CHECKED<%}%>
    onclick="document.getElementById('SoTPTPControl').style.display='none'">
    LEO-II with SInE (experimental)<BR>
    <INPUT TYPE=RADIO NAME="inferenceEngine" VALUE="LeoLocal" <% if (chosenEngine.equals("LeoLocal")) {%>CHECKED<%}%>
    onclick="document.getElementById('SoTPTPControl').style.display='none'">
    LEO-II local (experimental)<BR>
    <INPUT TYPE=RADIO NAME="inferenceEngine" VALUE="LeoGlobal" <% if (chosenEngine.equals("LeoGlobal")) {%>CHECKED<%}%>
    onclick="document.getElementById('SoTPTPControl').style.display='none'">
    LEO-II global (experimental)<BR>	
    <INPUT TYPE=RADIO NAME="inferenceEngine" VALUE="SoTPTP" <% if (chosenEngine.equals("SoTPTP")) {%>CHECKED<%}%>
    onclick="document.getElementById('SoTPTPControl').style.display='inline'">
    System on TPTP<BR>
<%
//----System selection
%>
  <DIV ID="SoTPTPControl" <% if (!chosenEngine.equals("SoTPTP")) {%>style="display:none;"<%}%>>
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
    if (chosenEngine.equals("EProver")) {
        if ((resultEProver != null) && (resultEProver.indexOf("Syntax error detected") != -1))         
            out.println("<font color='red'>A syntax error was detected in your input.</font>");
        else if (resultEProver != null){
        	System.out.println("in AskTell.jsp: trying EProver--------------");
        	com.articulate.sigma.trans.TPTP3ProofProcessor tpp = com.articulate.sigma.trans.TPTP3ProofProcessor.parseProofOutput(resultEProver, kb);
        	System.out.println("in AskTell.jsp: sending the HTML formatter--------------");
            out.println(HTMLformatter.formatTPTP3ProofResult(tpp,stmt,lineHtml,kbName,language));
        }
    }
    if ((chosenEngine.equals("SoTPTP")) && (resultSoTPTP != null))
        out.print(resultSoTPTP);
    if (chosenEngine.equals("LeoSine") || chosenEngine.equals("LeoLocal") || chosenEngine.equals("LeoGlobal")) {
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
 
