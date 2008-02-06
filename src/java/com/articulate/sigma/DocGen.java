/* This code is copyrighted by Articulate Software (c) 2007.
It is released under the GNU Public License <http://www.gnu.org/copyleft/gpl.html>.
Users of this code also consent, by use of this code, to credit Articulate Software in any
writings, briefings, publications, presentations, or other representations of any
software which incorporates, builds on, or uses this code.
Please cite the following article in any publication with references:

Pease, A., (2003). The Sigma Ontology Development Environment, 
in Working Notes of the IJCAI-2003 Workshop on Ontology and Distributed Systems,
August 9, Acapulco, Mexico.  See also http://sigmakee.sourceforge.net
*/
package com.articulate.sigma;
import java.util.*;
import java.io.*;

/* A class to generate a simplified HTML-based documentation for SUO-KIF terms. */

public class DocGen {     

    public static String header = "SUMO v. 75, June 2007";
    public static String footer = "<table width='100%'><tr class='title'><td>" +
        "(c) IEEE free release, see www.ontologyportal.org</td></tr></table>\n";

    /** ***************************************************************
     */
    public static void setHeader(String h) {
        header = h;
    }

    /** ***************************************************************
     */
    public static void setFooter(String f) {
        footer = f;
    }

    /** ***************************************************************
     *  Collect relations in the knowledge base 
     *
     *  @return The set of relations in the knowledge base.
     */
    private static ArrayList getRelations(KB kb) {

        ArrayList relations = new ArrayList();
        Iterator it = kb.terms.iterator();
        while (it.hasNext()) {
            String term = (String) it.next();
            if (kb.childOf(term,"BinaryPredicate"))
                relations.add(term.intern());            
        }
        return relations;
    }      

    /** ***************************************************************
     */
    private static String generateTOCHeader(TreeMap alphaList, TreeMap pageList) {

        StringBuffer result = new StringBuffer();
        result.append("<head><META http-equiv=\"Content-Type\" content=\"text/html; charset=utf-8\">" +
                   "<link rel=\"stylesheet\" type=\"text/css\" href=\"simple.css\"></head><body>\n");
        result.append("<table width=\"100%\"><tr><td colspan=\"35\" class=\"title\">\n");
        result.append(header + "</td></tr><tr class=\"letter\">\n");

        for (char c = 65; c < 90; c++) {
            String cString = Character.toString(c);
            if (alphaList.keySet().contains(cString)) {
                String filelink = "letter-" + cString + ".html";      
                result.append("<td><a href=\"" + filelink + "\">" + cString + "</a></td>\n");
            }
            else                 
                result.append("<td>" + cString + "</td>\n");
        }
        for (char c = 97; c < 122; c++) {
            String cString = Character.toString(c);
            if (alphaList.keySet().contains(cString)) {
                String filelink = "letter-" + cString + ".html";      
                result.append("<td><a href=\"" + filelink + "\">" + cString + "</a></td>\n");
            }
            else                 
                result.append("<td>" + cString + "</td>\n");
        }
        result.append("</td></tr></table>");
        return result.toString();
    }

    /** ***************************************************************
     */
    private static String generateTOCPage(KB kb, String filename, String header, TreeSet terms) {

        StringBuffer result = new StringBuffer();
        result.append("<table width=\"100%\">");
        Iterator it = terms.iterator();
        while (it.hasNext()) {
            String term = (String) it.next();
            result.append("<tr><td><a href=\"" + term + ".html\">" + term + "</a></td>\n");
            ArrayList docs = kb.askWithRestriction(0,"documentation",1,term);
            if (docs != null && docs.size() > 0) {
                Formula f = (Formula) docs.get(0);
                String docString = f.getArgument(3);  
                docString = kb.formatDocumentation("",docString);
                if (docString.length() > 1)                 
                    result.append("<td>" + docString.substring(1,docString.length()) + "</td>\n");
            }
            result.append("</tr>\n");
        }
        result.append("</tr>\n");
        result.append("</table>\n");
        return result.toString();
    }

    /** ***************************************************************
     */
    private static String createDocs(KB kb, String kbHref, String term) {

        StringBuffer result = new StringBuffer();
        ArrayList docs = kb.askWithRestriction(0,"documentation",1,term);
        if (docs != null && docs.size() > 0) {
            Formula f = (Formula) docs.get(0);
            String docString = f.getArgument(3);  
            docString = kb.formatDocumentation(kbHref,docString);
            result.append("<td class=\"description\">" + docString + "</td></tr>\n");
        }
        return result.toString();
    }

    /** ***************************************************************
     */
    private static String createSynonyms(KB kb, String kbHref, String term) {

        StringBuffer result = new StringBuffer();
        ArrayList syn = kb.askWithRestriction(0,"synonymousExternalConcept",2,term);
        if (syn != null && syn.size() > 0) {
            result.append("<tr><td><b>Synonym(s)</b>");
            for (int i = 0; i < syn.size(); i++) {
                Formula f = (Formula) syn.get(i);
                String s = f.getArgument(1); 
                if (i >0) result.append(", ");                
                result.append("<i>" + s + "</i>");
            }
            result.append("</td></tr>\n");
        }
        return result.toString();
    }

    /** ***************************************************************
     */
    private static String createParents(KB kb, String kbHref, String term) {

        StringBuffer result = new StringBuffer();
        ArrayList forms = kb.askWithRestriction(0,"subclass",1,term);
        if (forms != null && forms.size() > 0) {
            result.append("<tr><td class=\"label\">Parents</td>");
            for (int i = 0; i < forms.size(); i++) {
                Formula f = (Formula) forms.get(i);
                if (!f.sourceFile.endsWith(KB._cacheFileSuffix)) {
                    String s = f.getArgument(2); 
                    String termHref = "<a href=\"" + kbHref + "&term=" + s + "\">" + s + "</a>";
                    if (i > 0) result.append("<tr><td>&nbsp;</td>");                
                    result.append("<td class=\"cell\">" + termHref + "</td>");
                    ArrayList docs = kb.askWithRestriction(0,"documentation",1,s);
                    if (docs != null && docs.size() > 0) {
                        f = (Formula) docs.get(0);
                        String docString = f.getArgument(3);  
                        docString = kb.formatDocumentation(kbHref,docString);
                        result.append("<td class=\"cell\">" + docString + "</td>");
                    }
                    result.append("</tr>\n");
                }
            }
        }
        return result.toString();
    }

    /** ***************************************************************
     */
    private static String createChildren(KB kb, String kbHref, String term) {

        StringBuffer result = new StringBuffer();
        ArrayList forms = kb.askWithRestriction(0,"subclass",2,term);
        if (forms != null && forms.size() > 0) {
            result.append("<tr><td class=\"label\">Children</td>");
            for (int i = 0; i < forms.size(); i++) {
                Formula f = (Formula) forms.get(i);
                if (!f.sourceFile.endsWith(KB._cacheFileSuffix)) {
                    String s = f.getArgument(1); 
                    String termHref = "<a href=\"" + kbHref + "&term=" + s + "\">" + s + "</a>";
                    if (i > 0) result.append("<tr><td>&nbsp;</td>");                
                    result.append("<td class=\"cell\">" + termHref + "</td>");
                    ArrayList docs = kb.askWithRestriction(0,"documentation",1,s);
                    if (docs != null && docs.size() > 0) {
                        f = (Formula) docs.get(0);
                        String docString = f.getArgument(3);  
                        docString = kb.formatDocumentation(kbHref,docString);
                        result.append("<td class=\"cell\">" + docString + "</td>");
                    }
                    result.append("</tr>\n");
                }
            }
        }
        return result.toString();
    }

    /** ***************************************************************
     */
    private static String createInstances(KB kb, String kbHref, String term) {

        StringBuffer result = new StringBuffer();
        ArrayList forms = kb.askWithRestriction(0,"instance",2,term);
        if (forms != null && forms.size() > 0) {
            result.append("<tr><td class=\"label\">Child instances</td>");
            for (int i = 0; i < forms.size(); i++) {
                Formula f = (Formula) forms.get(i);
                if (!f.sourceFile.endsWith(KB._cacheFileSuffix)) {
                    String s = f.getArgument(1); 
                    String termHref = "<a href=\"" + kbHref + "&term=" + s + "\">" + s + "</a>";
                    if (i > 0) result.append("<tr><td>&nbsp;</td>");                
                    result.append("<td class=\"cell\">" + termHref + "</td>");
                    ArrayList docs = kb.askWithRestriction(0,"documentation",1,s);
                    if (docs != null && docs.size() > 0) {
                        f = (Formula) docs.get(0);
                        String docString = f.getArgument(3);  
                        docString = kb.formatDocumentation(kbHref,docString);
                        result.append("<td class=\"cell\">" + docString + "</td>");
                    }
                    result.append("</tr>\n");
                }
            }
        }
        return result.toString();
    }

    /** ***************************************************************
     */
    private static String createRelations(KB kb, String kbHref, String term) {

        StringBuffer result = new StringBuffer();
        ArrayList relations = getRelations(kb);
        boolean firstLine = true;
        for (int i = 0; i < relations.size(); i++) {
            String relation = (String) relations.get(i);
            // System.out.println("INFO in DocGen.createRElations(): relation: " + relation);
            if (!relation.equals("subclass") && !relation.equals("instance") &&
                !relation.equals("documentation")) {
                String relnHref = "<a href=\"" + kbHref + "&term=" + relation + "\">" + relation + "</a>";
                ArrayList statements = kb.askWithRestriction(0,relation,1,term);
                for (int j = 0; j < statements.size(); j++) {
                    Formula f = (Formula) statements.get(j);
                    if (!f.sourceFile.endsWith(KB._cacheFileSuffix)) {
                        String s = f.getArgument(2); 
                        String termHref = "<a href=\"" + kbHref + "&term=" + s + "\">" + s + "</a>";
                        if (firstLine) {
                            result.append("<tr><td class=\"label\">Relations</td>");                
                            firstLine = false;
                        }
                        else {
                            result.append("<tr><td>&nbsp;</td>");                
                        }
                        result.append("<td class=\"cell\">" + relnHref + "</td>");
                        result.append("<td class=\"cell\">" + termHref + "</td></tr>\n");
                    }
                }                
            }            
        }
        return result.toString();
    }

    /** ***************************************************************
     * Create an HTML page that lists information about a particular term,
     * with a limit on how many statements of each type should be
     * displayed.
     */
    public static String createPage(KB kb, String kbHref, String term, int limit, String language) {

        StringBuffer result = new StringBuffer();
        result.append("<table width=\"100%\">");
        result.append("<tr id=\"" + term + "\">");
        result.append("<td class=\"headword\">" + term + "</td></tr>\n<tr>");

        result.append(DocGen.createDocs(kb,kbHref,term));
        result.append(DocGen.createSynonyms(kb,kbHref,term));
        result.append("</table><P>\n");
        result.append("<table width=\"100%\">");
        result.append(DocGen.createParents(kb,kbHref,term));
        result.append(DocGen.createChildren(kb,kbHref,term));
        result.append(DocGen.createInstances(kb,kbHref,term));
        result.append(DocGen.createRelations(kb,kbHref,term));
        result.append("</table>\n");

        result.append(HTMLformatter.htmlDivider);
        result.append("<P><table><tr><td><b>Other statements</b></td></tr>");
        result.append("<tr><td class=\"cell\">These statements express (potentially complex) facts about the term, " +
                      "and are automatically generated.</td></tr>\n<tr><td class=\"cell\">");

        int localLimit = limit;
        String limitString = "";
        for (int argnum = 2; argnum < 6; argnum++) {
            localLimit = limit;
            limitString = "";
            ArrayList forms = kb.ask("arg",argnum,term);
            if (forms != null) {
                if (forms.size() < localLimit) 
                    localLimit = forms.size();
                else
                    limitString = "<br>Display limited to " + (new Integer(localLimit)).toString() + " statements of each type.<P>\n";
                for (int i = 0; i < localLimit; i++) {
                    Formula form = (Formula) forms.get(i);
                    result.append(LanguageFormatter.htmlParaphrase(kbHref,form.theFormula, kb.getFormatMap(language), 
                                   kb.getTermFormatMap(language), kb,language) + "<br>\n");
                }
            }
            result.append(limitString);
        }

        localLimit = limit;
        limitString = "";
        ArrayList forms = kb.ask("ant",0,term);
        if (forms != null) {
            if (forms.size() < localLimit) 
                localLimit = forms.size();
            else
                limitString = "<br>Display limited to " + (new Integer(localLimit)).toString() + " statements of each type.<P>\n";
            for (int i = 0; i < localLimit; i++) {
                Formula form = (Formula) forms.get(i);
                result.append(LanguageFormatter.htmlParaphrase(kbHref,form.theFormula, kb.getFormatMap(language), 
                               kb.getTermFormatMap(language), kb,language) + "\n");
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
                limitString = "<br>Display limited to " + (new Integer(localLimit)).toString() + " statements of each type.<P>\n";
            for (int i = 0; i < localLimit; i++) {
                Formula form = (Formula) forms.get(i);
                result.append(LanguageFormatter.htmlParaphrase(kbHref,form.theFormula, kb.getFormatMap(language), 
                               kb.getTermFormatMap(language), kb,language) + "\n");
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
                limitString = "<br>Display limited to " + (new Integer(localLimit)).toString() + " statements of each type.<P>\n";
            for (int i = 0; i < localLimit; i++) {
                Formula form = (Formula) forms.get(i);
                result.append(LanguageFormatter.htmlParaphrase(kbHref,form.theFormula, kb.getFormatMap(language), 
                               kb.getTermFormatMap(language), kb,language) + "<br>\n");
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
                limitString = "<br>Display limited to " + (new Integer(localLimit)).toString() + " statements of each type.<P>\n";
            for (int i = 0; i < localLimit; i++) {
                Formula form = (Formula) forms.get(i);
                result.append(LanguageFormatter.htmlParaphrase(kbHref,form.theFormula, kb.getFormatMap(language), 
                               kb.getTermFormatMap(language), kb,language) + "<br>\n");
            }
        }
        result.append(limitString);
        result.append("</td></tr></table><P>");
        return result.toString();
    }

    /** ***************************************************************
     *  Generate and save all the index pages that link to the
     *  individual term pages.
     *  @param alphaList is a map of all the terms keyed by their
     *                   first letter
     *  @pageList is a map of all term pages keyed by term name
     *  @dir is the directory in which to save the pages
     */
    private static String saveIndexes(KB kb, TreeMap alphaList, TreeMap pageList, String dir) throws IOException {

        String tocheader = generateTOCHeader(alphaList,pageList);
        FileWriter fw = null;
        PrintWriter pw = null; 
        String filename = "";
        try {
            Iterator it = alphaList.keySet().iterator();
            while (it.hasNext()) {
                String letter = (String) it.next();
                TreeSet terms = (TreeSet) alphaList.get(letter);
                filename = dir + File.separator + "letter-" + letter + ".html";
                fw = new FileWriter(filename);
                pw = new PrintWriter(fw);
                String page = generateTOCPage(kb,filename,header,terms);
                pw.println(tocheader);
                pw.println(page);
                pw.println(footer); 
                pw.close();
                fw.close();
            }   
            fw = new FileWriter(dir + File.separator + "index.html");
            pw = new PrintWriter(fw);
            pw.println(tocheader);
            pw.println(footer);
            pw.close();
            fw.close();
        }
        catch (java.io.IOException e) {
            throw new IOException("Error writing file " + filename + "\n" + e.getMessage());
        }
        finally {
            if (pw != null) {
                pw.close();
            }
            if (fw != null) {
                fw.close();
            }
        }

        return tocheader;
    }

    /** ***************************************************************
     *  Save pages below the KBs directory in a directory called
     *  HTML.  If that already exists, use HTML1, HTML2 etc.
     */
    private static void printPages(TreeMap pageList, String tocheader, String dir) throws IOException {

        FileWriter fw = null;
        PrintWriter pw = null; 
        Iterator it = pageList.keySet().iterator();
        while (it.hasNext()) {
            String term = (String) it.next();
            String page = (String) pageList.get(term);
            String filename = dir + File.separator + term + ".html";
            System.out.println("Info in DocGen.printPages(): filename : " + filename);
            try {
                fw = new FileWriter(filename);
                pw = new PrintWriter(fw);
                pw.println(tocheader);
                pw.println(page);
                pw.println(footer);
            }
            catch (java.io.IOException e) {
                throw new IOException("Error writing file " + filename + "\n" + e.getMessage());
            }
            finally {
                if (pw != null) {
                    pw.close();
                }
                if (fw != null) {
                    fw.close();
                }
            }
        }   
    }

    /** ***************************************************************
     *  Save pages below the SIGMA_HOME directory in a directory
     *  called HTML.  If that already exists, use HTML1, HTML2 etc.
     */
    private static String generateDir() throws IOException {

        String dir = KBmanager.getMgr().getPref("baseDir");
        String subdir = "HTML";
        int counter = 0;
        String path = dir + File.separator + subdir;
        File f = new File(path);
        while (f.exists()) {
            counter++;
            path = dir + File.separator + subdir + Integer.toString(counter);
            f = new File(path);
        }
        f.mkdir();
        return path;
    }

    /** ***************************************************************
     * Generate simplified HTML pages for all terms.
     * alphaList is a TreeMap of TreeSets.  The map key is single letters.
     * Each TreeSet is a set of all the terms that have that initial letter.
     * pageList is a TreeMap of Strings where the key is the term name and
     * the String is the body of the page describing the term.  It is 
     * combined with a table of contents header created in saveIndexes().
     */
    public static void generateHTML(KB kb, String language) {

        try {
            TreeSet firstCharSet = new TreeSet();
            TreeMap alphaList = new TreeMap();
            TreeMap pageList = new TreeMap();
            Iterator it = kb.terms.iterator();
            while (it.hasNext()) {
                String term = (String) it.next();
                String firstChar = term.substring(0,1);
                if (Character.isLetter(firstChar.charAt(0))) {
                    firstCharSet.add(firstChar);
                    TreeSet list = new TreeSet();
                    if (alphaList.get(firstChar) != null) 
                        list = (TreeSet) alphaList.get(firstChar); 
                    else
                        alphaList.put(firstChar,list);
                    list.add(term);
                    pageList.put(term,createPage(kb,"",term,200,language));
                }
            }
            String dir = generateDir();
            String tocheader = saveIndexes(kb,alphaList,pageList,dir);
            printPages(pageList,tocheader,dir);
        }
        catch (java.io.IOException e) {
            System.out.println(e.getMessage());
        }
    }      

    /** ***************************************************************
     * Generate a single HTML page showing all terms.
     */
    public static void generateSingleHTML(KB kb, String language) throws IOException {

        FileWriter fw = null;
        PrintWriter pw = null; 
        try {
            fw = new FileWriter(KBmanager.getMgr().getPref("baseDir") + File.separator + kb.name+ "-AllTerms.html");
            pw = new PrintWriter(fw);
            pw.println("<head><META http-equiv=\"Content-Type\" content=\"text/html; charset=utf-8\">" +
                       "<link rel=\"stylesheet\" type=\"text/css\" href=\"simple.css\"></head><body>\n");
            pw.println(DocGen.header);
            pw.println("<table border=0><tr bgcolor=#CCCCCC><td>Name</td><td>Documentation</td></tr>\n");
            boolean even = true;
            Iterator it = kb.terms.iterator();
            while (it.hasNext()) {
                String term = (String) it.next();
                if (even)
                    pw.println("<tr><td>");
                else
                    pw.println("<tr bgcolor=#CCCCCC><td>");
                even = !even;
                pw.println(term);
                pw.println("</td>");
                ArrayList docs = kb.askWithRestriction(0,"documentation",1,term);
                if (docs != null && docs.size() > 0) {
                    Formula f = (Formula) docs.get(0);
                    String docString = f.getArgument(3); 
                    if (docString != null && docString != "" && docString.length() > 100) 
                        docString = docString.substring(0,100) + "...\"";                    
                    pw.println("<td class=\"description\">" + docString);
                }
                else
                    pw.println("<td>");
                pw.println("</td></tr>\n");
            }
            pw.println("</table>\n");
            pw.println(DocGen.footer);
        }
        catch (java.io.IOException e) {
            System.out.println(e.getMessage());
        }
        finally {
            if (pw != null) {
                pw.close();
            }
            if (fw != null) {
                fw.close();
            }
        }
    }      

    /** *************************************************************
     * A test method.
     */
    public static void main (String args[]) {

        try {
            KBmanager.getMgr().initializeOnce();
        } catch (IOException ioe ) {
            System.out.println(ioe.getMessage());
        }
        KB kb = KBmanager.getMgr().getKB("SUMO");
        // DocGen.generateHTML(kb,"EnglishLanguage");
    }
}
