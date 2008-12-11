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

/** A class to generate simplified HTML-based documentation for SUO-KIF terms. */
public class DocGen {

    private static DocGen DEFAULT_INSTANCE = null;

    private DocGen() {
    }

    /** ***************************************************************
     */
    public static DocGen getInstance() {
        if (DEFAULT_INSTANCE == null) {
            DEFAULT_INSTANCE = new DocGen();
        }
        return DEFAULT_INSTANCE;
    }

    public String header = "";
    public String TOCheader = "";
    public String footer = "<a href=http://www.rightscom.com>www.rightscom.com</a>";
    
    // false = do not display termFormat 
    // expressions in place of term names
    public boolean simplified = false;

    private HashMap relationsByKB = new HashMap();
    private List namespaces = null;
    private List namespacePrefixes = null;
    private Set isoCodes = null;

    private Map compositesBT = new TreeMap(); // one level "broader than"
    private Map compositesNT = new TreeMap(); // one level "narrower than"
    private List ntGraphs = new ArrayList();  // complete list of "narrow than" sequences
    private Map compositesUC = new TreeMap(); // BT upward closure for each term

    private String ddexHeader = 
        ("<html xmlns:fo=\"http://www.w3.org/1999/XSL/Format\">" 
         + System.getProperty("line.separator")
         + "  <head>" 
         + System.getProperty("line.separator")
         + "    <meta http-equiv=\"Content-Type\" content=\"text/html; charset=utf-8\">"
         + System.getProperty("line.separator")
         + "    <link rel=\"stylesheet\" type=\"text/css\" href=\"ddex.css\">"
         + System.getProperty("line.separator")
         + "    <title>DDEX Data Dictionary</title>"
         + "  </head>");


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
    public void setHeader(String h) {
        header = h;
    }

    /** ***************************************************************
     */
    public void setFooter(String f) {
        footer = f;
    }

    /** ***************************************************************
     *  If the given term is an instance, and is an instance of the
     *  term with the headword "Composite".
     */
    public boolean isCompositeInstance(KB kb, String term) {
        /*
        //System.out.println("INFO in DocGen.isCompositeInstance(): term: " + term);
        //ArrayList headwords = kb.askWithRestriction(0,"headword",2,"\"Composite\"");
        //System.out.println("headwords " + headwords);
        //if (headwords != null && headwords.size() > 0) {
        //Formula f = (Formula) headwords.get(0);
        //String composite = f.getArgument(1);
        // 
        // Note that we could try to find the composite by doing an ask on
        // headword and then searching for "Composite" but KIF.parse doesn't
        // index on Strings so we can't use kb.askWithRestrictions()

        // String composite = "ddex_Composite";
        //System.out.println("INFO in DocGen.isCompositeInstance(): composite: " + composite);
        // ArrayList instances = null;
        if (StringUtil.isNonEmptyString(term)) {
        instances = kb.askWithPredicateSubsumption("instance",1,term);
        }

        // System.out.println("2. instances == " + instances);

        if (instances != null) 
        return (Diagnostics.findParent(kb,term,composite) &&
        instances.size() > 0);
        //return false;
        //}
        */
        return kb.isInstanceOf(term, "ddex_Composite");
    }


    /** **************************************************************
     * Returns true if term is supposed to be displayed (at all, in
     * any form), else returns false.
     *
     * @return true or false
     */
    private boolean isLegalForDisplay(KB kb, String term) {
        
        return (StringUtil.isNonEmptyString(term)
                && term.matches("^\\w+_.+")
                && !getNamespaces(kb).contains(term)
                && !term.matches(".*(?i)localinstance.*")
                // && !term.matches(".*(?i)xml.*")
                && !term.startsWith("coa_")
                && !term.startsWith("ns_")
                && !term.startsWith("x_")
                && !term.matches(".+_hw_.+")
                && !term.matches(".+_syn_.+")
                && !term.matches(".+_en_.+"));
    }

    /** **************************************************************
     * Filters the list, remove all items that match pattern.
     *
     */
    private void filterByPattern(List seq, String regex) {
        try {
            if ((seq instanceof List) && StringUtil.isNonEmptyString(regex)) {
                int len = seq.size();
                Object obj = null;
                for (int i = 0; i < len; i++) {
                    obj = (Object) seq.remove(0);
                    if ((obj instanceof String) && ((String) obj).matches(regex)) {
                        continue;
                    }
                    seq.add(obj);
                }
            }
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
        return;
    }
                
    /** **************************************************************
     * Collect all namespaces asserted to be such in the knowledge base.
     *
     *  @return A list of all the terms that denote namespaces.
     */
    private List getNamespaces(KB kb) {

        if (namespaces != null) {
            return namespaces;
        }
        ArrayList formulas = kb.ask("arg", 0, "inNamespace");
        if (formulas != null) {
            ArrayList nss = new ArrayList();
            Iterator it = formulas.iterator();
            Formula f = null;
            String term = null;
            while (it.hasNext()) {
                f = (Formula) it.next();
                term = f.getArgument(2);
                if (!nss.contains(term)) {
                    nss.add(term);
                }
            }
            Comparator comp = new Comparator() {
                    public int compare(Object o1, Object o2) {
                        int l1 = o1.toString().length();
                        int l2 = o2.toString().length();
                        int ans = 0;
                        if (l1 > l2) {
                            ans = -1;
                        }
                        else if (l1 < l2) {
                            ans = 1;
                        }
                        return ans;
                    }
                };
            Collections.sort(nss, comp);
            namespacePrefixes = new ArrayList();
            for (int i = 0; i < nss.size(); i++) {
                term = (String) nss.get(i);
                if (term.endsWith(DB.KIF_NS_DELIMITER) || term.endsWith(DB.W3C_NS_DELIMITER)) {
                    term = term.substring(0, term.length() - 1);
                }
                if (term.startsWith("ns_") || term.startsWith("ns:")) {
                    term = term.substring(3);
                }
                namespacePrefixes.add(term + DB.KIF_NS_DELIMITER);
                namespacePrefixes.add(term + DB.W3C_NS_DELIMITER);
            }
            Collections.sort(namespacePrefixes, comp);
            namespaces = nss;
            System.out.println("namespaces == " + namespaces);
            System.out.println("namespacePrefixes == " + namespacePrefixes);
        }
        return namespaces;
    }

    /** **************************************************************
     */
    private String getNamespacePrefix(KB kb, String term) {
        String ans = "";
        if (StringUtil.isNonEmptyString(term)) {
            if (namespacePrefixes == null) {
                getNamespaces(kb);
            }
            int len = namespacePrefixes.size();
            String prefix = null;
            for (int i = 0; i < len; i++) {
                prefix = (String) namespacePrefixes.get(i);
                if (term.startsWith(prefix)) {
                    ans = prefix;
                    break;
                }
            }
        }
        return ans;
    }

    /** **************************************************************
     */
    private String stripNamespacePrefix(KB kb, String term) {
        String ans = term;
        String prefix = getNamespacePrefix(kb, term);
        if (StringUtil.isNonEmptyString(prefix)) {
            ans = term.substring(prefix.length());
        }
        return ans;
    }

    /** **************************************************************
     */
    private String toKifNamespace(KB kb, String term) {
        String ans = term;
        List namespaces = getNamespaces(kb);
        String kifTerm = DB.w3cToKif(term);
        String prefix = "ns" + DB.KIF_NS_DELIMITER;
        if (!kifTerm.equals("ns") && !kifTerm.startsWith(prefix)) {
            kifTerm = prefix + kifTerm;
        }
        String ns = null;
        if ((namespaces != null) && !namespaces.isEmpty()) {
            for (int i = 0; i < namespaces.size(); i++) {
                ns = (String) namespaces.get(i);
                if (ns.equalsIgnoreCase(kifTerm)) {
                    ans = ns;
                    break;
                }
            }
        }
        return ans;
    }

    /** **************************************************************
     */
    private String removeLinkableNamespacePrefixes(KB kb, String text) {
        String ans = text;
        try {
            if (StringUtil.isNonEmptyString(text)) {
                List nss = getNamespaces(kb);
                int len = nss.size();
                String prefixPattern = null;
                for (int i = 0; i < len; i++) {
                    prefixPattern = (String) nss.get(i);
                    if (prefixPattern.startsWith("ns_")) { 
                        prefixPattern = prefixPattern.substring(3); 
                    }
                    prefixPattern = "\\&\\%" + prefixPattern;
                    prefixPattern += "_";
                    ans = ans.replaceAll(prefixPattern, "");
                }
            }
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
        return ans;
    }

    /** **************************************************************
     * Collect all ISO code values asserted to be such in the
     * knowledge base.
     *
     *  @return A list of all the terms that denote ISO code values.
     */
    private Set getIsoCodes(KB kb) {
        if (isoCodes != null) {
            return isoCodes;
        }
        Set codes = kb.getAllInstancesWithPredicateSubsumption("ddex_IsoCode");
        Set classNames = kb.getAllSubClassesWithPredicateSubsumption("ddex_IsoCode");
        classNames.add("ddex_IsoCode");
        Object[] namesArr = classNames.toArray();
        if (namesArr != null) {
            String[] relns = {"ddex_IsOneOf", "ddex_IsA"};
            String className = null;
            List formulae = new ArrayList();
            for (int i = 0; i < namesArr.length; i++) {
                className = (String) namesArr[i];
                for (int r = 0; r < relns.length; r++) {
                    formulae.addAll(kb.askWithRestriction(0, relns[r], 2, className));
                }
            }
            Formula f = null;
            for (int j = 0; j < formulae.size(); j++) {
                f = (Formula) formulae.get(j);
                codes.add(f.getArgument(1));
            }
        }
        isoCodes = codes;
        return isoCodes;
    }

    /** ***************************************************************
     *  Collect relations in the knowledge base
     *
     *  @return The set of relations in the knowledge base.
     */
    private ArrayList getRelations(KB kb) {

        ArrayList cached = (ArrayList) relationsByKB.get(kb);
        if (cached != null) {
            return cached;
        }

        TreeSet relations = new TreeSet();
        String[] classes = {"ddex_Relator", "BinaryPredicate"};
        ArrayList instances = null;
        Iterator it = null;
        Formula f = null;
        String reln = null;
        for (int i = 0; i < classes.length; i++) {
            instances = kb.askWithPredicateSubsumption("instance",2,classes[i]);

            // System.out.println("3. instances == " + instances);

            if (instances != null) {
                it = instances.iterator();
                while (it.hasNext()) {
                    f = (Formula) it.next();
                    reln = f.getArgument(1).intern();
                    if (reln.contains(DB.KIF_NS_DELIMITER)) {
                        relations.add(reln);
                    }
                }
            }
        }
        cached = new ArrayList(relations);
        relationsByKB.put(kb, cached);
        return cached;
    }

    /** ***************************************************************
     *
     */
    private void buildCompositeCaches(KB kb) {
        buildCompositeMaps(kb);
        buildCompositeUpwardClosureGraph(kb);
        return;
    }

    /** ***************************************************************
     *
     */
    private void buildCompositeMaps(KB kb) {
        try {
            System.out.println("Building composite maps");
            compositesBT.clear();
            compositesNT.clear();
            String[] relns = {"ddex_HasXmlElement", "ddex_HasXmlAttribute"};
            // Set accumulator = kb.getAllInstancesWithPredicateSubsumption("ddex_Composite");
            List working = new ArrayList();
            List tmp = null;
            for (int r = 0; r < relns.length; r++) {
                tmp = kb.ask("arg", 0, relns[r]);
                if (tmp != null) {
                    working.addAll(tmp);
                }
            }

            System.out.println(working.size() + " statements in working");
            String arg0 = null;
            String arg1 = null;
            String arg2 = null;
            Formula f = null;
            List upward = null;
            List downward = null;
            for (int i = 0; i < working.size(); i++) {
                f = (Formula) working.get(i);
                arg1 = f.getArgument(1);
                arg2 = f.getArgument(2);
                downward = (List) compositesNT.get(arg1);
                if (downward == null) {
                    downward = new ArrayList();
                    compositesNT.put(arg1, downward);
                }
                if (!downward.contains(arg2)) {
                    downward.add(arg2);
                }
                upward = (List) compositesBT.get(arg2);
                if (upward == null) {
                    upward = new ArrayList();
                    compositesBT.put(arg2, upward);
                }
                if (!upward.contains(arg1)) {
                    upward.add(arg1);
                }
            }

            List tops = new ArrayList();
            List leaves = new ArrayList();
            List keys = new ArrayList(compositesNT.keySet());
            String key = null;
            List val = null;
            for (int i = 0; i < keys.size(); i++) {
                key = (String) keys.get(i);
                val = (List) compositesNT.get(key);
                // System.out.println(key + " => " + val);
                if (compositesBT.get(key) == null) {
                    tops.add(key);
                }
            }
            // System.out.println("compositesBT: ");
            keys = new ArrayList(compositesBT.keySet());
            key = null;
            val = null;
            for (int i = 0; i < keys.size(); i++) {
                key = (String) keys.get(i);
                val = (List) compositesBT.get(key);
                // System.out.println(key + " => " + val);
                if (compositesNT.get(key) == null) {
                    leaves.add(key);
                }
            }

            System.out.println("Computed " + tops.size() + " tops");
            System.out.println("Computed " + leaves.size() + " leaves");

            List accumulator = new ArrayList();
            working.clear();
            for (int i = 0; i < tops.size(); i++) {
                String top = (String) tops.get(i);
                List topL = new ArrayList();
                topL.add(top);
                accumulator.add(topL);
            };
            List graphs = new ArrayList();
            String key2 = null;
            List val2 = null;
            working.addAll(accumulator);
            while (!working.isEmpty()) {

                // System.out.println("working.size() == " + working.size());

                graphs.addAll(working);

                // System.out.println("graphs.size() == " + graphs.size());

                accumulator.clear();
                for (int i = 0; i < working.size(); i++) {
                    val = (List) working.get(i);
                    key = (String) val.get(val.size() - 1);
                    val2 = (List) compositesNT.get(key);
                    if (val2 != null) {
                        for (int j = 0; j < val2.size(); j++) {
                            key2 = (String) val2.get(j);
                            List seq = new ArrayList();
                            seq.addAll(val);
                            if (seq.contains(key2)) { 
                                System.out.println("CYCLE DETECTED!");
                                System.out.println(seq + "<=" + key2);
                                // System.exit(1);
                            }
                            else {
                                seq.add(key2);
                                if (accumulator.contains(seq)) {
                                    System.out.println("CYCLE DETECTED!");
                                    System.out.println(seq + " is already in accumulator");
                                    // System.exit(1);
                                }
                                else {
                                    accumulator.add(seq);
                                }
                            }
                        }
                    }
                }
                working.clear();
                working.addAll(accumulator);
            }
            ntGraphs.addAll(graphs);

            /*
              for (int i = 0; i < graphs.size(); i++) {
              val = (List) graphs.get(i);
              System.out.println(i + ": " + val);
              }
              System.exit(1);
            */

            System.out.println(compositesBT.size() + " keys added to compositesBT");
            System.out.println(compositesNT.size() + " keys added to compositesNT");
            System.out.println(ntGraphs.size() + " lists added to ntGraphs");

            // System.out.println("compositesBT == " + compositesBT);
            // System.out.println("compositesNT == " + compositesNT);
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
        return;
    }

    /** ***************************************************************
     *
     */
    private void buildCompositeUpwardClosureGraph(KB kb) {
        try {
            System.out.println("Building compositesUC");
            compositesUC.clear();
            Iterator it = ntGraphs.iterator();
            List graph = null;
            List pairs = null;
            List val = null;
            String key = null;

            // 1. Build the basic graph structure.
            while (it.hasNext()) {
                val = (List) it.next();
                for (int i = 0; i < val.size(); i++) {
                    if (i > 0) {
                        key = (String) val.get(i);
                        graph = (List) compositesUC.get(key);
                        if (graph == null) {
                            graph = new ArrayList();
                            compositesUC.put(key, graph);
                        }
                        // pairs = new ArrayList();
                        for (int j = 0; j < i; j++) {
                            List pair = new ArrayList();
                            pair.add(val.get(j));
                            // pairs.add(pair);
                            if (!graph.contains(pair)) {
                                graph.add(pair);
                            }
                        }
                        /*
                          if (!graph.contains(pairs)) {
                          graph.add(pairs);
                          }
                        */
                    }
                }
            }

            // 2. Add display names and documentation strings for
            // each term, using the structure of each composite as
            // a guide.

            /*             */
            System.out.println("Adding context-specific display names, documentation");
            String key2 = null;
            it = compositesUC.keySet().iterator();
            while (it.hasNext()) {
                key = (String) it.next();
                graph = (List) compositesUC.get(key);
                if ((graph != null) && !graph.isEmpty()) {
                    List contexts = new ArrayList();
                    contexts.add(0, "XMLLabel");
                    contexts.add("EnglishLanguage");
                    for (int i = 0; i < graph.size(); i++) {
                        val = (List) graph.get(i);
                        key2 = (String) val.get(0);

                        // Add the print name for key2.
                        String printName = getFirstTermFormat(kb, key2, contexts);
                        val.add(printName);

                        // Add the documentation string, which
                        // should describe the role that the
                        // element key plays in the composite
                        // key2.
                        String doc = "";
                        ArrayList docForms = kb.askWithRestriction(0,"documentation",1,key);
                        if (docForms != null && docForms.size() > 0) {
                            for (int j = 0; j < docForms.size(); j++) {
                                Formula f = (Formula) docForms.get(j);
                                String context = f.getArgument(2);
                                if (context.equals(key2)) {
                                    doc = f.getArgument(3);
                                }
                            }
                        }
                        if (!StringUtil.isNonEmptyString(doc)) {
                            doc = getCompositeDocumentation(kb, key, contexts);
                        }
                        val.add(doc);

                    }

                    // System.out.println(key + " => " + graph);

                }
            }
            System.out.println(compositesUC.size() + " keys added to compositesUC");
            // System.exit(1);
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
        return;
    }

    /** ***************************************************************
     *
     */
    private List getCompositeUpwardClosure(KB kb, String term) {
        List ans = null;
        try {
            if (StringUtil.isNonEmptyString(term)) {
                if (compositesUC.isEmpty()) {
                    buildCompositeCaches(kb);
                }
                ans = (List) compositesUC.get(term);
            }
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
        return ans;
    }

    /** ***************************************************************
     *
     */
    private boolean hasSubComponents(KB kb, String term) {
        boolean ans = false;
        try {
            if (StringUtil.isNonEmptyString(term)) {
                ans = (getSubComponents(kb, term) != null);
            }
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
        return ans;
    }

    /** ***************************************************************
     *
     */
    private List getSubComponents(KB kb, String term) {
        List ans = null;
        try {
            if (StringUtil.isNonEmptyString(term)) {
                if (compositesNT.isEmpty()) {
                    buildCompositeCaches(kb);
                }
                ans = (List) compositesNT.get(term);
            }
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
        return ans;
    }

    /** ***************************************************************
     *
     */
    private String getFirstTermFormat(KB kb, String term, List contexts) {
        String ans = null;
        try {
            if (StringUtil.isNonEmptyString(term)) {
                List forms = kb.askWithRestriction(2, term, 0, "termFormat");
                if ((forms != null) && !forms.isEmpty()) {
                    String ctx = null;
                    Formula f = null;
                    for (int i = 0; i < contexts.size(); i++) {
                        ctx = (String) contexts.get(i);
                        for (int j = 0; j < forms.size(); j++) {
                            f = (Formula) forms.get(j);
                            if (f.getArgument(1).equals(ctx)) {
                                ans = f.getArgument(3);
                                break;
                            }
                        }
                        if (ans != null) { break; }
                    }
                    if ((ans == null) && term.matches(".*(?i)LocalInstance.*")) {
                        String moreGeneralTerm = getFirstGeneralTerm(kb, term);
                        if (StringUtil.isNonEmptyString(moreGeneralTerm)) {
                            ans = getFirstTermFormat(kb, moreGeneralTerm, contexts);
                        }
                    }
                }
                if (ans == null) { ans = term; }
                ans = StringUtil.removeEnclosingQuotes(ans);
            }
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
        return ans;
    }

    /** ***************************************************************
     *
     */
    private String getCompositeDocumentation(KB kb, String term, List contexts) {
        String ans = null;
        try {
            if (StringUtil.isNonEmptyString(term)) {
                List forms = kb.askWithRestriction(1, term, 0, "documentation");
                Formula f = null;
                if ((forms != null) && !forms.isEmpty()) {
                    if (!(contexts instanceof List)) {
                        contexts = new ArrayList();
                    }
                    if (!contexts.contains("XMLLabel")) {
                        contexts.add(0, "XMLLabel");
                    }
                    if (!contexts.contains("EnglishLanguage")) {
                        contexts.add("EnglishLanguage");
                    }
                    // contexts.addAll(Arrays.asList("XMLLabel", "EnglishLanguage"));
                    String ctx = null;
                    for (int i = 0; i < contexts.size(); i++) {
                        ctx = (String) contexts.get(i);
                        for (int j = 0; j < forms.size(); j++) {
                            f = (Formula) forms.get(j);
                            if (f.getArgument(2).equals(ctx)) {
                                ans = f.getArgument(3);
                                break;
                            }
                        }
                        if (ans != null) { break; }
                    }
                }
                if (ans == null) {
                    String classOfTerm = getFirstGeneralTerm(kb, term);
                    if (StringUtil.isNonEmptyString(classOfTerm)) {
                        ans = getCompositeDocumentation(kb, classOfTerm, contexts);
                    }
                }
                if (!StringUtil.isNonEmptyString(ans)) { ans = term; }
            }
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
        return ans;
    }

    /** **************************************************************
     */
    private String getFirstGeneralTerm(KB kb, String term) {
        String ans = null;
        try {
            String[] preds = {"instance", 
                              "ddex_IsOneOf", 
                              "ddex_IsA", 
                              "ddex_HasDataType",
                              "ddex_IsXmlExtensionOf",
                              "ddex_IsXmlCompositeFor"};
            List forms = null;
            for (int i = 0; i < preds.length; i++) {
                forms = kb.askWithRestriction(1, term, 0, preds[i]);
                if ((forms != null) && !forms.isEmpty()) {
                    Formula f = (Formula) forms.get(0);
                    ans = f.getArgument(2);
                    break;
                }
            }
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
        return ans;
    }

    /** ***************************************************************
     */
    private ArrayList getFirstSpecificTerms(KB kb, String term) {
        ArrayList ans = new ArrayList();
        /*          */
        try {
            if (StringUtil.isNonEmptyString(term)) {
                String[] relns = {"instance", 
                                  "ddex_IsOneOf", 
                                  "ddex_IsA", 
                                  // "ddex_HasDataType",
                                  "ddex_IsXmlExtensionOf",
                                  "ddex_IsXmlCompositeFor"};
                Formula f = null;
                String term2 = null;
                for (int j = 0; j < relns.length; j++) {
                    List forms = kb.askWithRestriction(0, relns[j], 2, term);
                    if (forms != null) {
                        for (int k = 0; k < forms.size(); k++) {
                            f = (Formula) forms.get(k);
                            term2 = f.getArgument(1);
                            if (!ans.contains(term2)) {
                                ans.add(term2);
                            }
                        }
                    }
                }
            }
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
        return ans;
    }

    /** ***************************************************************
     */
    private ArrayList getFirstInstances(KB kb, String term) {
        ArrayList ans = new ArrayList();
        /*          */
        try {
            if (StringUtil.isNonEmptyString(term)) {
                String[] relns = {"instance", 
                                  "ddex_IsOneOf", 
                                  "ddex_IsA"};
                Formula f = null;
                String term2 = null;
                for (int j = 0; j < relns.length; j++) {
                    List forms = kb.askWithRestriction(0, relns[j], 2, term);
                    if (forms != null) {
                        for (int k = 0; k < forms.size(); k++) {
                            f = (Formula) forms.get(k);
                            term2 = f.getArgument(1);
                            if (!ans.contains(term2)) {
                                ans.add(term2);
                            }
                        }
                    }
                }
            }
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
        return ans;
    }

    /** ***************************************************************
     */
    private ArrayList getFirstSubClasses(KB kb, String term) {
        ArrayList ans = new ArrayList();
        /*          */
        try {
            if (StringUtil.isNonEmptyString(term)) {
                List forms = kb.askWithPredicateSubsumption("subclass", 2, term);
                Formula f = null;
                String arg1 = null;
                if (forms != null) {
                    for (int k = 0; k < forms.size(); k++) {
                        f = (Formula) forms.get(k);
                        arg1 = f.getArgument(1);
                        if (!ans.contains(arg1)) {
                            ans.add(arg1);
                        }
                    }
                }
            }
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
        return ans;
    }

    /** ***************************************************************
     */
    private String createDocs(KB kb, String kbHref, String term, String language) {

        StringBuffer result = new StringBuffer();
        if (isLegalForDisplay(kb, term)) {
            ArrayList docs = kb.askWithRestriction(0,"documentation",1,term);
            if (docs != null && !docs.isEmpty()) {
                Formula f = (Formula) docs.get(0);
                String docString = f.getArgument(3);  
                docString = kb.formatDocumentation(kbHref,docString,language);
                docString = StringUtil.removeEnclosingQuotes(docString);
                // docString = StringUtil.escapeQuoteChars(docString);
                // docString = docString.replace("\\\"","\"");
                result.append("<td valign=\"top\" class=\"description\">" 
                              + docString + "</td></tr>\n");
            }
        }
        return result.toString();
    }

    /** ***************************************************************
     */
    private String createComments(KB kb, String kbHref, String term, String language) {

        StringBuffer result = new StringBuffer();
        if (isLegalForDisplay(kb, term)) {
            ArrayList docs = kb.askWithRestriction(0,"comment",1,term);
            if (docs != null && !docs.isEmpty()) {
                result.append("<tr><td valign=\"top\" class=\"label\">Comments</td>");
                for (int i = 0; i < docs.size(); i++) {
                    Formula f = (Formula) docs.get(i);
                    String docString = f.getArgument(3);  
                    docString = kb.formatDocumentation(kbHref,docString,language);
                    docString = StringUtil.removeEnclosingQuotes(docString);
                    // docString = StringUtil.escapeQuoteChars(docString);
                    // docString = docString.replace("\\\"","\"");
                    if (i > 0) result.append("<tr><td>&nbsp;</td>");
                    result.append("<td valign=\"top\" colspan=2 class=\"cell\">" 
                                  + docString + "</td></tr>\n");
                }
            }
        }
        return result.toString();
    }

    /** ***************************************************************
     */
    private String createSynonyms(KB kb, String kbHref, String term) {

        StringBuffer result = new StringBuffer();
        if (isLegalForDisplay(kb, term)) {
            ArrayList syn = null;
            if (StringUtil.isNonEmptyString(term)) {
                syn = kb.askWithRestriction(0,"termFormat",2,term);
            }
            boolean found = false;
            if (syn != null && syn.size() > 0) {
                for (int i = 0; i < syn.size(); i++) {
                    Formula f = (Formula) syn.get(i);
                    String namespace = f.getArgument(1);
                    if (namespace.endsWith("syn")) {
                        if (!found) 
                            result.append("Synonym(s)</td><td valign=\"top\" class=\"cell\"><i>");
                        String s = f.getArgument(3); 
                        s = StringUtil.removeEnclosingQuotes(s);
                        if (found) result.append(", ");                
                        result.append("<i>" + s + "</i>");
                        found = true;
                    }
                }
                result.append("</td></tr>\n");
            }
        }
        return result.toString();
    }

    /** ***************************************************************
     */
    private String createHasSameComponents(KB kb, 
                                           String kbHref, 
                                           String term, 
                                           String language) {

        StringBuffer result = new StringBuffer();
        if (isLegalForDisplay(kb, term)) {
            String suffix = "";
            if (StringUtil.emptyString(kbHref)) 
                suffix = ".html";
            ArrayList syn = kb.askWithRestriction(0,"ddex_IsXmlExtensionOf",1,term);
            if (syn != null && !syn.isEmpty()) {
                result.append("<tr><td valign=\"top\" class=\"label\">Has Same Components As</td>");
                for (int i = 0; i < syn.size(); i++) {
                    Formula f = (Formula) syn.get(i);
                    String s = f.getArgument(2); 
                    String termHref = "<a href=\"" + kbHref + s + suffix + "\">" + showTermName(kb,s,language) + "</a>";
                    if (i > 0) result.append("<tr><td>&nbsp;</td>");
                    result.append("<td valign=\"top\" class=\"cell\">" + termHref + "</td></tr>\n");
                }
            }
        }
        return result.toString();
    }

    /** ***************************************************************
     */
    private String createUsingSameComponents(KB kb, 
                                             String kbHref, 
                                             String term, 
                                             String language) {

        StringBuffer result = new StringBuffer();
        if (isLegalForDisplay(kb, term)) {
            String suffix = "";
            if (StringUtil.emptyString(kbHref)) 
                suffix = ".html";
            ArrayList syn = null;
            if (StringUtil.isNonEmptyString(term)) {
                syn = kb.askWithRestriction(0,"ddex_IsXmlExtensionOf",2,term);
            }
            if ((syn != null) && !syn.isEmpty()) {
                result.append("<tr><td valign=\"top\" class=\"label\">Composites Using Same Components</td>");
                for (int i = 0; i < syn.size(); i++) {
                    Formula f = (Formula) syn.get(i);
                    String s = f.getArgument(1); 
                    String termHref = "<a href=\"" + kbHref + s + suffix + "\">" 
                        + showTermName(kb,s,language) + "</a>";
                    if (i > 0) result.append("<tr><td>&nbsp;</td>");
                    result.append("<td valign=\"top\" class=\"cell\">" + termHref + "</td></tr>\n");
                }
            }
        }
        return result.toString();
    }

    /** ***************************************************************
     */
    private String createParents(KB kb, String kbHref, String term, String language) {

        String suffix = "";
        if (StringUtil.emptyString(kbHref)) 
            suffix = ".html";
        StringBuffer result = new StringBuffer();

        // System.out.println("4. forms == " + forms);
        ArrayList forms = new ArrayList();
        ArrayList parents = new ArrayList();
        String[] relns = {"subclass", "subrelation", "subAttribute", "subentity"};
        if (StringUtil.isNonEmptyString(term)) {
            for (int j = 0; j < relns.length; j++) {
                List tmp = kb.askWithPredicateSubsumption(relns[j], 1, term);
                if (tmp != null) {
                    forms.addAll(tmp);
                }
            }
            Formula f = null;
            for (int i = 0; i < forms.size(); i++) {
                f = (Formula) forms.get(i);
                if (!f.sourceFile.endsWith(KB._cacheFileSuffix)) {
                    String s = f.getArgument(2);
                    if (isLegalForDisplay(kb, s) && !parents.contains(s)) {
                        parents.add(s);
                    }
                }
            }
            if (!parents.isEmpty()) {
                Collections.sort(parents, String.CASE_INSENSITIVE_ORDER);
                result.append("<tr><td valign=\"top\" class=\"label\">Parents</td>");
                for (int j = 0; j < parents.size(); j++) {
                    String s = (String) parents.get(j);
                    String termHref = "<a href=\"" + kbHref + s + suffix + "\">" 
                        + showTermName(kb,s,language) + "</a>";
                    if (j > 0) result.append("<tr><td>&nbsp;</td>");                
                    result.append("<td valign=\"top\" class=\"cell\">" + termHref + "</td>");
                    ArrayList docs = kb.askWithRestriction(0,"documentation",1,s);
                    if (docs != null && !docs.isEmpty()) {
                        Formula docf = (Formula) docs.get(0);
                        String docString = docf.getArgument(3);  
                        docString = kb.formatDocumentation(kbHref,docString,language);
                        docString = StringUtil.removeEnclosingQuotes(docString);
                        // docString = StringUtil.escapeQuoteChars(docString);
                        // docString = docString.replace("\\\"","\"");
                        result.append("<td valign=\"top\" class=\"cell\">" + docString + "</td>");
                    }
                }
                result.append("</tr>\n");
            }
        }
        return result.toString();
    }

    /** ***************************************************************
     */
    private String createChildren(KB kb, String kbHref, String term, String language) {

        String suffix = "";
        if (StringUtil.emptyString(kbHref)) 
            suffix = ".html";
        StringBuffer result = new StringBuffer();
        String[] relns = {"subclass", "subrelation", "subAttribute", "subentity"};
        ArrayList forms = new ArrayList();
        if (StringUtil.isNonEmptyString(term)) {
            for (int i = 0; i < relns.length; i++) {
                List tmp = kb.askWithPredicateSubsumption(relns[i], 2, term);
                if ((tmp != null) && !tmp.isEmpty()) {
                    forms.addAll(tmp);
                }
            }
        }
        // System.out.println("5. forms == " + forms);

        if (forms != null && !forms.isEmpty()) {
            ArrayList kids = new ArrayList();
            for (int i = 0; i < forms.size(); i++) {
                Formula f = (Formula) forms.get(i);
                if (!f.sourceFile.endsWith(KB._cacheFileSuffix)) {
                    String s = f.getArgument(1);
                    if (isLegalForDisplay(kb, s) && !kids.contains(s)) {
                        kids.add(s);
                    }
                }
            }
            if (!kids.isEmpty()) {
                Collections.sort(kids, String.CASE_INSENSITIVE_ORDER);
                result.append("<tr><td valign=\"top\" class=\"label\">Children</td>");
                for (int j = 0; j < kids.size(); j++) {
                    String s = (String) kids.get(j);
                    String termHref = "<a href=\"" + kbHref + s + suffix + "\">" 
                        + showTermName(kb,s,language) + "</a>";
                    if (j > 0) result.append("<tr><td>&nbsp;</td>");                
                    result.append("<td valign=\"top\" class=\"cell\">" + termHref + "</td>");
                    ArrayList docs = kb.askWithRestriction(0,"documentation",1,s);
                    if (docs != null && docs.size() > 0) {
                        Formula f = (Formula) docs.get(0);
                        String docString = f.getArgument(3);  
                        docString = kb.formatDocumentation(kbHref,docString,language);
                        docString = StringUtil.removeEnclosingQuotes(docString);
                        // docString = StringUtil.escapeQuoteChars(docString);
                        // docString = docString.replace("\\\"","\"");
                        result.append("<td valign=\"top\" class=\"cell\">" + docString + "</td>");
                    }
                }
                result.append("</tr>\n");
            }
        }
        return result.toString();
    }

    /** ***************************************************************
     */
    private String createInstances(KB kb, 
                                   String kbHref, 
                                   String term, 
                                   String language, 
                                   ArrayList excluded) {

        String suffix = "";
        if (StringUtil.emptyString(kbHref)) 
            suffix = ".html";
        StringBuffer result = new StringBuffer();
        ArrayList forms = null;
        if (StringUtil.isNonEmptyString(term)) {
            forms = kb.askWithPredicateSubsumption("instance", 2, term);
        }

        // System.out.println("6. forms == " + forms);

        if (forms != null && !forms.isEmpty()) {
            ArrayList instances = new ArrayList();
            for (int i = 0; i < forms.size(); i++) {
                Formula f = (Formula) forms.get(i);
                if (!f.sourceFile.endsWith(KB._cacheFileSuffix)) {
                    String s = f.getArgument(1); 
                    if (!excluded.contains(s) && isLegalForDisplay(kb, s)) {
                        instances.add(s);
                    }
                }
            }
            if (!instances.isEmpty()) {
                Collections.sort(instances, String.CASE_INSENSITIVE_ORDER);
                result.append("<tr><td valign=\"top\" class=\"label\">Instances</td>");
                for (int j = 0; j < instances.size(); j++) {
                    String s = (String) instances.get(j);
                    String displayName = showTermName(kb,s,language);
                    String xmlName = "";
                    if (displayName.equals(s)) 
                        xmlName = showTermName(kb,s,"XMLLabel");
                    if (!StringUtil.emptyString(xmlName)) 
                        displayName = xmlName;
                    String termHref = "<a href=\"" + kbHref + s + suffix + "\">" 
                        + displayName + "</a>";
                    if (j > 0) result.append("<tr><td>&nbsp;</td>");                
                    result.append("<td valign=\"top\" class=\"cell\">" + termHref + "</td>");
                    ArrayList docs = kb.askWithRestriction(0,"documentation",1,s);
                    if (docs != null && docs.size() > 0) {
                        Formula f = (Formula) docs.get(0);
                        String docString = f.getArgument(3);  
                        docString = kb.formatDocumentation(kbHref,docString,language);
                        docString = StringUtil.removeEnclosingQuotes(docString);
                        // docString = StringUtil.escapeQuoteChars(docString);
                        // docString = docString.replace("\\\"","\"");
                        result.append("<td valign=\"top\" class=\"cell\">" + docString + "</td>");
                    }
                }
                result.append("</tr>\n");
            }
        }
        return result.toString();
    }

    /** ***************************************************************
     */
    private String createRelations(KB kb, String kbHref, String term, String language) {

        StringBuffer result = new StringBuffer();
        if (isLegalForDisplay(kb, term)) {
            String suffix = "";
            if (StringUtil.emptyString(kbHref)) 
                suffix = ".html";
            ArrayList relations = getRelations(kb);
            if ((relations != null) && !relations.isEmpty()) {
                ArrayList avoid = new ArrayList();
                avoid.add("subclass");
                avoid.add("instance");
                avoid.add("documentation");
                TreeMap map = new TreeMap();
                String relation = null;
                for (int i = 0; i < relations.size(); i++) {
                    relation = (String) relations.get(i);
                    // System.out.println("INFO in DocGen.createRElations(): relation: " + relation);
                    if (!avoid.contains(relation)) {
                        ArrayList statements = kb.askWithPredicateSubsumption(relation, 1, term);

                        // System.out.println("7. statements == " + statements);

                        if ((statements != null) && !statements.isEmpty()) {
                            ArrayList vals = new ArrayList();
                            for (int j = 0; j < statements.size(); j++) {
                                Formula f = (Formula) statements.get(j);
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
                    ArrayList vals = null;
                    String s = null;
                    boolean firstLine = true;
                    for (int k = 0; k < relations.size(); k++) {
                        relation = (String) relations.get(k);
                        vals = (ArrayList) map.get(relation);
                        if ((vals != null) && !vals.isEmpty()) {
                            String relnHref = "<a href=\"" + kbHref + relation + suffix + "\">" 
                                + showTermName(kb,relation,language) + "</a>";
                            for (int m = 0; m < vals.size(); m++) {
                                s = (String) vals.get(m);
                                String termHref = "<a href=\"" + kbHref + s + suffix + "\">" 
                                    + showTermName(kb,s,language) + "</a>";
                                if (firstLine) {
                                    result.append("<tr><td valign=\"top\" class=\"label\">Relations</td>");
                                    firstLine = false;
                                }
                                else {
                                    result.append("<tr><td>&nbsp;</td>");                
                                }
                                // System.out.println( relnHref );
                                // System.out.println( termHref );
                                result.append("<td valign=\"top\" class=\"cell\">" + relnHref + "</td>");
                                result.append("<td valign=\"top\" class=\"cell\">" + termHref + "</td></tr>\n");
                            }
                        }
                    }                
                }            
            }
        }
        return result.toString();
    }

    /** ***************************************************************
     */
    private String showCardinalityCell(KB kb, String kbHref, String term, String context) {

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
    private String createCompositeComponentLine(KB kb, 
                                                String kbHref, 
                                                String term, 
                                                int indent, 
                                                String language) {

        StringBuffer result = new StringBuffer();
        // if (isLegalForDisplay(kb, term)) {
        String suffix = "";
        if (StringUtil.emptyString(kbHref)) 
            suffix = ".html";
        result.append("<tr><td></td><td valign=\"top\" class=\"cell\">");
        String parentClass = "";;
        ArrayList instanceForms = kb.askWithPredicateSubsumption("instance", 1, term);

        // System.out.println("1. instanceForms == " + instanceForms);

        if (instanceForms != null && instanceForms.size() > 0) {
            Formula f = (Formula) instanceForms.get(0);
            parentClass = f.getArgument(2);
        }
        ArrayList termForms = null;
        if (StringUtil.isNonEmptyString(term)) {
            termForms = kb.askWithTwoRestrictions(0,"termFormat",1,"XMLLabel",2,term);
        }
        if (termForms != null) {
            for (int i = 0; i < termForms.size(); i++) {               
                Formula f = (Formula) termForms.get(i);
                result.append(indentChars("&nbsp;&nbsp;",indent));
                String termFormat = f.getArgument(3);
                termFormat = termFormat.substring(1,termFormat.length()-1);
                result.append("<a href=\"" + kbHref + parentClass + suffix + "\">" 
                              + termFormat 
                              + "</a>");                      
            }
        }
        result.append("</td><td valign=\"top\" class=\"cell\">");
        ArrayList docForms = kb.askWithRestriction(0,"documentation",1,term);
        if (docForms != null && docForms.size() > 0) {
            Formula f = (Formula) docForms.get(0);
            String docString = f.getArgument(3);
            docString = kb.formatDocumentation(kbHref,docString,language);
            docString = StringUtil.removeEnclosingQuotes(docString);
            // docString = StringUtil.escapeQuoteChars(docString);
            // docString = docString.replace("\\\"","\"");
            result.append(docString); 
        }
        result.append("</td><td valign=\"top\" class=\"cell\">");
        if (indent > 0)        
            result.append(showCardinalityCell(kb,kbHref,term,""));
        result.append("</td><td valign=\"top\" class=\"cell\">");
        ArrayList forms = kb.askWithRestriction(0,"ddex_HasDataType",1,term);
        if (forms != null && forms.size() > 0) {
            Formula f = (Formula) forms.get(0);
            String dataTypeName = f.getArgument(2);
            dataTypeName = showTermName(kb,dataTypeName,language);                
            result.append("<a href=\"" + kbHref + f.getArgument(2) + suffix + "\">" 
                          + dataTypeName 
                          + "</a>");        
        }
        result.append("</td></tr>\n");
        // }
        return result.toString();
    }

    /** ***************************************************************
     *  If a term has a termFormat in the given language, display
     *  that, otherwise, show its termFormat for English, otherwise
     *  just display the term name.
     */
    public String showTermName(KB kb, String term, String language) {

        String ans = term;
        try {
            ans = StringUtil.removeEnclosingQuotes(ans);

            String termFormat = (String) kb.getTermFormatMap(language).get(term);

            if (StringUtil.emptyString(termFormat)) {
                termFormat = (String) kb.getTermFormatMap("EnglishLanguage").get(term);
            }
            if (StringUtil.isNonEmptyString(termFormat)) {
                ans = StringUtil.removeEnclosingQuotes(termFormat);
            }
            if (getIsoCodes(kb).contains(term)) {  //(term, "ddex_IsoCode")) {
                int idx = ans.indexOf(DB.W3C_NS_DELIMITER);
                if (idx < 0) {
                    idx = ans.indexOf(DB.KIF_NS_DELIMITER);
                }
                if (idx >= 0) {
                    ans = ans.substring(idx + 1);
                }
            }
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
        return ans;
    }


    /** ***************************************************************
     */
    private String formatCompositeHierarchy(KB kb, 
                                            String kbHref, 
                                            ArrayList hier, 
                                            String language) {

        StringBuffer result = new StringBuffer();
        for (int i = 0; i < hier.size(); i++) {
            AVPair avp = (AVPair) hier.get(i);
            // if (isLegalForDisplay(kb, avp.attribute)) {
            result.append(createCompositeComponentLine(kb,kbHref,avp.attribute,Integer.parseInt(avp.value),language));
            // }
        }
        return result.toString();
    }

    /** ***************************************************************
     *  Don't display XmlChoice(s) or XmlSequence(s)
     */
    private ArrayList createCompositeRecurse(KB kb, String term, int indent) {

        ArrayList result = new ArrayList();
        AVPair avp = new AVPair();
        avp.attribute = term;
        avp.value = Integer.toString(indent);
        result.add(avp);
        ArrayList forms = kb.askWithRestriction(0,"ddex_HasXmlAttribute",1,term);
        if (forms != null) {
            for (int i = 0; i < forms.size(); i++) {
                Formula form = (Formula) forms.get(i);
                String t = form.getArgument(2);
                // This should return without children since
                // attributes don't have child elements
                result.addAll(createCompositeRecurse(kb,t,indent+1));  
            }
        }
        forms = kb.askWithRestriction(0,"ddex_HasXmlElement",1,term);
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
    private String createContainingCompositeComponentLine(KB kb, 
                                                          String kbHref, 
                                                          String containingComp,
                                                          // List containerData,
                                                          String instance, 
                                                          int indent, 
                                                          String language) {
        StringBuffer result = new StringBuffer();

        /*
          System.out.println("DocGen.createContainingCompositeCompoentLine("
          + kb
          + ", \"" + kbHref
          + "\", \"" + containingComp
          + "\", \"" + instance
          + "\", " + indent
          + "\", \"" + language + "\")");
        */

        if (StringUtil.isNonEmptyString(instance)) {

            String suffix = "";
            if (StringUtil.emptyString(kbHref)) 
                suffix = ".html";

            /*              */
            ArrayList docForms = kb.askWithRestriction(0,"documentation",1,instance);
            if (docForms != null && docForms.size() > 0) {
                for (int i = 0; i < docForms.size(); i++) {
                    Formula f = (Formula) docForms.get(i);
                    String context = f.getArgument(2);
                    if (context.equals(containingComp)) {

                        result.append("<tr><td></td><td valign=\"top\" class=\"cell\">");
                        result.append(indentChars("&nbsp;&nbsp;",indent));
                        result.append("<a href=\"" 
                                      + kbHref  
                                      + containingComp 
                                      + suffix + "\">"
                                      + showTermName(kb,containingComp,language) 
                                      // + "(" + instance + ")"
                                      + "</a>");        
                        result.append("</td><td valign=\"top\" class=\"cell\">");
                        String docString = f.getArgument(3);
                        docString = kb.formatDocumentation(kbHref,docString,language);
                        docString = StringUtil.removeEnclosingQuotes(docString);
                        // docString = StringUtil.escapeQuoteChars(docString);
                        // docString = docString.replace("\\\"","\"");
                        result.append(docString);                
                        result.append("</td><td valign=\"top\" class=\"cell\">");
                        result.append(showCardinalityCell(kb,kbHref,instance,context));
                        result.append("</td><td>");
                        result.append("</td></tr>\n");
                    }
                }
            }
        }
        String resultStr = result.toString();
        // System.out.println("  ==> " + resultStr);
        return resultStr;
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
    private String formatContainingComposites(KB kb, 
                                              String kbHref, 
                                              ArrayList containing, 
                                              String composite, 
                                              String language) {

        String resultStr = "";
        try {
            StringBuffer result = new StringBuffer();
            /*        */
            // ArrayList instances = null;  //dataTypes = null;

            // dataTypes = kb.askWithRestriction(0,"ddex_HasDataType",2,composite);
            /*
              ArrayList instances = getFirstSpecificTerms(kb, composite);
              if ((instances != null) && !instances.isEmpty()) {
              String inst = null;
              String compos = null;
            */

            List instances = getFirstSpecificTerms(kb, composite);

            String ccomp = null;
            for (int k = 0; k < containing.size(); k++) {
                ccomp = (String) containing.get(k);
                // for (int j = 0; j < instances.size(); j++) {
                // String displayName = (String) containerData.get(1);
                // String containingComp = (String) containing.get(i);

                // if (!displayName.matches(".*(?i)xmlsequence.*")
                //   && !displayName.matches(".*(?i)xmlchoice.*")) {
                String instance = null;
                for (int i = 0; i < instances.size(); i++) {
                    instance = (String) instances.get(i);
                    result.append(createContainingCompositeComponentLine(kb,
                                                                         kbHref,
                                                                         ccomp, 
                                                                         instance,
                                                                         0,
                                                                         language));
                }
            }

            /*
              System.out.println("DocGen.formatContainingComposite(" + kb 
              + ", \"" + kbHref + "\", " + containing + ", \""
              + composite + "\", \"" + language + "\")");
              System.out.println(" => \"" + resultStr + "\"");
            */
            resultStr = result.toString();
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
        return resultStr;
    }

    private static boolean isSkipNode(KB kb, String term) {
        try {
            /*   */
            String[] nodes = {"ddex_XmlSequence", "ddex_XmlChoice"};
            for (int i = 0; i < nodes.length; i++) {
                if (kb.isInstanceOf(term, nodes[i])) {
                    return true;
                }
            }
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
        return false;
    }

    /** ***************************************************************
     * Travel up the HasXmlElement and HasXmlAttribute relation
     * hierarchies to collect all parents.
     *
     */
    private ArrayList containingComposites(KB kb, String term) {
        ArrayList result = new ArrayList();
        try {
            List bt = (List) compositesBT.get(term);
            if (bt != null) {

                // System.out.println();
                // System.out.println("Containing composites for " + term + ": " + bt);
                // System.out.println();

                result.addAll(bt);
            }
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
        return result;
    }

    /** ***************************************************************
     */
    private String createBelongsToClass(KB kb, String kbHref, String term, String language) {

        String suffix = "";
        if (StringUtil.emptyString(kbHref)) 
            suffix = ".html";
        StringBuffer result = new StringBuffer();

        String className = getFirstGeneralTerm(kb, term);
        List classes = new ArrayList();
        if (StringUtil.isNonEmptyString(className) && isLegalForDisplay(kb, className)) {
            classes.add(className);
        }

        if (!classes.isEmpty()) {
            Collections.sort(classes, String.CASE_INSENSITIVE_ORDER);
            result.append("<tr><td valign=\"top\" class=\"label\">Belongs to class</td><td valign=\"top\" class=\"cell\">\n");
            Iterator it = classes.iterator();
            String cl = null;
            for (int j = 0; it.hasNext(); j++) {
                if (j > 0) { result.append("<br>"); }
                cl = (String) it.next();
                result.append("<a href=\"" + kbHref + cl + suffix + "\">" 
                              + showTermName(kb,cl,language) 
                              + "</a>");        
            }
            result.append("</td><td></td><td></td><td></td></tr>\n");
        }
        return result.toString();
    }

    /** ***************************************************************
     *  Iterate through all the &%ddex_HasDataType relations for the
     *  composite to collect the instances of this composite.  Then
     *  call containingComposite() travel up the hasXmlElement
     *  relations for those instances to find their containing
     *  composites (if any).
     */
    private ArrayList findContainingComposites(KB kb, String term) {

        ArrayList ans = new ArrayList();
        /*          */
        try {
            if (StringUtil.isNonEmptyString(term)) {
                List accumulator = containingComposites(kb, term);
                if (accumulator.isEmpty()) {
                    accumulator.addAll(getFirstSpecificTerms(kb, term));
                }
                List working = new ArrayList();
                String term2 = null;
                String term3 = null;
                while (!accumulator.isEmpty()) {
                    working.clear();
                    working.addAll(accumulator);
                    accumulator.clear();
                    for (int i = 0; i < working.size(); i++) {
                        term2 = (String) working.get(i);
                        List compArr1 = containingComposites(kb, term2);
                        for (int j = 0; j < compArr1.size(); j++) {
                            term3 = (String) compArr1.get(j);
                            if (isSkipNode(kb, term3)) {
                                accumulator.add(term3);
                            }
                            if (!ans.contains(term3)) {
                                ans.add(term3);                            
                            }
                        }
                    }
                }
            }
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
        return ans;
    }

    /** ***************************************************************
     * Create an HTML page that lists information about a particular term,
     * with a limit on how many statements of each type should be
     * displayed.
     */
    public String createAllStatements(KB kb, 
                                      String kbHref, 
                                      String term, 
                                      int limit) {

        StringBuffer result = new StringBuffer();

        String language = "EnglishLanguage";
        int localLimit = limit;
        String limitString = "";
        for (int argnum = 2; argnum < 6; argnum++) {
            localLimit = limit;
            limitString = "";
            ArrayList forms = null;
            if (StringUtil.isNonEmptyString(term)) {
                forms = kb.ask("arg",argnum,term);
            }
            if (forms != null) {
                if (forms.size() < localLimit) 
                    localLimit = forms.size();
                else
                    limitString = ("<br>Display limited to " 
                                   + (new Integer(localLimit)).toString() 
                                   + " statements of each type.<P>\n");
                for (int i = 0; i < localLimit; i++) {
                    Formula form = (Formula) forms.get(i);
                    result.append(LanguageFormatter.htmlParaphrase(kbHref,form.theFormula, 
                                                                   kb.getFormatMap(language), 
                                                                   kb.getTermFormatMap(language), 
                                                                   kb,language) 
                                  + "<br>\n");
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
                limitString = ("<br>Display limited to " 
                               + (new Integer(localLimit)).toString() 
                               + " statements of each type.<P>\n");
            for (int i = 0; i < localLimit; i++) {
                Formula form = (Formula) forms.get(i);
                result.append(LanguageFormatter.htmlParaphrase(kbHref,
                                                               form.theFormula, 
                                                               kb.getFormatMap(language), 
                                                               kb.getTermFormatMap(language), 
                                                               kb,
                                                               language) + "\n");
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
                limitString = ("<br>Display limited to " 
                               + (new Integer(localLimit)).toString() 
                               + " statements of each type.<P>\n");
            for (int i = 0; i < localLimit; i++) {
                Formula form = (Formula) forms.get(i);
                result.append(LanguageFormatter.htmlParaphrase(kbHref,
                                                               form.theFormula, 
                                                               kb.getFormatMap(language), 
                                                               kb.getTermFormatMap(language), 
                                                               kb,
                                                               language) + "\n");
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
                limitString = ("<br>Display limited to " 
                               + (new Integer(localLimit)).toString() 
                               + " statements of each type.<P>\n");
            for (int i = 0; i < localLimit; i++) {
                Formula form = (Formula) forms.get(i);
                result.append(LanguageFormatter.htmlParaphrase(kbHref,
                                                               form.theFormula, 
                                                               kb.getFormatMap(language), 
                                                               kb.getTermFormatMap(language), 
                                                               kb,
                                                               language) + "<br>\n");
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
                limitString = ("<br>Display limited to " 
                               + (new Integer(localLimit)).toString() 
                               + " statements of each type.<P>\n");
            for (int i = 0; i < localLimit; i++) {
                Formula form = (Formula) forms.get(i);
                result.append(LanguageFormatter.htmlParaphrase(kbHref,
                                                               form.theFormula, 
                                                               kb.getFormatMap(language), 
                                                               kb.getTermFormatMap(language), 
                                                               kb,
                                                               language) + "<br>\n");
            }
        }
        result.append(limitString);
        if (result.length() > 0) { 
            // note that the following 3 lines are inserted in reverse order
            result.insert(0,"</td></tr></table><P>");
            result.insert(0,"<tr><td valign=\"top\" class=\"cell\">These statements express (potentially complex) facts about the term, " +
                          "and are automatically generated.</td></tr>\n<tr><td valign=\"top\" class=\"cell\">");
            result.insert(0,"<P><table><tr><td valign=\"top\" class=\"label\"><b>Other statements</b></td></tr>");
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
    public String createCompositePage(KB kb, 
                                      String kbHref, 
                                      String term, 
                                      TreeMap alphaList, 
                                      int limit, 
                                      String language) {

        StringBuffer result = new StringBuffer();
        StringBuffer sb1 = new StringBuffer();

        if (!StringUtil.emptyString(kbHref)) 
            result.append(generateDynamicTOCHeader(kbHref));
        else
            result.append(generateTOCHeader(alphaList,kb.name + "-AllTerms.html"));
        result.append("<table width=\"100%\">");
        result.append("<tr bgcolor=#DDDDDD>");
        result.append("<td valign=\"top\" class=\"cell\"><font size=+2>");

        result.append(showTermName(kb,term,language));
        result.append("</font></td></tr>\n<tr>");
        result.append(createDocs(kb,kbHref,term,language));
        result.append("</table><P>\n");

        result.append(HTMLformatter.htmlDivider);
        result.append("<table>");
        result.append(createSynonyms(kb,kbHref,term));

        ArrayList superComposites = findContainingComposites(kb,term); 
        Collections.sort(superComposites, String.CASE_INSENSITIVE_ORDER);
        sb1.append(createHasSameComponents(kb,kbHref,term,language));

        if ((sb1.length() > 0) 
            || ((superComposites != null) && !superComposites.isEmpty())
            || hasSubComponents(kb, term)) {

            result.append("<tr class=\"title_cell\"><td valign=\"top\" class=\"label\">Component Structure</td><td valign=\"top\" colspan=4></td></tr>");

            if (sb1.length() > 0) {
                result.append(sb1);
                sb1.setLength(0);
            }
            // result.append(createHasSameComponents(kb,kbHref,term,language));
            if ((superComposites != null) && !superComposites.isEmpty()) {
                Collections.sort(superComposites, String.CASE_INSENSITIVE_ORDER);
                result.append("<tr><td valign=\"top\" class=\"label\">Member of Composites</td><td valign=\"top\" class=\"title_cell\">Composite Name</td>");
                result.append("<td valign=\"top\" class=\"title_cell\">Description of Element Role</td><td valign=\"top\" class=\"title_cell\">Cardinality</td><td></td></tr>\n");
                result.append(formatContainingComposites(kb,kbHref,superComposites,term,language));
            }

            if (hasSubComponents(kb, term)) {
                result.append("<tr><td valign=\"top\" class=\"label\">Components</td>" +
                              "<td valign=\"top\" class=\"title_cell\">Name</td>" +
                              "<td valign=\"top\" class=\"title_cell\">Description of Element Role</td>" +
                              "<td valign=\"top\" class=\"title_cell\">Cardinality</td>" +
                              "<td valign=\"top\" class=\"title_cell\">Data Type</td></tr>\n");

                ArrayList hier = createCompositeRecurse(kb, term, 0);
                // No need to show the composite itself.
                hier.remove(0); 
                /*
                  int hlen = hier.size();
                  AVPair avp = null;
                  for (int i = 0; i < hlen; i++) {
                  avp = (AVPair) hier.remove(0);
                  if (!isSkipNode(kb, avp.attribute)) {
                  hier.add(avp);
                  }
                  } 
                */                   
                result.append(formatCompositeHierarchy(kb,kbHref,hier,language));
            }
        }
        sb1.append(createBelongsToClass(kb,kbHref,term,language));
        sb1.append(createUsingSameComponents(kb,kbHref,term,language));
        if (sb1.length() > 0) {
            result.append("<tr class=\"title_cell\"><td valign=\"top\" class=\"label\">Relationships</td><td></td><td></td><td></td><td></td></tr>\n");
            result.append(sb1);
            sb1.setLength(0);
        }
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
    public String createPage(KB kb, 
                             String kbHref, 
                             String term, 
                             TreeMap alphaList,
                             int limit, 
                             String language) {

        StringBuffer result = new StringBuffer();
        StringBuffer sb1 = new StringBuffer();
        StringBuffer sb2 = new StringBuffer();

        if (!StringUtil.emptyString(kbHref)) {
            kbHref = kbHref + "&term=";
            result.append(generateDynamicTOCHeader(kbHref));
        }
        else
            result.append(generateTOCHeader(alphaList,kb.name + "-AllTerms.html"));

        result.append("<table width=\"100%\">");
        result.append("<tr bgcolor=#DDDDDD>");
        result.append("<td valign=\"top\" class=\"cell\"><font size=+2>");

        result.append(showTermName(kb,term,language));
        result.append("</font></td></tr>\n<tr>");
        result.append(createDocs(kb,kbHref,term,language));
        result.append("</table><P>\n");
        result.append("<table width=\"100%\">");
        result.append(createSynonyms(kb,kbHref,term));
        result.append(createComments(kb,kbHref,term,language));
        
        sb1.append(createParents(kb,kbHref,term,language));
        sb2.append(createChildren(kb,kbHref,term,language));

        if ((sb1.length() > 0) || (sb2.length() > 0)) {            
            result.append("<tr class=\"title_cell\"><td valign=\"top\" class=\"label\">Relationships</td><td>&nbsp;</td><td>&nbsp;</td></tr>\n");
            result.append(sb1);
            sb1.setLength(0);
            result.append(sb2);
            sb2.setLength(0);
        }

        ArrayList superComposites = findContainingComposites(kb,term); 
        Collections.sort(superComposites, String.CASE_INSENSITIVE_ORDER);

        // if ((superComposites != null) && !superComposites.isEmpty()) {
        // superComposites are excluded terms that will appear in the
        // member of composites section.

        result.append(createInstances(kb,kbHref,term,language,superComposites));
        // result.append(createInstances(kb,kbHref,term,language,superComposites));  
        // }
                                                                   
        result.append(createRelations(kb,kbHref,term,language));
        result.append(createUsingSameComponents(kb,kbHref,term,language));
        result.append(createBelongsToClass(kb,kbHref,term,language));

        if ((superComposites != null) && !superComposites.isEmpty()) {

            result.append("<tr><td valign=\"top\" class=\"label\">Member of Composites</td><td valign=\"top\" class=\"title_cell\">Composite Name</td>");
            result.append("<td valign=\"top\" class=\"title_cell\">Description of Element Role</td><td valign=\"top\" class=\"title_cell\">Cardinality</td><td></td></tr>\n");
 
            result.append(formatContainingComposites(kb,kbHref,superComposites,term,language));
        }

        result.append("</table>\n");

        result.append(HTMLformatter.htmlDivider);

        // result.append(createAllStatements(kb,kbHref,term,limit));
        return result.toString();
    }

    /** ***************************************************************
     *  Generate an alphabetic HTML list that points to the
     *  individual index pages (which collect all terms starting
     *  with a particular letter.
     */
    private String generateDynamicTOCHeader(String kbHref) {

        StringBuffer result = new StringBuffer();
        result.append("<table width=\"100%\"><tr>");
        for (char c = 65; c < 91; c++) {
            String cString = Character.toString(c);   
            result.append("<td valign=top><a href=\"" + kbHref + "&term=" + cString + "*\">" 
                          + cString 
                          + "</a></td>\n");            
        }
        result.append("</tr></table>");
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
    private String generateTOCHeader(TreeMap alphaList, String allname) {

        if (StringUtil.isNonEmptyString(TOCheader)) 
            return TOCheader;
        StringBuffer result = new StringBuffer();
        result.append("<head><META http-equiv=\"Content-Type\" content=\"text/html; charset=utf-8\">" +
                      "<link rel=\"stylesheet\" type=\"text/css\" href=\"simple.css\"></head><body>\n");

        result.append(this.header);
        result.append("<table width=\"100%\"><tr><td valign=\"top\" colspan=\"35\" class=\"title\">\n");
        result.append(this.header + "</td></tr><tr class=\"letter\">\n");
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
    private String generateTOCPage(KB kb, String firstLetter, TreeMap alphaList, String language) {

        /*
          System.out.println("INFO in generateTOCPage(" + kb 
          + ", \"" + firstLetter + "\", "
          + "alphaList" 
          + " \"" + language + "\")");
        */

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
                    docString = StringUtil.removeEnclosingQuotes(docString);
                    // docString = StringUtil.escapeQuoteChars(docString);
                    // docString = docString.replace("\\\"","\"");
                    if (docString.length() > 1)                 
                        result.append("<td valign=\"top\" class=\"cell\">" 
                                      + docString
                                      + "</td>\n");
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
    private String saveIndexPages(KB kb, 
                                  TreeMap alphaList, 
                                  String dir, 
                                  String language) throws IOException {

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
    private void printHTMLPages(TreeMap pageList, String dir) throws IOException {

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
    private String generateDir() throws IOException {

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
                    ans = StringUtil.removeEnclosingQuotes(f.getArgument(1));
                    break;
                }
            }
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
        return ans;
    }

    /** ***************************************************************
     *  @return a HashMap where the keys are the term names and the
     *  values are the "headwords" (with quotes removed).
     */
    public HashMap createHeadwordMap(KB kb) {

        HashMap result = new HashMap();
        ArrayList headwordForms = kb.ask("arg",0,"headword");
        if (headwordForms != null && headwordForms.size() > 0) {
            for (int i = 0; i < headwordForms.size(); i++) {
                Formula f = (Formula) headwordForms.get(i);
                String term = f.getArgument(2);
                String headword = StringUtil.removeEnclosingQuotes(f.getArgument(1));
                result.put(term,headword);
            }
        }
        return result;
    }

    /** ***************************************************************
     *  @return a HashMap where the keys are the headwords and the
     *  values are ArrayLists of term names (since headwords are not
     *  unique identifiers for terms). Don't put automatically
     *  created instances in the map. If there's no headword, use
     *  the term name. This map is the inverse of headwordMap. @see
     *  DB.stringToKifId()
     */
    public HashMap createInverseHeadwordMap(KB kb, HashMap headwordMap) {

        HashMap result = new HashMap();

        Iterator it = kb.terms.iterator();  
        while (it.hasNext()) {
            String term = (String) it.next();
            // Don't display automatically created instances
            if (term.indexOf("LocalInstance") > -1) 
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
    private TreeMap generateHTMLPages(KB kb, 
                                      TreeMap alphaList,
                                      HashMap inverseHeadwordMap, 
                                      String language) {
        TreeMap pageList = new TreeMap();
        TreeSet rejectedTerms = new TreeSet();
        Iterator it;
        it = inverseHeadwordMap.keySet().iterator();
        int count = 0;
        while (it.hasNext()) {
            String formattedTerm = (String) it.next();
            ArrayList termNames = (ArrayList) inverseHeadwordMap.get(formattedTerm);
            for (int i = 0; i < termNames.size(); i++) {
                String realTermName = (String) termNames.get(i);
                if (isLegalForDisplay(kb, realTermName)) {
                    if (isCompositeInstance(kb, realTermName)) {
                        pageList.put(realTermName,
                                     createCompositePage(kb,
                                                         "",
                                                         realTermName,
                                                         alphaList,
                                                         200,
                                                         language));
                    }
                    else {
                        pageList.put(realTermName,createPage(kb,
                                                             "",
                                                             realTermName,
                                                             alphaList,
                                                             200,
                                                             language));
                    }
                    if ((count++ % 100) == 1) { System.out.print("."); }
                }
                else {
                    rejectedTerms.add(realTermName);
                }
            }
        }
        System.out.println(rejectedTerms.size() + " terms rejected");
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
    public TreeMap createAlphaList(KB kb, HashMap stringMap) {

        TreeMap alphaList = new TreeMap();  

        System.out.println("INFO in DocGen.createAlphaList()");
        Iterator it = kb.terms.iterator();
        while (it.hasNext()) {
            String term = (String) it.next();
            if (isLegalForDisplay(kb, term)
                && !getIsoCodes(kb).contains(term)
                // && !term.matches("^iso\\d+.*_.+")
                ) {
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
    public void generateHTML(KB kb, String language, boolean s) {

        TreeMap pageList = new TreeMap();   // Keys are formatted term names, values are HTML pages
        TreeMap termMap = new TreeMap();    // Keys are headwords, values are terms        

        HashMap headwordMap = createHeadwordMap(kb); // A HashMap where the keys are the term names and the
                                                     // values are "headwords" (with quotes removed).
                                                     
        TreeMap alphaList = new TreeMap();       // a TreeMap of TreeMaps of ArrayLists.  @see createAlphaList()

        try {

            String context = toKifNamespace(kb, language);

            buildCompositeCaches(kb);

            simplified = s;                 // Display term format expressions instead of term names
            alphaList = createAlphaList(kb,headwordMap);

            // Headword keys and ArrayList values (since the same headword can
            // be found in more than one term)
            HashMap inverseHeadwordMap = createInverseHeadwordMap(kb,headwordMap);  

            System.out.println("INFO in DocGen.generateHTML(): generating alpha list");
            String dir = generateDir();
            System.out.println("INFO in DocGen.generateHTML(): saving index pages");
            saveIndexPages(kb,alphaList,dir,context);

            System.out.println("INFO in DocGen.generateHTML(): generating HTML pages");
            pageList = generateHTMLPages(kb,alphaList,inverseHeadwordMap,context);
            printHTMLPages(pageList,dir);

            System.out.println("INFO in DocGen.generateHTML(): creating single index page");
            generateSingleHTML(kb, dir, alphaList, context, s);
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
        return;
    } 

    /** ***************************************************************
     * Generate a single HTML page showing all terms.
     * @param alphaList a TreeMap of TreeMaps of ArrayLists.
     *                  @see createAlphaList()
     * 
     *      letter->    formattedTerm1->term11,term12...term1N
     *                  formattedTerm2->term21,term22...term2N
     */
    public void generateSingleHTML(KB kb, String dir, TreeMap alphaList,
                                   String language, boolean s) throws IOException {

        simplified = s;                // display term format expressions for term names
        FileWriter fw = null;
        PrintWriter pw = null; 
        try {
            fw = new FileWriter(dir + File.separator + kb.name+ "-AllTerms.html");
            pw = new PrintWriter(fw);
            pw.println("<head><META http-equiv=\"Content-Type\" content=\"text/html; charset=utf-8\">" +
                       "<link rel=\"stylesheet\" type=\"text/css\" href=\"simple.css\"></head><body>\n");
            pw.println(this.header);
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
                            docString = StringUtil.removeEnclosingQuotes(docString); 
                            // docString = StringUtil.escapeQuoteChars(docString);
                            // docString = docString.replace("\\\"","\"");
                            pw.println("<td valign=\"top\" class=\"description\">" + docString);
                        }
                        else
                            pw.println("<td>");
                        pw.println("</td></tr>\n");
                    }
                }
            }
            pw.println("</table>\n");
            pw.println(this.footer);
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
    public void generateSingleHTML(KB kb, String language, boolean s) throws IOException {

        HashMap headwordMap = createHeadwordMap(kb); 
        String dir = generateDir();
        TreeMap alphaList = createAlphaList(kb,headwordMap);
        generateSingleHTML(kb, dir, alphaList, language, s);
    }


    /** ***************************************************************
     */
    public void capitalizationAlternates(TreeSet alts)  {

        TreeSet allCaps = new TreeSet();
        Iterator it = alts.iterator();
        while (it.hasNext()) {
            String term = (String) it.next();
            String capTerm = term.toUpperCase();
            if (allCaps.contains(capTerm)) 
                System.out.println(term);
            else
                allCaps.add(capTerm);
        }
    }

    /** ***************************************************************
     *  @return a TreeSet of all the KB terms that are used in or
     *          support XML messages
     */
    public TreeSet generateMessageTerms(KB kb) {

        TreeSet result = new TreeSet();

        // (1) Collect all terms that are instances of Composite.

        TreeSet composites = kb.getAllInstances("ddex_Composite");
        result.addAll(composites);
        //TreeSet nonComposites = new TreeSet();
        //nonComposites.addAll(kb.terms);
        //nonComposites.removeAll(composites);
        System.out.println("results from (1)");
        System.out.println(result);

        // (1a) Collect all the parent classes of those instances.        
        if (composites != null) {
            Iterator it = composites.iterator();
            while (it.hasNext()) {
                String term = (String) it.next();
                ArrayList forms = kb.askWithPredicateSubsumption("instance", 1, term);

                // System.out.println("9. forms == " + forms);

                if (forms != null) {
                    for (int i = 0; i < forms.size(); i++) {
                        Formula f = (Formula) forms.get(i);
                        String elem = f.getArgument(2);
                        result.add(elem);
                    }
                }
            }
        }
        System.out.println("results plus (1a)");
        System.out.println(result);

        // (2) Add all terms that are classes of the second argument of 
        // ddex_HasXMLElement relationships.
        TreeSet step2 = new TreeSet();
        ArrayList forms = kb.ask("arg",0,"ddex_HasXmlElement");
        if (forms != null) {
            for (int i = 0; i < forms.size(); i++) {
                Formula f = (Formula) forms.get(i);
                String elem = f.getArgument(2);

                ArrayList classes = kb.askWithPredicateSubsumption("instance", 1, elem);

                // System.out.println("10. classes == " + classes);

                for (int j = 0; j < classes.size(); j++) {
                    Formula f2 = (Formula) classes.get(j);
                    String c = f2.getArgument(2);
                    result.add(c);
                    step2.add(c);
                }
            }
        }

        System.out.println("step (2)");
        System.out.println(step2);

        // (3) Add all terms that are classes of the second argument of 
        // ddex_HasXMLAttribute relationships.
        TreeSet step3 = new TreeSet();
        forms = kb.ask("arg",0,"ddex_HasXmlAttribute");

        if (forms != null) {
            for (int i = 0; i < forms.size(); i++) {
                Formula f = (Formula) forms.get(i);
                String arg = f.getArgument(2);

                ArrayList classes = kb.askWithPredicateSubsumption("instance", 1, arg);

                // System.out.println("11. classes == " + classes);

                for (int j = 0; j < classes.size(); j++) {
                    Formula f2 = (Formula) classes.get(j);
                    String c = f2.getArgument(2);
                    result.add(c);
                    step3.add(c);
                }
            }
        }

        System.out.println("step (3)");
        System.out.println(step3);

        TreeSet step4 = new TreeSet();
        TreeSet step5 = new TreeSet();
        ArrayList forStepSeven = new ArrayList();
        // (4) Add all terms that are linked to the Composites of list
        // (1) by an ddex_IsXmlExtensionOf relationship.
        if (composites != null) {
            Iterator it = composites.iterator();
            while (it.hasNext()) {            
                String arg = (String) it.next();;

                ArrayList classes = kb.askWithRestriction(0,"ddex_IsXmlExtensionOf",1,arg);
                for (int j = 0; j < classes.size(); j++) {
                    Formula f2 = (Formula) classes.get(j);
                    String c = f2.getArgument(2);
                    result.add(c);
                    forStepSeven.add(c);
                    step4.add(c);
                }

                // (5) Add all terms that are linked to the Composites
                // of list (1) by an IsXmlCompositeFor relationship.
                classes = kb.askWithRestriction(0,"ddex_IsXmlCompositeFor",1,arg);
                for (int j = 0; j < classes.size(); j++) {
                    Formula f2 = (Formula) classes.get(j);
                    String c = f2.getArgument(2);
                    result.add(c);
                    step5.add(c);
                }
            }
        }

        System.out.println("step (4)");
        System.out.println(step4);
        System.out.println("step (5)");
        System.out.println(step5);

        // (6) Add all terms that are the second argument of
        // ddex_HasDataType relationships.
        
        TreeSet step6 = new TreeSet();
        forms = kb.ask("arg",0,"ddex_HasDataType");
        if (forms != null) {
            for (int i = 0; i < forms.size(); i++) {
                Formula f = (Formula) forms.get(i);
                String arg = f.getArgument(2);
                if (!composites.contains(arg)) {
                    result.add(arg);
                    forStepSeven.add(arg);
                    step6.add(arg);
                }
            }
        }
        System.out.println("step (6)");
        System.out.println(step6);

        // (7) Add all terms that are the first argument of
        // instance/isOneOf relationships where the second argument is
        // from list (4) or (6).

        TreeSet step7 = new TreeSet();
        if (forStepSeven != null) {
            for (int i = 0; i < forStepSeven.size(); i++) {
                String arg = (String) forStepSeven.get(i);
                // ArrayList classes = kb.askWithRestriction(0,"instance",2,arg);
                ArrayList classes = kb.askWithPredicateSubsumption("instance", 2, arg);

                // System.out.println("12. classes == " + classes);

                for (int j = 0; j < classes.size(); j++) {
                    Formula f2 = (Formula) classes.get(j);
                    String c = f2.getArgument(1);
                    if (!c.contains("_LocalInstanceLeaf")) {
                        result.add(c);
                        step7.add(c);
                    }
                }
            }
        }

        System.out.println("step (7)");
        System.out.println(step7);

        // (8) Get all terms where their headword (quoted string) is also the 
        // third argument in a termFormat expression of another term. For example, 
        // include ddex_AudioBitRate in the list, from 
        // (termFormat ddex_hw ddex_AudioBitRate "AudioBitRate")
        // (termFormat Ern_TechnicalVideoDetails DDEX_LocalInstanceLeaf12_156 "AudioBitRate")         

        // (9) More complicated is also the list of terms that have a third 
        // argument of a termFormat relationship that has ddex_hw as a 
        // pseudo-language, if that string is also a third argument of another 
        // termFormat relationship that has one of the subnamespaces as a 
        // pseudo-language (e.g. DdexC_hw).        
        TreeSet step89 = new TreeSet();
        forms = kb.askWithRestriction(0,"termFormat",1,"ns_ddex_hw");
        forms.addAll(kb.askWithRestriction(0,"termFormat",1,"ns_ddexC_hw"));
        if (forms != null) {
            for (int i = 0; i < forms.size(); i++) {
                Formula f = (Formula) forms.get(i);
                String term = f.getArgument(2);
                String st = f.getArgument(3);
                ArrayList forms2 = kb.ask("arg",0,"termFormat");
                for (int j = 0; j < forms2.size(); j++) {
                    Formula f2 = (Formula) forms2.get(j);
                    String namespace = f2.getArgument(1);
                    String st2 = f2.getArgument(3);
                    if (st.equals(st2) 
                        && !namespace.equals("ns_ddex_hw") 
                        && !namespace.equals("ns_ddexC_hw")) {
                        result.add(term);
                        step89.add(term);
                    }
                }
            }
        }

        System.out.println("steps (8) and (9)");
        System.out.println(step89);

        // (10) NEW: We need to pick up the Relators IsA, IsSubClassOf
        // etc. (and their reciprocals) as FrameworkTerms. I have no
        // idea yet how to do this.

        TreeSet step10 = new TreeSet();
        TreeSet explicitList = new TreeSet();
        explicitList.add("instance");
        explicitList.add("ddex_IsA");
        explicitList.add("ddex_HasXmlElement"); 
        explicitList.add("ddex_HasXmlAttribute"); 
        explicitList.add("ddex_HasDataType"); 
        explicitList.add("ddex_HasSubClass"); 
        explicitList.add("ddex_HasSubRelator"); 
        explicitList.add("ddex_HasPart");
        explicitList.add("ddex_IsOneOf");
        explicitList.add("ddex_IsReciprocalOf"); 
        explicitList.add("ddex_IsXmlExtensionOf");
        explicitList.add("ddex_IsXmlCompositeFor"); 
        explicitList.add("ddex_IsXmlUnionOf"); 
        explicitList.add("ddex_IsXmlAttributeOf");
        explicitList.add("ddex_IsSubClassOf"); 
        explicitList.add("ddex_IsSubRelatorOf");
        explicitList.add("ddex_Flag");
        explicitList.add("ddex_File");

        if (explicitList != null) {
            Iterator it = explicitList.iterator();
            while (it.hasNext()) {
                String term = (String) it.next();
                forms = kb.askWithPredicateSubsumption("inverse", 1, term);

                // System.out.println("13. forms == " + forms);

                if (forms != null) {
                    for (int i = 0; i < forms.size(); i++) {
                        Formula f = (Formula) forms.get(i);
                        String elem = f.getArgument(2);
                        if (!result.contains(elem)) {
                            result.add(elem);
                            step10.add(elem);
                        }
                    }
                }
                forms = kb.askWithPredicateSubsumption("inverse", 2, term);

                // System.out.println("14. forms == " + forms);

                if (forms != null) {
                    for (int i = 0; i < forms.size(); i++) {
                        Formula f = (Formula) forms.get(i);
                        String elem = f.getArgument(1);
                        if (!result.contains(elem)) {
                            result.add(elem);
                            step10.add(elem);
                        }
                    }
                }
            }
        }
        result.addAll(explicitList);
        System.out.println("step (10)");
        System.out.println(step10);

        // (11) For all of these (1-7) add all terms that are parent
        // terms (using subrelation and subclass relationships)

        TreeSet step11 = new TreeSet();
        TreeSet newResult = new TreeSet();
        Iterator it = result.iterator();
        while (it.hasNext()) {
            String term = (String) it.next();
            newResult.add(term);            
            HashSet ps = (HashSet) kb.parents.get(term);
            if (ps != null) {          
                newResult.addAll(ps);
                step11.addAll(ps);
            }
        }
        // (1-3), (5-6) are MessageTerms
        // (4) are FrameworkTerms
        // (7-8) are SupportingTerms

        System.out.println("step (11)");
        System.out.println(step11);

        System.out.println("positive terms differing only in capitalization");
        capitalizationAlternates(newResult);

        return newResult;
    }

    /// BEGIN: code for XSD generation.

    private List ddexTermsToExclude = 
        Arrays.asList("ddex_CategoryType", "ddex_CharacteristicType", "ddex_ContactPersonRole",
                      "ddex_ContextType", "ddex_CreationType", "ddex_DescriptorType",
                      "ddex_EventType", "ddex_Genre", "ddex_IdentifierType", 
                      "ddex_IndirectResourceContributorRole", "ddex_InteractionType",
                      "ddex_LicenseContractPartyRole", "ddex_LicenseLevel", "ddex_NameType", 
                      "ddex_Namespace", "ddex_OperatingSystemType", "ddex_OutputType", 
                      "ddex_PartType", "ddex_Purpose", "ddex_QualityType", "ddex_QuantityType",
                      "ddex_SubGenre", "ddex_UserDefinedResourceType");

    private List ddexTermsToInclude = 
        Arrays.asList("ddex_HashSum", "ddex_NonIsoTerritoryCode", "ddex_SICI");

    private String[][][] ddexXsdImports = 
    { { {"namespace", "http://ddex.net/xml/XXX/iso3166a2"},
        {"schemaLocation", "iso3166a2.xsd"} },
      { {"namespace", "http://ddex.net/xml/XXX/iso4217a"},
        {"schemaLocation", "iso4217a.xsd"} },
      { {"namespace", "http://ddex.net/xml/XXX/iso639a2"},
        {"schemaLocation", "iso639a2.xsd"} }
    };


    /** *************************************************************
     * 
     */
    public void writeXsdFileForNamespace(KB kb, String namespace, String targetDirectory) {
        try {            
            String nsName = ((namespace.startsWith("ns_") || namespace.startsWith("ns:"))
                             ? namespace.substring(3)
                             : namespace); 
            String nsTerm = ("ns_" + nsName);
            List termsInNamespace = getTermsInNamespace(kb, nsTerm);
            List topLevelTerms = getXsdTopLevelTerms(kb, termsInNamespace);
            HashSet termsToInclude = new HashSet();
            HashSet termsToExclude = new HashSet();

            if (nsName.equalsIgnoreCase("ddex")) {
                termsToInclude.addAll(ddexTermsToInclude);
                termsToExclude.addAll(ddexTermsToExclude);
            }
            topLevelTerms.removeAll(termsToExclude);
            termsToInclude.addAll(topLevelTerms);
            topLevelTerms.clear();
            topLevelTerms.addAll(termsToInclude);

            if (!topLevelTerms.isEmpty()) {

                Collections.sort(topLevelTerms, String.CASE_INSENSITIVE_ORDER);

                String fs = System.getProperty("file.separator");
                File tDir = new File(targetDirectory);
                File outdir = tDir;
                if (!outdir.isDirectory()) {
                    outdir = new File(System.getProperty("user.dir"));
                }
                File outfile = new File(outdir, (nsName + ".xsd"));
                PrintWriter pw = new PrintWriter(new FileWriter(outfile));

                javax.xml.parsers.DocumentBuilderFactory _dbf = 
                    javax.xml.parsers.DocumentBuilderFactory.newInstance();
                _dbf.setNamespaceAware(true);
                javax.xml.parsers.DocumentBuilder _db = _dbf.newDocumentBuilder();
                DOMImplementation _di = _db.getDOMImplementation();
                Document _doc = _di.createDocument("http://www.w3.org/2001/XMLSchema", 
                                                   "xs:schema",
                                                   null);
                Element _docelem = _doc.getDocumentElement();
                String[][][] _imports = null;
                if (nsName.equalsIgnoreCase("ddex")) {
                    _imports = ddexXsdImports;
                }
                _docelem.setAttribute("targetNamespace", "http://ddex.net/xml/XXX/ddex");
                _docelem.setAttribute("elementFormDefault", "unqualified");
                _docelem.setAttribute("attributeFormDefault", "unqualified");
                _docelem.setAttributeNS("http://www.w3.org/2000/xmlns/",
                                        "xmlns:xs",
                                        "http://www.w3.org/2001/XMLSchema");
                _docelem.setAttributeNS("http://www.w3.org/2000/xmlns/",
                                        "xmlns:ddex",
                                        "http://ddex.net/xml/XXX/ddex");
                _docelem.setAttributeNS("http://www.w3.org/2000/xmlns/",
                                        "xmlns:iso639a2",
                                        "http://ddex.net/xml/XXX/iso639a2");
                _docelem.setAttributeNS("http://www.w3.org/2000/xmlns/",
                                        "xmlns:iso3166a2",
                                        "http://ddex.net/xml/XXX/iso3166a2");
                _docelem.setAttributeNS("http://www.w3.org/2000/xmlns/", 
                                        "xmlns:iso4217a",
                                        "http://ddex.net/xml/XXX/iso4217a");
                
                for (int i = 0; i < _imports.length; i++) {
                    Element _el = _doc.createElementNS("http://www.w3.org/2001/XMLSchema",
                                                       "xs:import");
                    String[][] attrPairs = _imports[i];
                    for (int j = 0; j < attrPairs.length; j++) {
                        String[] pair = attrPairs[j];
                        _el.setAttribute(pair[0], pair[1]);
                    }
                    _docelem.appendChild(_el);
                }
                Element _annotation = _doc.createElementNS("http://www.w3.org/2001/XMLSchema",
                                                           "xs:annotation");
                Element _documentation = _doc.createElementNS("http://www.w3.org/2001/XMLSchema",
                                                              "xs:documentation");
                _documentation.setTextContent("(c) 2006-2008 DDEX, LLC");
                _annotation.appendChild(_documentation);
                _docelem.appendChild(_annotation);

                Iterator it = topLevelTerms.iterator();
                String tlt = null;
                String tltName = null;
                String nsPrefix = null;
                while (it.hasNext()) {
                    tlt = (String) it.next();
                    tltName = showTermName(kb, tlt, nsTerm);
                    nsPrefix = getNamespacePrefix(kb, tltName);
                    if (StringUtil.isNonEmptyString(nsPrefix)) {
                        tltName = tltName.substring(nsPrefix.length());
                    }
                    ArrayList nsList = new ArrayList();
                    nsList.add(nsTerm);
                    String docustr = getCompositeDocumentation(kb, tlt, nsList);
                    docustr = removeLinkableNamespacePrefixes(kb, docustr);
                    docustr = StringUtil.removeEnclosingQuotes(docustr);
                    // docustr = StringUtil.escapeQuoteChars(docustr);
                    // docustr = docustr.replace("\"\"", "\\\"");
                    docustr = StringUtil.normalizeSpaceChars(docustr);
                    docustr = StringUtil.replaceNonAsciiChars(docustr);
                    List members = getAvsTypeMembers(kb, nsTerm, termsInNamespace, tlt);
                    Element _annotation2 = _doc.createElementNS("http://www.w3.org/2001/XMLSchema",
                                                                "xs:annotation");
                    Element _documentation2 = 
                        _doc.createElementNS("http://www.w3.org/2001/XMLSchema",
                                             "xs:documentation");
                    Element _simpleType = _doc.createElementNS("http://www.w3.org/2001/XMLSchema",
                                                               "xs:simpleType");
                    _simpleType.setAttribute("name", tltName);
                    _documentation2.setTextContent(docustr);
                    _annotation2.appendChild(_documentation2);
                    _simpleType.appendChild(_annotation2);
                    Element _restriction = _doc.createElementNS("http://www.w3.org/2001/XMLSchema",
                                                                "xs:restriction");
                    _restriction.setAttribute("base", "xs:string");
                    if (!members.isEmpty()) {
                        Iterator mIt = members.iterator();
                        String m = null;
                        String mName = null;
                        while (mIt.hasNext()) {
                            m = (String) mIt.next();
                            mName = stripNamespacePrefix(kb, m);  // showTermName(kb, m, nsTerm);
                            Element _enumeration = 
                                _doc.createElementNS("http://www.w3.org/2001/XMLSchema",
                                                     "xs:enumeration");
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
    public void writeXsdFileForNamespace2(KB kb, String namespace, String targetDirectory) {
        try {            
            String nsName = ((namespace.startsWith("ns_") || namespace.startsWith("ns:"))
                             ? namespace.substring(3)
                             : namespace); 
            String nsTerm = ("ns_" + nsName);
            List termsInNamespace = getTermsInNamespace(kb, nsTerm);
            List topLevelTerms = getXsdTopLevelTerms(kb, termsInNamespace);
            HashSet termsToInclude = new HashSet();
            HashSet termsToExclude = new HashSet();

            if (nsName.equalsIgnoreCase("ddex")) {
                termsToInclude.addAll(ddexTermsToInclude);
                termsToExclude.addAll(ddexTermsToExclude);
            }
            topLevelTerms.removeAll(termsToExclude);
            termsToInclude.addAll(topLevelTerms);
            topLevelTerms.clear();
            topLevelTerms.addAll(termsToInclude);

            if (!topLevelTerms.isEmpty()) {

                Collections.sort(topLevelTerms, String.CASE_INSENSITIVE_ORDER);

                // String fs = System.getProperty("file.separator");
                File tDir = new File(targetDirectory);
                File workdir = tDir;
                if (!workdir.isDirectory()) {
                    workdir = new File(System.getProperty("user.dir"));
                }

                // Give up if we can't load a skeleton document.
                Document _doc = loadDdexSkeletonFile(kb, nsTerm, workdir);
                if (_doc == null) {
                    System.out.println("Giving up.  The skeleton file for \"" 
                                       + namespace
                                       + "\" could not be loaded");
                    return;
                }

                Element _docelem = _doc.getDocumentElement();
                System.out.println("Document root element: " + _docelem.getNodeName());

                File outfile = new File(workdir, (nsName + ".xsd"));
                PrintWriter pw = new PrintWriter(new FileWriter(outfile));

                Iterator it = topLevelTerms.iterator();
                String tlt = null;
                String tltName = null;
                String nsPrefix = null;
                while (it.hasNext()) {
                    tlt = (String) it.next();
                    tltName = showTermName(kb, tlt, nsTerm);
                    nsPrefix = getNamespacePrefix(kb, tltName);
                    if (StringUtil.isNonEmptyString(nsPrefix)) {
                        tltName = tltName.substring(nsPrefix.length());
                    }
                    ArrayList nsList = new ArrayList();
                    nsList.add(nsTerm);
                    String docustr = getCompositeDocumentation(kb, tlt, nsList);
                    docustr = removeLinkableNamespacePrefixes(kb, docustr);
                    docustr = StringUtil.removeEnclosingQuotes(docustr);
                    // docustr = StringUtil.escapeQuoteChars(docustr);
                    // docustr = docustr.replace("\"\"", "\\\"");
                    docustr = StringUtil.normalizeSpaceChars(docustr);
                    docustr = StringUtil.replaceNonAsciiChars(docustr);
                    List members = getAvsTypeMembers(kb, nsTerm, termsInNamespace, tlt);
                    Element _annotation2 = _doc.createElementNS("http://www.w3.org/2001/XMLSchema",
                                                                "xs:annotation");
                    Element _documentation2 = 
                        _doc.createElementNS("http://www.w3.org/2001/XMLSchema",
                                             "xs:documentation");
                    Element _simpleType = _doc.createElementNS("http://www.w3.org/2001/XMLSchema",
                                                               "xs:simpleType");
                    _simpleType.setAttribute("name", tltName);
                    _documentation2.setTextContent(docustr);
                    _annotation2.appendChild(_documentation2);
                    _simpleType.appendChild(_annotation2);
                    Element _restriction = _doc.createElementNS("http://www.w3.org/2001/XMLSchema",
                                                                "xs:restriction");
                    _restriction.setAttribute("base", "xs:string");
                    if (!members.isEmpty()) {
                        Iterator mIt = members.iterator();
                        String m = null;
                        String mName = null;
                        while (mIt.hasNext()) {
                            m = (String) mIt.next();
                            mName = stripNamespacePrefix(kb, m);  // showTermName(kb, m, nsTerm);
                            Element _enumeration = 
                                _doc.createElementNS("http://www.w3.org/2001/XMLSchema",
                                                     "xs:enumeration");
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
    private TreeSet getDdexMessageTypes(KB kb) {
        TreeSet result = new TreeSet();
        try {
            List formulae = kb.askWithRestriction(0, "ddex_IsOneOf", 2, "ddex_MessageType");
            if (formulae != null) {
                Formula f = null;
                for (int i = 0; i < formulae.size(); i++) {
                    f = (Formula) formulae.get(i);
                    result.add(f.getArgument(1));
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
    private TreeMap getDdexMessageTypesByNamespace(KB kb, Set msgTypes) {
        TreeMap result = new TreeMap();
        try {
            if ((msgTypes != null) && !msgTypes.isEmpty()) {
                Iterator it = msgTypes.iterator();
                String term = null;
                String ns = null;
                List terms = null;
                while (it.hasNext()) {
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
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
        return result;
    }

    /** *************************************************************
     * 
     */
    private String getTermNamespace(KB kb, String term) {
        String result = "";
        try {
            if (StringUtil.isNonEmptyString(term)) {
                String prefix = getNamespacePrefix(kb, term);
                if (StringUtil.isNonEmptyString(prefix)) {
                    result = ("ns_" + prefix.substring(0, (prefix.length() - 1)));
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
    private Document loadDdexSkeletonFile(KB kb, String namespaceTerm, File indir) {
        Document doc = null;
        File skeleton = null;
        String suffix = ".skeleton";
        try {
            String nsName = ((namespaceTerm.startsWith("ns_") || namespaceTerm.startsWith("ns:"))
                             ? namespaceTerm.substring(3)
                             : namespaceTerm); 
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
                System.out.println("The file \""
                                   + skeleton.getCanonicalPath()
                                   + "\" does not exist or cannot be read");
                return doc;
            }
            BufferedReader br = new BufferedReader(new FileReader(skeleton));
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
                }
                catch (Exception ioe) {
                    ioe.printStackTrace();
                }
            }
        }
        catch (Exception ex) {
            doc = null;
            System.out.println("The skeleton file for " + namespaceTerm + " could not be loaded");
            ex.printStackTrace();
        }
        return doc;
    }

    /** *************************************************************
     * 
     */
    private ArrayList getIsDataTypeOfTerms(KB kb, String term) {
        ArrayList result = new ArrayList();
        try {
            String[] inverses = {"ddex_IsDataTypeOf", "ddex_HasDataType"};
            String reln = null;
            List formulae = null;
            Formula f = null;
            String arg = null;
            for (int j = 0; j < inverses.length; j++) {
                reln = inverses[j];
                formulae = kb.askWithRestriction(0, 
                                                 reln, 
                                                 (j == 0 ? 1 : 2),
                                                 term);
                if (formulae != null) {
                    int argnum = (j == 0 ? 2 : 1);
                    for (int i = 0; i < formulae.size(); i++) {
                        f = (Formula) formulae.get(i);
                        arg = f.getArgument(argnum);
                        if (!result.contains(arg)) {
                            result.add(arg);
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
     * 
     */
    private ArrayList getHasDataTypeTerms(KB kb, String term) {
        ArrayList result = new ArrayList();
        try {
            String[] inverses = {"ddex_HasDataType", "ddex_IsDataTypeOf"};
            String reln = null;
            List formulae = null;
            Formula f = null;
            String arg = null;
            for (int j = 0; j < inverses.length; j++) {
                reln = inverses[j];
                formulae = kb.askWithRestriction(0, 
                                                 reln, 
                                                 (j == 0 ? 1 : 2),
                                                 term);
                if (formulae != null) {
                    int argnum = (j == 0 ? 2 : 1);
                    for (int i = 0; i < formulae.size(); i++) {
                        f = (Formula) formulae.get(i);
                        arg = f.getArgument(argnum);
                        if (!result.contains(arg)) {
                            result.add(arg);
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
     * 
     */
    private String getFirstHasDataType(KB kb, String term) {
        String dtype = null;
        try {
            List types = getHasDataTypeTerms(kb, term);
            if (!types.isEmpty()) {
                dtype = (String) types.get(0);
            }
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
        return dtype;
    }

    /** *************************************************************
     * 
     */
    private boolean isDataType(KB kb, String term) {
        boolean ans = false;
        try {
            // For DDEX, predicate will be "ddex_HasDataType".
            String[] inverses = {"ddex_HasDataType", "ddex_IsDataTypeOf"};
            String reln = null;
            List formulae = null;
            Formula f = null;
            for (int j = 0; j < inverses.length; j++) {
                reln = inverses[j];
                formulae = kb.askWithRestriction(0, 
                                                 reln, 
                                                 (j == 0 ? 2 : 1),
                                                 term);
                if ((formulae != null) || !formulae.isEmpty()) {
                    ans = true;
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
     * 
     */
    private String getContentRegexPattern(KB kb, String term) {
        String pattern = null;
        try {
            List formulae = kb.askWithRestriction(0, "contentRegexPattern", 1, term);
            if ((formulae != null) && !formulae.isEmpty()) {
                Formula f = null;
                for (int j = 0; j < formulae.size(); j++) {
                    f = (Formula) formulae.get(j);
                    pattern = f.getArgument(2);
                    break;
                }
            }
            if (StringUtil.emptyString(pattern)) {
                List specs = getFirstSpecificTerms(kb, term);
                specs.addAll(getIsDataTypeOfTerms(kb, term));
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
    private String getHasXmlType(KB kb, String term) {
        String xmlType = null;
        try {
            String[] inverses = {"ddex_HasXmlType", "ddex_IsXmlTypeOf"};
            String reln = null;
            List formulae = null;
            Formula f = null;
            for (int j = 0; j < inverses.length; j++) {
                reln = inverses[j];
                formulae = kb.askWithRestriction(0, 
                                                 reln, 
                                                 (j == 0 ? 1 : 2),
                                                 term);
                if (formulae != null) {
                    int argnum = (j == 0 ? 2 : 1);
                    for (int i = 0; i < formulae.size(); i++) {
                        f = (Formula) formulae.get(i);
                        xmlType = f.getArgument(argnum);
                        break;
                    }
                }
                if (xmlType != null) { break; }
            }
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
        return xmlType;
    }

    /** *************************************************************
     * 
     */
    private ArrayList getHasXmlElementTerms(KB kb, String term) {
        ArrayList result = new ArrayList();
        try {
            String[] inverses = {"ddex_HasXmlElement", "ddex_IsXmlElementOf"};
            String reln = null;
            List formulae = null;
            Formula f = null;
            String arg = null;
            for (int j = 0; j < inverses.length; j++) {
                reln = inverses[j];
                formulae = kb.askWithRestriction(0, 
                                                 reln, 
                                                 (j == 0 ? 1 : 2),
                                                 term);
                if (formulae != null) {
                    int argnum = (j == 0 ? 2 : 1);
                    for (int i = 0; i < formulae.size(); i++) {
                        f = (Formula) formulae.get(i);
                        arg = f.getArgument(argnum);
                        if (!result.contains(arg)) {
                            result.add(arg);
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
     * 
     */
    private ArrayList getIsXmlElementOfTerms(KB kb, String term) {
        ArrayList result = new ArrayList();
        try {
            String[] inverses = {"ddex_IsXmlElementOf", "ddex_HasXmlElement"};
            String reln = null;
            List formulae = null;
            Formula f = null;
            String arg = null;
            for (int j = 0; j < inverses.length; j++) {
                reln = inverses[j];
                formulae = kb.askWithRestriction(0, 
                                                 reln, 
                                                 (j == 0 ? 1 : 2),
                                                 term);
                if (formulae != null) {
                    int argnum = (j == 0 ? 2 : 1);
                    for (int i = 0; i < formulae.size(); i++) {
                        f = (Formula) formulae.get(i);
                        arg = f.getArgument(argnum);
                        if (!result.contains(arg)) {
                            result.add(arg);
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
     * 
     */
    private ArrayList getHasXmlAttributeTerms(KB kb, String term) {
        ArrayList result = new ArrayList();
        try {
            String[] inverses = {"ddex_HasXmlAttribute", "ddex_IsXmlAttributeOf"};
            String reln = null;
            List formulae = null;
            Formula f = null;
            String arg = null;
            for (int j = 0; j < inverses.length; j++) {
                reln = inverses[j];
                formulae = kb.askWithRestriction(0, 
                                                 reln, 
                                                 (j == 0 ? 1 : 2),
                                                 term);
                if (formulae != null) {
                    int argnum = (j == 0 ? 2 : 1);
                    for (int i = 0; i < formulae.size(); i++) {
                        f = (Formula) formulae.get(i);
                        arg = f.getArgument(argnum);
                        if (!result.contains(arg)) {
                            result.add(arg);
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
     * 
     */
    private ArrayList getIsXmlAttributeOfTerms(KB kb, String term) {
        ArrayList result = new ArrayList();
        try {
            String[] inverses = {"ddex_IsXmlAttributeOf", "ddex_HasXmlAttribute"};
            String reln = null;
            List formulae = null;
            Formula f = null;
            String arg = null;
            for (int j = 0; j < inverses.length; j++) {
                reln = inverses[j];
                formulae = kb.askWithRestriction(0, 
                                                 reln, 
                                                 (j == 0 ? 1 : 2),
                                                 term);
                if (formulae != null) {
                    int argnum = (j == 0 ? 2 : 1);
                    for (int i = 0; i < formulae.size(); i++) {
                        f = (Formula) formulae.get(i);
                        arg = f.getArgument(argnum);
                        if (!result.contains(arg)) {
                            result.add(arg);
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
     * 
     */
    private ArrayList getHasXmlExtensionTerms(KB kb, String term) {
        ArrayList result = new ArrayList();
        try {
            String[] inverses = {"ddex_HasXmlExtension", "ddex_IsXmlExtensionOf"};
            String reln = null;
            List formulae = null;
            Formula f = null;
            String arg = null;
            for (int j = 0; j < inverses.length; j++) {
                reln = inverses[j];
                formulae = kb.askWithRestriction(0, 
                                                 reln, 
                                                 (j == 0 ? 1 : 2),
                                                 term);
                if (formulae != null) {
                    int argnum = (j == 0 ? 2 : 1);
                    for (int i = 0; i < formulae.size(); i++) {
                        f = (Formula) formulae.get(i);
                        arg = f.getArgument(argnum);
                        if (!result.contains(arg)) {
                            result.add(arg);
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
     * 
     */
    private ArrayList getIsXmlExtensionOfTerms(KB kb, String term) {
        ArrayList result = new ArrayList();
        try {
            String[] inverses = {"ddex_IsXmlExtensionOf", "ddex_HasXmlExtension"};
            String reln = null;
            List formulae = null;
            Formula f = null;
            String arg = null;
            for (int j = 0; j < inverses.length; j++) {
                reln = inverses[j];
                formulae = kb.askWithRestriction(0, 
                                                 reln, 
                                                 (j == 0 ? 1 : 2),
                                                 term);
                if (formulae != null) {
                    int argnum = (j == 0 ? 2 : 1);
                    for (int i = 0; i < formulae.size(); i++) {
                        f = (Formula) formulae.get(i);
                        arg = f.getArgument(argnum);
                        if (!result.contains(arg)) {
                            result.add(arg);
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
     * 
     */
    private ArrayList getHasXmlUnionTerms(KB kb, String term) {
        ArrayList result = new ArrayList();
        try {
            String[] inverses = {"ddex_HasXmlUnion", "ddex_IsXmlUnionOf"};
            String reln = null;
            List formulae = null;
            Formula f = null;
            String arg = null;
            for (int j = 0; j < inverses.length; j++) {
                reln = inverses[j];
                formulae = kb.askWithRestriction(0, 
                                                 reln, 
                                                 (j == 0 ? 1 : 2),
                                                 term);
                if (formulae != null) {
                    int argnum = (j == 0 ? 2 : 1);
                    for (int i = 0; i < formulae.size(); i++) {
                        f = (Formula) formulae.get(i);
                        arg = f.getArgument(argnum);
                        if (!result.contains(arg)) {
                            result.add(arg);
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
     * 
     */
    private ArrayList getIsXmlUnionOfTerms(KB kb, String term) {
        ArrayList result = new ArrayList();
        try {
            String[] inverses = {"ddex_IsXmlUnionOf", "ddex_HasXmlUnion"};
            String reln = null;
            List formulae = null;
            Formula f = null;
            String arg = null;
            for (int j = 0; j < inverses.length; j++) {
                reln = inverses[j];
                formulae = kb.askWithRestriction(0, 
                                                 reln, 
                                                 (j == 0 ? 1 : 2),
                                                 term);
                if (formulae != null) {
                    int argnum = (j == 0 ? 2 : 1);
                    for (int i = 0; i < formulae.size(); i++) {
                        f = (Formula) formulae.get(i);
                        arg = f.getArgument(argnum);
                        if (!result.contains(arg)) {
                            result.add(arg);
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
     * 
     */
    private ArrayList getHasXmlCompositeTerms(KB kb, String term) {
        ArrayList result = new ArrayList();
        try {
            String[] inverses = {"ddex_HasXmlComposite", "ddex_IsXmlCompositeFor"};
            String reln = null;
            List formulae = null;
            Formula f = null;
            String arg = null;
            for (int j = 0; j < inverses.length; j++) {
                reln = inverses[j];
                formulae = kb.askWithRestriction(0, 
                                                 reln, 
                                                 (j == 0 ? 1 : 2),
                                                 term);
                if (formulae != null) {
                    int argnum = (j == 0 ? 2 : 1);
                    for (int i = 0; i < formulae.size(); i++) {
                        f = (Formula) formulae.get(i);
                        arg = f.getArgument(argnum);
                        if (!result.contains(arg)) {
                            result.add(arg);
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
     * 
     */
    private ArrayList getIsXmlCompositeForTerms(KB kb, String term) {
        ArrayList result = new ArrayList();
        try {
            String[] inverses = {"ddex_IsXmlCompositeFor", "ddex_HasXmlComposite"};
            String reln = null;
            List formulae = null;
            Formula f = null;
            String arg = null;
            for (int j = 0; j < inverses.length; j++) {
                reln = inverses[j];
                formulae = kb.askWithRestriction(0, 
                                                 reln, 
                                                 (j == 0 ? 1 : 2),
                                                 term);
                if (formulae != null) {
                    int argnum = (j == 0 ? 2 : 1);
                    for (int i = 0; i < formulae.size(); i++) {
                        f = (Formula) formulae.get(i);
                        arg = f.getArgument(argnum);
                        if (!result.contains(arg)) {
                            result.add(arg);
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
     * 
     */
    private ArrayList getIsXmlCompositeForArg2TermsInNamespace(KB kb, String namespace) {
        ArrayList result = new ArrayList();
        try {
            String kifNamespace = toKifNamespace(kb, namespace);
            if (StringUtil.isNonEmptyString(kifNamespace) && kifNamespace.startsWith("ns_")) {
                String prefix = (kifNamespace.substring(3) + "_");
                String[] inverses = {"ddex_IsXmlCompositeFor", "ddex_HasXmlComposite"};
                String reln = null;
                List formulae = null;
                Formula f = null;
                String arg = null;
                for (int j = 0; j < inverses.length; j++) {
                    reln = inverses[j];
                    formulae = kb.ask("arg", 0, reln);
                    if (formulae != null) {
                        int argnum = (j == 0 ? 2 : 1);
                        for (int i = 0; i < formulae.size(); i++) {
                            f = (Formula) formulae.get(i);
                            arg = f.getArgument(argnum);
                            if (arg.startsWith(prefix) && !result.contains(arg)) {
                                result.add(arg);
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

    /** *************************************************************
     * 
     */
    private ArrayList getIsXmlUnionOfArg2TermsInNamespace(KB kb, String namespace) {
        ArrayList result = new ArrayList();
        try {
            String kifNamespace = toKifNamespace(kb, namespace);
            if (StringUtil.isNonEmptyString(kifNamespace) && kifNamespace.startsWith("ns_")) {
                String prefix = (kifNamespace.substring(3) + "_");
                String[] inverses = {"ddex_IsXmlUnionOf", "ddex_HasXmlUnion"};
                String reln = null;
                List formulae = null;
                Formula f = null;
                String arg = null;
                for (int j = 0; j < inverses.length; j++) {
                    reln = inverses[j];
                    formulae = kb.ask("arg", 0, reln);
                    if (formulae != null) {
                        int argnum = (j == 0 ? 2 : 1);
                        for (int i = 0; i < formulae.size(); i++) {
                            f = (Formula) formulae.get(i);
                            arg = f.getArgument(argnum);
                            if (arg.startsWith(prefix) && !result.contains(arg)) {
                                result.add(arg);
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

    /** *************************************************************
     * 
     */
    private ArrayList getIsOneOfArg1Terms(KB kb, String arg2) {
        ArrayList result = new ArrayList();
        try {
            List formulae = kb.askWithRestriction(0, "ddex_IsOneOf", 2, arg2);
            Formula f = null;
            if (formulae != null) {
                List sortable = new ArrayList();
                String arg1 = null;
                for (int i = 0; i < formulae.size(); i++) {
                    f = (Formula) formulae.get(i);
                    arg1 = f.getArgument(1);
                    String[] pair = new String[2];
                    pair[0] = arg1;
                    pair[1] = getTermPresentationName(kb, "", arg1);
                    sortable.add(pair);
                }
                Comparator comp = new Comparator() {
                        public int compare(Object o1, Object o2) {
                            String name1 = ((String[]) o1)[1];
                            String name2 = ((String[]) o2)[1];
                            return String.CASE_INSENSITIVE_ORDER.compare(name1, name2);
                        }
                    };
                Collections.sort(sortable, comp);
                String[] strArr = null;
                for (int j = 0; j < sortable.size(); j++) {
                    strArr = (String[]) sortable.get(j);
                    result.add(strArr[0]);
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
    private ArrayList getAllTerms(KB kb, 
                                  int argx, 
                                  String argxVal, 
                                  int argy,
                                  String argyVal,
                                  int gatherArg) {
        ArrayList result = new ArrayList();
        try {
            TreeSet accumulator = new TreeSet();
            List formulae = kb.askWithRestriction(argx, argxVal, argy, argyVal);
            Formula f = null;
            if (formulae != null) {
                for (int i = 0; i < formulae.size(); i++) {
                    f = (Formula) formulae.get(i);
                    accumulator.add(f.getArgument(gatherArg));
                }
            }
            if (!accumulator.isEmpty()) {
                result.addAll(accumulator);
            }
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
        return result;
    }

    /** *************************************************************
     * Sorts stringList by each term's presentation name, which could
     * be very different from the raw term name.
     *
     * @param kb The KB from which to obtain the presentation names.
     *
     * @param namespaceTerm A KIF term denoting a namespace.
     *
     * @param stringList The List of Strings to be sorted.
     *
     * @return A sorted ArrayList.
     *
     */
    private ArrayList sortByPresentationName(KB kb, String namespaceTerm, List stringList) {
        ArrayList sorted = new ArrayList();
        try {
            if (stringList != null) {
                List sortable = new ArrayList();
                String term = null;
                for (int i = 0; i < stringList.size(); i++) {
                    term = (String) stringList.get(i);
                    String[] pair = new String[2];
                    pair[0] = term;
                    pair[1] = getTermPresentationName(kb, namespaceTerm, term);
                    sortable.add(pair);
                }
                Comparator comp = new Comparator() {
                        public int compare(Object o1, Object o2) {
                            String name1 = ((String[]) o1)[1];
                            String name2 = ((String[]) o2)[1];
                            return String.CASE_INSENSITIVE_ORDER.compare(name1, name2);
                        }
                    };
                Collections.sort(sortable, comp);
                String[] strArr = null;
                for (int j = 0; j < sortable.size(); j++) {
                    strArr = (String[]) sortable.get(j);
                    sorted.add(strArr[0]);
                }
            }
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
        return sorted;
    }

    /** *************************************************************
     * Sorts stringList by explicit numeric ordinal values obtained
     * from assertions made with the predicate arg1SortOrdinal.  Terms
     * lacking such assertions will occur at the end of the sorted
     * list.  If no term in the list has a relevant arg1SortOrdinal
     * assertion, the list will not be sorted.
     *
     * @param kb The KB from which to obtain the presentation names.
     *
     * @param namespaceTerm A KIF term denoting a namespace.
     *
     * @param arg2Term The term to which "belong", or are related by
     * some predicate, all of the arg1 values to be sorted.
     *
     * @param arg1OrdFormulae An optional List of arg1SortOrdinal
     * formulae.  If this list is not passed in, the method will try
     * to compute it.
     *
     * @param stringList The List of Strings to be sorted.
     *
     * @return An ArrayList sorted by each element's explicitly stated
     * ordinal number.
     *
     */
    private ArrayList sortByArg1Ordinal(KB kb, 
                                        String namespaceTerm, 
                                        String arg2Term, 
                                        List arg1OrdFormulae,
                                        List stringList) {
        ArrayList sorted = new ArrayList();
        try {
            if (stringList != null) {
                List formulae = arg1OrdFormulae;
                if (formulae == null) {
                    formulae = kb.askWithRestriction(0, "arg1SortOrdinal", 3, arg2Term);
                }
                if ((formulae == null) || formulae.isEmpty()) {
                    sorted.addAll(stringList);
                }
                else {
                    List sortable = new ArrayList();
                    Formula f = null;
                    String term = null;
                    boolean sortValueFound = false;
                    for (int i = 0; i < stringList.size(); i++) {
                        sortValueFound = false;
                        term = (String) stringList.get(i);
                        String[] pair = new String[2];
                        pair[0] = term;
                        pair[1] = String.valueOf(Integer.MAX_VALUE);
                        sortable.add(pair);
                        for (int j = 0; j < formulae.size(); j++) {
                            f = (Formula) formulae.get(j);
                            if (term.equals(f.getArgument(2))) {
                                pair[1] = f.getArgument(4);
                                sortValueFound = true;
                            }
                        }
                        if (!sortValueFound) {
                                System.out.println("Warning: no ordinal sort value found for "
                                                  + term);
                        }
                    }
                    Comparator comp = new Comparator() {
                            public int compare(Object o1, Object o2) {
                                int ord1 = Integer.parseInt(((String[]) o1)[1]);
                                int ord2 = Integer.parseInt(((String[]) o2)[1]);
                                return ((ord1 < ord2)
                                        ? -1
                                        : ((ord1 == ord2)
                                           ? 0
                                           : 1));
                            }
                        };
                    Collections.sort(sortable, comp);
                    String[] strArr = null;
                    for (int j = 0; j < sortable.size(); j++) {
                        strArr = (String[]) sortable.get(j);
                        sorted.add(strArr[0]);
                    }
                }
            }
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
        return sorted;
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

            System.out.println("path == " + newPath);

            String docustr = getCompositeDocumentation(kb, term, newPath);
            if (StringUtil.isNonEmptyString(docustr)) {
                docustr = removeLinkableNamespacePrefixes(kb, docustr);
                docustr = StringUtil.removeEnclosingQuotes(docustr);
                // docustr = StringUtil.escapeQuoteChars(docustr);
                // docustr = docustr.replace("\"\"", "\\\"");
                docustr = StringUtil.removeQuoteEscapes(docustr);
                docustr = StringUtil.normalizeSpaceChars(docustr);
                // docustr = StringUtil.replaceNonAsciiChars(docustr);
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
            String pred = (cardtype + "Cardinality");
            List cardForms = kb.askWithRestriction(0, pred, 2, term);
            if (cardForms != null && !cardForms.isEmpty()) {
                Formula f = (Formula) cardForms.get(0);
                if (context.equals("") || context.equals(f.getArgument(1))) {
                    val = f.getArgument(3);
                }
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
        try {
            // ignoring context for now, since each LocalInstance term
            // can have only one set of cardinaltiy values.
            String nodeName = elem.getNodeName();
            String exact = getCardinality(kb, "exact", term, "");
            boolean isExact = StringUtil.isNonEmptyString(exact);
            String min = (isExact
                          ? exact
                          : getCardinality(kb, "min", term, ""));
            String max = (isExact
                          ? exact
                          : getCardinality(kb, "max", term, ""));
            boolean isMin = StringUtil.isNonEmptyString(min);
            boolean isMax = StringUtil.isNonEmptyString(max);

            if (nodeName.matches(".*element.*") || nodeName.matches(".*choice.*")) {
                if (!isMin) { min = "0"; isMin = true; }
                if (isMin && !min.equals("1")) {
                    elem.setAttribute("minOccurs", min);
                }
                if (!isMax) { max = "unbounded"; isMax = true; };
                if (isMax && !max.equals("1")) {
                    elem.setAttribute("maxOccurs", max);
                }
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
                String dtype = getFirstHasDataType(kb, term);
                if (!StringUtil.isNonEmptyString(dtype)) {
                    dtype = getFirstGeneralTerm(kb, term);
                }
                if (StringUtil.isNonEmptyString(dtype)) {
                    dtype = substituteXsdDataType(kb, namespace, dtype);
                    dtype = DB.kifToW3c(dtype);
                    elem.setAttribute("type", dtype);
                }
                setElementCardinality(kb, elem, term, path);
            }
            addXsdNodeAnnotationChildNode(kb, doc, elem, defaultNamespace, namespace, term, newPath);
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
                addXsdNodeAnnotationChildNode(kb, doc, elem, defaultNamespace, namespace, term, newPath);
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
                addXsdNodeAnnotationChildNode(kb, doc, elem, defaultNamespace, namespace, term, newPath);
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
            addXsdNodeAnnotationChildNode(kb, doc, elem, defaultNamespace, namespace, term, newPath);
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
    public String getTermPresentationName(KB kb, String namespace, String term) {
        String name = term;
        try {
            List context = new ArrayList();
            if (StringUtil.isNonEmptyString(namespace)) {
                context.add(namespace);
            }
            if (!context.contains("XMLLabel")) {
                context.add(0, "XMLLabel");
            }
            if (!context.contains("EnglishLanguage")) {
                context.add("EnglishLanguage");
            }
            name = getFirstTermFormat(kb, term, context);
            if (!StringUtil.isNonEmptyString(name) || name.equals(term)) {
                name = showTermName(kb, term, namespace);
            }
            if (StringUtil.isNonEmptyString(namespace)) {
                name = stripNamespacePrefix(kb, name);
            }
            // The for loop below is solely to handle
            // NonIsoTerritoryCode^Worldwide.
            String[] delims = {"^"};
            for (int i = 0; i < delims.length; i++) {
                int idx = name.indexOf(delims[i]);
                while ((idx > -1) && (idx < name.length())) {
                    name = name.substring(idx + 1);
                    idx = name.indexOf(delims[i]);
                }
            }
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
        return name;
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
            String dtype = getFirstHasDataType(kb, term);
            if (!StringUtil.isNonEmptyString(dtype)) {
                dtype = getFirstGeneralTerm(kb, term);
            }
            if (StringUtil.isNonEmptyString(dtype)) {
                dtype = substituteXsdDataType(kb, namespace, dtype);
                dtype = DB.kifToW3c(dtype);
            }
            else {
                dtype = "xs:string";
            }
            elem.setAttribute("type", dtype);
            setElementCardinality(kb, elem, term, path);
            addXsdNodeAnnotationChildNode(kb, doc, elem, defaultNamespace, namespace, term, newPath);
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
                baseName = DB.kifToW3c(baseName);
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

                Element contentElem = null;
                if (hasComplexContent(extensionElem)) {
                    contentElem = makeElement(doc, "xs:complexContent");
                }
                else {
                    contentElem = makeElement(doc, "xs:simpleContent");
                }
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
                List types = getIsXmlUnionOfTerms(kb, term);
                if (!types.isEmpty()) {
                    String typelist = "";
                    for (int i = 0; i < types.size(); i++) {
                        if (i > 0) { typelist += " "; }
                        String termName = DB.kifToW3c((String) types.get(i));
                        typelist += termName;
                    }
                    Element _unionElem = makeElement(doc, "xs:union");
                    _unionElem.setAttribute("memberTypes", typelist);
                    parentElem.appendChild(_unionElem);
                }
            }
            else {
                List valueSetTerms = getIsOneOfArg1Terms(kb, term);
                if (!valueSetTerms.isEmpty()) {
                    Element _restrictionElem = makeElement(doc, "xs:restriction");
                    String dtype = getFirstHasDataType(kb, term);
                    if (StringUtil.isNonEmptyString(dtype)) {
                        dtype = substituteXsdDataType(kb, nsContext, dtype);
                        dtype = DB.kifToW3c(dtype);
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
                List subElementTerms = getHasXmlElementTerms(kb, term);
                String subTerm = null;
                for (int i = 0; i < subElementTerms.size(); i++) {
                    subTerm = (String) subElementTerms.get(i);
                    Element newElem = null;
                    if (isSkipNode(kb, subTerm)) {  

                        // ddex_XmlChoice or ddex_XmlSequence
                        if (kb.isInstanceOf(subTerm, "ddex_XmlChoice")) {
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
            List subAttrTerms = getHasXmlAttributeTerms(kb, term);
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
     * Collects all top-level terms for each namespace-specific XSD
     * file.  Returns a Map in which the keys are KIF namespace terms,
     * and the values are lists of all top-level XSD terms in the
     * indexed namespaces.
     *
     */
    private Map computeTopLevelTermsByNamespace(KB kb) {
        Map tops = new HashMap();
        Set done = new HashSet();
        try {
            Set accumulator = new HashSet();
            String term = null;
            List termList = new ArrayList();
            // String dtype = null;
            String namespace = null;
            for (int c = 0; (c == 0) || !accumulator.isEmpty(); c++) {
                termList.clear();
                termList.addAll(accumulator);
                accumulator.clear();

                // Start with the message types and LocalInstance
                // terms, but don't include them in the tops Map.
                if (c == 0) {
                    termList.addAll(getDdexMessageTypes(kb));
                    /*
                    Iterator kbit = kb.terms.iterator();
                    while (kbit.hasNext()) {
                        term = (String) kbit.next();
                        namespace = getTermNamespace(kb, term);
                        if (// StringUtil.isNonEmptyString(namespace)
                            // || 
                            term.matches(".*(?i)LocalInstance.*")) {
                            termList.add(term);
                        }
                    }
                    */
                }

                // System.out.println("termList == " + termList);

                for (int i = 0; i < termList.size(); i++) {
                    term = (String) termList.get(i);
                    if (!done.contains(term)) {

                        if (!isSkipNode(kb, term) && (c > 0)) {

                            namespace = getTermNamespace(kb, term);

                            if (term.matches(".*(?i)LocalInstance.*")) {

                                String dtype = getFirstHasDataType(kb, term);
                                if (StringUtil.isNonEmptyString(dtype)) {
                                    namespace = getTermNamespace(kb, dtype);
                                    if (StringUtil.isNonEmptyString(namespace)
                                        && !accumulator.contains(dtype)) {
                                        accumulator.add(dtype);
                                    }
                                }

                                String pattern = getContentRegexPattern(kb, term);
                                if (StringUtil.isNonEmptyString(pattern)) {
                                    String io = getFirstGeneralTerm(kb, term);
                                    if (StringUtil.isNonEmptyString(io)) {
                                        namespace = getTermNamespace(kb, io);
                                        if (StringUtil.isNonEmptyString(namespace)
                                            && !accumulator.contains(io)) {
                                            accumulator.add(io);
                                        }
                                    }
                                }
                            }
                            else {
                                addToMapByNS(tops, namespace, term);
                            }
                        }
                        accumulator.addAll(getHasXmlElementTerms(kb, term));
                        accumulator.addAll(getHasXmlAttributeTerms(kb, term));
                        accumulator.addAll(getIsXmlExtensionOfTerms(kb, term));
                        accumulator.addAll(getIsXmlUnionOfTerms(kb, term));
                        accumulator.addAll(getHasXmlCompositeTerms(kb, term));
                        // accumulator.addAll(getIsXmlCompositeForTerms(kb, term));
                        done.add(term);
                    }
                }
                /*
                if (c == 0) {
                    Iterator kbit = kb.terms.iterator();
                    while (kbit.hasNext()) {
                        term = (String) kbit.next();
                        namespace = getTermNamespace(kb, term);
                        if (// StringUtil.isNonEmptyString(namespace)
                            // || 
                            term.matches(".*(?i)LocalInstance.*")) {
                            accumulator.add(term);
                        }
                    }
                }
                */
            }
            Iterator it = tops.keySet().iterator();
            while (it.hasNext()) {

                // A namespace.
                term = (String) it.next();

                // A list of terms in namespace.
                termList = (List) tops.get(term);
                termList = sortByPresentationName(kb, namespace, termList);
                tops.put(term, termList);

                // Collections.sort(termList, String.CASE_INSENSITIVE_ORDER);
            }
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
        return tops;
    }

    /** *************************************************************
     * Adds term to a List in the Map m.  The List is indexed by the
     * KIF namespace term nsTerm.
     *
     */
    private void addToMapByNS(Map m, String nsTerm, String term) {
        try {
            List termList = (List) m.get(nsTerm);
            if (termList == null) {
                termList = new ArrayList();
                m.put(nsTerm, termList);
            }
            if (!termList.contains(term)) {
                termList.add(term);
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
    public void writeXsdFileForNamespace3(KB kb, 
                                          String namespace, 
                                          List topLevelTerms,
                                          String targetDirectory,
                                          int version) {
        try {            
            String nsName = ((namespace.startsWith("ns_") || namespace.startsWith("ns:"))
                             ? namespace.substring(3)
                             : namespace); 
            String nsTerm = ("ns_" + nsName);
            List termsInNamespace = getTermsInNamespace(kb, nsTerm);

            if (!topLevelTerms.isEmpty()) {

                List filteredTerms = new ArrayList();
                String ft = null;
                String nativeType = null;
                for (int i = 0; i < topLevelTerms.size(); i++) {
                    ft = (String) topLevelTerms.get(i);
                    nativeType = substituteXsdDataType(kb, nsTerm, ft);
                    if (!nativeType.startsWith("xs:")
                        || nativeType.equals("xs:IDREF")
                        || nativeType.equals("xs:ID")) {
                        filteredTerms.add(ft);
                    }
                }
                topLevelTerms = filteredTerms;

                // topLevelTerms = sortByPresentationName(kb, nsTerm, topLevelTerms);
                // Collections.sort(topLevelTerms, String.CASE_INSENSITIVE_ORDER);

                System.out.println("topLevelTerms == " + topLevelTerms);

                // String fs = System.getProperty("file.separator");
                File tDir = new File(targetDirectory);
                File workdir = tDir;
                if (!workdir.isDirectory()) {
                    workdir = new File(System.getProperty("user.dir"));
                }

                // Give up if we can't load a skeleton document.
                Document _doc = loadDdexSkeletonFile(kb, nsTerm, workdir);
                if (_doc == null) {
                    System.out.println("Could not load the skeleton file for \"" 
                                       + namespace
                                       + "\"");
                    return;
                }

                Element _docelem = _doc.getDocumentElement();
                System.out.println("Document root element: " + _docelem.getNodeName());

                // reviseDdexSchemaAttributes(_docelem, version);

                String filename = getXsdFileName(kb, nsTerm);
                File outfile = new File(workdir, filename);
                PrintWriter pw = new PrintWriter(new FileWriter(outfile));

                Iterator it = topLevelTerms.iterator();
                String term = null;
                String termName = null;
                String nsPrefix = null;
                while (it.hasNext()) {
                    term = (String) it.next();
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
                        if (!xsdType.equals(DB.kifToW3c(term))) {
                            datatype = xsdType;
                        }
                    }
                    else {
                        String specVal = null;
                        String dtype = null;
                        boolean specHasDataType = false;
                        for (int i = 0; i < specs.size(); i++) {
                            specVal = (String) specs.get(i);
                            dtype = getFirstHasDataType(kb, specVal);
                            if (StringUtil.isNonEmptyString(dtype)) {
                                specHasDataType = true;
                                xsdType = substituteXsdDataType(kb, nsTerm, dtype);
                                dtype = DB.kifToW3c(dtype);
                                if (!dtype.equals(xsdType)) {
                                    datatype = xsdType;
                                    break;
                                }
                            }
                        }
                        if (!specHasDataType) {
                            xsdType = substituteXsdDataType(kb, nsTerm, term);
                            if (!xsdType.equals(DB.kifToW3c(term))) {
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

                    List members = getAvsTypeMembers(kb, nsTerm, termsInNamespace, term);

                    // if (term.matches(".*(?i)IsoCode.*") 
                    //     || term.matches(".*(?i)TerritoryCode.*")
                    //     || term.matches(".*(?i)LanguageCode.*")
                    //     || term.matches(".*(?i)CurrencyCode.*")) {
                    // Collections.sort(members, String.CASE_INSENSITIVE_ORDER);
                    // }

                    // members = sortByPresentationName(kb, "", members);

                    List arg1OrdFormulae = kb.askWithRestriction(0, "arg1SortOrdinal", 3, term);
                    if ((arg1OrdFormulae == null) || arg1OrdFormulae.isEmpty()) {
                        members = sortByPresentationName(kb, nsTerm, members);
                    }
                    else {
                        members = sortByArg1Ordinal(kb, nsTerm, term, arg1OrdFormulae, members);
                    }

                    System.out.println("members == " + members);

                    if (!members.isEmpty()) {
                        Iterator mIt = members.iterator();
                        String m = null;
                        String mName = null;
                        while (mIt.hasNext()) {
                            m = (String) mIt.next();
                            mName = getTermPresentationName(kb, nsTerm, m);
                            mName = stripNamespacePrefix(kb, mName);
                            Element _enumeration = makeElement(_doc, "xs:enumeration");
                            if (getIsoCodes(kb).contains(m)) {
                                mName = stripNamespacePrefix(kb, m);
                                addXsdNodeAnnotationChildNode(kb, 
                                                              _doc, 
                                                              _enumeration, 
                                                              nsTerm, 
                                                              nsTerm, 
                                                              m,
                                                              nsList);
                            }
                            _enumeration.setAttribute("value", mName);
                            _restriction.appendChild(_enumeration);
                        }
                    }
                    _simpleType.appendChild(_restriction);
                    _docelem.appendChild(_simpleType);
                }
                printXmlNodeTree2(_docelem, 0, pw);
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
            String nsName = ((namespaceTerm.startsWith("ns_") || namespaceTerm.startsWith("ns:"))
                             ? namespaceTerm.substring(3)
                             : namespaceTerm); 
            filename = nsName + suffix;
            List filenames = kb.askWithRestriction(0, "xsdFileName", 2, namespaceTerm);
            if ((filenames != null) && !filenames.isEmpty()) {
                Formula f = (Formula) filenames.get(0);
                filename = StringUtil.removeEnclosingQuotes(f.getArgument(1));
                if (!filename.endsWith(suffix)) {
                    filename += suffix;
                }
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
    private void reviseDdexSchemaAttributes(Element docElem, int version) {
        try {
            SimpleDateFormat _sdf = new SimpleDateFormat("yyyyMMdd");
            _sdf.setTimeZone(new SimpleTimeZone(0, "Greenwich"));
            NamedNodeMap nmap = docElem.getAttributes();
            Node attr = null;
            String nodeValue = null;
            String yyyyMMdd = _sdf.format(new Date());
            String vstr = String.valueOf(version);
            int maplen = -1;
            if (nmap != null) {
                maplen = nmap.getLength();
                for (int i = 0; i < maplen; i++) {
                    attr = nmap.item(i);
                    nodeValue = attr.getNodeValue();
                    if (nodeValue.endsWith("YYY")) {
                        nodeValue = nodeValue.replace("XXX", yyyyMMdd.substring(0, 4));
                        nodeValue = nodeValue.replace("YYY", vstr);
                    }
                    else {
                        nodeValue = nodeValue.replace("XXX", yyyyMMdd);
                    }
                    attr.setNodeValue(nodeValue);
                }
            }
            NodeList nlist = docElem.getChildNodes();
            int nlen = nlist.getLength();
            Node elem = null;
            String nodeName = null;
            for (int j = 0; j < nlen; j++) {
                elem = nlist.item(j);
                nodeName = elem.getNodeName();
                if (nodeName.matches(".*import.*")) {
                    nmap = elem.getAttributes();
                    maplen = nmap.getLength();
                    for (int i = 0; i < maplen; i++) {
                        attr = nmap.item(i);
                        nodeValue = attr.getNodeValue();
                        if (nodeValue.endsWith("YYY")) {
                            nodeValue = nodeValue.replace("XXX", yyyyMMdd.substring(0, 4));
                            nodeValue = nodeValue.replace("YYY", vstr);
                        }
                        else {
                            nodeValue = nodeValue.replace("XXX", yyyyMMdd);
                        }
                        attr.setNodeValue(nodeValue);
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
    public void writeDdexXsdFiles(KB kb, String targetDirectory, int version) {
        try {

            String defaultNamespace = "ns_ddex";
            File tDir = new File(targetDirectory);
            File workdir = tDir;
            if (!workdir.isDirectory()) {
                workdir = new File(System.getProperty("user.dir"));
            }

            List namespaces = getNamespaces(kb);
            Collections.sort(namespaces, String.CASE_INSENSITIVE_ORDER);
            Set msgTypes = getDdexMessageTypes(kb);
            Map nsMsgTypes = getDdexMessageTypesByNamespace(kb, msgTypes);
            Map tops = computeTopLevelTermsByNamespace(kb);

            Iterator it = namespaces.iterator();
            String nsTerm = null;
            String nsName = null;
            while (it.hasNext()) {
                nsTerm = (String) it.next();
                nsName = ((nsTerm.startsWith("ns_") || nsTerm.startsWith("ns:"))
                          ? nsTerm.substring(3)
                          : nsTerm); 
                nsTerm = (nsName.equals("ns") ? nsName : "ns_" + nsName);

                System.out.println("Processing terms for \"" + nsName + "\"");

                List topsForNS = new ArrayList();
                List cachedTops = (List) tops.get(nsTerm);
                if ((cachedTops != null) && !cachedTops.isEmpty()) {
                    topsForNS.addAll(cachedTops);
                }
                topsForNS = sortByPresentationName(kb, nsTerm, topsForNS);
                // Collections.sort(topsForNS, String.CASE_INSENSITIVE_ORDER);
                List cachedMsgTypes = (List) nsMsgTypes.get(nsTerm);
                if ((cachedMsgTypes != null) && !cachedMsgTypes.isEmpty()) {
                    cachedMsgTypes = sortByPresentationName(kb, nsTerm, cachedMsgTypes);
                    // Collections.sort(cachedMsgTypes, String.CASE_INSENSITIVE_ORDER);
                    topsForNS.addAll(cachedMsgTypes);
                }

                if (nsTerm.equals("ns_ddex") || nsTerm.matches(".*iso.*")) {

                    writeXsdFileForNamespace3(kb, nsTerm, topsForNS, targetDirectory, version);
                }
                else {
                    Document _doc = loadDdexSkeletonFile(kb, nsTerm, workdir);
                    if (_doc == null) {
                        System.out.println("Cannot load a skeleton file for \"" + nsName + "\"");
                        continue;
                    }
                    System.out.println("Loaded skeleton for \"" + nsName + "\"");

                    Element _docelem = _doc.getDocumentElement();
                    System.out.println("Document root element: " + _docelem.getNodeName());

                    // reviseDdexSchemaAttributes(_docelem, version);

                    String filename = getXsdFileName(kb, nsTerm);
                    File outfile = new File(workdir, filename);

                    PrintWriter pw = null;

                    try {

                        for (int i = 0; i < topsForNS.size(); i++) {
                            String term = (String) topsForNS.get(i);
                            List path = new ArrayList();
                            path.add(0, term);

                            // System.out.println("term == " + term);

                            Element _elem = null;
                            String firstIo = getFirstGeneralTerm(kb, term);
                            if (!StringUtil.isNonEmptyString(firstIo)) {
                                firstIo = "";
                            }

                            if (isXsdComplexType(kb, term)) {
                                if (firstIo.equals("ddex_MessageType")) {
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

                                    List extended = getIsXmlExtensionOfTerms(kb, term);
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
                                List extended = getIsXmlExtensionOfTerms(kb, term);
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
                        printXmlNodeTree2(_docelem, 0, pw);
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
        return;
    }

    /** *************************************************************
     * 
     */
    private ArrayList getXsdTopLevelTerms(KB kb, List termsInNamespace) {
        ArrayList result = new ArrayList();
        try {
            List accumulator = new ArrayList();
            accumulator.addAll(getAvssInNamespace(kb, termsInNamespace));
            accumulator.addAll(getCompositesInNamespace(kb, termsInNamespace));
            accumulator.addAll(getSubClassesInNamespace(kb, termsInNamespace, "ddex_Type"));
            accumulator.addAll(getSubClassesInNamespace(kb, termsInNamespace, "ddex_IsoCode"));
            accumulator.addAll(getSubClassesInNamespace(kb, termsInNamespace, "ddex_LanguageCode"));
            accumulator.addAll(getSubClassesInNamespace(kb, termsInNamespace, "ddex_TerritoryCode"));
            accumulator.addAll(getSubClassesInNamespace(kb, termsInNamespace, "ddex_CurrencyCode"));
            String term = null;
            for (int i = 0; i < accumulator.size(); i++) {
                term = (String) accumulator.get(i);
                if (!result.contains(term)) {
                    result.add(term);
                }
            }
            Collections.sort(result, String.CASE_INSENSITIVE_ORDER);
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
        return result;
    }

    /** *************************************************************
     * 
     */
    private ArrayList getTermsInNamespace(KB kb, String namespace) {
        ArrayList result = new ArrayList();
        try {
            String prefixedNs = (namespace.startsWith("ns_") || namespace.equals("ns")
                                 ? namespace
                                 : ("ns_" + namespace));
            List formulae = kb.askWithRestriction(0, "inNamespace", 2, prefixedNs);
            String term = null;
            if (formulae != null) {
                Formula f = null;
                for (int i = 0; i < formulae.size(); i++) {
                    f = (Formula) formulae.get(i);
                    term = f.getArgument(1);
                    if (!result.contains(term)) {
                        result.add(term);
                    }
                }
                Collections.sort(result, String.CASE_INSENSITIVE_ORDER);
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
    private ArrayList getAvsTypeMembers(KB kb, 
                                        String nsTerm, 
                                        List termsInNamespace, 
                                        String avsType) {
        ArrayList result = new ArrayList();
        try {
            List formulae = new ArrayList();
            formulae.addAll(kb.askWithRestriction(0, "ddex_IsOneOf", 2, avsType));
            formulae.addAll(kb.askWithRestriction(0, "ddex_IsA", 2, avsType));
            if (formulae != null) {
                Formula f = null;
                String arg1 = null;
                for (int i = 0; i < formulae.size(); i++) {
                    f = (Formula) formulae.get(i);
                    arg1 = f.getArgument(1);
                    if (!result.contains(arg1)) {
                        result.add(arg1);
                    }
                }
                result = sortByPresentationName(kb, "", result);
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
    private TreeSet getAvssInNamespace(KB kb, List termsInNamespace) {
        TreeSet avss = new TreeSet();
        try {
            List formulae = kb.ask("arg", 0, "ddex_IsOneOf");
            if (formulae != null) {
                Formula f = null;
                String arg2 = null;
                for (int i = 0; i < formulae.size(); i++) {
                    f = (Formula) formulae.get(i);
                    arg2 = f.getArgument(2);
                    if (termsInNamespace.contains(arg2)) {
                        avss.add(arg2);
                    }
                }
            }
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
        return avss;
    }

    /** *************************************************************
     * 
     */
    private TreeSet getCompositesInNamespace(KB kb, List termsInNamespace) {
        TreeSet composites = new TreeSet();
        try {
            Set terms = kb.getAllInstancesWithPredicateSubsumption("ddex_Composite");
            if (terms != null) {
                String term = null;
                Iterator it = terms.iterator();
                while (it.hasNext()) {
                    term = (String) it.next();
                    if (termsInNamespace.contains(term)) {
                        composites.add(term);
                    }
                }
            }
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
        return composites;
    }

    /** *************************************************************
     * 
     */
    private TreeSet getSubClassesInNamespace(KB kb, List termsInNamespace, String className) {
        TreeSet subclasses = new TreeSet();
        try {
            Set terms = kb.getAllSubClassesWithPredicateSubsumption(className);
            if (terms != null) {
                String term = null;
                Iterator it = terms.iterator();
                while (it.hasNext()) {
                    term = (String) it.next();
                    if (termsInNamespace.contains(term)) {
                        subclasses.add(term);
                    }
                }
            }
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
        return subclasses;
    }

    /** *************************************************************
     * 
     */
    private void printXmlNodeTree(org.w3c.dom.Node rootNode, int indent, PrintWriter pw) {
        try {
            String nodeName = rootNode.getNodeName();
            List childNodes = new ArrayList();
            if (rootNode.getNodeType() == org.w3c.dom.Node.ELEMENT_NODE) {
                if (nodeName.matches(".*(?i)documentation.*")) {
                    String txt = rootNode.getTextContent();
                    pw.println(DocGen.indentChars("    ", indent) 
                               + "<" + nodeName + ">"
                               + txt
                               + "</" + nodeName + ">");
                }
                else {
                    pw.print(DocGen.indentChars("    ", indent) + "<" + nodeName);
                    if (rootNode.hasAttributes()) {
                        NamedNodeMap attributes = rootNode.getAttributes();
                        int alen = attributes.getLength();
                        int alast = (alen - 1);
                        for (int i = 0; i < alen; i++) {
                            Node attr = attributes.item(i);
                            String attrName = attr.getNodeName();
                            String attrVal = attr.getNodeValue();
                            String spacer = " ";
                            if (i > 0) {
                                spacer = (DocGen.indentChars("    ", indent) 
                                          + DocGen.indentChars(" ", nodeName.length())
                                          + "  ");
                            }
                            pw.print(spacer + attrName
                                     + "=\"" + attrVal + "\""
                                     + ((i < alast) 
                                        ? System.getProperty("line.separator")
                                        : ""));
                        }
                    }
                    if (rootNode.hasChildNodes()) {
                        NodeList children = rootNode.getChildNodes();
                        int clen = children.getLength();
                        for (int i = 0; i < clen; i++) {
                            Node child = children.item(i);
                            if (child.getNodeType() != org.w3c.dom.Node.ATTRIBUTE_NODE) {
                                childNodes.add(child);
                            }
                        }
                    }
                    if (childNodes.isEmpty()) {
                        pw.println(" />");
                    }
                    else {
                        pw.println(">");
                        Node kid = null;
                        for (int i = 0; i < childNodes.size(); i++) {
                            kid = (Node) childNodes.get(i);
                            printXmlNodeTree(kid, (indent + 1), pw);
                        }
                        pw.println(DocGen.indentChars("    ", indent)
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
    private boolean isXsdComplexType(KB kb, String term) {
        boolean ans = false;
        try {
            if (isDataType(kb, term)) {
                List attrs = getHasXmlAttributeTerms(kb, term);
                List elems = getHasXmlElementTerms(kb, term);
                ans = !(attrs.isEmpty() && elems.isEmpty());
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
                List terms = getIsXmlUnionOfTerms(kb, term);
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
                for (int i = 0; i < working.size(); i++) {
                    node_i = (Node) working.get(i);
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
    private void printXmlNodeTree2(org.w3c.dom.Node rootNode, int indent, PrintWriter pw) {
        try {
            String nodeName = rootNode.getNodeName();
            if (rootNode.getNodeType() == org.w3c.dom.Node.ELEMENT_NODE) {
                if (nodeName.matches(".*(?i)documentation.*")) {
                    String txt = rootNode.getTextContent();
                    pw.println(DocGen.indentChars("    ", indent) 
                               + "<" + nodeName + ">"
                               + txt
                               + "</" + nodeName + ">");
                }
                else {
                    if (nodeName.matches(".*(?i)schema.*")) {
                        pw.println("<?xml version=\"1.0\"?>");
                    }
                    pw.print(DocGen.indentChars("    ", indent) + "<" + nodeName);
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
                              spacer = (DocGen.indentChars("    ", indent) 
                              + DocGen.indentChars(" ", nodeName.length())
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
                            if (children.item(i).getNodeType() != org.w3c.dom.Node.ATTRIBUTE_NODE) {
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
                                printXmlNodeTree2(kid, (indent + 1), pw);
                            }
                        }
                        pw.println(DocGen.indentChars("    ", indent)
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
     * A test method.
     */
    private String substituteXsdDataType(KB kb, String nsTerm, String term) {
        // System.out.println("ENTER DocGen.substituteXsdDataType( " + kb + ", \"" + nsTerm + "\", \""
        //                + term + "\")");
        String xmlType = term;
        try {
            String newType = getHasXmlType(kb, term);
            if (StringUtil.isNonEmptyString(newType)) {
                if (nsTerm.equals("ns_ddex")
                    || (!newType.equals("xs_IDREF")
                        && !newType.equals("xs_ID"))) {
                    xmlType = newType;
                }
            }
            xmlType = DB.kifToW3c(xmlType);
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
        // System.out.println("EXIT DocGen.substituteXsdDataType( " + kb + ", \"" + nsTerm + "\", \""
        //                + term + "\") -> " + "\"" + xmlType + "\"");
        return xmlType;
    }

    /// END: code for XSD generation.

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
        DocGen gen = DocGen.getInstance();

        System.out.println("terms differing only in capitalization");
        gen.capitalizationAlternates(kb.terms);

        TreeSet ts = gen.generateMessageTerms(kb);
        System.out.println(ts);
        System.out.println();
        System.out.println("Negative terms:");
        kb.terms.removeAll(ts);
        System.out.println(kb.terms);

        //String exp = "(documentation foo \"blah blah is so \\\"blah\\\" yeah\")";
        //System.out.println(exp);
        //exp = exp.replace("\\\"","\"");
        //System.out.println(exp);
        //System.out.println(kb.ask("arg",0,"headword"));
        //System.out.println(kb.ask("arg",2,"Composite"));
        //System.out.println(kb.ask("arg",2,"\"Composite\""));
        //gen.generateHTML(kb,"ddex",true);    // KB, language, simplified
    }
}
