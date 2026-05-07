package com.articulate.sigma.user;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Migrates the legacy Sigma user database schema into the newer UserDatabase schema.
 */
public class LegacyUserDatabaseMigration {

    private static final String JDBC_URL =
            "jdbc:h2:file:" + System.getProperty("user.home") + "/var/passwd;AUTO_SERVER=TRUE";

    private static final String H2_DRIVER = "org.h2.Driver";
    private static final String INITIAL_ADMIN_USER = "sumo";

    /********************************************************************
     * Migrates legacy user tables into the new users table.
     * @param connection the database connection
     * @throws SQLException if migration fails
     */
    private static void migrate(Connection connection) throws SQLException {

        validateLegacySchema(connection);
        validateSafeToMigrate(connection);

        try (Statement statement = connection.createStatement()) {
            statement.execute("DROP TABLE IF EXISTS users_new");
            statement.execute(
                    "CREATE TABLE users_new (" +
                    "username VARCHAR(50) PRIMARY KEY, " +
                    "password VARCHAR(255), " +
                    "email VARCHAR(255) UNIQUE, " +
                    "role VARCHAR(20), " +
                    "firstName VARCHAR(100), " +
                    "lastName VARCHAR(100), " +
                    "organization VARCHAR(255), " +
                    "notRobot VARCHAR(1000)" +
                    ")"
            );

            statement.executeUpdate(
                    "INSERT INTO users_new " +
                    "(username, password, email, role, firstName, lastName, organization, notRobot) " +
                    "SELECT " +
                    "LOWER(TRIM(u.username)), " +
                    "u.password, " +
                    "a.email, " +
                    "CASE LOWER(TRIM(u.role)) " +
                    "    WHEN 'administrator' THEN 'admin' " +
                    "    WHEN 'admin' THEN 'admin' " +
                    "    WHEN 'registered' THEN 'user' " +
                    "    WHEN 'user' THEN 'user' " +
                    "    WHEN 'guest' THEN 'guest' " +
                    "    ELSE 'user' " +
                    "END, " +
                    "'', " +
                    "'', " +
                    "'', " +
                    "'Migrated legacy account' " +
                    "FROM users u " +
                    "LEFT JOIN ( " +
                    "    SELECT " +
                    "        LOWER(TRIM(username)) AS username, " +
                    "        MIN(NULLIF(LOWER(TRIM(email)), '')) AS email " +
                    "    FROM attributes " +
                    "    GROUP BY LOWER(TRIM(username)) " +
                    ") a " +
                    "ON LOWER(TRIM(u.username)) = a.username"
            );

            int oldCount = countRows(connection, "users");
            int newCount = countRows(connection, "users_new");
            if (oldCount != newCount) {
                statement.execute("DROP TABLE IF EXISTS users_new");
                throw new SQLException("Migration count mismatch. Old users: " +
                        oldCount + ", new users: " + newCount);
            }

            renameTableIfExists(connection, "users", "users_legacy");
            renameTableIfExists(connection, "attributes", "attributes_legacy");
            renameTableIfExists(connection, "projects", "projects_legacy");

            statement.execute("ALTER TABLE users_new RENAME TO users");

            statement.execute(
                    "CREATE TABLE IF NOT EXISTS password_reset_tokens (" +
                    "token_hash VARCHAR(64) PRIMARY KEY, " +
                    "username VARCHAR(50) NOT NULL, " +
                    "expires_at TIMESTAMP NOT NULL, " +
                    "used_at TIMESTAMP, " +
                    "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
                    "FOREIGN KEY (username) REFERENCES users(username) ON DELETE CASCADE" +
                    ")"
            );
        }
    }

    /********************************************************************
     * Validates that the expected legacy tables exist before migration.
     * @param connection the database connection
     * @throws SQLException if required legacy tables are missing
     */
    private static void validateLegacySchema(Connection connection) throws SQLException {

        if (!tableExists(connection, "users")) {
            throw new SQLException("Legacy users table does not exist. Nothing to migrate.");
        }

        if (!tableExists(connection, "attributes")) {
            throw new SQLException("Legacy attributes table does not exist. Cannot migrate email addresses.");
        }

        if (!tableExists(connection, "projects")) {
            System.out.println("WARNING: Legacy projects table does not exist. Continuing without it.");
        }
    }

    /********************************************************************
     * Validates that normalized usernames and emails will not violate new constraints.
     * @param connection the database connection
     * @throws SQLException if unsafe duplicate data is found
     */
    private static void validateSafeToMigrate(Connection connection) throws SQLException {

        try (Statement statement = connection.createStatement()) {
            try (ResultSet resultSet = statement.executeQuery(
                    "SELECT LOWER(TRIM(username)) AS normalized_username, COUNT(*) AS count " +
                    "FROM users " +
                    "GROUP BY LOWER(TRIM(username)) " +
                    "HAVING COUNT(*) > 1"
            )) {
                if (resultSet.next()) {
                    throw new SQLException("Duplicate usernames after normalization. Example: " +
                            resultSet.getString("normalized_username"));
                }
            }

            try (ResultSet resultSet = statement.executeQuery(
                    "SELECT email, COUNT(*) AS count " +
                    "FROM ( " +
                    "    SELECT " +
                    "        LOWER(TRIM(username)) AS username, " +
                    "        MIN(NULLIF(LOWER(TRIM(email)), '')) AS email " +
                    "    FROM attributes " +
                    "    GROUP BY LOWER(TRIM(username)) " +
                    ") " +
                    "WHERE email IS NOT NULL " +
                    "GROUP BY email " +
                    "HAVING COUNT(*) > 1"
            )) {
                if (resultSet.next()) {
                    throw new SQLException("Duplicate emails after migration. Example: " +
                            resultSet.getString("email"));
                }
            }
        }
    }

    /********************************************************************
     * Counts rows in a table.
     * @param connection the database connection
     * @param tableName the table to count
     * @return the number of rows in the table
     * @throws SQLException if the count query fails
     */
    private static int countRows(Connection connection, String tableName) throws SQLException {

        String sql = "SELECT COUNT(*) FROM " + tableName;
        try (Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(sql)) {
            if (resultSet.next()) return resultSet.getInt(1);
        }
        return 0;
    }

    /********************************************************************
     * Renames a table if it exists, replacing any stale backup table of the same name.
     * @param connection the database connection
     * @param oldName the current table name
     * @param newName the new table name
     * @throws SQLException if the rename fails
     */
    private static void renameTableIfExists(Connection connection, String oldName, String newName) throws SQLException {

        if (!tableExists(connection, oldName)) return;

        try (Statement statement = connection.createStatement()) {
            statement.execute("DROP TABLE IF EXISTS " + newName);
            statement.execute("ALTER TABLE " + oldName + " RENAME TO " + newName);
        }
    }

    /********************************************************************
     * Checks whether a table exists.
     * @param connection the database connection
     * @param tableName the table name
     * @return true if the table exists
     * @throws SQLException if metadata lookup fails
     */
    private static boolean tableExists(Connection connection, String tableName) throws SQLException {

        DatabaseMetaData metadata = connection.getMetaData();

        try (ResultSet resultSet = metadata.getTables(null, null, tableName.toUpperCase(), null)) {
            if (resultSet.next()) return true;
        }

        try (ResultSet resultSet = metadata.getTables(null, null, tableName.toLowerCase(), null)) {
            return resultSet.next();
        }
    }

    /********************************************************************
     * Runs the legacy user database migration.
     * @param args command-line arguments
     */
    public static void main(String[] args) {

        try {
            Class.forName(H2_DRIVER);
        }
        catch (ClassNotFoundException e) {
            System.err.println("Could not load H2 driver: " + e.getMessage());
            e.printStackTrace();
            return;
        }

        try (Connection connection = DriverManager.getConnection(JDBC_URL, INITIAL_ADMIN_USER, "")) {
            connection.setAutoCommit(false);

            try {
                migrate(connection);
                connection.commit();
                System.out.println("Migration completed successfully.");
                System.out.println("Old tables were preserved as users_legacy, attributes_legacy, and projects_legacy.");
            }
            catch (SQLException e) {
                connection.rollback();
                System.err.println("Migration failed. Rolled back changes: " + e.getMessage());
                e.printStackTrace();
            }
            finally {
                connection.setAutoCommit(true);
            }
        }
        catch (SQLException e) {
            System.err.println("Could not connect to database: " + e.getMessage());
            e.printStackTrace();
        }
    }
}