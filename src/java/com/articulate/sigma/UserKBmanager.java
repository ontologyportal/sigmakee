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
import com.articulate.sigma.nlg.NLGUtils;
import com.articulate.sigma.VerbNet.VerbNet;
import com.articulate.sigma.wordNet.OMWordnet;
import com.articulate.sigma.wordNet.WordNet;
import com.articulate.sigma.trans.SUMOKBtoTPTPKB;
import com.articulate.sigma.utils.StringUtil;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import java.io.*;
import java.nio.file.*;
import java.util.*;

/**
 * A per-user KB manager that mirrors the behavior of KBmanager, but
 * persists its own config and serialization to:
 *
 *   ~/.sigmakee/userKBs/<username>/<username>_config.xml
 *   ~/.sigmakee/userKBs/<username>/<username>_kbmanager.ser
 *
 * This class is intentionally independent of KBmanager's singleton.
 * It can run concurrently with the global KBmanager.
 */

public class UserKBmanager implements Serializable {

    boolean debug = false;
    private static final String USERS_ROOT_DIR = KButilities.SIGMA_HOME + File.separator + "userKBs";
    private static final String USER_CONFIG_SUFFIX = "_config.xml";
    private static final String USER_SER_SUFFIX    = "_kbmanager.ser";

    private final Map<String,String> preferences = new HashMap<>();
    private final Map<String,KB> kbs = new HashMap<>();

    private transient boolean initialized = false;
    private transient boolean initializing = false;
    private transient String username;
    private transient String userDir;       // ~/.sigmakee/userKBs/<username>
    private transient Path serPath;         // ~/.sigmakee/userKBs/<username>/<username>_kbmanager.ser
    private transient Path configPath;      // ~/.sigmakee/userKBs/<username>/<username>_config.xml

    /** ***************************************************************
     * Initialize (or rehydrate) this per-user KB manager once.
     * Creates user dirs, loads config/serialization, and prepares KBs.
     * @param username login/user id (non-null, non-empty)
     */
    public synchronized void initializeOnceUser(String username) {

        if (initializing || initialized) {
            if(debug) System.out.println("UserKBmanager.initializeOnceUser(): already initialized for " + this.username);
            return;
        }
        this.initializing = true;
        this.username = Objects.requireNonNull(username, "username must not be null").trim();
        if (this.username.isEmpty()) throw new IllegalArgumentException("username must not be empty");
        bootstrapUserLayout();
        boolean loaded = false;
        SimpleElement cfg = readUserConfiguration();
        if (cfg == null) throw new RuntimeException("Failed to read user config: " + configPath);
        if (serializedExists() && !serializedOld(cfg)) {
            loaded = loadSerialized();
        }
        if (!loaded) {
            if(debug) System.out.println("UserKBmanager.initializeOnceUser(): loading from sources for " + username);
            setConfiguration(cfg);
            initLinguistics();
            serialize();
        }
        for (KB kb : kbs.values()) {
            loadKBforInference(kb);
        }
        this.initializing = false;
        this.initialized = true;
        if(debug) System.out.println("UserKBmanager.initializeOnceUser(): complete for " + username);
    }

    /** ***************************************************************
     * Ensure the per-user filesystem layout exists and user config file
     * is present (copy base_config.xml or write a minimal one).
     */
    private void bootstrapUserLayout() {

        this.userDir = USERS_ROOT_DIR + File.separator + username;
        File udir = new File(userDir);
        if (!udir.exists()) {
            boolean ok = udir.mkdirs();
            if(debug) System.out.println("UserKBmanager.bootstrapUserLayout(): mkdirs " + userDir + " -> " + ok);
        }
        this.configPath = Paths.get(userDir, username + USER_CONFIG_SUFFIX);
        this.serPath    = Paths.get(userDir, username + USER_SER_SUFFIX);
        if (!Files.exists(configPath)) {
            Path baseConfig = Paths.get(USERS_ROOT_DIR, "base_config.xml");
            try {
                if (Files.exists(baseConfig)) {
                    Files.copy(baseConfig, configPath);
                    if(debug) System.out.println("UserKBmanager.bootstrapUserLayout(): copied base_config -> " + configPath);
                } else {
                    writeMinimalConfig();
                    if(debug) System.out.println("UserKBmanager.bootstrapUserLayout(): wrote minimal user config -> " + configPath);
                }
            } catch (IOException e) {
                throw new RuntimeException("Failed to create user config for " + username, e);
            }
        }
    }

    /** ***************************************************************
     * Write a minimal per-user configuration XML to the user's config path.
     * Populates kbDir, sumokbname, cache, and TPTP.
     * @throws IOException on filesystem errors
     */
    private void writeMinimalConfig() throws IOException {

        String xml =
            "<configuration>\n" +
            "  <preference name=\"kbDir\" value=\"" + escapeFilename(userDir) + "\"/>\n" +
            "  <preference name=\"sumokbname\" value=\"SUMO\"/>\n" +
            "  <preference name=\"cache\" value=\"yes\"/>\n" +
            "  <preference name=\"TPTP\" value=\"yes\"/>\n" +
            "</configuration>\n";
        Files.writeString(configPath, xml, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
    }

    /** ***************************************************************
     * Check whether the per-user .ser snapshot exists.
     * @return true if the serialized snapshot file exists
     */
    private boolean serializedExists() {

        boolean exists = Files.exists(serPath);
        if(debug) System.out.println("UserKBmanager.serializedExists(" + username + "): " + exists);
        return exists;
    }

    /** ***************************************************************
     * Decide if the serialized snapshot is older than config or any constituent.
     * @param configuration parsed per-user configuration element
     * @return true if snapshot should be considered stale
     */
    private boolean serializedOld(SimpleElement configuration) {

        try {
            File cfg = configPath.toFile();
            if (!serPath.toFile().exists()) return true;
            Date cfgDate = new Date(cfg.lastModified());
            Date serDate = new Date(serPath.toFile().lastModified());
            if(debug) System.out.println("UserKBmanager.serializedOld(" + username + "): ser saved: " + serDate + " cfg: " + cfgDate);
            if (serDate.compareTo(cfgDate) < 0) return true;
            for (String constituent : kbFilenamesFromXML(configuration)) {
                File f = new File(constituent);
                if (f.exists() && new Date(f.lastModified()).after(serDate)) {
                    if(debug) System.out.println("UserKBmanager.serializedOld(" + username + "): newer constituent -> " + f);
                    return true;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            return true;
        }
        return false;
    }
    
    /** ***************************************************************
     * Serialize this UserKBmanager state to the per-user .ser file.
     * Destroys any existing snapshot.
     */
    private void serialize() {
        try (Output out = new Output(Files.newOutputStream(serPath))) {
            KButilities.kryoLocal.get().writeObject(out, this);
            KButilities.kryoLocal.remove();
            if(debug) System.out.println("UserKBmanager.serialize(): wrote " + serPath);
        } catch (IOException e) {
            System.err.println("UserKBmanager.serialize(): failed");
            e.printStackTrace();
        }
    }

    /** ***************************************************************
     * Load (rehydrate) this manager from the per-user .ser snapshot.
     * Reconnects transient fields and initializes linguistics.
     * @return true if successful, false on failure
     */
    @SuppressWarnings("unchecked")
    private boolean loadSerialized() {

        try (Input in = new Input(Files.newInputStream(serPath))) {
            UserKBmanager loaded = KButilities.kryoLocal.get().readObject(in, UserKBmanager.class);
            KButilities.kryoLocal.remove();
            this.preferences.clear();
            this.preferences.putAll(loaded.preferences);
            this.kbs.clear();
            this.kbs.putAll(loaded.kbs);
            this.username = loaded.username != null ? loaded.username : this.username;
            this.userDir  = USERS_ROOT_DIR + File.separator + this.username;
            this.configPath = Paths.get(userDir, username + USER_CONFIG_SUFFIX);
            this.serPath    = Paths.get(userDir, username + USER_SER_SUFFIX);
            initLinguistics();
            if(debug) System.out.println("UserKBmanager.loadSerialized(): rehydrated " + username);
            return true;
        } catch (IOException e) {
            System.err.println("UserKBmanager.loadSerialized(): failed");
            e.printStackTrace();
            return false;
        }
    }

    /** ***************************************************************
     * Save out the current serialized snapshot (.ser) for faster startup.
     */
    public void saveSerialized() {

        serialize();
    }

    /** ***************************************************************
     * Remove a constituent file from a KB, delete the file if owned
     * by the user area, and persist config/serialization.
     * @param kbName knowledge base name
     * @param constituentPath absolute path to the constituent file
     * @return status string describing the outcome
     */
    public String removeConstituent(String kbName, String constituentPath) {

        KB kb = getKB(kbName);
        if (kb == null) {
            return "KB " + kbName + " does not exist.";
        }
        if (kb.constituents == null || !kb.constituents.contains(constituentPath)) {
            return "Constituent " + constituentPath + " not found in KB " + kbName;
        }
        kb.constituents.remove(constituentPath);
        try {
            File f = new File(constituentPath);
            if (f.exists() && f.getCanonicalPath().startsWith(userDir)) {
                boolean deleted = f.delete();
                if(debug) System.out.println("UserKBmanager.removeConstituent(): deleted=" + deleted + " file=" + f);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            writeUserConfiguration();
            serialize();
        } catch (IOException ioe) {
            System.err.println("Error persisting after removeConstituent: " + ioe.getMessage());
        }
        return "Removed constituent " + constituentPath + " from KB " + kbName;
    }

    /** ***************************************************************
     * Read and parse the per-user configuration XML from disk.
     * Also forces kbDir preference to the user's directory.
     * @return SimpleElement root of configuration, or null on error
     */
    private SimpleElement readUserConfiguration() {

        try (Reader r = new BufferedReader(new FileReader(configPath.toFile()))) {
            preferences.put("kbDir", new File(userDir).getCanonicalPath());
            SimpleDOMParser sdp = new SimpleDOMParser();
            SimpleElement cfg = sdp.parse(r);
            if (!"configuration".equals(cfg.getTagName()))
                throw new IllegalStateException("Bad config root tag: " + cfg.getTagName());
            return cfg;
        } catch (Exception e) {
            System.err.println("UserKBmanager.readUserConfiguration(): error");
            e.printStackTrace();
            return null;
        }
    }

    /** ***************************************************************
     * Apply configuration: load preferences, KBs, and derived settings.
     * @param configuration parsed per-user configuration element
     */
    private void setConfiguration(SimpleElement configuration) {

        if(debug) System.out.println("UserKBmanager.setConfiguration(" + username + "):");
        preferencesFromXML(configuration);
        kbsFromXML(configuration);
        initCWA();
        termFormatsToSynsets();
    }

    /** ***************************************************************
     * Build KBs from configuration, load their constituents, and
     * build caches/arity checks as needed.
     * @param configuration parsed per-user configuration element
     */
    private void kbsFromXML(SimpleElement configuration) {

        long t0 = System.currentTimeMillis();
        for (SimpleElement child : configuration.getChildElements()) {
            if ("kb".equals(child.getTagName())) {
                String kbName = child.getAttribute("name");
                addKB(kbName);
                List<String> constituents = new ArrayList<>();
                boolean useCacheFile = "yes".equalsIgnoreCase(getPref("cache"));
                for (SimpleElement c : child.getChildElements()) {
                    if (!"constituent".equals(c.getTagName())) continue;
                    String fn = c.getAttribute("filename");
                    if (!StringUtil.emptyString(fn)) {
                        if (!fn.startsWith(File.separator))
                            fn = getPref("kbDir") + File.separator + fn;
                        if (KButilities.isCacheFile(fn)) {
                            if (useCacheFile) constituents.add(fn);
                        } else {
                            constituents.add(fn);
                        }
                    }
                }
                loadKB(kbName, constituents);
            }
        }
        if(debug) System.out.println("UserKBmanager.kbsFromXML(" + username + "): loaded in " +
                (System.currentTimeMillis() - t0)/KButilities.ONE_K + "s");
    }

    /** ***************************************************************
     * Collect absolute filenames for all KB constituents listed in config.
     * Honors cache-file preference.
     * @param configuration parsed per-user configuration element
     * @return list of absolute constituent paths
     */
    private List<String> kbFilenamesFromXML(SimpleElement configuration) {

        List<String> result = new ArrayList<>();
        boolean useCacheFile = "yes".equalsIgnoreCase(getPref("cache"));
        for (SimpleElement child : configuration.getChildElements()) {
            if ("kb".equals(child.getTagName())) {
                for (SimpleElement c : child.getChildElements()) {
                    if (!"constituent".equals(c.getTagName())) continue;
                    String fn = c.getAttribute("filename");
                    if (!StringUtil.emptyString(fn)) {
                        if (!fn.startsWith(File.separator))
                            fn = getPref("kbDir") + File.separator + fn;
                        if (KButilities.isCacheFile(fn)) {
                            if (useCacheFile) result.add(fn);
                        } else {
                            result.add(fn);
                        }
                    }
                }
            }
        }
        return result;
    }

    /** ***************************************************************
     * Initialize linguistics resources (NLG, WordNet, VerbNet, OMW)
     * according to user preferences.
     */
    private void initLinguistics() {

        try {
            String kbDir = getPref("kbDir");
            NLGUtils.init(kbDir);
            if (!prefEquals("loadLexicons","false")) {
                WordNet.initOnce();
                VerbNet.initOnce();
                VerbNet.processVerbs();
                OMWordnet.readOMWfiles();
            } else {
                WordNet.disable = true;
                VerbNet.disable = true;
                OMWordnet.disable = true;
            }
        } catch (Exception e) {
            System.err.println("UserKBmanager.initLinguistics(): " + e.getMessage());
            e.printStackTrace();
        }
    }

    /** ***************************************************************
     * Initialize Closed World Assumption flag from preferences.
     */
    private void initCWA() {

        String cwa = preferences.get("cwa");
        SUMOKBtoTPTPKB.CWA = !StringUtil.emptyString(cwa) && cwa.equals("true");
    }

    /** ***************************************************************
     * Optionally map termFormat facts to WordNet synsets and serialize
     * WordNet, if enabled by preferences.
     */
    private void termFormatsToSynsets() {

        if ("yes".equals(getPref("termFormats")) && !prefEquals("loadLexicons","false")) {
            for (String kbName : kbs.keySet()) {
                WordNet.wn.termFormatsToSynsets(getKB(kbName));
                WordNet.serialize();
            }
        }
    }

    /** ***************************************************************
     * Create/replace a KB with the given constituents and build caches.
     * @param kbName KB name to (re)load
     * @param constituents list of absolute file paths to add
     * @return true on successful load
     */
    public boolean loadKB(String kbName, List<String> constituents) {

        boolean ok = false;
        try {
            if (existsKB(kbName)) removeKB(kbName);
            addKB(kbName);
            KB kb = getKB(kbName);
            for (String filename : constituents) {
                if(debug) System.out.println("UserKBmanager.loadKB(" + username + "): add " + filename + " to " + kbName);
                kb.addConstituent(filename);
            }
            long t0 = System.currentTimeMillis();
            kb.kbCache = new KBcache(kb);
            kb.kbCache.buildCaches();
            kb.checkArity();
            if(debug) System.out.println("UserKBmanager.loadKB(): seconds: " +
                    (System.currentTimeMillis() - t0)/KButilities.ONE_K);
            ok = true;
        } catch (Exception e) {
            System.err.println("UserKBmanager.loadKB(): " + e.getMessage());
            e.printStackTrace();
        }
        return ok;
    }

    /** ***************************************************************
     * Prepare a KB for theorem proving based on preferences (E/Vampire).
     * @param kb KB instance
     */
    private void loadKBforInference(KB kb) {
        
        if ("yes".equals(getPref("TPTP"))) {
            String prover = getPref("prover"); // optional user pref
            boolean useVampire = !"EPROVER".equalsIgnoreCase(prover);
            if(debug) System.out.println("UserKBmanager.loadKBforInference(" + username + "): " +
                    (useVampire ? "Vampire" : "EProver"));
            if (useVampire) kb.loadVampire(); else kb.loadEProver();
        }
    }

    /** ***************************************************************
     * Add a visible KB with the given name using the user's kbDir.
     * @param name KB name
     */
    public void addKB(String name) { 
    
        addKB(name, true); 
    }

    /** ***************************************************************
     * Add a KB with visibility control using the user's kbDir.
     * @param name KB name
     * @param isVisible whether the KB should appear in listings
     */
    public void addKB(String name, boolean isVisible) {

        KB kb = new KB(name, getPref("kbDir"), isVisible);
        kbs.put(name.intern(), kb);
    }

    /** ***************************************************************
     * Remove a KB, terminating any attached prover process first.
     * @param name KB name
     * @return human-readable status
     */
    public String removeKB(String name) {

        KB kb = kbs.get(name);
        if (kb == null) return "KB " + name + " does not exist and cannot be removed.";
        try {
            if (kb.eprover != null) {
                kb.eprover.terminate();
                kb.eprover = null;
            }
        } catch (IOException ioe) {
            System.err.println("Error in UserKBmanager.removeKB(): " + ioe.getMessage());
        }
        kbs.remove(name);
        return "KB " + name + " successfully removed.";
    }

    /** ***************************************************************
     * Get a KB by name from this user manager.
     * @param name KB name
     * @return KB or null if not found
     */
    public KB getKB(String name) {

        if (!kbs.containsKey(name) && debug)
            System.out.println("WARN in UserKBmanager.getKB(): KB " + name + " not found.");
        return kbs.get(name);
    }

    /** ***************************************************************
     * Test whether a KB exists in this user manager.
     * @param name KB name
     * @return true if present
     */
    public boolean existsKB(String name) {

        return kbs.containsKey(name); 
    }

    /** ***************************************************************
     * Get the set of visible KB names managed by this user manager.
     * @return set of KB names
     */
    public Set<String> getKBnames() {

        Set<String> names = new HashSet<>();
        for (Map.Entry<String,KB> e : kbs.entrySet()) {
            if (e.getValue().isVisible()) names.add(e.getKey());
        }
        return names;
    }

    /** ***************************************************************
     * Get a user preference value, or empty string if unset.
     * @param key preference name
     * @return preference value or ""
     */
    public String getPref(String key) {

        String val = preferences.get(key);
        return val == null ? "" : val;
    }

    /** ***************************************************************
     * Set or override a user preference.
     * @param key preference name
     * @param value preference value
     */
    public boolean prefEquals(String key, String value) {

        String val = preferences.get(key);
        return (val == null ? "" : val).equals(value);
    }

    /** ***************************************************************
     * Load preferences from the configuration element into this manager.
     * Forces kbDir to the user's directory.
     * @param configuration parsed per-user configuration element
     */
    public void setPref(String key, String value) {

        preferences.put(key, value);
    }

    /** ***************************************************************
     * Write the per-user configuration XML (<username>_config.xml)
     * including preferences and KB manifests.
     * @throws IOException on filesystem errors
     */
    private void preferencesFromXML(SimpleElement configuration) {

        for (SimpleElement child : configuration.getChildElements()) {
            if ("preference".equals(child.getTagName())) {
                String name = child.getAttribute("name");
                String value = child.getAttribute("value");
                if (name != null && value != null) {
                    if ("kbDir".equals(name)) {
                        value = pathCanonical(userDir);
                    }
                    preferences.put(name, value);
                }
            }
        }
    }

    /** ***************************************************************
     * Write the per-user configuration XML (<username>_config.xml)
     * including preferences and KB manifests.
     * @throws IOException on filesystem errors
     */
    public void writeUserConfiguration() throws IOException {

        if(debug) System.out.println("INFO in UserKBmanager.writeUserConfiguration()");
        String dir = preferences.get("kbDir");
        File fDir = new File(dir);
        String config_file = username + "_config.xml";
        File file = new File(fDir, config_file);
        String canonicalPath = file.getCanonicalPath();
        SimpleElement configXML = new SimpleElement("configuration");
        for (Map.Entry<String, String> element : preferences.entrySet()) {
            String key = element.getKey();
            String value = element.getValue();
            if (KBmanager.FILE_KEYS.contains(key)) {
                value = KBmanager.escapeFilename(value);
            }
            if (!Arrays.asList("userName", "userRole").contains(key)) {
                SimpleElement preference = new SimpleElement("preference");
                preference.setAttribute("name", key);
                preference.setAttribute("value", value);
                configXML.addChildElement(preference);
            }
        }
        for (KB kb : kbs.values()) {
            SimpleElement kbXML = kb.writeConfiguration();
            configXML.addChildElement(kbXML);
        }
        try (FileWriter fw = new FileWriter(file);
            PrintWriter pw = new PrintWriter(fw)) {
            pw.println(configXML.toFileString());
        }
    }

    /** ***************************************************************
     * Canonicalize a filesystem path; returns input on error.
     * @param p raw path
     * @return canonical (absolute, normalized) path
     */
    private static String pathCanonical(String p) {

        try { return new File(p).getCanonicalPath(); }
        catch (IOException e) { return p; }
    }

    /** ***************************************************************
     * Escape backslashes for XML persistence (mirrors KBmanager logic).
     * @param fname raw filename
     * @return escaped filename
     */
    public static String escapeFilename(String fname) {
        
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < fname.length(); i++) {
            char c = fname.charAt(i);
            if (c == '\\') {
                sb.append("\\\\");
                if (i+1 < fname.length() && fname.charAt(i+1) == '\\') i++;
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }
}
