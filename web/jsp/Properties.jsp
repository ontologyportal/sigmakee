<%@ include    file="Prelude.jsp" %>

<%
if (!role.equalsIgnoreCase("admin"))
    response.sendRedirect("KBs.jsp");
%>
<HTML>
<HEAD>
<TITLE>Sigma Knowledge Engineering Environment</TITLE>
</HEAD>
<BODY BGCOLOR=#FFFFFF>

    <%
        String pageName = "Preferences";
        String pageString = "Preferences";
    %>
    <%@include file="CommonHeader.jsp" %>

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
August 9, Acapulco, Mexico.  See also http://github.com/ontologyportal
*/
  boolean changed = false;
  boolean reload = false;
  String inferenceEngine = request.getParameter("inferenceEngine");
  String iePref = KBmanager.getMgr().getPref("inferenceEngine");
  if (iePref == null) iePref = "";
  if (inferenceEngine != null) {
      if (!inferenceEngine.equals(iePref)) {
          changed = true;
          reload = true;
          KBmanager.getMgr().setPref("inferenceEngine",inferenceEngine);
      }
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
      reload = true;
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
      reload = true;
      KBmanager.getMgr().setPref("holdsPrefix",holdsPrefix);
  }
  else {
      holdsPrefix = KBmanager.getMgr().getPref("holdsPrefix");
      if (holdsPrefix == null)
          holdsPrefix = "no";
  }

  String overwrite = request.getParameter("overwrite");
  if (StringUtil.isNonEmptyString(overwrite)) {
      changed = true;
      KBmanager.getMgr().setPref("overwrite",overwrite);
  }
  else {
      overwrite = KBmanager.getMgr().getPref("overwrite");
      if (StringUtil.emptyString(overwrite)
          || !overwrite.equalsIgnoreCase("yes"))
          overwrite = "no";
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
      reload = true;
      KBmanager.getMgr().setPref("TPTP",TPTP);
  }
  else {
      TPTP = KBmanager.getMgr().getPref("TPTP");
      if (TPTP == null)
          TPTP = "yes";
  }

  String testOutputDir = request.getParameter("testOutputDir");
  if (testOutputDir != null) {
        changed = true;
        KBmanager.getMgr().setPref("testOutputDir", testOutputDir);
  }
  else {
      testOutputDir = KBmanager.getMgr().getPref("testOutputDir");
      if (testOutputDir == null)
          testOutputDir = "";
  }

  String TPTPDisplay = request.getParameter("TPTPDisplay");
  if (TPTPDisplay != null) {
      changed = true;
      KBmanager.getMgr().setPref("TPTPDisplay",TPTPDisplay);
  }
  else {
      TPTP = KBmanager.getMgr().getPref("TPTP");
      if (TPTP == null)
          TPTP = "yes";
  }

  String typePrefix = request.getParameter("typePrefix");
  if (typePrefix != null) {
      changed = true;
      reload = true;
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

  String systemsDir = request.getParameter("systemsDir");
  if (systemsDir != null) {
      changed = true;
      KBmanager.getMgr().setPref("systemsDir",systemsDir);
  }
  else {
      systemsDir = KBmanager.getMgr().getPref("systemsDir");
      if (systemsDir == null)
          systemsDir = "";
  }

  String graphDir = request.getParameter("graphDir");
  if (graphDir != null) {
      changed = true;
      KBmanager.getMgr().setPref("graphDir",graphDir);
  }
  else {
      graphDir = KBmanager.getMgr().getPref("graphDir");
      if (graphDir == null)
          graphDir = "";
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

  String adminBrowserLimit = request.getParameter("adminBrowserLimit");
  if (adminBrowserLimit != null) {
      changed = true;
      KBmanager.getMgr().setPref("adminBrowserLimit",adminBrowserLimit);
  }
  else {
      adminBrowserLimit = KBmanager.getMgr().getPref("adminBrowserLimit");
      if (adminBrowserLimit == null)
          adminBrowserLimit = "200";
  }
  String userBrowserLimit = request.getParameter("userBrowserLimit");
  if (userBrowserLimit != null) {
      changed = true;
      KBmanager.getMgr().setPref("userBrowserLimit",userBrowserLimit);
  }
  else {
      userBrowserLimit = KBmanager.getMgr().getPref("userBrowserLimit");
      if (userBrowserLimit == null)
          userBrowserLimit = "25";
  }

  String logDir = request.getParameter("logDir");
  if (logDir != null) {
      changed = true;
      KBmanager.getMgr().setPref("logDir", logDir);
  }
  else {
      logDir = KBmanager.getMgr().getPref("logDir");
      if (logDir == null)
          logDir = "";
  }

  String logLevel = request.getParameter("logLevel");
  if (logLevel != null) {
      changed = true;
      KBmanager.getMgr().setPref("logLevel", logLevel);
  }
  else {
      logLevel = KBmanager.getMgr().getPref("logLevel");
      if (logLevel == null)
          logLevel = "warning";
  }

  if (changed == true)
      KBmanager.getMgr().writeConfiguration();

  // Force retranslation and new inference engine creation if inference
  // parameters have changed.

  Set<String> kbNames = KBmanager.getMgr().getKBnames();
  Iterator<String> it = null;
  if ((kbNames != null) && !(kbNames.isEmpty())) {
      if (reload) {
          it = kbNames.iterator();
          while (it.hasNext()) {
              String akbName = (String) it.next();
              KB akb = KBmanager.getMgr().getKB(akbName);
              if (akb != null) {
                  System.out.println("INFO in Properties.jsp: reloading the entire KB");
                  akb.reload();
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

    <label for="testOutputDir">
    <INPUT type="text" SIZE=50 name="testOutputDir" value=<%=testOutputDir %> >
    Directory in which consistency check output can be found</label><P>

    <label for="tptpHomeDir">
    <INPUT type="text" SIZE=50 name="tptpHomeDir" value=<%=tptpHomeDir %> >
    Directory in which local copy of TPTPWorld is installed</label><P>

    <label for="graphDir">
    <INPUT type="text" SIZE=50 name="graphDir" value=<%=graphDir %> >
    Directory in which dot graphs will be saved</label><P>

    <label for="systemsDir">
    <INPUT type="text" SIZE=50 name="systemsDir" value=<%=systemsDir %> >
    Directory in which built in ATP systems are located</label><P>

    <label for="editorCommand">
    <INPUT type="text" name="editorCommand" value=<%=editorCommand %> >
    Command to invoke text editor</label><P>

    <label for="lineNumberCommand">
    <INPUT type="text" name="lineNumberCommand" value=<%=lineNumberCommand %> >
    Command line option for text editor to set cursor at a particular line</label><P>

    <label for="logDir">
    <INPUT type="text" SIZE=50 name="logDir" value=<%=logDir %> >
    Directory where log files are saved.</label><P>

    <label for="logLevel">
    <INPUT type="text" name="logLevel" value=<%=logLevel %> >
    Level of messages to be logged (can be severe, warning, info, config, finest) </label><P>

    <label for="overwrite">
    <INPUT type="radio" name="overwrite" value="yes" <%  // default is no
    overwrite = mgr.getPref("overwrite");
    if (StringUtil.isNonEmptyString(overwrite) &&
        overwrite.equalsIgnoreCase("yes"))
        out.print("checked=yes");
    %> > yes
    <INPUT type="radio" name="overwrite" value="no" <%
    if (StringUtil.emptyString(overwrite) ||
            overwrite.equalsIgnoreCase("no"))
            out.print("checked=no"); %> > no
      : Overwrite files of the same name when creating, copying, and loading KB constituents</label><P>

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

    <li>
        <label for="TPTPDisplay">
        <INPUT type="radio" name="TPTPDisplay" value="yes" <%   // default to no TPTPDisplay
        if (KBmanager.getMgr().getPref("TPTPDisplay") != null &&
            KBmanager.getMgr().getPref("TPTPDisplay").equalsIgnoreCase("yes"))
            out.print("checked=no");
        %> > yes</input>
        <INPUT type="radio" name="TPTPDisplay" value="no" <%
        if (KBmanager.getMgr().getPref("TPTPDisplay") == null ||
            KBmanager.getMgr().getPref("TPTPDisplay").equalsIgnoreCase("no"))
            out.print("checked=yes");
        %> > no
        : Display TPTP translation</label><P>
    </li>
      </ol>

  <INPUT type="submit" value="Save"> <b>Some options require a restart of Tomcat and Sigma.  Changing a translation option will force the KB to be reloaded.</b>
</FORM>

<br><table ALIGN="LEFT" WIDTH=80%><tr><TD BGCOLOR='#AAAAAA'><IMG SRC='pixmaps/1pixel.gif' width=1 height=1 border=0></TD></tr></table><BR>

<%@ include file="Postlude.jsp" %>
</BODY>
</HTML>

