<%@ page language="java"
    contentType="text/html; charset=UTF-8"
    pageEncoding="UTF-8"
    import="com.articulate.sigma.*, com.articulate.sigma.utils.StringUtil, java.sql.*, java.util.*" %>
<!DOCTYPE html>
<html>
<head>
  <meta charset="UTF-8">
  <title>Manage Users</title>
  <style>
    body { font-family: system-ui, -apple-system, Segoe UI, Roboto, Arial, sans-serif; margin: 24px; }
    h1 { margin: 0 0 16px; }
    .card { border: 1px solid #000; padding: 16px; margin: 12px 0; }
    .row { display:flex; gap:12px; flex-wrap:wrap; align-items:center; }
    .row > label { display:flex; flex-direction:column; gap:6px; min-width:220px; }
    .muted { color:#666; font-size:12px; }
    .notice { margin: 8px 0 16px; padding: 8px 10px; border:1px solid #000; }
    .success { color:#077d3f; }
    .danger  { color:#b00020; }
    .btn { padding: 6px 10px; cursor:pointer; }
    a { text-decoration:none; }
    .toplink { margin-bottom: 12px; display:inline-block; }
    .card { border: 1px solid #000; padding: 16px; margin: 12px 0; background: #fff; border-radius: 4px;}
    .grid { width: 100%; border-collapse: collapse; margin-top: 12px;}
    .grid th, .grid td { border: 1px solid #000; padding: 6px 8px; text-align: left;}
  </style>
</head>
<body>

<%
  // Require admin
  if ((String) session.getAttribute("role") == null || !"admin".equalsIgnoreCase((String) session.getAttribute("role"))) {
      response.sendRedirect("login.html");
      return;
  }
  // Flash message
  String created = request.getParameter("created");
  String error   = request.getParameter("err");
  String flash = null;
%>

<%! 
  String sha1Hex(String input) throws Exception {
      java.security.MessageDigest md = java.security.MessageDigest.getInstance("SHA-1");
      byte[] digest = md.digest(input.getBytes(java.nio.charset.StandardCharsets.UTF_8));
      StringBuilder sb = new StringBuilder(digest.length * 2);
      for (byte b : digest) sb.append(String.format("%02x", b));
      return sb.toString();
  }
  String esc(String s) {
      if (s == null) return "";
      return s.replace("&","&amp;")
              .replace("<","&lt;")
              .replace(">","&gt;")
              .replace("\"","&quot;")
              .replace("'","&#39;");
  }
%>

<%
    // Require admin
    String role = (String) session.getAttribute("role");
    if (role == null || !"admin".equalsIgnoreCase(role)) {
        response.sendRedirect("login.html");
        return;
    }
    
    PasswordService ps = PasswordService.getInstance();

    // Handle bulk apply
    String action = request.getParameter("action");
    if ("applyAll".equals(action) && "POST".equalsIgnoreCase(request.getMethod())) {
        try {
            String[] users   = request.getParameterValues("u");
            String[] roles   = request.getParameterValues("role");
            String[] emails  = request.getParameterValues("email");
            String[] passes  = request.getParameterValues("pass");
            String[] toDel   = request.getParameterValues("deleteUser");

            Set<String> deleteSet = new HashSet<>();
            if (toDel != null) deleteSet.addAll(Arrays.asList(toDel));

            if (users != null) {
                for (int i=0; i<users.length; i++) {
                    String uname = users[i];
                    User u = User.fromDB(ps.conn, uname);
                    if (u == null) continue;

                    if (deleteSet.contains(uname)) {
                        ps.deleteUser(uname);
                        continue;
                    }
                    // update role
                    if (roles != null && i < roles.length && roles[i] != null) {
                        u.role = roles[i];
                        u.updateRole(ps.conn);
                    }
                    // update email
                    if (emails != null && i < emails.length && emails[i] != null) {
                        u.attributes.put("email", emails[i]);
                        u.toDB(ps.conn);
                    }
                    // update password
                    if (passes != null && i < passes.length && passes[i] != null && !passes[i].isEmpty()) {
                        ps.changeUserPassword(uname, passes[i]);
                    }
                }
            }
            flash = "All changes applied.";
        } catch (Exception ex) {
            error = ex.toString();
        }
    }
%>

<!-- Flash messages -->
<% if (flash != null) { %>
  <div class="notice success"><%= flash %></div>
<% } %>
<div class="toplink">
  <a href="KBs.jsp">&larr; Home</a>
</div>
<% if (created != null && !"".equals(created)) { %>
  <div class="notice success">User '<%= created %>' created.</div>
<% } %>
<% if (error != null && !"".equals(error)) { %>
  <div class="notice danger"><b>Create error:</b> <%= error %></div>
<% } %>

<!-- Manage All Users Table -->
<div class="card">
  <h2 style="margin-top:0;">Manage Users</h2>
  <form method="post" onsubmit="return confirmApply();">
    <input type="hidden" name="action" value="applyAll"/>
    <table class="grid">
      <thead>
        <tr>
          <th style="width:1%;"><label>Delete</label></th>
          <th>Username</th>
          <th>Role</th>
          <th>Email</th>
          <th>New password</th>
        </tr>
      </thead>
      <tbody>
<%
      try {
        for (String uname : ps.userIDs()) {
          User u = User.fromDB(ps.conn, uname);
          if (u == null) continue;
          String email = u.attributes.get("email");
%>
          <tr>
              <td><input type="checkbox" name="deleteUser" value="<%= esc(uname) %>"></td>
              <td>
                  <b><%= esc(uname) %></b>
                  <input type="hidden" name="u" value="<%= esc(uname) %>">
              </td>
              <td>
                  <select name="role">
                      <option value="user"  <%= "user".equalsIgnoreCase(u.role) ? "selected" : "" %>>user</option>
                      <option value="admin" <%= "admin".equalsIgnoreCase(u.role) ? "selected" : "" %>>admin</option>
                      <option value="guest" <%= "guest".equalsIgnoreCase(u.role) ? "selected" : "" %>>guest</option>
                  </select>
              </td>
              <td><input type="text" name="email" value="<%= esc(email) %>" placeholder="email"></td>
              <td><input type="password" name="pass" value="" placeholder="leave blank to keep"></td>
          </tr>
<%
      }
    } catch (Exception ex) {
%>
    <tr>
        <td colspan="5" class="danger"><b>Error loading users:</b> <%= esc(ex.toString()) %></td>
    </tr>
<%
    }
%>
      </tbody>
      <tfoot>
        <tr>
          <td colspan="5" style="text-align:right;">
            <button class="btn" type="submit">Apply</button>
          </td>
        </tr>
      </tfoot>
    </table>
  </form>
</div>

<!-- User Creation Table -->
<div class="card">
  <h2 style="margin-top:0;">Create User</h2>
  <form class="row" method="post" action="CreateUser.jsp">
    <input type="hidden" name="returnTo" value="ManageUsers.jsp"/>
    <label>
      <span>First name</span>
      <input type="text" name="firstName" required>
    </label>
    <label>
      <span>Last name</span>
      <input type="text" name="lastName" required>
    </label>
    <label>
      <span>Username</span>
      <input type="text" name="userName" required>
    </label>
    <label>
      <span>Password</span>
      <input type="password" name="password" required>
    </label>
    <label>
      <span>Organization</span>
      <input type="text" name="organization" required>
    </label>
    <label>
      <span>Email</span>
      <input type="email" name="email" required>
    </label>
    <input type="hidden" name="notRobot" value="yes">
    <div style="flex-basis:100%;"></div>
    <button class="btn" type="submit">Create</button>
  </form>
</div>

</body>
</html>
