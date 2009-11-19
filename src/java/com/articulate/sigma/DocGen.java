/** This code is copyrighted by Articulate Software (c) 2007.  It is
released under the GNU Public License &lt;http://www.gnu.org/copyleft/gpl.html&gt;.  
Users of this code also consent, by use of this code, to credit Articulate
Software in any writings, briefings, publications, presentations, or other
representations of any software which incorporates, builds on, or uses this
code.  Please cite the following article in any publication with
references:

Pease, A., (2003). The Sigma Ontology Development Environment, 
in Working Notes of the IJCAI-2003 Workshop on Ontology and Distributed Systems,
August 9, Acapulco, Mexico.  See also http://sigmakee.sourceforge.net
*/

/*************************************************************************************************/
package com.articulate.sigma;

import java.util.*;
import java.text.*;
import java.io.*;
import javax.xml.*;
import org.w3c.dom.*;

/** A class to generate simplified HTML-based documentation for SUO-KIF terms. */
public class DocGen {

    // String tokens used as formatting directives.
    private static final String F_DD  = "dd";
    private static final String F_DD2 = "dd2";
    private static final String F_XSD = "xsd";
    private static final String F_TAB = "tab";

    private static final String DEFAULT_KEY = "docgen_default";

    private static Hashtable DOC_GEN_INSTANCES = new Hashtable();

    public static DocGen getInstance() {
        DocGen inst = null;
        try {
            inst = (DocGen) DOC_GEN_INSTANCES.get(DEFAULT_KEY);
            if (inst == null) {
                inst = new DocGen();
                inst.setLineSeparator(System.getProperty("line.separator"));
                DOC_GEN_INSTANCES.put(DEFAULT_KEY, inst);
            }
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
        return inst;
    }

    public static DocGen getInstance(String compositeKey) {

        // System.out.println("ENTER DocGen.getInstance(" + compositeKey + ")");

        DocGen inst = null;
        try {
            KBmanager mgr = KBmanager.getMgr();
            String interned = compositeKey.intern();
            inst = (DocGen) DOC_GEN_INSTANCES.get(interned);
            if (inst == null) {
                inst = new DocGen();
                inst.setLineSeparator(System.getProperty("line.separator"));
                int idx = interned.indexOf("-");
                KB kb = ((idx > -1) 
                         ? mgr.getKB(interned.substring(0, idx).trim())
                         : mgr.getKB(interned));
                if (kb instanceof KB)
                    inst.setKB(kb);
                String ontology = null;
                if ((idx > 0) && (idx < (interned.length() - 1))) {
                    ontology = interned.substring(idx + 1).trim();
                }
                if (StringUtil.emptyString(ontology) && (kb instanceof KB)) {
                    ontology = inst.getOntology(kb);
                }
                if (StringUtil.isNonEmptyString(ontology)) {
                    inst.setOntology(ontology);
                    inst.setDefaultNamespace(inst.getDefaultNamespace());
                    inst.setDefaultPredicateNamespace(inst.getDefaultPredicateNamespace());
                }
                inst.setDocGenKey(interned);
                DOC_GEN_INSTANCES.put(interned, inst);
            }
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }

        // System.out.println("EXIT DocGen.getInstance(" + key + ")");
        // System.out.println("  > " + inst.toString());

        return inst;
    }

    public static DocGen getInstance(KB kb, String ontology) {
        DocGen inst = null;
        try {
            inst = getInstance(kb.name + "-" + ontology);
            inst.setKB(kb);
            inst.setOntology(ontology);
            inst.setDefaultNamespace(inst.getDefaultNamespace());
            inst.setDefaultPredicateNamespace(inst.getDefaultPredicateNamespace());
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
        return inst;
    }

    /** *************************************************************
     * To obtain an instance of DocGen, use the static factory method
     * getInstance().
     */
    private DocGen() {
    }

    protected static final String SP2 = "  ";

    /** *************************************************************
     * The default base plus file suffix name for the main index file
     * for a set of HTML output files.
     */
    protected static String INDEX_FILE_NAME = "index.html";

    protected int localCounter = 0;

    public String getLineSeparator() {
        return System.getProperty("line.separator");
    }

    public void setLineSeparator(String ls) {
        System.setProperty("line.separator", ls);
        return;
    }

    /** *****************************************************************
     * A Set of String tokens (flags) that guides the document
     * generation process
     */
    private Set<String> docGenProcessTokens = new HashSet<String>();

    /** *****************************************************************
     * Returns the Set of String tokens that is used to guide aspects
     * of the document generation process.
     */
    public Set<String> getDocGenProcessTokens() {
        return docGenProcessTokens;
    }

    /** *****************************************************************
     * Adds a token to the Set of String tokens that is checked to
     * control aspects of the document generation process.
     */
    public void addDocGenProcessToken(String token) {
        if (StringUtil.isNonEmptyString(token))
            getDocGenProcessTokens().add(token.toLowerCase());
        return;
    }

    /** *****************************************************************
     * Clears the Set of String tokens that is checked to control
     * aspects of the document generation process.
     */
    public void clearDocGenProcessTokens() {
        getDocGenProcessTokens().clear();
        return;
    }

    /** *****************************************************************
     * Returns true if str is a member of the Set of String tokens
     * used to control the document generation process, else returns
     * false.
     */
    public boolean isDocGenProcessToken(String str) {
        boolean ans = false;
        if (StringUtil.isNonEmptyString(str))
            ans = getDocGenProcessTokens().contains(str.toLowerCase());
        return ans;
    }

    /** The default namespace associated with this DocGen object */
    private String defaultNamespace = "";

    /**
     * Returns the String denoting the default namespace
     * associated with this DocGen object.
     *
     */
    public String getDefaultNamespace() {
        try {
            if (StringUtil.emptyString(this.defaultNamespace)) {

                // If no value has been set, check to see if a value
                // is stored in the KB.
                KB kb = this.getKB();
                String onto = this.getOntology();
                setDefaultNamespace(kb.getFirstTermViaAskWithRestriction(0,
                                                                         "docGenDefaultNamespace",
                                                                         1,
                                                                         ontology,
                                                                         2));
            }
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
        return this.defaultNamespace;
    }

    /** *****************************************************************
     * Sets the default namespace for this DocGen object.
     */
    public void setDefaultNamespace(String namespace) {
        this.defaultNamespace = namespace;
        return;
    }

    /** *****************************************************************
     * The default namespace for predicates in the ontology associated
     * with this DocGen object
     */
    private String defaultPredicateNamespace = "";

    /** *****************************************************************
     * Returns the String denoting the default namespace for
     * predicates in the ontology associated with this DocGen
     * object.
     *
     */
    public String getDefaultPredicateNamespace() {
        try {
            if (StringUtil.emptyString(this.defaultPredicateNamespace)) {
                KB kb = getKB();
                String onto = getOntology();
                String dpn =
                    kb.getFirstTermViaAskWithRestriction(0,
                                                         "docGenDefaultPredicateNamespace",
                                                         1,
                                                         ontology,
                                                         2);
                setDefaultPredicateNamespace(dpn);
                if (StringUtil.emptyString(this.defaultPredicateNamespace)) {
                    setDefaultPredicateNamespace(getDefaultNamespace());
                }
            }
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
        return this.defaultPredicateNamespace;
    }

    /** *****************************************************************
     * Sets the default namespace for predicates in the ontology
     * associated with this DB object.
     *
     */
    public void setDefaultPredicateNamespace(String namespace) {
        this.defaultPredicateNamespace = namespace;
        return;
    }

    /** *****************************************************************
     * The full pathname of the KIF file to be generated by processing
     * a CSV or DIF input file produced from an Excel spreadsheet.
     */
    private String kifFilePathname = "";

    public String getKifFilePathname() {
        return this.kifFilePathname;
    }

    public void setKifFilePathname(String pathname) {
        this.kifFilePathname = pathname;
        return;
    }

    /** *****************************************************************
     * The ontology associated with this DocGen object, and for
     * which the DocGen object is used to generate files.
     */
    private String ontology = null;

    /** *****************************************************************
     */
    public void setOntology(String term) {
        this.ontology = term;
        return;
    }

    /** *****************************************************************
     * Returns a term denoting the default Ontology for this DocGen
     * object if an Ontology has been set, and tries very hard to find
     * a relevant Ontology if one has not been set.
     */
    public String getOntology() {
        try {
            String onto = this.ontology;
            if (StringUtil.emptyString(onto)) {
                KB kb = this.getKB();
                onto = this.getOntology(kb);
                if (StringUtil.isNonEmptyString(onto)) {
                    this.setOntology(onto);
                }
            }
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
        return this.ontology;
    }

    /** *****************************************************************
     * Returns a term denoting the default Ontology for this DocGen
     * object if an Ontology has been set, and tries very hard to find
     * a relevant Ontology if one has not been set.
     */
    public String getOntology(KB kb) {
        String onto = null;
        try {
            if (StringUtil.isNonEmptyString(this.ontology))
                onto = this.ontology;
            else {
                Set<String> candidates = new HashSet<String>();
                if (kb == null)
                    kb = this.getKB();
                if (kb instanceof KB) {
                    Iterator it = null;
                    // First, we try to find any obvious instances of
                    // Ontology, using predicate subsumption to take
                    // advantage of any predicates that have been
                    // liked with SUMO's predicates.
                    for (it = kb.getAllInstancesWithPredicateSubsumption("Ontology")
                             .iterator(); it.hasNext();) {
                        candidates.add((String) it.next());
                    }
                    if (candidates.isEmpty()) {
                        // Next, we check for explicit
                        // ontologyNamespace statements.
                        List formulae = kb.ask("arg", 0, "ontologyNamespace");
                        if ((formulae != null) && !formulae.isEmpty()) {
                            Formula f = null;
                            for (it = formulae.iterator(); it.hasNext();) {
                                f = (Formula) it.next();
                                candidates.add(f.getArgument(1));
                            }
                        }
                    }
                    if (!candidates.isEmpty()) {
                        // Here we try to match one of the ontologies
                        // to the name of the current KB, since we
                        // have no other obvious way to determine
                        // which ontology is appropriate if two or
                        // more are represented in the KB.  This
                        // section probably should use some word/token
                        // based partial matching algorithm, but does
                        // not.  We just accept the first fairly
                        // liberal regex match.
                        String termstr = null;
                        String ontoPattern = null;
                        String kbNamePattern = (".*(?i)" + kb.name + ".*");
                        for (it = candidates.iterator(); it.hasNext();) {
                            termstr = (String) it.next();
                            ontoPattern = (".*(?i)" + termstr + ".*");
                            if (termstr.matches(kbNamePattern) || kb.name.matches(ontoPattern)) {
                                onto = termstr;
                                break;
                            }
                        }
                        if (onto == null) {
                            // Finally, if onto is still null and
                            // candidates is not empty, we just grab a
                            // candidate and try it.
                            it = candidates.iterator();
                            if (it.hasNext()) {
                                onto = (String) it.next();
                            }
                        }
                    }
                }
                if (StringUtil.isNonEmptyString(onto))
                    this.setOntology(onto);
            }
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
        return onto;
    }

    /** *****************************************************************
     * The KB associated with this DocGen object.
     */
    private KB kb = null;

    /** *****************************************************************
     */
    public void setKB(KB kb) {
        this.kb = kb;
        return;
    }

    /** *****************************************************************
     *
     */
    public KB getKB() {
        return this.kb;
    }

    /** *************************************************************
     * A Set of Strings.
     */
    private Set codedIdentifiers = null;

    /** **************************************************************
     * Collects and returns the Set containing all known coded
     * identifiers in kb, including ISO code values stated to be such.
     * 
     * @param kb The KB in which to gather terms defined as coded
     * identifiers
     *
     *  @return A Set of all the terms that denote ISO code values and
     *  other coded identifiers
     */
    protected Set getCodedIdentifiers(KB kb) {
        try {
            if (codedIdentifiers == null) {
                codedIdentifiers = new TreeSet();
            }
            if (codedIdentifiers.isEmpty()) {
                Set codes = kb.getAllInstancesWithPredicateSubsumption("CodedIdentifier");
                Set classNames = kb.getAllSubClassesWithPredicateSubsumption("CodedIdentifier");
                classNames.add("CodedIdentifier");
                Object[] namesArr = classNames.toArray();
                if (namesArr != null) {
                    String className = null;
                    List formulae = new ArrayList();
                    for (int i = 0; i < namesArr.length; i++) {
                        className = (String) namesArr[i];
                        codes.addAll(kb.getTermsViaPredicateSubsumption("instance",
                                                                        2,
                                                                        className,
                                                                        1,
                                                                        false));
                    }
                }
                codedIdentifiers.addAll(codes);
            }
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
        return codedIdentifiers;
    }

    /** The document title text to be used for HTML generation */
    private String titleText = "";

    /** *************************************************************
     * Sets the title text to be used during HTML document generation.
     *
     * @param titlestr A String that will be used as the HTML document
     * title
     */
    public void setTitleText(String titlestr) {
        titleText = titlestr;
        return;
    }

    /** *************************************************************
     * Returns the String that will be used as the title text for HTML
     * document generation, else returns an empty String if no title
     * text value has been set.
     */
    public String getTitleText() {
        return titleText;
    }

    /** The document footer text to be used for HTML generation */
    private String footerText = "";

    // ("Produced by <a href=\"http://www.articulatesoftware.com\"> "
    //                          + "Articulate Software</a> and its partners");

    /** *************************************************************
     * Sets the footer text String to be used during HTML document
     * generation.
     *
     * @param str A String that will be used as the HTML document
     * footer text
     */
    public void setFooterText(String str) {
        footerText = str;
        return;
    }

    /** *************************************************************
     * Returns the String that will be used as the footer text for
     * HTML document generation, else returns an empty String if no
     * footer text value has been set.
     */
    public String getFooterText() {
        return footerText;
    }

    /** The style sheet (CSS filename) to be referenced in HTML generation */
    private String styleSheet = "simple.css";

    /** *************************************************************
     * Sets the base name plus suffix filename of the Cascading Style
     * Sheet file to be referenced during HTML document generation.
     *
     * @param filename A String that is a base filename plus a
     * filename suffix
     * 
     */
    public void setStyleSheet(String filename) {
        styleSheet = filename;
        return;
    }

    /** *************************************************************
     * Returns the base filename plus filename suffix form of the
     * Cascading Style Sheet file to be referenced during HTML
     * document generation, else returns an empty String if no value
     * has been set.
     */
    public String getStyleSheet() {
        return styleSheet;
    }

    /** The default image file (such as an organization's logo) to be
     * used in HTML generation
     */
    private String defaultImageFile = "articulate_logo.gif";

    /** *************************************************************
     * Sets the base name plus suffix filename of the logo image file
     * to be referenced during HTML document generation.
     *
     * @param filename A String that is a base filename plus a
     * filename suffix
     * 
     */
    public void setDefaultImageFile(String filename) {
        defaultImageFile = filename;
        return;
    }

    /** *************************************************************
     * Returns the base filename plus filename suffix form of the logo
     * image file to be referenced during HTML document generation,
     * else returns an empty String if no value has been set.
     */
    public String getDefaultImageFile() {
        return defaultImageFile;
    }

    /** The default image file (such as an organization's logo) to be
     * used in HTML generation, wrapped in any necessary additional
     * markup required for proper display.
     */
    private String defaultImageFileMarkup = "articulate_logo.gif";

    /** *************************************************************
     * Sets the base name plus suffix filename of the logo image file
     * to be referenced during HTML document generation.
     *
     * @param markup A String that includes the image file pathname
     * plus any additional markup required for proper display of the
     * image
     * 
     */
    public void setDefaultImageFileMarkup(String markup) {
        defaultImageFileMarkup = markup;
        return;
    }

    /** *************************************************************
     * Returns the base filename plus filename suffix form of the logo
     * image file, wrapped in any additional markup required for the
     * intended rendering of the image.
     */
    public String getDefaultImageFileMarkup() {
        return defaultImageFileMarkup;
    }

    /** The canonical pathname of the directory in which HTML output
     * files should be saved.
     */
    private String htmlOutputDirectoryPath = "";

    /** *************************************************************
     * Sets the canonical pathname String of the directory in which
     * HTML output files should be saved.
     *
     * @param pathname A canonical pathname String
     * 
     */
    public void setHtmlOutputDirectoryPath(String pathname) {
        htmlOutputDirectoryPath = pathname;
        return;
    }

    /** *************************************************************
     * Returns the canonical pathname String of the directory in which
     * HTML output files should be saved.
     */
    public String getHtmlOutputDirectoryPath() {
        try {
            if (StringUtil.emptyString(htmlOutputDirectoryPath)) {
                File dir = makeOutputDir("dd", StringUtil.getDateTime("yyyyMMdd"));
                if (dir.isDirectory() && dir.canWrite()) {
                    setHtmlOutputDirectoryPath(dir.getCanonicalPath());
                }
            }
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
        return htmlOutputDirectoryPath;
    }

    /** *************************************************************
     * A Map in which the keys are SUO-KIF terms and the values are
     * Lists.  Each element in a List is a "top level" term in a
     * subsumption graph formed by syntacticSubordinate or its
     * subrelations.  The keys are the focal terms for this DocGen
     * instance (e.g., MessageTypes or namespaces) .
     *
     */
    private Map topLevelTerms = null;

    /** *************************************************************
     * Sets the Map containing the List of top-level terms (values)
     * for each focal term (keys) in the ontology associated with this
     * DocGen instance.
     *
     * @param tops A Map in which each key is a focal term and each
     * value is a list of the top-level terms for the focal term
     */
    public void setTopLevelTerms(Map tops) {
        this.topLevelTerms = tops;
        return;
    }

    /** *************************************************************
     * Returns a Map in which the keys are SUO-KIF terms and the
     * values are Lists.  Each element in a List is a "top level" term
     * in a subsumption graph formed by syntacticSubordinate or its
     * subrelations.  The keys are the focal terms for the ontology
     * associated with this DocGen instance (e.g., MessageTypes or
     * namespaces).  Returns null if no Map for topLevelTerms has been
     * set.
     */
    public Map getTopLevelTerms() {
        return this.topLevelTerms;
    }

    /** 
     * A Map containing String replacement pairs.  This is to provide
     * adequate ASCII translations for HTML character entities, in
     * circumstances where occurrences of the entities might cause
     * parsing or rendering problems (e.g., apparently, in XSD files).
     *
     */
    private Map stringReplacementMap = null;

    /** *************************************************************
     * Sets the Map to be used for HTML character entity to ASCII 
     * replacements.
     */
    public void setStringReplacementMap(Map keyValPairs) {
        this.stringReplacementMap = keyValPairs;
        return;
    }

    /** *************************************************************
     * Returns the Map to be used for HTML character entity to ASCII
     * replacements, attempting to build it from
     * docGenCodeMapTranslation statements found in the KB if the Map
     * does not already exist.
     */
    public Map getStringReplacementMap() {
        try {
            if (stringReplacementMap == null) {
                Map srMap = new HashMap();
                KB kb = getKB();
                if (kb instanceof KB) {
                    List formulae = kb.ask("arg", 0, "docGenCodeMapTranslation");
                    if (formulae != null) {
                        Formula f = null;
                        for (Iterator it = formulae.iterator(); it.hasNext();) {
                            f = (Formula) it.next();
                            srMap.put(StringUtil.removeEnclosingQuotes(f.getArgument(2)),
                                      StringUtil.removeEnclosingQuotes(f.getArgument(4)));
                        }
                    }
                }
                else {
                    System.out.println("WARNING in DocGen.getStringReplacementMap()");
                    System.out.println("  DocGen.defaultKB is not set");
                }
                if (srMap.isEmpty()) {
                    System.out.println("WARNING in DocGen.getStringReplacementMap()");
                    System.out.println("  DocGen.stringReplacementMap is empty");
                }
                setStringReplacementMap(srMap);
            }
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
        return this.stringReplacementMap;
    }

    /** *************************************************************
     * Returns the String replacement for fromString, if one can be
     * located, else just returns fromString.
     *
     * @param fromString A String for which a replacement is sought
     *
     * @return A replacement String
     */
    public String getStringReplacement(String fromString) {
        String toString = fromString;
        try {
            Map replacements = getStringReplacementMap();
            if (replacements != null) {
                String rep = (String) replacements.get(fromString);
                if (rep != null) {
                    toString = rep;
                }
            }
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
        return toString;
    }

    /** A set of the predicates that should not be displayed to the user. */
    private Set inhibitDisplayRelations = null;

    /** *************************************************************
     * Sets the predicates for which diplay should be suppressed to
     * those contained in relations.
     *
     * @param relations A Set of predicate names
     *
     */
    public void setInhibitDisplayRelations(Set relations) {
        this.inhibitDisplayRelations = relations;
        return;
    }

    /** *************************************************************
     * Returns a Set containing the names of those predicates for
     * which diplay should be suppressed, and tries to create the Set
     * from docGenInhibitDisplayRelations statements found in the
     * current KB if the Set does not already exist.
     *
     * @return a Set of predicate names
     */
    public Set getInhibitDisplayRelations() {
        try {
            if (inhibitDisplayRelations == null) {
                KB kb = getKB();
                String ontology = getOntology();
                Set idr = new TreeSet();
                if ((kb != null) && StringUtil.isNonEmptyString(ontology)) {
                    idr.addAll(kb.getTermsViaAskWithRestriction(0, 
                                                                "docGenInhibitDisplayRelation", 
                                                                1, 
                                                                ontology,
                                                                2));
                }
                setInhibitDisplayRelations(idr);
                if (inhibitDisplayRelations.isEmpty()) {
                    System.out.println("WARNING in DocGen.getInhibitDisplayRelations()");
                    System.out.println("  DocGen.inihibitDisplayRelations is empty");
                }
            }
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
        return inhibitDisplayRelations;
    }

    /** *************************************************************
     * The header to be used for the the table of contents (or index
     * list) section during HTML generation.
     */
    private String tocHeader = "";

    /** *************************************************************
     * Sets the String header to be used in generated HTML files to
     * header.
     */
    public void setTocHeader(String header) {
        this.tocHeader = header;
        return;
    }

    /** *************************************************************
     * Returns the String header to be used in generated HTML files.
     */
    public String getTocHeader() {
        return this.tocHeader;
    }

    private HashMap sumoMappings = new HashMap();
    protected Map basesToQWords = new HashMap();
    protected Map headwords = new HashMap();
    private Map namespacesToHeadwords = null;
    private Map headwordsToNamespaces = null;

    /** A default key to identify this particular DocGen object **/
    private String docGenKey = DEFAULT_KEY;

    /**
     * Returns the String key that is the index for this particular
     * DocGen object.
     *
     */
    public String getDocGenKey() {
        return this.docGenKey;
    }

    /**
     * Sets the String key that is the index for this particular
     * DocGen object.
     *
     */
    public void setDocGenKey(String key) {
        this.docGenKey = key;
        return;
    }

    public static String getKifNamespaceDelimiter() {
        return StringUtil.getKifNamespaceDelimiter();
    }

    public static String getW3cNamespaceDelimiter() {
        return StringUtil.getW3cNamespaceDelimiter();
    }

    public static String getSafeNamespaceDelimiter() {
        return StringUtil.getSafeNamespaceDelimiter();
    }

    public static List<String> getNamespaceDelimiters() {
        return Arrays.asList("^", 
                             "!", 
                             StringUtil.getKifNamespaceDelimiter(),
                             StringUtil.getW3cNamespaceDelimiter());
    }

    protected static final String NAMES_FIELD_PATTERN = ".*hw!.*|.*syn!.*|.*en!.*|.*xsdname!.*";

    /** 
     * A List containing the token Strings that denote some kind of
     * relationship between a term and a name.
     */
    private static final Map<String, String> tokenRelationMap = new HashMap<String, String>();
    private static final List<String> relationTokens = new ArrayList<String>();
    static {
        String[][] tokenRelations = { {"_hw",     "headword"   },
                                      {"_syn",    "synonym"    },
                                      {"_en",     "termFormat" },
                                      {"xsdname", "xsdFileName"} };
        for (int i = 0; i < tokenRelations.length; i++) {
            tokenRelationMap.put(tokenRelations[i][0], tokenRelations[i][1]);
        }
        relationTokens.addAll(tokenRelationMap.keySet());
    }
            

    protected List<String> getNameRelationTokens() {
        return relationTokens;
    }

    protected String getNameRelationForToken(String token) {
        return tokenRelationMap.get(token);
    }

    /** *************************************************************
     * If true, a termFormat value obtained for term will be displayed
     * rather than the term name itself.
     */
    public boolean simplified = false;

    /** *************************************************************
     * A Map in which each key is a KB name and the corresponding
     * value is a List of the Predicates defined in the KB.
     */
    private HashMap relationsByKB = new HashMap();

    public HashMap getRelationsByKB() {
        return relationsByKB;
    }

    /** *************************************************************
     * Returns a String consisting of str concatenated indent times.
     *
     * @param str The String to be concatentated with itself
     *
     * @param indent An int indicating the number of times str should
     * be concatenated
     *
     * @return A String
     */
    public static String indentChars(String str, int indent) {

        StringBuilder result = new StringBuilder();
        for (int i = 0; i < indent; i++) {
            result.append(str);
        }
        return result.toString();
    }

    /** *************************************************************
     * The parent directory for target subdirectories of HTML, XSD,
     * and other types of files generated by this DocGen object.
     */
    private File outputParentDir = new File(KBmanager.getMgr().getPref("baseDir"));

    /** *************************************************************
     * Sets the parent directory in which subdirectories for different
     * types of output files will be created to the File obj, and
     * tries to create the directory pathname if it does not already
     * exist.
     *
     * @param obj A File object representing a directory
     */
    public void setOutputParentDir(File obj) {
        try {
            if (obj instanceof File) {
                if (!obj.isDirectory() || !obj.canWrite()) {
                    System.out.println("WARNING in DocGen.setOutputParentDir(" + obj + "):");
                    System.out.println("  " + obj + " is not an accessible directory");
                    String pathname = obj.getCanonicalPath();
                    if (StringUtil.isNonEmptyString(pathname)) {
                        System.out.println("  Will try to create " + pathname);
                        obj.mkdirs();
                    }
                }
                if (obj.isDirectory() && obj.canWrite()) {
                    this.outputParentDir = obj;
                }
                else {
                    System.out.println("WARNING in DocGen.setOutputParentDir(" + obj + "):");
                    System.out.println("  Could not set outputParentDir");
                }
            }
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
        return;
    }

    /** *************************************************************
     * Sets to pathname the parent directory in which subdirectories
     * for different types of output files will be created, and tries
     * to create the directory pathname if it does not already exist.
     *
     * @param pathname A String representing a directory pathname
     */
    public void setOutputParentDir(String pathname) {
        try {
            if (StringUtil.isNonEmptyString(pathname)) {
                setOutputParentDir(new File(pathname));
            }
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
        return;
    }

    /** *************************************************************
     * Sets the parent directory in which subdirectories for different
     * types of output files will be created to the File obj, and
     * tries to create the directory pathname if it does not already
     * exist.
     *
     * @param pathname A String representing a directory pathname
     */
    public void setOutputParentDir(List pathnameComponents) {
        try {
            String fs = System.getProperty("file.separator");
            StringBuilder sb = new StringBuilder();
            String comp = null;
            boolean isFirst = true;
            for (Iterator it = pathnameComponents.iterator(); it.hasNext();) {
                comp = (String) it.next();
                if (isFirst) {
                    if ((comp instanceof String) && comp.equals("")) {
                        comp = fs;
                    }
                }
                else {
                    sb.append(fs);
                }
                sb.append(comp);
                isFirst = false;
            }
            String pathname = sb.toString();
            if (StringUtil.isNonEmptyString(pathname)) {
                setOutputParentDir(new File(pathname));
            }
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
        return;
    }

    /** *************************************************************
     * Sets the parent directory in which subdirectories for different
     * types of output files will be created to the abstract pathname
     * represented by a statement in kb formed with the predicate
     * docGenOutputParentDirectory and the argument ontology.  Tries
     * to create the directory pathname if it does not already exist.
     *
     * @param kb The KB containing a statement formed with the
     * predicate docGenOutputParentDirectory and ontology
     *
     * @param ontology The ontology referred to in a statement formed
     * with the predicate docGenOutputParentDirectory in kb
     *
     */
    public void setOutputParentDir(KB kb, String ontology) {
        try {
            String flist = kb.getFirstTermViaAskWithRestriction(0,
                                                                "docGenOutputParentDirectory",
                                                                1,
                                                                ontology,
                                                                2);
            if (StringUtil.isNonEmptyString(flist)) {
                Formula f = new Formula();
                f.read(flist);
                if (f.listP()) {
                    ArrayList pathnameComponents = new ArrayList();
                    String comp = null;
                    for (int i = 0; f.listP() && !f.empty(); i++) {
                        comp = StringUtil.removeEnclosingQuotes(f.car());
                        if (!((i == 0) && comp.equals("ListFn"))) {
                            pathnameComponents.add(comp);
                        }
                        f.read(f.cdr());
                    }
                    setOutputParentDir(pathnameComponents);
                }
            }
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
        return;
    }

    /** *************************************************************
     * Returns a File object representing the directory in which the
     * subdirectories for the various types of output files will be
     * located.
     */
    public File getOutputParentDir() {
        return this.outputParentDir;
    }

    /** *************************************************************
     * The DisplayFilter which, if present, determines if a given
     * SUO-KIF object may be displayed or output by this DocGen
     * object.
     */
    private DisplayFilter displayFilter = null;

    /** *************************************************************
     * Sets the DisplayFilter associated with this DocGen object to
     * filterObj.
     *
     * @param filterObj An instance of DisplayFilter
     */
    public void setDisplayFilter(DisplayFilter filterObj) {
        this.displayFilter = filterObj;
        return;
    }

    /** *************************************************************
     * Returns the DisplayFilter object associated with this DocGen
     * object, or null if no DisplayFilter has been set.
     */
    public DisplayFilter getDisplayFilter() {
        return this.displayFilter;
    }

    /** *************************************************************
     *  @param stringMap is a map of String keys and values
     *  @return a TreeMap of TreeMaps of ArrayLists where the keys
     *          are uppercase single characters (of term formats or
     *          headwords) and the values are TreeMaps with a key of
     *          the term formats or headwords and ArrayList values
     *          of the actual term names.  Note that if "simplified"
     *          is false actual term names will be used instead of
     *          term formats or headwords and the interior map will
     *          have keys that are the same as their values.
     * 
     *          Pictorially:
     *      letter->    formattedTerm1->term11,term12...term1N
     *                  formattedTerm2->term21,term22...term2N
     */
    public TreeMap createAlphaList(KB kb // , HashMap stringMap
                                   ) {

        long t1 = System.currentTimeMillis();
        System.out.println("ENTER DocGen.createAlphaList("
                           + kb.name // + ", "
                           // + "[map with " + stringMap.size() + " entries]" 
                           + ")");

        TreeMap alphaList = new TreeMap(String.CASE_INSENSITIVE_ORDER);
        try {
            for (Iterator it = kb.terms.iterator(); it.hasNext();) {
                String term = (String) it.next();
                if (isLegalForDisplay(StringUtil.w3cToKif(term))
                    && !getCodedIdentifiers(kb).contains(term)
                    // && !term.matches("^iso\\d+.*_.+")
                    ) {
                    String formattedTerm = stripNamespacePrefix(kb, term);
                    if (simplified) {
                        String smterm = // (String) stringMap.get(term);
                            getTermPresentationName(kb, term);
                        if (StringUtil.isNonEmptyString(smterm)) {
                            formattedTerm = stripNamespacePrefix(kb, smterm);
                        }
                    }
                    if (StringUtil.isNonEmptyString(formattedTerm)) {                
                        String firstLetter = 
                            Character.toString(Character.toUpperCase(formattedTerm.charAt(0)));

                        Set alset = alphaList.keySet();
                        if ((alset != null) && alset.contains(firstLetter)) {
                            TreeMap map = (TreeMap) alphaList.get(firstLetter);
                            ArrayList al = (ArrayList) map.get(formattedTerm);
                            if (al == null) {
                                al = new ArrayList();                    
                                map.put(formattedTerm,al);
                            }
                            al.add(term);
                            //System.out.println(firstLetter + " " + formattedTerm + " " + term);
                        }
                        else {
                            TreeMap map = new TreeMap(String.CASE_INSENSITIVE_ORDER);
                            ArrayList al = new ArrayList();
                            al.add(term);
                            map.put(formattedTerm,al);
                            alphaList.put(firstLetter,map);
                            //System.out.println(firstLetter + " " + formattedTerm + " " + term);
                        }
                    }
                    else {
                        System.out.println("  >          term == " + term);
                        System.out.println("  > formattedTerm == " + formattedTerm);
                    }
                }
            }
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
        System.out.println("EXIT DocGen.createAlphaList("
                           + kb.name // + ", "
                           // + "[map with " + stringMap.size() + " entries]" 
                           + ")");
        System.out.println("  > "
                           + ((System.currentTimeMillis() - t1) / 1000.0)
                           + " seconds elapsed time");

        return alphaList;
    }

    /** **************************************************************
     * Returns true if term is an instance or subclass of
     * CompositeContentBearingObject in kb, else returns false.
     *
     * @param kb The KB in which to check the definition of term
     *
     * @param term A SUO-KIF term
     *
     * @return true or false
     */
    public static boolean isComposite(KB kb, String term) {
        boolean ans = false;
        try {
            ans = (kb.isInstanceOf(term, "CompositeContentBearingObject")
                   || kb.isSubclass(term, "CompositeContentBearingObject")
                   || kb.isInstanceOf(term, "CompositeContentBearingObjectType"));
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
        return ans;
    }

    /** *************************************************************
     * 
     * 
     */
    public static File dataFileToKifFile(KB kb, 
                                         String ontology, 
                                         String baseFileName, 
                                         File infile,
                                         boolean overwrite,
                                         boolean loadConstituent) {
        File outfile = null;
        try {
            KBmanager mgr = KBmanager.getMgr();
            String inCanonicalPath = infile.getCanonicalPath();
            String inFileName = infile.getName();
            int lidx = inFileName.lastIndexOf(".");
            String suffix = ((lidx != -1)
                             ? inFileName.substring(lidx)
                             : "");
            List rows = null;
            if (suffix.equalsIgnoreCase(".csv"))
                rows = DB.readSpreadsheet(inCanonicalPath, Arrays.asList("u,", "U,", "$,"));
            else if (suffix.equalsIgnoreCase(".dif")) 
                rows = DB.readDataInterchangeFormatFile(inCanonicalPath);
            if ((rows != null) && !rows.isEmpty()) {

                System.out.println("\n");
                for (Iterator itr = rows.iterator(); itr.hasNext();) {
                    System.out.println((List) itr.next());
                }
                System.out.println("");

                String kbDir = mgr.getPref("kbDir");
                File kbDirFile = new File(kbDir);
                String base = baseFileName;
                if (StringUtil.emptyString(base))
                    base = inFileName.substring(0, lidx);
                File baseOutfile = new File(kbDirFile, (base + ".kif"));
                File tmpOutfile = StringUtil.renameFileIfExists(baseOutfile);
                DocGen spi = DocGen.getInstance(kb, ontology);
                outfile = spi.processDataRows(kb, 
                                              rows, 
                                              ontology, 
                                              base, 
                                              tmpOutfile, 
                                              overwrite, 
                                              loadConstituent);
            }
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
        return outfile;
    }

    /** ********************************************************************
     * Generates HTML files or sets of files from a KB based on the
     * input arguments and, more importantly, on the contents of the
     * KB and the control meta-data represented in it.  Each set of
     * files is saved in a uniquely named
     *
     * @param args A List of String tokens that control the behavior
     * of this method.  The first token must be "rcdocgen".  The
     * second token must be the name of a the KB from which the files
     * will be generated.  The third token must be the name of an
     * ontology that is defined in the KB identified by the second
     * token.  The fourth token indicates a specific type of file to
     * be generated.  The supported file type tokens and corresponding
     * types of files are as follows:<code>
     *
     *      dd - "Data Dictionary" HTML files;
     *
     *     tab - "Tabular display" HTML files.
     * <code>
     *
     * @param simplified If true, the contents of the KB will be
     * printed in some version of a condensed, tabular format
     *
     * @param titleText A String that, if present, will be used the
     * construct the title region of each generated page or document,
     * and that may include arbitrary HTML markup
     *
     * @param footerTest A String that, if present, will be used the
     * construct the footer region of each generated page or document,
     * and that may include arbitrary HTML markup
     * 
     * @return A String indicating the exit status of this method
     */
    public static String generateHtmlFiles(List<String> args, 
                                           boolean simplified,
                                           String titleText,
                                           String footerText) {
        long t1 = System.currentTimeMillis();
        System.out.println("ENTER DocGen.generateHtmlFiles(" 
                           + args + ", "
                           + simplified + ", "
                           + titleText + ", "
                           + footerText + ")");
        String status = "";
        try {
            int arglen = args.size();
            String kbName = null;
            String ontology = null;
            List<String> fileTypes = new ArrayList<String>();
            String str = "";
            int i = 0;
            for (Iterator it = args.iterator(); it.hasNext(); i++) {
                str = StringUtil.removeEnclosingQuotes((String) it.next());
                if (i == 1) kbName = str;
                if (i == 2) ontology = str;
                if (i > 2) fileTypes.add(str);
            }
            boolean ok = true;
            while (ok) {
                String ls = System.getProperty("line.separator");
                if (arglen < 2) {
                    status += (ls + "Too few input arguments");
                    ok = false;
                    break;
                }

                if (StringUtil.emptyString(kbName)) {                
                    status += (ls + "No KB name was entered");
                    ok = false;
                    break;
                }

                if (StringUtil.emptyString(ontology)) {
                    status += (ls + "No ontology name was entered");
                    ok = false;
                    break;
                }

                KBmanager mgr = KBmanager.getMgr();
                mgr.initializeOnce();
                KB kb = mgr.getKB(kbName);
                if (!(kb instanceof KB)) {
                    status += (ls + "There is no KB named " + kbName);
                    ok = false;
                    break;
                }
                DocGen spi = DocGen.getInstance(kb, ontology);
                spi.setKB(kb);
                spi.setOntology(ontology);
                spi.setOutputParentDir(kb, ontology);
                if (StringUtil.isNonEmptyString(titleText))
                    spi.setTitleText(titleText);
                if (StringUtil.isNonEmptyString(footerText))
                    spi.setFooterText(footerText);
                spi.getNamespaces(kb, ontology, true);
                DisplayFilter df = new DisplayFilter() {
                        Map boolMap = new HashMap();
                        public boolean isLegalForDisplay(DocGen dg, String term) {
                            boolean ans = StringUtil.isNonEmptyString(term);
                            try {
                                String boolStr = (String) boolMap.get(term);
                                if (StringUtil.isNonEmptyString(boolStr)) {
                                    ans = Boolean.parseBoolean(boolStr);
                                }
                                else if (ans && (dg != null)) {
                                    KB dgkb = dg.getKB();
                                    String dgonto = 
                                        StringUtil.removeEnclosingQuotes(dg.getOntology());
                                    if ((dgkb != null) 
                                        && StringUtil.isNonEmptyString(dgonto)) {
                                        String nsd = StringUtil.getW3cNamespaceDelimiter();
                                        ans = (getClientOntologyNames().contains(dgonto)
                                               && term.matches("^\\w+" + nsd + ".+")
                                               && !StringUtil.isLocalTermReference(term));

                                        if (ans) {
                                            String namespace = 
                                                dg.getTermNamespace(dgkb, term);
                                            List ontoNamespaces = 
                                                // dg.getOntologyNamespaces(dgkb, dgonto);
                                                dg.getNamespaces(dgkb, dgonto, false);
                                            ans = 
                                                (ontoNamespaces.contains(namespace)
                                                 || ontoNamespaces.contains(term));
                                        }
                                    }
                                    boolMap.put(term, Boolean.toString(ans));
                                }
                            }
                            catch (Exception ex) {
                                ex.printStackTrace();
                            }
                            return ans;
                        }
                    };

                spi.setDisplayFilter(df);

                List<String> predicates = Arrays.asList("docGenDefaultNamespace",
                                                        "docGenDefaultPredicateNamespace",
                                                        "docGenLogoImageFile",
                                                        "docGenLogoImageMarkup",
                                                        "docGenStyleSheet",
                                                        "docGenTitleText",
                                                        "docGenFooterText");
                for (String pred : predicates) {
                    String val = 
                        kb.getFirstTermViaAskWithRestriction(0,
                                                             pred,
                                                             1,
                                                             ontology,
                                                             2);
                    val = StringUtil.removeEnclosingQuotes(val);
                    val = StringUtil.removeQuoteEscapes(val);
                    if (StringUtil.isNonEmptyString(val)) {
                        if (pred.equals("docGenDefaultNamespace"))
                            spi.setDefaultNamespace(val);
                        else if (pred.equals("docGenDefaultPredicateNamespace"))
                            spi.setDefaultPredicateNamespace(val);
                        else if (pred.equals("docGenLogoImageFile"))
                            spi.setDefaultImageFile(val);
                        else if (pred.equals("docGenLogoImageMarkup"))
                            spi.setDefaultImageFileMarkup(val);
                        else if (pred.equals("docGenStyleSheet"))
                            spi.setStyleSheet(val);
                        else if (pred.equals("docGenTitleText") 
                                 && StringUtil.emptyString(titleText))
                            spi.setTitleText(val);
                        else if (pred.equals("docGenFooterText")
                                 && StringUtil.emptyString(footerText))
                            spi.setFooterText(val);
                    }
                }
                spi.setTopLevelTerms(spi.computeTopLevelTermsByNamespace(kb, ontology));
                Set<String> formatTokens = new HashSet<String>();
                List<String> ftterms = kb.getTermsViaAskWithRestriction(0, 
                                                                        "docGenOutputFormat", 
                                                                        1, 
                                                                        ontology,
                                                                        2);
                for (String ftterm : ftterms) {
                    formatTokens.add(StringUtil.removeEnclosingQuotes(ftterm));
                }

                for (String token : fileTypes) {
                    String lcToken = token.toLowerCase();
                    formatTokens.add(lcToken);
                    File outdir = 
                        spi.makeOutputDir((kb.name + "_" + lcToken), 
                                          StringUtil.getDateTime("yyyyMMdd"));
                    String outdirPath = outdir.getCanonicalPath();
                    status = outdirPath;
                    if (token.equalsIgnoreCase(F_TAB)) {
                        if (spi.getCoreTerms().isEmpty()) {
                            spi.computeTermRelevance(kb, ontology);
                        }
                        spi.generateTabularFocalTermFiles(kb, ontology, outdirPath);
                    }
                    else if (token.equalsIgnoreCase(F_DD) || token.equalsIgnoreCase(F_DD2)) {
                        spi.setHtmlOutputDirectoryPath(outdirPath);
                        if (spi.getCoreTerms().isEmpty()) {
                            spi.computeTermRelevance(kb, ontology);
                        }
                        spi.generateHTML(kb, 
                                         spi.getDefaultNamespace(), 
                                         simplified,
                                         formatTokens);
                    }
                    // Copy the .css and logo files into the directory
                    // where the HTML output files have been saved.
                    String fname = null;
                    for (int j = 0; j < 2; j++) {
                        switch(j) { 
                        case 0 : fname = spi.getDefaultImageFile();
                            break;
                        case 1 : fname = spi.getStyleSheet();
                            break;
                        }
                        if (StringUtil.isNonEmptyString(fname)) {
                            File kbDirFile = new File(KBmanager.getMgr().getPref("kbDir"));
                            File infile = new File(kbDirFile, fname);
                            File outfile = new File(outdir, fname);
                            KBmanager.copyFile(infile, outfile);
                        }
                    }
                }
                ok = false;
            }
        }
        catch (Exception ex) {
            String errStr = ex.getMessage();
            status = ("Error generating HTML files: " + errStr);
            System.out.println(errStr);
            ex.printStackTrace();
        }
        System.out.println("EXIT DocGen.generateHtmlFiles(" 
                           + args + ", "
                           + simplified + ", "
                           + titleText + ", "
                           + footerText + ")");
        System.out.println("  > "
                           + ((System.currentTimeMillis() - t1) / 1000.0)
                           + " seconds total elapsed time");
        return status;
    }

    /** ********************************************************************
     * Generates sets of documents from a KB based on the directive
     * tokens provided in the input String[] args.  The array args
     * must contain at least two tokens.
     *
     * The first token must be the name of a Sigma KB, which will be
     * constructed by loading one or more KIF files prior to document
     * generation.
     *
     * The second token must be the name of an ontology that is
     * defined in the KB identified by the first token.
     *
     * The first two tokens, plus additional file pathname information
     * retrieved from the newly constructed KB, are the only inputs
     * required in order to enable and cause the generation of a KIF
     * file from an Excel-derived CSV or DIF input file (i.e., a SCOW
     * input file).  The KIF file is then loaded into the KB and
     * serves as the basis for the generation of sets of files
     * indicated by the remaining tokens, if any.
     *
     * Each of the third and subsequent tokens indicates a specific
     * type of file to be generated.  These tokens may occur in any
     * combination or order, and the generation of each type of file
     * will proceed in the order the tokens are listed.  Note that the
     * tokens themselves are used as the base names for the
     * subdirectories in which files of the denoted type will be
     * saved.  The supported tokens and corresponding types of files
     * are as follows:
     *
     *     xsd - XML Schema (aka XSD) files
     *
     *      dd - Data Dictionary HTML files
     *     dd2 - Data Dictionary HTML files, format #2
     *
     *     tab - "Tabular display" HTML files
     *
     * @param args A List of tokens that control the behavior of this
     * method
     *
     * @return A String indicating the exit status of this method
     *
     */
    public static String generateDocumentsFromKB(List<String> args) {

        long t1 = System.currentTimeMillis();
        System.out.println("ENTER DocGen.generateDocumentsFromKB(" + args + ")");

        long inputFileElapsedTime = 0;
        long tltElapsedTime = 0;
        long xsdElapsedTime = 0;
        long ddElapsedTime = 0;
        long tabElapsedTime = 0;
        long relevanceElapsedTime = 0;
        String inputFileCanonicalPath = "";

        Set argset = new LinkedHashSet(args);
        int arglen = argset.size();
        String argstr = "";
        Iterator it = null;
        int i = 0;
        for (it = argset.iterator(); it.hasNext(); i++) {
            if (i > 0) argstr += ", ";
            argstr += (String) it.next();
        }

        String status = "";
        String ls = System.getProperty("line.separator");
        try {            
            String kbName = null;
            String ontology = null;
            List<String> fileTypes = new ArrayList<String>();

            status += "DocGen.generateDocumentsFromKB(";

            String str = "";
            i = 0;
            for (it = argset.iterator(); it.hasNext(); i++) {
                str = StringUtil.removeEnclosingQuotes((String) it.next());
                if (i == 0) kbName = str;
                if (i == 1) ontology = str;
                if (i > 1) fileTypes.add(str);
                status += (" " + str);
            }
            status += ")";

            boolean ok = true;
            while (ok) {

                if (arglen < 2) {
                    status += (ls + "Too few input arguments");
                    ok = false;
                    break;
                }

                if (StringUtil.emptyString(kbName)) {                
                    status += (ls + "No KB name was entered");
                    ok = false;
                    break;
                }

                if (StringUtil.emptyString(ontology)) {
                    status += (ls + "No ontology name was entered");
                    ok = false;
                    break;
                }

                KBmanager mgr = KBmanager.getMgr();
                mgr.initializeOnce();
                KB kb = mgr.getKB(kbName);
                if (!(kb instanceof KB)) {
                    status += (ls + "There is no KB named " + kbName);
                    ok = false;
                    break;
                }

                String indirPath = mgr.getPref("kbDir");
                File indir = (StringUtil.isNonEmptyString(indirPath)
                              ? new File(indirPath)
                              : null);
                if (!indir.isDirectory()) {
                    status += (ls + indirPath + " is not a directory");
                    ok = false;
                    break;
                }

                String inputFilename = 
                    StringUtil
                    .removeEnclosingQuotes(kb.getFirstTermViaAskWithRestriction(0,
                                                                                "docGenInputFile",
                                                                                1, 
                                                                                ontology,
                                                                                2));
                if (StringUtil.emptyString(inputFilename)) {
                    status += (ls + "No input file could be found for " + ontology);
                    ok = false;
                    break;
                }

                File inputFile = new File(indir, inputFilename);
                if (!inputFile.canRead()) {
                    status += (ls + "The file " + inputFile.getCanonicalPath()  
                               + " does not exist, or cannot be read");
                    ok = false;
                    break;
                }
                inputFileCanonicalPath = inputFile.getCanonicalPath();
                // int csvIdx = csvCanonicalPath.lastIndexOf(".csv");
                int suffIdx = -1;
                String suff = "";
                for (Iterator ifi = getInputFileSuffixes().iterator(); ifi.hasNext();) {
                    String suffstr = (String) ifi.next();
                    suffIdx = inputFileCanonicalPath.lastIndexOf(suffstr);
                    if (suffIdx > -1) {
                        suff = suffstr;
                        break;
                    }
                }
                String kifFilename = ((suffIdx > 0)
                                      ? (inputFileCanonicalPath.substring(0, suffIdx) + ".kif")
                                      : (inputFileCanonicalPath + ".kif"));
                File kifFile = new File(kifFilename);
                String kifCanonicalPath = kifFile.getCanonicalPath();

                DocGen spi = DocGen.getInstance(kb, ontology);
                spi.setKifFilePathname(kifCanonicalPath);
                spi.setKB(kb);
                spi.setOntology(ontology);
                spi.setOutputParentDir(kb, ontology);

                List rows = null;
                if (suff.equalsIgnoreCase(".csv")) {
                    rows = DB.readSpreadsheet(inputFileCanonicalPath, 
                                              Arrays.asList("u,", "U,", "$,"));
                }
                else if (suff.equalsIgnoreCase(".dif")) {
                    rows = DB.readDataInterchangeFormatFile(inputFileCanonicalPath);
                }
                if ((rows != null) && !rows.isEmpty()) {

                    spi.processSpreadsheet(kb, rows, ontology, kifCanonicalPath);

                    if (kifFile.canRead() && !fileTypes.isEmpty()) {

                        // kb.addConstituent(kifCanonicalPath);

                        long t2 = System.currentTimeMillis();
                        inputFileElapsedTime = (t2 - t1);

                        DisplayFilter df = new DisplayFilter() {
                                Map boolMap = new HashMap();
                                public boolean isLegalForDisplay(DocGen dg, String term) {
                                    boolean ans = StringUtil.isNonEmptyString(term);
                                    try {
                                        String boolStr = (String) boolMap.get(term);
                                        if (StringUtil.isNonEmptyString(boolStr)) {
                                            ans = Boolean.parseBoolean(boolStr);
                                        }
                                        else if (ans && (dg != null)) {
                                            KB dgkb = dg.getKB();
                                            String dgonto = 
                                                StringUtil.removeEnclosingQuotes(dg.getOntology());
                                            if ((dgkb != null) 
                                                && StringUtil.isNonEmptyString(dgonto)) {
                                                String nsd = StringUtil.getW3cNamespaceDelimiter();
                                                ans = 
                                                    (getClientOntologyNames().contains(dgonto)
                                                     && term.matches("^\\w+" + nsd + ".+")
                                                     /*
                                                       && !dg.getNamespaces(dgkb,
                                                       dgonto, 
                                                       false).contains(term)
                                                     */
                                                     && !StringUtil.isLocalTermReference(term));

                                                if (ans) {
                                                    String namespace = 
                                                        dg.getTermNamespace(dgkb, term);
                                                    List ontoNamespaces = 
                                                        // dg.getOntologyNamespaces(dgkb, dgonto);
                                                        dg.getNamespaces(dgkb, dgonto, false);
                                                    ans = 
                                                        (ontoNamespaces.contains(namespace)
                                                         || ontoNamespaces.contains(term));

                                                }
                                            }
                                            boolMap.put(term, Boolean.toString(ans));
                                        }
                                        /*
                                          System.out.println("INFO isLegalForDisplay("
                                          + dg + ", "
                                          + term + ")");
                                          System.out.println("  ==> " + ans);
                                        */
                                    }
                                    catch (Exception ex) {
                                        ex.printStackTrace();
                                    }
                                    return ans;
                                }
                            };

                        spi.setDisplayFilter(df);

                        List<String> predicates = Arrays.asList("docGenDefaultNamespace",
                                                                "docGenDefaultPredicateNamespace",
                                                                "docGenLogoImageFile",
                                                                "docGenLogoImageMarkup",
                                                                "docGenStyleSheet",
                                                                "docGenTitleText",
                                                                "docGenFooterText");
                        for (String pred : predicates) {
                            String val = 
                                kb.getFirstTermViaAskWithRestriction(0,
                                                                     pred,
                                                                     1,
                                                                     ontology,
                                                                     2);
                            val = StringUtil.removeEnclosingQuotes(val);
                            val = StringUtil.removeQuoteEscapes(val);
                            if (StringUtil.isNonEmptyString(val)) {
                                if (pred.equals("docGenDefaultNamespace"))
                                    spi.setDefaultNamespace(val);
                                else if (pred.equals("docGenDefaultPredicateNamespace"))
                                    spi.setDefaultPredicateNamespace(val);
                                else if (pred.equals("docGenLogoImageFile"))
                                    spi.setDefaultImageFile(val);
                                else if (pred.equals("docGenLogoImageMarkup"))
                                    spi.setDefaultImageFileMarkup(val);
                                else if (pred.equals("docGenStyleSheet"))
                                    spi.setStyleSheet(val);
                                else if (pred.equals("docGenTitleText"))
                                    spi.setTitleText(val);
                                else if (pred.equals("docGenFooterText"))
                                    spi.setFooterText(val);
                            }
                        }

                        t2 = System.currentTimeMillis();
                        spi.setTopLevelTerms(spi.computeTopLevelTermsByNamespace(kb, ontology));
                        tltElapsedTime = (System.currentTimeMillis() - t2);

                        t2 = System.currentTimeMillis();
                        Set<String> formatTokens = new HashSet<String>();
                        List<String> ftterms = 
                            kb.getTermsViaAskWithRestriction(0, 
                                                             "docGenOutputFormat", 
                                                             1, 
                                                             ontology,
                                                             2);
                        for (String ftterm : ftterms) {
                            formatTokens.add(StringUtil.removeEnclosingQuotes(ftterm));
                        }

                        for (String token : fileTypes) {
                            String lcToken = token.toLowerCase();
                            formatTokens.add(lcToken);
                            File outdir = 
                                spi.makeOutputDir((kb.name + "_" + lcToken), 
                                                  StringUtil.getDateTime("yyyyMMdd"));
                            String outdirPath = outdir.getCanonicalPath();
                            if (token.equalsIgnoreCase("xsd")) {
                                spi.writeXsdFiles(kb, 
                                                  ontology, 
                                                  spi.getDefaultNamespace(),
                                                  outdirPath);
                                xsdElapsedTime = (System.currentTimeMillis() - t2);
                            }
                            else if (token.equalsIgnoreCase("tab")) {
                                if (spi.getCoreTerms().isEmpty()) {
                                    spi.computeTermRelevance(kb, ontology);
                                    relevanceElapsedTime = (System.currentTimeMillis() - t2);
                                    t2 = System.currentTimeMillis();
                                }
                                spi.generateTabularFocalTermFiles(kb, ontology, outdirPath);
                                tabElapsedTime = (System.currentTimeMillis() - t2);
                            }
                            else if (token.equalsIgnoreCase("dd") 
                                     || token.equalsIgnoreCase("dd2")) {
                                spi.setHtmlOutputDirectoryPath(outdirPath);
                                if (spi.getCoreTerms().isEmpty()) {
                                    spi.computeTermRelevance(kb, ontology);
                                    relevanceElapsedTime = (System.currentTimeMillis() - t2);
                                    t2 = System.currentTimeMillis();
                                }
                                spi.generateHTML(kb, 
                                                 spi.getDefaultNamespace(), 
                                                 true,
                                                 formatTokens);
                                ddElapsedTime = (System.currentTimeMillis() - t2);
                            }
                            String fname = null;
                            for (int j = 0; j < 2; j++) {
                                switch(j) { 
                                case 0 : fname = spi.getDefaultImageFile();
                                    break;
                                case 1 : fname = spi.getStyleSheet();
                                    break;
                                }
                                if (StringUtil.isNonEmptyString(fname)) {
                                    File kbDirFile = new File(KBmanager.getMgr().getPref("kbDir"));
                                    File infile = new File(kbDirFile, fname);
                                    File outfile = new File(outdir, fname);
                                    KBmanager.copyFile(infile, outfile);
                                }
                            }
                            t2 = System.currentTimeMillis();
                        }
                    }
                    // System.out.println("  basesToQWords == " + spi.basesToQWords);
                }
                ok = false;
            }
        }
        catch (Throwable th) {
            System.out.println(th.getMessage());
            th.printStackTrace();
        }

        System.out.println("EXIT DocGen.generateDocumentsFromKB(" + argstr + ")");
        System.out.println("  > "
                           + ((System.currentTimeMillis() - t1) / 1000.0)
                           + " seconds total elapsed time");
        System.out.println("    > " 
                           + (inputFileElapsedTime / 1000.0) 
                           + " seconds to process " + inputFileCanonicalPath);
        if (tltElapsedTime > 0L) {
            System.out.println("    > " 
                               + (tltElapsedTime / 1000.0) 
                               + " seconds to compute top-level terms");
        }
        if (xsdElapsedTime > 0L) {
            System.out.println("    > " 
                               + (xsdElapsedTime / 1000.0) 
                               + " seconds to generate XSD files");
        }
        if (ddElapsedTime > 0L) {
            System.out.println("    > " 
                               + (relevanceElapsedTime / 1000.0) 
                               + " seconds to compute term relevance");
            System.out.println("    > " 
                               + (ddElapsedTime / 1000.0) 
                               + " seconds to generate Data Dictionary files");
        }
        if (tabElapsedTime > 0L) {
            System.out.println("    > " 
                               + (tabElapsedTime / 1000.0) 
                               + " seconds to generate tabular display files");
        }
        return status;
    }

    /** *************************************************************
     * 
     */
    private void generateDocuments(String kbName, 
                                   String ontologyName,
                                   String inputDirectory,
                                   String outputDirectory) {
        try {

            System.out.println("ENTER DocGen.generateDocuments(\n"
                               + "  " + kbName + ",\n"
                               + "  " + ontologyName + ",\n"
                               + "  " + inputDirectory + ",\n"
                               + "  " + outputDirectory + ")");

            KBmanager mgr = KBmanager.getMgr();
            if (StringUtil.isNonEmptyString(inputDirectory)) {
                mgr.initializeOnce(inputDirectory);
            }
            kbName = kbName.intern();
            KB kb = mgr.getKB(kbName);
            if (kb == null) {
                File configFile = new File(mgr.getPref("kbDir"), 
                                           KBmanager.CONFIG_FILE);
                throw new Exception("The KB \"" + kbName + "\" does not exist."
                                    + "  Check " + configFile.getCanonicalPath() + ".");
            }
            File outdir = new File(mgr.getPref("baseDir"));
            if (StringUtil.isNonEmptyString(outputDirectory)) {
                outdir = new File(outputDirectory);
                if (!outdir.isDirectory()) {
                    try {
                        outdir.mkdirs();
                    }
                    catch (Exception oe) {
                        System.out.println(oe.getMessage());
                        oe.printStackTrace();
                    }
                    if (!outdir.isDirectory()) {
                        System.out.println("Cannot find or create "
                                           + outdir.getCanonicalPath());
                        outdir = new File(System.getProperty("user.dir"));
                    }
                }
            }
            this.setOutputParentDir(outdir);
            String ontoTerm = null;
            if (isInstanceOf(kb, ontologyName, "Ontology")) {
                ontoTerm = ontologyName;
            }
            else {
                List formulae = kb.ask("arg", 0, "termFormat");
                Formula f = null;
                String term = null;
                if (formulae != null) {
                    for (Iterator it = formulae.iterator(); it.hasNext();) {
                        f = (Formula) it.next();
                        if (f.getArgument(3).matches(".*" + ontologyName + ".*")) {
                            term = f.getArgument(2);
                            if (isInstanceOf(kb, term, "Ontology")) {
                                ontoTerm = term;
                                break;
                            }
                        }
                    }
                }
            }
            if (StringUtil.emptyString(ontoTerm)) {
                System.out.println(ontologyName + " may not be an Ontology.  Trying anyway.");
                ontoTerm = ontologyName;
            }
            this.setOntology(ontoTerm);
            String fileInst = kb.getFirstTermViaAskWithRestriction(0, "containsInformation",
                                                                   2, ontoTerm,
                                                                   1);
            if (StringUtil.emptyString(fileInst)) {
                throw new Exception("Cannot find a File instance for " 
                                    + ontoTerm
                                    + ".  Giving up.");
            }
            String dataFileName = kb.getFirstTermViaAskWithRestriction(0, "filename",
                                                                       2, fileInst,
                                                                       1);
            dataFileName = StringUtil.removeEnclosingQuotes(dataFileName);
            if (StringUtil.emptyString(dataFileName)) {
                throw new Exception("Cannot find a file name string for "
                                    + fileInst
                                    + ".  Giving up.");
            }
            File indir = new File(mgr.getPref("kbDir"));
            File dataFile = new File(indir, dataFileName);
            if (!dataFile.canRead()) {
                throw new Exception("Cannot read " 
                                    + dataFile.getCanonicalPath() 
                                    + ".  Giving up.");
            }
            long t1 = System.currentTimeMillis();
            String dataFileCanonPath = dataFile.getCanonicalPath();
            int suffIdx = dataFileCanonPath.lastIndexOf(".");
            String suff = ((suffIdx != -1)
                           ? dataFileCanonPath.substring(suffIdx)
                           : "");
            List rows = null;
            if (suff.equalsIgnoreCase(".csv")) {
                rows = DB.readSpreadsheet(dataFileCanonPath, Arrays.asList("u,", "U,", "$,"));
            }
            else if (suff.equalsIgnoreCase(".dif")) {
                rows = DB.readDataInterchangeFormatFile(dataFileCanonPath);
            }
            String kifFileName = dataFileCanonPath;
            int endIdx = kifFileName.lastIndexOf(".");
            kifFileName = (kifFileName.substring(0, endIdx) + ".kif");
            String defaultNamespace = 
                kb.getFirstTermViaAskWithRestriction(0, 
                                                     "docGenDefaultNamespace",
                                                     1, 
                                                     ontoTerm,
                                                     2);
            if (StringUtil.emptyString(defaultNamespace)) {
                throw new Exception("Cannot find the default namespace for " 
                                    + ontoTerm
                                    + ".  Giving up.");
            }
            processSpreadsheet(kb, rows, getOntology(), kifFileName);
            System.out.println("  Time: " 
                               + ((System.currentTimeMillis() - t1) / 1000.0) 
                               + " seconds to process "
                               + dataFileName);

            kb.addConstituent(kifFileName);
            Set<String> formatTokens = new HashSet<String>();
            List<String> ftterms = 
                kb.getTermsViaAskWithRestriction(0, 
                                                 "docGenOutputFormat", 
                                                 1, 
                                                 ontology,
                                                 2);
            for (String ftterm : ftterms) {
                formatTokens.add(StringUtil.removeEnclosingQuotes(ftterm));
            }
            if (formatTokens.isEmpty()) 
                formatTokens.add(F_DD);

            generateHTML(kb, 
                         defaultNamespace, 
                         true, 
                         formatTokens);
        }
        catch (Exception ex) {
            System.out.println(ex.getMessage());
            ex.printStackTrace();
        }

        System.out.println("EXIT DocGen.generateDocuments(\n"
                           + "  " + kbName + ",\n"
                           + "  " + ontologyName + ",\n"
                           + "  " + inputDirectory + ",\n"
                           + "  " + outputDirectory + ")");

        return;
    }

    /** *************************************************************
     * Create an HTML page that lists information about a particular
     * composite term, which is a representation of an XML
     * structure.
     *
     * @param alphaList a TreeMap of TreeMaps of ArrayLists.  @see
     *                   createAlphaList()
     */
    public String createCompositePage(KB kb, 
                                      String kbHref, 
                                      String term, 
                                      TreeMap alphaList, 
                                      int limit, 
                                      String language,
                                      Set formatTokens) {

        /*
          System.out.println("ENTER " + this.toString() + ".createCompositePage("
          + kb.name + ", "
          + kbHref + ", "
          + term + ", "
          + "[alphaList with " + alphaList.size() + " entries], "
          + limit + ", "
          + language + ", "
          + formatTokens + ")");
        */

        String markup = "";

        try {
            if (StringUtil.isNonEmptyString(term)) {

                if ((formatTokens != null) && formatTokens.contains(F_DD2)) {
                    markup = createCompositePage(kb, kbHref, term, alphaList, limit, language);
                }
                else {
                    StringBuilder result = new StringBuilder();
                    StringBuilder sb1 = new StringBuilder();

                    if (StringUtil.isNonEmptyString(kbHref)) 
                        result.append(generateDynamicTOCHeader(kbHref));
                    else
                        result.append(generateTocHeader(kb, 
                                                        alphaList, 
                                                        INDEX_FILE_NAME
                                                        ));
                    result.append("<table width=\"100%\">");
                    result.append(StringUtil.getLineSeparator());
                    result.append("  <tr bgcolor=\"#DDDDDD\">");
                    result.append(StringUtil.getLineSeparator());
                    result.append("    <td valign=\"top\" class=\"title\">");
                    result.append(StringUtil.getLineSeparator());
                    result.append("      ");
                    result.append(showTermName(kb,term,language,true));
                    result.append(StringUtil.getLineSeparator());
                    result.append("    </td>");
                    result.append(StringUtil.getLineSeparator());
                    result.append("  </tr>");
                    result.append(StringUtil.getLineSeparator());

                    String relevance = createTermRelevanceNotice(kb, kbHref, term, language);
                    if (StringUtil.isNonEmptyString(relevance)) {
                        result.append("  <tr bgcolor=\"#DDDDDD\">");
                        result.append(StringUtil.getLineSeparator());
                        result.append("    <td valign=\"top\" class=\"cell\">");
                        result.append(StringUtil.getLineSeparator());
                        result.append(relevance);
                        result.append(StringUtil.getLineSeparator());
                        result.append("    </td>");
                        result.append(StringUtil.getLineSeparator());
                        result.append("  </tr>");
                        result.append(StringUtil.getLineSeparator());
                    }

                    result.append(createDocs(kb,kbHref,term,language));
                    result.append("</table>");
                    result.append(StringUtil.getLineSeparator());
                    result.append("<table>");
                    result.append(StringUtil.getLineSeparator());
                    result.append(createDisplayNames(kb, kbHref, term, formatTokens));
                    result.append(StringUtil.getLineSeparator());
                    result.append(createSynonyms(kb, kbHref, term, formatTokens));
                    result.append(StringUtil.getLineSeparator());

                    ArrayList superComposites = findContainingComposites(kb, term); 
                    Collections.sort(superComposites, String.CASE_INSENSITIVE_ORDER);

                    sb1.append(createHasSameComponents(kb, kbHref, term, language));
                    if ((sb1.length() > 0) 
                        || !superComposites.isEmpty()
                        || hasSubComponents(kb, term)) {

                        result.append("<tr class=\"title_cell\">");
                        result.append(StringUtil.getLineSeparator());
                        result.append("  <td valign=\"top\" class=\"label\">Component Structure</td>");
                        result.append(StringUtil.getLineSeparator());
                        result.append("  <td valign=\"top\" colspan=\"4\"></td>");
                        result.append(StringUtil.getLineSeparator());
                        result.append("</tr>");
                        result.append(StringUtil.getLineSeparator());

                        if (sb1.length() > 0) {
                            result.append(sb1);
                            sb1.setLength(0);
                        }

                        if (hasSubComponents(kb, term)) {
                            result.append("<tr>");
                            result.append(StringUtil.getLineSeparator());
                            result.append("  <td valign=\"top\" class=\"label\">Components</td>");
                            result.append(StringUtil.getLineSeparator());
                            result.append("  <td valign=\"top\" class=\"title_cell\">Name</td>");
                            result.append(StringUtil.getLineSeparator());
                            result.append("  <td valign=\"top\" class=\"title_cell\">");
                            result.append(StringUtil.getLineSeparator());
                            result.append("    Description of Element Role");
                            result.append(StringUtil.getLineSeparator());
                            result.append("  </td>");
                            result.append(StringUtil.getLineSeparator());
                            result.append("  <td valign=\"top\" class=\"title_cell\">Cardinality</td>");
                            result.append(StringUtil.getLineSeparator());
                            result.append("  <td valign=\"top\" class=\"title_cell\">Data Type</td>");
                            result.append(StringUtil.getLineSeparator());
                            result.append("</tr>");
                            result.append(StringUtil.getLineSeparator());
                
                            ArrayList attrs = new ArrayList();
                            ArrayList elems = new ArrayList();

                            // If there are shared components, add them first.
                            ArrayList accumulator = 
                                new ArrayList(getSyntacticExtensionTerms(kb, term, 2, false));
                            ArrayList sharesComponentsWith = new ArrayList();
                    
                            // System.out.println("  term == " + term);
                            // System.out.println("  accumulator == " + accumulator);

                            while (!accumulator.isEmpty()) {
                                sharesComponentsWith.clear();
                                sharesComponentsWith.addAll(accumulator);
                                accumulator.clear();
                                String nextTerm = null;
                                for (Iterator it = sharesComponentsWith.iterator(); it.hasNext();) {
                                    nextTerm = (String) it.next();
                                    ArrayList nextPair = createCompositeRecurse(kb, nextTerm, false, 0);
                                    ArrayList nextAttrs = ((ArrayList) nextPair.get(0));
                                    ArrayList nextElems = ((ArrayList) nextPair.get(1));
                                    attrs.addAll(0, nextAttrs);
                                    if (!nextElems.isEmpty()) {
                                        nextElems.remove(0);
                                        elems.addAll(0, nextElems);
                                    }
                                    accumulator.addAll(getSyntacticExtensionTerms(kb, 
                                                                                  nextTerm, 
                                                                                  2, 
                                                                                  false));

                                    // System.out.println("  nextTerm == " + nextTerm);
                                    // System.out.println("  accumulator == " + accumulator);

                                }
                            }

                            // Now add the components that pertain to only this
                            // term.  
                            ArrayList localPair = createCompositeRecurse(kb, term, false, 0);
                            // No need to show the composite itself.
                            ArrayList localAttrs = ((ArrayList) localPair.get(0));
                            ArrayList localElems = ((ArrayList) localPair.get(1));
                            attrs.addAll(localAttrs);
                            if (!localElems.isEmpty()) {
                                localElems.remove(0);
                                elems.addAll(localElems);
                            }

                            ArrayList hier = new ArrayList(attrs);
                            hier.addAll(elems);
                            result.append(formatCompositeHierarchy(kb, kbHref, hier, language));
                        }

                        if (!superComposites.isEmpty()) {
                            Collections.sort(superComposites, String.CASE_INSENSITIVE_ORDER);
                            result.append("<tr>");
                            result.append(StringUtil.getLineSeparator());
                            result.append("  <td valign=\"top\" class=\"label\">");
                            result.append(StringUtil.getLineSeparator());
                            result.append("    Is Member of Composites");
                            result.append(StringUtil.getLineSeparator());
                            result.append("  </td>");
                            result.append(StringUtil.getLineSeparator());
                            result.append("  <td valign=\"top\" class=\"title_cell\">");
                            result.append(StringUtil.getLineSeparator());
                            result.append("    Composite Name");
                            result.append(StringUtil.getLineSeparator());
                            result.append("  </td>");
                            result.append(StringUtil.getLineSeparator());
                            result.append("  <td valign=\"top\" class=\"title_cell\">");
                            result.append(StringUtil.getLineSeparator());
                            result.append("    Description of Element Role");
                            result.append(StringUtil.getLineSeparator());
                            result.append("  </td>");
                            result.append(StringUtil.getLineSeparator());
                            result.append("  <td valign=\"top\" class=\"title_cell\">");
                            result.append(StringUtil.getLineSeparator());
                            result.append("    Cardinality");
                            result.append("  </td>");
                            result.append(StringUtil.getLineSeparator());
                            result.append("  <td valign=\"top\" class=\"title_cell\"> &nbsp; </td>");
                            result.append(StringUtil.getLineSeparator());
                            result.append("</tr>");
                            result.append(StringUtil.getLineSeparator());

                            result.append(formatContainingComposites(kb,
                                                                     kbHref,
                                                                     superComposites,
                                                                     term,
                                                                     language));
                        }
                    }

                    sb1.append(createBelongsToClass(kb, kbHref, term, language));
                    sb1.append(createUsingSameComponents(kb, kbHref, term, language));
                    if (sb1.length() > 0) {
                        result.append("<tr class=\"title_cell\">");
                        result.append(StringUtil.getLineSeparator());
                        result.append("  <td valign=\"top\" class=\"label\">");
                        result.append(StringUtil.getLineSeparator());
                        result.append("    Relationships");
                        result.append(StringUtil.getLineSeparator());
                        result.append("  </td>");
                        result.append(StringUtil.getLineSeparator());
                        result.append("  <td></td><td></td><td></td><td></td>");
                        result.append(StringUtil.getLineSeparator());
                        result.append("</tr>");
                        result.append(StringUtil.getLineSeparator());

                        result.append(sb1);

                        sb1.setLength(0);
                    }
                    result.append("</table>");
                    result.append(StringUtil.getLineSeparator());

                    result.append(generateHtmlFooter(""));

                    result.append("  </body>");
                    result.append(StringUtil.getLineSeparator());
                    result.append("</html>"); 
                    result.append(StringUtil.getLineSeparator());

                    markup = result.toString();
                }
            }
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }

        /*
          System.out.println("EXIT " + this.toString() + ".createCompositePage("
          + kb.name + ", "
          + kbHref + ", "
          + term + ", "
          + "[alphaList with " + alphaList.size() + " entries], "
          + limit + ", "
          + language + ", "
          + formatTokens + ")");
          System.out.println("  > markup == " + markup.length() + " chars");
        */

        return markup;
    }

    /** *************************************************************
     * Create an HTML page that lists information about a particular
     * composite term, which is a representation of an XML
     * structure.
     *
     * @param alphaList a TreeMap of TreeMaps of ArrayLists.
     * @see createAlphaList()
     * 
     */
    public String createCompositePage(KB kb,   // for Data Dictionary format #2
                                      String kbHref, 
                                      String term, 
                                      TreeMap alphaList, 
                                      int limit, 
                                      String language) {
        /*
          long t1 = System.currentTimeMillis();
          System.out.println("ENTER DocGen.createCompositePage("
          + kb.name + ", "
          + kbHref + ", "
          + term + ", "
          + "[map with " + alphaList.size() + " entries], "
          + limit + ", "
          + language + ")");                          
        */
        String markup = "";

        try {
            if (StringUtil.isNonEmptyString(term)) {
                StringBuilder result = new StringBuilder();
                StringBuilder sb1 = new StringBuilder();

                if (StringUtil.isNonEmptyString(kbHref)) 
                    result.append(generateDynamicTOCHeader(kbHref));
                else
                    result.append(generateTocHeader(kb, 
                                                    alphaList, 
                                                    INDEX_FILE_NAME
                                                    ));
                result.append("<table width=\"100%\">");
                result.append(StringUtil.getLineSeparator());
                result.append("  <tr bgcolor=\"#DDDDDD\">");
                result.append(StringUtil.getLineSeparator());
                result.append("    <td valign=\"top\" class=\"title\">");
                result.append(StringUtil.getLineSeparator());
                result.append("      ");
                result.append(showTermName(kb,term,language,true));
                result.append(StringUtil.getLineSeparator());
                result.append("      ");
                result.append(StringUtil.getLineSeparator());
                result.append("    </td>");
                result.append(StringUtil.getLineSeparator());
                result.append("  </tr>");
                result.append(StringUtil.getLineSeparator());
                result.append(createDocs(kb,kbHref,term,language));
                result.append("</table>");
                result.append(StringUtil.getLineSeparator());
                result.append("<table>");
                result.append(StringUtil.getLineSeparator());
                result.append(createDisplayNames(kb, kbHref, term));
                result.append(StringUtil.getLineSeparator());
                result.append(createSynonyms(kb, kbHref, term));
                result.append(StringUtil.getLineSeparator());

                sb1.append(createRelations(kb, kbHref, term, language));
                sb1.append(StringUtil.getLineSeparator());

                if (sb1.length() > 0) {

                    result.append(sb1);                    

                    sb1 = new StringBuilder();
                }
                result.append("</table>");
                result.append(StringUtil.getLineSeparator());

                result.append(generateHtmlFooter(""));

                result.append("  </body>");
                result.append(StringUtil.getLineSeparator());
                result.append("</html>"); 
                result.append(StringUtil.getLineSeparator());

                markup = result.toString();
            }
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
        /*
          System.out.println("EXIT DocGen.createCompositePage("
          + kb.name + ", "
          + kbHref + ", "
          + term + ", "
          + "[map with " + alphaList.size() + " entries], "
          + limit + ", "
          + language + ")");
          System.out.println("  > "
          + ((System.currentTimeMillis() - t1) / 1000.0)
          + " seconds elapsed time");
        */
        return markup;
    }

    /** *************************************************************
     * Create an HTML page that lists information about a particular term,
     * with a limit on how many statements of each type should be
     * displayed.
     * 
     * @param alphaList a TreeMap of TreeMaps of ArrayLists.
     *                   @see createAlphaList()
     */
    public String createPage(KB kb, 
                             String kbHref, 
                             String term, 
                             TreeMap alphaList,
                             int limit, 
                             String language,
                             Set formatTokens) {

        /*
          System.out.println("ENTER " + this.toString() + ".createPage("
          + kb.name + ", "
          + kbHref + ", "
          + term + ", "
          + "[alphaList with " + alphaList.size() + " entries], "
          + limit + ", "
          + language + ", "
          + formatTokens + ")");
        */

        String output = "";
        try {
            if ((formatTokens != null) && formatTokens.contains(F_DD2)) {
                output = createPage(kb, kbHref, term, alphaList, limit, language);
            }
            else {
                StringBuilder result = new StringBuilder();
                StringBuilder sb1 = new StringBuilder();
                StringBuilder sb2 = new StringBuilder();

                if (StringUtil.isNonEmptyString(kbHref)) {
                    if (!kbHref.endsWith("&term=")) {
                        kbHref += "&term=";
                    }
                    result.append(generateDynamicTOCHeader(kbHref));
                }
                else {
                    result.append(generateTocHeader(kb, 
                                                    alphaList, 
                                                    INDEX_FILE_NAME
                                                    ));
                }
                result.append("<table width=\"100%\">");
                result.append(StringUtil.getLineSeparator());
                result.append("  <tr bgcolor=\"#DDDDDD\">");
                result.append(StringUtil.getLineSeparator());
                result.append("    <td valign=\"top\" class=\"title\">");
                result.append(StringUtil.getLineSeparator());
                result.append("      ");
                result.append(showTermName(kb,term,language,true));
                result.append(StringUtil.getLineSeparator());
                result.append("      ");
                result.append(StringUtil.getLineSeparator());
                result.append("    </td>");
                result.append(StringUtil.getLineSeparator());
                result.append("  </tr>"); 
                result.append(StringUtil.getLineSeparator());

                String relevance = createTermRelevanceNotice(kb, kbHref, term, language);
                if (StringUtil.isNonEmptyString(relevance)) {
                    result.append("  <tr bgcolor=\"#DDDDDD\">");
                    result.append(StringUtil.getLineSeparator());
                    result.append("    <td valign=\"top\" class=\"cell\">");
                    result.append(StringUtil.getLineSeparator());
                    result.append(relevance);
                    result.append(StringUtil.getLineSeparator());
                    result.append("    </td>");
                    result.append(StringUtil.getLineSeparator());
                    result.append("  </tr>");
                    result.append(StringUtil.getLineSeparator());
                }

                result.append(createDocs(kb, kbHref, term, language));
                result.append(StringUtil.getLineSeparator());
                result.append("</table>");
                result.append(StringUtil.getLineSeparator());
                result.append("<table width=\"100%\">");
                result.append(StringUtil.getLineSeparator());
                result.append(createDisplayNames(kb, kbHref, term, formatTokens));
                result.append(StringUtil.getLineSeparator());
                result.append(createSynonyms(kb, kbHref, term, formatTokens));
                result.append(StringUtil.getLineSeparator());
                result.append(createComments(kb, kbHref, term, language));
                result.append(StringUtil.getLineSeparator());

                Set<String> parents = new HashSet<String>();
                sb1.append(createParents(kb, kbHref, term, language, parents));
                sb1.append(StringUtil.getLineSeparator());
                sb2.append(createChildren(kb, kbHref, term, language));
                sb2.append(StringUtil.getLineSeparator());

                if ((sb1.length() > 0) || (sb2.length() > 0)) {
                    result.append("<tr class=\"title_cell\">");
                    result.append(StringUtil.getLineSeparator());
                    result.append("  <td valign=\"top\" class=\"label\">");
                    result.append(StringUtil.getLineSeparator());
                    result.append("    Relationships");
                    result.append(StringUtil.getLineSeparator());
                    result.append("  </td>");
                    result.append(StringUtil.getLineSeparator());
                    result.append("  <td>&nbsp;</td>");
                    result.append(StringUtil.getLineSeparator());
                    result.append("  <td>&nbsp;</td>");
                    result.append(StringUtil.getLineSeparator());
                    result.append("  <td>&nbsp;</td>");
                    result.append(StringUtil.getLineSeparator());
                    result.append("</tr>");
                    result.append(StringUtil.getLineSeparator());

                    // Parents
                    result.append(sb1.toString());
                    sb1.setLength(0);

                    // Children
                    result.append(sb2.toString());
                    sb2.setLength(0);
                }

                ArrayList superComposites = findContainingComposites(kb, term); 
                Collections.sort(superComposites, String.CASE_INSENSITIVE_ORDER);

                result.append(createInstances(kb, kbHref, term, language, superComposites));
                result.append(StringUtil.getLineSeparator());

                result.append(createRelations(kb, kbHref, term, language, formatTokens));
                result.append(StringUtil.getLineSeparator());

                result.append(createUsingSameComponents(kb, kbHref, term, language));
                result.append(StringUtil.getLineSeparator());

                result.append(createBelongsToClass(kb, kbHref, term, language, parents));
                result.append(StringUtil.getLineSeparator());

                if (!superComposites.isEmpty()) {
                    result.append("<tr>");
                    result.append(StringUtil.getLineSeparator());
                    result.append("  <td valign=\"top\" class=\"label\">");
                    result.append(StringUtil.getLineSeparator());
                    result.append("    Is Member of Composites");
                    result.append(StringUtil.getLineSeparator());
                    result.append("  </td>");
                    result.append(StringUtil.getLineSeparator());
                    result.append("  <td valign=\"top\" class=\"title_cell\">");
                    result.append(StringUtil.getLineSeparator());
                    result.append("    Composite Name");
                    result.append(StringUtil.getLineSeparator());
                    result.append("  </td>");
                    result.append(StringUtil.getLineSeparator());
                    result.append("  <td valign=\"top\" class=\"title_cell\">");
                    result.append(StringUtil.getLineSeparator());
                    result.append("    Description of Element Role");
                    result.append(StringUtil.getLineSeparator());
                    result.append("  </td>");
                    result.append(StringUtil.getLineSeparator());
                    result.append("  <td valign=\"top\" class=\"title_cell\">");
                    result.append(StringUtil.getLineSeparator());
                    result.append("    Cardinality");
                    result.append(StringUtil.getLineSeparator());
                    result.append("  </td>");
                    result.append(StringUtil.getLineSeparator());
                    result.append("  <td> &nbsp; </td>");
                    result.append(StringUtil.getLineSeparator());
                    result.append("</tr>");
                    result.append(StringUtil.getLineSeparator());

                    result.append(formatContainingComposites(kb,
                                                             kbHref,
                                                             superComposites,
                                                             term,
                                                             language));
                    result.append(StringUtil.getLineSeparator());
                }

                result.append("</table>");
                result.append(StringUtil.getLineSeparator());
                result.append(generateHtmlFooter(""));
                result.append(StringUtil.getLineSeparator());
                result.append("  </body>");
                result.append(StringUtil.getLineSeparator());
                result.append("</html>");
                result.append(StringUtil.getLineSeparator());

                // result.append(createAllStatements(kb,kbHref,term,limit));

                output = result.toString();

                /*
                  System.out.println("INFO createPage(" 
                  + kb.name + ", "
                  + kbHref + ", "
                  + term + ", "
                  + "[" + alphaList.size() + " pairs], "
                  + limit + ", "
                  + language + ")");
                  System.out.println("  ==> " + output);
                */
            }
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }

        /*
          System.out.println("EXIT " + this.toString() + ".createPage("
          + kb.name + ", "
          + kbHref + ", "
          + term + ", "
          + "[alphaList with " + alphaList.size() + " entries], "
          + limit + ", "
          + language + ", "
          + formatTokens + ")");
          System.out.println("  > output == " + output.length() + " chars");
        */

        return output;
    }

    /** *************************************************************
     * Create an HTML page that lists information about a particular term,
     * with a limit on how many statements of each type should be
     * displayed.
     * 
     * @param alphaList a TreeMap of TreeMaps of ArrayLists.
     *                   @see createAlphaList()
     */
    public String createPage(KB kb,   // for Data Dictionary format #2
                             String kbHref, 
                             String term, 
                             TreeMap alphaList,
                             int limit, 
                             String language) {
        /*
          long t1 = System.currentTimeMillis();
          System.out.println("ENTER DocGen.createPage("
          + kb.name + ", "
          + kbHref + ", "
          + term + ", "
          + "[map with " + alphaList.size() + " entries], "
          + limit + ", "
          + language + ")");                          
        */
        String output = "";
        try {
            StringBuilder result = new StringBuilder();
            StringBuilder sb1 = new StringBuilder();
            StringBuilder sb2 = new StringBuilder();

            if (StringUtil.isNonEmptyString(kbHref)) {
                if (!kbHref.endsWith("&term=")) {
                    kbHref += "&term=";
                }
                result.append(generateDynamicTOCHeader(kbHref));
            }
            else {
                result.append(generateTocHeader(kb, 
                                                alphaList, 
                                                INDEX_FILE_NAME
                                                ));
            }
            result.append("<table width=\"100%\">");
            result.append(StringUtil.getLineSeparator());
            result.append("  <tr bgcolor=\"#DDDDDD\">");
            result.append(StringUtil.getLineSeparator());
            result.append("    <td valign=\"top\" class=\"title\">");
            result.append(StringUtil.getLineSeparator());
            result.append("      ");
            result.append(showTermName(kb,term,language,true));
            result.append(StringUtil.getLineSeparator());
            result.append("      ");
            result.append(StringUtil.getLineSeparator());
            result.append("    </td>");
            result.append(StringUtil.getLineSeparator());
            result.append("  </tr>"); 
            result.append(StringUtil.getLineSeparator());

            result.append(createDocs(kb, kbHref, term, language));
            result.append(StringUtil.getLineSeparator());
            result.append("</table>");
            result.append(StringUtil.getLineSeparator());
            result.append("<table width=\"100%\">");
            result.append(StringUtil.getLineSeparator());
            result.append(createDisplayNames(kb, kbHref, term));
            result.append(StringUtil.getLineSeparator());
            result.append(createSynonyms(kb, kbHref, term));
            result.append(StringUtil.getLineSeparator());
            result.append(createComments(kb, kbHref, term, language));
            result.append(StringUtil.getLineSeparator());

            result.append(createRelations(kb, kbHref, term, language));
            result.append(StringUtil.getLineSeparator());

            result.append("</table>");
            result.append(StringUtil.getLineSeparator());
            result.append(generateHtmlFooter(""));
            result.append(StringUtil.getLineSeparator());
            result.append("  </body>");
            result.append(StringUtil.getLineSeparator());
            result.append("</html>");
            result.append(StringUtil.getLineSeparator());

            output = result.toString();
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
        /*
          System.out.println("EXIT DocGen.createPage("
          + kb.name + ", "
          + kbHref + ", "
          + term + ", "
          + "[map with " + alphaList.size() + " entries], "
          + limit + ", "
          + language + ")");
          System.out.println("  > "
          + ((System.currentTimeMillis() - t1) / 1000.0)
          + " seconds elapsed time");
        */
        return output;
    }

    /** *************************************************************
     * Returns an ArrayList of namespace delimiter Strings gathered
     * from all loaded KBs, obtained by collecting statements formed
     * with the predicate docGenNamespaceDelimiter.
     *
     * @return An ArrayList<String> of namespace delimiter tokens,
     * which could be empty
     */
    public ArrayList<String> getAllNamespaceDelimiters() {
        ArrayList<String> ans = new ArrayList<String>();
        try {
            Set<String> reduce = new HashSet<String>();
            Map kbs = KBmanager.getMgr().kbs;
            if (!kbs.isEmpty()) {
                KB kb = null;
                for (Iterator it = kbs.values().iterator(); it.hasNext();) {
                    kb = (KB) it.next();
                    reduce.addAll(kb.getTermsViaAsk(0,"docGenNamespaceDelimiter",2));
                }
            }
            reduce.add(StringUtil.getW3cNamespaceDelimiter());
            reduce.add(StringUtil.getKifNamespaceDelimiter());
            ans.addAll(reduce);
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
        return ans;
    }

    /** *************************************************************
     * The keys of this Map are SUO-KIF terms.  The values are Sets of
     * SUO-KIF terms.  Each key term is "contained" by, or somehow
     * central to the definition of, the terms in the corresponding
     * Set.
     */
    private HashMap coreTerms = new HashMap();

    public HashMap getCoreTerms() {
        return coreTerms;
    }

    /** *************************************************************
     * The keys of this Map are SUO-KIF terms.  The values are Sets of
     * SUO-KIF terms.  Each key term is part of the general vocabulary
     * framework that defines other terms considered more central or
     * "core" to the definitions of the terms in the corresponding
     * Set.  In other words, each key is only indirectly and
     * syntactically relevant to the definition of the terms in the
     * corresponding Set.
     */
    private HashMap frameworkTerms = new HashMap();

    public HashMap getFrameworkTerms() {
        return frameworkTerms;
    }

    /** *************************************************************
     * The keys of this Map are SUO-KIF terms.  The values are Sets of
     * SUO-KIF terms.  Each key term is part of the supporting
     * vocabulary that defines other terms considered more central or
     * "core" to the definitions of the terms in the corresponding
     * Set.  In other words, each key is only indirectly semantically
     * relevant to the definition of the terms in the corresponding
     * Set.
     */
    private HashMap supportingTerms = new HashMap();

    public HashMap getSupportingTerms() {
        return supportingTerms;
    }

    /** *************************************************************
     * Each key in this Map is a top-level focalTerm in this KB and
     * ontology.  The corresponding value is a String representation
     * of the int value that indicates the computed maximum "depth",
     * or number of nodes in the longest path, along
     * syntacticSubordinate from the focalTerm.
     */
    private HashMap maxDepthFromFocalTerm = new HashMap();

    public HashMap getMaxDepthFromFocalTerm() {
        return maxDepthFromFocalTerm;
    }

    /** *************************************************************
     * Returns a String of HTML markup for the start of a document,
     * using title as the document title String.
     *
     * @param title A String to be used as the document title
     *
     * @return A String of HTML markup encoding the start of an HTML
     * document
     */
    public String generateHtmlDocStart(String title) {

        String result = "";

        try {

            String cssf = getStyleSheet();
            cssf = StringUtil.removeEnclosingQuotes(cssf);

            String docTitle = title;
            if (!StringUtil.isNonEmptyString(docTitle)) {
                docTitle = getTitleText();
            }
            docTitle = StringUtil.removeEnclosingQuotes(docTitle);
            docTitle = StringUtil.removeQuoteEscapes(docTitle);

            StringBuilder sb = new StringBuilder();

            sb.append("<html>");
            sb.append(StringUtil.getLineSeparator());
            sb.append("  <head>");
            sb.append(StringUtil.getLineSeparator());
            sb.append("    <meta http-equiv=\"Content-Type\" ");
            sb.append("content=\"text/html; charset=utf-8\">");
            sb.append(StringUtil.getLineSeparator());
            if (StringUtil.isNonEmptyString(cssf)) {
                sb.append("    <link rel=\"stylesheet\" type=\"text/css\" href=\"");
                sb.append(cssf);
                sb.append("\">");
                sb.append(StringUtil.getLineSeparator());
            }
            if (StringUtil.isNonEmptyString(docTitle)) {
                sb.append("    <title>");
                sb.append(docTitle);
                sb.append("</title>");
                sb.append(StringUtil.getLineSeparator());
            }
            sb.append("  </head>");
            sb.append(StringUtil.getLineSeparator());
            sb.append("  <body>");
            sb.append(StringUtil.getLineSeparator());
            result = sb.toString();
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
        return result;
    }

    /** *************************************************************
     * Returns a String of HTML markup encoding the footer section of
     * an HTML document, and using footerText as the text to be
     * displayed at the bottom of the page.
     *
     * @param footerText The text String to be diplayed at the bottom
     * of an HTML document
     *
     * @return A String of HTML markup
     */
    protected String generateHtmlFooter(String footerText) {
        String result = "";
        try {
            String text = footerText;
            if (!StringUtil.isNonEmptyString(text)) {
                text = getFooterText();
            }
            text = StringUtil.removeEnclosingQuotes(text);
            text = StringUtil.removeQuoteEscapes(text);

            StringBuilder sb = new StringBuilder();

            sb.append("<table width=\"100%\">" );
            sb.append(StringUtil.getLineSeparator());
            sb.append("  <tr class=\"title\">");
            sb.append(StringUtil.getLineSeparator());
            sb.append("    <td>");
            // sb.append(StringUtil.getLineSeparator());
            sb.append(text);
            // sb.append(StringUtil.getLineSeparator());
            sb.append("    </td>");
            sb.append(StringUtil.getLineSeparator());
            sb.append("  </tr>");
            sb.append(StringUtil.getLineSeparator());
            sb.append("</table>");
            sb.append(StringUtil.getLineSeparator());
            result = sb.toString();
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
        return result;
    }

    /** **************************************************************
     * Returns true if statements that include term and occur in the
     * kb and ontology associated with this DocGen object may be
     * displayed or output (at all, in any form).
     *
     * @return true or false
     */
    protected boolean isLegalForDisplay(String term) {
        boolean ans = StringUtil.isNonEmptyString(term);
        try {
            DisplayFilter df = getDisplayFilter();
            if (ans && (df != null)) {
                ans = df.isLegalForDisplay(this, term);
            }
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
        return ans;
    }

    /** **************************************************************
     * Filters the List of Strings seq, removing all items that match
     * the regular expression pattern regex.
     *
     * @param seq A List of Strings
     *
     * @param regex A regular expression pattern String that will be
     * matched against the Strings in seq
     *
     */
    protected void removeByPattern(List seq, String regex) {
        try {
            if ((seq instanceof List) && StringUtil.isNonEmptyString(regex)) {
                Object obj = null;
                for (ListIterator it = seq.listIterator(); it.hasNext();) {
                    obj = (Object) it.next();
                    if (obj.toString().matches(regex)) {
                        it.remove();
                    }
                }
            }
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
        return;
    }

    /** **************************************************************
     * Filters the List of Strings seq, retaining only items that match
     * the regular expression pattern regex.
     *
     * @param seq A List of Strings
     *
     * @param regex A regular expression pattern String that will be
     * matched against the Strings in seq
     *
     */
    protected void retainByPattern(List seq, String regex) {
        try {
            if ((seq instanceof List) && StringUtil.isNonEmptyString(regex)) {
                Object obj = null;
                for (ListIterator it = seq.listIterator(); it.hasNext();) {
                    obj = (Object) it.next();
                    if (!obj.toString().matches(regex)) {
                        it.remove();
                    }
                }
            }
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
        return;
    }

    /** **************************************************************
     * Returns a List of all SUO-KIF terms denoting those namespaces
     * containing terms that are defined in, or occur in, statements
     * in ontology.  An association (correspondence) between a
     * namespace and an ontology is represented by a statement formed
     * with the SUO-KIF predicate ontologyNamespace.
     *
     * @param kb The KB in which ontologyNamespace statements will be
     * sought
     *
     * @param ontology The name of the ontology that will be checked
     *
     * @return An ArrayList of SUO-KIF terms that denote namespaces
     * and occur in statements formed with the predicate
     * ontologyNamespace
     */
    protected ArrayList<String> getOntologyNamespaces(KB kb, String ontology) {
        ArrayList<String> ans = new ArrayList<String>();
        try {
            ans.addAll(new HashSet<String>(kb.getTermsViaAskWithRestriction(0, 
                                                                            "ontologyNamespace", 
                                                                            1, 
                                                                            ontology,
                                                                            2)));
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
        return ans;
    }
                
    /** **************************************************************
     * Returns a List of all Strings used as namespace delimiters in
     * terms defined or referred to in ontology, as specified by
     * statements formed with the SUO-KIF predicate
     * docGenNamespaceDelimiter.
     *
     * @param kb The KB that will be checked to find statements formed
     * with docGenNamespaceDelimiter
     *
     * @param ontology The name of the ontology that will be checked
     *
     * @return An ArrayList of tokens (Strings) that are used as
     * delimiters between a qualified term name and the namespace
     * prefix that qualifies the term
     */
    protected ArrayList<String> getNamespaceDelimiters(KB kb, String ontology) {
        ArrayList<String> ans = new ArrayList<String>();
        try {
            List<String> delims = kb.getTermsViaAskWithRestriction(0, 
                                                                   "docGenNamespaceDelimiter", 
                                                                   1, 
                                                                   ontology,
                                                                   2);
            ans.addAll(new HashSet<String>(delims));
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
        return ans;
    }

    /** *************************************************************
     * A List of currently known namespace prefixes.
     */
    private ArrayList<String> namespacePrefixes = new ArrayList<String>();

    /** **************************************************************
     * Returns an ArrayList of all known namespace prefixes sorted by
     * length, from longest to shortest.
     *
     * @return A List of all known namespace prefixes
     */
    public ArrayList<String> getNamespacePrefixes() {
        try {
            synchronized (namespacePrefixes) {
                if (namespacePrefixes.isEmpty()) {
                    Set<String> delims = new HashSet<String>(getAllNamespaceDelimiters());
                    delims.addAll(Arrays.asList(StringUtil.getKifNamespaceDelimiter(),
                                                StringUtil.getW3cNamespaceDelimiter(),
                                                StringUtil.getSafeNamespaceDelimiter()));
                    ArrayList<String> nsprefs = new ArrayList<String>();
                    for (String delim : delims) {
                        nsprefs.add("ns" + delim);
                    }
                    String prefix = null;
                    int idx = -1;
                    for (String term : getNamespaces()) {
                        prefix = term;
                        for (String nspref : nsprefs) {
                            if (term.startsWith(nspref)) {
                                idx = nspref.length();
                                if (idx < term.length()) {
                                    prefix = prefix.substring(idx);
                                    break;
                                }
                            }
                        }
                        for (String delim : delims) {
                            namespacePrefixes.add(prefix + delim);
                        }
                    }
                    if (namespacePrefixes.size() > 1)
                        sortByTermLength(namespacePrefixes);

                    System.out.println("");
                    System.out.println("  > namespacePrefixes == " + namespacePrefixes);
                    System.out.println("");

                }
            }
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
        return namespacePrefixes;
    }

    /** *************************************************************
     * A List of currently known namespaces.
     */
    private ArrayList<String> namespaces = new ArrayList<String>();
                
    /** **************************************************************
     * Returns a List of all SUO-KIF terms that denote namespaces in
     * any loaded KB, obtained by gathering statements formed with the
     * predicates inNamespace and ontologyNamespace as well as
     * explicit instance statements.
     *
     * @return A List of all known SUO-KIF terms that denote
     * namespaces
     */
    public ArrayList<String> getNamespaces() {
        try {
            synchronized (namespaces) {
                if (namespaces.isEmpty()) {
                    HashSet<String> reduce = new HashSet<String>();
                    KB kb = null;
                    for (Iterator it = KBmanager.getMgr().kbs.values().iterator(); it.hasNext();) {
                        kb = (KB) it.next();
                        reduce.addAll(kb.getTermsViaAsk(0,"inNamespace",2));
                        reduce.addAll(kb.getTermsViaAsk(0,"ontologyNamespace",2));
                        reduce.addAll(kb.getAllInstancesWithPredicateSubsumption("Namespace"));
                    }
                    if (!reduce.isEmpty()) 
                        namespaces.addAll(reduce);
                    if (namespaces.size() > 1)
                        sortByTermLength(namespaces);
                }
            }
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
        System.out.println("");
        System.out.println("  > namespaces == " + namespaces);
        System.out.println("");
        return namespaces;
    }

    /** **************************************************************
     * Returns a List of all SUO-KIF terms denoting namespaces in kb
     * or in ontology, using the predicates inNamespace and
     * ontologyNamespace.
     *
     * @param kb The KB in which statements will be checked
     *
     * @param ontology The name of the ontology that will be checked
     *
     * @param force If true, this parameter will force the List of
     * namespaces to be recomputed
     *
     * @return A List of all the SUO-KIF terms that denote namespaces
     * and occur in statements formed with inNamespace or
     * ontologyNamespace
     */
    protected ArrayList<String> getNamespaces(KB kb, String ontology, boolean force) {
        try {
            synchronized (namespaces) {
                if (namespaces.isEmpty() || force) {
                    if (force) {
                        namespaces.clear();
                        namespacePrefixes.clear();
                    }
                    HashSet<String> reduce = new HashSet<String>();
                    reduce.addAll(kb.getTermsViaAsk(0, "inNamespace", 2));
                    if (StringUtil.emptyString(ontology)) {
                        ontology = getOntology();
                    }
                    if (StringUtil.isNonEmptyString(ontology)) {
                        reduce.addAll(getOntologyNamespaces(kb, ontology));
                    }
                    reduce.addAll(kb.getAllInstancesWithPredicateSubsumption("Namespace"));
                    namespaces.addAll(reduce);
                    if (namespaces.size() > 1)
                        sortByTermLength(namespaces);
                    if (!namespaces.isEmpty()) {
                        Set<String> delims = new HashSet<String>(getAllNamespaceDelimiters());
                        delims.addAll(Arrays.asList(StringUtil.getKifNamespaceDelimiter(), 
                                                    StringUtil.getW3cNamespaceDelimiter(),
                                                    StringUtil.getSafeNamespaceDelimiter()));
                        ArrayList<String> nsprefs = new ArrayList<String>();
                        for (String delim : delims) {
                            nsprefs.add("ns" + delim);
                        }
                        String prefix = null;
                        int idx = -1;
                        for (String term : namespaces) {
                            prefix = term;
                            for (String nspref : nsprefs) {
                                if (term.startsWith(nspref)) {
                                    idx = nspref.length();
                                    if (idx < term.length()) {
                                        prefix = prefix.substring(idx);
                                        break;
                                    }
                                }
                            }
                            for (String delim : delims) {
                                namespacePrefixes.add(prefix + delim);
                            }
                        }
                        if (namespacePrefixes.size() > 1)
                            sortByTermLength(namespacePrefixes);
                    }

                    System.out.println("");
                    System.out.println("  > namespaces == " + namespaces);
                    System.out.println("  > namespacePrefixes == " + namespacePrefixes);
                    System.out.println("");

                }
            }
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
        return namespaces;
    }

    /** **************************************************************
     * Returns the namespace prefix of term based on the namespaces
     * known in kb, else returns the empty String if term appears to
     * have no namespace prefix.
     */
    protected String getNamespacePrefix(KB kb, String term) {
        String ans = "";
        try {
            if (StringUtil.isNonEmptyString(term)) {
                for (String prefix : getNamespacePrefixes()) {
                    if (term.startsWith(prefix)) {
                        ans = prefix;
                        break;
                    }
                }
            }
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
        return ans;
    }

    /** **************************************************************
     * Returns term without its namespace prefix if it appears to have
     * one in kb, else just returns term.
     */
    protected String stripNamespacePrefix(KB kb, String term) {
        String ans = term;
        try {
            String prefix = getNamespacePrefix(kb, term);
            if (StringUtil.isNonEmptyString(prefix)) {
                ans = term.substring(prefix.length());
            }
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
        return ans;
    }

    /** **************************************************************
     * Returns a SUO-KIF term denoting a namespace.
     *
     * @param kb The KB in which to determine if term is an namespace
     *
     * @param term A String denoting a namespace, perhaps in W3C format
     *
     * @return String A term denoting a namespace in SUO-KIF format,
     * else just returns the input term if no syntactic transformation
     * is warranted
     */
    protected String toKifNamespace(KB kb, String term) {
        String ans = term;
        try {
            String kifTerm = StringUtil.w3cToKif(term);
            String prefix = ("ns" + StringUtil.getKifNamespaceDelimiter());
            if (!kifTerm.equals("ns") && !kifTerm.startsWith(prefix)) {
                kifTerm = prefix + kifTerm;
            }
            List namespaces = getNamespaces(kb, getOntology(), false);
            if ((namespaces != null) && !namespaces.isEmpty()) {
                String ns = null;
                for (Iterator it = namespaces.iterator(); it.hasNext();) {
                    ns = (String) it.next();
                    if (ns.equalsIgnoreCase(kifTerm)) {
                        ans = ns;
                        break;
                    }
                }
            }
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
        return ans;
    }

    /** **************************************************************
     * Removes namespace prefixes from all SUO-KIF terms to be
     * hyperlinked in text.
     *
     * @param kb The KB is use when checking for valid namespace prefixes
     *
     * @param text The String in which all linkable SUO-KIF terms are
     * to be transformed
     * 
     * @return A String with all linkable SUO-KIF terms have had their
     * namespace prefixes removed
     */
    protected String removeLinkableNamespacePrefixes(KB kb, String text) {
        String ans = text;
        try {
            if (StringUtil.isNonEmptyString(text)) {
                if (namespacePrefixes == null) {
                    getNamespaces(kb, getOntology(), true);
                }
                String prefix = null;
                for (Iterator it = namespacePrefixes.iterator(); it.hasNext();) {
                    prefix = (String) it.next();
                    if (prefix.endsWith(StringUtil.getKifNamespaceDelimiter())) {
                        prefix = "\\&\\%" + prefix;
                        ans = ans.replaceAll(prefix, "");
                    }
                }
            }
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
        return ans;
    }

    /** *************************************************************
     *  Collects and returns a List of all Predicates in kb.
     *
     * @param kb The KB from which to gather all terms that are
     * instances of BinaryPredicate
     *
     * @return A List of BinaryPredicates (Strings)
     */
    protected ArrayList getPredicates(KB kb) {
        ArrayList cached = null;
        try {
            cached = (ArrayList) getRelationsByKB().get(kb);
            if (cached == null) {
                TreeSet predSet = new TreeSet();
                Set classNames = kb.getAllSubClassesWithPredicateSubsumption("Predicate");
                classNames.add("Predicate");
                Iterator it = classNames.iterator();
                String cn = null;
                String p0 = null;
                String p1 = null;
                String p2 = null;
                String namespace = null;
                Iterator it2 = null;
                List predList = null;
                while (it.hasNext()) {
                    cn = (String) it.next();
                    predList = kb.getTermsViaPredicateSubsumption("instance",
                                                                  2,
                                                                  cn,
                                                                  1,
                                                                  true);

                    // System.out.println("3. instances == " + instances);

                    for (it2 = predList.iterator(); it2.hasNext();) {
                        p1 = (String) it2.next();
                        namespace = getTermNamespace(kb, p1);
                        if (StringUtil.isNonEmptyString(namespace)
                            && getOntologyNamespaces(kb, getOntology()).contains(namespace)) {
                            // pred.contains(StringUtil.getKifNamespaceDelimiter())) {
                            predSet.add(p1);
                        }
                    }
                }
                List p0List = new ArrayList();
                List working = new ArrayList();
                Set accumulator = new HashSet(Arrays.asList("subrelation", "inverse"));
                while (!accumulator.isEmpty()) {
                    working.clear();
                    working.addAll(accumulator);
                    accumulator.clear();
                    for (it = working.iterator(); it.hasNext();) {
                        p0 = (String) it.next();
                        namespace = getTermNamespace(kb, p0);
                        if (StringUtil.isNonEmptyString(namespace)
                            && getOntologyNamespaces(kb, getOntology()).contains(namespace)) {
                            predSet.add(p0);
                        }
                        if (!p0List.contains(p0)) {
                            p0List.add(p0);
                        }
                        accumulator.addAll(kb.getTermsViaPredicateSubsumption("subrelation",
                                                                              2,
                                                                              p0,
                                                                              1,
                                                                              true));
                    }
                }
                List formulae = null;
                Formula f = null;
                for (it = p0List.iterator(); it.hasNext();) {
                    p0 = (String) it.next();
                    formulae = kb.ask("arg", 0, p0);
                    if (formulae != null) {
                        for (it2 = formulae.iterator(); it2.hasNext();) {
                            f = (Formula) it2.next();
                            p1 = f.getArgument(1);
                            namespace = getTermNamespace(kb, p1);
                            if (StringUtil.isNonEmptyString(namespace)
                                && getOntologyNamespaces(kb, getOntology()).contains(namespace)) {
                                predSet.add(p1);
                            }
                            p2 = f.getArgument(2);
                            namespace = getTermNamespace(kb, p2);
                            if (StringUtil.isNonEmptyString(namespace)
                                && getOntologyNamespaces(kb, getOntology()).contains(namespace)) {
                                predSet.add(p2);
                            }
                        }
                    }
                }
                cached = new ArrayList(predSet);
                getRelationsByKB().put(kb, cached);
            }
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
        return cached;
    }

    /** *************************************************************
     * Returns true if term has syntactic subcomponents such as XML
     * elements or XML attributes in kb, else returns false.
     *
     * @param kb The KB in which term is defined
     *
     * @param term A String denoting a SUO-KIF constant name
     * 
     * @return true or false
     */
    protected boolean hasSubComponents(KB kb, String term) {
        boolean ans = false;
        try {
            if (StringUtil.isNonEmptyString(term)) {
                ans = (getSubComponents(kb, term) != null);
            }
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
        return ans;
    }

    /** *************************************************************
     * Returns a List containing those terms that are immediate
     * syntactic subordinates of term in kb.
     *
     * @param kb The KB in which term is defined
     *
     * @param term A String that is a SUO-KIF constant
     * 
     * @return A List of Strings that denote SUO-KIF constants, or an
     * empty List
     */
    protected ArrayList getSubComponents(KB kb, String term) {
        ArrayList ans = new ArrayList();
        try {
            if (StringUtil.isNonEmptyString(term)) {
                ans.addAll(kb.getTermsViaPredicateSubsumption("syntacticSubordinate",
                                                              2,
                                                              term,
                                                              1,
                                                              true));
            }
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
        return ans;
    }
 
    /** *************************************************************
     * Returns a List containing those terms that are immediate
     * syntactic superiors or "containers" of term in kb.
     *
     * @param kb The KB in which term is defined
     *
     * @param term A String, a SUO-KIF constant
     * 
     * @return A List of Strings that denote SUO-KIF constants, or an
     * empty List
     */
    protected ArrayList getSuperComponents(KB kb, String term) {
        ArrayList ans = new ArrayList();
        try {
            if (StringUtil.isNonEmptyString(term)) {
                ans.addAll(kb.getTermsViaPredicateSubsumption("syntacticSubordinate",
                                                              1,
                                                              term,
                                                              2,
                                                              true));
            }
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
        return ans;
    }
 
    /** *************************************************************
     * Returns a String that is the first termFormat value obtained
     * for term in kb, else returns null if no termFormat value
     * exists.
     *
     * @param kb The KB in which term is defined
     *
     * @param term A String that is a SUO-KIF constant
     * 
     * @param contexts A List of namespaces or other terms that index
     * context-specific termFormat statements
     * 
     * @return A List of Strings that denote SUO-KIF constants, or an
     * empty List
     */
    protected String getFirstTermFormat(KB kb, String term, List contexts) {
        String ans = null;
        try {
            if (StringUtil.isNonEmptyString(term)
                && !StringUtil.isQuotedString(term)) {
                List forms = //kb.askWithRestriction(2, term, 0, "termFormat");
                    kb.askWithRestriction(2, term, 0, "headword");
                if ((forms != null) && !forms.isEmpty()) {
                    String ctx = null;
                    Formula f = null;
                    for (int i = 0; i < contexts.size(); i++) {
                        ctx = (String) contexts.get(i);
                        for (int j = 0; j < forms.size(); j++) {
                            f = (Formula) forms.get(j);
                            if (f.getArgument(1).equals(ctx)) {
                                ans = f.getArgument(3);
                                break;
                            }
                        }
                        if (ans != null) { break; }
                    }
                    if ((ans == null) && StringUtil.isLocalTermReference(term)) {
                        String moreGeneralTerm = getFirstGeneralTerm(kb, term);
                        if (StringUtil.isNonEmptyString(moreGeneralTerm)) {
                            ans = getFirstTermFormat(kb, moreGeneralTerm, contexts);
                        }
                    }
                }
                if (ans == null)
                    ans = term;
            }
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
        return ans;
    }

    /** *************************************************************
     * Returns the first documentation String obtained for term in kb,
     * using the List of namespaces or other contextualizing terms in
     * contexts.
     * 
     * @param kb The KB in which term is defined
     *
     * @param term A String that is a SUO-KIF constant
     * 
     * @param contexts A List of namespaces or other terms that index
     * context-specific documentation or comment statements
     * 
     * @return A documentation String, or an empty String if no
     * documentation String can be found
     */
    protected String getContextualDocumentation(KB kb, String term, List contexts) {
        String ans = "";
        try {
            if (StringUtil.isNonEmptyString(term)) {
                List forms = kb.askWithRestriction(1, term, 0, "documentation");
                if ((forms != null) && !forms.isEmpty()) {

                    if (!(contexts instanceof List)) {
                        contexts = new ArrayList();
                    }
                    List supers = getSuperComponents(kb, term);
                    contexts.addAll(supers);
                    contexts.add(0, term);
                    contexts.add(getDefaultNamespace());
                    if (!contexts.contains("EnglishLanguage")) {
                        contexts.add("EnglishLanguage");
                    }

                    String ctx = null;
                    Iterator itf = null;
                    Formula f = null;
                    for (Iterator itc = contexts.iterator(); itc.hasNext();) {
                        ctx = (String) itc.next();
                        for (itf = forms.iterator(); itf.hasNext();) {
                            f = (Formula) itf.next();
                            if (f.getArgument(2).equals(ctx)) {
                                ans = f.getArgument(3);
                                break;
                            }
                        }
                        if (StringUtil.isNonEmptyString(ans)) break;
                    }
                    if (StringUtil.emptyString(ans)) {
                        String classOfTerm = getFirstGeneralTerm(kb, term);
                        if (StringUtil.isNonEmptyString(classOfTerm)) {
                            ans = getContextualDocumentation(kb, classOfTerm, null);
                        }
                    }
                }
            }
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
        return ans;
    }

    /** *************************************************************
     * Returns the first containing Class that can be found for term
     * in kb.
     * 
     * @param kb The KB in which term is defined
     *
     * @param term A String that is a SUO-KIF constant
     * 
     * @return A SUO-KIF term denoting a Class, or null if no Class
     * can be found
     */
    protected String getNearestContainingClass(KB kb, String term) {
        String ans = null;
        try {
            List<String> predicates = new LinkedList<String>();
            Set<String> accumulator = new HashSet<String>();
            List<String> working = new LinkedList<String>();
            accumulator.add("instance");
            while (!accumulator.isEmpty()) {
                for (String p1 : accumulator) {
                    if (!predicates.contains(p1)) {
                        predicates.add(0, p1);
                    }
                }
                working.clear();
                working.addAll(accumulator);
                accumulator.clear();
                for (String p2 : working) {
                    accumulator.addAll(kb.getTermsViaPredicateSubsumption("subrelation",
                                                                          2,
                                                                          p2,
                                                                          1,
                                                                          false));
                }
            }
            for (String p3 : predicates) {
                ans = kb.getFirstTermViaAskWithRestriction(0,
                                                           p3,
                                                           1,
                                                           term,
                                                           2);
                if (ans != null) break;
                accumulator.addAll(kb.getTermsViaPredicateSubsumption("inverse",
                                                                      2,
                                                                      p3,
                                                                      1,
                                                                      false));
                accumulator.addAll(kb.getTermsViaPredicateSubsumption("inverse",
                                                                      1,
                                                                      p3,
                                                                      2,
                                                                      false));
                for (String p4 : accumulator) {
                    ans = kb.getFirstTermViaAskWithRestriction(0,
                                                               p4,
                                                               2,
                                                               term,
                                                               1);
                    if (ans != null) break;
                }
                if (ans != null) break;
                accumulator.clear();
            }
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
        return ans;
    }

    /** *************************************************************
     * Returns the first purely "subsuming" entity that can be found
     * for term in kb, assuming that term denotes a Class or a
     * Relation.
     * 
     * @param kb The KB in which term is defined
     *
     * @param term A String that denotes a SUO-KIF Class or Relation
     * 
     * @return A SUO-KIF term, or null if no subsuming term can be
     * found
     */
    protected String getFirstSubsumingTerm(KB kb, String term) {
        String ans = null;
        try {
            List<String> preds = Arrays.asList("subclass", "subrelation");
            List<String> terms = null;
            for (String p : preds) {
                terms = kb.getTermsViaPredicateSubsumption(p,
                                                           1,
                                                           term,
                                                           2,
                                                           true);
                if (!terms.isEmpty()) {
                    ans = terms.get(0);
                    break;
                }
            }
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
        return ans;
    }

    /** *************************************************************
     * Returns the first containing, subsuming, or superordinate
     * entity that can be found for term in kb.
     * 
     * @param kb The KB in which term is defined
     *
     * @param term A String that is a SUO-KIF constant
     * 
     * @return A SUO-KIF term, or null if no more general term can be
     * found
     */
    protected String getFirstGeneralTerm(KB kb, String term) {
        String ans = null;
        try {
            List<String> preds = Arrays.asList("instance", 
                                               // "subclass",
                                               "datatype",
                                               "syntacticExtension",
                                               "syntacticComposite",
                                               "subclass"
                                               );
            List<String> terms = null;
            for (String p : preds) {
                terms = kb.getTermsViaPredicateSubsumption(p,
                                                           1,
                                                           term,
                                                           2,
                                                           false);
                if (!terms.isEmpty()) {
                    ans = terms.get(0);
                    break;
                }
            }
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
        return ans;
    }

    /** *************************************************************
     * Returns a List of the first containing, subsuming, or
     * superordinate entities found for term in kb.
     * 
     * @param kb The KB in which term is defined
     *
     * @param term A String that denotes a SUO-KIF term
     * 
     * @return A List of SUO-KIF terms, or an empty List if no
     * generalizations of term can be found
     */
    protected ArrayList<String> getFirstGeneralTerms(KB kb, String term) {
        ArrayList<String> ans = new ArrayList<String>();
        try {
            List<String> preds = Arrays.asList("instance", 
                                               // "subclass",
                                               "datatype",
                                               "syntacticExtension",
                                               "syntacticComposite",
                                               "subclass"
                                               );
            Set<String> terms = new HashSet<String>();
            for (String p : preds) {
                terms.addAll(kb.getTermsViaPredicateSubsumption(p,
                                                                1,
                                                                term,
                                                                2,
                                                                true));
            }
            ans.addAll(terms);
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
        return ans;
    }

    /** *************************************************************
     * Returns a List of the first instances or syntactic subordinate
     * entities that can be found for term in kb.
     * 
     * @param kb The KB in which term is defined
     *
     * @param term A String that denotes a SUO-KIF term
     * 
     * @return A List of SUO-KIF terms, or an empty List
     * 
     */
    protected ArrayList<String> getFirstSpecificTerms(KB kb, String term) {
        ArrayList<String> ans = new ArrayList<String>();
        /*          */
        try {
            if (StringUtil.isNonEmptyString(term)) {
                List<String> preds = Arrays.asList("instance", 
                                                   // "datatype",
                                                   "syntacticExtension",
                                                   "syntacticComposite"
                                                   // "subclass"
                                                   );
                for (String p : preds) {
                    ans.addAll(kb.getTermsViaPredicateSubsumption(p,
                                                                  2,
                                                                  term,
                                                                  1,
                                                                  true));
                    if (!ans.isEmpty()) break;
                }
            }
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
        return ans;
    }

    /** *************************************************************
     * Returns a List of the entities that are immediate syntactic
     * subordinates of term in kb.
     * 
     * @param kb The KB in which term is defined
     *
     * @param term A String that denotes a SUO-KIF term
     * 
     * @return A List of SUO-KIF terms, or an empty List
     * 
     */
    protected ArrayList<String> getSyntacticSubordinateTerms(KB kb, String term) {
        ArrayList<String> ans = new ArrayList<String>();
        /*          */
        try {
            if (StringUtil.isNonEmptyString(term)) {
                List<String> preds = Arrays.asList("syntacticSubordinate", 
                                                   // "subclass",
                                                   "datatype",
                                                   "syntacticExtension",
                                                   "syntacticComposite");
                Set<String> terms = new HashSet<String>();
                for (String p : preds) {
                    terms.addAll(kb.getTermsViaPredicateSubsumption(p,
                                                                    2,
                                                                    term,
                                                                    1,
                                                                    true));
                }
                terms.addAll(kb.getTermsViaPredicateSubsumption("syntacticUnion",
                                                                1,
                                                                term,
                                                                2,
                                                                true));
                ans.addAll(terms);
            }
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
        return ans;
    }

    /** *************************************************************
     * Returns a List of the entities that are immediate instances of
     * term in kb.
     * 
     * @param kb The KB in which term is defined
     *
     * @param term A String that denotes a SUO-KIF Class
     * 
     * @return A List of SUO-KIF terms, or an empty List
     * 
     */
    protected ArrayList getFirstInstances(KB kb, String term) {
        ArrayList ans = new ArrayList();
        /*          */
        try {
            if (StringUtil.isNonEmptyString(term)) {
                List terms = kb.getTermsViaPredicateSubsumption("instance",
                                                                2,
                                                                term,
                                                                1,
                                                                true);
                if (!terms.isEmpty()) {
                    ans.addAll(terms);
                }
            }
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
        return ans;
    }

    /** *************************************************************
     * Returns a SUO-KIF constant that denotes the first containing
     * Class of term obtained in kb.
     * 
     * @param kb The KB in which term is defined
     *
     * @param term A String that denotes a SUO-KIF term
     * 
     * @return A String that denotes a SUO-KIF Class, or null
     * 
     */
    protected String getFirstContainingClass(KB kb, String term) {
        return kb.getFirstTermViaPredicateSubsumption("instance",
                                                      1,
                                                      term,
                                                      2,
                                                      true);
    }

    /** *************************************************************
     * Returns a List containing the immediate SUO-KIF subclasses of
     * term in kb.
     * 
     * @param kb The KB in which term is defined
     *
     * @param term A String that denotes a SUO-KIF term
     * 
     * @return A List of SUO-KIF Classes, or an empty List
     * 
     */
    protected ArrayList getFirstSubClasses(KB kb, String term) {
        ArrayList ans = new ArrayList();
        /*          */
        try {
            if (StringUtil.isNonEmptyString(term)) {
                List terms = kb.getTermsViaPredicateSubsumption("subclass",
                                                                2,
                                                                term,
                                                                1,
                                                                false);
                if (!terms.isEmpty()) {
                    ans.addAll(terms);
                }
            }
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
        return ans;
    }

    /** *************************************************************
     * Returns a String consisting of HTML markup for a documentation
     * String for term obtained from kb and indexed by language.
     * 
     * @param kb The KB in which term is defined
     *
     * @param kbHref A String containing the constant parts of the
     * href link for term, or an empty String
     *
     * @param term A String that denotes a SUO-KIF term
     * 
     * @param language A String denoting a SUO-KIF namespace, a
     * natural language, or other type of entity that indexes
     * documentation Strings in kb
     * 
     * @return A String containing HTML markup, or an empty String if
     * term is supposed to be suppressed for display
     * 
     */
    protected String createDocs(KB kb, String kbHref, String term, String language) {
        String markup = "";
        try {
            if (isLegalForDisplay(term)) {
                StringBuilder result = new StringBuilder();
                ArrayList context = new ArrayList();
                context.add(language);
                String docString = getContextualDocumentation(kb, term, context);
                docString = processDocString(kb, kbHref, language, docString, false, true);
                result.append("<tr>");
                result.append(StringUtil.getLineSeparator());
                result.append("  <td valign=\"top\" class=\"description\">");
                result.append(StringUtil.getLineSeparator());
                result.append("    ");
                result.append(docString);
                result.append(StringUtil.getLineSeparator());
                result.append("  </td>"); 
                result.append(StringUtil.getLineSeparator());
                result.append("</tr>");
                
                markup = result.toString();
            }
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
        return markup;
    }

    /** *************************************************************
     * Returns a String containing the HTML markup for the Comment
     * field in a page displaying the definition of term in kb.
     * 
     * @param kb The KB in which term is defined
     *
     * @param kbHref A String containing the constant parts of the
     * href link for term, or an empty String
     *
     * @param term A String that denotes a SUO-KIF term
     * 
     * @param language A String denoting a SUO-KIF namespace, a
     * natural language, or other type of entity that indexes
     * comment Strings in kb
     * 
     * @return A String containing HTML markup, or an empty String if
     * term is supposed to be suppressed for display
     * 
     */
    protected String createComments(KB kb, String kbHref, String term, String language) {

        StringBuilder result = new StringBuilder();
        if (isLegalForDisplay(term)) {
            List formulae = kb.askWithRestriction(0,"comment",1,term);
            if (formulae != null && !formulae.isEmpty()) {
                Formula f = null;
                List docs = new ArrayList();
                for (int c = 0; c < formulae.size(); c++) {
                    f = (Formula) formulae.get(c);
                    docs.add(f.getArgument(3));
                }

                Collections.sort(docs);

                result.append("<tr>");
                result.append(StringUtil.getLineSeparator());
                result.append("  <td valign=\"top\" class=\"label\">Comments</td>");

                String docString = null;
                for (int i = 0; i < docs.size(); i++) {
                    docString = (String) docs.get(i);
                    docString = processDocString(kb, kbHref, language, docString, false, true);
                    if (i > 0) {
                        result.append("<tr>");
                        result.append(StringUtil.getLineSeparator());
                        result.append("  <td>&nbsp;</td>");
                        result.append(StringUtil.getLineSeparator());
                    }
                    result.append("  <td valign=\"top\" colspan=\"2\" class=\"cell\">");
                    result.append(StringUtil.getLineSeparator());
                    result.append("      ");
                    result.append(docString);
                    result.append("<br/>");
                    result.append(StringUtil.getLineSeparator());
                    result.append("  </td>");
                    result.append(StringUtil.getLineSeparator());
                    result.append("</tr>");
                    result.append(StringUtil.getLineSeparator());
                }
            }
        }
        return result.toString();
    }

    /** *************************************************************
     * Returns a String containing HTML markup for the synonym field
     * of an HTML page displaying the definition of term in kb.
     * 
     * @param kb The KB in which term is defined
     *
     * @param kbHref A String containing the constant parts of the
     * href link for term, or an empty String
     *
     * @param term A String that denotes a SUO-KIF term
     * 
     * @return A String containing HTML markup, or an empty String if
     * term is supposed to be suppressed for display
     * 
     */
    protected String createSynonyms(KB kb, String kbHref, String term, Set formatTokens) {
        String result = "";
        try {
            if (isLegalForDisplay(term)) {
                ArrayList alternates = new ArrayList();
                if (StringUtil.isNonEmptyString(term)) {
                    alternates.addAll(kb.askWithRestriction(0, "synonym", 2, term));
                    alternates.addAll(kb.askWithRestriction(0, "headword", 2, term));
                    if (!alternates.isEmpty()) {
                        // String namespace = null;
                        // boolean found = false;
                        String presentationName = getTermPresentationName(kb, term);
                        ArrayList<String> synonyms = new ArrayList<String>();
                        Formula f = null;
                        String syn = null;
                        String hwsuff = "_hw";
                        int sidx = -1;
                        String namespace = null;
                        String prefix = null;
                        for (Iterator it = alternates.iterator(); it.hasNext();) {
                            f = (Formula) it.next();
                            namespace = f.getArgument(1);
                            prefix = stripNamespacePrefix(kb, namespace);
                            syn = StringUtil.removeEnclosingQuotes(f.getArgument(3));
                            if (!syn.equals(presentationName)) {
                                if (prefix.matches("^iso\\d+.*")) {
                                    sidx = prefix.lastIndexOf(hwsuff);
                                    if (sidx > -1)
                                        prefix = prefix.substring(0, sidx);
                                    syn = (prefix + StringUtil.getW3cNamespaceDelimiter() + syn);
                                }
                                synonyms.add(syn);
                            }
                            // }
                        }
                        if (!synonyms.isEmpty()) {
                            sortByPresentationName(kb, getDefaultNamespace(), synonyms);
                            StringBuilder sb = new StringBuilder();
                            if ((formatTokens != null) && formatTokens.contains(F_DD2)) {
                                sb.append("<tr>");
                                sb.append(getLineSeparator());
                                sb.append("  <td class=\"label\">");
                                sb.append(getLineSeparator());
                                sb.append("    Synonym");
                                sb.append((synonyms.size() > 1) ? "s" : "");
                                sb.append(getLineSeparator());
                                sb.append("  </td>");
                                sb.append(getLineSeparator());
                                sb.append("  <td class=\"syn\">");
                                sb.append(getLineSeparator());
                                sb.append("    ");
                                boolean isFirst = true;
                                for (String syn2 : synonyms) {
                                    sb.append(isFirst ? "" : ", ");
                                    isFirst = false;
                                    sb.append(syn2);
                                }
                                sb.append(getLineSeparator());
                                sb.append("  </td>");
                                sb.append(getLineSeparator());
                                sb.append("</tr>");
                                sb.append(StringUtil.getLineSeparator());
                            }
                            else {
                                sb.append("<tr>");
                                sb.append(StringUtil.getLineSeparator());
                                sb.append("  <td valign=\"top\" class=\"cell\">");
                                sb.append("<strong>Synonym");
                                sb.append((synonyms.size() > 1) ? "s" : "");
                                sb.append("</strong>");
                                boolean isFirst = true;
                                for (String syn1 : synonyms) {
                                    sb.append(isFirst ? " " : ", ");
                                    isFirst = false;
                                    sb.append("<i>");
                                    sb.append(syn1);
                                    sb.append("</i>");
                                }
                                sb.append("</td>");
                                sb.append(StringUtil.getLineSeparator());
                                sb.append("</tr>");
                                sb.append(StringUtil.getLineSeparator());
                            }
                            result = sb.toString();
                        }
                    }
                }
            }
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
        return result;
    }

    /** *************************************************************
     * Returns a String containing HTML markup for the synonym field
     * of an HTML page displaying the definition of term in kb.
     * 
     * @param kb The KB in which term is defined
     *
     * @param kbHref A String containing the constant parts of the
     * href link for term, or an empty String
     *
     * @param term A String that denotes a SUO-KIF term
     * 
     * @return A String containing HTML markup, or an empty String if
     * term is supposed to be suppressed for display
     * 
     */
    protected String createSynonyms(KB kb, String kbHref, String term) {
        // for Data Dictionary format #2
        String result = "";
        try {
            Set<String> formatTokens = new HashSet<String>();
            formatTokens.add(F_DD2);
            result = createSynonyms(kb, kbHref, term, formatTokens);
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
        return result;
    }

    /** *************************************************************
     * Returns a String containing HTML markup for the Display Labels
     * field of an HTML page displaying statements about term in kb.
     * 
     * @param kb The KB in which term is defined
     *
     * @param kbHref A String containing the constant parts of the
     * href link for term, or an empty String
     *
     * @param term A String that denotes a SUO-KIF term
     * 
     * @param formatTokens A Set of String tokens that control the
     * format of the output
     * 
     * @return A String containing HTML markup, or an empty String if
     * term is supposed to be suppressed for display
     */
    protected String createDisplayNames(KB kb, String kbHref, String term, Set formatTokens) {
        String result = "";
        try {
            if (isLegalForDisplay(term)) {
                List<String> labels = new ArrayList<String>();
                if (StringUtil.isNonEmptyString(term)) {
                    String defaultNamespace = getDefaultNamespace();
                    if (StringUtil.isNonEmptyString(defaultNamespace)) {
                        labels.addAll(kb.getTermsViaAWTR(2, term, 
                                                         0, "displayName",
                                                         1, defaultNamespace,
                                                         3));
                    }
                    if (labels.isEmpty()) {
                        labels.addAll(kb.getTermsViaAskWithRestriction(2, term,
                                                                       0, "displayName",
                                                                       3));
                    }
                    if (!labels.isEmpty()) {
                        if (labels.size() > 1)
                            Collections.sort(labels, String.CASE_INSENSITIVE_ORDER);
                        StringBuilder sb = new StringBuilder();
                        if ((formatTokens != null) && formatTokens.contains(F_DD2)) {
                            sb.append("<tr>");
                            sb.append(getLineSeparator());
                            sb.append("  <td class=\"label\">");
                            sb.append(getLineSeparator());
                            sb.append("    Display Label");
                            sb.append((labels.size() > 1) ? "s" : "");
                            sb.append(getLineSeparator());
                            sb.append("  </td>");
                            sb.append(getLineSeparator());
                            sb.append("  <td class=\"syn\">");
                            sb.append(getLineSeparator());
                            sb.append("    ");
                            boolean isFirst = true;
                            for (String lab2 : labels) {
                                sb.append(isFirst ? "" : ", ");
                                isFirst = false;
                                sb.append(StringUtil.removeEnclosingQuotes(lab2));
                            }
                            sb.append(getLineSeparator());
                            sb.append("  </td>");
                            sb.append(getLineSeparator());
                            sb.append("</tr>");
                            sb.append(StringUtil.getLineSeparator());
                        }
                        else {
                            sb.append("<tr>");
                            sb.append(StringUtil.getLineSeparator());
                            sb.append("  <td valign=\"top\" class=\"cell\">");
                            sb.append("<strong>Display Label");
                            sb.append((labels.size() > 1) ? "s" : "");
                            sb.append("</strong>");
                            boolean isFirst = true;
                            for (String lab1 : labels) {
                                sb.append(isFirst ? " " : ", ");
                                isFirst = false;
                                sb.append("<i>");
                                sb.append(StringUtil.removeEnclosingQuotes(lab1));
                                sb.append("</i>");
                            }
                            sb.append("</td>");
                            sb.append(StringUtil.getLineSeparator());
                            sb.append("</tr>");
                            sb.append(StringUtil.getLineSeparator());
                        }
                        result = sb.toString();
                    }
                }
            }
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
        return result;
    }

    /** *************************************************************
     * Returns a String containing HTML markup for the Display Labels
     * field of an HTML page displaying statements about term in kb.
     * 
     * @param kb The KB in which term is defined
     *
     * @param kbHref A String containing the constant parts of the
     * href link for term, or an empty String
     *
     * @param term A String that denotes a SUO-KIF term
     * 
     * @return A String containing HTML markup, or an empty String if
     * term is supposed to be suppressed for display
     */
    protected String createDisplayNames(KB kb, String kbHref, String term) {
        Set<String> formatTokens = new HashSet<String>();
        formatTokens.add(F_DD2);
        return createDisplayNames(kb, kbHref, term, formatTokens);
    }

    /** *************************************************************
     * Returns a String containing HTML markup for the Has Same
     * Components As field of an HTML page displaying the definition
     * of term in kb.
     * 
     * @param kb The KB in which term is defined
     *
     * @param kbHref A String containing the constant parts of the
     * href link for term, or an empty String
     *
     * @param term A String that denotes a SUO-KIF term
     * 
     * @param language A String denoting a SUO-KIF namespace, a
     * natural language, or other type of entity that indexes
     * termFormat Strings in kb
     * 
     * @return A String containing HTML markup, or an empty String if
     * term is supposed to be suppressed for display
     * 
     */
    protected String createHasSameComponents(KB kb, 
                                             String kbHref, 
                                             String term, 
                                             String language) {

        StringBuilder result = new StringBuilder();
        if (isLegalForDisplay(term)) {
            String suffix = (StringUtil.emptyString(kbHref)
                             ? ".html"
                             : "");
            ArrayList<String> extensionOfs = getSyntacticExtensionTerms(kb, term, 2, true);
            if (!extensionOfs.isEmpty()) {
                result.append("<tr>");
                result.append(StringUtil.getLineSeparator());
                result.append("  <td valign=\"top\" class=\"label\">");
                result.append(StringUtil.getLineSeparator());
                result.append("    Has Same Components As");
                result.append(StringUtil.getLineSeparator());
                result.append("  </td>");
                result.append(StringUtil.getLineSeparator());
                boolean isFirst = true;
                StringBuilder hrefSB = new StringBuilder();
                for (String extended : extensionOfs) {
                    hrefSB.setLength(0);
                    hrefSB.append("<a href=\"");
                    hrefSB.append(kbHref);
                    hrefSB.append(StringUtil.toSafeNamespaceDelimiter(kbHref, extended));
                    hrefSB.append(suffix);
                    hrefSB.append("\">");
                    hrefSB.append(showTermName(kb, extended, language, true));
                    hrefSB.append("</a>");
                    if (isFirst) {
                        result.append("  <td valign=\"top\" class=\"cell\">");
                        result.append(StringUtil.getLineSeparator());
                        isFirst = false;
                    }
                    result.append("    ");
                    result.append(hrefSB.toString());
                    result.append("<br/>");
                    result.append(StringUtil.getLineSeparator());
                }
                result.append("  </td>");
                result.append(StringUtil.getLineSeparator());
                result.append("</tr>");
                result.append(StringUtil.getLineSeparator());
            }
        }
        return result.toString();
    }

    /** *************************************************************
     * Returns a String containing HTML markup for the Composites
     * Using Same Components field of an HTML page displaying the
     * definition of term in kb.
     * 
     * @param kb The KB in which term is defined
     *
     * @param kbHref A String containing the constant parts of the
     * href link for term, or an empty String
     *
     * @param term A String that denotes a SUO-KIF term
     * 
     * @param language A String denoting a SUO-KIF namespace, a
     * natural language, or other type of entity that indexes
     * termFormat Strings in kb
     * 
     * @return A String containing HTML markup, or an empty String if
     * term is supposed to be suppressed for display
     * 
     */
    protected String createUsingSameComponents(KB kb, 
                                               String kbHref, 
                                               String term, 
                                               String language) {

        StringBuilder result = new StringBuilder();
        try {
            if (StringUtil.isNonEmptyString(term)) {
                if (isLegalForDisplay(term)) {
                    String suffix = "";
                    if (StringUtil.emptyString(kbHref)) 
                        suffix = ".html";
                    ArrayList extensions = getSyntacticExtensionTerms(kb, term, 1, true);
                    /*
                      kb.getTransitiveClosureViaPredicateSubsumption("syntacticExtension",
                      2,
                      term,
                      1,
                      true);
                    */
                    if (!extensions.isEmpty()) {
                        result.append("<tr>");
                        result.append(StringUtil.getLineSeparator());
                        result.append("  <td valign=\"top\" class=\"label\">");
                        result.append(StringUtil.getLineSeparator());
                        result.append("    ");
                        result.append("Composites Using Same Components");
                        result.append(StringUtil.getLineSeparator());
                        result.append("  </td>");
                        result.append(StringUtil.getLineSeparator());
                        String extension = null;
                        boolean isFirst = true;
                        StringBuilder hrefSB = new StringBuilder();
                        for (Iterator it = extensions.iterator(); it.hasNext();) {
                            extension = (String) it.next();
                            hrefSB.setLength(0);
                            hrefSB.append("<a href=\"");
                            hrefSB.append(kbHref);
                            hrefSB.append(StringUtil.toSafeNamespaceDelimiter(kbHref, extension));
                            hrefSB.append(suffix);
                            hrefSB.append("\">");
                            hrefSB.append(showTermName(kb, extension, language, true));
                            hrefSB.append("</a>");
                            if (isFirst) {
                                result.append("  <td valign=\"top\" class=\"cell\">");
                                result.append(StringUtil.getLineSeparator());
                                isFirst = false;
                            }
                            result.append("    ");
                            result.append(hrefSB.toString());
                            result.append("<br/>");
                            result.append(StringUtil.getLineSeparator());
                        }
                        result.append("  </td>");
                        result.append(StringUtil.getLineSeparator());
                        result.append("</tr>");
                        result.append(StringUtil.getLineSeparator());
                    }
                }
            }
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
        return result.toString();
    }

    /** *************************************************************
     * Returns a String containing HTML markup for the Parents field
     * of an HTML page displaying the definition of term in kb.
     * 
     * @param kb The KB in which term is defined
     *
     * @param kbHref A String containing the constant parts of the
     * href link for term, or an empty String
     *
     * @param term A String that denotes a SUO-KIF term
     * 
     * @param language A String denoting a SUO-KIF namespace, a
     * natural language, or other type of entity that indexes
     * termFormat Strings in kb
     * 
     * @param parentsSet A Set for accumulating the parent terms of
     * term
     * 
     * @return A String containing HTML markup, or an empty String if
     * term is supposed to be suppressed for display
     * 
     */
    protected String createParents(KB kb, 
                                   String kbHref, 
                                   String term, 
                                   String language, 
                                   Set parentsSet) {
        String result = "";
        try {
            String suffix = "";
            if (StringUtil.emptyString(kbHref)) 
                suffix = ".html";

            // System.out.println("4. forms == " + forms);
            ArrayList forms = new ArrayList();
            Set<String> parents = new HashSet<String>();
            List<String> relations = Arrays.asList("subclass", 
                                                   "subrelation", 
                                                   "subAttribute", 
                                                   "subentity");
            if (StringUtil.isNonEmptyString(term)) {
                for (String pred : relations) {
                    forms.addAll(kb.askWithPredicateSubsumption(pred, 1, term));
                }
                String s = null;
                Formula f = null;
                for (Iterator it = forms.iterator(); it.hasNext();) {
                    f = (Formula) it.next();
                    if (!f.sourceFile.endsWith(KB._cacheFileSuffix)) {
                        s = f.getArgument(2);
                        if (isLegalForDisplay(s)) {
                            parents.add(s);
                        }
                    }
                }
                if (!parents.isEmpty()) {
                    parentsSet.addAll(parents);
                    StringBuilder sb = new StringBuilder();
                    List<String> sorted = new ArrayList<String>(parents);
                    Collections.sort(sorted, String.CASE_INSENSITIVE_ORDER);
                    sb.append("<tr>");
                    sb.append(StringUtil.getLineSeparator());
                    sb.append("  <td valign=\"top\" class=\"label\">Parents</td>");
                    sb.append(StringUtil.getLineSeparator());
                    StringBuilder hrefSB = new StringBuilder();
                    boolean isFirst = true;
                    for (String parent : sorted) {
                        hrefSB.setLength(0);
                        hrefSB.append("<a href=\"");
                        hrefSB.append(kbHref);
                        hrefSB.append(StringUtil.toSafeNamespaceDelimiter(kbHref, parent));
                        hrefSB.append(suffix);
                        hrefSB.append("\">");
                        hrefSB.append(showTermName(kb, parent, language, true));
                        hrefSB.append("</a>");
                        if (!isFirst) {
                            sb.append("<tr>");
                            sb.append(StringUtil.getLineSeparator());
                            sb.append("  <td>&nbsp;</td>");                
                            sb.append(StringUtil.getLineSeparator());
                        }
                        isFirst = false;
                        sb.append("  <td valign=\"top\" class=\"cell\">");
                        sb.append(hrefSB.toString());
                        sb.append("</td>");
                        sb.append(StringUtil.getLineSeparator());
                        String docStr = getContextualDocumentation(kb, parent, null);
                        sb.append("  <td valign=\"top\" class=\"cell\">");
                        sb.append(StringUtil.getLineSeparator());
                        sb.append("    ");
                        sb.append(processDocString(kb, 
                                                   kbHref, 
                                                   language, 
                                                   docStr,
                                                   false,
                                                   true));
                        sb.append(StringUtil.getLineSeparator());
                        sb.append("  </td>");
                        sb.append(StringUtil.getLineSeparator());
                    }
                    sb.append("</tr>");
                    sb.append(StringUtil.getLineSeparator());
                    result = sb.toString();
                }
            }
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
        return result;
    }

    /** *************************************************************
     * Returns a String containing HTML markup for the Children field
     * of an HTML page displaying the definition of term in kb.
     * 
     * @param kb The KB in which term is defined
     *
     * @param kbHref A String containing the constant parts of the
     * href link for term, or an empty String
     *
     * @param term A String that denotes a SUO-KIF term
     * 
     * @param language A String denoting a SUO-KIF namespace, a
     * natural language, or other type of entity that indexes
     * termFormat Strings in kb
     * 
     * @return A String containing HTML markup, or an empty String if
     * term is supposed to be suppressed for display
     * 
     */
    protected String createChildren(KB kb, String kbHref, String term, String language) {

        String suffix = "";
        if (StringUtil.emptyString(kbHref)) 
            suffix = ".html";
        StringBuilder result = new StringBuilder();
        String[] relns = {"subclass", "subrelation", "subAttribute", "subentity"};
        ArrayList forms = new ArrayList();
        if (StringUtil.isNonEmptyString(term)) {
            List tmp = null;
            for (int i = 0; i < relns.length; i++) {
                tmp = kb.askWithPredicateSubsumption(relns[i], 2, term);
                if ((tmp != null) && !tmp.isEmpty()) {
                    forms.addAll(tmp);
                }
            }
        }
        // System.out.println("5. forms == " + forms);

        if (forms != null && !forms.isEmpty()) {
            ArrayList kids = new ArrayList();
            Formula f = null;
            String s = null;
            for (Iterator it = forms.iterator(); it.hasNext();) {
                f = (Formula) it.next();
                if (!f.sourceFile.endsWith(KB._cacheFileSuffix)) {
                    s = f.getArgument(1);
                    if (isLegalForDisplay(s) && !kids.contains(s)) {
                        kids.add(s);
                    }
                }
            }
            if (!kids.isEmpty()) {
                Collections.sort(kids, String.CASE_INSENSITIVE_ORDER);
                result.append("<tr>");
                result.append(StringUtil.getLineSeparator());
                result.append("  <td valign=\"top\" class=\"label\">Children</td>");
                result.append(StringUtil.getLineSeparator());
                boolean isFirst = true;
                for (Iterator ik = kids.iterator(); ik.hasNext();) {
                    s = (String) ik.next();
                    String termHref = ("<a href=\"" 
                                       + kbHref 
                                       + StringUtil.toSafeNamespaceDelimiter(kbHref, s)
                                       + suffix 
                                       + "\">" 
                                       + showTermName(kb, s, language, true) 
                                       + "</a>");
                    if (!isFirst) result.append("<tr><td>&nbsp;</td>");                
                    result.append("<td valign=\"top\" class=\"cell\">" + termHref + "</td>");
                    String docString = getContextualDocumentation(kb, s, null);
                    docString = processDocString(kb, kbHref, language, docString, false, true);
                    result.append("<td valign=\"top\" class=\"cell\">" + docString + "</td>");
                    isFirst = false;
                }
                result.append("</tr>" + StringUtil.getLineSeparator());
            }
        }
        return result.toString();
    }

    /** *************************************************************
     * Returns a String containing HTML markup for the Instances
     * section of an HTML page displaying the definition of term in
     * kb.
     * 
     * @param kb The KB in which term is defined
     *
     * @param kbHref A String containing the constant parts of the
     * href link for term, or an empty String
     *
     * @param term A String that denotes a SUO-KIF term
     * 
     * @param language A String denoting a SUO-KIF namespace, a
     * natural language, or other type of entity that indexes
     * termFormat Strings in kb
     * 
     * @param excluded A List of terms to be excluded from the display
     * 
     * @return A String containing HTML markup, or an empty String if
     * term is supposed to be suppressed for display
     * 
     */
    protected String createInstances(KB kb, 
                                     String kbHref, 
                                     String term, 
                                     String language, 
                                     List excluded) {

        String markup = "";
        try {
            if (StringUtil.isNonEmptyString(term)) {
                String suffix = "";
                if (StringUtil.emptyString(kbHref)) 
                    suffix = ".html";
                StringBuilder result = new StringBuilder();
                List working = new ArrayList();
                working.add(term);
                List<String> extRelns = Arrays.asList("syntacticUnion", "syntacticExtension");
                List extendeds = null;
                String subent = null;
                for (String extr : extRelns) {
                    extendeds = kb.getTermsViaPredicateSubsumption(extr,
                                                                   1,
                                                                   term,
                                                                   2,
                                                                   false);
                    for (Iterator it = extendeds.iterator(); it.hasNext();) {
                        subent = (String) it.next();;
                        if (!working.contains(subent)) {
                            working.add(subent);
                        }
                    }
                }

                ArrayList instances = new ArrayList();
                String inst = null;
                Formula f = null;
                List forms = null;
                for (Iterator itw = working.iterator(); itw.hasNext();) {
                    subent = (String) itw.next();
                    forms = kb.askWithPredicateSubsumption("instance", 2, subent);
                    for (Iterator itf = forms.iterator(); itf.hasNext();) {
                        f = (Formula) itf.next();
                        if (!f.sourceFile.endsWith(KB._cacheFileSuffix)) {
                            inst = f.getArgument(1); 
                            if (!excluded.contains(inst) && isLegalForDisplay(inst)) {
                                instances.add(inst);
                            }
                        }
                    }
                }

                Set instSet = kb.getAllInstancesWithPredicateSubsumption(term, false);
                for (Iterator its = instSet.iterator(); its.hasNext();) {
                    inst = (String) its.next();
                    if (!excluded.contains(inst) && isLegalForDisplay(inst)) {
                        instSet.add(inst);
                    }
                }

                // Remove duplicate strings, if any.
                instSet.addAll(instances);
                instances.clear();
                instances.addAll(instSet);

                if (!instances.isEmpty()) {
                    sortByPresentationName(kb, getDefaultNamespace(), instances);
                    String displayName = null;
                    String xmlName = null;
                    String termHref = null;
                    for (int j = 0; j < instances.size(); j++) {
                        if (j == 0) {
                            result.append("<tr><td valign=\"top\" class=\"label\">Instances</td>");
                        }
                        else {
                            result.append("<tr><td>&nbsp;</td>");
                        }
                        inst = (String) instances.get(j);
                        displayName = showTermName(kb, inst, language, true);
                        if (displayName.contains(inst)) {
                            xmlName = showTermName(kb, inst, "XMLLabel", true);
                            if (!StringUtil.emptyString(xmlName)) 
                                displayName = xmlName;
                        }
                        termHref = ("<a href=\"" 
                                    + kbHref 
                                    + StringUtil.toSafeNamespaceDelimiter(kbHref, inst)
                                    + suffix 
                                    + "\">" 
                                    + displayName 
                                    + "</a>");
                        result.append("<td valign=\"top\" class=\"cell\">" + termHref + "</td>");
                        ArrayList clist = new ArrayList();
                        clist.add(language);
                        String docString = getContextualDocumentation(kb, inst, clist);
                        docString = processDocString(kb, 
                                                     kbHref, 
                                                     language, 
                                                     docString, 
                                                     false, 
                                                     true);
                        result.append("<td valign=\"top\" class=\"cell\">");
                        result.append(docString);
                        result.append("</td>");
                        result.append("</tr>");
                        result.append(StringUtil.getLineSeparator());
                    }
                }
                markup = result.toString();
            }
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
        return markup;
    }

    /** *************************************************************
     * Returns a String containing HTML markup for a SUO-KIF Formula.
     * 
     * @param kb The KB in which formula occurs
     *
     * @param kbHref A String containing the constant parts of the
     * href link for the constants in formula, or an empty String
     *
     * @param indentSeq A character sequence that will be used as the
     * indentation quantum for formula
     *
     * @param level The current indentation level
     *
     * @param previousTerm A String, the term that occurs sequentially
     * before currentTerm in the same level of nesting.  The value of
     * previousTerm aids in determining how a given Formula should be
     * formatted, and could be null.
     * 
     * @param currentTerm A String denoting a SUO-KIF Formula or part
     * of a Formula
     * 
     * @param context A String denoting a SUO-KIF namespace, a
     * natural language, or other type of entity that indexes
     * termFormat Strings in kb
     * 
     * @return A String containing HTML markup, or an empty String if
     * formula cannot be processed
     * 
     */
    protected String createFormula(KB kb, 
                                   String kbHref, 
                                   String indentSeq, 
                                   int level,
                                   String previousTerm,
                                   String currentTerm, 
                                   String context) {
        String result = "";
        try {
            StringBuilder sb = new StringBuilder();
            String suffix = "";
            if (StringUtil.emptyString(kbHref)) 
                suffix = ".html";
            if (StringUtil.isNonEmptyString(previousTerm) && previousTerm.matches(".*\\w+.*"))
                previousTerm = previousTerm.trim();
            if (currentTerm.matches(".*\\w+.*"))
                currentTerm = currentTerm.trim();
            if (Formula.listP(currentTerm)) {
                if (Formula.empty(currentTerm)) {
                    sb.append(currentTerm);
                }
                else {
                    Formula f = new Formula();
                    f.read(currentTerm);
                    List tuple = f.literalToArrayList();
                    boolean isQuantifiedVarlist = Formula.isQuantifier(previousTerm);
                    Iterator it = tuple.iterator();
                    while (isQuantifiedVarlist && it.hasNext()) {
                        isQuantifiedVarlist = Formula.isVariable((String) it.next());
                    }
                    if (!isQuantifiedVarlist && (level > 0)) {
                        sb.append("<br>");
                        sb.append(StringUtil.getLineSeparator());
                        for (int i = 0; i < level; i++) {
                            sb.append(indentSeq);
                        }
                    }
                    sb.append("(");
                    int i = 0;
                    String prevterm = null;
                    String nextterm = null;
                    for (it = tuple.iterator(); it.hasNext(); i++) {
                        nextterm = (String) it.next();
                        if (i > 0) sb.append(" ");
                        sb.append(createFormula(kb,
                                                kbHref,
                                                indentSeq,
                                                (level + 1),
                                                prevterm,
                                                nextterm,
                                                context));
                        prevterm = nextterm;
                    }
                    sb.append(")");
                }
            }
            else if (Formula.isVariable(currentTerm)
                     || Formula.isLogicalOperator(currentTerm)
                     || Formula.isFunction(currentTerm)
                     || StringUtil.isQuotedString(currentTerm)
                     || StringUtil.isDigitString(currentTerm)) {
                sb.append(currentTerm);
            }
            else {
                sb.append("<a href=\"");
                sb.append(kbHref);
                sb.append(StringUtil.toSafeNamespaceDelimiter(kbHref, currentTerm));
                sb.append(suffix);
                sb.append("\">");
                sb.append(showTermName(kb, currentTerm, context, true));
                sb.append("</a>");
            }
            result = sb.toString();
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
        return result;
    }

    private static Map<String, Integer> ontoRelnOrds = new HashMap<String, Integer>();
    private static Map<String, Integer> ruleRelnOrds = new HashMap<String, Integer>();

    static {
        int i = 0;
        for (String p : Arrays.asList("coa:IsSubClassOf",
                                      "coa:HasSubClass",
                                      "coa:IsSubRelatorOf",
                                      "coa:HasSubRelator",
                                      "coa:HasDomain",
                                      "coa:IsDomainOf",
                                      "coa:HasRange",
                                      "coa:IsRangeOf",
                                      "coa:IsReciprocalOf",
                                      "coa:IsOneOf",
                                      "coa:HasAvsMember",
                                      "coa:IsA",
                                      "coa:HasInstance",
                                      "coa:IsDisjointWith")) {
            ontoRelnOrds.put(p, new Integer(i));
            i++;
        }

        i = 0;
        for (String p : Arrays.asList("coa_itd:HasRuleType",
                                      "coa_itd:AppliesToDomainEntityType",
                                      "coa_itd:AppliesToOperation",
                                      "coa_itd:IsRuleTypeOf",
                                      "coa_itd:IsDomainEntityTypeForRule",
                                      "coa_itd:IsOperationForRule",
                                      "coa:ConstrainsElement",
                                      "coa:IsElementConstrainedByRule",
                                      "coa:ConstrainsClass",
                                      "coa:IsClassConstrainedByRule",
                                      "coa:ConstrainsSubClass",
                                      "coa:IsSubClassConstrainedByRule",
                                      "coa:ConstrainsSuperClass",
                                      "coa:IsSuperClassConstrainedByRule",
                                      "coa:ConstrainsReciprocalEntityClass",
                                      "coa:IsReciprocalEntityClassConstrainedByRule",
                                      "coa:ConstrainsProperty",
                                      "coa:IsPropertyConstrainedByRule",
                                      "coa:ConstrainsAVS",
                                      "coa:IsAVSConstrainedByRule",
                                      "coa:ConstrainsRelator",
                                      "coa:IsRelatorConstrainedByRule",
                                      "coa:ConstrainsDomain",
                                      "coa:IsDomainConstrainedByRule",
                                      "coa:ConstrainsRange",
                                      "coa:IsRangeConstrainedByRule",
                                      "coa:ConstrainsLinkRoleValue",
                                      "coa:IsLinkRoleValueConstrainedByRule",
                                      "coa:ConstrainsReciprocalLinkRoleValue",
                                      "coa:IsLinkRoleReciprocalValueConstrainedByRule",
                                      "coa:AllowsLinkRoleReciprocalValue",
                                      "coa:IsLinkRoleReciprocalValueAllowedByRule",
                                      "coa:ConstrainsLinkRole1ToAVS",
                                      "coa:IsAvsConstrainedToLinkRole1ByRule",
                                      "coa:ConstrainsLinkRole2ToAVS",
                                      "coa:IsAvsConstrainedToLinkRole2ByRule",
                                      "coa:ConstrainsEntity1ToClass",
                                      "coa:IsClassConstrainedToEntity1ByRule",
                                      "coa:ConstrainsEntity2ToClass",
                                      "coa:IsClassConstrainedToEntity2ByRule",
                                      "coa:EnforcesValue",
                                      "coa:IsValueEnforcedByRule",
                                      "coa:HasMinCardinality",
                                      "coa:HasMaxCardinality",
                                      "coa:HasExactCardinality",
                                      "coa:AppliesToAVS",
                                      "coa:IsAvsConstrainedByCategoryConstraint",
                                      "coa:AppliesToFlagType",
                                      "coa:IsFlagTypeConstrainedByFlagConstraint",
                                      "coa:AppliesToFlagValue",
                                      "coa:IsFlagValueConstrainedByFlagConstraint",
                                      "coa:HasEquivalentClass",
                                      "coa:IsEquivalentClassInRule",
                                      "coa:HasExcludedClass",
                                      "coa:IsExcludedClassInRule",
                                      "coa:HasEquivalentAVS",
                                      "coa:IsEquivalentAvsInRule",
                                      "coa:HasEquivalentAllowedValue",
                                      "coa:IsEquivalentAllowedValueInRule")) {
            ruleRelnOrds.put(p, new Integer(i));
            i++;
        }
    }

    private Set<String> ruleRelations = new HashSet<String>();

    /** *************************************************************
     * Returns a String containing HTML markup for the Relations
     * section of an HTML page displaying the definition of term in
     * kb.
     * 
     * @param kb The KB in which term is defined
     *
     * @param kbHref A String containing the constant parts of the
     * href link for term, or an empty String
     *
     * @param term A String that denotes a SUO-KIF term
     * 
     * @param language A String denoting a SUO-KIF namespace, a
     * natural language, or other type of entity that indexes
     * termFormat Strings in kb
     * 
     * @return A String containing HTML markup, or an empty String if
     * term is supposed to be suppressed for display
     * 
     */
    protected String createRelations(KB kb,   // for Data Dictionary format #2
                                     String kbHref, 
                                     String term, 
                                     String language) {
        /*
          long t1 = System.currentTimeMillis();

          System.out.println("ENTER DocGen.createRelations("
          + kb.name + ", "
          + term + ", "
          + language + ")");                          
        */
        String result = "";
        try {
            StringBuilder sb = new StringBuilder();
            StringBuilder relsb = new StringBuilder();
            if (!StringUtil.isQuotedString(term) && isLegalForDisplay(term)) {
                String suffix = "";
                if (StringUtil.emptyString(kbHref)) 
                    suffix = ".html";

                Set avoid = getInhibitDisplayRelations();
                Set statements = new LinkedHashSet();
                Formula f = null;
                String arg0 = null;
                String arg1 = null;
                List formulae = kb.ask("arg", 1, term);
                Iterator itf = null;
                if (formulae != null) {
                    for (itf = formulae.iterator(); itf.hasNext();) {
                        f = (Formula) itf.next();
                        arg0 = f.getArgument(0);
                        if (f.sourceFile.endsWith(KB._cacheFileSuffix)
                            || !isLegalForDisplay(arg0)
                            // || !arg0.contains(StringUtil.getW3cNamespaceDelimiter())
                            || avoid.contains(arg0)) {
                            itf.remove();
                        }
                    }


                    if (!formulae.isEmpty()) {

                        Set<String> relationsPrinted = new HashSet<String>();
                        if (ruleRelations.isEmpty()) {
                            ruleRelations.addAll(kb.getTermsViaPredicateSubsumption("coa:IsOneOf",
                                                                                    2,
                                                                                    "spi:RuleRelator",
                                                                                    1,
                                                                                    false));
                        }

                        List sectionLabels = Arrays.asList("Ontological Relationships", 
                                                           "Rule Relationships");
                        List ontoFormulae = new ArrayList();
                        List ruleFormulae = new ArrayList();

                        for (itf = formulae.iterator(); itf.hasNext();) {
                            f = (Formula) itf.next();
                            arg0 = f.getArgument(0);
                            if (ruleRelations.contains(arg0)) ruleFormulae.add(f);
                            else ontoFormulae.add(f);
                        }

                        List sections = Arrays.asList(ontoFormulae, ruleFormulae);
                        int ssize = sections.size();
                        List section = null;
                        String label = null;
                        for (int si = 0; si < ssize; si++) {
                            label = (String) sectionLabels.get(si);
                            section = (List) sections.get(si);
                            if (!section.isEmpty()) {
                                Map valmap = new HashMap();
                                ArrayList vals = null;
                                for (itf = section.iterator(); itf.hasNext();) {
                                    f = (Formula) itf.next();
                                    arg0 = f.getArgument(0);
                                    vals = (ArrayList) valmap.get(arg0);
                                    if (vals == null) {
                                        vals = new ArrayList();
                                        valmap.put(arg0, vals);
                                    }
                                    vals.add(f.getArgument(2));
                                }
                                if (!valmap.isEmpty()) {
                                    List sortedKeys = new ArrayList();
                                    List keys = new ArrayList(valmap.keySet());
                                    sortByPresentationName(kb, language, keys);

                                    boolean isFirstLine = true;

                                    Map predOrdMap = (label.matches(".*(?i)ontological.*")
                                                      ? ontoRelnOrds
                                                      : ruleRelnOrds);

                                    Iterator itk = null;
                                    for (itk = keys.iterator(); itk.hasNext();) {
                                        arg0 =  (String) itk.next();
                                        Integer igr = (Integer) predOrdMap.get(arg0);
                                        if (igr != null) {
                                            if (sortedKeys.isEmpty()) sortedKeys.add(arg0);
                                            else {
                                                int igrval = igr.intValue();
                                                int sklen = sortedKeys.size();
                                                int lasti = (sklen - 1);
                                                int ordval = -1;
                                                String sk = null;
                                                for (int i = 0; i < sklen; i++) {
                                                    sk = (String) sortedKeys.get(i);
                                                    ordval = 
                                                        ((Integer) predOrdMap.get(sk)).intValue();
                                                    if (igrval < ordval) {
                                                        sortedKeys.add(i, arg0);
                                                        break;
                                                    }
                                                    else if (i == lasti) {
                                                        sortedKeys.add(arg0);
                                                    }
                                                }
                                            }
                                            itk.remove();
                                        }
                                    }
                                    sortedKeys.addAll(keys);
                                    for (itk = sortedKeys.iterator(); itk.hasNext();) {
                                        arg0 = (String) itk.next();
                                        if (!relationsPrinted.contains(arg0)) {
                                            vals = (ArrayList) valmap.get(arg0);
                                            if ((vals != null) && !vals.isEmpty()) {
                                                sortByPresentationName(kb, language, vals);
                                                        
                                                relsb = new StringBuilder();
                                                relsb.append("<a href=\"");
                                                relsb.append(kbHref);
                                                relsb.append(StringUtil.toSafeNamespaceDelimiter(kbHref, arg0));
                                                relsb.append(suffix);
                                                relsb.append("\">");
                                                relsb.append(showTermName(kb,arg0,language,true));
                                                relsb.append("</a>");

                                                int m = 0;
                                                for (Iterator itv = vals.iterator(); 
                                                     itv.hasNext(); 
                                                     m++) {
                                                    String s = (String) itv.next();
                                                    String arg2ToPrint = 
                                                        (s.matches("\\s")
                                                         ? processDocString(kb, 
                                                                            kbHref, 
                                                                            language, 
                                                                            s,
                                                                            false,
                                                                            true)
                                                         : createFormula(kb, 
                                                                         kbHref, 
                                                                         StringUtil.concatN("&nbsp;", 4),
                                                                         0,
                                                                         null,
                                                                         s, 
                                                                         language));

                                                    if (StringUtil.isDigitString(arg2ToPrint))
                                                        arg2ToPrint = StringUtil.quote(arg2ToPrint);

                                                    if (isFirstLine) {
                                                        sb.append("<tr class=\"reltype\">");
                                                        sb.append(getLineSeparator());
                                                        sb.append("  <td class=\"label\">");
                                                        sb.append(getLineSeparator());
                                                        sb.append("    ");
                                                        sb.append((String) sectionLabels.get(si));
                                                        sb.append(getLineSeparator());
                                                        sb.append("  </td>");
                                                        sb.append(getLineSeparator());
                                                        sb.append("  <td>&nbsp;</td>");
                                                        sb.append(getLineSeparator());
                                                        sb.append("</tr>");
                                                        sb.append(getLineSeparator());
                                                        isFirstLine = false;
                                                    }
                                                    sb.append("<tr>");
                                                    sb.append(getLineSeparator());

                                                    // predicate
                                                    sb.append("  <td class=\"reln\">");
                                                    sb.append(getLineSeparator());
                                                    sb.append("    ");
                                                    sb.append((m == 0) 
                                                              ? relsb
                                                              : "&nbsp;");
                                                    sb.append(getLineSeparator());
                                                    sb.append("  </td>");
                                                    sb.append(getLineSeparator());

                                                    // arg2
                                                    sb.append("  <td class=\"arg2\">");
                                                    sb.append(getLineSeparator());
                                                    sb.append("    ");
                                                    sb.append(arg2ToPrint);
                                                    sb.append(getLineSeparator());
                                                    sb.append("  </td>");
                                                    sb.append(getLineSeparator());
                                                    sb.append("</tr>");
                                                    sb.append(getLineSeparator());

                                                }
                                            }
                                            relationsPrinted.add(arg0);
                                        }
                                    }
                                }
                            }
                        }
                        result = sb.toString();
                    }
                }
            }
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
        /*
          System.out.println("EXIT DocGen.createRelations("
          + kb.name + ", "
          + kbHref + ", "
          + term + ", "
          + language + ")");
          System.out.println("  > "
          + ((System.currentTimeMillis() - t1) / 1000.0)
          + " seconds elapsed time");
        */
        return result;
    }

    /** *************************************************************
     * Returns a String containing HTML markup for the Relations
     * section of an HTML page displaying the definition of term in
     * kb.
     * 
     * @param kb The KB in which term is defined
     *
     * @param kbHref A String containing the constant parts of the
     * href link for term, or an empty String
     *
     * @param term A String that denotes a SUO-KIF term
     * 
     * @param language A String denoting a SUO-KIF namespace, a
     * natural language, or other type of entity that indexes
     * termFormat Strings in kb
     * 
     * @return A String containing HTML markup, or an empty String if
     * term is supposed to be suppressed for display
     * 
     */
    protected String createRelations(KB kb, 
                                     String kbHref, 
                                     String term, 
                                     String language,
                                     Set formatTokens) {
        String result = "";
        try {
            if ((formatTokens != null) && formatTokens.contains(F_DD2)) {
                result = createRelations(kb, kbHref, term, language);
            }
            else {
                if (isLegalForDisplay(term)) {
                    String suffix = "";
                    if (StringUtil.emptyString(kbHref)) 
                        suffix = ".html";
                    ArrayList relations = getPredicates(kb);

                    // System.out.println(StringUtil.getLineSeparator() + "relations == " + relations + StringUtil.getLineSeparator());

                    if (!relations.isEmpty()) {
                        StringBuilder sb = new StringBuilder();
                        Set avoid = getInhibitDisplayRelations();
                        Map map = new HashMap();
                        String relation = null;
                        Iterator itr = null;
                        for (itr = relations.iterator(); itr.hasNext();) {
                            relation = (String) itr.next();

                            // boolean isFormula = relation.matches(".*(?i)kif.*");

                            if (!avoid.contains(relation)) {
                                ArrayList statements = kb.askWithPredicateSubsumption(relation, 
                                                                                      1, 
                                                                                      term);

                                // if (isFormula) {
                                //     System.out.println("\n" + "statements == " + statements + "\n");
                                // }

                                if (!statements.isEmpty()) {
                                    ArrayList vals = new ArrayList();
                                    for (Iterator its = statements.iterator(); its.hasNext();) {
                                        Formula f = (Formula) its.next();
                                        if (!f.sourceFile.endsWith(KB._cacheFileSuffix)) {
                                            vals.add(f.getArgument(2));
                                        }
                                    }
                                    if (!vals.isEmpty()) {
                                        map.put(relation, vals);
                                    }
                                }
                            }
                        }
                        if (!map.isEmpty()) {
                            List keys = new ArrayList(map.keySet());
                            sortByPresentationName(kb, language, keys);
                            ArrayList vals = null;
                            String s = null;
                            boolean firstLine = true;
                            for (itr = keys.iterator(); itr.hasNext();) {
                                relation = (String) itr.next();
                                vals = (ArrayList) map.get(relation);
                                if ((vals != null) && !vals.isEmpty()) {
                                    String relnHref = ("<a href=\"" 
                                                       + kbHref 
                                                       + StringUtil.toSafeNamespaceDelimiter(kbHref, relation)
                                                       + suffix 
                                                       + "\">" 
                                                       + showTermName(kb,relation,language,true) 
                                                       + "</a>");
                                    int m = 0;
                                    for (Iterator itv = vals.iterator(); itv.hasNext(); m++) {
                                        s = (String) itv.next();
                                        String termHref = createFormula(kb, 
                                                                        kbHref, 
                                                                        "&nbsp;&nbsp;&nbsp;&nbsp;",
                                                                        0,
                                                                        null,
                                                                        s, 
                                                                        language);

                                        if (firstLine) {
                                            sb.append("<tr><td valign=\"top\" class=\"label\">"
                                                      + "Relations"
                                                      + "</td>");
                                            firstLine = false;
                                        }
                                        else {
                                            sb.append("<tr><td>&nbsp;</td>");
                                        }
                                        // System.out.println( relnHref );
                                        // System.out.println( termHref );
                                        if (m == 0) {
                                            sb.append("<td valign=\"top\" class=\"cell\">" 
                                                      + relnHref 
                                                      + "</td>");
                                        }
                                        else {
                                            sb.append("<td valign=\"top\" class=\"cell\">&nbsp;</td>");
                                        }
                                        sb.append("<td valign=\"top\" class=\"cell\">" 
                                                  + termHref
                                                  + "</td></tr>"
                                                  + StringUtil.getLineSeparator());
                                    }
                                }
                            }                
                        }
                        result = sb.toString();
                    }
                }
            }
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
        return result;
    }

    /** *************************************************************
     * Returns a String containing HTML markup for the Cardinality
     * field of an HTML page displaying the definition of term in kb.
     * 
     * @param kb The KB in which term is defined
     *
     * @param kbHref A String containing the constant parts of the
     * href link for term, or an empty String
     *
     * @param term A String that denotes a SUO-KIF term
     * 
     * @param context A String denoting a SUO-KIF namespace, a natural
     * language, or other type of entity that indexes termFormat
     * Strings in kb -- this parameter is currently ignored, since
     * LocalInstance terms are already highly specific
     * 
     * @return A String containing HTML markup, or an empty String if
     * no markup can be generated
     * 
     */
    protected String showCardinalityCell(KB kb, String kbHref, String term, String context) {
        /*
          System.out.println("ENTER DocGen.showCardinalityCell("
          + kb.name + ", "
          + kbHref + ", "
          + term + ", "
          + context + ")");
        */
        String cardVal = "";
        try {
            ArrayList cardForms = kb.askWithPredicateSubsumption("hasExactCardinality", 1, term);
            // kb.askWithRestriction(0,"exactCardinality",2,term);
            if (cardForms != null && !cardForms.isEmpty()) {
                Formula f = (Formula) cardForms.get(0);
                // if (context.equals("") || context.equals(f.getArgument(1)))             
                //     return f.getArgument(3);
                cardVal = f.getArgument(2);
            }
            else {
                String minCard = "0";
                String maxCard = "n";
                StringBuilder result = new StringBuilder();
                cardForms = kb.askWithPredicateSubsumption("hasMinCardinality", 1, term);
                // kb.askWithRestriction(0,"minCardinality",2,term);
                if (cardForms != null && cardForms.size() > 0) {
                    Formula f = (Formula) cardForms.get(0);
                    // if (context == "" || context.equals(f.getArgument(1)))             
                    //     minCard = f.getArgument(3);
                    minCard = f.getArgument(2);
                }
                cardForms = kb.askWithPredicateSubsumption("hasMaxCardinality", 1, term);
                // kb.askWithRestriction(0,"maxCardinality",2,term);
                if (cardForms != null && cardForms.size() > 0) {
                    Formula f = (Formula) cardForms.get(0);
                    // if (context.equals("") || context.equals(f.getArgument(1)))             
                    //     maxCard = f.getArgument(3);
                    maxCard = f.getArgument(2);
                }
                cardVal = (minCard + "-" + maxCard);
            }
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
        /*
          System.out.println("EXIT DocGen.showCardinalityCell("
          + kb.name + ", "
          + kbHref + ", "
          + term + ", "
          + context + ")");
          System.out.println("  ==> " + cardVal);
        */
        return cardVal;
    }

    /** *************************************************************
     * Returns a String containing HTML markup for a single table row
     * in the Composite Component section of an HTML page displaying
     * the partial definition of term in kb.
     * 
     * @param kb The KB in which term is defined
     *
     * @param kbHref A String containing the constant parts of the
     * href link for term, or an empty String
     *
     * @param term A String that denotes a SUO-KIF term
     * 
     * @param ident A int value that determines the depth to which
     * term will be indented when displayed
     * 
     * @param language A String denoting a SUO-KIF namespace, a
     * natural language, or other type of entity that indexes
     * termFormat Strings in kb
     * 
     * @return A String containing HTML markup, or an empty String if
     * term is supposed to be suppressed for display
     * 
     */
    protected String createCompositeComponentLine(KB kb, 
                                                  String kbHref, 
                                                  String term, 
                                                  int indent, 
                                                  String language) {
        String result = "";
        try {
            StringBuilder sb = new StringBuilder();
            String suffix = "";
            if (StringUtil.emptyString(kbHref)) 
                suffix = ".html";
            sb.append("<tr>");
            sb.append(StringUtil.getLineSeparator());
            sb.append("  <td></td>");
            sb.append(StringUtil.getLineSeparator());

            sb.append("  <td valign=\"top\" class=\"cell\">");
            sb.append(StringUtil.getLineSeparator());

            String parentClass = "";
            ArrayList instanceForms = kb.askWithPredicateSubsumption("instance", 1, term);

            // System.out.println("1. instanceForms == " + instanceForms);

            if (instanceForms != null && instanceForms.size() > 0) {
                Formula f = (Formula) instanceForms.get(0);
                parentClass = f.getArgument(2);
            }
            ArrayList termForms = null;
            if (StringUtil.isNonEmptyString(term)) {
                termForms = kb.askWithTwoRestrictions(0, "termFormat", 1, "XMLLabel", 2, term);
            }
            if (termForms != null) {
                boolean isAttribute = isXmlAttribute(kb, term);
                if (!isAttribute) isAttribute = isXmlAttribute(kb, parentClass);
                for (Iterator ita = termForms.iterator(); ita.hasNext();) {               
                    Formula f = (Formula) ita.next();
                    sb.append(indentChars("&nbsp;&nbsp;",indent));
                    String termFormat = StringUtil.removeEnclosingQuotes(f.getArgument(3));
                    sb.append("<a href=\"");
                    sb.append(kbHref); 
                    sb.append(StringUtil.toSafeNamespaceDelimiter(kbHref, parentClass));
                    sb.append(suffix);
                    sb.append("\">");
                    if (isAttribute) sb.append("<span class=\"attribute\">");
                    sb.append(termFormat);
                    if (isAttribute) sb.append("</span>");
                    sb.append("</a>");                      
                }
            }
            sb.append("  </td>");
            sb.append(StringUtil.getLineSeparator());

            sb.append("  <td valign=\"top\" class=\"cell\">");
            sb.append(StringUtil.getLineSeparator());
            ArrayList clist = new ArrayList();
            clist.add(language);
            String docString = getContextualDocumentation(kb, term, clist);
            docString = processDocString(kb, kbHref, language, docString, false, true);
            sb.append("    ");
            sb.append(docString); 
            sb.append(StringUtil.getLineSeparator());
            sb.append("  </td>");
            sb.append(StringUtil.getLineSeparator());

            sb.append("  <td valign=\"top\" class=\"card\">");
            if (indent > 0)        
                sb.append(showCardinalityCell(kb, kbHref, term, ""));
            sb.append("  </td>");
            sb.append(StringUtil.getLineSeparator());

            sb.append("  <td valign=\"top\" class=\"cell\">");
            String dataTypeName = getFirstDatatype(kb, term);
            if (StringUtil.isNonEmptyString(dataTypeName)) {
                String dtToPrint = showTermName(kb, dataTypeName, language, true);                
                sb.append("<a href=\"");
                sb.append(kbHref);
                sb.append(StringUtil.toSafeNamespaceDelimiter(kbHref, dataTypeName));
                sb.append(suffix);
                sb.append("\">");
                sb.append(dtToPrint);
                sb.append("</a>");
                String xsdType = getClosestXmlDataType(kb, dataTypeName);
                if (StringUtil.isNonEmptyString(xsdType)) {
                    sb.append(" (");
                    sb.append(StringUtil.kifToW3c(xsdType));
                    sb.append(")");
                }
                sb.append(StringUtil.getLineSeparator());
            }
            sb.append("  </td>");
            sb.append(StringUtil.getLineSeparator());

            sb.append("</tr>");
            sb.append(StringUtil.getLineSeparator());

            result = sb.toString();
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
        return result;
    }

    /** *************************************************************
     * Returns the termFormat entry for term in kb and language,
     * otherwise returns the termFormat entry for term in English,
     * otherwise just returns the term name.
     * 
     * @param kb The KB in which term is defined
     *
     * @param term A String that denotes a SUO-KIF term
     * 
     * @param language A String denoting a SUO-KIF namespace, a
     * natural language, or another type of entity that contextualizes
     * or indexes termFormat Strings in kb
     * 
     * @param withSpanTags If true, the returned String is wrapped in
     * HTML span tags that allow additional formatting for term via a
     * style sheet
     * 
     * @return A String providing a context-specific name for term,
     * possibly including HTML markup, or just term if no
     * context-specific form can be found or produced
     * 
     */
    public String showTermName(KB kb, String term, String language, boolean withSpanTags) {

        String ans = term;
        try {
            ans = StringUtil.removeEnclosingQuotes(ans);
            String termFormat = getFirstTermFormat(kb, term, Arrays.asList(language));
            if (StringUtil.emptyString(termFormat)) {
                termFormat = (String) kb.getTermFormatMap(language).get(term);
            }
            if (StringUtil.emptyString(termFormat)) {
                termFormat = (String) kb.getTermFormatMap("EnglishLanguage").get(term);
            }
            if (StringUtil.isNonEmptyString(termFormat)) {
                ans = StringUtil.removeEnclosingQuotes(termFormat);
            }
            else {
                String namespace = getTermNamespace(kb, term);
                if (StringUtil.isNonEmptyString(namespace) 
                    && (namespace.equals(language)
                        || namespace.equals(getDefaultNamespace()))) {
                    ans = stripNamespacePrefix(kb, term);
                }
                else {
                    ans = StringUtil.kifToW3c(term);
                }
            }
            if (getCodedIdentifiers(kb).contains(term)) {  //(term, "IsoCode")) {
                List<String> delims = Arrays.asList(StringUtil.getW3cNamespaceDelimiter(), 
                                                    StringUtil.getKifNamespaceDelimiter());
                for (String delim : delims) {
                    int idx = ans.indexOf(delim);
                    if (idx > -1) {
                        idx += delim.length();
                        if (idx < ans.length()) {
                            ans = ans.substring(idx);
                            break;
                        }
                    }
                }
            }
            if (withSpanTags) {
                if (StringUtil.isNonEmptyString(ans)) {
                    if (isXmlAttribute(kb, term)) {
                        ans = ("<span class=\"attribute\">" + ans + "</span>");
                    }
                }
            }
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
        return ans;
    }

    /** *************************************************************
     * Returns the termFormat entry for term in kb and language,
     * otherwise returns the termFormat entry for term in English,
     * otherwise just returns the term name.
     * 
     * @param kb The KB in which term is defined
     *
     * @param term A String that denotes a SUO-KIF term
     * 
     * @param language A String denoting a SUO-KIF namespace, a
     * natural language, or other type of entity that indexes
     * termFormat Strings in kb
     * 
     * @return A String providing a context-specific name for term,
     * possibly including HTML markup, or just term
     * 
     */
    public String showTermName(KB kb, String term, String language) {
        return showTermName(kb, term, language, false);
    }

    /** *************************************************************
     * Returns a String containing HTML markup for a hierarchy or tree
     * display of terms that denote nested composite components.
     * 
     * @param kb The KB in which term is defined
     *
     * @param kbHref A String containing the constant parts of the
     * href link for term, or an empty String
     *
     * @param hier A List containing term names and representing one
     * sub-branch in a tree
     * 
     * @param language A String denoting a SUO-KIF namespace, a
     * natural language, or other type of entity that indexes
     * termFormat Strings in kb
     * 
     * @return A String containing HTML markup, or an empty String if
     * no markup can be generated
     * 
     */
    protected String formatCompositeHierarchy(KB kb, 
                                              String kbHref, 
                                              List hier, 
                                              String language) {
        String markup = "";
        try {
            StringBuilder result = new StringBuilder();
            for (Iterator it = hier.iterator(); it.hasNext();) {
                AVPair avp = (AVPair) it.next();
                // if (!kb.isInstanceOf(avp.attribute, "XmlSequenceElement")) {
                result.append(createCompositeComponentLine(kb,
                                                           kbHref,
                                                           avp.attribute,
                                                           Integer.parseInt(avp.value),
                                                           language));
                // }
            }
            markup = result.toString();
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
        return markup;
    }

    /** *************************************************************
     * Recursively computes and then returns a List that constitutes
     * the graph containing those XML elements and attributes
     * syntactically subordinate to term in kb.
     * 
     * @param kb The KB in which term is defined
     *
     * @param term A String that denotes a SUO-KIF term
     * 
     * @param isAttribute If true, this parameter indicates that term
     * denotes an XML attribute
     * 
     * @param int An integer indicating the depth or level to which
     * term should be indented when displayed
     * 
     * @return A List containing two Lists, the first of which is a
     * List of terms that denote XML attributes, and the second of
     * which is a list of terms that denote XML elements
     * 
     */
    protected ArrayList createCompositeRecurse(KB kb, 
                                               String term, 
                                               boolean isAttribute, 
                                               int indent) {
        ArrayList pair = new ArrayList();
        try {
            ArrayList attrs = new ArrayList();
            ArrayList elems = new ArrayList();
            pair.add(attrs);
            pair.add(elems);

            AVPair avp = new AVPair();
            avp.attribute = term;
            avp.value = Integer.toString(indent);
            if (isAttribute) attrs.add(avp);
            else elems.add(avp);

            List terms = null;
            String nextTerm = null;
            Iterator it = null;
            List<String> preds = Arrays.asList("subordinateXmlAttribute", "subordinateXmlElement");
            for (String pred : preds) {
                terms = kb.getTermsViaPredicateSubsumption(pred, 2, term, 1, true);
                Collections.sort(terms, String.CASE_INSENSITIVE_ORDER);
                boolean isAttributeList = pred.equals("subordinateXmlAttribute");
                for (it = terms.iterator(); it.hasNext();) {
                    nextTerm = (String) it.next();
                    // This should return without children for
                    // subordinateXmlAttribute, since attributes
                    // don't have child elements.
                    ArrayList newPair = 
                        createCompositeRecurse(kb, nextTerm, isAttributeList, (indent + 1));
                    attrs.addAll((ArrayList) newPair.get(0));
                    elems.addAll((ArrayList) newPair.get(1));
                }
            }
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
        return pair;
    }

    /** *************************************************************
     * Returns a List that constitutes the graph containing those XML
     * elements and attributes syntactically subordinate to term in
     * kb.
     * 
     * @param kb The KB in which term is defined
     *
     * @param term A String that denotes a SUO-KIF term
     * 
     * @param isAttribute If true, this parameter indicates that term
     * denotes an XML attribute
     * 
     * @param int An integer indicating the depth or level to which
     * term should be indented when displayed
     * 
     * @return A String containing HTML markup, or an empty String if
     * no markup can be generated
     * 
     */
    protected boolean isXmlAttribute(KB kb, String term) {
        boolean ans = false;
        try {
            String kif = term;
            List terms = kb.getTermsViaPredicateSubsumption("subordinateXmlAttribute",
                                                            1,
                                                            kif,
                                                            2,
                                                            true);
            ans = !terms.isEmpty();
            if (!ans) {
                kif = getFirstGeneralTerm(kb, kif);
                if (StringUtil.isNonEmptyString(kif)) {
                    terms = kb.getTermsViaPredicateSubsumption("subordinateXmlAttribute",
                                                               1,
                                                               kif,
                                                               2,
                                                               true);
                    ans = !terms.isEmpty();
                }
            }
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
        return ans;
    }

    /** *************************************************************
     * Returns a String containing HTML markup for a row displaying a
     * contained component in an HTML page displaying the partial
     * definition of instance in kb.
     * 
     * @param kb The KB in which term is defined
     *
     * @param kbHref A String containing the constant parts of the
     * href link for term, or an empty String
     *
     * @param containingComp A String that denotes the term that
     * contains, or is syntactically superordinate to, instance
     * 
     * @param instance A String that denotes a SUO-KIF term
     * 
     * @param ident An int value that determines the depth to which
     * instance will be indented when displayed
     * 
     * @param language A String denoting a SUO-KIF namespace, a
     * natural language, or other type of entity that indexes
     * termFormat Strings in kb
     * 
     * @return A String containing HTML markup, or an empty String if
     * no markup can be generated
     * 
     */
    protected String createContainingCompositeComponentLine(KB kb, 
                                                            String kbHref, 
                                                            String containingComp,
                                                            // List containerData,
                                                            String instance, 
                                                            int indent, 
                                                            String language) {
        /*
          System.out.println("DocGen.createContainingCompositeCompoentLine("
          + kb
          + ", \"" + kbHref
          + "\", \"" + containingComp
          + "\", \"" + instance
          + "\", " + indent
          + "\", \"" + language + "\")");
        */

        String result = "";

        try {
            if (StringUtil.isNonEmptyString(instance)) {
                StringBuilder sb = new StringBuilder();
                String suffix = (StringUtil.emptyString(kbHref) ? ".html" : "");
                ArrayList docForms = kb.askWithRestriction(0,"documentation",1,instance);
                Formula f = null;
                String context = null;
                for (Iterator it = docForms.iterator(); it.hasNext();) {
                    f = (Formula) it.next();
                    context = f.getArgument(2);
                    if (context.equals(containingComp)) {

                        sb.append("<tr>");
                        sb.append(StringUtil.getLineSeparator());
                        sb.append("  <td>&nbsp;</td>");
                        sb.append(StringUtil.getLineSeparator());
                        sb.append("  <td valign=\"top\" class=\"cell\">");
                        sb.append(indentChars("&nbsp;&nbsp;", indent));
                        sb.append("<a href=\"");
                        sb.append(kbHref);
                        sb.append(StringUtil.toSafeNamespaceDelimiter(kbHref, containingComp));
                        sb.append(suffix);
                        sb.append("\">");
                        sb.append(showTermName(kb,containingComp,language,true));
                        sb.append("</a></td>");
                        sb.append(StringUtil.getLineSeparator());
                        sb.append("  <td valign=\"top\" class=\"cell\">");
                        sb.append(StringUtil.getLineSeparator());
                        sb.append("    ");
                        sb.append(processDocString(kb, 
                                                   kbHref, 
                                                   language, 
                                                   f.getArgument(3), 
                                                   false, 
                                                   true));
                        sb.append(StringUtil.getLineSeparator());
                        sb.append("  </td>");
                        sb.append(StringUtil.getLineSeparator());
                        sb.append("  <td valign=\"top\" class=\"card\">");
                        sb.append(showCardinalityCell(kb,kbHref,instance,context));
                        sb.append("  </td>");
                        sb.append(StringUtil.getLineSeparator());
                        sb.append("  <td>&nbsp;</td>");
                        sb.append(StringUtil.getLineSeparator());
                        sb.append("</tr>");
                        sb.append(StringUtil.getLineSeparator());
                    }
                }
                result = sb.toString();
            }
            // System.out.println("  ==> " + result);
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
        return result;
    }

    /** *************************************************************
     *  Given the SUO-KIF statements:
     * 
     * (hasXmlElement PartyDescriptor LocalInstance_2_459)
     * (datatype LocalInstance_2_459 PartyId)
     * (documentation LocalInstance_2_459
     * PartyDescriptor "A Composite containing details...")
     * 
     * show PartyDescriptor as one of the "containing
     * composites" of PartyId, and show the documentation for
     * the instance node next to the parent composite.
     */
    protected String formatContainingComposites(KB kb, 
                                                String kbHref, 
                                                ArrayList containing, 
                                                String composite, 
                                                String language) {

        /*
          System.out.println("ENTER DocGen.formatContainingComposites("
          + kb.name + ", "
          + kbHref + ", "
          + containing + ", "
          + composite + ", "
          + language + ")");
        */

        String resultStr = "";
        try {
            StringBuilder result = new StringBuilder();
            List instances = getFirstSpecificTerms(kb, composite);
            String ccomp = null;
            String inst = null;
            Iterator it2 = null;
            for (Iterator it1 = containing.iterator(); it1.hasNext();) {
                ccomp = (String) it1.next();
                for (it2 = instances.iterator(); it2.hasNext();) {
                    inst = (String) it2.next();
                    result.append(createContainingCompositeComponentLine(kb,
                                                                         kbHref,
                                                                         ccomp, 
                                                                         inst,
                                                                         0,
                                                                         language));
                }
            }
            resultStr = result.toString();
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }

        /*
          System.out.println("EXIT DocGen.formatContainingComposites("
          + kb.name + ", "
          + kbHref + ", "
          + containing + ", "
          + composite + ", "
          + language + ")");
          System.out.println("  ==> " + resultStr);
        */

        return resultStr;
    }

    /** *************************************************************
     * Returns true if term should be skipped over during printing,
     * else returns false.
     *
     */
    protected static boolean isSkipNode(KB kb, String term) {
        boolean ans = false;
        try {
            ans = (isInstanceOf(kb, term, "XmlSequenceElement")
                   || isInstanceOf(kb, term, "XmlChoiceElement"));
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
        return ans;
    }

    /** *************************************************************
     * Travels up the HasXmlElement and HasXmlAttribute relation
     * hierarchies to collect all parents, and returns them in an
     * ArrayList.
     *
     * @return An ArrayList of terms, which could be empty
     */
    protected ArrayList getContainingComposites(KB kb, String term) {
        ArrayList result = new ArrayList();
        try {
            result.addAll(kb.getTermsViaPredicateSubsumption("syntacticSubordinate",
                                                             1,
                                                             term,
                                                             2,
                                                             true));
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
        return result;
    }

    /** *************************************************************
     * Returns a String containing HTML markup for the Belongs to Class
     * section of an HTML page displaying the partial
     * definition of term in kb.
     * 
     * @param kb The KB in which term is defined
     *
     * @param kbHref A String containing the constant parts of the
     * href link for term, or an empty String
     *
     * @param term A String that denotes a SUO-KIF term
     * 
     * @param language A String denoting a SUO-KIF namespace, a
     * natural language, or other type of entity that indexes
     * termFormat Strings in kb
     * 
     * @param parents A Set containing the terms displayed in the
     * Parent field for term, to avoid duplication between the Parents
     * field and the Belongs to Class field
     * 
     * @return A String containing HTML markup, or an empty String if
     * no markup can be generated
     * 
     */
    protected String createBelongsToClass(KB kb, 
                                          String kbHref, 
                                          String term, 
                                          String language,
                                          Set<String> parents) {
        String markup = "";
        try {
            String suffix = "";
            if (StringUtil.emptyString(kbHref)) 
                suffix = ".html";
            String className = getNearestContainingClass(kb, term);
            if (StringUtil.isNonEmptyString(className) 
                && isLegalForDisplay(className)
                && ((parents == null)
                    || !parents.contains(className))) {
                StringBuilder result = new StringBuilder();
                result.append("<tr>");
                result.append(StringUtil.getLineSeparator());
                result.append("  <td valign=\"top\" class=\"label\">");
                result.append(StringUtil.getLineSeparator());
                result.append("    Belongs to Class");
                result.append(StringUtil.getLineSeparator());
                result.append("  </td>");
                result.append(StringUtil.getLineSeparator());
                result.append("  <td valign=\"top\" class=\"cell\">");
                result.append(StringUtil.getLineSeparator());
                result.append("<a href=\""); 
                result.append(kbHref);
                result.append(StringUtil.toSafeNamespaceDelimiter(kbHref, className));
                result.append(suffix); 
                result.append("\">");
                result.append(showTermName(kb, className, language, true));
                result.append("</a>");        
                result.append("  </td>");
                result.append(StringUtil.getLineSeparator());
                result.append("  <td></td><td></td><td></td>");
                result.append(StringUtil.getLineSeparator());
                result.append("</tr>"); 
                result.append(StringUtil.getLineSeparator());

                markup = result.toString();
            }
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
        return markup;
    }

    /** *************************************************************
     * Returns a String containing HTML markup for the Belongs to Class
     * section of an HTML page displaying the partial
     * definition of term in kb.
     * 
     * @param kb The KB in which term is defined
     *
     * @param kbHref A String containing the constant parts of the
     * href link for term, or an empty String
     *
     * @param term A String that denotes a SUO-KIF term
     * 
     * @param language A String denoting a SUO-KIF namespace, a
     * natural language, or other type of entity that indexes
     * termFormat Strings in kb
     * 
     * @return A String containing HTML markup, or an empty String if
     * no markup can be generated
     * 
     */
    protected String createBelongsToClass(KB kb, 
                                          String kbHref, 
                                          String term, 
                                          String language) {

        return createBelongsToClass(kb, kbHref, term, language, null);
    }

    protected ArrayList findContainingComposites(KB kb, String term) {

        ArrayList ans = new ArrayList();
        /*          */
        try {
            if (StringUtil.isNonEmptyString(term)) {
                List accumulator = getContainingComposites(kb, term);
                if (accumulator.isEmpty()) {
                    accumulator.addAll(getFirstSpecificTerms(kb, term));
                }
                List working = new ArrayList();
                String term2 = null;
                String term3 = null;
                while (!accumulator.isEmpty()) {
                    working.clear();
                    working.addAll(accumulator);
                    accumulator.clear();
                    for (int i = 0; i < working.size(); i++) {
                        term2 = (String) working.get(i);
                        List compArr1 = getContainingComposites(kb, term2);
                        for (int j = 0; j < compArr1.size(); j++) {
                            term3 = (String) compArr1.get(j);
                            if (isSkipNode(kb, term3)) {
                                accumulator.add(term3);
                            }
                            if (!ans.contains(term3)) {
                                ans.add(term3);
                            }
                        }
                    }
                }
            }
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
        return ans;
    }

    /** *************************************************************
     * Create an HTML page that lists information about a particular term,
     * with a limit on how many statements of each type should be
     * displayed.
     */
    public String createAllStatements(KB kb, 
                                      String kbHref, 
                                      String term, 
                                      int limit) {

        StringBuilder result = new StringBuilder();

        if (StringUtil.isNonEmptyString(term)) {
            String language = "EnglishLanguage";
            int localLimit = limit;
            String limitString = "";
            ArrayList forms = null;
            for (int argnum = 2; argnum < 6; argnum++) {
                localLimit = limit;
                limitString = "";
                forms = kb.ask("arg",argnum,term);
                if (forms != null) {
                    if (forms.size() < localLimit) 
                        localLimit = forms.size();
                    else
                        limitString = ("<br>Display limited to " 
                                       + localLimit
                                       + " statements of each type.<p>"
                                       + StringUtil.getLineSeparator());
                    for (int i = 0; i < localLimit; i++) {
                        Formula form = (Formula) forms.get(i);
                        result.append(LanguageFormatter.htmlParaphrase(kbHref,
                                                                       form.theFormula, 
                                                                       kb.getFormatMap(language), 
                                                                       kb.getTermFormatMap(language), 
                                                                       kb,
                                                                       language) 
                                      + "<br>"
                                      + StringUtil.getLineSeparator());
                    }
                }
                result.append(limitString);
            }

            localLimit = limit;
            limitString = "";
            forms = kb.ask("ant",0,term);
            if (forms != null) {
                if (forms.size() < localLimit) 
                    localLimit = forms.size();
                else
                    limitString = ("<br>Display limited to " 
                                   + localLimit
                                   + " statements of each type.<p>"
                                   + StringUtil.getLineSeparator());
                for (int i = 0; i < localLimit; i++) {
                    Formula form = (Formula) forms.get(i);
                    result.append(LanguageFormatter.htmlParaphrase(kbHref,
                                                                   form.theFormula, 
                                                                   kb.getFormatMap(language), 
                                                                   kb.getTermFormatMap(language), 
                                                                   kb,
                                                                   language) 
                                  + StringUtil.getLineSeparator());
                }
            }
            result.append(limitString);

            localLimit = limit;
            limitString = "";
            forms = kb.ask("cons",0,term);
            if (forms != null) {
                if (forms.size() < localLimit) 
                    localLimit = forms.size();
                else
                    limitString = ("<br>Display limited to " 
                                   + localLimit
                                   + " statements of each type.<p>"
                                   + StringUtil.getLineSeparator());
                for (int i = 0; i < localLimit; i++) {
                    Formula form = (Formula) forms.get(i);
                    result.append(LanguageFormatter.htmlParaphrase(kbHref,
                                                                   form.theFormula, 
                                                                   kb.getFormatMap(language), 
                                                                   kb.getTermFormatMap(language), 
                                                                   kb,
                                                                   language) 
                                  + StringUtil.getLineSeparator());
                }
            }
            result.append(limitString);

            localLimit = limit;
            limitString = "";
            forms = kb.ask("stmt",0,term);
            if (forms != null) {
                if (forms.size() < localLimit) 
                    localLimit = forms.size();
                else
                    limitString = ("<br>Display limited to " 
                                   + localLimit
                                   + " statements of each type.<p>"
                                   + StringUtil.getLineSeparator());
                for (int i = 0; i < localLimit; i++) {
                    Formula form = (Formula) forms.get(i);
                    result.append(LanguageFormatter.htmlParaphrase(kbHref,
                                                                   form.theFormula, 
                                                                   kb.getFormatMap(language), 
                                                                   kb.getTermFormatMap(language), 
                                                                   kb,
                                                                   language) 
                                  + "<br>" 
                                  + StringUtil.getLineSeparator());
                }
            }
            result.append(limitString);

            localLimit = limit;
            limitString = "";
            forms = kb.ask("arg",0,term);
            if (forms != null) {
                if (forms.size() < localLimit) 
                    localLimit = forms.size();
                else
                    limitString = ("<br>Display limited to " 
                                   + localLimit
                                   + " statements of each type.<p>"
                                   + StringUtil.getLineSeparator());
                for (int i = 0; i < localLimit; i++) {
                    Formula form = (Formula) forms.get(i);
                    result.append(LanguageFormatter.htmlParaphrase(kbHref,
                                                                   form.theFormula, 
                                                                   kb.getFormatMap(language), 
                                                                   kb.getTermFormatMap(language), 
                                                                   kb,
                                                                   language) 
                                  + "<br>"
                                  + StringUtil.getLineSeparator());
                }
            }
            result.append(limitString);
            if (result.length() > 0) { 
                // note that the following 3 lines are inserted in reverse order
                result.insert(0,"</td></tr></table><p>");
                result.insert(0, ("<tr><td valign=\"top\" class=\"cell\">"
                                  + "These statements express (potentially complex) "
                                  + "facts about the term and are "
                                  + "automatically generated.</td></tr>"
                                  + StringUtil.getLineSeparator()
                                  + "<tr><td valign=\"top\" class=\"cell\">"));
                result.insert(0, ("<p><table><tr><td valign=\"top\" class=\"label\">"
                                  + "<b>Other statements</b>"
                                  + "</td></tr>"));
            }
        }
        return result.toString();
    }

    /** *************************************************************
     * 
     * 
     */
    protected String createTermRelevanceNotice(KB kb, 
                                               String kbHref, 
                                               String term, 
                                               String namespace) {
        String result = "";
        try {
            String suffix = "";
            if (StringUtil.emptyString(kbHref)) 
                suffix = ".html";
            String onto = getOntology();
            List focalClasses = kb.getTermsViaAskWithRestriction(0, 
                                                                 "docGenFocalInstanceClass", 
                                                                 1, 
                                                                 onto,
                                                                 2);

            if (!focalClasses.isEmpty()) {

                StringBuilder sb = new StringBuilder();

                // For now, assume just one focalClass.
                String focalClass = (String) focalClasses.get(0);

                Set ftSet = new HashSet(computeFocalTerms(kb, onto));

                String relevanceType = getTermRelevance(kb, onto, term);

                if (StringUtil.isNonEmptyString(relevanceType)) {
                    Set relevanceSet = (Set) getCoreTerms().get(term);
                    if (relevanceType.equals("supporting")) {
                        relevanceSet = (Set) getSupportingTerms().get(term);
                    }
                    else if (relevanceType.equals("framework")) {
                        relevanceSet = (Set) getFrameworkTerms().get(term);
                    }

                    if (relevanceSet != null) {
                        sb.append("<span class=\"relevance\">");
                        if (relevanceSet.containsAll(ftSet)) {
                            sb.append("A <span class=\"relevance-type\">");
                            sb.append(relevanceType);
                            sb.append("</span> term for every <a href=\"");
                            sb.append(kbHref);
                            sb.append(StringUtil.toSafeNamespaceDelimiter(kbHref, focalClass));
                            sb.append(suffix);
                            sb.append("\">");
                            sb.append(showTermName(kb, focalClass, namespace, false));
                            sb.append("</a>");
                        }                
                        else {
                            ArrayList sorted = new ArrayList(relevanceSet);
                            sortByPresentationName(kb, namespace, sorted);
                            String focalTerm = null;
                            int slen = sorted.size();
                            int last = (slen - 1);
                            int c = 0;
                            boolean isPair = (sorted.size() == 2);
                            boolean isFirst = true;
                            sb.append("A <span class=\"relevance-type\">");
                            sb.append(relevanceType);
                            sb.append("</span> term for ");
                            for (Iterator it = sorted.iterator(); it.hasNext();) {
                                focalTerm = (String) it.next();
                                if (!isFirst) {
                                    if (isPair) sb.append(" ");
                                    else sb.append(", ");
                                }
                                if ((slen > 1) && (c == last)) sb.append("and ");
                                sb.append("<a href=\"");
                                sb.append(kbHref);
                                sb.append(StringUtil.toSafeNamespaceDelimiter(kbHref, focalTerm));
                                sb.append(suffix);
                                sb.append("\">");
                                sb.append(showTermName(kb, focalTerm, namespace, false));
                                sb.append("</a>");
                                isFirst = false;
                                c++;
                            }
                        }
                        sb.append(".</span>");
                        result = sb.toString();
                    }
                }
            }
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
        return result;
    }

    /** *************************************************************
     * Returns a String denoting term's relative "centrality" or
     * relevance within ontology, or the empty String if no relevance
     * for term can be computed.
     *
     * @parfam kb The KB to use in computing terms' relevance within
     *            ontology
     *
     * @param ontology A String denoting the ontology within which
     *                 term has a relative degree of significance or
     *                 relevance
     * 
     * @param term A String denoting a term in ontology
     * 
     * @return A String describing term's relevance within ontology,
     *         or an empty String if term's relevance cannot be
     *         computed.
     */
    protected String getTermRelevance(KB kb, String ontology, String term) {
        String ans = "";
        try {
            if (getCoreTerms().isEmpty()) computeTermRelevance(kb, ontology);

            if (getCoreTerms().get(term) != null) {
                ans = "core";
            }
            else if (getFrameworkTerms().get(term) != null) {
                ans = "framework";
            }
            else if (getSupportingTerms().get(term) != null) {
                ans = "supporting";
            }

            if (ans.equals("core"))
                ans = "message";
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
        return ans;
    }

    /** *************************************************************
     *  Generate an alphabetic HTML list that points to the
     *  individual index pages (which collect all terms starting
     *  with a particular letter.
     */
    public String generateDynamicTOCHeader(String kbHref) {

        String markup = "";
        try {
            StringBuilder result = new StringBuilder();
            result.append(generateHtmlDocStart(""));
            result.append(StringUtil.getLineSeparator());
            result.append("<table width=\"100%\">");
            result.append(StringUtil.getLineSeparator());
            result.append("  <tr>");
            for (char c = 65; c < 91; c++) {
                result.append(StringUtil.getLineSeparator());
                String cString = Character.toString(c);   
                result.append("    <td valign=\"top\"><a href=\"");
                result.append(kbHref);
                if (StringUtil.isNonEmptyString(kbHref) 
                    && !kbHref.endsWith("&term=")) {
                    result.append("&term=");
                }
                result.append(cString);
                result.append("*\">");
                result.append(cString);
                result.append("</a></td>");
                result.append(StringUtil.getLineSeparator());
            }
            result.append("  </tr>");
            result.append(StringUtil.getLineSeparator());
            result.append("</table>");
            result.append(StringUtil.getLineSeparator());
            markup = result.toString();
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
        return markup;
    }

    /** *************************************************************
     *  Generate an alphabetic HTML list that points to the
     *  individual index pages (which collect all terms or term
     *  formats) starting with a particular letter.
     * 
     * @param alphaList a TreeMap of TreeMaps of ArrayLists.  @see
     *                   createAlphaList()
     */
    protected String generateTocHeader(KB kb, TreeMap alphaList, String allname) {

        if (StringUtil.emptyString(getTocHeader())) {
            StringBuilder result = new StringBuilder();
            try {
                ArrayList keyList = new ArrayList(alphaList.keySet());
                int klSize = keyList.size();

                sortByPresentationName(kb, getDefaultNamespace(), keyList);

                String title = getTitleText();
                title = StringUtil.removeEnclosingQuotes(title);

                String imgFile = getDefaultImageFileMarkup();
                if (StringUtil.isNonEmptyString(imgFile)) {
                    imgFile = StringUtil.removeEnclosingQuotes(imgFile);
                }

                // Add the header.
                result.append(generateHtmlDocStart(""));

                // We assemble the columns first, so as to get the
                // correct value for the table's colspan attribute.
                int colNum = 0;
                StringBuilder sb2 = new StringBuilder();

                // for (char c = 48; c < 58; c++) {                // numbers
                String cString = null;
                for (int i = 0; i < klSize; i++) {
                    cString = (String) keyList.get(i);  //Character.toString(c);
                    if (Character.isDigit(cString.charAt(0))) {
                        colNum++;
                        String filelink = "number-" + cString + ".html";      
                        sb2.append("    <td><a href=\""); 
                        sb2.append(filelink); 
                        sb2.append("\">");
                        sb2.append(cString);
                        sb2.append("</a></td>");
                        sb2.append(StringUtil.getLineSeparator());
                    }
                }

                // for (char c = 65; c < 91; c++) {                // letters
                for (int i = 0; i < klSize; i++) {
                    cString = (String) keyList.get(i);
                    if (!Character.isDigit(cString.charAt(0))) {
                        colNum++;
                        String filelink = "letter-" + cString + ".html";      
                        sb2.append("    <td><a href=\""); 
                        sb2.append(filelink); 
                        sb2.append("\">");
                        sb2.append(cString);
                        sb2.append("</a></td>");
                        sb2.append(StringUtil.getLineSeparator());
                    }
                }

                // Increment once more for All.
                colNum++;

                StringBuilder sb1 = new StringBuilder();
                sb1.append("<table width=\"100%\">");
                sb1.append(StringUtil.getLineSeparator());
                sb1.append("  <tr>");
                sb1.append(StringUtil.getLineSeparator());
                sb1.append("    <td valign=\"top\" colspan=\"");
                sb1.append(colNum);
                sb1.append("\" class=\"title\">");
                if (StringUtil.isNonEmptyString(imgFile)) {
                    sb1.append(imgFile);
                    sb1.append("&nbsp;&nbsp;");
                }
                sb1.append(title);
                sb1.append("    </td>");
                sb1.append(StringUtil.getLineSeparator());
                sb1.append("  </tr>");
                sb1.append(StringUtil.getLineSeparator());
                sb1.append("  <tr class=\"letter\">"); 
                sb1.append(StringUtil.getLineSeparator());

                // Assemble everything in the correct order.
                result.append(sb1);
                result.append(sb2);

                result.append("    <td><a href=\""); 
                result.append(allname); 
                result.append("\">All</a></td>"); 
                result.append(StringUtil.getLineSeparator());
                result.append("  </tr>");
                result.append(StringUtil.getLineSeparator());
                result.append("</table>");
                result.append(StringUtil.getLineSeparator());

                setTocHeader(result.toString());
            }
            catch (Exception ex) {
                ex.printStackTrace();
            }
        }
        return getTocHeader();
    }

    /** *************************************************************
     *  Generate an HTML page that lists term name and its
     *  documentation
     * 
     * @param alphaList a TreeMap of TreeMaps of ArrayLists.  @see
     *                   createAlphaList()
     */
    protected String generateTOCPage(KB kb, 
                                     String firstLetter, 
                                     TreeMap alphaList, 
                                     String language) {

        /*
          System.out.println("INFO in generateTOCPage(" + kb 
          + ", \"" + firstLetter + "\", "
          + "alphaList" 
          + " \"" + language + "\")");
        */
        String result = "";
        try {
            int count = 0;
            StringBuilder sb = new StringBuilder();
            sb.append("<table width=\"100%\">");
            TreeMap map = (TreeMap) alphaList.get(firstLetter);
            ArrayList sorted = new ArrayList(map.keySet());
            sortByPresentationName(kb, language, sorted);
            String formattedTerm = null;
            ArrayList al = null;
            Iterator it2 = null;
            String realTermName = null;
            String termToPrint = null;
            String docString = null;
            for (Iterator it = sorted.iterator(); it.hasNext();) {
                formattedTerm = (String) it.next();
                al = (ArrayList) map.get(formattedTerm);
                sortByPresentationName(kb, language, al);
                for (it2 = al.iterator(); it2.hasNext();) {

                    realTermName = (String) it2.next();
                    docString = getContextualDocumentation(kb, realTermName, null);
                    if (StringUtil.emptyString(docString)) {
                        // continue;
                        docString = "[missing definition]";
                    }
                    termToPrint = showTermName(kb, realTermName, language, true);
                    sb.append("  <tr>");
                    sb.append(StringUtil.getLineSeparator());

                    // Term Name
                    sb.append("    <td valign=\"top\" class=\"cell\">");
                    sb.append(StringUtil.getLineSeparator());
                    sb.append("      <a href=\""); 
                    sb.append(StringUtil.toSafeNamespaceDelimiter(realTermName)); 
                    sb.append(".html\">");
                    sb.append(StringUtil.getLineSeparator());
                    sb.append("        ");
                    sb.append(termToPrint);
                    sb.append(StringUtil.getLineSeparator());
                    sb.append("      </a>");
                    sb.append(StringUtil.getLineSeparator());
                    sb.append("    </td>");
                    sb.append(StringUtil.getLineSeparator());

                    // Relevance
                    sb.append("    <td valign=\"top\" class=\"cell\">");
                    sb.append(StringUtil.getLineSeparator());
                    sb.append("      ");
                    sb.append(getTermRelevance(kb, getOntology(), realTermName).equals("message")
                              ? "&nbsp;&nbsp;&nbsp;&nbsp;MT&nbsp;&nbsp;&nbsp;&nbsp;"
                              : "");
                    sb.append("    </td>");
                    sb.append(StringUtil.getLineSeparator());

                    // Documentation
                    docString = processDocString(kb, "", language, docString, false, true);

                    sb.append("    <td valign=\"top\" class=\"cell\">");
                    sb.append(StringUtil.getLineSeparator());
                    sb.append("      ");
                    sb.append(docString);
                    sb.append(StringUtil.getLineSeparator());
                    sb.append("    </td>");
                    sb.append(StringUtil.getLineSeparator());
                    sb.append("  </tr>"); 
                    sb.append(StringUtil.getLineSeparator());
                }
            }
            sb.append("</table>");
            sb.append(StringUtil.getLineSeparator());
            result = sb.toString();
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
        return result;
    }

    /** *************************************************************
     * Generate and save all the index pages that link to the
     * individual term pages.
     *
     * @param pageList is a map of all term pages keyed by term
     *                  name
     * @param dir is the directory in which to save the pages
     * 
     * @param alphaList a TreeMap of TreeMaps of ArrayLists.  @see
     *                   createAlphaList()
     */
    protected void saveIndexPages(KB kb, 
                                  TreeMap alphaList, 
                                  String dir, 
                                  String language) {

        long t1 = System.currentTimeMillis();
        System.out.println("ENTER DocGen.saveIndexPages("
                           + kb.name + ", "
                           + "[map with " + alphaList.size() + " entries}, "
                           + dir + ", "
                           + language + ")");

        PrintWriter pw = null; 
        File outfile = null;
        String outpath = null;
        try {
            String tocheader = generateTocHeader(kb, 
                                                 alphaList, 
                                                 INDEX_FILE_NAME);
            File parentDir = new File(dir);
            int count = 0;
            for (Iterator it = alphaList.keySet().iterator(); it.hasNext();) {
                String letter = (String) it.next();
                outfile = new File(parentDir,
                                   ((letter.compareTo("A") < 0) ? "number-" : "letter-")
                                   + letter
                                   + ".html");
                outpath = outfile.getCanonicalPath();
                pw = new PrintWriter(new FileWriter(outfile));
                String page = generateTOCPage(kb, letter, alphaList, language);
                pw.println(tocheader);
                pw.println(page);
                pw.println(generateHtmlFooter("")); 
                try {
                    pw.close();
                }
                catch (Exception pwex) {
                    System.out.println("Error writing \"" 
                                       + outpath + "\": " 
                                       + pwex.getMessage());
                    pwex.printStackTrace();
                }
                if ((count++ % 100) == 1) System.out.print(".");
            }   
            System.out.println("x");
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
        finally {
            try {
                if (pw != null) {
                    pw.close();
                }
            }
            catch (Exception ioe) {
                ioe.printStackTrace();
            }
        }
        System.out.println("EXIT DocGen.saveIndexPages("
                           + kb.name + ", "
                           + "[map with " + alphaList.size() + " entries}, "
                           + dir + ", "
                           + language + ")");
        System.out.println("  > "
                           + ((System.currentTimeMillis() - t1) / 1000.0)
                           + " seconds elapsed time");

        return;
    }

    /** *************************************************************
     *  Save pages below the KBs directory in a directory called
     *  HTML.  If that already exists, use HTML1, HTML2 etc.
     */
    protected void printHTMLPages(TreeMap pageList, String dirpath) {

        long t1 = System.currentTimeMillis();
        System.out.println("ENTER DocGen.printHTMLPages("
                           + "[map with " + pageList.size() + " entries], "
                           + dirpath + ")");

        FileWriter fw = null;
        PrintWriter pw = null;
        String term = null;
        String page = null;
        File outfile = null;
        String filename = null;
        try {
            File outdir = new File(dirpath);
            for (Iterator it = pageList.keySet().iterator(); it.hasNext();) {
                term = (String) it.next();
                page = (String) pageList.get(term);
                outfile = new File(outdir, StringUtil.toSafeNamespaceDelimiter(term) + ".html");
                filename = outfile.getCanonicalPath();
                //System.out.println("Info in DocGen.printPages(): filename == " + filename);
                try {
                    pw = new PrintWriter(new FileWriter(filename));
                    pw.println(page);
                }
                catch (Exception e) {
                    System.out.println("ERROR in DocGen.printHTMLPages("
                                       + "[map with " + pageList.keySet().size() + " keys], "
                                       + dirpath + ")");
                    System.out.println("Error writing file " 
                                       + filename
                                       + StringUtil.getLineSeparator() + ": "
                                       + e.getMessage());
                    e.printStackTrace();
                }
                finally {
                    try {
                        if (pw != null) {
                            pw.close();
                        }
                    }
                    catch (Exception e2) {
                        e2.printStackTrace();
                    }
                }
            }
        }
        catch (Exception oe) {
            oe.printStackTrace();
        }

        System.out.println("EXIT DocGen.printHTMLPages("
                           + "[map with " + pageList.size() + " entries], "
                           + dirpath + ")");
        System.out.println("  > "
                           + ((System.currentTimeMillis() - t1) / 1000.0)
                           + " seconds elapsed time");

        return;
    }

    /** **************************************************************
     * Create a subdirectory below a specified parent directory, or
     * use the SIGMA_HOME directory as the parent directory if another
     * has not been set.  Use key1 and key2 to make a unique
     * subdirectory name.
     */
    protected File makeOutputDir(String key1, String key2) {
        File outdir = null;
        try {
            File parentDir = this.getOutputParentDir();
            if ((parentDir == null) || !parentDir.isDirectory()) {
                parentDir = new File(KBmanager.getMgr().getPref("baseDir"));
            }
            String dirname = (StringUtil.removeEnclosingQuotes(key1) 
                              + "_" 
                              + StringUtil.removeEnclosingQuotes(key2));
            dirname = dirname.replaceAll(" ", "_");
            String dirbase = dirname;
            int counter = 0;
            dirname = (dirbase + "_" + counter);
            File f = new File(parentDir, dirname);
            while (f.exists()) {
                counter++;
                dirname = (dirbase + "_" + counter);
                f = new File(parentDir, dirname);
            }
            f.mkdirs();
            if (f.isDirectory() && f.canWrite()) {
                outdir = f;
            }
        }
        catch (Exception ex) {
            System.out.println(ex.getMessage());
            ex.printStackTrace();
            if (outdir == null) {
                System.out.println("DocGen.makeOutputDir("
                                   + key1 + ", "
                                   + key2 + ") failed");
            }
        }
        return outdir;
    }

    /** *************************************************************
     *  @return The headword for term in kb.  This assumes that term
     *  is specific to a particular namespace.
     */
    public String getHeadword(KB kb, String term) {
        String ans = term;
        try {
            List formulae = null;
            if (StringUtil.isNonEmptyString(term)) {
                formulae = kb.askWithRestriction(2, term, 0, "headword");
            }
            if ((formulae != null) && !formulae.isEmpty()) {
                // We assume, perhaps wrongly, that there is only one.
                Formula f = null;
                for (int i = 0; i < formulae.size(); i++) {
                    f = (Formula) formulae.get(i);
                    ans = StringUtil.removeEnclosingQuotes(f.getArgument(3));
                    break;
                }
            }
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
        return ans;
    }

    /** *************************************************************
     *  @return a HashMap where the keys are the term names and the
     *  values are the "headwords" (with quotes removed).
     */
    public HashMap createHeadwordMap(KB kb) {

        long t1 = System.currentTimeMillis();
        System.out.println("ENTER DocGen.createHeadwordMap(" + kb.name + ")");
                           
        HashMap result = new HashMap();
        try {
            ArrayList headwordForms = kb.ask("arg", 0, "headword");
            if ((headwordForms != null) && !headwordForms.isEmpty()) {
                for (Iterator it = headwordForms.iterator(); it.hasNext();) {
                    Formula f = (Formula) it.next();
                    String term = f.getArgument(2);
                    String headword = StringUtil.removeEnclosingQuotes(f.getArgument(3));
                    result.put(term, headword);
                }
            }
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }

        System.out.println("EXIT DocGen.createHeadwordMap(" + kb.name + ")");
        System.out.println("  > "
                           + ((System.currentTimeMillis() - t1) / 1000.0)
                           + " seconds elapsed time");

        return result;
    }

    /** *************************************************************
     *  @return a HashMap where the keys are the headwords and the
     *  values are ArrayLists of term names (since headwords are not
     *  unique identifiers for terms). Don't put automatically
     *  created instances in the map. If there's no headword, use
     *  the term name. This map is the inverse of headwordMap. @see
     *  DB.stringToKifId()
     */
    public HashMap createInverseHeadwordMap(KB kb, HashMap headwordMap) {

        long t1 = System.currentTimeMillis();
        System.out.println("ENTER DocGen.createInverseHeadwordMap("
                           + kb.name + ", "
                           + "[map with " + headwordMap.size() + " entries])");

        HashMap result = new HashMap();
        try {
            String term = null;
            String headword = null;
            String h2 = null;
            List al = null;
            Iterator it = null;
            for (it = kb.terms.iterator(); it.hasNext();) {
                term = (String) it.next();
                // Don't display automatically created instances
                if (isLegalForDisplay(term)) {
                    headword = term;
                    if (simplified) {
                        h2 = (String) headwordMap.get(term);
                        if (h2 != null) headword = h2;
                    }
                    al = (List) result.get(headword);
                    if (al == null) {
                        al = new ArrayList();
                        result.put(headword, al);
                    }
                    al.add(term);
                }
            }
            for (it = result.values().iterator(); it.hasNext();) {
                al = (List) it.next();
                sortByPresentationName(kb, getDefaultNamespace(), al);
            }
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }

        System.out.println("EXIT DocGen.createInverseHeadwordMap("
                           + kb.name + ", "
                           + "[map with " + headwordMap.size() + " entries])");
        System.out.println("  > "
                           + ((System.currentTimeMillis() - t1) / 1000.0)
                           + " seconds elapsed time");

        return result;
    }

    /** *************************************************************
     * @param alphaList a TreeMap of TreeMaps of ArrayLists.  @see
     *                   createAlphaList()
     * 
     * @param inverseHeadwordMap is a HashMap where the keys are the
     *          headwords and the values are ArrayLists of term
     *          names (since headwords are not unique identifiers
     *          for terms). If there's no headword, the term name is
     *          used.
     */
    protected TreeMap generateHTMLPages(KB kb, 
                                        TreeMap alphaList,
                                        // HashMap inverseHeadwordMap, 
                                        String language,
                                        Set formatTokens) {

        long t1 = System.currentTimeMillis();
        System.out.println("ENTER DocGen.generateHTMLPages("
                           + kb.name + ", "
                           + "[map with " + alphaList.keySet().size() + " keys], "
                           // + "[map with " + inverseHeadwordMap.keySet().size() + " keys], "
                           + language + ", "
                           + formatTokens + ")");

        TreeMap pageList = new TreeMap();
        TreeSet rejectedTerms = new TreeSet();

        try {
            String formattedTerm = null;
            List termNames = null;
            String realTermName = null;
            int count = 0;
            for (Iterator it = kb.terms.iterator();
                 // inverseHeadwordMap.keySet().iterator(); 
                 it.hasNext();) {
                // formattedTerm = (String) it.next();
                // termNames = (List) inverseHeadwordMap.get(formattedTerm);
                // for (Iterator tni = termNames.iterator(); tni.hasNext();) {
                // realTermName = (String) tni.next();
                realTermName = (String) it.next();
                if (isLegalForDisplay(realTermName)) {
                    if (isComposite(kb, realTermName)) {
                        pageList.put(realTermName,
                                     createCompositePage(kb,
                                                         "",
                                                         realTermName,
                                                         alphaList,
                                                         200,
                                                         language,
                                                         formatTokens));
                    }
                    else {
                        pageList.put(realTermName,createPage(kb,
                                                             "",
                                                             realTermName,
                                                             alphaList,
                                                             200,
                                                             language,
                                                             formatTokens));
                    }
                    if ((count++ % 100) == 1) System.out.print(".");
                }
                else {
                    rejectedTerms.add(realTermName);
                }
                // }
            }
            System.out.println("x");
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }

        System.out.println("EXIT DocGen.generateHTMLPages("
                           + kb.name + ", "
                           + "[map with " + alphaList.keySet().size() + " keys], "
                           // + "[map with " + inverseHeadwordMap.keySet().size() + " keys], "
                           + language + ", "
                           + formatTokens + ")");
        System.out.println("  > " + rejectedTerms.size() + " terms rejected");
        System.out.println("  > " 
                           + ((System.currentTimeMillis() - t1) / 1000.0)
                           + " seconds elapsed time");

        return pageList;
    }

    /** *************************************************************
     * Generate simplified HTML pages for all terms.  Output is a
     * set of HTML files sent to the directory specified in
     * makeOutputDir()
     * 
     * @param simplified Indicates whether to present a "simplified"
     * view of terms, meaning using a termFormat or headword, rather
     * than the term name itself
     */
    public void generateHTML(KB kb, String language, boolean simplified, Set formatTokens) {
        long t1 = System.currentTimeMillis();
        System.out.println("ENTER DocGen.generateHTML("
                           + kb.name + ", "
                           + language + ", "
                           + simplified + ", "
                           + formatTokens + ")");
        try {

            // Keys are headwords, values are terms
            TreeMap termMap = new TreeMap(String.CASE_INSENSITIVE_ORDER);

            // A HashMap where the keys are the term names and the values
            // are "headwords" (with quotes removed).
            /*
              HashMap headwordMap = createHeadwordMap(kb); 

              System.out.println("headwordMap == ");
              String key = null;
              String val = null;
              for (Iterator it = headwordMap.keySet().iterator(); it.hasNext();) {
              key = (String) it.next();
              val = (String) headwordMap.get(key);
              System.out.println(key + " -> " + val);
              }
            */

            String context = toKifNamespace(kb, language);
            this.defaultNamespace = context;

            if (getCoreTerms().isEmpty()) computeTermRelevance(kb, getOntology());

            // a TreeMap of TreeMaps of ArrayLists.  @see createAlphaList()
            TreeMap alphaList = createAlphaList(kb //, headwordMap
                                                );

            // Headword keys and ArrayList values (since the same headword can
            // be found in more than one term)
            // HashMap inverseHeadwordMap = createInverseHeadwordMap(kb, headwordMap);  
            /*
              System.out.println("inverseHeadwordMap == ");
              key = null;
              List lval = null;
              for (Iterator iti = inverseHeadwordMap.keySet().iterator(); iti.hasNext();) {
              key = (String) iti.next();
              lval = (List) inverseHeadwordMap.get(key);
              System.out.println(key + " -> " + lval);
              }
            */
            // System.out.println("  INFO in DocGen.generateHTML(): generating alpha list");
            String dir = getHtmlOutputDirectoryPath();
            // System.out.println("  INFO in DocGen.generateHTML(): saving index pages");
            saveIndexPages(kb, alphaList, dir, context);

            // System.out.println("  INFO in DocGen.generateHTML(): generating HTML pages");
            // Keys are formatted term names, values are HTML pages
            TreeMap pageList = generateHTMLPages(kb, 
                                                 alphaList, 
                                                 // inverseHeadwordMap, 
                                                 context, 
                                                 formatTokens);
            printHTMLPages(pageList, dir);

            // System.out.println("  INFO in DocGen.generateHTML(): creating single index page");
            generateSingleHTML(kb, dir, alphaList, context, simplified);
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
        System.out.println("EXIT DocGen.generateHTML("
                           + kb.name + ", "
                           + language + ", "
                           + simplified + ", "
                           + formatTokens + ")");
        System.out.println("  > "
                           + ((System.currentTimeMillis() - t1) / 1000.0)
                           + " seconds elapsed time");
        return;
    }

    /** *************************************************************
     * Generate a single HTML page showing all terms.
     *
     * @param alphaList a TreeMap of TreeMaps of ArrayLists.
     *                  @see createAlphaList()
     *      letter->    formattedTerm1->term11,term12...term1N
     *                  formattedTerm2->term21,term22...term2N
     * 
     * @param simplified Indicates whether to present a "simplified"
     * view of terms, meaning using a termFormat or headword, rather
     * than the term name itself
     */
    public void generateSingleHTML(KB kb, 
                                   String dir, 
                                   TreeMap alphaList,
                                   String language, 
                                   boolean simplified) {

        long t1 = System.currentTimeMillis();
        System.out.println("ENTER DocGen.generateSingleHTML("
                           + kb.name + ", "
                           + dir + ", "
                           + "[map with " + alphaList.size() + " entries], "
                           + language + ", "
                           + simplified + ")");

        PrintWriter pw = null; 
        try {
            File filedir = new File(dir);
            File outfile = new File(filedir, INDEX_FILE_NAME);
            pw = new PrintWriter(new FileWriter(outfile));
            pw.println(generateTocHeader(kb, alphaList, INDEX_FILE_NAME));
            pw.println("<table border=\"0\">");
            String letter = null;
            Map values = null;
            List sortedKeys = new ArrayList();
            Iterator it2 = null;
            Iterator it3 = null;
            String formattedTerm = null;
            List terms = null;
            String term = null;
            String printableTerm = null;
            List docs = null;
            Formula f = null;
            String docStr = null;
            for (Iterator it = alphaList.keySet().iterator(); it.hasNext();) {
                letter = (String) it.next();
                values = (TreeMap) alphaList.get(letter);
                sortedKeys.clear();
                sortedKeys.addAll(values.keySet());
                sortByPresentationName(kb, language, sortedKeys);
                for (it2 = sortedKeys.iterator(); it2.hasNext();) {
                    formattedTerm = (String) it2.next();
                    terms = (List) values.get(formattedTerm);
                    for (it3 = terms.iterator(); it3.hasNext();) {
                        term = (String) it3.next();
                        term = StringUtil.w3cToKif(term);
                        if (isLegalForDisplay(term)) {
                            docStr = "";
                            docs = kb.askWithRestriction(0, "documentation", 1, term);
                            if ((docs != null) && !docs.isEmpty()) {
                                f = (Formula) docs.get(0);
                                docStr = processDocString(kb, 
                                                          "", 
                                                          language, 
                                                          f.getArgument(3), 
                                                          false, 
                                                          true);
                            }
                            if (StringUtil.isLocalTermReference(term) 
                                || StringUtil.emptyString(docStr))
                                continue;

                            pw.println("  <tr>");

                            // Term
                            pw.println("    <td valign=\"top\" class=\"cell\">");
                            printableTerm = (simplified
                                             ? showTermName(kb, term, language, true)
                                             : term);
                            pw.print("      <a href=\"");
                            pw.print(StringUtil.toSafeNamespaceDelimiter(term));
                            pw.print(".html\">");
                            pw.print(printableTerm);
                            pw.println("</a>");
                            pw.println("    </td>");

                            // Relevance
                            pw.println("    <td valign=\"top\" class=\"cell\">");
                            pw.print("      ");
                            pw.println(getTermRelevance(kb, getOntology(), term).equals("message")
                                       ? "&nbsp;&nbsp;&nbsp;&nbsp;MT&nbsp;&nbsp;&nbsp;&nbsp;"
                                       : "");
                            pw.println("    </td>");

                            // Documentation
                            pw.println("    <td valign=\"top\" class=\"description\">");
                            pw.print("    ");
                            pw.println(docStr);
                            pw.println("    </td>");
                            pw.println("  </tr>");
                        }
                    }
                }
            }
            pw.println("</table>");
            pw.println("");
            pw.println(generateHtmlFooter(""));
            pw.println("  </body>");
            pw.println("</html>");
        }
        catch (Exception ex) {
            System.out.println(ex.getMessage());
            ex.printStackTrace();
        }
        finally {
            try {
                if (pw != null) {
                    pw.close();
                }
            }
            catch (Exception pwe) {
                pwe.printStackTrace();
            }
        }

        System.out.println("EXIT DocGen.generateSingleHTML("
                           + kb.name + ", "
                           + dir + ", "
                           + "[map with " + alphaList.size() + " entries], "
                           + language + ", "
                           + simplified + ")");
        System.out.println("  > "
                           + ((System.currentTimeMillis() - t1) / 1000.0)
                           + " seconds elapsed time");

        return;
    }

    /** *************************************************************
     */
    public void generateSingleHTML(KB kb, String language, boolean simplified) 
        throws IOException {

        // HashMap headwordMap = createHeadwordMap(kb); 
        String dirpath = getHtmlOutputDirectoryPath();
        TreeMap alphaList = createAlphaList(kb //, headwordMap
                                            );
        generateSingleHTML(kb, dirpath, alphaList, language, simplified);
    }


    /// START: code for processing Context Families

    // basicTypes for Context Family processing.
    private static final int CF_FAMILY   = 0;
    private static final int CF_CONTEXT  = 1;
    private static final int CF_RESOURCE = 2;
    private static final int CF_TIME     = 3;
    private static final int CF_PLACE    = 4;

    private static final String DOT_OP   = "-";  // was: "."

    /** *************************************************************
     * Processes the Context Family .dif files found in the directory
     * denoted by indirpath, and saves the resulting KIF file in the
     * directory denoted by outdirpath.
     *
     * @param indirpath A String denoting a directory pathname
     *
     * @param outdirpath A String denoting a directory pathname
     *
     * @return void
     */
    public void cfFilesToKifHierarchyFile(String indirpath, String outdirpath) {
        long t1 = System.currentTimeMillis();
        System.out.println("ENTER DocGen.cfFilesToKifHierarchyFile(");
        System.out.println("  " + indirpath + ", ");
        System.out.println("  " + outdirpath + ")");
        File outdir = null;
        File indir = null;
        PrintWriter pw = null;
        try {
            indir = new File(indirpath);
            if (indir.canRead()) {
                Map hier = new HashMap();
                Set<String> compositeRelators = new TreeSet<String>();
                Set<String> statements = new LinkedHashSet<String>();
                Set<String> localStatements = new LinkedHashSet<String>();
                List<String> skipFiles = Arrays.asList("CF_Template", 
                                                       "OntX_log",
                                                       "scow",
                                                       "SOW_Template",
                                                       "Summary");
                List<File> files = Arrays.asList(indir.listFiles());
                Collections.sort(files);
                DocGen spi = DocGen.getInstance();

                // Read and process CF files.
                int cfsDone = 0;
                for (File f : files) {
                    if (!f.isDirectory()) {
                        String filename = f.getName();
                        String suff = (filename.matches(".+(?i)\\.dif$")
                                       ? ".dif"
                                       : (filename.matches(".+(?i)\\.csv$")
                                          ? ".csv"
                                          : ""));
                        int fnlen = filename.length();
                        int eidx = (fnlen - suff.length());
                        String base = ((eidx < fnlen)
                                       ? filename.substring(0, eidx)
                                       : filename);
                        if (!skipFiles.contains(base)) {
                            String cp = f.getCanonicalPath();
                            List tblarr = (suff.equals(".dif")
                                           ? DB.readDataInterchangeFormatFile(cp)
                                           : (suff.equals(".csv")
                                              ? DB.readSpreadsheet(cp, 
                                                                   Arrays.asList("u,", 
                                                                                 "U,",
                                                                                 "$,"))
                                              : new ArrayList()));

                            /*
                              if (!tblarr.isEmpty()) {
                              System.out.println("");
                              for (Iterator it = tblarr.iterator(); it.hasNext();) {
                              System.out.println( it.next() );
                              }
                              }
                              System.out.println("");
                            */

                            /*  */
                            if (!tblarr.isEmpty()) {
                                normalizeCfTable(tblarr);
                                statements.add(((cfsDone > 0)
                                                ? spi.getLineSeparator()
                                                : "") + ";; " + base);
                                localStatements.clear();
                                processCfTable(tblarr, 
                                               hier, 
                                               compositeRelators, 
                                               localStatements);
                                statements.addAll(localStatements);
                                cfsDone++;
                            }
                        }
                    }
                }

                System.out.println("  > " 
                                   + compositeRelators.size() 
                                   + " composite relators created");
                // System.out.println("");
                // System.out.println("  hier == " + hier);
                // System.out.println("");

                Formula f = null;
                String arg1 = null;
                String arg2 = null;
                List plist1 = null;
                List plist2 = null;
                String p1 = null;
                String p2 = null;
                int subRelCount = 0;
                for (String fstr : compositeRelators) {
                    f = new Formula();
                    f.read(fstr);
                    arg1 = f.getArgument(1);
                    arg2 = f.getArgument(2);
                    plist1 = (List) hier.get(arg1);
                    if ((plist1 != null) && !plist1.isEmpty()) {
                        plist2 = (List) hier.get(arg2);
                        if ((plist2 != null) && !plist2.isEmpty()) {
                            for (Iterator it1 = plist1.iterator(); it1.hasNext();) {
                                p1 = (String) it1.next();
                                for (Iterator it2 = plist2.iterator(); it2.hasNext();) {
                                    p2 = (String) it2.next();
                                    String fstr2 = ("(PredicateFn " + p1 + " " + p2 + ")");
                                    if (compositeRelators.contains(fstr2)
                                        && !fstr.equals(fstr2)) {
                                        if (subRelCount == 0) {
                                            statements.add(spi.getLineSeparator() 
                                                           + ";; coa:IsSubRelatorOf");
                                        }
                                        statements.add(DocGen.makeStatement("coa:IsSubRelatorOf",
                                                                            fstr,
                                                                            fstr2));
                                        subRelCount++;
                                    }
                                }
                            }
                        }
                    }
                }

                System.out.println("  " + subRelCount + " coa:IsSubRelatorOf statements added");

                // Write SUO-KIF statements.
                outdir = new File(outdirpath);
                if (outdir.canWrite()) {
                    File outfile = new File(outdir, "ContextFamilies.kif");
                    pw = new PrintWriter(new FileWriter(outfile));
                    DB.printSuoKifStatements(statements, pw);
                }
            }
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
        finally {
            try {
                if (pw != null) pw.close();
            }
            catch (Exception ioe) {
                ioe.printStackTrace();
            }
        }

        System.out.println("EXIT DocGen.cfFilesToKifHierarchyFile(");
        System.out.println("  " + indirpath + ", ");
        System.out.println("  " + outdirpath + ")");
        System.out.println("  > "
                           + ((System.currentTimeMillis() - t1) / 1000.0)
                           + " seconds elapsed time");
        
        return;
    }

    /** *************************************************************
     * Processes the List of Lists tblarr to produce Strings denoting
     * KIF statements, which are added to the Set statements.
     *
     * @param tblarr A List of Lists representing a spreadsheet or
     * database table
     *
     * @param hier A Map used to compute the parent (subclass)
     * hierarchy
     *
     * @param compositeRelators The set of all computed relators
     *
     * @param statements A Set containing Strings that represent KIF
     * statements
     *
     * @return void
     */
    private void processCfTable(List<List> tblarr, 
                                Map hier,
                                Set<String> compositeRelators,
                                Set<String> statements) {
        try {
            Set<String> zeroCardinality = new HashSet<String>();
            Map<String, String> cardinalities = new HashMap<String, String>();
            Map<String, String> relatorComponents = new HashMap<String, String>();
            String famSuff = "_CF";
            String nsToken = "coa";
            String namespace = ("ns" + StringUtil.getW3cNamespaceDelimiter() + nsToken);
            String nsPrefix = (nsToken + StringUtil.getW3cNamespaceDelimiter());
            buildTableColumnNames(null, null);
            List commentCols = Arrays.asList("I", "J", "K");
            List adjCols = Arrays.asList("L", "M", "N");
            List adjNames = Arrays.asList("HistoricAdjective", 
                                          "CurrentAdjective", 
                                          "PotentialAdjective");
            List propCols = Arrays.asList("O", "P", "Q");
            List propNames = Arrays.asList("HistoricProperty", 
                                           "CurrentProperty", 
                                           "PotentialProperty");
            List<String> synonyms = new ArrayList<String>();
            String family = null;
            String contextType = null;
            List<String> utilList = new ArrayList<String>();  // an empty List
            for (List row : tblarr) {
                int bti = -1;
                String basicType = (String) row.get(getColumnIndex("A"));
                if (StringUtil.isNonEmptyString(basicType)) {
                    basicType = basicType.trim();
                    if (basicType.equals("$$$")) {
                        break;
                    }
                    String rowFocalTerm = null;
                    String familyMembers = (String) row.get(getColumnIndex("B"));
                    List<String> fmList = (StringUtil.isNonEmptyString(familyMembers)
                                           ? Arrays.asList(familyMembers.split("\\|"))
                                           : utilList);
                    String parents = (String) row.get(getColumnIndex("C"));
                    List<String> parentList = (StringUtil.isNonEmptyString(parents)
                                               ? Arrays.asList(parents.split("\\|"))
                                               : utilList);
                    String cardinality = (String) row.get(getColumnIndex("E"));
                    if (StringUtil.emptyString(cardinality)) {
                        cardinality = "";
                    }
                    String minstr = "";
                    String maxstr = "";
                    int min = -1;
                    int max = -1;
                    int ridx = cardinality.indexOf("-");
                    if (ridx > -1) {
                        minstr = cardinality.substring(0, ridx);
                        maxstr = cardinality.substring(ridx + 1);
                        if (maxstr.equals("n")) {
                            maxstr = Integer.toString(Integer.MAX_VALUE);
                        }
                    }
                    else if (cardinality.equals("n")) {
                        minstr = Integer.toString(Integer.MAX_VALUE);
                        maxstr = minstr;
                    }
                    else if (StringUtil.isDigitString(cardinality)) {
                        minstr = cardinality;
                        maxstr = cardinality;
                    }
                    try {
                        min = Integer.parseInt(minstr);
                    }
                    catch (Exception e1) {
                        min = -1;
                    }
                    try {
                        max = Integer.parseInt(maxstr);
                    }
                    catch (Exception e2) {
                        max = -1;
                    }

                    // System.out.println("  min == " + min);
                    // System.out.println("  max == " + max);

                    String twoOrMoreP = ((max > 1) ? "true" : "false");
                    String definition = (String) row.get(getColumnIndex("F"));

                    // if (!parentList.isEmpty()) System.out.println("  parentList == " + parentList);

                    if (!fmList.isEmpty()) {
                        if (basicType.equals("Family")) {
                            bti = CF_FAMILY;
                            family = (String) fmList.get(0);
                            if (StringUtil.isNonEmptyString(family)) {
                                if (!family.endsWith(famSuff)) family += famSuff;
                                rowFocalTerm = family;
                                statements.add(makeStatement("coa:IsA", 
                                                             family, 
                                                             "coa:ContextFamily"));
                            }
                            for (String term : fmList) {
                                int idx = term.indexOf(StringUtil.getW3cNamespaceDelimiter());
                                String newTerm = null;
                                if ((idx > 0) && (idx < term.length())) {
                                    newTerm = term.substring(idx + 1);
                                }
                                synonyms.add(newTerm);
                            }
                            for (String term : parentList) {
                                String parentFamily = (term.endsWith(famSuff)
                                                       ? term
                                                       : (term + famSuff));
                                statements.add(makeStatement("coa:HasLogicalParent",
                                                             family,
                                                             parentFamily));
                            }
                        }
                        else if (basicType.equals("ContextType")) {
                            bti = CF_CONTEXT;
                            int i = 0;
                            for (String term : fmList) {
                                int idx = term.indexOf(StringUtil.getW3cNamespaceDelimiter());
                                String newTerm = null;
                                if ((idx > 0) && (idx < term.length())) {
                                    newTerm = term.substring(idx + 1);
                                }
                                if (i == 0) {
                                    newTerm = (nsPrefix + newTerm);
                                    contextType = newTerm;
                                    rowFocalTerm = contextType;
                                    relatorComponents.put(StringUtil.w3cToKif(rowFocalTerm),
                                                          twoOrMoreP);
                                }
                                else {
                                    statements.add(makeStatement("synonym",
                                                                 namespace,
                                                                 contextType,
                                                                 StringUtil.quote(newTerm)));
                                }
                                i++;
                            }
                            for (String syn : synonyms) {
                                statements.add(makeStatement("synonym",
                                                             namespace,
                                                             contextType,
                                                             StringUtil.quote(syn)));
                            }                                
                        }
                        else if (basicType.startsWith("ResourceRole")) {
                            bti = CF_RESOURCE;
                        }
                        else if (basicType.startsWith("TimeRole")) {
                            bti = CF_TIME;
                        }
                        else if (basicType.startsWith("PlaceRole")) {
                            bti = CF_PLACE;
                        }

                        if (bti > CF_CONTEXT) {
                            int i = 0;
                            for (String term : fmList) {
                                int idx = term.indexOf(StringUtil.getW3cNamespaceDelimiter());
                                String newTerm = null;
                                if ((idx > 0) && (idx < term.length())) {
                                    newTerm = term.substring(idx + 1);
                                }
                                if (i == 0) {
                                    newTerm = (nsPrefix + newTerm);
                                    rowFocalTerm = newTerm;
                                    if (bti == CF_RESOURCE) {
                                        relatorComponents.put(StringUtil.w3cToKif(rowFocalTerm),
                                                              twoOrMoreP);
                                    }
                                }
                                else {
                                    statements.add(makeStatement("synonym",
                                                                 namespace,
                                                                 rowFocalTerm,
                                                                 StringUtil.quote(newTerm)));
                                }
                                i++;
                            }
                        }

                        if (StringUtil.isNonEmptyString(rowFocalTerm)) {

                            if ((bti > CF_FAMILY) && StringUtil.isNonEmptyString(family)) {
                                statements.add(makeStatement("coa:HasCfMember",
                                                             family,
                                                             rowFocalTerm));
                                if (!parentList.isEmpty()) {
                                    for (String p : parentList) {
                                        statements.add(makeStatement("coa:IsSubClassOf",
                                                                     rowFocalTerm,
                                                                     p));
                                    }
                                }
                            }

                            if ((bti == CF_CONTEXT) || (bti == CF_RESOURCE)) {
                                String kifTerm = StringUtil.w3cToKif(rowFocalTerm);
                                if (!parentList.isEmpty()) {
                                    List plist = (List) hier.get(kifTerm);
                                    if (plist == null) {
                                        plist = new LinkedList();
                                        hier.put(kifTerm, plist);
                                    }
                                    for (String s : parentList) {
                                        String kifParent = StringUtil.w3cToKif(s);
                                        if (!plist.contains(kifParent)) {
                                            plist.add(kifParent);
                                        }
                                    }
                                }

                                // System.out.println("  cardinality == " + cardinality);

                                if (StringUtil.isNonEmptyString(cardinality)) {
                                    addCfCardinalityValues(kifTerm, 
                                                           cardinality, 
                                                           cardinalities,
                                                           zeroCardinality);
                                }
                            }
                        }
                    }
                }
            }

            List rcKeys = new ArrayList(relatorComponents.keySet());
            Iterator its = null;
            Iterator itr = null;
            for (String zc : zeroCardinality) {
                for (its = statements.iterator(); its.hasNext();) {
                    String stmt = (String) its.next();
                    if (stmt.contains(zc) && !stmt.matches(".*Has\\w+Cardinality.*")) {
                        its.remove();
                        System.out.println("  removed stmt == " + stmt);
                    }
                }
                /*
                  for (itr = rcKeys.iterator(); itr.hasNext();) {
                  String key = (String) itr.next();
                  if (key.contains(zc)) {
                  relatorComponents.remove(key);
                  System.out.println("  removed key == " + key);
                  }
                  }
                */
            }

            // System.out.println("  relatorComponents == " + relatorComponents);
            String c1 = null;
            String c2 = null;
            String boolStr = null;
            for (Iterator it1 = relatorComponents.keySet().iterator(); it1.hasNext();) {
                c1 = (String) it1.next();
                boolStr = (String) relatorComponents.get(c1);
                boolean isTwoOrMore = (StringUtil.isNonEmptyString(boolStr) 
                                       && boolStr.equals("true"));
                for (Iterator it2 = relatorComponents.keySet().iterator(); it2.hasNext();) {
                    c2 = (String) it2.next();
                    String domainCard = (String) cardinalities.get(c1);
                    if (domainCard == null) {
                        domainCard = "nil";
                    }
                    String rangeCard = (String) cardinalities.get(c2);
                    if (rangeCard == null) {
                        rangeCard = "nil";
                    }
                    if (!c1.equals(c2) || isTwoOrMore) {

                        String pred = ("(PredicateFn " + c1 + " " + c2 + ")");

                        // if (c1.equals(c2) && isTwoOrMore) {
                        //     System.out.println("  same domain and range: " + pred);
                        // }

                        String relatorStr = (stripNamespace(c1)
                                             + DOT_OP
                                             + stripNamespace(c2));

                        if (!domainCard.equals("0") && !rangeCard.equals("0")) {
                            compositeRelators.add(pred);
                            statements.add(makeStatement("termFormat",
                                                         namespace,
                                                         pred,
                                                         StringUtil.quote(relatorStr)));
                            statements.add(makeStatement("coa:HasCfMember",
                                                         family,
                                                         pred));
                            statements.add(makeStatement("coa:IsA", pred, "coa:Relator"));
                            statements.add(makeStatement("coa:HasDomain", pred, c1));
                            statements.add(makeStatement("coa:HasRange", pred, c2));
                            String inv = ("(PredicateFn " + c2 + " " + c1 + ")");
                            statements.add(makeStatement("coa:IsReciprocalOf", inv, pred));
                        }

                        String domainConstraint = ("coarule:" + relatorStr + "_DomainCardinality");
                        String rangeConstraint = ("coarule:" + relatorStr + "_RangeCardinality");
                        statements.add(makeStatement("coa_itd:HasRuleType",
                                                     domainConstraint,
                                                     "coa:RelatorCardinalityConstraint"));
                        statements.add(makeStatement("coa:ConstrainsDomain",
                                                     domainConstraint,
                                                     c1));
                        statements.add(makeStatement("coa:ConstrainsRelator",
                                                     domainConstraint,
                                                     pred));
                        makeCfConstraintCardinalityStatements(domainConstraint, 
                                                              domainCard,
                                                              statements);
                        statements.add(makeStatement("coa_itd:HasRuleType",
                                                     rangeConstraint,
                                                     "coa:RelatorCardinalityConstraint"));
                        statements.add(makeStatement("coa:ConstrainsRange",
                                                     rangeConstraint,
                                                     c2));
                        statements.add(makeStatement("coa:ConstrainsRelator",
                                                     rangeConstraint,
                                                     pred));
                        makeCfConstraintCardinalityStatements(rangeConstraint, 
                                                              rangeCard,
                                                              statements);
                    }
                }
            }
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
        return;
    }

    private static void makeCfConstraintCardinalityStatements(String constraint, 
                                                              String cardinality,
                                                              Set statements) {
        try {
            if (StringUtil.isNonEmptyString(constraint)
                && StringUtil.isNonEmptyString(cardinality)) {
                int minint = -1;
                int maxint = -1;
                String min = null;
                String max = null;
                int didx = cardinality.indexOf("-");
                if (didx > -1) { 
                    min = cardinality.substring(0,didx);
                    max = ((cardinality.length() > didx)
                           ? cardinality.substring(didx+1)
                           : "");
                }
                else if (StringUtil.isDigitString(cardinality)) {
                    min = cardinality;
                    max = cardinality;
                }
                if (min.equals("n")) {
                    min = Integer.toString(Integer.MAX_VALUE);
                }
                if (max.equals("n")) {
                    max = Integer.toString(Integer.MAX_VALUE);
                }
                try {
                    minint = Integer.parseInt(min);
                }
                catch (Exception e1) {
                    minint = -1;
                }
                try {
                    maxint = Integer.parseInt(max);
                }
                catch (Exception e2) {
                    maxint = -1;
                }
                ArrayList arg0s = new ArrayList();
                ArrayList arg2s = new ArrayList();
                if (minint > -1) {
                    if (minint == maxint) {
                        if (minint < Integer.MAX_VALUE) {
                            arg0s.add("coa:HasExactCardinality");
                            arg2s.add(Integer.toString(minint));
                        }
                        else {
                            arg0s.add("coa:HasMaxCardinality");
                            arg2s.add(Integer.toString(minint));  // Integer.toString(minint);
                        }
                    }
                    else if (minint < maxint) {
                        arg0s.add("coa:HasMinCardinality");
                        arg2s.add(Integer.toString(minint));
                        arg0s.add("coa:HasMaxCardinality");
                        arg2s.add(Integer.toString(maxint));
                    }
                }
                int a0Len = arg0s.size();
                String cArg2 = null;
                String cArg0 = null;
                String specArg0 = null;
                for (int i = 0; i < a0Len; i++) {
                    cArg0 = (String) arg0s.get(i);
                    cArg2 = (String) arg2s.get(i);
                    statements.add(makeStatement(cArg0, constraint, cArg2));
                }
            }
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
        return;
    }

    /** *************************************************************
     * Processes cardinality value strings.
     *
     * @param countable A String, the SUO-KIF term that denotes a type
     * of countable entity that is a member of a Context Family.
     *
     * @param cardinality A String token that denotes an integer
     * value, or a range consisting of min and max values
     *
     * @param cardinalitiess A Map relating Context Family members to
     * their cardinality values represented as Strings
     *
     * @param zeroCardinality A Set containing those Context Family
     * members for which the explicit cardinality is 0
     *
     * @return void
     */
    private void addCfCardinalityValues(String countable, 
                                        String cardinality,
                                        Map<String, String> cardinalities,
                                        Set zeroCardinality) {
        try {
            if (StringUtil.isNonEmptyString(countable)
                && StringUtil.isNonEmptyString(cardinality)) {
                String min = null;
                String max = null;
                int didx = cardinality.indexOf("-");
                if (didx > -1) { 
                    min = cardinality.substring(0,didx);
                    max = ((cardinality.length() > didx)
                           ? cardinality.substring(didx+1)
                           : "");
                }
                else if (StringUtil.isDigitString(cardinality)) {
                    min = cardinality;
                    max = cardinality;
                }
                if (min.equals("n")) {
                    min = Integer.toString(Integer.MAX_VALUE);
                }
                if (max.equals("n")) {
                    max = Integer.toString(Integer.MAX_VALUE);
                }
                if (!StringUtil.isDigitString(min)) {
                    min = max;
                }
                if (!StringUtil.isDigitString(max)) {
                    max = min;
                }
                String val = min;
                if (!min.equals(max)) {
                    val += ("-" + max);
                }
                cardinalities.put(countable, val);
                if (val.equals("0")) {
                    zeroCardinality.add(stripNamespace(countable));
                }
            }
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
        return;
    }

    /** *************************************************************
     * Normalizes each field of tblarr, which is a List of Lists.
     *
     * @param tblarr A List of Lists representing a spreadsheet or
     * database table
     *
     * @return void
     */
    private void normalizeCfTable(List<List> tblarr) {
        try {
            buildTableColumnNames(null, null);
            List constantKeys = Arrays.asList("B", "C", "L", "M", "N", "O", "P", "Q");
            boolean isNormalizing = false;
            String nsToken = "coa";
            String nsPrefix = (nsToken + StringUtil.getW3cNamespaceDelimiter());
            String termDelim = "|";
            String nameDelim = "!";
            StringBuilder sb = new StringBuilder();
            String cell = null;
            String newCell = null;
            String key = null;
            for (List row : tblarr) {
                int rowlen = row.size();
                for (int i = 0; i < rowlen; i++) {
                    key = getColumnKey(i);
                    cell = (String) row.get(i);
                    if (StringUtil.isNonEmptyString(cell)) {

                        // System.out.println("  cell == " + cell);
                        if (cell.matches(".*\\w+.*"))
                            cell = cell.trim();
                        newCell = StringUtil.removeEnclosingQuotes(cell);

                        if (key.equals("A") && newCell.equals("Family")) {
                            isNormalizing = true;
                        }
                        else if (isNormalizing) {
                            if (constantKeys.contains(key)) {

                                // if (key.equals("C")) {
                                //     System.out.println("  newCell == " + newCell);
                                // }

                                if (newCell.matches(".*\\s+.*") 
                                    // || newCell.matches(".*\\s*syn!.*")
                                    || newCell.matches(NAMES_FIELD_PATTERN)
                                    ) {
                                    sb = new StringBuilder();
                                    List<String> strings = 
                                        (newCell.contains("syn!")
                                         ? Arrays.asList(newCell.split("\\s*syn!"))
                                         : Arrays.asList(newCell.split("\\s+")));

                                    // System.out.println("  strings == " + strings);

                                    int j = 0;
                                    for (String str : strings) {
                                        int nidx = str.indexOf(nameDelim);
                                        if ((nidx > 0) 
                                            && (nidx < (str.length() - 1))) {
                                            str = str.substring(nidx + 1);
                                        }
                                        if (!str.startsWith(nsToken)) {
                                            str = (nsPrefix + str);
                                        }
                                        if (j > 0) sb.append(termDelim);
                                        sb.append(str);
                                        j++;
                                    }
                                    newCell = sb.toString();
                                }
                                else if (!newCell.startsWith(nsToken)) {
                                    newCell = (nsPrefix + newCell);
                                }
                            }
                        }
                        row.set(i, newCell);
                    }
                }
            }
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
        return;
    }


    /// END: code for processing Context Families


    /** *************************************************************
     * Compute core terms.
     */
    protected void computeCoreTerms(int n, 
                                    KB kb, 
                                    String focalTerm, 
                                    Set termSet, 
                                    List path,
                                    Map marked) {
        try {
            String nextTerm = null;
            Set reachable = (Set) marked.get(focalTerm);
            if (reachable == null) {
                reachable = new HashSet();
                marked.put(focalTerm, reachable);
            }
            for (Iterator it = termSet.iterator(); it.hasNext();) {

                nextTerm = (String) it.next();
                reachable.add(nextTerm);

                String ioClass = null;
                if (StringUtil.isLocalTermReference(nextTerm)) {
                    if (!isSkipNode(kb, nextTerm)) {
                        ioClass = getFirstContainingClass(kb, nextTerm);
                        if (StringUtil.isNonEmptyString(ioClass)) {
                            reachable.add(ioClass);
                        }
                    }
                }

                // reachable.add(nextTerm);

                Set nextTerms = new HashSet();
                nextTerms.addAll(kb.getTermsViaPredicateSubsumption("syntacticSubordinate",
                                                                    2,
                                                                    nextTerm,
                                                                    1,
                                                                    true));
                if (nextTerms.isEmpty() && (ioClass != null)) {
                    nextTerms
                        .addAll(kb.getTermsViaPredicateSubsumption("syntacticSubordinate",
                                                                   2,
                                                                   ioClass,
                                                                   1,
                                                                   true));
                }

                // 4.
                List more = getSyntacticExtensionTerms(kb, nextTerm, 2, false);
                if (more.isEmpty() && (ioClass != null)) {
                    more = getSyntacticExtensionTerms(kb, ioClass, 2, false);
                }
                nextTerms.addAll(more);
                        
                more.clear();
                more = getSyntacticUnionTerms(kb, nextTerm, 2);
                if (more.isEmpty() && (ioClass != null)) {
                    more = getSyntacticUnionTerms(kb, ioClass, 2);
                }
                nextTerms.addAll(more);

                // 7.
                List<String> subpreds = Arrays.asList("instance" //, "subclass"
                                                      );
                List children = null;
                String child = null;
                for (String pred : subpreds) {
                    nextTerms.addAll(kb.getTermsViaPredicateSubsumption(pred,
                                                                        2,
                                                                        nextTerm,
                                                                        1,
                                                                        true));
                }

                List nextPath = new LinkedList(path);
                nextPath.add(nextTerm);
                nextTerms.removeAll(reachable);
                nextTerms.removeAll(nextPath);
                if (!nextTerms.isEmpty()) {
                    computeCoreTerms((n + 1), 
                                     kb, 
                                     focalTerm, 
                                     nextTerms, 
                                     nextPath, 
                                     marked);
                }
            }
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
        return;
    }

    /** *************************************************************
     * Compute the core terms that can "belong" to focalTerm, along
     * with framework terms and supporting terms.
     */
    protected void computeCoreTerms(KB kb, String focalTerm) {
        long t1 = System.currentTimeMillis();

        /*            */
        System.out.println("ENTER DocGen.computeCoreTerms("
                           + kb.name + ", "
                           + focalTerm + ")");

        Set coreTermSet = new HashSet();
        getMaxDepthFromFocalTerm(kb, focalTerm, coreTermSet);

        String key = null;
        Set cts = null;
        for (Iterator it = coreTermSet.iterator(); it.hasNext();) {
            key = (String) it.next();
            if (!StringUtil.isLocalTermReference(key)) {
                cts = (Set) getCoreTerms().get(key);
                if (cts == null) {
                    cts = new HashSet();
                    getCoreTerms().put(key, cts);
                }
                cts.add(focalTerm);
            }
            List instances = kb.getTermsViaPredicateSubsumption("instance",
                                                                2,
                                                                key,
                                                                1,
                                                                true);
            String inst = null;
            for (Iterator it2 = instances.iterator(); it2.hasNext();) {
                inst = (String) it2.next();
                if (!StringUtil.isLocalTermReference(inst)) {
                    cts = (Set) getCoreTerms().get(inst);
                    if (cts == null) {
                        cts = new HashSet();
                        getCoreTerms().put(inst, cts);
                    }
                    cts.add(focalTerm);
                }
            }
        }

        /*        */
        System.out.println("EXIT DocGen.computeCoreTerms("
                           + kb.name + ", "
                           + focalTerm + ")");
        System.out.println("  > " 
                           + ((System.currentTimeMillis() - t1) / 1000.0)
                           + " seconds elpsed time");

        return;
    }

    /** *************************************************************
     * Compute framework terms in general.  Note that this method does
     * not compute the framework terms actually used in a given focal
     * term type.  Instead, it computes all of the framework terms
     * that could be used.
     */
    protected void computeFrameworkTerms(KB kb, String ontology) {

        long t1 = System.currentTimeMillis();
        System.out.println("ENTER DocGen.computeFrameworkTerms("
                           + kb.name + ", "
                           + ontology + ")");

        try {
            TreeSet focalTerms = new TreeSet(computeFocalTerms(kb, ontology));
            Set terms = new HashSet();
            List<String> initialTerms = Arrays.asList("instance",
                                                      "subclass",
                                                      "syntacticSubordinate",
                                                      "syntacticComposite",
                                                      "syntacticExtension",
                                                      "syntacticUnion",
                                                      "datatype",
                                                      "hasCardinality",
                                                      "part",
                                                      "subrelation",
                                                      "inverse",

                                                      "XmlNode");
            Set done = new HashSet();
            Set accumulator = new HashSet(initialTerms);
            List working = new ArrayList();
            String namespace = null;
            List subsumed = null;
            String term = null;
            String wt = null;
            Iterator it = null;
            Iterator it2 = null;

            while (!accumulator.isEmpty()) {
                working.clear();
                working.addAll(accumulator);
                accumulator.clear();

                for (it = working.iterator(); it.hasNext();) {
                    wt = (String) it.next();
                    if (!done.contains(wt)) {
                        if (!StringUtil.isLocalTermReference(wt)) {
                            namespace = getTermNamespace(kb, wt);
                            if (StringUtil.isNonEmptyString(namespace)) {
                                Set ftset = (Set) getFrameworkTerms().get(wt);
                                if (ftset == null) {
                                    ftset = focalTerms;
                                    getFrameworkTerms().put(wt, ftset);
                                }
                                else {
                                    ftset.addAll(focalTerms);
                                }
                            }
                        }

                        accumulator.addAll(kb.getTermsViaPredicateSubsumption("subrelation",
                                                                              2,
                                                                              wt,
                                                                              1,
                                                                              true));

                        accumulator.addAll(kb.getTermsViaPredicateSubsumption("inverse",
                                                                              2,
                                                                              wt,
                                                                              1,
                                                                              true));

                        accumulator.addAll(kb.getTermsViaPredicateSubsumption("inverse",
                                                                              1,
                                                                              wt,
                                                                              2,
                                                                              true));
                    
                        accumulator.addAll(kb.getTermsViaPredicateSubsumption("subclass",
                                                                              2,
                                                                              wt,
                                                                              1,
                                                                              true));

                        done.add(wt);
                    }
                }
            }
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }

        System.out.println("EXIT DocGen.computeFrameworkTerms("
                           + kb.name + ", "
                           + ontology + ")");
        System.out.println("  > "
                           + ((System.currentTimeMillis() - t1) / 1000.0)
                           + " seconds elapsed time");

        return;
    }

    /** *************************************************************
     * Compute supporting terms for the core terms in KB.
     */
    protected void computeSupportingTerms(KB kb) {

        long t1 = System.currentTimeMillis();
        System.out.println("ENTER DocGen.computeSupportingTerms("
                           + kb.name + ")");

        try {
            String key = null;
            String parent = null;
            String namespace = null;
            for (Iterator it = getCoreTerms().keySet().iterator(); it.hasNext();) {
                key = (String) it.next();
                parent = getFirstSubsumingTerm(kb, key);
                if (StringUtil.isNonEmptyString(parent)) {
                    namespace = getTermNamespace(kb, parent);
                    if (StringUtil.isNonEmptyString(namespace)) {
                        if ((getCoreTerms().get(parent) == null) 
                            && (getFrameworkTerms().get(parent) == null)) {
                            getSupportingTerms().put(parent, getCoreTerms().get(key));
                        }
                    }
                }
            }
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }

        System.out.println("EXIT DocGen.computeSupportingTerms(" + kb.name + ")");
        System.out.println("  > "
                           + ((System.currentTimeMillis() - t1) / 1000.0)
                           + " seconds elapsed time");

        return;
    }

    /** *************************************************************
     * Computes the maximum depth of the graph reachable along
     * syntacticSubordinate from the terms in focalTermSet.
     *
     * @param kb The KB in which the graph is to be computed
     *
     * @param focalTerm A top-level focal term from which maximum
     * reachable depth along syntacticSubordinate and its subrelations
     * is to be computed
     *
     * @param focalTermSet A Set containing the current "layer" of
     * nodes (terms) in the graph
     *
     * @param pathTerms A Set containing all of the unique
     * syntactically subordinate terms that can be reached from
     * focalTerm along a specific path of arcs (relations) and nodes
     * (terms)
     *
     * @param coreTermSet As a side effect, if non-null, this Set is
     * used to collect "core" terms that can be reached from focalTerm
     *
     * @return The int that is the maximum depth of the path
     * accessible from the terms in focalTermSet along
     * syntacticSubordinate
     *
     */
    protected int computeMaxDepthFromFocalTerm(KB kb, 
                                               String focalTerm,
                                               Set focalTermSet, 
                                               Set pathTerms,
                                               Set coreTermSet) {
        int depth = 0;
        try {
            depth = pathTerms.size();

            String nextTerm = null;
            for (Iterator it = focalTermSet.iterator(); it.hasNext();) {
                nextTerm = (String) it.next();

                if (coreTermSet != null) {
                    // 
                    // Begin compute core terms
                    // 
                    String ioClass = null;
                    if (StringUtil.isLocalTermReference(nextTerm)) {
                        if (!isSkipNode(kb, nextTerm)) {
                            ioClass = getFirstContainingClass(kb, nextTerm);
                            if (StringUtil.isNonEmptyString(ioClass)) {
                                coreTermSet.add(ioClass);
                            }
                        }
                    }
                    else {
                        coreTermSet.add(nextTerm);
                    }

                    List unionedTerms = getSyntacticUnionTerms(kb, nextTerm, 2);
                    if (unionedTerms.isEmpty() && (ioClass != null)) {
                        unionedTerms = getSyntacticUnionTerms(kb, ioClass, 2);
                    }
                    coreTermSet.addAll(unionedTerms);
                    // 
                    // End compute core terms
                    // 
                }

                if (!pathTerms.contains(nextTerm)) {
                    Set nextFocalTermSet = new HashSet();
                    nextFocalTermSet.addAll(getSubordinateAttributes(kb, nextTerm));
                    nextFocalTermSet.addAll(getSubordinateElements(kb, nextTerm));
                    List pair = getInheritedSubordinates(kb, 
                                                         nextTerm, 
                                                         coreTermSet // To compute core terms
                                                         );
                    nextFocalTermSet.addAll((List) pair.get(0));
                    nextFocalTermSet.addAll((List) pair.get(1));
                    Set nextPathTerms = new HashSet(pathTerms);
                    nextPathTerms.add(nextTerm);
                    int nextDepth = computeMaxDepthFromFocalTerm(kb, 
                                                                 focalTerm,
                                                                 nextFocalTermSet, 
                                                                 nextPathTerms,
                                                                 coreTermSet);
                    depth = ((depth > nextDepth) ? depth : nextDepth);
                }
            }
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
        return depth;
    }

    /** *************************************************************
     * Returns the maximum number of steps "descending" along
     * syntacticSubordinate from focalTerm.
     *
     * @param kb The KB in which syntacticSubordinate graph is
     * computed
     *
     * @param focalTerm The top-level term from which maximum graph
     * depth is to be computed
     *
     * @param coreTermSet As a side effect, if non-null, this Set is
     * used to collect core terms
     *
     * @return The int that is the maximum depth reachable from
     * focalTerm
     *
     */
    protected int getMaxDepthFromFocalTerm(KB kb, String focalTerm, Set coreTermSet) {

        long t1 = System.currentTimeMillis();
        System.out.println("ENTER DocGen.getMaxDepthFromFocalTerm("
                           + kb.name + ", "
                           + focalTerm + ", "
                           + ((coreTermSet == null)
                              ? null
                              : ("[coreTermSet: " + coreTermSet.size() + " terms]"))
                           + ")");

        int ans = 0;
        try {
            String intStr = (String) maxDepthFromFocalTerm.get(focalTerm);
            if (intStr == null) {
                Set focalTermSet = new HashSet();
                focalTermSet.add(focalTerm);
                Set pathTerms = new HashSet();
                ans = computeMaxDepthFromFocalTerm(kb, 
                                                   focalTerm,
                                                   focalTermSet, 
                                                   pathTerms, 
                                                   coreTermSet);
                maxDepthFromFocalTerm.put(focalTerm, String.valueOf(ans));
            }
            else {
                ans = Integer.parseInt(intStr);
            }
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }

        System.out.println("EXIT DocGen.getMaxDepthFromFocalTerm("
                           + kb.name + ", "
                           + focalTerm + ", "
                           + ((coreTermSet == null)
                              ? null
                              : ("[coreTermSet: " + coreTermSet.size() + " terms]"))
                           + ")");
        System.out.println("  > ans == " + maxDepthFromFocalTerm.get(focalTerm));

        return ans;
    }

    /** *************************************************************
     * Returns a List containing the subordinate XmlAttributes of
     * kifTerm, else return an empty List.
     */
    protected ArrayList getSubordinateAttributes(KB kb, String kifTerm) {
        ArrayList attrs = new ArrayList();
        try {
            // Get the direct subordinate attributes of kifTerm.
            String pred = "subordinateXmlAttribute";
            attrs.addAll(kb.getTermsViaPredicateSubsumption(pred,
                                                            2,
                                                            kifTerm,
                                                            1,
                                                            true));

            if (attrs.isEmpty()) {
                if (StringUtil.isLocalTermReference(kifTerm)) {
                    String gt = getFirstGeneralTerm(kb, kifTerm);
                    if (StringUtil.isNonEmptyString(gt)) {
                        attrs.addAll(kb.getTermsViaPredicateSubsumption(pred,
                                                                        2,
                                                                        gt,
                                                                        1,
                                                                        true));
                    }
                }
            }

            Collections.sort(attrs, String.CASE_INSENSITIVE_ORDER);
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
        return attrs;
    }

    /** *************************************************************
     * Returns a List containing the subordinate XmlElements of
     * kifTerm, else return an empty List.
     */
    protected ArrayList getSubordinateElements(KB kb, String kifTerm) {
        ArrayList elems = new ArrayList();
        try {
            String pred = "subordinateXmlElement";
            List nextElems = kb.getTermsViaPredicateSubsumption(pred,
                                                                2,
                                                                kifTerm,
                                                                1,
                                                                true);

            if (nextElems.isEmpty()) {
                if (StringUtil.isLocalTermReference(kifTerm)) {
                    String gt = getFirstGeneralTerm(kb, kifTerm);
                    if (StringUtil.isNonEmptyString(gt)) {
                        nextElems.addAll(kb.getTermsViaPredicateSubsumption(pred,
                                                                            2,
                                                                            gt,
                                                                            1,
                                                                            true));
                    }
                }
            }

            Collections.sort(nextElems, String.CASE_INSENSITIVE_ORDER);
            String eterm = null;
            for (Iterator eti = nextElems.iterator(); eti.hasNext();) {
                eterm = (String) eti.next();
                if (isInstanceOf(kb, eterm, "XmlSequenceElement")) {
                    elems.addAll(getSubordinateElements(kb, eterm));
                }
                else {
                    elems.add(eterm);
                }
            }
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
        return elems;
    }

    /** *************************************************************
     * Returns a List containing the "inherited" subordinates of
     * kifTerm, else return an empty List.  As a side effect, if
     * focalTerm is non-null the "core terms" inherited through
     * kifTerm are recorded in a Set that is a value in the coreTerms
     * Map.
     *
     * @param kb The KB from which relevant statements or terms are
     * gathered
     *
     * @param kifTerm The SUO-KIF term that is the starting point for
     * computing inherited syntactic subordinate terms
     *
     * @param coreTermSet As a side effect, if non-null, this Set is
     * used to collect "core" terms
     *
     * @return A List of the inherited syntactic subordinates of
     * kifTerm, or an empty List if no inherited subordinates can be
     * computed
     */
    protected ArrayList getInheritedSubordinates(KB kb, 
                                                 String kifTerm,
                                                 Set coreTermSet) {
        ArrayList subs = new ArrayList();
        try {
            List attrs = new ArrayList();
            List elems = new ArrayList();
            List extended = 
                kb.getTermsViaPredicateSubsumption("syntacticExtension",
                                                   1,
                                                   kifTerm,
                                                   2,
                                                   true);
            if (extended.isEmpty() && StringUtil.isLocalTermReference(kifTerm)) {
                String gt = getFirstGeneralTerm(kb, kifTerm);
                if (coreTermSet != null) coreTermSet.add(gt);
                extended.addAll(kb.getTermsViaPredicateSubsumption("syntacticExtension",
                                                                   1,
                                                                   gt,
                                                                   2,
                                                                   true));
            }
            String ext = null;
            for (Iterator exi = extended.iterator(); exi.hasNext();) {
                ext = (String) exi.next();
                if ((coreTermSet != null) && !StringUtil.isLocalTermReference(ext))
                    coreTermSet.add(ext);
                List inhattrs = getSubordinateAttributes(kb, ext);
                if (!inhattrs.isEmpty()) {
                    Collections.sort(inhattrs, String.CASE_INSENSITIVE_ORDER);
                    attrs.addAll(0, inhattrs);
                }
                List inhelems = getSubordinateElements(kb, ext);
                if (!inhelems.isEmpty()) {
                    Collections.sort(inhelems, String.CASE_INSENSITIVE_ORDER);
                    elems.addAll(0, inhelems);
                }
                List pair = getInheritedSubordinates(kb, ext, coreTermSet);
                List attrs1 = (List) pair.get(0);
                if (!attrs1.isEmpty()) {
                    Collections.sort(attrs1, String.CASE_INSENSITIVE_ORDER);
                    attrs.addAll(0, attrs1);
                }
                List elems1 = (List) pair.get(1);
                if (!elems1.isEmpty()) {
                    Collections.sort(elems1, String.CASE_INSENSITIVE_ORDER);
                    elems.addAll(0, elems1);
                }
            }

            subs.add(attrs);
            subs.add(elems);

        }
        catch (Exception ex) {
            ex.printStackTrace();
        }

        return subs;
    }

    /** *************************************************************
     * 
     */
    protected ArrayList getSyntacticExtensionTerms(KB kb, 
                                                   String term, 
                                                   int targetArgnum,
                                                   boolean computeClosure) {
        ArrayList result = null;
        try {
            int idxArgnum = ((targetArgnum == 2) ? 1 : 2);
            if (computeClosure) {
                result = kb.getTransitiveClosureViaPredicateSubsumption("syntacticExtension",
                                                                        idxArgnum,
                                                                        term,
                                                                        targetArgnum,
                                                                        true);
            }
            else {
                result = kb.getTermsViaPredicateSubsumption("syntacticExtension",
                                                            idxArgnum,
                                                            term,
                                                            targetArgnum,
                                                            true);
            }
            if (result.isEmpty() && StringUtil.isLocalTermReference(term)) {
                String gt = getFirstGeneralTerm(kb, term);
                if (StringUtil.isNonEmptyString(gt)) {
                    if (computeClosure) {
                        result = 
                            kb.getTransitiveClosureViaPredicateSubsumption("syntacticExtension",
                                                                           idxArgnum,
                                                                           gt,
                                                                           targetArgnum,
                                                                           true);
                    }
                    else {
                        result = 
                            kb.getTermsViaPredicateSubsumption("syntacticExtension",
                                                               idxArgnum,
                                                               gt,
                                                               targetArgnum,
                                                               true);
                    }
                }
            }
            SetUtil.removeDuplicates(result);
        }
        catch (Exception ex) {
            ex.printStackTrace();
            result = new ArrayList();
        }
        /*
          System.out.println("EXIT DocGen.getSyntacticExtensionTerms("
          + kb.name + ", "
          + term + ", "
          + targetArgnum + ")");
          System.out.println("  ==> [" + result.size() + " terms]");
        */
        return result;
    }

    /** *************************************************************
     * 
     */
    protected ArrayList getSyntacticUnionTerms(KB kb, String term, int targetArgnum) {
        ArrayList result = null;
        try {
            int idxArgnum = ((targetArgnum == 2) ? 1 : 2);
            result = kb.getTermsViaPredicateSubsumption("syntacticUnion",
                                                        idxArgnum,
                                                        term,
                                                        targetArgnum,
                                                        true);
            SetUtil.removeDuplicates(result);
        }
        catch (Exception ex) {
            ex.printStackTrace();
            result = new ArrayList();
        }
        /*
          System.out.println("EXIT DocGen.getSyntacticUnionTerms("
          + kb.name + ", "
          + term + ", "
          + targetArgnum + ")");
          System.out.println("  ==> [" + result.size() + " terms]");
        */
        return result;
    }

    /** *************************************************************
     * 
     */
    protected ArrayList getSyntacticCompositeTerms(KB kb, String term, int targetArgnum) {
        ArrayList result = null;
        try {
            int idxArgnum = ((targetArgnum == 2) ? 1 : 2);
            result = kb.getTermsViaPredicateSubsumption("syntacticComposite",
                                                        idxArgnum,
                                                        term,
                                                        targetArgnum,
                                                        true);
            SetUtil.removeDuplicates(result);
        }
        catch (Exception ex) {
            ex.printStackTrace();
            result = new ArrayList();
        }
        /*
          System.out.println("EXIT DocGen.getSyntacticCompositeTerms("
          + kb.name + ", "
          + term + ", "
          + targetArgnum + ")");
          System.out.println("  ==> [" + result.size() + " terms]");
        */
        return result;
    }

    /** *************************************************************
     * 
     */
    protected String getClosestXmlDataType(KB kb, String term) {
        String xmlType = null;
        try {
            xmlType = kb.getFirstTermViaPredicateSubsumption("closestXmlDataType",
                                                             1,
                                                             term,
                                                             2,
                                                             false);
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
        return xmlType;
    }

    /** *************************************************************
     * Supports memoization for isInstanceOf(kb, c1, c2).
     */
    private static Map isInstanceOfCache = new HashMap();


    /** *************************************************************
     * Returns true if i is an instance of c, else returns false.
     *
     * @param kb A KB object
     * @param i A String denoting an instance
     * @param c A String denoting a Class
     * @return true or false
     */
    protected static boolean isInstanceOf(KB kb, String i, String c) {
        boolean ans = false;
        try {
            Set classes = (Set) isInstanceOfCache.get(i);
            if (classes == null) {
                classes = kb.getAllInstanceOfsWithPredicateSubsumption(i);
                isInstanceOfCache.put(i, classes);
            }
            ans = classes.contains(c);
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
        return ans;
    }

    /** *************************************************************
     * 
     */
    protected String getFirstDatatype(KB kb, String term) {
        String dtype = null;
        try {
            List types = getDatatypeTerms(kb, term, 2);
            if (!types.isEmpty()) {
                dtype = (String) types.get(0);
            }
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
        return dtype;
    }

    /** *************************************************************
     * Gather the terms that should serve as the starting point for
     * exploring the graph that constitutes one or more documents.
     *
     */
    protected ArrayList computeFocalTerms(KB kb, String ontology) {
        ArrayList ans = new ArrayList();
        try {
            List classes = kb.getTermsViaAskWithRestriction(0, 
                                                            "docGenFocalInstanceClass", 
                                                            1, 
                                                            ontology,
                                                            2);
            if (!classes.isEmpty()) {
                Set reduced = new HashSet();
                String term = null;
                String inst = null;
                Set instances = null;
                Iterator it2 = null;
                for (Iterator it = classes.iterator(); it.hasNext();) {
                    term = (String) it.next();
                    instances = kb.getAllInstancesWithPredicateSubsumption(term);
                    for (it2 = instances.iterator(); it2.hasNext();) {
                        inst = (String) it2.next();
                        if (!StringUtil.isLocalTermReference(inst)) {
                            reduced.add(inst);
                        }
                    }
                }
                ans.addAll(reduced);
            }
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
        return ans;
    }

    /** *************************************************************
     * The time, represented in milliseconds since Jan 1 1970, at
     * which the most recent attempt to compute term relevance
     * occurred.  We use this value to avoid frequent, useless
     * attempts to compute term relevance.
     *
     */
    private long lastComputeTermRelevanceAttemptTime = 0L;

    /** *************************************************************
     * The time interval, in milliseconds, to wait before trying to
     * recompute term relevance.  We use this value to avoid frequent,
     * useless attempts to compute term relevance, but still permit
     * periodic retries.
     *
     */
    private long computeTermRelevanceRetryInterval = 600000L;  // 10 minutes

    /** *************************************************************
     * Assign terms to relevance sets according to whether the term is
     * core, framework, or supporting.
     *
     */
    protected void computeTermRelevance(KB kb, String ontology) {

        long t1 = System.currentTimeMillis();

        if ((lastComputeTermRelevanceAttemptTime == 0L)
            || ((lastComputeTermRelevanceAttemptTime + computeTermRelevanceRetryInterval) < t1)) {
                
            System.out.println("ENTER DocGen.computeTermRelevance("
                               + kb.name + ", "
                               + ontology + ")");

            try {

                // Start with focal terms.
                List focalTerms = computeFocalTerms(kb, ontology);
                if (!focalTerms.isEmpty()) {
                    String ft = null;
                    for (Iterator it = focalTerms.iterator(); it.hasNext();) {
                        ft = (String) it.next();
                        computeCoreTerms(kb, ft);
                    }

                    computeFrameworkTerms(kb, ontology);

                    computeSupportingTerms(kb);
                }
            }
            catch (Exception ex) {
                ex.printStackTrace();
            }

            lastComputeTermRelevanceAttemptTime = System.currentTimeMillis();

            System.out.println("EXIT DocGen.computeTermRelevance("
                               + kb.name + ", "
                               + ontology + ")");
            System.out.println("  > " 
                               + ((System.currentTimeMillis() - t1) / 1000.0)
                               + " seconds elapsed time");
        }

        return;
    }

    /** *************************************************************
     * 
     */
    protected ArrayList getDatatypeTerms(KB kb, String term, int targetArgnum) {
        ArrayList result = null;
        try {
            int idxArgnum = ((targetArgnum == 2) ? 1 : 2);
            result = kb.getTermsViaPredicateSubsumption("datatype",
                                                        idxArgnum,
                                                        term,
                                                        targetArgnum,
                                                        true);
            SetUtil.removeDuplicates(result);
        }
        catch (Exception ex) {
            ex.printStackTrace();
            result = new ArrayList();
        }
        return result;
    }

    /** *************************************************************
     * 
     */
    protected boolean isDataType(KB kb, String term) {
        boolean ans = false;
        try {
            List terms = getDatatypeTerms(kb, term, 1);
            if (terms.isEmpty()) {
                terms.addAll(kb.getTermsViaPredicateSubsumption("instance",
                                                                2,
                                                                term,
                                                                1,
                                                                true));
            }
            ans = !terms.isEmpty();
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
        return ans;
    }

    /** *************************************************************
     * This method computes and returns a List of Formulae with
     * instance or its child predicates that are entailed by
     * (derivable from) statements in the KB.
     *
     * @param kb The KB from which relevant statements or terms are
     * gathered
     *
     * @param term The index/reference term used to obtain initial
     * seed Formulae
     *
     * @param termArgNum An int value, the argument number of term
     *
     * @param isNamespaceRequired If true, only Formulae with terms
     * that have explicit namespace prefixes will be computed
     *
     * @return A List of derived Formulae
     */
    protected ArrayList<Formula> computeDerivedInstanceFormulae(KB kb, 
                                                                int termArgNum,
                                                                String term,
                                                                boolean isNamespaceRequired) {
        ArrayList<Formula> result = new ArrayList<Formula>();
        try {
            Set reduced = new TreeSet();
            Set broader = new TreeSet();
            Set accumulator = new TreeSet();
            accumulator.addAll(kb.getTermsViaPredicateSubsumption("instance",
                                                                  termArgNum,
                                                                  term,
                                                                  ((termArgNum == 1)
                                                                   ? 2
                                                                   : 1),
                                                                  true));
            List working = new LinkedList();
            String bt = null;
            Iterator it = null;
            while (!accumulator.isEmpty()) {
                working.clear();
                working.addAll(accumulator);
                accumulator.clear();
                for (it = working.iterator(); it.hasNext();) {
                    bt = (String) it.next();
                    if (!isNamespaceRequired 
                        || StringUtil.isNonEmptyString(getTermNamespace(kb, bt))) {
                        broader.add(bt);
                    }
                    accumulator.addAll(kb.getAllSuperClassesWithPredicateSubsumption(bt));
                }
            }
            List preds = kb.getTermsViaPredicateSubsumption("subrelation",
                                                            2,
                                                            "instance",
                                                            1,
                                                            true);
            preds.add("instance");
            Set narrower = new TreeSet();
            String nt = null;
            for (it = preds.iterator(); it.hasNext();) {
                nt = (String) it.next();
                if (!isNamespaceRequired 
                    || StringUtil.isNonEmptyString(getTermNamespace(kb, nt))) {
                    narrower.add(nt);
                }
            }
            StringBuilder sb = new StringBuilder();
            Iterator it2 = null;
            String p = null;
            String x = null;
            String y = null;
            for (it = narrower.iterator(); it.hasNext();) {
                p = (String) it.next();
                for (it2 = broader.iterator(); it2.hasNext();) {
                    x = term;
                    y = (String) it2.next();
                    if (termArgNum == 2) {
                        x = y;
                        y = term;
                    }
                    sb.setLength(0);
                    sb.append("(");
                    sb.append(p);
                    sb.append(" ");
                    sb.append(x);
                    sb.append(" ");
                    sb.append(y);
                    sb.append(")");
                    Formula newF = new Formula();
                    newF.read(sb.toString());
                    reduced.add(newF);
                }
            }
            result.addAll(reduced);
            Collections.sort(result);
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
        return result;
    }

    /** *************************************************************
     * 
     */
    public String getTermPresentationName(KB kb, String term) {
        String name = term;
        try {
            String namespace = this.getDefaultNamespace();
            name = getTermPresentationName(kb, namespace, term);
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
        return name;
    }

    /** *************************************************************
     * 
     */
    public String getTermPresentationName(KB kb, 
                                          String namespace, 
                                          String term,
                                          boolean withSpanTags) {
        String name = term;
        try {
            List context = new ArrayList();
            if (StringUtil.isNonEmptyString(namespace)) {
                context.add(namespace);
            }
            if (!context.contains("XMLLabel")) {
                context.add(0, "XMLLabel");
            }
            if (!context.contains("EnglishLanguage")) {
                context.add("EnglishLanguage");
            }
            name = getFirstTermFormat(kb, term, context);
            if (!StringUtil.isNonEmptyString(name) || name.equals(term)) {
                name = showTermName(kb, term, namespace);
            }
            if (StringUtil.isNonEmptyString(namespace)) {
                name = stripNamespacePrefix(kb, name);
            }
            // The for loop below is solely to handle
            // NonIsoTerritoryCode^Worldwide.
            String[] delims = {"^"};
            for (int i = 0; i < delims.length; i++) {
                int idx = name.indexOf(delims[i]);
                while ((idx > -1) && (idx < name.length())) {
                    name = name.substring(idx + 1);
                    idx = name.indexOf(delims[i]);
                }
            }
            if (withSpanTags) {
                if (isXmlAttribute(kb, term)) {
                    name = ("<span class=\"attribute\">" + name + "</span>");
                }
            }
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
        return name;
    }

    /** *************************************************************
     * 
     */
    public String getTermPresentationName(KB kb, 
                                          String namespace, 
                                          String term) {
        return getTermPresentationName(kb, namespace, term, false);
    }

    /** *************************************************************
     * Sorts stringList in place by the presentation name of each its
     * terms, which could be very different from the raw term name.
     *
     * @param kb The KB from which to obtain the presentation names
     *
     * @param namespaceTerm A KIF term denoting a namespace
     *
     * @param stringList The List of Strings to be sorted
     *
     * @return void
     *
     */
    protected void sortByPresentationName(KB kb, String namespaceTerm, List stringList) {
        /*
          System.out.println("ENTER DocGen.sortByPresentationName("
          + kb.name + ", "
          + namespaceTerm + ", "
          + stringList + ")");
        */
        try {
            if (!SetUtil.isEmpty(stringList) && (stringList.size() > 1)) {
                List<String[]> sortable = new ArrayList<String[]>();
                String kifNamespace = toKifNamespace(kb, namespaceTerm);
                String[] pair = null;
                for (Iterator it = stringList.iterator(); it.hasNext();) {
                    pair = new String[2];
                    pair[0] = (String) it.next();
                    pair[1] = getTermPresentationName(kb, kifNamespace, pair[0]);
                    sortable.add(pair);
                }
                Comparator comp = new Comparator() {
                        public int compare(Object o1, Object o2) {
                            String[] sa1 = (String[]) o1;
                            String[] sa2 = (String[]) o2;
                            return String.CASE_INSENSITIVE_ORDER.compare(sa1[1], sa2[1]);
                        }
                    };
                String msg = null;
                try {
                    Collections.sort(sortable, comp);
                }
                catch (Exception ex1) {
                    msg = ex1.getMessage();
                    System.out.println(msg);
                    ex1.printStackTrace();
                }
                if ((msg == null) && (sortable.size() == stringList.size())) {
                    stringList.clear();
                    for (String[] sa : sortable) {
                        stringList.add(sa[0]);
                    }
                }
            }
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
        /*
          System.out.println("EXIT DocGen.sortByPresentationName("
          + kb.name + ", "
          + namespaceTerm + ", "
          + stringList + ")");
        */
        return;
    }

    /** *************************************************************
     * Sorts the List terms by the length of the Strings it contains,
     * from longest to shortest.
     */
    protected void sortByTermLength(List terms) {
        try {
            if (!terms.isEmpty() && (terms.size() > 1)) {
                Comparator comp = new Comparator() {
                        public int compare(Object o1, Object o2) {
                            int l1 = o1.toString().length();
                            int l2 = o2.toString().length();
                            int ans = 0;
                            if (l1 > l2) {
                                ans = -1;
                            }
                            else if (l1 < l2) {
                                ans = 1;
                            }
                            return ans;
                        }
                    };
                Collections.sort(terms, comp);
            }
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
        return;
    }

    /** *************************************************************
     * 
     */
    public String getTermNamespace(KB kb, String term) {
        String result = "";
        try {
            if (StringUtil.isNonEmptyString(term)) {
                String prefix = getNamespacePrefix(kb, term);
                if (StringUtil.isNonEmptyString(prefix)) {
                    List<String> delims = Arrays.asList(StringUtil.getW3cNamespaceDelimiter(), 
                                                        StringUtil.getKifNamespaceDelimiter());
                    for (String delim : delims) {
                        if (prefix.endsWith(delim)) {
                            prefix = prefix.substring(0, prefix.length() - delim.length());
                            break;
                        }
                    }
                    result = (prefix.equals("ns")
                              ? prefix
                              : ("ns" + StringUtil.getKifNamespaceDelimiter() + prefix));
                }
            }
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
        return result;
    }

    /** *************************************************************
     * 
     * 
     * @param isXmlDoc If true, HTML character entities will be
     *                 replaced with their ASCII equivalents, when
     *                 possible
     *
     * @param addHrefs If true, HTML anchor markup will be added
     *                 for recognized terms
     *
     */
    protected String processDocString(KB kb, 
                                      String kbHref, 
                                      String namespace, 
                                      String docString,
                                      boolean isXmlDoc,
                                      boolean addHrefs) {
        /*
          System.out.println("ENTER DocGen.processDocString("
          + kb.name + ", "
          + kbHref + ", "
          + namespace + ", "
          + docString + ", "
          + isXmlDoc + ", "
          + addHrefs + ")");
        */
        String ans = docString;
        try {
            if (StringUtil.isNonEmptyString(docString)) {
                String nsTerm = toKifNamespace(kb, namespace);
                String tmpstr = StringUtil.normalizeSpaceChars(docString);
                Map srmap = getStringReplacementMap();
                if (isXmlDoc) {
                    if (srmap != null) {
                        String fromString = null;
                        String toString = null;
                        for (Iterator it = srmap.keySet().iterator(); it.hasNext();) {
                            fromString = (String) it.next();
                            toString = (String) srmap.get(fromString);
                            if (toString != null) {
                                tmpstr = tmpstr.replace(fromString, toString);
                            }
                        }
                    }
                }
                else {
                    // The "put" immediately below is to prevent
                    // the "&" in "&%" pairs from being replaced
                    // by the corresponding HTML entity.
                    srmap.put("&%", "&%");

                    StringBuilder sb = new StringBuilder(tmpstr);
                    String amp = "&";
                    int amplen = amp.length();
                    String repl = "&amp;";
                    int repllen = repl.length();
                    int p1f = sb.indexOf(amp);
                    String token = null;
                    while (p1f > -1) {
                        int p2f = -1;
                        for (Iterator it = srmap.keySet().iterator(); it.hasNext();) {
                            token = (String) it.next();
                            p2f = sb.indexOf(token, p1f);
                            if ((p2f > -1) && (p1f == p2f)) break;
                        }
                        if ((p2f > -1) && (p1f == p2f)) {
                            p2f += token.length();
                            if (p2f < sb.length()) {
                                p1f = sb.indexOf(amp, p2f);
                            }
                        }
                        else {
                            sb.replace(p1f, p1f + amplen, repl);
                            p2f = p1f + repllen;
                            p1f = sb.indexOf(amp, p2f);
                        }
                    }
                    tmpstr = sb.toString();
                }
                tmpstr = StringUtil.removeEnclosingQuotes(tmpstr);
                tmpstr = StringUtil.removeQuoteEscapes(tmpstr);
                if (StringUtil.isNonEmptyString(tmpstr)) {
                    String commentToken = " //";
                    int headPos = tmpstr.indexOf(commentToken);
                    if ((headPos > -1) && ((headPos + 4) < tmpstr.length())) {
                        String head = tmpstr.substring(0, headPos);
                        String tail = tmpstr.substring(headPos + commentToken.length());
                        tmpstr = ("<span class=\"commentHead\">"
                                  + head
                                  + "</span><br/>"
                                  + tail);
                    }
                    if (addHrefs) {
                        tmpstr = kb.formatDocumentation(kbHref, tmpstr, nsTerm);
                    }
                }
                ans = tmpstr;
            }
            else {
                ans = "";
            }
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
        /*
          System.out.println("EXIT DocGen.processDocString("
          + kb.name + ", "
          + kbHref + ", "
          + namespace + ", "
          + docString + ", "
          + isXmlDoc + ", "
          + addHrefs + ")");
          System.out.println("  ans == " + ans);
        */
        return ans;
    }

    /** *************************************************************
     * This method writes a set of SUO-KIF Formulae (Strings) to the
     * file identified by the pathname String outpath.  First, it
     * gathers and sorts all instances of the Classes or Sets provided
     * in the List classes.  Then, for each instance or element, it
     * retrieves statements formed with the relations and argument
     * specifications provided in the List relationsToUse, and writes
     * all statements to the output file.
     *
     * @param kb The KB from which relevant statements or terms are
     * gathered
     *
     * @param formulaType A term denoting a type of Formula, the
     * instances or elements of which will be gathered
     *
     * @param relationsToUse A List of predicates that are used to
     * gather statements pertaining to the instances of the Classes
     * listed in classes
     *
     * @param replacements A List of regular expression/substitution
     * pairs that, if present, will be used to transform each
     * expression before it is written to pw
     *
     * @param isNamespaceRequired If true, output of Formulae for
     * which the main predicate (argument 0) lacks an explicit
     * namespace prefix will be suppressed
     *
     * @param defaultNamespace If a non-empty String, this value
     * denotes the default namespace for computation
     *
     * @param outpath The pathname String of the output file
     *
     * @return void
     */
    protected void exportFormulaeToFile(KB kb, 
                                        String formulaType,
                                        List relationsToUse,
                                        List replacements,
                                        boolean isNamespaceRequired,
                                        String defaultNamespace,
                                        String outpath) {

        System.out.println("ENTER DocGen.exportFormulaeToFile("
                           + kb.name + ", "
                           + formulaType + ", "
                           + relationsToUse + ", "
                           + replacements + ", "
                           + isNamespaceRequired + ", "
                           + defaultNamespace + ", "
                           + outpath + ")");

        PrintWriter pw = null;
        try {
            if (StringUtil.isNonEmptyString(formulaType) 
                && StringUtil.isNonEmptyString(outpath)) {
                Set instances = new TreeSet();
                instances.addAll(kb.getAllInstancesWithPredicateSubsumption(formulaType));
                
                // Try harder to get all relevant terms.
                /*
                  List predlist = new LinkedList();
                  predlist.add("kifManifestation");
                  String pred = kb.getFirstTermViaPredicateSubsumption("subrelation",
                  2,
                  "kifManifestation",
                  1,
                  true);
                  if (StringUtil.isNonEmptyString(pred)) {
                  predlist.add(pred);
                  }

                  for (it = predlist.iterator(); it.hasNext();) {
                  pred = (String) it.next();
                  instances.addAll(kb.getTermsViaAsk(0, pred, 1));
                  }
                */

                List pair = null;
                String inst = null;
                String idxStr = null;
                Iterator itr = null;
                Set formulae = new LinkedHashSet();
                StringBuilder sb = new StringBuilder();
                String sbout = "";

                System.out.println(instances.size() + " instances");

                List printable = new ArrayList(instances);
                Collections.sort(printable);
                for (Iterator it = printable.iterator(); it.hasNext();) {
                    inst = (String) it.next();
                    String tns = getTermNamespace(kb, inst);

                    // System.out.println("namespace for " + inst + " == " + tns);
                    
                    if (!isNamespaceRequired || StringUtil.isNonEmptyString(tns)) {
                        formulae.clear();
                        String pred = null;
                        for (itr = relationsToUse.iterator(); itr.hasNext();) {
                            pair = (List) itr.next();
                            if ((pair != null) && (pair.size() > 1)) {
                                pred = (String) pair.get(0);
                                idxStr = (String) pair.get(1);
                                int argIdx = 1;
                                try {
                                    argIdx = Integer.parseInt(idxStr);
                                }
                                catch (Exception pex) {
                                    argIdx = 1;
                                    pex.printStackTrace();
                                }
                                List flist = kb.askWithPredicateSubsumption(pred, argIdx, inst);
                                if (pred.equals("documentation")) {
                                    String ptrn = (".*documentation.+" 
                                                   + (StringUtil.isNonEmptyString(defaultNamespace)
                                                      ? defaultNamespace
                                                      : "EnglishLanguage")
                                                   + ".*");
                                    retainByPattern(flist, ptrn);
                                }
                                formulae.addAll(flist);
                            }
                        }

                        List iflist = computeDerivedInstanceFormulae(kb, 
                                                                     1,
                                                                     inst,
                                                                     isNamespaceRequired);
                        // retainByPattern(iflist, ".+Rule\\)$|.+Constraint\\)$");
                        formulae.addAll(iflist);

                        // System.out.println(formulae.size() + " formulae");

                        if (!formulae.isEmpty()) {
                            // sb.setLength(0);
                            sb.append(";; ");
                            sb.append(inst);
                            sb.append(StringUtil.getLineSeparator());
                            sb.append(StringUtil.getLineSeparator());
                            Formula f = null;
                            for (Iterator itf = formulae.iterator(); itf.hasNext();) {
                                f = (Formula) itf.next();
                                sb.append(f.format("", "  ", StringUtil.getLineSeparator()));
                                sb.append(StringUtil.getLineSeparator());
                                sb.append(StringUtil.getLineSeparator());

                                String arg0 = f.car();
                                if (arg0.matches(".*(?i)kif.*")
                                    && StringUtil.isNonEmptyString(getTermNamespace(kb, arg0))) {
                                    Formula origF = new Formula();
                                    origF.read(f.getArgument(2));
                                    sb.append(origF.format("", "  ", StringUtil.getLineSeparator()));
                                    sb.append(StringUtil.getLineSeparator());
                                    sb.append(StringUtil.getLineSeparator());
                                }
                            }
                        }
                    }
                }
                sbout = sb.toString();
                if (StringUtil.isNonEmptyString(sbout)) {
                    if ((replacements != null) && !replacements.isEmpty()) {
                        String patx = null;
                        String paty = null;
                        for (itr = replacements.iterator(); itr.hasNext();) {
                            pair = (List) itr.next();
                            if ((pair != null) && (pair.size() > 1)) {
                                patx = (String) pair.get(0);
                                paty = (String) pair.get(1);
                                sbout = sbout.replace(patx, paty);
                            }
                        }
                    }

                    File outfile = new File(outpath);
                    pw = new PrintWriter(new FileWriter(outfile));
                    pw.println(sbout);
                    pw.flush();

                    if (outfile.exists() && outfile.canRead()) {
                        System.out.println("  Written: " + outpath);
                    }
                }
            }
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
        finally {
            try {
                if (pw != null) pw.close();
            }
            catch (Exception ioe) {
                ioe.printStackTrace();
            }
        }

        System.out.println("EXIT DocGen.exportFormulaeToFile("
                           + kb.name + ", "
                           + formulaType + ", "
                           + relationsToUse + ", "
                           + replacements + ", "
                           + isNamespaceRequired + ", "
                           + defaultNamespace + ", "
                           + outpath + ")");

        return;
    }

    /** *************************************************************
     * Writes a List of Strings to an OutputStream.
     *
     * @param data A List of String objects
     *
     * @param stream An OutputSteam object
     *
     * @return void
     */
    public static void writeToStream(List<String> data, OutputStream stream) {
        DataOutputStream dout = null;
        try {
            if (!data.isEmpty()) {
                dout = new DataOutputStream(new BufferedOutputStream(stream));
                for (String chars : data) {
                    dout.writeChars(chars);
                }
                dout.flush();
            }
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
        finally {
            try {
                if (dout != null) dout.close();
            }
            catch (Exception ioe) {
                ioe.printStackTrace();
            }
        }
        return;
    }

    /** *************************************************************
     * Returns the list of file type suffixes that identify input SCOW
     * spreadsheet files.
     */
    private static final List getInputFileSuffixes() {
        return Arrays.asList(".dif", ".csv");
    }

    /** *************************************************************
     * Returns a List of the names of all client ontologies currently
     * represented in any loaded KB.
     */
    protected static List<String> getClientOntologyNames() {
        ArrayList<String> clientOntologyNames = new ArrayList<String>();
        try {
            Set<String> ontologies = new HashSet<String>();
            for (Iterator it = KBmanager.getMgr().kbs.values().iterator(); it.hasNext();) {
                KB kb = (KB) it.next();
                ontologies.addAll(kb.getTermsViaAsk(0, "docGenClientOntology", 2));
            }
            clientOntologyNames.addAll(ontologies);
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
        return clientOntologyNames;
    }

    private static final String[] ssColumns = 
    {"A",  "B",  "C",  "D",  "E",  "F",  "G",  "H",  "I",  "J",  
     "K",  "L",  "M",  "N",  "O",  "P",  "Q",  "R",  "S",  "T", 
     "U",  "V",  "W",  "X",  "Y",  "Z",  "AA", "AB", "AC", "AD", 
     "AE", "AF", "AG", "AH", "AI", "AJ", "AK", "AL", "AM", "AN",
     "AO", "AP", "AQ", "AR", "AS", "AT", "AU", "AV", "AW", "AX", 
     "AY", "AZ", "BA", "BB", "BC", "BD", "BE", "BF", "BG", "BH",
     "BI", "BJ", "BK", "BL", "BM", "BN", "BO", "BP", "BQ", "BR",
     "BS", "BT", "BU", "BV", "BW", "BX", "BY", "BZ", "CA", "CB"};

    /** *************************************************************
     * A List of Lists.  Each inner list is a List of Strings.  This
     * List is used to index columns by name in
     * DocGen.processSpreadsheet().
     */
    private ArrayList<ArrayList> tableColumnNames = new ArrayList<ArrayList>();

    /**
     * Tries to build a list of column name indexes, so the symbolic
     * names can be used to select the appropriate column when
     * processing a spreadsheet ArrayList.  If either kb or ontology
     * cannot be determined, the method just uses the static String[]
     * ssColumns.
     *
     */
    protected void buildTableColumnNames (KB kb, String ontology) {
        try {
            String infoObject = null;
            if (kb instanceof KB) {
                if (StringUtil.isNonEmptyString(ontology)) {
                    infoObject = kb.getFirstTermViaAskWithRestriction(0, "containsInformation",
                                                                      2, ontology,
                                                                      1);
                }
                if (StringUtil.isNonEmptyString(infoObject)) {
                    List formulae = kb.askWithRestriction(0,
                                                          "tableIndexColumnNames",
                                                          1, 
                                                          infoObject);
                    if (!formulae.isEmpty()) {
                        int cap = formulae.size();
                        tableColumnNames.ensureCapacity(cap);
                        Formula f = null;
                        int highestIdx = -1;
                        int idx = -1;
                        String nameList = null;
                        ArrayList nameArr = null;
                        String arg0 = null;
                        for (Iterator it = formulae.iterator(); it.hasNext();) {
                            try {
                                f = (Formula) it.next();
                                idx = Integer.parseInt(f.getArgument(2));
                                while (tableColumnNames.size() < (idx + 1)) {
                                    tableColumnNames.add(new ArrayList());
                                }
                                nameList = f.getArgument(3);
                                Formula newF = new Formula();
                                newF.read(nameList);
                                if (newF.listP() && !newF.empty()) {
                                    arg0 = StringUtil.removeEnclosingQuotes(newF.car());
                                    if (arg0.equals("ListFn")) {
                                        newF.read(newF.cdr());
                                    }
                                    nameArr = (ArrayList) tableColumnNames.get(idx);
                                    while (newF.listP() && !newF.empty()) {
                                        arg0 = StringUtil.removeEnclosingQuotes(newF.car());
                                        if (!nameArr.contains(arg0)) {
                                            nameArr.add(arg0);
                                        }
                                        newF.read(newF.cdr());
                                    }
                                    tableColumnNames.set(idx, nameArr);
                                }
                            }
                            catch (Exception fex) {
                                fex.printStackTrace();
                            }
                        }
                    }
                }
            }
            while (tableColumnNames.size() < ssColumns.length) {
                tableColumnNames.add(new ArrayList());
            }
            int tcnLen = tableColumnNames.size();
            ArrayList nameArr = null;
            for (int i = 0; i < tcnLen; i++) {
                nameArr = (ArrayList) tableColumnNames.get(i);
                if (nameArr.isEmpty()) {
                    nameArr.add(ssColumns[i]);
                }
            }
            // System.out.println("tableColumnNames == " + tableColumnNames);
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
        return;
    }

    /** *******************************************************************
     */
    public static boolean emptyString(String s) {
        return StringUtil.emptyString(s);
    }

    /** *******************************************************************
     */
    public static boolean isNonEmptyString(String s) {
        return StringUtil.isNonEmptyString(s);
    }

    /** *******************************************************************
     */
    protected int getColumnIndex(String columnName) {
        int ans = -1;
        try {
            if (isNonEmptyString(columnName)) {
                int tcnLen = tableColumnNames.size();
                List nameList = null;
                for (int i = 0; i < tcnLen; i++) {
                    nameList = (List) tableColumnNames.get(i);
                    if (nameList.contains(columnName)) {
                        ans = i;
                        break;
                    }
                }
            }
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
        return ans;
    }

    /** *******************************************************************
     */
    protected String getColumnKey(int colIdx) {
        String ans = "";
        try {
            if ((colIdx > -1) && (colIdx < tableColumnNames.size())) {
                List nameList = (List) tableColumnNames.get(colIdx);
                if ((nameList != null) && !nameList.isEmpty()) {
                    ans = (String) nameList.get(0);
                }
            }
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
        return ans;
    }

    /** *******************************************************************
     */
    private String unnamedReferenceToKifTerm(int seqNum, 
                                             int rowNum, 
                                             int colNum, 
                                             Map nodeNameMap,
                                             String s) {
        String ans = s;
        try {
            if (s instanceof String) {
                String val = s.trim();
                val = StringUtil.removeEnclosingQuotes(val);
                if (StringUtil.isNonEmptyString(val)
                    && (val.startsWith("#") || val.startsWith("~"))) {
                    String base = StringUtil.getLocalReferenceBaseName();
                    // if (ch == '#') { base += "Leaf"; }
                    String nameKey = (base + "_" + seqNum + "_" + val);
                    String nodeName = (String) nodeNameMap.get(nameKey);
                    if (StringUtil.emptyString(nodeName)) {
                        nodeName = (base 
                                    + "_" 
                                    + seqNum 
                                    + "_" 
                                    + rowNum 
                                    + "_"
                                    + colNum);
                        nodeNameMap.put(nameKey, nodeName);
                    }
                    ans = nodeName;
                }
            }
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
        return ans;
    }

    /** *******************************************************************
     * Parse a file of delimited fields into an ArrayList of
     * ArrayLists.  If rowFlags contains any Strings, concatenate
     * lines that do not start with one of the Strings, else ignore
     * rowFlags.
     * 
     * @param inPath The pathname of the file to be processed.
     *
     * @param delimiter A char that delimits each field in a row.
     *
     * @param noOfDelimiters An int that indicates the number of
     * expected delimiter chars per row.
     *
     * @param rowFlags A List of the tokens (Strings) that indicate
     * the start of a new row.
     *
     * @param targetFieldIdx An int indicating the index of the
     * column in which KIF formulae are available.
     *
     * @return void
     */
    public void writeScowKifFormulae(String inPath,
                                     char delimiter,
                                     int noOfDelimiters,
                                     List rowFlags,
                                     int targetFieldIdx) {
        try {
            List al = readSpreadsheetFile(inPath, delimiter, noOfDelimiters, rowFlags);
            if ((al != null) && !al.isEmpty()) {
                int alen = al.size();
                String ls = System.getProperty("line.separator");
                String basename = inPath.substring(0, inPath.lastIndexOf("."));
                String outPath = basename + ".kif";
                File outfile = new File(outPath);
                PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(outfile)));
                int exprCount = 0;
                List obj = null;
                int olen = -1;
                String expr = null;
                for (int i = 0; i < alen; i++) {
                    obj = (List) al.get(i);
                    olen = obj.size();
                    expr = null;
                    if (olen > targetFieldIdx) {
                        expr = (String) obj.get(targetFieldIdx);
                    }
                    if (isNonEmptyString(expr)) {
                        expr = expr.replaceAll("\\)  ", "\\)" + ls + SP2);
                        expr = expr.replaceAll("=>  ", "=>" + ls + SP2);
                        expr = expr.replaceAll("<=>  ", "<=>" + ls + SP2);
                        expr = expr.replaceAll("and  ", "and" + ls + SP2);
                        expr = expr.replaceAll("or  ", "or" + ls + SP2);
                        expr = expr.replaceAll("not  ", "not" + ls + SP2);
                        expr = expr.replaceAll("HaveCardinality  ", "HaveCardinality" + ls + SP2);
                        expr = expr.replaceAll("    \\(HaveCardinalityFn",
                                               ls + "    \\(HaveCardinalityFn");
                        expr = expr.replaceAll("      \\(KappaFn", ls + "      \\(KappaFn");
                        out.println("");
                        out.println(";; Row " + i);
                        out.println(expr);
                        exprCount++;
                    }
                }
                if (out != null) {
                    try {
                        out.close();
                    }
                    catch (Exception pwe) {
                    }
                }
                System.out.println(exprCount + " expressions written to " + outPath);
            }
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
        return;
    }
                                                

    /** *******************************************************************
     * Parses a file of delimited fields into an ArrayList of
     * ArrayLists.  If rowFlags contains any Strings, concatenate
     * lines that do not start with one of the Strings, else ignore
     * rowFlags.
     * 
     * @param fname The pathname of the file to be processed
     *
     * @param delimiter A char that delimits each field in a row
     *
     * @param delimitersPerRow An int that indicates the number of
     * expected delimiter chars per row
     *
     * @param rowFlags A List of the tokens (Strings) that indicate
     * the start of a new row
     *
     * @return An ArrayList of ArrayLists
     */
    public ArrayList readSpreadsheetFile(String inpath, 
                                         char delimiter, 
                                         int delimitersPerRow,
                                         List<String> rowFlags) {

        ArrayList rows = new ArrayList();
        LineNumberReader lr = null;

        try {
            File infile = new File(inpath);
            lr = new LineNumberReader(new FileReader(infile));
            boolean useDelimitersPerRow = (delimitersPerRow > 0);
            boolean useRowFlags = ((rowFlags instanceof List) && !rowFlags.isEmpty());
            int rowFlagsSize = (useRowFlags ? rowFlags.size() : 0);
            int rowsSize = 0;
            int lastIdx = -1;
            int prevDelimCount = 0;
            int delimiterCount = 0;
            boolean isRowStart = false;
            String prevLine = "";
            String line = null;

            // 1. Try to parse the input file into a List of Strings,

            // with each String representing one row.
            while ((line = lr.readLine()) != null) {

                rowsSize = rows.size();
                lastIdx = rowsSize - 1;
                if (lastIdx >= 0) { prevLine = (String) rows.get(lastIdx); }

                if (!useDelimitersPerRow && !useRowFlags) {
                    rows.add(line);
                    continue;
                }

                // 1.a. Use explicit leading tokens to determine when
                // a new row starts.
                if (useRowFlags) {
                    isRowStart = false;
                    for (String token : rowFlags) {
                        if (line.startsWith(token)) {
                            isRowStart = true;
                            break;
                        }
                    }
                    if (isRowStart) {
                        rows.add(line);
                    }
                    else if (lastIdx >= 0) {
                        line = prevLine + line;
                        rows.set(lastIdx, line);
                    }
                }

                // 1.b. Try to determine when a new row starts by
                // counting the number of field delimiters in each
                // line.  This assumes that the number of fields
                // (delimiters) in each row will be constant.
                if (useDelimitersPerRow) {
                    if (!useRowFlags && rows.isEmpty()) {
                        rows.add(line); 
                        continue;
                    }

                    prevDelimCount = countDelimiters(prevLine, delimiter);
                    delimiterCount = countDelimiters(line, delimiter);

                    if (!useRowFlags) {
                        if (prevDelimCount == delimitersPerRow) {
                            rows.add(line);
                        }
                        else if (prevDelimCount < delimitersPerRow) {
                            line = prevLine + line;
                            rows.set(lastIdx, line);
                        }
                    }

                    if (countDelimiters(line, delimiter) > delimitersPerRow) {
                        System.out.println("WARNING in DocGen.readSpreadsheetFile(" 
                                           + inpath + ", " 
                                           + delimiter + ", " 
                                           + delimitersPerRow + ", " 
                                           + rowFlags + ")");
                        System.out.println("  Too many delimiters read near line "
                                           + lr.getLineNumber());
                    }
                }
            }

            System.out.println(lr.getLineNumber() + " lines read from " + inpath);

            // Close the input stream.
            if (lr != null) { lr.close(); }

            // 2. Convert the List of Strings into a List of Lists in
            // which each List member represents one cell from the
            // original spreadsheet.
            if (!rows.isEmpty()) {

                // Add a dummy row to the beginning to make the row
                // numbers (maybe) match those in the spreadsheet.
                rows.add(0, "start");

                // Convert all rows to Lists.
                convertRowStringsToLists(rows, delimiter);
            }
        }
        catch (Exception ex) {
            ex.printStackTrace();
        } 
        return rows;
    }

    /** *******************************************************************
     * Converts a List of char-delimited Strings to a List of Lists
     * (i.e., an array) representing a spreadsheet.
     * 
     * @param rows A List of char-delimited Strings.  The list is
     * processed in place, and will contain only Lists when
     * processing is finished.
     *
     * @param delimiter A char that delimits each field in line.
     *
     * @return void
     */
    private void convertRowStringsToLists(List rows, char delimiter) {
        try {
            String line = null;
            StringBuilder cell = null;
            boolean inString = false;
            char ch = '0';
            int rowCount = rows.size();
            System.out.println("INFO in DocGen.convertRowStringsToLists(" 
                               + "[" + rowCount + " rows], " 
                               + delimiter + ")");
            System.out.println("  Converting " + rowCount + " Strings to Lists");
            for (int c = 0; c < rowCount; c++) {
                line = (String) rows.remove(0);
                ArrayList row = new ArrayList();
                if (isNonEmptyString(line)) {
                    inString = false;
                    cell = new StringBuilder();
                    for (int j = 0; j < line.length(); j++) {
                        ch = line.charAt(j);
                        if (ch == delimiter && !inString) {
                            row.add(cell.toString().trim());
                            cell = new StringBuilder();
                        } else if (ch == '"' 
                                   && (j == 0 || line.charAt(j-1) != '\\')) {
                            inString = !inString;
                        } else  {
                            cell.append(ch);
                        }
                    }
                    row.add(cell.toString().trim());
                }
                // row will be an empty ArrayList if line is an empty
                // String.
                rows.add(row);
            }
            System.out.println(SP2 + rows.size() + " rows saved");
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
        return;
    }

    /** *******************************************************************
     * Counts the number of field delimiters in line and returns the
     * count, ignoring delimiters that are inside Strings.
     * 
     * @param line A line of data fields that are separated by
     * occurrences of a delimiter char.
     *
     * @param delimiter A char that delimits each field in line.
     *
     * @return An int indicating the number of delimiters in line.
     */
    private int countDelimiters(String line, char delimiter) {
        int count = 0;
        try {
            int len = line.length();
            boolean inString = false;
            char ch = '0';
            for (int i = 0; i < len; i++) {
                ch = line.charAt(i);
                if (ch == delimiter && !inString) {
                    count++;
                } 
                else if (ch == '"' && (i == 0 || line.charAt(i-1) != '\\')) {
                    inString = !inString;
                }
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        return count;
    }

    /** *******************************************************************
     * Reads all RDFQL triples from the input file and returns a List
     * of Lists.  Each sublist is a triple consisting of three
     * Strings.
     * 
     * @param fname The pathname of the input file.
     *
     * @return A List of Lists.
     */
    public ArrayList readRdfqlDataFile(String fname) {
        ArrayList triples = new ArrayList();
        FileReader fr = null;
        BufferedReader br = null;
        try {
            fr = new FileReader(fname);
            br = new BufferedReader(fr);
            ArrayList triple = new ArrayList();
            StringBuilder term = new StringBuilder();
            char ch0 = '0';
            char ch1 = '0';
            boolean inString = false;
            boolean inTriple = false;
            String line = null;
            int i = 0;
            int len = 0;
            while ((line = br.readLine()) != null) {
                len = line.length();
                for (i = 0; i < len; i++) {
                    ch1 = line.charAt(i);
                    if (!inString && (ch1 == '/')) {
                        // The rest of the line is commented out.
                        break;
                    }
                    if (inTriple && (inString || ((ch1 != '{') && (ch1 != '}')))) {
                        term.append(ch1);
                    }
                    if ((ch1 == '{') && !inString) {
                        inTriple = true;
                    }
                    if ((ch1 == '}') && !inString) {
                        triples.add(triple);
                        triple = new ArrayList();
                        inTriple = false;
                    }
                    if ((ch1 == '\'') && (ch0 != '\\')) { 
                        inString = !inString;
                    }
                    if (((ch1 == ']') || (ch1 == '\'')) && inTriple && !inString) {
                        triple.add(term.toString().trim());
                        term = new StringBuilder();
                    }
                    ch0 = ch1;
                }
            }
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
        try {
            if (br != null) { br.close(); }
            if (fr != null) { fr.close(); }
        }
        catch (Exception ex2) {
        }
        return triples;
    }

    /** *******************************************************************
     * Reads all RDFQL triples from the input file and uses these
     * triples as a "seed" to generate new data files, creating new
     * SPID terms.  The resulting files are put in the same directory
     * as the seed file, and are named by adding successive numeric
     * suffixes to the seed file name.  Returns an ArrayList
     * containing the absolute pathnames (Strings) of the files
     * written.
     * 
     * @param seedFile The pathname of the input file.
     *
     * @param totalStatements The total number of statements to
     * generate.
     *
     * @return An ArrayList containing the pathnames of the files
     * written.
     */
    public ArrayList writeRdfqlSampleDataFiles(String seedFile, int totalStatements) {
        PrintWriter out = null;
        ArrayList written = new ArrayList();
        try {
            List seedList = readRdfqlDataFile(seedFile);
            if ((seedList != null) && !seedList.isEmpty()) {
                String pkgPath = "/ccli/data/";
                File f = new File(seedFile);
                String fullPath = f.getCanonicalPath();
                String suffix = fullPath.substring(fullPath.lastIndexOf("."), fullPath.length());
                String base = fullPath.substring(0, fullPath.lastIndexOf("."));
                HashMap idPairs = new HashMap();
                List statementBuffer = new ArrayList();
                int totalStatementCount = 0;
                int fileStatementCount = 0;
                int idCount = 1;
                int fileCount = 1;
                String outpath = (base + "_" + fileCount + suffix);
                File file = new File(outpath);
                try {
                    if (file.exists()) { 
                        file.delete(); 
                    }
                }
                catch (Exception ex) {
                }
                file = new File(outpath);
                out = new PrintWriter(new BufferedWriter(new FileWriter(file, true)));
                out.println("// " + pkgPath + file.getName());
                int statementsPerFile = 25000;
                int insertsPerGroup = 1000;
                List oldTriple = null;
                List newTriple = null;
                boolean hasSpid = false;
                String oldTerm = null;
                String newTerm = null;
                int i = 0;
                while ((i < seedList.size()) && (totalStatementCount < totalStatements)) {
                    if (i == 0) { idPairs.clear(); }
                    hasSpid = false;
                    oldTriple = (List) seedList.get(i);
                    newTriple = new ArrayList();
                    for (int j = 0; j < oldTriple.size(); j++) {
                        oldTerm = (String) oldTriple.get(j);
                        newTerm = oldTerm;
                        if (oldTerm.startsWith("[ccli:spid_")) {
                            hasSpid = true;
                            newTerm = (String) idPairs.get(oldTerm.intern());
                            if (newTerm == null) {
                                newTerm = (oldTerm.substring(0, oldTerm.length()-1)
                                           + "_"
                                           + idCount
                                           + "]");
                                idCount++;
                                idPairs.put(oldTerm.intern(), newTerm.intern());
                            }
                        }
                        newTriple.add(newTerm);
                    }
                    if (hasSpid) { statementBuffer.add(newTriple); }
                    if ((statementBuffer.size() >= insertsPerGroup)
                        || (!statementBuffer.isEmpty() 
                            && (statementBuffer.size() + totalStatementCount) > totalStatements)) {
                        out.println();
                        out.println("INSERT");
                        while (!statementBuffer.isEmpty()) {
                            newTriple = (List) statementBuffer.remove(0);
                            out.print("  {");
                            for (int k = 0; k < newTriple.size(); k++) {
                                out.print(newTriple.get(k));
                                if (k < 2) { out.print(" "); }
                            }
                            out.println("}");
                            fileStatementCount++;
                            totalStatementCount++;
                        }
                        out.println("INTO #ccli_data;");
                        if (!written.contains(outpath)) { written.add(outpath); }
                        System.out.print(". ");
                    }
                    if (fileStatementCount >= statementsPerFile) {
                        try {
                            if (out != null) {
                                out.close();
                                out = null;
                            }
                        }
                        catch (Exception cex) {
                        }
                        if (totalStatementCount < totalStatements) {
                            fileStatementCount = 0;
                            fileCount++;
                            outpath = (base + "_" + fileCount + suffix);
                            File file1 = new File(outpath);
                            try {
                                if (file1.exists()) { 
                                    file1.delete(); 
                                }
                            } 
                            catch (Exception ex) {
                            }
                            file1 = new File(outpath);
                            out = new PrintWriter(new BufferedWriter(new FileWriter(file1, true)));
                            out.println("// " + pkgPath + file1.getName());
                        }
                    }
                    i++;
                    if (i >= seedList.size()) {
                        i = 0;
                    }
                }
                System.out.println(totalStatementCount + " total statements written");
            }
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
        try { 
            if (out != null) {
                out.close();
                out = null;
            }
        }
        catch (Exception ex1) { 
            ex1.printStackTrace(); 
        }
        return written;
    }

    /** *******************************************************************
     * Get the value of a spreadsheet cell, which is represented by String 
     * in a particular position in an ArrayList that is a row.  Cells may 
     * have namespaces and values, delimited by "^" or "!".
     * 
     * 
     *
     * @param relator if true will lowercase the first char of the
     *                return
     */
    private String getSpreadsheetValue(List row, 
                                       int rowNum,
                                       String colName,
                                       String nsDefault, 
                                       Set statements, 
                                       boolean isRelator) {
        return getSpreadsheetValue(row,
                                   rowNum,
                                   getColumnIndex(colName),
                                   nsDefault,
                                   statements,
                                   isRelator);
    }

    /** *******************************************************************
     * Get the value of a spreadsheet cell, which is represented by String 
     * in a particular position in an ArrayList that is a row.  Cells may 
     * have namespaces and values, delimited by "^" or "!".
     * 
     * @param relator if true will lowercase the first char of the
     *                return
     */
    private String getSpreadsheetValue(List row, 
                                       int rowNum,
                                       int colNum, 
                                       String nsDefault, 
                                       Set statements, 
                                       boolean isRelator) {
        String value = "";
        String namespace = "";
        String headword = "";
        nsDefault = normalizeNamespace(nsDefault);
        try {
            if (row.size() > colNum) {
                String cell = (String) row.get(colNum);
                // cell = cell.trim();
                if (cell.matches(".*\\w+.*"))
                    cell = cell.trim();
                if (cell.matches(".*(?i)\\$blank.*")
                    || cell.matches("(?i)null")) {
                    cell = "";
                }

                value = cell;

                if (isNonEmptyString(value) 
                    && !value.matches(".*\\s+.*")
                    && !StringUtil.isUri(value)
                    && !StringUtil.isQuotedString(value)) {

                    List<String> valList = new ArrayList<String>();
                    if (colNum == getColumnIndex("R")) {
                        valList.addAll(Arrays.asList(value.split("\\|")));
                    }
                    else {
                        valList.add(value);
                    }
                    String token = null;
                    String subject = null;
                    String newVal = null;
                    int nsDelimIdx = -1;
                    String kifDefaultNs = StringUtil.w3cToKif(nsDefault);
                    String w3cDefautlNs = StringUtil.kifToW3c(nsDefault);
                    String kifSubject = null;
                    String kifNamespace = null;
                    String w3cNamespace = null;
                    String localW3cNsPrefix = null;
                    for (String v : valList) {
                        // System.out.println("v == " + v);
                        nsDelimIdx = v.lastIndexOf(getW3cNamespaceDelimiter());
                        if (nsDelimIdx > -1) {
                            namespace = v.substring(0, nsDelimIdx);
                            namespace = normalizeNamespace(namespace);
                            // System.out.println("namespace == " + namespace);
                            newVal = v.substring(nsDelimIdx + getW3cNamespaceDelimiter().length());
                            token = getNameRelationToken(namespace);
                            kifNamespace = StringUtil.w3cToKif(namespace);
                            w3cNamespace = StringUtil.kifToW3c(kifNamespace);
                            localW3cNsPrefix = 
                                (w3cNamespace
                                 .substring(w3cNamespace.lastIndexOf(getW3cNamespaceDelimiter())
                                            + getW3cNamespaceDelimiter().length())
                                 + getW3cNamespaceDelimiter());
                            if (isNonEmptyString(token)) {
                                kifNamespace = getRelationTokenNamespace(namespace, 
                                                                         token,
                                                                         kifDefaultNs);
                                subject = getSpreadsheetValue(row, 
                                                              rowNum,
                                                              getColumnIndex("M"), 
                                                              nsDefault, 
                                                              statements, 
                                                              isRelator);
                                kifSubject = StringUtil.w3cToKif(subject);

                                // System.out.println("-1- kifSubject == " + kifSubject);

                                if (token.equals("_hw")) {
                                    statements.add(makeStatement("headword", 
                                                                 kifNamespace,
                                                                 kifSubject,
                                                                 StringUtil.quote(newVal)));
                                    if (!v.startsWith(localW3cNsPrefix)) {
                                        statements.add(makeStatement("termFormat",
                                                                     kifNamespace,
                                                                     kifSubject,
                                                                     StringUtil.quote(newVal)));
                                    }
                                    statements.add(makeStatement("termFormat EnglishLanguage",
                                                                 kifSubject,
                                                                 StringUtil.quote(newVal)));
                                }
                                else if (token.equals("_syn")) {
                                    statements.add(makeStatement("synonym",
                                                                 kifNamespace,
                                                                 kifSubject,
                                                                 StringUtil.quote(newVal)));
                                }
                                else if (token.equals("_en")) {
                                    statements.add(makeStatement("termFormat EnglishLanguage",
                                                                 kifSubject,
                                                                 StringUtil.quote(newVal)));
                                }
                                else if (token.equals("xsdname")) {
                                    statements.add(makeStatement("xsdFileName",
                                                                 StringUtil.quote(newVal),
                                                                 kifSubject));
                                }
                                // System.out.println("pseudoTypeOrd == " + pseudoTypeOrd);
                                // System.out.println("           v1 == " + v1);
                            }
                            else {
                                headword = getHeadword(v);
                                kifSubject = StringUtil.w3cToKif(v);

                                // System.out.println("-2- kifSubject == " + kifSubject);

                                int didx = kifSubject.indexOf(getKifNamespaceDelimiter());
                                String pref = null;
                                if (didx > -1) {
                                    pref = kifSubject.substring(0, didx);
                                    if (kifNamespace.endsWith(pref)) {
                                        statements.add(makeStatement("inNamespace",
                                                                     kifSubject,
                                                                     kifNamespace));
                                    }
                                }
                                if (isRelator) {
                                    statements.add(makeStatement("instance",
                                                                 kifSubject,
                                                                 "Predicate"));
                                }
                                if (isNonEmptyString(nsDefault)
                                    && isNonEmptyString(namespace)
                                    && !nsDefault.equalsIgnoreCase(namespace)) {
                                    String nsHeadword = getHeadword(namespace);
                                    String nsPrefix = (nsHeadword + getW3cNamespaceDelimiter());
                                    String nsDefaultHeadword = getHeadword(nsDefault);
                                    String nsDefaultPrefix = 
                                        (nsDefaultHeadword + getW3cNamespaceDelimiter());
                                    newVal = ((kifDefaultNs.equals(kifSubject)
                                               || nsHeadword.equals(getHeadword(nsDefault))
                                               || nsPrefix.equals(nsDefaultPrefix))
                                              ? headword
                                              : (nsPrefix + headword));
                                    
                                    int delIdx = kifDefaultNs.indexOf(getKifNamespaceDelimiter());
                                    int kndLen = getKifNamespaceDelimiter().length();
                                    String kifDefaultNsPrefix = 
                                        ((delIdx > -1)
                                         ? (kifDefaultNs.substring(delIdx + kndLen) 
                                            + getW3cNamespaceDelimiter())
                                         : "");
                                    if (isNonEmptyString(kifDefaultNsPrefix)
                                        && newVal.startsWith(kifDefaultNsPrefix)) {
                                        newVal = stripNamespace(newVal);
                                    }

                                    statements.add(makeStatement("termFormat",
                                                                 kifDefaultNs,
                                                                 kifSubject,
                                                                 StringUtil.quote(newVal)));
                                }
                            }
                            value = v;
                        }
                    }
                    // value = value.trim();
                    if (value.matches(".*\\w+.*"))
                        value = value.trim();
                }
            }
        }
        catch (Exception ex) {
            System.out.println("ERROR in DocGen.getSpreadsheetValue("
                               + row + ", "
                               + rowNum + ", "
                               + colNum + ", "
                               + nsDefault + ", "
                               + statements + ", "
                               + isRelator + ")");
            ex.printStackTrace();
        }
        return value;
    }

    /** *******************************************************************
     * Returns the value of the input String, minus any namespace
     * prefix.
     *
     * @return String
     */
    protected String stripNamespace(String input) {
        String value = input;
        try {
            int idx = -1;
            for (String token : getNamespaceDelimiters()) {
                idx = value.indexOf(token);
                if (idx > -1) {
                    idx += token.length();
                    if (idx < value.length()) {
                        value = value.substring(idx);
                        break;
                    }
                }
            }
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
        return value;
    }

    /** *******************************************************************
     * 
     */
    private String getNameRelationToken(String pseudoNamespace) {
        String ans = "";
        try {
            for (String token : getNameRelationTokens()) {
                if (pseudoNamespace.endsWith(token)) {
                    ans = token;
                    break;
                }
            }
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
        return ans;
    }

    /** *******************************************************************
     * 
     */
    private String getRelationTokenNamespace(String pseudoNamespace, 
                                             String token,
                                             String defaultNamespace) {
        String ans = defaultNamespace;
        try {
            int idx = pseudoNamespace.indexOf(token);
            if (idx > -1) {
                ans = pseudoNamespace.substring(0, idx);
            }
            ans = normalizeNamespace(ans);
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
        return ans;
    }

    /** *******************************************************************
     * Returns the KIF term for the namespace of the input term.  In
     * normalized (W3C) form, namespace prefixes are delimited from
     * base (unqualified) terms by ":".
     *
     * @param input A term, which may or may not have a namespace
     * prefix
     *
     * @return The KIF term that represents the namespace of the input
     * term, or an empty string if no namespace can be determined for
     * the input term
     */
    private String getNamespace(String term) {
        String namespace= "";
        try {
            if (isNonEmptyString(term) 
                && !StringUtil.isStringWithSpaces(term)
                && !StringUtil.isUri(term)
                && !StringUtil.isDigitString(term)
                && !Formula.listP(term)) {
                int idx = term.indexOf(getW3cNamespaceDelimiter());
                if (idx > -1) {
                    namespace = term.substring(0, idx);
                    namespace = normalizeNamespace(namespace);
                }
            }
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
        return namespace;
    }

    /** *******************************************************************
     * Get the namespace for the term in a spreadsheet cell, where a
     * cell is represented by a String in a particular position in the
     * List that is a row.  Cells may have namespaces and values.  In
     * normalized (W3C) form, namespace names are delimited from
     * values by ":".
     */
    private String getNamespace(List row, int colNum) {
        String namespace= "";
        if (row.size() > colNum) {                                          
            String cell = (String) row.get(colNum);
            if (isNonEmptyString(cell)) {
                namespace = getNamespace(cell);
            }
        }
        return namespace;
    }

    /** *******************************************************************
     * Get the namespace for a spreadsheet cell, where a cell is
     * represented by a String in a particular position in the List
     * that is a row.  Cells may have namespaces and values.  In
     * normalized (W3C) form, namespace names are delimited from
     * values by by ":".
     */
    private String getNamespace(List row, String colNumStr) {
        int colNum = getColumnIndex(colNumStr);
        return getNamespace(row, colNum);
    }

    /** *******************************************************************
     * Tries to return a normalized (canonical) SUO-KIF term name for
     * namespace.
     */
    public String normalizeNamespace(String namespace) {
        String normalized = namespace;
        try {
            if (isNonEmptyString(namespace)) {
                normalized = normalized.trim();
                String prefix = "ns" + StringUtil.getW3cNamespaceDelimiter();
                String kifNsPrefix = "ns" + StringUtil.getKifNamespaceDelimiter();
                if (!normalized.equals("ns") 
                    && !normalized.startsWith(kifNsPrefix)) {
                    if (normalized.startsWith(prefix)) {
                        normalized = (kifNsPrefix + normalized.substring(prefix.length()));
                    }
                    else {
                        String val = (String) headwordsToNamespaces.get(normalized);
                        if (isNonEmptyString(val)) {
                            normalized = val;
                        }
                        else {
                            String hw = normalized;
                            normalized = kifNsPrefix + hw;
                            headwordsToNamespaces.put(hw, normalized);
                            namespacesToHeadwords.put(normalized, hw);
                            headwords.put(normalized, hw);
                        }
                    }
                }
            }
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
        return normalized;
    }

    /** *******************************************************************
     */
    private String getHeadword(String term) {
        String ans = term;
        if (isNonEmptyString(term)) {
            String val = (String) headwords.get(term);
            if (isNonEmptyString(val)) {
                ans = val;
            }
            else {
                int idx = term.indexOf(getW3cNamespaceDelimiter());
                if (idx > -1) {
                    int wndLen = getW3cNamespaceDelimiter().length();
                    val = term.substring(idx + wndLen);
                }
                if (isNonEmptyString(val))
                    ans = val;
                else {
                    idx = term.indexOf(getSafeNamespaceDelimiter());
                    if (idx > -1) {
                        int slen = getSafeNamespaceDelimiter().length();
                        val = term.substring(idx + slen);
                        if (isNonEmptyString(val))
                            ans = val;
                    }
                }
            }
        }
        return ans;
    }

    /** *******************************************************************
     * Returns a String reprentation of a SUO-KIF Formula in which all
     * terms have been normalized by adding the proper namespace
     * prefix.
     * 
     * @param flist A String representing a SUO-KIF Formula, or some
     * term in a Formula
     *
     * @param idx An int value, the index of the current term (flist)
     * in the list containing it, if any
     *
     * @param nsPrefixes A List of permitted namespace prefixes, to be
     * used for normalizing the terms in flist
     *
     * @param namespace A String denoting the default namespace
     *
     * @return A String 
     * 
     */
    private String normalizeTermsInFormula(String flist,
                                           int idx,
                                           List nsPrefixes,
                                           String namespace) {

        /*
          System.out.println("ENTER DocGen.normalizeTermsInFormula("
          + flist + ", "
          + idx + ", "
          + nsPrefixes + ", "
          + namespace + ")");
        */

        String result = "";
        try {
            StringBuilder sb = new StringBuilder();
            flist = flist.trim();
            sortByTermLength(nsPrefixes);
            if (Formula.listP(flist)) {
                if (Formula.empty(flist)) {
                    sb.append(flist);
                }
                else {
                    Formula f = new Formula();
                    f.read(flist);
                    List tuple = f.literalToArrayList();
                    sb.append("(");
                    int i = 0;
                    for (Iterator it = tuple.iterator(); it.hasNext(); i++) {
                        if (i > 0) sb.append(" ");
                        sb.append(normalizeTermsInFormula((String) it.next(),
                                                          i,
                                                          nsPrefixes,
                                                          namespace));
                    }
                    sb.append(")");
                }
            }
            else if (Formula.isVariable(flist)
                     || Formula.isLogicalOperator(flist)
                     || Formula.isFunction(flist)) {
                sb.append(flist);
            }
            else if (StringUtil.isQuotedString(flist)) {
                sb.append(flist);
            }
            else {
                String defaultNS = namespace;
                if (idx == 0) {
                    defaultNS = getDefaultPredicateNamespace();
                    if (StringUtil.emptyString(defaultNS)) {
                        defaultNS = namespace;
                    }
                }
                sb.append(normalizeTermsInString(flist, nsPrefixes, defaultNS));
            }
            result = sb.toString();
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }

        /*
          System.out.println("  result == " + result);
          System.out.println("EXIT DocGen.normalizeTermsInFormula("
          + flist + ", "
          + idx + ", "
          + nsPrefixes + ", "
          + namespace + ")");
        */

        return result;
    }

    /** *******************************************************************
     */
    private List normalizeTerms(List rows, String defaultNamespace) {

        long t1 = System.currentTimeMillis();
        System.out.println("ENTER DocGen.normalizeTerms([" 
                           + rows.size() + " rows], "
                           + defaultNamespace + ")");

        // System.out.println("Normalizing terms in SUO-KIF statements");
        Map namespacesToHws = new HashMap();
        Map hwsToNamespaces = new HashMap();
        Map terms = new HashMap();
        List normalized = new ArrayList();
        normalized.add(namespacesToHws);
        normalized.add(hwsToNamespaces);
        normalized.add(terms);
        try {
            if (rows != null) {
                String coaNs = null;
                String coaNsHw = null;
                String coaTerm = null;
                List columns = null;
                String cell = null;
                String cellPart = null;
                String newCellPart = null;
                List vals = new ArrayList();
                int nsIdx = -1;
                String nsNamespace = "ns";
                String nsNamespacePrefix = "ns" + getW3cNamespaceDelimiter();
                String namespace = null;
                String nsPrefix = null;
                String nsHeadword = null;
                String term = null;
                String sep = null;
                String[] nsSeparators = {"!", "^"};
                int seqNum = 0;
                int previousBlankNodeRow = -1;
                Map nodeNameMap = new HashMap();
                int totalRows = rows.size();
                for (int r = 0; r < totalRows; r++) {
                    columns = (List) rows.get(r);
                    if (columns != null) {
                        for (int c = 0; c < columns.size(); c++) {
                            cell = (String) columns.get(c);
                            if (cell instanceof String) {
                                cell = StringUtil.removeEnclosingQuotes(cell);
                                // cell = cell.trim();
                                if (cell.matches(".*\\w+.*"))
                                    cell = cell.trim();
                                if (isNonEmptyString(cell)) {
                                    if (StringUtil.isLocalTermReference(cell)) {
                                        if ((r - previousBlankNodeRow) > 1) {
                                            seqNum++;
                                        }
                                        cell = unnamedReferenceToKifTerm(seqNum, 
                                                                         r, 
                                                                         c, 
                                                                         nodeNameMap,
                                                                         cell);
                                        previousBlankNodeRow = r;
                                    }
                                    else if (StringUtil.isUri(cell)
                                             || (cell.matches(".+\\s.+\\s.+\\s.+")
                                                 && !cell.matches(NAMES_FIELD_PATTERN))) {
                                        continue;
                                    }
                                    else {
                                        vals.clear();
                                        if (cell.matches(NAMES_FIELD_PATTERN)) {
                                            vals.addAll(Arrays.asList(cell.split("\\s")));
                                        }
                                        else {
                                            vals.add(cell);
                                        }
                                        cell = "";
                                        for (int v = 0; v < vals.size(); v++) {
                                            cellPart = (String) vals.get(v);
                                            // cellPart = cellPart.trim();
                                            if (cellPart.matches(".*\\w+.*"))
                                                cellPart = cellPart.trim();
                                            newCellPart = cellPart;
                                            nsIdx = -1;
                                            namespace = null;
                                            nsHeadword = null;
                                            for (int i = 0; i < nsSeparators.length; i++) {
                                                sep = nsSeparators[i];
                                                nsIdx = cellPart.indexOf(sep);
                                                if (nsIdx > -1) {
                                                    namespace = cellPart.substring(0, nsIdx);
                                                    namespace = 
                                                        StringUtil.replaceNonIdChars(namespace);
                                                    if (namespace.equals(nsNamespace)) {
                                                        nsHeadword = nsNamespace;
                                                    }
                                                    else if (namespace.startsWith(nsNamespacePrefix)) {
                                                        nsHeadword = 
                                                            namespace.substring(nsNamespacePrefix.length());
                                                    }
                                                    else if (namespace.matches(".*xsdname.*")) {
                                                        nsHeadword = "xsdname";
                                                        namespace = (nsNamespacePrefix 
                                                                     + nsHeadword);
                                                    }
                                                    else {
                                                        nsHeadword = namespace.intern();
                                                        namespace = (nsNamespacePrefix 
                                                                     + namespace);
                                                    }
                                                    namespace = namespace.intern();
                                                    namespacesToHws.put(namespace, nsHeadword);
                                                    hwsToNamespaces.put(nsHeadword, namespace);
                                                    nsPrefix = 
                                                        (nsHeadword + getW3cNamespaceDelimiter());
                                                    nsPrefix = nsPrefix.intern();
                                                    term = cellPart.substring(nsIdx + 1);
                                                    String newTerm = 
                                                        StringUtil.replaceNonIdChars(term);
                                                    String key = nsPrefix + newTerm;
                                                    String hw = term;
                                                    if (sep.equals("!") 
                                                        && nsPrefix.matches(".*(?i)_hw.*")) {
                                                        int caretIdx = term.lastIndexOf("^");
                                                        if ((caretIdx > -1) 
                                                            && (caretIdx < term.length())) {
                                                            hw = term.substring(caretIdx + 1);
                                                        }
                                                        headwords.put(key, hw);
                                                    }
                                                    hw = (String) headwords.get(key);
                                                    if (!StringUtil.isNonEmptyString(hw)) {
                                                        headwords.put(key, term);
                                                    }
                                                    newCellPart = nsPrefix + term;
                                                    if (sep.equals("^")) {
                                                        newCellPart = key;
                                                    }
                                                    else if (sep.equals("!") 
                                                             && nsPrefix.matches(".*(?i)_hw.*")) {
                                                        newCellPart = nsPrefix + hw;
                                                    }
                                                    newCellPart = newCellPart.intern();
                                                    terms.put(cellPart, newCellPart);
                                                    break;
                                                }
                                            }
                                            if (v > 0) { cell += "|"; }
                                            cell += newCellPart;
                                        }
                                    }
                                    columns.set(c, cell);
                                }
                            }
                        }
                    }
                }
                if (!namespacesToHws.isEmpty()) {
                    List nslist = new ArrayList(namespacesToHws.keySet());
                    String nsk = null;
                    String nsk2 = null;
                    String nsv = null;
                    String w3cpref = ("ns" + getW3cNamespaceDelimiter());
                    String kifpref = ("ns" + getKifNamespaceDelimiter());
                    for (Iterator it = nslist.iterator(); it.hasNext();) {
                        nsk = (String) it.next();
                        if (nsk.startsWith(w3cpref)) {
                            nsv = (String) namespacesToHws.get(nsk);
                            if (StringUtil.isNonEmptyString(nsv)) {
                                nsk2 = (kifpref + nsk.substring(w3cpref.length()));
                                namespacesToHws.put(nsk2, nsv);
                            }
                        }
                    }
                }
            }
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }

        System.out.println("EXIT DocGen.normalizeTerms([" 
                           + rows.size() + " rows], "
                           + defaultNamespace + ")");
        System.out.println("  > "
                           + ((System.currentTimeMillis() - t1) / 1000.0)
                           + " seconds elapsed time");

        return normalized;
    }

    /** *******************************************************************
     *
     */
    private String normalizeTermsInString(String input, List nsHeadwords, String namespace) {
        /*
          System.out.println("ENTER DocGen.normalizeTermsInString(" 
          + input + ", "
          + nsHeadwords + ", "
          + namespace + ")");
        */
        String ans = input;
        try {
            if (isNonEmptyString(input) && !StringUtil.isDigitString(input)) {
                sortByTermLength(nsHeadwords);
                String key = null;
                String hw = null;
                String repl = null;
                for (Iterator it = nsHeadwords.iterator(); it.hasNext();) {
                    hw = (String) it.next();
                    repl = (hw + getKifNamespaceDelimiter());
                                    
                    // The two lines immediately below are to deal
                    // with occurrences of the old KIF namespace
                    // delimiter, a single "_".  When possible, it
                    // should be removed.
                    key = (hw + "_");
                    ans = ans.replaceAll("(?i)" + key, repl);
                    ans = ans.replaceAll("(?i)" + repl, repl);
                    ans = ans.replaceAll("(?i)" + hw + getKifNamespaceDelimiter() + "+", repl);
                }

                // The following regex replacement expr is to remove
                // the non-ASCII characters that mark those uppercase
                // characters in the SCOW that are *not* supposed to
                // be preceded by &%.
                ans = ans.replaceAll("[^\\p{ASCII}]([\\p{Upper}])", "$1");

                /*
                  ans = ans.replaceAll("([\\p{Alnum}])&([\\p{Alnum}])", 
                  "$1&amp;$2");
                */

                ans = ans.replaceAll("(\\s[^&%]+\\w+)" + getKifNamespaceDelimiter() + "(\\w+)",
                                     "$1\\" + getW3cNamespaceDelimiter() + "$2");

                // cell = StringUtil.replaceNonAsciiChars(cell);
                if (!ans.contains(getKifNamespaceDelimiter()) 
                    && isNonEmptyString(namespace)
                    && (namespacesToHeadwords != null)) {
                    hw = (String) namespacesToHeadwords.get(namespace);
                    if (isNonEmptyString(hw)) {
                        ans = (hw + getKifNamespaceDelimiter() + ans);
                    }
                }
            }
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
        /*
          System.out.println("EXIT DocGen.normalizeTermsInString(" 
          + input + ", "
          + nsHeadwords + ", "
          + namespace + ")");
          System.out.println("  ==> " + ans);
        */
        return ans;
    }

    /** *******************************************************************
     *
     */
    private void normalizeTermsInComplexFields(List rows, 
                                               Map hwsToNamespaces, 
                                               String defaultNamespace) {

        long t1 = System.currentTimeMillis();
        System.out.println("ENTER DocGen.normalizeTermsInComplexFields("
                           + "[" + rows.size() + " rows], " 
                           + "[" + hwsToNamespaces.size() + " map entries], "
                           + defaultNamespace + ")");

        // System.out.println("Normalizing SUO-KIF terms in formulae and documentation strings");
        try {
            if ((rows != null) && (hwsToNamespaces != null)) {
                List nsHeadwords = new LinkedList();
                String hw = null;
                Iterator it = null;
                for (it = hwsToNamespaces.keySet().iterator(); it.hasNext();) {
                    hw = (String) it.next();
                    if (isNonEmptyString(hw)) {
                        hw = hw.trim();
                        while (hw.endsWith("_")) {
                            hw = hw.substring(0, hw.length() - 1);
                        }
                        if (!nsHeadwords.contains(hw)) {
                            nsHeadwords.add(hw);
                        }
                    }
                }
                sortByTermLength(nsHeadwords);
                List columns = null;
                String cell = null;
                String key = null;
                String repl = null;
                Iterator it2 = null;
                int ri = 0;
                for (it = rows.iterator(); it.hasNext(); ri++) {
                    columns = (List) it.next();
                    if (columns != null) {
                        int clen = columns.size();
                        for (int cidx = 0; cidx < clen; cidx++) {
                            cell = (String) columns.get(cidx);
                            if (isNonEmptyString(cell)) {
                                if (// (getColumnIndex("AU") == cidx)
                                    // || 
                                    (getColumnIndex("BE") == cidx)
                                    || (getColumnIndex("BF") == cidx)) {
                                    cell = StringUtil.removeEnclosingQuotes(cell);
                                    cell = StringUtil.replaceNonAsciiChars(cell);
                                }
                                else if (getColumnIndex("AU") != cidx) {
                                    cell = StringUtil.normalizeSpaceChars(cell);
                                    if (!StringUtil.isUri(cell)
                                        && (cell.matches(".+\\s.+\\s.+\\s.+")
                                            || cell.contains("&%"))) {

                                        // cell = StringUtil.replaceNonAsciiChars(cell);
                                        cell = normalizeTermsInString(cell,
                                                                      nsHeadwords,
                                                                      "");
                                    }
                                }
                                columns.set(cidx, cell);
                            }
                        }
                    }
                }
            }
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }

        System.out.println("EXIT DocGen.normalizeTermsInComplexFields(["
                           + "[" + rows.size() + " rows], " 
                           + "[" + hwsToNamespaces.size() + " map entries], "
                           + defaultNamespace + ")");
        System.out.println("  > "
                           + ((System.currentTimeMillis() - t1) / 1000.0)
                           + " seconds elapsed time");

        return;
    }

    /** *******************************************************************
     *
     */
    private ArrayList<String> processNamespace(String localNamespace, String defaultNamespace) {
        ArrayList<String> statements = new ArrayList<String>();
        try {
            if (isNonEmptyString(localNamespace)) {
                localNamespace = normalizeNamespace(localNamespace);
                String token = getNameRelationToken(localNamespace);
                if (isNonEmptyString(token) && !token.matches(".*(?i)xsdname.*")) {
                    String baseNamespace = getRelationTokenNamespace(localNamespace, 
                                                                     token,
                                                                     defaultNamespace);
                    baseNamespace = normalizeNamespace(baseNamespace);
                    statements.add(makeStatement("subentity",
                                                 localNamespace,
                                                 baseNamespace));
                    statements.add(makeStatement("headword",
                                                 baseNamespace,
                                                 localNamespace,
                                                 StringUtil.quote(getHeadword(localNamespace))));
                    if (isNonEmptyString(defaultNamespace)) {
                        defaultNamespace = normalizeNamespace(defaultNamespace);
                        if (!baseNamespace.equals(defaultNamespace)) {
                            statements.add(makeStatement("subentity",
                                                         baseNamespace,
                                                         defaultNamespace));
                            statements.add(makeStatement("headword",
                                                         defaultNamespace,
                                                         baseNamespace,
                                                         StringUtil.quote(getHeadword(baseNamespace))));
                        }
                    }
                }
            }
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
        return statements;
    }

    /** *******************************************************************
     *
     */
    protected static String makeStatement(List<String> tuple) {
        String stmt = "";
        try {
            StringBuilder sb = new StringBuilder();
            sb.append("(");
            int i = 0;
            for (String term : tuple) {
                if (StringUtil.emptyString(term)) {
                    throw new Exception("Error in DocGen.makeStatement(" 
                                        + tuple 
                                        + "): Malformed tuple");
                }
                if (i > 0) sb.append(" ");
                if ((StringUtil.isUri(term)
                     || (term.matches(".*\\S+\\s+\\S+.*")
                         && !Formula.listP(term)))
                    && !StringUtil.isQuotedString(term)) {
                    term = StringUtil.quote(term);
                }
                if (!StringUtil.isQuotedString(term) 
                    && !Formula.listP(term)
                    && !Formula.isVariable(term)
                    && !StringUtil.isDigitString(term)) {
                    term = StringUtil.w3cToKif(term);
                }
                sb.append(term);
                i++;
            }
            sb.append(")");
            stmt = sb.toString();
        }
        catch (Exception ex) {
            System.out.println(ex.getMessage());
            ex.printStackTrace();
        }
        return stmt;
    }

    /** *******************************************************************
     *
     */
    protected static String makeStatement(String predicate, String arg1, String arg2) {
        String stmt = "";
        try {
            List<String> tuple = Arrays.asList(predicate, arg1, arg2);
            stmt = makeStatement(tuple);
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
        return stmt;
    }

    /** *******************************************************************
     *
     */
    protected static String makeStatement(String predicate, 
                                          String arg1, 
                                          String arg2, 
                                          String arg3) {
        String stmt = "";
        try {
            List<String> tuple = Arrays.asList(predicate, arg1, arg2, arg3);
            stmt = makeStatement(tuple);
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
        return stmt;
    }

    /** *******************************************************************
     *
     */
    protected static String makeStatement(String predicate, 
                                          String arg1, 
                                          String arg2, 
                                          String arg3,
                                          String arg4) {
        String stmt = "";
        try {
            List<String> tuple = Arrays.asList(predicate, arg1, arg2, arg3, arg4);
            stmt = makeStatement(tuple);
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
        return stmt;
    }

    /** *******************************************************************
     * This method forces reloadiing of kb so not to leave kb's data
     * structures in an inconsistent state.
     * 
     */
    public File processDataRows(KB kb,
                                List rows, 
                                String ontology, 
                                String baseFileName,
                                File kifOutputFile,
                                boolean overwrite,
                                boolean loadConstituent) {

        long t1 = System.currentTimeMillis();

        System.out.println("ENTER DocGen.processDataRows("
                           + kb.name + ", "
                           + "[" + rows.size() + " rows], "
                           + ontology + ", "
                           + baseFileName + ", "
                           + kifOutputFile + ", "
                           + overwrite + ", "
                           + loadConstituent + ")");

        File newConstituentFile = null;
        String kifCanonicalPath = null;
        PrintWriter pw = null;

        try {
            synchronized (kb) {
                kifCanonicalPath = kifOutputFile.getCanonicalPath();
                String nsDefault = normalizeNamespace(getDefaultNamespace());

                if (tableColumnNames.isEmpty()) {
                    buildTableColumnNames(kb, ontology);
                }

                List normalized = normalizeTerms(rows, nsDefault);
                if (namespacesToHeadwords != null) { namespacesToHeadwords.clear(); }
                if (headwordsToNamespaces != null) { headwordsToNamespaces.clear(); }
                namespacesToHeadwords = (Map) normalized.get(0);
                headwordsToNamespaces = (Map) normalized.get(1);
                normalizeTermsInComplexFields(rows, headwordsToNamespaces, nsDefault);
                headwords.putAll(namespacesToHeadwords);

                HashSet<String> relatorSet = new HashSet<String>();

                List namespaceArray = new ArrayList();
                Set statements = new LinkedHashSet();
                Set rules = new LinkedHashSet();

                String uTerm = "";
                int nRowsSkipped = 0;
                int totalRows = rows.size();

                int rowIdx = 0;
                for (Iterator itr = rows.iterator(); itr.hasNext(); rowIdx++) {
    
                    List row = (List) itr.next();

                    if ((row != null) && !row.isEmpty()) {

                        // column A - Skip rows in which the first token
                        // is $.
                        String tokenA = getCellValue(row, "A");
                        if (!tokenA.equalsIgnoreCase("u")) {
                            nRowsSkipped++;
                        }
                        else {

                            // column L - Indicates if the triple V, X, Y in
                            // the current row is universally valid, locally
                            // valid, defines an AVS member, defines a
                            // bootstrapping term, or is not a triple at all.
                            String tokenL           = getCellValue(row, "L");
                            boolean isBootstrapTerm = tokenL.equals("bu");
                            boolean isBootstrapRelation = tokenL.equals("bur");
                            boolean isLocalRef     = tokenL.equals("rrcs");
                            boolean isValidTriple  = (isNonEmptyString(tokenL) 
                                                      && !tokenL.equals("nt"));
                            boolean isAvsMember    = tokenL.equals("avs");
                            boolean isNewUniversal = tokenL.equals("u");

                            // column M
                            String subject         = getCellValue(row, "M");
                            boolean isValidSubject = isNonEmptyString(subject);
                            String namespace = (isValidSubject
                                                ? getNamespace(subject)
                                                : "");

                            if (isValidSubject) {
                                int pidx = subject.indexOf(getW3cNamespaceDelimiter());
                                String pref = "";
                                String base = "";
                                if ((pidx > -1) && (pidx < subject.length())) {
                                    pref = subject.substring(0, pidx);
                                    base = subject.substring(pidx + 1);
                                }
                            }

                            if (isNewUniversal && isValidSubject) uTerm = subject;

                            // column N - SAuth - Scheme Authority
                            String schemeAuthority  = getCellValue(row, "N");

                            // column O
                            String definition       = getCellValue(row, "O");

                            // column P
                            String meaningType      = getCellValue(row, "P");

                            // column Q
                            String primitiveType    = getCellValue(row, "Q");

                            // column R
                            String namesText        = getCellValue(row, "R");

                            // System.out.println("  row == " + rowIdx + ", namesText == " + namesText);

                            // column S
                            String nestingLevel     = getCellValue(row, "S");

                            // column T
                            String seqOrdinal       = getCellValue(row, "T");

                            // column U
                            String tripleID         = getCellValue(row, "U");

                            // column V
                            String domain           = getCellValue(row, "V");

                            // column X
                            String relator          = getCellValue(row, "X");

                            // column Y
                            String range            = getCellValue(row, "Y");

                            // column Z
                            String rangeClass       = getCellValue(row, "Z");

                            // column AA
                            String rangeDataType    = getCellValue(row, "AA");

                            // column AB
                            String label            = getCellValue(row, "AB");

                            // column AC
                            String rangeRegex       = getCellValue(row, "AC");

                            // column AD
                            String cardinality      = getCellValue(row, "AD");

                            // column AI
                            String parentSet        = getCellValue(row, "AI");

                            // column AJ
                            String childSet         = getCellValue(row, "AJ");

                            // column AK
                            String mappingStatus    = getCellValue(row, "AK");

                            // column AL
                            String mappingAuthority = getCellValue(row, "AL");

                            // column AM
                            String mappingRelator   = getCellValue(row, "AM");

                            // column AN
                            String mappingRange     = getCellValue(row, "AN");

                            // column A0
                            String nativeComment    = getCellValue(row, "AO");

                            // column AQ
                            String sigmaDefinition  = getCellValue(row, "AQ");

                            // column AR
                            String sigmaComment     = getCellValue(row, "AR");

                            // column AS
                            String sumoMapping      = getCellValue(row, "AS");

                            // column AT
                            String sparqlExprs      = getCellValue(row, "AT");

                            // column AV
                            String coaDefinition    = getCellValue(row, "AV");

                            // column AW
                            String coaComment       = getCellValue(row, "AW");

                            // column BB
                            String displayName      = getCellValue(row, "BB");

                            // column BE
                            String owlProperty      = getCellValue(row, "BE");

                            // column BF
                            String ttlExpr          = getCellValue(row, "BF");

                            if (isValidSubject) {
                                if (isNonEmptyString(namespace)) {
                                    if (!namespaceArray.contains(namespace)) {
                                        List pnsp = processNamespace(namespace, nsDefault);
                                        String nspf = null;
                                        for (Iterator it = pnsp.iterator(); it.hasNext();) {
                                            nspf = (String) it.next();
                                            statements.add(nspf);
                                        }
                                        namespaceArray.add(namespace);
                                    }
                                    statements.add(makeStatement("inNamespace", 
                                                                 subject, 
                                                                 namespace));
                                }
                                if (isNonEmptyString(sumoMapping)) {
                                    sumoMappings.put(subject, sumoMapping);
                                }
                                if (isNonEmptyString(namesText)) {
                                    makeNameStatements(rowIdx, 
                                                       statements, 
                                                       nsDefault, 
                                                       subject, 
                                                       namesText);
                                }
                                if (isNonEmptyString(displayName)) {
                                    displayName = (StringUtil.isQuotedString(displayName)
                                                   ? displayName
                                                   : StringUtil.quote(displayName));
                                    statements.add(makeStatement("displayName",
                                                                 nsDefault,
                                                                 subject,
                                                                 displayName));
                                }

                                List preds = Arrays.asList("documentation", "comment");
                                List values = Arrays.asList(sigmaDefinition, sigmaComment);
                                int plen = preds.size();
                                for (int i = 0; i < plen; i++) {
                                    String pred = (String) preds.get(i);
                                    String value = (String) values.get(i);
                                    if (isNonEmptyString(value)) {
                                        String arg1 = subject;
                                        if (isLocalRef && isNonEmptyString(range))
                                            arg1 = subject;
                                        String arg2 = nsDefault;
                                        if (isLocalRef && isNonEmptyString(uTerm))
                                            arg2 = uTerm;
                                        String arg3 = 
                                            StringUtil.replaceRepeatedDoubleQuotes(value);
                                        statements.add(StringUtil.wordWrap(makeStatement(pred,
                                                                                         arg1,
                                                                                         arg2,
                                                                                         StringUtil.quote(arg3)),
                                                                           70));
                                    }
                                }

                                /*
                                  makeMappingStatements(rowIdx, 
                                  statements, 
                                  subject, 
                                  mappingAuthority,
                                  mappingRelator,
                                  mappingRange);
                                */
                            }

                            if (isValidTriple
                                && isNonEmptyString(domain)
                                && isNonEmptyString(relator)
                                && isNonEmptyString(range)) {
                                relatorSet.add(relator);
                                statements.add(makeStatement("instance", relator, "Predicate"));
                                if (StringUtil.isUri(range) 
                                    || StringUtil.isStringWithSpaces(range)) {
                                    range = StringUtil.quote(range);
                                }
                                String kifTriple = makeStatement(relator, domain, range);
                                statements.add(kifTriple);
                                if (StringUtil.isLocalTermReference(range)) {
                                    if (isNonEmptyString(rangeClass)) {
                                        statements.add(makeStatement("instance", 
                                                                     range, 
                                                                     rangeClass));
                                        if (isNonEmptyString(rangeClass))
                                            statements.add(makeStatement("datatype", 
                                                                         range, 
                                                                         rangeDataType));
                                        if (isNonEmptyString(cardinality)) {
                                            makeRangeCardinalityStatements(kb,
                                                                           rowIdx,
                                                                           statements,
                                                                           range,
                                                                           cardinality);
                                        }
                                        if (isNonEmptyString(label)) {
                                            statements.add(makeStatement("termFormat",
                                                                         "XMLLabel",
                                                                         range,
                                                                         StringUtil.quote(label)));
                                        }
                                    }
                                }

                                if (isNonEmptyString(seqOrdinal)) {
                                    statements.add(makeStatement("arg1SortOrdinal",
                                                                 relator,
                                                                 domain,
                                                                 range,
                                                                 seqOrdinal));
                                }

                                if (isNonEmptyString(rangeRegex)) {
                                    rangeRegex = StringUtil.escapeEscapeChars(rangeRegex);
                                    statements.add(makeStatement("contentRegexPattern",
                                                                 range,
                                                                 StringUtil.quote(rangeRegex)));
                                }
                            }
                    
                            makeSparqlStatements(kb, 
                                                 rowIdx, 
                                                 rules, 
                                                 subject, 
                                                 sparqlExprs, 
                                                 sigmaDefinition);
                            // makeKifRules(kb, rowIdx, rules, subject, kifFormulae, sigmaDefinition);
                            // System.out.println("  basesToQWords == " + basesToQWords);
                            // System.out.println("  rules == " + rules);

                        }
                    }
                }  // for loop through all rows

                System.out.println("  > " + nRowsSkipped + " rows skipped");

                for (String r : relatorSet) {
                    if (isNonEmptyString(r)) {
                        statements.add(makeStatement("format",
                                                     "EnglishLanguage",
                                                     StringUtil.w3cToKif(r),
                                                     StringUtil.quote("%1 &%" 
                                                                      + StringUtil.w3cToKif(r) 
                                                                      + " %2")));
                    }
                }

                if (!rules.isEmpty()) {
                    Formula f = new Formula();
                    String rstr = null;
                    for (Iterator itr = rules.iterator(); itr.hasNext();) {
                        rstr = (String) itr.next();
                        if (rstr.matches(".*(?i)sparqlmanifest.*") && rstr.contains("{")) {
                            f.read(rstr);
                            String arg0 = f.getArgument(0);
                            String arg1 = f.getArgument(1);
                            String arg2 = f.getArgument(2);
                            arg2 = arg2.replaceAll("\\.", " .");
                            arg2 = arg2.replaceAll("\\s+", " ");
                            arg2 = addTermNamespacePrefixes(arg2);
                            rstr = makeStatement(arg0, arg1, StringUtil.quote(arg2));
                        }
                        statements.add(rstr);
                    }
                }

                addStatementsToKB(kb, statements, kifCanonicalPath);
                kb.buildRelationCaches();
                computeDerivations(kb);
                kb.buildRelationCaches();
                DB.printSuoKifStatements(kb, kifCanonicalPath);

                String errStr = "";
                File kbFileDir = new File(KBmanager.getMgr().getPref("kbDir"));
                File constituentFile = new File(kbFileDir, (baseFileName + ".kif"));
                String cfCanonicalPath = constituentFile.getCanonicalPath();
                if (overwrite && !cfCanonicalPath.equalsIgnoreCase(kifCanonicalPath)) {

                    // Maybe rename the constituent file.
                    boolean overwriteSucceeded = false;
                    try {
                        if ((!constituentFile.exists() || constituentFile.delete())
                            && kifOutputFile.renameTo(constituentFile)) {
                            overwriteSucceeded = constituentFile.canRead();
                        }
                    }
                    catch (Exception owex) {
                        owex.printStackTrace();
                    }
                    if (!overwriteSucceeded)
                        errStr = ("Error: Could not overwrite " + cfCanonicalPath);
                }
                if (StringUtil.emptyString(errStr)) {
                    if (!overwrite) {
                        constituentFile = kifOutputFile;
                        cfCanonicalPath = constituentFile.getCanonicalPath();
                    }

                    // We reload the KB no matter what, to restore the
                    // integrity of its data structures.  If
                    // loadConstituent is true, the new constituent
                    // will be included in the reload.
                    for (ListIterator lit = kb.constituents.listIterator();
                         lit.hasNext();) {
                        String cnst = (String) lit.next();
                        if (cnst.contains(baseFileName)) {
                            lit.remove();
                            System.out.println("INFO in DocGen.processDataRows("
                                               + kb.name + ", "
                                               + "[" + rows.size() + " rows], "
                                               + ontology + ", "
                                               + baseFileName + ", "
                                               + kifOutputFile + ", "
                                               + overwrite + ", "
                                               + loadConstituent + ")");
                            System.out.println("  > removed " + cnst);
                            System.out.println("  > kb.constituents == " + kb.constituents);
                        }
                    }
                    if (loadConstituent) {
                        System.out.println("  > adding constituent " + cfCanonicalPath);
                        kb.constituents.add(cfCanonicalPath);
                        System.out.println("  > kb.constituents == " + kb.constituents);
                    }
                    KBmanager.getMgr().writeConfiguration();
                    kb.reload();
                    newConstituentFile = constituentFile;
                }
            } // end synchronized (kb)
        }
        catch (Exception ex) {
            System.out.println("ERROR in DocGen.processDataRows("
                               + kb.name + ", "
                               + "[" + rows.size() + " rows], "
                               + ontology + ", "
                               + baseFileName + ", "
                               + kifOutputFile + ", "
                               + overwrite + ", "
                               + loadConstituent + ")");
            System.out.println("  > Error writing file " + kifCanonicalPath);
            ex.printStackTrace();
        }
        finally {
            try {
                if (pw != null) {
                    pw.close();
                }
            }
            catch (Exception ioe) {
                ioe.printStackTrace();
            }
        }
        System.out.println("EXIT DocGen.processDataRows("
                           + kb.name + ", "
                           + "[" + rows.size() + " rows], "
                           + ontology + ", "
                           + baseFileName + ", "
                           + kifOutputFile + ", "
                           + overwrite + ", "
                           + loadConstituent + ")");
        System.out.println("  > "
                           + ((System.currentTimeMillis() - t1) / 1000.0)
                           + " seconds elapsed time");
        return newConstituentFile;
    }

    /** *******************************************************************
     * NB: This method alters the List rows by normalizing all term
     * names.
     */
    public void processSpreadsheet(KB kb,
                                   List rows, 
                                   String ontology, 
                                   String kifOutputFilePathname) {

        long t1 = System.currentTimeMillis();

        System.out.println("ENTER DocGen.processSpreadsheet("
                           + kb.name + ", "
                           + "[" + rows.size() + " rows], "
                           + ontology + ", "
                           + kifOutputFilePathname + ")");

        PrintWriter pw = null;

        try {
            synchronized (kb) {

                String nsDefault = normalizeNamespace(getDefaultNamespace());

                if (tableColumnNames.isEmpty()) {
                    buildTableColumnNames(kb, ontology);
                }

                List normalized = normalizeTerms(rows, nsDefault);
                if (namespacesToHeadwords != null) { namespacesToHeadwords.clear(); }
                if (headwordsToNamespaces != null) { headwordsToNamespaces.clear(); }
                namespacesToHeadwords = (Map) normalized.get(0);
                headwordsToNamespaces = (Map) normalized.get(1);
                normalizeTermsInComplexFields(rows, headwordsToNamespaces, nsDefault);
                headwords.putAll(namespacesToHeadwords);

                HashSet<String> relatorSet = new HashSet<String>();

                List namespaceArray = new ArrayList();
                Set statements = new LinkedHashSet();
                Set rules = new LinkedHashSet();

                String uTerm = "";
                int nRowsSkipped = 0;
                int totalRows = rows.size();

                int rowIdx = 0;
                for (Iterator itr = rows.iterator(); itr.hasNext(); rowIdx++) {
    
                    List row = (List) itr.next();

                    if ((row != null) && !row.isEmpty()) {

                        // column A - Skip rows in which the first token
                        // is $.
                        String tokenA = getCellValue(row, "A");
                        if (!tokenA.equalsIgnoreCase("u")) {
                            nRowsSkipped++;
                        }
                        else {

                            // column L - Indicates if the triple V, X, Y in
                            // the current row is universally valid, locally
                            // valid, defines an AVS member, defines a
                            // bootstrapping term, or is not a triple at all.
                            String tokenL           = getCellValue(row, "L");
                            boolean isBootstrapTerm = tokenL.equals("bu");
                            boolean isBootstrapRelation = tokenL.equals("bur");
                            boolean isLocalRef     = tokenL.equals("rrcs");
                            boolean isValidTriple  = (isNonEmptyString(tokenL) 
                                                      && !tokenL.equals("nt"));
                            boolean isAvsMember    = tokenL.equals("avs");
                            boolean isNewUniversal = tokenL.equals("u");

                            // column M
                            String subject         = getCellValue(row, "M");
                            boolean isValidSubject = isNonEmptyString(subject);
                            String namespace = (isValidSubject
                                                ? getNamespace(subject)
                                                : "");

                            if (isValidSubject) {
                                int pidx = subject.indexOf(getW3cNamespaceDelimiter());
                                String pref = "";
                                String base = "";
                                if ((pidx > -1) && (pidx < subject.length())) {
                                    pref = subject.substring(0, pidx);
                                    base = subject.substring(pidx + 1);
                                }
                            }

                            if (isNewUniversal && isValidSubject) uTerm = subject;

                            // column N - SAuth - Scheme Authority
                            String schemeAuthority  = getCellValue(row, "N");

                            // column O
                            String definition       = getCellValue(row, "O");

                            // column P
                            String meaningType      = getCellValue(row, "P");

                            // column Q
                            String primitiveType    = getCellValue(row, "Q");

                            // column R
                            String namesText        = getCellValue(row, "R");

                            // System.out.println("  row == " + rowIdx + ", namesText == " + namesText);

                            // column S
                            String nestingLevel     = getCellValue(row, "S");

                            // column T
                            String seqOrdinal       = getCellValue(row, "T");

                            // column U
                            String tripleID         = getCellValue(row, "U");

                            // column V
                            String domain           = getCellValue(row, "V");

                            // column X
                            String relator          = getCellValue(row, "X");

                            // column Y
                            String range            = getCellValue(row, "Y");

                            // column Z
                            String rangeClass       = getCellValue(row, "Z");

                            // column AA
                            String rangeDataType    = getCellValue(row, "AA");

                            // column AB
                            String label            = getCellValue(row, "AB");

                            // column AC
                            String rangeRegex       = getCellValue(row, "AC");

                            // column AD
                            String cardinality      = getCellValue(row, "AD");

                            // column AI
                            String parentSet        = getCellValue(row, "AI");

                            // column AJ
                            String childSet         = getCellValue(row, "AJ");

                            // column AK
                            String mappingStatus    = getCellValue(row, "AK");

                            // column AL
                            String mappingAuthority = getCellValue(row, "AL");

                            // column AM
                            String mappingRelator   = getCellValue(row, "AM");

                            // column AN
                            String mappingRange     = getCellValue(row, "AN");

                            // column A0
                            String nativeComment    = getCellValue(row, "AO");

                            // column AQ
                            String sigmaDefinition  = getCellValue(row, "AQ");

                            // column AR
                            String sigmaComment     = getCellValue(row, "AR");

                            // column AS
                            String sumoMapping      = getCellValue(row, "AS");

                            // column AT
                            String sparqlExprs      = getCellValue(row, "AT");

                            // column AV
                            String coaDefinition    = getCellValue(row, "AV");

                            // column AW
                            String coaComment       = getCellValue(row, "AW");

                            // column BB
                            String displayName      = getCellValue(row, "BB");

                            // column BE
                            String owlProperty      = getCellValue(row, "BE");

                            // column BF
                            String ttlExpr          = getCellValue(row, "BF");

                            if (isValidSubject) {
                                if (isNonEmptyString(namespace)) {
                                    if (!namespaceArray.contains(namespace)) {
                                        List pnsp = processNamespace(namespace, nsDefault);
                                        String nspf = null;
                                        for (Iterator it = pnsp.iterator(); it.hasNext();) {
                                            nspf = (String) it.next();
                                            statements.add(nspf);
                                        }
                                        namespaceArray.add(namespace);
                                    }
                                    statements.add(makeStatement("inNamespace", 
                                                                 subject, 
                                                                 namespace));
                                }
                                if (isNonEmptyString(sumoMapping)) {
                                    sumoMappings.put(subject, sumoMapping);
                                }
                                if (isNonEmptyString(namesText)) {
                                    makeNameStatements(rowIdx, 
                                                       statements, 
                                                       nsDefault, 
                                                       subject, 
                                                       namesText);
                                }
                                if (isNonEmptyString(displayName)) {
                                    displayName = (StringUtil.isQuotedString(displayName)
                                                   ? displayName
                                                   : StringUtil.quote(displayName));
                                    statements.add(makeStatement("displayName",
                                                                 nsDefault,
                                                                 subject,
                                                                 displayName));
                                }

                                List preds = Arrays.asList("documentation", "comment");
                                List values = Arrays.asList(sigmaDefinition, sigmaComment);
                                int plen = preds.size();
                                for (int i = 0; i < plen; i++) {
                                    String pred = (String) preds.get(i);
                                    String value = (String) values.get(i);
                                    if (isNonEmptyString(value)) {
                                        String arg1 = subject;
                                        if (isLocalRef && isNonEmptyString(range))
                                            arg1 = subject;
                                        String arg2 = nsDefault;
                                        if (isLocalRef && isNonEmptyString(uTerm))
                                            arg2 = uTerm;
                                        String arg3 = 
                                            StringUtil.replaceRepeatedDoubleQuotes(value);
                                        statements.add(StringUtil.wordWrap(makeStatement(pred,
                                                                                         arg1,
                                                                                         arg2,
                                                                                         StringUtil.quote(arg3)),
                                                                           70));
                                    }
                                }

                                /*
                                  makeMappingStatements(rowIdx, 
                                  statements, 
                                  subject, 
                                  mappingAuthority,
                                  mappingRelator,
                                  mappingRange);
                                */
                            }

                            if (isValidTriple
                                && isNonEmptyString(domain)
                                && isNonEmptyString(relator)
                                && isNonEmptyString(range)) {
                                relatorSet.add(relator);
                                statements.add(makeStatement("instance", relator, "Predicate"));
                                if (StringUtil.isUri(range) 
                                    || StringUtil.isStringWithSpaces(range)) {
                                    range = StringUtil.quote(range);
                                }
                                String kifTriple = makeStatement(relator, domain, range);
                                statements.add(kifTriple);
                                if (StringUtil.isLocalTermReference(range)) {
                                    if (isNonEmptyString(rangeClass)) {
                                        statements.add(makeStatement("instance", 
                                                                     range, 
                                                                     rangeClass));
                                        if (isNonEmptyString(rangeClass))
                                            statements.add(makeStatement("datatype", 
                                                                         range, 
                                                                         rangeDataType));
                                        if (isNonEmptyString(cardinality)) {
                                            makeRangeCardinalityStatements(kb,
                                                                           rowIdx,
                                                                           statements,
                                                                           range,
                                                                           cardinality);
                                        }
                                        if (isNonEmptyString(label)) {
                                            statements.add(makeStatement("termFormat",
                                                                         "XMLLabel",
                                                                         range,
                                                                         StringUtil.quote(label)));
                                        }
                                    }
                                }

                                if (isNonEmptyString(seqOrdinal)) {
                                    statements.add(makeStatement("arg1SortOrdinal",
                                                                 relator,
                                                                 domain,
                                                                 range,
                                                                 seqOrdinal));
                                }

                                if (isNonEmptyString(rangeRegex)) {
                                    rangeRegex = StringUtil.escapeEscapeChars(rangeRegex);
                                    statements.add(makeStatement("contentRegexPattern",
                                                                 range,
                                                                 StringUtil.quote(rangeRegex)));
                                }
                            }
                    
                            makeSparqlStatements(kb, 
                                                 rowIdx, 
                                                 rules, 
                                                 subject, 
                                                 sparqlExprs, 
                                                 sigmaDefinition);
                            // makeKifRules(kb, rowIdx, rules, subject, kifFormulae, sigmaDefinition);
                            // System.out.println("  basesToQWords == " + basesToQWords);
                            // System.out.println("  rules == " + rules);

                        }
                    }
                }  // for loop through all rows

                System.out.println("  > " + nRowsSkipped + " rows skipped");

                for (String r : relatorSet) {
                    if (isNonEmptyString(r)) {
                        statements.add(makeStatement("format",
                                                     "EnglishLanguage",
                                                     StringUtil.w3cToKif(r),
                                                     StringUtil.quote("%1 &%" 
                                                                      + StringUtil.w3cToKif(r) 
                                                                      + " %2")));
                    }
                }

                if (!rules.isEmpty()) {
                    Formula f = new Formula();
                    String rstr = null;
                    for (Iterator itr = rules.iterator(); itr.hasNext();) {
                        rstr = (String) itr.next();
                        if (rstr.matches(".*(?i)sparqlmanifest.*") && rstr.contains("{")) {
                            f.read(rstr);
                            String arg0 = f.getArgument(0);
                            String arg1 = f.getArgument(1);
                            String arg2 = f.getArgument(2);
                            arg2 = arg2.replaceAll("\\.", " .");
                            arg2 = arg2.replaceAll("\\s+", " ");
                            arg2 = addTermNamespacePrefixes(arg2);
                            rstr = makeStatement(arg0, arg1, StringUtil.quote(arg2));
                        }
                        statements.add(rstr);
                    }
                }

                addStatementsToKB(kb, statements, kifOutputFilePathname);
                kb.buildRelationCaches();
                computeDerivations(kb);
                kb.buildRelationCaches();
                DB.printSuoKifStatements(kb, kifOutputFilePathname);

                // kb.addConstituent(kifOutputFilePathname);

            } // end synchronized (kb)
        }
        catch (Exception ex) {
            System.out.println("Error writing file " + kifOutputFilePathname);
            ex.printStackTrace();
        }
        finally {
            try {
                if (pw != null) {
                    pw.close();
                }
            }
            catch (Exception ioe) {
                ioe.printStackTrace();
            }
        }

        System.out.println("EXIT DocGen.processSpreadsheet("
                           + kb.name + ", "
                           + "[" + rows.size() + " rows], "
                           + ontology + ", "
                           + kifOutputFilePathname + ")");
        System.out.println("  > "
                           + ((System.currentTimeMillis() - t1) / 1000.0)
                           + " seconds elapsed time");
        return;
    }

    /** *******************************************************************
     * 
     */
    private void computeDerivations(KB kb) {

        long t1 = System.currentTimeMillis();
        System.out.println("ENTER DocGen.computeDerivations(" + kb.name + ")");

        try {
            Map<String, Object> memo = new HashMap<String, Object>();

            // Compute and add inverse statements.
            System.out.print("  > computing inverse statements ");
            memo.clear();
            Set<Formula> newFormulae = new LinkedHashSet<Formula>();
            Map.Entry<String, Formula> entry = null;
            Formula f = null;
            Iterator it = null;
            StringBuilder sb = null;
            int count = 0;
            for (it = kb.formulaMap.entrySet().iterator(); it.hasNext();) {
                entry = (Map.Entry<String, Formula>) it.next();
                f = (Formula) entry.getValue();
                newFormulae.add(f);
                it.remove();
                if (f.listP()
                    && !f.getSourceFile().endsWith(KB._cacheFileSuffix)) {
                    String arg0 = f.car();
                    if (StringUtil.isNonEmptyString(arg0)
                        && !Formula.isLogicalOperator(arg0)
                        && (f.listLength() == 3)) {
                        String inv = (String) memo.get(arg0);
                        if (inv == null) {
                            inv = getInverse(kb, arg0);
                            if (inv == null) inv = "no_inverse";
                            memo.put(arg0, inv);
                        }
                        if (!inv.equals("no_inverse")) {
                            String arg1 = f.getArgument(1);
                            String arg2 = f.getArgument(2);
                            if (!StringUtil.isStringWithSpaces(arg2)) {
                                sb = new StringBuilder();
                                sb.append("(");
                                sb.append(inv);
                                sb.append(" ");
                                sb.append(arg2);
                                sb.append(" ");
                                sb.append(arg1);
                                sb.append(")");
                                Formula derivedF = new Formula();
                                derivedF.read(sb.toString());
                                derivedF.setSourceFile(f.getSourceFile());
                                derivedF.setIsComputed(true);
                                newFormulae.add(derivedF);
                                if ((count++ % 100) == 1) System.out.print(".");
                                sb = new StringBuilder();
                                sb.append("(computedFormula ");
                                sb.append(kb.name);
                                sb.append(" ");
                                sb.append(f.toString());
                                sb.append(" ");
                                sb.append(derivedF.toString());
                                sb.append(")");
                                Formula metaF = new Formula();
                                metaF.read(sb.toString());
                                metaF.setSourceFile(f.getSourceFile());
                                metaF.setIsComputed(true);
                                newFormulae.add(metaF);
                                if ((count++ % 100) == 1) System.out.print(".");
                            }
                        }
                    }
                }
            }
            System.out.println("x");
            System.out.println("  > " + count + " statements computed");

            if (!newFormulae.isEmpty()) {
                List<Formula> formulaList = new LinkedList<Formula>(newFormulae);
                newFormulae.clear();
                kb.formulas.clear();
                kb.formulaMap.clear();
                kb.terms.clear();
                kb.clearFormatMaps();
                addFormulaeToKB(kb, formulaList);
            }
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }

        System.out.println("EXIT DocGen.computeDerivations(" + kb.name + ")");
        System.out.println("  > "
                           + ((System.currentTimeMillis() - t1) / 1000.0)
                           + " seconds elapsed time");

        return;
    }

    /** *******************************************************************
     *
     * 
     */
    private int addStatementsToKB(KB kb,
                                  Set<String> statements,
                                  String constituentCanonicalPath) {
        long t1 = System.currentTimeMillis();
        System.out.println("ENTER DocGen.addStatementsToKB("
                           + kb.name + ", "
                           + "[set of " + statements.size() + " statements], "
                           + constituentCanonicalPath + ")");
        int duplicateCount = 0;
        int numberAdded = 0;
        Set errors = new LinkedHashSet();
        KIF kif = null;
        try {
            if ((statements != null) && !statements.isEmpty()) {
                StringBuilder sb = new StringBuilder();
                for (Iterator it = statements.iterator(); it.hasNext();) {
                    sb.append((String) it.next());
                    sb.append(kb.getLineSeparator());
                    // sb.append(kb.getLineSeparator());
                }
                kif = new KIF();
                kif.setFilename(constituentCanonicalPath);
                StringReader sr = new StringReader(sb.toString());
                errors.addAll(kif.parse(sr));
                sr.close();

                System.out.print("  > adding KIF.formulaSet to KB.formulaMap ");
                String key = null;
                int count = 0;
                Formula f = null;
                for (String fstr : kif.formulaSet) {
                    key = fstr.intern();
                    f = (Formula) kb.formulaMap.get(key);
                    if (f == null) {
                        f = new Formula();
                        f.read(key);
                        f.setSourceFile(constituentCanonicalPath);
                        kb.formulaMap.put(key, f);
                        numberAdded++;
                    }
                    else {
                        duplicateCount++;
                        /*
                          sb = new StringBuilder();
                          sb.append("The KB ");
                          sb.append(kb.name);
                          sb.append(" already contains the formula ");
                          sb.append(kb.getLineSeparator());
                          sb.append(f.toString());
                          sb.append(kb.getLineSeparator());
                          sb.append(" loaded from "); 
                          sb.append(f.getSourceFile());
                          errors.add(sb.toString());
                        */
                    }
                    if ((count++ % 100) == 1) System.out.print(".");
                }
                System.out.println("x");

                System.out.print("  > adding KIF.formulas to KB.formulas ");
                count = 0;
                for (Iterator itk = kif.formulas.keySet().iterator(); itk.hasNext(); count++) {
                    // Iterate through the KIF Formulae created from
                    // the SCOW spreadsheet file, adding them to the
                    // KB at the appropriate key.
                    key = (String) itk.next();         
                    ArrayList kifVal = (ArrayList) kif.formulas.get(key);
                    ArrayList kbVal = (ArrayList) kb.formulas.get(key);
                    if ((kifVal != null) && !kifVal.isEmpty()) {
                        if (kbVal == null) {
                            kbVal = new ArrayList();
                            kb.formulas.put(key, kbVal);
                        }
                        for (Iterator itv = kifVal.iterator(); itv.hasNext();) {
                            f = (Formula) itv.next();
                            if (kbVal.contains(f)) {
                                /*
                                  sb = new StringBuilder();
                                  sb.append("The KB ");
                                  sb.append(kb.name);
                                  sb.append(" already contains the formula ");
                                  sb.append(kb.getLineSeparator());
                                  sb.append(f.toString());
                                  sb.append(kb.getLineSeparator());
                                  sb.append("loaded from "); 
                                  sb.append(f.getSourceFile());
                                  errors.add(sb.toString());
                                */
                            }
                            else {
                                kbVal.add(f);
                            }
                        }
                    }
                    if ((count % 100) == 1) System.out.print(".");
                }
                System.out.println("x");

                System.out.print("  > adding KIF.terms to KB.terms ");
                int oldSize = kb.terms.size();
                kb.terms.addAll(kif.terms);
                System.out.println("... " 
                                   + (kb.terms.size() - oldSize)
                                   + " terms added");

                System.out.println("  > adding " 
                                   + constituentCanonicalPath
                                   + " to KB.constituents");
                if (!kb.constituents.contains(constituentCanonicalPath))
                    kb.constituents.add(constituentCanonicalPath);

                System.out.println("  > clearing KB.formatMap and KB.termFormatMap");
                kb.clearFormatMaps();
            }
            if (duplicateCount > 0) {
                errors.add(duplicateCount + " duplicate statement"
                           + ((duplicateCount > 1) ? "s " : " ")
                           + "detected in " 
                           + (StringUtil.emptyString(constituentCanonicalPath)
                              ? "the input file"
                              : constituentCanonicalPath));
            }
        }
        catch (Exception ex) {
            ex.printStackTrace();
            errors.add(ex.getMessage());
        }
        if ((kif != null) && !errors.isEmpty()) {
            KBmanager mgr = KBmanager.getMgr();
            String esb1str = null;
            StringBuilder esb1 = new StringBuilder();
            StringBuilder esb2 = new StringBuilder();
            for (Iterator ite = errors.iterator(); ite.hasNext();) {
                esb1 = new StringBuilder();
                esb1.append("WARNING in DocGen.addStatementsToKB(");
                esb1.append(kb.name);
                esb1.append(", ");
                esb1.append("[set of ");
                esb1.append(statements.size());
                esb1.append(" statements], ");
                esb1.append(constituentCanonicalPath);
                esb1.append("): ");
                esb1.append((String) ite.next());

                esb1str = esb1.toString();
                System.out.println();
                System.out.println(esb1str);

                esb2.append("\n<br/>");
                esb2.append(esb1str);
                esb2.append("\n<br/>");
            }
            mgr.setError(mgr.getError() + esb2.toString());
        }
        System.out.println("  > numberAdded == " + numberAdded);
        System.out.println("EXIT DocGen.addStatementsToKB("
                           + kb.name + ", "
                           + "[set of " + statements.size() + " statements], "
                           + constituentCanonicalPath + ")");
        System.out.println("  > "
                           + ((System.currentTimeMillis() - t1) / 1000.0)
                           + " seconds elapsed time");

        return numberAdded;
    }

    /** *******************************************************************
     * For adding Formulae when the there might be multiple
     * constituent files, and the filenames are known.  This method
     * assumes that the Formulae in formulaList are already in proper
     * (insertion) order, with derived Formulae coming immediately
     * after the main Formula from which they were derived/computed.
     * 
     */
    private int addFormulaeToKB(KB kb, List<Formula> formulaList) {
        long t1 = System.currentTimeMillis();
        System.out.println("ENTER DocGen.addFormulaeToKB("
                           + kb.name + ", "
                           + "[list of " + formulaList.size() + " formulae])");
        int duplicateCount = 0;
        int numberAdded = 0;
        Set errors = new LinkedHashSet();
        KIF kif = null;
        String sourcePath = null;
        try {
            if ((formulaList != null) && !formulaList.isEmpty()) {

                // 1. Add the Formulae in formulaList to
                // kb.formulaMap.  At the same time, add each
                // statement to a StringBuilder so that the statements
                // can be read with a KIF instance, as if processing a
                // KIF constituent file.
                StringBuilder sb = new StringBuilder();
                Formula   f = null;
                String  key = null;

                System.out.print("  > adding "
                                 + formulaList.size() 
                                 + " Formulae to KB.formulaMap ");
                int count = 0;
                for (Iterator it = formulaList.iterator(); it.hasNext(); count++) {
                    f = (Formula) it.next();
                    key = f.theFormula.intern();
                    kb.formulaMap.put(key, f);
                    numberAdded++;
                    sb.append(key);
                    sb.append(kb.getLineSeparator());
                    if ((count % 100) == 1) System.out.print(".");
                }
                System.out.println("x");

                // 2. Create a KIF object and populate KIF.formulaSet,
                // KIF.formulas, and KIF.terms by parsing the
                // expressions in StringBuilder.
                System.out.println("  > KIF.parse(): reading statements from StringReader");
                kif = new KIF();
                kif.setFilename("");
                StringReader sr = new StringReader(sb.toString());
                errors.addAll(kif.parse(sr));
                sr.close();

                // 3. Populate KB.formulas from KB.formulaMap and
                // KIF.formulas.
                System.out.print("  > adding KIF.formulas to KB.formulas ");
                Set<String> newConstituents = new HashSet<String>();
                count = 0;
                for (Iterator itk = kif.formulas.keySet().iterator(); itk.hasNext(); count++) {
                    // Iterate through the KIF Formulae created from
                    // the SCOW spreadsheet file, adding them to the
                    // KB at the appropriate key.
                    key = (String) itk.next();         
                    ArrayList kifVal = (ArrayList) kif.formulas.get(key);
                    ArrayList kbVal = (ArrayList) kb.formulas.get(key);
                    Formula kbf = null;
                    if ((kifVal != null) && !kifVal.isEmpty()) {
                        if (kbVal == null) {
                            kbVal = new ArrayList();
                            kb.formulas.put(key, kbVal);
                        }
                        for (Iterator itv = kifVal.iterator(); itv.hasNext();) {
                            f = (Formula) itv.next();
                            kbf = (Formula) kb.formulaMap.get(f.theFormula.intern());
                            if (kbf != null) {
                                f = kbf;
                            }
                            if (kbVal.contains(f)) {
                                duplicateCount++;
                                /*
                                  sb = new StringBuilder();
                                  sb.append("The KB ");
                                  sb.append(kb.name);
                                  sb.append(" already contains the formula");
                                  sb.append(kb.getLineSeparator());
                                  sb.append(f.toString());
                                  sb.append(kb.getLineSeparator());
                                  sb.append("loaded from "); 
                                  sb.append(f.getSourceFile());
                                  errors.add(sb.toString());
                                */
                            }
                            else {
                                kbVal.add(f);
                                sourcePath = f.getSourceFile();
                                if (StringUtil.isNonEmptyString(sourcePath)
                                    && !kb.constituents.contains(sourcePath)) {
                                    newConstituents.add(sourcePath);
                                }
                            }
                        }
                    }
                    if ((count % 100) == 1) System.out.print(".");
                }
                System.out.println("x");

                if (duplicateCount > 0) {
                    errors.add(duplicateCount + " duplicate statement"
                               + ((duplicateCount > 1) ? "s " : " ")
                               + "detected in " 
                               + (StringUtil.emptyString(sourcePath)
                                  ? "the input file"
                                  : sourcePath));
                }

                System.out.print("  > adding KIF.terms to KB.terms ");
                int oldSize = kb.terms.size();
                kb.terms.addAll(kif.terms);
                System.out.println("... " 
                                   + (kb.terms.size() - oldSize)
                                   + " terms added");

                if (!newConstituents.isEmpty()) {
                    System.out.println("  > adding " + newConstituents + " to KB.constituents");
                    newConstituents.addAll(kb.constituents);
                    kb.constituents.clear();
                    kb.constituents.addAll(newConstituents);
                    Collections.sort(kb.constituents);
                    newConstituents.clear();
                }

                System.out.println("  > clearing KB.formatMap and KB.termFormatMap");
                kb.clearFormatMaps();
            }
        }
        catch (Exception ex) {
            ex.printStackTrace();
            errors.add(ex.getMessage());
        }
        if ((kif != null) && !errors.isEmpty()) {
            KBmanager mgr = KBmanager.getMgr();
            StringBuilder esb1 = null;
            StringBuilder esb2 = new StringBuilder();
            String esb1str = null;
            for (Iterator ite = errors.iterator(); ite.hasNext();) {
                esb1 = new StringBuilder();
                esb1.append("WARNING in DocGen.addFormulaeToKB(");
                esb1.append(kb.name);
                esb1.append(", ");
                esb1.append("[list of ");
                esb1.append(formulaList.size());
                esb1.append(" statements]): ");
                esb1.append((String) ite.next());

                esb1str = esb1.toString();
                System.out.println(esb1str);

                esb2.append("\n<br/>");
                esb2.append(esb1);
                esb2.append("\n<br/>");
            }
            mgr.setError(mgr.getError() + esb2.toString());
        }

        System.out.println("  > numberAdded == " + numberAdded);
        System.out.println("EXIT DocGen.addFormulaeToKB("
                           + kb.name + ", "
                           + "[list of " + formulaList.size() + " formulae])");
        System.out.println("  > "
                           + ((System.currentTimeMillis() - t1) / 1000.0)
                           + " seconds elapsed time");

        return numberAdded;
    }

    /** *******************************************************************
     * 
     */
    private String getInverse(KB kb, String pred) {
        String inv = null;
        try {
            if (!StringUtil.isStringWithSpaces(pred)) {
                Set inverses = new HashSet();
                inverses.addAll(kb.getTermsViaPredicateSubsumption("inverse",
                                                                   1,
                                                                   pred,
                                                                   2,
                                                                   true));
                inverses.addAll(kb.getTermsViaPredicateSubsumption("inverse",
                                                                   2,
                                                                   pred,
                                                                   1,
                                                                   true));
                if (!inverses.isEmpty()) {
                    String invstr = (String) inverses.iterator().next();
                    if (!StringUtil.isStringWithSpaces(invstr)) {
                        inv = invstr;
                    }
                }
            }
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
        return inv;
    }

    /** *************************************************************
     * Returns the value in row indexed by colIdxStr.
     *
     * @param row A List representing one row of a spreadsheet or
     * database table
     *
     * @param colIdxStr A String, in most cases a column name, which
     * indexes a single cell in row
     *
     * @return A String, which will be an empty String if the cell
     * contains no value
     */
    private String getCellValue(List row, String colIdxStr) {
        String result = "";
        try {
            int idx = getColumnIndex(colIdxStr);
            if ((idx > -1) && (row.size() > idx)) {
                String v = (String) row.get(idx);
                if (isNonEmptyString(v)) {
                    // v = v.trim();
                    if (v.matches(".*\\w+.*"))
                        v = v.trim();
                    v = StringUtil.removeEnclosingQuotes(v);
                    if (v.matches(".*(?i)$blank.*") || v.matches("(?i)null")) {
                        v = "";
                    }
                }
                result = v;
            }
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
        return result;
    }

    /** *************************************************************
     * Creates headword, synonym, and xsdfile statements based on
     * pseudo-namespace values.
     *
     * @param rowIdx An int value that indexes the table row currently
     * being processed
     *
     * @param statements A Set in which the String representations of
     * KIF formulae are collected
     *
     * @param subject A String, the SUO-KIF term to which the names
     * apply
     *
     * @param nameCellValue A String possibly containing multiple
     * values delimited by a vertical bar character
     *
     * @return void
     */
    private void makeNameStatements(int rowIdx, 
                                    Set statements, 
                                    String defaultNamespace,
                                    String subject, 
                                    String nameCellValue) {
        try {
            if (isNonEmptyString(subject) && isNonEmptyString(nameCellValue)) {
                List<String> valList = Arrays.asList(nameCellValue.split("\\|"));
                String w3cd = getW3cNamespaceDelimiter();
                String kifd = getKifNamespaceDelimiter();
                String kifDefaultNS = normalizeNamespace(defaultNamespace);
                for (String v : valList) {
                    // System.out.println("v == " + v);
                    int w3cIdx = v.lastIndexOf(w3cd);
                    if (w3cIdx > -1) {

                        String kifPseudoNS = normalizeNamespace(v.substring(0, w3cIdx));
                        String baseName = v.substring(w3cIdx + w3cd.length());
                        String token = getNameRelationToken(kifPseudoNS);

                        // System.out.println("  token == " + token);

                        if (isNonEmptyString(token)) {
                            String w3cPseudoNS = StringUtil.kifToW3c(kifPseudoNS);
                            String w3cPseudoNsPrefix = 
                                (w3cPseudoNS.substring(w3cPseudoNS.lastIndexOf(w3cd)
                                                       + w3cd.length()) + w3cd);
                            String kifNsOfToken = getRelationTokenNamespace(w3cPseudoNS, 
                                                                            token,
                                                                            kifDefaultNS);
                            
                            statements.add(makeStatement("termFormat",
                                                         kifPseudoNS,
                                                         subject,
                                                         StringUtil.quote(baseName)));
                            
                            String predicate = getNameRelationForToken(token);

                            // System.out.println("  predicate == " + predicate);

                            if (token.equals("xsdname")) {
                                statements.add(makeStatement(predicate, 
                                                             StringUtil.quote(baseName), 
                                                             subject));
                            }
                            else if (getNameRelationTokens().contains(token)) {
                                String context = (token.equals("_en")
                                                  ? "EnglishLanguage"
                                                  : kifNsOfToken);
                                statements.add(makeStatement((token.equals("_en")
                                                              ? "termFormat"
                                                              : predicate), 
                                                             context, 
                                                             subject, 
                                                             StringUtil.quote(baseName)));
                                /*
                                  if (predicate.equals("headword")) {
                                  statements.add(makeStatement("termFormat", 
                                  context, 
                                  subject, 
                                  StringUtil.quote(baseName)));
                                  }
                                */
                            }

                            /*
                              if (!token.equals("xsdname") && !kifNsOfToken.equals(kifDefaultNS)) {
                              String w3cNsOfToken = StringUtil.kifToW3c(kifNsOfToken);
                              String w3cNsOfTokenPrefix = 
                              (w3cNsOfToken.substring(w3cNsOfToken.lastIndexOf(w3cd)
                              + w3cd.length()) + w3cd);

                              statements.add(makeStatement("termFormat",
                              kifDefaultNS,
                              subject,
                              StringUtil.quote(w3cNsOfTokenPrefix 
                              + baseName)));
                              }
                            */
                            // System.out.println("");

                        }
                    }
                }
            }
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
        return;
    }

    /** *************************************************************
     * Creates statements mapping terms from more specific to more
     * general ontologies.
     *
     * @param rowIdx An int value that indexes the table row currently
     * being processed
     *
     * @param statements A Set in which the String representations of
     * KIF statements are collected
     *
     * @param subject A String, the SUO-KIF term that is the subject
     * or focal term of the row
     *
     * @param mappingAuthority A token indicating the authority for
     * the mapping statement, and from which a namespace can be
     * obtained
     *
     * @param mappingRelator The relator of the mapping statement
     *
     * @param mappingRange The range or second argument of the mapping
     * statement
     *
     * @return void
     */
    private void makeMappingStatements(int rowIdx, 
                                       Set statements, 
                                       String subject, 
                                       String mappingAuthority,
                                       String mappingRelator,
                                       String mappingRange) {
        try {
            if (isNonEmptyString(subject) 
                && isNonEmptyString(mappingAuthority)
                && isNonEmptyString(mappingRelator)
                && isNonEmptyString(mappingRange)) {
                String namespace = mappingAuthority;
                int dotIdx = namespace.indexOf(".");
                if (dotIdx > -1) namespace = namespace.substring(0, dotIdx);
                if (namespace.equalsIgnoreCase("x")) namespace = "coa";
                String w3cd = getW3cNamespaceDelimiter();
                String w3cPref = (namespace + w3cd);
                if (mappingRelator.indexOf(w3cd) == -1) 
                    mappingRelator = (w3cPref + mappingRelator);
                if (mappingRange.indexOf(w3cd) == -1) 
                    mappingRange = (w3cPref + mappingRange);
                statements.add(makeStatement(mappingRelator, subject, mappingRange));
            }
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
        return;
    }

    /** *************************************************************
     * Creates cardinality statement Strings and adds them to the Set
     * statements.
     *
     * @param kb The current Sigma KB
     *
     * @param rowIdx An int value that indexes the table row currently
     * being processed
     *
     * @param statements A Set in which the String representations of
     * KIF formulae are collected
     *
     * @param range A String, the SUO-KIF term that will be the first
     * argument in the created cardinality statements
     *
     * @param cardinality A String token that denotes an integer
     * value, or a range consisting of min and max values
     *
     * @return void
     */
    private void makeRangeCardinalityStatements(KB kb,
                                                int rowIdx,
                                                Set statements, 
                                                String range, 
                                                String cardinality) {
        try {
            if (isNonEmptyString(cardinality)) {
                String min = null;
                String max = null;
                int didx = cardinality.indexOf("-");
                if (didx > -1) { 
                    min = cardinality.substring(0,didx);
                    max = ((cardinality.length() > didx)
                           ? cardinality.substring(didx+1)
                           : "");
                }
                ArrayList arg0s = new ArrayList();
                ArrayList arg2s = new ArrayList();
                if (isNonEmptyString(min) && !min.equals("0")) {
                    arg0s.add("hasMinCardinality");
                    arg2s.add(min);
                }
                if (isNonEmptyString(max) && !max.equals("n")) {
                    arg0s.add("hasMaxCardinality");
                    arg2s.add(max);
                }
                if (!cardinality.contains("-")
                    && !cardinality.equals("0") 
                    && !cardinality.equals("n")) {
                    arg0s.add("hasExactCardinality");
                    arg2s.add(cardinality);
                }
                int a0Len = arg0s.size();
                String cArg2 = null;
                String cArg0 = null;
                String specArg0 = null;
                for (int i = 0; i < a0Len; i++) {
                    cArg0 = (String) arg0s.get(i);
                    specArg0 = kb.getFirstTermViaPredicateSubsumption("subrelation",
                                                                      2,
                                                                      cArg0,
                                                                      1,
                                                                      true);
                    if (StringUtil.isNonEmptyString(specArg0)) {
                        cArg0 = specArg0;
                    }
                    cArg2 = (String) arg2s.get(i);
                    statements.add(makeStatement(cArg0, range, cArg2));
                }
            }
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
        return;
    }

    /** *******************************************************************
     * Returns true if formula is an atomic statement, else returns
     * false.  Note that only conventional logical operators are
     * checked.  Statements formed with second order predicates, which
     * might take quoted statements as arguments, will pass this test.
     *
     * @param formula The String representation of the formula to be
     * tested
     * 
     * @param allowNegatedForm If true, this method will return true
     * if formula is a negated atomic statement
     * 
     * @return true or false
     */
    private boolean isAtomicStatement(String formula, boolean allowNegatedForm) {
        boolean ans = false;
        try {
            if (Formula.listP(formula) && !Formula.empty(formula)) {
                Formula f = new Formula();
                f.read(formula);
                String arg0 = f.car();
                if (allowNegatedForm && arg0.equals(Formula.NOT)) {
                    ans = isAtomicStatement(f.getArgument(1), false);
                }
                else {
                    ans = !Formula.isLogicalOperator(arg0);
                }
            }
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
        return ans;
    }

    /** *******************************************************************
     * Returns a new String in which all logical operators in the
     * input, assumed to be the String representation of a KIF
     * Formula, have been replaced with their duals, and variables are
     * renumbered as appropriate.  The resulting String may be used as
     * a query expression to check the validity of the original input
     * Formula.
     *
     * @param input A String representing a SUO-KIF Formula or part of
     * a Formula
     * 
     * @param replaceSkolemTerms If true, Skolem terms in the original
     * Formula will be replaced with variables in the result
     * 
     * @return A new String, or an empty String if the input cannot be
     * transformed by substitution of duals.
     */
    private String swapDualOperators(String input, boolean replaceSkolemTerms) {
        String result = "";
        try {
            String flist = input.trim();
            boolean isSkolem = Formula.isSkolemTerm(flist);
            StringBuilder sb = new StringBuilder();
            String newOp = null;
            if (Formula.listP(flist)) {
                if (Formula.empty(flist)) {
                    sb.append(flist);
                }
                else {
                    Formula f = new Formula();
                    f.read(flist);
                    List tuple = f.literalToArrayList();
                    int flen = tuple.size();
                    String arg0 = ((flen > 0)
                                   ? (String) tuple.get(0)
                                   : "");
                    if (Formula.isLogicalOperator(arg0)) {
                        if (arg0.equals(Formula.NOT)) {
                            String next = f.getArgument(1);
                            if (!isAtomicStatement(next, false)) {
                                next = swapDualOperators(next, replaceSkolemTerms);
                            }
                            sb.append(next);
                        }
                        else {
                            StringBuilder sb2 = new StringBuilder();
                            arg0 = Formula.getDualOperator(arg0);
                            sb2.append(Formula.LP);
                            sb2.append(arg0);
                            boolean isConjunct = arg0.equals(Formula.AND);
                            boolean allAreAtomic = isConjunct;
                            for (int i = 1; i < flen; i++) {
                                String argN = (String) tuple.get(i);
                                sb2.append(" ");
                                if (isConjunct) 
                                    allAreAtomic = (allAreAtomic && isAtomicStatement(argN,
                                                                                      true));
                                sb2.append(swapDualOperators(argN, replaceSkolemTerms));
                            }
                            sb2.append(Formula.RP);
                            String subf = sb2.toString();
                            if (allAreAtomic) 
                                subf = Formula.normalizeVariables(subf, true);
                            sb.append(subf);
                        }
                    }
                    else {
                        sb.append("(not ");
                        sb.append(flist);
                        sb.append(")");
                    }
                }
            }
            result = sb.toString();
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
        return result;
    }

    /** *************************************************************
     * Creates String representations of SUO-KIF formulae, typically
     * conditional statements or other "rules", and adds them to the
     * Set rules.
     *
     * @param kb The current Sigma KB
     *
     * @param rowIdx An int value that indexes the table row currently
     * being processed
     *
     * @param rules A Set in which the String representations of KIF
     * formulae are collected
     *
     * @param subject A String, the SUO-KIF constant that identifies
     * (indexes) a formula
     *
     * @param kifFormulae A String representation of at least one and
     * possibly multiple SUO-KIF formulae, from which the formulae
     * will be parsed to construct kif metastatements
     *
     * @param documentation A documentation String that describes the
     * intended meaning of the formulae parsed from kifFormulae
     *
     * @return void
     */
    private void makeKifRules(KB kb, 
                              int rowIdx, 
                              Set rules, 
                              String subject, 
                              String kifFormulae,
                              String documentation) {
        try {
            if (StringUtil.isNonEmptyString(subject) 
                && StringUtil.isNonEmptyString(kifFormulae)) {
                List<Formula> formulae = readKif(kifFormulae);
                if (!formulae.isEmpty()) {

                    // For the "ordinary" form.
                    StringBuilder sb1 = new StringBuilder();

                    // For the canonical "positive" form.
                    StringBuilder sb2 = new StringBuilder();

                    // For the query form.
                    StringBuilder sb3 = new StringBuilder();

                    int flen = formulae.size();
                    // int negCount = 0;
                    int i = 0;
                    for (Iterator itr = formulae.iterator(); itr.hasNext(); i++) {
                        Formula f = (Formula) itr.next();
                        f.theFormula = 
                            StringUtil.treeReplace(".*(?i)MaxInteger$", 
                                                   Integer.toString(Integer.MAX_VALUE),
                                                   f.theFormula);
                        Formula quantF = new Formula();
                        if (Formula.isLogicalOperator(f.car())) {
                            quantF.read(f.makeQuantifiersExplicit(false));
                        }
                        else {
                            quantF = f;
                        }

                        Formula negQuantF = new Formula();
                        if (Formula.isLogicalOperator(f.car())) {
                            negQuantF.read("(not " + quantF.theFormula + ")");
                        }
                        else {
                            negQuantF.read("(not " + f.theFormula + ")");
                        }

                        // Add to the non-canonical form.
                        if (i > 0) sb1.append(" ");
                        sb1.append(f.theFormula);

                        // Add to the canonical form.
                        Formula canonF = null;
                        if (Formula.isLogicalOperator(f.car())) {
                            canonF = quantF.toCanonicalClausalForm();
                        }
                        else {
                            canonF = f.toCanonicalKifSpecialForm(true);
                        }
                        if (i > 0) sb2.append(" ");
                        sb2.append(canonF.theFormula);

                        // Add to the data validation query form.
                        /*
                          Formula canonNegF = null;
                          if (Formula.isLogicalOperator(f.car())) {
                          canonNegF = negQuantF.toCanonicalClausalForm();
                          }
                          else {
                          canonNegF = f.toCanonicalKifSpecialForm(true);
                          }
                        */
                        if (i > 0) sb3.append(" ");
                        // sb3.append(canonNegF.theFormula.replace("Sk", "?VAR"));
                        sb3.append(f.toOpenQueryForNegatedDualForm().theFormula);
                        
                        /*
                          String query = swapDualOperators(canonF.theFormula, true);
                          if (StringUtil.isNonEmptyString(query)) {
                          sb3.append(query);
                          negCount++;
                          }
                        */
                    }
                    if (flen > 1) {
                        sb1.insert(0, "(and ");
                        sb1.append(")");
                        sb2.insert(0, "(and ");
                        sb2.append(")");
                        sb3.insert(0, "(or ");
                        sb3.append(")");
                    }
                    /*
                      if (negCount > 1) {
                      sb3.insert(0, "(or ");
                      sb3.append(")");
                      }
                    */
                    String predicate = "kifManifestation";
                    String specpred = 
                        kb.getFirstTermViaPredicateSubsumption("subrelation",
                                                               2,
                                                               predicate,
                                                               1,
                                                               true);
                    if (StringUtil.isNonEmptyString(specpred)) {
                        predicate = specpred;
                    }

                    subject = StringUtil.w3cToKif(subject);

                    // Rule name
                    rules.add(getLineSeparator() + ";; " + subject + getLineSeparator());

                    // Rule documentation
                    if (isNonEmptyString(documentation)) {
                        String arg3 = StringUtil.quote(StringUtil.replaceRepeatedDoubleQuotes(documentation));
                        Formula docF = new Formula();
                        docF.read(makeStatement("documentation",
                                                subject,
                                                "EnglishLanguage",
                                                arg3));
                        rules.add(docF.format("", SP2, getLineSeparator()) + getLineSeparator());
                    }

                    // Rule indexed by name
                    Formula nameF = new Formula();
                    nameF.read(makeStatement(predicate, subject, sb1.toString()));
                    rules.add(nameF.format("", SP2, getLineSeparator()) + getLineSeparator());

                    // Canonical form of rule indexed by name
                    Formula metaF = new Formula();
                    metaF.read(makeStatement("canonicalKifManifestation",
                                             subject,
                                             sb2.toString()));
                    rules.add(metaF.format("", SP2, getLineSeparator()) + getLineSeparator());

                    // Canonical negated form of rule indexed by name
                    if (sb3.length() > 0) {
                        Formula canonicalNegF = new Formula();
                        canonicalNegF.read(makeStatement("kifDataIntegrityViolationQuery",
                                                         subject,
                                                         sb3.toString()));
                        rules.add(canonicalNegF.format("", SP2, getLineSeparator()) + getLineSeparator());
                    }

                    // The expression as input to a first order prover
                    Formula origF = new Formula();
                    origF.read(sb1.toString());
                    rules.add(origF.format("", SP2, getLineSeparator()) + getLineSeparator());
                }
            }
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
        return;
    }

    /** *******************************************************************
     */
    private ArrayList<Formula> readKif(String input) {

        /*
          System.out.println("ENTER DocGen.readKif(" + input + ")");
        */

        ArrayList<Formula> formulae = new ArrayList<Formula>();
        try {
            if (StringUtil.isNonEmptyString(input)) {
                KIF parser = new KIF();
                parser.setParseMode(KIF.RELAXED_PARSE_MODE);
                String err = parser.parseStatement(input);

                if (StringUtil.isNonEmptyString(err)) {
                    System.out.println("ERROR in DocGen.readKif(" + input + ")");
                    System.out.println(err);
                }

                for (Iterator it = parser.formulaSet.iterator(); it.hasNext();) {
                    Formula f = new Formula();
                    f.read((String) it.next());
                    formulae.add(f);
                }
            }
        }
        catch (Exception ex) {
            System.out.println("ERROR in DocGen.readKif(" + input + ")");
            System.out.println(ex.getMessage());
            ex.printStackTrace();
        }

        /*
          System.out.println("EXIT DocGen.readKif(" + input + ")");
          System.out.println("  ==> " + formulae);
        */

        return formulae;
    }

    /** *************************************************************
     * Creates String representations of SPARQL expressions.
     *
     * @param kb The current Sigma KB
     *
     * @param rowIdx An int value that indexes the table row currently
     * being processed
     *
     * @param statements A Set in which the String representations of
     * SPARQL formulae are collected
     *
     * @param subject A String, the SUO-KIF constant that identifies
     * (indexes) a SPARQL expression
     *
     * @param sparqlExprs A String representation of at least one and
     * possibly multiple SPARQL expressions, which will be parsed to
     * construct wff expressions
     *
     * @param documentation A documentation String that describes the
     * intended meaning of the expressions parsed from sparqlExprs
     *
     * @return void
     */
    private void makeSparqlStatements(KB kb, 
                                      int rowIdx, 
                                      Set statements, 
                                      String subject, 
                                      String sparqlExprs,
                                      String documentation) {
        try {
            if (StringUtil.isNonEmptyString(subject) 
                && StringUtil.isNonEmptyString(sparqlExprs)) {

                String predicate = "sparqlManifestation";
                String specpred = 
                    kb.getFirstTermViaPredicateSubsumption("subrelation",
                                                           2,
                                                           predicate,
                                                           1,
                                                           true);
                if (StringUtil.isNonEmptyString(specpred)) {
                    predicate = specpred;
                }

                subject = StringUtil.w3cToKif(subject);

                // SPARQL rule name
                statements.add(getLineSeparator() + ";; " + subject + getLineSeparator());

                // Rule documentation
                if (isNonEmptyString(documentation)) {
                    String arg3 = 
                        StringUtil.quote(StringUtil.replaceRepeatedDoubleQuotes(documentation));
                    Formula docF = new Formula();
                    docF.read(makeStatement("documentation",
                                            subject,
                                            "EnglishLanguage",
                                            arg3));
                    statements.add(docF.format("", SP2, getLineSeparator()) 
                                   + getLineSeparator());
                }

                String normalized = sparqlExprs;
                if (normalized.contains("{")) {
                    normalized = normalized.trim();
                    normalized = StringUtil.removeEscapedDoubleQuotes(normalized);
                    normalized = StringUtil.removeInternalDoubleQuotes(normalized);
                    normalized = normalized.replaceAll("\\{", " { ");
                    normalized = normalized.replaceAll("\\}", " } ");
                    normalized = normalized.replaceAll("\\(", " ( ");
                    normalized = normalized.replaceAll("\\)", " ) ");
                    normalized = normalized.replaceAll("\\.", " .");
                    normalized = normalized.replaceAll("\\s+", " ");
                    normalized = normalized.replaceAll("\\s+\\#", "\n\n#");
                    // normalized = normalized.replaceAll("(\\#+)", "\n\n\1");
                    // normalized = normalized.replaceAll("(?i)select ", "\nSELECT ");
                    // normalized = normalized.replaceAll("(?i)construct ", "\nCONSTRUCT ");
                    // normalized = normalized.replaceAll("(?i)where ", "WHERE ");
                    // normalized = normalized.replaceAll("(?i)from ", "FROM ");
                    // normalized = normalized.replaceAll("(?i)where", "\nWHERE");
                }
                normalized = normalized.trim();

                // Rule indexed by name
                statements.add(makeStatement(predicate, subject, StringUtil.quote(normalized)));
            }
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
        return;
    }

    /** *******************************************************************
     *
     */
    private String addTermNamespacePrefixes(String expr) {
        String result = expr;
        try {
            if (StringUtil.isNonEmptyString(expr)) {
                if (expr.contains("{")) {
                    List<String> ignore = 
                        Arrays.asList("INSERT", "insert",
                                      "INTO", "into",
                                      "FROM", "from",
                                      "WHERE", "where",
                                      "CONSTRUCT", "construct",
                                      "DELETE", "delete",
                                      "SELECT", "select",
                                      "DESCRIBE", "describe",
                                      "NOT", "not");
                    String splitPattern =
                        "\\s*\\{\\s*|\\s*\\}\\s*|\\s*\\.\\s*|\\s*\\(\\s*|\\s*\\)\\s*|\\s+";
                    List<String> terms = Arrays.asList(expr.split(splitPattern));
                    for (String str : terms) {
                        if (StringUtil.isNonEmptyString(str)
                            && !ignore.contains(str) 
                            && !str.startsWith("?")
                            && !str.contains(getW3cNamespaceDelimiter())) {
                            String replStr = (String) basesToQWords.get(str);
                            if (StringUtil.isNonEmptyString(replStr)) {
                                result = result.replaceAll((" " + str), (" " + replStr));
                            }
                        }
                    }
                }
            }
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
        return result;
    }

    /// BEGIN: code for Turtle generation.

    public static String writeTurtleFile(KB kb, File outfile) {
        String status = "";
        FileWriter fw = null;
        BufferedWriter bw = null;
        try {
            fw = new FileWriter(outfile);
            bw = new BufferedWriter(fw);

            LinkedHashSet<String> ttlFormulae = new LinkedHashSet<String>();
            ArrayList<String> owlFormulae = new ArrayList<String>();

            String w3c = StringUtil.getW3cNamespaceDelimiter();
            String prefix = ("coa" + w3c);
            String op = "-";
            int flen = -1;
            String arg0 = null;
            String arg1 = null;
            String arg2 = null;
            StringBuilder sb = null;
            for (Formula f : kb.formulaMap.values()) {
                flen = f.listLength();
                if (flen == 3) {
                    arg0 = f.getArgument(0);
                    arg1 = f.getArgument(1);
                    arg2 = f.getArgument(2);

                    if (arg0.contains(w3c)) {

                        arg0 = kifToSpiCompositeRelator(arg0);

                        sb = new StringBuilder();
                        sb.append(arg1);
                        sb.append(" ");
                        sb.append(arg0);
                        sb.append(" ");
                        String unq = arg2;
                        if (StringUtil.isQuotedString(arg2)) 
                            unq = StringUtil.unquote(unq);
                        if (!StringUtil.isStringWithSpaces(unq)) {
                            if (StringUtil.isDigitString(unq)) {
                                arg2 = ("\"" + unq + "\"" + "^^xsd:int");
                            }
                        }
                        sb.append(arg2);
                        sb.append(" .");

                        ttlFormulae.add(sb.toString());

                        sb = new StringBuilder();
                        sb.append(arg0);
                        sb.append(" ");
                        sb.append("rdf:type ");
                        sb.append("rdf:Property .");
                        String owlProp = sb.toString();
                        if (!owlFormulae.contains(owlProp))
                            owlFormulae.add(owlProp);
                        if (arg0.matches(".*(?i):IsSubClassOf$") || arg0.matches(".*:IsA$")) {
                            if (arg0.matches(".*(?i):IsSubClassOf$")) {
                                sb = new StringBuilder();
                                sb.append(arg1);
                                sb.append(" ");
                                sb.append("rdf:type ");
                                sb.append("rdfs:Class .");
                                owlProp = sb.toString();
                                if (!owlFormulae.contains(owlProp)) 
                                    owlFormulae.add(owlProp);
                            }
                            sb = new StringBuilder();
                            sb.append(arg2);
                            sb.append(" ");
                            sb.append("rdf:type ");
                            sb.append("rdfs:Class .");
                            owlProp = sb.toString();
                            if (!owlFormulae.contains(owlProp))
                                owlFormulae.add(owlProp);
                        }
                    }
                }
            }

            if (!owlFormulae.isEmpty()) {
                Collections.sort(owlFormulae);
                for (String fstr : owlFormulae) {
                    bw.write(fstr.toCharArray());
                    bw.newLine();
                }
                bw.newLine();
                bw.flush();
            }

            if (!ttlFormulae.isEmpty()) {
                for (String ttl : ttlFormulae) {
                    bw.write(ttl.toCharArray());
                    bw.newLine();
                }
                bw.flush();
                try {
                    if (bw != null) bw.close();
                    if (fw != null) fw.close();
                }
                catch (Exception ioe) {
                    ioe.printStackTrace();
                }
            }

            if (outfile.canRead())
                status = outfile.getCanonicalPath();
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
        finally {
            try {
                if (fw != null) fw.close();
                if (bw != null) bw.close();
            }
            catch(Exception ex2) {
                ex2.printStackTrace();
            }
        }
        return status;
    }

    private static String kifToSpiCompositeRelator(String nonAtomicTerm) {
        String relator = nonAtomicTerm;
        try {
            if (nonAtomicTerm.startsWith("(") && nonAtomicTerm.contains("PredicateFn")) {
                Formula f = new Formula();
                f.read(nonAtomicTerm);
                if (f.listLength() == 3) {
                    String w3c = StringUtil.getW3cNamespaceDelimiter();
                    int w3clen = w3c.length();
                    String prefix = ("coa" + w3c);
                    String catop = ".";
                    String arg0 = f.getArgument(0);
                    String arg1 = f.getArgument(1);
                    String arg2 = f.getArgument(2);
                    if (arg0.equals("PredicateFn")) {
                        int idx = arg1.indexOf(w3c);
                        if ((idx != -1) && ((idx + w3clen) < (arg1.length() - 1)))
                            arg1 = arg1.substring(idx + w3clen);
                        idx = arg2.indexOf(w3c);
                        if ((idx != -1) && ((idx + w3clen) < (arg2.length() - 1)))
                            arg2 = arg2.substring(idx + w3clen);
                        StringBuilder sb = new StringBuilder();
                        sb.append(prefix);
                        sb.append(arg1);
                        sb.append(catop);
                        sb.append(arg2);
                        relator = sb.toString();
                    }
                }
            }
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
        return relator;
    }

    /// END: code for Turtle generation.


    /// BEGIN: code for XSD generation.

    /** *************************************************************
     * 
     */
    private TreeMap mapFocalTermsByNamespace(KB kb, List focalTerms) {
        TreeMap result = new TreeMap();
        try {
            String term = null;
            String ns = null;
            List terms = null;
            for (Iterator it = focalTerms.iterator(); it.hasNext();) {
                term = (String) it.next();
                ns = getTermNamespace(kb, term);
                if (StringUtil.isNonEmptyString(ns)) {
                    terms = (List) result.get(ns);
                    if (terms == null) {
                        terms = new ArrayList();
                        result.put(ns, terms);
                    }
                    if (!terms.contains(term)) {
                        terms.add(term);
                    }
                }
            }
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
        return result;
    }

    /** *************************************************************
     * Returns an org.w3c.dom.Document XML Document object created
     * from data pertaining to namespace in kb.  If some data cannot
     * be obtained from kb, the Document object will be incomplete and
     * probably invalid.
     *
     * @param kb The KB from which data about namespace will be
     * retrieved
     *
     * @param namespace A SUO-KIF term denoting a namespace
     *
     * @return An XML Document object 
     */
    private Document createXsdDocumentFromKb(KB kb, String namespace) {
        Document doc = null;
        try {
            javax.xml.parsers.DocumentBuilderFactory _dbf = 
                javax.xml.parsers.DocumentBuilderFactory.newInstance();
            _dbf.setNamespaceAware(true);
            javax.xml.parsers.DocumentBuilder _db = _dbf.newDocumentBuilder();
            DOMImplementation _di = _db.getDOMImplementation();
            doc = _di.createDocument("http://www.w3.org/2001/XMLSchema", 
                                     "xs:schema",
                                     null);
            Element docelem = doc.getDocumentElement();

            String[][] attrs = { {"uri", "targetNamespace"},
                                 {"xsdElementFormDefault", "elementFormDefault"},
                                 {"xsdAttributeFormDefault", "attributeFormDefault"}
            };

            String val = null;
            String pred = null;
            String attr = null;
            int sarg = 1;
            int varg = 2;
            for (int i = 0; i < attrs.length; i++) {
                pred = attrs[i][0];
                attr = attrs[i][1];
                if (pred.equals("uri")) { sarg = 2; varg = 1; }
                val = kb.getFirstTermViaAskWithRestriction(0, 
                                                           pred, 
                                                           sarg, 
                                                           namespace,
                                                           varg);
                val = StringUtil.removeEnclosingQuotes(val);
                if (StringUtil.isNonEmptyString(val)) {
                    docelem.setAttribute(attr, val);
                }
            }

            // xmlns
            List formulae = kb.askWithRestriction(0, "xmlnsAbbrev", 1, namespace);
            Formula f = null;
            String ns2 = null;
            String abbrev = null;
            String uri = null;
            Iterator it = null;
            for (it = formulae.iterator(); it.hasNext();) {
                f = (Formula) it.next();
                ns2 = f.getArgument(2);
                abbrev = StringUtil.removeEnclosingQuotes(f.getArgument(3));
                uri = kb.getFirstTermViaAskWithRestriction(0, "uri", 2, ns2, 1);
                uri = StringUtil.removeEnclosingQuotes(uri);
                if (StringUtil.isNonEmptyString(abbrev)
                    && StringUtil.isNonEmptyString(uri)) {
                    docelem.setAttributeNS("http://www.w3.org/2000/xmlns/",
                                           ("xmlns:" + abbrev),
                                           uri);
                }
            }

            // imports
            List imported = kb.getTermsViaAskWithRestriction(0, "xsdImport", 1, namespace, 2);
            String filename = null;
            for (it = imported.iterator(); it.hasNext();) {
                ns2 = (String) it.next();
                uri = kb.getFirstTermViaAskWithRestriction(0, "uri", 2, ns2, 1);
                uri = StringUtil.removeEnclosingQuotes(uri);
                filename = getXsdFileName(kb, ns2);
                filename = StringUtil.removeEnclosingQuotes(filename);
                if (StringUtil.isNonEmptyString(uri) 
                    && StringUtil.isNonEmptyString(filename)) {
                    Element _el = doc.createElementNS("http://www.w3.org/2001/XMLSchema",
                                                      "xs:import");
                    _el.setAttribute("namespace", uri);
                    _el.setAttribute("schemaLocation", filename);
                    docelem.appendChild(_el);
                }
            }

            // annotation
            String annostr = kb.getFirstTermViaAskWithRestriction(0, 
                                                                  "xsdAnnotation", 
                                                                  1, 
                                                                  namespace,
                                                                  3);
            annostr = StringUtil.removeEnclosingQuotes(annostr);
            if (StringUtil.isNonEmptyString(annostr)) {
                Element _annotation = doc.createElementNS("http://www.w3.org/2001/XMLSchema",
                                                          "xs:annotation");
                Element _documentation = doc.createElementNS("http://www.w3.org/2001/XMLSchema",
                                                             "xs:documentation");
                _documentation.setTextContent(annostr);
                _annotation.appendChild(_documentation);
                docelem.appendChild(_annotation);
            }
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
        return doc;
    }

    /** *************************************************************
     * Returns an org.w3c.dom.Document XML Document object created
     * from a skeleton file, or from data in kb.  If a skeleton file
     * cannot be loaded and data cannot be obtained from kb, the
     * Document object will be incomplete and probably invalid.
     *
     * @param kb The KB from which data about namespace will be
     * retrieved
     *
     * @param namespace A SUO-KIF term denoting a namespace
     *
     * @return An XML Document object 
     */
    private Document createXsdDocument(KB kb, String namespace) {
        Document doc = null;
        try {
            String indirPath = KBmanager.getMgr().getPref("kbDir");
            if (StringUtil.emptyString(indirPath)) {
                System.out.println("WARNING in DocGen.createXsdDocument("
                                   + kb.name + ", "
                                   + namespace + ")");
                System.out.println("  No value is set for kbDir");
            }
            File indir = new File(indirPath);
            if (!indir.isDirectory() || !indir.canRead()) {
                System.out.println("WARNING in DocGen.createXsdDocument("
                                   + kb.name + ", "
                                   + namespace + ")");
                System.out.println("  Cannot read from " + indir.getCanonicalPath());
            }
            else {                
                doc = loadXsdSkeletonFile(kb, namespace, indir);
            }
            if (doc == null) {
                doc = createXsdDocumentFromKb(kb, namespace);
            }
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
        return doc;
    }

    /** *************************************************************
     * 
     */
    private Document loadXsdSkeletonFile(KB kb, String namespaceTerm, File indir) {
        Document doc = null;
        File skeleton = null;
        String suffix = ".skeleton";
        BufferedReader br = null;
        try {
            String nsName = stripNamespacePrefix(kb, namespaceTerm);
            String inFileName = nsName + suffix;
            String xsdFileName = getXsdFileName(kb, namespaceTerm);
            if (StringUtil.isNonEmptyString(xsdFileName)) {
                int eidx = xsdFileName.indexOf(".xsd");
                if (eidx > -1) {
                    xsdFileName = xsdFileName.substring(0, eidx);
                }
                inFileName = xsdFileName + suffix;
            }
            skeleton = new File(indir, inFileName);

            // Give up if there is no skeleton file.
            if (!skeleton.canRead()) {
                System.out.println("WARNING in DocGen.loadXsdSkeletonFile("
                                   + kb.name + ", "
                                   + namespaceTerm + ", "
                                   + indir.getCanonicalPath() + ")");
                System.out.println("  Cannot read " + skeleton.getCanonicalPath());
                return doc;
            }
            br = new BufferedReader(new FileReader(skeleton));
            javax.xml.parsers.DocumentBuilderFactory _dbf = 
                javax.xml.parsers.DocumentBuilderFactory.newInstance();
            _dbf.setNamespaceAware(true);
            javax.xml.parsers.DocumentBuilder _db = _dbf.newDocumentBuilder();
            DOMImplementation _di = _db.getDOMImplementation();
            org.w3c.dom.ls.DOMImplementationLS _dils = 
                (org.w3c.dom.ls.DOMImplementationLS) _di.getFeature("LS", "3.0");
            org.w3c.dom.ls.LSInput _lsi = _dils.createLSInput();
            _lsi.setCharacterStream(br);
            org.w3c.dom.ls.LSParser _lsp = 
                _dils.createLSParser(org.w3c.dom.ls.DOMImplementationLS.MODE_SYNCHRONOUS,
                                     "http://www.w3.org/2001/XMLSchema");
            doc = _lsp.parse(_lsi);
            if (br != null) {
                try {
                    br.close();
                    br = null;
                }
                catch (Exception ioe) {
                    ioe.printStackTrace();
                }
            }
        }
        catch (Exception ex) {
            doc = null;
            System.out.println("Cannot load a skeleton file for " + namespaceTerm);
            ex.printStackTrace();
            try {
                if (br != null) {
                    br.close();
                }
            }
            catch (Exception ioe2) {
                ioe2.printStackTrace();
            }
        }
        if (doc != null) {
            System.out.println("INFO in DocGen.loadXsdSkeletonFile("
                               + kb.name + ", "
                               + namespaceTerm + ", "
                               + indir + ")");
            System.out.println("  Skeleton file loaded for " + namespaceTerm);
        }
        return doc;
    }

    /** *************************************************************
     * 
     */
    private String getContentRegexPattern(KB kb, String term) {
        String pattern = null;
        try {
            pattern = kb.getFirstTermViaPredicateSubsumption("contentRegexPattern",
                                                             1,
                                                             term,
                                                             2,
                                                             false);
            if (StringUtil.emptyString(pattern)) {
                List specs = getFirstSpecificTerms(kb, term);
                specs.addAll(getDatatypeTerms(kb, term, 1));
                String inst = null;
                for (int i = 0; i < specs.size(); i++) {
                    inst = (String) specs.get(i);
                    pattern = getContentRegexPattern(kb, inst);
                    if (StringUtil.isNonEmptyString(pattern)) {
                        break;
                    }
                }
            }
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
        return pattern;
    }

    /** *************************************************************
     * 
     */
    private ArrayList getSubordinateXmlElementTerms(KB kb, String term, int targetArgnum) {
        ArrayList result = null;
        try {
            int idxArgnum = ((targetArgnum == 2) ? 1 : 2);
            result = kb.getTermsViaPredicateSubsumption("subordinateXmlElement",
                                                        idxArgnum,
                                                        term,
                                                        targetArgnum,
                                                        true);
            SetUtil.removeDuplicates(result);

            Collections.sort(result);
        }
        catch (Exception ex) {
            ex.printStackTrace();
            result = new ArrayList();
        }
        /*
          System.out.println("EXIT DocGen.getSubordinateXmlElementTerms("
          + kb.name + ", "
          + term + ", "
          + targetArgnum + ")");
          System.out.println("  ==> [" + result.size() + " terms]");
        */
        return result;
    }

    /** *************************************************************
     * 
     */
    private ArrayList getSubordinateXmlAttributeTerms(KB kb, String term, int targetArgnum) {
        ArrayList result = null;
        try {
            int idxArgnum = ((targetArgnum == 2) ? 1 : 2);
            result = kb.getTermsViaPredicateSubsumption("subordinateXmlAttribute",
                                                        idxArgnum,
                                                        term,
                                                        targetArgnum,
                                                        true);
            SetUtil.removeDuplicates(result);
        }
        catch (Exception ex) {
            ex.printStackTrace();
            result = new ArrayList();
        }
        /*
          System.out.println("EXIT DocGen.getSubordinateXmlAttributeTerms("
          + kb.name + ", "
          + term + ", "
          + targetArgnum + ")");
          System.out.println("  ==> [" + result.size() + " terms]");
        */
        return result;
    }

    /** *************************************************************
     * Sorts stringList in place based on explicit numeric ordinal
     * values obtained from assertions made with the predicate
     * arg1SortOrdinal.  Terms lacking such assertions will occur at
     * the end of the sorted list.  If no term in the list has a
     * relevant arg1SortOrdinal assertion, the list will not be
     * sorted.
     *
     * @param kb The KB from which to obtain the presentation names
     *
     * @param namespaceTerm A SUO-KIF term denoting a namespace
     *
     * @param arg2Term The term to which all of the arg1 values to be
     * sorted are related by some predicate, arg0
     *
     * @param arg1OrdFormulae An optional List of arg1SortOrdinal
     * formulae, which the method will try to compute if one is not
     * passed in
     *
     * @param stringList The List of Strings to be sorted.
     *
     * @return void
     *
     */
    protected void sortByArg1Ordinal(KB kb, 
                                     String namespaceTerm, 
                                     String arg2Term, 
                                     List arg1OrdFormulae,
                                     List stringList) {
        try {
            if (!SetUtil.isEmpty(stringList)) {
                List formulae = arg1OrdFormulae;
                if (SetUtil.isEmpty(formulae)) {
                    formulae = kb.askWithRestriction(0, "arg1SortOrdinal", 3, arg2Term);
                }
                if (!SetUtil.isEmpty(formulae)) {
                    Map fmap = new HashMap();
                    Formula f = null;
                    for (Iterator it = formulae.iterator(); it.hasNext();) {
                        f = (Formula) it.next();
                        fmap.put(f.getArgument(2), f);
                    }
                    List<String[]> sortable = new ArrayList<String[]>();
                    String[] pair = null;
                    String term = null;
                    String ord = null;
                    boolean sortValueFound = false;
                    for (Iterator it2 = stringList.iterator(); it2.hasNext();) {
                        sortValueFound = false;
                        term = (String) it2.next();
                        pair = new String[2];
                        pair[0] = term;
                        pair[1] = String.valueOf(Integer.MAX_VALUE);
                        f = (Formula) fmap.get(term);
                        if (f != null) {
                            ord = f.getArgument(4);
                            if (!ord.matches(".*\\D.*")) {

                                // If ord contains no non-digit chars
                                // ...
                                pair[1] = ord;
                                sortValueFound = true;
                            }
                        }
                        sortable.add(pair);
                        if (!sortValueFound) {
                            System.out.println("Warning: no ordinal sort value found for the "
                                               + arg2Term + ", "
                                               + term);
                        }
                    }
                    Comparator comp = new Comparator() {
                            public int compare(Object o1, Object o2) {
                                Integer ord1 = Integer.decode(((String[]) o1)[1]);
                                Integer ord2 = Integer.decode(((String[]) o2)[1]);
                                return ord1.compareTo(ord2);
                            }
                        };
                    String msg = null;
                    try {
                        Collections.sort(sortable, comp);
                    }
                    catch (Exception ex1) {
                        msg = ex1.getMessage();
                        System.out.println(msg);
                        ex1.printStackTrace();
                    }
                    if (StringUtil.emptyString(msg) && (sortable.size() == stringList.size())) {
                        stringList.clear();
                        for (Iterator it3 = sortable.iterator(); it3.hasNext();) {
                            pair = (String[]) it3.next();
                            stringList.add(pair[0]);
                        }
                    }
                }
            }
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
        return;
    }

    /** *************************************************************
     * Returns an xs:documentation Element, or null if term has no
     * documentation.
     * 
     */
    private Element makeDocumentationElement(KB kb, 
                                             Document doc, 
                                             String defaultNamespace,
                                             String namespace, 
                                             String term, 
                                             List path) {
        Element elem = null;
        try {
            List newPath = new ArrayList();
            newPath.addAll(path);
            if (!newPath.contains(term)) {
                newPath.add(0, term);
            }
            if (!newPath.contains(namespace)) {
                newPath.add(namespace);
            }
            if (!newPath.contains(defaultNamespace)) {
                newPath.add(defaultNamespace);
            }

            // System.out.println("path == " + newPath);

            String docustr = getContextualDocumentation(kb, term, newPath);
            if (StringUtil.isNonEmptyString(docustr)) {
                docustr = removeLinkableNamespacePrefixes(kb, docustr);
                docustr = processDocString(kb, "", namespace, docustr, true, true);
                elem = doc.createElementNS("http://www.w3.org/2001/XMLSchema",
                                           "xs:documentation");
                elem.setTextContent(docustr);
            }
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
        return elem;
    }

    /** *************************************************************
     * Returns an xs:annotation Element, or null if term has no
     * documentation or comments.
     *
     */
    private Element makeAnnotationElement(KB kb, 
                                          Document doc, 
                                          String defaultNamespace,
                                          String namespace, 
                                          String term, 
                                          List path) {
        Element elem = null;
        try {
            List newPath = new ArrayList();
            newPath.addAll(path);
            if (!newPath.contains(term)) {
                newPath.add(0, term);
            }
            Element docuElem = makeDocumentationElement(kb, doc, defaultNamespace, namespace, term, newPath);
            if (docuElem != null) {
                elem = doc.createElementNS("http://www.w3.org/2001/XMLSchema",
                                           "xs:annotation");
                elem.appendChild(docuElem);
            }
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
        return elem;
    }

    /** ***************************************************************
     */
    private String getCardinality(KB kb, String cardtype, String term, String context) {
        String val = "";
        try {
            String pred = "hasCardinality";
            if (cardtype.matches(".*(?i)min.*")) {
                pred = "hasMinCardinality";
            }
            else if (cardtype.matches(".*(?i)max.*")) {
                pred = "hasMaxCardinality";
            }
            else if (cardtype.matches(".*(?i)exact.*")) {
                pred = "hasExactCardinality";
            }
            List cardForms = kb.askWithPredicateSubsumption(pred, 1, term);
            if (!cardForms.isEmpty()) {
                Formula f = (Formula) cardForms.get(0);
                // if (context.equals("") || context.equals(f.getArgument(1))) {
                // val = f.getArgument(3);
                // }
                val = f.getArgument(2);
            }
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
        return val;
    }

    /** ***************************************************************
     */
    private void setElementCardinality(KB kb, Element elem, String term, List context) {
        /*
          System.out.println("ENTER DocGen.setElementCardinality("
          + kb.name + ", "
          + elem.getNodeName() + ", "
          + term + ", "
          + context + ")");
        */
        try {
            // ignoring context for now, since each LocalInstance term
            // can have only one set of cardinaltiy values.
            String nodeName = elem.getNodeName();
            String exact = getCardinality(kb, "exact", term, "");

            // System.out.println("  exact == " + exact);

            boolean isExact = StringUtil.isNonEmptyString(exact);
            String min = (isExact
                          ? exact
                          : getCardinality(kb, "min", term, ""));

            // System.out.println("  min == " + min);

            String max = (isExact
                          ? exact
                          : getCardinality(kb, "max", term, ""));

            // System.out.println("  max == " + max);

            boolean isMin = StringUtil.isNonEmptyString(min);
            boolean isMax = StringUtil.isNonEmptyString(max);

            if (nodeName.matches(".*element.*") 
                || nodeName.matches(".*choice.*")
                || nodeName.matches(".*sequence.*")) {
                if (!isMin) { min = "0"; isMin = true; }
                if (isMin && !min.equals("1")) {
                    elem.setAttribute("minOccurs", min);
                }

                // System.out.println("  min == " + min);

                if (!isMax) { max = "unbounded"; isMax = true; };
                if (isMax && !max.equals("1")) {
                    elem.setAttribute("maxOccurs", max);
                }

                // System.out.println("  max == " + max);

            }
            else if (nodeName.matches(".*attribute.*")) {
                if (isExact || (isMin && !min.equals("0"))) {
                    elem.setAttribute("use", "required");
                }
                else {
                    elem.setAttribute("use", "optional");
                }
            }
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }

        /*
          System.out.println("EXIT DocGen.setElementCardinality("
          + kb.name + ", "
          + elem.getNodeName() + ", "
          + term + ", "
          + context + ")");
        */

        return;
    }

    /** *************************************************************
     * 
     */
    private Element makeElement(Document doc, String name) {
        Element elem = null;
        try {
            elem = doc.createElementNS("http://www.w3.org/2001/XMLSchema",
                                       name);
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
        return elem;
    }

    /** *************************************************************
     * 
     */
    private Element makeElementElement(KB kb, 
                                       Document doc, 
                                       Element parentElement,
                                       String defaultNamespace,
                                       String namespace, 
                                       String term,
                                       List path) {
        Element elem = null;
        try {
            List newPath = new ArrayList();
            newPath.addAll(path);
            if (!newPath.contains(term)) {
                newPath.add(0, term);
            }
            elem = doc.createElementNS("http://www.w3.org/2001/XMLSchema",
                                       "xs:element");
            elem.setAttribute("name", getTermPresentationName(kb, namespace, term));
            if (!parentElement.getNodeName().matches(".*schema.*")) {
                String kifType = null;
                String dtype = getFirstDatatype(kb, term);
                if (!StringUtil.isNonEmptyString(dtype)) {
                    dtype = getFirstGeneralTerm(kb, term);
                }
                if (StringUtil.isNonEmptyString(dtype)) {
                    dtype = substituteXsdDataType(kb, namespace, dtype);
                    dtype = StringUtil.kifToW3c(dtype);
                    elem.setAttribute("type", dtype);
                }
                setElementCardinality(kb, elem, term, path);
            }
            addXsdNodeAnnotationChildNode(kb, 
                                          doc, 
                                          elem, 
                                          defaultNamespace, 
                                          namespace, 
                                          term, 
                                          newPath);
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
        return elem;
    }

    /** *************************************************************
     * 
     */
    private Element makeComplexTypeElement(KB kb, 
                                           Document doc, 
                                           Element parentElem,
                                           String defaultNamespace,
                                           String namespace, 
                                           String term,
                                           List path) {
        Element elem = null;
        try {
            List newPath = new ArrayList();
            newPath.addAll(path);
            if (!newPath.contains(term)) {
                newPath.add(0, term);
            }
            elem = doc.createElementNS("http://www.w3.org/2001/XMLSchema",
                                       "xs:complexType");
            if (parentElem.getNodeName().matches(".*schema.*")) {
                elem.setAttribute("name", getTermPresentationName(kb, namespace, term));
                addXsdNodeAnnotationChildNode(kb, 
                                              doc, 
                                              elem, 
                                              defaultNamespace, 
                                              namespace, 
                                              term, 
                                              newPath);
            }
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
        return elem;
    }

    /** *************************************************************
     * 
     */
    private Element makeSimpleTypeElement(KB kb, 
                                          Document doc, 
                                          Element parentElem,
                                          String defaultNamespace,
                                          String namespace, 
                                          String term,
                                          List path) {
        Element elem = null;
        try {
            List newPath = new ArrayList();
            newPath.addAll(path);
            if (!newPath.contains(term)) {
                newPath.add(0, term);
            }
            elem = doc.createElementNS("http://www.w3.org/2001/XMLSchema",
                                       "xs:simpleType");
            if (parentElem.getNodeName().matches(".*schema.*")) {
                elem.setAttribute("name", getTermPresentationName(kb, namespace, term));
                addXsdNodeAnnotationChildNode(kb, 
                                              doc, 
                                              elem, 
                                              defaultNamespace, 
                                              namespace, 
                                              term, 
                                              newPath);
            }
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
        return elem;
    }

    /** *************************************************************
     * 
     */
    private Element makeEnumerationElement(KB kb, 
                                           Document doc, 
                                           Element parentElem,
                                           String defaultNamespace,
                                           String namespace, 
                                           String term,
                                           List path) {
        Element elem = null;
        try {
            List newPath = new ArrayList();
            newPath.addAll(path);
            if (!newPath.contains(term)) {
                newPath.add(0, term);
            }
            elem = doc.createElementNS("http://www.w3.org/2001/XMLSchema",
                                       "xs:enumeration");
            elem.setAttribute("value", getTermPresentationName(kb, namespace, term));
            addXsdNodeAnnotationChildNode(kb, 
                                          doc, 
                                          elem, 
                                          defaultNamespace, 
                                          namespace, 
                                          term, 
                                          newPath);
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
        return elem;
    }

    /** *************************************************************
     * 
     */
    private Element makeSequenceElement(Document doc) {
        Element elem = null;
        try {
            elem = doc.createElementNS("http://www.w3.org/2001/XMLSchema",
                                       "xs:sequence");
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
        return elem;
    }

    /** *************************************************************
     * 
     */
    private Element makeChoiceElement(KB kb, 
                                      Document doc,
                                      String term,
                                      List path) {
        List newPath = new ArrayList();
        newPath.addAll(path);
        if (!newPath.contains(term)) {
            newPath.add(0, term);
        }
        Element elem = null;
        try {
            elem = doc.createElementNS("http://www.w3.org/2001/XMLSchema",
                                       "xs:choice");
            setElementCardinality(kb, elem, term, newPath);
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
        return elem;
    }

    /** *************************************************************
     * 
     */
    private Element makeAttributeElement(KB kb, 
                                         Document doc,
                                         String defaultNamespace,
                                         String namespace, 
                                         String term,
                                         List path) {
        Element elem = null;
        try {
            List newPath = new ArrayList();
            newPath.addAll(path);
            if (!newPath.contains(term)) {
                newPath.add(0, term);
            }
            elem = doc.createElementNS("http://www.w3.org/2001/XMLSchema",
                                       "xs:attribute");
            elem.setAttribute("name", getTermPresentationName(kb, namespace, term));
            String dtype = getFirstDatatype(kb, term);
            if (!StringUtil.isNonEmptyString(dtype)) {
                dtype = getFirstGeneralTerm(kb, term);
            }
            if (StringUtil.isNonEmptyString(dtype)) {
                dtype = substituteXsdDataType(kb, namespace, dtype);
                dtype = StringUtil.kifToW3c(dtype);
            }
            else {
                dtype = "xs:string";
            }
            elem.setAttribute("type", dtype);
            setElementCardinality(kb, elem, term, path);
            addXsdNodeAnnotationChildNode(kb, 
                                          doc, 
                                          elem, 
                                          defaultNamespace, 
                                          namespace, 
                                          term, 
                                          newPath);
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
        return elem;
    }

    /** *************************************************************
     * 
     */
    private Element makeExtensionElement(KB kb, 
                                         Document doc, 
                                         Element parentElem,
                                         String defaultNamespace,
                                         String namespace, 
                                         String term,
                                         List path) {
        Element elem = null;
        try {
            List newPath = new ArrayList();
            newPath.addAll(path);
            if (!newPath.contains(term)) {
                newPath.add(0, term);
            }
            elem = doc.createElementNS("http://www.w3.org/2001/XMLSchema",
                                       "xs:extension");
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
        return elem;
    }

    /** *************************************************************
     * 
     * Adds xs:extension child node to elem, with xs:simpleContent or
     * xs:complexContent wrapper.
     *
     */
    private void addXsdNodeExtensionChildNode(KB kb, 
                                              Document doc, 
                                              Element parentElem, 
                                              String baseTerm,
                                              String defaultNamespace,
                                              String namespace, 
                                              String term,
                                              List path) {
        try {
            if (StringUtil.isNonEmptyString(baseTerm)) {
                List newPath = new ArrayList();
                newPath.addAll(path);
                if (!newPath.contains(baseTerm)) {
                    newPath.add(0, baseTerm);
                }
                if (!newPath.contains(term)) {
                    newPath.add(0, term);
                }
                String baseName = substituteXsdDataType(kb, namespace, baseTerm);
                baseName = StringUtil.kifToW3c(baseName);
                Element extensionElem = makeExtensionElement(kb, 
                                                             doc, 
                                                             parentElem,
                                                             defaultNamespace,
                                                             namespace, 
                                                             term,
                                                             path);
                extensionElem.setAttribute("base", baseName);

                addXsdNodeElementChildNodes(kb, 
                                            doc, 
                                            extensionElem, 
                                            defaultNamespace,
                                            namespace, 
                                            term,
                                            path);

                addXsdNodeAttributeChildNodes(kb, 
                                              doc, 
                                              extensionElem, 
                                              defaultNamespace,
                                              namespace, 
                                              term,
                                              path);

                String contentElemName = (hasComplexContent(extensionElem)
                                          ? "xs:complexContent"
                                          : "xs:simpleContent");
                Element contentElem = makeElement(doc, contentElemName);
                contentElem.appendChild(extensionElem);
                parentElem.appendChild(contentElem);
            }
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
        return;
    }

    /** *************************************************************
     * 
     * Adds one "layer" of child xs:element Elements to elem,
     * descending into xs:choice and xs:sequence subelements to add
     * their contents, too.
     *
     */
    private void addXsdNodeElementChildNodes(KB kb, 
                                             Document doc, 
                                             Element parentElem, 
                                             String defaultNamespace,
                                             String nsContext, 
                                             String term,
                                             List path) {
        try {
            List newPath = new ArrayList();
            newPath.addAll(path);
            if (!newPath.contains(term) && !isSkipNode(kb, term)) {
                newPath.add(0, term);
            }
            if (parentElem.getNodeName().matches(".*simpleType.*")) {
                List types = getSyntacticUnionTerms(kb, term, 2);
                if (!types.isEmpty()) {
                    String typelist = "";
                    for (int i = 0; i < types.size(); i++) {
                        if (i > 0) { typelist += " "; }
                        String termName = StringUtil.kifToW3c((String) types.get(i));
                        typelist += termName;
                    }
                    Element _unionElem = makeElement(doc, "xs:union");
                    _unionElem.setAttribute("memberTypes", typelist);
                    parentElem.appendChild(_unionElem);
                }
            }
            else {
                List valueSetTerms = kb.getTermsViaPredicateSubsumption("element",
                                                                        2,
                                                                        term,
                                                                        1,
                                                                        true);
                if (!valueSetTerms.isEmpty()) {
                    Element _restrictionElem = makeElement(doc, "xs:restriction");
                    String dtype = getFirstDatatype(kb, term);
                    if (StringUtil.isNonEmptyString(dtype)) {
                        dtype = substituteXsdDataType(kb, nsContext, dtype);
                        dtype = StringUtil.kifToW3c(dtype);
                    }
                    else {
                        dtype = "xs:string";
                    }
                    _restrictionElem.setAttribute("base", dtype);
                    String vsTerm = null;
                    for (int i = 0; i < valueSetTerms.size(); i++) {
                        vsTerm = (String) valueSetTerms.get(i);
                        _restrictionElem.appendChild(makeEnumerationElement(kb, 
                                                                            doc, 
                                                                            parentElem,
                                                                            defaultNamespace,
                                                                            nsContext, 
                                                                            vsTerm,
                                                                            newPath));
                    }
                    parentElem.appendChild(_restrictionElem);
                }
                List subElementTerms = getSubordinateXmlElementTerms(kb, term, 1);
                String subTerm = null;
                for (int i = 0; i < subElementTerms.size(); i++) {
                    subTerm = (String) subElementTerms.get(i);
                    Element newElem = null;
                    if (isSkipNode(kb, subTerm)) {  

                        // XmlChoiceElement or XmlSequenceElement
                        if (isInstanceOf(kb, subTerm, "XmlChoiceElement")) {
                            newElem = makeChoiceElement(kb, doc, subTerm, newPath);
                        }
                        else {
                            newElem = makeSequenceElement(doc);
                        }
                        addXsdNodeElementChildNodes(kb, 
                                                    doc, 
                                                    newElem, 
                                                    defaultNamespace, 
                                                    nsContext, 
                                                    subTerm, 
                                                    newPath);
                    }
                    else {
                        newElem = makeElementElement(kb, 
                                                     doc, 
                                                     parentElem,
                                                     defaultNamespace, 
                                                     nsContext, 
                                                     subTerm,
                                                     newPath);
                    }
                    parentElem.appendChild(newElem);
                }
            }
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
        return;
    }

    /** *************************************************************
     * 
     * Adds xs:attribute child nodes to elem.
     *
     */
    private void addXsdNodeAttributeChildNodes(KB kb, 
                                               Document doc, 
                                               Element parentElem, 
                                               String defaultNamespace,
                                               String nsContext, 
                                               String term,
                                               List path) {
        try {
            List newPath = new ArrayList();
            newPath.addAll(path);
            if (!newPath.contains(term)) {
                newPath.add(0, term);
            }
            List subAttrTerms = getSubordinateXmlAttributeTerms(kb, term, 1);
            String subTerm = null;
            for (int i = 0; i < subAttrTerms.size(); i++) {
                subTerm = (String) subAttrTerms.get(i);
                Element newElem = makeAttributeElement(kb, 
                                                       doc, 
                                                       defaultNamespace, 
                                                       nsContext, 
                                                       subTerm,
                                                       newPath);
                if (newElem != null) {
                    parentElem.appendChild(newElem);
                }
            }
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
        return;
    }

    /** *************************************************************
     * 
     * Adds an xs:pattern child node to parentElem, if possible.
     *
     */
    private void addXsdNodePatternChildNode(KB kb, 
                                            Document doc, 
                                            Element parentElem, 
                                            String defaultNamespace,
                                            String namespace, 
                                            String term,
                                            List path) {
        try {
            String pattern = getContentRegexPattern(kb, term);
            if (StringUtil.isNonEmptyString(pattern)) {
                pattern = StringUtil.removeEnclosingQuotes(pattern);
                List newPath = new ArrayList();
                newPath.addAll(path);
                if (!newPath.contains(term)) {
                    newPath.add(0, term);
                }
                Element patternElem = makeElement(doc, "xs:pattern");
                patternElem.setAttribute("value", pattern);
                parentElem.appendChild(patternElem);
            }
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
        return;
    }

    /** *************************************************************
     * 
     * Adds an xs:annotation child node to elem, if possible.
     *
     */
    private void addXsdNodeAnnotationChildNode(KB kb, 
                                               Document doc, 
                                               Element parentElem, 
                                               String defaultNamespace,
                                               String namespace, 
                                               String term,
                                               List path) {
        try {
            List newPath = new ArrayList();
            newPath.addAll(path);
            if (!newPath.contains(term)) {
                newPath.add(0, term);
            }
            Element annoElem = makeAnnotationElement(kb, 
                                                     doc, 
                                                     defaultNamespace, 
                                                     namespace, 
                                                     term, 
                                                     newPath);
            if (annoElem != null) {
                parentElem.appendChild(annoElem);
            }
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
        return;
    }

    /** *************************************************************
     * Collects all top-level terms for each namespace-specific XSD
     * file.  Returns a Map in which the keys are KIF namespace terms,
     * and the values are Lists of all top-level XSD terms in the
     * indexed namespaces.
     *
     */
    protected Map computeTopLevelTermsByNamespace(KB kb, String ontology) {

        long t1 = System.currentTimeMillis();
        System.out.println("ENTER DocGen.computeTopLevelTermsByNamespace("
                           + kb.name + ", "
                           + ontology + ")");

        Map tops = new HashMap();
        Set done = new HashSet();
        try {
            List focalTerms = computeFocalTerms(kb, ontology);

            // System.out.println("  focalTerms == " + focalTerms);

            List working = new LinkedList(focalTerms);
            Set accumulator = new HashSet();
            String term = null;
            Iterator it = null;
            for (it = working.iterator(); it.hasNext();) {
                term = (String) it.next();
                accumulator.add(term);
                accumulator.addAll(kb.getAllInstancesWithPredicateSubsumption(term));
            }

            // System.out.println("  accumulator == " + accumulator);

            // String dtype = null;
            String namespace = null;
            while (!accumulator.isEmpty()) {
                // System.out.print("  " + accumulator.size() + " new terms to process ");
                working.clear();
                working.addAll(accumulator);
                accumulator.clear();

                for (it = working.iterator(); it.hasNext();) {
                    term = (String) it.next();
                    if (!done.contains(term)) {

                        if (!isSkipNode(kb, term)) {

                            namespace = getTermNamespace(kb, term);

                            // System.out.println("  term == " + term);

                            if (StringUtil.isLocalTermReference(term)) {

                                String dtype = getFirstDatatype(kb, term);
                                if (StringUtil.isNonEmptyString(dtype)) {
                                    namespace = getTermNamespace(kb, dtype);
                                    if (StringUtil.isNonEmptyString(namespace)) {

                                        // System.out.println("  dtype == " + dtype);

                                        accumulator.add(dtype);
                                    }
                                }

                                String pattern = getContentRegexPattern(kb, term);
                                if (StringUtil.isNonEmptyString(pattern)) {
                                    String io = getFirstGeneralTerm(kb, term);
                                    if (StringUtil.isNonEmptyString(io)) {
                                        namespace = getTermNamespace(kb, io);
                                        if (StringUtil.isNonEmptyString(namespace)) {

                                            // System.out.println("  io == " + io);

                                            accumulator.add(io);
                                        }
                                    }
                                }
                            }
                            else {
                                addToMapOfLists(tops, namespace, term);
                            }
                        }
                        accumulator.addAll(getSubordinateXmlElementTerms(kb, term, 1));
                        accumulator.addAll(getSubordinateXmlAttributeTerms(kb, term, 1));
                        accumulator.addAll(getSyntacticExtensionTerms(kb, term, 2, false));
                        accumulator.addAll(getSyntacticUnionTerms(kb, term, 2));
                        accumulator.addAll(getSyntacticCompositeTerms(kb, term, 1));
                        done.add(term);
                        if ((done.size() % 100) == 0) {
                            System.out.print(".");
                        }
                    }
                }
                System.out.println("x");
            }
            // System.out.print("  Sorting terms by presentation name ");
            for (it = tops.keySet().iterator(); it.hasNext();) {

                // A namespace.
                term = (String) it.next();

                // A list of terms in namespace.
                working = (List) tops.get(term);
                sortByPresentationName(kb, namespace, working);
                // System.out.print(".");
            }
            // System.out.println(" done");
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }

        System.out.println("EXIT DocGen.computeTopLevelTermsByNamespace("
                           + kb.name + ", "
                           + ontology + ")");
        System.out.println("  > [" + tops.size() + " keys computed]");
        System.out.println("  > " 
                           + ((System.currentTimeMillis() - t1) / 1000.0)
                           + " seconds elapsed time");

        return tops;
    }

    /** *************************************************************
     * Adds val to the List indexed by key in the Map m.
     *
     */
    private void addToMapOfLists(Map m, String key, String val) {
        try {
            List valList = (List) m.get(key);
            if (valList == null) {
                valList = new ArrayList();
                m.put(key, valList);
            }
            if (!valList.contains(val)) {
                valList.add(val);
            }
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
        return;
    }

    /** *************************************************************
     * 
     */
    public void writeXsdFileForNamespace(KB kb, 
                                         String namespace, 
                                         List topLevelTerms,
                                         String outputDirName) {
        try {            
            String nsName = stripNamespacePrefix(kb, namespace);
            String nsTerm = ("ns" + StringUtil.getKifNamespaceDelimiter() + nsName);
            List termsInNamespace = getTermsInNamespace(kb, nsTerm, true);

            if (!topLevelTerms.isEmpty()) {

                List filteredTerms = new ArrayList();
                String ft = null;
                String nativeType = null;
                for (int i = 0; i < topLevelTerms.size(); i++) {
                    ft = (String) topLevelTerms.get(i);
                    nativeType = substituteXsdDataType(kb, nsTerm, ft);
                    if (!nativeType.startsWith("xs:")
                        || nativeType.equals("xs:IDREF")
                        || nativeType.equals("xs:ID")) {
                        filteredTerms.add(ft);
                    }
                }
                topLevelTerms = filteredTerms;

                // sortByPresentationName(kb, nsTerm, topLevelTerms);

                System.out.println("topLevelTerms == " + topLevelTerms);

                File outputDir = new File(outputDirName);
                if (!outputDir.isDirectory() || !outputDir.canWrite()) {
                    System.out.println("Cannot write to " + outputDir.getCanonicalPath());
                    return;
                }
                System.out.println("  outputDir == " + outputDir.getCanonicalPath());

                Document _doc = createXsdDocument(kb, nsTerm);
                if (_doc == null) {
                    System.out.println("Cannot create a Document for " + namespace);
                    return;
                }
                System.out.println("Document created for " + namespace);

                Element _docelem = _doc.getDocumentElement();
                String filename = getXsdFileName(kb, nsTerm);
                File outfile = new File(outputDir, filename);
                PrintWriter pw = new PrintWriter(new FileWriter(outfile));

                Iterator it = topLevelTerms.iterator();
                String term = null;
                String termName = null;
                String nsPrefix = null;
                while (it.hasNext()) {
                    term = (String) it.next();
                    termName = getTermPresentationName(kb, nsTerm, term);
                    ArrayList nsList = new ArrayList();
                    nsList.add(nsTerm);
                    Element _simpleType = makeSimpleTypeElement(kb, 
                                                                _doc, 
                                                                _docelem,
                                                                nsTerm,
                                                                nsTerm, 
                                                                term,
                                                                nsList);
                    List specs = getFirstSpecificTerms(kb, term);
                    String datatype = "xs:string";
                    String xsdType = null;
                    if (specs.isEmpty()) {
                        xsdType = substituteXsdDataType(kb, nsTerm, term);
                        if (!xsdType.equals(StringUtil.kifToW3c(term))) {
                            datatype = xsdType;
                        }
                    }
                    else {
                        String specVal = null;
                        String dtype = null;
                        boolean specHasDataType = false;
                        for (int i = 0; i < specs.size(); i++) {
                            specVal = (String) specs.get(i);
                            dtype = getFirstDatatype(kb, specVal);
                            if (StringUtil.isNonEmptyString(dtype)) {
                                specHasDataType = true;
                                xsdType = substituteXsdDataType(kb, nsTerm, dtype);
                                dtype = StringUtil.kifToW3c(dtype);
                                if (!dtype.equals(xsdType)) {
                                    datatype = xsdType;
                                    break;
                                }
                            }
                        }
                        if (!specHasDataType) {
                            xsdType = substituteXsdDataType(kb, nsTerm, term);
                            if (!xsdType.equals(StringUtil.kifToW3c(term))) {
                                datatype = xsdType;
                            }
                        }
                    }
                    Element _restriction = makeElement(_doc, "xs:restriction");
                    _restriction.setAttribute("base", datatype);
                    addXsdNodePatternChildNode(kb, 
                                               _doc, 
                                               _restriction, 
                                               nsTerm,
                                               nsTerm, 
                                               term,
                                               nsList);

                    List members = getAvsTypeMembers(kb, term);
                    List arg1OrdFormulae = kb.askWithRestriction(0, "arg1SortOrdinal", 3, term);
                    if ((arg1OrdFormulae == null) || arg1OrdFormulae.isEmpty()) {
                        sortByPresentationName(kb, nsTerm, members);
                    }
                    else {
                        sortByArg1Ordinal(kb, nsTerm, term, arg1OrdFormulae, members);
                    }

                    // System.out.println("members == " + members);

                    if (!members.isEmpty()) {
                        Iterator mIt = members.iterator();
                        String m = null;
                        String mName = null;
                        while (mIt.hasNext()) {
                            m = (String) mIt.next();
                            mName = getTermPresentationName(kb, nsTerm, m);
                            // mName = stripNamespacePrefix(kb, mName);
                            Element _enumeration = makeElement(_doc, "xs:enumeration");
                            if (getCodedIdentifiers(kb).contains(m)) {
                                // mName = stripNamespacePrefix(kb, m);
                                addXsdNodeAnnotationChildNode(kb, 
                                                              _doc, 
                                                              _enumeration, 
                                                              nsTerm, 
                                                              nsTerm, 
                                                              m,
                                                              nsList);
                            }
                            _enumeration.setAttribute("value", mName);
                            _restriction.appendChild(_enumeration);
                        }
                    }
                    _simpleType.appendChild(_restriction);
                    _docelem.appendChild(_simpleType);
                }
                printXmlNodeTree(_docelem, 0, pw);
                if (pw != null) {
                    try {
                        pw.close();
                    }
                    catch (Exception pwex) {
                        pwex.printStackTrace();
                    }
                }
            }
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
        return;
    }

    /** *************************************************************
     * 
     */
    private String getXsdFileName(KB kb, String namespaceTerm) {
        String filename = "";
        String suffix = ".xsd";
        try {
            String nsName = namespaceTerm;
            List<String> nsprefixes = Arrays.asList(("ns" + StringUtil.getKifNamespaceDelimiter()),
                                                    ("ns" + StringUtil.getW3cNamespaceDelimiter()));
            for (String pref : nsprefixes) {
                if (namespaceTerm.startsWith(pref)) {
                    nsName = namespaceTerm.substring(pref.length());
                    break;
                }
            }
            filename = nsName + suffix;
            String fn1 = kb.getFirstTermViaAskWithRestriction(0, 
                                                              "xsdFileName", 
                                                              2, 
                                                              namespaceTerm,
                                                              1);
            fn1 = StringUtil.removeEnclosingQuotes(fn1);
            if (StringUtil.isNonEmptyString(fn1)) {
                if (!fn1.endsWith(suffix)) {
                    fn1 += suffix;
                }
                filename = fn1;
            }
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
        return filename;
    }

    /** *************************************************************
     * 
     */
    public void writeXsdFiles(KB kb, 
                              String ontology, 
                              String namespace,
                              String outputDirName) {

        System.out.println("ENTER DocGen.writeXsdFiles("
                           + kb.name + ", "
                           + ontology + ", "
                           + namespace + ", "
                           + outputDirName + ")");

        try {
            String defaultNamespace = namespace;
            if (StringUtil.emptyString(defaultNamespace)) {
                defaultNamespace = kb.getFirstTermViaAskWithRestriction(0,
                                                                        "docGenDefaultNamespace",
                                                                        1,
                                                                        ontology,
                                                                        2);
                if (StringUtil.emptyString(defaultNamespace)) {
                    throw new Exception("Cannot find the default namespace for " + ontology);
                }
            }
            defaultNamespace = toKifNamespace(kb, namespace);
            System.out.println("  defaultNamespace == " + defaultNamespace);

            String outdirStr = outputDirName;
            File outdir = new File(outdirStr);
            if (!outdir.isDirectory() || !outdir.canWrite()) {
                throw new Exception("Cannot write to " + outdir.getCanonicalPath());
            }
            System.out.println("  outdir == " + outdir.getCanonicalPath());

            List namespaces = getNamespaces(kb, ontology, true);

            List focalTerms = (List) computeFocalTerms(kb, ontology);
            Map refTermsNsMap = mapFocalTermsByNamespace(kb, focalTerms);
            Map tops = getTopLevelTerms();
            if (tops == null) {
                tops = computeTopLevelTermsByNamespace(kb, ontology);
            }
            // if (getCoreTerms().isEmpty()) computeTermRelevance(kb, ontology);

            List refTermsForNS = null;
            String nsStr = null;
            String nsTerm = null;
            String nsName = null;
            for (Iterator it = namespaces.iterator(); it.hasNext();) {
                nsStr = (String) it.next();
                nsTerm = toKifNamespace(kb, nsStr);

                System.out.println("Processing terms for " + nsTerm);

                Set reducedTops = new HashSet();
                List cachedTops = (List) tops.get(nsTerm);
                if (cachedTops != null) {
                    reducedTops.addAll(cachedTops);
                }
                List topsForNS = new ArrayList(reducedTops);
                sortByPresentationName(kb, nsTerm, topsForNS);

                // System.out.println("  tops == " + topsForNS);

                // Collections.sort(topsForNS, String.CASE_INSENSITIVE_ORDER);
                refTermsForNS = (List) refTermsNsMap.get(nsTerm);
                if ((refTermsForNS != null) && !refTermsForNS.isEmpty()) {
                    sortByPresentationName(kb, nsTerm, refTermsForNS);
                    // Collections.sort(cachedMsgTypes, String.CASE_INSENSITIVE_ORDER);
                    
                    int origTopsLen = topsForNS.size();
                    String topTerm = null;
                    for (int i = 0; i < origTopsLen; i++) {
                        topTerm = (String) topsForNS.remove(0);
                        if (!refTermsForNS.contains(topTerm)) {
                            topsForNS.add(topTerm);
                        }
                    }
                    topsForNS.addAll(refTermsForNS);
                }

                if (nsTerm.equals(getDefaultNamespace()) || nsTerm.matches(".*iso.*")) {

                    writeXsdFileForNamespace(kb, nsTerm, topsForNS, outputDirName);
                }
                else if (!topsForNS.isEmpty()) {
                    Document _doc = createXsdDocument(kb, nsTerm);
                    if (_doc == null) {
                        System.out.println("Cannot create a Document for " + nsTerm);
                        continue;
                    }
                    System.out.println("Document created for " + nsTerm);

                    Element _docelem = _doc.getDocumentElement();
                    String filename = getXsdFileName(kb, nsTerm);
                    File outfile = new File(outdir, filename);

                    PrintWriter pw = null;

                    try {

                        for (Iterator topIt = topsForNS.iterator(); topIt.hasNext();) {
                            String term = (String) topIt.next();
                            List path = new ArrayList();
                            path.add(0, term);

                            // System.out.println("term == " + term);

                            Element _elem = null;

                            if (isXsdComplexType(kb, term)) {
                                String focalInstanceClass = 
                                    kb.getFirstTermViaAskWithRestriction(0,
                                                                         "docGenFocalInstanceClass",
                                                                         1,
                                                                         ontology,
                                                                         2);
                                if (isInstanceOf(kb, term, focalInstanceClass)) {

                                    Element _msgElem = makeElementElement(kb, 
                                                                          _doc, 
                                                                          _docelem,
                                                                          defaultNamespace, 
                                                                          nsTerm, 
                                                                          term, 
                                                                          path);

                                    _elem = makeComplexTypeElement(kb, 
                                                                   _doc,
                                                                   _msgElem, 
                                                                   defaultNamespace,
                                                                   nsTerm,
                                                                   term,         
                                                                   path);

                                    addXsdNodeElementChildNodes(kb, 
                                                                _doc, 
                                                                _elem, 
                                                                defaultNamespace,
                                                                nsTerm, 
                                                                term, 
                                                                path);

                                    addXsdNodeAttributeChildNodes(kb, 
                                                                  _doc, 
                                                                  _elem, 
                                                                  defaultNamespace, 
                                                                  nsTerm, 
                                                                  term, 
                                                                  path);

                                    _msgElem.appendChild(_elem);
                                    _docelem.appendChild(_msgElem);
                                }
                                else {
                                    _elem = makeComplexTypeElement(kb, 
                                                                   _doc,
                                                                   _docelem, 
                                                                   defaultNamespace,
                                                                   nsTerm,
                                                                   term,         
                                                                   path);

                                    List extended = getSyntacticExtensionTerms(kb, term, 2, false);
                                    String baseTerm = null;
                                    if (!extended.isEmpty()) {
                                        baseTerm = (String) extended.get(0);
                                    }
                                    if (StringUtil.isNonEmptyString(baseTerm)) {
                                        addXsdNodeExtensionChildNode(kb, 
                                                                     _doc, 
                                                                     _elem, 
                                                                     baseTerm,
                                                                     defaultNamespace,
                                                                     nsTerm, 
                                                                     term,
                                                                     path);
                                    }
                                    else {
                                        addXsdNodeElementChildNodes(kb, 
                                                                    _doc, 
                                                                    _elem, 
                                                                    defaultNamespace,
                                                                    nsTerm, 
                                                                    term, 
                                                                    path);

                                        addXsdNodeAttributeChildNodes(kb, 
                                                                      _doc, 
                                                                      _elem, 
                                                                      defaultNamespace, 
                                                                      nsTerm, 
                                                                      term, 
                                                                      path);
                                    }
                                    _docelem.appendChild(_elem);
                                }
                            }
                            else if (isDataType(kb, term)) {
                                // xs:simpleType
                                _elem = makeSimpleTypeElement(kb, 
                                                              _doc, 
                                                              _docelem,
                                                              defaultNamespace,
                                                              nsTerm, 
                                                              term,
                                                              path);
                                List extended = getSyntacticExtensionTerms(kb, term, 2, false);
                                String baseTerm = null;
                                if (!extended.isEmpty()) {
                                    baseTerm = (String) extended.get(0);
                                }
                                if (StringUtil.isNonEmptyString(baseTerm)) {
                                    addXsdNodeExtensionChildNode(kb, 
                                                                 _doc, 
                                                                 _elem, 
                                                                 baseTerm,
                                                                 defaultNamespace,
                                                                 nsTerm, 
                                                                 term,
                                                                 path);
                                }
                                else {
                                    addXsdNodeElementChildNodes(kb, 
                                                                _doc, 
                                                                _elem, 
                                                                defaultNamespace,
                                                                nsTerm, 
                                                                term, 
                                                                path);

                                    addXsdNodeAttributeChildNodes(kb, 
                                                                  _doc, 
                                                                  _elem, 
                                                                  defaultNamespace, 
                                                                  nsTerm, 
                                                                  term, 
                                                                  path);
                                }
                                _docelem.appendChild(_elem);
                            }
                            else {
                                // xs:element
                                _elem = makeElementElement(kb, 
                                                           _doc, 
                                                           _docelem,
                                                           defaultNamespace, 
                                                           nsTerm, 
                                                           term, 
                                                           path);

                                addXsdNodeElementChildNodes(kb, 
                                                            _doc, 
                                                            _elem, 
                                                            defaultNamespace,
                                                            nsTerm, 
                                                            term, 
                                                            path);

                                addXsdNodeAttributeChildNodes(kb, 
                                                              _doc, 
                                                              _elem, 
                                                              defaultNamespace, 
                                                              nsTerm, 
                                                              term, 
                                                              path);

                                _docelem.appendChild(_elem);
                            }
                        }
                        pw = new PrintWriter(new FileWriter(outfile));
                        printXmlNodeTree(_docelem, 0, pw);
                    }
                    catch (Exception ex3) {
                        ex3.printStackTrace();
                    }
                    if (pw != null) {
                        try {
                            pw.close();
                        }
                        catch (Exception ioe) {
                            ioe.printStackTrace();
                        }
                    }
                }
            }
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }

        System.out.println("EXIT DocGen.writeXsdFiles("
                           + kb.name + ", "
                           + ontology + ", "
                           + namespace + ", "
                           + outputDirName + ")");

        return;
    }

    /** *************************************************************
     * Computes and returns a list of all terms in namespace, based
     * either on syntactic matching of each term in KB against
     * namespace, or on each term's occurrence in a statement formed
     * with the SUO-KIF predicate inNamespace.
     *
     * @param kb The KB from which terms and inNamespace statements
     * will be gathered
     *
     * @param namespace The SUO-KIF name of a namespace in kb
     *
     * @param useSemantics If true, only terms in inNamespace
     * statements will be gathered.  If false, only terms with a
     * prefix derived from namespace will be gathered.
     *
     * @return A List of SUO-KIF terms sorted in case-insensitive
     * alphanumeric order
     */
    private ArrayList getTermsInNamespace(KB kb, String namespace, boolean useSemantics) {
        ArrayList result = new ArrayList();
        try {
            String kifNs = toKifNamespace(kb, namespace);
            if (useSemantics) {
                List formulae = kb.askWithRestriction(0, "inNamespace", 2, kifNs);
                if (formulae != null) {
                    Set reduced = new HashSet();
                    Formula f = null;
                    for (Iterator it = formulae.iterator(); it.hasNext();) {
                        f = (Formula) it.next();
                        reduced.add(f.getArgument(1));
                    }
                    result.addAll(reduced);
                }
            }
            else {
                String term = null;
                for (Iterator it = kb.terms.iterator(); it.hasNext();) {
                    term = (String) it.next();
                    if (getTermNamespace(kb, term).equals(kifNs)) {
                        result.add(term);
                    }
                }
            }
            if (!result.isEmpty()) {
                Collections.sort(result, String.CASE_INSENSITIVE_ORDER);
            }
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
        return result;
    }

    /** *************************************************************
     * Computes and returns a list of all terms in one of the
     * namespaces explicitly identified for ontology via an
     * ontologyNamespace statement.
     *
     * @param kb The KB from which terms and ontologyNamespace
     * statements will be gathered
     *
     * @param ontology The ontology from which terms and namespaces
     * will be gathered
     *
     * @return A List of SUO-KIF terms sorted in case-insensitive
     * alphanumeric order
     */
    private ArrayList<String> getTermsInOntologyNamespaces(KB kb, String ontology) {
        ArrayList<String> result = new ArrayList<String>();
        try {
            List<String> namespaces = getOntologyNamespaces(kb, ontology);
            if (!namespaces.isEmpty()) {
                String term = null;
                for (Iterator it = kb.terms.iterator(); it.hasNext();) {
                    term = (String) it.next();
                    if (namespaces.contains(getTermNamespace(kb, term))) {
                        result.add(term);
                    }
                }
                if (!result.isEmpty()) {
                    Collections.sort(result, String.CASE_INSENSITIVE_ORDER);
                }
            }
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
        return result;
    }

    /** *************************************************************
     * 
     */
    private ArrayList<String> getAvsTypeMembers(KB kb, String avsType) {
        ArrayList<String> result = new ArrayList<String>();
        try {
            List<String> members = kb.getTermsViaPredicateSubsumption("element",
                                                                      2,
                                                                      avsType,
                                                                      1,
                                                                      false);
            if (members.isEmpty() && isSubclass(kb, avsType, "CodedIdentifier")) {
                members = kb.getTermsViaPredicateSubsumption("instance",
                                                             2,
                                                             avsType,
                                                             1,
                                                             false);
            }
            SetUtil.removeDuplicates(members);
            sortByPresentationName(kb, "", members);
            result.addAll(members);
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
        return result;
    }

    /** *************************************************************
     * Supports memoization for isSubclass(kb, c1, c2).
     */
    private static Map isSubclassCache = new HashMap();

    /** *************************************************************
     * Returns true if c1 is found to be a subclass of c2, else
     * returns false.
     *
     * @param kb, A KB object
     * @param c1 A String, the name of a SetOrClass
     * @param c2 A String, the name of a SetOrClass
     * @return boolean
     */
    private static boolean isSubclass(KB kb, String c1, String c2) {
        boolean ans = false;
        try {
            if (StringUtil.isNonEmptyString(c1) && StringUtil.isNonEmptyString(c2)) {
                Set terms = (Set) isSubclassCache.get(c1);
                if (terms == null) {
                    terms = kb.getAllSuperClassesWithPredicateSubsumption(c1);
                    isSubclassCache.put(c1, terms);
                }
                ans = terms.contains(c2);
            }
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
        return ans;
    }

    /** *************************************************************
     * Returns true if c1 is found to be a subclass of c2, else
     * returns false.
     *
     * @param kb, A KB object
     * @param c1 A String, the name of a SetOrClass
     * @param c2 A String, the name of a SetOrClass
     * @return boolean
     */
    private static boolean isSubentity(KB kb, String c1, String c2) {
        boolean ans = false;
        try {
            if (StringUtil.isNonEmptyString(c1) && StringUtil.isNonEmptyString(c2)) {
                ans = isSubclass(kb, c1, c2);
                if (!ans) {
                    List<String> terms = kb.getTermsViaPredicateSubsumption("subentity", 
                                                                            2, 
                                                                            c2,
                                                                            1,
                                                                            true);
                    ans = terms.contains(c1);
                }
            }
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
        return ans;
    }

    /** *************************************************************
     * 
     */
    private boolean isXsdComplexType(KB kb, String term) {
        boolean ans = false;
        try {

            if (isComposite(kb, term) || isDataType(kb, term)) {
                List elems = getSubordinateXmlElementTerms(kb, term, 1);
                List attrs = getSubordinateXmlAttributeTerms(kb, term, 1);
                ans = (!attrs.isEmpty() || !elems.isEmpty());
            }
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
        return ans;
    }

    /** *************************************************************
     * 
     */
    private boolean isXsdSimpleType(KB kb, String term) {
        boolean ans = false;
        try {
            if (isDataType(kb, term)) {
                List terms = getSyntacticUnionTerms(kb, term, 2);
                ans = !terms.isEmpty();
            }
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
        return ans;
    }

    /** *************************************************************
     * 
     */
    private boolean hasComplexContent(Element elem) {
        boolean ans = false;
        try {
            Set nodeNames = new HashSet();
            List accumulator = new ArrayList();
            accumulator.add(elem);
            List working = new ArrayList();
            Node node_i = null;
            Node node_j = null;
            for (int c = 0; !accumulator.isEmpty(); c++) {
                working.clear();
                working.addAll(accumulator);
                accumulator.clear();
                for (Iterator it = working.iterator(); it.hasNext();) {
                    node_i = (Node) it.next();
                    if ((c > 0) && (node_i.getNodeType() == Node.ELEMENT_NODE)) { 
                        nodeNames.add(node_i.getNodeName()); 
                    }
                    NodeList children = node_i.getChildNodes();
                    for (int j = 0; j < children.getLength(); j++) {
                        node_j = children.item(j);
                        if (node_j.getNodeType() == Node.ELEMENT_NODE) {
                            accumulator.add(node_j);
                        }
                    }
                }
            }
            String attrVal = elem.getAttribute("base");
            ans = (nodeNames.contains("xs:sequence") 
                   || nodeNames.contains("xs:choice")
                   || ((nodeNames.size() > 1)
                       && !nodeNames.contains("xs:attribute"))

                   // This is a hack.  Remove when possible.
                   || (StringUtil.isNonEmptyString(attrVal)
                       && attrVal.startsWith("ddexC:")));
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
        return ans;
    }

    /** *************************************************************
     * 
     */
    private void printXmlNodeTree(org.w3c.dom.Node rootNode, int indent, PrintWriter pw) {
        try {
            String nodeName = rootNode.getNodeName();
            if (rootNode.getNodeType() == org.w3c.dom.Node.ELEMENT_NODE) {
                if (nodeName.matches(".*(?i)documentation.*")) {
                    String txt = rootNode.getTextContent();
                    pw.println(DocGen.indentChars("    ", indent) 
                               + "<" + nodeName + ">"
                               + txt
                               + "</" + nodeName + ">");
                }
                else {
                    if (nodeName.matches(".*(?i)schema.*")) {
                        pw.println("<?xml version=\"1.0\"?>");
                    }
                    pw.print(DocGen.indentChars("    ", indent) + "<" + nodeName);
                    if (rootNode.hasAttributes()) {
                        String[] orderedNames = {"name", "type", "minOccurs", "maxOccurs"};
                        NamedNodeMap attributes = rootNode.getAttributes();
                        List attrsInPrintOrder = new ArrayList();
                        Node attr = null;
                        for (int j = 0; j < orderedNames.length; j++) {
                            attr = attributes.getNamedItem(orderedNames[j]);
                            if (attr != null) { attrsInPrintOrder.add(attr); }
                        }
                        int alen = attributes.getLength();
                        for (int i = 0; i < alen; i++) {
                            attr = attributes.item(i);
                            if (!attrsInPrintOrder.contains(attr)) {
                                attrsInPrintOrder.add(attr);
                            }
                        }
                        int alast = (alen - 1);
                        String spacer = " ";
                        String attrName = null;
                        String attrVal = null;
                        for (int i = 0; i < alen; i++) {
                            attr = (Node) attrsInPrintOrder.get(i);
                            attrName = attr.getNodeName();
                            attrVal = attr.getNodeValue();
                            /*
                              if (i > 0) {
                              spacer = (DocGen.indentChars("    ", indent) 
                              + DocGen.indentChars(" ", nodeName.length())
                              + "  ");
                              }
                            */
                            pw.print(spacer + attrName + "=\"" + attrVal + "\""
                                     /*
                                       + ((i < alast) 
                                       ? System.getProperty("line.separator")
                                       : "")
                                     */
                                     );
                        }
                    }
                    boolean descend = false;
                    NodeList children = null;
                    if (rootNode.hasChildNodes()) {
                        children = rootNode.getChildNodes();
                        int clen = children.getLength();
                        for (int i = 0; i < clen; i++) {
                            if (children.item(i).getNodeType() 
                                != org.w3c.dom.Node.ATTRIBUTE_NODE) {
                                descend = true;
                                break;
                            }
                        }
                    }
                    if (!descend) {
                        pw.println(" />");
                    }
                    else {
                        pw.println(">");
                        children = rootNode.getChildNodes();
                        int clen = children.getLength();
                        Node kid = null;
                        for (int i = 0; i < clen; i++) {
                            kid = children.item(i);
                            if (kid.getNodeType() != org.w3c.dom.Node.ATTRIBUTE_NODE) {
                                printXmlNodeTree(kid, (indent + 1), pw);
                            }
                        }
                        pw.println(DocGen.indentChars("    ", indent)
                                   + "</" + nodeName + ">");
                    }
                }
            }
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
        return;
    }

    /** *************************************************************
     * 
     */
    private String substituteXsdDataType(KB kb, String nsTerm, String term) {
        String xmlType = term;
        try {
            String newType = getClosestXmlDataType(kb, term);
            if (StringUtil.isNonEmptyString(newType)) {
                if (nsTerm.equals(getDefaultNamespace())
                    || (!newType.equals(StringUtil.w3cToKif("xs:IDREF"))
                        && !newType.equals(StringUtil.w3cToKif("xs:ID")))) {
                    xmlType = newType;
                }
            }
            xmlType = StringUtil.kifToW3c(xmlType);
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
        return xmlType;
    }

    /// END: code for XSD generation.


    /// BEGIN: code for DDEX "Tabular Display"

    /** *************************************************************
     * Generates HTML files in a tabular format.
     */
    public void generateTabularFocalTermFiles(KB kb, String ontology, String outdirPath) {

        System.out.println("ENTER DocGen.generateTabularFocalTermFiles("
                           + kb.name + ", "
                           + ontology + ", "
                           + outdirPath + ")");

        try {
            setOntology(ontology);
            List focalTerms = computeFocalTerms(kb, ontology);
            if (getCoreTerms().isEmpty()) computeTermRelevance(kb, ontology);
            List accumulator = new LinkedList();
            List working = new LinkedList();
            String refterm = null;
            String description = null;
            String namespace = null;
            Iterator rti = null;
            for (rti = focalTerms.iterator(); rti.hasNext();) {

                refterm = (String) rti.next();
                writeTabularFocalTermFile(kb, refterm, outdirPath);
            }
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }

        System.out.println("EXIT DocGen.generateTabularFocalTermFiles("
                           + kb.name + ", "
                           + ontology + ", "
                           + outdirPath + ")");

        return;
    }

    /** *************************************************************
     * Create a PrintWriter for the focal term kifTerm.
     */
    private PrintWriter createFocalTermPrintWriter(String kifTerm, String outdirPath) {
        PrintWriter pw = null;
        try {
            File outdir = new File(outdirPath);
            String w3cTerm = StringUtil.kifToW3c(kifTerm);
            String filename = StringUtil.toSafeNamespaceDelimiter(w3cTerm) + ".html";
            File outfile = new File(outdir, filename);
            pw = new PrintWriter(new FileWriter(outfile));
        }
        catch (Exception ex) {
            if (pw == null) {
                System.out.println("ERROR in DocGen.createFocalTermPrintWriter("
                                   + kifTerm + ", "
                                   + outdirPath + ")");
                System.out.println("Could not create PrintWriter for " + kifTerm);
            }
            ex.printStackTrace();
        }
        return pw;
    }

    /** *************************************************************
     * Returns the maximum number of steps "descending" along
     * syntacticSubordinate from focalTerm.
     *
     * @param kb The KB in which syntacticSubordinate graph is
     * computed
     *
     * @param focalTerm The top-level term from which maximum graph
     * depth is to be computed
     *
     * @return The int that is the maximum depth reachable from
     * focalTerm
     *
     */
    private int getMaxDepthFromFocalTerm(KB kb, String focalTerm) {
        return getMaxDepthFromFocalTerm(kb, focalTerm, new HashSet());
    }

    /** *************************************************************
     * Generates HTML files in a tabular format.
     */
    private void writeTabularFocalTermFile(KB kb, String kifTerm, String outdirPath) {
        PrintWriter pw = null;
        try {
            String namespace = getTermNamespace(kb, kifTerm);
            // String pname = getTermPresentationName(kb, namespace, kifTerm);
            ArrayList context = new ArrayList();
            context.add(namespace);
            String description = getTabularOutputTermDocumentation(kb, 
                                                                   kifTerm, 
                                                                   context);
            String w3cName = StringUtil.kifToW3c(kifTerm);
            // String titleName = termName.replace(":", "-");

            List attrs = getSubordinateAttributes(kb, kifTerm);
            List elems = getSubordinateElements(kb, kifTerm);
            if (!attrs.isEmpty() || !elems.isEmpty()) {

                int maxDepth = getMaxDepthFromFocalTerm(kb, kifTerm);
                int columns = (maxDepth + 3);
                int cspan1 = (columns - 1);
                int columns2 = (columns - 3);
                int cspan2 = (columns2 - 1);

                pw = createFocalTermPrintWriter(kifTerm, outdirPath);

                pw.println(generateHtmlDocStart(w3cName));

                // TO DO: Represent the number of columns and their
                // headings, so that this information can be loaded
                // from the KB.

                pw.println("<table>");

                pw.println("  <tr>");
                pw.println("    <td class=\"title-cell\" colspan=\"" + columns + "\">");
                pw.print("      ");
                pw.println(w3cName);
                pw.println("    </td>");
                pw.println("  </tr>");

                pw.println("  <tr>");
                pw.println("    <td colspan=\"" + columns + "\">");
                pw.print("      ");
                pw.println(description);
                pw.println("    </td>");
                pw.println("  </tr>");

                pw.println("  <tr>");
                pw.println("    <td class=\"title-cell\" colspan=\"" + columns2 + "\">");
                pw.println("      Message Element");
                pw.println("    </td>");
                pw.println("    <td class=\"title-cell\">");
                pw.println("      Data Type");
                pw.println("    </td>");
                pw.println("    <td class=\"title-cell\">");
                pw.println("      Card");
                pw.println("    </td>");
                pw.println("    <td class=\"title-cell\">");
                pw.println("      Element Description");
                pw.println("    </td>");
                pw.println("  </tr>");

                LinkedList<String> path = new LinkedList<String>();
                path.addFirst(kifTerm);
                writeTabularFocalTermRows(pw, kb, namespace, path, 0, columns2);

                // pw.println(generateHtmlFooter(""));

                pw.println("</table>");
                pw.println("  </body>");
                pw.println("</html>");
            }
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
        finally {
            try {
                if (pw != null) pw.close();
            }
            catch (Exception pwe) {
                pwe.printStackTrace();
            }
        }
        return;
    }

    /** *************************************************************
     * Generates HTML table rows.
     */
    private void writeTabularFocalTermRows(PrintWriter pw, 
                                           KB kb, 
                                           String namespace,
                                           LinkedList<String> path,
                                           int depth,
                                           int maxDepth) {

        long t1 = System.currentTimeMillis();
        /*
          System.out.println("ENTER DocGen.writeTabularFocalTermRows("
          + pw.toString() + ", "
          + kb.name + ", "
          + namespace + ", "
          + path + ")");
        */
        try {
            String kifTerm = path.getFirst();
            List attrs = getSubordinateAttributes(kb, kifTerm);
            List elems = getSubordinateElements(kb, kifTerm);
            List pair = getInheritedSubordinates(kb, kifTerm, null);
            attrs.addAll(0, (List) pair.get(0));
            elems.addAll(0, (List) pair.get(1));
            String subord = null;
            String pname = null;
            String datatype = null;
            String cardinality = null;
            String description = null;
            int blen = path.size();
            int multiplier = ((blen > 0) ? (blen - 1) : blen);
            int indent = (multiplier * 10);
            for (Iterator ati = attrs.iterator(); ati.hasNext();) {
                subord = (String) ati.next();
                if (!path.contains(subord)) {
                    writeTabularTermRow(pw, kb, namespace, subord, depth, maxDepth, true);
                    LinkedList<String> newPath = new LinkedList<String>(path);
                    newPath.addFirst(subord);
                    writeTabularFocalTermRows(pw, 
                                              kb, 
                                              namespace,
                                              newPath,
                                              (depth + 1),
                                              maxDepth);
                }
            }
            for (Iterator eli = elems.iterator(); eli.hasNext();) {
                subord = (String) eli.next();
                if (!path.contains(subord)) {
                    writeTabularTermRow(pw, kb, namespace, subord, depth, maxDepth, false);
                    LinkedList<String> newPath = new LinkedList<String>(path);
                    newPath.addFirst(subord);
                    writeTabularFocalTermRows(pw, 
                                              kb, 
                                              namespace,
                                              newPath,
                                              (depth + 1),
                                              maxDepth);
                }
            }
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }

        long t2 = (System.currentTimeMillis() - t1);

        /*
          System.out.println("EXIT DocGen.writeTabularFocalTermRows("
          + pw.toString() + ", "
          + kb.name + ", "
          + namespace + ", "
          + path + ")");
          System.out.println("  " + (t2 / 1000.0) + " seconds");
        */

        return;
    }

    /** *************************************************************
     * Generates one HTML table row.
     */
    private void writeTabularTermRow(PrintWriter pw, 
                                     KB kb, 
                                     String namespace, 
                                     String kifTerm,
                                     int indent,
                                     int maxDepth,
                                     boolean isAttribute) {
        try {
            int cspan = (maxDepth - indent);
            List context = new ArrayList();
            context.add(namespace);
            if (!context.contains(kifTerm)) context.add(kifTerm);
            String pname = getTermPresentationName(kb, namespace, kifTerm, true);
            String datatype = getFirstDatatype(kb, kifTerm);
            if (StringUtil.isNonEmptyString(datatype)) {
                datatype = showTermName(kb, datatype, getDefaultNamespace(), true);
            }
            else {
                datatype = "";
            }
            String cardinality = showCardinalityCell(kb, "", kifTerm, "");
            String description = getTabularOutputTermDocumentation(kb, 
                                                                   kifTerm, 
                                                                   context);
            String classVal = (isAttribute ? "attribute" : "element");
            List keys = Arrays.asList("datatype", "cardinality", "description");
            List vals = new ArrayList();
            vals.add(datatype);
            vals.add(cardinality);
            vals.add(description);

            // Start a new row.
            pw.println("  <tr>");

            for (int i = 0; i < indent; i++) {
                pw.println("    <td>&nbsp;</td>");
            }

            pw.print("    <td class=\"");
            pw.print(classVal);
            pw.print("\" colspan=\"" + cspan + "\"> ");
            pw.print(pname);
            pw.println(" </td>");

            // The final three columns.
            String key = null;
            String val = null;
            int vlen = keys.size();
            for (int i = 0; i < vlen; i++) {
                key = (String) keys.get(i);
                val = (String) vals.get(i);
                pw.println("    <td class=\"" + key + "\">");
                pw.print("      ");
                pw.println(val);
                pw.println("    </td>");
            }
            pw.println("  </tr>");

        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
        return;
    }

    /** *************************************************************
     * Returns a documetation String for kifTerm, specially formatted
     * for tabular output mode.
     */
    private String getTabularOutputTermDocumentation(KB kb, String kifTerm, List context) {
        String docstr = "";
        try {
            String tmpstr = getContextualDocumentation(kb, kifTerm, context);
            tmpstr = processDocString(kb, 
                                      "", 
                                      getDefaultNamespace(), 
                                      tmpstr,
                                      false,
                                      false);
            docstr = tmpstr.replaceAll("&%\\w+_", "");
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
        return docstr;
    }

    /// END: code for DDEX "Tabular Display"

    /** *************************************************************
     * 
     */
    public static void main (String[] args) {
        String status = "";
        String ls = System.getProperty("line.separator");
        try {
            String token = null;
            if (args.length > 0) token = args[0];

            if (StringUtil.emptyString(token)) {
                System.out.println("No application token was entered");
                System.exit(1);
            }

            if (token.equalsIgnoreCase("rcdocgen")) {
                if (args.length > 2) {
                    ArrayList<String> newargs = new ArrayList<String>();
                    for (int i = 1; i < args.length; i++) {
                        newargs.add(args[i]);
                    }
                    status += (ls + generateDocumentsFromKB(newargs));
                }
            }
        }
        catch (Throwable th) {
            System.out.println(status);
            System.out.println(th.getMessage());
            th.printStackTrace();
        }
        return;
    }

    /*
      public static void main (String args[]) {

      try {

      // 1. In config.xml, set the KB name to something
      // appropriate.

      // 2. In config.xml, specify the desired initial
      // constituent files.  For DDEX, these would be Merge.kif,
      // RCOX.kif, and DdexTranslation.kif.

      // 3. Make sure $SIGMA_HOME is set appropriately.

      // 4. Initialize the KB: KBmanager.getMgr().initializeOnce();

      // 4. Call DocGen with the appropriate input arguments.
      // Now, this need be only the name of the ontology: DDEX.
            
      // 5. Process the declarative information needed to
      // generate the KIF file from the SCOW .csv file.

      // 6. Wrtie out the .kif file.

      // 7. Add the canonical pathname of the .kif file to the
      // KB's constituent list.

      // 8. Reload the KB.

      // 9. Generate the various types of files.

      } 
      catch (Exception ex) {
      System.out.println(ex.getMessage());
      }
      }
    */

} // DocGen.java
