<%@include file="Prelude.jsp" %>
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
%>

<HTML>
<HEAD>
  <TITLE>Sigma Knowledge Engineering Environment - Test Statement</TITLE>
</HEAD>
<%
    System.out.println("INFO in TestStatement.jsp");
    StringBuffer status = new StringBuffer();
    ArrayList processedStmts = null;
    String stmt = request.getParameter("stmt");
    String req = request.getParameter("request");
    boolean error = false;

    if ((kbName == null) || kbName.equals("")) {
        System.out.println("Error: No knowledge base specified");
        kbName = KBmanager.getMgr().getPref("sumokbname");
    }
    
    if (stmt != null)
        System.out.println("text box input: " + stmt.trim());

    if (stmt == null || stmt.equalsIgnoreCase("null"))   // check if there is an attribute for stmt
        stmt = "(instance ?X Relation)";    
    else {
        String msg = (new KIF()).parseStatement(stmt);
        if (msg != null) {
            status.append("Syntax Error: " + msg + "<P>\n");
            error = true;
        }
        else {
            Formula f = new Formula(stmt);
            FormulaPreprocessor fp = new FormulaPreprocessor();
            Set<Formula> res = fp.preProcess(f,false,kb);
            if (f.errors != null && f.errors.size() > 0) {
                status.append("Error:  " + f.errors.toString() + "<P>\n");
                error = true;
            }
            SUMOtoTFAform.initOnce();
            SUMOtoTFAform.varmap = fp.findAllTypeRestrictions(f, kb);
            if (SUMOtoTFAform.inconsistentVarTypes()) {
                status.append("Inconsistent variable types: " + SUMOtoTFAform.varmap + "<P>\n");
                error = true;
            }
            if (Diagnostics.quantifierNotInStatement(f)) {
                status.append("Quantifier not in body<P>\n");
                error = true;
            }
            HashSet<String> svars = Diagnostics.singleUseVariables(f);
            if (svars != null && svars.size() > 0) {
                status.append("Single use variables: " + svars + "<P>\n");
                error = true;
            }
            String pred = PredVarInst.hasCorrectArity(f,kb);
            if (pred != null) {
                status.append("Incorrect arity for predicate: " + pred + "<P>\n");
                error = true;
            }
            Set<String> terms = f.collectTerms();
            if (terms != null) {
                for (String s : terms) {
                    if (!kb.terms.contains(s) && !Formula.isVariable(s)) {
                        status.append("Unknown term: " + s + "<P>\n");
                      error = true;
                    }
                }
            }
        }
    }

    if (!error)
        status.append("Statement ok<P>");

    String lineHtml =
      "<table ALIGN='LEFT' WIDTH='40%'><tr><TD BGCOLOR='#AAAAAA'><IMG SRC='pixmaps/1pixel.gif' width=1 height=1 border=0></TD></tr></table><BR>\n";
%>

<BODY style="face=Arial,Helvetica" BGCOLOR=#FFFFFF>
<FORM name="TestStmnt" ID="TestStmnt" action="TestStmnt.jsp" METHOD="POST">
    <%
        String pageName = "TestStmnt";
        String pageString = "Test A Statement";
    %>
    <%@include file="CommonHeader.jsp" %>
    
    <IMG SRC='pixmaps/1pixel.gif' width=1 height=1 border=0><BR>
    <textarea rows="5" cols="70" name="stmt"><%=stmt%></textarea>
    <br>
<% if (role != null && role.equalsIgnoreCase("admin")) { %>
    <INPUT type="submit" name="request" value="Test"><BR>
<% } %>
</FORM>
<table ALIGN='LEFT' WIDTH='80%'><tr><TD BGCOLOR='#AAAAAA'>
<IMG SRC='pixmaps/1pixel.gif' width=1 height=1 border=0></TD></tr></table><BR>

<%
    if (status != null && status.toString().length() > 0) {
        out.println(status.toString());
    }
%>
    <p>

<%@ include file="Postlude.jsp" %>

</BODY>
</HTML>
 
