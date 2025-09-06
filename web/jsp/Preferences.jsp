<%@ include file="Prelude.jsp" %>

<%!
/** Return a preference, preferring the per-user manager if present. */
public static String pref(javax.servlet.http.HttpSession session, KBmanager globalMgr, String key) {
    Object o = session.getAttribute("kbManager");
    if (o instanceof UserKBmanager) {
        String v = ((UserKBmanager) o).getPref(key);
        if (v != null && v.length() > 0) return v;
    }
    return globalMgr.getPref(key);
}
%>

<%
    // ---- Setup -------------------------------------------------------------
    boolean isAdmin = "admin".equalsIgnoreCase(role);
    UserKBmanager userMgr = (UserKBmanager) session.getAttribute("kbManager");

    // current values (prefer user)
    String curKbDir = pref(session, mgr, "kbDir");
    String curCache = pref(session, mgr, "cache");         // "yes" | "no"
    String curTptp  = pref(session, mgr, "TPTP");          // "yes" | "no"
    String curLimit = pref(session, mgr, "userBrowserLimit"); // e.g., "25"
    if (curLimit == null || curLimit.isEmpty()) curLimit = "25";

    String message = "";

    // ---- Handle POST save --------------------------------------------------
    if ("POST".equalsIgnoreCase(request.getMethod())) {
        boolean makeGlobal = "true".equalsIgnoreCase(request.getParameter("makeGlobal"));
        String newCache = "on".equalsIgnoreCase(request.getParameter("cache")) ? "yes" : "no";
        String newTptp  = "on".equalsIgnoreCase(request.getParameter("tptp"))  ? "yes" : "no";
        String newLimit = request.getParameter("limit");
        if (newLimit == null || newLimit.trim().isEmpty()) newLimit = curLimit;

        try {
            if (!makeGlobal && userMgr != null) {
                // Save to per-user config
                userMgr.setPref("cache", newCache);
                userMgr.setPref("TPTP", newTptp);
                userMgr.setPref("userBrowserLimit", newLimit);
                // kbDir is derived for users; show it but don't let users change it here
                userMgr.writeUserConfiguration();
                userMgr.saveSerialized();
                message = "Saved user preferences.";
            } else if (makeGlobal && isAdmin) {
                // Admin saves to global config
                mgr.setPref("cache", newCache);
                mgr.setPref("TPTP", newTptp);
                mgr.setPref("userBrowserLimit", newLimit);
                KBmanager.getMgr().writeConfiguration();
                message = "Saved global preferences.";
            } else {
                message = "Not authorized to save global preferences.";
            }
        } catch (Exception e) {
            message = "Error saving preferences: " + e.getMessage();
            e.printStackTrace();
        }

        // refresh displayed values after save
        curKbDir = pref(session, mgr, "kbDir");
        curCache = pref(session, mgr, "cache");
        curTptp  = pref(session, mgr, "TPTP");
        curLimit = pref(session, mgr, "userBrowserLimit");
        if (curLimit == null || curLimit.isEmpty()) curLimit = "25";
    }
%>

<html>
<head>
  <title>SigmaKEE Preferences</title>
</head>
<body bgcolor="#FFFFFF">

<table ALIGN="LEFT" WIDTH="80%"><tr><td bgcolor="#AAAAAA">
  <img src="pixmaps/1pixel.gif" width="1" height="1" border="0">
</td></tr></table><br/>

<h2>Preferences</h2>

<% if (message != null && message.length() > 0) { %>
  <p><b><%= message %></b></p>
<% } %>

<form method="POST" action="Preferences.jsp">
  <table cellpadding="4" cellspacing="2" border="0">
    <tr>
      <td><b>User</b></td>
      <td><%= (username == null ? "guest" : username) %></td>
    </tr>
    <tr>
      <td><b>KB Directory</b></td>
      <td>
        <input type="text" size="70" value="<%= curKbDir %>" readonly>
        <div style="font-size:12px;color:#555;">(Per-user manager derives this automatically)</div>
      </td>
    </tr>
    <tr>
      <td><b>Enable Cache</b></td>
      <td>
        <input type="checkbox" name="cache" <%= "yes".equalsIgnoreCase(curCache) ? "checked" : "" %> >
      </td>
    </tr>
    <tr>
      <td><b>Enable TPTP</b></td>
      <td>
        <input type="checkbox" name="tptp" <%= "yes".equalsIgnoreCase(curTptp) ? "checked" : "" %> >
      </td>
    </tr>
    <tr>
      <td><b>User Browser Limit</b></td>
      <td>
        <input type="number" name="limit" min="1" max="10000" value="<%= curLimit %>">
      </td>
    </tr>

    <% if (isAdmin) { %>
      <tr>
        <td><b>Make Global</b></td>
        <td>
          <input type="checkbox" name="makeGlobal" value="true">
          <span style="font-size:12px;color:#555;">(Admin only; saves to global configuration)</span>
        </td>
      </tr>
    <% } %>

    <tr>
      <td></td>
      <td><input type="submit" value="Save Preferences"></td>
    </tr>
  </table>
</form>

<p><a href="KBs.jsp">Back to KBs</a></p>

<%@ include file="Postlude.jsp" %>
</body>
</html>
