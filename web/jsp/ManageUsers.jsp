<%@ page language="java"
    contentType="text/html; charset=UTF-8"
    pageEncoding="UTF-8"
    import="com.articulate.sigma.*, com.articulate.sigma.user.UserManager, com.articulate.sigma.user.User, com.articulate.sigma.utils.*, com.articulate.sigma.utils.StringUtil, java.sql.*, java.util.*" %>
<!DOCTYPE html>
<html>
<head>
  <meta charset="UTF-8">
  <title>Manage Users</title>
  <style>
    body { font-family: system-ui, -apple-system, Segoe UI, Roboto, Arial, sans-serif; margin: 24px; }
    h1 { margin: 0 0 16px; }
    .card {
        width: fit-content;
        max-width: 100%;
        border: 1px solid #000;
        padding: 16px;
        margin: 12px 0;
        background: #fff;
        border-radius: 4px;
        overflow-x: auto;
    }

    .grid {
        width: max-content;
        border-collapse: collapse;
        margin-top: 12px;
    }
  
    .grid th, .grid td { border: 1px solid #000; padding: 6px 8px; text-align: left;}
    .row { display:flex; gap:12px; flex-wrap:wrap; align-items:center; }
    .row > label { display:flex; flex-direction:column; gap:6px; min-width:220px; }
    .muted { color:#666; font-size:12px; }
    .notice { margin: 8px 0 16px; padding: 8px 10px; border:1px solid #000; }
    .success { color:#077d3f; }
    .danger  { color:#b00020; }
    .btn { padding: 6px 10px; cursor:pointer; }
    a { text-decoration:none; }
    .toplink { margin-bottom: 12px; display:inline-block; }
  </style>
</head>
<body>
<%
    // Flash message
    String created = request.getParameter("created");
    String error   = request.getParameter("err");
    String flash = null;
    
    UserManager userManager = (UserManager) application.getAttribute("userManager");
    try {
        userManager.getAllUsernames(request);// Forces an admin check before rendering the page.
    }
    catch (SecurityException se) {
        response.sendRedirect("login.jsp");
        return;
    }
    String action = request.getParameter("action");

    if ("createUser".equals(action) && "POST".equalsIgnoreCase(request.getMethod())) {
        try {
            String firstName    = ValidationUtils.sanitizeString(request.getParameter("firstName"));
            String lastName     = ValidationUtils.sanitizeString(request.getParameter("lastName"));
            String username     = ValidationUtils.sanitizeString(request.getParameter("username"));
            String password     = request.getParameter("password");
            String organization = ValidationUtils.sanitizeString(request.getParameter("organization"));
            String email        = ValidationUtils.sanitizeString(request.getParameter("email"));
            String role         = ValidationUtils.sanitizeString(request.getParameter("role"));
            String notRobot     = ValidationUtils.sanitizeString(request.getParameter("notRobot"));
            boolean success = userManager.createUser(
                    request,
                    username,
                    password,
                    email,
                    role,
                    firstName,
                    lastName,
                    organization,
                    notRobot
            );
            if (success) flash = "User '" + username + "' created.";
            else error = "Could not create user '" + username + "'. The username may already exist.";
        }
        catch (Exception ex) {
            error = ex.toString();
        }
    }
    if ("applyAll".equals(action) && "POST".equalsIgnoreCase(request.getMethod())) {
        try {
            String[] users  = request.getParameterValues("u");
            String[] roles  = request.getParameterValues("role");
            String[] emails = request.getParameterValues("email");
            String[] passes = request.getParameterValues("pass");
            String[] toDel  = request.getParameterValues("deleteUser");
            Set<String> deleteSet = new HashSet<>();
            if (toDel != null) {
                deleteSet.addAll(Arrays.asList(toDel));
            }
            if (users != null) {
                for (int i = 0; i < users.length; i++) {
                    String uname = ValidationUtils.sanitizeString(users[i]);
                    if (deleteSet.contains(uname)) {
                        userManager.deleteUser(request, uname);
                        continue;
                    }
                    if (roles != null && i < roles.length && roles[i] != null) {
                        String newRole = ValidationUtils.sanitizeString(roles[i]);
                        userManager.updateUserRole(request, uname, newRole);
                    }
                    if (emails != null && i < emails.length && emails[i] != null) {
                        String newEmail = ValidationUtils.sanitizeString(emails[i]);
                        userManager.updateUserEmail(request, uname, newEmail);
                    }
                    if (passes != null && i < passes.length && passes[i] != null && !passes[i].isEmpty()) {
                        userManager.updateUserPassword(request, uname, passes[i]);
                    }
                }
            }
            flash = "All changes applied.";
        }
        catch (Exception ex) {
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
          <th>First Name</th>
          <th>Last Name</th>
          <th>Organization</th>
          <th>New password</th>
        </tr>
      </thead>
      <tbody>
<%
    try {
        for (String uname : userManager.getAllUsernames(request)) {
            User u = userManager.getUser(request, uname);
            if (u == null) continue;
            String email = u.getEmail();
            String role = u.getRole();
            String firstName = u.getFirstName();
            String lastName = u.getLastName();
            String organization = u.getOrganization();
%>
          <tr>
              <td><input type="checkbox" name="deleteUser" value="<%= ValidationUtils.sanitizeString(uname) %>"></td>
              <td>
                  <b><%= ValidationUtils.sanitizeString(uname) %></b>
                  <input type="hidden" name="u" value="<%= ValidationUtils.sanitizeString(uname) %>">
              </td>
              <td>
                  <select name="role">
                      <option value="user"  <%= "user".equalsIgnoreCase(role) ? "selected" : "" %>>user</option>
                      <option value="admin" <%= "admin".equalsIgnoreCase(role) ? "selected" : "" %>>admin</option>
                      <option value="guest" <%= "guest".equalsIgnoreCase(role) ? "selected" : "" %>>guest</option>
                  </select>
              </td>
              <td><input type="text" name="email" value="<%= ValidationUtils.sanitizeString(email) %>" placeholder="email"></td>
              <td><input type="text" name="firstName" value="<%= ValidationUtils.sanitizeString(firstName) %>" placeholder="firstName"></td>
              <td><input type="text" name="lastName" value="<%= ValidationUtils.sanitizeString(lastName) %>" placeholder="lastName"></td>
              <td><input type="text" name="organization" value="<%= ValidationUtils.sanitizeString(organization) %>" placeholder="organization"></td>
              <td><input type="password" name="pass" value="" placeholder="leave blank to keep"></td>
          </tr>
<%
        }
    }
    catch (Exception ex) {
%>
    <tr>
        <td colspan="5" class="danger"><b>Error loading users:</b> <%= ValidationUtils.sanitizeString(ex.toString()) %></td>
    </tr>
<%
    }
%>
      </tbody>
      <tfoot>
        <tr>
          <td colspan="8" style="text-align:right;">
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
  <form class="row" method="post" action="ManageUsers.jsp">
    <input type="hidden" name="action" value="createUser">
    <label>
      <span>Username</span>
      <input type="text" name="username" required>
    </label>
    <label>
      <span>Role</span>
      <select name="role">
        <option value="user">user</option>
        <option value="guest">guest</option>
        <option value="admin">admin</option>
      </select>
    </label>
    <label>
      <span>Email</span>
      <input type="email" name="email" required>
    </label>   
    <label>
      <span>First name</span>
      <input type="text" name="firstName" required>
    </label>
    <label>
      <span>Last name</span>
      <input type="text" name="lastName" required>
    </label>
    <label>
      <span>Organization</span>
      <input type="text" name="organization" required>
    </label>
    <label>
      <span>Password</span>
      <input type="password" name="password" required>
    </label> 
    <input type="hidden" name="notRobot" value="yes">
    <div style="flex-basis:100%;"></div>
    <button class="btn" type="submit">Create</button>
  </form>
</div>
</body>
</html>
