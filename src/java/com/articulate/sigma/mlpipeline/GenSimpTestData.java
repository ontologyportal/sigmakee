package com.articulate.sigma.mlpipeline;

import com.articulate.sigma.*;
import com.articulate.sigma.nlg.NLGUtils;
import com.articulate.sigma.utils.AVPair;
import com.articulate.sigma.utils.StringUtil;
import com.articulate.sigma.wordNet.WordNet;
import com.articulate.sigma.wordNet.WordNetUtilities;

import java.io.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
 * - epistemics and authorship
 * - counts and measures of things
 * - social and job roles as well as names of people
 * - negations
 */

public class GenSimpTestData {

    public static boolean debug = false;
    public static KB kb;
    public static boolean skip = false;
    public static HashSet<String> skipTypes = new HashSet<>();
    public static final int instLimit = 200;
    public static PrintWriter pw = null;

    public static final int loopMax = 3; // how many features at each level of linguistic composition
    public static final int attMax = 3;
    public static final int modalMax = 3;
    public static final int humanMax = 3; // separate constant to limit number of human names
    public static final int freqLimit = 3; // SUMO terms used in a statement must have an equivalent
                                           // synset with a frequency of at least freqLimit

    public static final Random rand = new Random();

    public static final int NOTIME = -1;
    public static final int PAST = 0;         // spoke, docked
    public static final int PASTPROG = 1;     // was speaking, was docking
    public static final int PRESENT = 2;      // speaks, docks
    public static final int PROGRESSIVE = 3;  // is speaking, is docking
    public static final int FUTURE = 4;       // will speak, will dock
    public static final int FUTUREPROG = 5;   // will be speaking, will be docking
    public static final int IMPERATIVE = 6;   // treat imperatives like a tense

    public static final HashSet<String> verbEx = new HashSet<>(
            Arrays.asList("Acidification","Vending","OrganizationalProcess",
                    "NaturalProcess","Corkage","LinguisticCommunication"));
    public static final ArrayList<Word> attitudes = new ArrayList<>();
    public static final HashSet<String> suppress = new HashSet<>( // forms to suppress, usually for testing
            Arrays.asList());

    public static final HashMap<String,HashSet<Capability>> capabilities = new HashMap<>();

    public static PrintWriter englishFile = null; //generated English sentences
    public static PrintWriter logicFile = null;   //generated logic sentences, one per line,
                                                  // NL/logic should be on same line in the different files

    public static long estSentCount = 1;
    public static long sentCount = 0;
    public static long sentMax = 10000000;
    public static boolean startOfSentence = true;
    public static ArrayList<String> numbers = new ArrayList<>();
    public static ArrayList<String> requests = new ArrayList<>(); // polite phrase at start of sentence
    public static ArrayList<String> endings = new ArrayList<>(); // polite phrase at end of sentence
    public static ArrayList<String> others = new ArrayList<>(); // when next noun is same as a previous one
    public static HashMap<String,String> prepPhrase = new HashMap<>();

    /** ***************************************************************
     */
    public GenSimpTestData() {

        initNumbers();
        initRequests();
        initAttitudes();
        initOthers();
        initPrepPhrase();
        initEndings();
        genProcTable();
    }

    /** ***************************************************************
     * estimate the number of sentences that will be produced
     */
    public static void initNumbers() {

        numbers.add("zero");
        numbers.add("one");
        numbers.add("two");
        numbers.add("three");
        numbers.add("four");
        numbers.add("five");
        numbers.add("six");
        numbers.add("seven");
        numbers.add("eight");
        numbers.add("nine");
        numbers.add("ten");
    }

    /** ***************************************************************
     * Politeness wrappers for imperatives
     */
    public static void initRequests() {

        requests.add("Could you please ");
        requests.add("Can you please ");
        requests.add("Would you please ");
        requests.add("Could you ");
        requests.add("Would you ");
        requests.add("Please ");
    }

    /** ***************************************************************
     * Politeness wrappers for imperatives
     */
    public static void initEndings() {

        endings.add("please");
        endings.add("for me please");
        endings.add("for me");
    }

    /** ***************************************************************
     * Politeness wrappers for imperatives
     */
    public static void initOthers() {

        others.add("another ");
        others.add("a different ");
        others.add("the other ");
    }

    /** ***************************************************************
     * Politeness wrappers for imperatives
     */
    public static void initPrepPhrase() {

        prepPhrase.put("vote","for ");
        prepPhrase.put("search","for ");
        prepPhrase.put("partake","of ");
        prepPhrase.put("dreaming","of "); // TODO up
        prepPhrase.put("fall","off of "); // TODO or on, onto, under, outside, inside...
        prepPhrase.put("sing","about "); // TODO for, to, with
        prepPhrase.put("pull","on "); // TODO pull at, pull over, pull out, or just pull a vehicle
        prepPhrase.put("dream","of "); // TODO about
        prepPhrase.put("tell","about "); // TODO of, someone about
        prepPhrase.put("act","as ");
        prepPhrase.put("pretend","to be ");
        prepPhrase.put("act","as ");
        prepPhrase.put("nod","to ");
        prepPhrase.put("hunt","for "); // must have a direct object
        prepPhrase.put("read","about ");  // TODO but also "read a book"
        prepPhrase.put("wading","in ");
    }

    /** ***************************************************************
     * estimate the number of sentences that will be produced
     */
    public static String printTense(int t) {

        switch (t) {
            case -1: return "NOTIME";
            case 0:  return "PAST";
            case 1:  return "PASTPROG";
            case 2:  return "PRESENT";
            case 3:  return "PROGRESSIVE";
            case 4:  return "FUTURE";
            case 5:  return "FUTUREPROG";
            case 6:  return "IMPERATIVE";
        }
        return "";
    }

    /** ***************************************************************
     * estimate the number of sentences that will be produced
     */
    public static long estimateSentCount(LFeatures lfeat) {

        long count = 2; // include negation
        if (!suppress.contains("attitude"))
            count = count * attMax;
        if (!suppress.contains("modal"))
            count = count * modalMax * 2; //include negation
        count = count * (humanMax + lfeat.socRoles.size());
        count = count * loopMax; // lfeat.intProc.size();
        count = count * loopMax; // lfeat.direct.size();
        count = count * loopMax; // lfeat.indirect.size();
        return count;
    }

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
            if (max > instLimit) {
                max = instLimit;
                if (sig.size() > 2)  // avoid combinatorial explosion in higher arities
                    max = 100;
                if (sig.size() > 3)
                    max = 31;
                if (sig.size() > 4)
                    max = 15;
            }
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
            if (forms.size() % 1000 == 0)
                System.out.println("genFormulas(): size so far: " + forms.size());
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
                kb.getTermFormatMap("EnglishLanguage"), kb, "EnglishLanguage");
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
    public static HashMap<String,String> readHumans() {

        HashMap<String,String> result = new HashMap<>();
        ArrayList<ArrayList<String>> fn = DB.readSpreadsheet(kb.kbDir +
                File.separator + "WordNetMappings/FirstNames.csv", null, false, ',');
        fn.remove(0);  // remove the header
        for (ArrayList<String> ar : fn) {
            String firstName = ar.get(0);
            String g = ar.get(1);
            result.put(firstName,g);
        }
        return result;
    }

    /** ***************************************************************
     * generate new SUMO termFormat and instance statements for names
     */
    public static void generateAllHumans() {

        HashMap<String,String> hums = readHumans();
        for (String firstName : hums.keySet()) {
            String g = hums.get(firstName);
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
     * generate new SUMO statements for names
     */
    public String genSUMOForHuman(LFeatures lfeat, String name, String var) {

        StringBuffer sb = new StringBuffer();
        if (name != null) {
            String gender = "Male";
            String g = lfeat.genders.get(name);
            if (g.equalsIgnoreCase("F"))
                gender = "Female";
            //sb.append("(instance " + var + " Human) ");
            sb.append("(attribute " + var + " " + gender + ") ");
            sb.append("(names \"" + name + "\" " + var + ") ");
        }
        return sb.toString();
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
                            logicFile.println(form);
                            String actual = toEnglish(form);
                            englishFile.println(StringUtil.filterHtml(actual));
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
        System.out.println("handleGroundStatements(): search through " + forms.size() + " statements");
        for (Formula f : forms) {
            if (f.isGround() && formatMap.containsKey(f.relation) && !StringUtil.emptyString(f.toString())) {
                englishFile.print(toEnglish(f.toString()));
                logicFile.println(f);
            }
        }
    }

    /** ***************************************************************
     * Generate arguments for all relations and output their English
     * paraphrase
     */
    public static void generate() {

        System.out.println("GenSimpTestData.generate()");
        KBmanager.getMgr().initializeOnce();
        //resultLimit = 0; // don't limit number of results on command line
        kb = KBmanager.getMgr().getKB(KBmanager.getMgr().getPref("sumokbname"));
        System.out.println("generate(): # relations: " + kb.kbCache.relations.size());
        HashMap<String, String> formatMap = kb.getFormatMap("EnglishLanguage");
        skipTypes.addAll(Arrays.asList("Formula") );
        System.out.println("generate(): output existing ground statements ");
        handleGroundStatements(formatMap);
        System.out.println("generate(): create ground statements ");
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
                logicFile.println(form.replace("\n", "").replace("\r", ""));
                String actual = toEnglish(form);
                englishFile.println(StringUtil.filterHtml(actual));
            }
        }
    }

    /** ***************************************************************
     */
    public static void progressPrint() {

        if ((sentCount % 100) != 0) return;
        if (!debug) System.out.print("\r\33[2K");
        double value = ((double) sentCount / (double) sentMax);
        System.out.print(String.format("%.2f", value));
        System.out.print("% complete. ");
        System.out.print(sentCount + " of total " + sentMax);
        if (debug) System.out.println();
    }

    /** ***************************************************************
     */
    public static void testProgressPrint() {

        estSentCount = 1000000;
        do {
            progressPrint();
            sentCount++;
        } while (true);
    }

    /** ***************************************************************
     */
    public class Preposition {
        public String procType = null;
        public String prep = null;
        public String noun = null;
        public String toString() {
            return procType + ": " + prep + ": " + noun;
        }
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
            p.procType = s;
            objects.add(p);
        }
    }

    /** ***************************************************************
     * Initialize the grammatical forms of propositional attitudes
     */
    public void initAttitudes() {

        for (int i = 0; i < 50; i++)
            attitudes.add(new Word("None","","",""));
        if (!suppress.contains("attitude")) {
            attitudes.add(new Word("knows", "know", "knows", "knew"));
            attitudes.add(new Word("believes", "believe", "believes", "believed"));
            attitudes.add(new Word("says", "say", "says", "said"));
            attitudes.add(new Word("desires", "desire", "desires", "desired"));
        }
    }

    /** ***************************************************************
     * Collect capability axioms of a specific form: Antecedent must be
     * a single (instance ?X ?Y) literal.  Consequent must be a single
     * (capability ?A ?B ?X) literal.  Returns ?Y - class of the thing,
     * ?A - the Process type and ?B the role that ?Y plays in the process.
     * Also accept (requiredRole ?A ?B ?C) and (prohibitedRole ?A ?B ?C)
     * forms
     * @return Capability objects
     */
    public HashSet<Capability> collectCapabilities() {

        HashSet<Capability> result = new HashSet<>();
        ArrayList<Formula> forms2 = kb.ask("arg",0,"requiredRole");
        System.out.println("collectCapabilities(): requiredRoles: " + forms2);
        for (Formula f : forms2) {
            //System.out.println("collectCapabilities(): form: " + f);
            Capability p = this.new Capability();
            p.proc = f.getStringArgument(1);
            p.caserole = f.getStringArgument(2);
            p.object = f.getStringArgument(3);
            p.must = true;
            result.add(p);
        }

        ArrayList<Formula> forms3 = kb.ask("arg",0,"prohibitedRole");
        for (Formula f : forms3) {
            //System.out.println("collectCapabilities(): form: " + f);
            Capability p = this.new Capability();
            p.proc = f.getStringArgument(1);
            p.caserole = f.getStringArgument(2);
            p.object = f.getStringArgument(3);
            p.mustNot = true;
            result.add(p);
        }

        ArrayList<Formula> forms = kb.ask("cons",0,"capability");
        for (Formula f : forms) {
            //System.out.println("collectCapabilities(): form: " + f);
            String ant = FormulaUtil.antecedent(f);
            Formula fant = new Formula(ant);
            if (fant.isSimpleClause(kb) && fant.car().equals("instance")) {
                String antClass = fant.getStringArgument(2); // the thing that plays a role
                String cons = FormulaUtil.consequent(f);
                Formula fcons = new Formula(cons);
                String kind = fcons.getStringArgument(0); // capability or requiredRole
                if (fcons.isSimpleClause(kb)) {
                    String consClass = fcons.getStringArgument(1);  // the process type
                    String rel = fcons.getStringArgument(2);  // the role it plays
                    Capability p = this.new Capability();
                    p.proc = consClass;
                    p.caserole = rel;
                    p.object = antClass;
                    if (forms2.contains(f))
                        p.must = true;
                    else
                        p.can = true;
                    result.add(p);
                }
                else if (fcons.isSimpleNegatedClause(kb)) {
                    Formula fneg = fcons.getArgument(1);
                    String consClass = fneg.getStringArgument(1);  // the process type
                    String rel = fneg.getStringArgument(2);  // the role it plays
                    Capability p = this.new Capability();
                    p.proc = consClass;
                    p.caserole = rel;
                    p.object = antClass;
                    if (forms2.contains(f))
                        p.must = true;
                    else
                        p.can = true;
                    p.negated = true;
                    result.add(p);
                }
            }
        }
        return result;
    }

    /** ***************************************************************
     * @return modifications to the parameter as a side effect
     */
    public void constrainTerms(Collection<String> terms) {

        if (debug) System.out.println("constrainTerms(): ");
        HashSet<String> newProcList = new HashSet<>(terms);
        terms.clear();
        for (String proc : newProcList) {
            if (debug) System.out.println("constrainTerms(): proc list size: " + newProcList.size() + " for terms: " + terms);
            ArrayList<String> synsets = WordNetUtilities.getEquivalentSynsetsFromSUMO(proc);
            int maxInt = 0;
            for (String s : synsets) {
                if (WordNet.wn.senseFrequencies.containsKey(s)) {
                    int freq = WordNet.wn.senseFrequencies.get(s);
                    if (freq > maxInt)
                        maxInt = freq;
                }
            }
            if (maxInt > freqLimit)
                terms.add(proc);
        }
    }

    /** ***************************************************************
     */
    public class Word {
        public String term = null;
        public String root = null;
        public String present = null;
        public String part = null;
        public String trans = null; // transitivity string
        public String derivedFromParentClass = ""; // is the term a subclass of the authored relationship

        public Word(String t, String r, String pr, String pa) {
            term = t; root = r; present = pr; part = pa;
        }
    }

    /** ***************************************************************
     */
    public class Capability {

        public boolean negated = false; // not a capability
        public String proc = null; // the process or verb
        public String object = null; // SUMO term for direct or indirect object type
        public String caserole = null; // the CaseRole
        public String prep = null; // the preposition to use
        public boolean must = false; // must have the following CaseRole
        public boolean mustNot = false; // must not have the following CaseRole
        public boolean can = false; // can have the following CaseRole
        public String fromParent = null; // which parent Process is the info inherited from, if any
        public String toString() {
            return proc + " : " + object + " : " + caserole + " : " + negated;
        }
    }

    /** ***************************************************************
     * @param terms a collection of SUMO terms
     * @return an ArrayList of AVPair with a value of log of frequency
     *          derived from the equivalent synsets the terms map to. Attribute
     *          of each AVPair is a SUMO term.
     */
    public ArrayList<AVPair> findWordFreq(Collection<String> terms) {

        ArrayList<AVPair> avpList = new ArrayList<>();
        for (String term : terms) {
            ArrayList<String> synsets = WordNetUtilities.getEquivalentSynsetsFromSUMO(term);
            //if (debug) System.out.println("findWordFreq(): proc list size: " + terms.size());
            int count;
            if (synsets == null || synsets.size() == 0)
                count = 0;
            else {
                int freq = 0;
                for (String s : synsets) {
                    ArrayList<String> resultWords = WordNet.wn.getWordsFromSynset(s);
                    //System.out.println("findWordFreq(): freq: " + WordNet.wn.senseFrequencies.size());
                    //System.out.println("findWordFreq(): synset: " + s);
                    int f = 0;
                    if (WordNet.wn.senseFrequencies.containsKey(s))
                        f = WordNet.wn.senseFrequencies.get(s);
                    if (f > freq)
                        freq = f;
                }
                count = (int) Math.round(Math.log(freq) + 1.0);
            }
            AVPair avp = new AVPair(term,Integer.toString(count));
            avpList.add(avp);
        }
        return avpList;
    }

    /** ***************************************************************
     * Create action sentences from a subject, preposition, direct object,
     * preposition and indirect object.  Indirect object and its preposition
     * can be left out.  Actions will eventually be past and future tense or
     * wrapped in modals.
     */
    public void runGenSentence() {

        System.out.println("GenSimpTestData.initActions(): start");
        KBmanager.getMgr().initializeOnce();
        kb = KBmanager.getMgr().getKB(KBmanager.getMgr().getPref("sumokbname"));
        System.out.println("GenSimpTestData.initActions(): finished loading KBs");

        LFeatures lfeat = new LFeatures(this);
        ArrayList<String> terms = new ArrayList<>();
        //if (debug) System.out.println("GenSimpTestData.initActions():  lfeat.direct: " + lfeat.direct);
        //System.exit(1);
        //for (Preposition p : lfeat.direct)
        //    terms.add(p.procType);
        estSentCount = estimateSentCount(lfeat);
        genAttitudes(lfeat);
    }

    /** ***************************************************************
     * also return true if there's no termFormat for the process
     */
    public boolean compoundVerb(String term) {

        String word = kb.getTermFormat("EnglishLanguage",term);
        if (word == null || word.contains(" ")) {
            if (debug) System.out.println("compoundVerb(): or null: " + word + " " + term);
            return true;
        }
        return false;
    }

    /** ***************************************************************
     * return true if the tense is past progressive, present progressive,
     * or future progressive
     */
    public boolean isProgressive(int time) {

        if (time == PROGRESSIVE || time == PASTPROG || time == FUTUREPROG)
            return true;
        else
            return false;
    }

    /** ***************************************************************
     * @return the correct version of the copula for tense, number and
     * negation including randomized contractions of negation
     */
    public String conjCopula(boolean negated, int time, boolean plural) {

        String cop = "is";
        String neg = "";
        boolean cont = rand.nextBoolean() && negated;
        switch (time) {
            case NOTIME: break;
            case PAST: cop = "was"; if (plural) cop = "were"; if (negated) cop = cop + " not"; break;
            case PASTPROG:  cop = "was"; if (plural) cop = "were"; if (negated) cop = cop + " not"; break;
            case PRESENT:  cop = "is"; if (plural) cop = "are"; if (negated) cop = cop + " not"; break;
            case PROGRESSIVE:  cop = "is"; if (plural) cop = "are"; if (negated) cop = cop + " not"; break;
            case FUTURE: cop = "will be"; if (negated) cop = "will not be"; break;
            case FUTUREPROG: cop = "will be"; if (negated) cop = "will not be"; break;
        }
        if (cont && time < FUTURE)
            return cop.replace(" not","n't ");
        if (cont)
            return cop.replace("will not","won't") + " ";
        return cop + " ";
    }

    /** ***************************************************************
     * Handle the auxilliary construction of "play X" when X is a Game
     * @param term is a SUMO term
     */
    public String verbForm(String term, boolean negated, String word, boolean plural, StringBuffer english,
                           LFeatures lfeat) {

        String result = "";
        if (debug) System.out.println("verbForm(): (term,word): " + term + ", " + word);
        if (debug) System.out.println("verbForm(): tense: " + printTense(lfeat.tense));
        if (debug) System.out.println("verbForm(): plural: " + plural);
        if (debug) System.out.println("verbForm(): negated: " + negated);
        if (debug) System.out.println("verbForm(): subj: " + lfeat.subj);
        if (lfeat == null) {
            System.out.println("Error! verbForm(): null lfeat");
            return "";
        }
        if (debug) System.out.println("verbForm(): subj: " + lfeat.subj);
        if (!StringUtil.emptyString(lfeat.subj) && lfeat.subj.equals("You")) {
            if (debug) System.out.println("verbForm(): using imperative tense for 'you'");
            lfeat.tense = IMPERATIVE;
        }
        String root = "";
        String nounForm = "";
        if (kb.isSubclass(term,"Game") && !term.equals("Game")) {
            nounForm = " " + word;
            root = "play";
        }
        if (word == null) {
            System.out.println("verbForm(): null input or no term format for " + term);
            return null;
        }
        String neg = "";
        if (negated) {
            if (lfeat.subj != null && lfeat.subj.equals("You")) {
                if (english.toString().contains("should")) {
                    if (rand.nextBoolean())
                        neg = "not ";
                    else {
                        neg = "n't ";
                        english.deleteCharAt(english.length()-1);
                    }
                }
                else if (rand.nextBoolean())
                    neg = "don't ";
                else
                    neg = "do not ";
            }
            else {
                if (lfeat.tense == PAST)
                    neg = "has not ";
                else if (lfeat.tense == FUTURE)
                    neg = "will not ";
                else
                    neg = "not ";
            }
        }
        if (debug) System.out.println("verbForm(): neg: " + neg);
        if (!StringUtil.emptyString(lfeat.subj) && lfeat.subj.equals("You"))
            return capital(neg + word) + nounForm;
        if (nounForm == "") // only modify if we don't use an auxilliary
            root = WordNet.wn.verbRootForm(word,word.toLowerCase());
        if (root == null)
            root = word;

        if (debug) System.out.println("verbForm(): word: " + word);
        if (debug) System.out.println("verbForm(): root: " + root);
        if (debug) System.out.println("verbForm(): nounForm: " + nounForm);
        String copula = conjCopula(negated,lfeat.tense,plural);
        if (english.toString().endsWith("is ") && copula.startsWith("is"))
            copula = copula.substring(2);
        if (copula.endsWith("won't"))
            copula = copula + " ";
        if (isProgressive(lfeat.tense)) {
            if (WordNet.wn.exceptVerbProgHash.containsKey(root)) {
                word = copula + WordNet.wn.exceptVerbProgHash.get(root);
                return word + nounForm;
            }
            else {
                if (root.endsWith("e") && !root.endsWith("ee"))
                    result = copula + root.substring(0,root.length()-1) + "ing" + nounForm;
                else if (root.matches(".*[aeiou][aeiou][bcdfglmnprstvz]$") || root.matches(".*[bcdfglmnprstvz]er$"))
                    result = copula + root + "ing" + nounForm;
                else if (root.matches(".*[aeiou][bcdfglmnprstvz]$"))
                    result = copula + root + root.substring(root.length()-1) + "ing" + nounForm;
                else
                    result = copula + root + "ing" + nounForm;
                if (debug) System.out.println("verbForm(): result: " + result);
                return result;
            }
        }
        if (lfeat.tense == PAST) {
            if (WordNet.wn.exceptionVerbPastHash.containsKey(root))
                word = WordNet.wn.exceptionVerbPastHash.get(root);
            else {
                if (root.endsWith("e"))
                    word = root + "d";
                else
                  word = root + "ed";
            }
            return neg + word + nounForm;
        }
        String es = "s";
        if (plural)
            es = "";
        if (lfeat.tense == PRESENT || lfeat.tense == NOTIME) {
            if (root.endsWith("ch") || root.endsWith("sh") || root.endsWith("ss") || root.equals("go"))
                es = "es";
            if (root.endsWith("e"))
                es = "s";
            if (negated) {
                return "doesn't " + root + nounForm;
            }
            else {
                if (root.endsWith("y") && !root.matches(".*[aeiou]y$"))
                    return capital(root.substring(0, root.length() - 1) + "ies");
                return capital(root) + es + nounForm;
            }
        }
        if (lfeat.tense == FUTURE) {
            if (neg.startsWith("do"))
                neg = "not ";
            if (rand.nextBoolean() && !StringUtil.emptyString(neg))
                return "won't " + root + nounForm;
            if (!negated)
                return "will " + root + nounForm;
            return neg + root + nounForm;
        }
        if (root.endsWith("y") && !root.matches(".*[aeiou]y$") && !negated && nounForm == "")
            return capital(root.substring(0,root.length()-1) + "ies");
        if (lfeat.tense != NOTIME)
            System.out.println("Error in verbForm(): time is unallowed value: " + lfeat.tense);
        if (negated)
            return "doesn't " + root + nounForm;
        else {
            if (nounForm == "")
                return capital(root) + es;
            else
                return capital(root) + nounForm + es;
        }
    }

    /** ***************************************************************
     * @param term is a SUMO term
     * @return English in the attribute and SUMO in the value
     */
    public AVPair getQuantity(String term) {

        if (debug) System.out.println("getQuantity(): term: " + term);
        float val = rand.nextFloat() * 20;
        ArrayList<Formula> forms = kb.askWithRestriction(0,"roomTempState",1,term);
        if (debug) System.out.println("getQuantity(): forms: " + forms);
        if (forms == null || forms.size() == 0)
            return null;
        Formula f = forms.get(0);
        String state = f.getStringArgument(2);
        if (StringUtil.emptyString(state))
            return null;
        String unitType = "UnitOfMass";
        if (state.equals("Liquid") || state.equals("Gas"))
            unitType = "UnitOfVolume";
        if (debug) System.out.println("getQuantity(): unitType: " + unitType);
        HashSet<String> units = kb.kbCache.getInstancesForType(unitType);
        if (debug) System.out.println("getQuantity(): units: " + units);
        String unit = (String) units.toArray()[rand.nextInt(units.size())];
        String unitEng = kb.getTermFormat("EnglishLanguage",unit);
        if (WordNet.wn.exceptionNounPluralHash.containsKey(unitEng))
            unitEng = WordNet.wn.exceptionNounPluralHash.get(unitEng);
        else
            unitEng = unitEng + "s";
        int decimalPlaces = rand.nextInt(5);
        String english = String.format("%." + Integer.toString(decimalPlaces) + "f", val) + " " + unitEng + " ";
        String sumo = "(measure ?DO (MeasureFn " + Float.toString(val) + " " + unit + ")) ";
        AVPair result = new AVPair(english,sumo);
        if (debug) System.out.println("getQuantity(): term: " + term);
        return result;
    }

    /** ***************************************************************
     * @param term is a SUMO term
     * @param avp is a hack to return whether there was a plural, and its count
     */
    public String nounFormFromTerm(String term, AVPair avp, String other) {

        String word = kb.getTermFormat("EnglishLanguage",term);
        if (word == null) {
            System.out.println("nounFormFromTerm(): no term format for " + term);
            return null;
        }
        if (kb.isInstance(term)) {
            if (kb.kbCache.isInstanceOf(term,"Human"))
                return word;
            else {
                if (StringUtil.emptyString(other))
                    return capital("the ") + word;
                else
                    return capital(word);
            }

        }
        boolean subst = kb.isSubclass(term,"Substance");
        if (subst) {
            if (rand.nextBoolean()) {
                AVPair quant = getQuantity(term);
                if (quant == null)
                    return capital("some ") + word;
                else {
                    avp.attribute = quant.attribute;
                    avp.value = quant.value;
                    return capital(avp.attribute) + "of " + word;
                }
            }
            else
                return capital("some ") + word;
        }
        String number = "";
        if (biasedBoolean(1,5) && kb.isSubclass(term,"CorpuscularObject")) { // occasionally make this a plural or count
            int index = rand.nextInt(numbers.size());  // how many of the object
            avp.value = Integer.toString(index);
            if (rand.nextBoolean())
                number = numbers.get(index);  // sometimes spell out the number as a word...
            else
                number = Integer.toString(index);  // ...and sometimes just use the number
            String suffix = "";
            if (index != 1) {
                avp.attribute = "true";
                if (word.contains(" ")) { // handle multi-word nouns
                    String headword = word.substring(word.lastIndexOf(" ")+1);
                    if (debug) System.out.println("nounFormFromTerm(): headword: " + headword);
                    if (WordNet.wn.exceptionNounPluralHash.containsKey(headword)) {
                        String newheadword = WordNet.wn.exceptionNounPluralHash.get(headword);
                        word = word.replace(headword,newheadword);
                    }
                    else
                        word = word + "s";
                }
                else if (WordNet.wn.exceptionNounPluralHash.containsKey(word))
                    word = WordNet.wn.exceptionNounPluralHash.get(word);
                else if (word.endsWith("y")) {
                    if (word.endsWith("ey"))
                        word = word.substring(0, word.length() - 2) + "ies";
                    else
                        word = word.substring(0, word.length() - 1) + "ies";
                }
                else if (word.endsWith("ss"))
                    word = word + "es";
                else
                    word = word + "s";
            }
            else
                avp.attribute = "false";
        }
        else
            avp.attribute = "false";
        if (avp.attribute.equals("true"))
            return capital(number) + " " + word;
        if (word.matches("^[aeiouAEIOH].*")) // pronouncing "U" alone is like a consonant, H like a vowel
            return capital("an ") + word;
        return capital("a ") + word;
    }

    /** ***************************************************************
     * Generate a boolean true value randomly num out of max times.
     * So biasedBoolean(8,10) generates a true most of the time
     * (8 out of 10 times on average)
     */
    public static String capital(String s) {

        if (debug) System.out.println("capital(): startOfSentence: " + startOfSentence);
        if (debug) System.out.println("capital(): s: " + s);
        if (StringUtil.emptyString(s))
            return s;
        if (!startOfSentence)
            return s;
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

    /** ***************************************************************
     * Generate a boolean true value randomly num out of max times.
     * So biasedBoolean(8,10) generates a true most of the time
     * (8 out of 10 times on average)
     */
    public static boolean biasedBoolean(int num, int max) {

        int val = rand.nextInt(max);
        return val < num;
    }

    /** ***************************************************************
     * Add SUMO content about a plural noun
     * @param prop is the formula to append to
     * @param term is the SUMO type of the noun
     * @param plural is the count of the plural as a String integer in the value field
     * @param var is the variable for the term in the formula
     */
    private static void addSUMOplural(StringBuffer prop, String term, AVPair plural, String var) {

        if (debug) System.out.println("addSUMOplural(): prop: " + prop);
        //prop.append("(instance " + var + " Collection) ");
        prop.append("(memberType " + var + " " + term + ") ");
        prop.append("(memberCount " + var + " " + plural.value + ") ");
    }

    /** ***************************************************************
     * Add SUMO content about a plural noun
     * @param prop is the formula to append to
     */
    private void addBodyPart(StringBuffer english, StringBuffer prop, LFeatures lfeat) {

        String bodyPart = lfeat.bodyParts.getNext();     // get a body part
        AVPair plural = new AVPair();
        english.append(capital(nounFormFromTerm(bodyPart,plural,"")) + " ");
        if (plural.attribute.equals("true"))
            addSUMOplural(prop,bodyPart,plural,"?O");
        else
            prop.append("(instance ?O " + bodyPart + ") ");
        prop.append("(possesses ?H ?O) ");
    }

    /** ***************************************************************
     * Generate the subject of the sentence conforming to the verb frame
     * for a human
     */
    public void generateHumanSubject(StringBuffer english, StringBuffer prop,
                                     LFeatures lfeat) {

        if (debug) System.out.println("human subject for (prop,synset,word): " +
                prop + ", " + lfeat.verbSynset + ", " + lfeat.verb);
        if (debug) System.out.println("generateHumanSubject(): startOfSentence: " + startOfSentence);
        StringBuffer type = new StringBuffer();
        StringBuffer name = new StringBuffer();
        if ((lfeat.attitude.equals("None") || lfeat.attitude.equals("says")) &&
                lfeat.modal.attribute.equals("None") && english.length() == 0)
            generateHuman(english,prop,true,"?H",type,name,lfeat);
        else
            generateHuman(english,prop,false,"?H",type,name,lfeat);
        lfeat.subj = type.toString();
        lfeat.subjName = name.toString();
        if (!lfeat.subj.equals("You"))
            startOfSentence = false;
        else {
            if (biasedBoolean(1,5)) {
                english.append("Please, ");
                startOfSentence = false;
            }
            else if (biasedBoolean(1,5)) {
                english.append("You should ");
                startOfSentence = false;
            }
        }
        if (lfeat.attitude.equals("says") && lfeat.subj.equals("You") && // remove "that" for says "You ..."
                english.toString().endsWith("that \"")) {
            english.delete(english.length() - 7, english.length());
            english.append(" \""); // restore space and quote
        }
        if (biasedBoolean(1,5) && english.length() == 0) {
            lfeat.subj = "who";
            lfeat.question = true;
            english.append(capital(lfeat.subj) + " ");
            startOfSentence = false;
        }
        else if (kb.isInstanceOf(lfeat.subj,"SocialRole")) { // a plumber... etc
            if (lfeat.framePart.startsWith("Somebody's (body part)")) {
                if (lfeat.subj.equals("You"))
                    english.append(capital("your"));
                else {
                    english.deleteCharAt(english.length()-1); // delete trailing space
                    english.append("'s ");
                }
                addBodyPart(english,prop,lfeat);
            }
            else {
                // english.append(capital(nounFormFromTerm(lfeat.subj)) + " "); // already created in generateHuman
            }
        }
        else {                                              // John... etc
            if (lfeat.framePart.startsWith("Somebody's (body part)")) {
                // english.append(capital(lfeat.subj) + "'s ");
                english.append("'s ");
                addBodyPart(english,prop,lfeat);
            }
            else {
                if (!lfeat.subj.equals("You")) {  // if subj is you-understood, don't generate subject
                    //english.append(capital(lfeat.subj) + " ");
                    //prop.append("(instance ?H Human) ");
                    //prop.append("(names \"" + lfeat.subj + "\" ?H) ");
                }
            }
        }
        removeFrameSubject(lfeat);
        if (debug) System.out.println("generateHumanSubject(2): startOfSentence: " + startOfSentence);
    }

    /** ***************************************************************
     */
    public void removeFrameSubject(LFeatures lfeat) {

        if (lfeat.framePart.startsWith("Something is"))
            lfeat.framePart = lfeat.framePart.substring(13);
        else if (lfeat.framePart.startsWith("It is"))
            lfeat.framePart = lfeat.framePart.substring(5);
        else if (lfeat.framePart.startsWith("Something"))
            lfeat.framePart = lfeat.framePart.substring(10);
        else if (lfeat.framePart.startsWith("It"))
            lfeat.framePart = lfeat.framePart.substring(3);
        else if (lfeat.framePart.startsWith("Somebody's (body part)"))
            lfeat.framePart = lfeat.framePart.substring(23);
        else if (lfeat.framePart.startsWith("Somebody"))
            lfeat.framePart = lfeat.framePart.substring(9);
    }

    /** ***************************************************************
     * Generate the subject of the sentence conforming to the verb frame
     * for a thing
     */
    public void generateThingSubject(StringBuffer english, StringBuffer prop,
                                LFeatures lfeat) {

        if (debug) System.out.println("non-human subject for (prop,synset,word): " +
                prop + ", " + lfeat.verbSynset + ", " + lfeat.verb);
        if (biasedBoolean(1,5) && english.length() == 0) {
            lfeat.subj = "what";
            lfeat.question = true;
            english.append(capital(lfeat.subj) + " ");
        }
        else if (lfeat.framePart.startsWith("Something")) {
            String term = lfeat.objects.getNext();
            lfeat.subj = term;
            AVPair plural = new AVPair();
            english.append(capital(nounFormFromTerm(term,plural,"")) + " ");
            if (plural.attribute.equals("true")) {
                addSUMOplural(prop, term, plural, "?H");
                lfeat.subjectPlural = true;
            }
            else
                prop.append("(instance ?H " + term + ") ");
            if (lfeat.framePart.startsWith("Something is")) {
                english.append("is ");
            }
            else
                lfeat.framePart = lfeat.framePart.substring(10);
        }
        else {  // frame must be "It..."
            if (lfeat.framePart.startsWith("It is")) {
                english.append("It is ");
            }
            else {
                english.append("It ");
            }
        }
        removeFrameSubject(lfeat);
        startOfSentence = false;
    }

    /** ***************************************************************
     */
    public boolean questionWord(String q) {

        return q.equalsIgnoreCase("who") || q.equalsIgnoreCase("what") || q.equalsIgnoreCase("when did") ||
                q.equalsIgnoreCase("did") || q.equalsIgnoreCase("where did") || q.equalsIgnoreCase("why did");
    }

    /** ***************************************************************
     * Generate the subject of the sentence conforming to the verb frame
     */
    public void generateSubject(StringBuffer english, StringBuffer prop,
                                  LFeatures lfeat) {

        if (debug) System.out.println("generateSubject(): english: " + english);
        if (debug) System.out.println("generateSubject(): attitude: " + lfeat.attitude);
        if (debug) System.out.println("generateSubject(): modal: " + lfeat.modal);
        if (debug) System.out.println("generateSubject(): startOfSentence: " + startOfSentence);
        if (!StringUtil.emptyString(lfeat.subj)) {  // for testing, allow for a pre-set subject
            System.out.println("generateSubject(): non-empty subj " + lfeat.subj + " returning");
            english.append(lfeat.subjName + " ");
            removeFrameSubject(lfeat);
            return;
        }
        if (lfeat.framePart.startsWith("It") || lfeat.framePart.startsWith("Something")) {
            generateThingSubject(english, prop, lfeat);
        }
        else { // Somebody
            generateHumanSubject(english, prop, lfeat);
        }
        if (debug) System.out.println("generateSubject(): english: " + english);
        if (debug) System.out.println("generateSubject(): lfeat.framePart: " + lfeat.framePart);
        if (debug) System.out.println("generateSubject(): startOfSentence: " + startOfSentence);
    }

    /** ***************************************************************
     */
    public void generateVerb(boolean negated,StringBuffer english, StringBuffer prop,
                                String proc, String word, LFeatures lfeat) {

        if (debug) System.out.println("generateVerb(): word,proc,subj: " + word + ", " + proc + ", " + lfeat.subj);
        english.append(verbForm(proc,negated,word,lfeat.subjectPlural,english,lfeat) + " ");
        prop.append("(instance ?P " + proc + ") ");
        if (lfeat.framePart.startsWith("It") ) {
            System.out.println("non-human subject for (prop,synset,word): " +
                    prop + ", " + lfeat.verbSynset + ", " + lfeat.verb);
        }
        else if (lfeat.framePart.startsWith("Something"))
            prop.append("(involvedInEvent ?P ?H) ");
        else if (lfeat.subj != null &&
                  (kb.isSubclass(lfeat.subj,"AutonomousAgent") ||
                   kb.isInstanceOf(lfeat.subj,"SocialRole") ||
                   lfeat.subj.equals("You"))) {
            if (kb.isSubclass(proc,"IntentionalProcess"))
                prop.append("(agent ?P ?H) ");
            else if (kb.isSubclass(proc,"BiologicalProcess"))
                prop.append("(experiencer ?P ?H) ");
            else
                prop.append("(agent ?P ?H) ");
        }
        else {
            if (debug) System.out.println("generateVerb() non-Agent, non-Role subject " + lfeat.subj + " for action " + proc);
            prop.delete(0,prop.length());
            return;
        }
        // if (time == -1) do nothing extra
        if (lfeat.tense == PAST)
            prop.append("(before (EndFn (WhenFn ?P)) Now) ");
        if (lfeat.tense == PRESENT)
            prop.append("(temporallyBetween (BeginFn (WhenFn ?P)) Now (EndFn (WhenFn ?P))) ");
        if (lfeat.tense == FUTURE)
            prop.append("(before Now (BeginFn (WhenFn ?P))) ");
        if (lfeat.framePart.startsWith("----s")) {
            if (lfeat.framePart.length() < 6)
                lfeat.framePart = "";
            else
                lfeat.framePart = lfeat.framePart.substring(6);
        }
        else if (lfeat.framePart.trim().startsWith("----ing")) {
            if (lfeat.framePart.trim().length() < 8)
                lfeat.framePart = "";
            else
                lfeat.framePart = lfeat.framePart.trim().substring(8);
        }
        else
            if (debug) System.out.println("Error in generateVerb(): bad format in frame: " + lfeat.framePart);
        if (debug) System.out.println("generateVerb(): english: " + english);
        if (debug) System.out.println("generateVerb(): prop: " + prop);
        if (debug) System.out.println("generateVerb(): lfeat.framePart: " + lfeat.framePart);
    }

    /** ***************************************************************
     * How many occurrences remaining in the frame of 'something' and 'someone'
     */
    public int countSomes(String frame) {

        String str = "frame";
        String something = "something";
        String somebody = "somebody";
        return (str.split(something,-1).length-1) + (str.split(somebody,-1).length-1);
    }

    /** ***************************************************************
     * Get a person or thing.  Fill in directName, directtype, preposition
     * as a side effect in lfeat
     */
    public void getDirect(StringBuffer english, LFeatures lfeat) {

        if (debug) System.out.println("getDirect(): lfeat.framePart: " + lfeat.framePart);
        if (lfeat.framePart.trim().startsWith("somebody") || lfeat.framePart.trim().startsWith("to somebody") ) {
            if (rand.nextBoolean()) {
                lfeat.directName = lfeat.humans.getNext();
                lfeat.directType = "Human";
            }
            else {
                lfeat.directType = lfeat.socRoles.getNext();
            }
            if (lfeat.framePart.trim().startsWith("to somebody"))
                lfeat.directPrep = "to ";
            int index = lfeat.framePart.indexOf("somebody");
            if (index + 10 < lfeat.framePart.length())
                lfeat.framePart = lfeat.framePart.substring(index + 9);
            else
                lfeat.framePart = "";
        }
        else if (lfeat.framePart.trim().startsWith("something") || lfeat.framePart.trim().startsWith("on something")) {
            lfeat.directType = lfeat.objects.getNext();
            if (lfeat.framePart.contains("on something"))
                lfeat.directPrep = "on ";
            int index = lfeat.framePart.indexOf("something");
            if (index + 10 < lfeat.framePart.length())
                lfeat.framePart = lfeat.framePart.substring(index + 10);
            else
                lfeat.framePart = "";
        }
        else if (lfeat.framePart.trim().startsWith("to INFINITIVE") || lfeat.framePart.trim().startsWith("INFINITIVE") ||
                lfeat.framePart.trim().startsWith("whether INFINITIVE")) {
            getVerb(lfeat,true);
            if (lfeat.framePart.trim().startsWith("to")) {
                lfeat.directPrep = "to ";
            }
            else if (lfeat.framePart.trim().startsWith("whether")) {
                lfeat.directPrep = "whether";
                lfeat.secondVerb = "to " + lfeat.secondVerb;
            }
            else
                lfeat.secondVerb = "to " + lfeat.secondVerb;
            lfeat.secondVerbType = lfeat.secondVerbType;
            int index = lfeat.framePart.indexOf("INFINITIVE");
            if (index + 10 < lfeat.framePart.length())
                lfeat.framePart = lfeat.framePart.substring(index + 10);
            else
                lfeat.framePart = "";
        }
        else if (lfeat.framePart.trim().startsWith("VERB-ing")) {
            getVerb(lfeat,true);
            lfeat.tense = PROGRESSIVE;
            lfeat.secondVerb = verbForm(lfeat.secondVerbType,false,lfeat.secondVerb,false,english,lfeat);
            if (lfeat.secondVerb.startsWith("is "))
                lfeat.secondVerb = lfeat.secondVerb.substring(3);
            lfeat.framePart = "";
        }
        if (debug) System.out.println("getDirect(2): lfeat.directType: " + lfeat.directType);
        if (debug) System.out.println("getDirect(2): lfeat.directName: " + lfeat.directName);
        if (debug) System.out.println("getDirect(2): lfeat.framePart: " + lfeat.framePart);
        if (debug) System.out.println("getDirect(2): lfeat.prep: " + lfeat.directPrep);
    }

    /** ***************************************************************
     */
    public void addSecondVerb(StringBuffer english, StringBuffer prop,
                                     LFeatures lfeat) {

        if (!StringUtil.emptyString(lfeat.directPrep))
            english.append(lfeat.directPrep + " ");
        english.append(lfeat.secondVerb + " ");
        prop.append("(instance ?V2 " + lfeat.secondVerbType + ") ");
        prop.append("(refers ?DO ?V2) ");
    }

    /** ***************************************************************
     * @param prop will be empty on return if the sentence so far is rejected
     */
    public void generateDirectObject(StringBuffer english, StringBuffer prop,
                             LFeatures lfeat) {

        if (debug) System.out.println("generateDirectObject(1): english: " + english);
        if (debug) System.out.println("generateDirectObject(1): prep: " + lfeat.directPrep);
        if (debug) System.out.println("generateDirectObject(1): type: " + lfeat.directType);
        if (lfeat.framePart == "") {
            prop.delete(0,prop.length());
            return;
        }
        String role = "patient";
        String other = ""; // if there
        if (StringUtil.emptyString(lfeat.directType)) // allow pre-set objects for testing
            getDirect(english,lfeat);
        else
            System.out.println("generateDirectObject(): non-empty object, using specified input " + lfeat.directType);
        if (lfeat.directType != null && lfeat.directType.equals(lfeat.subj))
            if (!lfeat.directType.equals("Human") ||
                    (lfeat.directType.equals("Human") && lfeat.directName.equals(lfeat.subjName)))
                other = RandSet.listToEqualPairs(others).getNext() + " ";
        if (lfeat.directType != null && lfeat.directType.equals("Human"))
            prop.append(genSUMOForHuman(lfeat, lfeat.directName, "?DO"));
        AVPair plural = new AVPair();
        if (lfeat.secondVerbType != "") {
            addSecondVerb(english,prop,lfeat);
            if (debug) System.out.println("generateDirectObject(2): added second verb, english: " + english);
        }
        else if (kb.isSubclass(lfeat.verbType, "Translocation") &&
                (kb.isSubclass(lfeat.directType,"Region") || kb.isSubclass(lfeat.directType,"StationaryObject"))) {
            //    (kb.isSubclass(dprep.noun,"Region") || kb.isSubclass(dprep.noun,"StationaryObject"))) {
            if (debug) System.out.println("generateDirectObject(3): location, region or object: " + english);
            if (lfeat.directType.equals("Human"))
                english.append("to " + other + nounFormFromTerm(lfeat.directName,plural,other) + " ");
            else
                english.append("to " + other + nounFormFromTerm(lfeat.directType,plural,other) + " ");
            if (plural.attribute.equals("true"))
                addSUMOplural(prop,lfeat.directType,plural,"?DO");
            else
                prop.append("(instance ?DO " + lfeat.directType + ") ");
            prop.append("(destination ?P ?DO) ");
            role = "destination";
        }
        else if (lfeat.directType != null) {
            if (debug) System.out.println("generateDirectObject(4): some other type, english: " + english);
            if (debug) System.out.println("generateDirectObject(4): prep: " + lfeat.directPrep);
            if (lfeat.directType.equals("Human"))
                english.append(lfeat.directPrep + other + lfeat.directName + " ");
            else
                english.append(lfeat.directPrep + other + nounFormFromTerm(lfeat.directType,plural,other) + " ");
            if (plural.attribute.equals("true"))
                addSUMOplural(prop,lfeat.directType,plural,"?DO");
            else
                prop.append("(instance ?DO " + lfeat.directType + ") ");

            if (lfeat.directPrep.equals("to ")) {
                prop.append("(destination ?P ?DO) ");
                role = "destination";
            }
            else if (lfeat.directPrep.equals("on ")) {
                prop.append("(orientation ?P ?DO On) ");
                role = "orientation";
            }
            else {
                if (kb.isSubclass(lfeat.verbType,"Transfer")) {
                    prop.append("(objectTransferred ?P ?DO) ");
                    role = "objectTransferred";
                }
                else {
                    prop.append("(patient ?P ?DO) ");
                }
            }
        }
        if (!checkCapabilities(lfeat.verbType,role,lfeat.directType)) {
            System.out.println("generateDirectObject() rejecting (proc,role,obj): " + lfeat.verbType + ", " + role + ", " + lfeat.directType);
            prop.delete(0,prop.length());
            english.delete(0,english.length());
            return;
        }
        if (debug) System.out.println("generateDirectObject(5): english: " + english);
        if (debug) System.out.println("generateDirectObject(5): prop: " + prop);
        if (debug) System.out.println("generateDirectObject(5): plural: " + plural);
        if (debug) System.out.println("generateDirectObject(5): lfeat.framePart: " + lfeat.framePart);
    }

    /** ***************************************************************
     */
    private String closeParens(LFeatures lfeat) {

        StringBuffer result = new StringBuffer();
        if (debug) System.out.println("closeParens(): lfeat.attitude: " + lfeat.attitude);
        if (debug) System.out.println("closeParens(): lfeat.attNeg: " + lfeat.attNeg);
        if (debug) System.out.println("closeParens(): lfeat.modal.attribute: " + lfeat.modal.attribute);
        if (debug) System.out.println("closeParens(): lfeat.negatedModal: " + lfeat.negatedModal);
        if (debug) System.out.println("closeParens(): lfeat.negatedBody: " + lfeat.negatedBody);
        if (lfeat.negatedBody) result.append(")");
        result.append(")) "); // close off the starting 'exists' and 'and'
        if (!lfeat.modal.attribute.equals("None")) result.append(lfeat.modal.attribute + ")");
        if (lfeat.negatedModal && !lfeat.modal.attribute.equals("None")) result.append(")");
        if (!lfeat.attitude.equals("None")) {
            result.append(")))");
            if (lfeat.attNeg) result.append(")");
        }
        return result.toString();
    }

    /** ***************************************************************
     * extract prepositions and auxiliaries from a verb frame.  Put in the right
     * direct or indirect preposition field in lfeat
     * 15-19, 30, 31 are preps to indirect objects
     */
    public static void getPrepFromFrame(LFeatures lfeat) {

        if (debug) System.out.println("getPrepFromFrame(): frame: " + lfeat.frame);
        if (StringUtil.emptyString(lfeat.framePart)) {
            System.out.println("Error in getPrepFromFrame(): empty frame");
            return;
        }
        int fnum = WordNetUtilities.verbFrameNum(lfeat.framePart);
        if (debug) System.out.println("getPrepFromFrame(): frame num: " + fnum);
        Pattern pattern = Pattern.compile("(to |on |from |with |of |that |into |whether )", Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(lfeat.framePart);
        boolean matchFound = matcher.find();
        if (matchFound) {
            String prep = matcher.group(1).trim() + " ";
            if ((fnum >=15 && fnum <=19) || fnum==30 || fnum==31)
                lfeat.indirectPrep = prep;
            else
                lfeat.directPrep = prep;
        }
        else
            return;
    }

    /** ***************************************************************
     * extract prepositions and auxiliaries from a verb frame
     */
    public static String getCaseRoleFromPrep(String prep) {

        if (prep.equals("to "))
            return ("destination");
        if (prep.equals("from "))
            return ("origin");
        if (prep.equals("with "))
            return ("instrument");
        if (prep.equals("of "))
            return ("patient");
        if (prep.equals("on "))
            return ("destination");
        return "involvedInEvent";
    }

    /** ***************************************************************
     * Also handle the INFINITIVE verb frame
     */
    public boolean generateIndirectObject(int indCount,
                                          StringBuffer english, StringBuffer prop,
                                          LFeatures lfeat,
                                          boolean onceWithoutInd) {

        if (debug) System.out.println("generateIndirectObject(): sentCount: " + sentCount);
        if (!StringUtil.emptyString(lfeat.framePart))
            getPrepFromFrame(lfeat);
        if (lfeat.indirectPrep != "")
            lfeat.indirectPrep = lfeat.directPrep + " ";
        if (debug) System.out.println("generateIndirectObject(): frame: " + lfeat.framePart);
        if (debug) System.out.println("generateIndirectObject(): indirect prep: " + lfeat.indirectPrep);
        if (lfeat.framePart != "" && lfeat.framePart.contains("somebody") || lfeat.framePart.contains("something")) {
            String prep = null;
            getIndirect(lfeat);
            if (!StringUtil.emptyString(lfeat.indirectPrep))
                prep = getCaseRoleFromPrep(lfeat.indirectPrep);
            if (StringUtil.emptyString(prep))
                prep = "patient";
            AVPair plural = new AVPair();
            if (lfeat.indirectType.equals("Human"))
                english.append(lfeat.indirectPrep + lfeat.indirectName);
            else
                english.append(lfeat.indirectPrep + nounFormFromTerm(lfeat.indirectType,plural,""));
            if (debug) System.out.println("generateIndirectObject(): plural: " + plural);

            if (plural.attribute.equals("true"))
                addSUMOplural(prop,lfeat.indirectType,plural,"?IO");
            else
                prop.append("(instance ?IO " + lfeat.indirectType + ") ");
            if (lfeat.framePart.contains("somebody"))
                prop.append(genSUMOForHuman(lfeat,lfeat.indirectName,"?IO"));
            else
                prop.append("(instance ?IO " + lfeat.indirectType + ")");

            if (english.toString().endsWith(" "))
                english.delete(english.length()-1,english.length());
            if (lfeat.polite && !lfeat.politeFirst)
                english.append(RandSet.listToEqualPairs(endings).getNext());
            if (lfeat.polite) //lfeat.subj.equals("You") && !english.toString().startsWith("Please") && rand.nextBoolean())
                english.append("!");
            else if (english.indexOf(" ") != -1 &&
                    questionWord(english.toString().substring(0,english.toString().indexOf(" "))))
                english.append("?");
            else
                english.append(".");
            if (lfeat.attitude != null && lfeat.attitude.equals("says")) {
                english.append("\"");
            }
            prop.append("(" + prep + " ?P ?IO) ");

            if (lfeat.subj != null && lfeat.subj.equals("You")) {
                String newProp = prop.toString().replace(" ?H"," You");
                prop.setLength(0);
                prop.append(newProp);
            }
            prop.append(closeParens(lfeat));
            onceWithoutInd = false;
            if (debug) System.out.println("generateIndirectObject(): " + english);
            if (KButilities.isValidFormula(kb,prop.toString())) {
                if (debug) System.out.println("generateIndirectObject(): valid formula: " + Formula.textFormat(prop.toString()));
                String finalEnglish = english.toString().replaceAll("  "," ");
                englishFile.println(english);
                logicFile.println(prop);
                sentCount++;
            }
            else {
                System.out.println("generateIndirectObject(): Error invalid formula: " + Formula.textFormat(prop.toString()));
                System.out.println(english);
            }
        }
        else if (lfeat.framePart.contains("INFINITIVE")) {
            getVerb(lfeat,true);
            if (debug) System.out.println("generateIndirectObject(): word: " + lfeat.secondVerb);
            if (debug) System.out.println("generateIndirectObject(): frame: " + lfeat.framePart);
            if (lfeat.framePart.contains("to"))
                lfeat.secondVerb = "to " + lfeat.secondVerb;
            if (debug) System.out.println("generateIndirectObject(2): word: " + lfeat.secondVerb);
            english.append(lfeat.secondVerb + " ");
            if (lfeat.polite && !lfeat.politeFirst)
                english.append(RandSet.listToEqualPairs(endings).getNext());
            if (english.toString().endsWith(" "))
                english.delete(english.length()-1,english.length());
            if (lfeat.subj.equals("You") && !english.toString().startsWith("Please") && rand.nextBoolean())
                english.append("!");
            else if (english.indexOf(" ") != -1 &&
                    questionWord(english.toString().substring(0,english.toString().indexOf(" "))))
                english.append("?");
            else
                english.append(".");
            if (lfeat.attitude != null && lfeat.attitude.equals("says")) {
                english.append("\"");
            }
            prop.append(closeParens(lfeat));
            if (lfeat.subj != null && lfeat.subj.equals("You")) {
                String newProp = prop.toString().replace(" ?H"," You");
                prop.setLength(0);
                prop.append(newProp);
            }
            if (debug) System.out.println("generateIndirectObject(): " + english);
            if (KButilities.isValidFormula(kb,prop.toString())) {
                if (debug) System.out.println("generateIndirectObject(): valid formula: " + Formula.textFormat(prop.toString()));
                String finalEnglish = english.toString().replaceAll("  "," ");
                englishFile.println(english);
                logicFile.println(prop);
                sentCount++;
            }
            else {
                System.out.println("generateIndirectObject(): Error invalid formula: " + Formula.textFormat(prop.toString()));
                System.out.println(english);
            }
        }
        else {  // close off the formula without an indirect object
            if (debug) System.out.println("generateIndirectObject(): attitude: " + lfeat.attitude);
            if (lfeat.polite && !lfeat.politeFirst)
                english.append(RandSet.listToEqualPairs(endings).getNext());
            if (english.toString().endsWith(" "))
                english.delete(english.length()-1,english.length());
            if (!StringUtil.emptyString(lfeat.subj) && lfeat.subj.equals("You") && rand.nextBoolean())
                english.append("!");
            else if (english.indexOf(" ") != -1 &&
                    questionWord(english.toString().substring(0,english.toString().indexOf(" "))))
                english.append("?");
            else
                english.append(".");
            if (lfeat.attitude != null && lfeat.attitude.equals("says"))
                english.append("\"");
            prop.append(closeParens(lfeat));
            if (lfeat.subj != null && lfeat.subj.equals("You")) {
                String newProp = prop.toString().replace(" ?H"," You");
                prop.setLength(0);
                prop.append(newProp);
            }
            indCount = 0;
            if (!onceWithoutInd) {
                if (debug) System.out.println("====== generateIndirectObject(): " + english);
                if (KButilities.isValidFormula(kb,prop.toString())) {
                    if (debug) System.out.println("generateIndirectObject(): valid formula: " + Formula.textFormat(prop.toString()));
                    String finalEnglish = english.toString().replaceAll("  "," ");
                    englishFile.println(english);
                    logicFile.println(prop);
                    sentCount++;
                }
                else {
                    System.out.println("generateIndirectObject(): Error invalid formula: " + Formula.textFormat(prop.toString()));
                    System.out.println(english);
                }
            }
            onceWithoutInd = true;
        }
        return onceWithoutInd;
    }

    /** ***************************************************************
     */
    public boolean excludedVerb(String v) {

        if (debug) System.out.println("excludedVerb(): checking: " + v);
        if (compoundVerb(v))  // exclude compound verbs for now since the morphology is too difficult
            return true;
        if (verbEx.contains(v)) // check for specifically excluded verbs and their SUMO subclasses
            return true;
        for (String s : verbEx) {
            if (kb.isSubclass(v,s))
                return true;
        }
        if (debug) System.out.println("excludedVerb(): not excluded: " + v);
        return false;
    }

    /** ***************************************************************
     */
    public ArrayList<String> getVerbFramesForTerm(String term) {

        ArrayList<String> frames = new ArrayList<>();
        ArrayList<String> synsets = WordNetUtilities.getEquivalentVerbSynsetsFromSUMO(term);
        if (debug) System.out.println("GenSimpTestData.getVerbFramesForTerm(): synsets size: " +
                synsets.size() + " for term: " + term);
        if (synsets.size() == 0)
            return frames;
            //synsets = WordNetUtilities.getVerbSynsetsFromSUMO(term);
        for (String s : synsets) {
            ArrayList<String> words = WordNet.wn.getWordsFromSynset(s);
            for (String w : words) {
                ArrayList<String> newframes = WordNetUtilities.getVerbFramesForWord(s,w);
                if (newframes != null)
                    frames.addAll(newframes);
            }
        }
        return frames;
    }

    /** ***************************************************************
     * Get a person or thing
     */
    public void getIndirect(LFeatures lfeat) {

        if (debug) System.out.println("getIndirect(): frame: " + lfeat.framePart);
        if (lfeat.framePart.endsWith("somebody")) {
            if (rand.nextBoolean()) {
                lfeat.indirectName = lfeat.humans.getNext();
                lfeat.indirectType = "Human";
            }
            else {
                lfeat.indirectType = lfeat.socRoles.getNext();
            }
        }
        else if (lfeat.framePart.endsWith("something")) {
            lfeat.indirectType = lfeat.objects.getNext();
        }
        if (debug) System.out.println("getIndirect(): type: " + lfeat.indirectType);
    }

    /** ***************************************************************
     * Strip tense in some frames
     */
    public String stripTenseFromFrame(String frame) {

        if (frame.equals("Is is ----ing"))
            return "It ----s";
        return frame;
    }

    /** ***************************************************************
     * Skip frames not currently handled
     */
    public boolean skipFrame(String frame) {

        if (frame.contains("PP") || frame.contains("CLAUSE") || frame.contains("V-ing") || frame.contains("Adjective"))
            return true;
        else
            return false;
    }

    /** ***************************************************************
     * @return the word part of 9-digit synset concatenated with a "-" and root of the verb
     */
    private String getWordPart(String s) {

        if (s.length() < 11) {
            System.out.println("Error in getWordPart(): bad input: " + s);
            return "";
        }
        return s.substring(10);
    }

    /** ***************************************************************
     * @return the synset part of 9-digit synset concatenated with a "-" and root of the verb
     */
    private String getSynsetPart(String s) {

        if (s.length() < 11) {
            System.out.println("Error in getSynsetPart(): bad input: " + s);
            return "";
        }
        return s.substring(0,9);
    }

    /** ***************************************************************
     * Get a randomized next verb
     * @return an AVPair with an attribute of the SUMO term and the value
     * of the 9-digit synset concatenated with a "-" and root of the verb
     */
    public void getVerb(LFeatures lfeat, boolean second) {

        if (lfeat.testMode && !StringUtil.emptyString(lfeat.verb)) { // for testing
            System.out.println("getVerb(): non empty verb: " + lfeat.verb + " returning");
            return;
        }
        String proc = "";
        String word = "";
        String synset = "";
        ArrayList<String> synsets = null;
        do {
            do {
                proc = lfeat.processes.getNext();
                synsets = WordNetUtilities.getEquivalentVerbSynsetsFromSUMO(proc);
                if (synsets.size() == 0) // keep searching for processes with equivalent synsets
                    if (debug) System.out.println("getVerb(): no equivalent synsets for: " + proc);
            } while (excludedVerb(proc) || synsets.size() == 0); // too hard grammatically for now to have compound verbs
            if (debug) System.out.println("getVerb(): checking process: " + proc);
            if (debug) System.out.println("getVerb(): synsets size: " + synsets.size() + " for term: " + proc);
            synset = synsets.get(rand.nextInt(synsets.size()));
            ArrayList<String> words = WordNet.wn.getWordsFromSynset(synset);
            int count = 0;
            do {
                count++;
                word = words.get(rand.nextInt(words.size()));
            } while (count < words.size() && word.contains("_"));
        } while (word.contains("_"));  // if all the words in a synset are compound words, start over with a new Process
        if (debug) System.out.println("getVerb(): return word: " + word);
        if (second) {
            lfeat.secondVerbType = proc;
            lfeat.secondVerb = word;
            lfeat.secondVerbSynset = synset;
        }
        else {
            lfeat.verbType = proc;
            lfeat.verb = word;
            lfeat.verbSynset = synset;
        }
    }

    /** ***************************************************************
     */
    public static String getFormattedDate(LocalDate date) {

        int day = date.getDayOfMonth();
        if (!((day > 10) && (day < 19)))
            switch (day % 10) {
                case 1:
                    return "d'st' 'of' MMMM yyyy";
                case 2:
                    return "d'nd' 'of' MMMM yyyy";
                case 3:
                    return "d'rd' 'of' MMMM yyyy";
                default:
                    return "d'th' 'of' MMMM yyyy";
            }
        return "d'th' 'of' MMMM yyyy";
    }

    /** ***************************************************************
     * Create action sentences from a subject, preposition, direct object,
     * preposition and indirect object based on WordNet verb frames
     */
    public void addTimeDate(StringBuffer english,
                        StringBuffer prop,
                        LFeatures lfeat) {

        boolean hastime = false;
        boolean hasdate = false;
        int month = rand.nextInt(12)+1;
        int yearMult = 1;
        LocalDateTime now = LocalDateTime.now();
        int year = now.getYear();
        if (lfeat.tense == PAST || lfeat.tense == PASTPROG) {
            yearMult = -1;
        }
        int theYear = (rand.nextInt(100) * yearMult) + year;
        LocalDate d = LocalDate.of(theYear,month,1);
        YearMonth ym = YearMonth.from(d);
        LocalDate endOfMonth = ym.atEndOfMonth();
        int day = rand.nextInt(endOfMonth.getDayOfMonth()-1) + 1;
        d = d.withDayOfMonth(day);
        int hour = rand.nextInt(24);
        LocalTime t = LocalTime.of(hour,0,0);
        LocalDateTime ldt = LocalDateTime.of(year,month,day,hour,0,0);
        ArrayList<String> dateOptions = new ArrayList<>();
        dateOptions.add("d MMM uuuu"); dateOptions.add("dd-MM-yyyy"); dateOptions.add("EEE, d MMM yyyy");
        dateOptions.add(getFormattedDate(d));
        String dateOption = dateOptions.get(rand.nextInt(dateOptions.size()));
        if (biasedBoolean(2,5)) { // sometimes add a time
            english.append(capital("at "));
            DateTimeFormatter format = DateTimeFormatter.ofPattern("ha");
            english.append(t.format(format) + " ");
            prop.append("(instance ?T (HourFn " + hour + ")) (during ?P ?T) ");
            hastime = true;
            startOfSentence = false;
        }
        if (!hastime && biasedBoolean(2,5)) { // sometimes add a date
            DateTimeFormatter format = DateTimeFormatter.ofPattern(dateOption);
            english.append(capital("on "));
            english.append(d.format(format) + " ");
            prop.append("(instance ?T (DayFn " + day + " (MonthFn " + month + " (YearFn " + year + ")))) (during ?P ?T) ");
            hasdate = true;
            startOfSentence = false;
        }
        if (!hastime && !hasdate) { // sometimes add both
            english.append(capital("on "));
            DateTimeFormatter format = DateTimeFormatter.ofPattern(dateOption + " 'at' ha");
            prop.append("(instance ?T (HourFn " + hour + " (DayFn " + day +
                    " (MonthFn " + month + " (YearFn " + year + "))))) (during ?P ?T) ");
            english.append(ldt.format(format) + " ");
            startOfSentence = false;
        }
        if (debug) System.out.println("addTimeDate() startOfSentence: " + startOfSentence);
    }

    /** ***************************************************************
     */
    private void getFrame(LFeatures lfeat) {

        String frame = null;
        int count = 0;
        do {
            frame = lfeat.frames.get(rand.nextInt(lfeat.frames.size()));
            count++;
        } while (count < (lfeat.frames.size() * 2) && (StringUtil.emptyString(frame) || skipFrame(frame)));
        if (count >= (lfeat.frames.size() * 2) || StringUtil.emptyString(frame) || skipFrame(frame))
            return;
        frame = stripTenseFromFrame(frame);
        if (debug) System.out.println("getFrame() set frame to: " + frame);
        lfeat.framePart = frame;
        lfeat.frame = frame;
    }

    /** ***************************************************************
     * Create action sentences from a subject, preposition, direct object,
     * preposition and indirect object based on WordNet verb frames
     */
    public void genProc(StringBuffer english,
                        StringBuffer prop,
                        LFeatures lfeat) {

        progressPrint();
        lfeat.tense = rand.nextInt(IMPERATIVE+1) - 1;
        if (lfeat.tense == IMPERATIVE && rand.nextBoolean()) {
            lfeat.polite = true;
            if (rand.nextBoolean())
                lfeat.politeFirst = false; // put at end
            else
                english.insert(0,RandSet.listToEqualPairs(requests).getNext() + english);
        }
        lfeat.frames = WordNetUtilities.getVerbFramesForWord(lfeat.verbSynset, lfeat.verb);
        if (lfeat.frames == null || lfeat.frames.size() == 0) {
            if (debug) System.out.println("genProc() no frames for word: " + lfeat.verb);
            return;
        }
        if (debug) System.out.println("genProc() frames: " + lfeat.frames);
        if (debug) System.out.println("genProc() time: " + printTense(lfeat.tense));
        if (debug) System.out.println("genProc() synset: " + lfeat.verbSynset);

        if (debug) System.out.println("genProc() word: " + lfeat.verb);
        getFrame(lfeat);
        if (lfeat.framePart == null) {
            if (debug) System.out.println("genProc() no acceptable frames for word: " + lfeat.verb);
            return;
        }
        if ((lfeat.attitude.equals("None") && lfeat.modal.attribute.equals("None")) ||
                lfeat.attitude.equals("says"))
            startOfSentence = true;
        if (!lfeat.testMode)
            lfeat.clearSVO(); // clear flags set for each sentence
        getPrepFromFrame(lfeat);
        if (debug) System.out.println("genProc() verb: '" + lfeat.verb + "'");
        if (debug) System.out.println("genProc() prepPhrases: " + prepPhrase);
        if (debug) System.out.println("genProc() preposition: " + lfeat.directPrep);
        if (debug) System.out.println("genProc() contains key: " + prepPhrase.containsKey(lfeat.verb));
        if (debug) System.out.println("genProc() empty prep: " + StringUtil.emptyString(lfeat.directPrep));
        if (debug) System.out.println("genProc() new prep: " + prepPhrase.get(lfeat.verb));
        if (prepPhrase.containsKey(lfeat.verb) && StringUtil.emptyString(lfeat.directPrep))
            lfeat.directPrep = prepPhrase.get(lfeat.verb);
        if (debug) System.out.println("genProc() frame: " + lfeat.framePart);
        if (debug) System.out.println("genProc() startOfSentence: " + startOfSentence);
        if (debug) System.out.println("genProc() preposition: " + lfeat.directPrep);
        lfeat.negatedBody = biasedBoolean(2,10);  // make it negated one time out of 5
        int indCount = 0;
        boolean onceWithoutInd = false;
        lfeat.indirectType = lfeat.objects.getNext();
        if (lfeat.negatedBody)
            prop.append("(not ");
        prop.append("(exists (?H ?P ?DO ?IO) (and ");
        if (biasedBoolean(1,10) && english.length() == 0)
            addTimeDate(english,prop,lfeat);
        if (debug) System.out.println("genProc(2) startOfSentence: " + startOfSentence);
        generateSubject(english, prop, lfeat);
        generateVerb(lfeat.negatedBody, english, prop, lfeat.verbType, lfeat.verb, lfeat);
        if (prop.equals(""))
            return;
        startOfSentence = false;
        generateDirectObject(english, prop, lfeat);
        if (StringUtil.emptyString(prop.toString()))
            return;
        generateIndirectObject(indCount, english, prop, lfeat, onceWithoutInd);
        lfeat.framePart = lfeat.frame;  // recreate frame destroyed during generation
    }

    /** ***************************************************************
     * Generate a person's name, or a SocialRole, or the diectic "You"
     *
     * @param english the English for the named human or role, as a
     *                side effect.
     * @param prop the SUMO for the named human or role, as a side
     *             effect.
     * @param allowYou is whether to allow returning the "You (understood)" form
     * @param var the existentially quantified variable for the
     *            human, an input.
     * @param type the type of the human, whether "Human" or
     *             "Attribute" or subAttribute, as a side effect.
     * @param name the name of the named human, as a side effect.
     *
     * lfeat.prevHumans are names or roles from previous parts of the
     *                 sentence that should not be repeated, modified
     *                 as a side effect.
     */
    public void generateHuman(StringBuffer english,
                              StringBuffer prop,
                              boolean allowYou,
                              String var,
                              StringBuffer type,
                              StringBuffer name,
                              LFeatures lfeat) {

        if (debug) System.out.println("GenSimpTestData.generateHuman(): allow You (understood): " + allowYou);
        boolean found = false;
        do {
            int val = rand.nextInt(10);
            if (allowYou && val < 2) { // "You" - no appended English or SUMO
                type.append("You");
                if (debug) System.out.println("GenSimpTestData.generateHuman(): generated a You (understood)");
            }
            else if (val < 6) { // a role
                type.append(lfeat.socRoles.getNext());
                prop.append("(attribute " + var + " " + type + ") ");
                AVPair plural = new AVPair();
                english.append(capital(nounFormFromTerm(type.toString(),plural,"")) + " ");
                if (lfeat.prevHumans.contains(type))  // don't allow the same name or role twice in a sentence
                    found = true;
                else
                    lfeat.prevHumans.add(type.toString());
            }
            else {  // a named human
                name.append(lfeat.humans.getNext());
                type.append("Human");
                prop.append("(instance " + var + " Human) ");
                prop.append("(names \"" + name + "\" " + var + ") ");
                english.append(name + " ");
                if (lfeat.prevHumans.contains(name)) // don't allow the same name or role twice in a sentence
                    found = true;
                else
                    lfeat.prevHumans.add(name.toString());
            }
        } while (found);
        if (!StringUtil.emptyString(lfeat.framePart) && lfeat.framePart.length() > 9 &&
                lfeat.framePart.toLowerCase().startsWith("somebody"))
            lfeat.framePart = lfeat.framePart.substring(9);
        if (debug) System.out.println("GenSimpTestData.generateHuman(): type: " + type);
        if (debug) System.out.println("GenSimpTestData.generateHuman(): frame: " + lfeat.framePart);
        if (debug) System.out.println("GenSimpTestData.generateHuman(): name: " + name);
        if (debug) System.out.println("GenSimpTestData.generateHuman(): english: " + english);
    }

    /** ***************************************************************
     * Create action sentences from a subject, preposition, direct object,
     * preposition and indirect object.  Indirect object and its preposition
     * can be left out.  Actions can be past and future tense or
     * wrapped in modals.
     */
    public void genWithRoles(StringBuffer english,
                             StringBuffer prop,
                             LFeatures lfeat) {

        if (debug) System.out.println("GenSimpTestData.genWithRoles()");
        int humCount = 0;
        for (int i = 0; i < humanMax; i++) {
            String role = lfeat.socRoles.getNext();
            if (lfeat.subj.equals(role)) continue;
            if (humCount++ > loopMax) break;
            lfeat.subj = role;
            int tryCount = 0;
            StringBuffer prop1 = null;
            do {
                prop1 = new StringBuffer(prop);
                StringBuffer english1 = new StringBuffer(english);
                getVerb(lfeat,false);
                genProc(english1, prop1, lfeat);
            } while (tryCount++ < 10 && prop1.equals(""));
        }
    }
    /** ***************************************************************
     */
    public void genWithHumans(StringBuffer english,
                              StringBuffer prop,
                              LFeatures lfeat) {


        if (debug) System.out.println("GenSimpTestData.genWithHumans()");
        int humCount = 0;
        lfeat.humans.clearReturns();
        for (int i = 0; i < humanMax; i++) {
            lfeat.subj = lfeat.humans.getNext();
            if (lfeat.subj.equals(lfeat.attSubj)) continue;
            if (humCount++ > humanMax) break;
            int tryCount = 0;
            StringBuffer prop1 = null;
            do {
                prop1 = new StringBuffer(prop);
                StringBuffer english1 = new StringBuffer(english);
                getVerb(lfeat,false);
                genProc(english1, prop1, lfeat);
            } while (tryCount++ < 10 && prop1.equals(""));
        }
    }

    /** ***************************************************************
     * use None, knows, believes, says, desires for attitudes
     */
    public void genAttitudes(LFeatures lfeat) {

        if (debug) System.out.println("GenSimpTestData.genAttitudes(): ");
        if (debug) System.out.println("GenSimpTestData.genAttitudes(): human list size: " + lfeat.humans.size());
        String that = "that ";
        int humCount = 0;
        HashSet<String> previous = new HashSet<>(); // humans appearing already in the sentence
        while (sentCount < sentMax) {
            StringBuffer english = new StringBuffer();
            StringBuffer prop = new StringBuffer();
            startOfSentence = true;
            Word attWord = attitudes.get(rand.nextInt(attitudes.size()));
            lfeat.attitude = attWord.term;
            if (debug) System.out.println("GenSimpTestData.genAttitudes(): ========================= start ");
            if (!attWord.term.equals("None") && !suppress.contains("attitude")) {
                StringBuffer type = new StringBuffer();
                StringBuffer name = new StringBuffer();
                lfeat.attNeg = rand.nextBoolean();
                if (lfeat.attNeg)
                    prop.append("(not (exists (?HA) (and  ");
                else
                    prop.append("(exists (?HA) (and ");
                generateHuman(english,prop,false,"?HA",type,name,lfeat);
                prop.append("(" + attWord.term + " ?HA ");
                lfeat.attSubj = name.toString(); // the subject of the propositional attitude
                startOfSentence = false;
                if (rand.nextBoolean() || lfeat.attitude.equals("desires"))
                    that = "that ";
                else
                    that = "";
                if (lfeat.attNeg)
                    english.append("doesn't " + attWord.root + " " + that);
                else
                    english.append(attWord.present + " " + that);
                if (attWord.term.equals("says"))
                    english.append("\"");
            }
            if (debug) System.out.println("GenSimpTestData.genAttitudes(): english: " + english);
            genWithModals(english,prop,lfeat);
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
    public void genWithModals(StringBuffer english,
                              StringBuffer prop,
                              LFeatures lfeat) {

        if (debug) System.out.println("GenSimpTestData.genWithModals()");
        int humCount = 0;
        AVPair modal = lfeat.modals.get(rand.nextInt(lfeat.modals.size()));
        lfeat.modal = modal;
        lfeat.negatedModal = biasedBoolean(2,10); // make it negated one time out of 5
        StringBuffer englishNew = new StringBuffer(english);
        StringBuffer propNew = new StringBuffer(prop);
        if (debug) System.out.println("genWithModals(): " + modal);
        if (!lfeat.modal.attribute.equals("None") && !suppress.contains("modal")) {
            if (lfeat.negatedModal)
                propNew.append("(not (modalAttribute ");
            else
                propNew.append("(modalAttribute ");
            englishNew.append(negatedModal(modal.value,lfeat.negatedModal));
            if (startOfSentence)
                englishNew.replace(0,1,englishNew.substring(0,1).toUpperCase());
            startOfSentence = false;
        }
        if (debug) System.out.println("GenSimpTestData.genWithModals(): english: " + english);
        int tryCount = 0;
        StringBuffer prop1 = null;
        do {
            prop1 = new StringBuffer(propNew);
            StringBuffer english1 = new StringBuffer(englishNew);
            getVerb(lfeat,false);
            genProc(english1, prop1, lfeat);
        } while (tryCount++ < 10 && prop1.equals(""));
    }

    /** ***************************************************************
     * negated, proc, object, caserole, prep, mustTrans, mustNotTrans, canTrans
     */
    public void genProcTable() {

        System.out.println("GenSimpTestData.genProcTable(): start");
        KBmanager.getMgr().initializeOnce();
        kb = KBmanager.getMgr().getKB(KBmanager.getMgr().getPref("sumokbname"));
        Collection<Capability> caps = collectCapabilities();
        //System.out.println("GenSimpTestData.genProcTable(): " + caps.size() + " capability entries");
        //System.out.println(caps);
        extendCapabilities(caps);
    }

    /** ***************************************************************
     * generate subclasses for each capability
     */
    public void extendCapabilities(Collection<Capability> caps) {

        for (Capability c : caps) {
            HashSet<String> childClasses = kb.kbCache.getChildClasses(c.proc);
            if (childClasses != null) {
                childClasses.add(c.proc); // add the original class
                for (String cls : childClasses) { // add each of the subclasses
                    c.proc = cls;
                    HashSet<Capability> hs = new HashSet<>();
                    if (capabilities.containsKey(c.proc))
                        hs = capabilities.get(c.proc);
                    hs.add(c);
                    capabilities.put(cls, hs);
                }
            }
            else {
                HashSet<Capability> hs = new HashSet<>();
                if (capabilities.containsKey(c.proc))
                    hs = capabilities.get(c.proc);
                hs.add(c);
                capabilities.put(c.proc, hs);
            }
        }
    }

    /** ***************************************************************
     * negated, proc, object, caserole, prep, mustTrans, mustNotTrans, canTrans
     * @return true if ok
     */
    public boolean checkCapabilities(String proc, String role, String obj) {

        if (debug) System.out.println("checkCapabilities(): starting");
        if (debug) System.out.println("checkCapabilities(): (proc,role,obj): " + proc + ", " + role + ", " + obj);
        if (capabilities.containsKey(proc)) {
            if (debug) System.out.println("checkCapabilities(): found capabilities: " + capabilities.get(proc));
            HashSet<Capability> caps = capabilities.get(proc);
            for (Capability c : caps) {
                if (c.caserole.equals(role) && (c.object.equals(obj) || kb.isSubclass(obj,c.object)) && !c.negated && c.must) {
                    if (debug) System.out.println("checkCapabilities(): approved");
                    return true;
                }
                if (c.caserole.equals(role) && !c.object.equals(obj) & !kb.isSubclass(obj,c.object) && c.must) {
                    if (debug) System.out.println("checkCapabilities(): rejected, object is not a " + c.object);
                    return false;
                }
                if (c.caserole.equals(role) && c.object.equals(obj) && c.mustNot) {
                    if (debug) System.out.println("checkCapabilities(): rejected.  Conflict with: " + c);
                    return false;
                }
                if (c.caserole.equals(role) && !c.object.equals(obj) && !kb.isSubclass(obj,c.object) && !c.negated && c.must) {
                    if (debug) System.out.println("checkCapabilities(): rejected.  Conflict with: " + c);
                    return false;
                }
                if (c.caserole.equals(role) && c.object.equals(obj) && c.negated) {
                    if (debug) System.out.println("checkCapabilities(): rejected.  Conflict with: " + c);
                    return false;
                }
            }
        }
        else {
            if (debug) System.out.println("checkCapabilities(): " + proc + " not found.");
        }
        if (debug) System.out.println("checkCapabilities(): approved by default");
        return true;
    }

    /** ***************************************************************
     * find attributes in SUMO that have equivalences to WordNet
     */
    public void showAttributes() {

        HashSet<String> attribs = kb.kbCache.getInstancesForType("Attribute");
        for (String s : attribs) {
            ArrayList<String> synsets = WordNetUtilities.getEquivalentSynsetsFromSUMO(s);
            if (debug) System.out.println("term and synset: " + s + ", " + synsets);
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
        System.out.println("  -a <filename> - generate logic/language pairs for all statements in KB");
        System.out.println("  -g <filename> - generate ground statement pairs for all relations");
        System.out.println("  -s <filename> <optional count> - generate NL/logic compositional <count> sentences to <filename> (no extension)");
        System.out.println("  -n - generate term formats from term names in a file");
        System.out.println("  -u - other utility");
    }

    /** ***************************************************************
     * init and call main routine.
     */
    public static void main(String args[]) {

        KBmanager.prefOverride.put("TPTP","no");
        try {
            if (args == null || args.length == 0 || args[0].equals("-h"))
                showHelp();
            else {
                FileWriter fweng = null;
                FileWriter fwlog = null;
                if (args.length > 1) {
                    fweng = new FileWriter(args[1] + "-eng.txt");
                    englishFile = new PrintWriter(fweng);
                    fwlog = new FileWriter(args[1] + "-log.txt");
                    logicFile = new PrintWriter(fwlog);
                }
                else {
                    if (args[0].equals("-s") || args[0].equals("-a") ||args[0].equals("-g")) {
                        System.out.println("Missing filename parameter for option");
                        System.exit(1);
                    }
                }
                if (args != null && args.length > 1 && args[0].equals("-s")) { // create NL/logic synthetically
                    if (args.length > 2)
                        sentMax = Integer.parseInt(args[2]);
                    GenSimpTestData gstd = new GenSimpTestData();
                    gstd.runGenSentence();
                    englishFile.close();
                    logicFile.close();
                }
                if (args != null && args.length > 0 && args[0].equals("-g")) { // generate ground statements
                    generate();
                    englishFile.close();
                    logicFile.close();
                }
                if (args != null && args.length > 0 && args[0].equals("-u")) {
                    GenSimpTestData gstd = new GenSimpTestData();
                }
                if (args != null && args.length > 0 && args[0].equals("-a")) { // generate NL/logic for all existing axioms
                    allAxioms();
                    englishFile.close();
                    logicFile.close();
                }
                if (args != null && args.length > 0 && args[0].equals("-tf"))
                    genMissingTermFormats();
                if (args != null && args.length > 0 && args[0].equals("-hu"))
                    generateAllHumans();
                if (args != null && args.length > 0 && args[0].equals("-t")) {
                    //testTypes();
                    //testNLG();
                    //testGiving();

                    //testPutting();
                    //testToy();
                    //testProgressPrint();
                    GenSimpTestData gstd = new GenSimpTestData();
                    KBmanager.getMgr().initializeOnce();
                    kb = KBmanager.getMgr().getKB(KBmanager.getMgr().getPref("sumokbname"));
                    String word = "Sheep";
                    System.out.println("exceptions: " + WordNet.wn.exceptionNounPluralHash);
                    for (int i = 0; i < 10; i++)
                        System.out.println("noun form: " + gstd.nounFormFromTerm(word,new AVPair(),""));
                    if (WordNet.wn.exceptionNounPluralHash.containsKey(word))
                        System.out.println("noun form for sheep: " + WordNet.wn.exceptionNounPluralHash.get(word));
                    LFeatures lfeat = new LFeatures(gstd);
                    lfeat.frame = "Something ----s something Adjective/Noun";
                    lfeat.framePart = lfeat.frame;
                    System.out.println("frame: ");
                    gstd.getPrepFromFrame(lfeat);
                    //gstd.testVerbs();
                    //gstd.testLFeatures();
                }
                if (args != null && args.length > 1 && args[0].equals("-n")) {
                    genTermFormatFromNames(args[1]);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
