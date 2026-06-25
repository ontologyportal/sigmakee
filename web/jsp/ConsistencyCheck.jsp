<%@ page import="java.util.List" %>
<%@ page import="com.articulate.sigma.ConsistencyCheckManager" %>
<%@ page import="com.articulate.sigma.tp.TheoremProverController" %>
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

HttpSession hsObj = request.getSession();
hsObj.setMaxInactiveInterval(-1);

String pageName = "ConsistencyCheck";
String pageString = "Knowledge Based Consistency Check";

String ccheckPage = request.getParameter("page");
if (StringUtil.emptyString(ccheckPage))
    ccheckPage = "0";

int pageNum = 0;
try {
    pageNum = Integer.parseInt(ccheckPage);
}
catch (NumberFormatException nfe) {
    pageNum = 0;
}

String action = request.getParameter("action");
String override = request.getParameter("override");
boolean submitCheck = !StringUtil.emptyString(action) || "true".equalsIgnoreCase(override);

String kbHref = HTMLformatter.createHrefStart() +
        "/sigma/Browse.jsp?lang=" + lang +
        "&kb=" + kbName +
        "&flang=" + flang;

String translationMode = request.getParameter("translationMode");
if (StringUtil.emptyString(translationMode))
    translationMode = "FOL";

String TPTPlang = request.getParameter("TPTPlang");
if (StringUtil.emptyString(TPTPlang)) {
    if ("HOL".equalsIgnoreCase(translationMode))
        TPTPlang = "thf";
    else
        TPTPlang = "fof";
}

String inferenceEngine = request.getParameter("inferenceEngine");
if (StringUtil.emptyString(inferenceEngine))
    inferenceEngine = "EPROVER";

String chosenEngine = inferenceEngine;
if ("Vampire".equalsIgnoreCase(chosenEngine))
    chosenEngine = "VAMPIRE";
else if ("EProver".equalsIgnoreCase(chosenEngine) || "E".equalsIgnoreCase(chosenEngine))
    chosenEngine = "EPROVER";
else if ("LeoLocal".equalsIgnoreCase(chosenEngine) || "LEO-III".equalsIgnoreCase(chosenEngine))
    chosenEngine = "LEO";

String atpLanguage;
if ("HOL".equalsIgnoreCase(translationMode))
    atpLanguage = "THF";
else if ("tff".equalsIgnoreCase(TPTPlang))
    atpLanguage = "TFF";
else
    atpLanguage = "FOF";

if ("LEO".equalsIgnoreCase(chosenEngine))
    atpLanguage = "THF";

String vampireMode = request.getParameter("vampireMode");
if (StringUtil.emptyString(vampireMode))
    vampireMode = "CASC";

String cwa = request.getParameter("CWA");
boolean closedWorldAssumption = "yes".equalsIgnoreCase(request.getParameter("CWA"));
boolean modusPonens = "yes".equalsIgnoreCase(request.getParameter("ModusPonens"));
boolean dropOnePremise = "true".equalsIgnoreCase(request.getParameter("dropOnePremise"));
boolean holUseModals = "yes".equalsIgnoreCase(request.getParameter("HolUseModals"));

String timeoutStr = request.getParameter("timeout");
int timeout = 30;
if (!StringUtil.emptyString(timeoutStr)) {
    try {
        timeout = Integer.parseInt(timeoutStr);
    }
    catch (NumberFormatException nfe) {
        timeout = 30;
    }
}

String maxAnswersStr = request.getParameter("maxAnswers");
int maxAnswers = 1;
if (!StringUtil.emptyString(maxAnswersStr)) {
    try {
        maxAnswers = Integer.parseInt(maxAnswersStr);
    }
    catch (NumberFormatException nfe) {
        maxAnswers = 1;
    }
}

List<String> availableProvers = TheoremProverController.availableProvers();

ConsistencyCheckManager consistencyCheckManager =
        (ConsistencyCheckManager) application.getAttribute("consistencyCheckManager");

if (consistencyCheckManager == null) {
    synchronized (application) {
        consistencyCheckManager =
                (ConsistencyCheckManager) application.getAttribute("consistencyCheckManager");

        if (consistencyCheckManager == null) {
            consistencyCheckManager = new ConsistencyCheckManager();
            application.setAttribute("consistencyCheckManager", consistencyCheckManager);
        }
    }
}

ConsistencyCheckManager.ConsistencyCheckStatus status =
        consistencyCheckManager.ccheckStatus(kb.name);

StringBuilder show = new StringBuilder();
boolean showConsistencyCheckForm = false;
%>

<html>
<head>
    <title>Knowledge Base Consistency Check</title>
</head>

<body bgcolor="#FFFFFF">

<form action="ConsistencyCheck.jsp" method="get">

<%@ include file="fragments/universal/CommonHeader.jspf" %>

<%
if (status == ConsistencyCheckManager.ConsistencyCheckStatus.ONGOING) {
    show.append(HTMLformatter.formatConsistencyCheck(
            kb.name + " is currently undergoing checks. Partial results are available.",
            consistencyCheckManager.ccheckResults(kb.name),
            lang,
            pageNum));

    show.append("<p>[&nbsp; <a href='ConsistencyCheck.jsp?kb=")
            .append(kb.name)
            .append("&lang=")
            .append(lang)
            .append("&flang=")
            .append(flang)
            .append("&page=")
            .append(pageNum)
            .append("&override=false'>Refresh</a>&nbsp; ]</p>");
}
else if (status == ConsistencyCheckManager.ConsistencyCheckStatus.DONE) {
    show.append(HTMLformatter.formatConsistencyCheck(
            kb.name + " has been checked. Results can be found below. " +
                    "[<a href='ConsistencyCheck.jsp?kb=" + kb.name +
                    "&lang=" + lang +
                    "&flang=" + flang +
                    "&override=true&page=0'>Restart Check</a>]",
            consistencyCheckManager.ccheckResults(kb.name),
            lang,
            pageNum));
}
else if (status == ConsistencyCheckManager.ConsistencyCheckStatus.NOCCHECK && !submitCheck) {
    show.append("Please choose translation and prover settings.<br>");
    showConsistencyCheckForm = true;
}
else if (status == ConsistencyCheckManager.ConsistencyCheckStatus.NOCCHECK && submitCheck) {

    show.append("Chosen inference engine: ").append(chosenEngine).append("<br>");
    show.append("Chosen translation language: ").append(atpLanguage).append("<br>");
    show.append("Entered timeout: ").append(timeout).append(" seconds.<br>");
    show.append("Maximum answers: ").append(maxAnswers).append("<br>");

    if (StringUtil.emptyString(chosenEngine)) {
        show.append("Cannot start consistency check because no inference engine was chosen.");
    }
    else if ("EPROVER".equalsIgnoreCase(chosenEngine) && "THF".equalsIgnoreCase(atpLanguage)) {
        show.append("EProver does not support THF/HOL. Choose FOF/TFF or use Vampire/LEO.");
    }
    else {
        ConsistencyCheckManager.ConsistencyCheckStatus queueStatus =
                consistencyCheckManager.performConsistencyCheck(
                        kb,
                        hsObj.getId(),
                        chosenEngine,
                        atpLanguage,
                        vampireMode,
                        closedWorldAssumption,
                        modusPonens,
                        dropOnePremise,
                        holUseModals,
                        timeout,
                        maxAnswers);

        if (queueStatus == ConsistencyCheckManager.ConsistencyCheckStatus.QUEUED) {
            show.append(kb.name).append(" has been added to the queue for consistency checks.<br>");
            show.append("<p>[&nbsp; <a href='ConsistencyCheck.jsp?kb=")
                    .append(kb.name)
                    .append("&lang=")
                    .append(lang)
                    .append("&flang=")
                    .append(flang)
                    .append("&page=")
                    .append(pageNum)
                    .append("&override=false'>Refresh</a>&nbsp; ]</p>");
        }
        else if (queueStatus == ConsistencyCheckManager.ConsistencyCheckStatus.ONGOING) {
            show.append(kb.name).append(" is already undergoing consistency checks.<br>");
            show.append("<p>[&nbsp; <a href='ConsistencyCheck.jsp?kb=")
                    .append(kb.name)
                    .append("&lang=")
                    .append(lang)
                    .append("&flang=")
                    .append(flang)
                    .append("&page=")
                    .append(pageNum)
                    .append("&override=false'>Refresh</a>&nbsp; ]</p>");
        }
        else {
            show.append("Error trying to start consistency check for ")
                    .append(kb.name)
                    .append(". Please try again.");
        }
    }
}
else {
    show.append("Error trying to start consistency check for ")
            .append(kb.name)
            .append(". Please try again.");
}
%>
<table align="left" width="50%">
    <tr>
        <td bgcolor="#A8BACF">
            <img src="pixmaps/1pixel.gif" width="1" height="1" border="0">
        </td>
    </tr>
</table>
<br><br>
<%= show.toString() %>
<% if (showConsistencyCheckForm) { %>
    <%@ include file="fragments/tp/TranslationSelector.jspf" %>
    <%@ include file="fragments/tp/ProverSelector.jspf" %>
    <input type="hidden" name="override" value="true">
    <input type="hidden" name="kb" value="<%= kbName %>">
    <input type="hidden" name="lang" value="<%= lang %>">
    <input type="hidden" name="flang" value="<%= flang %>">
    <input type="hidden" name="page" value="0">
    <br>
    <input type="submit" name="action" value="Submit Consistency Check">
<% } %>
</form>
<br>
<%@ include file="fragments/universal/Postlude.jspf" %>
</body>
</html>