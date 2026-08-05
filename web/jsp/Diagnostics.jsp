<%@ page import="com.articulate.sigma.Formula" %>
<%@ page import="com.articulate.sigma.Diagnostics" %>
<%@ page import="com.articulate.sigma.DiagnosticsCache" %>
<%@ page import="com.articulate.sigma.KBmanager" %>
<%@ page import="com.articulate.sigma.editor.ErrRec" %>
<%@ include file="fragments/universal/Prelude.jspf" %>
<%@ page import="com.articulate.sigma.editor.KifFileChecker" %>
<%@ page import="java.net.URLEncoder" %>
<%@ page import="java.nio.charset.StandardCharsets" %>
<%@ page import="java.io.File" %>
<html>
  <head>
    <title> Knowledge base Diagnostics</title>
  </head>
  <body bgcolor="#FFFFFF">
<%
/** This code is copyright Teknowledge (c) 2003, Articulate Software (c) 2003-2017, 2020-
    Infosys (c) 2017-2020.

    This software is released under the GNU Public License
    <http://www.gnu.org/copyleft/gpl.html>.

    Please cite the following article in any publication with references:

    Pease A., and Benzmüller C. (2013). Sigma: An Integrated Development Environment
    for Logical Theories. AI Communications 26, pp79-97.  See also
    http://github.com/ontologyportal
*/
  if (!role.equals("admin") && !role.equals("user")) {
    response.sendRedirect("KBs.jsp");
    return;
  }
  long t0 = System.currentTimeMillis();
  String kbHref = null;
  String formattedFormula = null;
  Map theMap = null;
  kbHref = HTMLformatter.createHrefStart() + "/sigma/Browse.jsp?lang=" + lang + "&kb=" + kbName + "&flang=" + flang;

  Map<String, DiagnosticsCache> diagnosticsCaches =
          (Map<String, DiagnosticsCache>) application.getAttribute("diagnosticsCaches");
  DiagnosticsCache diagnosticsCache =
          diagnosticsCaches == null ? null : diagnosticsCaches.get(kbName);
  boolean diagnosticsCacheAvailable = diagnosticsCache != null;
  if (!diagnosticsCacheAvailable)
      diagnosticsCache = DiagnosticsCache.empty(kbName);

  String termDependencyMessage = "";
  String termDependencyAction = request.getParameter("diagAction");

  if ("generateTermDependency".equals(termDependencyAction)) {
      if (Diagnostics.dependencyCacheExists(Diagnostics.TERM_DEPENDENCY_CACHE_FILE)) {
          termDependencyMessage = "Term dependency cache already exists at " + Diagnostics.dependencyCachePath(Diagnostics.TERM_DEPENDENCY_CACHE_FILE) + ".";
      }
      else {
          try {
              long depStart = System.currentTimeMillis();
              Diagnostics.saveDependenciesForAllKif(Diagnostics.TERM_DEPENDENCY_CACHE_FILE);
              kb = KBmanager.getMgr().getKB(kbName);
              double seconds = (System.currentTimeMillis() - depStart) / 1000.0;
              if (Diagnostics.dependencyCacheExists(Diagnostics.TERM_DEPENDENCY_CACHE_FILE)) {
                  termDependencyMessage = "Generated term dependency cache in " + seconds + " seconds at " + Diagnostics.dependencyCachePath(Diagnostics.TERM_DEPENDENCY_CACHE_FILE) + ".";
              }
              else {
                  termDependencyMessage = "Term dependency cache generation completed, but the cache file was not found.";
              }
          }
          catch (Exception e) {
              termDependencyMessage = "ERROR generating term dependency cache. See server logs. " + e.getClass().getSimpleName() + ": " + e.getMessage();
              e.printStackTrace();
          }
      }
  }
%>
<form action="Diagnostics.jsp">
    <%
        String pageName = "Diag";
        String pageString = "Knowledge Base Diagnostics";
    %>
    <%@include file="fragments/universal/CommonHeader.jspf" %>
</form>
<a href="WNDiag.jsp?kb=<%=kbName%>">Run WordNet diagnostics</a><p>
<form id="generateTermDependencyForm" method="post" action="Diagnostics.jsp">
  <input type="hidden" name="kb" value="<%=kbName%>">
  <input type="hidden" name="lang" value="<%=lang%>">
  <input type="hidden" name="flang" value="<%=flang%>">
  <input type="hidden" name="diagAction" value="generateTermDependency">
</form>
<details id="sigmaRsAuditPanel" data-kb="<%= URLEncoder.encode(kbName, StandardCharsets.UTF_8.name()) %>">
  <summary>
    <b>Logical consistency audit</b>
    <hr>
  </summary>

  <p>
    Run sigma-rs native saturation over the loaded constituents of
    <b><%= kbName %></b>
  </p>

  <label>
    Timeout
    <input id="sigmaAuditTimeout"
           type="number"
           min="1"
           value="60">
  </label>

  <label>
    Contradiction limit
    <input id="sigmaAuditLimit"
           type="number"
           min="1"
           value="64">
  </label>

  <label>
    Thoroughness
    <input id="sigmaAuditThoroughness"
           type="number"
           min="0.01"
           max="1"
           step="0.01"
           value="1.0">
  </label>

  <label>
    Scope
    <input id="sigmaAuditScope"
           type="number"
           min="1"
           step="0.1"
           value="2.0">
  </label>

  <button id="sigmaAuditStart" type="button">
    Run consistency audit
  </button>

  <span id="sigmaAuditStatus">Not run</span>

  <pre id="sigmaAuditReport"
       style="white-space:pre-wrap; overflow:auto; max-height:600px;
              padding:10px; border:1px solid #aaa;"></pre>
</details>
<%
  boolean termDependencyCacheExists = Diagnostics.dependencyCacheExists(Diagnostics.TERM_DEPENDENCY_CACHE_FILE);
  String termDependencyCachePath = Diagnostics.dependencyCachePath(Diagnostics.TERM_DEPENDENCY_CACHE_FILE).toString();
%>
<%
if (!diagnosticsCacheAvailable)
    out.println("<div style=\"color:DarkRed;\">Diagnostics cache is unavailable for KB "
            + kbName + ". Restart SigmaKEE to rebuild it.</div><br>");

Map<String, Set<String>> syntaxErrors =
        diagnosticsCache.getKifSyntaxErrors();
int syntaxErrorCount = 0;
for (Set<String> errors : syntaxErrors.values())
    syntaxErrorCount += errors.size();
out.println("<details" + (!syntaxErrors.isEmpty() ? " open" : "") + ">");
out.println("<summary><b style=\"color:DarkRed;\">"
        + "Error: KIF syntax errors (" + syntaxErrorCount + ")"
        + "</b><hr></summary>");
if (syntaxErrors.isEmpty()) {
    out.println("No KIF syntax errors found.");
}
else {
    for (Map.Entry<String, Set<String>> entry : syntaxErrors.entrySet()) {
        String constituentPath = entry.getKey();
        for (String syntaxError : entry.getValue()) {
            int errorLine = KifFileChecker.getLineNum(syntaxError);
            if (errorLine < 1)
                errorLine = 1;
            String editorUrl =
                    request.getContextPath()
                    + "/Editor.jsp?path="
                    + URLEncoder.encode(
                            constituentPath,
                            StandardCharsets.UTF_8.name())
                    + "&amp;line="
                    + errorLine;
            String label =
                    new File(constituentPath).getName()
                    + ":"
                    + errorLine;
            String displayedError = syntaxError
                    .replace("&", "&amp;")
                    .replace("<", "&lt;")
                    .replace(">", "&gt;");
            out.println(
                    "<a href=\""
                    + editorUrl
                    + "\"><b>"
                    + label
                    + "</b></a>: "
                    + displayedError
                    + "<br>");
        }
    }
}
out.println("</details></br>");

// Terms without parents
  List<String> termsWithoutParent = diagnosticsCache.getTermsNotBelowEntity();
  out.println("<details>");
  out.println("<summary><b style=\"color:DarkRed;\">Error: Terms without a root at Entity</b><hr></summary>");
  out.println(HTMLformatter.termList(termsWithoutParent,kbHref));
  out.println("</details></br>");

  // Terms with unloaded constituents
  out.println("<details>");
  out.println("<summary>");
  out.println("<b style=\"color:DarkRed;\">Error: Terms with unloaded constituents</b>");
  if (!termDependencyCacheExists) out.println("<button type=\"submit\" form=\"generateTermDependencyForm\" " + "onclick=\"event.stopPropagation();\" " + "style=\"margin-left:12px;\">Generate term dependency cache</button>");
  out.println("<hr>");
  out.println("</summary>");
  if (termDependencyMessage != null && !termDependencyMessage.isEmpty()) {
      out.println("<div style=\"padding:8px; border:1px solid #AAAAAA; background:#F5F5F5; margin:10px 0;\">");
      out.println(termDependencyMessage);
      out.println("</div>");
  }
  if (diagnosticsCache.isTermDependencyCacheAvailable())
      out.println(Diagnostics.printMissingConstituentDependencies(
              diagnosticsCache.getMissingConstituentDependencies(), kbHref));
  else {
      out.println("The term dependency cache has not been generated yet.<br>");
      out.println("Required cache file: " + termDependencyCachePath);
  }
  out.println("</details></br>");

  // Children of disjoint parents
  List<String> disjoint = diagnosticsCache.getChildrenOfDisjointParents();
  out.println("<details>");
  out.println("<summary><b style=\"color:DarkRed;\">Error: Terms with disjoint parents</b><hr></summary>");
  out.println(HTMLformatter.termList(disjoint,kbHref));
  out.println("</details></br>");

  // Children of disjoint parents
  List<String> parts = diagnosticsCache.getPartitionViolations();
  out.println("<details>");
  out.println("<summary><b style=\"color:DarkRed;\">Error: Partition violations</b><hr></summary>");
  for (String s : parts) {
      out.println(s + "<br>\n");
  }
  out.println(HTMLformatter.termList(disjoint,kbHref));
  out.println("</details></br>");

  // Formulae with type conflicts
  out.println("<details>");
  out.println("<summary><b style=\"color:DarkRed;\">Error: Formulae with type conflicts</b><hr></summary>");
  out.println(Diagnostics.printFormulaeWithTypeViolations(diagnosticsCache.getFormulaeWithTypeViolations(), kbHref));
  out.println("</details></br>");

  // relations without format
  List<String> termsWithoutFormat = diagnosticsCache.getRelationsWithoutFormat();
  out.println("<details>");
  out.println("<summary><b style=\"color:#DAA520;\">Warning: Relations without format</b><hr></summary>");
  out.println(HTMLformatter.termList(termsWithoutFormat,kbHref));
  out.println("</details></br>");

  // Terms without documentation
  List<String> termsWithoutDoc = diagnosticsCache.getTermsWithoutDoc();
  out.println("<details>");
  out.println("<summary><b style=\"color:#DAA520;\">Warning: Terms without documentation</b><hr></summary>");
  out.println(HTMLformatter.termList(termsWithoutDoc,kbHref));
  out.println("</details></br>");

  // Terms with multiple documentation
  List<String> termsWithMultipleDoc = diagnosticsCache.getTermsWithMultipleDoc();
  out.println("<details>");
  out.println("<summary><b style=\"color:#DAA520;\">Warning: Terms with multiple documentation</b><hr></summary>");
  out.println(HTMLformatter.termList(termsWithMultipleDoc,kbHref));
  out.println("</details></br>");

  // Terms differing only in capitalization
  List<String> termCapDiff = diagnosticsCache.getTermCapDiff();
  out.println("<details>");
  out.println("<summary><b style=\"color:#DAA520;\">Warning: Terms differing only in capitalization</b><hr></summary>");
  out.println(HTMLformatter.termList(termCapDiff,kbHref));
  out.println("</details></br>");

  // Members (instances) of a parent class that are not also members
  // of one of the subclasses that constitute the exhaustive
  // decomposition of the parent class.
  List<String> termsMissingFromPartition = diagnosticsCache.getMembersNotInAnyPartitionClass();
  out.println("<details>");
  out.println("<summary><b style=\"color:#DAA520;\">Warning: Instances of a partitioned class that are not instances of one of the class's partitioning subclasses</b><hr></summary>");
  out.println(HTMLformatter.termList(termsMissingFromPartition,kbHref));
  out.println("</details></br>");

  // Terms without rules
  List<String> norule = diagnosticsCache.getTermsWithoutRules();
  out.println("<details>");
  out.println("<summary><b style=\"color:#DAA520;\">Warning: Terms that do not appear in any rules</b><hr></summary>");
  out.println(HTMLformatter.termList(norule,kbHref));
  out.println("</details></br>");

  // Formulae extraneous quanitified variables
  List<Formula> noquant = diagnosticsCache.getQuantifierNotInBody();
  out.println("<details>");
  out.println("<summary><b style=\"color:#DAA520;\">Warning: Formulae with extraneous quantified variables</b><hr></summary>");
  for (Formula f : noquant)
	  out.println(f.htmlFormat(kbHref) + "<p>");
  out.println("</details></br>");

  // Formulae with unquantified variables appearing only in consequent
  List<Formula> noquantconseq = diagnosticsCache.getUnquantsInConseq();
  out.println("<details>");
  out.println("<summary><b style=\"color:#DAA520;\">Warning: Formulae with unquantified variable appearing only in consequent</b><hr></summary>");
  for (Formula f : noquantconseq)
	  out.println(f.htmlFormat(kbHref) + "<p>");
  out.println("</details></br>");

  // Files with mutual term dependencies
  out.println("<details>");
  out.println("<summary><b style=\"color:#DAA520;\">Warning: Files with mutual dependencies</b><hr></summary>");
  out.println(Diagnostics.printMutualDependencies(diagnosticsCache.getMutualDependencies(), kbHref));
  out.println("</details></br>");

  // Diagnostic runtime
  System.out.println("  > " + ((System.currentTimeMillis() - t0) / 1000.0)
                     + " seconds to run all diagnostics");
%>
<%@ include file="fragments/universal/Postlude.jspf" %>
    <script>
        (() => {
        const panel = document.getElementById("sigmaRsAuditPanel");
        const kb = decodeURIComponent(panel.dataset.kb);
        const button = document.getElementById("sigmaAuditStart");
        const status = document.getElementById("sigmaAuditStatus");
        const report = document.getElementById("sigmaAuditReport");

        let timer = null;

        async function refresh() {
            try {
                const response = await fetch(
                "SigmaRsAuditServlet?action=status&kb=" +
                    encodeURIComponent(kb),
                {
                    credentials: "same-origin",
                    cache: "no-store"
                }
                );

                const text = await response.text();

                if (!text) {
                throw new Error(
                    "The audit servlet returned an empty response"
                );
                }

                let data;

                try {
                data = JSON.parse(text);
                }
                catch (error) {
                throw new Error(
                    "Invalid response from audit servlet: " + text
                );
                }

                if (!response.ok) {
                throw new Error(
                    data.message || "Status request failed"
                );
                }

                status.textContent = data.state || "UNKNOWN";

                let output = data.report || "";

                if (data.diagnostics) {
                if (output) {
                    output += "\n\n";
                }

                output += "Diagnostics:\n" + data.diagnostics;
                }

                if (data.error) {
                if (output) {
                    output += "\n\n";
                }

                output += "Error:\n" + data.error;
                }

                report.textContent = output;

                if (data.state === "RUNNING") {
                button.disabled = true;
                return;
                }

                button.disabled = false;

                if (timer !== null) {
                clearInterval(timer);
                timer = null;
                }
            }
            catch (error) {
                status.textContent = "Status error";
                report.textContent = String(error);

                button.disabled = false;

                if (timer !== null) {
                clearInterval(timer);
                timer = null;
                }
            }
            }

        button.addEventListener("click", async () => {
            button.disabled = true;
            status.textContent = "Starting…";
            report.textContent = "";

            try {
                const body = new URLSearchParams({
                action: "start",
                kb,
                timeout:
                    document.getElementById("sigmaAuditTimeout").value,
                limit:
                    document.getElementById("sigmaAuditLimit").value,
                thoroughness:
                    document.getElementById("sigmaAuditThoroughness").value,
                scope:
                    document.getElementById("sigmaAuditScope").value
                });

                const response = await fetch("SigmaRsAuditServlet", {
                method: "POST",
                credentials: "same-origin",
                headers: {
                    "Content-Type":
                    "application/x-www-form-urlencoded; charset=UTF-8"
                },
                body
                });

                const text = await response.text();
                const data = text ? JSON.parse(text) : {};

                if (!response.ok) {
                throw new Error(
                    data.message || "Unable to start audit"
                );
                }

                status.textContent = "RUNNING";

                if (timer !== null) {
                clearInterval(timer);
                }

                timer = setInterval(refresh, 2000);
                await refresh();
            }
            catch (error) {
                status.textContent = "Start failed";
                report.textContent = String(error);
                button.disabled = false;
            }
            });

        refresh();
        })();
    </script>
  </body>
</html>