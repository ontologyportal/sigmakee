<%@ include file="fragments/universal/Prelude.jspf" %>
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
    response.sendRedirect("login.jsp");
    return;
}

String ccheckPage = request.getParameter("page");
if (ccheckPage == null)
    ccheckPage = "0";
String action = request.getParameter("action");
String override = request.getParameter("override");

StringBuilder show = new StringBuilder();       // Variable to contain the HTML page generated.
String kbHref = null;
String htmlDivider = "<table ALIGN='LEFT' WIDTH='50%'><tr><TD BGCOLOR='#A8BACF'><IMG SRC='pixmaps/1pixel.gif' width=1 height=1 border=0></TD></tr></table><BR><BR>\n";
String formattedFormula = null;
Map theMap = null;
HttpSession hsObj = request.getSession();
hsObj.setMaxInactiveInterval(-1);
kbHref = HTMLformatter.createHrefStart() + "/sigma/Browse.jsp?lang=" + lang + "&kb=" + kbName + "&flang=" + flang;
%>
<html>
<HEAD><TITLE> Knowledge base Browser</TITLE></HEAD>
<BODY BGCOLOR="#FFFFFF">
<FORM action="CCheck.jsp">
<%
    String pageName = "CCheck";
    String pageString = "Knowledge Based Consistency Check";
%>
<%@include file="fragments/universal/CommonHeader.jspf" %>
<%
String translationMode = request.getParameter("translationMode");
if (StringUtil.emptyString(translationMode))
    translationMode = "FOL";
String TPTPlang = request.getParameter("TPTPlang");
if (StringUtil.emptyString(TPTPlang)) {
    if ("HOL".equalsIgnoreCase(translationMode)) TPTPlang = "thf";
    else TPTPlang = "fof";
}

String cwa = request.getParameter("CWA");
boolean closedWorldAssumption = "yes".equalsIgnoreCase(cwa);

String inferenceEngine = request.getParameter("inferenceEngine");
if (StringUtil.emptyString(inferenceEngine)) inferenceEngine = "EPROVER";

String vampireMode = request.getParameter("vampireMode");
if (StringUtil.emptyString(vampireMode)) vampireMode = "CASC";

boolean modusPonens = "yes".equalsIgnoreCase(request.getParameter("ModusPonens"));
Boolean dropOnePremise = "true".equalsIgnoreCase(request.getParameter("dropOnePremise"));
boolean holUseModals = "yes".equalsIgnoreCase(request.getParameter("HolUseModals"));

String timeoutStr = request.getParameter("timeout");
int timeout = 30;
if (!StringUtil.emptyString(timeoutStr))
    timeout = Integer.parseInt(timeoutStr);

String maxAnswersStr = request.getParameter("maxAnswers");
int maxAnswers = 1;
if (!StringUtil.emptyString(maxAnswersStr))
    maxAnswers = Integer.parseInt(maxAnswersStr);

List<String> availableProvers = TheoremProverController.availableProvers();
%>

<%
show = new StringBuilder();
int pageNum = Integer.parseInt(ccheckPage);
boolean overrideValue = false;
if (!StringUtil.emptyString(action))
    overrideValue = true;
if (override != null && override.equalsIgnoreCase("true"))
    overrideValue = true;

CCheckManager ccheckManager = (CCheckManager) application.getAttribute("ccheckManager");
if (ccheckManager == null) {
    synchronized (application) {
        ccheckManager = (CCheckManager) application.getAttribute("ccheckManager");
        if (ccheckManager == null) {
            ccheckManager = new CCheckManager();
            application.setAttribute("ccheckManager", ccheckManager);
        }
    }
}

if (ccheckManager.ccheckStatus(kb.name) == CCheckStatus.ONGOING) {
    show.append(HTMLformatter.formatConsistencyCheck(kb.name + " is currently undergoing checks.  Partial results are available.", ccheckManager.ccheckResults(kb.name), lang, pageNum));
    show.append("<p>[&nbsp; <a href='CCheck.jsp?kb=" + kb.name + "&lang=" + lang + "&page=" + pageNum + "&override=false'>Refresh</a>&nbsp; ] </p>");
}
else if (ccheckManager.ccheckStatus(kb.name) == CCheckStatus.DONE)
    show.append(HTMLformatter.formatConsistencyCheck(kb.name + "  has been checked. Results can be found below. [<a href=CCheck.jsp?kb=" + kb.name + "&lang=" + lang + "&override=true&page=0>Restart Check</a>]", ccheckManager.ccheckResults(kb.name), lang, pageNum));
else if (ccheckManager.ccheckStatus(kb.name) == CCheckStatus.QUEUED) {
    show.append(kb.name + " has been added to the queue for consistency checks.");
    show.append("<p>[&nbsp; <a href='CCheck.jsp?kb=" + kb.name + "&lang=" + lang + "&page=" + pageNum + "&override=false'>Refresh</a>&nbsp; ] </p>");
}
else if (!overrideValue && ccheckManager.ccheckStatus(kb.name) == CCheckStatus.NOCCHECK) {
    show.append("Please choose translation and prover settings.<br>");
    show.append("__SHOW_CCHECK_FORM__");
}
else if (overrideValue && ccheckManager.ccheckStatus(kb.name) == CCheckStatus.NOCCHECK) {
    String chosenEngine = inferenceEngine;
    if ("Vampire".equalsIgnoreCase(chosenEngine)) chosenEngine = "VAMPIRE";
    else if ("EProver".equalsIgnoreCase(chosenEngine) || "E".equalsIgnoreCase(chosenEngine)) chosenEngine = "EPROVER";
    else if ("LeoLocal".equalsIgnoreCase(chosenEngine) || "LEO-III".equalsIgnoreCase(chosenEngine)) chosenEngine = "LEO";
    String systemChosen = "";
    boolean ccheck = false;
    String location = "";
    if (StringUtil.emptyString(chosenEngine)) show.append("Cannot start consistency check as no inference engine was chosen.");
    else ccheck = true;
    if (ccheck) {
        String atpLanguage;
        if ("HOL".equalsIgnoreCase(translationMode)) atpLanguage = "THF";
        else if ("tff".equalsIgnoreCase(TPTPlang)) atpLanguage = "TFF";
        else atpLanguage = "FOF";
        if ("LEO".equalsIgnoreCase(chosenEngine)) atpLanguage = "THF";

        show.append("Chosen inference engine: " + chosenEngine + "<br>");
        show.append("Chosen translation language: " + atpLanguage + "<br>");
        show.append("Entered timeout: " + timeout + " seconds.<br>");
        show.append("Maximum answers: " + maxAnswers + "<br>");

        if ("EPROVER".equalsIgnoreCase(chosenEngine) && "THF".equalsIgnoreCase(atpLanguage)) {
            show.append("EProver does not support THF/HOL. Choose FOF/TFF or use Vampire/LEO.");
        }
        else if (ccheckManager.performConsistencyCheck(
                kb,
                hsObj.getId(),
                chosenEngine,
                atpLanguage,
                vampireMode,
                closedWorldAssumption,
                modusPonens,
                Boolean.TRUE.equals(dropOnePremise),
                holUseModals,
                timeout,
                maxAnswers) == CCheckStatus.QUEUED) {
            show.append(kb.name + " has been added to the queue for consistency checks. <br>");
            show.append("<p>[&nbsp; <a href='CCheck.jsp?kb=" + kb.name + "&lang=" + lang + "&page=" + pageNum + "&override=false'>Refresh</a>&nbsp; ] </p>");
        }
        else show.append("Error trying to start consistency check for " + kb.name + ". Please try again.");
    }
}
else show.append("Error trying to start consistency check for " + kb.name + ". Please try agian.");

show.append("</form>");
%>

<table ALIGN='LEFT' WIDTH='50%'>
    <tr>
        <TD BGCOLOR='#A8BACF'>
            <IMG SRC='pixmaps/1pixel.gif' width=1 height=1 border=0>
        </TD>
    </tr>
</table>
<BR><BR>

<%
String showString = show.toString();
boolean showCCheckForm = showString.contains("__SHOW_CCHECK_FORM__");
showString = showString.replace("__SHOW_CCHECK_FORM__", "");
%>

<%=showString%>

<% if (showCCheckForm) { %>
    <%@ include file="fragments/universal/TranslationSelector.jspf" %>
    <%@ include file="fragments/universal/ProverSelector.jspf" %>

    <input type="hidden" name="override" value="true">
    <input type="hidden" name="kb" value="<%=kbName%>">
    <input type="hidden" name="lang" value="<%=lang%>">
    <input type="hidden" name="flang" value="<%=flang%>">
    <input type="hidden" name="page" value="0">

    <br>
    <input type="submit" name="action" value="Submit Consistency Check">
<% } %>

<BR>
<%@ include file="fragments/universal/Postlude.jspf" %>
</BODY>
</HTML>