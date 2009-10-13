
<%@ include file="Prelude.jsp" %>

<HTML>
<HEAD>
<TITLE>Sigma Knowledge Engineering Environment - Main</TITLE>
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
August 9, Acapulco, Mexico.  See also http://sigmakee.sourceforge.net
*/
%>
</HEAD>

<BODY BGCOLOR=#FFFFFF>
<%
    TreeMap result = null;
    String status = null;
    System.out.println("INFO in Mapping.jsp");
    String prefUserName = KBmanager.getMgr().getPref("userName");
    String hostname = KBmanager.getMgr().getPref("hostname");
    if (StringUtil.emptyString(hostname))
        hostname = "localhost";
    String port = KBmanager.getMgr().getPref("port");
    if (StringUtil.emptyString(port))
        port = "8080";
    String matchMethod = request.getParameter("matchMethod");
    if (matchMethod == null) 
        matchMethod = "Substring";
    String thresholdSt = request.getParameter("threshold");
    int threshold = 10;
    if (thresholdSt != null) 
        threshold = Integer.valueOf(thresholdSt).intValue();    
    String kbname1 = request.getParameter("kbname1");
    String kbname2 = request.getParameter("kbname2");
    String save = request.getParameter("save");
    if (kbname1 == null) 
        kbname1 = "Select a KB";
    if (kbname2 == null) 
        kbname2 = "Select a KB";
    if (!kbname1.equals("Select a KB") && !kbname2.equals("Select a KB")) {
        Mapping m = new Mapping();
        result = m.mapOntologies(kbname1,kbname2,threshold);
        if (save != null && save.startsWith("Save")) 
            status = m.writeEquivalences(result,kbname1,kbname2,threshold);
    }
    String kbHref1 = "http://" + hostname + ":" + port + "/sigma/Browse.jsp?kb=" + kbname1 + "&term=";
    String kbHref2 = "http://" + hostname + ":" + port + "/sigma/Browse.jsp?kb=" + kbname2 + "&term=";
%>

<table width="95%" cellspacing="0" cellpadding="0">
    <tr>
        <td valign="top">
            <table cellspacing="0" cellpadding="0">
                <tr>
                    <td align="left" valign="top"><img src="pixmaps/sigmaSymbol.gif"></td>
                    <td>&nbsp;&nbsp;</td>
                    <td align="left" valign="top"><img src="pixmaps/logoText.gif"></td>
                </tr>                
            </table>
        </td>
        <td><font face="Arial,helvetica" SIZE=-1>
        <b>[ <A href="Properties.jsp">Preferences</b></A>&nbsp;</FONT>
        <font face="Arial,helvetica" SIZE=-1> ]</b></FONT></td>
    </tr>
</table>
<br>

<table ALIGN="LEFT" WIDTH=80%><tr><TD BGCOLOR='#AAAAAA'>
  <IMG SRC='pixmaps/1pixel.gif' width=1 height=1 border=0></TD></tr></table><BR>
</TABLE>

<FORM name=kbmapper ID=kbmapper action="Mapping.jsp" method="GET">
<%
       ArrayList kbnames = new ArrayList();
       kbnames.addAll(KBmanager.getMgr().getKBnames());
       kbnames.add("Select a KB");
       out.println(HTMLformatter.createMenu("kbname1",kbname1,kbnames));
       out.println(HTMLformatter.createMenu("kbname2",kbname2,kbnames));
       out.println("<P><INPUT type=\"submit\" NAME=\"submit\" VALUE=\"Find Mappings\"><P>");
       out.println("<P>");
       if (result != null) {
           out.println("<table><tr><td><b>" + kbname1 + "</b></td><td><b>" + 
                         kbname2 + "</b></td></tr>\n");
           boolean even = true;
           Iterator it = result.keySet().iterator();
           while (it.hasNext()) {
               String term1 = (String) it.next();
               if (even) 
                   out.println("<tr bgcolor=#DDDDDD>");
               else
                   out.println("<tr>");
               even = !even;
               KB kb1 = KBmanager.getMgr().getKB(kbname1);
               String name1 = Mapping.getTermFormat(kb1,term1);
               if (name1 != null)                
                   out.println("<td><a href=\"" + kbHref1 + term1 + "\">" + term1 + "</a> (" +
                               name1 + ")</td><td>");
               else
                   out.println("<td><a href=\"" + kbHref1 + term1 + "\">" + term1 + "</a>" +
                               "</td><td>");
               TreeMap value = (TreeMap) result.get(term1);
               int counter = 0;
               Iterator it2 = value.keySet().iterator();
               while (it2.hasNext() && counter < 10) {
                   counter++;
                   Integer score = (Integer) it2.next();
                   String term2 = (String) value.get(score);

                   KB kb2 = KBmanager.getMgr().getKB(kbname2);
                   String name2 = Mapping.getTermFormat(kb2,term2);
                   if (name2 != null)                        
                       out.println("<a href=\"" + kbHref2 + term2 + "\">" + term2 + "</a> (" +
                                   name2 + ") - " + score.toString() + "<br>");
                   else
                       out.println("<a href=\"" + kbHref2 + term2 + "\">" + term2 + "</a>" +
                                   " - " + score.toString() + "<br>");
               }
               out.println("</tr>");
           }
           out.println("</table>");
       }
       if (status != null)        
           out.println(status + "<P>");
%>
  <P>Match Threshold (lower is more strict): 
     <INPUT type="text" size="5" name="threshold" value="<%=threshold%>">
  <p>String match method: 
     <input type="radio" name="matchMethod" value="Substring" <%=matchMethod.equals("Substring") ? "checked" : ""%>>Substring 
     <input type="radio" name="matchMethod" value="JaroWinkler" <%=matchMethod.equals("JaroWinkler") ? "checked" : ""%>>JaroWinkler<br>
  <P><INPUT type="submit" NAME="save" VALUE="Save Mappings"><P>
</FORM><P>

<%@ include file="Postlude.jsp" %>
</BODY>
</HTML>

 
