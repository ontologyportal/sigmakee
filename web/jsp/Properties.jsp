<%@ include    file="fragments/universal/Prelude.jspf" %>
<%
if (!role.equalsIgnoreCase("admin")) {
    response.sendRedirect("KBs.jsp");
    return;
}
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
    <%@include file="fragments/universal/CommonHeader.jspf" %>
    <table ALIGN="LEFT" WIDTH=80%>
    <tr>
        <TD BGCOLOR='#AAAAAA'>
        <IMG SRC='pixmaps/1pixel.gif' width=1 height=1 border=0>
        </TD>
    </tr>
    </table>
    <BR>
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
  Configuration sigmaConfig = KBmanager.configuration;

  Set<String> reloadKeys = new HashSet<>(Arrays.asList(
        "cache",
        "cacheDisjoint",
        "kbDir",
        "sumoDir",
        "maxPredicateArity",
        "termFormats",
        "typePrefix"
    ));

  if ("POST".equalsIgnoreCase(request.getMethod())) {
      for (String key : Configuration.CONFIG_KEYS) {
          String newValue = request.getParameter(key);
          if (newValue == null)
              continue;

          String oldValue = sigmaConfig.getPreference(key);
          if (!Objects.equals(oldValue, newValue)) {
              sigmaConfig.setPreference(key, newValue);
              changed = true;
              if (reloadKeys.contains(key))
                  reload = true;
          }
      }

      if (changed) {
          sigmaConfig.writeXml();
          KBmanager.configuration = new Configuration(sigmaConfig.getConfigFilePath());
          sigmaConfig = KBmanager.configuration;
      }

      if (reload) {
          Set<String> kbNames = KBmanager.getMgr().getKBnames();
          if (kbNames != null && !kbNames.isEmpty()) {
              for (String akbName : kbNames) {
                  KB akb = KBmanager.getMgr().getKB(akbName);
                  if (akb != null) {
                      System.out.println("INFO in Properties.jsp: reloading the entire KB");
                      akb.reload();
                  }
              }
          }
      }
  }
%>

<FORM method="POST" ACTION="Properties.jsp">

<p><strong>Server</strong></p>

<label for="hostname">
<INPUT type="text" name="hostname" value="<%=sigmaConfig.getHostname() %>">
Hostname</label><P>

<label for="port">
<INPUT type="text" name="port" value="<%=sigmaConfig.getPort() %>">
Tomcat port</label><P>

<label for="https">
<INPUT type="radio" name="https" value="true" <%=sigmaConfig.isHttps() ? "checked" : "" %>> yes
<INPUT type="radio" name="https" value="false" <%=!sigmaConfig.isHttps() ? "checked" : "" %>> no
: Use HTTPS links</label><P>

<label for="inferenceTestDir">
<INPUT type="text" SIZE=80 name="inferenceTestDir" value="<%=sigmaConfig.getInferenceTestDir() %>">
Directory in which inference tests are found</label><P>

<label for="isAws">
<INPUT type="radio" name="isAws" value="true" <%=sigmaConfig.isAws() ? "checked" : "" %>> yes
<INPUT type="radio" name="isAws" value="false" <%=!sigmaConfig.isAws() ? "checked" : "" %>> no
: AWS mode</label><P>

<p><strong>Directories</strong></p>

<label for="baseDir">
<INPUT type="text" SIZE=80 name="baseDir" value="<%=sigmaConfig.getBaseDir() %>">
Base Sigma directory</label><P>

<label for="kbDir">
<INPUT type="text" SIZE=80 name="kbDir" value="<%=sigmaConfig.getKbDir() %>">
Generated KB/runtime directory</label><P>

<label for="sumoDir">
<INPUT type="text" SIZE=80 name="sumoDir" value="<%=sigmaConfig.getSumoDir() %>">
SUMO source directory</label><P>

<label for="graphDir">
<INPUT type="text" SIZE=80 name="graphDir" value="<%=sigmaConfig.getGraphDir() %>">
Directory in which dot graphs will be saved</label><P>

<label for="graphVizExec">
<INPUT type="text" SIZE=80 name="graphVizExec" value="<%=sigmaConfig.getGraphVizExec() %>">
GraphViz executable directory</label><P>

<label for="systemsDir">
<INPUT type="text" SIZE=80 name="systemsDir" value="<%=sigmaConfig.getStringPreference("systemsDir", "") %>">
Directory in which ATP systems are located</label><P>

<label for="verbnetDir">
<INPUT type="text" SIZE=80 name="verbnetDir" value="<%=sigmaConfig.getVerbnetDir() %>">
VerbNet directory</label><P>

<p><strong>Executables</strong></p>

<label for="eproverExec">
<INPUT type="text" SIZE=80 name="eproverExec" value="<%=sigmaConfig.getEproverExec() %>">
E prover executable</label><P>

<label for="vampireExec">
<INPUT type="text" SIZE=80 name="vampireExec" value="<%=sigmaConfig.getVampireExec() %>">
Vampire executable</label><P>

<label for="leoExec">
<INPUT type="text" SIZE=80 name="leoExec" value="<%=sigmaConfig.getLeoExec() %>">
LEO executable</label><P>

<label for="tptpExec">
<INPUT type="text" SIZE=80 name="tptpExec" value="<%=sigmaConfig.getTptpExec() %>">
tptp4X executable</label><P>

<label for="jeditExec">
<INPUT type="text" SIZE=80 name="jeditExec" value="<%=sigmaConfig.getJeditExec() %>">
jEdit executable</label><P>

<p><strong>Browser</strong></p>

<label for="adminBrowserLimit">
<INPUT type="text" name="adminBrowserLimit" value="<%=sigmaConfig.getAdminBrowserLimit() %>">
Admin browser limit</label><P>

<label for="userBrowserLimit">
<INPUT type="text" name="userBrowserLimit" value="<%=sigmaConfig.getUserBrowserLimit() %>">
User browser limit</label><P>

<label for="showCachedFormulas">
<INPUT type="radio" name="showCachedFormulas" value="true" <%=sigmaConfig.isShowCachedFormulas() ? "checked" : "" %>> yes
<INPUT type="radio" name="showCachedFormulas" value="false" <%=!sigmaConfig.isShowCachedFormulas() ? "checked" : "" %>> no
: Show cached formulas in the term browser</label><P>

<p><strong>KB / Translation</strong></p>

<label for="maxPredicateArity">
<INPUT type="text" name="maxPredicateArity" value="<%=sigmaConfig.getMaxPredicateArity() %>">
Maximum predicate arity</label><P>

<label for="cache">
<INPUT type="radio" name="cache" value="true" <%=sigmaConfig.isCache() ? "checked" : "" %>> yes
<INPUT type="radio" name="cache" value="false" <%=!sigmaConfig.isCache() ? "checked" : "" %>> no
: Employ statement caching</label><P>

<label for="cacheDisjoint">
<INPUT type="radio" name="cacheDisjoint" value="true" <%=sigmaConfig.isCacheDisjoint() ? "checked" : "" %>> yes
<INPUT type="radio" name="cacheDisjoint" value="false" <%=!sigmaConfig.isCacheDisjoint() ? "checked" : "" %>> no
: Cache disjoint relation data</label><P>

<label for="termFormats">
<INPUT type="radio" name="termFormats" value="true" <%=sigmaConfig.isTermFormats() ? "checked" : "" %>> yes
<INPUT type="radio" name="termFormats" value="false" <%=!sigmaConfig.isTermFormats() ? "checked" : "" %>> no
: Load term formats</label><P>

<label for="typePrefix">
<INPUT type="radio" name="typePrefix" value="true" <%=sigmaConfig.isTypePrefix() ? "checked" : "" %>> yes
<INPUT type="radio" name="typePrefix" value="false" <%=!sigmaConfig.isTypePrefix() ? "checked" : "" %>> no
: Add type guards to axioms</label><P>

<label for="loadLexicons">
<INPUT type="radio" name="loadLexicons" value="true" <%=sigmaConfig.isLoadLexicons() ? "checked" : "" %>> yes
<INPUT type="radio" name="loadLexicons" value="false" <%=!sigmaConfig.isLoadLexicons() ? "checked" : "" %>> no
: Load lexicons</label><P>

<p><strong>Ollama</strong></p>

<label for="ollamaHost">
<INPUT type="text" SIZE=80 name="ollamaHost" value="<%=sigmaConfig.getOllamaHost() %>">
Ollama host</label><P>

<p><strong>Email</strong></p>

<label for="smtpEmailServer">
<INPUT type="text" SIZE=80 name="smtpEmailServer" value="<%=sigmaConfig.getSmtpEmailServer() %>">
SMTP email server</label><P>

<label for="smtpEmailAddress">
<INPUT type="text" SIZE=80 name="smtpEmailAddress" value="<%=sigmaConfig.getSmtpEmailAddress() %>">
SMTP email address</label><P>

<label for="smtpEmailUser">
<INPUT type="text" SIZE=80 name="smtpEmailUser" value="<%=sigmaConfig.getSmtpEmailUser() %>">
SMTP email user</label><P>

<label for="smtpEmailPassword">
<INPUT type="password" SIZE=80 name="smtpEmailPassword" value="<%=sigmaConfig.getSmtpEmailPassword() %>">
SMTP email password</label><P>

<INPUT type="submit" value="Save">
<b>Some options require a restart of Tomcat and Sigma. Changing KB or translation options may force the KB to reload.</b>

</FORM>
<br><table ALIGN="LEFT" WIDTH=80%><tr><TD BGCOLOR='#AAAAAA'><IMG SRC='pixmaps/1pixel.gif' width=1 height=1 border=0></TD></tr></table><BR>
<%@ include file="fragments/universal/Postlude.jspf" %>
</BODY>
</HTML>
