/** This code is copyright Articulate Software (c) 2005.
This software is released under the GNU Public License <http://www.gnu.org/copyleft/gpl.html>.
Users of this code also consent, by use of this code, to credit Articulate Software
and Ted Gordon in any writings, briefings, publications, presentations, or
other representations of any software which incorporates, builds on, or uses this
code.  */
package com.articulate.sigma;

//import org.h2.tools.Server;

import com.articulate.sigma.utils.StringUtil;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.*;
import java.util.*;
import java.io.*;

import javax.mail.*;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import static java.lang.System.exit;

/** *****************************************************************
 * A class that encrypts a string and checks it against another stored
 * encrypted string, in order to validate a user login.
 */
public final class PasswordService {

    private static PasswordService instance;

    // open the password DB as a server so both Sigma and SigmaNLP can access at once
    public static final String JDBC_CREATE_DB = "jdbc:h2:file:" + System.getProperty("user.home") + "/var/passwd;AUTO_SERVER=TRUE";
    public static final String JDBC_ACCESS_DB = JDBC_CREATE_DB;
    public static final String INITIAL_ADMIN_USER = "sumo"; // <- initial user when creating DB
    public Connection conn = null; // <- for JSP

    /** *****************************************************************
     * Create an instance of PasswordService
     */
    private PasswordService() {

        System.out.println("PasswordService()");
        try {
            Class.forName("org.h2.Driver"); // <- redundant for local invocation, but the JSPs need this
            conn = DriverManager.getConnection(JDBC_ACCESS_DB, INITIAL_ADMIN_USER, "");
            System.out.println("init(): Opened DB via: " + JDBC_ACCESS_DB);
        }
        catch (ClassNotFoundException | SQLException e) {
            System.err.println("Error in PasswordService(): " + e.getMessage());
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
            return pass.equals(user.password);
        }
        else
            return false;
    }

    /** *****************************************************************
     */
    public boolean userExists(String username) {

        boolean result = false;
        try (Statement stmt = conn.createStatement();
            ResultSet res = stmt.executeQuery("SELECT * FROM USERS where username='" + username + "';")) {
            result = res.next();
        }
        catch (SQLException e) {
            System.err.println("Error in userExistsDB(): " + e.getMessage());
            e.printStackTrace();
        }
        return result;
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

        Set<String> result = new HashSet<>();
        try (Statement stmt = conn.createStatement();
            ResultSet res = stmt.executeQuery("SELECT username FROM USERS;")) {
            while (!res.isLast()) {
                res.next();
                result.add(res.getString(1));
            }
        }
        catch (SQLException e) {
            System.err.println("Error in userIDs(): " + e.getMessage());
            e.printStackTrace();
        }
        return result;
    }

    /** *****************************************************************
     */
    public void deleteUser(String uname) {

        try (Statement stmt = conn.createStatement()) {
            stmt.execute("delete FROM USERS where username='" + uname + "';");
        }
        catch (Exception e) {
            System.err.println("Error in deleteUser(): " + e.getMessage());
            e.printStackTrace();
            return;
        }
        System.out.println("PasswordService.deleteUser(): deleted user " + uname);
    }

    /** *****************************************************************
     */
    public void login() {

        System.out.println("Login");
        Console c = System.console();
        if (c == null) {
            System.err.println("No console.");
            exit(1);
        }
        String login = c.readLine("Enter your username: ");
        String password = new String(c.readPassword("Enter your password: "));
        //System.out.println("password: " + password);
        if (userExists(login)) {
            boolean valid = authenticate(login,encrypt(password));
            if (valid) {
                //System.out.println(User.fromDB(conn, username));
                System.out.println("login successful");
            }
            else
                System.out.println("Invalid username/password");
        }
        else
            System.out.println("User " + login + " does not exist");
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
                    @Override
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
            messageobj.setText("Thank you for registering on ontologyportal " + firstName + " " + lastName + ". " +
                    "Please reply to this message to complete your registration, which will submit your request to the moderator.\n\n" +
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
            exit(1);
        }
        String login = c.readLine("Enter your username: ");
        String password = new String(c.readPassword("Enter your password: "));
        if (userExists(login))
            System.out.println("User " + login + " already exists");
        else {
            String email = c.readLine("Enter your email address: ");
            User u = new User();
            u.username = login;
            u.password = encrypt(password);
            u.role = "guest";
            u.attributes.put("email",email);
            u.attributes.put("registrId",encrypt(Long.toString(System.currentTimeMillis())));
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
            exit(1);
        }
        String login = c.readLine("Enter your username: ");
        String password = new String(c.readPassword("Enter your password: "));
        if (userExists(login))
            System.out.println("User " + login + " already exists");
        else {
            String email = c.readLine("Enter your email address: ");
            User u = new User();
            u.username = login;
            u.password = encrypt(password);
            u.role = "admin";
            u.attributes.put("email",email);
            u.attributes.put("registrId",encrypt(Long.toString(System.currentTimeMillis())));
            addUser(u);
        }
    }

    /** *****************************************************************
     */
    public void createAdmin3(String user, String p, String e) {

        System.out.println("Create admin");
        String login = user;
        String password = p;
        if (userExists(login))
            System.err.println("Error: User " + login + " already exists");
        else {
            String email = e;
            User u = new User();
            u.username = login;
            u.password = encrypt(password);
            u.role = "admin";
            u.attributes.put("email",email);
            u.attributes.put("registrId",encrypt(Long.toString(System.currentTimeMillis())));
            addUser(u);
        }
    }

    /** *****************************************************************
     */
    public void createUser(String user) {

        System.out.println("Create user " +user);
        Console c = System.console();
        if (c == null) {
            System.err.println("No console.");
            exit(1);
        }
        String password = new String(c.readPassword("Enter user password: "));
        if (userExists(user))
            System.err.println("User " + user + " already exists");
        else {
            String email = c.readLine("Enter your email address: ");
            User u = new User();
            u.username = user;
            u.password = encrypt(password);
            u.role = "user";
            u.attributes.put("email",email);
            u.attributes.put("registrId",encrypt(Long.toString(System.currentTimeMillis())));
            addUser(u);
        }
    }

    /** *****************************************************************
     */
    public void changeUserRole(String id) {

        System.out.println("Toggle user role between guest and user");
        Console c = System.console();
        if (c == null) {
            System.err.println("No console.");
            exit(1);
        }
        String login = c.readLine("Enter admin login: ");
        String password = new String(c.readPassword("Enter admin password: "));
        if (userExists(login) && authenticate(login,encrypt(password))) {
            User u = User.fromDB(conn,id);
            u.toggleRole(conn);
        }
        else
            System.err.println("invalid login");
    }

    /** *****************************************************************
     */
    public static void showHelp() {

        System.out.println("PasswordService: ");
        System.out.println("-h    show this Help message");
        System.out.println("-l    Login");
        System.out.println("-c    Create db");
        System.out.println("-a    create Admin user");

        System.out.println("-u    show User IDs");
        System.out.println("-r    Register a new username and password (fail if username taken)");
        System.out.println("-a3 <u> <p> <e>  create Admin user");
        System.out.println("-o <id>          change user role");
        System.out.println("-n <id>          create New guest user");
        System.out.println("-f <id>          find user with given ID");
        System.out.println("-d <id>          Delete user with given ID");
    }

    /** *****************************************************************
     */
    public static void main(String args[]) {

        PasswordService ps = PasswordService.getInstance();
        try {
            if (args != null && args.length > 0) {
                if (args[0].equals("-h"))
                    showHelp();
                else if (args[0].equals("-l"))
                    ps.login();
                else if (args[0].equals("-c"))
                    User.createDB();
                else if (args[0].equals("-a"))
                    ps.createAdmin();
                else if (args[0].equals("-u"))
                    System.out.println(ps.userIDs());
                else if (args[0].equals("-r"))
                    ps.register();
                else if (args.length > 3 && args[0].equals("-a3"))
                    ps.createAdmin3(args[1],args[2],args[3]);
                else if (args.length > 1 && args[0].equals("-o"))
                    ps.changeUserRole(args[1]);
                else if (args.length > 1 && args[0].equals("-n"))
                    ps.createUser(args[1]);
                else if (args.length > 1 && args[0].equals("-f"))
                    System.out.println(User.fromDB(ps.conn, args[1]));
                else if (args.length > 1 && args[0].equals("-d"))
                    ps.deleteUser(args[1]);
                else {
                    System.out.println("unrecognized command:" + args[0] + "\n");
                    showHelp();
                }
            }
            else
                showHelp();
        }
        finally {
            //finally block used to close resources
            try {
                if (ps != null && ps.conn != null)
                    ps.conn.close();
                System.out.println("PasswordService.main(): Closed DB");
                //Server.shutdownTcpServer(JDBCString,"", true, true);
            }
            catch(SQLException se){
                se.printStackTrace();
            }
        }
        System.out.println("completed PasswordService.main(): ");
    }

}
