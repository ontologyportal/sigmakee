package com.articulate.sigma.parsing;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import com.articulate.sigma.utils.*;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.File;
import java.util.*;


public class Configuration {

    private final String configFilePath;
    private HashMap<String, String> preferences;
    private HashMap<String, List<String>> kbConstituentList;
    private List<String> warnings = new ArrayList<>();
    private List<String> errors = new ArrayList<>();

    public static final List<String> CONFIG_KEYS = Arrays.asList(
        "adminBrowserLimit",
        "baseDir",
        "cache",
        "cacheDisjoint",
        "cwa",
        "eproverExec",
        "graphDir",
        "graphVizDir",
        "hostname",
        "https",
        "inferenceTestDir",
        "jeditExec",
        "kbDir",
        "loadFresh",
        "loadLexicons",
        "leoExec",
        "maxPredicateArity",
        "port",
        "termFormats",
        "tptpExec",
        "typePrefix",
        "userBrowserLimit",
        "vampireExec",
        "verbnetDir",
        "ollamaHost",
        "showCachedFormulas",
        "smtpEmailAddress",
        "smtpEmailUser",
        "smtpEmailPassword",
        "smtpEmailServer",
        "systemsDir",
        "isAws"
    );

    public static final List<String> DIR_KEYS = Arrays.asList(
        "baseDir",
        "graphDir",
        "inferenceTestDir",
        "kbDir",
        "verbnetDir",
        "systemsDir"
    );

    public static final List<String> EXECUTABLE_KEYS = Arrays.asList(
        "eproverExec",
        "graphVizDir",
        "jeditExec",
        "leoExec",
        "tptpExec",
        "vampireExec"
    );

    public static final List<String> BOOLEAN_KEYS = Arrays.asList(
        "cache",
        "cacheDisjoint",
        "cwa",
        "https",
        "loadFresh",
        "loadLexicons",
        "showCachedFormulas",
        "termFormats",
        "typePrefix",
        "isAws"
    );

    public static final List<String> INTEGER_KEYS = Arrays.asList(
        "adminBrowserLimit",
        "maxPredicateArity",
        "userBrowserLimit"
    );

    public static final List<String> STRING_KEYS = Arrays.asList(
        "hostname",
        "ollamaHost",
        "port",
        "smtpEmailAddress",
        "smtpEmailUser",
        "smtpEmailPassword",
        "smtpEmailServer"
    );

    /*****************************************************************
     * Returns a new configuration object populated with config.xml data.
     * @param configPath path to config.xml.
     */
    public Configuration(String configPath) {

        this.configFilePath = configPath;
        File configFile = new File(configPath);
        if (!configFile.exists()) {
            File parent = configFile.getParentFile();
            if (parent != null && !parent.exists())
                parent.mkdirs();
            setAllPreferencesAsDefault();
            this.kbConstituentList = new LinkedHashMap<>();
            writeXml();
        }
        else {
            setAllFromXml();
        }
    }

    /*****************************************************************
     * Updates a configuration preference in memory.
     * @param key preference key
     * @param value preference value
     */
    public void setPreference(String key, String value) {

        if (!CONFIG_KEYS.contains(key)) {
            warnings.add("Unknown preference: " + key);
            return;
        }
        preferences.put(key, value);
    }

    /*****************************************************************
     * Writes current preferences and KB constituents back to config.xml.
     */
    public void writeXml() {

        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.newDocument();
            Element root = doc.createElement("configuration");
            doc.appendChild(root);
            for (String key : CONFIG_KEYS) {
                String value = preferences.get(key);
                if (value == null) value = "";
                Element pref = doc.createElement("preference");
                pref.setAttribute("name", key);
                pref.setAttribute("value", value);
                root.appendChild(pref);
            }
            for (Map.Entry<String, List<String>> entry : kbConstituentList.entrySet()) {
                Element kb = doc.createElement("kb");
                kb.setAttribute("name", entry.getKey());
                for (String filename : entry.getValue()) {
                    Element constituent = doc.createElement("constituent");
                    constituent.setAttribute("filename", filename);
                    kb.appendChild(constituent);
                }
                root.appendChild(kb);
            }
            Transformer transformer = TransformerFactory.newInstance().newTransformer();
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
            transformer.transform(new DOMSource(doc), new StreamResult(new File(configFilePath)));
        }
        catch (Exception e) {
            throw new RuntimeException("Could not write config XML file: " + configFilePath, e);
        }
    }

    public int getAdminBrowserLimit() { return getIntegerPreference("adminBrowserLimit", 200); }

    public String getBaseDir() { return getStringPreference("baseDir", ""); }

    public boolean isCache() { return getBooleanPreference("cache", true); }

    public boolean isCacheDisjoint() { return getBooleanPreference("cacheDisjoint", true); }

    public boolean isCwa() { return getBooleanPreference("cwa", false); }

    public String getEproverExec() { return getStringPreference("eproverExec", ""); }

    public String getGraphDir() { return getStringPreference("graphDir", ""); }

    public String getGraphVizDir() { return getStringPreference("graphVizDir", "/usr/bin"); }

    public String getHostname() { return getStringPreference("hostname", "localhost"); }

    public String getInferenceTestDir() { return getStringPreference("inferenceTestDir", getKbDir() + File.separator + "tests"); }

    public boolean isHttps() { return getBooleanPreference("https", false); }

    public String getJeditExec() { return getStringPreference("jeditExec", "/usr/share/jedit/jedit"); }

    public String getKbDir() { return getStringPreference("kbDir", ""); }

    public boolean isLoadFresh() { return getBooleanPreference("loadFresh", true); }

    public boolean isLoadLexicons() { return getBooleanPreference("loadLexicons", true); }

    public String getLeoExec() { return getStringPreference("leoExec", ""); }

    public int getMaxPredicateArity() { return getIntegerPreference("maxPredicateArity", 7); }

    public String getPort() { return getStringPreference("port", "8080"); }

    public boolean isTermFormats() { return getBooleanPreference("termFormats", true); }

    public String getTptpExec() { return getStringPreference("tptpExec", ""); }

    public boolean isTypePrefix() { return getBooleanPreference("typePrefix", true); }

    public int getUserBrowserLimit() { return getIntegerPreference("userBrowserLimit", 25); }

    public String getVampireExec() { return getStringPreference("vampireExec", ""); }

    public String getVerbnetDir() { return getStringPreference("verbnetDir", ""); }

    public String getOllamaHost() { return getStringPreference("ollamaHost", "http://127.0.0.1:11434"); }

    public boolean isShowCachedFormulas() { return getBooleanPreference("showCachedFormulas", true); }

    public String getSmtpEmailAddress() { return getStringPreference("smtpEmailAddress", ""); }

    public String getSmtpEmailUser() { return getStringPreference("smtpEmailUser", ""); }

    public String getSmtpEmailPassword() { return getStringPreference("smtpEmailPassword", ""); }

    public String getSmtpEmailServer() { return getStringPreference("smtpEmailServer", ""); }

    public String getSystemsDir() { return getStringPreference("systemsDir", ""); }

    public boolean isAws() { return getBooleanPreference("isAws", false); }

    /*****************************************************************
     * Sets all preferences and KB constituents from the XML
     */
    private void setAllFromXml() {

        Document doc = readXmlFile(configFilePath);
        HashMap<String, String> defaults = getDefaultPreferences();
        this.preferences = getPreferencesFromXml(doc);
        validateAllPreferencesFromXml(defaults);
        this.kbConstituentList = getKbConstituentListFromXml(doc);
    }

    private HashMap<String, String> getDefaultPreferences() {

        String sep = File.separator;
        String userHome = System.getProperty("user.home");
        String sigmaHome = System.getenv("SIGMA_HOME");
        String tomcatHome = System.getenv("CATALINA_HOME");
        String systemsHome = System.getenv("SYSTEMS_HOME");
        if (StringUtil.emptyString(sigmaHome)) sigmaHome = userHome + sep + ".sigmakee";
        if (StringUtil.emptyString(tomcatHome)) tomcatHome = System.getProperty("user.dir");
        if (StringUtil.emptyString(systemsHome)) systemsHome = System.getProperty("user.dir");
        HashMap<String, String> defaults = new HashMap<>();
        defaults.put("adminBrowserLimit", "200");
        defaults.put("baseDir", sigmaHome);
        defaults.put("cache", "true");
        defaults.put("cacheDisjoint", "true");
        defaults.put("cwa", "false");
        defaults.put("eproverExec", userHome + sep + "Programs" + sep + "E" + sep + "bin" + sep + "e_ltb_runner");
        defaults.put("graphDir", tomcatHome + sep + "webapps" + sep + "sigma" + sep + "graph");
        defaults.put("graphVizDir", "/usr/bin");
        defaults.put("hostname", "localhost");
        defaults.put("https", "false");
        defaults.put("inferenceTestDir", sigmaHome + sep + "KBs" + sep + "tests");
        defaults.put("jeditExec", "/usr/share/jedit/jedit");
        defaults.put("kbDir", sigmaHome + sep + "KBs");
        defaults.put("loadFresh", "true");
        defaults.put("loadLexicons", "true");
        defaults.put("leoExec", userHome + sep + "leo");
        defaults.put("maxPredicateArity", "7");
        defaults.put("port", "8080");
        defaults.put("showCachedFormulas", "true");
        defaults.put("systemsDir", systemsHome);
        defaults.put("termFormats", "true");
        defaults.put("tptpExec", userHome + sep + "workspace" + sep + "TPTP4X" + sep + "tptp4X");
        defaults.put("typePrefix", "true");
        defaults.put("userBrowserLimit", "25");
        defaults.put("vampireExec", userHome + sep + "Programs" + sep + "vampire" + sep + "build" + sep + "vampire");
        defaults.put("verbnetDir", "");
        defaults.put("ollamaHost", "http://127.0.0.1:11434");
        defaults.put("smtpEmailAddress", "");
        defaults.put("smtpEmailUser", "");
        defaults.put("smtpEmailPassword", "");
        defaults.put("smtpEmailServer", "");
        defaults.put("isAws", "false");
        return defaults;
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

        HashMap<String, List<String>> kbMap = new LinkedHashMap<>();
        NodeList kbNodes = doc.getElementsByTagName("kb");
        for (int i = 0; i < kbNodes.getLength(); i++) {
            Element kbElement = (Element) kbNodes.item(i);
            String kbName = kbElement.getAttribute("name");
            List<String> constituents = new ArrayList<>();
            NodeList constituentNodes = kbElement.getElementsByTagName("constituent");
            for (int j = 0; j < constituentNodes.getLength(); j++) {
                Element constituent = (Element) constituentNodes.item(j);
                String filename = constituent.getAttribute("filename");
                if (filename != null && !filename.isEmpty()) {
                    File file = new File(filename);
                    if (!file.isAbsolute()) {
                        String kbDir = preferences.get("kbDir");
                        if (!StringUtil.emptyString(kbDir)) file = new File(kbDir, filename);
                    }
                    if (file.exists() && file.isFile()) constituents.add(filename);
                    else  {
                        errors.add("Missing KB constituent " + filename + " in KB " + kbName);
                        LoggingUtils.log("ERROR", "Skipping missing KB constituent: " + filename + " for KB: " + kbName);
                    }
                }
            }
            if (kbName != null && !kbName.isEmpty()) kbMap.put(kbName, constituents);
        }
        return kbMap;
    }

    /*****************************************************************
     * Sets all preferences to their default values.
     */
    public void setAllPreferencesAsDefault() {

        this.preferences = getDefaultPreferences();
    }

    public String getStringPreference(String key, String defaultValue) {

        String value = this.preferences.get(key);
        if (StringUtil.emptyString(value)) return defaultValue;
        return value;
    }

    public int getIntegerPreference(String key, int defaultValue) {

        String value = this.preferences.get(key);
        if (!StringUtil.isInteger(value)) return defaultValue;
        return Integer.parseInt(value);
    }

    public boolean getBooleanPreference(String key, boolean defaultValue) {

        String value = this.preferences.get(key);
        if (StringUtil.emptyString(value)) return defaultValue;
        value = value.trim().toLowerCase(Locale.ROOT);
        if ("true".equals(value) || "yes".equals(value)) return true;
        if ("false".equals(value) || "no".equals(value)) return false;
        errors.add("Invalid boolean pref (" + key + ", " + value + ")");
        return defaultValue;
    }

    public void validateAllPreferencesFromXml(HashMap<String, String> defaults) {

        validateKnownPreferences();
        validateMissingPreferences(defaults);
        validateDirectoryPreferences(defaults);
        validateExecutablePreferences(defaults);
        validateIntegerPreferences(defaults);
        validateBooleanPreferences(defaults);
        validateStringPreferences(defaults);
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

    private void validateKnownPreferences() {

        for (String key : preferences.keySet()) {
            if (!CONFIG_KEYS.contains(key))
                warnings.add("Unknown preference: " + key);
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
     * Clears all configured KB constituent lists.
     */
    public void clearKbConstituentLists() {

        this.kbConstituentList.clear();
    }

    /*****************************************************************
     * Sets the constituent list for one KB.
     * @param kbName KB name.
     * @param constituents constituent file list.
     */
    public void setKbConstituentList(String kbName, List<String> constituents) {

        if (StringUtil.emptyString(kbName)) return;
        if (constituents == null) constituents = new ArrayList<>();
        this.kbConstituentList.put(kbName, new ArrayList<>(constituents));
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

    private void printPreferences() {

        System.out.println("Preferences");
        for (Map.Entry<String, String> entry : this.preferences.entrySet()) System.out.println("        " + entry.getKey() + ": " + entry.getValue());
    }

    private void printKBs() {

        for (Map.Entry<String, List<String>> entry : this.kbConstituentList.entrySet()) {
            System.out.println("KB: " + entry.getKey());
            for (String constituent : entry.getValue()) System.out.println("   " + constituent);
        }
    }

    public void printConfig() {
        
        System.out.println("===================================================\nPrinting values in " + configFilePath);
        printPreferences();
        printKBs();
        System.out.println("Config Errors:");
        for (String error : this.errors) System.out.println("    " + error);
        System.out.println("Config Warnings:");
        for (String warning : this.warnings) System.out.println("    " + warning);
        System.out.println("===================================================");
    }

    /******************************************************************
     */
    public static void showHelp() {

        System.out.println("Configuration.main() Options:");
        System.out.println("  -h - show this help screen");
        System.out.println("  -p - print config data");
    }

    /******************************************************************
     * Test method for this class.
     */
    public static void main(String args[]) {

        Map<String, List<String>> argMap = CLIMapParser.parse(args);
        if (argMap.isEmpty() || argMap.containsKey("h")) showHelp();
        else if (argMap.containsKey("p")) {
            Configuration config = new Configuration("/home/shaun/.sigmakee/KBs/config.xml");
            config.printConfig();
        }
        
    }
}