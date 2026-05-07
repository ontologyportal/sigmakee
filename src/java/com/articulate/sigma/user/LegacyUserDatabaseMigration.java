package com.articulate.sigma.user;

import java.sql.*;

public class LegacyUserDatabaseMigration {

    private static final String JDBC_URL =
            "jdbc:h2:file:" + System.getProperty("user.home") + "/var/passwd;AUTO_SERVER=TRUE";

    private static final String INITIAL_ADMIN_USER = "sumo";

    /********************************************************************
     * Migrates legacy user tables into the new users table.
     * @param connection the database connection
     * @throws SQLException if migration fails
     */
    private static void migrate(Connection connection) throws SQLException {

        try (Statement statement = connection.createStatement()) {
            statement.execute("DROP TABLE IF EXISTS users_new");
            statement.execute("""
                CREATE TABLE users_new(
                    username varchar(50) primary key,
                    password varchar(255),
                    email varchar(255) unique,
                    role varchar(20),
                    firstName varchar(100),
                    lastName varchar(100),
                    organization varchar(255),
                    notRobot varchar(1000)
                )
                """);
            statement.executeUpdate("""
                INSERT INTO users_new
                    (username, password, email, role, firstName, lastName, organization, notRobot)
                SELECT
                    LOWER(TRIM(u.username)),
                    u.password,
                    a.email,
                    LOWER(TRIM(u.role)),
                    '',
                    '',
                    '',
                    'Migrated legacy account'
                FROM users u
                LEFT JOIN (
                    SELECT
                        LOWER(TRIM(username)) AS username,
                        MIN(NULLIF(LOWER(TRIM(email)), '')) AS email
                    FROM attributes
                    GROUP BY LOWER(TRIM(username))
                ) a
                ON LOWER(TRIM(u.username)) = a.username
                """);
            int oldCount = countRows(connection, "users");
            int newCount = countRows(connection, "users_new");
            if (oldCount != newCount) {
                statement.execute("DROP TABLE IF EXISTS users_new");
                throw new SQLException("Migration count mismatch. Old users: " +
                        oldCount + ", new users: " + newCount);
            }
            statement.execute("DROP TABLE IF EXISTS projects");
            statement.execute("DROP TABLE IF EXISTS attributes");
            statement.execute("DROP TABLE IF EXISTS users");
            statement.execute("ALTER TABLE users_new RENAME TO users");
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
     * Runs the legacy user database migration.
     * @param args command-line arguments
     */
    public static void main(String[] args) {

        try (Connection connection = DriverManager.getConnection(JDBC_URL, INITIAL_ADMIN_USER, "")) {
            connection.setAutoCommit(false);
            try {
                migrate(connection);
                connection.commit();
                System.out.println("Migration completed successfully.");
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