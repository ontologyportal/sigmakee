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