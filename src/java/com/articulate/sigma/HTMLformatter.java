package com.articulate.sigma;

/** This code is copyright Articulate Software (c) 2003.  Some portions
copyright Teknowledge (c) 2003 and reused under the terms of the GNU license.
This software is released under the GNU Public License <http://www.gnu.org/copyleft/gpl.html>.
Users of this code also consent, by use of this code, to credit Articulate Software
and Teknowledge in any writings, briefings, publications, presentations, or 
other representations of any software which incorporates, builds on, or uses this 
code.  Please cite the following article in any publication with references:

Pease, A., (2003). The Sigma Ontology Development Environment, 
in Working Notes of the IJCAI-2003 Workshop on Ontology and Distributed Systems,
August 9, Acapulco, Mexico.
*/

import java.util.*;
import java.util.regex.*;
import java.io.*;

 /** A utility class that creates HTML-formatting Strings for various purposes. */
public class HTMLformatter {

    public static String htmlDivider = "<table ALIGN='LEFT' WIDTH='50%'><tr><TD BGCOLOR='#A8BACF'>" + 
        "<IMG SRC='pixmaps/1pixel.gif' width=1 height=1 border=0></TD></tr></table><BR><BR>\n";
    public static String kbHref = ""; // set by BrowseBody.jsp or SimpleBrowseBody.jsp
    public static String language = "EnglishLanguage"; // set by BrowseBody.jsp or SimpleBrowseBody.jsp

    /** *************************************************************
     *  Create the HTML for a single step in a proof.
     */
    public static String proofTableFormat(String query, ProofStep step, String kbName, String language) {

        StringBuffer result = new StringBuffer();
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
            result.append("<td valign=top width=50%>" + "True" + "</td>");
        }
        else
            result.append("<td valign=top width=50%>" + f.htmlFormat(kbHref) + "</td>");
        result.append("<td valign=top width=10%>");
        for (int i = 0; i < step.premises.size(); i++) {
            Integer stepNum = (Integer) step.premises.get(i);
            result.append(stepNum.toString() + " ");            
        }
        if (step.premises.size() == 0) {
            if (Formula.isNegatedQuery(query,f.theFormula)) 
                result.append("[Negated Query]");
            else  
                result.append("[KB]");
        }
        result.append("</td><td width=40% valign=top>");
        if (language != null && language.length() > 0) {
	    String pph = LanguageFormatter.htmlParaphrase(kbHref,f.theFormula, 
                            KBmanager.getMgr().getKB(kbName).getFormatMap(language), 
                            KBmanager.getMgr().getKB(kbName).getTermFormatMap(language), kb,
                            language);
	    if (pph == null)
                pph = "";
            if (Formula.isNonEmptyString(pph)) {
                if (language.equalsIgnoreCase("ar")) {
                    pph = ("<span dir=\"rtl\">" + pph + "</span>");
                    // pph = ("&#x202b;" + pph + "&#x202c;");
                }
            }
            result.append(pph);        
	}
        result.append("</td>");
        return result.toString();
    }

    /** *************************************************************
     *  Show a hyperlinked list of terms.
     */
    public static String termList(ArrayList terms, String kbHref) {

        StringBuffer show = new StringBuffer();
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

        StringBuffer show = new StringBuffer();
        show.append("<b>Knowledge base statistics: </b><br><table>");
        show.append("<tr bgcolor=#eeeeee><td>Total Terms</td><td>Total Axioms</td><td>Total Rules</td><tr><tr align='center'>\n");
        show.append("<td>  " + kb.getCountTerms());
        show.append("</td><td> " + kb.getCountAxioms());
        show.append("</td><td> " + kb.getCountRules());
        show.append("</td><tr> </table><P>\n");
        show.append("Relations: " + kb.getCountRelations());
        show.append("<P>\n");
        return show.toString();
    }

    /** *************************************************************
     *  Show knowledge base statements containing the given term at
     * different argument positions.
     */
    public static String showFormulas(KB kb, String term) {

        return showFormulasLimit(kb,term,0,25);
    }

    /** *************************************************************
     *  Show knowledge base statements containing the given term at
     * different argument positions.
     */
    public static String showFormulasLimitArg(KB kb, String term, int start, int limit, int arg, String type) {

        StringBuffer show = new StringBuffer();
       
        int localLimit = start + limit;
        String limitString = "";
        ArrayList forms = kb.ask(type,arg,term);
        if (forms != null && KBmanager.getMgr().getPref("showcached").equalsIgnoreCase("no")) 
            forms = TaxoModel.removeCached(forms);
        if (forms != null && forms.size() > 0) {
            Collections.sort(forms);
            show.append("<br><b>&nbsp;appearance as argument number " + (new Integer(arg)).toString() + "</B>");
            show.append(htmlDivider + "<TABLE width='95%'>");

            if (forms.size() < localLimit) 
                localLimit = forms.size();
            else {
                limitString = "<tr><td><br></td></tr><tr><td>Display limited to " + 
                        (new Integer(limit)).toString() + " items. " +
                        "<a href=\"BrowseExtra.jsp?term=" + term + "&kb=" + kb.name + 
                        "&start=" + (new Integer(start+limit)).toString() + "&arg=" + (new Integer(arg)).toString() +                       
                        "&type=" + type + "\">Show next " + (new Integer(limit)).toString() + "</a></td></tr>\n";
            }
            for (int i = start; i < localLimit; i++) {
                Formula f = (Formula) forms.get(i);
                if (KBmanager.getMgr().getPref("showcached").equalsIgnoreCase("yes") ||
                    !f.sourceFile.endsWith(KB._cacheFileSuffix) ) {
                    show.append("<TR><TD WIDTH='50%' valign=top>");
                    String formattedFormula = f.htmlFormat(kbHref) + "</td>\n<TD width='10%' valign=top BGCOLOR=#B8CADF>";
                    if (f.theFormula.length() > 14 && f.theFormula.substring(1,14).compareTo("documentation") == 0)
                        show.append(kb.formatDocumentation(kbHref,formattedFormula));
                    else
                        show.append(formattedFormula);
                    String sourceFilename = f.sourceFile.substring(f.sourceFile.lastIndexOf(File.separator) + 1,f.sourceFile.length());
                    show.append("<A href=\"EditFile.jsp?file=" + f.sourceFile + "&line=");
                    show.append((new Integer(f.startLine)).toString() + "\">");
                    show.append(sourceFilename);
                    show.append(" " + (new Integer(f.startLine)).toString() + "-" + (new Integer(f.endLine)).toString());
                    show.append("</A>");
                    show.append("</TD>\n<TD width='40%' valign=top>");
                    String pph = "";
                    if (f.theFormula.substring(1,14).compareTo("documentation") == 0 || f.theFormula.substring(1,7).compareTo("format") == 0) 
                        show.append("</TD></TR>\n");		    
                    else 
                        pph = LanguageFormatter.htmlParaphrase(kbHref,f.theFormula,kb.getFormatMap(language),kb.getTermFormatMap(language),kb,language);
                    if (pph == null)
                        pph = "";
                    if (Formula.isNonEmptyString(pph)) {
                        if (language.equalsIgnoreCase("ar")) {
                            pph = ("<span dir=\"rtl\">" + pph + "</span>");
                            // pph = ("&#x202b;" + pph + "&#x202c;");
                        }
                    }
                    show.append(pph + "<br></TD></TR>\n");
                }                
            }
            show.append(limitString);
            show.append("</TABLE>\n");
        }        
        return show.toString();
    }

    /** *************************************************************
     *  Show knowledge base statements containing the given term at
     * different argument positions.
     */
    public static String showFormulasLimit(KB kb, String term, int start, int limit) {

        StringBuffer show = new StringBuffer();
       
        for (int arg = 1; arg < 6; arg++)
            show.append(showFormulasLimitArg(kb,term,start,limit,arg,"arg"));       
        return show.toString();
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

        StringBuffer show = new StringBuffer();
        ArrayList pictures = kb.askWithRestriction(0,"externalImage",1,term);   // Handle picture diplay
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
                                imageFile + "\"><img width=100 src=" + url + "></a>\n" );
                }
                else 
                    show.append("<a href=" + url + "><img width=100 src=" + url + "></a>\n");                
            }
            if (more) 
                show.append("<a href=\"AllPictures.jsp?term=" + term + "&kb=" + kb.name + "\">more pictures...</a>");            
        }
        return show.toString();
    }

    /** *************************************************************
     *  Show alphabetic list of neighbor terms
     */
    public static String showNeighborTerms(KB kb, String term) {

        StringBuffer show = new StringBuffer();
        ArrayList relations = kb.getNearestRelations(term);     // that is not present in the KB.
        ArrayList nonRelations = kb.getNearestNonRelations(term);
        show.append(" <FONT face='Arial,helvetica' size=+3> <b> ");
        if (term != null)
            show.append(term);
        show.append("</b></FONT><br><br>");
        show.append("<TABLE><tr><td>");
        show.append("<TABLE>");
        for (int i = 0; i < 30; i++) {
            String relation = (String) relations.get(i);
            String nonRelation = (String) nonRelations.get(i);
            if (relation != "" || nonRelation != "") {
                if (i == 15) {
                    show.append("<TR>\n");
                    show.append("  <TD><A href=\""+ kbHref +"&term=");
                    show.append(   relation + "\">" + relation + "</A>" + "</TD><TD>&nbsp;&nbsp;</TD>\n");
                    show.append("  <TD><A href=\""+ kbHref +"&term=");
                    show.append(   nonRelation + "\">" + nonRelation + "</A>" + "</TD>\n");
                    show.append("</TR>\n");
                    show.append("<TR><TD><FONT SIZE=4 COLOR=\"RED\">" + term + " </FONT></TD><TD>&nbsp;&nbsp;</TD>\n");
                    show.append("<TD><FONT SIZE=4 COLOR=\"RED\">" + term + " </FONT></TD></TR>\n");
                }
                else {
                    show.append("<TR>\n");
                    show.append("  <TD><A href=\""+ kbHref +"&term=");
                    show.append(   relation + "\">" + relation + "</A>" + "</TD><TD>&nbsp;&nbsp;</TD>\n");
                    show.append("  <TD><A href=\""+ kbHref +"&term=");
                    show.append(   nonRelation + "\">" + nonRelation + "</A>" + "</TD>\n");
                    show.append("</TR>\n");
                }
            }
        }
        show.append("</TABLE></td>");
        return show.toString();
    }

    /** *************************************************************
     *  Show a hyperlinked list of term mappings from WordNet.
     */
    public static String termMappingsList(String terms, String kbHref) {

        StringBuffer result = new StringBuffer();
        String[] sumoList = terms.split("\\s+");
        result.append("<P><ul><li>\tSUMO Mappings:  ");
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

        StringBuffer show = new StringBuffer();
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
    public static String browserSectionFormatLimit(String term, ArrayList forms, String header, 
                                              KB kb, String language, int start, int limit, int arg, String type) {

        StringBuffer show = new StringBuffer();
        String limitString = "";
        int localLimit = start + limit;

        if (forms != null && forms.size() > 0) {
            Collections.sort(forms);
            if (header != null) 
                show.append("<br><b>&nbsp;" + header + "</B>");
            if (htmlDivider != null) 
                show.append(htmlDivider);
            show.append("<TABLE width=95%%>");
	    String pph = null;
            if (forms.size() < localLimit) 
                localLimit = forms.size();
            else {
                limitString = "<tr><td><br></td></tr><tr><td>Display limited to " + 
                        (new Integer(limit)).toString() + " items. " +
                        "<a href=\"BrowseExtra.jsp?term=" + term + "&kb=" + kb.name + 
                        "&start=" + (new Integer(start+limit)).toString() + "&arg=" + (new Integer(arg)).toString() +
                        "&type=" + type + "\">Show next " + (new Integer(limit)).toString() + "</a></td></tr>\n";
            }
            
            for (int i = start; i < localLimit; i++) {
                Formula f = (Formula) forms.get(i);
                show.append("<TR><TD width=50%% valign=top>");
                show.append(f.htmlFormat(kbHref) + "</td>\n<TD width=10%% valign=top BGCOLOR=#B8CADF>");
                show.append(f.sourceFile.substring(f.sourceFile.lastIndexOf(File.separator) + 1,f.sourceFile.length()) + 
                            " " + (new Integer(f.startLine)).toString() + 
                            "-" + (new Integer(f.endLine)).toString() + "</TD>\n<TD width=40%% valign=top>");
		pph = LanguageFormatter.htmlParaphrase(kbHref,
    			 f.theFormula,kb.getFormatMap(language), 
                         kb.getTermFormatMap(language), kb, language);
		if (pph == null) 
                    pph = ""; 
                if (Formula.isNonEmptyString(pph)) {
                    if (language.equalsIgnoreCase("ar")) {
                        pph = ("<span dir=\"rtl\">" + pph + "</span>");
                        // pph = ("&#x202b;" + pph + "&#x202c;");
                    }
                }
                show.append(pph + "</TD></TR>\n"); 
            }
            show.append(limitString);
            show.append("</TABLE>\n");
        }     
        return show.toString();
    }

    /** *************************************************************
     *  Create the HTML for a section of the Sigma term browser page.
     */
    public static String browserSectionFormat(String term, ArrayList forms, String header, 
                                              KB kb, String language, int arg, String type) {

        return browserSectionFormatLimit(term, forms, header,kb, language, 0, 50, arg,type);
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
     *  Create an HTML menu, given an ArrayList of Strings where the
     *  value(s) are String representations of int(s) but the displayed
     *  menu items are String(s).
     */
    public static String createNumberedMenu(String menuName, String selectedOption, ArrayList options) {

        StringBuffer result = new StringBuffer();

        String menuNameProcessed = encodeForURL(menuName);
        result.append("<select name=" + menuNameProcessed);
        result.append(">\n  ");
        for (int i = 0; i < options.size(); i++) {
            result.append("<option value='");
            String menuItem = (String) options.get(i);
            String menuItemProcessed = encodeForURL(menuItem);
            result.append(Integer.toString(i));
            if (selectedOption != null && selectedOption.equalsIgnoreCase(Integer.toString(i))) 
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
     *  Create an HTML menu, given an ArrayList of Strings.
     */
    public static String createMenu(String menuName, String selectedOption, ArrayList options) {
        String params = null;
        return createMenu(menuName, selectedOption, options, params);
    }


    /** *************************************************************
     *  Create an HTML menu with an ID, given an ArrayList of Strings.
     */
    public static String createMenu(String menuName, String selectedOption, ArrayList options, String params) {

        StringBuffer result = new StringBuffer();

        String menuNameProcessed = encodeForURL(menuName);
        result.append("<select name=" + menuNameProcessed);
        if (params != null) {
            result.append(" " + params + " ");
        }
        result.append(">\n  ");
        for (int i = 0; i < options.size(); i++) {
            result.append("<option value='");
            String menuItem = (String) options.get(i);
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

	// System.out.println( "INFO in HTMLformatter.formatProofResult(): " + result );

        StringBuffer html = new StringBuffer();
        if (result != null && result.toString().length() > 0) {
            BasicXMLparser res = new BasicXMLparser(result.toString());

            // System.out.print("INFO in AskTell.jsp: Number of XML elements: ");
            // System.out.println(res.elements.size());

            ProofProcessor pp = new ProofProcessor(res.elements);
            for (int i = 0; i < pp.numAnswers(); i++) {
                ArrayList proofSteps = pp.getProofSteps(i);
                proofSteps = new ArrayList(ProofStep.normalizeProofStepNumbers(proofSteps));

                // System.out.print("Proof steps: ");
                // System.out.println(proofSteps.size());

                if (i != 0) 
                    html = html.append(lineHtml + "\n");
                html = html.append("Answer " + "\n");
                html = html.append(i+1);
                html = html.append(". " + pp.returnAnswer(i) + "\n");
                if (!pp.returnAnswer(i).equalsIgnoreCase("no")) {
                    html = html.append("<P><TABLE width=95%%>" + "\n");
                    for (int j = 0; j < proofSteps.size(); j++) {

                        // System.out.print("Printing proof step: ");

                        // System.out.println(j);
                        html = html.append("<TR>" + "\n");
                        html = html.append("<TD valign=top>" + "\n");
                        html = html.append(j+1);
                        html = html.append(". </TD>" + "\n");
                        html = html.append(HTMLformatter.proofTableFormat(stmt,(ProofStep) proofSteps.get(j), kbName, language) + "\n");                       
                        // System.out.println(HTMLformatter.proofTableFormat(stmt,(ProofStep) proofSteps.get(j), kbName, language));                       
			
                        html = html.append("</TR>\n" + "\n");
                    }
                    html = html.append("</TABLE>" + "\n");
                }
            }
        }
        return html.toString();
    }
}

