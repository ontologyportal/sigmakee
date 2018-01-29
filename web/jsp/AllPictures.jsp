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

/**
 * Browse.jsp responds to several HTTPD parameters:
 * term     = <name>   - the SUMO term to browse
 * kb       = <name>   - the name of the knowledge base
 * lang     = <lang>   - the name of the language used to display axiom paraphrases
 * */
%>
<html>
<HEAD><TITLE>Knowledge Base Picture Browser - <%=term%></TITLE></HEAD>
<BODY BGCOLOR="#FFFFFF">

<%
 StringBuffer show = new StringBuffer();

 show.append("<table width='95%'><tr><td width='50%'><FONT face='Arial,helvetica' size=+3><b>");
 if (!StringUtil.emptyString(term)) {
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
