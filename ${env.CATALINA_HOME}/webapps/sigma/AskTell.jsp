
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
    
    ServletUtil.dumpServletReq(request);  // debugging
    
    // ask_tell uses multiple level request arrangement.  "req" determine the type of requests
    // while "a_t_req" determines detail of ask_tell.
    
    String req = request.getParameter("req");
    String a_t_req = request.getParameter("a_t_req");
    String skbName = request.getParameter("skb");
    String href ="skb.jsp?req=skb_sr&skb=" + skbName + "&term=";
    
    boolean isConsistCheck = false;
    String ccTmp = request.getParameter("consist_check");
    System.out.println("consist_check = " + ccTmp);
    if (ccTmp != null && ccTmp.equalsIgnoreCase("on"))
        isConsistCheck = true;
    
    StringBuffer sbStatus = new StringBuffer();   // for error messages and user feedback
    boolean isError = false;          // show stopper found
    
    if (skbName == null) {
        System.out.println("Error: No knowledge base specified");
        sbStatus.append("Error: No knowledge base specified: " + skbName);
        isError = true;
        return;
    }
    SigmaKB theSKB = SKBMgr.getDefaultSKBMgr().getSKB(skbName,false);  // the SKB for processing
    
    System.out.println("here for " + skbName + " and req is " + req + " and a_t_req " + a_t_req);
    
    // setup the default editing window
    String stmt    = request.getParameter("stmt");
    String stmtToShow = stmt;
    SFormula sform   = null;
    if (stmt == null) {  // check if there is an attribute for stmt
        Object stmtObj = session.getAttribute("stmt");
        if (stmtObj == null) // still null
            stmt="(instance ?X Relation)";
        else {
            stmt = stmtObj.toString();
            session.removeAttribute("stmt");  // session attribute one time only.
        }
        stmtToShow = stmt;
    }
    else {
        try {
            sform = SFormula.parseRaw(stmt);
        }
        catch (KifSyntaxException kse) {
            sbStatus.append("<font color='red'>Error: Syntax Error in statement: " + stmt);
            sbStatus.append("Message: " + kse.getMessage()+"</font><br>\n");
            isError = true;
        }
    }
    
    // get parameters for inference handle
    String user = request.getParameter("user");
    String context = "TopLevelContext"; // Default to this context for now.
    if (user == null)
        user = "default_user";
    
    StringWriter result = new StringWriter();
    InferEngHandler ieh = null;
    try {
        System.out.println("INFO ask_tell.jsp: Getting inference engine handler.");
        ieh = InferEngShepherd.getDefaultInferEngHandler(skbName);
        System.out.println("INFO ask_tell.jsp: Should have gotten inference engine handler.");
        if (ieh == null) System.out.println("But handler is null.");
    }
    catch (ConnectionException ce) {
        ieh = null;
        sbStatus.append("<FONT COLOR='red'>Error: Connection exception. </FONT>");
    }
    if (ieh == null) {
        sbStatus.append("<FONT COLOR='red'>Error: No Inference Server Available. </FONT>");
        isError = true;
    }
    
    if (!isError && a_t_req!= null && a_t_req.equalsIgnoreCase("ask")) {
        System.out.println(" a&t::query()");
        int backchains_max = Integer.parseInt(request.getParameter("opt_backchains_max"));
        int answers_max = Integer.parseInt(request.getParameter("opt_answers_max"));
        int timeout= Integer.parseInt(request.getParameter("opt_timeout"));
        ArrayList ans = InferEngAgent_Vam.query(sform,context,skbName,timeout,answers_max, backchains_max,ieh);
        result = RsUtil.formatQueryResult(theSKB,ans,href);
        a_t_req = null;  // finish
    }
    
    boolean isConflict = false;
    boolean isRedundant = false;
    boolean isContradictory = false;
    
    boolean isAssertionAffirmed = false;  // assert only when this is true
    boolean isAffirmationNeeded = false;  // require affirmation
    ArrayList termsToBeAffirmed = null;   // the list of terms not found in the knowledge base
    
    // assertion affirmation
    if (!isError && a_t_req!=null && (a_t_req.equalsIgnoreCase("assert"))) {
        System.out.println("This is an assertion request.");
        // check grammer
        if (!SigmaUtil.checkAssertionGrammer(theSKB, stmt, sbStatus))
            isError = true;
      
        // Check existence of all terms before performing consistency checking
        Collection col = (Collection)Util.getAllTerms(stmt);
        col = FOLUtil.removeLiteralTerm(col);
        col = FOLUtil.removeLogicPredicates(col);
        col = FOLUtil.removeNum(col);
        col = theSKB.filterTermsNotFound(col); // return
        ArrayList terms = (ArrayList) col;
        if (terms.size() == 0)  // All terms found  
            isAssertionAffirmed = true;
        else {
            isAffirmationNeeded = true;
            termsToBeAffirmed = terms;
        }
    }
    
    if (!isError && a_t_req!=null && (a_t_req.equalsIgnoreCase("assert_affirmed")))
        isAssertionAffirmed = true;
    
    // do that actual assertion
    if (!isError && isAssertionAffirmed) { // Consistent checking or not
        if (isConsistCheck) {       
            ArrayList proofList = new ArrayList();
            int backchains_max = Integer.parseInt(request.getParameter("ct_opt_backchains_max"));
            int answers_max = Integer.parseInt(request.getParameter("ct_opt_answers_max"));
            int timeout= Integer.parseInt(request.getParameter("ct_opt_timeout"));
            String assertResult = InferEngAgent_Vam.assertWithChecks(sform.toString(),skbName,context,user,timeout,answers_max,backchains_max,proofList,ieh);
            result = RsUtil.formatQueryResult(theSKB, proofList, href);
            System.out.println("result " + assertResult);
            System.out.println("proof size " + proofList.size());
        
            if (assertResult.equalsIgnoreCase(InferEngAgent_Vam.NOT_ASSERTED))
                sbStatus.append("<font color=red> Assertion failed.</font>");
            else if(assertResult.equalsIgnoreCase(InferEngAgent_Vam.ASSERTED)) {
                sbStatus.append("<font color=green>Assertion is ok.</font>");
                sbStatus.append(SigmaUtil.hyperGen1(null,stmtToShow,href));
                sbStatus.append("<br>");
                theSKB.f_assertSigmaStmt(stmtToShow);
            }
            else if(assertResult.equalsIgnoreCase(InferEngAgent_Vam.REDUNDANT)) {
                isConflict = true;
                isRedundant = true;
            }
            else if(assertResult.equalsIgnoreCase(InferEngAgent_Vam.CONTRADICTORY)) {
                isConflict = true;
                isContradictory = true;
            }
        }
        else { // for checking no conflict
            System.out.println("Checking is passed and now do the actual assertion.");
            if (!InferEngAgent_Vam.assert_(sform,skbName,context,user,ieh))
                sbStatus.append("<font color=red> Assertion failed.</font>");
            else {
                sbStatus.append("<font color=green>Assertion is ok.</font>");
                sbStatus.append("<br>");
                sbStatus.append(SigmaUtil.genStmt(theSKB,stmtToShow,href));
                theSKB.f_assertSigmaStmt(stmtToShow);
            }
        }
        a_t_req = null;
    }
    
    // retraction
    if (!isError && a_t_req != null && a_t_req.equalsIgnoreCase("retract")) {
        if (!InferEngAgent_Vam.retract(stmt, skbName, context,ieh))
            sbStatus.append("<font color=red> Retraction failed. </font>");
        else
            sbStatus.append("<font color=green> Retraction is successful. </font>");
        a_t_req = null;
    }
    
    // keep it
    if (!isError && a_t_req!=null && (a_t_req.equalsIgnoreCase("keep"))) {
        InferEngAgent_Vam.assert_(sform,skbName,context,user,ieh);
        sbStatus.append("<font color=green> keep chosen and the assertion is ok.</font><br>");
        theSKB.f_assertSigmaStmt(stmtToShow);
        a_t_req = null;
    }
    
    // reject it
    if (!isError && a_t_req!=null && (a_t_req.equalsIgnoreCase("revert"))) {
        sbStatus.append("<font color=green> Revertion is successful.</font><br>");
        a_t_req = null;
    }

%>

<BODY style="face=Arial,Helvetica" NoTonClick="theForm_onclick"  BGCOLOR=#FFFFFF>
<FORM name="ask_tell" ID="ask_tell"  METHOD="POST">
    <TABLE width="95%" cellspacing="0" cellpadding="0">
      <TR>
          <TD align="left" valign="top"><img src="pixmaps/sigmaSymbol-gray.gif"></TD>
          <TD align="left" valign="top"><img src="pixmaps/logoText-gray.gif"><br> <B>Inference Interface</B></TD>
          <TD valign="bottom"></TD>
          <TD>
            <font FACE="Arial, Helvetica" SIZE=-1><b>[ <A href="home.jsp">Help</A></b>&nbsp;|&nbsp;
            <b><A href="prefs.jsp?back=true">Prefs</A></b>&nbsp;|&nbsp;
            <b><A href="login.jsp?log_out=true">Logout</A> ]</b></font><BR>
          KB: <%= Util.genSelector( SKBMgr.getDefaultSKBMgr().getSKBKeys(),skbName,"skb") %><BR>
          <!-- CTT: <%= Util.genSelector(theSKB.listAllCttNames(),null,"ctt") %><BR>
          Context: <%=context%> -->
          </TD>
      </TR>
    </TABLE>

    <br><table ALIGN="LEFT" WIDTH=80%><tr><TD BGCOLOR='#AAAAAA'><IMG SRC='pixmaps/1pixel.gif' width=1 height=1 border=0></TD></tr></table><BR>
      <!-- Show Editing Window -->
      <textarea rows="5" cols="70" name="stmt"><%=stmtToShow%></textarea>
      <br>

      <!-- Show AT button -->
      <TABLE CELLPADDING="10">
        <TR>
          <TD><INPUT type="submit" name="a_t_req" value="ask"> </INPUT></TD>
          <%=Util.writeOption("Backchain Depth","opt_backchains_max","20", request.getParameter("opt_backchains_max"))%>
          <%=Util.writeOption("Maximum Answers","opt_answers_max","10",request.getParameter("opt_answers_max"))%>
          <%=Util.writeOption("Minimum Answers","opt_answers_min","1",request.getParameter("opt_answers_min"))%>
          <%=Util.writeOption("Query Time","opt_timeout","10",request.getParameter("opt_timeout"))%>
        </TR>
        <TR>
          <TD><INPUT type="submit" name="a_t_req" value="assert"> </INPUT></TD>
          <TD>
            <label for="consist_check">
              <INPUT type="checkbox" onClick="this.form.submit()" name="consist_check" <%= isConsistCheck? "checked" : "" %> >
              <%= "Check for consistency"  %>
            </label>
          </TD>
<%
    String assertedCttName = PrefUtil.getSysProp(SigmaUtil.SYS_PROP_PATH, SigmaUtil.PROP_ASSERTED_CTT_NAME,SigmaUtil.DEFAULT_ASSERTED_CTT_NAME);
    SigmaCtt asserted = theSKB.getSigmaCtt(assertedCttName);
    if (asserted != null)
        out.println("<TD></TD><TD> <em> <A href = 'edit_asserted.jsp?skb="+ skbName +"'> Total " + asserted.getTotal() + " asserted </A><em></TD>");
%>
        </TR>
          <%if (isConsistCheck) { %>
          <TR>
          <TD> </TD>
          <%=Util.writeOption("Backchain Depth","ct_opt_backchains_max","20", request.getParameter("ct_opt_backchains_max"))%>
          <%=Util.writeOption("Maximum Answers","ct_opt_answers_max","10",request.getParameter("ct_opt_answers_max"))%>
          <%=Util.writeOption("Minimum Answers","ct_opt_answers_min","1",request.getParameter("ct_opt_answers_min"))%>
          <%=Util.writeOption("Query Time","ct_opt_timeout","10",request.getParameter("ct_opt_timeout"))%>
          </TR>
          <%}%>
      </TABLE>

      <!-- Show Assertion Conflict Buttons -->
<%
    if ( isConflict) { // only show when in conflict
        System.out.println("<hr>\n");
        if (isRedundant) {
%>

      <TABLE CELLPADDING="10">
        <TR>
          <TD> <B>Diagnostics : <FONT color=red>Redundant</FONT></B></TD>
          <TD><INPUT type="submit" name="a_t_req" value="revert"> </INPUT></TD>
          <TD><INPUT type="submit" name="a_t_req" value="keep"> </INPUT></TD>
        </TR>
      </TABLE>
<%
        }
        else if (isContradictory) {
%>
      <TABLE CELLPADDING="10">
        <TR>
          <TD> <B>Diagnostics : <FONT color=red> Contradiction </FONT></B><br> </TD>
          <TD><INPUT type="submit" name="a_t_req" value="revert"> </INPUT></TD>
        </TR>
      </TABLE>
<%
        }
    }
    if (!isError && isAffirmationNeeded) {
        StringBuffer termList = new StringBuffer();
        Iterator ite = termsToBeAffirmed.iterator();
        while (ite.hasNext()) {
            String curTerm = ite.next().toString();
            System.out.println("cur term:"+ curTerm);
            termList.append(curTerm).append(" ");
        }
%>
      <TABLE CELLPADDING="10">
        <TR>
          <TD> <B> Assertion with terms not found : <FONT color=red> <%=termList.toString()%> </FONT></B><br> </TD>
          <TD><INPUT type="submit" name="a_t_req" value="assert_affirmed"> </INPUT></TD>
        </TR>
      </TABLE>
<%
    }
%>

</FORM>

<%= sbStatus.toString().length()>0? "<table ALIGN='LEFT' WIDTH=80%%><tr><TD BGCOLOR='#AAAAAA'><IMG SRC='pixmaps/1pixel.gif' width=1 height=1 border=0></TD></tr></table><BR> <H4> Status: </H4>" + sbStatus.toString():"" %>
<%= result.toString().length()>0? "<table ALIGN='LEFT' WIDTH=80%%><tr><TD BGCOLOR='#AAAAAA'><IMG SRC='pixmaps/1pixel.gif' width=1 height=1 border=0></TD></tr></table><BR>" + result.toString():"" %>

<table ALIGN="LEFT" WIDTH=80%><tr><TD BGCOLOR='#AAAAAA'><IMG SRC='pixmaps/1pixel.gif' width=1 height=1 border=0></TD></tr></table><BR>
<small><i>&copy;2003 Teknowledge Corporation, All rights reserved</i></small>
</BODY>
</HTML>