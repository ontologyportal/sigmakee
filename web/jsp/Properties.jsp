<%@ include	file="Prelude.jsp" %>

<% 
if (!KBmanager.getMgr().getPref("userName").equalsIgnoreCase("admin"))         
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

boolean ieChanged = false;
  String inferenceEngine = request.getParameter("inferenceEngine");
String iePref = KBmanager.getMgr().getPref("inferenceEngine");
if ( iePref == null ) { iePref = ""; }
  if (inferenceEngine != null) {
      if ( ! inferenceEngine.equals(iePref) ) {
	  changed = true;
	  ieChanged = true;
	  KBmanager.getMgr().setPref("inferenceEngine",inferenceEngine);
      }
  }
  else {
      inferenceEngine = KBmanager.getMgr().getPref("inferenceEngine");
      if (inferenceEngine == null) {
          inferenceEngine = "";
      }
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
          hostname = "localhost";
  }

  String port = request.getParameter("port");
  if (port != null) {
      changed = true;
      KBmanager.getMgr().setPref("port",port);
  }
  else {
      port = KBmanager.getMgr().getPref("port");
      if (port == null)
          port = "8080";
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

  String holdsPrefix = request.getParameter("holdsPrefix");
  if (holdsPrefix != null) {
      changed = true;
      KBmanager.getMgr().setPref("holdsPrefix",holdsPrefix);
  }
  else {
      holdsPrefix = KBmanager.getMgr().getPref("holdsPrefix");
      if (holdsPrefix == null)
          holdsPrefix = "no";
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

  String TPTP = request.getParameter("TPTP");
  if (TPTP != null) {
      changed = true;
      KBmanager.getMgr().setPref("TPTP",TPTP);
  }
  else {
      TPTP = KBmanager.getMgr().getPref("TPTP");
      if (TPTP == null)
          TPTP = "yes";
  }

  String typePrefix = request.getParameter("typePrefix");
  if (typePrefix != null) {
      changed = true;
      KBmanager.getMgr().setPref("typePrefix",typePrefix);
  }
  else {
      typePrefix = KBmanager.getMgr().getPref("typePrefix");
      if (typePrefix == null)
          typePrefix = "yes";
  }

  String inferenceTestDir = request.getParameter("inferenceTestDir");
  if (inferenceTestDir != null) {
      changed = true;
      KBmanager.getMgr().setPref("inferenceTestDir",inferenceTestDir);
  }
  else {
      inferenceTestDir = KBmanager.getMgr().getPref("inferenceTestDir");
      if (inferenceTestDir == null)
          inferenceTestDir = "";
  }

  String tptpHomeDir = request.getParameter("tptpHomeDir");
  if (tptpHomeDir != null) {
      changed = true;
      KBmanager.getMgr().setPref("tptpHomeDir",tptpHomeDir);
  }
  else {
      tptpHomeDir = KBmanager.getMgr().getPref("tptpHomeDir");
      if (tptpHomeDir == null)
          tptpHomeDir = "";
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

if (changed == true) {
      KBmanager.getMgr().writeConfiguration();
}

// Force retranslation and new inference engine creation if inference
// parameters have changed.
int oldBitVal = KBmanager.getMgr().getOldInferenceBitValue();
int newBitVal = KBmanager.getMgr().getInferenceBitValue();
boolean bitsChanged = false;
Set kbNames = KBmanager.getMgr().getKBnames();
String kbName = null;
KB kb = null;
Iterator it = null;
if ( (kbNames != null) && !(kbNames.isEmpty()) ) {
    if ( oldBitVal != newBitVal ) {
	if ( oldBitVal != -1 ) {
	    bitsChanged = true;
	}
	KBmanager.getMgr().setOldInferenceBitValue( newBitVal );
    }
    if ( ieChanged || bitsChanged ) {
	boolean useTypePrefixChanged = ((oldBitVal & KBmanager.USE_TYPE_PREFIX)
					!= (newBitVal & KBmanager.USE_TYPE_PREFIX));
	boolean useHoldsPrefixChanged = ((oldBitVal & KBmanager.USE_HOLDS_PREFIX)
					 != (newBitVal & KBmanager.USE_HOLDS_PREFIX));
	boolean useCacheChanged = ((oldBitVal & KBmanager.USE_CACHE)
				   != (newBitVal & KBmanager.USE_CACHE));
	boolean useTptpTurnedOn = ((oldBitVal & KBmanager.USE_TPTP)
				   < (newBitVal & KBmanager.USE_TPTP));
	it = kbNames.iterator();
	while ( it.hasNext() ) {
	    kbName = (String) it.next();
	    kb = KBmanager.getMgr().getKB( kbName );
	    if ( kb != null ) {
		boolean callReload = false;
		boolean callLoadVampire = false;
		boolean callCache = false;
		boolean callTptpParse = false;
		if ( useCacheChanged ) {
		    callLoadVampire = true;
		    int oldCacheSetting = (oldBitVal & KBmanager.USE_CACHE);
		    int newCacheSetting = (newBitVal & KBmanager.USE_CACHE);
		    boolean cachingTurnedOn = (newCacheSetting > oldCacheSetting);
		    if ( cachingTurnedOn ) {
			callCache = true;
		    }
		    // We know the cache setting has changed, so
		    // if caching has not been newly set to "yes",
		    // it must have been newly set to "no", and we
		    // should reload everything.
		    else {
			callReload = true;
		    }
		}
		if ( ieChanged || useTypePrefixChanged || useHoldsPrefixChanged ) {
		    callLoadVampire = true;
		}

		if ( callReload ) {
		    System.out.println( "INFO in Properties.jsp: reloading the entire KB" );
		    kb.reload();
		}
		else if ( callCache ) {
		    System.out.println( "INFO in Properties.jsp: writing the cache file and reloading Vampire" );
		    kb.cache();
		    kb.loadVampire();
		}
		else if ( callLoadVampire ) {
		    System.out.println( "INFO in Properties.jsp: reloading Vampire" );
		    kb.loadVampire();
		}
		else if ( useTptpTurnedOn ) {
		    // If we reach this point, the only action
		    // required is that a TPTP translation be
		    // generated and stored in the
		    // Formula.theTPTPFormula field for each
		    // formula.
		    System.out.println( "INFO in Properties.jsp: generating TPTP for all formulas" );
		    kb.tptpParse();
		}
	    }
	}
    }
}

%>

<FORM method="POST" ACTION="Properties.jsp">
    <label for="inferenceEngine">
    <INPUT type="text" SIZE=50 name="inferenceEngine" value=<%=inferenceEngine %> >
    Fully qualified path and name of the inference engine</label><P>

    <label for="celtdir">
    <INPUT type="text" SIZE=50 name="celtdir" value=<%=celtdir %> >
    Directory in which the CELT system is located</label><P>

    <label for="prolog">
    <INPUT type="text" SIZE=50 name="prolog" value=<%=prolog %> >
    Fully qualified path and name of SWI prolog</label><P>

    <label for="hostname">
    <INPUT type="text" name="hostname" value=<%=hostname %> >
    DNS address of the computer on which Sigma is hosted</label><P>

    <label for="port">
    <INPUT type="text" name="port" value=<%=port %> >
    Port number on which Tomcat responds</label><P>

    <label for="sumokbname">
    <INPUT type="text" name="sumokbname" value=<%=sumokbname %> >
    Name of the SUMO KB in Sigma</label><P>

    <label for="inferenceTestDir">
    <INPUT type="text" SIZE=50 name="inferenceTestDir" value=<%=inferenceTestDir %> >
    Directory in which tests for the inference engine are found</label><P>

    <label for="tptpHomeDir">
    <INPUT type="text" SIZE=50 name="tptpHomeDir" value=<%=tptpHomeDir %> >
    Directory in which local copy of TPTPWorld is installed</label><P>

    <label for="editorCommand">
    <INPUT type="text" name="editorCommand" value=<%=editorCommand %> >
    Command to invoke text editor</label><P>

    <label for="lineNumberCommand">
    <INPUT type="text" name="lineNumberCommand" value=<%=lineNumberCommand %> >
    Command line option for text editor to set cursor at a particular line</label><P>

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
    : Show cached statements in the term browser</label><P>

    <label for="loadCELT">  
    <INPUT type="radio" name="loadCELT" value="yes" <%                            // default to no CELT
        if (KBmanager.getMgr().getPref("loadCELT") != null &&
            KBmanager.getMgr().getPref("loadCELT").equalsIgnoreCase("yes")) 
            out.print("checked=no"); 
        %> > yes</input> 
    <INPUT type="radio" name="loadCELT" value="no" <% 
        if (KBmanager.getMgr().getPref("loadCELT") == null ||
            KBmanager.getMgr().getPref("loadCELT").equalsIgnoreCase("no")) 
            out.print("checked=yes"); 
        %> > no
    : Load CELT at startup</label><P>


<p><strong>Formula Translation Options</strong></p>

      <ol>
	<li>
	    <label for="typePrefix">  
	    <INPUT type="radio" name="typePrefix" value="yes" <% 
		if (KBmanager.getMgr().getPref("typePrefix") != null &&
		    KBmanager.getMgr().getPref("typePrefix").equalsIgnoreCase("yes")) 
		    out.print("checked=no"); 
		%> > yes</input> 
	    <INPUT type="radio" name="typePrefix" value="no" <% 
		if (KBmanager.getMgr().getPref("typePrefix") == null ||
		    KBmanager.getMgr().getPref("typePrefix").equalsIgnoreCase("no")) 
		    out.print("checked=yes"); 
		%> > no
	    : Add a "sortal" antecedent to every axiom </label><P>
	</li>

	<li>
	    <label for="holdsPrefix">  
	    <INPUT type="radio" name="holdsPrefix" value="yes" <%
		if (KBmanager.getMgr().getPref("holdsPrefix") != null &&
		    KBmanager.getMgr().getPref("holdsPrefix").equalsIgnoreCase("yes")) 
		    out.print("checked=no"); 
		%> > yes</input> 
	    <INPUT type="radio" name="holdsPrefix" value="no" <% 
		if (KBmanager.getMgr().getPref("holdsPrefix") == null ||
		    KBmanager.getMgr().getPref("holdsPrefix").equalsIgnoreCase("no")) 
		    out.print("checked=yes"); 
		%> > no
		: Prefix all clauses with "holds" (otherwise instantiate all variables in predicate position)</label><P>
	</li>

	<li>
	    <label for="cache">  
	    <INPUT type="radio" name="cache" value="yes" <%   // default to no caching
		if (KBmanager.getMgr().getPref("cache") != null &&
		    KBmanager.getMgr().getPref("cache").equalsIgnoreCase("yes")) 
		    out.print("checked=no"); 
		%> > yes</input> 
	    <INPUT type="radio" name="cache" value="no" <% 
		if (KBmanager.getMgr().getPref("cache") == null ||
		    KBmanager.getMgr().getPref("cache").equalsIgnoreCase("no")) 
		    out.print("checked=yes"); 
		%> > no
	    : Employ statement caching</label><P>
	</li>

	<li>
	    <label for="TPTP">  
	    <INPUT type="radio" name="TPTP" value="yes" <%   // default to no TPTP
		if (KBmanager.getMgr().getPref("TPTP") != null &&
		    KBmanager.getMgr().getPref("TPTP").equalsIgnoreCase("yes")) 
		    out.print("checked=no"); 
		%> > yes</input> 
	    <INPUT type="radio" name="TPTP" value="no" <% 
		if (KBmanager.getMgr().getPref("TPTP") == null ||
		    KBmanager.getMgr().getPref("TPTP").equalsIgnoreCase("no")) 
		    out.print("checked=yes"); 
		%> > no
	    : Perform TPTP translation</label><P>
	</li>
      </ol>


  <INPUT type="submit" value="Save"> <b>Some options require a restart of Tomcat and Sigma.  Changing a translation option will force the KB to be reloaded.</b>
</FORM>

<br><table ALIGN="LEFT" WIDTH=80%><tr><TD BGCOLOR='#AAAAAA'><IMG SRC='pixmaps/1pixel.gif' width=1 height=1 border=0></TD></tr></table><BR>

<%@ include file="Postlude.jsp" %>
</BODY>
</HTML>

