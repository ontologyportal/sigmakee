<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"
    import="com.articulate.sigma.user.UserManager,
            com.articulate.sigma.security.ValidationUtils,
            com.articulate.sigma.utils.StringUtil" %>
<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <title>Forgot Password</title>
</head>
<body>
<h2>Reset Password</h2>
<%
String method = request.getMethod();
if ("POST".equalsIgnoreCase(method)) {
    String email = request.getParameter("email");
    UserManager userManager = (UserManager) application.getAttribute("userManager");
    userManager.requestPasswordReset(email);
%>
    <p>If an account exists for that email address, a password reset link will be sent.</p>
    <p><a href="login.jsp">Return to login</a></p>
<%
    return;
}
%>
<form method="post" action="ForgotPassword.jsp">
    <p>
        <label>Email address:</label><br>
        <input type="email" name="email" size="40" required>
    </p>
    <p>
        <input type="submit" value="Send reset link">
    </p>
</form>
</body>
</html>