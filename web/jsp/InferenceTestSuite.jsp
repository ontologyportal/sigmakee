<%@ include file="Prelude.jsp" %>

<%
/** This code is copyright Teknowledge (c) 2003, Articulate Software (c) 2003-2017,
    Infosys (c) 2017-present.

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
  <TITLE>Sigma Knowledge Engineering Environment - Inference Test Suite</TITLE>

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
    String req = request.getParameter("request");
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
 
    if (request.getParameter("maxAnswers") != null) 
        maxAnswers = Integer.parseInt(request.getParameter("maxAnswers"));
    if (request.getParameter("timeout") != null)
        timeout= Integer.parseInt(request.getParameter("timeout"));
    if ((kbName == null) || kbName.equals("")) {
        System.out.println("Error: No knowledge base specified");
        return;
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
  
  String tstpFormat = request.getParameter("tstpFormat");
  String systemChosen;

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
  String lineHtml = "<table ALIGN='LEFT' WIDTH='40%'><tr><TD BGCOLOR='#AAAAAA'>" +
                      "<IMG SRC='pixmaps/1pixel.gif' width=1 height=1 border=0></TD></tr></table><BR>\n";
%>
<BODY style="face=Arial,Helvetica" BGCOLOR=#FFFFFF>
<FORM name="AskTell" ID="AskTell" action="InferenceTestSuite.jsp" METHOD="POST">
    <%
        String pageName = "InferenceTestSuite";
        String pageString = "Inference Test Suite";
    %>
    <%@include file="CommonHeader.jsp" %>
    
    Query time limit:<input TYPE="TEXT" NAME="timeout" VALUE="<%=timeout%>"><BR>
    <IMG SRC='pixmaps/1pixel.gif' width=1 height=1 border=0><BR>
    Choose an inference engine for testing:<BR>
    <INPUT TYPE=RADIO NAME="inferenceEngine" VALUE="EProver" <% if (chosenEngine.equals("EProver")) {%>CHECKED<%}%>
    onclick="document.getElementById('SoTPTPControl').style.display='none'"
    <% if ( kb.eprover == null ) { %> DISABLED <% } %> >
    EProver <BR>
    <INPUT TYPE=RADIO NAME="inferenceEngine" VALUE="LEO" <% if (chosenEngine.equals("LEO")) {%>CHECKED<%}%>
    onclick="document.getElementById('SoTPTPControl').style.display='none'">
    LEO-II <BR>
    <INPUT TYPE=RADIO NAME="inferenceEngine" VALUE="SoTPTP" <% if (chosenEngine.equals("SoTPTP")) {%>CHECKED<%}%>
    onclick="document.getElementById('SoTPTPControl').style.display='inline'">
    System on TPTP<BR>
<%
//----System selection
%>
  <DIV ID="SoTPTPControl" <% if (chosenEngine.equals("EProver") || chosenEngine.equals("LEO")) {%>style="display:none;"<%}%>>
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
  onClick="javascript:toggleList('Remote');">Remote
&nbsp;
  System:
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
  </DIV>
    <INPUT type="submit" name="request" value="Test"><br><br>
    <INPUT TYPE="hidden" NAME="test" VALUE="inference">
</FORM>
<table ALIGN='LEFT' WIDTH='80%'><tr><TD BGCOLOR='#AAAAAA'>
<IMG SRC='pixmaps/1pixel.gif' width=1 height=1 border=0></TD></tr></table><BR>

<%
  StringBuffer sb = new StringBuffer();
  String inferenceTestDir = KBmanager.getMgr().getPref("inferenceTestDir");
  if (inferenceTestDir == null)
      sb = sb.append("Error: No inference test directory specified.  Set in <A href=\"Preferences.jsp\">Preferences</A>");
  else {
      String test = request.getParameter("test");
      if (test != null && test.equalsIgnoreCase("inference") &&
          req != null && req.equalsIgnoreCase("test")) {
          if (chosenEngine.equalsIgnoreCase("EProver")) {
              out.println("(Testing EProver)<br>");
              sb = sb.append(InferenceTestSuite.test(kb, "EProver", timeout));
          }               
          //if (chosenEngine.equalsIgnoreCase("LEO")) {
          //    out.println("(Testing LEO)<br>");
          //    THF thf = new THF();
          //    sb = sb.append(thf.testLEO(kb).replaceAll("\\n","<br>"));
          //}          
          if (chosenEngine.equalsIgnoreCase("SoTPTP")) {
              if (location.equalsIgnoreCase("local")&&(!tptpWorldExists)&&builtInExists) {
                  out.println("(Testing built-in SystemOnTPTP)<br>");
                  location="builtin";
              }
              if (location.equalsIgnoreCase("local")&&tptpWorldExists) {
                  out.println("(Testing local SystemOnTPTP)<br>");
                  location="local";
              }
              if (location.equalsIgnoreCase("remote")) {
                  out.println("(Testing remote SystemOnTPTP)<br>");
                  location="remote";
              }
              sb = sb.append(InferenceTestSuite.test(kb, systemChosen, timeout, location));
          }          
      }
      if (test != null && test.equalsIgnoreCase("english")) 
          sb = sb.append(CELTTestSuite.test(kb));
  }
  out.println(sb.toString());
%>
<p>
<%@ include file="Postlude.jsp" %>
