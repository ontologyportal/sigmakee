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
August 9, Acapulco, Mexico.  See also http://sigmakee.sourceforge.net
*/

/** This jsp page handles listing the files which comprise a knowledge base,
    adding new constituents (files), and deleting constituents.  It redirects
    to AddConstituent.jsp to add new constituents.  The page takes several
    parameters:
    kbName - the name of the knowledge base for which the manifest is displayed.
    constituent - a constituent to be added to the KB.
    delete - a constituent to be deleted from the KB.
*/
    String kbDir = KBmanager.getMgr().getPref("kbDir");
    File kbDirFile = new File(kbDir);
    String saveAs = request.getParameter("saveAs");
    String constituent = request.getParameter("constituent");
    String saveFile = request.getParameter("saveFile");
    String delete = request.getParameter("delete");
    String result = "";

    if (KBmanager.getMgr().getPref("userRole") == null || !KBmanager.getMgr().getPref("userRole").equalsIgnoreCase("administrator")) { 
    	saveAs = null;
    	saveFile = null;
    	constituent = null;
    	delete = null;
    }

    if ((kb == null) || StringUtil.emptyString(kbName))
        response.sendRedirect("KBs.jsp");  // That KB does not exist  

    else if (StringUtil.isNonEmptyString(saveAs)) {
        if (saveAs.equalsIgnoreCase("prolog")) {
            File plFile = new File(kbDirFile, (kb.name + ".pl"));
            String pfcp = null;
            String prologFile = null;
            try {
                pfcp = plFile.getCanonicalPath();
                Prolog.kb = kb;
                prologFile = Prolog.writePrologFile(pfcp);
            }
            catch (Exception pfe) {
                pfe.printStackTrace();
            }
            result = ((StringUtil.isNonEmptyString(prologFile) && plFile.canRead())
                      ? ("Wrote the Prolog file " + prologFile)
                      : "Could not write a Prolog file");
        }
        else if (saveAs.equalsIgnoreCase("TPTP") || saveAs.equalsIgnoreCase("tptpFOL")) {
            // Force translation of the KB to TPTP, even if the user has not
            // requested this on the Preferences page.
            if (!KBmanager.getMgr().getPref("TPTP").equalsIgnoreCase("yes")) {
                System.out.println( "INFO in Manifest.jsp: generating TPTP for all formulas");
                SUMOKBtoTPTPKB skbtptpkb = new SUMOKBtoTPTPKB();
                skbtptpkb.kb = kb;
                skbtptpkb.tptpParse();
            }
            boolean onlyPlainFOL = saveAs.equalsIgnoreCase("tptpFOL");
            File tptpf = new File(kbDirFile, (saveFile + ".tptp"));
            String tptpfcp = null;
            String tptpFile = null;
            try {
                tptpfcp = tptpf.getCanonicalPath();
                SUMOKBtoTPTPKB skbtptpkb = new SUMOKBtoTPTPKB();
        		skbtptpkb.kb = kb;
                tptpFile = skbtptpkb.writeTPTPFile(tptpfcp, null, onlyPlainFOL, "");
            }
            catch (Exception tptpfe) {
                tptpfe.printStackTrace();
            }
            if (StringUtil.isNonEmptyString(tptpFile))
            	result = ("Wrote the TPTP file " + tptpFile);
           	else
  				result = "Could not write a TPTP file";
        }
        else if (saveAs.equalsIgnoreCase("OWL")) {
            OWLtranslator ot = new OWLtranslator();
            ot.kb = KBmanager.getMgr().getKB(kbName);
            File owlFile = new File(kbDirFile, (saveFile + ".owl"));
            String ofcp = null;
            try {
                ofcp = owlFile.getCanonicalPath();
                ot.writeKB(ofcp);
            }
            catch (Exception ofe) {
                ofe.printStackTrace();
            }
            result = ((StringUtil.isNonEmptyString(ofcp) && owlFile.canRead())
                      ? ("Wrote the OWL file " + ofcp)
                      : "Could not write an OWL file");
        }
        else if (saveAs.equalsIgnoreCase("KIF")) {
            File kifFile = new File(kbDirFile, (kbName + ".kif"));
            String kfcp = null;
            try {
                kfcp = kifFile.getCanonicalPath();
                kb.writeFile(kfcp);
            }
            catch (Exception kfe) {
                kfe.printStackTrace();
            }
            result = ((StringUtil.isNonEmptyString(kfcp) && kifFile.canRead())
                      ? ("Wrote the KIF file " + kfcp)
                      : "Could not write a KIF file");
        }
    }
    if (delete != null) {
        int i = kb.constituents.indexOf(constituent.intern());
        if (i == -1)
            System.out.println("Error in Manifest.jsp: No such constituent: " + constituent.intern());       
        else {
            kb.constituents.remove(i);
            KBmanager.getMgr().writeConfiguration();
        }
	    result = kb.reload();
    }
    else if (constituent != null) {
        kb.addNewConstituent(constituent);
        KBmanager.getMgr().writeConfiguration();
	    if (KBmanager.getMgr().getPref("cache").equalsIgnoreCase("yes")) 
	        kb.kbCache.cache();	    
	    kb.loadEProver();
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
  </table>
        <input type="submit" name="submit" value="Load">
    </form>

    <hr>
    <p><b>Save KB to other formats</b></p>

<% if (StringUtil.isNonEmptyString(result)) {
       out.println("<p>");
       out.println(result); 
       out.println("</p>");
       result = "";
   } %>

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
        </select>
    </FORM>

<% } 
  HTMLformatter.kbHref = "http://" + hostname + ":" + port + "/sigma/Browse.jsp?";
  String er = KBmanager.getMgr().getError();
  if (!kb.errors.isEmpty()) {
      out.println("<br/><b>Errors in KB " + kb.name + "</b><br>\n");
      out.println(HTMLformatter.formatErrors(kb,HTMLformatter.kbHref + 
                  "language=" + language + "&kb=" + kb.name));  
  }
  
  if (StringUtil.isNonEmptyString(er)) 
      out.println(er);    
  else
      if (StringUtil.isNonEmptyString(constituent) && StringUtil.emptyString(delete))
          out.println("File " + constituent + " loaded successfully.");
%>
<P>
  <a href="KBs.jsp">Return to home page</a><p>
<%@ include file="Postlude.jsp" %>
</BODY>
</HTML>
