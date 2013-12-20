
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
import com.articulate.sigma.KB;

public class Editor {

    /** *************************************************************
     * Create an HTML form for editing facts about a term.
     */
    public static String createFormPage(KB kb, String term, Formula f) {

        if (f.theFormula.indexOf("(",1) < 0) {  // a simple statement
            Formula temp = new Formula();
            temp.read(f.theFormula);
            String relation = temp.car();
            temp.read(temp.cdr());
            
            while (!temp.empty()) {
                String t = temp.car();
                temp.read(temp.cdr());
            }
            return "";
        }
        else 
            return "Editing of complex statements not currently supported.<P>";        
    }

    /** *************************************************************
     * A test method.
     */
    public static void main (String args[]) {

    }
}
