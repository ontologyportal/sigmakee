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

import com.articulate.sigma.VerbNet.VerbNet;
import com.articulate.sigma.nlg.NLGUtils;
import com.articulate.sigma.trans.SUMOKBtoTPTPKB;
import com.articulate.sigma.trans.TPTPGenerationManager;
import com.articulate.sigma.parsing.Configuration;
import com.articulate.sigma.utils.StringUtil;
import com.articulate.sigma.wordNet.OMWordnet;
import com.articulate.sigma.wordNet.WordNet;
import com.articulate.sigma.utils.*;

import com.esotericsoftware.kryo.io.*;

import py4j.GatewayServer;

import java.io.*;
import java.util.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.io.IOException;
import java.util.stream.Stream;

/** This is a class that manages a group of knowledge bases.  It should only
 *  have one instance, contained in its own static member variable.
 */
public class KBmanager implements Serializable {

    public static Configuration configuration = new Configuration(KButilities.SIGMA_HOME + "/KBs/config.xml");

    private static final java.util.concurrent.locks.ReentrantLock SER_LOCK = new java.util.concurrent.locks.ReentrantLock();

    protected static final String CONFIG_FILE = "config.xml";
    protected static final String KB_MANAGER_SER = "kbmanager.ser";

    private static KBmanager manager = new KBmanager();

    public static boolean initialized = false;
    public static boolean initializing = false;
    public static boolean debug = false;

    public enum Prover { NONE, EPROVER, VAMPIRE, LEO };
    public Prover prover = Prover.VAMPIRE;
    public Map<String,KB> kbs = new HashMap<>();

    private String error = "";

    /** Build version loaded from version.properties packaged in the WAR/JAR. */
    public static final String BUILD_VERSION = loadBuildVersion();

    /** Raw commit SHA loaded from version.properties (e.g. "ab25e6c"), or "dev". */
    public static final String BUILD_COMMIT = loadBuildProperty("build.commit", "dev");

    /** Build date loaded from version.properties. */
    public static final String BUILD_DATE = loadBuildProperty("build.date", "");

    private static String loadBuildProperty(String key, String fallback) {
        try (InputStream is = KBmanager.class.getResourceAsStream("/version.properties")) {
            if (is == null) return fallback;
            Properties p = new Properties();
            p.load(is);
            return p.getProperty(key, fallback);
        }
        catch (Exception e) {
            return fallback;
        }
    }

    public String getDefaultKbName() {

        if (kbs != null && kbs.containsKey("SUMO")) return "SUMO";
        if (kbs != null && !kbs.isEmpty()) return kbs.keySet().iterator().next();
        return "";
    }

    private static String loadBuildVersion() {
        return loadBuildProperty("build.version", "dev");
    }

    public static String getBuildVersion() {
        return BUILD_VERSION;
    }

    public static String getBuildCommit() {
        return BUILD_COMMIT;
    }

    public static String getBuildDate() {
        return BUILD_DATE;
    }

    /*****************************************************************
     * Set an error string for file loading.
     * @param er the error String
     */
    public void setError(String er) {
        error = er;
    }

    /*****************************************************************
     * Get the error string for file loading.
     * @return The error String
     */
    public String getError() {
        return error;
    }

    /*****************************************************************
     * Check whether the serialized KB exists.
     * @return true if the serialized KB exists
     */
    public static boolean serializedExists() {

        String kbDir = KButilities.SIGMA_HOME + File.separator + "KBs";
        File serfile = new File(kbDir + File.separator + "kbmanager.ser");
        return serfile.exists();
    }

    /*****************************************************************
     * Check whether KB constituents/config/source code are newer than serialized version.
     * @return true if serialized cache is missing or older than constituents/config/source.
     */
    public static boolean isSerializedOld() {

        File serfile = new File(KBmanager.configuration.getKbDir(), KB_MANAGER_SER);
        if (!serfile.exists() || serfile.length() == 0) return true;
        Date kbserDate = new Date(serfile.lastModified());
        Date newestKbSourceDate = newestConfigOrConstituentDate();
        Date newestCodeDate = newestSigmakeeCodeDate();
        Date newestSourceDate = newestKbSourceDate.after(newestCodeDate)
                ? newestKbSourceDate
                : newestCodeDate;
        return kbserDate.compareTo(newestSourceDate) < 0;
    }

    /*****************************************************************
     * Finds the newest SIGMAKEE source/deployed-code modification date.
     * @return newest source or deployed code date, or epoch if unavailable
     */
    public static Date newestSigmakeeCodeDate() {

        long newest = 0L;
        newest = Math.max(newest, newestPathModifiedTime(sourcePathFromEnv()));
        return new Date(newest);
    }

    /*****************************************************************
     * Finds the newest relevant file modification time under a path.
     * @param root root file or directory
     * @return newest modification time in milliseconds, or 0 if unavailable
     */
    private static long newestPathModifiedTime(Path root) {

        if (root == null || !Files.exists(root)) return 0L;
        try {
            if (Files.isRegularFile(root)) return Files.getLastModifiedTime(root).toMillis();
            try (Stream<Path> paths = Files.walk(root)) {
                return paths
                        .filter(Files::isRegularFile)
                        .filter(KBmanager::isCodeOrWebFile)
                        .mapToLong(KBmanager::lastModifiedMillis)
                        .max()
                        .orElse(0L);
            }
        }
        catch (Exception e) {
            System.err.println("WARN KBmanager.newestPathModifiedTime(): " + e.getMessage());
            return 0L;
        }
    }

    /*****************************************************************
     * Checks whether a file should be considered source/deployed code.
     * @param path file path
     * @return true if file is code relevant to cache compatibility
     */
    private static boolean isCodeOrWebFile(Path path) {

        String name = path.getFileName().toString().toLowerCase();
        return name.endsWith(".java") ||
                name.endsWith(".class") ||
                name.endsWith(".jar") ||
                name.endsWith(".jsp") ||
                name.endsWith(".jspf") ||
                name.endsWith(".xml") ||
                name.endsWith(".properties");
    }

    /*****************************************************************
     * Gets last modified millis for a path.
     * @param path file path
     * @return last modified time, or 0 on error
     */
    private static long lastModifiedMillis(Path path) {

        try {
            return Files.getLastModifiedTime(path).toMillis();
        }
        catch (Exception e) {
            return 0L;
        }
    }

    /*****************************************************************
     * Gets SIGMAKEE source path from ONTOLOGYPORTAL_GIT.
     * @return sigmakee source path, or null if unavailable
     */
    private static Path sourcePathFromEnv() {

        String gitRoot = System.getenv("ONTOLOGYPORTAL_GIT");
        if (StringUtil.emptyString(gitRoot)) return null;
        return Paths.get(gitRoot, "sigmakee");
    }

    /*****************************************************************
     * Return the newest modified date among config.xml and constituent files.
     * @return newest modified date among config.xml and constituents.
     */
    public static Date newestConfigOrConstituentDate() {

        return newestConfigOrConstituentDate(false);
    }

    /*****************************************************************
     * Return the newest modified date among config.xml and constituent files.
     * @param ignoreUserAssertions whether to skip dynamic user/session assertion files.
     * @return newest modified date among config.xml and constituents.
     */
    private static Date newestConfigOrConstituentDate(boolean ignoreUserAssertions) {

        File configFile = new File(KBmanager.configuration.getConfigFilePath());
        long newest = configFile.lastModified();
        String newestPath = configFile.getAbsolutePath();
        for (String constituent : configuredConstituentPaths()) {
            if (ignoreUserAssertions && isUserAssertionOrTempInferenceFile(constituent)) {
                System.out.println("Skipping dynamic inference file: " + constituent);
                continue;
            }
            File constituentFile = new File(constituent);
            long modified = constituentFile.lastModified();
            if (modified > newest) {
                newest = modified;
                newestPath = constituentFile.getAbsolutePath();
            }
        }
        if (ignoreUserAssertions) {
            System.out.println("Newest base source for TPTP freshness: " + newestPath);
            System.out.println("Newest base source modified: " + new Date(newest));
        }
        return new Date(newest);
    }

    /*****************************************************************
     * Returns all configured KB constituent paths resolved against kbDir.
     * @return resolved constituent paths.
     */
    private static List<String> configuredConstituentPaths() {

        List<String> result = new ArrayList<>();
        Map<String, List<String>> allConstituents =
                KBmanager.configuration.getAllKbConstituentLists();
        if (allConstituents == null)
            return result;
        for (List<String> kbFiles : allConstituents.values()) {
            for (String filename : kbFiles) {
                result.add(resolveKbConstituentPath(filename));
            }
        }
        return result;
    }

    /*****************************************************************
     * Resolves a KB constituent path against kbDir if relative.
     * @param filename configured constituent filename.
     * @return resolved constituent path.
     */
    private static String resolveKbConstituentPath(String filename) {

        File file = new File(filename);
        if (!file.isAbsolute())
            file = new File(KBmanager.configuration.getKbDir(), filename);
        return file.getPath();
    }

    /*****************************************************************
     * Return the newest modified date among config.xml and constituent files.
     * @param configDirPath directory containing config.xml.
     * @return newest modified date among config.xml and constituents.
     */
    private static Date newestConfigOrConstituentDate(String configDirPath) {

        KBmanager.getMgr().readConfiguration(configDirPath);
        return newestConfigOrConstituentDate();
    }

    public static Date newestBaseConfigOrConstituentDateIgnoringUserAssertions() {

        return newestConfigOrConstituentDate(true);
    }

    private static boolean isUserAssertionOrTempInferenceFile(String path) {

        if (StringUtil.emptyString(path)) {
            return false;
        }
        String name = new File(path).getName();
        return name.endsWith(KB._userAssertionsString) ||
            name.endsWith(KB._userAssertionsTPTP) ||
            name.endsWith(KB._userAssertionsTFF) ||
            name.endsWith(KB._userAssertionsTHF) ||
            name.startsWith("temp-stmt.") ||
            name.startsWith("temp-comb.");
    }

    /*****************************************************************
     * Check whether config.xml or any base constituent is newer than the
     * corresponding TPTP/TFF/THF file, ignoring dynamic user assertions.
     * @param lang inference file extension, such as tptp, tff, or thf
     * @return true if the base inference file is missing or stale
     */
    public boolean infBaseFileOldIgnoringUserAssertions(String lang) {

        String kbDir = configuration.getKbDir();
        for (String kbname : kbs.keySet()) {
            File base = new File(kbDir + File.separator + kbname + "." + lang);
            if (!base.exists()) return true;
            long baseTimeStamp = base.lastModified();
            Date newestSourceDate = newestBaseConfigOrConstituentDateIgnoringUserAssertions();
            if (baseTimeStamp < newestSourceDate.getTime()) return true;
        }
        return false;
    }

    /*****************************************************************
     * Load the most recently saved serialized KBmanager.
     * @return true if serialized was loaded successfully.
     */
    public static boolean loadSerialized() {

        manager = null;
        try {
            manager = decoder();
            if (manager == null) return false;
            initialized = true;
        }
        catch (Exception ex) {
            LoggingUtils.log("ERROR", "");
            ex.printStackTrace();
            return false;
        }
        return true;
    }

    /*****************************************************************
     * Serializes the object to .sigmakee/KBs/
     * @param object object to be serialized.
     */
    public static void encoder(Object object) {

        String kbDir = KButilities.SIGMA_HOME + File.separator + "KBs";
        Path path = Paths.get(kbDir, KB_MANAGER_SER);
        try (Output output = new Output(Files.newOutputStream(path))) {
            KButilities.kryoLocal.get().writeObject(output, object);
            KButilities.kryoLocal.remove();
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }

    /*****************************************************************
     * Load and return the serialized KBmanager from kbmanager.ser.
     * @return the kbmanager
     */
    public static <T> T decoder() {

        KBmanager ob = null;
        String kbDir = KButilities.SIGMA_HOME + File.separator + "KBs";
        Path path = Paths.get(kbDir, KB_MANAGER_SER);
        try (Input input = new Input(Files.newInputStream(path))) {
            ob = KButilities.kryoLocal.get().readObject(input,KBmanager.class);
            KButilities.kryoLocal.remove();
        }
        catch (IOException e) {
            e.printStackTrace();
        }
        return (T) ob;
    }

    /*****************************************************************
     * Save the serialized KBmanager to kbmanager.ser.
     */
    public static void serialize() {

        SER_LOCK.lock();
        try {
            encoder(manager);
        }
        catch (Exception ex) {
            LoggingUtils.log("ERROR", "IOException is caught");
            ex.printStackTrace();
        } finally {
            SER_LOCK.unlock();
        }
    }

    /*****************************************************************
     * Loads all KBs from the typed Configuration object.
     */
    private void loadKBsFromConfiguration() {

        Map<String, List<String>> allKbConstituents =
                KBmanager.configuration.getAllKbConstituentLists();
        if (allKbConstituents == null || allKbConstituents.isEmpty()) {
            throw new IllegalStateException("No KBs found in configuration: " + KBmanager.configuration.getConfigFilePath());
        }
        for (Map.Entry<String, List<String>> entry : allKbConstituents.entrySet()) {
            String kbName = entry.getKey();
            if (StringUtil.emptyString(kbName)) {
                LoggingUtils.log("ERROR", "Skipping KB with empty name.");
                continue;
            }
            List<String> constituentsToAdd = new ArrayList<>();
            for (String filename : entry.getValue()) {
                if (!StringUtil.emptyString(filename))
                    constituentsToAdd.add(resolveKbConstituentPath(filename));
            }
            boolean loaded = loadKB(kbName, constituentsToAdd);
            if (!loaded) throw new IllegalStateException("Failed to load KB: " + kbName);
        }
    }

    /*****************************************************************
     * Validate a list of consituent files by checking if the file exists or not.
     * @constituentList list of constituent file paths to be validated
     * @return validated list of constituent paths
     */
    public List<String> validateConstituentList(List<String> constituentList) {

        List<String> validConstituents = new ArrayList<>();
        for (String constituent : constituentList) {
            if (Files.exists(Paths.get(constituent))) validConstituents.add(constituent);
            else setError("ERROR: File " + constituent + " does not exist");
        }
        return validConstituents;
    }

    /*****************************************************************
     * Loads the constituents of the KB from ~/.sigmakee/config.xml
     *
     * @param kbName the name of the KB
     * @param constituents a list of constituents to load
     * @return true if the loading was successful
     */
    public boolean loadKB(String kbName, List<String> constituents) {

        constituents = validateConstituentList(constituents);
        boolean retVal = false;
        try {
            if (existsKB(kbName)) removeKB(kbName);
            addKB(kbName);
            KB kb = getKB(kbName);
            if (!constituents.isEmpty()) retVal = _loadKB(kbName, constituents, kb);
            long millis = System.currentTimeMillis();
            kb.kbCache = new KBcache(kb);
            kb.kbCache.buildCaches();
            kb.checkArity();
            if (debug) LoggingUtils.log("seconds: " + (System.currentTimeMillis() - millis) / KButilities.ONE_K);
        }
        catch (Exception e) {
            LoggingUtils.log("ERROR", "Unable to save configuration: " + e.getMessage());
            e.printStackTrace(System.err);
            retVal = false;
        }
        return retVal;
    }

    /*****************************************************************
     * Conventional/sequential version
     */
    private boolean _loadKB(String kbName, List<String> constituents, KB kb) {

        int i = 1;
        for (String filename : constituents) {
            LoggingUtils.printProgressBar("INFO", kbName + " Adding Constituents:", i, constituents.size(), "(" + i + "/"  + constituents.size() + ")");
            i++;
            try {
                kb.addConstituent(filename);
            } catch (Exception e1) {
                LoggingUtils.log("ERROR", e1.getMessage());
                e1.printStackTrace();
                return false;
            }
        }
        return true;
    }

    /*****************************************************************
     * Threaded version.
     * Turns out not to be much help timewise and even causes an out of order
     * situation with many constituents being loaded (tdn) 4/22/25
     * @deprecated
     */
    @Deprecated
    private boolean _t_loadKB(String kbName, List<String> constituents, KB kb) {

        Future<Boolean> future;
        List<Future<Boolean>> futures = new ArrayList<>();
        boolean retVal = false;
        for (String filename : constituents) {
            Callable<Boolean> r = () -> {
                try {
                    if (debug) LoggingUtils.log("add constituent " + filename + " to " + kbName);
                    kb.addConstituent(filename);
                }
                catch (Exception e1) {
                    LoggingUtils.log("ERROR", e1.getMessage());
                    e1.printStackTrace();
                    return false;
                }
                return true;
            };
            future = KButilities.EXECUTOR_SERVICE.submit(r);
            futures.add(future);
        }

        for (Future<Boolean> f : futures)
            try {
                retVal = f.get(); // waits for task completion
                if (!retVal)
                    break;
            } catch (InterruptedException | ExecutionException ex) {
                LoggingUtils.log("ERROR", ex.toString());
                ex.printStackTrace();
                retVal = false;
                break;
            }
        return retVal;
     }

    /*****************************************************************
     * Copies an XML configuration file to the File out location.
     * @param in the file to copy
     * @param out the location to copy the in param to
     */
    public static void copyFile(File in, File out) {

        try (InputStream fis = new FileInputStream(in);
            OutputStream fos = new FileOutputStream(out)
        ){
            byte[] buf = new byte[1024];
            int i;
            while ((i = fis.read(buf)) != -1)
                fos.write(buf, 0, i);
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    /*****************************************************************
     * Loads the typed configuration from config.xml, creating parent dirs if needed.
     * @param configPathOrDir directory containing config.xml, or a config XML file path.
     */
    private void readConfiguration(String configPathOrDir) {

        try {
            File configPath;
            if (StringUtil.emptyString(configPathOrDir)) configPath = new File(KButilities.SIGMA_HOME + File.separator + "KBs");
            else configPath = new File(configPathOrDir);
            File configFile;
            if (configPath.isFile() || configPath.getName().toLowerCase(Locale.ROOT).endsWith(".xml")) configFile = configPath;
            else configFile = new File(configPath, CONFIG_FILE);
            File parent = configFile.getParentFile();
            if (parent != null && !parent.exists()) parent.mkdirs();
            KBmanager.configuration = new Configuration(configFile.getCanonicalPath());
        }
        catch (IOException ex) {
            LoggingUtils.log("ERROR", configPathOrDir + "):\n" + "  Exception reading configuration file \n" + ex.getMessage());
            ex.printStackTrace();
        }
    }

    /*****************************************************************
     * Reads in the KBs and other parameters defined in the XML
     * configuration file, or uses the default parameters. Entry point
     * for the web app (Prelude.jsp).
     */
    public void initializeOnce() {

        if (!initialized && !KButilities.insideWebContext)
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                KButilities.getInstance().contextDestroyed(null);
            }));
        String base = KButilities.SIGMA_HOME;
        initializeOnce(base + File.separator + "KBs");
    }

    /*****************************************************************
     * Reads in the KBs and other parameters defined in config.xml.
     * @param configFileDir the directory of config.xml, typically ~/.sigmakee/KBs.
     */
    public void initializeOnce(String configFileDir) {

        long start = System.nanoTime();
        LoggingUtils.printSigmaWelcome();
        LoggingUtils.log("Initializing KBmanager!");

        boolean loaded = false;
        if (initializing || initialized) return;

        initializing = true;

        try {
            readConfiguration(configFileDir);

            LoggingUtils.log("Loading English Lexicons.");
            initializeLexicons(KBmanager.configuration.getKbDir());

            if (!KBmanager.configuration.isLoadFresh() &&
                    serializedExists() &&
                    !isSerializedOld()) {

                LoggingUtils.log("Loading from serialized cache.");
                loaded = loadSerialized();

                if (loaded) {
                    LoggingUtils.log("Building SUMO Term Taxonomy.");
                    for (KB kb : manager.kbs.values()) {
                        final KB kbFinal = kb;
                        KButilities.EXECUTOR_SERVICE.submit(() -> {
                            try {
                                kbFinal.kbCache.buildSymbolTaxonomy();
                            }
                            catch (Exception e) {
                                LoggingUtils.log("ERROR",
                                        "buildSymbolTaxonomy failed: " + e.getMessage());
                            }
                        });
                    }
                }
            }

            if (!loaded) {
                LoggingUtils.log("Regenerating Fresh Cache.");
                manager = this;
                loadKBsFromConfiguration();
                serialize();
            }

            initializing = false;
            initialized = true;

            if (manager.kbs == null || manager.kbs.isEmpty()) {
                throw new IllegalStateException("KBmanager initialized with no KBs from " + KBmanager.configuration.getConfigFilePath());
            }
            if (KBmanager.configuration.isLoadLexicons() && WordNet.wn == null) {
                throw new IllegalStateException("loadLexicons is true but WordNet.wn was not initialized.");
            }
            LoggingUtils.log("Starting TPTP Background Generation.");
            TPTPGenerationManager.startBackgroundGeneration();

            if ("true".equalsIgnoreCase(System.getenv("TPTP_BG_WAIT"))) {
                try {
                    Thread.sleep(120000);
                }
                catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
        catch (Exception ex) {
            initializing = false;
            LoggingUtils.log("ERROR", ex.getMessage());
            ex.printStackTrace();
            return;
        }

        cleanupOrphanedSessionDirectories();

        double elapsedSeconds = (System.nanoTime() - start) / 1_000_000_000.0;
        LoggingUtils.log("Initialization completed in " + elapsedSeconds + " seconds!");
    }

    /*****************************************************************
     * Check whether config.xml/Constituents are newer than its TPTP/TFF/THF file
     * @return true if tptp file is older and config/constituents
     */
    public boolean infFileOld() {

        String lang = "tff";
        if (SUMOKBtoTPTPKB.getLang().equals("fof")) lang = "tptp";
        return infFileOld(lang);
    }

    /*****************************************************************
     * Check whether config file or any .kif constituent is newer than its
     * corresponding TPTP/TFF/THF file.
     * @param lang inference file extension, such as tptp, tff, or thf
     * @return true if the inference file is older than config/constituents
     */
    public boolean infFileOld(String lang) {

        String kbDir = KButilities.SIGMA_HOME + File.separator + "KBs";
        for (String kbname : kbs.keySet()) {
            KB kb = getKB(kbname);
            File infFile = new File(kbDir + File.separator + kbname + "." + lang);
            Date infFileDate = new Date(infFile.lastModified());
            Date newestSourceDate = newestConfigOrConstituentDate();
            if (infFileDate.compareTo(newestSourceDate) < 0) return true;
        }
        return false;
    }

    /*****************************************************************
     * Initialize WordNet, NLGUtils, OMWordnet, VerbNet.
     * @param configFileDir configuration directory
     */
    public void initializeLexicons(String configFileDir) {

        if (!KBmanager.configuration.isLoadLexicons()) {
            WordNet.disable = true;
            VerbNet.disable = true;
            OMWordnet.disable = true;
            return;
        }
        WordNet.disable = false;
        OMWordnet.disable = false;
        WordNet.initOnce();
        if (WordNet.wn == null) {
            throw new IllegalStateException(
                    "WordNet.initOnce() returned without initializing WordNet.wn. " +
                    "Check WordNet.disable, kbDir, and WordNet data files under " +
                    KBmanager.configuration.getKbDir());
        }
        NLGUtils.init(configFileDir);
        OMWordnet.readOMWfiles();
        if (!StringUtil.emptyString(KBmanager.configuration.getVerbnetDir())) {
            VerbNet.disable = false;
            VerbNet.initOnce();
            VerbNet.processVerbs();
        }
        else {
            VerbNet.disable = true;
        }
    }

    /*****************************************************************
     * Double the backslash in a filename so that it can be saved to a text file and read back.
     * @param fname filename to escape
     * @return escaped filename
     */
    public static String escapeFilename(String fname) {

        StringBuilder newstring = new StringBuilder("");
        for (int i = 0; i < fname.length(); i++) {
            if (fname.charAt(i) == 92 && fname.charAt(i+1) != 92) newstring = newstring.append("\\\\");
            if (fname.charAt(i) == 92 && fname.charAt(i+1) == 92) {
                newstring = newstring.append("\\\\");
                i++;
            }
            if (fname.charAt(i) != 92) newstring = newstring.append(fname.charAt(i));
        }
        return newstring.toString();
    }

    /*****************************************************************
     * Create a new empty KB with a name.
     * @param name - the name of the KB
     */
    public void addKB(String name) {
        addKB(name, true);
    }

    public void addKB(String name, boolean isVisible) {

        KB kb = new KB(name, KBmanager.configuration.getKbDir(), isVisible);
        kbs.put(name.intern(), kb);
    }

    /*****************************************************************
     * Remove a knowledge base.
     * @param name - the name of the KB
     * @return indication of KB removal
     */
    public String removeKB(String name) {

        KB kb = kbs.get(name);
        if (kb == null) return "KB " + name + " does not exist and cannot be removed.";
        kb = kbs.remove(name);
        return "KB " + kb.name + " successfully removed.";
    }

    /*****************************************************************
     * Write the current configuration of the system.
     */
    public void writeConfiguration() throws IOException {

        try {
            KBmanager.configuration.clearKbConstituentLists();
            for (KB kb : kbs.values()) {
                SimpleElement kbXML = kb.writeConfiguration();
                String kbName = kbXML.getAttribute("name");
                List<String> constituents = new ArrayList<>();
                for (SimpleElement child : kbXML.getChildElements()) {
                    if ("constituent".equals(child.getTagName())) {
                        String filename = child.getAttribute("filename");
                        if (!StringUtil.emptyString(filename))
                            constituents.add(filename);
                    }
                }
                KBmanager.configuration.setKbConstituentList(kbName, constituents);
            }
            KBmanager.configuration.writeXml();
        }
        catch (RuntimeException e) {
            LoggingUtils.log("ERROR", "Error writing configuration: " + e.getMessage());
            throw new IOException("Error writing configuration", e);
        }
    }

    /*****************************************************************
     * Gets the active configuration for this KB manager.
     * @return active configuration object.
     */
    public Configuration getConfiguration() {

        return configuration;
    }

    /*****************************************************************
     * Sets the active configuration for this KB manager.
     * @param configuration active configuration object.
     */
    public void setConfiguration(Configuration configuration) {

        if (configuration == null) throw new IllegalArgumentException("configuration cannot be null");
        KBmanager.configuration = configuration;
    }
    /*****************************************************************
     * Get the KB that has the given name.
     */
    public KB getKB(String name) {

        return kbs.get(name);
    }

    /*****************************************************************
     * Returns true if a KB with the given name exists.
     */
    public boolean existsKB(String name) {

        return kbs.containsKey(name);
    }

    /*****************************************************************
     * Remove the KB that has the given name.
     */
    public void remove(String name) {

        kbs.remove(name);
    }

    /*****************************************************************
     * Get the one instance of KBmanager from its class variable.
     */
    public static KBmanager getMgr() {

        return manager;
    }

    /*****************************************************************
     * Get the Set of KB names in this manager.
     */
    public Set<String> getKBnames() {

        Set<String> names = new HashSet<>();
        KB kb;
        for (String kbName : kbs.keySet()) {
            kb = getKB(kbName);
            if (kb.isVisible()) names.add(kbName);
        }
        return names;
    }

    /*****************************************************************
     * Get the the complete list of languages available in all KBs
     */
    public List<String> allAvailableLanguages() {

        List<String> result = new ArrayList<>();
        Iterator<String> it = kbs.keySet().iterator();
        String kbName;
        KB kb;
        while (it.hasNext()) {
            kbName = (String) it.next();
            kb = getKB(kbName);
            result.addAll(kb.availableLanguages());
        }
        return result;
    }

    /*****************************************************************
     * Clean up session directories older than the HTTP session timeout.
     * Called at startup to remove orphaned directories from crashes/kills.
     */
    private static void cleanupOrphanedSessionDirectories() {

        String kbDir = KBmanager.configuration.getKbDir();
        Path sessionsDir = Paths.get(kbDir, "sessions");
        if (!Files.exists(sessionsDir)) {
            return;
        }
        long cutoffTime = System.currentTimeMillis() - (60 * 60 * 1000);
        try {
            java.util.concurrent.atomic.AtomicInteger removed = new java.util.concurrent.atomic.AtomicInteger(0);
            Files.list(sessionsDir).forEach(sessionDir -> {
                if (!Files.isDirectory(sessionDir)) return;
                try {
                    long dirTime = Files.getLastModifiedTime(sessionDir).toMillis();
                    if (dirTime < cutoffTime) {
                        String sessionId = sessionDir.getFileName().toString();
                        LoggingUtils.log("Removing orphaned session directory: " + sessionId);
                        com.articulate.sigma.trans.SessionTPTPManager.cleanupSession(sessionId);
                        removed.incrementAndGet();
                    }
                } catch (IOException e) {
                    LoggingUtils.log("ERROR", "Checking session directory: " + e.getMessage());
                }
            });
            if (removed.get() > 0) LoggingUtils.log("INFO", "Removed " + removed.get() + " orphaned session directories!");
            else LoggingUtils.log("No orphaned session directories found");
        } catch (IOException e) {
            LoggingUtils.log("Error during orphaned session cleanup: " + e.getMessage());
        }
    }

    /*****************************************************************
     * Create an server-based interface for Python to call the KB object.
     * https://pypi.python.org
     *
     * from py4j.java_gateway import JavaGateway
     * gateway = JavaGateway()             # connect to the JVM
     * sigma_app = gateway.entry_point     # get the KB instance
     * print(sigma_app.getTerms())         # call a method
     */
    public static void pythonServer() {

        LoggingUtils.log("begin initialization");
        try {
            KBmanager.getMgr().initializeOnce();
        }
        catch (Exception e ) {
            LoggingUtils.log("ERROR", e.getMessage());
        }
        GatewayServer server = new GatewayServer(new PythonAPI());
        server.start();
        LoggingUtils.log("completed initialization, server running");
    }

    /*****************************************************************
     */
    public static void printHelp() {

        System.out.println("KBmanager class");
        System.out.println("  options:");
        System.out.println("  -h - show this help screen");
        System.out.println("  -p - demo Python interface");
        System.out.println("  with no arguments show this help screen and execute a test");
    }

    /*****************************************************************
     */
    public static void main(String[] args) {

        if (args == null || args.length == 0) {
            printHelp();
            try {
                KBmanager.getMgr().initializeOnce();
            }
            catch (Exception e) {
                LoggingUtils.log("ERROR", e.getMessage());
            }
            KB kb = KBmanager.getMgr().getKB(KBmanager.getMgr().getDefaultKbName());
            Formula f = new Formula();
            f.read("(=> (and (wears ?A ?C) (part ?P ?C)) (wears ?A ?P))");

        }
        else {
            if (args.length > 0 && args[0].equals("-p")) {
                pythonServer();
            }
            if (args.length > 0 && args[0].equals("-h")) {
                printHelp();
            }
        }
    }
}