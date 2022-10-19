package com.articulate.sigma.mlpipeline;

import com.articulate.sigma.*;
import com.articulate.sigma.nlg.LanguageFormatter;
import com.articulate.sigma.nlg.NLGUtils;
import com.articulate.sigma.utils.AVPair;
import com.articulate.sigma.utils.StringUtil;
import com.articulate.sigma.wordNet.WordNet;
import com.articulate.sigma.wordNet.WordNetUtilities;

import java.io.*;
import java.util.*;

/** ***************************************************************
 * This code generates language-logic pairs designed for training
 * a machine learning system.  Several approaches are used
 * - instantiate relations with arguments of appropriate types
 *   and then generate NL paraphrases from them
 * - run through all formulas in SUMO and generate NL paraphrases
 * - build up sentences and logic expressions compositionally
 *
 * The compositional generation is potentially the most comprehensive.
 * It consists of building ever more complex statements that wrap or
 * extend simpler statements.  Currently, this means starting with
 * a simple subject-verb-object construction and adding:
 * - indirect objects
 * - tenses for the verbs
 * - modals
 *
 */

public class GenSimpTestData {

    public static boolean debug = false;
    public static KB kb;
    public static Random rand = new Random();
    public static boolean skip = false;
    public static HashSet<String> skipTypes = new HashSet<>();
    public static final int instLimit = 500;
    public static PrintWriter pw = null;
    public static final int loopMax = 10;
    public static final int NOTIME = -1;
    public static final int PAST = 0;
    public static final int PRESENT = 1;
    public static final int FUTURE = 2;

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
     * Information about a process
     */
    public class ProcInfo {
        public String term = null;
        public String synset = null; // the synset equivalent to the SUMO term
        public ArrayList<String> words = new ArrayList<>();
        public ArrayList<String> trans = new ArrayList<>(); // transitivity of the word
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
     */
    public ArrayList<AVPair> initModals() {

        ArrayList<AVPair> modals = new ArrayList<>();
        modals.add(new AVPair("None",""));
        modals.add(new AVPair("Necessity","it is necessary that "));
        modals.add(new AVPair("Possibility","it is possible that "));
        modals.add(new AVPair("Obligation","it is obligatory that "));
        modals.add(new AVPair("Permission","it is permitted that "));
        modals.add(new AVPair("Prohibition","it is prohibited that "));
        modals.add(new AVPair("Likely","it is likely that "));
        modals.add(new AVPair("Unlikely","it is unlikely that "));
        return modals;
    }

    /** ***************************************************************
     * @return objects
     */
    public HashSet<Preposition> collectCapabilities() {

        HashSet<Preposition> indirect = new HashSet<>();
        ArrayList<Formula> forms = kb.ask("cons",0,"capability");
        for (Formula f : forms) {
            String ant = FormulaUtil.antecedent(f);
            Formula fant = new Formula(ant);
            if (fant.isSimpleClause(kb) && fant.car().equals("instance")) {
                String antClass = fant.getStringArgument(2); // the thing that plays a role
                String cons = FormulaUtil.consequent(f);
                Formula fcons = new Formula(cons);
                if (fcons.isSimpleClause(kb)) {
                    String consClass = fcons.getStringArgument(1);  // the process type
                    String rel = fcons.getStringArgument(2);  // the role it plays
                    Preposition p = this.new Preposition();
                    p.procType = consClass;
                    p.prep = rel;
                    p.noun = antClass;
                    indirect.add(p);
                }
            }
        }
        return indirect;
    }

    /** ***************************************************************
     * Create action sentences from a subject, preposition, direct object,
     * preposition and indirect object.  Indirect object and its preposition
     * can be left out.  Actions will eventually be past and future tense or
     * wrapped in modals.
     */
    public void initActions(PrintWriter pw) {

        System.out.println("GenSimpTestData.initActions(): start");
        KBmanager.getMgr().initializeOnce();
        kb = KBmanager.getMgr().getKB(KBmanager.getMgr().getPref("sumokbname"));
        System.out.println("GenSimpTestData.initActions(): finished loading KBs");
        //artifact, organic object
        HashSet<String> socRoles = kb.kbCache.getInstancesForType("SocialRole");
        //System.out.println("initActions(): roles: " + socRole);
        HashSet<String> artInst = kb.kbCache.getInstancesForType("Artifact");
        HashSet<String> artClass = kb.kbCache.getChildClasses("Artifact");
        HashSet<String> intProc = kb.kbCache.getChildClasses("DualObjectProcess");
        HashSet<String> orgInst = kb.kbCache.getInstancesForType("OrganicObject");
        HashSet<String> orgClass = kb.kbCache.getChildClasses("OrganicObject");
        HashSet<Preposition> objects = new HashSet<>();
        addArguments(artInst, false, false, objects);
        addArguments(artClass, false, false, objects);
        addArguments(orgInst, false, false, objects);
        addArguments(orgClass, false, false, objects);
        HashSet<Preposition> indirect = new HashSet<>();
        //  get capabilities from axioms like
        //  (=> (instance ?GUN Gun) (capability Shooting instrument ?GUN))
        indirect = collectCapabilities();
        System.out.println("GenSimpTestData.initActions() finished collecting terms");
        Collection<AVPair> humans = readHumans();
        ArrayList<AVPair> modals = initModals();
        genWithModals(pw,modals,humans,socRoles,intProc,objects,indirect);
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
    public String verbForm(String term, int time) {

        String word = kb.getTermFormat("EnglishLanguage",term);
        if (word == null) {
            System.out.println("verbForm(): no term format for " + term);
            return null;
        }
        if (word.endsWith("ing")) {
            if (time == PAST)
                return "was " + word;
            if (time == PRESENT)
                return "is " + word;
            if (time == FUTURE)
                return "will be " + word;
        }
        String root =  WordNet.wn.verbRootForm(word,word.toLowerCase());
        if (root == null)
            root = word;
        if (time == PAST) {
            if (root.endsWith("e"))
                return "was " + root.substring(0,root.length()-1) + "ing";
            else
                return "was " + root + "ing";
        }
        if (time == PRESENT) {
            if (root.endsWith("y"))
                return root.substring(0,root.length()-1) + "ies";
            return root + "s";
        }
        if (time == FUTURE)
            return "will " + root;
        if (root.endsWith("y"))
            return root.substring(0,root.length()-1) + "ies";
        return root + "s";
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
        if (kb.isInstance(term)) {
            if (kb.kbCache.isInstanceOf(term,"Human"))
                return word;
            else
                return "the " + word;
        }
        if (subst)
            return "some " + word;
        if (word.matches("^[aeiouh].*"))
            return "an " + word;
        return "a " + word;
    }

    /** ***************************************************************
     */
    public void generateSubject(StringBuffer english, StringBuffer prop,
                                  AVPair subj) {

        if (kb.isInstanceOf(subj.attribute,"SocialRole")) { // a plumber... etc
            english.append(nounForm(subj.attribute) + " ");
            prop.append("(attribute ?H " + subj.attribute + ") ");
        }
        else {                                              // John... etc
            english.append(subj.attribute + " ");
            prop.append("(instance ?H Human) ");
            prop.append("(names \"" + subj.attribute + "\" ?H) ");
        }
    }

    /** ***************************************************************
     */
    public void generateVerb(StringBuffer english, StringBuffer prop,
                                String proc, int time) {

        english.append(verbForm(proc,time) + " ");
        prop.append("(instance ?P " + proc + ") (agent ?P ?H) ");
        // if (time == -1) do nothing extra
        if (time == PAST)
            prop.append("(before (EndFn (WhenFn ?P)) Now) ");
        if (time == PRESENT)
            prop.append("(temporallyBetween (BeginFn (WhenFn ?P)) Now (EndFn (WhenFn ?P))) ");
        if (time == FUTURE)
            prop.append("(before Now (BeginFn (WhenFn ?P))) ");
    }

    /** ***************************************************************
     */
    public void generateDirectObject(StringBuffer english, StringBuffer prop,
                             Preposition dprep) {

        english.append(nounForm(dprep.noun) + " ");
        prop.append("(instance ?DO " + dprep.noun + ") (patient ?P ?DO) ");
    }

    /** ***************************************************************
     */
    public ProcInfo findProcInfo(String proc) {

        ProcInfo result = this.new ProcInfo();
        ArrayList<String> synsets = WordNetUtilities.getEquivalentSynsetsFromSUMO(proc);
        if (synsets == null || synsets.size() == 0)
            return null;
        if (synsets.size() > 1)
            System.out.println("GenSimpTestData.findProcInfo(): more than one synset " + synsets + " for " + proc);
        result.synset = synsets.get(0);
        result.words = WordNet.wn.getWordsFromSynset(result.synset);
        for (String w : result.words) {
            String trans = WordNet.wn.getTransitivity(result.synset,w);
            result.trans.add(trans);
        }
        return result;
    }

    /** ***************************************************************
     */
    public boolean generateIndirectObject(PrintWriter pw, int indCount,
                                       StringBuffer english, StringBuffer prop,
                                       AVPair modal,
                                       String proc, Preposition indprep,
                                       boolean onceWithoutInd) {

        if (debug) System.out.println("generateIndirectObject(): proc, procType: " + proc + ", " + indprep.procType);
        if (kb.isSubclass(proc,indprep.procType)) {
            english.append("with " + nounForm(indprep.noun));
            prop.append("(instance ?IO " + indprep.noun + ") (" + indprep.prep + " ?P ?IO)))");
            if (!modal.attribute.equals("None"))
                prop.append(" " + modal.attribute + ")");
            onceWithoutInd = false;
            pw.println(english.toString());
            pw.println(prop.toString() + "\n");
        }
        else {
            prop.append("))");
            if (!modal.attribute.equals("None"))
                prop.append(" " + modal.attribute + ")");
            indCount = 0;
            if (!onceWithoutInd) {
                pw.println(english.toString());
                pw.println(prop.toString() + "\n");
            }
            onceWithoutInd = true;
        }
        return onceWithoutInd;
    }

    /** ***************************************************************
     * Create action sentences from a subject, preposition, direct object,
     * preposition and indirect object.  Indirect object and its preposition
     * can be left out.  Actions will eventually be past and future tense or
     * wrapped in modals.
     */
    public void genProc(PrintWriter pw,
                        StringBuffer english,
                        StringBuffer prop,
                        AVPair modal,
                        AVPair subj,
                        Collection<String> intProc,
                        Collection<Preposition> direct,
                        Collection<Preposition> indirect) {

        int procCount = 0;
        for (String proc : intProc) {
            ProcInfo pi = findProcInfo(proc);
            if (pi == null) {
                pi = new ProcInfo();
                pi.term = proc;
                pi.words.add(kb.getTermFormat("EnglishLanguage",proc));
            }
            if (compoundVerb(proc)) { continue; } // too hard grammatically for now to have compound verbs
            if (debug) System.out.println("genProc(): proc: " + proc);
            if (procCount++ > loopMax) break;
            for (int time = -1; time < 3; time++) { // -1 = no time spec, 0=past, 1=present, 2=future
                int directCount = 0;
                for (Preposition dprep : direct) {
                    if (directCount++ > loopMax) break;
                    if (debug) System.out.println("genProc(): proc: " + proc + " direct: " + dprep.noun);
                    int indCount = 0;
                    boolean onceWithoutInd = false;
                    for (Preposition indprep : indirect) {
                        if (indCount++ > loopMax) break;
                        if (debug)
                            System.out.println("genProc(): proc: " + proc + " direct: " + dprep.noun + " indirect: " + indprep.noun);
                        StringBuffer english1 = new StringBuffer(english);
                        StringBuffer prop1 = new StringBuffer(prop);
                        prop1.append("(exists (?H ?P ?DO ?IO) (and ");
                        generateSubject(english1, prop1, subj);
                        generateVerb(english1, prop1, proc, time);
                        generateDirectObject(english1, prop1, dprep);
                        onceWithoutInd = generateIndirectObject(pw, indCount, english1, prop1, modal, proc, indprep, onceWithoutInd);
                    }
                }
            }
        }
    }

    /** ***************************************************************
     * Create action sentences from a subject, preposition, direct object,
     * preposition and indirect object.  Indirect object and its preposition
     * can be left out.  Actions will eventually be past and future tense or
     * wrapped in modals.
     */
    public void genWithHumans(PrintWriter pw,
                              StringBuffer english,
                              StringBuffer prop,
                              AVPair modal,
                              Collection<AVPair> humans,
                              Collection<String> intProc,
                              Collection<Preposition> direct,
                              Collection<Preposition> indirect) {

        System.out.println("GenSimpTestData.genActions()");
        int humCount = 0;
        for (AVPair human : humans) {
            if (humCount++ > loopMax) break;
            StringBuffer english1 = new StringBuffer(english);
            StringBuffer prop1 = new StringBuffer(prop);
            genProc(pw,english1,prop1,modal,human,intProc,direct,indirect);
        }
    }

    /** ***************************************************************
     * Create action sentences from a subject, preposition, direct object,
     * preposition and indirect object.  Indirect object and its preposition
     * can be left out.  Actions will eventually be past and future tense or
     * wrapped in modals.
     */
    public void genWithModals(PrintWriter pw,
                              Collection<AVPair> modals,
                              Collection<AVPair> humans,
                              Collection<String> socRoles,
                              Collection<String> intProc,
                              Collection<Preposition> direct,
                              Collection<Preposition> indirect) {

        System.out.println("GenSimpTestData.genActions()");
        int humCount = 0;
        for (AVPair modal : modals) {
            StringBuffer english = new StringBuffer();
            StringBuffer prop = new StringBuffer();
            System.out.println("genWithModals(): " + modal);
            if (!modal.attribute.equals("None")) {
                prop.append("(modalAttribute ");
                english.append(modal.value);
            }
            StringBuffer english1 = new StringBuffer(english);
            StringBuffer prop1 = new StringBuffer(prop);
            genWithHumans(pw,english1,prop1,modal,humans,intProc,direct,indirect);
            StringBuffer english2 = new StringBuffer(english);
            StringBuffer prop2 = new StringBuffer(prop);
            genWithRoles(pw,english2,prop2,modal,socRoles,intProc,direct,indirect);
        }
    }

    /** ***************************************************************
     * Create action sentences from a subject, preposition, direct object,
     * preposition and indirect object.  Indirect object and its preposition
     * can be left out.  Actions will eventually be past and future tense or
     * wrapped in modals.
     */
    public void genWithRoles(PrintWriter pw,
                             StringBuffer english,
                             StringBuffer prop,
                             AVPair modal,
                             Collection<String> roles,
                              Collection<String> intProc,
                              Collection<Preposition> direct,
                              Collection<Preposition> indirect) {

        System.out.println("GenSimpTestData.genActions()");
        int humCount = 0;
        for (String s : roles) {
            if (humCount++ > loopMax) break;
            AVPair role = new AVPair(s,null);
            StringBuffer english1 = new StringBuffer(english);
            StringBuffer prop1 = new StringBuffer(prop);
            genProc(pw,english1,prop1,modal,role,intProc,direct,indirect);
        }
    }

    /** ***************************************************************
     * init and call main routine.
     */
    public static void main(String args[]) {

        try {
            FileWriter fw = new FileWriter("out.txt");
            pw = new PrintWriter(fw);
            GenSimpTestData gstd = new GenSimpTestData();
            gstd.initActions(pw);
            pw.close();
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        //testTypes();
        //generate();
        //allAxioms();

        //genMissingTermFormats();
        //testNLG();
        //genTermFormatFromNames("/home/apease/workspace/sumo/WorldAirports.kif");
        //genHumans();
    }
}
