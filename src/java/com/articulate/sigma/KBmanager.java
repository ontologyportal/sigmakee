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
August 9, Acapulco, Mexico. See also http://sigmakee.sourceforge.net
*/

import com.articulate.sigma.CCheckManager.CCheckStatus;
import com.articulate.sigma.nlg.NLGUtils;

import java.io.*;
import java.util.*;

/** This is a class that manages a group of knowledge bases.  It should only
 *  have one instance, contained in its own static member variable.
 */
public class KBmanager {

    /** ***************************************************************
     * A numeric (bitwise) constant used to signal whether type
     * prefixes (sortals) should be added during formula
     * preprocessing.
     */    
    public static final int USE_TYPE_PREFIX  = 1;

    /** ***************************************************************
     * A numeric (bitwise) constant used to signal whether holds
     * prefixes should be added during formula preprocessing.
     */    
    public static final int USE_HOLDS_PREFIX = 2;

    /** ***************************************************************
     * A numeric (bitwise) constant used to signal whether the closure
     * of instance and subclass relastions should be "cached out" for
     * use by the inference engine.
     */    
    public static final int USE_CACHE        = 4;

    /** ***************************************************************
     * A numeric (bitwise) constant used to signal whether formulas
     * should be translated to TPTP format during the processing of KB
     * constituent files.
     */    
    public static final int USE_TPTP         = 8;
    private static CCheckManager ccheckManager = new CCheckManager();
    
    private static KBmanager manager = new KBmanager();
    protected static final String CONFIG_FILE = "config.xml";

    private HashMap<String,String> preferences = new HashMap<String,String>();
    protected HashMap<String,KB> kbs = new HashMap<String,KB>();
    boolean initialized = false;
    private int oldInferenceBitValue = -1;
    private String error = "";
    public boolean initializing = false;

    public KBmanager() {
    }
    
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

    /* Future:
    private SigmaServer sigmaServer = null;

    public void setSigmaServer(SigmaServer ss) {
        this.sigmaServer = ss;
        return;
    }

    public SigmaServer getSigmaServer() {
        return this.sigmaServer;
    }
    */

    /** ***************************************************************
     * Set default attribute values if not in the configuration file.
     */
    private void setDefaultAttributes() {
        
        try {
            String sep = File.separator;
            String base = System.getenv("SIGMA_HOME");
            String tptpHome = System.getenv("TPTP_HOME");
            String systemsHome = System.getenv("SYSTEMS_HOME");
            if (StringUtil.emptyString(base))
                base = System.getProperty("user.dir");
            if (StringUtil.emptyString(tptpHome))
                tptpHome = System.getProperty("user.dir");
            if (StringUtil.emptyString(systemsHome))
                systemsHome = System.getProperty("user.dir");
            String tomcatRoot = System.getenv("CATALINA_HOME");
            if (StringUtil.emptyString(tomcatRoot))
                tomcatRoot = System.getProperty("user.dir");
            File tomcatRootDir = new File(tomcatRoot);
            File baseDir = new File(base);
            File tptpHomeDir = new File(tptpHome);
            File systemsDir = new File(systemsHome);
            File kbDir = new File(baseDir, "KBs");
            File inferenceTestDir = new File(kbDir, "tests");
            File logDir = new File(baseDir, "logs");
            logDir.mkdirs();
           
            // The links for the test results files will be broken if
            // they are not put under [Tomcat]/webapps/sigma.
            // Unfortunately, we don't know where [Tomcat] is.
            File testOutputDir = new File(tomcatRootDir,
                                          ("webapps" + sep + "sigma" + sep + "tests"));
            preferences.put("baseDir",baseDir.getCanonicalPath());
            preferences.put("tptpHomeDir",tptpHomeDir.getCanonicalPath());
            preferences.put("systemsDir",systemsDir.getCanonicalPath());
            preferences.put("kbDir",kbDir.getCanonicalPath());
            preferences.put("inferenceTestDir",inferenceTestDir.getCanonicalPath());  
            preferences.put("testOutputDir",testOutputDir.getCanonicalPath());
            
            File graphVizDir = new File("/usr/bin");
            preferences.put("graphVizDir", graphVizDir.getCanonicalPath());
          
            File graphDir = new File(tomcatRootDir, "webapps" + sep + "sigma" + sep + "graph");
            graphDir.mkdir();
            preferences.put("graphDir", graphDir.getCanonicalPath());
            
            // There is no foolproof way to determine the actual
            // inferenceEngine path without asking the user.  But we
            // can make an educated guess.
            String _OS = System.getProperty("os.name");
            String ieExec = "e_ltb_runner";
            if (StringUtil.isNonEmptyString(_OS) && _OS.matches("(?i).*win.*"))
                ieExec = "e_ltb_runner.exe";
            File ieDirFile = new File(baseDir, "inference");
            File ieExecFile = (ieDirFile.isDirectory()
                               ? new File(ieDirFile, ieExec)
                               : new File(ieExec));
            String leoExec = "leo";
            File leoExecFile = (ieDirFile.isDirectory()
                  ? new File(ieDirFile, leoExec)
                  : new File(leoExec));
            preferences.put("inferenceEngine",ieExecFile.getCanonicalPath());
            preferences.put("leoExecutable",leoExecFile.getCanonicalPath());
            preferences.put("loadCELT","no");  
            preferences.put("showcached","yes");  
            preferences.put("typePrefix","no");

            // If no then instantiate variables in predicate position.
            preferences.put("holdsPrefix","no");  
            preferences.put("cache","no");
            preferences.put("TPTP","yes");  
            preferences.put("TPTPDisplay","no");  
            preferences.put("userBrowserLimit","25");
            preferences.put("adminBrowserLimit","200");
            preferences.put("port","8080");            
            preferences.put("hostname","localhost");  
            
            // Default logging things
            preferences.put("logDir", logDir.getCanonicalPath());
            preferences.put("logLevel", "warning");
            
        }
        catch (Exception ex) {
            System.out.println("Error in KBmanager.setDefaultAttributes(): " + Arrays.toString(ex.getStackTrace()));
            ex.printStackTrace();
        }
        return;
    }

    /** *************************************************************** 
     */
    public static CCheckStatus initiateCCheck(KB kb, String chosenEngine, String systemChosen, String location,
            String language, int timeout) {
        
        return ccheckManager.performConsistencyCheck(kb, chosenEngine, systemChosen, location, language, timeout);        
    }

    public static String ccheckResults(String kbName) {        
        return ccheckManager.ccheckResults(kbName); 
    }
    
    public static CCheckStatus ccheckStatus(String kbName) {  
        return ccheckManager.ccheckStatus(kbName);            
        //return HTMLformatter.formatConsistencyCheck(msg, ccheckManager.ccheckResults(kb.name), language, page);
    }
        
    /** ***************************************************************  
     */
    private void preferencesFromXML(SimpleElement configuration) {
        
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
            }
        }
    }
    
    /** *************************************************************** 
     */
    private void kbsFromXML(SimpleElement configuration) {
        
        if (!configuration.getTagName().equals("configuration")) 
        	System.out.println("Error in KBmanager.fromXML(): Bad tag: " + configuration.getTagName());
        else {
            for (int i = 0; i < configuration.getChildElements().size(); i++) {
                SimpleElement element = (SimpleElement) configuration.getChildElements().get(i);
                if (element.getTagName().equals("kb")) {
                    String kbName = (String) element.getAttribute("name");
                    addKB(kbName);
                    ArrayList<String> constituentsToAdd = new ArrayList<String>();
                    boolean useCacheFile = KBmanager.getMgr().getPref("cache").equalsIgnoreCase("yes");
                    for (int j = 0; j < element.getChildElements().size(); j++) {
                        SimpleElement kbConst = (SimpleElement) element.getChildElements().get(j);
                        if (!kbConst.getTagName().equals("constituent")) 
                        	System.out.println("Error in KBmanager.fromXML(): Bad tag: " + kbConst.getTagName());
                        String filename = (String) kbConst.getAttribute("filename");
                        if (!StringUtil.emptyString(filename)) {
                            if (filename.endsWith(KB._cacheFileSuffix) ) {
                                if (useCacheFile) 
                                    constituentsToAdd.add(filename);                                
                            }
                            else 
                                constituentsToAdd.add(filename);                            
                        }
                    }
                    loadKB(kbName, constituentsToAdd);
                }
            }
        }
    }
    
    /** ***************************************************************
     */
    public boolean loadKB(String kbName, List<String> constituents) {
        
        boolean useCacheFile = KBmanager.getMgr().getPref("cache").equalsIgnoreCase("yes");
        KB kb = null;
        try {
            if (existsKB(kbName))
                removeKB(kbName);
            addKB(kbName);
            kb = getKB(kbName);

            if (!(constituents.isEmpty())) {
                Iterator<String> it = constituents.iterator();
                while (it.hasNext()) {
                    String filename = it.next();
                    try {
                        //kb.addConstituent(filename, false, false, false);
                        kb.addConstituent(filename);
                    } 
                    catch (Exception e1) {
                    	System.out.println("Error in KBmanager.loadKB():  " + e1.getMessage());
                    	e1.printStackTrace();
                        return false;
                    }
                }
            }
            //writeConfiguration();
        } 
        catch (Exception e) {
        	System.out.println("Error in KBmanager.loadKB(): Unable to save configuration: " + e.getMessage());
        	e.printStackTrace();
            return false;
        }

        // build kb cache when "cache" = "yes"
        if (useCacheFile) {
            kb.kbCache = new KBcache(kb);
            kb.kbCache.buildCaches();
        }
        kb.checkArity();
        // load inference engine only when "TPTP" = "yes"
        if (KBmanager.getMgr().getPref("TPTP").equals("yes"))
            kb.loadEProver();
        return true;
    }
    
    /** ***************************************************************
     */
    private void fromXML(SimpleElement configuration) {

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
                        ArrayList<String> constituentsToAdd = new ArrayList<String>();
                        boolean useCacheFile = KBmanager.getMgr().getPref("cache").equalsIgnoreCase("yes");
                        for (int j = 0; j < element.getChildElements().size(); j++) {
                            SimpleElement kbConst = (SimpleElement) element.getChildElements().get(j);
                            if (!kbConst.getTagName().equals("constituent")) 
                            	System.out.println("Error in KBmanager.fromXML(): Bad tag: " + kbConst.getTagName());
                            String filename = (String) kbConst.getAttribute("filename");
                            if (!StringUtil.emptyString(filename)) {
                                if (filename.endsWith(KB._cacheFileSuffix)) {
                                    if (useCacheFile) 
                                        constituentsToAdd.add(filename);                                    
                                }
                                else 
                                    constituentsToAdd.add(filename);                                
                            }
                        }
                        loadKB(kbName, constituentsToAdd);
                    }
                    else 
                    	System.out.println("Error in KBmanager.fromXML(): Bad tag: " + element.getTagName());                    
                }
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
    public static void copyFile(File in, File out) { 
        
        FileInputStream fis  = null;
        FileOutputStream fos = null;
        try {
            fis = new FileInputStream(in);
            fos = new FileOutputStream(out);
            byte[] buf = new byte[1024];  
            int i = 0;  
            while ((i = fis.read(buf)) != -1) {  
                fos.write(buf, 0, i);  
            }  
            fos.flush();
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
        finally {  
            try {
                if (fis != null) fis.close();  
                if (fos != null) fos.close();  
            }
            catch (Exception ioe) {
                ioe.printStackTrace();
            }
        }  
        return;
    }

    /** ***************************************************************
     * Reads an XML configuration file from the directory
     * configDirPath, and tries to find a configuration file elsewhere
     * if configDirPath is null.  The method initializeOnce() sets the
     * preferences based on the contents of the configuration file.
     * This routine has the side effect of setting the variable called
     * "configuration".  It also creates the KBs directory and an
     * empty configuration file if none exists.
     */
    protected SimpleElement readConfiguration(String configDirPath) {
        
        SimpleElement configuration = null;
        BufferedReader br = null;
        try {
            String kbDirStr = configDirPath;
            if (StringUtil.emptyString(kbDirStr)) {
                kbDirStr = (String) preferences.get("kbDir");
                if (StringUtil.emptyString(kbDirStr)) 
                    kbDirStr = System.getProperty("user.dir");                
            }
            File kbDir = new File(kbDirStr);
            if (!kbDir.exists()) {
                kbDir.mkdir();
                preferences.put("kbDir", kbDir.getCanonicalPath());
            }
            String username = (String) preferences.get("userName");
            String userrole = (String) preferences.get("userRole");
            String config_file = ((StringUtil.isNonEmptyString(username)
                                   && StringUtil.isNonEmptyString(userrole)
                                   && userrole.equalsIgnoreCase("administrator") 
                                   && !username.equalsIgnoreCase("admin"))
                                  ? (username + "_")
                                  : "") + CONFIG_FILE;
            File configFile = new File(kbDir, config_file);
            File global_config = new File(kbDir, CONFIG_FILE);
            if (!configFile.exists()) {
                if (global_config.exists()) {
                    copyFile(global_config, configFile);
                    configFile = global_config;
                }
                else 
                    writeConfiguration();
            }
            br = new BufferedReader(new FileReader(configFile));
            SimpleDOMParser sdp = new SimpleDOMParser();
            configuration = sdp.parse(br);
        }
        catch (Exception ex) {
            System.out.println("ERROR in KBmanager.readConfiguration(" + configDirPath
                               + "):\n" + "  Exception parsing configuration file \n" + ex.getMessage());
            ex.printStackTrace();
        }
        finally {
            try {
                if (br != null) 
                    br.close();                
            }
            catch (Exception ex2) {
                ex2.printStackTrace();
            }
        }
        return configuration;
    }

    /** ***************************************************************
     * Reads in the KBs and other parameters defined in the XML
     * configuration file, or uses the default parameters.  
     */
    public boolean initializeOnce() {
        
        System.out.println("Info in KBmanager.initializeOnce()");
        String base = System.getenv("SIGMA_HOME");
        return initializeOnce(base + File.separator + "KBs");
    }

    /** ***************************************************************
     * Reads in the KBs and other parameters defined in the XML
     * configuration file, or uses the default parameters.  If
     * configFileDir is not null and a configuration file can be read
     * from the directory, reinitialization is forced.
     */
    public boolean initializeOnce(String configFileDir) {

        boolean performedInit = false;
        if (initialized)
            return false;
        try {
            initializing = true;
            if (StringUtil.isNonEmptyString(configFileDir)) {    
                setDefaultAttributes();
                SimpleElement configuration = readConfiguration(configFileDir);
                if (configuration == null) 
                    throw new Exception("Error reading configuration file in KBmanager.initializeOnce()");

                setConfiguration(configuration);
            }
            else
                setDefaultAttributes();
            performedInit = true;
        }
        catch (Exception ex) {
        	System.out.println(ex.getMessage());
            ex.printStackTrace();
        }        
        initialized = true;
        initializing = false;           
        return performedInit;
    }

    /** ***************************************************************
     * Sets instance fields by reading the xml found in the configuration file.
     * @param configuration
     */
    void setConfiguration(SimpleElement configuration) {
        preferencesFromXML(configuration);
        kbsFromXML(configuration);
        String kbDir = (String) preferences.get("kbDir");
        //System.out.println("Info in KBmanager.initializeOnce(): Using kbDir: " + kbDir);
        NLGUtils.readKeywordMap(kbDir);
        WordNet.wn.initOnce();
        OMWordnet.readOMWfiles();
        if (kbs != null && kbs.size() > 0) {
            Iterator<String> it = kbs.keySet().iterator();
            while (it.hasNext()) {
                String kbName = it.next();
                System.out.println("INFO in KBmanager.setConfiguration(): " + kbName);
                WordNet.wn.termFormatsToSynsets(KBmanager.getMgr().getKB(kbName));
            }
        }
        else
            System.out.println("Error in KBmanager.setConfiguration(): No kbs");
    }

    /** ***************************************************************
     * Double the backslash in a filename so that it can be saved to a text
     * file and read back properly.
     */
    public static String escapeFilename(String fname) {

        StringBuilder newstring = new StringBuilder("");        
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
        addKB(name, true);        
    }

    public void addKB(String name, boolean isVisible) {
    	
        KB kb = new KB(name,(String) preferences.get("kbDir"), isVisible);
        kbs.put(name.intern(),kb); 
    }

    /** ***************************************************************
     * Remove a knowledge base.
     * @param name - the name of the KB
     */
    public String removeKB(String name) {

        KB kb = (KB) kbs.get(name);
        if (kb == null) 
            return "KB " + name + " does not exist and cannot be removed.";        
        try {
            if (kb.eprover != null) 
                kb.eprover.terminate();
        }
        catch (Exception ioe) {
            System.out.println("Error in KBmanager.removeKB(): ");
            System.out.println("  Error terminating inference engine: " + ioe.getMessage());
        }
        kbs.remove(name);
        try {
            //writeConfiguration();
        }
        catch (Exception ioe) {
        	System.out.println("Error in KBmanager.removeKB(): ");
        	System.out.println("  Error writing configuration file: " + ioe.getMessage());
        }
        return "KB " + name + " successfully removed.";
    }

    /** ***************************************************************
     * Write the current configuration of the system.  Call 
     * writeConfiguration() on each KB object to write its manifest.
     */
    public void writeConfiguration() throws IOException {
        
        System.out.println("INFO in KBmanager.writeConfiguration()");
        FileWriter fw = null;
        PrintWriter pw = null;
        String dir = (String) preferences.get("kbDir");
        File fDir = new File(dir);
        String username = (String) preferences.get("userName");
        String userrole = (String) preferences.get("userRole");
        String config_file = (((username != null) 
                               && userrole.equalsIgnoreCase("administrator") 
                               && !username.equalsIgnoreCase("admin"))
                              ? username + "_" 
                              : "") + CONFIG_FILE;
        File file = new File(fDir, config_file);
        String canonicalPath = file.getCanonicalPath();

        SimpleElement configXML = new SimpleElement("configuration");
        Iterator<String> it = preferences.keySet().iterator();
        while (it.hasNext()) {
        	String key = it.next();
        	String value = preferences.get(key);
            if (Arrays.asList("kbDir","celtdir","inferenceEngine",
            		"inferenceTestDir","leoExecutable").contains(key))
                value = escapeFilename(value);
            if (!Arrays.asList("userName", "userRole").contains(key)) {
                SimpleElement preference = new SimpleElement("preference");
                preference.setAttribute("name",key);
                preference.setAttribute("value",value);
                configXML.addChildElement(preference);
            }
        }
        Iterator<String> it2 = kbs.keySet().iterator();
        while (it2.hasNext()) {
        	String key = it2.next();
            KB kb = kbs.get(key);
            SimpleElement kbXML = kb.writeConfiguration();            
            configXML.addChildElement(kbXML);
        }
        try {
            fw = new FileWriter(file);
            pw = new PrintWriter(fw);
            pw.println(configXML.toFileString());
        }
        catch (java.io.IOException e) {                                                
            System.out.println("Error writing file " + canonicalPath + ".\n " + e.getMessage());
            throw new IOException("Error writing file " + canonicalPath + ".\n " + e.getMessage());
        }
        finally {
            if (pw != null) 
                pw.close();            
            if (fw != null) 
                fw.close();            
        }
        return;
    }

    /** ***************************************************************
     * Get the KB that has the given name.
     */
    public KB getKB(String name) {

        if (!kbs.containsKey(name))
        	System.out.println("KB " + name + " not found.");
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
     * Reset the one instance of KBmanager from its class variable.
     */
    public static KBmanager newMgr(String username) {

        manager = new KBmanager();
        manager.initialized = false;
        String userRole = PasswordService.getInstance().getUser(username).getRole();
        manager.setPref("userName",username);
        manager.setPref("userRole", userRole);     
        return manager;
    }
    
    /** ***************************************************************
     * Get the Set of KB names in this manager.
     */
    public HashSet<String> getKBnames() {
        
        HashSet<String> names = new HashSet<String>();        
        Iterator<String> it = kbs.keySet().iterator();
        while (it.hasNext()) {
            String kbName = (String) it.next();
            KB kb = (KB) getKB(kbName);
            if (kb.isVisible())
                names.add(kbName);
        }
        return names;
    }
    
    /** ***************************************************************
     * Get the the complete list of languages available in all KBs
     */
    public ArrayList<String> allAvailableLanguages() {

        ArrayList<String> result = new ArrayList<String>();
        Iterator<String> it = kbs.keySet().iterator();
        while (it.hasNext()) {
            String kbName = (String) it.next();
            KB kb = (KB) getKB(kbName);
            result.addAll(kb.availableLanguages());
        }
        return result;
    }
    
    /** ***************************************************************
     * Get the preference corresponding to the given kef.
     */    
    public String getPref(String key) {
        
        String ans = (String) preferences.get(key);
        if (ans == null) 
            ans = "";        
        return ans;
    }
    
    /** ***************************************************************
     * Set the preference to the given value.
     */
    public void setPref(String key, String value) {
        
        preferences.put(key,value);
    }

    /** ***************************************************************
     * Returns an int value, the bitwise interpretation of which
     * indicates the current configuration of inference parameter
     * (preference) settings.  The int value is computed from the
     * KBmanager preferences at the time this method is evaluated.
     *
     * @return An int value indicating the current configuration of
     * inference parameters, according to KBmanager preference
     * settings.
     */
    public int getInferenceBitValue() {
        
        int bv = 0;
        String[] keys = { "typePrefix", "holdsPrefix", "cache", "TPTP" };
        int[] vals = { USE_TYPE_PREFIX, USE_HOLDS_PREFIX, USE_CACHE, USE_TPTP };
        String pref = null;
        for (int i = 0; i < keys.length; i++) {
            pref = this.getPref( keys[i] );
            if (!StringUtil.emptyString(pref) && pref.equalsIgnoreCase("yes")) 
                bv += vals[i];            
        }
        return bv;
    }

    /** ***************************************************************
     * Returns the last cached inference bit value setting.
     *
     * @return An int value indicating the inference parameter
     * configuration at the time the value was set.
     */
    public int getOldInferenceBitValue() {
        
        return this.oldInferenceBitValue;
    }

    /** ***************************************************************
     * Sets the value of the private variable oldInferenceBitValue.
     *
     * @return void
     */
    public void setOldInferenceBitValue (int bv) {
        
        this.oldInferenceBitValue = bv;
        return;
    }

    /** ***************************************************************
     * A test method.
     */
    public static void main(String[] args) {

        try {
            KBmanager.getMgr().initializeOnce();
        } 
        catch (Exception e ) {
            System.out.println(e.getMessage());
        }
        KB kb = KBmanager.getMgr().getKB("SUMO");
        Formula f = new Formula();
        f.read("(=> (and (wears ?A ?C) (part ?P ?C)) (wears ?A ?P))");
        FormulaPreprocessor fp = new FormulaPreprocessor();
        System.out.println(fp.preProcess(f,false,kb));
    }
}
