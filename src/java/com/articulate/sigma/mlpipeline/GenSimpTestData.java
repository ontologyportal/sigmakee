package com.articulate.sigma.mlpipeline;

import com.articulate.sigma.*;
import com.articulate.sigma.nlg.LanguageFormatter;
import com.articulate.sigma.nlg.NLGUtils;
import com.articulate.sigma.utils.AVPair;
import com.articulate.sigma.utils.StringUtil;

import java.io.*;
import java.util.*;

public class GenSimpTestData {

    public static boolean debug = false;
    public static KB kb;
    public static Random rand = new Random();
    public static boolean skip = false;
    public static HashSet<String> skipTypes = new HashSet<>();
    public static final int instLimit = 500;
    public static PrintWriter pw = null;
    public static final int loopMax = 2;

    /** ***************************************************************
     * handle the case where the argument type is a subclass
     */
    public static String handleClass(String t, HashMap<String, ArrayList<String>> instMap) {

        String arg = "";
        String bareClass = t.substring(0, t.length() - 1);
        if (debug) System.out.println("handleClass(): bareClass: " + bareClass);
        if (bareClass.equals("Class"))
            skip = true;
        else {
            HashSet<String> children = kb.kbCache.getChildClasses(bareClass);
            ArrayList<String> cs = new ArrayList<>();
            cs.addAll(children);
            if (children == null || children.size() == 0)
                skip = true;
            else {
                int rint = rand.nextInt(cs.size());
                arg = cs.get(rint);
            }
        }
        return arg;
    }

    /** ***************************************************************
     * generate new SUMO statements for relations using the set of
     * available instances for each argument type and output English
     * paraphrase
     */
    public static ArrayList<Formula> genFormulas(String rel, ArrayList<String> sig,
                                                 HashMap<String, ArrayList<String>> instMap) {

        ArrayList<StringBuffer> forms = new ArrayList<>();
        for (int i = 1; i < sig.size(); i++) {
            String currT = sig.get(i);
            if (currT.endsWith("+"))
                return new ArrayList<Formula>(); // bail out if there is a subclass argument
        }

        StringBuffer form = new StringBuffer();
        form.append("(" + rel + " ");
        forms.add(form);

        for (int i = 1; i < sig.size(); i++) {
            ArrayList<StringBuffer> newforms = new ArrayList<>();
            String currT = sig.get(i);
            if (debug) System.out.println("genFormula() currT: " + currT);
            if (instMap.get(currT) == null || instMap.get(currT).size() < 1)
                return new ArrayList<Formula>();
            int max = instMap.get(currT).size();
            if (max > instLimit)
                max = instLimit;
            for (int j = 0; j < max; j++) {
                String arg = "";
                arg = instMap.get(currT).get(j);
                for (StringBuffer sb : forms) {
                    StringBuffer f = new StringBuffer(sb);
                    f.append(arg + " ");
                    newforms.add(f);
                }
            }
            forms = newforms;
        }

        ArrayList<Formula> formsList = new ArrayList<>();
        for (StringBuffer sb : forms) {
            sb.deleteCharAt(sb.length() - 1);
            sb.append(")");
            Formula f = new Formula(sb.toString());
            formsList.add(f);
        }
        return formsList;
    }

    /** ***************************************************************
     * handle quantities
     */
    public static void handleQuantity(String t, HashMap<String, ArrayList<String>> instMap) {

        TreeSet<String> instances = kb.getAllInstances(t);
        if (instances.size() < 1) {
            if (debug) System.out.println("handleQuantity(): no instances for " + t);
            return;
        }
        ArrayList<String> arInsts = new ArrayList<>();
        arInsts.addAll(instances);
        int rint = rand.nextInt(instances.size());
        String inst = arInsts.get(rint); // get an instance of a quantity
        float num = rand.nextFloat() * 100;
        String f = "(MeasureFn " + num + " " + inst + ")";
        if (instMap.containsKey(t)) {
            ArrayList<String> insts = instMap.get(t);
            insts.add(f);
        }
        else {
            ArrayList<String> insts = new ArrayList<>();
            insts.add(f);
            instMap.put(t,insts);
        }
    }

    /** ***************************************************************
     * handle the case where the argument type is not a subclass
     */
    public static void handleNonClass(String t, HashMap<String, ArrayList<String>> instMap) {

        if (debug) System.out.println("handleNonClass(): t: " + t);
        HashSet<String> hinsts = kb.kbCache.getInstancesForType(t);
        if (hinsts.contains("statementPeriod"))
            if (debug) System.out.println("handleNonClass(): hinsts: " + hinsts);
        ArrayList<String> insts = new ArrayList<>();
        insts.addAll(hinsts);
        if (debug) System.out.println("handleNonClass(): insts: " + insts);
        if (insts.size() > 0) {
            if (instMap.containsKey(t)) {
                ArrayList<String> oldinsts = instMap.get(t);
                oldinsts.addAll(insts);
            }
            else
                instMap.put(t,insts);
        }
        else {
            String term = t + "1";
            if (debug) System.out.println("handleNonClass(2): t: " + t);
            String lang = "EnglishLanguage";
            insts.add(term);
            if (debug) System.out.println("handleNonClass(): insts(2): " + insts);
            //System.out.println("handleNonClass(): term format size: " + kb.getTermFormatMap(lang).keySet().size());
            //System.out.println("handleNonClass(): containsKey: " + kb.getTermFormatMap(lang).containsKey(t));
            //System.out.println("handleNonClass(): termFormat: " + kb.getTermFormatMap(lang).get(t));
            String fString = "a " + kb.getTermFormatMap(lang).get(t); // kb.getTermFormat(lang,t);
            String form = "(termFormat EnglishLanguage " + term + " \"" + fString + "\")";
            HashMap<String, String> langTermFormatMap = kb.getTermFormatMap(lang);
            langTermFormatMap.put(term, fString);
            //System.out.println(form);
            kb.tell(form);
            instMap.put(t, insts);
        }
        if (debug) System.out.println("handleNonClass(): instMap: " + instMap);
    }

    /** ***************************************************************
     * generate new SUMO statements for relations and output English
     * paraphrase
     */
    public static String toEnglish(String form) {

        return NLGUtils.htmlParaphrase("", form, kb.getFormatMap("EnglishLanguage"),
                kb.getTermFormatMap("EnglishLanguage"), kb, "EnglishLanguage") + "\n";
    }

    /** ***************************************************************
     * generate new SUMO termFormat statements for constants in a file
     */
    public static void genTermFormatFromNames(String fname) {

        try {
            File constituent = new File(fname);
            String canonicalPath = constituent.getCanonicalPath();
            KIF kif = new KIF(canonicalPath);
            kif.setParseMode(KIF.RELAXED_PARSE_MODE);
            kif.readFile(fname);
            for (Formula f : kif.formulaMap.values()) {
                if (f.car().equals("instance")) {
                    String t = f.getStringArgument(1);
                    String s = StringUtil.camelCaseToSep(t);
                    System.out.println("(termFormat EnglishLanguage " + t + " \"" + s + "\")");
                }
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    /** ***************************************************************
     * generate new SUMO termFormat and instance statements for names
     */
    public static Collection<AVPair> readHumans() {

        Collection<AVPair> result = new HashSet<>();
        ArrayList<ArrayList<String>> fn = DB.readSpreadsheet("/home/apease/workspace/sumo" +
                File.separator + "WordNetMappings/FirstNames.csv", null, false, ',');
        for (ArrayList<String> ar : fn) {
            String firstName = ar.get(0);
            String g = ar.get(1);
            AVPair avp = new AVPair(firstName, g);
            result.add(avp);
        }
        return result;
    }

    /** ***************************************************************
     * generate new SUMO termFormat and instance statements for names
     */
    public static void genHumans() {

        Collection<AVPair> avps = readHumans();
        for (AVPair avp : avps) {
            String firstName = avp.attribute;
            String g = avp.value;
            if (firstName != null) {
                String gender = "Male";
                if (g.toUpperCase().equals("F"))
                    gender = "Female";
                System.out.println("(instance " + firstName + " Human)");
                System.out.println("(attribute " + firstName + " " + gender + ")");
                System.out.println("(names \"" + firstName + "\" " + firstName + ")");
            }
        }
    }

    /** ***************************************************************
     * generate missing SUMO termFormat statements
     */
    public static void genMissingTermFormats() {

        System.out.println("GenSimpTestData.genMissingTermFormats(): start");
        KBmanager.getMgr().initializeOnce();
        kb = KBmanager.getMgr().getKB(KBmanager.getMgr().getPref("sumokbname"));
        for (String s : kb.terms) {
            ArrayList<Formula> res = kb.askWithRestriction(0,"termFormat",2,s);
            if (res == null || res.size() == 0) {
                boolean inst = false;
                if (kb.isInstance(s))
                    inst = true;
                String news = StringUtil.camelCaseToSep(s,true,inst);
                System.out.println("(termFormat EnglishLanguage " + s + " \"" + news + "\")");
            }
        }
    }
    /** ***************************************************************
     * generate new SUMO statements for relations and output English
     * paraphrase
     */
    public static void genStatements(HashMap<String, String> formatMap) {

        for (String rel : kb.kbCache.relations) {
            skip = false;
            if (formatMap.get(rel) != null && !kb.isFunction(rel)) {
                boolean skip = false;
                if (debug) System.out.println("genStatements()  rel: " + rel);
                ArrayList<String> sig = kb.kbCache.getSignature(rel);
                HashMap<String, ArrayList<String>> instMap = new HashMap<>();
                if (debug) System.out.println("sig: " + sig);
                for (String t : sig) {
                    if (skipTypes.contains(t))
                        skip = true;
                    if (StringUtil.emptyString(t) || skipTypes.contains(t))
                        continue;
                    if (debug) System.out.println("genStatements() t: " + t);
                    if (!t.endsWith("+") && !kb.isSubclass(t,"Quantity")) {
                        handleNonClass(t,instMap);
                    }
                    else if (kb.isSubclass(t,"Quantity")) {
                        if (debug) System.out.println("genStatements(): found quantity for : " + rel);
                        handleQuantity(t, instMap);
                    }
                }
                if (!skip) {
                    ArrayList<Formula> forms = genFormulas(rel,sig,instMap);
                    for (Formula f : forms) {
                        String form = f.getFormula();
                        if (!StringUtil.emptyString(form)) {
                            pw.println(form);
                            String actual = toEnglish(form);
                            pw.println(StringUtil.filterHtml(actual));
                        }
                    }
                }
            }
        }
    }

    /** ***************************************************************
     * generate English for all ground relation statements
     */
    public static void handleGroundStatements(HashMap<String, String> formatMap ) {

        HashSet<Formula> forms = new HashSet<>();
        forms.addAll(kb.formulaMap.values());
        for (Formula f : forms) {
            if (f.isGround() && formatMap.containsKey(f.relation) && !StringUtil.emptyString(f.toString()))
                System.out.println(toEnglish(f.toString()));
        }
    }

    /** ***************************************************************
     * init and call main routine.
     */
    public static void generate() {

        System.out.println("GenSimpTestData.generate()");
        KBmanager.getMgr().initializeOnce();
        //resultLimit = 0; // don't limit number of results on command line
        kb = KBmanager.getMgr().getKB(KBmanager.getMgr().getPref("sumokbname"));
        System.out.println("generate(): # relations: " + kb.kbCache.relations.size());
        HashMap<String, String> formatMap = kb.getFormatMap("EnglishLanguage");
        skipTypes.addAll(Arrays.asList("Formula") );
        handleGroundStatements(formatMap);
        genStatements(formatMap);
    }

    /** ***************************************************************
     * print all SUMO axioms in the current knowledge base along with
     * their natural language paraphrases
     */
    public static void allAxioms() {

        System.out.println("GenSimpTestData.allAxioms()");
        KBmanager.getMgr().initializeOnce();
        kb = KBmanager.getMgr().getKB(KBmanager.getMgr().getPref("sumokbname"));
        for (Formula f : kb.formulaMap.values()) {
            String form = f.getFormula();
            if (!StringUtil.emptyString(form) && !form.contains("\"") &&
                    !Formula.DOC_PREDICATES.contains(f.car())) {
                pw.println(form.replace("\n", "").replace("\r", ""));
                String actual = toEnglish(form);
                pw.println(StringUtil.filterHtml(actual));
            }
        }
    }

    /** ***************************************************************
     */
    public static void testNLG() {

        System.out.println("GenSimpTestData.allAxioms()");
        KBmanager.getMgr().initializeOnce();
        kb = KBmanager.getMgr().getKB(KBmanager.getMgr().getPref("sumokbname"));
        String s = "(=> (and (valence ?REL ?NUMBER) (instance ?REL Predicate)) (forall (@ROW) (=> (?REL @ROW) (equal (ListLengthFn (ListFn @ROW)) ?NUMBER))))";
        String actual = toEnglish(s);
        System.out.println("Strike:" + kb.getTermFormat("EnglishLanguage","BaseballStrike"));
        System.out.println("Strike2:" + kb.getTermFormatMap("EnglishLanguage").get("BaseballStrike"));
        System.out.println("Strike3:" + LanguageFormatter.translateWord(kb.getTermFormatMap("EnglishLanguage"),"BaseballStrike"));
        System.out.println(actual);
    }

    /** ***************************************************************
     */
    public static void testTypes() {

        System.out.println("GenSimpTestData.testTypes()");
        KBmanager.getMgr().initializeOnce();
        kb = KBmanager.getMgr().getKB(KBmanager.getMgr().getPref("sumokbname"));
        String t = "Object";
        HashSet<String> hinsts = kb.kbCache.getInstancesForType(t);
        System.out.println(hinsts);
        System.out.println("Object has instance BinaryPredicate: " + hinsts.contains("BinaryPredicate"));
        System.out.println("signature of 'half': " + kb.kbCache.getSignature("half"));
    }

    /** ***************************************************************
     */
    public class Preposition {
        public String procType = null;
        public String prep = null;
        public String noun = null;
    }

    /** ***************************************************************
     * @return objects
     */
    public void addArguments(Collection<String> col, boolean dob, boolean incPrep,
                             HashSet<Preposition> objects) {

        for (String s : col) {
            Preposition p = this.new Preposition();
            p.prep = "";
            p.noun = s;
            objects.add(p);
        }
    }

    /** ***************************************************************
     * Create action sentences from a subject, preposition, direct object,
     * preposition and indirect object.  Indirect object and its preposition
     * can be left out.  Actions will eventually be past and future tense or
     * wrapped in modals.
     */
    public void initActions() {

        System.out.println("GenSimpTestData.initActions(): start");
        KBmanager.getMgr().initializeOnce();
        kb = KBmanager.getMgr().getKB(KBmanager.getMgr().getPref("sumokbname"));
        System.out.println("GenSimpTestData.initActions(): finished loading KBs");
        //artifact, organic object
        TreeSet<String> socRole = kb.getAllInstances("SocialRole");
        TreeSet<String> artInst = kb.getAllInstances("Artifact");
        HashSet<String> artClass = kb.kbCache.getChildClasses("Artifact");
        HashSet<String> intProc = kb.kbCache.getChildClasses("DualObjectProcess");
        TreeSet<String> orgInst = kb.getAllInstances("OrganicObject");
        HashSet<String> orgClass = kb.kbCache.getChildClasses("OrganicObject");
        HashSet<Preposition> objects = new HashSet<>();
        addArguments(artInst, false, false, objects);
        addArguments(artClass, false, false, objects);
        addArguments(orgInst, false, false, objects);
        addArguments(orgClass, false, false, objects);
        HashSet<Preposition> indirect = new HashSet<>();
        //  get capabilities from axioms like
        //  (=> (instance ?GUN Gun) (capability Shooting instrument ?GUN))
        ArrayList<Formula> forms = kb.ask("cons",0,"capability");
        for (Formula f : forms) {
            String ant = FormulaUtil.antecedent(f);
            Formula fant = new Formula(ant);
            if (fant.isSimpleClause(kb) && fant.car().equals("instance")) {
                String antClass = fant.getStringArgument(2);
                String cons = FormulaUtil.consequent(f);
                Formula fcons = new Formula(cons);
                if (fcons.isSimpleClause(kb)) {
                    String consClass = fcons.getStringArgument(1);
                    String rel = fcons.getStringArgument(2);
                    Preposition p = this.new Preposition();
                    p.procType = consClass;
                    p.prep = rel;
                    p.noun = antClass;
                    indirect.add(p);
                }
            }
        }
        System.out.println("GenSimpTestData.initActions() finished collecting terms");
        Collection<AVPair> humans = readHumans();
        genActions(humans,socRole,intProc,objects,indirect);
    }

    /** ***************************************************************
     * also return true if there's no termFormat for the process
     */
    public boolean compoundVerb(String term) {

        String word = kb.getTermFormat("EnglishLanguage",term);
        if (word == null || word.contains(" ")) {
            System.out.println("compoundVerb(): or null: " + word + " " + term);
            return true;
        }
        return false;
    }

    /** ***************************************************************
     */
    public String verbForm(String term) {

        String word = kb.getTermFormat("EnglishLanguage",term);
        if (word == null) {
            System.out.println("verbForm(): no term format for " + term);
            return null;
        }
        if (word.endsWith("ing"))
            return "is " + word;
        return word + "s";
    }

    /** ***************************************************************
     */
    public String nounForm(String term) {

        String word = kb.getTermFormat("EnglishLanguage",term);
        boolean subst = kb.isSubclass(term,"Substance");
        if (word == null) {
            System.out.println("nounForm(): no term format for " + term);
            return null;
        }
        if (kb.isInstance(term))
            return "the " + word;
        if (subst)
            return "some " + word;
        if (word.matches("^[aeiouh].*"))
            return "an " + word;
        return "a " + word;
    }

    /** ***************************************************************
     * Create action sentences from a subject, preposition, direct object,
     * preposition and indirect object.  Indirect object and its preposition
     * can be left out.  Actions will eventually be past and future tense or
     * wrapped in modals.
     */
    public void genActions(Collection<AVPair> humans,
                           Collection<String> roles,
                           Collection<String> intProc,
                           Collection<Preposition> direct,
                           Collection<Preposition> indirect) {

        System.out.println("GenSimpTestData.genActions()");
        int humCount = 0;
        for (AVPair avp : humans) {
            if (humCount++ > loopMax) break;
            int procCount = 0;
            for (String proc : intProc) {
                if (compoundVerb(proc)) { continue; } // too hard grammatically for now to have compound verbs
                if (procCount++ > loopMax) break;
                int directCount = 0;
                for (Preposition dprep : direct) {
                    if (directCount++ > loopMax) break;
                    int indCount = 0;
                    boolean onceWithoutInd = false;
                    for (Preposition indprep : indirect) {
                        if (indCount++ > loopMax) break;
                        StringBuffer english = new StringBuffer();
                        StringBuffer prop = new StringBuffer();
                        english.append(avp.attribute + " ");
                        prop.append("(exists (?H ?P ?DO ?IO) (and ");
                        prop.append("(instance ?H Human) ");
                        prop.append("(names \"" + avp.attribute + "\" ?H) ");
                        english.append(verbForm(proc) + " ");
                        prop.append("(instance ?P " + proc + ") (agent ?P ?H) ");
                        english.append(nounForm(dprep.noun) + " ");
                        prop.append("(instance ?DO " + dprep.noun + ") (patient ?P ?DO) ");
                        if (kb.isSubclass(proc,indprep.procType)) {
                            english.append("with " + nounForm(indprep.noun));
                            prop.append("(instance ?IO " + indprep.noun + ") (instrument ?P ?IO)))");
                            onceWithoutInd = false;
                        }
                        else {
                            prop.append("))");
                            indCount = 0;
                            onceWithoutInd = true;
                            if (!onceWithoutInd) {
                                System.out.println(english.toString());
                                System.out.println(prop.toString() + "\n");
                                onceWithoutInd = true;
                            }
                        }
                        if (!onceWithoutInd) {
                            System.out.println(english.toString());
                            System.out.println(prop.toString() + "\n");
                        }
                    }
                }
            }
        }
    }

    /** ***************************************************************
     * init and call main routine.
     */
    public static void main(String args[]) {

        try {
            FileWriter fw = new FileWriter("out.txt");
            pw = new PrintWriter(fw);
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        //testTypes();
        //generate();
        //allAxioms();
        GenSimpTestData gstd = new GenSimpTestData();
        gstd.initActions();
        //genMissingTermFormats();
        //testNLG();
        //genTermFormatFromNames("/home/apease/workspace/sumo/WorldAirports.kif");
        //genHumans();
    }
}
