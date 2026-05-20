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
import com.articulate.sigma.utils.*;

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
public final class UserManager {

    private int debug = 0;
    private UserDatabase userDatabase; 

    /********************************************************************
     * Constructs a new instance of UserManager
     */
    public UserManager() {

        this.userDatabase = new UserDatabase();
        LoggingUtils.log("User Manager Created!");
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
     * Returns the user associated with a valid password reset token.
     * @param tokenHash the hashed password reset token
     * @return the matching user, or null if the token is invalid or expired
     */
    public User getUserForValidPasswordResetToken(String tokenHash) {

        if (StringUtil.emptyString(tokenHash)) return null;
        return this.userDatabase.getUserForValidPasswordResetToken(tokenHash);
    }

    /********************************************************************
     * Resets a user's password using a valid password reset token.
     * @param tokenHash the hashed password reset token
     * @param username the username associated with the token
     * @param newPassword the new plaintext password
     * @return true if the password was reset successfully
     */
    public boolean resetPasswordWithToken(String tokenHash, String username, String newPassword) {

        if (StringUtil.emptyString(tokenHash)) return false;
        if (StringUtil.emptyString(username)) return false;
        if (StringUtil.emptyString(newPassword)) return false;

        User user = this.userDatabase.getUserForValidPasswordResetToken(tokenHash);

        if (user == null) return false;
        if (!username.equals(user.getUsername())) return false;

        boolean updated = this.userDatabase.updatePassword(username, newPassword);

        if (!updated) return false;

        this.userDatabase.markPasswordResetTokenUsed(tokenHash);
        this.userDatabase.invalidatePasswordResetTokensForUser(username);

        return true;
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
        if (this.userDatabase.insertUser(user)) {
            EmailService emailService = new EmailService();
            emailService.requestAdminApprovalForUser(user, this.userDatabase.getAdminEmails());
            return true;
        }
        else return false;
    }

    /********************************************************************
     * Requests a password reset email for a user.
     * @param email the user's email address
     * @return true if the request was processed
     */
    public boolean requestPasswordReset(String email) {

        if (StringUtil.emptyString(email)) return true;
        User user = this.userDatabase.getUserByEmail(email.trim());
        if (user == null) return true;

        String rawToken = PasswordService.generateResetToken();
        boolean created = this.userDatabase.createPasswordResetToken(user.getUsername(), PasswordService.hashResetToken(rawToken));
        if (!created) return true;
        EmailService emailService = new EmailService();
        emailService.sendPasswordResetLink(user, rawToken);
        return true;
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
     * Deletes a user account after confirming admin access.
     * @param request the current HTTP request
     * @param username the username of the account to delete
     * @return true if the user was deleted successfully
     */
    public boolean deleteUser(HttpServletRequest request, String username) {
        
        requireAdmin(request);
        return this.userDatabase.deleteUser(username);
    }

    public void shutdown() {

        if (debug > 0) System.out.printf("INFO  [UserManager.shutdown()] for %s%n", UserManager.class.getName());
        if (this.userDatabase != null) this.userDatabase.shutdown();
    }

    /********************************************************************
     * Prints command-line usage options for UserManager.
     */
    public static void showHelp() {

        System.out.println("UserManager: ");
        System.out.println("-h    show this help message");
    }

    /********************************************************************
     * Command line entry point for UserManager
     * @param args given command line arguments
     */
    public static void main(String args[]) {

        KBmanager.getMgr().initializeOnce();
        KB kb = KBmanager.getMgr().getKB(KBmanager.getMgr().getPref("sumokbname"));
        UserManager userManager = new UserManager();
        if (args != null && args.length > 0) {
            if (args[0].equals("-h")) showHelp();
            else showHelp();
        }
        else showHelp();
    }
}