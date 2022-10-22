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
    public static boolean skip = false;
    public static HashSet<String> skipTypes = new HashSet<>();
    public static final int instLimit = 500;
    public static PrintWriter pw = null;

    public static final int loopMax = 3; // how many features at each level of linguistic composition
    public static final boolean randomize = true; // whether to go through features in order or randomize
    public static final Random rand = new Random();

    public static final int NOTIME = -1;
    public static final int PAST = 0;
    public static final int PRESENT = 1;
    public static final int FUTURE = 2;

    public static final String INTRANS = "intransitive";
    public static final String TRANS = "transitive";
    public static final String DITRANS = "ditransitive";
    public static final HashSet<String> verbEx = new HashSet<>(
            Arrays.asList("Acidification","Vending","OrganizationalProcess","NaturalProcess"));
    public static final HashSet<Word> attitudes = new HashSet<>();

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
        public HashMap<String,String> words = new HashMap<>(); // word is the key, value is transitivity string
        public String noun = null;
    }

    /** ***************************************************************
     * @return objects
     */
    public void addArguments(Collection<String> col, Collection<Preposition> objects) {

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
     * Initialize the grammatical forms of propositional attitudes
     */
    public void initAttitudes() {
        attitudes.add(new Word("None","","",""));
        attitudes.add(new Word("knows","know","knows","knew"));
        attitudes.add(new Word("believes","believe","believes","believed"));
        attitudes.add(new Word("says","say","says","said"));
        attitudes.add(new Word("desires","desire","desires","desired"));
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
     */
    public class Word {
        public String term = null;
        public String root = null;
        public String present = null;
        public String part = null;
        public String trans = null; // transitivity string

        public Word(String t, String r, String pr, String pa) {
            term = t; root = r; present = pr; part = pa;
        }
    }
    /** ***************************************************************
     */
    public class LFeatures {

        public boolean attNeg = false; // for propositional attitudes
        public String attSubj = null; // the agent holding the attitude
        public String attitude = null;
        public boolean negated = false;
        public Collection<AVPair> modals = null;
        public AVPair modal= null;
        public Collection<AVPair> humans = null;
        public Collection<String> socRoles = null;
        public AVPair subj= null;
        public Collection<String> intProc = null;
        public Collection<Preposition> direct = null;
        public Preposition dirPrep = null;
        public Collection<Preposition> indirect = null;
        public Preposition indPrep = null;
        public String word = null;
        public int plevel = 0; //paren level 0 is no open parens

        public LFeatures() {

            //  get capabilities from axioms like
            //  (=> (instance ?GUN Gun) (capability Shooting instrument ?GUN))
            indirect = collectCapabilities();
            humans = readHumans();
            modals = initModals();
            socRoles = kb.kbCache.getInstancesForType("SocialRole");

            HashSet<String> artInst = kb.kbCache.getInstancesForType("Artifact");
            HashSet<String> artClass = kb.kbCache.getChildClasses("Artifact");
            intProc = kb.kbCache.getChildClasses("DualObjectProcess");
            HashSet<String> orgInst = kb.kbCache.getInstancesForType("OrganicObject");
            HashSet<String> orgClass = kb.kbCache.getChildClasses("OrganicObject");
            direct = new HashSet<>();
            addArguments(artInst, direct);
            addArguments(artClass, direct);
            addArguments(orgInst, direct);
            addArguments(orgClass, direct);
        }
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

        initAttitudes();
        LFeatures lfeat = new LFeatures();
        StringBuffer english = new StringBuffer();
        StringBuffer prop = new StringBuffer();
        genAttitudes(pw,english,prop,lfeat);
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
    public String verbForm(String term, boolean negated, int time, String word) {

        //String word = kb.getTermFormat("EnglishLanguage",term);
        if (word == null) {
            System.out.println("verbForm(): null input or no term format for " + term);
            return null;
        }
        String neg = "";
        if (negated)
            neg = "not ";
        if (word.endsWith("ing")) {
            if (time == PAST)
                return "was " + neg + word;
            if (time == PRESENT)
                return "is " + neg + word;
            if (time == FUTURE)
                return "will " + neg + "be " + word;
            if (time == NOTIME)
                return "is " + neg + word; // for time = -1
        }
        String root =  WordNet.wn.verbRootForm(word,word.toLowerCase());
        if (root == null)
            root = word;
        if (time == PAST) {
            if (root.endsWith("e"))
                return "was " + neg + root.substring(0,root.length()-1) + "ing";
            else
                return "was " + neg + root + "ing";
        }
        if (time == PRESENT) {
            if (negated) {
                return "doesn't " + root;
            }
            else {
                if (root.endsWith("y"))
                    return root.substring(0, root.length() - 1) + "ies";
                return root + "s";
            }
        }
        if (time == FUTURE)
            return "will " + neg + root;
        if (root.endsWith("y") && !negated)
            return root.substring(0,root.length()-1) + "ies";
        if (time != NOTIME)
            System.out.println("Error in verbForm(): time is unallowed value: " + time);
        if (negated)
            return "doesn't " + root;
        else
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
        if (word.matches("^[aeiouAEIO].*")) // pronouncing "U" alone is like a consonant
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
    public void generateVerb(boolean negated,StringBuffer english, StringBuffer prop,
                                String proc, String word, int time) {

        english.append(verbForm(proc,negated,time,word) + " ");
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
                             ProcInfo proc,
                             Preposition dprep) {

        if (kb.isSubclass(proc.term,"Translocation") &&
                (kb.isSubclass(dprep.noun,"Region") || kb.isSubclass(dprep.noun,"StationaryObject"))) {
            english.append("to " + nounForm(dprep.noun) + " ");
            prop.append("(instance ?DO " + dprep.noun + ") (destination ?P ?DO) ");
        }
        else {
            english.append(nounForm(dprep.noun) + " ");
            prop.append("(instance ?DO " + dprep.noun + ") (patient ?P ?DO) ");
        }
    }

    /** ***************************************************************
     * Get equivalent synsets and transitivity flags for a SUMO term
     */
    public ProcInfo findProcInfo(String proc) {

        ProcInfo result = this.new ProcInfo();
        ArrayList<String> synsets = WordNetUtilities.getEquivalentSynsetsFromSUMO(proc);
        if (synsets == null || synsets.size() == 0)
            return null;
        if (synsets.size() > 1)
            System.out.println("GenSimpTestData.findProcInfo(): more than one synset " + synsets + " for " + proc);
        result.synset = synsets.get(0);
        ArrayList<String> resultWords = WordNet.wn.getWordsFromSynset(result.synset);
        for (String w : resultWords) {
            if (w.contains("_")) // too hard grammatically for now to have compound verbs
                continue;
            String trans = WordNet.wn.getTransitivity(result.synset,w);
            result.words.put(w,trans);
        }
        return result;
    }

    /** ***************************************************************
     */
    private String closeParens(LFeatures lfeat) {

        StringBuffer result = new StringBuffer();
        if (!lfeat.attitude.equals("None")) {
            result.append(")))");
            if (lfeat.attNeg) result.append(")");
        }
        if (!lfeat.modal.attribute.equals("None")) result.append(")");
        if (lfeat.negated) result.append(")");
        return result.toString();
    }

    /** ***************************************************************
     */
    public boolean generateIndirectObject(PrintWriter pw, int indCount,
                                          StringBuffer english, StringBuffer prop,
                                          ProcInfo proc,
                                          LFeatures lfeat,
                                          boolean onceWithoutInd) {

        if (debug) System.out.println("generateIndirectObject(): proc, procType: " +
                lfeat.indPrep.noun + ", " + lfeat.indPrep.procType);
        if (kb.isSubclass(lfeat.indPrep.noun,lfeat.indPrep.procType) && proc.words.get(lfeat.word).contains(DITRANS)) {
            english.append("with " + nounForm(lfeat.indPrep.noun));
            prop.append("(instance ?IO " + lfeat.indPrep.noun + ") (" + lfeat.indPrep.prep + " ?P ?IO)");
            if (!lfeat.modal.attribute.equals("None"))
                prop.append(lfeat.modal.attribute + "))");
            else
                prop.append("))");
            prop.append(closeParens(lfeat));
            onceWithoutInd = false;
            pw.println(english);
            pw.println(prop+ "\n");
        }
        else {  // close off the formula without an indirect object
            if (!lfeat.modal.attribute.equals("None"))
                prop.append(lfeat.modal.attribute + "))");
            else
                prop.append("))");
            prop.append(closeParens(lfeat));
            indCount = 0;
            if (!onceWithoutInd) {
                pw.println(english);
                pw.println(prop + "\n");
            }
            onceWithoutInd = true;
        }
        return onceWithoutInd;
    }

    /** ***************************************************************
     */
    public boolean excludedVerb(String v) {

        if (compoundVerb(v))
            return true;
        if (verbEx.contains(v))
            return true;
        for (String s : verbEx) {
            if (kb.isSubclass(v,s))
                return true;
        }
        return false;
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
                        LFeatures lfeat) {

        int procCount = 0;
        ArrayList<String> processTypes = new ArrayList<>();
        processTypes.addAll(lfeat.intProc);
        while (procCount++ < loopMax) {
            String proc = null;
            if (randomize)
                proc = processTypes.get(rand.nextInt(processTypes.size()));
            else
                proc = processTypes.get(procCount);
            ProcInfo pi = findProcInfo(proc);
            if (pi == null) {
                pi = new ProcInfo();
                pi.term = proc;
                pi.words.put(kb.getTermFormat("EnglishLanguage",proc),"[" + TRANS + "]");
            }
            if (excludedVerb(proc)) { continue; } // too hard grammatically for now to have compound verbs
            for (String word : pi.words.keySet()) {
                if (debug) System.out.println("genProc(): proc: " + proc);
                if (procCount++ > loopMax) break;
                lfeat.word = word;
                for (int time = -1; time < 3; time++) { // -1 = no time spec, 0=past, 1=present, 2=future
                    int directCount = 0;
                    for (Preposition dprep : lfeat.direct) {
                        lfeat.dirPrep = dprep;
                        if (directCount++ > loopMax) break;
                        if (debug) System.out.println("genProc(): proc: " + proc + " direct: " + dprep.noun);
                        int indCount = 0;
                        boolean onceWithoutInd = false;
                        for (Preposition indprep : lfeat.indirect) {
                            lfeat.indPrep = indprep;
                            if (indCount++ > loopMax) break;
                            if (debug)
                                System.out.println("genProc(): proc: " + proc + " direct: " + dprep.noun + " indirect: " + indprep.noun);
                            StringBuffer english1 = new StringBuffer(english);
                            StringBuffer prop1 = new StringBuffer(prop);
                            if (lfeat.negated)
                                prop1.append("(not ");
                            prop1.append("(exists (?H ?P ?DO ?IO) (and ");
                            generateSubject(english1, prop1, lfeat.subj);
                            generateVerb(lfeat.negated,english1, prop1, proc, word, time);
                            if (pi.words.get(word).contains(TRANS))
                                generateDirectObject(english1, prop1, pi, dprep);
                            onceWithoutInd = generateIndirectObject(pw, indCount, english1, prop1, pi, lfeat, onceWithoutInd);
                        }
                    }
                }
            }
        }
    }

    /** ***************************************************************
     */
    public void genWithHumans(PrintWriter pw,
                              StringBuffer english,
                              StringBuffer prop,
                              LFeatures lfeat) {

        System.out.println("GenSimpTestData.genActions()");
        int humCount = 0;
        for (AVPair human : lfeat.humans) {
            lfeat.subj = human;
            if (lfeat.subj.attribute.equals(lfeat.attSubj)) continue;
            if (humCount++ > loopMax) break;
            StringBuffer english1 = new StringBuffer(english);
            StringBuffer prop1 = new StringBuffer(prop);
            genProc(pw,english1,prop1,lfeat);
        }
    }

    /** ***************************************************************
     * use None, knows, believes, says, desires for attitudes
     */
    public void genAttitudes(PrintWriter pw,
                             StringBuffer english,
                             StringBuffer prop,
                             LFeatures lfeat) {

        System.out.println("GenSimpTestData.genAttitudes(): ");
        System.out.println("GenSimpTestData.genAttitudes(): human list size: " + lfeat.humans.size());
        int humCount = 0;
        for (AVPair human : lfeat.humans) {
            if (humCount++ > loopMax) break;
            lfeat.attSubj = human.attribute; // the subject of the propositional attitude
            StringBuffer english1 = new StringBuffer(english);
            StringBuffer prop1 = new StringBuffer(prop);
            for (Word attWord : attitudes) {
                lfeat.attitude = attWord.term;
                StringBuffer english2 = new StringBuffer(english1);
                StringBuffer prop2 = new StringBuffer(prop1);
                lfeat.attNeg = false;
                if (!attWord.term.equals("None")) {
                    english2.append(human.attribute + " " + attWord.present + " that ");
                    prop2.append("(exists (?HA) (and (instance ?HA Human) ");
                    prop2.append("(names \"" + human.attribute + "\" ?HA) (" + attWord.term + " ?HA ");
                    lfeat.plevel = 3;
                }
                genWithModals(pw, english2,prop2,lfeat);

                StringBuffer english3 = new StringBuffer(english1);
                StringBuffer prop3 = new StringBuffer(prop1);
                lfeat.attNeg = true;
                if (!attWord.term.equals("None")) {
                    english3.append(human.attribute + " doesn't " + attWord.root + " that ");
                    prop3.append("(not (exists (?HA) (and (instance ?HA Human) ");
                    prop3.append("(names \"" + human.attribute + "\" ?HA) (" + attWord.term + " ?HA ");
                    lfeat.plevel = 4;
                }
                genWithModals(pw, english3,prop3,lfeat);
            }
        }

        for (String role : lfeat.socRoles) {
            if (humCount++ > loopMax) break;
            lfeat.attSubj = role; // the subject of the propositional attitude
            StringBuffer english1 = new StringBuffer(english);
            StringBuffer prop1 = new StringBuffer(prop);
            for (Word attWord : attitudes) {
                lfeat.attitude = attWord.term;
                StringBuffer english2 = new StringBuffer(english1);
                StringBuffer prop2 = new StringBuffer(prop1);
                lfeat.attNeg = false;
                if (!attWord.term.equals("None")) {
                    english2.append("a " + role + " " + attWord.present + " that ");
                    prop2.append("(exists (?HA) (and (instance ?HA Human) ");
                    prop2.append("(attribute ?HA" + role + ") (" + attWord.term + " ?HA ");
                }
                genWithModals(pw, english2,prop2,lfeat);

                StringBuffer english3 = new StringBuffer(english1);
                StringBuffer prop3 = new StringBuffer(prop1);
                lfeat.attNeg = true;
                if (!attWord.term.equals("None")) {
                    english3.append("a " + role + " doesn't " + attWord.root + " that ");
                    prop3.append("(not (exists (?HA) (and (instance ?HA Human) ");
                    prop3.append("(attribute ?HA" + role + ") (" + attWord.term + " ?HA ");
                }
                genWithModals(pw, english3,prop3,lfeat);
            }
        }
    }

    /** ***************************************************************
     */
    public String negatedModal(String modal,boolean negated) {

        if (!negated)
            return modal;
        else {
            return modal.substring(0,5) + " not" + modal.substring(5);
        }
    }

    /** ***************************************************************
     * Create action sentences from a subject, preposition, direct object,
     * preposition and indirect object.  Indirect object and its preposition
     * can be left out.  Actions will eventually be past and future tense or
     * wrapped in modals.
     */
    public void genWithModals(PrintWriter pw,
                              StringBuffer english,
                              StringBuffer prop,
                              LFeatures lfeat) {

        System.out.println("GenSimpTestData.genActions()");
        int humCount = 0;
        for (AVPair modal : lfeat.modals) {
            lfeat.modal = modal;
            StringBuffer englishNew = new StringBuffer(english);
            StringBuffer propNew = new StringBuffer(prop);
            System.out.println("genWithModals(): " + modal);
            if (!lfeat.modal.attribute.equals("None")) {
                propNew.append("(modalAttribute ");
                englishNew.append(negatedModal(modal.value,lfeat.negated));
                lfeat.negated = false;
            }
            StringBuffer english1 = new StringBuffer(englishNew);
            StringBuffer prop1 = new StringBuffer(propNew);
            genWithHumans(pw,english1,prop1,lfeat);
            StringBuffer english2 = new StringBuffer(englishNew);
            StringBuffer prop2 = new StringBuffer(propNew);
            genWithRoles(pw,english2,prop2,lfeat);
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
                             LFeatures lfeat) {

        System.out.println("GenSimpTestData.genActions()");
        int humCount = 0;
        for (String s : lfeat.socRoles) {
            if (lfeat.subj.attribute.equals(s)) continue;
            if (humCount++ > loopMax) break;
            AVPair role = new AVPair(s,null);
            lfeat.subj = role;
            StringBuffer english1 = new StringBuffer(english);
            StringBuffer prop1 = new StringBuffer(prop);
            genProc(pw,english1,prop1,lfeat);
        }
    }

    /** ***************************************************************
     */
    public static void showHelp() {

        System.out.println("Sentence generation");
        System.out.println("  options:");
        System.out.println("  -h - show this help screen");
        System.out.println("  -tf - generate any missing termFormat statements");
        System.out.println("  -hu - generate SUOKIF from a list of gendered names");
        System.out.println("  -t - run tests");
        System.out.println("  -a - generate logic/language pairs for all statements in KB");
        System.out.println("  -g - generate ground statement pairs for all relations");
        System.out.println("  -s - generate NL/logic compositional sentences");
        System.out.println("  -n - generate term formats from term names in a file");
    }

    /** ***************************************************************
     * init and call main routine.
     */
    public static void main(String args[]) {

        if (args == null || args.length == 0 || args[0].equals("-h"))
            showHelp();
        else {
            if (args != null && args.length > 0 && args[0].equals("-s")) {
                try {
                    FileWriter fw = new FileWriter("out.txt");
                    pw = new PrintWriter(fw);
                    GenSimpTestData gstd = new GenSimpTestData();
                    gstd.initActions(pw);
                    pw.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            if (args != null && args.length > 0 && args[0].equals("-g"))
                generate();
            if (args != null && args.length > 0 && args[0].equals("-a"))
                allAxioms();
            if (args != null && args.length > 0 && args[0].equals("-tf"))
                genMissingTermFormats();
            if (args != null && args.length > 0 && args[0].equals("-hu"))
                genHumans();
            if (args != null && args.length > 0 && args[0].equals("-t")) {
                testTypes();
                testNLG();
            }
            if (args != null && args.length > 1 && args[0].equals("-n")) {
                genTermFormatFromNames(args[1]);
            }
        }
    }
}
