<%@ include	file="Prelude.jsp" %>

<% if (!KBmanager.getMgr().getPref("userName").equalsIgnoreCase("admin"))         
       response.sendRedirect("KBs.jsp");     
%>
<HTML>
<HEAD>
<TITLE>Sigma Knowledge Engineering Environment</TITLE>
</HEAD>
<BODY BGCOLOR=#FFFFFF>
<table width=95% cellspacing=0 cellpadding=0>
  <tr>
    <td valign="top">
      <table cellspacing=0 cellpadding=0>
        <tr>
          <td align="left" valign="top"><img src="pixmaps/sigmaSymbol-gray.gif"></td>
          <td>&nbsp;</td>
          <td align="left" valign="top"><img src="pixmaps/logoText-gray.gif"></td>
        </tr>
      </table>
    </td>
    <td valign="bottom"></td>
    <td><font face="Arial,helvetica" SIZE=-1><b>[ <A href="KBs.jsp">Home</A> ]</b></FONT></td>
  </tr>
</table>
<table ALIGN="LEFT" WIDTH=80%><tr><TD BGCOLOR='#AAAAAA'><IMG SRC='pixmaps/1pixel.gif' width=1 height=1 border=0></TD></tr></table><BR>

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
  boolean changed = false;
  String inferenceEngine = request.getParameter("inferenceEngine");
  if (inferenceEngine != null) {
      changed = true;
      KBmanager.getMgr().setPref("inferenceEngine",inferenceEngine);
  }
  else {
      inferenceEngine = KBmanager.getMgr().getPref("inferenceEngine");
      if (inferenceEngine == null)
          inferenceEngine = "";
  }

  String celtdir = request.getParameter("celtdir");
  if (celtdir != null) {
      changed = true;
      KBmanager.getMgr().setPref("celtdir",celtdir);
  }
  else {
      celtdir = KBmanager.getMgr().getPref("celtdir");
      if (celtdir == null)
          celtdir = "";
  }

  String prolog = request.getParameter("prolog");
  if (prolog != null) {
      changed = true;
      KBmanager.getMgr().setPref("prolog",prolog);
  }
  else {
      prolog = KBmanager.getMgr().getPref("prolog");
      if (prolog == null)
          prolog = "";
  }

  String hostname = request.getParameter("hostname");
  if (hostname != null) {
      changed = true;
      KBmanager.getMgr().setPref("hostname",hostname);
  }
  else {
      hostname = KBmanager.getMgr().getPref("hostname");
      if (hostname == null)
          hostname = "";
  }

  String sumokbname = request.getParameter("sumokbname");
  if (sumokbname != null) {
      changed = true;
      KBmanager.getMgr().setPref("sumokbname",sumokbname);
  }
  else {
      sumokbname = KBmanager.getMgr().getPref("sumokbname");
      if (sumokbname == null)
          sumokbname = "";
  }

  String cache = request.getParameter("cache");
  if (cache != null) {
      changed = true;
      KBmanager.getMgr().setPref("cache",cache);
  }
  else {
      cache = KBmanager.getMgr().getPref("cache");
      if (cache == null)
          cache = "no";
  }

  String showcached = request.getParameter("showcached");
  if (showcached != null) {
      changed = true;
      KBmanager.getMgr().setPref("showcached",showcached);
  }
  else {
      showcached = KBmanager.getMgr().getPref("showcached");
      if (showcached == null)
          showcached = "yes";
  }
  
  String loadCELT = request.getParameter("loadCELT");
  if (loadCELT != null) {
      changed = true;
      KBmanager.getMgr().setPref("loadCELT",loadCELT);
  }
  else {
      loadCELT = KBmanager.getMgr().getPref("loadCELT");
      if (loadCELT == null)
          loadCELT = "yes";
  }

  String inferenceTestDir = request.getParameter("inferenceTestDir");
  if (inferenceTestDir != null) {
      changed = true;
      KBmanager.getMgr().setPref("inferenceTestDir",inferenceTestDir);
  }
  else {
      inferenceTestDir = KBmanager.getMgr().getPref("inferenceTestDir");
      if (inferenceTestDir == null)
          inferenceTestDir = "yes";
  }

  String editorCommand = request.getParameter("editorCommand");
  if (editorCommand != null) {
      changed = true;
      KBmanager.getMgr().setPref("editorCommand",editorCommand);
  }
  else {
      editorCommand = KBmanager.getMgr().getPref("editorCommand");
      if (editorCommand == null)
          editorCommand = "";
  }


  String lineNumberCommand = request.getParameter("lineNumberCommand");
  if (lineNumberCommand != null) {
      changed = true;
      KBmanager.getMgr().setPref("lineNumberCommand",lineNumberCommand);
  }
  else {
      lineNumberCommand = KBmanager.getMgr().getPref("lineNumberCommand");
      if (lineNumberCommand == null)
          lineNumberCommand = "";
  }

  if (changed == true)
      KBmanager.getMgr().writeConfiguration();
%>

<FORM method="POST" ACTION="Properties.jsp">
    <label for="inferenceEngine">
    <INPUT type="text" name="inferenceEngine" value=<%=inferenceEngine %> >
    Fully qualified path and name of the inference engine</label><P>

    <label for="celtdir">
    <INPUT type="text" name="celtdir" value=<%=celtdir %> >
    Directory in which the CELT system is located</label><P>

    <label for="prolog">
    <INPUT type="text" name="prolog" value=<%=prolog %> >
    Fully qualified path and name of SWI prolog</label><P>

    <label for="hostname">
    <INPUT type="text" name="hostname" value=<%=hostname %> >
    DNS address of the computer on which Sigma is hosted</label><P>

    <label for="sumokbname">
    <INPUT type="text" name="sumokbname" value=<%=sumokbname %> >
    Name of the SUMO KB in Sigma</label><P>

    <label for="inferenceTestDir">
    <INPUT type="text" name="inferenceTestDir" value=<%=inferenceTestDir %> >
    Directory in which tests for the inference engine are found</label><P>

    <label for="editorCommand">
    <INPUT type="text" name="editorCommand" value=<%=editorCommand %> >
    Command to invoke text editor</label><P>

    <label for="lineNumberCommand">
    <INPUT type="text" name="lineNumberCommand" value=<%=lineNumberCommand %> >
    Command line option for text editor to set cursor at a particular line</label><P>

    <label for="cache">  
    <INPUT type="radio" name="cache" value="yes" <%                            // default to no caching
        if (KBmanager.getMgr().getPref("cache") != null &&
            KBmanager.getMgr().getPref("cache").equalsIgnoreCase("yes")) 
            out.print("checked=no"); 
        %> > yes</input> 
    <INPUT type="radio" name="cache" value="no" <% 
        if (KBmanager.getMgr().getPref("cache") == null ||
            KBmanager.getMgr().getPref("cache").equalsIgnoreCase("no")) 
            out.print("checked=yes"); 
        %> > no
    : Should caching be employed</label><P>

    <label for="showcached">                                                   
    <INPUT type="radio" name="showcached" value="yes" <%                       // default to showing cached statements
        if (KBmanager.getMgr().getPref("showcached") == null ||
            KBmanager.getMgr().getPref("showcached").equalsIgnoreCase("yes")) 
            out.print("checked=yes"); 
        %> > yes
    <INPUT type="radio" name="showcached" value="no" <% 
        if (KBmanager.getMgr().getPref("showcached") != null &&
            KBmanager.getMgr().getPref("showcached").equalsIgnoreCase("no")) 
            out.print("checked=no"); %> > no
    : Should cached statements be shown in the term browser</label><P>


    <label for="loadCELT">  
    <INPUT type="radio" name="loadCELT" value="yes" <%                            // default to no caching
        if (KBmanager.getMgr().getPref("loadCELT") != null &&
            KBmanager.getMgr().getPref("loadCELT").equalsIgnoreCase("yes")) 
            out.print("checked=no"); 
        %> > yes</input> 
    <INPUT type="radio" name="loadCELT" value="no" <% 
        if (KBmanager.getMgr().getPref("loadCELT") == null ||
            KBmanager.getMgr().getPref("loadCELT").equalsIgnoreCase("no")) 
            out.print("checked=yes"); 
        %> > no
    : Should CELT be loaded at startup</label><P>

    <INPUT type="submit" name="submit">
</FORM>

<br><table ALIGN="LEFT" WIDTH=80%><tr><TD BGCOLOR='#AAAAAA'><IMG SRC='pixmaps/1pixel.gif' width=1 height=1 border=0></TD></tr></table><BR>
</BODY>
</HTML>

