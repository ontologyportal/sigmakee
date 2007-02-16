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
    private boolean initialized = false;
    private String error = "";

    /** ***************************************************************
     * Set an error string for file loading.
     */
    public void setError(String er) {
        error = er;
    }

    /** ***************************************************************
     * Get the error string for file loading.
     */
    public String getError() {
        return error;
    }

    /** ***************************************************************
     * Set default attribute values if not in the configuration file.
     */
    private void setDefaultAttributes() {

        String sep = File.separator;
        preferences.put("kbDir",System.getProperty("user.dir") + sep + "KBs");
        preferences.put("testOutputDir",System.getProperty("user.dir") + sep + "webapps" + sep + "sigma" + sep + "tests");
        preferences.put("inferenceTestDir","C:\\Program Files\\Apache Tomcat 4.0\\tests");  
        preferences.put("inferenceEngine","C:\\Artic\\vampire\\Vampire_VSWorkspace\\vampire\\Release\\kif.exe");  
        preferences.put("cache","no");  
        preferences.put("showcached","yes");  
        preferences.put("loadCELT","no");  
        preferences.put("TPTP","no");  
    }

    /** ***************************************************************
     */
    private String fromXML(SimpleElement configuration) {

        StringBuffer result = new StringBuffer();
        if (!configuration.getTagName().equals("configuration")) 
            System.out.println("Error in KBmanager.fromXML(): Bad tag: " + configuration.getTagName());
        else {
            for (int i = 0; i < configuration.getChildElements().size(); i++) {
                SimpleElement element = (SimpleElement) configuration.getChildElements().get(i);
                if (element.getTagName().equals("preference")) {
                    String name = (String) element.getAttribute("name");
                    String value = (String) element.getAttribute("value");
                    preferences.put(name,value);
                }
                else {
                    if (element.getTagName().equals("kb")) {
                        String kbName = (String) element.getAttribute("name");
                        addKB(kbName);
                        KB kb = getKB(kbName);
                        for (int j = 0; j < element.getChildElements().size(); j++) {
                            SimpleElement kbConst = (SimpleElement) element.getChildElements().get(j);
                            if (!kbConst.getTagName().equals("constituent")) 
                                System.out.println("Error in KBmanager.fromXML(): Bad tag: " + kbConst.getTagName());
                            String filename = (String) kbConst.getAttribute("filename");
                            try {                            
                                result.append(kb.addConstituent(filename)); 
                            } 
                            catch (IOException ioe) {
                                System.out.println("Error in KBmanager.fromXML(): " + ioe.getMessage());
                            }
                        }
                        if (KBmanager.getMgr().getPref("cache") != null &&
                            KBmanager.getMgr().getPref("cache").equalsIgnoreCase("yes"))
                            kb.cache();
                    }
                    else
                        System.out.println("Error in KBmanager.fromXML(): Bad tag: " + element.getTagName());
                }
            }
        }
        return result.toString();
    }

    /** ***************************************************************
     * Read an XML-formatted configuration file. The method initializeOnce()
     * sets the preferences based on the contents of the configuration file.
     * This routine has the side effect of setting the variable 
     * called "configuration".  It also creates the KBs directory and an empty
     * configuration file if none exists.
     */
    private SimpleElement readConfiguration() throws IOException {

        SimpleElement configuration = null;
        System.out.println("INFO in KBmanager.readConfiguration()"); 
        String fname = "config.xml";
        StringBuffer xml = new StringBuffer();
        String dir = System.getProperty("user.dir") + File.separator + "KBs";
        File f = new File(dir);
        if (!f.exists())
            f.mkdir();
        f = new File(dir + File.separator + fname);
        if (!f.exists()) 
            writeConfiguration();
        BufferedReader br = new BufferedReader(new FileReader(dir + File.separator + fname));

        try {
            SimpleDOMParser sdp = new SimpleDOMParser();
            configuration = sdp.parse(br);
        }
        catch (java.io.IOException e) {
            System.out.println("Error in KBmanager.readConfiguration(): IO exception parsing file " + 
                               fname + "\n" + e.getMessage());
        }
        finally {
            if (br != null) 
                br.close();
        }
        return configuration;
    }

    /** ***************************************************************
     * Read in any KBs defined in the configuration.
     */
    public void initializeOnce() throws IOException {

        System.out.println("INFO in KBmanager.initializeOnce() ");
        if (!initialized) {
            setDefaultAttributes();
            try {
                SimpleElement configuration = readConfiguration();
                String result = fromXML(configuration);
                if (result !="") 
                    error = result;
                LanguageFormatter.readKeywordMap((String) preferences.get("kbDir"));
            }
            catch (IOException ioe) {
                System.out.println("Error in KBmanager.initializeOnce(): Configuration file not read.");
                System.out.println(ioe.getMessage());
            }
            initialized = true;            
        }
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
        if (kb == null) {
            error = "KB " + name + " does not exist and cannot be removed.";
            return;
        }
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
        String fname = "config.xml";
        String key;
        String value;
        KB kb = null;

        SimpleElement configXML = new SimpleElement("configuration");

        it = preferences.keySet().iterator();
        while (it.hasNext()) {
            key = (String) it.next();
            value = (String) preferences.get(key);
            //System.out.println("INFO in KBmanager.writeConfiguration(): key, value: " + key + " " + value);
            if (key.compareTo("kbDir") == 0 || key.compareTo("celtdir") == 0 || 
                key.compareTo("inferenceEngine") == 0 || key.compareTo("inferenceTestDir") == 0)
                value = escapeFilename(value);
            if (key.compareTo("userName") != 0) {
                SimpleElement preference = new SimpleElement("preference");
                preference.setAttribute("name",key);
                preference.setAttribute("value",value);
                configXML.addChildElement(preference);
            }
        }
        it = kbs.keySet().iterator();
        while (it.hasNext()) {
            key = (String) it.next();
            kb = (KB) kbs.get(key);
            SimpleElement kbXML = kb.writeConfiguration();            
            configXML.addChildElement(kbXML);
        }

        try {
            fw = new FileWriter(dir + File.separator + fname);
            pw = new PrintWriter(fw);
            pw.println(configXML.toFileString());
        }
        catch (java.io.IOException e) {                                                  
            throw new IOException("Error writing file " + dir + File.separator + fname + ".\n " + e.getMessage());
        }
        finally {
            if (pw != null) {
                pw.close();
            }
            if (fw != null) {
                fw.close();
            }
        }
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
        System.out.println(LanguageFormatter.htmlParaphrase("", "(or (instance ?X0 Relation) (not (instance ?X0 TotalValuedRelation)))", 
                                                      kb.getFormatMap("en"), kb.getTermFormatMap("en"), "en"));

    }

}

