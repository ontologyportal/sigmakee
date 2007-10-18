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
    public static String footer = "<table width=\"100%\"><tr class=\"title\"><td>" +
        "(c) IEEE free release, see www.ontologyportal.org</td></tr></table>\n";

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
                   "<link rel=\"stylesheet\" type=\"text/css\" href=\"coa.css\"></head><body>\n");
        result.append("<table width=\"100%\"><tr><td colspan=\"35\" class=\"title\">\n");
        result.append(header + "</td></tr><tr class=\"letter\">\n");

        Iterator it = alphaList.keySet().iterator();
        while (it.hasNext()) {
            String letter2 = (String) it.next();
            String filelink = "letter-" + letter2 + ".html";      
            result.append("<td><a href=\"" + filelink + "\">" + letter2 + "</a></td>\n");
        }
        result.append("</td></tr></table>");
        return result.toString();
    }

    /** ***************************************************************
     */
    private static String generateTOCPage(KB kb, String filename, String header, TreeSet terms) {

        StringBuffer result = new StringBuffer();
        Iterator it = terms.iterator();
        result.append("<table width=\"100%\">");
        while (it.hasNext()) {
            String term = (String) it.next();
            result.append("<tr><td><a href=\"" + term + ".html\">" + term + "</a></td>\n");
            ArrayList docs = kb.askWithRestriction(0,"documentation",1,term);
            if (docs != null && docs.size() > 0) {
                Formula f = (Formula) docs.get(0);
                String docString = f.getArgument(2);  // Note this will become 3 if we add language to documentation
                docString = kb.formatDocumentation("",docString);
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
            String docString = f.getArgument(2);  // Note this will become 3 if we add language to documentation
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
                if (!f.sourceFile.endsWith("_Cache.kif")) {
                    String s = f.getArgument(2); 
                    if (i > 0) result.append("<tr><td>&nbsp;</td>");                
                    result.append("<td class=\"cell\">" + s + "</td>");
                    ArrayList docs = kb.askWithRestriction(0,"documentation",1,s);
                    if (docs != null && docs.size() > 0) {
                        f = (Formula) docs.get(0);
                        String docString = f.getArgument(2);  // Note this will become 3 if we add language to documentation
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
                if (!f.sourceFile.endsWith("_Cache.kif")) {
                    String s = f.getArgument(1); 
                    if (i > 0) result.append("<tr><td>&nbsp;</td>");                
                    result.append("<td class=\"cell\">" + s + "</td>");
                    ArrayList docs = kb.askWithRestriction(0,"documentation",1,s);
                    if (docs != null && docs.size() > 0) {
                        f = (Formula) docs.get(0);
                        String docString = f.getArgument(2);  // Note this will become 3 if we add language to documentation
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
            System.out.println("INFO in DocGen.createRElations(): relation: " + relation);
            if (!relation.equals("subclass") && !relation.equals("instance") &&
                !relation.equals("documentation")) {
                ArrayList statements = kb.askWithRestriction(0,relation,1,term);
                for (int j = 0; j < statements.size(); j++) {
                    Formula f = (Formula) statements.get(j);
                    if (!f.sourceFile.endsWith("_Cache.kif")) {
                        String s = f.getArgument(2); 
                        if (firstLine) {
                            result.append("<tr><td class=\"label\">Relations</td>");                
                            firstLine = false;
                        }
                        else {
                            result.append("<tr><td>&nbsp;</td>");                
                        }
                        result.append("<td class=\"cell\">" + relation + "</td>");
                        result.append("<td class=\"cell\">" + s + "</td></tr>\n");
                    }
                }                
            }            
        }
        return result.toString();
    }

    /** ***************************************************************
     */
    public static String createPage(KB kb, String kbHref, String term) {

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
        result.append(DocGen.createRelations(kb,kbHref,term));
        result.append("</table>\n");

        return result.toString();
    }

    /** ***************************************************************
     */
    private static String saveIndexes(KB kb, TreeMap alphaList, TreeMap pageList) throws IOException {

        String tocheader = generateTOCHeader(alphaList,pageList);
        FileWriter fw = null;
        PrintWriter pw = null; 
        Iterator it = alphaList.keySet().iterator();
        while (it.hasNext()) {
            String letter = (String) it.next();
            TreeSet terms = (TreeSet) alphaList.get(letter);
            String filename = "letter-" + letter + ".html";
            try {
                fw = new FileWriter(filename);
                pw = new PrintWriter(fw);
                String page = generateTOCPage(kb,filename,header,terms);
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
        return tocheader;
    }

    /** ***************************************************************
     */
    private static void printPages(TreeMap pageList, String tocheader) throws IOException {

        FileWriter fw = null;
        PrintWriter pw = null; 
        Iterator it = pageList.keySet().iterator();
        while (it.hasNext()) {
            String term = (String) it.next();
            String page = (String) pageList.get(term);
            String filename = term + ".html";
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
     * Generate simplified HTML pages for all terms.
     * alphaList is a TreeMap of TreeSets.  The map key is single letters.
     * Each TreeSet is a set of all the terms that have that initial letter.
     * pageList is a TreeMap of Strings where the key is the term name and
     * the String is the body of the page describing the term.  It is 
     * combined with a table of contents header created in saveIndexes().
     */
    public static void generateHTML(KB kb) {

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
                    pageList.put(term,createPage(kb,"",term));
                }
            }
            String tocheader = saveIndexes(kb,alphaList,pageList);
            printPages(pageList,tocheader);
        }
        catch (java.io.IOException e) {
            System.out.println(e.getMessage());
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
        DocGen.generateHTML(kb);
    }
}
