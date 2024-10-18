/** This code is copyright Articulate Software (c) 2005.
This software is released under the GNU Public License <http://www.gnu.org/copyleft/gpl.html>.
Users of this code also consent, by use of this code, to credit Articulate Software
and Ted Gordon in any writings, briefings, publications, presentations, or
other representations of any software which incorporates, builds on, or uses this
code.  */
package com.articulate.sigma;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import java.util.*;

/** *****************************************************************
 * A class that encrypts a string and checks it against another stored
 * encrypted string, in order to validate a user login.
 */
public class User {

    public String username;
      /** Encrypted password */
    public String password;
      /** A String which is one of: user, registered, administrator */
    public String role = "user";
      /** A HashMap of String keys and String values */
    public Map<String,String> attributes = new HashMap<>();
      /** An ArrayList of String keys consisting of unique project names. */
    public List<String> projects = new ArrayList();

    /** *****************************************************************
     */
    public String toString() {

        return username + "\n" + password + "\n" + role + "\n" + attributes.toString() + "\n" + projects.toString();
    }

    /** *****************************************************************
     *  Create a database with columns like this class
     */
    public static void createDB() {

        System.out.println("User.createDB(): creating database");
        Statement stmt;
        try (Connection conn = DriverManager.getConnection(PasswordService.JDBC_CREATE_DB, PasswordService.INITIAL_ADMIN_USER, "")) {
            System.out.println("User.createDB(): Opened DB " + PasswordService.JDBC_CREATE_DB);
            String str = "drop table if exists users;";
            stmt = conn.createStatement();
            stmt.execute(str);
            str = "create table users(username varchar(20), password varchar(40), role varchar(10));";
            stmt = conn.createStatement();
            stmt.execute(str);
            str = "drop table if exists attributes;";
            stmt = conn.createStatement();
            stmt.execute(str);
            str = "create table attributes(username varchar(20), id varchar(50), email varchar(50));";
            stmt = conn.createStatement();
            stmt.execute(str);
            str = "drop table if exists projects;";
            stmt = conn.createStatement();
            stmt.execute(str);
            str = "create table projects(username varchar(20), project varchar(50));";
            stmt = conn.createStatement();
            stmt.execute(str);
            stmt.close();
        }
        catch (SQLException e) {
            System.err.println("Error in User.createDB(): " + e.getMessage());
            e.printStackTrace();
        }
    }

    /** *****************************************************************
     *  Load the object from a relational DB
     */
    public static User fromDB(Connection conn, String username) {

        String str = "select * from users where username='" + username + "';";
        try {
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(str);
            User user = new User();
            user.username = username;
            if (!rs.next()) {
                System.err.println("fromDB(): no user " + username);
                return null;
            }
            else {
                user.password = rs.getString("password");
                user.role = rs.getString("role");
            }
            str = "select * from attributes where username='" + username + "';";
            stmt = conn.createStatement();
            rs = stmt.executeQuery(str);
            String key;
            String value;
            while (rs.next()) {
                key = rs.getString("id");
                value = rs.getString("email");
                user.attributes.put(key,value);
            }
            str = "select * from projects where username='" + username + "';";
            stmt = conn.createStatement();
            rs = stmt.executeQuery(str);
            String proj;
            while (rs.next()) {
                proj = rs.getString("project");
                user.projects.add(proj);
            }
            //System.out.println("fromDB(): " + user);
            stmt.close();
            return user;
        }
        catch (SQLException e) {
            System.err.println("Error in fromDB(): " + e.getMessage());
            e.printStackTrace();
        }
        return null;
    }

    /** *****************************************************************
     *  save the object in the relational DB
     */
    public void toDB(Connection conn) {

        try {
            String str = "insert into users(username,password,role) values ('" + this.username +
                    "', '" + this.password + "', '" + this.role + "');";
            //System.out.println("toDB(): " + str);
            Statement stmt = conn.createStatement();
            stmt.execute(str);
            for (String attrib : attributes.keySet()) {
                str = "insert into attributes(username,id,email) values ('" + this.username +
                        "', '" + attrib +
                        "', '" + this.attributes.get(attrib) + "');";
                stmt = conn.createStatement();
                stmt.execute(str);
            }
            for (String proj : projects) {
                str = "insert into projects(username,project) values ('" + this.username +
                        "', '" + proj + "');";
                stmt = conn.createStatement();
                stmt.execute(str);
            }
            stmt.close();
        }
        catch (SQLException e) {
            System.err.println("Error in toDB(): " + e.getMessage());
            e.printStackTrace();
        }
    }

    /** *****************************************************************
     * toggle user role between 'guest' and 'user'
     */
    public void toggleRole(Connection conn) {

        String newRole = role;
        if (role.equals("user"))
            newRole = "guest";
        else if (role.equals("guest"))
            newRole = "user";
        try {
            String str = "update users set role='" + newRole + "' where username='" + this.username + "';";
            //System.out.println("toDB(): " + str);
            try (Statement stmt = conn.createStatement()) {
                stmt.execute(str);
                System.out.println("User.toggleRole(): " + this.username + " is now a " + newRole);
            }
        }
        catch (SQLException e) {
            System.err.println("Error in toDB(): " + e.getMessage());
            e.printStackTrace();
        }

    }

    /** *****************************************************************
     */
    public String getRole() {

        return role;
    }

    /** *****************************************************************
     */
    public static void main(String args[]) {

    }

}
