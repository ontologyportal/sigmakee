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
August 9, Acapulco, Mexico.
*/

/** This jsp page handles listing the files which comprise a knowledge base,
    adding new constituents (files), and deleting constituents.  It redirects
    to AddConstituent.jsp to add new constituents.  The page takes several
    parameters:
    kbName - the name of the knowledge base for which the manifest is displayed.
    constituent - a constituent to be added to the KB.
    delete - a constituent to be deleted from the KB.
*/

    String kbName = request.getParameter("kb");
    String kbDir = KBmanager.getMgr().getPref("kbDir");
    File kbDirFile = new File(kbDir);
    String saveAs = request.getParameter("saveAs");
    String constituent = request.getParameter("constituent");
    String saveFile = request.getParameter("saveFile");
    String delete = request.getParameter("delete");
    String result = "";
    KB kb = KBmanager.getMgr().getKB(kbName);

    if ((kb == null) || StringUtil.emptyString(kbName))
        response.sendRedirect("KBs.jsp");  // That KB does not exist  

    if (saveAs != null && saveAs.equals("prolog")) {
        File plFile = new File(kbDirFile, (kb.name + ".pl"));
        String prologFile = kb.writePrologFile(plFile.getCanonicalPath());
        String statusStr = ( "\n<br/>Wrote file " + prologFile + "\n<br/>" );
        if (!StringUtil.isNonEmptyString(prologFile))
            statusStr = "\n<br/>Could not write a Prolog file\n<br/>";
        KBmanager.getMgr().setError(KBmanager.getMgr().getError() + statusStr);
    }

    if (StringUtil.isNonEmptyString(saveAs) 
        && (saveAs.equalsIgnoreCase("TPTP") || saveAs.equalsIgnoreCase("tptpFOL"))) {
        // Force translation of the KB to TPTP, even if the user has not
        // requested this on the Preferences page.
    	if (!KBmanager.getMgr().getPref("TPTP").equalsIgnoreCase("yes")) {
    	    System.out.println( "INFO in Manifest.jsp: generating TPTP for all formulas");
    	    kb.tptpParse();
    	}
    	boolean onlyPlainFOL = saveAs.equalsIgnoreCase("tptpFOL");
        File tptpf = new File(kbDirFile, (saveFile + ".tptp"));
        String tptpFile = kb.writeTPTPFile(tptpf.getCanonicalPath(), null, onlyPlainFOL, "");
    	String statusStr = ("\n<br/>Wrote file " + tptpFile + "\n<br/>");
    	if (StringUtil.emptyString(tptpFile)) 
    	    statusStr = "\n<br/>Could not write a TPTP file\n<br/>";
        KBmanager.getMgr().setError(KBmanager.getMgr().getError() + statusStr);
    }

    if (StringUtil.isNonEmptyString(saveAs) && saveAs.equalsIgnoreCase("Turtle")) {
        System.out.println("INFO in Manifest.jsp: generating Turtle file");

        String fname = saveFile;
        if (StringUtil.emptyString(fname)) 
            fname = kb.name;
        String ttldir = KBmanager.getMgr().getPref("kbDir");
        File ttlDirFile = new File(ttldir);
        File ttlFile = new File(ttlDirFile, (fname + ".ttl"));
        String ttlCanonicalPath = ttlFile.getCanonicalPath();
        String ttlResult = DocGen.writeTurtleFile(kb, ttlFile);
        String ttlMsg = "";
    	if (StringUtil.emptyString(ttlResult) || !ttlFile.canRead()) { 
    	    ttlMsg = ("\n<br/>Could not write the Turtle file " 
                      + ttlCanonicalPath
                      + "\n<br/>");
        }
        else {
    	    ttlMsg = ("\n<br/>Wrote the Turtle file " 
                      + ttlCanonicalPath
                      + "\n<br/>");
        }
        KBmanager.getMgr().setError(KBmanager.getMgr().getError() + ttlMsg);
    }

    if (StringUtil.isNonEmptyString(saveAs) && saveAs.equals("OWL")) {
        OWLtranslator owt = new OWLtranslator();
        File owlFile = new File(kbDirFile, saveFile);
        owt.write(kbName, owlFile.getCanonicalPath());
    }

    if (saveAs != null && saveAs.equals("KIF")) {
        File kifFile = new File(kbDirFile, (kbName + ".kif"));
        kb.writeFile(kifFile.getCanonicalPath());
    }

    if (delete != null) {
        int i = kb.constituents.indexOf(constituent.intern());
        if (i == -1)
            System.out.println("Error in Manifest.jsp: No such constituent: " + constituent.intern());       
        else {
            kb.constituents.remove(i);
            KBmanager.getMgr().writeConfiguration();
        }
        System.out.println("INFO in Manifest.jsp");
        System.out.println("  > kb.constituents 81 == " + kb.constituents);
        System.out.println("  > call: kb.reload()");
	    result = kb.reload();
        System.out.println("  > kb.constituents 84 == " + kb.constituents);
    }
    else if (constituent != null) {
            System.out.println("  > kb.constituents 87 == " + kb.constituents);
        kb.addConstituent(constituent, true, false);
            System.out.println("  > kb.constituents 89 == " + kb.constituents);
        //System.out.println("INFO in Manifest.jsp (top): The error string is : " + KBmanager.getMgr().getError());
        KBmanager.getMgr().writeConfiguration();
            System.out.println("INFO in Manifest.jsp");
            System.out.println("  > kb.constituents 93 == " + kb.constituents);
	    if (KBmanager.getMgr().getPref("cache").equalsIgnoreCase("yes")) {
            System.out.println("INFO in Manifest.jsp");
            System.out.println("  > kb.constituents 96 == " + kb.constituents);
            System.out.println("  > call: kb.cache()");
	        kb.cache();
	    }
	    kb.loadVampire();
    }
%>
<HTML>
<HEAD>
<TITLE>Sigma Knowledge Engineering Environment - Constituents of <%=kbName %></TITLE>
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
    <td>
    <span class="navlinks">
        <b>[&nbsp;<a href="KBs.jsp">Home</a>&nbsp;]</b>
    </span>
    </td>
  </tr>
</table>
<table ALIGN="LEFT" WIDTH=80%><tr><TD BGCOLOR='#AAAAAA'><IMG SRC='pixmaps/1pixel.gif' width=1 height=1 border=0></TD></tr></table><BR>

<b>Files which are the <I>constituents</I> of the <B><%=kbName %></b> knowledge base </b>
<%
  if (kb.constituents == null || kb.constituents.size() <= 0) {
      %>
        <H3>No source files have been added to this knowledge base.</H3>
        <P>
        <%
  }
  else {
%>
      <P>
      <TABLE border="0" cellspacing="2" cellpadding="2">
        <Td>File Name</Td>
        <Td>Operations</Td>
<%
        for (int i = 0; i < kb.constituents.size(); i++) {
            String aConstituent = (String) kb.constituents.get(i);
%>
          <TR VALIGN="center" <%= (i % 2)==0? "bgcolor=#eeeeee":""%> >
          <TD><%=aConstituent%>&nbsp;</TD>
          <TD>

          <% if (KBmanager.getMgr().getPref("userRole") != null && KBmanager.getMgr().getPref("userRole").equalsIgnoreCase("administrator")) { %>
                <A href="Manifest.jsp?delete=true&constituent=<%=aConstituent%>&kb=<%=kbName%>">Remove</A>            
          <%     } %>
          </TD>
          </TR>
<%
        }  // for
      %>
      </TABLE>
      <BR>
<%
  }   // if
%>
<P>

<% if (KBmanager.getMgr().getPref("userRole") != null && KBmanager.getMgr().getPref("userRole").equalsIgnoreCase("administrator")) { %>
    <hr><b>Add a new constituent</b>
    <form name="kbUploader" id="kbUploader" action="AddConstituent.jsp" method="POST" enctype="multipart/form-data">
        <input type="hidden" name="kb" value=<%=kbName%>><br> 
  <table>
    <tr>
      <td>
        <b>KB Constituent:</b>&nbsp;
      </td>
      <td>
        <input type="file" name="constituent">
      </td>
    </tr>
    <tr>
      <td>&nbsp;</td>
      <td>
        <input type="checkbox" name="overwrite" value="yes">&nbsp;Replace existing file
      </td>
    </tr>
  </table>
        <input type="submit" name="submit" value="Load">
    </form>

    <hr><B>Save KB to other formats</B>
    <FORM name=save ID=save action="Manifest.jsp" method="GET">
        <INPUT type="hidden" name="kb" value=<%=kbName%>><br> 
        <B>Filename:</B>&nbsp;<INPUT type="text" name="saveFile" value=<%=kbName%>><BR>
        <INPUT type="submit" NAME="submit" VALUE="Save">
        <select name="saveAs">
            <option value="OWL">OWL
            <option value="prolog">Prolog
            <option value="KIF">KIF
            <option value="TPTP">TPTP
            <option value="tptpFOL">TPTP FOL
            <option value="Turtle">Turtle
        </select>
    </FORM>

<% } 

  String er = KBmanager.getMgr().getError();
  //out.println("INFO in Manifest.jsp: Error string is : " + er);
  if (StringUtil.isNonEmptyString(er)) {
      out.println(er);  
      //System.out.println(er);
      KBmanager.getMgr().setError("");
  }
  else
      if (StringUtil.isNonEmptyString(constituent) && StringUtil.emptyString(delete))
          out.println("File " + constituent + " loaded successfully.");
  if (StringUtil.isNonEmptyString(result))
      out.println(result);

%>

<P>
  <a href="KBs.jsp" >Return to home page</a><p>

<%@ include file="Postlude.jsp" %>
</BODY>
</HTML>
