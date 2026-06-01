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

    /********************************************************************
     * Creates a UserDatabase object and opens a database connection.
     */
    public UserDatabase() {
        if (debug>0) System.out.printf("\nUserDatabase()");
        try {
            Class.forName(H2_DRIVER);
            this.connection = DriverManager.getConnection(JDBC_ACCESS_DB, INITIAL_ADMIN_USER, "");
            if (debug>0) System.out.println("init(): Opened PASSWD DB via: " + JDBC_ACCESS_DB);
        }
        catch (ClassNotFoundException | SQLException e) {
            System.err.println("Error in UserDatabase(): " + e.getMessage());
            e.printStackTrace();
        }
    }

    /********************************************************************
     * Inserts a new user into the database if the username is available.
     * @param user the user to insert
     * @return true if the user was inserted successfully
     */
    public boolean insertUser(User user) {

        String username = user.getUsername().trim().toLowerCase();
        if (userExists(user.getUsername())) {
            System.err.println("Error in UserDatabase.addUser():  User " + username + " already exists!");
            return false;
        }
        return toDB(user);
    }

    /********************************************************************
     * Inserts a new admin user into the database.
     * @param username the admin user's username
     * @param password the admin user's password
     * @param email the admin user's email
     * @param firstName the admin user's first name
     * @param lastName the admin user's last name
     * @param organization the admin user's organization
     * @param notRobot the admin user's verification statement
     * @return true if the admin user was inserted successfully
     */
    public boolean insertAdmin(String username, String password, String email, String firstName, String lastName, String organization, String notRobot) {

        username = username.trim().toLowerCase();
        email = email.trim().toLowerCase();
        User user = new User(username, password, email, "admin", firstName, lastName, organization, notRobot);
        return insertUser(user);
    }

    /********************************************************************
     * Authenticates a user and returns their role if successful.
     * @param username the username to authenticate
     * @param password the password to authenticate
     * @return the user's role if authentication succeeds, or null otherwise
     */
    public String authenticateUser(String username, String password) {

        if (username == null || username.trim().isEmpty()) return null;
        if (password == null || password.isEmpty()) return null;
        username = username.trim().toLowerCase();
        String storedPassword;
        String role;
        String sql = "SELECT password, role FROM users WHERE username = ?";
        try (PreparedStatement statement = this.connection.prepareStatement(sql)) {
            statement.setString(1, username);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) return null;
                storedPassword = resultSet.getString("password");
                role = resultSet.getString("role");
            }
            if (storedPassword == null || role == null) return null;
            if (!PasswordService.verifyPassword(password, storedPassword)) return null;
            if (PasswordService.isLegacySha1Hash(storedPassword)) {
                updatePassword(username, password);
                System.out.println("UserDatabase.authenticateUser(): upgraded password hash to SHA-256 for " + username);
            }
            return role;
        }
        catch (SQLException e) {
            System.err.println("Error in UserDatabase.authenticateUser(): " + e.getMessage());
            e.printStackTrace();
        }
        return null;
    }

    /********************************************************************
     * Creates a password reset token record for a user.
     * @param username the username associated with the reset token
     * @param tokenHash the hashed reset token to store
     * @param expiresAt the timestamp when the reset token expires
     * @return true if the reset token was created successfully
     */
    public boolean createPasswordResetToken(String username, String tokenHash) {
        
        int minutesUntilExpiry = 30;
        username = normalizeUsername(username);
        if (username == null || username.isEmpty()) return false;
        if (tokenHash == null || tokenHash.trim().isEmpty()) return false;
        String sql =
            "INSERT INTO password_reset_tokens " +
            "(token_hash, username, expires_at) " +
            "VALUES (?, ?, ?)";
        try (PreparedStatement statement = this.connection.prepareStatement(sql)) {
            statement.setString(1, tokenHash);
            statement.setString(2, username);
            statement.setTimestamp(3, new Timestamp(System.currentTimeMillis() + (minutesUntilExpiry * 60 * 1000)));
            return statement.executeUpdate() > 0;
        }
        catch (SQLException e) {
            System.err.println("Error in UserDatabase.createPasswordResetToken(): " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /********************************************************************
     * Returns the user associated with a valid, unused password reset token.
     * @param tokenHash the hashed password reset token
     * @return the matching user, or null if the token is invalid, used, or expired
     */
    public User getUserForValidPasswordResetToken(String tokenHash) {

        if (tokenHash == null || tokenHash.trim().isEmpty()) return null;
        String sql =
                "SELECT u.username, u.email, u.role, u.firstName, u.lastName, u.organization, u.notRobot " +
                "FROM password_reset_tokens prt " +
                "JOIN users u ON u.username = prt.username " +
                "WHERE prt.token_hash = ? " +
                "AND prt.used_at IS NULL " +
                "AND prt.expires_at > CURRENT_TIMESTAMP";
        try (PreparedStatement statement = this.connection.prepareStatement(sql)) {
            statement.setString(1, tokenHash);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) return null;
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
            System.err.println("Error in UserDatabase.getUserForValidPasswordResetToken(): " + e.getMessage());
            e.printStackTrace();
        }
        return null;
    }

    /********************************************************************
     * Marks a password reset token as used.
     * @param tokenHash the hashed password reset token
     * @return true if the token was marked used
     */
    public boolean markPasswordResetTokenUsed(String tokenHash) {

        if (tokenHash == null || tokenHash.trim().isEmpty()) return false;
        String sql =
                "UPDATE password_reset_tokens " +
                "SET used_at = CURRENT_TIMESTAMP " +
                "WHERE token_hash = ? " +
                "AND used_at IS NULL";

        try (PreparedStatement statement = this.connection.prepareStatement(sql)) {
            statement.setString(1, tokenHash);
            return statement.executeUpdate() > 0;
        }
        catch (SQLException e) {
            System.err.println("Error in UserDatabase.markPasswordResetTokenUsed(): " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /********************************************************************
     * Invalidates all unused password reset tokens for a user.
     * @param username the username whose reset tokens should be invalidated
     * @return true if the operation completed successfully
     */
    public boolean invalidatePasswordResetTokensForUser(String username) {

        username = normalizeUsername(username);
        if (username == null || username.isEmpty()) return false;
        String sql =
                "UPDATE password_reset_tokens " +
                "SET used_at = CURRENT_TIMESTAMP " +
                "WHERE username = ? " +
                "AND used_at IS NULL";
        try (PreparedStatement statement = this.connection.prepareStatement(sql)) {
            statement.setString(1, username);
            statement.executeUpdate();
            return true;
        }
        catch (SQLException e) {
            System.err.println("Error in UserDatabase.invalidatePasswordResetTokensForUser(): " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /********************************************************************
     * Returns all usernames stored in the database.
     * @return a set of usernames
     */
    public Set<String> getAllUsernames() {

        Set<String> result = new HashSet<>();
        String sql = "SELECT username FROM users";
        try (Statement statement = connection.createStatement();
            ResultSet resultSet = statement.executeQuery(sql)) {
            while (resultSet.next()) result.add(resultSet.getString("username"));
        }
        catch (SQLException e) {
            System.err.println("Error in getAllUsernames(): " + e.getMessage());
            e.printStackTrace();
        }
        return result;
    }

    /********************************************************************
     * Returns the email addresses for all admin users.
     * @return a list of admin email addresses
     */
    public List<String> getAdminEmails() {

        List<String> adminEmails = new ArrayList<>();
        String sql = """
            SELECT email
            FROM users
            WHERE LOWER(role) = 'admin'
            AND email IS NOT NULL
            AND TRIM(email) <> ''
            """;
        try (PreparedStatement statement = this.connection.prepareStatement(sql);
            ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                String adminEmail = resultSet.getString("email");
                if (adminEmail != null && !adminEmail.trim().isEmpty()) adminEmails.add(adminEmail.trim());
            }
        }
        catch (SQLException e) {
            System.err.println("Error in UserDatabase.getAdminEmails(): " + e.getMessage());
            e.printStackTrace();
        }
        return adminEmails;
    }

    /********************************************************************
     * Loads a user from the database by email address.
     * @param email the email address to look up
     * @return the matching user, or null if not found
     */
    public User getUserByEmail(String email) {

        email = normalizeEmail(email);
        if (email == null || email.isEmpty()) return null;
        String sql =
                "SELECT username, email, role, firstName, lastName, organization, notRobot " +
                "FROM users " +
                "WHERE LOWER(email) = ?";
        try (PreparedStatement statement = this.connection.prepareStatement(sql)) {
            statement.setString(1, email);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) return null;
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
            System.err.println("Error in UserDatabase.getUserByEmail(): " + e.getMessage());
            e.printStackTrace();
        }
        return null;
    }

    /********************************************************************
     * Updates a user's password in the database.
     * @param username the username of the account to update
     * @param newPassword the new password to store
     * @return true if the password was updated successfully
     */
    public boolean updatePassword(String username, String newPassword) {

        username = normalizeUsername(username);
        if (username == null || username.isEmpty()) return false;
        if (newPassword == null || newPassword.isEmpty()) return false;
        String sql = "UPDATE users SET password = ? WHERE username = ?";
        try (PreparedStatement statement = this.connection.prepareStatement(sql)) {
            statement.setString(1, PasswordService.hashPassword(newPassword));
            statement.setString(2, username);
            boolean updated = statement.executeUpdate() > 0;
            if (updated) System.out.println("User.updatePassword(): password updated for " + username);
            return updated;
        }
        catch (SQLException e) {
            System.err.println("Error in User.updatePassword(): " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /********************************************************************
     * Updates a user's role in the database.
     * @param username the username of the account to update
     * @param role the new role to assign
     * @return true if the role was updated successfully
     */
    public boolean updateRole(String username, String role) {

        username = normalizeUsername(username);
        if (username == null || username.isEmpty()) return false;
        if (role == null || role.trim().isEmpty()) return false;
        String sql = "UPDATE users SET role = ? WHERE username = ?";
        try (PreparedStatement statement = this.connection.prepareStatement(sql)) {
            statement.setString(1, role.trim());
            statement.setString(2, username);
            return statement.executeUpdate() > 0;
        }
        catch (SQLException e) {
            System.err.println("Error in User.updateRole(): " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    /********************************************************************
     * Updates a user's email address in the database.
     * @param username the username of the account to update
     * @param email the new email address to store
     * @return true if the email was updated successfully
     */
    public boolean updateEmail(String username, String email) {
        
        username = username.trim().toLowerCase();
        email = email.trim().toLowerCase();
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
     * @param username the username of the account to delete
     * @return true if the user was deleted successfully
     */
    public boolean deleteUser(String username) {

        username = username.trim().toLowerCase();
        String sql = "DELETE FROM users WHERE username = ?";
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
     * Checks whether a username already exists in the database.
     * @param username the username to check
     * @return true if the username exists
     */
    public boolean userExists(String username) {

        username = normalizeUsername(username);
        if (username == null || username.isEmpty()) return false;
        String sql = "SELECT 1 FROM users WHERE username = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, username);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next();
            }
        }
        catch (SQLException e) {
            System.err.println("Error in userExistsDB(): " + e.getMessage());
            e.printStackTrace();
        }
        return false;
    }

    /********************************************************************
     * Drops user database tables in dependency-safe order.
     */
    private void dropTables() {

        try (Statement statement = this.connection.createStatement()) {
            statement.execute("DROP TABLE IF EXISTS password_reset_tokens");
            statement.execute("DROP TABLE IF EXISTS users");
        }
        catch (SQLException e) {
            System.err.println("Error in UserDatabase.dropTables(): " + e.getMessage());
            e.printStackTrace();
        }
    }

    /********************************************************************
     * Creates the users table if it does not already exist.
     */
    public void createUsersTable() {

        String sql =
                "CREATE TABLE IF NOT EXISTS users (" +
                "username VARCHAR(50) PRIMARY KEY, " +
                "password VARCHAR(255), " +
                "email VARCHAR(255) UNIQUE, " +
                "role VARCHAR(20), " +
                "firstName VARCHAR(100), " +
                "lastName VARCHAR(100), " +
                "organization VARCHAR(255), " +
                "notRobot VARCHAR(1000)" +
                ")";
        try (Statement statement = this.connection.createStatement()) {
            statement.execute(sql);
        }
        catch (SQLException e) {
            System.err.println("Error in UserDatabase.createUsersTable(): " + e.getMessage());
            e.printStackTrace();
        }
    }

    /********************************************************************
     * Creates the password reset token table if it does not already exist.
     */
    public void createPasswordResetTokenTable() {

        String sql =
                "CREATE TABLE IF NOT EXISTS password_reset_tokens (" +
                "token_hash VARCHAR(64) PRIMARY KEY, " +
                "username VARCHAR(50) NOT NULL, " +
                "expires_at TIMESTAMP NOT NULL, " +
                "used_at TIMESTAMP, " +
                "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
                "FOREIGN KEY (username) REFERENCES users(username) ON DELETE CASCADE" +
                ")";
        try (Statement statement = this.connection.createStatement()) {
            statement.execute(sql);
        }
        catch (SQLException e) {
            System.err.println("Error in UserDatabase.createPasswordResetTokenTable(): " + e.getMessage());
            e.printStackTrace();
        }
    }

    /********************************************************************
     * Creates a fresh user database schema.
     */
    public void createDB() {

        dropTables();
        createUsersTable();
        createPasswordResetTokenTable();
    }

    /********************************************************************
     * Loads a user from the database by username.
     * @param username the username to look up
     * @return the matching user, or null if not found
     */
    public User fromDB(String username) {

        username = username.trim().toLowerCase();
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
     * Saves a user to the database.
     * @param user the user to save
     * @return true if the user was saved successfully
     */
    public boolean toDB(User user) {

        String username = normalizeUsername(user.getUsername());
        String email = normalizeEmail(user.getEmail());
        if (username == null || username.isEmpty()) return false;
        if (email == null || email.isEmpty()) return false;
        if (user.getPassword() == null || user.getPassword().isEmpty()) return false;
        String sql = """
            INSERT INTO users
            (username, password, email, role, firstName, lastName, organization, notRobot)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?)
            """;
        try (PreparedStatement statement = this.connection.prepareStatement(sql)) {
            statement.setString(1, username);
            statement.setString(2, PasswordService.hashPassword(user.getPassword()));
            statement.setString(3, email);
            statement.setString(4, user.getRole());
            statement.setString(5, user.getFirstName());
            statement.setString(6, user.getLastName());
            statement.setString(7, user.getOrganization());
            statement.setString(8, user.getNotRobot());
            return statement.executeUpdate() > 0;
        }
        catch (SQLException e) {
            System.err.println("Error in UserDatabase.toDB(): " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    /********************************************************************
     * Shuts down the H2 database connection.
     * @return nothing
     */
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
     * Closes the H2 database connection without shutting down the database.
     */
    public void close() {

        try {
            if (this.connection != null && !this.connection.isClosed()) this.connection.close();
        }
        catch (SQLException e) {
            System.err.println("Error in UserDatabase.close(): " + e.getMessage());
            e.printStackTrace();
        }
    }

    /********************************************************************
     * Normalizes a username for consistent storage and lookup.
     * @param username the username to normalize
     * @return the normalized username, or null if username is null
     */
    private String normalizeUsername(String username) {

        if (username == null) return null;
        return username.trim().toLowerCase(java.util.Locale.ROOT);
    }

    /********************************************************************
     * Normalizes an email address for consistent storage.
     * @param email the email address to normalize
     * @return the normalized email, or null if email is null
     */
    private String normalizeEmail(String email) {

        if (email == null) return null;
        return email.trim().toLowerCase(java.util.Locale.ROOT);
    }

    /********************************************************************
     * Prints command-line usage options for UserDatabase.
     */
    public static void showHelp() {

        System.out.println("UserDatabase: ");
        System.out.println("-h    show this help message");
        System.out.println("-l    login");
        System.out.println("-c    create db");
        System.out.println("-a    create admin user");
    }

    /********************************************************************
     * Runs command-line user database operations.
     * @param args the command-line arguments
     */
    public static void main(String args[]) {
        UserDatabase db = new UserDatabase();
        PasswordService ps = new PasswordService();
        if (args != null && args.length > 0) {
            if (args[0].equals("-h")) showHelp();
            else if (args[0].equals("-e") && args[1] != null) System.out.println(PasswordService.hashPassword(args[1]));
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