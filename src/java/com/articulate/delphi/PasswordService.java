/** This code is copyright Articulate Software (c) 2005.  
This software is released under the GNU Public License <http://www.gnu.org/copyleft/gpl.html>.
Users of this code also consent, by use of this code, to credit Articulate Software
and Ted Gordon in any writings, briefings, publications, presentations, or 
other representations of any software which incorporates, builds on, or uses this 
code.  */
package com.articulate.delphi;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import sun.misc.BASE64Encoder;
import sun.misc.CharacterEncoder;
import java.util.*;
import java.io.*;
import java.text.*;
import com.articulate.sigma.*;

/** *****************************************************************
 * A class that encrypts a string and checks it against another stored
 * encrypted string, in order to validate a user login.
 */
public final class PasswordService {

    private static PasswordService instance;
    private static HashMap users = new HashMap();
  
    /** ***************************************************************** 
     * Create an instance of PasswordService
     */
    public PasswordService() {

        try {
            String config = readUserFile();
            processUserFile(config);
        }
        catch (java.io.IOException e) {
            System.out.println("Error in PasswordService(): IO exception reading file " + e.getMessage());
        }
    }

    /** ***************************************************************** 
     * Encrypts a string with a deterministic algorithm.
     */
    public synchronized String encrypt(String plaintext) {

        System.out.println("PasswordService.encrypt(): input: " + plaintext);
        MessageDigest md = null;
        try {
            md = MessageDigest.getInstance("SHA");
        }
        catch(NoSuchAlgorithmException e) {
            System.out.println(e.getMessage());
        }
        try {
            md.update(plaintext.getBytes("UTF-8"));
        }
        catch (UnsupportedEncodingException e) {
            System.out.println(e.getMessage());
        }
        byte raw[] = md.digest(); 
        String hash = (new BASE64Encoder()).encode(raw);
        System.out.println("PasswordService.encrypt(): output: " + hash);
        return hash; 
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

        if (users.containsKey(username)) {
            User user = (User) users.get(username);
            System.out.println("INFO in PasswordService.authenticate(): Input: " + username + " " + pass);
            System.out.println("INFO in PasswordService.authenticate(): Reading: " + user.username + " " + user.password);
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
    public User getUser(String username) {

        if (username != null && username != "") 
            return (User) users.get(username);
        else
            return null;
    }

    /** ***************************************************************
     * Read an XML-formatted configuration file. 
     */
    private String readUserFile() throws IOException {
        
        String fname = System.getProperty("user.dir") + File.separator + "users.txt";
        StringBuffer xml = new StringBuffer();
        File f = new File(fname);
        if (!f.exists()) 
            return "";
        System.out.println("INFO in PasswordService.readUserFile(): Reading: " + fname);
        BufferedReader br = new BufferedReader(new FileReader(fname));

        try {
            do {
                String line = br.readLine();
                xml.append(line + "\n");
            } while (br.ready());
        }
        catch (java.io.IOException e) {
            System.out.println("Error in PasswordService.readUserFile(): IO exception parsing file " + fname);
        }
        finally {
            if (br != null) 
                br.close();
        }
        System.out.println(xml.toString());
        return xml.toString();
    }

    /** ***************************************************************
     * Read an XML-formatted configuration file. 
     */
    private void processUserFile(String configuration) {

        users = new HashMap();
        BasicXMLparser config = new BasicXMLparser(configuration);
        System.out.println("INFO in PasswordService.processUserFile(): Initializing.");
        System.out.print("INFO in PasswordService.processUserFile(): Number of users:");
        System.out.println(config.elements.size());
        for (int i = 0; i < config.elements.size(); i++) {
            BasicXMLelement element = (BasicXMLelement) config.elements.get(i);
            if (element.tagname.equalsIgnoreCase("user")) {
                User user = new User();
                user.fromXML(element);
                users.put(user.username,user);
            }
            else
                System.out.println("Error in PasswordService.processUserFile(): Bad element: " + element.tagname);
        }        
    }

    /** ***************************************************************** 
     */
    public void writeUserFile() throws IOException {

        FileWriter fw = null;
        PrintWriter pw = null;
        String fname = "users.txt";

        //System.out.println("INFO in PasswordService.writeUserFile: Writing user file.");
        try {
            fw = new FileWriter(fname);
            pw = new PrintWriter(fw);
            Iterator it = users.keySet().iterator();
            while (it.hasNext()) {
                String username = (String) it.next();
                User user = (User) users.get(username);
                System.out.println("PasswordService.writeUserFile(): writing user: " + user);
                pw.print(user.toXML());
            }
            System.out.println("PasswordService.writeUserFile(): " + users.size() + " records written");
        }
        catch (java.io.IOException e) {
            throw new IOException("Error writing file " + fname + ". " + e.getMessage());
        }
        finally {
            // System.out.println("INFO in PasswordService.writeUserFile: Completed writing user file");           
            if (pw != null) {
                pw.close();
            }
            if (fw != null) {
                fw.close();
            }
        }
    }

    /** *****************************************************************
     */
    public boolean userExists(String username) {
        
        return users.keySet().contains(username);
    }

    /** *****************************************************************
     */
    public void updateUser(User user) {

        if (!userExists(user.username)) {
            System.out.println("Error in PasswordService.addUser():  User " + user.username + " doesn't exist.");
            return;
        }
        users.put(user.username,user);

        try {
            writeUserFile();
        }
        catch (java.io.IOException e) {
            System.out.println("Error in PasswordService.addUser():  Error writing user file." + e.getMessage());
        }
    }

    /** *****************************************************************
     */
    public void addUser(User user) {

        if (userExists(user.username)) {
            System.out.println("Error in PasswordService.addUser():  User " + user.username + " already exists.");
            return;
        }
        users.put(user.username,user);

        System.out.println("INFO in PasswordService.addUser():  Password: " + user.password);
        try {
            writeUserFile();
        }
        catch (java.io.IOException e) {
            System.out.println("Error in PasswordService.addUser():  Error writing user file." + e.getMessage());
        }
    }

    /** *****************************************************************
     */
    public void login() {

        Console c = System.console();
        if (c == null) {
            System.err.println("No console.");
            System.exit(1);
        }

        String login = c.readLine("Enter your login: ");
        char [] password = c.readPassword("Enter your password: ");
        if (userExists(login)) {
            boolean valid = authenticate(login,password.toString());
            if (valid)
                System.out.println(getUser(login));
            else
                System.out.println("Invalid username/password");
        }
        else
            System.out.println("User " + login + " does not exist");
    }

    /** *****************************************************************
     */
    public void register() {

        Console c = System.console();
        if (c == null) {
            System.err.println("No console.");
            System.exit(1);
        }

        String login = c.readLine("Enter your login: ");
        char [] password = c.readPassword("Enter your password: ");
        if (userExists(login))
            System.out.println("User " + login + " already exists");
        else {
            User u = new User();
            u.username = login;
            u.password = password.toString();
            addUser(u);
        }
    }

    /** *****************************************************************
     */
    public static void showHelp() {

        System.out.println("PasswordService: ");
        System.out.println("-h    show this help message");
        System.out.println("-l    login");
        System.out.println("-r    register a new username and password (fail if username taken)");
    }

    /** ***************************************************************** 
     */
    public static void main(String args[]) {

        PasswordService ps = new PasswordService();
        System.out.println("test encrypted: " + ps.encrypt("test"));
        if (args != null) {
            if (args.length > 0 && args[0].equals("-r"))
                ps.register();
            if (args.length > 0 && args[0].equals("-l"))
                ps.login();
        }
        else
            showHelp();
    }

}
