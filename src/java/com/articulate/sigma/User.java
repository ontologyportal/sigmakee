/** This code is copyright Articulate Software (c) 2005.  
This software is released under the GNU Public License <http://www.gnu.org/copyleft/gpl.html>.
Users of this code also consent, by use of this code, to credit Articulate Software
and Ted Gordon in any writings, briefings, publications, presentations, or 
other representations of any software which incorporates, builds on, or uses this 
code.  */
package com.articulate.sigma;
import java.util.*;
import java.io.*;
import java.text.*;
import com.articulate.sigma.*;
import java.net.*;

/** *****************************************************************
 * A class that encrypts a string and checks it against another stored
 * encrypted string, in order to validate a user login.
 */
public class User {

    private static final String CHARSET = "UTF-8";

    private String username;
    /** Encrypted password */
    private String password;
    /** A String which is one of: user, administrator */
    private String role = "user";
    /** A HashMap of String keys and String values */
    private Map<String, String> attributes = new HashMap<String, String>();
    /** A List of String keys consisting of unique project names. */
    private List<String> projects = new ArrayList<String>();
  
    /** ***************************************************************** 
     */
    public String toString() {

        return username + "\n" + password + "\n" + role + "\n";
    }

    /** ***************************************************************** 
     *  Read in an XML object
     */
    public void fromXML(BasicXMLelement xml) {

        username = (String) xml.attributes.get("name");
        password = (String) xml.attributes.get("password");
        try {
            password = URLDecoder.decode(password,CHARSET);
        }
        catch (UnsupportedEncodingException uee) {
            System.out.println("Error in User.fromXML(): Unsupported encoding exception: " 
                               + uee.getMessage());
        }
        setRole((String) xml.attributes.get("role"));
        System.out.println("Read role: " + xml.attributes.get("role"));
        //System.out.println("INFO in PasswordService.processUserFile(): Number of projects: " + element.subelements.size());
        for (int j = 0; j < xml.subelements.size(); j++) {
            BasicXMLelement subelement = (BasicXMLelement) xml.subelements.get(j);
            if (subelement.tagname.equalsIgnoreCase("project")) {
                String projectName = (String) subelement.attributes.get("name");
                projects.add(projectName); 
            }
            else {
                if (subelement.tagname.equalsIgnoreCase("attribute")) {
                    String attribute = (String) subelement.attributes.get("name");
                    String value = (String) subelement.attributes.get("value");
                    attributes.put(attribute,value);
                }
                else
                    System.out.println("Error in User.fromXML(): Bad element: " + subelement.tagname);
            }
        }
    }

    /** ***************************************************************** 
     *  Create an XML-formatted String
     */
    public String toXML() {

        StringBuffer result = new StringBuffer();

        try {
            result.append("<user name=\"" + username + "\" password=\"" + 
                          URLEncoder.encode(password,CHARSET) + "\" role=\"" + getRole() + "\">\n");
        }
        catch (UnsupportedEncodingException uee) {
            System.out.println("Error in User.toXML(): Unsupported encoding exception: " + uee.getMessage());
        }
        Iterator it = attributes.keySet().iterator();
        while (it.hasNext()) {
            String attribute = (String) it.next();
            String value = (String) attributes.get(attribute);
            result.append("  <attribute name=\"" + attribute + "\" value=\"" + value + "\"/>\n");
        }
        Iterator it2 = projects.iterator();
        while (it2.hasNext()) {
            String project = (String) it.next();
            result.append("  <project name=\"" + project + "\">\n");
        }
        result.append("</user>\n");
        return result.toString();
    }

    /** ***************************************************************** 
     */
    public void setUsername(String uname) {
        username = uname;
        return;
    }

    /** ***************************************************************** 
     */
    public String getUsername() {
        return username;
    }

    /** ***************************************************************** 
     */
    public void setPassword(String newPassword) {
        password = newPassword;
        return;
    }

    /** ***************************************************************** 
     */
    public String getPassword() {
        return password;
    }

    /** ***************************************************************** 
     */
    public void setAttribute(String k, String v) {
        this.attributes.put(k, v);
        return;
    }

    /** ***************************************************************** 
     */
    public String getAttribute(String k) {
        return this.attributes.get(k);
    }

    /** ***************************************************************** 
     */
    public void setRole(String newRole) {
        if (Arrays.asList(StringUtil.encrypt(PasswordService.USER_ROLE, CHARSET), 
                          StringUtil.encrypt(PasswordService.ADMIN_ROLE, CHARSET))
            .contains(newRole))
            role = newRole;
        else
            System.out.println("Error in User.setRole(): Bad role name: " + newRole);
        return;
    }

    /** ***************************************************************** 
     */
    public String getRole() {
        return role;
    }

    /** ***************************************************************** 
     */
    public boolean addProject(String projectName) {
        boolean result = false;
        if (StringUtil.isNonEmptyString(projectName)) {
            String canonicalName = projectName.intern();
            if (!projects.contains(canonicalName)) 
                result = projects.add(canonicalName);
        }
        return result;
    }

    /** ***************************************************************** 
     */
    public boolean removeProject(String projectName) {
        boolean result = false;
        if (StringUtil.isNonEmptyString(projectName)) {
            result = projects.remove(projectName.intern());
        }
        return result;
    }

    /** ***************************************************************** 
     */
    public List<String> getProjects() {
        return projects;
    }

    /** ***************************************************************** 
     */
    public static void main(String[] args) {

    }

}
