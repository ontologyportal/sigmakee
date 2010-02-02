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

import java.lang.Math;
import java.util.*;
import java.text.*;
import java.io.*;
import javax.xml.*;
import org.w3c.dom.*;

/** A class to generate HTML and XSD files for ontologies derived from OntologyX. */
public class OntXDocGen extends DocGen {

    // Tokens for controlling output file and format types.
    // protected static final String F_SI = "si"; // default for Sigma's "simple" SUO-KIF format
    private static final String F_KIF = "kif"; // SCOW data file to KIF file
    private static final String F_DD1 = "dd1"; // default for DDEX Data Dictionary
    private static final String F_DD2 = "dd2"; // default for CCLI Data Dictionary
    private static final String F_TAB = "tab"; // DDEX "tabular" format
    private static final String F_XSD = "xsd"; // DDEX XSchema generation

    static {
        if (F_CONTROL_TOKENS == null)
            F_CONTROL_TOKENS = new ArrayList<String>();
        getControlTokens().addAll(Arrays.asList(F_KIF,F_DD1,F_DD2,F_TAB,F_XSD));
    }

    /** *************************************************************
     * The default base plus file suffix name for the main index file
     * for a set of HTML output files.
     */
    protected static String INDEX_FILE_NAME = "index.html";

    protected int localCounter = 0;

    private static final String DEFAULT_KEY = "docgen_default";

    private static Hashtable DOC_GEN_INSTANCES = new Hashtable();

    public static OntXDocGen getInstance() {
        OntXDocGen inst = null;
        try {
            inst = (OntXDocGen) DOC_GEN_INSTANCES.get(DEFAULT_KEY);
            if (inst == null) {
                inst = new OntXDocGen();
                inst.setLineSeparator(StringUtil.getLineSeparator());
                DOC_GEN_INSTANCES.put(DEFAULT_KEY, inst);
            }
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
        return inst;
    }

    public static OntXDocGen getInstance(String compositeKey) {

        // System.out.println("ENTER OntXDocGen.getInstance(" + compositeKey + ")");

        OntXDocGen inst = null;
        try {
            KBmanager mgr = KBmanager.getMgr();
            String interned = compositeKey.intern();
            inst = (OntXDocGen) DOC_GEN_INSTANCES.get(interned);
            if (inst == null) {
                inst = new OntXDocGen();
                inst.setLineSeparator(StringUtil.getLineSeparator());
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

        // System.out.println("EXIT OntXDocGen.getInstance(" + compositeKey + ")");
        // System.out.println("    inst == " + inst.toString());

        return inst;
    }

    public static OntXDocGen getInstance(KB kb, String ontology) {
        // System.out.println("ENTER OntXDocGen.getInstance(" + kb.name + ", " + ontology + ")");
        OntXDocGen inst = null;
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
        // System.out.println("EXIT OntXDocGen.getInstance(" + kb.name + ", " + ontology + ")");
        // System.out.println("    inst == " + inst.toString());
        return inst;
    }

    /** *************************************************************
     * To obtain an instance of OntXDocGen, use the static factory method
     * getInstance().
     */
    private OntXDocGen() {
    }

    /** *****************************************************************
     * A List of the full canonical pathnames of the KIF files to be
     * generated by processing CSV or DIF input files produced from an
     * Excel spreadsheet.
     */
    private ArrayList<String> kifFilePathnames = new ArrayList<String>();

    public ArrayList<String> getKifFilePathnames() {
        return this.kifFilePathnames;
    }

    public void addKifFilePathname(String pathname) {
        if (!kifFilePathnames.contains(pathname))
            kifFilePathnames.add(pathname);
        return;
    }

    /** *************************************************************
     * A Map in which the keys are SUO-KIF terms and the values are
     * Lists.  Each element in a List is a "top level" term in a
     * subsumption graph formed by syntacticSubordinate or its
     * subrelations.  The keys are the focal terms for this OntXDocGen
     * instance (e.g., MessageTypes or namespaces) .
     *
     */
    private HashMap topLevelTermsByNamespace = new HashMap();

    /** *************************************************************
     * Sets the Map containing the List of top-level terms (values)
     * for each focal term (keys) in the ontology associated with this
     * OntXDocGen instance.
     *
     * @param tops A Map in which each key is a focal term and each
     * value is a list of the top-level terms for the focal term
     */
    public void clearTopLevelTermsByNamespace() {
        try {
            synchronized (topLevelTermsByNamespace) {
                topLevelTermsByNamespace.clear();
            }
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
        return;
    }

    /** *************************************************************
     * Returns a Map in which the keys are SUO-KIF terms and the
     * values are Lists.  Each element in a List is a "top level" term
     * in a subsumption graph formed by syntacticSubordinate or its
     * subrelations.  The keys are the focal terms for the ontology
     * associated with this OntXDocGen instance (e.g., MessageTypes or
     * namespaces).  Forces computation of topLevelTermsByNamespace if
     * the Map is empty.
     */
    public HashMap getTopLevelTermsByNamespace() {
        try {
            if (topLevelTermsByNamespace.isEmpty()) {
                synchronized (topLevelTermsByNamespace) {
                    topLevelTermsByNamespace.putAll(computeTopLevelTermsByNamespace(getKB(), 
                                                                                    getOntology()));
                }
            }
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
        return topLevelTermsByNamespace;
    }

    private HashMap sumoMappings = new HashMap();
    protected Map basesToQWords = new HashMap();
    protected Map headwords = new HashMap();
    private Map namespacesToHeadwords = null;
    private Map headwordsToNamespaces = null;

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

    /** **************************************************************
     * Returns the first String token retrieved from ontology in kb
     * that denotes an HTML "Data Dictionary" output format.  The
     * tokens currently supported are as follows:
     * <code>
     *      si == Sigma's default "simple" format
     *     dd1 == the format preferred by DDEX
     *     dd2 == the format specified for CCLI
     *     tab == DDEX "tabular" format
     * </code>
     * @param kb The KB in which to look for docGenOutputFormat statements
     *
     * @param ontology A SUO-KIF term denoting an Ontology
     *
     * @return a String token, or null.
     */
    public static String getFirstHtmlFormatToken(KB kb, String ontology) {
        String token = null;
        try {
            List<String> htmlTokens = Arrays.asList(F_SI, F_DD1, F_DD2);
            if (StringUtil.isNonEmptyString(ontology)) {
                for (String tkn : getOutputFormatTokens(kb, ontology)) {
                    tkn = StringUtil.removeEnclosingQuotes(tkn);
                    if (htmlTokens.contains(tkn)) {
                        token = tkn;
                        break;
                    }
                }
            }
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
        return token;
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

                /*
                  System.out.println("\n");
                  for (Iterator itr = rows.iterator(); itr.hasNext();) {
                  System.out.println((List) itr.next());
                  }
                  System.out.println("");
                */

                String kbDir = mgr.getPref("kbDir");
                File kbDirFile = new File(kbDir);
                String base = baseFileName;
                if (StringUtil.emptyString(base))
                    base = inFileName.substring(0, lidx);
                File baseOutfile = new File(kbDirFile, (base + ".kif"));
                File tmpOutfile = StringUtil.renameFileIfExists(baseOutfile);
                OntXDocGen spi = OntXDocGen.getInstance(kb, ontology);
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
     * File generation is controlled by an int value that represents
     * bit values.  The bit values correspond to String tokens, each
     * of which denotes a type of file.  The tokens and their
     * corresponding bit values and file types are as follows:<code>
     *
     *      dd == 1: "Data Dictionary" HTML files, generic.
     *     dd1 == 2: "Data Dictionary" HTML files, format #1.
     *     dd2 == 4: "Data Dictionary" HTML files, format #2.
     *
     *     tab == 8: "Tabular display" HTML files.
     * <code>
     *
     * @param kbName The name of the KB to be used for file generation
     *
     * @param ontology The name of the ontology to be used for file
     * generation
     *
     * @param controlBits An int value representing the bit values
     * that control what kind of output files are generated, and how
     * they are generated
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
    public static String generateHtmlFiles(String kbName,
                                           String ontology,
                                           int controlBits,
                                           boolean simplified,
                                           String titleText,
                                           String footerText) {
        long t1 = System.currentTimeMillis();
        System.out.println("ENTER OntXDocGen.generateHtmlFiles(" 
                           + kbName + ", "
                           + ontology + ", "
                           + controlBits + ", "
                           + simplified + ", "
                           + titleText + ", "
                           + footerText + ")");
        String status = "";
        try {
            String ls = StringUtil.getLineSeparator();
            boolean ok = true;
            while (ok) {
                KBmanager mgr = KBmanager.getMgr();
                mgr.initializeOnce();
                KB kb = mgr.getKB(kbName);
                if (!(kb instanceof KB)) {
                    status += (ls + "There is no KB named " + kbName);
                    ok = false;
                    break;
                }

                OntXDocGen spi = OntXDocGen.getInstance(kb, ontology);
                spi.setKB(kb);
                spi.setOntology(ontology);
                spi.setOutputParentDir(kb, ontology);
                spi.clearDocGenControlBits();
                spi.addDocGenControlBits(controlBits);
                if (StringUtil.isNonEmptyString(titleText))
                    titleText = titleText.trim();
                spi.setTitleText(titleText);
                if (StringUtil.isNonEmptyString(footerText))
                    footerText = footerText.trim();
                spi.setFooterText(footerText);
                spi.getNamespaces(kb, ontology, true);
                spi.setMetaDataFromKB(kb, ontology);
                spi.getTopLevelTermsByNamespace();

                for (String token : getControlTokens()) {
                    token = StringUtil.removeEnclosingQuotes(token);
                    if (spi.testDocGenControlBits(token)) {
                        File outdir = spi.makeOutputDir(token);
                        String outdirPath = outdir.getCanonicalPath();
                        status = outdirPath;
                        spi.setOutputDirectoryPath(outdirPath);
                        spi.computeTermRelevance(kb, ontology);
                        if (token.equalsIgnoreCase(F_TAB)) {
                            spi.generateTabularFocalTermFiles(kb, ontology, outdirPath);
                        }
                        else if (Arrays.asList(F_SI, F_DD1, F_DD2).contains(token)) {
                            spi.generateHTML(kb, 
                                             spi.getDefaultNamespace(), 
                                             simplified,
                                             token);
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
                }
                ok = false;
            }
        }
        catch (Exception ex) {
            String errStr = ex.getMessage();
            status = ("Error generating files: " + errStr);
            System.out.println(errStr);
            ex.printStackTrace();
        }
        System.out.println("EXIT OntXDocGen.generateHtmlFiles(" 
                           + kbName + ", "
                           + ontology + ", "
                           + controlBits + ", "
                           + simplified + ", "
                           + titleText + ", "
                           + footerText + ")");
        System.out.println("  "
                           + ((System.currentTimeMillis() - t1) / 1000.0)
                           + " seconds total elapsed time");
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

            System.out.println("ENTER OntXDocGen.generateDocuments(\n"
                               + "  " + kbName + ",\n"
                               + "  " + ontologyName + ",\n"
                               + "  " + inputDirectory + ",\n"
                               + "  " + outputDirectory + ")");

            if (StringUtil.isNonEmptyString(kbName) 
                && StringUtil.isNonEmptyString(ontologyName)
                && StringUtil.isNonEmptyString(inputDirectory)
                && StringUtil.isNonEmptyString(outputDirectory)) {
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
                String dataFileName = kb.getFirstTermViaAskWithRestriction(0, 
                                                                           "docGenInputFile",
                                                                           1, 
                                                                           ontoTerm,
                                                                           2);
                dataFileName = StringUtil.removeEnclosingQuotes(dataFileName);
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

                // kb.addConstituent(kifFileName);

                generateHTML(kb, 
                             defaultNamespace, 
                             true, 
                             F_SI);
            }
        }
        catch (Exception ex) {
            System.out.println(ex.getMessage());
            ex.printStackTrace();
        }

        System.out.println("EXIT OntXDocGen.generateDocuments(\n"
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
                                      String formatToken) {

        /*             
                       System.out.println("ENTER OntXDocGen.createCompositePage("
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

                if (formatToken.equalsIgnoreCase(F_DD2)) {
                    markup = createCompositePage(kb, kbHref, term, alphaList, limit, language);
                }
                else {
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
                }
            }
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }

        /*              
                        System.out.println("EXIT OntXDocGen.createCompositePage("
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
                     System.out.println("ENTER OntXDocGen.createCompositePage("
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
                    System.out.println("EXIT OntXDocGen.createCompositePage("
                    + kb.name + ", "
                    + kbHref + ", "
                    + term + ", "
                    + "[map with " + alphaList.size() + " entries], "
                    + limit + ", "
                    + language + ")");
                    System.out.println("  "
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
                             String formatToken) {

        /*              
                        System.out.println("ENTER OntXDocGen.createPage("
                        + kb.name + ", "
                        + kbHref + ", "
                        + term + ", "
                        + "[alphaList with " + alphaList.size() + " entries], "
                        + limit + ", "
                        + language + ", "
                        + formatToken + ")");
        */
        String output = "";
        try {
            if (formatToken.equalsIgnoreCase(F_DD2)) {
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
                result.append(createDisplayNames(kb, kbHref, term, formatToken));
                result.append(StringUtil.getLineSeparator());
                result.append(createSynonyms(kb, kbHref, term, formatToken));
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

                result.append(createRelations(kb, kbHref, term, language, formatToken));
                result.append(StringUtil.getLineSeparator());

                result.append(createUsingSameComponents(kb, kbHref, term, language));
                result.append(StringUtil.getLineSeparator());

                result.append(createBelongsToClass(kb, kbHref, term, language, parents));
                result.append(StringUtil.getLineSeparator());

                if (!superComposites.isEmpty()) {
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
                        result.append(StringUtil.getLineSeparator());
                        result.append("  </td>");
                        result.append(StringUtil.getLineSeparator());
                        result.append("  <td> &nbsp; </td>");
                        result.append(StringUtil.getLineSeparator());
                        result.append("</tr>");
                        result.append(StringUtil.getLineSeparator());

                        result.append(formattedContainingComposites);
                        result.append(StringUtil.getLineSeparator());
                    }
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
                     System.out.println("EXIT OntXDocGen.createPage("
                     + kb.name + ", "
                     + kbHref + ", "
                     + term + ", "
                     + "[alphaList with " + alphaList.size() + " entries], "
                     + limit + ", "
                     + language + ", "
                     + formatToken + ")");
                     System.out.println("  output == " + output.length() + " chars");
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
                     System.out.println("ENTER OntXDocGen.createPage("
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
                       System.out.println("EXIT OntXDocGen.createPage("
                       + kb.name + ", "
                       + kbHref + ", "
                       + term + ", "
                       + "[map with " + alphaList.size() + " entries], "
                       + limit + ", "
                       + language + ")");
                       System.out.println("  "
                       + ((System.currentTimeMillis() - t1) / 1000.0)
                       + " seconds elapsed time");
        */
        return output;
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
                            namespace = f.getArgument(1);
                            prefix = stripNamespacePrefix(kb, namespace);
                            syn = StringUtil.removeEnclosingQuotes(f.getArgument(3));
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
            result = createSynonyms(kb, kbHref, term, F_DD2);
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
            if (isLegalForDisplay(term) && !formatToken.equalsIgnoreCase(F_DD1)) {
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
        return createDisplayNames(kb, kbHref, term, F_DD2);
    }

    private static Map<String, Integer> ontoRelnOrds = new HashMap<String, Integer>();
    private static Map<String, Integer> ruleRelnOrds = new HashMap<String, Integer>();

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

                      System.out.println("ENTER OntXDocGen.createRelations("
                      + kb.name + ", "
                      + term + ", "
                      + language + ")");                          
        */
        String result = "";
        try {
            if (ontoRelnOrds.isEmpty()) {
                List<String> ontoRelns = getRangeValueList(kb, "docGenOntologicalRelations");
                int orlen = ontoRelns.size();
                String reln = null;
                for (int i = 0; i < orlen; i++) {
                    reln = (String) ontoRelns.get(i);
                    ontoRelnOrds.put(reln, new Integer(i));
                }
            }
            if (ruleRelnOrds.isEmpty()) {
                List<String> ruleRelns = getRangeValueList(kb, "docGenRuleRelations");
                int rrlen = ruleRelns.size();
                String reln = null;
                for (int i = 0; i < rrlen; i++) {
                    reln = (String) ruleRelns.get(i);
                    ruleRelnOrds.put(reln, new Integer(i));
                }
            }
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
                            if (kb.isInstanceOf(arg0, "BinaryPredicate") 
                                || (f.listLength() == 3)) {
                                if (ruleRelations.contains(arg0)) ruleFormulae.add(f);
                                else ontoFormulae.add(f);
                            }
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
                     System.out.println("EXIT OntXDocGen.createRelations("
                     + kb.name + ", "
                     + kbHref + ", "
                     + term + ", "
                     + language + ")");
                     System.out.println("  "
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
                                     String formatToken) {
        /*
          System.out.println("ENTER OntXDocGen.createRelations("
          + kb.name + ", "
          + kbHref + ", "
          + term + ", "
          + language + ", "
          + formatToken + ")");
        */
        String result = "";
        try {
            if (formatToken.equalsIgnoreCase(F_DD2)) {
                result = createRelations(kb, kbHref, term, language);
            }
            else {
                if (isLegalForDisplay(term)) {
                    String suffix = "";
                    if (StringUtil.emptyString(kbHref)) 
                        suffix = ".html";
                    ArrayList relations = getPredicates(kb, true);

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
        /*
          System.out.println("EXIT OntXDocGen.createRelations("
          + kb.name + ", "
          + kbHref + ", "
          + term + ", "
          + language + ", "
          + formatToken + ")");
        */
        return result;
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
     * 
     * 
     */
    protected void clearTermRelevance() {
        try {
            getCoreTerms().clear();
            getSupportingTerms().clear();
            getFrameworkTerms().clear();
            getMaxDepthFromFocalTerm().clear();
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
        return;
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
            Set ftSet = new HashSet(computeFocalTerms(kb, onto));

            if (!ftSet.isEmpty()) {

                StringBuilder sb = new StringBuilder();

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
                        /*
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
                        */               
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
                        /*
                          }
                        */
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
            if (getCoreTerms().isEmpty()) 
                computeTermRelevance(kb, ontology);

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
        System.out.println("ENTER OntXDocGen.createHeadwordMap(" + kb.name + ")");
                           
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

        System.out.println("EXIT OntXDocGen.createHeadwordMap(" + kb.name + ")");
        System.out.println("  "
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
        System.out.println("ENTER OntXDocGen.createInverseHeadwordMap("
                           + kb.name + ", "
                           + "[map with " + headwordMap.size() + " entries])");

        HashMap result = new HashMap();
        try {
            String term = null;
            String headword = null;
            String h2 = null;
            List al = null;
            Iterator it = null;
            synchronized (kb.getTerms()) {
                for (it = kb.getTerms().iterator(); it.hasNext();) {
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
            }
            for (it = result.values().iterator(); it.hasNext();) {
                al = (List) it.next();
                sortByPresentationName(kb, getDefaultNamespace(), al);
            }
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }

        System.out.println("EXIT OntXDocGen.createInverseHeadwordMap("
                           + kb.name + ", "
                           + "[map with " + headwordMap.size() + " entries])");
        System.out.println("  "
                           + ((System.currentTimeMillis() - t1) / 1000.0)
                           + " seconds elapsed time");

        return result;
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
        System.out.println("ENTER OntXDocGen.generateHTML("
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

            String ontology = getOntology();
            if (StringUtil.isNonEmptyString(ontology)) {
                computeTermRelevance(kb, ontology);
            }

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
            // System.out.println("  INFO in OntXDocGen.generateHTML(): generating alpha list");
            String dir = getOutputDirectoryPath();
            // System.out.println("  INFO in OntXDocGen.generateHTML(): saving index pages");
            saveIndexPages(kb, alphaList, dir, context);

            // System.out.println("  INFO in OntXDocGen.generateHTML(): generating HTML pages");
            // Keys are formatted term names, values are HTML pages
            TreeMap pageList = generateHTMLPages(kb, 
                                                 alphaList, 
                                                 // inverseHeadwordMap, 
                                                 context, 
                                                 formatToken);
            printHTMLPages(pageList, dir);

            // System.out.println("  INFO in OntXDocGen.generateHTML(): creating single index page");
            generateSingleHTML(kb, dir, alphaList, context, simplified);
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
        System.out.println("EXIT OntXDocGen.generateHTML("
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
        System.out.println("ENTER OntXDocGen.generateSingleHTML("
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

        System.out.println("EXIT OntXDocGen.generateSingleHTML("
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
        System.out.println("ENTER OntXDocGen.cfFilesToKifHierarchyFile(");
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
                OntXDocGen spi = OntXDocGen.getInstance();

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

                System.out.println("  " 
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
                                        statements.add(OntXDocGen.makeStatement("coa:IsSubRelatorOf",
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
                    DB.writeSuoKifStatements(statements, pw);
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

        System.out.println("EXIT OntXDocGen.cfFilesToKifHierarchyFile(");
        System.out.println("  " + indirpath + ", ");
        System.out.println("  " + outdirpath + ")");
        System.out.println("  "
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
        System.out.println("ENTER OntXDocGen.computeCoreTerms("
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
        System.out.println("EXIT OntXDocGen.computeCoreTerms("
                           + kb.name + ", "
                           + focalTerm + ")");
        System.out.println("  " 
                           + ((System.currentTimeMillis() - t1) / 1000.0)
                           + " seconds elapsed time");

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
        System.out.println("ENTER OntXDocGen.computeFrameworkTerms("
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

        System.out.println("EXIT OntXDocGen.computeFrameworkTerms("
                           + kb.name + ", "
                           + ontology + ")");
        System.out.println("  "
                           + ((System.currentTimeMillis() - t1) / 1000.0)
                           + " seconds elapsed time");

        return;
    }

    /** *************************************************************
     * Compute supporting terms for the core terms in KB.
     */
    protected void computeSupportingTerms(KB kb) {

        long t1 = System.currentTimeMillis();
        System.out.println("ENTER OntXDocGen.computeSupportingTerms("
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

        System.out.println("EXIT OntXDocGen.computeSupportingTerms(" + kb.name + ")");
        System.out.println("  "
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
        System.out.println("ENTER OntXDocGen.getMaxDepthFromFocalTerm("
                           + kb.name + ", "
                           + focalTerm + ", "
                           + ((coreTermSet == null)
                              ? null
                              : ("[coreTermSet: " + coreTermSet.size() + " terms]"))
                           + ")");

        int ans = 0;
        try {
            String intStr = (String) getMaxDepthFromFocalTerm().get(focalTerm);
            if (intStr == null) {
                Set focalTermSet = new HashSet();
                focalTermSet.add(focalTerm);
                Set pathTerms = new HashSet();
                ans = computeMaxDepthFromFocalTerm(kb, 
                                                   focalTerm,
                                                   focalTermSet, 
                                                   pathTerms, 
                                                   coreTermSet);
                getMaxDepthFromFocalTerm().put(focalTerm, String.valueOf(ans));
            }
            else {
                ans = Integer.parseInt(intStr);
            }
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }

        System.out.println("EXIT OntXDocGen.getMaxDepthFromFocalTerm("
                           + kb.name + ", "
                           + focalTerm + ", "
                           + ((coreTermSet == null)
                              ? null
                              : ("[coreTermSet: " + coreTermSet.size() + " terms]"))
                           + ")");
        System.out.println("  ans == " + getMaxDepthFromFocalTerm().get(focalTerm));

        return ans;
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
            if (StringUtil.isNonEmptyString(kifTerm)) {
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
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }

        return subs;
    }

    /** *************************************************************
     * Gather the terms that should serve as the starting point for
     * exploring the graph that constitutes one or more documents.
     *
     */
    protected ArrayList computeFocalTerms(KB kb, String ontology) {
        ArrayList ans = new ArrayList();
        try {
            List docGenFocalTermFormulae = kb.askWithRestriction(0, 
                                                                 "docGenFocalTerm",
                                                                 1,
                                                                 ontology);
            if (!docGenFocalTermFormulae.isEmpty()) {
                Set reduced = new HashSet();
                Formula f = null;
                String pred = null;
                String arg2 = null;
                Iterator it2 = null;
                for (Iterator it = docGenFocalTermFormulae.iterator(); it.hasNext();) {
                    f = (Formula) it.next();
                    pred = f.getArgument(2);
                    arg2 = f.getArgument(3);
                    for (String arg1 : kb.getTermsViaAskWithRestriction(0, 
                                                                        pred, 
                                                                        2, 
                                                                        arg2,
                                                                        1)) {
                        if (!StringUtil.isLocalTermReference(arg1)) {
                            reduced.add(arg1);
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
     * Assign terms to relevance sets according to whether the term is
     * core, framework, or supporting.
     *
     */
    protected void computeTermRelevance(KB kb, String ontology) {
        if (getCoreTerms().isEmpty()) {
            /*
              long t1 = System.currentTimeMillis();
              System.out.println("ENTER OntXDocGen.computeTermRelevance("
              + kb.name + ", "
              + ontology + ")");
            */
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
            /*
              System.out.println("EXIT OntXDocGen.computeTermRelevance("
              + kb.name + ", "
              + ontology + ")");
              System.out.println("  " 
              + ((System.currentTimeMillis() - t1) / 1000.0)
              + " seconds elapsed time");
            */
        }
        return;
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
            if (StringUtil.isNonEmptyString(term)) {
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

        System.out.println("ENTER OntXDocGen.exportFormulaeToFile("
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

        System.out.println("EXIT OntXDocGen.exportFormulaeToFile("
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
     * Returns the list of file type suffixes that identify input SCOW
     * spreadsheet files.
     */
    private static final List getInputFileSuffixes() {
        return Arrays.asList(".dif", ".csv");
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
     * OntXDocGen.processSpreadsheet().
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
                    List formulae = kb.askWithRestriction(0,
                                                          "tableIndexColumnNames",
                                                          1, 
                                                          ontology);
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
                        System.out.println("WARNING in OntXDocGen.readSpreadsheetFile(" 
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
            System.out.println("ERROR in OntXDocGen.getSpreadsheetValue("
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
          System.out.println("ENTER OntXDocGen.normalizeTermsInFormula("
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
          System.out.println("EXIT OntXDocGen.normalizeTermsInFormula("
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
        System.out.println("ENTER OntXDocGen.normalizeTerms([" 
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

        System.out.println("EXIT OntXDocGen.normalizeTerms([" 
                           + rows.size() + " rows], "
                           + defaultNamespace + ")");
        System.out.println("  "
                           + ((System.currentTimeMillis() - t1) / 1000.0)
                           + " seconds elapsed time");

        return normalized;
    }

    /** *******************************************************************
     *
     */
    private String normalizeTermsInString(String input, List nsHeadwords, String namespace) {
        /*
          System.out.println("ENTER OntXDocGen.normalizeTermsInString(" 
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
          System.out.println("EXIT OntXDocGen.normalizeTermsInString(" 
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
        System.out.println("ENTER OntXDocGen.normalizeTermsInComplexFields("
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

        System.out.println("EXIT OntXDocGen.normalizeTermsInComplexFields(["
                           + "[" + rows.size() + " rows], " 
                           + "[" + hwsToNamespaces.size() + " map entries], "
                           + defaultNamespace + ")");
        System.out.println("  "
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
                    throw new Exception("Error in OntXDocGen.makeStatement(" 
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
     */
    public File processDataRows(KB kb,
                                List rows, 
                                String ontology, 
                                String baseFileName,
                                File kifOutputFile,
                                boolean overwrite,
                                boolean loadConstituent) {

        long t1 = System.currentTimeMillis();

        System.out.println("ENTER OntXDocGen.processDataRows("
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
                                            arg1 = range;
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
                                        if (isNonEmptyString(rangeDataType))
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
                                    // rangeRegex = rangeRegex.replace("\\","\\\\");
                                    // rangeRegex = rangeRegex.replace("\\","\\\\");
                                    rangeRegex = StringUtil.escapeEscapeChars(rangeRegex);
                                    // rangeRegex = StringUtil.escapeEscapeChars(rangeRegex);
                                    // rangeRegex = StringUtil.escapeEscapeChars(rangeRegex);
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

                System.out.println("  " + nRowsSkipped + " rows skipped");

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
                kb.buildRelationCaches(false);
                computeDerivations(kb);
                kb.buildRelationCaches(false);
                DB.writeSuoKifStatements(kb, kifCanonicalPath);

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
                    if (!overwrite
                        || cfCanonicalPath.equalsIgnoreCase(kifCanonicalPath)) {
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
                            System.out.println("INFO in OntXDocGen.processDataRows("
                                               + kb.name + ", "
                                               + "[" + rows.size() + " rows], "
                                               + ontology + ", "
                                               + baseFileName + ", "
                                               + kifOutputFile + ", "
                                               + overwrite + ", "
                                               + loadConstituent + ")");
                            System.out.println("  removed " + cnst);
                            System.out.println("  kb.constituents == " + kb.constituents);
                        }
                    }
                    if (loadConstituent) {
                        System.out.println("  adding constituent " + cfCanonicalPath);
                        kb.constituents.add(cfCanonicalPath);
                        System.out.println("  kb.constituents == " + kb.constituents);
                    }
                    KBmanager.getMgr().writeConfiguration();
                    kb.reload();
                    newConstituentFile = constituentFile;
                }
            } // end synchronized (kb)

            // Resort kb.terms to ignore namespace prefixes, where
            // appropriate.
            this.resortKbTerms(kb);
        }
        catch (Exception ex) {
            System.out.println("ERROR in OntXDocGen.processDataRows("
                               + kb.name + ", "
                               + "[" + rows.size() + " rows], "
                               + ontology + ", "
                               + baseFileName + ", "
                               + kifOutputFile + ", "
                               + overwrite + ", "
                               + loadConstituent + ")");
            System.out.println("  Error writing file " + kifCanonicalPath);
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
        System.out.println("EXIT OntXDocGen.processDataRows("
                           + kb.name + ", "
                           + "[" + rows.size() + " rows], "
                           + ontology + ", "
                           + baseFileName + ", "
                           + kifOutputFile + ", "
                           + overwrite + ", "
                           + loadConstituent + ")");
        System.out.println("  "
                           + ((System.currentTimeMillis() - t1) / 1000.0)
                           + " seconds elapsed time");
        return newConstituentFile;
    }

    /** *******************************************************************
     * NB: This method alters the List rows by normalizing all term
     * names.
     */
    public File processSpreadsheet(KB kb,
                                   List rows, 
                                   String ontology, 
                                   String kifOutputFilePathname) {

        long t1 = System.currentTimeMillis();

        System.out.println("ENTER OntXDocGen.processSpreadsheet("
                           + kb.name + ", "
                           + "[" + rows.size() + " rows], "
                           + ontology + ", "
                           + kifOutputFilePathname + ")");
        File result = null;
        String resultCanonicalPath = null;
        try {
            File outfile = new File(kifOutputFilePathname);
            String baseFileName = outfile.getName();
            int lidx = baseFileName.lastIndexOf(".");
            if (lidx > -1)
                baseFileName = baseFileName.substring(0, lidx);
            result = processDataRows(kb, 
                                     rows, 
                                     ontology, 
                                     baseFileName, 
                                     outfile, 
                                     true, 
                                     true);
            if (result != null)
                resultCanonicalPath = result.getCanonicalPath();
        }
        catch (Exception ex) {
            System.out.println("Error writing file " + kifOutputFilePathname);
            ex.printStackTrace();
        }

        System.out.println("EXIT OntXDocGen.processSpreadsheet("
                           + kb.name + ", "
                           + "[" + rows.size() + " rows], "
                           + ontology + ", "
                           + kifOutputFilePathname + ")");
        System.out.println("  result == " + resultCanonicalPath);
        System.out.println("  "
                           + ((System.currentTimeMillis() - t1) / 1000.0)
                           + " seconds elapsed time");
        return result;
    }

    /*
      public void processSpreadsheet(KB kb,
      List rows, 
      String ontology, 
      String kifOutputFilePathname) {

      long t1 = System.currentTimeMillis();

      System.out.println("ENTER OntXDocGen.processSpreadsheet("
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

      // makeMappingStatements(rowIdx, 
      // statements, 
      // subject, 
      // mappingAuthority,
      // mappingRelator,
      // mappingRange);

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
      if (isNonEmptyString(rangeDataType))
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

      System.out.println("  " + nRowsSkipped + " rows skipped");

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
      DB.writeSuoKifStatements(kb, kifOutputFilePathname);

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

      System.out.println("EXIT OntXDocGen.processSpreadsheet("
      + kb.name + ", "
      + "[" + rows.size() + " rows], "
      + ontology + ", "
      + kifOutputFilePathname + ")");
      System.out.println("  "
      + ((System.currentTimeMillis() - t1) / 1000.0)
      + " seconds elapsed time");
      return;
      }
    */

    /** *******************************************************************
     * 
     */
    private void computeDerivations(KB kb) {

        long t1 = System.currentTimeMillis();
        System.out.println("ENTER OntXDocGen.computeDerivations(" + kb.name + ")");

        try {
            Map<String, Object> memo = new HashMap<String, Object>();

            // Compute and add inverse statements.
            System.out.print("  computing inverse statements ");
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
                        if (!Arrays.asList("no_inverse", arg0).contains(inv)) {
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
                                sb.append(f.theFormula);
                                sb.append(" ");
                                sb.append(derivedF.theFormula);
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
            System.out.println("  " + count + " statements computed");

            if (!newFormulae.isEmpty()) {
                List<Formula> formulaList = new LinkedList<Formula>(newFormulae);
                newFormulae.clear();
                synchronized (kb.getTerms()) {
                    kb.formulas.clear();
                    kb.formulaMap.clear();
                    kb.getTerms().clear();
                    kb.clearFormatMaps();
                    addFormulaeToKB(kb, formulaList);
                }
            }
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }

        System.out.println("EXIT OntXDocGen.computeDerivations(" + kb.name + ")");
        System.out.println("  "
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
        System.out.println("ENTER OntXDocGen.addStatementsToKB("
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

                System.out.print("  adding KIF.formulaSet to KB.formulaMap ");
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
                          sb.append(f.theFormula);
                          sb.append(kb.getLineSeparator());
                          sb.append(" loaded from "); 
                          sb.append(f.getSourceFile());
                          errors.add(sb.toString());
                        */
                    }
                    if ((count++ % 100) == 1) System.out.print(".");
                }
                System.out.println("x");

                System.out.print("  adding KIF.formulas to KB.formulas ");
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
                                  sb.append(f.theFormula);
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
                synchronized (kb.getTerms()) {
                    System.out.print("  adding KIF.terms to KB.terms ");
                    int oldSize = kb.getTerms().size();
                    kb.getTerms().addAll(kif.terms);
                    System.out.println("... " 
                                       + (kb.getTerms().size() - oldSize)
                                       + " terms added");

                    System.out.println("  adding " 
                                       + constituentCanonicalPath
                                       + " to KB.constituents");
                    if (!kb.constituents.contains(constituentCanonicalPath))
                        kb.constituents.add(constituentCanonicalPath);

                    System.out.println("  clearing KB.formatMap and KB.termFormatMap");
                    kb.clearFormatMaps();
                }
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
                esb1.append("WARNING in OntXDocGen.addStatementsToKB(");
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
        System.out.println("  numberAdded == " + numberAdded);
        System.out.println("EXIT OntXDocGen.addStatementsToKB("
                           + kb.name + ", "
                           + "[set of " + statements.size() + " statements], "
                           + constituentCanonicalPath + ")");
        System.out.println("  "
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
        System.out.println("ENTER OntXDocGen.addFormulaeToKB("
                           + kb.name + ", "
                           + "[list of " + formulaList.size() + " formulae])");
        int duplicateCount = 0;
        int numberAdded = 0;
        Set<String> duplicateFormulae = new HashSet<String>();
        Set errors = new LinkedHashSet();
        KIF kif = null;
        String sourcePath = null;
        try {
            if ((formulaList != null) && !formulaList.isEmpty()) {
                synchronized (kb.getTerms()) {

                    // 1. Add the Formulae in formulaList to
                    // kb.formulaMap.  At the same time, add each
                    // statement to a StringBuilder so that the statements
                    // can be read with a KIF instance, as if processing a
                    // KIF constituent file.
                    StringBuilder sb = new StringBuilder();
                    Formula   f = null;
                    String  key = null;

                    System.out.print("  adding "
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
                    System.out.println("  KIF.parse(): reading statements from StringReader");
                    kif = new KIF();
                    kif.setFilename("");
                    StringReader sr = new StringReader(sb.toString());
                    errors.addAll(kif.parse(sr));
                    sr.close();

                    // 3. Populate KB.formulas from KB.formulaMap and
                    // KIF.formulas.
                    System.out.print("  adding KIF.formulas to KB.formulas ");
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
                                    duplicateFormulae.add(f.theFormula.intern());
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

                    duplicateCount = duplicateFormulae.size();
                    if (duplicateCount > 0) {
                        errors.add(duplicateCount + " duplicate statement"
                                   + ((duplicateCount > 1) ? "s " : " ")
                                   + "detected in " 
                                   + (StringUtil.emptyString(sourcePath)
                                      ? "the input file"
                                      : sourcePath));
                    }
                    System.out.print("  adding KIF.terms to KB.terms ");
                    int oldSize = kb.getTerms().size();
                    kb.getTerms().addAll(kif.terms);
                    System.out.println("... " 
                                       + (kb.getTerms().size() - oldSize)
                                       + " terms added");

                    if (!newConstituents.isEmpty()) {
                        System.out.println("  adding " + newConstituents + " to KB.constituents");
                        newConstituents.addAll(kb.constituents);
                        kb.constituents.clear();
                        kb.constituents.addAll(newConstituents);
                        Collections.sort(kb.constituents);
                        newConstituents.clear();
                    }

                    System.out.println("  clearing KB.formatMap and KB.termFormatMap");
                    kb.clearFormatMaps();
                }
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
                esb1.append("WARNING in OntXDocGen.addFormulaeToKB(");
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

        System.out.println("  numberAdded == " + numberAdded);
        System.out.println("EXIT OntXDocGen.addFormulaeToKB("
                           + kb.name + ", "
                           + "[list of " + formulaList.size() + " formulae])");
        System.out.println("  "
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
          System.out.println("ENTER OntXDocGen.readKif(" + input + ")");
        */

        ArrayList<Formula> formulae = new ArrayList<Formula>();
        try {
            if (StringUtil.isNonEmptyString(input)) {
                KIF parser = new KIF();
                parser.setParseMode(KIF.RELAXED_PARSE_MODE);
                String err = parser.parseStatement(input);

                if (StringUtil.isNonEmptyString(err)) {
                    System.out.println("ERROR in OntXDocGen.readKif(" + input + ")");
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
            System.out.println("ERROR in OntXDocGen.readKif(" + input + ")");
            System.out.println(ex.getMessage());
            ex.printStackTrace();
        }

        /*
          System.out.println("EXIT OntXDocGen.readKif(" + input + ")");
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
                System.out.println("WARNING in OntXDocGen.createXsdDocument("
                                   + kb.name + ", "
                                   + namespace + ")");
                System.out.println("  No value is set for kbDir");
            }
            File indir = new File(indirPath);
            if (!indir.isDirectory() || !indir.canRead()) {
                System.out.println("WARNING in OntXDocGen.createXsdDocument("
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
                System.out.println("WARNING in OntXDocGen.loadXsdSkeletonFile("
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
            System.out.println("INFO in OntXDocGen.loadXsdSkeletonFile("
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
          System.out.println("EXIT OntXDocGen.getSubordinateXmlElementTerms("
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
          System.out.println("EXIT OntXDocGen.getSubordinateXmlAttributeTerms("
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
          System.out.println("ENTER OntXDocGen.setElementCardinality("
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
          System.out.println("EXIT OntXDocGen.setElementCardinality("
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
                // pattern = StringUtil.escapeEscapeChars(pattern);
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
     * Computes all top-level terms for each namespace-specific XSD
     * file.  Returns a Map in which the keys are KIF namespace terms,
     * and the values are Lists of all top-level XSD terms in the
     * indexed namespaces.
     *
     */
    private HashMap computeTopLevelTermsByNamespace(KB kb, String ontology) {

        long t1 = System.currentTimeMillis();
        System.out.println("ENTER OntXDocGen.computeTopLevelTermsByNamespace("
                           + kb.name + ", "
                           + ontology + ")");
        HashMap tops = new HashMap();
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

        System.out.println("EXIT OntXDocGen.computeTopLevelTermsByNamespace("
                           + kb.name + ", "
                           + ontology + ")");
        System.out.println("  [" + tops.size() + " keys computed]");
        System.out.println("  " 
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
            String nsTerm = toKifNamespace(kb, namespace);
            List termsInNamespace = getTermsInNamespace(kb, nsTerm, true);

            if (!topLevelTerms.isEmpty()) {

                List filteredTerms = new ArrayList();
                String ft = null;
                String nativeType = null;
                Iterator itt = null;
                for (itt = topLevelTerms.iterator(); itt.hasNext();) {
                    ft = (String) itt.next();
                    nativeType = substituteXsdDataType(kb, nsTerm, ft);
                    if (!nativeType.startsWith("xs:")
                        || nativeType.equals("xs:IDREF")
                        || nativeType.equals("xs:ID")) {
                        filteredTerms.add(ft);
                    }
                }
                topLevelTerms = filteredTerms;

                // sortByPresentationName(kb, nsTerm, topLevelTerms);

                System.out.println("  topLevelTerms for " + namespace + " == " + topLevelTerms);

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
                for (itt = topLevelTerms.iterator(); itt.hasNext();) {
                    term = (String) itt.next();
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
                            Element _enumeration = makeEnumerationElement(kb, 
                                                                          _doc, 
                                                                          _simpleType,
                                                                          nsTerm,
                                                                          nsTerm, 
                                                                          m,
                                                                          nsList);
                            /*
                              makeElement(_doc, "xs:enumeration");
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
                            */
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

        System.out.println("ENTER OntXDocGen.writeXsdFiles("
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
            List refTermsForNS = null;
            String nsStr = null;
            String nsTerm = null;
            String nsName = null;
            for (Iterator it = namespaces.iterator(); it.hasNext();) {
                nsStr = (String) it.next();
                nsTerm = toKifNamespace(kb, nsStr);

                System.out.println("Processing terms for " + nsTerm);

                Set reducedTops = new HashSet();
                List cachedTops = (List) (getTopLevelTermsByNamespace().get(nsTerm));
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
                                if (focalTerms.contains(term)) {

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

        System.out.println("EXIT OntXDocGen.writeXsdFiles("
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
                synchronized (kb.getTerms()) {
                    for (Iterator it = kb.getTerms().iterator(); it.hasNext();) {
                        term = (String) it.next();
                        if (getTermNamespace(kb, term).equals(kifNs)) {
                            result.add(term);
                        }
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
                synchronized (kb.getTerms()) {
                    String term = null;
                    for (Iterator it = kb.getTerms().iterator(); it.hasNext();) {
                        term = (String) it.next();
                        if (namespaces.contains(getTermNamespace(kb, term))) {
                            result.add(term);
                        }
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
                    pw.println(OntXDocGen.indentChars("    ", indent) 
                               + "<" + nodeName + ">"
                               + txt
                               + "</" + nodeName + ">");
                }
                else {
                    if (nodeName.matches(".*(?i)schema.*")) {
                        pw.println("<?xml version=\"1.0\"?>");
                    }
                    pw.print(OntXDocGen.indentChars("    ", indent) + "<" + nodeName);
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
                              spacer = (OntXDocGen.indentChars("    ", indent) 
                              + OntXDocGen.indentChars(" ", nodeName.length())
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
                        pw.println(OntXDocGen.indentChars("    ", indent)
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

        System.out.println("ENTER OntXDocGen.generateTabularFocalTermFiles("
                           + kb.name + ", "
                           + ontology + ", "
                           + outdirPath + ")");

        try {
            setOntology(ontology);
            List focalTerms = computeFocalTerms(kb, ontology);
            computeTermRelevance(kb, ontology);
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

        System.out.println("EXIT OntXDocGen.generateTabularFocalTermFiles("
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
                System.out.println("ERROR in OntXDocGen.createFocalTermPrintWriter("
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
          System.out.println("ENTER OntXDocGen.writeTabularFocalTermRows("
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
          System.out.println("EXIT OntXDocGen.writeTabularFocalTermRows("
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

    /** ********************************************************************
     * This method generates sets of documents from a KB based on the
     * directive String tokens provided in the input List args.  The
     * List args must contain at least three tokens.
     *
     * The first token must be the name of a Sigma KB, which will be
     * constructed by loading one or more KIF files prior to document
     * generation.
     *
     * The second token must be the name of an ontology that is
     * defined in the KB identified by the first token.
     *
     * Each of the third and subsequent tokens indicates a specific
     * type or format of output file to be generated.  These tokens
     * may occur in any combination and order.  If the token
     * &quot;kif&quot; is among the tokens in args, the procedure will
     * try to translate the input DIF or CSV data files to KIF files
     * and then load the KIF files before trying to generate files of
     * the types indicated by the other tokens.  Note that each file
     * type token is interpolated between kbName and a String
     * representing the current date in order to generate the name of
     * the directory in which files of a specific type will be saved.
     * The supported tokens and corresponding types of output files
     * are as follows:<code>
     *
     *     kif == 1: KIF files, generated by processing data input files in DIF or CSV format
     *
     *      dd == 2: Data Dictionary HTML files, generic format
     *     dd1 == 4: Data Dictionary HTML files, format #1
     *     dd2 == 8: Data Dictionary HTML files, format #2
     *
     *     tab == 16: "Tabular display" HTML files
     *
     *     xsd == 32: XML Schema (aka XSD) files
     *</code>
     *
     * @param args A List of tokens that control the behavior of this
     * method
     *
     * @return A String indicating the exit status of this method
     */
    public static String generateDocumentsFromKB(List<String> args) {
        String result = "";
        try {
            while (true) {
                if (!(args instanceof List)) {
                    result = "Error: No input arguments were entered";
                    break;
                }
                if (args.isEmpty()) {
                    result = "Error: Too few input arguments were entered";
                    break;
                }
                while (args.remove("rcdocgen")) {
                    continue;
                }
                if (args.size() < 3) {
                    result = "Error: Too few input arguments were entered";
                    break;
                }
                String kbName = (String) args.remove(0);
                String ontology = (String) args.remove(0);
                int controlBits = 0;
                for (String token : args) {
                    controlBits = (controlBits | getControlBitValue(token));
                }
                if (controlBits == 0) {
                    result = ("Error: The input arguments " + args + " were not understood");
                    break;
                }
                result = generateDocumentsFromKB(kbName, ontology, controlBits);
                break;
            }
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
        return result;
    }

    /** ********************************************************************
     * This method generates sets of documents from ontology in the KB
     * identified by kbName.  The types and formats of the output
     * files to be generated are determined by the value of
     * controlBits.
     *
     * @param kbName The name of a KB currently loaded in Sigma
     *
     * @param ontology The name of an ontology represented in the KB
     * identified by kbName
     *
     * @param controlBits An int value that represents the bit values
     * that control the types or formats of the output files to be
     * generated
     *
     * @return A String indicating the exit status of this method
     */
    public static String generateDocumentsFromKB(String kbName, 
                                                 String ontology,
                                                 int controlBits) {
        String status = "";
        try {            
            status += generateDocumentsFromKB(kbName,
                                              ontology,
                                              controlBits,
                                              true,  // simplified
                                              "",
                                              "");
        }
        catch (Throwable th) {
            System.out.println(th.getMessage());
            th.printStackTrace();
        }
        return status;
    }

    /** ********************************************************************
     * This method generates sets of documents from an ontology in the
     * KB identified by kbName.  The types and formats of the output
     * files to be generated are determined by the bit values
     * represented in the int controlBits.  These values correspond to
     * the meanings of a set of String tokens, which are also used for
     * application control.  The String tokens are defined in this
     * file as static constants: F_KIF, F_SI, F_DD1, F_DD2, F_TAB,
     * F_XSD.  
     * <code>
     *
     *   F_KIF == "kif" == 1 : Tells the procedure to generate new KIF
     *   files from .dif or .csv input files
     *
     *   F_SI == "dd" == 2 : Tells the procedure to generate a Data
     *   Dictionary in "generic" format
     *
     *   F_DD1 == "dd1" == 4 : Tells the procedure to generate a Data
     *   Dictionary in the format specified for DDEX
     *
     *   F_DD2 == "dd2" == 8 : Tells the procedure to generate a Data
     *   Dictionary in the format specified for CCLI
     *
     *   F_TAB == "tab" == 16 : Tells the procedure to generate one
     *   file in DDEX's "tabular" format
     *
     *   F_XSD == "xsd" == 32 : Tells the procedure to generate XSD
     *   files; so far, this is really supported only for DDEX
     * 
     * <code>
     * @param kbName The name of a KB currently loaded in Sigma
     *
     * @param ontology The name of an ontology represented in the KB
     * identified by kbName
     *
     * @param controlBits An int value that represents the bit values
     * that control the types or formats of the output files to be
     * generated
     *
     * @param simplified If true, used some variation of the
     * simplified browser display
     *
     * @param headerText If a non-empty String, this value will be
     * used as the title of each page
     *
     * @param footerText If a non-empty String, this value will be
     * used as the footer text of each page
     *
     * @return A directory pathname String if all went well, or a
     * message beginning with Error if all did not go well
     */
    public static String generateDocumentsFromKB(String kbName, 
                                                 String ontology,
                                                 int controlBits,
                                                 boolean simplified,
                                                 String headerText,
                                                 String footerText) {

        long t1 = System.currentTimeMillis();
        long t2 = t1;
        System.out.println("ENTER OntXDocGen.generateDocumentsFromKB(" 
                           + kbName + ", "
                           + ontology + ", "
                           + controlBits + ", "
                           + simplified + ", "
                           + headerText + ", "
                           + footerText + ")");

        long kbInitElapsedTime = 0;
        long inputFileElapsedTime = 0;
        long tltElapsedTime = 0;
        long xsdElapsedTime = 0;
        long ddElapsedTime = 0;
        long tabElapsedTime = 0;
        long relevanceElapsedTime = 0;
        String ls = StringUtil.getLineSeparator();
        String status = "";
        OntXDocGen spi = null;
        try {            
            boolean ok = true;
            while (ok) {
                KBmanager mgr = KBmanager.getMgr();
                mgr.initializeOnce();

                KB kb = mgr.getKB(kbName);
                if (!(kb instanceof KB)) {
                    status += ("Error: There is no KB named " + kbName + ls);
                    ok = false;
                    break;
                }

                spi = OntXDocGen.getInstance(kb, ontology);
                spi.setKB(kb);
                spi.setOntology(ontology);
                spi.setOutputParentDir(kb, ontology);
                spi.clearDocGenControlBits();
                spi.addDocGenControlBits(controlBits);
                if (StringUtil.isNonEmptyString(headerText))
                    spi.setTitleText(headerText);
                if (StringUtil.isNonEmptyString(footerText))
                    spi.setFooterText(footerText);

                kbInitElapsedTime += (System.currentTimeMillis() - t2);
                t2 = System.currentTimeMillis();

                for (String token : getControlTokens()) {
                    token = StringUtil.removeEnclosingQuotes(token);

                    if (spi.testDocGenControlBits(token)) {

                        System.out.println("  token == " + token);

                        if (token.equalsIgnoreCase(F_KIF)) {

                            t2 = System.currentTimeMillis();

                            spi.getKifFilePathnames().clear();
                            String indirPath = mgr.getPref("kbDir");
                            File indir = (StringUtil.emptyString(indirPath)
                                          ? null
                                          : new File(indirPath));
                            if ((indir == null) || !indir.isDirectory()) {
                                String sigmaHome = System.getenv("SIGMA_HOME");
                                if (StringUtil.isNonEmptyString(sigmaHome)) {
                                    File baseDir = new File(sigmaHome);
                                    indir = new File(baseDir, "KBs");
                                }
                            }
                            if (!indir.isDirectory() || !indir.canRead()) {
                                status += ("Error: " + indir.getCanonicalPath() 
                                           + " is not an accessible directory" + ls);
                                ok = false;
                                break;
                            }
                            for (String infileName : 
                                     kb.getTermsViaAskWithRestriction(0,
                                                                      "docGenInputFile",
                                                                      1, 
                                                                      ontology,
                                                                      2)) {
                                infileName = StringUtil.removeEnclosingQuotes(infileName);
                                File inputFile = new File(indir, infileName);
                                String inputFileCanonicalPath = inputFile.getCanonicalPath();

                                // System.out.println("inputFileCanonicalPath == " 
                                //                    + inputFileCanonicalPath);

                                if (!inputFile.canRead()) {
                                    status += ("Error: The file " + inputFileCanonicalPath
                                               + " does not exist or cannot be read" + ls);
                                    ok = false;
                                    break;
                                }

                                int suffIdx = -1;
                                String suff = "";
                                for (Iterator ifi = getInputFileSuffixes().iterator(); 
                                     ifi.hasNext();) {
                                    String suffstr = (String) ifi.next();
                                    suffIdx = inputFileCanonicalPath.lastIndexOf(suffstr);
                                    if (suffIdx > -1) {
                                        suff = suffstr;
                                        break;
                                    }
                                }
                                String kifFilename = 
                                    ((suffIdx > 0)
                                     ? (inputFileCanonicalPath.substring(0, suffIdx) + ".kif")
                                     : (inputFileCanonicalPath + ".kif"));
                                File kifOutputFile = new File(kifFilename);
                                String kifOutputFileCanonicalPath = 
                                    kifOutputFile.getCanonicalPath();

                                List rows = null;
                                if (suff.equalsIgnoreCase(".csv")) {
                                    rows = DB.readSpreadsheet(inputFileCanonicalPath, 
                                                              Arrays.asList("u,", "U,", "$,"));
                                }
                                else if (suff.equalsIgnoreCase(".dif")) {
                                    rows = DB.readDataInterchangeFormatFile(inputFileCanonicalPath);
                                }
                                if ((rows == null) || rows.isEmpty()) {
                                    status += ("Error: No data could be read from " 
                                               + inputFileCanonicalPath + ls);
                                    ok = false;
                                    break;
                                }
                    
                                kifOutputFile = spi.processSpreadsheet(kb, 
                                                                       rows, 
                                                                       ontology, 
                                                                       kifOutputFileCanonicalPath);

                                // kb.addConstituent(kifCanonicalPath);

                                if ((kifOutputFile == null) || !kifOutputFile.canRead()) {
                                    status += ("Error: The file " + kifOutputFileCanonicalPath 
                                               + " is not accessible" + ls);
                                    ok = false;
                                    break;
                                }

                                status += (kifOutputFileCanonicalPath + ls);
                                spi.addKifFilePathname(kifOutputFile.getCanonicalPath());
                            }

                            inputFileElapsedTime += (System.currentTimeMillis() - t2);

                            if (!ok) break;

                            spi.clearAlphaList();
                            spi.clearTopLevelTermsByNamespace();
                            spi.clearTermRelevance();
                        }
                        else {

                            spi.resortKbTerms(kb);

                            t2 = System.currentTimeMillis();

                            spi.setMetaDataFromKB(kb, ontology);
                            spi.getTopLevelTermsByNamespace();
                            File outdir = spi.makeOutputDir(token);
                            String outdirPath = outdir.getCanonicalPath();
                            spi.setOutputDirectoryPath(outdirPath);
                            tltElapsedTime += (System.currentTimeMillis() - t2);

                            t2 = System.currentTimeMillis();

                            if (token.equalsIgnoreCase(F_XSD)) {
                                spi.writeXsdFiles(kb, 
                                                  ontology, 
                                                  spi.getDefaultNamespace(),
                                                  outdirPath);
                                status += (outdirPath + ls);
                                xsdElapsedTime += (System.currentTimeMillis() - t2);
                                t2 = System.currentTimeMillis();
                            }
                            else {
                                if (spi.getCoreTerms().isEmpty()) {
                                    spi.computeTermRelevance(kb, ontology);
                                    relevanceElapsedTime += (System.currentTimeMillis() - t2);
                                    t2 = System.currentTimeMillis();
                                }

                                if (token.equalsIgnoreCase(F_TAB)) {
                                    spi.generateTabularFocalTermFiles(kb, ontology, outdirPath);
                                    status += (outdirPath + ls);
                                    tabElapsedTime += (System.currentTimeMillis() - t2);
                                    t2 = System.currentTimeMillis();
                                }
                                else if (Arrays.asList(F_SI, F_DD1, F_DD2).contains(token)) {
                                    spi.generateHTML(kb, 
                                                     spi.getDefaultNamespace(), 
                                                     simplified,
                                                     token);
                                    status += (spi.getOutputDirectoryPath() + ls);
                                    ddElapsedTime += (System.currentTimeMillis() - t2);
                                    t2 = System.currentTimeMillis();
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
                                        File kbDirFile = 
                                            new File(KBmanager.getMgr().getPref("kbDir"));
                                        File infile = new File(kbDirFile, fname);
                                        File outfile = new File(outdir, fname);
                                        KBmanager.copyFile(infile, outfile);
                                    }
                                }
                            }
                        }
                    }
                }
                ok = false;
            }
        }
        catch (Throwable th) {
            System.out.println(th.getMessage());
            th.printStackTrace();
        }

        System.out.println("EXIT OntXDocGen.generateDocumentsFromKB(" 
                           + kbName + ", "
                           + ontology + ", "
                           + controlBits + ", "
                           + simplified + ", "
                           + headerText + ", "
                           + footerText + ")");
        System.out.println("    "
                           + ((System.currentTimeMillis() - t1) / 1000.0)
                           + " seconds total elapsed time");
        System.out.println("        "
                           + (kbInitElapsedTime / 1000.0)
                           + " seconds to build initial KB");
        if (spi.testDocGenControlBits(F_KIF)) {
            List generatedKifFiles = spi.getKifFilePathnames();
            if (!generatedKifFiles.isEmpty()) {
                System.out.println("        " 
                                   + (inputFileElapsedTime / 1000.0) 
                                   + " seconds to generate KIF files: " 
                                   + spi.getKifFilePathnames());
            }
        }
        if (spi.testDocGenControlBits(F_SI)
            || spi.testDocGenControlBits(F_DD1)
            || spi.testDocGenControlBits(F_DD2)
            || spi.testDocGenControlBits(F_TAB)
            || spi.testDocGenControlBits(F_XSD)) {
            if (tltElapsedTime > 0L) {
                System.out.println("        " 
                                   + (tltElapsedTime / 1000.0) 
                                   + " seconds to compute top-level terms");
            }
            if (xsdElapsedTime > 0L) {
                System.out.println("        " 
                                   + (xsdElapsedTime / 1000.0) 
                                   + " seconds to generate XSD files");
            }
            if (ddElapsedTime > 0L) {
                System.out.println("        " 
                                   + (relevanceElapsedTime / 1000.0) 
                                   + " seconds to compute term relevance");
                System.out.println("        " 
                                   + (ddElapsedTime / 1000.0) 
                                   + " seconds to generate Data Dictionary files");
            }
            if (tabElapsedTime > 0L) {
                System.out.println("        " 
                                   + (tabElapsedTime / 1000.0) 
                                   + " seconds to generate tabular display files");
            }
        }
        return status;
    }

    /** *************************************************************
     * 
     */
    public static void main (String[] args) {
        String status = "";
        String ls = StringUtil.getLineSeparator();
        try {
            if (args.length < 3) {
                System.out.println("Too few arguments were entered");
                System.exit(1);
            }
            ArrayList<String> arglist = new ArrayList<String>(Arrays.asList(args));
            status += generateDocumentsFromKB(arglist);
            System.out.println(status);
        }
        catch (Throwable th) {
            System.out.println(status);
            System.out.println(th.getMessage());
            th.printStackTrace();
        }
        return;
    }

} // OntXDocGen.java
