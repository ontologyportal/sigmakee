
<%@ include file="Prelude.jsp" %>
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
August 9, Acapulco, Mexico. See also http://sigmakee.sourceforge.net
*/

/**
 * Browse.jsp responds to several HTTPD parameters:
 * term     = <name>   - the SUMO term to browse
 * kb       = <name>   - the name of the knowledge base
 * lang     = <lang>   - the name of the language used to display axiom paraphrases
 * */

 String term = request.getParameter("term");
 if (!Formula.isNonEmptyString(term))
     term = "";
%>
<html>
<HEAD><TITLE>Knowledge Base Picture Browser - <%=term%></TITLE></HEAD>
<BODY BGCOLOR="#FFFFFF">

<%
 String kbName = "";
 StringBuffer show = new StringBuffer();
 term = request.getParameter("term");
 kbName = request.getParameter("kb");
 if (Formula.isNonEmptyString(kbName)) {
     kb = KBmanager.getMgr().getKB(kbName);
 }
 if (kb == null)
     response.sendRedirect("login.html");

 show.append("<table width='95%'><tr><td width='50%'><FONT face='Arial,helvetica' size=+3><b>");
 if (Formula.isNonEmptyString(term)) {
     term = term.intern();
     show.append(term);
     show.append("</b></FONT>");
     if (Character.isLowerCase(term.charAt(0)) || term.endsWith("Fn")) {
         Map fm = kb.getFormatMap(language);
         String fmValue = null;
         if (fm != null)
             fmValue = (String) fm.get(term); 
         if (fmValue == null)
             System.out.println("INFO in BrowseBody.jsp: No format map entry for \"" +
                                term + "\" in language " + language);	   
     }
     else {
         Map tfm = kb.getTermFormatMap(language);
         String tfmValue = null;
         if (tfm != null)
             tfmValue = (String) tfm.get(term);
         if (tfmValue != null) 
             show.append("(" + tfmValue + ")");	    
         else
             System.out.println("INFO in BrowseBody.jsp: No term format map entry for \"" +
                                term + "\" in language " + language);	   
     }
     show.append("</td></tr>\n<tr><td>");
     show.append(HTMLformatter.showNumberPictures(kb,term,200));
     show.append("</td></tr></table><P>\n");
 }
%>


<br>
 <%=show.toString() %><BR>

<%@ include file="Postlude.jsp" %>
</BODY>
</HTML>
