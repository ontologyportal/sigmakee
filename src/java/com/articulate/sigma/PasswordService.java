/** This code is copyright Articulate Software (c) 2005.  
This software is released under the GNU Public License <http://www.gnu.org/copyleft/gpl.html>.
Users of this code also consent, by use of this code, to credit Articulate Software
and Ted Gordon in any writings, briefings, publications, presentations, or 
other representations of any software which incorporates, builds on, or uses this 
code.  */
package com.articulate.sigma;

import java.io.UnsupportedEncodingException;
import java.util.*;
import java.io.*;
import java.text.*;
import com.articulate.sigma.*;

/** *****************************************************************
 * A class that encrypts a string and checks it against another stored
 * encrypted string, in order to validate a user login.
 */
public final class PasswordService {

    private static final String CHARSET = "UTF-8";
    private static final String USERS_FILENAME = "users.txt";
    protected static final String ADMIN_ROLE = "administrator";
    protected static final String USER_ROLE = "user";
    private static final String DELIMITER1 = ":";
    private static final String DELIMITER2 = "0xyz1";
    private static final String USER_DELIMITER = "1uuuxuuu2";
    private static PasswordService INSTANCE = null;
    private static HashMap<String, User> users = new HashMap<String, User>();
  
    /** ***************************************************************** 
     * Use the static factory method getInstance().
     */
    private PasswordService() {
    }

    /** ***************************************************************** 
     * Encrypts a string with a deterministic algorithm.
     */
    public String encrypt(String plaintext) {
        return StringUtil.encrypt(plaintext, CHARSET);
    }

    /** ***************************************************************** 
     */
    public static PasswordService getInstance() {
        try {
            synchronized (users) {
                if (INSTANCE == null) {
                    INSTANCE = new PasswordService();
                    SigmaServer ss = KBmanager.getMgr().getSigmaServer();
                    INSTANCE.setUsersFileDirectory(ss.getWebDirectory());
                }
                if (INSTANCE.users.isEmpty()) {
                    INSTANCE.readUserFile();

                    // If no user file exists, create one and initialize
                    // it with user "admin".
                    if (INSTANCE.users.isEmpty()) {
                        User admin = new User();
                        admin.setUsername("admin");
                        admin.setPassword(INSTANCE.encrypt("admin"));
                        admin.setRole(INSTANCE.encrypt(ADMIN_ROLE));
                        INSTANCE.addUser(admin);
                    }
                }
            }
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
        return INSTANCE;
    }

    private File usersFileDirectory = null;

    public void setUsersFileDirectory(File dir) {
        usersFileDirectory = dir;
        return;
    }

    public File getUsersFileDirectory() {
        return this.usersFileDirectory;
    }

    /** *****************************************************************
     * Accepts as input a base 64 String consisting of a user name and
     * password.  Returns true if the user can be authenticated, else
     * returns false.
     *
     * @param String A concatenated user name and password in base 64
     * representation
     *
     * @return True if the user can be authenticated, else false
     */
    protected boolean isUserAuthenticated(String usernamePassword64) {
        boolean ans = false;
        try {
            List<String> pair = toNamePasswordPairFrom64(usernamePassword64);
            String username = pair.get(0);
            String password = pair.get(1);
            if (StringUtil.isNonEmptyString(password)) {
                ans = encrypt(password).equals(getUser(username).getPassword());
            }
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
        return ans;
    }

    /** *****************************************************************
     * Accepts as input a base 64 String consisting of a user name and
     * password.  Breaks and decodes the input String, if possible,
     * and returns a List containing a user name String and an
     * encrypted password String.
     *
     * @param usernamePassword64 A base 64 encoded String consisting
     * of a user name and an encrypted password
     *
     * @return A two-element List consisting of a user name and an
     * encrypted password.  Both the user name and the encrypted
     * password could be empty Strings if decoding of the input String
     * fails.
     */
    protected ArrayList<String> toNamePasswordPairFrom64(String usernamePassword64) {
        ArrayList<String> pair = new ArrayList<String>();
        try {
            String authtype = "Basic ";
            int bidx = usernamePassword64.indexOf(authtype);
            if (bidx != -1)
                usernamePassword64 = usernamePassword64.substring(bidx + authtype.length());
            String usernamePassword = StringUtil.fromBase64(usernamePassword64, CHARSET);
            int idx1 = usernamePassword.indexOf(DELIMITER1);
            int d1len = DELIMITER1.length();
            String username = "";
            String password = "";
            if (idx1 != -1) {
                username = usernamePassword.substring(0, idx1);
                User user = getUser(username);
                if (user != null) {
                    int idx2 = (idx1 + d1len);
                    password = ((idx2 < usernamePassword.length())
                                ? usernamePassword.substring(idx2)
                                : "");
                }
            }
            pair.add(username);
            pair.add(password);
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
        return pair;
    }

    /** ***************************************************************** 
     */
    public User getUser(String username) {
        User result = null;
        synchronized (users) {
            result = users.get(username);
        }
        return result;
    }

    /** ***************************************************************** 
     */
    protected User getUserFromNamePassword64(String namepass64) {
        User u = null;
        try {
            u = getUser(toNamePasswordPairFrom64(namepass64).get(0));
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
        return u;
    }

    /** ***************************************************************
     * Read a text file consisting of partly encrypted user password
     * and role authorization data.
     */
    private void readUserFile() {
        System.out.println("ENTER PasswordService.readUserFile()");
        BufferedReader br = null;
        File f = null;
        String canonicalPath = null;
        try {
            File fdir = getUsersFileDirectory(); // new File(KBmanager.getMgr().getPref("kbDir"));
            f = new File(fdir, USERS_FILENAME);
            canonicalPath = f.getCanonicalPath();
            if (f.canRead()) {
                br = new BufferedReader(new FileReader(f));
                StringBuilder sb = new StringBuilder();
                users.clear();
                int i = -1;
                while ((i = br.read()) != -1) {
                    sb.append((char) i);
                }
                if (sb.length() > 0) {
                    String filestr = StringUtil.fromBase64(sb.toString(), CHARSET);
                    List<String> userStrs = Arrays.asList(filestr.split(USER_DELIMITER));
                    for (String udata : userStrs) {
                        if (StringUtil.isNonEmptyString(udata)) {
                            List<String> u_p_r = Arrays.asList(udata.split(DELIMITER2));
                            if (u_p_r.size() > 2) {
                                User user = new User();
                                String userName = u_p_r.get(0);
                                String password = u_p_r.get(1);
                                String role = u_p_r.get(2);
                                System.out.println("  > userName == " + userName);
                                user.setUsername(userName);
                                System.out.println("  > password == " + password);
                                user.setPassword(password);
                                System.out.println("  > role == " + role);
                                user.setRole(role);
                                users.put(userName, user);
                            }
                        }
                    }
                }
            }
            else {
                System.out.println("WARNING in PasswordService.readUserFile()");
                System.out.println("  > Cannot read " + canonicalPath);
            }
        }
        catch (Exception ex) {
            System.out.println("ERROR in PasswordService.readUserFile()");
            System.out.println("  > f == " + canonicalPath);
            System.out.println("  > " + ex.getMessage());
            ex.printStackTrace();
        }
        finally {
            try {
                if (br != null) 
                    br.close();
            }
            catch (Exception e1) {
                e1.printStackTrace();
            }
        }
        //System.out.println(xml.toString());
        System.out.println("EXIT PasswordService.readUserFile()");
        return;
    }

    /** ***************************************************************
     * Read an XML-formatted configuration file. 
     * @deprecated
     */
    private void processUserFile(String configuration) {

        if (users == null) users = new HashMap();
        users.clear();
        BasicXMLparser config = new BasicXMLparser(configuration);
        //System.out.println("INFO in PasswordService.processUserFile(): Initializing.");
        //System.out.print("INFO in PasswordService.processUserFile(): Number of users:");
        //System.out.println(config.elements.size());
        for (int i = 0; i < config.elements.size(); i++) {
            BasicXMLelement element = (BasicXMLelement) config.elements.get(i);
            if (element.tagname.equalsIgnoreCase("user")) {
                User user = new User();
                user.fromXML(element);
                users.put(user.getUsername(),user);
            }
            else
                System.out.println("Error in PasswordService.processUserFile(): Bad element: " 
                                   + element.tagname);
        }        
    }

    /** ***************************************************************** 
     */
    protected void writeUserFile() {
        System.out.println("ENTER PasswordService.writeUserFile()");
        PrintWriter pw = null;
        File usersFile = null;
        String canonicalPath = null;
        try {
            File ufDir = getUsersFileDirectory(); // new File(KBmanager.getMgr().getPref("kbDir"));
            usersFile = new File(ufDir, USERS_FILENAME);
            canonicalPath = usersFile.getCanonicalPath();
            pw = new PrintWriter(new FileWriter(usersFile));
            StringBuilder sb = new StringBuilder();
            User u = null;
            String password = null;
            String role = null;
            for (String username : users.keySet()) {
                u = users.get(username);
                password = u.getPassword();
                role = u.getRole();
                System.out.println("  > username == " + username);
                sb.append(username);
                sb.append(DELIMITER2);
                System.out.println("  > password == " + password);
                sb.append(u.getPassword());
                sb.append(DELIMITER2);
                System.out.println("  > role == " + role);
                sb.append(u.getRole());
                sb.append(USER_DELIMITER);
            }
            String udata = StringUtil.toBase64(sb.toString(), CHARSET);
            System.out.println("  > udata == " + udata);
            pw.println(udata);
        }
        catch (Exception ex) {
            System.out.println("Error writing file " 
                               + ((usersFile == null)
                                  ? USERS_FILENAME
                                  : canonicalPath)
                               + ": " 
                               + ex.getMessage());
            ex.printStackTrace();
        }
        finally {
            if (pw != null) {
                pw.close();
            }
        }
        return;
    }

    /** *****************************************************************
     */
    public boolean userExists(String username) {
        return (getUser(username) != null);
    }

    /** *****************************************************************
     */
    public void updateUser(User user) {
        try {        
            synchronized (users) {
                String uname = user.getUsername();
                if (userExists(uname)) {
                    users.put(uname, user);
                    writeUserFile();
                }
                else {
                    System.out.println("ERROR in PasswordService.updateUser(" + uname + ")");
                    System.out.println("  > User " + uname + " does not exist");
                }
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        return;
    }

    /** *****************************************************************
     */
    public void addUser(User user) {
        try {
            synchronized (users) {
                String uname = user.getUsername();
                if (userExists(uname)) {
                    System.out.println("ERROR in PasswordService.addUser(" + uname + ")");
                    System.out.println("  > User " + uname + " already exists");
                }
                else {
                    users.put(uname, user);
                    writeUserFile();
                }
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        return;
    }

    /** ***************************************************************** 
     */
    public static void main(String args[]) {

    }

}
