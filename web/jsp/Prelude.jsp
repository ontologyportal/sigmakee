<%@ page
   language="java"
   import="com.articulate.sigma.*,com.articulate.sigma.tp.*,com.articulate.sigma.trans.*,com.articulate.sigma.utils.*,com.articulate.sigma.wordNet.*,com.articulate.sigma.VerbNet.*,com.articulate.sigma.CCheckManager.*,java.text.ParseException,java.net.URLConnection, javax.servlet.http.HttpServletRequest, java.net.URL,com.oreilly.servlet.multipart.MultipartParser,com.oreilly.servlet.multipart.Part,com.oreilly.servlet.multipart.ParamPart,com.oreilly.servlet.multipart.FilePart,java.util.*,java.io.*, tptp_parser.*, TPTPWorld.*"
   pageEncoding="UTF-8"
   contentType="text/html;charset=UTF-8"
%>
<!DOCTYPE html
   PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN"
   "https://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<meta http-equiv="Content-Type" content="text/html; charset=iso-8859-1" />
<html xmlns="https://www.w3.org/1999/xhtml" lang="en-US" xml:lang="en-US">
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
boolean debug = false;
List<String> userPages = new ArrayList<>();
userPages.add("AllPictures.jsp");
userPages.add("Browse.jsp");
userPages.add("BrowseExtra.jsp");
userPages.add("Graph.jsp");
userPages.add("Intersect.jsp");
userPages.add("KBs.jsp");
userPages.add("LogLearn.jsp");
userPages.add("Manifest.jsp");
userPages.add("OWL.jsp");
userPages.add("OMW.jsp");
userPages.add("SimpleBrowse.jsp");
userPages.add("TreeView.jsp");
userPages.add("WordNet.jsp");
userPages.add("CheckKifFile.jsp");
userPages.add("Preferences.jsp");
String URLString = request.getRequestURL().toString();
String pageURLString = URLString.substring(URLString.lastIndexOf("/") + 1);

String username = (String) session.getAttribute("user");
String role = (String) session.getAttribute("role");
String language = (String) session.getAttribute("language");
if (username != null && role != null)
    if (debug) System.out.println("Prelude.jsp: username:role  " + username + " : " + role);
else
    if (debug) System.out.println("Prelude.jsp: null username or role");
String welcomeString = " : Welcome guest : <a href=\"login.html\">log in</a>";
if (!StringUtil.emptyString(username))
    welcomeString = " : Welcome " + username;
if (debug) System.out.println("Prelude.jsp: KBmanager initialized  " + KBmanager.initialized);
if (debug) System.out.println("Prelude.jsp: KBmanager initializing  " + KBmanager.initializing);
KBmanager mgr = KBmanager.getMgr();

if (StringUtil.emptyString(role)) {
    role = "guest";
}

if (!KBmanager.initialized) {
    if (debug) System.out.println("Prelude.jsp: SUMOKBtoTPTPKB.rapidParsing==" + SUMOKBtoTPTPKB.rapidParsing);
    mgr.initializeOnce();
    System.out.println("Prelude.jsp: initializing.  Redirecting to init.jsp.");
    response.sendRedirect("init.jsp");
    return;
}

if (role.equalsIgnoreCase("guest") && !userPages.contains(pageURLString)) {
    mgr.setError("You are not authorized to visit " + pageURLString);
    System.out.println("Prelude.jsp: Redirecting to KBs.jsp.");
    response.sendRedirect("KBs.jsp");
    return;
}

String simple = request.getParameter("simple");
if (StringUtil.isNonEmptyString(simple) && simple.equalsIgnoreCase("yes")) {
    out.println("");
    out.println("<link rel=\"stylesheet\" type=\"text/css\" href=\"simple.css\" />");
}

String kbName = request.getParameter("kb");
if (StringUtil.emptyString(kbName)) {
    kbName = "SUMO";
    if (!mgr.kbs.keySet().contains("SUMO"))
        kbName = mgr.getPref("sumokbname");
}

KB kb = mgr.getKB(kbName);
if (kb == null) {
    UserKBmanager userMgr = (UserKBmanager) session.getAttribute("kbManager");
    if (userMgr != null) {
        kb = userMgr.getKB(kbName);
    }
}
if (kb != null) {
    TaxoModel.kbName = kbName;
}

String filename = "";
String line = "";

String langParam = request.getParameter("lang");
if (StringUtil.isNonEmptyString(langParam)) {
    language = HTMLformatter.processNaturalLanguage(langParam, kb);
    session.setAttribute("language", language);
}

String hostname = mgr.getPref("hostname");
if (hostname == null)
    hostname = "localhost";
String port = mgr.getPref("port");
if (port == null)
    port = "8080";
String term = request.getParameter("term");
if (StringUtil.emptyString(term))
    term = "";
%>

<%
String reqLang = request.getParameter("lang");
if (StringUtil.isNonEmptyString(reqLang)) {
    session.setAttribute("language", reqLang);
}

String reqFlang = request.getParameter("flang");
if (StringUtil.isNonEmptyString(reqFlang)) {
    session.setAttribute("flang", reqFlang);
}

String reqTree = request.getParameter("treeView");
if (StringUtil.isNonEmptyString(reqTree)) {
    session.setAttribute("treeView", reqTree);
}

if (StringUtil.emptyString(language)) {
    language = "EnglishLanguage";
    session.setAttribute("language", language);
}

String flang = (String) session.getAttribute("flang");
if (StringUtil.emptyString(flang)) {
    flang = "SUO-KIF";
    session.setAttribute("flang", flang);
}

String treeView = (String) session.getAttribute("treeView");
if (StringUtil.emptyString(treeView)) {
    treeView = "default"; // whatever makes sense
    session.setAttribute("treeView", treeView);
}
%>
