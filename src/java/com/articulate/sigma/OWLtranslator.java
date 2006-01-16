package com.articulate.sigma;

import java.util.*;
import java.io.*;

/** This code is copyright Articulate Software (c) 2004.  
This software is released under the GNU Public License <http://www.gnu.org/copyleft/gpl.html>.
Users of this code also consent, by use of this code, to credit Articulate Software
in any writings, briefings, publications, presentations, or 
other representations of any software which incorporates, builds on, or uses this 
code.  Please cite the following article in any publication with references:

Pease, A., (2003). The Sigma Ontology Development Environment, 
in Working Notes of the IJCAI-2003 Workshop on Ontology and Distributed Systems,
August 9, Acapulco, Mexico.
*/

/** Read and write OWL format from Sigma data structures.
 */
public class OWLtranslator {

    public KB kb;


    /** ***************************************************************
     * Write OWL format.
     */
    public void write() {

        System.out.println("<rdf:RDF");
        System.out.println("xmlns:rdf ='http://www.w3.org/1999/02/22-rdf-syntax-ns#'");
        System.out.println("xmlns:rdfs='http://www.w3.org/2000/01/rdf-schema#'");
        System.out.println("xmlns:owl ='http://www.w3.org/2002/07/owl#'");
        System.out.println("xmlns:sumo='http://www.ontologyportal.org/translations/SUMO.owl.txt#'>");
        Iterator it = kb.terms.iterator();
        while (it.hasNext()) {
            String term = (String) it.next();
            if (Character.isUpperCase(term.charAt(0)) &&
                (term.substring(term.length()-2,term.length()) != "Fn")) {
                ArrayList instances = kb.askWithRestriction(0,"instance",1,term);  // Instance expressions for term.
                ArrayList classes = kb.askWithRestriction(0,"subclass",1,term);    // Class expressions for term.
                String documentation = null;
                Formula form;
                ArrayList doc = kb.askWithRestriction(0,"documentation",1,term);    // Class expressions for term.
                if (doc.size() > 0) {
                    form = (Formula) doc.get(0);
                    documentation = form.getArgument(2);
                }

                if (instances.size() > 0) {                
                    System.out.println("<rdf:Description rdf:ID='" + term + "'>");
                    for (int i = 0; i < instances.size(); i++) {
                        form = (Formula) instances.get(i);
                        String parent = form.getArgument(2);
                        System.out.println("<rdf:type rdf:resource='#" + parent + "'/>");
                    }
                    if (documentation != null) 
                        System.out.println("<rdfs:comment>" + documentation + "</rdfs:comment>");
                    System.out.println("</rdf:Description>");
                    System.out.println();
                }
                if (classes.size() > 0) {
                    System.out.println("<rdfs:Class rdf:ID='" + term + "'>");
                    for (int i = 0; i < instances.size(); i++) {
                        form = (Formula) classes.get(i);
                        String parent = form.getArgument(2);
                        System.out.println("<rdf:subClassOf rdf:resource='#" + parent + "'/>");
                    }
                    if (documentation != null) 
                        System.out.println("<rdfs:comment>" + documentation + "</rdfs:comment>");
                    System.out.println("</rdf:Class>");
                    System.out.println();
                }
            }
        }

        System.out.println("</rdf:RDF>");
    }

    /** *************************************************************
     * A test method.
     */
    public static void main(String args[]) {

        OWLtranslator ot = new OWLtranslator();
        ot.kb = new KB("foo","");
        try {
            ot.kb.addConstituent("C:\\Program Files\\Apache Tomcat 4.0\\KBs\\test.txt");
        }
        catch (Exception e) {
            System.out.println(e.getMessage());
        }
        ot.write();
    }

}


