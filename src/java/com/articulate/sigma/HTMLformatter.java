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
August 9, Acapulco, Mexico. See also http://github.com/ontologyportal
 */

import com.articulate.sigma.nlg.NLGUtils;
import com.articulate.sigma.trans.SUMOKBtoTPTPKB;
import com.articulate.sigma.trans.TPTP2SUMO;
import com.articulate.sigma.trans.TPTP3ProofProcessor;
import com.articulate.sigma.trans.TPTPutil;
import com.articulate.sigma.utils.FileUtil;
import com.articulate.sigma.utils.StringUtil;
import com.articulate.sigma.wordNet.WordNetUtilities;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.time.format.DateTimeFormatter;
import java.time.LocalDateTime;

import tptp_parser.*;

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

    public static List<String> availableFormalLanguages =
            new ArrayList<>(Arrays.asList("SUO-KIF", "TPTP", "traditionalLogic", "OWL"));

    public static boolean debug = false;

    /**************************************************************
     * Create the HTML for the labeled divider between the sections
     * of the term display.  Each section displays a sorted list of
     * the Formulae for which a term appears in a specified argument
     * position.
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

    /**************************************************************
     * Create the HTML for a kb link.
     */
    public static String createKBHref(String kbName, String language) {

        return createHrefStart() + "/sigma/Browse.jsp?lang=" + language + "&kb=" + kbName;
    }

    /**************************************************************
     * Create the HTML for a link, taking care of http/https, hostname and port
     */
    public static String createHrefStart() {

        String hostname = KBmanager.getMgr().getPref("hostname");
        if (hostname == null)
            hostname = "localhost";
        String port = KBmanager.getMgr().getPref("port");
        if (port == null)
            port = "8080";
        String https = KBmanager.getMgr().getPref("https");
        //System.out.println("Info in HTMLformatter.createHrefStart(): https is " + https);
        if (https == null || !https.equals("true"))
            https = "http";
        else
            https = "https";
        return https + "://" + hostname + ":" + port;
    }

    /**************************************************************
     * Create the text for a single step in a proof.
     */
    public static String proofTextFormat(String query, ProofStep step, String kbName, String language) {

        StringBuilder result = new StringBuilder();
        Formula f = new Formula();
        KB kb = KBmanager.getMgr().getKB(kbName);
        f.read(step.axiom);
        f.read(Formula.postProcess(f.getFormula()));
        f.read(ProofProcessor.removeNestedAnswerClause(f.getFormula()));

        if (StringUtil.isNonEmptyString(language)) {
            String pph = NLGUtils.htmlParaphrase("",
                    f.getFormula(),
                    KBmanager.getMgr().getKB(kbName).getFormatMap(language),
                    KBmanager.getMgr().getKB(kbName).getTermFormatMap(language),
                    kb,
                    language);
            if (StringUtil.emptyString(pph))
                pph = "";
            else {
                pph = NLGUtils.upcaseFirstVisibleChar(pph, true, language);
            }
            result.append(pph);
        }
        else {
            if (f.getFormula().equalsIgnoreCase("FALSE")) {        // Successful resolution theorem proving results in a contradiction.
                f.read("true");                           // Change "FALSE" to "True" so it makes more sense to the user.
                result.append("QED");
            }
            else
                result.append(Formula.textFormat(f.getFormula()));
            if (step.inferenceType != null && step.inferenceType.equals("assume_negation")) {
                result.append("[Negated Query]");
            }
            else {
                Integer stepNum;
                for (int i = 0; i < step.premises.size(); i++) {
                    stepNum = step.premises.get(i);
                    result.append(stepNum.toString()).append(" ");
                }
                if (step.premises.isEmpty()) {
                    if (step.formulaType != null && step.formulaType.equals("conjecture"))
                        result.append("[Query]");
                    else if (step.formulaRole != null)
                        result.append(step.formulaRole);
                    else
                        result.append("[KB]");
                }
            }
        }
        return result.toString();
    }

    /**************************************************************
     * Create the HTML for a single step in a proof.
     */
    public static String proofTableFormat(String query, TPTPFormula step, String kbName, String language) {

        if (debug) System.out.println("Info in HTMLformatter.proofTableFormat(): " + step);
        StringBuilder result = new StringBuilder();
        Formula f = new Formula();
        KB kb = KBmanager.getMgr().getKB(kbName);
        f.read(step.sumo);
        f.read(Formula.postProcess(f.getFormula()));
        f.read(ProofProcessor.removeNestedAnswerClause(f.getFormula()));
        String kbHref = HTMLformatter.createKBHref(kbName, language);

        if (f.getFormula().equalsIgnoreCase("FALSE")) {        // Successful resolution theorem proving results in a contradiction.
            f.read("true");                           // Change "FALSE" to "True" so it makes more sense to the user.
            result.append("<td valign=\"top\" width=\"50%\">" + "QED" + "</td>");
        }
        else
            result.append("<td valign=\"top\" width=\"50%\">").append(f.htmlFormat(kbHref)).append("</td>");
        result.append("<td valign=\"top\" width=\"10%\">");

        if (debug) System.out.println("Info in HTMLformatter.proofTableFormat(): premises : " + step.supports);
        if (step.infRule != null && step.infRule.equals("assume_negation")) {
            result.append("[Negated Query]");
        }
        else {
            for (int i = 0; i < step.supports.size(); i++) {
                //String stepName = step.supports.get(i);
                result.append(step.intsupports.get(i)).append(" ");
            }
            if (step.intsupports.isEmpty()) {
                if (step.type != null && step.type.equals("conjecture"))
                    result.append("[Query]");
                else if (!StringUtil.emptyString(step.infRule) &&
                        !step.infRule.equals("input") &&
                        !step.infRule.startsWith("kb_")) {
                    if (KBmanager.getMgr().prover == KBmanager.Prover.VAMPIRE)
                        result.append("[<a href=\"VampProofSteps.html\">").append(step.infRule).append("</a>]");
                    else
                        result.append("[").append(step.infRule).append("]");
                }
                else if (f.getFormula().contains("ans0"))
                    result.append("answer literal introduction");
                else {
                    result.append("[KB -");
                    String key = step.infRule;
                    Formula originalF = SUMOKBtoTPTPKB.axiomKey.get(key);
                    if (originalF != null)
                        result.append(originalF.startLine).append(":").append(FileUtil.noPath(originalF.getSourceFile()));
                    result.append("]");
                }
            }
            else if (!StringUtil.emptyString(step.infRule))
                result.append("[<a href=\"VampProofSteps.html\">").append(step.infRule).append("</a>]");
        }
        result.append("</td><td width=\"40%\" valign=\"top\">");
        if (StringUtil.isNonEmptyString(language)) {
            String pph = NLGUtils.htmlParaphrase(kbHref,
                    f.getFormula(),
                    KBmanager.getMgr().getKB(kbName).getFormatMap(language),
                    KBmanager.getMgr().getKB(kbName).getTermFormatMap(language),
                    kb,
                    language);
            if (StringUtil.emptyString(pph))
                pph = "";
            else {
                pph = NLGUtils.upcaseFirstVisibleChar(pph, true, language);
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

    /**************************************************************
     */
    public static String processFormalLanguage(String flang) {

        if (!StringUtil.isNonEmptyString(flang) || !availableFormalLanguages.contains(flang))
            return "SUO-KIF";
        else
            return flang;
    }

    /**************************************************************
     */
    public static String processNaturalLanguage(String lang, KB kb) {

        if (kb == null || !kb.availableLanguages().contains(lang) || !StringUtil.isNonEmptyString(lang))
            return "EnglishLanguage";
        else
            return lang;
    }

    /**************************************************************
     * Show a hyperlinked list of terms.
     */
    public static String termList(List<String> terms, String kbHref) {

        StringBuilder show = new StringBuilder();
        String term;
        for (int i = 0; i < terms.size(); i++) {
            term = terms.get(i);
            show.append("<a href=\"").append(kbHref).append("&term=").append(term).append("\">").append(term).append("</a>");
            if (i < terms.size() - 1)
                show.append(", ");
        }
        return show.toString();
    }

    /**************************************************************
     */
    public static String getDate() {

        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy/MM/dd");
        LocalDateTime now = LocalDateTime.now();
        return dtf.format(now);
    }

    /**************************************************************
     * Show knowledge base statistics
     */
    public static String showStatistics(KB kb) {

        StringBuilder show = new StringBuilder();
        show.append("<b>Knowledge base statistics: </b> as of ");
        show.append(getDate()).append("<br><table>");
        show.append("<tr bgcolor=#eeeeee><td>Total Terms</td><td>Total Axioms</td><td>Total Rules</td><tr><tr align='center'>\n");
        show.append("<td>  ").append(kb.getCountTerms());
        show.append("</td><td> ").append(kb.getCountAxioms());
        show.append("</td><td> ").append(kb.getCountRules());
        show.append("</td></tr> </table><p>\n");

        show.append("<table><tr><td>Relations: </td><td align=right>").append(kb.getCountRelations()).append("</td></tr>\n");
        show.append("<tr><td>non-linguistic axioms: </td><td align=right>").append(KButilities.getCountNonLinguisticAxioms(kb)).append("</td></tr>\n");
        show.append("</table>\n");

        Map<String, Integer> stats = (HashMap) KButilities.countFormulaTypes(kb);
        show.append("<P><table><tr><td>Ground tuples: </td><td align=right>").append(stats.get("ground")).append("</td></tr>\n");
        show.append("<tr><td>&nbsp;&nbsp;of which are binary: </td><td align=right>").append(stats.get("binary")).append("</td></tr>\n");
        show.append("<tr><td>&nbsp;&nbsp;of which arity more than binary: </td><td align=right>").append(stats.get("higher-arity")).append("</td></tr>\n");
        show.append("</table>\n");

        show.append("<P><table><tr><td>Rules: </td><td align=right>").append(kb.getCountRules()).append("</td></tr>\n");
        show.append("<tr><td>&nbsp;&nbsp;of which are</td><td> horn: </td><td align=right>").append(stats.get("horn")).append("</td></tr>\n");
        show.append("<tr><td></td><td> first-order: </td><td align=right>").append(stats.get("first-order")).append("</td></tr>\n");
        show.append("<tr><td></td><td> temporal: </td><td align=right>").append(stats.get("temporal")).append("</td></tr>\n");
        show.append("<tr><td></td><td>modal: </td><td align=right>").append(stats.get("modal")).append("</td></tr>\n");
        show.append("<tr><td></td><td>epistemic: </td><td align=right>").append(stats.get("epistemic")).append("</td></tr>\n");
        show.append("<tr><td></td><td>other higher-order: </td><td align=right>").append(stats.get("otherHOL")).append("</td></tr>\n");
        show.append("</table><P>\n");
        return show.toString();
    }

    /**************************************************************
     * Show knowledge base statistics
     */
    public static String showLanguageStats(KB kb, String lang) {

        StringBuilder show = new StringBuilder();
        show.append("<tr><td>termFormats: </td><td align=right>").append(KButilities.getCountTermFormats(kb, lang)).append("</td></tr>\n");
        show.append("<tr><td>unique terms in termFormats: </td><td align=right> ").append(KButilities.getCountUniqueTermFormats(kb, lang)).append("</td></tr>\n");
        show.append("</table><p>\n");
        return show.toString();
    }

    /**************************************************************
     * Show a map if coordinates are given in the kb
     */
    public static String showMap(KB kb, String term) {

        List<Formula> lats = kb.askWithRestriction(0, "latitude", 1, term);
        List<Formula> lons = kb.askWithRestriction(0, "longitude", 1, term);
        String result = "";
        int zoom = 12;
        if (lats != null && !lats.isEmpty() && lons != null && !lons.isEmpty()) {
            Formula f = lats.get(0);
            String lat = f.getStringArgument(2);
            f = lons.get(0);
            String lon = f.getStringArgument(2);
            if (kb.childOf(term, "Nation"))
                zoom = 6;
            if (kb.childOf(term, "Ocean"))
                zoom = 3;
            if (kb.childOf(term, "Continent"))
                zoom = 6;
            result = "<a href=\"http://maps.google.com/maps?q=" + lat + "," + lon + "&zoom=" + zoom +
                    "&markers=label:" + term +
                    "\"><img src=\"http://maps.google.com/maps/api/staticmap?center=" + lat + "," +
                    lon + "&size=200x100&sensor=false&zoom=" + zoom + "&markers=label:" + term + "\"></a>\n";
        }
        return result;
    }

    /**************************************************************
     * Show knowledge base pictures
     */
    public static String showPictures(KB kb, String term) {

        return showNumberPictures(kb, term, 4);
    }

    /**************************************************************
     * Show knowledge base pictures
     */
    public static String showNumberPictures(KB kb, String term, int count) {

        StringBuilder show = new StringBuilder();
        List<Formula> pictures = kb.askWithRestriction(0, "externalImage", 1, term);   // Handle picture display
        if (pictures != null && !pictures.isEmpty()) {
            show.append("<br>");
            int numPictures = pictures.size();
            boolean more = false;
            if (pictures.size() > count) {
                numPictures = count;
                more = true;
            }

            Formula f;
            String url;
            String imageFile, domain;
            for (int i = 0; i < numPictures; i++) {
                f = pictures.get(i);
                url = f.getStringArgument(2);
                if (url.startsWith("\"https://upload.wikimedia.org")) {
                    imageFile = url.substring(url.lastIndexOf("/") + 1, url.length() - 1);
                    if (imageFile.matches("\\d+px-.*"))
                        imageFile = imageFile.substring(imageFile.indexOf("px-") + 3);
                    domain = "https://simple.wikipedia.org/";
                    if (url.contains("/en/"))
                        domain = "https://en.wikipedia.org/";
                    if (url.contains("/commons/"))
                        domain = "https://commons.wikimedia.org/";
                    show.append("<a href=\"").append(domain).append("wiki/Image:").append(imageFile).append("\"><img width=\"100\" src=").append(url).append("></a>\n");
                }
                else
                    show.append("<a href=").append(url).append("><img width=\"100\" src=").append(url).append("></a>\n");
            }
            if (more)
                show.append("<a href=\"AllPictures.jsp?term=").append(term).append("&kb=").append(kb.name).append("\">more pictures...</a>");
        }
        return show.toString();
    }

    /**************************************************************
     */
    public static String showNeighborTerms(KB kb, String term) {
        return HTMLformatter.showNeighborTerms(kb, term, term);
    }

    /**************************************************************
     * Show alphabetic list of neighbor terms
     */
    public static String showNeighborTerms(KB kb, String nonRelTerm, String relTerm) {

        String markup = "";
        try {
            StringBuilder show = new StringBuilder();
            List<String> relations = kb.getNearestRelations(relTerm);
            List<String> nonRelations = kb.getNearestNonRelations(nonRelTerm);
            String lowcaseTerm = Character.toLowerCase(nonRelTerm.charAt(0)) + nonRelTerm.substring(1);
            String uppercaseTerm = Character.toUpperCase(relTerm.charAt(0)) + relTerm.substring(1);
            show.append("<table><tr><td>");
            show.append("<table>");
            show.append("<tr><td><FONT face='Arial,helvetica' size=+3> <b> ").append(relTerm).append("</b></FONT></td>");
            show.append("<td><FONT face='Arial,helvetica' size=+3> <b> ").append(nonRelTerm).append("</b></FONT></td></tr>\n<br><br>");

            String relation, relationName, nonRelation, nonRelationName;
            for (int i = 0; i < 30; i++) {
                relation = (String) relations.get(i);
                relationName = DocGen.getInstance(kb.name).showTermName(kb, relation, language);
                nonRelation = (String) nonRelations.get(i);
                nonRelationName = DocGen.getInstance(kb.name).showTermName(kb, nonRelation, language);
                if (!relation.isEmpty() || !nonRelation.isEmpty()) {
                    if (i == 0)
                        show.append("<tr><td><i><a href=\"").append(kbHref).append("&nonrelation=").append(nonRelations.get(0)).append("&relation=").append(relations.get(0)).append("&KBPOS=").append(1).append("\">previous ").append(25).append("</a></i></td></tr>\n");

                    show.append("<tr>\n");
                    show.append("  <td><a href=\"").append(kbHref).append("&term=");
                    show.append(nonRelation).append("\">").append(nonRelation).append(" (").append(nonRelationName).append(")</a></td>");
                    show.append("  <td><a href=\"").append(kbHref).append("&term=");
                    show.append(relation).append("\">").append(relation).append(" (").append(relationName).append(")</a></td>");
                    show.append("</tr>\n");
                    if (i == 14)
                        show.append("<tr><td><FONT SIZE=4 COLOR=\"RED\">").append(uppercaseTerm).append(" </FONT></td><td><FONT SIZE=4 COLOR=\"RED\">").append(lowcaseTerm).append(" </FONT></td></tr>");
                    if (i == 29)
                        show.append("<tr><td><i><a href=\"").append(kbHref).append("&nonrelation=").append(nonRelations.get(29)).append("&relation=").append(relations.get(29)).append("&KBPOS=").append(1).append("\">next ").append(25).append("</a></i></td></tr>\n");
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

    /******************************************************
     */
    public static List<String> getAllRelTerms(KB kb, List<String> matchesList) {

        List<String> result = new ArrayList<>();
        for (int i = 0; i < matchesList.size(); i++)
            if (kb.kbCache.relations.contains(matchesList.get(i)))
                result.add(matchesList.get(i));
        return result;
    }

    /******************************************************
     */
    public static List<String> getAllNonRelTerms(KB kb, List<String> matchesList) {

        List<String> result = new ArrayList<>();
        for (int i = 0; i < matchesList.size(); i++)
            if (!kb.kbCache.relations.contains(matchesList.get(i)))
                result.add(matchesList.get(i));
        return result;
    }

    /******************************************************
     * Show list of 30 relation & nonRelation terms that contain a match to the input RE term. The inputed Strings
     * relREmatch and nonRelREmatch are the two relation and nonRelation terms respectively that are the first terms
     * at the top of the list. They are passed into the method to keep track of what 30 terms are being viewed.
     */
    public static String showREMatches(KB kb, String relREmatch, String nonRelREmatch, String term) {

        String markup = "";
        try {
            StringBuilder show = new StringBuilder();
            List<String> matchesList = kb.getREMatch(term, true);
            List<String> relTermsList = getAllRelTerms(kb, matchesList);
            List<String> nonRelTermsList = getAllNonRelTerms(kb, matchesList);
            List<String> largerList = (relTermsList.size() > nonRelTermsList.size()) ? relTermsList : nonRelTermsList;
            List<String> smallerList = (relTermsList.size() > nonRelTermsList.size()) ? nonRelTermsList : relTermsList;
            int sizeDiff = largerList.size() - smallerList.size();
            for (int i = 0; i < sizeDiff; i++) {                //buffer smaller list
                smallerList.add("");
            }
            show.append("<table><tr><td>");
            show.append("<table>");
            show.append("<tr><td><FONT face='Arial,helvetica' size=+3> <b> ").append(term).append("</b></FONT></td>");
            show.append("</tr>\n<br><br>");
            int matchIndex, listLength, finalIndex;
            for (String t : largerList) {
                if (t.equals((largerList == relTermsList ? relREmatch : nonRelREmatch))) {        //keeps track of which term is at the top
                    matchIndex = largerList.indexOf(t);      //matchIndex is the index of an REmatch in the larger list
                    listLength = largerList.size();          //listLength is the the larger count of either relMatches or nonRelMatches
                    finalIndex = (listLength > (matchIndex + 29) ? (matchIndex + 30) : listLength);    //finalIndex is 1 + the index of the final match that will be displayed
                    //If there are at least 30 more matches after REmatch, then finalIndex=matchIndex+30, otherwise finalIndex = listLength
                    for (int i = matchIndex; i < finalIndex; i++) {
                        if (i == matchIndex && i != 0)     //if there are other matches before REmatch, previous 30 should be linked at the top of the page
                            show.append("<tr><td><i><a href=\"").append(kbHref).append("&relREmatch=").append(relTermsList.get(matchIndex - 29)).append("&nonRelREmatch=").append(nonRelTermsList.get(matchIndex - 29)).append("&KBPOS=").append(2).append("&term=").append(encodeForURL(term)).append("\">previous ").append(30).append("</a></i></td></tr>\n");
                        show.append("<tr>\n");
                        if ("".equals(nonRelTermsList.get(i)))
                            show.append("    <td><b> " + " " + "</b></td>");
                        else {
                            show.append("    <td><a href=\"").append(kbHref).append("&term=");
                            show.append(nonRelTermsList.get(i)).append("\">").append(nonRelTermsList.get(i)).append("</a></td>");
                        }
                        if ("".equals(relTermsList.get(i)))
                            show.append("    <td><b> " + " " + "</b></td>");
                        else {
                            show.append("    <td><a href=\"").append(kbHref).append("&term=");
                            show.append(relTermsList.get(i)).append("\">").append(relTermsList.get(i)).append("</a></td>");
                        }
                        show.append("</tr>\n");
                        if (i == (finalIndex - 1) && listLength > (matchIndex + 30)) {
                            int nextCount = (listLength > finalIndex + 29) ? 30 : (listLength - finalIndex + 1);
                            show.append("<tr><td><i><a href=\"").append(kbHref).append("&relREmatch=").append(relTermsList.get(i)).append("&nonRelREmatch=").append(nonRelTermsList.get(i)).append("&KBPOS=").append(2).append("&term=").append(encodeForURL(term)).append("\">next ").append(nextCount).append("</a></i></td></tr>\n");
                        }
                    }
                    show.append("</table></td>");
                    markup = show.toString();
                    break;
                }
            }
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
        return markup;
    }

    /**************************************************************
     * Show a hyperlinked list of term mappings from WordNet.
     */
    public static String termMappingsList(String terms, String kbHref) {

        StringBuilder result = new StringBuilder();
        String[] sumoList = terms.split("\\s+");
        result.append("<p><ul><li>\tSUMO Mappings:  ");
        Pattern p;
        Matcher m;
        char symbol;
        for (String sumoEquivalent : sumoList) {
            sumoEquivalent = sumoEquivalent.trim();

            p = Pattern.compile("\\&\\%");
            m = p.matcher(sumoEquivalent);
            sumoEquivalent = m.replaceFirst("");
            p = Pattern.compile("[\\=\\|\\+\\@]");
            m = p.matcher(sumoEquivalent);
            symbol = sumoEquivalent.charAt(sumoEquivalent.length() - 1);
            sumoEquivalent = m.replaceFirst("");
            result.append(kbHref);
            result.append(sumoEquivalent).append("\">").append(sumoEquivalent).append("</a>  ");
            String mapping = WordNetUtilities.mappingCharToName(symbol);
            result.append(" (").append(mapping).append(" mapping) ");
        }
        result.append("\n\n</li></ul>");
        return result.toString();
    }

    /**************************************************************
     * Show a hyperlinked list of WordNet synsets.
     */
    public static String synsetList(List<String> synsets, String kbHref) {

        StringBuilder show = new StringBuilder();
        String synset;
        for (int i = 0; i < synsets.size(); i++) {
            synset = (String) synsets.get(i);
            if (Character.isDigit(synset.charAt(0)))
                show.append("<a href=\"").append(kbHref).append("&synset=").append(synset).append("\">").append(synset).append("</a>");
            else
                show.append(synset);
            if (i < synsets.size() - 1)
                show.append(", ");
            if (i % 10 == 0)
                show.append("\n");
        }
        return show.toString();
    }

    /**************************************************************
     * Create the HTML for a section of the Sigma term browser page.
     * Needs a <table>...</table> enclosure to format HTML properly.
     */
    public static String formatFormulaList(List<Formula> forms, String header, KB kb,
                                           String language, String flang, int start, int localLimit, String limitString) {

        Set<String> printedForms = new HashSet<>();
        boolean traditionalLogic = false;
        if (flang.equals("traditionalLogic"))
            traditionalLogic = true;
        StringBuilder show = new StringBuilder();
        boolean isArabic = (language.matches(".*(?i)arabic.*") || language.equalsIgnoreCase("ar"));
        if (forms.size() < localLimit || localLimit == 0)
            localLimit = forms.size();

        String strForm, arg0, formattedFormula, sourceFilename, jeditcmd, pph = null;
        Formula f;
        File srcfile;
        for (int i = start; i < localLimit; i++) {
            //System.out.println("formatFormulaList(): " + forms.get(i).getClass().getName());
            strForm = forms.get(i).getFormula();
            if (printedForms.contains(strForm))
                continue;
            printedForms.add(strForm);
            //System.out.println("INFO in HTMLformatter.formatFormulaList(): formula: " + strForm);
            f = (Formula) kb.formulaMap.get(strForm);
            if (f == null) {
                System.out.println("Error in HTMLformatter.formatFormulaList(): null formula object for " +
                        strForm);
                continue;
            }
            //System.out.println("INFO in HTMLformatter.formatFormulaList(): structured formula: " + f);
            if (KBmanager.getMgr().getPref("showcached").equalsIgnoreCase("yes") ||
                    !KButilities.isCacheFile(f.sourceFile)) {
                arg0 = f.getStringArgument(0);
                show.append("<tr><td width=\"50%\" valign=\"top\">");
                if (flang.equals("TPTP") || flang.equals("traditionalLogic"))
                    formattedFormula = TPTPutil.htmlTPTPFormat(f, kbHref, traditionalLogic) + "</td>\n<td width=\"10%\" valign=\"top\" bgcolor=\"#B8CADF\">";
                else
                    formattedFormula = f.htmlFormat(kbHref) + "</td>\n<td width=\"10%\" valign=\"top\" bgcolor=\"#B8CADF\">";
                if (Formula.DOC_PREDICATES.contains(arg0))
                    show.append(kb.formatDocumentation(kbHref, formattedFormula, language));
                else
                    show.append(formattedFormula);
                srcfile = new File(f.sourceFile);
                sourceFilename = srcfile.getName();
                if (StringUtil.isNonEmptyString(sourceFilename)) {
                    jeditcmd = KBmanager.getMgr().getPref("jedit");
                    if (!StringUtil.emptyString(jeditcmd)) {
                        show.append("<a href=\"").append(kbHref).append("&file=").append(sourceFilename).append("&line=").append(f.startLine).append("\">");
                    }
                    show.append(sourceFilename);
                    show.append(" ").append(f.startLine).append("-").append(f.endLine);
                    if (!StringUtil.emptyString(jeditcmd))
                        show.append("</a>");
                }
                show.append("</a>");
                show.append("</td>\n<td width=\"40%\" valign=\"top\">");
                if (!Formula.DOC_PREDICATES.contains(arg0))
                    pph = NLGUtils.htmlParaphrase(kbHref, f.getFormula(),
                            kb.getFormatMap(language),
                            kb.getTermFormatMap(language),
                            kb, language);
                if (StringUtil.emptyString(pph))
                    pph = "";
                else if (isArabic)
                    pph = ("<span dir=\"rtl\">" + pph + "</span>");
                else
                    pph = NLGUtils.upcaseFirstVisibleChar(pph, true, language);
                show.append(pph).append("</td></tr>\n");
            }
        }
        show.append(limitString);
        return show.toString();
    }

    /**************************************************************
     * Launch the jEdit editor with the cursor at the specified line
     * number.  Edit the file in the specified editDir, which should
     * be different from Sigma's KBs directory.  Recommended practice
     * is to edit .kif files in your local Git repository and then
     * copy them to the .sigmakee/KBs directory
     */
    public static void launchEditor(String file, int line) {

        String editDir = KBmanager.getMgr().getPref("editDir");
        if (StringUtil.emptyString(editDir)) {
            String git = System.getenv("ONTOLOGYPORTAL_GIT");
            if (!StringUtil.emptyString(git))
                editDir = git + File.separator + "sumo";
        }
        String jeditcmd = KBmanager.getMgr().getPref("jedit");
        if (StringUtil.emptyString(jeditcmd))
                jeditcmd = "/user/share/jedit/jedit"; // default
        List<String> commands = new ArrayList<>(Arrays.asList(
                jeditcmd, editDir + File.separator + file, " +line:" + line,
                "-norestore", "-reuseview"));
        System.out.println("EProver(): command: " + commands);
        try {
            System.out.println("launchEditor(): commands: " + commands);
            ProcessBuilder _builder = new ProcessBuilder(commands);
            _builder.redirectErrorStream(false);
            Process _jedit = _builder.start();
        }
        catch (IOException ioe) {
            System.err.println("launchEditor(): " + ioe.getMessage());
            ioe.printStackTrace();
        }
    }

    /**************************************************************
     * Create the HTML for a section of the Sigma term browser page.
     */
    public static String browserSectionFormatLimit(String term, String header, KB kb,
                                                   String language, String flang, int start, int limit,
                                                   int arg, String type) {

        List<Formula> forms = kb.ask(type, arg, term);
        StringBuilder show = new StringBuilder();
        String limitString = "";
        int localLimit = start + limit;
        if (forms != null && !KBmanager.getMgr().getPref("showcached").equalsIgnoreCase("yes"))
            forms = TaxoModel.removeCached(forms);
        if (forms != null && !forms.isEmpty()) {
            Collections.sort(forms);
            show.append(htmlDivider(header));
            show.append("<table width=\"95%\">");
            if (forms.size() < localLimit || localLimit == 0)
                localLimit = forms.size();
            else
                limitString = ("<tr><td><br></td></tr><tr><td>Display limited to "
                        + limit + " items. "
                        + "<a href=\"BrowseExtra.jsp?term=" + term + "&lang=" + language + "&flang=" + flang
                        + "&kb=" + kb.name + "&start=" + (start + limit)
                        + "&arg=" + arg + "&type=" + type + "\">Show next "
                        + limit + "</a></td></tr>\n");

            show.append(formatFormulaList(forms, header, kb, language, flang, start, localLimit, limitString));
            show.append(limitString);
            show.append("</table>\n");
        }
        return show.toString();
    }

    /**************************************************************
     * Create the HTML for a section of the Sigma term browser page.
     */
    public static String browserSectionFormat(String term, String header,
                                              KB kb, String language, String flang, int arg, String type) {

        return browserSectionFormatLimit(term, header, kb, language, flang, 0, 50, arg, type);
    }

    /**************************************************************
     * Change spaces to "%20" along with many other URL codes. (for passing regex expressions through hyperlinks)
     */
    public static String encodeForURL(String s) {

        s = s.replaceAll(" ", "%20");
        s = s.replaceAll("\\!", "%21");
        s = s.replaceAll("\\$", "%24");
        s = s.replaceAll("\\(", "%28");
        s = s.replaceAll("\\)", "%29");
        s = s.replaceAll("\\*", "%2A");
        s = s.replaceAll("\\+", "%2B");
        s = s.replaceAll("\\.", "%2E");
        s = s.replaceAll("\\?", "%3F");
        s = s.replaceAll("\\[", "%5B");
        s = s.replaceAll("\\]", "%5D");
        s = s.replaceAll("\\^", "%5E");

        return s;
    }

    /**************************************************************
     * Change spaces to "%20"
     */
    public static String decodeFromURL(String s) {

        return s.replaceAll("%20", " ");
    }

    /**************************************************************
     * change reserved characters from '&' tags
     */
    public static String encodeForHTML(String s) {

        s = s.replaceAll("&lt;", "<");
        s = s.replaceAll("&gt;", ">");
        return s;
    }

    /**************************************************************
     * change reserved characters to '&' tags
     */
    public static String decodeFromHTML(String s) {

        s = s.replaceAll("<", "&lt;");
        s = s.replaceAll(">", "&gt;");
        return s;
    }

    /**************************************************************
     * Create an HTML menu, given an ArrayList of Strings where the
     * value(s) are String representations of int(s) but the displayed
     * menu items are String(s).
     */
    public static String createNumberedMenu(String menuName, String selectedOption,
                                            ArrayList<String> options) {

        StringBuilder result = new StringBuilder();

        String menuNameProcessed = encodeForURL(menuName);
        result.append("<select name=").append(menuNameProcessed);
        result.append(">\n  ");
        String menuItem;
        for (int i = 0; i < options.size(); i++) {
            result.append("<option value='");
            menuItem = (String) options.get(i);
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

    /**************************************************************
     * Create an HTML menu, given an ArrayList of Strings.
     */
    public static String createMenu(String menuName, String selectedOption, List<String> options) {

        String params = null;
        return createMenu(menuName, selectedOption, options, params);
    }

    /**************************************************************
     * Create an HTML menu of KB names
     */
    public static String createKBMenu(String kbName) {

        List<String> kbnames = new ArrayList<>();
        kbnames.addAll(KBmanager.getMgr().getKBnames());
        return (HTMLformatter.createMenu("kb", kbName, kbnames));
    }

    /**************************************************************
     * hyperlink formulas in error messages.  It assumes that the errors
     * are in a TreeSet of Strings in kb.errors.  It further
     * assumes that the error message is given first, followed by
     * a colon, and then the axiom.  There must be no other colon
     * characters.
     */
    public static String formatErrorsWarnings(Set<String> list, KB kb) {

        System.out.println("INFO in HTMLformatter.formatErrorsWarnings(): href: " + kbHref);
        StringBuilder result = new StringBuilder();
        int p;
        String begin, end;
        Formula f;
        for (String err : list) {
            err = err.replaceAll("\\n", "<br>");
            p = err.indexOf(":");
            begin = "<br>";
            end = "";
            if (p > -1) {
                begin += err.substring(0, p + 1);
                end = err.substring(p + 1);
                f = new Formula();
                f.read(end);
                //end = f.htmlFormat(kbHref);
                end = f.htmlFormat(kb, kbHref);
            }
            else
                begin = err;

            result.append(begin).append(end).append("<P>");
        }
        return result.toString();
    }

    /**************************************************************
     * Create an HTML menu with an ID, given an ArrayList of
     * Strings, and possibly multiple selections.
     */
    public static String createMultiMenu(String menuName, Map<String, String> options) {

        StringBuilder result = new StringBuilder();

        String menuNameProcessed = encodeForURL(menuName);
        result.append("<select name=").append(menuNameProcessed);
        result.append(" MULTIPLE size=");
        if (options == null)
            return "";
        result.append(Integer.toString(options.keySet().size())).append(">\n  ");
        Iterator<String> it = options.keySet().iterator();
        String menuItem, selected, menuItemProcessed;
        while (it.hasNext()) {
            result.append("<option value='");
            menuItem = it.next();
            selected = options.get(menuItem);
            menuItemProcessed = encodeForURL(menuItem);
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

    /**************************************************************
     * Create an HTML menu with an ID, given an ArrayList of Strings.
     */
    public static String createMenu(String menuName, String selectedOption,
                                    List<String> options, String params) {

        //System.out.println("createMenu(): menuName: " + menuName);
        //System.out.println("createMenu(): options: " + options);
        //System.out.println("createMenu(): selectedOption: " + selectedOption);
        if (options == null)
            return "";
        StringBuilder result = new StringBuilder();
        Set<String> menuOptions = new TreeSet<>();
        menuOptions.addAll(options);

        String menuNameProcessed = encodeForURL(menuName);
        result.append("<select name=").append(menuNameProcessed);
        if (params != null)
            result.append(" ").append(params).append(" ");
        result.append(">\n  ");
        String menuItemProcessed;
        for (String menuItem : menuOptions) {
            result.append("<option value='");
            menuItemProcessed = encodeForURL(menuItem);
            result.append(menuItemProcessed);
            if (selectedOption != null && selectedOption.equalsIgnoreCase(menuItem))
                result.append("' selected='yes'>");
            else
                result.append("'>");
            result.append(menuItem);
            result.append("</option>");
        }
        result.append("\n</select>\n");
        //System.out.println("createMenu(): result: " + result);
        return result.toString();
    }

    /**************************************************************
     * Create HTML formatted output for a TPTP3 proof
     */
    public static String formatTPTP3ProofResult(TPTP3ProofProcessor tpp, String stmt,
                                                String lineHtml, String kbName, String language) {

        StringBuilder html = new StringBuilder();
        System.out.println("INFO in HTMLformatter.formatTPTP3ProofResult(): number of steps: " + tpp.proof.size());
        //System.out.println("INFO in HTMLformatter.formatTPTP3ProofResult(): status: " + tpp.status);
        //System.out.println("INFO in HTMLformatter.formatTPTP3ProofResult(): no conjecture: " + tpp.noConjecture);
        if (tpp.proof == null || tpp.proof.isEmpty()) {
            html.append("Fail with status: ").append(tpp.status).append("<br>\n");
        }
        if (tpp != null && !StringUtil.emptyString(tpp.status) &&
                (tpp.status.equals("Refutation") || tpp.status.equals("CounterSatisfiable") || tpp.status.equals("Theorem")) && tpp.noConjecture) {
            tpp.inconsistency = true;
            html.append("<b>Danger! No conjecture, possible inconsistency!</b><P>\n");
        }

        String term, kbHref;
        if (tpp.bindingMap != null && !tpp.bindingMap.keySet().isEmpty()) { // if an answer predicate appears in the proof, use it
            for (String s : tpp.bindingMap.keySet()) {
                html.append("Answer " + "\n");
                html.append(s).append(" = ");
                term = TPTP2SUMO.transformTerm(tpp.bindingMap.get(s));
                kbHref = HTMLformatter.createKBHref(kbName, language);
                html.append("<a href=\"").append(kbHref).append("&term=").append(term).append("\">").append(term).append("</a>");
                html.append("<br/>\n");
            }
        }
        else {
            for (int i = 0; i < tpp.bindings.size(); i++) {
                //if (i != 0)
                //    html.append(lineHtml + "\n");
                html.append("Answer " + "\n");
                html.append(i + 1);
                html.append(". ");
                term = TPTP2SUMO.transformTerm(tpp.bindings.get(i));
                kbHref = HTMLformatter.createKBHref(kbName, language);
                html.append("<a href=\"").append(kbHref).append("&term=").append(term).append("\">").append(term).append("</a>");
                html.append("<br/>\n");
            }
        }
        html.append("<p><table width=\"95%\">" + "\n");

        TPTPFormula ps;
        for (int l = 0; l < tpp.proof.size(); l++) {
            ps = tpp.proof.get(l);
            //System.out.println("HTMLformatter.formatTPTP3ProofResult(): role: " + ps.role);
            if (ps.role.equals("type"))
                continue; // ignore type definitions in tff proof output
            if (l % 2 == 1)
                html.append("<tr bgcolor=#EEEEEE>" + "\n");
            else
                html.append("<tr>" + "\n");
            html.append("<td valign=\"top\">\n");
            html.append(ps.id).append(".");
            html.append("</td>\n");
            html.append(HTMLformatter.proofTableFormat(stmt, tpp.proof.get(l), kbName, language)).append("\n");
            html.append("</tr>\n\n");
        }
        html.append("</table>\n");
        return html.toString();
    }

    /**************************************************************
     */
    public static String formatConsistencyCheck(String msg, String ccheckResult,
                                                String language, int page) {

        StringBuilder html = new StringBuilder();
        String lineHtml = "<table ALIGN='LEFT' WIDTH='40%'><tr><TD BGCOLOR='#AAAAAA'><IMG SRC='pixmaps/1pixel.gif' width=1 height=1 border=0></TD></tr></table><BR>\n";
        html.append(msg);
        html.append("<br/>");

        if (ccheckResult != null) {
            BasicXMLparser res = new BasicXMLparser(ccheckResult);
            String kbName = null;
            try {
                if (res != null) {
                    List elements = res.elements;
                    List subElements = ((BasicXMLelement) elements.get(0)).subelements;

                    BasicXMLelement item, entryItem;
                    List entries, entry;
                    String pagelink, query = null, type = null, sourceFile = null, processedQ, proof = null;
                    for (int i = 0; i < subElements.size(); i++) {
                        item = (BasicXMLelement) subElements.get(i);

                        if (item.tagname.equals("kb"))
                            kbName = item.contents;
                        else if (item.tagname.equals("entries")) {
                            entries = ((BasicXMLelement) subElements.get(i)).subelements;
                            html.append("<br/><b><u>Consistency Check Results:</u></b><br />");
                            if (page == 0) {
                                pagelink = "CCheck.jsp?kb=" + kbName + "&lang=" + language + "&page=";
                                html.append("<br />");
                                html.append("<table width=80% frame='border'>");
                                html.append("<tr><td>Query</td><td>Result Type</td><td>Source File</td><tr>");

                                for (int j = 0; j < entries.size(); j++) {
                                    entry = ((BasicXMLelement) entries.get(j)).subelements;
                                    for (int k = 0; k < entry.size(); k++) {
                                        entryItem = (BasicXMLelement) entry.get(k);
                                        switch (entryItem.tagname) {
                                            case "query":
                                                query = entryItem.contents;
                                                break;
                                            case "type":
                                                type = entryItem.contents;
                                                break;
                                            case "sourceFile":
                                                sourceFile = entryItem.contents;
                                                break;
                                            default:
                                                break;
                                        }
                                    }
                                    int pageNum = j + 1;
                                    html.append("<tr><td><a href='").append(pagelink).append(pageNum).append("'>").append(query).append("</a></td><td>").append(type).append("</td><td>").append(sourceFile).append("</td></tr>");
                                }
                                html.append("</table>");
                                if (!entries.isEmpty())
                                    html.append("<br/><a href='CCheck.jsp?lang=").append(language).append("&kb=").append(kbName).append("&page=1'><p>Individual Results&#32;&gt;&gt;</p></a>");
                            }
                            else if (page >= 1 && page <= entries.size()) {
                                int j = page - 1;
                                entry = ((BasicXMLelement) entries.get(j)).subelements;
                                for (int k = 0; k < entry.size(); k++) {
                                    entryItem = (BasicXMLelement) entry.get(k);
                                    switch (entryItem.tagname) {
                                        case "query":
                                            query = entryItem.contents;
                                            break;
                                        case "type":
                                            type = entryItem.contents;
                                            break;
                                        case "processedStatement":
                                            processedQ = entryItem.contents;
                                            break;
                                        case "sourceFile":
                                            sourceFile = entryItem.contents;
                                            break;
                                        case "proof":
                                            if (!type.contains("Error"))
                                                //if (entryItem.attributes.get("src") != null && entryItem.attributes.get("src").equals("EProver"))
                                                // TODO update to TPTP3 ANTLR	proof = formatTPTP3ProofResult(entryItem.subelements, query, kbName, language, 0);
                                                ;
                                            else {
                                                proof = entryItem.contents;
                                                proof = proof.replaceAll("%3C", "<");
                                                proof = proof.replaceAll("%3E", ">");
                                            }
                                            // else proof = entryItem.contents;
                                            break;
                                        default:
                                            break;
                                    }
                                }
                                html.append("<br/>Query:  ").append(query).append("<br />");
                                html.append("Type:  ").append(type).append("<br />");
                                html.append("Source File: ").append(sourceFile).append("<br /><br />");
                                html.append(proof);

                                html.append(lineHtml);
                                html.append("<table width=80% frame='void'");
                                html.append("<tr>");
                                int before = page - 1;
                                int after = page + 1;
                                if (page == 1)
                                    html.append("<td><a href='CCheck.jsp?lang=").append(language).append("&kb=").append(kbName).append("&page=0'>&lt;&lt;&#32;Summary Result</a></td>");
                                else if (page > 1) {
                                    html.append("<td><a href='CCheck.jsp?lang=").append(language).append("&kb=").append(kbName).append("&page=").append(before).append("'>&lt;&lt;&#32;Prev</a></td>");
                                    html.append("<td><a href='CCheck.jsp?lang=").append(language).append("&kb=").append(kbName).append("&page=0'>&lt;&lt;&#32;Summary Results&#32;&gt;&gt;</a></td>");
                                }
                                if (after <= entries.size() && page >= 1)
                                    html.append("<td><a href='CCheck.jsp?lang=").append(language).append("&kb=").append(kbName).append("&page=").append(after).append("'>Next&#32;&gt;&gt;</a></td>");
                                html.append("</tr></table>");
                            }
                        }
                    }
                }
            }
            catch (Exception ex) {
                System.err.println(ex.getMessage());
            }
        }
        return html.toString();
    }

    /****************************************************************
     */
    public static void showHelp() {

        System.out.println("HTMLformatter class");
        System.out.println("  options:");
        System.out.println("  -h - show this help screen");
        System.out.println("  -t - run tests");
    }

    /** **************************************************************
     */
    public static void main(String[] args) throws IOException {

        System.out.println("INFO in KB.main()");
        if (args != null && args.length > 0 && args[0].equals("-h"))
            showHelp();
        else {
            KBmanager.prefOverride.put("loadLexicons", "false");
            System.out.println("KB.main(): Note! Not loading lexicons.");
            KBmanager.getMgr().initializeOnce();
            String kbName = KBmanager.getMgr().getPref("sumokbname");
            KB kb = KBmanager.getMgr().getKB(kbName);
            if (args != null)
                System.out.println("KB.main(): args[0]: " + args[0]);
            if (args != null && args.length > 0 && args[0].equals("-t")) {
                System.out.println("INFO in HTMLformatter.main()");
                System.out.println("INFO in HTMLformatter.main(): " + showStatistics(kb));
                List<Formula> forms = KButilities.termIntersection(kb, "ShapeChange", "ShapeAttribute");
                    /* should get from Merge.kif 15034-15041
                     * (=>
                (and
                    (instance ?OBJ Object)
                    (attribute ?OBJ Pliable))
                (exists (?CHANGE)
                    (and
                        (instance ?CHANGE ShapeChange)
                        (patient ?CHANGE ?OBJ))))
                        */
                System.out.println("INFO in HTMLformatter.main(): got intersections: " + forms);
                System.out.println("HTMLformatter.main() ready to call formatFormulaList( )");
                System.out.println(HTMLformatter.formatFormulaList(forms, "", kb, "EnglishLanguage", "SUO-KIF", 0, 0, ""));
            }
        }
    }
}

