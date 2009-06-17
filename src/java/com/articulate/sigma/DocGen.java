/** This code is copyrighted by Articulate Software (c) 2007.
It is released under the GNU Public License &lt;http://www.gnu.org/copyleft/gpl.html&gt;.
Users of this code also consent, by use of this code, to credit Articulate Software in any
writings, briefings, publications, presentations, or other representations of any
software which incorporates, builds on, or uses this code.
Please cite the following article in any publication with references:

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

    /** ***************************************************************
     * The default DocGen object.
     *
     * @see getInstance()
     */
    protected static DocGen DEFAULT_INSTANCE = null;

    /** ***************************************************************
     * The default base plus file suffix name for the main index file
     * for a set of HTML output files.
     */
    protected static String INDEX_FILE_NAME = "index.html";

    /** ***************************************************************
     * To obtain an instance of DocGen, use the static factory method
     * getInstance().
     */
    protected DocGen() {
    }

    /** ***************************************************************
     * Returns the default line separator token for the current
     * runtime platform.
     */
    public String getLineSeparator() {
        return System.getProperty("line.separator");
    }

    /** ***************************************************************
     * Ths static factory method to be used to obtain a DocGen
     * instance.
     */
    public static DocGen getInstance() {
        if (DEFAULT_INSTANCE == null) {
            DEFAULT_INSTANCE = new DocGen();
        }
        return DEFAULT_INSTANCE;
    }

    /** ***************************************************************
     * The keys of this Map are SUO-KIF terms.  The values are Sets of
     * SUO-KIF terms.  Each key term is "contained" by, or somehow
     * central to the definition of, the terms in the corresponding
     * Set.
     */
    protected HashMap coreTerms = new HashMap();

    /** ***************************************************************
     * The keys of this Map are SUO-KIF terms.  The values are Sets of
     * SUO-KIF terms.  Each key term is part of the general vocabulary
     * framework that defines other terms considered more central or
     * "core" to the definitions of the terms in the corresponding
     * Set.  In other words, each key is only indirectly and
     * syntactically relevant to the definition of the terms in the
     * corresponding Set.
     */
    private HashMap frameworkTerms = new HashMap();

    /** ***************************************************************
     * The keys of this Map are SUO-KIF terms.  The values are Sets of
     * SUO-KIF terms.  Each key term is part of the supporting
     * vocabulary that defines other terms considered more central or
     * "core" to the definitions of the terms in the corresponding
     * Set.  In other words, each key is only indirectly semantically
     * relevant to the definition of the terms in the corresponding
     * Set.
     */
    private HashMap supportingTerms = new HashMap();

    /** ***************************************************************
     * Each key in this Map is a top-level focalTerm in this KB and
     * ontology.  The corresponding value is a String representation
     * of the int value that indicates the computed maximum "depth",
     * or number of nodes in the longest path, along
     * syntacticSubordinate from the focalTerm.
     */
    private HashMap maxDepthFromFocalTerm = new HashMap();

    /** The default KB to use for document generation */
    private KB defaultKB = null;

    /** ***************************************************************
     * Sets the default KB for this DocGen instance.
     */
    public void setDefaultKB(KB kb) {
        this.defaultKB = kb;
        return;
    }

    /** ***************************************************************
     * Returns the default KB for this DocGen instance, or null if no
     * default KB value has been set.
     */
    public KB getDefaultKB() {
        return defaultKB;
    }

    /** The default namespace to use for document generation */
    private String defaultNamespace = "";

    /** ***************************************************************
     * Sets the default namespace for this DocGen instance.
     *
     * @param suoKifTerm A SUO-KIF term name denoting a namespace.
     */
    public void setDefaultNamespace(String suoKifTerm) {
        defaultNamespace = suoKifTerm;
        return;
    }

    /** ***************************************************************
     * Returns a SUO-KIF term denoting the default namespace for this
     * DocGen instance, else returns an empty String if no default
     * namespace value has been set.
     */
    public String getDefaultNamespace() {
        return defaultNamespace;
    }

    /** The document title text to be used for HTML generation */
    private String titleText = "";

    /** ***************************************************************
     * Sets the title text to be used during HTML document generation.
     *
     * @param titlestr A String that will be used as the HTML document
     * title
     */
    public void setTitleText(String titlestr) {
        titleText = titlestr;
        return;
    }

    /** ***************************************************************
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

    /** ***************************************************************
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

    /** ***************************************************************
     * Returns the String that will be used as the footer text for
     * HTML document generation, else returns an empty String if no
     * footer text value has been set.
     */
    public String getFooterText() {
        return footerText;
    }

    /** The style sheet (CSS filename) to be referenced in HTML generation */
    private String styleSheet = "simple.css";

    /** ***************************************************************
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

    /** ***************************************************************
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

    /** ***************************************************************
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

    /** ***************************************************************
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

    /** ***************************************************************
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

    /** ***************************************************************
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

    /** ***************************************************************
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

    /** ***************************************************************
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

    /** ***************************************************************
     * A Map in which the keys are SUO-KIF terms and the values are
     * Lists.  Each element in a List is a "top level" term in a
     * subsumption graph formed by syntacticSubordinate or its
     * subrelations.  The keys are the focal terms for this DocGen
     * instance (e.g., MessageTypes or namespaces) .
     *
     */
    private Map topLevelTerms = null;

    /** ***************************************************************
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

    /** ***************************************************************
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

    /** ***************************************************************
     * Sets the Map to be used for HTML character entity to ASCII 
     * replacements.
     */
    public void setStringReplacementMap(Map keyValPairs) {
        this.stringReplacementMap = keyValPairs;
        return;
    }

    /** ***************************************************************
     * Returns the Map to be used for HTML character entity to ASCII
     * replacements, attempting to build it from
     * docGenCodeMapTranslation statements found in the KB if the Map
     * does not already exist.
     */
    public Map getStringReplacementMap() {
        try {
            if (stringReplacementMap == null) {
                Map srMap = new HashMap();
                KB kb = getDefaultKB();
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

    /** ***************************************************************
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

    /** ***************************************************************
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

    /** ***************************************************************
     * Returns a Set containing the names of those predicates for
     * which diplay should be suppressed, and tries to create the Set
     * from docGenInhibitDisplayRelations statements found in the
     * current KB if the Set does not already exist.
     *
     * @return a Set of predicate names
     *
     */
    public Set getInhibitDisplayRelations() {
        try {
            if (inhibitDisplayRelations == null) {
                KB kb = getDefaultKB();
                String onto = getOntology();
                Set idr = new TreeSet();
                if ((kb != null) && StringUtil.isNonEmptyString(onto)) {
                    String arg2 = 
                        kb.getFirstTermViaAskWithRestriction(0,
                                                             "docGenInhibitDisplayRelations",
                                                             1,
                                                             onto,
                                                             2);
                    if (StringUtil.isNonEmptyString(arg2)) {
                        List relnList = StringUtil.kifListToArrayList(arg2);
                        if (!relnList.isEmpty()) {
                            if (((String) relnList.get(0)).equals("ListFn")) {
                                relnList.remove(0);
                            }
                        }
                        idr.addAll(relnList);
                    }
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

    /** ***************************************************************
     * The header to be used for the the table of contents (or index
     * list) section during HTML generation.
     */
    private String tocHeader = "";

    /** ***************************************************************
     * Sets the String header to be used in generated HTML files to
     * header.
     */
    public void setTocHeader(String header) {
        this.tocHeader = header;
        return;
    }

    /** ***************************************************************
     * Returns the String header to be used in generated HTML files.
     */
    public String getTocHeader() {
        return this.tocHeader;
    }

    /** ***************************************************************
     * If true, a termFormat value obtained for term will be displayed
     * rather than the term name itself.
     */
    public boolean simplified = false;

    /** ***************************************************************
     * A Map in which each key is a KB name and the corresponding
     * value is a List of the Predicates defined in the KB.
     */
    private HashMap relationsByKB = new HashMap();

    /** ***************************************************************
     * A List of currently known namespaces.
     */
    private ArrayList<String> namespaces = null;

    /** ***************************************************************
     * A List of currently known namespace prefixes.
     */
    private ArrayList<String> namespacePrefixes = new ArrayList<String>();

    /** ***************************************************************
     * A Set of Strings.
     */
    private Set codedIdentifiers = null;

    /** ***************************************************************
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

    /** ***************************************************************
     * The parent directory for target subdirectories of HTML, XSD,
     * and other types of files generated by this DocGen object.
     */
    private File outputParentDir = new File(KBmanager.getMgr().getPref("baseDir"));

    /** ***************************************************************
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

    /** ***************************************************************
     * Sets the parent directory in which subdirectories for different
     * types of output files will be created to the File obj, and
     * tries to create the directory pathname if it does not already
     * exist.
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

    /** ***************************************************************
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

    /** ***************************************************************
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

    /** ***************************************************************
     * Returns a File object representing the directory in which the
     * subdirectories for the various types of output files will be
     * located.
     */
    public File getOutputParentDir() {
        return this.outputParentDir;
    }

    /** ***************************************************************
     * The ontology for which this DocGen object is being used to
     * generate files.
     */
    private String ontology = null;

    /** ***************************************************************
     * Sets the ontology associated with this DocGen object to term.
     *
     * @param term A SUO-KIF term denoting an ontology
     */
    public void setOntology(String term) {
        this.ontology = term;
        return;
    }

    /** ***************************************************************
     * Returns a SUO-KIF term (String) denoting the ontology
     * associated with this DocGen object.
     */
    public String getOntology() {
        return this.ontology;
    }

    /** ***************************************************************
     * The DisplayFilter which, if present, determines if a given
     * SUO-KIF object may be displayed or output by this DocGen
     * object.
     */
    private DisplayFilter displayFilter = null;

    /** ***************************************************************
     * Sets the DisplayFilter associated with this DocGen object to
     * filterObj.
     *
     * @param filterObj An instance of DisplayFilter
     */
    public void setDisplayFilter(DisplayFilter filterObj) {
        this.displayFilter = filterObj;
        return;
    }

    /** ***************************************************************
     * Returns the DisplayFilter object associated with this DocGen
     * object, or null if no DisplayFilter has been set.
     */
    public DisplayFilter getDisplayFilter() {
        return this.displayFilter;
    }

    /** ***************************************************************
     * Returns a String of HTML markup for the start of a document, using title as
     * the document title String.
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
            sb.append(getLineSeparator());
            sb.append("  <head>");
            sb.append(getLineSeparator());
            sb.append("    <meta http-equiv=\"Content-Type\" ");
            sb.append("content=\"text/html; charset=utf-8\">");
            sb.append(getLineSeparator());
            if (StringUtil.isNonEmptyString(cssf)) {
                sb.append("    <link rel=\"stylesheet\" type=\"text/css\" href=\"");
                sb.append(cssf);
                sb.append("\">");
                sb.append(getLineSeparator());
            }
            if (StringUtil.isNonEmptyString(docTitle)) {
                sb.append("    <title>");
                sb.append(docTitle);
                sb.append("</title>");
                sb.append(getLineSeparator());
            }
            sb.append("  </head>");
            sb.append(getLineSeparator());
            sb.append("  <body>");
            sb.append(getLineSeparator());
            result = sb.toString();
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
        return result;
    }

    /** ***************************************************************
     * Returns a String of HTML markup encoding the footer section of
     * an HTML document, and using footerText as the text to be
     * displayed at the bottom of the page.
     *
     * @param footerText The text String to be diplayed at the bottom
     * of an HTML document
     *
     * @return A String of HTML markup
     */
    private String generateHtmlFooter(String footerText) {
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
            sb.append(getLineSeparator());
            sb.append("  <tr class=\"title\">");
            sb.append(getLineSeparator());
            sb.append("    <td>");
            // sb.append(getLineSeparator());
            sb.append(text);
            // sb.append(getLineSeparator());
            sb.append("    </td>");
            sb.append(getLineSeparator());
            sb.append("  </tr>");
            sb.append(getLineSeparator());
            sb.append("</table>");
            sb.append(getLineSeparator());
            result = sb.toString();
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
        return result;
    }

    /** **************************************************************
     * Returns true if term is an instance or subclass of
     * CompositeContentBearingObject in kb, else returns false.
     *
     * @param kb The KB in which to check the definition of term
     *
     * @param term A String that names a SUO-KIF term
     *
     * @return true or false
     */
    public boolean isComposite(KB kb, String term) {
        boolean ans = false;
        try {
            ans = (isInstanceOf(kb, term, "CompositeContentBearingObject")
                   || isSubclass(kb, term, "CompositeContentBearingObject")
                   || isInstanceOf(kb, term, "CompositeContentBearingObjectType"));
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
        return ans;
    }

    /** **************************************************************
     * Returns true if statements that include term and occur in the
     * kb and ontology associated with this DocGen object may be
     * displayed or output (at all, in any form).
     *
     * @return true or false
     */
    private boolean isLegalForDisplay(String term) {
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
    private void removeByPattern(List seq, String regex) {
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
    private void retainByPattern(List seq, String regex) {
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
     * defined in, or especially pertinent to, ontology, as specified
     * by statements formed with the SUO-KIF predicate
     * ontologyNamespace that occur in kb.
     *
     * @param kb The KB in which statements will be checked
     *
     * @param ontology The name of the ontology that will be checked
     *
     * @return A List of the SUO-KIF terms that denote namespaces and
     * occur in statements formed with ontologyNamespace in kb
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
            if ((namespaces == null) || namespaces.isEmpty() || force) {
                if (namespaces == null) {
                    namespaces = new ArrayList<String>();
                }
                namespaces.clear();
                List<String> terms = kb.getTermsViaAsk(0, "inNamespace", 2);
                List<String> terms2 = new ArrayList<String>();
                if (StringUtil.emptyString(ontology)) {
                    ontology = getOntology();
                }
                if (StringUtil.isNonEmptyString(ontology)) {
                    terms2.addAll(getOntologyNamespaces(kb, ontology));
                }
                HashSet<String> hs = new HashSet<String>();
                hs.addAll(terms);
                hs.addAll(terms2);
                namespaces.addAll(hs);
                if (!namespaces.isEmpty()) {
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
                    Collections.sort(namespaces, comp);
                    if (namespacePrefixes == null) {
                        namespacePrefixes = new ArrayList<String>();
                    }
                    namespacePrefixes.clear();
                    List<String> delims = Arrays.asList(DB.getKifNamespaceDelimiter(), 
                                                        DB.getW3cNamespaceDelimiter());
                    List<String> nsPrefs = Arrays.asList("ns" + DB.getKifNamespaceDelimiter(),
                                                         "ns" + DB.getW3cNamespaceDelimiter());
                    String prefix = null;
                    for (String term : namespaces) {
                        prefix = term;
                        for (String delim : delims) {
                            if (prefix.endsWith(delim)) {
                                prefix = prefix.substring(0, prefix.length() - delim.length());
                                break;
                            }
                        }
                        for (String nsPref : nsPrefs) {
                            if (prefix.startsWith(nsPref)) {
                                prefix = prefix.substring(nsPref.length());
                                break;
                            }
                        }
                        namespacePrefixes.add(prefix + DB.getKifNamespaceDelimiter());
                        namespacePrefixes.add(prefix + DB.getW3cNamespaceDelimiter());
                    }
                    Collections.sort(namespacePrefixes, comp);
                }
                System.out.println("");
                System.out.println("namespaces == " + namespaces);
                System.out.println("namespacePrefixes == " + namespacePrefixes);
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
    private String getNamespacePrefix(KB kb, String term) {
        String ans = "";
        if (StringUtil.isNonEmptyString(term)) {
            if (namespacePrefixes == null) {
                getNamespaces(kb, getOntology(), true);
            }
            String prefix = null;
            for (Iterator it = namespacePrefixes.iterator(); it.hasNext();) {
                prefix = (String) it.next();
                if (term.startsWith(prefix)) {
                    ans = prefix;
                    break;
                }
            }
        }
        return ans;
    }

    /** **************************************************************
     * Returns term without its namespace prefix if it appears to have
     * one in kb, else just returns term.
     */
    private String stripNamespacePrefix(KB kb, String term) {
        String ans = term;
        String prefix = getNamespacePrefix(kb, term);
        if (StringUtil.isNonEmptyString(prefix)) {
            ans = term.substring(prefix.length());
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
    private String toKifNamespace(KB kb, String term) {
        String ans = term;
        try {
            String kifTerm = DB.w3cToKif(term);
            String prefix = ("ns" + DB.getKifNamespaceDelimiter());
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
    private String removeLinkableNamespacePrefixes(KB kb, String text) {
        String ans = text;
        try {
            if (StringUtil.isNonEmptyString(text)) {
                if (namespacePrefixes == null) {
                    getNamespaces(kb, getOntology(), true);
                }
                String prefix = null;
                for (Iterator it = namespacePrefixes.iterator(); it.hasNext();) {
                    prefix = (String) it.next();
                    if (prefix.endsWith(DB.getKifNamespaceDelimiter())) {
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
    private Set getCodedIdentifiers(KB kb) {
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

    /** ***************************************************************
     *  Collects and returns a List of all Predicates in kb.
     *
     * @param kb The KB from which to gather all terms that are
     * instances of BinaryPredicate
     *
     * @return A List of BinaryPredicates (Strings)
     */
    private ArrayList getPredicates(KB kb) {
        ArrayList cached = null;
        try {
            cached = (ArrayList) relationsByKB.get(kb);
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
                            // pred.contains(DB.getKifNamespaceDelimiter())) {
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
                relationsByKB.put(kb, cached);
            }
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
        return cached;
    }

    /** ***************************************************************
     * Returns true if term has syntactic subcomponents such as XML
     * elements or XML attributes in kb, else returns false.
     *
     * @param kb The KB in which term is defined
     *
     * @param term A String denoting a SUO-KIF constant name
     * 
     * @return true or false
     */
    private boolean hasSubComponents(KB kb, String term) {
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

    /** ***************************************************************
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
    private ArrayList getSubComponents(KB kb, String term) {
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
 
    /** ***************************************************************
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
    private ArrayList getSuperComponents(KB kb, String term) {
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
 
    /** ***************************************************************
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
    private String getFirstTermFormat(KB kb, String term, List contexts) {
        String ans = null;
        try {
            if (StringUtil.isNonEmptyString(term)) {
                List forms = kb.askWithRestriction(2, term, 0, "termFormat");
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
                    if ((ans == null) && DB.isLocalTermReference(term)) {
                        String moreGeneralTerm = getFirstGeneralTerm(kb, term);
                        if (StringUtil.isNonEmptyString(moreGeneralTerm)) {
                            ans = getFirstTermFormat(kb, moreGeneralTerm, contexts);
                        }
                    }
                }
                if (ans == null) { ans = term; }
                ans = StringUtil.removeEnclosingQuotes(ans);
            }
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
        return ans;
    }

    /** ***************************************************************
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
    private String getContextualDocumentation(KB kb, String term, List contexts) {
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

    /** ***************************************************************
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
    private String getNearestContainingClass(KB kb, String term) {
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

    /** ***************************************************************
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
    private String getFirstSubsumingTerm(KB kb, String term) {
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

    /** ***************************************************************
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
    private String getFirstGeneralTerm(KB kb, String term) {
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

    /** ***************************************************************
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
    private ArrayList<String> getFirstGeneralTerms(KB kb, String term) {
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

    /** ***************************************************************
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
    private ArrayList<String> getFirstSpecificTerms(KB kb, String term) {
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

    /** ***************************************************************
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
    private ArrayList<String> getSyntacticSubordinateTerms(KB kb, String term) {
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

    /** ***************************************************************
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
    private ArrayList getFirstInstances(KB kb, String term) {
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

    /** ***************************************************************
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
    private String getFirstContainingClass(KB kb, String term) {
        return kb.getFirstTermViaPredicateSubsumption("instance",
                                                      1,
                                                      term,
                                                      2,
                                                      true);
    }

    /** ***************************************************************
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
    private ArrayList getFirstSubClasses(KB kb, String term) {
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

    /** ***************************************************************
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
    private String createDocs(KB kb, String kbHref, String term, String language) {
        String markup = "";
        try {
            if (isLegalForDisplay(term)) {
                StringBuilder result = new StringBuilder();
                ArrayList context = new ArrayList();
                context.add(language);
                String docString = getContextualDocumentation(kb, term, context);
                docString = processDocString(kb, kbHref, language, docString, false, true);
                result.append("<tr>");
                result.append(getLineSeparator());
                result.append("  <td valign=\"top\" class=\"description\">");
                result.append(getLineSeparator());
                result.append("    ");
                result.append(docString);
                result.append(getLineSeparator());
                result.append("  </td>"); 
                result.append(getLineSeparator());
                result.append("</tr>");
                
                markup = result.toString();
            }
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
        return markup;
    }

    /** ***************************************************************
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
    private String createComments(KB kb, String kbHref, String term, String language) {

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
                result.append(getLineSeparator());
                result.append("  <td valign=\"top\" class=\"label\">Comments</td>");

                String docString = null;
                for (int i = 0; i < docs.size(); i++) {
                    docString = (String) docs.get(i);
                    docString = processDocString(kb, kbHref, language, docString, false, true);
                    if (i > 0) {
                        result.append("<tr>");
                        result.append(getLineSeparator());
                        result.append("  <td>&nbsp;</td>");
                        result.append(getLineSeparator());
                    }
                    result.append("  <td valign=\"top\" colspan=\"2\" class=\"cell\">");
                    result.append(getLineSeparator());
                    result.append("      ");
                    result.append(docString);
                    result.append("<br/>");
                    result.append(getLineSeparator());
                    result.append("  </td>");
                    result.append(getLineSeparator());
                    result.append("</tr>");
                    result.append(getLineSeparator());
                }
            }
        }
        return result.toString();
    }

    /** ***************************************************************
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
    private String createSynonyms(KB kb, String kbHref, String term) {

        StringBuilder result = new StringBuilder();
        try {
            if (isLegalForDisplay(term)) {
                ArrayList alternates = new ArrayList();
                if (StringUtil.isNonEmptyString(term)) {
                    alternates.addAll(kb.askWithRestriction(0, "synonym", 2, term));
                }
                if (!alternates.isEmpty()) {
                    String namespace = null;
                    boolean found = false;
                    Formula f = null;
                    ArrayList<String> synonyms = new ArrayList<String>();
                    for (Iterator it = alternates.iterator(); it.hasNext();) {
                        f = (Formula) it.next();
                        // namespace = f.getArgument(1);
                        // if (namespace.endsWith("syn")) {
                        synonyms.add(StringUtil.removeEnclosingQuotes(f.getArgument(3)));
                        // }
                    }
                    if (!synonyms.isEmpty()) {
                        result.append("<tr>");
                        result.append(getLineSeparator());
                        result.append("  <td valign=\"top\" class=\"cell\">");
                        result.append("<strong>Synonyms(s)</strong>");
                        boolean isFirst = true;
                        for (String syn : synonyms) {
                            result.append(isFirst ? " " : ", ");
                            isFirst = false;
                            result.append("<i>");
                            result.append(syn);
                            result.append("</i>");
                        }
                        result.append("</td>");
                        result.append(getLineSeparator());
                        result.append("</tr>");
                        result.append(getLineSeparator());
                    }
                }
            }
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
        return result.toString();
    }

    /** ***************************************************************
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
    private String createHasSameComponents(KB kb, 
                                           String kbHref, 
                                           String term, 
                                           String language) {

        StringBuilder result = new StringBuilder();
        if (isLegalForDisplay(term)) {
            String suffix = "";
            if (StringUtil.emptyString(kbHref)) 
                suffix = ".html";
            ArrayList<String> extensionOfs = getSyntacticExtensionTerms(kb, term, 2, true);
            if (!extensionOfs.isEmpty()) {
                result.append("<tr>");
                result.append(getLineSeparator());
                result.append("  <td valign=\"top\" class=\"label\">");
                result.append(getLineSeparator());
                result.append("    Has Same Components As");
                result.append(getLineSeparator());
                result.append("  </td>");
                result.append(getLineSeparator());
                boolean isFirst = true;
                StringBuilder hrefSB = new StringBuilder();
                for (String extended : extensionOfs) {
                    hrefSB.setLength(0);
                    hrefSB.append("<a href=\"");
                    hrefSB.append(kbHref);
                    hrefSB.append(extended);
                    hrefSB.append(suffix);
                    hrefSB.append("\">");
                    hrefSB.append(showTermName(kb, extended, language, true));
                    hrefSB.append("</a>");
                    if (isFirst) {
                        result.append("  <td valign=\"top\" class=\"cell\">");
                        result.append(getLineSeparator());
                        isFirst = false;
                    }
                    result.append("    ");
                    result.append(hrefSB.toString());
                    result.append("<br/>");
                    result.append(getLineSeparator());
                }
                result.append("  </td>");
                result.append(getLineSeparator());
                result.append("</tr>");
                result.append(getLineSeparator());
            }
        }
        return result.toString();
    }

    /** ***************************************************************
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
    private String createUsingSameComponents(KB kb, 
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
                        result.append(getLineSeparator());
                        result.append("  <td valign=\"top\" class=\"label\">");
                        result.append(getLineSeparator());
                        result.append("    ");
                        result.append("Composites Using Same Components");
                        result.append(getLineSeparator());
                        result.append("  </td>");
                        result.append(getLineSeparator());
                        String extension = null;
                        boolean isFirst = true;
                        StringBuilder hrefSB = new StringBuilder();
                        for (Iterator it = extensions.iterator(); it.hasNext();) {
                            extension = (String) it.next();
                            hrefSB.setLength(0);
                            hrefSB.append("<a href=\"");
                            hrefSB.append(kbHref);
                            hrefSB.append(extension);
                            hrefSB.append(suffix);
                            hrefSB.append("\">");
                            hrefSB.append(showTermName(kb, extension, language, true));
                            hrefSB.append("</a>");
                            if (isFirst) {
                                result.append("  <td valign=\"top\" class=\"cell\">");
                                result.append(getLineSeparator());
                                isFirst = false;
                            }
                            result.append("    ");
                            result.append(hrefSB.toString());
                            result.append("<br/>");
                            result.append(getLineSeparator());
                        }
                        result.append("  </td>");
                        result.append(getLineSeparator());
                        result.append("</tr>");
                        result.append(getLineSeparator());
                    }
                }
            }
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
        return result.toString();
    }

    /** ***************************************************************
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
    private String createParents(KB kb, 
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
                    sb.append(getLineSeparator());
                    sb.append("  <td valign=\"top\" class=\"label\">Parents</td>");
                    sb.append(getLineSeparator());
                    StringBuilder hrefSB = new StringBuilder();
                    boolean isFirst = true;
                    for (String parent : sorted) {
                        hrefSB.setLength(0);
                        hrefSB.append("<a href=\"");
                        hrefSB.append(kbHref);
                        hrefSB.append(parent);
                        hrefSB.append(suffix);
                        hrefSB.append("\">");
                        hrefSB.append(showTermName(kb, parent, language, true));
                        hrefSB.append("</a>");
                        if (!isFirst) {
                            sb.append("<tr>");
                            sb.append(getLineSeparator());
                            sb.append("  <td>&nbsp;</td>");                
                            sb.append(getLineSeparator());
                        }
                        isFirst = false;
                        sb.append("  <td valign=\"top\" class=\"cell\">");
                        sb.append(hrefSB.toString());
                        sb.append("</td>");
                        sb.append(getLineSeparator());
                        String docStr = getContextualDocumentation(kb, parent, null);
                        sb.append("  <td valign=\"top\" class=\"cell\">");
                        sb.append(getLineSeparator());
                        sb.append("    ");
                        sb.append(processDocString(kb, 
                                                   kbHref, 
                                                   language, 
                                                   docStr,
                                                   false,
                                                   true));
                        sb.append(getLineSeparator());
                        sb.append("  </td>");
                        sb.append(getLineSeparator());
                    }
                    sb.append("</tr>");
                    sb.append(getLineSeparator());
                    result = sb.toString();
                }
            }
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
        return result;
    }

    /** ***************************************************************
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
    private String createChildren(KB kb, String kbHref, String term, String language) {

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
                result.append(getLineSeparator());
                result.append("  <td valign=\"top\" class=\"label\">Children</td>");
                result.append(getLineSeparator());
                boolean isFirst = true;
                for (Iterator ik = kids.iterator(); ik.hasNext();) {
                    s = (String) ik.next();
                    String termHref = ("<a href=\"" + kbHref + s + suffix + "\">" 
                                       + showTermName(kb,s,language,true) 
                                       + "</a>");
                    if (!isFirst) result.append("<tr><td>&nbsp;</td>");                
                    result.append("<td valign=\"top\" class=\"cell\">" + termHref + "</td>");
                    String docString = getContextualDocumentation(kb, s, null);
                    docString = processDocString(kb, kbHref, language, docString, false, true);
                    result.append("<td valign=\"top\" class=\"cell\">" + docString + "</td>");
                    isFirst = false;
                }
                result.append("</tr>" + getLineSeparator());
            }
        }
        return result.toString();
    }

    /** ***************************************************************
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
    private String createInstances(KB kb, 
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
                String[] extRelns = {"syntacticUnion", "syntacticExtension"};
                List extendeds = null;
                String subent = null;
                for (int g = 0; g < extRelns.length; g++) {
                    extendeds = kb.getTermsViaPredicateSubsumption(extRelns[g],
                                                                   1,
                                                                   term,
                                                                   2,
                                                                   false);
                    for (int h = 0; h < extendeds.size(); h++) {
                        subent = (String) extendeds.get(h);
                        if (!working.contains(subent)) {
                            working.add(subent);
                        }
                    }
                }

                ArrayList instances = new ArrayList();
                String inst = null;
                Formula f = null;
                List forms = null;
                for (int w = 0; w < working.size(); w++) {
                    subent = (String) working.get(w);
                    forms = kb.askWithPredicateSubsumption("instance", 2, subent);
                    for (Iterator itf = forms.iterator(); itf.hasNext();) {
                        f = (Formula) itf.next();
                        if (!f.sourceFile.endsWith(KB._cacheFileSuffix)) {
                            inst = f.getArgument(1); 
                            if (!excluded.contains(inst) 
                                && isLegalForDisplay(inst)) {
                                instances.add(inst);
                            }
                        }
                    }
                }

                Set instSet = kb.getAllInstancesWithPredicateSubsumption(term);
                for (Iterator its = instSet.iterator(); its.hasNext();) {
                    inst = (String) its.next();
                    if (!excluded.contains(inst) 
                        && isLegalForDisplay(inst)) {
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
                        termHref = ("<a href=\"" + kbHref + inst + suffix + "\">" 
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
                        result.append(getLineSeparator());
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

    /** ***************************************************************
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
    private String createFormula(KB kb, 
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
            if (StringUtil.isNonEmptyString(previousTerm)) {
                previousTerm = previousTerm.trim();
            }
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
                        sb.append(getLineSeparator());
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
                sb.append(currentTerm);
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

    /** ***************************************************************
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
    private String createRelations(KB kb, String kbHref, String term, String language) {
        String result = "";
        try {
            StringBuilder sb = new StringBuilder();
            if (isLegalForDisplay(term)) {
                String suffix = "";
                if (StringUtil.emptyString(kbHref)) 
                    suffix = ".html";
                ArrayList relations = getPredicates(kb);

                // System.out.println(getLineSeparator() + "relations == " + relations + getLineSeparator());

                if (!relations.isEmpty()) {
                    Set avoid = getInhibitDisplayRelations();
                    TreeMap map = new TreeMap();
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
                        ArrayList vals = null;
                        String s = null;
                        boolean firstLine = true;
                        for (itr = relations.iterator(); itr.hasNext();) {
                            relation = (String) itr.next();
                            vals = (ArrayList) map.get(relation);
                            if ((vals != null) && !vals.isEmpty()) {
                                String relnHref = ("<a href=\"" + kbHref + relation + suffix + "\">" 
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
                                              + getLineSeparator());
                                }
                            }
                        }                
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

    /** ***************************************************************
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
    private String showCardinalityCell(KB kb, String kbHref, String term, String context) {
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

    /** ***************************************************************
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
    private String createCompositeComponentLine(KB kb, 
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
            sb.append(getLineSeparator());
            sb.append("  <td></td>");
            sb.append(getLineSeparator());

            sb.append("  <td valign=\"top\" class=\"cell\">");
            sb.append(getLineSeparator());

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
                for (int i = 0; i < termForms.size(); i++) {               
                    Formula f = (Formula) termForms.get(i);
                    sb.append(indentChars("&nbsp;&nbsp;",indent));
                    String termFormat = StringUtil.removeEnclosingQuotes(f.getArgument(3));
                    sb.append("<a href=\"");
                    sb.append(kbHref); 
                    sb.append(parentClass);
                    sb.append(suffix);
                    sb.append("\">");
                    if (isAttribute) sb.append("<span class=\"attribute\">");
                    sb.append(termFormat);
                    if (isAttribute) sb.append("</span>");
                    sb.append("</a>");                      
                }
            }
            sb.append("  </td>");
            sb.append(getLineSeparator());

            sb.append("  <td valign=\"top\" class=\"cell\">");
            sb.append(getLineSeparator());
            ArrayList clist = new ArrayList();
            clist.add(language);
            String docString = getContextualDocumentation(kb, term, clist);
            docString = processDocString(kb, kbHref, language, docString, false, true);
            sb.append("    ");
            sb.append(docString); 
            sb.append(getLineSeparator());
            sb.append("  </td>");
            sb.append(getLineSeparator());

            sb.append("  <td valign=\"top\" class=\"card\">");
            if (indent > 0)        
                sb.append(showCardinalityCell(kb, kbHref, term, ""));
            sb.append("  </td>");
            sb.append(getLineSeparator());

            sb.append("  <td valign=\"top\" class=\"cell\">");
            String dataTypeName = getFirstDatatype(kb, term);
            if (StringUtil.isNonEmptyString(dataTypeName)) {
                String dtToPrint = showTermName(kb, dataTypeName, language, true);                
                sb.append("<a href=\"");
                sb.append(kbHref);
                sb.append(dataTypeName);
                sb.append(suffix);
                sb.append("\">");
                sb.append(dtToPrint);
                sb.append("</a>");
                String xsdType = getClosestXmlDataType(kb, dataTypeName);
                if (StringUtil.isNonEmptyString(xsdType)) {
                    sb.append(" (");
                    sb.append(DB.kifToW3c(xsdType));
                    sb.append(")");
                }
                sb.append(getLineSeparator());
            }
            sb.append("  </td>");
            sb.append(getLineSeparator());

            sb.append("</tr>");
            sb.append(getLineSeparator());

            result = sb.toString();
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
        return result;
    }

    /** ***************************************************************
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
     * @param withSpanTags If true, the returned String is wrapped in
     * HTML span tags that allow additional formatting for term via a
     * style sheet
     * 
     * @return A String providing a context-specific name for term,
     * possibly including HTML markup, or just term
     * 
     */
    public String showTermName(KB kb, String term, String language, boolean withSpanTags) {

        String ans = term;
        try {
            ans = StringUtil.removeEnclosingQuotes(ans);

            String termFormat = (String) kb.getTermFormatMap(language).get(term);

            if (StringUtil.emptyString(termFormat)) {
                termFormat = (String) kb.getTermFormatMap("EnglishLanguage").get(term);
            }
            if (StringUtil.isNonEmptyString(termFormat)) {
                ans = StringUtil.removeEnclosingQuotes(termFormat);
            }
            else {
                String namespace = getTermNamespace(kb, term);
                if (StringUtil.isNonEmptyString(namespace) && namespace.equals(language)) {
                    ans = stripNamespacePrefix(kb, term);
                }
                else {
                    ans = DB.kifToW3c(term);
                }
            }
            if (getCodedIdentifiers(kb).contains(term)) {  //(term, "IsoCode")) {
                List<String> delims = Arrays.asList(DB.getW3cNamespaceDelimiter(), 
                                                    DB.getKifNamespaceDelimiter());
                for (String delim : delims) {
                    int idx = ans.indexOf(delim);
                    if (idx > -1) {
                        ans = ans.substring(idx + delim.length());
                        break;
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

    /** ***************************************************************
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

    /** ***************************************************************
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
    private String formatCompositeHierarchy(KB kb, 
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

    /** ***************************************************************
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
    private ArrayList createCompositeRecurse(KB kb, String term, boolean isAttribute, int indent) {
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

    /** ***************************************************************
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
    private boolean isXmlAttribute(KB kb, String term) {
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

    /** ***************************************************************
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
    private String createContainingCompositeComponentLine(KB kb, 
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
                        sb.append(getLineSeparator());
                        sb.append("  <td>&nbsp;</td>");
                        sb.append(getLineSeparator());
                        sb.append("  <td valign=\"top\" class=\"cell\">");
                        sb.append(indentChars("&nbsp;&nbsp;", indent));
                        sb.append("<a href=\"");
                        sb.append(kbHref);
                        sb.append(containingComp);
                        sb.append(suffix);
                        sb.append("\">");
                        sb.append(showTermName(kb,containingComp,language,true));
                        sb.append("</a></td>");
                        sb.append(getLineSeparator());
                        sb.append("  <td valign=\"top\" class=\"cell\">");
                        sb.append(getLineSeparator());
                        sb.append("    ");
                        sb.append(processDocString(kb, 
                                                   kbHref, 
                                                   language, 
                                                   f.getArgument(3), 
                                                   false, 
                                                   true));
                        sb.append(getLineSeparator());
                        sb.append("  </td>");
                        sb.append(getLineSeparator());
                        sb.append("  <td valign=\"top\" class=\"card\">");
                        sb.append(showCardinalityCell(kb,kbHref,instance,context));
                        sb.append("  </td>");
                        sb.append(getLineSeparator());
                        sb.append("  <td>&nbsp;</td>");
                        sb.append(getLineSeparator());
                        sb.append("</tr>");
                        sb.append(getLineSeparator());
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

    /** ***************************************************************
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
    private String formatContainingComposites(KB kb, 
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

    /** ***************************************************************
     * Returns true if term should be skipped over during printing,
     * else returns false.
     *
     */
    private static boolean isSkipNode(KB kb, String term) {
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

    /** ***************************************************************
     * Travels up the HasXmlElement and HasXmlAttribute relation
     * hierarchies to collect all parents.
     *
     */
    private ArrayList containingComposites(KB kb, String term) {
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

    /** ***************************************************************
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
    private String createBelongsToClass(KB kb, 
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
                result.append(getLineSeparator());
                result.append("  <td valign=\"top\" class=\"label\">");
                result.append(getLineSeparator());
                result.append("    Belongs to Class");
                result.append(getLineSeparator());
                result.append("  </td>");
                result.append(getLineSeparator());
                result.append("  <td valign=\"top\" class=\"cell\">");
                result.append(getLineSeparator());
                result.append("<a href=\""); 
                result.append(kbHref);
                result.append(className);
                result.append(suffix); 
                result.append("\">");
                result.append(showTermName(kb, className, language, true));
                result.append("</a>");        
                result.append("  </td>");
                result.append(getLineSeparator());
                result.append("  <td></td><td></td><td></td>");
                result.append(getLineSeparator());
                result.append("</tr>"); 
                result.append(getLineSeparator());

                markup = result.toString();
            }
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
        return markup;
    }

    /** ***************************************************************
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
    private String createBelongsToClass(KB kb, 
                                        String kbHref, 
                                        String term, 
                                        String language) {

        return createBelongsToClass(kb, kbHref, term, language, null);
    }

    /** ***************************************************************
     *  Iterate through all the datatype relations for the
     *  composite to collect the instances of this composite.  Then
     *  call containingComposite() travel up the syntacticSubordinate
     *  relations for those instances to find their containing
     *  composites (if any).
     */
    /*
      private ArrayList findContainingComposites(KB kb, String term) {

      ArrayList ans = new ArrayList();
      try {
      if (StringUtil.isNonEmptyString(term)) {
      Set accumulator = 
      new HashSet(kb.getTermsViaPredicateSubsumption("syntacticSubordinate",
      1,
      term,
      2,
      true));
      // containingComposites(kb, term);
      if (accumulator.isEmpty()) {
      List narrowerTerms = getFirstSpecificTerms(kb, term);
      String nt = null;
      for (Iterator it = narrowerTerms.iterator(); it.hasNext();) {
      nt = (String) it.next();
      accumulator
      .addAll(kb.getTermsViaPredicateSubsumption("syntacticSubordinate",
      1,
      nt,
      2,
      true));
      }
      }
      List working = new LinkedList();
      String nextTerm = null;
      while (!accumulator.isEmpty()) {
      working.clear();
      working.addAll(accumulator);
      accumulator.clear();
      for (Iterator it2 = working.iterator(); it2.hasNext();) {
      nextTerm = (String) it2.next();
      if (isSkipNode(kb, nextTerm)) {
      accumulator.addAll(findContainingComposites(kb, nextTerm));
      }
      else if (!ans.contains(nextTerm)) {
      ans.add(nextTerm); 
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
    */

    private ArrayList findContainingComposites(KB kb, String term) {

	ArrayList ans = new ArrayList();
        /*          */
	try {
            if (StringUtil.isNonEmptyString(term)) {
		List accumulator = containingComposites(kb, term);
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
                        List compArr1 = containingComposites(kb, term2);
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

    /** ***************************************************************
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
                                       + getLineSeparator());
                    for (int i = 0; i < localLimit; i++) {
                        Formula form = (Formula) forms.get(i);
                        result.append(LanguageFormatter.htmlParaphrase(kbHref,
                                                                       form.theFormula, 
                                                                       kb.getFormatMap(language), 
                                                                       kb.getTermFormatMap(language), 
                                                                       kb,
                                                                       language) 
                                      + "<br>"
                                      + getLineSeparator());
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
                                   + getLineSeparator());
                for (int i = 0; i < localLimit; i++) {
                    Formula form = (Formula) forms.get(i);
                    result.append(LanguageFormatter.htmlParaphrase(kbHref,
                                                                   form.theFormula, 
                                                                   kb.getFormatMap(language), 
                                                                   kb.getTermFormatMap(language), 
                                                                   kb,
                                                                   language) 
                                  + getLineSeparator());
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
                                   + getLineSeparator());
                for (int i = 0; i < localLimit; i++) {
                    Formula form = (Formula) forms.get(i);
                    result.append(LanguageFormatter.htmlParaphrase(kbHref,
                                                                   form.theFormula, 
                                                                   kb.getFormatMap(language), 
                                                                   kb.getTermFormatMap(language), 
                                                                   kb,
                                                                   language) 
                                  + getLineSeparator());
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
                                   + getLineSeparator());
                for (int i = 0; i < localLimit; i++) {
                    Formula form = (Formula) forms.get(i);
                    result.append(LanguageFormatter.htmlParaphrase(kbHref,
                                                                   form.theFormula, 
                                                                   kb.getFormatMap(language), 
                                                                   kb.getTermFormatMap(language), 
                                                                   kb,
                                                                   language) 
                                  + "<br>" 
                                  + getLineSeparator());
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
                                   + getLineSeparator());
                for (int i = 0; i < localLimit; i++) {
                    Formula form = (Formula) forms.get(i);
                    result.append(LanguageFormatter.htmlParaphrase(kbHref,
                                                                   form.theFormula, 
                                                                   kb.getFormatMap(language), 
                                                                   kb.getTermFormatMap(language), 
                                                                   kb,
                                                                   language) 
                                  + "<br>"
                                  + getLineSeparator());
                }
            }
            result.append(limitString);
            if (result.length() > 0) { 
                // note that the following 3 lines are inserted in reverse order
                result.insert(0,"</td></tr></table><P>");
                result.insert(0, ("<tr><td valign=\"top\" class=\"cell\">"
                                  + "These statements express (potentially complex) "
                                  + "facts about the term and are "
                                  + "automatically generated.</td></tr>"
                                  + getLineSeparator()
                                  + "<tr><td valign=\"top\" class=\"cell\">"));
                result.insert(0, ("<p><table><tr><td valign=\"top\" class=\"label\">"
                                  + "<b>Other statements</b>"
                                  + "</td></tr>"));
            }
        }
        return result.toString();
    }

    /** ***************************************************************
     * 
     * 
     */
    private String createTermRelevanceNotice(KB kb, String kbHref, String term, String namespace) {
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
                    Set relevanceSet = (Set) coreTerms.get(term);
                    if (relevanceType.equals("supporting")) {
                        relevanceSet = (Set) supportingTerms.get(term);
                    }
                    else if (relevanceType.equals("framework")) {
                        relevanceSet = (Set) frameworkTerms.get(term);
                    }

                    if (relevanceSet != null) {
                        sb.append("<span class=\"relevance\">");
                        if (relevanceSet.containsAll(ftSet)) {
                            sb.append("A <span class=\"relevance-type\">");
                            sb.append(relevanceType);
                            sb.append("</span> term for every <a href=\"");
                            sb.append(kbHref);
                            sb.append(focalClass);
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
                                sb.append(focalTerm);
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

    /** ***************************************************************
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
    private String getTermRelevance(KB kb, String ontology, String term) {
        String ans = "";
        try {
            if (coreTerms.isEmpty()) computeTermRelevance(kb, ontology);

            if (coreTerms.get(term) != null) {
                ans = "core";
            }
            else if (frameworkTerms.get(term) != null) {
                ans = "framework";
            }
            else if (supportingTerms.get(term) != null) {
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

    /** ***************************************************************
     * Create an HTML page that lists information about a particular
     * composite term, which is a representation of an XML
     * structure.
     * @param alphaList a TreeMap of TreeMaps of ArrayLists.  @see
     *                   createAlphaList()
     */
    public String createCompositePage(KB kb, 
                                      String kbHref, 
                                      String term, 
                                      TreeMap alphaList, 
                                      int limit, 
                                      String language) {

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
                                                    // showTermName(kb, language, language) 
                                                    // + "Dictionary.html"
                                                    ));
                result.append("<table width=\"100%\">");
                result.append(getLineSeparator());
                result.append("  <tr bgcolor=\"#DDDDDD\">");
                result.append(getLineSeparator());
                result.append("    <td valign=\"top\" class=\"cell\"><font size=\"+2\">");
                result.append(showTermName(kb,term,language,true));
                result.append("</font>");
                result.append("    </td>");
                result.append(getLineSeparator());
                result.append("  </tr>");
                result.append(getLineSeparator());

                String relevance = createTermRelevanceNotice(kb, kbHref, term, language);
                if (StringUtil.isNonEmptyString(relevance)) {
                    result.append("  <tr bgcolor=\"#DDDDDD\">");
                    result.append(getLineSeparator());
                    result.append("    <td valign=\"top\" class=\"cell\">");
                    result.append(getLineSeparator());
                    result.append(relevance);
                    result.append(getLineSeparator());
                    result.append("    </td>");
                    result.append(getLineSeparator());
                    result.append("  </tr>");
                    result.append(getLineSeparator());
                }

                result.append(createDocs(kb,kbHref,term,language));
                result.append("</table>");
                result.append(getLineSeparator());
                // result.append("<p>");
                // result.append(getLineSeparator());

                // result.append(HTMLformatter.htmlDivider);

                result.append("<table>");
                result.append(getLineSeparator());
                result.append(createSynonyms(kb, kbHref, term));

                ArrayList superComposites = findContainingComposites(kb, term); 
                Collections.sort(superComposites, String.CASE_INSENSITIVE_ORDER);

                sb1.append(createHasSameComponents(kb, kbHref, term, language));
                if ((sb1.length() > 0) 
                    || !superComposites.isEmpty()
                    || hasSubComponents(kb, term)) {

                    result.append("<tr class=\"title_cell\">");
                    result.append(getLineSeparator());
                    result.append("  <td valign=\"top\" class=\"label\">Component Structure</td>");
                    result.append(getLineSeparator());
                    result.append("  <td valign=\"top\" colspan=\"4\"></td>");
                    result.append(getLineSeparator());
                    result.append("</tr>");
                    result.append(getLineSeparator());

                    if (sb1.length() > 0) {
                        result.append(sb1);
                        sb1.setLength(0);
                    }

                    if (hasSubComponents(kb, term)) {
                        result.append("<tr>");
                        result.append(getLineSeparator());
                        result.append("  <td valign=\"top\" class=\"label\">Components</td>");
                        result.append(getLineSeparator());
                        result.append("  <td valign=\"top\" class=\"title_cell\">Name</td>");
                        result.append(getLineSeparator());
                        result.append("  <td valign=\"top\" class=\"title_cell\">");
                        result.append(getLineSeparator());
                        result.append("    Description of Element Role");
                        result.append(getLineSeparator());
                        result.append("  </td>");
                        result.append(getLineSeparator());
                        result.append("  <td valign=\"top\" class=\"title_cell\">Cardinality</td>");
                        result.append(getLineSeparator());
                        result.append("  <td valign=\"top\" class=\"title_cell\">Data Type</td>");
                        result.append(getLineSeparator());
                        result.append("</tr>");
                        result.append(getLineSeparator());
                
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
                                accumulator.addAll(getSyntacticExtensionTerms(kb, nextTerm, 2, false));

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
                        result.append(getLineSeparator());
                        result.append("  <td valign=\"top\" class=\"label\">");
                        result.append(getLineSeparator());
                        result.append("    Is Member of Composites");
                        result.append(getLineSeparator());
                        result.append("  </td>");
                        result.append(getLineSeparator());
                        result.append("  <td valign=\"top\" class=\"title_cell\">");
                        result.append(getLineSeparator());
                        result.append("    Composite Name");
                        result.append(getLineSeparator());
                        result.append("  </td>");
                        result.append(getLineSeparator());
                        result.append("  <td valign=\"top\" class=\"title_cell\">");
                        result.append(getLineSeparator());
                        result.append("    Description of Element Role");
                        result.append(getLineSeparator());
                        result.append("  </td>");
                        result.append(getLineSeparator());
                        result.append("  <td valign=\"top\" class=\"title_cell\">");
                        result.append(getLineSeparator());
                        result.append("    Cardinality");
                        result.append("  </td>");
                        result.append(getLineSeparator());
                        result.append("  <td valign=\"top\" class=\"title_cell\"> &nbsp; </td>");
                        result.append(getLineSeparator());
                        result.append("</tr>");
                        result.append(getLineSeparator());

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
                    result.append(getLineSeparator());
                    result.append("  <td valign=\"top\" class=\"label\">");
                    result.append(getLineSeparator());
                    result.append("    Relationships");
                    result.append(getLineSeparator());
                    result.append("  </td>");
                    result.append(getLineSeparator());
                    result.append("  <td></td><td></td><td></td><td></td>");
                    result.append(getLineSeparator());
                    result.append("</tr>");
                    result.append(getLineSeparator());

                    result.append(sb1);

                    sb1.setLength(0);
                }
                result.append("</table>");
                result.append(getLineSeparator());

                result.append(generateHtmlFooter(""));

                result.append("  </body>");
                result.append(getLineSeparator());
                result.append("</html>"); 
                result.append(getLineSeparator());

                markup = result.toString();
            }
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
        return markup;
    }

    /** ***************************************************************
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
                             String language) {

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
                                                // showTermName(kb, language, language)
                                                // + "Dictionary.html"
                                                ));
            }
            result.append("<table width=\"100%\">");
            result.append(getLineSeparator());
            result.append("  <tr bgcolor=\"#DDDDDD\">");
            result.append(getLineSeparator());
            result.append("    <td valign=\"top\" class=\"cell\">");
            result.append(getLineSeparator());
            result.append("      <font size=\"+2\">");
            result.append(getLineSeparator());
            result.append("        ");
            result.append(showTermName(kb,term,language,true));
            result.append(getLineSeparator());
            result.append("      </font>");
            result.append(getLineSeparator());
            result.append("    </td>");
            result.append(getLineSeparator());
            result.append("  </tr>"); 
            result.append(getLineSeparator());

            String relevance = createTermRelevanceNotice(kb, kbHref, term, language);
            if (StringUtil.isNonEmptyString(relevance)) {
                result.append("  <tr bgcolor=\"#DDDDDD\">");
                result.append(getLineSeparator());
                result.append("    <td valign=\"top\" class=\"cell\">");
                result.append(getLineSeparator());
                result.append(relevance);
                result.append(getLineSeparator());
                result.append("    </td>");
                result.append(getLineSeparator());
                result.append("  </tr>");
                result.append(getLineSeparator());
            }

            result.append(createDocs(kb, kbHref, term, language));
            result.append(getLineSeparator());
            result.append("</table>");
            result.append(getLineSeparator());
            // result.append("<p>");
            // result.append(getLineSeparator());
            result.append("<table width=\"100%\">");
            result.append(getLineSeparator());
            result.append(createSynonyms(kb, kbHref, term));
            result.append(getLineSeparator());
            result.append(createComments(kb, kbHref, term, language));
            result.append(getLineSeparator());

            Set<String> parents = new HashSet<String>();
            sb1.append(createParents(kb, kbHref, term, language, parents));
            sb1.append(getLineSeparator());
            sb2.append(createChildren(kb, kbHref, term, language));
            sb2.append(getLineSeparator());

            if ((sb1.length() > 0) || (sb2.length() > 0)) {
                result.append("<tr class=\"title_cell\">");
                result.append(getLineSeparator());
                result.append("  <td valign=\"top\" class=\"label\">");
                result.append(getLineSeparator());
                result.append("    Relationships");
                result.append(getLineSeparator());
                result.append("  </td>");
                result.append(getLineSeparator());
                result.append("  <td>&nbsp;</td>");
                result.append(getLineSeparator());
                result.append("  <td>&nbsp;</td>");
                result.append(getLineSeparator());
                result.append("  <td>&nbsp;</td>");
                result.append(getLineSeparator());
                result.append("</tr>");
                result.append(getLineSeparator());

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
            result.append(getLineSeparator());

            result.append(createRelations(kb, kbHref, term, language));
            result.append(getLineSeparator());

            result.append(createUsingSameComponents(kb, kbHref, term, language));
            result.append(getLineSeparator());

            result.append(createBelongsToClass(kb, kbHref, term, language, parents));
            result.append(getLineSeparator());

            if (!superComposites.isEmpty()) {
                result.append("<tr>");
                result.append(getLineSeparator());
                result.append("  <td valign=\"top\" class=\"label\">");
                result.append(getLineSeparator());
                result.append("    Is Member of Composites");
                result.append(getLineSeparator());
                result.append("  </td>");
                result.append(getLineSeparator());
                result.append("  <td valign=\"top\" class=\"title_cell\">");
                result.append(getLineSeparator());
                result.append("    Composite Name");
                result.append(getLineSeparator());
                result.append("  </td>");
                result.append(getLineSeparator());
                result.append("  <td valign=\"top\" class=\"title_cell\">");
                result.append(getLineSeparator());
                result.append("    Description of Element Role");
                result.append(getLineSeparator());
                result.append("  </td>");
                result.append(getLineSeparator());
                result.append("  <td valign=\"top\" class=\"title_cell\">");
                result.append(getLineSeparator());
                result.append("    Cardinality");
                result.append(getLineSeparator());
                result.append("  </td>");
                result.append(getLineSeparator());
                result.append("  <td> &nbsp; </td>");
                result.append(getLineSeparator());
                result.append("</tr>");
                result.append(getLineSeparator());

                result.append(formatContainingComposites(kb,
                                                         kbHref,
                                                         superComposites,
                                                         term,
                                                         language));
                result.append(getLineSeparator());
            }

            result.append("</table>");
            result.append(getLineSeparator());

            // result.append(HTMLformatter.htmlDivider);

            result.append(generateHtmlFooter(""));
            result.append(getLineSeparator());
            result.append("  </body>");
            result.append(getLineSeparator());
            result.append("</html>");
            result.append(getLineSeparator());

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
        catch (Exception ex) {
            ex.printStackTrace();
        }
        return output;
    }

    /** ***************************************************************
     *  Generate an alphabetic HTML list that points to the
     *  individual index pages (which collect all terms starting
     *  with a particular letter.
     */
    public String generateDynamicTOCHeader(String kbHref) {

        String markup = "";
        try {
            StringBuilder result = new StringBuilder();
            result.append(generateHtmlDocStart(""));
            result.append(getLineSeparator());
            result.append("<table width=\"100%\">");
            result.append(getLineSeparator());
            result.append("  <tr>");
            for (char c = 65; c < 91; c++) {
                result.append(getLineSeparator());
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
                result.append(getLineSeparator());
            }
            result.append("  </tr>");
            result.append(getLineSeparator());
            result.append("</table>");
            result.append(getLineSeparator());
            markup = result.toString();
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
        return markup;
    }

    /** ***************************************************************
     *  Generate an alphabetic HTML list that points to the
     *  individual index pages (which collect all terms or term
     *  formats) starting with a particular letter.
     * 
     * @param alphaList a TreeMap of TreeMaps of ArrayLists.  @see
     *                   createAlphaList()
     */
    private String generateTocHeader(KB kb, TreeMap alphaList, String allname) {

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
                        sb2.append(getLineSeparator());
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
                        sb2.append(getLineSeparator());
                    }
                }

                // Increment once more for All.
                colNum++;

                StringBuilder sb1 = new StringBuilder();
                sb1.append("<table width=\"100%\">");
                sb1.append(getLineSeparator());
                sb1.append("  <tr>");
                sb1.append(getLineSeparator());
                sb1.append("    <td valign=\"top\" colspan=\"");
                sb1.append(colNum);
                sb1.append("\" class=\"title\">");
                if (StringUtil.isNonEmptyString(imgFile)) {
                    sb1.append(imgFile);
                    sb1.append("&nbsp;&nbsp;");
                }
                sb1.append(title);
                sb1.append("    </td>");
                sb1.append(getLineSeparator());
                sb1.append("  </tr>");
                sb1.append(getLineSeparator());
                sb1.append("  <tr class=\"letter\">"); 
                sb1.append(getLineSeparator());

                // Assemble everything in the correct order.
                result.append(sb1);
                result.append(sb2);

                result.append("    <td><a href=\""); 
                result.append(allname); 
                result.append("\">All</a></td>"); 
                result.append(getLineSeparator());
                result.append("  </tr>");
                result.append(getLineSeparator());
                result.append("</table>");
                result.append(getLineSeparator());

                setTocHeader(result.toString());
            }
            catch (Exception ex) {
                ex.printStackTrace();
            }
        }
        return getTocHeader();
    }

    /** ***************************************************************
     *  Generate an HTML page that lists term name and its
     *  documentation
     * 
     * @param alphaList a TreeMap of TreeMaps of ArrayLists.  @see
     *                   createAlphaList()
     */
    private String generateTOCPage(KB kb, String firstLetter, TreeMap alphaList, String language) {

        /*
          System.out.println("INFO in generateTOCPage(" + kb 
          + ", \"" + firstLetter + "\", "
          + "alphaList" 
          + " \"" + language + "\")");
        */

        int count = 0;
        StringBuilder result = new StringBuilder();
        result.append("<table width=\"100%\">");
        TreeMap map = (TreeMap) alphaList.get(firstLetter);
        ArrayList sorted = new ArrayList(map.keySet());
        sortByPresentationName(kb, language, sorted);
        String formattedTerm = null;
        ArrayList al = null;
        Iterator it2 = null;
        String realTermName = null;
        ArrayList docs = null;
        Iterator it3 = null;
        Formula f = null;
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
                    continue;
                }
                termToPrint = showTermName(kb, realTermName, language, true);
                result.append("  <tr>");
                result.append(getLineSeparator());

                // Term Name
                result.append("    <td valign=\"top\" class=\"cell\">");
                result.append(getLineSeparator());
                result.append("      <a href=\""); 
                result.append(realTermName); 
                result.append(".html\">");
                result.append(getLineSeparator());
                result.append("        ");
                result.append(termToPrint);
                result.append(getLineSeparator());
                result.append("      </a>");
                result.append(getLineSeparator());
                result.append("    </td>");
                result.append(getLineSeparator());

                // Relevance
                result.append("    <td valign=\"top\" class=\"cell\">");
                result.append(getLineSeparator());
                result.append("      ");
                result.append(getTermRelevance(kb, getOntology(), realTermName).equals("message")
                              ? "&nbsp;&nbsp;&nbsp;&nbsp;MT&nbsp;&nbsp;&nbsp;&nbsp;"
                              : "");
                result.append("    </td>");
                result.append(getLineSeparator());

                // Documentation
                docString = processDocString(kb, "", language, docString, false, true);

                result.append("    <td valign=\"top\" class=\"cell\">");
                result.append(getLineSeparator());
                result.append("      ");
                result.append(docString);
                result.append(getLineSeparator());
                result.append("    </td>");
                result.append(getLineSeparator());
                result.append("  </tr>"); 
                result.append(getLineSeparator());
            }
            //if ((count++ % 100) == 1) { System.out.print("."); }
        }
        //System.out.println();
        // result.append("</tr>" + getLineSeparator());
        result.append("</table>");
        result.append(getLineSeparator());
        return result.toString();
    }

    /** ***************************************************************
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
    private void saveIndexPages(KB kb, 
                                TreeMap alphaList, 
                                String dir, 
                                String language) {
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
                // if ((count++ % 100) == 1) System.out.print(".");
            }   
            System.out.println();

            /*
              outfile = new File(parentDir, "index.html");
              outpath = outfile.getCanonicalPath();
              pw = new PrintWriter(new FileWriter(outfile));
              pw.println(tocheader);
              pw.println(generateHtmlFooter(""));
              try {
              pw.close();
              }
              catch (Exception pwex2) {
              System.out.println("Error writing \"" 
              + outpath + "\": " 
              + pwex2.getMessage());
              pwex2.printStackTrace();
              }
            */
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
        return;
    }

    /** ***************************************************************
     *  Save pages below the KBs directory in a directory called
     *  HTML.  If that already exists, use HTML1, HTML2 etc.
     */
    private void printHTMLPages(TreeMap pageList, String dir) {

        FileWriter fw = null;
        PrintWriter pw = null;
        String term = null;
        String page = null;
        String filename = null;
        try {
            for (Iterator it = pageList.keySet().iterator(); it.hasNext();) {
                term = (String) it.next();
                page = (String) pageList.get(term);
                filename = dir + File.separator + term + ".html";
                //System.out.println("Info in DocGen.printPages(): filename == " + filename);
                try {
                    fw = new FileWriter(filename);
                    pw = new PrintWriter(fw);
                    pw.println(page);
                }
                catch (Exception e) {
                    System.out.println("ERROR in DocGen.printHTMLPages("
                                       + "[map with "
                                       + pageList.keySet().size()
                                       + " keys], "
                                       + dir + ")");
                    System.out.println("Error writing file " 
                                       + filename
                                       + getLineSeparator()
                                       + e.getMessage());
                    e.printStackTrace();
                }
                finally {
                    try {
                        if (pw != null) {
                            pw.close();
                        }
                        if (fw != null) {
                            fw.close();
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

    /** ***************************************************************
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
                    ans = StringUtil.removeEnclosingQuotes(f.getArgument(1));
                    break;
                }
            }
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
        return ans;
    }

    /** ***************************************************************
     *  @return a HashMap where the keys are the term names and the
     *  values are the "headwords" (with quotes removed).
     */
    public HashMap createHeadwordMap(KB kb) {
        HashMap result = new HashMap();
        try {
            ArrayList headwordForms = kb.ask("arg", 0, "headword");
            if (headwordForms != null && !headwordForms.isEmpty()) {
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
        return result;
    }

    /** ***************************************************************
     *  @return a HashMap where the keys are the headwords and the
     *  values are ArrayLists of term names (since headwords are not
     *  unique identifiers for terms). Don't put automatically
     *  created instances in the map. If there's no headword, use
     *  the term name. This map is the inverse of headwordMap. @see
     *  DB.stringToKifId()
     */
    public HashMap createInverseHeadwordMap(KB kb, HashMap headwordMap) {
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
        return result;
    }

    /** ***************************************************************
     * @param alphaList a TreeMap of TreeMaps of ArrayLists.  @see
     *                   createAlphaList()
     * 
     * @param inverseHeadwordMap is a HashMap where the keys are the
     *          headwords and the values are ArrayLists of term
     *          names (since headwords are not unique identifiers
     *          for terms). If there's no headword, the term name is
     *          used.
     */
    private TreeMap generateHTMLPages(KB kb, 
                                      TreeMap alphaList,
                                      HashMap inverseHeadwordMap, 
                                      String language) {
        TreeMap pageList = new TreeMap();
        TreeSet rejectedTerms = new TreeSet();
        String formattedTerm = null;
        List termNames = null;
        String realTermName = null;
        int count = 0;
        for (Iterator it = inverseHeadwordMap.keySet().iterator(); it.hasNext();) {
            formattedTerm = (String) it.next();
            termNames = (List) inverseHeadwordMap.get(formattedTerm);
            for (Iterator tni = termNames.iterator(); tni.hasNext();) {
                realTermName = (String) tni.next();
                if (isLegalForDisplay(realTermName)) {
                    if (isComposite(kb, realTermName)) {
                        pageList.put(realTermName,
                                     createCompositePage(kb,
                                                         "",
                                                         realTermName,
                                                         alphaList,
                                                         200,
                                                         language));
                    }
                    else {
                        pageList.put(realTermName,createPage(kb,
                                                             "",
                                                             realTermName,
                                                             alphaList,
                                                             200,
                                                             language));
                    }
                    if ((count++ % 100) == 1) System.out.print(".");
                }
                else {
                    rejectedTerms.add(realTermName);
                }
            }
        }
        System.out.println("INFO in DocGen.generateHTMLPages("
                           + kb.name + ", "
                           + "[map with " + 
                           + alphaList.keySet().size() + " keys], "
                           + "[map with " + 
                           + inverseHeadwordMap.keySet().size() + " keys], "
                           + language + ")");
        System.out.println("  " + rejectedTerms.size() + " terms rejected");
        return pageList;
    }

    /** ***************************************************************
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
    public TreeMap createAlphaList(KB kb, HashMap stringMap) {

        TreeMap alphaList = new TreeMap(String.CASE_INSENSITIVE_ORDER);
        try {
            for (Iterator it = kb.terms.iterator(); it.hasNext();) {
                String term = (String) it.next();
                if (isLegalForDisplay(term)
                    && !getCodedIdentifiers(kb).contains(term)
                    // && !term.matches("^iso\\d+.*_.+")
                    ) {
                    String formattedTerm = stripNamespacePrefix(kb, term);
                    if (simplified) {
                        String smterm = (String) stringMap.get(term);
                        if (smterm != null) {
                            formattedTerm = stripNamespacePrefix(kb, smterm);
                        }
                    }
                
                    String firstLetter = 
                        Character.toString(Character.toUpperCase(formattedTerm.charAt(0)));

                    if (alphaList.keySet() != null && alphaList.keySet().contains(firstLetter)) {
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
            }
            /*
              System.out.println("INFO DocGen.createAlphaList("
              + kb.name + ", "
              + stringMap + ")");
              System.out.println("  ==> " + alphaList);
            */
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
        return alphaList;
    }

    /** ***************************************************************
     * Generate simplified HTML pages for all terms.  Output is a
     * set of HTML files sent to the directory specified in
     * makeOutputDir()
     * 
     * @param s indicates whether to present "simplified" views of
     *          terms, meaning using a termFormat or headword,
     *          rather than the term name itself
     */
    public void generateHTML(KB kb, String language, boolean s) {

        long t1 = System.currentTimeMillis();
        try {

            System.out.println("ENTER DocGen.generateHTML("
                               + kb.name + ", "
                               + language + ", "
                               + s + ")");

            // Keys are formatted term names, values are HTML pages
            TreeMap pageList = new TreeMap();

            // Keys are headwords, values are terms
            TreeMap termMap = new TreeMap(String.CASE_INSENSITIVE_ORDER);

            // A HashMap where the keys are the term names and the values
            // are "headwords" (with quotes removed).
            HashMap headwordMap = createHeadwordMap(kb); 

            String context = toKifNamespace(kb, language);
            this.defaultNamespace = context;

            if (coreTerms.isEmpty()) computeTermRelevance(kb, getOntology());

            // Display term format expressions instead of term names
            simplified = s; 

            // a TreeMap of TreeMaps of ArrayLists.  @see createAlphaList()
            TreeMap alphaList = createAlphaList(kb, headwordMap);

            // Headword keys and ArrayList values (since the same headword can
            // be found in more than one term)
            HashMap inverseHeadwordMap = createInverseHeadwordMap(kb,headwordMap);  

            System.out.println("  INFO in DocGen.generateHTML(): generating alpha list");
            String dir = getHtmlOutputDirectoryPath();
            System.out.println("  INFO in DocGen.generateHTML(): saving index pages");
            saveIndexPages(kb, alphaList, dir, context);

            System.out.println("  INFO in DocGen.generateHTML(): generating HTML pages");
            pageList = generateHTMLPages(kb, alphaList, inverseHeadwordMap, context);
            printHTMLPages(pageList, dir);

            System.out.println("  INFO in DocGen.generateHTML(): creating single index page");
            generateSingleHTML(kb, dir, alphaList, context, s);
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }

        System.out.println("EXIT DocGen.generateHTML("
                           + kb.name + ", "
                           + language + ", "
                           + s + ")");
        System.out.println("  Time: "
                           + ((System.currentTimeMillis() - t1) / 1000.0)
                           + " seconds");

        return;
    } 

    /** ***************************************************************
     * Generate a single HTML page showing all terms.
     * @param alphaList a TreeMap of TreeMaps of ArrayLists.
     *                  @see createAlphaList()
     * 
     *      letter->    formattedTerm1->term11,term12...term1N
     *                  formattedTerm2->term21,term22...term2N
     */
    public void generateSingleHTML(KB kb, 
                                   String dir, 
                                   TreeMap alphaList,
                                   String language, 
                                   boolean s) {

        simplified = s;                // display term format expressions for term names
        PrintWriter pw = null; 
        try {
            File filedir = new File(dir);
            File outfile = new File(filedir, INDEX_FILE_NAME);
            pw = new PrintWriter(new FileWriter(outfile));

            /*
              pw.println("<html>" 
              + getLineSeparator()
              + "  <head><meta http-equiv=\"Content-Type\" "
              + "content=\"text/html; charset=utf-8\">" 
              + getLineSeparator()
              + "    <link rel=\"stylesheet\" type=\"text/css\" "
              + "href=\"simple.css\">" 
              + getLineSeparator()
              + "  </head>" 
              + getLineSeparator()
              + "  <body>" 
              + getLineSeparator());
            */

            // pw.println(this.header);

            pw.println(generateTocHeader(kb, alphaList, INDEX_FILE_NAME));

            pw.println("<table border=\"0\">");
            // <tr bgcolor=#CCCCCC><td>Name</td><td>Documentation</td></tr>\n");

            // boolean even = true;
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
                        docStr = "";
                        docs = kb.askWithRestriction(0, "documentation", 1, term);
                        if (docs != null && !docs.isEmpty()) {
                            f = (Formula) docs.get(0);
                            docStr = processDocString(kb, 
                                                      "", 
                                                      language, 
                                                      f.getArgument(3), 
                                                      false, 
                                                      true);
                        }
                        if (DB.isLocalTermReference(term) || StringUtil.emptyString(docStr))
                            continue;

                        pw.println("  <tr>");

                        // Term
                        pw.println("    <td valign=\"top\" class=\"cell\">");
                        printableTerm = (simplified
                                         ? showTermName(kb, term, language, true)
                                         : term);
                        pw.print("      <a href=\"");
                        pw.print(term);
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
                        pw.println("  <td valign=\"top\" class=\"description\">");
                        pw.print("    ");
                        pw.println(docStr);
                        pw.println("  </td>");

                        pw.println("</tr>");
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
        return;
    }

    /** ***************************************************************
     */
    public void generateSingleHTML(KB kb, String language, boolean s) throws IOException {

        HashMap headwordMap = createHeadwordMap(kb); 
        String dirpath = getHtmlOutputDirectoryPath();
        TreeMap alphaList = createAlphaList(kb, headwordMap);
        generateSingleHTML(kb, dirpath, alphaList, language, s);
    }


    /** ***************************************************************
     */
    public void capitalizationAlternates(TreeSet alts)  {

        TreeSet allCaps = new TreeSet();
        Iterator it = alts.iterator();
        while (it.hasNext()) {
            String term = (String) it.next();
            String capTerm = term.toUpperCase();
            if (allCaps.contains(capTerm)) 
                System.out.println(term);
            else
                allCaps.add(capTerm);
        }
    }


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
     * 
     */
    protected String getTermNamespace(KB kb, String term) {
        String result = "";
        try {
            if (StringUtil.isNonEmptyString(term)) {
                String prefix = getNamespacePrefix(kb, term);
                if (StringUtil.isNonEmptyString(prefix)) {
                    List<String> delims = Arrays.asList(DB.getW3cNamespaceDelimiter(), 
                                                        DB.getKifNamespaceDelimiter());
                    for (String delim : delims) {
                        if (prefix.endsWith(delim)) {
                            prefix = prefix.substring(0, prefix.length() - delim.length());
                            break;
                        }
                    }
                    result = (prefix.equals("ns")
                              ? prefix
                              : ("ns" + DB.getKifNamespaceDelimiter() + prefix));
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
    private ArrayList getDatatypeTerms(KB kb, String term, int targetArgnum) {
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
    private String getFirstDatatype(KB kb, String term) {
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
     * 
     */
    private boolean isDataType(KB kb, String term) {
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
    private String getClosestXmlDataType(KB kb, String term) {
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
     * 
     */
    private ArrayList getSyntacticExtensionTerms(KB kb, 
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
            if (result.isEmpty() && DB.isLocalTermReference(term)) {
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
    private ArrayList getSyntacticUnionTerms(KB kb, String term, int targetArgnum) {
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
    private ArrayList getSyntacticCompositeTerms(KB kb, String term, int targetArgnum) {
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
    private void sortByPresentationName(KB kb, String namespaceTerm, List stringList) {
        try {
            if (!SetUtil.isEmpty(stringList)) {
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
        return;
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
    private void sortByArg1Ordinal(KB kb, 
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
                    dtype = DB.kifToW3c(dtype);
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
                dtype = DB.kifToW3c(dtype);
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
                baseName = DB.kifToW3c(baseName);
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
                        String termName = DB.kifToW3c((String) types.get(i));
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
                        dtype = DB.kifToW3c(dtype);
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
     * The time, represented in milliseconds since Jan 1 1970, at
     * which the most recent attempt to compute term relevance
     * occurred.  We use this value to avoid frequent, useless
     * attempts to compute term relevance.
     *
     */
    private long lastComputeTermRelevanceAttemptTime = 0L;

    /** *************************************************************
     * The time interval, in milliseconds, to wait before trying to
     * compute term relevance.  We use this value to avoid frequent,
     * useless attempts to compute term relevance, but still permit
     * period retries.
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
            System.out.println("  Time: " 
                               + ((System.currentTimeMillis() - t1) / 1000.0)
                               + " seconds");
        }

        return;
    }

    /** *************************************************************
     * Gather the terms that should serve as the starting point for
     * exploring the graph that constitutes one or more documents.
     *
     */
    private ArrayList computeFocalTerms(KB kb, String ontology) {
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
                        if (!DB.isLocalTermReference(inst)) {
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
     * Collects all top-level terms for each namespace-specific XSD
     * file.  Returns a Map in which the keys are KIF namespace terms,
     * and the values are Lists of all top-level XSD terms in the
     * indexed namespaces.
     *
     */
    protected Map computeTopLevelTermsByNamespace(KB kb, String ontology) {

        System.out.println("ENTER DocGen.computeTopLevelTermsByNamespace("
                           + kb.name + ", "
                           + ontology + ")");

        Map tops = new HashMap();
        Set done = new HashSet();
        try {
            List focalTerms = computeFocalTerms(kb, ontology);

            System.out.println("  focalTerms == " + focalTerms);

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

            System.out.println("");
            // String dtype = null;
            String namespace = null;
            while (!accumulator.isEmpty()) {
                System.out.print("  " + accumulator.size() + " new terms to process ");
                working.clear();
                working.addAll(accumulator);
                accumulator.clear();

                for (it = working.iterator(); it.hasNext();) {
                    term = (String) it.next();
                    if (!done.contains(term)) {

                        if (!isSkipNode(kb, term)) {

                            namespace = getTermNamespace(kb, term);

                            // System.out.println("  term == " + term);

                            if (DB.isLocalTermReference(term)) {

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
            System.out.print("  Sorting terms by presentation name ");
            for (it = tops.keySet().iterator(); it.hasNext();) {

                // A namespace.
                term = (String) it.next();

                // A list of terms in namespace.
                working = (List) tops.get(term);
                sortByPresentationName(kb, namespace, working);
                System.out.print(".");
            }
            System.out.println("done");
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }

        System.out.println("EXIT DocGen.computeTopLevelTermsByNamespace("
                           + kb.name + ", "
                           + ontology + ")");
        System.out.println("  ==> [" + tops.size() + " keys]");

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
            String nsTerm = ("ns" + DB.getKifNamespaceDelimiter() + nsName);
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
                        if (!xsdType.equals(DB.kifToW3c(term))) {
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
                                dtype = DB.kifToW3c(dtype);
                                if (!dtype.equals(xsdType)) {
                                    datatype = xsdType;
                                    break;
                                }
                            }
                        }
                        if (!specHasDataType) {
                            xsdType = substituteXsdDataType(kb, nsTerm, term);
                            if (!xsdType.equals(DB.kifToW3c(term))) {
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
            List<String> nsprefixes = Arrays.asList(("ns" + DB.getKifNamespaceDelimiter()),
                                                    ("ns" + DB.getW3cNamespaceDelimiter()));
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
            // if (coreTerms.isEmpty()) computeTermRelevance(kb, ontology);

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
    private TreeSet getAvssInNamespace(KB kb, List termsInNamespace) {
        TreeSet avss = new TreeSet();
        try {
            List predicates = kb.getTermsViaPredicateSubsumption("subrelation",
                                                                 2,
                                                                 "element",
                                                                 1,
                                                                 false);
            if (!predicates.contains("element")) { predicates.add("element"); }
            String pred = null;
            String avs = null;
            Formula f = null;
            List formulae = null;
            for (int j = 0; j < predicates.size(); j++) {
                pred = (String) predicates.get(j);
                formulae = kb.ask("arg", 0, pred);
                for (int i = 0; i < formulae.size(); i++) {
                    f = (Formula) formulae.get(i);
                    avs = f.getArgument(2);
                    if (termsInNamespace.contains(avs)) {
                        avss.add(avs);
                    }
                }
            }
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
        return avss;
    }

    /** *************************************************************
     * 
     */
    private TreeSet getCompositesInNamespace(KB kb, List termsInNamespace) {
        TreeSet composites = new TreeSet();
        try {
            Set terms = 
                kb.getAllInstancesWithPredicateSubsumption("CompositeContentBearingObjectType");
            if (terms != null) {
                String term = null;
                Iterator it = terms.iterator();
                while (it.hasNext()) {
                    term = (String) it.next();
                    if (termsInNamespace.contains(term)) {
                        composites.add(term);
                    }
                }
            }
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
        return composites;
    }

    /** *************************************************************
     * 
     */
    private TreeSet getSubClassesInNamespace(KB kb, List termsInNamespace, String className) {
        TreeSet subclasses = new TreeSet();
        try {
            Set terms = kb.getAllSubClassesWithPredicateSubsumption(className);
            if (terms != null) {
                String term = null;
                Iterator it = terms.iterator();
                while (it.hasNext()) {
                    term = (String) it.next();
                    if (termsInNamespace.contains(term)) {
                        subclasses.add(term);
                    }
                }
            }
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
        return subclasses;
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
     * 
     * @param isXmlDoc If true, HTML character entities will be
     *                 replaced with their ASCII equivalents, when
     *                 possible
     *
     * @param addHrefs If true, HTML anchor markup will be added
     *                 for recognized SUO-KIF terms
     *
     */
    private String processDocString(KB kb, 
                                    String kbHref, 
                                    String namespace, 
                                    String docString,
                                    boolean isXmlDoc,
                                    boolean addHrefs) {
        String ans = docString;
        try {
            if (StringUtil.isNonEmptyString(docString)) {
                String nsTerm = toKifNamespace(kb, namespace);
                String tmpstr = StringUtil.normalizeSpaceChars(docString);
                if (isXmlDoc) {
                    Map srmap = getStringReplacementMap();
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
        return ans;
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
                    || (!newType.equals(DB.w3cToKif("xs:IDREF"))
                        && !newType.equals(DB.w3cToKif("xs:ID")))) {
                    xmlType = newType;
                }
            }
            xmlType = DB.kifToW3c(xmlType);
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
        return xmlType;
    }

    /// END: code for XSD generation.

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
            String csvFileName = kb.getFirstTermViaAskWithRestriction(0, "filename",
                                                                      2, fileInst,
                                                                      1);
            csvFileName = StringUtil.removeEnclosingQuotes(csvFileName);
            if (StringUtil.emptyString(csvFileName)) {
                throw new Exception("Cannot find a file name string for "
                                    + fileInst
                                    + ".  Giving up.");
            }
            File indir = new File(mgr.getPref("kbDir"));
            File csvFile = new File(indir, csvFileName);
            if (!csvFile.canRead()) {
                throw new Exception("Cannot read " 
                                    + csvFile.getCanonicalPath() 
                                    + ".  Giving up.");
            }
            long t1 = System.currentTimeMillis();
            DB db = DB.getInstance(ontoTerm);
            String csvFileCanonPath = csvFile.getCanonicalPath();
            List rows = db.readSpreadsheet(csvFileCanonPath, true);
            String kifFileName = csvFileCanonPath;
            int endIdx = kifFileName.lastIndexOf(".");
            kifFileName = (kifFileName.substring(0, endIdx) + ".kif");
            String defaultNamespace = 
                kb.getFirstTermViaAskWithRestriction(0, 
                                                     "translationDefaultNamespace",
                                                     1, 
                                                     ontoTerm,
                                                     2);
            if (StringUtil.emptyString(defaultNamespace)) {
                throw new Exception("Cannot find the default namespace for " 
                                    + ontoTerm
                                    + ".  Giving up.");
            }
            db.processSpreadsheet(rows, defaultNamespace, kifFileName);
            System.out.println("  Time: " 
                               + ((System.currentTimeMillis() - t1) / 1000.0) 
                               + " seconds to process "
                               + csvFileName);

            kb.addConstituent(kifFileName);
            generateHTML(kb, defaultNamespace, true);
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
            if (coreTerms.isEmpty()) computeTermRelevance(kb, ontology);
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
     * Create a PrintWrite for the focal term kifTerm.
     */
    private PrintWriter createFocalTermPrintWriter(String kifTerm, String outdirPath) {
        PrintWriter pw = null;
        try {
            File outdir = new File(outdirPath);
            String filename = (DB.kifToW3c(kifTerm).replace(":", "-") + ".html");
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
     * Compute core terms.
     */
    private void computeCoreTerms(int n, 
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
                if (DB.isLocalTermReference(nextTerm)) {
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
    private void computeCoreTerms(KB kb, String focalTerm) {
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
            if (!DB.isLocalTermReference(key)) {
                cts = (Set) coreTerms.get(key);
                if (cts == null) {
                    cts = new HashSet();
                    coreTerms.put(key, cts);
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
                if (!DB.isLocalTermReference(inst)) {
                    cts = (Set) coreTerms.get(inst);
                    if (cts == null) {
                        cts = new HashSet();
                        coreTerms.put(inst, cts);
                    }
                    cts.add(focalTerm);
                }
            }
        }

        /*        */
        System.out.println("EXIT DocGen.computeCoreTerms("
                           + kb.name + ", "
                           + focalTerm + ")");
        System.out.println("  Time: " 
                           + ((System.currentTimeMillis() - t1) / 1000.0)
                           + " seconds");

        return;
    }

    /** *************************************************************
     * Compute framework terms in general.  Note that this method does
     * not compute the framework terms actually used in a given focal
     * term type.  Instead, it computes all of the framework terms
     * that could be used.
     */
    private void computeFrameworkTerms(KB kb, String ontology) {

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
                        if (!DB.isLocalTermReference(wt)) {
                            namespace = getTermNamespace(kb, wt);
                            if (StringUtil.isNonEmptyString(namespace)) {
                                Set ftset = (Set) frameworkTerms.get(wt);
                                if (ftset == null) {
                                    ftset = focalTerms;
                                    frameworkTerms.put(wt, ftset);
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
        System.out.println("  Time: "
                           + ((System.currentTimeMillis() - t1) / 1000.0)
                           + " seconds");

        return;
    }

    /** *************************************************************
     * Compute supporting terms for the core terms in KB.
     */
    private void computeSupportingTerms(KB kb) {

        long t1 = System.currentTimeMillis();
        System.out.println("ENTER DocGen.computeSupportingTerms("
                           + kb.name + ")");

        try {
            String key = null;
            String parent = null;
            String namespace = null;
            for (Iterator it = coreTerms.keySet().iterator(); it.hasNext();) {
                key = (String) it.next();
                parent = getFirstSubsumingTerm(kb, key);
                if (StringUtil.isNonEmptyString(parent)) {
                    namespace = getTermNamespace(kb, parent);
                    if (StringUtil.isNonEmptyString(namespace)) {
                        if ((coreTerms.get(parent) == null) 
                            && (frameworkTerms.get(parent) == null)) {
                            supportingTerms.put(parent, coreTerms.get(key));
                        }
                    }
                }
            }
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }

        System.out.println("EXIT DocGen.computeSupportingTerms("
                           + kb.name + ")");
        System.out.println("  Time: "
                           + ((System.currentTimeMillis() - t1) / 1000.0)
                           + " seconds");

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
    private int computeMaxDepthFromFocalTerm(KB kb, 
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
                    if (DB.isLocalTermReference(nextTerm)) {
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
    private int getMaxDepthFromFocalTerm(KB kb, String focalTerm, Set coreTermSet) {
        long t1 = System.currentTimeMillis();
        System.out.println("  ENTER DocGen.getMaxDepthFromFocalTerm("
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

        System.out.println("  EXIT DocGen.getMaxDepthFromFocalTerm("
                           + kb.name + ", "
                           + focalTerm + ", "
                           + ((coreTermSet == null)
                              ? null
                              : ("[coreTermSet: " + coreTermSet.size() + " terms]"))
                           + ")");
        System.out.println("    ==> " + maxDepthFromFocalTerm.get(focalTerm));
        /*
          System.out.println("  Time: " 
          + ((System.currentTimeMillis() - t1) / 1000.0) 
          + " seconds");
        */

        return ans;
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
        return getMaxDepthFromFocalTerm(kb, focalTerm, null);
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
            String w3cName = DB.kifToW3c(kifTerm);
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

    /** *************************************************************
     * Returns a List containing the subordinate XmlAttributes of
     * kifTerm, else return an empty List.
     */
    private ArrayList getSubordinateAttributes(KB kb, String kifTerm) {
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
                if (DB.isLocalTermReference(kifTerm)) {
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
    private ArrayList getSubordinateElements(KB kb, String kifTerm) {
        ArrayList elems = new ArrayList();
        try {
            String pred = "subordinateXmlElement";
            List nextElems = kb.getTermsViaPredicateSubsumption(pred,
                                                                2,
                                                                kifTerm,
                                                                1,
                                                                true);

            if (nextElems.isEmpty()) {
                if (DB.isLocalTermReference(kifTerm)) {
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
    private ArrayList getInheritedSubordinates(KB kb, 
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
            if (extended.isEmpty() && DB.isLocalTermReference(kifTerm)) {
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
                if ((coreTermSet != null) && !DB.isLocalTermReference(ext)) 
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
                            sb.append(getLineSeparator());
                            sb.append(getLineSeparator());
                            Formula f = null;
                            for (Iterator itf = formulae.iterator(); itf.hasNext();) {
                                f = (Formula) itf.next();
                                sb.append(f.format("", "  ", getLineSeparator()));
                                sb.append(getLineSeparator());
                                sb.append(getLineSeparator());

                                String arg0 = f.car();
                                if (arg0.matches(".*(?i)kif.*")
                                    && StringUtil.isNonEmptyString(getTermNamespace(kb, arg0))) {
                                    Formula origF = new Formula();
                                    origF.read(f.getArgument(2));
                                    sb.append(origF.format("", "  ", getLineSeparator()));
                                    sb.append(getLineSeparator());
                                    sb.append(getLineSeparator());
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
     * A test method.
     */
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
            // KB's constituent's list.

            // 8. Reload the KB.

            // 9. Generate the various types of files.

        } 
        catch (Exception ex) {
            System.out.println(ex.getMessage());
        }
    }
} // DocGen.java
