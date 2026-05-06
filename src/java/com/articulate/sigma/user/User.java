/** This code is copyright Articulate Software (c) 2005.
This software is released under the GNU Public License <http://www.gnu.org/copyleft/gpl.html>.
Users of this code also consent, by use of this code, to credit Articulate Software
and Ted Gordon in any writings, briefings, publications, presentations, or
other representations of any software which incorporates, builds on, or uses this
code.  */
package com.articulate.sigma.user;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import java.util.*;

/********************************************************************
 * A model class representing a user of the system
 */
public class User {

    private String username;
    private String password;
    private String email;
    private String role = "guest";
    private String firstName;
    private String lastName;
    private String organization;
    private String notRobot;

    /********************************************************************
     * Constructs a new User object.
     * @param username
     * @param password
     * @param role
     * @param email
     * @param firstName
     * @param lastName
     * @param registerId
     * @param notRobot
     */
    public User(String username, String password, String email, String role, String firstName, String lastName, String organization, String notRobot) {
        
        this.username = username;
        this.password = password;
        this.role = role;
        this.email = email;
        this.firstName = firstName;
        this.lastName = lastName;
        this.organization = organization;
        this.notRobot = notRobot;
    }

    public String getUsername() { return username; }
    public String getPassword() { return password; }
    public String getRole() { return role; }
    public String getFirstName() { return firstName; }
    public String getLastName() { return lastName; }
    public String getEmail() { return email; }
    public String getOrganization() { return organization; }
    public String getNotRobots() { return notRobot; }
    public String getNotRobot() { return notRobot; }
}