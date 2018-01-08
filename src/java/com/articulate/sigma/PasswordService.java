/** This code is copyright Articulate Software (c) 2005.  
This software is released under the GNU Public License <http://www.gnu.org/copyleft/gpl.html>.
Users of this code also consent, by use of this code, to credit Articulate Software
and Ted Gordon in any writings, briefings, publications, presentations, or 
other representations of any software which incorporates, builds on, or uses this 
code.  */
package com.articulate.sigma;

import org.h2.tools.Server;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.*;
import java.io.*;

import javax.mail.*;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

/** *****************************************************************
 * A class that encrypts a string and checks it against another stored
 * encrypted string, in order to validate a user login.
 */
public final class PasswordService {

    private static PasswordService instance;
    private static HashMap users = new HashMap();

    // open the password DB as a server so both Sigma and SigmaNLP can access at once
    public static final String JDBCString = "jdbc:h2:tcp://localhost/~/var/passwd";
    public static String UserName = "";
    public Connection conn = null;
  
    /** ***************************************************************** 
     * Create an instance of PasswordService
     */
    public PasswordService() {

        try {
            Class.forName("org.h2.Driver");
            Server server = Server.createTcpServer().start();
            conn = DriverManager.getConnection(JDBCString, UserName, "");
            System.out.println("main(): Opened DB " + JDBCString);
        }
        catch (Exception e) {
            System.out.println("Error in main(): " + e.getMessage());
            e.printStackTrace();
        }
    }

    /** *****************************************************************
     * Encrypts a string with a deterministic algorithm.  Thanks to
     * https://howtodoinjava.com/security/how-to-generate-secure-password-hash-md5-sha-pbkdf2-bcrypt-examples/
     */
    public synchronized String encrypt(String plaintext) {

        plaintext = plaintext.trim();
        String generatedPassword = null;
        //System.out.println("PasswordService.encrypt(): input: '" + plaintext + "'");
        try {
            // Create MessageDigest instance for MD5
            MessageDigest md = MessageDigest.getInstance("SHA-1");
            //Add password bytes to digest
            md.update(plaintext.getBytes());
            //Get the hash's bytes
            byte[] bytes = md.digest();
            //This bytes[] has bytes in decimal format. Convert it to hexadecimal format
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < bytes.length; i++)
                sb.append(Integer.toString((bytes[i] & 0xff) + 0x100, 16).substring(1));
            //Get complete hashed password in hex format
            generatedPassword = sb.toString();
        }
        catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        //System.out.println("PasswordService.encrypt(): output: " + generatedPassword);
        return generatedPassword;
    }

    /** ***************************************************************** 
     */
    public static synchronized PasswordService getInstance() {

        if (instance == null)
            instance = new PasswordService();
        return instance;        
    }

    /** *****************************************************************
     * Take a user name and an encrypted password and compare it to an
     * existing collection of users with encrypted passwords.
     */
    public boolean authenticate(String username, String pass) {

        if (userExists(username)) {
            User user = User.fromDB(conn,username);
            //System.out.println("INFO in PasswordService.authenticateDB(): Input: " + username + " " + pass);
            //System.out.println("INFO in PasswordService.authenticateDB(): Reading: " + user.username + " " + user.password);
            if (pass.equals(user.password)) {
                return true;
            }
            else
                return false;
        }
        else
            return false;
    }

    /** *****************************************************************
     */
    public boolean userExists(String username) {

        try {
            Statement stmt = conn.createStatement();
            ResultSet res = stmt.executeQuery("SELECT * FROM USERS where username='" + username + "';");
            return res.next();
        }
        catch (Exception e) {
            System.out.println("Error in userExistsDB(): " + e.getMessage());
            e.printStackTrace();
        }
        return true;
    }

    /** *****************************************************************
     */
    public void addUser(User user) {

        if (userExists(user.username)) {
            System.out.println("Error in PasswordService.addUser():  User " + user.username + " already exists.");
            return;
        }
        user.toDB(conn);
    }

    /** *****************************************************************
     */
    public Set<String> userIDs() {

        HashSet<String> result = new HashSet<>();
        try {
            Statement stmt = conn.createStatement();
            ResultSet res = stmt.executeQuery("SELECT username FROM USERS;");
            while (!res.isLast()) {
                res.next();
                result.add(res.getString(1));
            }
        }
        catch (Exception e) {
            System.out.println("Error in userIDs(): " + e.getMessage());
            e.printStackTrace();
        }
        return result;
    }

    /** *****************************************************************
     */
    public void deleteUser(String uname) {

        HashSet<String> result = new HashSet<>();
        try {
            Statement stmt = conn.createStatement();
            stmt.execute("delete FROM USERS where username='" + uname + "';");
        }
        catch (Exception e) {
            System.out.println("Error in deleteUser(): " + e.getMessage());
            e.printStackTrace();
            return;
        }
        System.out.println("PasswordService.deleteUser(): deleted user " + uname);
    }

    /** *****************************************************************
     */
    public void login() {

        Console c = System.console();
        if (c == null) {
            System.err.println("No console.");
            System.exit(1);
        }

        String username = c.readLine("Enter your username: ");
        String password = new String(c.readPassword("Enter your password: "));
        //System.out.println("password: " + password);
        if (userExists(username)) {
            boolean valid = authenticate(username,encrypt(password));
            if (valid) {
                //System.out.println(User.fromDB(conn, username));
                System.out.println("login successful");
            }
            else
                System.out.println("Invalid username/password");
        }
        else
            System.out.println("User " + username + " does not exist");
    }

    /** *****************************************************************
     */
    public void mailModerator(User user) {

        String destmailid = user.attributes.get("email");
        String from = System.getenv("SIGMA_EMAIL_ADDRESS");
        String firstName = user.attributes.get("firstName");
        String lastName = user.attributes.get("lastName");
        String username = user.username;
        String notRobot = user.attributes.get("notRobot");
        String registrId = user.attributes.get("registrId");
        final String pwd = System.getenv("SIGMA_EMAIL_PASS");
        final String uname = System.getenv("SIGMA_EMAIL_USER");
        System.out.println("mailModerator(): uname: " + uname); // the system username for the email server

        String host = KBmanager.getMgr().getPref("hostname");
        String port = KBmanager.getMgr().getPref("port");
        String appURL = "";
        try {
            String https = KBmanager.getMgr().getPref("https");
            if (https == null || !https.equals("true"))
                https = "http";
            else
                https = "https";
            appURL = "ModeratorApproval.jsp?user=" +
                    username + "&id=" + URLEncoder.encode(registrId, "UTF-8");
            if (!StringUtil.emptyString(host) && !StringUtil.emptyString(port))
                appURL = https + "://" + host + ":" + port + "/sigma/" + appURL;
        }
        catch (UnsupportedEncodingException uee) {
            uee.printStackTrace();
        }

        String smtphost = System.getenv("SIGMA_EMAIL_SERVER");
        System.out.println("mailModerator(): host: " + smtphost);
        Properties propvls = new Properties();
        propvls.put("mail.smtp.auth", "true");
        propvls.put("mail.smtp.starttls.enable", "true");
        propvls.put("mail.smtp.host", smtphost);
        propvls.put("mail.smtp.port", "587");
        //Create a Session object & authenticate uid and pwd
        Session sessionobj = Session.getInstance(propvls,
                new javax.mail.Authenticator() {
                    protected PasswordAuthentication getPasswordAuthentication() {
                        return new PasswordAuthentication(uname, pwd);
                    }
                });
        try {
            //Create MimeMessage object & set values
            Message messageobj = new MimeMessage(sessionobj);
            messageobj.setFrom(new InternetAddress(from));
            messageobj.setRecipients(Message.RecipientType.TO,InternetAddress.parse(destmailid));
            messageobj.setSubject("Registration request from " + firstName + " " + lastName);
            messageobj.setText("Thank you for registering on ontologyportal!  " +
                    "Reply to this message to complete registration by submitting your request to the moderator.\n\n" +
                    "Dear Moderator, please approve this <a href=\"" + appURL + "\">request</a>");
            Transport.send(messageobj);
        }
        catch (MessagingException exp) {
            throw new RuntimeException(exp);
        }
    }

    /** *****************************************************************
     */
    public void register() {

        System.out.println("Register");
        Console c = System.console();
        if (c == null) {
            System.err.println("No console.");
            System.exit(1);
        }
        String login = c.readLine("Enter your login: ");
        String password = new String(c.readPassword("Enter your password: "));
        if (userExists(login))
            System.out.println("User " + login + " already exists");
        else {
            String email = new String(c.readLine("Enter your email address: "));
            User u = new User();
            u.username = login;
            u.password = encrypt(password);
            u.role = "guest";
            u.attributes.put("email",email);
            u.attributes.put("registrId",encrypt(Long.valueOf(System.currentTimeMillis()).toString()));
            addUser(u);
            mailModerator(u);
        }
    }

    /** *****************************************************************
     */
    public void createAdmin() {

        System.out.println("Create admin");
        Console c = System.console();
        if (c == null) {
            System.err.println("No console.");
            System.exit(1);
        }
        String login = c.readLine("Enter your login: ");
        String password = new String(c.readPassword("Enter your password: "));
        if (userExists(login))
            System.out.println("User " + login + " already exists");
        else {
            String email = new String(c.readLine("Enter your email address: "));
            User u = new User();
            u.username = login;
            u.password = encrypt(password);
            u.role = "admin";
            u.attributes.put("email",email);
            u.attributes.put("registrId",encrypt(Long.valueOf(System.currentTimeMillis()).toString()));
            addUser(u);
        }
    }

    /** *****************************************************************
     */
    public static void showHelp() {

        System.out.println("PasswordService: ");
        System.out.println("-h    show this Help message");
        System.out.println("-l    Login");
        System.out.println("-r    Register a new username and password (fail if username taken)");
        System.out.println("-c    Create db");
        System.out.println("-a    create Admin user");
        System.out.println("-u    show User IDs");
        System.out.println("-f <id>    find user with given ID");
        System.out.println("-d <id>    Delete user with given ID");
    }

    /** ***************************************************************** 
     */
    public static void main(String args[]) {

        PasswordService ps = new PasswordService();
        if (args != null) {
            if (args.length > 0 && args[0].equals("-r"))
                ps.register();
            else if (args.length > 0 && args[0].equals("-l"))
                ps.login();
            else if (args.length > 0 && args[0].equals("-c"))
                User.createDB();
            else if (args.length > 0 && args[0].equals("-a"))
                ps.createAdmin();
            else if (args.length > 0 && args[0].equals("-u"))
                System.out.println(ps.userIDs());
            else if (args.length > 1 && args[0].equals("-f"))
                System.out.println(User.fromDB(ps.conn,args[1]));
            else if (args.length > 1 && args[0].equals("-d"))
                ps.deleteUser(args[1]);
            else {
                System.out.println("unrecognized command\n");
                showHelp();
            }
        }
        else
            showHelp();
    }

}
