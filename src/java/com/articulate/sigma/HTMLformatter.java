package com.articulate.sigma;

/* This code is copyright Articulate Software (c) 2003-2011.  Some portions
copyright Teknowledge (c) 2003 and reused under the terms of the GNU license.
This software is released under the GNU Public License <http://www.gnu.org/copyleft/gpl.html>.
Users of this code also consent, by use of this code, to credit Articulate Software
and Teknowledge in any writings, briefings, publications, presentations, or
other representations of any software which incorporates, builds on, or uses this
code.  Please cite the following article in any publication with references:

Pease, A., (2003). The Sigma Ontology Development Environment,
in Working Notes of the IJCAI-2003 Workshop on Ontology and Distributed Systems,
August 9, Acapulco, Mexico. See also http://sigmakee.sourceforge.net
 */

import java.util.*;
import java.util.regex.*;
import java.io.*;

/** A utility class that creates HTML-formatting Strings for various purposes. */
public class HTMLformatter {

    public static String htmlDivider =
        ("<table align=\"left\" width=\"50%\">"
                + "<tr><td bgcolor=\"#A8BACF\">"
                + "<img src=\"pixmaps/1pixel.gif\" width=\"1\" height=\"1\" border=\"0\">"
                + "</td></tr>"
                + "</table><br><br>\n");

    // set by BrowseBody.jsp or SimpleBrowseBody.jsp
    public static String kbHref = "";

    // set by BrowseBody.jsp or SimpleBrowseBody.jsp
    public static String language = "EnglishLanguage";

    public static ArrayList<String> availableFormalLanguages =
        new ArrayList(Arrays.asList("SUO-KIF","TPTP","traditionalLogic","OWL"));

    /** *************************************************************
     *  Create the HTML for the labeled divider between the sections
     *  of the term display.  Each section displays a sorted list of
     *  the Formulae for which a term appears in a specified argument
     *  position.
     */
    public static String htmlDivider(String label) {

        String result = "";
        try {
            StringBuilder sb = new StringBuilder();
            sb.append("<div><br>");
            sb.append("<table align=\"left\" width=\"50%\">");
            sb.append(StringUtil.getLineSeparator());
            if (StringUtil.isNonEmptyString(label)) {
                sb.append("  <tr>");
                sb.append(StringUtil.getLineSeparator());
                sb.append("    <td align=\"left\" valign=\"bottom\">");
                sb.append(StringUtil.getLineSeparator());
                sb.append("      <b>");
                sb.append(label);
                sb.append("</b>");
                sb.append(StringUtil.getLineSeparator());
                sb.append("    </td>");
                sb.append(StringUtil.getLineSeparator());
                sb.append("  </tr>");
                sb.append(StringUtil.getLineSeparator());
            }
            sb.append("  <tr>");
            sb.append(StringUtil.getLineSeparator());
            sb.append("    <td bgcolor=\"#A8BACF\">");
            sb.append(StringUtil.getLineSeparator());
            sb.append("      ");
            sb.append("<img src=\"pixmaps/1pixel.gif\" ");
            sb.append("alt=\"-------------------------\" width=\"1\" height=\"1\">");
            sb.append(StringUtil.getLineSeparator());
            sb.append("    </td>");
            sb.append(StringUtil.getLineSeparator());
            sb.append("  </tr>");
            sb.append(StringUtil.getLineSeparator());
            sb.append("</table>");
            sb.append(StringUtil.getLineSeparator());
            sb.append("<br>");
            sb.append("<br>");
            sb.append(StringUtil.getLineSeparator());
            result = sb.toString();
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
        return result;
    }

    /** *************************************************************
     *  Create the HTML for a single step in a proof.
     */
    public static String proofTableFormat(String query, ProofStep step, String kbName, String language) {

        // System.out.println("Info in HTMLformatter.proofTableFormat(): " + step);
        StringBuilder result = new StringBuilder();
        Formula f = new Formula();
        KB kb = KBmanager.getMgr().getKB(kbName);
        f.read(step.axiom);
        String axiom = new String(f.theFormula);
        f.theFormula = Formula.postProcess(f.theFormula);
        String hostname = KBmanager.getMgr().getPref("hostname");
        if (hostname == null)
            hostname = "localhost";
        String port = KBmanager.getMgr().getPref("port");
        if (port == null)
            port = "8080";
        String kbHref = "http://" + hostname + ":" + port + "/sigma/Browse.jsp?lang=" + language + "&kb=" + kbName;

        if (f.theFormula.equalsIgnoreCase("FALSE")) {        // Successful resolution theorem proving results in a contradiction.
            f.theFormula = "True";                           // Change "FALSE" to "True" so it makes more sense to the user.
            result.append("<td valign=\"top\" width=\"50%\">" + "True" + "</td>");
        }
        else
            result.append("<td valign=\"top\" width=\"50%\">" + f.htmlFormat(kbHref) + "</td>");
        result.append("<td valign=\"top\" width=\"10%\">");

        // System.out.println("Info in HTMLformatter.proofTableFormat(): premises : " + step.premises);
        for (int i = 0; i < step.premises.size(); i++) {
            Integer stepNum = (Integer) step.premises.get(i);
            result.append(stepNum.toString() + " ");
        }
        if (step.premises.size() == 0) {
            if (step.formulaRole != null)
                result.append(step.formulaRole);
            else if (Formula.isNegatedQuery(query,f.theFormula))
                result.append("[Negated Query]");
            else
                result.append("[KB]");
        }
        result.append("</td><td width=\"40%\" valign=\"top\">");
        if (StringUtil.isNonEmptyString(language)) {
            String pph = LanguageFormatter.htmlParaphrase(kbHref,
                    f.theFormula,
                    KBmanager.getMgr().getKB(kbName).getFormatMap(language),
                    KBmanager.getMgr().getKB(kbName).getTermFormatMap(language),
                    kb,
                    language);
            if (StringUtil.emptyString(pph))
                pph = "";
            else {
                pph = LanguageFormatter.upcaseFirstVisibleChar(pph, true, language);
                boolean isArabic = (language.matches(".*(?i)arabic.*")
                        || language.equalsIgnoreCase("ar"));
                if (isArabic)
                    pph = ("<span dir=\"rtl\">" + pph + "</span>");
                // pph = ("&#x202b;" + pph + "&#x202c;");
            }
            result.append(pph);
        }
        result.append("</td>");
        return result.toString();
    }

    /** *************************************************************
     */
    public static String processFormalLanguage(String flang) {

        if (!StringUtil.isNonEmptyString(flang) || !availableFormalLanguages.contains(flang))
            return "SUO-KIF";
        else
            return flang;
    }

    /** *************************************************************
     */
    public static String processNaturalLanguage(String lang, KB kb) {

        if (kb == null || !kb.availableLanguages().contains(lang) || !StringUtil.isNonEmptyString(lang))
            return "EnglishLanguage";
        else
            return lang;
    }

    /** *************************************************************
     *  Show a hyperlinked list of terms.
     */
    public static String termList(ArrayList terms, String kbHref) {

        StringBuilder show = new StringBuilder();
        for (int i = 0; i < terms.size(); i++) {
            String term = (String) terms.get(i);
            show.append("<a href=\"" + kbHref + "&term=" + term + "\">" + term + "</a>");
            if (i < terms.size()-1)
                show.append(", ");
        }
        return show.toString();
    }

    /** *************************************************************
     *  Show knowledge base statistics
     */
    public static String showStatistics(KB kb) {

        StringBuilder show = new StringBuilder();
        show.append("<b>Knowledge base statistics: </b><br><table>");
        show.append("<tr bgcolor=#eeeeee><td>Total Terms</td><td>Total Axioms</td><td>Total Rules</td><tr><tr align='center'>\n");
        show.append("<td>  " + kb.getCountTerms());
        show.append("</td><td> " + kb.getCountAxioms());
        show.append("</td><td> " + kb.getCountRules());
        show.append("</td><tr> </table><p>\n");
        show.append("Relations: " + kb.getCountRelations());
        show.append("<p>\n");
        return show.toString();
    }

    /** *************************************************************
     *  Show a map if coordinates are given in the kb
     */
    public static String showMap(KB kb, String term) {

        ArrayList<Formula> lats = kb.askWithRestriction(0,"latitude",1,term);
        ArrayList<Formula> lons = kb.askWithRestriction(0,"longitude",1,term);
        String result = "";
        int zoom = 12;
        if (lats != null && lats.size() > 0 && lons != null && lons.size() > 0) {
            Formula f = lats.get(0);
            String lat = f.getArgument(2);
            f = lons.get(0);
            String lon = f.getArgument(2);
            if (kb.childOf(term,"Nation"))
                zoom = 6;
            if (kb.childOf(term,"Ocean"))
                zoom = 3;
            if (kb.childOf(term,"Continent"))
                zoom = 6;
            result = "<a href=\"http://maps.google.com/maps?q=" + lat + "," + lon + "&zoom=" + zoom +
            "&markers=label:" + term +
            "\"><img src=\"http://maps.google.com/maps/api/staticmap?center=" + lat + "," +
            lon + "&size=200x100&sensor=false&zoom=" + zoom + "&markers=label:" + term + "\"></a>\n";
        }
        return result;
    }

    /** *************************************************************
     *  Show knowledge base pictures
     */
    public static String showPictures(KB kb, String term) {

        return showNumberPictures(kb,term,4);
    }

    /** *************************************************************
     *  Show knowledge base pictures
     */
    public static String showNumberPictures(KB kb, String term, int count) {

        StringBuilder show = new StringBuilder();
        ArrayList pictures = kb.askWithRestriction(0,"externalImage",1,term);   // Handle picture display
        if (pictures != null && pictures.size() > 0) {
            show.append("<br>");
            int numPictures = pictures.size();
            boolean more = false;
            if (pictures.size() > count) {
                numPictures = count;
                more = true;
            }
            for (int i = 0; i < numPictures; i++) {
                Formula f = (Formula) pictures.get(i);
                String url = f.getArgument(2);
                if (url.startsWith("\"http://upload.wikimedia.org")) {
                    String imageFile = url.substring(url.lastIndexOf("/")+1,url.length()-1);
                    if (imageFile.matches("\\d+px-.*"))
                        imageFile = imageFile.substring(imageFile.indexOf("px-")+3);
                    String domain = "http://simple.wikipedia.org/";
                    if (url.indexOf("/en/") > -1)
                        domain = "http://en.wikipedia.org/";
                    if (url.indexOf("/commons/") > -1)
                        domain = "http://commons.wikimedia.org/";
                    show.append("<a href=\"" + domain + "wiki/Image:" +
                            imageFile + "\"><img width=\"100\" src=" + url + "></a>\n" );
                }
                else
                    show.append("<a href=" + url + "><img width=\"100\" src=" + url + "></a>\n");
            }
            if (more)
                show.append("<a href=\"AllPictures.jsp?term=" + term + "&kb=" + kb.name + "\">more pictures...</a>");
        }
        return show.toString();
    }

    public static String showNeighborTerms(KB kb, String term) {
    	return HTMLformatter.showNeighborTerms(kb, term, term);
    }

    /** *************************************************************
     *  Show alphabetic list of neighbor terms
     */
    public static String showNeighborTerms(KB kb, String nonRelTerm, String relTerm) {
        String markup = "";
        try {
            StringBuilder show = new StringBuilder();
            ArrayList relations = kb.getNearestRelations(relTerm); 
            ArrayList nonRelations = kb.getNearestNonRelations(nonRelTerm);
            String lowcaseTerm = Character.toLowerCase(nonRelTerm.charAt(0)) + nonRelTerm.substring(1);
            String uppercaseTerm = Character.toUpperCase(relTerm.charAt(0)) + relTerm.substring(1);
            show.append("<table><tr><td>");
            show.append("<table>");
            show.append("<tr><td><FONT face='Arial,helvetica' size=+3> <b> " + relTerm + "</b></FONT></td>");
            show.append("<td><FONT face='Arial,helvetica' size=+3> <b> " + nonRelTerm + "</b></FONT></td></tr>\n<br><br>");
            for (int i = 0; i < 30; i++) {
                String relation = (String) relations.get(i);
                String relationName = DocGen.getInstance(kb.name).showTermName(kb,relation,language);
                String nonRelation = (String) nonRelations.get(i);
                String nonRelationName = DocGen.getInstance(kb.name).showTermName(kb,nonRelation,language);
                if (relation != "" || nonRelation != "") {
                	if (i == 0)
                		show.append("<tr><td><i><a href=\"" + kbHref +"&nonrelation=" + nonRelations.get(0) + "&relation=" + relations.get(0) + "\">previous " + 25 + "</a>" + "</i></td></tr>\n");

                    show.append("<tr>\n");
                    show.append("  <td><a href=\"" + kbHref +"&term=");
                    show.append(   relation + "\">" + relation + " (" + relationName + ")</a>" + "</td>");
                    show.append("  <td><a href=\"" + kbHref +"&term=");
                    show.append(   nonRelation + "\">" + nonRelation + " (" + nonRelationName + ")</a>" + "</td>");
                    show.append("</tr>\n");
                    if (i == 14) 
                        show.append("<tr><td><FONT SIZE=4 COLOR=\"RED\">" + uppercaseTerm + " </FONT></td>" +
                        			"<td><FONT SIZE=4 COLOR=\"RED\">" + lowcaseTerm + " </FONT></td></tr>"); 
                    if (i == 29)
                        show.append("<tr><td><i><a href=\"" + kbHref +"&nonrelation=" + nonRelations.get(29) + "&relation=" + relations.get(29) + "\">next " + 25 + "</a>" + "</i></td></tr>\n");
                 }
            }
            show.append("</table></td>");
            markup = show.toString();
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
        return markup;
    }

      
    /** *************************************************************
     *  Show a hyperlinked list of term mappings from WordNet.
     */
    public static String termMappingsList(String terms, String kbHref) {

        StringBuilder result = new StringBuilder();
        String[] sumoList = terms.split("\\s+");
        result.append("<p><ul><li>\tSUMO Mappings:  ");
        for (int j=0; j<sumoList.length; j++) {
            String sumoEquivalent = sumoList[j];
            sumoEquivalent.trim();

            Pattern p = Pattern.compile("\\&\\%");
            Matcher m = p.matcher(sumoEquivalent);
            sumoEquivalent = m.replaceFirst("");
            p = Pattern.compile("[\\=\\|\\+\\@]");
            m = p.matcher(sumoEquivalent);
            char symbol = sumoEquivalent.charAt(sumoEquivalent.length() - 1);
            sumoEquivalent = m.replaceFirst("");
            result.append(kbHref);
            result.append(sumoEquivalent + "\">" + sumoEquivalent + "</a>  ");
            String mapping = WordNetUtilities.mappingCharToName(symbol);
            result.append(" (" + mapping + " mapping) ");
        }
        result.append("\n\n</li></ul>");
        return result.toString();
    }

    /** *************************************************************
     *  Show a hyperlinked list of WordNet synsets.
     */
    public static String synsetList(ArrayList synsets, String kbHref) {

        StringBuilder show = new StringBuilder();
        for (int i = 0; i < synsets.size(); i++) {
            String synset = (String) synsets.get(i);
            if (Character.isDigit(synset.charAt(0)))
                show.append("<a href=\"" + kbHref + "&synset=" + synset + "\">" + synset + "</a>");
            else
                show.append(synset);
            if (i < synsets.size()-1)
                show.append(", ");
            if (i % 10 == 0)
                show.append("\n");
        }
        return show.toString();
    }

    /** *************************************************************
     *  Create the HTML for a section of the Sigma term browser page.
     */
    public static String browserSectionFormatLimit(String term, String header, KB kb,
            String language, String flang, int start, int limit,
            int arg, String type) {

        ArrayList forms = kb.ask(type,arg,term);
        StringBuilder show = new StringBuilder();
        String limitString = "";
        int localLimit = start + limit;
        boolean traditionalLogic = false;

        if (flang.equals("traditionalLogic"))
            traditionalLogic = true;
        if (forms != null && !KBmanager.getMgr().getPref("showcached").equalsIgnoreCase("yes"))
            forms = TaxoModel.removeCached(forms);
        if (forms != null && !forms.isEmpty()) {
            Collections.sort(forms);
            show.append(htmlDivider(header));
            show.append("<table width=\"95%\">");
            if (forms.size() < localLimit)
                localLimit = forms.size();
            else
                limitString = ("<tr><td><br></td></tr><tr><td>Display limited to "
                        + limit + " items. "
                        + "<a href=\"BrowseExtra.jsp?term=" + term + "&lang=" + language + "&flang=" + flang
                        + "&kb=" + kb.name + "&start=" + (start+limit)
                        + "&arg=" + arg + "&type=" + type + "\">Show next "
                        + limit + "</a></td></tr>\n");

            boolean isArabic = (language.matches(".*(?i)arabic.*")
                    || language.equalsIgnoreCase("ar"));
            //System.out.println("INFO in HTMLformatter.browserSectionFormatLimit(): localLimit" + localLimit);
            for (int i = start; i < localLimit; i++) {
                Formula f = (Formula) forms.get(i);

                if (KBmanager.getMgr().getPref("showcached").equalsIgnoreCase("yes") ||
                        !f.sourceFile.endsWith(KB._cacheFileSuffix) ) {
                    String arg0 = f.getArgument(0);
                    show.append("<tr><td width=\"50%\" valign=\"top\">");
                    String formattedFormula = null;
                    if (flang.equals("TPTP") || flang.equals("traditionalLogic"))
                        formattedFormula = TPTPutil.htmlTPTPFormat(f,kbHref,traditionalLogic) + "</td>\n<td width=\"10%\" valign=\"top\" bgcolor=\"#B8CADF\">";
                    else
                        formattedFormula = f.htmlFormat(kbHref) + "</td>\n<td width=\"10%\" valign=\"top\" bgcolor=\"#B8CADF\">";
                    if (Formula.DOC_PREDICATES.contains(arg0))
                        show.append(kb.formatDocumentation(kbHref,formattedFormula,language));
                    else
                        show.append(formattedFormula);
                    File srcfile = new File(f.sourceFile);
                    String sourceFilename = srcfile.getName();
                    if (StringUtil.isNonEmptyString(sourceFilename)) {
                        show.append(sourceFilename);
                        show.append(" " + f.startLine + "-" + f.endLine);
                    }
                    show.append("</a>");
                    show.append("</td>\n<td width=\"40%\" valign=\"top\">");
                    String pph = null;
                    if (!Formula.DOC_PREDICATES.contains(arg0))
                        pph = LanguageFormatter.htmlParaphrase(kbHref,f.theFormula,
                                kb.getFormatMap(language),
                                kb.getTermFormatMap(language),
                                kb, language);
                    if (StringUtil.emptyString(pph))
                        pph = "";
                    else if (isArabic)
                        pph = ("<span dir=\"rtl\">" + pph + "</span>");
                    else
                        pph = LanguageFormatter.upcaseFirstVisibleChar(pph, true, language);
                    show.append(pph + "</td></tr>\n");
                }
            }
            show.append(limitString);
            show.append("</table>\n");
        }
        return show.toString();
    }

    /** *************************************************************
     *  Create the HTML for a section of the Sigma term browser page.
     */
    public static String browserSectionFormat(String term, String header,
            KB kb, String language, String flang, int arg, String type) {

        return browserSectionFormatLimit(term, header,kb, language, flang, 0, 50, arg,type);
    }

    /** *************************************************************
     *  Change spaces to "%20"
     */
    public static String encodeForURL(String s) {

        return s.replaceAll(" ","%20");
    }

    /** *************************************************************
     *  Change spaces to "%20"
     */
    public static String decodeFromURL(String s) {

        return s.replaceAll("%20"," ");
    }

    /** *************************************************************
     *  change reserved characters from '&' tags
     */
    public static String encodeForHTML(String s) {

        s = s.replaceAll("&lt;","<");
        s = s.replaceAll("&gt;",">");
        return s;
    }

    /** *************************************************************
     *  change reserved characters to '&' tags
     */
    public static String decodeFromHTML(String s) {

        s = s.replaceAll("<","&lt;");
        s = s.replaceAll(">","&gt;");
        return s;
    }

    /** *************************************************************
     *  Create an HTML menu, given an ArrayList of Strings where the
     *  value(s) are String representations of int(s) but the displayed
     *  menu items are String(s).
     */
    public static String createNumberedMenu(String menuName, String selectedOption, ArrayList options) {

        StringBuilder result = new StringBuilder();

        String menuNameProcessed = encodeForURL(menuName);
        result.append("<select name=" + menuNameProcessed);
        result.append(">\n  ");
        for (int i = 0; i < options.size(); i++) {
            result.append("<option value='");
            String menuItem = (String) options.get(i);
            String menuItemProcessed = encodeForURL(menuItem);
            result.append(Integer.toString(i));
            if (selectedOption != null && selectedOption.equalsIgnoreCase(Integer.toString(i)))
                result.append("' selected='yes'>\n");
            else
                result.append("'>\n");
            result.append(menuItem);
            result.append("</option>");
        }
        result.append("\n</select>\n");
        return result.toString();
    }

    /** *************************************************************
     *  Create an HTML menu, given an ArrayList of Strings.
     */
    public static String createMenu(String menuName, String selectedOption, ArrayList options) {
        String params = null;
        return createMenu(menuName, selectedOption, options, params);
    }

    /** *************************************************************
     * hyperlink formulas in error messages
     */
    public static String formatErrors(KB kb, String kbHref) {

        StringBuilder result = new StringBuilder();
        Iterator it = kb.errors.iterator();
        while (it.hasNext()) {
            String err = (String) it.next();
            int p = err.indexOf("(");
            String begin = "";
            String end = "";
            if (p > -1) {
                begin = err.substring(0,p);
                end = err.substring(p);
                Formula f = new Formula();
                f.theFormula = end;
                end = f.htmlFormat(kbHref);
            }
            else
                begin = err;

            result.append(begin + end + "<br>\n");
        }
        return result.toString();
    }

    /** *************************************************************
     *  Create an HTML menu with an ID, given an ArrayList of
     *  Strings, and possibly multiple selections.
     */
    public static String createMultiMenu(String menuName, TreeMap options) {

        StringBuilder result = new StringBuilder();

        String menuNameProcessed = encodeForURL(menuName);
        result.append("<select name=" + menuNameProcessed);
        result.append(" MULTIPLE size=");
        result.append(Integer.toString(options.keySet().size()) + ">\n  ");
        for (Iterator it = options.keySet().iterator(); it.hasNext();) {
            result.append("<option value='");
            String menuItem = (String) it.next();
            String selected = (String) options.get(menuItem);
            String menuItemProcessed = encodeForURL(menuItem);
            result.append(menuItemProcessed);
            if (selected != null && selected.equals("yes"))
                result.append("' selected='yes'>");
            else
                result.append("'>");
            result.append(menuItem);
            result.append("</option>");
        }
        result.append("\n</select>\n");
        return result.toString();
    }

    /** *************************************************************
     *  Create an HTML menu with an ID, given an ArrayList of Strings.
     */
    public static String createMenu(String menuName, String selectedOption, ArrayList options, String params) {

        StringBuilder result = new StringBuilder();
        TreeSet menuOptions = new TreeSet();
        menuOptions.addAll(options);

        String menuNameProcessed = encodeForURL(menuName);
        result.append("<select name=" + menuNameProcessed);
        if (params != null) {
            result.append(" " + params + " ");
        }
        result.append(">\n  ");
        for (Iterator it = menuOptions.iterator(); it.hasNext();) {
            String menuItem = (String) it.next();
            result.append("<option value='");
            String menuItemProcessed = encodeForURL(menuItem);
            result.append(menuItemProcessed);
            if (selectedOption != null && selectedOption.equalsIgnoreCase(menuItem))
                result.append("' selected='yes'>");
            else
                result.append("'>");
            result.append(menuItem);
            result.append("</option>");
        }
        result.append("\n</select>\n");
        return result.toString();
    }

    /** *************************************************************
     *  Create an HTML formatted result of a query.
     */
    public static String formatProofResult(String result, String stmt, String processedStmt,
            String lineHtml, String kbName, String language) {
        return formatProofResult(result, stmt, processedStmt, lineHtml, kbName, language, 1);
    }

    public static String formatProofResult(String result, String stmt, String processedStmt,
            String lineHtml, String kbName, String language, int answerOffset) {

        StringBuilder html = new StringBuilder();
        if (result != null && result.toString().length() > 0) {
            BasicXMLparser res = new BasicXMLparser(result.toString());

            ProofProcessor pp = new ProofProcessor(res.elements);
            for (int i = 0; i < pp.numAnswers(); i++) {
                ArrayList proofSteps = pp.getProofSteps(i);
                proofSteps = new ArrayList(ProofStep.normalizeProofStepNumbers(proofSteps));
                proofSteps = new ArrayList(ProofStep.removeDuplicates(proofSteps));

                if (i != 0)
                    html = html.append(lineHtml + "\n");
                html = html.append("Answer " + "\n");
                html = html.append(i+answerOffset);                
                html = html.append(". ");
                String[] answer = pp.returnAnswer(i, processedStmt).split(";");
                for(int k=0; k<answer.length; k++) 
                	html.append(answer[k]+ "<br/>");
                if (!pp.returnAnswer(i, processedStmt).equalsIgnoreCase("no")) {
                    html = html.append("<p><table width=\"95%\">" + "\n");
                    for (int j = 0; j < proofSteps.size(); j++) {
                        if (j % 2 == 1)
                            html = html.append("<tr bgcolor=#EEEEEE>" + "\n");
                        else
                            html = html.append("<tr>" + "\n");
                        html = html.append("<td valign=\"top\">" + "\n");
                        html = html.append(j+1);
                        html = html.append(". </td>" + "\n");
                        html = html.append(HTMLformatter.proofTableFormat(stmt,(ProofStep) proofSteps.get(j), kbName, language) + "\n");
                        System.out.println(HTMLformatter.proofTableFormat(stmt,(ProofStep) proofSteps.get(j), kbName, language));
                        html = html.append("</tr>\n" + "\n");
                    }
                    html = html.append("</table>" + "\n");
                }
            }
        }
        return html.toString();
    }
}

