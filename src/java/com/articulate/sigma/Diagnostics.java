package com.articulate.sigma;

/** This code is copyright Articulate Software (c) 2003.  
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
import java.io.*;
import java.text.ParseException;

/** *****************************************************************
 * A class that finds problems in a knowledge base.  It is not meant
 * to be instantiated.
 */
public class Diagnostics {

    /** *****************************************************************
     * Return a list of terms that do not have a documentation string.
     */
    public static ArrayList termsWithoutDoc(KB kb) {

        ArrayList result = new ArrayList();
        Iterator it = kb.terms.iterator();
        while (it.hasNext()) {
            String term = (String) it.next();
            ArrayList forms = kb.ask("arg",1,term);
            if (forms == null || forms.size() < 1) 
                result.add(term);
            else {
                boolean found = false;
                for (int i = 0; i < forms.size(); i++) {
                    Formula formula = (Formula) forms.get(i);
                    if (formula.theFormula.substring(1,14).equalsIgnoreCase("documentation")) 
                        found = true;
                }
                if (found == false)
                    result.add(term);
            }
        }
        return result;
    }

    /** *****************************************************************
     * Return a list of terms that do not have a documentation string.
     */
    public static ArrayList termsWithoutParent(KB kb) {

        ArrayList result = new ArrayList();
        Iterator it = kb.terms.iterator();
        while (it.hasNext()) {
            String term = (String) it.next();
            ArrayList forms = kb.ask("arg",1,term);
            if (forms == null || forms.size() < 1) 
                result.add(term);
            else {
                boolean found = false;
                for (int i = 0; i < forms.size(); i++) {
                    Formula formula = (Formula) forms.get(i);
                    if (formula.theFormula.substring(1,9).equalsIgnoreCase("instance") || 
                        formula.theFormula.substring(1,9).equalsIgnoreCase("subclass") ||
                        formula.theFormula.substring(1,13).equalsIgnoreCase("subAttribute") ||
                        formula.theFormula.substring(1,12).equalsIgnoreCase("subrelation") ||
                        formula.theFormula.substring(1,14).equalsIgnoreCase("subCollection")) 
                        found = true;
                }
                if (found == false)
                    result.add(term);
            }
        }
        return result;
    }

    /** *****************************************************************
     * Return a list of terms that have parents which are disjoint.
     */
    public static ArrayList childrenOfDisjointParents(KB kb) {

        TreeSet result = new TreeSet();
        Iterator it = kb.terms.iterator();
        while (it.hasNext()) {
            String term = (String) it.next();
            ArrayList subs = kb.askWithRestriction(0, "subclass", 1, term);
            if (subs.size() > 1) {
                ArrayList parents = new ArrayList();
                for (int i = 0; i < subs.size(); i++) {
                    Formula exp = (Formula) subs.get(i);
                    String expression = exp.theFormula;
                    String parent = expression.substring(expression.indexOf(" ",10)+1,expression.indexOf(")",10));
                    parents.add(parent.intern());
                }
                for (int i = 0; i < parents.size(); i++) {
                    String term1 = (String) parents.get(i);
                    ArrayList d = (ArrayList) kb.disjoint.get(term1.intern());
                    if (d != null && d.size() > 0) {
                        for (int j = i+1; j < parents.size(); j++) {
                            String term2 = (String) parents.get(j);
                            if (d.contains(term2.intern())) {
                                result.add(term);
                                System.out.println("INFO in childrenOfDisjointParents(): " + term1 + 
                                                   " and " + term2 + " are disjoint parents of " + term + ".");
                            }
                        }
                    }
                }
            }
        }
        ArrayList res = new ArrayList();
        res.addAll(result);
        return res;
    }

    /** *****************************************************************
     * Return a list of classes that are subclasses of a partitioned class,
     * which do not appear in the partition listing.  For example,
     * (subclass E A), (partition A B C D).  "exhaustiveDecomposition" has
     * the same meaning and needs to be checked also.
     */
    public static ArrayList extraSubclassInPartition(KB kb) {

        ArrayList result = new ArrayList();
        ArrayList forms = kb.ask("arg",0,"partition");
        if (forms == null) 
            forms = new ArrayList();
        ArrayList forms2 = kb.ask("arg",0,"exhaustiveDecomposition");
        if (forms2 != null) 
            forms.addAll(forms2);
        for (int i = 0; i < forms.size(); i++) {
            Formula form = (Formula) forms.get(i);
            String parent = form.getArgument(1);
            ArrayList partition = form.argumentsToArrayList(2);
            ArrayList subs = kb.askWithRestriction(0, "subclass", 2, parent);
            if (subs != null) {
                for (int j = 0; j < subs.size(); j++) {
                    Formula subform = (Formula) subs.get(j);
                    String child = subform.getArgument(1);
                    if (!partition.contains(child.intern())) {
                        result.add(child);
                    }
                }
            }
        }
        return result;
    }

    /** *****************************************************************
     * Find all terms which do not appear in any implication (rule).
     */
    public static ArrayList termsWithoutRules(KB kb) {

        ArrayList result = new ArrayList();
        Iterator it = kb.terms.iterator();
        while (it.hasNext()) {
            String term = (String) it.next();
            ArrayList forms = kb.ask("ant",-1,term);
            ArrayList forms2 = kb.ask("cons",-1,term);
            if (forms == null && forms2 == null) 
                result.add(term);
        }
        return result;
    }

    /** *****************************************************************
     * @return true if a quantifiers in a quantifier list is not found
     * in the body of the statement.
     */
    private static boolean quantifierNotInStatement(Formula f) {

        if (f.theFormula == null || f.theFormula.length() < 1 ||
            f.theFormula.equals("()") || f.theFormula.indexOf("(") == -1)
            return false;
        if (!f.car().equalsIgnoreCase("forall") &&                       // Recurse for complex expressions.
            !f.car().equalsIgnoreCase("exists")) {
            Formula f1 = new Formula();
            f1.read(f.car());
            Formula f2 = new Formula();
            f2.read(f.cdr());
            return (quantifierNotInStatement(f1) || quantifierNotInStatement(f2));
        }
        Formula form = new Formula();
        form.read(f.theFormula);
        if (form.car() != null && form.car().length() > 0) {    // This test shouldn't be needed.
            String rest = form.cdr();                   // Quantifier list plus rest of statement
            Formula quant = new Formula();
            quant.read(rest);

            String q = quant.car();                     // Now just the quantifier list.
            String body = quant.cdr();
            quant.read(q);
            ArrayList qList = quant.argumentsToArrayList(0);  // Put all the quantified variables into a list.
            if (rest.indexOf("exists") != -1 || rest.indexOf("forall") != -1) { //nested quantifiers
                Formula restForm = new Formula();
                restForm.read(rest);
                restForm.read(restForm.cdr());
                if (quantifierNotInStatement(restForm)) 
                    return true;
            }
            for (int i = 0; i < qList.size(); i++) {
                String var = (String) qList.get(i);
                if (body.indexOf(var) == -1) 
                    return true;
            }
        }
        return false;
    }

    /** *****************************************************************
     * Find cases where a variable appears in a quantifier list, but not
     * in the body of the quantified expression.  For example
     * (exists (?FOO) (bar ?FLOO Shmoo))
     * @return an ArrayList of Formula(s).
     */
    public static ArrayList quantifierNotInBody(KB kb) {

        ArrayList result = new ArrayList();
        ArrayList forms = kb.ask("ant",-1,"forall");        // Collect all the axioms with quantifiers.
        if (forms == null) 
            forms = new ArrayList();
        ArrayList forms2 = kb.ask("cons",-1,"forall");
        if (forms2 != null) 
            forms.addAll(forms2);
        forms2 = kb.ask("stmt",-1,"forall");
        if (forms2 != null) 
            forms.addAll(forms2);
        forms2 = kb.ask("ant",-1,"exists");
        if (forms2 != null) 
            forms.addAll(forms2);
        forms2 = kb.ask("cons",-1,"exists");
        if (forms2 != null) 
            forms.addAll(forms2);
        forms2 = kb.ask("stmt",-1,"exists");
        if (forms2 != null) 
            forms.addAll(forms2);
        for (int i = 0; i < forms.size(); i++) {             // Iterate through all the axioms.
            Formula form = (Formula) forms.get(i);
            if (quantifierNotInStatement(form)) 
                result.add(form);
        }
        return result;
    }

    /** *****************************************************************
     * Return a list of terms that do not ultimately subclass from Entity.
     */
    public static ArrayList unrootedTerms(KB kb) {

        ArrayList result = new ArrayList();
        Iterator it = kb.terms.iterator();
        while (it.hasNext()) {
            String term = (String) it.next();
            ArrayList forms = kb.ask("arg",1,term);
            if (forms == null || forms.size() < 1) {
                result.add(term);
            }
            else {
                boolean found = false;
                boolean isClassOrInstance = false;
                for (int i = 0; i < forms.size(); i++) {
                    Formula formula = (Formula) forms.get(i);
                    if (formula.theFormula.substring(1,9).equalsIgnoreCase("instance") || 
                        formula.theFormula.substring(1,9).equalsIgnoreCase("subclass")) {
                        isClassOrInstance = true;
                        String parent = formula.theFormula.substring(formula.theFormula.indexOf(" ",10)+1,formula.theFormula.indexOf(")",10));
                        ArrayList parentList = (ArrayList) kb.parents.get(parent.intern());
                        if ((parentList != null && parentList.contains("Entity")) || parent.equalsIgnoreCase("Entity")) {
                            found = true;                                                                     
                        }
                    }
                }
                if (found == false && isClassOrInstance) {
                    result.add(term);
                }
            }
        }
        return result;
    }
}
