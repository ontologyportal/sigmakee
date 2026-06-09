<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"
    import="com.articulate.sigma.user.User,
            com.articulate.sigma.user.UserManager,
            com.articulate.sigma.user.PasswordService,
            com.articulate.sigma.utils.ValidationUtils,
            com.articulate.sigma.utils.StringUtil" %>
<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <meta name="referrer" content="no-referrer">
    <title>Reset Password</title>
</head>
<body>
<h2>Reset Password</h2>
<%
String token = request.getParameter("token");
if (StringUtil.emptyString(token)) {
    out.println("<p>Invalid or expired password reset link.</p>");
    return;
}
UserManager userManager = (UserManager) application.getAttribute("userManager");
String tokenHash = PasswordService.hashResetToken(token);
User user = userManager.getUserForValidPasswordResetToken(tokenHash);
if (user == null) {
    out.println("<p>Invalid or expired password reset link.</p>");
    return;
}
if ("POST".equalsIgnoreCase(request.getMethod())) {
    String password = request.getParameter("password");
    String confirmPassword = request.getParameter("confirmPassword");
    if (StringUtil.emptyString(password) || !password.equals(confirmPassword)) {
        out.println("<p>Passwords are empty or do not match.</p>");
    }
    else {
        boolean reset = userManager.resetPasswordWithToken(tokenHash, user.getUsername(), password);
        if (reset) {
%>
            <p>Your password has been reset.</p>
            <p><a href="login.jsp">Log in</a></p>
<%
            return;
        }
        else {
            out.println("<p>Unable to reset password. The link may have expired.</p>");
        }
    }
}
%>
<form method="post" action="ResetPassword.jsp">
    <input type="hidden" name="token" value="<%= ValidationUtils.sanitizeString(token) %>">
    <p>
        <label>New password:</label><br>
        <input type="password" name="password" required>
    </p>
    <p>
        <label>Confirm new password:</label><br>
        <input type="password" name="confirmPassword" required>
    </p>
    <p>
        <input type="submit" value="Reset password">
    </p>
</form>
</body>
</html>