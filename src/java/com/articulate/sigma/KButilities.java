/** This code is copyright Articulate Software (c) 2003.  Some
portions copyright Teknowledge (c) 2003 and reused under the termsof the GNU
license.  This software is released under the GNU Public License
<http://www.gnu.org/copyleft/gpl.html>.  Users of this code also consent,
by use of this code, to credit Articulate Software and Teknowledge in any
writings, briefings, publications, presentations, or other representations
of any software which incorporates, builds on, or uses this code.  Please
cite the following article in any publication with references:

Pease, A., (2003). The Sigma Ontology Development Environment, in Working
Notes of the IJCAI-2003 Workshop on Ontology and Distributed Systems,
August 9, Acapulco, Mexico. see also 
http://sigmakee.sourceforge.net 
*/

/*************************************************************************************************/
package com.articulate.sigma;
import java.io.*;
import java.util.*;

/** *****************************************************************
 *  Contains utility methods for KBs
 */
public class KButilities {

    /** *************************************************************
     */
    public static void countRelations(KB kb) {

        System.out.println("Relations: " + kb.getCountRelations());
        Iterator it = kb.terms.iterator();
        while (it.hasNext()) {
            String term = (String) it.next();
            ArrayList al = kb.ask("arg",0,term);
            if (al != null && al.size() > 0) {
                System.out.println(term + " " + al.size());
            }
        }
    }

    /** *************************************************************
     */
    public static void main(String[] args) {

        try {
            KBmanager.getMgr().initializeOnce();
            // WordNet.initOnce();
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
        KB kb = KBmanager.getMgr().getKB("SUMO");
        countRelations(kb);
    }
}

