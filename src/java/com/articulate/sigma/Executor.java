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

/** ****************************************************************
 * The purpose of this class is to generate instances that serve as
 * wrappers and convenient access points for applications intended to
 * be invoked from the command line.  It can also be used for testing.
 * No methods, members, or any other references to this class should
 * ever appear in any other part of the Sigma source code.  It should
 * be possible to remove this file from the source tree and still
 * compile a working Sigma application.
 */
public class Executor {


    /** ****************************************************************
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
     * file from an Excel-derived CSV file (i.e., a SCOW).  The KIF
     * file is then loaded into the KB and serves as the basis for the
     * generation of sets of files indicated by the remaining tokens,
     * if any.
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
     *
     *     tab - "Tabular display" HTML files
     *
     * @param args A String[] of tokens that control the behavior of this
     * method
     *
     * @return A String indicating the exit status of this method
     *
     */
    private static String generateDocumentsFromKB(List<String> args) {

        long t1 = System.currentTimeMillis();
        long csvElapsedTime = 0;
        long tltElapsedTime = 0;
        long xsdElapsedTime = 0;
        long ddElapsedTime = 0;
        long tabElapsedTime = 0;
        long relevanceElapsedTime = 0;

        int arglen = args.size();
        String argstr = "";
        for (int i = 0; i < arglen; i++) {
            if (i > 0) argstr += ", ";
            argstr += (String) args.get(i);
        }
        System.out.println("ENTER Executor.generateDocumentsFromKB(" + argstr + ")");

        String status = "";
        String ls = System.getProperty("line.separator");
        try {            
            String kbName = null;
            String ontology = null;
            List<String> fileTypes = new ArrayList<String>();

            status += "Executor.generateDocumentsFromKB(";

            String str = "";
            for (int i = 0; i < arglen; i++) {
                str = StringUtil.removeEnclosingQuotes((String) args.get(i));
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

                if (arglen > 5) {
                    status += (ls + "Too many input arguments");
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

                /*
                  if (fileTypes.isEmpty()) {
                  status += (ls + "No file types were entered");
                  ok = false;
                  break;
                  }
                */

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

                String csvFilename = 
                    StringUtil
                    .removeEnclosingQuotes(kb.getFirstTermViaAskWithRestriction(0,
                                                                                "docGenCsvFile",
                                                                                1, 
                                                                                ontology,
                                                                                2));
                if (StringUtil.emptyString(csvFilename)) {
                    status += (ls + "No .csv file could be found for " + ontology);
                    ok = false;
                    break;
                }

                File csvInfile = new File(indir, csvFilename);
                if (!csvInfile.canRead()) {
                    status += (ls + "The file " + csvInfile.getCanonicalPath()  
                               + " does not exist, or cannot be read");
                    ok = false;
                    break;
                }
                String csvCanonicalPath = csvInfile.getCanonicalPath();
                // int csvIdx = csvCanonicalPath.lastIndexOf(".csv");
                int csvIdx = csvCanonicalPath.lastIndexOf(".dif");
                String kifFilename = ((csvIdx > 0)
                                      ? (csvCanonicalPath.substring(0, csvIdx) + ".kif")
                                      : (csvCanonicalPath + ".kif"));
                File kifFile = new File(kifFilename);
                String kifCanonicalPath = kifFile.getCanonicalPath();

                DB db = DB.getInstance(kb, ontology);
                db.setKifFilePathname(kifCanonicalPath);

                // List rows = db.readSpreadsheet(csvCanonicalPath, true);
                List rows = db.readDataInterchangeFormatFile(csvCanonicalPath);
                if (!rows.isEmpty()) {

                    db.processSpreadsheet(kb, ontology, kifCanonicalPath, rows);

                    if (kifFile.canRead() && !fileTypes.isEmpty()) {

                        kb.addConstituent(kifCanonicalPath);
                        long t2 = System.currentTimeMillis();
                        csvElapsedTime = (t2 - t1);

                        DocGen gen = DocGen.getInstance();
                        gen.setDefaultKB(kb);
                        gen.setOntology(ontology);
                        gen.setOutputParentDir(kb, ontology);

                        DisplayFilter df = new DisplayFilter() {
                                public boolean isLegalForDisplay(DocGen dg, String term) {
                                    boolean ans = StringUtil.isNonEmptyString(term);
                                    try {
                                        if (ans && (dg != null)) {
                                            KB dgkb = dg.getDefaultKB();
                                            String dgonto = dg.getOntology();
                                            if ((dgkb != null) 
                                                && StringUtil.isNonEmptyString(dgonto)) {
                                                if (dgonto.matches(".*(?i)ddex.*")
                                                    || dgonto.matches(".*(?i)ccli.*")) {
                                                    ans = 
                                                        (term.matches("^\\w+" 
                                                                      + DB.KIF_NS_DELIMITER 
                                                                      + ".+")
                                                         && !dg.getNamespaces(dgkb,
                                                                              dgonto, 
                                                                              false).contains(term)
                                                         && !DB.isLocalTermReference(term));

                                                    if (ans) {
                                                        String namespace = 
                                                            dg.getTermNamespace(dgkb, term);
                                                        List ontoNamespaces = 
                                                            dg.getOntologyNamespaces(dgkb, dgonto);
                                                        ans = ontoNamespaces.contains(namespace);

                                                    }
                                                }
                                            }
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

                        gen.setDisplayFilter(df);

                        List<String> predicates = Arrays.asList("docGenDefaultNamespace",
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
                                    gen.setDefaultNamespace(val);
                                else if (pred.equals("docGenLogoImageFile"))
                                    gen.setDefaultImageFile(val);
                                else if (pred.equals("docGenLogoImageMarkup"))
                                    gen.setDefaultImageFileMarkup(val);
                                else if (pred.equals("docGenStyleSheet"))
                                    gen.setStyleSheet(val);
                                else if (pred.equals("docGenTitleText"))
                                    gen.setTitleText(val);
                                else if (pred.equals("docGenFooterText"))
                                    gen.setFooterText(val);
                            }
                        }

                        t2 = System.currentTimeMillis();
                        gen.setTopLevelTerms(gen.computeTopLevelTermsByNamespace(kb, ontology));
                        tltElapsedTime = (System.currentTimeMillis() - t2);

                        t2 = System.currentTimeMillis();
                        for (String token : fileTypes) {
                            String lcToken = token.toLowerCase();
                            File outdir = 
                                gen.makeOutputDir(lcToken, StringUtil.getDateTime("yyyyMMdd"));
                            String outdirPath = outdir.getCanonicalPath();
                            if (token.equalsIgnoreCase("xsd")) {
                                gen.writeXsdFiles(kb, 
                                                  ontology, 
                                                  gen.getDefaultNamespace(),
                                                  outdirPath);
                                xsdElapsedTime = (System.currentTimeMillis() - t2);
                            }
                            else if (token.equalsIgnoreCase("tab")) {
                                if (gen.coreTerms.isEmpty()) {
                                    gen.computeTermRelevance(kb, ontology);
                                    relevanceElapsedTime = (System.currentTimeMillis() - t2);
                                    t2 = System.currentTimeMillis();
                                }
                                gen.generateTabularFocalTermFiles(kb, ontology, outdirPath);
                                tabElapsedTime = (System.currentTimeMillis() - t2);
                            }
                            else if (token.equalsIgnoreCase("dd")) {
                                gen.setHtmlOutputDirectoryPath(outdirPath);
                                if (gen.coreTerms.isEmpty()) {
                                    gen.computeTermRelevance(kb, ontology);
                                    relevanceElapsedTime = (System.currentTimeMillis() - t2);
                                    t2 = System.currentTimeMillis();
                                }
                                gen.generateHTML(kb, gen.getDefaultNamespace(), true);
                                ddElapsedTime = (System.currentTimeMillis() - t2);
                            }
                            t2 = System.currentTimeMillis();
                        }
                    }

                    // System.out.println("  basesToQWords == " + db.basesToQWords);

                }
                ok = false;
            }
        }
        catch (Throwable th) {
            System.out.println(th.getMessage());
            th.printStackTrace();
        }

        System.out.println("EXIT Executor.generateDocumentsFromKB(" + argstr + ")");
        System.out.println("  Total elapsed time: "
                           + ((System.currentTimeMillis() - t1) / 1000.0)
                           + " seconds");
        System.out.println("    " 
                           + (csvElapsedTime / 1000.0) 
                           + " seconds to process and load the .csv file");
        if (tltElapsedTime > 0L) {
            System.out.println("    " 
                               + (tltElapsedTime / 1000.0) 
                               + " seconds to compute top-level terms");
        }
        if (xsdElapsedTime > 0L) {
            System.out.println("    " 
                               + (xsdElapsedTime / 1000.0) 
                               + " seconds to generate XSD files");
        }
        if (ddElapsedTime > 0L) {
            System.out.println("    " 
                               + (relevanceElapsedTime / 1000.0) 
                               + " seconds to compute term relevance");
            System.out.println("    " 
                               + (ddElapsedTime / 1000.0) 
                               + " seconds to generate Data Dictionary files");
        }
        if (tabElapsedTime > 0L) {
            System.out.println("    " 
                               + (tabElapsedTime / 1000.0) 
                               + " seconds to generate tabular display files");
        }
        return status;
    }

    // basicTypes
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
    public static void cfFilesToKifHierarchyFile(String indirpath, String outdirpath) {
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
                                                       "Summary", 
                                                       "SOW_Template", 
                                                       "OntX_log");
                List<File> files = Arrays.asList(indir.listFiles());
                Collections.sort(files);
                DB db = DB.getInstance();

                // Read and process CF files.
                int cfsDone = 0;
                for (File f : files) {
                    if (!f.isDirectory()) {
                        String filename = f.getName();
                        int eidx = filename.indexOf(".dif");
                        String base = ((eidx > 0)
                                       ? filename.substring(0, eidx)
                                       : filename);
                        if (!skipFiles.contains(base)) {
                            String cp = f.getCanonicalPath();
                            List tblarr = db.readDataInterchangeFormatFile(cp);

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
                                normalizeCfTable(db, tblarr);
                                statements.add(((cfsDone > 0)
                                                ? db.getLineSeparator()
                                                : "") + ";; " + base);
                                localStatements.clear();
                                processCfTable(db, 
                                               tblarr, 
                                               hier, 
                                               compositeRelators, 
                                               localStatements);
                                statements.addAll(localStatements);
                                cfsDone++;
                            }
                        }
                    }
                }

                System.out.println("");
                System.out.println(compositeRelators.size() + " composite relators created");
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
                                            statements.add(db.getLineSeparator() 
                                                           + ";; coa:IsSubRelatorOf");
                                        }
                                        statements.add(DB.makeStatement("coa:IsSubRelatorOf",
                                                                        fstr,
                                                                        fstr2));
                                        subRelCount++;
                                    }
                                }
                            }
                        }
                    }
                }

                System.out.println(subRelCount + " coa:IsSubRelatorOf statements added");
                System.out.println("");

                // Write SUO-KIF statements.
                outdir = new File(outdirpath);
                if (outdir.canWrite()) {
                    File outfile = new File(outdir, "ContextFamilies.kif");
                    pw = new PrintWriter(new FileWriter(outfile));
                    db.printSuoKifStatements(statements, pw);
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
        return;
    }

    /** *************************************************************
     * Processes the List of Lists tblarr to produce Strings denoting
     * KIF statements, which are added to the Set statements.
     *
     * @param db A DB utility object used to process tblarr
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
    private static void processCfTable(DB db, 
                                       List<List> tblarr, 
                                       Map hier,
                                       Set<String> compositeRelators,
                                       Set<String> statements) {
        try {
            Set<String> zeroCardinality = new HashSet<String>();
            Map<String, String> cardinalities = new HashMap<String, String>();
            Map<String, String> relatorComponents = new HashMap<String, String>();
            String nsToken = "coa";
            String namespace = ("ns" + DB.getW3cNamespaceDelimiter() + nsToken);
            String nsPrefix = (nsToken + DB.getW3cNamespaceDelimiter());
            db.buildTableColumnNames(null, null);
            List commentCols = Arrays.asList("I", "J", "K", "L", "M", "N", "O", "P", "Q", "R");
            List adjCols = Arrays.asList("S", "T", "U");
            List adjNames = Arrays.asList("HistoricAdjective", 
                                          "CurrentAdjective", 
                                          "PotentialAdjective");
            List propCols = Arrays.asList("V", "W", "X");
            List propNames = Arrays.asList("HistoricProperty", 
                                           "CurrentProperty", 
                                           "PotentialProperty");
            List<String> synonyms = new ArrayList<String>();
            String family = null;
            String contextType = null;
            List<String> utilList = new ArrayList<String>();  // an empty List
            for (List row : tblarr) {
                int bti = -1;
                String basicType = (String) row.get(db.getColumnIndex("A"));
                if (StringUtil.isNonEmptyString(basicType)) {
                    basicType = basicType.trim();
                    if (basicType.equals("$$$")) {
                        break;
                    }
                    String rowFocalTerm = null;
                    String familyMembers = (String) row.get(db.getColumnIndex("B"));
                    List<String> fmList = (StringUtil.isNonEmptyString(familyMembers)
                                           ? Arrays.asList(familyMembers.split("\\|"))
                                           : utilList);
                    String parents = (String) row.get(db.getColumnIndex("C"));
                    List<String> parentList = (StringUtil.isNonEmptyString(parents)
                                               ? Arrays.asList(parents.split("\\|"))
                                               : utilList);
                    String cardinality = (String) row.get(db.getColumnIndex("E"));
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
                    String definition = (String) row.get(db.getColumnIndex("F"));

                    // if (!parentList.isEmpty()) System.out.println("  parentList == " + parentList);

                    if (basicType.equals("Family")) {
                        bti = CF_FAMILY;
                        family = familyMembers;
                        rowFocalTerm = family;
                        if (StringUtil.isNonEmptyString(family)) {
                            statements.add(DB.makeStatement("coa:IsA", 
                                                            family, 
                                                            "coa:ContextFamily"));
                        }
                    }
                    else if (!fmList.isEmpty()) {

                        // System.out.println("  fmList == " + fmList);

                        int fmlen = fmList.size();
                        int i = 0;
                        if (basicType.equals("Verb")) {
                            for (String term : fmList) {
                                int idx = term.indexOf(DB.getW3cNamespaceDelimiter());
                                String newTerm = null;
                                if ((idx > 0) && (idx < term.length())) {
                                    newTerm = term.substring(idx + 1);
                                }
                                synonyms.add(newTerm);
                            }
                            for (String term : parentList) {
                                String parentFamily = (term + "_CF");
                                statements.add(DB.makeStatement("coa:HasLogicalParent",
                                                                family,
                                                                parentFamily));
                            }
                        }
                        else if (basicType.equals("ContextType")) {
                            bti = CF_CONTEXT;
                            for (String term : fmList) {
                                int idx = term.indexOf(DB.getW3cNamespaceDelimiter());
                                String newTerm = null;
                                if ((idx > 0) && (idx < term.length())) {
                                    newTerm = term.substring(idx + 1);
                                }
                                if (i == 0) {
                                    newTerm = (nsPrefix + newTerm);
                                    contextType = newTerm;
                                    rowFocalTerm = contextType;
                                    relatorComponents.put(DB.w3cToKif(rowFocalTerm),
                                                          twoOrMoreP);
                                }
                                else {
                                    statements.add(DB.makeStatement("synonym",
                                                                    namespace,
                                                                    contextType,
                                                                    DB.quote(newTerm)));
                                }
                                i++;
                            }
                            for (String syn : synonyms) {
                                statements.add(DB.makeStatement("synonym",
                                                                namespace,
                                                                contextType,
                                                                DB.quote(syn)));
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
                            for (String term : fmList) {
                                int idx = term.indexOf(DB.getW3cNamespaceDelimiter());
                                String newTerm = null;
                                if ((idx > 0) && (idx < term.length())) {
                                    newTerm = term.substring(idx + 1);
                                }
                                if (i == 0) {
                                    newTerm = (nsPrefix + newTerm);
                                    rowFocalTerm = newTerm;
                                    if (bti == CF_RESOURCE) {
                                        relatorComponents.put(DB.w3cToKif(rowFocalTerm),
                                                              twoOrMoreP);
                                    }
                                }
                                else {
                                    statements.add(DB.makeStatement("synonym",
                                                                    namespace,
                                                                    rowFocalTerm,
                                                                    DB.quote(newTerm)));
                                }
                                i++;
                            }
                        }
                        if (StringUtil.isNonEmptyString(rowFocalTerm)) {
                            if (StringUtil.isNonEmptyString(family)) {
                                statements.add(DB.makeStatement("coa:HasCfMember",
                                                                family,
                                                                rowFocalTerm));
                            }
                            if (!parentList.isEmpty()) {
                                for (String p : parentList) {
                                    statements.add(DB.makeStatement("coa:IsSubClassOf",
                                                                    rowFocalTerm,
                                                                    p));
                                }
                            }
                        }
                        if ((bti == CF_CONTEXT) || (bti == CF_RESOURCE)) {
                            if (StringUtil.isNonEmptyString(rowFocalTerm)) {
                                String kifTerm = DB.w3cToKif(rowFocalTerm);
                                if (!parentList.isEmpty()) {
                                    List plist = (List) hier.get(kifTerm);
                                    if (plist == null) {
                                        plist = new LinkedList();
                                        hier.put(kifTerm, plist);
                                    }
                                    for (String s : parentList) {
                                        String kifParent = DB.w3cToKif(s);
                                        if (!plist.contains(kifParent)) {
                                            plist.add(kifParent);
                                        }
                                    }
                                }

                                // System.out.println("  cardinality == " + cardinality);

                                if (StringUtil.isNonEmptyString(cardinality)) {
                                    addCfCardinalityValues(db,
                                                           kifTerm, 
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

                        String relatorStr = (db.stripNamespace(c1)
                                             + DOT_OP
                                             + db.stripNamespace(c2));

                        if (!domainCard.equals("0") && !rangeCard.equals("0")) {
                            compositeRelators.add(pred);
                            statements.add(DB.makeStatement("termFormat",
                                                            namespace,
                                                            pred,
                                                            DB.quote(relatorStr)));
                            statements.add(DB.makeStatement("coa:HasCfMember",
                                                            family,
                                                            pred));
                            statements.add(DB.makeStatement("coa:IsA", pred, "coa:Relator"));
                            statements.add(DB.makeStatement("coa:HasDomain", pred, c1));
                            statements.add(DB.makeStatement("coa:HasRange", pred, c2));
                            String inv = ("(PredicateFn " + c2 + " " + c1 + ")");
                            statements.add(DB.makeStatement("coa:IsReciprocalOf", inv, pred));
                        }

                        String domainConstraint = ("coarule:" + relatorStr + "_DomainCardinality");
                        String rangeConstraint = ("coarule:" + relatorStr + "_RangeCardinality");
                        statements.add(DB.makeStatement("coa_itd:HasRuleType",
                                                        domainConstraint,
                                                        "coa:RelatorCardinalityConstraint"));
                        statements.add(DB.makeStatement("coa:ConstrainsDomain",
                                                        domainConstraint,
                                                        c1));
                        statements.add(DB.makeStatement("coa:ConstrainsRelator",
                                                        domainConstraint,
                                                        pred));
                        makeCfConstraintCardinalityStatements(domainConstraint, 
                                                              domainCard,
                                                              statements);
                        statements.add(DB.makeStatement("coa_itd:HasRuleType",
                                                        rangeConstraint,
                                                        "coa:RelatorCardinalityConstraint"));
                        statements.add(DB.makeStatement("coa:ConstrainsRange",
                                                        rangeConstraint,
                                                        c2));
                        statements.add(DB.makeStatement("coa:ConstrainsRelator",
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
                    statements.add(DB.makeStatement(cArg0, constraint, cArg2));
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
    private static void addCfCardinalityValues(DB db,
                                               String countable, 
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
                    zeroCardinality.add(db.stripNamespace(countable));
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
     * @param db A DB utility object used to process tblarr
     *
     * @param tblarr A List of Lists representing a spreadsheet or
     * database table
     *
     * @return void
     */
    private static void normalizeCfTable(DB db, List<List> tblarr) {
        try {
            db.buildTableColumnNames(null, null);
            List constantKeys = Arrays.asList("B", "C", "S", "T", "U", "V", "W", "X");
            boolean isNormalizing = false;
            String nsToken = "coa";
            String nsPrefix = (nsToken + DB.getW3cNamespaceDelimiter());
            String termDelim = "|";
            String nameDelim = "!";
            StringBuilder sb = new StringBuilder();
            String cell = null;
            String newCell = null;
            String key = null;
            for (List row : tblarr) {
                int rowlen = row.size();
                for (int i = 0; i < rowlen; i++) {
                    key = db.getColumnKey(i);
                    cell = (String) row.get(i);
                    if (StringUtil.isNonEmptyString(cell)) {

                        // System.out.println("  cell == " + cell);

                        newCell = StringUtil.removeEnclosingQuotes(cell.trim());

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
                                    || newCell.matches(DB.NAMES_FIELD_PATTERN)
                                    ) {
                                    sb.setLength(0);
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

}