<%@include file="Prelude.jsp" %>
<HEAD>
  <TITLE>Sigma Knowledge Engineering Environment - Ask/Tell</TITLE>
  <!-- <style>@import url(kifb.css);</style> -->
</HEAD>
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

if (!role.equals("admin")) {
    response.sendRedirect("login.html");
    return;
}

System.out.println("INFO in CELT.jsp");
String result = null;
StringBuffer sbStatus = new StringBuffer();

String req = request.getParameter("request");
String stmt = request.getParameter("stmt");
String href = "Browse.jsp?kb=" + kbName + "&lang=" + language + "&flang=" + flang + "&term=";
boolean english = false;

int maxAnswers = 1;
int timeout = 30;
if (request.getParameter("maxAnswers") != null)
    maxAnswers = Integer.parseInt(request.getParameter("maxAnswers"));
if (request.getParameter("timeout") != null)
    timeout= Integer.parseInt(request.getParameter("timeout"));

if (kbName == null) {
    System.out.println("Error: No knowledge base specified");
    return;
}

if (stmt != null)
    System.out.println(stmt.trim());

KB kb = KBmanager.getMgr().getKB(kbName);
if (stmt == null)   // check if there is an attribute for stmt
    stmt="(instance ?X Relation)";
else {
    if (stmt.trim().charAt(0) != '(')
        english = true;
    else {
        String msg = (new KIF()).parseStatement(stmt);
        if (msg != null) {
            sbStatus.append("<font color='red'>Error: Syntax Error in statement: " + stmt);
            sbStatus.append("Message: " + msg + "</font><br>\n");
        }
    }
}

if (english)
    stmt = kb.celt.submit(stmt);
System.out.println("INFO in CELT.jsp: Completed translation.");
System.out.println(stmt);

if (req != null) {
    try {
        if (req.equalsIgnoreCase("ask")) {
    result = kb.ask( stmt, timeout, maxAnswers );
        }
        if (req.equalsIgnoreCase("tell")) {
            Formula statement = new Formula();
            statement.theFormula = stmt;
            String kbHref = "http://" + hostname + ":8080/sigma/Browse.jsp?kb=" + kbName;
            sbStatus.append(kb.tell(stmt) + "<P>\n" + statement.htmlFormat(kbHref));
        }
    }
    catch (IOException ioe) {
        sbStatus.append(ioe.getMessage());
    }
}
%>

<BODY style="face=Arial,Helvetica" BGCOLOR=#FFFFFF>
<FORM name="CELT" ID="CELT" action="CELT.jsp" METHOD="POST">
    <%
        String pageName = "CELT";
        String pageString = "CELT";
    %>
    <%@include file="CommonHeader.jsp" %>
    
    <IMG SRC='pixmaps/1pixel.gif' width=1 height=1 border=0><BR>
    <textarea rows="5" cols="70" name="stmt"><%=stmt%></textarea><br>
    Maximum answers: <input TYPE="TEXT" NAME="maxAnswers" VALUE="<%=maxAnswers%>">
    Query time limit:<input TYPE="TEXT" NAME="timeout" VALUE="<%=timeout%>"><BR>
    <INPUT type="submit" name="request" value="ask">
    <INPUT type="submit" name="request" value="tell"><BR>
</FORM>
<table ALIGN='LEFT' WIDTH=80%%><tr><TD BGCOLOR='#AAAAAA'>
<IMG SRC='pixmaps/1pixel.gif' width=1 height=1 border=0></TD></tr></table><BR>

<%
String lineHtml = "<table ALIGN='LEFT' WIDTH=40%%><tr><TD BGCOLOR='#AAAAAA'><IMG SRC='pixmaps/1pixel.gif' width=1 height=1 border=0></TD></tr></table><BR>\n";
if (sbStatus != null && sbStatus.toString().length() > 0) {
    out.println("Status: ");
    out.println(sbStatus.toString());
}
if (result != null && result.toString().length() > 0) {
    BasicXMLparser res = new BasicXMLparser(result.toString());
    System.out.print("INFO in CELT.jsp: Number of XML elements: ");
    System.out.println(res.elements.size());
    ProofProcessor pp = new ProofProcessor(res.elements);
    for (int i = 0; i < pp.numAnswers()-1; i++) {
        ArrayList proofSteps = pp.getProofSteps(i);
        proofSteps = new ArrayList(ProofStep.normalizeProofStepNumbers(proofSteps));
        System.out.print("Proof steps: ");
        System.out.println(proofSteps.size());
        if (i != 0)
            out.println(lineHtml);
        out.println("Answer ");
        out.println(i+1);
        out.println(". " + pp.returnAnswer(i));
        if (!pp.returnAnswer(i).equalsIgnoreCase("no")) {
            out.println("<P><TABLE width=95%%>");
            for (int j = 0; j < proofSteps.size(); j++) {
                System.out.print("Printing proof step: ");
                System.out.println(j);
                out.println("<TR>");
                out.println("<TD valign=top>");
                out.print(j+1);
                out.println(". </TD>");
                out.println(HTMLformatter.proofTableFormat(stmt,(ProofStep) proofSteps.get(j), kbName, language));
                System.out.println(HTMLformatter.proofTableFormat(stmt,(ProofStep) proofSteps.get(j), kbName, language));
                out.println("</TR>\n");
            }
            out.println("</TABLE>");
        }
    }
}
%>
<p>
<%@ include file="Postlude.jsp" %>
</BODY>
</HTML>
