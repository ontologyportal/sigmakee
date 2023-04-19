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
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.articulate.sigma.dataProc.Infrastructure;
import com.articulate.sigma.nlg.LanguageFormatter;
import com.articulate.sigma.nlg.NLGUtils;
import com.articulate.sigma.trans.SUMOtoTFAform;
import com.articulate.sigma.utils.MapUtils;
import com.articulate.sigma.utils.StringUtil;
import com.articulate.sigma.wordNet.WordNet;
import com.google.common.collect.Sets;
import org.json.simple.JSONAware;
import org.json.simple.JSONValue;

/** *****************************************************************
 *  Contains utility methods for KBs
 */
public class KButilities {

    public static boolean debug = false;

    /** Errors found during processing formulas */
    public static TreeSet<String> errors = new TreeSet<String>();

    /** Warnings found during processing formulas */
    public static TreeSet<String> warnings = new TreeSet<String>();

    /** *************************************************************
     */
    public static boolean isRelation(KB kb, String term) {
        
        return kb.isInstanceOf(term,"Relation");
    }

    /** *************************************************************
     */
    public static boolean isFunction(KB kb, String term) {
        return kb.isInstanceOf(term,"Function");        
    }
    
    /** *************************************************************
     */
    public static boolean isAttribute(KB kb, String term) {
        return kb.isInstanceOf(term,"Attribute");
    }

    /** *************************************************************
     */
    public static void clearErrors() {
        errors = new TreeSet<>();
    }

    /** *************************************************************
     */
    public static boolean hasCorrectTypes(KB kb, Formula f) {

        SUMOtoTFAform.initOnce();
        SUMOtoTFAform.varmap = SUMOtoTFAform.fp.findAllTypeRestrictions(f, kb);
        if (debug) System.out.println("hasCorrectTypes() varmap: " + SUMOtoTFAform.varmap);
        HashMap<String, HashSet<String>> explicit = SUMOtoTFAform.fp.findExplicitTypes(kb, f);
        if (debug) System.out.println("hasCorrectTypes() explicit: " + explicit);
        KButilities.mergeToMap(SUMOtoTFAform.varmap,explicit,kb);
        if (SUMOtoTFAform.inconsistentVarTypes()) {
            String error = "inconsistent types in " + SUMOtoTFAform.varmap;
            System.out.println("hasCorrectTypes(): " + SUMOtoTFAform.errors);
            errors.addAll(SUMOtoTFAform.errors);
            return false;
        }
        if (SUMOtoTFAform.typeConflict(f)) {
            String error = "Type conflict: " + SUMOtoTFAform.errors;
            System.out.println("hasCorrectTypes(): " + SUMOtoTFAform.errors);
            errors.addAll(SUMOtoTFAform.errors);
            return false;
        }
        if (debug) System.out.println("hasCorrectTypes() no conflicts in: " + f);
        return true;
    }

    /** *************************************************************
     */
    public static boolean isValidFormula(KB kb, String form) {

        SUMOtoTFAform.initOnce();
        String result = "";
        try {
            KIF kif = new KIF();
            result = kif.parseStatement(form);
        }
        catch (Exception e) {
            e.printStackTrace();
            result = "";
        }
        if (!StringUtil.emptyString(result)) {
            System.out.println("isValidFormula(): Error: " + result);
            return false;
        }
        Formula f = new Formula(form);
        String term = PredVarInst.hasCorrectArity(f, kb);
        if (!StringUtil.emptyString(term)) {
            String error = "Formula rejected due to arity error of predicate " + term
                    + " in formula: \n" + f.getFormula();
            errors.add(error);
            System.out.println("isValidFormula(): Error: " + error);
            return false;
        }
        if (!hasCorrectTypes(kb,f))
            return false;
        if (debug) System.out.println("isValidFormula() valid formula: " + form);
        return true;
    }

    /** *************************************************************
     */
    public static boolean isClass(KB kb, String term) {

        return kb.isInstanceOf(term,"Class");
    }
    
    /** *************************************************************
     */
    public static boolean isInstance(KB kb, String term) {

        return !kb.isInstanceOf(term,"Class");
    }

    /** *************************************************************
     */
    public static boolean isVariableArity(KB kb, String term) {

        return kb.isInstanceOf(term,"VariableArityRelation");
    }

    /** *************************************************************
     */
    public static String getDocumentation(KB kb, String term) {

        ArrayList<Formula> forms = kb.askWithRestriction(0,"documentation",1,term);
        if (forms == null || forms.size() == 0)
            return null;
        Formula form = forms.get(0);
        if (form == null)
            return null;
        if (form.complexArgumentsToArrayList(0).size() < 3)
            return null;
        return form.getStringArgument(3);
    }

    /** *************************************************************
     * Get count of all the termFormat strings for the given language
     */
    public static int getCountTermFormats(KB kb, String lang) {

        ArrayList<Formula> forms = kb.askWithRestriction(0,"termFormat",1,lang);
        return forms.size();
    }

    /** *************************************************************
     * Get count of all the termFormat strings for unique SUMO terms
     * for the given language.  So if a term has more than one
     * termFormat, only count one
     */
    public static int getCountUniqueTermFormats(KB kb, String lang) {

        ArrayList<Formula> forms = kb.askWithRestriction(0,"termFormat",1,lang);
        HashSet<String> terms = new HashSet<>();
        for (Formula f : forms) {
            String s = f.getStringArgument(2);
            terms.add(s);
        }
        return terms.size();
    }


    /** *************************************************************
     * Get count of all the different kinds of formulas as to their
     * logical expressivity
     */
    public static Map<String,Integer> countFormulaTypes(KB kb) {

        Map<String,Integer> result = new HashMap<>();
        for (Formula f : kb.formulaMap.values()) {
            if (f.isRule()) {
                MapUtils.addToFreqMap(result, "rules", 1);
                if (f.isHorn(kb))
                    MapUtils.addToFreqMap(result, "horn", 1);
                if (f.isHigherOrder(kb)) {
                    if (f.isModal(kb))
                        MapUtils.addToFreqMap(result, "modal", 1);
                    if (f.isEpistemic(kb))
                        MapUtils.addToFreqMap(result, "epistemic", 1);
                    if (f.isTemporal(kb))
                        MapUtils.addToFreqMap(result, "temporal", 1);
                    if (f.isOtherHOL(kb))
                        MapUtils.addToFreqMap(result, "otherHOL", 1);
                }
                else
                    MapUtils.addToFreqMap(result, "first-order", 1);
            }
            else {
                if (f.isGround())
                    MapUtils.addToFreqMap(result,"ground",1);
                if (f.isBinary())
                    MapUtils.addToFreqMap(result,"binary",1);
                else
                    MapUtils.addToFreqMap(result,"higher-arity",1);
            }
        }
        return result;
    }

    /** *************************************************************
     * Generate default synonymousExternalConcept statements for a .tsv
     */
    public static void genSynLinks(String fname) {

        int termcol = 0;
        ArrayList<ArrayList<String>> spread = new ArrayList<>();
        spread = DB.readSpreadsheet(fname,null,false,'\t');
        for (ArrayList<String> row : spread) {
            if (row != null && row.size() > 1) {
                String label = row.get(termcol);
                System.out.println("(synonymousExternalConcept \"" + label + "\" Entity Taxonomy)");
            }
        }
    }

    /** *************************************************************
     * convert the numerical result of compare() to text
     */
    public static String eqNum2Text(int val) {

        switch (val) {
            case -1 : return "is shallower than";
            case 0 : return "is equal to";
            case 1 : return "is deeper than";
        }
        return "error bad value from KButilities.eqNum2Text()";
    }

    /** *************************************************************
     * Get all formulas that contain both terms. 
     */
    public static ArrayList<Formula> termIntersection(KB kb, String term1, String term2) {
  	
    	ArrayList<Formula> ant1 = kb.ask("ant",0,term1);
    	ArrayList<Formula> ant2 = kb.ask("ant",0,term2);
        ArrayList<Formula> cons1 = kb.ask("cons",0,term1);
        ArrayList<Formula> cons2 = kb.ask("cons",0,term2);
        HashSet<Formula> hrule1 = new HashSet<Formula>();
        hrule1.addAll(ant1);
        hrule1.addAll(cons1);
        HashSet<Formula> hrule2 = new HashSet<Formula>();
        hrule2.addAll(ant2);
        hrule2.addAll(cons2);
        ArrayList<Formula> result = new ArrayList<Formula>();
        result.addAll(hrule1);
        result.retainAll(hrule2);
        ArrayList<Formula> stmt1 = kb.ask("stmt",0,term1);
        ArrayList<Formula> stmt2 = kb.ask("stmt",0,term2);
        stmt1.retainAll(stmt2);
        result.addAll(stmt1);
        for (int i = 0; i < 7; i++) {
            for (int j = 0; j < 7; j++) {
            	if (j != i) {
                    ArrayList<Formula> stmt = kb.askWithRestriction(i,term1,j,term2);
                    result.addAll(stmt);
            	}
            }
        }
        return result;
    }

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
    public static boolean isCacheFile(String filename) {

        if (StringUtil.emptyString(filename))
            return false;
        if (filename.endsWith("_Cache.kif"))
            return true;
        else
            return false;
    }

    /** *************************************************************
     */
    public static void countProcesses(KB kb) {

        int count = 0;
        int wncount = 0;
        Iterator it = kb.terms.iterator();
        while (it.hasNext()) {
            String term = (String) it.next();
            if (kb.isSubclass(term,"Process")) {
                count++;
                if (WordNet.wn.SUMOHash.containsKey(term))
                    wncount += WordNet.wn.SUMOHash.get(term).size();
            }
        }
        System.out.println("SUMO Process subclass count: " + count);
        System.out.println("SUMO Process synsets: " + wncount);
    }

    /** *************************************************************
     */
    private static boolean uRLexists(String URLName){

        try {
            HttpURLConnection.setFollowRedirects(false);
            // note : you may also need
            //        HttpURLConnection.setInstanceFollowRedirects(false)
            HttpURLConnection con =
                (HttpURLConnection) new URL(URLName).openConnection();
            con.setRequestMethod("HEAD");
            return(con.getResponseCode() == HttpURLConnection.HTTP_OK);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /** *************************************************************
     */
    public static void checkURLs(KB kb) {

        URL u = null;
        ArrayList<Formula> results = kb.ask("arg",0,"externalImage");
        for (int i = 0; i < results.size(); i++) {
            Formula f = (Formula) results.get(i);
            String url = StringUtil.removeEnclosingQuotes(f.getStringArgument(2));
            if (!uRLexists(url)) 
                System.out.println(f + " doesn't exist");
        }
    }

    /** *************************************************************
     */
    public static void validatePictureList() {

        // (externalImage WaterVehicle "http://upload.wikimedia.org/wikipedia/commons/1/12/2003_LWGO_ubt.JPG") 
        // http://commons.wikimedia.org/wiki/File:2003_LWGO_ubt.JPG
        // http://upload.wikimedia.org/wikipedia/commons/f/ff/Fishing_boat_ORL-3_Gdynia_Poland_2003_ubt.JPG
        // http://en.wikipedia.org/wiki/File:Reef.jpg
        // (externalImage Reef "http://upload.wikimedia.org/wikipedia/en/3/33/Reef.jpg") 
        // http://upload.wikimedia.org/wikipedia/commons/3/33/Reef.jpg
        // 
        URL u = null;
        String line = null;

        FileReader fr = null;
        LineNumberReader lr = null;

        try {
            fr = new FileReader("pictureList.kif");
            lr = new LineNumberReader(fr);
            Pattern p = Pattern.compile("([^ ]+) ([^ ]+) \"([^\"]+)\"\\)");
            while ((line = lr.readLine()) != null) {
                Matcher m = p.matcher(line);
                if (m.matches()) {
                    String url = StringUtil.removeEnclosingQuotes(m.group(3));
                    //System.out.println("the url: " + url);
                    if (!uRLexists(url)) 
                        System.out.println(";; " + line);
                    else
                        System.out.println(line);
                }
                else
                    System.out.println(line);
            }
        }
        catch (java.io.IOException e) {
            System.out.println("Error reading pictureList.kif\n" + e.getMessage());
        }
        finally {
            try {
                if (lr != null) 
                    lr.close();            
                if (fr != null) 
                    fr.close(); 
            }
            catch (Exception ex) {
            }
        }
    }

    /** *************************************************************
     */
    public class GraphArc implements JSONAware, Comparable {

        public GraphArc(String s, String r, String t) {
            source = s;
            rel = r;
            target = t;
        }
        public String source = "";
        public String rel = "";
        public String target = "";

        public int compareTo(Object o) {

            if (o.getClass().toString().endsWith("GraphArc")) {
                GraphArc ga2 = (GraphArc) o;
                String s1 = source + rel + target;
                String s2 = ga2.source + ga2.rel + ga2.target;
                return s1.compareTo(s2);
            }
            else {
                throw new ClassCastException(o.getClass().toString());
            }
        }

        public boolean equals(Object o) {
            if (o.getClass().toString().endsWith("GraphArc")) {
                GraphArc ga2 = (GraphArc) o;
                String s1 = source + rel + target;
                String s2 = ga2.source + ga2.rel + ga2.target;
                return s1.equals(s2);
            }
            else throw new ClassCastException();
        }

        public String toString() {

            StringBuffer sb = new StringBuffer();
            sb.append("{\"source\":\"" + source + "\",\"rel\":\"" + rel + "\",\"target\":\"" + target + "\"}");
            return sb.toString();
        }

        public String toJSONString() {
            return toString();
        }
    }

    /** *************************************************************
     */
    public String semnetAsJSON3(KB kb, boolean cached, boolean strings) {

        Set<String> s = generateSemanticNetwork(kb, cached, strings);
        ArrayList<GraphArc> al = new ArrayList();
        for (String st : s) {
            String[] sp = st.split(" ");
            GraphArc ga = this.new GraphArc(sp[0],sp[1],sp[2]);
            al.add(ga);
        }
        StringBuffer sb = new StringBuffer();
        sb.append(JSONValue.toJSONString(al));
        return sb.toString();
    }

    /** *************************************************************
     */
    public Set<GraphArc> generateSemNetNeighbors(KB kb, boolean cached, boolean strings, boolean links, String term, int count) {

        if (debug) System.out.println("generateSemNetNeighbors(): term: " + term + " count: " + count);
        TreeSet<GraphArc> resultSet = new TreeSet<>();
        TreeSet<String> targets = new TreeSet<>();
        for (Formula f : kb.formulaMap.values()) {          // look at all formulas in the KB
            //if (debug) System.out.println("generateSemNetNeighbors(): check formula: " + f);
            if (isCacheFile(f.sourceFile)  && !cached) {
                if (debug) System.out.println("generateSemNetNeighbors(): cached: ");
                continue;
            }
            if ((!f.isSimpleClause(kb) || !f.isGround()) && links) {
                if (debug) System.out.println("generateSemNetNeighbors(): not simple");
                Set<String> terms = f.collectTerms();
                for (String term1 : terms) {
                    if (!term1.equals(term))
                        continue;
                    if (Formula.isLogicalOperator(term1) || Formula.isVariable(term1) || (!strings && StringUtil.isQuotedString(term1)))
                        continue;
                    for (String term2 : terms) {
                        if (Formula.isLogicalOperator(term2) || Formula.isVariable(term2) || (!strings && StringUtil.isQuotedString(term2)))
                            continue;
                        if (!term1.equals(term2)) {
                            GraphArc ga = new GraphArc(term1,"link",term2);
                            resultSet.add(ga);
                            targets.add(term2);
                        }
                    }
                    GraphArc ga = new GraphArc(term1,"inAxiom", "\"" + f.getFormula() + "\"");
                    resultSet.add(ga);
                }
            }
            else {
                String predicate = f.getStringArgument(0);
                if (debug) System.out.println("generateSemNetNeighbors(): simple");
                ArrayList<String> args = f.argumentsToArrayListString(0);
                if ((args != null && args.size() == 3) || args.get(0).equals("documentation")) { // could have a function which would return null
                    String arg1 = f.getStringArgument(1);
                    if (arg1.equals(term)) {
                        if (debug) System.out.println("generateSemNetNeighbors(): check ground formula: " + f);
                        String arg2 = f.getStringArgument(2);
                        if (args.get(0).equals("documentation"))
                            arg2 = f.getStringArgument(3);
                        if (!Formula.isVariable(arg1) && !Formula.isVariable(arg2) &&
                                (strings || !StringUtil.isQuotedString(arg1)) && (strings || !StringUtil.isQuotedString(arg2))) {
                            if (StringUtil.isQuotedString(arg2))
                                arg2 = StringUtil.removeEnclosingQuotes(arg2);
                            GraphArc ga = new GraphArc(arg1, predicate, arg2);
                            resultSet.add(ga);
                            targets.add(arg2);
                        }
                    }
                    arg1 = f.getStringArgument(2);
                    if (arg1.equals(term)) {
                        if (debug) System.out.println("generateSemNetNeighbors(): check ground formula: " + f);
                        String arg2 = f.getStringArgument(1);
                        if (!Formula.isVariable(arg1) && !Formula.isVariable(arg2) &&
                                (strings || !StringUtil.isQuotedString(arg1)) && (strings || !StringUtil.isQuotedString(arg2))) {
                            if (StringUtil.isQuotedString(arg1))
                                arg2 = StringUtil.removeEnclosingQuotes(arg1);
                            GraphArc ga = new GraphArc(arg2, predicate, arg1);
                            resultSet.add(ga);
                            targets.add(arg2);
                        }
                    }
                }
            }
        }
        //System.out.println("generateSemNetNeighbors(): before recursion: " + resultSet);
        if (count > 0)
            for (String s : targets)
                resultSet.addAll(generateSemNetNeighbors(kb,cached,strings,links,s,count-1));
        //System.out.println("generateSemNetNeighbors(): returning: " + resultSet);
        return resultSet;
    }

    /** *************************************************************
     *  Turn SUMO into a semantic network by extracting all ground
     *  binary relations, turning all higher arity relations into a
     *  set of binary relations, and making all term co-occurring in
     *  an axiom to be related with a general "link" relation. Also
     *  use the subclass hierarchy to relate all parents of terms in
     *  domain statements, through the relation itself but with a
     *  suffix designating it as a separate relation.
     *  Optionally don't show cached statements,
     *  if cached is false, or relations with String arguments, if strings
     *  is false.
     */
    private static Set<String> generateSemanticNetwork(KB kb, boolean cached, boolean strings) {

        TreeSet<String> resultSet = new TreeSet<String>();
        for (Formula f : kb.formulaMap.values()) {          // look at all formulas in the KB
            if (isCacheFile(f.sourceFile))
                continue;
            if (!f.isSimpleClause(kb) || !f.isGround()) {
                Set<String> terms = f.collectTerms();
                for (String term1 : terms) {
                    if (Formula.isLogicalOperator(term1) || Formula.isVariable(term1) || (!strings && StringUtil.isQuotedString(term1)))
                        continue;                
                    for (String term2 : terms) {
                        if (Formula.isLogicalOperator(term2) || Formula.isVariable(term2) || (!strings && StringUtil.isQuotedString(term2)))
                            continue;  
                        //resultSet.add("(link " + term1 + " " + term2 + ")");
                        if (!term1.equals(term2) && !StringUtil.isNumeric(term1) && !StringUtil.isNumeric(term2))
                            resultSet.add(term1 + " link " +  term2);
                    }
                    if (!f.getFormula().contains("\"") && (!f.getFormula().contains("(")) &&
                            (!f.getFormula().contains(")")))
                    resultSet.add(term1 + " inAxiom \"" + f.getFormula() + "\"");
                }
            }
            else {
                String predicate = f.getStringArgument(0);
                ArrayList<String> args = f.argumentsToArrayListString(1);
                if (args != null && args.size() == 2) { // could have a function which would return null
                    String arg1 = f.getStringArgument(1);
                    String arg2 = f.getStringArgument(2);
                    if (arg1.contains("(") || arg1.contains(")") || arg2.contains("(") || arg2.contains(")"))
                        System.out.println("error in generateSemanticNetwork(): for formula: " + f);
                    else if (!Formula.isLogicalOperator(arg1) && !Formula.isLogicalOperator(arg2) &&
                            !Formula.isVariable(arg1) && !Formula.isVariable(arg1) &&
                            (strings || !StringUtil.isQuotedString(arg1)) && (strings || !StringUtil.isQuotedString(arg2)))
                        resultSet.add(arg1 + " " + predicate + " " +  arg2);
                }
            }
        }
        return resultSet;
    }

    /** *************************************************************
     */
    private static String semnetAsDot(Set<String> triples) {

        StringBuffer sb = new StringBuffer();
        sb.append("graph G {");
        for (String s : triples) {
            String[] tuple = s.split(" ");
            sb.append("  \"" + tuple[0] + "\" -- \"" + tuple[2] + "\" [ label=\"" + tuple[1] + "\" ];\n");
        }
        sb.append("}");
        return sb.toString();
    }

    /** *************************************************************
     * A basic use of the JSON Graph Format http://jsongraphformat.info/
     */
    private static String semnetAsJSON(Set<String> triples, KB kb,String language) {

        if (StringUtil.emptyString(language))
            language = "EnglishLanguage";
        StringBuffer sb = new StringBuffer();
        sb.append("{\n");
        sb.append("    \"graphs\" : [ \n");
        sb.append("        {\n");
        sb.append("            \"id\": \"" + kb.name + "\",\n");
        sb.append("            \"type\": \"SUMO-graph\",\n");
        sb.append("            \"label\": \"" + kb.name + "\",\n");
        sb.append("            \"nodes\": {\n");
        for (String s : kb.getTerms()) {
            if (Formula.isLogicalOperator(s))
                continue;
            sb.append("                \"" + s + "\": {\n");
            ArrayList<Formula> forms = kb.askWithTwoRestrictions(0,"termFormat",1,language,2,s);
            String formStr = "";
            if (forms != null && forms.size() > 0) {
                Formula form = forms.iterator().next().getArgument(3);
                if (form != null && form.atom())
                    formStr = form.getFormula();
                if (!StringUtil.emptyString(formStr))
                    formStr = StringUtil.removeEnclosingQuotes(formStr);
            }
            else
                formStr = s;
            sb.append("                    \"label\" : \"" + formStr + "\"\n");
            sb.append("                },\n");
        }
        sb.deleteCharAt(sb.length()-2);
        sb.append("            },\n");
        sb.append("            \"edges\": [\n");
        for (String s : triples) {
            String[] tuple = s.split(" ");
            sb.append("                {\n");
            sb.append("                    \"source\": \"" + tuple[0] + "\",\n");
            sb.append("                    \"relation\": \"" + tuple[1] + "\",\n");
            sb.append("                    \"target\": \"" + tuple[2] + "\"\n");
            sb.append("                },\n");
        }
        sb.deleteCharAt(sb.length()-2);
        sb.append("            ]\n");
        sb.append("        }\n");
        sb.append("    ]\n");
        sb.append("}");
        return sb.toString();
    }

    /** *************************************************************
     * A basic use of the JSON Graph Format http://jsongraphformat.info/
     */
    private static void semnetAsJSON2(Set<String> triples, KB kb,String language) {

        String kbDir = KBmanager.getMgr().getPref("kbDir");
        String nodeFileStr = kbDir + File.separator + "nodes.json";
        String edgeFileStr = kbDir + File.separator + "edges.json";
        if (StringUtil.emptyString(language))
            language = "EnglishLanguage";
        StringBuffer sb = new StringBuffer();
        PrintWriter nodepw = null;
        PrintWriter edgepw = null;
        try {
            nodepw = new PrintWriter(new FileWriter(nodeFileStr, false));
            edgepw = new PrintWriter(new FileWriter(edgeFileStr, false));
            sb.append("[\n");
            for (String s : kb.getTerms()) {
                if (Formula.isLogicalOperator(s))
                    continue;
                sb.append("    { \"id\" : \"" + s + "\",\n");
                ArrayList<Formula> forms = kb.askWithTwoRestrictions(0, "termFormat", 1, language, 2, s);
                String formStr = "";
                if (forms != null && forms.size() > 0) {
                    Formula form = forms.iterator().next().getArgument(3);
                    if (form != null && form.atom())
                        formStr = form.getFormula();
                    if (!StringUtil.emptyString(formStr))
                        formStr = StringUtil.removeEnclosingQuotes(formStr);
                }
                else
                    formStr = s;
                sb.append("        \"label\" : \"" + formStr + "\"\n");
                sb.append("    },\n");
            }
            sb.deleteCharAt(sb.length() - 2);
            sb.append("]\n");
            nodepw.print(sb.toString());

            sb = new StringBuffer();
            sb.append("[\n");
            for (String s : triples) {
                String[] tuple = s.split(" ");
                sb.append("    {\n");
                sb.append("        \"source\" : \"" + tuple[0] + "\",\n");
                sb.append("        \"relation\" : \"" + tuple[1] + "\",\n");
                sb.append("        \"target\" : \"" + tuple[2] + "\"\n");
                sb.append("    },\n");
            }
            sb.deleteCharAt(sb.length() - 2);
            sb.append("]\n");
            edgepw.print(sb.toString());
        }
        catch (Exception e) {
            System.out.println(e.getMessage());
            e.printStackTrace();
        }
        finally {
            if (nodepw != null)
                nodepw.close();
            if (edgepw != null)
                edgepw.close();
        }
    }

    /** *************************************************************
     * A basic use of the JSON Graph Format http://jsongraphformat.info/
     */
    private static void semnetAsTriples(Set<String> triples, KB kb,String language) {

        String kbDir = KBmanager.getMgr().getPref("kbDir");
        String fileStr = kbDir + File.separator + "triples.txt";
        if (StringUtil.emptyString(language))
            language = "EnglishLanguage";
        StringBuffer sb = new StringBuffer();
        PrintWriter pw = null;
        try {
            pw = new PrintWriter(new FileWriter(fileStr, false));
            for (String s : kb.getTerms()) {
                if (Formula.isLogicalOperator(s))
                    continue;
                ArrayList<Formula> forms = kb.askWithTwoRestrictions(0, "termFormat", 1, language, 2, s);
                String formStr = "";
                if (forms != null && forms.size() > 0) {
                    Formula form = forms.iterator().next().getArgument(3);
                    if (form != null && form.atom())
                        formStr = form.getFormula();
                    if (!StringUtil.emptyString(formStr))
                        formStr = StringUtil.removeEnclosingQuotes(formStr);
                }
                else
                    formStr = s;
                pw.println(s + "|" + formStr);
                String doc = KButilities.getDocumentation(kb,s);
                if (!StringUtil.emptyString(doc))
                    pw.println(s + "|documentation|" + doc);
            }
            for (String s : triples) {
                String[] tuple = s.split(" ");
                pw.println(tuple[0] + "|" + tuple[1] + "|" + tuple[2]);
            }
        }
        catch (Exception e) {
            System.out.println(e.getMessage());
            e.printStackTrace();
        }
        finally {
            if (pw != null)
                pw.close();
        }
    }

    /** *************************************************************
     * A basic use of the JSON Graph Format http://jsongraphformat.info/
     */
    private static void semnetAsSQLGraph(Set<String> triples, KB kb,String language) {

        String kbDir = KBmanager.getMgr().getPref("kbDir");
        String nodeFileStr = kbDir + File.separator + "nodes.sql";
        String edgeFileStr = kbDir + File.separator + "edges.sql";
        if (StringUtil.emptyString(language))
            language = "EnglishLanguage";
        StringBuffer sb = new StringBuffer();
        PrintWriter nodepw = null;
        PrintWriter edgepw = null;
        try {
            nodepw = new PrintWriter(new FileWriter(nodeFileStr, false));
            edgepw = new PrintWriter(new FileWriter(edgeFileStr, false));
            for (String s : kb.getTerms()) {
                if (Formula.isLogicalOperator(s))
                    continue;
                sb.append("INSERT INTO nodes (id, label) values ('" + s + "',");
                ArrayList<Formula> forms = kb.askWithTwoRestrictions(0, "termFormat", 1, language, 2, s);
                String formStr = "";
                if (forms != null && forms.size() > 0) {
                    Formula form = forms.iterator().next().getArgument(3);
                    if (form != null && form.atom())
                        formStr = form.getFormula();
                    if (!StringUtil.emptyString(formStr))
                        formStr = StringUtil.removeEnclosingQuotes(formStr);
                }
                else
                    formStr = s;
                sb.append("'" + formStr + "');\n");
            }
            nodepw.print(sb.toString());

            sb = new StringBuffer();
            for (String s : triples) {
                String[] tuple = s.split(" ");
                sb.append(" INSERT INTO edges (source, rel, target) values ('" + tuple[0] + "'");
                sb.append(", '" + tuple[1] + "', '" + tuple[2] + "');\n");
            }
            edgepw.print(sb.toString());
        }
        catch (Exception e) {
            System.out.println(e.getMessage());
            e.printStackTrace();
        }
        finally {
            if (nodepw != null)
                nodepw.close();
            if (edgepw != null)
                edgepw.close();
        }
    }

    /** *************************************************************
     * Generate line pairs of formula and NL paraphrase of formula
     */
    public static String generateAllNL(KB kb) {

        StringBuffer result = new StringBuffer();
        for (String f : kb.formulaMap.keySet()) {
            if (!f.startsWith("(documentation") && !f.startsWith("(format") && !f.startsWith("(termFormat"))
                result.append(f + "\n" + StringUtil.filterHtml(NLGUtils.htmlParaphrase("", f,
                    kb.getFormatMap("EnglishLanguage"), kb.getTermFormatMap("EnglishLanguage"), kb, "EnglishLanguage")) + "\n");
        }
        return result.toString();
    }

    /** *************************************************************
     *  Find all cases of where (instance A B) (instance B C) as
     *  well as all cases of where (instance A B) (instance B C)
     *  (instance C D).  Report true if any such cases are found,
     *  false otherwise.
     */
    public static boolean instanceOfInstanceP(KB kb) {

        boolean result = false;
        for (String term : kb.terms) {
            ArrayList<Formula> al = kb.askWithRestriction(0,"instance",1,term);
            for (int i = 0; i < al.size(); i++) {
                Formula f = (Formula) al.get(i);
                String term2 = f.getStringArgument(2);
                if (Formula.atom(term2)) {
                    ArrayList<Formula> al2 = kb.askWithRestriction(0,"instance",1,term2);
                    if (al2.size() > 0)
                        result = true;
                    for (int j = 0; j < al2.size(); j++) {
                        Formula f2 = (Formula) al2.get(j);
                        String term3 = f2.getStringArgument(2);
                        if (Formula.atom(term3)) {
                            ArrayList<Formula> al3 = kb.askWithRestriction(0,"instance",1,term3);
                            for (int k = 0; k < al3.size(); k++) {
                                Formula f3 = (Formula) al3.get(k);
                                String term4 = f3.getStringArgument(2);
                            }
                        }
                    }
                }
            }
        }
        return result;
    }

    /** *************************************************************
     */
    public static void writeDisplayText(KB kb, String displayFormatPredicate, String displayTermPredicate, 
            String language, String fname) throws IOException {
        
        PrintWriter pr = null;
        try {
            pr = new PrintWriter(new FileWriter(fname, false));            
            //get all formulas that have the display predicate as the predicate           
            ArrayList<Formula> formats = kb.askWithRestriction(0, displayFormatPredicate, 1, language);
            ArrayList<Formula> terms = kb.askWithRestriction(0, displayTermPredicate, 1, language);            
            HashMap<String,String> termMap = new HashMap<String,String>();            
            for (int i = 0; i < terms.size(); i++) {
                Formula term = terms.get(i);                
                String key = term.getStringArgument(2);
                String value = term.getStringArgument(3);
                if (key != "" && value != "") 
                    termMap.put(key, value);                
            }            
            for (int i = 0; i < formats.size(); i++) {
                Formula format = formats.get(i);                
                // This is the current predicate whose format we are keeping track of. 
                String key = format.getStringArgument(2);
                String value = format.getStringArgument(3);
                if (key != "" && value != "") {                
                    // This basically gets all statements that use the current predicate in the 0 position
                    ArrayList<Formula> predInstances = kb.ask("arg", 0, key);                    
                    for(int j=0; j < predInstances.size(); j++) {
                        StringBuilder sb = new StringBuilder();
                        String displayText = String.copyValueOf(value.toCharArray());                        
                        Formula f = predInstances.get(j);
                        ArrayList arguments = f.complexArgumentsToArrayList(0);
                        sb.append(key);
                        sb.append(",");           
                        // check if each of the arguments for the statements is to be replaced in its
                        // format statement.
                        for (int k = 1; k < arguments.size(); k++) {
                            String argName = f.getStringArgument(k);
                            String term = (String) termMap.get(argName);
                            term = StringUtil.removeEnclosingQuotes(term);
                            String argNum = "%" + String.valueOf(k);
                        
                            // also, add the SUMO Concept that is replaced in the format
                            if (displayText.contains(argNum)) {
                                sb.append(argName);
                                sb.append(",");
                                displayText = displayText.replace(argNum, term);                                
                            }                                                                
                        }                                             
                        sb.append(displayText);                                               
                        // resulting line will be something like:
                        // <predicate>, <argument_0>, ..., <argument_n>, <display_text>
                        // note: argument_0 to argument_n is only placed there if their 
                        // termFormat is used in the display_text.
                        pr.println(sb.toString());                        
                    }                    
                }
            }            
            
        }
        catch (java.io.IOException e) {
            System.out.println(e.getMessage());
            e.printStackTrace();
        }
        catch (Exception e) {
            System.out.println(e.getMessage());
            e.printStackTrace();
        }
        finally {
            if (pr != null) 
                pr.close();            
        }
    }

    /** *************************************************************
     */
    public static void generateTPTPTestAssertions() {
        
        try {
            int counter = 0;
            System.out.println("INFO in KB.generateTPTPTestAssertions()");
            KBmanager.getMgr().initializeOnce();
            KB kb = KBmanager.getMgr().getKB(KBmanager.getMgr().getPref("sumokbname"));
            System.out.println("INFO in KB.generateTPTPTestAssertions(): printing predicates");
            for (String term : kb.terms) {
                if (Character.isLowerCase(term.charAt(0)) && kb.kbCache.valences.get(term) <= 2) {
                    /*
                    ArrayList<Formula> forms = kb.askWithRestriction(0,"domain",1,term);
                    for (int i = 0; i < forms.size(); i++) {
                        String argnum = forms.get(i).getArgument(2);
                        String type = forms.get(i).getArgument(3);
                        if (argnum.equals("1"))
                            System.out.print("(instance Foo " + type + "),");
                        if (argnum.equals("2"))
                            System.out.print("(instance Bar " + type + ")");
                    }
                    */
                    String argType1 = kb.getArgType(term,1);
                    String argType2 = kb.getArgType(term,2);
                    if (argType1 != null && argType2 != null) {
                        System.out.print("fof(local_" + counter++ + ",axiom,(s__" + term + "(s__Foo,s__Bar))).|");
                        System.out.print("fof(local_" + counter++ + ",axiom,(s__instance(s__Foo,s__" + argType1 + "))).|");
                        System.out.println("fof(local_" + counter++ + ",axiom,(s__instance(s__Bar,s__" + argType2 + "))).");
                    }                    
                }
            }
        } 
        catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }
    
    /** *************************************************************
     * Note this simply assumes that initial lower case terms are relations.
     */
    public static void generateRelationList() {
        
        try {
            System.out.println("INFO in KB.generateRelationList()");
            KBmanager.getMgr().initializeOnce();
            KB kb = KBmanager.getMgr().getKB(KBmanager.getMgr().getPref("sumokbname"));
            System.out.println("INFO in KB.generateRelationList(): printing predicates");
            for (String term : kb.terms) {
                if (Character.isLowerCase(term.charAt(0)))
                    System.out.println(term);
            }
        }
        catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }

    /** *************************************************************
     */
    public static int getCountNonLinguisticAxioms(KB kb) {

        HashSet<String> rels = new HashSet<>();
        rels.add("documentation");
        rels.add("termFormat");
        rels.add("format");
        int counter = 0;
        HashSet<Formula> forms = new HashSet<>();
        forms.addAll(kb.formulaMap.values());
        for (Formula f : forms) {
            if (!rels.contains(f.getArgument(0)))
                counter++;
        }
        return counter;
    }

    /** *************************************************************
     * Count the number of words in all the strings in a knowledge base
     */
    public static void countStringWords(KB kb) {

        int total = 0;
        System.out.println("INFO in KB.countStringWords(): counting words");
        Iterator<String> it = kb.formulas.keySet().iterator();
        while (it.hasNext()) {
            String s = it.next();
            Pattern p = Pattern.compile("\"(.+)\"");
            Matcher m = p.matcher(s);
            boolean b = m.find();
            if (b) {
                String quoted = m.group(1);
                String[] ar = quoted.split(" ");
                for (int i = 0; i < ar.length-1; i++) {
                    if (ar[i].matches("\\w+"))
                        total++;
                }
                System.out.println(quoted);
                System.out.println(ar.length);
            }
        }
        System.out.println(total);
    }

    /** *************************************************************
     *  Find all formulas in which the SUMO term is involved.  
     */
    public static Set<Formula> getAllFormulasOfTerm(KB kb, String term) {

		HashSet<Formula> result = new HashSet<>();
		Pattern pattern = Pattern.compile("(\\s|\\()" + term + "(\\s|\\))");		
		for (String f : kb.formulaMap.keySet()){
			Matcher matcher = pattern.matcher(f);
			if (matcher.find()) {
				result.add(kb.formulaMap.get(f));
			}
		}
		return result;
	}

    /** *************************************************************
     *  Find all formulas in which the SUMO term is involved.
     */
    public static String generateFormulasAndDoc(KB kb) {

        StringBuffer sb = new StringBuffer();
        for (String t : kb.terms) {
            String doc = getDocumentation(kb,t);
            if (!StringUtil.emptyString(doc)) {
                Set<Formula> allForms = getAllFormulasOfTerm(kb,t);
                sb.append("!!doc " + doc + "\n");
                for (Formula f : allForms) {
                    if (!FormulaUtil.isDoc(f))
                        sb.append(f.toString() + "\n");
                }
            }
        }
        return sb.toString();
    }

    /** *************************************************************
     * List all the terms and their termFormat expressions.
     */
    public static String termFormatIndex(KB kb) {

        StringBuffer sb = new StringBuffer();
        ArrayList<Formula> forms = kb.ask("arg", 0, "termFormat");
        for (Formula f : forms) {
            String term = f.getStringArgument(2);
            String str = f.getStringArgument(3);
            if (!StringUtil.emptyString(term) && ! StringUtil.emptyString((str)))
                sb.append(term + "\t" + str + "\n");
        }
        return sb.toString();
    }

    /** ***************************************************************
     * utility method to merge two HashMaps of String keys and a values
     * of an HashSet of Strings.  Note that parent classes in the set of
     * classes will be removed
     */
    public static HashMap<String, HashSet<String>> mergeToMap(HashMap<String, HashSet<String>> map1,
                                                              HashMap<String, HashSet<String>> map2, KB kb) {

        HashMap<String, HashSet<String>> result = new HashMap<String,HashSet<String>>(map1);

        for (String key : map2.keySet()) {
            Set<String> value = new HashSet<String>();
            if (result.containsKey(key)) {
                value = result.get(key);
            }
            value.addAll(map2.get(key));
            value = kb.removeSuperClasses(value);
            result.put(key, Sets.newHashSet(value));
        }
        return result;
    }

    /** ***************************************************************
    */
    public static void showHelp() {

        System.out.println("KButilities class");
        System.out.println("  options:");
        System.out.println("  -h - show this help screen");
        System.out.println("  -c <fname> - generate external links from file fname");
        System.out.println("  -s - count strings and processes");
        System.out.println("  -d - generate semantic network as .dot");
        System.out.println("  -j - generate semantic network as JSON");
        System.out.println("  -o - generate semantic network as another JSON format");
        System.out.println("  -q - generate semantic network as SQL");
        System.out.println("  -r - generate semantic network as |-delimited tripls");
        System.out.println("  -n - generate NL for every formula");
        System.out.println("  -f - list formulas for every documentation string term");
        System.out.println("  -v - is formula valid");
        System.out.println("  -a \"<formula>\" - show all attributes of a SUO-KIF formula");
        System.out.println("  -t - generate a table of termFormat(s)");
    }

    /** *************************************************************
     */
    public static void main(String[] args) {

        if (args != null && args.length > 0 && args[0].equals("-h"))
            showHelp();
        else {
            KBmanager.getMgr().initializeOnce();
            KB kb = KBmanager.getMgr().getKB(KBmanager.getMgr().getPref("sumokbname"));
            System.out.println("KBmutilities.main(): completed init");
            //countRelations(kb);
            //checkURLs(kb);
            //validatePictureList();
            //for (String s : generateSemanticNetwork(kb))
            //    System.out.println(s);
            KButilities kbu = new KButilities();
            Infrastructure infra = new Infrastructure();
            if (args != null && args.length > 1 && args[0].equals("-c")) {
                genSynLinks(args[1]);
            }
            else if (args != null && args.length > 0 && args[0].equals("-j")) {
                Set<String> tuples = generateSemanticNetwork(kb,false,false);
                semnetAsJSON2(tuples,kb,"EnglishLanguage");
            }
            else if (args != null && args.length > 0 && args[0].equals("-o")) {
                System.out.println(kbu.semnetAsJSON3(kb,true,true));
            }
            else if (args != null && args.length > 2 && args[0].equals("-n")) {
                // KB kb, boolean cached, boolean strings, boolean links, String term, int count
                Set<GraphArc> ts = kbu.generateSemNetNeighbors(kb,false,true,false,args[1],Integer.parseInt(args[2]));
                System.out.println(JSONValue.toJSONString(ts).replaceAll("\\{\"","\n\\{\""));
            }
            else if (args != null && args.length > 0 && args[0].equals("-q")) {
                Set<String> tuples = generateSemanticNetwork(kb,false,false);
                semnetAsSQLGraph(tuples,kb,"EnglishLanguage");
            }
            else if (args != null && args.length > 0 && args[0].equals("-r")) {
                Set<String> tuples = generateSemanticNetwork(kb,false,false);
                semnetAsTriples(tuples,kb,"EnglishLanguage");
            }
            else if (args != null && args.length > 0 && args[0].equals("-s")) {
                countStringWords(kb);
                countProcesses(kb);
            }
            else if (args != null && args.length > 0 && args[0].equals("-d")) {
                Set<String> tuples = generateSemanticNetwork(kb,false,false);
                System.out.println(semnetAsDot(tuples));
            }
            else if (args != null && args.length > 0 && args[0].equals("-n")) {
                System.out.println(generateAllNL(kb));
            }
            else if (args != null && args.length > 0 && args[0].equals("-f")) {
                System.out.println(generateFormulasAndDoc(kb));
            }
            else if (args != null && args.length > 1 && args[0].equals("-v")) {
                SUMOtoTFAform.initOnce();
                System.out.println(isValidFormula(kb,args[1]));
            }
            else if (args != null && args.length > 1 && args[0].equals("-a")) {
                SUMOtoTFAform.initOnce();
                Formula f = new Formula(StringUtil.removeEnclosingQuotes(args[1]));
                f.debug = true;
                System.out.println("higherOrder : " + f.isHigherOrder(kb));
                f.debug = false;
                System.out.println("isFunctional : " + f.isFunctional);
                System.out.println("isGround : " + f.isGround);
                System.out.println("termCache : " + f.termCache);
                System.out.println("predVarCache : " + f.predVarCache);
                System.out.println("quantVarsCache : " + f.quantVarsCache);
            }
            else if (args != null && args.length > 0 && args[0].equals("-t")) {
                System.out.println(termFormatIndex(kb));
            }
            else
                showHelp();
        }
    }
}

