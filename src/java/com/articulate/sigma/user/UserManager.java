/** 
This code is copyright Articulate Software (c) 2005.
This software is released under the GNU Public License <http://www.gnu.org/copyleft/gpl.html>.
Users of this code also consent, by use of this code, to credit Articulate Software
and Ted Gordon in any writings, briefings, publications, presentations, or
other representations of any software which incorporates, builds on, or uses this
code.  
*/
package com.articulate.sigma.user;

import com.articulate.sigma.utils.StringUtil;
import com.articulate.sigma.security.ValidationUtils;
import com.articulate.sigma.*;

import java.net.URLEncoder;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.SQLException;
import java.util.*;
import java.io.*;

import static java.lang.System.exit;

import javax.mail.*;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

/********************************************************************
 * A class that encrypts a string and checks it against another stored
 * encrypted string, in order to validate a user login.
 */
@WebListener
public final class UserManager implements ServletContextListener {

    private int debug = 0;
    private UserDatabase userDatabase; 

    /********************************************************************
     * Constructs a new instance of UserManager
     */
    public UserManager() {

        this.userDatabase = new UserDatabase();
    }

    /********************************************************************
     * Creates a new user account after confirming admin access.
     * @param request the current HTTP request
     * @param username the new user's username
     * @param password the new user's password
     * @param email the new user's email
     * @param role the user's role: user, admin, or guest
     * @param firstName the user's first name
     * @param lastName the user's last name
     * @param organization the user's organization
     * @param notRobot the user's verification statement
     * @return true if the user was created successfully
     */
    public boolean createUser(HttpServletRequest request,
                          String username,
                          String password,
                          String email,
                          String role,
                          String firstName,
                          String lastName,
                          String organization,
                          String notRobot) {

        requireAdmin(request);
        if (!"user".equals(role) && !"admin".equals(role) && !"guest".equals(role)) {
            throw new IllegalArgumentException("Invalid role: " + role);
        }
        User user = new User(username, password, email, role, firstName, lastName, organization, notRobot);
        return this.userDatabase.insertUser(user);
    }

    /********************************************************************
     * Returns all usernames after confirming admin access.
     * @param request the current HTTP request
     * @return a set of all usernames
     */
    public Set<String> getAllUsernames(HttpServletRequest request) {

        requireAdmin(request);
        return this.userDatabase.getAllUsernames();
    }

    /********************************************************************
     * Returns a user by username after confirming admin access.
     * @param request the current HTTP request
     * @param username the username to look up
     * @return the matching user, or null if not found
     */
    public User getUser(HttpServletRequest request, String username) {

        requireAdmin(request);
        return this.userDatabase.fromDB(username);
    }

    /********************************************************************
     * Updates a user's email address after confirming admin access.
     * @param request the current HTTP request
     * @param username the username of the account to update
     * @param email the new email address
     * @return true if the email was updated successfully
     */
    public boolean updateUserEmail(HttpServletRequest request, String username, String email) {

        requireAdmin(request);
        return this.userDatabase.updateEmail(username, email);
    }

    /********************************************************************
     * Verifies that the current request belongs to a logged-in admin user.
     * @param request the current HTTP request
     * @throws SecurityException if the request is invalid or the user is not an admin
     */
    private void requireAdmin(HttpServletRequest request) {

        if (request == null) throw new SecurityException("Request is required.");
        HttpSession session = request.getSession(false);
        if (session == null) throw new SecurityException("User must be logged in.");
        String username = (String) session.getAttribute("username");
        if (username == null || username.trim().isEmpty()) throw new SecurityException("User must be logged in.");
        User user = userDatabase.fromDB(username);
        if (user == null || !"admin".equals(user.getRole())) throw new SecurityException("Admin privileges required.");
    }

    /********************************************************************
     * Logs in a user and creates a new authenticated session.
     * @param request the current HTTP request
     * @param username the username to authenticate
     * @param password the password to authenticate
     * @return true if login succeeds
     */
    public boolean login(HttpServletRequest request, String username, String password) {

        if (request == null) return false;
        String role = this.userDatabase.authenticateUser(username, password);
        if (role == null) return false;
        HttpSession oldSession = request.getSession(false);
        if (oldSession != null) oldSession.invalidate();
        HttpSession newSession = request.getSession(true);
        newSession.setAttribute("username", username);
        newSession.setAttribute("role", role);
        newSession.setMaxInactiveInterval(60 * 60);
        return true;
    }

    /********************************************************************
     * Logs out the current user by invalidating their session.
     * @param request the current HTTP request
     * @return true if a session existed and was invalidated, false otherwise
     */
    public boolean logout(HttpServletRequest request) {

        if (request == null) return false;
        HttpSession session = request.getSession(false);
        if (session == null) return false;
        session.invalidate();
        return true;
    }

    /********************************************************************
     * Returns admin email addresses after confirming admin access.
     * @param request the current HTTP request
     * @return a list of admin email addresses
     */
    public List<String> getAdminEmails(HttpServletRequest request) {

        requireAdmin(request);
        return this.userDatabase.getAdminEmails();
    }

    /********************************************************************
     * Registers a new guest user account.
     * @param username the guest user's username
     * @param password the guest user's password
     * @param email the guest user's email
     * @param firstName the guest user's first name
     * @param lastName the guest user's last name
     * @param organization the guest user's organization
     * @param notRobot the guest user's verification statement
     * @return true if the guest user was registered successfully
     */
    public boolean registerGuest(String username, String password, String email, String firstName, String lastName, String organization, String notRobot) {
        
        User user = new User(username, password, email, "guest", firstName, lastName, organization, notRobot);
        return this.userDatabase.insertUser(user);
    }

    /********************************************************************
     * Updates a user's role after confirming admin access.
     * @param request the current HTTP request
     * @param username the username of the account to update
     * @param newRole the new role to assign
     * @return true if the role was updated successfully
     */
    public boolean updateUserRole(HttpServletRequest request, String username, String newRole) {

        requireAdmin(request);
        return this.userDatabase.updateRole(username, newRole);
    }

    /********************************************************************
     * Updates a user's password after confirming admin access.
     * @param request the current HTTP request
     * @param username the username of the account to update
     * @param newPassword the new password to assign
     * @return true if the password was updated successfully
     */
    public boolean updateUserPassword (HttpServletRequest request, String username, String newPassword) {

        requireAdmin(request);
        return this.userDatabase.updatePassword(username, newPassword);
    }

    /********************************************************************
     *
     */
    public void sendResetPasswordLink() {

    }

    /********************************************************************
     * Deletes a user account after confirming admin access.
     * @param request the current HTTP request
     * @param username the username of the account to delete
     * @return true if the user was deleted successfully
     */
    public boolean deleteUser(HttpServletRequest request, String username) {
        
        requireAdmin(request);
        return this.userDatabase.deleteUser(username);
    }

    /********************************************************************
     * Sends an account registration request email to the moderators.
     * @param user the user requesting registration
     * @return true if the email was sent successfully
     */
    public boolean mailModerator(User user) {

        if (debug>0) System.out.printf("\nUserManager.mailModerator(%s)", user);
        List<String> adminEmails = this.userDatabase.getAdminEmails();
        if (adminEmails.isEmpty()) {
            System.err.println("ERROR: No admin emails found. Cannot send moderator request.");
            return false;
        }
        String destmailid = String.join(",", adminEmails);
        String from = KBmanager.getMgr().getPref("smtpEmailAddress");
        final String uname = KBmanager.getMgr().getPref("smtpEmailUser");
        final String pwd = KBmanager.getMgr().getPref("smtpEmailPassword");
        String smtphost = KBmanager.getMgr().getPref("smtpEmailServer");
        String host = KBmanager.getMgr().getPref("hostname");
        String port = KBmanager.getMgr().getPref("port");
        String appURL = "";
        try {
            String https = KBmanager.getMgr().getPref("https");
            if (https == null || !https.equals("true")) https = "http";
            else https = "https";
            appURL = "ApproveUser.jsp?user=" + URLEncoder.encode(user.getUsername(), "UTF-8");
            if (!StringUtil.emptyString(host) && !StringUtil.emptyString(port)) appURL = https + "://" + host + ":" + port + "/sigma/" + appURL;
        }
        catch (UnsupportedEncodingException uee) {
            uee.printStackTrace();
            return false;
        }
        System.out.println("UserManager.mailModerator(): host: " + smtphost);
        Properties propvls = new Properties();
        propvls.put("mail.smtp.auth", "true");
        propvls.put("mail.smtp.starttls.enable", "true");
        propvls.put("mail.smtp.host", smtphost);
        propvls.put("mail.smtp.port", "587");
        Session sessionobj = Session.getInstance(propvls,
            new javax.mail.Authenticator() {
                @Override
                protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication(uname, pwd);
                }
            });
        try {
            Message messageobj = new MimeMessage(sessionobj);
            messageobj.setFrom(new InternetAddress(from));
            messageobj.setRecipients(Message.RecipientType.TO,InternetAddress.parse(destmailid));
            messageobj.setSubject("SigmaKEE: Account Registration Request from " + user.getFirstName() + " " + user.getLastName());
            String manageUsersURL = "https://" + host + ":" + port + "/sigma/ManageUsers.jsp";
            String loginURL       = "https://" + host + ":" + port + "/sigma/login.jsp";
            String safeNotRobot = (user.getNotRobot() == null || user.getNotRobot().trim().isEmpty()) ? "(No statement provided)" : ValidationUtils.sanitizeString(user.getNotRobot());
            String htmlMsg = createHtmlForAdminApprovalEmail(user);
            messageobj.setContent(htmlMsg, "text/html; charset=utf-8");
            Transport.send(messageobj);
            return true;
        }
        catch (MessagingException exp) {
            throw new RuntimeException(exp);
        }
    }

    /********************************************************************
     * Builds the HTML body for the admin approval email.
     * @param user the user requesting registration
     * @return the formatted HTML email body
     */
    private String createHtmlForAdminApprovalEmail(User user) {
        
        return """
            <h2>New SigmaKEE User Registration Request</h2>
            <p>The following user has requested access to SigmaKEE:</p>
            <ul>
                <li><b>First Name: </b> %s </li>
                <li><b>Last Name: </b> %s </li>
                <li><b>Username:</b> %s </li>
                <li><b>Email: </b> %s </li>
                <li><b>Organization:</b> %s </li>
            </ul>
            <h3>Not a Robot Verification Statement:</h3>
            <blockquote style='border-left:3px solid #ccc;padding-left:10px;color:#444'>
                %s
            </blockquote>
            <hr>
            <p style='font-size:12px;color:#666'>This email was generated automatically by SigmaKEE.</p>
            """.formatted(user.getFirstName(), user.getLastName(), user.getUsername(), user.getEmail(), user.getOrganization(), user.getNotRobots());
    }

    /********************************************************************
     * Logs that the UserManager servlet context has started.
     * https://howtodoinjava.com/security/how-to-generate-secure-password-hash-md5-sha-pbkdf2-bcrypt-examples/
     * @param servletContextEvent the servlet context initialization event
     */
    @Override
    public void contextInitialized(ServletContextEvent servletContextEvent) {
        System.out.println("Starting " + UserManager.class.getName() + "...");
    }

    /********************************************************************
     * Shuts down the user database when the servlet context is destroyed.
     * https://github.com/spring-projects/spring-boot/issues/21221
     * and: https://stackoverflow.com/questions/9972372/what-is-the-proper-way-to-close-h2
     * @param servletContextEvent the servlet context destruction event
     */
    @Override
    public void contextDestroyed(ServletContextEvent servletContextEvent) {

        if (debug > 0) System.out.printf("UserManager.contextDestroyed() for %s", UserManager.class.getName());
        if (this.userDatabase != null) this.userDatabase.shutdown();
    }

    /********************************************************************
     * Prints command-line usage options for UserManager.
     */
    public static void showHelp() {

        System.out.println("UserManager: ");
        System.out.println("-h    show this help message");
        System.out.println("-l    login");
        System.out.println("-c    create db");
        System.out.println("-a    create admin user");
    }

    /********************************************************************
     * Command line entry point for UserManager
     * @param args given command line arguments
     */
    public static void main(String args[]) {

        UserManager userManager = new UserManager();
        if (args != null && args.length > 0) {
            if (args[0].equals("-h")) showHelp();
            if (args[0].equals("-m")) {
                System.out.println("UserManager mailModerator:");
                User user = new User("John", "321", "roseshaun01@gmail.com", "Guest", "John", "Bose", "NPS", "Does not compute");
                userManager.mailModerator(user);
            }
        }
        else showHelp();
    }
}