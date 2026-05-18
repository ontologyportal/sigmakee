<%
/** This code is copyright Teknowledge (c) 2003, Articulate Software (c) 2003-2017,
    Infosys (c) 2017-present.

    This software is released under the GNU Public License
    <http://www.gnu.org/copyleft/gpl.html>.

    Please cite the following article in any publication with references:

    Pease A., and Benzmüller C. (2013). Sigma: An Integrated Development Environment
    for Logical Theories. AI Communications 26, pp79-97.  See also
    http://github.com/ontologyportal
*/

show = new StringBuilder();
filename = request.getParameter("file");
line = request.getParameter("line");
if (StringUtil.emptyString(lang)) lang = "EnglishLanguage";
if (StringUtil.emptyString(flang)) flang = "SUO-KIF";
nonRelTerm = request.getParameter("nonrelation");
relTerm = StringUtil.emptyString(relTerm) ? request.getParameter("relation") : "";

// ???
if (!StringUtil.emptyString(filename)) {
    int l = 0;
    if (!StringUtil.emptyString(line)) {
        try {l = Integer.parseInt(line);}
        catch (NumberFormatException nfe) {}
    }
    HTMLformatter.launchEditor(filename,l);
}
// Base href link 
HTMLformatter.kbHref = HTMLformatter.createHrefStart() + "/sigma/" + parentPage + "?kb=" + kbName + "&flang=" + flang + "&lang=" + lang;

// If Kb is not empty, and term is empty, and relTerm is empty, and nonRelTerm is empty, show statistics
if (kb != null && StringUtil.emptyString(term) && StringUtil.emptyString(relTerm) && StringUtil.emptyString(nonRelTerm)) show.append(HTMLformatter.showStatistics(kb)).append(HTMLformatter.showLanguageStats(kb,lang));

// Else, if KB doesn't contain term, Show termList
else if (kb != null && term != null && !kb.containsTerm(term)) {
    // If 
    if (StringUtil.hasREchars(term) && StringUtil.isValidRegex(term)) show.append(HTMLformatter.termList(kb.getREMatch(term,true),HTMLformatter.kbHref));
    else if (StringUtil.isValidRegex(".*" + term + ".*")) show.append(HTMLformatter.termList(kb.getREMatch(".*" + term + ".*",true),HTMLformatter.kbHref));
    show.append(HTMLformatter.showNeighborTerms(kb,term));
    // Show WordNet word sense keys
    Map<String,List<String>> wordNetSenseKeys = WordNet.wn.getSenseKeysFromWord(term);
    if (wordNetSenseKeys != null) {
        show.append("<td width=\"10%\"><img src=\"pixmaps/1pixel.gif\" width=\"1\" height=\"1\" border=\"0\"></td>");
        show.append("<td width=\"40%\" valign=\"top\"><small>");
        show.append(WordNetUtilities.formatWordsList(wordNetSenseKeys, kbName));
        // Verbnet verbs
        String verbs = VerbNet.formatVerbsList(wordNetSenseKeys);
        if (!StringUtil.emptyString(verbs)) show.append("<P>" + verbs);
        show.append("</small></td>");
    }
    show.append("</td></table>");
}
else if ((kb != null) && (term == null) && (nonRelTerm != null) && (relTerm != null)) {
    show.append(HTMLformatter.showNeighborTerms(kb,nonRelTerm, relTerm));
    show.append("</td></table>");
}
else if ((kb != null) && (term != null) && kb.containsTerm(term)) {  // Build the HTML format for all the formulas in
    term = kb.simplifyTerm(term, true);

    show.append("<title>Sigma KEE - " + term + "</title>\n");   // which the given term appears.
    show.append("<table width=\"95%\"><tr><td width=\"50%\"><font face=\"Arial,helvetica\" size=\"+3\"><b>");

    term = term.intern();
    show.append(term);
    show.append("</b></font>");
    boolean isArabic = (lang.matches(".*(?i)arabic.*") || lang.equalsIgnoreCase("ar"));
    if (Character.isLowerCase(term.charAt(0)) || term.endsWith("Fn")) {
        Map<String, String> fm = kb.getFormatMap(lang);
        String fmValue = null;
        if (fm != null) fmValue = fm.get(term);
        if (fmValue == null) System.out.println("INFO in BrowseBody.jsp: No format map entry for \"" + term + "\" in language " + lang);
    }
    else {
        Map<String, String> tfm = kb.getTermFormatMap(lang);
        String tfmValue = null;
        if (tfm != null) tfmValue = tfm.get(term);
        if (tfmValue != null) {
            if (isArabic) tfmValue = "<span dir=\"rtl\">" + tfmValue + "</span>";
            show.append("(" + tfmValue + ")");
        }
        // else if (lang != null && lang.equals("EnglishLanguage")) System.out.println("INFO in BrowseBody.jsp: No term format map entry for \"" + term + "\" in language " + lang);
        if (role != null && role.equalsIgnoreCase("admin")) show.append(" [<a href=\"" + HTMLformatter.createHrefStart() + "/sigma/InstFiller.jsp?kb=" + kbName + "&flang=" + flang + "&lang=" + lang + "&term=" + term + "\">assert facts</a>]<br>");
        show.append(HTMLformatter.showMap(kb,term));
        show.append(HTMLformatter.showPictures(kb,term));
        show.append("</td>");
        Map<String,String> tm = WordNet.wn.getWordsFromTerm(term);
        if (tm != null) {
            show.append("<td width=\"10%\"><img src=\"pixmaps/1pixel.gif\" width=\"1\" height=\"1\" border=\"0\"></td>");
            show.append("<td width=\"40%\"><small>");
            if (lang.equals("EnglishLanguage")) show.append(WordNetUtilities.formatWords(tm,kbName));
            else show.append(OMWordnet.formatWords(term,kbName,lang,HTMLformatter.createHrefStart() + "/sigma/"));
            String verbs = VerbNet.formatVerbs(tm);
            if (!StringUtil.emptyString(verbs)) show.append("<P>" + verbs);
            show.append("</small></td>");
        }
        else System.out.println("INFO in BrowseBody.jsp: No synsets for term " + term);
        show.append("</tr></table>\n");
    }
    show.append ("</b></font></td></tr></table>\n");
    int limit = Integer.decode(KBmanager.getMgr().getPref("userBrowserLimit")).intValue();
    if (role != null && !role.equalsIgnoreCase("guest")) limit = Integer.decode(KBmanager.getMgr().getPref("adminBrowserLimit")).intValue();
    for (int arg = 1; arg < 6; arg++) show.append(HTMLformatter.browserSectionFormatLimit(term, "appearance as argument number " + arg, kb, lang,flang,0,limit,arg,"arg"));
    show.append(HTMLformatter.browserSectionFormatLimit(term, "antecedent", kb, lang,flang,0,limit,0,"ant"));
    show.append(HTMLformatter.browserSectionFormatLimit(term, "consequent", kb, lang,flang,0,limit,0,"cons"));
    show.append(HTMLformatter.browserSectionFormatLimit(term, "statement", kb, lang,flang,0,limit,0,"stmt"));
    show.append(HTMLformatter.browserSectionFormatLimit(term, "appearance as argument number 0", kb, lang,flang,0,limit,0,"arg"));
    show.append("<p><table align=\"left\" width=\"50%\"><tr><td bgcolor=\"#A8BACF\">" + "<img src=\"pixmaps/1pixel.gif\" width=\"1\" height=\"1\" border=\"0\"></td></tr>" + "</table><br>\n");
    if (!parentPage.equals("TreeView.jsp")) show.append("\n<small><a href=\"" + HTMLformatter.createHrefStart() + "/sigma/TreeView.jsp" + "?kb=" + kbName + "&flang=" + flang + "&lang=" + lang + "&term=" + term + "\">Show full definition with tree view</a></small><br>\n");
    show.append("\n<small><a href=\"" + HTMLformatter.createHrefStart() + "/sigma/SimpleBrowse.jsp" + "?kb=" + kbName + "&flang=" + flang + "&lang=" + lang + "&simple=yes" + "&term=" + term + "\">Show simplified definition (without tree view)</a></small><br>\n");
    show.append("\n<small><a href=\"" + HTMLformatter.createHrefStart() + "/sigma/TreeView.jsp" + "?kb=" + kbName + "&flang=" + flang + "&lang=" + lang + "&simple=yes" + "&term=" + term + "\">Show simplified definition (with tree view)</a></small><p>\n");
}
%>