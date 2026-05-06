package com.articulate.sigma.user;

import static java.lang.System.console;

import java.io.Console;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Scanner;
import java.util.Set;

import javax.servlet.ServletContextEvent;
import java.sql.*;

/** DO NOT USE THIS CLASS DIRECTLY IN JSP, ALWAYS USE UserManager */
public class UserDatabase {
    int debug = 0;
    private static final String JDBC_CREATE_DB = "jdbc:h2:file:" + System.getProperty("user.home") + "/var/passwd;AUTO_SERVER=TRUE";
    private static final String JDBC_ACCESS_DB = JDBC_CREATE_DB;
    private static final String H2_DRIVER = "org.h2.Driver";
    private static final String INITIAL_ADMIN_USER = "sumo";
    private Connection connection;

    public UserDatabase() {
        if (debug>0) System.out.printf("\nUserDatabase()");
        try {
            Class.forName(H2_DRIVER);
            this.connection = DriverManager.getConnection(JDBC_ACCESS_DB, INITIAL_ADMIN_USER, "");
            if (debug>0) System.out.println("init(): Opened PASSWD DB via: " + JDBC_ACCESS_DB);
        }
        catch (ClassNotFoundException | SQLException e) {
            System.err.println("Error in PasswordService(): " + e.getMessage());
            e.printStackTrace();
        }
    }

    /********************************************************************
     * Add a user to the DB
     * @param username the user name
     * @param password user password (will be encrypted)
     * @param email user email
     * @param role user role
     * @return an instance of the user to add to the DB
     */
    public boolean insertUser(User user) {

        if (userExists(user.getUsername())) {
            System.err.println("Error in PasswordService.addUser():  User " + user.getUsername() + " already exists!");
            return false;
        }
        return toDB(user);
    }

    /********************************************************************
     * @param username
     * @param password
     * @param email
     * @param firstName
     * @param lastName
     * @param organization
     */
    public boolean insertAdmin(String username, String password, String email, String firstName, String lastName, String organization, String notRobot) {

        User user = new User(username, password, email, "admin", firstName, lastName, organization, notRobot);
        return insertUser(user);
    }

    /********************************************************************
     * Take a user name and an encrypted password and compare it to an
     * existing collection of users with encrypted passwords.
     * @param username username to authenicate against DB
     * @param password password to authenticate against DB
     * @return User role
     */
    public String authenticateUser(String username, String password) {

        if (username == null || username.trim().isEmpty()) return null;
        if (password == null || password.isEmpty()) return null;
        String query = "SELECT password, role FROM users WHERE username = ?";
        try (PreparedStatement statement = this.connection.prepareStatement(query)) {
            statement.setString(1, username);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) return null;
                String storedPassword = resultSet.getString("password");
                String role = resultSet.getString("role");
                if (storedPassword == null || role == null) return null;
                String encryptedPassword = encrypt(password);
                if (encryptedPassword.equals(storedPassword)) return role;
                return null;
            }
        }
        catch (SQLException e) {
            System.err.println("Error in UserDatabase.authenticateUser(): " + e.getMessage());
            e.printStackTrace();
        }
        return null;
    }

    /********************************************************************
     * Returns all usernames in the user table of the database
     * @return Set of usernames found in DB
     */
    public Set<String> getAllUsernames() {

        Set<String> result = new HashSet<>();
        try (Statement query = connection.createStatement();
            ResultSet resultSet = query.executeQuery("SELECT username FROM USERS;")) {
            while (!resultSet.isLast()) {
                resultSet.next();
                result.add(resultSet.getString(1));
            }
        }
        catch (SQLException e) {
            System.err.println("Error in userIDs(): " + e.getMessage());
            e.printStackTrace();
        }
        return result;
    }

    /********************************************************************
     * Return a list of all admin users' email addresses.
     * @return List of admin email strings from DB
     */
    public List<String> getAdminEmails() {

        List<String> adminEmails = new ArrayList<>();
        try (Statement query = connection.createStatement();
            ResultSet resultSet = query.executeQuery("SELECT username FROM USERS WHERE role='admin';")) {
            while (resultSet.next()) {
                String username = resultSet.getString(1);
                User user = fromDB(username);
                if (user != null) {
                    String adminEmail = user.getEmail();
                    if (adminEmail != null && !adminEmail.trim().isEmpty()) adminEmails.add(adminEmail.trim());
                    else System.err.println("WARNING: Admin user " + username + " has no valid email.");
                }
            }
        }
        catch (SQLException e) {
            System.err.println("Error in getAdminEmails(): " + e.getMessage());
            e.printStackTrace();
        }
        return adminEmails;
    }

    /********************************************************************
     * Update just the password for this user
     */
    public boolean updatePassword(String username, String newPassword) {

        try (Statement stmt = this.connection.createStatement()) {
            String str = "update users set password='" + encrypt(newPassword) + "' where username='" + username + "';";
            stmt.execute(str);
            System.out.println("User.updatePassword(): password updated for " + username);
            return true;
        }
        catch (SQLException e) {
            System.err.println("Error in User.updatePassword(): " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /********************************************************************
     * Update the role for this user
     */
    public boolean updateRole(String username, String role) {

        try (Statement stmt = this.connection.createStatement()) {
            String query = "update users set role='" + role + "' where username='" + username + "';";
            stmt.execute(query);
            return true;
        }
        catch (SQLException e) {
            System.err.println("Error in User.updateRole(): " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    public boolean updateEmail(String username, String email) {

        String sql = "UPDATE users SET email = ? WHERE username = ?";
        try (PreparedStatement stmt = this.connection.prepareStatement(sql)) {
            stmt.setString(1, email);
            stmt.setString(2, username);
            return stmt.executeUpdate() > 0;
        }
        catch (SQLException e) {
            System.err.println("Error in UserDatabase.updateEmail(): " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /********************************************************************
     * Deletes a user from the database.
     * @param username the username to be deleted from the DB.
     * @return true if a user row was deleted, false otherwise
     */
    public boolean deleteUser(String username) {


        String sql = "DELETE FROM USERS WHERE username = ?";
        try (PreparedStatement query = connection.prepareStatement(sql)) {
            query.setString(1, username);
            return query.executeUpdate() > 0;
        }
        catch (SQLException e) {
            System.err.println("Error in deleteUser(): " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /********************************************************************
     * Check if this user already exists in the DB
     * @param username checks if this username exists in the DB
     */
    public boolean userExists(String username) {

        try (Statement query = connection.createStatement();
            ResultSet resultSet = query.executeQuery("SELECT * FROM USERS where username='" + username + "';")) {
            return resultSet.next();
        }
        catch (SQLException e) {
            System.err.println("Error in userExistsDB(): " + e.getMessage());
            e.printStackTrace();
        }
        return false;
    }

    /********************************************************************
     *  Create a database with columns like this class
     */
    public void createDB() {

        Statement statement;
        try {
            String query = "drop table if exists users;";
            statement = this.connection.createStatement();
            statement.execute(query);
            query = "create table users(" +
                "username varchar(50), " +
                "password varchar(255), " +
                "email varchar(255), " +
                "role varchar(20), " +
                "firstName varchar(100), " +
                "lastName varchar(100), " +
                "organization varchar(255), " +
                "notRobot varchar(1000)" +
                ");";
            statement = this.connection.createStatement();
            statement.execute(query);
            statement.close();
        }
        catch (SQLException e) {
            System.err.println("Error in User.createDB(): " + e.getMessage());
            e.printStackTrace();
        }
    }

    /********************************************************************
     * Load the object from a relational DB
     * @param username the user name to search for
     * @return the register user from the DB
     */
    public User fromDB(String username) {

        String query = """
            SELECT username, email, role, firstName, lastName, organization, notRobot
            FROM users
            WHERE username = ?
            """;
        try (PreparedStatement statement = this.connection.prepareStatement(query)) {
            statement.setString(1, username);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    System.err.println("User.fromDB(): no user " + username);
                    return null;
                }
                return new User(
                    resultSet.getString("username"),
                    null,
                    resultSet.getString("email"),
                    resultSet.getString("role"),
                    resultSet.getString("firstName"),
                    resultSet.getString("lastName"),
                    resultSet.getString("organization"),
                    resultSet.getString("notRobot")
                );
            }
        }
        catch (SQLException e) {
            System.err.println("Error in User.fromDB(): " + e.getMessage());
            e.printStackTrace();
        }
        return null;
    }

    /********************************************************************
     * Save the object in the relational DB
     * @param user 
     */
    public boolean toDB(User user) {

        try {
            String query = "insert into users(username,password,email,role,firstName,lastName,organization,notRobot) values ('" + 
                user.getUsername() + "', '" + 
                encrypt(user.getPassword()) + "', '" + 
                user.getEmail() + "', '" +
                user.getRole() + "', '" +
                user.getFirstName() + "', '" +
                user.getLastName() + "', '" +
                user.getOrganization() + "', '" + 
                user.getNotRobot() + 
                "');";
            Statement statement = this.connection.createStatement();
            statement.execute(query);
            statement.close();
            return true;
        }
        catch (SQLException e) {
            System.err.println("Error in UserDatabase.toDB(): " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    public void shutdown() {

        org.h2.Driver.unload();
        try (Statement stmt = this.connection.createStatement()) {
            stmt.execute("SHUTDOWN");
        }
        catch (SQLException e) {
            System.err.println(H2_DRIVER + " shutdown issues: " + e.getLocalizedMessage());
        }
    }

    /********************************************************************
     * Encrypts a string with a deterministic algorithm. Thanks to
     * https://howtodoinjava.com/security/how-to-generate-secure-password-hash-md5-sha-pbkdf2-bcrypt-examples/
     * @param password user password to be encrypted
     */
    public synchronized String encrypt(String password) {

        //if(debug>0) System.out.printf("PasswordService.encrypt(%s)", password);
        StringBuilder sb = new StringBuilder();
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-1");
            md.update(password.trim().getBytes());
            byte[] bytes = md.digest();
            for (int i = 0; i < bytes.length; i++) sb.append(Integer.toString((bytes[i] & 0xff) + 0x100, 16).substring(1));
        }
        catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return sb.toString();
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

    public static void main(String args[]) {
        UserDatabase db = new UserDatabase();
        if (args != null && args.length > 0) {
            if (args[0].equals("-h")) showHelp();
            else if (args[0].equals("-e") && args[1] != null) System.out.println(db.encrypt(args[1]));
            else if (args[0].equals("-c")) db.createDB();
            else if (args[0].equals("-a")) {
                Scanner scanner = new Scanner(System.in);
                System.out.print("Creating a new admin:\n");
                System.out.print("    Enter Username: ");
                String username = scanner.nextLine();
                if(db.userExists(username)) { System.out.println("User already exists!"); return; };
                String password = new String(System.console().readPassword("    Enter Password: "));
                System.out.print("    Enter email: ");
                String email = scanner.nextLine();
                System.out.print("    Enter First Name: ");
                String firstName = scanner.nextLine();
                System.out.print("    Enter Last Name: ");
                String lastName = scanner.nextLine();
                System.out.print("    Enter Organization: ");
                String organization = scanner.nextLine();
                System.out.print("    Why are you not a robot?: ");
                String notRobot = scanner.nextLine();
                if (db.insertAdmin(username, password, email, firstName, lastName, organization, notRobot)) System.out.println("Admin creation successful!");
                else System.out.println("Admin creation failed!");
            }
            else if (args[0].equals("-l")) {
                System.out.println("Logging in:");
                String username = new String(System.console().readLine("    Enter Username: "));
                String password = new String(System.console().readPassword("    Enter Password: "));
                String role = db.authenticateUser(username, password);
                if(role != null) System.out.println("Login successful! Role: " + role);
                else System.out.println("Login failed!");
            }
        }
        else showHelp();
        db.shutdown();
    }
}