<%@ include	file="Prelude.jsp" %>
<HTML>
<HEAD>
<TITLE>Sigma KB Preferences</TITLE>
<!-- <style>@import url(kifb.css);</style> -->
</HEAD>

<BODY BGCOLOR=#FFFFFF>
<table width=95% cellspacing=0 cellpadding=0><tr><td valign="top">
<table cellspacing=0 cellpadding=0><tr><td align="left" valign="top"><img src="pixmaps/sigmaSymbol-gray.gif"></td>
<td>&nbsp;</td><td align="left" valign="top"><img src="pixmaps/logoText-gray.gif">
<BR>&nbsp;&nbsp;&nbsp;<font COLOR=teal></font></td></tr></table></td><td valign="bottom"></td><td>
<font face="Arial,helvetica" SIZE=-1><b>[ <A href="home.jsp">Help</A> ]</b></FONT></td></tr></table>
<br><table ALIGN="LEFT" WIDTH=80%><tr><TD BGCOLOR='#AAAAAA'><IMG SRC='pixmaps/1pixel.gif' width=1 height=1 border=0></TD></tr></table><BR>

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

   String sub = request.getParameter("change");
   System.out.println("sub = " + sub);
   if ( sub != null && sub.trim().equalsIgnoreCase("change"))
   {
      // standard proof step
      String varStr = request.getParameter("stdPrf");
      System.err.println("proof: " + varStr);
      if ( varStr != null )
      {
        if ( varStr.indexOf("raw") >= 0)
        {
          RsUtil.g_proofFormat = RsUtil.RAW_FORMAT;
        }
        else if ( varStr.indexOf("standard") >= 0)
        {
          RsUtil.g_proofFormat = RsUtil.STANDARD_FORMAT;
        }
      }
      // english format
      varStr = request.getParameter("engFormat");
      if ( varStr != null && varStr.equalsIgnoreCase("on"))
      {
        PrefUtil.setSysProp(SigmaUtil.SYS_PROP_PATH,"isEngFormat","true");
      }
      else
      {
        PrefUtil.setSysProp(SigmaUtil.SYS_PROP_PATH,"isEngFormat","false");
      }
      // pretty print
      varStr = request.getParameter("prettyPrint");
      System.out.println(varStr);
      if ( varStr != null && varStr.equalsIgnoreCase("on"))
      {
        System.out.println(varStr);
        PrefUtil.setSysProp(SigmaUtil.SYS_PROP_PATH,"isPrettyPrint","true");
      }
      else
      {
        PrefUtil.setSysProp(SigmaUtil.SYS_PROP_PATH,"isPrettyPrint","false");
      }
      // hyper linke
      varStr = request.getParameter("hyperLink");
      if ( varStr != null && varStr.equalsIgnoreCase("on"))
      {
        PrefUtil.setSysProp(SigmaUtil.SYS_PROP_PATH,"isHyperLink","true");
      }
      else
      {
        PrefUtil.setSysProp(SigmaUtil.SYS_PROP_PATH,"isHyperLink","false");
      }

      // hyper linke
      varStr = request.getParameter("answerSkip");
      if ( varStr != null && varStr.equalsIgnoreCase("on"))
      {
        RsUtil.g_removeAnswerTag = true;
      }
      else
      {
        RsUtil.g_removeAnswerTag = false;
      }

      // tick wanted
      varStr = request.getParameter("tickWanted");
      if ( varStr != null && varStr.equalsIgnoreCase("on"))
      {
        SFormula.g_tickWanted = true;
      }
      else
      {
        SFormula.g_tickWanted = false;
      }

      // hold expansion
      varStr = request.getParameter("holdExpansion");
      if ( varStr != null && varStr.equalsIgnoreCase("on"))
      {
        SFormula.g_holdExpansion = true;
      }
      else
      {
        SFormula.g_holdExpansion = false;
      }

      // row expansion
      varStr = request.getParameter("rowExpansion");
      if ( varStr != null && varStr.equalsIgnoreCase("on"))
      {
        SFormula.g_rowExpansion = true;
      }
      else
      {
        SFormula.g_rowExpansion = false;
      }

      // "submit" shall return to preivous browsing page.
      if ( isBack!=null)
      {
        String backUrl = (String)session.getAttribute("back_url");
        if ( backUrl == null )
        {
          response.sendRedirect("home.jsp");
        }
        else
        {
          response.sendRedirect(backUrl);
        }
      }
      else
      {
        response.sendRedirect("home.jsp");
      }
      return;
   }
%>
<FORM method="POST">
    <H3>Presentation</H3>
    <P>
    <label for="stdPrf">
      <INPUT type="radio" name="stdPrf" value="<%=RsUtil.RAW_FORMAT%>" <%= RsUtil.g_proofFormat.equalsIgnoreCase(RsUtil.RAW_FORMAT) ? "checked" : "" %> >
      Show raw proof steps
      <INPUT type="radio" name="stdPrf" value="<%=RsUtil.STANDARD_FORMAT%>" <%= RsUtil.g_proofFormat.equalsIgnoreCase(RsUtil.STANDARD_FORMAT) ? "checked" : "" %> >
      Show standard proof steps
    </label>
    <P>
    <label for="engFormat">
    <INPUT type="checkbox" name="engFormat" id="engFormat" <%= SigmaUtil.g_isEngFormat? "checked" : "" %> >
    Create English paraphrasing wherever possible</label>
    <P>
    <label for="prettyPrint">
    <INPUT type="checkbox" name="prettyPrint"  id="prettyPrint" <%= SigmaUtil.g_isPrettyPrint ? "checked" : "" %> >
    Pretty-print assertions on constant display pages</label>
    <P>
    <label for="hyperlinkStrings">
    <INPUT type="checkbox" name="hyperLink"  id="hyperlinkStrings"<%= SigmaUtil.g_isHyperLink ? "checked" : "" %> >
    Hyper-link constant names within strings</label>
    <P>
    <label for="answerSkip">
    <INPUT type="checkbox" name="answerSkip"  id="answerSkip"<%= RsUtil.g_removeAnswerTag ? "checked" : "" %> >
    Substitue $Answer with the actual binding. </label>
    <P>
    <label for="tickWanted">
    <INPUT type="checkbox" name="tickWanted"  id="tickWanted"<%= SFormula.g_tickWanted ? "checked" : "" %> >
    Add tick for every embedded formula</label>
    <P>
    <label for="holdExpansion">
    <INPUT type="checkbox" name="holdExpansion"  id="holdExpansion"<%= SFormula.g_holdExpansion ? "checked" : "" %> >
    Adding "hold" to every non-logical, non-quantifier expression</label>
    <P>
    <label for="rowExpansion">
    <INPUT type="checkbox" name="rowExpansion"  id="rowExpansion"<%= SFormula.g_rowExpansion ? "checked" : "" %> >
    Row variable macro expansion</label>
    <H3>Content</H3>
    <INPUT type="submit" name="change" value="change">
</FORM>

<br><table ALIGN="LEFT" WIDTH=80%><tr><TD BGCOLOR='#AAAAAA'><IMG SRC='pixmaps/1pixel.gif' width=1 height=1 border=0></TD></tr></table><BR>
<small><i>&copy;2003 Teknowledge Corporation, All rights reserved</i></small>
</BODY>
</HTML>

