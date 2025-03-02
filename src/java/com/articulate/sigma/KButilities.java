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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

//import com.articulate.sigma.dataProc.Infrastructure;
import com.articulate.sigma.nlg.NLGUtils;
import com.articulate.sigma.trans.SUMOtoTFAform;
import com.articulate.sigma.utils.FileUtil;
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

    /** Uses the number of available processors to set the thread pool count */
    public static final ExecutorService EXECUTOR_SERVICE = Executors.newWorkStealingPool();

    public static boolean debug = false;

    /** Errors found during processing formulas */
    public static Set<String> errors = new TreeSet<>();

    /** Warnings found during processing formulas */
    public static Set<String> warnings = new TreeSet<>();

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
        errors.clear();
    }

    /** *************************************************************
     */
    public static boolean hasCorrectTypes(KB kb, Formula f) {

        SUMOtoTFAform.initOnce();
        SUMOtoTFAform.varmap = SUMOtoTFAform.fp.findAllTypeRestrictions(f, kb);
        if (debug) System.out.println("hasCorrectTypes() varmap: " + SUMOtoTFAform.varmap);
        Map<String, Set<String>> explicit = SUMOtoTFAform.fp.findExplicitTypes(kb, f);
        if (debug) System.out.println("hasCorrectTypes() explicit: " + explicit);
        KButilities.mergeToMap(SUMOtoTFAform.varmap,explicit,kb);
        String error;
        if (SUMOtoTFAform.inconsistentVarTypes()) {
            error = "inconsistent types in " + SUMOtoTFAform.varmap;
            System.out.println("hasCorrectTypes(): " + error);
            errors.addAll(SUMOtoTFAform.errors);
            return false;
        }
        if (SUMOtoTFAform.typeConflict(f)) {
            error = "Type conflict: " + SUMOtoTFAform.errors;
            System.out.println("hasCorrectTypes(): " + error);
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
        String result;
        KIF kif = new KIF();
        try {
            result = kif.parseStatement(form);
        }
        catch (Exception e) {
            e.printStackTrace();
            result = "";
        }
        if (!StringUtil.emptyString(result)) {
            errors.addAll(kif.errorSet);
            if (debug) System.out.println("isValidFormula(): Error: " + result);
            return false;
        }
        Formula f = new Formula(form);
        String term = PredVarInst.hasCorrectArity(f, kb);
        if (!StringUtil.emptyString(term)) {
            String error = "Formula rejected due to arity error of predicate " + term
                    + " in formula: \n" + f.getFormula();
            errors.add(error);
            System.err.println("isValidFormula(): Error: " + error);
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

        List<Formula> forms = kb.askWithRestriction(0,"documentation",1,term);
        if (forms == null || forms.isEmpty())
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

        List<Formula> forms = kb.askWithRestriction(0,"termFormat",1,lang);
        return forms.size();
    }

    /** *************************************************************
     * Get count of all the termFormat strings for unique SUMO terms
     * for the given language.  So if a term has more than one
     * termFormat, only count one
     */
    public static int getCountUniqueTermFormats(KB kb, String lang) {

        List<Formula> forms = kb.askWithRestriction(0,"termFormat",1,lang);
        Set<String> terms = new HashSet<>();
        String s;
        for (Formula f : forms) {
            s = f.getStringArgument(2);
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
        List<List<String>> spread = DB.readSpreadsheet(fname,null,false,'\t');
        String label;
        for (List<String> row : spread) {
            if (row != null && row.size() > 1) {
                label = row.get(termcol);
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
    public static List<Formula> termIntersection(KB kb, String term1, String term2) {

    	List<Formula> ant1 = kb.ask("ant",0,term1);
    	List<Formula> ant2 = kb.ask("ant",0,term2);
        List<Formula> cons1 = kb.ask("cons",0,term1);
        List<Formula> cons2 = kb.ask("cons",0,term2);
        Set<Formula> hrule1 = new HashSet<>();
        hrule1.addAll(ant1);
        hrule1.addAll(cons1);
        Set<Formula> hrule2 = new HashSet<>();
        hrule2.addAll(ant2);
        hrule2.addAll(cons2);
        List<Formula> result = new ArrayList<>();
        result.addAll(hrule1);
        result.retainAll(hrule2);
        List<Formula> stmt1 = kb.ask("stmt",0,term1);
        List<Formula> stmt2 = kb.ask("stmt",0,term2);
        stmt1.retainAll(stmt2);
        result.addAll(stmt1);
        List<Formula> stmt;
        for (int i = 0; i < 7; i++) {
            for (int j = 0; j < 7; j++) {
            	if (j != i) {
                    stmt = kb.askWithRestriction(i,term1,j,term2);
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
        String term;
        List al;
        while (it.hasNext()) {
            term = (String) it.next();
            al = kb.ask("arg",0,term);
            if (al != null && !al.isEmpty()) {
                System.out.println(term + " " + al.size());
            }
        }
    }

    /** *************************************************************
     */
    public static boolean isCacheFile(String filename) {

        if (StringUtil.emptyString(filename))
            return false;
        return filename.endsWith("_Cache.kif");
    }

    /** *************************************************************
     */
    public static void countProcesses(KB kb) {

        int count = 0;
        int wncount = 0;
        for (String term : kb.terms) {
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
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    /** *************************************************************
     */
    public static void checkURLs(KB kb) {

        URL u;
        List<Formula> results = kb.ask("arg",0,"externalImage");
        Formula f;
        String url;
        for (int i = 0; i < results.size(); i++) {
            f = (Formula) results.get(i);
            url = StringUtil.removeEnclosingQuotes(f.getStringArgument(2));
            if (!uRLexists(url))
                System.err.println(f + " doesn't exist");
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
        URL u;
        String line;

        try (FileReader fr = new FileReader("pictureList.kif"); LineNumberReader lr = new LineNumberReader(fr)) {
            Pattern p = Pattern.compile("([^ ]+) ([^ ]+) \"([^\"]+)\"\\)");
            Matcher m;
            String url;
            while ((line = lr.readLine()) != null) {
                m = p.matcher(line);
                if (m.matches()) {
                    url = StringUtil.removeEnclosingQuotes(m.group(3));
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
        catch (IOException e) {
            System.err.println("Error reading pictureList.kif\n" + e.getMessage());
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

        @Override
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

        @Override
        public boolean equals(Object o) {
            if (o.getClass().toString().endsWith("GraphArc")) {
                GraphArc ga2 = (GraphArc) o;
                String s1 = source + rel + target;
                String s2 = ga2.source + ga2.rel + ga2.target;
                return s1.equals(s2);
            }
            else throw new ClassCastException();
        }

        @Override
        public String toString() {

            StringBuilder sb = new StringBuilder();
            sb.append("{\"source\":\"").append(source).append("\",\"rel\":\"").append(rel).append("\",\"target\":\"").append(target).append("\"}");
            return sb.toString();
        }

        @Override
        public String toJSONString() {
            return toString();
        }
    }

    /** *************************************************************
     */
    public String semnetAsJSON3(KB kb, boolean cached, boolean strings) {

        Set<String> s = generateSemanticNetwork(kb, cached, strings);
        List<GraphArc> al = new ArrayList();
        for (String st : s) {
            String[] sp = st.split(" ");
            GraphArc ga = this.new GraphArc(sp[0],sp[1],sp[2]);
            al.add(ga);
        }
        StringBuilder sb = new StringBuilder();
        sb.append(JSONValue.toJSONString(al));
        return sb.toString();
    }

    /** *************************************************************
     */
    public Set<GraphArc> generateSemNetNeighbors(KB kb, boolean cached, boolean strings, boolean links, String term, int count) {

        if (debug) System.out.println("generateSemNetNeighbors(): term: " + term + " count: " + count);
        Set<GraphArc> resultSet = new TreeSet<>();
        Set<String> targets = new TreeSet<>();
        Set<String> terms;
        GraphArc ga;
        String predicate, arg1, arg2;
        List<String> args;
        for (Formula f : kb.formulaMap.values()) {          // look at all formulas in the KB
            //if (debug) System.out.println("generateSemNetNeighbors(): check formula: " + f);
            if (isCacheFile(f.sourceFile)  && !cached) {
                if (debug) System.out.println("generateSemNetNeighbors(): cached: ");
                continue;
            }
            if ((!f.isSimpleClause(kb) || !f.isGround()) && links) {
                if (debug) System.out.println("generateSemNetNeighbors(): not simple");
                terms = f.collectTerms();
                for (String term1 : terms) {
                    if (!term1.equals(term))
                        continue;
                    if (Formula.isLogicalOperator(term1) || Formula.isVariable(term1) || (!strings && StringUtil.isQuotedString(term1)))
                        continue;
                    for (String term2 : terms) {
                        if (Formula.isLogicalOperator(term2) || Formula.isVariable(term2) || (!strings && StringUtil.isQuotedString(term2)))
                            continue;
                        if (!term1.equals(term2)) {
                            ga = new GraphArc(term1,"link",term2);
                            resultSet.add(ga);
                            targets.add(term2);
                        }
                    }
                    ga = new GraphArc(term1,"inAxiom", "\"" + f.getFormula() + "\"");
                    resultSet.add(ga);
                }
            }
            else {
                predicate = f.getStringArgument(0);
                if (debug) System.out.println("generateSemNetNeighbors(): simple");
                args = f.argumentsToArrayListString(0);
                if ((args != null && args.size() == 3) || args.get(0).equals("documentation")) { // could have a function which would return null
                    arg1 = f.getStringArgument(1);
                    if (arg1.equals(term)) {
                        if (debug) System.out.println("generateSemNetNeighbors(): check ground formula: " + f);
                        arg2 = f.getStringArgument(2);
                        if (args.get(0).equals("documentation"))
                            arg2 = f.getStringArgument(3);
                        if (!Formula.isVariable(arg1) && !Formula.isVariable(arg2) &&
                                (strings || !StringUtil.isQuotedString(arg1)) && (strings || !StringUtil.isQuotedString(arg2))) {
                            if (StringUtil.isQuotedString(arg2))
                                arg2 = StringUtil.removeEnclosingQuotes(arg2);
                            ga = new GraphArc(arg1, predicate, arg2);
                            resultSet.add(ga);
                            targets.add(arg2);
                        }
                    }
                    arg1 = f.getStringArgument(2);
                    if (arg1.equals(term)) {
                        if (debug) System.out.println("generateSemNetNeighbors(): check ground formula: " + f);
                        arg2 = f.getStringArgument(1);
                        if (!Formula.isVariable(arg1) && !Formula.isVariable(arg2) &&
                                (strings || !StringUtil.isQuotedString(arg1)) && (strings || !StringUtil.isQuotedString(arg2))) {
                            if (StringUtil.isQuotedString(arg1))
                                arg2 = StringUtil.removeEnclosingQuotes(arg1);
                            ga = new GraphArc(arg2, predicate, arg1);
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

        Set<String> resultSet = new TreeSet<>();
        Set<String> terms;
        String predicate, arg1, arg2;
        List<String> args;
        for (Formula f : kb.formulaMap.values()) {          // look at all formulas in the KB
            if (isCacheFile(f.sourceFile))
                continue;
            if (!f.isSimpleClause(kb) || !f.isGround()) {
                terms = f.collectTerms();
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
                predicate = f.getStringArgument(0);
                args = f.argumentsToArrayListString(1);
                if (args != null && args.size() == 2) { // could have a function which would return null
                    arg1 = f.getStringArgument(1);
                    arg2 = f.getStringArgument(2);
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

        StringBuilder sb = new StringBuilder();
        sb.append("graph G {");
        String[] tuple;
        for (String s : triples) {
            tuple = s.split(" ");
            sb.append("  \"").append(tuple[0]).append("\" -- \"").append(tuple[2]).append("\" [ label=\"").append(tuple[1]).append("\" ];\n");
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
        StringBuilder sb = new StringBuilder();
        sb.append("{\n");
        sb.append("    \"graphs\" : [ \n");
        sb.append("        {\n");
        sb.append("            \"id\": \"").append(kb.name).append("\",\n");
        sb.append("            \"type\": \"SUMO-graph\",\n");
        sb.append("            \"label\": \"").append(kb.name).append("\",\n");
        sb.append("            \"nodes\": {\n");
        List<Formula> forms;
        String formStr;
        Formula form;
        for (String s : kb.getTerms()) {
            if (Formula.isLogicalOperator(s))
                continue;
            sb.append("                \"").append(s).append("\": {\n");
            forms = kb.askWithTwoRestrictions(0,"termFormat",1,language,2,s);
            formStr = "";
            if (forms != null && !forms.isEmpty()) {
                form = forms.iterator().next().getArgument(3);
                if (form != null && form.atom())
                    formStr = form.getFormula();
                if (!StringUtil.emptyString(formStr))
                    formStr = StringUtil.removeEnclosingQuotes(formStr);
            }
            else
                formStr = s;
            sb.append("                    \"label\" : \"").append(formStr).append("\"\n");
            sb.append("                },\n");
        }
        sb.deleteCharAt(sb.length()-2);
        sb.append("            },\n");
        sb.append("            \"edges\": [\n");
        String[] tuple;
        for (String s : triples) {
            tuple = s.split(" ");
            sb.append("                {\n");
            sb.append("                    \"source\": \"").append(tuple[0]).append("\",\n");
            sb.append("                    \"relation\": \"").append(tuple[1]).append("\",\n");
            sb.append("                    \"target\": \"").append(tuple[2]).append("\"\n");
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
        StringBuilder sb = new StringBuilder();
        List<Formula> forms;
        String formStr;
        Formula form;
        try (PrintWriter nodepw = new PrintWriter(new FileWriter(nodeFileStr, false)); PrintWriter edgepw = new PrintWriter(new FileWriter(edgeFileStr, false))) {
            sb.append("[\n");
            for (String s : kb.getTerms()) {
                if (Formula.isLogicalOperator(s))
                    continue;
                sb.append("    { \"id\" : \"").append(s).append("\",\n");
                forms = kb.askWithTwoRestrictions(0, "termFormat", 1, language, 2, s);
                formStr = "";
                if (forms != null && !forms.isEmpty()) {
                    form = forms.iterator().next().getArgument(3);
                    if (form != null && form.atom())
                        formStr = form.getFormula();
                    if (!StringUtil.emptyString(formStr))
                        formStr = StringUtil.removeEnclosingQuotes(formStr);
                }
                else
                    formStr = s;
                sb.append("        \"label\" : \"").append(formStr).append("\"\n");
                sb.append("    },\n");
            }
            sb.deleteCharAt(sb.length() - 2);
            sb.append("]\n");
            nodepw.print(sb.toString());

            sb = new StringBuilder();
            sb.append("[\n");
            String[] tuple;
            for (String s : triples) {
                tuple = s.split(" ");
                sb.append("    {\n");
                sb.append("        \"source\" : \"").append(tuple[0]).append("\",\n");
                sb.append("        \"relation\" : \"").append(tuple[1]).append("\",\n");
                sb.append("        \"target\" : \"").append(tuple[2]).append("\"\n");
                sb.append("    },\n");
            }
            sb.deleteCharAt(sb.length() - 2);
            sb.append("]\n");
            edgepw.print(sb.toString());
        }
        catch (IOException e) {
            System.err.println(e.getMessage());
            e.printStackTrace();
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
        String formStr, doc;
        List<Formula> forms;
        Formula form;
        String[] tuple;
        try (PrintWriter pw = new PrintWriter(new FileWriter(fileStr, false))) {
            for (String s : kb.getTerms()) {
                if (Formula.isLogicalOperator(s))
                    continue;
                forms = kb.askWithTwoRestrictions(0, "termFormat", 1, language, 2, s);
                formStr = "";
                if (forms != null && !forms.isEmpty()) {
                    form = forms.iterator().next().getArgument(3);
                    if (form != null && form.atom())
                        formStr = form.getFormula();
                    if (!StringUtil.emptyString(formStr))
                        formStr = StringUtil.removeEnclosingQuotes(formStr);
                }
                else
                    formStr = s;
                pw.println(s + "|" + formStr);
                doc = KButilities.getDocumentation(kb,s);
                if (!StringUtil.emptyString(doc))
                    pw.println(s + "|documentation|" + doc);
            }
            for (String s : triples) {
                tuple = s.split(" ");
                pw.println(tuple[0] + "|" + tuple[1] + "|" + tuple[2]);
            }
        }
        catch (Exception e) {
            System.err.println(e.getMessage());
            e.printStackTrace();
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
        StringBuilder sb = new StringBuilder();
        List<Formula> forms;
        String formStr;
        Formula form;
        String[] tuple;
        try (PrintWriter nodepw = new PrintWriter(new FileWriter(nodeFileStr, false)); PrintWriter edgepw = new PrintWriter(new FileWriter(edgeFileStr, false))) {
            for (String s : kb.getTerms()) {
                if (Formula.isLogicalOperator(s))
                    continue;
                sb.append("INSERT INTO nodes (id, label) values ('").append(s).append("',");
                forms = kb.askWithTwoRestrictions(0, "termFormat", 1, language, 2, s);
                formStr = "";
                if (forms != null && !forms.isEmpty()) {
                    form = forms.iterator().next().getArgument(3);
                    if (form != null && form.atom())
                        formStr = form.getFormula();
                    if (!StringUtil.emptyString(formStr))
                        formStr = StringUtil.removeEnclosingQuotes(formStr);
                }
                else
                    formStr = s;
                sb.append("'").append(formStr).append("');\n");
            }
            nodepw.print(sb.toString());

            sb = new StringBuilder();
            for (String s : triples) {
                tuple = s.split(" ");
                sb.append(" INSERT INTO edges (source, rel, target) values ('").append(tuple[0]).append("'");
                sb.append(", '").append(tuple[1]).append("', '").append(tuple[2]).append("');\n");
            }
            edgepw.print(sb.toString());
        }
        catch (IOException e) {
            System.err.println(e.getMessage());
            e.printStackTrace();
        }
    }

    /** *************************************************************
     * Generate line pairs of formula and NL paraphrase of formula
     */
    public static String generateAllNL(KB kb) {

        StringBuilder result = new StringBuilder();
        for (String f : kb.formulaMap.keySet()) {
            if (!f.startsWith("(documentation") && !f.startsWith("(format") && !f.startsWith("(termFormat"))
                result.append(f).append("\n").append(StringUtil.filterHtml(NLGUtils.htmlParaphrase("", f,
                        kb.getFormatMap("EnglishLanguage"), kb.getTermFormatMap("EnglishLanguage"), kb, "EnglishLanguage"))).append("\n");
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
        List<Formula> al, al2, al3;
        Formula f, f2, f3;
        String term2, term3, term4;
        for (String term : kb.terms) {
            al = kb.askWithRestriction(0,"instance",1,term);
            for (int i = 0; i < al.size(); i++) {
                f = (Formula) al.get(i);
                term2 = f.getStringArgument(2);
                if (Formula.atom(term2)) {
                    al2 = kb.askWithRestriction(0,"instance",1,term2);
                    if (!al2.isEmpty())
                        result = true;
                    for (int j = 0; j < al2.size(); j++) {
                        f2 = (Formula) al2.get(j);
                        term3 = f2.getStringArgument(2);
                        if (Formula.atom(term3)) {
                            al3 = kb.askWithRestriction(0,"instance",1,term3);
                            for (int k = 0; k < al3.size(); k++) {
                                f3 = (Formula) al3.get(k);
                                term4 = f3.getStringArgument(2);
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

        try (PrintWriter pr = new PrintWriter(new FileWriter(fname, false))) {
            //get all formulas that have the display predicate as the predicate
            List<Formula> formats = kb.askWithRestriction(0, displayFormatPredicate, 1, language);
            List<Formula> terms = kb.askWithRestriction(0, displayTermPredicate, 1, language);
            Map<String,String> termMap = new HashMap<>();
            Formula term;
            String key, value, argName, argNum;
            for (int i = 0; i < terms.size(); i++) {
                term = terms.get(i);
                key = term.getStringArgument(2);
                value = term.getStringArgument(3);
                if (!"".equals(key) && !"".equals(value))
                    termMap.put(key, value);
            }
            Formula format, f;
            String sTerm, displayText;
            StringBuilder sb;
            List<Formula> predInstances, arguments;
            for (int i = 0; i < formats.size(); i++) {
                format = formats.get(i);
                // This is the current predicate whose format we are keeping track of.
                key = format.getStringArgument(2);
                value = format.getStringArgument(3);
                if (!"".equals(key) && !"".equals(value)) {
                    // This basically gets all statements that use the current predicate in the 0 position
                    predInstances = kb.ask("arg", 0, key);
                    for(int j=0; j < predInstances.size(); j++) {
                        sb = new StringBuilder();
                        displayText = String.copyValueOf(value.toCharArray());
                        f = predInstances.get(j);
                        arguments = f.complexArgumentsToArrayList(0);
                        sb.append(key);
                        sb.append(",");
                        // check if each of the arguments for the statements is to be replaced in its
                        // format statement.
                        for (int k = 1; k < arguments.size(); k++) {
                            argName = f.getStringArgument(k);
                            sTerm = (String) termMap.get(argName);
                            sTerm = StringUtil.removeEnclosingQuotes(sTerm);
                            argNum = "%" + String.valueOf(k);

                            // also, add the SUMO Concept that is replaced in the format
                            if (displayText.contains(argNum)) {
                                sb.append(argName);
                                sb.append(",");
                                displayText = displayText.replace(argNum, sTerm);
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
        catch (IOException e) {
            System.err.println(e.getMessage());
            e.printStackTrace();
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
            String argType1, argType2;
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
                    argType1 = kb.getArgType(term,1);
                    argType2 = kb.getArgType(term,2);
                    if (argType1 != null && argType2 != null) {
                        System.out.print("fof(local_" + counter++ + ",axiom,(s__" + term + "(s__Foo,s__Bar))).|");
                        System.out.print("fof(local_" + counter++ + ",axiom,(s__instance(s__Foo,s__" + argType1 + "))).|");
                        System.out.println("fof(local_" + counter++ + ",axiom,(s__instance(s__Bar,s__" + argType2 + "))).");
                    }
                }
            }
        }
        catch (Exception e) {
            System.err.println(e.getMessage());
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
            System.err.println(e.getMessage());
        }
    }

    /** *************************************************************
     */
    public static int getCountNonLinguisticAxioms(KB kb) {

        Set<String> rels = new HashSet<>();
        rels.add("documentation");
        rels.add("termFormat");
        rels.add("format");
        int counter = 0;
        Set<Formula> forms = new HashSet<>();
        forms.addAll(kb.formulaMap.values());
        for (Formula f : forms) {
            if (!rels.contains(f.getArgument(0).toString()))
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
        Pattern p;
        Matcher m;
        boolean b;
        String quoted;
        String[] ar;
        for (String s : kb.formulas.keySet()) {
            p = Pattern.compile("\"(.+)\"");
            m = p.matcher(s);
            b = m.find();
            if (b) {
                quoted = m.group(1);
                ar = quoted.split(" ");
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

        Set<Formula> result = new HashSet<>();
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

        StringBuilder sb = new StringBuilder();
        String doc;
        Set<Formula> allForms;
        for (String t : kb.terms) {
            doc = getDocumentation(kb,t);
            if (!StringUtil.emptyString(doc)) {
                allForms = getAllFormulasOfTerm(kb,t);
                sb.append("!!doc ").append(doc).append("\n");
                for (Formula f : allForms) {
                    if (!FormulaUtil.isDoc(f))
                        sb.append(f.toString()).append("\n");
                }
            }
        }
        return sb.toString();
    }

    /** *************************************************************
     * List all the terms and their termFormat expressions.
     */
    public static String termFormatIndex(KB kb) {

        StringBuilder sb = new StringBuilder();
        List<Formula> forms = kb.ask("arg", 0, "termFormat");
        String term, str;
        for (Formula f : forms) {
            term = f.getStringArgument(2);
            str = f.getStringArgument(3);
            if (!StringUtil.emptyString(term) && ! StringUtil.emptyString((str)))
                sb.append(term).append("\t").append(str).append("\n");
        }
        return sb.toString();
    }

    /** *************************************************************
     * Generate a textual list of all documentation strings and write to
     * a file.
     */
    public static void genAllDoc(KB kb, String fname) {

        List<Formula> al = kb.ask("arg",0,"documentation");
        try (PrintWriter pr = new PrintWriter(new FileWriter(fname, false))) {
            String arg;
            for (Formula form : al) {
                arg = form.getArgument(3).toString();
                arg = arg.replace("&%", "");
                pr.println(form.getArgument(1) + "\t" + arg);
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    /** *************************************************************
     * Generate a textual list of terms and their documentation strings
     * from a file.
     */
    public static void genDoc(KB kb, String fname) {

        System.out.println(genDocHeader(true));
        List<Formula> al = kb.ask("arg",0,"documentation");
        for (Formula form : al) {
            String arg;
            if (form.sourceFile.endsWith(fname)) {
                arg = form.getArgument(3).toString();
                arg = arg.replace("&%","");
                System.out.println(form.getArgument(1) + "\t" + arg);
            }
        }
    }

    /** *************************************************************
     */
    private static List<String> genAscii() {

        List<String> al = new ArrayList<>();
        for (int i = 65; i < 91; i++)
            al.add(Character.valueOf((char) i).toString());
        for (int i = 97; i < 123; i++)
            al.add(Character.valueOf((char) i).toString());
        return al;
    }

    /** *************************************************************
     */
    private static String genDocHeader(boolean onePage) {

        StringBuilder sb = new StringBuilder();
        sb.append("<h2>Data Dictionary</h2>");
        List<String> ascii = genAscii();
        if (onePage) {
            for (String s : ascii)
                sb.append("<a href=\"dict.html#").append(s).append("\">").append(s).append("</a>&nbsp;&nbsp;");
        }
        else
            for (String s : ascii) {
                sb.append("<a href=\"").append(s).append("dict.html\">").append(s).append("</a>&nbsp;&nbsp;");
        }
        sb.append("<P>\n");
        sb.append("<table><tr><th style:\"width:20%\"><b>Term</b></th><th style=\"width:75%\"><b>Doc</b></th></tr>\n");
        return sb.toString();
    }

    /** *************************************************************
     */
    private static Map<String,PrintWriter> genHTMLDocFiles(KB kb) {

        String head = genDocHeader(false);
        Map<String,PrintWriter> files = new TreeMap<>();
        List<String> ascii = genAscii();
        for (String s : ascii) {
            try (FileWriter fw = new FileWriter(s + "dict.html"); PrintWriter pw = new PrintWriter(fw)) {
                pw.println(head + "\n");
                files.put(s,pw);
            }
            catch (Exception e) {
                System.err.println(e.getMessage());
                e.printStackTrace();
            }
        }
        return files;
    }

    /** *************************************************************
     * collect all the documentation strings
     */
    private static Map<String,String> genDocList(KB kb) {

        List<Formula> al = kb.ask("arg",0,"documentation");
        Map<String,String> map = new TreeMap<>();
        String arg2;
        for (Formula form : al) {
            if (form.getArgument(2).toString().equals("EnglishLanguage")) {
                arg2 = form.getArgument(3).toString();
                //arg = arg.replace("&%","");
                String arg1 = form.getArgument(1).toString();
                arg2 = StringUtil.removeEnclosingQuotes(kb.formatStaticDocumentation(arg2, "EnglishLanguage", true));
                arg2 = arg2.replace("&term=", "");
                map.put(arg1, arg2);
            }
        }
        return map;
    }

    /** *************************************************************
     * flush and close all files
     */
    private static void closeDocList(Map<String,PrintWriter> files) {

        for (String term : files.keySet()) {
            try (PrintWriter pw = files.get(term)) {
                pw.println("</table>\n");
            }
        }
    }

    /** *************************************************************
     * Generate a HTML list of labels from termFormats
     * TODO: Does this work? Only returning an empty List
     */
    public static List<String> getLabelsForTerm(KB kb, String term, String lang) {

        List<String> result = new ArrayList<>();
        List<Formula> al = kb.askWithTwoRestrictions(0,"termFormat",1,lang,2,term);
        return result;
    }

    /** *************************************************************
     * flush and close all files
     * @param coreTerm just marks in the table whether the term is "core" or not.
     *                 This is useful only for specific applications
     * Show term, label, inCore, doc
     */
    private static String htmlForDoc(KB kb, String term, String lang, String doc, boolean noSUMO, boolean coreTerm) {

        List<String> labels = getLabelsForTerm(kb,term,lang);
        StringBuilder sb = new StringBuilder();
        sb.append("<a name=\"").append(term).append("\">").append(term).append("</a></td><td>");
        for (String s : labels)
            sb.append(s).append(", ");
        sb.delete(sb.length()-2,sb.length());
        sb.append("</td><td>");
        if (coreTerm)
            sb.append("yes");
        else
            sb.append("no");
        sb.append("</td><td>");
        sb.append(doc);
        if (!noSUMO) {
            sb.append("[and <a href=\"https://sigma.ontologyportal.org:8443/sigma/Browse.jsp?term=");
            sb.append(term).append("\">full SUMO definition</a>]");
        }
        sb.append("</td></tr>\n");
        return sb.toString();
    }

    /** *************************************************************
     * generate one documentation string entry
     */
    private static String genDocLine(boolean shade, KB kb, String term, String lang, String doc, boolean noSUMO, boolean coreTerm) {

        StringBuilder sb = new StringBuilder();
        if (shade)
            sb.append("<tr bgcolor=\"#ddd\"><td>");
        else
            sb.append("<tr><td>");
        sb.append(htmlForDoc(kb,term,lang,doc,noSUMO,coreTerm));
        return sb.toString();
    }

    /** *************************************************************
     * Generate a HTML list of terms and their documentation strings
     * from a file as upper and lowercase individual files so that each
     * file isn't gigantic
     */
    public static void genAllAlphaHTMLDoc(KB kb) {

        Map<String,String> map = genDocList(kb);
        Map<String,PrintWriter> files = genHTMLDocFiles(kb);
        boolean shade = false;
        String arg2;
        PrintWriter pw;
        for (String term : map.keySet()) {
            arg2 = map.get(term);
            pw = files.get(Character.toString(term.charAt(0)));
            pw.println(genDocLine(shade, kb, term, "EnglishLanguage", arg2,false,false));
            shade = ! shade;
        }
        closeDocList(files);
    }

    /** *************************************************************
     * Generate a HTML list of terms and their documentation strings
     * from a file.
     */
    public static void genAllHTMLDoc(KB kb) {

        System.out.println("<h2>Data Dictionary</h2>");
        System.out.println("<table><tr><th style:\"width:20%\"><b>Term</b></th><th style=\"width:75%\"><b>Doc</b></th></tr>\n");
        Map<String,String> map = genDocList(kb);
        boolean shade = false;
        String arg2;
        for (String term : map.keySet()) {
            arg2 = map.get(term);
            if (shade)
                System.out.println("<tr bgcolor=\"#ddd\"><td>");
            else
                System.out.println("<tr><td>");
            // (KB kb, String term, String lang, String doc, boolean noSUMO, boolean coreTerm)
            System.out.println(htmlForDoc(kb,term,"EnglishLanguage",arg2,false,false));
            shade = !shade;
        }
        System.out.println("</table>\n");
    }

    /** *************************************************************
     */
    public static List<String> getLinkedTermsInDoc(KB kb, String term) {

        List<String> result = new ArrayList<>();
        return result;
    }

    /** *************************************************************
     * Generate a HTML list of terms and their documentation strings
     * from a file.
     */
    public static void genSpecificTermDoc(KB kb, String file, String lang) {

        List<String> lines = FileUtil.readLines(file);
        List<String> aux = new ArrayList<>();
        genDocHeader(true); // true = one page
        System.out.println("<h2>Data Dictionary</h2>");
        System.out.println("<table><tr><th style:\"width:20%\"><b>Term</b></th><th><b>label</b></th><th><b>in List or Aux</b></th><th style=\"width:75%\"><b>Doc</b></th></tr>\n");
        Map<String,String> map = genDocList(kb);
        boolean shade = false;
        List<String> links;
        String arg2;
        for (String term : map.keySet()) {
            if (!lines.contains(term))
                continue;
            links = getLinkedTermsInDoc(kb,term); // empty list if none
            aux.addAll(links);
            arg2 = map.get(term);
            if (shade)
                System.out.println("<tr bgcolor=\"#ddd\"><td>");
            else
                System.out.println("<tr><td>");
            System.out.println(htmlForDoc(kb,term,"EnglishLanguage",arg2,false,true));
            shade = !shade;
        }
        System.out.println("</table>\n");
    }

    /** ***************************************************************
     * utility method to merge two HashMaps of String keys and a values
     * of an HashSet of Strings.  Note that parent classes in the set of
     * classes will be removed
     */
    public static Map<String, Set<String>> mergeToMap(Map<String, Set<String>> map1,
                                                              Map<String, Set<String>> map2, KB kb) {

        Map<String, Set<String>> result = new HashMap<>(map1);
        Set<String> value;
        for (String key : map2.keySet()) {
            value = new HashSet<>();
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
     * Must be called whenever a *.tptp, *.tff or *.fof file is written
     * to allow for clean shutdown of the JVM. Call this at the bottom
     * of any main class where the executor is invoked.
     */
    public static void shutDownExecutorService() {

        EXECUTOR_SERVICE.shutdown();
        try {
            if (!EXECUTOR_SERVICE.awaitTermination(10, TimeUnit.SECONDS)) {
                EXECUTOR_SERVICE.shutdownNow();
            }
        } catch (InterruptedException e) {
            EXECUTOR_SERVICE.shutdownNow();
        }
        System.out.println("KButilities.shutDownExecutorService(): ExecutorService shutdown");
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
        System.out.println("  -r - generate semantic network as |-delimited triples");
        System.out.println("  -n - generate NL for every formula");
        System.out.println("  -f - list formulas for every documentation string term");
        System.out.println("  -v - is formula valid");
        System.out.println("  -a \"<formula>\" - show all attributes of a SUO-KIF formula");
        System.out.println("  -t - generate a table of termFormat(s)");
        System.out.println("  -l - list all terms in the KB");
        System.out.println("  -doc <file> - list doc strings for all terms to a file");
        System.out.println("  -odoc <file> - list terms with doc strings for all terms in a file one per line");
        System.out.println("  -adoc - list terms with doc strings for all terms in a KB");
    }

    /** *************************************************************
     */
    public static void main(String[] args) {

        if (args != null && args.length > 0 && args[0].equals("-h"))
            showHelp();
        else {
            KBmanager.getMgr().initializeOnce();
            KB kb = KBmanager.getMgr().getKB(KBmanager.getMgr().getPref("sumokbname"));
            System.out.println("KButilities.main(): completed init");
            //countRelations(kb);
            //checkURLs(kb);
            //validatePictureList();
            //for (String s : generateSemanticNetwork(kb))
            //    System.out.println(s);
            KButilities kbu = new KButilities();
//            Infrastructure infra = new Infrastructure();
            if (args != null && args.length > 1 && args[0].equals("-c")) {
                genSynLinks(args[1]);
            }
            else if (args != null && args.length > 1 && args[0].equals("-doc")) {
                System.out.println("KBmutilities.main(): writing all documentation string");
                genAllDoc(kb,args[1]);
            }
            else if (args != null && args.length > 0 && args[0].equals("-adoc")) {
                System.out.println("KBmutilities.main(): writing documentation");
                genAllAlphaHTMLDoc(kb);
            }
            else if (args != null && args.length > 1 && args[0].equals("-odoc")) {
                System.out.println("KBmutilities.main(): writing documentation");
                genSpecificTermDoc(kb,args[1],args[2]);
            }
            else if (args != null && args.length > 0 && args[0].equals("-l")) {
                for (String t : kb.terms)
                    System.out.println(t);
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
                boolean valid = isValidFormula(kb,args[1]);
                String s = "Formula " + args[1] + "\nis valid: ";
                StringBuilder sb = new StringBuilder();
                sb.append(s);
                if (valid)
                    System.out.println(sb.append(valid));
                else
                    System.err.println(sb.append(valid));
            }
            else if (args != null && args.length > 1 && args[0].equals("-a")) {
                SUMOtoTFAform.initOnce();
                Formula f = new Formula(StringUtil.removeEnclosingQuotes(args[1]));
                Formula.debug = true;
                System.out.println("higherOrder : " + f.isHigherOrder(kb));
                Formula.debug = false;
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

