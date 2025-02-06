/** This code is copyrighted by Rearden Commerce (c) 2011.  It is
released under the GNU Public License &lt;http://www.gnu.org/copyleft/gpl.html&gt;.

Users of this code also consent, by use of this code, to credit
Articulate Software in any writings, briefings, publications,
presentations, or other representations of any software which
incorporates, builds on, or uses this code.  Please cite the following
article in any publication with references:

Pease, A., (2003). The Sigma Ontology Development Environment, in Working
Notes of the IJCAI-2003 Workshop on Ontology and Distributed Systems,
August 9, Acapulco, Mexico.  See also http://sigmakee.sourceforge.net.
 */

/*************************************************************************************************/
package com.articulate.sigma;

import com.articulate.sigma.wordNet.WordNet;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.LineNumberReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** A class that generates a GUI for elements of the ontology. */
public class EditGUI {

    public static List<String> allowedTerms = new ArrayList<>();
    public static boolean initOnce = false;

    /** *************************************************************
     */
    public static void initOnce() {

        if (initOnce) return;
        LineNumberReader lnr = null;
        String fname = "allowedTerms.txt";
        String kbDir = KBmanager.getMgr().getPref("kbDir");

        try {
            File fin  = new File(kbDir + File.separator + fname);
            FileReader fr = new FileReader(fin);
            if (fr != null) {
                lnr = new LineNumberReader(fr);
                String line;
                while ((line = lnr.readLine()) != null) {
                    line = line.trim();
                    allowedTerms.add(line);
                }
            }
        }
        catch (IOException ioe) {
            System.err.println("Error in EditGUI.readConfig() reading file: " + kbDir + File.separator + fname);
            System.out.println(ioe.getMessage());
        }
        finally {
            try {
                if (lnr != null) lnr.close();
            }
            catch (IOException e) {
                System.out.println("Exception in EditGUI.readConfig()");
            }
        }
        initOnce = true;
    }

    /** *************************************************************
     */
    public String genRelPage(KB kb, String rel) {

        StringBuilder sb = new StringBuilder();

        // get classes or instances that apply for each argument
        return sb.toString();
    }

    /** *************************************************************
     * Get and print all instances of a class
     */
    public static String printInstances(KB kb, String className) {

        StringBuilder sb = new StringBuilder();

        Set ts = kb.getAllInstances(className);
        Iterator it = ts.iterator();
        String term;
        while (it.hasNext()) {
            term = (String) it.next();
            sb.append(term);
            if (it.hasNext()) sb.append(", ");
        }
        return sb.toString();
    }

    /** *************************************************************
     * Get the all parent classes of term.  Return the full transitive closure
     * of instance and subclass through the subclass relation all the way
     * to the root term of the hierarchy.
     * @return an ArrayList of term strings
     */
    public static List<String> genAllParentList(KB kb, String term) {

        List<String> parents = new ArrayList<>();
        Set<String> immedParents = new HashSet<>();
        List<Formula> res = kb.askWithRestriction(0,"instance",1,term);
        Formula f;
        for (int i = 0; i < res.size(); i++) {
            f = res.get(i);
            immedParents.add(f.getStringArgument(2));
        }
        res = kb.askWithRestriction(0,"subclass",1,term);
        for (int i = 0; i < res.size(); i++) {
            f = res.get(i);
            immedParents.add(f.getStringArgument(2));
        }
        parents.addAll(kb.getAllSuperClasses(immedParents));
        return parents;
    }

    /** *************************************************************
     * Get the just the immediate parent classes of a term
     * @return an ArrayList of term strings
     */
    public static List<String> genImmedParentList(KB kb, String term) {

        List<String> parents = new ArrayList<>();
        List<Formula> res = kb.askWithRestriction(0,"instance",1,term);
        Formula f;
        for (int i = 0; i < res.size(); i++) {
            f = res.get(i);
            parents.add(f.getStringArgument(2));
        }
        res = kb.askWithRestriction(0,"subclass",1,term);
        for (int i = 0; i < res.size(); i++) {
            f = res.get(i);
            parents.add(f.getStringArgument(2));
        }
        return parents;
    }

    /** *************************************************************
     * Collect a set of relations in which the type of given term is an argument instance type,
     * and collect the set of possible argument fillers.
     * @return a map of relation name keys and array values, where each element of
     * the array is a particular argument, and consists of an array of possible
     * string values.
     */
    public static Map<String,List<List<String>>> genInstList(KB kb, String term) {

        Map<String,List<List<String>>> result = new HashMap<>();

        List<String> parents = genAllParentList(kb,term);
        // get relations that apply
        List<String> relations = new ArrayList<>();
        String parent;
        List<Formula> res;
        Formula f;
        for (int i = 0; i < parents.size(); i++) {
            parent = parents.get(i);
            res = kb.askWithRestriction(0,"domain",3,parent);
            //sb.append(";; res = " + Integer.toString(res.size()) + " for " +
            //		  "(domain ?X ?Y " + parent + ")");
            for (int k = 0; k < res.size(); k++) {
                f = res.get(k);
                relations.add(f.getStringArgument(1));
            }
        }

        // get the arity of each relation
        Map<String,Integer> arity = new HashMap<>();
        String r;
        int a;
        for (int i = 0; i < relations.size(); i++) {
            r = relations.get(i);
            a = kb.kbCache.valences.get(r);
            arity.put(r, a);
        }

        List al, al2;
        String className;
        for (int i = 0; i < relations.size(); i++) {
            r = relations.get(i);
            al = new ArrayList();
            result.put(r,al);
            a = arity.get(r);
            for (int j = 1; j <= a; j++) {
                al2 = new ArrayList();
                className = kb.getArgTypeClass(r,j);
                if (className.endsWith("+"))
                    al2.addAll(kb.kbCache.getChildClasses(className.substring(0,className.length()-1)));
                else
                    al2.addAll(kb.getAllInstances(className));
                al.add(al2);
            }
        }
        return result;
    }

    /** *************************************************************
     * Collect a set of relations in which the type of given term is an argument subclass,
     * and collect the set of possible argument fillers.
     * @return a map of relation name keys and array values, where each element of
     * the array is a particular argument, and consists of an array of possible
     * string values.
     */
    public static Map<String,List<List<String>>> genClassList(KB kb, String term) {

        Map<String,List<List<String>>> result = new HashMap<>();

        List<String> parents = genAllParentList(kb,term);
        // get relations that apply
        List<String> relations = new ArrayList<>();
        List<Formula> res;
        String parent;
        Formula f;
        for (int i = 0; i < parents.size(); i++) {
            parent = parents.get(i);
            res = kb.askWithRestriction(0,"domainSubclass",3,parent);
            //sb.append(";; res = " + Integer.toString(res.size()) + " for " +
            //		  "(domain ?X ?Y " + parent + ")");
            for (int k = 0; k < res.size(); k++) {
                f = res.get(k);
                relations.add(f.getStringArgument(1));
            }
        }

        // get the arity of each relation
        Map<String,Integer> arity = new HashMap<>();
        String r;
        int a;
        for (int i = 0; i < relations.size(); i++) {
            r = relations.get(i);
            a = kb.kbCache.valences.get(r);
            arity.put(r, a);
        }

        List al, al2;
        String className;
        for (int i = 0; i < relations.size(); i++) {
            r = relations.get(i);
            al = new ArrayList();
            result.put(r,al);
            a = arity.get(r);
            for (int j = 1; j <= a; j++) {
                al2 = new ArrayList();
                className = kb.getArgTypeClass(r,j);
                if (className.endsWith("+"))
                    al2.addAll(kb.kbCache.getChildClasses(className.substring(0,className.length()-1)));
                else
                    al2.addAll(kb.getAllInstances(className));
                al.add(al2);
            }
        }
        return result;
    }

    /** *************************************************************
     * Generate fields for an HTML form that allow a user to assert
     * statements by using menus to set parameters that are arguments
     * to relations.
     */
    public static String genInstPage(KB kb, String term, String kbHref) {

        initOnce();
        StringBuilder sb = new StringBuilder();
        Map<String,List<List<String>>> instList = EditGUI.genInstList(kb,term);
        List<String> parents = genAllParentList(kb,term);

        // show the instance and its class
        sb.append("Instance relations for: <font size=+3><a href=\"").append(kbHref).append(term).append("\">").append(term).append("</a></font>:");
        String parent;
        for (int i = 0; i < parents.size(); i++) {
            parent = parents.get(i);
            sb.append("<a href=\"").append(kbHref).append(parent).append("\">").append(parent).append("</a>,");
        }
        sb.append("<P>\n");
        sb.append("<table>\n");
        sb.append("<tr><td><b>relation</b></td><td><b>arguments</b></td><td><b>assert?</b></td></tr>\n");
        // get relevant relations and their argument types
        List<List<String>> arguments;
        List<String> fillers;
        for (String relation : instList.keySet()) {
            if (allowedTerms.size() < 1 || allowedTerms.contains(relation)) {
                sb.append("<tr><td><a href=\"").append(kbHref).append(relation).append("\">").append(relation).append("</a>:</td><td>");
                arguments = instList.get(relation);
                for (int i = 0; i < arguments.size(); i++) {
                    fillers = arguments.get(i);
                    if (fillers.size() < 1)
                        sb.append("<input type=\"text\" name=\"").append(relation).append("--").append(Integer.toString(i)).append("\" >");
                    else
                        sb.append(HTMLformatter.createMenu(relation + "--" + Integer.toString(i),"", fillers, ""));
                }
                sb.append("</td><td><input type=\"checkbox\" name=\"checkbox-").append(relation).append("\" value=\"checkbox-").append(relation).append("\" /></td></tr>\n");
            }
        }
        sb.append("</table>\n");
        // for each relation, get other class and instances that apply
        return sb.toString();
    }

    /** *************************************************************
     * Generate a set of menus comprising relations that apply to this class
     * i.e. which has (domainSubclass [relation] [N] [thisclass])
     * If the allowedTerms list is non-empty, ensure that only terms in that
     * list are used.
     */
    public static String genClassPage(KB kb, String term, String kbHref) {

        initOnce();
        StringBuilder sb = new StringBuilder();
        Map<String,List<List<String>>> instList = EditGUI.genClassList(kb,term);
        List<String> parents = genAllParentList(kb,term);

        // show the instance and its class
        sb.append("Class relations for: <font size=+3><a href=\"").append(kbHref).append(term).append("\">").append(term).append("</a></font>:");
        String parent;
        for (int i = 0; i < parents.size(); i++) {
            parent = parents.get(i);
            sb.append("<a href=\"").append(kbHref).append(parent).append("\">").append(parent).append("</a>,");
        }
        sb.append("<P>\n");
        sb.append("<table>\n");
        sb.append("<tr><td><b>relation</td><td>arguments</b></td><td><b>assert?</b></td></tr>\n");
        // get relevant relations and their argument types
        List<List<String>> arguments;
        List<String> fillers;
        for (String relation : instList.keySet()) {
            if (allowedTerms.size() < 1 || allowedTerms.contains(relation)) {
                sb.append("<tr><td><a href=\"").append(kbHref).append(relation).append("\">").append(relation).append("</a>:</td><td>");
                arguments = instList.get(relation);
                for (int i = 0; i < arguments.size(); i++) {
                    fillers = arguments.get(i);
                    if (fillers.size() < 1)
                        sb.append("<input type=\"text\" name=\"").append(relation).append("--").append(Integer.toString(i)).append("\" >");
                    else
                        sb.append(HTMLformatter.createMenu(relation + "--" + Integer.toString(i),"", fillers, ""));
                }
                sb.append("</td><td><input type=\"checkbox\" name=\"checkbox-").append(relation).append("\" value=\"checkbox-").append(relation).append("\" /></td></tr>\n");
            }
        }
        sb.append("</table>\n");
        // for each relation, get other class and instances that apply
        return sb.toString();
    }

    /** *************************************************************
     * Interpret a map as a key relation name and ArrayList of values as arguments.
     * @return a String status message that includes a hyperlinked presentation of each
     * formula that is successfully asserted.
     */
    public static String assertFacts(KB kb, Map<String, List<String>> cbset, String kbHref) {

        System.out.println("INFO in EditGUI.assertFacts(): cbset: "+ cbset);
        StringBuilder status = new StringBuilder();
        StringBuilder sb;
        List<String> al;
        String val;
        Formula f;
        for (String rel : cbset.keySet()) {
            sb = new StringBuilder();
            sb.append("(").append(rel);
            al = cbset.get(rel);
            for (int i = 0; i < al.size(); i++) {
                val = al.get(i);
                sb.append(" ").append(val);
            }
            sb.append(")");
            f = new Formula();
            f.read(sb.toString());
            status.append(f.htmlFormat(kbHref)).append("<P>\n");
            status.append(kb.tell(sb.toString())).append("<P>\n");
        }
        return status.toString();
    }

    /** *************************************************************
     * A test method.
     */
    public static void main (String args[]) {

        try {
            KBmanager.getMgr().initializeOnce();
            WordNet.initOnce();
        }
        catch (Exception e) {
            System.err.println(e.getMessage());
        }
        KB kb = KBmanager.getMgr().getKB(KBmanager.getMgr().getPref("sumokbname"));
        System.out.println(genInstPage(kb,"UnitedStates",""));
    }

}
