package com.articulate.sigma.user;

import java.io.Console;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
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
        toDB(user);
        return true;
    }

    /********************************************************************
     * @param username
     * @param password
     * @param email
     * @param firstName
     * @param lastName
     * @param registerId
     */
    public void insertAdmin(String username, String password, String email, String firstName, String lastName, String organization) {

        User user = new User(username, password, email, "admin", firstName, lastName, organization, null);
        insertUser(user);
    }

    /********************************************************************
     * Take a user name and an encrypted password and compare it to an
     * existing collection of users with encrypted passwords.
     * @param username username to authenicate against DB
     * @param encryptedPassword password to authenticate against DB
     * @return User role
     */
    public String authenticateUser(String username, String password) {

        //if(debug>0) System.out.printf("PasswordService.authenticate(%s, %s)", username, encryptedPassword);
        if (userExists(username)) {
            User user = fromDB(username);
            if (encrypt(password).equals(user.getPassword())) return user.getRole();
            else return null;
        }
        else return null;
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
            query = "create table users(username varchar(20), password varchar(40), email varchar(20), role varchar(10), firstName varchar(20), lastName varchar(20), registerId varchar(20));";
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

        User user = null;
        String query = "SELECT * FROM users WHERE username='" + username + "';";
        try {
            Statement statement = this.connection.createStatement();
            ResultSet resultSet = statement.executeQuery(query);
            if (!resultSet.next()) {
                System.err.println("User.fromDB(): no user " + username);
                return null;
            }
            else {
                user = new User(
                    username, 
                    resultSet.getString("password"), 
                    resultSet.getString("email"), 
                    resultSet.getString("role"),
                    resultSet.getString("firstName"),
                    resultSet.getString("lastName"),
                    resultSet.getString("registerId"),
                    resultSet.getString("notRobots") 
                );
            }
            resultSet.close();
            statement.close();
        }
        catch (SQLException e) {
            System.err.println("Error in User.fromDB(): " + e.getMessage());
            e.printStackTrace();
        }
        return user;
    }

    /********************************************************************
     * Save the object in the relational DB
     * @param user 
     */
    public void toDB(User user) {

        try {
            String query = "insert into users(username,password,email,role,firstName,lastName,registerId) values ('" + 
                user.getUsername() + "', '" + 
                encrypt(user.getPassword()) + "', '" + 
                user.getEmail() + "', '" +
                user.getRole() + "', '" +
                user.getFirstName() + "', '" +
                user.getLastName() + "', '" +
                user.getRegisterId() + "', '" + 
                user.getOrganization() + "', '" + 
                "');";
            Statement statement = this.connection.createStatement();
            statement.execute(query);
            statement.close();
        }
        catch (SQLException e) {
            System.err.println("Error in User.toDB(): " + e.getMessage());
            e.printStackTrace();
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
}