<%@ include file="fragments/universal/Prelude.jspf" %>

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
August 9, Acapulco, Mexico.  See also https://github.com/ontologyportal/sigmakee
*/

/** This jsp page handles listing the files which comprise a knowledge base,
    adding new constituents (files), and deleting constituents.  It redirects
    to AddConstituent.jsp to add new constituents.  The page takes several
    parameters:
    kbName - the name of the knowledge base for which the manifest is displayed.
    constituent - a constituent to be added to the KB.
    delete - a constituent to be deleted from the KB.
    reload - a request to reload the constituents of the KB.
    refetch - a request to 'git pull' to update the constituents of the KB.
*/
    String kbDir = KBmanager.configuration.getKbDir();
    File kbDirFile = new File(kbDir);
    String sumoDir = KBmanager.configuration.getKbDir();
    File sumoDirFile = new File(sumoDir);
    String saveAs = request.getParameter("saveAs");
    String constituent = request.getParameter("constituent");
    String saveFile = request.getParameter("saveFile");
    String delete = request.getParameter("delete");
    String reload = request.getParameter("reload");
    String refetch = request.getParameter("refetch");
    String result = "";
    String reinitializeManifest = request.getParameter("reinitializeManifest");
String[] selectedKifFiles = request.getParameterValues("kifFile");

    if (role == null || !role.equalsIgnoreCase("admin")) {
    	saveAs = null;
    	saveFile = null;
    	constituent = null;
    	delete = null;
        reinitializeManifest = null;
        selectedKifFiles = null;
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
            boolean onlyPlainFOL = saveAs.equalsIgnoreCase("tptpFOL");
            File tptpf = new File(kbDirFile, (saveFile + ".tptp"));
            String tptpfcp = null;
            String tptpFile = null;
            try {
                tptpfcp = tptpf.getCanonicalPath();
                com.articulate.sigma.trans.SUMOKBtoTPTPKB skbtptpkb = new com.articulate.sigma.trans.SUMOKBtoTPTPKB();
        		skbtptpkb.kb = kb;
        		PrintWriter pw = new PrintWriter(new FileWriter(tptpfcp));
                tptpFile = skbtptpkb.writeFile(tptpfcp, null, false, pw);
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
            com.articulate.sigma.trans.OWLtranslator ot = new com.articulate.sigma.trans.OWLtranslator();
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
if (reinitializeManifest != null && role != null && role.equalsIgnoreCase("admin")) {

    List<String> selected = new ArrayList<>();
    String kbDirCanonical = kbDirFile.getCanonicalPath();

    if (selectedKifFiles != null) {
        for (String fileName : selectedKifFiles) {
            if (StringUtil.emptyString(fileName)) continue;
            if (!fileName.toLowerCase().endsWith(".kif")) continue;
            if (fileName.contains("..") || fileName.contains("/") || fileName.contains("\\")) continue;

            File f = new File(kbDirFile, fileName);
            String fCanonical = f.getCanonicalPath();

            if (f.exists() && f.isFile() && fCanonical.startsWith(kbDirCanonical + File.separator))
                selected.add(fileName);
        }
    }

    if (selected.isEmpty()) {
        result = "No KIF files selected. Config was not changed.";
    }
    else {
        Collections.sort(selected);

        KBmanager.configuration.setKbConstituentList(kbName, selected);
        KBmanager.getMgr().writeConfiguration();

        new File(kbDirFile, kbName + ".tptp").delete();
        new File(kbDirFile, kbName + ".tff").delete();
        new File(kbDirFile, kbName + ".thf").delete();
        new File(kbDirFile, kbName + "_plain.thf").delete();
        new File(kbDirFile, kbName + "_modal.thf").delete();

        KBmanager.getMgr().reinitializeFromConfiguration();
        kb = KBmanager.getMgr().getKB(kbName);

        result = "Updated config.xml, reinitialized " + kbName + ", and started TPTP regeneration.";
    }
}
else if (delete != null) {
        int i = kb.constituents.indexOf(constituent.intern());
        if (i == -1)
            System.out.println("Error in Manifest.jsp: No such constituent: " + constituent.intern());
        else {
            kb.constituents.remove(i);
            KBmanager.getMgr().writeConfiguration();
        }
	kb.reload();
    }
    else if (constituent != null) {
        kb.addConstituent(constituent);
        KBmanager.getMgr().writeConfiguration();
	    if (KBmanager.configuration.isCache()) {
	        kb.kbCache = new KBcache(kb);
	        kb.kbCache.buildCaches();
	        kb.kbCache.writeCacheFile();
	    }
    }
    else if (reload != null)
        kb.reload();
    else if (refetch != null) {
        Map<String, Integer> dirs = new HashMap<String, Integer>();
        for (int i = 0 ; i < kb.constituents.size() ; i++) {
            String cname = (String) kb.constituents.get(i);
            File file = new File(cname);
            if (!file.isAbsolute())
                file = new File(sumoDirFile, cname);
            String dir = file.getParent();
            if (!StringUtil.emptyString(dir) && !dirs.containsKey(dir))
                dirs.put(dir, 0);
        }
        for (String dir : dirs.keySet()) {
            ProcessBuilder pb = new ProcessBuilder("git", "pull");
            pb.directory(new File(dir));
            Process p = pb.start();
            p.waitFor();
            int exitvalue = p.exitValue();
            java.util.Scanner s = new java.util.Scanner(p.getInputStream()).useDelimiter("\\A");
            String stdout = s.hasNext() ? s.next() : "";
            s = new java.util.Scanner(p.getErrorStream()).useDelimiter("\\A");
            String stderr = s.hasNext() ? s.next() : "";
            System.out.println("INFO git pull (" + dir + ") exitValue: " + exitvalue);
            System.out.println("INFO git pull (" + dir + ") stdout: " + stdout);
            System.out.println("INFO git pull (" + dir + ") stderr: " + stderr);
        }
    }
%>
<HTML>
<HEAD>
<TITLE>Sigma Knowledge Engineering Environment - Constituents of <%=kbName %></TITLE>
</HEAD>
<BODY BGCOLOR=#FFFFFF>
    <%
        String pageName = "Manifest";
        String pageString = "Manifest";
    %>
    <%@include file="fragments/universal/CommonHeader.jspf" %>
<b>Files which are the <I>constituents</I> of the <B><%=kbName %></b> knowledge base </b>

<%
    Set<String> active = new HashSet<>();

    List<String> configuredFiles = KBmanager.configuration.getKbConstituentList(kbName);
    if (configuredFiles != null) {
        for (String c : configuredFiles)
            active.add(new File(c).getName());
    }

    File[] kifFiles = kbDirFile.listFiles((dir, name) ->
            name.toLowerCase().endsWith(".kif")
    );

    if (kifFiles != null)
        Arrays.sort(kifFiles, Comparator.comparing(File::getName));
%>

<hr>
<b>Configure KB constituents from <%=kbDirFile.getAbsolutePath()%></b>

<form name="manifestEditor" id="manifestEditor" action="Manifest.jsp" method="POST">
    <input type="hidden" name="kb" value="<%=kbName%>">

    <table border="0" cellspacing="2" cellpadding="2">
        <tr>
            <td><b>Use</b></td>
            <td><b>KIF File</b></td>
        </tr>

<%
    if (kifFiles == null || kifFiles.length == 0) {
%>
        <tr>
            <td colspan="2">No .kif files found in <%=kbDirFile.getAbsolutePath()%></td>
        </tr>
<%
    }
    else {
        for (File kifFile : kifFiles) {
            String fileName = kifFile.getName();
            boolean checked = active.contains(fileName);
%>
        <tr>
            <td>
                <input type="checkbox"
                       name="kifFile"
                       value="<%=fileName%>"
                       <%= checked ? "checked" : "" %>
                       <%= (role != null && role.equalsIgnoreCase("admin")) ? "" : "disabled" %>>
            </td>
            <td><%=fileName%></td>
        </tr>
<%
        }
    }
%>
    </table>

<% if (role != null && role.equalsIgnoreCase("admin")) { %>
    <br>
    <input type="submit" name="reinitializeManifest" value="Reinitialize KB with checked files">
<% } else { %>
    <p><i>Log in as admin to change selected constituents.</i></p>
<% } %>
</form>

<%
  HTMLformatter.kbHref = HTMLformatter.createHrefStart() + "/sigma/Browse.jsp?";
  String er = KBmanager.getMgr().getError();
  if (!kb.errors.isEmpty()) {
      Set<String> errors = kb.errors;
      out.println("<br/><b>Errors in KB " + kb.name + "</b><br>\n");
      out.println(HTMLformatter.formatErrorsWarnings(errors,kb));
  }
  if (!kb.warnings.isEmpty()) {
      Set<String> warns = kb.warnings;
      out.println("<br/><b>Warnings in KB " + kb.name + "</b><br>\n");
      out.println(HTMLformatter.formatErrorsWarnings(warns,kb));
  }

  if (StringUtil.isNonEmptyString(er))
      out.println(er);
  else
      if (StringUtil.isNonEmptyString(constituent) && StringUtil.emptyString(delete))
          out.println("File " + constituent + " loaded successfully.");
%>
<P>
  <a href="KBs.jsp">Return to home page</a><p>
<%@ include file="fragments/universal/Postlude.jspf" %>
</BODY>
</HTML>
