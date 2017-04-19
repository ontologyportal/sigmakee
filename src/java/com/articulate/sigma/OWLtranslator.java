package com.articulate.sigma;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.LineNumberReader;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import com.articulate.sigma.KB;
/** This code is copyright Articulate Software (c) 2010.
This software is released under the GNU Public License <http://www.gnu.org/copyleft/gpl.html>.
Users of this code also consent, by use of this code, to credit Articulate Software
in any writings, briefings, publications, presentations, or 
other representations of any software which incorporates, builds on, or uses this 
code.  Please cite the following article in any publication with references:
Pease, A., (2003). The Sigma Ontology Development Environment, 
in Working Notes of the IJCAI-2003 Workshop on Ontology and Distributed Systems,
August 9, Acapulco, Mexico. See also sigmakee.sourceforge.net
*/
/** Read and write OWL format from Sigma data structures.
 */
public class OWLtranslator {
    public static OWLtranslator ot = new OWLtranslator();
    public static boolean initNeeded = true;
    public KB kb;
    /** Relations in SUMO that have a corresponding relation in
     *  OWL and therefore require special treatment. */
    private static ArrayList SUMOReservedRelations = 
        new ArrayList(Arrays.asList("disjoint",                 // owl:disjointWith
                                    "disjointDecomposition",    // owl:distinctMembers
                                    "documentation",            // rdfs:comment
                                    "domain",                   // rdfs:domain
                                    "instance",
                                    "inverse",                  // owl:inverseOf
                                    "range",                    // rdfs:range
                                    "subclass",                 // rdfs:subClassOf
                                    "subrelation",
                                    "synonymousExternalConcept")); // owl:sameAs or owl:equivalentClass or owl:equivalentProperty
    private static ArrayList OWLReservedRelations =             // c=class, i=instance, r=relation
        new ArrayList(Arrays.asList("rdf:about",
                                    "rdf:ID",
                                    "rdf:nodeID",
                                    "rdf:resource",
                                    "rdfs:comment",             // SUMO:documentation
                                    "rdfs:domain",              // SUMO:domain 1
                                    "rdfs:range",               // SUMO:domain 1 or SUMO:range
                                    "rdfs:subClassOf",          // SUMO:subclass
                                    "owl:allValuesFrom",
                                    "owl:backwardCompatibleWith",
                                    "owl:cardinality",
                                    "owl:complimentOf",         // c,c : not allowed in OWL-Lite
                                    "owl:differentFrom",        // i,i
                                    "owl:disjointWith",         // c,c : SUMO:disjoint, not allowed in OWL-Lite
                                    "owl:distinctMembers",      // SUMO:disjointDecomposition
                                    "owl:equivalentClass",      // c,c : SUMO:synonymousExternalConcept
                                    "owl:equivalentProperty",   // r,r : SUMO:synonymousExternalConcept
                                    "owl:hasValue",             // not allowed in OWL-Lite
                                    "owl:imports",
                                    "owl:incompatibleWith",
                                    "owl:intersectionOf",
                                    "owl:inverseOf",            // r,r : SUMO:inverse                                                         
                                    "owl:maxCardinality",
                                    "owl:minCardinality",
                                    "owl:oneOf",                // not allowed in OWL-Lite
                                    "owl:onProperty",
                                    "owl:priorVersion",
                                    "owl:sameAs",               // i,i : SUMO:synonymousExternalConcept (OWL instances)
                                    "owl:someValuesFrom",
                                    "owl:unionOf",              // not allowed in OWL-Lite
                                    "owl:versionInfo"
                                    ));
    /** OWL DL requires a pairwise separation between classes,
     *  datatypes, datatype properties, object properties,
     *  annotation properties, ontology properties (i.e., the import
     *  and versioning stuff), individuals, data values and the
     *  built-in vocabulary. */
    private static ArrayList OWLReservedClasses = 
        new ArrayList(Arrays.asList("rdf:List",
                                    "rdf:Property",
                                    "rdfs:Class",
                                    "owl:AllDifferent",
                                    "owl:AnnotationProperty",
                                    "owl:Class", // same as rdfs:Class for OWL-Full
                                    "owl:DataRange",  // not allowed in OWL-Lite
                                    "owl:DatatypeProperty",
                                    "owl:DeprecatedClass",
                                    "owl:DeprecatedProperty",
                                    "owl:FunctionalProperty",
                                    "owl:InverseFunctionalProperty",
                                    "owl:Nothing",
                                    "owl:ObjectProperty", // same as rdf:Property for OWL-Full
                                    "owl:Ontology",
                                    "owl:OntologyProperty",
                                    "owl:Restriction",
                                    "owl:SymmetricProperty",
                                    "owl:Thing", // any instance - same as rdfs:Resource for OWL-Full
                                    "owl:TransitiveProperty"
                                    ));
    /** A map of functional statements and the automatically
     *  generated term that is created for it. */
    private HashMap functionTable = new HashMap();
    /** Keys are SUMO term name Strings, values are YAGO/DBPedia
     *  term name Strings. */
    private HashMap SUMOYAGOMap = new HashMap();
    private TreeMap axiomMap = new TreeMap();
    private static String termPrefix = ""; //"http://sigma.ontologyportal.org:4010/OWL.jsp";
    
    private static int _debugLevelCounter = 0;

    /** ***************************************************************
     */
    private static String processStringForXMLOutput(String s) {

        if (s == null) 
            return null;
        s = s.replaceAll("<","&lt;");
        s = s.replaceAll(">","&gt;");
        s = s.replaceAll("&","&amp;");
        return s;
    }

    /** ***************************************************************
     */
    private static String processStringForKIFOutput(String s) {

        if (s == null) 
            return null;
        return s.replaceAll("\"","&quot;");
    }

    /** ***************************************************************
     *  Remove quotes around a string
     */
    public static String removeQuotes(String s) {

        if (s == null) 
            return s;
        s = s.trim();
        if (s.length() < 1)
            return s;
        if (s.length() > 1 && s.charAt(0) == '"' && s.charAt(s.length()-1) == '"') 
            s = s.substring(1,s.length()-1);        
        return s;
    }

    /** ***************************************************************
     *  Turn a function statement into an identifier.
     */
    private String instantiateFunction(String s) {

        String result = removeQuotes(s);
        result = result.substring(1,s.length()-1);  // remove outer parens
        result = StringUtil.StringToKIFid(result);
        functionTable.put(s,result);
        return result;
    }

    /** ***************************************************************
     *  State definitional information for automatically defined
     *  terms that replace function statements.
     */
    private void defineFunctionalTerms(PrintWriter pw) {

        Iterator it = functionTable.keySet().iterator();
        while (it.hasNext()) {
            String functionTerm = (String) it.next();
            String term = (String) functionTable.get(functionTerm);
            Formula f = new Formula();
            f.read(functionTerm);
            String func = f.getArgument(0);
            ArrayList ranges = kb.askWithRestriction(0,"range",1,func);
            String range;
            if (ranges.size() > 0) {
                Formula f2 = (Formula) ranges.get(0);
                range = f2.getArgument(2);
                pw.println("<owl:Thing rdf:about=\"#" + term + "\">");
                pw.println("  <rdf:type rdf:resource=\"" + (range.equals("Entity") ? "&owl;Thing" : "#" + range) + "\"/>");
                pw.println("  <rdfs:comment>A term generated automatically in the " +
                           "translation from SUO-KIF to OWL to replace the functional " +
                           "term " + functionTerm + " that connect be directly " +
                           "expressed in OWL. </rdfs:comment>");
                pw.println("</owl:Thing>");
                pw.println();
            }
            else {
                ArrayList subranges = kb.askWithRestriction(0,"rangeSubclass",1,functionTerm);
                if (subranges.size() > 0) {
                    Formula f2 = (Formula) subranges.get(0);
                    range = f2.getArgument(2);
                    pw.println("<owl:Class rdf:about=\"#" + term + "\">");
                    pw.println("  <rdfs:subClassOf rdf:resource=\"" + (range.equals("Entity") ? "&owl;Thing" : "#" + range) + "\"/>");
                    pw.println("  <rdfs:comment>A term generated automatically in the "+
                               "translation from SUO-KIF to OWL to replace the functional "+
                               "term " + functionTerm + " that connect be directly "+
                                                          "expressed in OWL. </rdfs:comment>");
                    pw.println("</owl:Class>");
                    pw.println();
                }
                else
                    return;
            }
        }
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
        return StringUtil.StringToKIFid(value);
    }

    /** ***************************************************************
     * Read OWL format and write out KIF.
     */
    private static void decode(PrintWriter pw, SimpleElement se, String parentTerm, 
                               String parentTag, String indent) {
        
    	_debugLevelCounter++;
    	//System.out.println("INFO in OWLtranslator().decode(): Debug level: " + _debugLevelCounter);
    	//System.out.println(se);
        String tag = se.getTagName();
        String value = null;
        String existential = null;
        String parens = null;
        //System.out.println(";; " + tag);
        if (tag.equals("owl:Class") || tag.equals("owl:ObjectProperty") || 
            tag.equals("owl:DatatypeProperty") || tag.equals("owl:FunctionalProperty") || 
            tag.equals("owl:InverseFunctionalProperty") || tag.equals("owl:TransitiveProperty") || 
            tag.equals("owl:SymmetricProperty") || tag.equals("rdf:Description")) {
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
                else {
                    // pw.println(";; nodeID? ");
                    parentTerm = se.getAttribute("rdf:nodeID");
                    if (parentTerm != null) {
                        parentTerm = "?nodeID-" + parentTerm;
                        existential = parentTerm;
                    }
                }
            }
            parentTerm = StringUtil.StringToKIFid(parentTerm);
            // pw.println(";; parentTerm" + parentTerm);
            if ((tag.equals("owl:ObjectProperty") || tag.equals("owl:DatatypeProperty") || 
                 tag.equals("owl:InverseFunctionalProperty")) && parentTerm != null) 
                pw.println(indent + "(instance " + parentTerm + " BinaryRelation)");  
            if (tag.equals("owl:TransitiveProperty") && parentTerm != null) 
                pw.println(indent + "(instance " + parentTerm + " TransitiveRelation)");              
            if (tag.equals("owl:FunctionalProperty") && parentTerm != null) 
                pw.println(indent + "(instance " + parentTerm + " SingleValuedRelation)");              
            if (tag.equals("owl:SymmetricProperty") && parentTerm != null) 
                pw.println(indent + "(instance " + parentTerm + " SymmetricRelation)");              
        }
        else if (tag.equals("rdfs:domain")) {
            value = se.getAttribute("rdf:resource");
            if (value != null) {                
                if (value.indexOf("#") > -1) 
                    value = value.substring(value.indexOf("#") + 1);
                value = StringUtil.StringToKIFid(value);
                if (value != null && parentTerm != null) 
                    pw.println(indent + "(domain " + parentTerm + " 1 " + value + ")");
            }
        }
        else if (tag.equals("rdfs:range")) {
            value = se.getAttribute("rdf:resource");
            if (value != null) {                
                if (value.indexOf("#") > -1) 
                    value = value.substring(value.indexOf("#") + 1);
                value = StringUtil.StringToKIFid(value);
                if (value != null && parentTerm != null) 
                    pw.println(indent + "(domain " + parentTerm + " 2 " + value + ")");
            }
        }
        else if (tag.equals("rdfs:comment")) {
            String text = se.getText();
            text = processStringForKIFOutput(text);
            if (parentTerm != null && text != null) 
                pw.println(DB.wordWrap(indent + "(documentation " + parentTerm + " EnglishLanguage \"" + 
                                       text + "\")",70));
        }
        else if (tag.equals("rdfs:label")) {
            String text = se.getText();
            text = processStringForKIFOutput(text);
            if (parentTerm != null && text != null) 
                pw.println(DB.wordWrap(indent + "(termFormat EnglishLanguage " + parentTerm + " \"" + 
                                       text + "\")",70));
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
            value = StringUtil.StringToKIFid(value);
            if (value != null && parentTerm != null) 
                pw.println(indent + "(inverse " + parentTerm + " " + value + ")");
        }
        else if (tag.equals("rdfs:subClassOf")) {
            value = getParentReference(se);
            value = StringUtil.StringToKIFid(value);
            if (value != null) 
                pw.println(indent + "(subclass " + parentTerm + " " + value + ")");           
            else
                pw.println(";; missing or unparsed subclass statment for " + parentTerm);
        }
        else if (tag.equals("owl:Restriction")) { }
        else if (tag.equals("owl:onProperty")) { }
        else if (tag.equals("owl:unionOf")) { _debugLevelCounter--; return; }
        else if (tag.equals("owl:complimentOf")) { _debugLevelCounter--; return; }
        else if (tag.equals("owl:intersectionOf")) { _debugLevelCounter--; return; }
        else if (tag.equals("owl:cardinality")) { }
        else if (tag.equals("owl:FunctionalProperty")) {
            value = se.getAttribute("rdf:about");
            if (value != null) {                
                if (value.indexOf("#") > -1) 
                    value = value.substring(value.indexOf("#") + 1);
                value = StringUtil.StringToKIFid(value);
                pw.println(indent + "(instance " + value + " SingleValuedRelation)");
            }
        }
        else if (tag.equals("owl:minCardinality")) { }
        else if (tag.equals("owl:maxCardinality")) { }
        else if (tag.equals("rdf:type")) {
            value = getParentReference(se);
            value = StringUtil.StringToKIFid(value);
            if (value != null) 
                pw.println(indent + "(instance " + parentTerm + " " + value + ")"); 
            else
                pw.println(";; missing or unparsed subclass statment for " + parentTerm);
        }
        else {
            value = se.getAttribute("rdf:resource");
            if (value != null) {                
                if (value.indexOf("#") > -1) 
                    value = value.substring(value.indexOf("#") + 1);
                value = StringUtil.StringToKIFid(value);
                tag = StringUtil.StringToKIFid(tag);
                if (value != null && parentTerm != null) 
                    pw.println(indent + "(" + tag + " " + parentTerm + " " + value + ")");
            }
            else {
                String text = se.getText();
                String datatype = se.getAttribute("rdf:datatype");
                text = processStringForKIFOutput(text);
                if (datatype == null || 
                    (!datatype.endsWith("integer") && !datatype.endsWith("decimal"))) 
                    text = "\"" + text + "\"";
                tag = StringUtil.StringToKIFid(tag);
                if (!DB.emptyString(text) && !text.equals("\"\"")) {
                    if (parentTerm != null && tag != null && text != null)  
                        pw.println(indent + "(" + tag + " " + parentTerm + " " + text + ")"); 
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
                                pw.println(indent + "(" + tag + " " + parentTerm + " " + value + ")");
                        }
                    }
                }
            }
        }
        if (existential != null) {
            pw.println("(exists (" + existential + ") ");
            if (se.getChildElements().size() > 1) {
                pw.println("  (and ");
                indent = indent + "    ";
                parens = "))";
            }
            else {
                indent = indent + "  ";
                parens = ")";
            }
        }
        //System.out.println("INFO in OWLtranslator().decode(): reading attributes");
        Set s = se.getAttributeNames();
        Iterator it = s.iterator();
        while (it.hasNext()) {
            String att = (String) it.next();
            String val = (String) se.getAttribute(att);
        }
        //System.out.println("INFO in OWLtranslator().decode(): reading children");
        ArrayList al = se.getChildElements();
        it = al.iterator();
        while (it.hasNext()) {
            SimpleElement child = (SimpleElement) it.next();
            decode(pw,child,parentTerm,tag,indent);
        }
        if (existential != null) {
            existential = null;
            pw.println (parens);
            parens = null;
        }
        _debugLevelCounter--;
        //System.out.println("INFO in OWLtranslator().decode(): exiting decode with level: " + _debugLevelCounter);
    }

    /** ***************************************************************
     * Read OWL format.
     */
    public static void read(String filename) throws IOException {

        FileWriter fw = null;
        PrintWriter pw = null;
        _debugLevelCounter = 0;
        try {
            SimpleElement se = SimpleDOMParser.readFile(filename);
            System.out.println("INFO in WLtranslator.read(): input filename: " + filename);
            fw = new FileWriter(filename + ".kif");
            pw = new PrintWriter(fw);
            System.out.println("INFO in WLtranslator.read(): output filename: " + filename + ".kif");
            decode(pw,se,"","","");
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

    /** ***************************************************************
     * Remove special characters in documentation.
     */
    private static String processDoc(String doc) {

        String result = doc;
        result = result.replaceAll("&%","");
        result = result.replaceAll("&","&#38;");
        result = result.replaceAll(">","&gt;");
        result = result.replaceAll("<","&lt;");
        result = removeQuotes(result);
        return result;
    }

    /** ***************************************************************
     */
    private void writeTermFormat(PrintWriter pw, String term) {

        ArrayList al = kb.askWithRestriction(0,"termFormat",2,term);
        if (al.size() > 0) {
            for (int i = 0; i < al.size(); i++) {
                Formula form = (Formula) al.get(i);
                String lang = form.getArgument(1);
                if (lang.equals("EnglishLanguage")) 
                    lang = "en";
                String st = form.getArgument(3);
                st = removeQuotes(st);
                pw.println("  <rdfs:label xml:lang=\"" + lang + "\">" + st + "</rdfs:label>");
            }
        }
    }

    /** ***************************************************************
     */
    private void writeSynonymous(PrintWriter pw, String term, String termType) {

        ArrayList syn = kb.askWithRestriction(0,"synonymousExternalConcept",2,term);
        if (syn.size() > 0) {
            for (int i = 0; i < syn.size(); i++) {
                Formula form = (Formula) syn.get(i);
                String st = form.getArgument(1);
                st = StringUtil.StringToKIFid(st);
                String lang = form.getArgument(3);
                if (termType.equals("relation")) 
                    pw.println("  <owl:equivalentProperty rdf:resource=\"" + (lang.equals("Entity") ? "&owl;Thing" : "#" + lang) + ":" + st + "\" />");
                else if (termType.equals("instance")) 
                    pw.println("  <owl:sameAs rdf:resource=\"" + (lang.equals("Entity") ? "&owl;Thing" : "#" + lang) + ":" + st + "\" />");
                else if (termType.equals("class")) 
                    pw.println("  <owl:equivalentClass rdf:resource=\"" + (lang.equals("Entity") ? "&owl;Thing" : "#" + lang) + ":" + st + "\" />");                
            }
        }
    }

    /** ***************************************************************
     */
    private void writeAxiomLinks(PrintWriter pw, String term) {

        ArrayList al = kb.ask("ant",0,term);
        for (int i = 0; i < al.size(); i++) {
            Formula f = (Formula) al.get(i);
            String st = f.createID();
            pw.println("  <kbd:axiom rdf:resource=\"#axiom-" + st + "\"/>");
        }
        al = kb.ask("cons",0,term);
        for (int i = 0; i < al.size(); i++) {
            Formula f = (Formula) al.get(i);
            String st = f.createID();
            pw.println("  <kbd:axiom rdf:resource=\"#axiom-" + st + "\"/>");
        }
        //pw.println("  <fullDefinition rdf:datatype=\"&xsd;anyURI\">" + 
        //           "http://sigma.ontologyportal.org:4010/sigma/Browse.jsp?lang=EnglishLanguage&kb=SUMO&term=" +
        //           term + "</fullDefinition>");
    }

    /** ***************************************************************
     */
    private void writeWordNetLink(PrintWriter pw, String term) {

        WordNet.wn.initOnce();
          // get list of synsets with part of speech prepended to the synset number.
        ArrayList al = (ArrayList) WordNet.wn.SUMOHash.get(term);  
        if (al != null) {
            for (int i = 0; i < al.size(); i++) {
                String synset = (String) al.get(i);
                String termMapping = null;
                  // GetSUMO terms with the &% prefix and =, +, @ or [ suffix.   
                switch (synset.charAt(0)) {
                  case '1': termMapping = (String) WordNet.wn.nounSUMOHash.get(synset.substring(1)); break;
                  case '2': termMapping = (String) WordNet.wn.verbSUMOHash.get(synset.substring(1)); break;
                  case '3': termMapping = (String) WordNet.wn.adjectiveSUMOHash.get(synset.substring(1)); break;
                  case '4': termMapping = (String) WordNet.wn.adverbSUMOHash.get(synset.substring(1)); break;
                }
                String rel = null;
                if (termMapping != null) {
                    switch (termMapping.charAt(termMapping.length()-1)) {
                      case '=': rel = "equivalenceRelation"; break;
                      case '+': rel = "subsumingRelation"; break;
                      case '@': rel = "instanceRelation"; break;
                      case ':': rel = "antiEquivalenceRelation"; break;
                      case '[': rel = "antiSubsumingRelation"; break;
                      case ']': rel = "antiInstanceRelation"; break;
                    }
                }
                pw.println("  <wnd:" + rel + " rdf:resource=\"&wnd;WN30-" + synset + "\"/>");
            }
        }
    }

    /** ***************************************************************
     */
    private void createAxiomMap() {

        axiomMap = new TreeMap<String,Formula>();
        Iterator it = kb.formulaMap.values().iterator();
        while (it.hasNext()) {
            Formula f = (Formula) it.next();
            if (f.isRule()) {
                axiomMap.put("axiom-" + f.createID(),f);            
                //System.out.println("INFO in OWLtranslator.createAxiomMap(): key: " + "axiom-" + f.createID());
            }
        }
    }

    /** ***************************************************************
     */
    private void writeAxioms(PrintWriter pw) {

        TreeSet ts = new TreeSet();
        ts.addAll(kb.formulaMap.values());
        Iterator tsit = ts.iterator();
        while (tsit.hasNext()) {
            Formula f = (Formula) tsit.next();
            if (f.isRule()) {
                String form = f.toString();
                form = form.replaceAll("<=>","iff");
                form = form.replaceAll("=>","implies");
                form = processDoc(form);
                pw.println("<owl:Thing rdf:about=\"#axiom-" + f.createID() + "\">");
                pw.println("  <rdfs:comment xml:lang=\"en\">A SUO-KIF axiom that may not be directly expressible in OWL. " +
                           "See www.ontologyportal.org for the original SUO-KIF source.\n " + 
                           form + "</rdfs:comment>");
                pw.println("</owl:Thing>");
            }
        }
    }

    /** ***************************************************************
     */
    private void writeOneAxiom(PrintWriter pw, String id) {

        //System.out.println("INFO in OWLtranslator.writeOneAxiom(): write axiom ID: " + id);        
        Formula f = (Formula) axiomMap.get(id);
        if (f != null && f.isRule()) {
            String form = f.toString();
            form = form.replaceAll("<=>","iff");
            form = form.replaceAll("=>","implies");
            form = processDoc(form);
            pw.println("<owl:Thing rdf:about=\"#" + id + "\">");
            pw.println("  <rdfs:comment xml:lang=\"en\">A SUO-KIF axiom that may not be directly expressible in OWL. " +
                       "See www.ontologyportal.org for the original SUO-KIF source.\n " + 
                       form + "</rdfs:comment>");
            pw.println("</owl:Thing>");
        }        
        else 
            System.out.println("Error in OWLtranslator.writeOneAxiom(): null or non-axiom for ID: " + id);        
    }

    /** ***************************************************************
     */
    private void writeDocumentation(PrintWriter pw, String term) {

        ArrayList doc = kb.askWithRestriction(0,"documentation",1,term);    // Class expressions for term.
        if (doc.size() > 0) {
            for (int i = 0; i < doc.size(); i++) {
                Formula form = (Formula) doc.get(i);
                String lang = form.getArgument(2);
                String documentation = form.getArgument(3);
                String langString = "";
                if (lang.equals("EnglishLanguage")) 
                    langString = " xml:lang=\"en\"";
                if (documentation != null) 
                    pw.println("  <rdfs:comment" + langString + ">" + 
                               StringUtil.wordWrap(processDoc(documentation)) + "</rdfs:comment>");
            }
        }
    }

    /** ***************************************************************
     */
    private void writeYAGOMapping(PrintWriter pw, String term) {

        //System.out.println("INFO in OWLtranslator.writeYAGOMapping(): writing term: " + term);
        String YAGO = (String) SUMOYAGOMap.get(term);
        if (YAGO != null) {        
            pw.println("  <owl:sameAs rdf:resource=\"http://dbpedia.org/resource/" + YAGO + "\" />");
            pw.println("  <owl:sameAs rdf:resource=\"http://yago-knowledge.org/resource/" + YAGO + "\" />");
            pw.println("  <rdfs:seeAlso rdf:resource=\"http://en.wikipedia.org/wiki/" + YAGO + "\" />");
        }                    
    }

    /** ***************************************************************
     * Write OWL format.
     */
    private void writeRelations(PrintWriter pw, String term) {

        String propType = "ObjectProperty";
        if (kb.childOf(term,"SymmetricRelation"))         
            propType = "SymmetricProperty";
        else if (kb.childOf(term,"TransitiveRelation"))
            propType = "TransitiveProperty";
        else if (kb.childOf(term,"Function"))
            propType = "FunctionalProperty";
        pw.println("<owl:"+ propType + " rdf:about=\"#" + term + "\">");
        ArrayList argTypes = kb.askWithRestriction(0,"domain",1,term);  // domain expressions for term.
        ArrayList subs = kb.askWithRestriction(0,"subrelation",1,term);  // subrelation expressions for term.
        if (argTypes.size() > 0) {
            for (int i = 0; i < argTypes.size(); i++) {
                Formula form = (Formula) argTypes.get(i);
                String arg = form.getArgument(2);
                String argType = form.getArgument(3);
                if (arg.equals("1") && Formula.atom(argType)) 
                    pw.println("  <rdfs:domain rdf:resource=\"" + (argType.equals("Entity") ? "&owl;Thing" : "#" + argType) + "\" />");
                if (arg.equals("2") && Formula.atom(argType)) 
                    pw.println("  <rdfs:range rdf:resource=\"" + (argType.equals("Entity") ? "&owl;Thing" : "#" + argType) + "\" />");
            }
        }
        ArrayList ranges = kb.askWithRestriction(0,"range",1,term);  // domain expressions for term.
        if (ranges.size() > 0) {
            Formula form = (Formula) ranges.get(0);
            String argType = form.getArgument(2);
            if (Formula.atom(argType))                 
                pw.println("  <rdfs:range rdf:resource=\"" + (argType.equals("Entity") ? "&owl;Thing" : "#" + argType) + "\" />");
        }
        ArrayList inverses = kb.askWithRestriction(0,"inverse",1,term);  // inverse expressions for term.
        if (inverses.size() > 0) {
            Formula form = (Formula) inverses.get(0);
            String arg = form.getArgument(2);
            if (Formula.atom(arg))                 
                pw.println("  <owl:inverseOf rdf:resource=\"" + (arg.equals("Entity") ? "&owl;Thing" : "#" + arg) + "\" />");
        }
        if (subs.size() > 0) {
            for (int i = 0; i < subs.size(); i++) {
                Formula form = (Formula) subs.get(i);
                String superProp = form.getArgument(2);
                pw.println("  <owl:subPropertyOf rdf:resource=\"" + (superProp.equals("Entity") ? "&owl;Thing" : "#" + superProp) + "\" />");
            }
        }
        writeDocumentation(pw,term);
        writeSynonymous(pw,term,"relation");
        writeYAGOMapping(pw,term);
        writeTermFormat(pw,term);
        writeAxiomLinks(pw,term);
        writeWordNetLink(pw,term);
        pw.println("</owl:" + propType + ">");
        pw.println();
    }

    /** ***************************************************************
     */
    private void writeInstances(PrintWriter pw, String term, ArrayList instances) {

        pw.println("<owl:Thing rdf:about=\"#" + term + "\">");
        pw.println("  <rdfs:isDefinedBy rdf:resource=\"http://www.ontologyportal.org/SUMO.owl\"/>");
        for (int i = 0; i < instances.size(); i++) {
            Formula form = (Formula) instances.get(i);
            String parent = form.getArgument(2);
            if (Formula.atom(parent)) 
                pw.println("  <rdf:type rdf:resource=\"" + (parent.equals("Entity") ? "&owl;Thing" : "#" + parent) + "\"/>");
        }
        writeDocumentation(pw,term);
        ArrayList statements = kb.ask("arg",1,term); 
        for (int i = 0; i < statements.size(); i++) {
            Formula form = (Formula) statements.get(i);
            String rel = form.getArgument(0);
            if (!rel.equals("instance") && !rel.equals("subclass") && 
               !rel.equals("documentation") && 
               !rel.equals("subrelation") && kb.childOf(rel,"BinaryRelation")) { 
                String range = form.getArgument(2);
                if (range == null || range == "" ) {
                    System.out.println("Error in OWLtranslator.writeInstance(): missing range in statement: " + form);
                    continue;
                }
                if (Formula.listP(range)) 
                    range = instantiateFunction(range);
                if (range.charAt(0) == '"' && range.charAt(range.length()-1) == '"') {
                    range = removeQuotes(range);
                    if (range.startsWith("http://")) 
                        pw.println("  <" + rel + " rdf:datatype=\"&xsd;anyURI\">" + 
                                   range + "</" + rel + ">");
                    else
                        pw.println("  <" + rel + " rdf:datatype=\"&xsd;string\">" + 
                                   range + "</" + rel + ">");
                }
                else if (((range.charAt(0) == '-' && Character.isDigit(range.charAt(1))) ||
                         (Character.isDigit(range.charAt(0)))) && range.indexOf(".") < 0)
                    pw.println("  <" + rel + " rdf:datatype=\"&xsd;integer\">" + 
                               range + "</" + rel + ">");                  
                else
                    pw.println("  <" + rel + " rdf:resource=\"" + (range.equals("Entity") ? "&owl;Thing" : "#" + range) + "\" />");
            }
        }
        
        writeSynonymous(pw,term,"instance");
        writeTermFormat(pw,term);
        writeAxiomLinks(pw,term);
        writeYAGOMapping(pw,term);
        writeWordNetLink(pw,term);
        pw.println("</owl:Thing>");
        pw.println();
    }

    /** ***************************************************************
     */
    private void writeClasses(PrintWriter pw, String term, ArrayList classes, 
                              boolean isInstance) {

        if (isInstance)         
            pw.println("<owl:Class rdf:about=\"#" + term + "\">");
        else
            pw.println("<owl:Class rdf:about=\"#" + term + "\">");
        pw.println("  <rdfs:isDefinedBy rdf:resource=\"http://www.ontologyportal.org/SUMO.owl\"/>");
        for (int i = 0; i < classes.size(); i++) {
            Formula form = (Formula) classes.get(i);
            String parent = form.getArgument(2);
            if (Formula.atom(parent)) 
                pw.println("  <rdfs:subClassOf rdf:resource=\"" + (parent.equals("Entity") ? "&owl;Thing" : "#" + parent) + "\"/>");
        }
        writeDocumentation(pw,term);
        ArrayList statements = kb.ask("arg",1,term); 
        for (int i = 0; i < statements.size(); i++) {
             Formula form = (Formula) statements.get(i);
             String rel = form.getArgument(0);
             if (!rel.equals("instance") && !rel.equals("subclass") && 
                !rel.equals("documentation") &&
                !rel.equals("subrelation") && kb.childOf(rel,"BinaryRelation")) { 
                 String range = form.getArgument(2);
                 if (Formula.listP(range)) 
                     range = instantiateFunction(range);
                 if (rel.equals("disjoint")) 
                     pw.println("  <owl:disjointWith rdf:resource=\"" + (range.equals("Entity") ? "&owl;Thing" : "#" + range) + "\" />");
                 else if (rel.equals("synonymousExternalConcept")) {                     
                     // since argument order is reversed between OWL and SUMO, this must be handled below
                 }
                 else if (range.charAt(0) == '"' && range.charAt(range.length()-1) == '"') {
                     range = removeQuotes(range);
                     if (range.startsWith("http://")) 
                         pw.println("  <" + rel + " rdf:datatype=\"&xsd;anyURI\">" + 
                                    range + "</" + rel + ">");
                     else
                         pw.println("  <" + rel + " rdf:datatype=\"&xsd;string\">" + 
                                    range + "</" + rel + ">");
                 }
                 else if (((range.charAt(0) == '-' && Character.isDigit(range.charAt(1))) ||
                          (Character.isDigit(range.charAt(0)))) && range.indexOf(".") < 0)
                     pw.println("  <" + rel + " rdf:datatype=\"&xsd;integer\">" + 
                                range + "</" + rel + ">");                  
                 else
                     pw.println("  <" + rel + " rdf:resource=\"" + (range.equals("Entity") ? "&owl;Thing" : "#" + range) + "\" />");
             }
        }
        ArrayList syn = kb.askWithRestriction(0,"synonymousExternalConcept",2,term);
        if (syn.size() > 0) {
            for (int i = 0; i < syn.size(); i++) {
                Formula form = (Formula) syn.get(i);
                String st = form.getArgument(1);
                st = StringUtil.StringToKIFid(st);
                String lang = form.getArgument(3);
                pw.println("  <owl:equivalentClass rdf:resource=\"" + (lang.equals("Entity") ? "&owl;Thing" : "#" + lang) + ":" + st + "\" />");
            }
        }
        writeSynonymous(pw,term,"class");
        writeTermFormat(pw,term);
        writeYAGOMapping(pw,term);
        writeAxiomLinks(pw,term);
        writeWordNetLink(pw,term);
        pw.println("</owl:Class>");
        pw.println();
    }

    /** ***************************************************************
     * Read a mapping file from YAGO to SUMO terms and store in SUMOYAGOMap
     */
    private void readYAGOSUMOMappings() {

        FileReader r = null;
        LineNumberReader lr = null;
        try {
            System.out.println("INFO in OWLtranslator.readYAGOSUMOMappings()");
            String kbDir = KBmanager.getMgr().getPref("kbDir");
            File f = new File(kbDir + File.separator + "yago-sumo-mappings.txt");
            if (!f.canRead()) {
                System.out.println( "INFO in readYAGOSUMOMappings(): " 
                                    + "The mappings file " +
                                    kbDir + File.separator + "yago-sumo-mappings.txt does not exist" );
                return;
            }
            r = new FileReader(f);
            lr = new LineNumberReader(r);
            String line = null;
            String YAGO = null;
            String SUMO = null;
            while ((line = lr.readLine()) != null) {
                line = line.trim();
                if (StringUtil.isNonEmptyString(line) && line.charAt(0) != '#') {
                    YAGO = line.substring(0,line.indexOf(" "));
                    SUMO = line.substring(line.indexOf(" ")+1);
                    SUMOYAGOMap.put(SUMO,YAGO);
                }
            }
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
        finally {
            try {
                if (lr != null) lr.close();
            }
            catch (Exception lre) {
                System.out.println("Exception in OWLtranslator.readYAGOSUMOMappings()");
                System.out.println(lre);
            }
            try {
                if (r != null) r.close();
            }
            catch (Exception re) {
                System.out.println("Exception in OWLtranslator.readYAGOSUMOMappings()");
                System.out.println(re);
            }
        }
        return;
    }

    /** ***************************************************************
     * Write OWL file header.
     */
    private void writeKBHeader(PrintWriter pw) {

    	pw.println("<!DOCTYPE rdf:RDF [");
    	pw.println("   <!ENTITY wnd \"http://www.ontologyportal.org/WNDefs.owl#\">");
        pw.println("   <!ENTITY kbd \"http://www.ontologyportal.org/KBDefs.owl#\">");
        pw.println("   <!ENTITY xsd \"http://www.w3.org/2001/XMLSchema#\">");
    	pw.println("   <!ENTITY rdf \"http://www.w3.org/1999/02/22-rdf-syntax-ns#\">");
    	pw.println("   <!ENTITY rdfs \"http://www.w3.org/2000/01/rdf-schema#\">");
    	pw.println("   <!ENTITY owl \"http://www.w3.org/2002/07/owl#\">");
    	pw.println("]>");
        pw.println("<rdf:RDF");
        pw.println("xmlns=\"http://www.ontologyportal.org/SUMO.owl#\"");
        pw.println("xml:base=\"http://www.ontologyportal.org/SUMO.owl\"");
        pw.println("xmlns:wnd=\"http://www.ontologyportal.org/WNDefs.owl#\"");
        pw.println("xmlns:kbd=\"http://www.ontologyportal.org/KBDefs.owl#\"");
        pw.println("xmlns:xsd=\"http://www.w3.org/2001/XMLSchema#\"");
        pw.println("xmlns:rdf =\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\"");
        pw.println("xmlns:rdfs=\"http://www.w3.org/2000/01/rdf-schema#\"");
        pw.println("xmlns:owl =\"http://www.w3.org/2002/07/owl#\">");
        pw.println("<owl:Ontology rdf:about=\"http://www.ontologyportal.org/SUMO.owl\">");
        pw.println("<rdfs:comment xml:lang=\"en\">A provisional and necessarily lossy translation to OWL.  Please see");
        pw.println("www.ontologyportal.org for the original KIF, which is the authoritative");
        pw.println("source.  This software is released under the GNU Public License"); 
        pw.println("www.gnu.org.</rdfs:comment>");
        Date d = new Date();
        pw.println("<rdfs:comment xml:lang=\"en\">Produced on date: " + d.toString() + "</rdfs:comment>");
        pw.println("</owl:Ontology>");
    }

    /** ***************************************************************
     * Write OWL format.
     */
     public void writeSUMOTerm(PrintWriter pw, String term) {

         if (kb.childOf(term,"BinaryRelation") && kb.isInstance(term)) 
             writeRelations(pw,term);                
         if (Character.isUpperCase(term.charAt(0))) {
             ArrayList instances = kb.askWithRestriction(0,"instance",1,term);  // Instance expressions for term.
             ArrayList classes = kb.askWithRestriction(0,"subclass",1,term);    // Class expressions for term.
             String documentation = null;
             Formula form;
             if (instances.size() > 0 && !kb.childOf(term,"BinaryRelation"))
                 writeInstances(pw,term,instances);   
             boolean isInstance = false;
             if (classes.size() > 0) {
                 if (instances.size() > 0) 
                     isInstance = true;
                 writeClasses(pw,term,classes,isInstance); 
                 isInstance = false;
             }
         }
     }

    /** ***************************************************************
     * Write OWL format.
     */
     public void writeKB() throws IOException {
         writeKB("");
     }

    /** ***************************************************************
     * Write OWL format.
     */
     public void writeKB(String path) throws IOException {

         System.out.println("INFO in OWLtranslator.write(): writing " + path);
         readYAGOSUMOMappings();
         FileWriter fw = new FileWriter(path);
         PrintWriter pw = new PrintWriter(fw);
         writeKBHeader(pw);
         Set<String> kbterms = kb.getTerms();
         synchronized (kbterms) {
             for (String term : kbterms) { 
                 writeSUMOTerm(pw,term);
             	 pw.flush();
             }
         }
         defineFunctionalTerms(pw);
         writeAxioms(pw);
         pw.println("</rdf:RDF>");
         pw.close();
    }

    /** ***************************************************************
     */
    private void writeWordNetClassDefinitions(PrintWriter pw) throws IOException {

        ArrayList WordNetClasses = 
            new ArrayList(Arrays.asList("Synset","NounSynset","VerbSynset","AdjectiveSynset","AdverbSynset"));
        Iterator it = WordNetClasses.iterator();
        while (it.hasNext()) {
            String term = (String) it.next();
            pw.println("<owl:Class rdf:about=\"#" + term + "\">");
            pw.println("  <rdfs:label xml:lang=\"en\">" + term + "</rdfs:label>");
            if (!term.equals("Synset")) {
                pw.println("  <rdfs:subClassOf rdf:resource=\"#Synset\"/>");   
                String POS = term.substring(0,term.indexOf("Synset"));
                pw.println("  <rdfs:comment xml:lang=\"en\">A group of " + POS + 
                           "s having the same meaning.</rdfs:comment>");
            }
            else {
                pw.println("  <rdfs:comment xml:lang=\"en\">A group of words having the same meaning.</rdfs:comment>");
            }
            pw.println("</owl:Class>");
        }
        pw.println("<owl:Class rdf:about=\"#WordSense\">");
        pw.println("  <rdfs:label xml:lang=\"en\">word sense</rdfs:label>");
        pw.println("  <rdfs:comment xml:lang=\"en\">A particular sense of a word.</rdfs:comment>");
        pw.println("</owl:Class>");
        pw.println("<owl:Class rdf:about=\"#Word\">");
        pw.println("  <rdfs:label xml:lang=\"en\">word</rdfs:label>");
        pw.println("  <rdfs:comment xml:lang=\"en\">A particular word.</rdfs:comment>");
        pw.println("</owl:Class>");
        pw.println("<owl:Class rdf:about=\"#VerbFrame\">");
        pw.println("  <rdfs:label xml:lang=\"en\">verb frame</rdfs:label>");
        pw.println("  <rdfs:comment xml:lang=\"en\">A string template showing allowed form of use of a verb.</rdfs:comment>");
        pw.println("</owl:Class>");
    }

    /** ***************************************************************
     */
    private void writeVerbFrames(PrintWriter pw) throws IOException {

        ArrayList VerbFrames = new ArrayList(Arrays.asList("Something ----s",
          "Somebody ----s",
          "It is ----ing",
          "Something is ----ing PP",
          "Something ----s something Adjective/Noun",
          "Something ----s Adjective/Noun",
          "Somebody ----s Adjective",
          "Somebody ----s something",
          "Somebody ----s somebody",
          "Something ----s somebody",
          "Something ----s something",
          "Something ----s to somebody",
          "Somebody ----s on something",
          "Somebody ----s somebody something",
          "Somebody ----s something to somebody",
          "Somebody ----s something from somebody",
          "Somebody ----s somebody with something",
          "Somebody ----s somebody of something",
          "Somebody ----s something on somebody",
          "Somebody ----s somebody PP",
          "Somebody ----s something PP",
          "Somebody ----s PP",
          "Somebody's (body part) ----s",
          "Somebody ----s somebody to INFINITIVE",
          "Somebody ----s somebody INFINITIVE",
          "Somebody ----s that CLAUSE",
          "Somebody ----s to somebody",
          "Somebody ----s to INFINITIVE",
          "Somebody ----s whether INFINITIVE",
          "Somebody ----s somebody into V-ing something",
          "Somebody ----s something with something",
          "Somebody ----s INFINITIVE",
          "Somebody ----s VERB-ing",
          "It ----s that CLAUSE",
          "Something ----s INFINITIVE"));
        for (int i = 0; i < VerbFrames.size(); i ++) {
            String frame = (String) VerbFrames.get(i);
            String numString = String.valueOf(i);
            if (numString.length() == 1) 
                numString = "0" + numString;
            pw.println("<owl:Thing rdf:about=\"#WN30VerbFrame-" + numString + "\">");
            pw.println("  <rdfs:comment xml:lang=\"en\">" + frame + "</rdfs:comment>");
            pw.println("  <rdfs:label xml:lang=\"en\">" + frame + "</rdfs:label>");
            pw.println("  <rdf:type rdf:resource=\"#VerbFrame\"/>");
            pw.println("</owl:Thing>");
        }
    }

    /** ***************************************************************
     */
    private void writeWordNetRelationDefinitions(PrintWriter pw) throws IOException {

        ArrayList WordNetRelations = new ArrayList(Arrays.asList("antonym",
          "hypernym", "instance-hypernym", "hyponym", "instance-hyponym", 
          "member-holonym", "substance-holonym", "part-holonym", "member-meronym", 
          "substance-meronym", "part-meronym", "attribute", "derivationally-related", 
          "domain-topic", "member-topic", "domain-region", "member-region", 
          "domain-usage", "member-usage", "entailment", "cause", "also-see", 
          "verb-group", "similar-to", "participle", "pertainym"));
        Iterator it = WordNetRelations.iterator();
        while (it.hasNext()) {
            String rel = (String) it.next();
            String tag = null;
            if (rel.equals("antonym") || rel.equals("similar-to") ||
                rel.equals("verb-group") || rel.equals("derivationally-related")) 
                tag = "owl:SymmetricProperty";
            else 
                tag = "owl:ObjectProperty";
            pw.println("<" + tag+ " rdf:about=\"#" + rel + "\">");
            pw.println("  <rdfs:label xml:lang=\"en\">" + rel + "</rdfs:label>");
            pw.println("  <rdfs:domain rdf:resource=\"#Synset\" />");
            pw.println("  <rdfs:range rdf:resource=\"#Synset\" />");
            pw.println("</" + tag + ">");
        }
        pw.println("<owl:ObjectProperty rdf:about=\"#word\">");
        pw.println("  <rdfs:domain rdf:resource=\"#Synset\" />");
        pw.println("  <rdfs:range rdf:resource=\"rdfs:Literal\" />");
        pw.println("  <rdfs:label xml:lang=\"en\">word</rdfs:label>");
        pw.println("  <rdfs:comment xml:lang=\"en\">A relation between a WordNet synset and a word\n" +
                   "which is a member of the synset.</rdfs:comment>");
        pw.println("</owl:ObjectProperty>");
        pw.println("<owl:ObjectProperty rdf:about=\"#singular\">");
        pw.println("  <rdfs:domain rdf:resource=\"#Word\" />");
        pw.println("  <rdfs:range rdf:resource=\"rdfs:Literal\" />");
        pw.println("  <rdfs:label xml:lang=\"en\">singular</rdfs:label>");
        pw.println("  <rdfs:comment xml:lang=\"en\">A relation between a WordNet synset and a word\n" +
                   "which is a member of the synset.</rdfs:comment>");
        pw.println("</owl:ObjectProperty>");
        pw.println("<owl:ObjectProperty rdf:about=\"#infinitive\">");
        pw.println("  <rdfs:domain rdf:resource=\"#Word\" />");
        pw.println("  <rdfs:range rdf:resource=\"rdfs:Literal\" />");
        pw.println("  <rdfs:label xml:lang=\"en\">infinitive</rdfs:label>");
        pw.println("  <rdfs:comment xml:lang=\"en\">A relation between a word\n" +
                   " in its past tense and infinitive form.</rdfs:comment>");
        pw.println("</owl:ObjectProperty>");
        pw.println("<owl:ObjectProperty rdf:about=\"#senseKey\">");
        pw.println("  <rdfs:domain rdf:resource=\"#Word\" />");
        pw.println("  <rdfs:range rdf:resource=\"#WordSense\" />");
        pw.println("  <rdfs:label xml:lang=\"en\">sense key</rdfs:label>");
        pw.println("  <rdfs:comment xml:lang=\"en\">A relation between a word\n" +
                   "and a particular sense of the word.</rdfs:comment>");
        pw.println("</owl:ObjectProperty>");
        pw.println("<owl:ObjectProperty rdf:about=\"#synset\">");
        pw.println("  <rdfs:domain rdf:resource=\"#WordSense\" />");
        pw.println("  <rdfs:range rdf:resource=\"#Synset\" />");
        pw.println("  <rdfs:label xml:lang=\"en\">synset</rdfs:label>");
        pw.println("  <rdfs:comment xml:lang=\"en\">A relation between a sense of a particular word\n" +
                   "and the synset in which it appears.</rdfs:comment>");
        pw.println("</owl:ObjectProperty>");
        pw.println("<owl:ObjectProperty rdf:about=\"#verbFrame\">");
        pw.println("  <rdfs:domain rdf:resource=\"#WordSense\" />");
        pw.println("  <rdfs:range rdf:resource=\"#VerbFrame\" />");
        pw.println("  <rdfs:label xml:lang=\"en\">verb frame</rdfs:label>");
        pw.println("  <rdfs:comment xml:lang=\"en\">A relation between a verb word sense and a template that\n"+
                   "describes the use of the verb in a sentence.</rdfs:comment>");
        pw.println("</owl:ObjectProperty>");
    }

    /** ***************************************************************
     * Write OWL format for SUMO-WordNet mappings.
     * @param synset is a POS prefixed synset number
     */
    private void writeWordNetSynset(PrintWriter pw, String synset) {

    	System.out.println("INFO in OWLtranslator.writeWordNetSynset(): " + synset);
        if (synset.startsWith("WN30-")) 
            synset = synset.substring(5);
        ArrayList al = (ArrayList) WordNet.wn.synsetsToWords.get(synset);
        if (al != null) {
            pw.println("<owl:Thing rdf:about=\"#WN30-" + synset + "\">");
            String parent = "Noun";
            switch (synset.charAt(0)) {
              case '1': parent = "NounSynset"; break;
              case '2': parent = "VerbSynset"; break;
              case '3': parent = "AdjectiveSynset"; break;
              case '4': parent = "AdverbSynset"; break;
            }
            pw.println("  <rdf:type rdf:resource=\"" + (parent.equals("Entity") ? "&owl;Thing" : "&wnd;" + parent) + "\"/>");
            if (al.size() > 0) 
                pw.println("  <rdfs:label>" + ((String) al.get(0)) + "</rdfs:label>");
            for (int i = 0; i < al.size(); i++) {
                String word = (String) al.get(i);
                String wordAsID = StringUtil.StringToKIFid(word);
                pw.println("  <wnd:word rdf:resource=\"#WN30Word-" + wordAsID + "\"/>");
            }
            String doc = null;
            switch (synset.charAt(0)) {
              case '1': doc = (String) WordNet.wn.nounDocumentationHash.get(synset.substring(1)); break;
              case '2': doc = (String) WordNet.wn.verbDocumentationHash.get(synset.substring(1)); break;
              case '3': doc = (String) WordNet.wn.adjectiveDocumentationHash.get(synset.substring(1)); break;
              case '4': doc = (String) WordNet.wn.adverbDocumentationHash.get(synset.substring(1)); break;
            }
            doc = processStringForXMLOutput(doc);
            pw.println("  <rdfs:comment xml:lang=\"en\">" + doc + "</rdfs:comment>");
            al = (ArrayList) WordNet.wn.relations.get(synset);
            if (al != null) {
                for (int i = 0; i < al.size(); i++) {
                    AVPair avp = (AVPair) al.get(i);
                    String rel = StringUtil.StringToKIFid(avp.attribute);
                    pw.println("  <wnd:" + rel + " rdf:resource=\"#WN30-" + avp.value + "\"/>");
                }
            }
            pw.println("</owl:Thing>");
        }
    }

    /** ***************************************************************
     */
    private void writeWordNetExceptions(PrintWriter pw) throws IOException {

        Iterator it = WordNet.wn.exceptionNounHash.keySet().iterator();
        while (it.hasNext()) {
            String plural = (String) it.next();
            String singular = (String) WordNet.wn.exceptionNounHash.get(plural);
            pw.println("<owl:Thing rdf:about=\"#" + plural + "\">");
            pw.println("  <wnd:singular>" + singular + "</wnd:singular>");
            pw.println("  <rdf:type rdf:resource=\"#Word\"/>");
            pw.println("  <rdfs:label xml:lang=\"en\">" + singular + "</rdfs:label>");
            pw.println("  <rdfs:comment xml:lang=\"en\">\"" + singular + "\", is the singular form" +
                       " of the irregular plural \"" + plural + "\"</rdfs:comment>");
            pw.println("</owl:Thing>");
        }
        it = WordNet.wn.exceptionVerbHash.keySet().iterator();
        while (it.hasNext()) {
            String past = (String) it.next();
            String infinitive = (String) WordNet.wn.exceptionVerbHash.get(past);
            pw.println("<owl:Thing rdf:about=\"#" + past + "\">");
            pw.println("  <wnd:infinitive>" + infinitive + "</wnd:infinitive>");
            pw.println("  <rdf:type rdf:resource=\"#Word\"/>");
            pw.println("  <rdfs:label xml:lang=\"en\">" + past + "</rdfs:label>");
            pw.println("  <rdfs:comment xml:lang=\"en\">\"" + past + "\", is the irregular past tense form" +
                       " of the infinitive \"" + infinitive + "\"</rdfs:comment>");
            pw.println("</owl:Thing>");
        }
    }

    /** ***************************************************************
     */
    private void writeOneWordToSenses(PrintWriter pw, String word) {

        String wordAsID = StringUtil.StringToKIFid(word);
        pw.println("<owl:Thing rdf:about=\"#WN30Word-" + wordAsID + "\">");
        pw.println("  <rdf:type rdf:resource=\"#Word\"/>");
        pw.println("  <rdfs:label xml:lang=\"en\">" + word + "</rdfs:label>");
        String wordOrPhrase = "word";
        if (word.indexOf("_") != -1) 
            wordOrPhrase = "phrase";
        pw.println("  <rdfs:comment xml:lang=\"en\">The English " + wordOrPhrase + " \"" + word + "\".</rdfs:comment>");
        ArrayList senses = (ArrayList) WordNet.wn.wordsToSenseKeys.get(word);
        if (senses != null) {
        	for (int i = 0; i < senses.size(); i++) {
        		String sense = (String) senses.get(i);
        		pw.println("  <wnd:senseKey rdf:resource=\"#WN30WordSense-" + sense + "\"/>");
        	}
        }
        else
        	System.out.println("Error in OWLtranslator.writeOneWordToSenses(): no senses for word: " + word);
        pw.println("</owl:Thing>");
    }

    /** ***************************************************************
     */
    private void writeWordsToSenses(PrintWriter pw) throws IOException {

        Iterator it = WordNet.wn.wordsToSenseKeys.keySet().iterator();
        while (it.hasNext()) {
            String word = (String) it.next();
            writeOneWordToSenses(pw,word);
        }
    }

    /** ***************************************************************
     */
    private void writeSenseIndex(PrintWriter pw) throws IOException {

        Iterator it = WordNet.wn.senseIndex.keySet().iterator();
        while (it.hasNext()) {
            String sense = (String) it.next();
            String synset = (String) WordNet.wn.senseIndex.get(sense);
            pw.println("<owl:Thing rdf:about=\"#WN30WordSense-" + sense + "\">");
            pw.println("  <rdf:type rdf:resource=\"#WordSense\"/>");
            pw.println("  <rdfs:label xml:lang=\"en\">" + sense + "</rdfs:label>");
            pw.println("  <rdfs:comment xml:lang=\"en\">The WordNet word sense \"" + sense + "\".</rdfs:comment>");
            String pos = WordNetUtilities.getPOSfromKey(sense);
            String word = WordNetUtilities.getWordFromKey(sense);
            String posNum = WordNetUtilities.posLettersToNumber(pos);
            pw.println("  <wnd:synset rdf:resource=\"#WN30-" + posNum + synset + "\"/>");
            if (posNum.equals("2")) {
                ArrayList frames = (ArrayList) WordNet.wn.verbFrames.get(synset + "-" + word);
                if (frames != null) {
                    for (int i = 0; i < frames.size(); i++) {
                        String frame = (String) frames.get(i);
                        pw.println("  <wnd:verbFrame rdf:resource=\"#WN30VerbFrame-" + frame + "\"/>");
                    }
                }
            }
            pw.println("</owl:Thing>");
        }
    }

    /** ***************************************************************
     */
    private void writeWordNetHeader(PrintWriter pw) {

    	pw.println("<!DOCTYPE rdf:RDF [");
    	pw.println("   <!ENTITY wnd \"http://www.ontologyportal.org/WNDefs.owl#\">");
        pw.println("   <!ENTITY kbd \"http://www.ontologyportal.org/KBDefs.owl#\">");
        pw.println("   <!ENTITY xsd \"http://www.w3.org/2001/XMLSchema#\">");
    	pw.println("   <!ENTITY rdf \"http://www.w3.org/1999/02/22-rdf-syntax-ns#\">");
    	pw.println("   <!ENTITY rdfs \"http://www.w3.org/2000/01/rdf-schema#\">");
    	pw.println("   <!ENTITY owl \"http://www.w3.org/2002/07/owl#\">");
    	pw.println("]>");
        pw.println("<rdf:RDF");
        pw.println("xmlns=\"http://www.ontologyportal.org/WordNet.owl#\"");
        pw.println("xml:base=\"http://www.ontologyportal.org/WordNet.owl\"");
        pw.println("xmlns:wnd =\"http://www.ontologyportal.org/WNDefs.owl#\"");
        pw.println("xmlns:kbd =\"http://www.ontologyportal.org/KBDefs.owl#\"");
        pw.println("xmlns:xsd=\"http://www.w3.org/2001/XMLSchema#\"");
        pw.println("xmlns:rdf =\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\"");
        pw.println("xmlns:rdfs=\"http://www.w3.org/2000/01/rdf-schema#\"");
        pw.println("xmlns:owl =\"http://www.w3.org/2002/07/owl#\">");
        pw.println("<owl:Ontology rdf:about=\"http://www.ontologyportal.org/WordNet.owl\">");
        pw.println("<rdfs:comment xml:lang=\"en\">An expression of the Princeton WordNet " +
                   "( http://wordnet.princeton.edu ) " +
                   "in OWL.  Use is subject to the Princeton WordNet license at " +
                   "http://wordnet.princeton.edu/wordnet/license/</rdfs:comment>");
        Date d = new Date();
        pw.println("<rdfs:comment xml:lang=\"en\">Produced on date: " + d.toString() + "</rdfs:comment>");
        pw.println("</owl:Ontology>");
    }

    /** ***************************************************************
     * Write OWL format for SUMO-WordNet mappings.
     */
    public void writeWordNet(PrintWriter pw) throws IOException {

        System.out.println("INFO in OWLtranslator.writeWordNet()");
        writeWordNetHeader(pw);
        writeWordNetRelationDefinitions(pw);
        writeWordNetClassDefinitions(pw);
          // Get POS-prefixed synsets.
        Iterator it = WordNet.wn.synsetsToWords.keySet().iterator();
        while (it.hasNext()) {
            String synset = (String) it.next();
            writeWordNetSynset(pw,synset);
        }
        writeWordNetExceptions(pw);
        writeVerbFrames(pw);
        writeWordsToSenses(pw);
        writeSenseIndex(pw);
        pw.println("</rdf:RDF>");
    }

    /** ***************************************************************
     * Write OWL format for a SUMO or WordNet term.
     */
    public void writeTerm(PrintWriter pw, String term) {

    	if (kb == null) {
    		System.out.println("Error in OWLtranslator.writeTerm(): no KB");
    		return;
    	}
        if (term.startsWith("WN30")) {
            writeWordNetHeader(pw);
            if (term.startsWith("WN30-")) 
                writeWordNetSynset(pw,term);
            else if (term.startsWith("WN30Word-")) 
                writeOneWordToSenses(pw,term.substring(9));
            else if (term.startsWith("WN30WordSense-")) 
                writeOneWordToSenses(pw,term.substring(14));
        }
        else {
            writeKBHeader(pw);
            if (term.startsWith("axiom-"))
                writeOneAxiom(pw,term);
            else
                writeSUMOTerm(pw,term);                 
        }
        pw.println("</rdf:RDF>");
        pw.flush();
    }

    /** ***************************************************************
     * Write OWL format.
     */
    public void writeSUMOOWLDefs(PrintWriter pw) {

        pw.println("<owl:ObjectProperty rdf:about=\"#axiom\">");
        pw.println("  <rdfs:domain rdf:resource=\"&owl;Thing\" />");          
        pw.println("  <rdfs:range rdf:resource=\"rdfs:string\"/>");
        pw.println("  <rdfs:label xml:lang=\"en\">axiom</rdfs:label>");
        pw.println("  <rdfs:comment xml:lang=\"en\">A relation between a term\n" +
                   "and a SUO-KIF axiom that defines (in part) the meaning of the term.</rdfs:comment>");
        pw.println("</owl:ObjectProperty>");
    }

    /** ***************************************************************
     * Write OWL format.
     */
    public void writeDefsAsFiles() {

        FileWriter fw = null;
        PrintWriter pw = null;
        termPrefix = "";
        try {
            fw = new FileWriter("KBDefs.owl");
            pw = new PrintWriter(fw);
            pw.println("<!DOCTYPE rdf:RDF [");
        	pw.println("   <!ENTITY wnd \"http://www.ontologyportal.org/WNDefs.owl#\">");
            pw.println("   <!ENTITY kbd \"http://www.ontologyportal.org/KBDefs.owl#\">");
            pw.println("   <!ENTITY xsd \"http://www.w3.org/2001/XMLSchema#\">");
        	pw.println("   <!ENTITY rdf \"http://www.w3.org/1999/02/22-rdf-syntax-ns#\">");
        	pw.println("   <!ENTITY rdfs \"http://www.w3.org/2000/01/rdf-schema#\">");
        	pw.println("   <!ENTITY owl \"http://www.w3.org/2002/07/owl#\">");
        	pw.println("]>");
            pw.println("<rdf:RDF");
            pw.println("xmlns=\"http://www.ontologyportal.org/KBDefs.owl#\"");
            pw.println("xml:base=\"http://www.ontologyportal.org/KBDefs.owl\"");
            pw.println("xmlns:wnd=\"http://www.ontologyportal.org/WNDefs.owl#\"");
            pw.println("xmlns:kbd=\"http://www.ontologyportal.org/KBDefs.owl#\"");
            pw.println("xmlns:xsd=\"http://www.w3.org/2001/XMLSchema#\"");
            pw.println("xmlns:rdf =\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\"");
            pw.println("xmlns:rdfs=\"http://www.w3.org/2000/01/rdf-schema#\"");
            pw.println("xmlns:owl =\"http://www.w3.org/2002/07/owl#\">");
            pw.println("<owl:Ontology rdf:about=\"http://www.ontologyportal.org/KBDefs.owl\">");
            pw.println("<rdfs:comment xml:lang=\"en\">A provisional and necessarily lossy translation to OWL.  Please see");
            pw.println("www.ontologyportal.org for the original KIF, which is the authoritative");
            pw.println("source.  This software is released under the GNU Public License");
            pw.println("www.gnu.org.</rdfs:comment>");
            Date d = new Date();
            pw.println("<rdfs:comment xml:lang=\"en\">Produced on date: " + d.toString() + "</rdfs:comment>");
            pw.println("</owl:Ontology>");
            writeSUMOOWLDefs(pw);
            pw.println("</rdf:RDF>");
            pw.flush();
            fw.close();
            fw = new FileWriter("WNDefs.owl");
            pw = new PrintWriter(fw);
            pw.println("<!DOCTYPE rdf:RDF [");
        	pw.println("   <!ENTITY wnd \"http://www.ontologyportal.org/WNDefs.owl#\">");
            pw.println("   <!ENTITY kbd \"http://www.ontologyportal.org/KBDefs.owl#\">");
            pw.println("   <!ENTITY xsd \"http://www.w3.org/2001/XMLSchema#\">");
        	pw.println("   <!ENTITY rdf \"http://www.w3.org/1999/02/22-rdf-syntax-ns#\">");
        	pw.println("   <!ENTITY rdfs \"http://www.w3.org/2000/01/rdf-schema#\">");
        	pw.println("   <!ENTITY owl \"http://www.w3.org/2002/07/owl#\">");
        	pw.println("]>");
            pw.println("<rdf:RDF");
            pw.println("xmlns=\"http://www.ontologyportal.org/WNDefs.owl#\"");
            pw.println("xml:base=\"http://www.ontologyportal.org/WNDefs.owl\"");
            pw.println("xmlns:wnd=\"http://www.ontologyportal.org/WNDefs.owl#\"");
            pw.println("xmlns:kbd=\"http://www.ontologyportal.org/KBDefs.owl#\"");
            pw.println("xmlns:xsd=\"http://www.w3.org/2001/XMLSchema#\"");
            pw.println("xmlns:rdf =\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\"");
            pw.println("xmlns:rdfs=\"http://www.w3.org/2000/01/rdf-schema#\"");
            pw.println("xmlns:owl =\"http://www.w3.org/2002/07/owl#\">");
            pw.println("<owl:Ontology rdf:about=\"http://www.ontologyportal.org/WNDefs.owl\">");
            pw.println("<rdfs:comment xml:lang=\"en\">An expression of the Princeton WordNet " +
                       "( http://wordnet.princeton.edu ) " +
                       "in OWL.  Use is subject to the Princeton WordNet license at " +
                       "http://wordnet.princeton.edu/wordnet/license/</rdfs:comment>");
            pw.println("<rdfs:comment xml:lang=\"en\">Produced on date: " + d.toString() + "</rdfs:comment>");
            pw.println("</owl:Ontology>");
            writeWordNetExceptions(pw);
            writeWordNetRelationDefinitions(pw);
            writeVerbFrames(pw);
            writeWordNetClassDefinitions(pw);
            pw.println("</rdf:RDF>");
            pw.flush();
            fw.close();
        }
        catch (Exception e ) {
            System.out.println(e.getMessage());
            e.printStackTrace();
        }
    }

    /** ***************************************************************
     */
    public static void initOnce(String kbName) {
   
    	if (ot.kb == null || !kbName.equals(ot.kb.name))
    		initNeeded = true;
        if (initNeeded == true) {
            initNeeded = false;
            ot.writeDefsAsFiles();
            ot.kb = KBmanager.getMgr().getKB(kbName);
            ot.createAxiomMap();
            ot.readYAGOSUMOMappings();
        }
    }

    /** *************************************************************
     * A test method.
     */
    public static void main(String args[]) {

    	/*
        OWLtranslator ot = new OWLtranslator();
        try {
            KBmanager.getMgr().initializeOnce();
            ot.kb = KBmanager.getMgr().getKB("SUMO");
            ot.createAxiomMap();
            ot.writeDefsAsFiles();
            ot.readYAGOSUMOMappings();
            PrintWriter pw = new PrintWriter(System.out);
            pw.flush();
            ot.writeTerm(pw,"WN30-100019613");
            pw.flush();
            ot.writeTerm(pw,"Object");
            pw.flush();
            ot.writeTerm(pw,"axiom-N226655687Merge.kif");
            pw.flush();
        } catch (Exception e ) {
            System.out.println(e.getMessage());
            e.printStackTrace();
        }
        */
        try {
        	OWLtranslator.read(args[0]);
        }
        catch (Exception e ) {
            System.out.println(e.getMessage());
            e.printStackTrace();
        }    	
    }
}
