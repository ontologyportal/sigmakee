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
