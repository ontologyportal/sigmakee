<%@ page import="java.io.File, java.util.*, com.articulate.sigma.*, com.articulate.sigma.utils.StringUtil" %>
<%@ include file="Prelude.jsp" %>
<%
    if (!role.equalsIgnoreCase("admin")) { response.sendRedirect("login.html"); return; }

    String inferenceTestDir = KBmanager.getMgr().getPref("inferenceTestDir");
    String engine = Optional.ofNullable(request.getParameter("engine")).orElse("Vampire");
    int timeout = 30;
    try { if (request.getParameter("timeout") != null) timeout = Math.max(1, Integer.parseInt(request.getParameter("timeout"))); } catch (Exception ignore) {}

    String action = request.getParameter("action");   // "run" when a RUN button is pressed
    String tqName = request.getParameter("tq");       // file name only
    String mode   = request.getParameter("mode");     // "normal" or "mp"

    // Persist per-cell results in the session
    Map<String,Object> cellMap = (Map<String,Object>) session.getAttribute("cellMap");
    if (cellMap == null) {
        cellMap = new HashMap<>();
        session.setAttribute("cellMap", cellMap);
    }

    // Handle a single test run if requested
    if ("run".equalsIgnoreCase(action) && inferenceTestDir != null && tqName != null) {
        String tqPath = inferenceTestDir + File.separator + tqName;
        boolean modusPonens = "mp".equalsIgnoreCase(mode);

        long t0 = System.currentTimeMillis();
        boolean pass = false;
        long millis = 0L;
        String detailsHtml = null;

        try {
            InferenceTestSuite its = new InferenceTestSuite();
            // ---- call your single-test method (add this to InferenceTestSuite) ----
            InferenceTestSuite.OneResult r = its.runOne(kb, engine, timeout, tqPath, modusPonens);
            pass = r.pass;
            millis = r.millis;
            detailsHtml = r.html;

            Map<String,Object> cell = new HashMap<>();
            String key = tqName + "|" + mode;  // e.g., "foo.tq|normal"
            cell.put("pass",   r.pass);
            cell.put("millis", r.millis);
            cell.put("meta",   "(engine=" + esc(engine) + ", t=" + timeout + "s)");
            cell.put("expected", r.expected);
            cell.put("actual",   r.actual);
            cell.put("html",     r.html);
            cellMap.put(key, cell);

        } catch (Throwable ex) {
            detailsHtml = "<pre style='color:#b00'>ERROR: " + ex.getClass().getSimpleName() + ": " + esc(ex.getMessage()) + "</pre>";
            millis = System.currentTimeMillis() - t0;
            pass = false;
        }
    }
%>

<%
    String reloadMsg = null;

    if ("reloadKB".equalsIgnoreCase(action)) {
        long t0 = System.currentTimeMillis();
        try {
            // FULL reload: wipe user assertions + reload base KB + rebuild inference view
            kb.deleteUserAssertionsAndReload();
            KBmanager.getMgr().loadKBforInference(kb);

            // Optionally pre-load provers so first run is warm:
            try {
                String eproverExec = KBmanager.getMgr().getPref("eprover");
                if (eproverExec != null && new File(eproverExec).exists()) kb.loadEProver();
            } catch (Exception ignore) {}
            try { kb.loadLeo(); } catch (Exception ignore) {}

            reloadMsg = "KB reloaded in " + (System.currentTimeMillis() - t0) + " ms.";
        } catch (Exception ex) {
            reloadMsg = "Reload failed: " + esc(ex.getMessage());
        }
    }
%>

<%
    if ("clearSession".equalsIgnoreCase(action)) {
        session.removeAttribute("cellMap");
        session.removeAttribute("runMeta");
        reloadMsg = "Session test results cleared.";

        // Force immediate page reload on the client
        out.println("<script>window.location.href='InferenceTestSuite.jsp';</script>");
    }
%>

<html>
<head>
    <title>Sigma - Inference Test Suite</title>
    <style>
        body { font-family: Arial, Helvetica, sans-serif; }
        .cellHead { display:flex; justify-content:space-between; align-items:center; gap:8px; }
        .statusPass { color: #0a0; font-weight: bold; }
        .statusFail { color: #b00; font-weight: bold; }
        .tiny { font-size: 12px; color:#666; }
        .controls { margin: 10px 0 16px; display:flex; gap:16px; align-items:center; }
        .runBtn { padding:4px 10px; }
    </style>

    <style>
        .testTable {
            width: 70%;
            margin: 25px auto;
            border-collapse: collapse;
            font-family: Arial, Helvetica, sans-serif;
            font-size: 14px;
            background: #fafafa;
            box-shadow: 0 2px 6px rgba(0,0,0,0.1);
            border-radius: 6px;
            overflow: hidden;
        }
        .testTable th, .testTable td {
            border: 1px solid #ddd;
            padding: 8px 10px;
            text-align: left;
            vertical-align: top;
        }
        .testTable th {
            background-color: #f2f2f2;
            font-weight: bold;
        }
        .testTable tr:nth-child(even) { background-color: #fdfdfd; }
        .testTable tr:hover { background-color: #f7faff; }
        .cellHead { display: flex; justify-content: space-between; align-items: center; gap: 6px; }
        .runBtn {
            background: #1d75b8;
            color: white;
            border: none;
            border-radius: 4px;
            padding: 4px 8px;
            cursor: pointer;
            font-size: 12px;
        }
        .runBtn:hover { background: #135c91; }
        .tiny { font-size: 12px; color: #666; }
        .fileName { font-weight: bold; }
        .filePath { font-size: 12px; color: #777; word-wrap: break-word; }
    </style>

    <script>
        function runOne(tq, mode) {
            const form = document.getElementById('runnerForm');
            form.tq.value = tq;
            form.mode.value = mode;
            form.submit();
        }
    </script>

    <script>
        function viewTestFile(fileName) {
            const url = 'ViewTest.jsp?name=' + encodeURIComponent(fileName);
            window.open(url, '_blank');
        }
    </script>

</head>
<body style="face=Arial,Helvetica" bgcolor="#FFFFFF">

<%
    String pageName = "InferenceTestSuite";
    String pageString = "Inference Interface";
%>
<%@include file="CommonHeader.jsp" %>
<table ALIGN="LEFT" WIDTH=80%><tr><TD BGCOLOR='#AAAAAA'>
    <IMG SRC='pixmaps/1pixel.gif' width=1 height=1 border=0></TD></tr></table><BR>

<h2>Inference Test Suite</h2>

<form id="runnerForm" method="POST" action="InferenceTestSuite.jsp">
    <!-- global controls persist across runs -->
    <div class="controls">
        <label><b>Engine:</b>
            <select name="engine">
                <option value="Vampire" <%= "Vampire".equals(engine) ? "selected" : "" %>>Vampire</option>
                <option value="EProver" <%= "EProver".equals(engine) ? "selected" : "" %>>EProver</option>
            </select>
        </label>
        <label><b>Timeout (sec):</b>
            <input type="number" name="timeout" min="1" value="<%=timeout%>">
        </label>
        <span class="tiny">Tip: each RUN uses these settings.</span>
    </div>

    <!-- hidden fields set when clicking RUN -->
    <input type="hidden" name="action" value="run">
    <input type="hidden" name="tq" value="">
    <input type="hidden" name="mode" value="">
</form>

<%
    if (inferenceTestDir == null) {
%>
<div style="color:#b00">No inference test directory set. Configure it in <a href="Preferences.jsp">Preferences</a>.</div>
<%
} else {
    File dir = new File(inferenceTestDir);
    File[] files = dir.listFiles((d, name) -> name.toLowerCase().endsWith(".tq"));
    Arrays.sort(files, Comparator.comparing(File::getName, String.CASE_INSENSITIVE_ORDER));

%>
<table class="testTable">
    <thead>
    <tr>
        <th style="width:40%">File</th>
        <th style="width:30%">Normal</th>
        <th style="width:30%">ModusPonens</th>
    </tr>
    </thead>
    <tbody>
    <%
        if (files == null || files.length == 0) {
    %>
    <tr><td colspan="3" style="text-align:center;"><i>No .tq files found in <%= esc(inferenceTestDir) %></i></td></tr>
    <%
    } else {
        for (File f : files) {
            String name = f.getName();
            String kN = name + "|normal";
            String kM = name + "|mp";

            Map cN = (Map) cellMap.get(kN);
            Map cM = (Map) cellMap.get(kM);

            String metaN = (cN == null) ? "" : (String)cN.get("meta");
            String metaM = (cM == null) ? "" : (String)cM.get("meta");

            String sumN = (cN == null) ? null : (String)cN.get("html");
            String sumM = (cM == null) ? null : (String)cM.get("html");
    %>
    <tr>
        <!-- File column -->
        <td>
            <div class="fileName">
                <a href="javascript:void(0);"
                   onclick="viewTestFile('<%=esc(name)%>')"
                   style="color:#0073e6; text-decoration:underline;">
                    <%= esc(name) %>
                </a>
            </div>
            <div class="filePath tiny"><%= esc(f.getAbsolutePath()) %></div>
        </td>

        <!-- Normal column -->
        <td>
            <div class="cellHead">
                <button class="runBtn" onclick="runOne('<%=esc(name)%>','normal'); return false;">RUN</button>
                <span class="tiny"><%= esc(metaN) %></span>
            </div>
            <div>
                <% if (cN == null) { %>
                <span class='tiny'>- not run yet -</span>
                <% } else { %>
                    <div><%= (String)cN.get("html") %></div>
                <% } %>
            </div>
        </td>

        <!-- ModusPonens column -->
        <td>
            <div class="cellHead">
                <button class="runBtn" onclick="runOne('<%=esc(name)%>','mp'); return false;">RUN</button>
                <span class="tiny"><%= esc(metaM) %></span>
            </div>
            <div>
                <% if (cM == null) { %>
                <span class='tiny'>- not run yet -</span>
                <% } else { %>
                    <div><%= (String)cM.get("html") %></div>
                <% } %>
            </div>
        </td>
    </tr>
    <%
            }
        }
    %>
    </tbody>
</table>
<%
    } // inferenceTestDir != null
%>

<hr style="margin:24px 0; border:0; border-top:1px solid #ddd; width:70%; margin-left:auto; margin-right:auto;">

<div style="width:70%; margin:0 auto 20px auto; display:flex; justify-content:space-between; align-items:center;">
    <div>
        <form method="post" onsubmit="return confirm('This will fully reload the KB and clear user assertions. Continue?');">
            <input type="hidden" name="action" value="reloadKB">
            <!-- preserve current UI selections (optional) -->
            <input type="hidden" name="engine" value="<%= esc(engine) %>">
            <input type="hidden" name="timeout" value="<%= timeout %>">
            <button type="submit"
                    style="background:#b33;color:#fff;border:0;border-radius:4px;padding:6px 12px;cursor:pointer;">
                Reload KB
            </button>
            <span class="tiny" style="margin-left:8px;">Reloads base ontology and clears user assertions.</span>
        </form>

        <form method="post" style="margin-top:8px;"
              onsubmit="return confirm('This will clear all stored test results for this page. Continue?');">
            <input type="hidden" name="action" value="clearSession">
            <button type="submit"
                    style="background:#777;color:#fff;border:0;border-radius:4px;padding:6px 12px;cursor:pointer;">
                Clear Results
            </button>
            <span class="tiny" style="margin-left:8px;">Removes all saved test outcomes from memory.</span>
        </form>
    </div>
</div>

<%@ include file="Postlude.jsp" %>
</body>
</html>

<%!
    private static String esc(String s){
        if (s == null) return "";
        StringBuilder b = new StringBuilder(s.length()+16);
        for (int i=0; i<s.length(); i++){
            char c = s.charAt(i);
            switch (c){
                case '&': b.append("&amp;"); break;
                case '<': b.append("&lt;");  break;
                case '>': b.append("&gt;");  break;
                case '"': b.append("&quot;");break;
                case '\'':b.append("&#39;"); break;
                default:  b.append(c);
            }
        }
        return b.toString();
    }
%>