package com.articulate.sigma;

/** This code is copyright Articulate Software (c) 2003.  Some portions
copyright Teknowledge (c) 2003 and reused under the terms of the GNU license.
This software is released under the GNU Public License <http://www.gnu.org/copyleft/gpl.html>.
Users of this code also consent, by use of this code, to credit Articulate Software
and Teknowledge in any writings, briefings, publications, presentations, or 
other representations of any software which incorporates, builds on, or uses this 
code.  Please cite the following article in any publication with references:

Pease, A., (2003). The Sigma Ontology Development Environment, 
in Working Notes of the IJCAI-2003 Workshop on Ontology and Distributed Systems,
August 9, Acapulco, Mexico.
*/

import java.util.*;
import java.io.*;
import java.text.*;

/** This is a class that manages a group of knowledge bases.  It should only
 *  have one instance, contained in its own static member variable.
 */
public class KBmanager {
    
    private static KBmanager manager = new KBmanager();
    private HashMap preferences = new HashMap();

    private HashMap kbs = new HashMap();
    private String configuration = null;
    private boolean initialized = false;

    /** ***************************************************************
     * Constructor which reads in a configuration from a file.
     */
    public KBmanager() {

        String sep = File.separator;
        if (KBmanager.manager == null) {
            try {
                preferences.put("kbDir",System.getProperty("user.dir") + sep + "KBs");
                preferences.put("testOutputDir",System.getProperty("user.dir") + sep + "webapps" + sep + "sigma" + sep + "tests");
                readConfiguration();
            }
            catch (IOException ioe) {
                System.out.println("Error in KBmanager: Configuration file not read.");
                System.out.println(ioe.getMessage());
            }
            finally {
                NLformatter.readKeywordMap((String) preferences.get("kbDir"));
                if (!preferences.containsKey("inferenceTestDir"))
                    preferences.put("inferenceTestDir","C:\\Program Files\\Apache Tomcat 4.0\\tests");  
                if (!preferences.containsKey("inferenceEngine"))
                    preferences.put("inferenceEngine","C:\\Artic\\vampire\\Vampire_VSWorkspace\\vampire\\Release\\kif.exe");  
                if (!preferences.containsKey("cache"))
                    preferences.put("cache","no");  
                if (!preferences.containsKey("showcached"))
                    preferences.put("showcached","yes");  
                if (!preferences.containsKey("loadCELT"))
                    preferences.put("loadCELT","no");  
            }
        }
    }
    
    /** ***************************************************************
     * Read in any KBs defined in the configuration.
     */
    public void initializeOnce() throws IOException, ParseException {

        if (!initialized) {
            BasicXMLparser config = new BasicXMLparser(configuration);
            System.out.println("INFO in KBmanager.initializeOnce(): Initializing.");
            System.out.print("INFO in KBmanager.initializeOnce(): Number of preferences:");
            System.out.println(config.elements.size());
            for (int i = 0; i < config.elements.size(); i++) {
                BasicXMLelement element = (BasicXMLelement) config.elements.get(i);
                if (element.tagname.equalsIgnoreCase("preference")) {
                    String name = (String) element.attributes.get("key");
                    String value = (String) element.attributes.get("value");
                    preferences.put(name,value);
                    System.out.println("INFO in KBmanager.initializeOnce(): Storing preferences: " + name + " " + value);
                }
                if (element.tagname.equalsIgnoreCase("kb")) {
                    String kbName = (String) element.attributes.get("name");
                    addKB(kbName);
                    KB kb = getKB(kbName);
                    System.out.println("INFO in KBmanager.initializeOnce(): Number of constituents: " + element.subelements.size());
                    for (int j = 0; j < element.subelements.size(); j++) {
                        BasicXMLelement kbConst = (BasicXMLelement) element.subelements.get(j);
                        if (!kbConst.tagname.equalsIgnoreCase("constituent")) 
                            System.out.println("Error in KBmanager.initialize(): Bad element: " + kbConst.tagname);
                        String filename = (String) kbConst.attributes.get("filename");
                        kb.addConstituent(filename); 
                    }
                    System.out.println("INFO in KBmanager.initializeOnce(): value of cache: " + KBmanager.getMgr().getPref("cache"));
                    if (KBmanager.getMgr().getPref("cache") != null &&
                        KBmanager.getMgr().getPref("cache").equalsIgnoreCase("yes"))
                        kb.cache();
                }
            }
            initialized = true;
        }
        System.out.println("INFO in KBmanager.initializeOnce(): celtdir: " + (String) preferences.get("celtdir"));
    }

    /** ***************************************************************
     * Double the backslash in a filename so that it can be saved to a text
     * file and read back properly.
     */
    public static String escapeFilename(String fname) {

        StringBuffer newstring = new StringBuffer("");
        
        for (int i = 0; i < fname.length(); i++) {
            if (fname.charAt(i) == 92 && fname.charAt(i+1) != 92) 
                newstring = newstring.append("\\\\");
            if (fname.charAt(i) == 92 && fname.charAt(i+1) == 92) {
                newstring = newstring.append("\\\\");
                i++;
            }
            if (fname.charAt(i) != 92)
                newstring = newstring.append(fname.charAt(i));
        }
        return newstring.toString();
    }

    /** ***************************************************************
     * Create a new empty KB with a name.
     * @param name - the name of the KB
     */

    public void addKB(String name) {

        KB kb = new KB(name,(String) preferences.get("kbDir"));
        kbs.put(name.intern(),kb); 
        System.out.println("INFO in KBmanager.addKB: Adding KB: " + name);
    }

    /** ***************************************************************
     * Remove a knowledge base.
     * @param name - the name of the KB
     */

    public void removeKB(String name) {

        KB kb = (KB) kbs.get(name);
        try {
            if (kb.inferenceEngine != null) 
                kb.inferenceEngine.terminate();
        }
        catch (IOException ioe) {
            System.out.println("Error in KBmanager.removeKB(): Error terminating inference engine: " + ioe.getMessage());
        }
        kbs.remove(name);
        try {
            writeConfiguration();
        }
        catch (IOException ioe) {
            System.out.println("Error in KBmanager.removeKB(): Error writing configuration file. " + ioe.getMessage());
        }

        System.out.println("INFO in KBmanager.removeKB: Removing KB: " + name);
    }

    /** ***************************************************************
     * Write the current configuration of the system.  Call 
     * writeConfiguration() on each KB object to write its manifest.
     */

    public void writeConfiguration() throws IOException {

        FileWriter fw = null;
        PrintWriter pw = null;
        Iterator it; 
        String dir = (String) preferences.get("kbDir");
        String fname = "config.txt";
        String key;
        String value;
        KB kb = null;
        File f;

        System.out.println("INFO in KBmanager.writeConfiguration: Writing configuration.");
        try {
            fw = new FileWriter(dir + File.separator + fname);
            pw = new PrintWriter(fw);
            it = preferences.keySet().iterator();
            while (it.hasNext()) {
                key = (String) it.next();
                value = (String) preferences.get(key);
                if (key.compareTo("kbDir") == 0 || key.compareTo("celtdir") == 0 || 
                    key.compareTo("inferenceEngine") == 0 || key.compareTo("inferenceTestDir") == 0)
                    value = escapeFilename(value);
                if (key.compareTo("userName") != 0)
                    pw.println("<preference key=\"" + key + "\" value=\"" + value + "\"/>");
            }
            //System.out.print("INFO in KBmanager.writeConfiguration(): number of KBs: ");
            //System.out.println(kbs.keySet().size());
            it = kbs.keySet().iterator();
            while (it.hasNext()) {
                key = (String) it.next();
                kb = (KB) kbs.get(key);
                kb.writeConfiguration(pw);

                System.out.print("INFO in KBmanager.writeConfiguration: Number of constituents in kb: " + kb.name + " is: ");
                System.out.println(kb.constituents.size());
            }
        }
        catch (java.io.IOException e) {
            throw new IOException("Error writing file " + dir + File.separator + fname);
        }
        finally {
            System.out.println("INFO in KBmanager.writeConfiguration: Completed writing configuration");
            
            if (pw != null) {
                pw.close();
            }
            if (fw != null) {
                fw.close();
            }
        }
    }

    /** ***************************************************************
     * Read an XML-formatted configuration file. The method initializeOnce()
     * sets the preferences based on the contents of the configuration file.
     * This routine has the side effect of setting the variable 
     * called "configuration".  It also creates the KBs directory and an empty
     * configuration file if none exists.
     */
    private void readConfiguration() throws IOException {
        
        String fname = "config.txt";
        StringBuffer xml = new StringBuffer();
        String dir = System.getProperty("user.dir") + File.separator + "KBs";
        File f = new File(dir);
        if (!f.exists())
            f.mkdir();
        f = new File(dir + File.separator + fname);
        if (!f.exists()) 
            writeConfiguration();
        System.out.println("INFO in KBmanager.readConfiguration(): Reading: " + dir);
        BufferedReader br = new BufferedReader(new FileReader(dir + File.separator + fname));

        try {
            do {
                String line = br.readLine();
                xml.append(line + "\n");
            } while (br.ready());
        }
        catch (java.io.IOException e) {
            System.out.println("Error in KBmanager.readConfiguration(): IO exception parsing file " + fname);
        }
        finally {
            if (br != null) 
                br.close();
        }
        System.out.println(xml.toString());
        configuration = xml.toString();
    }

    /** ***************************************************************
     * Get the KB that has the given name.
     */

    public KB getKB(String name) {

        if (!kbs.containsKey(name))
            System.out.println("Error in KBmanager.getKB(): KB " + name + " not found.");
        return (KB) kbs.get(name.intern());
    }

    /** ***************************************************************
     * Returns true if a KB with the given name exists.
     */

    public boolean existsKB(String name) {

        return kbs.containsKey(name);
    }

    
    /** ***************************************************************
     * Remove the KB that has the given name.
     */
	
    public void remove(String name) {
        kbs.remove(name);
    }
	
    /** ***************************************************************
     * Get the one instance of KBmanager from its class variable.
     */

    public static KBmanager getMgr() {

        if (manager == null) 
            manager = new KBmanager();
        return manager;
    }
	
    /** ***************************************************************
     * Get the Set of KB names in this manager.
     */

    public Set getKBnames() {
        return kbs.keySet();
    }
    
    /** ***************************************************************
     * Get the preference corresponding to the given kef.
     */
    
    public String getPref(String key) {
        return (String) preferences.get(key);
    }
    
    /** ***************************************************************
     * Set the preference to the given value.
     */
    
    public void setPref(String key, String value) {
        preferences.put(key,value);
    }

    /** ***************************************************************
     * A test method.
     */

    public static void main(String[] args) {

        KB kb = KBmanager.getMgr().getKB("SUMO");
        System.out.println(KBmanager.getMgr().getKBnames());
        System.out.println(kb.name);
        System.out.println(NLformatter.htmlParaphrase("", "(or (instance ?X0 Relation) (not (instance ?X0 TotalValuedRelation)))", 
                                                      kb.getFormatMap("en"), kb.getTermFormatMap("en"), "en"));

    }

}

