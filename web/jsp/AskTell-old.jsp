
<%@include file="Prelude.jsp" %>
<HEAD>
  <TITLE>Sigma Knowledge Engineering Environment - Ask/Tell</TITLE>
  <!-- <style>@import url(kifb.css);</style> -->
</HEAD>
<%

/** This code is copyright Articulate Software (c) 2003.  Some portions
copyright Teknowledge (c) 2003 and reused under the terms of the GNU license.
This software is released under the GNU Public License <http://www.gnu.org/copyleft/gpl.html>.
Users of this code also consent, by use of this code, to credit Articulate Software
and Teknowledge in any writings, briefings, publications, presentations, or 
other representations of any software which incorporates, builds on, or uses this 
code.  Please cite the following article in any publication with references:

Pease, A., (2003). The Sigma Ontology Development Environment, 
in Working Notes of the IJCAI-2003 Workshop on Ontology and Distributed Systems,
August 9, Acapulco, Mexico.
*/
    System.out.println("INFO in AskTell.jsp");
    String result = null;        
    StringBuffer sbStatus = new StringBuffer();
    String processedStmt = null;

    String hostname = KBmanager.getMgr().getPref("hostname");
    if (hostname == null)
        hostname = "localhost";
    String kbName = request.getParameter("kb");
    String language = request.getParameter("lang");
    String req = request.getParameter("request");
    String stmt = request.getParameter("stmt");
    String href = "Browse.jsp?kb=" + kbName + "&lang=" + language + "&term=";

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

    KB kb = KBmanager.getMgr().getKB(kbName);
    if (stmt == null)   // check if there is an attribute for stmt
        stmt="(instance ?X Relation)";    
    else {
        String msg = (new KIF()).parseStatement(stmt,"");
        if (msg != null) {
            sbStatus.append("<font color='red'>Error: Syntax Error in statement: " + stmt);
            sbStatus.append("Message: " + msg + "</font><br>\n");
        }
    }
            
    if (req != null) {
        try {
            if (req.equalsIgnoreCase("ask")) {
                Formula query = new Formula();
                query.theFormula = stmt;
                processedStmt = query.preProcess();
                result = kb.inferenceEngine.submitQuery(processedStmt,timeout,maxAnswers);
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
<FORM name="AskTell" ID="AskTell" action="AskTell.jsp" METHOD="POST">
    <TABLE width="95%" cellspacing="0" cellpadding="0">
      <TR>
          <TD align="left" valign="top"><img src="pixmaps/sigmaSymbol-gray.gif"></TD>
          <TD align="left" valign="top"><img src="pixmaps/logoText-gray.gif"><br><B>Inference Interface</B></TD>
          <TD valign="bottom"></TD>
          <TD>
            <font FACE="Arial, Helvetica" SIZE=-1><b>[ <A href="KBs.jsp">Home</A></b>&nbsp;|&nbsp;
            <A href=\"Graph.jsp?kb=<%=kbName %>&lang=<%=language %>"><B>Graph</B></A>&nbsp;|&nbsp;                                                      
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
        System.out.print("INFO in AskTell.jsp: Number of XML elements: ");
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

</BODY>
</HTML>