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

    public static String header = "";
    public static String TOCheader = "";
    public static String footer = "www.rightscom.com";
    public static boolean simplified = false;   // false = do not display termFormat 
                                                // expressions in place of term names

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
     *  If the given term is an instance, and is an instance of the
     *  term with the headword "Composite".
     */
    public static boolean isCompositeInstance(KB kb, String term) {

        //System.out.println("INFO in DocGen.isCompositeInstance(): term: " + term);
        //ArrayList headwords = kb.askWithRestriction(0,"hasHeadword",2,"\"Composite\"");
        //System.out.println("headwords " + headwords);
        //if (headwords != null && headwords.size() > 0) {
            //Formula f = (Formula) headwords.get(0);
            //String composite = f.getArgument(1);
            // 
            // Note that we could try to find the composite by doing an ask on
            // hasHeadword and then searching for "Composite" but KIF.parse doesn't
            // index on Strings so we can't use kb.askWithRestrictions()
            String composite = "Ddex_Composite";
            //System.out.println("INFO in DocGen.isCompositeInstance(): composite: " + composite);
            ArrayList instances = kb.askWithRestriction(0,"instance",1,term);
            if (instances != null) 
                return (Diagnostics.findParent(kb,term,composite) &&
                    instances.size() > 0);
            //return false;
        //}
        return false;
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
     *  Generate an alphabetic HTML list that points to the
     *  individual index pages (which collect all terms starting
     *  with a particular letter.
     */
    private static String generateDynamicTOCHeader(String kbHref) {

        StringBuffer result = new StringBuffer();
        result.append("<table width=\"100%\"><tr>");
        for (char c = 65; c < 91; c++) {
            String cString = Character.toString(c);   
            result.append("<td valign=top><a href=\"" + kbHref + "&term=" + cString + "*\">" + cString + "</a></td>\n");            
        }
        result.append("</tr></table>");
        return result.toString();
    }

    /** ***************************************************************
     */
    private static String createDocs(KB kb, String kbHref, String term, String language) {

        StringBuffer result = new StringBuffer();
        ArrayList docs = kb.askWithRestriction(0,"documentation",1,term);
        if (docs != null && docs.size() > 0) {
            Formula f = (Formula) docs.get(0);
            String docString = f.getArgument(3);  
            docString = kb.formatDocumentation(kbHref,docString,language);
            docString = removeEnclosingQuotes(docString);   
            docString = docString.replace("\\\"","\"");
            result.append("<td class=\"description\">" + docString + "</td></tr>\n");
        }
        return result.toString();
    }

    /** ***************************************************************
     */
    private static String createComments(KB kb, String kbHref, String term, String language) {

        StringBuffer result = new StringBuffer();
        ArrayList docs = kb.askWithRestriction(0,"comment",1,term);
        if (docs != null && docs.size() > 0) {
            result.append("<tr><td class=\"label\">Comments</td>");
            for (int i = 0; i < docs.size(); i++) {
                Formula f = (Formula) docs.get(i);
                String docString = f.getArgument(3);  
                docString = kb.formatDocumentation(kbHref,docString,language);
                docString = removeEnclosingQuotes(docString);   
                docString = docString.replace("\\\"","\"");
                if (i > 0) result.append("<tr><td>&nbsp;</td>");
                result.append("<td colspan=2 class=\"cell\">" + docString + "</td></tr>\n");
            }
        }
        return result.toString();
    }

    /** ***************************************************************
     */
    private static String createSynonyms(KB kb, String kbHref, String term) {

        StringBuffer result = new StringBuffer();
        ArrayList syn = kb.askWithRestriction(0,"termFormat",2,term);
        boolean found = false;
        if (syn != null && syn.size() > 0) {
            for (int i = 0; i < syn.size(); i++) {
                Formula f = (Formula) syn.get(i);
                String namespace = footer.getArgument(1);
                if (namespace.endsWith("syn") {
                    if (!found) 
                        result.append("<b>Synonym(s)</b></td><td class=\"cell\"><i>");
                    String s = f.getArgument(3); 
                    s = removeEnclosingQuotes(s);
                    if (found) result.append(", ");                
                    result.append("<i>" + s + "</i>");
                    found = true;
                }
            }
            result.append("</td></tr>\n");
        }
        return result.toString();
    }

    /** ***************************************************************
     */
    private static String createHasSameComponents(KB kb, String kbHref, String term, String language) {

        String suffix = "";
        if (DB.emptyString(kbHref)) 
            suffix = ".html";
        StringBuffer result = new StringBuffer();
        ArrayList syn = kb.askWithRestriction(0,"isXmlExtensionOf",1,term);
        if (syn != null && syn.size() > 0) {
            result.append("<tr><td class=\"label\">Has Same Components As</td>");
            for (int i = 0; i < syn.size(); i++) {
                Formula f = (Formula) syn.get(i);
                String s = f.getArgument(2); 
                String termHref = "<a href=\"" + kbHref + s + suffix + "\">" + showTermName(kb,s,language) + "</a>";
                if (i > 0) result.append("<tr><td>&nbsp;</td>");
                result.append("<td class=\"cell\">" + termHref + "</td></tr>\n");
            }
        }
        return result.toString();
    }

    /** ***************************************************************
     */
    private static String createUsingSameComponents(KB kb, String kbHref, String term, String language) {

        String suffix = "";
        if (DB.emptyString(kbHref)) 
            suffix = ".html";
        StringBuffer result = new StringBuffer();
        ArrayList syn = kb.askWithRestriction(0,"isXmlExtensionOf",2,term);
        if (syn != null && syn.size() > 0) {
            result.append("<tr><td class=\"label\">Composites Using Same Components</td>");
            for (int i = 0; i < syn.size(); i++) {
                Formula f = (Formula) syn.get(i);
                String s = f.getArgument(1); 
                String termHref = "<a href=\"" + kbHref + s + suffix + "\">" + showTermName(kb,s,language) + "</a>";
                if (i > 0) result.append("<tr><td>&nbsp;</td>");
                result.append("<td class=\"cell\">" + termHref + "</td></tr>\n");
            }
        }
        return result.toString();
    }

    /** ***************************************************************
     */
    private static String createParents(KB kb, String kbHref, String term, String language) {

        String suffix = "";
        if (DB.emptyString(kbHref)) 
            suffix = ".html";
        StringBuffer result = new StringBuffer();
        ArrayList forms = kb.askWithRestriction(0,"subclass",1,term);
        if (forms != null && forms.size() > 0) {
            result.append("<tr><td class=\"label\">Parents</td>");
            for (int i = 0; i < forms.size(); i++) {
                Formula f = (Formula) forms.get(i);
                if (!f.sourceFile.endsWith(KB._cacheFileSuffix)) {
                    String s = f.getArgument(2); 
                    String termHref = "<a href=\"" + kbHref + s + suffix + "\">" + showTermName(kb,s,language) + "</a>";
                    if (i > 0) result.append("<tr><td>&nbsp;</td>");                
                    result.append("<td class=\"cell\">" + termHref + "</td>");
                    ArrayList docs = kb.askWithRestriction(0,"documentation",1,s);
                    if (docs != null && docs.size() > 0) {
                        f = (Formula) docs.get(0);
                        String docString = f.getArgument(3);  
                        docString = kb.formatDocumentation(kbHref,docString,language);
                        docString = removeEnclosingQuotes(docString);   
                        docString = docString.replace("\\\"","\"");
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
    private static String createChildren(KB kb, String kbHref, String term, String language) {

        String suffix = "";
        if (DB.emptyString(kbHref)) 
            suffix = ".html";
        StringBuffer result = new StringBuffer();
        ArrayList forms = kb.askWithRestriction(0,"subclass",2,term);
        if (forms != null && forms.size() > 0) {
            result.append("<tr><td class=\"label\">Children</td>");
            for (int i = 0; i < forms.size(); i++) {
                Formula f = (Formula) forms.get(i);
                if (!f.sourceFile.endsWith(KB._cacheFileSuffix)) {
                    String s = f.getArgument(1); 
                    String termHref = "<a href=\"" + kbHref + s + suffix + "\">" + showTermName(kb,s,language) + "</a>";
                    if (i > 0) result.append("<tr><td>&nbsp;</td>");                
                    result.append("<td class=\"cell\">" + termHref + "</td>");
                    ArrayList docs = kb.askWithRestriction(0,"documentation",1,s);
                    if (docs != null && docs.size() > 0) {
                        f = (Formula) docs.get(0);
                        String docString = f.getArgument(3);  
                        docString = kb.formatDocumentation(kbHref,docString,language);
                        docString = removeEnclosingQuotes(docString);   
                        docString = docString.replace("\\\"","\"");
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
    private static String createInstances(KB kb, String kbHref, String term, String language, ArrayList excluded) {

        String suffix = "";
        if (DB.emptyString(kbHref)) 
            suffix = ".html";
        StringBuffer result = new StringBuffer();
        ArrayList forms = kb.askWithRestriction(0,"instance",2,term);
        if (forms != null && forms.size() > 0) {
            for (int i = 0; i < forms.size(); i++) {
                Formula f = (Formula) forms.get(i);
                if (!f.sourceFile.endsWith(KB._cacheFileSuffix)) {
                    String s = f.getArgument(1); 
                    if (!excluded.contains(s)) {
                        if (i == 0)                         
                            result.append("<tr><td class=\"label\">Instances</td>");
                        String displayName = showTermName(kb,s,language);
                        String xmlName = "";
                        if (displayName.equals(s)) 
                            xmlName = showTermName(kb,s,"XMLLabel");
                        if (!DB.emptyString(xmlName)) 
                            displayName = xmlName;
                        String termHref = "<a href=\"" + kbHref + s + suffix + "\">" + displayName + "</a>";
                        if (i > 0) result.append("<tr><td>&nbsp;</td>");                
                        result.append("<td class=\"cell\">" + termHref + "</td>");
                        ArrayList docs = kb.askWithRestriction(0,"documentation",1,s);
                        if (docs != null && docs.size() > 0) {
                            f = (Formula) docs.get(0);
                            String docString = f.getArgument(3);  
                            docString = kb.formatDocumentation(kbHref,docString,language);
                            docString = removeEnclosingQuotes(docString);   
                            docString = docString.replace("\\\"","\"");
                            result.append("<td class=\"cell\">" + docString + "</td>");
                        }
                        result.append("</tr>\n");
                    }
                }
            }
        }
        return result.toString();
    }

    /** ***************************************************************
     */
    private static String createRelations(KB kb, String kbHref, String term, String language) {

        String suffix = "";
        if (DB.emptyString(kbHref)) 
            suffix = ".html";
        StringBuffer result = new StringBuffer();
        ArrayList relations = getRelations(kb);
        boolean firstLine = true;
        for (int i = 0; i < relations.size(); i++) {
            String relation = (String) relations.get(i);
            // System.out.println("INFO in DocGen.createRElations(): relation: " + relation);
            if (!relation.equals("subclass") && !relation.equals("instance") &&
                !relation.equals("documentation")) {
                String relnHref = "<a href=\"" + kbHref + relation + suffix + "\">" + showTermName(kb,relation,language) + "</a>";
                ArrayList statements = kb.askWithRestriction(0,relation,1,term);
                for (int j = 0; j < statements.size(); j++) {
                    Formula f = (Formula) statements.get(j);
                    if (!f.sourceFile.endsWith(KB._cacheFileSuffix)) {
                        String s = f.getArgument(2); 
                        String termHref = "<a href=\"" + kbHref + s + suffix + "\">" + showTermName(kb,s,language) + "</a>";
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
     */
    public static String indentChars(String c, int indent) {

        StringBuffer result = new StringBuffer();
        for (int i = 0; i < indent; i++) {
            result.append(c);
        }
        return result.toString();
    }

    /** ***************************************************************
     */
    private static String showCardinalityCell(KB kb, String kbHref, String term, String context) {

        ArrayList cardForms = kb.askWithRestriction(0,"exactCardinality",2,term);
        if (cardForms != null && cardForms.size() > 0) {
            Formula f = (Formula) cardForms.get(0);
            if (context == "" || context.equals(f.getArgument(1)))             
                return f.getArgument(3);
        }
        else {
            String minCard = "0";
            String maxCard = "n";
            StringBuffer result = new StringBuffer();
            cardForms = kb.askWithRestriction(0,"minCardinality",2,term);
            if (cardForms != null && cardForms.size() > 0) {
                Formula f = (Formula) cardForms.get(0);
                if (context == "" || context.equals(f.getArgument(1)))             
                    minCard = f.getArgument(3);
            }
            cardForms = kb.askWithRestriction(0,"maxCardinality",2,term);
            if (cardForms != null && cardForms.size() > 0) {
                Formula f = (Formula) cardForms.get(0);
                if (context == "" || context.equals(f.getArgument(1)))             
                    maxCard = f.getArgument(3);
            }
            return minCard + "-" + maxCard;
        }
        return "";
    }

    /** ***************************************************************
     */
    private static String createCompositeComponentLine(KB kb, String kbHref, String term, int indent, String language) {

        String suffix = "";
        if (DB.emptyString(kbHref)) 
            suffix = ".html";
        StringBuffer result = new StringBuffer();
        result.append("<tr><td></td><td class=\"cell\">");
        String parentClass = "";;
        ArrayList instanceForms = kb.askWithRestriction(0,"instance",1,term);
        if (instanceForms != null && instanceForms.size() > 0) {
            Formula f = (Formula) instanceForms.get(0);
            parentClass = f.getArgument(2);
        }
        ArrayList termForms = kb.askWithTwoRestrictions(0,"termFormat",1,"XMLLabel",2,term);
        if (termForms != null) {
            for (int i = 0; i < termForms.size(); i++) {               
                Formula f = (Formula) termForms.get(i);
                result.append(indentChars("&nbsp;&nbsp;",indent));
                String termFormat = f.getArgument(3);
                termFormat = termFormat.substring(1,termFormat.length()-1);
                result.append("<a href=\"" + kbHref + parentClass + suffix + "\">" + termFormat + "</a>");                      
            }
        }
        result.append("</td><td class=\"cell\">");
        ArrayList docForms = kb.askWithRestriction(0,"documentation",1,term);
        if (docForms != null && docForms.size() > 0) {
            Formula f = (Formula) docForms.get(0);
            String docString = f.getArgument(3);
            docString = kb.formatDocumentation(kbHref,docString,language);
            docString = removeEnclosingQuotes(docString);
            docString = docString.replace("\\\"","\"");
            result.append(docString); 
        }
        result.append("</td><td class=\"cell\">");
        if (indent > 0)        
            result.append(showCardinalityCell(kb,kbHref,term,""));
        result.append("</td><td class=\"cell\">");
        ArrayList forms = kb.askWithRestriction(0,"dataType",1,term);
        if (forms != null && forms.size() > 0) {
            Formula f = (Formula) forms.get(0);
            String dataTypeName = f.getArgument(2);
            dataTypeName = showTermName(kb,dataTypeName,language);                
            result.append("<a href=\"" + kbHref + f.getArgument(2) + suffix + "\">" + dataTypeName + "</a>");        
        }
        result.append("</td></tr>\n");
        return result.toString();
    }

    /** ***************************************************************
     *  Remove the quotes in the first and last character of a
     *  String, if present.
     */
    public static String removeEnclosingQuotes(String s) {

        if (!DB.emptyString(s) && s.charAt(0) == '"' && s.charAt(s.length()-1) == '"') 
            return s.substring(1,s.length()-1);    
        else
            return s;
    }

    /** ***************************************************************
     *  If a term has a termFormat in the given language, display
     *  that, otherwise, show its termFormat for English, otherwise
     *  just display the term name.
     */
    public static String showTermName(KB kb, String term, String language) {

        HashMap synonyms = kb.getTermFormatMap(language);
        String syn = (String) synonyms.get(term);
        if (DB.emptyString(syn)) {
            synonyms = kb.getTermFormatMap("EnglishLanguage");
            syn = (String) synonyms.get(term);
            if (DB.emptyString(syn)) 
                return(term);
            else {
                syn = removeEnclosingQuotes(syn);
                return(syn);
            }
        }
        else {
            syn = removeEnclosingQuotes(syn);
            return(syn);
        }
    }

    /** ***************************************************************
     */
    private static String formatCompositeHierarchy(KB kb, String kbHref, ArrayList hier, String language) {

        StringBuffer result = new StringBuffer();
        for (int i = 0; i < hier.size(); i++) {
            AVPair avp = (AVPair) hier.get(i);
            result.append(createCompositeComponentLine(kb,kbHref,avp.attribute,Integer.parseInt(avp.value),language));
        }
        return result.toString();
    }

    /** ***************************************************************
     *  Don't display XmlChoice(s) or XmlSequence(s)
     */
    private static ArrayList createCompositeRecurse(KB kb, String term, int indent) {

        ArrayList result = new ArrayList();
        AVPair avp = new AVPair();
        avp.attribute = term;
        avp.value = Integer.toString(indent);
        result.add(avp);
        ArrayList forms = kb.askWithRestriction(0,"hasXmlAttribute",1,term);
        if (forms != null) {
            for (int i = 0; i < forms.size(); i++) {
                Formula form = (Formula) forms.get(i);
                String t = form.getArgument(2);
                result.addAll(createCompositeRecurse(kb,t,indent+1));  // This should return without children since
                                                                      // attributes don't have child elements
            }
        }
        forms = kb.askWithRestriction(0,"hasXmlElement",1,term);
        if (forms != null) {
            for (int i = 0; i < forms.size(); i++) {
                Formula form = (Formula) forms.get(i);
                String t = form.getArgument(2);
                result.addAll(createCompositeRecurse(kb,t,indent+1));
            }
        }
        return result;
    }

    /** ***************************************************************
     */
    private static String createContainingCompositeComponentLine(KB kb, String kbHref, String containingComp,
                                                                 String instance, int indent, String language) {

        String suffix = "";
        if (DB.emptyString(kbHref)) 
            suffix = ".html";
        StringBuffer result = new StringBuffer();
        ArrayList docForms = kb.askWithRestriction(0,"documentation",1,instance);
        if (docForms != null && docForms.size() > 0) {
            for (int i = 0; i < docForms.size(); i++) {
                Formula f = (Formula) docForms.get(i);
                String context = f.getArgument(2);
                if (context.equals(containingComp)) {
                    result.append("<tr><td></td><td class=\"cell\">");
                    result.append("<a href=\"" + kbHref  + containingComp + suffix + "\">" + 
                                  showTermName(kb,containingComp,language) + "</a>");        
                    result.append("</td><td class=\"cell\">");
                    String docString = f.getArgument(3);
                    docString = kb.formatDocumentation(kbHref,docString,language);
                    docString = removeEnclosingQuotes(docString);
                    docString = docString.replace("\\\"","\"");
                    result.append(docString);                
                    result.append("</td><td class=\"cell\">");
                    result.append(showCardinalityCell(kb,kbHref,instance,context));
                    result.append("</td><td>");
                    result.append("</td></tr>\n");
                }
            }
        }
        return result.toString();
    }

    /** ***************************************************************
     *  Given the SUO-KIF statements:
     * 
     * (hasXmlElement DdexC_PartyDescriptor DDEX_LocalInstance2_459)
     * (dataType DDEX_LocalInstanceLeaf2_459 DdexC_PartyId)
     * (documentation DDEX_LocalInstanceLeaf2_459
     * DdexC_PartyDescriptor "A Composite containing details...")
     * 
     * show DdexC_PartyDescriptor as one of the "containing
     * composites" of DdexC_PartyID, and show the documentation for
     * the instance node next to the parent composite.
     */
    private static String formatContainingComposites(KB kb, String kbHref, 
                                                     ArrayList containing, String composite, String language) {

        StringBuffer result = new StringBuffer();
        ArrayList dataTypes = kb.askWithRestriction(0,"dataType",2,composite);
        if (dataTypes != null) {
            for (int j = 0; j < dataTypes.size(); j++) {
                Formula f = (Formula) dataTypes.get(j);
                String instance = f.getArgument(1);
                for (int i = 0; i < containing.size(); i++) {
                    String containingComp = (String) containing.get(i);
                    result.append(createContainingCompositeComponentLine(kb,kbHref,containingComp,
                                                                         instance,0,language));
                }
            }
        }
        return result.toString();
    }

    /** ***************************************************************
     *  Travel up the hasXmlElement relation hierarchy to collect
     *  all parents.
     */
    private static ArrayList containingComposites(KB kb, String term) {

        ArrayList result = new ArrayList();
        ArrayList instances = kb.askWithRestriction(0,"hasXmlElement",2,term);
        if (instances != null) {
            for (int i = 0; i < instances.size(); i++) {
                Formula form = (Formula) instances.get(i);
                String t = form.getArgument(1);
                if (isCompositeInstance(kb,t)) {
                    result.add(t);
                }
                result.addAll(containingComposites(kb,t));
            }
        }
        return result;
    }

    /** ***************************************************************
     */
    private static String createBelongsToClass(KB kb, String kbHref, String term, String language) {

        String suffix = "";
        if (DB.emptyString(kbHref)) 
            suffix = ".html";
        StringBuffer result = new StringBuffer();
        ArrayList forms = kb.askWithRestriction(0,"instance",1,term);
        if (forms != null && forms.size() > 0) {
            result.append("<tr><td class=\"label\">Belongs to class</td><td class=\"cell\">\n");
            for (int i = 0; i < forms.size(); i++) {
                Formula form = (Formula) forms.get(i);
                String cl = form.getArgument(2);
                if (i > 0) 
                    result.append("<br>");
                result.append("<a href=\"" + kbHref + cl + suffix + "\">" + showTermName(kb,cl,language) + "</a>");        
            }
            result.append("</td><td></td><td></td><td></td></tr>\n");
        }
        return result.toString();
    }

    /** ***************************************************************
     *  Iterate through all the &%dataType relations for the
     *  composite to collect the instances of this composite.  Then
     *  call containingComposite() travel up the hasXmlElement
     *  relations for those instances to find their containing
     *  composites (if any).
     */
    private static ArrayList findContainingComposites(KB kb, String term) {

        ArrayList result = new ArrayList();
        ArrayList dataTypes = kb.askWithRestriction(0,"dataType",2,term);
        if (dataTypes != null) {
            for (int i = 0; i < dataTypes.size(); i++) {
                Formula form = (Formula) dataTypes.get(i);
                String t = form.getArgument(1);
                ArrayList comps = containingComposites(kb,t);
                if (comps != null) {
                    for (int j = 0; j < comps.size(); j++) {
                        String comp = (String) comps.get(j);
                        if (!result.contains(comp)) {
                            result.add(comp);
                        }
                    }
                }
            }
        }
        return result;
    }

    /** ***************************************************************
     * Create an HTML page that lists information about a particular term,
     * with a limit on how many statements of each type should be
     * displayed.
     */
    public static String createAllStatements(KB kb, String kbHref, String term, 
                                    int limit) {

        StringBuffer result = new StringBuffer();

        String language = "EnglishLanguage";
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
        if (result.length() > 0) {                  // note that the following 3 lines are inserted in reverse order
            result.insert(0,"</td></tr></table><P>");
            result.insert(0,"<tr><td class=\"cell\">These statements express (potentially complex) facts about the term, " +
                          "and are automatically generated.</td></tr>\n<tr><td class=\"cell\">");
            result.insert(0,"<P><table><tr><td class=\"label\"><b>Other statements</b></td></tr>");
        }
        return result.toString();
    }

    /** ***************************************************************
     * Create an HTML page that lists information about a particular
     * composite term, which is a representation of an XML
     * structure.
     * @param alphaList a TreeMap of TreeMaps of ArrayLists.  @see
     *                   createAlphaList()
     */
    public static String createCompositePage(KB kb, String kbHref, String term, TreeMap alphaList, 
                                             int limit, String language) {

        StringBuffer result = new StringBuffer();

        if (!DB.emptyString(kbHref)) 
            result.append(generateDynamicTOCHeader(kbHref));
        else
            result.append(generateTOCHeader(alphaList,kb.name + "-AllTerms.html"));
        result.append("<table width=\"100%\">");
        result.append("<tr bgcolor=#DDDDDD>");
        result.append("<td class=\"cell\"><font size=+2>");

        result.append(showTermName(kb,term,language));
        result.append("</font></td></tr>\n<tr>");
        result.append(createDocs(kb,kbHref,term,language));
        result.append("</table><P>\n");

        result.append(HTMLformatter.htmlDivider);
        result.append("<table>");
        result.append(createSynonyms(kb,kbHref,term));
        result.append("<tr class=\"title_cell\"><td class=\"label\">Component Structure</td><td colspan=4></td></tr>");
        result.append(createHasSameComponents(kb,kbHref,term,language));
        result.append("<tr><td class=\"label\">Member of Composites</td><td class=\"title_cell\">Composite Name</td>");
        result.append("<td class=\"title_cell\">Description of Element Role</td><td class=\"title_cell\">Cardinality</td><td></td></tr>\n");
        ArrayList superComposites = findContainingComposites(kb,term); 
        result.append(formatContainingComposites(kb,kbHref,superComposites,term,language));

        result.append("<tr><td class=\"label\">Components</td>" +
                      "<td class=\"title_cell\">Name</td>" +
                      "<td class=\"title_cell\">Description of Element Role</td>" +
                      "<td class=\"title_cell\">Cardinality</td>" +
                      "<td class=\"title_cell\">Data Type</td></tr>\n");
        ArrayList hier = createCompositeRecurse(kb, term, 0);
        hier.remove(hier.get(0));                                           // no need to show the composite itself
        result.append(formatCompositeHierarchy(kb,kbHref,hier,language));

        result.append("<tr class=\"title_cell\"><td class=\"label\">Relationships</td><td></td><td></td><td></td><td></td></tr>\n");
        result.append(createBelongsToClass(kb,kbHref,term,language));
        result.append(createUsingSameComponents(kb,kbHref,term,language));
        result.append("</table>\n");
        return result.toString();
    }

    /** ***************************************************************
     * Create an HTML page that lists information about a particular term,
     * with a limit on how many statements of each type should be
     * displayed.
     * 
     * @param alphaList a TreeMap of TreeMaps of ArrayLists.
     *                   @see createAlphaList()
     */
    public static String createPage(KB kb, String kbHref, String term, TreeMap alphaList,
                                    int limit, String language) {

        StringBuffer result = new StringBuffer();
        if (!DB.emptyString(kbHref)) {
            kbHref = kbHref + "&term=";
            result.append(generateDynamicTOCHeader(kbHref));
        }
        else
            result.append(generateTOCHeader(alphaList,kb.name + "-AllTerms.html"));

        result.append("<table width=\"100%\">");
        result.append("<tr bgcolor=#DDDDDD>");
        result.append("<td class=\"cell\"><font size=+2>");

        result.append(showTermName(kb,term,language));
        result.append("</font></td></tr>\n<tr>");

        result.append(createDocs(kb,kbHref,term,language));
        result.append("</table><P>\n");
        result.append("<table width=\"100%\">");
        result.append(createSynonyms(kb,kbHref,term));
        result.append(createComments(kb,kbHref,term,language));
        result.append("<tr class=\"title_cell\"><td class=\"label\">Relationships</td><td>&nbsp;</td><td>&nbsp;</td></tr>\n");
        result.append(createParents(kb,kbHref,term,language));
        result.append(createChildren(kb,kbHref,term,language));

        ArrayList superComposites = findContainingComposites(kb,term); 

        result.append(createInstances(kb,kbHref,term,language,superComposites));  // superComposites are excluded terms that will appear
                                                                                  // in the member of composites section
        result.append(createRelations(kb,kbHref,term,language));
        result.append(createUsingSameComponents(kb,kbHref,term,language));
        result.append(createBelongsToClass(kb,kbHref,term,language));

        result.append("<tr><td class=\"label\">Member of Composites</td><td class=\"title_cell\">Composite Name</td>");
        result.append("<td class=\"title_cell\">Description of Element Role</td><td class=\"title_cell\">Cardinality</td><td></td></tr>\n");
 
        result.append(formatContainingComposites(kb,kbHref,superComposites,term,language));
        result.append("</table>\n");

        result.append(HTMLformatter.htmlDivider);

        // result.append(createAllStatements(kb,kbHref,term,limit));
        return result.toString();
    }

    /** ***************************************************************
     *  Generate an alphabetic HTML list that points to the
     *  individual index pages (which collect all terms or term
     *  formats) starting with a particular letter.
     * 
     * @param alphaList a TreeMap of TreeMaps of ArrayLists.  @see
     *                   createAlphaList()
     */
    private static String generateTOCHeader(TreeMap alphaList, String allname) {

        if (!DB.emptyString(TOCheader)) 
            return TOCheader;
        StringBuffer result = new StringBuffer();
        result.append("<head><META http-equiv=\"Content-Type\" content=\"text/html; charset=utf-8\">" +
                   "<link rel=\"stylesheet\" type=\"text/css\" href=\"simple.css\"></head><body>\n");

        result.append(DocGen.header);
        result.append("<table width=\"100%\"><tr><td colspan=\"35\" class=\"title\">\n");
        result.append(header + "</td></tr><tr class=\"letter\">\n");
        result.append("<td><a href=\"" + allname + "\">All</a></td>\n");

        for (char c = 48; c < 58; c++) {                // numbers
            String cString = Character.toString(c);
            if (alphaList.keySet().contains(cString)) {
                String filelink = "number-" + cString + ".html";      
                result.append("<td><a href=\"" + filelink + "\">" + cString + "</a></td>\n");
            }
            else                 
                result.append("<td>" + cString + "</td>\n");
        }

        for (char c = 65; c < 91; c++) {                // letters
            String cString = Character.toString(c);
            if (alphaList.keySet().contains(cString)) {
                String filelink = "letter-" + cString + ".html";      
                result.append("<td><a href=\"" + filelink + "\">" + cString + "</a></td>\n");
            }
            else                 
                result.append("<td>" + cString + "</td>\n");
        }
        result.append("</tr></table>");
        TOCheader = result.toString();
        return result.toString();
    }

    /** ***************************************************************
     *  Generate an HTML page that lists term name and its
     *  documentation
     * 
     * @param alphaList a TreeMap of TreeMaps of ArrayLists.  @see
     *                   createAlphaList()
     */
    private static String generateTOCPage(KB kb, String firstLetter, TreeMap alphaList, String language) {

        //System.out.println("INFO in generateTOCPage()");
        int count = 0;
        StringBuffer result = new StringBuffer();
        result.append("<table width=\"100%\">");
        TreeMap map = (TreeMap) alphaList.get(firstLetter);
        Iterator it = map.keySet().iterator();
        while (it.hasNext()) {
            String formattedTerm = (String) it.next();
            ArrayList al = (ArrayList) map.get(formattedTerm);
            for (int i = 0; i < al.size(); i++) {
                String realTermName = (String) al.get(i);
                result.append("<tr><td><a href=\"" + realTermName + ".html\">" + 
                              showTermName(kb,realTermName,language) + "</a></td>\n");
                ArrayList docs = kb.askWithRestriction(0,"documentation",1,realTermName);
                if (docs != null && docs.size() > 0) {
                    Formula f = (Formula) docs.get(0);
                    String docString = f.getArgument(3);  
                    docString = kb.formatDocumentation("",docString,language);
                    docString = docString.replace("\\\"","\"");
                    if (docString.length() > 1)                 
                        result.append("<td class=\"cell\">" + removeEnclosingQuotes(docString) + "</td>\n");
                }
                result.append("</tr>\n");
            }
            //if ((count++ % 100) == 1) { System.out.print("."); }
        }
        //System.out.println();
        result.append("</tr>\n");
        result.append("</table>\n");
        return result.toString();
    }

    /** ***************************************************************
     *  Generate and save all the index pages that link to the
     *  individual term pages.
     *  @param pageList is a map of all term pages keyed by term
     *                  name
     *  @param dir is the directory in which to save the pages
     * 
     * @param alphaList a TreeMap of TreeMaps of ArrayLists.  @see
     *                   createAlphaList()
     */
    private static String saveIndexPages(KB kb, TreeMap alphaList, String dir, String language) throws IOException {

        System.out.println("INFO in DocGen.saveIndexPages()");
        String tocheader = generateTOCHeader(alphaList,kb.name + "-AllTerms.html");
        FileWriter fw = null;
        PrintWriter pw = null; 
        String filename = "";
        int count = 0;
        try {
            Iterator it = alphaList.keySet().iterator();  // iterate through single letters
            while (it.hasNext()) {
                String letter = (String) it.next();
                if (letter.compareTo("A") < 0) 
                    filename = dir + File.separator + "number-" + letter + ".html";
                else
                    filename = dir + File.separator + "letter-" + letter + ".html";
                fw = new FileWriter(filename);
                pw = new PrintWriter(fw);
                String page = generateTOCPage(kb,letter,alphaList,language);
                pw.println(tocheader);
                pw.println(page);
                pw.println(footer); 
                pw.close();
                fw.close();
                if ((count++ % 100) == 1) { System.out.print("."); }
            }   
            System.out.println();
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
    private static void printHTMLPages(TreeMap pageList, String dir) throws IOException {

        FileWriter fw = null;
        PrintWriter pw = null; 
        Iterator it = pageList.keySet().iterator();
        while (it.hasNext()) {
            String term = (String) it.next();
            String page = (String) pageList.get(term);
            String filename = dir + File.separator + term + ".html";
            //System.out.println("Info in DocGen.printPages(): filename : " + filename);
            try {
                fw = new FileWriter(filename);
                pw = new PrintWriter(fw);
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
     *  @return a HashMap where the keys are the term names and the
     *  values are the "headwords" (with quotes removed).
     */
    public static HashMap createHeadwordMap(KB kb) {

        HashMap result = new HashMap();
        ArrayList headwordForms = kb.ask("arg",0,"hasHeadword");
        if (headwordForms != null && headwordForms.size() > 0) {
            for (int i = 0; i < headwordForms.size(); i++) {
                Formula f = (Formula) headwordForms.get(i);
                String term = f.getArgument(1);
                String headword = removeEnclosingQuotes(f.getArgument(2));
                result.put(term,headword);
            }
        }
        return result;
    }

    /** ***************************************************************
     *  @return a HashMap where the keys are the headwords and the
     *  values are ArrayLists of term names (since headwords are not
     *  unique identifiers for terms). Don't put automatically
     *  creased instances in the map. If there's no headword, use
     *  the term name. This map is the inverse of headwordMap. @see
     *  DB.StringToKIFid()
     */
    public static HashMap createInverseHeadwordMap(KB kb, HashMap headwordMap) {

        HashMap result = new HashMap();

        Iterator it = kb.terms.iterator();  
        while (it.hasNext()) {
            String term = (String) it.next();
            if (term.indexOf("LocalInstance") > -1) // Don't display automatically created instances
                continue;                          
            String headword = term;
            if (simplified && headwordMap.get(term) != null) 
                headword = (String) headwordMap.get(term);
            ArrayList al;
            if (result.containsKey(headword)) 
                al = (ArrayList) result.get(headword);
            else {
                al = new ArrayList();
                result.put(headword,al);
            }
            al.add(term);
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
    private static TreeMap generateHTMLPages(KB kb, TreeMap alphaList,
                                             HashMap inverseHeadwordMap, String language) {

        TreeMap pageList = new TreeMap();
        Iterator it;
        it = inverseHeadwordMap.keySet().iterator();
        int count = 0;
        while (it.hasNext()) {
            String formattedTerm = (String) it.next();
            ArrayList termNames = (ArrayList) inverseHeadwordMap.get(formattedTerm);
            for (int i = 0; i < termNames.size(); i++) {
                String realTermName = (String) termNames.get(i);
                if (isCompositeInstance(kb,realTermName)) 
                    pageList.put(realTermName,createCompositePage(kb,"",realTermName,alphaList,200,language));
                else
                    pageList.put(realTermName,createPage(kb,"",realTermName,alphaList,200,language));     
                if ((count++ % 100) == 1) { System.out.print("."); }
            }
        }
        System.out.println();
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
    public static TreeMap createAlphaList(KB kb, HashMap stringMap) {

        TreeMap alphaList = new TreeMap();  

        System.out.println("INFO in DocGen.createAlphaList()");
        Iterator it = kb.terms.iterator();
        while (it.hasNext()) {
            String term = (String) it.next();
            if (term.indexOf("LocalInstance") > -1) 
                continue;
            String formattedTerm = term;
            if (simplified && stringMap.get(term) != null) 
                formattedTerm = (String) stringMap.get(term);
            String firstLetter = Character.toString(Character.toUpperCase(formattedTerm.charAt(0)));

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
                TreeMap map = new TreeMap();
                ArrayList al = new ArrayList();
                al.add(term);
                map.put(formattedTerm,al);
                alphaList.put(firstLetter,map);
                //System.out.println(firstLetter + " " + formattedTerm + " " + term);
            }
        }
        return alphaList;
    }

    /** ***************************************************************
     * Generate simplified HTML pages for all terms.  Output is a
     * set of HTML files sent to the directory specified in
     * generateDir()
     * 
     * @param s indicates whether to present "simplified" views of
     *          terms, meaning using a termFormat or headword,
     *          rather than the term name itself
     */
    public static void generateHTML(KB kb, String language, boolean s) {

        TreeMap pageList = new TreeMap();   // Keys are formatted term names, values are HTML pages
        TreeMap termMap = new TreeMap();    // Keys are headwords, values are terms        

        HashMap headwordMap = createHeadwordMap(kb); // A HashMap where the keys are the term names and the
                                                     // values are "headwords" (with quotes removed).
                                                     
        TreeMap alphaList = new TreeMap();       // a TreeMap of TreeMaps of ArrayLists.  @see createAlphaList()

        try {
            simplified = s;                 // Display term format expressions instead of term names
            alphaList = createAlphaList(kb,headwordMap);

                                            // Headword keys and ArrayList values (since the same headword can
                                            // be found in more than one term)
            HashMap inverseHeadwordMap = createInverseHeadwordMap(kb,headwordMap);  

            System.out.println("INFO in DocGen.generateHTML(): generating alpha list");
            String dir = generateDir();
            System.out.println("INFO in DocGen.generateHTML(): saving index pages");
            saveIndexPages(kb,alphaList,dir,language);

            System.out.println("INFO in DocGen.generateHTML(): generating HTML pages");
            pageList = generateHTMLPages(kb,alphaList,inverseHeadwordMap,language);
            printHTMLPages(pageList,dir);

            System.out.println("INFO in DocGen.generateHTML(): creating single index page");
            generateSingleHTML(kb, dir, alphaList, language, s);
        }
        catch (java.io.IOException e) {
            System.out.println(e.getMessage());
        }
    }      

    /** ***************************************************************
     * Generate a single HTML page showing all terms.
     * @param alphaList a TreeMap of TreeMaps of ArrayLists.
     *                  @see createAlphaList()
     * 
     *      letter->    formattedTerm1->term11,term12...term1N
     *                  formattedTerm2->term21,term22...term2N
     */
    public static void generateSingleHTML(KB kb, String dir, TreeMap alphaList,
                                          String language, boolean s) throws IOException {

        simplified = s;                // display term format expressions for term names
        FileWriter fw = null;
        PrintWriter pw = null; 
        try {
            fw = new FileWriter(dir + File.separator + kb.name+ "-AllTerms.html");
            pw = new PrintWriter(fw);
            pw.println("<head><META http-equiv=\"Content-Type\" content=\"text/html; charset=utf-8\">" +
                       "<link rel=\"stylesheet\" type=\"text/css\" href=\"simple.css\"></head><body>\n");
            pw.println(DocGen.header);
            pw.println("<table border=0><tr bgcolor=#CCCCCC><td>Name</td><td>Documentation</td></tr>\n");
            boolean even = true;
            Iterator it = alphaList.keySet().iterator();
            while (it.hasNext()) {
                String letter = (String) it.next();
                TreeMap values = (TreeMap) alphaList.get(letter);
                Iterator it2 = values.keySet().iterator();
                while (it2.hasNext()) {
                    String formattedTerm = (String) it2.next();
                    ArrayList terms = (ArrayList) values.get(formattedTerm);
                    for (int i = 0; i < terms.size(); i++) {
                        String term = (String) terms.get(i);
                        if (term.indexOf("LocalInstance") > -1) 
                            continue;
                        if (even)
                            pw.println("<tr><td>");
                        else
                            pw.println("<tr bgcolor=#CCCCCC><td>");
                        even = !even;
                        String printableTerm;
                        if (simplified) 
                            printableTerm = showTermName(kb,term,language);
                        else
                            printableTerm = term;
                        pw.println("<a href=\"" + term + ".html\">" + printableTerm + "</a>");
                        pw.println("</td>");
                        ArrayList docs = kb.askWithRestriction(0,"documentation",1,term);
                        if (docs != null && docs.size() > 0) {
                            Formula f = (Formula) docs.get(0);
                            String docString = f.getArgument(3);                     
                            if (docString != null && docString != "" && docString.length() > 100) 
                                docString = docString.substring(0,100) + "...\"";  
                            docString = kb.formatDocumentation("",docString,language);
                            docString = removeEnclosingQuotes(docString); 
                            docString = docString.replace("\\\"","\"");
                            pw.println("<td class=\"description\">" + docString);
                        }
                        else
                            pw.println("<td>");
                        pw.println("</td></tr>\n");
                    }
                }
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

    /** ***************************************************************
     */
    public static void generateSingleHTML(KB kb, String language, boolean s) throws IOException {

        HashMap headwordMap = createHeadwordMap(kb); 
        String dir = generateDir();
        TreeMap alphaList = createAlphaList(kb,headwordMap);
        generateSingleHTML(kb, dir, alphaList, language, s);
    }

    /** ***************************************************************
     */
    public static TreeSet generateMessageTerms(KB kb) {

        TreeSet result = new TreeSet();

        // (1) Collect all terms which are instances of Composite.
        ArrayList composites = kb.getAllInstances("Ddex_Composite");
        result.addAll(composites);

        System.out.println("results from (1)");
        System.out.println(result);

        // (2) Add all terms that are classes of the second argument of 
        // hasXMLElement relationships.
        ArrayList forms = kb.ask("arg",0,"hasXmlElement");
        if (forms != null) {
            for (int i = 0; i < forms.size(); i++) {
                Formula f = (Formula) forms.get(i);
                String elem = f.getArgument(2);

                ArrayList classes = kb.askWithRestriction(0,"instance",1,elem);
                for (int j = 0; j < classes.size(); j++) {
                    Formula f2 = (Formula) classes.get(j);
                    String c = f2.getArgument(2);
                    result.add(c);
                }
            }
        }

        System.out.println("results plus (2)");
        System.out.println(result);

        // (3) Add all terms that are classes of the second argument of 
        // hasXMLAttribute relationships.
        forms = kb.ask("arg",0,"hasXmlAttribute");

        if (forms != null) {
            for (int i = 0; i < forms.size(); i++) {
                Formula f = (Formula) forms.get(i);
                String arg = f.getArgument(2);

                ArrayList classes = kb.askWithRestriction(0,"instance",1,arg);
                for (int j = 0; j < classes.size(); j++) {
                    Formula f2 = (Formula) classes.get(j);
                    String c = f2.getArgument(2);
                    result.add(c);
                }
            }
        }

        System.out.println("results plus (3)");
        System.out.println(result);

        ArrayList forStepSeven = new ArrayList();
        // (4) Add all terms that are linked to the Composites of list (1) by an isXmlExtensionOf relationship.        
        if (composites != null) {
            for (int i = 0; i < composites.size(); i++) {
                Formula f = (Formula) composites.get(i);
                String arg = f.getArgument(2);

                ArrayList classes = kb.askWithRestriction(0,"isXmlExtensionOf",1,arg);
                for (int j = 0; j < classes.size(); j++) {
                    Formula f2 = (Formula) classes.get(j);
                    String c = f2.getArgument(2);
                    result.add(c);
                    forStepSeven.add(c);
                }

                // (5) Add all terms that are linked to the Composites of list (1) by an IsXmlCompositeFor relationship. 
                classes = kb.askWithRestriction(0,"IsXmlCompositeFor",1,arg);
                for (int j = 0; j < classes.size(); j++) {
                    Formula f2 = (Formula) classes.get(j);
                    String c = f2.getArgument(2);
                    result.add(c);
                }
            }
        }

        // (6) Add all terms that are the second argument of dataType relationships.
        forms = kb.ask("arg",0,"dataType");
        if (forms != null) {
            for (int i = 0; i < forms.size(); i++) {
                Formula f = (Formula) forms.get(i);
                String arg = f.getArgument(2);
                result.add(arg);
                forStepSeven.add(arg);
            }
        }


        // (7) Add all terms that are the first argument of instance/isOneOf relationships 
        // where the second argument is from list (4) or (6). 
        
        if (forStepSeven != null) {
            for (int i = 0; i < forStepSeven.size(); i++) {
                Formula f = (Formula) forStepSeven.get(i);
                String arg = f.getArgument(2);
                ArrayList classes = kb.askWithRestriction(0,"instance",2,arg);
                for (int j = 0; j < classes.size(); j++) {
                    Formula f2 = (Formula) classes.get(j);
                    String c = f2.getArgument(1);
                    result.add(c);
                }
            }
        }

        System.out.println("results plus (6) and (7)");
        System.out.println(result);

        // (8) There are some lists that are more complicated. XML labels are 
        // picked up using termFormat relationships where the third argument (a 
        // quoted string) is also the headword of a term in the ontology, i.e. 
        // where it is the third argument of another termFormat relationship that 
        // has Ddex_hw as a pseudo-language.        
        // 
        // (9) More complicated is also the list of terms that have a third 
        // argument of a termFormat relationship that has Ddex_hw as a 
        // pseudo-language, if that string is also a third argument of another 
        // termFormat relationship that has one of the subnamespaces as a 
        // pseudo-language (e.g. DdexC_hw).
        forms = kb.askWithRestriction(0,"termFormat",1,"Ddex_hw");
        forms = kb.askWithRestriction(0,"termFormat",1,"DdexC_hw");
        if (forms != null) {
            for (int i = 0; i < forms.size(); i++) {
                Formula f = (Formula) forms.get(i);
                String term = f.getArgument(2);
                String st = f.getArgument(3);
                ArrayList forms2 = kb.ask("arg",0,"termFormat");
                for (int j = 0; j < forms2.size(); j++) {
                    Formula f2 = (Formula) forms2.get(j);
                    String namespace = f2.getArgument(1);
                    String st2 = f2.getArgument(2);
                    if (st.equals(st2) && !namespace.equals("Ddex_hw") && !namespace.equals("DdexC_hw")) 
                        result.add(st2);
                }
            }
        }

        System.out.println("results plus (8) and (9)");
        System.out.println(result);

        // (10) NEW: We need to pick up the Relators IsA, IsSubClassOf etc. (and their reciprocals) 
        // as FrameworkTerms. I have no idea yet how to do this. 
        
        // (11) For all of these (1-7) add all terms that are parent terms (using 
        // subrelation and subclass relationships)

        TreeSet newResult = new TreeSet();
        Iterator it = result.iterator();
        while (it.hasNext()) {
            String term = (String) it.next();
            newResult.add(term);
            HashSet ps = (HashSet) kb.parents.get(term);
            if (ps != null)             
                newResult.addAll(ps);
        }
        // (1-3), (5-6) are MessageTerms
        // (4) are FrameworkTerms
        // (7-8) are SupportingTerms

        return newResult;
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
        KB kb = KBmanager.getMgr().getKB("DDEX");
        TreeSet ts = generateMessageTerms(kb);
        System.out.println(ts);
        System.out.println();
        System.out.println("Negative terms:");
        kb.terms.removeAll(ts);
        System.out.println(kb.terms);

        //String exp = "(documentation foo \"blah blah is so \\\"blah\\\" yeah\")";
        //System.out.println(exp);
        //exp = exp.replace("\\\"","\"");
        //System.out.println(exp);
        //System.out.println(kb.ask("arg",0,"hasHeadword"));
        //System.out.println(kb.ask("arg",2,"Composite"));
        //System.out.println(kb.ask("arg",2,"\"Composite\""));
        //DocGen.generateHTML(kb,"ddex",true);    // KB, language, simplified
    }
}
