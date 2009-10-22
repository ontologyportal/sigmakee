
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
<script type="text/javascript">
    // Toggle the checked status of all the equivalence checkboxes
    function ToggleAllEq(theForm,cb){

        var check = false;
        if (cb.checked) {
            check = true;
        } 
        for (i = 0; i < theForm.length; i++) {
            var e = theForm.elements[i];
            if (e.name.substring(0,9) == "checkbox_") {
                e.checked = check;
            }
        }
    }

    // Toggle the checked status of just the best mapping for
    // each term.
    function ToggleBestEq(theForm,cb){

        var check = false;
        if (cb.checked) {
            check = true;
        } 
        for (i = 0; i < theForm.length; i++) {
            var e = theForm.elements[i];
            if (e.name.substring(0,11) == "checkbox_T_") {
                e.checked = check;
            }
        }
    }

    // Toggle the checked status of all the equivalence checkboxes
    function ToggleAllSub(theForm,cb){

        var check = false;
        if (cb.checked) {
            check = true;
        } 
        for (i = 0; i < theForm.length; i++) {
            var e = theForm.elements[i];
            if (e.name.substring(0,13) == "sub_checkbox_") {
                e.checked = check;
            }
        }
    }

    // Toggle the checked status of just the best mapping for
    // each term.
    function ToggleBestSub(theForm,cb){

        var check = false;
        if (cb.checked) {
            check = true;
        } 
        for (i = 0; i < theForm.length; i++) {
            var e = theForm.elements[i];
            if (e.name.substring(0,13) == "sub_checkbox_" && e.name.substring(0,15) != "sub_checkbox_T_") {
                e.checked = check;
            }
        }
    }
</script>

<BODY BGCOLOR=#FFFFFF>
<%
    TreeSet cbset = new TreeSet();
    String status = null;
    Enumeration params = request.getParameterNames();
    while (params.hasMoreElements()) {
         String elem = (String) params.nextElement();
         if (elem.startsWith("checkbox") || elem.startsWith("sub_checkbox")) 
             cbset.add(elem);         
    }
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
    String find = request.getParameter("find");
    String save = request.getParameter("save");
    String merge = request.getParameter("merge");
    if (kbname1 == null) 
        kbname1 = "Select a KB";
    if (kbname2 == null) 
        kbname2 = "Select a KB";
    if (!kbname1.equals("Select a KB") && !kbname2.equals("Select a KB")) {
        if (find != null && find.startsWith("Find")) 
            Mapping.mapOntologies(kbname1,kbname2,threshold);        
        if (save != null && save.startsWith("Save")) 
            status = Mapping.writeEquivalences(cbset,kbname1,kbname2);
        if (merge != null && merge.startsWith("Merge")) {
            status = Mapping.merge(cbset,kbname1,kbname2);
            cbset = new TreeSet();
            Mapping.mappings = new TreeMap();
        }
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
        <b>[ <a href="KBs.jsp">Home</b></a>&nbsp;|
            &nbsp;<A href="Properties.jsp">Preferences</b></A>&nbsp;</FONT> ]</b></FONT></td>
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
       out.println("<table><tr><td>KB #1</td><td>KB #2</td></tr>");
       out.print("<tr><td>");
       out.println(HTMLformatter.createMenu("kbname1",kbname1,kbnames));
       out.print("</td><td>");
       out.println(HTMLformatter.createMenu("kbname2",kbname2,kbnames));
       out.println("</td></tr></table>");
       out.println("<P><INPUT type=\"submit\" NAME=\"find\" VALUE=\"Find Mappings\"><P><hr align=left width=\"30%\">");
       out.println("<P>");
       if (Mapping.mappings != null && Mapping.mappings.keySet().size() > 0 && 
           kbname1 != null && !kbname1.equals("Select a KB") && 
           kbname2 != null && !kbname2.equals("Select a KB") ) {
           out.println("<table><tr><td><b>KB#1: " + kbname1 + 
                       "</b></td><td>equiv.</td><td>subclass</td><td><b>KB#2: " + 
                         kbname2 + "</b></td></tr>\n");
           out.print("<tr><td></td><td><input type=\"checkbox\" name=\"toggleAll\"" +
                       " title=\"ToggleAllEq\" onclick=\"ToggleAllEq(document.kbmapper, this);\" />" +
                       "toggle all</td>");
           out.println("<td><input type=\"checkbox\" name=\"toggleAll\"" +
                       " title=\"ToggleAllSub\" onclick=\"ToggleAllSub(document.kbmapper, this);\" />" +
                       "toggle all</td><td></td></tr>");

           out.print("<tr><td></td><td><input type=\"checkbox\" name=\"toggleBest\"" +
                       " title=\"ToggleBestEq\" onclick=\"ToggleBestEq(document.kbmapper, this);\" />" +
                       "toggle best</td>");
           out.println("<td><input type=\"checkbox\" name=\"toggleAll\"" +
                       " title=\"ToggleBestSub\" onclick=\"ToggleBestSub(document.kbmapper, this);\" />" +
                       "toggle best</td><td></td></tr>");
           boolean even = true;
           Iterator it = Mapping.mappings.keySet().iterator();
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
                               name1 + ")</td>");
               else
                   out.println("<td><a href=\"" + kbHref1 + term1 + "\">" + term1 + "</a>" +
                               "</td>");
               TreeMap value = (TreeMap) Mapping.mappings.get(term1);
               int counter = 0;
               Iterator it2 = value.keySet().iterator();
               while (it2.hasNext() && counter < 10) {
                   counter++;
                   Integer score = (Integer) it2.next();
                   String term2 = (String) value.get(score);

                   KB kb2 = KBmanager.getMgr().getKB(kbname2);
                   String name2 = Mapping.getTermFormat(kb2,term2);
                   String topScoreFlag = "";
                   if (counter == 1)
                                            // Since we're iterating through a TreeSet we know
                       topScoreFlag = "T_"; // the first element has the top score.                   
                   if (counter > 1) 
                       out.println("<tr><td></td>");
                   out.print("<td><input type=\"checkbox\" name=\"checkbox_" +
                               topScoreFlag + term1 + "-" + term2 + "\" id=\"checkbox_"+
                               topScoreFlag + term1 + "-" + term2 + "\" ");
                   if (((cbset == null || cbset.size() < 1) && counter == 1) || 
                       ((cbset != null && cbset.contains("checkbox_" + topScoreFlag + term1 + "-" + term2))))
                       out.print("checked");
                   out.println(" /></td>");
                   
                   out.print("<td><input type=\"checkbox\" name=\"sub_checkbox_" +
                               topScoreFlag + term1 + "-" + term2 + "\" id=\"checkbox_"+
                               topScoreFlag + term1 + "-" + term2 + "\" ");

                   if (((cbset == null || cbset.size() < 1) && counter != 1) || 
                       ((cbset != null && cbset.contains("sub_checkbox_" + topScoreFlag + term1 + "-" + term2))))
                       out.print("checked");
                   out.println(" /></td>");
                   if (name2 != null)                        
                       out.println("<td><a href=\"" + kbHref2 + term2 + "\">" + term2 + "</a> (" +
                                   name2 + ") - " + score.toString() + "</td>");
                   else
                       out.println("<td><a href=\"" + kbHref2 + term2 + "\">" + term2 + "</a>" +
                                   " - " + score.toString() + "</td>");
                   out.println("</tr>");
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
  <P><INPUT type="submit" NAME="merge" VALUE="Merge (#2 into #1)"><P>
</FORM><P>

<%@ include file="Postlude.jsp" %>
</BODY>
</HTML>


