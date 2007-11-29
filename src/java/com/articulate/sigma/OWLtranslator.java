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
     */
    private static String processStringForKIFOutput(String s) {

        if (s == null) 
            return null;
        return s.replaceAll("\"","&quot;");
    }

    /** ***************************************************************
     */
    private static String StringToKIFid(String s) {

        if (s == null) 
            return s;
        s = s.trim();
        if (s.length() < 1)
            return s;
        if (!Character.isJavaIdentifierStart(s.charAt(0)) || 
               s.charAt(0) > 122)
               s = "S" + s.substring(1);
        int i = 0;
        while (i < s.length()) {
            if (!Character.isJavaIdentifierPart(s.charAt(i)) || 
                s.charAt(i) > 122) 
                s = s.substring(0,i) + "-" + s.substring(i+1);
            i++;
        }
        return s;
    }

    /** ***************************************************************
     */
    private static String getParentReference(SimpleElement se) {

        String value = null;
        ArrayList children = se.getChildElements();
        if (children.size() > 0) {
            SimpleElement child = (SimpleElement) children.get(0);
            if (child.getTagName().equals("owl:Class")) {
                value = child.getAttribute("rdf:ID");
                if (value == null) 
                    value = child.getAttribute("rdf:about");
                if (value != null && value.indexOf("#") > -1) 
                    value = value.substring(value.indexOf("#") + 1);
            }
        }
        else {
            value = se.getAttribute("rdf:resource");
            if (value != null) {                
                if (value.indexOf("#") > -1) 
                    value = value.substring(value.indexOf("#") + 1);
            }
        }
        return StringToKIFid(value);
    }

    /** ***************************************************************
     * Write OWL format.
     */
    private static void decode(SimpleElement se, String parentTerm, String parentTag) {

        String tag = se.getTagName();
        String value = null;
        // System.out.println(";; " + tag);
        if (tag.equals("owl:Class") || tag.equals("owl:ObjectProperty") || 
            tag.equals("owl:DatatypeProperty") || tag.equals("owl:FunctionalProperty") || 
            tag.equals("owl:InverseFunctionalProperty") || tag.equals("owl:TransitiveProperty") || 
            tag.equals("owl:SymmetricProperty")) {
            parentTerm = se.getAttribute("rdf:ID");
            if (parentTerm != null) {
                if (parentTerm.indexOf("#") > -1) 
                    parentTerm = parentTerm.substring(parentTerm.indexOf("#") + 1);
            }
            else {
                parentTerm = se.getAttribute("rdf:about");
                if (parentTerm != null) {
                    if (parentTerm.indexOf("#") > -1) 
                        parentTerm = parentTerm.substring(parentTerm.indexOf("#") + 1);
                }
            }
            parentTerm = StringToKIFid(parentTerm);
            if ((tag.equals("owl:ObjectProperty") || tag.equals("owl:DatatypeProperty") || 
                 tag.equals("owl:InverseFunctionalProperty")) && parentTerm != null) 
                System.out.println("(instance " + parentTerm + " BinaryRelation)");  
            if (tag.equals("owl:TransitiveProperty") && parentTerm != null) 
                System.out.println("(instance " + parentTerm + " TransitiveRelation)");              
            if (tag.equals("owl:FunctionalProperty") && parentTerm != null) 
                System.out.println("(instance " + parentTerm + " SingleValuedRelation)");              
            if (tag.equals("owl:SymmetricProperty") && parentTerm != null) 
                System.out.println("(instance " + parentTerm + " SymmetricRelation)");              
        }
        else if (tag.equals("rdfs:domain")) {
            value = se.getAttribute("rdf:resource");
            if (value != null) {                
                if (value.indexOf("#") > -1) 
                    value = value.substring(value.indexOf("#") + 1);
                //kb.tell("(domain " + parentTerm + " 1 " + value + ")");
                value = StringToKIFid(value);
                if (value != null && parentTerm != null) 
                    System.out.println("(domain " + parentTerm + " 1 " + value + ")");
            }
        }
        else if (tag.equals("rdfs:range")) {
            value = se.getAttribute("rdf:resource");
            if (value != null) {                
                if (value.indexOf("#") > -1) 
                    value = value.substring(value.indexOf("#") + 1);
                //kb.tell("(domain " + parentTerm + " 2 " + value + ")");
                value = StringToKIFid(value);
                if (value != null && parentTerm != null) 
                    System.out.println("(domain " + parentTerm + " 2 " + value + ")");
            }
        }
        else if (tag.equals("rdfs:comment")) {
            String text = se.getText();
            text = processStringForKIFOutput(text);
            //kb.tell("(documentation " + parentTerm + " \"" + text + "\")");
            if (parentTerm != null && text != null) 
                System.out.println(DB.wordWrap("(documentation " + parentTerm + " \"" + text + "\")",70));
        }
        else if (tag.equals("owl:inverseOf")) {
            ArrayList children = se.getChildElements();
            if (children.size() > 0) {
                SimpleElement child = (SimpleElement) children.get(0);
                if (child.getTagName().equals("owl:ObjectProperty") || 
                    child.getTagName().equals("owl:InverseFunctionalProperty")) {
                    value = child.getAttribute("rdf:ID");
                    if (value == null) 
                        value = child.getAttribute("rdf:about");
                    if (value == null) 
                        value = child.getAttribute("rdf:resource");
                    if (value != null && value.indexOf("#") > -1) 
                        value = value.substring(value.indexOf("#") + 1);
                }
            }
            //kb.tell("(inverse " + parentTerm + " " + value + ")");
            value = StringToKIFid(value);
            if (value != null && parentTerm != null) 
                System.out.println("(inverse " + parentTerm + " " + value + ")");
        }
        else if (tag.equals("rdfs:subClassOf")) {
            value = getParentReference(se);
            value = StringToKIFid(value);
            //kb.tell("(subclass " + parentTerm + " " + value + ")");
            if (value != null) 
                System.out.println("(subclass " + parentTerm + " " + value + ")");           
            //else
              //  System.out.println(";; missing or unparsed subclass statment for " + parentTerm);
        }
        else if (tag.equals("owl:Restriction")) { }
        else if (tag.equals("owl:onProperty")) { }
        else if (tag.equals("owl:unionOf")) { return; }
        else if (tag.equals("owl:complimentOf")) { return; }
        else if (tag.equals("owl:intersectionOf")) { return; }
        else if (tag.equals("owl:cardinality")) { }
        else if (tag.equals("owl:FunctionalProperty")) {
            value = se.getAttribute("rdf:ID");
            if (value != null) {                
                if (value.indexOf("#") > -1) 
                    value = value.substring(value.indexOf("#") + 1);
                //kb.tell("(domain " + parentTerm + " 2 " + value + ")");
                value = StringToKIFid(value);
                System.out.println("(instance " + value + " SingleValuedRelation)");
            }
        }
        else if (tag.equals("owl:minCardinality")) { }
        else if (tag.equals("owl:maxCardinality")) { }
        else if (tag.equals("rdf:type")) {
            value = getParentReference(se);
            value = StringToKIFid(value);
            if (value != null) 
                System.out.println("(instance " + parentTerm + " " + value + ")"); 
           // else
                // System.out.println(";; missing or unparsed subclass statment for " + parentTerm);
        }
        else {
            value = se.getAttribute("rdf:resource");
            if (value != null) {                
                if (value.indexOf("#") > -1) 
                    value = value.substring(value.indexOf("#") + 1);
                //kb.tell("(domain " + parentTerm + " 2 " + value + ")");
                value = StringToKIFid(value);
                tag = StringToKIFid(tag);
                if (value != null && parentTerm != null) 
                    System.out.println("(" + tag + " " + parentTerm + " " + value + ")");
            }
            else {
                String text = se.getText();
                text = processStringForKIFOutput(text);
                tag = StringToKIFid(tag);
                if (!DB.emptyString(text)) {
                    if (parentTerm != null && tag != null && text != null) 
                        System.out.println("(" + tag + " " + parentTerm + " \"" + text + "\")"); 
                }
                else {
                    ArrayList children = se.getChildElements();
                    if (children.size() > 0) {
                        SimpleElement child = (SimpleElement) children.get(0);
                        if (child.getTagName().equals("owl:Class")) {
                            value = child.getAttribute("rdf:ID");
                            if (value == null) 
                                value = child.getAttribute("rdf:about");
                            if (value != null && value.indexOf("#") > -1) 
                                value = value.substring(value.indexOf("#") + 1);
                            if (value != null && parentTerm != null) 
                                System.out.println("(" + tag + " " + parentTerm + " " + value + ")");
                        }
                    }
                }
            }
        }

        Set s = se.getAttributeNames();
        Iterator it = s.iterator();
        while (it.hasNext()) {
            String att = (String) it.next();
            String val = (String) se.getAttribute(att);
        }
        ArrayList al = se.getChildElements();
        it = al.iterator();
        while (it.hasNext()) {
            SimpleElement child = (SimpleElement) it.next();
            decode(child,parentTerm,tag);
        }
        //System.out.println(se.toString());
    }

    /** ***************************************************************
     * Write OWL format.
     */
    public static void read(String filename) {

        SimpleElement se = SimpleDOMParser.readFile(filename);
        decode(se,"","");
        //System.out.println(se.toString());
    }

    /** ***************************************************************
     * Write OWL format.
     */
    public static String processDoc(String doc) {

        String result = doc;
        result = result.replaceAll("&%","");
        result = result.replaceAll("&","&#38;");
        result = result.replaceAll(">","&gt;");
        result = result.replaceAll("<","&lt;");
        return result;
    }

    /** ***************************************************************
     * Write OWL format.
     */
    private void writeRelations(PrintWriter pw, String term) {

        pw.println("<owl:ObjectProperty rdf:ID=\"" + term + "\">");
        ArrayList argTypes = kb.askWithRestriction(0,"domain",1,term);  // domain expressions for term.
        ArrayList subs = kb.askWithRestriction(0,"subrelation",1,term);  // domain expressions for term.
        if (argTypes.size() > 0) {
            for (int i = 0; i < argTypes.size(); i++) {
                Formula form = (Formula) argTypes.get(i);
                String arg = form.getArgument(2);
                String superProp = form.getArgument(3);
                if (arg.equals("1") && Formula.atom(superProp)) 
                    pw.println("  <owl:domain rdf:resource=\"#" + superProp + "\" />");
                if (arg.equals("2") && Formula.atom(superProp)) 
                    pw.println("  <owl:range rdf:resource=\"#" + superProp + "\" />");
            }
        }
        if (subs.size() > 0) {
            for (int i = 0; i < subs.size(); i++) {
                Formula form = (Formula) subs.get(i);
                String superProp = form.getArgument(2);
                pw.println("  <owl:subPropertyOf rdf:resource=\"#" + superProp + "\" />");
            }
        }
        ArrayList doc = kb.askWithRestriction(0,"documentation",1,term);    // documentation expressions for term.
        if (doc.size() > 0) {                                               // Note that this ignores the language parameter
            Formula form = (Formula) doc.get(0);
            String documentation = form.getArgument(3);
            if (documentation != null) 
                pw.println("  <owl:comment>" + processDoc(documentation) + "</owl:comment>");
        }
      /**  if (!term.equals("instance") && !term.equals("subclass") && 
            !term.equals("documentation") && !term.equals("subrelation")) {
            ArrayList statements = kb.ask("arg",0,term);
            for (int i = 0; i < statements.size(); i++) {
                Formula form = (Formula) statements.get(i);
                String domain = form.getArgument(1);
                String range = form.getArgument(2);
                pw.println("  <owl:subPropertyOf rdf:resource=\"#" + superProp + "\" />");
            }
        } */
        pw.println("</owl:ObjectProperty>");
        pw.println();
    }

    /** ***************************************************************
     */
    private void writeInstances(PrintWriter pw, String term, ArrayList instances, String documentation) {

        pw.println("<rdf:Description rdf:ID=\"" + term + "\">");
        for (int i = 0; i < instances.size(); i++) {
            Formula form = (Formula) instances.get(i);
            String parent = form.getArgument(2);
            if (Formula.atom(parent)) 
                pw.println("  <rdf:type rdf:resource=\"#" + parent + "\"/>");
        }
        if (documentation != null) 
            pw.println("  <rdfs:comment>" + processDoc(documentation) + "</rdfs:comment>");
        pw.println("</rdf:Description>");
        pw.println();
    }

    /** ***************************************************************
     */
    private void writeClasses(PrintWriter pw, String term, ArrayList classes, 
                              String documentation, boolean isInstance) {

        if (isInstance)         
            pw.println("<owl:Class rdf:About=\"" + term + "\">");
        else
            pw.println("<owl:Class rdf:ID=\"" + term + "\">");
        for (int i = 0; i < classes.size(); i++) {
            Formula form = (Formula) classes.get(i);
            String parent = form.getArgument(2);
            if (Formula.atom(parent)) 
                pw.println("  <rdfs:subClassOf rdf:resource=\"#" + parent + "\"/>");
        }
        if (documentation != null) 
            pw.println("  <rdfs:comment>" + processDoc(documentation) + "</rdfs:comment>");
        pw.println("</owl:Class>");
        pw.println();
    }

    /** ***************************************************************
     * Write OWL format.
     */
    public void write(String filename) throws IOException {

        FileWriter fw = null;
        PrintWriter pw = null; 

        try {
            fw = new FileWriter(filename);
            pw = new PrintWriter(fw);
 
            pw.println("<rdf:RDF");
            pw.println("xmlns:rdf =\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\"");
            pw.println("xmlns:rdfs=\"http://www.w3.org/2000/01/rdf-schema#\"");
            pw.println("xmlns:owl =\"http://www.w3.org/2002/07/owl#\">");

            pw.println("<owl:Ontology rdf:about=\"\">");
                pw.println("<rdfs:comment xml:lang=\"en\">A provisional and necessarily lossy translation to OWL.  Please see");
                pw.println("www.ontologyportal.org for the original KIF, which is the authoritative");
                pw.println("source.  This software is released under the GNU Public License"); 
                pw.println("www.gnu.org.</rdfs:comment>");
              pw.println("</owl:Ontology>");
            Iterator it = kb.terms.iterator();
            while (it.hasNext()) {
                String term = (String) it.next();
                if (kb.childOf(term,"BinaryRelation")) 
                    writeRelations(pw,term);                
                if (Character.isUpperCase(term.charAt(0)) &&
                    !kb.childOf(term,"Function")) {
                    ArrayList instances = kb.askWithRestriction(0,"instance",1,term);  // Instance expressions for term.
                    ArrayList classes = kb.askWithRestriction(0,"subclass",1,term);    // Class expressions for term.
                    String documentation = null;
                    Formula form;
                    ArrayList doc = kb.askWithRestriction(0,"documentation",1,term);    // Class expressions for term.
                    if (doc.size() > 0) {
                        form = (Formula) doc.get(0);
                        documentation = form.getArgument(3);
                    }
                    if (instances.size() > 0 && !kb.childOf(term,"BinaryRelation"))
                        writeInstances(pw,term,instances,documentation);   
                    boolean isInstance = false;
                    if (classes.size() > 0) {
                        if (instances.size() > 0) 
                            isInstance = true;
                        writeClasses(pw,term,classes,documentation,isInstance); 
                        isInstance = false;
                    }
                }
            }
            pw.println("</rdf:RDF>");
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

    /** *************************************************************
     * A test method.
     */
    public static void main(String args[]) {
/**
        OWLtranslator ot = new OWLtranslator();
        ot.kb = new KB("foo","");
        try {
            ot.kb.addConstituent("C:\\Program Files\\Apache Tomcat 4.0\\KBs\\test.txt");
            ot.write("sample-owl.owl");
        }
        catch (Exception e) {
            System.out.println(e.getMessage());
        }
 * */
        read(args[0]);
    }

}


