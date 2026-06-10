package com.articulate.sigma.user;

import com.articulate.sigma.KB;
import com.articulate.sigma.KBmanager;
import com.articulate.sigma.user.User;
import com.articulate.sigma.user.UserManager;
import com.articulate.sigma.utils.StringUtil;
import com.articulate.sigma.utils.ValidationUtils;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public class EmailService {

    private static final String SIGMA_WEBAPP_PATH = "/sigma/";
    private static final String SMTP_PORT = "587";

    private final String smtpEmailAddress;
    private final String smtpEmailUser;
    private final String smtpEmailPassword;
    private final String smtpEmailServer;
    private final String webHostname;
    private final String webPort;
    private final String webProtocol;
    private final String baseSigmaUrl;

    public EmailService() {

        KBmanager kbManager = KBmanager.getMgr();
        this.smtpEmailAddress = kbManager.getPref("smtpEmailAddress");
        this.smtpEmailUser = kbManager.getPref("smtpEmailUser");
        this.smtpEmailPassword = kbManager.getPref("smtpEmailPassword");
        this.smtpEmailServer = kbManager.getPref("smtpEmailServer");
        this.webHostname = kbManager.getPref("hostname");
        this.webPort = kbManager.getPref("port");
        this.webProtocol = "true".equalsIgnoreCase(kbManager.getPref("https")) ? "https" : "http";
        this.baseSigmaUrl = this.webProtocol + "://" + this.webHostname + ":" + webPort + "/sigma/";
    }

    /********************************************************************
     * Sends a generic HTML notification email to all admin users.
     * @param subject the email subject
     * @param htmlBody the HTML body
     * @return true if the email was sent successfully
     */
    public boolean sendAdminNotification(String subject, String htmlBody) {

        UserDatabase userDatabase = new UserDatabase();
        try {
            // Commented out to avoid spamming all admins.
            // List<String> adminEmails = userDatabase.getAdminEmails();
            List<String> adminEmails = new ArrayList<>();
            adminEmails.add("shaunrose831@gmail.com");
            return sendHtmlEmail(adminEmails, subject, htmlBody);
        }
        finally {
            userDatabase.close();
        }
    }

    /********************************************************************
     * Sends a generic HTML email to the given recipients.
     * @param recipients the email recipient addresses
     * @param subject the email subject
     * @param htmlBody the HTML body
     * @return true if the email was sent successfully
     */
    public boolean sendHtmlEmail(List<String> recipients, String subject, String htmlBody) {

        if (recipients == null || recipients.isEmpty()) {
            System.err.println("ERROR: No email recipients provided.");
            return false;
        }
        if (StringUtil.emptyString(subject)) {
            System.err.println("ERROR: Cannot send email with empty subject.");
            return false;
        }
        if (StringUtil.emptyString(htmlBody)) {
            System.err.println("ERROR: Cannot send email with empty body.");
            return false;
        }
        if (!isSmtpConfigured()) {
            printSmtpConfigurationError();
            return false;
        }

        try {
            MimeMessage message = new MimeMessage(createMailSession());
            message.setFrom(new InternetAddress(smtpEmailAddress));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(String.join(",", recipients)));
            message.setSubject(subject, StandardCharsets.UTF_8.name());
            message.setContent(htmlBody, "text/html; charset=UTF-8");
            Transport.send(message);
            return true;
        }
        catch (MessagingException me) {
            System.err.println("ERROR: Unable to send email.");
            me.printStackTrace();
            return false;
        }
    }

    /********************************************************************
     * Sends an account registration approval request email to the moderators.
     * @param user the user requesting registration
     * @param adminEmails the moderator/admin email addresses
     * @return true if the email was sent successfully
     */
    public boolean requestAdminApprovalForUser(User user, List<String> adminEmails) {

        if (user == null) {
            System.err.println("ERROR: Cannot send moderator approval email for null user.");
            return false;
        }
        if (adminEmails == null || adminEmails.isEmpty()) {
            System.err.println("ERROR: No admin emails found. Cannot send moderator request.");
            return false;
        }
        if (!isSmtpConfigured()) {
            printSmtpConfigurationError();
            return false;
        }
        String approvalURL = this.baseSigmaUrl + "ApproveUser.jsp?user=" +  URLEncoder.encode(user.getUsername(), StandardCharsets.UTF_8);
        String recipientList = String.join(",", adminEmails);
        try {
            MimeMessage message = new MimeMessage(createMailSession());
            message.setFrom(new InternetAddress(smtpEmailAddress));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(recipientList));
            message.setSubject("SigmaKEE: Account Registration Request from " + safe(user.getFirstName()) + " " + safe(user.getLastName()), StandardCharsets.UTF_8.name());
            message.setContent(createAdminApprovalEmailHtml(user, approvalURL), "text/html; charset=UTF-8");
            Transport.send(message);
            return true;
        }
        catch (MessagingException me) {
            System.err.println("ERROR: Unable to send moderator approval email.");
            me.printStackTrace();
            return false;
        }
    }

    /********************************************************************
     * Sends an email notifying a user that their SigmaKEE account was approved.
     * @param user the approved user
     * @return true if the email was sent successfully
     */
    public boolean sendAccountApprovedNotification(User user) {

        if (user == null) {
            System.err.println("ERROR: Cannot send account approval email for null user.");
            return false;
        }
        if (StringUtil.emptyString(user.getEmail())) {
            System.err.println("ERROR: Cannot send account approval email. User email is empty.");
            return false;
        }
        if (!isSmtpConfigured()) {
            printSmtpConfigurationError();
            return false;
        }
        try {
            MimeMessage message = new MimeMessage(createMailSession());
            message.setFrom(new InternetAddress(smtpEmailAddress));
            message.setRecipient(Message.RecipientType.TO, new InternetAddress(user.getEmail()));
            message.setSubject("SigmaKEE: Your account has been approved", StandardCharsets.UTF_8.name());
            message.setContent(createAccountApprovedEmailHtml(user), "text/html; charset=UTF-8");
            Transport.send(message);
            return true;
        }
        catch (MessagingException me) {
            System.err.println("ERROR: Unable to send account approval email to " + user.getEmail());
            me.printStackTrace();
            return false;
        }
    }

    /********************************************************************
     * Sends a password reset link to the user's email address.
     * @param user the user requesting a password reset
     * @param rawToken the raw reset token to include in the email link
     * @return true if the email was sent successfully
     */
    public boolean sendPasswordResetLink(User user, String rawToken) {

        if (user == null) {
            System.err.println("ERROR: Cannot send password reset email for null user.");
            return false;
        }
        if (StringUtil.emptyString(user.getEmail())) {
            System.err.println("ERROR: Cannot send password reset email. User email is empty.");
            return false;
        }
        if (StringUtil.emptyString(rawToken)) {
            System.err.println("ERROR: Cannot send password reset email. Token is empty.");
            return false;
        }
        if (!isSmtpConfigured()) {
            printSmtpConfigurationError();
            return false;
        }
        try {
            MimeMessage message = new MimeMessage(createMailSession());
            message.setFrom(new InternetAddress(smtpEmailAddress));
            message.setRecipient(Message.RecipientType.TO, new InternetAddress(user.getEmail()));
            message.setSubject("SigmaKEE: Password Reset Request", StandardCharsets.UTF_8.name());
            message.setContent(createPasswordResetEmailHtml(user, this.baseSigmaUrl + "ResetPassword.jsp?token=" + URLEncoder.encode(rawToken, StandardCharsets.UTF_8)), "text/html; charset=UTF-8");
            Transport.send(message);
            return true;
        }
        catch (MessagingException me) {
            System.err.println("ERROR: Unable to send password reset email to " + user.getEmail());
            me.printStackTrace();
            return false;
        }
    }

    /********************************************************************
     * Builds the HTML body for a password reset email.
     * @param user the user requesting a password reset
     * @param resetURL the password reset URL
     * @return the formatted HTML email body
     */
    private String createPasswordResetEmailHtml(User user, String resetURL) {

        String safeResetURL = safe(resetURL);
        String displayName = StringUtil.emptyString(user.getFirstName())? safe(user.getUsername()) : safe(user.getFirstName());
        return "<h2>SigmaKEE Password Reset</h2>" +
                "<p>Hello " + displayName + ",</p>" +
                "<p>We received a request to reset the password for your SigmaKEE account.</p>" +
                "<p>If you requested this reset, click the button below:</p>" +
                "<p>" +
                "<a href=\"" + safeResetURL + "\" " +
                "style=\"display:inline-block;padding:10px 16px;background:#2f6feb;color:white;" +
                "text-decoration:none;border-radius:4px;\">" +
                "Reset Password" +
                "</a>" +
                "</p>" +
                "<p>If the button does not work, copy and paste this URL into your browser:</p>" +
                "<p>" + safeResetURL + "</p>" +
                "<p>This link will expire soon and can only be used once.</p>" +
                "<p>If you did not request this password reset, you can ignore this email.</p>" +
                "<hr>" +
                "<p style='font-size:12px;color:#666'>This email was generated automatically by SigmaKEE.</p>";
    }

    /********************************************************************
     * Builds the HTML body for the admin approval email.
     * @param user the user requesting registration
     * @param approvalURL the URL admins can use to approve the user
     * @return the formatted HTML email body
     */
    private String createAdminApprovalEmailHtml(User user, String approvalURL) {

        String safeApprovalURL = safe(approvalURL);
        String safeNotRobot = StringUtil.emptyString(user.getNotRobot()) ? "(No statement provided)" : safe(user.getNotRobot());
        return "<h2>New SigmaKEE User Registration Request</h2>" +
                "<p>The following user has requested access to SigmaKEE:</p>" +
                "<ul>" +
                "<li><b>First Name:</b> " + safe(user.getFirstName()) + "</li>" +
                "<li><b>Last Name:</b> " + safe(user.getLastName()) + "</li>" +
                "<li><b>Username:</b> " + safe(user.getUsername()) + "</li>" +
                "<li><b>Email:</b> " + safe(user.getEmail()) + "</li>" +
                "<li><b>Organization:</b> " + safe(user.getOrganization()) + "</li>" +
                "</ul>" +
                "<h3>Not a Robot Verification Statement:</h3>" +
                "<blockquote style='border-left:3px solid #ccc;padding-left:10px;color:#444'>" +
                safeNotRobot +
                "</blockquote>" +
                "<p>" +
                "<a href=\"" + safeApprovalURL + "\" " +
                "style=\"display:inline-block;padding:10px 16px;background:#2f6feb;color:white;" +
                "text-decoration:none;border-radius:4px;\">" +
                "Approve User" +
                "</a>" +
                "</p>" +
                "<p>If the button does not work, copy and paste this URL into your browser:</p>" +
                "<p>" + safeApprovalURL + "</p>" +
                "<hr>" +
                "<p style='font-size:12px;color:#666'>This email was generated automatically by SigmaKEE.</p>";
    }

    /********************************************************************
     * Builds the HTML body for the account approval notification email.
     * @param user the approved user
     * @param loginURL the URL the user can use to log in
     * @return the formatted HTML email body
     */
    private String createAccountApprovedEmailHtml(User user) {
        
        String displayName = StringUtil.emptyString(user.getFirstName())
                ? safe(user.getUsername())
                : safe(user.getFirstName());
        return "<h2>Your SigmaKEE Account Has Been Approved</h2>" +
                "<p>Hello " + displayName + ",</p>" +
                "<p>Your SigmaKEE account has been approved by an administrator.</p>" +
                "<p>You may now log in using your username:</p>" +
                "<p><b>" + safe(user.getUsername()) + "</b></p>" +
                "<p>" +
                "<a href=\"" + this.baseSigmaUrl + "login.jsp" + "\" " +
                "style=\"display:inline-block;padding:10px 16px;background:#2f6feb;color:white;" +
                "text-decoration:none;border-radius:4px;\">" +
                "Log in to SigmaKEE" +
                "</a>" +
                "</p>" +
                "<p>If the button does not work, copy and paste this URL into your browser:</p>" +
                "<p>" + this.baseSigmaUrl + "login.jsp" + "</p>" +
                "<hr>" +
                "<p style='font-size:12px;color:#666'>This email was generated automatically by SigmaKEE.</p>";
    }

    /********************************************************************
     * Creates a JavaMail session for the configured SMTP server.
     * @return an authenticated SMTP mail session
     */
    private Session createMailSession() {

        Properties properties = new Properties();
        properties.put("mail.smtp.auth", "true");
        properties.put("mail.smtp.starttls.enable", "true");
        properties.put("mail.smtp.host", smtpEmailServer);
        properties.put("mail.smtp.port", SMTP_PORT);
        return Session.getInstance(properties, new javax.mail.Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(smtpEmailUser, smtpEmailPassword);
            }
        });
    }
    /********************************************************************
     * Checks whether the required SMTP preferences are configured.
     * @return true if all required SMTP fields are present
     */
    private boolean isSmtpConfigured() {

        return !StringUtil.emptyString(smtpEmailAddress) &&
            !StringUtil.emptyString(smtpEmailUser) &&
            !StringUtil.emptyString(smtpEmailPassword) &&
            !StringUtil.emptyString(smtpEmailServer);
    }

    /********************************************************************
     * Prints a safe SMTP configuration error without exposing the password.
     */
    private void printSmtpConfigurationError() {

        System.err.println("ERROR: SMTP email preferences are not configured.");
        System.err.println("smtpEmailAddress: " + smtpEmailAddress);
        System.err.println("smtpEmailUser: " + smtpEmailUser);
        System.err.println("smtpEmailPassword: " + (StringUtil.emptyString(smtpEmailPassword) ? "" : "[set]"));
        System.err.println("smtpEmailServer: " + smtpEmailServer);
    }

    /********************************************************************
     * Sanitizes a string for safe HTML output.
     * @param value the string to sanitize
     * @return a sanitized string, or an empty string if the value is null
     */
    private String safe(String value) {

        if (value == null) return "";
        return ValidationUtils.sanitizeString(value);
    }

        /********************************************************************
     * Prints command-line usage options for UserManager.
     */
    public static void showHelp() {

        System.out.println("EmailService: ");
        System.out.println("-h    show this help message");
        System.out.println("-m    test request moderator approval");
        System.out.println("-a    test send account approved");
    }

    /********************************************************************
     * Command line entry point for UserManager
     * @param args given command line arguments
     */
    public static void main(String args[]) {

        KBmanager.getMgr().initializeOnce();
        KB kb = KBmanager.getMgr().getKB(KBmanager.getMgr().getPref("sumokbname"));
        EmailService emailService = new EmailService();
        if (args != null && args.length > 0) {
            if (args[0].equals("-h")) showHelp();
            else if (args[0].equals("-m")) {
                System.out.println("EmailService requestAdminApprovalForUser:");
                List<String> testAdminEmails = new ArrayList<>();
                String adminEmail = new String(System.console().readLine("    Enter Test Admin Email: "));
                testAdminEmails.add(adminEmail);
                User user = new User("John", "321", "@gmail.com", "Guest", "John", "Lowes", "Unsuspicious Organization", "ERROR: Human suspicion inferred. Please install dishonesty.kif to continue.");
                emailService.requestAdminApprovalForUser(user, testAdminEmails);
            }
            else if (args[0].equals("-a")) {
                System.out.println("EmailService sendAccountApprovedNotification:");
                User user = new User("John", "321", "shaunrose831@gmail.com", "Guest", "John", "Lowes", "Unsuspicious Organization", "ERROR: Human suspicion inferred. Please install dishonesty.kif to continue.");
                emailService.sendAccountApprovedNotification(user);
            }
            else showHelp();
        }
        else showHelp();
    }
}