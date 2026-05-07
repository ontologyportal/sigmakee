<%@ page language="java"
         contentType="text/html; charset=US-ASCII"
         pageEncoding="US-ASCII"
         import="com.articulate.sigma.user.UserManager,
                 com.articulate.sigma.security.ValidationUtils" %>

<%
    String error = "";
    if ("POST".equalsIgnoreCase(request.getMethod())) {
        String username = ValidationUtils.sanitizeString(request.getParameter("username"));
        String password = ValidationUtils.sanitizeString(request.getParameter("password"));
        UserManager userManager = new UserManager();
        boolean loggedIn = userManager.login(request, username, password);
        if (loggedIn) {
            System.out.println("login.jsp: Successful login for " + username);
            response.sendRedirect("KBs.jsp");
            return;
        }
        else {
            System.err.println("Bad login attempt in login.jsp for " + username);
            error = "Incorrect username or password!";
        }
    }
%>
<html>
    <head>
        <title>Sigma Login</title>
    </head>
    <body>
        <table width="95%" cellspacing="0" cellpadding="0">
            <tr>
                <td valign="top">
                    <table cellspacing="0" cellpadding="0">
                        <tr>
                            <td align="left" valign="top">
                                <img src="pixmaps/sigmaSymbol.gif" alt="pixmaps/sigmaSymbol.gif">
                            </td>
                            <td>&nbsp;&nbsp;</td>
                            <td align="left" valign="top">
                                <img src="pixmaps/logoText.gif" alt="pixmaps/logoText.gif"><br>
                                <b>Sigma Login</b>
                            </td>
                        </tr>
                    </table>
                </td>
            </tr>
        </table>
        <table align="left" width="80%">
            <tr>
                <td bgcolor="#AAAAAA">
                    <img src="pixmaps/1pixel.gif" alt="pixmaps/1pixel.gif" width="1" height="1" border="0">
                </td>
            </tr>
        </table>
        <br>
        <% if (!error.isEmpty()) { %>
            <div style="color:red;"><%= error %></div>
        <% } %>
        <form method="POST" action="login.jsp">
            <table>
                <tr>
                    <td><b>User name:</b></td>
                    <td><b><input name="username" type="text" maxlength="20" size="10"></b></td>
                </tr>
                <tr>
                    <td><b>Password:</b></td>
                    <td><b><input name="password" type="password" maxlength="20" size="6"></b></td>
                </tr>
                <tr>
                    <td><b><input value="  Log In  " type="submit"></b></td>
                    <td></td>
                </tr>
            </table>
        </form>
        <p><a href="ForgotPassword.jsp">Forgot your password?</a></p>
        <table align="left" width="80%">
            <tr>
                <td bgcolor="#AAAAAA">
                    <img src="pixmaps/1pixel.gif" alt="pixmaps/1pixel.gif" width="1" height="1" border="0">
                </td>
            </tr>
        </table>
        <br>
        <form method="POST" action="Register.jsp">
            <table>
                <tr>
                    <td>
                        <b><input value="Register new account" type="submit"></b>
                        (requires moderator approval)
                    </td>
                </tr>
            </table>
        </form>
    </body>
</html>