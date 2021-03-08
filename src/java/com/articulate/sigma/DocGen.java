/** This code is copyrighted by Articulate Software (c) 2007.  It is
released under the GNU Public License
&lt;http://www.gnu.org/copyleft/gpl.html&gt;.  Users of this code also
consent, by use of this code, to credit Articulate Software in any
writings, briefings, publications, presentations, or other representations
of any software which incorporates, builds on, or uses this code.  Please
cite the following article in any publication with references:
Pease, A., (2003). The Sigma Ontology Development Environment, in Working
Notes of the IJCAI-2003 Workshop on Ontology and Distributed Systems,
August 9, Acapulco, Mexico.  See also http://sigmakee.sourceforge.net
*/
/*************************************************************************************************/

package com.articulate.sigma;
import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.LineNumberReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

import com.articulate.sigma.nlg.NLGUtils;
import com.articulate.sigma.utils.AVPair;
import com.articulate.sigma.utils.SetUtil;
import com.articulate.sigma.utils.StringUtil;

/** A class to generate simplified HTML-based documentation for SUO-KIF terms. */
public class DocGen {

    /** *************************************************************
     * This String token denotes Sigma's "simple" HTML layout, and is
     * used as a flag in the HTML generation code to switch between
     * full and simple modes.
     */
    protected static final String F_SI = "si";
    protected static List<String> F_CONTROL_TOKENS = null;
    static {
        if (F_CONTROL_TOKENS == null)
            F_CONTROL_TOKENS = new ArrayList<String>();
        F_CONTROL_TOKENS.add(F_SI);
    }
    public static List<String> getControlTokens() {
        return F_CONTROL_TOKENS;
    }
    public static int getControlBitValue(String token) {
        int bitValue = 0;
        try {
            int idx = getControlTokens().indexOf(token);
            if (idx > -1) {
                bitValue = Double.valueOf(Math.pow(2.0d, 
                                                   Integer.valueOf(idx).doubleValue())).intValue();
            }
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
        return bitValue;
    }
    protected static final String SP2 = "  ";

    /** *************************************************************
     * The default base plus file suffix name for the main index file
     * for a set of HTML output files.
     */
    protected static String INDEX_FILE_NAME = "index.html";
    protected int localCounter = 0;
    protected static final String DEFAULT_KEY = "docgen_default";
    protected static Hashtable DOC_GEN_INSTANCES = new Hashtable();

    public static DocGen getInstance() {

        DocGen inst = null;
        try {
            inst = (DocGen) DOC_GEN_INSTANCES.get(DEFAULT_KEY);
            if (inst == null) {
                inst = new DocGen();
                inst.setLineSeparator(StringUtil.getLineSeparator());
                DOC_GEN_INSTANCES.put(DEFAULT_KEY, inst);
            }
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
        return inst;
    }

    /** *************************************************************
     */
    public static DocGen getInstance(String compositeKey) {

        // System.out.println("ENTER DocGen.getInstance(" + compositeKey + ")");
        DocGen inst = null;
        try {
            KBmanager mgr = KBmanager.getMgr();
            String interned = compositeKey.intern();
            inst = (DocGen) DOC_GEN_INSTANCES.get(interned);
            if (inst == null) {
                inst = new DocGen();
                inst.setLineSeparator(StringUtil.getLineSeparator());
                int idx = interned.indexOf("-");
                KB kb = ((idx > -1) 
                         ? mgr.getKB(interned.substring(0, idx).trim())
                         : mgr.getKB(interned));
                if (kb instanceof KB)
                    inst.setKB(kb);
                String ontology = null;
                if ((idx > 0) && (idx < (interned.length() - 1))) 
                    ontology = interned.substring(idx + 1).trim();                
                if (StringUtil.emptyString(ontology)) 
                    ontology = inst.getOntology(kb);                
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
        // System.out.println("EXIT DocGen.getInstance(" + compositeKey + ")");
        // System.out.println("    inst == " + inst.toString());
        return inst;
    }

    /** *************************************************************
     */
    public static DocGen getInstance(KB kb, String ontology) {

        // System.out.println("ENTER DocGen.getInstance(" + kb.name + ", " + ontology + ")");
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
        // System.out.println("EXIT DocGen.getInstance(" + kb.name + ", " + ontology + ")");
        // System.out.println("    inst == " + inst.toString());
        return inst;
    }

    /** *************************************************************
     * To obtain an instance of DocGen, use the static factory method
     * getInstance().
     */
    protected DocGen() {
    }
    protected String lineSeparator = StringUtil.getLineSeparator();
    public String getLineSeparator() {
        return lineSeparator;
    }
    public void setLineSeparator(String ls) {
        lineSeparator = ls;
        return;
    }
    /** *****************************************************************
     * A int value representing the bit values that control the file
     * generation process.
     */
    protected int docGenControlBits = 0;
    /** *****************************************************************
     * Returns the int value that represents the bit values used to
     * guide aspects of the document generation process for this
     * DocGen instance.
     *
     * @return An int value representing bit values
     */
    public int getDocGenControlBits() {
        return docGenControlBits;
    }
    /** *****************************************************************
     * Sets to 0 the int value that represents the bit values used to
     * guide aspects of the document generation process for this
     * DocGen instance.
     */
    public void clearDocGenControlBits() {
        docGenControlBits = 0;
        return;
    }
    /** *****************************************************************
     * Adds val via bitwise OR to the int value that represents the
     * bit values used to control the document generation process for
     * this DocGen instance.
     *
     * @param val An integer representing bit values
     *
     * @return An int value representing the result of the bitwise OR
     * operation.
     */
    public int addDocGenControlBits(int val) {
        docGenControlBits = (docGenControlBits | val);
        return docGenControlBits;
    }
    /** *****************************************************************
     * Adds via bitwise OR the bit value corresponding to token to the
     * int value that represents the bit values used to control the
     * document generation process for this DocGen instance.
     *
     * @param token A String representing bit values
     *
     * @return An int value representing the result of the bitwise OR
     * operation.
     */
    public int addDocGenControlBits(String token) {
        int bitVal = getControlBitValue(token);
        return addDocGenControlBits(bitVal);
    }
    /** *****************************************************************
     * Returns true if the bit values represented by valToTest are
     * among the control bits represented for this DocGen instance.
     *
     * @param valToTest An integer representing bit values to be tested
     *
     * @return true or false
     */
    public boolean testDocGenControlBits(int valToTest) {

        // System.out.println("ENTER DocGen.testDocGenControlBits(" + valToTest + ")");
        boolean ans = false;
        try {
            // System.out.println("  getDocGenControlBits() == " + getDocGenControlBits());
            int bitAnd = (valToTest & getDocGenControlBits());
            // System.out.println("  bitAnd == " + bitAnd);
            ans = (bitAnd == valToTest);
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
        // System.out.println("EXIT DocGen.testDocGenControlBits(" + valToTest + ")");
        // System.out.println("  ans == " + ans);
        return ans;
    }

    /** *****************************************************************
     * Returns true if the bit values corresponding to token are among
     * the control bits represented for this DocGen instance.
     *
     * @param token A String token corresponding to a bit value
     *
     * @return true or false
     */
    public boolean testDocGenControlBits(String token) {
        int bitVal = getControlBitValue(token);
        return testDocGenControlBits(bitVal);
    }

    /** **************************************************************
     * Returns a List of String tokens that determine how the output
     * should be formatted.  The List could be empty.
     *
     * @param kb The KB in which to look for docGenOutputFormat statements
     *
     * @param ontology A SUO-KIF term denoting an Ontology
     *
     * @return a List of Strings, which could be empty
     */
    public static ArrayList<String> getOutputFormatTokens(KB kb, String ontology) {

        ArrayList<String> tokens = new ArrayList<String>();
        try {
            String tkn = null;
            if (StringUtil.isNonEmptyString(ontology)) {
                for (Iterator it = kb.getTermsViaAskWithRestriction(0, 
                                                                    "docGenOutputFormat", 
                                                                    1, 
                                                                    ontology,
                                                                    2).iterator(); 
                     it.hasNext();) {
                    tkn = StringUtil.removeEnclosingQuotes(it.next().toString().toLowerCase());
                    if (!tokens.contains(tkn)) tokens.add(tkn);
                }
            }
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
        return tokens;
    }

    /** **************************************************************
     * Returns the first String token retrieved from ontology in kb
     * that denotes an HTML output format.  Such tokens may be the
     * third element in statements for which the predicate is
     * docGenOutputFormat.
     *
     * @param kb The KB in which to look for docGenOutputFormat statements
     *
     * @param ontology A SUO-KIF term denoting an Ontology
     *
     * @return a String token, or null.
     */
    public static String getFirstHtmlFormatToken(KB kb, String ontology) {
        return F_SI;
    }

    /** The default namespace associated with this DocGen object */

    protected String defaultNamespace = "";

    /** *****************************************************************
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
                setDefaultNamespace(StringUtil.isNonEmptyString(onto)
                                    ? kb.getFirstTermViaAskWithRestriction(0,
                                                                           "docGenDefaultNamespace",
                                                                           1,
                                                                           onto,
                                                                           2)
                                    : null);
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
    protected String defaultPredicateNamespace = "";

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
                    (StringUtil.isNonEmptyString(onto)
                     ? kb.getFirstTermViaAskWithRestriction(0,
                                                            "docGenDefaultPredicateNamespace",
                                                            1,
                                                            onto,
                                                            2)
                     : null);
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
     * The ontology associated with this DocGen object, and for
     * which the DocGen object is used to generate files.
     */
    protected String ontology = null;

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
                Iterator it = null;
                // First, we try to find any obvious instances of
                // Ontology, using predicate subsumption to take
                // advantage of any predicates that have been
                // liked with SUMO's predicates.
                if (kb.kbCache.instanceOf.get("Ontology") != null) {
                    for (it = kb.kbCache.instanceOf.get("Ontology")
                            .iterator(); it.hasNext(); ) {
                        candidates.add((String) it.next());
                    }
                }
                if (candidates.isEmpty()) {
                    // Next, we check for explicit
                    // ontologyNamespace statements.
                    List formulae = kb.ask("arg", 0, "ontologyNamespace");
                    if ((formulae != null) && !formulae.isEmpty()) {
                        Formula f = null;
                        for (it = formulae.iterator(); it.hasNext();) {
                            f = (Formula) it.next();
                            candidates.add(f.getStringArgument(1));
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
                        if (it.hasNext()) 
                            onto = (String) it.next();                            
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
    protected KB kb = null;

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
    protected Set codedIdentifiers = null;

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
                Set codes = kb.kbCache.instanceOf.get("CodedIdentifier");
                Set classNames = kb.kbCache.instanceOf.get("CodedIdentifier");
                classNames.add("CodedIdentifier");
                Object[] namesArr = classNames.toArray();
                if (namesArr != null) {
                    String className = null;
                    for (int i = 0; i < namesArr.length; i++) {
                        className = (String) namesArr[i];
                        codes.addAll(kb.getTermsViaPredicateSubsumption("instance",2,className,1,false));
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
    protected String titleText = "";

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

    protected String footerText = "";
    //"Produced by <a href=\"http://www.articulatesoftware.com\"> " + "Articulate Software</a> and its partners";

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

    protected String styleSheet = "simple.css";

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

    protected String defaultImageFile = "articulate_logo.gif";

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

    protected String defaultImageFileMarkup = "articulate_logo.gif";

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
    /** The canonical pathname of the current directory in which
     * output files will be (are being) saved.
     */

    protected String outputDirectoryPath = "";

    /** *************************************************************
     * Sets the canonical pathname String of the current directory in
     * which output files will be (are being) saved.
     *
     * @param pathname A canonical pathname String
     * 
     */
    public void setOutputDirectoryPath(String pathname) {
        outputDirectoryPath = pathname;
        return;
    }

    /** *************************************************************
     * Returns the canonical pathname String of the current directory
     * in which output files will be (are being) saved.
     */
    public String getOutputDirectoryPath() {
        return this.outputDirectoryPath;
    }

    /** *************************************************************
     * A Map containing String replacement pairs.  This is to provide
     * adequate ASCII translations for HTML character entities, in
     * circumstances where occurrences of the entities might cause
     * parsing or rendering problems (e.g., apparently, in XSD files).
     *
     */
    protected Map stringReplacementMap = null;
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
                if (kb != null) {
                    List formulae = kb.ask("arg", 0, "docGenCodeMapTranslation");
                    if (formulae != null) {
                        Formula f = null;
                        for (Iterator it = formulae.iterator(); it.hasNext();) {
                            f = (Formula) it.next();
                            srMap.put(StringUtil.removeEnclosingQuotes(f.getStringArgument(2)),
                                      StringUtil.removeEnclosingQuotes(f.getStringArgument(4)));
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
    protected Set inhibitDisplayRelations = null;

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
    protected String tocHeader = "";

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

    /** A default key to identify this particular DocGen object **/
    protected String docGenKey = DEFAULT_KEY;

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

    /** *************************************************************
     * If true, a termFormat value obtained for term will be displayed
     * rather than the term name itself.
     */
    protected boolean simplified = false;

    /** *************************************************************
     * Returns true if a termFormat value obtained for term will be
     * displayed during HTML rendering rather than the term name
     * itself.
     */
    public boolean getSimplified() {
        return this.simplified;
    }

    /** *************************************************************
     * Sets this.simplified to val.  If this.simplified is true, the
     * statements in Sigma's KB will be rendered in a simple
     * frame-like HTML format rather than as SUO-KIF Formulas.
     */
    public void setSimplified(boolean val) {
        this.simplified = val;
        return;
    }

    /** *************************************************************
     * A Map in which each key is a KB name and the corresponding
     * value is a List of the Predicates defined in the KB.
     */
    protected HashMap relationsByKB = new HashMap();
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
    protected File outputParentDir = null;  // new File(KBmanager.getMgr().getPref("baseDir"));

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
            if (obj != null) {
                if (!obj.exists() || !obj.isDirectory() || !obj.canWrite()) {
                    System.out.println("WARNING in DocGen.setOutputParentDir(" + obj + "):");
                    System.out.println("  " + obj + " is not an accessible directory");
                    String pathname = obj.getCanonicalPath();
                    if (StringUtil.isNonEmptyString(pathname)) {
                        System.out.println("  Will try to create " + pathname);
                        obj.mkdirs();
                    }
                }
                if (obj.isDirectory() && obj.canWrite()) 
                    this.outputParentDir = obj;                
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
     * @param pathnameComponents A String representing a directory pathname
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
            if (StringUtil.isNonEmptyString(ontology)) {
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

    public interface DisplayFilter {
        /** ***************************************************************
         * Returns true if suoKifTerm may be displayed or included in the
         * particular UI text or other output generated by the DocGen
         * object dg.
         *
         * @param dg The DocGen object that will use this filter to
         * determine which terms should be displayed or otherwise included
         * in generated output
         *
         * @param suoKifTerm A term in the SUO-KIF representation
         * language, which could be an atomic constant, a variable, a
         * quoted character string, or a list
         *
         * @return true or false
         */
        public boolean isLegalForDisplay (DocGen dg, String suoKifTerm);
    }

    /** *************************************************************
     * The DisplayFilter which, if present, determines if a given
     * SUO-KIF object may be displayed or output by this DocGen
     * object.
     */
    protected DisplayFilter displayFilter = null;

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

    public class PresentationNameComparator implements Comparator {

        protected DocGen docGen = null;
        public DocGen getDocGen() {
            return docGen;
        }
        public void setDocGen(DocGen gen) {
            docGen = gen;
            return;
        }
        protected KB kb = null;
        public KB getKB() {
            return kb;
        }
        public void setKB(KB kbObj) {
            kb = kbObj;
            return;
        }

        public int compare(Object o1, Object o2) {
            String str1 = ((o1 == null) ? "" : StringUtil.removeEnclosingQuotes(o1.toString()));
            String str2 = ((o2 == null) ? "" : StringUtil.removeEnclosingQuotes(o2.toString()));
            DocGen gen = getDocGen();
            if (gen != null) {
                KB gKB = gen.getKB();
                str1 = gen.getTermPresentationName(gKB, str1);
                str2 = gen.getTermPresentationName(gKB, str2);
            }
            return String.CASE_INSENSITIVE_ORDER.compare(str1, str2);
        }

        public boolean equals(Object obj) {
            boolean ans = true;
            if (ans) {
                PresentationNameComparator pnc = (PresentationNameComparator) obj;
                KB eKB = getKB();
                DocGen dg = getDocGen();
                ans = ((dg != null) 
                       && (pnc instanceof PresentationNameComparator)
                       && (pnc.getDocGen().equals(dg)));
                if (ans) {
                    ans = (eKB == pnc.getKB());
                }
            }
            return ans;
        }
        
        /** ***************************************************************
         * should never be called so throw an error.
         */   
        public int hashCode() {
            assert false : "DocGen.hashCode not designed";
            return 0;
        }
        
    } // end of PresentationNameComparator

    /** *************************************************************
     *  Rebuilds the TreeSet containing all terms in kb, and forces
     *  the new TreeSet to sort according to each term's presentation
     *  name.
     */
    public SortedSet<String> resortKbTerms(KB kb) {

        long t1 = System.currentTimeMillis();
        System.out.println("ENTER DocGen.resortKbTerms(" + kb.name + ")");
        try {
            PresentationNameComparator pnc = new PresentationNameComparator();
            pnc.setKB(kb);
            pnc.setDocGen(this);
            TreeSet<String> ts = new TreeSet<String>(pnc);
            synchronized (kb.getTerms()) {
                ts.addAll(kb.getTerms());
            }
            kb.setTerms((SortedSet) ts);
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
        System.out.println("EXIT DocGen.resortKbTerms(" + kb.name + ")");
        System.out.println("    " 
                           + kb.getTerms().size() 
                           + " KB terms sorted in "
                           + ((System.currentTimeMillis() - t1) / 1000.0)
                           + " seconds");
        return kb.getTerms();
    }

    /** *************************************************************
     *  A TreeMap of TreeMaps of ArrayLists where the keys are
     *  uppercase single characters (of term formats or headwords) and
     *  the values are TreeMaps with a key of the term formats or
     *  headwords and ArrayList values of the actual term names.  Note
     *  that if "simplified" is false actual term names will be used
     *  instead of term formats or headwords and the interior map will
     *  have keys that are the same as their values.
     * 
     *  Pictorially:
     *
     *      letter->    formattedTerm1->term11,term12...term1N
     *                  formattedTerm2->term21,term22...term2N
     */
    protected TreeMap alphaList = new TreeMap(String.CASE_INSENSITIVE_ORDER);

    /** *************************************************************
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
    public TreeMap getAlphaList(KB kb) {

        try {
            if (alphaList.isEmpty()) {
                synchronized (alphaList) {
                    createAlphaList(kb);
                }
            }
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
        return alphaList;
    }

    /** *************************************************************
     *  Clears the alphaList for this DocGen object.
     */
    public void clearAlphaList() {

        try {
            synchronized (alphaList) {
                alphaList.clear();
            }
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
        return;
    }

    /** *************************************************************
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
    protected TreeMap createAlphaList(KB kb) { // , HashMap stringMap
        /*
          long t1 = System.currentTimeMillis();
          System.out.println("ENTER DocGen.createAlphaList("
          + kb.name // + ", "
          // + "[map with " + stringMap.size() + " entries]" 
          + ")");
        */
        try {
            alphaList.clear();
            Set kbterms = kb.getTerms();
            synchronized (kbterms) {
                for (Iterator it = kbterms.iterator(); it.hasNext();) {
                    String term = (String) it.next();
                    if (isLegalForDisplay(StringUtil.w3cToKif(term))
                        && !getCodedIdentifiers(kb).contains(term)
                        // && !term.matches("^iso\\d+.*_.+")
                        ) {
                        String formattedTerm = stripNamespacePrefix(kb, term);
                        if (getSimplified()) {
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
                            // System.out.println("           term == " + term);
                            // System.out.println("  formattedTerm == " + formattedTerm);
                        }
                    }
                }
            }
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
        /*
          System.out.println("EXIT DocGen.createAlphaList("
          + kb.name // + ", "
          // + "[map with " + stringMap.size() + " entries]" 
          + ")");
          System.out.println("  "
          + ((System.currentTimeMillis() - t1) / 1000.0)
          + " seconds elapsed time");
        */
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

    /** **************************************************************
     * Returns an ArrayList of Strings extracted from the range
     * argument (arg2) of the first retrieved statement formed with
     * predicate.  If no statement can be retrieved, the ArrayList
     * will be empty.
     *
     * @param kb The KB from which to retrieve a statement with predicate
     *
     * @param predicate
     *
     * @return An ArrayList of Strings, which could be empty.
     */
    public static ArrayList<String> getRangeValueList(KB kb, String predicate) {

        ArrayList<String> rangeList = new ArrayList<String>();
        try {
            List<String> range = kb.getTermsViaAsk(0,predicate,2);
            if (!range.isEmpty()) {
                String kifList = (String) range.get(0);
                if (StringUtil.isNonEmptyString(kifList)) {
                    kifList = StringUtil.removeEnclosingQuotes(kifList);
                    Formula f = new Formula();
                    f.read(kifList);
                    String term = null;
                    for (int i = 0; f.listP() && !f.empty(); i++) {
                        term = StringUtil.removeEnclosingQuotes(f.car());
                        if (i > 0) {
                            rangeList.add(term);
                        }
                        f.read(f.cdr());
                    }
                }
            }
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
        return rangeList;
    }

    /** **************************************************************
     * Tries to use the values obtained from kb and ontology to set
     * some of the parameter values used for HTML generation.
     *
     * @param kb The KB from which to gather stated parameter values
     *
     * @param ontology The ontology from which to gather stated
     * parameter values
     *
     * @return void
     */
    public void setMetaDataFromKB(KB kb, String ontology) {

        if (StringUtil.isNonEmptyString(ontology)) {
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
                        setDefaultNamespace(val);
                    else if (pred.equals("docGenDefaultPredicateNamespace"))
                        setDefaultPredicateNamespace(val);
                    else if (pred.equals("docGenLogoImageFile"))
                        setDefaultImageFile(val);
                    else if (pred.equals("docGenLogoImageMarkup"))
                        setDefaultImageFileMarkup(val);
                    else if (pred.equals("docGenStyleSheet"))
                        setStyleSheet(val);
                    else if (pred.equals("docGenTitleText") 
                             && StringUtil.emptyString(getTitleText()))
                        setTitleText(val);
                    else if (pred.equals("docGenFooterText")
                             && StringUtil.emptyString(getFooterText()))
                        setFooterText(val);
                }
            }
        }
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
        setDisplayFilter(df);
        return;
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
                                      String formatToken) {
        /*             
                       System.out.println("ENTER DocGen.createCompositePage("
                       + kb.name + ", "
                       + kbHref + ", "
                       + term + ", "
                       + "[alphaList with " + alphaList.size() + " entries], "
                       + limit + ", "
                       + language + ", "
                       + formatToken + ")");
        */
        String markup = "";
        try {
            if (StringUtil.isNonEmptyString(term)) {
                /*
                  if (formatToken.equalsIgnoreCase(F_SI2)) {
                  markup = createCompositePage(kb, kbHref, term, alphaList, limit, language);
                  }
                  else {
                */
                StringBuilder result = new StringBuilder();
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
                result.append(showTermName(kb,term,language));
                result.append(StringUtil.getLineSeparator());
                result.append("    </td>");
                result.append(StringUtil.getLineSeparator());
                result.append("  </tr>");
                result.append(StringUtil.getLineSeparator());
                String relevance = ""; // createTermRelevanceNotice(kb, kbHref, term, language);
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
                result.append(createDisplayNames(kb, kbHref, term, formatToken));
                result.append(StringUtil.getLineSeparator());
                result.append(createSynonyms(kb, kbHref, term, formatToken));
                result.append(StringUtil.getLineSeparator());
                ArrayList superComposites = findContainingComposites(kb, term); 
                Collections.sort(superComposites, String.CASE_INSENSITIVE_ORDER);
                StringBuilder sb1 = new StringBuilder();
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
                        String formattedContainingComposites =
                            formatContainingComposites(kb,
                                                       kbHref,
                                                       superComposites,
                                                       term,
                                                       language);
                        if (StringUtil.isNonEmptyString(formattedContainingComposites)) {
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
                            result.append(formattedContainingComposites);
                            result.append(StringUtil.getLineSeparator());
                        }
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
                // }
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
                        + "[alphaList with " + alphaList.size() + " entries], "
                        + limit + ", "
                        + language + ", "
                        + formatToken + ")");
                        System.out.println("  markup == " + markup.length() + " chars");
        */
        return markup;
    }

    /** *************************************************************
     * Create an HTML page that lists information about a particular term,
     * with a limit on how many statements of each type should be
     * displayed.
     * 
     * @param alphaList a TreeMap of TreeMaps of ArrayLists.
     */
    public String createPage(KB kb, 
                             String kbHref, 
                             String term, 
                             TreeMap alphaList,
                             int limit, 
                             String language,
                             String formatToken) {

        String output = "";
        try {
            String sep = StringUtil.getLineSeparator();
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
            result.append("<table width=\"100%\">" + sep);
            result.append("  <tr bgcolor=\"#DDDDDD\">" + sep);
            result.append("    <td valign=\"top\" class=\"title\">"+ sep);
            result.append("      ");
            result.append(showTermName(kb,term,language)+ sep);
            result.append("      "+ sep);
            result.append("    </td>"+ sep);
            result.append("  </tr>"+ sep);
            String relevance = "";  // createTermRelevanceNotice(kb, kbHref, term, language);
            if (StringUtil.isNonEmptyString(relevance)) {
                result.append("  <tr bgcolor=\"#DDDDDD\">"+ sep);
                result.append("    <td valign=\"top\" class=\"cell\">"+ sep);
                result.append(relevance+ sep);
                result.append("    </td>"+ sep);
                result.append("  </tr>"+ sep);
            }
            result.append(createDocs(kb, kbHref, term, language)+ sep);
            result.append("</table>"+ sep);
            result.append("<table width=\"100%\">"+ sep);
            result.append(createDisplayNames(kb, kbHref, term, formatToken)+ sep);
            result.append(createSynonyms(kb, kbHref, term, formatToken)+ sep);
            result.append(createComments(kb, kbHref, term, language)+ sep);
            Set<String> parents = new HashSet<String>();
            sb1.append(createParents(kb, kbHref, term, language, parents));
            sb1.append(StringUtil.getLineSeparator());
            sb2.append(createChildren(kb, kbHref, term, language));
            sb2.append(StringUtil.getLineSeparator());
            if ((sb1.length() > 0) || (sb2.length() > 0)) {
                result.append("<tr class=\"title_cell\">"+ sep);
                result.append("  <td valign=\"top\" class=\"label\">"+ sep);
                result.append("    Relationships"+ sep);
                result.append("  </td>"+ sep);
                result.append("  <td>&nbsp;</td>"+ sep);
                result.append("  <td>&nbsp;</td>"+ sep);
                result.append("  <td>&nbsp;</td>"+ sep);
                result.append("</tr>"+ sep);
                // Parents
                result.append(sb1.toString());
                sb1.setLength(0);
                // Children
                result.append(sb2.toString());
                sb2.setLength(0);
            }
            ArrayList superComposites = findContainingComposites(kb, term); 
            Collections.sort(superComposites, String.CASE_INSENSITIVE_ORDER);
            result.append(createInstances(kb, kbHref, term, language, superComposites)+ sep);
            result.append(createRelations(kb, kbHref, term, language, formatToken)+ sep);
            result.append(createUsingSameComponents(kb, kbHref, term, language)+ sep);
            result.append(createBelongsToClass(kb, kbHref, term, language, parents)+ sep);
            if (!superComposites.isEmpty()) {
                String formattedContainingComposites = 
                    formatContainingComposites(kb,
                                               kbHref,
                                               superComposites,
                                               term,
                                               language);
                if (StringUtil.isNonEmptyString(formattedContainingComposites)) {
                    result.append("<tr>"+ sep);
                    result.append("  <td valign=\"top\" class=\"label\">"+ sep);
                    result.append("    Is Member of Composites"+ sep);
                    result.append("  </td>"+ sep);
                    result.append("  <td valign=\"top\" class=\"title_cell\">"+ sep);
                    result.append("    Composite Name"+ sep);
                    result.append("  </td>"+ sep);
                    result.append("  <td valign=\"top\" class=\"title_cell\">"+ sep);
                    result.append("    Description of Element Role"+ sep);
                    result.append("  </td>"+ sep);
                    result.append("  <td valign=\"top\" class=\"title_cell\">"+ sep);
                    result.append("    Cardinality"+ sep);
                    result.append("  </td>"+ sep);
                    result.append("  <td> &nbsp; </td>"+ sep);
                    result.append("</tr>"+ sep);
                    result.append(formattedContainingComposites+ sep);
                }
            }
            result.append("</table>"+ sep);
            result.append(generateHtmlFooter("")+ sep);
            result.append("  </body>"+ sep);
            result.append("</html>"+ sep);
            // result.append(createAllStatements(kb,kbHref,term,limit));
            output = result.toString();
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
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
            if (StringUtil.isNonEmptyString(ontology)) {
                ans.addAll(new HashSet<String>(kb.getTermsViaAskWithRestriction(0, 
                                                                                "ontologyNamespace", 
                                                                                1, 
                                                                                ontology,
                                                                                2)));
            }
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
            if (StringUtil.isNonEmptyString(ontology)) {
                List<String> delims = kb.getTermsViaAskWithRestriction(0, 
                                                                       "docGenNamespaceDelimiter", 
                                                                       1, 
                                                                       ontology,
                                                                       2);
                ans.addAll(new HashSet<String>(delims));
            }
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
        return ans;
    }

    /** *************************************************************
     * A List of currently known namespace prefixes.
     */
    protected ArrayList<String> namespacePrefixes = new ArrayList<String>();

    /** **************************************************************
     * Returns an ArrayList of all known namespace prefixes sorted by
     * length, from longest to shortest.
     *
     * @return A List of all known namespace prefixes
     */
    public ArrayList<String> getNamespacePrefixes() {

        try {
            if (namespacePrefixes.isEmpty()) {
                synchronized (namespacePrefixes) {
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
                    // System.out.println("");
                    // System.out.println("  namespacePrefixes == " + namespacePrefixes);
                    // System.out.println("");
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
    protected ArrayList<String> namespaces = new ArrayList<String>();
                
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
                        reduce.addAll(kb.kbCache.instanceOf.get("Namespace"));
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
        // System.out.println("");
        // System.out.println("  namespaces == " + namespaces);
        // System.out.println("");
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
            // if (StringUtil.isNonEmptyString(ontology)) {
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
                    reduce.addAll(kb.kbCache.instanceOf.get("Namespace"));
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
                    // System.out.println("");
                    // System.out.println("  namespaces == " + namespaces);
                    // System.out.println("  namespacePrefixes == " + namespacePrefixes);
                    // System.out.println("");
                }
            }
            // }
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
        /*
        try {
            if (StringUtil.isNonEmptyString(term)) {
                String kifTerm = StringUtil.w3cToKif(term);
                String prefix = ("ns" + StringUtil.getKifNamespaceDelimiter());
                if (!kifTerm.equals("ns") && !kifTerm.startsWith(prefix)) {
                    kifTerm = prefix + kifTerm;
                }
                String ns = null;
                String ontology = getOntology();
                if (StringUtil.isNonEmptyString(ontology)) {
                    for (Iterator it = getNamespaces(kb,ontology,false).iterator(); 
                         it.hasNext();) {
                        ns = (String) it.next();
                        if (ns.equalsIgnoreCase(kifTerm)) {
                            ans = ns;
                            break;
                        }
                    }
                }
            }
        }
        catch (Exception ex) {
            ex.printStackTrace();
        } */
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
                String prefix = null;
                for (Iterator it = getNamespacePrefixes().iterator(); it.hasNext();) {
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
    protected ArrayList getPredicates(KB kb, boolean requireNamespace) {

        ArrayList cached = null;
        try {
            cached = (ArrayList) getRelationsByKB().get(kb);
            if (cached == null) {
                TreeSet predSet = new TreeSet();
                Set classNames = kb.kbCache.instanceOf.get("Predicate");
                if (classNames == null)
                    return null;
                classNames.add("Predicate");
                classNames.add("BinaryPredicate");
                Iterator it = classNames.iterator();
                String cn = null;
                String p0 = null;
                String p1 = null;
                String p2 = null;
                String namespace = null;
                Iterator it2 = null;
                List predList = null;
                String ontology = getOntology();
                boolean isOntology = StringUtil.isNonEmptyString(ontology);
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
                        if (requireNamespace) {
                            namespace = getTermNamespace(kb, p1);
                            if (StringUtil.isNonEmptyString(namespace)
                                && isOntology
                                && getOntologyNamespaces(kb, ontology).contains(namespace)) {
                                // pred.contains(StringUtil.getKifNamespaceDelimiter())) {
                                predSet.add(p1);
                            }
                        }
                        else {
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
                        if (requireNamespace) {
                            namespace = getTermNamespace(kb, p0);
                            if (StringUtil.isNonEmptyString(namespace)
                                && isOntology
                                && getOntologyNamespaces(kb, ontology).contains(namespace)) {
                                predSet.add(p0);
                            }
                        }
                        else {
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
                            p1 = f.getStringArgument(1);
                            if (requireNamespace) {
                                namespace = getTermNamespace(kb, p1);
                                if (StringUtil.isNonEmptyString(namespace)
                                    && isOntology
                                    && getOntologyNamespaces(kb, ontology).contains(namespace)) {
                                    predSet.add(p1);
                                }
                            }
                            else {
                                predSet.add(p1);
                            }
                            p2 = f.getStringArgument(2);
                            if (requireNamespace) {
                                namespace = getTermNamespace(kb, p2);
                                if (StringUtil.isNonEmptyString(namespace)
                                    && isOntology
                                    && getOntologyNamespaces(kb, ontology).contains(namespace)) {
                                    predSet.add(p2);
                                }
                            }
                            else {
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
                List forms = kb.askWithRestriction(2, term, 0, "headword");
                if (forms.isEmpty())
                    forms = kb.askWithRestriction(2, term, 0, "termFormat");
                if (!forms.isEmpty()) {
                    String ctx = null;
                    Formula f = null;
                    for (int i = 0; i < contexts.size(); i++) {
                        ctx = (String) contexts.get(i);
                        for (int j = 0; j < forms.size(); j++) {
                            f = (Formula) forms.get(j);
                            if (f.getArgument(1).equals(ctx)) {
                                ans = f.getStringArgument(3);
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
                    Formula f = null;
                    if (StringUtil.isLocalTermReference(term) && (forms.size() == 1)) {
                        f = (Formula) forms.get(0);
                        ans = f.getStringArgument(3);
                    }
                    else {
                        if (contexts == null) 
                            contexts = new ArrayList();                        
                        List supers = getSuperComponents(kb, term);
                        contexts.addAll(supers);
                        contexts.add(0, term);
                        contexts.add(getDefaultNamespace());
                        if (!contexts.contains("EnglishLanguage")) 
                            contexts.add("EnglishLanguage");                        
                        String ctx = null;
                        Iterator itf = null;
                        for (Iterator itc = contexts.iterator(); itc.hasNext();) {
                            ctx = (String) itc.next();
                            for (itf = forms.iterator(); itf.hasNext();) {
                                f = (Formula) itf.next();
                                if (f.getStringArgument(2).equals(ctx)) {
                                    ans = f.getStringArgument(3);
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
            if (StringUtil.isNonEmptyString(term)) {
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
            if (StringUtil.isNonEmptyString(term)) {
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
            if (StringUtil.isNonEmptyString(term)) {
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
    protected String createSynonyms(KB kb, String kbHref, String term, String formatToken) {

        String result = "";
        try {
            if (isLegalForDisplay(term)) {
                ArrayList alternates = new ArrayList();
                if (StringUtil.isNonEmptyString(term)) {
                    alternates.addAll(kb.askWithRestriction(0, "synonym", 2, term));
                    alternates.addAll(kb.askWithRestriction(0, "headword", 2, term));
                    if (!alternates.isEmpty()) {
                        String presentationName = getTermPresentationName(kb, term);
                        String basePresentationName = stripNamespacePrefix(kb, presentationName);
                        ArrayList<String> synonyms = new ArrayList<String>();
                        Formula f = null;
                        String syn = null;
                        String hwsuff = "_hw";
                        int sidx = -1;
                        String namespace = null;
                        String prefix = null;
                        for (Iterator it = alternates.iterator(); it.hasNext();) {
                            f = (Formula) it.next();
                            namespace = f.getStringArgument(1);
                            prefix = stripNamespacePrefix(kb, namespace);
                            syn = StringUtil.removeEnclosingQuotes(f.getStringArgument(3));
                            if (!syn.equals(basePresentationName)) {
                                if (prefix.matches("^iso\\d+.*")) {
                                    sidx = prefix.lastIndexOf(hwsuff);
                                    if (sidx > -1)
                                        prefix = prefix.substring(0, sidx);
                                    syn = (prefix + StringUtil.getW3cNamespaceDelimiter() + syn);
                                }
                                synonyms.add(syn);
                            }
                        }
                        if (!synonyms.isEmpty()) {
                            sortByPresentationName(kb, getDefaultNamespace(), synonyms);
                            StringBuilder sb = new StringBuilder();
                            /*
                              if (formatToken.equalsIgnoreCase(F_DD2)) {
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
                            */
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
                            // }
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
     * @param formatToken A String token that partly determines the
     * format of the output
     * 
     * @return A String containing HTML markup, or an empty String if
     * term is supposed to be suppressed for display
     */
    protected String createDisplayNames(KB kb, String kbHref, String term, String formatToken) {

        String result = "";
        try {
            if (isLegalForDisplay(term) && !formatToken.equalsIgnoreCase(F_SI)) {
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
                    else {
                        if (labels.size() > 1)
                            Collections.sort(labels, String.CASE_INSENSITIVE_ORDER);
                        StringBuilder sb = new StringBuilder();
                        /*
                          if (formatToken.equalsIgnoreCase(F_DD2)) {
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
                        */
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
                        // }
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
                    hrefSB.append(showTermName(kb, extended, language));
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
                            hrefSB.append(showTermName(kb, extension, language));
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
                    if (!KButilities.isCacheFile(f.sourceFile)) {
                        s = f.getStringArgument(2);
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
                        hrefSB.append(showTermName(kb, parent, language));
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
                if (!KButilities.isCacheFile(f.sourceFile)) {
                    s = f.getStringArgument(1);
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
                                       + showTermName(kb, s, language) 
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
                        subent = (String) it.next();
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
                        if (!KButilities.isCacheFile(f.sourceFile)) {
                            inst = f.getStringArgument(1);
                            if (!excluded.contains(inst) && isLegalForDisplay(inst)) {
                                instances.add(inst);
                            }
                        }
                    }
                }
                instances.addAll(kb.kbCache.instanceOf.get(term));
                Set instSet = new HashSet();
                for (Iterator its = instances.iterator(); its.hasNext();) {
                    inst = (String) its.next();
                    if (!excluded.contains(inst) && isLegalForDisplay(inst)) {
                        instSet.add(inst);
                    }
                }
                // Remove duplicate strings, if any.
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
                        displayName = showTermName(kb, inst, language);
                        if (displayName.contains(inst)) {
                            xmlName = showTermName(kb, inst, "XMLLabel");
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
                     || kb.isFunction(currentTerm)
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
                sb.append(showTermName(kb, currentTerm, context));
                sb.append("</a>");
            }
            result = sb.toString();
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
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
                                     String formatToken) {
        /*
          System.out.println("ENTER DocGen.createRelations("
          + kb.name + ", "
          + kbHref + ", "
          + term + ", "
          + language + ", "
          + formatToken + ")");
        */
        String result = "";
        try {
            /*
              if (formatToken.equalsIgnoreCase(F_DD2)) {
              result = createRelations(kb, kbHref, term, language);
              }
              else {
            */
            if (isLegalForDisplay(term)) {
                String suffix = "";
                if (StringUtil.emptyString(kbHref)) 
                    suffix = ".html";
                ArrayList relations = getPredicates(kb,!formatToken.equalsIgnoreCase(F_SI));
                // System.out.println(StringUtil.getLineSeparator() + "relations == " 
                // + relations + StringUtil.getLineSeparator());
                if (relations != null && !relations.isEmpty()) {
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
                                    if (!KButilities.isCacheFile(f.sourceFile)) {
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
                                String relnHref = 
                                    ("<a href=\"" 
                                     + kbHref 
                                     + StringUtil.toSafeNamespaceDelimiter(kbHref, relation)
                                     + suffix 
                                     + "\">" 
                                     + showTermName(kb,relation,language) 
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
            // }
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
        /*
          System.out.println("EXIT DocGen.createRelations("
          + kb.name + ", "
          + kbHref + ", "
          + term + ", "
          + language + ", "
          + formatToken + ")");
        */
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
                cardVal = f.getStringArgument(2);
            }
            else {
                String minCard = "0";
                String maxCard = "n";
                cardForms = kb.askWithPredicateSubsumption("hasMinCardinality", 1, term);
                // kb.askWithRestriction(0,"minCardinality",2,term);
                if (cardForms != null && cardForms.size() > 0) {
                    Formula f = (Formula) cardForms.get(0);
                    // if (context == "" || context.equals(f.getArgument(1)))             
                    //     minCard = f.getArgument(3);
                    minCard = f.getStringArgument(2);
                }
                cardForms = kb.askWithPredicateSubsumption("hasMaxCardinality", 1, term);
                // kb.askWithRestriction(0,"maxCardinality",2,term);
                if (cardForms != null && cardForms.size() > 0) {
                    Formula f = (Formula) cardForms.get(0);
                    // if (context.equals("") || context.equals(f.getArgument(1)))             
                    //     maxCard = f.getArgument(3);
                    maxCard = f.getStringArgument(2);
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
     * @param indent A int value that determines the depth to which
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
                parentClass = f.getStringArgument(2);
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
                    String termFormat = StringUtil.removeEnclosingQuotes(f.getStringArgument(3));
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
                String dtToPrint = showTermName(kb, dataTypeName, language);                
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
     * @return A String providing a context-specific name for term,
     * possibly including HTML markup, or just term if no
     * context-specific form can be found or produced
     * 
     */
    public String showTermName(KB kb, String term, String language) {

    	//, boolean withSpanTags) {    
        String ans = term;
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
     
    public String showTermName(KB kb, String term, String language) {
        return showTermName(kb, term, language, false);
    }
*/
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
     * @param indent An integer indicating the depth or level to which
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
     * @param indent An int value that determines the depth to which
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
                    context = f.getStringArgument(2);
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
                        sb.append(showTermName(kb,containingComp,language));
                        sb.append("</a></td>");
                        sb.append(StringUtil.getLineSeparator());
                        sb.append("  <td valign=\"top\" class=\"cell\">");
                        sb.append(StringUtil.getLineSeparator());
                        sb.append("    ");
                        sb.append(processDocString(kb, 
                                                   kbHref, 
                                                   language, 
                                                   f.getStringArgument(3),
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
            if (StringUtil.isNonEmptyString(term)) {
                result.addAll(kb.getTermsViaPredicateSubsumption("syntacticSubordinate",
                                                                 1,
                                                                 term,
                                                                 2,
                                                                 true));
            }
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
                result.append(showTermName(kb, className, language));
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

    /** *************************************************************
     * Returns an ArrayList holding the composite entities (Elements)
     * that contain term, or returns an empty ArrayList.
     * 
     * @param kb The KB in which term is defined
     *
     * @param term A String that denotes a SUO-KIF term
     * 
     * @return An ArrayList containing the names of the Elements that
     * contain term.
     * 
     */
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
            for (int argnum = 2; argnum < 7; argnum++) {
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
                        result.append(NLGUtils.htmlParaphrase(kbHref,
                                form.getFormula(),
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
                    result.append(NLGUtils.htmlParaphrase(kbHref,
                            form.getFormula(),
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
                    result.append(NLGUtils.htmlParaphrase(kbHref,
                            form.getFormula(),
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
                    result.append(NLGUtils.htmlParaphrase(kbHref,
                            form.getFormula(),
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
                    result.append(NLGUtils.htmlParaphrase(kbHref,
                            form.getFormula(),
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
                    termToPrint = showTermName(kb, realTermName, language);
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
                    /*
                    sb.append(getTermRelevance(kb, getOntology(), realTermName).equals("message")
                              ? "&nbsp;&nbsp;&nbsp;&nbsp;MT&nbsp;&nbsp;&nbsp;&nbsp;"
                              : "");
                    */
                    sb.append("&nbsp;");
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
        System.out.println("  "
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
        System.out.println("  "
                           + ((System.currentTimeMillis() - t1) / 1000.0)
                           + " seconds elapsed time");
        return;
    }

    /** **************************************************************
     * Creates a subdirectory of SIGMA_HOME/files/, in which output
     * files of the type specified by token are saved.  token is also
     * used, along with the value returned by this.getKB() and the
     * current date, to create the directory name.  Returns a File
     * object representing the canonical pathname of the directory.
     *
     * @param token A token that indicates a type of output file
     *
     * @return A File representing a directory
     */
    protected File makeOutputDir(String token) {

        File outdir = null;
        try {
            File parentDir = this.getOutputParentDir();
            for (int i = 0; (((parentDir == null) 
                              || !parentDir.isDirectory() 
                              || !parentDir.canWrite())
                             && (i < 3)); i++) {
                String dirpath = "";
                switch (i) {
                case 0 : 
                    dirpath = System.getenv("SIGMA_HOME");
                    break;
                case 1 :
                    dirpath = KBmanager.getMgr().getPref("baseDir");
                    break;
                case 2 : 
                    dirpath = System.getProperty("user.dir");
                    break;
                }
                if (StringUtil.isNonEmptyString(dirpath)) {
                    dirpath = StringUtil.removeEnclosingQuotes(dirpath);
                    parentDir = new File(dirpath);
                    ArrayList<String> components =
                        new ArrayList<String>(Arrays.asList("files", 
                                                            this.getKB().name.toLowerCase()));
                    String component = null;
                    for (Iterator it = components.iterator();
                         ((parentDir.isDirectory() || parentDir.mkdirs())
                          && it.hasNext());) {
                        component = (String) it.next();
                        component = component.replaceAll(" ", "_");
                        if (!parentDir.getName().endsWith(component))
                            parentDir = new File(parentDir, component);
                    }
                }
            }
            String leafBasePath = (this.getKB().name.toUpperCase()
                                   + "_" 
                                   + StringUtil.removeEnclosingQuotes(token)
                                   + "_"
                                   + StringUtil.getDateTime("yyyyMMdd"));
            leafBasePath = leafBasePath.replaceAll(" ", "_");
            boolean isValidParent =
                ((parentDir != null) && parentDir.isDirectory() && parentDir.canWrite());
            int count = 0;
            String leafBasePlusCount = (leafBasePath + "_" + count);
            File leafBaseDir = null;
            if (isValidParent)
                leafBaseDir = new File(parentDir, leafBasePlusCount);
            else
                leafBaseDir = new File(leafBasePlusCount);
            while (leafBaseDir.exists()) {
                count++;
                leafBasePlusCount = (leafBasePath + "_" + count);
                if (isValidParent)
                    leafBaseDir = new File(parentDir, leafBasePlusCount);
                else
                    leafBaseDir = new File(leafBasePlusCount);
            }
            leafBaseDir.mkdirs();
            if (leafBaseDir.isDirectory() && leafBaseDir.canWrite()) 
                outdir = leafBaseDir;
        }
        catch (Exception ex) {
            if (outdir == null) {
                System.out.println("ERROR: DocGen.makeOutputDir(" + token + ") failed");
            }
            System.out.println(ex.getMessage());
            ex.printStackTrace();
        }
        return outdir;
    }

    /** *************************************************************
     * @param alphaList a TreeMap of TreeMaps of ArrayLists.  @see
     *                   createAlphaList()
     */
    protected TreeMap generateHTMLPages(KB kb, 
                                        TreeMap alphaList,
                                        String language,
                                        String formatToken) {

        long t1 = System.currentTimeMillis();
        System.out.println("ENTER DocGen.generateHTMLPages("
                           + kb.name + ", "
                           + "[map with " + alphaList.keySet().size() + " keys], "
                           // + "[map with " + inverseHeadwordMap.keySet().size() + " keys], "
                           + language + ", "
                           + formatToken + ")");
        TreeMap pageList = new TreeMap();
        TreeSet rejectedTerms = new TreeSet();
        try {
            String formattedTerm = null;
            List termNames = null;
            String realTermName = null;
            int count = 0;
            synchronized (kb.getTerms()) {
                for (Iterator it = kb.getTerms().iterator();
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
                                                             formatToken));
                        }
                        else {
                            pageList.put(realTermName,createPage(kb,
                                                                 "",
                                                                 realTermName,
                                                                 alphaList,
                                                                 200,
                                                                 language,
                                                                 formatToken));
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
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
        System.out.println("EXIT DocGen.generateHTMLPages("
                           + kb.name + ", "
                           + "[map with " + alphaList.keySet().size() + " keys], "
                           // + "[map with " + inverseHeadwordMap.keySet().size() + " keys], "
                           + language + ", "
                           + formatToken + ")");
        System.out.println("  " + rejectedTerms.size() + " terms rejected");
        System.out.println("  " 
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
    public void generateHTML(KB kb, String language, boolean simplified, String formatToken) {

        long t1 = System.currentTimeMillis();
        System.out.println("ENTER DocGen.generateHTML("
                           + kb.name + ", "
                           + language + ", "
                           + simplified + ", "
                           + formatToken + ")");
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
            // computeTermRelevance(kb, getOntology());
            // a TreeMap of TreeMaps of ArrayLists.  @see createAlphaList()
            TreeMap alphaList = getAlphaList(kb); // headwordMap
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
            String dir = getOutputDirectoryPath();
            // System.out.println("  INFO in DocGen.generateHTML(): saving index pages");
            saveIndexPages(kb, alphaList, dir, context);
            // System.out.println("  INFO in DocGen.generateHTML(): generating HTML pages");
            // Keys are formatted term names, values are HTML pages
            TreeMap pageList = generateHTMLPages(kb, 
                                                 alphaList, 
                                                 // inverseHeadwordMap, 
                                                 context, 
                                                 formatToken);
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
                           + formatToken + ")");
        System.out.println("  "
                           + ((System.currentTimeMillis() - t1) / 1000.0)
                           + " seconds elapsed time");
        return;
    }

    /** *************************************************************
     * Generate a single HTML page showing all terms.
     *
     * @param alphaList a TreeMap of TreeMaps of ArrayLists.
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
                                                          f.getStringArgument(3),
                                                          false, 
                                                          true);
                            }
                            if (StringUtil.isLocalTermReference(term) 
                                || StringUtil.emptyString(docStr))
                                continue;
                            pw.println("  <tr>");
                            // Term
                            pw.println("    <td valign=\"top\" class=\"cell\">");
                            printableTerm = (getSimplified()
                                             ? showTermName(kb, term, language)
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
                            /*
                            pw.println(getTermRelevance(kb, getOntology(), term).equals("message")
                                       ? "&nbsp;&nbsp;&nbsp;&nbsp;MT&nbsp;&nbsp;&nbsp;&nbsp;"
                                       : "");
                            */
                            pw.println("&nbsp;");
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
        System.out.println("  "
                + ((System.currentTimeMillis() - t1) / 1000.0)
                + " seconds elapsed time");
        return;
    }

    /** *************************************************************
     */
    public void generateSingleHTML(KB kb, String language, boolean simplified) 
        throws IOException {

        // HashMap headwordMap = createHeadwordMap(kb); 
        String dirpath = getOutputDirectoryPath();
        TreeMap alphaList = getAlphaList(kb); // headwordMap
        generateSingleHTML(kb, dirpath, alphaList, language, simplified);
    }

    /** *************************************************************
     * Returns a List containing the subordinate XmlAttributes of
     * kifTerm, else return an empty List.
     */
    protected ArrayList getSubordinateAttributes(KB kb, String kifTerm) {

        ArrayList attrs = new ArrayList();
        try {
            if (StringUtil.isNonEmptyString(kifTerm)) {
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
            if (StringUtil.isNonEmptyString(kifTerm)) {
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
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
        return elems;
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
            if (StringUtil.isNonEmptyString(term)) {
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
            if (StringUtil.isNonEmptyString(term)) {
                int idxArgnum = ((targetArgnum == 2) ? 1 : 2);
                result = kb.getTermsViaPredicateSubsumption("syntacticUnion",
                                                            idxArgnum,
                                                            term,
                                                            targetArgnum,
                                                            true);
                SetUtil.removeDuplicates(result);
            }
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
            if (StringUtil.isNonEmptyString(term)) {
                int idxArgnum = ((targetArgnum == 2) ? 1 : 2);
                result = kb.getTermsViaPredicateSubsumption("syntacticComposite",
                                                            idxArgnum,
                                                            term,
                                                            targetArgnum,
                                                            true);
                SetUtil.removeDuplicates(result);
            }
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
            if (StringUtil.isNonEmptyString(term)) {
                xmlType = kb.getFirstTermViaPredicateSubsumption("closestXmlDataType",
                                                                 1,
                                                                 term,
                                                                 2,
                                                                 false);
            }
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
        return xmlType;
    }

    /** *************************************************************
     * Supports memoization for isInstanceOf(kb, c1, c2).
     */
    protected static Map isInstanceOfCache = new HashMap();

    /** *************************************************************
     * Returns true if i is an instance of c, else returns false.
     *
     * @param kb A KB object
     * @param i A String denoting an instance
     * @param c A String denoting a Class
     * @return true or false
     */
    protected static boolean isInstanceOf(KB kb, String i, String c) {

        return kb.kbCache.instanceOf.get(i).contains(c);
    }

    /** *************************************************************
     * 
     */
    protected String getFirstDatatype(KB kb, String term) {

        String dtype = null;
        try {
            if (StringUtil.isNonEmptyString(term)) {
                List types = getDatatypeTerms(kb, term, 2);
                if (!types.isEmpty()) {
                    dtype = (String) types.get(0);
                }
            }
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
        return dtype;
    }

    /** *************************************************************
     * 
     */
    protected ArrayList getDatatypeTerms(KB kb, String term, int targetArgnum) {

        ArrayList result = null;
        try {
            if (StringUtil.isNonEmptyString(term)) {
                int idxArgnum = ((targetArgnum == 2) ? 1 : 2);
                result = kb.getTermsViaPredicateSubsumption("datatype",
                                                            idxArgnum,
                                                            term,
                                                            targetArgnum,
                                                            true);
                SetUtil.removeDuplicates(result);
            }
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
            if (StringUtil.isNonEmptyString(term)) {
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
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
        return ans;
    }

    /** *************************************************************
     * 
     */
    public String getTermPresentationName(KB kb, String term) {

        String name = term;
        try {
            if (StringUtil.isNonEmptyString(term)) {
                String namespace = this.getDefaultNamespace();
                if (namespace == null) namespace = "";
                name = getTermPresentationName(kb, namespace, term);
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
            if (StringUtil.emptyString(name) || name.equals(term)) {
                name = showTermName(kb, term, namespace);
            }
            if (StringUtil.isNonEmptyString(namespace)) {
                name = stripNamespacePrefix(kb, name);
            }
            name = StringUtil.removeEnclosingQuotes(name);
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
    public void sortByPresentationName(KB kb, String namespaceTerm, List stringList) {
        /*
          System.out.println("ENTER DocGen.sortByPresentationName("
          + kb.name + ", "
          + namespaceTerm + ", "
          + stringList + ")");
        */
        try {
            if (!SetUtil.isEmpty(stringList) && (stringList.size() > 1)) {
                List<String[]> sortable = new ArrayList<String[]>();
                String kifNamespace = (StringUtil.emptyString(namespaceTerm)
                                       ? ""
                                       : toKifNamespace(kb, namespaceTerm));
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
                //String nsTerm = toKifNamespace(kb, namespace);
                String nsTerm = namespace;
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
     * */
    public ArrayList<ArrayList<String>> readSpreadsheetFile(String inpath,
                                                            char delimiter) {
        return readSpreadsheetFile(inpath, delimiter, 0, null);
    }

    /** *******************************************************************
     * Parses a file of delimited fields into an ArrayList of
     * ArrayLists.  If rowFlags contains any Strings, the method
     * concatenates sequential lines that do not start with one of the
     * Strings, else rowFlags is ignored.
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
    public ArrayList<ArrayList<String>> readSpreadsheetFile(String inpath,
                                         char delimiter, 
                                         int delimitersPerRow,
                                         List<String> rowFlags) {

        ArrayList<ArrayList<String>> result = null;
        ArrayList<String> rows = new ArrayList<String>();
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
                // 1.a. Use explicit leading tokens to determine when a new row starts.
                if (useRowFlags) {
                    isRowStart = false;
                    for (String token : rowFlags) {
                        if (line.startsWith(token)) {
                            isRowStart = true;
                            break;
                        }
                    }
                    if (isRowStart)
                        rows.add(line);
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
                                           + inpath + ", " + delimiter + ", "
                                           + delimitersPerRow + ", " + rowFlags + ")");
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
                result = convertRowStringsToLists(rows, delimiter);
            }
        }
        catch (Exception ex) {
            ex.printStackTrace();
        } 
        return result;
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
    protected ArrayList<ArrayList<String>> convertRowStringsToLists(List<String> rows, char delimiter) {

        ArrayList<ArrayList<String>> result = new ArrayList<ArrayList<String>>();
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
                ArrayList<String> row = new ArrayList<String>();
                if (StringUtil.isNonEmptyString(line)) {
                    inString = false;
                    cell = new StringBuilder();
                    for (int j = 0; j < line.length(); j++) {
                        ch = line.charAt(j);
                        if (ch == delimiter && !inString) {
                            row.add(cell.toString().trim());
                            cell = new StringBuilder();
                        }
                        else if (ch == '"'
                                   && (j == 0 || line.charAt(j-1) != '\\')) {
                            inString = !inString;
                        }
                        else  {
                            cell.append(ch);
                        }
                    }
                    row.add(cell.toString().trim());
                }
                // row will be an empty ArrayList if line is an empty
                // String.
                result.add(row);
            }
            System.out.println(SP2 + rows.size() + " rows saved");
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
        return result;
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
    protected int countDelimiters(String line, char delimiter) {

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

    /** *************************************************************
     * 
     */
    public static void main (String[] args) {

        String status = "";
        String ls = StringUtil.getLineSeparator();
        try {
            // Nothing here yet.
        }
        catch (Throwable th) {
            System.out.println(status);
            System.out.println(th.getMessage());
            th.printStackTrace();
        }
        return;
    }
} // DocGen.java
