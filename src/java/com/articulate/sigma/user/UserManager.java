/** 
This code is copyright Articulate Software (c) 2005.
This software is released under the GNU Public License <http://www.gnu.org/copyleft/gpl.html>.
Users of this code also consent, by use of this code, to credit Articulate Software
and Ted Gordon in any writings, briefings, publications, presentations, or
other representations of any software which incorporates, builds on, or uses this
code.  
*/
package com.articulate.sigma;

import com.articulate.sigma.user.User;
import com.articulate.sigma.utils.StringUtil;
import com.articulate.sigma.security.ValidationUtils;

import java.net.PasswordAuthentication;
import java.net.URLEncoder;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.*;
import java.util.*;
import java.io.*;

import static java.lang.System.exit;

import java.com.articulate.sigma.user.CurrentUser;
import java.com.articulate.sigma.usermanagement.UserDatabase;

import javax.mail.*;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;

/********************************************************************
 * A class that encrypts a string and checks it against another stored
 * encrypted string, in order to validate a user login.
 */
@WebListener
public final class UserManager implements ServletContextListener {

    private int debug = 0;
    private UserDatabase userDatabase; 

    /********************************************************************
     * Constructs a new instance of PasswordService
     */
    public UserManager() {

        this.userDatabase = new UserDatabase();
    }

    /********************************************************************
     */
    private void requireAdmin(HttpServletRequest request) {

        if (request == null) throw new SecurityException("Request is required.");
        HttpSession session = request.getSession(false);
        if (session == null) throw new SecurityException("User must be logged in.");
        String username = (String) session.getAttribute("username");
        if (username == null || username.trim().isEmpty()) throw new SecurityException("User must be logged in.");
        CurrentUser user = userDatabase.fromDB(username);
        if (user == null || !"admin".equals(user.getRole())) throw new SecurityException("Admin privileges required.");
    }

    /********************************************************************
     */
    public boolean login(String username, String password) {
        
        String role = this.userDatabase.authenticateUser(username, password);
        if (role != null) {
            session.setAttribute("currentUser", new CurrentUser(username, role));
            return true;
        } return false;
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
     */
    public boolean registerGuest(String username, String password, String email, String firstName, String lastName, String organization, String notRobot) {
        
        User user = new User(username, password, email, "guest", firstName, lastName, organization, notRobot);
        return this.userDatabase.insertUser(user);
    }

    public boolean updateUserRole(HttpServletRequest request, String username, String newRole) {

        requireAdmin(request);
        return this.userDatabase.updateRole(username, newRole);
    }

    public boolean updateUserPassword (HttpServletRequest request, String username, String newPassword) {

        requireAdmin(request);
        this.passwordDatabase.updatePassword(username, newPassword);
    }

    public void sendResetPasswordLink() {

    }

    /********************************************************************
     */
    public boolean deleteUser(HttpServletRequest request, String username) {
        
        requireAdmin(request);
        return this.userDatabase.deleteUser(username);
    }

    /********************************************************************
     * Sends the moderator an email requesting a user to be registered a
     * Sigma account
     * @param user the user information to register an account for
     */
    public void mailModerator(User user) {

        if (debug>0) System.out.printf("\nPasswordService.mailModerator() Admins:\n" + adminEmails);
        List<String> adminEmails = getAdminEmails();
        if (adminEmails.isEmpty()) {
            System.err.println("ERROR: No admin emails found. Cannot send moderator request.");
            return;
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
            appURL = "ModeratorApproval.jsp?user=" + user.getUsername() + "&id=" + URLEncoder.encode(user.getRegisterId(), "UTF-8");
            if (!StringUtil.emptyString(host) && !StringUtil.emptyString(port)) appURL = https + "://" + host + ":" + port + "/sigma/" + appURL;
        }
        catch (UnsupportedEncodingException uee) {
            uee.printStackTrace();
        }
        System.out.println("PasswordService.mailModerator(): host: " + smtphost);
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
            String loginURL       = "https://" + host + ":" + port + "/sigma/login.html";
            String safeNotRobot = (user.getNotRobot() == null || user.getNotRobot().trim().isEmpty()) ? "(No statement provided)" : ValidationUtils.sanitizeString(user.getNotRobot());
            String htmlMsg = createHtmlForAdminApprovalEmail(user);
            messageobj.setContent(htmlMsg, "text/html; charset=utf-8");
            Transport.send(messageobj);
        }
        catch (MessagingException exp) {
            throw new RuntimeException(exp);
        }
    }

    private string createHtmlForAdminApprovalEmail(User user) {
        
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
     * 
     */
    public static void showHelp() {

        System.out.println("PasswordService: ");
        System.out.println("-h    show this help message");
        System.out.println("-l    login");
        System.out.println("-c    create db");
        System.out.println("-a    create admin user");
        System.out.println("-u    show user IDs");
        System.out.println("-r    register a new guest username and password (fail if username taken)");
        System.out.println("-a3 <u> <p> <e>  create admin user");
        System.out.println("-o <id>          change user role");
        System.out.println("-n <id>          create new user");
        System.out.println("-f <id>          find user with given ID");
        System.out.println("-d <id>          delete user with given ID");
    }

    /********************************************************************
     * Command line entry point
     * @param args given command line arguments
     */
    public static void main(String args[]) {

        // UserManager ps = UserManager.getInstance();
        // try {
        //     if (args != null && args.length > 0) {
        //         if (args[0].equals("-h")) showHelp();
        //         else if (args[0].equals("-l")) ps.login();
        //         else if (args[0].equals("-c")) User.createDB();
        //         else if (args[0].equals("-a")) ps.createAdmin();
        //         else if (args[0].equals("-r")) ps.register();
        //         else if (args[0].equals("-u")) System.out.println(ps.userIDs());
        //         else if (args.length > 3 && args[0].equals("-a3")) ps.createAdmin(args[1], args[2], args[3]);
        //         else if (args.length > 1 && args[0].equals("-o")) ps.changeUserRole(args[1]);
        //         else if (args.length > 1 && args[0].equals("-n")) ps.createUser(args[1]);
        //         else if (args.length > 1 && args[0].equals("-d")) ps.deleteUser(args[1]);
        //         else if (args.length > 1 && args[0].equals("-f")) {
        //             User user = User.fromDB(ps.conn, args[1]);
        //             System.out.println(user == null ? "" : user);
        //         }
        //     }
        //     else showHelp();
        // }
        // finally {
        //     try {
        //         if (ps != null && ps.connection != null) ps.connection.close();
        //     }
        //     catch(SQLException se){
        //         se.printStackTrace();
        //     }
        // }
    }
}