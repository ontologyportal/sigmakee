<%@ include file="Prelude.jsp" %>
<html>

<HEAD>
    <TITLE>Sigma KB Hyper-Browser</TITLE>
    <style>
    @import url(kifb.css);
    </style>
</HEAD>

<BODY BGCOLOR="#FFFFFF">

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

  StringBuffer show = new StringBuffer();       // Variable to contain the HTML page generated.
  StringBuffer sbStatus = new StringBuffer();   // Variable to contain error messages and user feedback.
  
  // Variables corresponding to inputs in the skb web form.

  String isPattern = null;
  String relation = null;
  String skb_href = null;
  String skbName = null;   // Name of the knowledge base
  SigmaKB theSKB = null;   // The knowledge base object.

  // Parameters for showing alphabetic neighbours of a term.
  int lowerTermIndex = -1;
  int upperTermIndex = -1;
  String lowerCaseTerm = null;
  String upperCaseTerm = null;

  /* The request can be
        skb_ss - show knowledge base statistics
        skb_sc - show regular expression match
        skb_sr - show browser page for a term
        skb_sx - show alphabetic range near a term
        skb_sn - show alphabetic range near a term
  */

  String reqName = request.getParameter("req");
  String term = request.getParameter("term");
  String language = request.getParameter("lang");
  isPattern = request.getParameter("isPattern");
  skbName = request.getParameter("skb");
  theSKB = SKBMgr.getDefaultSKBMgr().getSKB(skbName,false);
  Map theMap = theSKB.getFormatMap(language);      // Map of natural language format strings.
  if (language != null) 
      theSKB.language = language;
  
  if (reqName == null)
      reqName = "skb_ss";   // Default to show knowledge base statistics.
  
  if (!reqName.startsWith("skb")) {// skb request  
      sbStatus.append("Error: No such request \"" + reqName + "\"\n");
      throw new IllegalArgumentException("No such request");
  }
  else {
      skb_href = "skb.jsp?req=skb_sr&lang=" + theSKB.language + "&skb=" + skbName + "&term=";

      if (term == null || term.equals(""))
          reqName = "skb_ss";  // Show statistics only when no term is specified.
      else if (isPattern != null && isPattern.equalsIgnoreCase("on")) 
          reqName = "skb_sc";
      else if (reqName.equalsIgnoreCase("skb_sx")) { // Show the alphabetic neighbors of a string.      
          int termIndex = -1;
          lowerCaseTerm = term.substring(0,1).toLowerCase()+term.substring(1);
          upperCaseTerm = term.substring(0,1).toUpperCase()+term.substring(1);
          lowerTermIndex= theSKB.getTermArrayIndex(lowerCaseTerm);
          upperTermIndex= theSKB.getTermArrayIndex(upperCaseTerm);
          if (lowerTermIndex < 0 && upperTermIndex < 0)  // show the position of the term if it is not found
              reqName = "skb_sn";
          else if (lowerTermIndex >= 0 && upperTermIndex >= 0)  {// match both          
              // show ^[Aa]ttribute
              String upperInitial = term.substring(0,1).toUpperCase();
              String lowerInitial = term.substring(0,1).toLowerCase();
              String thePattern = "^[" + upperInitial + lowerInitial +"]"+term.substring(1);
              response.sendRedirect("skb.jsp?req=skb_sc&skb="+ skbName +"&term="+ thePattern + "&isPattern=on");
              return;
          }
          else if (lowerTermIndex >=0 && upperTermIndex < 0) { // The pattern matches initial lower case, so show lower case term.          
              term = lowerCaseTerm;
              reqName = "skb_sr";
          }
          else if (lowerTermIndex < 0 && upperTermIndex >= 0) { // The pattern matches initial upper case, so show upper case term.          
              term = upperCaseTerm;
              reqName = "skb_sr";
          }
      }

      // Set the default relation for graphing.
      if (SigmaUtil.getRelationType(term).equals("class")) {
          Properties prop = Util.getProp(null,"sigma_properties","subclass_term_default","subclass");
          relation = (String)prop.get("subclass_term_default");
      }
      else {
          Properties prop = Util.getProp( null,"sigma_properties","subrelation_term_default","subrelation");
          relation = (String)prop.get("subrelation_term_default");
      }
  }

  try {
      if (reqName.startsWith("skb")) { // check to make sure SKB exists      
          if (skbName == null) {
              sbStatus.append("<font color='red'>Error: No knowledge base specified.</font>\n");
              throw new IllegalArgumentException();
          }
          else if (theSKB == null) {
              sbStatus.append("<font color='red'>Error: Failed to locate the requested knowledge base.</font>\n");
              throw new IllegalArgumentException();
          }
      }
    
      if (reqName.equalsIgnoreCase("skb_ss")) { // show statistics      
          show.append("<b>Knowledge base statistics: </b><br><table>");
          show.append("<tr bgcolor=#eeeeee><td>Total Terms</td><td>Total Axioms</td><td>Total Rules</td><tr><tr align='center'>\n");
          show.append("<td>  " + theSKB.getConsts().size());
          show.append("</td><td> " + theSKB.getCountAxioms());
          show.append("</td><td> " + theSKB.getCountRules());
          show.append("</td><tr> </table>\n");
      }
    
      if (reqName.equalsIgnoreCase("skb_sc")) { // Show all terms matching a regular expression.      
          ArrayList matchingTerms = null;
          try {
              matchingTerms = Util.patternMatchingCollect(theSKB.getConstsArray(),term);
          }
          catch (java.util.regex.PatternSyntaxException pse) {
              sbStatus.append("<Font color='red'> Error: "+pse.getDescription()+"</font>\n");
              throw new IllegalArgumentException("Error with regular expression");
          }
          if (matchingTerms == null || matchingTerms.size() == 0)          
              show.append("There is no match.");
          else {
              show.append("<TABLE border=0><TR>");
              String curTerm = null;
              for (int i = 0 ; i < matchingTerms.size(); i++) {
                  curTerm = matchingTerms.get(i).toString();
                  show.append("<TD><A href='skb.jsp?req=skb_sr&skb="+ skbName +"&term="+curTerm+"'>" + curTerm+"</A>" + "</TD>\n");
                  if ((i+1) % 5 == 0) show.append("</TR><TR>");
              }
              show.append("</TR></TABLE>");
          }
      }
    
      if (reqName.equalsIgnoreCase("skb_sn")) { // Show the term's alphabetic neighbours     
          // Show alphabetically later terms.
          lowerTermIndex = - lowerTermIndex - 2; // cal the actual position
          upperTermIndex = - upperTermIndex - 2;
    
          Object[] theArray = theSKB.getConstsArray();
    
          int lowerBeginIndex = lowerTermIndex - 15;
          int lowerEndIndex = lowerTermIndex + 15;
          int upperBeginIndex = upperTermIndex - 15;
          int upperEndIndex = upperTermIndex + 15;
    
          if (lowerBeginIndex < 0) lowerBeginIndex = 0;
          if (lowerEndIndex >= theArray.length) lowerEndIndex = theArray.length - 1;
          if (upperBeginIndex < 0) upperBeginIndex = 0;
          if (upperEndIndex >= theArray.length) upperEndIndex = theArray.length - 1;
          show.append(" <FONT face='Arial,helvetica' size=+3> <b> ");
          
          if (term != null) show.append(term);
          show.append("</b></FONT><br><br>");
          show.append("<TABLE>");
        
          for (int i = 0 ; i < 30; i++) {
              show.append("<TR>");
              if (( i + lowerBeginIndex ) <= lowerEndIndex) {
                  String curTerm = theArray[i+lowerBeginIndex].toString();
                  show.append( "<TD><A href='skb.jsp?req=skb_sr&skb="+ skbName +"&term="+curTerm+"'>" + curTerm+"</A>" + "</TD>\n");
              }
              else
                  show.append("<TD></TD>");
              if (( i + upperBeginIndex ) <= upperEndIndex) {
                  String curTerm = theArray[i+upperBeginIndex].toString();
                  show.append( "<TD><A href='skb.jsp?req=skb_sr&skb="+ skbName +"&term="+curTerm+"'>" + curTerm+"</A>" + "</TD>\n");
              }
              else
                  show.append("<TD></TD>\n");
              if ( (i + lowerBeginIndex ) == lowerTermIndex)
                  show.append("<TR><TD>"+"<FONT SIZE=4 COLOR=\"RED\">"+  lowerCaseTerm +" would appear here </FONT>"+ "</TD><TD></TD></TR>\n");
              
              if ( (i + upperBeginIndex ) == upperTermIndex)
                  show.append("<TR></TD><TD><TD>"+"<FONT SIZE=4 COLOR=\"RED\">"+  upperCaseTerm +" would appear here </FONT>"+ "</TD></TR>\n");             
          }
          show.append("</TABLE>");
      }
    
      // show the results statement for verified constant
      if (reqName.equalsIgnoreCase("skb_sr")) {
          // Maintain the list of recently-selected terms.
          ArrayList lsts = (ArrayList)session.getAttribute("searchTerms");
          if (lsts == null ) {
              lsts = new ArrayList();
              session.setAttribute("searchTerms",lsts);
          }
          if (!lsts.contains(term)) {
              if (isBrowseOnly && (lsts.size() > 10))   // Browse-only users are limited to a history list of 10 terms.
                  lsts.remove(0);
              lsts.add(term);
          }
    
          // Show all the statements that include the requested term.
    
          show.append("<FONT face='Arial,helvetica' size=+3><b>");
          if (term != null) 
              show.append( term);
          show.append("</b></FONT><br><br>");
    
          Collection col = null;
          Iterator ite = null;
          for (int arg=1; arg<6; arg++) {
              col = theSKB.getNonRuleStatements(term,arg, null,null);
              show = SigmaUtil.htmlSKB(theSKB, show,skbName,col,"appearance as argument number "+arg, theMap, skb_href,isBrowseOnly);
          }     
          col = theSKB.getAntecedentStatements(term,null);
          show = SigmaUtil.htmlSKB(theSKB, show,skbName,col,"Antecedent:", theMap, skb_href,isBrowseOnly);
          col = theSKB.getConsequentStatements(term,"TopLevelContext");
          show = SigmaUtil.htmlSKB(theSKB, show,skbName,col,"Consequent:", theMap, skb_href,isBrowseOnly);
          col = theSKB.getNonRuleNestedStatements(term,null);
          show = SigmaUtil.htmlSKB(theSKB, show,skbName,col,"Statements:", theMap, skb_href,isBrowseOnly);
          col = theSKB.getNonRuleStatements(term,0, null,null);
          show = SigmaUtil.htmlSKB(theSKB, show,skbName,col,"Ground statements", theMap, skb_href,isBrowseOnly);
      }
  }
  catch (IllegalArgumentException iae) {
      System.out.println("Error: Illegal argument supplied to skb.jsp.\n");
  }
  finally { }

%>

<!-- show SKB header and SKB search input -->

<FORM action="skb.jsp">
    <INPUT type="hidden" name="req" value="skb_sx">
    <table width="95%" cellspacing="0" cellpadding="0">
        <tr>
            <td align="left" valign="top"><img src="pixmaps/sigmaSymbol-gray.gif"></td>
            <td>&nbsp;</td>
            <td align="left" valign="top"><img src="pixmaps/logoText-gray.gif"><br>
                <B>Browsing Interface</B></td>
            <td valign="bottom"></td>
            <td><font face="Arial,helvetica"><b>[ <a href="home.jsp">Help</b></a>&nbsp;|&nbsp;</font>
                <%if (!isBrowseOnly){ %>
                <font face="Arial,helvetica"><A href="ask_tell.jsp?req=a_t&skb=<%=skbName %>"><b>Ask_Tell</b></A>&nbsp;|&nbsp;</font>
                <%}%>
                <%if (!isBrowseOnly) %>
                <font face="Arial,helvetica"><a href="prefs.jsp?back=true"><b>Prefs</b></a>&nbsp;|&nbsp;</font>
                <%%>
                <%if (!isBrowseOnly) %>
                <font face="Arial,helvetica"><a href="login.jsp?log_out=true"><b>Logout</b></a>&nbsp;|&nbsp;</font>
                <%%>
                <font face="Arial,helvetica"><a href="graphxml.jsp?skb=<%=skbName%>&relation=<%=relation%>&center=<%=term%>">
                <b>Graph</b></a>&nbsp;</font>
                <%if (!isBrowseOnly){ %>
                <font face="Arial,helvetica">|&nbsp; <a href="save_as.jsp?skb=<%=skbName%>"><b>Save As</b></a></font>
                <%}%>
                <B>]</B> <br>
                <img src="pixmaps/1pixel.gif" HEIGHT="3" ><br>
                <font face="Arial,helvetica"><b>KB:&nbsp;</b></font>
          <%= Util.genSelector(SKBMgr.getDefaultSKBMgr().getSKBKeys(),skbName,"skb") %>
          <font face="Arial,helvetica"><b>Language:&nbsp;</b></font>
          <%= Util.genSelector(theSKB.getLanguages(),language,"lang") %>
        </td>
        </tr>
    </table>
    <br>
    <table cellspacing="0" cellpadding="0">
        <tr>
            <td WIDTH="100"><font face="Arial,helvetica"><b>KB Term:&nbsp;</b></font></TD>
            <TD align="left" valign="top" COLSPAN="2">
                <INPUT type="text" size="38" name="term" value=<%= "\"" + (term==null?"":term) + "\"" %>>
            </td>
            <td align="left" valign="top">
                <INPUT type="submit" value="Show">
            </TD>
            <td align="left" valign="top">
                <INPUT type="checkbox" name="isPattern" <%= isPattern != null ? "checked" : "" %> >
                &nbsp;Pattern Search</TD>
        </tr>
        <TR>
            <TD><IMG SRC="pixmaps/1pixel.gif" Height="3"></TD>
        </TR>
</FORM>
<!-- show WordNet search input -->
<form method="GET" action="wn.jsp">
    <INPUT type="hidden" name="req" value="SC_eng">
    <tr>
        <td WIDTH="100"><font face="Arial,helvetica"><b>English Word:&nbsp;</b></font></TD>
        <td align="left" valign="top">
            <input type="text" size="27" name="term">
            <IMG SRC="pixmaps/1pixel.gif" WIDTH="3"></TD>
        <TD align="left" valign="top">
            <select name="POS">
                <option value="1">Noun <option value="2">Verb
            </select>
        </TD>
        <td align="left" valign="top">
            <input type="submit" value="Show">
        </TD>
    </TR>
    </TABLE>
</form>

<%
  // Show the list of most recently searched terms
  ArrayList lsts = (ArrayList) session.getAttribute("searchTerms");
  if (lsts != null && lsts.size() != 0) {
      if (!isBrowseOnly)
          out.println("<FORM action='Util.jsp' method='GET'>");
      out.println("<b> Latest search terms:</b>");
      Iterator ite = lsts.iterator();
      while (ite.hasNext()) {
          String str = (String)ite.next();
          out.println("<A href='" + skb_href + str + "'><font size='1'>" + str + "</font></A>\n");
      }
      if (!isBrowseOnly) {
    %>
        
<input type="hidden" name="attName" value="searchTerms">
<input type="hidden" name="back" value="true">
<input type="hidden" name="req" value="mvAtt">
<input type="submit" value="reset">
</FORM>
    <%
      }
  }

%>

<br>
<table ALIGN="LEFT" WIDTH="80%">
    <tr>
        <TD BGCOLOR='#AAAAAA'><IMG SRC='images/pixel.gif' width="1" height="1" border="0"></TD>
    </tr>
</table>
<BR>
<%
  if (show.length() > 0) {
      out.println(show);
      out.println("<br><table ALIGN='LEFT' WIDTH=80%%> <tr>\n");
      out.println("<TD BGCOLOR='#AAAAAA'><IMG SRC='pixmaps/1pixel.gif' width='1' height='1' border='0'></TD></tr></table><BR>\n"); 
  } 
  if (sbStatus.length() > 0) { 
      out.println("<h4>Status: </h4> " + sbStatus.toString()); 
      out.println("<br><table ALIGN='LEFT' WIDTH=80%%><tr>\n");
      out.println("<TD BGCOLOR='#AAAAAA'><IMG SRC='pixmaps/1pixel.gif' width='1' height='1' border='0'></TD></tr></table><BR>\n"); 
  } 
%>

</BODY>
</HTML>

