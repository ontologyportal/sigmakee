<%@ include file="Prelude.jsp" %>

<%
/** This code is copyright Teknowledge (c) 2003, Articulate Software (c) 2003-2017,
    Infosys (c) 2017-present.

    This software is released under the GNU Public License
    <http://www.gnu.org/copyleft/gpl.html>.

    Please cite the following article in any publication with references:

    Pease A., and Benzmüller C. (2013). Sigma: An Integrated Development Environment
    for Logical Theories. AI Communications 26, pp79-97.  See also
    https://github.com/ontologyportal
*/

if (!role.equalsIgnoreCase("admin")) {
    response.sendRedirect("login.html");
    return;
}
%>

<HTML>
<HEAD>
  <TITLE>Sigma Knowledge Engineering Environment - Inference Test Suite</TITLE>
</HEAD>
<%
    String req = request.getParameter("request");
    String inferenceEngine = request.getParameter("inferenceEngine");
    if (inferenceEngine == null) {
        inferenceEngine = "Vampire";
    }
    int maxAnswers = 1;
    int timeout = 30;
    String overrideTimeout = request.getParameter("overrideTimeout");
    if (overrideTimeout != null)
        InferenceTestSuite.overrideTimeout = true;

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

    String eproverExec = KBmanager.getMgr().getPref("eprover");
    String tptpFile = KBmanager.getMgr().getPref("kbDir") + File.separator + "SUMO.tptp";
    File ep = new File(eproverExec);
    if (kb.eprover == null && ep.exists())
        kb.eprover = new com.articulate.sigma.tp.EProver(eproverExec,tptpFile);

    if (request.getParameter("maxAnswers") != null) 
        maxAnswers = Integer.parseInt(request.getParameter("maxAnswers"));
    if (request.getParameter("timeout") != null)
        timeout= Integer.parseInt(request.getParameter("timeout"));
    if ((kbName == null) || kbName.equals("")) {
        System.out.println("Error: No knowledge base specified");
        return;
    }

  String resultEProver = null;
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
    
    Query time limit:<input TYPE="TEXT" NAME="timeout" VALUE="<%=timeout%>">
    <input type="checkbox" id="overrideTimeout" name="overrideTimeout" value="yes">
    <label for="overrideTimeout">Override individual timeouts</label><br>

    <IMG SRC='pixmaps/1pixel.gif' width=1 height=1 border=0><BR>
    Choose an inference engine for testing:<BR>
    <INPUT TYPE=RADIO NAME="inferenceEngine" VALUE="EProver" <% if (inferenceEngine.equals("EProver")) {%>CHECKED<%}%>
    <% if ( kb.eprover == null ) { %> DISABLED <% } %> >
    EProver <BR>
    <INPUT TYPE=RADIO NAME="inferenceEngine" VALUE="Vampire" <% if (inferenceEngine.equals("Vampire")) {%>CHECKED<%}%> >
    Vampire <BR>
    [
          <input type="radio" id="TPTPlang" name="TPTPlang" value="tptp"
              <% if (SUMOformulaToTPTPformula.lang.equals("fof")) { out.print(" CHECKED"); } %> >
              <label>tptp mode</label>
          <input type="radio" id="TPTPlang" name="TPTPlang" value="tff"
              <% if (SUMOformulaToTPTPformula.lang.equals("tff")){ out.print(" CHECKED"); } %> >
              <label>tff mode</label> ]
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
          if (inferenceEngine.equalsIgnoreCase("EProver")) {
              out.println("(Testing EProver)<br>");
              InferenceTestSuite its = new InferenceTestSuite();
              sb = sb.append(its.test(kb, "EProver", timeout));
          }
        if (inferenceEngine.equalsIgnoreCase("Vampire")) {
            out.println("(Testing Vampire)<br>");
            InferenceTestSuite its = new InferenceTestSuite();
            sb = sb.append(its.test(kb, "Vampire", timeout));
        }
      }
  }
  out.println(sb.toString());
%>
<p>
<%@ include file="Postlude.jsp" %>
