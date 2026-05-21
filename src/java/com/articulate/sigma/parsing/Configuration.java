package com.articulate.sigma.parsing;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import com.articulate.sigma.utils.*;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;
import java.util.*;

public class Configuration {

    private final String configFilePath;
    private HashMap<String, String> preferences;
    private HashMap<String, List<String>> kbConstituentList;

    public static final List<String> CONFIG_KEYS = Arrays.asList(
        "adminBrowserLimit",
        "baseDir",
        "cache",
        "cacheDisjoint",
        "cwa",
        "dbUser",
        "eproverExec",
        "graphDir",
        "graphVizExec",
        "hostname",
        "https",
        "inferenceTestDir",
        "jeditExec",
        "kbDir",
        "loadSerialized",
        "loadLexicons",
        "leoExec",
        "port",
        "termFormats",
        "tptpExec",
        "userBrowserLimit",
        "vampireExec",
        "vampireHolExec",
        "verbnetDir",
        "ollamaLocalHost",
        "smtpEmailAddress",
        "smtpEmailUser",
        "smtpEmailPassword",
        "smtpEmailServer",
        "isAws"
    );

    public static final List<String> DIR_KEYS = Arrays.asList(
        "baseDir",
        "graphDir",
        "inferenceTestDir",
        "kbDir",
        "verbnetDir"
    );

    public static final List<String> EXECUTABLE_KEYS = Arrays.asList(
        "eproverExec",
        "graphVizExec",
        "jeditExec",
        "leoExec",
        "tptpExec",
        "vampireExec",
        "vampireHolExec"
    );

    public static final List<String> BOOLEAN_KEYS = Arrays.asList(
        "cache",
        "cacheDisjoint",
        "cwa",
        "https",
        "loadSerialized",
        "loadLexicons",
        "termFormats",
        "isAws"
    );

    public static final List<String> INTEGER_KEYS = Arrays.asList(
        "adminBrowserLimit",
        "port",
        "userBrowserLimit"
    );

    public static final List<String> STRING_KEYS = Arrays.asList(
        "dbUser",
        "hostname",
        "ollamaLocalHost",
        "smtpEmailAddress",
        "smtpEmailUser",
        "smtpEmailPassword",
        "smtpEmailServer"
    );

    /*****************************************************************
     * Returns a new configuration object populated with config.xml data.
     */
    public Configuration(String configPath) {

        this.configFilePath = configPath;
        setAllFromXml();
    }

    /*****************************************************************
     * Sets all preferences and KB constituents from the XML
     */
    private void setAllFromXml() {

        Document doc = readXmlFile(configFilePath);
        this.preferences = getPreferencesFromXml(doc);
        this.kbConstituentList = getKbConstituentListFromXml(doc);
        validateAllPreferencesFromXml();
    }

    /*****************************************************************
     * Safely reads XML file, protecting against XML external entity injection
     * @param configPath path to the config.xml file.
     * @return XML file parsed as a document tree.
     */
    private Document readXmlFile(String configPath) {

        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
            factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            factory.setExpandEntityReferences(false);
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(new File(configPath));
            doc.getDocumentElement().normalize();
            return doc;
        }
        catch (Exception e) {
            throw new RuntimeException("Could not read config XML file: " + configPath, e);
        }
    }

    /*****************************************************************
     * Returns all preferences found in XML doc tree
     * @param doc Parsed XML doc tree
     * @return Map of preferences with associated values
     */
    private HashMap<String, String> getPreferencesFromXml(Document doc) {

        HashMap<String, String> prefs = new HashMap<>();
        NodeList nodes = doc.getElementsByTagName("preference");
        for (int i = 0; i < nodes.getLength(); i++) {
            Element preference = (Element) nodes.item(i);
            String name = preference.getAttribute("name");
            String value = preference.getAttribute("value");
            if (name != null && !name.isEmpty()) prefs.put(name, value);
        }
        return prefs;
    }

    /*****************************************************************
     * Returns all KBs and constituents found in XML doc tree
     * @param doc Parsed XML doc tree.
     * @return Map of KBs and associate constituents.
     */
    private HashMap<String, List<String>> getKbConstituentListFromXml(Document doc) {

        HashMap<String, List<String>> kbMap = new HashMap<>();
        NodeList kbNodes = doc.getElementsByTagName("kb");
        for (int i = 0; i < kbNodes.getLength(); i++) {
            Element kbElement = (Element) kbNodes.item(i);
            String kbName = kbElement.getAttribute("name");
            List<String> constituents = new ArrayList<>();
            NodeList constituentNodes = kbElement.getElementsByTagName("constituent");
            for (int j = 0; j < constituentNodes.getLength(); j++) {
                Element constituent = (Element) constituentNodes.item(j);
                String filename = constituent.getAttribute("filename");
                if (filename != null && !filename.isEmpty()) constituents.add(filename);
            }
            if (kbName != null && !kbName.isEmpty()) kbMap.put(kbName, constituents);
        }
        return kbMap;
    }

    /*****************************************************************
     * Sets all preferences to their default values.
     */
    public void setAllPreferencesAsDefault() {

        String sep = File.separator;
        String userHome = System.getProperty("user.home");
        String sigmaHome = System.getenv("SIGMA_HOME");
        String tomcatHome = System.getenv("CATALINA_HOME");

        this.preferences = new HashMap<>();
        this.preferences.put("adminBrowserLimit", "200");
        this.preferences.put("baseDir", sigmaHome);
        this.preferences.put("cache", "true");
        this.preferences.put("cacheDisjoint", "true");
        this.preferences.put("cwa", "false");
        this.preferences.put("dbUser", "SUMO");
        this.preferences.put("eproverExec", userHome + sep + "Programs" + sep + "E" + sep + "bin" + sep + "e_ltb_runner");
        this.preferences.put("graphDir", tomcatHome + sep + "webapps" + sep + "sigma" + sep + "graph");
        this.preferences.put("graphVizExec", "/usr/bin");
        this.preferences.put("hostname", "localhost");
        this.preferences.put("https", "false");
        this.preferences.put("inferenceTestDir", sigmaHome + sep + "tests");
        this.preferences.put("jeditExec", "/usr/share/jedit/jedit");
        this.preferences.put("kbDir", sigmaHome + sep + "KBs");
        this.preferences.put("loadSerialized", "true");
        this.preferences.put("loadLexicons", "true");
        this.preferences.put("leoExec", userHome + sep + "leo");
        this.preferences.put("port", "8080");
        this.preferences.put("termFormats", "true");
        this.preferences.put("tptpExec", userHome + sep + "workspace" + sep + "TPTP4X" + sep + "tptp4X");
        this.preferences.put("userBrowserLimit", "25");
        this.preferences.put("vampireExec", userHome + sep + "Programs" + sep + "vampire" + sep + "build" + sep + "vampire");
        this.preferences.put("vampireHolExec", userHome + sep + "Programs" + sep + "vampire" + sep + "build_hol" + sep + "vampire");
        this.preferences.put("verbnetDir", "");
        this.preferences.put("ollamaLocalHost", "http://127.0.0.1:11434");
        this.preferences.put("smtpEmailAddress", "");
        this.preferences.put("smtpEmailUser", "");
        this.preferences.put("smtpEmailPassword", "");
        this.preferences.put("smtpEmailServer", "");
        this.preferences.put("isAws", "false");
    }

    public void validateAllPreferencesFromXml() {

        for (String key : preferences.keySet()) {
            if (!CONFIG_KEYS.contains(key)) {
                System.out.println("WARNING: Unknown config preference: " + key);
            }
        }
    }

    public String getConfigFilePath() {
        return this.configFilePath;
    }

    public HashMap<String, String> getPreferences() {
        return this.preferences;
    }

    public String getPreference(String key) {
        return this.preferences.get(key);
    }

    public List<String> getKbConstituentList(String kb) {
        return this.kbConstituentList.get(kb);
    }

    public HashMap<String, List<String>> getAllKbConstituentLists() {
        return this.kbConstituentList;
    }

    /*****************************************************************
     * Warns about unknown preferences found in the XML.
     * @param defaults default preferences.
     */
    private void validateKnownPreferences(HashMap<String, String> defaults) {
        for (String key : preferences.keySet()) {
            if (!CONFIG_KEYS.contains(key)) LoggingUtils.log("WARN", "Unknown config preference: " + key);
        }
    }

    /*****************************************************************
     * Adds missing known preferences using default values.
     * @param defaults default preferences.
     */
    private void validateMissingPreferences(HashMap<String, String> defaults) {

        for (String key : CONFIG_KEYS) {
            if (!preferences.containsKey(key)) {
                String defaultValue = defaults.get(key);
                LoggingUtils.log("WARN", "Missing config preference: " + key + ". Setting default value: " + defaultValue);
                preferences.put(key, defaultValue);
            }
        }
    }

    /*****************************************************************
     * Validates directory preferences.
     * @param defaults default preferences.
     */
    private void validateDirectoryPreferences(HashMap<String, String> defaults) {

        for (String key : DIR_KEYS) {
            String value = preferences.get(key);
            if (StringUtil.emptyString(value)) {
                setDefaultForInvalidKey(key, value, defaults, "Directory value is empty");
                continue;
            }
            File dir = new File(value);
            if (!dir.exists() || !dir.isDirectory()) setDefaultForInvalidKey(key, value, defaults, "Directory does not exist");
        }
    }

    /*****************************************************************
     * Validates executable preferences.
     * @param defaults default preferences.
     */
    private void validateExecutablePreferences(HashMap<String, String> defaults) {

        for (String key : EXECUTABLE_KEYS) {
            String value = preferences.get(key);
            if (StringUtil.emptyString(value)) {
                setDefaultForInvalidKey(key, value, defaults, "Executable value is empty");
                continue;
            }
            File executable = new File(value);
            if (!executable.exists() || !executable.isFile()) setDefaultForInvalidKey(key, value, defaults, "Executable file does not exist");
        }
    }

    /*****************************************************************
     * Validates integer preferences.
     * @param defaults default preferences.
     */
    private void validateIntegerPreferences(HashMap<String, String> defaults) {

        for (String key : INTEGER_KEYS) {
            String value = preferences.get(key);
            if (!StringUtil.isInteger(value)) setDefaultForInvalidKey(key, value, defaults, "Value is not a valid integer");
        }
    }

    /*****************************************************************
     * Validates boolean preferences.
     * @param defaults default preferences.
     */
    private void validateBooleanPreferences(HashMap<String, String> defaults) {

        for (String key : BOOLEAN_KEYS) {
            String value = preferences.get(key);
            if (!StringUtil.isBoolean(value)) setDefaultForInvalidKey(key, value, defaults, "Value is not a valid boolean");
        }
    }

    /*****************************************************************
     * Validates string preferences.
     * @param defaults default preferences.
     */
    private void validateStringPreferences(HashMap<String, String> defaults) {

        for (String key : STRING_KEYS) {
            String value = preferences.get(key);
            if (StringUtil.emptyString(value)) setDefaultForInvalidKey(key, value, defaults, "String value is empty");
        }
    }

    /*****************************************************************
     * Logs an invalid preference and resets it to its default value.
     * @param key preference key.
     * @param invalidValue invalid preference value.
     * @param defaults default preferences.
     * @param reason reason the value is invalid.
     */
    private void setDefaultForInvalidKey(String key, String invalidValue, HashMap<String, String> defaults, String reason) {

        String defaultValue = defaults.get(key);
        LoggingUtils.log("WARN", "Invalid config preference: " + key +
                " value: " + invalidValue +
                ". Reason: " + reason +
                ". Setting default value: " + defaultValue);
        preferences.put(key, defaultValue);
    }

    /******************************************************************
     */
    public static void showHelp() {

        System.out.println("Configuration.main() Options:");
        System.out.println("  -h - show this help screen");
        System.out.println("  -p - print configuration from xml");
        
    }

    /******************************************************************
     * Test method for this class.
     */
    public static void main(String args[]) {

        Map<String, List<String>> argMap = CLIMapParser.parse(args);
        if (argMap.isEmpty() || argMap.containsKey("h")) showHelp();
        else  {
            Configuration config = new Configuration("");
            if (argMap.containsKey("a")) {
            
            }
        }
    }
}