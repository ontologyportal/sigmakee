/** This code is copyright Articulate Software (c) 2003.  Some portions
copyright Teknowledge (c) 2003 and reused under the terms of the GNU license.
This software is released under the GNU Public License <http://www.gnu.org/copyleft/gpl.html>.
Users of this code also consent, by use of this code, to credit Articulate Software
and Teknowledge in any writings, briefings, publications, presentations, or
other representations of any software which incorporates, builds on, or uses this
code.  Please cite the following article in any publication with references:

Pease, A., (2003). The Sigma Ontology Development Environment,
in Working Notes of the IJCAI-2003 Workshop on Ontology and Distributed Systems,
August 9, Acapulco, Mexico. See also http://github.com/ontologyportal

 Authors:
 Adam Pease
 Infosys LTD.
*/

package com.articulate.sigma;

import com.articulate.sigma.CCheckManager.CCheckStatus;
import com.articulate.sigma.VerbNet.VerbNet;
import com.articulate.sigma.nlg.NLGUtils;
import com.articulate.sigma.trans.SUMOKBtoTPTPKB;
import com.articulate.sigma.utils.StringUtil;
import com.articulate.sigma.wordNet.OMWordnet;
import com.articulate.sigma.wordNet.WordNet;
import py4j.GatewayServer;

import java.io.*;
import java.util.*;

/** This is a class that manages a group of knowledge bases.  It should only
 *  have one instance, contained in its own static member variable.
 */
public class KBmanager implements Serializable {

    private static CCheckManager ccheckManager = new CCheckManager();

    private static KBmanager manager = new KBmanager();
    protected static final String CONFIG_FILE = "config.xml";

    // preferences set before initialization that override values in config.xml
    public static HashMap<String,String> prefOverride = new HashMap<String,String>();
    private HashMap<String,String> preferences = new HashMap<String,String>();
    public HashMap<String,KB> kbs = new HashMap<String,KB>();
    public static boolean initialized = false;
    public static boolean initializing = false;
    public static boolean debug = false;
    private String error = "";

    public static final List<String> configKeys =
            Arrays.asList("sumokbname", "testOutputDir", "TPTPDisplay", "semRewrite",
                    "eprover", "inferenceTestDir", "baseDir", "hostname",
                    "logLevel", "systemsDir", "dbUser", "loadFresh", "userBrowserLimit",
                    "adminBrowserLimit", "https", "graphWidth", "overwrite", "typePrefix",
                    "graphDir", "nlpTools","TPTP","TPTPlang","cache","editorCommand","graphVizDir",
                    "kbDir","loadCELT","celtdir","lineNumberCommand","prolog","port",
                    "tptpHomeDir","showcached","leoExecutable","holdsPrefix","logDir",
                    "englishPCFG","multiWordAnnotatorType","dbpediaSrcDir", "vampire",
                    "reportDup", "reportFnError", "verbnet", "jedit", "editdir", "termFormats",
                    "loadLexicons", "cacheDisjoint");

    public static final List<String> fileKeys =
            Arrays.asList("testOutputDir", "eprover", "inferenceTestDir", "baseDir",
                    "systemsDir","graphVizDir", "kbDir", "celtdir", "tptpHomeDir", "logDir",
                    "englishPCFG");

    public enum Prover { NONE, EPROVER, VAMPIRE, LEO };

    public Prover prover = Prover.VAMPIRE;

    /** ***************************************************************
     */
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

    /** ***************************************************************
     *  Check whether sources are newer than serialized version.
     */
    public static boolean serializedExists() {

        String kbDir = System.getenv("SIGMA_HOME") + File.separator + "KBs";
        File serfile = new File(kbDir + File.separator + "kbmanager.ser");
        System.out.println("KBmanager.serializedExists(): " + serfile.exists());
        return serfile.exists();
    }

    /** ***************************************************************
     *  Check whether sources are newer than serialized version.
     */
    public static boolean serializedOld(SimpleElement configuration) {

        System.out.println("KBmanager.serializedOld(config): ");
        String kbDir = System.getenv("SIGMA_HOME") + File.separator + "KBs";
        File configFile = new File(kbDir + File.separator + "config.xml");
        Date configDate = new Date(configFile.lastModified());
        File serfile = new File(kbDir + File.separator + "kbmanager.ser");
        Date saveDate = new Date(serfile.lastModified());
        System.out.println("KBmanager.serializedOld(config): save date: " + saveDate.toString());
        if (saveDate.compareTo(configDate) < 0)
            return true;
        ArrayList<ArrayList<String>> kbFilenames = kbFilenamesFromXML(configuration);
        for (ArrayList<String> thekb : kbFilenames) { // iterate through the kbs
            for (String f : thekb) { // iterate through the constituents
                File file = new File(f);
                Date fileDate = new Date(file.lastModified());
                System.out.println("serializedOld(): file " + f + " was saved on " + fileDate);
                if (saveDate.compareTo(fileDate) < 0) {
                    return true;
                }
            }
        }
        System.out.println("KBmanager.serializedOld(config): returning false (not old)");
        return false;
    }

    /** ***************************************************************
     *  Check whether config file or any .kif constituent is newer than its
     *  corresponding TPTP/TFF/THF file
     */
    public boolean infFileOld(String lang) {

        String kbDir = System.getenv("SIGMA_HOME") + File.separator + "KBs";
        File configFile = new File(kbDir + File.separator + "config.xml");
        Date configDate = new Date(configFile.lastModified());
        for (String kbname : kbs.keySet()) { // iterate through the kbs
            KB kb = getKB(kbname);
            File file = new File(kbDir + File.separator + kbname + "." + lang);
            Date fileDate = new Date(file.lastModified());
            System.out.println("KBmanager.infFileOld(): file " + kbname + "." + lang + " was saved on " + fileDate);
            if (fileDate.compareTo(configDate) < 0) {
                return true;
            }
            for (String f : kb.constituents) { // iterate through the constituents
                File sfile = new File(f);
                Date sfileDate = new Date(sfile.lastModified());
                System.out.println("KBmanager.infFileOld(): file " + f + " was saved on " + sfileDate);
                if (fileDate.compareTo(sfileDate) < 0) {
                    return true;
                }
            }
        }
        System.out.println("KBmanager.infFileOld(config): returning false (not old)");
        return false;
    }

    /** ***************************************************************
     *  Check whether config file or any .kif constituent is newer than its
     *  corresponding TPTP/TFF/THF file
     */
    public boolean infFileOld() {

        System.out.println("KBmanager.tptpOld(config): ");
        String lang = "tff";
        if (SUMOKBtoTPTPKB.lang.equals("fof"))
            lang = "tptp";
        return infFileOld(lang);
    }

    /** ***************************************************************
     *  Load the most recently saved serialized version.
     */
    public static boolean loadSerialized() {

        manager = null;
        try {
            String kbDir = System.getenv("SIGMA_HOME") + File.separator + "KBs";
            FileInputStream file = new FileInputStream(kbDir + File.separator + "kbmanager.ser");
            ObjectInputStream in = new ObjectInputStream(file);
            // Method for deserialization of object
            KBmanager temp = (KBmanager) in.readObject();
            manager = temp;
            in.close();
            file.close();
            System.out.println("KBmanager.loadSerialized(): KBmanager has been deserialized ");
            initialized = true;
        }
        catch (java.io.InvalidClassException ex) {
            System.out.println("Error in KBmanager.loadSerialized(): InvalidClassException is caught");
            ex.printStackTrace();
            return false;
        }
        catch (Exception ex) {
            System.out.println("Error in KBmanager.loadSerialized(): IOException is caught");
            ex.printStackTrace();
            return false;
        }
        manager.preferences.putAll(prefOverride);
        return true;
    }

    /** ***************************************************************
     *  save serialized version.
     */
    public static void serialize() {

        try {
            // Reading the object from a file
            String kbDir = System.getenv("SIGMA_HOME") + File.separator + "KBs";
            FileOutputStream file = new FileOutputStream(kbDir + File.separator + "kbmanager.ser");
            ObjectOutputStream out = new ObjectOutputStream(file);
            // Method for deserialization of object
            out.writeObject(manager);
            out.close();
            file.close();
            System.out.println("KBmanager.serialize(): KBmanager has been serialized ");
        }
        catch (IOException ex) {
            System.out.println("Error in KBmanager.serialize(): IOException is caught");
            ex.printStackTrace();
        }
    }

    /** ***************************************************************
     * Set default attribute values if not in the configuration file.
     */
    public void setDefaultAttributes() {

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
            if (!graphDir.exists())
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
            preferences.put("cache","yes");
            preferences.put("TPTP","yes");
            preferences.put("TPTPDisplay","no");
            preferences.put("userBrowserLimit","25");
            preferences.put("adminBrowserLimit","200");
            preferences.put("port","8080");
            preferences.put("hostname","localhost");
            preferences.put("https","false");
            preferences.put("sumokbname","SUMO");

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
        	System.out.println("Error in KBmanager.preferencesFromXML(): Bad tag: " + configuration.getTagName());
        else {
            for (int i = 0; i < configuration.getChildElements().size(); i++) {
                SimpleElement element = (SimpleElement) configuration.getChildElements().get(i);
                if (element.getTagName().equals("preference")) {
                    String name = (String) element.getAttribute("name");
                    String value = (String) element.getAttribute("value");
                    //System.out.println("KBmanager.preferencesFromXML(): Adding: " + name + " " + value);
                    if (name != null && value != null && name.equals("holdsPrefix") && value.equals("yes"))
                        System.out.println("Warning: KBmanager.preferencesFromXML(): holds prefixing is deprecated.");
                    preferences.put(name,value);
                }
                else
                    if (!element.getTagName().equals("kb"))
                        System.out.println("Error in KBmanager.preferencesFromXML(): Bad tag: " + element.getTagName());
            }
        }
        if (debug) System.out.println("KBmanager.preferencesFromXML(): number of preferences: " +
                preferences.keySet().size());
    }

    /** ***************************************************************
     * Note that filenames that are not full paths are prefixed with the
     * value of preference kbDir
     */
    private static void kbsFromXML(SimpleElement configuration) {

        long milis = System.currentTimeMillis();
        boolean SUMOKBexists = false;
        if (!configuration.getTagName().equals("configuration"))
        	System.out.println("Error in KBmanager.kbsFromXML(): Bad tag: " + configuration.getTagName());
        else {
            for (int i = 0; i < configuration.getChildElements().size(); i++) {
                SimpleElement element = (SimpleElement) configuration.getChildElements().get(i);
                if (element.getTagName().equals("kb")) {
                    String kbName = (String) element.getAttribute("name");
                    if (kbName.equals(getMgr().getPref("sumokbname")))
                        SUMOKBexists = true;
                    KBmanager.getMgr().addKB(kbName);
                    ArrayList<String> constituentsToAdd = new ArrayList<String>();
                    boolean useCacheFile = KBmanager.getMgr().getPref("cache").equalsIgnoreCase("yes");
                    for (int j = 0; j < element.getChildElements().size(); j++) {
                        SimpleElement kbConst = (SimpleElement) element.getChildElements().get(j);
                        if (!kbConst.getTagName().equals("constituent"))
                        	System.out.println("Error in KBmanager.kbsFromXML(): Bad tag: " + kbConst.getTagName());
                        String filename = (String) kbConst.getAttribute("filename");
                        if (!filename.startsWith((File.separator)))
                            filename = KBmanager.getMgr().getPref("kbDir") + File.separator + filename;
                        if (!StringUtil.emptyString(filename)) {
                            if (KButilities.isCacheFile(filename)) {
                                if (useCacheFile)
                                    constituentsToAdd.add(filename);
                            }
                            else
                                constituentsToAdd.add(filename);
                        }
                    }
                    KBmanager.getMgr().loadKB(kbName, constituentsToAdd);
                }
            }
        }
        System.out.println("kbsFromXML(): Completed loading KBs");
        System.out.println("kbsFromXML(): seconds: " + (System.currentTimeMillis() - milis) / 1000);
        if (!SUMOKBexists)
            System.out.println("Error in KBmanager.kbsFromXML(): no SUMO kb.  Some Sigma functions will not work.");
    }

    /** ***************************************************************
     * Note that filenames that are not full paths are prefixed with the
     * value of preference kbDir
     */
    private static ArrayList<ArrayList<String>> kbFilenamesFromXML(SimpleElement configuration) {

        ArrayList<ArrayList<String>> result = new ArrayList<>();
        if (!configuration.getTagName().startsWith("configuration")) {
            System.out.println("Error in KBmanager.kbsFilenamesFromXML(): Bad tag: " + configuration.getTagName());
            System.out.println("Error in KBmanager.kbsFilenamesFromXML(): expected <configuration>");
        }
        else {
            for (int i = 0; i < configuration.getChildElements().size(); i++) {
                SimpleElement element = (SimpleElement) configuration.getChildElements().get(i);
                if (element.getTagName().equals("kb")) {
                    ArrayList<String> kb = new ArrayList<>();
                    result.add(kb);
                    boolean useCacheFile = KBmanager.getMgr().getPref("cache").equalsIgnoreCase("yes");
                    for (int j = 0; j < element.getChildElements().size(); j++) {
                        SimpleElement kbConst = (SimpleElement) element.getChildElements().get(j);
                        if (!kbConst.getTagName().equals("constituent")) {
                            System.out.println("Error in KBmanager.kbsFilenamesFromXML(): Bad tag: " + kbConst.getTagName());
                            System.out.println("Error in KBmanager.kbsFilenamesFromXML(): expected <constituent>");
                        }
                        String filename = (String) kbConst.getAttribute("filename");
                        if (!filename.startsWith((File.separator)))
                            filename = KBmanager.getMgr().getPref("kbDir") + File.separator + filename;
                        if (!StringUtil.emptyString(filename)) {
                            if (KButilities.isCacheFile(filename)) {
                                if (useCacheFile)
                                    kb.add(filename);
                            }
                            else
                                kb.add(filename);
                        }
                    }
                }
            }
        }
        System.out.println("kbsFilenamesFromXML(): Completed loading KB names");
        return result;
    }

    /** ***************************************************************
     */
    public void loadKBforInference(KB kb) {

        System.out.println("KBmanager.loadKBforInference(): KB: " + kb.name);
        if (KBmanager.getMgr().getPref("TPTP").equals("yes")) {
            if (KBmanager.getMgr().prover.equals(Prover.VAMPIRE)) {
                System.out.println("KBmanager.loadKBforInference(): loading Vampire");
                kb.loadVampire();
            }
            else if (KBmanager.getMgr().prover.equals(Prover.EPROVER)) {
                System.out.println("KBmanager.loadKBforInference(): loading EProver");
                kb.loadEProver();
            }
        }
    }

    /** ***************************************************************
     */
    public boolean loadKB(String kbName, List<String> constituents) {

        KB kb = null;
        try {
            if (existsKB(kbName))
                removeKB(kbName);
            addKB(kbName);
            kb = getKB(kbName);
            if (!(constituents.isEmpty())) {
                for (String filename : constituents) {
                    try {
                        System.out.println("KBmanager.loadKB(): add constituent " + filename + " to " + kbName);
                        kb.addConstituent(filename);
                    }
                    catch (Exception e1) {
                    	System.out.println("Error in KBmanager.loadKB():  " + e1.getMessage());
                    	e1.printStackTrace();
                        return false;
                    }
                }
            }
        }
        catch (Exception e) {
        	System.out.println("Error in KBmanager.loadKB(): Unable to save configuration: " + e.getMessage());
        	e.printStackTrace();
            return false;
        }

        long millis = System.currentTimeMillis();
        kb.kbCache = new KBcache(kb);
        kb.kbCache.buildCaches();
        kb.checkArity();
        System.out.println("KBmanager.loadKB(): seconds: " + (System.currentTimeMillis() - millis) / 1000);
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
                    if (!configKeys.contains(name)) {
                        System.out.println("Error in KBmanager.fromXML(): Bad key: " + name);
                        // continue; // set it anyway
                    }
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
                                if (KButilities.isCacheFile(filename)) {
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
            preferences.putAll(prefOverride);
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

        try (FileInputStream fis = new FileInputStream(in);
             FileOutputStream fos = new FileOutputStream(out)
        ){
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

        System.out.println("KBmanager.readConfiguration()");
        SimpleElement configuration = null;
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
            String config_file = CONFIG_FILE;
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
            try (BufferedReader br = new BufferedReader(new FileReader(configFile))) {
                SimpleDOMParser sdp = new SimpleDOMParser();
                configuration = sdp.parse(br);
            }
        }
        catch (Exception ex) {
            System.out.println("ERROR in KBmanager.readConfiguration(" + configDirPath
                               + "):\n" + "  Exception parsing configuration file \n" + ex.getMessage());
            ex.printStackTrace();
        }
        return configuration;
    }

    /** ***************************************************************
     * Reads in the KBs and other parameters defined in the XML
     * configuration file, or uses the default parameters.
     */
    public void initializeOnce() {

        System.out.println("Info in KBmanager.initializeOnce()");
        //Thread.dumpStack();
        String base = System.getenv("SIGMA_HOME");
        initializeOnce(base + File.separator + "KBs");
        return;
    }

    /** ***************************************************************
     * Reads in the KBs and other parameters defined in the XML
     * configuration file, or uses the default parameters.  If
     * configFileDir is not null and a configuration file can be read
     * from the directory, reinitialization is forced.
     */
    public void initializeOnce(String configFileDir) {

        long millis = System.currentTimeMillis();
        boolean loaded = false;
        if (initializing || initialized) {
            System.out.println("Info in KBmanager.initializeOnce(): initialized is " + initialized);
            System.out.println("Info in KBmanager.initializeOnce(): initializing is " + initializing);
            System.out.println("Info in KBmanager.initializeOnce(): returning ");
            return;
        }
        initializing = true;
        KBmanager.getMgr().setPref("kbDir",configFileDir);
        if (debug) System.out.println("KBmanager.initializeOnce(): number of preferences: " +
                preferences.keySet().size());
        try {
            System.out.println("Info in KBmanager.initializeOnce(): initializing with " + configFileDir);
            SimpleElement configuration = readConfiguration(configFileDir);
            if (configuration == null)
                throw new Exception("Error reading configuration file in KBmanager.initializeOnce()");
            if (serializedExists() && !serializedOld(configuration)) {
                if (debug) System.out.println("KBmanager.initializeOnce(): serialized exists and is not old ");
                loaded = loadSerialized();
                if (loaded) {
                    if (debug) System.out.println("KBmanager.initializeOnce(): manager is loaded ");
                    if (!prefEquals("loadLexicons","false")) {
                        WordNet.initOnce();
                        NLGUtils.init(configFileDir);
                        OMWordnet.readOMWfiles();
                        if (!VerbNet.disable) {
                            VerbNet.initOnce();
                            VerbNet.processVerbs();
                        }
                    }
                    else {
                        WordNet.disable = true;
                        VerbNet.disable = true;
                        OMWordnet.disable = true;
                    }
                    if (debug) System.out.println("KBmanager.initializeOnce(): kbs: " + manager.kbs.values());
                    initializing = false;
                    initialized = true;
                }
            }
            if (!loaded) { // if there was an error loading the serialized file, or there is none,
                            // then reload from sources
                System.out.println("Info in KBmanager.initializeOnce(): reading from sources");
                if (debug) System.out.println("KBmanager.initializeOnce(): number of preferences: " +
                        preferences.keySet().size());
                manager = this;
                KBmanager.getMgr().setPref("kbDir",configFileDir); // need to restore config file path
                if (StringUtil.isNonEmptyString(configFileDir)) {
                    setDefaultAttributes();
                    setConfiguration(configuration);
                }
                else
                    setDefaultAttributes();
                System.out.println("Info in KBmanager.initializeOnce(): completed initialization");
                if (debug) System.out.println("KBmanager.initializeOnce(): kbs: " + manager.kbs.values());
                serialize();
                initializing = false;
                initialized = true;
                for (KB kb : kbs.values())  // transform to TPTP only once all other initialization complete
                    loadKBforInference(kb);
            }
        }
        catch (Exception ex) {
        	System.out.println(ex.getMessage());
            ex.printStackTrace();
            return;
        }
        System.out.println("Info in KBmanager.initializeOnce(): initialized is " + initialized);
        if (debug) System.out.println("KBmanager.initializeOnce(): number of preferences: " +
                preferences.keySet().size());
        System.out.println("KBmanager.initializeOnce(): total init time in seconds: " + (System.currentTimeMillis() - millis) / 1000);
        return;
    }

    /** ***************************************************************
     * Sets instance fields by reading the xml found in the configuration file.
     * @param configuration
     */
    public void setConfiguration(SimpleElement configuration) {

        System.out.println("Info in KBmanager.setConfiguration():");
        preferencesFromXML(configuration);
        kbsFromXML(configuration);
        String kbDir = (String) preferences.get("kbDir");
        String sep = File.separator;
        System.out.println("Info in KBmanager.setConfiguration(): Using kbDir: " + kbDir);
        long milis = System.currentTimeMillis();
        NLGUtils.init(kbDir);
        if (!prefEquals("loadLexicons","false")) {
            WordNet.wn.initOnce();
            VerbNet.initOnce();
            VerbNet.processVerbs();
            OMWordnet.readOMWfiles();
        }
        System.out.println("KBmanager.setConfiguration(): linguistics load time: " + (System.currentTimeMillis() - milis) / 1000);
        if (kbs != null && kbs.size() > 0 && !WordNet.initNeeded) {
            for (String kbName : kbs.keySet()) {
                System.out.println("INFO in KBmanager.setConfiguration(): " + kbName);
                File f3 = new File(kbDir + sep + kbName + KB._userAssertionsString);
                f3.delete();
                File f4 = new File(kbDir + sep + kbName + KB._userAssertionsTPTP);
                f4.delete();
                if (KBmanager.getMgr().getPref("termFormats").equals("yes") && !prefEquals("loadLexicons","false")) {
                    WordNet.wn.termFormatsToSynsets(KBmanager.getMgr().getKB(kbName));
                    WordNet.wn.serialize(); // have to serialize it again if there are new synsets
                }
                else
                    System.out.println("INFO in WordNet.termFormatsToSynsets(): term format to synsets is not activated");
            }
        }
        else
            System.out.println("Error in KBmanager.setConfiguration(): No kbs");
        if (debug) System.out.println("KBmanager.setConfiguration(): number of preferences: " +
                preferences.keySet().size());
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
        String dir = preferences.get("kbDir");
        File fDir = new File(dir);
        String username = preferences.get("userName");
        String userrole = preferences.get("userRole");
        String config_file = (((username != null) 
                               && userrole.equalsIgnoreCase("administrator") 
                               && !username.equalsIgnoreCase("admin"))
                              ? username + "_"
                              : "") + CONFIG_FILE;
        File file = new File(fDir, config_file);
        String canonicalPath = file.getCanonicalPath();

        SimpleElement configXML = new SimpleElement("configuration");

        for (Map.Entry<String, String> element : preferences.entrySet()) {
            String key = element.getKey();
            String value = element.getValue();
            if (fileKeys.contains(key))
                value = escapeFilename(value);
            if (!Arrays.asList("userName", "userRole").contains(key)) {
                SimpleElement preference = new SimpleElement("preference");
                preference.setAttribute("name",key);
                preference.setAttribute("value",value);
                configXML.addChildElement(preference);
            }
        }
        for (KB kb : kbs.values()) {
            SimpleElement kbXML = kb.writeConfiguration();
            configXML.addChildElement(kbXML);
        }
        try (FileWriter fw = new FileWriter(file);
             PrintWriter pw = new PrintWriter(fw)
        ) {
            pw.println(configXML.toFileString());
        }
        catch (java.io.IOException e) {
            System.out.println("Error writing file " + canonicalPath + ".\n " + e.getMessage());
            throw new IOException("Error writing file " + canonicalPath + ".\n " + e.getMessage());
        }
        return;
    }

    /** ***************************************************************
     * Get the KB that has the given name.
     */
    public KB getKB(String name) {

        if (!kbs.containsKey(name))
        	System.out.println("KBmanager.getKB(): KB " + name + " not found.");
        return kbs.get(name);
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

        //if (manager == null)
        //    manager = new KBmanager();
        return manager;
    }

    /** ***************************************************************
     * Get the Set of KB names in this manager.
     */
    public HashSet<String> getKBnames() {

        HashSet<String> names = new HashSet<String>();
        for (String kbName : kbs.keySet()) {
            KB kb = getKB(kbName);
            if (kb.isVisible()) {
                names.add(kbName);
            }
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
     * Print all preferences to stdout
     */
    public void printPrefs() {

        System.out.println("KBmanager.printPrefs()");
        if (preferences == null || preferences.size() == 0)
            System.out.println("KBmanager.printPrefs(): preference list is empty");
        for (String key : preferences.keySet()) {
            String value = preferences.get(key);
            System.out.println(key + " : " + value);
        }
    }

    /** ***************************************************************
     * Get the preference corresponding to the given key
     */
    public String getPref(String key) {

        if (!configKeys.contains(key)) {
            System.out.println("Error in KBmanager.getPref(): bad key: " + key);
            return "";
        }
        String ans = (String) preferences.get(key);
        if (ans == null)
            ans = "";
        return ans;
    }

    /** ***************************************************************
     * Safer than getPref().equals() since it can check for null
     */
    public boolean prefEquals(String key, String value) {

        if (!configKeys.contains(key)) {
            System.out.println("Error in KBmanager.getPref(): bad key: " + key);
            return false;
        }
        String ans = (String) preferences.get(key);
        if (ans == null)
            ans = "";
        if (ans.equals(value))
            return true;
        return false;
    }

    /** ***************************************************************
     * Set the preference to the given value.
     */
    public void setPref(String key, String value) {

        if (!configKeys.contains(key)) {
            System.out.println("Error in KBmanager.setPref(): bad key: " + key);
            return;
        }
        preferences.put(key,value);
    }

    /** ***************************************************************
     * Create an server-based interface for Python to call the KB object.
     * https://pypi.python.org
     *
     * from py4j.java_gateway import JavaGateway
     * gateway = JavaGateway()             # connect to the JVM
     * sigma_app = gateway.entry_point     # get the KB instance
     * print(sigma_app.getTerms())         # call a method
     */
    public static void pythonServer() {

        System.out.println("KBmanager.pythonServer(): begin initialization");
        try {
            KBmanager.getMgr().initializeOnce();
        }
        catch (Exception e ) {
            System.out.println(e.getMessage());
        }
        GatewayServer server = new GatewayServer(new PythonAPI());
        server.start();
        System.out.println("KBmanager.pythonServer(): completed initialization, server running");
    }

    /** ***************************************************************
     */
    public static void printHelp() {

        System.out.println("Sigma Knowledge Engineering Environment");
        System.out.println("  options:");
        System.out.println("  -h - show this help screen");
        System.out.println("  -p - demo Python interface");
        System.out.println("  with no arguments show this help screen an execute a test");
    }

    /** ***************************************************************
     */
    public static void main(String[] args) {

        if (args == null) {
            printHelp();
            try {
                KBmanager.getMgr().initializeOnce();
            }
            catch (Exception e) {
                System.out.println(e.getMessage());
            }
            KB kb = KBmanager.getMgr().getKB(KBmanager.getMgr().getPref("sumokbname"));
            Formula f = new Formula();
            f.read("(=> (and (wears ?A ?C) (part ?P ?C)) (wears ?A ?P))");
            FormulaPreprocessor fp = new FormulaPreprocessor();
            System.out.println(fp.preProcess(f, false, kb));
        }
        else {
            if (args != null && args.length > 0 && args[0].equals("-p")) {
                pythonServer();
            }
            if (args != null && args.length > 0 && args[0].equals("-h")) {
                printHelp();
            }
        }
    }
}
